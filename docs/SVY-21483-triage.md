# Triage Report — SVY-21483

**Verdict:** PROCEED

## Reported problem
In the responsive form editor, when dragging a button (component) from one row to
another, the drag preview ("hover") is rendered much bigger than the original element.
The ticket includes `hover.jpg` (an oversized button preview during dnd) and a sample
solution `respSmp.servoy`.

The ticket describes only the symptom; it proposes no solution.

## Root-cause assessment

The responsive drag-and-drop preview is a **separate element** rendered inside the
content iframe, not the actual dragged DOM node — and it is sized from the persisted
design-time `model.size`, which does not match the size the element actually renders at
inside a responsive (flex) row.

Flow:

1. `dragselection-responsive.component.ts` `onMouseMove()` starts the drag and posts
   `createDraggedComponent` to the iframe with just the element's `svy-id` and the
   `dragCopy` flag — **no size information**
   (`com.servoy.eclipse.designer.rfb/node/src/designer/dragselection-responsive/dragselection-responsive.component.ts:113`).

2. The iframe handles `createDraggedComponent` in
   `designform_component.component.ts` and, for a component (non-layout), builds the
   preview `draggedElementItem` with a `layout` derived from `model.size` (falling back
   to `200 × 100`):
   ```
   const elWidth  = insertedClone.model.size ? insertedClone.model.size.width  : 200;
   const elHeight = insertedClone.model.size ? insertedClone.model.size.height : 100;
   insertedClone.layout = { width: elWidth + 'px', height: elHeight + 'px' };
   ...
   this.draggedElementItem = new ComponentCache('dragged_element', ..., insertedClone.layout, ...);
   ```
   (`com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts:349-379`).

3. The preview is rendered by the template as an absolutely-positioned
   `#svy_draggedelement` wrapper whose width/height come from that `layout`
   (`designform_component.component.ts:66,76`; sizing applied by the `svyContainerLayout`
   directive in `com.servoy.eclipse.ngclient.ui/node/src/servoycore/addattribute.directive.ts:36-46`).

In a responsive layout, a button's actual rendered size is determined by the flex
container / CSS, and is typically far smaller than its `model.size` design width/height
(or the `200 × 100` fallback when `model.size` is absent). Because the preview is sized
from `model.size` rather than from the element as actually rendered, it appears "huge"
compared to the original — exactly the reported symptom.

**Corroborating asymmetry:** the *absolute*-layout drag
(`dragselection/dragselection.component.ts`) does not have this problem because it drags
the real DOM node (or a `cloneNode(true)` of it) and measures it with
`getBoundingClientRect()` (`dragselection.component.ts:228-229,262-267`) — so its preview
always matches the rendered size. Only the responsive path constructs a preview sized
independently of the live element.

The three ways the mismatch can arise — (a) `model.size` unset → `200 × 100` fallback,
(b) `model.size` present but larger than the flex-rendered size, (c) preview not subject
to the same responsive/flex constraints as the real element — all converge on the same
root cause: **the responsive drag preview is sized from the model instead of from the
element as it is actually rendered.** No reporter input is needed to disambiguate.

## Ticket premise check
The ticket proposes no approach, so there is nothing to challenge there. The problem is a
genuine Servoy Developer (RFB designer) defect, reproducible with the attached sample, and
localized to the responsive dnd preview sizing. It is not user misconfiguration nor a
third-party issue.

## Approaches considered

1. **Send the real rendered size from the responsive drag source and use it for the
   preview.** In `dragselection-responsive.component.ts`, at drag start the actual
   `dragNode` is available (the code already reads `dragNode.clientWidth/clientHeight` at
   line 87). Capture its rendered size (`getBoundingClientRect()` / `clientWidth` /
   `clientHeight`) and include it in the `createDraggedComponent` message; in
   `designform_component.component.ts`, prefer that measured size over `model.size` when
   building `draggedElementItem.layout`.
   - Pros: fixes the actual root cause; mirrors what the absolute-layout path already does
     with `getBoundingClientRect()`; preview matches the original the user sees; minimal,
     well-scoped change across the two known files.
   - Cons: touches the iframe message contract (add an optional size field) and the
     ngclient designform handler; needs the fallback preserved for the case where no size
     is supplied.

2. **Cap/clamp the preview size in the iframe** (e.g. never exceed some max, or shrink to
   fit the row).
   - Pros: contained to one file.
   - Cons: a heuristic, not a fix; still wrong whenever the design size differs from the
     rendered size; brittle.

