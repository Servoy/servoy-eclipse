# Spec: SVY-21249 (v2) — Save All doesn't persist form editor changes when assigning an event handler

> This supersedes the original SVY-21249 spec. The first attempt fixed the symptom in the form
> editor (`doSave()` undo+redo re-apply) but introduced a regression: it corrupted grid/component
> columns configured via the wizard, because those changes use non-idempotent commands that must
> not be re-executed. This version fixes the **root cause** in the model layer instead.

## 1. Goal

When a user assigns an event handler (e.g. onAction) to a component via "OK & Show", the form's
unsaved in-memory change (the event handler property) must NOT be silently discarded. After pressing
"Save All", the handler must be persisted to disk and work at runtime, without requiring a manual
re-save of the form editor. The fix must not corrupt other component configuration (e.g. grid columns
created via the property wizard).

## 2. Root cause

### 2.1 Reproduction

1. Open a form (clean, not dirty).
2. Select a component, go to Properties, assign `onAction` → popup → create a new method → **OK & Show**.
3. At that moment: the onAction property is set on the editing form persist (form becomes dirty via the
   command stack), the `ScriptMethod` is created, and its `.js` file is written to disk.
4. Press **Save All**.
5. Run the solution → the onAction does NOT fire (the form was saved without the handler).

### 2.2 The destructive sequence (confirmed via debugger)

Writing the new method's `.js` file at OK & Show synchronously triggers:

```
MethodDialog.open()  ("OK & Show")
  → NewMethodAction.createNewMethod()
    → JavaScriptFilePostSaveListener.saved()          // the .js was saved to disk
      → ServoyModel.handleChangedFiles()
        → SolutionDeserializer.updateSolution()        // reads the .js, adds ScriptMethod to REAL form
        → solution.acceptVisitor()
          → visit(Form)   // Form.isChanged() == true (it gained a ScriptMethod child)
            → ServoyProject.updateEditingPersist(form, true)   // FULL composite overwrite
```

`updateEditingPersist(form, true)` overwrites the **editing** form from the **real** solution. The real
form was just re-read for the `.js`, but its `.frm` file on disk has NO onAction (never saved). So the
full overwrite **wipes the unsaved onAction** from the editing persist. The command stack still holds the
`SetValueCommand`, so the editor still shows dirty (`*`), but the editing persist no longer has the
handler. A subsequent `doSave()` then writes the form without it.

### 2.3 Why the Form is flagged changed

In `SolutionDeserializer`, only a persist whose own file carries the `changed` JSON attribute is flagged
(`handleChanged`, `deserializePersist`). When the `.js` is read, only the `ScriptMethod` is flagged. The
Form becomes `isChanged()` purely as a side effect of gaining the child (`AbstractBase.addChild` →
`flagChanged`). The Form's own `.frm` file was NOT re-read and did NOT change. This distinction is the
key to the fix.

## 3. Design

### 3.1 Model-layer selective merge (the fix)

In `ServoyModel.handleChangedFiles()`, when the persist-tree visitor encounters a composite
(`SolutionSerializer.isCompositeWithItems(persist)` — e.g. Form, TableNode) that is flagged changed,
determine whether the composite's **own file** is among the files that actually changed on disk:

- **Own file DID change** (e.g. an external `.frm` edit / git checkout): keep existing behavior — do the
  full `updateEditingPersist(persist, true)` overwrite.
- **Own file did NOT change** (only a child script `.js` changed/was added): do **NOT** overwrite the whole
  editing composite. The changed script child is still handled by the existing `IScriptElement` path
  (`changedScriptElements`), so the new method is merged into the editing solution, while the editing
  form's other in-memory state (the unsaved onAction, layout, etc.) is left intact.

Detection: build a set of absolute paths of the actually-changed files, and compare against the
composite's own file path computed via `SolutionSerializer.getRelativeFilePath(persist, false)` resolved
against the project location.

