---
name: assistai-sync
description: "Use when syncing the Servoy Copilot MCP bundle's ported files with upstream changes from the open-source AssistAI project (github.com/gradusnikov/eclipse-chatgpt-plugin). One-way only: upstream -> Servoy. Fetches new AssistAI commits since a recorded baseline, reviews each change touching a ported file with the user, and applies the agreed updates while preserving the deliberate port differences. Triggered by 'assistai sync', 'sync assistai', 'update ported files', 'check assistai upstream', or '/assistai-sync'. For SVY-21303."
---

# AssistAI Upstream Sync (one-way: upstream → Servoy)

You are the **orchestrator** for keeping the Servoy Copilot MCP bundle's *ported*
files in sync with the upstream open-source **AssistAI** project. The Servoy
`com.servoy.eclipse.developer.mcp` bundle was partly ported from AssistAI; that
upstream repo gets frequent commits, and this skill periodically pulls relevant
upstream changes into the Servoy ports.

**Direction is strictly one-way for now: upstream → Servoy.** The reverse
direction (turning Servoy-only fixes into pull requests on AssistAI) is **out of
scope** — do not open PRs on the upstream repo. This is the SVY-21303 "first,
just one direction" deliverable.

## Upstream

- **Repo:** `https://github.com/gradusnikov/eclipse-chatgpt-plugin` (AssistAI, MIT
  licensed — porting changes in is license-clean).
- **Default branch:** `main`.
- **Service source directory (upstream):**
  `plugins/com.github.gradusnikov.eclipse.plugin.assistai.main/src/com/github/gradusnikov/eclipse/assistai/mcp/services/`

## The ported files and their upstream counterparts

All ported files live in the Servoy Copilot repo under
`bundles/com.servoy.eclipse.developer.mcp/src/com/servoy/eclipse/developer/mcp/services/`.
The authoritative mapping and the recorded baseline SHA live in the **tracking
file** `docs/assistai-sync.md` in the Servoy Copilot repo — always read it first
and treat it as the source of truth (this skill only describes the process).

The current mapping (confirmed against upstream `main`):

| Servoy file | Upstream AssistAI class |
|---|---|
| `CodeEditingService.java` | `CodeEditingService.java` |
| `WorkspaceService.java` | `ResourceService.java` |
| `ProjectService.java` | `ProjectService.java` |
| `GitService.java` | `GitService.java` |
| `LocalHistoryService.java` | `LocalHistoryService.java` |
| `MarkdownService.java` | `MarkdownService.java` |
| `IdeStateService.java` | `ConsoleService.java` + `EditorService.java` (merged) |

> If `docs/assistai-sync.md` is missing in the Copilot repo, Phase 1 bootstraps
> it. If the upstream directory layout or a mapping has changed, update
> `docs/assistai-sync.md` AND the table above in the same run.

## The deliberate port differences (must survive every sync)

The Servoy ports intentionally dropped Java/JDT coupling and AssistAI's own
access layer. **When porting an upstream change in, re-apply these differences —
never reintroduce what was stripped:**

- **No JDT dependency** — no Java refactoring, no Java code formatter, no
  organize-imports, no Java-nature detection. AssistAI's Java-specific behaviour
  is replaced with generic Eclipse-resource behaviour.
- **No `AiIgnoreService` / `.aiignore` access checks** — access control is at the
  MCP Bearer-token layer in Servoy, so upstream `AiIgnoreService` calls are
  omitted.
- **No `UISynchronize` / editor refresh** — the Servoy MCP server runs headless,
  so upstream editor-UI refresh code is omitted.
- **Servoy additions kept** — e.g. `WorkspaceService.readProjectResource`
  populates `ServoyResourceCache` as a side-effect; preserve such Servoy-only
  additions.

Each ported file documents its own differences in its class Javadoc
(`Ported from AssistAI's {...}. Differences: ...`). Read that header before
editing and keep it accurate.

## Design principles

- **Review before apply.** Never apply an upstream change without showing the
  user the upstream diff and getting approval. Each upstream commit is triaged
  into one of: *port-forward* / *not-applicable* / *already-have*.
- **No silent reintroduction.** Dropping of JDT/AiIgnore/editor-UI code is
  deliberate — if an upstream change is entirely such code, classify it
  *not-applicable* and skip it (record why).
- **Prefer Eclipse MCP tools** for all Java edits (per the repo AGENTS.md):
  `eclipse-coder_*` to edit, then the compile loop with `eclipse-ide_*`.
- **Clone upstream to a temp dir** — never add it as a sibling checkout or a
  submodule. Use `/tmp/opencode/assistai-upstream`.
- **One commit, confirmed.** Only commit on explicit user approval; subject ends
  ` [ai]` and carries `SVY-21303`.

---

## Command form

| Command | Action |
|---------|--------|
| `/assistai-sync` | Run the full sync: fetch upstream, diff since baseline, review each change, apply approved changes, update baseline. |
| `/assistai-sync <sha>` | Same, but treat `<sha>` as the baseline (override the recorded one) — e.g. to re-review from an earlier point. |

