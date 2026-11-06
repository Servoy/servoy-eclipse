# Spec: SVY-21734 — pnpm mode: Titanium NG build silently "succeeds" without doing anything

## 1. Goal

With `-Dservoy.jsRuntime=pnpm`, the Titanium NG build must either run the real pnpm commands or stop with a clear message. Today it can finish in a few seconds with only warnings because no pnpm process is ever started. Make an explicit `servoy.pnpmPath` count regardless of how node is resolved, and turn the two silent failure paths (node/pnpm unavailable, tarball not created) into visible aborts.

## 2. Background

### 2.1 How the build ends up doing nothing

- `Activator.extractNode()` reads `servoy.pnpmPath`, but only in the branch where `servoy.nodePath` and `servoy.npmPath` are also set. With a bundled node the pnpm path comes only from extracting the bundled pnpm archive. A source checkout ships none, so `Activator.pnpmPath` stays `null`.
- `Activator.createNPMCommand()` returns `NoOpNPMCommand` when node is `null`, or when in pnpm mode pnpm is `null`. `NoOpNPMCommand` reports exit code 0 and only writes to the Eclipse error log.
- Every `pnpm pack`, `pnpm install` and `pnpm run build_*` therefore "succeeds". `WebPackagesListener.PackageCheckerJob.packPackage()` then logs `- WARNING: 'pnpm pack' for <pkg> produced no .tgz file` and returns `null`, the package is skipped, and the job ends `Status.OK_STATUS` ("Total time ... 3 s").
- `copyAngularLocales()` finds nothing because nothing was installed (a follow-on warning, no change needed).

### 2.2 Related documents

`docs/SVY-21456-ship-pnpm-runtime.spec.md` (pnpm runtime), `docs/SVY-21502-prebuilt-pnpm-tarballs.spec.md` (tarball flow). Triage: `docs/SVY-21734-triage.md`.

## 3. Design

### 3.1 Configured `servoy.pnpmPath` always wins

In `Activator.extractNode()`'s extraction job, after the bundled pnpm is resolved, replace it with the configured one when `servoy.pnpmPath` is set. The decision is a pure static, `WebPackagesListener.preferConfiguredPnpm(String configuredPath, File bundledPnpm)`: `configuredPath != null ? new File(configuredPath) : bundledPnpm`. The `servoy.nodePath`/`servoy.npmPath` branch is unchanged (it already sets the configured pnpm).

### 3.2 Fail loud when node or pnpm is unavailable

At the start of `PackageCheckerJob.run()`, in pnpm mode only: wait for node extraction, read `Activator.getNodePath()` / `getPnpmPath()`, and compute `WebPackagesListener.missingPnpmRuntimeMessage(node, pnpm)`. It returns `null` when both are existing files, otherwise a message naming both values and the settings to check (`servoy.pnpmPath / servoy.nodePath / servoy.npmPath`). On a non-null result the job writes `ERROR: <message>` to the NG Build Console, logs it, and returns `new Status(IStatus.WARNING, ...)`.

`IStatus.WARNING` is deliberate: every other failure status in this job is a warning, and the caller turns any non-OK result into the final build-failure handling (`handleTitaniumNGClientBuildFailure`). An `ERROR` status would open an additional error dialog per failure.

### 3.3 Fail loud when a tarball cannot be created

`PackageCheckerJob` gets a `List<String> packFailures`, cleared at the start of each `run()`. A name is added when:

- `packPackage()` gets a non-zero exit code from `pnpm pack`, finds no new `.tgz`, or throws;
- `copyShippedPublicTarball()` returns `null` in pnpm mode (`@servoy/public`).

The console lines for the first two cases change from `- WARNING:` to `- ERROR:`. After the package check loop (inside the `ng2Services.size() > 0 || componentPackageSpecToReader.size() > 0` block), `WebPackagesListener.packFailuresMessage(packFailures)` returns `null` for an empty/`null` collection, otherwise `pnpm mode: could not create the tarball(s) for [<names>]; installing without them would give a broken Angular build. Titanium NG build aborted (see the messages above).` The job writes `ERROR: <message>` to the console and returns an `IStatus.WARNING` status, before any install or build runs.

### 3.3b WAR export fails on any non-OK build result

`WebPackagesListener.exportNG2ToWar` used to throw only for `IStatus.ERROR`. Every other non-OK status fell through to the `dist/app/browser` check, which a stale `<solution>_dist` from an earlier export satisfies, so a WAR could be produced from an old build.

Checked every non-OK status `PackageCheckerJob.run()` can return:

- `WARNING`: node/pnpm missing, tarball not created (new); `npm run build_lib_debug_nowatch`, `install`, `ci`, `update`, `dedup` and `npm run <build>` failed (exit code or exception); `pnpm remove` of non-exported components failed. All mean the Angular build did not complete. None is a "finished with a harmless problem" case.
- `ERROR`: undecided pnpm build-script approval (already fatal).
- `CANCEL`: user cancel, or the build disabled for junit runs (`Activator.isNodeExtractionAndTitaniumBuildDisabled`).
- `OK` is returned only after the build step ran.

So there is no warning that is fine for a WAR export. `exportNG2ToWar` now throws a `RuntimeException` (message of the status, or a generic one) for every non-OK status, except `CANCEL` when the monitor was not cancelled (the junit-disabled case keeps its old behaviour: it falls to the dist check). `WarExporter` already wraps the exception in an `ExportException`, or returns quietly when the user cancelled. `MobileExporter` calls the same method and gets the same behaviour.

### 3.3c Removed dead code

The pnpm-mode install used to fall back to `pnpm add ./dist-public/` when `copiedPublicTarball == null`. That fallback came from SVY-21456 (`23d5af282e`), when the tarball was packed from `dist-public` per solution and packing could fail. SVY-21502 (`bd4d3e86e0`) replaced that with the shipped tarball and left the branch in place. It did make sense when it was written, but it was already wrong after SVY-21502: in pnpm mode `dist-public` is never built (`build_lib_debug_nowatch` is skipped), and `NodeFolderCreatorJob` does not copy it (`/dist-public/` is an ignored resource), so the fallback would only fail later with a missing directory. After this change a missing tarball aborts earlier, so the branch is unreachable.

Removed: the fallback branch; the `copyShippedPublicTarball` Javadoc now says the caller aborts. To make "never null at the install" true, the shipped tarball is now copied for every pnpm run, not only when component/service packages exist, and the pack-failure abort sits outside that `if`.

### 3.4 `@servoy/public` self-dependency in `node/package.json` (no code change)

Out of the implementation, by decision: `"@servoy/public": "file:dist-public"` stays in git. Tried and rejected: `link:dist-public` (npm cannot parse it), `optionalDependencies` (same), a `link_public` script chained into `build_lib*`, and re-adding the dependency after each library build (see the triage). Servoy Developer is not affected because `NodeFolderCreatorJob.removePublicFileDependency()` and `npm uninstall @servoy/public` already handle it.

Manual procedure for running pnpm directly in the source `node/` folder: temporarily remove the `@servoy/public` line, run `pnpm install`, put the line back, then build the library. Revisit when the repo moves fully to pnpm.

## 4. Implementation plan

1. `Activator.java`: in the extraction job use `pnpm = WebPackagesListener.preferConfiguredPnpm(pnpmPth, pnpm)`.
2. `WebPackagesListener.java`:
   1. Add the public statics `preferConfiguredPnpm`, `missingPnpmRuntimeMessage` and `packFailuresMessage` next to `publicTarballName`.
   2. Add `packFailures` to `PackageCheckerJob`; clear it at the start of `run()`.
   3. Add the pnpm-mode node/pnpm check at the start of `run()` (3.2).
   4. Record failures in `packPackage()` (three places) and for the shipped `@servoy/public` tarball; change the two console lines to `- ERROR:`. Copy the shipped tarball for every pnpm run.
   5. Abort after the package check when `packFailuresMessage(...)` is non-null (3.3).
   6. `exportNG2ToWar`: throw for every non-OK build status except a not-user-cancelled `CANCEL` (3.3b).
   7. Remove the unreachable `./dist-public/` fallback in the install block and fix the `copyShippedPublicTarball` Javadoc (3.3c).
3. Add `PnpmRuntimeResolutionTest` (plainjunit) in `com.servoy.eclipse.ngclient.ui.tests`.
4. No change to `package.json`, `NoOpNPMCommand`, or `RunPNPMCommand`.

## 5. Acceptance criteria

- [ ] With `servoy.jsRuntime=pnpm` and a valid `servoy.pnpmPath` but without `servoy.nodePath`/`servoy.npmPath`, the configured pnpm is used (the console shows `---- Running pnpm command:` lines). The same holds when a bundled pnpm exists.
- [ ] With `servoy.pnpmPath` unset, the bundled pnpm is used as before.
- [ ] With node or pnpm missing (null or file does not exist), the job stops immediately with a console message naming both values and the settings to check, and returns a non-OK (`WARNING`) status.
- [ ] When any component tarball (or the shipped `@servoy/public` tarball) cannot be created, the job stops before install/build with a message listing the package names, and returns a non-OK (`WARNING`) status.
- [ ] A build in which all tarballs are created and node/pnpm exist behaves exactly as before.
- [ ] npm mode is unchanged (none of the new checks run).
- [ ] A failed run produces only the final build-failure dialog, not one error dialog per failure.
- [ ] A WAR export whose production build returns any non-OK status (except a `CANCEL` that was not a user cancel) fails with that status message, also when a stale `<solution>_dist` exists; a user cancel ends the export quietly.

