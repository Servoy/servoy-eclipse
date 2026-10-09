---
name: assistai-contribute
description: "Use when contributing Servoy-side fixes to the ported MCP service files BACK to the upstream open-source AssistAI project (github.com/gradusnikov/eclipse-chatgpt-plugin) as draft pull requests. One-way only: Servoy -> upstream. Selects Servoy fixes since a contribute baseline, re-ports each to the Java/JDT upstream (re-adding what the Servoy port stripped), builds and tests upstream, and opens a DRAFT PR from the Servoy fork. The reverse of assistai-sync. Triggered by 'assistai contribute', 'contribute to assistai', 'upstream our fixes', 'open assistai PR', or '/assistai-contribute'. For SVY-21303."
---

# AssistAI Contribute (one-way: Servoy → upstream, draft PRs)

You are the **orchestrator** for contributing Servoy-side fixes to the ported MCP
service files **back to the upstream AssistAI project** as **draft pull
requests**. This is the reverse of the `assistai-sync` skill.

**Direction is strictly one-way: Servoy → upstream.** This skill never pulls
upstream changes in (that is `assistai-sync`). It only proposes Servoy fixes
upstream, and only as **draft** PRs — a human reviews and marks ready.

**The hard requirement:** AssistAI is designed for **Java projects and routes
everything through JDT**. The Servoy ports deliberately stripped that. So a
Servoy fix is **not** a reverse patch — it must be **re-ported into the Java
world**: re-add the JDT / `AiIgnoreService` / `UISynchronize` scaffolding the
Servoy port removed, drop Servoy-only additions, and then **it must build and
pass tests on the upstream side** before any PR is opened. This skill enforces
that with a Maven build gate.

## Upstream & fork

- **Upstream (PR target):** `https://github.com/gradusnikov/eclipse-chatgpt-plugin` (AssistAI, MIT), branch `main`.
- **Servoy fork (push branches here):** `https://github.com/Servoy/eclipse-chatgpt-plugin`.
- **Service source directory:** `plugins/com.github.gradusnikov.eclipse.plugin.assistai.main/src/com/github/gradusnikov/eclipse/assistai/mcp/services/`
- **Build:** Tycho 5.0.2 reactor. Confirmed to build in this environment with
  `mvn clean verify -Dtycho.localArtifacts=ignore` (needs network for p2 target
  resolution, ~1–2 min). Do NOT use `-o` for a fresh target resolution.

## Mapping & shared state

The file↔class mapping and the deliberate port differences live in
`docs/assistai-sync.md` (Servoy Copilot repo) — read it first; it is the source
of truth shared with `assistai-sync`. The mapping (Servoy file → upstream class):

| Servoy file | Upstream AssistAI class |
|---|---|
| `CodeEditingService.java` | `CodeEditingService.java` |
| `WorkspaceService.java` | `ResourceService.java` |
| `ProjectService.java` | `ProjectService.java` |
| `GitService.java` | `GitService.java` |
| `LocalHistoryService.java` | `LocalHistoryService.java` |
| `MarkdownService.java` | `MarkdownService.java` |
| `IdeStateService.java` | `ConsoleService.java` + `EditorService.java` (merged) |

> Contributing a change back means doing the mapping **in reverse** and
> **re-adding** what the Servoy port stripped:
> - **Re-add JDT** behaviour where upstream has it (editor-buffer reads,
>   compilation-unit consistency, Java-nature detection, refactoring/formatter
>   where relevant).
> - **Re-add `AiIgnoreService` / `.aiignore`** access checks on file access.
> - **Re-add `UISynchronize` / editor refresh**.
> - **Drop Servoy-only additions** (e.g. the `ServoyResourceCache` population in
>   `WorkspaceService.readProjectResource`).

## Design principles

- **Draft-first, human-gated.** Always open PRs as **draft**. Never mark ready,
  never merge. A human reviews the Java correctness.
- **Build/test gate is non-negotiable.** No PR is opened unless the upstream
  reactor builds and tests pass with the change applied (and the pristine clone
  built green first, so a failure is attributable to our change).
- **Strict triage.** Only *general* fixes belong upstream. Servoy-specific
  behaviour (anything touching Servoy types, `ServoyResourceCache`, Servoy
  solution model, etc.) is NOT contributed.
- **No echo loop.** Never contribute back a change that `assistai-sync` pulled
  *in* from upstream — it is already upstream. Exclude forward-sync pull-ins.
- **Idempotent.** Never re-propose a fix already in the contributed ledger.
- **Confirm before anything remote.** Pushing a branch to the fork and opening a
  PR both require explicit user confirmation.

---

## Command form

