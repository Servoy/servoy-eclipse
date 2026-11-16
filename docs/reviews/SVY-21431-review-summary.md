# Review Summary: SVY-21431 improve servoy builder performance

**Risk: ELEVATED.** `DUPLICATE_UUID_OTHER_FILE` is only cleared on a path its own value makes unreachable (`ServoyBuilder.java:1799-1821`), so a "UUID duplicate found" ERROR marker can persist after the duplicate is fixed. A second path (`SolutionDeserializer.java:1891-1895`) can raise the same error where the old disk scan never did. Both were confirmed by reading the code only; the IDE was not run.

**Scope reviewed:** `servoy-eclipse`, commit `62ae7701ca` on `master` (Fix Version 2026.12, release-target check MATCH). No commits in sibling repositories.

## Manual test plan

**Verifying the fix**
1. Load a large solution (ideally the one from the ticket) and run a full Servoy build. Compare build time against the parent commit (`62ae7701ca~1`). No baseline numbers exist.
2. Copy a `.frm` file (same UUID) in the workspace, refresh and build. The "UUID duplicate found" marker should appear on the form.
3. Watch the progress label during the build. Check that it changes while forms are validated, and whether the percentage moves.

**Regression checks**
1. Latch check: after step 2 above, delete the copy (or change its UUID), refresh and build. The marker should disappear. If it stays, check whether an IDE restart or solution reload clears it.
2. External rename: with the solution loaded, `git mv` a form file, refresh and build. The old behaviour showed no duplicate marker.
3. Script UUID change: change the UUID of a method in a `.js` file but keep the name, then build (the SVY-21072 steps). The old behaviour showed no marker. Repeat with a form element whose UUID changes in the `.frm` while the name stays.
4. Modules: two modules sharing one form UUID. `UUIDDuplicateIn` already existed. Check whether a second `UUIDDuplicate` marker now appears.
5. Incremental build: duplicate a file, then edit only the other one. Check that detection still works with a partial build delta.
6. Quick fix: with a duplicate marker present, open the quick fix (`ServoyQuickFixGenerator`) and check that it still gets the `Uuid` and `SolutionName` attributes.

**Automated checks:** `DeveloperPersistIndexDuplicateUUIDTest` (plainjunit, `com.servoy.eclipse.model.tests`). It does not cover `checkDuplicateUUID` or the deserializer set-sites, so neither High risk is covered. Spotbugs was not run.

## Possible improvements / follow-ups
- Clear `DUPLICATE_UUID_OTHER_FILE` when the duplicate is resolved (file deleted, renamed, or given a new UUID), or re-validate it instead of treating any non-null value as confirmed.
- Decide whether persists reaching `SolutionDeserializer.java:1891-1895` (same file, same name, different UUID) should be flagged. The property is set to the persist's own file name there.
- Add tests for `ServoyBuilder.checkDuplicateUUID` and the deserializer set-sites, including the clear-after-fix path.
- The progress change only calls `monitor.subTask(name)` and never `worked()`, so the percentage still does not move. Decide whether label-only is enough for the ticket, and confirm that forms update the label (the slow path is in `ServoyFormBuilder`'s visitor).
- Measure build time before and after on the ticket's solution.
- Cross-module duplicates may now produce both `UUIDDuplicate` and `UUIDDuplicateIn` markers. Projects outside the active module set may lose detection (the index is the active flattened solution). Neither was verified at runtime.
- Minor: `AGENTS.md` says version `2026.6.0-SNAPSHOT` while the newest unreleased Jira version is 2026.12.
