# Triage Report — SVY-21173

**Verdict:** PROCEED

> **Outcome (post-implementation).** Both recommended fixes were implemented and
> verified, plus a third defect found during verification. The final design — including
> two places where the approach below turned out not to work as planned — is documented
> in `docs/SVY-21173-cypress-runner-reopen-fixes.spec.md`. Deltas from this report:
>
> 1. **Auto-create placement.** This report and the initial spec put the auto-create call
>    in `exportActiveSolution()`. That is too late: the build's problem-marker check runs
>    first and fails with "table is not accessible", returning before
>    `exportActiveSolution()` is reached. A new `afterSolutionActivated()` hook was added
>    to `AbstractWorkspaceExporter` (no-op default) and the call moved there.
> 2. **`EclipseDatabaseUtils` is unusable headlessly.** Approach 2 below assumed the fix
>    could "reuse existing, already-hardened logic
>    (`EclipseDatabaseUtils.createNewTableFromColumnInfo`)". It cannot: that class lives in
>    `com.servoy.eclipse.core`, whose activator refuses to start when
>    `ModelUtils.setUIDisabled(true)` is in effect (every headless exporter sets it), so
>    merely loading the class throws `NoClassDefFoundError`. A minimal, headless-safe
>    table creator using only `servoy_shared` APIs was written inside
>    `CypressFormTestRunner` instead. `EclipseDatabaseUtils` is untouched.
> 3. **Third and fourth defects (not in this report).** Every form spec after the first
>    failed with `Expected to find element: [data-cy^="<form>."]` — each spec silently got
>    the solution's `firstFormID` instead of its own form, which is what made the reopen's
>    symptoms look partly like "db issues". This turned out to be **two** independent bugs
>    with the same symptom, in two separate WebSocket session factory registrations:
>    - *Headless runner:* `CypressFormTestRunner.activateNgClientBundle()`'s `instanceof`
>      guard always bailed out, so the runner's formpreview factory was never installed.
>      Guard removed.
>    - *Servoy Developer UI:* `com.servoy.eclipse.ngclient.startup.Activator`'s factory had
>      `nodebug` and `svy_developer` branches but no `formpreview` branch. Branch added.
>
>    Fixing the first did nothing for the second — after that fix the headless runner was
>    6/6 while Developer was still 1/6 on the same specs with the same error.
>
> Verified in both paths: headless `Total: 6 | Passed: 6 | Failed: 0` / `Export DONE.`;
> Developer `Runs: 6/6, Failures: 0, Errors: 0`.

## Reported problem

SVY-21173 asks for a dedicated headless Servoy Developer application that opens a
workspace, discovers Cypress form tests, runs them, and reports pass/fail — usable as
a CI/Jenkins build step. This was implemented as `com.servoy.eclipse.cypress`'s
`CypressFormTestRunner` (extends `AbstractWorkspaceExporter`, same base class as the
WAR/solution/mobile exporters) and the ticket was closed.

It was **reopened on 2026-09-28** by Marius Muntean with two bundled symptoms:

1. "db issues"
2. A crash on shutdown:
   ```
   java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
     at org.eclipse.core.internal.resources.SaveManager.forEachProjectInParallel(SaveManager.java:1906)
     at org.eclipse.core.internal.resources.SaveManager.visitAndSave(SaveManager.java:1861)
     at org.eclipse.core.internal.resources.SaveManager.save(SaveManager.java:1297)
     at org.eclipse.core.internal.resources.Workspace.save(Workspace.java:2549)
     at org.eclipse.core.internal.resources.Workspace.save(Workspace.java:2538)
     at com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter.start(AbstractWorkspaceExporter.java:204)
   ```

A separate, earlier investigation session (treated here as candidate evidence, not
fact) attributed the "db issues" part to an in-memory HSQLDB server (`servoy_test`,
`jdbc:hsqldb:mem:.`) whose `orders` table is never auto-created headlessly, because the
auto-create-from-`.dbi` flow is UI-only and skipped in headless mode.