| Command | Action |
|---------|--------|
| `/assistai-contribute` | Full run: select Servoy fixes since the contribute baseline, triage, re-port to Java, build-gate, open draft PR(s), update ledger + baseline. |
| `/assistai-contribute <sha>` | Same, but treat `<sha>` as the contribute baseline (override the recorded one). |

`$ARGUMENTS` may be empty, or a single Servoy commit SHA to use as the baseline.
It never changes direction.

---

## Phase 0 — Preflight

1. **Locate the Servoy Copilot repo** (holds `bundles/com.servoy.eclipse.developer.mcp`).
   If not found, ask the user and stop.

2. **Verify tooling:**
   - `git` and `mvn` (`mvn -version`) and a JDK — all required for the build gate.
   - `gh --version` and `gh auth status` — must be authenticated. If not
     authenticated, stop and tell the user to `gh auth login`.

   **Probe push access to the fork early** — the skill does not hardcode any user;
   whoever is authenticated must have write access to `Servoy/eclipse-chatgpt-plugin`
   or the Phase 5 push / PR will fail late. Check it now and stop early if not:
   ```bash
   gh repo view Servoy/eclipse-chatgpt-plugin --json viewerPermission -q .viewerPermission
   ```
   The result must be `WRITE`, `MAINTAIN`, or `ADMIN`. If it is `READ`, `NONE`,
   empty, or the call errors (no access / repo moved), **stop** and tell the user
   the authenticated account (`gh api user -q .login`) lacks push access to the
   fork — they need org write access or to authenticate as an account that has it.
   Do not proceed to clone/build/PR without confirmed push access.

3. **Clone/refresh the fork** into a temp dir and wire the upstream remote:
   ```bash
   FK=/tmp/opencode/assistai-fork
   if [ -d "$FK/.git" ]; then
     git -C "$FK" fetch origin main
   else
     mkdir -p /tmp/opencode
     git clone https://github.com/Servoy/eclipse-chatgpt-plugin "$FK"
   fi
   git -C "$FK" remote get-url upstream >/dev/null 2>&1 \
     || git -C "$FK" remote add upstream https://github.com/gradusnikov/eclipse-chatgpt-plugin
   git -C "$FK" fetch upstream main
   # keep fork main current with upstream before branching
   git -C "$FK" checkout main && git -C "$FK" merge --ff-only upstream/main || true
   ```

4. **Baseline-green build (gate sanity).** Before any edit, confirm the pristine
   clone builds, so a later failure is attributable to our change:
   ```bash
   ( cd "$FK" && mvn -B clean verify -Dtycho.localArtifacts=ignore )
   ```
   If the pristine build fails (upstream broken, or no network for p2), **stop**
   and report — the gate can't function, so do not open PRs. (Fallback only if
   the user explicitly accepts it: open draft PRs marked clearly "UNVERIFIED —
   build gate unavailable", for a human to build before marking ready.)

---

## Phase 1 — Contribute baseline

The contribute baseline is the **Servoy** commit from which we look for
contributable fixes. It is recorded in `docs/assistai-sync.md` on the
`Last contribute baseline:` line.

1. **If `$ARGUMENTS` holds a SHA**, use it (validate it resolves in the Copilot repo).
2. **Else read `docs/assistai-sync.md`** and parse the recorded contribute baseline.
3. **Bootstrap (first run / none recorded):** adopt the current Copilot `master`
   HEAD as the baseline, record it + today's date, and **stop with no PRs** —
   future runs contribute fixes made after it. (Same bootstrap pattern as the
   forward skill; the agreed decision for SVY-21303.)

---

## Phase 2 — Select candidate Servoy fixes

Let `SVCDIR_SVY=bundles/com.servoy.eclipse.developer.mcp/src/com/servoy/eclipse/developer/mcp/services`.

1. **List Servoy commits** since the baseline touching any ported file:
   ```bash
   git -C <copilot> log --oneline <baseline>..HEAD -- \
     "$SVCDIR_SVY/CodeEditingService.java" \
     "$SVCDIR_SVY/WorkspaceService.java" \
     "$SVCDIR_SVY/ProjectService.java" \
     "$SVCDIR_SVY/GitService.java" \
     "$SVCDIR_SVY/LocalHistoryService.java" \
     "$SVCDIR_SVY/MarkdownService.java" \
     "$SVCDIR_SVY/IdeStateService.java"
   ```
2. **Exclude** commits that came *from* upstream: cross-check against the
   `assistai-sync` **sync log** in `docs/assistai-sync.md` (anything applied by a
   forward sync is already upstream) and against the **contributed ledger**
   (anything already proposed). What remains are Servoy-origin fixes.
3. Present the surviving candidates to the user as a short table (SHA, subject,
   file(s)) before triage.

