# SVY-21297 — Peer Review Summary

**Verdict: LOW risk (as it stands on `release`).** The commit shipped two real defects in
isolation (Maven ran no tests; Jenkins CI reporting produced nothing), but both were fixed
within ~24h by follow-up commits already present on the same branch, so `release` today is
whole. The one live residual is a newly-skipped integration test suite that does not appear
to be tracked.

**Scope reviewed:** commit `9a4dce56e6` — "migrate test framework from Karma/Jasmine to
Vitest [ai]" (Johan Compagner, 2026-08-05), `com.servoy.eclipse.ngclient.ui/node/`, 86 files
(+2628/−5253). Test-infrastructure only: Karma/Jasmine → Vitest 4 + jsdom via Angular's
`@angular/build:unit-test` builder, 47 spec files rewritten, one incidental production-src
type cast (`PopupForm as any`, no runtime effect).

## Manual test plan

**Verifying the migration**
1. From `com.servoy.eclipse.ngclient.ui/node/`, run `npm ci` then `npm run test`. Expect it
   to drive `ng test ngclient2 --no-watch` through the unit-test builder on Vitest/jsdom,
   reporting ~241 passing / 11 skipped / 0 failures.
2. Run each per-project script (`test_public`, `test_default`, `test_dialogs`, `test_window`,
   `test_ngclientutils`) and confirm each resolves and runs.
3. Confirm no Chrome/Edge/headless browser is launched (jsdom only) and no `karma*.conf.js`
   is referenced anywhere.

**Regression checks**
1. Confirm `describe.skip('FormComponentComponentTest')` in `form_component.component.spec.ts`
   is only skipped, not deleted — the test body must still exist so it can be re-enabled.
2. Confirm all 47 `*.spec.ts` files are discovered by the runner.
3. Run the fake-timer specs (`check`, `tooltip`, `spinner`, `typeahead`, `tabpanel`) a few
   times and confirm deterministic results (no flakiness from the `setTimeout(r,0)`
   compensation pattern some specs use).
4. Run `npm run build_libs` and the normal production/WAR build; confirm the `dummy` scaffold
   app's output (`dist/dummy`) does not appear in any shipped artifact.
5. Run `npm run lint` across all three Angular sub-projects (zero warnings is the gate).
6. Run the ngclient.ui Maven module test phase and confirm it actually invokes Vitest (not
   the removed `test_headless` script).
7. Inspect the latest Jenkins build on `release`/`master`: confirm the Vitest JUnit XML
   (`vitest-results.xml`) is produced and ingested, and the unit-test step is green.

## Possible improvements / follow-ups

- **Re-enable or formally track the `FormComponentComponentTest` suite** — it was the
  heaviest integration coverage (FormService + push-to-server round-trip) in the app project
  and is now disabled with no linked follow-up ticket found in Jira.
- **Pin `vitest`/`jsdom` to exact versions** in `package.json` for consistency with the rest
  of the manifest (the lockfile already pins exact versions with integrity hashes, so this
  is cosmetic, not a reproducibility risk).
- **Revisit the `PopupForm as any` cast** in `projects/window/src/lib/windowservice.module.ts`
  — added to satisfy the AOT test compile; worth fixing the underlying type mismatch properly.
- **Treat the migration as one atomic unit when forward-merging to lts**: this commit plus
  `184ffdb4fa`, `f4f6a9d9a5`, and `7b27ce2ab8` must travel together, or a maintenance branch
  reproduces the broken-Maven-tests / broken-Jenkins-reporting state this commit shipped with.
  Expect hard conflicts on `package.json` / `package-lock.json` / `angular.json`.
- **Confirm a green CI run** was never directly observed as part of this review (reasoned
  from config state and the content of the fix commits) — worth a human spot-check on the
  latest Jenkins build.

## Scope

- Repository: `servoy-eclipse`, branch `release` (also on `master`, `groovy-iguana`; not on
  any `lts_*`).
- Commit: `9a4dce56e6`.
