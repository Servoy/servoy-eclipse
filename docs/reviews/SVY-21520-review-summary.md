# SVY-21520 — Peer-review summary

**AG Grid in collapsible not rendering the whole foundset**

## Risk verdict

**LOW.** The fix is small, correct for its actual purpose, and has no security surface. The "header re-show" path raised during review is not a real regression — see Context below — and no other finding survived verification.

## Reviewed scope

`Servoy/aggridcomponents` @ `df3e6cbd3489ce4eeca05a53965d5c799c888a1d` ("SVY-21520 render all rows when grid header is hidden via CSS [ai]", Gabi Boros, 2026-10-01) — a single commit on `master`, +54/-0, identical guard in `datagrid.ts` and `powergrid.ts`. Fix version 2026.9.0.

The fix adds a guard at the top of `sizeHeader()`: if the new helper `isHeaderHiddenByCSS()` detects `display:none` on `.ag-header` (or an ancestor up to the grid root), it reports `headerHeight = 0` and returns early. This corrects ag-grid reserving header space in `autoHeight` layout, which was stealing body-viewport height and dropping the last row(s).

## Context (author clarification, 2026-10-09)

The real-world trigger is the **svyCloud** solution: it hides the AG Grid header by applying a custom CSS rule (`display:none` on `.ag-header`) via a styleClass, instead of using the correct mechanism — setting `gridOptions.headerHeight` to `0`. svyCloud has many places doing this CSS-hack already, so migrating all of them to the proper grid setting was impractical. The fix is a **compatibility shim**: it detects this specific CSS hack and, when present, applies the correct `headerHeight: 0` grid setting on the component's behalf — without requiring svyCloud (or any other solution with the same hack) to be rewritten.

It is **not** meant to support toggling the header hidden/shown at runtime. Nobody removes a CSS "hide header" hack at runtime to show the header again — that would be an unusual use of an already-acknowledged hack. The correct guidance for anyone who *does* want to show/hide the header dynamically is to use `gridOptions.headerHeight` directly, which already works correctly in both directions (confirmed: SVY-13737 made header height settable to 0, and the normal measurement path is untouched by this fix).

**Conclusion:** the "header stays at 0 after CSS un-hide" behavior raised during the regression review is real as a matter of code (`svyOnChanges()` has no `styleClass` case, so nothing re-triggers `sizeHeader()` on a class change), but it does not justify additional code. Adding a handler for a scenario nobody will exercise — on a component that is already large — is not worth the extra surface. This finding is retracted as a blocking concern; it is kept below only as a documented, intentionally-not-handled edge case.

## Manual test plan

**Verifying the fix**
1. Reproduce the ticket: two AG Grids in a collapsible; drag rows from one to the other so the target foundset grows; confirm every row renders (no clipped last row).
2. Put a grid in `autoHeight` (responsiveHeight < 0) with a `no-header` styleClass that sets `.ag-header { display:none }`; confirm all rows render and no empty header band remains.

**Regression checks**
1. **PowerGrid pinned header-text row + hidden header:** confirm the pinned top row isn't clipped or doubled with `headerHeight: 0`.
2. **Floating filters + hidden header:** confirm the filter row layout stays sane.
3. **Explicit `gridOptions.headerHeight`** still honoured with a visible header; **SVY-13737** (header height settable to 0) still works.
4. **Large-grid column-resize drag** (wide grid, many rows): confirm no new perceptible lag from the added `isHeaderHiddenByCSS()` DOM walk.

**Surfaces:** DataGrid (foundset, server-side) and PowerGrid (dataset, client-side) — identical change in both. `autoHeight` layout is where the symptom lives.

**Automated checks:** from `aggrid/` — `npm run build`, `npm run lint`, `npm test` (`ng test --no-watch` + the Chromium browser suite); all must pass with zero lint warnings. No existing spec covers the new behaviour.

## Possible improvements / follow-ups

- **Not planned — documented edge case:** toggling a CSS-based header hide/show at runtime (e.g. via a dataprovider-driven `styleClass`) will leave the header at `headerHeight: 0` after showing it again, because `svyOnChanges()` has no `styleClass` case to re-trigger `sizeHeader()`. This is intentionally not handled: the proper way to control header visibility dynamically is `gridOptions.headerHeight`, which already works correctly in both directions. No code change proposed.
- **Test coverage (minor, optional):** the hidden→0 path has no automated test; the existing Chromium browser-spec harness could assert it directly if desired. Not required for this fix.
- **Cost micro-optimisation (very low priority):** `isHeaderHiddenByCSS()` runs a recursive DOM walk + `getComputedStyle` on every `sizeHeader()` call. Could short-circuit when there are zero header cells, or locate `.ag-header` directly — not worth it unless profiling shows a real cost.

## Security

NONE. The helper reads only DOM the component rendered and the computed CSS of those elements, producing one boolean that drives a single layout decision. No injection, I/O, dependency, or auth/data-exposure change.
