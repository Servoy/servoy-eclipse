# Spec: SVY-21642 — WAR export: optional precompressed brotli/gzip resources + optional Tomcat serving config

Jira: SVY-21642 (Task, R&D - New Capability). Status: spec finalized, nothing implemented yet.

Simplified: no master switch, step runs when brotli or gzip is on.

## 1. Goal

Add an **opt-in** WAR-export step that, after all WAR content is assembled and **immediately before** the temp WAR directory is zipped, precompresses compressible static resources into sibling `.br` (brotli) and/or `.gz` (gzip) files. Add a separate **opt-in** option that injects Tomcat `DefaultServlet` precompressed-serving config into the generated `web.xml`. With every option off (the default) the export output is unchanged.

Approved decisions (authoritative):

- Brotli and gzip are produced by the **bundled Node** (`zlib.brotliCompressSync` + `zlib.gzipSync`, no npm package). One bundled Node script does both the file walk/selection and the compression, so the rules live in one place.
- It runs only when NG2 resources were exported (`exportNG2Mode() != "false"`). Otherwise it is a safe no-op with a log message.
- Level is always max: brotli quality 11, `lgwin` 24; gzip level 9. No level option.
- Three opt-in options, all off by default: `precompressBrotli`, `precompressGzip`, `injectTomcatPrecompressed`. The precompression step runs when brotli and/or gzip is on (no master switch). Each is wired into the model, wizard, saved settings and headless args.
- Tomcat config goes into the generated `web.xml` (only when the user supplied no custom `web.xml`). The `servoy web.xml version` comment stays at 7 (see 3.6).

## 2. Background

### 2.1 Where it plugs in (verified)

`com.servoy.eclipse.model.war.exporter.WarExporter.doExport(IProgressMonitor)` (bundle `com.servoy.eclipse.model`) runs this order:

```
createNewWarFile → doActiveSolutionExports (if tmpWarDir == null) → copyRootWebappFiles → copyPlugins
→ copyStandardLibs → copyDrivers → moveSlf4j → copyWebXml → copyLog4jConfigurationFile
→ createTomcatContextXML → addServoyProperties → exportAdminUser → copyComponentsAndServicesPlusLibs
→ copyExportedComponentsAndServicesPropertyFile
→ copyNGClient2(monitor)   // only if exportNG2Mode() == null || !"false"; wrapped in try/catch RuntimeException
→ copyWARLibs → createDeployPropertiesFile → checkDuplicateJars
→ (if !getStartRMI(): move plugins/* jars into WEB-INF/lib, delete plugins dir)
→ zipDirectory(tmpWarDir, warFile)
→ deleteDirectory(tmpWarDir)
```

The new step goes between the "move plugins" block (which ends with `monitor.worked(1); if (monitor.isCanceled()) return null;`) and `zipDirectory(...)`. The draft's step order matches the code.

The mode check in the code is `exportModel.exportNG2Mode() == null || !exportModel.exportNG2Mode().equals("false")`. The new step must reuse exactly this condition. The headless default is `"true"` (`WarArgumentChest.parseArguments`), and the wizard never gives `"false"`.

### 2.2 How the NG2 resources get into the WAR (verified)

`WarExporter.copyNGClient2` calls `com.servoy.eclipse.model.Activator.exportNG2ToWar(ING2WarExportModel)`. That calls a `Consumer<ING2WarExportModel>` registered by `com.servoy.eclipse.ngclient.ui.Activator.start()` via `setNG2WarExporter(WebPackagesListener::exportNG2ToWar)`.

**This is the important architectural constraint.** `com.servoy.eclipse.model` cannot depend on `com.servoy.eclipse.ngclient.ui`, because ngclient.ui has `Require-Bundle: com.servoy.eclipse.model`. `WarExporter` therefore cannot call `RunNPMCommand` or `Activator.getNodePath()` directly. The precompression needs the same hook pattern: a second `Consumer` registered by ngclient.ui in `Activator.start()`.

`WebPackagesListener.exportNG2ToWar` copies `<mainTargetFolder>/<solution>_dist/dist/app/browser` into `model.getExportLocation()`. It skips `index.html` and `favicon.ico`, then copies `index.html` to `WEB-INF/angular-index.html`. Precompression stays in the `WarExporter` flow, not inside `exportNG2ToWar`. That way it also sees the other root files (less-compiled css, copied assets) and every sub-directory.

