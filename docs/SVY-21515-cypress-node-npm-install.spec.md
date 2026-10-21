# Spec: SVY-21515 — MCP: Cypress should be using node/npm settings of servoy.ini and install automatically

Triage: `docs/SVY-21515-triage.md` (verdict PROCEED, approved approach 1: fix `FormSpecRunner` in place).

## 1. Goal

Make the Cypress E2E / form-spec runner behave like the rest of Servoy Developer's node tooling. It must resolve node, npm and pnpm from the same place the NG client build does (`-Dservoy.nodePath`, `-Dservoy.npmPath`, `-Dservoy.pnpmPath`, `-Dservoy.jsRuntime=pnpm`, set via `servoy.ini` or environment). It must also guarantee that the Cypress **binary** (not just the npm package) is present before `cypress run`, installing it automatically when missing. Cypress's binary cache is kept under the workspace `.metadata` instead of the user-global cache. Failures must be reported with clear, actionable messages.

## 2. Background

### 2.1 Where the code lives

All execution is in `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecRunner.java`. It was moved from Servoy-Copilot in `f51f46bf4` (SVY-21296). Servoy-Copilot's `FormPreviewService` (`Servoy-Copilot/bundles/com.servoy.eclipse.developer.mcp/.../services/FormPreviewService.java:193-255`) still calls these public members: `getCypressDir()`, `ensureCypressInstalled(Path)`, `getNodePath()`, `resolveLocalCypressCmd(Path)`.

Node is provided by `com.servoy.eclipse.ngclient.ui.Activator`:
- `extractNode()` honours `servoy.nodePath` + `servoy.npmPath` (+ `servoy.pnpmPath`). Otherwise it schedules a job that extracts the bundled node/npm (and pnpm in pnpm mode). It does this on every call.
- `getNodePath()` and `getPnpmPath()` are public. There is **no** `getNpmPath()`, and `waitForNodeExtraction()` is package-private.
- `createNPMCommand(...)` waits for extraction, then builds `RunNPMCommand` (`node <npmPath> ...`) or `RunPNPMCommand` (`<pnpmPath> ...`, with `--config.strictDepBuilds=false`). Both put the node directory first on `PATH`.
- `isPnpmMode()` is public static.

### 2.2 Confirmed gaps (from triage)

