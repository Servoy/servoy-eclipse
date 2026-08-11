# AGENTS.md - Servoy Developer Eclipse IDE

## Project Overview

This is the **Servoy Developer IDE** source code — a large Eclipse RCP application built as a multi-module Maven/Tycho project. It consists of ~40+ OSGi plugin bundles covering the IDE core, form designers (with Angular frontends), NG client, exporters, AI assistant integration, and platform-specific runtime bundles.

- **Version:** 2026.6.0-SNAPSHOT
- **Java version:** 21
- **Build system:** Maven 3.9.0+ with Eclipse Tycho 4.0.12
- **License:** AGPL v3 (compatible with all open source licenses except GPL)
- **Base platform:** Eclipse 2025-12

## Tool Usage Policy (MCP Servers)

This project has Eclipse MCP servers configured in `opencode.json`. **Always prefer the MCP server tools over built-in tools** for the following operations:

### File Operations
- **Use `eclipse-coder` tools** (`replaceString`, `replaceFileContent`, `insertIntoFile`, `createFile`, `deleteFile`, `deleteLinesInFile`) for all code edits instead of built-in file write/edit tools.
- **Use `eclipse-ide` tools** (`readProjectResource`, `getSource`, `getFilteredSource`, `getMethodSource`, `getClassOutline`) for reading Java source files.
- **Use `eclipse-ide` tools** (`fileSearch`, `fileSearchRegExp`, `findFiles`, `findReferences`) for searching code.

### Navigation & Discovery Tools

For quick codebase orientation and type/method lookup, use the JDT-powered search tools:

- **`eclipse-ide_searchTypes`** — Fuzzy type search via JDT SearchEngine. Supports wildcards (`*Payment*`), CamelCase (`PS` → `PaymentService`), prefix, and package-qualified patterns. Equivalent to Eclipse's Open Type (Ctrl+Shift+T). Use this instead of grep/glob when looking for a class by partial name.
- **`eclipse-ide_searchMethods`** — Method name search with the same pattern support, plus optional declaring type filter. Use when you need to find where a method is defined without knowing the full class name.
- **`eclipse-ide_getPackageSummary`** — Returns each type's name, kind, Javadoc first sentence, method/field counts, and interfaces for a package — a table-of-contents in one call. Use to quickly understand what a package contains.
- **`eclipse-ide_getWorkspaceOverview`** — High-level architectural map of projects → packages → type names for immediate orientation. Use as the first step when exploring an unfamiliar part of the codebase.

### After Every Code Change
1. **Always call `eclipse-ide_getCompilationErrors`** after modifying code to check for compilation errors.
2. If errors are found and have quick fixes available, **use `eclipse-ide_executeQuickFix`** to resolve them automatically.
3. **Use `eclipse-coder_organizeImports`** to fix import issues after edits.
4. **Spotbugs:** Spotbugs errors of the **two highest severity levels** are treated as blocking errors. Always try to fix these in any new or modified code to keep the codebase robust and clean.

### Git Operations
- **Use `eclipse-git` tools** (`gitStatus`, `gitDiff`, `gitAdd`, `gitCommit`, `gitBranch`, etc.) instead of command-line git.
- **After every `gitCommit`**, display the full commit message (subject line + body) in a formatted block so the user can verify the naming and content before moving on.
- **Never push directly.** You may create commits, but never run `git push` until the user has explicitly reviewed and approved the commit(s). Always wait for user confirmation before pushing.

### Running and Debugging
- **Use `eclipse-runner` tools** for launching, debugging, and testing Java applications.
- **Use `eclipse-pde` tools** for PDE-specific operations (target platform, plugin tests).

### Testing
- **Use `eclipse-ide_runAllTests`**, `eclipse-ide_runClassTests`, or `eclipse-ide_runTestMethod` for running JUnit tests.
- **Use `eclipse-pde_runJUnitPluginTests`** or `eclipse-pde_runJUnitPluginTestClass` for plugin integration tests.
- Test projects: `com.servoy.eclipse.model.tests`, `com.servoy.eclipse.ui.tests`, `com.servoy.eclipse.designer.tests`, `j2db_documentation.tests`, `com.servoy.eclipse.ngclient.ui.tests`

### Other Tools
- **Use `eclipse-ide_formatFile`** or `eclipse-coder_formatFile` to format Java files after editing.
- **Use `eclipse-context`** tools for workspace context, file history, and cached resources.
- **Use `time`** for time-related operations.

