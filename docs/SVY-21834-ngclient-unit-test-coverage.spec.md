# Spec: SVY-21834 — Raise unit test coverage of the NG client Java bundles to 70%

## 1. Goal
Raise the Java line coverage of `com.servoy.eclipse.ngclient` (34.5%, 417 lines) and
`com.servoy.eclipse.ngclient.ui` (14.2%, 3103 lines) to at least 70% in the "Java Coverage
(JaCoCo, unit + integration)" result of the `master/servoy-eclipse` Jenkins job. The work is
part of the coverage catch-up under umbrella case SVY-21836. Most of the uncovered code sits in
the Titanium NG build pipeline (`WebPackagesListener.PackageCheckerJob`, `NodeFolderCreatorJob`,
the npm/pnpm command wrappers, node extraction). Every Developer Titanium build and every WAR
export runs through this code, and it has had several recent regressions-by-change (SVY-21502,
SVY-21642, SVY-21734). Tests that guard it are worth more here than the number itself.

The approach is plainjunit first (JUnit 6 + Mockito, `@TempDir` node folders), mocking at the
Eclipse/runtime boundary. It includes one deliberate, behaviour-preserving production refactor:
splitting `PackageCheckerJob.run` into testable steps. Without it, 70% cannot be reached for
`com.servoy.eclipse.ngclient.ui` (see 2.2).

## 2. Background

### 2.1 Baseline (Jenkins master/servoy-eclipse build 518)
| Bundle | Lines | Covered | Needed for 70% |
|---|---|---|---|
| `com.servoy.eclipse.ngclient` | 417 | 34.5% | +148 lines |
| `com.servoy.eclipse.ngclient.ui` | 3103 | 14.2% | +1732 lines |

Classes with the most missed lines: `WebPackagesListener$PackageCheckerJob` (872 / 0 covered),
`NodeFolderCreatorJob` (233 / 0), `RunPNPMCommand` (224 / 39), `WebPackagesListener` (182 / 86),
`utils.TarInputStream` (165 / 0), `ngclient.ui.Activator` (143 / 47).

Use `skills/jenkins/scripts/jacoco-baseline.ps1` on the archived `<bundle>/target/site/jacoco/jacoco.xml`
of a build to read the current numbers.

### 2.2 Why seams alone are not enough
`PackageCheckerJob.run(IProgressMonitor)` is one method of about 915 lines
(`WebPackagesListener.java` lines 344–1254). It:
- reads the console, runtime and mode from `Activator.getInstance()` (lines 348, 356–367);
- collects services, component, layout and CSS/asset info from `WebComponentSpecProvider.getSpecProviderState()` (371–475);
- copies the shipped `@servoy/public` tarball and runs `checkPackage` per package reader (481–548);
- rewrites `allservices.service.ts`, component/structure templates, the form-template copy and `allcomponents.module.ts` (551–756);
- rewrites `angular.json` styles and assets (759–865), and `polyfills.ts` for zone.js (867–891);
- refreshes open editors through `PlatformUI` (895–911);
- runs the install block: npm build of `dist-public`, `pnpm add`/`npm install`, frozen `ci` for WAR, `update`, `dedup` plus the hand-written node_modules dedup pass, and the pnpm build-approval window (913–1190);
- copies Angular locales and runs `npm run <build>` (1192–1224);
- reschedules itself from the `scheduled` counter in `finally` (1231–1253).

70% of 3103 lines is 2172 covered lines. Without the 872 lines of `PackageCheckerJob`, 2231 lines
remain, so about 97% of everything else would need coverage. The job body has to be split into
steps that can be called from a test.

### 2.3 Test projects
- `com.servoy.eclipse.ngclient.ui.tests` is a plain plugin (not a fragment) with
  `Require-Bundle: com.servoy.eclipse.ngclient.ui`. It has 13 plainjunit and 2 integrationpde
  classes, JaCoCo wiring for both runs, and it is in the Jenkinsfile `-Pintegration -pl` list.
  Because it is a separate bundle, package-private members of `com.servoy.eclipse.ngclient.ui`
  are not visible to it under OSGi. Helpers a test calls must be `public`. Its MANIFEST has no
  `org.mockito` import yet.
- `com.servoy.eclipse.ngclient.tests` is a fragment of `com.servoy.eclipse.ngclient`, so
  package-private helpers are visible. It has one plainjunit class
  (`NGClientWebsocketSessionFactoryTest`) and plainjunit JaCoCo wiring. It has no integrationpde
  run and is not in the Jenkins integration list. Its MANIFEST has no `org.mockito` import yet.

