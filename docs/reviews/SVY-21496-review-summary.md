# SVY-21496 — Peer-review summary: Form opened in browser throws IDE error

**Risk: MODERATE.** The worst surviving finding is a real but Developer-IDE-only, bounded
(10s), non-crash latency stall in the form-preview recycle path; plus a stale-reused-form
question the author left open and an unguarded NPE race. No crash, data, security, or
end-user/runtime exposure.

**Scope reviewed:** servoy-eclipse @ `4cac16b6c4` — `Activator.java` +
`FormPreviewNGClient.java` in `com.servoy.eclipse.ngclient/.../startup/` (+ spec/triage docs).
Single repo, single commit, on `master`.

## Manual test plan

1. Open a form in the browser via the Developer preview (`?formpreview=<formName>`).
2. Open a **second** preview on the same session (reconnect / re-request a different form).
   Confirm the form shows and **no** `Unexpected execution outside of the event dispatch
   thread in DAL code` ERROR appears in the IDE log.
3. Confirm exactly one `FormPreviewNGClient` is active (single-instance invariant preserved).
4. **Stale-reuse:** preview form A, add a new form B to the solution, re-request
   `?formpreview=B` on the same session. Confirm B renders (not blank / not-found).
5. **Stall:** open a preview, make the old client's event thread busy (or force its
   dispatcher into teardown), open a second preview, and save an edited form during that
   window. Confirm the IDE does not freeze longer than ~10s and recovers (warn logged,
   proceeds).
6. **NPE race:** race a Cypress `shutdownExisting()` against a concurrent `?formpreview=`
   open; confirm no error surfaces from the websocket `init`.
7. **Cypress license:** run a multi-spec Cypress form-test batch; confirm no "No more
   licenses available" warnings between specs.

No automated test ships with this commit (deliberate — spec §4a; follow-up SVY-21508). A
targeted unit on `shutdownRouting` with a stub dispatcher whose `addEvent` is a no-op would
cheaply confirm the drop-case 10s wait.

## Possible improvements / follow-ups

- **Monitor-held 10s stall.** The constructor / `shutdownExisting()` hold the
  `FormPreviewNGClient.class` monitor across `shutdownRouting`'s `done.await(10s)`, so a
  wedged old dispatcher freezes `getInstance()` / `shutdownExisting()` — including the
  SVY-21509 editor-save refresh on the AWT thread — for up to 10s. Consider releasing the
  monitor before awaiting, or a shorter timeout.
- **Latch never counts down on an exited dispatcher.** If the old dispatcher is already
  destroyed (`exit==true`) but `getEventDispatcher(false)` still returns it and
  `isShutDown()` is false, `EventDispatcher.postEvent` silently drops the posted shutdown
  `Runnable` (it only cancels `Future`s), so `done.countDown()` never runs and the caller
  waits the full 10s. Check the dispatcher is live/not-exited before posting.
- **Reused DAL / form-manager reset (author's open question, spec §7).** The reuse path keeps
  the existing form manager + DAL and only calls `showFormInMainPanel(newForm)`; decide
  whether it must rebuild the form set when the solution changed between previews.
- **Unguarded NPE race.** The reuse branch dereferences `getWebsocketSession()` and casts
  `getFormManager()` with no null guard (the sibling `refreshFormPreview` does guard
  `getWebsocketSession() == null`).
- **Release targeting.** Fix Version is `2026.9.0` (released) but the commit is on `master`;
  confirm the cherry-pick to the 2026.9.x line actually landed.

Security relevance: LOW — no auth, `formName`-validation, or multi-tenant surface touched;
reuse is gated by the same `getSessionKey()` equality the debug client uses.
