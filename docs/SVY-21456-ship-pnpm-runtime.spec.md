# Spec: SVY-21456 — Ship pnpm alongside npm inside the Node.js runtime plugins (opt-in)

## 1. Goal

Add [pnpm](https://pnpm.io) as an *opt-in, coexisting* package manager for the Titanium
NG (TiNG) client build, shipped **inside the existing per-OS/arch Node.js runtime plugins**
(the same way `npm` already ships as part of the bundled Node.js), rather than as separate
plugins and rather than replacing npm. A new `servoy.jsRuntime=pnpm` system property
switches Servoy Developer into "pnpm mode"; by default it keeps using npm exactly as today.
pnpm uses a global content-addressable store with hardlinks + a symlinked / flattened
`node_modules`, which eliminates the duplicated `node_modules` copies that Servoy currently
works around with a hand-written dedup pass in `WebPackagesListener`. In pnpm mode that
custom dedup does not run.

pnpm is chosen over Bun because pnpm is a **Node.js-based, first-class Angular citizen**:
it runs the Angular CLI build through the *bundled Node* (`pnpm run build` → `node .../ng.js`),
so there is no JS-engine-compatibility risk. Bun cannot reliably run `@angular/build` (it
delegates the CLI to a *system* `node` we cannot assume exists, and forcing Bun's own engine
deadlocks the Angular compiler). pnpm delivers the same global-store dedup win with none of
Bun's build risk.

Because pnpm must invoke the bundled Node to run scripts, and we cannot assume Node is
installed globally, pnpm is **extracted into the very same directory as the bundled Node**
(the plugin state location under the Eclipse `.metadata/.plugins/...` dir). With both
`node(.exe)` and `pnpm(.exe)` sitting next to each other and that directory on `PATH`,
`pnpm run build` finds the bundled Node automatically. Shipping pnpm inside the Node plugin
also keeps the two binaries versioned and delivered as one coherent unit — exactly like npm
today.

The `--legacy-peer-deps` flag is removed from **all** runners (npm and pnpm), since it is no
longer needed. Keeping npm as the instant default fallback lets us measure pnpm on real
solutions and revert quickly.

## 2. Background

### 2.1 How the Node.js/npm runtime is shipped today

- Servoy ships one platform fragment per OS/arch, each carrying a Node.js archive and
  declaring the `node`/`npm` relative paths via the `com.servoy.eclipse.ngclient.ui.nodejs`
  extension point:
  - `com.servoy.eclipse.nodejs.win32.win32.x86_64`
  - `com.servoy.eclipse.nodejs.win32.win32.aarch64`
  - `com.servoy.eclipse.nodejs.macosx.cocoa.x86_64`
  - `com.servoy.eclipse.nodejs.macosx.cocoa.aarch64`
  - `com.servoy.eclipse.nodejs.linux.gtk.x86_64`
- Each fragment is a small PDE plugin. Example
  `com.servoy.eclipse.nodejs.win32.win32.x86_64/plugin.xml`:
  ```xml
  <extension point="com.servoy.eclipse.ngclient.ui.nodejs">
     <nodejs id="…" name="…"
        nodePath="node-v24.18.0-win-x64/node.exe"
        npmPath="node-v24.18.0-win-x64/node_modules/npm/bin/npm-cli.js"
        archive="node.zip">
     </nodejs>
  </extension>
  ```
  Its `pom.xml` uses `download-maven-plugin` (`wget` goal, `generate-resources` phase) to
  fetch `node-v${project.version}-win-x64.zip` from `nodejs.org` into `node.zip` (which is
  git-ignored). `npm` is already inside that Node archive
  (`node_modules/npm/bin/npm-cli.js`) — npm ships *as part of* the Node runtime.
- The extension-point schema is `com.servoy.eclipse.ngclient.ui/schema/nodejs.exsd`; the
  extension point is declared in `com.servoy.eclipse.ngclient.ui/plugin.xml`.
- All five fragments are registered as `<module>`s in the root `pom.xml` and as `<plugin>`s
  in `com.servoy.eclipse.feature/feature.xml`.

### 2.2 How the runtime is resolved and invoked

- `Activator.extractNode()`
  (`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/Activator.java:142`)
  reads the first `nodejs` extension, calls `extractPath()` (`:202`) to unzip the archive
  into the plugin state location on first run (guarded by a `.fullygenerated` marker), and
  stores `nodePath` / `npmPath` `File`s. It honours the existing overrides
  `servoy.nodePath` / `servoy.npmPath` (`Activator.java:146`). `extractPath(element,
  attribute, deletePreviousPaths)` (`:202`) unpacks `archive` into the state location and
  returns the `File` at the attribute's relative path — it is generic and reusable.
- `Activator.createNPMCommand(File folder, List<String> commandArguments)`
  (`Activator.java:271`) is the single factory. It returns a `NoOpNPMCommand` when no
  runtime is present, else `new RunNPMCommand(nodePath, npmPath, folder, commandArguments)`.
- `RunNPMCommand`
  (`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/RunNPMCommand.java`)
  implements `IRunNPMCommand`. It builds the process command line as
  `node <npm-cli.js> <args…>` (`:172-174`), prepends the node dir to `PATH` (`:155`), sets
  `NODE_OPTIONS` and `NG_PERSISTENT_BUILD_CACHE` (`:157-158`), spawns via `ProcessBuilder`,
  streams stdout to the "Titanium NG Build Console", and detects NG-build completion by
  sniffing an output line containing `Date:` + `Hash:` + `Time:` (`:202-205`).
- `IRunNPMCommand`
  (`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/IRunNPMCommand.java`)
  is the abstraction the callers use; `NoOpNPMCommand` is the no-runtime fallback.

### 2.3 The command sequence and custom dedup

`WebPackagesListener.PackageCheckerJob.run()`
(`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/WebPackagesListener.java`)
is the main caller. When a rebuild is needed it runs, in order:

1. `run build_lib_debug_nowatch` (`:705-706`)
2. `install <packages…> ./dist-public/ --legacy-peer-deps` (`:727-732`)
3. either `ci --legacy-peer-deps` (`:752`) or `update --legacy-peer-deps` (`:771`)
4. `dedup` (`:788`)
5. a **custom hand-written dedup** (`:804-864`) that compares the solution's `node_modules`
   against the *root* `node_modules` and physically deletes duplicate package folders shared
   between them (via `DeletePathVisitor`), to reduce the "copies everywhere" problem.
6. `run <whatToRun>` — the Angular CLI build (`build_debug_nowatch` / `build` /
   `build_sourcemap` etc., `:882`).

Other callers:

- `NodeFolderCreatorJob` (`.../NodeFolderCreatorJob.java:243-250`) runs
  `uninstall @servoy/public` then `install --legacy-peer-deps` on the parent root folder.
- `NGClientStarter` also drives builds via the same factory.
- `NGClientConstants.NG_BUILD_COMMAND = ["run-script", "build_debug"]`
  (`.../utils/NGClientConstants.java:31`) is a shared build command list, used by
  `RunNPMCommand` for the NG-build-running flag detection.

### 2.4 Why `--legacy-peer-deps` exists

`--legacy-peer-deps` is passed because the source-included Angular component packages have
peer-dependency conflicts (documented in the repo's own "Troubleshooting: Angular Source
Build Failures" notes in `AGENTS.md`). The approved approach is to remove it entirely for
both runtimes; the peer-dep alignment across component `package.json` files is assumed to
already be in place / validated when the build is actually run.

### 2.5 Why pnpm and not Bun (the pivot)

The original investigation considered Bun. Bun fails a hard requirement: it cannot drive the
Angular CLI build without a *system* Node install. When `bun run build` executes the mapped
`ng build`, Bun resolves `node_modules/.bin/ng` — a `#!/usr/bin/env node` shebang or a
`node`-invoking launcher — and delegates to the system `node`, which we cannot assume exists.
Forcing Bun's own engine (`bun --bun run build`) does not help: `@angular/build` relies on
Node-specific native APIs (`node:worker_threads`, IPC pipes, `sass-embedded`) and
deadlocks/crashes under Bun's JavaScriptCore engine.

pnpm avoids this entirely: pnpm is *itself* a Node program and runs scripts through Node. By
extracting pnpm next to the bundled Node and putting that directory on `PATH`, `pnpm run
build` uses the bundled Node and the Angular CLI build runs byte-for-byte as it does today.
pnpm still delivers the "no duplicated node_modules" win through its global
content-addressable store (hardlinks) + symlinked/flattened `node_modules`, which is exactly
the mechanism the custom dedup was hand-rolling.

### 2.6 Git history (from triage)

- The Node payload is refreshed periodically, unrelated to this case; newest bump is commit
  `73b46503c8` ("node js update", 2026-07-29) to `node-v24.18.0`.
- The npm command sequence + custom dedup were last tuned under **SVY-21284**
  ("Investigate how to run integration tests faster", commits `af87d1794a`, `0cf972fde4`,
  `6a9b319fa9`, `1824f05927`) — deliberately optimised code, so pnpm mode must *bypass* it
  cleanly rather than delete the npm path.

## 3. Design

### 3.1 Runtime selection via system property

Introduce a single selector, read once in `Activator`:

- Property name: `servoy.jsRuntime`, values `npm`/`node` (default) or `pnpm`. Read through
  the existing `getSystemOrEvironmentProperty(...)` helper so both `-D` system properties and
  environment variables work, consistent with `servoy.nodePath` / `servoy.npmPath`.
- Add a helper `Activator.isPnpmMode()` returning `true` when the property equals `pnpm`
  (case-insensitive). All new branching keys off this one method.
- npm remains the default: with the property unset, behaviour is byte-for-byte the current
  behaviour.

### 3.2 pnpm ships inside the Node.js plugins and extracts next to Node

Rather than new fragment plugins, pnpm is bundled **into each existing
`com.servoy.eclipse.nodejs.*` plugin** — the same way npm already ships inside the Node
archive. This keeps Node + npm + pnpm delivered and versioned as one unit.

- Each Node plugin gains a **second downloaded artifact**, `pnpm.zip` (or the raw
  standalone binary packed into a zip), fetched by a second `download-maven-plugin`
  execution in that plugin's `pom.xml` from pnpm's official release for the matching
  platform. `pnpm.zip` is git-ignored like `node.zip`.
- `build.properties` in each Node plugin bundles both `node.zip` and `pnpm.zip`.
- The `nodejs` extension point gains an **optional** `pnpmPath` attribute (see §3.3). Each
  Node plugin's `<nodejs>` element declares where the pnpm binary lands.
- On first run, `Activator` extracts Node as today, and — when needed — **extracts pnpm into
  the same plugin state-location directory** (the Eclipse
  `.metadata/.plugins/com.servoy.eclipse.ngclient.ui/...` dir where Node is unpacked). The
  two executables end up in / next to the same directory, so with that directory on `PATH`
  they find each other: `pnpm run build` locates the bundled `node`.

  Two acceptable layouts (implementer's choice, whichever the pnpm archive makes cleanest):
  either drop `pnpm(.exe)` directly beside `node(.exe)` inside the extracted Node dir, or
  extract it to a sibling dir and add **both** dirs to `PATH`. The requirement is only that
  `pnpm` can resolve the bundled `node` at runtime without a system install.

### 3.3 Extending the `nodejs` extension point (optional `pnpmPath`)

Extend the existing schema `com.servoy.eclipse.ngclient.ui/schema/nodejs.exsd` rather than
adding a new extension point (pnpm belongs to the Node install, mirroring how `npmPath`
already lives on the same element):

- Add an **optional** `pnpmPath` attribute to the `<nodejs>` element (relative path to the
  `pnpm`/`pnpm.exe` binary). Optional so that a Node plugin without a bundled pnpm still
  validates and npm mode is unaffected.
- Add a corresponding **optional** `pnpmArchive` attribute (the bundled pnpm zip within the
  plugin), separate from the existing required `archive` (the Node zip). If the pnpm archive
  is instead merged into `node.zip` at build time, `pnpmArchive` can be omitted and only
  `pnpmPath` is needed — implementer picks whichever packaging is simplest, but the schema
  supports the separate-archive form.
- Update each `com.servoy.eclipse.nodejs.*/plugin.xml` `<nodejs>` element to add `pnpmPath`
  (and `pnpmArchive` if used).

### 3.4 `Activator` — extracting pnpm and selecting the runner

- Add `isPnpmMode()` reading `servoy.jsRuntime`.
- Add a `pnpmPath` `File` field. In `extractNode()`:
  - Always extract Node (`nodePath`/`npmPath`) as today — Node is needed for the Angular
    build and to run pnpm.
  - **Additionally**, when a `pnpmPath` (and `pnpmArchive`, if separate) attribute is present
    on the extension, extract pnpm via the generic `extractPath(...)` into the same state
    location, mark it executable (as Node is at `:169`), and store `pnpmPath`. Do this
    unconditionally when the attribute exists (cheap; makes pnpm available whenever selected)
    or gate on `isPnpmMode()` — implementer's choice; gating on `isPnpmMode()` avoids the
    extra unzip in the default path.
  - Honour a new optional override `servoy.pnpmPath` alongside the existing
    `servoy.nodePath` / `servoy.npmPath`.
- In `createNPMCommand(...)`: in pnpm mode return
  `new RunPNPMCommand(pnpmPath, nodePath, npmPath, folder, args)` when both `pnpmPath` and
  `nodePath` are available, else `NoOpNPMCommand`. In npm mode, return `RunNPMCommand`
  exactly as today.

### 3.5 `RunPNPMCommand` — a pnpm-aware `IRunNPMCommand`

Add `com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/RunPNPMCommand.java`
implementing `IRunNPMCommand`, structurally based on `RunNPMCommand` (same `WorkspaceJob`,
console streaming, cancel handling, exit-code plumbing, environment setup). It holds
`pnpmPath` and `nodePath` and prepends the **Node directory** (and the pnpm directory if it
is separate) to `PATH`, and keeps `NODE_OPTIONS` / `NG_PERSISTENT_BUILD_CACHE`. This is what
lets `pnpm run build` find the bundled Node.

- **Command line:** `pnpm <args…>` (single binary), instead of `node <npm-cli.js> <args…>`.
- **Verb translation** — pnpm's CLI is close to npm's; map the verbs Servoy actually uses:
  - `install` → `pnpm install` when no explicit packages are listed; `pnpm add <pkg…>` when
    the command carries package names + a local path (pnpm splits "install all deps" from
    "add these packages", whereas npm's `install <pkg>` does both). The main install call
    (`WebPackagesListener:727-730`) lists packages + `./dist-public/`; in pnpm mode these
    package arguments are **tarball paths** (`.tgz`), not directory paths, so it maps to
    `pnpm add <pkg1.tgz> <pkg2.tgz> …` (see §3.7.1 — directory deps do not bring their
    transitive dependencies under pnpm; tarballs do).
  - `ci` → `pnpm install --frozen-lockfile`.
  - `update` → `pnpm update`.
  - `uninstall <pkg>` → `pnpm remove <pkg>`.
  - `run <script>` / `run-script <script>` → `pnpm run <script>` (runs via bundled Node).
  - `dedup` → **no-op** (return exit code 0 without spawning a process). Deduplication is
    handled by pnpm's global store + symlinked `node_modules` (§3.7), so there is nothing to
    do.
- **Local path installs:** the `./dist-public/` and workspace-source package paths continue
  to work as `pnpm add <path>` (pnpm supports local folder deps via path specifiers).
- **NG-build completion detection:** keep the `Date:`/`Hash:`/`Time:` output sniff
  unchanged. That line comes from the **Angular CLI's** build output, and the build is driven
  by the same bundled Node in both modes, so it is identical.

`IRunNPMCommand` stays the interface; no signature changes. `RunNPMCommand` is left untouched
other than removing `--legacy-peer-deps` handling at its call sites (§3.6).

### 3.6 Runner selection + removing `--legacy-peer-deps`

- `Activator.createNPMCommand(...)` is the single switch point (§3.4). Callers
  (`WebPackagesListener`, `NodeFolderCreatorJob`, `NGClientStarter`) are unchanged in how
  they obtain a runner.
- Remove `--legacy-peer-deps` from **all** call sites, for both runtimes:
  - `WebPackagesListener.java:731` (drop `command.add("--legacy-peer-deps")`)
  - `WebPackagesListener.java:752` (`ci` list)
  - `WebPackagesListener.java:771` (`update` list)
  - `NodeFolderCreatorJob.java:250` (`install` list)
  Since the flag is npm-specific and now removed, `RunPNPMCommand` never sees it; the npm
  path also stops passing it.

### 3.7 Bypassing the custom dedup in pnpm mode + pnpm store/linker

- Guard the `dedup` call and the hand-written dedup block
  (`WebPackagesListener.java:788-864`) with `!Activator.isPnpmMode()`, so in pnpm mode
  neither the `dedup` verb nor the manual folder-deletion pass runs. In npm mode both run
  exactly as today. (Belt-and-braces: `RunPNPMCommand` also treats a `dedup` verb as a
  no-op, so a stray call cannot break the build.)
- pnpm already uses a global content-addressable store with hardlinks, so packages are
  hardlinked from the store rather than copied — no per-solution duplication of bytes on
  disk regardless of how many `node_modules` trees exist. This makes the historical
  "shared root `node_modules` across all solutions" optimisation (an npm-era space saver)
  unnecessary in pnpm mode; see §3.7.1 for the resulting install topology.

#### 3.7.1 Solution `node_modules` topology: tarball installs + per-solution hoisted linker (CRITICAL)

The naive assumption that the existing directory-based installs (`install ./packages/<x>`,
`install ./projects/<x>`, `install ./dist-public/`, resolving to `link:`/`file:` directory
specifiers) would "just work" under pnpm is **wrong**, and this is the core install-topology
requirement of pnpm mode. Two independent facts, both reproduced against the shipped pnpm
(bundled `pnpm.exe`, pnpm v12.5.1) driving the real generated `target/Test` solution:

1. **pnpm does not install the transitive dependencies of a directory dependency.** Per
   pnpm's own docs ("Unlike npm, pnpm does not perform installation for the file
   dependencies") and confirmed empirically: a `link:../foo` or `file:../foo` *directory*
   dependency is symlinked, but pnpm never resolves/installs `foo`'s own `dependencies`. npm
   historically *did* install them, which is why the current directory-install flow works
   under npm and breaks under pnpm. The generated build then fails with e.g.
   `TS2307: Cannot find module '@ng-bootstrap/ng-bootstrap'`,
   `Cannot find module '@eonasdan/tempus-dominus'`, `@angular/cdk`, `bootstrap`,
   `@tinymce/tinymce-angular`, plus the `NG1010: 'imports' must be an array …` cascade —
   because `@servoy/servoydefault`, `@servoy/bootstrapcomponents`, `@servoy/nggrids`,
   `@servoy/servoyextracomponents` etc. declare those as their own `dependencies` and none of
   them are installed.

   **Fix:** install every Servoy web package as a real **tarball** (`.tgz`) instead of as a
   directory. A tarball dependency (`file:.../<pkg>.tgz`) is treated by pnpm (and by npm) as a
   real package install: pnpm reads the tarball's manifest and resolves + installs its
   transitive `dependencies`. Verified: after packing `@servoy/servoydefault` and installing
   the tarball, `@ng-bootstrap/ng-bootstrap`, `@eonasdan/tempus-dominus`, `@angular/cdk`,
   `bootstrap`, `@tinymce/tinymce-angular` are all present and resolvable.

1b. **The solution folder must carry the FULL core `package.json`, not the empty
   `package_solution.json`.** This is the second half of the same root-cause and the biggest
   divergence from npm mode. Under npm, the historical design put the full core dependency set
   (Angular, ag-grid, the `@angular/cli`/`@angular/build` toolchain, eslint, zone.js,
   lodash-es, ...) in the **shared root** `package.json` and gave each solution an *empty*
   `package_solution.json`; the solution build then resolved everything by npm's parent
   `node_modules` lookup (both for module resolution and for the `.bin/ng` script PATH). That
   parent-lookup is a **flat-tree npm behaviour that pnpm does not have** — pnpm resolves only
   the current project's own `node_modules`/`.pnpm` and its `.bin`, never a parent's. Verified
   empirically: a solution-folder `pnpm run <script>` that calls a bare `ng`/`json5` does not
   find the parent root's `.bin`, and core libraries the build imports directly
   (`ag-grid-community` — itself only a transitive of the root's `ag-grid-angular`/
   `ag-grid-enterprise` — `zone.js`, `lodash-es`, `@fortawesome`, the whole
   `@angular/cli`+`@angular/build` toolchain) are absent from the solution's `node_modules`,
   so the build fails with `'ng' is not recognized` and `Could not resolve
   "ag-grid-community/styles/ag-grid.css"`.

   **Fix:** in pnpm mode the solution folder's `package.json` is the **full core
   `package.json`** (the same content npm puts at the root), extended with the web-package
   tarball refs, and `@servoy/public` provided via its tarball rather than `file:dist-public`.
   The empty `package_solution.json` is an npm-only artefact and is not used in pnpm mode. The
   solution then installs its complete toolchain + libraries locally.

   **Why this is acceptable (no per-solution bloat):** the whole reason npm used the empty-
   solution + shared-root split was to avoid re-installing hundreds of identical packages per
   solution. pnpm removes that concern: its global content-addressable store hardlinks every
   package, so N solutions each with a full `node_modules` cost the bytes **once** on disk, and
   a solution install reuses ~all packages straight from the store (observed: ~545 packages
   "reused", install done in seconds). So each solution being a full standalone project is
   cheap under pnpm, exactly the trade-off the store was designed for.

   **Consequence for the shared root:** in pnpm mode the shared `target/` root is no longer an
   install target. `NodeFolderCreatorJob`'s root-level `uninstall @servoy/public` + `install`
   (§2.2) is an npm-era step that seeds the shared root; it is **skipped in pnpm mode** (it also
   currently fails there, since `@servoy/public` is not a root dependency). npm mode keeps the
   root install exactly as today.

2. **Hoisting must be per-solution, never at the shared root.** The target layout is
   `…/com.servoy.eclipse.ngclient.ui/target/` (shared root: core Angular / ag-grid / eslint
   modules, one per developer install) with one subdirectory **per solution**
   (`target/Test/`, `target/Example/`, …). Solutions share the root core modules but are
   otherwise independent and may legitimately carry *different* web-package sets or versions
   (e.g. a different bootstrap-components package). Therefore each solution's web packages and
   their transitive deps **must** land in that solution's own `node_modules`, isolated from
   sibling solutions. Applying `nodeLinker: hoisted` / `shamefullyHoist` at the shared
   `target/` root is explicitly wrong: it flattens solution-specific libraries into the shared
   root where a second solution would overwrite them.

   **Fix:** the solution directory becomes a **standalone pnpm project with its own flat
   `node_modules`**, not a pnpm workspace member of the root. Concretely:
   - The solution dir carries its own `package.json` whose `dependencies` are the core module
     set **plus** the web-package tarball refs (the core `package.json` content — the same one
     the root uses — extended with the `@servoy/*` tarballs).
   - A `pnpm-workspace.yaml` **inside the solution dir** sets `nodeLinker: hoisted` +
     `shamefullyHoist: true` + `strictDepBuilds: false`, producing an npm-equivalent flat
     `node_modules` that contains the web packages **and** their transitive deps
     (`@ng-bootstrap`, `@eonasdan/tempus-dominus`, `@angular/cdk`, `bootstrap`, uppy, the
     ag-grid CSS, etc.). This flat shape is required because generated `angular.json` `styles`
     and `src/styles.css` reference libraries by bare/`./node_modules/...` relative paths
     (e.g. `ag-grid-community/styles/ag-grid.css`,
     `./node_modules/@eonasdan/tempus-dominus/dist/css/tempus-dominus.css`) that an isolated
     store layout does not expose at the top level. See §3a for the deeper reason (the Servoy
     `~`-csslibrary contract + single-shared-version requirement) and why this must not be
     narrowed to `public-hoist-pattern` or the isolated default.
   - The solution-local `pnpm-workspace.yaml` makes pnpm treat the **solution folder** as its
     own workspace root (pnpm resolves the *nearest* `pnpm-workspace.yaml` walking up from the
     cwd). Verified against the shipped pnpm: with a `pnpm-workspace.yaml` in both the parent
     and the solution dir, an install run from the solution dir uses the solution's own file,
     writes its own `pnpm-lock.yaml` + `node_modules`, and does **not** become a member of the
     parent workspace. So the shared `target/` root may keep its own workspace file (it is only
     used by the root's own core install); the solution stays standalone regardless. The root
     remains a plain `pnpm install` of the core `package.json`.
   - This `pnpm-workspace.yaml` is **not generated at runtime**: it is a static source file
     shipped at `com.servoy.eclipse.ngclient.ui/node/pnpm-workspace.yaml`, copied into the
     solution folder together with the rest of the node sources by
     `NodeFolderCreatorJob.copyAllEntries("/node", ...)`. That guarantees it is present before
     the very first pnpm command runs in the solution folder (including the first
     `build_lib_debug_nowatch`, which triggers an implicit install), so `strictDepBuilds: false`
     and the hoisted linker apply from the start. npm mode ignores the file entirely (verified:
     `npm install` with a `pnpm-workspace.yaml` next to `package.json` is unaffected), so
     shipping it in the shared node sources is harmless for the npm path.

3a. **Why `shamefullyHoist: true` (a flat `node_modules`) is the correct choice here, not a
   compromise.** It is tempting to "improve" this to pnpm's default isolated linker or a
   narrower `public-hoist-pattern`. Do **not** — the flat layout is required by, and is the
   safest match for, Servoy's long-standing component CSS-library mechanism:
   - Servoy web-component `.spec` files declare CSS libraries with a `~`-prefixed path, e.g.
     the `calendar` component in `bootstrapcomponents` and the `groupingtable` component in
     `aggridcomponents` both declare
     `ng2Config.dependencies.csslibrary = ["~@eonasdan/tempus-dominus/dist/css/tempus-dominus.css;priority=5"]`.
     `WebPackagesListener` rewrites the leading `~` to `./node_modules/` and writes it into the
     solution's `angular.json` `styles` array as
     `./node_modules/@eonasdan/tempus-dominus/dist/css/tempus-dominus.css`. The `~` convention
     is therefore a **flat-`node_modules` contract**: every CSS-providing package must be
     reachable top-level at `./node_modules/<pkg>/...`. Only a hoisted/flat layout satisfies
     that for packages that are merely *transitive* (tempus-dominus comes in via
     `@servoy/bootstrapcomponents` / `@servoy/nggrids` / aggrid, never as a direct solution
     dependency).
   - It also enforces a **single shared version** of such a library across all components. If
     two components allow slightly different ranges (`<6.11` vs `<6.11.0`), pnpm's *isolated*
     layout could place two physical tempus-dominus copies in one web app — exactly the
     "two different versions of the same package in one web app" situation that is confusing and
     that many packages do not support. A flat top-level `node_modules` guarantees one shared
     copy, matching the single `./node_modules/@eonasdan/tempus-dominus/...` styles entry.
   - A `public-hoist-pattern` alternative would require maintaining an allow-list that mirrors
     every `~`-csslibrary across every component `.spec` (and every future one), so it is
     strictly more fragile with no benefit. The usual objections to a flat tree (phantom deps in
     source, version conflicts) do not apply: this is a generated, disposable per-solution build
     directory, not maintained source, and per-solution isolation already prevents cross-solution
     bleed. `shamefullyHoist: true` is the intended, robust choice.

3. **`tsconfig.json` `paths` for the four source libraries stay.** The four default source
   libraries (`@servoy/dialogs`, `@servoy/ngclientutils`, `@servoy/window`,
   `@servoy/servoydefault`) keep their `compilerOptions.paths` entries pointing at
   `projects/<lib>/src/public-api` so the Angular compiler resolves them from source with the
   correct module resolution. The tarball install of these still provides their *transitive*
   deps to `node_modules`; the `paths` entry only redirects the library's own entry point to
   source. Removing these `paths` makes the compiler pick up the tarball's bundled `src`
   without proper resolution and fails with `Could not resolve "@servoy/servoydefault"` /
   `NG2012`.

**Reproduced end-to-end.** With (a) all nine web packages installed as tarballs into the
solution dir, (b) a solution-local `pnpm-workspace.yaml` with `nodeLinker: hoisted` +
`shamefullyHoist: true`, and (c) the four source-lib `tsconfig` `paths` retained,
`pnpm run build_debug_nowatch` completes successfully (7.43 MB initial bundle) against the
real `target/Test` solution. Any one of the three missing reproduces a build failure.

**Store/linker config scope.** Keep pnpm config minimal and confined to store/linker
settings. `strictDepBuilds: false` is retained (matches the existing ignored-build-scripts
handling, §3.5's `--config.strictDepBuilds=false`). No other pnpm config is introduced. The
root install keeps pnpm's default (isolated) linker; only the **solution** dir is hoisted.

#### 3.7.2 Content-addressable store & cross-drive hardlinks (store-dir on a foreign volume)

How the installed files physically land on disk, and the one case that needs handling:

- **Per-file hardlinks, not per-package.** pnpm's global store is *content-addressable*: it
  stores each **file** under the hash of its content (`<store>/v11/files/<hash-shard>/<hash>`),
  not each package as a unit. Every file in a solution's `node_modules` is a **hardlink** to
  the single stored blob (`fsutil hardlink list <file>` shows the store path plus every install
  that shares it). This is deliberately *more* space-efficient than per-package linking:
  identical files across different packages/versions (licenses, unchanged `.d.ts`, unchanged
  utilities) share one blob, and a version bump only stores the changed files. The cost is that
  a "link the whole package dir in one operation" shortcut does not exist for the flat/hoisted
  layout we use — that single-operation form only exists as a directory *junction* (pnpm's
  isolated linker), which §3a rules out for the CSS-library contract. On the shipped setup this
  is fine: creating hardlinks on NTFS is cheap, and a warm store installs in seconds
  (`~545 packages reused`).
- **Hardlinks cannot cross volumes — the real constraint.** A Windows hardlink is a second
  directory entry for the same MFT record *within one volume*; you cannot hardlink a file on
  `D:` to a blob in a store on `C:`. When the store and the install target are on different
  drives, pnpm **falls back to copying** every file into the target (correct, but slower and it
  loses the dedup benefit). The bundled store default is the user home
  (`C:\Users\<user>\AppData\Local\pnpm\store\v11`), i.e. always on `C:`. The Eclipse
  plugin-state-location that holds both the bundled pnpm/node and the `target/` solution
  folders is on the **workspace volume**. So:
  - When the workspace is on `C:` (the vast majority of installs), store and target share the
    `C:` volume, hardlinks work, and **nothing needs to be done** — rely on the default.
  - When the workspace is on another drive (e.g. `D:\servoy_workspaces\workspace1`), the default
    `C:` store is on a foreign volume and pnpm would copy instead of hardlink. In that case set
    a **`store-dir` on the target's own volume**.
- **The store must be a generic per-drive location shared across workspaces, not inside a
  workspace.** Multiple workspaces can live on the same drive
  (`D:\workspace`, `D:\servoy_workspaces\workspace1`, `D:\servoy_workspaces\workspace2`); they
  should all share **one** store on `D:` so dedup works across them. Putting the store inside a
  workspace's own dir would both fragment the store per workspace (killing cross-workspace
  dedup) and risk being wiped when that workspace's target is cleaned. So the store-dir must be
  a stable, generic path at the **root of the target's drive** (e.g. `<drive>:\.pnpm-store` or a
  similar fixed per-drive location), independent of any workspace.
- **Detection & behaviour (design):** in pnpm mode, compare the drive/volume of the install
  target against the drive of pnpm's default store. If they match, do nothing (default store).
  If they differ, pass a `store-dir` pointing at the generic per-drive store on the target's
  volume (via a `--config.store-dir=<path>` / `--store-dir` argument on the pnpm invocations, or
  the `storeDir` setting — note `storeDir` is one of the settings pnpm still honours from config
  even though workspace-root-only settings are restricted). `store-dir` is NOT put in the
  shipped `pnpm-workspace.yaml`, because the correct value is machine/drive-dependent and must
  be computed at runtime from where the workspace actually lives.

**Status:** the drive-mismatch `store-dir` handling is a known follow-up within this case; the
`C:`-workspace happy path (the common case) already works with the default store and no extra
config. See the open-questions table.

### 3.8 What stays identical

- Node.js + npm remain the default and are fully functional with the property unset.
- `RunNPMCommand` and the custom dedup path are retained — only *bypassed* in pnpm mode. The
  bundled Node is still required in pnpm mode (pnpm runs on it and drives the Angular CLI
  build through it).
- The Angular CLI build step is identical in both modes (same bundled Node); only the
  package-install/update step differs (pnpm vs npm).
- `NoOpNPMCommand` remains the fallback for both runtimes when no binary is present
  (important for tests, where `nodeExtractionAndTitaniumBuildDisabled` is true).

## 4. Implementation plan

1. **Bundle pnpm into each Node plugin** — in each of the five
   `com.servoy.eclipse.nodejs.*` plugins:
   - Add a second `download-maven-plugin` execution to `pom.xml` fetching the correct
     standalone pnpm executable for that platform into `pnpm.zip`.
   - Add `pnpm.zip` to `build.properties` (and to `.gitignore`).
   - Update the `<nodejs>` element in `plugin.xml` with `pnpmPath` (and `pnpmArchive` if the
     pnpm zip is kept separate from `node.zip`).

2. **Extend the `nodejs` extension point** — add optional `pnpmPath` (and optional
   `pnpmArchive`) attributes to `com.servoy.eclipse.ngclient.ui/schema/nodejs.exsd`.

3. **`Activator`** —
   - Add `isPnpmMode()` reading `servoy.jsRuntime`, and honour a new optional
     `servoy.pnpmPath` override.
   - Add a `pnpmPath` field; in `extractNode()` extract pnpm into the same state-location dir
     as Node (via `extractPath`), mark it executable.
   - In `createNPMCommand(...)`, return `RunPNPMCommand` in pnpm mode (given `pnpmPath` +
     `nodePath`/`npmPath`), else `RunNPMCommand`; `NoOpNPMCommand` when binaries are missing.

4. **`RunPNPMCommand`** — new `IRunNPMCommand` implementation based on `RunNPMCommand`,
   building `pnpm <args>` command lines with the bundled Node dir on `PATH`, translating npm
   verbs to pnpm verbs (`install`/`add`, `ci`→`install --frozen-lockfile`, `update`,
   `remove`, `run`), treating `dedup` as a no-op, keeping console/cancel/exit-code behaviour.

5. **Remove `--legacy-peer-deps`** from all four call sites
   (`WebPackagesListener.java:731,752,771`, `NodeFolderCreatorJob.java:250`).

6. **Bypass custom dedup in pnpm mode** — guard `WebPackagesListener.java:788-864` with
   `!Activator.isPnpmMode()`.

7. **Solution install topology in pnpm mode (§3.7.1)** — in `WebPackagesListener` (and
   `checkPackage`), when `Activator.isPnpmMode()`:
   - Pack each Servoy web package (the default source projects, the workspace `DirPackageReader`
     source packages, the `ZipPackageReader` packages, and `@servoy/public`/`dist-public`) into
     a `.tgz` via the bundled `pnpm pack`, and install those tarball paths (`pnpm add
     <pkg.tgz> …`) instead of the directory paths. Update the "already installed" detection so
     a package is considered installed when the recorded dependency specifier matches its
     current tarball (so packing/installing does not re-run every cycle).
   - Ensure the solution dir is a **standalone** pnpm project: it has its own `package.json`
     (core deps + tarball refs) and a solution-local `pnpm-workspace.yaml` with
     `nodeLinker: hoisted`, `shamefullyHoist: true`, `strictDepBuilds: false`. Ensure **no**
     `pnpm-workspace.yaml` exists at the shared `target/` root.
   - Keep the four source-lib `tsconfig` `paths` entries (§3.7.1 point 3).
   In npm mode this whole step is skipped; the existing directory-install flow is unchanged.

8. **Build/validate** — `mvn clean verify`, then validate a pnpm-driven TiNG build against a
   real solution on at least one platform: pack the web packages and `pnpm add <tarballs>` for
   the package steps, then `pnpm run build_debug_nowatch` (Angular CLI via the bundled Node),
   confirming pnpm resolves the bundled Node from the extraction dir and that the flat
   solution `node_modules` contains the web packages' transitive deps; confirm npm mode is
   unchanged with the property unset.

## 5. Acceptance criteria

- [ ] With `servoy.jsRuntime` unset (or `npm`/`node`), the build uses npm and behaves exactly
      as before (same command sequence, custom dedup still runs), except that
      `--legacy-peer-deps` is no longer passed.
- [ ] pnpm is bundled inside each of the five `com.servoy.eclipse.nodejs.*` plugins (not as
      separate plugins), fetched at build time, and extracted into the same state-location
      directory as the bundled Node.
- [ ] With `servoy.jsRuntime=pnpm`, package verbs (`install`/`add`, `ci`, `update`,
      `uninstall`) run through the bundled pnpm binary, and `pnpm run <script>` runs the
      Angular build using the **bundled** Node (never a system Node); all complete with exit
      code 0 on a real solution.
- [ ] The extracted `pnpm` and `node` binaries locate each other from the extraction dir via
      `PATH` — no globally installed Node is assumed.
- [ ] In pnpm mode, neither the `dedup` call nor the hand-written custom dedup pass in
      `WebPackagesListener` runs, and pnpm's store yields no duplicated per-solution
      `node_modules` bytes on disk (hardlinked from the global store).
- [ ] In pnpm mode, every Servoy web package is installed into the solution dir as a
      `.tgz` tarball (not a directory dependency), so each package's transitive
      dependencies (`@ng-bootstrap/ng-bootstrap`, `@eonasdan/tempus-dominus`, `@angular/cdk`,
      `bootstrap`, `@tinymce/tinymce-angular`, uppy, ag-grid CSS, …) are resolved and present
      in the solution's `node_modules`.
- [ ] In pnpm mode, the solution dir is a standalone pnpm project with a solution-local
      `pnpm-workspace.yaml` (`nodeLinker: hoisted`, `shamefullyHoist: true`,
      `strictDepBuilds: false`) producing a flat `node_modules`; there is no
      `pnpm-workspace.yaml` at the shared `target/` root, so sibling solutions never overwrite
      each other's web packages and each keeps its own (possibly different) package set.
- [ ] The four source-lib `tsconfig` `paths` entries (`@servoy/dialogs`,
      `@servoy/ngclientutils`, `@servoy/window`, `@servoy/servoydefault`) remain, pointing at
      `projects/<lib>/src/public-api`.
- [ ] `--legacy-peer-deps` is removed from every runner call site (npm and pnpm).
- [ ] The `nodejs` extension point carries an optional `pnpmPath` (npm mode unaffected when
      absent).
- [ ] A full `mvn clean verify` succeeds; the pnpm runtime is a committed, shippable artifact
      (not a spike report), covering win x64/arm64, macOS x64/arm64, linux x64.
- [ ] Both runtimes remain selectable and functional side by side.

## 6. Out of scope

- Removing npm-as-default or the bundled Node — the Node plugins stay; pnpm runs on top of
  the bundled Node.
- Making pnpm the default runtime.
- Realigning component-package peer dependencies as new work — assumed already in place /
  validated when the pnpm build is run.
- WAR / solution export: not affected. Export tooling only consumes the production `dist`
  build artifacts that the Angular build outputs — it never reads `node_modules` — so pnpm's
  store/link layout is irrelevant to it.
- Bun (evaluated and rejected in §2.5).
- Benchmarking pnpm vs npm (may be captured informally but is not a required deliverable).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Confirm the exact standalone-binary asset name/URL for the latest pnpm release on each of the five platforms (win x64/arm64, macOS x64/arm64, linux x64). | dev | open |
| Packaging choice: drop `pnpm` inside the extracted Node dir vs a sibling dir with both on PATH — pick whichever the pnpm archive makes cleanest. | dev | open |
| Whether any minimal pnpm store/linker config (`node-linker`, `store-dir`) is needed, or the defaults (linked, flattened `node_modules`) suffice. | dev | **resolved** — solution dir needs `nodeLinker: hoisted` + `shamefullyHoist: true` in a solution-local `pnpm-workspace.yaml`; that file makes pnpm treat the solution as its own workspace root (nearest `pnpm-workspace.yaml` wins), so the shared root's own workspace file (if any) does not need deleting and the root keeps its defaults. See §3.7.1. |
| Confirm pnpm's `add <localPath>` handles the `./dist-public/` and workspace-source package installs the same way npm's `install <path>` does. | dev | **resolved — NO.** pnpm does *not* install a directory dependency's transitive deps (npm did). Web packages must be installed as `.tgz` tarballs instead. See §3.7.1. |
| Cross-drive store: when the workspace is on a non-`C:` drive, the default `C:` store is on a foreign volume so pnpm copies instead of hardlinking. Implement runtime detection: if the target's drive differs from the default store's drive, pass a `store-dir` pointing at a generic per-drive store (e.g. `<drive>:\.pnpm-store`) shared across all workspaces on that drive. `C:`-workspace happy path already works with the default store (no config). See §3.7.2. | dev | open |