### 2.4 Existing pattern to continue
Recent work in this area extracted `public static` helpers from `WebPackagesListener` and covered
them with plainjunit tests: `rewriteCssLibUrl`, `publicTarballName`, `preferConfiguredPnpm`,
`missingPnpmRuntimeMessage`, `packFailuresMessage`, `generateAllComponentsModuleContent`,
`copyAngularLocales`. `ngclient` uses package-visible static routing helpers
(`NGClientWebsocketSessionFactory.runRouting`/`shutdownRouting`/`selectClientKind`,
`FormPreviewNGClient.shouldReuseFormPreviewClient`). New seams follow these patterns.

### 2.5 Reflection already used in tests
The ticket forbids reflection on private members. These existing tests break that rule:
- `WebPackagesListenerPrecompressArgsTest` → `buildPrecompressArgs` (package-private);
- `RunPNPMCommandTest` → `translateToPnpmArguments`, `getVolumeRoot`, `getFileStoreOfNearestExisting` (private static);
- `PnpmBuildScriptConfigTest` → `parseModulesYamlIgnoredBuilds`, `parseIgnoredBuildsErrorLine` (private).

The approved scope names `buildPrecompressArgs` explicitly. The `RunPNPMCommand` helpers get
made public anyway, as part of step 1 (3.1). Changing `PnpmBuildScriptConfigTest` is optional
(section 7).

### 2.6 Git history
- `14febb667e` (2026-06-06, J. Compagner): current form of `PackageCheckerJob.run`.
- `af87d1794a` (2026-08-27, A. Costescu): `Activator.isNodeExtractionAndTitaniumBuildDisabled()`
  guard at the top of `run()` and `extractNode()`. `AbstractTitaniumIntegrationTest` uses it to keep
  the Titanium build off during PDE runs. The refactor must keep the guard as the first statement.
- `55a7afe04`, `c1a62e608`: JaCoCo wiring for plainjunit and integrationpde (source of the baseline).
- `cb6fedecb` SVY-21502 (reproducible tarballs, `DeterministicTarball` + test), `f620cf826` /
  `979c368ad` SVY-21642 (precompress), `251313f97` SVY-21734 (pnpm path, missing `@servoy/public`
  tarball aborts the job). These carry ordering constraints the refactor must preserve
  (see 3.3.3).
- No earlier spec in `docs/` for this case. Sibling coverage cases SVY-21828…SVY-21835 are open.

## 3. Design

### 3.1 Phase 1: pure code, no or minimal production change
Each item is a plainjunit class in `com.servoy.eclipse.ngclient.ui.tests.plainjunit` (or
`com.servoy.eclipse.ngclient.startup.plainjunit[.resourceprovider]` for `ngclient`).

