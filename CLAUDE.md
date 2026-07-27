# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## MCP Server Policy

This project has Eclipse MCP servers configured in `opencode.json`. **Always prefer MCP server tools over built-in tools** when they are available.

| Operation | Preferred Tool |
|-----------|---------------|
| Reading Java source | `eclipse-ide` (`readProjectResource`, `getSource`, `getClassOutline`, `getMethodSource`) |
| Editing code | `eclipse-coder` (`replaceString`, `insertIntoFile`, `createFile`, `replaceFileContent`) |
| Searching code | `eclipse-ide` (`fileSearch`, `fileSearchRegExp`, `findReferences`) |
| Git operations | `eclipse-git` instead of CLI git |
| Running/debugging | `eclipse-runner`, `eclipse-pde` |

**After every code change:**
1. `eclipse-ide_getCompilationErrors` — check for compile errors
2. `eclipse-ide_executeQuickFix` — auto-resolve errors with available quick fixes
3. `eclipse-coder_organizeImports` — fix imports
4. `eclipse-coder_formatFile` — format the file
5. Spotbugs errors at the two highest severity levels are **blocking** — fix them in any new/modified code

See `AGENTS.md` for the full tool policy and workflow.

## Build

**Prerequisites:** Maven 3.9.0+, Java 21. Tycho version is set in `.mvn/maven.config`.

```bash
# Build all plugins (default)
mvn clean verify

# Full product with bundled JREs and Node.js
mvn clean verify -Pproduct

# Feature and product only (skips JREs/Node.js)
mvn clean verify -Ponly_product

# Target platform definition only
mvn clean verify -Ptarget
```

Target platform files are in `launch_targets/`. The main target (`com.servoy.eclipse.target.target`) pulls Eclipse 2025-12, GEF Classic 3.26.0, NatTable 2.6.0, Nebula 3.2.0, and Equo Chromium/CEF.

## Testing

- **Java plugin tests:** `com.servoy.eclipse.tests` (eclipse-test-plugin) — run via `eclipse-ide_runClassTests` or `eclipse-pde_runJUnitPluginTestClass`
- **Angular (NGClient UI):** `com.servoy.eclipse.ngclient.ui/node/run_tests.bat`
- **Angular (Designer RFB):** `com.servoy.eclipse.designer.rfb/node/` — Karma (`karma.conf.js`)
- **Angular (WPM):** `com.servoy.eclipse.designer.wpm/node/src/test.ts`

## Architecture

This is a **multi-module Maven/Tycho Eclipse RCP application** (~40+ OSGi plugin bundles) built on Eclipse 2025-12. Java 21, version `2026.6.0-SNAPSHOT`.

### Key Plugin Groups

**Core:**
- `com.servoy.eclipse.core` — main plugin, extension point schemas, launch configurations
- `com.servoy.eclipse.model` — data model layer (solution model, type inference, natures)
- `com.servoy.eclipse.ui` — views, perspectives, editors, exporters UI
- `com.servoy.eclipse.debug` — debugger support
- `com.servoy.eclipse.cloud` — cloud integration

**Form Designers** (three separate builders):
- `com.servoy.eclipse.designer` — base form designer
- `com.servoy.eclipse.designer.rfb` — Responsive Form Builder; Angular frontend lives in `node/`
- `com.servoy.eclipse.designer.rib` — legacy RIB builder
- `com.servoy.eclipse.designer.wpm` — Web Package Manager; Angular frontend in `node/`

**NG Client:**
- `com.servoy.eclipse.ngclient` — support module
- `com.servoy.eclipse.ngclient.ui` — full Angular workspace in `node/`

**Exporters:** solution, WAR, NG Desktop, mobile — each in a separate `com.servoy.eclipse.exporter.*` plugin with a shared extension-point pattern.

**AI Integration:**
- `com.servoy.eclipse.opencode` — "Servoy AI" perspective, embedded browser editor (Equo Chromium)
- `com.servoy.eclipse.servoypilot` + `servoypilot.langchain4j` — AI assistant with LangChain4j
- `com.servoy.eclipse.aibridge` — AI backend bridge

**Platform bundles:** `com.servoy.eclipse.jre.*` and `com.servoy.eclipse.nodejs.*` provide bundled JREs and Node.js for win32/linux/macosx × x86_64/aarch64.

### Angular Frontends

The RFB designer, WPM, and NGClient UI each have a full Angular CLI workspace under their `node/` subdirectory. These are compiled separately from Maven (via the `tycho-extras` frontend plugin) and bundled into the OSGi plugin at build time.

### Extension Point Architecture

`com.servoy.eclipse.core` defines the main extension points (`aiprovider`, `debugstarter`, `activesolutionlistener`, `preInitializeJob`). Plugins contribute to these declaratively via `plugin.xml`. This is standard Eclipse plugin architecture — search `plugin.xml` files for extension/extension-point declarations when tracing feature contributions.

## Spec / Design Documents

Feature specs and design documents live in `docs/` at the repository root.

- Name files after the Jira case with a `.spec.md` extension: `docs/SVY-21080-embedded-opencode.spec.md`
- Never place spec files inside a plugin or module subdirectory — they don't belong in OSGi bundles and won't be built into JARs.
- When asked to write a spec, always create it in `docs/` unless explicitly told otherwise.

## Commit Message Conventions

- Include the Jira case number when relevant: `SERVOY-293 fix NPE in WAR export [ai]`
- Append `[ai]` to the subject line when the commit is mostly AI-generated
- Case prefixes: `SVY-`, `SVYX-`, `SERVOY-`

## License

AGPL v3. Compatible with all open source licenses except GPL.
