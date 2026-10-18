# Triage Report — SVY-21502

**Verdict:** PROCEED

## Reported problem

With the opt-in pnpm package-manager runner (SVY-21456), pnpm 11+ turns the old
npm-era "Ignored build scripts" warning into a **hard install failure**
(`ERR_PNPM_IGNORED_BUILDS`, exit 1) via its `strictDepBuilds` setting: any dependency
that ships an `install`/`postinstall` build script must be explicitly approved, or pnpm
refuses to complete `install`/`add`/`update`/`remove`.

SVY-21456 only shipped a **stopgap**: `RunPNPMCommand` demotes `strictDepBuilds` back to
a warning for every package-mutating verb, so ignored build scripts no longer fail the
build. The consequence is that those build scripts are **never run at all**. That is fine
for the TiNG client build today (the ignored scripts — `esbuild`, `@parcel/watcher`,
`lmdb`, `msgpackr-extract` — are launcher shims or optional native optimizations with
working fallbacks), but it is wrong for any component/service package a client installs
(`servoy-bootstrap-components`, `servoy-extra-components`, `servoy-nggrids`, or a
customer's own package) that genuinely needs a native build step.

The ticket's requested behaviour: a developer must be able to review and
approve/deny the build scripts for their current solution setup; that approval must be
**persisted in the solution** (committed to git); and the **headless WAR export** must
read that approval and feed pnpm an explicit `onlyBuiltDependencies` list so genuinely
needed native build scripts run unattended in the cloud.

## Root-cause assessment

The behaviour is **not a Servoy bug** — it is intended pnpm 11+ behaviour, and the
current Servoy code is a deliberately-labelled stopgap. The real gap is a **missing
feature**: Servoy has no per-solution concept of an approved build-script allow-list, and
no code to translate such a list into pnpm's `onlyBuiltDependencies`.

Evidence:

- The stopgap lives in `com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/RunPNPMCommand.java`,
  `addUnblockDepBuildsFlag(...)` (line ~454), which appends
  `--config.strictDepBuilds=false` to every package-mutating verb
  (`install`/`add`, `ci`, `update`, `remove`) in `translateToPnpmArguments(...)`. Its
  Javadoc explicitly names this case as the follow-up: *"Until proper per-solution
  approval of dependency build scripts is designed ... tracked as a separate case."*
- The ticket's comment (Johan Compagner, 2026-09-24) corrects a detail in the premise:
  the quick unblock is **not** reliably a CLI flag; the shipped mechanism also sets the
  env var `pnpm_config_strict_dep_builds=false` on the pnpm `ProcessBuilder`. A grep of
  the current tree finds the **`--config.strictDepBuilds=false` argument form** in
  `RunPNPMCommand` but **no `pnpm_config_strict_dep_builds` env-var write** — i.e. the
  shipped code uses the argument form on the package-mutating verbs, and the solution-local
  `node/pnpm-workspace.yaml` additionally carries a static `strictDepBuilds: false`.
  Either way, the net effect today is "scripts ignored, not run", which this case must
  replace.
- The command runner is reached from `WebPackagesListener` and `NodeFolderCreatorJob`
  via `Activator.createNPMCommand(...)` (gated on `Activator.isPnpmMode()`), the same
  install/`ci`/`update`/`run build` path used by both the Developer build and the headless
  WAR export (the WAR export reuses `WebPackagesListener` with a `warExportModel`).
  `IRunNPMCommand.setExtraEnvironment(Map)` already exists as a seam for passing config to
  the process, which the new mechanism can use.
- Persistence target: SVY-21456 made each solution's TiNG folder a **standalone pnpm
  project** — it carries the full core `package.json` plus a solution-local
  `node/pnpm-workspace.yaml` (`nodeLinker: hoisted`, `shamefullyHoist: true`,
  `strictDepBuilds: false`), copied from the shipped
  `com.servoy.eclipse.ngclient.ui/node/pnpm-workspace.yaml`. That standalone layout is the
  natural home for a generated `onlyBuiltDependencies` list, but the solution dir itself is
  a generated/`target/` artifact — **not** the git-committed source — so the *persisted,
  committed* allow-list and the *generated pnpm config* are two distinct things that must
  be wired together.

## Ticket premise check

The premise holds. This is a legitimate follow-up to a self-declared stopgap, and the
proposed shape — Developer UI approval → persisted-in-solution allow-list → WAR export
generates `onlyBuiltDependencies` — is the correct pnpm mechanism (`onlyBuiltDependencies`
is pnpm's explicit, non-interactive allow-list; the npm-era `allowScripts` key in the
shipped `package.json` files does nothing under pnpm, as the comment notes).

Two premise refinements worth flagging to the implementer (not blockers):

1. **The whole pnpm mode is still opt-in and unreleased** (`servoy.jsRuntime=pnpm`;
   npm remains the default). So this is R&D on an unreleased path, not a production
   regression — the urgency is "before pnpm mode is promoted to default", not "hotfix".
2. **"Persist in the solution, committed to git" is under-specified on *where*.** A
   Servoy solution's git-committed sources do not today include the generated
   `target/<solution>/` pnpm project. The committed home could be a solution property /
   meta file, a dedicated file in the solution's media/resources, or a project-level
   setting; the WAR exporter (CLI, headless) must be able to read it **without a running
   workspace UI**. This is a design decision, not an ambiguity that blocks triage — a
   single coherent design exists — but the spec author must pick the committed location
   explicitly and define how `NodeFolderCreatorJob`/`WebPackagesListener` turn it into the
   generated solution `package.json`'s `onlyBuiltDependencies` (and how the WAR export CLI,
   `com.servoy.eclipse.exporter.solution` `WarArgumentChest`, surfaces/consumes it).

