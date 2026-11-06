# Triage Report — SVY-21734

**Verdict:** PROCEED

## Reported problem

With `-Dservoy.jsRuntime=pnpm -Dservoy.pnpmPath=...` the Titanium NG build finishes in about 3 s with only warnings and no error:

- `WARNING: 'pnpm pack' for @servoy/12grid | @servoy/nggrids | @servoy/bootstrapcomponents | @servoy/servoyextracomponents produced no .tgz file`
- `WARNING: Angular locale files not found or copy failed, skipping`
- `Total time to check/install Titanium NG solution/dependencies ... is 3 s.`

The NG Build Console never shows a `---- Running pnpm command:` line, so no pnpm process was started. The target folder is left without installed packages and without an Angular build. The job reports success.

Separately, running pnpm directly in the source `node/` folder fails on the `"@servoy/public": "file:dist-public"` dependency, because `dist-public` does not exist before the first library build. npm tolerates a missing `file:` directory, pnpm does not.

The ticket proposes no solution. The reporter asks two questions: why are the warnings there and can the build pass with them, and why does the build neither continue nor fail.

## Root-cause assessment

All three symptoms come from one chain.

1. **`servoy.pnpmPath` is ignored when node is bundled.** `Activator.extractNode()` (ngclient.ui `Activator.java`) only reads `servoy.pnpmPath` inside the `nodePth != null && npmPth != null` branch. In the other branch (bundled node, run from a source checkout) the pnpm path comes only from the bundled-pnpm extraction. A source checkout ships no pnpm archive, so `pnpmPath` stays `null`.
2. **A null node/pnpm silently becomes a no-op.** `Activator.createNPMCommand()` returns `NoOpNPMCommand` when `nodePath == null`, or in pnpm mode when `pnpmPath == null`. `NoOpNPMCommand.runCommand()` only writes to the Eclipse error log and `getExitCode()` is always `0`. `pnpm pack`, `pnpm install` and `pnpm run build` therefore all "succeed" without doing anything.
3. **Failed packs are only warnings.** In `WebPackagesListener.PackageCheckerJob.packPackage()` a non-zero exit code, a missing `.tgz` or an exception writes `- WARNING: ...` and returns `null`. `resolveInstallSpecifier()` then returns `null` and the package is skipped (`checkPackage()` returns `null`). The install and the Angular build run without it. With the no-op commands this is the 3 s success in the log.
4. **The locale warning is a follow-on symptom.** `copyAngularLocales()` looks in `node_modules/@angular/common/locales`. Nothing was installed, so it does not exist.

Answers to the reporter's questions:

- **Can the build pass with the warnings?** No. Without the component tarballs the Angular build cannot produce a working result. The log shows no install and no build at all.
- **Why neither continue nor error?** The status of the job is `OK` as long as every `IRunNPMCommand` reports exit code 0, and the no-op command always does.

The reporter's `pnpm-workspace.yaml: allowBuilds allowed=[], denied=[]` line is correct for a fresh solution and is unrelated.

### The `@servoy/public` self-dependency

`node/package.json` (the source project) declares `"@servoy/public": "file:dist-public"`. `NodeFolderCreatorJob.removePublicFileDependency()` already strips it from the solution copy in pnpm mode, and `npm uninstall @servoy/public` handles npm mode. So Servoy Developer is not affected. Only a developer running `pnpm install` directly in the source `node/` folder hits it.

It cannot simply be removed from git. `tsconfig.json` has `paths: {}`, so the app only finds `@servoy/public` through the `node_modules` link that `npm install` creates for that entry. Verified: with the link removed, `tsc --noEmit -p src/tsconfig.app.json` reports 294 errors (`TS2307`). The alternatives were evaluated and rejected:

- `link:dist-public` (pnpm style, also as `optionalDependencies`): npm 12.2.1 fails with `EUNSUPPORTEDPROTOCOL: Unsupported URL Type "link:"` on `npm install`.
- A `link_public` script chained into `build_lib*`: works, but adds a linking script and leaves a link that a later `npm/pnpm install` prunes.
- Re-adding the dependency after the library build: dirties a tracked file on every build (including Maven `initialize`), and `build_lib_debug` never exits.

## Ticket premise check

The description of the case, in its first version, proposed moving `@servoy/public` to `optionalDependencies` with `link:dist-public`. That does not hold (see above): it breaks `npm install`, bypasses `removePublicFileDependency` (which only looks at `dependencies`), and npm is still the default runtime. The case should be updated to say the self-dependency is a documented manual workaround (see Recommendation).

The Java part of the ticket (honour `servoy.pnpmPath`, fail loud instead of silently succeeding) holds up and is the real defect.

## Approaches considered

1. **Honour `servoy.pnpmPath` everywhere and fail loud on missing runtime / failed packs (recommended).**
   - Pros: removes the silent no-op for the actual cause; small and local to `Activator` and `WebPackagesListener`; pure rules can be unit tested.
   - Cons: a pnpm-mode build that previously "passed" silently now stops with a message.
2. **Make `NoOpNPMCommand` report a non-zero exit code.**
   - Pros: fixes the symptom at its source for every caller.
   - Cons: `NoOpNPMCommandTest` and `ActivatorCreateNPMCommandIntegrationTest` assert exit code 0; the no-op is used deliberately when Node is unavailable (e.g. tests with the Titanium build disabled). Larger blast radius, so not done here.
3. **Change `node/package.json` so pnpm accepts the self-dependency.**
   - Rejected, see the section above.
4. **No code change needed.**
   - The reporter could pass `-Dservoy.nodePath` and `-Dservoy.npmPath` as well, which makes `servoy.pnpmPath` count. That is a workaround for a hidden coupling, and the silent success remains for any other way node or pnpm ends up `null`.

## Recommendation

PROCEED with approach 1:

- `Activator`: a configured `servoy.pnpmPath` wins over the bundled pnpm in both branches.
- `WebPackagesListener`: in pnpm mode, abort at the start of the job with a console message and a non-OK status if node or pnpm is missing; abort after the package check if any tarball (including the shipped `@servoy/public`) could not be created.
- Use `IStatus.WARNING` for these aborts, like every other failure status in this job, so the developer gets only the final failure dialog and not one error dialog per failure (decided by the developer).
- Keep the pure rules as small public statics on `WebPackagesListener` so they are testable without OSGi.
- Leave `node/package.json` unchanged. Document the one-time manual step for running pnpm in the source folder: temporarily remove the `@servoy/public` line, `pnpm install`, then put it back. Revisit when the repo moves fully to pnpm (e.g. as an optional dependency).

## Git history findings

- `9ac34fb579` (2026-09-28, SVY-21456 "bundle pnpm runtime and add pnpm-aware command runner"): introduced `servoy.pnpmPath` (`getSystemOrEvironmentProperty("servoy.pnpmPath")`) but only in the `nodePth != null && npmPth != null` branch, and `NoOpNPMCommand` for a missing pnpm binary (with a `getLog().warn`).
- `14febb667e`: the `servoy.nodePath` / `servoy.npmPath` branch the pnpm path was attached to.
- `bd4d3e86e0` (2026-10-20, SVY-21502 "pre-build @servoy/public tarball ..."): added the shipped-tarball flow and the `pnpm pack` warnings in `packPackage()`; those warnings were never made fatal. It also added the `pack_public` script.
- `NodeFolderCreatorJob.removePublicFileDependency()` (SVY-21456) already drops the `file:dist-public` entry in pnpm mode.
- Existing specs: `docs/SVY-21456-ship-pnpm-runtime.spec.md`, `docs/SVY-21502-prebuilt-pnpm-tarballs.spec.md`. Neither requires pack failures to be non-fatal.
