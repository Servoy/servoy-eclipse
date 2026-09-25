# Triage Report — SVY-21496

**Verdict:** PROCEED

## Reported problem

Opening a form in the browser (the form-preview flow) throws an IDE error. The
stack trace logged is:

```
WARN  ... - Shutting down existing FormPreviewNGClient before creating a new one
ERROR com.servoy.j2db.server.ngclient.DataAdapterList - DataAdapterList(...) [internal]
      Unexpected execution outside of the event dispatch thread in DAL code...
java.lang.RuntimeException: DAL of form test
    at DataAdapterList.checkThatThisIsTheEventThread(DataAdapterList.java:646)
    at DataAdapterList.clearToWatchRelations(DataAdapterList.java:1399)
    at DataAdapterList.destroy(DataAdapterList.java:1383)
    at WebFormUI.destroy(WebFormUI.java:576)
    at WebFormController.destroy(WebFormController.java:306)
    at NGFormManager.destroySolutionSettings(NGFormManager.java:453)
    at NGFormManager$1.run(NGFormManager.java:423)
    at NGFormManager.propertyChange(NGFormManager.java:431)
    ...
    at ClientState.closeSolution(ClientState.java:1324)
    at NGClient.closeSolution(NGClient.java:933)
    at ClientState.shutDown(ClientState.java:1135)
    at NGClient.shutDown(NGClient.java:1429)
    at FormPreviewNGClient.<init>(FormPreviewNGClient.java:56)
    at Activator$1$1.init(Activator.java:104)
    at WebsocketEndpoint.start(WebsocketEndpoint.java:163)
    at NGClientEndpoint.start(NGClientEndpoint.java:74)
    ...
    at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(...)
    at ThreadPoolExecutor.runWorker(...)
    at java.lang.Thread.run(...)
```

The reporter's question is simply: *why do I get this exception?*

The ticket proposes no solution.

## Root-cause assessment

This is a **thread-affinity violation**: the previous `FormPreviewNGClient` is being
torn down on the wrong thread.

Walking the stack from the bottom up:

1. A new websocket connection for the form preview arrives on a **Tomcat request
   thread** (`NioEndpoint$SocketProcessor` → `WsHttpUpgradeHandler.init` →
   `NGClientEndpoint.start` → `WebsocketEndpoint.start`).
2. That calls the session's `init(...)`, which for the `formpreview` request path
   constructs a new `FormPreviewNGClient`
   (`com.servoy.eclipse.ngclient/src/com/servoy/eclipse/ngclient/startup/Activator.java:104`).
3. The `FormPreviewNGClient` **constructor** shuts down the previous singleton
   instance inline (`FormPreviewNGClient.java:53-57`):

   ```java
   if (instance != null && !instance.isShutDown())
   {
       Debug.warn("Shutting down existing FormPreviewNGClient before creating a new one");
       instance.shutDown(true);
   }
   instance = this;
   ```
4. `shutDown(true)` → `closeSolution(...)` fires the `"solution"` property change
   (`ClientState.closeSolution` `J2DBGlobals.firePropertyChange(this, "solution", ...)`
   — line 1324 in the trace), which `NGFormManager.propertyChange` handles by
   destroying the forms, reaching `DataAdapterList.destroy` →
   `clearToWatchRelations` → `checkThatThisIsTheEventThread()`.
5. `checkThatThisIsTheEventThread()`
   (`servoy_ngclient/src/com/servoy/j2db/server/ngclient/DataAdapterList.java:632`)
   logs an ERROR (constructing a `RuntimeException` purely as a stack marker) because
   the current thread is the Tomcat request thread, **not** the shutting-down client's
   event dispatch thread.

So the exception is not a real failure — it is a **defensive assertion** that DAL
teardown must happen on the client's event dispatch thread. The teardown of the
*old* client is being driven synchronously from the *new* client's constructor, which
runs on Tomcat's thread. The `checkThatThisIsTheEventThread` guard explicitly
whitelists `IDebugClient` (`if (app instanceof IDebugClient) return;`) precisely
because debug-client launch shuts down off-thread — but `FormPreviewNGClient` is a
plain `NGClient` subclass, not an `IDebugClient`, so it does not get that pass and
the assertion fires.

Confirmed the class hierarchy: `FormPreviewNGClient extends NGClient` and does **not**
implement `IDebugClient` (unlike `DebugNGClient`, which does).

## Ticket premise check

The ticket proposes no fix, so there is no premise to challenge. The report is a valid
"why does this happen" — and it is a genuine defect: an internal thread-affinity
assertion is being tripped by the form-preview client-recycling logic. It is Servoy
code, not user misconfiguration.

## Approaches considered

