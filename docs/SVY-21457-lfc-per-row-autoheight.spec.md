# Spec: SVY-21457 — List Form Component does not auto-size properly

## 1. Goal

When a List Form Component (LFC) is placed in a responsive form, contains a responsive
form component, and is configured with `responsiveHeight = -1` (auto-size to fit all
rows, no scrollbar) and `responsivePageSize = 0`, each row must size itself to its own
content in listview mode instead of every row being locked to the height of the first
rendered row. Each row is measured explicitly once its content has settled, and the
measured height is fed back into AG Grid's server-side row model via a `getRowHeight`
callback, so filters and re-renders recompute heights correctly and no first-show flicker
is introduced.

## 2. Background

### 2.1 Reported behaviour

The reporter (Paolo Aronne) uses an LFC in a responsive form with a responsive form
component, wanting each item to auto-size (`responsiveHeight = -1`, `responsivePageSize = 0`).
Instead, the component measures the first item once and applies that height to every row:

- First render: all rows are sized to the first item's height.
- Filtering so only a taller item is visible, then pressing F5, re-measures and then keeps
  that height for all rows.
- Removing the filter: the previously measured height sticks for every entry.

Both the reporter and the architect (Gabor Boros) agree this is expected only for
relatively small lists (typically 10–30, rarely up to ~100 entries). Lazy-loaded huge
lists with per-row auto-height are explicitly not a target.

### 2.2 What `responsiveHeight` does today

In scrolling mode `responsiveHeight` controls only the LFC container height, not the
individual item height. Per the architect's diagnosis:

- `> 0`: fixed pixel container height, with scrollbar.
- `0`: grows to 100% of parent (parent needs a known height / flex-content layout).
- `-1`: container auto-sizes to fit all rows, no scrollbar.

Row heights are currently uniform for all rows regardless of the `responsiveHeight` value.

### 2.3 Root cause (in `listformcomponent.ts`)

The scrolling LFC uses an AG Grid `serverSide` row model. Three cooperating pieces force a
single uniform row height when `!isInAbsoluteLayout() && responsiveHeight() < 0` (this
predicate is the "per-row auto-height path", exposed as `isPerRowAutoHeight()`):

1. **AG Grid per-row auto-height is explicitly disabled** for this case, via a conditional
   on `columnDefs[].autoHeight`. Every other path (absolute layout, or responsive with
   `responsiveHeight >= 0`) uses `autoHeight: true`; only the per-row auto-height path turns
   it off.
2. **A single measurement is taken from the first rendered row and applied globally**
   (`onRowRendererAfterViewInit`), latched by a `rowHeightMeasured` field so it runs once and
   calls `agGrid.api.setGridOption('rowHeight', measuredHeight)` + `resetRowHeights()`.
   Because the guard latches, only the first row's `scrollHeight` is ever used, and it sticks
   across re-renders and filters — the reported symptom. `row-renderer.component.ts` invoked
   this from the first row's `ngAfterViewInit`.
3. **`domLayout: 'autoHeight'`** is set for `responsiveHeight < 0`, so the grid container
   grows to fit all rows — but rows all used the single global `rowHeight`.

### 2.4 Git history — why the current behaviour exists (do not fully revert)

This behaviour was introduced deliberately in commit **268f34252a** —
*"SVY-21244 ListFormComponent flickers on auto-height mode"* (2026-07-15, fix version
2026.3.1). Before it, the row column used `autoHeight: true` unconditionally. That commit:

- changed `autoHeight` to `false` for the per-row auto-height path,
- added the `rowHeightMeasured` field and the `onRowRendererAfterViewInit`
  single-measurement logic, and
- hardened the resize observer to only refresh when the **column count** actually changes
  (`newNumberOfColumns !== this.numberOfColumns`) to stop a width flicker/resize loop caused
  by a vertical scrollbar toggling the width by ~6px.

