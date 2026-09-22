---
name: release-notes
description: "Use when generating Servoy LTS/product release notes across the multi-repo checkout (sablo, server, servoy-client, servoy-eclipse, servoy-extensions): collect commits between two tags, build Bug Fixes / Other Changes / Dependency Updates tables and a plain case list, and write RELEASE_NOTES_<version>.md. Triggered by 'release notes', 'generate release notes', 'release notes for 2025.3.7', or '/release-notes'."
---

# Release Notes — Cross-Repo Generator

You generate the release notes for a Servoy product release. The product is made of **five
git repositories** checked out side by side under a common parent directory. This skill
collects everything that changed between the previous release tag and now, across all five,
and writes a single Markdown release-notes document.

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

## Phase 2 — Collect commits (all five repos)

For each repo, get the commit list between the confirmed tag and HEAD:

```
git -C "<ROOT>/<repo>" log --oneline "<PREVIOUS_TAG>..HEAD"
```

Then classify:

1. **Bug fixes & changes** — commits containing a case reference (`SVY-\d+` or `SVYX-\d+`).
   Collect them for a **Bug Fixes** table with columns **Case | Description | Component**
   (Component = the repo the commit came from, or a finer module if obvious from the path).
   A commit may carry several cases; list each case.

2. **Other changes** — commits with no case reference that are still meaningful. **Skip**
   merge commits, version bumps, and Jenkins/CI/build-file-only changes. Collect the rest for
   an **Other Changes** table with columns **Component | Description**.

Keep descriptions concise — the commit subject, cleaned up (drop trailing markers like
`[ai]`, drop the case key from the description column since it has its own column).

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

## Phase 4 — Write the document

Write the full release notes as Markdown to:

```
<ROOT>/servoy-eclipse/release_notes/RELEASE_NOTES_<NEW_VERSION>.md
```

Create the `release_notes` directory if it does not exist. The document contains, in order:

1. A short title/header naming the version.
2. A **Bug Fixes** table (Case | Description | Component).
3. An **Other Changes** table (Component | Description).
4. A **Dependency Updates** section, split per repo/file, each with From/To columns.
5. A plain **Case List** fenced code block listing every `SVY-xxx` and `SVYX-xxx` case
   number (deduplicated, sorted), suitable for pasting into a tracker or changelog.

Look at the most recent existing `RELEASE_NOTES_*.md` in that directory first and match its
structure and heading style, so successive releases stay consistent.

## Phase 5 — Report

Tell the user the path written, the counts (bug fixes, other changes, dependency updates,
distinct cases), and the confirmed range. **Do not commit or push** — writing the file is the
deliverable; committing is the user's call. If the user asks to commit, follow the repo's
commit convention (case key not applicable here; add a trailing `[ai]` marker since the notes
are generated), and never push without explicit approval.

## Notes

- If a repo has no commits in the range, say so rather than omitting it silently.
- If `git` is unavailable or a repo is missing, stop and report which one — the notes would be
  incomplete otherwise.
- This skill replaces the old copy-paste `release_notes_prompt.md` that used to live in
  `servoy-eclipse/release_notes/` — it is the single source of truth for release-notes
  generation now.
