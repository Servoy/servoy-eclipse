# Spec: SVY-21504 — DND a style-variant button creates a too-small button in the editor

## 1. Goal
When a developer drags a style-variant preview (e.g. the green "Success" button) from the
palette into the form editor, the created button must keep the natural size it is rendered
at with its full variant style classes (e.g. `btn btn-success btn-lg`), instead of
collapsing to the component spec default (80×30). The variant's colour classes already
reach the element; the regression is that its **size** does not — the padded `btn-lg` size
is overridden by the plain-button fixed size. There is no server/spec size for a variant
(a variant is only a named set of CSS classes), so the natural size exists only once the
classes are rendered. The fix measures that rendered size in the hidden VariantsForm preview
and carries it through mouse-down → drop to the created persist.

> **Revision note (post-review / live testing):** Several earlier attempts seeded or measured
> per-variant sizes inside the hidden VariantsForm (`sendVariantSizes`) across every variant in
> `DesignFormComponent`. They were brittle — they measured the wrong element (the zero-box
> custom-element host or a flex-stretched wrapper) and depended on the 50 ms popover interval
> having run — and were reverted. Live debugging (DOM inspector) then pinned down the real
> chain:
>
> 1. **Regression source:** the palette model carries `designsize: {80,30}` but
>    `size: {undefined}`. SVY-19023 moved the design size to the server-only `designsize`
>    property, so `model.size` is now `undefined` for the dropped variant and the drop fell
>    back to the plain spec default. Each variant has a *different* natural size and the server
>    has no per-variant size, so the natural size can only come from a live render.
> 2. **Where to measure:** the fix lives entirely in `PaletteComponent` — it already owns the
>    popover and the mouse-down. `onVariantMouseDown` measures the **one variant the user
>    grabbed**, in the `VariantsForm` iframe, via `elementFromPoint(pageX, pageY)` → the real
>    rendered control (walking to the `bts-*`/`<button>`/`<label>` element, never the
>    flex-stretched `svy-layoutcontainer`/`variant_item` wrapper, whose width is clamped by the
>    narrow popover column and made a long label measure too small).
> 3. **Box-model correction:** on the form the dropped button is wrapped in `.svy-wrapper`,
>    whose component CSS (`.svy-wrapper > * > .bts-button { width:100%; padding:0 12px;
>    overflow:hidden; text-overflow:ellipsis }`) adds 12px horizontal padding and ellipsizes.
>    The popover button is NOT in a `.svy-wrapper`, so its box has no such padding — reusing it
>    verbatim left the text a few px short and clipped ("Prim…"/"Roun…"). So the measurement
>    sizes the dropped button for the width the **text** needs: `scrollWidth` − the popover
>    control's own padding (bare text width) + the form's `2×12px` padding + the control's
>    borders, max'd with the measured box, `Math.ceil`, +2px sub-pixel buffer.
>
> **Final approach:** all in `PaletteComponent` — no `DesignFormComponent` / `sendVariantSizes`
> / `createVariants` change, no interval timing dependency, no per-variant loop. The measured
> size is written onto `draggedVariant.size` and into the `model` posted for the drag preview,
> and the drop payload (`onMouseUp`) forwards `w`/`h` only when finite `>0`.

## 2. Background

### 2.1 Two drop paths, this spec covers only the style-variant path
The Triage report (`docs/SVY-21504-triage.md`) identified two code paths that each set a
dropped button's size, and the reporter confirmed the failing case is **Path A — the
style-variant drop** (the green "Success" chip under the Button palette entry), not the
plain Button. The reporter confirmed the created button measured 80×30 — exactly the
`bootstrapcomponents-button` spec `designsize` default — which proves the variant's
measured size never carried through to the created persist. It is a regression (it produced
a correctly-sized "Success" button in a previous Developer version) and reproduces only for
the variant drop. This spec therefore scopes the fix to the designer-frontend variant path.
Explicitly out of scope: the `bootstrapcomponents` spec default and any blanket minimum-size
floor on the plain-button path.

### 2.2 The variant preview / drag / drop pipeline
The variant feature spans three Angular layers plus the Java create handler:

1. **Variant data source (Java → editor frontend).**
   `EditorServiceHandler.getVariantsForCategory` (`com.servoy.eclipse.designer/.../EditorServiceHandler.java:643-653`)
   returns a `JSONArray` from `fl.getVariantsHandler().getVariantsForCategory(category)`.
   Each entry is a *style* definition only — in the frontend it is typed as
   `Variant { name; category; displayName; classes }`
   (`com.servoy.eclipse.designer.rfb/node/src/designer/services/editorsession.service.ts:620-625`).
   **There is no width/height in a `Variant`.**