These are two **independent** problems bundled into one reopen comment. Neither is
the "proposed solution" from the original ticket (which only asked for the runner to
exist) — this is a reopen due to newly discovered defects in the implementation, not a
challenge to the original feature request.

## Root-cause assessment

### 1. `ArrayIndexOutOfBoundsException` in `SaveManager.forEachProjectInParallel`

This is a **confirmed upstream Eclipse Platform bug**, not a defect in Servoy code.

- `AbstractWorkspaceExporter.start()` line 204 calls
  `ResourcesPlugin.getWorkspace().save(true, null)` inside a `finally` block, after
  `ApplicationServerRegistry.get().doNativeShutdown()`. This call is identical across
  all `AbstractWorkspaceExporter` subclasses (WAR exporter, solution exporter, mobile
  exporter, and the new Cypress runner) — it is inherited from the shared base class
  and none of the sibling exporters do anything different or add any workaround for it.
- `org.eclipse.core.internal.resources.SaveManager.forEachProjectInParallel` (current
  Eclipse Platform `master`, fetched directly from
  `eclipse-platform/eclipse.platform`) does:
  ```java
  private void forEachProjectInParallel(IProgressMonitor m, CoreConsumer<IProject> consumer) throws CoreException {
      IProject[] projects = workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
      ...
      stats = executor.submit(() -> Arrays.stream(projects).parallel().map(project -> {
          ...
          return Status.error("Error with project " + project.getName(), e);
      }).filter(Objects::nonNull).toArray(IStatus[]::new)).get();
      ...
      if (stats.length == 1) {
          throw new CoreException(stats[0]);
      }
  ```
  This is exactly the method and line pattern in the reopen's stack trace
  (`SaveManager.java:1906` in `forEachProjectInParallel`, called from `visitAndSave`
  at `SaveManager.java:1861`, called from `save` at `SaveManager.java:1297`).
