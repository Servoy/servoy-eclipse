# Spec: SVY-21431 — Improve Servoy Builder performance (duplicate-UUID check)

## 1. Goal

Speed up **Servoy Build** in Servoy Developer by eliminating the redundant per-file disk
reads performed by `ServoyBuilder.checkDuplicateUUID(IPersist, IProject)` during a build,
and give the user visible build progress while duplicate-UUID validation runs. The
duplicate-UUID validation itself is preserved (it guards real copy/rename/team-merge
collisions) but is satisfied from the already-loaded in-memory persist model instead of
re-reading and re-parsing sibling `.obj`/`.js` files off disk.

## 2. Background

### 2.1 The reported hot path

A suspended-thread stack trace on the ticket shows build time being spent in:

```
ServoyJSONObject.replaceEmbeddedStringNewlines(String)
ServoyJSONObject.<init>(String, boolean, boolean, boolean)
SolutionDeserializer.getUUID(File)
ServoyBuilder.checkDuplicateUUID(IPersist, IProject)
ServoyFormBuilder$1.visit(IPersist)  → addFormMarkers(...)
ServoyBuilder$4.visit(IPersist)      → checkServoyProject(...)
ServoyBuilder.fullBuild(...)
```

The architect's comment on the ticket sets the direction: *"why does it read so many
files? shouldn't the solution already be in memory (the whole persist structure of a
solution)?"*

### 2.2 What the current file-based check does

`ServoyBuilder.checkDuplicateUUID(IPersist, IProject)`
(`com.servoy.eclipse.model/src/com/servoy/eclipse/model/builder/ServoyBuilder.java:1772`):

- Runs its body only when the persist carries the runtime flag
  `SolutionDeserializer.POSSIBLE_DUPLICATE_UUID` (line 1776) — so it is already scoped to
  persists that deserialization flagged as *suspect*.
- When flagged, it lists **every** JSON file in the persist's folder
  (`file.listFiles(...)`, line 1794) and calls `SolutionDeserializer.getUUID(File)` on each
  (line 1806).
- `SolutionDeserializer.getUUID(File)` (`SolutionDeserializer.java:2236`) does
  `Utils.getTXTFileContent(file)` (full file read) then
  `new ServoyJSONObject(txtFileContent, false)` (full JSON parse + the newline
  normalization pass seen in the trace) purely to read one `uuid` property.
- On a match (same UUID, different file) it adds a `MarkerMessages.UUIDDuplicate` marker.

It has two guarded call sites:
- `ServoyFormBuilder.java:311-315` (per form element)
- `ServoyBuilder.java:2037-2040` (per top-level solution object)

### 2.3 An in-memory duplicate-UUID mechanism already exists

The codebase **already** tracks duplicate UUIDs in memory, independent of the file scan:

- `DeveloperPersistIndex` (`com.servoy.eclipse.model/.../DeveloperPersistIndex.java`)
  keeps `Map<UUID, List<IPersist>> duplicatesUUIDs` (line 64). Its `putInCache(IPersist)`
  override (line 311) detects when a second persist with an already-present UUID is cached
  and records the collision. `getDuplicateUUIDList()` (line 371) returns the map, pruning
  stale entries.
- `ServoyBuilder.checkPersistDuplicateUUID()` (`ServoyBuilder.java:1207`) consumes that map
  via `DeveloperFlattenedSolution.getDuplicateUUIDList()` and emits
  `MarkerMessages.UUIDDuplicateIn` markers on the active project. It is already called from
  the build flow (`ServoyBuilder.java:1921`) and from
  `ServoyBuilderUtils`/`ServoyValuelistBuilder`/`ServoyRelationBuilder`/`ServoyMediaBuilder`.

So the model already holds the information the file-scan re-derives from disk — for the
case where two distinct persists exist. The per-persist file-scanning
`checkDuplicateUUID(IPersist, IProject)` is the redundant, slow path. Note (see §3.1): this
in-memory index covers only the Site 2 set-site; the Site 1 and Site 3 set-sites keep a
single reused instance that the index does not record, which is why §3.1 adds the
`DUPLICATE_UUID_OTHER_FILE` companion property rather than relying on the index alone.

### 2.4 Progress reporting

