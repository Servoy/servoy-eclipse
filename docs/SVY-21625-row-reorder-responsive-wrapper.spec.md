# Spec: SVY-21625 — Cannot drag a row between two rows in the responsive form designer

## 1. Goal
In a responsive (12grid) form, dragging a layout container (e.g. `row3`) in the RFB form designer and dropping it between two siblings (`row1` and `row2`) must reorder it inside its parent container. Today the row is moved to the form root or stays where it was. This is a regression from SVY-21248 and must be fixed in the designer drop-target resolution (`DesignerUtilsService.getParent`).

## 2. Background

### 2.1 Reported problem
Form `resp` (attachment `row_reorder.servoy`): `container-fluid > row1 > col-md-12 > fileupload_3`, `row2 > col2 > button33`, `row3 > column12 > aggrid1`. Dragging `row3` over `row2` and dropping gives a wrong result:
- the preview and the drop put the row "on top of the container", at the form root, or
- nothing changes (row stays put).

The `columns` ghost strip overlapping the "property 'columns'" label of `aggrid1` (visible in the screenshot) is a separate cosmetic issue and is out of scope.

### 2.2 Root cause (from `docs/SVY-21625-triage.md`)
SVY-21248 (`18f0c91473`, first shipped in 2026.9.0) renders every child of a layout container inside `<div class="svy-responsive-wrapper">` (`display: contents`, no `svy-id`, no `svy-layoutname`). Template: `#svyResponsiveDiv` in `com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts`. Top-level form children are not wrapped. Resulting DOM:

```
.svy-form
  div.svy-layoutcontainer [svy-id=container, svy-layoutname=12grid.container]
    div.svy-responsive-wrapper
      div.svy-layoutcontainer [svy-id=row1, 12grid.row]
    div.svy-responsive-wrapper
      div.svy-layoutcontainer [svy-id=row2, 12grid.row]
    div.svy-responsive-wrapper
      div.svy-layoutcontainer [svy-id=row3, 12grid.row]
```

`DesignerUtilsService.getParent(dt, realName)` (`com.servoy.eclipse.designer.rfb/node/src/designer/services/designerutils.service.ts`, ~lines 256-273) is called from `getDropNode` (~line 130) with `dt = row2`:
1. `row2` (`12grid.row`) does not accept `12grid.row`, so it recurses with `row2.parentElement`, which is now the wrapper (formerly the container).
2. The wrapper has no `svy-layoutname`, so `editorSession.getAllowedChildrenForContainer(null)` falls back to the `'topContainer'` list. That list contains `12grid.row`, so `getParent` returns the wrapper as the drop target.
3. The wrapper has no `svy-id`. `dragselection-responsive.component.ts` (~line 164) sends `dropTarget: null`. The iframe (`designform_component.component.ts` ~406-417) then uses the form root. On drop (`onMouseUp` ~199-213) the server (`MoveInResponsiveLayoutHandler`) cannot resolve the target and falls back to the form, so `ChangeParentCommand` moves `row3` to the form root.
4. In the bottom/right 30% band of `row2`, `getNextElementSibling(row2)` reads through the wrapper and returns `row3`, the dragged node. `onMouseMove` (~line 153) ignores that, so the row stays put (the second symptom).

`getDropNode` is also used by `palette.component.ts` (~lines 343 and 495), so dropping a layout container from the palette onto a nested container that rejects that type can hit the same bug.

### 2.3 Git history
- `18f0c91473` (SVY-21248, 2026-07-28): added the wrapper; the regression source.
- `c2a38d6ffa` (SVY-21578, 2026-10-12): fixed the same class of bug locally in `MouseSelectionComponent.insertACopyAction`. Same pattern: skip the wrapper.
- `getParent` last changed functionally in `e8929eed4b` (SVY-20282) and `0708bf8146` (SVY-19767); neither knew about the wrapper. Specs of those cases in `docs/` (if any) should be checked by the implementer for constraints.
- `getNextElementSibling` and the "drop directly on the node" branch of `getDropNode` (`querySelector('[svy-id]')`) already handle the wrapper; `getParent` does not.

## 3. Design

### 3.1 Approved approach: `getParent` steps over non-element wrappers
In `DesignerUtilsService.getParent`, before resolving allowed children for `dt`, skip any `.svy-responsive-wrapper` (more generally: an element that has no `svy-id` and is a pure wrapper, not the `.svy-form` root) by continuing with its `parentElement`:

```ts
public getParent(dt: Element, realName?: string): Element {
    if (!dt) return null!;
    // SVY-21625: the responsive wrapper (SVY-21248) is a transparent element, never a drop target
    if (dt.classList.contains('svy-responsive-wrapper') && dt.parentElement) {
        return this.getParent(dt.parentElement, realName);
    }
    ...
}
```