| Target | What to test | Production change |
|---|---|---|
| `utils.TarInputStream`, `utils.TarEntry`, `utils.TarException` | Build tar bytes in the test (headers written by hand or by `DeterministicTarball`): regular files, directories, long names / ustar prefix, zero-size entries, padding to 512, end-of-archive blocks, bad checksum → `TarException`, truncated stream, `read()` vs `read(byte[],int,int)` across entry boundaries, `TarEntry` mode/size/time getters. | None |
| `utils.ZipUtils` | `isZipFile`/`isTarGZFile`/`isTarXZFile` by URL suffix; `extractZip`, `extractTarGZ`, `extractTarXZ` into `@TempDir` (archives made in the test with `java.util.zip`, `GZIPOutputStream` and `org.tukaani.xz` if it is on the bundle classpath); directory entries, nested paths, executable bits where applied; zip-slip entries (`../`) must not land outside the destination, and the test pins what the code actually does. | None, unless a zip-slip gap is found. That is reported as a separate case, not fixed here. |
| `DirectorySync` | Initial sync of a source tree into a target, `ignoreFilter` honoured, `createFolder`, `copyOrCreateFile` (create, overwrite, unchanged), `destroy()` stops the watcher. Watcher event tests use a bounded poll on the target file, no fixed sleeps. | Make `createFolder`/`copyOrCreateFile` `public static` if tests call them directly. |
| `IndexPageFilter` | Mocked `HttpServletRequest`/`Response`/`FilterChain`: pass-through for non-index paths, solution name from URI (`getSolutionNameFromURI`), the loading page while a build runs, the build-failed page, the index from the project folder. | Seam for `Activator.getInstance().getSolutionProjectFolder()` and the bundle entry lookups (`/resources/loadingclient.html`, `/resources/buildfailedclient.html`): `protected File getSolutionProjectFolder()` and `protected URL getBundleEntry(String)` on the filter, overridden in a test subclass. |
| `RunPNPMCommand` helpers | `translateToPnpmArguments` (all npm verbs it maps), `computeStoreDirArgIfCrossVolume`, `computeHardlinkImportMethodArgIfSameVolume`, `getVolumeRoot`, `getFileStoreOfNearestExisting`, `getDefaultPnpmStoreDir`, the build-script-discovery demotion begin/end, `getExitCode`/`setOutputStream`/`setExtraEnvironment`. | Make the private static helpers `public static`. Turn the two instance `compute…` methods into `public static` methods that take the store dir as a parameter; the instance methods call them. Remove the reflection from `RunPNPMCommandTest`. |
| `RunPNPMCommand.runCommand` / `RunNPMCommand.runCommand` | Run against a fake `pnpm`/`npm` script in `@TempDir` (as `PrecompressWarScriptTest` does for node): exit code propagated, output captured in the `StringOutputStream`, extra environment passed, cancel kills the process. | None if the fake-script route works on Windows and Linux CI. Otherwise add a `protected ProcessBuilder createProcessBuilder(List<String>)` seam. |
| `NodeFolderCreatorJob` static helpers | `ignoredResource`, `createFolder`, `copyOrCreateFile`, `removePublicFileDependency` (rewrite of a `package.json`), the file-watcher filter (`accept`). | Make the used helpers `public static`. `removePublicFileDependency` becomes `public static`, with the console as a parameter. |
| `PnpmBuildApprovals*`, `PnpmBuildScriptConfig` | Fill in branch gaps left by the existing tests. | None |
| `ngclient` `ResourceProvider` | `setPackages`/`updatePackageResources`/`setRemovedPackages`/`getDefaultPackageNames` reader bookkeeping, `computeURL` for `/war/...` and sablo paths, `doFilter` with mocked servlet API: served resource with content type, pass-through on miss, removed package not served. `BundlePackageReader` getters against a mocked `Bundle`/`Manifest`. | Seams for `Activator.getNClientBundle()`, `Activator.getSaboBundle()` and the `ApplicationServerRegistry` lookup in the `templatesDir` initializer: package-visible static `Supplier`s or `protected` getters. Package-private is fine because `ngclient.tests` is a fragment. |
| `ngclient` `ComponentResourcesExporter` | Copies `/war/` entries filtered by exported packages and templates into `@TempDir`, from a mocked bundle that serves `getEntryPaths`/`getEntry`. | Same bundle seam as `ResourceProvider`. |
| `ngclient` `DeveloperMediaResourcesServlet` | Delegation to the debug client handler, with `ApplicationServerRegistry` mocked via `mockStatic`. | None |

### 3.2 Phase 2: `ngclient.ui` `Activator` extraction paths
`extractPath`, `extractPnpmPath`, `readPnpmMarkerToken` and the `extractNode` job body read
`plugin.getStateLocation()`, `Platform.getBundle(pluginId).getResource(...)` and the extension
registry. Seams:

- Extract the extraction logic into `public static` methods that take everything as parameters:
  `File extractPath(File baseDir, URL archiveUrl, String path, boolean deletePreviousPaths, ILog log)`
  and the pnpm equivalent with a `String currentToken`. The existing private methods resolve
  `baseDir`/`archiveUrl`/token from the `IConfigurationElement` and delegate.
- Extract the selection logic of the `extractNode` job (`cf.length > 0`, pnpm only in pnpm mode
  when `pnpmPath` is declared, configured `servoy.pnpmPath` wins) into a `public static` method
  that takes `IConfigurationElement[]` (mockable interface), the pnpm mode and the configured path.
- `getSystemOrEvironmentProperty` and `readPnpmMarkerToken` become `public static`.

