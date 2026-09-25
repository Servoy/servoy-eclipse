# Spec: SVY-21496 — Form opened in browser throws IDE error (off-thread FormPreviewNGClient teardown)

## 1. Goal

Opening a form in the browser via the form-preview flow (`?formpreview=formName`) a
second time logs an ERROR with a full stack trace in the IDE, even though the preview
still works. The cause is that the previous `FormPreviewNGClient` singleton is torn down
synchronously from the *new* client's constructor, which runs on a Tomcat request
thread — not on the old client's event dispatch thread. This trips the
`DataAdapterList.checkThatThisIsTheEventThread()` defensive assertion during DAL
teardown. The goal is to eliminate this spurious ERROR by making the form-preview flow
mirror the normal **debug NG client** recycle pattern: when a second preview arrives on
the same websocket session, **reuse** the existing `FormPreviewNGClient` and retarget it
to the new form (as the debug client reuses its instance on a session-key match) instead
of shutting it down and recreating it. For the remaining non-reuse case (different session
/ stale instance), the old instance's shutdown is routed onto its own event dispatch
thread so the assertion is never tripped. This preserves the SVY-21323
single-active-instance intent.

## 2. Background

### 2.1 The form-preview client-recycling flow

When a websocket connection with the `formpreview` request parameter arrives, the
session factory in
`com.servoy.eclipse.ngclient/src/com/servoy/eclipse/ngclient/startup/Activator.java:99-106`
constructs a new `FormPreviewNGClient` on the incoming Tomcat request thread
(`NioEndpoint$SocketProcessor` → `WsHttpUpgradeHandler.init` → `NGClientEndpoint.start`
→ `WebsocketEndpoint.start` → session `init(...)`).

`FormPreviewNGClient` maintains a single static `instance`. Its constructor
(`FormPreviewNGClient.java:53-59`) shuts down the previous instance inline:

```java
if (instance != null && !instance.isShutDown())
{
    Debug.warn("Shutting down existing FormPreviewNGClient before creating a new one");
    instance.shutDown(true);
}
instance = this;
```

`shutDown(true)` → `ClientState.closeSolution(...)` fires the `"solution"` property
change, which `NGFormManager.propertyChange` handles by destroying the forms, reaching
`WebFormUI.destroy` → `DataAdapterList.destroy` → `clearToWatchRelations` →
`checkThatThisIsTheEventThread()`.

### 2.2 The thread-affinity assertion

`DataAdapterList.checkThatThisIsTheEventThread()`
(`servoy_ngclient/src/com/servoy/j2db/server/ngclient/DataAdapterList.java:632-655`)
logs an ERROR (wrapping a `RuntimeException` as a stack marker) when DAL teardown runs
off the client's event dispatch thread:

```java
INGApplication app = formController.getApplication();
if (app instanceof IDebugClient) return; // debug client launch can call app.shutDown(true) outside of the event thread...
INGClientWebsocketSession wss;
if ((wss = app.getWebsocketSession()) != null)
{
    IEventDispatcher ed;
    if ((ed = wss.getEventDispatcher(false)) != null)
    {
        if (!ed.isEventDispatchThread())
            log.error(...);
    }
    ...
}
```

It explicitly whitelists `IDebugClient` because debug-client launch legitimately calls
`shutDown(true)` off-thread. `FormPreviewNGClient extends NGClient` and does **not**
implement `IDebugClient`, so it does not get that pass and the assertion fires.

### 2.3 The precedent this mirrors

`DebugClientHandler.createDebugNGClient(...)`
(`servoy_debug/src/com/servoy/j2db/debug/DebugClientHandler.java:555-563`) does exactly
the same recycle — shut down the previous `DebugNGClient` off-thread, then create a new
one — but `DebugNGClient` implements `IDebugClient`, so the assertion is bypassed. The
`FormPreviewNGClient` singleton logic (added by SVY-21323, commit `9610799716`) was
modelled on this pattern but on a client type that is not exempt.

