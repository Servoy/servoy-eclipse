# AGENTS.md - Servoy Developer Eclipse IDE

## Session Startup — Skills Check

At the start of each session, quickly verify that the repo's global skills are installed:

1. Read `~/.config/opencode/opencode.jsonc` (or `opencode.json`) and check if `skills.paths` contains this repo's `skills/` directory.
2. If not registered, tell the user: "This repo has global skills that aren't installed yet. Run `/install-skills` to make them available in all projects."
3. If registered, list the `skills/*/` directories in this repo and compare against `~/.config/opencode/commands/`. If any command files (`.md` files that are NOT `SKILL.md`) are missing from the global commands dir, tell the user: "New commands found in skills/. Run `/install-skills` to update."

This check should be fast — just a directory listing and a file read. Do not block on it.

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
- **Never push directly.** You may create commits (once approved), but never run `git push` until the user has explicitly reviewed and approved the commit(s). Always wait for user confirmation before pushing.
- **Prefer new commits over `--amend`.** Only amend your own unpushed commits when explicitly asked. If in doubt, make a new commit — it's always safe.

### Running and Debugging
- **Use `eclipse-runner` tools** for launching, debugging, and testing Java applications.
- **Use `eclipse-pde` tools** for PDE-specific operations (target platform, plugin tests).

### Testing
- **Use `eclipse-ide_runAllTests`**, `eclipse-ide_runClassTests`, or `eclipse-ide_runTestMethod` for running JUnit tests.
- **Use `eclipse-pde_runJUnitPluginTests`** or `eclipse-pde_runJUnitPluginTestClass` for plugin integration tests.
- Test projects: `com.servoy.eclipse.model.tests`, `com.servoy.eclipse.ui.tests`, `com.servoy.eclipse.designer.tests`, `j2db_documentation.tests`, `com.servoy.eclipse.ngclient.ui.tests`

### Refactoring
- **Use `eclipse-coder_refactorRenameJavaType`** to rename classes, interfaces, enums, or records — this updates all references across the workspace.
- **Use `eclipse-coder_refactorRenamePackage`** to rename packages — updates all package declarations and references.
- **Use `eclipse-coder_refactorMoveJavaType`** to move types between packages.
- **For method, field, and variable renames:** use `eclipse-ide_findReferences` first to find all usages, then apply the rename consistently. Prefer Eclipse refactor tools over manual find-and-replace to ensure all references are updated correctly.

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
| `com.servoy.eclipse.designer` | Form designer (Java: commands, handlers, palette) |
| `com.servoy.eclipse.designer.rfb` | RFB designer (Angular frontend in `node/`, communicates via websocket) |
| `com.servoy.eclipse.designer.rib` | RIB designer (legacy) |
| `com.servoy.eclipse.designer.wpm` | Web Package Manager (Angular frontend in `node/`) |

#### Form Designer Architecture

The form designer is a multi-layer system:

1. **Java server** (`com.servoy.eclipse.designer` + `com.servoy.eclipse.designer.rfb`)
   - `DesignerFilter.java` — serves palette JSON data (`/designer/palette` endpoint)
   - `CreateComponentCommand.java` — creates persists on drop
   - `ElementFactory.java` — programmatic component creation (Place Field wizard)
   - `EditorServiceHandler.java` — routes websocket calls from Angular to Java handlers

2. **Angular designer frontend** (`com.servoy.eclipse.designer.rfb/node/`)
   - `palette.component.ts` — drag/drop from palette, sends `createElement` to iframe
   - `mouseselection.component.ts` — selection decorators, resize knobs
   - `ghostscontainer.component.ts` — ghost elements (parts, tabs)
   - `editorsession.service.ts` — websocket communication with Java server

3. **Angular content iframe** (`com.servoy.eclipse.ngclient.ui/node/src/designer/`)
   - `designform_component.component.ts` — renders the actual form being designed
   - Receives `createElement` messages for drag preview rendering

