---
name: servoy-component-release
description: "Use when releasing a Servoy component package: create a git tag and publish a GitHub release with a changelog and the built zip asset. Triggered by 'release', 'publish', 'github release', 'draft release', 'tag', or '/release'."
---

# Servoy Component Release Pipeline

You are the **orchestrator** for releasing a Servoy component package. This skill tags the repository, builds the release zip, generates a changelog from git commits, and publishes a GitHub release using the GitHub CLI (`gh`).

## Design Principles

- **Draft-first**: The default action creates a *draft* release — nothing is tagged or published until a human explicitly publishes. This is fully reversible.
- **State-aware**: Always detect what already exists at the target tag before acting, and tell the user what will happen.
- **Confirm before irreversible steps**: Never create/push a tag or publish a release without explicit user confirmation.
- **Never overwrite published assets**: A release that is already published is a hard stop.
- **Updatable**: When tooling or conventions change, update the relevant phase section.

## Prerequisites

Before this skill can run, the following must be available:

1. **GitHub CLI (`gh`)** installed and authenticated:
   - Install: use the OS package manager (e.g. `sudo apt install gh`, `brew install gh`) or https://cli.github.com. **Do not attempt to install it silently** — if it is missing, stop and instruct the user.
   - Authenticate: `gh auth login`, or set `GH_TOKEN` / `GITHUB_TOKEN` with `repo` scope.
2. **git** — used for tags and the changelog.

`gh` creates the release, uploads the built zip as an asset, and (on publish) creates the tag. GitHub automatically attaches the two source archives (`.zip` / `.tar.gz`), so the only asset this skill uploads is the built component zip.

---

## Command forms

| Command | Action |
|---------|--------|
| `/release` | Build → changelog → create a **draft** release (title `<Bundle-Name> <version>`). No tag pushed. |
| `/release publish` | Detect state at the target tag → tell the user + confirm → promote an existing draft, OR run a full publish if none exists, OR hard-stop if already published → then offer a version bump. |

The version is **always** read from the 3 version files. It is never passed as an argument.

---

## Phase 0 — Preflight & version confirmation

1. **Verify `gh`**:
   - `gh --version` — if not found, stop and tell the user to install the GitHub CLI.
   - `gh auth status` — if not authenticated, stop and tell the user to run `gh auth login` (or set `GH_TOKEN`/`GITHUB_TOKEN` with `repo` scope).

2. **Detect the repository** from the git remote:
   ```bash
   git remote get-url origin
   ```
   Derive `owner/repo` (e.g. `Servoy/aggridcomponents`).

3. **Read the version from all three files and verify they match:**
   - `package.json` → `.version`
   - `META-INF/MANIFEST.MF` → `Bundle-Version` (always dot-separated numbers, e.g. `2026.9.0`)
   - `projects/*/package.json` → `.version` (the library package.json)

   If any of the three disagree, **report the mismatch and stop** — do not attempt to reconcile automatically.

4. **Read `Bundle-Name`** from `META-INF/MANIFEST.MF` — used for the release title.

5. **Compute:**
   - Tag: `v<version>` (e.g. `v2026.9.0`)
   - Title: `<Bundle-Name> <version>` (e.g. `Servoy NG-Grids 2026.9.0`)

6. **Ask the user to confirm the version is correct** before continuing.

7. **Confirm the working tree** is clean, on the correct release branch, and pushed to the remote. If there are uncommitted changes, report them and let the user decide.

---

## Phase 1 — Build the release zip

Each Servoy component repo exposes an `npm run build` that produces the release zip.

1. Run the build (detected at runtime — do not hardcode a path):
   ```bash
   npm run build
   ```
2. Locate the produced `.zip` (the single asset this skill uploads). If no zip is produced, **stop and report** — do not create a release without the asset.

> Skipped when Phase 3 (publish) is promoting an existing draft — that draft already has its asset.

---

## Phase 2 — Changelog table from git commits

The changelog is built from git commits (subject + body) — no Jira lookup. All real-work commits are included (keyed and keyless); only version-maintenance / release-plumbing commits are filtered out.

### Detect the start tag

The changelog covers commits since the previous release tag. The tag history is mixed, so detect the start tag robustly:

