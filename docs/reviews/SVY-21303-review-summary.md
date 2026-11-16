# Peer Review Summary — SVY-21303 (third round)

**Risk: ELEVATED** — `gitAdd`/`gitReset` with `projectName` still stage or unstage nothing, and still answer "Added:", when given a repository-relative path of a file outside the project (for example `docs/<KEY>.spec.md`); the new tool text says that cannot happen.

**Reviewed scope:** Servoy-Copilot `master` `3cf5930` (GitService, ServoyGitServer, assistai-sync doc, assistai-contribute skill, two test classes). servoy-eclipse: no new code since round 2. Fix Version 2026.12, both repos on `master` (release-target check: match). Earlier rounds: round 1 (2026-11-07, markdown skills only), round 2 (2026-11-13, `d0f8207^..10e9f92` plus servoy-eclipse `6e7cb73ba4`).

## Round-2 follow-ups after round 3

| # | Item | Status |
|---|---|---|
| 1 | read tools narrowed to the project | Fixed |
| 2 | `gitStatus` repo-relative vs `gitAdd` prefix; `gitAdd "."` | Partly: in-project paths work in both forms; out-of-project repo paths still prefixed; tool-level `gitAdd` text still says "stage all changes" while the param text says "whole project" |
| 3 | restrict `remote` to `gitRemoteList` names | Not addressed (author left it to the reviewer; matches upstream) |
| 4 | ledger SHAs and counts | Partly: new SHAs and log count right; rows for parts 1 to 3 still say `resolveProjectPaths`, `d6e62c7`, "18 → 26 → 33" |
| 5 | `applyPatchToIndex` | Partly: lock leak fixed; rollback through `index.lock` untouched |
| 6 | `gitRemoteList` credential redaction | Partly: fetch/pull/push failure text still carries the remote URL; password-only form not covered on the real code path |
| 7 | `pullIn` lock across network / conflicted index / `replaceInFile` guard | Partly: 60 s timeout added (per-I/O, not a total deadline); the other two untouched |
| 8 | slash commands, stale global copies, `--title` | Partly: `--title` single-quoted, breaks on a title with an apostrophe |
| 9 | follow-up cases SVY-21784/85/86 | Unchanged, still open |

## Manual test plan

Use a scratch clone for anything that stages, resets or fetches. Servoy-Copilot is a nested layout; use project `com.servoy.eclipse.developer.mcp`.

**Verifying the fix**
1. Change `bundles/com.servoy.eclipse.developer.mcp/src/<file>`, `docs/x.md` and one file under `tests/com.servoy.eclipse.developer.mcp.tests`.
   a. `gitStatus(projectName=<main bundle>)`, `gitAdd(projectName=<main bundle>, filePattern="docs/x.md")`, `gitStatus`. Expect staged, or an error. Still unstaged with "Added:" confirms the High finding. Repeat with the `tests/...` path and with `gitReset`.
   b. `gitAdd` with the in-project path exactly as `gitStatus` printed it, then the project-relative form (`src/...`). Both should stage. Same pair for `gitReset`.
   c. `gitAdd(projectName=<main bundle>, ".")` with a modified file under `docs/`. Expect the project only.
2. `gitDiff`, `gitLog`, `gitShow` with `projectName` and no filter: whole repository. With `pathFilter="."`: project only. Then `gitDiff(projectName=<main bundle>, pathFilter="docs/x.md")` and note the output.
3. `gitRemoteList` on remotes `https://user:pw@host/r`, `https://tok@host/r`, `https://:tok@host/r`, `git@host:o/r.git`.
4. `gitFetch` against a remote `https://<token>@host/...` that fails; check whether the token appears in the returned text.
5. `gitStagePatch` against a bare repository fails, and a following `gitAdd` on another repository does not hang.

**Regression checks**
6. Single project at the repository root: `gitAdd "."`, `gitAdd src/Foo.java`, `gitDiff`, `gitLog` behave as before.
7. `gitAdd(repo=<name>, filePattern=<repo-relative path>)` with no `projectName` still works.
8. A sibling-project path (`tests/com.servoy.eclipse.developer.mcp.tests/...`) with `projectName=<main bundle>`: staged or a clear message, not "Added:" with nothing staged.
9. `gitFetch` against a firewalled (DROP) remote returns "FAILED" after about 60 s; `gitPull` over SSH with a passphrase prompt left open for more than 60 s.
10. Windows: `gitAdd` with `bundles\x\src\Foo.java` and with a leading `/src/Foo.java`.
11. Contribute skill: a PR title with an apostrophe, then one with `"`, under bash and PowerShell.

**Automated checks:** plain `GitServiceToolsTest` (50/50 green in the review run); `eclipse-pde runJUnitPluginTests` with `PDEIntegrationTestsBaseCopy-release.launch`, package `integrationpde` (not run by the reviewer; author reports `GitNestedProject` 9/9, `ServoyGitServer` 16/16, `ServoyCoderServer` 27/27); Spotbugs in the IDE.

## Possible improvements / follow-ups

1. Decide what `gitAdd`/`gitReset` (and the read tools) do with `projectName` plus a repository path outside the project: accept it, reject it, or report "0 paths matched" instead of "Added:". Upstream's `gitStatus` returns both `filePath` (project-relative) and `repoPath`; the port prints repo-relative only.
2. Make the tool-level `gitAdd` description and the param text agree, and say which call the `sdd.md` commands in both repos should use to stage a `docs/` spec in a nested layout.
3. Redact remote URLs in the `fetchIn`/`pullIn`/`pushIn` failure text with the same redactor as `gitRemoteList`, and run the redactor on the configured raw URL (not `URIish.toString()`), with a test through `renderRemoteList`.
4. Decide on restricting `remote` to `gitRemoteList` names (needs a ledger row, since upstream does not).
5. The 60 s timeout is per I/O call; consider whether a total deadline is needed.
6. Fix the stale rows in `docs/assistai-sync.md` (`resolveProjectPaths`, `d6e62c7`, 18 → 26 → 33) and the handover file; record the `prefixOnce` divergence under "Known divergences".
7. Roll the index back through `index.lock` in `applyPatchToIndex`; consider covering `WorkspaceService.replaceInFile` and the git write tools with `WriteAccessValidator`.
8. Write the PR title to a file in the contribute skill instead of single quoting it.
9. Open follow-up cases: SVY-21784, SVY-21785, SVY-21786.

Reviewed: Servoy-Copilot `master` `3cf5930`; servoy-eclipse `master` (no new code).
