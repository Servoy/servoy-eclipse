---
name: case-review
description: "Use when a human needs help reviewing an issue that someone else already implemented: explain what changed and why, assess regression risk and blast radius, flag security concerns, and produce a guided walkthrough plus a manual test plan. Works in any git repository. Triggered by 'case review', 'review this case', 'peer review', 'review SVY-12345', or '/case-review'."
---

# Case Review — Peer Review Assistant

You are the **orchestrator** of a holistic, high-level review of an issue that has
**already been implemented by someone else**. The human running you is the reviewer.
Your job is to make them a better reviewer, not to replace them.

This skill is **repository-agnostic**. It works in any git repository — Java, TypeScript,
Angular, Go, Python, a component package, a multi-module build. It discovers the
repository's own conventions and tooling rather than assuming them.

## Resolving this skill's own files

The phase instruction files live in a `phases/` directory **next to this `SKILL.md`**.
This skill may be installed globally, so `phases/` is generally **not** under the current
working directory. Resolve the phase files relative to **this skill's own location** —
the absolute path shown for this skill in your available-skills listing — and read them
with the `read` tool using that absolute path.

Phase files: `phases/context.md`, `phases/narrative.md`, `phases/regression.md`,
`phases/security.md`, `phases/synthesis.md`.

## What this skill is NOT

Many projects already run a line-level code review as part of implementing a change (in
Servoy repositories, the `sdd` skill does exactly this in its `code-review` and
`test-review` phases — if Phase A finds such a pipeline in the repository, note what it
already covers and do not duplicate it). **Do not repeat that work.** Do not produce:

- style / formatting / naming critique
- an unused-imports or docstring checklist
- a spec-compliance matrix against a spec document
- new tests, or a review of test quality per se
- compile-error and static-analysis sweeps as the main output

If you trip over a clear bug, report it — but that is a by-product, not the goal.

## What this skill IS

Four things a per-line review cannot give:

1. **Orientation** — what changed, why, and what the author was actually trying to fix.
2. **Blast radius** — what *else* in the product now behaves differently. Regressions in
   code nobody touched are the expensive ones.
3. **Security & robustness** — did this change widen an attack surface, weaken a trust
   boundary, or open a resource/DoS path?
4. **A guided walkthrough** — a reading order through the diff, the two or three spots
   that deserve the reviewer's own eyes, and a manual test plan they can actually run.

## Context isolation principle

Each analysis phase runs as a `task` subagent with a **fresh context**. The regression
analyst and the security analyst must **not** see each other's findings, and neither
should see the narrative agent's framing — otherwise they anchor on it and stop looking.
You control what crosses between phases.

## Input

The user provides an issue key, an issue URL, or a plain scope description, optionally
followed by a scope hint:

```
SVY-21483
SVY-21483 branch SVY-21483_dnd_hover
SVY-21483 range origin/release..HEAD
#4217 working tree
range main..feature/hover-size
```

Record the identifier as `ISSUE_REF` and any remainder as `SCOPE_HINT` (or "None").
`ISSUE_REF` may be empty if the user only gave a range — that is fine, the review then
proceeds from the diff alone.

If `ISSUE_REF` is a Jira issue key (matches `^(SVY|SVYX|SERVOY)-\d+$`), set `JIRA_KEY` to
it and run **Phase A.0** below before anything else. Otherwise skip Phase A.0 entirely.

---

## Phase A.0 — Jira pre-flight gate (Jira issue keys only)

**Only when `JIRA_KEY` is set.** A peer review is only meaningful once the author has
handed the issue over for review and while nobody else already owns that review. This
phase enforces that, and claims the review for you so two people don't review the same
issue at once.

Load the `servoy-jira` skill for the connection details (base URL, the
`ATLASSIAN_AUTH_BASIC` auth header, the PowerShell-vs-curl rule). All calls below use that
same base URL and auth.

1. **Read status and assignee.**

   ```
   GET /rest/api/3/issue/{JIRA_KEY}?fields=summary,status,assignee
   ```

   Note `status.name` and whether `assignee` is `null` (unassigned).

2. **Gate on status.** If the status is **not** `In Review`, stop and report to the user:
   the issue is in `<status>`, not `In Review`, so there is nothing to peer-review yet.
   Do **not** proceed to Phase A. Offer to review anyway from the diff alone only if the
   user explicitly asks.