1. List all tags: `git tag`.
2. **Normalize** each candidate: strip a leading `v`. **Ignore any tag containing `_`** (underscore tags are noise and must be excluded entirely).
3. Find the immediate predecessor of the new version by decrementing the last numeric segment (e.g. new `2026.9.3` → look for `2026.9.2`), matching either `v<pred>` or the bare `<pred>` form (old tags were sometimes created without the `v` prefix).
4. If the exact predecessor is not found, fall back to the **highest normalized version tag that sorts below the new version** (version-aware numeric-segment sort, underscore tags excluded).

### Confirm the start tag

**Show the detected start tag to the user and ask for confirmation.** If it is wrong, allow the user to enter a different tag or ref (another tag, a commit SHA, or `HEAD~N`).

### Build the table

1. `git log <startTag>..HEAD --pretty=format:%h%x09%s%x09%b` to get, per commit, the short hash, subject, and body (so a richer description can be written — see step 6).
2. **Filter out version-maintenance / release-plumbing commits.** These are noise and must NOT appear in the changelog, whether or not they carry a Jira key. Drop a commit when its subject matches (case-insensitive) any of:
   - `bump version` / `version bump` / a bare version-only subject like `2026.9.1`
   - `release` used in the plumbing sense (e.g. `prepare release`, `release 2026.9.1`, `tag release`) — do NOT drop a commit that merely fixes a bug *for* a release if it describes real work
   - `publish to SPM` / `publish to spm`
   - merge commits (`Merge branch ...`, `Merge remote-tracking ...`)

   When in doubt about whether a commit is real work or plumbing, keep it and let the user remove it during review (step 7).
3. Extract Jira keys matching `SVY-`, `SVYX-`, or `SERVOY-` (e.g. `SVY-20489`, `SVYX-1127`, `SERVOY-293`) from each remaining subject.
4. **Include every remaining commit**, keyed or not:
   - Commits **with** a Jira key come first, in commit order, with the key in the Case cell.
   - Commits **without** a Jira key are appended **after** the keyed ones, with an empty Case cell (`—`).
5. Format as a Markdown table (bootstrapcomponents style):
   ```markdown
   | Case | Description |
   | --- | --- |
   | [SVY-20489](https://servoy-cloud.atlassian.net/browse/SVY-20489) | Configured basic properties across all core packages and components so they surface correctly in the designer. |
   | [SVYX-1127](https://servoy-cloud.atlassian.net/browse/SVYX-1127) | Added an `onCellFocusGained` event to NG Grid. It fires when a cell receives focus, letting solutions react to keyboard/mouse navigation. |
   | — | Fixed a rendering glitch where the typeahead dropdown caret was misaligned on the first open. |
   ```
   - The Case cell links to `https://servoy-cloud.atlassian.net/browse/<KEY>`, or is `—` for keyless commits.

### Write a clear, concise description (2–4 sentences)

