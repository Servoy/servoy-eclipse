# Peer Review Summary — SVY-21303 (fourth round)

**Risk: ELEVATED** — the credential fix is incomplete: failures that are thrown (not returned as text) still reach the model through the root cause of the exception chain (`McpServerFactory.java:266-277`), and `7780329` made the redactor stop masking a password or token that contains `#` or `?`.

**Reviewed scope:** Servoy-Copilot `master` `150c49a` and `7780329` (GitService, ServoyGitServer, WorkspaceService, assistai-sync/handover docs, assistai-contribute skill, tests). servoy-eclipse: no new code (only the round-3 summary `40d6195860`). Fix Version 2026.12, both repos on `master` (release-target check: match). Earlier rounds: round 1 (2026-11-07), round 2 (2026-11-13), round 3 (2026-11-16, `3cf5930`).

## Where the problems come from (AssistAI vs. the Servoy port)

Checked against the AssistAI fork (`eclipse-chatgpt-plugin` `main` `3218e3b`), by reading the code. AssistAI itself was not run.

| Problem | Origin |
|---|---|
| Fetch/pull/push failure text carries the remote URL (token as user name) | **Inherited, and rooted in JGit.** Upstream builds these texts from `e.getMessage()` and JGit puts the URL in it: `TransportException(URIish, String)` and `Transport.open`'s "URI not supported" message strip only the password, never the user name. `https://:token@host` is not parsed as user info at all, so the secret stays in the path. Upstream AssistAI has no redaction. Reported to JGit: https://github.com/eclipse-jgit/jgit/issues/322 (git itself strips user info from the URLs it prints, `transport_anonymize_url`). |
| `gitRemoteList` prints remote URLs unredacted | **Inherited.** Upstream prints `config.getURIs().get(0).toString()`. |
| No timeout on fetch/pull/push, lock held over the network in pull | **Inherited** (upstream has no `setTimeout`); fixed in the port. |
| Redactor misses `#`/`?` in userinfo; thrown failures bypass the redactor | **Servoy-only.** Upstream has no redactor. The thrown-failure bypass sits in Servoy's `McpServerFactory` (not compared with an upstream equivalent). |
| `gitAdd`/`gitReset` "Added:" with nothing staged for a path outside the project | **Port design.** Upstream's `gitStatus` returns both `filePath` (project-relative) and `repoPath`; the port prints repo-relative prose only and had to resolve the ambiguity per call. Recorded as a divergence tied to SVY-21784. |
| Read and write resolvers collapsed into one; `getProject(null)` NPE | **Port errors**, not upstream bugs. |

## Round-3 findings after round 4

| # | Round-3 item | Status |
|---|---|---|
| H | `gitAdd`/`gitReset` with `projectName` and a repo path outside the project says "Added:", stages nothing | Fixed for writes (refused, `GitService.java:963-969`). Partly for reads: a repo-root path that exists only in history (deleted and committed, staged deletion) is not refused and still answers empty / "No changes." |
| Q1 | What should happen; before/after `gitStatus` | Fixed (refuse + report by outcome). No before/after transcript given. |
| Q2 | Tool text contradicts param text; which call for `sdd.md` | Partly. The four `@Tool` texts agree. The four `filePattern` texts still say "both work, a path is never prefixed twice". `sdd.md:232` unchanged in both repos. Read-tool texts do not mention the refusal. |
| Q3 | Fetch/pull/push failure text carries the URL | Partly. Returned-string branches are redacted. Thrown branches are redacted only in the wrapper text; the MCP layer prints the root cause. `fetchIn`'s `"Remote said: "` line is not redacted. |
| Q4 | `renderRemoteList` used `URIish.toString()` | Partly. Now redacts the raw configured URL; `7780329` then broke `#`/`?` in userinfo. |
| Q5 | Stale ledger rows, record the divergence | Partly. History kept, divergence recorded. Per the narrative report: intermediate counts "16 → 24 / 24 → 32", `d6e62c7` in a row, handover "2144/2144", and the `7780329` message cites a non-existent SHA `0fc3ce2` remain. |
| F1 | `index.lock` rollback | Partly. Implemented (`restoreIndexThroughLock`), untested; `exists()` then truncating write, and the error path deletes the lock unconditionally. |
| F2 | `replaceInFile` outside `WriteAccessValidator` | Fixed, untested. |
| F3 | `--title` quoting | Partly. Title now goes through a file; the old "single quotes" paragraph remains in `SKILL.md:287`; bash only. |
| F4-F6 | total deadline over the 60 s timeout; `remote` restriction; conflicted-index diff | Not addressed (author left to the reviewer / not reproduced). |
| F7 | bare repository, read-only file + team provider (round-3 manual items) | Not addressed. |
| F8 | SVY-21784/85/86 | Unchanged, still open. |

## Manual test plan

Use a scratch clone for anything that stages, resets or fetches. Servoy-Copilot is a nested layout; use project `com.servoy.eclipse.developer.mcp`.

**Verifying the fix**
1. Change `docs/x.md` (repo root) and a file under the project.
   a. `gitAdd(projectName=<bundle>, filePattern="docs/x.md")` returns an error naming the project and `repo`; `gitStatus` shows nothing staged.
   b. `gitAdd(repo=<repo>, filePattern="docs/x.md")` with no `projectName` stages it and lists it.
   c. In-project path in both forms (`src/...` and `bundles/<bundle>/src/...`) stages. Same for `gitReset`. A path in neither reading (`src/*.nothing`) answers "Nothing to add", not "Added".