The SVY-21457 fix must reverse only the **row-height** portion of SVY-21244 while keeping
the **resize-observer** portion intact, so it does not reintroduce the flicker. No prior
spec for SVY-21457 exists under `docs/`.

## 3. Design

### 3.1 Approach actually implemented (native `autoHeight` was tried and rejected)

The initial approach — enabling AG Grid's native per-row `autoHeight` for the per-row
auto-height path and deleting the one-shot measurement — was implemented, tested, and then
**reverted** after live testing (manual verification against a running LFC solution)
reproduced the exact SVY-21244 flicker: native `autoHeight` attaches a `ResizeObserver` to
every cell, and the nested responsive form inside each row (bootstrap `12grid`,
`float`-based `.row`/`.col-*` columns, no clearfix) reflows across several layout passes
before settling. Each of those passes re-triggered AG Grid's own observer, causing a
remeasure/re-render loop that was visible as a rapid flicker on first show, with the
grid effectively collapsing to show only the first row while it looped.

The implemented design instead keeps native `autoHeight` **disabled** for the per-row
auto-height path (as before SVY-21457) and replaces the old *single first-row* measurement
with an explicit *per-row* measurement:

- `columnDefs[].autoHeight` is `!this.isPerRowAutoHeight()` — unchanged (`true`) for every
  path except the per-row auto-height path, where it is `false`.
- Each `RowRenderer` cell measures its own content once, after its view has settled, and
  reports that height back to `ListFormComponent`, which stores it and tells AG Grid's
  server-side row model to re-resolve row heights via a `getRowHeight` callback.

This achieves the SVY-21457 goal (each row sized to its own content) without native
`autoHeight`'s per-cell `ResizeObserver`, so the SVY-21244 flicker mechanism cannot
recur.

### 3.2 Per-row measurement (`RowRenderer`)

- `RowRenderer` implements `ngAfterViewInit` (Angular's own hook — AG Grid's optional
  `afterGuiAttached` hook is only invoked for filters, cell editors and date components,
  **not** for cell renderers, so it cannot be used here).
- On `ngAfterViewInit`, if `lfc.isPerRowAutoHeight()` is false, do nothing (all other paths
  are unaffected).
- Otherwise, wait a double `requestAnimationFrame` (the nested responsive form's floated
  layout needs a settled frame before measuring) and then measure the row's real content
  height via `measureContentHeight()`.
- `measureContentHeight()` cannot simply read the row wrapper's own `offsetHeight`/
  `scrollHeight`: the wrapper and the nested form's `12grid` `.row`/`.col-*` elements are all
  `float: left` with no clearfix, so a floated element's own height never grows to contain
  its floated children — this collapse compounds at every nesting level. Instead it
  recursively visits all descendants and takes the lowest `getBoundingClientRect().bottom`
  relative to the row wrapper's top, which reflects the actual rendered content regardless
  of the float collapse.
