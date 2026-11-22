# Review Summary: SVY-21431 improve servoy builder performance

**Risk: ELEVATED (round 2).** Of the two round 1 problems, the Site 1 false positive is fixed. The marker-latch problem is only partly fixed. A duplicate resolved by giving the copy a new UUID still leaves the ERROR marker until the solution is reloaded (`SolutionDeserializer.java:1915-1925`, `ServoyBuilder.java:1825-1832`). All findings come from reading the code; the IDE was not run.

**Scope reviewed:** `servoy-eclipse`, `master` (Fix Version 2026.12, release-target check MATCH). No commits in sibling repositories.
- Round 1: `62ae7701ca` (the in-memory duplicate check)
- Round 2: `01f2ed8359` (fixes for the round 1 findings: absolute path plus existence check, Site 1 no longer sets the property, `SubMonitor` progress, new `ServoyBuilderDuplicateUUIDTest`)

## Round 1 findings vs round 2

| Round 1 finding | Round 2 verdict |
|---|---|
| Marker can never clear once `DUPLICATE_UUID_OTHER_FILE` is set | **Partly fixed.** It now clears when the recorded file is deleted, and when the original is deleted and the copy kept. It does **not** clear when the copy is given a new UUID (R2 below). |
| Same-file / same-name / other-UUID persist gets a false duplicate marker | **Fixed.** That branch no longer records the property. Site 3 excludes `.js`, so scripts never carry it. |
| Progress only changes the label | **Partly fixed.** The percentage now moves and cannot overshoot, but see R3. |
| Core logic untested | **Partly fixed.** The new test covers the decision logic. The deserializer set-sites and the progress code are still untested. |

## Open findings

- **R2 [High] New UUID in the copy keeps the marker.**
  - Copy `orig.frm` to `copy.frm`. Site 3 flags the original persist and records `copy.frm`.
  - Changing the `uuid` inside `copy.frm` creates a new persist for the copy and leaves the flagged original alone.
  - `copy.frm` still exists, so the existence check stays true. The old code re-read the sibling UUIDs and cleared the flag.
- **R1 [Medium] False duplicate after an external rename.**
  - On a pull or `git mv` that also changes the form's inner name, Site 3 fires because the persist's cached file name still differs from the file being read.
  - The recorded "other" file is then the persist's own new file, which exists, so the marker is raised. The old scan skipped the persist's own file.
  - Not verified: the rename timing in `completePersist`, and whether a rename through the Servoy UI avoids this.
- **R3 [Low] Progress freezes after the first project in a multi-project build.**
  - `SubMonitor.convert(monitor, N)` on an existing `SubMonitor` redistributes the unused ticks, so the first project consumes all of them.
  - This hits the uninitialised-dependencies path (`ServoyBuilder.java:726-729`), which is the long first build in the ticket.
  - This comes from the upstream `SubMonitor` source, not the target-platform jar.
  - Forms are one tick each, and `ServoyFormBuilder`, where the ticket's stack trace sits, reports nothing.
- **R5/R6 [Low] Smaller points.**
  - The existence check reads the workspace tree, so a file deleted outside Eclipse and not yet refreshed counts as existing for one build.
  - `getDuplicateUUIDList()` now runs per flagged persist and mutates a plain `HashMap`. This is theoretical.

## Manual test plan

**Verifying the fix**
1. Load a large solution (ideally the one from the ticket) and run a full Servoy build. Compare build time against `62ae7701ca~1`. No baseline numbers exist.
2. Copy `forms/orig.frm` to `forms/copy.frm` in the file system and refresh. The "UUID duplicate found" marker should appear.
3. Delete `copy.frm`, refresh and build. The marker should disappear without a restart. This should now work.
4. Watch the progress bar and label during a full build. The percentage should move steadily and the label should show names.

**Regression checks**
1. **R2 check:** repeat step 2, then change the `uuid` inside `copy.frm` to a new GUID and save. Run a clean or full build. The marker should be gone; if it stays, R2 is confirmed (check whether an IDE restart clears it).
2. **R1 check:** with the solution loaded, `git mv forms/f1.frm forms/f2.frm` and change the inner `"name"` to `f2`. Refresh and build. There should be no duplicate marker on `f2`. Also rename a form through the Servoy UI and check the same.
3. **Delete the original:** repeat step 2, then delete `orig.frm` and keep the copy. There should be no stuck marker.
4. **Progress with several modules:** start Developer fresh (dependencies uninitialised) with an active solution that has several modules, then build. Check whether the bar reaches 100% after the first module and then stops (R3).
5. **Script UUID change:** change the UUID of a method in a `.js` file but keep the name, then build (the SVY-21072 steps). There should be no marker. This is the case round 2 fixed.
6. **Modules:** two modules sharing one form UUID. `UUIDDuplicateIn` already existed. Check whether a second `UUIDDuplicate` marker appears.
7. **Incremental build:** duplicate a file, then edit only the other one. Check that detection still works with a partial build delta.
8. **Quick fix:** with a duplicate marker present, open the quick fix (`ServoyQuickFixGenerator`) and check that it still gets the `Uuid` and `SolutionName` attributes.

**Automated checks:** `ServoyBuilderDuplicateUUIDTest` and `DeveloperPersistIndexDuplicateUUIDTest` (plainjunit, `com.servoy.eclipse.model.tests`, `junitVersion: "6"`). Neither can catch R1 or R2. The existence predicate is injected, so the real `workspaceFileExists` is never exercised. Neither test was run in this review, and Spotbugs was not run either.

## Possible improvements / follow-ups
- **R2:** re-validate the recorded file instead of only checking that it exists, for example by checking that it still declares this persist's UUID. Alternatively, clear the flags when the other file is re-read with a different UUID.
- **R1:** ignore a recorded "other file" that is the persist's own current file, as the old scan did (`!pathPair.getRight().equals(f.getName())`).
- **R3:** use one `SubMonitor` per project, for example `SubMonitor.split(...)`, instead of repeated `convert(monitor, N)` on the same instance. Consider reporting progress inside `ServoyFormBuilder` too.
- Add tests that exercise the real file-existence check, the deserializer set-sites, and the new-UUID and rename scenarios.
- `DeveloperPersistIndexDuplicateUUIDTest.java:164` still sets `DUPLICATE_UUID_OTHER_FILE` to the round 1 value `"formA.frm"`. It passes, but no longer matches the production meaning of an absolute path.
- The new test calls the package-private method reflectively. Since `acef0ffad5` (SVY-21640), the sdd skills say to avoid reflection in generated tests. As a fragment, the test could call it directly from package `com.servoy.eclipse.model.builder`.
- Measure build time before and after on the ticket's solution.
- Minor: `AGENTS.md` says version `2026.6.0-SNAPSHOT` while the newest unreleased Jira version is 2026.12.