2. **Variant preview rendering (content iframe).**
   `VariantsContentComponent.sendStylesToVariantsForm`
   (`com.servoy.eclipse.designer.rfb/node/src/designer/variantscontent/variantscontent.component.ts:79-105`)
   posts `createVariants` with `{ variants, model, name }` to the hidden `VariantsForm`
   iframe. `DesignFormComponent` (content iframe) handles `createVariants`
   (`com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts:269-310`)
   and builds a `ComponentCache` per variant. There it does:
   ```
   const layout = { width: variant.width + 'px', height: variant.height + 'px' };   // :288
   ...
   componentModel.size!.width  = variant.width;    // :296
   componentModel.size!.height = variant.height;   // :297
   ```
   Because `variant.width`/`variant.height` **do not exist** on a `Variant`, `layout`
   becomes `"undefinedpx"` and `componentModel.size.width/height` become `undefined`. The
   variant element therefore renders at its CSS/content size (correct on screen), but its
   *model* `size` is never populated with the real measured dimensions.

3. **Popover sizing (measurement that currently goes nowhere useful for the drop).**
   `DesignFormComponent.sendVariantSizes`
   (`.../designform_component.component.ts:475-489`) measures the laid-out variant elements
   with `getBoundingClientRect()` but aggregates them into a single popover width/height
   (`resizePopover` → `VariantsPreviewComponent.setPopoverSizeAndPosition`). This measured
   size is used only to size the popover; it is **not** written back per-variant onto each
   variant's `ComponentCache.model.size`.

4. **Mouse-down → drag state (palette).**
   On variant mouse-down the content iframe posts `onVariantMouseDown` with the selected
   variant's `model` (`.../designform_component.component.ts:522`). `PaletteComponent.onVariantMouseDown`
   (`com.servoy.eclipse.designer.rfb/node/src/designer/palette/palette.component.ts:137-172`)
   copies `this.draggedVariant.size = model.size`. Since `model.size` was never populated with
   the measured size (step 2), `draggedVariant.size` carries `{ width: undefined, height: undefined }`
   (or NaN).

5. **Drop → payload to Java (palette).**
   `PaletteComponent.onMouseUp` (`palette.component.ts:291-299`) sets
   `component.w = this.draggedVariant.size!.width; component.h = this.draggedVariant.size!.height`.
   With undefined/NaN dimensions, the drop payload's `w`/`h` are invalid.

6. **Java create handler falls back to the spec default.**
   `CreateComponentCommand.CreationOptions.fromJSON` reads `new Dimension(args.optInt("w"), args.optInt("h"))`
   (`.../CreateComponentCommand.java:1204`). `optInt` of a missing/NaN value yields `0`, so the
   size is `EMPTY_SIZE (0,0)`. In `createWebComponent` (`CreateComponentCommand.java:460-470`)
   `EMPTY_SIZE.equals(args.getSize())` is true, so it applies the spec `designsize` default →
   `bootstrapcomponents-button` = 80×30. That is exactly the size the reporter saw.

### 2.3 Prior art (SVY-21294 and SVY-21483)
- **SVY-21294** added the palette-spec guard "should use model size with realistic button
  dimensions, not 3px" (`palette.component.spec.ts:331`), confirming this variant-size path has
  mis-measured before.
- **SVY-21483** (`.../designform_component.component.ts:352-367`) established the pattern of
  preferring a **real measured size** (`getBoundingClientRect()`) over the design-time
  `model.size`, with a positive-number guard (`typeof ... === 'number' && > 0`) and a sensible
  fallback. The fix here should reuse that measurement/guard pattern: capture the real measured
  size for each variant and feed it through, falling back to a non-destructive value when the
  measurement is not available, never overriding a plain (non-variant) drop.

### 2.4 Git history (confirmation)
- The variant-size assignment `componentModel.size.width/height = variant.width/height`
  (`designform_component.component.ts:296-297`) traces through formatting commit
  `d1794cf2fd` back to the original variants work (`SVY-17727`, commit `89fd21d859`), where the
  per-variant model size was wired to `variant.width/height`. The `Variant` type that
  `getVariantsForCategory` returns has never carried `width`/`height`, so these have been
  reading `undefined` — matching the "variant measured size does not reach the persist"
  root cause. (The 80×30 fallback branch in `CreateComponentCommand` is long-standing,
  intentional default behaviour — `4014f5b8738`, `23ffcbda1cf` — not the regression.)
