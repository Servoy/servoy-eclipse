# SVY-21563 Review Summary

**Risk: LOW** — the only substantive concern is that the new `.catch(() => {})` is unconditional, so it also silently absorbs the documented "server refused the selection change" rejection path, not just the superseded-selection case it targets.

## Manual test plan

1. **Repro the original bug (regression check):** Open a form with an LFC bound to a foundset where a checkbox dataprovider feeds a calculation used as `enabledDataprovider` on another field in the same row. With browser dev tools open, click the checkbox rapidly on different rows. Before the fix this threw "Uncaught (in promise): Selection change defer cancelled...". Confirm the console stays clean.
2. **Keyboard path:** With a row focused, use arrow keys to move selection quickly across rows that have the same checkbox/calc setup. Confirm no unhandled rejection appears.
3. **Server-veto behavior (the main risk, needs a solution with `onRecordSelection` or equivalent server-side block):** If any available test solution has a related foundset with a selection-blocking `onRecordSelection` handler, try to select an LFC row that should be refused. Confirm current behavior (does the UI silently do nothing now, with no console trace at all?) and decide if that's acceptable.
4. **Baseline regression:** Normal single-click row selection and keyboard navigation in an LFC without any calc/checkbox interaction still behaves identically (selection highlight, `onSelectionChanged` callback firing).
5. **Automated tests:** Run `npx ng test --include="**/listformcomponent.spec.ts" --no-watch` once the local `projects/servoydefault` node_modules gap (`@ng-bootstrap/ng-bootstrap` missing) is resolved — unexecuted in this review for that reason. Additionally verify the two new tests fail when the `.catch()` is temporarily removed, to rule out a vacuously-passing assertion.

## Possible improvements / follow-ups

- Consider discriminating the caught rejection by reason/message so a genuine server-side veto of a selection change isn't silently dropped alongside the intended superseded-selection case — currently there's no console trace at all if a solution relies on `onRecordSelection`-style blocking for LFC rows.
- No second-party confirmation exists that the fix eliminates the exact error shown in the Jira ticket's screen recording (not transcribed in this review); the race-condition mechanism is the author's own diagnosis, plausible but unverified externally.

## Scope reviewed

Single commit `3a37dc090da9d4b8593383797bd1b5698719cfba` on branch `release` of `servoy-eclipse`. No other commits or spec document found for SVY-21563 in this repo or in sibling repos (`sablo`, `server`, `servoy-client`, `servoy-extensions`, `build`).
