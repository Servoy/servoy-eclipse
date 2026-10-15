# Spec: SVY-21579 — WAR exporter prints `[Fatal Error] :1:1: Premature end of file.` on fresh-workspace import

> **Root cause corrected.** An earlier version of this spec blamed DLTK's `.buildpath`
> parse and proposed a two-repo fix (DLTK `ScriptProject` + an exporter `.buildpath` guard).
> A runtime breakpoint proved that wrong. The real culprit is WST's
> `ServerPreferenceModifyListener`, triggered during `Project.open()`. The DLTK and
> `.buildpath` changes have been reverted. This spec now describes the single, correctly
> targeted fix: suppress the spurious stderr line at the exporter boundary. See
> `docs/SVY-21579-triage.md` for the full investigation and the captured stack trace.

## 1. Goal

Stop the command-line WAR/solution exporter (`war_export.bat` / `war_export.sh`) from
printing

```
[Fatal Error] :1:1: Premature end of file.
```

to the console once per imported project when run against a fresh workspace (a clean git
clone with no `.metadata`). The message is cosmetic noise — the WAR builds correctly — but
it is alarming in CI/Bitbucket pipeline logs. The fix removes exactly that line while the
exporter imports/opens projects, and leaves every other stderr output untouched.

## 2. Background

### 2.1 Symptom (confirmed)

Running, e.g.

```
war_export.bat -data "C:/path/to/fresh_clone" -verbose -s <mainSolution> -o C:\out \
  -warFileName test -dbi -defaultAdminUser <u> -defaultAdminPassword <p>
```

against a fresh clone prints `[Fatal Error] :1:1: Premature end of file.` after almost every
imported project. The WAR is correct. Opening Servoy Developer once makes the message
disappear (the metadata/preferences are then populated). The reporter runs `war_export.sh`
in a Bitbucket pipeline, which always starts from a fresh workspace.

### 2.2 Confirmed root cause (runtime stack trace)

> Confirmed by a live debug run — see `docs/SVY-21579-triage.md` for the captured stack.

The line is the stock JAXP message printed to `System.err` by a **handler-less**
`DocumentBuilder.parse(...)` given **empty input**. It originates in **WST**
(`org.eclipse.wst.server.core`), in
`ServerPreferenceModifyListener.removeLockedServerRuntimePreference` (line 56) — not in
Servoy code and not in DLTK.

Chain:

1. The exporter opens each project in
   `AbstractWorkspaceExporter.importExistingAndOpenClosedProjects` (`p.open(null)`, line 492).
2. `Project.open()` refreshes and reads the project's `.settings` preferences
   (`ProjectPreferences.read` → `File.updateMetadataFiles`) and fires the preference-apply
   listeners.
3. WST's `preApply` → `removeLockedServerRuntimePreference` does, in effect:

   ```java
   File f = new File(prefs.get("runtimes", ""));   // key "runtimes", default ""
   if (!f.exists()) return;
   DocumentBuilderFactory.newInstance().newDocumentBuilder()
       .parse(new InputSource(new StringReader(f.toString())));   // parses the PATH string
   ```

   On a fresh workspace the `"runtimes"` preference is absent → defaults to `""` → WST parses
   the empty string with a handler-less parser → JAXP's default error handler prints
   `[Fatal Error] :1:1: Premature end of file.` to stderr and throws; WST swallows it, so
   nothing reaches `.metadata/.log`.

The parsed empty input is **not** a committed repo file (which is why the reporter found no
empty XML in the tree); it is the WST `runtimes` preference value.

### 2.3 Why it regressed (not a Servoy/DLTK code change)

The platform/runtime moved between the 2024.3 line and master: the target platform bumped
Eclipse `releases/2023-12` → `releases/2026-06` (newer `org.eclipse.wst.server.core`) and the
bundled JRE moved to OpenJDK 25, whose default handler-less JAXP parser now routes the
empty-input fatal error to stderr (`DocumentBuilder.parse("")` reproduces the line on the
bundled JRE). No Servoy or DLTK source change is involved.

### 2.4 Why suppression (and not a code fix of the parser)

The faulty parser is inside `org.eclipse.wst.server.core`, a third-party WTP bundle consumed
from the target platform's p2 site — not a Servoy fork we rebuild/ship. So the honest,
lowest-risk fix we own is to suppress exactly this noise line at the exporter boundary, in
the exact window where it occurs. The WAR output is already correct.

### 2.5 Code anchor

`com.servoy.eclipse.exporter.solution`
`src/com/servoy/eclipse/exporter/apps/common/AbstractWorkspaceExporter.java`:

- `checkAndExportSolutions(T)` — contains the import/open/refresh block
  (`importExistingAndOpenClosedProjects(...)`, the aggregate-workspace loop, and the
  `refreshLocal` loop). This block drives every `Project.open()` that triggers the WST
  listener.