Tests: first extraction creates files and `.fullygenerated`; a second call with the marker is a
no-op; a missing marker re-extracts and deletes old dirs except `NG2_FOLDER`; a missing archive
returns `null`; a pnpm token change removes the old `pnpm-<v>/` dir and leaves node alone; a
`.pnpmgenerated` marker with a matching token skips extraction. `createNPMCommand` selection
(no runtime → `NoOpNPMCommand`, pnpm mode → `RunPNPMCommand`, else `RunNPMCommand`) is already
partly covered by `ActivatorCreateNPMCommandIntegrationTest`. Add plainjunit coverage only if it
can be reached through a static helper without a running bundle.

### 3.3 Phase 3: step extraction of `PackageCheckerJob.run`
This is a **production change** in the Titanium build path. It is reviewed as production code,
and the plan includes a manual smoke test (4, step 12).

#### 3.3.1 Shape
`run()` becomes a short orchestrator that calls named steps in the current order, with the same
early returns and the same `Status` objects (severity, message text, plugin). Each step is a
`public` method on `PackageCheckerJob`, or `public static` on `WebPackagesListener` when it needs
no job state. `PackageCheckerJob` itself becomes `public static` with a `public` constructor that
tests can use. Proposed steps, mapping to the blocks in 2.2:

1. `checkRuntimeAvailable(console)` → the pnpm-mode missing node/pnpm check (returns the error `IStatus` or `null`).
2. `collectSpecInfo(SpecProviderState)` → a value object holding `ng2Services`, `componentPackageSpecToReader`, `cssLibs`, `assetsToAdd`, structure tag names, directives, `hasFontAwesomePro`. It takes the export model filter into account.
3. `prepareTarballsAndCheckPackages(info, console)` → shipped public tarball copy (pnpm), `checkPackage` loop, `packFailed` handling. Returns `sourceChanged`, `packageToInstall`, or an error status.
4. `writeAllServices(info, console)`, `writeComponentTemplates(info, console)`, `writeAllComponentsModule(info, console)` → file rewrites. Each returns whether it changed the file.
5. `updateAngularJsonStyles(info, console)`, `updateAngularJsonAssets(info, console)` → `angular.json` edits.
6. `ensureZoneJsPolyfill(console)` → `polyfills.ts`, still gated by `warExportModel == null` and `ti.ng.includezonejs`.
7. `needsInstallOrBuild(...)` → the `packageToInstall.size() > 0 || sourceChanged || !dist.exists() || cleanInstall` decision.
8. `refreshOpenEditors()` → the `PlatformUI` block, behind the UI seam.
9. `runInstallBlock(monitor, console, ...)` → split further into `buildPublicDist`, `addPackages`, `removeNonExportedComponents` (exists), `frozenInstall`, `update`, `npmDedup` + `dedupAgainstRootNodeModules(projectFolder)` (the hand-written pass, made `public static`, pure file logic), plus the approvals window in `finally`.
10. `runAngularBuild(monitor, console, whatToRun)` → locales copy plus `npm run <whatToRun>`, `SOURCE_DEBUG` respected.
11. `finishAndReschedule()` → the `finally` with the `scheduled` counter.

The exact split can follow the code. The requirement is that each step can be called alone from a
test with a `@TempDir` project folder and fake collaborators, and that the order and the early
returns of `run()` do not change.

#### 3.3.2 Seams
One small collaborator interface, `PackageCheckerJob.Environment` (public, nested), with a default
implementation that delegates to the current static calls:

| Seam method | Default delegates to |
|---|---|
| `StringOutputStream console()` | `Activator.getInstance().getConsole().outputStream()` |
| `boolean isPnpmMode()` | `Activator.isPnpmMode()` |
| `File nodePath()` / `File pnpmPath()` | `Activator.getInstance().getNodePath()/getPnpmPath()` |
| `IRunNPMCommand createCommand(File folder, List<String> args)` | `Activator.getInstance().createNPMCommand(...)` |
| `ILog log()` | `Activator.getInstance().getLog()` |
| `SpecProviderState specProviderState()` | `WebComponentSpecProvider.getSpecProviderState()` |
| `void refreshOpenEditors()` | the `PlatformUI` block |
| approvals hooks | `applyKnownPnpmBuildApprovals` / `resolvePnpmBuildScriptApprovals` (these use `ServoyModelManager` / `ResourcesPlugin` through `resolveApprovalsProject`) |

The production constructor uses the default environment. A second `public` constructor takes an
`Environment` for tests. Tests implement it with Mockito or a small fake that records the
command arguments and returns scripted exit codes, so no real npm/pnpm process runs.

