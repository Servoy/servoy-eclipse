# Triage Report — SVY-21834

**Verdict:** PROCEED

## Reported problem
Not a bug: a test-debt task. Java line coverage of the two NG client bundles is below the 70% target set by the umbrella case SVY-21836 (Jenkins master/servoy-eclipse build 518, JaCoCo unit + integration merged):

| Bundle | Lines | Covered | Needed for 70% |
|---|---|---|---|
| `com.servoy.eclipse.ngclient` | 417 | 34.5% | +148 lines |
| `com.servoy.eclipse.ngclient.ui` | 3103 | 14.2% | +1732 lines |

Biggest gaps named in the ticket: `WebPackagesListener$PackageCheckerJob` (872 missed / 0 covered), `NodeFolderCreatorJob` (233/0), `RunPNPMCommand` (224/39), `WebPackagesListener` (182/86), `utils.TarInputStream` (165/0), `ngclient.ui.Activator` (143/47).

Proposed approach (from the ticket): plainjunit JUnit 6 + Mockito first, mock at the Eclipse/runtime boundary, small protected seam methods for static lookups, no reflection on private members, integrationpde only as a thin canary.

## Root-cause assessment
The low number is real and comes from the shape of the code, not from missing test infrastructure:

- **Test projects already exist and are wired.** `com.servoy.eclipse.ngclient.ui.tests` (plain plugin, `Require-Bundle` on `com.servoy.eclipse.ngclient.ui`, 13 plainjunit + 2 integrationpde classes, JaCoCo for both runs, in the Jenkinsfile `-Pintegration -pl` list). `com.servoy.eclipse.ngclient.tests` is a fragment of `com.servoy.eclipse.ngclient` with one plainjunit class (`NGClientWebsocketSessionFactoryTest`) and plainjunit JaCoCo wiring; it has no integrationpde run and is not in the Jenkins integration list. JaCoCo was added in `55a7afe04` / `c1a62e608`.
- **`PackageCheckerJob.run(IProgressMonitor)` is one ~915-line method** (`WebPackagesListener.java` lines 344–1259, introduced in `14febb667e`, J. Compagner, 2026-06-06; the disable guard at line 346 is `af87d1794a`). It reads the active project/solution, walks package readers, spawns npm/pnpm through `Activator.createNPMCommand`, writes files into the node folder and the console. There is no seam to drive it from a test, so it is at 0%.
- **The 70% target cannot be reached without covering `PackageCheckerJob`.** 70% of 3103 = 2172 covered lines. Without the 872 lines of `PackageCheckerJob`, 2231 lines remain, so ~97% of everything else would have to be covered. Any plan therefore has to make `PackageCheckerJob.run` testable, which means extracting steps from it (production refactor), not just adding seam getters.
- `NodeFolderCreatorJob.run` (~240 lines) and `Activator.extractNode`/`extractPath` (bundle-entry and archive extraction) have the same issue at smaller scale: they read from `Activator.getInstance().getBundle()` and the extension registry. `TarInputStream`/`TarEntry`/`ZipUtils` are pure stream code and are directly testable (`DeterministicTarballTest` already reads tarballs through `TarInputStream` but evidently does not hit the parsing branches enough to register; `ZipUtils.extractTarGz/Xz` is untested).
- `com.servoy.eclipse.ngclient` is small (7 classes). `NGClientWebsocketSessionFactory` and `FormPreviewNGClient` already have package-visible static routing helpers (`runRouting`, `shutdownRouting`, `selectClientKind`, `shouldReuseFormPreviewClient`) built for testing; `ResourceProvider` (531 source lines, a servlet filter) is the main uncovered piece and can be tested with mocked `HttpServletRequest`/`Response` plus a seam for the package-reader lookup.

## Ticket premise check
The approach in the ticket holds up, with three adjustments:

1. **Seams alone are not enough for `PackageCheckerJob`.** The ticket says "keep that a small, behaviour-preserving production change". For a 915-line `run()` the change is necessarily a step-extraction refactor (e.g. dependency collection, `package.json` rewrite, install/pack decision, template generation as separate package-visible methods). That is a larger production change than the ticket suggests and touches the Titanium build path used by every developer and WAR export, so it carries regression risk. This should be stated in the spec and reviewed as production code, not as "test-only".
2. **"No reflection" already conflicts with existing tests.** `WebPackagesListenerPrecompressArgsTest` calls `getDeclaredMethod("buildPrecompressArgs")` + `setAccessible(true)` on a package-private method. Because `com.servoy.eclipse.ngclient.ui.tests` is a separate plugin (not a fragment), package-private members are not visible from it in OSGi. Either make such helpers `public` (as the other extracted helpers in `WebPackagesListener` already are: `rewriteCssLibUrl`, `publicTarballName`, `preferConfiguredPnpm`, …) or accept this pattern. New seams/extracted steps must follow the public-static-helper pattern already used in this class.
3. **The umbrella's test-strategy text is not in `AGENTS.md` yet** (no "seam" / "Test strategy" section; SVY-21836 is still Open). This case can proceed using the rules in the ticket text, but the spec should follow whatever the umbrella writes if it lands first.