---

## Phase 3 — Triage each candidate WITH the user (strict)

For each candidate, show the Servoy diff and classify, telling the user your
reasoning:

- **general — contribute** — a fix in logic the upstream Java version shares and
  would benefit from (e.g. the SVY-21472 `applyPatch` hardening: diff-engine
  robustness that is not Servoy-specific). Candidate for a PR.
- **servoy-specific — do not contribute** — touches Servoy types, the solution
  model, `ServoyResourceCache`, or behaviour that only makes sense in Servoy.
  Skip; record why.
- **already upstream** — came from a forward sync or already proposed. Skip.

Only *general* fixes the user approves proceed. When unsure, ask.

---

## Phase 4 — Re-port each approved fix to Java + build gate

Per approved fix (one branch + one PR per logical fix, named e.g.
`contrib/SVY-xxxxx-<slug>`):

1. **Create a branch in the fork** off `main`:
   ```bash
   git -C "$FK" checkout -b contrib/SVY-xxxxx-<slug> main
   ```
2. **Re-port the change into the upstream Java file(s)** — reverse the mapping and
   **re-add** JDT / `AiIgnoreService` / `UISynchronize`/editor-refresh scaffolding,
   drop Servoy-only bits, adapt names (`WorkspaceService` → `ResourceService`,
   `IdeStateService` → `ConsoleService`/`EditorService`, etc.). Edit the files in
   the fork clone with the generic `read`/`edit`/`write` tools (the Eclipse MCP
   tools target the Servoy workspace, not this clone).
3. **Build/test gate — mandatory:**
   ```bash
   ( cd "$FK" && mvn -B clean verify -Dtycho.localArtifacts=ignore )
   ```
   - **Pass** → proceed to the PR.
   - **Fail** → do **not** open a PR for this fix. Report the failure, fix the
     re-port if it's a translation mistake and re-run, or abandon the candidate
     (record why). Never open a PR on a red build.

---

## Phase 5 — Draft PR + ledger

Per fix that passed the gate, **with user confirmation before each remote step**:

1. **Commit** in the fork branch (upstream style, credit the Servoy case):
   ```
   SVY-xxxxx <concise description of the general fix>
   ```
   (Follow upstream's own commit conventions, not Servoy's `[ai]` suffix — this
   lands in their history.)
2. **Push the branch to the fork** (`origin`):
   ```bash
   git -C "$FK" push -u origin contrib/SVY-xxxxx-<slug>
   ```
3. **Open a DRAFT PR** against upstream `main`:
   ```bash
   gh pr create --repo gradusnikov/eclipse-chatgpt-plugin \
     --base main --head Servoy:contrib/SVY-xxxxx-<slug> \
     --draft \
     --title "<concise title>" \
     --body "<what the fix does, why it's general, that it was first made in Servoy's port (SVY-xxxxx), and that upstream mvn verify passes>"
   ```
   Draft only. Never `--fill` from a Servoy `[ai]` message. Never mark ready.
4. **Record in the contributed ledger** in `docs/assistai-sync.md`: Servoy SHA →
   PR URL → status (`draft`).

---

## Phase 6 — Record baseline & report

1. **Update `docs/assistai-sync.md`** (Copilot repo):
   - Advance `Last contribute baseline:` to the Copilot HEAD reviewed this run,
     with the date.
   - Append a dated **contribute log** entry per candidate: decision
     (contributed / servoy-specific / already-upstream), and the PR URL for
     contributed ones.
2. **Report**: which fixes were contributed (with PR URLs), which were skipped
   (reasons), the old → new baseline, and the build-gate result for each.
3. **Commit the tracking-file change in the Copilot repo** only on explicit user
   approval (show staged files + message first; zero compilation errors in the
   bundle). Subject:
   ```
   SVY-21303 record AssistAI upstream contributions [ai]
   ```
   Do not push the Copilot commit unless asked. (The fork pushes in Phase 5 are
   separate and already confirmed there.)

---

## Relationship to `assistai-sync`

- `assistai-sync` = upstream → Servoy (pull in). `assistai-contribute` = Servoy →
  upstream (push out, draft PR). They share `docs/assistai-sync.md`.
- A change must never travel both ways: forward-sync pull-ins are excluded from
  contribute candidates (Phase 2), and contributed fixes, once merged upstream,
  will simply be *already-have* on the next forward sync.

## Versioning & future updates

- **Fork/upstream URLs** — update Phase 0/5 if the fork moves.
- **Build command** — the gate is `mvn verify`; update if upstream's build
  changes (Tycho version, target definition, profiles).
- **Mapping drift** — keep the mapping in sync with `docs/assistai-sync.md` and
  the `assistai-sync` skill.
