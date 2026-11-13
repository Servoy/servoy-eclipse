# Peer Review Summary — SVY-21303 (second round)

**Risk: HIGH** — `gitAdd`/`gitReset` with `projectName` now prefix the project path onto a path that `gitStatus` still prints repo-relative, so in a nested layout (every project in Servoy-Copilot) the path silently stages nothing while the tool answers "Added: ...".

**Reviewed scope:** Servoy-Copilot `master` `d0f8207^..10e9f92` (GitService, ServoyGitServer, CodeEditingService, WorkspaceService, new WriteAccessValidator, MANIFEST, assistai-sync/contribute skills, docs) plus servoy-eclipse `6e7cb73ba4` (skills removed). First-round commits (markdown only) were reviewed on 2026-11-07. Fix Version 2026.12, both repos on `master`.

## Manual test plan

Use a scratch clone for anything that stages, pushes or resets. Servoy-Copilot is itself a nested layout (projects under `bundles/` and `tests/`); use project `com.servoy.eclipse.developer.mcp`.

**Verifying the fix**
1. Nested `gitAdd`: change one file in `bundles/com.servoy.eclipse.developer.mcp` and one in `tests/com.servoy.eclipse.developer.mcp.tests`. Run `gitStatus`, then `gitAdd(projectName=<main bundle>, filePattern=<path exactly as gitStatus printed it>)`, then `gitStatus` again. Repeat with the project-relative form (`src/...`). Repeat the pair for `gitReset`.
2. Nested `gitDiff`/`gitLog`: call `gitDiff(projectName=<main bundle>)` and `gitDiff(staged="true", projectName=<main bundle>)`; check whether the `tests/` change shows. Compare `gitLog(projectName=...)` with `gitLog(repo=<repo>)`. Compare against a build before `d0f8207`.
3. `gitAdd "."` with `projectName`, with a modified file under `docs/`: is it staged?
4. `replaceStringInFile` on a 10-line file: `endLine=9999` should be refused with the line count; `startLine=0` refused; a valid range succeeds.
5. Remote check on a throwaway repo with no remotes plus an empty bare repo: `gitPush(remote=<absolute path of bare repo>, setUpstream=true)`, then read `branch.<name>.remote` in `.git/config`. Try `gitFetch`/`gitPull` with the same raw path.
6. CRLF search: file with CRLF endings and a known string on line 40; the search tool must report line 40.
7. Slash commands: in an opencode session in Servoy-Copilot try `/assistai-sync` and `/assistai-contribute`; if missing, check the skills load by name.

**Regression checks**
8. `gitStatus` in the nested layout still lists changes from both projects; `gitCommit` after repo-wide staging works.
9. Single project at the repo root: `gitAdd "."`, `gitDiff`, `gitLog` behave as before (empty prefix).
10. `gitStagePatch` with a deliberately broken patch: working-tree change still present, index unchanged.
11. Read-only file via a write tool (e.g. `replaceFileContent`), with and without a team provider; watch for a dialog on a tool thread.
12. Skills on Windows/PowerShell (author says unverified); both skills still hard-code `/tmp/opencode` in bash snippets.

**Automated checks:**
- Plain: eclipse-ide `runJUnitTests`, project `com.servoy.eclipse.developer.mcp.tests`, package `com.servoy.eclipse.developer.mcp.plainjunit`.
- PDE: eclipse-pde `runJUnitPluginTests`, launcher `/com.servoy.eclipse.developer.mcp.tests/PDEIntegrationTestsBaseCopy-release.launch` (or `-lts_2026` if the workspace is on that branch), package `com.servoy.eclipse.developer.mcp.integrationpde`, `junitVersion` 6. The author could not run these locally; they do not cover a nested project.
- Spotbugs (top two severities block): run in the IDE.

## Possible improvements / follow-ups

1. `projectName` with no path filter now narrows `gitLog`/`gitDiff`/`gitShow` to the project folder; upstream AssistAI uses the whole repository. Decide if intended and say so in the tool descriptions.
2. Either make `gitStatus` print project-relative paths (as upstream does) or make `resolveProjectPaths` recognise an already-prefixed path; `gitAdd "."` no longer reaches repo-root files (same root as SVY-21785).
3. Restrict `remote` in `gitFetch`/`gitPull`/`gitPush` to names from `gitRemoteList`; `setUpstream` currently writes any string into `branch.<n>.remote`.
4. Fix the ledger in `docs/assistai-sync.md`: Servoy SHAs `d6e62c7`, `1a9b722`, `3c5b295` do not resolve, one row is still `_pending_` (`2113e54` exists). Tool counts: code shows 16 to 32 `@Tool`s, Jira reply says 18 to 33.
5. Move the snapshot in `applyPatchToIndex` inside the `try` (lock leak on a bare repo); roll back the index through `index.lock`.
6. `gitRemoteList` prints the user part of remote URLs (embedded tokens); redact userinfo.
7. `pullIn` holds the repository lock across the network fetch; `gitDiff`/`gitShow` may fail on a conflicted index after `gitMerge`/`gitRebase`; `WorkspaceService.replaceInFile` is not covered by `WriteAccessValidator`.
8. Slash commands `/assistai-sync` and `/assistai-contribute` are still advertised although `.opencode/commands/assistai-*.md` were deleted; `/install-skills` leaves stale global copies; contribute skill leaves `--title` inline-quoted.
9. Open follow-up cases: SVY-21784, SVY-21785, SVY-21786.

Reviewed: Servoy-Copilot `master` `d0f8207^..10e9f92`; servoy-eclipse `6e7cb73ba4`.
