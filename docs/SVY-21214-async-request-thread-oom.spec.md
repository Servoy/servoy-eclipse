# Spec: SVY-21214 — `executeAsyncRequest()` causes unbounded thread creation and OOM via `Deferred.resolve()` → `invokeLater()` → `SessionClient.executing.lock()` contention

## 1. Goal

Prevent unbounded thread creation and OOM crashes when `Deferred.resolve()` / `Deferred.reject()` is called on headless clients (particularly REST-WS pooled `SessionClient` instances under load). The fix must preserve the single-threaded execution model of the script engine (the `executing` lock guarantees only one thread runs in the script engine at a time) while ensuring threads do not pile up indefinitely, and are properly cleaned up when a REST call completes or a client is shut down.

## 2. Background

### 2.1 How `Deferred` works today

`Deferred` (in `servoy_shared`) wraps a Rhino `NativePromise`. When a plugin resolves or rejects the deferred, it calls `application.invokeLater(runnable)` where the runnable enters a Rhino `Context`, calls the resolve/reject function, and processes microtasks.

- **Source:** `servoy_shared/src/com/servoy/j2db/scripting/Deferred.java` (lines 64–83 for resolve, 85–104 for reject)

### 2.2 How `SessionClient.invokeLater` works

`ClientState.invokeLater(Runnable)` delegates to `doInvokeLater(Runnable)`. In `SessionClient`, `doInvokeLater` directly calls `invokeAndWait(r)` which acquires the `executing` ReentrantLock before running the callback:

```java
protected void doInvokeLater(Runnable r) {
    invokeAndWait(r);
}

public void invokeAndWait(Runnable r) {
    IServiceProvider prev = testThreadLocals();
    if (!SwingUtilities.isEventDispatchThread()) executing.lock();
    try { r.run(); }
    finally {
        if (!SwingUtilities.isEventDispatchThread()) executing.unlock();
        unsetThreadLocals(prev);
    }
}
```

- **Source:** `servoy_headless_client/src/com/servoy/j2db/server/headlessclient/SessionClient.java` (lines 858–877)

### 2.3 Why the `executing` lock exists — single-threaded script engine invariant

The `executing` lock ensures that **only one thread at a time executes in the script engine** for a given client. The Rhino script engine is not thread-safe — concurrent execution in the same script engine would cause corruption. This is analogous to an "event thread" in a GUI application: all script execution must be serialized through this lock.

**This invariant must NOT be broken.** Any fix must preserve the guarantee that only one thread runs in the script engine at any time.

### 2.4 The contention problem

When `executeAsyncRequest()` fires HTTP requests via the executor pool (from `IClientPluginAccess.getExecutor()`), each completed request calls `deferred.resolve()` which calls `application.invokeLater()` which on `SessionClient` calls `invokeAndWait()`. This tries to acquire `executing` — but that lock is **already held** by the Tomcat/request thread currently executing the `ws_*` method that initiated the async calls.

Result: every resolve/reject callback thread blocks on the lock until the `ws_*` method returns. Under load (e.g. 50+ concurrent REST requests, each firing 10–20 async HTTP calls), this creates thousands of threads waiting on the same lock — observed as 5,500+ threads in production, leading to `OutOfMemoryError: unable to create native thread`.

### 2.5 The fundamental design mismatch

As the architect noted:

> "It is an async request! It is meant to be executed after the current call. So async can't really be used in a ws_xx stuff! That is meant to be synchronized!"

> "really can't work... when would those promises be done?? when will that be executed? [...] all those requests will just end up in 'void' because the ws_test code directly returns."

The `ws_*` method holds the lock for the duration of the request. When the method returns, the REST-WS infrastructure releases the lock and returns the client to the pool. At that point, all blocked threads would stampede to acquire the lock one by one. But by then the REST response has already been sent — the resolved promise values have nowhere to go.