1. **Move the previous-instance shutdown onto the correct thread** — instead of
   calling `instance.shutDown(true)` inline in the constructor (on the Tomcat thread),
   dispatch it onto the *old* instance's event dispatch thread (e.g. via its
   websocket session's event dispatcher), or shut the old instance down *before* the
   new websocket `init` runs. This addresses the actual cause (wrong thread) rather
   than silencing the symptom.
   - Pros: fixes the real problem; DAL teardown then runs where it expects to; no
     weakening of the assertion that protects all other NG clients.
   - Cons: needs care around ordering (the old dispatcher must still be alive) and
     the `synchronized`/singleton handling in the constructor.

2. **Make the assertion whitelist `FormPreviewNGClient`** (or a broader
   "shutdown may run off-thread" marker interface), mirroring the existing
   `app instanceof IDebugClient` bypass.
   - Pros: minimal, matches the precedent already in `checkThatThisIsTheEventThread`;
     the form-preview client is a developer-only, throwaway client where off-thread
     forced teardown is arguably acceptable just like the debug client.
   - Cons: hides a real off-thread teardown rather than correcting it; if that
     teardown ever races with the old client's own dispatcher it could still be
     unsafe. It treats the log noise, not the threading.

3. **Do the recycle shutdown eagerly in `Activator.init` before constructing the new
   client**, still off-thread — same threading issue, so it would need to be combined
   with approach 1 to actually help.
   - Pros: separates "kill old" from "build new".
   - Cons: on its own does not change the thread the teardown runs on, so it does not
     fix the assertion.

4. **No code change** — treat the ERROR as benign log noise (the teardown of a
   throwaway preview client, forced, does complete).
   - Pros: nothing to break; functionally the preview still opens.
   - Cons: it logs an ERROR with a full stack trace on a normal, repeatable user
     action (open a form in the browser), which is exactly the "IDE error" the ticket
     is complaining about. It also means real off-thread-teardown bugs elsewhere would
     be indistinguishable from this expected noise. Not acceptable as a resolution.

5. **Reuse the existing preview client (mirror the debug NG client), retarget the form**
   — instead of shutting the old singleton down and creating a new one, reuse it when the
   incoming websocket session key matches (exactly as `Activator.java:125-128` reuses the
   `DebugNGClient` on a session-key match) and retarget it to the newly requested form via
   `showFormInMainPanel`; only shut down + create when there is no reusable instance.
   - Pros: eliminates the recycle shutdown entirely in the common case, so the DAL
     assertion is never reached; consistent with the established debug-client precedent;
     fewer client teardown/recreate cycles.
     - Cons: needs a retarget path on the reused client (show the new form on its event
     thread); a non-reusable/stale instance still needs a correctly-threaded shutdown
     (so approach #1 is retained as the fallback for that case).

## Recommendation

**PROCEED.** Following reviewer feedback, the chosen approach is **#5 — reuse the existing
preview client (mirroring the debug NG client) and retarget it to the new form**, with
approach **#1 (correctly-threaded off-thread shutdown)** retained as the fallback for the
non-reuse case (different session key / stale instance). This eliminates the recycle
shutdown entirely in the common same-session case (so the DAL assertion is never reached),
matches the established debug-client precedent, and keeps a safe shutdown for the residual
case. Approach #2 (assertion whitelist) is not taken — it treats the symptom, and #5+#1
never trip the assertion in the first place.

> **Note (updated after review):** the initial recommendation was approach #1 (off-thread
> shutdown) alone. Reviewer feedback pointed at the debug client's reuse pattern; the plan
> was updated to reuse-and-retarget (#5) with #1 as the fallback. See the spec
> (`docs/SVY-21496-formpreview-offthread-shutdown.spec.md`, §3) for the detailed design.

> **Note (verification):** the fix is verified by **manual testing**, not an automated
> test. The routing helpers stay package-private and `DataAdapterList` is untouched, so
> the change is a self-contained servoy-eclipse changeset that cherry-picks to `2026.LTS`
> without cross-repo coupling. See spec §4a for why no automated test was added (avoiding a
> single-test module here, and avoiding a `public`-API widening that a test in another repo
> would depend on).

## Git history findings

The off-thread shutdown was introduced by commit **9610799716**
(*"SVY-21323 reuse single FormPreviewNGClient instance and fix shutdown [ai]"*,
lvostinar, 2026-08-14). That change added the singleton `instance` tracking and the
"shut down the previous instance in the constructor" logic (lines 53-59) plus the
`shutdownExisting()` helper. The `FormPreviewNGClient` class itself was added earlier
by **c3d2cb399c** (*"SVY-21173 Create headless Cypress form test runner application"*).

So the exception is a direct consequence of the SVY-21323 client-recycling design:
it correctly reuses a single preview client, but it runs the previous client's forced
teardown on the wrong thread. Any fix should preserve the SVY-21323 intent (single
reused instance, previous one shut down) while moving the teardown onto a thread the
DAL accepts.
