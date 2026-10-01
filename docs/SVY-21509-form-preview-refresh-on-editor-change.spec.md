# Spec: SVY-21509 — Form opened in browser (Show Form in Browser) does not refresh after a change in the form editor

## 1. Goal

When a form is opened in the browser through the form editor's **Show Form in Browser**
action (the `?formpreview=<formName>` / `FormPreviewNGClient` flow), a change made and
saved afterwards in the form editor must be reflected automatically in the browser
preview — the same way the regular debug NG client already refreshes on editor changes.

Today the preview browser only updates on a manual reload. This spec closes that gap by
delivering the existing editor persist-change notification to the live
`FormPreviewNGClient` and running a **scoped, preview-only refresh** that reuses the proven
debug form-reload logic, executed on the preview client's own event dispatch thread.

The fix must keep `FormPreviewNGClient` **lightweight** — it must NOT become an
`IDebugClient`, must NOT subclass `DebugNGClient`, and must NOT be registered for the full
set of debug-client broadcasts.

## 2. Background

### 2.1 The editor-change → client-refresh notification pipeline

Editor saves reach running clients through a persist-change broadcast:

1. `com.servoy.eclipse.core.Activator` registers a persist-change listener
   (`servoyModel.addPersistChangeListener(true, ...)`, around line 885). On every editor
   save its `persistChanges(Collection<IPersist> changes)` runs (wrapped in
   `UIUtils.invokeLaterOnAWT`) and finally calls:

   ```java
   IDebugClientHandler dch = getDebugClientHandler();
   dch.refreshDebugClients(changes);
   ```

2. `DebugClientHandler.refreshDebugClients(Collection<IPersist>)`
   (`servoy_debug/.../DebugClientHandler.java`, lines 155–166) fans the change set out to a
   **fixed set of debug-client fields it owns** — `debugJ2DBClient`, `debugNGClient`,
   `debugDeveloperNGClient`, `debugHeadlessClient`, `debugAuthenticator` — plus any
   registered `customDebugClients`, each via:

   ```java
   runInClientEventThread(client, cl -> cl.refreshPersists(changes));
   ```

   `runInClientEventThread` (lines 168–175) posts the refresh onto that client's own event
   thread via `debugClient.invokeLater(...)` and guards on `isShutDown()`.

3. `refreshPersists(Collection<IPersist>)` is declared on `IDebugClient` and implemented on
   `DebugNGClient` (`servoy_debug`, lines 346–392). It:
   - flushes the solution-model form-element cache on the solution copy;
   - calls `DebugUtils.getScopesAndFormsToReload(this, changes)` to compute
     `Set<IFormController>[] { scopesToReload, formsToReload }`;
   - `reload()`s any affected `FlattenedForm`s;
   - decides `forcePageReload` (e.g. a solution `.css`/`.less` Media change);
   - calls the private `refreshForms(formsToReload, forcePageReload)`;
   - reloads affected form scopes.

4. `DebugNGClient.refreshForms(Collection<IFormController>, boolean)` (private, lines
   278–344) is the part that actually makes the browser re-render:
   - clears cached form elements on every cached `WebFormUI`
     (`clearCachedFormElements()`);
   - for each affected controller: `notifyVisible(false)`, re-init the form UI
     (`((WebFormController)controller).initFormUI()`), re-run `onLoad` if visible, then
     `notifyVisible(true)` (with a datasource-change destroy/recreate branch);
   - finally issues a client `"reload"` async service call on the window service
     (`NGRuntimeWindowManager.WINDOW_SERVICE`) through
     `NGClientWebsocketSessionWindows` and flushes, so the browser re-renders.

### 2.2 Why the preview client is excluded

`FormPreviewNGClient extends NGClient` (`com.servoy.eclipse.ngclient.startup`) and
implements only the plain NG / `IApplication` interfaces. Confirmed by inspection:

- It is **not** an `IDebugClient`/`IDebugNGClient` and is **not** registered with the
  `DebugClientHandler` (not one of its fields, not in `customDebugClients`), so
  `refreshDebugClients` never fans out to it.
- Plain `NGClient` (and therefore `FormPreviewNGClient`) has **no `refreshPersists`** and
  no equivalent reload machinery. Searching the whole workspace, the only
  `refreshPersists(Collection<IPersist>)` on any NG client is `DebugNGClient`'s.

So the preview client is simply outside the editor-change notification pipeline. This is a
genuine feature gap (see §3.4 git history), not a regression or misconfiguration.