- The measured height is passed to `lfc.applyMeasuredRowHeight(rowId, node, measuredHeight)`.
- `node.detail` (not the deprecated `node.isFullWidthCell()`, AG Grid warning #61) guards
  against full-width/master-detail rows, which the LFC never uses but which would not be
  measurable this way.

### 3.3 Feeding the measured height back into AG Grid (`ListFormComponent`)

- `measuredRowHeights: Map<string, number>` stores the last-applied height per AG Grid row
  id, keyed so repeated re-renders (e.g. after a filter) do not needlessly reapply the same
  height and retrigger a refresh loop.
- `applyMeasuredRowHeight(rowId, node, measuredHeight)`: guarded by `isPerRowAutoHeight()`
  and `measuredHeight > 0`; no-ops if the height is unchanged from what's already stored.
  Otherwise stores the height, calls `node.setRowHeight(measuredHeight)`, and calls
  `agGrid.api.onRowHeightChanged()`.
- **Why a `getRowHeight` grid-options callback is also required:** the server-side row model
  re-resolves every row's height from `getRowHeight()`/`rowHeight` (not from the value passed
  to `node.setRowHeight()` alone) whenever it recalculates row positions — including from the
  very `onRowHeightChanged()` call above. Without a `getRowHeight` callback, that
  recalculation clobbers the per-row heights back to the single `agGridOptions.rowHeight`
  value. `agGridOptions.getRowHeight` is therefore set (only when `isPerRowAutoHeight()`) to
  look up `measuredRowHeights` for that row id, falling back to a default estimate (42, AG
  Grid's own default row height) until the row has been measured for the first time. For
  every other path `getRowHeight` is left `undefined`, so AG Grid uses the plain `rowHeight`
  option exactly as before.
- `agGridOptions.rowHeight` itself is unchanged (`this.getRowHeight()`) — it is only used as
  the *initial estimate* for not-yet-measured rows and by the non-per-row-auto-height paths.

### 3.4 Anti-flicker — hide the grid until every visible row has measured

Even with native `autoHeight` disabled, the very first paint still briefly shows rows at
the estimate height before their real measurement lands one to a few frames later, which
would be its own (smaller) flicker. To avoid that:

- `hideUntilFirstRender` is set to `isPerRowAutoHeight()`'s value in `svyOnInit()`.
- `getAGGridStyle()` adds `visibility: hidden` while `hideUntilFirstRender && !firstRenderDone`
  (this preserves layout — the grid still occupies space via `domLayout: 'autoHeight'` — so
  nothing outside the LFC jumps).
- `onFirstDataRendered` waits a few extra animation frames (beyond the double-rAF each row's
  own measurement uses) before calling `revealAfterFirstRender()`, so every row attached
  during the initial render has already applied its measured height before the grid becomes
  visible.
- `onModelUpdated` is a fallback that reveals the grid when the model has settled with zero
  displayed rows (the empty-foundset case, where `onFirstDataRendered` never fires).
- The fixed-height paths (`responsiveHeight >= 0`) and absolute layout are never hidden —
  `hideUntilFirstRender` is `false` for them.

### 3.5 Regression guard — resize-observer loop (SVY-21244, hardened further)

During implementation, live testing surfaced a **second** flicker mechanism distinct from
the one above: with `domLayout: 'autoHeight'`, the grid's own height grows/shrinks as rows
are measured, which can toggle a vertical scrollbar and shift the observed element's width
by the scrollbar's width. The pre-existing SVY-21244 resize observer reacted to that width
delta (`newWidth !== this.previousWidth`) by purging and refreshing the grid, which
re-measured, re-toggled the scrollbar, and looped indefinitely.

The resize observer is hardened so it never reacts to a raw width delta: it only acts when
the **computed column count** (`calculateNumberOfColumns()`) actually differs from
`this.numberOfColumns`, and it re-checks the column count again after the existing 200ms
debounce before purging, in case a transient reflow already settled. A scrollbar-only width
change never changes the column count, so it can no longer retrigger the loop. This
hardening is unconditional (applies to every non-absolute-layout responsive LFC, not only
the per-row auto-height path) — see §6 for why this is accepted as in-scope.

## 4. Implementation plan

1. `columnDefs[].autoHeight`: `!this.isPerRowAutoHeight()` (was unconditionally `true`
   pre-SVY-21457, with the per-row-auto-height exception added by SVY-21244).
2. Add `isPerRowAutoHeight()`: `!this.servoyApi.isInAbsoluteLayout() && this.responsiveHeight() < 0`.
3. Add `measuredRowHeights: Map<string, number>` and `applyMeasuredRowHeight(rowId, node, measuredHeight)`
   on `ListFormComponent` (see §3.3).
4. Add `agGridOptions.getRowHeight`, set only when `isPerRowAutoHeight()`, reading from
   `measuredRowHeights` with a default-estimate fallback (see §3.3).
5. Remove the `rowHeightMeasured` field and the old `onRowRendererAfterViewInit` single-first-row
   measurement block.
6. In `row-renderer.component.ts`: replace the removed `onRowRendererAfterViewInit` call with
   `ngAfterViewInit` calling a new `measureAndApplyRowHeight()` (double `requestAnimationFrame`,
   guarded by `lfc.isPerRowAutoHeight()`) and `measureContentHeight()` (recursive
   `getBoundingClientRect()`-based measurement, see §3.2).
7. Add the anti-flicker hide/reveal (`hideUntilFirstRender`/`firstRenderDone`,
   `getAGGridStyle()` `visibility: hidden` branch, `onFirstDataRendered` multi-frame reveal,
   `onModelUpdated` empty-foundset fallback) — see §3.4.
8. Harden the resize observer to react to column-count changes only, with a settle re-check
   after the debounce — see §3.5.
9. Replace the deprecated `node.isFullWidthCell()` check with `node.detail`.
10. Validate: `npx tsc --noEmit -p src/tsconfig.app.json`, `npx ng lint`, then
    `npx ng build ngclient2 --configuration development` (run from
    `com.servoy.eclipse.ngclient.ui/node`).
11. Manually test the scenarios in section 5, including both SVY-21244 regression scenarios
    (resize-observer loop and first-show flicker).

## 5. Acceptance criteria

- [x] In a responsive form, an LFC containing a responsive form component with
      `responsiveHeight = -1` and `responsivePageSize = 0` in listview mode sizes **each
      row to its own content**, not to the first row's height. *(Verified manually against
      a running LFC solution: rows with materially different content — e.g. a very long
      repeated-text row vs. short one-line rows — render at correspondingly different
      heights.)*
