# Spec: SVY-21502 — pnpm per-solution, version-scoped approval of dependency build scripts

## 1. Goal

Replace the SVY-21456 stopgap (which globally disables pnpm's `strictDepBuilds` and
therefore silently *skips* every dependency build script) with a real, auditable,
**version-scoped** per-solution allow/deny list of dependency build scripts, backed by a
**committed pnpm lockfile** so that CI / WAR builds install the *exact* same dependency
versions every time. Before a full build, a small bounded discovery query enumerates which
`name@version` packages pnpm wants to run build scripts for, and reports to the developer "you
need to decide on these packages to do a build". The developer allows or denies each one; those
decisions are persisted in a **git-committed file inside the solution's `ng_web_packages`
web-packages directory** (the same folder that holds the component/service package zips),
alongside a **committed `pnpm-lock.yaml`**. Both the Developer build and the **headless WAR
export CLI** translate the allowed list into pnpm's explicit versioned `allowBuilds` map so
genuinely-needed native build scripts (for `servoy-bootstrap-components`,
`servoy-extra-components`, `servoy-nggrids`, or a customer's own component/service package)
**run unattended** with no interactive pnpm TUI prompt and no `ERR_PNPM_IGNORED_BUILDS` failure.

The headline security property: because approval is keyed by exact `name@version`, a transitive
version bump to a version the developer never reviewed is automatically *pending* — the Developer
build re-asks for exactly that new version, and WAR/headless does **not** silently trust it. A
committed lockfile, installed frozen, means those versions cannot silently drift between CI runs
in the first place.

This is R&D on the still-opt-in, unreleased pnpm mode (`servoy.jsRuntime=pnpm`; npm remains
the default), so it is not a shipped-product regression — the target is to land it before
pnpm mode is promoted to default.

## 2. Background

### 2.1 The SVY-21456 stopgap (what we are removing)

SVY-21456 shipped the opt-in pnpm runner. pnpm 11+ turns the old npm-era "Ignored build
scripts" warning into a hard install failure (`ERR_PNPM_IGNORED_BUILDS`, exit 1) via its
`strictDepBuilds` setting: any dependency that ships an `install`/`postinstall` build script
must be explicitly approved or pnpm refuses to complete `install`/`add`/`update`/`remove`.

The stopgap disables that check two different ways in the current tree:

- **`RunPNPMCommand.addUnblockDepBuildsFlag(List)`**
  (`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/RunPNPMCommand.java:444-455`)
  appends `--config.strictDepBuilds=false` to every package-mutating verb. It is called from
  `translateToPnpmArguments(...)` (`:385-442`) for `install`/`add`, `ci`
  (`install --frozen-lockfile`), `update`, and `uninstall`→`remove`. Its own Javadoc names
  this follow-up case:
  > *"Until proper per-solution approval of dependency build scripts is designed (Developer
  > UI prompt + persisted in the solution so the headless WAR export can build unattended -
  > tracked as a separate case), disable the strict check …"*
- **The solution-local `pnpm-workspace.yaml`**
  (`com.servoy.eclipse.ngclient.ui/node/pnpm-workspace.yaml`) statically carries
  `strictDepBuilds: false` alongside `nodeLinker: hoisted` and `shamefullyHoist: true`. This
  file is copied into every solution's standalone pnpm project by
  `NodeFolderCreatorJob.copyAllEntries("/node", ...)`.

Net effect today: pnpm demotes `ERR_PNPM_IGNORED_BUILDS` to a warning and completes exit 0,
but the build scripts are **never run**. That is harmless for the TiNG client itself (the
ignored scripts — `esbuild`, `@parcel/watcher`, `lmdb`, `msgpackr-extract` — are launcher
shims / optional native optimizations with working fallbacks), but wrong for any installed
component/service package that genuinely needs a native build step.

The npm-era `allowScripts` key in the shipped `package.json` files does nothing under pnpm;
pnpm uses `allowBuilds` instead (2.2).

This case **fully retires** the stopgap: `addUnblockDepBuildsFlag` / the unconditional
`--config.strictDepBuilds=false` and the static `strictDepBuilds: false` are removed, replaced
by allowed/denied-list-driven, **version-scoped** `allowBuilds` generation. No replacement
"allow all" opt-out property is added, and pnpm's own `dangerouslyAllowAllBuilds` is **not**
used — once this mechanism is in place we build strictly on the allowed/denied lists.

### 2.2 pnpm's non-interactive, version-matching allow-list mechanism

**Runtime version note:** the bundled pnpm is **12.7.0** (`<pnpm.version>12.7.0</pnpm.version>`
in the `com.servoy.eclipse.nodejs.*` platform poms). pnpm **removed** the pnpm-10-era
`onlyBuiltDependencies` / `ignoredBuiltDependencies` / `neverBuiltDependencies` settings in
**v11**; from v11 the single mechanism is the **`allowBuilds` map**
(documented at pnpm.io/settings/build, added v10.26.0): `{ <matcher>: true|false }` where
`true` lets matching dependencies run their install/build scripts and `false` refuses them.
`strictDepBuilds` is still valid (added v10.3.0): with it on, any dependency that has a build
script and is **not** matched by an `allowBuilds` entry is a *pending* build that fails/warns
the install until decided.

**`allowBuilds` is a version matcher — verified live against the bundled pnpm 12.7.0:**

- A bare name key (`esbuild: true`) trusts **every** installed version of that package.
- A `name@version` key (`esbuild@0.25.0: true`) trusts **only** that exact version. Verified:
  key `esbuild@9.9.9` does **not** allow installed `esbuild@0.25.0` (install still fails
  `ERR_PNPM_IGNORED_BUILDS`); key `esbuild@0.25.0` **does** (postinstall runs, exit 0).
- A disjunction key (`esbuild@0.25.0 || 0.26.0: true`) trusts those listed versions; verified it
  allows `0.25.0`.

**This version-matching is deliberate pnpm supply-chain design, not a limitation we work around.**
pnpm's supply-chain documentation states the intent: explicitly listing trusted dependencies via
`allowBuilds` means *"if a dependency did not require a build in the past, it won't suddenly run a
malicious script if a compromised version is published."* pnpm's v10.19 / v10.26 release notes:
*"You can now allow specific versions of dependencies to run postinstall scripts … package names
with exact versions (and disjunctions via `||`)."* pnpm also ships `dangerouslyAllowAllBuilds`
(we deliberately do **not** use it) and, since v12.11, a `permissions:` map that outranks
`allowBuilds` (we do **not** need it).