**Component sizing:** All spec lookups for default dimensions use `IContentSpecConstants.PROPERTY_DESIGN_SIZE` first, fallback to `PROPERTY_SIZE`. The `designsize` property is server-only and declared in component `.spec` files.

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
- **Cypress plugin tests:** `com.servoy.eclipse.cypress.tests` — fragment of `com.servoy.eclipse.cypress`. Split by naming convention (below); some classes are pure-logic unit tests, others need the OSGi/workbench and must run as PDE plug-in tests.
- **SVY-21515 Cypress node/npm toolchain + binary cache:** `com.servoy.eclipse.cypress.plainjunit.services.CypressToolchainTest` (pure-logic tests over `CypressToolchain`, no process launched, `@TempDir` for paths) covers: the plugin folder name equals `com.servoy.eclipse.cypress.Activator.PLUGIN_ID`; `getPluginMetadataDir`/`getFallbackCypressDir` for POSIX and Windows-style roots; the cache folder lookup `servoy.cypress.cacheFolder` system property, then `CYPRESS_CACHE_FOLDER` environment variable, then the `.metadata` default (blank values ignored, trimmed); `NodeTools.isUsable()`; `buildPackageManagerCommand` honours a configured `servoy.npmPath`; `buildCypressCliCommand`; `buildEnvironment` (PATH order, `Path` vs `PATH` key spelling, `NODE_PATH`, `CYPRESS_CACHE_FOLDER`, proxy variables untouched, input map not mutated); `formatFailure` (step, exit code, command, cache folder, output tail, proxy hint). `GetCypressDirTest` expects the cypress folder under `com.servoy.eclipse.cypress` (it used to be `com.servoy.eclipse.developer.mcp`), and `FormSpecRunnerTest` checks that `ensureCypressBinary(Path)` and `configureCypressEnvironment(ProcessBuilder, Path)` exist. These are `plainjunit` tests: run them with `eclipse-ide_runJUnitTests`. The runner's install lock (`INSTALL_LOCK`) and the "install did not produce a usable binary" message are covered by manual runs only. There is no pnpm support on `lts_2026`, so `servoy.jsRuntime=pnpm` is not handled here.
- **Angular tests:** `com.servoy.eclipse.ngclient.ui/node/run_tests.bat`
- **Designer RFB tests:** `com.servoy.eclipse.designer.rfb/node/` → `npm test` (Vitest, 307 tests)
- **WPM tests:** `com.servoy.eclipse.designer.wpm/node/` → `npm test` (Vitest, 94 tests)
- **SVY-21118 jsType in signatures:** `j2db_documentation.tests` → `FunctionDocumentationTest` [Plugin JUnit] — tests that `getJSType()` is used in signature generation when set on a parameter
- **SVY-21118 doc generator parsing:** `com.servoy.eclipse.docgenerator.tests` (in `docgenerator-ui` repo) → `DocumentedParameterDataTest` [Plugin JUnit, requires m2e in target] — tests that `{Object<String>}` in `@param` descriptions is extracted as jsType
- **SVY-21257 WebComponent clone UUID:** `j2db_test` → `WebComponentCloneTest` [JUnit] — tests that `WebComponent.cloneObj()` regenerates UUIDs for custom type children (AG Grid columns)
- **SVY-21121 Console/AI view in perspective:** `com.servoy.eclipse.ui.tests` → `DesignPerspectiveTest` [Plugin JUnit] — tests that Console and Servoy AI views are added as visible views (not placeholders) in the bottom folder of the Servoy Design perspective
- **SVY-21272 ClassCastException in hasChildren:** `com.servoy.eclipse.ui.tests` → `SolutionExplorerTreeContentProviderHasChildrenTest` [JUnit] — tests that `hasChildren()` does not throw ClassCastException when parent is a plain SimpleUserNode (e.g., RETURNTYPEPLACEHOLDER), and correctly returns false for UserNode with TABLE/INMEMORY_DATASOURCE/VIEW_FOUNDSET types
- **SVY-21149 Dark theme icon existence:** `com.servoy.eclipse.ui.tests` → `ImageReplacementMapperDarkIconsTest` [JUnit] — tests that `darkicons/expandall-disabled.png` and `darkicons/expandall-disabled@2x.png` exist in `com.servoy.eclipse.ui.tweaks` and are valid PNGs with correct dimensions (16×16 and 32×32)
- **SVY-21405 Zoom button enabled signal:** `com.servoy.eclipse.designer.rfb/node` → `toolbaritem.component.spec.ts` [Vitest unit, jsdom] and `toolbarbutton.browser.spec.ts` [Vitest browser, Chromium] — unit tests verify `ToolbarItemComponent.isDisabled()` narrows the `enabled` signal for both boolean and function-valued forms and reflects `enabled.set(...)`; the browser test verifies the OnPush `ToolbarButtonComponent`'s `[disabled]` DOM attribute updates when `item.enabled.set(...)` is called from outside an Angular event (the regression fix)
- **SVY-21356 CSS-position no-body / relation severity builder markers:** `com.servoy.eclipse.ui.tests` → `FormCssPositionAndRelationSeverityIntegrationTest` [Plugin JUnit, JUnit 4] (with shared harness `BuilderMarkerTestBase`) — creates forms/relations in a synthetic solution and runs `com.servoy.eclipse.core.servoyBuilder` to assert `FORM_CSS_POSITION_NO_BODY_PART` (ERROR, scoped to non-responsive CSS-position forms, honours flattened-form inheritance) and `RELATION_ITEM_TYPE_PROBLEM` (raised WARNING→ERROR) markers, plus NO-GAP regression scenarios (dangling valuelistID, databaseValuesType, unresolvable dataSource). Ported from Servoy-Copilot where it did not belong.
- **SVY-21469 data-target legacy customProperties (read path):** `com.servoy.eclipse.model.tests` → `WebFormComponentChildTypeTest` (nested class `LegacyStringCustomPropertiesNormalization`) [Plugin JUnit, JUnit 5] — reflectively drives the private `WebFormComponentChildType.getJson(forMutation, flattened)` (built via Mockito `CALLS_REAL_METHODS` + reflected `fcCompAndPropPath`/`parent` fields, `element` left null for the non-merge branch) to assert (§5.1) the flattened read `getJson(false, true)` converts a form-component child's legacy loose-string `customProperties` (`attributes:{ data-Target:"dashboard-health" }`) into a `JSONObject` with `attributes.data-Target`; that the normalized value, fed into a real `BaseComponent` (public-constructor `TestableBean`), is delivered by `getAttributes()` as a non-empty map containing `data-Target`; that an already-object value is returned unchanged (idempotent, `assertSame`); and (regression guard) that the plain `getJson(false, false)` read does NOT normalize and does NOT mutate the shared parent json (over-widening guard). Reverting the flattened-read widening leaves `customProperties` a String and fails these tests.
- **SVY-21469 data-target legacy customProperties (migration write):** `com.servoy.eclipse.model.tests` → `SolutionSerializerTest` (nested class `NormalizeFormComponentChildCustomProperties`) [Plugin JUnit, JUnit 5] — reflectively invokes the package-private `SolutionSerializer.normalizeFormComponentChildCustomProperties(JSONObject)` to assert (§5.2) a nested FC-child legacy string `customProperties` in a WebComponent json blob is rewritten as a `JSONObject` with `attributes.data-Target`; idempotent for an already-object value (`assertSame`); NPE-safe/no-op for null json, json without FC children, and a child without `customProperties`; recurses one extra level into a nested form component's own child (`n1.formComponent2.n2.customProperties`); and confirms the `ServoyJSONObject(str,false,false,true)` parse of the loose `attributes:{ data-Target:"x" }` fragment yields `attributes.data-Target`.
- **SVY-21469 data-target legacy customProperties (FormElementHelper runtime fix):** `servoy_ngclient.tests` → `com.servoy.j2db.server.ngclient.property.FormElementHelperCustomPropertiesTest` [Plugin JUnit, JUnit 4, extends `AbstractSolutionTest`] — builds a real in-memory solution (an `fce-container` web component on a `main` form + a form-component form `fcform` holding a `Field` child named `child1`, registered via the `FormElementHelperCustomPropertiesTest.manifest`/`-fcecontainer.spec` test package) and reflectively invokes the private static `FormElementHelper.generateFormComponentPersists(INGFormElement, PropertyDescription, JSONObject, Form, FlattenedSolution)` with a per-child `formElementValue` of `{ child1: { customProperties: "attributes:{ data-Target:\"dashboard-health\" }" } }`. Asserts (§5.3) the generated child persist's `BaseComponent.getAttributes()` contains `data-Target == dashboard-health` — i.e. the legacy String `customProperties` was merged via `setCustomProperties` rather than skipped by the incompatible-legacy-property guard (reverting the widened guard makes `getAttributes()` empty and the test fails); a second test adds a genuinely incompatible legacy `size` String alongside and asserts it is skipped without throwing while `data-Target` still reaches the child.
- **SVY-21423 PersistHelper null vs empty array for unset array-of-custom-object properties:** `servoy_ngclient.tests` → `com.servoy.j2db.persistence.PersistHelperGetWebComponentPropertyTest` [JUnit] — tests that `PersistHelper.getWebComponentProperty` returns `null` for an array-of-custom-object property with zero configured children, and still returns the correct populated `IChildWebObject[]` when children exist (regression guard for the SVY-20784 refactor that had made this branch always return an empty array instead of `null`, which caused the navbar dropdown-caret-for-empty-menu bug).
- **SVY-21386 palette bottom-drop responsive row order (shared ordering-counter helper):** `com.servoy.eclipse.designer.tests` → `com.servoy.eclipse.designer.editor.commands.AddContainerCommandComputeNextIndexTest` [Plugin JUnit, JUnit 5] — pure-logic tests over the now-`public static` `AddContainerCommand.computeNextLayoutContainerIndex(IPersist)` that both the Outline-add and palette bottom-drop paths share (AC1/AC2/AC5/AC6): a `Form` (reflected ctor) holding the repro rows returns `401 = max(x,y)+1`, and the new `(index,index)` counter is strictly greater than every sibling's x AND y; a single sibling at (7,10) yields 11; an empty parent yields 1; a `CSSPositionLayoutContainer` parent returns its **child count** (2, not max+1 901) per the CSS-position-ancestor branch; an empty CSS-position container returns 0. (AC3 right-sibling and AC4 absolute-layout branches live in `CreateComponentCommand.createComponent`, which needs the live editor/websocket, so they are covered by code inspection — the shared helper is only invoked in the responsive/no-right-sibling branch.) NOTE: the PDE plugin-test launcher is very slow in this workspace (~15 min); the class compiles clean and its `.class` is emitted, but a full plugin-test run was not completed here.
- **SVY-21576 form `.less` edit refreshes designer preview (own-file guard narrowing):** `com.servoy.eclipse.core.tests` → `com.servoy.eclipse.core.ServoyModelOwnFileChangedTest` [Plugin JUnit, JUnit 5] — pure path-logic tests over the extracted package-visible `ServoyModel.isOwnCompositeFileChanged(IPersist, File, Set<String>)` helper that `handleChangedFiles` now uses to decide the SVY-21249 skip-vs-full-refresh branch. Builds a reflected `Solution` (fileVersion = `VERSION_38` so `Form`/`Relation` count as composites-with-items) + `Form`/`Relation` and feeds fabricated `changedFilePaths` composed with the same `solutionParentDir + SolutionSerializer.getRelativeFilePath/getRelativePath` convention as production. Asserts: a form `.less`-only change → `true` (SVY-21576 regression fix — would be `false` before the widening, leaving the preview stale) with the matched path pinned as `<parent>/<solution>/forms/<form>.less`; a child `.js`-only change → `false` (SVY-21249 skip branch preserved — would be `true` if the guard were naively dropped); a form `.frm` change → `true`; an empty changed set → `false`; a non-`Form` composite (`Relation`) with only a `.less`-named sibling changed → `false` (the `.less` widening is form-only) while its real own-file change → `true`. This new `com.servoy.eclipse.core.tests` `eclipse-test-plugin` fragment (`Fragment-Host: com.servoy.eclipse.core`) was added to the root reactor pom. NOTE: as with SVY-21386, the PDE plugin-test launcher does not report results in this workspace; the class compiles clean (zero errors, imports organized, formatted) and all six cases were verified green by invoking the compiled `ServoyModel.isOwnCompositeFileChanged` directly against the production `bin/` class via reflection.

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

