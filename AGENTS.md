# AGENTS.md - Servoy Developer Eclipse IDE

## Project Overview

This is the **Servoy Developer IDE** source code — a large Eclipse RCP application built as a multi-module Maven/Tycho project. It consists of ~40+ OSGi plugin bundles covering the IDE core, form designers (with Angular frontends), NG client, exporters, AI assistant integration, and platform-specific runtime bundles.

- **Version:** 2026.6.0-SNAPSHOT
- **Java version:** 21
- **Build system:** Maven 3.9.0+ with Eclipse Tycho 4.0.12
- **License:** AGPL v3 (compatible with all open source licenses except GPL)
- **Base platform:** Eclipse 2025-12

## Tool Usage Policy (MCP Servers)

This project has Eclipse MCP servers configured in `opencode.json`. **Always prefer the MCP server tools over built-in tools** for the operations below.

### ⚠️ Everything runs through Code Mode — READ THIS FIRST

The Eclipse MCP servers (`eclipse-coder`, `eclipse-ide`, `eclipse-git`, `eclipse-pde`, `eclipse-runner`, `eclipse-context`) and the other MCP tools (`memory`, `time`, the graph/search tools) are exposed **only through Code Mode**. There is **no direct top-level tool** for any of them.

- To call any Eclipse/MCP tool you MUST write JavaScript inside the **`execute`** tool and call the tool by its exact catalog `path`, using bracket notation:
  - `await tools["eclipse-coder"].replaceString({ ... })`
  - `await tools["eclipse-ide"].getCompilationErrors({ ... })`
  - `await tools["eclipse-git"].gitStatus({ ... })`
- **NEVER** call these as if they were plain tools (e.g. `eclipse-coder_replaceString`, `eclipse-ide_getCompilationErrors`, or `tools.eclipse_ide.getCompilationErrors`). Those names do **not** exist in Code Mode; the call fails with *"No tool named ... is currently available."* When that happens, **do not fall back to the built-in `edit`/`write`** — fix the call by wrapping it in `execute` with the bracket form instead.
- The Code Mode catalog is **partial**. If a tool is not shown, find it with `search(...)` **inside** an `execute` script (it is synchronous — call it without `await`), then call it by the returned `path`. Do not guess tool names.
- Throughout the rest of this document, whenever a tool is written as `eclipse-coder_replaceString` or `eclipse-ide_getCompilationErrors`, read it as shorthand for `tools["eclipse-coder"].replaceString(...)` / `tools["eclipse-ide"].getCompilationErrors(...)` called via `execute`.

**The only tools called directly (not through Code Mode):** the built-in `read`, `grep`, `glob`, and `shell`. Use them only for the narrow cases noted below. Everything else goes through `execute`.

### File Operations — MANDATORY

**Every file inside an Eclipse workspace project MUST be edited through the `eclipse-coder` tools**, never the built-in `edit`/`write` tools. The built-in tools write straight to disk behind Eclipse's back, so the open editor, the JDT model, incremental compilation, and local-history undo all drift out of sync. This is a hard rule, not a preference.

- Single targeted replacement → `tools["eclipse-coder"].replaceString`; multi-hunk → `applyPatch` (or `replaceFileContent` for a full rewrite); insert/delete lines → `insertIntoFile`/`deleteLinesInFile`; new file → `createFile`; delete/rename → `deleteFile`/`renameFile`.
- **Reading Java source:** `tools["eclipse-ide"]` — `readProjectResource`, `getSource`, `getFilteredSource`, `getMethodSource`, `getClassOutline`.
- **Searching code:** `tools["eclipse-ide"]` — `fileSearch`, `fileSearchRegExp`, `findFiles`, `findReferences`. Use the built-in `grep`/`glob` only for non-code files or when Eclipse search returns nothing.

The built-in `edit`/`write` are acceptable **only** for files that are NOT inside any Eclipse project — repo-root docs (`AGENTS.md`, `README.md`), CI YAML, `opencode.json`, shell scripts. When in doubt (file under `com.servoy.*/` etc.), use `eclipse-coder`.