This means the threads are not just contending — they are **doing useless work** (resolving promises whose results are lost). The real fix must:
1. Prevent the unbounded thread pileup (the OOM crash)
2. Discard waiting threads when the REST call that spawned them completes
3. Ensure that on client shutdown, all threads waiting on the lock are released and exit cleanly
4. Preserve the single-threaded execution invariant

### 2.6 REST-WS client lifecycle

The REST-WS plugin manages `SessionClient` (actually `HeadlessClient`) instances in an Apache Commons Pool2 keyed pool:

1. **Borrow:** `RestWSPlugin.getClient(solutionName)` → `pool.borrowObject(solutionName)` (`RestWSPlugin.java:368`)
2. **Execute:** Servlet handler calls `client.getPluginAccess().executeMethod()` → `invokeAndWait()` → acquires `executing` lock → runs `ws_*` method → releases lock (`SessionClient.java:867–874`)
3. **Release:** Servlet's finally block calls `plugin.releaseClient(poolKey, client, reloadSolution)` (`RestWSServlet.java:281`)
4. **Return to pool:** `releaseClient` either returns directly (`pool.returnObject`) or does async close+reload first (`RestWSPlugin.java:377–429`)

**Key insight:** After step 2 completes and the lock is released, any threads still waiting to acquire the lock are doing useless work — the HTTP response has already been sent. These threads should be discarded immediately.

- **Source:** `servoy-extensions/.../plugins/rest_ws/RestWSPlugin.java` (lines 364–429)
- **Source:** `servoy-extensions/.../plugins/rest_ws/RestWSServlet.java` (line 281)

### 2.7 Affected APIs

| API | Location | Mechanism |
|-----|----------|-----------|
| `BaseRequest.executeAsyncRequest()` (Promise variant) | `servoy-extensions/.../http/BaseRequest.java:516` | `Deferred.resolve()` on executor thread |
| `BaseRequest.js_executeAsyncRequest()` (callback variant) | `servoy-extensions/.../http/BaseRequest.java:638` | `FunctionDefinition.executeAsync()` → `invokeLater()` |
| `HttpClient.executeRequest(BaseRequest[])` | `servoy-extensions/.../http/HttpClient.java:515` | `Deferred.resolve()` on executor thread |
| `RawSQLProvider.js_executeSQLAsync()` | `servoy-extensions/.../rawSQL/RawSQLProvider.java:591` | `Deferred.resolve()` on executor thread |

### 2.8 Contrast: NGClient

`NGClient.doInvokeLater` posts events to a single-threaded `EventDispatcher` queue, which does **not** block the calling thread. This architecture is immune to the problem because:
1. The event is queued (non-blocking enqueue).
2. The event dispatcher drains events sequentially.

`SessionClient` lacks an event dispatcher and instead uses a simple lock-based `invokeAndWait`.

### 2.9 Shutdown behavior gap

Currently, `SessionClient.shutDown()`:
- Sets `shuttingDown = true`
- Calls `super.shutDown()` (sets `isShutdown = true`, closes solution)
- Shuts down `scheduledExecutorService`

But it does **NOT**:
- Interrupt threads blocked on `executing.lock()`
- Check `isShutDown()` before acquiring the lock in `invokeAndWait`
- Signal waiting threads to abort

This means if a client is shut down while threads are waiting, those threads remain blocked indefinitely (or until the lock holder finishes — but it may never finish if the client is being destroyed).

## 3. Design

### 3.1 Primary mechanism: Discard waiting threads after REST call completes

The core fix uses the `ReentrantLock` itself to identify and interrupt waiting threads — no separate tracking data structures needed.

**Key insight:** `ReentrantLock` internally tracks its wait queue. By subclassing it to expose the protected `getQueuedThreads()` method, we can directly interrupt all threads waiting for the lock without maintaining a parallel `Set<Thread>`.