**This case therefore chooses the version-scoped form**: the generated `pnpm-workspace.yaml`
emits `name@version` `allowBuilds` keys, and the committed approvals file stores `name@version`
entries. The looser bare-name form was the easier choice earlier; the version-scoped form is the
safe one and is what we adopt. We write this map directly, never via pnpm's interactive TUI
(`pnpm approve-builds`, `pnpm add --allow-build=<pkg>`).

### 2.2.1 pnpm stores the approval decision nowhere committed

Verified against the bundled pnpm 12.7.0: the allow/deny *decision* is not persisted in any
file pnpm would have us commit, so our own committed file remains the only home for it.

- `pnpm-lock.yaml` has a `settings:` block (`autoInstallPeers`, `excludeLinksFromLockfile`) but
  **no** `allowBuilds` / `onlyBuilt` / `ignored` keys — it records dependency versions and
  integrity hashes only.
- `node_modules/.modules.yaml` *does* carry `ignoredBuilds: [esbuild@0.28.1, esbuild@0.28.2,
  lmdb@3.5.6]` and `pendingBuilds: []`, but it is a throwaway `node_modules` artifact, not
  committed and not stable.

So the approvals decision lives only in **our** committed
`ng_web_packages/pnpm-build-approvals.json` (3.2). The lockfile is committed too, but for a
different reason — to pin *which versions install* (3.10), not to record the build decision.

### 2.3 The pnpm solution project layout (SVY-21456)

Each solution's TiNG folder under `target/<solution>/` is a **standalone pnpm project** (not
a workspace member of the shared root): it carries the full core `package.json` plus the
web-package tarballs, and a solution-local `pnpm-workspace.yaml`
(`nodeLinker: hoisted`, `shamefullyHoist: true`, `strictDepBuilds: false`) copied from the
shipped `com.servoy.eclipse.ngclient.ui/node/pnpm-workspace.yaml`. See
`docs/SVY-21456-ship-pnpm-runtime.spec.md` §3.5 / §3.7.1.

Crucially, `target/<solution>/` is a **generated / `target/` artifact — not git-committed
source**. The generated `target/<solution>/` currently contains *both* a `pnpm-lock.yaml` and a
`package-lock.json`, but both are regenerated target artifacts, not committed. The shipped
`com.servoy.eclipse.ngclient.ui/node/` has **no** committed lockfile, and
`NodeFolderCreatorJob.checkForHigherTimestamp` even explicitly **skips** `package-lock.json`.
`NodeFolderCreatorJob.copyAllEntries("/node", ...)` copies the shipped node template into
`target/<solution>/`.

So the *persisted, committed* artifacts and the *generated pnpm config* are distinct things that
must be wired together:

- the committed **approvals** file (in `ng_web_packages`, 2.5) is the source of truth for the
  `allowBuilds` decisions; the generated `pnpm-workspace.yaml`'s `allowBuilds` map is derived
  from it on every build (3.3);
- the committed **`pnpm-lock.yaml`** (also in `ng_web_packages`, 3.10) pins which versions
  install; the generated `target/<solution>/pnpm-lock.yaml` is seeded from it and copied back to
  it after a deliberate developer install.

### 2.4 The install/build call path (Developer and WAR export)

Both the Developer build and the headless WAR export reach pnpm through the same factory and
job classes:

- `Activator.createNPMCommand(File folder, List<String> args)`
  (`Activator.java:453-470`) returns a `RunPNPMCommand` when `Activator.isPnpmMode()` and a
  bundled pnpm exists, else `RunNPMCommand`/`NoOpNPMCommand`.
- `IRunNPMCommand.setExtraEnvironment(Map<String,String>)` already exists as a seam for
  passing per-process config/env to pnpm (`RunPNPMCommand` merges `extraEnvironment` into the
  `ProcessBuilder` environment at `runCommand`, `:122-271`).
- **Developer build:** `WebPackagesListener.createNodeFolderAndCheckPackages()` →
  `NodeFolderCreatorJob` → `WebPackagesListener.checkPackages(false)` →
  `PackageCheckerJob.run()` (the inner job that installs web-package tarballs and runs the
  Angular build). The active solution folder is `Activator.getSolutionProjectFolder()`.
- **Headless WAR export:** `WebPackagesListener.exportNG2ToWar(ING2WarExportModel)`
  (`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/WebPackagesListener.java:1776-1805`)
  runs a `NodeFolderCreatorJob` + a `PackageCheckerJob` with a `warExportModel` against
  `target/<solution>_dist`. The solution name comes from `model.getSolutionName()` (`:1783`).
  The CLI entry is `WarWorkspaceExporter.exportActiveSolution(WarArgumentChest)` →
  `WarExporter.doExport(...)` → `WarExporter.copyNGClient2(...)` →
  `com.servoy.eclipse.model.Activator.exportNG2ToWar(ING2WarExportModel)` →
  `WebPackagesListener.exportNG2ToWar(...)`. This path runs inside a **headless but fully
  loaded Eclipse workspace with a live `ServoyModel` and an active solution project** — so it
  can read the committed approvals + lockfile from the active solution's `ng_web_packages`
  folder without any UI.
- **Verb translation:** `RunPNPMCommand.translateToPnpmArguments(...)` already maps the `ci`
  verb to `install --frozen-lockfile` — the frozen install this case leans on for WAR/CI (3.10)
  is already reachable through that translation (or an explicit `--frozen-lockfile` flag).

### 2.5 Where a solution's committed web packages live (the persistence home)

Component/service **web-package zips** are stored inside the solution's Eclipse project in the
**`ng_web_packages` directory** (`SolutionSerializer.NG_PACKAGES_DIR_NAME = "ng_web_packages"`,
`com.servoy.eclipse.model/src/com/servoy/eclipse/model/repository/SolutionSerializer.java:126`).
Verified real folder (holds `12grid.zip`, `aggrid.zip`, `bootstrapcomponents.zip`,
`fontawesome.zip`, `servoyextra.zip`, …). This is the directory behind the Solution Explorer node
`UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES`:

- The WPM / Solution Explorer import handlers write the `.zip` packages there:
  `InstallWebPackageHandler.importZipFileComponent(...)` targets
  `RemoveWebPackageHandler.checkPackagesFolderCreated(selectedSolution, NG_PACKAGES_DIR_NAME)`
  (`InstallWebPackageHandler.java:154-158`, `RemoveWebPackageHandler.java:48,65-90`), which
  resolves `ServoyModel.getServoyProject(solutionName).getProject().getFolder("ng_web_packages")`
  (`RemoveWebPackageHandler.getInstallTargetProject`, `:92-97`), creating it if missing.
