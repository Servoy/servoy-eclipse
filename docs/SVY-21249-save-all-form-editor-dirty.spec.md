# Spec: SVY-21249 — Save All button doesn't save changes on form editor

## 1. Goal

Ensure that "Save All" correctly persists form editor changes even when the form editor is not the active editor. When a property change (e.g., assigning an onAction event handler via "OK & Show") modifies the form's persist tree and then the script editor opens, pressing "Save All" must persist both the script and the form's property changes. The onAction must work at runtime without requiring a manual save of the form editor.

## 2. Background

### 2.1 Dirty-state tracking in BaseVisualFormEditor

The form editor (`BaseVisualFormEditor`) reports dirty via two mechanisms:

1. `isModified` flag — set by `flagModified()`, cleared by `doSave()`
2. `super.isDirty()` — delegates to nested editors → `GraphicalEditor.isDirty()` → `getCommandStack().isDirty()`

Eclipse's "Save All" only calls `doSave()` on editors whose `isDirty()` returns `true`.

### 2.2 The dummyCommandStack bypass

`VisualFormEditor.getAdapter(CommandStack.class)` (line 415–419) returns a no-op `dummyCommandStack` whenever `getActiveEditor() != graphicaleditor`. Property sheet entries that obtain the command stack via `getAdapter()` may execute commands on this dummy stack, whose dirty state is never consulted by `isDirty()`.

### 2.3 The persistChanges() notification path

`BaseVisualFormEditor.persistChanges(Collection<IPersist>)` is called by the Servoy model's persist-change notification system whenever any persist in the form's hierarchy is modified. It builds `changedChildren`, determines whether a `form_refresh` or `full_refresh` is needed, and calls `refresh()`. However, it never calls `flagModified()` — so external changes are rendered visually but the editor does not report dirty.

### 2.4 Prior art

- **SVY-20348** (`52406ba1d9`): Fixed `getActiveEditor()` returning null during init → `dummyCommandStack` used → form appeared clean.
- **SVY-20279** (`18775a8e23`): Routed `DeveloperMenuCommand` changes through the command stack to ensure dirty tracking.

Both confirm this is a known vulnerability class.

### 2.5 The VfeCommandStackEventListener.isRunningCommand() guard

The `VfeCommandStackEventListener` (inner class) tracks the last command-stack event state. Its `isRunningCommand()` method returns `true` between a PRE and POST event — i.e., while the real command stack is executing a command. This can be used to distinguish changes that are already tracked by the command stack (should NOT double-dirty) from changes arriving externally (SHOULD dirty).

### 2.6 The updateEditingPersist race condition (root cause)

When "Save All" runs with both a form editor and a script editor dirty:

1. Eclipse iterates dirty editors in undefined order
2. If the **script editor saves first**: the `.js` file is written to disk via `ITextFileBuffer.commit()` — this does NOT increment `resourceChangesHandlerCounter`
3. The resource change handler fires immediately (`ServoyModel.resourcesPostChanged()`)
4. `SolutionDeserializer.updateSolution()` reads the `.js` from disk, adds the new ScriptMethod to the **real** Form, marks the real Form as changed
5. `ServoyProject.updateEditingPersist(form, true)` **overwrites the editing form** with the real form (read from disk)
6. The real form's `.frm` file still has the OLD content (no onAction) because the form editor hasn't saved yet
7. **The in-memory onAction property is destroyed** on the editing persist
8. THEN the form editor's `doSave()` runs — writes the now-reverted form — onAction is lost

The DLTK script editor's save goes through standard Eclipse file buffers and does NOT increment `resourceChangesHandlerCounter` (unlike `EclipseRepository.updateNodesInWorkspace()` which does). This is why the resource change handler immediately processes the `.js` change and triggers the destructive re-read.

## 3. Design

### 3.1 Add flagModified() call in persistChanges()

At the end of `persistChanges()`, after determining that `changedChildren` is non-empty, add a call to `flagModified()` — but only when the change did NOT originate from the form editor's own command stack and the editor is not currently saving.

The guard condition:

```java
if (changedChildren.size() > 0)
{
    if (!commandStackEventListener.isRunningCommand() && !isSaving)
    {
        flagModified();
    }
    refresh(changedChildren, full_refresh);
}
```

