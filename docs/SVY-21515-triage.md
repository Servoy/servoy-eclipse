# Triage Report: SVY-21515

**Verdict:** PROCEED

## Reported problem

Jira summary: "MCP: Cypress should be using node/npm settings of servoy.ini and install automaticly".

The description makes two claims:

1. The Cypress E2E calls should be "more stable and truly hook into the servoy.ini params". Today it "appears to be performing a search on my entire system instead of searching inside the .metadata (where the other npm packages are also installed)".
2. The attached output is an agent transcript, not a Developer log. It says the Cypress binary is missing from `/Users/rene/Library/Caches/Cypress/15.13.1/Cypress.app`. The agent could not run `npx cypress install` because of a shell allowlist, so it asked the user to run `cd jenkins-custom/e2e-test-scripts; npx cypress install && npx cypress run ...` by hand.

The ticket proposes no code-level solution. It states an expected behaviour only: use the servoy.ini node/npm settings, keep everything under `.metadata`, and install Cypress automatically.

There are no attachments, comments, or linked issues. The reporter's wording ("appears to") is not backed by evidence of where the search happens.

## Root-cause assessment

All Cypress execution is in `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecRunner.java`. It was moved there from Servoy-Copilot in commit `f51f46bf4` (SVY-21296, 2026-08-14). The Servoy-Copilot `FormPreviewService` still calls it for screenshots. The code does not fully honour the servoy.ini settings, and it never installs the Cypress **binary**. I found four gaps.

### Gap 1: the Cypress binary is never installed, which is the symptom in the log

`ensureProjectCypressInstalled()` (lines 793-858) runs `npm install` only when `node_modules/.bin/cypress[.cmd]` is missing.

- On the reporter's machine, `jenkins-custom/e2e-test-scripts/node_modules` almost certainly already exists, with the npm package `cypress@15.13.1`. The `.bin/cypress` launcher is therefore present.
- The Cypress **binary** is a separate download. It is fetched by the npm postinstall step into a per-user cache (`~/Library/Caches/Cypress/<version>`, `~/.cache/Cypress`, or `%LOCALAPPDATA%\Cypress\Cache`), not into `node_modules`. If that cache lacks the version (cleared cache, version bump, new user), the launcher exists but the binary does not. `cypress run` then fails with "binary is missing".
- Nothing in `FormSpecRunner` ever runs `cypress install` or `cypress verify`. A grep for `cypress install` in the module returns nothing.
- This matches the reporter's log exactly: `/Users/rene/Library/Caches/Cypress/15.13.1/Cypress.app is missing`.

### Gap 2: servoy.npmPath is ignored

`Activator.extractNode()` (ngclient.ui `Activator.java:145-161`) honours `-Dservoy.nodePath`, `-Dservoy.npmPath`, and `-Dservoy.pnpmPath`, from system properties or environment variables. That is what "servoy.ini params" means in practice.

`FormSpecRunner.getNodePath()` (lines 457-473) does two things:

- It calls `extractNode()`, then `createNPMCommand(new File("."), ["--version"])` only to force a wait for extraction. This creates a throw-away command object.
- It then reads the private `nodePath` field of `Activator` by reflection. `Activator.getNodePath()` is public (line 236) but is not used.

Everything npm-related is then derived from `nodePath.getParent()`: `npm.cmd`, `npm`, `npx.cmd`, and `npx` (lines 322-326, 693-697, 816-820, 886-890).

- `servoy.npmPath` and `servoy.jsRuntime=pnpm` are never consulted.
- `Activator.createNPMCommand` and `RunNPMCommand` handle this correctly. They invoke `node <npmPath>` and add the node directory to PATH.
- If node comes from `servoy.nodePath`, `<nodedir>/npm` may not exist or may differ from the configured `servoy.npmPath`.
- In that case the fallback is whatever `npm` is on the system PATH. That is a likely source of the "searches my entire system" impression.

### Gap 3: the execution environment is not isolated to .metadata

