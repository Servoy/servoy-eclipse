---
name: sdd-java-eclipse
description: "Spec-Driven Development pipeline for Servoy Java / Eclipse-OSGi repositories (Tycho plugins and plain-Java-OSGi libs): Jira issue → triage → spec → implementation → code review → test generation → test review → commit. Triggered by the per-repo /sdd command, 'sdd', 'spec driven development', or a Jira issue key like SVY-12345 in a Java repo."
---

# SDD — Spec-Driven Development Pipeline (Java / Eclipse-OSGi)

You are the **orchestrator** for the full SDD pipeline:
Triage → PM Agent → Coding → Code Review → Test Gen → Test Review → Commit.

You collect output from each phase, show summaries to the user at approval gates,
and thread context forward **selectively** to maintain isolation between phases.

This skill is the shared, repository-agnostic **Java / Eclipse-OSGi** variant. It is
installed globally and invoked in any Servoy Java repo (Tycho eclipse-plugin projects
or plain-Java-OSGi bundles). Everything project-specific comes from the repo's own
`project-context.md` (see below) — the phase logic here never hardcodes one repo.

## Resolving this skill's own files

The phase instruction files live in a `phases/` directory **next to this `SKILL.md`**.
This skill is installed globally, so `phases/` is generally **NOT** under the current
working directory. Resolve the phase files relative to **this skill's own location** —
the absolute path shown for this skill in your available-skills listing — and read them
with the `read` tool using that absolute path.

Phase files: `phases/triage.md`, `phases/pm-agent.md`, `phases/coding.md`,
`phases/code-review.md`, `phases/test-gen.md`, `phases/test-review.md`.

## Repo-local project context (REQUIRED)

Every repo that uses this skill commits a `project-context.md` describing its own stack,
module layout, conventions, and gotchas. Its canonical location is:

```
.opencode/sdd/project-context.md   (relative to the repo root / current working directory)
```

The per-repo `/sdd` command normally inlines this file's contents into the prompt that
loaded this skill. **If you were given the project-context inline, use that.** Otherwise,
read `.opencode/sdd/project-context.md` from the current working directory now. If it is
missing, tell the user the repo has not been onboarded to SDD (it needs
`.opencode/sdd/project-context.md`) and ask whether to proceed with generic assumptions.

Record the full project-context text as `PROJECT_CONTEXT`. You MUST pass `PROJECT_CONTEXT`
into every phase subagent prompt (Triage, PM, Coding, Code Review, Test Gen, Test Review),
because those subagents start with a fresh context and cannot see the repo otherwise.

## Context isolation principle

Each phase runs as a `subagent` (the built-in `general` agent) with a **fresh context**.
This prevents bias:
- The Triage agent evaluates the problem free of any spec-writing incentive
- The Coder only sees the spec + project context, not the PM's internal analysis
- The Code Reviewer only sees the spec + actual code, not the Coder's reasoning
- The Test Generator only sees the spec + implementation, not review findings

You control exactly what information flows between phases via the subagent prompt.

**Spawning a phase:** use the `subagent` tool with `agent: general`. The built-in
`general` agent has shell, edit/write, and the Eclipse MCP tools (eclipse-coder,
eclipse-ide, eclipse-git, eclipse-pde, eclipse-runner) — everything the Java phases need.
Do NOT use the read-only `explore` agent for any phase that must run shell commands
(Jira API, git) or write files (triage report, spec, code, tests).

## Input

The user provides a Jira issue key or URL, optionally followed by extra context, e.g.:
`SVY-21080 some text meant to give more context about the case`

Parse the first token as the issue key/URL. Everything after it is supplementary
context provided by the user to clarify or augment the Jira ticket.

Record the issue key as `ISSUE_KEY` and the extra text (if any) as `USER_CONTEXT`.

---

## Phase 0 — Triage & Root-Cause Investigation

Before writing any spec, run an isolated triage agent whose only mandate is to find
the *actual* root cause and decide whether — and how — the problem should be addressed.
This prevents the pipeline from taking the ticket's proposed solution at face value.