- [x] Rows with more content are not clipped/overflowing; rows with less content do not
      leave large empty gaps. *(Verified manually.)*
- [x] Filtering so that a taller item is the only visible one, then removing the filter,
      shows correct per-row heights for all items (no "sticky" first-row height across
      re-renders/filters). *(Verified manually; the `measuredRowHeights` map keys heights
      per AG Grid row id, so each row keeps/recomputes its own height independently.)*
- [x] The grid container still auto-sizes to fit all rows with no vertical scrollbar for
      `responsiveHeight = -1` (`domLayout: 'autoHeight'` preserved — unchanged by this fix).
- [x] Existing behaviour is unchanged for absolute-layout LFCs and for
      `responsiveHeight >= 0` (fixed height / `0` / grow-to-parent) cases. *(All new
      behaviour is gated behind `isPerRowAutoHeight()`; `columnDefs[].autoHeight` reduces to
      its pre-SVY-21457 value for every other path.)*
- [x] **Regression:** the SVY-21244 width-flicker/resize-loop scenario (a small responsive
      list whose content causes a vertical scrollbar) does **not** recur. *(Verified
      manually; the resize observer now only acts on a genuine column-count change — see
      §3.5. This required hardening beyond the original `newNumberOfColumns !==
      this.numberOfColumns` guard, because native-autoHeight experimentation during this fix
      surfaced that the guard alone was insufficient once the grid's own height could change
      the container width via a scrollbar toggle.)*
- [x] **Regression:** on first show of the responsive `responsiveHeight = -1` LFC, no
      per-row height flicker is visible — the grid stays hidden until every row has
      measured, then appears at the correct measured heights. *(Verified manually; this
      required abandoning native AG Grid `autoHeight` — see §3.1 — after it reproduced the
      flicker during live testing.)*
