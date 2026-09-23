---
name: release-notes
description: "Use when generating Servoy product release notes across the multi-repo checkout (sablo, server, servoy-client, servoy-eclipse, servoy-extensions): collect commits between two tags, filter to product-relevant cases, review each case's Jira security level (public vs private) with the user, then write the public cases into the GitBook major-release document (release-notes/<major>.md). Triggered by 'release notes', 'generate release notes', 'release notes for 2026.9_RC2', or '/release-notes'."
---

# Release Notes — Cross-Repo Generator

You generate the release notes for a Servoy product release. The product is made of **five
git repositories** checked out side by side under a common parent directory. This skill
collects everything that changed between the previous release tag and now, across all five,
filters to the product-relevant cases, reviews each case's public/private status with the
user, and writes the public cases into the **GitBook** major-release document (one Markdown
file per major release — **not** into the product repos).

**Pipeline:** locate repos (Phase 0) → confirm the tag range (Phase 1) → collect commits and
filter to relevant cases (Phase 2) → review each case's security level with the user, twice,
so nothing private is published (Phase 2.5) → dependency notes (Phase 3) → write into the
GitBook major-release file (Phase 4) → report (Phase 5).

The five repositories:

- `sablo`
- `server`
- `servoy-client`
- `servoy-eclipse`
- `servoy-extensions`

## Inputs

Two parameters drive everything:

- `PREVIOUS_TAG` — the tag the last release was cut from (e.g. `2025.3.6`).
- `NEW_VERSION` — the version these notes are for (e.g. `2025.3.7`).

The user may give one, both, or neither, optionally in the trigger phrase
("release notes for 2025.3.7", "notes from 2025.3.6 to 2025.3.7").

## Phase 0 — Locate the repositories

You may be started from inside one of the five repos (commonly `servoy-eclipse`) rather than
the parent. Do **not** assume the current directory is the parent.

1. Find the parent that holds the five sibling checkouts. Start from the current repo root
   (`git rev-parse --show-toplevel`), go up one level, and confirm that the five repository
   directories exist there as git repos. If the current repo is `servoy-eclipse`, its parent
   is the natural candidate.
2. If any of the five are missing from that parent, list what you found and what you expected
   and **ask the user** for the correct parent directory rather than guessing.

Record the absolute parent path as `ROOT`. All `git` calls below run per-repo with
`git -C "<ROOT>/<repo>"`.

## Phase 1 — Infer the tags, then CONFIRM (always ask)

Auto-infer sensible defaults, then **always** ask the user to confirm before doing any work —
never run the collection on inferred values silently.

1. **Infer `PREVIOUS_TAG`** if the user did not give it. In `servoy-eclipse` (the reference
   repo), list tags newest-first and pick the most recent real release tag:

   ```
   git -C "<ROOT>/servoy-eclipse" tag --sort=-creatordate
   ```

   Prefer a plain release tag over pre-release/RC/LTS variants unless the user asked for one
   — skip tags containing `_RC`, `_LTS`, `RC1`, etc. when picking the "last release", but show
   them in the list so the user can override. Tag naming varies (`2025.3.6`, `2026.06.0`,
   `2026.03`), so present the top handful rather than trusting a single guess.

2. **Infer `NEW_VERSION`** if the user did not give it, by bumping the inferred
   `PREVIOUS_TAG`'s last numeric segment (`2025.3.6` → `2025.3.7`). This is a guess and must
   be confirmed.

3. **CONFIRM with the `question` tool** — this gate is mandatory even when both values were
   given explicitly, because a wrong tag silently produces wrong notes:

   - Header: "Release Range"
   - Question: "Generate release notes for `<NEW_VERSION>`, collecting commits in all five
     repos from `<PREVIOUS_TAG>` to `HEAD`. Is that right?"
   - Options:
     - "Yes, that's correct (Recommended)"
     - "Different previous tag" — then ask which (offer the tag list you gathered)
     - "Different new version" — then ask for it
     - "Let me specify both"

   Do not proceed to Phase 2 until the user confirms the final `PREVIOUS_TAG` and
   `NEW_VERSION`. If the confirmed `PREVIOUS_TAG` does not exist in a repo (tags can differ
   per repo), say so and ask how to handle that repo (a different tag, or skip it).