- The WAR/solution exporter reads the same folder per module:
  `ExportSolutionJob.getModulesWebPackages()` iterates
  `moduleProject.getProject().getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME).members()`
  (`ExportSolutionJob.java:134-173`).

So `<solution-project>/ng_web_packages/` is already a **git-committed source folder that
travels with the solution** and is the natural home for both the per-solution build-script
allow/deny lists **and** the committed `pnpm-lock.yaml` — right next to the zips those decisions
are about. It is reachable in both Developer and the headless WAR CLI via the active
`ServoyProject` (`ServoyModel.getServoyProject(name).getProject().getFolder("ng_web_packages")` /
`getLocation()`).

## 3. Design

### 3.1 Committed persistence location (THE key design decision)

**Two new git-committed files live inside the solution's web-packages directory** (the same
`ng_web_packages` folder that holds the component/service zips):

```
<solution-project>/ng_web_packages/pnpm-build-approvals.json   (the build-script decisions)
<solution-project>/ng_web_packages/pnpm-lock.yaml              (the pinned dependency versions)
```

A single approvals file holds both the allowed and denied lists (3.2). Rationale, and why the
alternatives were rejected:

- **They live with the web-package zips they govern.** The build-script decisions and the
  version pins are about the packages whose zips sit in `ng_web_packages/`; keeping both in that
  same committed folder is the most discoverable, cohesive place. It is already git-committed and
  travels with the solution sources, exactly like the zips.
- **They are readable headlessly by the WAR export CLI with no UI.** The WAR export CLI runs with
  a live `ServoyModel` and the active solution name (`model.getSolutionName()`, 2.4). Each file
  is resolved as
  `ServoyModel.getServoyProject(solutionName).getProject().getFolder("ng_web_packages")` +
  `/pnpm-build-approvals.json` (and `/pnpm-lock.yaml`) — the same `getFolder(NG_PACKAGES_DIR_NAME)`
  lookup `getModulesWebPackages` and `checkPackagesFolderCreated` already use. No running
  Developer UI, no workspace editor, no websocket — plain file reads.
- **Not at the project root:** the user wants them in the webpackages directory alongside the
  zips, not loose at the project root.
- **Not `target/<solution>/`** (the generated project): that directory is a non-committed
  `target/` artifact (2.3), so persisting there would not survive a clean and would never be
  committed to git. The generated `target/<solution>/pnpm-workspace.yaml` and
  `target/<solution>/pnpm-lock.yaml` are the *consumers* of the committed files, not their home.
- **Modules:** the lists and lockfile are **per solution project**, written in the project that
  owns the build. For a solution with modules, this case maintains the files in the active (main)
  solution's `ng_web_packages`; aggregating per-module files is a possible follow-up (noted in §6).

### 3.2 Approvals file format (version-scoped)

`ng_web_packages/pnpm-build-approvals.json` — a small, hand-reviewable JSON document holding
**both** lists, keyed by **exact `name@version`**, versioned for forward compatibility:

```json
{
  "version": 1,
  "allowed": [
    "esbuild@0.28.1",
    "esbuild@0.28.2",
    "lmdb@3.5.6"
  ],
  "denied": [
    "some-package@1.2.3"
  ]
}
```

- `allowed` → each `name@version` becomes an `allowBuilds: { '<name@version>': true }` entry (the
  specific versions whose build scripts are allowed to run).
- `denied` → each `name@version` becomes an `allowBuilds: { '<name@version>': false }` entry
  (explicitly refused; kept off the "pending" prompt on subsequent runs so the developer is not
  re-asked for that exact version).
- A `name@version` that is in **neither** list is **pending** — it is surfaced for a decision by
  the upfront discovery query (3.4) and must be decided before the build proceeds (3.5 developer
  flow). The build does **not** silently run a pending script.
- **Version-scoped entries are the point.** A package legitimately appears at **multiple
  versions** in one tree — verified: the real tree has both `esbuild@0.28.1` **and**
  `esbuild@0.28.2`, so both are listed. Approving `esbuild@0.28.1` does **not** cover a
  (potentially hijacked) `esbuild@0.28.3`; a bump to `0.28.3` is automatically pending and
  re-asked. This is the deliberate supply-chain behavior (2.2): a newly-published compromised
  version cannot inherit a prior version's trust.
- Denied is likewise version-scoped: denying `foo@1.2.3` leaves a later `foo@1.3.0` pending again
  (correct — the developer reviews the new version).

A small Java helper type (`PnpmBuildApprovals`) encapsulates load
(`fromWebPackagesFolder(IProject)` / `fromFile(File)`), save, and the
`allowed`/`denied`/`pending(discovered)` sets (all `name@version`), so there is exactly one
reader/writer. Per §3.8 the **read-only** form of this helper (and the file-location logic) must
be reachable from the headless model/exporter side; the UI editing lives in
`com.servoy.eclipse.ngclient.ui`.

### 3.3 Generating pnpm config from the committed lists (versioned)

On every build in pnpm mode, after `NodeFolderCreatorJob` has copied `/node` into
`target/<solution>/` (which places the shipped `pnpm-workspace.yaml`), and **before** the
first package-mutating pnpm command runs, the generated solution-local
`target/<solution>/pnpm-workspace.yaml` is rewritten to carry the committed decisions:

- Read `<solution-project>/ng_web_packages/pnpm-build-approvals.json`.
- Write into the generated `pnpm-workspace.yaml` (pnpm 12 syntax, 2.2):
  - an `allowBuilds` map with **one entry per exact `name@version`**: each `allowed`
    `name@version` → `true`, each `denied` `name@version` → `false`. Example:

    ```yaml
    nodeLinker: hoisted
    shamefullyHoist: true
    strictDepBuilds: true
    allowBuilds:
      'esbuild@0.28.1': true
      'esbuild@0.28.2': true
      'lmdb@3.5.6': true
      'some-package@1.2.3': false
    ```

  - keep `nodeLinker: hoisted`, `shamefullyHoist: true`.
  - **YAML quoting:** any key that contains `@`, `/`, a space, or `||` is single-quoted, with
    YAML single-quote escaping (`''`); the value is the bare literal `true`/`false`. Since every
    versioned key contains `@`, they are always single-quoted.
- **One-entry-per-exact-version is the chosen generation form** (simplest to generate and the
  cleanest diff). The disjunction form (`'esbuild@0.28.1 || 0.28.2': true`) is equivalent and
  available (2.2) but not used, to keep generation and review trivial.
- **Replace** the static `strictDepBuilds: false` with `strictDepBuilds: true`, so that a
  *newly appearing* pending build script (a `name@version` not matched by any `allowBuilds`
  entry) is surfaced (caught by the discovery query 3.4) rather than silently ignored.