- [x] No deprecated AG Grid API usage: `node.isFullWidthCell()` (AG Grid warning #61)
      replaced with `node.detail`. *(Verified: warning no longer appears in the browser
      console log during manual testing.)*
- [x] `npx tsc --noEmit`, `npx ng lint`, and the development build all pass with no new
      errors.

## 5b. Follow-up (release-branch code review)

A code review on the `release`/`master` ports of this fix (commits `318e404c72` on
`lts_2026` and the zoneless-specific follow-up `0602d9c1f0` on `release`/`master`) flagged
three items. Disposition, re-checked against `lts_2026` specifically (the zoneless/`Eager`
items turned out not to apply there — see below):

1. **`measuredRowHeights` never cleared on a purge (medium) — fixed on `lts_2026`.** Without
   a `getRowId` grid-options callback, AG Grid's server-side row model assigns each row node
   a position-based id, so after any `refreshServerSide({ purge: true })` the same `node.id`
   can be reused for a different foundset record. The map was never cleared on any of the
   purge call sites, so `getRowHeight()` could keep returning a stale previous-occupant
   height for a position until (or unless) that row got re-measured, and the map also grew
   unboundedly over a long filtering/scrolling session. Fixed with
   `clearMeasuredRowHeightsOnPurge()`, called immediately before each of the four
   `refreshServerSide({ purge: true })` call sites (the `viewportRowsCompletelyChanged`/
   `fullValueChanged` branch, the insert/delete branch, the "else" branch of the
   `viewportRowsUpdated` handler, and the settled column-count resize path). Guarded by
   `isPerRowAutoHeight()` so it is a no-op on every other path. Covered by new tests in
   `listformcomponent.spec.ts` under `clearMeasuredRowHeightsOnPurge (code review
   follow-up)`, which exercise the real call sites (not just the helper) so a future call
   site that forgets to clear is caught.
2. **`RowRenderer` `ChangeDetectionStrategy.Eager` (low) — not applicable to `lts_2026`.**
   Introduced only by the `release`-branch zoneless-migration follow-up commit
   (`0602d9c1f0`); `lts_2026`'s `RowRenderer` has no explicit `changeDetection` (default
   strategy), matching the state before that commit. No action needed here; track on the
   `release` branch separately if not already resolved there.
3. **`measureContentHeight` full-subtree `getBoundingClientRect()` reads (low, performance)
   — accepted, unchanged.** This is the deliberate float-collapse workaround from §3.2;
   already called out as a tradeoff in §6 (small lists only). No action unless a real
   large-form case reports a problem.
4. **Zoneless Vitest assertion for the reveal — not applicable to `lts_2026`.** `lts_2026`
   does not use zoneless change detection or Vitest (Zone.js + Jasmine/Karma per
   `AGENTS.md`); the reveal path (`revealAfterFirstRender`/`getAGGridStyle`) was unaffected
   by the zoneless-specific commit and needs no change here.

## 6. Out of scope

- Optimising per-row auto-height for lazy-loaded / infinite-scroll large lists. The
  documented caveat is accepted: each row is measured once when it is first attached to the
  DOM, so this is intended only for small lists (~10–100 rows) as agreed by the reporter and
  architect.
- Changing the meaning of `responsiveHeight` values other than `-1`, or the
  `responsivePageSize` semantics.
- The non-scrolling (paging) LFC code path (`useScrolling === false`).
- Absolute-layout LFC row sizing.
- **Accepted scope widening:** the resize-observer hardening (§3.5) is not gated behind
  `isPerRowAutoHeight()` and therefore also affects responsive LFCs with `responsiveHeight >= 0`.
  This is accepted because it is strictly a bug fix (reacting to column-count changes instead
  of raw width deltas is a strict subset of when the old code refreshed the grid — never a
  superset), not a new behaviour, and is required to fix the resize-loop regression that
  surfaced during this fix's own testing.

## 7. Open questions

None outstanding — the three questions raised in the original approach (native `autoHeight`
+ `rowHeight: null` behaviour, whether `getRowHeight()` needed refactoring, and whether an
explicit refresh was needed after a filter) are moot: that approach was replaced (§3.1) by
explicit per-row measurement plus a `getRowHeight` grid-options callback, which was
necessary regardless (§3.3) and has been verified manually against a running solution,
including the filter/unfilter scenario.