Read `phases/triage.md` (resolve it relative to this skill's own location) and pass its
full content as instructions in a `subagent` prompt:

```
subagent(agent='general', prompt="""
<contents of phases/triage.md>

--- PROJECT CONTEXT ---
<PROJECT_CONTEXT>

Issue: ISSUE_KEY
User context: USER_CONTEXT (or "None" if the user provided no extra text)
""")
```

**Important:** Pass ONLY the issue key + user context + project context. The triage agent
must investigate the codebase and git history itself and form an independent judgement —
do not hand it a proposed solution.

The task's output will be the relative path to the triage report it created. Record that
as `TRIAGE_PATH`. Read the report to obtain its **verdict** (`PROCEED`, `NO_ACTION`, or
`NEEDS_INPUT`) and recommendation.

**HUMAN GATE — Triage decision**

Display a short summary of the triage report (verdict + recommendation), then present
the appropriate gate based on the verdict.

**The AI recommends, the human decides.** Never auto-stop on `NO_ACTION` — always route
through a gate and let the user confirm.

### Gate for `PROCEED` verdict

Use the `question` tool:
- Header: "Triage Decision"
- Question: "Phase 0 complete. Triage report at `TRIAGE_PATH` (verdict: PROCEED). Please review it, then choose how to proceed:"
- Options:
  - "Proceed to spec" — accept the recommended approach and run the PM Agent
  - "No action — stop pipeline" — end the pipeline; nothing further is generated
  - "Redirect approach" — provide a different direction; I'll run the PM Agent with that

Handle the choice:
- **"Proceed to spec"** — record the recommended approach from the report as
  `APPROVED_APPROACH` and continue to Phase 1.
- **"No action — stop pipeline"** — inform the user the pipeline has ended with no changes
  and stop. Do not run any further phase.
- **"Redirect approach"** — record the user's direction as `APPROVED_APPROACH` and continue
  to Phase 1.

### Gate for `NO_ACTION` verdict

Use the `question` tool:
- Header: "Triage Decision"
- Question: "Phase 0 complete. Triage report at `TRIAGE_PATH` (verdict: NO_ACTION — triage recommends no code change). Please review it, then choose how to proceed:"
- Options:
  - "No action — stop pipeline" — agree with triage; nothing further is generated
  - "Redirect approach" — override triage and provide a direction for the spec

Handle the choice:
- **"No action — stop pipeline"** — inform the user the pipeline has ended with no changes
  and stop. Do not run any further phase.
- **"Redirect approach"** — record the user's direction as `APPROVED_APPROACH` and continue
  to Phase 1.

### Gate for `NEEDS_INPUT` verdict

The report contains a "Questions for the reporter" section with the specific information
needed. Present these to the user via the `question` tool:

- Header: "Missing Information"
- Question: "Triage needs the following information before a spec can be written:\n\n<numbered list from the report's 'Questions for the reporter' section>\n\nHow would you like to proceed?"
- Options:
  - "Answer here" — I'll provide the answers now
  - "Post questions to Jira" — post these questions as a comment on the case
  - "Stop pipeline" — end here

Handle the choice:
- **"Answer here"** — ask the user for answers, record their answers as `USER_CONTEXT`
  additions, then present the **post-answer gate** below. The answers feed **forward** into
  the PM Agent — do **not** loop back into Triage.
- **"Post questions to Jira"** — use the `servoy-jira` skill (or the repo's `JIRA.md`) to
  post a clean, numbered, reporter-facing comment. Always show the exact comment text and
  get explicit confirmation before posting. Never post internal triage reasoning — only the
  reporter-facing questions. After posting, tell the user the pipeline is paused pending a
  reply on the ticket.
- **"Stop pipeline"** — inform the user the pipeline has ended and stop.

### Post-answer gate (after `NEEDS_INPUT` — "Answer here")

- Header: "Triage Decision"
- Question: "You've provided the information triage was missing. How would you like to proceed?"
- Options:
  - "Proceed with my answers" — use the answers as context and run the PM Agent
  - "No action — stop pipeline" — end the pipeline; nothing further is generated
  - "Redirect approach" — provide a different direction; I'll run the PM Agent with that

Handle as in the PROCEED gate, combining the triage recommendation (if any) with the
user's answers as `APPROVED_APPROACH`.

---

## Phase 1 — PM Agent: Jira → Spec

Read `phases/pm-agent.md` and spawn:

```
subagent(agent='general', prompt="""
<contents of phases/pm-agent.md>

--- PROJECT CONTEXT ---
<PROJECT_CONTEXT>

Issue: ISSUE_KEY
User context: USER_CONTEXT (or "None" if the user provided no extra text)
Triage report: TRIAGE_PATH
Approved approach: APPROVED_APPROACH
""")
```

**Important:** Pass the triage report path and the human-approved approach. These are
**authoritative** — the PM Agent must bound the spec to the approved approach rather than
the raw ticket, and reuse the triage's root-cause findings instead of re-investigating
from scratch.

The task's output will be the relative path to the spec file it created.
Record that as `SPEC_PATH`.

**HUMAN GATE — Spec approval**

Use the `question` tool:
- Header: "Spec Review"
- Question: "Phase 1 complete. Spec written at `SPEC_PATH`. Please review it, then choose:"
- Options:
  - "Approve" — proceed to implementation
  - "Request changes" — provide feedback and I'll revise

If the user requests changes, apply edits yourself for minor revisions. For substantial
rewrites, spawn a `subagent(agent='general')` with the feedback. Loop until approved.

---

## Phase 2 — Coding: Spec → Implementation

Read `phases/coding.md` and spawn:

```
subagent(agent='general', prompt="""
<contents of phases/coding.md>

--- PROJECT CONTEXT ---
<PROJECT_CONTEXT>

Spec file to implement: SPEC_PATH
""")
```

**Important:** Pass the project context + coding instructions + spec path. Do NOT include
the PM agent's analysis, code samples, or reasoning — the coder should form their own
implementation approach based solely on the spec and project context.

Record the returned file list as `CHANGED_FILES`.

---

## Phase 3 — Code Review

Read `phases/code-review.md` and spawn:

```
subagent(agent='general', prompt="""
<contents of phases/code-review.md>

--- PROJECT CONTEXT ---
<PROJECT_CONTEXT>

Spec file: SPEC_PATH
""")
```

**Important:** Pass ONLY the project context + spec path. The reviewer must look at the
actual code via git diff and source reading tools — not be influenced by the coder's context.

The task's response must begin with `APPROVED` or `CHANGES NEEDED`.

**If `CHANGES NEEDED`:**

Use the `question` tool:
- Options:
  - "Auto-fix" — spawn a coding agent with the review findings
  - "I'll fix manually" — pause until user says to continue
  - "Override" — proceed despite findings

If **Auto-fix**: spawn a `subagent(agent='general')` with the project context, the spec
path, the review findings (explicitly passed — they are the contract for the fix), and the
instructions from `phases/coding.md`. Then re-run Phase 3. Repeat until `APPROVED` or user
overrides.

---

## Phase 4 — Test Generation

Read `phases/test-gen.md` and spawn:

```
subagent(agent='general', prompt="""
<contents of phases/test-gen.md>

--- PROJECT CONTEXT ---
<PROJECT_CONTEXT>

Spec file: SPEC_PATH
""")
```

Record the output as `TEST_FILES`.

---

## Phase 5 — Test Review

Read `phases/test-review.md` and spawn:

```
subagent(agent='general', prompt="""
<contents of phases/test-review.md>

--- PROJECT CONTEXT ---
<PROJECT_CONTEXT>

Spec file: SPEC_PATH
""")
```

Response must begin with `APPROVED` or `CHANGES NEEDED`.

**If `CHANGES NEEDED`:** spawn a fix agent with review findings + project context + spec
path, then re-run Phase 5. Repeat until `APPROVED`.

---

## Phase 6 — Commit

**HUMAN GATE — Final approval**

Use the `question` tool:
- Header: "Ready to commit"
- Question: "All phases complete. Spec: `SPEC_PATH`, Implementation: `CHANGED_FILES`, Tests: `TEST_FILES`. Ready to commit?"
- Options:
  - "Commit now"
  - "Let me review first"

When approved, use `eclipse-git_gitStatus` to see all changes. Stage every file
belonging to this feature with `eclipse-git_gitAdd`.

**Include:** feature code, spec file, test files, modified pom.xml, AGENTS.md (if test docs were added)
**Exclude:** opencode.json, .opencode/, unrelated files

Commit with `eclipse-git_gitCommit`. Message format (the trailing ` [ai]` is mandatory per
the repo's AGENTS.md; include the Jira key):
```
<JIRA_KEY> <short description from spec title> [ai]

- bullet points summarising what was built

Co-Authored-By: opencode <noreply@opencode.ai>
```

After committing, display the full commit message in a formatted block.

---

## Error handling

- If any phase subagent returns a tool error, report it to the user and ask how to proceed.
- If the Jira API is unreachable, the PM phase falls back to WebFetch of the issue URL.
- If a phase produces unexpected output, show it to the user via the `question` tool.
- If a phase subagent reports it lacks shell/write/Eclipse tools, you spawned the wrong
  agent — re-spawn it with `agent: general`.