Because this generated file is a `target/` artifact, regenerating it every build is correct
and cheap; the committed `ng_web_packages/pnpm-build-approvals.json` remains the single source
of truth, and the generated yaml is derived from it.

Implementation note: writing YAML can be done with the project's existing YAML/stringbuilder
approach used for the shipped file (it is a tiny, fixed-shape document). The shipped
`com.servoy.eclipse.ngclient.ui/node/pnpm-workspace.yaml` has its static `strictDepBuilds:
false` removed (the generated rewrite is authoritative; see §7).

### 3.4 Upfront discovery query (what the developer must decide — versioned)

Before a full build, enumerate the `name@version` packages pnpm wants to run build scripts for.

- **Authoritative source: `node_modules/.modules.yaml`'s `ignoredBuilds`.** Verified: it is the
  structured, **versioned** source of pending packages (`ignoredBuilds: [esbuild@0.28.1,
  esbuild@0.28.2, lmdb@3.5.6]`). Read it directly and parse the `name@version` entries. This is
  what discovery uses.
- **`pnpm ignored-builds` is NOT used for the versioned set:** the CLI prints the bare **names**
  only (`esbuild`, `lmdb`), which loses the version and so cannot distinguish `esbuild@0.28.1`
  from `esbuild@0.28.2`. It is kept only as a note, not as the pending source.
- **Acceptable fallback:** parsing the `ERR_PNPM_IGNORED_BUILDS` error line, which is also
  **versioned** (`esbuild@0.28.1, esbuild@0.28.2, lmdb@3.5.6`). Use this only if `.modules.yaml`
  is unavailable.
- Both `.modules.yaml` and the error line reflect the state of the **installed** `node_modules`,
  so discovery is meaningful only **after** an install has materialized the dependency tree.
  Running against an empty `target/<solution>/` (a clean/first build) finds nothing, which would
  make the gate a silent no-op exactly when it matters. Discovery therefore runs **against an
  installed tree**: either (a) do the first install for the solution with `strictDepBuilds`
  demoted *for that discovery install only* (via `setExtraEnvironment`, scoped and local — not
  the retired blanket stopgap) and then read `.modules.yaml`, or (b) capture the structured
  ignored-builds the first install reports. The implementation keeps the parse isolated and falls
  back to an empty pending set rather than failing the build.
- **Dedup against the committed lists (by `name@version`):** subtract `name@version` entries
  already in `allowed` **and** already in `denied` →
  `pending = discovered(name@version) − allowed − denied`. A copy-and-build or clean-and-build
  must **not** re-ask for `name@version` entries already decided. But a *new* version of an
  already-decided package (e.g. `esbuild@0.28.3` when only `0.28.1`/`0.28.2` are decided) **is**
  pending and **is** re-asked — the headline re-prompt-on-version-change behavior.

The point of this query is to report to the developer up front: "you need to decide on these
specific `name@version` packages to do a build." In the headless WAR export there is **no**
discovery prompt (3.5 / 3.9) — the export executes the committed list and fails loud if an
undecided version is reached (3.10).

### 3.5 Developer full-build flow (ordered)

On a Developer full build in pnpm mode, in this order:

a. **Seed the lockfile, then apply the known allowed list.** Seed
   `target/<solution>/pnpm-lock.yaml` from the committed
   `ng_web_packages/pnpm-lock.yaml` if present (3.10), then generate the solution
   `pnpm-workspace.yaml` (3.3) from the committed `allowed`/`denied` lists so the already-decided
   versions are fed to pnpm as versioned `allowBuilds` entries.

b. **Run the upfront discovery query (3.4)** to see whether there are MORE `name@version`
   packages that need a decision — those in neither the allowed nor the denied list (the pending
   set, including any newly-bumped version of an already-known package).

c. **Gate the build on resolving the pending set.** If the pending set is non-empty, prompt the
   developer to allow or deny **each** pending `name@version` before the build moves on — the
   prompt shows the exact `name@version` so the developer knowingly approves a specific version.
   On confirm, persist the decisions into the `allowed`/`denied` lists in
   `ng_web_packages/pnpm-build-approvals.json` (via the 3.2 helper), regenerate the solution
   `pnpm-workspace.yaml` (3.3), then **continue** the build. If the pending set is empty, the
   build proceeds immediately.

d. **Commit-back the lockfile.** After a successful install/approve/rebuild cycle, copy the
   resulting `target/<solution>/pnpm-lock.yaml` back to the committed
   `ng_web_packages/pnpm-lock.yaml` (3.10), so the developer's install is what gets committed and
   only a deliberate install changes the pinned versions.

The prompt is the only interactive surface, and everything pnpm sees is the generated explicit
versioned list (never pnpm's own TUI prompt). The prompt is gated on a real UI being present
(`!Activator.isNodeExtractionAndTitaniumBuildDisabled()` and a UI display), so automated runs
and the WAR CLI never pop a dialog.

### 3.6 Denied packages stay denied; the build proceeds anyway

A `name@version` in the `denied` list stays denied: its build script is **not** run (its
`allowBuilds` entry is `false`) and the developer is **not** re-prompted for that exact version
on subsequent builds. The build still proceeds as normal. That build **could** fail because of
the denial (if the denied package genuinely needed its native step) — that outcome is
**accepted** and is the user's responsibility. We do not hard-fail or re-prompt on account of a
denied version. (A *different* version of the same package is a distinct entry and is pending —
3.4.)

### 3.7 Context-menu view to review/edit the two lists

A **context menu on the `ng_web_packages` node**
(`UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES`, handled in
`SolutionExplorerView.fillContextMenu` at `SolutionExplorerView.java:3011-3015`) opens a
view/dialog that shows the **allowed** and **denied** `name@version` entries for that solution,
letting the developer review and edit them (move a `name@version` between lists, remove an
entry). On save it writes `ng_web_packages/pnpm-build-approvals.json` via the 3.2 helper. This is
pure UI and lives on the UI side (§3.8). It is the manual counterpart to the build-time prompt
(3.5c).

### 3.8 Module placement & dependencies

- **UI side (`com.servoy.eclipse.ngclient.ui`, or the UI plugin):** the build-time
  allow/deny prompt (3.5c), the context-menu review/edit view (3.7), and the editing/save of
  `pnpm-build-approvals.json`. ngclient.ui is the better home for the pure-UI pieces. The
  **model must not contain UI code.**
- **Headless/model side:** only needs to **read** the saved lists and the committed lockfile,
  generate the derived `pnpm-workspace.yaml` (3.3), and do the lockfile seed/commit-back (3.10).
  Put the pure file-format + file-location reader (`PnpmBuildApprovals` load +
  versioned `allowBuilds` extraction) and the lockfile copy helpers where both the Developer
  build and the WAR export path reach them without a new cross-plugin dependency:
  `com.servoy.eclipse.ngclient.ui` already owns `WebPackagesListener.exportNG2ToWar`, so the
  reader can live there (SWT-free), and the WAR CLI path uses it with **no UI at all** — it reads
  the committed approvals + lockfile from the `ng_web_packages` dir and runs the build unattended.
- Keep every `org.eclipse.swt`/JFace import out of the headless path so the WAR CLI never loads
  UI classes.
- No new target-platform dependency is expected (JSON via the existing `org.json`
  `JSONObject`; file IO via `commons-io` `FileUtils` / `java.nio`; the `.modules.yaml` /
  `pnpm-lock.yaml` parsing is line-oriented text with the existing YAML helper approach).

### 3.9 What stays identical

- npm mode (`servoy.jsRuntime` unset/`npm`) is completely unaffected — no approval file and no
  committed `pnpm-lock.yaml` is read or generated, `pnpm-workspace.yaml` is ignored by npm, and
  the behaviour is byte-for-byte today's. (We deliberately do **not** commit `package-lock.json`
  for npm mode; npm mode is out of scope — §6.)