- `runFormCypressTests` and `runE2ECypressTests` do not set `CYPRESS_CACHE_FOLDER`, `npm_config_cache`, or `CYPRESS_INSTALL_BINARY`. The binary therefore goes into, and is searched for in, the user-global cache.
- `PATH` is the full system PATH with the node dir prepended (lines 362-365 and 722-727), so system tools are visible.
- `scriptsRoot` is `<workspaceRoot>/jenkins-custom/e2e-test-scripts`. That is a user-controlled git checkout, not `.metadata`. This is the primary location for specs and `node_modules` and looks intentional.
- Only the fallback install (`getCypressDir()`) lives under `.metadata`.

### Gap 4: smaller inconsistencies in the same area

- The fallback Cypress is pinned to `^13.0.0` (`ensureCypressInstalled`, line 878), while the project uses `15.13.1`. A fallback to the internal install would run a different major version.
- The `MCP_PLUGIN_DIR` constant is still `com.servoy.eclipse.developer.mcp` (line 35). Since the plugin extraction it points to a directory that is not owned by this plugin. The Javadoc says `com.servoy.eclipse.copilot`. This is cosmetic, but any `.metadata` redirection should settle on one owner.
- `extractNode()` schedules a new "extracting nodejs" job on every `getNodePath()` call when the system properties are not set. `getNodePath()` is called up to three times per run.
- `ensureProjectCypressInstalled` runs `npm install` without `--no-audit` and without a custom cache, and it waits for 300 seconds. After the wait it does not check whether `waitFor` returned false before calling `exitValue()`. That would throw `IllegalThreadStateException` on timeout.

I could not reproduce any of this. `D:\Eclipse\MasterWorkspace` has no `jenkins-custom/e2e-test-scripts`, and no Developer log was attached. Gap 1 is confirmed from the code and matches the reporter's transcript. Gap 2 is confirmed from the code. The "searches the entire system" claim is plausible via Gap 2 and Gap 3, but I have no direct evidence of a filesystem search.

## Ticket premise check

- **Problem exists in this project's code: yes.** It is not misuse of a third-party tool. `FormSpecRunner` does not honour `servoy.npmPath` and never installs the Cypress binary.
- **The transcript is evidence of a missing install, not of a system-wide search.** The block in the description is output from an AI agent running in a shell sandbox. It tried `npx cypress install` itself, which suggests the agent went around the MCP tool, or the tool returned a "binary missing" error. Either way, the underlying fact is that the binary was not in the cache and nothing in Developer installed it. That is Gap 1.
- **Proposed approach:** the ticket proposes none. Its expectations (use servoy.ini settings, install automatically, stay within `.metadata`) are reasonable and are mostly achievable with small local changes. One expectation needs a decision. "Inside .metadata" conflicts with the current design, where the spec project and `node_modules` live in the workspace's `jenkins-custom/e2e-test-scripts`. Only the Cypress binary cache, npm cache, and fallback install can sensibly move into `.metadata`.

## Approaches considered

1. **Fix `FormSpecRunner` in place (recommended).**
   - Use `Activator.getInstance().getNodePath()` and add a public `getNpmPath()` / `getPnpmPath()` accessor.
   - Run npm as `node <npmPath>` through `Activator.createNPMCommand(...)`/`RunNPMCommand`, so it respects servoy.npmPath and the pnpm mode.
   - Before `cypress run`, run `cypress install` (or `cypress verify`, then `install` on failure), using the project-local binary or `node <npmPath> exec cypress install`.
   - Set `CYPRESS_CACHE_FOLDER` to a directory under `.metadata/.plugins/<this plugin>/cypress-cache`, and optionally `npm_config_cache`.
   - Refactor the duplicated command-building and env code from `runFormCypressTests` and `runE2ECypressTests` into one helper. The Servoy-Copilot `FormPreviewService` can then reuse it.
   - Pros: small, local to one class, and consistent with how the NG client builds. Fixes the reporter's failure. Cons: a first run downloads about 250 MB and may be slow or blocked on a restricted network, so it needs clear progress and error output. The cache move means users with an existing global cache download Cypress again once.
