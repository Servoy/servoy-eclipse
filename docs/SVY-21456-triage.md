# Triage Report — SVY-21456

**Verdict:** NEEDS_INPUT

## Reported problem

This is not a bug report — it is a **feasibility investigation**. The ticket asks
whether Servoy Developer can *ship and use [Bun](https://bun.sh) instead of the
per-OS/arch Node.js + npm* that it currently bundles.

Today (confirmed in code):

- We ship one platform fragment per OS/arch that carries a Node.js archive:
  `com.servoy.eclipse.nodejs.win32.win32.x86_64`, `.win32.win32.aarch64`,
  `.macosx.cocoa.x86_64`, `.macosx.cocoa.aarch64`, `.linux.gtk.x86_64`. Each declares
  its `node`/`npm` paths and archive via the `com.servoy.eclipse.ngclient.ui.nodejs`
  extension point (e.g. `com.servoy.eclipse.nodejs.win32.win32.x86_64/plugin.xml`,
  currently `node-v24.18.0`).
- `Activator.extractNode()` / `extractPath()`
  (`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/Activator.java:142`,
  `:202`) unzip that archive into the plugin state location under
  `.metadata/.plugins/com.servoy.eclipse.ngclient.ui/…` on first run.
- `RunNPMCommand`
  (`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/RunNPMCommand.java:114`)
  runs commands by spawning `node <npm-cli.js> <args…>` via a `ProcessBuilder`.
- `WebPackagesListener`
  (`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/WebPackagesListener.java:700`)
  is the main caller. It runs a sequence of npm verbs: `run build_lib_debug_nowatch`,
  `install … --legacy-peer-deps`, then either `ci --legacy-peer-deps` or
  `update --legacy-peer-deps`, then `dedup`, followed by a **custom hand-written dedup**
  that compares the solution's `node_modules` against a root `node_modules` and deletes
  duplicate package dirs, and finally `run <build script>`. `NodeFolderCreatorJob` and
  `NGClientStarter` are other callers.

The ticket itself flags the two things it is unsure about:

1. Command compatibility — "most commands are compatible but some like `dedup` are a
   bit different".
2. Whether Bun's **global cache** can be used so we stop having copies of
   `node_modules` everywhere.

## Root-cause assessment

There is no defect to root-cause. The relevant facts for a feasibility call:

- **The npm surface Servoy depends on is small and concrete**: `install`, `ci`,
  `update`, `dedup`, `run <script>` (Angular CLI builds), plus `--legacy-peer-deps`.
  These are the exact strings passed to `createNPMCommand(...)`. Any Bun migration must
  reproduce this exact behaviour set.
- **`dedup` is the real friction point, and it is load-bearing.** Servoy does not just
  call `npm dedup`; it then runs its *own* dedup pass in `WebPackagesListener` (lines
  804–864) that physically deletes duplicate package folders shared between the
  solution's `node_modules` and a root `node_modules`. This whole mechanism exists to
  reduce the "copies everywhere" problem the ticket wants to solve with Bun's global
  cache. Bun does not have a `dedup` verb with the same semantics, and Bun's global
  cache / hardlink model would *replace*, not augment, this custom logic. So "swap npm
  for bun" is not a drop-in change here — the dedup subsystem would have to be
  rethought or removed.
- **`--legacy-peer-deps` is npm-specific.** Bun resolves peer dependencies
  differently and has no identical flag; the reason Servoy needs `--legacy-peer-deps`
  (peer-dep conflicts across the source-included component packages — documented in the
  repo's own "Angular Source Build Failures" notes) would need to be re-validated under
  Bun's resolver.
- **Bun ships as a single self-contained binary per OS/arch**, which maps cleanly onto
  the existing per-platform fragment + extension-point mechanism (`node.exe` →
  `bun.exe`, `npm-cli.js` path → the bun binary). The extraction/`ProcessBuilder`
  plumbing (`Activator.extractPath`, `RunNPMCommand.runCommand`) is largely
  agnostic — it would need the argument-construction changed (today it does
  `node <npm-cli.js> <args>`; with Bun it would be `bun <args>` or `bunx`), and the
  NG-build "finished" detection in `RunNPMCommand` (the `Date:`/`Hash:`/`Time:` output
  sniff, line ~197) may need to change if Bun/Angular output differs.

In short: **the ticket's premise is plausible but not proven, and the answer depends on
product/architecture decisions this triage cannot make** (do we accept dropping the
custom dedup in favour of Bun's cache model? do we require Bun on all five shipped
platforms? do we keep npm as a fallback?). That is the definition of NEEDS_INPUT.

## Ticket premise check

The premise ("we can/should ship Bun instead of Node+npm") **holds up structurally**
(single binary, existing extension-point mechanism fits) but **does not hold up as a
straight swap**:

- The ticket's own claim that "most commands are compatible" is true for the *simple*
  verbs but glosses over the two that matter most here: `dedup` and
  `--legacy-peer-deps`, both of which are entangled with Servoy's custom node_modules
  de-duplication logic.
- The ticket's aside "Angular build/cli … seems to work fine [with Bun]" is an
  *unverified assumption* stated in the ticket, not something this triage could confirm
  without actually running a Bun-driven Angular build of the TiNG client. It is exactly
  the kind of thing the investigation must prove, not assume.
- The ticket does not state whether Bun would *replace* Node/npm entirely (dropping the
  five `com.servoy.eclipse.nodejs.*` fragments) or *coexist*. That scope decision
  changes the work substantially.

## Approaches considered

1. **Full replacement — ship Bun, delete the Node/npm fragments, rewrite
   `RunNPMCommand` arg construction, and drop the custom dedup in favour of Bun's global
   cache.**
   - Pros: biggest payoff on the "copies everywhere" goal; one binary per platform;
     faster installs/builds.
   - Cons: highest risk; must re-prove Angular CLI builds under Bun for the
     source-included component packages (the repo already documents fragile peer-dep
     interactions there); loses/replaces the load-bearing dedup subsystem; must validate
     all five shipped platforms.

2. **Coexistence / opt-in — bundle Bun alongside Node, add a preference or system
   property to select the runner, keep npm as the default fallback.**
   - Pros: lets us measure Bun on real solutions without betting the build on it;
     reversible; the existing `servoy.nodePath`/`servoy.npmPath` override
     (`Activator.java:146`) already hints at a pluggable-runner shape.
   - Cons: two runtimes to ship and maintain; `IRunNPMCommand` abstraction would need a
     Bun implementation; doesn't by itself deliver the global-cache win unless Bun
     becomes the default.

3. **Spike only — a time-boxed proof of concept (Angular TiNG build + a solution's
   package install/build driven by Bun) with no product code committed, to answer the
   open questions with data.**
   - Pros: matches what the ticket literally asks ("investigate"); cheap; produces the
     evidence needed to choose between approaches 1 and 2.
   - Cons: not a shippable outcome on its own; needs a follow-up implementation ticket.

4. **No code change.**
   - Pros: zero risk; Node/npm works today.
   - Cons: leaves the stated pain (duplicated `node_modules`, per-platform Node payload
     size, install/build speed) unaddressed. Not a satisfying answer to an explicit
     investigation request, but honestly on the table if the spike shows Bun breaks the
     Angular build or the dedup model.

## Recommendation

I cannot pick a single approach as a spec because the choice hinges on decisions only
the reporter/architecture can make. The **natural next step is Approach 3 (a time-boxed
spike)** to answer the concrete unknowns, after which Approach 1 or 2 becomes a real
implementation spec. But before any spec is written, the questions below need answers —
so the verdict is NEEDS_INPUT rather than PROCEED.

## Git history findings

- The Node payload is refreshed periodically, not tied to this case — the newest change
  to the platform fragments is commit `73b46503c8` ("node js update", 2026-07-29,
  Gabi Boros), bumping the bundled Node to `node-v24.18.0`.
- The npm command sequence and custom dedup in `WebPackagesListener` were most recently
  touched under **SVY-21284** ("Investigate how to run integration tests faster",
  commits `af87d1794a`, `0cf972fde4`, `6a9b319fa9`, `1824f05927`). That is relevant:
  the current dedup/`--legacy-peer-deps` behaviour is a deliberately tuned mechanism, so
  a Bun migration would be modifying recently-optimised, intentional code rather than
  cleaning up neglected code.
- No prior `docs/SVY-21456-*.spec.md` exists.

## Questions for the reporter (NEEDS_INPUT only)

1. **Scope: replace or coexist?** Should Bun fully replace the bundled Node.js + npm
   (removing the five `com.servoy.eclipse.nodejs.*` platform fragments), or should Bun
   be added alongside Node as a selectable/opt-in runner while npm remains the default
   for now?

2. **The custom dedup is the crux.** Servoy currently runs `npm dedup` **and** a
   hand-written dedup in `WebPackagesListener` that deletes duplicate package folders
   between the solution's and the root `node_modules`. If we adopt Bun's global cache
   (hardlink/symlink) model, are you comfortable **removing that custom dedup logic
   entirely** and relying on Bun's cache? Or must the on-disk layout stay compatible
   with anything downstream (WAR export, solution export, the Cypress/integration-test
   tooling) that expects the current `node_modules` shape?

3. **`--legacy-peer-deps`.** We pass `--legacy-peer-deps` to npm because the
   source-included Angular component packages have peer-dependency conflicts. Is it
   acceptable for the investigation to require aligning those peer deps so Bun's stricter
   resolver works, or must Bun reproduce npm's lenient behaviour without touching the
   component package.json files?

4. **Target platforms & minimum outcome.** Must a first deliverable cover all five
   shipped platforms (win x64/arm64, macOS x64/arm64, linux x64), or is a single-platform
   proof-of-concept an acceptable first milestone? And what does "success" look like for
   this ticket — a committed shippable Bun runtime, or a written spike report with
   benchmarks that then feeds a separate implementation case?

5. **Angular/CLI validation.** The ticket states the Angular build "seems to work fine"
   with Bun. Has that already been verified against the actual TiNG client build
   (`npm run build_debug` and the `build_lib_*` scripts, including the
   source-included external component packages), or is confirming that part of the work
   we should do?