1. The Cypress binary is never installed or verified. `ensureProjectCypressInstalled` only runs `npm install` when `node_modules/.bin/cypress[.cmd]` is missing. The binary lives in the Cypress cache (`~/Library/Caches/Cypress/<ver>`, `~/.cache/Cypress`, `%LOCALAPPDATA%\Cypress\Cache`), so a present launcher with a missing binary fails at `cypress run`. This is the reporter's symptom.
2. `servoy.npmPath` and pnpm mode are ignored. `getNodePath()` reflects on the private `Activator.nodePath` field (after a throw-away `createNPMCommand(".", --version)` used only to wait). Everything else is derived as `nodePath.getParent()/npm[.cmd]` and `npx[.cmd]`, falling back to whatever is on the system `PATH`.
3. The environment is not isolated. `CYPRESS_CACHE_FOLDER` is not set. `PATH` is the full system `PATH` with the node dir prepended.
4. Smaller defects:
   - The fallback install pins `cypress ^13.0.0`, but the project uses 15.x (the reporter's log shows 15.13.1).
   - `MCP_PLUGIN_DIR = "com.servoy.eclipse.developer.mcp"` is not owned by this plugin. The class Javadoc says `com.servoy.eclipse.copilot`.
   - `extractNode()` re-schedules a job on each `getNodePath()` call, up to three times per run.
   - After `p.waitFor(300|180, SECONDS)`, `exitValue()` is called without checking the `waitFor` result. A timeout throws `IllegalThreadStateException`, which is swallowed and reported as a generic error.
   - **Additional finding while writing this spec:** every `ProcessBuilder` here reads stdout to EOF *before* calling `waitFor(timeout)`. The timeout is therefore never actually enforced, because the read blocks until the process exits. The install timeouts (300 s / 180 s) and the run timeouts (`effectiveTimeout`, default 60 s) are all affected. See §3.8 and Open Questions.

### 2.3 Decisions already made (human approved at triage)

- The Cypress **binary cache moves into `.metadata`** (one-time re-download accepted).
- `scriptsRoot` (`<workspace>/jenkins-custom/e2e-test-scripts`) stays where it is. It is a user-controlled git checkout holding the specs and project `node_modules`. Only the binary cache and the fallback install live under `.metadata`.
- Public `FormSpecRunner` method signatures used by Servoy-Copilot stay source-compatible. See §3.7: they are source-stable but not behaviour-stable without a small coordinated Copilot change.

### 2.4 Related prior work and constraints

- `docs/SVY-21456-ship-pnpm-runtime.spec.md` added the pnpm mode. `RunPNPMCommand` has private static verb translation and cross-volume store-dir logic; it is not reused here (see §3.3).
- SVY-21369 (`5a00325e2`): `getCypressDir()` must stay under the *workspace root's* `.metadata`. `GetCypressDirTest` guards this and must keep passing, with its expected plugin-dir segment updated.
- `FormSpecRunnerTest` asserts by reflection that `ensureCypressInstalled`, `ensureCypressConfig`, `getCypressDir` and `getNodePath` exist on `FormSpecRunner`. Keep all four.

## 3. Design

### 3.1 `ngclient.ui` `Activator` changes (small, additive)

In `com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/Activator.java`:

- Add `public File getNpmPath()` returning `npmPath`, next to `getNodePath()` / `getPnpmPath()`.
- Add `public boolean waitForNodeExtraction(long timeout, TimeUnit unit)` that returns `nodeReady.await(timeout, unit)`. The existing package-private no-arg `waitForNodeExtraction()` stays.
  - Reason: when `isNodeExtractionAndTitaniumBuildDisabled()` is true, `extractNode()` returns early and `nodeReady` is never counted down. The existing `createNPMCommand` would block forever; a bounded wait lets the runner fail with a clear error.
- Make `extractNode()` idempotent: if `nodeReady.getCount() == 0`, or an extraction job is already pending/running, do nothing. Today, every call with unset system properties schedules another "extracting nodejs" job. Use a small `volatile boolean extractionStarted` guard. Keep the existing semantics for `setNodeExtractionAndTitaniumBuildDisabled(false)`, which calls `extractNode()` after the disabled flag flips and must still extract once.
- No other behavioural change to `createNPMCommand`, `RunNPMCommand` or `RunPNPMCommand`.

### 3.2 New pure-logic helper `CypressToolchain` (package `com.servoy.eclipse.cypress.services`)

A small class, so the interesting logic is unit-testable without OSGi, a workspace or a process. Static methods plus an immutable value type:

- `record NodeTools(File node, File npm, File pnpm, boolean pnpmMode)`. It has `File packageManager()` returning `pnpm` in pnpm mode and `npm` otherwise, plus `boolean isUsable()`: node != null, and the package manager is non-null.
- `static Path getPluginMetadataDir(Path workspaceRoot)` returns `<root>/.metadata/.plugins/com.servoy.eclipse.cypress` (see §3.6).
- `static Path getFallbackCypressDir(Path workspaceRoot)` returns `<pluginDir>/cypress`.
- `static Path resolveCacheFolder(Path workspaceRoot, String systemPropertyValue, String envCypressCacheFolder)` resolves the Cypress binary cache folder in this order, ignoring blank values and trimming:
  1. the system property `servoy.cypress.cacheFolder` (`CypressToolchain.CACHE_FOLDER_PROPERTY`), for example `-Dservoy.cypress.cacheFolder=E:\cypress-cache` after `-vmargs` in `servoy.ini`. This follows the same "system property, then environment variable" order as `servoy.nodePath` / `servoy.npmPath`;
  2. the `CYPRESS_CACHE_FOLDER` environment variable, so CI or a user who deliberately set it keeps control;
  3. the default `<pluginDir>/cypress-cache`.
  The two-argument overload `resolveCacheFolder(Path, String)` (environment variable, then default) is kept. `HTTP_PROXY` / `HTTPS_PROXY` stay environment-only and are passed through unchanged.
- `static List<String> buildPackageManagerCommand(NodeTools tools, List<String> args)`:
  - npm mode: `[node, npm, args...]`, matching how `RunNPMCommand` runs `node <npmPath>`. The `npm` file is a node script.
  - pnpm mode: `[pnpm, args..., "--config.strictDepBuilds=false"]` when `args` starts with `install`, matching `RunPNPMCommand`.
  - Paths are absolute/canonical. Never relies on `PATH` lookup for the launcher itself.
- `static List<String> buildCypressCliCommand(File node, Path projectDir, List<String> cypressArgs)` returns `[node, <projectDir>/node_modules/cypress/bin/cypress, args...]`. Running Cypress's JS entry point through the configured node works for npm and pnpm layouts and needs no `.bin` shims or `npx`.
- `static Map<String,String> buildEnvironment(Map<String,String> base, NodeTools tools, Path projectDir, Path nodeModulesDir, Path cacheFolder)` returns a copy of `base` with:
  - `PATH` = node dir, then pnpm dir if different, then `<projectDir>/node_modules/.bin`, then the original `PATH`, in this order. It uses the correct key (`Path` vs `PATH`) by finding the existing key case-insensitively, so it also works on Windows.
  - `NODE_PATH` = `nodeModulesDir`.
  - `CYPRESS_CACHE_FOLDER` = `cacheFolder`.
  - Everything else is inherited unchanged, so proxy variables and `CYPRESS_INSTALL_BINARY` pass through.
- `static String formatFailure(String step, List<String> command, int exitCode, Path cacheFolder, String output)` builds the user-facing error: what step failed, the command, the cache folder, the last ~60 lines of output, and a hint. If the output mentions a proxy/network error, the hint says to check proxy settings. If `CYPRESS_INSTALL_BINARY=0` is in effect, the hint says that skips the download.

### 3.3 `FormSpecRunner` rework

- **Tool resolution.** Add `private NodeTools resolveNodeTools()`:
  1. `Activator a = Activator.getInstance()`; if null, return an empty `NodeTools`.
  2. If `a.getNodePath() == null`, call `a.extractNode()` once.
  3. Wait with `a.waitForNodeExtraction(120, SECONDS)`. If it times out (extraction disabled or hung), return an empty `NodeTools`; callers produce the error from §3.5.
  4. Return `new NodeTools(a.getNodePath(), a.getNpmPath(), a.getPnpmPath(), Activator.isPnpmMode())`.
- **`getNodePath()` (public, kept).** Returns `resolveNodeTools().node()`. No reflection and no throw-away `createNPMCommand` call. It is still null-safe.
- **Process helper.** Add `private ProcessResult runProcess(List<String> command, Path workDir, Map<String,String> env, int timeoutSeconds)`:
  - It drains stdout/stderr (merged) on a worker thread, then calls `process.waitFor(timeout)`.
  - On timeout it calls `destroyForcibly()` and returns `timedOut=true`. It never calls `exitValue()` unless `waitFor` returned true.
  - It is used for the install, verify and binary-install steps. See §3.8 for the run steps.
- **Package install.** `ensureProjectCypressInstalled(Path scriptsDir)` and `ensureCypressInstalled(Path cypressDir)` both use `buildPackageManagerCommand(tools, ["install"])` (plus `--no-audit --no-fund` for npm only) and `buildEnvironment(...)`. Both fail with a clear error when `tools.isUsable()` is false (see §3.5).
- **Project install is keyed on the real requirement.** Install deps when `package.json` exists and the launcher is missing, as today. If `package.json` exists but does not list `cypress`, treat it as "no project-managed Cypress" and fall back, as today.
- **Fallback install** (`ensureCypressInstalled`, public, kept):
  - Write `package.json` with `"cypress": "<DEFAULT_CYPRESS_VERSION>"`. The new constant is `^15.13.1`, aligned with the project's 15.x.
  - Reinstall if `node_modules/cypress` is missing *or* its installed major version differs from the constant's major. Read the installed version from `node_modules/cypress/package.json`.
  - After the package install it also ensures the binary (§3.4), so the method keeps its contract: "return null when Cypress is ready to run from `cypressDir`".
- **Command + env building.** `runFormCypressTests(..., timeout, extraArgs)` and `runE2ECypressTests` currently duplicate roughly 60 lines each. Extract one private `CypressInvocation prepareInvocation(Path scriptsRoot)` that returns `{command prefix, projectDir, env}` or an error string. The flow is:
  1. Project install check.
  2. Choose the local launcher, otherwise the fallback dir.
  3. Ensure the binary.
  4. Build the command prefix. If the project-local launcher exists, use `localCypressCmd run` (unchanged behaviour). If the fallback dir is used, use `buildCypressCliCommand(node, cypressDir, ["run"])`, which replaces `npx cypress run`.
  5. Build the env.
  - Both public run methods then append `--spec`, `--config-file`, `--config`, `--headed` and the extra args exactly as today, and run the process.
  - `ensureCypressConfig(...)` is unchanged.
- **Fallback and `NODE_PATH`.** When the fallback dir is used, `getCypressDir().resolve("node_modules")` is used, as today.

### 3.4 Ensure the Cypress binary

New `public String ensureCypressBinary(Path projectDir)` on `FormSpecRunner`. It returns null on success, or an error string. It requires `projectDir/node_modules/cypress/bin/cypress` to exist; otherwise it returns a clear "Cypress package not installed in …" error.

1. **Verify:** run `buildCypressCliCommand(node, projectDir, ["verify"])` with the §3.2 env, in `projectDir`. Timeout 120 s. Exit 0 means done.
2. **Install:** otherwise run `… cypress install` with the same env. Timeout 600 s. It logs progress to the Developer log and to the NG console the way the other steps log, since the binary is about 250 MB. The console output is whatever `runProcess` captured.
3. **Verify again.** If it still fails, return an error. When `cypress install` itself exited non-zero the error is `formatFailure("Cypress binary install", ...)`. When `install` exited 0 but `verify` still fails (seen when the download is silently blocked, for example by an unreachable proxy), the error is `CypressToolchain.formatBinaryNotInstalled(...)`: it says "Cypress binary install did not produce a usable binary", lists the install and verify commands, the cache folder and the output of both commands, and adds a proxy hint when `HTTP_PROXY` / `HTTPS_PROXY` is set for the run (only the variable names are printed, never the values, which can contain credentials) or the output mentions a network problem.
4. A static, synchronized, in-memory set records `(projectDir, cacheFolder, packageVersion)` tuples already verified in this Developer session. Repeat runs skip steps 1–3. A failed run does not add to the set. The set is never cleared during a session; a Developer restart resets it.

Because the env is shared with the run step (§3.3), the binary that `verify` / `install` places in `CYPRESS_CACHE_FOLDER` is exactly where `cypress run` looks.

pnpm mode: pnpm does not run dependency `postinstall` scripts unless they are explicitly allowed. Confirmed with pnpm 12.11.2: `pnpm install` printed `Ignored build scripts: cypress@15.13.1`, the binary was not downloaded, and the ensure step then downloaded it. This is the reason the explicit ensure step is required, rather than relying on the package manager to trigger the Cypress download.

Install lock: `ensureProjectCypressInstalled`, `ensureCypressInstalled` and `ensureCypressBinary` each run inside one static `INSTALL_LOCK` (the public methods delegate to private `doEnsure...` methods). Parallel runs queue behind the first one, which installs; the others then find the binary in the verified set and skip. Without the lock several runs started `cypress install` into the same cache folder at once. The lock covers only the install steps, not the Cypress run itself; parallel runs of the same form can still interfere through `FormPreviewNGClient.shutdownExisting()` (pre-existing behaviour, out of scope).

### 3.5 Error handling and messages

All errors keep the existing `"Error: ..."` string contract, because callers and tests parse for the prefix.

- No Node: `Error: Node.js is not available (servoy.nodePath/servoy.npmPath not set and the bundled Node.js was not extracted). Check servoy.ini or the Developer log.`
- pnpm mode without pnpm: `Error: servoy.jsRuntime=pnpm is set but no pnpm binary is available.`
- Install or binary failure: `formatFailure(...)` as in §3.2.
- `waitFor` timeout: `Error: <step> timed out after N seconds.` (no `exitValue()` access).
- Replace the remaining `System.out.println` / `System.err.println` in these methods with `ServoyLog.logInfo` / `logWarning` (the class already imports `ServoyLog`).

### 3.6 Plugin ownership of `.metadata` directory

- Replace `MCP_PLUGIN_DIR = "com.servoy.eclipse.developer.mcp"` with the cypress plugin's own id (`com.servoy.eclipse.cypress.Activator.PLUGIN_ID`, a compile-time constant), routed through `CypressToolchain.getPluginMetadataDir`.
- `getCypressDir()` and `getCypressDir(Path)` keep their signatures and return `<root>/.metadata/.plugins/com.servoy.eclipse.cypress/cypress`.
- Fix the class Javadoc, which still references `com.servoy.eclipse.copilot`.
- The cache lives in `<root>/.metadata/.plugins/com.servoy.eclipse.cypress/cypress-cache`.
- Existing installs under the old `com.servoy.eclipse.developer.mcp/cypress` directory are left in place. They are never read again. No migration or cleanup is done. This is mentioned in the Open Questions.

### 3.7 Servoy-Copilot `FormPreviewService` (separate repo, coordinated change)

The method signatures stay source-compatible. Behaviourally, Copilot launches its own `ProcessBuilder` for screenshots (`FormPreviewService.java:249-255`) without `CYPRESS_CACHE_FOLDER`. After this change the binary is installed in the `.metadata` cache, so that process would look in the user-global cache and fail with "binary missing", which would be a regression.

Add a small public API on `FormSpecRunner`: `public void configureCypressEnvironment(ProcessBuilder pb, Path projectDir)`. It applies §3.2's `buildEnvironment` to `pb.environment()`. This gives Copilot a single call that covers PATH, NODE_PATH and the cache folder.

In Servoy-Copilot, make a minimal change in `FormPreviewService`:
- Call `specRunner.configureCypressEnvironment(pb, cypressDir)` instead of hand-setting `PATH` / `NODE_PATH`.
- Optionally drop the `npx.cmd` fallback branch in favour of `buildCypressCliCommand`.

This must land together with the servoy-eclipse change. It is called out in the implementation plan and acceptance criteria.

Status: the Copilot edit is made in `FormPreviewService` (the hand-built `PATH` / `NODE_PATH` lines are replaced by `specRunner.configureCypressEnvironment(pb, cypressDir)`), but it has not been run yet and is not committed. Servoy-Copilot builds against `lts_2026`, so the new `configureCypressEnvironment` method must be cherry-picked to `lts_2026` first, or the Copilot bundle will not compile there.

### 3.8 Run-phase timeout (deliberately conservative)

The run phases keep their current blocking read, with `waitFor` result handling fixed (no `exitValue()` unless the process actually exited). They do not start enforcing `effectiveTimeout` as a hard kill, because that would change the behaviour of every existing form test: the default 60 s currently never kills a long Cypress run, and real runs routinely exceed 60 s including Cypress start-up. Only the new/changed install, verify and binary-install steps use the enforcing `runProcess` helper. Whether to enforce the run timeout is an open question for the owner.

### 3.9 Git history

- `f51f46bf4` (SVY-21296) moved the runner and introduced the reflective `getNodePath`, the npx/npm derivation from `nodePath.getParent()`, and the stale `MCP_PLUGIN_DIR`.
- `5a00325e2` (SVY-21369) fixed `getCypressDir()` creating `.metadata` outside the workspace (guarded by `GetCypressDirTest`).
- `ab87f17f5`, `961079971`, `9149bbb68` (SVY-21323) and `2e14753d9`, `8275d4aed`, `e10807cf0` (SVY-21174) touched artifact handling, form preview shutdown and run-all discovery. They did not touch the install or node/npm resolution, so no constraint comes from them.
- `Activator.extractNode()` support for `servoy.nodePath` / `servoy.npmPath` predates the Cypress work; SVY-21456 added the pnpm path (`docs/SVY-21456-ship-pnpm-runtime.spec.md`).
- No existing spec in `docs/` covers Cypress node/npm resolution.

## 4. Implementation plan

1. **`com.servoy.eclipse.ngclient.ui` `Activator`**
   - Add `getNpmPath()`.
   - Add `waitForNodeExtraction(long, TimeUnit)` (public, bounded).
   - Make `extractNode()` idempotent with an `extractionStarted` guard. Keep `setNodeExtractionAndTitaniumBuildDisabled(false)` behaviour.
2. **Create `CypressToolchain.java`** (in `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/`) with `NodeTools`, the directory/cache resolvers, `buildPackageManagerCommand`, `buildCypressCliCommand`, `buildEnvironment` and `formatFailure`, per §3.2. It must not depend on OSGi, SWT or the workspace.
3. **Refactor `FormSpecRunner`**
   - Add `resolveNodeTools()`. Rewrite `getNodePath()` with no reflection.
   - Add the `runProcess` helper (drain thread, `waitFor` result check, `destroyForcibly` on timeout).
   - Rewrite `ensureProjectCypressInstalled` and `ensureCypressInstalled` to use the toolchain + helper. Pin the fallback version via `DEFAULT_CYPRESS_VERSION = "^15.13.1"` and add the major-version reinstall check.
   - Add `ensureCypressBinary(Path)` plus the per-session verified set.
   - Extract `prepareInvocation(...)` and use it in both `runFormCypressTests` and `runE2ECypressTests`. Use `buildCypressCliCommand` for the fallback instead of `npx`.
   - Add `configureCypressEnvironment(ProcessBuilder, Path)` (public).
   - Replace `MCP_PLUGIN_DIR` with the cypress plugin id. Fix the class Javadoc.
   - Fix the post-`waitFor` `exitValue()` handling in the run methods. Replace `System.out` / `System.err` with `ServoyLog`.
   - Keep these signatures unchanged: `getCypressDir()`, `getCypressDir(Path)`, `getNodePath()`, `ensureCypressInstalled(Path)`, `resolveLocalCypressCmd(Path)`, both `runFormCypressTests` overloads, `runE2ECypressTests`, `cancel()`.
4. **Tests** (`com.servoy.eclipse.cypress.tests`, JUnit 5 as in the neighbouring tests)
   - Update `GetCypressDirTest`: the expected segment changes from `com.servoy.eclipse.developer.mcp` to `com.servoy.eclipse.cypress`, in `endsWithExpectedSuffix` and `correctStructureForVariousRoots`. The SVY-21369 regression test stays as is.
   - Add `CypressToolchainTest` (see §5 for the cases).
   - `FormSpecRunnerTest` should still pass unchanged (reflective method-existence checks). Add `ensureCypressBinary` and `configureCypressEnvironment` existence checks there.
5. **Servoy-Copilot (separate repo and commit):** update `FormPreviewService` per §3.7 to call `configureCypressEnvironment`. Land with, or immediately after, step 3.
6. **Docs:** add an `SVY-21515` entry to the `## Testing` section of `AGENTS.md`, following the existing convention (test class, what it asserts, how it is run).
7. **Verify:** build both bundles (`getCompilationErrors`, organize imports, format), run `CypressToolchainTest`, `GetCypressDirTest` and `FormSpecRunnerTest`, then the manual scenario in §5.

## 5. Acceptance criteria

Automated (pure logic, `CypressToolchainTest`, no processes launched):
- [ ] `resolveCacheFolder`: with a blank or null override, returns `<root>/.metadata/.plugins/com.servoy.eclipse.cypress/cypress-cache`, with no `..` and under the workspace root. With a non-blank override, returns exactly that path.
- [ ] `getFallbackCypressDir` / `getPluginMetadataDir` are under `<root>/.metadata/.plugins/com.servoy.eclipse.cypress` for several roots, including a Windows-style root.
- [ ] `buildPackageManagerCommand` in npm mode returns `[node, npm, install, ...]` using the *configured* npm file. Its parent directory need not equal node's parent, which proves `servoy.npmPath` is respected.
- [ ] `buildPackageManagerCommand` in pnpm mode returns `[pnpm, install, --config.strictDepBuilds=false]` and does not contain the node path.
- [ ] `buildCypressCliCommand` returns `[node, <projectDir>/node_modules/cypress/bin/cypress, run, ...]`.
- [ ] `buildEnvironment`:
  - sets `CYPRESS_CACHE_FOLDER` and `NODE_PATH`;
  - puts the node dir first, then (when different) the pnpm dir, then `node_modules/.bin`, then the original `PATH`;
  - uses the existing key spelling (`Path` / `PATH`);
  - leaves other variables (for example `HTTPS_PROXY`, `CYPRESS_INSTALL_BINARY`) untouched;
  - does not mutate the input map.
- [ ] `NodeTools.isUsable()` is false when node is null, when npm is null in npm mode, and when pnpm is null in pnpm mode.
- [ ] `formatFailure` includes the step name, the command, the cache folder, the tail of the output, and the proxy hint when the output contains a network or proxy error.
- [ ] `GetCypressDirTest` and `FormSpecRunnerTest` pass.

Behavioural (manual, requires a Developer with a workspace):
- [ ] With `-Dservoy.nodePath` and `-Dservoy.npmPath` pointing to non-bundled installs, install and run use those, and the command log shows no use of a system `npm` / `npx`.
- [ ] With a project `e2e-test-scripts` that has `node_modules` and a `cypress` launcher but an empty Cypress cache, `runE2ECypressTests` downloads the binary into `.metadata/.plugins/com.servoy.eclipse.cypress/cypress-cache` and then runs the spec. No manual `npx cypress install` is needed. This is the reporter's scenario.
- [ ] On the second run, the verify step is skipped (session cache).
- [ ] A blocked network produces an `Error:` message naming the step, the command, the cache folder and the proxy hint. It does not produce a generic "Error running spec" or an `IllegalThreadStateException`.
- [ ] No project `package.json`: the fallback install lands in `.metadata/.plugins/com.servoy.eclipse.cypress/cypress` with a 15.x Cypress, and the run uses `node …/cypress/bin/cypress run`.
- [ ] `servoy.jsRuntime=pnpm` with the bundled pnpm: install and ensure-binary work end to end.
- [ ] Servoy-Copilot screenshots (`FormPreviewService`) still work after step 5, using the same cache folder.
- [ ] No new Spotbugs errors of the two highest severities in modified code, and no `System.out` / `System.err` left in the touched methods.

## 6. Out of scope

- Moving `jenkins-custom/e2e-test-scripts`, its `node_modules` or the specs into `.metadata`.
- Delegating Cypress execution to `RunNPMCommand` / `RunPNPMCommand` jobs (triage approach 3).
- Setting `npm_config_cache` or any other npm cache redirection.
- Cross-volume pnpm store handling (SVY-21456 §3.7.2) for the Cypress installs.
- Enforcing the run-phase test timeout as a hard kill (see Open Questions).
- Cleaning up or migrating the old `com.servoy.eclipse.developer.mcp/cypress` directory.
- Serialising whole Cypress runs of the same form (parallel runs can close each other's form preview client), and the SVY-21456 NG client build passing `--config.store-dir` to `pnpm run` scripts when the workspace and pnpm's store are on different drives (`ng build` rejects the unknown argument). Both were seen during testing and are outside this ticket.
- Cypress/Chromium system-dependency problems (for example missing Linux libraries or Xvfb). They surface through the `verify` output but are not fixed here.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Honour a user-set cache folder instead of always overriding it? | Product / reviewer | answered: yes. The `servoy.cypress.cacheFolder` system property (servoy.ini) wins, then the `CYPRESS_CACHE_FOLDER` environment variable, then the `.metadata` default |
| The run-phase timeout (default 60 s) is not enforced today, because stdout is read to EOF before `waitFor`. Enforcing it would kill runs that currently succeed in more than 60 s. Keep as is, raise the default, or enforce and document? | Owner of the Cypress runner | open |
| Servoy-Copilot `FormPreviewService` needs the coordinated change in §3.7 or its screenshots will look in the wrong cache. Who lands it, and in which release? | Servoy-Copilot maintainers | open |
| The fallback Cypress version constant (`^15.13.1`) is taken from the reporter's log, because `jenkins-custom/e2e-test-scripts` is not in this workspace. Confirm the project's real version and the policy for bumping it. | Reporter / test-scripts owner | open |
| Does pnpm skip the Cypress postinstall by default? | Implementer | answered: yes, seen with pnpm 12.11.2 (`Ignored build scripts: cypress`); the ensure-binary step downloaded the binary |
| Old `.metadata/.plugins/com.servoy.eclipse.developer.mcp/cypress` directories stay on disk after the move. Is a one-time cleanup wanted? | Product | open |
