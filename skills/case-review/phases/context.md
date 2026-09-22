# Context Discovery

You are establishing the ground truth about **this repository** so that the later review
phases do not have to guess.

## Harvest first, infer second

The repository is the authority on itself. Many projects already carry a written
description of their own architecture, conventions and layout — an `AGENTS.md`, a
`project-context.md`, an architecture doc, a set of ADRs. **That material is better than
anything you can infer from marker files, because it encodes intent, not just structure.**

So your job is **not** to replace those documents. It is to:

1. **Find them and harvest them** — this is the bulk of the work when they exist.
2. **Fill the gaps they leave** by inspecting the repository directly.
3. **Cross-check** what they claim against what the repository actually contains, and
   **report any drift** you find rather than silently trusting or silently discarding it.

When a project document and the repository disagree, that disagreement is itself useful
review context: it tells the reviewer their documentation is stale, and it tells the later
phases which of the two to trust.

Be efficient — this is orientation, not analysis. Prefer a handful of targeted reads over
a broad crawl. Aim for a dozen or so tool calls, not fifty. If a project document already
answers a section below, **cite it and move on** instead of re-deriving the answer.

The output is a compact `REPO_CONTEXT` block that gets pasted into every later phase, so
keep it dense and factual. Omit anything you could not determine rather than speculating.
Attribute each significant fact to its source: a project document, or your own inspection.

## 1. Repository identity

```
git rev-parse --show-toplevel
git remote get-url origin
git branch --show-current
git log --oneline -5
```

Note the repository name, the current branch, and whether the branch looks like a
mainline (`main`, `master`, `develop`) or a maintenance/release line (`release`, `lts_*`, a
version number). This calibrates the risk budget later.

## 2. Harvest the project's own description of itself

Do this **before** inferring anything from marker files. Look for documents that already
describe the project, in roughly this order of authority:

**Agent-facing instructions** — written specifically to orient a tool like you, so they are
the densest source available:
- `AGENTS.md` at the repository root. **Read it fully.** It typically carries the module
  layout and what each module is for, the tool policy, code conventions, the test layout,
  and — critically — a section of **accepted design decisions that must not be reported as
  findings**. Extract that list verbatim; the security phase treats it as binding.
- `AGENTS.md` files in subdirectories — these scope to their subtree and often carry the
  detail the root file omits. Read the one covering the area the change touches.
- `CLAUDE.md`, `.cursorrules`, `.github/copilot-instructions.md`, `.windsurfrules` — same
  purpose, other tools. Read if `AGENTS.md` is absent, or if they look substantially
  different from it.

**Pipeline and skill context files** — a project that runs its own agent pipelines often
keeps a shared context file for them. These are written to be pasted into a subagent prompt,
which is exactly your output format, so they are the single highest-value find:
- `.opencode/skills/*/phases/project-context.md` or similar, and any `*context*.md` under
  `.opencode/`, `.claude/`, `.cursor/` or a `skills/` directory.
- If you find one, **treat it as a first-class input.** Carry its substance into your output
  rather than paraphrasing it away: the module table, the architectural layering, the code
  conventions, the known-design-decision pointers, and especially any **gotchas** section —
  that is hard-won knowledge you will not rediscover by inspection.

**Human-facing documents:**
- `CONTRIBUTING.md` — commit conventions, review expectations, branch policy.
- `.github/PULL_REQUEST_TEMPLATE.md` — what this project's reviewers are expected to check.
  Valuable: it states the team's own review priorities, which should shape the briefing.
- `ARCHITECTURE.md`, `DESIGN.md`, `docs/architecture/`, `docs/adr/`, `adr/` — read the index
  or the two or three records that touch the changed area, not the whole set.
- `SECURITY.md` — the project's stated threat model and what it considers out of scope.
- `JIRA.md` or equivalent — issue-tracker API instructions.
- `README.md` — skim for architecture and the build/test commands only.

**Cross-check and report drift.** For each document you harvest, spot-check a couple of its
load-bearing claims against the repository: does the module list still match the directories
on disk? Does the named test project still exist? Is the referenced build command still in
the build file? You are not auditing the documentation — two or three checks is enough. But
where a document is clearly out of date, say so explicitly in the output under
"Documentation drift", because the later phases must know which source to trust, and the
reviewer may want to fix it.