- `importExistingAndOpenClosedProjects(...)` — `p.open(null)` is at line 492 (the frame in
  the captured stack).
- The exporter already has `outputExtra(...)` (verbose), `output(...)`, `outputError(...)`
  (writes to `System.err`), and `ServoyLog.logError(...)`.

## 3. Design

### 3.1 Boundary stderr filter

Replace `System.err` with a filtering `PrintStream` for the duration of the project
import/open/refresh block in `checkAndExportSolutions`, and restore the original stream in a
`finally`:

- A nested `static final class PrematureEndOfFileFilterStream extends PrintStream` wraps the
  original `System.err`.
- It buffers output **line by line** (the JAXP default handler emits the message via
  `System.err.println(String)`), and when a completed line contains **both** `[Fatal Error]`
  and `Premature end of file.` it is dropped; every other line (and any buffered partial line
  on `flush`) is forwarded verbatim to the original stream.
- Matching on both tokens keeps the filter narrow: a genuinely different error — even another
  `[Fatal Error]` or another `Premature end of file.` in a different context — is only
  dropped if it is exactly this known noise line.

### 3.2 Scope

- The swap is confined to the import/open/refresh section of `checkAndExportSolutions` (the
  only place `Project.open()` runs), restored in `finally` so later export output is never
  filtered.
- No change to DLTK, no change to any `.buildpath` or `.settings` file, no change to the WAR
  contents or the export flow.

## 4. Implementation plan

> Single repository, **one commit** in `servoy-eclipse`. The earlier two-repo plan is void;
> the DLTK (`org.eclipse.dltk.core`) edits and the exporter `.buildpath` guard have been
> reverted. Commit subject must include `SVY-21579` and end with ` [ai]`.

File: `com.servoy.eclipse.exporter.solution/src/com/servoy/eclipse/exporter/apps/common/AbstractWorkspaceExporter.java`

1. In `checkAndExportSolutions`, wrap the project import/open/refresh block (from
   `importExistingAndOpenClosedProjects(...)` through the `refreshLocal` loop) in a
   `try { … } finally { System.setErr(originalErr); }`, after setting
   `System.setErr(new PrematureEndOfFileFilterStream(originalErr))`.
2. Add the nested `static final class PrematureEndOfFileFilterStream extends PrintStream`
   that line-buffers writes and drops only the `[Fatal Error] … Premature end of file.`
   line, forwarding everything else (including partial buffered content on `flush`).
3. Add the `java.io.PrintStream` import. Run `getCompilationErrors`, `organizeImports`,
   `formatFile`; fix any top-two-severity Spotbugs findings.

## 5. Acceptance criteria

- [ ] Running `war_export.bat`/`war_export.sh` against a fresh workspace prints **no**
      `[Fatal Error] :1:1: Premature end of file.` lines; the WAR still builds correctly.
- [ ] The filter drops **only** a line containing both `[Fatal Error]` and
      `Premature end of file.`; all other stderr output during import is forwarded unchanged.
- [ ] `System.err` is restored to the original stream after the import/open/refresh block
      (verified via the `finally`), so later export errors are not filtered.
- [ ] The suppression scope is limited to `checkAndExportSolutions`' import block; no DLTK
      change, no `.buildpath`/`.settings` file mutation, no change to WAR contents.
- [ ] No new compilation errors; imports organized; file formatted; no top-two-severity
      Spotbugs findings introduced.
- [ ] One commit in `servoy-eclipse`, subject containing `SVY-21579` and ending with ` [ai]`.

## 6. Out of scope

- Patching WST (`org.eclipse.wst.server.core.ServerPreferenceModifyListener`) — it is a
  third-party WTP bundle from the target platform, not a Servoy fork.
- Any DLTK (`org.eclipse.dltk.core`) change — the earlier `ScriptProject`/`.buildpath` work
  was off-target and has been reverted.
- Seeding or editing the WST `runtimes` preference to pre-empt the parse (approach 2 in
  triage) — more fragile than boundary suppression.
- Reverting the platform/JRE bump that surfaced the line (that is the environmental cause,
  not something to roll back).
- Any change to the generated WAR or the export flow (export already succeeds).

## 7. Open questions

| # | Question | Assumption taken for this spec |
|---|----------|-------------------------------|
| 1 | Match the noise line on both `[Fatal Error]` and `Premature end of file.`, or on the full exact string? | Both-token match: narrow enough to avoid hiding genuine errors, robust to minor whitespace/locale differences in the fixed JAXP text. |
| 2 | Suppress for the whole import/open/refresh block, or only around `importExistingAndOpenClosedProjects`? | The whole block — `refreshLocal` can also re-trigger `Project.open`-style preference reads; the `finally` restores `System.err` immediately after. |
| 3 | Should a WST patch be pursued upstream as a proper fix? | Out of scope here; could be filed separately against WTP. Boundary suppression resolves the ticket. |