## 6. Out of scope

- Changing `NoOpNPMCommand` to report a failure (affects the Node-unavailable case and existing tests; separate case if wanted).
- Changing the `@servoy/public` self-dependency in `node/package.json` (see 3.4).
- The "Angular locale files not found" warning (follow-on symptom).
- Pre-existing `pnpm-workspace.yaml` / `pnpm-lock.yaml` / launch-file changes in the working tree; they are not part of this change.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `NoOpNPMCommand` report a non-zero exit code when Node is unavailable (separate case)? | Andrei | open |
| Move `@servoy/public` to `optionalDependencies` once the repo is fully on pnpm? | Andrei | open |

### Behaviour changes to be aware of

- Running from a source checkout without the Maven-built `node/tarballs/servoy-public-<version>.tgz` now aborts in pnpm mode with `@servoy/public` in the message, where it used to attempt a directory install (which could not work, see 3.3c).
- A WAR (or mobile) export now fails on any non-OK result of the production build, in npm mode too (3.3b). Previously a failed `npm install` / `npm run build` during an export could still package a stale `<solution>_dist`.

## 8. Test cases

All are plain JUnit (`com.servoy.eclipse.ngclient.ui.tests.plainjunit.PnpmRuntimeResolutionTest`); the three rules are public statics, so no OSGi/Activator context is needed.

| Group | Test | AC |
|-------|------|----|
| `PreferConfiguredPnpm` | configured path overrides the bundled pnpm | 1 |
| `PreferConfiguredPnpm` | configured path used when there is no bundled pnpm | 1 |
| `PreferConfiguredPnpm` | bundled pnpm kept without a configured path | 2 |
| `PreferConfiguredPnpm` | nothing configured, nothing bundled gives `null` | 2 |
| `MissingPnpmRuntimeMessage` | existing node and pnpm give no error | 5 |
| `MissingPnpmRuntimeMessage` | null pnpm names pnpm, the settings and "aborted" | 3 |
| `MissingPnpmRuntimeMessage` | null node names node | 3 |
| `MissingPnpmRuntimeMessage` | non-existing pnpm path appears in the message | 3 |
| `MissingPnpmRuntimeMessage` | non-existing node path appears in the message | 3 |
| `MissingPnpmRuntimeMessage` | both null: the error names both | 3 |
| `MissingPnpmRuntimeMessage` | a directory in place of pnpm gives an error containing that path | 3 |
| `PackFailuresMessage` | null and empty give no error | 5 |
| `PackFailuresMessage` | failed packages are listed, "aborted" present | 4 |
| `PackFailuresMessage` | `@servoy/public` reported by name | 4 |

Not unit tested (needs a live OSGi/pnpm install): the wiring of the three rules inside `PackageCheckerJob.run()` and `packPackage()`, and the single-dialog behaviour. Covered by the manual test plan in the review summary.

## 9. Manual test plan

Run in Servoy Developer with `-Dservoy.jsRuntime=pnpm` and check the NG Build Console.

1. **Configured pnpm wins:** only `-Dservoy.pnpmPath=<valid pnpm>` set (no node/npm properties), source checkout. Expect `---- Running pnpm command:` lines and a real install and build. Repeat with a bundled pnpm present; the configured one must be used.
2. **Nothing configured:** `servoy.pnpmPath` unset with a bundled pnpm. The bundled one is used, as before.
3. **Missing runtime:** `-Dservoy.pnpmPath=C:\does\not\exist\pnpm.exe`, then unset on a checkout with no bundled pnpm, then a bogus `servoy.nodePath`/`servoy.npmPath`. Expect `ERROR: servoy.jsRuntime=pnpm is set but node (...) or pnpm (...) is not available; ...` and no pnpm process started.
4. **Tarball failure:** make `pnpm pack` fail for one component (or remove `node/tarballs/servoy-public-<version>.tgz`). Expect `- ERROR: 'pnpm pack' for <pkg> ...`, then `ERROR: pnpm mode: could not create the tarball(s) for [<pkg>]; ... aborted`, and no install or build.
5. **Regressions:** a normal pnpm build completes as before; an npm-mode build (and npm-mode WAR export) shows none of the new messages.
6. **Dialogs:** steps 3 and 4 produce one build-failure dialog, not one per failure.
7. **WAR export fails loud:** make the production build fail (for example `-Dservoy.pnpmPath` pointing to a missing file, or break a component's `package.json`) while a `<solution>_dist/dist/app/browser` from an earlier export still exists. The WAR export must fail with the message from the console, and no WAR must be produced from the stale dist. Cancelling an export from the progress dialog must still end it quietly.
8. **WAR export, healthy path:** a normal WAR export in pnpm mode and in npm mode still completes.