### After Every Code Change
1. **Always call `tools["eclipse-ide"].getCompilationErrors`** after modifying code to check for compilation errors.
2. If errors are found and have quick fixes available, **use `tools["eclipse-ide"].executeQuickFix`** to resolve them automatically.
3. **Use `tools["eclipse-coder"].organizeImports`** to fix import issues after edits.
4. **Spotbugs:** Spotbugs errors of the **two highest severity levels** are treated as blocking errors. Always try to fix these in any new or modified code to keep the codebase robust and clean.

### Long-running operations (all Eclipse servers)

Builds, tests, launches and refactors run asynchronously. Every Eclipse server exposes `listOperations`, `getOperationStatus`, and `cancelOperation`. When a tool returns an `operationId`, poll `tools["<server>"].getOperationStatus({ operationId })` (via `execute`) until it finishes.

### Git Operations

> **ALWAYS ASK PERMISSION BEFORE EVERY COMMIT. EVERY COMMIT, EVEN IN THE SAME SESSION.**
>
> Permission is **per commit, and never implied**. It does not carry over:
> - An earlier "commit and push" authorises **that one commit only** — not the next one.
> - Approval to *change code* (answering a design question, picking an option, saying "fix it")
>   is **not** approval to commit it.
> - Ask for **commit** and **push** separately. Never push on the strength of a "yes" to committing.
> - Before each commit: show the staged file list and the proposed message, then wait.
> - Pushing to a shared branch (`master`, `release`, `lts_2026`, …) always needs its own explicit yes.
> - Never force-push or rewrite pushed history without the user confirming, in that moment, that it is safe.

- **Use `tools["eclipse-git"]`** (`gitStatus`, `gitDiff`, `gitAdd`, `gitCommit`, `gitLog`, `gitShow`, `gitReadFile`, `gitBranch`, branch/stash/tag ops, etc.) instead of command-line git.
- **After every `gitCommit`**, display the full commit message (subject line + body) in a formatted block so the user can verify the naming and content before moving on.

### Running and Debugging
- **Use `tools["eclipse-runner"]`** for launching, debugging, and testing Java applications (`runJavaApplication`, `debugJavaApplication`, breakpoints, stepping, `evaluateExpression`, `hotCodeReplace`).
- **Use `tools["eclipse-pde"]`** for PDE-specific operations (`runJUnitPluginTests`, `getActiveTarget`/`setActiveTarget`, `reloadTarget`, `reloadWorkspaceBundle`).

### Testing
- **Use `tools["eclipse-ide"].runJUnitTests`** for plain JUnit tests (use `findTestClasses` to discover them).
- **Use `tools["eclipse-pde"].runJUnitPluginTests`** for plugin integration tests.
- Test project: `com.servoy.eclipse.tests`

### Other Tools
- **Formatting:** `tools["eclipse-coder"].formatFile` or `tools["eclipse-ide"].formatCode` after editing.
- **Use `tools["eclipse-context"]`** for workspace context, file history (`getFileHistory`, `restoreFileVersion`), and cached resources.
- **Use `tools.time`** for time-related operations.

## Workflow for Code Changes

All Eclipse tool calls below go through the **`execute`** tool using the bracket notation shown (never as direct `eclipse-*_*` tools, never the built-in `edit`/`write`).

```
1. Read/understand code:      tools["eclipse-ide"].getClassOutline / getMethodSource / getFilteredSource
2. Make changes:              tools["eclipse-coder"].replaceString / applyPatch / insertIntoFile / createFile
3. Organize imports:          tools["eclipse-coder"].organizeImports
4. Format file:               tools["eclipse-coder"].formatFile
5. Check errors:              tools["eclipse-ide"].getCompilationErrors
6. If errors have quick fixes: tools["eclipse-ide"].executeQuickFix
7. If errors remain: fix manually (again via eclipse-coder) and repeat from step 5
8. Run relevant tests:        tools["eclipse-ide"].runJUnitTests or tools["eclipse-pde"].runJUnitPluginTests
```

## Project Structure

### Core Plugins
| Module | Purpose |
|--------|---------|
| `com.servoy.eclipse.core` | Main plugin, launch configs, schemas |
| `com.servoy.eclipse.model` | Data model layer |
| `com.servoy.eclipse.ui` | UI components |
| `com.servoy.eclipse.ui.tweaks` | UI customizations/icons |
| `com.servoy.eclipse.debug` | Debugger support |
| `com.servoy.eclipse.cloud` | Cloud integration |

