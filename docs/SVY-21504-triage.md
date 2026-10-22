# Triage Report — SVY-21504

**Verdict:** NEEDS_INPUT

## Reported problem
Dragging a "Success" button from the palette into the form editor produces a button
that is too small to fit its own label — it renders as `Suc...` (text truncated/clipped)
instead of showing `Success`.

The two attached screenshots show:
- `image-20260924-130054.png` — the palette, where the **Success** preview renders as a
  correctly-sized green button labelled "Success".
- `image-20260924-130036.png` — the result after drop: a small **green** button in the
  editor whose text is clipped to "Suc…", with selection handles around it.

The ticket proposes **no** solution — it only reports the symptom.

## Root-cause assessment
The green styling on the dropped button confirms a **style variant** ("Success") was
applied, so the relevant drop path is the palette **variant drag**, not a plain
component drag. But there are two distinct code paths that each set a button's size on
drop, and they fail in materially different ways. The ticket does not contain enough
detail to confirm which one actually produced the screenshot, nor what the resulting
width was.

**Path A — style-variant drop (most likely, given the green button).**
The variant's size is *measured in the browser* and carried through, not taken from the
spec default:
- The Success variant is rendered inside a hidden variants iframe. When Angular asks for
  its size, `DesignFormComponent.sendVariantSizes()`
  (`com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts:475`)
  measures each variant element with `getBoundingClientRect()` and posts a `resizePopover`
  message; the per-variant width/height end up on each variant's model
  (`...component.ts:296-297`, `componentModel.size.width/height = variant.width/height`).
- On mouse-down the palette copies that measured size into the drag state:
  `PaletteComponent.onVariantMouseDown` →
  `this.draggedVariant.size = model.size`
  (`com.servoy.eclipse.designer.rfb/node/src/designer/palette/palette.component.ts:140`).
- On drop, for a variant, the palette sends that measured size straight to Java:
  `component.w = this.draggedVariant.size.width; component.h = ...height`
  (`palette.component.ts:291-293`).
- So the created button's width is whatever the Success variant *measured* in the hidden
  variants form at that moment. If the variant element was measured before its text/CSS
  fully laid out (or in a narrow/zero-box container), the width comes out too small and
  the real "Success" label no longer fits.

This path already has a history of mis-measuring: **SVY-21294** fixed a case where the
variant size came through as ~3px, and the palette spec still guards against it
(`palette.component.spec.ts:331` — "should use model size with realistic button
dimensions, not 3px"). A residual under-measurement (small-but-not-3px) would reproduce
exactly the "Suc…" clip in the screenshot.

**Path B — plain component drop (if the green came from a default style, not a variant).**
If the button were dropped as the generic palette "Button" with no measured size, the
palette sends no `w`/`h`, so Java falls back to the spec default size in
`CreateComponentCommand.createWebComponent`
(`com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/actions/handlers/CreateComponentCommand.java:460-470`):
it reads `designsize` from the spec, and for `bootstrapcomponents-button` that default is
`{"width":80,"height":30}` (`bootstrapcomponents/components/button/button.spec:14`). An
80×30 button is simply too narrow for the word "Success" at the default font, which would
also produce "Suc…". In this path the root cause is the **spec default `designsize`**,
not any designer code.

Both paths yield the same visible symptom (a too-narrow button), but the fix location is
completely different: Path A is a frontend measurement/timing issue in the variants
preview; Path B is a component-spec default (and arguably "working as designed" — 80px is
the historical default button width, and the user is expected to resize).

## Ticket premise check
The ticket proposes no approach, so there is nothing to refute. The premise that *"a
small button that doesn't fit its text" is a defect* is reasonable, but the ticket omits
the one fact that decides the whole investigation: **was "Success" dragged as a style
variant, or as the plain Button whose default happens to look green?** The green styling
in the result image points to a variant, but the palette screenshot shows both a generic
"Button" entry and a "Success" preview, so it is not conclusive.

## Approaches considered
1. **Fix the variant size measurement (Path A)** — ensure `sendVariantSizes()` measures
   the variant only after layout is stable, and/or clamp the measured width to the
   variant's natural content width.
   - Pros: targets the most likely path given the green button; isolated to the designer
     frontend; reuses the SVY-21294 pattern.
   - Cons: hard to confirm without a reproduction; risk of fixing a path that wasn't the
     one in the screenshot.
2. **Apply a minimum sensible size on drop** — floor the dropped button width/height in
   `CreateComponentCommand` or in the palette drop so a button is never created narrower
   than, say, its label needs.
   - Pros: fixes both paths at once; low blast radius.
   - Cons: arbitrary minimum; could override an intentionally small measured size; masks
     the real measurement bug rather than fixing it.
3. **Raise the `designsize` default for the button spec (Path B)** — change `80×30` to a
   width that fits common labels.
   - Pros: trivial; fixes the plain-drop path.
   - Cons: lives in the `bootstrapcomponents` repo, not this one; changes the default for
     *every* new button for everyone; does nothing for the variant path if that is the
     real cause.
4. **No code change** — the 80px default (or the measured variant size) is expected and
   the user resizes the button after drop.
   - Pros: no regression risk; a dropped component being resized is normal designer flow.
   - Cons: the palette preview promises a correctly-sized "Success" button and the drop
     silently delivers a clipped one — that mismatch is a genuine UX defect, so doing
     nothing is hard to justify if Path A is confirmed.

## Recommendation
This needs a human decision because the divergence test trips: the symptom maps to (at
least) two root causes in different repos/layers — the variant-size measurement in the
designer frontend (Path A) versus the component spec's `designsize` default (Path B) —
and the ticket lacks the detail to rule either out. A reproduction and one piece of
information (variant vs. plain drop, and the actual width of the created button) would
converge this immediately. See the questions below.

## Git history findings
- The size-default branch in `CreateComponentCommand.createWebComponent`
  (`designsize` → `size` → 80×30 → 200×100 fallback) dates to **4014f5b8738**
  (Rob Gansevles, 2023-09-08); the `designsize`-preferred lookup was layered on in
  **23ffcbda1cf** (Johan Compagner, 2026-08-18). This is long-standing, intentional
  default behaviour, not a recent regression.
- The variant measurement path has prior art: **SVY-21294** added the
  "realistic button dimensions, not 3px" guard still present in
  `palette.component.spec.ts:331`, and **SVY-21483** ("Hover size for buttons during dnd
  is huge", commit `f2cebd0fc8`) reworked the non-variant drag-preview sizing in
  `designform_component.component.ts:352-367` to prefer a measured size over `model.size`.
  Both show the drag/variant sizing has been actively churned recently, which makes a
  residual under-measurement in the variant path (Path A) plausible — but none of these
  commits is an obvious single regression that introduced *this* ticket's symptom.
- The `bootstrapcomponents-button` `designsize` default of `{80,30}`
  (`bootstrapcomponents/components/button/button.spec:14`) lives in a different repo
  (`C:\Users\merae\git\bootstrapcomponents`), not in this one.

## Questions for the reporter
1. When you dragged "Success", did you drag the **style-variant** preview (the green
   "Success" chip under the Button entry), or the plain **Button** component? The result
   button is green, which suggests the variant — can you confirm?
2. What are the exact width/height of the button that was created (select it and read the
   size, or check its `size`/`designsize` in the properties view)? Is it 80×30, or some
   other small value?
3. Is this a **regression** — did the same drag produce a correctly-sized "Success" button
   in a previous Developer version? If so, which version did it last work in?
4. Does it reproduce only for the "Success" variant, or for any button variant (and for
   the plain Button too)? Does it depend on the target form being absolute vs. responsive
   layout?
