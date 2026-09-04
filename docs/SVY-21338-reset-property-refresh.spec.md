# Spec: SVY-21338 — Reset to default of the datasource of a form - refresh & undo problem in prop. view

## 1. Goal

Fix two bugs in `RetargetToEditorPersistProperties` when editing properties via the Properties view with a persist selected in Solution Explorer:

1. **Refresh bug:** "Restore Default Value" does not immediately reflect the reset value in the UI — the user must click away and back.
2. **Undo bug:** Neither set nor reset operations can be undone (Ctrl+Z does nothing) because the anonymous `Command` in `updateProperty()` never overrides `undo()`.

## 2. Background

### 2.1 The Properties view "Restore Default Value" flow

When the user selects a persist (e.g. a form) in Solution Explorer and right-clicks a property key in the Properties view, "Restore Default Value" triggers this call chain:

1. `PropertySheetEntry.resetPropertyValue()` calls `source.resetPropertyValue(descriptor.getId())`
2. After the call returns, it sets `change = true` and calls `refreshFromRoot()`
3. `refreshFromRoot()` re-reads the property value from the model and updates the UI

The critical assumption is that step 1 has already mutated the model by the time step 2 reads it back.

### 2.2 The `asyncExec` vs `syncExec` problem

`RetargetToEditorPersistProperties` wraps property mutations so they go through the correct editor's command stack. Both `resetPropertyValue()` and `setPropertyValue()` schedule work on the SWT display thread.

- `setPropertyValue()` (line 94) uses `Display.getCurrent().syncExec()` — the Runnable executes inline when already on the UI thread, so the model is updated before the method returns.
- `resetPropertyValue()` (line 75) uses `Display.getCurrent().asyncExec()` — the Runnable is posted to the event queue and executes *after* the current event finishes, meaning the model is still unchanged when `refreshFromRoot()` runs.

### 2.3 The undo problem

`updateProperty()` (lines 103–141) creates an anonymous `Command` that only overrides `execute()`. The base GEF `Command.undo()` is a **no-op**, so when the command is pushed onto the editor's `CommandStack` and the user presses Ctrl+Z, nothing happens.

The designer plugin already has proper command implementations for this exact pattern:
- `ResetValueCommand` — saves the old value in `execute()`, restores it in `undo()`
- `SetValueCommand` — saves the old value in `execute()`, restores it in `undo()`

The fix should follow the same pattern: capture the old value before executing, and restore it on undo.

### 2.4 Git history

- **Commit `0d078765309`** (Jan Blok, 2010-06-05, "first commit"): Both methods originally used `asyncExec`. The anonymous Command in `updateProperty()` never had `undo()` — this was an oversight from day one.
- **Commit `36a1864c9bc`** (Rob Gansevles, 2024-03-15, SVY-18860): `setPropertyValue()` was changed from `asyncExec` to `syncExec` to fix the same class of refresh problem. `resetPropertyValue()` was not updated — this oversight is the direct cause of the refresh bug.

## 3. Design

### 3.1 Change `asyncExec` to `syncExec` in `resetPropertyValue()`

In `RetargetToEditorPersistProperties.java` line 75, replace `asyncExec` with `syncExec`. This is a one-token change that aligns `resetPropertyValue()` with the existing `setPropertyValue()` pattern.

When called from the UI thread (which is always the case for Properties view interactions), `syncExec` executes the Runnable immediately inline. The model mutation completes before the method returns, so the caller's subsequent `refreshFromRoot()` reads the updated value.

**Already implemented in Phase 2.**

### 3.2 Add undo/redo support to `updateProperty()`

Replace the bare anonymous `Command` in `updateProperty()` with one that properly implements `undo()` and `redo()`, following the pattern from `ResetValueCommand` and `SetValueCommand` in the designer plugin.