#### 3.3.3 Behaviour that must not change
- `isNodeExtractionAndTitaniumBuildDisabled()` stays the first check in `run()`.
- SVY-21734: in pnpm mode the shipped `@servoy/public` tarball is copied into `.tarballs/` **before** the install block and before `checkPackage`. A missing tarball aborts the job.
- SVY-21502: pnpm-mode argument is the **relative** `./.tarballs/<name>.tgz` specifier. WAR export with pnpm uses `pnpm remove` of non-exported components plus a frozen install, never an unfrozen `add`. A clean install from the menu never runs frozen. The strict-dep-builds demotion window is always closed in `finally`, and approvals run only after a successful install. A headless ERROR from approvals is returned.
- npm-only paths (`dist-public` build+pack, `dedup` + own dedup pass) stay npm-only.
- Same console messages and same `Status` severities/messages for each failure.
- `scheduled` counter semantics in `finally` (reschedule when more than one request came in).

#### 3.3.4 Tests
One plainjunit class per step group (e.g. `PackageCheckerJobSpecInfoTest`,
`PackageCheckerJobTemplatesTest`, `PackageCheckerJobAngularJsonTest`,
`PackageCheckerJobInstallBlockTest`, `PackageCheckerJobDedupTest`), plus an orchestration test
that drives `run()` with a fake `Environment` and asserts the recorded command sequence for:
npm Developer first build, pnpm Developer build, pnpm WAR export (remove + frozen), clean
install, "no changes" (no commands, "Skipping install / build" message), missing pnpm runtime
(error status, no commands), failing `add` (WARNING status, demotion window closed, approvals not
run).

### 3.4 Phase 4: remove reflection from `WebPackagesListenerPrecompressArgsTest`
Make `WebPackagesListener.buildPrecompressArgs(File, boolean, boolean)` `public static` and call
it directly from the test. Same for the `RunPNPMCommandTest` reflection, as part of 3.1.

### 3.5 Test bundle wiring
- Add `org.mockito` and `org.mockito.stubbing` (and `org.mockito.junit.jupiter` if used) to
  `Import-Package` of both test MANIFESTs.
- `com.servoy.eclipse.ngclient.tests`: add the servlet API import if `ResourceProvider` tests need
  it and the host does not export it to the fragment.
- No new test projects. No suite classes.

### 3.6 Optional: one integrationpde canary
Optional, from triage approach 3: one `integrationpde` test in `com.servoy.eclipse.ngclient.ui.tests`
that runs `PackageCheckerJob` in the workbench with the fake `Environment` (no real npm) on a
generated project folder. It checks that the real spec provider and file writes work together.
It is not required for the 70% target, so it is optional.

## 4. Implementation plan
1. Test MANIFESTs: add Mockito imports to `com.servoy.eclipse.ngclient.ui.tests` and `com.servoy.eclipse.ngclient.tests`.
2. `TarInputStream`/`TarEntry`/`ZipUtils` tests (`TarInputStreamTest`, `ZipUtilsTest`).
3. `DirectorySync` tests, plus `public static` on its helpers if needed.
4. `RunPNPMCommand`: make the helpers `public static` (store-dir computations take the store dir as a parameter), rewrite `RunPNPMCommandTest` without reflection, add `runCommand` tests with a fake script. Same for `RunNPMCommand`.
5. `NodeFolderCreatorJob`: make the helpers `public static`, add `NodeFolderCreatorJobHelpersTest`. If coverage of `run()` is still needed for the target, add an `Environment`-style seam for bundle entries, the console and command creation, the same as 3.3.2.
6. `IndexPageFilter`: add the seams, add `IndexPageFilterTest`.
7. `ngclient`: add the bundle/registry seams to `ResourceProvider` and `ComponentResourcesExporter`, then add `ResourceProviderTest`, `ComponentResourcesExporterTest`, `DeveloperMediaResourcesServletTest`.
8. `ngclient.ui` `Activator`: extract the static extraction/selection helpers (3.2), add `ActivatorExtractionTest`.
9. `WebPackagesListener.PackageCheckerJob`: add the `Environment` seam with the default implementation. Do not change behaviour yet. Compile, run the existing tests.
10. Extract the steps from `run()` one block at a time (3.3.1), compiling and running the existing tests after each. Add the step tests (3.3.4) alongside.
11. `buildPrecompressArgs` → `public static`. Update `WebPackagesListenerPrecompressArgsTest`.
12. Manual smoke test (required, because of step 10):
    - npm mode: activate a solution with default + extra component packages, start a Titanium client, confirm the build finishes and the client loads.
    - pnpm mode (`servoy.jsRuntime=pnpm`): same, plus a "clean install" from the menu.
    - Add or remove a web package and confirm a rebuild is triggered and succeeds.
    - WAR export (pnpm mode) of the solution with a subset of packages: export succeeds, WAR runs.
    - Edit a component's spec/template and confirm the regenerated `allcomponents.module.ts` / templates.
