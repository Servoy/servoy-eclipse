# Project Context — Servoy Eclipse IDE

This project is the **Servoy Developer IDE** — a large Eclipse RCP application built
as a multi-module Maven/Tycho project consisting of ~40+ OSGi plugin bundles.

## SDD variant

This repo uses the **sdd-java-eclipse** shared skill (Java / Eclipse-OSGi pipeline).

## Technology stack

| Aspect | Value |
|--------|-------|
| Java version | 21 |
| Build system | Maven 3.9.0+ with Eclipse Tycho 5.0.3 |
| Platform | Eclipse RCP (target: `launch_targets/com.servoy.eclipse.target.target`) |
| Module system | OSGi (each plugin is a bundle with MANIFEST.MF) |
| UI framework | Eclipse SWT/JFace + Angular (designer frontends) |
| Version | 2026.12.0-SNAPSHOT (master line) |

## Eclipse plugin development essentials

When writing code for this project, you are writing **OSGi bundles**, not plain Java:

### Dependencies
- Declare in `META-INF/MANIFEST.MF` under `Require-Bundle` or `Import-Package`
- Tycho resolves dependencies from the **active target platform** (see `launch_targets/`)
- Use `eclipse-pde_getActiveTarget` to check what target is currently active
- If a dependency is already in the target platform, just add it to MANIFEST.MF
- If a dependency is NOT in the target platform, add it as a Maven dependency in the
  target definition file (`.target` file) — then it becomes available for MANIFEST.MF

### Extension points
- Register contributions in `plugin.xml`
- Look at existing plugins for patterns (e.g. `com.servoy.eclipse.core/plugin.xml`)

### Services & activation
- Use OSGi Declarative Services or `BundleActivator` for lifecycle
- Prefer lazy activation (`Bundle-ActivationPolicy: lazy`)

### Packages & visibility
- Export public API packages in MANIFEST.MF `Export-Package`
- Keep internal packages unexported (convention: `*.internal.*`)
- Never reference another plugin's internal packages

### New plugin checklist
If creating a new plugin bundle:
1. Create `META-INF/MANIFEST.MF` with proper headers
2. Create `build.properties` listing source folders and output
3. Create `plugin.xml` if contributing extension points
4. Add the module to the parent `pom.xml`
5. Add to the feature (`com.servoy.eclipse.feature/feature.xml`)

## Code conventions

- Follow existing patterns in neighboring files — consistency over personal preference
- Use try-with-resources for all `Closeable` resources
- Use `volatile` or proper synchronization for shared mutable state
- Log via the plugin's `ILog` or SLF4J (check what the module uses)
- No `System.out.println` — use proper logging
- Prefer existing utility classes (check `com.servoy.eclipse.model` and `com.servoy.eclipse.core`)

## Key project structure

| Module | Purpose |
|--------|---------|
| `com.servoy.eclipse.core` | Main plugin, launch configs, schemas |
| `com.servoy.eclipse.model` | Data model layer |
| `com.servoy.eclipse.ui` | UI components |
| `com.servoy.eclipse.debug` | Debugger support |
| `com.servoy.eclipse.designer` | Form designer |
| `com.servoy.eclipse.ngclient` | NG Client support |
| `com.servoy.eclipse.exporter.war` | WAR exporter (server-side deploy packaging) |
| `com.servoy.eclipse.exporter.solution` | Solution exporter |
| `com.servoy.eclipse.exporter.mobile` | Mobile exporter |

## Testing

