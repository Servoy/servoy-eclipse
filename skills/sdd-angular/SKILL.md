---
name: sdd-angular
description: "Spec-Driven Development pipeline for Servoy Angular repositories (TiNG NG-client, RFB/WPM designers, web-component packages): Jira issue → triage → spec → implementation → code review → test generation → test review → commit. Triggered by the per-repo /sdd command, 'sdd', 'spec driven development', or a Jira issue key like SVY-12345 in an Angular repo."
---

# SDD — Spec-Driven Development Pipeline (Angular)

You are the **orchestrator** for the full SDD pipeline:
Triage → PM Agent → Coding → Code Review → Test Gen → Test Review → Commit.

You collect output from each phase, show summaries to the user at approval gates,
and thread context forward **selectively** to maintain isolation between phases.

This skill is the shared, repository-agnostic **Angular** variant. It is installed globally
and invoked in any Servoy Angular repo (the TiNG NG-client, the RFB/WPM designer frontends,
and standalone web-component packages). Everything project-specific comes from the repo's own
`project-context.md` — the phase logic here never hardcodes one repo's Angular version, test
runner, or build commands.

## Resolving this skill's own files

The phase instruction files live in a `phases/` directory **next to this `SKILL.md`**.
This skill is installed globally, so `phases/` is generally **NOT** under the current
working directory. Resolve the phase files relative to **this skill's own location** — the
absolute path shown for this skill in your available-skills listing — and read them with the
`read` tool using that absolute path.

Phase files: `phases/triage.md`, `phases/pm-agent.md`, `phases/coding.md`,
`phases/code-review.md`, `phases/test-gen.md`, `phases/test-review.md`.

## Repo-local project context (REQUIRED)

Every repo that uses this skill commits a `project-context.md` describing its own stack
(Angular version, test runner, build/lint commands, library layout, gotchas). Its canonical
location is:

```
.opencode/sdd/project-context.md   (relative to the repo root / current working directory)
```

The per-repo `/sdd` command normally passes this file's contents into the prompt that loaded
this skill. **If you were given the project-context inline, use that.** Otherwise, read
`.opencode/sdd/project-context.md` from the current working directory now. If it is missing,
tell the user the repo has not been onboarded to SDD (it needs
`.opencode/sdd/project-context.md`) and ask whether to proceed with generic assumptions.

Record the full project-context text as `PROJECT_CONTEXT`. You MUST pass `PROJECT_CONTEXT`
into every phase subagent prompt — those subagents start with a fresh context and cannot see
the repo otherwise. **Do not assume Angular version, test runner (Vitest vs Karma/Jasmine),
or build commands — take them from PROJECT_CONTEXT.**

## Context isolation principle

Each phase runs as a `subagent` (the built-in `general` agent) with a **fresh context**.
This prevents bias:
- The Triage agent evaluates the problem free of any spec-writing incentive
- The Coder only sees the spec + project context, not the PM's internal analysis
- The Code Reviewer only sees the spec + actual code, not the Coder's reasoning
- The Test Generator only sees the spec + implementation, not review findings

You control exactly what information flows between phases via the subagent prompt.

**Spawning a phase:** use the `subagent` tool with `agent: general`. The built-in `general`
agent has shell, edit/write, and (in Eclipse-hosted checkouts) the Eclipse MCP tools —
everything the Angular phases need to run `npm`/`ng`/`vitest`, edit files, and use git. Do
NOT use the read-only `explore` agent for any phase that must run shell commands (Jira API,
git, npm) or write files (triage report, spec, code, tests).

## Input

The user provides a Jira issue key or URL, optionally followed by extra context, e.g.:
`SVY-21234 some text meant to give more context about the case`

Parse the first token as the issue key/URL. Everything after it is supplementary context.
Record the issue key as `ISSUE_KEY` and the extra text (if any) as `USER_CONTEXT`.

---

## Phase 0 — Triage & Root-Cause Investigation

Before writing any spec, run an isolated triage agent whose only mandate is to find the
*actual* root cause and decide whether — and how — the problem should be addressed.