The preview client is deliberately lightweight (SVY-21173): it skips auth, skips the
debugger, and shows a single target form. Turning it into a full `IDebugClient` or a
`customDebugClient` would pull in debug-only behaviour (script-engine/debug-frame handling,
I18N/table/package broadcasts, shutdown-on-new-debug recycle) far beyond the preview use
case. The narrower requirement is: **when an editor save affects the previewed form,
re-render that form on the preview client's own event thread.**

### 2.3 The SVY-21496 event-thread seams

SVY-21496 (commit `4cac16b`) established that form-affecting work on the preview client
must run on the preview client's own event dispatch thread, and added reusable seams for
exactly that:

- `FormPreviewNGClient.runOnEventThread(Runnable)` (private, lines 118–134): resolves this
  client's `IEventDispatcher` from `getWebsocketSession().getEventDispatcher(false)` and
  delegates to `runRouting`.
- `FormPreviewNGClient.runRouting(IEventDispatcher, Runnable)` (package-private, lines
  136–161): if the dispatcher is live and we are not already on its event thread, posts via
  `dispatcher.addEvent(action)`; otherwise runs the action directly on the calling thread.
- These already back `retarget(String)` (lines 101–116), which runs
  `((NGFormManager)getFormManager()).showFormInMainPanel(formName)` on the event thread.

Running the refresh through this seam keeps thread affinity correct and avoids tripping
`DataAdapterList.checkThatThisIsTheEventThread()` — the assertion SVY-21496 was fixing.

## 3. Design

Approach #1 from the triage (approved): deliver the persist-change set to the live
`FormPreviewNGClient` from `com.servoy.eclipse.core.Activator` (where the dependencies
already line up), and give the preview client a scoped refresh that reuses the shared
`DebugUtils.getScopesAndFormsToReload` + `DebugNGClient` form-reload sequence, executed on
the preview client's own event dispatch thread and scoped to loaded/affected forms only.

### 3.1 Notification delivery to the preview client

Hook the notification in `com.servoy.eclipse.core.Activator`'s existing persist-change
listener (the one at ~line 885 that already calls `dch.refreshDebugClients(changes)`),
**not** by adding a `servoy_debug` dependency to the lightweight ngclient bundle and
**not** by registering the preview client with `DebugClientHandler`.

`com.servoy.eclipse.core` already requires all three needed bundles (`servoy_debug`,
`servoy_ngclient`, `com.servoy.eclipse.ngclient` — confirmed in its `MANIFEST.MF`), so it
is the natural seam. In the same `persistChanges` body, after
`dch.refreshDebugClients(changes)`, also notify the preview singleton:

```java
FormPreviewNGClient preview = FormPreviewNGClient.getInstance();
if (preview != null)
{
    preview.refreshPreview(changes); // new lightweight, event-thread-routed refresh
}
```

`FormPreviewNGClient.getInstance()` already exists (returns the active singleton or null).
The new `refreshPreview(Collection<IPersist>)` method lives on `FormPreviewNGClient` and
performs the scoped refresh (§3.2) on the client's own event thread (§3.3). Note this is a
*new preview-specific method*, deliberately NOT the `IDebugClient.refreshPersists` contract
— the preview client stays a plain `NGClient`.

Because the notification runs inside the existing `UIUtils.invokeLaterOnAWT` block, the
`refreshPreview` call itself must route the work onto the preview client's event thread
(it will not already be on it) — see §3.3.

### 3.2 Scoped preview refresh reusing the shared form-reload logic

The refresh must reuse `DebugUtils.getScopesAndFormsToReload(clientState, changes)` and the
`DebugNGClient.refreshForms(...)` re-init/reload sequence rather than hand-rolling a
parallel implementation. Two integration options — the coder should pick based on how much
of `getScopesAndFormsToReload` is safe for a non-debug client:

- **Preferred — extract a shared form-reload helper in `servoy_debug`.**
  `DebugNGClient.refreshForms(Collection<IFormController>, boolean)` is currently `private`.
  Extract the form-reload body into a reusable static helper (e.g.
  `DebugUtils.reloadForms(NGClient client, Collection<IFormController> forms, boolean
  forcePageReload)`), have `DebugNGClient.refreshForms` delegate to it (behaviour
  unchanged), and call the same helper from the preview refresh. This guarantees both paths
  run identical form-reload code and avoids drift. `FormPreviewNGClient.refreshPreview`
  then does the `DebugNGClient.refreshPersists`-equivalent orchestration itself (flush
  form-element cache on the solution copy → `getScopesAndFormsToReload(this, changes)` →
  `FlattenedForm.reload()` for affected flattened forms → `DebugUtils.reloadForms(this,
  formsToReload, forcePageReload)`), reloading form scopes only if meaningful for the
  preview.

