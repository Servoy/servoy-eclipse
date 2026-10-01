# Triage Report — SVY-21431

**Verdict:** PROCEED

## Reported problem

In Servoy Developer, a **Servoy Build** (the `com.servoy.eclipse.core.servoyBuilder`
incremental/full builder) takes a long time, and the reporter's suspended-thread stack
trace points the finger at `ServoyBuilder.checkDuplicateUUID(IPersist, IProject)`. That
method calls `SolutionDeserializer.getUUID(File)` for sibling files in a folder, which
reads each file from disk and parses it via `new ServoyJSONObject(txtFileContent, false)`
— the hot frame in the trace is `ServoyJSONObject.replaceEmbeddedStringNewlines(String)`.

Two distinct complaints:
1. **Performance** — the duplicate-UUID check reads many files from disk during the build.
2. **Progress reporting** — the build's reported percentage "stays the same" during this
   long operation, so the user gets no feedback.

Reporter-proposed solutions (noted, not assumed correct):
- "improve this duplicate uuid thing (or maybe even remove it, shouldn't happen much anyway)"
- Architect comment (Johan Compagner): *"why does it read so many files? shouldn't the
  solution already be in memory (the whole persist structure of a solution)?"*

## Root-cause assessment

Two independent issues, both real and both in this project's code.

**A. Redundant disk reads in the duplicate-UUID check.**
- `ServoyBuilder.checkDuplicateUUID` (`com.servoy.eclipse.model/.../builder/ServoyBuilder.java:1772`)
  is already **guarded** — it only runs the expensive body when the persist carries the
  runtime flag `SolutionDeserializer.POSSIBLE_DUPLICATE_UUID` (line 1776). It is invoked
  from two guarded call sites: `ServoyFormBuilder.java:311-315` (form elements) and
  `ServoyBuilder.java:2037-2040` (top-level solution objects).
- When the flag *is* set, the method lists every JSON file in the persist's folder
  (`file.listFiles(...)`, line 1794) and calls `SolutionDeserializer.getUUID(File)` on each
  (line 1806). `getUUID(File)` (`SolutionDeserializer.java:2236`) does
  `Utils.getTXTFileContent(file)` then `new ServoyJSONObject(txtFileContent, false)` — a
  full read + JSON parse (and the newline-normalization pass seen in the stack trace) of
  every sibling file, **just to extract one `uuid` property**.
- The architect's observation is the crux: the whole persist structure of the solution is
  already deserialized in memory (`FlattenedSolution` / the `IPersist` tree). A duplicate
  UUID can therefore be detected by walking in-memory persists rather than re-reading and
  re-parsing files off disk. The current implementation goes back to the filesystem for
  data it already holds. That is the root inefficiency.