### Designer Plugins
| Module | Purpose |
|--------|---------|
| `com.servoy.eclipse.designer` | Form designer |
| `com.servoy.eclipse.designer.rfb` | RFB designer (Angular frontend in `node/`) |
| `com.servoy.eclipse.designer.rib` | RIB designer (legacy) |
| `com.servoy.eclipse.designer.wpm` | Web Package Manager (Angular frontend in `node/`) |

### Client Plugins
| Module | Purpose |
|--------|---------|
| `com.servoy.eclipse.ngclient` | NG Client support |
| `com.servoy.eclipse.ngclient.ui` | NG Client UI (Angular workspace in `node/`) |

### Exporters
| Module | Purpose |
|--------|---------|
| `com.servoy.eclipse.exporter.solution` | Solution exporter |
| `com.servoy.eclipse.exporter.war` | WAR exporter |
| `com.servoy.eclipse.exporter.ngdesktop` | NG Desktop exporter |
| `com.servoy.eclipse.exporter.mobile` | Mobile exporter |

### AI/Pilot
| Module | Purpose |
|--------|---------|
| `com.servoy.eclipse.servoypilot` | AI assistant UI |
| `com.servoy.eclipse.servoypilot.langchain4j` | LangChain4j integration |
| `com.servoy.eclipse.aibridge` | AI bridge |

### Product/Feature
| Module | Purpose |
|--------|---------|
| `com.servoy.eclipse.feature` | Eclipse feature definition |
| `com.servoy.eclipse.product` | Product definition |

### Platform Bundles
- `com.servoy.eclipse.jre.*` — Bundled JREs per platform
- `com.servoy.eclipse.nodejs.*` — Bundled Node.js per platform

## Build

### Maven Profiles
| Profile | Command | Purpose |
|---------|---------|---------|
| `plugins` (default) | `mvn clean verify` | Build all plugin modules |
| `product` | `mvn clean verify -Pproduct` | Build full product with JREs/Node.js |
| `only_product` | `mvn clean verify -Ponly_product` | Build just feature and product |
| `target` | `mvn clean verify -Ptarget` | Build target platform definition |

### Target Platforms
Located in `launch_targets/`:
- `com.servoy.eclipse.target.target` — Main target (Eclipse 2025-12, GEF, NatTable, Nebula, Chromium/CEF)
- `eclipse_local.target` — Local development target
- `open_source.target` — Open source target

## Spec / Design Documents

Feature specs and design documents live in **`docs/`** at the repository root.

- Name files after the Jira case with a `.spec.md` extension: `docs/SVY-21080-embedded-opencode.spec.md`
- Never place spec files inside a plugin or module subdirectory.
- When asked to write a spec, always create it in `docs/` unless explicitly told otherwise.

## Jira API

When asked to create, update, or link Jira issues, use the `servoy-jira` skill (global opencode skill).

## Code Style & Conventions

- Follow existing code style and conventions for each language and module
- Java: standard Eclipse plugin conventions, OSGi declarative services
- TypeScript/Angular: follows Angular CLI conventions in `node/` subdirectories
- No hardcoded secrets, credentials, or proprietary information
- All code must be compatible with open source licenses (except GPL)
- **Commit messages:** When the code is mostly AI-generated, the commit subject line must end with `[ai]`
- **Commit messages for cases:** When a commit is related to a Jira case, the case number (e.g. `SVY-123`, `SVYX-456`, `SERVOY-293`) must be included in the commit subject line. Example: `SERVOY-293 fix NPE in WAR export copyRequiredBundles [ai]`

## Testing