`ServoyBuilder` holds an `IProgressMonitor monitor` (line 602) and only uses it for
cancellation (`checkCancel()`, line 3568). During the per-persist visitor loops in
`checkServoyProject` / `ServoyFormBuilder.addFormMarkers` the monitor is not advanced, so
the reported percentage appears frozen during a long build.

### 2.5 Git history

- `checkDuplicateUUID` dates to 2010 (commit `c429910792`); last meaningful build rework was
  SVY-14841 in 2020 (commit `10671e7d28`, incremental-build file scoping). The slowness is a
  longstanding design cost, not a recent regression.
- `SolutionDeserializer.getUUID(File)` was relocated by the 2026-06 end-of-line
  normalization change (commit `6a95cafbc1`); that is why `replaceEmbeddedStringNewlines`
  appears in the trace, but the per-file read behavior predates it.
- No dependency / target-platform version bump is involved.

## 3. Design

### 3.1 Confirm duplicates entirely from in-memory state — no build-time file reads

Rework `ServoyBuilder.checkDuplicateUUID(IPersist, IProject)` so that, when a persist is
flagged `POSSIBLE_DUPLICATE_UUID`, it confirms the duplicate **without touching the
filesystem** — removing `file.listFiles` + `SolutionDeserializer.getUUID(File)` and
introducing no replacement disk scan.

The challenge is that `POSSIBLE_DUPLICATE_UUID` is set at three different points in
`SolutionDeserializer` and they do **not** all leave two in-memory persists behind. Verified
empirically (a throwaway probe over `DeveloperPersistIndex.putInCache` /
`getDuplicateUUIDList`, run on the PDE harness):

- **Site 2** (`SolutionDeserializer.java:1896`) — a newly-created persist whose UUID already
  appeared in `solutionUUIDs`. Two **distinct** instances end up in the tree, so the
  developer persist index records them in `duplicatesUUIDs` and reports the collision.
- **Site 1** (`SolutionDeserializer.java:1880`) — same name, different UUID in the same file.
  The existing persist instance is **reused** and its UUID reset to the incoming one
  (`resetUUID(uuid)`). Only **one** instance carries that UUID → the index reports nothing.
- **Site 3** (`SolutionDeserializer.java:1906`) — an existing node re-read from a **different
  file**. Still the **same single instance** → the index reports nothing.

So the in-memory duplicate index alone covers Site 2 but misses Site 1 and Site 3. Relying on
the index only would silently drop the duplicate-UUID marker for those two cases; a disk-scan
fallback would re-introduce exactly the per-file read cost the ticket wants removed (and it
would fire precisely in the Site 1/Site 3 cases). Neither is acceptable.

**Design: carry the evidence on the flag.** At the moment deserialization sets
`POSSIBLE_DUPLICATE_UUID` for the single-instance cases, it already holds the conflicting
`file`. Record that file's name on the persist in a companion runtime property, so the
builder can confirm the duplicate from memory:

- Add `SolutionDeserializer.DUPLICATE_UUID_OTHER_FILE` — a
  `RuntimeProperty<String>` declared next to `POSSIBLE_DUPLICATE_UUID`.
- At **Site 3 only** (same persist re-read from a second file), in addition to setting
  `POSSIBLE_DUPLICATE_UUID`, set `DUPLICATE_UUID_OTHER_FILE` to the other file's **absolute
  path** (`file.getAbsolutePath()` — the file is in hand there; no extra I/O).
- **Site 1 deliberately does not set it** (peer review correction). Site 1 is the same-file /
  same-name / other-UUID case: no other file declares the UUID, and the old sibling-file scan
  found nothing there. Recording this file would raise a "UUID duplicate found" error that never
  appeared before. Site 2 does not need it either; the index already covers that case.

`checkDuplicateUUID` then confirms from two in-memory sources, in order:

1. **Site 2** — query the developer persist index the same way `checkPersistDuplicateUUID()`
   does: `FlattenedSolution fs = ServoyModelFinder.getServoyModel().getFlattenedSolution();`
   and, when `fs instanceof DeveloperFlattenedSolution`,
   `((DeveloperFlattenedSolution) fs).getDuplicateUUIDList().get(uuid)` — a collision exists
   if the list holds a persist other than this one.