This ensures:
- Normal edits via the design surface go through the command stack → `isRunningCommand()` is `true` → no double-dirty
- External changes (property sheet on dummy stack, script editor, developer menu) → `isRunningCommand()` is `false` → editor is flagged dirty
- Save-triggered persist notifications → `isSaving` is `true` → no spurious re-dirtying after save

### 3.2 Re-apply command stack changes in doSave()

Before writing the form to disk, `doSave()` must ensure the editing persist reflects the user's unsaved changes — even if `updateEditingPersist` has overwritten them. This is done by undoing all commands to the save point and then redoing them, which re-applies the property changes via `SetValueCommand.redo()` → `PersistPropertySource.setPropertyValue()`.

```java
isSaving = true;
try
{
    CommandStack cs = getCommandStack();
    if (cs.isDirty())
    {
        servoyModel.startCollectingPersistChanges(false);
        try
        {
            while (cs.canUndo()) cs.undo();
            while (cs.canRedo()) cs.redo();
        }
        finally
        {
            servoyModel.stopCollectingPersistChanges(false);
        }
    }
    // ... remove orphans, save, mark command stack ...
}
finally
{
    isSaving = false;
}
```

The `startCollectingPersistChanges/stopCollectingPersistChanges` wrapper suppresses persist change notifications during the undo+redo cycle to avoid side effects. The `isSaving` flag prevents `persistChanges()` from re-dirtying the editor during the save operation's own resource change notifications.

### 3.3 Why undo+redo works

After `updateEditingPersist` overwrites the editing form:
- The editing persist object is the SAME instance (properties overwritten in place)
- The `SetValueCommand` in the command stack holds a reference to the `PersistPropertySource` target (still valid)
- `undo()` sets the old value on the persist (harmless intermediate state)
- `redo()` sets the new value (the user's intended change, e.g., onAction method ID)
- After the full undo+redo cycle, the editing persist has all user changes re-applied
- `saveEditingSolutionNodes(form, true)` then writes the correct state to disk

## 4. Implementation plan

1. **Add `isSaving` field** to `BaseVisualFormEditor`: `private volatile boolean isSaving = false;`

2. **Edit `doSave()`**: Wrap with `isSaving = true` / `finally { isSaving = false; }`. Before the orphan-removal and save logic, add the undo+redo re-apply block guarded by `cs.isDirty()`.

3. **Edit `persistChanges()`** (end of method): Add the `flagModified()` guard inside the `if (changedChildren.size() > 0)` block, before `refresh()`, with condition `!commandStackEventListener.isRunningCommand() && !isSaving`.

4. **Verify compilation**: Zero errors.

5. **Manual verification steps**:
   - Open a responsive form, add a button, save.
   - Assign an onAction event handler via "OK & Show" (opens script editor).
   - Add a print line in the script editor.
   - Press "Save All" without activating the form editor.
   - Launch Titanium client → confirm onAction fires.
   - Return to form editor → confirm no unsaved-changes indicator.

## 5. Acceptance criteria

- [ ] After assigning an event handler via "OK & Show" and pressing Save All, the onAction is persisted to disk and works at runtime
- [ ] "Save All" persists the form editor's changes without requiring manual activation of the form editor
- [ ] Normal edits via the design surface do NOT cause double-dirtying (command stack dirty tracking still works as before)
- [ ] Undo/redo behavior is unaffected — the re-apply in `doSave()` does not alter the command stack's save-location tracking
- [ ] After Save All completes, the form editor does not show a spurious dirty marker

## 6. Out of scope

- Fixing the `dummyCommandStack` mechanism itself — requires separate analysis of undo/redo implications on Parts/Security/TabSequence tabs
- Making `updateEditingPersist` aware of dirty editors (model-layer change) — the undo+redo approach in the editor is safer and self-contained
- Controlling Save All editor ordering — Eclipse does not provide a priority mechanism

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Could there be edge cases where undo+redo fails (e.g., compound commands with side effects)? | Dev | Low risk — SetValueCommand is the standard property command, undo/redo are idempotent |
| Should we also fix `updateEditingPersist` to skip dirty forms at the model layer? | Architect | Deferred — current fix is safe and localized to the form editor |
| Is the `isSaving` flag sufficient for async resource change notifications arriving after `doSave()` returns? | Dev | Yes — `saveEditingSolutionNodes` blocks via latch until disk write completes; notifications from that write arrive during the latch wait while `isSaving` is still true |
