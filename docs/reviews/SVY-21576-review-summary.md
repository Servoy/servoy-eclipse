# Review Summary — SVY-21576: getFormLayout MCP tool appears to not include form-level css/less

**Risk: ELEVATED** — Saving the form's Less tab while the form designer has unsaved changes very likely reverts those changes and re-adds unsaved deletions. This was confirmed by code reading, not by running the IDE.

**Reviewed scope:** `servoy-eclipse` commit `3e88c32d45` (SVY-21576 refresh form .less preview in designer after save [ai]), on `master` and `release`, not on `lts_2026`. Fix Version 2026.12 matches master.

## Why it is ELEVATED

The fix widens the SVY-21249 own-file guard in `ServoyModel.handleChangedFiles` so a changed `forms/<form>.less` takes the full `updateEditingPersist(form, true)` path. That path overwrites the editing form from disk.

`VisualFormEditor.doSave` writes the Less editor first and the form second. The `.less` write drains the POST_CHANGE listener synchronously, so the overwrite most likely runs before the designer state is saved. The same data-loss pattern was fixed for `.js` re-reads by SVY-21249 and SVY-21697.

## Manual test plan

**Verifying the fix**
1. Open a form with a form-level `.less`. Add a rule such as `.smoke-test-label { background-color: red; }` for an existing label. Save from the Less tab only. The design preview should show red without reopening the form.
2. With the form open, call the `getFormLayout` MCP tool. The label's computed style should be red.
3. Change the colour and save again. The preview should update each time.

**Regression checks**
1. Unsaved designer edits plus Less save (key check).
   - Move a component, delete another, and add a new one in the designer, without saving.
   - Edit a rule in the Less tab and press Ctrl+S once.
   - Verify the move, the deletion and the new element are all still there, and that the saved `.frm` has them. Repeat with Save All.
2. Unsaved property edit plus Less save. Change a form property in the Properties view without saving, then save the Less tab. The property value should survive.
3. External `.less` change. With an unsaved designer edit, change `forms/<form>.less` outside the IDE (editor or `git checkout`) and refresh. Check whether the designer edit survives.
4. SVY-21249 flow. Assign `onAction` via "OK & Show" without saving the form, then Save All. The handler must stay assigned.
5. SVY-21697 flow. Delete a component without saving, then create a form variable (this writes the form `.js`). The component must stay deleted.
6. Save the Less tab with no content change. The form should not be marked changed.
7. Open a form that extends the edited form. Check whether its preview picks up the change.

**Automated checks:** `ServoyModelOwnFileChangedTest` in `com.servoy.eclipse.core.tests` (plainjunit). A full `mvn clean verify` confirms the new module wiring. Neither was run during the review.

**Surfaces to cover:** the Servoy Developer form editor (Design and Less tabs) and the `getFormLayout` MCP tool. The NG client, runtime and Servoy-Copilot bundle need none.

## Possible improvements / follow-ups

- A narrower fix may be safer: refresh only the editing form's `formCss` (for example `editingForm.setFormCss(form.getFormCss())` plus adding it to `changedEditing`) instead of `updateEditingPersist(form, true)`. This was the triage's alternative approach.
- Any `.less`-only change now triggers the full overwrite, including a VCS update, an external editor, or `AISolutionGenerator` writing the file.
- Only the path helper is unit tested. Nothing exercises `handleChangedFiles` end to end, the `changedEditing` hand-off, or the unsaved-state scenario.
- The commit's AGENTS.md note says the PDE launcher reported no results and the author verified by reflection. Confirm the new test runs in CI.
- Spec §7 is unanswered: can a form stylesheet ever be serialised as `.css`? The helper handles only `.less`.
- Unverified: refresh of forms that extend the edited form, and behaviour when a `.less` file is deleted.
- Unverified: whether the reporter's `customer_detail` symptom, on a freshly opened solution with no Less-tab save, has the same cause.
- `AGENTS.md` "Project Overview" is stale (2026.6.0-SNAPSHOT / Tycho 4.0.12, versus 2026.12.0-SNAPSHOT / Tycho 5.0.3 in the pom).
