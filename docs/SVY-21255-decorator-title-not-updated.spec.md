# Spec: SVY-21255 — Responsive designer decorator label not updated on class change

## 1. Goal

When a layout container's class changes in the responsive form designer (e.g. `col-md-2` → `col-md-3`), the selection decorator's wireframe label drawn on the canvas must update to reflect the new class (`md-2` → `md-3`), matching what the Properties view already shows.

The label is CSS-generated text: `mouseselection.component.css:8-9` renders `.decorationOverlay.showWireframe::before { content: attr(svytitle); }`, so the fix is to keep the decorator node's `svytitle` DOM attribute fresh on the property-change refresh path.

## 2. Background

> This is the **reopen** of SVY-21255, and is a NARROWER, separate defect from the original fix documented in `docs/SVY-21255-column-size-not-updated-in-form-editor.spec.md`. The original fix addressed the column *size* update, a frontend crash guard, and the Java editor-header text. That spec is not touched by this one. This reopen is purely the wireframe/decorator title on the selection decorator.

### 2.1 The two decorator refresh paths

The decorator's `svytitle` attribute is written in exactly one place —
`MouseSelectionComponent.applyWireframeForNode()` (`mouseselection.component.ts:410`):

```ts
this.renderer.setAttribute(selectedNode.nativeElement, 'svytitle', node.getAttribute('svy-title')!);
```

It copies the **content element's** `svy-title` attribute onto the **decorator's** `svytitle` attribute, guarded so it only applies to real layout containers:

```ts
if (node.classList.contains('svy-layoutcontainer') && !node.getAttribute('data-maincontainer')
    && !node.classList.contains('svy-responsivecontainer') && position.width > 0 && position.height > 0) {
```

There are two distinct refresh paths, and only one of them copies the title:

- **`applyWireframe()` → `applyWireframeForNode()`** (`mouseselection.component.ts:398-421`) copies `svy-title` → `svytitle`. It runs only from:
  - `onMouseUp()` (`:386`) — a fresh click/selection, and
  - the constructor `effect()` (`:49-54`), which re-runs **only when `selectedRef()` changes** — i.e. when decorator nodes are added/removed, not when a selected element's properties change.
- **`redrawDecorators()`** (`mouseselection.component.ts:78-97`) runs on a `redrawDecorators` content message. It **only recomputes each node's position/size style** and never calls `applyWireframe()`/`applyWireframeForNode()`, so `svytitle` is never re-copied on this path.

On a property/class change, the server pushes an incremental update that ends in a `redrawDecorators` content message → `contentMessageReceived` (`:108-112`) → `selectionChanged(sel, true)` (`:110`) → `redrawDecorators()` (`:104`). Because the `selectedRef()` viewChildren list does not change on a property update, the constructor `effect()` does not fire either. Net effect: the decorator keeps its stale `svytitle` (`md-2`) indefinitely.

### 2.2 The content element's `svy-title` IS already correct

This is a frontend-only decorator-copy gap. The **content element's own** `svy-title` is refreshed correctly server-side:

- `ChildrenJSONGenerator.writeLayoutContainer(...)` emits `attributes["svy-title"]` into the `ng2containers` incremental-update array (`servoy-client` `ChildrenJSONGenerator.java:693`), computed via `FormLayoutStructureGenerator.getLayouContainerTitle(...)` (`:443-463`), which strips the leading `col-` so `col-md-3` → `md-3`.
- On the client, the container's `attributes` is reassigned to a new object reference, firing `AddAttributeDirective.ngOnChanges`, which re-applies every attribute — including the fresh `svy-title` — onto the content DOM element.

So after a class change the content element's `svy-title` is already `md-3`; only the decorator's copied `svytitle` stays stale. Re-copying it on the redraw path is sufficient.

### 2.3 Residual defect, not a migration regression

This gap has existed since the wireframe feature was introduced (`3494a3bf4d`, SVY-16294). The zoneless/signals migration `228632ce71` replaced `selectedRef.changes.subscribe(() => applyWireframe())` with the semantically equivalent constructor `effect(() => { selectedRef(); … applyWireframe(); })` — both fire only on decorator-list changes, never on a property update. `redrawDecorators()` never re-applied the title before or after the migration. The prior SVY-21255 fix (`477aa5f32d`) addressed size, not the wireframe title, so it did not touch this path.

## 3. Design

### 3.1 Approved approach (Approach 2): re-copy the title in the redraw path

In `MouseSelectionComponent.redrawDecorators()`, after recomputing each node's position, also re-copy the fresh `svy-title` from the content element onto the decorator node's `svytitle` attribute, reusing the same guard already in `applyWireframeForNode()`:

- element has class `svy-layoutcontainer`
- element does NOT have attribute `data-maincontainer`
- element does NOT have class `svy-responsivecontainer`
- computed size is non-zero (`width > 0 && height > 0`)
- and only when `editorSession.showWireframe()` is on

Factor the title-copy into a small reusable helper so `applyWireframeForNode()` and `redrawDecorators()` share it rather than duplicating the guard. Keep the change frontend-only in `com.servoy.eclipse.designer.rfb`.

### 3.2 Reaching the decorator ElementRef from inside the redraw map