### 2.3 Node toolchain (verified)

- `com.servoy.eclipse.ngclient.ui.Activator.getNodePath()` returns the configured or bundled Node executable (`File`), or `null` if Node was not extracted. `waitForNodeExtraction()` is package-private. `Activator.isNodeExtractionAndTitaniumBuildDisabled()` exists (used in junit runs).
- `RunNPMCommand(File nodePath, File npmPath, File projectFolder, List<String> commands)` builds the command line `node.getCanonicalPath() + npmPath.getCanonicalPath() + commands...`. It puts `nodePath.getParent()` on PATH, redirects stderr to stdout, streams output to the console, supports cancel and exposes `getExitCode()`. The "npm path" is just the first argument handed to node, so **a bundled script file can be passed as `npmPath`** and the existing launch pattern is reused unchanged. Call `runCommand(monitor)` directly (synchronous, as `exportNG2ToWar` does) and read `getExitCode()`. `RunNPMCommand.setOutputStream(StringOutputStream)` can redirect output. By default it writes to the TiNG build console via `Activator.getInstance().getConsole()`.
- `RunPNPMCommand` is for pnpm and is not needed. A plain `RunNPMCommand` is the right launcher for running a bundled script with node.
- Note: `runCommand` closes the console stream in `finally`. Use a fresh `StringOutputStream` (or the default console) per run.

### 2.4 Where the Node script lives (finding)

- It lives in bundle `com.servoy.eclipse.ngclient.ui`, folder `resources/` (it already holds `loadingclient.html` and `buildfailedclient.html`). `resources/` is already in `build.properties` `bin.includes`, so **no `build.properties` change** is needed.
- New file: `com.servoy.eclipse.ngclient.ui/resources/precompress-war.mjs`.
- Runtime location: the existing code reads bundle resources with `Activator.getInstance().getBundle().getEntry("/resources/...")` (see `IndexPageFilter`). Node needs a real file path, not a `bundleentry://` URL. So resolve with `org.eclipse.core.runtime.FileLocator.toFileURL(entry)`. This gives a real file when the bundle is installed as a directory and extracts to a bundle-cache file when it is a jar. If the result is not a `file:` URL, or the file does not exist, copy the entry stream to a temp file and delete it afterwards. The code should do the temp-copy as the fallback so it works in both layouts.
- MANIFEST: `FileLocator` is in `org.eclipse.core.runtime`, which is already imported (`Import-Package: org.eclipse.core.runtime;version="3.7.0"`). **No MANIFEST change** is needed in ngclient.ui. In `com.servoy.eclipse.model` also no change, because only plain Java and the existing `Activator` are used.

### 2.5 Options, model and headless wiring (verified)

- `IWarExportModel extends IExportSolutionModel, ITiNGExportModel`. `exportNG2Mode()` is declared in `ITiNGExportModel`. Existing option getters, e.g. `isCreateTomcatContextXML()`, are plain `boolean` methods.
- Implementors that need the new methods:
  - `com.servoy.eclipse.warexporter.export.ExportWarModel extends AbstractWarExportModel` (wizard). Settings keys use the form `export.tomcat.createTomcatContextXML`, loaded in the `ExportWarModel(IDialogSettings)` constructor with `Utils.getAsBoolean(settings.get(...))` and saved in `saveSettings`.
  - `WarWorkspaceExporter.CommandLineWarExportModel extends AbstractWarExportModel` (headless, in `com.servoy.eclipse.exporter.solution`). It delegates to `cmdLineArguments` (`WarArgumentChest`), e.g. `isCreateTomcatContextXML()` returns `cmdLineArguments.isCreateTomcatContextXML()`.
  - `AbstractWarExportModel` is abstract. The new methods can be added to the interface without defaults, since both concrete models are updated. `MobileExporter` and `ExportMobileWizard` only implement `ING2WarExportModel`/`ITiNGExportModel` (not `IWarExportModel`), so they are unaffected.
