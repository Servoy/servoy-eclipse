# Review Summary — SVY-21527: reviewer skill should check fix version against the branches it was pushed against

**Risk: MODERATE** (as reviewed, before the follow-up rework). The first version of the Fix Version vs branch rule assumed `YYYY.MM` Fix Version names and "the mainline ships the newest version", and its MISMATCH wording ("will not reach the release") contradicted the merge-forward rule. Both were reworked during the review: each branch's version is now read from source (`ClientVersion`, extension `MANIFEST.MF`), every Fix Version is checked, and a pending forward merge counts as a MISMATCH.

## Manual test plan

This is a prompt change, so the test is to dry-run `/case-review <KEY>` on cases in `In Review`. Check the reported branch and version against `ClientVersion` on that remote branch.

**Verifying the fix**
1. Commits on the branch whose `ClientVersion` equals the Fix Version: expect `MATCH`, a one-line reassurance, no Top-risk entry.
2. Commits only on a maintenance branch (`release` or `lts_*`) with a Fix Version that belongs to a later line: expect `MISMATCH` ("merge/backport pending"), a Medium Top-risk entry, a neutral question for the author, rating no higher than MODERATE. After the forward merge, expect `MATCH`.
3. Several Fix Versions, only some covered: expect `MISMATCH` marked partial, listing satisfied and unsatisfied versions.
4. A Fix Version that has already shipped: the source version at the commit's sha must be used, not today's (greater than the Fix Version = `MISMATCH`, equal = `MATCH`). Compare with `git tag --contains <sha>`.
5. A fix in an extension package: Fix Versions come from the issue comments, the `MANIFEST.MF` is found with `git ls-files "*META-INF/MANIFEST.MF"`, and the result is `INDETERMINATE` if the comments name none.
6. A feature-branch case or no Fix Version: `INDETERMINATE` in "Scope reviewed" only. A non-Jira ref (`range main..feature/x`): "none (not a Jira issue)" and no Jira call.

**Regression checks**
1. Trigger the Ambiguity gate (commits on two branches): the check must run after the user chooses, on the confirmed set.
2. Run on a `SVYX-*` or `SERVOY-*` key: no hard-coded `/project/SVY/` path is used.
3. Choose "Yes — write summary" after a `MISMATCH` or `INDETERMINATE`: the committed summary keeps the verdict.
4. `phases/regression.md` and `phases/security.md` still contain no `FIX_VERSION` text.
5. Run twice on the same `lts_*` case: both runs give the same verdict.

## Possible improvements / follow-ups

- The follow-up rework is not covered by a real dry-run yet. It was checked only by reading `ClientVersion` on remote branches of `servoy-client`.
- The extension-package path (comments for Fix Versions, `MANIFEST.MF` location) was confirmed to exist in `servoy-extra-components` but its version fields were not read.
- A fix that only exists as a differently-worded commit message (no issue key) will not be found by the `--grep` cherry-pick lookup.
- A Fix Version that no branch reports and that has no commit-time source version available is `INDETERMINATE`; consider whether tags should be mandatory in that case.
- A MISMATCH only caps the overall rating at MODERATE and has no floor; a LOW briefing can still carry a Medium finding.

## Scope

`servoy-eclipse`, commit `6759727e10` on `master` (plus uncommitted follow-up edits to `skills/case-review/` and `docs/SVY-21527-*`).
