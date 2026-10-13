# Spec: SVY-21284 — PDE/plain-JUnit test infra consolidation and cross-repo launcher sharing (servoy-eclipse)

## 1. Goal

Make the PDE (full-workbench) integration tests across servoy-eclipse's `.tests` projects fast, reliable, and runnable from a single shared launcher — and keep that launcher usable by sibling repos (Servoy-Copilot) without requiring a live cross-repo Eclipse workspace or OSGi dependency. This is the servoy-eclipse half of a broader effort (started under SVY-21284 "faster integration tests") that also touches Servoy-Copilot; see that repo's own spec for its side.

## 2. Background

### 2.1 Starting point

Prior SVY-21284 work (see git history) already:
- Extracted Cypress testing into its own plugin (`com.servoy.eclipse.cypress` + `com.servoy.eclipse.cypress.tests`).
- Sped up PDE integration tests broadly: switched from fixed `Thread.sleep` waits to condition-based polling, disabled unnecessary Titanium/ng builds during JUnit runs via a system property + `NodeFolderCreatorJob.setDisabled(...)`, refactored common integration-test logic into shared base classes, added a dialog guard so an unexpected SWT dialog fails a test immediately instead of hanging the whole run, and fixed assorted flaky tests.
- Made the Jenkins job fail when tests fail/error instead of silently passing (previously only ~6 of 20+ test classes were even running on Jenkins).

### 2.2 Remaining gaps going into this session

- Multiple `.tests` projects (`com.servoy.eclipse.cypress.tests`, `com.servoy.eclipse.core.tests`, `com.servoy.eclipse.ngclient.ui.tests`, `com.servoy.eclipse.ui.tests`, `com.servoy.eclipse.tests`) mixed plain-JUnit (no OSGi/workbench) tests and PDE plug-in (full-workbench) tests in ways that weren't consistently separated by package, and most had no Maven/Tycho wiring to run the PDE ones in CI at all.
- No shared PDE JUnit Plug-in Test launch config existed that other `.tests` projects — in this repo or in Servoy-Copilot — could reuse without hand-rolling their own `.launch` file (duplicating VM args, bundle selections, Servoy-install wiring).
- `servoy-eclipse/Jenkinsfile` had no stage running the `-Pintegration` Maven profile at all, so none of the PDE integration tests ran on CI for this repo.
- Two sibling classes had naming inconsistencies: `com.servoy.eclipse.ngclient.ui.tests` used package `integrationode` instead of the convention's `integrationpde`, and a class was misspelled `AbstracTitaniumtIntegrationTestBaseTest`.
- An earlier design (same effort, different session) had `com.servoy.eclipse.developer.mcp.tests` in Servoy-Copilot `Require-Bundle` this repo's `com.servoy.eclipse.core.tests`/`com.servoy.eclipse.ngclient.ui.tests` to reuse shared PDE test-infra base classes. That only resolved in a live combined Eclipse workspace; Servoy-Copilot's actual Jenkins/Tycho build has no path to those bundles (`.tests` projects are correctly excluded from `com.servoy.eclipse.feature`'s p2 update site). Standing up a real cross-repo artifact-publishing pipeline for test-only bundles was judged not worth the infrastructure, so the dependency was removed in favor of duplicated, clearly-marked classes (see Servoy-Copilot's spec).

## 3. Design

### 3.1 `plainjunit` / `integrationpde` package convention

Every `.tests` project's tests are split into two top-level packages, which is what actually determines which runner picks a test up (not a class-name suffix):

- `<base>.plainjunit[.*]` — pure JUnit, no OSGi container, no live workspace. Run with `eclipse-ide_runJUnitTests` / plain `tycho-surefire-plugin`.
- `<base>.integrationpde[.*]` — needs a full Eclipse workbench + OSGi container (`ResourcesPlugin`, `Display`, `ServoyModelManager`, extension points), including tests whose own method bodies don't touch OSGi directly but exercise code that transitively does (e.g. an `Activator.getInstance()` that is `null` outside OSGi).

