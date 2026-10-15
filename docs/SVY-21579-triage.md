# Triage Report — SVY-21579

**Verdict:** PROCEED

> **Investigation history.** This report went through three stages: an initial
> `NEEDS_INPUT` (the swallowed stack trace left two divergent candidates), then a
> `PROCEED` built on static analysis that blamed DLTK's `.buildpath` parse, and finally
> the **confirmed** root cause below, obtained by catching the live stack at a runtime
> breakpoint. **The DLTK `.buildpath` theory was wrong** — the earlier DLTK/`.buildpath`
> sections have been removed. The culprit is a WST (`org.eclipse.wst.server.core`)
> preference listener, not DLTK. The runtime stack trace is authoritative.

## Reported problem

Running the command-line WAR exporter (`war_export.bat` / `war_export.sh`) against a
**fresh workspace** (a clean git clone with no `.metadata` folder) prints

```
[Fatal Error] :1:1: Premature end of file.
```

to the console after almost every project is imported. The export still succeeds and
produces a working WAR, so the message is cosmetic, but it looks alarming in CI/build
logs (the reporter runs `war_export.sh` in a Bitbucket pipeline, which always starts from
a fresh workspace).

Key facts from the ticket and attachments:

- **Regression:** not seen with **2024.03.11 (build 3954)**; appears with **2026.3.1
  (build 4143)**, same repository and same command line. Same behaviour on Windows and
  Linux.
- Only happens when the exporter actually **imports** the projects
  (`AbstractWorkspaceExporter.importExistingAndOpenClosedProjects`, right after
  `p.create(...)` / `p.open(...)`). With an already-populated workspace it does not appear.
- `.metadata/.log` contains **no** `SAXParseException` or stack trace — only the parser's
  default error handler writes to stderr, so the exception is swallowed.
- The repository contains **no empty XML files**; `.project` / `.buildpath` files all have
  content. (This is itself a strong hint that the parsed empty input is **not** a committed
  repo file — see the confirmed cause.)

The ticket proposes no fix; it asks a **question**: which file/setting is being parsed as
XML at import time, and can the error be suppressed or at least logged with the offending
file path.

## Confirmed root cause (runtime stack trace)

The message is the stock JAXP line printed to `System.err` by a **handler-less**
`DocumentBuilder.parse(...)` given **empty input**. It originates in **WST**, in
`org.eclipse.wst.server.core.internal.ServerPreferenceModifyListener.removeLockedServerRuntimePreference`
(line 56), **not** in Servoy code and **not** in DLTK.

A breakpoint was hit during a live `war_export` debug run; the captured `main`-thread
stack (top → down, abridged) is:

```
0  com.sun.org.apache.xerces…DOMParser.parse                                  ← empty XML parse; XMLParseException
1  com.sun.org.apache.xerces…DocumentBuilderImpl.parse
2  org.eclipse.wst.server.core.internal.ServerPreferenceModifyListener.removeLockedServerRuntimePreference  (56)
3  org.eclipse.wst.server.core.internal.ServerPreferenceModifyListener.preApply                             (44)
…
9  org.eclipse.core.internal.resources.ProjectPreferences.read                (251)
10 org.eclipse.core.internal.resources.ProjectPreferences.updatePreferences   (374)
11 org.eclipse.core.internal.resources.File.updateMetadataFiles               (592)
12 org.eclipse.core.internal.localstore.RefreshLocalVisitor.visit             (321)
…
17 org.eclipse.core.internal.resources.Project.open                           (1189)
19 com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter.importExistingAndOpenClosedProjects (492)  ← our p.open(null)
20 …checkAndExportSolutions (275)  →  WarWorkspaceExporter  →  start (196)
```

Exact chain, all verified at runtime:

1. The exporter opens each project in `importExistingAndOpenClosedProjects` (`p.open(null)`,
   line 492). `Project.open()` performs a local refresh that reads the project's `.settings`
   preferences (`ProjectPreferences.read`, `File.updateMetadataFiles`) and fires the
   preference-apply listeners (`PreferencesService.firePreApplyEvent`).
2. WST registers a `PreferenceModifyListener`. Its `preApply` calls
   `removeLockedServerRuntimePreference(prefs)`.
3. The decompiled WST body is:

   ```java
   private void removeLockedServerRuntimePreference(Preferences prefs) {
       File f = new File(prefs.get("runtimes", ""));   // key "runtimes", default ""
       if (!f.exists()) return;
       Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder()
               .parse(new InputSource(new StringReader(f.toString())));   // ← parses the PATH string
       // … iterate <runtime locked="true"> elements …
   }
   ```

   On a fresh workspace the `"runtimes"` preference is absent, so it defaults to `""`.
   WST then builds `new File("")` and — the actual WST bug — parses `f.toString()` (the
   **path string**, here `""`), **not** the file's contents. Parsing the empty string with
   a handler-less parser makes JAXP's default error handler print
   `[Fatal Error] :1:1: Premature end of file.` to stderr and throw. WST swallows it, so
   nothing reaches `.metadata/.log`. (`new File("").exists()` returning true is
   platform/JVM-dependent; on the reporter's and this machine's JVM it passed, so the parse
   ran.)