- **Java plugin tests:** `com.servoy.eclipse.tests` (eclipse-test-plugin packaging)
- **UI property tests (SVY-21432):** `com.servoy.eclipse.ui.tests` fragment — `com.servoy.eclipse.ui.property.RepositoryHelperShouldShowNameTest` (Form `name` shown again, other props still suppressed) and `com.servoy.eclipse.ui.property.IdentDocumentValidatorFormNameTest` (form-name validation rule the setter enforces)
- **Cypress plugin tests:** `com.servoy.eclipse.cypress.tests` — fragment of `com.servoy.eclipse.cypress`. Split by naming convention (below); some classes are pure-logic unit tests, others need the OSGi/workbench and must run as PDE plug-in tests.
- **Angular tests:** `com.servoy.eclipse.ngclient.ui/node/run_tests.bat`
- **LFC per-row auto-height (SVY-21457):** `com.servoy.eclipse.ngclient.ui/node/src/servoycore/listformcomponent/listformcomponent.spec.ts` and `row-renderer.component.spec.ts` (Jasmine/Karma) — asserts that native AG Grid `autoHeight` is disabled only for the per-row auto-height path (responsive, `responsiveHeight < 0`) and unchanged elsewhere; that `getRowHeight`/`applyMeasuredRowHeight` feed each row's explicitly-measured height back into AG Grid's server-side row model; the `visibility: hidden` anti-flicker reveal; the hardened resize-observer guard (column-count only, immune to scrollbar-driven width jitter); and (in `row-renderer.component.spec.ts`) `RowRenderer`'s per-row measurement via `ngAfterViewInit` and the recursive float-collapse-safe `measureContentHeight`
- **Designer RFB tests:** `com.servoy.eclipse.designer.rfb/node/src/test.ts`
- **WPM tests:** `com.servoy.eclipse.designer.wpm/node/src/test.ts`

### Test types, naming convention & how to Run Tests

Each test project (e.g `/com.servoy.eclipse.cypress.tests`, `/com.servoy.eclipse.ngclient.ui.tests`, ...) has it's tests split between plain JUnit tests and plugin PDE integration JUnit tests.

- if you want to run **all the tests** in a project, use the plugin PDE integration JUnit test way of running (as the plain tests should run fine with that as well) as specified below and give only the project as a target parameter to the MCP tool.
- **suite classes are not used**: there are no `@Suite`/`AllXxxTests` aggregator classes in these projects anymore — so that they don't go out of date & running a package or the whole project directly executes each test class exactly once
- all tests use **JUnit 5/6 (Jupiter) APIs**
- **Package placement is based on what the code under test needs, not what the test method itself calls directly.** `SkillsZipExtractorTest` lives in `integrationpde` even though its own test methods don't touch OSGi APIs — they can transitively call `com.servoy.eclipse.core.Activator.getInstance()` for example, which returns `null` outside a running OSGi container, so the test only passes under the PDE/workbench runner. When deciding where a test belongs, check what the code it exercises needs, not just the test body.
- **Maven wiring:** has the same `plainjunit`/`integrationpde` tycho-surefire split and `-Pintegration` profile as `com.servoy.eclipse.developer.mcp.tests` (see "Maven wiring" below), and is included in the Jenkinsfile's `-Pintegration -pl` list.

**Polling:** Tests could take a long time to run. If the tool times out saying the tests are still running, poll with `eclipse-pde_getOperationStatus` using increasing wait times (e.g. 5 s, 10 s, 20 s, you decide) to reduce token usage between checks. While an operation is still running, `getOperationStatus` also surfaces intermediate `summary` and `results` — you can request those to see if everything is ok using `includeResults` to see how many tests have passed/failed so far, without waiting for the whole run. You can also check console output and the progress of what was already tested.

#### About the tests splitting by type
Tests in a `<plugin>.tests` project fall into two categories that use **different runners** — the top-level **package** tells you which one (not a class name suffix):

| Test type | Package | Runner | Needs |
|-----------|---------|--------|-------|
| Unit test (pure logic) | `<base>.plainjunit[.*]` | `eclipse-ide_runJUnitTests` | Nothing — no OSGi container, no live workspace |
| Integration test | `<base>.integrationpde[.*]` | `eclipse-pde_runJUnitPluginTests` | Eclipse workbench + OSGi container; so a Servoy Developer should be started (e.g. `ResourcesPlugin`, `Display`, `ServoyModelManager`, extension points) |