- **Alternative — add a shared static orchestrator in `servoy_debug`** that takes an
  `NGClient` (or `ClientState`) + `changes` and runs the whole "compute affected + reload
  forms + client reload" sequence, called by both `DebugNGClient.refreshPersists` and
  `FormPreviewNGClient.refreshPreview`. This centralises even more but requires care that
  the debug-only branches inside `getScopesAndFormsToReload` remain guarded.

Either way, the wiring lives in `servoy_debug` / `com.servoy.eclipse.core` — **do not add a
`servoy_debug` dependency to `com.servoy.eclipse.ngclient`.** Since `refreshPreview` needs
to call `servoy_debug` code (`DebugUtils`), the orchestration that touches `DebugUtils`
should live on the `servoy_debug` side and be invoked from `Activator`, with
`FormPreviewNGClient` exposing only the plain-NG building blocks it already has
(event-thread routing, `getWebsocketSession()`, `getFormManager()`). Concretely, prefer:

- `Activator.persistChanges` calls a new `servoy_debug` entry point, e.g.
  `DebugUtils.refreshFormPreview(FormPreviewNGClient preview, Collection<IPersist> changes)`
  (or a method on `DebugClientHandler`), which does the `getScopesAndFormsToReload` +
  `reloadForms` orchestration and routes it via the preview client's event-thread seam
  (§3.3). This keeps the `DebugUtils` dependency inside bundles that already require
  `servoy_debug` and leaves `FormPreviewNGClient` free of any debug dependency.

**Scope to loaded/affected forms only.** `getScopesAndFormsToReload` already returns only
the cached/affected form controllers (it queries
`getFormManager().getCachedFormControllers(...)`), so an unrelated save that touches no
loaded form yields an empty `formsToReload` and therefore no client reload. Preserve that:
do not force a full reload for unrelated changes. The `forcePageReload` for solution
CSS/LESS Media changes (from `refreshPersists`) may be carried over if the previewed form
uses solution styling; treat broader debug-only reload triggers as out of scope unless
they affect the previewed form.

The **debug-only branches** of `getScopesAndFormsToReload` (script-engine scope handling,
`RemoteDebugScriptEngine.recompileScriptCalculation`, `DebugSwingFormMananger`,
`DebugJ2DBClient` menu fill, etc.) are gated on `clientState instanceof DebugJ2DBClient` /
debug engine types and will simply not fire for a plain `NGClient`/`FormPreviewNGClient` —
so passing the preview client as the `ClientState` is expected to compute the correct
form set without executing debug-only work. The coder must verify this holds for the
preview client's script engine type (see §7 open questions).

### 3.3 Thread affinity / event-thread execution

The refresh must run on the preview client's own event dispatch thread, reusing the
SVY-21496 seam:

- Route the whole compute-and-reload sequence through
  `FormPreviewNGClient.runOnEventThread(Runnable)` (or the package-private
  `runRouting(dispatcher, action)` used the same way `retarget` does).
- Since the `Activator` notification runs on the AWT thread (inside
  `UIUtils.invokeLaterOnAWT`), it will not be on the preview client's event thread, so the
  action is posted via `IEventDispatcher.addEvent(...)` — fire-and-forget, consistent with
  `retarget`.
- `runInClientEventThread` in `DebugClientHandler` guards on `isShutDown()`; the preview
  path must equivalently be safe if the client shut down between notification and
  execution (the `runRouting`/dispatcher-null path already degrades gracefully; also
  re-check the singleton/`getWebsocketSession()` before touching form state).

This keeps `DataAdapterList.checkThatThisIsTheEventThread()` from tripping and honours the
SVY-21496 lesson.

### 3.4 Git history

- `FormPreviewNGClient` was introduced by **c3d2cb399c** (*SVY-21173 Create headless
  Cypress form test runner application [ai]*) as a deliberately lightweight NG client
  (skips auth, skips debugger, shows a single target form). Refreshing on editor changes
  was never part of its design — this ticket is a genuine feature gap, not a regression.