Applied (via package renames, mostly using Eclipse's `refactorRenamePackage`/`refactorRenameJavaType` to keep all references correct) to:
- `com.servoy.eclipse.cypress.tests` (`cypress.plainjunit*` / `cypress.integrationpde`)
- `com.servoy.eclipse.core.tests` (shared base classes relocated to `core.tests.integration.utils`, non-fragment plugin, see 2.1 background)
- `com.servoy.eclipse.ngclient.ui.tests` (`ngclient.ui.tests.plainjunit` / `ngclient.ui.tests.integrationpde[.utils]` — renamed from the inconsistent `integrationode`)
- `com.servoy.eclipse.ui.tests` (`ui.tests.plainjunit[.*]` / `ui.tests.integrationpde[.*]`)
- `com.servoy.eclipse.tests` (`tests.plainjunit.designer.util`)

Also fixed the `AbstracTitaniumtIntegrationTestBaseTest` → `AbstractTitaniumIntegrationTestBaseTest` class-name typo in `com.servoy.eclipse.ngclient.ui.tests` via `refactorRenameJavaType`.

### 3.2 Maven/Tycho wiring per `.tests` project

Each of the five `.tests` projects above got matching `pom.xml` wiring (mirroring the pattern already used by Servoy-Copilot's `com.servoy.eclipse.developer.mcp.tests`):

- Default-build `tycho-surefire-plugin` execution with `<includes><include>**/plainjunit/**/*Test.java</include></includes>` — what `mvn clean verify` runs, no OSGi/Tomcat needed.
- An `integration` Maven profile (`-Pintegration`) with its own `tycho-surefire-plugin` execution: `<includes><include>**/integrationpde/**/*Test.java</include></includes>`, `useUIHarness=true`, `useUIThread=false`, `product=com.servoy.eclipse.core.ide`, `application=org.eclipse.ui.ide.workbench`, `deleteOsgiDataDirectory=false` (workspace reuse between runs — see 3.5), and a `maven-antrun-plugin` step (`prepare-servoy-install`) that extracts a real Servoy install from `${servoy.install.tarball.for.appserver}` into `testresources/servoy_lts_extracted` before tests run (no-op locally unless that tarball exists on disk, i.e. on Jenkins).
- A project-local `testresources/servoy.properties` the integration run's `-Dproperty-file` points at.

Also added `servoy.install.tarball.for.appserver` as a property on the root `servoy-eclipse/pom.xml` (`/data/www/latest/servoy_lts_2026/servoy_linux.tar.gz`, matching Servoy-Copilot's existing value) so the new antrun steps have it available without per-project duplication.

### 3.3 Shared cross-repo PDE launcher: `PDEIntegrationTestsBase`

A single saved JUnit Plug-in Test launch config, `com.servoy.eclipse.core.tests/PDEIntegrationTestsBase.launch`, replaces the various ad-hoc/duplicated `.launch` files that used to live in Servoy-Copilot (`AllDeveloperMcpIntegrationTests.launch`, its `_mac` variant, and a stale draft `AllEclipseCypressPDEtests.launch`).

- Lives in `com.servoy.eclipse.core.tests` (the project most other `.tests` projects already depend on for shared infra), not in any one feature-specific `.tests` project, so it reads as neutral/shared rather than owned by one feature.
- Carries the **full** `selected_workspace_bundles`/`selected_target_bundles` sets inherited from the old `AllDeveloperMcpIntegrationTests` launcher — deliberately not trimmed to one project's needs, so other `.tests` projects reusing it are likely to already have their dependencies covered.
- Its checked-in default target is `com.servoy.eclipse.ui.tests`, package `com.servoy.eclipse.ui.tests.integrationpde` (`TEST_KIND=junit6`). Any caller — human or agent — overrides `projectName`/`packageName`/`className` per-run via the JUnit Plug-in Test launch tooling rather than editing the saved file.
- Servoy-Copilot keeps its own copy, `PDEIntegrationTestsBaseCopy.launch`, differing only in its `-Dproperty-file` path (that repo's own `testresources/servoy.properties`) and whatever project/package it's checked in pointing at. The two files are expected to be kept in sync for everything else (bundle selections, VM args, structure) — see this repo's `AGENTS.md`.
- **Agent guidance:** if a target project's tests aren't found when reusing this launcher, try passing `junitVersion` explicitly (`"4"` or `"6"`) — auto-detection depends on the target project's actual JUnit version on its build path. (A separate pitfall observed in practice: pointing `packageName` at a `*.integrationpde.utils` sub-package that holds only shared base/utility classes with no `@Test` methods reports zero tests, which looks like a failure but is caller error, not a tooling bug.)

### 3.4 Jenkins CI

Added an "Integration Tests" stage to `servoy-eclipse/Jenkinsfile`, mirroring Servoy-Copilot's existing stage shape:

```groovy
stage('Integration Tests') {
    steps {
        wrap([$class: 'Xvfb', installationName: 'xvfb', autoDisplayName: true]) {
            configFileProvider([...]) {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    sh 'mvn -B -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" verify -Pintegration -pl com.servoy.eclipse.core.tests,com.servoy.eclipse.cypress.tests,com.servoy.eclipse.ngclient.ui.tests,com.servoy.eclipse.ui.tests,com.servoy.eclipse.tests -am'
                }
            }
        }
    }
}
```

Runs after the existing "Build with Tycho 5" stage, on the assumption the Jenkins node is Linux (Xvfb available) like Servoy-Copilot's.

### 3.5 Workspace-not-cleared-between-runs (unverified)

Carried over as *intent only* from the prior session: `deleteOsgiDataDirectory=false` in each `.tests` project's integration profile, and per-test-class `@BeforeClass` cleanup (`deleteProjects(...)`) so each class starts from a known state while still reusing node/cypress caches under `.metadata` across runs. The actual Jenkins job configuration (e.g. "Wipe out workspace" checkbox) was not inspected — flagged as still unverified.

## 4. Implementation plan

1. Rename `com.servoy.eclipse.cypress.tests`' test packages into `plainjunit`/`integrationpde` (already in flight from a prior session; completed/verified this session).
2. Fix `com.servoy.eclipse.ngclient.ui.tests`' `integrationode` → `integrationpde` package naming (via `refactorRenamePackage`) and the `AbstracTitaniumtIntegrationTestBaseTest` typo (via `refactorRenameJavaType`).
3. Split `com.servoy.eclipse.ui.tests` and `com.servoy.eclipse.tests` into `plainjunit`/`integrationpde` packages.
4. Add matching `tycho-surefire-plugin` default + `-Pintegration` profile wiring to the `pom.xml` of all five `.tests` projects (`core.tests`, `cypress.tests`, `ngclient.ui.tests`, `ui.tests`, `tests`).
5. Normalize `Bundle-Version`/`<version>` mismatches introduced by earlier ad-hoc edits (`2026.6.0`/`2026.9.0` → `2026.3.2`, matching the parent POM) in `core.tests`, `ui.tests`, `ngclient.ui.tests`.
6. Create `com.servoy.eclipse.core.tests/PDEIntegrationTestsBase.launch` (adapted from the deleted `AllDeveloperMcpIntegrationTests.launch`), verify it against at least one real target via `runJUnitPluginTests`.
7. Add `servoy.install.tarball.for.appserver` to the root `pom.xml`.
8. Add the "Integration Tests" stage to `Jenkinsfile`.
9. Document the `plainjunit`/`integrationpde` convention, the shared launcher, and the (now-removed) cross-repo `Require-Bundle` approach in `AGENTS.md`.
10. Verify: compile clean across all five `.tests` projects; at least one full `-Pintegration` PDE run per project family via the Eclipse MCP JUnit Plug-in Test tooling (`com.servoy.eclipse.ui.tests`: 87/87 passed).

## 5. Acceptance criteria

- [x] `com.servoy.eclipse.core.tests`, `com.servoy.eclipse.cypress.tests`, `com.servoy.eclipse.ngclient.ui.tests`, `com.servoy.eclipse.ui.tests`, `com.servoy.eclipse.tests` each have `plainjunit`/`integrationpde` top-level test packages with no mixing.
- [x] Each of those five projects has a `pom.xml` default-build + `-Pintegration` profile split matching the package convention.
- [x] `com.servoy.eclipse.core.tests/PDEIntegrationTestsBase.launch` exists, is runnable via `eclipse-pde_runJUnitPluginTests`, and a real run against it passes (verified: `com.servoy.eclipse.ui.tests` 87/87).
- [x] `servoy-eclipse/Jenkinsfile` has an "Integration Tests" stage running `-Pintegration` for all five projects.
- [x] All five projects compile with 0 errors.
- [ ] A full `mvn verify -Pintegration` run across all five projects on the real Jenkins node (not just via Eclipse PDE launches) — not run this session; local `mvn` resolution failed on an unrelated pre-existing `.m2` target-platform artifact issue. Left for the user to verify on Jenkins directly.
- [ ] Jenkins "Wipe out workspace" / workspace-persistence behavior — not verified against actual job config.

## 6. Out of scope

- Setting up a real artifact-publishing pipeline so Servoy-Copilot could `Require-Bundle` this repo's `.tests` projects in its own Tycho/Jenkins build (considered, explicitly rejected as not worth the infrastructure for test-only bundles).
- Fixing the pre-existing local `.m2` resolution issue that blocked a local `mvn verify` run for `com.servoy.eclipse.cypress.tests` (`com.servoy:com.servoy.eclipse.target:target:2026.3.2-SNAPSHOT` not found in the local repo) — unrelated to this work, flagged for the user.
- macOS launch-config variants (`_mac` suffix) for the new shared launcher — intentionally left for a colleague with Mac access, per explicit decision.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `PDEIntegrationTestsBase`'s checked-in default target move off `com.servoy.eclipse.ui.tests` to something more "neutral" (e.g. back to Cypress, or a dedicated no-op placeholder)? | user | resolved — keep `ui.tests` as the default |
| Jenkins node OS/Xvfb availability for `servoy-eclipse` assumed same as Servoy-Copilot (Linux) — needs confirmation on the actual Jenkins job config | user | open, user said they'll check |

