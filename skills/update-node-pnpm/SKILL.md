---
name: update-node-pnpm
description: "Use when updating the bundled Node.js and/or pnpm runtime versions in the Servoy Developer Eclipse IDE (the five com.servoy.eclipse.nodejs.* platform plugins plus the frontend-maven-plugin nodeVersion in the Angular build poms). Bumps Node to the latest patch of the current LTS line (minor/patch bumps applied directly; a new LTS major is first checked against Angular support before proposing), bumps the bundled pnpm to the latest, and updates every place the versions appear: the five plugin.xml node paths, the five MANIFEST.MF + pom <version> (Node), the pnpm.version pom property, the nodeVersion in the three Angular-build poms, and the five feature.xml plugin versions. Triggered by 'update node', 'update pnpm', 'update_node', 'bump node', 'bump pnpm', 'update the bundled node/pnpm', or '/update-node-pnpm'."
---

# Update bundled Node.js & pnpm

You are updating the **Node.js** and **pnpm** runtimes that Servoy Developer bundles and ships. This skill finds the current versions, determines the right target versions (respecting Angular's Node support for any major bump), updates **every** file that carries a version, re-downloads the bundled pnpm binaries, and verifies the result.

Node and pnpm are bumped **together** — see the shared-memory reminder in `.assistai/memory.json` ("SVY-21456 pnpm binary update reminder").

## Run in the current session — do not delegate

Run every phase directly in the invoking session so the user sees each decision (especially the Angular-compatibility confirmation for a major Node bump) inline. Do not spawn a subagent for any phase.

---

## Key facts about this repo's layout

There are **two independent runtime concerns**, both updated by this skill:

1. **The bundled Node.js + pnpm binaries** that ship inside the product — five platform plugins:
   - `com.servoy.eclipse.nodejs.win32.win32.x86_64` (node.zip, pnpm.zip)
   - `com.servoy.eclipse.nodejs.win32.win32.aarch64` (node.zip, pnpm.zip)
   - `com.servoy.eclipse.nodejs.linux.gtk.x86_64` (node.tar.xz, pnpm.tar.gz)
   - `com.servoy.eclipse.nodejs.macosx.cocoa.x86_64` (node.tar.gz, pnpm.tar.gz)
   - `com.servoy.eclipse.nodejs.macosx.cocoa.aarch64` (node.tar.gz, pnpm.tar.gz)

2. **The Node used to build the Angular frontends at Maven build time** (frontend-maven-plugin `install-node-and-npm`) — three poms:
   - `com.servoy.eclipse.ngclient.ui/pom.xml` (**four** `<nodeVersion>` occurrences)
   - `com.servoy.eclipse.designer.rfb/pom.xml` (one)
   - `com.servoy.eclipse.designer.wpm/pom.xml` (one)

> **Critical versioning rule:** the plugin/pom `<version>` AND the `Bundle-Version` of each `com.servoy.eclipse.nodejs.*` plugin is the **Node version** (e.g. `24.21.0`), *not* pnpm's. pnpm's version lives only in the `<pnpm.version>` pom property and the download URL. This mirrors how npm (also not that version) ships inside Node. The `feature.xml` plugin `version="…"` entries for the five nodejs plugins are also the **Node version**.

The Node version string also appears **inside the extraction path** in each `plugin.xml`:
`nodePath="node-v<NODE>-<platform>/..."` and `npmPath="node-v<NODE>-<platform>/..."`. These MUST match the new Node version or the runtime cannot find node.

pnpm's version is NOT in the extraction path — `pnpmPath` is just `pnpm.exe`/`pnpm`. A `.pnpmgenerated` marker stores the pnpm version token at runtime and the extraction dir is `pnpm-<token>/`, so a pnpm bump self-heals extraction; nothing in the committed files needs the pnpm version other than the pom property + URL.

---

## Phase 0 — Read the current versions

Read these to establish the baseline (use file-reading / grep tools):

- Current **Node**: `grep` for `nodeVersion` in `com.servoy.eclipse.ngclient.ui/pom.xml` → `v<NODE>`; cross-check `<version>` in any `com.servoy.eclipse.nodejs.*/pom.xml`.
- Current **pnpm**: `grep` for `<pnpm.version>` in any `com.servoy.eclipse.nodejs.*/pom.xml`.
- Current **Angular**: `.dependencies["@angular/core"]` in `com.servoy.eclipse.ngclient.ui/node/package.json`.

Report the three current versions to the user.

---

## Phase 1 — Determine target versions

### Node

1. Fetch the Node release index: `https://nodejs.org/dist/index.json` (webfetch, format text).
2. Identify the current Node **major** (e.g. `24`) and its LTS codename (the `lts` field, e.g. `"Krypton"`).
3. Compute two candidates:
   - **Latest patch of the current LTS line** (same major, highest `version`). This is a **minor/patch bump** — apply it directly, no Angular check needed.
   - **Latest LTS major** overall (highest major where `lts` is a non-false string). If this major is **higher** than the current one, it is a **major bump**.
4. Default target = latest patch of the **current** LTS line (safe). Only consider a major bump when the user explicitly asks to move to a new LTS major, OR when the current major is end-of-life.

### pnpm

5. Fetch the latest pnpm version: `https://registry.npmjs.org/pnpm/latest` (webfetch) → `.version`. pnpm minor/patch bumps are applied as-is. For a pnpm **major** bump, skim the pnpm release notes for breaking changes that affect `install`/`add`/`update`/`run` verb behavior, lockfile format, or `pnpm-workspace.yaml` keys (the Servoy pnpm runner in `com.servoy.eclipse.ngclient.ui` depends on these — see `docs/SVY-21456-*` and `docs/SVY-21502-*`), and flag anything risky to the user before proceeding.

### Angular compatibility gate (MAJOR Node bump only)

6. If (and only if) proposing a **new Node major**, verify Angular supports it before doing anything else:
   - Check Angular's official Node support matrix: `https://angular.dev/reference/versions` (webfetch). Confirm the repo's Angular major (from Phase 0) lists the target Node major as supported.
   - If Angular does **not** yet support the target Node major, **stop** and tell the user: stay on the current LTS line's latest patch instead. Do not bump to an unsupported major.
   - Also sanity-check the frontend toolchain's declared engines if in doubt (`@angular/build`, `@angular/cli` `engines.node` in the installed package, or their npm metadata).

7. **Present the proposed targets** (Node `old → new`, pnpm `old → new`, whether it is a minor or major bump, and the Angular-support finding for a major) and get the user's confirmation before editing anything.

---

## Phase 2 — Apply the version changes

Let `NODE` = new Node version (e.g. `24.22.0`), `PNPM` = new pnpm version (e.g. `12.14.0`).

**All files below are inside Eclipse projects → edit them with `tools["eclipse-coder"].replaceString` (via `execute`), NOT the built-in edit/write.** (`feature.xml` lives in the `com.servoy.eclipse.feature` project, so it too goes through `eclipse-coder`.)

### 2a. The five `com.servoy.eclipse.nodejs.*` plugins

For **each** of the five plugins, update:

| File | What to change |
|------|----------------|
| `pom.xml` | `<version>OLD_NODE</version>` → `<version>NODE</version>` (the Node version); `<pnpm.version>OLD_PNPM</pnpm.version>` → `<pnpm.version>PNPM</pnpm.version>` |
| `META-INF/MANIFEST.MF` | `Bundle-Version: OLD_NODE` → `Bundle-Version: NODE` |
| `plugin.xml` | both `nodePath` and `npmPath` — replace the `node-v OLD_NODE -<platform>` path segment with `node-v NODE -<platform>` (keep the per-platform suffix: `-win-x64`, `-win-arm64`, `-linux-x64`, `-darwin-x64`, `-darwin-arm64`) |

The Node download URL in each pom uses `v${project.version}` so it follows `<version>` automatically — no URL edit needed for Node. The pnpm URL uses `v${pnpm.version}` so it follows the property — no URL edit needed for pnpm either.

### 2b. The three Angular-build poms (frontend-maven-plugin)

Replace every `<nodeVersion>vOLD_NODE</nodeVersion>` with `<nodeVersion>vNODE</nodeVersion>`:
- `com.servoy.eclipse.ngclient.ui/pom.xml` — **4 occurrences** (main `node`, plus `projects/servoydefault`, `projects/dialogs`, `projects/window`)
- `com.servoy.eclipse.designer.rfb/pom.xml` — 1
- `com.servoy.eclipse.designer.wpm/pom.xml` — 1

Use `replaceString` with `replaceAll`-style repetition or edit each occurrence; verify the count with a grep afterwards.

### 2c. `com.servoy.eclipse.feature/feature.xml`

The five nodejs plugins each have a `<plugin … version="OLD_NODE"/>` entry. Update all five `version="OLD_NODE"` → `version="NODE"`. Match them by the surrounding `id="com.servoy.eclipse.nodejs.*"` so you do not touch unrelated plugin versions (JRE plugins have their own version and must NOT change).

### 2d. Sanity grep

After editing, grep the whole repo for the OLD Node version and the OLD pnpm version. The only remaining hits should be in `docs/*.spec.md`, `.assistai/memory.json`, `target/` build output, and historical references — **no** hit should remain in any `pom.xml`, `plugin.xml`, `MANIFEST.MF`, or `feature.xml`.

---

## Phase 3 — Re-download the bundled binaries

The committed `node.*` and `pnpm.*` archives are produced by the `download-maven-plugin` during `generate-resources`. They are **not** normally committed as source — check `git status` to see whether the repo tracks them (the `node.zip`/`node.tar.*` and `pnpm.zip`/`pnpm.tar.gz` files exist in the working tree). 

- **If the archives are NOT git-tracked** (they are build artifacts): nothing to commit for the binaries — the next Maven build re-downloads them for the new version automatically because the URLs are driven by `${project.version}` / `${pnpm.version}`. You only commit the text changes.
- **If the archives ARE git-tracked**: they must be refreshed so the committed binary matches the new version. The cleanest way is to run the download plugin for each nodejs plugin so Maven fetches the right files:
  ```
  mvn -q -pl com.servoy.eclipse.nodejs.win32.win32.x86_64,com.servoy.eclipse.nodejs.win32.win32.aarch64,com.servoy.eclipse.nodejs.linux.gtk.x86_64,com.servoy.eclipse.nodejs.macosx.cocoa.x86_64,com.servoy.eclipse.nodejs.macosx.cocoa.aarch64 generate-resources
  ```
  (run from the repo root; this only runs the `wget` executions). Then `git status` should show the refreshed archives. If the machine cannot download, tell the user the binaries must be refreshed on a build machine / CI.

Confirm with `git status` which of the two cases applies before acting.

---

## Phase 4 — Verify

1. **Grep check** (Phase 2d) — no stale version left in source files.
2. **Count check** — confirm `nodeVersion` appears the expected number of times with the new value:
   - `com.servoy.eclipse.ngclient.ui/pom.xml`: 4
   - `com.servoy.eclipse.designer.rfb/pom.xml`: 1
   - `com.servoy.eclipse.designer.wpm/pom.xml`: 1
   - five nodejs `pom.xml` `<version>`: 1 each
   - five `MANIFEST.MF` `Bundle-Version`: 1 each
   - five `plugin.xml`: nodePath + npmPath both updated
   - `feature.xml`: 5 nodejs plugin `version` entries
   - five `<pnpm.version>`: 1 each
3. **Build check (optional but recommended):** a full product build is expensive. A lighter verification is to run the Angular frontend install/build for one project to confirm the new Node works with the toolchain, e.g. from `com.servoy.eclipse.ngclient.ui/node` run `npm ci` + `npm run build_lib`. For the OSGi side, if the user wants it, run the Tycho build of the feature. State clearly what you verified and what you did not (a full `mvn -Pproduct` build that downloads all five platform binaries is usually left to CI).

---

## Phase 5 — Commit

Ask the user before committing (per repo git-safety rules — permission is per commit).

- Stage the edited text files (the five nodejs plugin `pom.xml`/`plugin.xml`/`MANIFEST.MF`, the three Angular poms, `feature.xml`) and, if git-tracked, the refreshed binaries.
- Suggested subject (include the case if the user names one, and the `[ai]` suffix for AI-generated changes):
  `bump bundled Node to <NODE> and pnpm to <PNPM> [ai]`
  or, scoped to a case: `SVY-XXXXX bump bundled Node to <NODE> (LTS) and pnpm to <PNPM> [ai]`
- Show the staged file list and the message, then wait. **Do not push** without a separate explicit yes.

---

## Reference: every place a version lives

Node version (`NODE`):
- `com.servoy.eclipse.nodejs.*/pom.xml` → `<version>` (×5)
- `com.servoy.eclipse.nodejs.*/META-INF/MANIFEST.MF` → `Bundle-Version` (×5)
- `com.servoy.eclipse.nodejs.*/plugin.xml` → `nodePath` + `npmPath` path segment `node-v<NODE>-<platform>` (×5 files, 2 each)
- `com.servoy.eclipse.feature/feature.xml` → `version="<NODE>"` on the 5 nodejs `<plugin>` entries
- `com.servoy.eclipse.ngclient.ui/pom.xml` → `<nodeVersion>v<NODE></nodeVersion>` (×4)
- `com.servoy.eclipse.designer.rfb/pom.xml` → `<nodeVersion>` (×1)
- `com.servoy.eclipse.designer.wpm/pom.xml` → `<nodeVersion>` (×1)

pnpm version (`PNPM`):
- `com.servoy.eclipse.nodejs.*/pom.xml` → `<pnpm.version>` property (×5); the download URL uses `${pnpm.version}` so it follows automatically.

Not a version source (do not edit for a version change): `pnpmPath` (`pnpm.exe`/`pnpm`), `archive`/`pnpmArchive` file names, `build.properties`, the JRE plugin versions in `feature.xml`.

---

## Versioning & future updates

- If a new platform plugin is added (new OS/arch), add it to Phase 2a and the feature.xml list.
- If the Angular build stops using frontend-maven-plugin, revisit Phase 2b.
- Keep the `.assistai/memory.json` "SVY-21456 pnpm binary update reminder" in sync if the layout changes.