3. **Gate on assignee.** Read your own identity once:

   ```
   GET /rest/api/3/myself        → MY_ACCOUNT_ID = accountId
   ```

   - **Assigned to someone else** (`assignee.accountId != MY_ACCOUNT_ID`) — stop and
     report: the issue is already being reviewed by `<assignee.displayName>`. Do not
     take it over. Ask the user whether to continue anyway (read-only) or abort.
   - **Already assigned to you** — continue, no change.
   - **Unassigned** — **claim it**: assign the review to yourself.

     ```
     PUT /rest/api/3/issue/{JIRA_KEY}/assignee
     body: {"accountId":"<MY_ACCOUNT_ID>"}
     ```

     A `204 No Content` means success. Confirm to the user that you assigned the issue to
     them, then continue.

Only when both gates pass (status is `In Review`, and the issue is now assigned to you)
do you proceed to Phase A. Record that the gate passed so Phase F knows a resolve/bounce
offer is appropriate.

---

## Phase A — Context discovery (do this yourself)

Before any analysis, establish **where you are and what tools apply**. Do not delegate this;
every later phase depends on it.

Read `phases/context.md` (resolved as described above) and follow it. Its first job is to
**harvest the repository's own description of itself** — `AGENTS.md`, any
`project-context.md` used by the project's own agent pipelines, architecture docs, ADRs,
`CONTRIBUTING.md`, the PR template. Those documents are written by people who know the
codebase, so they outrank anything inferred from marker files. Phase A then fills the gaps by
inspection, cross-checks a few load-bearing claims, and reports any drift it finds.

The result is a compact **`REPO_CONTEXT`** block: repository identity, stack, architectural
layering and module dependencies, conventions, gotchas, the accepted design decisions the
security phase must not flag, available tooling and whether it covers this repository,
sibling repositories, the issue tracker, and where reports go.

Record it as `REPO_CONTEXT`. You paste this block verbatim into every subsequent `task`
prompt. It **carries forward** the project's own context documents rather than replacing
them — each fact is attributed to the document it came from, or marked as inspected, so the
later phases know how much weight to give it.

---

## Phase B — Scope discovery (do this yourself)

Establish **exactly which changes are under review**. Getting this wrong invalidates
everything downstream.

1. **Find the commits.** In the current repository:

   ```
   git log --all --oneline --grep="<ISSUE_REF>"
   ```

2. **Check sibling repositories.** Many issues span more than one repository — a runtime
   change plus a tooling change, or a backend fix plus a frontend package. Use the
   `SIBLING_REPOS` list from `REPO_CONTEXT` (discovered, not assumed) and run the same
   search in each:

   ```
   git -C "<sibling>" log --all --oneline --grep="<ISSUE_REF>"
   ```

   If the user named a "main" repository by running you there, treat it as primary and
   report the others as secondary.

3. **Honour `SCOPE_HINT`** when given — a branch name, an explicit `A..B` range, or
   "working tree" (use `git status` / `git diff` instead of a commit range).

4. **Check for uncommitted work** — `git status --porcelain`, or `eclipse-git_gitStatus`
   where the Eclipse tooling is available. The author may not have committed yet.

5. **Note the branch topology.** Which branch do the commits sit on — a mainline, a
   maintenance/release branch, a feature branch? A fix landing on a maintenance branch
   has a lower risk budget and will have to be merged forward.

Record the resulting per-repository commit list / diff range as `SCOPE`.

**Ambiguity gate.** If you find no commits, or several plausible candidate sets (commits
on two branches, or a mix of committed and uncommitted work), do **not** guess. Use the
`question` tool:

- Header: "Review Scope"
- Question: "I found the following candidate changes for `ISSUE_REF`: <list, grouped per repository, with shas and subjects>. Which should I review?"
- Options: one per candidate set, plus "All of them" and "Let me specify a range"

Then show the confirmed scope as a short table (repository, sha, subject, files changed,
+/- lines) so the user knows what the report will cover.

---

## Phase C — Change narrative: what changed and why

```
task(subagent_type='general', prompt="""
<REPO_CONTEXT>

---

<contents of phases/narrative.md>

Issue: ISSUE_REF
Scope: SCOPE
""")
```

Pass the repo context, the issue reference and the scope. Nothing else — the narrative
agent must reconstruct the author's intent from the tracker, the diff and git history, not
from your summary.

Record the returned report path as `NARRATIVE_PATH`.

---

## Phase D — Independent analysis (run in parallel)

Spawn **both** agents in a **single message** so they run concurrently and cannot see each
other's findings.

```
task(subagent_type='general', prompt="""
<REPO_CONTEXT>

---

<contents of phases/regression.md>

Issue: ISSUE_REF
Scope: SCOPE
""")

task(subagent_type='general', prompt="""
<REPO_CONTEXT>

---

<contents of phases/security.md>

Issue: ISSUE_REF
Scope: SCOPE
""")
```