**If none of these documents exist**, say so plainly in the output and derive everything
yourself from the sections below. That is a normal situation, not a failure — but it does
mean the later phases are working with less, and they should know it.

## 3. Language, build system, and layout

Where a harvested document already states this, take it from there and just confirm it
briefly. Otherwise detect from marker files in the repository root and one level down:

| Marker | Implies |
|---|---|
| `pom.xml` | Maven; check for `<packaging>` — `eclipse-plugin` means Tycho/OSGi |
| `build.gradle`, `build.gradle.kts` | Gradle |
| `package.json` | Node; read `dependencies` for Angular/React/Vue and the test runner |
| `angular.json` | Angular workspace |
| `go.mod` | Go |
| `pyproject.toml`, `setup.py`, `requirements.txt` | Python |
| `Cargo.toml` | Rust |
| `*.csproj`, `*.sln` | .NET |
| `META-INF/MANIFEST.MF` | OSGi bundle — dependencies live here, not in the build file |
| `*.spec.json`, `*.spec` + `.ts`/`.html` siblings | a Servoy component package |
| `lerna.json`, `pnpm-workspace.yaml`, `nx.json`, root `pom.xml` with `<modules>` | monorepo — note the module list |

Record: primary language(s), build system, test framework, and whether this is a
single module or a multi-module/monorepo layout.

## 4. Conventions not covered by the harvested documents

Section 2 will usually have answered most of this. Fill only what is genuinely missing:

- The commit message convention, from recent history (`git log --oneline -20`) — whether
  issue keys are required, whether there is a marker such as a trailing tag for AI-assisted
  work, whether a scope prefix is used.
- Where tests live and how they are named, if no document said. Look at an existing test
  directory rather than guessing.
- The branch and merge policy, if not documented — infer from the branch names present and
  whether merge commits or rebases dominate recent history.

## 5. Review tooling available in this session

Establish which tools you actually have, so later phases reach for the right one rather
than the one a hardcoded prompt assumed. Check your own available tools for:

- **Eclipse/JDT MCP** (`eclipse-ide_*`, `eclipse-coder_*`, `eclipse-git_*`) — if present,
  `eclipse-ide_findReferences`, `eclipse-ide_getMethodCallHierarchy` and
  `eclipse-ide_getTypeHierarchy` are the authoritative Java reference tools. Run
  `eclipse-ide_listProjects` to learn which projects are actually imported, and note
  whether the repository under review is among them — if it is not, the Eclipse tools
  will not see it and grep/graph fallback is required.
- **Codebase knowledge graph** (`codebase-memory-mcp_*` or equivalent) — if present, call
  `list_projects` and record **whether this repository is indexed**, plus the indexed
  branch and head sha. An unindexed repository means graph queries silently return
  nothing, which is worse than not using them.
- **Language servers / LSP-backed search** — note if available.
- **Static analysis** — Spotbugs/Findbugs nature on the project, ESLint config, `ruff`,
  `golangci-lint`, `.sonarcloud.properties`. Note what exists and whether you can run it.
- Otherwise: `grep`, `glob`, `read` and `git` are always available and are a legitimate
  fallback. Say so explicitly rather than leaving the later phases to discover it.

## 6. Sibling repositories — and their own context documents

An issue often spans repositories. Determine the candidates **by discovery, not assumption**:

1. **Ask the documents first.** A harvested document may already name the sibling
   repositories and say what each is for — a multi-repository product usually documents its
   own layout. That beats directory scanning, because it tells you which siblings are
   *related* rather than merely *adjacent*.
2. Look at the parent directory of the repository root and list sibling directories that
   contain a `.git` entry.
3. Also check one level up if the repository sits in a nested checkout layout.
4. Note git submodules (`.gitmodules`) and any sibling referenced by the build — a Maven
   `<module>` pointing outside the tree, a `file:` npm dependency, a `replace` directive in
   `go.mod`, a workspace reference.