- `WarArgumentChest` (in `com.servoy.eclipse.exporter.solution`, package `com.servoy.eclipse.exporter.apps.war`): flag options are `private static final String` names. `parseArguments(HashMap<String,String>)` stores `argumentsMap = argsMap`, and boolean getters use `argumentsMap.containsKey(name)` (e.g. `isCreateTomcatContextXML`). Help text is built in `getHelpMessage()`.
- `ExportWarWizard.appendToBuilder(...)` has `(sb, String arg, boolean)` and `(sb, String arg, String)` overloads, used in the command-line generation (around line 820-870).
- `DeployConfigurationPage` (wizard page, 3-column grid) holds the `context.xml` file text and browse button. The Tomcat checkboxes there are commented out. It also has a restore-defaults method (around line 405-425) that resets `exportModel` values.

### 2.6 web.xml template (verified)

`com.servoy.eclipse.model/src/com/servoy/eclipse/model/war/exporter/resources/web.xml` contains the comment `<!--  servoy web.xml version:7 -->` and ends with `</web-app>`. `WarExporter.copyWebXml()` loads it with `WarExporter.class.getResourceAsStream("resources/web.xml")` only when `exportModel.getWebXMLFileName() == null`. Otherwise it validates and copies the user's file. The content is copied raw through `copyStream`.

### 2.7 Test locations (verified)

- `com.servoy.eclipse.model.tests` — packages under `com.servoy.eclipse.model.tests.plainjunit` (e.g. `.builder`, `.util`, `.repository`). JUnit 6, Jupiter, `src/test/java`. Plain tests for `WarExporter` logic go here.
- `com.servoy.eclipse.ngclient.ui.tests` — `com.servoy.eclipse.ngclient.ui.tests.plainjunit` (e.g. `RunPNPMCommandTest`, `AngularLocalesCopyTest`). Tests for the ngclient.ui launcher and the Node script go here.
- There is no existing test project for `com.servoy.eclipse.exporter.solution` or `com.servoy.eclipse.exporter.war`. A `WarArgumentChest` test would need a new test project, so it is out of scope (see 6). The arg parsing is covered indirectly (see 4, step 11).
- Run plain tests with `eclipse-ide_runJUnitTests`, `junitVersion: "6"`.

## 3. Design

### 3.1 New model surface

`IWarExportModel` gets three methods:

```java
boolean isPrecompressBrotli();
boolean isPrecompressGzip();
boolean isInjectTomcatPrecompressed();
```

| Option | Saved-settings key | Headless arg | Meaning |
|---|---|---|---|
| precompressBrotli | `export.precompress.brotli` | `-precompressBrotli` | produce `.br` |
| precompressGzip | `export.precompress.gzip` | `-precompressGzip` | produce `.gz` |
| injectTomcatPrecompressed | `export.precompress.tomcat` | `-injectTomcatPrecompressed` | Tomcat DefaultServlet config in `web.xml` |

Semantics:

- All default to `false` (headless: absent means false; wizard: unset setting means false).
- The step runs when `isPrecompressBrotli() || isPrecompressGzip()` (computed in `WarExporter`, no helper on the interface). Brotli and gzip are each produced only if their own flag is true. With both off nothing runs.
- `injectTomcatPrecompressed` is fully independent. The wizard checkbox is always enabled. Headless: `web.xml` injection happens whenever the flag is given, even without `-precompressBrotli`/`-precompressGzip` (the user may precompress elsewhere). Document this in the help text.

### 3.2 Hook between model and ngclient.ui

In `com.servoy.eclipse.model.Activator`, next to `exportNG2ToWar` / `setNG2WarExporter`, add:

```java
public void precompressWar(IWarPrecompressModel model)       // calls the registered consumer, or logs a warning if none
public void setWarPrecompressor(Consumer<IWarPrecompressModel> precompressor)
```

`IWarPrecompressModel` is a small new interface in `com.servoy.eclipse.model.war.exporter` (exported package already used by ngclient.ui):

```java
public interface IWarPrecompressModel
{
    File getWarDirectory();          // tmpWarDir
    boolean isBrotli();
    boolean isGzip();
    IProgressMonitor getProgressMonitor();
}
```

If no precompressor is registered (ngclient.ui not started) the exporter throws an `ExportException`, since the user asked for it explicitly. Failures are reported with `ExportException`, never swallowed: a requested optimisation that silently did not run is worse than a failed export.