The command must:
1. In `execute()`: capture the old value via `persistProperties.getPropertyValue(id)` before applying the change. If the old value is an `IPropertySource`, unwrap it via `getEditableValue()`.
2. Apply the change: call `persistProperties.setPropertyValue(id, value)` for set, or `persistProperties.resetPropertyValue(id)` for reset.
3. In `undo()`: restore the old value via `persistProperties.setPropertyValue(id, oldValue)`. For reset operations where the property was not previously set, call `persistProperties.resetPropertyValue(id)` instead.
4. In `redo()`: re-apply the change (call `execute()` or repeat the set/reset logic).
5. Set a descriptive label on the command (e.g. "Set Property" / "Reset Property") so the Edit menu shows "Undo Set Property".

**Before:**
```java
Command cmd = new Command()
{
    @Override
    public void execute()
    {
        if (set)
        {
            persistProperties.setPropertyValue(id, value);
        }
        else
        {
            persistProperties.resetPropertyValue(id);
        }
    }
};
```

**After (conceptual):**
```java
Command cmd = new Command(set ? "Set Property" : "Reset Property")
{
    private Object oldValue;

    @Override
    public void execute()
    {
        oldValue = persistProperties.getPropertyValue(id);
        if (oldValue instanceof IPropertySource)
        {
            oldValue = ((IPropertySource)oldValue).getEditableValue();
        }
        doExecute();
    }

    private void doExecute()
    {
        if (set)
        {
            persistProperties.setPropertyValue(id, value);
        }
        else
        {
            persistProperties.resetPropertyValue(id);
        }
    }

    @Override
    public void undo()
    {
        if (oldValue != null)
        {
            persistProperties.setPropertyValue(id, oldValue);
        }
        else
        {
            persistProperties.resetPropertyValue(id);
        }
    }

    @Override
    public void redo()
    {
        doExecute();
    }
};
```

### 3.3 Risk assessment

- The `asyncExec` → `syncExec` change mirrors the exact pattern used by `setPropertyValue()` since March 2024 (SVY-18860), which has been stable in production.
- The undo/redo logic follows the established pattern from `ResetValueCommand` and `SetValueCommand` in the designer plugin.
- `syncExec` on the current display thread when already on the UI thread simply runs the Runnable inline — no thread-blocking or deadlock risk.

## 4. Implementation plan

1. In `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/property/RetargetToEditorPersistProperties.java` line 75, change `asyncExec` to `syncExec`. **(Already done.)**
2. In the same file, rewrite the anonymous `Command` in `updateProperty()` (lines 116–130) to:
   - Save the old value before executing
   - Override `undo()` to restore the old value
   - Override `redo()` to re-apply the change
   - Set a descriptive label
3. Add the `IPropertySource` import if not already present.
4. Organize imports and format file.
5. Verify no compilation errors.

## 5. Acceptance criteria

- [ ] `resetPropertyValue()` in `RetargetToEditorPersistProperties` uses `syncExec` instead of `asyncExec`
- [ ] Right-clicking a property and choosing "Restore Default Value" immediately reflects the reset value in the Properties view (no need to click away and back)
- [ ] After setting a property value via the Properties view, Ctrl+Z undoes the change
- [ ] After resetting a property value via "Restore Default Value", Ctrl+Z restores the previous value
- [ ] Redo (Ctrl+Y) re-applies the undone change
- [ ] Setting property values via the Properties view continues to work correctly (no regression in `setPropertyValue()`)
- [ ] No compilation errors in `com.servoy.eclipse.ui`

## 6. Out of scope

- Refactoring the anonymous `Runnable` classes to lambdas (unrelated cleanup)
- Reviewing other `IPropertySource` implementations for similar `asyncExec` usage
- Changes to `PropertySheetEntry` or any Eclipse platform code
- Reusing `SetValueCommand` / `ResetValueCommand` from the designer plugin (they have designer-specific dependencies like `PropertySheetEntry`, `ComplexPropertySourceWithStandardReset`)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| *None* | | |
