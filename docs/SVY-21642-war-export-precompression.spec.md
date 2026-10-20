# SVY-21642 — WAR export: optional precompressed brotli/gzip + optional Tomcat config

> Status: **draft / half spec** — captured now so the case can be picked up later via an SDD run.
> Nothing is implemented yet. This document fixes the scope, the integration point, the open
> design decision, and the acceptance criteria so implementation can start cleanly.

Jira: SVY-21642 (Task, R&D - New Capability)

## Problem

When a WAR is exported with Angular compiled in **production mode**, every static resource that
lands in the WAR root — the Angular build artifacts plus everything copied alongside it (locales,
css, tinymce assets, fonts, json, …) — is served **uncompressed** unless the servlet container or a
reverse proxy compresses it on the fly. On-the-fly compression (brotli especially) costs CPU per
request and gzip-on-the-fly is a middling compromise.

We want the exporter to be able to **precompute** the compressed variants once, at export time,
and ship them inside the WAR, so a container can serve the already-compressed bytes.

## Goal

Add an **opt-in** WAR-export step that, just before the temp WAR directory is zipped, precompresses
the compressible static resources into sibling `.br` (brotli) and/or `.gz` (gzip) files. Plus an
**optional, separate** sub-option to inject Tomcat `DefaultServlet` precompressed-serving config, so
Tomcat serves the precomputed variants out of the box — without making the WAR Tomcat-specific.

## Where it plugs in

`com.servoy.eclipse.model.war.exporter.WarExporter.doExport(IProgressMonitor)` is the single
orchestration method. Today the tail of it is:

```
copyRootWebappFiles → copyPlugins → copyStandardLibs → copyDrivers → moveSlf4j
→ copyWebXml → copyLog4jConfigurationFile → createTomcatContextXML → addServoyProperties
→ exportAdminUser → copyComponentsAndServicesPlusLibs → copyExportedComponentsAndServicesPropertyFile
→ copyNGClient2 (NG2 production build: copies dist/app/browser into the WAR root,
                 index.html → WEB-INF/angular-index.html)   [only when exportNG2Mode != "false"]
→ copyWARLibs → createDeployPropertiesFile → checkDuplicateJars
→ (move plugins into WEB-INF/lib)
→ zipDirectory(tmpWarDir, warFile)   ← precompression goes IMMEDIATELY BEFORE this
→ deleteDirectory(tmpWarDir)
```

The new step runs **after** all copy steps have populated `tmpWarDir` (root + sub-dirs) and
**immediately before** `zipDirectory`, so it sees the final file set, including the NG2 locales and
any package assets copied into sub-dirs.

The NG2 production build itself happens in
`com.servoy.eclipse.ngclient.ui.WebPackagesListener.exportNG2ToWar(ING2WarExportModel)`, which copies
`dist/app/browser` into the export location. Precompression deliberately runs **in WarExporter after**
that copy, not inside `exportNG2ToWar`, so it also covers non-NG2 root files and keeps one code path.

## File selection

Walk `tmpWarDir` recursively.

- **Skip directories** `WEB-INF/` and `META-INF/` entirely (jars, classes, config — not statically
  served, and `WEB-INF/angular-index.html` is served through a filter, not as a static file).
- **Compress** only text-like / highly-compressible extensions. Starting set (configurable constant):
  `js, mjs, cjs, css, html, htm, svg, json, xml, txt, map, wasm, ico, ttf, otf, eot`.
  This recurses into sub-dirs, so Angular locale chunks and tinymce `.js`/`.css` are included.
- **Skip** already-compressed / binary formats: `woff, woff2, png, jpg, jpeg, gif, webp, avif, zip,
  br, gz` (and anything not in the compress set).
- **Size guards:**
  - skip files below a small threshold (≈ 1 KB — compression overhead not worth it);
  - only write a `.br`/`.gz` if it is **actually smaller** than the original; otherwise discard it.
- **Compression level: always maximum.** Precompression is a one-off cost at export time, while
  decompression on the client is **level-independent** — a brotli-11 / gzip-9 file decodes just as
  fast (if not faster, because fewer bytes) as a lower-level one; only the encoder works harder. So
  there is no runtime downside to maxing it out, and we do:
  - **brotli**: quality `11` (max), window `lgwin = 24` — the standard for static-asset precompression;
  - **gzip**: level `9` (`Deflater.BEST_COMPRESSION`).
  - No user-facing level knob. The only reason to ever lower it would be export duration (brotli-11
    over a large WAR can take noticeable time); revisit a level option only if that becomes a problem
    in practice.
