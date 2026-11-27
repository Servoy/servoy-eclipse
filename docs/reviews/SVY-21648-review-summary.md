# Review Summary - SVY-21648: Zoneless: BaseTabpanel signal writes during change detection

**Risk: LOW.** The fix is sound. `tabIndex` now stays a tab name when the named tab is already selected (`basetabpanel.ts:153`). This was reproduced manually on all three components, but it matches the NG1 client (see Behaviour vs NG1), so it is not a regression against NG1.

Scope: `servoy-eclipse`, branch `release`, commits `db5e0a69d0`, `34ffabf11f`, `7155fb5379`.

Fix Version `2026.9.0` names the 2026.9 line, and `release` is that line, so the commits are on the right branch. The `2026.9.0` tag predates them, which is expected for a follow-up on the maintenance line.

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

## Manual test results
Run in the NG client (localhost:8183) with a test form holding a tabpanel, a tablesspanel and an accordion, each with 3 named tabs. Results read from the form's label and the Developer log.

| Check | tabpanel | tablesspanel | accordion |
|---|---|---|---|
| Assign the name of the already-selected tab, then read `tabIndex` | string (`second_d`) | string (`second_t`) | string (`second_a`) |
| Assign tab 2's name, then tab 3's name, in one call | tab 3 shown, `tabIndex` 3 (number) | same | same |
| Tab-change handler: `tabIndex` inside it | new number | new number | new number |
| Change tab, then leave the form | no console problems reported | not run | not run |

- The string result is a behaviour change against the previous TiNG code, where `getRealTabIndex()` rewrote the name to a number. It is also visible to scripts, because the server value is the string too.
- Not verified: a dev-mode console check for NG0100/NG0600 (the panel only showed a harmless `slider-vertical` CSS warning, and it is unconfirmed that the client ran in dev mode), the leave-the-form check with a delay, and a pre-fix comparison.

## Behaviour vs NG1
- NG1 (`servoy_ngclient/war/servoydefault/tabpanel/tabpanel.js`) accepts a number, a numeric string or a tab name in `tabIndex`. `refresh()` (lines 42-55) resolves the name to an index and does not rewrite `model.tabIndex`.
- NG1 writes the number back only in `setFormVisible()` (line 192). `select()` returns early when the tab is already selected (line 259), so a name assigned for the already-selected tab stays a name there too.
- SVY-21526 asked for exactly this parity: set a tab by name in TiNG as in NG1. The fix in SVY-21648 (`34ffabf11f`) makes TiNG behave like NG1 in this case.
- Not read: the NG1 paths that also write `model.tabIndex` (lines 467, 483, 497), and the full path from the assignment to `select()`.

## Author answers
- Destroy guard: not needed. The write is a 0 ms timeout. If the component is destroyed first, the Angular output code logs a warning (NG0953, "Unexpected emit for destroyed OutputRef") and returns, without throwing.
- `tabIndex` as a name: supported since NG1, so a string value is expected.

## Possible improvements / follow-ups
- Optional, not a defect: normalize the name to the number also when the named tab is already selected. This would differ from NG1, so it would be a new decision and not a fix.
- Quick double name assignment: "last assignment wins" held in the manual test.
- `com.servoy.eclipse.ngclient.ui/node/AGENTS.md` still mentions Karma files that no longer exist.
