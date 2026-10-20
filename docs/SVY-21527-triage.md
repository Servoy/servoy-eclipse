# Triage Report — SVY-21527

**Verdict:** PROCEED

## Reported problem

> **Summary:** "reviewer skill should check fix version against the branches it was pushed against"
> **Description:** "should/fix report any mismatch"

The reporter wants the **peer-review assistant skill** (`case-review`) to verify that an
issue's Jira **Fix Version** is consistent with the **branch(es)** the reviewed commits
actually landed on, and to **report a mismatch** when the two disagree.

The symptom: today a reviewer can run the case-review skill on an issue whose Fix Version
says one release line (say a non-LTS `2026.x` on `master`) while the commits were pushed to
a different line (say an `lts_2025` / `release` maintenance branch), and nothing in the
review flags it. A wrong Fix Version means the fix ships in the wrong release, or the branch
it was pushed to never carries it into the stated version — a real delivery bug that the
review is well-placed to catch but currently ignores.

There is no proposed *implementation* in the ticket — it states the desired behaviour
("check … report any mismatch") and leaves the how open. Priority Minor, Bug, Fix Version
2026.12, currently In Progress. No comments, no attachments, no linked issues.

## Root-cause assessment

**This is a missing-feature bug in the `case-review` skill, not a regression.**

