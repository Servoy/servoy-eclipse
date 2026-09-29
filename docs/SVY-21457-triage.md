# Triage Report — SVY-21457

**Verdict:** PROCEED

## Reported problem

A List Form Component (LFC) placed in a responsive form, containing a responsive
form component, with `responsiveHeight = -1` and `responsivePageSize = 0`, does not
auto-size each row to its own content. Instead every row is rendered at the height of
the **first** measured row. Rows with more content are clipped/overflow, and rows with
less content leave large empty gaps.

Concretely (per the reporter and attachments `image-20260914-111513/533/553.png`):
- First render: all rows sized to the first item's height.
- After filtering so only a taller item is visible and pressing F5, the grid re-measures
  and then keeps using that height for all rows.
- After removing the filter, the previously measured height sticks for every entry.

The expectation: in listview mode with `responsiveHeight = -1`, each row's height should
be computed from its own content (per-item auto-height), not fixed to the first row.

The reporter and the architect (Gabor Boros) agree this is intended for relatively small
lists (10–30, rarely up to ~100 entries) — lazy-loaded huge lists with per-row auto-height
are explicitly *not* expected.

## Root-cause assessment

The behaviour is by design in the current code and lives entirely in the NG Client LFC:
`com.servoy.eclipse.ngclient.ui/node/src/servoycore/listformcomponent/listformcomponent.ts`.

Three cooperating pieces force a single uniform row height when
`!isInAbsoluteLayout() && responsiveHeight() < 0`:

1. **AG Grid per-row auto-height is explicitly disabled** for this case
   (`listformcomponent.ts:319`):
   ```ts
   columnDefs: [
     { cellRenderer: 'row-renderer',
       autoHeight: !this.servoyApi.isInAbsoluteLayout() && this.responsiveHeight() < 0 ? false : true }
   ]
   ```
   So the responsive `responsiveHeight = -1` path is the *only* one that turns `autoHeight`
   off; all other paths use `autoHeight: true`.

2. **A single measurement is taken from the first rendered row and applied globally**
   (`listformcomponent.ts:625-640`, `onRowRendererAfterViewInit`), guarded by
   `rowHeightMeasured` so it runs once:
   ```ts
   if (!this.rowHeightMeasured && !this.servoyApi.isInAbsoluteLayout() && this.responsiveHeight() < 0) {
       this.rowHeightMeasured = true;
       requestAnimationFrame(() => {
           const contentEl = elementRef.nativeElement.querySelector(':first-child');
           const measuredHeight = contentEl ? contentEl.scrollHeight : elementRef.nativeElement.scrollHeight;
           if (measuredHeight > 0) {
               agGrid.api.setGridOption('rowHeight', measuredHeight); // global rowHeight
               agGrid.api.resetRowHeights();
           }
       });
   }
   ```
   `row-renderer.component.ts:43-45` calls this from the first row's `ngAfterViewInit`.
   Because the guard latches `true`, only the first row's `scrollHeight` is ever used, and
   it "sticks" across re-renders and filters — exactly the reported symptom.

3. **`domLayout: 'autoHeight'`** is set for `responsiveHeight < 0`
   (`listformcomponent.ts:353`) so the grid container grows to fit all rows — but the rows
   themselves all use the single global `rowHeight`.

This matches the architect's own diagnosis in the ticket comment (2026-09-18).

## Ticket premise check

There is no incorrect "solution" baked into the ticket to push back on — the reporter only
describes the symptom, and the architect (Gabor Boros) already outlined the intended fix
direction in a comment, which the reporter accepted. The premise holds:

- The problem **is** in Servoy code (the LFC component), not user misconfiguration or a
  third-party defect. `responsiveHeight = -1` + `responsivePageSize = 0` is a supported,
  documented combination (auto-size to fit all rows, no scrollbar).
- It is a genuine functional gap, not "works as intended" — per-item auto-height is the
  natural expectation for `responsiveHeight = -1`, and every non-`-1`-responsive path
  already gets `autoHeight: true`.

The one nuance worth preserving: the current single-measurement approach was **deliberately
introduced** (see Git history) to fix a flicker/resize loop (SVY-21244). Any fix must not
reintroduce that flicker. This is a real tradeoff, not a reason to reject the ticket.

## Approaches considered