## Phase 2 — Collect commits and filter to the relevant cases

For each repo, get the commit list between the confirmed tag and HEAD:

```
git -C "<ROOT>/<repo>" log --oneline "<PREVIOUS_TAG>..HEAD"
```

Extract every case reference (`SVY-\d+`, `SVYX-\d+`, `SERVOY-\d+`) and deduplicate across all
five repos. A commit may carry several cases; keep each. Also keep the non-case commits that
are genuinely meaningful for an "Other Changes" summary.

**Filter out irrelevant cases and commits right away** — these never reach the review list or
the notes, because they are not user-facing product changes:

- merge commits, pure version bumps ("updated version", "rc2", "bumped the plugin code"),
  and Jenkins/CI/build-file-only changes;
- cases that are purely **internal development, tooling, or release plumbing** — e.g. build
  infrastructure, test-only migrations/speed-ups, `skill`/`opencode`/AI-agent config work,
  repo/URL/port changes, or anything whose only effect is on how Servoy is built or released
  rather than on the product a customer runs.

When in doubt about whether a case is product-relevant, keep it for now — the user reviews the
list in Phase 2.5 and can drop it there. But obvious build/release/internal noise should be
dropped here so the review list stays focused.

Keep descriptions concise — the commit subject, cleaned up (drop trailing markers like `[ai]`,
drop the case key from the description column since it has its own column).

The output of this phase is `RELEVANT_CASES`: the deduplicated set of product-relevant case
keys, each with its summary and originating component/repo.

## Phase 2.5 — Case review & privacy gate (MANDATORY — these notes are published publicly)

The release notes are published on the public GitBook, so **no private/customer-confidential
case may end up in them**. Jira carries a per-issue **security level** that decides this, and
it is machine-readable. But the user also wants a chance to flip cases either way (make a
currently-public case private, or clear a private flag that should be public) *before* the
notes are cut — so this phase is a full review of the whole relevant list, not just the
already-restricted ones.

1. **Fetch the security level and summary for every case in `RELEVANT_CASES`** from Jira
   (load the `servoy-jira` skill for the base URL + `ATLASSIAN_AUTH_BASIC` auth). The field
   is `security`:

   ```
   GET /rest/api/3/issue/{KEY}?fields=security,summary
   ```

   - `security` is **`null`** → no security level set → currently **PUBLIC** (the normal state
     for a public case).
   - `security` is **non-null** (e.g. `security.name == "Private"`) → currently **RESTRICTED**.

2. **Show the user the WHOLE relevant list** as a single table with **clickable links**, so
   they can open any case and adjust its security level in Jira as they see fit — the user may
   want some public ones made private and some private ones made public:

   - Link format: `https://servoy-cloud.atlassian.net/browse/{KEY}`
   - Columns: **Key (linked) | Security (Public / Private) | Summary**.
   - Tell the user plainly: "Here is the full relevant case list with its current security
     level. Go over them and adjust the security level in Jira for any that are wrong. Tell me
     when you're done and I'll re-query."

3. **Wait for the user**, then **re-query the security level for every case** (same GET as
   step 1) so the table reflects their edits — do not reuse the first result.

4. **Show the FINAL table** (same three columns, clickable links) reflecting the re-queried
   state, and confirm it with the user before writing anything.

5. **Split by the final security level:**
   - **Public** cases (`security == null`) → included in the release notes.
   - **Restricted** cases (`security != null`) → **excluded** from the public notes.

   Do not name the excluded restricted cases in any public artifact — only report the count to
   the user in chat. When in doubt, leave a case out.

Never write a restricted case into the public notes. The final published set is exactly the
cases that are `security == null` in the re-queried Phase 2.5 table.