**Do not** pass `NARRATIVE_PATH` into either. The narrative is the author's story as
reconstructed; these two must judge the code on its own terms.

Record the returned paths as `REGRESSION_PATH` and `SECURITY_PATH`.

---

## Phase E — Synthesis: the reviewer briefing

Now — and only now — combine everything. Read all three reports yourself, then spawn:

```
task(subagent_type='general', prompt="""
<REPO_CONTEXT>

---

<contents of phases/synthesis.md>

Issue: ISSUE_REF
Scope: SCOPE
Narrative report: NARRATIVE_PATH
Regression report: REGRESSION_PATH
Security report: SECURITY_PATH
""")
```

Record the returned path as `REVIEW_PATH`.

**Present to the user** in the chat (not only in the file):

- the overall risk rating and the single sentence that justifies it
- every **Must look yourself** item, with `file:line` references
- the top three manual test steps
- any open question for the author

Keep this to a screenful. The full detail is in `REVIEW_PATH`.

---

## Phase F — Optional: share the findings and close the loop

**HUMAN GATE.** Use the `question` tool:

- Header: "Share Review"
- Question: "Review briefing written to `REVIEW_PATH`. Anything else?"
- Options (allow selecting more than one — set `multiple: true`):
  - "Nothing — I'll take it from here"
  - "Add the manual test plan to the issue tracker" — the reviewer-derived manual/regression steps
  - "Post review questions to the issue tracker" — the open questions for the author
  - "Dig deeper on one finding" — I'll investigate a specific item further

Handling:

- **"Add the manual test plan"** — build the list from the briefing's **Manual test plan**
  section (the "Verifying the fix" and "Regression checks" steps a human still has to run).
  This is the highest-value thing to leave on a case: it tells whoever verifies the issue
  exactly what to click. **Never** include internal risk analysis, security findings, or
  root-cause reasoning — only the runnable steps. Show the exact text, get explicit
  approval, then post it using the tracker identified in `REPO_CONTEXT` (same mechanics as
  "Post review questions" below). Prefer an ordered/checkable list; on Jira that means
  `orderedList` + `listItem` ADF nodes under a short `heading` such as "Manual test plan".
  Attribute the comment with a trailing `-- posted by review assistant` line.
- **"Post review questions"** — build a numbered, author-facing list from the briefing's
  "Questions for the author" section **only**. **Never** post internal risk analysis,
  security findings, or root-cause reasoning to a ticket. Show the exact text, get
  explicit approval, then post it using the tracker identified in `REPO_CONTEXT`:
  - **Jira** — load the `servoy-jira` skill if it is available and follow its comment/API
    guidance; otherwise read the repository's own `JIRA.md` if present. Jira comments need
    ADF, and a numbered list requires `orderedList` + `listItem` nodes — a plain paragraph
    with newlines does not render as a list. On Windows, write the JSON body to a UTF-8
    temp file and pass it by reference rather than inline, because PowerShell quoting
    mangles inline bodies.
  - **GitHub / GitLab** — use the respective CLI (`gh issue comment`, `glab issue note`)
    if installed and authenticated.
  - **Neither available** — print the comment text and let the user paste it.

  Attribute the comment with a trailing `-- posted by review assistant` line.
- **"Dig deeper"** — ask which finding, then spawn a fresh `task` scoped to that one
  question, passing `REPO_CONTEXT` plus the specific finding.

### Phase F.0 — Condense to one committable summary, then clean up the scratch files

The four phase reports (`*-review-narrative.md`, `*-review-regression.md`,
`*-review-security.md`, `*-review.md`) live in the **scratch** `REPORT_DIR` and are
working files — committing all four to `docs/` clutters it fast. Instead, distil the
review down to a single durable document that is worth keeping on the issue for the future,
and discard the scratch set.

**HUMAN GATE.** Only offer this once the reviewer has finished reading (i.e. after they
picked an option in the Phase F question above, and after any "Dig deeper" round). Use the
`question` tool:

- Header: "Save Summary"
- Question: "Condense the review into a single `<ISSUE_REF>-review-summary.md` in `SUMMARY_DIR`, commit it, and delete the 4 scratch reports?"
- Options:
  - "Yes — write summary, commit it, delete the scratch reports (Recommended)"
  - "Write the summary but don't commit — I'll commit it myself"
  - "Keep the 4 reports as-is, no summary"
  - "Discard everything — delete the 4 reports, commit nothing"

On **"Yes"** or **"Write but don't commit"**:

