# Spec: SVY-21294 — Wrong min width for components with variants

## 1. Goal

Fix the incorrect min-width (3px) applied to buttons and labels when dropped from the variants popup onto a CSS position form editor. The dropped component should use the correct width as displayed in the variants preview.

## 2. Background

### 2.1 Variant drop flow

When a user clicks a variant in the variants popup, the following flow occurs:

1. The variants popup is rendered inside an iframe (`VariantsForm`) which uses the NG Client's `designform_component.component.ts` to render variant items.
2. On mousedown inside the variants iframe, `onVariantsMouseDown()` in `designform_component.component.ts` measures the clicked variant's DOM element and posts an `onVariantMouseDown` message to the parent (designer frame) with the model and measured size.
3. The designer's `palette.component.ts` receives this message and stores the size in `this.draggedVariant.size`.
4. On mouse-up (drop), the palette sends a `createComponent` WebSocket call to the Eclipse backend with `component.w` and `component.h` from the variant's measured size.
5. The backend (`CreateComponentCommand.java`) calls `CSSPositionUtils.setSize()` using the provided width/height, which becomes the element's CSS position `min-width`.

### 2.2 Root cause

In `designform_component.component.ts` at line 419–422, the width measurement uses an incorrect DOM traversal depth:

```typescript
const targetHeight = Math.ceil(targetElement!.getBoundingClientRect().height);
targetElement = targetElement!.firstElementChild;
const targetWidth = Math.ceil(targetElement!.getBoundingClientRect().width) + 3;
```

After finding the element with `svy-id` (the `variant_item` flex wrapper div), the code navigates to `firstElementChild` — the Angular component host element (e.g., `<servoydefault-button>`). This host element does not have intrinsic width dimensions; its `getBoundingClientRect().width` returns 0, resulting in `0 + 3 = 3` pixels.

By contrast, the `sendVariantSizes()` method (line 389) correctly measures at `variant.firstChild.firstChild` — the actual rendered HTML element inside the host (e.g., the `<button>` element).

### 2.3 Additional issue: redundant DOM measurement

The variant model already has correct `size.width` and `size.height` values, set during variant creation (lines 233–234):

```typescript
componentModel.size!.width = variant.width;
componentModel.size!.height = variant.height;
```

These values come from the backend's variants definition. The `onVariantsMouseDown()` method unnecessarily overwrites these with DOM measurements (which are unreliable due to the wrong traversal depth).

### 2.4 Git history

- The `onVariantsMouseDown()` method was introduced by Johan Compagner in commit `6a95cafbc1e` (2026-06-06) during the initial variants implementation.
- The `+ 3` padding was intentionally added (comment: "not adding 3 px then the text content is getting clipped after drop").
- Commit `a6e8005ce13` (Gabi Boros, 2026-07-28) only added `strictNullChecks` type assertions without changing logic.

## 3. Design

### 3.1 Fix the width measurement

The fix should use the model's pre-computed size values instead of measuring from the DOM, since the model already holds the correct dimensions from the variant definition. If DOM measurement is still desired as a fallback, it should use `firstElementChild.firstElementChild` (matching `sendVariantSizes()`'s approach) and enforce a minimum width.

### 3.2 Proposed code change

In `com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts`, method `onVariantsMouseDown()`:

**Option A — Use model size (preferred):** Remove the DOM measurement of width and use the model's existing `size` values directly. The model is already populated with correct dimensions at variant creation time.

```typescript
if (selectedVariant) {
    this.windowRefService.nativeWindow.parent.postMessage({
        id: 'onVariantMouseDown',
        pageX: event.pageX,
        pageY: event.pageY,
        model: (selectedVariant.items![0] as ComponentCache).model
    }, '*');
}
```

**Option B — Fix DOM measurement depth:** If DOM measurement is needed (e.g., the rendered size differs from the spec-defined size due to CSS), fix the measurement to traverse to the correct depth and add a minimum width guard:

```typescript
const targetHeight = Math.ceil(targetElement!.getBoundingClientRect().height);
targetElement = targetElement!.firstElementChild?.firstElementChild ?? targetElement!.firstElementChild;
const targetWidth = Math.max(
    Math.ceil(targetElement!.getBoundingClientRect().width) + 3,
    20  // minimum reasonable width
);
```

### 3.3 Backend consideration

The backend code in `CreateComponentCommand.java` for buttons/labels (lines 296–316) does not handle the `variant` property from the JSON args. The `variant` style class is sent but not applied to the `GraphicalComponent`. This is a separate concern but worth noting for a follow-up fix if the variant styling needs to be persisted on the component.

## 4. Implementation plan

1. In `com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts`, modify `onVariantsMouseDown()`:
   - Remove lines 419–422 (DOM-based width/height measurement)
   - Remove lines 434–436 (model size overwrite with DOM measurements)
   - Send the model directly without modifying its `size` property (it already has correct values)
2. Verify the `sendVariantSizes()` method still works correctly (it uses its own measurement and is unaffected).
3. Test dropping buttons and labels with variants onto CSS position forms — verify the component gets the correct width matching the variant's defined size.
4. Test that the variant popup still renders and resizes correctly.

## 5. Acceptance criteria

- [ ] Dropping a button with a variant from the palette onto a CSS position form results in a component with a reasonable min-width (matching the variant's defined width), not 3px.
- [ ] Dropping a label with a variant from the palette onto a CSS position form results in a component with a reasonable min-width (matching the variant's defined width), not 3px.
- [ ] Dropping variants on responsive form layouts continues to work correctly.
- [ ] The variants popup continues to size itself correctly via `sendVariantSizes()`.
- [ ] Existing non-variant palette drops (regular button/label) are unaffected.

## 6. Out of scope

- Applying the `variant` style class property on the backend for `GraphicalComponent` (buttons/labels) — this is a separate enhancement.
- Changes to the `sendVariantSizes()` measurement logic (it already works correctly).
- The `+ 3` pixel padding logic — if DOM measurement is kept, the padding intent is valid (prevent text clipping).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the model's pre-defined `size` be used directly (Option A), or should the DOM measurement be fixed (Option B)? Option A is simpler and more reliable. | Dev | open |
| Is the `variant` property persistence on `GraphicalComponent` needed in this ticket or a follow-up? | PM | open |