Design notes:
- Prefer matching on the explicit class `svy-responsive-wrapper` over "no svy-id", because the form root (`.svy-form`) and existing `getParent` paths rely on no-`svy-id` elements being handled by the existing `topContainer` fallback. If a generic rule is used instead, it must exclude `.svy-form` and must keep `getParent(.svy-form)` returning the form.
- With the fix, the flow for the sample is: `getParent(row2)` → recurse on wrapper → skipped → `container` (`12grid.container`) accepts `12grid.row` → `realDropTarget = container`. `getDropNode` then returns `{ dropAllowed: true, dropTarget: container, beforeChild: row2 }` (or the next sibling in the 30% band).
- Top-level behavior is unchanged: top-level items are not wrapped and `getParent(.svy-form)` still returns the form.

### 3.2 `beforeChild` in the 30% band
In the `realDropTarget != dropTarget` branch, `dropTarget = getNextElementSibling(dropTarget)` may return the dragged element itself (e.g. dragging `row3` over the lower part of `row2`, so `beforeChild == row3`). The implementer must check that, after fixing `getParent`, this produces a harmless result (dropping `row3` before itself is a no-op reorder that keeps the row inside the container, not a move to the form root). If `onMouseMove` still rejects it and the preview does not render, handle it explicitly (for example, treat `beforeChild` equal to the dragged node as "stay in place within `realDropTarget`"). Only change this if the manual test in section 5 shows a wrong result; do not widen the scope otherwise.

### 3.2b Component under the pointer is not a parent for a layout container (found during manual testing)
Manual testing with Chrome DevTools logpoints on the live designer showed a second, pre-existing path in `getParent`: when a layout container (e.g. `12grid.row`) is dragged over a plain component (e.g. `button33`), the component has no `svy-layoutname`, so `getAllowedChildrenForContainer(null)` returns the form root's `'topContainer'` list. That list contains `12grid.row`, so the component itself was returned as the drop target. The iframe cannot resolve a component as a layout container, so the preview row disappeared and a release at that moment would not land inside the container.

Fix, in `getParent` right after the wrapper skip: when the dragged item is a layout container (`realName !== 'component'`) and `dt` has no `svy-layoutname`, no `svy-types` and is not `.svy-form`, continue with `dt.parentElement`. Dragging components, custom containers with `svy-types`, and the form root keep their current behaviour.

### 3.2c Preview collapse while moving a tall layout container (found during manual testing)
Moving a layout container whose content renders asynchronously (e.g. a row holding an ag-grid) to another preview position re-creates its content, which starts at 0 height. Logs showed the dragged row shrinking from 373px to 40px between two mouse moves, the container shrinking with it, and the unchanged pointer ending up below the container; `getNode` then found nothing and the form-root branch moved the preview (and the drop) out of the container.

The 12grid design CSS already gives `.rowDesign` / `.columnDesign` a `min-height: 40px !important`, which is exactly the height the row collapsed to; a fixed floor cannot hold an arbitrary measured height, and an inline `min-height` would lose against that `!important` rule.

Fix, CSS-driven, in the iframe:
- `designform.css`: `.svy-layoutcontainer.svy-dragged-layout { min-height: var(--svy-dragged-layout-height) !important; }` (two classes + `!important` wins over the 40px design floor).
- `designform_component.component.ts`: on `createDraggedComponent` for a layout container (not a drag copy) with a measured `size` (already sent by RFB since SVY-21483), remember its uuid and set `--svy-dragged-layout-height` on the designform host element; `getNGClass` adds `svy-dragged-layout` to that container only; `destroyElement` clears both. Only active during a drag; drop resolution is unchanged.

### 3.3 Why not the alternatives
- Changing the `'topContainer'` fallback in `getAllowedChildrenForContainer` is broader and riskier (the form root and components rely on it).
- Removing the wrapper (reworking SVY-21248) is far larger and out of scope for a minor regression fix. It may be raised with the SVY-21248 author separately.

## 4. Implementation plan
Angular/TypeScript and CSS only, in `com.servoy.eclipse.designer.rfb/node/` (3.1, 3.2b) and `com.servoy.eclipse.ngclient.ui/node/src/designer/` (3.2c). Read each project's `AGENTS.md`. No Java, MANIFEST, or pom changes. Note: Eclipse MCP tools only work through `execute`; run the Angular commands with npm from the `node/` folders.