3. **Style `#svy_draggedelement` so its width/height auto-fit its content** instead of
   being driven by `layout`.
   - Pros: no message-contract change.
   - Cons: the preview is absolutely positioned and detached from the flex row, so it has
     no flex context to shrink to; likely reintroduces `200 × 100`-ish sizing; risk of
     regressing the working absolute-layout and layout-container drag previews.

4. **No code change.**
   - Pros: none.
   - Cons: this is a real, reproducible visual defect with an attached sample; leaving it
     is not appropriate.

## Recommendation
**PROCEED with Approach 1** — measure the dragged element's real rendered size in
`dragselection-responsive.component.ts` and pass it through the `createDraggedComponent`
message so `designform_component.component.ts` sizes the `draggedElementItem` preview to
match the element as rendered, falling back to the existing `model.size` / `200 × 100`
behaviour when no measured size is available. This addresses the root cause and aligns the
responsive preview with the already-correct absolute-layout preview.

Alternatives 2 and 3 are heuristics/styling patches that do not fix the underlying
model-vs-rendered mismatch and risk regressions; Approach 4 is rejected.

## Git history findings
- The component-preview sizing (`elWidth`/`elHeight` from `model.size`, fallback
  `200 × 100`) dates back to the original responsive dnd implementation
  (`c0234dcf3f SVY-16453 [ng2 designer] implement dnd from palette in responsive layout`);
  it has been carried through subsequent refactors (`33ac41ff48 SVY-20562 multiple
  selection copy and move - responsive form`, `d1794cf2fd`/`48c3906039` SVY-19023
  formatting/signal migration). The *code* is the original design.

- **REGRESSION TRIGGER (correction after further investigation, independently verified
  in a second pass):** the *symptom* IS a regression from the Angular 22 migration, even
  though the preview code is old. Commit `872f86e SVY-19023 remove size/location from
  component specs - handled as internal by framework` deleted the client `size` model
  property (`"size": {default: {width:80, height:30}}`) from 21 bootstrap component specs
  (e.g. `bootstrapcomponents/components/button/button.spec`) and replaced it with a
  **server-only** `designsize` property (`f9d70be`/`9033faf`, `"serveronly": true`,
  consumed only by Java via `IContentSpecConstants.PROPERTY_DESIGN_SIZE`).

  **Verified empirically, not just from the commit message:** the commit message claims
  removed sizing is "handled as internal by framework", but reading
  `DefaultComponentPropertiesProvider.addDefaultComponentProperties()` (Java) shows the
  size/location/anchors re-injection block is **commented out** — it does NOT add `size`
  back to any component's client model. Two independent server-side chokepoints confirm
  `designsize` never reaches the browser: `ComponentTemplateGenerator.genereateSpec()`
  skips `isServerOnly()` properties when generating Angular template bindings (with a
  dedicated regression test, `ComponentTemplateGeneratorTest.internalPropertiesNotInOutput()`,
  asserting `[size]="..."` never appears in output), and `FormElement.propertiesForTemplateJSON()`
  likewise skips server-only properties when building the JSON sent to the client. So
  `model.size` is genuinely `undefined` at runtime for these components — not merely
  theorized to be.

  Because `model.size` is `undefined`, the old `model.size ? … : 200 × 100` fallback in
  the responsive drag preview now always hits `200 × 100`. Before the migration, the
  button's `size` default of `80 × 30` masked the bug. This is why it appeared "all of a
  sudden".

  The recommended fix (measure the rendered element) still applies and is root-cause-
  agnostic — it stops depending on `model.size` for this preview entirely, so it is
  correct regardless of why `model.size` is what it is. Restoring a client `size`
  property would re-add data the framework deliberately keeps server-only, would only
  help spec-declared components, and would still be the *design* size rather than the
  *flex-rendered* size — so it would not fully fix the mismatch even where applied.

  **Second-pass check for alternative causes (ruled out):** no CSS change, no
  `AddAttributeDirective.ngOnChanges` timing/signal bug, and no stale-value/race
  condition was found. Angular 22 signal inputs still fire `ngOnChanges` normally (this
  is documented as intentional in the RFB project's own `AGENTS.md`), and `layout` is
  set exactly once per drag with no second pass that could clobber or race it. The bug is
  that the one assignment computes the wrong number, not a timing issue.
- Precedent: `177ac53c5a SVY-21294 fix wrong min-width for components with variants`
  fixed a closely related problem — it removed an incorrect DOM-based width measurement in
  `onVariantsMouseDown()` and instead sent the model's pre-computed size. That was the
  *variant palette* case (where the model size is the intended size). The present case is
  the opposite direction: a *live component in a responsive row* whose rendered size, not
  its model size, is the correct preview size — so measuring the rendered element (as the
  absolute-layout path already does) is the right source here.
- No prior spec for SVY-21483 exists in `docs/`.
