# Review Summary - SVY-21578: Insert column is done outside the container

**Risk: MODERATE.** The fix holds up on reading, but it also changes where "insert before/after" lands inside CSS-position containers, and neither the designer nor the Vitest suite was run on that path.

Reviewed: `servoy-eclipse` commit `c2a38d6ffa` on `master` (Fix Version 2026.12, release-target check: MATCH).

## Manual test plan

**Verifying the fix**
1. Open a responsive form with container > row > column, with at least two sibling columns in the row.
2. Select the first column and click "Insert a 12grid.column before". The new column should appear in the same row, before the selected one.
3. Select a middle column and click "after". The new column should appear in the same row, directly after it, not at the end of the row.
4. Select the last column and click "after". The new column should be appended in the same row, inside the container and not on the form root.
5. Check the persisted structure (Outline or Properties view) to confirm the parent is the row and not the form.

**Regression checks**
1. Select a top-level layout container (direct child of the responsive form, no wrapper) and click "after". The new container should be placed directly after it (relies on the helper's fallback path).
2. Put a form-component container directly after a column. Select the column and click "after". Check where the new column is placed.
3. In a CSS-position layout container with a nested layout container, select the nested one and click before/after. Check which parent is used and where the new element renders.
4. Drag and drop in a responsive form, inside a container and at the end of one. The code is unchanged, but it shares `getNextElementSibling`.
5. Confirm absolute-layout forms and form-component descendants still show no before/after buttons.
6. If the form extends a super form, insert before/after on an inherited column (override path via `ElementUtil.getOverridePersist`).

**Automated checks** (in `com.servoy.eclipse.designer.rfb/node`): `npm test`, `npm run lint`, `npm run build_debug_nowatch`. Prerequisite: `cd ../../com.servoy.eclipse.ngclient.ui/node && npm run build_libs` before `npm install`. Vitest was not runnable during the review (`vitest`/`jsdom` not installed), so the "25 tests pass" claim is unverified.

## Possible improvements / follow-ups

- **CSS-position containers:** `closest('[svy-id]')` now resolves through the id-less `div.svy-wrapper` to the CSS-position container. Java then skips sibling ordering and uses the location `(1,1)`. Previously the element went to the form root. Decide whether this is wanted or whether the buttons should be hidden there (`mouseselection.component.ts:509`).
- **"Insert after" edge cases:** if the next sibling's root has no `svy-id` (e.g. a form-component container), the helper may append at the end or pick a nested descendant that puts the new column first (`designerutils.service.ts:379-393`). This is existing helper behaviour shared with the drag path.
- **Backport:** the SVY-21248 wrapper is also on `origin/release`, which lacks this fix. Decide whether to backport. A cherry-pick may conflict on `insertACopyAction` at the next merge into `master`; this was not verified.
- **Test fidelity:** `mouseselection.component.spec.ts` stubs `getNextElementSibling` with a hand-copied port of the real helper, so helper drift will not be caught there (`designerutils.service.spec.ts` covers the real helper).
- **Documentation drift:** root `AGENTS.md` still states version 2026.6.0 / Tycho 4.0.12; `designer.rfb/node/AGENTS.md` has a stale version and test count; `docs/SVY-21578-triage.md` does not follow the `docs/<KEY>-<slug>.spec.md` naming.
- **Open questions for the author:** backport plan; whether CSS-position behaviour is intended; whether the "fails on pre-fix code" claim for the new tests was confirmed by running them.