## Phase 3 — Dependency updates

Diff the dependency-bearing files between the tag and HEAD and record real version changes or
newly added dependencies (skip pure formatting/whitespace, skip pure project-version bumps):

- `servoy-extensions/com.servoy.extensions/pom.xml`
- All `pom.xml` files in `servoy-eclipse`:

  ```
  git -C "<ROOT>/servoy-eclipse" diff "<PREVIOUS_TAG>..HEAD" -- "*/pom.xml"
  ```

- `servoy-eclipse/launch_targets/com.servoy.eclipse.target.target`

Record each change as **Dependency | From | To**, grouped per repo/file.

## Phase 4 — Write into the GitBook major-release document

Release notes are **not** kept in the servoy-eclipse repo. They live in the separate **GitBook
repository**, one Markdown file per **major release**, at:

```
<GITBOOK_ROOT>/release-notes/<major>.md      e.g. release-notes/2026.09.md
```

Locate `<GITBOOK_ROOT>` — the `gitbook` checkout, usually a sibling of the product repos
(e.g. `C:\Users\jcomp\git\gitbook`). If you cannot find it, ask the user for its path. The
`<major>` is the marketing version the release belongs to (`2026.09`), derived from
`NEW_VERSION` — an RC like `2026.9_RC2` belongs to the `2026.09` document.

**Two cases:**

### A. The major-release file already exists (a later RC of an existing major)

This is the common case: RC1 (or the final) already produced `release-notes/<major>.md` with
the big prose sections (headline features, Security Hardening, Platform & Infrastructure, the
themed lists, and per-RC "All Cases" tables). You are **adding this RC's delta**, not
regenerating the file:

1. **Read the existing file first** and match its structure and heading style exactly.
2. **Generic / thematic improvements** (things worth describing in prose — a dependency bump,
   a signing change, a cross-cutting improvement) go into the relevant existing themed section,
   or a new `## <Theme>` section if none fits. This is where non-case build/tooling notes that
   ARE worth mentioning to customers can go, phrased generically.
3. **The RC's cases** go into a new **`## <major> RC<n> — All Cases`** table appended after the
   previous RC's table, matching the exact column style already in the file (typically
   **Components | Key | Summary**, every Key and Summary a clickable
   `https://servoy-cloud.atlassian.net/browse/{KEY}` link, Components linked to the
   project+component JQL as in the existing rows).
4. **Do not repeat a case** that already appears elsewhere in the document (an earlier RC table
   or a themed list). Check before adding.
5. Only the **public** cases from the final Phase 2.5 table are included.

### B. First RC of a brand-new major (the file does not exist yet)

Create `release-notes/<major>.md`. Match the structure of the most recent existing major file
(read the newest `release-notes/*.md` as a template): a title (`# Servoy <major> — Release
Notes`), a "Changes since the **<previous major>** release." line, the headline/prose sections
for the big features, themed sections where they help, and a first
`## <major> RC1 — All Cases` table. Again, public cases only.

In both cases, use the **Dependency Updates** findings from Phase 3 as prose (a themed section
or bullet), not as a raw table, unless the existing file uses a table for them.

## Phase 5 — Report

Tell the user the GitBook file written (created or appended), which RC table was added, the
counts (public cases published, restricted cases excluded, dependency/prose notes added), and
the confirmed range. **Do not commit or push** — the GitBook repo is the user's to commit. If
the user asks to commit, follow that repo's convention and never push without explicit
approval.

## Notes

- If a repo has no commits in the range, say so rather than omitting it silently.
- If `git` is unavailable or a repo is missing, stop and report which one — the notes would be
  incomplete otherwise.
- The output lives in the **GitBook repo**, one file per major release — never write release
  notes back into the servoy-eclipse repo.
- This skill replaces the old copy-paste `release_notes_prompt.md` that used to live in
  `servoy-eclipse/release_notes/` — it is the single source of truth for release-notes
  generation now.
