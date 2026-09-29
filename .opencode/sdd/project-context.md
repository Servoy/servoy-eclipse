# Project Context — Servoy Eclipse IDE (LTS line)

This project is the **Servoy Developer IDE** — a large Eclipse RCP application built
as a multi-module Maven/Tycho project consisting of ~40+ OSGi plugin bundles. This checkout
is the **LTS maintenance line**.

## SDD variant

This repo uses the **sdd-java-eclipse** shared skill (Java / Eclipse-OSGi pipeline).

## Technology stack

| Aspect | Value |
|--------|-------|
| Java version | 17 |
| Build system | Maven with Eclipse Tycho 4.0.13 |
| Platform | Eclipse RCP (target under `launch_targets/`) |
| Module system | OSGi (each plugin is a bundle with MANIFEST.MF) |
| UI framework | Eclipse SWT/JFace + Angular (designer frontends) |
| Version | 2025.3.6-SNAPSHOT (LTS line) |

## Eclipse plugin development essentials

You are writing **OSGi bundles**, not plain Java:

### Dependencies
- Declare in `META-INF/MANIFEST.MF` under `Require-Bundle` or `Import-Package`
- Tycho resolves dependencies from the **active target platform** (`launch_targets/`)
- Use `eclipse-pde_getActiveTarget` to check the active target
- If a dependency isn't in the target platform, add it to the `.target` file's Maven
  dependencies, then reload the target — only then can MANIFEST.MF reference it

### Packages & visibility
- Export public API packages in MANIFEST.MF `Export-Package`; keep internal packages unexported
- Never reference another plugin's internal packages

## Code conventions

- Follow existing patterns in neighboring files
- try-with-resources for all `Closeable` resources
- `volatile` / proper synchronization for shared mutable state
- Log via the plugin's `ILog` or SLF4J — no `System.out.println`
- Prefer existing utilities (`com.servoy.eclipse.model`, `com.servoy.eclipse.core`)

## Key project structure

| Module | Purpose |
|--------|---------|
| `com.servoy.eclipse.core` | Main plugin, launch configs, schemas |
| `com.servoy.eclipse.model` | Data model layer |
| `com.servoy.eclipse.ui` | UI components |
| `com.servoy.eclipse.designer` | Form designer |
| `com.servoy.eclipse.exporter.war` | WAR exporter |
| `com.servoy.eclipse.tests` | Integration tests (eclipse-test-plugin) |

## Testing

- Unit tests (pure logic): `<plugin>.tests` fragment, `eclipse-plugin` packaging,
  `eclipse-ide_runClassTests`, class suffix `*Test`.
- Integration/plugin tests (needs OSGi/workspace/`ServoyModel`): `eclipse-test-plugin`
  packaging, primary project `com.servoy.eclipse.tests`, `eclipse-pde_runJUnitPluginTestClass`,
  class suffix `*IntegrationTest`.
- Prefer shared integration bases/utilities (`AbstractIntegrationTest`, `TestUtilitiesClass`
  — `pumpEventsUntil`, `waitForWorkspaceBuildJobs`) over raw `Thread.sleep`.
- See `AGENTS.md` `## Testing` for the catalogue of existing test classes.

## AGENTS.md

Always read `AGENTS.md` at the start — it has the full tool usage policy, workflow, post-edit
checklist, and the `[ai]` + Jira-key commit-subject convention.

## Gotchas

- **MANIFEST.MF:** strict 72-byte line-length limits. Use eclipse-coder tools / `formatFile`.
- **Target platform is the source of truth for deps** — not Maven Central.
- **Plugin pom.xml is NOT for dependencies** — only build config; runtime deps come from
  MANIFEST.MF + target platform.
- **build.properties matters:** new folders must be in `bin.includes`.
- **SWT threading:** UI code must run on the SWT display thread (`Display.getDefault().asyncExec/syncExec`).
- **No JUnit in production MANIFEST:** test deps belong only in test bundles.