- **9610799716** (*SVY-21323 reuse single FormPreviewNGClient instance and fix shutdown*)
  added the static singleton `instance` and the recycle logic, and the `getInstance()`
  accessor this fix reuses to reach the live preview client.
- **4cac16b** (*SVY-21496 Form opened in browser throws IDE error [ai]*) added the
  reuse-and-retarget path plus the `runOnEventThread`/`runRouting` and
  `shutdownOnEventThread`/`shutdownRouting` event-thread seams. Its `retarget` and
  `runOnEventThread` are the direct building blocks for this fix, and its lesson — always
  run form-affecting work on the preview client's own event thread — must be honoured.
- The editor-change → client-refresh pipeline
  (`Activator` persist-change listener → `DebugClientHandler.refreshDebugClients` →
  `DebugNGClient.refreshPersists` → `DebugUtils.getScopesAndFormsToReload` / `refreshForms`)
  predates these and is the mechanism the preview client is now plugged into, in a scoped,
  lightweight way.

## 4. Implementation plan

Ordered, concrete task list for the coder:

1. **`servoy_debug` — extract a reusable form-reload helper.**
   In `com.servoy.j2db.debug.DebugNGClient`, move the body of the private
   `refreshForms(Collection<IFormController>, boolean)` (lines 278–344) into a new static
   helper — recommended `DebugUtils.reloadForms(NGClient client, Collection<IFormController>
   forms, boolean forcePageReload)` (in `com.servoy.j2db.debug.DebugUtils`) — and have
   `DebugNGClient.refreshForms` delegate to it. Behaviour must be byte-for-byte equivalent
   (clear cached form elements → notifyVisible(false) → initFormUI/datasource-recreate →
   forceExecuteOnLoadMethod if visible → notifyVisible(true) → client `"reload"` async
   service call + flush).

2. **`servoy_debug` — add the preview orchestrator.**
   Add a new entry point (recommended `DebugUtils.refreshFormPreview(FormPreviewNGClient
   preview, Collection<IPersist> changes)`, or a method on `DebugClientHandler`) that
   mirrors `DebugNGClient.refreshPersists`'s form-affecting subset for a plain preview
   client:
   - bail if the preview client is null or shut down;
   - flush the solution-model form-element cache on the solution copy (as
     `refreshPersists` does);
   - `Set<IFormController>[] r = DebugUtils.getScopesAndFormsToReload(preview, changes);`
   - `reload()` any affected `FlattenedForm`s in `r[1]`;
   - compute `forcePageReload` (solution CSS/LESS Media change) as in `refreshPersists`,
     scoped to the previewed form's styling;
   - call `DebugUtils.reloadForms(preview, r[1], forcePageReload)`;
   - reload affected form scopes in `r[0]` only if meaningful for the preview.
   Wrap the whole sequence so it executes on the preview client's event thread via the
   preview's `runOnEventThread`/`runRouting` seam (§3.3). If `FormPreviewNGClient` does not
   expose a public event-thread router, add a minimal public
   `runOnEventThread(Runnable)`/`runPreviewRefresh(Runnable)` on `FormPreviewNGClient` (it
   already has the private `runOnEventThread` and package-private `runRouting`).

3. **`com.servoy.eclipse.core.Activator` — deliver the notification.**
   In the persist-change listener at ~line 885, immediately after
   `dch.refreshDebugClients(changes);` (line 924), add:
   ```java
   FormPreviewNGClient preview = FormPreviewNGClient.getInstance();
   if (preview != null)
   {
       DebugUtils.refreshFormPreview(preview, changes); // event-thread-routed inside
   }
   ```
   (or call the `DebugClientHandler` method if that placement is chosen in step 2). No new
   bundle dependency is needed — `com.servoy.eclipse.core` already requires `servoy_debug`,
   `servoy_ngclient` and `com.servoy.eclipse.ngclient`.

4. **`FormPreviewNGClient` — expose the event-thread seam if needed.**
   If step 2's orchestrator lives outside the class, add a public router (e.g.
   `public void runPreviewRefresh(Runnable action) { runOnEventThread(action); }`) so the
   `servoy_debug` orchestrator can post onto the preview client's dispatcher without
   duplicating the dispatcher-resolution logic. Do NOT make `FormPreviewNGClient` implement
   `IDebugClient` or `refreshPersists`.

5. **Verify + build.**
   Run `eclipse-ide_getCompilationErrors` on `servoy_debug`, `com.servoy.eclipse.core`, and
   `com.servoy.eclipse.ngclient` after edits; fix any errors; `organizeImports` and
   `formatFile` on changed files. Address the two highest Spotbugs severities on changed
   code.