- The pnpm solution topology (standalone project, tarball installs, hoisted linker) from
  SVY-21456 is unchanged except for the `strictDepBuilds` / versioned `allowBuilds` lines in the
  generated `pnpm-workspace.yaml`, and the new lockfile seed/commit-back.
- The Angular CLI build step, console streaming, cancel handling, and exit-code plumbing in
  `RunPNPMCommand` are unchanged.
- **WAR export is fully unattended with no UI:** it reads the committed approvals + lockfile from
  `ng_web_packages` and executes frozen (3.10) — allowed versions' scripts run, denied ones do
  not, no prompt, no discovery gate; a reached-but-undecided version fails loud (3.10).

### 3.10 Committed lockfile + frozen CI reproducibility (NEW design area)

The intent: a developer checks in **two files** — the approvals file and the pnpm lockfile — so
that the WAR exporter / CI build (1) has the right version-scoped allows in the generated
`pnpm-workspace.yaml`, and (2) installs the **exact** same dependency versions every time via the
committed lockfile, so transitive deps cannot silently drift between builds. Versions change only
when a developer runs an install that updates the lockfile (and, if a newly-built version
appears, re-approves it) and commits the updated file(s).

**How the two files relate:**

- `ng_web_packages/pnpm-lock.yaml` pins **which** versions install — plus an integrity hash per
  package, so a hijacked tarball republished at an existing version fails to install at all.
- `ng_web_packages/pnpm-build-approvals.json` says **which of those installed versions'** build
  scripts may run.
- Together they give reproducible, version-safe builds across many CI runs: the lockfile freezes
  the version set; the approvals (version-scoped) freeze which of those may execute build scripts.
  Both change only on a deliberate developer install.

**Developer build (lockfile side, ties into 3.5):**

- **Before install:** seed the generated `target/<solution>/pnpm-lock.yaml` **from** the committed
  `ng_web_packages/pnpm-lock.yaml` if present, so the developer build is reproducible too and only
  a deliberate update changes it. This seeding is done **explicitly by our new step**, not by
  `copyAllEntries` — `NodeFolderCreatorJob.checkForHigherTimestamp` skips lockfiles (2.3), so the
  copy must not rely on the template copy.
- **After a successful install/approve/rebuild cycle (before the Angular build):** copy **both** the
  resulting `target/<solution>/package.json` **and** `target/<solution>/pnpm-lock.yaml` **back** to
  the committed `ng_web_packages/` (§3.11), so what the developer installed — the full component set
  and its exact pins — is what gets committed. The commit-back runs once the dependency tree is fully
  materialized (installs + `pnpm add <all tarballs>` + rebuild done), at which point
  `package.json`/`pnpm-lock.yaml` are complete and correct; the subsequent Angular build only reads
  them and does not mutate them, so the committed pair matches exactly what the build compiles
  against (and a non-dependency Angular build failure still leaves the correct pair committed). A
  deliberate dependency change therefore produces a `package.json` + lockfile diff the developer
  commits alongside any approvals diff.

**WAR export / CI (frozen, headless):**

- Seed all three committed files — `package.json`, `pnpm-lock.yaml` (both the FULL set), and the
  approvals-derived `pnpm-workspace.yaml` — into `target/<solution>/` (explicit step, as above, and
  §3.11).
- Pack the exported-subset tarballs, then `pnpm remove <each non-exported @servoy/* component>` so
  `package.json` and the lockfile drop exactly the components the WAR does not ship, with no version
  drift (§3.11).
- Run the install **frozen** — `pnpm install --frozen-lockfile` (the existing `ci` translation,
  2.4, or an explicit `--frozen-lockfile` flag) — so the installed versions are **exactly** the
  committed (now subset-trimmed) set and cannot drift. No UI, no prompt.

**Drift / fail-loud (the safety net):**

- Under `--frozen-lockfile`, if the committed lockfile does not match `package.json` (would need
  updating), pnpm **fails loudly** — that is the signal a developer must re-install and commit the
  updated lockfile. We do not silently fall back to `--no-frozen-lockfile` in CI.
- If a build script for a `name@version` **not** in the committed approvals is reached (e.g. the
  committed lockfile and approvals drifted out of sync), the headless build **fails loud** with the
  exact `name@version` list and a message telling the developer to open the solution, approve/deny,
  and commit — it **never** silently runs and **never** silently skips. We explicitly do **not**
  use `dangerouslyAllowAllBuilds` and add **no** opt-out property.

**Packaging note:** the committed `pnpm-lock.yaml` and approvals file live in the **solution
project** (not a bundle), so no bundle `build.properties` change is needed. But because
`NodeFolderCreatorJob` currently **skips** `package-lock.json` in its copy/timestamp logic (fact
in 2.3), the design must seed the committed `pnpm-lock.yaml` into `target/<solution>/` with an
**explicit new step**, not by relying on `copyAllEntries`.

### 3.11 Committed `package.json` + WAR subset via targeted `remove` (NEW — resolves the frozen-vs-subset problem)

A frozen install is only reproducible if the generated `target/<solution>/package.json` and the
seeded `pnpm-lock.yaml` **both** agree on the full component set. Two facts about the real build,
verified live, force this third committed file and the `remove` step:

1. **The generated core `package.json` does not declare the component tarballs.** `NodeFolderCreatorJob`
   copies the shipped core `/node/package.json` (which lists only the core/toolchain deps —
   `@angular/*`, `ag-grid-*`, `rxjs`, … 18 entries), and strips even `@servoy/public`
   (`removePublicFileDependency`, `NodeFolderCreatorJob.java:229`). The `@servoy/*` web-package
   tarballs are added to `package.json` only later, by `WebPackagesListener`'s unfrozen
   `pnpm add <tarball>`. So a freshly generated dist `package.json` has **zero** `@servoy/*`
   dependencies until an install mutates it.

2. **pnpm's `--frozen-lockfile` compares specifiers (content), not mtime.** Verified: touching
   `package.json` newer than the lockfile does **not** trip `ERR_PNPM_OUTDATED_LOCKFILE`; only a
   real specifier set mismatch does. If the committed lockfile lists 9 `@servoy/*` tarballs but the
   dist `package.json` lists 0 (or a different subset), frozen fails loud
   (`ERR_PNPM_OUTDATED_LOCKFILE: N dependencies were removed`) — verified for both "all 9 missing"
   and "1 of 9 missing (subset)". The earlier build only "passed" frozen because **both** the dist
   `package.json` and the seeded lockfile had ended up component-less (internally consistent but the
   wrong, empty set), so `node_modules` shipped without `@servoy/public` and the Angular build
   failed on hundreds of "Cannot find module".

**Therefore the committed source is THREE files, and the WAR path removes what it does not ship:**

```
<solution-project>/ng_web_packages/pnpm-build-approvals.json   (build-script decisions)
<solution-project>/ng_web_packages/pnpm-lock.yaml              (pinned versions, FULL set)
<solution-project>/ng_web_packages/package.json                (the FULL-set package.json)
```