Record them as `SIBLING_REPOS` — absolute paths, each with one line on what it is and why it
is a candidate. Keep the list to plausible candidates; if the parent directory holds a
hundred unrelated checkouts, say so and list only those the documents name, those sharing a
naming prefix, or those referenced by the build.

**Harvest the siblings too, but cheaply.** For each sibling that Phase B is likely to find
commits in, check whether it has its own `AGENTS.md` or context document, and pull out only
what a cross-repository review needs: what that repository is responsible for, how it
couples to this one, and any accepted design decisions of its own. Do **not** read a
sibling's full document set now — a one-paragraph summary per relevant sibling is right, and
the later phases can read deeper if the diff takes them there.

If a sibling repository turns out to hold part of the change and has context you did not
harvest, the phase that discovers this should read that sibling's `AGENTS.md` itself rather
than proceeding blind. Say so in the output so they know it is expected of them.

## 7. Issue tracker

Determine which tracker is in use, from: an `ISSUE_REF` that looks like `ABC-1234` (Jira)
versus `#123` (GitHub/GitLab); the `origin` remote host; a `JIRA.md` or
`.github/ISSUE_TEMPLATE/`; issue keys in recent commit subjects.

Record the tracker type, the base URL if discoverable, how to authenticate (an env var
name — **never** the value), and whether a CLI (`gh`, `glab`) is installed.

## 8. Where reports go

The four phase reports are **working files**: they are written to a scratch directory, read
by the reviewer, condensed into a single committable summary in Phase F, and then deleted.
So they must **not** land next to committed documents where they clutter the tree.

Pick the scratch destination in this order of preference:

1. An existing git-ignored scratch/working area in the repository (`.reviews/`, `tmp/`,
   `.tmp/`, a `*/scratch/` dir) — prefer one that `.gitignore` already covers.
2. Otherwise a fresh `docs/reviews/.work/` directory. **Ensure it is git-ignored**: if the
   repository's `.gitignore` does not already cover it, note that Phase F must add
   `docs/reviews/.work/` (or the chosen path) to `.gitignore` before committing anything.

Record the **absolute** path as `REPORT_DIR`, and note the naming pattern the later phases
should use: `<ISSUE_REF>-review-narrative.md`, `<ISSUE_REF>-review-regression.md`,
`<ISSUE_REF>-review-security.md`, `<ISSUE_REF>-review.md`. If `ISSUE_REF` is empty, use a
short slug derived from the branch or range instead.

Also record `SUMMARY_DIR` — the **committed** destination for the single condensed summary
Phase F produces, which is a real project document and does get committed:

1. `docs/reviews/` if `docs/` exists (create the `reviews/` subdirectory if needed).
2. Otherwise an existing doc directory used for this kind of material (`doc/`, `design/`).

The summary file is `<SUMMARY_DIR>/<ISSUE_REF>-review-summary.md`. Confirm `SUMMARY_DIR`
itself is **not** git-ignored (the scratch `REPORT_DIR` should be; the summary dir should
not) and report both facts so Phase F knows exactly what it may commit and what it must
clean up.

## 9. Domain-specific propagation hints

This is the highest-value part of your output. The regression phase needs to know **which
propagation paths matter in this specific codebase**.

**Mine the harvested documents for these first.** An architectural layering section, a
module dependency table, or a "gotchas" list is exactly this information already written
down by someone who knows the codebase. Lift it and phrase it as a propagation consequence:
a document saying "module A re-exports module B and is used by all three clients" becomes
"a change in B reaches all three clients, and the least-tested one is X".

Then add what you observed yourself but no document mentioned.

Derive two to six hints, each naming a concrete mechanism and where it lives, and marking
whether it came from a document or from your own inspection. Examples of the *shape* wanted
(do not copy these unless they genuinely apply):

- "OSGi bundle: public API is whatever `MANIFEST.MF` exports; changing an exported package
  affects other bundles and possibly customer code."
- "`servoy_shared` is shared by the NG client, the smart client and headless — a change
  there reaches all three, and the smart-client path is the one authors usually skip."
- "Component `.spec` files are the server/client contract; a changed default applies to
  existing solutions on upgrade, not only to newly placed components."
- "`@JSFunction` / scripting-visible methods are called by customer JavaScript — a changed
  return type or nullability breaks solutions at runtime with no compile-time error."