e.g. for `com.servoy.eclipse.cypress.tests`: plain tests live under `com.servoy.eclipse.cypress.plainjunit` (and subpackages like `.plainjunit.services`), PDE tests under `com.servoy.eclipse.cypress.integrationpde`. Every `.tests` project in this repo (`com.servoy.eclipse.core.tests`, `com.servoy.eclipse.cypress.tests`, `com.servoy.eclipse.ngclient.ui.tests`, `com.servoy.eclipse.ui.tests`) follows this split, each with its own `pom.xml` wiring (see "Maven wiring" below). A class's own name suffix (`*Test` vs `*IntegrationTest`) is just a naming habit carried over from before the split — the package is what actually determines which runner picks it up, since the Tycho `tycho-surefire-plugin` `<includes>` patterns match on package path, not class suffix.

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

## Shared Memory (`.assistai/memory.json`)

The file `.assistai/memory.json` is a **shared knowledge base** persisted via the `memory_remember` tool. It contains cross-session learnings, patterns, and architectural decisions discovered during development.

- **Always commit this file** when it changes — it is shared across developers and sessions via git
- It is NOT internal tooling state — it is deliberately accumulated project knowledge
- Use `memory_listMemories` at the start of a session to load relevant context
- Use `memory_remember` to persist new learnings that should survive across sessions