**Trigger:** the WST `runtimes` preference being empty/absent at project-open time on a
fresh workspace. It is **not** a committed repo file, which is why the reporter found no
empty XML in the tree. The per-project-during-`open()` ordering, and the fact that an
already-populated workspace is silent (the preference is then set), match this exactly.

**Why it regressed (not a Servoy source change):** the platform/runtime moved between the
2024.3 line and master — the target platform bumped Eclipse `releases/2023-12` →
`releases/2026-06` (so a newer `org.eclipse.wst.server.core`), and the bundled JRE moved to
a newer JDK (OpenJDK 25) whose default (handler-less) JAXP parser now routes the
empty-input fatal error to stderr. A direct `DocumentBuilder.parse("")` reproduction prints
the line on the bundled JRE. The surfaced line therefore comes from the upgraded
WST + JDK/JAXP stack, not from Servoy or DLTK code.

## Ticket premise check

The ticket proposes no solution, only a question, so there is no premise to overturn. Its
framing is sound: the error is harmless, it is a 2024.3→2026.3 regression, and it is tied
to the fresh-workspace import path. The reporter's own `workingsets.xml` suspicion is ruled
out — the live stack shows the parse happening per project during `open()`, inside WST's
server-runtime preference listener, not against `workingsets.xml`.

## Approaches considered

1. **Suppress only this line at the exporter boundary (chosen).** Wrap the
   import/`Project.open` section of `checkAndExportSolutions` so `System.err` is temporarily
   replaced by a filter that drops **only** a line containing both `[Fatal Error]` and
   `Premature end of file.`, restoring the original stream in a `finally`. Pros: targets the
   exact window and the exact noise; the parser belongs to WST (a third-party WTP bundle we
   do not fork), so suppression is the honest, lowest-risk fix; the WAR output is already
   correct; genuine errors are untouched. Cons: it is suppression, not a cure of the WST bug.

2. **Prevent the trigger** — seed/clear the WST `runtimes` preference before opening
   projects so the listener's `parse` is never reached. Pros: avoids the parse entirely.
   Cons: fragile — depends on WST-internal preference storage and on `new File("").exists()`
   behaviour across platforms; easy to get subtly wrong.

3. **Patch WST** (`ServerPreferenceModifyListener`) to guard the empty/invalid path and
   install an `ErrorHandler`. Pros: fixes the real defect. Cons: WST comes from the WTP
   target-platform p2 site, not a Servoy fork, so it is not ours to rebuild/ship like DLTK.

4. **No code change.** Pros: WAR is correct, message is cosmetic. Cons: the reporter has a
   legitimate CI-log-cleanliness complaint; closing with no action ignores the request.

## Recommendation

**PROCEED with approach 1 (boundary suppression), implemented in the exporter only.**

In `com.servoy.eclipse.exporter.solution`,
`AbstractWorkspaceExporter.checkAndExportSolutions`, wrap the project
import/open/refresh block: save `System.err`, set it to a `PrintStream` wrapper that
forwards everything **except** the single `[Fatal Error] … Premature end of file.` line, and
restore the original stream in a `finally`. The filter is line-buffered and matches on both
`[Fatal Error]` and `Premature end of file.` so no genuine error is hidden, and its scope is
limited to the import window.

This supersedes the earlier (reverted) recommendation to change DLTK `ScriptProject` and to
guard an empty `.buildpath` in the exporter — both targeted the wrong parser and have been
reverted.

## Git history findings

- `AbstractWorkspaceExporter.importExistingAndOpenClosedProjects`: the only functional
  change since the 2024.3 line is **`54db0e1a71` (SVY-20420)** — switched project creation to
  `workspace.loadProjectDescription(.project)` + `p.create(pd, …)`. It changes which name a
  project is created under; it does **not** add XML parsing and does not cause the
  `Premature end of file` line. It merely preserved the per-project `p.open(...)` that
  triggers WST's listener.
- **Platform/runtime regression window:** `launch_targets/com.servoy.eclipse.target.target`
  moved Eclipse `releases/2023-12` → `releases/2026-06` (newer `org.eclipse.wst.server.core`)
  and the bundled JRE moved to OpenJDK 25 (newer JAXP that prints the empty-input fatal error
  to stderr). The newly-surfaced line comes from this upgraded WST + JDK/JAXP stack during
  `Project.open()`, not from a Servoy or DLTK source change.