2. **Only auto-run `cypress install` and leave the rest.**
   - Pros: the smallest change, and it fixes the exact reported failure. Cons: the servoy.npmPath and cache-isolation expectations stay unmet. A user with a custom npm still falls back to whatever is on PATH.
3. **Delegate execution to the NG client build infrastructure** (`RunNPMCommand`, `RunPNPMCommand`), so Cypress is installed into the TiNG node folder.
   - Pros: one runner for all node tooling. Cons: it is a much bigger change. Output streaming, the cancel/timeout semantics, and the 60 s test timeout are different, and it risks regressions in the SVY-21456 pnpm work. It is not needed for this ticket.
4. **No code change needed.**
   - The user could run `npx cypress install` manually, as the agent suggested. This is not acceptable. The ticket asks for automation, and the tool already auto-installs the npm package, so the missing step is a clear gap.

## Recommendation

PROCEED with approach 1. Concretely:

- Add `getNpmPath()` and `getPnpmPath()` accessors to `com.servoy.eclipse.ngclient.ui.Activator`, next to the existing `getNodePath()`. Use `Activator.getNodePath()` instead of reflecting on the private field. Wait for node extraction once, with no throw-away `createNPMCommand` call.
- In `FormSpecRunner`, build every npm/npx/cypress process from the Activator-provided node and npm paths. In pnpm mode, use the bundled pnpm.
- Add an "ensure Cypress binary" step before `cypress run`. Treat a failure as a clear error message that includes the command output and the cache folder used. Honour `CYPRESS_INSTALL_BINARY` and corporate proxy variables by inheriting the environment.
- Set `CYPRESS_CACHE_FOLDER` explicitly under `.metadata`, and use that same folder for the verify step.
- Fix the `waitFor` result check so a timeout does not throw.
- Fix `MCP_PLUGIN_DIR`/`getCypressDir` so it is owned by this plugin. Check that `GetCypressDirTest` and the Servoy-Copilot callers still work. Align the fallback Cypress version with the project's version.
- Tests: `com.servoy.eclipse.cypress.tests` already has `FormSpecRunnerTest` and `GetCypressDirTest`, so the new pure-logic pieces (command and env builder, cache folder resolution) should be unit-testable without launching Cypress.

Decision for the human: should the Cypress binary cache move into `.metadata` (isolated, but a one-time re-download and more disk per workspace), or stay in the user-global cache (shared across workspaces) with only the missing-binary auto-install added? I recommend `.metadata`, as the reporter asked, but it is a product decision.

## Git history findings

- `f51f46bf4` (2026-08-14, SVY-21296, "extract Cypress testing into standalone plugin"): moved `FormSpecRunner` into `com.servoy.eclipse.cypress` and introduced `ensureProjectCypressInstalled`, the reflective `getNodePath`, and the npx/npm derivation from `nodePath.getParent()`.
  - It carried over the stale `MCP_PLUGIN_DIR = "com.servoy.eclipse.developer.mcp"`.
  - `5a00325e2` (SVY-21369) later fixed `getCypressDir()` creating `.metadata` outside the workspace.
- `ab87f17f5`, `961079971`, `9149bbb68` (SVY-21323) and `2e14753d9`, `8275d4aed`, `e10807cf0` (SVY-21174): later fixes to artifact handling, form preview shutdown, and run-all discovery. None touched the install or node/npm resolution logic.
- The `Activator.extractNode()` support for `servoy.nodePath`/`servoy.npmPath` predates the Cypress work. SVY-21456 added the pnpm variant (see `docs/SVY-21456-ship-pnpm-runtime.spec.md`), which `FormSpecRunner` also ignores.
- No existing spec in `docs/` covers Cypress node/npm resolution. `docs/SVY-21516-triage.md` and `docs/SVY-21514-triage.md` concern other Cypress/MCP bugs, not this one.
- Servoy-Copilot `FormPreviewService.java:193-255` has the same pattern: it uses `FormSpecRunner.getNodePath()`, `ensureCypressInstalled`, and `resolveLocalCypressCmd`. A fix should keep those public methods stable, or the Copilot side needs a coordinated change.
