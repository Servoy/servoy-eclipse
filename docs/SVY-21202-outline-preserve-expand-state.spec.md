# Spec: SVY-21202 — Annoying auto expanding in responsive form's outline view

## 1. Goal

Preserve the user's expanded/collapsed state in the form outline view when changes are made to the form. Currently, every persist change (add, delete, edit, drag) unconditionally re-expands the entire tree to a fixed depth, which is disorienting when working with deeply nested responsive form structures.

## 2. Background

### 2.1 Outline view architecture

The form outline view is implemented by `FormOutlinePage` (`com.servoy.eclipse.designer.outline.FormOutlinePage`), which extends Eclipse's `ContentOutlinePage`. It uses a standard `TreeViewer` with `FormOutlineContentProvider` as content provider and `PersistContext` as tree element objects.

`PersistContext` has proper `equals`/`hashCode` implementations based on its persist and context fields. This means Eclipse's `TreeViewer.refresh()` can match elements before and after a refresh and automatically preserve expanded state.

### 2.2 Current behavior (the problem)

When any persist change occurs:

1. `persistChanges()` is called (line 538)
2. It calls `refresh()` which internally calls `getTreeViewer().refresh()` — this correctly preserves expanded state
3. It then schedules `Display.getDefault().asyncExec(this::defaultExpand)` (line 553)
4. `defaultExpand()` calls `expandToLevel(ELEMENTS, 4)` for responsive forms (or level 3 for absolute forms)

Step 4 **unconditionally overrides** the user's expand state, expanding everything back to the default depth. This is the root cause of the reported annoyance.

### 2.3 Git history

The `defaultExpand()` call in `persistChanges` was introduced in commit `597e6d916` (SVY-20066: "Dragging elements in outline gives very strange behaviour"). The intent was to ensure elements remain visible after a drag-and-drop reorder. However, since `persistChanges` is called for all changes (not just drag), it has the unintended side effect of resetting the tree state on every single modification.

The commit `ef1874395d` ("nicer syntax") later only reformatted the lambda to a method reference, not changing behavior.

### 2.4 Existing reveal mechanism

The `selectionChanged` method (lines 494–508) already handles revealing newly selected elements in responsive forms: it expands the ancestor hierarchy of the selected persist. This means when a new element is added and selected, its ancestors will be expanded to make it visible — without needing `defaultExpand`.

## 3. Design

### 3.1 Remove unconditional defaultExpand from persistChanges

Remove the `Display.getDefault().asyncExec(this::defaultExpand)` call from `persistChanges()`. The `TreeViewer.refresh()` already preserves expanded state for existing elements, and `selectionChanged` handles revealing newly selected elements.

### 3.2 Keep defaultExpand for initial creation only

The `defaultExpand()` call in `createControl()` (line 354) remains unchanged — it provides the correct initial expansion when the outline view is first opened.

### 3.3 Keep defaultExpand for display type toggle

`GroupedOutlineViewToggleAction` calls `refresh()` when toggling between grouped/ungrouped view. Since this is a deliberate view mode switch (not an incremental change), the user expects the tree to re-expand. However, that code path currently only calls `refresh()` without `defaultExpand`, so no change needed there.

### 3.4 Consideration for drag-and-drop (SVY-20066 regression risk)

The original reason for adding `defaultExpand` was SVY-20066 (drag giving "strange behaviour"). The actual fix for SVY-20066 was likely that `refresh()` needed to be called to update the tree after a parent change — and the `defaultExpand` was added as a safeguard. Since `TreeViewer.refresh()` preserves expanded state AND `selectionChanged` reveals the dragged element at its new location, removing `defaultExpand` should not regress SVY-20066.

If a dragged element ends up under a collapsed parent, it will still be revealed because the editor selects it after the drop, triggering `selectionChanged` which expands ancestors.

## 4. Implementation plan

1. In `FormOutlinePage.java`, remove line 553 (`Display.getDefault().asyncExec(this::defaultExpand);`) from the `persistChanges` method.
2. Verify the `defaultExpand()` method is still used in `createControl()` (line 354) — do not remove the method itself.
3. Organize imports (no change expected since `Display` is still used in `refresh()`).
4. Run compilation check.
5. Test manually:
   - Open a responsive form with nested containers
   - Collapse some nodes
   - Add a new container → verify collapsed nodes stay collapsed, new element is visible
   - Drag an element → verify collapsed nodes stay collapsed, moved element is visible
   - Toggle grouped/ungrouped view → verify tree re-expands correctly

## 5. Acceptance criteria

- [ ] After adding a new element to a responsive form, previously collapsed outline nodes remain collapsed
- [ ] After editing a property of an element, the outline tree expand state is preserved
- [ ] After dragging an element to a new parent, previously collapsed nodes remain collapsed and the moved element is visible (selected)
- [ ] The initial outline expand still works (expand to level 4 for responsive, level 3 for absolute) when opening a form
- [ ] Toggling grouped/ungrouped view still refreshes the tree correctly
- [ ] No regression on SVY-20066 (drag-and-drop does not produce strange behavior)

## 6. Out of scope

- Persisting outline expand state across form editor close/reopen (session persistence)
- Smart auto-expand of newly added elements beyond what `selectionChanged` already provides
- Changes to absolute layout form behavior (though the fix applies equally to both)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should newly added elements that are NOT selected still be auto-expanded? (Edge case: paste multiple items at once) | Developer | open |
| Does SVY-20066 need additional testing beyond the acceptance criteria? | QA | open |