- The drop-side `component.w = draggedVariant.size.width` (`palette.component.ts:291-293`)
  is also long-standing (`89fd21d859` and earlier) and only passes along whatever
  `draggedVariant.size` holds, so the defect is upstream: the measured size never lands on the
  variant model.

## 3. Design

All changes are in `PaletteComponent`
(`com.servoy.eclipse.designer.rfb/node/src/designer/palette/palette.component.ts`), which
already owns the variants popover and receives the variant mouse-down. `DesignFormComponent`
is unchanged (it only resolves the clicked variant and posts its `model` as before).

### 3.1 Measure the grabbed variant in the popover at mouse-down
`PaletteComponent.onVariantMouseDown(pageX, pageY, model)` fires when the user presses a
variant chip. At that moment the clicked variant is fully laid out at its natural size in the
`VariantsForm` iframe. A new private `measureVariantSize(pageX, pageY)`:

- Resolves the `VariantsForm` iframe document (`getVariantsDocument()` — scans
  `editorContentService.getDocument().getElementsByTagName('iframe')` for `id === 'VariantsForm'`).
- `variantsDoc.elementFromPoint(pageX, pageY)` gives the clicked node, then
  `findVariantControl(...)` walks to the **real rendered control** (a `bts-*`/`<button>`/`<a>`/
  `<input>`/`<label>` element), explicitly **refusing** the flex-stretched layout wrappers
  (`svy-layoutcontainer`, `svy-wrapper`, `svy-responsive-wrapper`, `variant_item`,
  `variants_container`) whose width is clamped by the narrow popover column (which made a long
  label like "Primary"/"Round" measure too small).
- Computes a size that fits the **text** once the form re-pads the button (see 3.2):
  `textWidth = max(0, control.scrollWidth − popoverHPadding)`, then
  `width = ceil(max(rectWidth, textWidth + 2·FORM_BUTTON_HPADDING + border)) + 2`;
  `height = ceil(rectHeight)`. Returns `null` when nothing measurable / non-positive so the
  caller falls back.

### 3.2 Box-model correction (why the measured popover box is not enough)
On the form the dropped button is wrapped in `.svy-wrapper`, and the bootstrap button
component CSS `.svy-wrapper > * > .bts-button` sets `width:100%; padding:0 12px;
overflow:hidden; text-overflow:ellipsis`. In the popover the button is **not** inside a
`.svy-wrapper`, so it keeps its natural (smaller) padding and never ellipsizes. Reusing the
popover box width verbatim left the text a few px short and the form clipped it
("Prim…"/"Roun…"). So the measurement derives the bare text width from the control's
`scrollWidth` minus its own popover padding, then adds the form's `2×12px` (`FORM_BUTTON_HPADDING`)
plus the control's borders, so once the form re-pads to 12px/side the text still fits. A +2px
buffer absorbs sub-pixel rounding (`getBoundingClientRect` is fractional; the stored size is
integer).

### 3.3 The measured size reaches the drag state, the drag preview and the drop
- `onVariantMouseDown` sets `this.draggedVariant.size = measuredSize ?? model.size` and also
  writes `model.size = measuredSize` (when measured) so the `createElement` drag-preview
  message renders the dragged ghost at the real variant size instead of the 200×100 fallback.
- `onMouseUp`, inside `if (this.isDraggedVariant)`, forwards `component.w`/`component.h` from
  `draggedVariant.size` only when each is a finite number `> 0`; otherwise leaves them unset so
  any `snapData` size stands or Java applies its default — never sending `undefined`/`NaN`
  (which collapses to `EMPTY_SIZE` and the plain spec default). Strictly bounded to the variant
  branch, so the plain-button path is untouched.

### 3.4 Do NOT change the plain-button path or the spec default
- No change to `bootstrapcomponents/.../button.spec` `designsize` (different repo).
- No arbitrary global minimum size in `CreateComponentCommand` or the non-variant palette
  drop. The `EMPTY_SIZE` → `designsize` default behaviour stays as the correct fallback for a
  genuine plain drop with no measured size.

## 4. Implementation plan