Also note: the "process runner" the ticket mentions for npm/pnpm does not exist as an abstraction. `RunPNPMCommand.runCommand`/`RunNPMCommand` build a `ProcessBuilder` inline; a `ProcessBuilder`-creating seam (or a fake `pnpm` script in a temp folder, which `PrecompressWarScriptTest` already does for node) is needed.

## Approaches considered
1. **Plainjunit-first with step extraction in `PackageCheckerJob` (ticket approach + refactor).** Extract `run()` into package-visible/public steps with seams for `Activator`, `ServoyModelManager`, `ResourcesPlugin` and command creation; unit-test each step against a `@TempDir` node folder. Pure tests for `TarInputStream`/`TarEntry`/`ZipUtils`, `NodeFolderCreatorJob` helpers (`copyOrCreateFile`, `createFolder`, `ignoredResource`, timestamp check), `RunPNPMCommand` argument translation/store-dir logic, `DirectorySync`, `IndexPageFilter`, `PnpmBuildApprovals*`, and for `ngclient` `ResourceProvider` with mocked servlet API. — Pros: fast, deterministic, meets the target, matches the ticket's rules. Cons: real production refactor of the Titanium build path; needs careful review and a manual Titanium build / WAR export smoke test.
2. **Integrationpde-heavy: run a real Titanium build in the PDE workbench.** — Pros: no production refactor; covers `PackageCheckerJob`/`NodeFolderCreatorJob` end to end. Cons: needs node/pnpm and network on Jenkins, minutes per run, flaky; explicitly against the ticket's "thin canary only" rule; assertions would be coarse.
3. **Hybrid: approach 1 for everything except the npm/pnpm process steps, plus one integrationpde canary that runs `PackageCheckerJob` with a stubbed command factory.** — Pros: smaller refactor than full extraction; one realistic run of the job. Cons: still needs the command-creation seam; coverage of the job body depends on how much the canary exercises.
4. **No code change.** — Pros: zero risk. Cons: the case exists to raise coverage under the umbrella; the Titanium build pipeline (the largest untested code here) has had several recent regressions-by-change (SVY-21502, SVY-21642, SVY-21734 commits in the last weeks) and would stay unguarded. Not appropriate.

## Recommendation
PROCEED with approach 1 (optionally adding the single canary of approach 3).

Suggested order, by lines gained per unit of risk:
1. Pure code first, no production change: `TarInputStream`/`TarEntry`/`ZipUtils` (~300 lines), `DirectorySync`, `IndexPageFilter`, `RunPNPMCommand.translateToPnpmArguments`/store-dir helpers (make them public static if needed), `NodeFolderCreatorJob` static helpers, `ngclient` `ResourceProvider`/`ComponentResourcesExporter`.
2. `Activator` (ngclient.ui) extraction paths with a seam for the bundle/extension lookup.
3. `PackageCheckerJob.run` step extraction with seams, tested step by step. This is the part that decides whether 70% is reachable; scope it explicitly in the spec and have it peer-reviewed as a production change.
4. Replace the reflection in `WebPackagesListenerPrecompressArgsTest` by making `buildPrecompressArgs` public, to comply with the ticket's no-reflection rule.

If the refactor of `PackageCheckerJob` is judged too risky for 2026.12, the realistic ceiling for `com.servoy.eclipse.ngclient.ui` without it is roughly 50–55%; that would need a decision to lower the target for this bundle.

## Git history findings
- `14febb667e` (2026-06-06, Johan Compagner): current form of `PackageCheckerJob.run`.
- `af87d1794a` (2026-08-27, Andrei Costescu): `Activator.isNodeExtractionAndTitaniumBuildDisabled()` guard at the top of `run()`; tests use it via `AbstractTitaniumIntegrationTest` to keep the Titanium build off during PDE runs.
- `55a7afe04`, `c1a62e608`: JaCoCo wiring for plainjunit and integrationpde (source of the baseline numbers).
- Recent changes to the same code: `cb6fedecb` SVY-21502 (reproducible tarballs, added `DeterministicTarball` + test), `f620cf826`/`979c368ad` SVY-21642 (precompress), `251313f97` SVY-21734 (pnpm path). They added public static helpers with plainjunit tests, which is the pattern to continue.
- No prior spec in `docs/` for SVY-21834. Sibling area cases SVY-21828…SVY-21835 are all Open.
