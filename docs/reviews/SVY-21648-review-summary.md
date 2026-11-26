# Review Summary - SVY-21648: Zoneless: BaseTabpanel signal writes during change detection

**Risk: MODERATE.** The fix is sound, but `tabIndex` now stays a tab name when the named tab is already selected (`basetabpanel.ts:153`), and Fix Version 2026.9.0 was released without the commits.

Scope: `servoy-eclipse`, branch `release`, commits `db5e0a69d0`, `34ffabf11f`, `7155fb5379`.

## Release-target check: MISMATCH
- Fix Version `2026.9.0` in `servoy-eclipse`: tag `2026.9.0` (2026-10-09) contains none of the three commits (authored 2026-10-28, 11-05, 11-10). The commits are on `release` (2026.9.1) and `master` (2026.12.0). Either the fix is meant for 2026.9.1 or the Fix Version is wrong.

## Manual test plan
**Verifying the fix**
1. Run a dev client (zoneless, exhaustive checkNoChanges) with a form that has a tabpanel, a tablesspanel and an accordion. Load it and switch tabs. The console should show no NG0100 or NG0600.
2. With the accordion, set `tabIndex` to a tab name from a script. Expect no NG0600 and a numeric `tabIndex` pushed to the server shortly afterwards.

**Regression checks**
1. Select tab 2, then assign tab 2's name to `tabIndex`. Read `tabIndex` in a script: name or number?
2. Assign two different tab names in one script call while the form is slow to hide. Note which tab ends up shown.
3. Close or navigate away from the form right after a tab change. Watch the console for warnings.
4. Check that `onChangeMethodID` still fires with the old tab index, and that `tabIndex` reads as the new number inside it.

Automated: `npm run test_default` and `npm run lint` from `com.servoy.eclipse.ngclient.ui/node` (not run during review).

## Possible improvements / follow-ups
- Name of the already-selected tab leaves `tabIndex` as a string (`select()` returns early). Decide whether that is intended.
- Quick double name assignment while the previous hide is pending: confirm "last assignment wins" in the real client.
- The deferred `setTimeout` write has no `clearTimeout` or destroy guard (Low).
- `com.servoy.eclipse.ngclient.ui/node/AGENTS.md` still mentions Karma files that no longer exist.
