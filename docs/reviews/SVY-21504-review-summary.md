# Review Summary: SVY-21504 DND a primary button creates a small button in editor

**Risk: MODERATE.** The new drop size comes from measuring the variants popover DOM plus a hard-coded 12px form padding (`palette.component.ts:186`). It is unverified at runtime and only covers `bts-*` buttons.

**Scope reviewed:** `servoy-eclipse` commit `ccc3c494c` (master, 2026.12).
**Release-target check:** MATCH. Fix Version 2026.12 equals the version on `origin/master` (2026.12.0), and the commit is on it.

## Manual test plan

**Verifying the fix**
1. Open a form in the designer. In the palette, open the Button variants popover and drag Success onto the form. The button should drop wide enough to show its full text, not 80x30 with clipped text.
2. Repeat with Primary, Round and other variants. The dropped size should match the popover size, within about 2px.

**Regression checks**
1. Drag a plain Button (no variant). It should still drop at the spec default.
2. Drag variants of a label, a text field and a custom package component. Check that the sizes are sensible.
3. Do the drag in an absolute-layout form and in a responsive form. Compare the drag ghost with the dropped result.
4. Scroll the variants popover, then drag a lower variant. The drop should have the right size.
5. Drag a variant with long text and check the width is acceptable.
6. Switch to a theme with different button padding, if one is available.

Automated checks (not run during the review): `npm test` and `npm run lint` in `com.servoy.eclipse.designer.rfb/node`, plus the ngclient.ui tests for `designform_component.component.spec.ts`.

## Possible improvements / follow-ups
- The 12px padding is copied from a bootstrap components CSS rule (`.svy-wrapper > * > .bts-button`). Consider reading it from the rendered form instead, so a theme change does not silently break sizing.
- `findVariantControl` can fall back to an inner element, and any size above 0 is accepted. Non-button variants (label, input, custom components) may drop at odd sizes.
- The measured size is also written into `model.size` for the drag preview. Check the ghost against the final render in responsive rows.
- There is no upper bound on the measured width. A very long variant text gives a very wide drop (cosmetic, the user can resize).
- Open questions for the author:
  1. Which tests were removed from `designform_component.component.spec.ts`, and why?
  2. How are non-button variants meant to be sized?
  3. Does `CreateComponentCommand` tolerate `w` and `h` being absent?
  4. Can the reporter's confirmations, which are only in the spec, be added to the issue?
