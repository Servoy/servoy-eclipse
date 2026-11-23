# SVY-21655 peer-review summary

**Risk: LOW.** The worst remaining finding is that forms with no part named `body` no longer take their container overflow from their first part. The diff is two lines, and the server names the body part exactly `body` (`PartWrapper.getName`).

Reviewed scope: servoy-eclipse, `com.servoy.eclipse.ngclient.ui/node/src`. Commits `84e988fcfd` (lts_2024 and forward), `fb81c45362` (lts_2025/lts_2026), `8edeea0cac` (release), `16ac0c088b` (master), `f3dea16068` (spec stub) and `ea197ffe9d` (restores the fix lost in the lts_2026 to release merge). Fix Versions 2026.12, 2026.3.3 LTS, 2025.3.8 LTS and 2024.3.13 LTS all map to a branch that contains the fix.

## Manual test plan

### Verifying the fix
1. Create a form with Title Header, Header, Body and Footer parts. Set both scrollbars to "never" and put content larger than the container in the body. Open it in the NG client. The container shows no scrollbars and the content is clipped.
2. Place that form in a form container without an explicit height and repeat.
3. Repeat with the form inside a tabpanel, both servoydefault and bootstrap.

### Regression checks
1. A form with only a body part and scrollbars set to "never" still hides its scrollbars.
2. A form with only a body part and default scrollbars still shows scrollbars when the content overflows.
3. A form with a header and scrollbars set to "as needed" still shows scrollbars.
4. A header-only or footer-only form (no body part) loads without errors. Note whether its overflow differs from before the fix.
5. A form container with an explicit height behaves as before. The overflow branch is not entered.
6. A responsive form with header and footer parts renders correctly.
7. Where practical, check on each line: lts_2024, lts_2025, lts_2026, release and master.

Automated: `com.servoy.eclipse.ngclient.ui/node/run_tests.bat`, in particular `src/servoycore/formcontainer/formcontainer.spec.ts`, `src/ngclient/types.spec.ts` and `projects/servoydefault/src/lib/tabpanel/tabpanel.spec.ts`. Also run `tsc --noEmit` on release and master, which have stricter typing. None of these were run during the review.

## Possible improvements / follow-ups
- Forms without a `body` part now get no overflow style. Decide whether that is intended or whether a fallback is wanted.
- Add a spec for a form without a body part that asserts no overflow style is set and nothing throws.
- `getBodyPartLayout()` is also used by the servoydefault tabpanel (`basetabpanel.ts:85`) and the bootstrap tabpanel (`bootstrapcomponents`, `bts_basetabpanel.ts:30`). Their real behaviour with header + body forms is not covered by specs.
- The fix was silently dropped once in a forward merge (681c344). The lines are formatted differently, so re-check `getBodyPartLayout()` after the next lts_2026 to release to master merge.