**Rationale:** Once a `ws_*` method returns, any threads blocked waiting to resolve/reject promises are doing useless work — the HTTP response is already sent. Discarding them immediately:
- Prevents the thread stampede (threads don't execute one by one after unlock)
- Frees executor pool threads back to the shared pool instantly
- Leaves the client in a clean state before it's returned to the pool

```java
// SessionClient.java — subclass ReentrantLock to expose wait queue
private final ReentrantLock executing = new ReentrantLock() {
    public Collection<Thread> getQueuedThreads() {
        return super.getQueuedThreads();
    }
};

public void discardWaitingInvocations() {
    Collection<Thread> queued = executing.getQueuedThreads();
    if (!queued.isEmpty()) {
        Debug.log("SessionClient.discardWaitingInvocations: interrupting " + queued.size() + " waiting thread(s)");
        for (Thread t : queued) {
            t.interrupt();
        }
    }
}
```

```java
// RestWSPlugin.java — releaseClient()
public void releaseClient(final String poolKey, final IHeadlessClient client, boolean reloadSolution) {
    if (client instanceof SessionClient sessionClient) {
        sessionClient.discardWaitingInvocations();
    }
    // ... existing pool return logic ...
}
```

### 3.2 Interruptible lock acquisition in `invokeAndWait`

Replace `executing.lock()` with `executing.lockInterruptibly()` so that threads respond immediately to interrupts from `discardWaitingInvocations()`:

```java
public void invokeAndWait(Runnable r) {
    IServiceProvider prev = testThreadLocals();
    if (!SwingUtilities.isEventDispatchThread()) {
        try {
            executing.lockInterruptibly();
        } catch (InterruptedException e) {
            Debug.warn("SessionClient.invokeAndWait: interrupted while waiting for lock, discarding runnable");
            Thread.currentThread().interrupt();
            unsetThreadLocals(prev);
            return;
        }
    }
    try {
        r.run();
    } finally {
        if (!SwingUtilities.isEventDispatchThread()) executing.unlock();
        unsetThreadLocals(prev);
    }
}
```

**Benefits:**
- Minimal change: replaces `lock()` with `lockInterruptibly()` — one line difference in the lock path.
- Threads respond immediately to `discardWaitingInvocations()` via interrupt — no polling, no timeout.
- No extra data structures (`Set<Thread>`, `AtomicInteger`) — the lock's own queue is the source of truth.
- The single-threaded execution invariant is preserved: only one thread holds the lock at a time.

### 3.3 Secondary safety: Interrupt waiting threads on shutdown

In `SessionClient.shutDown()`, after setting `shuttingDown = true`, call `discardWaitingInvocations()` as a secondary safety net:

```java
// In shutDown():
shuttingDown = true;
discardWaitingInvocations();
```

This ensures threads are released on client destruction even if `discardWaitingInvocations()` was not called via `releaseClient` (e.g., client evicted from pool directly).

### 3.4 Alternatives considered and rejected

**Queue-based `invokeLater` (drain-on-unlock):**
Enqueue runnables and drain them at the end of `invokeAndWait`. Rejected because:
- It changes the single-threaded execution model: the current lock holder would execute queued runnables that belong to a different logical request, violating isolation.
- For one client there can be many requests executing (in the same script engine via REST-WS pooling). The queue drain would mix runnables from different requests.
- Runnables queued after the `ws_*` method returns would never drain unless another `invokeAndWait` happens, or complex trigger logic is added.

**Re-entrant inline execution:**
If the calling thread already holds the lock, execute inline. Rejected because it would allow concurrent execution in the script engine from the executor thread, breaking the single-threaded invariant.

**Interrupt only on `shutDown`:**
Original approach. Insufficient because threads still pile up during normal operation and only get released when the client is destroyed (which may be much later or never for pooled clients). The primary problem — thousands of threads blocking during a REST call — is not addressed by shutdown-only cleanup.

**Separate `Set<Thread>` tracking:**
Maintain a `ConcurrentHashMap.newKeySet()` of waiting threads, adding before `lock()` and removing after. Rejected because:
- Redundant — `ReentrantLock` already tracks its wait queue internally via `getQueuedThreads()`.
- Adds complexity and potential for inconsistency between the set and the actual lock queue.
- `getQueuedThreads()` is the authoritative source of truth.

## 4. Implementation plan

### 4.1 `SessionClient.java` changes

All in `servoy_headless_client/src/com/servoy/j2db/server/headlessclient/SessionClient.java`:

1. **Change `executing` field to expose wait queue:**
   - Subclass `ReentrantLock` inline to make `getQueuedThreads()` public
   
2. **Add `discardWaitingInvocations()` method:**
   - Public method that calls `executing.getQueuedThreads()` and interrupts each thread
   - Logs the count of interrupted threads

3. **Modify `invokeAndWait(Runnable r)`:**
   - Replace `executing.lock()` with `executing.lockInterruptibly()`
   - Add `try/catch (InterruptedException)` — log, re-set interrupt flag, return without executing

4. **Modify `shutDown(boolean force)`:**
   - After setting `shuttingDown = true`, call `discardWaitingInvocations()`

### 4.2 `RestWSPlugin.java` changes

In `servoy-extensions/.../plugins/rest_ws/RestWSPlugin.java`:

5. **Modify `releaseClient(String poolKey, IHeadlessClient client, boolean reloadSolution)`:**
   - Before the existing pool return logic, call `((SessionClient) client).discardWaitingInvocations()` (with appropriate type check)
   - This immediately releases all threads that were waiting during the REST call

### 4.3 Interface consideration

Since `RestWSPlugin` references `IHeadlessClient` (an interface), and `discardWaitingInvocations()` is on `SessionClient`:
- Option A: Add `discardWaitingInvocations()` to `IHeadlessClient` interface (in `servoy_shared`)
- Option B: Cast to `SessionClient` in `releaseClient` with `instanceof` check
- **Recommended: Option A** — the method is generally useful for any headless client implementation and makes the API explicit.

## 5. Acceptance criteria

- [ ] `releaseClient()` in `RestWSPlugin` calls `discardWaitingInvocations()` on the client before returning it to the pool
- [ ] `discardWaitingInvocations()` interrupts all threads currently waiting for the `executing` lock
- [ ] The `executing` lock is acquired with `lockInterruptibly()` instead of `lock()` — threads respond immediately to interrupt
- [ ] Threads waiting for the lock exit cleanly on interrupt (from `discardWaitingInvocations` or `shutDown`)
- [ ] `shutDown()` calls `discardWaitingInvocations()` as a secondary safety net
- [ ] The single-threaded script engine invariant is preserved: only one thread executes in the script engine at a time per client
- [ ] Under load (50+ concurrent REST requests each firing 20 async HTTP calls), waiting threads are discarded when each REST call completes
- [ ] No additional tracking data structures needed — uses `ReentrantLock.getQueuedThreads()` directly
- [ ] No behavioral regression for NGClient (which already uses event dispatcher)
- [ ] No regression for `FunctionDefinition.executeAsync()` callback path
- [ ] Client returned to pool is in a clean state (no lingering waiting threads)

## 6. Out of scope

- Changing `Deferred.java` API or adding thread-awareness to `Deferred` itself
- Changes to `NGClient` (already uses safe event dispatcher pattern)
- REST-WS plugin pool sizing or lifecycle management
- Deprecating `executeAsyncRequest()` usage in `ws_*` methods (may be addressed in documentation separately)
- Fixing the fact that promises cannot meaningfully resolve after a `ws_*` method returns (this is a developer usage issue, not a crash issue)
- Adding an event dispatcher/queue architecture to `SessionClient` (much larger architectural change)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `discardWaitingInvocations()` be added to `IHeadlessClient` interface or accessed via cast? | Architect | open |
| Should discarded runnables (due to interrupt) be logged at WARN or DEBUG level? | Architect | open |
