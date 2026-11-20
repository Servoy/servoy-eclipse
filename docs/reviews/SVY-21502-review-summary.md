# Review Summary — SVY-21502: pnpm: per-solution approval of dependency build scripts (persist in solution, use in WAR export)

**Risk: ELEVATED** — In pnpm mode the four built-in component packages are no longer installed, so a solution without the right external packages fails `ng build` with missing dependencies. The reviewer confirmed this manually with a new solution that has no separate extension packages. It is tracked in SVY-21816.

**Reviewed scope:** `servoy-eclipse` commits `cf86ca5f7d` (approvals, committed lockfile and package.json, frozen WAR install) and `bd4d3e86e0` (pre-built `@servoy/public` tarball, internal components as source packages), both on `master`. Fix Version 2026.12 matches master.

## Why it is ELEVATED

Commit `bd4d3e86e0` makes `checkPackage` return `null` for `servoydefault`, `dialogs`, `window` and `ngclientutils` in pnpm mode (`WebPackagesListener.java`, around line 1763). Nothing in the shipped root `node/package.json` supplies `@eonasdan/tempus-dominus`, `@ng-bootstrap/ng-bootstrap`, `tinymce`, `@tinymce/tinymce-angular` or `@popperjs/core`. Only the external packages (bootstrapcomponents, nggrids, extra-components) declare them. The core of the approval mechanism itself held up in review: version-scoped, no allow-all path, no YAML injection, and the WAR path never prompts.

## Manual test plan

Run with `-Dservoy.jsRuntime=pnpm`.

**Verifying the fix**
1. Developer build of a solution that pulls in a package with a build script (for example one depending on `esbuild`). Expect the allow/deny prompt, `ng_web_packages/pnpm-build-approvals.json` written, a `pnpm rebuild` in the console, and `package.json` plus `pnpm-lock.yaml` committed back to `ng_web_packages/`.
2. Rebuild the same solution. There should be no prompt and the script still runs. Deny one entry, rebuild, and check it is not asked again.
3. Bump a dependency so a new `name@version` appears. It should be asked again.
4. Solution Explorer: right-click the solution's `ng_web_packages` node, open the approvals editor, edit and save.
5. WAR export (wizard or headless) with complete committed approvals. It must pass without a prompt. Remove one entry from `allowed` and export again. Expect a failure listing the exact pending `name@version`.
6. WAR export with no committed `pnpm-lock.yaml`, or with a broken frozen install. On current master the export fails with a message. It must not package a stale `_dist`.

**Regression checks**
1. Build and WAR-export, in pnpm mode, a solution with an empty `ng_web_packages` and one with only 12grid. `ng build` must resolve the five libraries named above (open, see SVY-21816).
2. WAR export after a Servoy upgrade or a component content change: commit the pair with build A, then export with a build where the `servoy-public` version or a component's content differs. Check the console for "removing N component(s) not exported" and whether `@servoy/public` or an exported component is listed (follow-up case).
3. Commit a `package.json` with a harmless `postinstall` that writes a marker file, then export a WAR. Check whether the marker appears (follow-up case).
4. npm mode (the default): Solution Explorer clean build. The console now shows `npm update` where it used to show `npm ci`. Confirm this is acceptable, then run a normal build and a WAR export. Output should otherwise be unchanged.
5. pnpm mode: the generated `angular.json` styles for the four internal packages use `./projects/<name>/...`, and the running solution renders with its styles.
6. Run Servoy from the workspace (PDE) in pnpm mode without a Maven `initialize` build. Expect an abort that names `@servoy/public`, not a silent bad install.
7. Developer build and WAR export running at the same time in pnpm mode. Neither should fail because of the shared static demotion counter in `RunPNPMCommand`.

**Automated checks:** plain tests in `com.servoy.eclipse.ngclient.ui.tests` (package `.plainjunit`): `PnpmBuildApprovalsTest`, `PnpmBuildScriptConfigTest`, `RunPNPMCommandTest`, `WebPackagesListenerCssLibTest`. They were not run during the review. The discovery wrapper, seed and commit-back helpers, the SWT dialogs and the headless WAR path have no tests and need a live workspace with real pnpm.

**Surfaces to cover:** Servoy Developer on Windows in pnpm mode, a Linux WAR export or CI run (lockfile portability), and npm mode for the clean-build change. Designer, server and client repos need none.

## Possible improvements / follow-ups

- **SVY-21816:** the internal component dependencies are missing in pnpm mode (see above).
- **Follow-up case (to be created by the reviewer), WAR export trusts the solution's committed files:**
  - The committed `package.json` fully replaces the generated one (`PnpmBuildScriptConfig.seedLockfileFromCommitted`). Root scripts, lifecycle hooks, overrides and non-`@servoy` dependencies therefore come from the solution repo and run on the export machine. The approvals only cover dependency scripts. Take only the `@servoy/*` and web package `file:` entries from the committed file instead.
  - `removeNonExportedComponents` decides by tarball file name. `@servoy/public` has no exclusion and its name follows the `servoy-public` version. Component tarballs carry a content hash. A stale committed pair can therefore remove `@servoy/public` or a component the WAR does export. Compare by package name, never remove `@servoy/public`, and fail with a clear message when the pair is stale. Not triggered today (`servoy-public` is still at 2026.9.5). The component-tarball case is inferred from the code and not tested.
- npm mode: `cleanInstall.set(false)` now runs unconditionally, so a clean build runs `npm update` instead of `npm ci`. Low risk, because the folder is wiped and freshly installed just before. Decide whether it is intended or keep `npm ci` for npm mode.
- The committed approvals file is applied silently on a first build of a cloned solution. The reviewer accepted this (same model as pnpm's own `allowBuilds`).
- Approval entries are not validated when read from `pnpm-build-approvals.json`. A bare name or a range may be wider than the "exact `name@version`" the spec promises. pnpm's key grammar was not verified. There is no injection (YAML quoting is correct).
- `RunPNPMCommand.buildScriptDiscoveryDemotion` is a JVM-wide static counter. While one build's install window is open, every pnpm command in the JVM gets `strictDepBuilds` demoted.
- The prompt pre-selects Allow for every pending entry, and `parseIgnoredBuildsErrorLine` currently has no callers.
- Maven runs `build_lib` twice: once in the existing "build servoy public" execution and again inside `pack_public`. This is a build-time cost only, and the spec says it runs once.
- Tests were later moved to `.../ngclient/ui/tests/plainjunit/` on master, so they will not cherry-pick onto an older line.
- Documentation drift: `node/AGENTS.md` line 264 still said `strictDepBuilds: false`. It was corrected during the review and is an uncommitted edit in the working tree. It now describes the generated yaml, the tarballs and the committed pair.
- Unverified: whether the frozen WAR path has been run end to end with a real export, and pnpm's behaviour for root lifecycle scripts, `allowBuilds` key grammar and symlinked files in `ng_web_packages`.
