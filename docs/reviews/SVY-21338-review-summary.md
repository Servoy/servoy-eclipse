# SVY-21338 — Peer-review summary

**Reset to default of the datasource of a form — refresh problem in Properties view**

## Risk verdict

**ELEVATED at first pass, resolved by follow-up.** The initial fix removed the GEF `Command` wrapper in `RetargetToEditorPersistProperties.updateProperty()` for all callers, which re-broke a previously-fixed bug at the code level — the SVY-19810 "Clear property" marker quick-fix path (and `RelatedTabController` / `PersistPropertySource` recursive reset) call `resetPropertyValue()` directly on the retargeting wrapper, so with the wrapper gone no command was pushed onto the editor's `CommandStack`. The refresh half (`asyncExec`→`syncExec`) was always clean; the undo half was the concern. Landing on the `release` branch raised the stakes.

## Resolution (follow-up commit `eb5a7039e6`)

The R1 finding was addressed by `eb5a7039e6` ("SVY-21338 keep quick-fix clear undoable after reset-property refresh fix [ai]"). It adds a `wrapInCommand` flag (default `true`) to `RetargetToEditorPersistProperties`:
- The **Properties-view path** (`DesignerPropertyAdapterFactory` `retargetToEditor` branch) now passes `wrapInCommand=false`, so the change is applied directly and the caller's own `Set/ResetValueCommand` remains the single command on the stack — no double-wrap.
- The **direct-caller path** (bare `IPersist`, including the quick-fix) keeps `wrapInCommand=true`, wrapping the change in a `Command` that now implements `undo()`/`redo()` (capturing and restoring the old value) and pushing it onto the editor's `CommandStack`, with a `cmd.execute()` fallback when there is no stack. This also closes a latent undo gap: SVY-19810's original command only implemented `execute()`.
- Testing seams `resolveEditor()` / `resolveCommandStack()` are extracted, and the integration test now drives the real `updateProperty()` against a GEF `CommandStack` — asserting set/reset push and are undoable/redoable, fall back to direct execute with no stack, and that `wrapInCommand=false` touches the stack not at all.

This resolves the headline regression while preserving the double-wrap fix for the Properties view. Manual test step 4 below (quick-fix clear stays dirty + undoable) remains the recommended runtime confirmation.

## Reviewed scope

`servoy-eclipse` on branch `release`:
- `c95f976e8e` — SVY-21338 Fix reset-property refresh and undo in Properties view [ai] (5 files, +331/-22)
- `7c6e6dec8a` — SVY-21338 Add unit and integration tests [ai] (3 files, +500/-0)

Production files: `com.servoy.eclipse.ui/.../property/RetargetToEditorPersistProperties.java`, `com.servoy.eclipse.designer/.../property/OpenEditorUndoablePropertySheetEntry.java`, `com.servoy.eclipse.designer/.../property/ServoyViewPropertySheetPageAdapterFactory.java`. Fix version 2026.9.0.

## Manual test plan

**Verifying the fix**
1. New form on `example_data->orders`. Select it in Solution Explorer. In Properties view, right-click `dataSource` → "Restore Default Value". Expect the value to switch to "- none -" immediately, with no re-selection needed.
2. With the form editor open, set a property from the Properties view, then Ctrl+Z (undo) and Ctrl+Y (redo). Expect the property to revert/reapply and the Properties view to repaint each time.
3. Reset a property from the Properties view, then Ctrl+Z. Expect the reset to be undone and shown.

**Regression checks**
4. **(R1 — top priority)** On a form, produce a validation marker whose quick-fix is "Clear property …" (an invalid/stale property). Run the quick-fix. Confirm (a) the form editor becomes **dirty** and (b) Ctrl+Z undoes the clear. If either fails, the SVY-19810 symptom is back. This is the decision-maker — it needs a running IDE and cannot be settled statically.
5. **(R2)** Persist selected in Solution Explorer, Properties view focused, press Ctrl+Z with no prior property edit. Confirm it does not spuriously open a form editor and does not swallow an undo that should go to the active part.
6. **(R3)** Edit a property from the Properties view with the form editor open; watch for visible flicker or sluggishness on a form with many properties (`refreshFromRoot()` can now fire twice — once from the synchronous platform path, once from the new `CommandStackListener`).
7. Repeat the set/reset + undo/redo from the **Form Hierarchy view** (also routed through `ServoyViewPropertySheetPageAdapterFactory`).

**Surfaces:** Properties view for any persist edited while selected in Solution Explorer or Form Hierarchy view — the `syncExec` and undo wiring apply to every property, not just `dataSource`. No change to the form editor's own property-editing path or to Eclipse platform code. No sibling repository touched.

## Possible improvements / follow-ups

- **R1 (potential regression):** confirm the "Clear property" quick-fix still dirties the editor and remains undoable after this change. If it regresses, the fix needs to keep the quick-fix / direct-caller path pushing a command onto the stack while still avoiding the double-wrap on the Properties-view path.
- **Dead GEF wiring:** the `org.eclipse.gef` imports in `RetargetToEditorPersistProperties` and the `Require-Bundle: org.eclipse.gef` added for SVY-19810 appear unused after this change — clean up or confirm intentional.
- **Documentation drift:** the committed spec (`docs/SVY-21338-reset-property-refresh.spec.md` §3.2/§4) and triage describe *keeping and fixing* the command and frame it as a one-token change in `com.servoy.eclipse.ui` only; the shipped fix *removes* the command and touches two `com.servoy.eclipse.designer` files. Update the docs to match the shipped code.
- **Test coverage:** the integration test proves `resetPropertyValue()` runs synchronously (`syncExec`) only. It does not exercise the actual UI refresh, the command-removal behaviour, or the designer-side undo/redo wiring (`resolveCommandStack`, `CommandStackListener`, action-bar handlers). Those ship without automated coverage.
- **Forward merge:** no structural conflict expected into `master`/`lts_*` (no API removed), but if `master` layered changes on SVY-19810's `Command` block the forward merge needs manual reconciliation, since this commit deletes it.

## Security

NONE. Local, developer-driven IDE Properties-view editing — no network, untrusted input, deserialization, auth/crypto surface, or data leaving the process. The `Import-Package: org.junit;version="4.0.0"` pin is test-fragment-only (not in the shipped product) and resolves against JUnit 4 already in the target platform.