`$ARGUMENTS` may be empty, or a single upstream commit SHA / ref to use as the
baseline for this run. It never changes direction.

---

## Phase 0 — Preflight

1. **Locate the Servoy Copilot repo.** This skill operates on the
   `Servoy-Copilot` checkout (the one holding
   `bundles/com.servoy.eclipse.developer.mcp`). You are normally started from
   inside it. Confirm the bundle directory exists; if not, ask the user for the
   Copilot repo path and stop until given.

2. **Verify `git`** is available. (No `gh` needed — this is read-only against the
   public upstream over HTTPS.)

3. **Clone or refresh the upstream** into a temp dir:
   ```bash
   UP=/tmp/opencode/assistai-upstream
   if [ -d "$UP/.git" ]; then
     git -C "$UP" fetch origin main --tags
   else
     mkdir -p /tmp/opencode
     git clone --filter=blob:none https://github.com/gradusnikov/eclipse-chatgpt-plugin "$UP"
   fi
   git -C "$UP" rev-parse origin/main   # newest upstream SHA (the sync target)
   ```
   If the clone/fetch fails (offline, network), stop and tell the user — do not
   proceed on stale data.

---

## Phase 1 — Baseline

The baseline is the **upstream** AssistAI commit we last synced from. It is
recorded in `docs/assistai-sync.md` (Copilot repo) on the
`Last synced upstream commit:` line.

1. **If `$ARGUMENTS` holds a SHA/ref**, use it as the baseline for this run
   (validate it resolves in the upstream clone with `git -C "$UP" rev-parse`).

2. **Else read `docs/assistai-sync.md`** and parse the recorded baseline SHA.

3. **Bootstrap (first run / no baseline recorded):** if the tracking file is
   missing or has no valid baseline SHA:
   - Create/complete `docs/assistai-sync.md` from the template in **Appendix A**.
   - **Adopt the current upstream HEAD (`origin/main`) as the baseline, apply
     nothing this run.** Record that SHA + today's date as the baseline. This is
     the agreed bootstrap behaviour (option (a) for SVY-21303): establish the
     marker now; future runs catch changes after it.
   - Report to the user that the baseline was established at `<sha>` and that no
     code changed, then **stop** (unless the user explicitly asked to also
     reconcile history — then continue with the baseline set to the port point
     they name).

4. **Validate** the baseline SHA resolves in the upstream clone. If it does not
   (e.g. force-push upstream), tell the user and ask whether to pick a new
   baseline rather than guessing.

---

## Phase 2 — Collect upstream changes since the baseline

Restrict everything to the mapped upstream files. Let
`SVCDIR=plugins/com.github.gradusnikov.eclipse.plugin.assistai.main/src/com/github/gradusnikov/eclipse/assistai/mcp/services`.

1. **List commits** touching any mapped upstream file since the baseline:
   ```bash
   git -C "$UP" log --oneline <baseline>..origin/main -- \
     "$SVCDIR/CodeEditingService.java" \
     "$SVCDIR/ResourceService.java" \
     "$SVCDIR/ProjectService.java" \
     "$SVCDIR/GitService.java" \
     "$SVCDIR/LocalHistoryService.java" \
     "$SVCDIR/MarkdownService.java" \
     "$SVCDIR/ConsoleService.java" \
     "$SVCDIR/EditorService.java"
   ```
   If empty → report "already up to date at `<baseline>`", update the baseline
   date, and stop.

2. For each commit, note its SHA, subject, and which mapped upstream file(s) it
   touches. Build a short table of the pending commits and present it to the user
   so they see the scope before any review.

3. Also note any mapped upstream file that was **renamed, split, or deleted**
   upstream since the baseline (via `git -C "$UP" log --follow --name-status`),
   because that changes the mapping and must be reflected in
   `docs/assistai-sync.md`.

---

## Phase 3 — Triage each change WITH the user

Go commit-by-commit (oldest first), or group several commits that touch the same
file if that reads more clearly. For each:

1. **Show the upstream diff** for the mapped file(s):
   ```bash
   git -C "$UP" show <sha> -- "$SVCDIR/<UpstreamClass>.java"
   ```

2. **Classify** it, and tell the user your reasoning:
   - **port-forward** — a real behaviour/bug fix or improvement in logic the
     Servoy port shares. Candidate to apply.
   - **not-applicable** — the change is entirely in stripped territory (JDT /
     refactoring / formatter / organize-imports / `AiIgnoreService` /
     `UISynchronize` / editor-UI), or in an upstream-only method the Servoy port
     does not have. Skip; record why.
   - **already-have** — the Servoy file already contains the equivalent fix
     (e.g. the SVY-21472 applyPatch hardening may already cover an upstream
     change). Skip; record why.

3. **Get the user's decision.** Only *port-forward* items the user approves are
   applied. When unsure whether something is applicable, ask rather than guess.

Keep a running ledger (SHA → file → decision → note) for the Phase 5 report and
for writing the tracking file.

---