- Each `<plugin>.tests` project (`com.servoy.eclipse.cypress.tests`,
  `com.servoy.eclipse.ngclient.ui.tests`, `com.servoy.eclipse.ui.tests`, ...) splits its tests by
  **package**, not by class-name suffix (the Maven surefire includes match every class under
  the package):
  - Plain JUnit (pure logic, no OSGi/workspace): package `<base>.plainjunit[.*]`, run with
    `eclipse-ide_runJUnitTests`.
  - PDE integration (needs OSGi/workspace/`ServoyModel`/`Display`, or code that transitively
    needs them, e.g. `Activator.getInstance()`): package `<base>.integrationpde[.*]`, run with
    `eclipse-pde_runJUnitPluginTests` and `launcherName` =
    `/com.servoy.eclipse.core.tests/PDEIntegrationTestsBase.launch` (starts a full Servoy
    Developer; poll with `eclipse-pde_getOperationStatus`). Pass `junitVersion` ("6") if tests
    aren't found.
  - Never mix the two kinds in one package; no `@Suite`/`AllXxxTests` classes.
- Maven: default build runs only `**/plainjunit/**/*.java`; `-Pintegration` runs
  `**/integrationpde/**/*.java` in a full workbench (Jenkinsfile "Integration Tests" stage).
- Integration test classes must delete the workspace projects they use in `@BeforeAll`/
  `@BeforeClass` — the workspace is reused between runs for node/cypress caches.
- Shared PDE test infrastructure (`TestUtilitiesClass` — `pumpEventsUntil`,
  `waitForWorkspaceBuildJobs`, `waitForAppServer`; `ServoyRunnerTestBase`, `DialogGuardRule`,
  `AbstractTitaniumIntegrationTest`) lives in `com.servoy.eclipse.core.tests` (non-fragment
  plugin; it also has its own `plainjunit` tests but no `integrationpde` ones) and
  `com.servoy.eclipse.ngclient.ui.tests`. Prefer it over raw
  `Thread.sleep`. Servoy-Copilot keeps `Copy`-suffixed duplicates; keep them in sync.
- See `AGENTS.md` "Test types, naming convention & how to Run Tests" for full details.

## AGENTS.md

Always read `AGENTS.md` at the start of your work — it contains the full tool usage
policy, workflow requirements, and post-edit checklist that you must follow.

## Gotchas

Things that will trip you up if you don't know them:

- **MANIFEST.MF formatting:** The MANIFEST.MF file has strict line-length limits (72 bytes).
  Use eclipse-coder tools to edit it, not raw text replacement, or let `eclipse-coder_formatFile`
  handle it. Broken MANIFEST.MF = bundle won't load.

- **Target platform is the source of truth for deps:** You cannot use a library just because
  it exists on Maven Central. It must be in the active target platform first. If it's not
  there, add it to the `.target` file's Maven dependencies section, then reload the target
  (`eclipse-pde_reloadTarget`).

- **Plugin pom.xml is NOT for dependencies:** Unlike normal Maven projects, the `pom.xml`
  in a Tycho plugin project is only for build configuration (packaging type, parent, etc.).
  Runtime dependencies come from MANIFEST.MF + target platform. Never add `<dependency>`
  blocks to a plugin's pom.xml for library usage.

- **Require-Bundle vs Import-Package:** Prefer `Require-Bundle` for Servoy internal bundles.
  Use `Import-Package` for third-party libraries where you want loose coupling.

- **build.properties matters:** If you add a new folder (e.g. `resources/`), it must be
  listed in `build.properties` under `bin.includes` or it won't be in the built JAR.

- **Extension point IDs are global:** When contributing to extension points in `plugin.xml`,
  the `id` attribute must be globally unique across the entire Eclipse platform.

- **Activator vs DS:** Don't create a `BundleActivator` just for service registration.
  Use OSGi Declarative Services (DS) with component XML or annotations instead. Activators
  are only for bundle lifecycle hooks (start/stop).

- **SWT threading:** All UI code must run on the SWT display thread. Use
  `Display.getDefault().asyncExec()` or `syncExec()`. Touching UI from a background
  thread = instant crash.

- **No JUnit in production MANIFEST:** Never add JUnit dependencies to a production
  plugin's MANIFEST.MF. Test dependencies belong only in test project bundles.

- **Feature.xml ordering:** When adding a new plugin to `com.servoy.eclipse.feature/feature.xml`,
  add it alphabetically to maintain consistency.
