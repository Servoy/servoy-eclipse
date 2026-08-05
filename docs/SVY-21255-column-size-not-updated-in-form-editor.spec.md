# Spec: SVY-21255 â Column size is not updated in form editor

## 1. Goal

Fix the responsive form editor so that when a user zooms into a column (LayoutContainer), resizes it from the Properties view, and saves the form, the editor correctly reflects the new column size value â both in the rendered content and in the editor header bar.

## 2. Background

### 2.1 Responsive form editor architecture

The Servoy form designer (RFB editor) renders forms inside an Angular application with an embedded iframe (NG client). The editor has a "zoom in" feature that lets users double-click or navigate into a LayoutContainer to see its contents at full size. This is handled by `RfbVisualFormEditorDesignPage.zoomIn(LayoutContainer)` which sets `showedContainer` and reloads the editor URL with `&cont=<uuid>`.

### 2.2 Property change propagation

When a user modifies a property in the Properties view and the form is saved:

1. `BaseVisualFormEditor.doSave()` persists the form to disk
2. `persistChanges()` fires on the editor, calling `super.persistChanges()` â `refresh()` â `graphicaleditor.refreshPersists(persists, fullRefresh)`
3. `RfbVisualFormEditorDesignPage.PartListener.refreshPersists()` handles the refresh:
   - For **CSS position containers** that are the `showedContainer`: calls `refreshBrowserUrl(true)` (full page reload)
   - For all other cases: builds components JSON via `getComponentsJSON()` and sends `updateFormData` to the Angular iframe

### 2.3 Root cause 1: TypeError in updateFormData (frontend)

In `editorcontent.service.ts`, the `updateFormData()` method maintains a `reorderLayoutContainers` array that collects parent containers whose children need reordering. At line 84, when `formCache.getLayoutContainer(parentUUID)` returns `null` (e.g., the parent is not yet in the cache), the null value was pushed into the array without a guard. Later at line 410, `this.sortChildren(container.items)` crashes with:

```
TypeError: Cannot read properties of undefined (reading 'items')
```

This caused the entire `updateFormData` call to fail, preventing the editor from reflecting property changes.

### 2.4 Root cause 2: Stale header description (backend)

The editor header "Showing container: \<div class="col-md-X"\>" is set in `showContainer()` when the user first zooms in. When properties change via the `updateFormData` path (non-CSS-position containers), `refreshPersists()` never updates this header text. So even after the iframe content refreshes, the header continues showing the old class value.

## 3. Design

### 3.1 Fix null entries in reorderLayoutContainers (frontend)

In `editorcontent.service.ts`:

1. Add a `newParent &&` guard before pushing to `reorderLayoutContainers` at line 95, preventing null values from entering the array
2. Add an `if (container)` guard in the for-of loop at line 410 as a safety net

### 3.2 Update content description header on persist change (backend)

In `RfbVisualFormEditorDesignPage.PartListener.refreshPersists()`, after the `updateFormData` websocket call, check if the `showedContainer` is a LayoutContainer and is among the changed persists. If so, update the content description:

```java
if (showedContainer instanceof LayoutContainer && persists.contains(showedContainer))
{
    editorPart.setContentDescription("Showing container: " + DesignerUtil.getLayoutContainerAsString((LayoutContainer)showedContainer));
}
```

## 4. Implementation plan

1. **Fix `editorcontent.service.ts`** (line 95): Add `newParent &&` guard before pushing to `reorderLayoutContainers`
2. **Fix `editorcontent.service.ts`** (line 410): Add `if (container)` null check in the reorder loop
3. **Fix `RfbVisualFormEditorDesignPage.java`** (`refreshPersists` method): After the websocket `updateFormData` call, update `editorPart.setContentDescription()` when the `showedContainer` is in the changed persists list

## 5. Acceptance criteria

- [ ] After zooming into a column in a responsive form, changing its class from the Properties view, and saving, the form editor content shows the new value
- [ ] The editor header bar ("Showing container: ...") updates to reflect the new class value
- [ ] No `TypeError: Cannot read properties of undefined (reading 'items')` in the console
- [ ] CSS position containers zoomed-in behavior continues to work correctly (no regression)
- [ ] No unnecessary full refreshes occur when the changed persist is NOT the zoomed-in container

## 6. Out of scope

- Refactoring the `updateFormData` path for containers in general
- Improving performance of `refreshBrowserUrl(true)`
- Non-zoomed-in column resize scenarios (if they work correctly already)
- Replacing reference equality (`==`) with UUID equality in the CSS position guard (deferred)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Does the bug also reproduce when NOT zoomed in (i.e., column visible in parent view)? | QA | open |
| Should the CSS position guard also use UUID equality instead of `==`? | Dev | open |