- **Output:** write siblings next to the original — `app.js` → `app.js.br` and/or `app.js.gz`. The
  **original stays** (fallback for clients / containers that do not do precompressed serving).

## New export options

All **off by default**. Each must be wired the same way the existing options are (saved
`IDialogSettings` key, wizard control, and headless command-line arg), mirroring e.g.
`createTomcatContextXML`:

| Option | Setting key | Headless arg | Meaning |
|---|---|---|---|
| `precompressResources` | `export.precompress.enabled` | `precompressResources` | master switch for the step |
| `precompressBrotli` | `export.precompress.brotli` | `precompressBrotli` | produce `.br` (default on when the master switch is on) |
| `precompressGzip` | `export.precompress.gzip` | `precompressGzip` | produce `.gz` (default on when the master switch is on) |
| `injectTomcatPrecompressed` | `export.precompress.tomcat` | `injectTomcatPrecompressed` | add Tomcat DefaultServlet precompressed config |

Model surface to add (keep the existing naming style):

- `IWarExportModel`: `boolean isPrecompressResources()`, `isPrecompressBrotli()`,
  `isPrecompressGzip()`, `isInjectTomcatPrecompressed()`.
- `ExportWarModel` (wizard): fields + getters/setters, load in the `ExportWarModel(IDialogSettings)`
  constructor, persist in `saveSettings(IDialogSettings)`.
- `WarArgumentChest` (headless): the four `-...` args parsed in `parseArguments`, plus help text.
- `ExportWarWizard.appendToBuilder(...)`: emit the args into the generated command line when set.

Wizard UI: a new group on `DeployConfigurationPage` (where the — currently commented-out — Tomcat
context.xml controls live). Master checkbox enables the three sub-checkboxes; `injectTomcatPrecompressed`
is only meaningful when the master switch is on.

## Open design decision — how to produce the `.br` files

Gzip is trivially pure-JDK (`java.util.zip.GZIPOutputStream`, max level), **no new dependency**.
Brotli is the open question because the JDK has no brotli and there is no maintained pure-Java
**encoder**.

### Option A (recommended) — reuse the bundled Node toolchain (`zlib`)

Node ships brotli **and** gzip in its built-in `zlib` module (`zlib.brotliCompressSync`,
`zlib.gzipSync`) — **no npm package needed**. During a production WAR export the bundled Node is
already extracted and on the PATH, because `copyNGClient2` has just run the NG2 production build
through `RunNPMCommand` / `RunPNPMCommand` (`com.servoy.eclipse.ngclient.ui.Activator.getNodePath()`).

Approach: run a small bundled Node script that walks the WAR directory, applies the selection rules
above, and writes `.br`/`.gz` siblings, launched with the existing
`RunNPMCommand`/`RunPNPMCommand` process pattern (correct per-OS node binary, PATH already handled).

- **Pros:** no new Maven/target-platform dependency; brotli + gzip from one engine; platform/native
  problem already solved by the shipped Node; isolated from the JVM.
- **Cons:** couples precompression to Node being present. In practice that is fine — there is nothing
  to precompress in a pure smart-client WAR (`exportNG2Mode() == "false"`), which is exactly the case
  where Node may be absent. Guard: only offer/run precompression when NG2 resources were exported.
- **Note:** the walk/selection can stay in Java (JDK gzip) with only the **brotli** bytes produced by
  Node, or the whole walk can be the Node script. Prefer the Node script doing both, so the rules live
  in one place.

### Option B — `brotli4j` (JNI natives) in Java

`com.aayushatharva.brotli4j` (actively maintained, 2026) with `BrotliOutputStream`. Would need the
per-OS native fragment bundles added to the target platform, and Windows build agents need the MSVC
redistributable. Graceful fallback: if `Brotli4jLoader.ensureAvailability()` fails, log a warning,
skip `.br`, still write `.gz`.

- **Pros:** stays inside the JVM; best ratio; works even for non-NG2 WARs.
- **Cons:** most target-platform / native plumbing; platform-specific build-agent requirement.

### Option C — gzip only now, brotli later

Ship the whole feature (options, Tomcat config, selection) **gzip-only** using the JDK, and add
brotli behind the already-present `precompressBrotli` switch in a follow-up. Zero new dependencies,
works everywhere headless. Gzip is ~15–20% larger than brotli but still a large win over nothing.

