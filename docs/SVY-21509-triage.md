# Triage Report — SVY-21509

**Verdict:** PROCEED

## Reported problem

A form opened in the browser via the form editor's *Show Form in Browser* action
(the `?formpreview=formName` / `FormPreviewNGClient` flow) does **not** refresh when a
change is subsequently made and saved in the form editor. The regular NG client (the
normal debug NG client) refreshes automatically on editor changes; the preview client
should behave the same way. Today the preview only updates on a manual browser reload.

Steps to reproduce (from the ticket): open a form in the editor → open it in the browser
→ make and save a change in the editor → the browser preview does not update.

The ticket proposes no concrete implementation — it only states the expected behaviour
("should behave like the regular NG client"). Related to SVY-21496 (same preview flow,
just fixed on `release` / commit `4cac16b`).

## Root-cause assessment

Editor changes reach clients through a persist-change broadcast that **only targets
registered debug clients** — the preview client is not on that list, and it is not an
`IDebugClient`, so it is never told to refresh. Verified end to end:

1. **The push mechanism.** In `com.servoy.eclipse.core.Activator` a persist-change
   listener (`servoyModel.addPersistChangeListener(true, ...)`, around line 887) fires on
   every editor save and calls:

   ```java
   IDebugClientHandler dch = getDebugClientHandler();
   dch.refreshDebugClients(changes);
   ```

2. **The fan-out.** `DebugClientHandler.refreshDebugClients(Collection<IPersist>)`
   (`servoy_debug/.../DebugClientHandler.java`) fans the change out to a **fixed set of
   debug-client fields it owns** — `debugJ2DBClient`, `debugNGClient`,
   `debugDeveloperNGClient`, `debugHeadlessClient`, `debugAuthenticator` and any
   `customDebugClients` — each via `runInClientEventThread(client, cl -> cl.refreshPersists(changes))`,
   which posts the refresh onto that client's own event thread. `FormPreviewNGClient` is
   **not** one of these fields and is not added to `customDebugClients`, so it never
   receives the call.

3. **The refresh implementation is debug-only.** `refreshPersists(Collection<IPersist>)`
   is declared on `IDebugClient` and implemented on `DebugNGClient` (in `servoy_debug`).
   It uses `DebugUtils.getScopesAndFormsToReload(this, changes)` to compute which forms and
   scopes are affected, reloads flattened forms, clears cached form elements, re-inits the
   affected `WebFormController`s and finally triggers a client `"reload"` service call so
   the browser re-renders. Plain `NGClient` (and therefore `FormPreviewNGClient`) has **no
   `refreshPersists` and no equivalent reload machinery** — searching the whole workspace,
   the only `refreshPersists(Collection<IPersist>)` on any NG client is `DebugNGClient`'s.

4. **Confirmed class shape.** `FormPreviewNGClient extends NGClient` and implements only
   the plain NG/`IApplication` interfaces — it is **not** an `IDebugClient`/`IDebugNGClient`
   and is not registered with the `DebugClientHandler`. Its only registration is the
   private static singleton `instance` used by the SVY-21323/21496 recycle logic and by the
   Cypress test runner.

So the preview client is simply **outside the editor-change notification pipeline**. This
is a genuine functional gap, not user misconfiguration and not a bug elsewhere.

Secondary consideration surfaced during investigation: `DebugNGClient.refreshPersists`
depends on debug-only collaborators (`RemoteDebugScriptEngine.recompileScriptCalculation`,
`DebugSwingFormMananger`, debug script-engine scope handling, etc. inside
`DebugUtils.getScopesAndFormsToReload`). Not all of that is meaningful for a lightweight
preview client, but the **form-reload portion** (recompute affected form controllers →
re-init form UI → client `reload`) is exactly what the preview needs and is the part that
makes the browser update.

## Ticket premise check

