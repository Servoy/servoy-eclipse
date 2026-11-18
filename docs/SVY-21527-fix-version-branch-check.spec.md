# Spec: SVY-21527 — reviewer skill should check fix version against the branches it was pushed against

> **Target repository:** this is a prose/markdown change to the **`case-review`** skill in
> **servoy-eclipse**, at `skills/case-review/` (`SKILL.md` + `phases/context.md`,
> `phases/narrative.md`, `phases/synthesis.md`). There is **no** Java / Eclipse / Tycho work.
> This spec lives in the repository's `docs/` directory.
>
> **Fix Version of this issue:** `2026.12` (unreleased). **Type:** Bug, Minor, In Progress.

## 1. Goal

Teach the `case-review` peer-review skill to cross-check an issue's Jira **Fix Version**
against the **branch** the reviewed commits actually sit on, and to **report a mismatch** when
the two disagree — so a reviewer catches, during review, a fix that is targeted at a release
that the commits have not reached (e.g. a Fix Version shipped by the mainline whose commits
only landed on a maintenance branch and were not merged forward yet, or an LTS Fix Version
whose commits only landed on the mainline). The skill already
computes the branch topology and already has access to `fixVersions` through the sibling
`servoy-jira` skill; it simply never fetches the field nor joins the two. The check must be a
**flag, not a hard failure**: when the branch→version mapping cannot be determined with
confidence, surface it for the reviewer rather than blocking the review.

## 2. Background

### 2.1 The two halves already exist, but are never joined

- **Branch topology is already determined and recorded.** Phase B step 5
  (`SKILL.md:186-188`) explicitly says to "Note the branch topology. Which branch do the
  commits sit on — a mainline, a maintenance/release branch, a feature branch? A fix landing
  on a maintenance branch has a lower risk budget and will have to be merged forward." The
  resulting scope is recorded as `SCOPE` (`SKILL.md:190`). Phase A context discovery also
  classifies the current branch as *mainline (`main`/`master`/`develop`) | maintenance/release
  line (`release`/`lts_*`/a version number) | feature branch* (`context.md:41-43`) and emits
  it in `REPO_CONTEXT` as `**Branch:** <branch> — <mainline | maintenance/release line |
  feature branch>` (`context.md:269`).

- **The Fix Version is never fetched.** The only structured Jira reads are:
  - Phase A.0 pre-flight gate — `GET /rest/api/3/issue/{JIRA_KEY}?fields=summary,status,assignee`
    (`SKILL.md:96-98`). `fixVersions` is **not** in that field list.
  - The narrative agent's issue read (`phases/narrative.md:27-52`) — fetches "summary,
    description, comments, attachments, linked issues" but **not** `fixVersions`.
  The capability exists: the `servoy-jira` skill documents `fixVersions` as an array of
  version objects (`servoy-jira/SKILL.md`, "Setting Fix Version and Sprint") and lists them
  via `GET /project/SVY/versions`. The case-review skill simply never asks for the field.

- **Nothing compares the two.** No phase cross-checks the Fix Version against the branch. The
  synthesis "Scope reviewed" block (`phases/synthesis.md:82-87`) renders a scope table plus a
  single `Branch: <branch> — <and whether it must merge forward>` line, but has **no** Fix
  Version column and **no** consistency verdict. Findings surface in the "Top risks" section
  (`synthesis.md:96-102`).

### 2.2 The branch lines in play

The Java repositories share long-lived branch names: the mainline (`master`), `release`,
`lts`, `lts_latest`, `lts_<year>` and dated lines such as `2026.03`. Which release each of
them ships is **not** fixed: it changes every time a release is cut (the mainline moves on to
the next version, `release` moves to a new line, and so on). The skill therefore never
hard-codes a version-to-branch mapping. It reads the version from the source of each branch
(see 3.2). Jira Fix Version names also do not match the source format one-to-one (`2026.12`
vs `2026.12.0`, `2026.3.3 LTS` vs `2026.3.3` with `lts = true`), so they are normalised first.

### 2.3 Not a regression

Git history (`git log --oneline -- skills/case-review`) shows the skill was introduced whole
in `318981976f` and refined in `ce286fc4c7`, `3e25d98ccf`, `81e44ff69d`. No earlier version
ever carried a Fix-Version-vs-branch check — this is net-new behaviour.

## 3. Design