2. **Site 3** — if the index reported nothing, a non-null `DUPLICATE_UUID_OTHER_FILE` confirms
   the duplicate only when **both** hold:
   - the recorded file is **not the persist's own file** (derived from its current name via
     `SolutionSerializer.getFilePath`). After an external rename that also changes the inner
     name, the "other" file is the persist's own new file; the old scan skipped the own file
     too (peer review round 2, R1);
   - the recorded file **still exists** (an `IFile.exists()` workspace resource-tree lookup, no
     file is read).

   A copy given a **new UUID** (peer review round 2, R2) is handled where the copy is read
   again, not with an extra cache: when `deserializePersist` re-reads (`testExisting`) a json
   file whose UUID matches no existing persist, `clearDuplicateUUIDOtherFile(parent, file)`
   drops the `DUPLICATE_UUID_OTHER_FILE` of any sibling that recorded that file. The next build
   then no longer confirms the duplicate. So the marker clears when the copy is deleted,
   renamed, or given a new UUID.

   **Only persists with their own file record it.** Form elements are deserialized from their
   form's file, so a copied `.frm` also re-reads every element UUID of the original. The old
   scan compared only each file's top-level UUID and never reported elements, so Site 3 sets
   `DUPLICATE_UUID_OTHER_FILE` only when `!SolutionSerializer.isCompositeItem(persist)`, and the
   clear pass is skipped when the parent is a composite with items (a form). This also keeps the
   clear pass from running once per element of a re-read form.

   When neither confirms, the not-found tail clears both flags, so the marker does not outlive
   the duplicate.

The in-memory duplicate map (`getDuplicateUUIDList()`, which prunes stale entries) is fetched
once per pass through `ServoyBuilder.getDuplicateUUIDs()` (once per solution visit in
`checkServoyProject`, once per form in `ServoyFormBuilder.addFormMarkers`, and only when an
element is flagged) and passed to `checkDuplicateUUID(persist, project, indexDuplicates)`,
instead of being fetched again for every flagged persist.

The decision lives in the unit-testable helper
`ServoyBuilder.isDuplicateConfirmedInMemory(persist, indexDuplicates, ownFilePath, otherFileExists)`.

On confirmation, emit the existing `MarkerMessages.UUIDDuplicate` marker on the flagged
persist's file, preserving the current marker type, id `DUPLICATION_UUID_DUPLICATE`,
priority, and file-for-location behavior. When nothing is found, clear **both** runtime
flags so the persist is not re-checked on subsequent builds.

No `file.listFiles`, no `SolutionDeserializer.getUUID(File)`, no sibling-file reads of any
kind during the build.

### 3.2 Keep the `POSSIBLE_DUPLICATE_UUID` gate

The runtime-flag gate at the two call sites (`ServoyFormBuilder.java:311`,
`ServoyBuilder.java:2037`) and inside the method stays. It correctly scopes the work to
persists that deserialization actually flagged as suspect (set at
`SolutionDeserializer.java:1880, 1896, 1906`), so unflagged persists do no work. The new
`DUPLICATE_UUID_OTHER_FILE` property is set only at the Site 3 set-site (`:1906`), always
alongside `POSSIBLE_DUPLICATE_UUID`.

### 3.3 Progress reporting during the build

Advance the existing `IProgressMonitor` so the build percentage moves during the
per-persist marker/duplicate-UUID work:

- `build(...)` creates **one** root `SubMonitor` for the whole build and hands every project
  build its own child via `split(...)`: on the uninitialised-dependencies path each active
  module gets an equal share (`setWorkRemaining(modules)` + `split(1)`), a full build gets the
  whole range, an incremental build half each for the project delta and the resources-project
  delta. Previously `SubMonitor.convert` was called repeatedly on the same monitor, so the first
  project consumed the whole bar (peer review round 2, R3).
- `checkServoyProject` sets the child's total to `ServoyBuilder.countPersists(solution)` (an
  in-memory walk over every persist, form elements included) before `solution.acceptVisitor`.
- The visitor calls `reportBuildProgress(persist)` for every persist: a short `subTask` label
  plus `worked(1)`. After `ServoyFormBuilder.addFormMarkers` returns for a form, it adds
  `worked(countPersists(form) - 1)` for the elements validated there, so the slow form
  validation from the ticket's stack trace moves the bar in proportion to the form's size.
  Progress inside a single form is not reported.