## Approaches considered

1. **Full feature as described** — Developer UI review/approve of pending build scripts
   per solution, persist the approved allow-list in a git-committed solution artifact, and
   have both the Developer build and the headless WAR export generate
   `onlyBuiltDependencies` from it (removing/opt-out-gating the `strictDepBuilds=false`
   stopgap).
   - Pros: solves the real problem (native build scripts that are genuinely needed will
     run, unattended, in the cloud); replaces the stopgap with a correct, auditable
     mechanism; matches pnpm's intended model.
   - Cons: largest scope — touches Developer UI, solution persistence model, the pnpm
     command runner, and the WAR export CLI; needs a careful choice of the committed
     persistence location and a migration/compat story for existing solutions.

2. **Config-only, no UI (allow-list file maintained by hand)** — define a committed
   allow-list file/property that developers edit manually (no interactive approval UI);
   both Developer and WAR export generate `onlyBuiltDependencies` from it.
   - Pros: much smaller; delivers the headless-WAR unblock and the correctness fix without
     UI work; still removes the blanket stopgap.
   - Cons: no "review the pending scripts" affordance (the ticket explicitly asks for the
     developer to *validate and approve* via the Developer UI); worse DX; developers must
     know package names/versions by hand.

3. **Keep the stopgap, make it an explicit opt-out only** — leave `strictDepBuilds=false`
   as the behaviour but expose it as a documented per-solution opt-out flag; do not build
   the allow-list.
   - Pros: minimal; the TiNG client build keeps working today.
   - Cons: does **not** solve the stated problem — build scripts still never run, so any
     component/service that genuinely needs a native build step is still broken; only
     defers the real work. The ticket explicitly says the stopgap "must be removed/replaced
     by this case".

4. **No code change** — close as "pnpm mode is opt-in; the stopgap is good enough".
   - Pros: zero effort; nothing is currently broken for the shipped TiNG build.
   - Cons: leaves a known-wrong stopgap in place on a path intended to become the default;
     any customer component with a real build script silently has its build skipped;
     contradicts the author's own stated intent for this case. Not acceptable.

## Recommendation

**PROCEED with Approach 1** (full feature). It is the only approach that satisfies all
three acceptance points (Developer-UI approval, git-committed persistence, unattended WAR
export via generated `onlyBuiltDependencies`) and that properly retires the SVY-21456
stopgap. The author of the case is also the author of the stopgap and explicitly scoped
this follow-up, so there is a single, convergent intent.

**Alternatives if scope must be cut:** Approach 2 (config-only, no UI) is a viable reduced
scope that still delivers the correctness fix and the headless-WAR unblock — acceptable as
a first increment if the Developer-UI approval surface is deferred to its own case.
Approaches 3 and 4 are not recommended because they leave build scripts un-run.

**Key design decision the spec must settle (not a NEEDS_INPUT — a design task):** the exact
git-committed location of the per-solution allow-list and how it is read by the headless
WAR export CLI without a running workspace. Everything else (translating the list into the
solution `package.json` `onlyBuiltDependencies`, wiring through
`createNPMCommand`/`setExtraEnvironment`, and gating/removing the `strictDepBuilds=false`
stopgap in `RunPNPMCommand`) follows from that choice.

## Git history findings

- The stopgap and the whole pnpm runner were introduced in a single commit,
  **9ac34fb579** — *"SVY-21456 bundle pnpm runtime and add pnpm-aware command runner [ai]"*
  (Johan Compagner, 2026-09-28). `git log` on `RunPNPMCommand.java` shows this is the only
  commit touching it; `addUnblockDepBuildsFlag` and the `strictDepBuilds=false` argument are
  original to it and self-labelled as a stopgap for this follow-up case.
- The solution-local `node/pnpm-workspace.yaml` (`nodeLinker: hoisted`,
  `shamefullyHoist: true`, `strictDepBuilds: false`) was added in **23d5af282e** —
  *"SVY-21456 install web packages as tarballs in a standalone pnpm solution [ai]"*
  (same day). This is where the per-solution standalone pnpm layout — the natural host for
  a generated `onlyBuiltDependencies` — was established.
- A prior spec exists for the parent work: `docs/SVY-21456-ship-pnpm-runtime.spec.md`.
  §3.5/§3.7.1 cover the `strictDepBuilds` handling and the solution-local workspace file;
  the drive-mismatch `store-dir` handling (§3.7.2) was a separate follow-up. No `docs/`
  spec exists yet for SVY-21502.
- SVY-21502 is linked **Relates** to SVY-21456. Fix version **2026.12**, type New Feature,
  priority Minor, status In Progress. No regression-between-versions claim — this is new
  behaviour introduced by the (unreleased) pnpm mode, not a regression in a shipped feature.