The check is woven into the existing flow in three touch points — **fetch**, **carry**,
**compare & report** — with no new phase. This keeps it proportional to a Minor bug and
consistent with how the skill already treats branch topology and the `servoy-jira` field
vocabulary.

### 3.1 Fetching the Fix Version

Add `fixVersions` to the two places the issue is already read:

1. **Phase A.0 gate (`SKILL.md:96-98`).** Change the field list to
   `fields=summary,status,assignee,fixVersions`. Record the result as `FIX_VERSIONS` — the
   list of version `name`s (e.g. `["<version A>", "<version B>"]`), or **empty** when the issue has no Fix
   Version set. Note that an issue may carry **more than one** Fix Version (multiple release
   lines), and that a feature-branch review may legitimately have none yet.
   - This read only runs for Jira issue keys (`JIRA_KEY` set). When `ISSUE_REF` is a plain
     range or a GitHub/GitLab `#id`, there is no Jira Fix Version; set `FIX_VERSIONS` to
     "none (not a Jira issue)" and the check in 3.3 becomes a no-op that says so.

2. **Narrative agent's Jira read (`phases/narrative.md:27-52`).** Add `fixVersions` to the
   fetched fields so the narrative also has it available; record it alongside the reported
   symptom. This is secondary — the authoritative value for the check is the one captured in
   A.0 / Phase B — but keeps the narrative self-consistent. (Low priority; the mismatch check
   does **not** depend on the narrative agent.)

`FIX_VERSIONS` is carried in `REPO_CONTEXT`/`SCOPE` the same way the branch already is, so the
synthesis phase receives it without a new channel.

### 3.2 Fix Version → branch resolution (the rule)

No hard-coded version numbers and no assumption such as "the mainline ships the newest
version". After a release is made, the version in the source is bumped, so the value on a
branch is the **next** release that branch will produce. The skill resolves every Fix Version
to a branch by reading that value, then verifies the fix is on that branch.

1. **Read the version from source** (remote branch `origin/B`):
   - *Core Java repos:* `com.servoy.j2db.ClientVersion` in `servoy-client`
     (`servoy_shared/src/com/servoy/j2db/ClientVersion.java`). Branch names are the same across
     the Java repos, so branch `B` of any of them is read from branch `B` of `servoy-client`.
   - *Extension packages:* `Bundle-Version` in `<some dir>/META-INF/MANIFEST.MF` (the layout
     varies per repo); the root `webpackage.json` declares core compatibility.
2. **Normalise** Jira and source versions to `major.middle.minor` (missing minor = `0`; ignore
   ` LTS`, `versionPostfix`, `-SNAPSHOT`).
3. **Which Fix Versions.** Jira usually has no extension-package versions, so for a fix in an
   extension package they are taken from the issue comments (none found = `INDETERMINATE`).
4. **Resolve and verify.** Find the remote branch whose version equals the Fix Version and check
   the fix is on it (agent's choice of git commands). A pending forward merge is a `MISMATCH`.
5. **Already released.** If no branch reports the Fix Version any more, compare against the
   source version **at the commit's sha** (never today's): greater than the Fix Version =
   shipped before the fix (`MISMATCH`), equal = `MATCH`. Git tags refine when a release was cut.

**Verdicts:** `MATCH` = every Fix Version's release contains the fix. `MISMATCH` = at least one
does not (partial when others are satisfied; list both groups). `INDETERMINATE` = feature
branch, no Fix Version, unreadable source, a Fix Version that cannot be placed, or a non-Jira
`ISSUE_REF`. Always a flag, never a failure.

The check runs on the **confirmed** scope, i.e. after the Ambiguity gate, and the agent is free
to choose the git commands used to find which branches contain the commits.

### 3.3 The mismatch check and how it is reported

The orchestrator computes the verdict in **Phase B** (right after step 5, where the branch
topology is already noted), using `FIX_VERSIONS` from A.0 and the branch class. It records a
small `FIX_VERSION_CHECK` result — one of `MATCH`, `MISMATCH`, or `INDETERMINATE` — with a
one-line reason, and carries it into the synthesis prompt alongside `SCOPE`.

The **synthesis phase reports it in two places**, matching the existing structure exactly:

1. **"Scope reviewed" block (`phases/synthesis.md:82-87`).** Extend it to carry the Fix
   Version and the verdict next to the existing branch line, e.g.:

   ```markdown
   ## Scope reviewed
   | Repository | Commit | Subject | Files | +/- |
   |---|---|---|---|---|
   | servoy-eclipse | `abc1234` | ... | 3 | +40/-12 |

   Branch: `<branch>` (<mainline | maintenance line | feature branch>) — <merge-forward note>
   Fix Version: `<FIX_VERSIONS>` — **Release-target check: MATCH / MISMATCH / INDETERMINATE** — <one-line reason>
   ```

   On `MATCH` this is a one-line reassurance. On `INDETERMINATE` it states what could not be
   determined and asks the reviewer to confirm. On `MISMATCH` it states the disagreement
   plainly.

2. **"Top risks" (`phases/synthesis.md:96-102`) — only on `MISMATCH`.** Add a finding, e.g.:

   ```markdown
   N. **[Medium]** Fix Version(s) not covered by the commits
      - Fix Version `<v>`: <branch `<b>` (source version `<x.y.z>`) does not contain the fix yet
        | that release shipped before the commit>. Satisfied: `<versions>`; not satisfied:
        `<versions>` (mark "partial" if some are satisfied).
      - Likely cause: merge/backport pending, or wrong Fix Version. Confirm with the author.
   ```

   Severity is **Medium by default** (a delivery/targeting error, not a code-correctness or
   security one) and never on its own drives the overall rating above `MODERATE`. An
   `INDETERMINATE` result is **not** a Top risk — it lives only in "Scope reviewed" as a
   flag, consistent with "flag, don't block".

The author-facing phrasing of a `MISMATCH` (the "confirm which release" line) is suitable for
the Phase F "Questions for the author" list, so the reviewer can forward it verbatim.

## 4. Implementation plan

All edits are in `skills/case-review/` of this repository.
Use the generic `read`/`edit`/`write` tools (markdown, not Java).

1. **`SKILL.md`, Phase A.0 step 1 (around lines 96-100).**
   - Change the field list to `fields=summary,status,assignee,fixVersions`.
   - After the status/assignee note, add a sentence: "Record the Fix Version name(s) as
     `FIX_VERSIONS` (may be empty, or multiple). It feeds the Phase B release-target check."

2. **`SKILL.md`, Phase B step 5 (around lines 186-190).**
   - After the existing "Note the branch topology" paragraph, add a new sub-step
     **"Release-target check"** that: classifies the branch per the table in §3.2; compares it
     to `FIX_VERSIONS`; and records `FIX_VERSION_CHECK` = `MATCH` / `MISMATCH` /
     `INDETERMINATE` with a one-line reason. State the "flag, don't block" default on any
     uncertainty, feature branch, missing Fix Version, or non-Jira `ISSUE_REF`.
   - Inline the branch→version convention table (§3.2) here, or add a short
     **"Branch → Fix Version convention"** reference block right after Phase B so Phase A /
     context.md can point at a single canonical statement of the rule. (Prefer one canonical
     copy; cross-reference from the other.)
   - Note that `FIX_VERSION_CHECK` and `FIX_VERSIONS` are carried into the Phase E synthesis
     prompt alongside `SCOPE`.

3. **`SKILL.md`, Phase E prompt (around lines 299-311).**
   - Add `Fix Version: FIX_VERSIONS` and `Release-target check: FIX_VERSION_CHECK` lines to
     the synthesis prompt block so the synthesis agent receives the verdict.

4. **`phases/synthesis.md`.**
   - **Input list (lines 9-16):** add `FIX_VERSIONS` and `FIX_VERSION_CHECK` as inputs.
   - **"Scope reviewed" template (lines 82-87):** add the `Fix Version: … — Release-target
     check: … — <reason>` line beneath the `Branch:` line, per §3.3.1.
   - **"Top risks" guidance (lines 96-102):** add a note that a `MISMATCH` from
     `FIX_VERSION_CHECK` is listed as a Medium finding (template in §3.3.2), that an
     `INDETERMINATE` is a flag in "Scope reviewed" only, and that the mismatch alone caps the
     overall rating at `MODERATE`.
   - Optionally mention in the "Questions for the author" guidance (lines 138-141) that a
     `MISMATCH`'s confirm-release question belongs there.