`com.servoy.eclipse.ngclient.ui.Activator.start()` registers `WebPackagesListener::precompressWar` (a new `public static void precompressWar(IWarPrecompressModel)`) next to the existing `setNG2WarExporter(...)` line. Both are done in `start()`.

### 3.3 `WarExporter` changes

New private method `runPrecompression(IProgressMonitor)`, called just before `zipDirectory`:

```java
if (exportModel.isPrecompressBrotli() || exportModel.isPrecompressGzip())
{
    if (exportModel.exportNG2Mode() != null && exportModel.exportNG2Mode().equals("false"))
        // log "Skipping precompression: no Titanium NGClient resources were exported (-ng2 false)"; no failure
    else
    {
        monitor.subTask("Precompressing static resources (" + SDF.format(new Date()) + ")");
        // RuntimeException from the hook -> ExportException("could not precompress the WAR resources", e); if monitor cancelled return null
    }
}
```

Logging in the skip case uses `ServoyLog.logInfo` (this file already uses `ServoyLog`). The progress `monitor` totals: `doExport` uses `SubMonitor.convert(m, "Creating War File", 42)` and has already reduced remaining work with `setWorkRemaining`. Use `monitor.worked(1)` only when the step ran, and keep the existing `worked` calls unchanged, so that the default path stays identical.

### 3.4 `WebPackagesListener.precompressWar` (ngclient.ui)

```text
Activator a = Activator.getInstance(); a.waitForNodeExtraction();
File node = a.getNodePath(); if node == null -> throw RuntimeException("Node.js is not available, cannot precompress the WAR resources")
File script = resolve /resources/precompress-war.mjs (see 2.4)
List<String> args = [ warDir.getCanonicalPath(), "--brotli"?, "--gzip"? ]
RunNPMCommand cmd = new RunNPMCommand(node, script, warDir, args);
cmd.runCommand(monitor);
exit code: EXIT_CODE_CANCELLED -> return (the exporter checks monitor.isCanceled()); != 0 -> throw RuntimeException
delete temp script copy if one was made
```

The args are passed as a list, so there is no shell and no quoting issue. `projectFolder` (working dir) is the war dir.

### 3.5 Node script `precompress-war.mjs` (selection rules live here only)

Usage: `node precompress-war.mjs <warDir> [--brotli] [--gzip]`. The script is plain ESM using only `node:fs`, `node:path` and `node:zlib` (no dependencies; `.mjs` so it works regardless of any `package.json` near the WAR dir).

Rules:

- Walk `warDir` recursively. **Skip the top-level directories `WEB-INF` and `META-INF` entirely.** Do not follow symlinks.
- Compress only these extensions (case-insensitive), kept in one constant at the top of the script: `js, mjs, cjs, css, html, htm, svg, json, xml, txt, map, wasm, ico, ttf, otf, eot`. Everything else is skipped, which covers `woff, woff2, png, jpg, jpeg, gif, webp, avif, zip, br, gz`.
- Skip files smaller than 1024 bytes (constant `MIN_SIZE`).
- Brotli: `zlib.brotliCompressSync(buf, { params: { [BROTLI_PARAM_QUALITY]: 11, [BROTLI_PARAM_LGWIN]: 24, [BROTLI_PARAM_SIZE_HINT]: buf.length, [BROTLI_PARAM_MODE]: text/generic } })`. Gzip: `zlib.gzipSync(buf, { level: 9 })`.
- Write `<file>.br` / `<file>.gz` next to the original only if the compressed size is strictly smaller than the original. Otherwise write nothing (and delete a stale sibling of the same name if one exists). The original is always kept.
- Never compress a file that already has a `.br` or `.gz` extension, and do not re-process siblings created in the same run (collect the file list first, then compress).
- Output: one summary line to stdout (`precompressed N files (br: X, gz: Y), saved Z bytes`) and one line per failure. Exit code 0 on success, 1 on any I/O or compression error, 2 on bad arguments. Memory: read each file fully (the biggest is a few MB of JS/wasm).
- Brotli at quality 11 is slow on big bundles. The process is synchronous and sequential, so it is cancellable by `RunNPMCommand.canceling()` killing the process.

### 3.6 Tomcat DefaultServlet config in `web.xml`