## Phase 4 — Apply approved changes

For each approved *port-forward* change:

1. **Read the current Servoy file** (`eclipse-ide_readProjectResource` or
   `read`) and the upstream version/diff, and translate the upstream change into
   the Servoy file — **re-applying the port differences**: strip any JDT /
   `AiIgnoreService` / `UISynchronize` / editor-refresh code the upstream hunk
   carries, keep Servoy-only additions, and adapt types/names (`ResourceService`
   → `WorkspaceService`, the `ConsoleService`+`EditorService` merge →
   `IdeStateService`, etc.).

2. **Edit with Eclipse MCP tools** (`eclipse-coder_applyPatch` /
   `eclipse-coder_replaceString` / `eclipse-coder_applyTextEdits`). Keep the
   `Ported from AssistAI's {...}. Differences:` Javadoc header accurate if the
   set of differences changes.

3. **Compile loop (mandatory, per AGENTS.md).** After edits:
   - `eclipse-ide_getCompilationErrors` for the bundle.
   - If errors, review quick fixes and apply safe ones with
     `eclipse-ide_executeQuickFix`; re-check until clean.
   - Also address the two highest-severity SpotBugs levels in touched code.

4. If a ported file has a corresponding test
   (`tests/com.servoy.eclipse.developer.mcp.tests` or
   `com.servoy.eclipse.opencode.tests`), run the relevant unit test for the
   touched service to confirm the port still behaves (see the repo AGENTS.md for
   how to run tests). Report results.

---

## Phase 5 — Record the new baseline & report

1. **Update `docs/assistai-sync.md`** (Copilot repo):
   - Set `Last synced upstream commit:` to the **newest upstream commit you
     reviewed** this run (normally `origin/main`'s SHA, even for commits you
     classified not-applicable/already-have — they have been reviewed, so the
     baseline advances past them). Record the date.
   - Append a dated **sync log** entry listing each reviewed commit with its
     decision (applied / not-applicable / already-have) and a one-line note.
   - If any mapping changed (rename/split/delete upstream), update the mapping
     table in the tracking file (and this SKILL.md's table) to match.

2. **Report to the user**: what was applied, what was skipped (with reasons), the
   old → new baseline SHA, and the compile/test result.

3. **Commit only on explicit approval.** Show the staged files and the proposed
   message first (per AGENTS.md pre-commit checklist: zero compilation errors,
   show-then-confirm). Suggested subject:
   ```
   SVY-21303 sync ported MCP services with AssistAI upstream <shortSha> [ai]
   ```
   Stage the touched ported file(s) **and** `docs/assistai-sync.md` together. Do
   not push unless asked.

---

## Appendix A — `docs/assistai-sync.md` template

Written into the **Servoy Copilot** repo (not the skill dir). Create it on first
run if missing:

```markdown
# AssistAI upstream sync (SVY-21303)

One-way sync (upstream → Servoy) of the ported MCP service files against the
open-source AssistAI project. Maintained by the `assistai-sync` skill / the
`/assistai-sync` command. See that skill for the process.

- **Upstream:** https://github.com/gradusnikov/eclipse-chatgpt-plugin (branch `main`, MIT)
- **Upstream service dir:** `plugins/com.github.gradusnikov.eclipse.plugin.assistai.main/src/com/github/gradusnikov/eclipse/assistai/mcp/services/`
- **Servoy service dir:** `bundles/com.servoy.eclipse.developer.mcp/src/com/servoy/eclipse/developer/mcp/services/`

## Last synced upstream commit: <SHA> (<YYYY-MM-DD>)

## File mapping

| Servoy file | Upstream AssistAI class |
|---|---|
| CodeEditingService.java | CodeEditingService.java |
| WorkspaceService.java | ResourceService.java |
| ProjectService.java | ProjectService.java |
| GitService.java | GitService.java |
| LocalHistoryService.java | LocalHistoryService.java |
| MarkdownService.java | MarkdownService.java |
| IdeStateService.java | ConsoleService.java + EditorService.java (merged) |

## Deliberate port differences (never reintroduce)

- No JDT dependency (no Java refactoring / formatter / organize-imports / Java-nature detection).
- No AiIgnoreService / .aiignore checks (access control is the MCP Bearer token).
- No UISynchronize / editor refresh (the Servoy MCP server is headless).
- Keep Servoy-only additions (e.g. WorkspaceService.readProjectResource populates ServoyResourceCache).

## Sync log

- <YYYY-MM-DD> — baseline established at <SHA>; no code changes (bootstrap).
```

---

## Versioning & future updates

This skill is meant to be updated as things change:

- **Mapping drift** — if upstream renames/splits/deletes a mapped class, update
  the mapping table here and in `docs/assistai-sync.md`.
- **New ports** — if a new file is ported from AssistAI, add it to the mapping
  (grep for `Ported from AssistAI` in the Copilot bundle to find all ports).
- **Second direction** — when SVY-21303's reverse direction (Servoy → upstream
  PRs) is scoped, add it as a separate phase; it is intentionally absent now.