### 2.4 The old client's dispatcher is still alive at recycle time

Because the previous `FormPreviewNGClient` is only shut down *when the new one is being
constructed*, the old instance is still fully live at that moment — its websocket
session and event dispatcher thread still exist. `NGClient.shutDown(boolean)`
(`servoy_ngclient/.../NGClient.java:1425`) itself already interacts with the dispatcher
(e.g. `getWebsocketSession().getEventDispatcher().addEvent(null)`), confirming the
dispatcher is expected to be available during shutdown. `BaseWebsocketSession`
exposes `getEventDispatcher(false)` (returns null if none) and `getEventDispatcher()`
(creates on demand); `IEventDispatcher` exposes `isEventDispatchThread()` and
`addEvent(Runnable)`. This makes approach #1 (dispatch the old instance's shutdown onto
its own event thread) viable.

## 3. Design

### 3.1 Chosen approach — reuse the existing preview client (mirror the debug client), with a safe off-thread shutdown fallback

Reviewer feedback: the form-preview flow should mirror how the normal **debug NG client**
is recycled in `Activator.init`. There, the last `else` branch does **not** shut the
existing client down — it **reuses** it when the incoming websocket session matches:

```java
NGClient debugNGClient = (NGClient)service.getDebugNGClient();
if (debugNGClient != null && !debugNGClient.isShutDown() &&
    debugNGClient.getWebsocketSession().getSessionKey().equals(getSessionKey()))
    setClient(debugNGClient);
else
    setClient((NGClient)service.createDebugNGClient(this));
```

The form-preview path (`Activator.java:99-106`) instead unconditionally constructs a new
`FormPreviewNGClient`, whose constructor force-shuts-down the previous singleton on the
Tomcat request thread — which is what trips
`DataAdapterList.checkThatThisIsTheEventThread()`. We change it to mirror the debug
pattern:

**In `Activator`'s `formpreview` branch:**

- If there is an existing `FormPreviewNGClient` instance that is not shut down **and** its
  websocket session key equals the incoming session's key (`getSessionKey()`), **reuse**
  it: set it as the client (`setClient(existing)`) and **retarget** it to the newly
  requested form rather than destroying and recreating it. Retargeting is done by showing
  the new form on the reused client's main panel
  (`NGFormManager.showFormInMainPanel(formName)`), performed on the reused client's own
  event dispatch thread (post via its dispatcher if not already on it) so no thread
  affinity is violated. Update the tracked target form name accordingly.
- Otherwise (no reusable instance, or a different session key), construct a new
  `FormPreviewNGClient` as today. When an old, non-reusable instance still exists, it is
  shut down — but **correctly on the old client's own event dispatch thread** (the §3.2
  helper), never inline on the Tomcat thread.

This means:
- In the common "open preview again on the same session" case, no shutdown happens at all
  (exactly like the debug client) — so the DAL assertion is never reached.
- In the fallback case (stale/other-session instance), the shutdown still happens but is
  routed onto the correct thread, so the assertion is not tripped there either.

### 3.2 Safe off-thread shutdown helper (fallback path)

For the non-reuse case, keep a helper (`shutdownOnEventThread(old)`) that shuts the old
instance down on its own event dispatch thread:

- Obtain the old instance's event dispatcher via
  `getWebsocketSession().getEventDispatcher(false)`.
- If a dispatcher exists and the current thread is **not** its event dispatch thread, post
  `shutDown(true)` onto that dispatcher via `dispatcher.addEvent(...)` and wait
  (time-bounded) for it to complete.
- If no dispatcher exists (already gone) or we are already on its event thread, call
  `shutDown(true)` directly — this cannot trip the assertion (no dispatcher to check
  against, or already on the right thread).
