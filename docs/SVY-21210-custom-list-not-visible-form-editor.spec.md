# Spec: SVY-21210 — Custom list is not visible in form editor

## 1. Goal

Components whose rendering depends on a `containedForm` binding (currently `customrenderedcomponents-customlist`, `customrenderedcomponents-foundsetlist`, and similar community packages such as `lightboxgallery` and `treeview`) show no visible border or outline in the form editor, making them impossible to locate by eye. The fix must render a visible placeholder — the component's bounding rectangle with a dashed border — when the component is present on the canvas but renders no visible pixels in design mode.

## 2. Background

### 2.1 How components are rendered in the form editor

The Servoy form editor is a two-frame Angular application:

- **Outer frame** — `com.servoy.eclipse.designer.rfb` Angular app (the "RFB designer"). It overlays the inner frame with glass-pane decorations: selection boxes (`decorationOverlay`), ghost containers, highlight, resize knobs, etc.
- **Inner frame** — `com.servoy.eclipse.ngclient.ui` Angular app running the actual NG client in design mode. Components are rendered here with `svy-id` attributes so the outer frame can query them.

Visibility of a component in the editor therefore depends on two things:

1. The inner frame rendering *something* with a non-zero bounding rect.
2. The outer frame `HighlightComponent` / `MouseSelectionComponent` querying `[svy-id]` elements and overlaying decorators.

### 2.2 The `servoycore-listformcomponent` and related components

`servoycore-listformcomponent` (the base for `customlist`, `foundsetlist`, and other "custom rendered" components) wraps a `containedForm` — a sub-form that provides the row template. Its template has an `@if` guard:

```typescript
// listformcomponent.ts lines 49, 62
@if (cache&&containedForm()&&containedForm().absoluteLayout) { ... }
@if (cache&&containedForm()&&!containedForm().absoluteLayout) { ... }
```

In design mode `useScrolling` is `false` (the `svyOnInit` early-returns before setting it to `true`), so the ag-Grid branch is never entered. The template only renders inner content when **both** `cache` and `containedForm()` are truthy.

In the case reported (SVY-21210), the `containedForm` property is not set / is `null` at render time in the designer, so neither `@if` branch is entered and the `<div class="svy-listformcomponent">` is left empty. An empty `div` with `width:100%; height:100%` collapses to 0×0 pixels because its CSS-positioned parent wrapper itself does have a bounding rect, but the inner div has no content to push it open. As a result:

- `getBoundingClientRect()` on the element returns `{width: 0, height: 0}`.
- `checkIfNodeIsVisible()` in `mouseselection.component.ts:516` returns `false`, hiding the `decorationOverlay`.
- The highlight component skips the element entirely.
- The user sees nothing.

### 2.3 Why `containedForm` is null

`containedForm` is an Angular `input()` signal fed from the parent designer form component (`DesignFormComponent`). In the designer the server does not send a real foundset/form-component value for `containedForm` — it is intentionally omitted (the designer only needs the structure, not live data). The `FormComponentCache` for the LFC therefore has `containedForm = undefined`.

This is the same root cause that was partially fixed by SVY-20929 (which added the `cache&&` guard so that at least the template would not crash when `cache` was missing). However SVY-20929 did not address the case where `cache` is present but `containedForm()` is falsy.

### 2.4 Affected component types

The bug report lists:
- `customrenderedcomponents-customlist`
- `customrenderedcomponents-foundsetlist`
- `lightboxgallery` (assumed same base)
- `treeview` (assumed same base)

All of these extend `servoycore-listformcomponent` via the Servoy `ng-package` extension mechanism, so the fix in the base component applies to all of them.

### 2.5 Comparison with `invisible_element`

The `.invisible_element` CSS class (applied when `svyVisible === false`) gives a component `opacity: 0.3; border: 2px dashed` — exactly the kind of fallback treatment needed here. The same visual treatment should apply to LFC-type components that have no `containedForm` in the designer.

### 2.6 Git history

- **SVY-20929** (`80aefbb1b`) — "Form editor doesn't display any visual for lfc or its components" — added the `cache&&` guard. This commit is the direct predecessor of the current bug. It made progress (prevented a crash / blank render when `cache` was null) but left the `containedForm` null case unaddressed.

## 3. Design

### 3.1 Render a designer placeholder when `containedForm` is absent