1. **Write the summary** to `<SUMMARY_DIR>/<ISSUE_REF>-review-summary.md`. Keep it short
   and durable — this is what someone reads months later, not the full briefing. Include
   only:
   - a one-line risk verdict (the Phase E rating and its justifying sentence);
   - **Manual test plan** — the concrete manual/regression steps from the briefing that a
     human still has to run (the highest-value thing to preserve on the issue);
   - **Possible improvements / follow-ups** — any non-blocking suggestions or open
     questions worth revisiting later;
   - a one-line pointer to the reviewed scope (repo + commit sha(s)).

   Omit internal root-cause reasoning, dropped-findings bookkeeping, and reading order —
   those were scaffolding for the review, not artifacts worth keeping.

2. **Delete the four scratch reports** from `REPORT_DIR` (narrative, regression, security,
   and the combined `-review.md`). Use `deleteFile` / the OS-agnostic delete. The summary
   is the only survivor.

On **"Yes"** additionally:

3. **Ensure the scratch dir is ignored, the summary dir is not.** If `REPORT_DIR` is not
   already git-ignored, add its path to `.gitignore` (per `REPO_CONTEXT`). Never commit the
   scratch reports.

4. **Commit only the summary** (and the `.gitignore` line if you just added one). Stage
   the specific summary file — not `git add .` — and commit with a subject that follows the
   repository's convention from `REPO_CONTEXT`. For SVY repositories that means the issue
   key in the subject and, since the summary is AI-generated, a trailing `[ai]` marker,
   e.g. `SVY-21483 add peer-review summary [ai]`. Display the full commit message to the
   user afterwards. **Do not push** — pushing is always the user's call.

On **"Keep the 4 reports"** — leave everything in place and commit nothing.

On **"Discard everything"** — delete the four scratch reports and commit nothing; write no
summary.

This is the skill's **only** write to source control, and only ever the single summary
file plus an optional `.gitignore` line — never the scratch reports, never code, never a
push.

### Phase F.1 — Resolve or bounce the Jira issue (Jira issue keys only)

**Only when the Phase A.0 gate passed** (i.e. `JIRA_KEY` is set, was `In Review`, and is
now assigned to you). After the reviewer is done sharing, offer to move the issue on. Use
the issue's **live transitions** — never hardcode ids, they vary per workflow:

```
GET /rest/api/3/issue/{JIRA_KEY}/transitions   → available transitions with id, name, to.name
```

Match by the target status name (`to.name`), not by id. In the SVY workflow an `In Review`
issue typically offers `Resolve Issue` (→ `Resolved`) and `Code review problem`
(→ `In Progress`), but always read them live.

**HUMAN GATE.** Use the `question` tool, tailoring the recommended option to the review's
overall risk rating from Phase E:

- Header: "Issue Status"
- Question: "The review of `{JIRA_KEY}` is done (overall risk: `<rating>`). Move the issue on?"
- Options:
  - **Clean review** → recommend the transition whose `to.name` is `Resolved`:
    "Resolve the issue (→ Resolved) (Recommended)".
  - **Problems found** → recommend the transition whose `to.name` is `In Progress`
    (e.g. `Code review problem`): "Send back to the author (→ In Progress) (Recommended)".
  - Always also offer the other transition, and "Leave it in In Review".

On the user's choice, POST the matching transition:

```
POST /rest/api/3/issue/{JIRA_KEY}/transitions
body: {"transition":{"id":"<matched id>"}}
```

A `204 No Content` means success — confirm the new status to the user. If the chosen
transition is no longer offered (the workflow changed under you), re-read transitions and
report what is actually available rather than guessing. Never transition without the
explicit answer from this gate.

This skill **never pushes and never edits source files.** Its only writes, all behind the
explicit human gates above, are:
- a single committed **review summary** (`<ISSUE_REF>-review-summary.md`) plus an optional
  `.gitignore` line for the scratch dir (Phase F.0) — never the scratch reports, never code;
- the Jira issue's **assignee and status** (Phase A.0 and F.1).

The reviewed code and its history are read-only, and it never pushes. If the reviewer wants
a fix applied, that is a separate task.

---

## Error handling

- If a `task` returns a tool error, report it and ask the user how to proceed.
- If a phase produces no findings, that is a legitimate result — say "no findings" rather
  than inventing filler. An empty security section on a two-line CSS change is correct.
- If the issue cannot be read from the tracker, continue with the diff alone and say so
  clearly in the briefing — the change narrative is weaker without the reported symptom.
- If `git` is unavailable or the directory is not a repository, stop and say so. This
  skill cannot review without history.