**Recommendation:** start from **Option A** (Node `zlib`), because it gives both formats with no new
dependency and the toolchain is already there for the production build. Fall back to **Option C** if
the SDD decides the Node coupling is undesirable; **Option B** only if brotli for non-NG2 WARs is a
hard requirement.

## Optional Tomcat serving config (container-neutral)

A WAR must not require Tomcat; other containers must still deploy it. So the extra `.br`/`.gz` files
are inert by default (just unused siblings), and the serving config is **opt-in**.

When `injectTomcatPrecompressed` is on **and** the user has not supplied their own `web.xml`
(`getWebXMLFileName() == null`, the resource-template path in `copyWebXml()`), add an explicit
re-declaration of the container DefaultServlet with precompressed serving to the generated
`WEB-INF/web.xml` (the template at
`com.servoy.eclipse.model/.../war/exporter/resources/web.xml`):

```xml
<servlet>
  <servlet-name>default</servlet-name>
  <servlet-class>org.apache.catalina.servlets.DefaultServlet</servlet-class>
  <init-param><param-name>precompressed</param-name><param-value>br,gzip</param-value></init-param>
  <init-param><param-name>gzip</param-name><param-value>true</param-value></init-param>
</servlet>
<servlet-mapping>
  <servlet-name>default</servlet-name>
  <url-pattern>/</url-pattern>
</servlet-mapping>
```

- This is valid Jakarta `web.xml`; non-Tomcat containers that do not know this servlet class will
  ignore or fail-soft on the override (document the caveat), which is why it is opt-in and tied to the
  "inject Tomcat-specific" choice.
- Decide during implementation whether to put this in `web.xml` (above) or in
  `META-INF/context.xml` via `createTomcatContextXML()`. `web.xml` is self-contained inside the WAR;
  `context.xml` is already the Tomcat-specific place but is currently only written on a separate
  option. Leaning `web.xml` so it travels with the WAR regardless of the context.xml option.
- Bump the Servoy `web.xml` version comment (`servoy web.xml version:N`) when the template changes.

## Acceptance criteria

1. With all precompress options off (default), the exported WAR is **byte-for-byte unchanged** from
   today — no `.br`/`.gz` files, no `web.xml` change.
2. With `precompressResources` on, the WAR root and its sub-dirs contain `.br` and/or `.gz` siblings
   (per the brotli/gzip sub-switches) for the selected text extensions, including NG2 locale chunks
   and tinymce assets in sub-dirs.
3. `WEB-INF/` and `META-INF/` are never touched; no `.br`/`.gz` appear there.
4. A `.br`/`.gz` is written only when smaller than its original; sub-threshold files are skipped;
   originals are always kept.
5. Already-compressed assets (woff2, png, …) get no sibling.
6. With `injectTomcatPrecompressed` on and no user `web.xml`, the generated `web.xml` declares the
   DefaultServlet with `precompressed="br,gzip"`; with it off, `web.xml` is unchanged.
7. Headless export honours the four new command-line args; the wizard's generated command line emits
   them when set; the settings round-trip through `IDialogSettings`.
8. Non-NG2 (pure smart-client) export does not fail when precompression is on — it is a safe no-op /
   skipped (also drives the Option-A "Node may be absent" guard).

## Tests (to design in the SDD)

- **Selection logic** (pure unit, `com.servoy.eclipse.model.tests` or the war-exporter project):
  a helper that classifies a path as compress / skip, over a fabricated tree with `WEB-INF`,
  `META-INF`, nested `locales/`, mixed extensions, and already-`.br` files — asserts exactly the
  expected set, the threshold, and the "only-if-smaller" rule (feed it incompressible bytes).
- **web.xml injection** (unit): given the template + option on/off, assert the DefaultServlet /
  `precompressed` block is present/absent and the XML is well-formed.
- **Headless args** (unit on `WarArgumentChest`, like `RunPNPMCommandTest`): the four args parse into
  the model flags; absent → defaults off.
- **Integration** (manual / heavy): a real production WAR export with the option on, deployed to
  Tomcat, confirming `Content-Encoding: br` is served for a precompressed asset and the original is
  served to a client that does not accept br/gzip.

## Out of scope

- Compressing resources that are served out of jars at runtime (sablo/ngclient static resources read
  from inside WEB-INF/lib jars) — this only precompresses files physically in the WAR root tree.
- Per-container config for non-Tomcat servers (Jetty, etc.) — users configure those themselves; the
  precompressed files are available for them to use.