2. `gitDiff`, `gitLog`, `gitShow` with `projectName` and `pathFilter="docs/x.md"` refuse; a project-relative filter still works.
3. Token redaction through the MCP tools, one scratch remote each:
   a. `https://tok@127.0.0.1:9/n/r.git`: `gitFetch` returns "FAILED" without the token.
   b. `https://:tok@127.0.0.1:9/n/r.git` and `foo://tok@127.0.0.1/n/r.git`: check the returned text, the Eclipse log and the transcript for `tok`.
   c. `https://u:p#SECRET@127.0.0.1:9/n/r.git` and the `?` variant: `gitRemoteList`.
   d. A server message in `Remote said:` that contains a URL with credentials, if you can arrange one.
4. `gitStagePatch` with a patch that does not apply: index unchanged, no `index.lock` left. With a pre-created `index.lock`: the call says the patch failed and leaves the lock alone.
5. `searchAndReplace` on a read-only file: clear refusal from `WriteAccessValidator`; with a team provider, check for a dialog on the tool thread.

**Regression checks**
6. Delete and commit a repo-root `docs/a.md`, then `gitLog(projectName=<bundle>, pathFilter="docs/a.md")` and `gitDiff(staged=true, ...)` for a staged deletion. Note whether the answer is empty.
7. Project at the repository root: `gitAdd "."`, `gitAdd src/Foo.java`, `gitDiff`, `gitLog` as before. Stage a file, `gitReset(project, ".")`, check `gitStatus` (the rewritten test pins "Nothing to unstage").
8. Merge conflict, resolve with the incoming version, `gitAdd` it. Check whether the report says "Nothing to add".
9. Windows: `gitAdd` with `bundles\x\src\Foo.java`, with a leading `/src/Foo.java`, and with a path differing only in case.
10. `gitStagePatch` against a bare repository fails and a following `gitAdd` on another repository does not hang.
11. Contribute skill: a PR title with an apostrophe, then one with `"`, under bash. PowerShell is untested.

**Automated checks:** plain JUnit `eclipse-ide runJUnitTests`, project `com.servoy.eclipse.developer.mcp.tests`, package `plainjunit`, `junitVersion` "6" (review run: 2756 passed, 0 failed; `plainjunit.services` 1490/1490). PDE: `GitNestedProjectIntegrationTest` and `ServoyGitServerIntegrationTest` under `PDEIntegrationTestsBaseCopy-release` (not run by the reviewer). Spotbugs in the IDE (not run).

## Possible improvements / follow-ups

1. Make thrown remote failures safe end to end: throw an exception whose whole chain is redacted (or redact the root cause in `McpServerFactory`, and the Eclipse log line that carries the raw chain), pass `fetchIn`'s `"Remote said:"` through the redactor, handle the empty-authority form `https:///:tok@host`, and add a test that drives a credentialed URL through `fetchIn`/`pullIn`/`pushIn` and the MCP unwrap. The URL gets into the message inside JGit, which is filed as https://github.com/eclipse-jgit/jgit/issues/322. A JGit fix would only arrive with a new target platform, so our own redaction stays needed. Keep it as defence in depth once JGit changes, and re-run the four URL shapes from the issue against the new JGit.
2. Fix the `#`/`?` regression in `redactUserInfo` (end the authority by the last `@` before the first `/`), and add tests for both directions (`u:p#x@h` redacted, `h/r?x=a@b` untouched). The three `7780329` tests do not fail if that commit's change is reverted.
3. Decide what reads do with a repo-root path that exists only in history (refuse, search history, or say "0 paths matched").
4. Decide whether `gitReset "."` at a repo-root project should unstage everything (upstream adds no path in that case); the test now pins the no-op.
5. Update the four `filePattern` texts, the read-tool texts, both `sdd.md` files (`:232`) and `skills/sdd-java-eclipse/SKILL.md` for the refusal and the `repo=` call; delete the stale single-quote paragraph in `assistai-contribute/SKILL.md`.
6. Make `indexSnapshot` handle conflict stages; create `index.lock` with CREATE_NEW and delete it only if this call created it; add tests for `restoreIndexThroughLock` and the `replaceInFile` validator call.
7. Clean the docs: ledger counts (16 → 18 → 25 → 32), `d6e62c7`, handover "2144/2144", the non-existent `0fc3ce2`.
8. Reconcile the PDE counts: source has 16 `@Test` in `ServoyGitServerIntegrationTest` and 18 in `GitNestedProjectIntegrationTest`; the commit texts say 15/15 and 19/19.
9. Open by the author's choice: restrict `remote` to `gitRemoteList` names, total deadline over the 60 s per-I/O timeout, conflicted-index `gitDiff`/`gitShow`.
10. **Sync back to AssistAI.** Once items 1 and 2 are fixed and verified, contribute the fixes for code that originated in AssistAI to the upstream repo as a pull request, using `/assistai-contribute`. Candidates: redaction of remote URLs in `listRemotes` and in the fetch/pull/push failure texts (upstream has none), the `setTimeout` on fetch/pull/push, the lock-leak fix in `applyPatchToIndex`, and the "pathspec matched nothing" reporting (JGit's `AddCommand` does not fail on it, so upstream can also answer success with nothing staged). Do not contribute the redactor before items 1 and 2 are fixed, or upstream gets a half-working one. The `prefixOnce`/refusal logic is a Servoy-specific divergence (upstream returns both path forms from `gitStatus`) and stays out of the PR.
11. Open follow-up cases: SVY-21784, SVY-21785, SVY-21786.

Reviewed: Servoy-Copilot `master` `150c49a`, `7780329`; servoy-eclipse `master` (no new code).