e.g. for `com.servoy.eclipse.cypress.tests`: plain tests live under `com.servoy.eclipse.cypress.plainjunit` (and subpackages like `.plainjunit.services`), PDE tests under `com.servoy.eclipse.cypress.integrationpde`. The test-executing `.tests` projects in this repo (`com.servoy.eclipse.cypress.tests`, `com.servoy.eclipse.ngclient.ui.tests`, `com.servoy.eclipse.ui.tests`, `com.servoy.eclipse.tests`) follow this split, each with its own `pom.xml` wiring (see "Maven wiring" below). A class's own name suffix (`*Test` vs `*IntegrationTest`) is just a naming habit carried over from before the split — the package is what actually determines which runner picks it up, since the Tycho `tycho-surefire-plugin` `<includes>` patterns match on package path, not class suffix. (One exception: `com.servoy.eclipse.tests` keeps two legacy `Test*`-prefixed plain tests, `TestCssValues`/`TestSnapCSSPosition`, so its `plainjunit` include carries an extra `**/plainjunit/**/Test*.java` pattern on top of the usual `*Test.java` one — the other projects only match `*Test.java`.)

**`com.servoy.eclipse.core.tests` is NOT one of these — it is not a test-executing project.** It is a plain `eclipse-plugin` bundle (not `eclipse-test-plugin`) that holds only shared PDE integration-test infrastructure in `com.servoy.eclipse.core.tests.integration.utils` (`TestUtilitiesClass`, `ServoyRunnerTestBase`, `DialogGuardRule`, `DialogGuardBase`, `DialogExpectation`, `TestDialogInterceptor`), which the real `.tests` projects consume via `Require-Bundle`. It has no `plainjunit`/`integrationpde` packages, no tests of its own, no tycho-surefire config and no `integration` profile, and is therefore left out of the Jenkinsfile's `-Pintegration -pl` list (it is still built as a dependency via `-am`). Don't try to run tests "in" it.

> **If actual tests are ever added to `com.servoy.eclipse.core.tests`** (i.e. a real `com.servoy.eclipse.core.tests.plainjunit` and/or `...integrationpde` package with `@Test` classes), it must be turned back into a test-executing project. It was deliberately demoted to a plain bundle because it only held shared infra; the moment it carries real tests, revert that. To do so:
> 1. In `com.servoy.eclipse.core.tests/pom.xml`: change `<packaging>eclipse-plugin</packaging>` back to `<packaging>eclipse-test-plugin</packaging>` and restore the `tycho-surefire-plugin` config (with `<providerHint>junit6</providerHint>`, the `**/plainjunit/**/*Test.java` include, the `com.servoy.eclipse.extension.compatibility` dependency) plus the whole `integration` profile (the `maven-antrun-plugin` `prepare-servoy-install`/`create-artifacts-xml` executions and the second `tycho-surefire-plugin` with the `**/integrationpde/**/*Test.java` include and the full `argLine`). Copy the shape from any sibling such as `com.servoy.eclipse.cypress.tests/pom.xml` — they are all structurally identical.
> 2. In the `Jenkinsfile`, add `com.servoy.eclipse.core.tests` back into the `-Pintegration -pl …` module list of the "Integration Tests" stage.
> 3. Update this section (and the project list two paragraphs up) to move `com.servoy.eclipse.core.tests` back into the set of test-executing projects.
> Keep `MANIFEST.MF` as-is either way — it already exports `integration.utils` and that stays correct.

**Decision rule:** if the test needs `Platform.getBundle()`, the Eclipse workspace, `ServoyModel`/`ServoyModelManager`, `Display`/SWT, or extension-point contributions, or calls code that transitively needs any of those (e.g. an `Activator.getInstance()` that is `null` outside OSGi) → it belongs in `integrationpde`. Otherwise it's a `plainjunit` test. Never mix the two kinds in the same package.

### Plain Junit tests are located in a package with **`.plainjunit`** subfix and it's subpackages

These tests use no live Eclipse workspace or OSGi container — pure Java, reflection, and mocking.