13. Each change: `getCompilationErrors`, `organizeImports`, `formatFile`. No new SpotBugs high/highest findings.
14. Run all plainjunit tests of both test projects (`eclipse-ide_runJUnitTests`, `junitVersion: "6"`). Run the existing integrationpde tests of `com.servoy.eclipse.ngclient.ui.tests` with `PDEIntegrationTestsBase`.
15. After merge, read the JaCoCo numbers from the next Jenkins build (`jacoco-baseline.ps1`). If either bundle is below 70%, add tests for the largest remaining gaps.
16. Update `AGENTS.md` "Testing" with one line per new test class group, matching the existing entries.

## 5. Acceptance criteria
- [ ] Line coverage of `com.servoy.eclipse.ngclient` ≥ 70% and of `com.servoy.eclipse.ngclient.ui` ≥ 70% in the "Java Coverage (JaCoCo, unit + integration)" result of `master/servoy-eclipse`.
- [ ] All new tests are green on Jenkins. Plainjunit and integrationpde follow the package split in `AGENTS.md`. No `Assume.*`, no assertions that accept more than one outcome.
- [ ] Every new assertion fails when the code under test is broken (spot-checked in review by mutating the code under test).
- [ ] No test uses reflection on private members. `WebPackagesListenerPrecompressArgsTest` and `RunPNPMCommandTest` call public methods directly.
- [ ] `PackageCheckerJob.run` keeps the behaviour listed in 3.3.3, shown by the orchestration tests (command sequences and statuses per scenario) and by the manual smoke test in 4, step 12.
- [ ] Production changes are limited to visibility changes, extracted methods and seams with default implementations that delegate to the existing static calls. No functional change.
- [ ] No new compilation errors, no new SpotBugs findings of the two highest severities in changed code.

## 6. Out of scope
- TiNG Angular (TypeScript) coverage. It has its own Vitest coverage.
- Fixing bugs found while writing tests (e.g. zip-slip in `ZipUtils`, or a wrong status severity). File them as separate cases. Tests pin current behaviour, or are left out where the current behaviour is clearly wrong.
- Rewriting the npm/pnpm process handling into a general process-runner abstraction.
- UI classes with only SWT dialog code (`PnpmBuildApprovalsDialog`, `PnpmBuildApprovalPrompt` UI parts, `EditPnpmBuildApprovalsAction`, `CopySourceFolderAction`, `ConsoleFactory`, `EclipseIOConsole`), unless they turn out to be needed for the target.
- Adding an integrationpde run or Jenkins `-Pintegration` entry for `com.servoy.eclipse.ngclient.tests`.
- Writing the umbrella's servoy-eclipse "Test strategy" section in `AGENTS.md` (SVY-21836).

## 7. Open questions
| Question | Owner | Status |
|---|---|---|
| Is the `PackageCheckerJob.run` step extraction acceptable for 2026.12, given the risk to the Titanium build path? If not, the realistic ceiling for `ngclient.ui` is about 50–55% and the target needs lowering for this bundle. | J. Compagner / reviewer | open (approach approved in triage, confirm at code review) |
| Should the umbrella (SVY-21836) land its test-strategy rules first? If it lands during this case, the spec follows those rules. | umbrella owner | open |
| Also remove the reflection from `PnpmBuildScriptConfigTest` (make `parseModulesYamlIgnoredBuilds` / `parseIgnoredBuildsErrorLine` public)? Assumed yes if it is cheap, as it is the same rule. | implementer | open |
| Is `org.tukaani.xz` (or whatever `ZipUtils.extractTarXZ` uses) on the test bundle's classpath for building `.tar.xz` fixtures, or does a small checked-in fixture file work better? | implementer | open |
| Does the fake `pnpm`/`npm` script approach for `runCommand` work on the Linux Jenkins agents? If not, use the `ProcessBuilder` seam. | implementer | open |
| Is the optional integrationpde canary (3.6) wanted? | reviewer | open |