- Must not change cancellation semantics — `checkCancel()` (line 3568) continues to work.
- Keep it lightweight: no new long operations, just surfacing progress on the existing loop.

### 3.4 No behavioral change to markers seen by users

The user-visible outcome (a duplicate-UUID problem marker appears when, and only when, two
persists backed by different workspace files share a UUID) must be unchanged. Only the
*mechanism* (in-memory vs disk) and *progress reporting* change.

## 4. Implementation plan

1. **`SolutionDeserializer.java`** (`com.servoy.eclipse.model/src/com/servoy/eclipse/model/repository/SolutionDeserializer.java`):
   - Declare `public static final RuntimeProperty<String> DUPLICATE_UUID_OTHER_FILE` next to
     `POSSIBLE_DUPLICATE_UUID`.
   - At the Site 3 set-site only (`~:1906`, existing node re-read from another file; Site 1
     at `~:1880` must NOT set it, see §3.1), after setting
     `POSSIBLE_DUPLICATE_UUID`, also set `DUPLICATE_UUID_OTHER_FILE` to
     `file.getAbsolutePath()`. Leave Site 2 (`~:1896`) unchanged — the
     in-memory index covers it.
2. **`ServoyBuilder.checkDuplicateUUID(IPersist, IProject)`**
   (`com.servoy.eclipse.model/src/com/servoy/eclipse/model/builder/ServoyBuilder.java`, ~1772):
   - Remove the `file.listFiles(...)` + `SolutionDeserializer.getUUID(File)` loop (and any
     helper); introduce no disk-scan fallback.
   - Confirm from memory via `isDuplicateConfirmedInMemory`: first `DeveloperFlattenedSolution.getDuplicateUUIDList().get(uuid)`
     (Site 2 — a persist other than this one in the list); if that reports nothing, treat a
     non-null `DUPLICATE_UUID_OTHER_FILE` counts as confirmation only when it is not the
     persist's own file and that file still exists in the workspace (Site 3).
   - In `SolutionDeserializer.deserializePersist`, when the read uuid matches no existing
     persist, call `clearDuplicateUUIDOtherFile(parent, file)` so siblings that recorded this
     file drop the property (the copy now declares another uuid).
   - On confirmation emit the existing `MarkerMessages.UUIDDuplicate` marker with the current
     type/id (`DUPLICATION_UUID_DUPLICATE`)/priority/file-for-location arguments.
   - Keep the `POSSIBLE_DUPLICATE_UUID` guard; in the "not found" tail clear **both**
     `POSSIBLE_DUPLICATE_UUID` and `DUPLICATE_UUID_OTHER_FILE`.
3. **Progress reporting** — one root `SubMonitor` in `build(...)`, a `split(...)` child per
   `fullBuild`/`incrementalBuild` call; in `checkServoyProject` set the total from
   `countPersists(solution)` before `solution.acceptVisitor`; `reportBuildProgress(IPersist)`
   (`subTask(name)` + `worked(1)`) at the top of the persist visitor, plus
   `worked(countPersists(form) - 1)` after `addFormMarkers`. Preserve `checkCancel()` behavior;
   introduce no new long operations.
4. **Imports / cleanup:** remove now-unused `FileFilter` / `java.io.File` usage tied to the
   deleted scan if no longer referenced elsewhere in the class. Run organize-imports.
5. **Post-edit checklist (AGENTS.md):** `getCompilationErrors`, quick-fix, organize imports,
   format; address top-two-severity SpotBugs in changed code.

## 5. Acceptance criteria

- [ ] `ServoyBuilder.checkDuplicateUUID(IPersist, IProject)` performs **no filesystem reads**
      — no `file.listFiles(...)`, no `SolutionDeserializer.getUUID(File)`, and no disk-scan
      fallback. It confirms duplicates only from in-memory state.
- [ ] **Site 2** (two distinct in-memory persists share a UUID): a duplicate-UUID problem
      marker is produced via `getDuplicateUUIDList()`.
- [ ] **Site 3** (same persist re-read from a second file): a duplicate-UUID problem marker is
      produced via the `DUPLICATE_UUID_OTHER_FILE` runtime property recorded by
      `SolutionDeserializer`, so detection coverage matches the old file scan.