**Runner:** Plain JUnit tool (`eclipse-ide_runJUnitTests`). When you want to run only plain JUnit tests, it is much faster to use the plain JUnit runner.
**Target to test** give it the `projectName` and the `packageName` or `className` or ... that you want to test, according to the available parameters. When running all tests in a package, make sure you pick the correct parent package to run (don't give it packages like `com.servoy.eclipse.core.tests.integration.utils` if they have no tests, just utility methods). For example if you want to run all plain junit tests from project `com.servoy.eclipse.cypress.tests` then give the project and package `com.servoy.eclipse.cypress.plainjunit` as parameters.

#### Troubleshooting: JUnit 6 `NoSuchMethodError` (e.g. `Namespace.getParts()`)

If a JUnit 6 (Jupiter) test fails at runtime with:
```
java.lang.NoSuchMethodError: 'java.util.List org.junit.jupiter.api.extension.ExtensionContext$Namespace.getParts()'
```

**Root cause:** The `org.eclipse.dltk.javascript.rhino` workspace project (or others) used to bundle an older `junit-jupiter-api-5.x.jar` in its `test-libs/` folder. When PDE builds the flat classpath for a plain JUnit launch, it includes all workspace project dependencies — including rhino's test-libs. The older 5.x API jar appears earlier on the classpath than the target platform's 6.x jar, causing version conflicts.
**Fix:** Open `org.eclipse.dltk.javascript.rhino` → Java Build Path → Libraries tab → remove or uncheck the `test-libs/junit-jupiter-api-*.jar`, `test-libs/junit-4.*.jar`, and `test-libs/hamcrest-*.jar` entries. Alternatively, remove the `test-libs` folder from rhino's `.classpath` entirely. The test sources can also be removed from the Source tab if rhino tests are not being developed.

### Plugin PDE integration JUnit tests are located in a package with **`.integrationpde`** subfix and it's subpackages

These tests require a full Eclipse workbench (Servoy Developer, that also includes the Servoy App Server). They use `ResourcesPlugin`, `Display`, `ServoyModelManager`, etc.

**Runner:** Plugin PDE JUnit tool (`eclipse-pde_runJUnitPluginTests`).

**Parameters:**
- **Use the existing launch configuration as `launcherName`: `/com.servoy.eclipse.core.tests/PDEIntegrationTestsBase.launch`** (or a copy of it from the same place that targets the correct OS you are running); that should start-up correctly a test Servoy Developer. It takes a while to start, but then it should have all that is needed for plugin PDE intgration JUnit tests operational. You can use `eclipse-runner_listLaunchConfigurations` (with `typeFilter="junit-plugin"`) to discover the available saved launch configurations. On Windows, use the configuration **without** a `_mac` suffix.
- **Target to test** give it just the `projectName` to run all tests in that project (both plainjunit and integrationpde), or the `projectName` and the `packageName` or `className` or ... that you want to test, according to the available parameters. When running all tests in a package, make sure you pick the correct parent package to run. For example if you want to run all integrationpde junit tests from project `com.servoy.eclipse.cypress.tests` then give the project and package `com.servoy.eclipse.cypress.integrationpde` as parameters. When you only need to verify a specific class after a code change, pass `launcherName` + `projectName` + `className`
- There are other parameters that can be used to tweak the launcher if needed.

**Polling:** these tests start the Servoy Developer (integrationpde ones) & typically take 60–120 s or more due to a long time starting it up - and the initial MCP call will likely time out before they finish. So use the polling mechanism described above.

### When working on junit tests
**No `Assume.*` to skip tests.** A test that silently passes because a precondition (app server, Servoy project, workspace state, etc.) was not met provides zero value. If the setup is missing, fix the setup/launcher instead of skipping the test — a test must always exercise and assert a specific expected behaviour, not "did something happen" in general.

**No green-for-the-sake-of-green tests.** Every assertion must fail if the code under test is broken. Reject assertions that accept multiple outcomes (e.g. `result.contains("passed") || result.contains("failed")`) — assert the one specific outcome the scenario should produce.

### Maven/Tycho wiring for the `plainjunit`/`integrationpde` split

Each `.tests` project's `pom.xml` has:
- A default-build `tycho-surefire-plugin` with `<includes><include>**/plainjunit/**/*Test.java</include></includes>` — this is what `mvn clean verify` (and Jenkins' main build stage) runs. No OSGi/Tomcat/app-server needed.
- An `integration` Maven profile (`-Pintegration`) with its own `tycho-surefire-plugin` execution, `<includes><include>**/integrationpde/**/*Test.java</include></includes>`, `useUIHarness=true`, a real `product`/`application` (full workbench), and a `maven-antrun-plugin` step that extracts a real Servoy install from a tarball (`${servoy.install.tarball.for.appserver}`) into `testresources/servoy_lts_extracted` before the tests run (only when that tarball exists on disk — a no-op locally unless you have it).
- A project-local `testresources/servoy.properties` the integration run points `-Dproperty-file` at.

Run plain tests: `mvn clean verify -pl <project>`.
Run integration tests: `mvn verify -Pintegration -pl <project> -am`.
But when developing, use the eclipse MCP tools mentioned above to run tests instead.

### `PDEIntegrationTestsBase` — the shared cross-repo PDE integration-test launcher

`com.servoy.eclipse.core.tests/PDEIntegrationTestsBase.launch` is a saved JUnit Plug-in Test launch config meant to be **reused by any `.tests` project in this repo or in Servoy-Copilot that needs a full-workbench PDE integration-test run** (Tomcat/app-server startup, full target platform + workspace bundle selection), instead of every project hand-rolling its own `.launch` file from scratch.

- It currently targets `com.servoy.eclipse.ui.tests`, package `com.servoy.eclipse.ui.tests.integrationpde` (`org.eclipse.jdt.junit.CONTAINER` set to that package, `TEST_KIND` set to `junit6`). To point it at a different project/package temporarily, pass `launcherName="PDEIntegrationTestsBase"` plus `projectName`/`className`/`packageName` overrides to `eclipse-pde_runJUnitPluginTests` — do **not** edit and save over the checked-in file for a one-off run. Make sure `packageName` points at the actual `*.integrationpde` test package, not a sub-package that only holds shared base/utility classes (e.g. `...integrationpde.utils`) — that has no `@Test` classes and reports zero tests, not an error.
- It carries the **full** `selected_workspace_bundles`/`selected_target_bundles` sets inherited from the old `AllDeveloperMcpIntegrationTests` launcher (Servoy-Copilot) — deliberately not trimmed down to only what one project needs, so that other test projects reusing this launcher are likely to already have their dependencies covered. Don't remove bundles from these sets when adapting the launcher for a new target; add to them if a new target needs something not already selected.
- Servoy-Copilot has its **own copy**, `com.servoy.eclipse.developer.mcp.tests/PDEIntegrationTestsBaseCopy.launch` — same shape, but its `-Dproperty-file` points at that repo's own `testresources/servoy.properties`. **Keep the two launch files in sync** (bundle selections, VM args, structure) whenever you change one; only the target project/package and the property-file path are expected to differ between them.

### No cross-repo test-project dependency; "Copy"-suffixed duplicated utility classes

Earlier in this effort, `com.servoy.eclipse.developer.mcp.tests` (Servoy-Copilot) was supposed to depend on this repo's `com.servoy.eclipse.core.tests`/`com.servoy.eclipse.ngclient.ui.tests` via OSGi `Require-Bundle`, to reuse the shared PDE test-infra base classes (`TestUtilitiesClass`, `ServoyRunnerTestBase`, `DialogGuardRule`, `AbstractTitaniumIntegrationTest`, etc.) instead of duplicating them. **That cross-repo dependency intention has been dropped for now.** It only ever resolved in a live combined Eclipse workspace (both repos open together); Servoy-Copilot's actual Jenkins/Tycho build has no path to those bundles, because `.tests` projects are correctly excluded from `com.servoy.eclipse.feature`'s p2 update site, and standing up a proper publish/consume pipeline for test-only bundles was judged not worth the extra Jenkins infrastructure.

Instead:
- The shared base classes still live here, in `com.servoy.eclipse.core.tests.integration.utils` (`TestUtilitiesClass`, `ServoyRunnerTestBase`, `DialogGuardRule`, `DialogGuardBase`, `DialogExpectation`, `TestDialogInterceptor`) and `com.servoy.eclipse.ngclient.ui.tests.integrationpde.utils` (`AbstractTitaniumIntegrationTest`) — this remains the canonical/original copy.
- Servoy-Copilot keeps its **own duplicate** of each one, under `com.servoy.eclipse.developer.mcp.integrationpde.utils`, with a **`Copy` suffix** on every class name (`TestUtilitiesClassCopy`, `ServoyRunnerTestBaseCopy`, `DialogGuardRuleCopy`, `DialogGuardBaseCopy`, `DialogExpectationCopy`, `TestDialogInterceptorCopy`, `AbstractTitaniumIntegrationTestCopy`). The `Copy` suffix is intentional and permanent — it's not a TODO to remove, it's how both repos' agents recognize "this class must be kept behaviorally identical to its non-`Copy` counterpart in the other repo."
- **Whenever you change one of these classes, make the matching edit to its counterpart in the other repo** (same method bodies, same lifecycle behavior) — check each class's Javadoc, which cross-references its counterpart and says so explicitly.
- `com.servoy.eclipse.cypress.tests` and other `.tests` projects in **this** repo still depend on `com.servoy.eclipse.core.tests` via `Require-Bundle` — that part of the convention (shared infra within a single repo) is unaffected; only the *cross-repo* dependency was removed.

### Troubleshooting: JUnit 6 `NoSuchMethodError` (e.g. `Namespace.getParts()`)

If a JUnit 6 (Jupiter) test fails at runtime with:
```
java.lang.NoSuchMethodError: 'java.util.List org.junit.jupiter.api.extension.ExtensionContext$Namespace.getParts()'
```
**Root cause:** the `org.eclipse.dltk.javascript.rhino` workspace project bundles an older `junit-jupiter-api-5.x.jar` in its `test-libs/` folder. When PDE builds the flat classpath for a plain JUnit launch, it includes all workspace project dependencies — including rhino's `test-libs`. The older 5.x API jar can appear earlier on the classpath than the target platform's 6.x jar, causing version conflicts.

**Fix:** open `org.eclipse.dltk.javascript.rhino` → Java Build Path → Libraries tab → remove or uncheck the `test-libs/junit-jupiter-api-*.jar`, `test-libs/junit-4.*.jar` and `test-libs/hamcrest-*.jar` entries (or remove `test-libs` from rhino's `.classpath` entirely).

### Integration test infrastructure

PDE integration tests that activate a Servoy solution should extend a shared base class providing common utilities rather than reimplementing them — e.g. an SWT event-pump helper (`pumpEventsUntil(maxMs, assertions)` instead of raw `Thread.sleep`), `waitForWorkspaceBuildJobs()`, `waitForAppServer()`, solution creation/activation helpers, and a dialog guard that fails a test immediately if an unexpected SWT dialog pops up instead of hanging the run. If no such base class exists yet for a given `.tests` project, check whether one already exists in a sibling `.tests` project (e.g. in the Servoy-Copilot workspace) before writing new plumbing from scratch — it is often better to share/move the base class than to duplicate it.

**Convention: where shared PDE integration-test infrastructure lives.** Shared helpers (event-pump utilities, app-server wait, dialog guard, solution-activation base classes, etc.) live in the `.tests` plugin of whichever core plugin they are conceptually closest to — e.g. solution-activation/dialog-guard helpers → `com.servoy.eclipse.core.tests`; titanium-build-specific helpers → `com.servoy.eclipse.ngclient.ui.tests`. Other `.tests` projects **within this repo** depend on that shared project via `Require-Bundle` rather than duplicating the classes. **Across repos this is different** — see "No cross-repo test-project dependency" above: Servoy-Copilot keeps its own `Copy`-suffixed duplicate instead of a `Require-Bundle` dependency, because that cross-repo bundle dependency doesn't resolve in Servoy-Copilot's actual Jenkins/Tycho build.

Such shared-infra `.tests` projects **must be plain (non-fragment) OSGi plugins** if other, unrelated bundles need to `Require-Bundle` them: a fragment (`Fragment-Host: X`) can only be used by its single declared host, not by arbitrary other bundles. Converting a fragment to a plain plugin means dropping `Fragment-Host`, adding an explicit `Require-Bundle` for whatever it still needs from its former host, and adding `Export-Package` for what it shares. Its classes should also live in a package distinct from the host's own package, to avoid a split-package situation across two bundles. Helper methods meant to be called by subclasses living in other packages/bundles must be `public`, not `protected` — `protected` does not satisfy cross-package inheritance-based reuse the way this codebase uses it.

These PDE tests are typically run **without** cleaning the workspace between runs (to reuse node/cypress/... caches under `.metadata`). Because of that, each integration test class must delete the workspace projects it uses in `@BeforeClass`, so every run starts from a known state.

## Dependencies

Key external dependencies (from target platform):
- Eclipse 2025-12 release train
- Eclipse TM4E 0.17.1
- Eclipse GEF Classic 3.26.0
- Eclipse NatTable 2.6.0
- Eclipse Nebula 3.2.0
- Equo Chromium/CEF (embedded browser)
- Auth0 JWT
- Servoy DLTK (custom fork)
