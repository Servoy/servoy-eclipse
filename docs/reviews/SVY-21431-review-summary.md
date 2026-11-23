# Review Summary: SVY-21431 improve servoy builder performance

**Risk: MODERATE (round 3).** Both open findings from round 2 are fixed: a copy given a new UUID now clears its marker, and a pure external rename no longer raises a false one. No High risk remains. The fix introduced one Medium false negative: a form copied on disk whose inner name is then edited is no longer flagged (`ServoyBuilder.java:1853`, `SolutionDeserializer.java:1921-1929`). All findings come from reading the code; the IDE and the tests were not run.

**Scope reviewed:** `servoy-eclipse`, `master` (Fix Version 2026.12, release-target check MATCH). No commits in sibling repositories.
- Round 1: `62ae7701ca` (in-memory duplicate check)
- Round 2: `01f2ed8359` (absolute path plus existence check, Site 1 fix, `SubMonitor` progress)
- Round 3: `06bc085d34` (clear on new UUID, own-file guard, no recording for form elements, `split()` progress, extended tests)

## Findings across rounds

| Finding | Status after round 3 |
|---|---|
| Marker can never clear (round 1) | **Fixed.** Clears when the copy or the original is deleted, and now also when the copy gets a new UUID. |
| False marker for same-file / same-name / other-UUID persist (round 1) | **Fixed** in round 2. |
| New UUID in the copy keeps the marker (round 2, High) | **Fixed.** The re-read clears the recorded path (`clearDuplicateUUIDOtherFile`), and the next incremental build removes the marker. No full or clean build is needed. |
| False marker after an external rename (round 2, Medium) | **Fixed** for a pure rename, through the own-file guard. **New regression** below. |
| Progress freezes after the first project (round 2, Low) | **Fixed.** One root `SubMonitor` with a `split()` child per project, so the bar moves across modules, stays monotonic and cannot overshoot. |
| Core logic untested | **Partly fixed.** The decision method is tested, the deserializer and progress are not. |

## Open findings

- **1 [Medium] Copy plus inner rename is no longer flagged.**
  - The user copies `orig.frm` to `copy.frm` and edits only the inner `"name"` to `copy`. The same persist instance is renamed, so its own file becomes `copy.frm`. That equals the recorded "other" file, and the guard returns false.
  - The removed scan found `orig.frm` with the same UUID and raised the marker.
  - It is only reported after an IDE restart, and then only by the project-level `UUIDDuplicateIn` markers, not by the per-file marker.
  - The rename timing in `completePersist` was read from the code, not run.
- **2 [Low] Per-element markers for forms loaded at startup.**
  - Two form files with the same UUID at startup create two instances. Each flagged element is confirmed through the index, so one marker per element appears.
  - The old scan never reported elements. Round 3 only fixed the Site 3 half of this. Index membership of the element persists is likely but not confirmed.
- **3 [Low] Cancel between modules.**
  - `SubMonitor.split()` throws `OperationCanceledException` and does not call `forgetLastBuiltState()`, as the builder's own `checkCancel()` does.
  - Inside the `try` it is swallowed and logged. Whether the next build is then incremental instead of full is unverified.
- **4 [Low] Public API wider than needed.**
  - `getDuplicateUUIDs`, the 3-argument `checkDuplicateUUID` and `countPersists` are public in the exported package `com.servoy.eclipse.model.builder` but only used inside it.
  - Only `isDuplicateConfirmedInMemory` needs to be public, because of the test package.
- **Not new:** with duplicate files loaded at startup, giving the copy a new UUID does not clean the in-memory index until the solution is reloaded. Same as before round 3.
- **Not new:** if the copy also gets a new inner name, the marker on the original stays until the next full project check. Same as the old behaviour.

## Manual test plan

**Verifying the fix**
1. Load a large solution (ideally the one from the ticket) and run a full Servoy build. Compare build time against `62ae7701ca~1`. No baseline numbers exist.
2. Copy `forms/orig.frm` to `forms/copy.frm` (same inner name) and refresh. The "UUID duplicate found" marker should appear on the form.
3. Delete `copy.frm`. The marker should disappear on the next auto-build.
4. Watch the progress bar and label during a full build with several modules. The percentage should move across all modules and forms.

**Regression checks**
1. **Copy with new UUID (was High):** repeat step 2, then change the `uuid` inside `copy.frm` to a new GUID and save. The marker on `orig` should disappear on the next automatic build, without a clean build.
2. **Finding 1:** copy `forms/a.frm` to `forms/b.frm` and edit only `"name": "a"` to `"b"`. Wait for the auto-build. Compare with the pre-`62ae7701ca` builder, which shows the marker on `b.frm`. Then restart the IDE and check how the two forms are reported.
3. **Pure rename:** `git mv forms/f1.frm forms/f2.frm` and change the inner name to `f2`. There should be no duplicate marker on `f2`. Also rename a form through the Servoy UI.
4. **Rename without editing the inner name:** the marker is expected here, as with the old scan.
5. **Delete the original:** repeat step 2, then delete `orig.frm` and keep the copy. There should be no stuck marker.
6. **Finding 2:** start the IDE on a solution with two `.frm` files sharing one UUID and a few elements. Count the per-element duplicate markers.
7. **Finding 3:** start a multi-module first build and cancel between modules. Check that the remaining modules get their markers on the next build.
8. **Script UUID change:** change the UUID of a method in a `.js` file but keep the name. There should be no marker.
9. **Modules and incremental:** two modules sharing one form UUID, and an incremental build after duplicating a file. Check marker counts and that detection still works.
10. **Quick fix:** with a duplicate marker present, open the quick fix (`ServoyQuickFixGenerator`) and check that it still gets the `Uuid` and `SolutionName` attributes.

**Automated checks:** `ServoyBuilderDuplicateUUIDTest` and `DeveloperPersistIndexDuplicateUUIDTest` (plainjunit, `com.servoy.eclipse.model.tests`, `junitVersion: "6"`). They call the real `isDuplicateConfirmedInMemory` with injected paths. These tests were not run in this review, and Spotbugs was not run either. Not covered by any test: the deserializer clear and its trigger, the Site 3 guard for form elements, the own-file guard on real workspace paths, `checkDuplicateUUID`'s clear tail, `countPersists`, and progress. The `OtherFileFlag` test only reads back a runtime property and is not evidence for the feature.

## Possible improvements / follow-ups
- **Finding 1:** at `SolutionDeserializer.java:1921`, `getFileName(retval)` is the persist's previous file. Record it only if it still exists on disk: in a pure external rename it is gone, in a copy it is still there.
- Add tests for the deserializer clear, the form-element guard, the own-file guard with real paths (case, separators, null `getLocation()`), and the copy-plus-rename case.
- Make `getDuplicateUUIDs`, the 3-argument `checkDuplicateUUID` and `countPersists` package-private. Keep only `isDuplicateConfirmedInMemory` public for the test.
- Consider `forgetLastBuiltState()` handling when `split()` cancels between modules.
- The `worked(countPersists(form) - 1)` lump after a form is coarse: progress stalls inside a large form and then jumps.
- `DeveloperPersistIndexDuplicateUUIDTest` and `ServoyBuilderDuplicateUUIDTest` still use reflection. Since `acef0ffad5` (SVY-21640) the sdd skills say to avoid it. As a fragment, the test could call the package-private method directly.
- Measure build time before and after on the ticket's solution.
- Minor: `AGENTS.md` says version `2026.6.0-SNAPSHOT` while the newest unreleased Jira version is 2026.12.