## 5. Acceptance criteria

1. **Preview refreshes on a relevant editor save.** With a form open in the editor and
   previewed in the browser via *Show Form in Browser*, making and saving a change to that
   form (e.g. moving/renaming an element, changing a property) updates the browser preview
   automatically, without a manual reload — the same behaviour as the regular debug NG
   client.
2. **Unrelated saves do not force a full reload.** Saving a change that does not affect any
   form loaded in the preview client produces no client `"reload"` (empty `formsToReload`),
   so the preview is not needlessly reloaded.
3. **Preview client stays lightweight.** `FormPreviewNGClient` remains a plain `NGClient`:
   it does NOT implement `IDebugClient`/`IDebugNGClient`, does NOT subclass `DebugNGClient`,
   and is NOT registered with `DebugClientHandler` (not a field, not a `customDebugClient`).
   It does not participate in I18N/table/package/debug-recycle broadcasts.
4. **Runs on the preview client's own event thread.** The refresh is dispatched via the
   SVY-21496 `runOnEventThread`/`runRouting` seam;
   `DataAdapterList.checkThatThisIsTheEventThread()` is never tripped by this path.
5. **Shared form-reload logic is reused, not duplicated.** The preview refresh calls the
   same extracted form-reload helper (`DebugUtils.reloadForms` or equivalent) that
   `DebugNGClient.refreshForms` now delegates to; there is no second hand-rolled
   re-init/reload implementation. `DebugNGClient` behaviour is unchanged (regular debug NG
   client still refreshes exactly as before).
6. **No new bundle coupling.** `com.servoy.eclipse.ngclient` gains NO dependency on
   `servoy_debug`; the debug wiring stays in `com.servoy.eclipse.core` / `servoy_debug`.
7. **Compiles clean.** `servoy_debug`, `com.servoy.eclipse.core`, and
   `com.servoy.eclipse.ngclient` compile with no new errors; the two highest Spotbugs
   severities are clean on changed code.

## 6. Out of scope

- Turning `FormPreviewNGClient` into a debug client or registering it for the full
  debug-client broadcast set (I18N, table changes, package changes, shutdown-on-new-debug).
- Refreshing script-engine scopes, calculations, or other debug-only reload branches for
  the preview beyond what is needed to re-render the previewed form.
- Changes to the regular `DebugNGClient` refresh behaviour other than the pure extraction
  of the reusable form-reload helper (behaviour must stay identical).
- The `retarget`-on-save approach (triage approach #4) — insufficient on its own because
  `showFormInMainPanel` re-shows a cached controller without clearing cached form elements
  / re-initing the form UI.
- Any change to the `?formpreview=` URL flow, the singleton/recycle logic, or the Cypress
  test runner beyond wiring the new refresh.

## 7. Open questions

1. **Reuse boundary of `getScopesAndFormsToReload`.** The method mixes debug-only branches
   (script-engine scope/calculation handling gated on `DebugJ2DBClient`/debug engine types)
   with form-only branches. Confirm that passing `FormPreviewNGClient` as the `ClientState`
   computes the correct affected-form set without executing debug-only work — in particular
   that the preview client's script engine type does not enter the
   `RemoteDebugScriptEngine`/`ScriptCalculation` recompile branch. If it does, the coder
   should route only the form-computation subset (or guard it) rather than the whole method.
2. **Scope/valuelist/relation/menu/style changes for the preview.** Whether the preview
   needs to honour valuelist, relation, menu, and style changes (which `refreshForms`
   handles for the debug client) or only direct form/element changes. Default assumption:
   reuse the full form set from `getScopesAndFormsToReload` so these are handled uniformly;
   confirm none of them trigger debug-only side effects on the preview client.
3. **Form-scope reload (`r[0]`) for the preview.** Whether reloading form scopes
   (`controller.getFormScope().reload()`, done by `refreshPersists`) is meaningful/safe for
   the lightweight preview client, or whether the preview should reload form UI only. Lean
   toward mirroring the debug client unless it pulls in debug-only script-engine state.
4. **Placement of the orchestrator.** Whether the preview orchestrator belongs on
   `DebugUtils`, on `DebugClientHandler`, or as a small new method — all keep the
   `servoy_debug` dependency out of `com.servoy.eclipse.ngclient`; the choice is a code-
   organisation preference for the coder.
