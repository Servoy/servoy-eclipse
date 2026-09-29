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
- **Angular tests:** `com.servoy.eclipse.ngclient.ui/node/run_tests.bat`
- **LFC per-row auto-height (SVY-21457):** `com.servoy.eclipse.ngclient.ui/node/src/servoycore/listformcomponent/listformcomponent.spec.ts` and `row-renderer.component.spec.ts` (Jasmine/Karma) — asserts that native AG Grid `autoHeight` is disabled only for the per-row auto-height path (responsive, `responsiveHeight < 0`) and unchanged elsewhere; that `getRowHeight`/`applyMeasuredRowHeight` feed each row's explicitly-measured height back into AG Grid's server-side row model; the `visibility: hidden` anti-flicker reveal; the hardened resize-observer guard (column-count only, immune to scrollbar-driven width jitter); and (in `row-renderer.component.spec.ts`) `RowRenderer`'s per-row measurement via `ngAfterViewInit` and the recursive float-collapse-safe `measureContentHeight`
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