In `WarExporter.copyWebXml()`, when `name == null` (template path) and `exportModel.isInjectTomcatPrecompressed()`: read the template into a String (UTF-8) and insert the snippet immediately before the last `</web-app>`, then write it. The user-supplied `web.xml` path is never modified (log an info line if the flag is set but a custom `web.xml` is used). With the flag off, the template is copied byte-for-byte as before.

Snippet (keep as a `private static final String` in `WarExporter`, or a second resource `web-precompressed-snippet.xml` loaded the same way as `resources/web.xml`; the constant is preferred so the unit test needs no resource loading):

```xml
  <!-- SVY-21642: serve precompressed .br/.gz siblings (Tomcat DefaultServlet) -->
  <servlet>
    <servlet-name>default</servlet-name>
    <servlet-class>org.apache.catalina.servlets.DefaultServlet</servlet-class>
    <init-param>
      <param-name>precompressed</param-name>
      <param-value>br=.br,gzip=.gz</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
  </servlet>
```

Notes and corrections to the draft:

- The draft also had `<init-param>gzip=true</init-param>` and a `<servlet-mapping>` for `/`. Both are dropped. The `gzip` DefaultServlet parameter does not exist in current Tomcat (this code targets Tomcat 11, `org.apache.tomcat` bundle-version 11.0.8). The `default` servlet is already mapped to `/` by Tomcat's own `conf/web.xml`, so re-declaring only the servlet (same name) overrides its init-params. The coding agent must verify against the Tomcat 11 docs that the `precompressed` value format `br=.br,gzip=.gz` is accepted and that redeclaring `default` without `default` init params (listings, readonly, etc.) keeps them from the container default. If the override resets params, copy the container defaults that matter (`debug=0`, `listings=false`).
- Caveat to put in the tooltip/help: the snippet references a Tomcat class. Other containers ignore or may reject it, which is why the option is opt-in.
- Do NOT bump the version comment in the template `resources/web.xml`; it stays `servoy web.xml version:7`. `AbstractWarExportModel.checkWebXML()` compares a user-supplied `web.xml` version for strict equality with the template version, so a bump would reject every existing custom `web.xml` until users regenerate it. Resolved during implementation.
- Content-negotiation note: serving `.br` requires the client to send `Accept-Encoding: br` and a secure context (HTTPS) for browsers; manual test in 5.

### 3.7 Wizard, saved settings, headless args

- `ExportWarModel`: three `private boolean` fields, getters/setters (`setPrecompressBrotli` etc.), load in `ExportWarModel(IDialogSettings)` using `Utils.getAsBoolean(settings.get(key))`, save in `saveSettings(IDialogSettings)` next to the other `export.tomcat.*` puts.
- `DeployConfigurationPage`: new `Group` "Precompress static resources" (3-column span) below the context.xml controls: three plain checkboxes (brotli, gzip, "Inject Tomcat precompressed config in web.xml"), all always enabled and off by default. Tooltips state: needs the Titanium NGClient export, uses max compression (brotli 11 / gzip 9) so export takes longer, and the Tomcat caveat. Extend the page's restore-defaults method to reset all three to false. UI code runs on the SWT thread already (inside `createControl`/listeners); nothing special.
- `ExportWarWizard.appendToBuilder` command-line generation: emit ` -precompressBrotli`, ` -precompressGzip`, ` -injectTomcatPrecompressed` via `appendToBuilder(sb, " -x", boolean)`, each when its own flag is set.
- `WarArgumentChest`: constants `precompressBrotli`, `precompressGzip`, `injectTomcatPrecompressed`; getters `isPrecompressBrotli()` etc. using `argumentsMap.containsKey`. Add them to `getHelpMessage()` before the `-ng2` entry in the same style, mentioning "requires -ng2 other than false", "max compression", and the opt-in Tomcat caveat. Nothing to add in `parseArguments`, since flag-style args are read from `argumentsMap`. The coding agent must check that the chest's generic arg parsing treats an unknown flag without a value as a valid flag (as it does for `-createTomcatContextXML`).
- `WarWorkspaceExporter.CommandLineWarExportModel`: three delegating getters.

## 4. Implementation plan

All edits to files in Eclipse projects go through the eclipse-coder tools (via `execute` in Code Mode). After each Java change run `getCompilationErrors`, organize imports and format.