> ⚠️ **Location note (important).** Despite the PROJECT CONTEXT handed to this triage
> describing the **Servoy-Copilot** Java bundles, SVY-21527 is about neither
> `com.servoy.eclipse.developer.mcp` nor `com.servoy.eclipse.opencode`. The "reviewer
> skill" is the **`case-review`** skill, which lives in the **servoy-eclipse** repo at
> `C:\Users\Cristian\git\servoy\master\servoy-eclipse\skills\case-review\` (SKILL.md +
> `phases/context.md`, `narrative.md`, `regression.md`, `security.md`, `synthesis.md`).
> This triage report is written into `Servoy-Copilot/docs/` only because the orchestrator
> pins the report path there; the **actual fix must be made in servoy-eclipse**. Any
> downstream spec/implementation phase must target that repository, not Servoy-Copilot.

The case-review skill already knows both halves of the check independently — it just never
joins them:

1. **It tracks branch topology but not Fix Version.**
   `phases/context.md:42` and `:269` classify the branch as
   *mainline | maintenance/release line | feature branch*, and `SKILL.md:184-189`
   (Phase B step 5) explicitly records "Which branch do the commits sit on — a mainline, a
   maintenance/release branch, a feature branch?". `phases/regression.md:149-157` and
   `phases/synthesis.md:87` reason about whether the change "must merge forward". So the
   **branch side** of the comparison is already computed.

2. **It never fetches the Fix Version.**
   The only Jira read in the skill is the Phase A.0 pre-flight gate,
   `SKILL.md:96-98`, which requests `fields=summary,status,assignee` — **`fixVersions` is
   not among them**. The change-narrative agent (`phases/narrative.md:27-52`) fetches
   "summary, description, comments, attachments, linked issues" but again not `fixVersions`.
   The servoy-jira skill it routes through *does* document `fixVersions`
   (`servoy-jira/SKILL.md:32`, `:387-410`), so the capability exists — the case-review skill
   simply never asks for the field.

3. **Nothing compares the two.** There is no step, in any phase, that cross-checks the Jira
   Fix Version against the branch the commits sit on. The `synthesis.md` "Scope reviewed"
   table (`:82-87`) shows the branch and the merge-forward note, but no Fix Version column
   and no consistency verdict.

So the root cause is precisely what the ticket says: the reviewer skill has the branch
topology and has access to the Fix Version, but performs no mismatch check between them.

**Not a regression.** `git log` on the skill
(`git -C servoy-eclipse log --oneline -- skills/case-review`) shows the skill was introduced
whole in `318981976f` ("Add case-review peer-review skill with Jira workflow integration")
and refined in `ce286fc4c7`, `3e25d98ccf`, `81e44ff69d`. No earlier version ever contained a
Fix-Version-vs-branch check — this is net-new behaviour, not something that broke.

## Ticket premise check

**The premise holds.** The ask is well-formed and maps cleanly onto the skill's existing
structure:

- "**reviewer skill**" → the `case-review` skill (unambiguous — it is the only reviewer
  skill, and its own description says "review this case / peer review / review SVY-12345").
- "**fix version**" → the Jira `fixVersions` field, already a first-class concept in the
  sibling `servoy-jira` skill.
- "**the branches it was pushed against**" → the branch topology Phase B already determines.
- "**report any mismatch**" → surface it in the synthesis briefing (naturally the "Scope
  reviewed" section and/or a top-risk finding).

The one genuinely open piece the ticket does *not* specify is the **branch → Fix Version
mapping rule** — i.e. what counts as a "mismatch". The repository layout
(`master`, `release`, `lts_2024`, `lts_2025`, `lts_2026`) implies a convention (a non-LTS
future version like `2026.12` belongs on `master`; an LTS/maintenance Fix Version belongs on
the matching `lts_*` / `release` line), but the precise rule is a Servoy process convention
that the implementer must encode or have the skill infer. This is a **design detail for the
spec/implementation phase**, not an ambiguity that blocks triage — the desired behaviour
("check, and report mismatches") is clear, which is why the verdict is PROCEED rather than
NEEDS_INPUT.

## Approaches considered

1. **Add the Fix-Version-vs-branch check inside the existing case-review flow (recommended).**
   - Fetch `fixVersions` in the Phase A.0 gate (`SKILL.md:96-98`, add the field) and in the
     narrative agent's issue read. Record the Fix Version alongside the branch topology in
     Phase B / `REPO_CONTEXT`. Add an explicit mismatch check and surface it in
     `synthesis.md`'s "Scope reviewed" block (a Fix-Version row + a consistency verdict), and
     as a finding when they disagree. Encode a small, documented branch→version mapping with a
     conservative "flag, don't block" default when the mapping is unknown.
   - **Pros:** reuses machinery already present; lands exactly where the reviewer reads;
     Minor scope; no new tooling; consistent with how the skill already treats branch
     topology and the servoy-jira field vocabulary.
   - **Cons:** must pin down the branch→version convention; prose-only "skill" change, so its
     correctness is validated by running the skill, not by a unit test.

2. **New dedicated phase / standalone check.**
   - A separate "release-targeting" phase or a one-off gate script.
   - **Pros:** isolated, explicit.
   - **Cons:** heavier than a Minor bug warrants; duplicates branch-topology discovery that
     Phase B already does; more surface to keep in sync. Over-engineered.

3. **No code change.**
   - **Pros:** none beyond zero effort.
   - **Cons:** leaves the reporter's exact, reasonable request unmet. The skill genuinely
     lacks this check, the data to perform it is already at hand, and a wrong Fix Version is
     a real delivery error the review is positioned to catch. Rejecting this would be ignoring
     a valid bug. **Not appropriate.**

## Recommendation

**PROCEED with Approach 1** — extend the existing `case-review` skill (in **servoy-eclipse**,
not Servoy-Copilot) to:

1. Fetch `fixVersions` where it already reads the issue (Phase A.0 gate and the narrative
   agent's Jira read).
2. Carry the Fix Version next to the branch topology that Phase B already determines.
3. Compare them against a small, explicit branch→version convention and **report any
   mismatch** in the synthesis briefing's "Scope reviewed" section (and as a finding when
   they disagree), defaulting to "flag for the reviewer" rather than failing hard when the
   mapping cannot be determined with confidence.

Alternatives 2 (dedicated phase) and 3 (no change) are rejected: 2 is disproportionate for a
Minor bug and duplicates existing discovery; 3 leaves a valid, low-cost, high-signal request
unaddressed.

The spec/implementation phase should settle one design point: the exact branch → Fix Version
mapping rule (which branch is expected to carry which version), and how to behave when a
reviewed branch is a feature branch not yet merged to any release line.

## Git history findings

- `git -C servoy-eclipse log --oneline -- skills/case-review` →
  `81e44ff69d` (skill improvements), `3e25d98ccf` (route through servoy-jira),
  `ce286fc4c7` (log review time on transition), `318981976f` (initial add). The skill was
  added whole; **no prior Fix-Version check existed**, so this is a missing feature, not a
  regression.
- Branch layout in the servoy-eclipse checkout: `master`, `release`, `lts_2024`, `lts_2025`,
  `lts_2026` — this is the set of lines a Fix Version would be checked against.
- The `servoy-jira` skill already documents `fixVersions` reads/writes
  (`servoy-jira/SKILL.md:32`, `:387-410`), confirming the field is readily available to the
  case-review skill — it simply does not request it today.