```java
final Set<String> changedFilePaths = new HashSet<String>();
final File solutionLocationDir = project.getLocation().toFile();
for (File changedFile : changedFiles) changedFilePaths.add(changedFile.getAbsolutePath());

// inside visit(persist), when persist.isChanged():
boolean compositeWithItems = SolutionSerializer.isCompositeWithItems(persist);
boolean ownFileChanged = true;
if (compositeWithItems) {
    String ownFilePath = new File(solutionLocationDir,
        SolutionSerializer.getRelativeFilePath(persist, false)).getAbsolutePath();
    ownFileChanged = changedFilePaths.contains(ownFilePath);
}
if (compositeWithItems && !ownFileChanged) {
    // only a child script element changed; do not overwrite the whole editing composite
    persist.clearChanged();
    changed.put(persist.getUUID(), persist);
} else {
    IPersist editingPersist = servoyProject.updateEditingPersist(persist, compositeWithItems);
    persist.clearChanged();
    changed.put(persist.getUUID(), persist);
    changedEditing.put(persist.getUUID(), editingPersist);
}
```

### 3.2 Why this is correct and safe

- It precisely encodes the rule: *don't overwrite an editing composite from disk when that composite's own
  file didn't change.* The only thing that changed on disk is the script child, which is merged via its own
  path.
- It preserves ALL unsaved editor state on the form (not just onAction), because the form is never
  clobbered when only a script child changed.
- Legitimate external `.frm` changes (git checkout, external edit) still fully overwrite the editing form,
  because then the `.frm` IS in the changed-files set.
- No editor awareness, no command replay, no global resource-processing deferral, no counter to leak.

### 3.3 Revert the previous editor-side attempt

The first attempt in `BaseVisualFormEditor` must be fully reverted:
- Remove the undo+redo re-apply block in `doSave()` — it re-executed commands and corrupted
  non-idempotent wizard commands (`SetCustomArrayPropertiesCommand` for grid columns uses an incrementing
  id and in-place state mutation; re-running it loses/duplicates columns).
- Remove the `isSaving` field and its guard.
- Remove the `flagModified()` call added in `persistChanges()`.

These are no longer needed because the root cause is fixed at the model layer.

## 4. Implementation plan

1. **`ServoyModel.handleChangedFiles()`**: add the changed-file-path set and the composite own-file check
   as in §3.1.
2. **`BaseVisualFormEditor.java`**: revert all changes from the previous commit (undo/redo block,
   `isSaving`, `persistChanges` `flagModified`).
3. **Delete obsolete tests** from the previous attempt:
   - `VfeCommandStackEventListenerTest.java`
   - `BaseVisualFormEditorPersistChangesTest.java`
4. **Verify** both scenarios (see §5).

## 5. Acceptance criteria

- [ ] OK & Show to assign onAction → Save All → run → onAction fires; form shows no unsaved marker afterward.
- [ ] The newly created method still appears in the editing solution / Solution Explorer and is editable.
- [ ] Configuring grid/component columns via the property wizard → Save All → all columns are retained
      correctly (no loss, no duplication) — the previous regression is gone.
- [ ] An external `.frm` change (e.g. git checkout) on a NON-dirty form is still picked up (full overwrite
      still happens when the form's own file changed).
- [ ] Normal design-surface edits and undo/redo behavior are unchanged.

## 6. Out of scope

- Reordering Eclipse's Save All (not controllable across all trigger points; focus is usually in the
  script editor when Save All is pressed).
- The `dummyCommandStack` behavior of `VisualFormEditor.getAdapter(CommandStack.class)` (separate concern).
- A dirty-editor registry / `resourceChangesHandlerCounter` hold (considered but rejected: global deferral
  and counter-leak risk are worse than the targeted model-layer fix).

## 7. Notes / risks

- **Path comparison on Windows**: the fix compares absolute paths. If the guard ever "doesn't trigger",
  the path construction (`getRelativeFilePath` vs. the resource delta absolute paths, separators/casing) is
  the first suspect.
- **Script child merge**: confirmed the new method is still merged because the `ScriptMethod` is an
  `IScriptElement` visited independently; skipping the parent-form overwrite does not skip the script child.