Read `phases/triage.md` (resolve relative to this skill's own location) and spawn:

```
subagent(agent='general', prompt="""
<contents of phases/triage.md>

--- PROJECT CONTEXT ---
<PROJECT_CONTEXT>

Issue: ISSUE_KEY
User context: USER_CONTEXT (or "None")
""")
```

Pass ONLY the issue key + user context + project context. The triage agent investigates the
codebase and git history itself. Its output is the relative path to the triage report;
record it as `TRIAGE_PATH` and read it for the **verdict** (`PROCEED`, `NO_ACTION`,
`NEEDS_INPUT`) and recommendation.

**HUMAN GATE — Triage decision.** Display a short summary (verdict + recommendation), then
gate. The AI recommends, the human decides — never auto-stop on `NO_ACTION`.

### Gate for `PROCEED`
Use the `question` tool (Header "Triage Decision"): options "Proceed to spec" (record
recommendation as `APPROVED_APPROACH`, go to Phase 1), "No action — stop pipeline" (end),
"Redirect approach" (record user's direction as `APPROVED_APPROACH`, go to Phase 1).

### Gate for `NO_ACTION`
Options "No action — stop pipeline" (end) and "Redirect approach" (record as
`APPROVED_APPROACH`, go to Phase 1).

### Gate for `NEEDS_INPUT`
Present the report's "Questions for the reporter" via the `question` tool: "Answer here"
(record answers as `USER_CONTEXT` additions, then the post-answer gate below — feed forward,
do NOT loop back into Triage), "Post questions to Jira" (use the `servoy-jira` skill or the
repo's `JIRA.md`; show exact reporter-facing text and get explicit confirmation before
posting; never post internal reasoning; then pause pending a reply), or "Stop pipeline".

### Post-answer gate (after `NEEDS_INPUT` — "Answer here")
Options "Proceed with my answers" (combine triage recommendation + answers as
`APPROVED_APPROACH`), "No action — stop pipeline", "Redirect approach".

---

## Phase 1 — PM Agent: Jira → Spec

Read `phases/pm-agent.md` and spawn:

```
subagent(agent='general', prompt="""
<contents of phases/pm-agent.md>

--- PROJECT CONTEXT ---
<PROJECT_CONTEXT>

Issue: ISSUE_KEY
User context: USER_CONTEXT (or "None")
Triage report: TRIAGE_PATH
Approved approach: APPROVED_APPROACH
""")
```

The triage report path and approved approach are **authoritative** — bound the spec to the
approved approach, not the raw ticket, and reuse triage's root-cause findings. Output is the
spec path; record as `SPEC_PATH`.

**HUMAN GATE — Spec approval.** `question` (Header "Spec Review"): "Approve" (go to Phase 2)
or "Request changes" (revise — minor edits yourself, substantial rewrites via a
`subagent(agent='general')`; loop until approved).

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

Do NOT include the PM agent's analysis or reasoning. Record the returned file list as
`CHANGED_FILES`.

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

Response must begin with `APPROVED` or `CHANGES NEEDED`.

**If `CHANGES NEEDED`:** `question` with "Auto-fix" (spawn a `subagent(agent='general')` with
project context + spec path + review findings + `phases/coding.md`, then re-run Phase 3),
"I'll fix manually" (pause), or "Override". Repeat until `APPROVED` or overridden.

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

Response must begin with `APPROVED` or `CHANGES NEEDED`. **If `CHANGES NEEDED`:** spawn a fix
agent with review findings + project context + spec path, re-run Phase 5. Repeat until
`APPROVED`.

---

## Phase 6 — Commit

**HUMAN GATE — Final approval.** `question` (Header "Ready to commit"): "Commit now" or
"Let me review first".

When approved, review changes with `git status` (or `eclipse-git_gitStatus` when the Eclipse
MCP is available). Stage every file belonging to this feature.

**Include:** feature code, spec file, test files, package.json changes if a dependency was
added/aligned.
**Exclude:** `node_modules/`, build output (`dist*/`), `opencode.json`, `.opencode/`, unrelated files.

Commit. Message format (the trailing ` [ai]` is mandatory per the repo's AGENTS.md; include
the Jira key):
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
- If a phase subagent reports it lacks shell/write tools, you spawned the wrong agent —
  re-spawn it with `agent: general`.