5. **`phases/context.md` (branch classification, lines 41-43 & the `REPO_CONTEXT` `**Branch:**`
   line at 269).**
   - Add one sentence that the branch class recorded here is the input to Phase B's
     release-target check, and (if the canonical convention table lives in SKILL.md)
     cross-reference it. No behavioural change to context.md's own output shape.

6. **`phases/narrative.md`, step 1 (lines 27-52).** *(Secondary, low priority.)*
   - Add `fixVersions` to the narrative's Jira fetch and record the Fix Version with the
     reported symptom, so the narrative is self-consistent. The mismatch check does not depend
     on this.

7. **Consistency pass.** Grep the skill for any other place that enumerates the Jira fields it
   reads or that renders the branch/scope, to make sure the new field and verdict are not
   contradicted elsewhere.

## 5. Acceptance criteria

- [ ] Phase A.0's Jira read includes `fixVersions`; the value is recorded as `FIX_VERSIONS`
      (including the empty and multi-value cases).
- [ ] Phase B classifies the branch and computes a `FIX_VERSION_CHECK` of `MATCH`,
      `MISMATCH`, or `INDETERMINATE` using the documented branch→version convention.
- [ ] A Fix Version → branch resolution procedure is written into the skill (one canonical
      copy): where each project type keeps its version (`ClientVersion`, extension
      `MANIFEST.MF` / `webpackage.json`), normalisation, resolution and verification. It
      contains no hard-coded version numbers or "mainline ships the newest version" rule.
- [ ] Merge-forward direction is encoded: a fix on a maintenance branch whose Fix Version
      belongs to a later line is a MISMATCH until the forward merge has happened.
- [ ] With several Fix Versions, every one is checked; any unsatisfied one gives a MISMATCH,
      reported as partial when others are satisfied.
- [ ] On uncertainty (feature branch, no Fix Version, unclassifiable branch, unreadable
      version list, non-Jira `ISSUE_REF`), the result is `INDETERMINATE` and is **flagged, not
      failed** — the skill never aborts the review over this check.
- [ ] The synthesis "Scope reviewed" block shows the Fix Version and the release-target
      verdict next to the branch line, for all three verdicts.
- [ ] A `MISMATCH` additionally appears as a Medium "Top risks" finding with an author-facing
      "confirm which release / re-target or push" remediation; it alone does not push the
      overall rating above MODERATE.
- [ ] An `INDETERMINATE` appears only as a flag in "Scope reviewed", not as a Top risk.
- [ ] The Phase E synthesis prompt passes `FIX_VERSIONS` and `FIX_VERSION_CHECK` through.
- [ ] No existing phase behaviour is removed or contradicted; the change is additive prose.
- [ ] (Secondary) The narrative agent fetches and records `fixVersions`.

## 6. Out of scope

- A dedicated new review phase or standalone gate script (triage Approach 2 — rejected as
  disproportionate for a Minor bug and duplicative of Phase B's existing branch discovery).
- Any Java / Eclipse / Tycho / Servoy-Copilot bundle change.
- **Writing** or changing a Fix Version on the issue — the skill only reads it and reports; it
  never mutates `fixVersions`.
- Blocking, failing, or auto-transitioning the review on a mismatch — the check is advisory
  and always "flag for the reviewer".
- Teaching the skill a Servoy release calendar; it reads versions from the source of each
  branch and degrades to a flag when unsure.
- Generalising the convention to GitHub/GitLab milestones — the rule is Servoy/Jira-specific;
  non-Jira refs make the check a no-op.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Which branch ships which version (e.g. what `release` ships)? | Servoy release process owner | Resolved: not hard-coded; read from `ClientVersion` / `MANIFEST.MF` on each branch (see 3.2) |
| Already-released Fix Versions? | Servoy release process owner | Resolved: use the source version at the commit's sha (tags refine); shipped before the commit = MISMATCH |
| Extension-package fixes (no Jira versions)? | Servoy release process owner | Resolved: Fix Versions are taken from the issue comments |
| Should a maintenance-branch fix whose forward merge is pending be a MISMATCH? | Servoy release process owner | Resolved: yes, until the forward merge has happened |
| How to find which branches contain the commits? | Implementer | Resolved: agent's choice using git (suggestions in 3.2) |
| Multiple Fix Versions: is one match enough? | Servoy release process owner | Resolved: every Fix Version is checked; any unsatisfied one gives MISMATCH (partial) |
