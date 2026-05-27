# SDD — Spec-Driven Development Pipeline

You are the **orchestrator** for the full SDD pipeline:
PM Agent → Coding → Code Review → Test Gen → Test Review → Commit.

Each phase runs as a sub-agent (via the `Agent` tool). You collect its output,
show summaries to the user at approval gates, and thread context forward.

## Input

`$ARGUMENTS` — a Jira issue key or URL, e.g.:
`SVY-21080` or `https://servoy-cloud.atlassian.net/browse/SVY-21080`

---

## Phase 1 — PM Agent: Jira → Spec

Spawn a sub-agent with this prompt (fill in the actual value of `$ARGUMENTS`):

---
*You are a Product Manager agent. Follow all instructions in
`.claude/commands/pm-agent.md` exactly. Also read `AGENTS.md` and `CLAUDE.md` at
the repository root for project conventions.*

*Input: `$ARGUMENTS`*

*Your final message must be exactly the relative path to the spec file you
created, on a single line, e.g.: `docs/SVY-21080-embedded-opencode.spec.md`*
---

Record the spec file path from the agent's final message. Call it `SPEC_PATH`.

**🛑 HUMAN GATE — Spec approval**

Tell the user: "Phase 1 complete. Spec written at `SPEC_PATH`. Please review it
(you can open the file in your editor), then let me know:"

Ask (single-select):
- **Approve** — proceed to implementation
- **Request changes** — provide feedback and I'll revise the spec

If the user requests changes, collect their feedback, then spawn the PM agent
again:

---
*Revise the spec at `SPEC_PATH` based on this feedback from the product owner:*

*`<user feedback>`*

*Keep the same file path. Read it first, apply the changes, save it.
Your final message must be exactly the spec file path.*
---

Loop this gate until the user approves.

---

## Phase 2 — Coding Agent: Spec → Implementation

Spawn a sub-agent:

---
*You are a senior developer implementing a feature for the Servoy Eclipse IDE.*

*Read these files first to understand conventions:*
*- `AGENTS.md` (tool policy, workflow, project structure)*
*- `CLAUDE.md` (project instructions)*
*- `$SPEC_PATH` (the full spec — this is your implementation contract)*

*Implement everything described in the spec's "Implementation plan" section.*

*Mandatory post-edit workflow for every Java file you touch:*
*1. `eclipse-coder_organizeImports`*
*2. `eclipse-coder_formatFile`*
*3. `eclipse-ide_getCompilationErrors` — fix all errors before moving to the next file*
*4. If quick fixes are available: `eclipse-ide_executeQuickFix`*
*5. Fix any blocking Spotbugs issues (two highest severity levels)*

*Zero compilation errors must remain when you finish.*

*Output: a bulleted list of every file created or modified.*
---

Record the file list as `CHANGED_FILES`.

---

## Phase 3 — Code Review

Spawn a sub-agent:

---
*You are a senior engineer performing a code review.*
*Follow all instructions in `.claude/commands/code-review.md` exactly.*
*Also read `AGENTS.md` and `CLAUDE.md`.*

*Spec: `$SPEC_PATH`*

*Your response must begin with exactly `APPROVED` or `CHANGES NEEDED`.*
---

Parse the first word of the agent's response.

**If `CHANGES NEEDED`:**

Show the user the review output and ask (single-select):
- **Auto-fix** — spawn the coding agent with the review findings; re-run review afterwards
- **I'll fix manually** — pause here; tell me when you're done and I'll re-run the review
- **Override** — proceed to test generation despite the findings

If **Auto-fix**: spawn a coding agent sub-agent:

---
*You are a senior developer fixing code review findings.*
*Read `AGENTS.md`, `CLAUDE.md`, and `$SPEC_PATH`.*

*Fix every blocking issue in the review below. Apply the post-edit workflow
(organizeImports → formatFile → getCompilationErrors → executeQuickFix) after
each file. Zero errors when done.*

*Review findings:*
*`<full review output>`*
---

Then re-run Phase 3. Repeat until `APPROVED` or the user overrides.

---

## Phase 4 — Test Generation

Spawn a sub-agent:

---
*You are a test engineer writing JUnit tests.*
*Follow all instructions in `.claude/commands/test-gen.md` exactly.*
*Also read `AGENTS.md` and `CLAUDE.md`.*

*Spec: `$SPEC_PATH`*

*Output: list every test file created and which acceptance criteria each covers.*
---

Record the list as `TEST_FILES`.

---

## Phase 5 — Test Review

Spawn a sub-agent:

---
*You are a senior engineer reviewing a test suite.*
*Follow all instructions in `.claude/commands/test-review.md` exactly.*
*Also read `AGENTS.md` and `CLAUDE.md`.*

*Spec: `$SPEC_PATH`*

*Your response must begin with exactly `APPROVED` or `CHANGES NEEDED`.*
---

**If `CHANGES NEEDED`:** spawn the test generation agent with the review feedback:

---
*You are a test engineer improving an existing test suite.*
*Follow `.claude/commands/test-gen.md`. Also read `AGENTS.md` and `CLAUDE.md`.*

*Spec: `$SPEC_PATH`*

*Address every blocking issue in the test review below. Also add missing coverage
identified in the review.*

*Test review findings:*
*`<full test review output>`*
---

Re-run Phase 5. Repeat until `APPROVED`.

---

## Phase 6 — Commit

**🛑 HUMAN GATE — Final approval**

Tell the user: "All phases complete ✅
- Spec: `SPEC_PATH`
- Implementation: `CHANGED_FILES`
- Tests: `TEST_FILES`

Ready to commit?"

Ask (single-select):
- **Commit now**
- **Let me review first** — I'll tell you when to commit

When the user approves, use `eclipse-git_gitStatus` to see all changed/untracked
files. Stage every file that belongs to this feature:

**Include:**
- All files under `com.servoy.eclipse.<feature>/`
- Spec file (`docs/<KEY>-*.spec.md`)
- `pom.xml` if it adds the new module
- Any other project files changed as part of this feature

**Exclude (never commit):**
- `AGENTS.md`
- `CLAUDE.md`
- `.mcp.json`
- `.claude/`
- Unrelated launch files or workspace files

Stage with `eclipse-git_gitAdd`, then commit with `eclipse-git_gitCommit`.

Commit message format:
```
<JIRA_KEY> <short description from spec title> [ai]

<3-5 bullet points summarising what was built>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

---

## Error handling

- If any sub-agent reports a tool error (MCP unavailable, file not found, etc.),
  report it to the user and ask how to proceed before continuing.
- If the Atlassian MCP is not available, the PM agent will fall back to WebFetch —
  warn the user that Jira authentication may be required.
- If a phase produces unexpected output (e.g. the review doesn't start with
  APPROVED/CHANGES NEEDED), show the raw output to the user and ask what to do.