The cleanest fix is to show a fallback `<div>` inside `listformcomponent.ts` when the component is in design mode and `containedForm()` is falsy. This div will give the element a non-zero size so:

- The `svy-listformcomponent` div is not empty.
- `getBoundingClientRect()` returns a non-zero rect.
- The designer overlay and highlight work correctly.

The fallback div should be styled to communicate "this is an LFC with no preview available" — a dashed border rectangle filling the component bounds.

**Template change** (inside `listformcomponent.ts`):

```html
@if (!useScrolling) {
  @if (cache&&containedForm()&&containedForm().absoluteLayout) {
    <!-- existing absolute layout rendering ... -->
  }
  @if (cache&&containedForm()&&!containedForm().absoluteLayout) {
    <!-- existing responsive rendering ... -->
  }
  @if (servoyApi.isInDesigner() && (!cache || !containedForm())) {
    <div class="svy-listformcomponent-designer-placeholder"></div>
  }
}
```

### 3.2 CSS for the placeholder

Add to `listformcomponent.css`:

```css
.svy-listformcomponent-designer-placeholder {
    width: 100%;
    height: 100%;
    min-width: 20px;
    min-height: 20px;
    border: 2px dashed #999;
    box-sizing: border-box;
}
```

`width:100%; height:100%` follows the existing `.svy-listformcomponent` rule so the placeholder fills the component bounds as defined by the form's CSS position. The `min-width/height` guard ensures at least a pixel-visible element even for zero-size placements.

### 3.3 Why not apply `invisible_element` CSS class directly

The `invisible_element` CSS class (`opacity: 0.3; border: 2px dashed`) is applied to the `svy-wrapper` div that wraps the component in the designer template. It is driven by `item.model.svyVisible === false`. We cannot reuse it here because:

1. The placeholder needs to fill the component bounds, not just show a border on an empty element.
2. Changing `svyVisible` to force the class would have unintended side effects on the server model.

A dedicated CSS class is the safer and more explicit approach.

### 3.4 Scope of change

Only `listformcomponent.ts` (template) and `listformcomponent.css` need to change. No Java, no outer-frame RFB designer code, no ghost handler changes are required.

## 4. Implementation plan

1. **Edit `listformcomponent.ts` template** — add the designer placeholder `@if` block after the two existing `@if` blocks inside `@if (!useScrolling)`.
2. **Edit `listformcomponent.css`** — add the `.svy-listformcomponent-designer-placeholder` CSS rule.
3. **Verify in the designer** — open a form with a `customlist` or `foundsetlist` component and confirm the component area shows the dashed border and can be selected/highlighted.
4. **Run existing Angular tests** for listformcomponent (`com.servoy.eclipse.ngclient.ui/node/run_tests.bat`) to confirm no regressions.

## 5. Acceptance criteria

- [ ] A `customlist` component placed on an absolute-layout form is visible in the form editor (shows a dashed-border placeholder if no containedForm is set).
- [ ] A `foundsetlist` component placed on an absolute-layout form is visible in the form editor.
- [ ] The component can be selected (click on it to get the selection decorator).
- [ ] The highlight outline appears on mouse-hover over the component area.
- [ ] A `customlist` component that *does* have a `containedForm` assigned continues to render correctly (no regression).
- [ ] Other LFC-based third-party components (`lightboxgallery`, `treeview`) also show the placeholder.
- [ ] No new compilation errors in the Angular build.

## 6. Out of scope

- Rendering actual live preview data inside the LFC placeholder in design mode.
- Fixing the same symptom for non-LFC components (those use a different rendering path).
- Changes to the Java ghost handler or the outer-frame RFB designer.

## 8. Testing notes

Automated test generation was attempted but not completed (Angular/Jasmine test scaffolding
could not be produced by the agent). Manual testing was performed instead:

- Opened the `frmEditor` solution in Servoy Developer
- Placed `customlist` and `foundsetlist` components on a form without setting `containedForm`
- Confirmed both components are now visible and selectable in the form editor (blue selection
  border appears; components can be clicked and highlighted)
- Confirmed runtime browser rendering is unaffected

## 9. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Are `lightboxgallery` and `treeview` also based on `servoycore-listformcomponent`? If they use a completely different spec/component base the fix may not apply to them automatically. | Dev | open |
| Should the placeholder show the component's `name` label as text (similar to how ghost labels work for out-of-bounds components)? | Product | open |