- [ ] **Site 1** (same file, same name, other UUID) raises **no** new marker, as with the old
      file scan; `SolutionDeserializer` does not set `DUPLICATE_UUID_OTHER_FILE` there.
- [ ] `SolutionDeserializer` sets `DUPLICATE_UUID_OTHER_FILE` to the other file's absolute path
      at the Site 3 set-site, alongside `POSSIBLE_DUPLICATE_UUID`.
- [ ] The marker does not outlive the duplicate: once the recorded other file is deleted,
      renamed, or given a new UUID, the check no longer confirms it, and **both**
      `POSSIBLE_DUPLICATE_UUID` and `DUPLICATE_UUID_OTHER_FILE` are cleared, so the persist is
      not re-checked next build.
- [ ] A recorded "other file" that is the persist's own current file (external rename) raises no
      marker, as with the old scan.
- [ ] Copying a whole `.frm` raises the duplicate marker for the form only, not one per element,
      as with the old scan.
- [ ] The duplicate-UUID validation is **not** removed; unflagged persists still do no work
      (the `POSSIBLE_DUPLICATE_UUID` gate is preserved at both call sites and in the method).
- [ ] During a solution build, the reported progress percentage advances while duplicate-UUID
      / form-marker validation runs (no longer frozen), across all modules of a multi-module
      first build, not only the first one.
- [ ] Build cancellation still works (`checkCancel()` unaffected).
- [ ] `com.servoy.eclipse.model` compiles with no new errors; no new top-two-severity SpotBugs
      in the changed code.
- [ ] Tests cover the decision: `ServoyBuilderDuplicateUUIDTest` (index-confirmed without
      consulting files, only-self-in-index, recorded copy exists, recorded copy deleted,
      recorded file is the persist's own file, nothing recorded) and
      `DeveloperPersistIndexDuplicateUUIDTest` (the index contract). Both are in
      `com.servoy.eclipse.model.tests` under the `plainjunit` package. Not automated: the
      deserializer set-sites and `clearDuplicateUUIDOtherFile` (the new-UUID case), the workspace
      `IFile.exists()` lookup and the progress code; they are covered by the manual test plan in
      `docs/reviews/SVY-21431-review-summary.md`.

## 6. Out of scope

- Removing the duplicate-UUID validation entirely (reporter's fallback idea) — rejected in
  triage; the check guards real collisions.
- Changing *when* `SolutionDeserializer` sets `POSSIBLE_DUPLICATE_UUID` (the three set-sites
  and their conditions are unchanged). Only the companion `DUPLICATE_UUID_OTHER_FILE` is
  additionally recorded at the single-instance set-sites.
- Reworking `checkPersistDuplicateUUID()` / `DeveloperPersistIndex` duplicate tracking beyond
  what is needed to reuse it (its detection logic stays as-is).
- Broader Servoy Builder performance work unrelated to the duplicate-UUID path.
- The `SolutionDeserializer.getUUID(File, int position)` overload (used elsewhere) is not
  touched.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Does the in-memory `duplicatesUUIDs` index cover every case the file scan caught? **Resolved:** verified empirically that it covers only Site 2; Site 1 and Site 3 keep a single instance and are missed. Addressed by recording `DUPLICATE_UUID_OTHER_FILE` at Site 3 so no disk fallback is needed. Peer review then showed Site 1 must not record it: no other file declares that UUID and the old scan raised nothing there. | implementer | resolved |
| Should the per-persist `checkDuplicateUUID` be folded into `checkPersistDuplicateUUID`? **Resolved:** kept as a thin per-persist confirmation (index + flag); its `MarkerMessages.UUIDDuplicate` marker and the index-wide `UUIDDuplicateIn` marker already coexisted before this change, so marker behavior is unchanged. | implementer | resolved |
| Progress granularity: per-persist `subTask(name)` **and** `worked(1)` against an all-persists total (see §3.3), with a form counting as all its elements once `addFormMarkers` returns, and one `split()` child per project build. Progress within a single form is not reported. | implementer | resolved |
| Runtime-only checks from the peer review not verified in the IDE: build-time comparison on the ticket's solution, cross-module `UUIDDuplicate` + `UUIDDuplicateIn` double marker, incremental-build detection, quick-fix attributes. | reviewer / QA | open |