- The `POSSIBLE_DUPLICATE_UUID` flag is set in three deserialization branches
  (`SolutionDeserializer.java:1880, 1896, 1906`) — same-name/different-file, incoming
  UUID collision with `solutionUUIDs`, and filename mismatch. So the check is not
  gratuitous; it exists to catch genuine copy/rename collisions. Fully removing it (the
  reporter's fallback idea) would drop a real validation and is riskier than making it
  read from memory.

**B. No progress reporting during the check.**
- The builder loops over persists in `ServoyBuilder.checkServoyProject` /
  `ServoyFormBuilder.addFormMarkers` without advancing the `IProgressMonitor` around the
  duplicate-UUID work, so the percentage appears frozen during a long solution build. This
  is a UX defect independent of the performance fix.

This is **not** a regression from a specific recent commit — `checkDuplicateUUID` dates to
2010 and was last meaningfully touched in 2020 (SVY-14841, incremental-build scoping). The
`getUUID(File)` helper was relocated by the 2026-06 end-of-line-normalization change
(commit `6a95cafbc1`) but its file-reading behavior predates that. The slowness is
inherent to the design, exposed on large solutions, not newly introduced.

## Ticket premise check

- **"improve this duplicate uuid thing"** — holds up. The check is legitimately slow
  because it re-reads/parses sibling files for data already in memory. Improving it is the
  right call.
- **"or maybe even remove it"** — does *not* hold up. The check guards against real UUID
  collisions from copy/rename/team-merge scenarios (three distinct set-sites of the flag).
  Removing it silently drops that validation; not recommended.
- **Architect's "shouldn't the solution already be in memory?"** — correct and is the
  guiding principle for the fix: detect duplicates against the in-memory persist tree
  instead of the filesystem.
- The **progress-reporting** complaint is a separate, valid sub-issue the "duplicate uuid"
  framing partly obscures; it should be addressed alongside but tracked distinctly.

## Approaches considered

1. **Detect duplicate UUIDs from the in-memory model, drop the per-file disk scan.**
   Replace the `listFiles` + `getUUID(File)` loop with a lookup against already-loaded
   persists (e.g. a UUID→persist index built once per build over the
   `FlattenedSolution`/project persists), keeping the same `UUIDDuplicate` marker output.
   - Pros: removes the dominant cost (disk read + JSON parse per sibling file); uses data
     already resident; preserves the validation; matches the architect's direction.
   - Cons: must ensure the in-memory index covers the same set the file scan did
     (workspace files across a folder), and that the "different file, same uuid" semantics
     are reproduced without false positives/negatives.

2. **Cache/memoize `getUUID(File)` results (lighter-touch).**
   Keep the file-based approach but avoid re-parsing: cache UUID-per-file for the build
   pass, and/or extract the UUID with a cheap targeted read instead of full
   `ServoyJSONObject` construction + newline normalization.
   - Pros: smaller, lower-risk change; keeps existing control flow.
   - Cons: still touches the filesystem; a weaker fix than (1); the architect explicitly
     asked why it reads files at all.

3. **Add/repair progress reporting only.**
   Advance the `IProgressMonitor` (sub-tasks + `worked()`) around the duplicate-UUID and
   form-marker loops so the percentage moves.
   - Pros: fixes the "frozen percentage" complaint directly; low risk.
   - Cons: does nothing for the actual slowness — should accompany, not replace, (1).

4. **No code change.**
   - Pros: none — the check is confirmed to re-read files for in-memory data, and progress
     genuinely stalls.
   - Cons: leaves both a real performance problem and a real UX problem unaddressed;
     contradicts the architect's own comment. Rejected.

## Recommendation

**PROCEED with Approach 1 (in-memory duplicate-UUID detection) as the primary fix, plus
Approach 3 (progress reporting) as an accompanying improvement.**

- Rework `checkDuplicateUUID` (and its two guarded call sites) to determine duplicates
  from the in-memory persist tree — build a UUID→persist(s) map once for the project/
  solution being built and flag a persist whose UUID collides with another persist backed
  by a *different* workspace file, emitting the existing `MarkerMessages.UUIDDuplicate`
  marker. Eliminate the `file.listFiles` + `SolutionDeserializer.getUUID(File)` disk loop.
- Keep the `POSSIBLE_DUPLICATE_UUID` runtime-flag gate so the work still only happens for
  persists deserialization actually flagged as suspect (it already scopes the check well).
- Advance the `IProgressMonitor` around the per-persist marker/duplicate-UUID work in
  `checkServoyProject` / `ServoyFormBuilder.addFormMarkers` so build progress is visible.
- **Do not remove** the duplicate-UUID validation — it guards genuine collision cases.

Fallback if the in-memory index proves to have different coverage than the file scan:
Approach 2 (memoize/cheapen `getUUID(File)`) as a lower-risk interim, still paired with
Approach 3.

Primary files in scope:
- `com.servoy.eclipse.model/src/com/servoy/eclipse/model/builder/ServoyBuilder.java`
  (`checkDuplicateUUID`, call site ~line 2037, progress monitor in `checkServoyProject`)
- `com.servoy.eclipse.model/src/com/servoy/eclipse/model/builder/ServoyFormBuilder.java`
  (call site ~line 311, progress in `addFormMarkers`)
- `com.servoy.eclipse.model/src/com/servoy/eclipse/model/repository/SolutionDeserializer.java`
  (`getUUID(File)` — only if Approach 2 is used)

## Git history findings

- `checkDuplicateUUID` originates from 2010 (commit `c429910792`); last meaningful build
  rework was SVY-14841 in 2020 (commit `10671e7d28`, incremental-build file scoping). No
  recent regression introduced the slowness — it is a longstanding design cost surfaced on
  large solutions.
- `SolutionDeserializer.getUUID(File)` was relocated by the 2026-06 end-of-line
  normalization change (commit `6a95cafbc1`, Johan Compagner); the newline-normalization
  frame (`ServoyJSONObject.replaceEmbeddedStringNewlines`) in the reporter's stack trace
  comes from that `ServoyJSONObject` parse path, but the underlying per-file read behavior
  predates it.
- No dependency / target-platform version bump is implicated.
- No prior `docs/SVY-21431*` spec exists.