## Workflow for Code Changes

```
1. Read/understand code using eclipse-ide tools (getClassOutline, getMethodSource, getFilteredSource)
2. Make changes using eclipse-coder tools (replaceString, insertIntoFile, etc.)
3. Organize imports: eclipse-coder_organizeImports
4. Format file: eclipse-coder_formatFile
5. Check errors: eclipse-ide_getCompilationErrors
6. If errors have quick fixes: eclipse-ide_executeQuickFix
7. If errors remain: fix manually and repeat from step 5
8. Run relevant tests: eclipse-ide_runClassTests or eclipse-pde_runJUnitPluginTestClass
```

## Debugging CI-only Failures

When tests or builds fail on CI (Jenkins) but work locally, **do NOT spend multiple rounds theorizing about root causes**. Instead:

1. **Add diagnostic logging immediately** — log the state of the failing object/environment (e.g. `typeof`, `constructor.name`, `Object.keys()`, `JSON.stringify`) at the point of failure
2. **Push and let CI run** — get real data from the actual environment
3. **Analyze the output** — the diagnostics usually reveal the root cause in one CI round
4. **Fix based on evidence** — not on theory

Example: if `document.querySelector is not a function` on CI, don't guess why — log `typeof document`, `document.constructor.name`, `Object.keys(document)` in a setup file, push, and read the CI output. This approach saves 3-5 failed attempts.

This same principle applies to **local debugging**: when facing unclear errors, add diagnostic logging FIRST and run — don't immediately attempt code/config fixes based on assumptions. One log statement that shows the actual state is worth more than three speculative fixes.

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

When asked to create, update, or link Jira issues, load the instructions from `JIRA.md` in this repository.

## Code Style & Conventions

- Follow existing code style and conventions for each language and module
- Java: standard Eclipse plugin conventions, OSGi declarative services
- TypeScript/Angular: follows Angular CLI conventions in `node/` subdirectories
- **Angular sub-projects have their own `AGENTS.md`** — always read those first when working on Angular code for project-specific commands, conventions, and dependencies:
  - `com.servoy.eclipse.ngclient.ui/node/AGENTS.md` — NG Client UI (TiNG)
  - `com.servoy.eclipse.designer.rfb/node/AGENTS.md` — Form Designer (RFB)
  - `com.servoy.eclipse.designer.wpm/node/AGENTS.md` — Web Package Manager (WPM)
- Each Angular sub-project has its own `opencode.json` with Angular CLI MCP configured — for Angular-specific guidance (best practices, API docs), open that sub-project directory directly.
- **Cross-project Angular changes** (shared libraries, dependency wiring): work from this root directory. The Angular CLI MCP won't be available, but Eclipse MCP tools and git work across all projects.
- **After every Angular code change:** run lint (`npm run lint`) and build, then tests if applicable. All three projects must pass lint with zero warnings before committing.
- No hardcoded secrets, credentials, or proprietary information
- All code must be compatible with open source licenses (except GPL)
- **Commit messages:** When the code is mostly AI-generated, the commit subject line must end with `[ai]`
- **Commit messages for cases:** When a commit is related to a Jira case, the case number (e.g. `SVY-123`, `SVYX-456`, `SERVOY-293`) must be included in the commit subject line. Example: `SERVOY-293 fix NPE in WAR export copyRequiredBundles [ai]`

## Testing