`redrawDecorators()` maps over `nodes()` (the `signal<SelectionNode[]>` at `:27` that drives the template) and calls `this.nodes.set(...)`. The `svytitle`, however, is a DOM attribute on the **decorator's** `nativeElement`, not part of `SelectionNode` state — it is set imperatively via `renderer.setAttribute`, exactly as `applyWireframeForNode()` does today.

The decorator nativeElements are the `selectedRef` **viewChildren** (`readonly selectedRef = viewChildren<ElementRef<HTMLElement>>('selected')`, `:25`). Each decorator carries the element `id` matching its content element's svyid (this is how `applyWireframeForNode()` already resolves the content node: `getContentElement(selectedNode.nativeElement.getAttribute('id'))` at `:405`).

Concrete mechanism for the redraw map:

- For each `SelectionNode` being remapped (keyed by `selected.svyid`), look up the matching decorator `ElementRef` in `selectedRef()` by comparing its `nativeElement.getAttribute('id')` to `selected.svyid`.
- When a match is found and the shared guard passes and `showWireframe()` is on, call the shared helper to `renderer.setAttribute(decorator.nativeElement, 'svytitle', contentNode.getAttribute('svy-title'))`.
- Setting the attribute is a side effect on the decorator's `nativeElement`; it does not need to be part of the `SelectionNode` returned by the map, so the existing `nodes.set(...)` build-then-set pattern (and OnPush/zoneless behaviour) is unchanged. The position/size remap continues to flow through the returned `SelectionNode` exactly as before.

The redraw map already resolves the content element per node via `this.editorContentService.getContentElement(selected.svyid)` (`:82`); reuse that same `node` for reading `svy-title`, so no extra content-element lookup is required.

## 4. Implementation plan

All edits are in `com.servoy.eclipse.designer.rfb/node/src/designer/mouseselection/mouseselection.component.ts`. The CSS (`mouseselection.component.css`) is unchanged.

1. Extract the title-copy guard + `renderer.setAttribute(..., 'svytitle', ...)` currently inside `applyWireframeForNode()` (`:408-410`) into a small private helper, e.g. `applyWireframeTitle(decorator: ElementRef<HTMLElement>, contentNode: HTMLElement)`, that:
   - runs the shared guard (`svy-layoutcontainer`, not `data-maincontainer`, not `svy-responsivecontainer`, non-zero size),
   - only when `editorSession.showWireframe()` is on,
   - copies `contentNode.getAttribute('svy-title')` onto the decorator's `svytitle` attribute.
2. Have `applyWireframeForNode()` call the new helper for the title copy (leaving its background-color / `maxLevelDesign` handling as-is), so behaviour on the selection path is unchanged.
3. In `redrawDecorators()` (`:78-97`), inside the `currentNodes.map(...)` callback, after computing `node`/`position`, locate the decorator `ElementRef` in `selectedRef()` whose `nativeElement` `id` equals `selected.svyid` and, when found, call the new helper with that decorator and the content `node`. Guard for the no-match case (return `selected` unchanged, no throw).
4. Follow-up validation (per RFB `AGENTS.md`):
   - `npm run lint` — must pass with zero warnings
   - `npm run build_debug_nowatch` — must compile
   - `npm test` — Vitest unit tests must pass (including the new test)

## 5. Acceptance criteria

- [ ] After a selected layout container's class changes in the responsive designer (e.g. `col-md-2` → `col-md-3`), the on-canvas decorator label updates from `md-2` to `md-3` without requiring a reselect, matching the Properties view.
- [ ] The size/position remap behaviour of `redrawDecorators()` is unchanged (no regression to existing decorator repositioning).
- [ ] `applyWireframeForNode()` still applies the title on selection/mouse-up and via the constructor effect exactly as before (shared helper produces identical behaviour).
- [ ] Nothing happens (and nothing throws) when `showWireframe()` is off, when the node is not a layout container, when the node is `data-maincontainer` or `svy-responsivecontainer`, or when no matching decorator/content element is found.
- [ ] A new Vitest unit test is added to `mouseselection.component.spec.ts` under the existing `describe('redrawDecorators')` that:
  - sets up a selected layout container whose content element's `svy-title` changes from `md-2` to `md-3`,
  - calls `redrawDecorators()`,
  - asserts the decorator node's `svytitle` attribute is `md-3`,
  - and asserts `redrawDecorators()` does not set `svytitle` / does not throw when `showWireframe()` is off or the node is not a layout container.
- [ ] `npm run lint`, `npm run build_debug_nowatch`, and `npm test` all pass.

## 6. Out of scope

- The Java editor-header `setContentDescription` text (`RfbVisualFormEditorDesignPage`) — that was the earlier misdiagnosis and is not the reported symptom.
- Server-side `svy-title` generation (`ChildrenJSONGenerator` / `FormLayoutStructureGenerator`) — already correct.
- Absolute-layout decorators — the wireframe label applies only to responsive layout containers.
- The original SVY-21255 size/crash-guard fix in `docs/SVY-21255-column-size-not-updated-in-form-editor.spec.md`.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the shared title-copy helper also re-apply the background color / `maxLevelDesign` on the redraw path, or is title-only sufficient for the reopen? | Dev | open |
| Is matching decorator to node by `id === svyid` guaranteed 1:1 for all selection states (multi-select, nested containers)? | Dev | open |