1. `com.servoy.eclipse.model`: new `src/com/servoy/eclipse/model/war/exporter/IWarPrecompressModel.java` (3.2). Package already exported (verify in MANIFEST `Export-Package`; ngclient.ui already imports `com.servoy.eclipse.model.war.exporter`).
2. `com.servoy.eclipse.model/src/com/servoy/eclipse/model/Activator.java`: add the `precompressor` field, `precompressWar(IWarPrecompressModel)` and `setWarPrecompressor(...)` (next to `exportNG2ToWar`/`setNG2WarExporter`).
3. `com.servoy.eclipse.model/.../IWarExportModel.java`: add the three methods (3.1).
4. `com.servoy.eclipse.model/.../WarExporter.java`:
   - add `runPrecompression(IProgressMonitor)` and call it just before `zipDirectory(tmpWarDir, warFile)` in `doExport`, gated on brotli||gzip (3.3);
   - in `copyWebXml()` add the injection (3.6) via a small package-visible static helper `static String injectPrecompressedServlet(String webXml)` so it can be unit-tested without a model; add the snippet constant.
5. `com.servoy.eclipse.model/src/com/servoy/eclipse/model/war/exporter/resources/web.xml`: no change. The version stays `servoy web.xml version:7` (see 3.6; `AbstractWarExportModel.checkWebXML` is a strict equality check).
6. `com.servoy.eclipse.ngclient.ui`:
   - new `resources/precompress-war.mjs` (3.5). No `build.properties` or MANIFEST change (2.4);
   - `WebPackagesListener.java`: add `public static void precompressWar(IWarPrecompressModel)` (3.4) and a package-visible helper `static List<String> buildPrecompressArgs(File warDir, boolean brotli, boolean gzip)` for tests;
   - `Activator.start()`: `com.servoy.eclipse.model.Activator.getDefault().setWarPrecompressor(WebPackagesListener::precompressWar);`.