- **Eclipse Platform issue
  [eclipse-platform/eclipse.platform#2869](https://github.com/eclipse-platform/eclipse.platform/issues/2869),
  "ArrayIndexOutOfBoundException is thrown from SaveManager class"**, opened
  2026-08-12, has the *identical* stack trace shape (`forEachProjectInParallel` →
  `visitAndSave` → `save` → `Workspace.save`) and was reproduced against Eclipse
  4.36. It was fixed by
  [PR #2870](https://github.com/eclipse-platform/eclipse.platform/pull/2870),
  merged 2026-08-12, targeting milestone **4.41 M3**. The one-line fix:
  ```diff
  -    throw new CoreException(stats[1]);
  +    throw new CoreException(stats[0]);
  ```
  i.e. the method had an off-by-one bug: when exactly one project's save failed
  (`stats.length == 1`), it indexed `stats[1]` instead of `stats[0]`, which throws
  `ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1` — the exact
  message in the reopen comment.
- This means the **real underlying condition** is that at least one project's save
  callback (`visitAndSave`) itself threw a `CoreException` — the array-index bug is
  merely the buggy *error-reporting path* that fires when Eclipse tries to report that
  underlying per-project save failure. The `ArrayIndexOutOfBoundsException` masks
  whatever the original per-project save error was.
- This project's target platform (`launch_targets/com.servoy.eclipse.target.target`)
  pins Eclipse **2025-12**, which predates the 4.41 M3 fix (still in early-2026
  milestones at time of target selection) — so the workspace this project builds
  against still has the buggy `SaveManager`.
- `git log`/`git blame` on `AbstractWorkspaceExporter.java` shows one prior incident of
  a similar class of problem: commit `ba1234019b` ("Trying to fix an exception on
  Jenkins", 2026-05-25) fixed a *different* Eclipse resources exception
  (`ResourceException: Invalid project description ... overlaps the location of
  another project`) that also originated from concurrent/duplicate project handling
  during `importExistingAndOpenClosedProjects`. That precedent confirms this exporter
  family has hit multiple distinct Eclipse-workspace edge cases before, usually tied to
  project state at save/import time, and the existing fix pattern was to correct
  Servoy-side project-import logic, not touch `SaveManager` itself (which is out of
  Servoy's control).
- The Cypress runner does not do anything structurally different from the WAR/solution
  exporters in `checkAndExportSolutions`/`importExistingAndOpenClosedProjects` — it
  overrides `checkAndExportSolutions` only to install a driver-aware classloader
  before delegating to `super.checkAndExportSolutions(configuration)`
  (`CypressFormTestRunner.java:87-91`). It does not add, remove, or close any
  additional projects beyond what the base class already does. So there is nothing
  Cypress-runner-specific that would make it more likely to trip this bug than the WAR
  exporter — except that the Cypress runner is a newer, less-hardened code path being
  exercised for the first time in this environment, so it's the first exporter to
  surface a pre-existing Eclipse-side project-state edge case.

### 2. HSQLDB in-memory `servoy_test` / missing `orders` table

This part is **not verifiable from this repository** and appears to be a **test
environment/data provisioning issue, not a Servoy code bug** — with one caveat noted
below.

- Searched this repo for `servoy_test`, `testcypressstuff`, `hsqldb:mem`, and
  `server.5.URL` — **zero matches**. The `servoy.properties` file and the `orders`
  table/`.dbi` referenced in the other investigation session do not exist anywhere in
  this checkout; they belong to the reporter's local/CI test environment
  (`E:\ServoyInstalls\...\workspace`), not to source under version control here. This
  means the specific claim "these tables are never populated" cannot be independently
  confirmed against this repo's contents — it can only be evaluated structurally
  against the runner's code path.
- The auto-create-from-`.dbi`-cache mechanism the other session described does exist,
  but it lives **only** in `com.servoy.eclipse.core.ServoyModel.updateResources()`
  (`ServoyModel.java:1552-1599`):
  ```java
  // auto create in mem server's tables if dbis are available (very useful for working with test in mem DBs)
  try {
      if (server.getConfig().isInMemDriver() && !IServer.INMEM_SERVER.equals(server.getConfig().getServerName()) &&
          server.getTableNames(true).size() == 0) {
          IFolder serverInformationFolder = dataModelManager.getServerInformationFolder(server.getName());
          ...
          EclipseDatabaseUtils.createNewTableFromColumnInfo(server, tableName, dbiFileContent, EclipseDatabaseUtils.NO_UPDATE);
      }
  } catch (Exception e) { ServoyLog.logError(e); }
  ```
  This block runs inside `updateResources()`, called when the **active resources
  project** changes — i.e. it is IDE/model-driven (`ServoyModel` is
  `com.servoy.eclipse.core`'s live interactive model used by Developer). The
  introducing commit (`82d612efb4`, 2026-01-21, "Implemented/fixed SVY-20327...") shows
  this exact auto-create-from-dbi logic was hardened at that time (added `try/catch`,
  added `server.getTableNames(true).size() == 0` guard) but was already present before
  that in some form — it is a long-standing Developer-only convenience for in-memory
  test DBs.
- The headless Cypress runner (and every other `AbstractWorkspaceExporter` subclass —
  WAR, solution, mobile) uses `com.servoy.eclipse.exporter.apps.common.ExportServoyModel`
  instead, which extends the same `AbstractServoyModel` base but has its own
  `initialize()`/`setActiveResourcesProject()` (`ExportServoyModel.java:47-111`) with
  **no equivalent auto-create-from-dbi block at all**. This is a genuine, confirmed
  structural gap: `ExportServoyModel` was never designed to auto-populate in-memory
  test databases from `.dbi` cache, because none of the existing export apps (WAR,
  solution export) need that — they export what's already in the repository/model, they
  don't need a live, queryable in-memory table.
- The Cypress form test runner is different: `HeadlessFormTestExecutor` needs the NG
  client to actually render the form live against a real (even if in-memory) database,
  so a missing backing table is a real functional blocker for it — unlike for the WAR
  exporter, which has never needed this.
- **Conclusion for this half:** the "not accessible" behavior is real and is a
  functional gap in the Cypress runner (or the recommended docs/setup for it), not an
  Eclipse or exporter-framework bug, and not something that magically works for the
  other exporters because they never needed it. Whether this counts as "our code should
  auto-create it" vs. "test environment should be pre-provisioned with real
  data/schema" is a design question — see Approaches below. It is plausible this is
  simply a test-environment setup gap (the reporter's `servoy_test` HSQLDB server
  was never given the `orders` table before running), which would make it a
  provisioning issue rather than a runner defect — but because the Cypress runner is
  the first `AbstractWorkspaceExporter` subclass that actually needs live table access
  against in-memory test databases, there is also a legitimate argument that the
  runner should replicate `ServoyModel`'s auto-create convenience so headless CI runs
  don't require a manually pre-seeded schema.

## Ticket premise check

The original ticket's proposed solution (build a dedicated headless runner extending
the exporter framework) is not being challenged here — that part is done and
functionally sound. The reopen doesn't propose a specific fix; it just reports two
crashes/issues. Assessing each:

- **`ArrayIndexOutOfBoundsException`**: Not fixable in Servoy code directly — this is a
  bug inside `org.eclipse.core.resources` itself (already fixed upstream, but the fix
  lands in Eclipse 4.41, later than the project's currently pinned 2025-12 target
  platform). Any Servoy-side "fix" must work around the trigger condition (an
  underlying per-project save failure) rather than the crash itself, since the crash is
  Eclipse's own error-reporting code failing while reporting a different error.
- **HSQLDB missing table**: There is no ticket-proposed solution to check — the
  original ticket never mentions databases at all. The "fix" direction here is an open
  design question (see Approaches), not a premise to validate.

## Approaches considered

### For the `ArrayIndexOutOfBoundsException`

1. **Wrap `ResourcesPlugin.getWorkspace().save(true, null)` in `AbstractWorkspaceExporter.start()`'s
   finally block with broader exception handling, and add diagnostic logging for the
   underlying per-project save failure.** The current code already catches
   `CoreException` there (line 206-209), but `ArrayIndexOutOfBoundsException` is a
   `RuntimeException`, not a `CoreException`, so it propagates uncaught out of `start()`.
   Catching `Throwable` (or at least `RuntimeException`) around this specific call
   would prevent the crash from aborting/corrupting the runner's exit code and let the
   real underlying save error (if logged elsewhere, e.g. via `ServoyLog`) be diagnosed.
   Pros: minimal, safe, immediately deployable, doesn't wait on an Eclipse upstream
   release; also protects the WAR/solution/mobile exporters from the same latent crash.
   Cons: it's a workaround, not a root-cause fix; the underlying per-project save
   failure (whatever caused `visitAndSave` to throw in the first place) is still
   swallowed/logged only generically unless additional diagnostics are added.
2. **Upgrade the target platform to an Eclipse release that includes the
   `SaveManager` fix (≥ 4.41 M3).** Pros: fixes the actual bug at the source. Cons:
   4.41 M3 was not yet released at ticket time (fix merged 2026-08-12, this project is
   pinned to 2025-12); a full target-platform bump is a large, high-blast-radius change
   affecting the entire IDE build, not something to do just for this one runner. Not
   proportionate to the problem.
3. **Investigate and fix whatever underlying per-project save failure is triggering
   `visitAndSave` to throw in the first place** (the real bug the array-index bug is
   masking). Pros: addresses the actual defect. Cons: requires reproducing the crash
   with a build with better logging (approach 1) first, since currently the true
   error is invisible; can't be diagnosed further from static code inspection alone.
4. **No code change** — treat it as a known, already-fixed-upstream Eclipse bug and
   just document it / wait for target platform to naturally advance past 2025-12 in a
   future release cycle. Pros: zero effort now. Cons: the runner will keep crashing on
   affected CI machines until the target platform is bumped, which could be a long
   wait; poor experience for a newly shipped feature.

### For the HSQLDB / missing table issue

1. **Document that the Cypress form test runner requires databases to already have
   their tables provisioned (mirroring Developer's manual "create missing tracked
   tables" prompt) before running headlessly** — i.e. treat this as user/CI
   environment setup, not a code defect. Pros: zero code risk; consistent with how the
   WAR/solution exporters have always behaved (they never auto-create tables either).
   Cons: leaves a real headless-CI usability gap — anyone standing up a fresh in-memory
   test DB for Cypress form tests will hit this every time, undermining the "runs in
   CI easily" goal of the original ticket.
2. **Add the same auto-create-from-`.dbi`-cache convenience that `ServoyModel` has to
   `ExportServoyModel` (or specifically to the Cypress runner's initialization path),
   gated the same way** (`isInMemDriver() && tableNames.size() == 0`). Pros: makes
   headless CI runs "just work" against a throwaway in-memory DB the same way
   Developer already does interactively; directly unblocks the reported "db issues";
   small, well-scoped, reuses existing, already-hardened logic
   (`EclipseDatabaseUtils.createNewTableFromColumnInfo`). Cons: changes shared
   `ExportServoyModel` behavior for all exporters (WAR/solution/mobile too), which
   could have side effects nobody has asked for on those other paths; alternatively,
   the equivalent block could be added narrowly inside `CypressFormTestRunner`
   (e.g. in `checkAndExportSolutions` override, before `super.checkAndExportSolutions`)
   to scope it to only the new runner.
3. **No code change** — this may simply be a test-environment provisioning gap (the
   reporter's local/CI `servoy_test` HSQLDB instance was never seeded with the `orders`
   table before the run), not a Servoy code defect at all. Pros: correctly named — this
   really might not be "our bug". Cons: cannot be fully confirmed without reproducing in
   the reporter's actual environment, since the relevant `servoy.properties`/`.dbi`
   files aren't in this repo to inspect.

## Recommendation

**PROCEED** on the `ArrayIndexOutOfBoundsException`, with **approach 1** (defensive
catch + diagnostics around `ResourcesPlugin.getWorkspace().save(true, null)` in
`AbstractWorkspaceExporter.start()`'s finally block) as the immediate fix. This is a
small, safe, proportionate change: it stops an upstream Eclipse bug from crashing the
runner (and its siblings) while surfacing the real underlying per-project save error
for follow-up diagnosis. Approach 3 (root-cause the actual per-project save failure)
should be attempted as a fast-follow once better logging is in place, but should not
block this fix, since the crash currently prevents any diagnostics from being captured
at all. Approach 2 (target-platform bump) is out of proportion for this ticket and
should not be pursued here.

For the HSQLDB/missing-table issue, **PROCEED cautiously with approach 2, scoped
narrowly to `CypressFormTestRunner`** rather than the shared `ExportServoyModel` (to
avoid changing behavior for the WAR/solution/mobile exporters, which have never needed
or wanted this). This is the option that actually satisfies the original ticket's
stated goal ("can be triggered as a build step in Jenkins" implies it should work
against a disposable/fresh test DB without manual pre-seeding). However, this should be
confirmed with the reporter first: if the `servoy_test` HSQLDB server in their
environment was simply never provisioned with the `orders` table by their own test
setup scripts (i.e. purely environmental), approach 1 (documentation) may be sufficient
and less invasive — see Questions below.

Both fixes are independent and can ship separately; neither blocks the other.

### Recommendation as implemented

Both recommendations were followed. Two details differ from what is written above —
see the Outcome note at the top of this report for the full explanation:

- Approach 2's assumption that `EclipseDatabaseUtils.createNewTableFromColumnInfo` could
  be reused does not hold headlessly; a `servoy_shared`-only equivalent was written
  inside `CypressFormTestRunner`.
- The auto-create call had to run before the build's problem-marker check, not inside
  `exportActiveSolution()`, so a new `afterSolutionActivated()` hook was added to
  `AbstractWorkspaceExporter`.

The "confirm with the reporter first" caveat on approach 2 is moot: the server is
`jdbc:hsqldb:mem:.`, which is recreated empty on every JVM start, so no external
provisioning step could persist tables between runs. Auto-create is the only workable
option and approach 1 (documentation) would not have helped.

## Git history findings

- `AbstractWorkspaceExporter.java` — commit `ba1234019b` (2026-05-25, "Trying to fix an
  exception on Jenkins") previously fixed a different Eclipse `ResourceException`
  (`Invalid project description ... overlaps the location of another project`) in
  `importExistingAndOpenClosedProjects`, in the same finally-block-adjacent code region
  as the current crash. Precedent shows this exporter base class has repeatedly needed
  small defensive fixes for Eclipse workspace/project edge cases surfaced by Jenkins CI
  runs specifically (not by interactive Developer use).
- `ServoyModel.java` — commit `82d612efb4` (2026-01-21, "Implemented/fixed SVY-20327...")
  hardened the in-memory-DB auto-create-from-`.dbi` block (added guard
  `server.getTableNames(true).size() == 0` and wrapped in try/catch). Confirms this
  mechanism is intentionally Developer-only convenience code, not something ever ported
  to the exporter/headless model classes.
- `CypressFormTestRunner.java` / Cypress plugin — commits `f51f46bf4e` ("extract Cypress
  testing into standalone plugin") and `df026adfa3` ("backport cypress test module and
  app ID fix to lts_2026") are the only history; no prior fix attempts for either
  reported symptom exist yet in this codebase.
- Eclipse Platform upstream: issue
  [eclipse-platform/eclipse.platform#2869](https://github.com/eclipse-platform/eclipse.platform/issues/2869)
  and its fix, [PR #2870](https://github.com/eclipse-platform/eclipse.platform/pull/2870)
  (merged 2026-08-12, targeting 4.41 M3) — confirms the `ArrayIndexOutOfBoundsException`
  is a known, already-fixed, off-by-one bug (`stats[1]` vs `stats[0]`) in
  `SaveManager.forEachProjectInParallel`, not something introduced by Servoy code.

## Questions for the reporter

Not applicable — verdict is PROCEED, not NEEDS_INPUT. One clarification was noted here
as potentially sharpening the HSQLDB fix scope: whether the `servoy_test` server's
`orders` table was created by the reporter's own provisioning scripts, or whether the
runner was expected to auto-create it from `.dbi` cache.

**Resolved without needing the reporter.** The server is configured as
`server.7.URL=jdbc:hsqldb:mem:.` (`servoy.properties`), i.e. in-memory and recreated
empty on every JVM start — no external provisioning step can persist tables across runs,
so auto-create is the only workable option. Confirmed by the verified green run.

## Follow-ups identified during implementation

Neither is part of this reopen; both warranted separate tickets. #2 has since been filed
as SVY-21529 and is already resolved on this branch; #1 is still open.

1. **Two duplicate WebSocket session factory registrations both handling `formpreview`.**
   `CypressFormTestRunner.activateNgClientBundle()` and
   `com.servoy.eclipse.ngclient.startup.Activator` each register a factory for
   `WebsocketSessionFactory.CLIENT_ENDPOINT`, and both now need their own `formpreview`
   branch (that divergence is precisely what produced defects 3 and 4). They behave
   identically today, but the duplication is a latent source of the same class of bug.
   Worth consolidating into a single registration.
2. **`DataAdapterList` off-event-thread warnings during `FormPreviewNGClient` teardown** —
   tracked as **SVY-21529**, already resolved on this branch. `Unexpected execution outside
   of the event dispatch thread in DAL code` was logged (and swallowed) ~20 times per
   6-spec run from `FormPreviewNGClient.shutdownExisting()` and its constructor, because
   teardown ran on `main`/`http-nio` threads rather than the NG event dispatch thread.

   Fixed by commit `97536e3a89`, which took `origin/release`'s `FormPreviewNGClient` and
   ngclient `Activator` onto `lts_2026`. Release's `shutdownOnEventThread()` /
   `shutdownRouting()` posts `shutDown(true)` onto the old client's own event dispatcher
   (10s cap, direct call when there is no live dispatcher or already on its event thread),
   so DAL teardown runs where `checkThatThisIsTheEventThread()` expects. Release also
   reuses and retargets an existing preview client on the same websocket session rather
   than tearing it down and rebuilding, which removes most of the teardowns entirely.

   Verified: a full 6-spec headless run after `97536e3a89` produces **zero** such traces
   (still 6/6, `Export DONE.`), down from ~20.
