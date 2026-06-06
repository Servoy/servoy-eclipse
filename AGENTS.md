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

### After Every Code Change
1. **Always call `eclipse-ide_getCompilationErrors`** after modifying code to check for compilation errors.
2. If errors are found and have quick fixes available, **use `eclipse-ide_executeQuickFix`** to resolve them automatically.
3. **Use `eclipse-coder_organizeImports`** to fix import issues after edits.
4. **Spotbugs:** Spotbugs errors of the **two highest severity levels** are treated as blocking errors. Always try to fix these in any new or modified code to keep the codebase robust and clean.

### Git Operations
- **Use `eclipse-git` tools** (`gitStatus`, `gitDiff`, `gitAdd`, `gitCommit`, `gitBranch`, etc.) instead of command-line git.
- **After every `gitCommit`**, display the full commit message (subject line + body) in a formatted block so the user can verify the naming and content before moving on.

### Running and Debugging
- **Use `eclipse-runner` tools** for launching, debugging, and testing Java applications.
- **Use `eclipse-pde` tools** for PDE-specific operations (target platform, plugin tests).

### Testing
- **Use `eclipse-ide_runAllTests`**, `eclipse-ide_runClassTests`, or `eclipse-ide_runTestMethod` for running JUnit tests.
- **Use `eclipse-pde_runJUnitPluginTests`** or `eclipse-pde_runJUnitPluginTestClass` for plugin integration tests.
- Test project: `com.servoy.eclipse.tests`

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
- **Angular tests:** `com.servoy.eclipse.ngclient.ui/node/run_tests.bat`
- **Designer RFB tests:** `com.servoy.eclipse.designer.rfb/node/src/test.ts`
- **WPM tests:** `com.servoy.eclipse.designer.wpm/node/src/test.ts`

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
