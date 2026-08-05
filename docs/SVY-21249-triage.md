# Triage Report — SVY-21249

**Verdict:** PROCEED

## Reported problem

When a user adds a button to a responsive form, saves, then assigns an onAction event handler (which opens the script editor), and presses "Save All" without first activating the form editor, the form's changes are not persisted. The runtime client does not trigger the onAction handler, and upon returning to the form editor it still shows unsaved changes.

## Root-cause assessment

The form editor (`BaseVisualFormEditor`) tracks dirty state via two mechanisms:
1. `isModified` flag (set by `flagModified()`)
2. `super.isDirty()` → checks nested editors → `GraphicalEditor.isDirty()` → `getCommandStack().isDirty()`

Eclipse's "Save All" only calls `doSave()` on editors whose `isDirty()` returns true. The form editor's `doSave()` at `BaseVisualFormEditor.java:375` persists the in-memory form and marks the command stack's save location.

**The bug:** When the onAction event handler is assigned, the property change does not reach the form editor's real command stack. It either:
- Is executed on the `dummyCommandStack` (which nobody checks for dirty state), OR
- Bypasses the command stack entirely (command executed directly per `UndoablePropertySheetEntry.java:296`)

Evidence:
- `VisualFormEditor.getAdapter(CommandStack.class)` (line 415-419) returns `dummyCommandStack` whenever `getActiveEditor() != graphicaleditor`. The `dummyCommandStack` is a no-op stack whose dirty state is never consulted by `isDirty()`.
- `UndoablePropertySheetEntry.executeCommand()` (line 286-298) falls back to executing the command directly (without a stack) if `getCommandStack()` returns null.
- `BaseVisualFormEditor.persistChanges()` (line 541) refreshes the UI but does NOT call `flagModified()` — external changes to form children are never reflected in the dirty flag.
- Two nearly identical bugs were recently fixed:
  - **SVY-20348** (commit `52406ba1d9`): `getActiveEditor()` returned null during initialization → `dummyCommandStack` was used → form appeared clean.
  - **SVY-20279** (commit `18775a8e23`): Developer menu actions modified persists without going through the command stack.

The specific trigger is likely a timing/ordering issue where the property change goes through `OpenEditorUndoablePropertySheetEntry.getCommandStack()`, which calls `persistEditor.getAdapter(CommandStack.class)`, and at that moment `getActiveEditor() != graphicaleditor` — possibly because the form editor's page state is transiently inconsistent during or after the script editor activation.

Key files:
- `com.servoy.eclipse.designer/src/.../editor/VisualFormEditor.java:415-419` — dummyCommandStack guard
- `com.servoy.eclipse.designer/src/.../editor/BaseVisualFormEditor.java:986-995` — flagModified / isDirty
- `com.servoy.eclipse.designer/src/.../property/OpenEditorUndoablePropertySheetEntry.java:59-73` — dynamic command stack lookup
- `com.servoy.eclipse.designer/src/.../property/UndoablePropertySheetEntry.java:286-298` — fallback direct execution

## Ticket premise check

The ticket correctly identifies the symptom (Save All doesn't save the form editor). It does not propose a specific solution. The problem is real and is in Servoy's Eclipse IDE code.

## Approaches considered

1. **Make `persistChanges()` call `flagModified()` when a child of the current form is changed externally** — This would ensure the form editor reports dirty whenever its model is modified from outside the command stack. Pros: catches all cases, including future ones. Cons: might cause false dirty states if `persistChanges()` fires for changes that don't need saving (e.g., cosmetic refreshes or already-saved changes). Would need careful filtering to only flag when the change is genuinely unsaved.

2. **Remove the `dummyCommandStack` mechanism and always return the real command stack from `getAdapter()`** — The guard `getActiveEditor() == graphicaleditor` was introduced to prevent undo/redo conflicts when on non-design tabs (Security, Parts). Removing it could cause unintended side effects. Pros: simple, eliminates the root cause for this class of bugs. Cons: may break undo behavior on other tabs.

3. **In `BaseVisualFormEditor.doSave()`, always save if the editing form has unsaved changes compared to disk** — Rather than relying solely on dirty tracking, compare the in-memory form persist against the on-disk state. Pros: robust fallback. Cons: performance cost on every save, doesn't fix the UI dirty indicator.

4. **No code change** — The issue could be user-side (they didn't actually set the event before switching). Pros: none. Cons: the reporter provides clear, reproducible steps and the code analysis confirms the vulnerability exists.

## Recommendation

**Approach 1** is recommended: add `flagModified()` in `BaseVisualFormEditor.persistChanges()` when a genuine child property change is detected that modifies the form's persist tree. This is the safest fix that:
- Catches this specific bug
- Catches any future code paths that bypass the command stack
- Follows the same pattern used successfully in other editors (`VisualFormEditorSecurityPage`)

The filtering should only flag modified when `changedChildren` is non-empty or `form_refresh`/`full_refresh` is true, AND the change originates from outside the form editor's own command stack (to avoid double-dirtying during normal edits). The `VfeCommandStackEventListener.isRunningCommand()` method (line 1133) can be used to detect whether the change came from the command stack.

A complementary fix could address the `dummyCommandStack` guard to make it less fragile (approach 2), but that requires more careful analysis of the undo/redo implications on the Parts/Security/TabSequence tabs.

## Git history findings

- `52406ba1d9` (SVY-20348, Jul 10 2025): Fixed identical dirty-tracking bug caused by `getActiveEditor()` returning null during initialization. Fix: moved `setActivePage(0)` earlier.
- `18775a8e23` (SVY-20279, Jul 15 2025): Added `DeveloperMenuCommand` to route JSForm/JSWebComponent changes through the command stack. Same pattern: persist changes were bypassing dirty tracking.
- Both fixes confirm this is a known vulnerability class in the form editor's architecture.