- The wait is time-bounded (`CountDownLatch.await(SHUTDOWN_TIMEOUT_SECONDS, SECONDS)`)
  and proceeds with a `Debug.warn` on timeout/interrupt rather than blocking the caller
  (which may hold the class monitor) indefinitely.

### 3.3 Retargeting semantics

- `pendingTargetFormName` is read by `createFormManager().makeSolutionSettings(...)` only
  when a client is first created. On reuse the form manager already exists, so retargeting
  must call `showFormInMainPanel(newFormName)` directly on the reused client (on its event
  thread) and update `targetFormName` for consistency.
- The reused client keeps its existing websocket session, form manager and DAL — the
  browser reconnecting on the same session key expects that same client.

### 3.4 Ordering and safety considerations

- Keep `synchronized` singleton handling so concurrent preview connections do not race on
  `instance`.
- The reuse decision (session-key match) and the fallback shutdown target the **old**
  instance's dispatcher, captured before any `instance = this` reassignment.
- Guard against the old dispatcher having already terminated (`getEventDispatcher(false)`
  returns null, or the client is already shut down) — the direct `shutDown(true)` fallback
  is used and is safe.

### 3.5 Git history

The off-thread shutdown was introduced by commit `9610799716` (*"SVY-21323 reuse single
FormPreviewNGClient instance and fix shutdown [ai]"*), which added the singleton
`instance` tracking, the constructor shutdown block, and the `shutdownExisting()` helper.
`FormPreviewNGClient` itself was added earlier by `c3d2cb399c` (SVY-21173). The debug
client's reuse pattern (`Activator.java:125-128` + `DebugClientHandler.createDebugNGClient`)
is the model the reviewer wants the preview path to follow. This fix preserves the
SVY-21323 single-active-instance intent while switching the common case from
force-shutdown-and-recreate to reuse-and-retarget (like the debug client), and routing the
remaining fallback shutdown onto a thread the DAL accepts.

## 4. Implementation plan

1. In `com.servoy.eclipse.ngclient/src/com/servoy/eclipse/ngclient/startup/Activator.java`,
   rework the `requestParams.containsKey("formpreview")` branch (lines 99-106) to mirror
   the debug client's reuse pattern (lines 125-128):
   - resolve the existing `FormPreviewNGClient.getInstance()`;
   - if it is non-null, not shut down, and its
     `getWebsocketSession().getSessionKey().equals(getSessionKey())` → **reuse** it:
     `setClient(existing)` and retarget it to the requested form (§3.3);
   - else construct a new `FormPreviewNGClient` as today (`setPendingTargetFormName` +
     `new FormPreviewNGClient(...)` + `setClient(...)`).
2. In `FormPreviewNGClient`, expose what `Activator` needs:
   - a `static FormPreviewNGClient getInstance()` accessor (guarded, returns the current
     singleton);
   - a `retarget(String formName)` method that updates the tracked target form and shows
     the new form on the reused client's main panel
     (`((NGFormManager)getFormManager()).showFormInMainPanel(formName)` or equivalent),
     performed on the client's own event dispatch thread (post via its dispatcher if not
     already on it). Make `targetFormName` non-final to allow retargeting, or track the
     current target separately.
3. Keep/retain the safe off-thread shutdown helper (`shutdownOnEventThread(old)` →
   `shutdownRouting(...)`) for the **non-reuse** path (different session key / stale
   instance): the constructor and `shutdownExisting()` route the previous instance's
   teardown through it (§3.2), so any remaining shutdown runs on the old client's event
   thread with a bounded wait.
4. Verify callers: `eclipse-ide_findReferences` on the `FormPreviewNGClient` constructor,
   `shutdownExisting`, and the new `getInstance`/`retarget`. Confirm `Activator` is the
   only construction site and behaves correctly for reuse vs. new.
5. Post-edit workflow on every changed Java file: `eclipse-coder_organizeImports`,
   `eclipse-coder_formatFile`, `eclipse-ide_getCompilationErrors` (zero errors), fix any
   blocking Spotbugs.