1. **Measure the grabbed variant (palette).**
   In `palette.component.ts` add `measureVariantSize(pageX, pageY)`, `findVariantControl(...)`
   and `getVariantsDocument()`, and the `FORM_BUTTON_HPADDING` constant. `onVariantMouseDown`
   sets `draggedVariant.size` from the measurement (falling back to `model.size`) and feeds the
   measured size into the `model` posted for the drag preview.

2. **Guard the drop payload (palette).**
   In `onMouseUp`, only set `component.w`/`component.h` from `draggedVariant.size` when each is
   a finite number `> 0`; otherwise leave them unset. Strictly inside `if (this.isDraggedVariant)`.

3. **No `DesignFormComponent` / `createVariants` / `sendVariantSizes` change** — those stay as
   they were; `onVariantsMouseDown` just resolves the variant and posts its model.

4. **No Java change.** `CreateComponentCommand` applies a valid non-empty size and falls back
   to the spec default only for `EMPTY_SIZE`; once a valid size is sent the fallback no longer
   triggers.

5. **Tests (frontend).**
   - `palette.component.spec.ts` (`onVariantMouseDown (SVY-21504)`): with a mocked `VariantsForm`
     iframe whose `elementFromPoint` returns a `bts-*` control nested in a stretched wrapper, and
     a `defaultView.getComputedStyle` reporting the control's popover padding/border — assert the
     stored `draggedVariant.size` equals the text-fit formula (`scrollWidth − popover padding +
     2·12 + border`, ceil, +2), that the stretched 400px wrapper is never measured, that labels
     are sized for their text, that the measured size is fed into the drag-preview `model`, and
     that it falls back to `model.size` when the popover is not measurable.
   - `palette.component.spec.ts` (`onMouseUp` variant drop): `component.w`/`component.h` ARE set
     from a valid `draggedVariant.size`; are **not** set (no `undefined`/`NaN`) when the size is
     missing/undefined/NaN/≤0; an invalid variant size does not clobber a prior `snapData` size;
     the plain-button path (`isDraggedVariant === false`) is unaffected.
   - `designform_component.component.spec.ts` (`onVariantsMouseDown`): posts the resolved
     variant's `model` unchanged (measurement is no longer done here); DIV-click early return and
     the svy-id traversal still hold.

6. **Verify.** Per `node/AGENTS.md`: `npm run lint` (no new warnings) + the targeted specs in
   both `com.servoy.eclipse.designer.rfb/node` and `com.servoy.eclipse.ngclient.ui/node`.

## 5. Acceptance criteria
- [x] Dragging a style-variant (e.g. "Success", "Primary", "Round", or a label variant) from
      the palette into the form editor creates an element whose size matches the variant preview
      and whose **text fits** (not clipped to "Prim…"/"Roun…", not 80×30).
- [x] The drag preview (the ghost shown while dragging) also uses the measured variant size.
- [x] The dropped element's size reflects the measured variant size, not the
      `bootstrapcomponents-button` spec default.
- [x] A non-measurable (zero/`NaN`/≤0) variant never writes a bad size; the drop falls back
      without sending `undefined`/`NaN` to Java.
- [x] The plain **Button** drop (non-variant) is unchanged and still gets the spec default size.
- [x] The `bootstrapcomponents` button spec `designsize` default is NOT changed; no arbitrary
      global minimum size is introduced; no Java change.
- [x] `npm run lint` passes with no new warnings and the targeted specs pass in both
      `com.servoy.eclipse.designer.rfb/node` and `com.servoy.eclipse.ngclient.ui/node`.

## 6. Out of scope
- Changing the `bootstrapcomponents-button` (or any component) spec `designsize` default.
- Adding a blanket minimum button size on the plain (non-variant) drop path.
- Any change to `CreateComponentCommand`'s existing `EMPTY_SIZE` → `designsize` fallback
  (unless steps 1–3 prove the size still arrives empty).
- Variants of other components / label variants beyond confirming the button case is fixed
  (the fix is generic to the variant path, but the ticket's reproduction is the button).
- Absolute vs. responsive layout differences beyond what the shared variant path requires.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Does the per-variant measurement need to run on a stable layout tick (e.g. after the popover is shown) to avoid a zero-box read, or is the existing `sendVariantSizes` timing (50ms interval, skip first query) already late enough? Resolve during implementation; the SVY-21483 `>0` guard covers the bad-read case regardless. | implementer | open |
| Is the `model` object posted on `onVariantMouseDown` the same instance whose `size` is updated by measurement, or a pre-measurement snapshot? Confirm during implementation (step 2). | implementer | open |