The ticket's premise ("make the preview client refresh like the regular NG client") is
**correct in intent**, but the naive reading ("just make it behave like the debug NG
client") needs qualification:

- The debug NG client refreshes because it is **registered with and driven by**
  `DebugClientHandler`, and because it **is** an `IDebugClient` implementing the full
  `refreshPersists` reload path. `FormPreviewNGClient` is deliberately a *lightweight*
  client (skips auth, skips debugger, shows one target form) — turning it into a full
  `IDebugClient` or registering it as a `customDebugClient` would pull in debug-only
  behaviour and broaden its blast radius well beyond the preview use case.
- The real requirement is narrower: **when the editor saves a change that affects the
  previewed form, the preview browser should re-render that form.** That does not require
  the preview client to become a debug client — it requires it to (a) learn about the
  change and (b) run a reload for the affected form(s) on its own event thread.

So the premise is accepted, but the design must avoid the over-broad "make it a debug
client" interpretation.

## Approaches considered

1. **Deliver the existing persist-change broadcast to the preview client too, and give
   the preview client a scoped reload.** Extend the notification path so
   `FormPreviewNGClient`'s live instance also gets the change set, and implement a
   preview-scoped refresh that reuses the shared `DebugUtils.getScopesAndFormsToReload`
   form-reload logic (or the minimal subset of it) to re-init the affected form
   controllers and trigger the browser `reload`, run on the preview client's own event
   thread (the same `runOnEventThread` seam SVY-21496 already added).
   - Pros: directly closes the notification gap; reuses the proven debug reload machinery
     for the form part; keeps the preview client lightweight (no `IDebugClient`); runs on
     the correct event thread (no DAL assertion risk, consistent with SVY-21496).
   - Cons: `DebugUtils`/`refreshPersists` live in `servoy_debug`; wiring the preview client
     (in `com.servoy.eclipse.ngclient`, which does not require `servoy_debug`) into that
     path needs a clean seam — e.g. hook in `com.servoy.eclipse.core.Activator` (which
     already depends on `servoy_debug`, `servoy_ngclient` and `com.servoy.eclipse.ngclient`)
     rather than adding a `servoy_debug` dependency to the ngclient bundle. Needs care to
     only reload forms actually affected/loaded in the preview.

2. **Register `FormPreviewNGClient` as a `customDebugClient` on `DebugClientHandler`.**
   Then `refreshDebugClients` would fan out to it automatically.
   - Pros: minimal notification wiring — reuses the existing fan-out loop.
   - Cons: it still needs a working `refreshPersists`, which plain NG clients don't have,
     so this alone does not refresh anything. It also makes the preview client participate
     in *all* debug-client broadcasts (I18N, table, package changes, shutdown-on-new-debug
     recycle), broadening behaviour and coupling a lightweight client to debug lifecycle.
     Treats registration as the fix when the missing piece is the reload implementation.

3. **Make `FormPreviewNGClient` a full `IDebugClient`/subclass of `DebugNGClient`.**
   - Pros: inherits `refreshPersists` and the debug reload path for free; also inherits the
     `checkThatThisIsTheEventThread` `IDebugClient` bypass.
   - Cons: contradicts the deliberate "lightweight, no debugger, skip auth" design of
     `FormPreviewNGClient` (SVY-21173); pulls in script-engine/debug-frame/output-to-
     debugger behaviour that the preview neither wants nor uses; large blast radius for a
     preview-only feature. Over-engineered for the requirement.

4. **Re-show / retarget the form on save (reuse the SVY-21496 `retarget` path).** On an
   editor save affecting the previewed form, call the existing
   `retarget(formName)` → `showFormInMainPanel` on the preview client's event thread.
   - Pros: tiny, reuses code just added; runs on the right thread.
   - Cons: `showFormInMainPanel` re-shows an *already cached* form controller; without
     first clearing the cached form elements / re-initing the form UI (what
     `refreshPersists`/`refreshForms` do) it likely will **not** pick up the design change —
     so on its own it probably does not actually refresh the changed content. Would at best
     be a partial fix and needs the reload logic from approach #1 anyway.

5. **No code change.** Treat the manual browser reload as acceptable.
   - Pros: nothing to build.
   - Cons: fails the explicit acceptance expectation ("refresh like the regular NG
     client"); the preview is materially less useful than the debug client for iterating on
     a form. Not acceptable as a resolution.

## Recommendation

**PROCEED with approach #1** — deliver editor changes to the live `FormPreviewNGClient`
and give it a **scoped, preview-only refresh** that reuses the shared
`DebugUtils.getScopesAndFormsToReload` form-reload logic (re-init affected form
controllers + client `reload`), executed on the preview client's own event dispatch
thread (the `runOnEventThread` seam already introduced by SVY-21496).

Key design constraints for the PM/coder phase:
- **Keep the preview client lightweight** — do NOT turn it into an `IDebugClient` or a
  `DebugNGClient` subclass (rejects #3), and do NOT blanket-register it for all
  debug-client broadcasts (rejects #2 as the sole fix).
- **Hook the notification where the dependencies already line up.**
  `com.servoy.eclipse.core.Activator` already fires the persist-change listener and
  already requires both `servoy_debug` and `com.servoy.eclipse.ngclient`, so it is the
  natural place to also notify the preview singleton (via
  `FormPreviewNGClient.getInstance()`), avoiding a new `servoy_debug` dependency in the
  lightweight ngclient bundle.
- **Reuse, don't re-implement, the form-reload logic.** The form-affecting subset of
  `DebugUtils.getScopesAndFormsToReload` + the `DebugNGClient.refreshForms` re-init/reload
  sequence is what makes the browser update; the preview refresh should reuse that rather
  than hand-roll a parallel implementation. The script-engine/scope-reload and other
  debug-only branches are not needed for the preview.
- **Thread affinity.** Run the refresh on the preview client's event thread (as SVY-21496
  established) so the `DataAdapterList.checkThatThisIsTheEventThread()` assertion is never
  tripped.
- **Scope to loaded/affected forms only** so an unrelated save doesn't force a full reload.

Approach #4 (retarget-only) is not sufficient on its own but its event-thread routing is
reused by #1. Approaches #2, #3 and #5 are rejected for the reasons above.

## Git history findings

- `FormPreviewNGClient` was introduced by **c3d2cb399c** (*SVY-21173 Create headless
  Cypress form test runner application [ai]*) as a deliberately lightweight NG client
  (skips auth, skips debugger, shows a single target form). Refreshing on editor changes
  was never part of its design — this is a genuine feature gap, not a regression.
- **9610799716** (*SVY-21323 reuse single FormPreviewNGClient instance and fix shutdown*)
  added the static singleton `instance` and the recycle logic.
- **4cac16b** (*SVY-21496 Form opened in browser throws IDE error [ai]*) added the
  reuse-and-retarget path plus the `runOnEventThread`/`runRouting` and
  `shutdownOnEventThread`/`shutdownRouting` event-thread seams. SVY-21496's `retarget` and
  `runOnEventThread` are directly reusable building blocks for this fix, and its lesson —
  always run form-affecting work on the preview client's own event thread — must be
  honoured here.
- The editor-change → client-refresh pipeline (`com.servoy.eclipse.core.Activator`
  persist-change listener → `DebugClientHandler.refreshDebugClients` →
  `DebugNGClient.refreshPersists` → `DebugUtils.getScopesAndFormsToReload` /
  `refreshForms`) predates these and is the mechanism the preview client must be plugged
  into (in a scoped, lightweight way).