6. Manually verify (see §8 / manual test): opening a form preview a second time on the
   same session **reuses** the client (no `Shutting down existing FormPreviewNGClient`
   warn, no DAL ERROR) and shows the newly requested form; a non-reusable instance is
   shut down without the DAL off-thread ERROR.

## 4a. Testing

**No automated test is added for this fix.** The routing helpers (`shutdownRouting`,
`runRouting`) are package-private in `com.servoy.eclipse.ngclient.startup`, and the reuse
decision + retarget need a live NG websocket session/client. The only way to unit-test the
package-private seams from another bundle (the Servoy-Copilot `com.servoy.eclipse.developer.mcp.tests`
fragment, where the other form-preview tests live) would be to widen those seams to
`public`. That was deliberately rejected: this fix is cherry-picked to `2026.LTS`, and a
test in a *different* repo that depends on a `public`-widened API in servoy-eclipse would
not cherry-pick atomically — the test and the production visibility would have to be kept
in lockstep across branches. Co-locating an automated test in servoy-eclipse would require
a new `com.servoy.eclipse.ngclient.tests` fragment for a single pure-logic test, which was
also judged not worth the module footprint.

The fix is therefore verified by **manual testing** (§6): a second form preview on the same
session was confirmed to reuse+retarget the client with **no** `DataAdapterList ...
Unexpected execution outside of the event dispatch thread` ERROR in the runtime log, and
the newly requested form rendered correctly. The seams are kept package-private and
`DataAdapterList` is untouched, so the change cherry-picks cleanly.

## 5. Acceptance criteria

- [ ] Opening a form in the browser via `?formpreview=formName` a second time does **not**
      log the `ERROR com.servoy.j2db.server.ngclient.DataAdapterList - ... Unexpected
      execution outside of the event dispatch thread in DAL code...` message with its
      `RuntimeException: DAL of form ...` stack trace.
- [ ] When the second preview arrives on the **same** websocket session key, the existing
      `FormPreviewNGClient` is **reused** (not shut down and recreated) — mirroring the
      debug NG client's recycle pattern — and is **retargeted** to show the newly
      requested form.
- [ ] When no reusable instance exists (or the session key differs), a new
      `FormPreviewNGClient` is created; any old instance that must be shut down in that
      case has its DAL teardown run on that instance's event dispatch thread (or the
      direct-shutdown fallback only when no live dispatcher exists / already on the event
      thread).
- [ ] Only a single `FormPreviewNGClient` instance is active at a time (SVY-21323 intent
      preserved).
- [ ] The `checkThatThisIsTheEventThread()` guard in `DataAdapterList` is left unchanged
      for all client types (no assertion weakening).
- [ ] The bundles compile with no new errors and imports are organized/formatted.
- [ ] The routing helpers (`shutdownRouting`, `runRouting`) remain package-private (no
      visibility widening for testability), so the change cherry-picks to `2026.LTS`
      without any cross-repo test coupling.

## 6. Out of scope

- Any redesign of the broader NG client shutdown/threading model beyond the
  form-preview recycle path.
- Changing `DebugClientHandler` / `DebugNGClient` recycling behaviour.
- Changing the form-preview URL/HTTP filter flow or authentication bypass.
- Modifying the `DataAdapterList.checkThatThisIsTheEventThread()` assertion.
- An automated regression test (see §4a — verified manually instead, to avoid a
  single-test module in servoy-eclipse or cross-repo `public`-API coupling that would
  break the `2026.LTS` cherry-pick).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Does a second preview reconnect on the SAME websocket session key (→ reuse) or a NEW one (→ fallback create)? Resolved pragmatically by handling both: reuse on match, safe off-thread shutdown otherwise. | dev | resolved |
| Retargeting a reused client to a different form: is `showFormInMainPanel(formName)` on the event thread sufficient, or does the reused DAL need any additional reset? | dev | open |