- **Model tests:** `com.servoy.eclipse.model.tests` (eclipse-test-plugin packaging)
- **NG Client UI tests:** `com.servoy.eclipse.ngclient.ui.tests` (eclipse-test-plugin packaging)
- **Designer tests:** `com.servoy.eclipse.designer.tests` â `TestCssValues`, `TestSnapCSSPosition` [JUnit]
- **Angular tests:** `com.servoy.eclipse.ngclient.ui/node/run_tests.bat`
- **Designer RFB tests:** `com.servoy.eclipse.designer.rfb/node/` → `npm test` (Vitest, 307 tests)
- **WPM tests:** `com.servoy.eclipse.designer.wpm/node/` → `npm test` (Vitest, 94 tests)
- **SVY-21118 jsType in signatures:** `j2db_documentation.tests` → `FunctionDocumentationTest` [Plugin JUnit] — tests that `getJSType()` is used in signature generation when set on a parameter
- **SVY-21118 doc generator parsing:** `com.servoy.eclipse.docgenerator.tests` (in `docgenerator-ui` repo) → `DocumentedParameterDataTest` [Plugin JUnit, requires m2e in target] — tests that `{Object<String>}` in `@param` descriptions is extracted as jsType
- **SVY-21257 WebComponent clone UUID:** `j2db_test` → `WebComponentCloneTest` [JUnit] — tests that `WebComponent.cloneObj()` regenerates UUIDs for custom type children (AG Grid columns)
- **SVY-21121 Console/AI view in perspective:** `com.servoy.eclipse.ui.tests` → `DesignPerspectiveTest` [Plugin JUnit] — tests that Console and Servoy AI views are added as visible views (not placeholders) in the bottom folder of the Servoy Design perspective
- **SVY-21272 ClassCastException in hasChildren:** `com.servoy.eclipse.ui.tests` → `SolutionExplorerTreeContentProviderHasChildrenTest` [JUnit] — tests that `hasChildren()` does not throw ClassCastException when parent is a plain SimpleUserNode (e.g., RETURNTYPEPLACEHOLDER), and correctly returns false for UserNode with TABLE/INMEMORY_DATASOURCE/VIEW_FOUNDSET types
- **SVY-21149 Dark theme icon existence:** `com.servoy.eclipse.ui.tests` → `ImageReplacementMapperDarkIconsTest` [JUnit] — tests that `darkicons/expandall-disabled.png` and `darkicons/expandall-disabled@2x.png` exist in `com.servoy.eclipse.ui.tweaks` and are valid PNGs with correct dimensions (16×16 and 32×32)

## Troubleshooting: Angular Source Build Failures

When the generated ngclient build (`target/<solution>/`) fails with errors like:
- `NG8002: Can't bind to 'xxx' since it isn't a known property of 'div'`
- `NG6002: 'XxxModule' does not appear to be an NgModule class`
- `NG8004: No pipe found with name 'xxx'`

And the errors only affect packages in `packages/` (source-included external packages), NOT `projects/` (workspace libraries):

### Root cause: dependency version conflicts between packages

Angular's compiler fails to process a module if ANY of its imported dependencies has a version conflict with other packages in the build. This cascades: the entire module becomes unresolvable, making all its exported directives/pipes invisible.

### How to diagnose

1. Check which package fails: the error mentions the component/module path
2. Compare `package.json` dependency versions between the failing package and other packages that use the SAME library (e.g., `@ng-bootstrap/ng-bootstrap`, `@angular/cdk`, `ng-select2-component`)
3. Use `npm ls <package-name>` in the target folder to check for duplicate/conflicting versions
4. Key conflict pattern: one package requires `^21` and another requires `^20` of the same library → npm installs TWO versions → Angular compilation breaks

### How to fix

Align dependency versions across ALL package.json files:
- `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/package.json`
- `servoy-extra-components/components/projects/servoyextracomponents/package.json`
- `servoy-bootstrap-components/.../package.json`
- `servoy-nggrids/.../package.json`

All packages must use the SAME major version for shared Angular ecosystem libraries (especially `@ng-bootstrap/ng-bootstrap`, `@angular/cdk`, `ng-select2-component`).

### Example

servoyextra had `@ng-bootstrap/ng-bootstrap: "^21"` while servoydefault still had `"^20"`. npm installed both versions, breaking Angular's compilation of the source-included package.

---

## Shared Angular Libraries (@servoy/public, @servoy/sablo, @servoy/designer)

The Angular sub-projects share code via pre-built library packages:

- **Source:** `com.servoy.eclipse.ngclient.ui/node/` (sablo, designer, public)
- **Build:** `cd com.servoy.eclipse.ngclient.ui/node && npm run build_libs`
- **Output:** `dist-public/`, `dist-sablo/`, `dist-designer/` (each with a `.tgz` file)
- **Consumed by:** `com.servoy.eclipse.designer.rfb/node/package.json` as `file:` references to tgz

**Critical rules:**
- NEVER use tsconfig `paths` pointing to source in another Angular project — this causes duplicate `@angular/core` module instances, breaking signals, TestBed, and runtime behavior
- Use `npm pack` to create tgz files, then reference them with `file:path/to/package.tgz`
- `file:` to a tgz works like a real npm registry install (peer deps resolve from consumer)
- `file:` to a directory does NOT work (causes signal INPUT_SIGNAL_BRAND_WRITE_TYPE mismatch)
- Maven build handles the lib build step automatically via frontend-maven-plugin

---

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
