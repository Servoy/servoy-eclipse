# Spec: SVY-21502 — Pre-build `@servoy/public` at Maven time; stop packing the internal component tarballs

> **Status:** proposal / draft — continuation of SVY-21502 (still in progress).
> SVY-21456 shipped pnpm; SVY-21502 added the per-solution version-scoped build-script
> approvals + the committed `package.json` + `pnpm-lock.yaml` pair. This change finishes that
> work: it fixes the lockfile-seed-before-public-pack ordering bug (§2.2) and removes the wasteful
> per-solution `ng build @servoy/public` + internal `pnpm pack` by (a) pre-building `@servoy/public`
> once at the `com.servoy.eclipse.ngclient.ui` Maven build and shipping it, and (b) treating the four
> internal component packages as the SOURCE packages they already are (resolved by `tsconfig` `paths`),
> so they are not packed/installed as tarballs at all. External component packages are OUT of scope (§4.5).

## 1. Goal

In pnpm mode, stop the per-solution Angular library build and component packing. There are two
kinds of internal `@servoy/*` package (source under `com.servoy.eclipse.ngclient.ui/node/`), and
they are handled differently — this is the key correction from the first draft (see §2.4):

- **`@servoy/public`** — a real **compiled** ng-packagr library, resolved from `node_modules`
  (no `paths` alias). Pre-build it ONCE at the `com.servoy.eclipse.ngclient.ui` Maven build, ship
  the `.tgz` in `node/tarballs/`, and at solution-build time just **copy** it into
  `target/<solution>/.tarballs/` and install from it — no per-solution `ng build @servoy/public`.
- **`@servoy/servoydefault`, `@servoy/dialogs`, `@servoy/window`, `@servoy/ngclientutils`** —
  **source packages**: the solution's `tsconfig.json` aliases them (`paths`) to their live
  `projects/<name>/src/public-api`, so the Angular compiler reads the component TS/templates
  straight from the file-watched source (keeping `build_watch` live editing working). Their tarball
  `src/**` is never read. So **do not pack or install a tarball for them** — only ensure their
  `package.json` `dependencies` are present (§4.1). This means editing e.g.
  `projects/servoydefault/.../textfield.ts` keeps working via the alias. Their component-scoped CSS
  (`styleUrls: ['./x.css']`) is inlined by the Angular compiler from that same aliased source, so it
  is live too. Their one gap is the package-level **global** CSS (declared in the MANIFEST as
  `NG2-CSS-ClientLibs`, e.g. `servoy-menu.css`, `defaultcomponents.css`) which goes into the
  `angular.json` global `styles` array as a filesystem path — §4.3 rewrites that path to the live
  `projects/<name>/` source so it resolves (and is live) without the package being in `node_modules`.

The result: a given shipped Servoy has one fixed `@servoy/public` tarball (a customer never
rebuilds it), the four component packages are compiled from their aliased source (TS, templates,
component CSS and global CSS all live), and the `ERR_PNPM_TARBALL_READ_LOCAL_TARBALL` ordering bug
is gone because the public tarball exists on disk before any install.

## 2. Problem this solves

### 2.1 Wasted work on every build
Today `WebPackagesListener` packs each `@servoy/*` package into `.tarballs/` on every solution
build (`packPackage` → bundled `pnpm pack`), and separately runs `build_lib_debug_nowatch`
(`ng build @servoy/public`) to produce `dist-public` before packing public. For the internal
packages this is pure overhead on a customer machine: the shipped sources never change between
builds, so the output tarball is always byte-identical. It still runs an Angular library build and
several `pnpm pack` processes per solution, per workspace, repeatedly.

### 2.2 The ordering bug (observed: `ERR_PNPM_TARBALL_READ_LOCAL_TARBALL`)
SVY-21502 seeds a committed `pnpm-lock.yaml` (pinning every `@servoy/*` tarball by its file name)
into `target/<solution>/` **before** the install. But the `@servoy/public` tarball is only
produced *by* `build_lib_debug_nowatch`, which the current flow runs inside the same install
block. On a clean build the first pnpm install step (`run build_lib_debug_nowatch`) resolves
dependencies against the seeded lockfile, which already references
`.tarballs/servoy-public-<hash>.tgz` — a file that does not exist yet — and fails:

```
Error: ERR_PNPM_TARBALL_READ_LOCAL_TARBALL
Failed to read local tarball ...\.tarballs\servoy-public-1bf308b078ba.tgz:
The system cannot find the file specified. (os error 2)
```

Observed against a real install (`D:\installs\2026.12_nightly\...`): every other component was
packed (dialogs, ngclientutils, window, 12grid, nggrids, bootstrapcomponents, servoydefault,
servoyextracomponents), but `@servoy/public` was never packed before the lockfile-driven install
consumed it. Pre-building the internal tarballs up front removes this ordering hazard: the
lockfile's `file:` specifier for `@servoy/public` resolves against a tarball that already exists
on disk before any install runs.

### 2.3 Lockfile portability already assumes determinism
SVY-21502 deliberately made the tarball name stable per content so a fresh CI checkout of
byte-identical content yields the same name and keeps a committed lockfile valid (`contentHash` in
`WebPackagesListener`). Pre-building formalizes that for the internal set: the tarballs become a
shipped, versioned artifact rather than something re-derived on each machine.

### 2.4 The component packages are resolved via tsconfig `paths`, NOT from the tarball
Crucial finding (verified against a real solution build's generated `tsconfig.json`): the solution
build writes **`paths` aliases** for every component package that points the bare specifier at the
LIVE SOURCE, not at `node_modules`:

```
"paths": {
  "@servoy/servoydefault":        [".../projects/servoydefault/src/public-api"],
  "@servoy/dialogs":              [".../projects/dialogs/src/public-api"],
  "@servoy/window":               [".../projects/window/src/public-api"],
  "@servoy/ngclientutils":        [".../projects/ngclientutils/src/public-api"],
  "@servoy/12grid":               [".../packages/@servoy/12grid/src/public-api"],
  "@servoy/bootstrapcomponents":  [".../packages/@servoy/bootstrapcomponents/src/public-api"],
  "@servoy/nggrids":              [".../packages/@servoy/nggrids/src/public-api"],
  "@servoy/servoyextracomponents":[".../packages/@servoy/servoyextracomponents/src/public-api"]
}
```

`@servoy/public` is the ONLY package with no `paths` entry — it resolves from `node_modules` (its
real compiled tarball). `checkPackage` adds a `paths` entry for every default (internal) package
(line ~1674: `src/public-api`) and every `DirPackageReader` source project (line ~1827: the
`.sourcepath` `apiFile`).

**Consequence:** for all the component packages the Angular compiler reads the component code
through the `paths` alias → the live `projects/`/`packages/` source. The copy the file watcher
makes (`node/projects/servoydefault/...` → solution `projects/servoydefault/...`) is exactly what
the compiler reads, so **`build_watch` live editing keeps working.** The installed tarball's
`src/**` in `node_modules/@servoy/servoydefault/` is NEVER read for the component code — it is dead
weight. The tarball's ONLY real job for a component package is to let pnpm pull that library's own
**`dependencies`** (`@ng-bootstrap`, `@angular/cdk`, `tinymce`, `@popperjs/core`, `tslib`, …) into
the solution `node_modules`.

**Conclusion (revises the earlier draft):** a component package is a **source package** — resolved
by `paths`, not by its tarball. So for the internal components we should NOT ship/copy/pack a source
tarball at all; we only need their `dependencies` installed. `@servoy/public` is different: it has
no `paths` alias and is consumed as a real compiled library, so it DOES ship as a pre-built tarball.
This is what §4 implements.

## 3. Current behavior (for reference)

- **Maven** (`com.servoy.eclipse.ngclient.ui/pom.xml`, `frontend-maven-plugin`): at `initialize`
  it installs node, runs `npm install` (+ per-project `--omit=peer` installs for servoydefault /
  dialogs / window) and runs `npm run build_lib` (`ng build @servoy/public`). The
  `node/package.json` `pack_libs` script already does `npm pack` for `dist-public` / `dist-sablo` /
  `dist-designer` and renames the output to stable `servoy-public.tgz` etc. (used by the RFB
  designer's `file:` tgz references — a precedent for shipping built tarballs).
- **Runtime** (`WebPackagesListener`, pnpm mode):
  - `NodeFolderCreatorJob` removes `"@servoy/public": "file:dist-public"` from the solution
    `package.json` ("added later as a tarball").
  - For each component dir, `resolveInstallSpecifier` → `packPackage` → bundled `pnpm pack`
    into `.tarballs/`, named `<name>-<contentHash>.tgz`.
  - `build_lib_debug_nowatch` builds `dist-public`, then `packPackage(dist-public, @servoy/public)`.
  - `PnpmBuildScriptConfig.seedLockfileFromCommitted` seeds the committed `package.json` +
    `pnpm-lock.yaml` pair before installing.
  - Developer path installs unfrozen and commits the lockfile back; WAR/CI path trims non-exported
    components and installs `--frozen-lockfile`.
- **From-source detection:** `NodeFolderCreatorJob.createFileWatcher` and `WebPackagesListener`
  both detect running from source via `bundle.getLocation().indexOf("file:/")`. `NodeFolderCreatorJob`
  copies the plugin's `/node` bundle entries into the solution target, deliberately skipping
  `dist/`, `dist-public/`, `node_modules/` (see `ignoredResource`).

## 4. Proposed design

The model has two distinct kinds of internal package, treated differently:

### 4.1 The four component packages → **source packages** (deps-only install, no tarball)
`@servoy/servoydefault`, `@servoy/dialogs`, `@servoy/window`, `@servoy/ngclientutils` are already
resolved by the solution's `tsconfig.json` `paths` alias to their live `projects/<name>/src/public-api`
(see §2.4). The Angular compiler reads the component code through that alias — the file watcher's
copy of the source into `projects/` is what gets compiled, so `build_watch` live editing works. The
tarball's `src/**` is never read; its only purpose was to pull the library's own `dependencies`.

Therefore, in pnpm mode, DO NOT pack or install a tarball for these four. Instead:
- Keep writing the `paths` alias (unchanged `checkPackage` behaviour).
- Read each package's `package.json` `dependencies` and add them to the solution's pnpm install
  set, so pnpm pulls those transitive deps (`@ng-bootstrap`, `@angular/cdk`, `@popperjs/core`,
  `tslib`, …) into `node_modules`. The package itself is NOT added as a `file:` dependency.

> First-implementation simplification (per decision): do NOT build a dedicated deps-only install
> step yet. The four internal components' declared deps (`tslib`, `@angular/cdk`, `@popperjs/core`;
> ng-bootstrap/tinymce etc. come via the external component packages) are already present in the
> solution `node_modules` via the root `package.json` + the external packages under
> `shamefullyHoist`. So the initial change simply **stops packing/installing the internal component
> tarballs** and relies on the alias + already-satisfied deps. Verify with a from-source build that
> nothing is missing; add an explicit deps-only install only if a real gap appears.

This applies in BOTH from-source and binary/customer modes — a component package is a source
package by nature (its source is always copied into `projects/`), so there is no mode split for
these four.

#### 4.1.1 Global CSS of the component packages — rewrite to the source path
The four component packages have no TypeScript `import './x.css'` (verified: component styles use
Angular `styleUrls: ['./x.css']`, which the compiler inlines from the aliased source — already
live). The one thing NOT covered by the `paths` alias is each package's **package-level global CSS**
declared in the MANIFEST as `NG2-CSS-ClientLibs` with a `~`-prefixed URL, e.g.
`~@servoy/window/servoy-menu.css`, `~@servoy/servoydefault/defaultcomponents.css`. These go into the
`angular.json` global `styles` array, where the `~` historically expands to `./node_modules/`. With
the component no longer installed into `node_modules`, that path does not resolve and `ng build`
fails with `Could not resolve "./node_modules/@servoy/window/servoy-menu.css"`.

Fix: a `resolveCssLibUrl(CssLib)` helper rewrites the global style URL based on how the package is
resolved. For an INTERNAL default component in pnpm mode, `~@servoy/<name>/<rest>` is rewritten to
`./projects/<name>/<rest>` (the live, file-watched source); every other URL keeps the
`./node_modules/` mapping (external/binary packages, npm mode). This both fixes the build AND makes
the internal global CSS live (editing `servoy-menu.css` / `defaultcomponents.css` in source is
picked up by `ng build --watch` with no re-pack). Verified: the generated `angular.json` styles
array now shows `./projects/window/servoy-menu.css` and `./projects/servoydefault/defaultcomponents.css`
while all external packages keep `./node_modules/@servoy/<name>/...`.

### 4.2 `@servoy/public` → pre-built, shipped tarball
`@servoy/public` is the one internal package with NO `paths` alias; it is consumed as a real
compiled library from `node_modules`. It is the compiled `dist-public`, identical for a given
Servoy build. So it IS pre-built at the `com.servoy.eclipse.ngclient.ui` Maven build and shipped:

- A `node/package.json` `pack_public` script runs `build_lib` (`ng build @servoy/public`) then
  `npm pack`s `dist-public` into `com.servoy.eclipse.ngclient.ui/node/tarballs/`, named by version:
  `servoy-public-<version>.tgz`.
- A `frontend-maven-plugin` execution in `pom.xml` runs it after the existing `build_lib`.
- `node/tarballs/` is added to `build.properties` `bin.includes`, `node/.gitignore`, and
  `NodeFolderCreatorJob.ignoredResource`.

At solution-build time, in pnpm mode, `WebPackagesListener` **copies** the shipped
`node/tarballs/servoy-public-<version>.tgz` into `target/<solution>/.tarballs/` and installs
`@servoy/public` as `file:.tarballs/servoy-public-<version>.tgz` — skipping `build_lib_debug_nowatch`
entirely. When running from source the bundle is the `file:/` workspace, so the same bundle-entry
lookup reads the Maven-built tarball from the checkout — no mode split.

### 4.3 Naming — version, not content hash (resolves open question #4)
`@servoy/public`'s shipped tarball is named by package **version** (`servoy-public-<version>.tgz`),
not a content hash or timestamp: it is versioned in lockstep with the Servoy build, the name is
human-readable and obviously matched to the Servoy version, and a version is stable across CI
checkouts (no per-checkout rename). The content-hash scheme in `WebPackagesListener.contentHash`
still governs the **external** component packages (§4.5).

### 4.4 Developer GENERATES, WAR export CONSUMES (the seeding rule)
The Developer build — on ALL triggers: copy+build, clean+build, and the startup timestamp-check —
always GENERATES the dependency state: it does NOT seed the committed lockfile, it regenerates
`package.json` + `pnpm-lock.yaml` from the actual resolved set (non-frozen `add` + `update`), runs
`pnpm rebuild` for the allowed build scripts, and commits the fresh pair back to `ng_web_packages/`.
It uses the committed approvals JSON (so pnpm knows which build scripts are allowed) but never the
committed lockfile as input.

The WAR export always CONSUMES: it seeds the committed `package.json` + `pnpm-lock.yaml`, installs
`--frozen-lockfile` against them (building exactly what the developer last committed), trims the
not-exported components, and never commits back.

So the single axis is `warExportModel != null` (WAR = consume) vs `== null` (Developer = generate).
`cleanInstall` is unrelated to this: it only means "wipe the target and regenerate from scratch" —
it must NOT seed and must NOT run the frozen `ci`. (Two bugs fixed here that pre-dated this change
but were exposed by it: a clean install used to run the frozen `ci`, and a Developer run used to
seed — both wrong; both made the stale committed lockfile break `ci`/`rebuild` once the component
tarballs were gone.)

Because the four internal components are source packages (alias, no tarball) and `@servoy/public`
is read from the shipped tarball in both modes, there is **no from-source branch** for the package
resolution itself. A core developer editing `projects/servoydefault/.../textfield.ts` (or its CSS)
has it picked up through the `paths` alias / the §4.1.1 CSS rewrite; a core developer editing
`@servoy/public` source rebuilds the shipped tarball via the Maven `pack_public` (regenerate
`node/tarballs/`) — the live `build_watch` loop for public was already a `dist-public` rebuild,
which this change moves to the Maven step.

> Follow-up (web-package developer, e.g. `D:\git\bootstrapcomponents`): making an EXTERNAL web
> package run fully from source (consume the live source, skip the pack round-trip, and make its
> GLOBAL CSS live via the same `./packages/@servoy/<name>/` rewrite §4.1.1 does for internal) is a
> separate concern tracked in §7. This change does not alter external-package handling — editing an
> external package's global CSS in source still needs a copy+build to regenerate its tarball.

### 4.5 External component packages — OUT of scope (with a follow-up)
`@servoy/12grid`, `@servoy/nggrids`, `@servoy/bootstrapcomponents`, `@servoy/servoyextracomponents`
and other third-party/independent component packages are **completely independent**, released on
their own cycles, and can change at any time. They KEEP their current behavior: aliased via `paths`
AND packed to a content-hash tarball at solution-build time (so pnpm pulls their deps). This change
does not touch them.

**Follow-up case (§7):** change the way our web packages are zipped so the shipped artifact is
already a usable tarball rather than a tarball nested inside a zip (see
`D:\git\bootstrapcomponents\components\scripts\build.js` / the `make_release` script), so Servoy
could consume it directly with no re-pack.

### 4.6 Determinism / correctness guard
`@servoy/public`'s shipped tarball name (version) must match what the committed `pnpm-lock.yaml`
pins; a mismatch on the frozen WAR/CI path must fail loud (as today via the non-zero exit code),
not silently re-pack. The pre-build and the committed lockfile come from the same Servoy version so
the names stay matched. Because the internal component tarballs are gone, the committed pair no
longer lists `servoy-servoydefault-*`/`-dialogs-*`/`-window-*`/`-ngclientutils-*` entries at all —
it lists `@servoy/public` + the external component tarballs + the registry deps (including the
components' transitive deps). This pair shape is the same in from-source and binary modes, so
commit-back stays on the binary/Developer-customer path only (a from-source run must not overwrite
the committed, reproducible pair).

## 5. Impact / files (anticipated)

- `com.servoy.eclipse.ngclient.ui/pom.xml` — new `frontend-maven-plugin` npm execution that builds
  + packs **only `@servoy/public`** into `node/tarballs/`.
- `com.servoy.eclipse.ngclient.ui/node/package.json` — new `pack_public` (build + `npm pack`
  `dist-public`, version-named).
- `com.servoy.eclipse.ngclient.ui/build.properties` — ship `node/tarballs/`.
- `com.servoy.eclipse.ngclient.ui/node/.gitignore` — ignore the generated `node/tarballs/`.
- `NodeFolderCreatorJob.java` — add `node/tarballs/` to `ignoredResource`.
- `WebPackagesListener.java`:
  - copy the shipped `servoy-public-<version>.tgz` into `.tarballs/` (`copyShippedPublicTarball`,
    before the install block) and install `@servoy/public` from it; skip `build_lib_debug_nowatch`
    in pnpm mode (npm mode keeps it);
  - for the four internal component packages (`defaultPackages`), keep the `paths` alias but in pnpm
    mode return no install specifier from `checkPackage` (no tarball) — rely on the alias +
    already-present deps (§4.1 simplification);
  - `resolveCssLibUrl(CssLib)` rewrites the internal components' global-CSS style URLs to
    `./projects/<name>/...` instead of `./node_modules/...` (§4.1.1); external packages unchanged;
  - leave external-package packing (content-hash tarball) unchanged;
  - seed the committed pair ONLY for a WAR export (`warExportModel != null`); a Developer build (any
    trigger: copy+build, clean+build, startup timestamp-check) regenerates from scratch and commits
    the fresh pair back. `cleanInstall` no longer influences seeding or the frozen-vs-`update` branch
    — only a WAR export runs the frozen `ci` (§4.4 / §4.6).

## 6. Testing

- Unit: `@servoy/public` shipped-tarball name resolution (`servoy-public-<version>.tgz` from
  `node/package.json`-equivalent version) and the "is this package in the no-tarball internal
  component set" decision are pure logic — test via the existing reflection seam pattern used by
  `RunPNPMCommandTest` / `PnpmBuildScriptConfigTest`.
- Integration (manual, binary install, pnpm mode): a clean solution build runs no
  `ng build @servoy/public` and no internal `pnpm pack`; `.tarballs/` gets only the copied
  `servoy-public-<version>.tgz` (plus external component content-hash tarballs); the frozen install
  succeeds (no `ERR_PNPM_TARBALL_READ_LOCAL_TARBALL`); `@servoy/servoydefault` etc. compile via the
  `paths` alias.
- Integration (from source, core dev): editing `projects/servoydefault/.../textfield.ts` with
  `build_watch` running recompiles from the aliased source (no repack); editing an internal
  component's global CSS (`projects/window/servoy-menu.css`) is also live (§4.1.1). Editing
  `@servoy/public` requires regenerating the shipped tarball via the Maven `pack_public`.
- External packages unchanged: 12grid/nggrids/bootstrap/extra still alias + content-hash pack;
  editing their global CSS in source needs a copy+build (known limitation, §7 follow-up).
- WAR export (the CONSUME path, not exercisable from the dev run): seeds the committed pair,
  installs `--frozen-lockfile`, builds, and renders with styles; does not regenerate or commit back.

### 6.1 Verified against a real clean build (`D:\installs\2026.12_nightly\...test_webcomponents2`)
- `.tarballs/`: `servoy-public-<version>.tgz` (copied, not built) + the four external content-hash
  tarballs; NO internal component tarballs.
- `node_modules/@servoy/`: `public` present (installed from tarball) + external packages; the four
  internal components absent (resolved via `paths`).
- `tsconfig.json` `paths`: all 8 component packages aliased to live source; `@servoy/public` has no
  alias.
- `angular.json` styles: `./projects/window/servoy-menu.css` and
  `./projects/servoydefault/defaultcomponents.css` (internal → source), all others `./node_modules/`.
- Build log: `copied shipped @servoy/public tarball ... (no ng build)`, then `add` (public only) →
  `update` → `rebuild` → commit-back → `ng build ngclient2` completes. No
  `ERR_PNPM_TARBALL_READ_LOCAL_TARBALL`.

## 7. Open questions / follow-ups

1. ~~**Shipped location**~~ — resolved: `com.servoy.eclipse.ngclient.ui/node/tarballs/`.
2. ~~**Component packages from other repos**~~ — resolved: OUT of scope; they keep current behavior.
   Follow-up: make `make_release` emit a real `.tgz` instead of a zip-with-nested-tarball so Servoy
   can use the artifact directly (§4.5).
3. ~~**From-source trigger granularity**~~ — resolved: trigger on an actual source change (reuse
   existing change detection), scoped to the core-dev from-source path.
4. ~~**Naming: timestamp vs version**~~ — resolved: **version** for the internal set (§4.2); no
   timestamp, no content hash. Content hash stays for the external packages.
5. **Follow-up — external web-package "run from source":** an external component used as a source
   project (`DirPackageReader`) is aliased for TS (live) but still packed to a tarball, and its
   GLOBAL CSS still resolves from `./node_modules/` — so editing that CSS in source is not live
   until a copy+build re-packs it. Making an external source package fully live (skip the tarball
   like the internal ones, and rewrite its global CSS to `./packages/@servoy/<name>/` via the same
   `resolveCssLibUrl` mechanism) is a separate case. Needs its own ticket.
6. ~~**Confirm sablo/designer**~~ — resolved: the shipped internal set is **public, servoydefault,
   dialogs, window, ngclientutils**. `@servoy/sablo` / `@servoy/designer` are consumed only by the
   RFB designer via its own `file:` tgz references (see root `AGENTS.md` "Shared Angular Libraries")
   and are NOT installed into a solution's `.tarballs/`, so they are excluded from this set.