7. `com.servoy.eclipse.exporter.solution`: `WarArgumentChest.java` (constants, getters, help text) and `WarWorkspaceExporter.java` (three delegating getters in `CommandLineWarExportModel`). No MANIFEST change expected, so check that it already requires the model bundle (it implements `AbstractWarExportModel`, so it does).
8. `com.servoy.eclipse.exporter.war`: `ExportWarModel.java` (fields, accessors, load/save settings), `DeployConfigurationPage.java` (group, restore defaults), `ExportWarWizard.java` (command-line args). MANIFEST unchanged.
9. Any other `IWarExportModel` implementor found by `findReferences`/compile errors across the workspace (including test mocks) must get the new methods.
10. Tests (JUnit 6, plain):
    - `com.servoy.eclipse.model.tests.plainjunit.war.WarExporterPrecompressedWebXmlTest`: `injectPrecompressedServlet` on the real template (read via `WarExporter.class.getResourceAsStream("resources/web.xml")`): snippet inserted once before `</web-app>`, result is well-formed XML (parse with `DocumentBuilderFactory` with secure processing), contains `precompressed` `br=.br,gzip=.gz`, version comment is still `version:7`; and with the option off the template is unchanged (assert on the copy path via the helper's caller if testable, otherwise assert the helper is not applied).
    - `com.servoy.eclipse.ngclient.ui.tests.plainjunit.PrecompressWarScriptTest`: runs `resources/precompress-war.mjs` with the `node` from the system property `servoy.nodePath` (the same property `Activator.extractNode()` honours) or, if unset, from the bundled nodejs plugin / PATH, against a fabricated temp WAR tree containing `WEB-INF/x.js`, `META-INF/y.css`, `a.js` (compressible, > 1 KB), `locales/b.json`, `small.js` (< 1 KB), `font.woff2`, `rand.js` (random incompressible bytes), `already.js.gz`. Asserts: `.br`/`.gz` for `a.js` and `locales/b.json` only; none under WEB-INF/META-INF; none for small/woff2/rand/already-compressed; originals untouched; decoding each sibling (`zlib.brotliDecompress`/`GZIPInputStream`) equals the original; brotli-only flag produces no `.gz` and vice versa; exit code 2 for missing arg. Per project rules there is no `Assume` skipping. If node cannot be found the test fails with a clear message, so the test environment must provide it (see open question).
    - `WebPackagesListener.buildPrecompressArgs` unit test (same project): exact argument lists for brotli-only / gzip-only / both.
    - Run with `eclipse-ide_runJUnitTests`, `junitVersion: "6"`.
11. Headless arg parsing and wizard/settings round trip have no existing test project. Verify manually (see 5) and note this gap in the review.
12. After all changes: `getCompilationErrors` on `com.servoy.eclipse.model`, `com.servoy.eclipse.ngclient.ui`, `com.servoy.eclipse.exporter.solution`, `com.servoy.eclipse.exporter.war`; fix Spotbugs high-severity findings in new code.

## 5. Acceptance criteria

- [ ] With all three options off (default), the exported WAR is identical to today's: no `.br`/`.gz` files, `web.xml` byte-identical to the template.
- [ ] With brotli and/or gzip on and NG2 exported, `.br` and/or `.gz` siblings exist for selected extensions in the WAR root and sub-directories (Angular locale chunks, tinymce assets, css, js).
- [ ] Nothing under `WEB-INF/` or `META-INF/` is touched.
- [ ] A sibling is written only when strictly smaller than the original; files under 1 KB are skipped; originals are always kept.
- [ ] Already-compressed/binary formats (woff, woff2, png, jpg, gif, webp, avif, zip, br, gz) get no sibling.
- [ ] Max level is used (brotli quality 11, lgwin 24; gzip level 9), with no level option.
- [ ] `-ng2 false` with precompression on: export succeeds, step is skipped with a log message.
- [ ] If Node is unavailable or the script fails, the export fails with an `ExportException` carrying the reason. A cancel stops the process and the export returns quietly.
- [ ] `injectTomcatPrecompressed` on and no custom `web.xml`: generated `web.xml` declares the `default` servlet with `precompressed` and is well-formed. With a custom `web.xml`: unchanged. With the flag off: unchanged. Template version comment stays `7`.
- [ ] Headless export honours the three args; the wizard-generated command line emits them when set; the settings round-trip through `IDialogSettings`; restore-defaults resets them.
- [ ] Wizard UI: three plain checkboxes, all always enabled; defaults are off.
- [ ] New plain JUnit tests (10) pass; `getCompilationErrors` is clean in all four touched bundles.
- [ ] Manual check (not automated): real production WAR on Tomcat 11 serves `Content-Encoding: br` (HTTPS, `Accept-Encoding: br`) for a precompressed asset and the plain original for a client not accepting br/gzip.

## 6. Out of scope

- Compressing resources served out of jars inside `WEB-INF/lib` at runtime.
- Container-specific config for non-Tomcat servers (Jetty, etc.).
- A user-facing compression level option.
- Brotli via brotli4j/JNI (Option B) and gzip-only fallbacks (Option C).
- Precompression for pure smart-client WARs (`-ng2 false`), which is a documented no-op.
- A new test project for `com.servoy.eclipse.exporter.solution` / `exporter.war` (arg parsing and wizard wiring are verified manually).
- The legacy Tomcat `context.xml` options (commented out in the wizard) are unchanged.

## 7. Open questions

| Question | Owner | Status |
|---|---|---|
| Does Tomcat 11 `DefaultServlet` accept `precompressed=br=.br,gzip=.gz`, and does redeclaring the `default` servlet in `web.xml` keep the container's default init params (listings, readonly)? Coding agent verifies against the Tomcat 11 docs/source. | Coding agent | Open |
| Does bumping the web.xml version to 8 interact with `AbstractWarExportModel.checkWebXML()` for user-supplied files (strict equality would force users to regenerate)? | Coding agent / human | Resolved: strict equality, so the version stays 7 |
| How does the plain-junit test environment (and Jenkins) locate `node` for `PrecompressWarScriptTest`? Use `servoy.nodePath` or the extracted bundled node; fail (not skip) if absent. | Human | Resolved: approved as proposed |
| Headless `-injectTomcatPrecompressed` without `-precompressBrotli`/`-precompressGzip` still injects the config (design 3.1). | Human | Resolved: confirmed, inject independently of precompression |
| Node missing or script failure is a hard export failure (not a warning). | Human | Resolved: confirmed, hard failure with ExportException |