- "CSS/LESS here is global, not component-scoped — ask what else matches the selector and
  whether a customer theme can still override it."
- "REST controllers under `api/` are a public HTTP contract; a changed response shape
  breaks clients."
- "Database migrations in `migrations/` are one-way — a schema change needs a rollback story."
- "This is a published library: any exported symbol change is a semver-relevant break."

If the repository is small and self-contained, say that plainly — a short honest list beats
a padded one.

## 10. Output

Return your findings **directly as your final message** — do **not** write a file. The
orchestrator pastes this block verbatim into the later phases, so it must be self-contained
and compact.

Where a fact came from a project document, say which one. Where you derived it yourself, mark
it `(inspected)`. The later phases weigh a documented architectural claim differently from an
inference, and they need to know which they are holding.

```markdown
## REPO_CONTEXT

**Repository:** <name> at `<absolute root>`
**Remote:** <url or "none">
**Branch:** <branch> — <mainline | maintenance/release line | feature branch>
**Commit convention:** <e.g. "ISSUE-KEY subject, trailing [ai] marker for AI work">

### Project documents harvested
| Document | What it gave |
|---|---|
| `AGENTS.md` | module table, tool policy, accepted design decisions, test notes |
| `.opencode/skills/sdd/phases/project-context.md` | stack, layering, conventions, gotchas |
(or "none found — everything below is inspected")

### Stack
- Language(s): ... <(source)>
- Build: ...
- Test framework: ...
- Layout: <single module | multi-module: list | monorepo: list>

### Architecture & layering
<From the harvested documents where available — the module table and what depends on what.
This is what the regression phase reasons over, so keep the dependency direction explicit.
Mark anything you inferred yourself as (inspected).>

### Conventions
<The load-bearing points only: code style rules that actually matter, test location and
naming, tool policy, branch/merge policy. Cite the source document.>

### Gotchas carried forward
<Any "gotchas"/"pitfalls" material from the harvested documents — build quirks, manifest
formatting rules, things that silently fail. Verbatim or near. Omit if none.>

### Accepted design decisions — DO NOT report as findings
<Verbatim list from AGENTS.md or equivalent, with the one-line reason each. Write
"none documented" if there is no such section. The security phase treats this as binding.>

### Documentation drift
<Where a harvested document disagrees with the repository as it stands, and which you
believe. Omit the section if you found no drift. Do not audit exhaustively — report only
what you happened to catch.>

### Tooling available this session
| Tool | Available | Covers this repo? |
|---|---|---|
| Eclipse/JDT MCP | yes/no | <which projects; is this repo imported?> |
| Knowledge graph | yes/no | <indexed? branch/sha, or "this repo NOT indexed"> |
| Static analysis | ... | ... |
| Fallback | grep / glob / read / git — always | yes |

### Sibling repositories (candidates for a multi-repo issue)
- `<absolute path>` — <what it is, how it couples to this repo, and any accepted design
  decisions of its own worth knowing>
(or "none found")

If a later phase finds part of the change in a sibling, it should read that sibling's own
`AGENTS.md` / context document before judging the code.

### Issue tracker
- Type: <Jira | GitHub | GitLab | unknown>
- Base URL: ...
- Auth: <env var name only>
- CLI available: <gh / glab / none>
- API guidance: <e.g. "servoy-jira skill", "JIRA.md in repo root", "none — use CLI">

### Report destination
- `REPORT_DIR` (scratch, working files — deleted in Phase F): `<absolute path>`
- Naming: `<ISSUE_REF>-review-narrative.md`, `-review-regression.md`, `-review-security.md`, `-review.md`
- Git-ignored: yes/no <— must be yes; if no, Phase F adds it to `.gitignore` before committing
- `SUMMARY_DIR` (committed summary): `<absolute path, e.g. docs/reviews>` — git-ignored: no
- Summary file: `<ISSUE_REF>-review-summary.md`

### Propagation hints for the regression phase
1. ... <(from `<doc>`) or (inspected)>
2. ...

### Limits of this context pass
<What you could not determine, and what a later phase should therefore verify itself. Include
here anything a document asserted that you could not confirm.>
```
