# Peer-review summary — SVY-21280: Test and understand really how Servoy AI (skills/tools) work

**Risk: MODERATE.** Both softened flows (missing `AGENTS.md`, missing git repo) are correctly
gated behind an explicit user "yes" with no destructive or unconditional action, but the new
`AGENTS.md` auto-analysis gathers live DB/package data *before* the user answers anything, and
nothing in the diff verifies the result before it is delegated to a Reviewer-skipping write.

**Reviewed scope:** `skill4servoy` commit `0ca2a6f3cb6900a197910baee20957dec45b2777` on branch
`master` — `.opencode/agents/Orchestrator.md` (+20/-2). No sibling-repo commits.

## Manual test plan

### Verifying the fix
1. Open a workspace with no `AGENTS.md` and no git repo. Start a session and give the
   Orchestrator any ordinary task.
2. Expect: no hard stop. It silently detects DB/version info, then asks "AGENTS.md is missing.
   Want me to also analyze the project... or just fill in the defaults?" — confirm the question
   appears before any file is written.
3. Answer "just defaults." Expect: Developer is dispatched to write `AGENTS.md` into the active
   solution's project directory (not the workspace root), Reviewer is explicitly skipped for
   this one write, and the session continues to the git-repo check.
4. At the git check, expect: "This workspace is not a git repository... Want me to initialize it
   for you?" Answer "yes" and confirm `servoy-git_gitInit` runs and the session proceeds. Answer
   "no" on a repeat run and confirm it stops cleanly.

### Regression checks
1. Repeat with a workspace that already has a complete `AGENTS.md` and an initialized git repo —
   confirm bootstrap behaves exactly as before, no new questions.
2. Ask the Orchestrator for something it has no direct tool for but a subagent does (e.g.
   "install the X package") — confirm it delegates instead of saying "do this in the UI."
3. Ask for something no agent anywhere can do — confirm it still correctly reports the
   limitation instead of hallucinating a delegation.
4. In the AGENTS.md-missing case, answer "just defaults" on a workspace with two or more
   database servers configured. Inspect the written `AGENTS.md` by hand afterward and confirm
   the chosen "active" database is the right one.

### Automated checks worth running
None identified — no test suite exists for prompt/instruction files in this repo.

## Possible improvements / follow-ups
- Add a confirmation/preview step before Developer writes auto-detected defaults into
  `AGENTS.md`, rather than writing silently-gathered values straight through on a "just
  defaults" answer.
- Consider scoping `servoy-git_gitInit: allow` by path in the permission block itself, rather
  than relying on the tool's own implementation (believed to always target the workspace root)
  plus prose gating — the same block path-restricts `write`/`edit` to `handoff/**`/`docs/**`,
  but this new grant has no equivalent restriction.
- Rule 10 ("never claim impossible without checking subagents") hardcodes a tool-family list per
  subagent in its own prose; this will drift as subagent permission blocks change without a
  matching edit here (already observed in the shippable file today vs. this commit).
- Confirm whether the level of DB/security-pattern detail gathered in full-analysis mode should
  be opt-in per field rather than all-or-nothing behind a single yes/no.