6. The Description cell is **not** the raw commit subject. Write a short, human-readable summary of **what was done**, in **2–4 sentences**:
   - Use the commit subject AND body for context. Prefer plain, user-facing language (what changed and why it matters), not internal implementation jargon.
   - Strip the leading Jira key from the text (it already appears in the Case cell, so it isn't duplicated).
   - Keep it tight: 2 sentences for a simple fix, up to 4 for something with notable behavior/impact. Never a wall of text.
   - Do not invent details that aren't supported by the commit — if the subject/body is thin, a single clear sentence is fine.

7. **Sanitize each Description cell before writing it.** GitHub renders release notes as Markdown, so raw tokens get turned into unintended links or mentions:
   - `@word` (e.g. `@input`) is rendered as a **@-mention of a GitHub user** — if that account exists, it links to it and the release's auto-generated **Contributors** section lists that account. This is how a phantom "input" contributor appears from a subject like `add serveronly tag to spec properties without @input`.
   - `#123` is rendered as an **issue/PR link**.
   - `<...>` may be interpreted as an HTML tag and disappear.

   **Backtick-wrap each offending token so it renders literally.** A backslash escape (`\@input`) is NOT reliable for @-mentions — GitHub still resolves the mention and lists the phantom contributor. Backtick-wrapping is the only method confirmed to neutralize all three token types:
   - `@word` → `` `@input` `` (backtick-wrap). Do not use `\@` — it does not stop the mention. As a plain-text alternative that avoids inline-code styling, replace the leading `@` with the HTML entity `&#64;` (`&#64;input` renders as `@input` with no mention).
   - `#123` → `` `#123` `` (backtick-wrap).
   - `<Foo>` → `` `<Foo>` `` (backtick-wrap).

   Apply this only to the Description text — never to the Case link. The goal is that no description can trigger a GitHub mention, issue link, or HTML interpretation.

8. Write the table to a notes file (e.g. a temp file) and **present it to the user for review/edit** before it is used.

---

## Phase 3 — Create draft (default `/release`)

This is the default action when no `publish` argument is given.

```bash
gh release create v<version> \
  --draft \
  --title "<Bundle-Name> <version>" \
  --notes-file <changelog-file> \
  <path-to-built.zip>
```

- `--draft` means **no git tag is created or pushed yet** — the tag is only created when the draft is published. The draft is fully reversible (delete it and nothing was tagged).
- The built zip is attached as the asset.
- Output the **draft edit URL** returned by `gh` so the user can review and edit it in the GitHub UI.
- **No version bump** on draft creation — the version is not consumed until the release is actually published.

---

## Phase 3 — Publish (`/release publish`)

Query the state of the release at the target tag, **tell the user exactly what will happen, and get confirmation before acting.**

```bash
gh release view v<version>
```

Determine one of three states:

### Draft exists
Tell the user: *"A draft release for `v<version>` exists at `<url>`. Publishing will promote it: it creates and pushes the tag `v<version>` and makes the release public."* Ask for confirmation, then:
```bash
gh release edit v<version> --draft=false
```
Promoting a draft is when the tag `v<version>` is created on GitHub. No rebuild — the draft already has its notes and asset.

### No release exists
Tell the user: *"No release exists for `v<version>`. I'll run a full publish: build the zip, generate the changelog, then create and push the tag `v<version>` and publish immediately."* Ask for confirmation, then run **Phase 1** and **Phase 2**, and:
```bash
gh release create v<version> \
  --title "<Bundle-Name> <version>" \
  --notes-file <changelog-file> \
  <path-to-built.zip>
```
(No `--draft` — this creates the tag and publishes in one step.)

### Published (non-draft) release already exists
Tell the user: *"`v<version>` is already published at `<url>`. Published assets are never overwritten."* → **HARD STOP.** Do nothing further.

> Per project git-safety rules: never create/push a tag or publish a release without the explicit confirmation above.

---

## Phase 4 — Verify

- **Draft:** print the draft edit URL. Note that the tag `v<version>` is **NOT** yet created, and remind the user to publish it (via the GitHub UI or `/release publish`).
- **Published:** print the public release URL. Confirm the tag `v<version>` exists (`gh release view v<version>`) and that the zip asset uploaded successfully.

---

## Phase 5 — Post-publish version bump

Run this **only after a promote or a full publish** — NOT after creating a draft.

1. Tell the user the current released version (e.g. `2026.9.1`).
2. Propose a bump of the **last numeric segment** (e.g. `2026.9.1` → `2026.9.2`, `2026.9.0` → `2026.9.1`).
3. Ask whether to update the version in the 3 files. Let the user accept the proposal or enter a different version.
4. If the user chooses a new version, update all three files to the new version:
   - `package.json` → `version`
   - `META-INF/MANIFEST.MF` → `Bundle-Version`
   - `projects/*/package.json` → `version`
5. Commit the change with subject: `bump version to <newVersion> [ai]`. **Do not push** — leave the commit for the user to review.
6. If the user declines, skip without changes.

---

## Versioning & Future Updates

This skill is designed to be updated as tooling and conventions evolve:

- **`gh` changes**: update the Phase 3 commands if the GitHub CLI release syntax changes.
- **Tag prefix**: tags are always `v<version>`. Change Phase 0 / Phase 2 detection if this convention changes.
- **Version files**: if a new version-bearing file is added to component repos, add it to Phase 0 detection and Phase 5 bump.
- **Changelog format**: the table format mirrors the Servoy component release notes. Update Phase 2 if the release-notes style changes.