1. **Enable AG Grid native per-row `autoHeight` for the responsive `responsiveHeight = -1`
   case (recommended).**
   - Set `autoHeight: true` on the row column unconditionally (remove the special-casing at
     `listformcomponent.ts:319`).
   - Stop forcing a global `rowHeight` — remove the single-measurement block and the
     `rowHeightMeasured` guard (`listformcomponent.ts:157, 625-640`) and the
     `onRowRendererAfterViewInit` call in `row-renderer.component.ts`.
   - Ensure the row-renderer cell reports its natural `scrollHeight` (no forced
     `height: 100%` that would defeat AG Grid's measurement).
   - Keep `domLayout: 'autoHeight'` so the container still grows to fit.
   - Pros: each row sizes to its own content; matches user expectation; leans on AG Grid's
     built-in mechanism rather than a custom one-shot hack; naturally handles filters and
     re-renders.
   - Cons: AG Grid re-measures rows as they scroll in under serverSide + infinite scrolling
     — heavier for large lists. Must verify the SVY-21244 flicker/resize loop does not
     return (that fix touched both the measurement logic *and* the resize-observer guard;
     the resize-observer guard at `listformcomponent.ts:527-552` can stay). Needs testing
     with a vertical scrollbar appearing (the original flicker trigger).

2. **Measure every row individually instead of only the first, but keep a custom
   per-row height map.**
   - Extend `onRowRendererAfterViewInit` to measure each row's content and feed AG Grid
     a `getRowHeight` callback per node instead of a single global `rowHeight`.
   - Pros: avoids AG Grid's native autoHeight re-measurement cost; more control.
   - Cons: reimplements what AG Grid `autoHeight` already does; more code to maintain;
     easy to get the timing/`requestAnimationFrame` and re-measurement-on-data-change
     wrong; higher regression risk than approach 1.

3. **No code change — document the limitation and tell users to set a fixed
   `responsiveHeight` (e.g. 600).**
   - Basis: the reporter noted there is no issue with a fixed `responsiveHeight`; the
     architect noted per-row auto-height is heavier.
   - Pros: zero risk of reintroducing the SVY-21244 flicker; no work.
   - Cons: does not meet the accepted expectation for `responsiveHeight = -1` (auto-size to
     content). A fixed height cannot fit rows of differing content heights, which is the
     whole point of this bug. Both reporter and architect have already agreed a fix is
     wanted. Rejecting for `NO_ACTION` would ignore that agreement. Not recommended.

## Recommendation

**PROCEED with Approach 1** — enable AG Grid's native per-row `autoHeight` for the
responsive `responsiveHeight = -1` case and remove the single-first-row measurement hack.
This directly matches the architect's stated fix direction, which the reporter accepted,
and it delegates row sizing to AG Grid's proven mechanism instead of a custom one-shot
measurement.

Key implementation constraints for the downstream spec/implementation:
- Change `autoHeight` at `listformcomponent.ts:319` to `true` (or drop the conditional).
- Remove `rowHeightMeasured` (`:157`), the `onRowRendererAfterViewInit` body/measurement
  block (`:625-640`), and its invocation in `row-renderer.component.ts:43-45`.
- Do **not** set a fixed global `rowHeight` for this path so AG Grid measures each cell;
  review `getRowHeight()` usage at `:325` for this case.
- Verify the row-renderer template does not force `height: 100%` on the cell content.
- **Regression guard:** re-test the SVY-21244 scenario (small responsive list whose
  content causes a vertical scrollbar) to confirm the width flicker/resize loop does not
  return. The resize-observer's `newNumberOfColumns !== this.numberOfColumns` guard
  (`:532`) should remain in place; it is independent of the row-height measurement and was
  the other half of the SVY-21244 fix.
- Accept the documented caveat: best for small lists (10–100 rows), heavier for
  lazy-loaded large lists — both reporter and architect already agreed to this.

Alternatives: Approach 2 (custom per-row measurement) is a fallback if AG Grid native
autoHeight reintroduces the flicker and it cannot be otherwise contained. Approach 3
(no change) is not recommended given the agreed expectation.

## Git history findings

- The single-measurement / disabled-autoHeight behaviour was introduced intentionally in
  commit **268f34252a** — *"SVY-21244 ListFormComponent flickers on auto-height mode"*
  (Gabi Boros, 2026-07-15, fix version 2026.3.1). Before that commit the row column used
  `autoHeight: true` unconditionally. That commit:
  - changed `autoHeight` to `false` for `!isInAbsoluteLayout() && responsiveHeight() < 0`,
  - added the `rowHeightMeasured` field and `onRowRendererAfterViewInit` single-measurement
    logic,
  - and hardened the resize observer to only refresh when the **column count** actually
    changes (to stop a width flicker/resize loop caused by a vertical scrollbar toggling
    the width by ~6px).
- SVY-21244 (Closed, fix 2026.3.1) is therefore the source of the current behaviour. The
  fix for SVY-21457 must reverse the *row-height* portion of that change while keeping the
  *resize-observer* portion, so it does not revert an intentional flicker fix.
- No prior spec for SVY-21457 exists under `docs/`.