- The committed `package.json` + `pnpm-lock.yaml` are a **matched pair**: the lockfile is exactly
  the resolution of that `package.json`, both covering the **full** component set the developer
  built (all tarballs the developer's build packed). The developer commits both after a successful
  full build so there is a proven-complete, mutually-consistent pair.

- **The WAR export ships a subset.** `PackageCheckerJob` filters every component/service/layout
  against `warExportModel.getExportedPackagesExceptSablo()` / `getAllExportedComponents()`
  (`WebPackagesListener.java:270,305,327,424,487`), so a solution that does not export, e.g.,
  `servoydefault` packs 8 tarballs, not 9. The exported set — hence its complement, the components
  to drop — is known at build time from the war model (and is **not** limited to `servoydefault`;
  any non-exported component is dropped the same way).

- **WAR install = restore the full committed pair, then targeted `remove`, then frozen `ci`:**
  1. Seed all three committed files into `target/<solution>/` (restore the full `package.json`,
     the full `pnpm-lock.yaml`, and the approvals-derived `pnpm-workspace.yaml`).
  2. Pack the tarballs the WAR actually ships into `target/<solution>/.tarballs/` (the existing
     `packPackage` logic already does this for the exported subset).
  3. For each committed `@servoy/*` component **not** in the WAR's exported set, run a single
     `pnpm remove <component>…`. Verified live: `pnpm remove` deletes exactly those dependencies
     from both `package.json` and the lockfile and **does not re-resolve or drift** the remaining
     pinned versions (a pinned transitive stayed byte-identical across the remove). After the
     remove, `package.json` and the lockfile again agree — on the shipped subset.
  4. Run `pnpm install --frozen-lockfile` (`ci`). Because `package.json` and the lockfile now match
     the subset, frozen passes ("Lockfile is up to date"), installs the exact pinned versions, and
     the component tarballs are present → the Angular production build resolves every module.
     Verified end-to-end in a probe: restore full pair → `remove` the unused component → frozen `ci`
     → shipped component present, dropped component absent, no version drift.

- **No unfrozen `pnpm add` on the WAR path.** The previous B1 attempt (skip the add, rely on seed)
  shipped an empty set; the opposite over-correction (unfrozen `add` on the WAR path) would
  re-resolve and rewrite the committed lockfile. The `remove`-then-frozen approach keeps the WAR
  install reproducible against the committed pins while shipping only the exported subset — it is
  the only form that satisfies both.

- **Relative tarball specifiers (already done):** the tarballs are added to the committed
  `package.json`/lockfile with a path **relative** to the project folder (`file:.tarballs/<name>.tgz`,
  forward slashes), never an absolute `C:\…` path, and the tarball name is a **content hash** so it
  is stable across machines. This is what makes the committed `package.json`/lockfile portable to a
  Linux WAR exporter and identical across the dev and dist target folders (verified: the committed
  lockfile contains zero absolute paths).

## 4. Implementation plan

1. **Approvals model + committed-file IO (versioned)** — add `PnpmBuildApprovals`
   (version/`allowed`/`denied` as `name@version` string sets, with a `pending(discovered)` helper
   that does `discovered − allowed − denied`) and
   `fromWebPackagesFolder(IProject)` / `fromFile(File)` / `save(...)` reading/writing
   `<solution-project>/ng_web_packages/pnpm-build-approvals.json` via `org.json` + `FileUtils`.
   Resolve the folder with `project.getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME)` (the
   same lookup `getModulesWebPackages`/`checkPackagesFolderCreated` use). Keep the reader
   SWT-free and place it per §3.8 (`com.servoy.eclipse.ngclient.ui`).

2. **Generate pnpm config from the lists (versioned `allowBuilds`)** — add a method
   (`PnpmBuildScriptConfig`) that rewrites the solution-local
   `target/<solution>/pnpm-workspace.yaml` to carry a versioned `allowBuilds` map (pnpm 12 syntax,
   2.2/3.3): one entry per `name@version`, each `allowed` → `true`, each `denied` → `false`,
   single-quoting keys that contain `@`/`/`/space/`||`, plus `strictDepBuilds: true` and the
   retained `nodeLinker: hoisted` / `shamefullyHoist: true`. Call it in `NodeFolderCreatorJob` /
   `PackageCheckerJob.run()` before the first package-mutating pnpm command (both the Developer
   and `exportNG2ToWar` paths).

3. **Upfront discovery query (versioned, from `.modules.yaml`)** — add a small bounded discovery
   step that reads `node_modules/.modules.yaml`'s `ignoredBuilds` (`name@version`, the
   authoritative versioned source), falling back to parsing the `ERR_PNPM_IGNORED_BUILDS` error
   line (also versioned); do **not** use `pnpm ignored-builds` for the pending set (name-only).
   Then subtract the committed `allowed` and `denied` `name@version` sets to produce `pending`.
   Because `.modules.yaml` reflects the installed `node_modules`, run discovery **against an
   installed tree** (first install with `strictDepBuilds` demoted for the discovery step only, or
   capture the first install's ignored-builds output), not against an empty `target/<solution>/`
   (3.4). Keep the parse isolated and fall back to an empty pending set on unexpected output.

4. **Committed lockfile seed/commit-back wiring** — add explicit lockfile copy steps (§3.10):
   - **Before install (Developer + WAR):** if `ng_web_packages/pnpm-lock.yaml` exists, copy it to
     `target/<solution>/pnpm-lock.yaml` (explicit step in `NodeFolderCreatorJob` /
     `WebPackagesListener`, not via `copyAllEntries`, which skips lockfiles — 2.3).
   - **After a successful Developer install/rebuild:** copy `target/<solution>/pnpm-lock.yaml`
     back to `ng_web_packages/pnpm-lock.yaml`.
   - **WAR/CI install runs frozen:** `pnpm install --frozen-lockfile` (the existing `ci`
     translation or an explicit frozen flag — `RunPNPMCommand.translateToPnpmArguments`), so
     versions cannot drift; a lockfile-vs-package.json mismatch fails loud.

5. **Retire the SVY-21456 stopgap (no replacement property)** —
   - Drop the unconditional `addUnblockDepBuildsFlag(...)` calls from
     `RunPNPMCommand.translateToPnpmArguments(...)` package-mutating branches
     (`RunPNPMCommand.java:385-442`) and delete `addUnblockDepBuildsFlag` (`:444-455`).
   - Remove the static `strictDepBuilds: false` from the shipped
     `com.servoy.eclipse.ngclient.ui/node/pnpm-workspace.yaml` (step 2 governs it per build).
   - **Do NOT add** any `servoy.pnpm.allowAllBuilds` / "allow all" opt-out property, and do **not**
     use pnpm's `dangerouslyAllowAllBuilds` — the allowed/denied lists are the only control.
   - Update `RunPNPMCommandTest` to drop the `--config.strictDepBuilds=false` expectations and
     assert the verb translation no longer appends the unblock flag.

6. **Developer full-build flow (ordered gate)** — wire the ordered flow (3.5): seed the committed
   lockfile (step 4) → apply the known allowed/denied lists (step 2) → run discovery (step 3) → if
   the pending set is non-empty and a real UI is present
   (`!isNodeExtractionAndTitaniumBuildDisabled()`), show the allow/deny prompt (showing each
   `name@version`), persist choices via step 1, regenerate config (step 2), then continue the
   build → on success, commit-back the lockfile (step 4). Denied versions stay denied and the
   build proceeds regardless (3.6). No prompt in headless/test/WAR runs. UI lives in
   `com.servoy.eclipse.ngclient.ui` (§3.8).

7. **Context-menu review/edit view** — add a context-menu action on the
   `UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES` node in
   `SolutionExplorerView.fillContextMenu` (`:3011`) that opens a view/dialog showing the allowed
   and denied `name@version` entries for the solution and lets the developer edit them, saving via
   step 1. Pure UI, `com.servoy.eclipse.ngclient.ui` / UI plugin.

8. **Headless WAR export (frozen, no UI)** — in `WebPackagesListener.exportNG2ToWar(...)` read the
   committed approvals + lockfile for the solution (`model.getSolutionName()` →
   `ServoyModel.getServoyProject(name).getProject().getFolder("ng_web_packages")`), seed the
   lockfile into target (step 4), generate the versioned config (step 2) so allowed versions'
   scripts run unattended, and run the install **frozen** with **no UI and no discovery prompt**.
   A reached-but-undecided `name@version`, or a lockfile-vs-package.json mismatch, **fails loud**
   with the offending list (3.10); denied versions are simply not built and the build proceeds
   (3.6).

9. **Tests** — a `*Test` (plain unit, `com.servoy.eclipse.ngclient.ui.tests`) over the approvals
   model (parse/serialize both lists as `name@version`, `allowed`→`allowBuilds` `true`,
   `denied`→`allowBuilds` `false`, `pending = discovered − allowed − denied` with multiple
   versions of one package and a new-version-re-prompt case, version handling, missing/empty file)
   and the `pnpm-workspace.yaml` generation (idempotent, preserves linker keys, writes versioned
   quoted keys, `strictDepBuilds: true`, never emits removed `onlyBuiltDependencies`/
   `ignoredBuiltDependencies` keys). Update `RunPNPMCommandTest` (step 5).

10. **Build/validate** — `mvn clean verify`; manually validate in pnpm mode that: the upfront
    discovery query reports a `name@version` with a build script, the Developer flow gates the
    build until it is allowed/denied, allowing it writes `ng_web_packages/pnpm-build-approvals.json`
    with the `name@version`, the generated `pnpm-workspace.yaml` lists it in the `allowBuilds` map
    as `'name@version': true`, and the script runs; a second (copy/clean) build does **not**
    re-ask for already-decided versions; a transitive bump to a new version **is** re-asked; a
    denied version stays denied and the build still proceeds; the committed lockfile makes a
    subsequent headless WAR export install the exact versions **frozen** and run the allowed
    scripts unattended with no prompt; a lockfile/approvals drift fails loud; and npm mode is
    byte-for-byte unchanged.

## 5. Acceptance criteria

- [ ] Before a full build, a small bounded discovery query enumerates the **`name@version`**
      packages pnpm wants to run build scripts for (read from `node_modules/.modules.yaml`'s
      versioned `ignoredBuilds`, fallback the versioned `ERR_PNPM_IGNORED_BUILDS` line) and reports
      which need a decision.
- [ ] The developer can allow or deny each pending `name@version`, and those decisions are
      persisted in a git-committed file inside the solution's web-packages directory
      (`<solution-project>/ng_web_packages/pnpm-build-approvals.json`), holding an `allowed` and a
      `denied` list of `name@version` entries.
- [ ] The discovery step dedups against both committed lists by `name@version`: a copy-and-build
      or clean-and-build does **not** re-ask for any version already in `allowed` or `denied`.
- [ ] **Re-prompt on version change:** a transitive bump to a version **not** in the committed
      lists is automatically pending and is re-asked on the Developer build; an approval of
      `esbuild@0.28.1` does **not** cover `esbuild@0.28.3`, and WAR/headless does not silently
      trust the new version.
- [ ] The Developer full build runs in order: seed the committed lockfile, apply the known allowed
      versions (as `allowBuilds` `true` entries), run discovery, and if any `name@version` is in
      neither list the build is gated until the developer decides; decisions and the updated
      lockfile are persisted (commit-back) and the build then continues.
- [ ] On every pnpm-mode build the committed lists are translated into the generated solution
      `target/<solution>/pnpm-workspace.yaml` as a **versioned** `allowBuilds` map (each
      `name@version` → `true`/`false`, keys single-quoted), keeping `nodeLinker: hoisted` /
      `shamefullyHoist: true` and setting `strictDepBuilds: true`. (Must use the pnpm 12
      `allowBuilds` map, not the removed `onlyBuiltDependencies`/`ignoredBuiltDependencies` keys.)
- [ ] A committed `ng_web_packages/pnpm-lock.yaml` **and** `ng_web_packages/package.json` (a
      matched FULL-set pair, §3.11) are seeded into `target/<solution>/` (by an explicit step, not
      `copyAllEntries`). The WAR/CI install then drops the components the war does not export via
      `pnpm remove` (no version drift) and runs the remaining set **frozen** (`--frozen-lockfile`)
      so the installed versions are exactly the committed pins; a lockfile-vs-`package.json`
      mismatch **fails loud** rather than silently updating.
- [ ] After a successful Developer **full build**, the resulting `target/<solution>/package.json`
      **and** `pnpm-lock.yaml` are copied back to `ng_web_packages/`, so the developer commits the
      approvals file plus the matched `package.json`/lockfile pair; versions change only on a
      deliberate install. Tarball specifiers are **relative** (`file:.tarballs/<name>.tgz`,
      content-hash name), never absolute `C:\…`, so the committed pair is portable to a Linux WAR
      exporter.
- [ ] The WAR export ships only the **exported subset** of component tarballs: it restores the full
      committed `package.json`/lockfile pair, `pnpm remove`s each non-exported `@servoy/*` component
      (verified to not re-resolve/drift the remaining pins), then installs frozen — so a solution
      that does not export, e.g., `servoydefault` produces a WAR without it while every shipped
      component resolves.
- [ ] A denied `name@version` stays denied (its build script does not run, it is not re-prompted
      for that version) and the build proceeds anyway; a resulting failure is accepted.
- [ ] A context menu on the `ng_web_packages` node
      (`UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES`) opens a view showing the
      allowed and denied `name@version` entries and lets the developer edit them.
- [ ] A headless WAR export (`WarWorkspaceExporter` CLI → `WebPackagesListener.exportNG2ToWar`)
      reads the committed approvals + lockfile from `ng_web_packages` **with no UI at all** and
      builds unattended **frozen**: allowed versions' scripts run; no interactive pnpm TUI prompt;
      no discovery gate; a reached-but-undecided version **fails loud** with the exact
      `name@version` list (never silently runs or skips); no `dangerouslyAllowAllBuilds`.
- [ ] A package whose build script is genuinely needed (e.g. a native step in a
      component/service package) actually **runs** once that exact version is allowed and
      committed — the end-to-end regression the stopgap could not satisfy.
- [ ] The SVY-21456 stopgap is fully retired: `RunPNPMCommand` no longer appends
      `--config.strictDepBuilds=false` on any verb, `addUnblockDepBuildsFlag` is removed, and the
      static `strictDepBuilds: false` is gone from the shipped/generated config. **No** replacement
      "allow all" opt-out property exists and `dangerouslyAllowAllBuilds` is not used; the
      allowed/denied lists are the only control.
- [ ] The UI code (prompt, review/edit view) lives in `com.servoy.eclipse.ngclient.ui` / UI
      plugin; the model contains no UI code and only reads the committed lists + lockfile.
- [ ] npm mode (`servoy.jsRuntime` unset/`npm`) is byte-for-byte unchanged; no approval file,
      versioned config, or committed `pnpm-lock.yaml` is read, generated, or required, and no
      `package-lock.json` is committed for npm mode.
- [ ] `RunPNPMCommandTest` is updated (no `--config.strictDepBuilds=false` expectation) and new
      unit tests cover the versioned approvals model and the versioned `pnpm-workspace.yaml`
      generation.
- [ ] A full `mvn clean verify` succeeds.

## 6. Out of scope

- Making pnpm the default runtime, or any change to npm mode.
- Committing `package-lock.json` for npm mode — npm mode is unchanged and out of scope; only the
  pnpm `pnpm-lock.yaml` is committed.
- Per-module approval files / lockfiles for multi-module solutions — the files are maintained in
  the active (main) solution's `ng_web_packages` only; aggregating per-module
  `ng_web_packages/pnpm-build-approvals.json` / `pnpm-lock.yaml` files is a possible follow-up.
- Any replacement "allow all" / global opt-out property, and pnpm's `dangerouslyAllowAllBuilds`
  (deliberately not used — the lists are the only control).
- pnpm's v12.11 `permissions:` map (outranks `allowBuilds`; not needed here).
- The cross-drive `store-dir` handling (separate SVY-21456 follow-up).
- Changing the pnpm solution topology (tarball installs, hoisted linker) from SVY-21456.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Enumerating pending/ignored build scripts non-interactively — **resolved**: read the versioned `ignoredBuilds` from `node_modules/.modules.yaml` (verified `name@version`), fallback the versioned `ERR_PNPM_IGNORED_BUILDS` line; `pnpm ignored-builds` is name-only and so not used for the pending set. | dev | resolved |
| Exact committed lockfile filename/location — spec chooses `<solution-project>/ng_web_packages/pnpm-lock.yaml` (next to the zips and the approvals file). Confirm this is the preferred committed home vs. an alternative committed location. | dev | resolved — `ng_web_packages/pnpm-lock.yaml`, and **also** `ng_web_packages/package.json` (the matched FULL-set pair, §3.11). |
| WAR ships a subset of components, but the committed lockfile is the full set — how to install frozen without `ERR_PNPM_OUTDATED_LOCKFILE`? — **resolved** (§3.11): commit the full `package.json`+lockfile pair, restore both on the WAR path, `pnpm remove` each non-exported component (verified: no re-resolve / no version drift), then `pnpm install --frozen-lockfile`. No unfrozen `add` on the WAR path. | dev | resolved |
| Whether to also commit `package-lock.json` for npm mode — spec recommends **NOT** (npm mode unchanged, out of scope). | dev | open — spec assumes no. |
| Should the **shipped** `com.servoy.eclipse.ngclient.ui/node/pnpm-workspace.yaml` drop `strictDepBuilds: false` entirely (relying on the per-build regenerate), or keep it as an inert first-copy default that the generate step always overwrites? | dev | open — spec assumes the generate step is authoritative and removes the static false. |
