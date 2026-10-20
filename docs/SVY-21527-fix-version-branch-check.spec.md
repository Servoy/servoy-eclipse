# Spec: SVY-21527 — reviewer skill should check fix version against the branches it was pushed against

> **Target repository:** this is a prose/markdown change to the **`case-review`** skill in
> **servoy-eclipse**, at `C:\Users\Cristian\git\servoy\master\servoy-eclipse\skills\case-review\`
> (`SKILL.md` + `phases/context.md`, `phases/narrative.md`, `phases/synthesis.md`). There is
> **no** Java / Eclipse / Tycho work. The spec is written into the Servoy-Copilot repo's
> `docs/` only because the orchestrator reads it from there.
>
> **Fix Version of this issue:** `2026.12` (unreleased). **Type:** Bug, Minor, In Progress.

## 1. Goal

Teach the `case-review` peer-review skill to cross-check an issue's Jira **Fix Version**
against the **branch** the reviewed commits actually sit on, and to **report a mismatch** when
the two disagree — so a reviewer catches, during review, a fix that is targeted at the wrong
release line (e.g. a `2026.x` mainline version whose commits only landed on a maintenance
branch, or an LTS Fix Version whose commits only landed on `master`). The skill already
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

The servoy-eclipse checkout carries these long-lived lines (confirmed via `git branch -a`):
`master`, `release`, `2026.03`, `lts`, `lts_latest` locally, plus the convention-named
`lts_2024` / `lts_2025` / `lts_2026` the triage cites. Fix Versions are **date-based**
(`2026.12`, `2026.03`, …): the `YYYY.MM` number *is* the release the version ships in. The
current issue's Fix Version `2026.12` is unreleased and, by convention, belongs on `master`.

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
   list of version `name`s (e.g. `["2026.12"]`), or **empty** when the issue has no Fix
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

### 3.2 Branch → Fix Version convention (the mapping rule)

The rule the skill encodes. Fix Versions are date-based `YYYY.MM`; branches are either the
mainline, a dated/LTS maintenance line, or an in-flight feature branch. **Classify the branch,
then check consistency against the Fix Version's release line.**

| Branch class | What it is | Expected Fix Version |
|---|---|---|
| **Mainline** — `master` (also `main`, `develop`) | The next unreleased dev line | The **newest unreleased** `YYYY.MM` version (the one not yet released, e.g. `2026.12`). |
| **Maintenance / release line** — `release`, a dated line like `2026.03`, `lts`, `lts_latest`, `lts_2024`/`lts_2025`/`lts_2026` | A line that ships (or still patches) a specific release | A Fix Version **on that line**: the dated line's own version (`2026.03` → `2026.03`), or an LTS maintenance version carried by that LTS line. Must **not** be a newer version that only `master` ships. |
| **Feature branch** — anything else (`SVY-xxxxx_*`, `branch_*`, `copilot/*`, …) | Work not yet merged to any release line | **Cannot be determined from the branch alone** — the eventual release depends on where it merges. Treat as **"flag for the reviewer", never a mismatch.** |

**The mismatch rule, stated plainly:**

- A Fix Version is a **match** for a branch when the branch is the line that actually ships
  that version: the newest-unreleased version on `master`; a line's own dated/LTS version on
  that maintenance line.
- A **mismatch** is when they provably disagree — e.g. Fix Version `2026.12` (a mainline,
  not-yet-released version) but the commits sit **only** on a maintenance line such as
  `release` / `2026.03` / an `lts_*` branch; or an older/LTS Fix Version but the commits sit
  **only** on `master`. In date terms: the commit branch's release line and the Fix Version's
  `YYYY.MM` point at different releases, and neither is an ancestor relationship that a
  normal merge-forward would reconcile.
- **Indeterminate** (→ flag, do not call it a mismatch): a feature branch; no Fix Version set;
  multiple Fix Versions where at least one matches the branch; a branch name the skill cannot
  classify; or the version list could not be read. The default on any uncertainty is
  **"flag for the reviewer to confirm"**, phrased as a question, not a verdict.

**Merge-forward awareness.** Servoy maintenance fixes are expected to merge forward (a
`release`/LTS fix also reaches `master`). So a fix *on a maintenance line* with a maintenance
Fix Version is **consistent even though `master` will also carry it** — that is the intended
flow, already noted at `regression.md:149-157` / `synthesis.md:87` ("whether it must merge
forward"). The mismatch is the *reverse direction*: a **newer** (mainline-only) Fix Version on
a branch that will **not** carry it into that version, or a maintenance Fix Version on
`master` only (never back-ported to the line that version names).

**Confidence / conservatism.** The skill must not invent a release calendar. It decides
"newest unreleased version" from the Jira version list where available
(`GET /project/SVY/versions`, `released:false`), and otherwise from the branch-name signal
alone. If it cannot tell with confidence which line a Fix Version belongs to (unfamiliar
version string, ambiguous branch name), it **flags** rather than asserting a mismatch. This is
the one explicit design choice the triage left open, and it resolves to: **document the table
above in the skill, apply it, and degrade to "flag for reviewer" on any doubt.**

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
   Fix Version: `<2026.12>` — **Release-target check: MATCH / MISMATCH / INDETERMINATE** — <one-line reason>
   ```

   On `MATCH` this is a one-line reassurance. On `INDETERMINATE` it states what could not be
   determined and asks the reviewer to confirm. On `MISMATCH` it states the disagreement
   plainly.

2. **"Top risks" (`phases/synthesis.md:96-102`) — only on `MISMATCH`.** Add a finding, e.g.:

   ```markdown
   N. **[Medium]** Fix Version does not match the branch the commits landed on
      - Fix Version `2026.12` targets the `master` (next-release) line, but the reviewed
        commits sit only on `<branch>`, which ships `<other line>`. As written, the fix will
        not reach the `2026.12` release — or the stated Fix Version is wrong.
      - Confirm with the author which release this is meant to ship in, and either re-target
        the Fix Version or push/merge the change to the branch that carries it.
   ```

   Severity is **Medium by default** (a delivery/targeting error, not a code-correctness or
   security one) and never on its own drives the overall rating above `MODERATE`. An
   `INDETERMINATE` result is **not** a Top risk — it lives only in "Scope reviewed" as a
   flag, consistent with "flag, don't block".

The author-facing phrasing of a `MISMATCH` (the "confirm which release" line) is suitable for
the Phase F "Questions for the author" list, so the reviewer can forward it verbatim.

## 4. Implementation plan

All edits are in `C:\Users\Cristian\git\servoy\master\servoy-eclipse\skills\case-review\`.
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
- [ ] A concrete branch → Fix Version convention table is written into the skill (one
      canonical copy), covering mainline, maintenance/LTS lines, and feature branches.
- [ ] The convention encodes merge-forward direction: a maintenance-line fix with a
      maintenance Fix Version is a MATCH even though `master` will also carry it; a
      newer/mainline-only Fix Version on a branch that won't carry it is a MISMATCH.
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
- Verifying *merge-forward completion* across branches (whether a maintenance fix has actually
  been merged up yet) — the check reasons about targeting, not merge state.
- Teaching the skill the full Servoy release calendar; it uses the Jira version list's
  `released` flag and branch-name signals, and degrades to a flag when unsure.
- Generalising the convention to GitHub/GitLab milestones — the rule is Servoy/Jira-specific;
  non-Jira refs make the check a no-op.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Exact expected mapping for the `release` branch vs the newest dated maintenance line (e.g. is `release` always the single newest maintenance version, or can it trail?) — the spec treats `release` as "a maintenance line; its Fix Version must be a maintenance version, not a mainline-only one", which is enough for the flag, but a precise `release`→version identity would tighten MATCH detection. | Servoy release process owner | Assumed: `release` = maintenance line, flag on doubt |
| Should the check try `GET /project/SVY/versions` live to identify the "newest unreleased" version, or rely on branch-name signals only to avoid an extra API call per review? Spec allows either; defaults to using the version list when cheaply available, branch-name signal otherwise. | Implementer | Assumed: use version list when available, else branch signal |
| For an issue with **multiple** Fix Versions spanning lines, is "at least one matches the branch" sufficient for MATCH, or should every version be reconciled? Spec assumes at-least-one-matches → not a mismatch (flag only). | Reviewer/process owner | Assumed: at-least-one-match ⇒ not a mismatch |