1. Edit `src/designer/services/designerutils.service.ts`: add the wrapper skip at the top of `getParent` as in 3.1, and the plain-component skip right after it as in 3.2b. Keep the code style (4-space indentation as in the file, single quotes).
1b. In `com.servoy.eclipse.ngclient.ui/node/src/designer/`: add the `.svy-dragged-layout` rule to `designform.css` and the uuid/CSS-variable handling plus the `getNGClass` class to `designform_component.component.ts`, as in 3.2c. Add Vitest tests in `designform_component.component.spec.ts` for the class and the variable (set on drag start, kept after a preview move, cleared on drag end, not set without a size or for a copy).
2. Add Vitest regression tests in `src/designer/services/designerutils.service.spec.ts`, using the file's `makeNode` helper and `Object.create(DesignerUtilsService.prototype)` pattern (no `vi.stubGlobal('document'|'window')`):
   - `getParent` skips a `.svy-responsive-wrapper`: wrapper whose parent is a `svy-layoutname='12grid.container'` node that allows `12grid.row`, with `getAllowedChildrenForContainer` mocked as `name === '12grid.container' ? ['12grid.row'] : (name == null ? ['12grid.row'] : null)` to mimic the `topContainer` fallback; assert `getParent(row2, '12grid.row')` returns the container, not the wrapper. Must fail on the current code.
   - `getDropNode(false, 'layout', true, '12grid.row', event, undefined, undefined, row3)` with the real SVY-21248 wrapped DOM (`container > wrapper > row1/row2/row3`), `getNode` resolving to `row2` (mock `getAllContentElements` or spy `getNode`), cursor in the upper part of `row2`: assert `dropAllowed`, `dropTarget` is the container and `beforeChild` is `row2`. Must fail on current code (it returns the wrapper).
   - A guard test: `getParent` on an element whose parent is the `.svy-form` root (no wrapper) still returns the form/previous result (no regression for top-level items).
   - A test pinning the 30% band result (section 3.2): `beforeChild` is the dragged row, `dropTarget` stays the container.
   - Tests for 3.2b: a row over a component resolves to the column; a component over a component still returns the component; an element with `svy-types` is still returned.
3. Run `npm run lint` (zero new warnings), `npm run build_debug_nowatch`, `npm test` (and `npm run test:browser` if available). Update the test count in `AGENTS.md` only if the maintainers keep it current.
4. Manual verification with the attached `row_reorder.servoy`: drag `row3` between `row1` and `row2`; drop above `row1`; drop after `row2`; drag a Row from the palette onto a column and onto a row; check an absolute-layout form is unaffected.
5. Optional audit (separate, not part of this fix): look for other `parentElement`/`nextElementSibling` walks in the RFB code that assume a layout container child is a direct DOM child.

## 5. Acceptance criteria
- [ ] In a responsive form with `container-fluid > row1, row2, row3`, dragging `row3` over `row2` resolves the container as drop target with `row2` as the before-child; the drop reorders to `row1, row3, row2` inside the container.
- [ ] A row is never moved to the form root as a side effect of a drop inside a container (no `dropTarget` without a `svy-id`).
- [ ] Dropping in the lower/right 30% of a row works (row moves after it, or stays in place inside the container, never leaves it).
- [ ] Palette drops of layout containers/components onto nested containers resolve to the correct ancestor that accepts them.
- [ ] Top-level (unwrapped) drops and absolute-layout forms behave as before.
- [ ] Dragging a row over a plain component never returns the component as drop target; the preview row stays visible (3.2b).
- [ ] While a tall layout container (e.g. a row with a grid) is moved to another preview position, it keeps its measured height, so the preview does not jump out of the container (3.2c).
- [ ] New Vitest tests fail on the old code and pass on the new; `npm test`, `npm run lint` and `npm run build_debug_nowatch` pass.

## 6. Out of scope
- The `columns` ghost strip overlapping the "property 'columns'" text for `aggrid1`.
- Bottom autoscroll not starting while dragging (regression from SVY-21706, reported back on that case).
- The full-size floating drag image covering the preview slot of a tall row; and Bootstrap columns accepting nested rows as a drop target (existing behaviour).
- Removing/reworking the `.svy-responsive-wrapper` (SVY-21248) or changing the `'topContainer'` fallback.
- A general audit of other wrapper-blind DOM walks (may become a follow-up case).
- Any Java/server-side change in `MoveInResponsiveLayoutHandler`.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Does the 30% bottom/right band return the dragged node as `beforeChild` and need explicit handling (section 3.2)? | Implementer | resolved: yes, it is an in-place no-op that `onMouseMove` correctly skips; no change, pinned by a test |
| Match only the `svy-responsive-wrapper` class, or any element without `svy-id` (excluding `.svy-form`)? Default: the class. | Implementer | resolved: the class for the wrapper; plain components handled separately (3.2b) |
| Raise the wrapper design (approach 3) with the SVY-21248 author? | Team | open |
