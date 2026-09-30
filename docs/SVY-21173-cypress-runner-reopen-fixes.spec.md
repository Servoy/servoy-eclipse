# Spec: SVY-21173 — Fix reopen defects in the headless Cypress form test runner

> **Status: implemented and verified in both execution paths.** Against solution
> `test_webcomponents2` (workspace `E:\ServoyInstalls\Svy2026_03_lts2026_30_07_2026\workspace`,
> in-memory HSQLDB server `servoy_test`), 6 form specs:
>
> - **Headless runner** (`com.servoy.eclipse.cypress.cypressFormTestRunner`):
>   `Total: 6 | Passed: 6 | Failed: 0`, `Export DONE.`
> - **Servoy Developer** ("Run All Cypress Form Tests" context action):
>   `Runs: 6/6, Failures: 0, Errors: 0` — was 1/6 before fix §3.6.
>
> Sections 3 and 4 describe what was actually built, including two corrections made
> during implementation (see §3.3 and §3.4) where the originally planned approach turned
> out not to work.

## 1. Goal

Fix the defects reported when SVY-21173 was reopened (2026-09-28, Marius Muntean):

1. An `ArrayIndexOutOfBoundsException` crash from `SaveManager.forEachProjectInParallel`
   during `AbstractWorkspaceExporter.start()`'s final `ResourcesPlugin.getWorkspace().save(true, null)`
   call, which propagated uncaught and aborted the runner on shutdown.
2. "db issues" — the headless Cypress form test runner had no equivalent of
   `ServoyModel`'s Developer-only convenience that auto-creates in-memory test-server
   tables from cached `.dbi` metadata, so a fresh in-memory HSQLDB server (e.g.
   `servoy_test`) never got its tables (e.g. `orders`) created, and forms depending on
   them failed the build with "table is not accessible".

Two further defects were found while verifying the above and are fixed here too, because
without them the runner reports failures that look like the reported "db issues". Both
have the same symptom — every form spec except the one targeting the solution's first
form fails with `Expected to find element: [data-cy^="<form>."], but never found it` —
but they live in two independent WebSocket session factory registrations, so each needed
its own fix:

3. **Headless runner:** `CypressFormTestRunner.activateNgClientBundle()`'s guard always
   bailed out, so the runner's formpreview-aware factory was never installed and no
   `FormPreviewNGClient` was ever created (see §2.3).
4. **Servoy Developer UI** ("Run Cypress Form Test(s)" context actions):
   `com.servoy.eclipse.ngclient.startup.Activator`'s session factory had branches for
   `nodebug` and `svy_developer` but none for `formpreview`, so Developer never created a
   `FormPreviewNGClient` either (see §2.4).

All fixes are scoped to avoid changing behavior for the WAR/solution/mobile exporters,
which have never needed any of them.

## 2. Background

This builds on the SVY-21173 implementation already shipped across five repos:
`servoy-eclipse` (`CypressFormTestRunner`, `FormPreviewNGClient`, exported
`AbstractWorkspaceExporter` package), `servoy-eclipse-tomcat` (resilient Tomcat
activation), `build` (launcher scripts), and `Servoy-Copilot` (specs + contract tests).
See `docs/SVY-21173-triage.md` for the root-cause investigation this spec is based on.
The three problems below are independent; none blocks the others.

### 2.1 `ArrayIndexOutOfBoundsException` in `SaveManager.forEachProjectInParallel`

`AbstractWorkspaceExporter.start()` calls `ResourcesPlugin.getWorkspace().save(true, null)`
inside a `finally` block, after `ApplicationServerRegistry.get().doNativeShutdown()`:

```java
finally
{
    ApplicationServerRegistry.get().doNativeShutdown();
    try
    {
        ResourcesPlugin.getWorkspace().save(true, null);
    }
    catch (CoreException e)
    {
        ServoyLog.logError(e);
    }
}
```

This is a confirmed **upstream Eclipse Platform bug**
([eclipse-platform/eclipse.platform#2869](https://github.com/eclipse-platform/eclipse.platform/issues/2869),
fixed by [PR #2870](https://github.com/eclipse-platform/eclipse.platform/pull/2870),
merged 2026-08-12, targeting Eclipse 4.41 M3 — later than this project's pinned
2025-12 target platform). `SaveManager.forEachProjectInParallel` has an off-by-one bug:
when exactly one project's save callback fails, it indexes `stats[1]` instead of
`stats[0]` when building the `CoreException` to report that failure, throwing
`ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1` instead of
reporting the real per-project save error.

`ArrayIndexOutOfBoundsException` is a `RuntimeException`, not a `CoreException`, so the
existing `catch (CoreException e)` did not catch it — it propagated out of `start()`
uncaught, aborting the whole runner (and would do the same to the WAR/solution/mobile
exporters, which share this exact code, if they ever hit the same underlying
per-project save failure).

The 2026-05-25 commit `ba1234019b` ("Trying to fix an exception on Jenkins") is
precedent: it fixed a different Eclipse `ResourceException` from the same
project-import/save code region, also surfaced first by Jenkins CI rather than
interactive Developer use.

### 2.2 In-memory test database tables not auto-created headlessly

`com.servoy.eclipse.core.ServoyModel.updateResources()` (`ServoyModel.java:1552-1599`)
has a Developer-only convenience that auto-creates tables in in-memory test servers
from cached `.dbi` files, guarded by:

```java
if (server.getConfig().isInMemDriver() && !IServer.INMEM_SERVER.equals(server.getConfig().getServerName()) &&
    server.getTableNames(true).size() == 0)
```

and calling, per `.dbi` file found in the server's information folder:

```java
EclipseDatabaseUtils.createNewTableFromColumnInfo(server, tableName, dbiFileContent,
    EclipseDatabaseUtils.NO_UPDATE);
```

That block runs only inside the interactive `com.servoy.eclipse.core.ServoyModel`, which
the IDE uses. The headless exporter family (`WarWorkspaceExporter`,
`SolutionWorkspaceExporter`, `CypressFormTestRunner`, etc.) uses
`com.servoy.eclipse.exporter.apps.common.ExportServoyModel` instead
(`ExportServoyModel.java:47-111`), which has no equivalent logic in its
`initialize()`/`setActiveResourcesProject()`.

This has never mattered for WAR/solution/mobile export, because exporting reads the
repository/model, not live table data. `CypressFormTestRunner` is different: it needs
the NG client to render forms live against a real (even if throwaway, in-memory)
database, so a server configured with `jdbc:hsqldb:mem:.` (created empty on every JVM
start) and no auto-create step means any form whose table hasn't been pre-provisioned
by hand fails with "table is not accessible" — undermining the "runs easily in CI"
goal of the original SVY-21173 ticket.

Confirmed in the reporter's environment: `servoy.properties` defines
`server.7.URL=jdbc:hsqldb:mem:.` / `server.7.serverName=servoy_test`, and
`test_webcomponents2`'s forms bind to `orders` on that server.

### 2.3 Formpreview WebSocket factory never installed

`CypressFormTestRunner.activateNgClientBundle()` installs an
`IWebsocketSessionFactory` whose `init()` creates a `FormPreviewNGClient` when the
request parameters contain `formpreview`. `FormPreviewNGClient` is what bypasses
authentication (`showDefaultLogin()` override) and shows the requested form rather than
the solution's `firstFormID`.

It was gated by:

```java
IWebsocketSessionFactory existingFactory = WebsocketSessionManager
        .getWebsocketSessionFactory(WebsocketSessionFactory.CLIENT_ENDPOINT);
if (existingFactory != null && !(existingFactory instanceof WebsocketSessionFactory)) {
    outputExtra("Formpreview-aware WebSocket factory already registered.");
    return;
}
```

The factory registered by `com.servoy.eclipse.ngclient.startup.Activator` is an
**anonymous** `IWebsocketSessionFactory` — not a `WebsocketSessionFactory` instance —
and its `init()` has branches for `nodebug` and `svy_developer` but **no `formpreview`
branch at all**. So `!(existingFactory instanceof WebsocketSessionFactory)` was always
true, the guard read that as "already formpreview-aware", and returned early. The
runner's own factory was never installed and no `FormPreviewNGClient` was ever created.

Consequence: each spec's `cy.visit(...?formpreview=<form>...)` got a regular NG client,
which opens the solution's `firstFormID`. For `test_webcomponents2` that is
`f1_aggrid_editCellOnEnter`, so spec 1 passed by coincidence and specs 2-6 all timed out
in `beforeEach` looking for their own form's `data-cy` attributes. Clients were also not
released between specs, which additionally exhausted the license pool
("No more licenses available") on the later specs.

### 2.4 Same symptom in Developer, via a different factory

`com.servoy.eclipse.ngclient.startup.Activator.start()` registers its own
`IWebsocketSessionFactory` for `WebsocketSessionFactory.CLIENT_ENDPOINT`. Its `init()`
branched on `nodebug` and `svy_developer`, then fell through to the debug/regular client —
**no `formpreview` branch**. So Developer's "Run Cypress Form Test(s)" context actions hit
exactly the same wrong-form outcome as §2.3, for an unrelated reason: the headless runner
replaces this factory with its own, but in Developer this is the factory that serves the
request.

The two are genuinely independent registrations in different bundles, so fixing §2.3 did
nothing for Developer — verified empirically: after the §3.5 fix the headless runner was
6/6 while Developer was still 1/6, with the identical error on the same 5 specs.

## 3. Design

### 3.1 Defensive handling around the final workspace save

In `AbstractWorkspaceExporter.start()`, add a second catch for `RuntimeException`
alongside the existing `catch (CoreException e)` around
`ResourcesPlugin.getWorkspace().save(true, null)`, logging it via
`ServoyLog.logError(e)` exactly as `CoreException` is logged, so a buggy Eclipse-side
error-reporting path cannot abort the runner's shutdown or corrupt its exit code.

Two separate catch blocks rather than a multi-catch, so the `RuntimeException` one can
carry a comment identifying the known Eclipse Platform `SaveManager` issue.

The underlying per-project save failure that `SaveManager` is trying (and failing) to
report cannot be recovered here; logging and continuing shutdown is the correct scope.
Since `AbstractWorkspaceExporter` is the shared base of the WAR/solution/mobile
exporters, this protects all of them from the same latent Eclipse bug, with no
behavioral change on the success path.

### 3.2 New `afterSolutionActivated` hook in `AbstractWorkspaceExporter`

`AbstractWorkspaceExporter.checkAndExportSolutions()` had no extension point between
"solution activated" and "build + problem-marker check". Add a no-op hook called at
exactly that point, inside the per-solution loop:

```java
protected void afterSolutionActivated(T configuration, String solutionName)
{
    // no-op by default
}
```

invoked right after `sm.getActiveProject()`/`sm.getActiveResourcesProject()` are
confirmed non-null and before the `configuration.skipBuild()` branch that runs
`sm.buildActiveProjects(...)` and `checkProjectMarkers(...)`.

WAR/solution/mobile exporters do not override it, so they are unaffected.

### 3.3 Auto-create in-memory test tables — correction to planned placement

**Originally planned:** call the auto-create helper as the first statement of
`CypressFormTestRunner.exportActiveSolution()`.

**Why that does not work:** `exportActiveSolution()` runs *after* the build and
problem-marker check. A form bound to a not-yet-created in-memory table fails that check
with "table is not accessible", `checkAndExportSolutions()` sets
`exitCode = EXIT_EXPORT_FAILED` and returns early, and `exportActiveSolution()` is never
reached. The tables must exist *before* the build runs.

**As built:** `CypressFormTestRunner` overrides the §3.2 hook:

```java
@Override
protected void afterSolutionActivated(CypressFormTestArgumentChest configuration, String solutionName) {
    autoCreateInMemoryTestTables();
}
```

This is the correct point: `ExportServoyModel`'s resources project — and therefore its
`DataModelManager` — is populated by `sm.initialize(solutionName)` just above, while the
build has not yet run.

`autoCreateInMemoryTestTables()` itself:

1. Obtains `IServerManagerInternal` via `ApplicationServerRegistry.get().getServerManager()`
   and `DataModelManager` via `ServoyModelFinder.getServoyModel().getDataModelManager()`
   (an existing getter inherited from `AbstractServoyModel`; no new accessor was needed
   on `ExportServoyModel`, resolving §7's first open question).
2. Iterates `serverManager.getServerNames(true, true, true, true)`.
3. Applies `ServoyModel`'s guard verbatim:
   `server.getConfig().isInMemDriver() && !IServer.INMEM_SERVER.equals(server.getConfig().getServerName()) && server.getTableNames(true).size() == 0`.
4. For each `.dbi` file in that server's information folder, creates the table (see §3.4)
   and reports it via `outputExtra`.
5. Wraps the per-server block in `try/catch (Exception e)` logging via `ServoyLog.logError`
   plus `outputExtra`, so one server's failure neither blocks other servers nor aborts
   the run — this is a best-effort CI convenience, mirroring `ServoyModel`'s own
   exception-swallowing behavior.

### 3.4 Headless-safe table creation — correction to planned implementation

**Originally planned:** call
`EclipseDatabaseUtils.createNewTableFromColumnInfo(server, tableName, dbiFileContent, EclipseDatabaseUtils.NO_UPDATE)`,
identical to `ServoyModel`.

**Why that does not work:** `EclipseDatabaseUtils` lives in `com.servoy.eclipse.core`,
whose `Activator.start()` unconditionally calls `ModelUtils.assertUINotDisabled(...)`.
Every headless exporter sets `ModelUtils.setUIDisabled(true)` in
`AbstractWorkspaceExporter.start()`, so the moment OSGi lazily activates that bundle to
load *any* of its classes, activation throws and the caller gets:

```
java.lang.NoClassDefFoundError: com/servoy/eclipse/core/util/EclipseDatabaseUtils
  ...
Caused by: java.lang.RuntimeException: 'com.servoy.eclipse.core' bundle will not be
  started as Servoy is started without UI. Please ignore this log message.
```

Parameterizing `EclipseDatabaseUtils` (e.g. passing `DataModelManager` in rather than
resolving it from `ServoyModelManager`) does **not** help — the bundle-activation guard
fires on class load, independent of method signatures. `EclipseDatabaseUtils` is left
untouched.

**As built:** a private `createTableFromColumnInfo(IServerInternal server, String tableName, String dbiFileContent)`
in `CypressFormTestRunner`, using only `servoy_shared` APIs (no `com.servoy.eclipse.core`
dependency):

- `DatabaseUtils.deserializeTableInfo(dbiFileContent)` → `TableDef`; validates that the
  table name matches the `.dbi` name and that it declares at least one column.
- `server.createNewTable(DummyValidator.INSTANCE, tableName, false, true)`, then
  `setMarkedAsMetaData` / `setTableMarkedAsHiddenInDeveloper` from the `TableDef`.
- Columns created in `creationOrderIndex` order (ties broken by name, matching
  `ServoyModel`), each with `setDatabasePK` (from `IBaseColumn.PK_COLUMN`), `setFlags`,
  `setAllowNull`, and `setSequenceType` when
  `autoEnterType == ColumnInfo.SEQUENCE_AUTO_ENTER`, falling back to
  `ColumnInfo.SERVOY_SEQUENCE` when `server.supportsSequenceType(...)` says no.
- `server.syncTableObjWithDB(table, false, false)` to create it in the database, with
  `server.removeTable(table)` cleanup if the sync fails and the table is not yet in the DB.
- `server.createMissingDBSequences(table)`.
- Returns `null` on success or a newline-separated problem description, mirroring
  `EclipseDatabaseUtils.createNewTableFromColumnInfo`'s contract.

Deliberately **not** replicated: the `ColumnInfo`/`.dbi` write-back bookkeeping
(`dmm.updateAllColumnInfo`, per-column `ColumnInfo` population). That is Developer's
persisted-metadata concern and is exactly what pulled in the `com.servoy.eclipse.core`
dependency; it is meaningless for a throwaway in-memory table, and the original call
used `NO_UPDATE` anyway.

### 3.5 Always install the formpreview WebSocket factory

Delete the guard described in §2.3 so `activateNgClientBundle()` unconditionally
overwrites whatever factory is registered for `WebsocketSessionFactory.CLIENT_ENDPOINT`.

Overwriting is safe: the runner's factory calls `super.init(requestParams)` for any
request without a `formpreview` parameter, so plain `NGClientWebsocketSession` behaviour
is preserved for every other client.

No `instanceof`-based (or otherwise type-sniffing) detection should be reintroduced —
the factory this needs to replace is an anonymous class with no distinguishing type, so
any such check is unreliable by construction.

### 3.6 Add a `formpreview` branch to the shared ngclient Activator

Add the missing branch to `com.servoy.eclipse.ngclient.startup.Activator`'s session
factory `init()`, placed **first** (before `nodebug`), doing what the runner's factory
does:

```java
if (requestParams.containsKey("formpreview"))
{
    String formName = requestParams.get("formpreview").get(0);
    FormPreviewNGClient.setPendingTargetFormName(formName);
    setClient(new FormPreviewNGClient(this, designerCallback, formName));
}
else if (requestParams.containsKey("nodebug"))
...
```

`FormPreviewNGClient` is in the same package, so no new import is required.

**Blast radius.** This is the shared NG client bundle the whole IDE uses, so this is the
widest-reaching change in this spec. It is additive: the branch only fires when a
`formpreview` request parameter is present, which in practice only Cypress form specs send
(`FormSpecGenerator` is the only producer). No pre-existing branch or client type changed
behaviour, and passing `designerCallback` (rather than the `null` the headless runner uses)
keeps Developer's designer integration intact.

1. **`com.servoy.eclipse.exporter.solution/.../AbstractWorkspaceExporter.java`**
   - In `start()`'s `finally` block, add `catch (RuntimeException e) { ServoyLog.logError(e); }`
     after the existing `catch (CoreException e)`, with a comment citing
     eclipse-platform#2869 (§3.1).
   - Add the `afterSolutionActivated(T configuration, String solutionName)` no-op hook
     and call it in `checkAndExportSolutions()` immediately before the
     `configuration.skipBuild()` branch (§3.2).

2. **`com.servoy.eclipse.cypress/.../headless/CypressFormTestRunner.java`**
   - Override `afterSolutionActivated(...)` to call `autoCreateInMemoryTestTables()` (§3.3).
   - Add private `autoCreateInMemoryTestTables()` (§3.3) and private
     `createTableFromColumnInfo(...)` (§3.4).
   - Remove the early-return guard from `activateNgClientBundle()` (§3.5).

3. **`com.servoy.eclipse.ngclient/.../startup/Activator.java`**
   - Add the `formpreview` branch to the session factory's `init()`, before the
     `nodebug` branch (§3.6).

4. Verify zero compilation errors in all changed files.

5. Confirm the WAR/solution/mobile exporters are unaffected:
   - The `AbstractWorkspaceExporter` changes only widen a catch clause and add a no-op
     hook they do not override.
   - The runner changes are inside `CypressFormTestRunner` only. `EclipseDatabaseUtils`,
     `ExportServoyModel` and `ServoyModel` are untouched.
   - The `Activator` branch is additive and `formpreview`-gated (§3.6).

6. Run the 6 specs through **both** execution paths and confirm all pass:
   - the headless runner (`com.servoy.eclipse.cypress.cypressFormTestRunner`), and
   - Developer's "Run All Cypress Form Tests" context action.

   Both are required: they use different WebSocket session factories (§2.3 vs §2.4), so a
   green result in one says nothing about the other.

## 5. Acceptance criteria

- [x] `AbstractWorkspaceExporter.start()` no longer lets an `ArrayIndexOutOfBoundsException`
      (or any other `RuntimeException`) from `ResourcesPlugin.getWorkspace().save(true, null)`
      propagate out of the `finally` block; it is logged via `ServoyLog.logError` and
      shutdown completes normally. *(Verified: run ends `Export DONE.` with no crash.)*
- [x] `CypressFormTestRunner` auto-creates tables (from cached `.dbi` metadata) for any
      in-memory-driver server that currently has zero tables, **before the build's
      problem-marker check runs**. *(Verified: ~105 tables incl. `orders` reported as
      "Auto-created in-memory test table ... on server 'servoy_test'"; the
      "table is not accessible" markers are gone.)*
- [x] Table creation works with `com.servoy.eclipse.core` inactive (UI disabled).
      *(Verified: no `NoClassDefFoundError`.)*
- [x] A `FormPreviewNGClient` is created for every spec in the **headless runner**, so each
      spec's own form renders. *(Verified: `CypressFormTestRunner$2$1.init` →
      `FormPreviewNGClient.<init>` in the log for each form, and each form's own
      `DataAdapterList` is active.)*
- [x] A `FormPreviewNGClient` is created for every spec in **Developer** too.
      *(Verified: Developer's "Run All Cypress Form Tests" went from 1/6 to 6/6, each spec
      asserting its own form's `data-cy` elements and completing in ~4-5s rather than
      timing out at 30s.)*
- [x] All discovered form specs pass in both paths. *(Verified: headless
      `Total: 6 | Passed: 6 | Failed: 0`; Developer `Runs: 6/6, Failures: 0, Errors: 0`.)*
- [x] The `Activator` change does not regress the headless path. *(Verified: headless run
      after the change is still 6/6. Note it exercises the runner's own factory, so this
      confirms no regression rather than confirming the new branch.)*
- [x] `WarWorkspaceExporter`, the solution exporter, and the mobile exporter are
      unaffected — they do not override the new hook, no auto-create logic runs for them,
      and their workspace-save error handling differs only in that a `RuntimeException`
      is now logged instead of crashing (intentional and safe).
- [x] Zero compilation errors after the change.

## 6. Out of scope

- Root-causing the actual per-project save failure that trips the Eclipse `SaveManager`
  bug in the first place (the bug masks it; needs a target platform with
  eclipse-platform#2870 to surface the real error).
- Bumping the Eclipse target platform to ≥ 4.41 M3 to get the upstream fix directly.
- Porting the auto-create-from-`.dbi` logic to the shared `ExportServoyModel` (would
  change behavior for WAR/solution/mobile exporters, which don't need it).
- Any change to `ServoyModel.updateResources()` or `EclipseDatabaseUtils` — both work
  correctly for the interactive IDE case.
- Documentation of manual test-database provisioning as an alternative fix (superseded
  by the auto-create approach).
- Unifying the two WebSocket session factory registrations (§2.3 / §2.4) so `formpreview`
  is handled in one place only. Both now behave identically, but the duplication remains
  and is a latent source of exactly this class of divergence. Follow-up ticket.
- **`DataAdapterList` off-event-thread warnings** during `FormPreviewNGClient` teardown —
  **SVY-21529**, resolved on this branch in `97536e3a89`, which replaced this branch's
  `FormPreviewNGClient` and ngclient `Activator` with `origin/release`'s versions. Release
  routes `shutDown(true)` onto the old client's own event dispatcher
  (`shutdownOnEventThread()` / `shutdownRouting()`) and reuses/retargets an existing
  preview client on the same session instead of rebuilding it. A full 6-spec headless run
  afterwards logs zero `Unexpected execution outside of the event dispatch thread` traces,
  down from ~20, still 6/6.

## 7. Open questions — resolved

| Question | Resolution |
|----------|------------|
| Should `ExportServoyModel` expose a public getter for `dataModelManager`? | **Not needed.** `AbstractServoyModel` already provides `getDataModelManager()`; `ServoyModelFinder.getServoyModel().getDataModelManager()` is used and no new accessor was added. |
| Does the reporter's `servoy_test` HSQLDB environment lack a provisioning step, or is auto-create expected? | **Auto-create is required.** The server is `jdbc:hsqldb:mem:.`, recreated empty on every JVM start, so no provisioning step can persist. Verified by the green run. |
| Is the whole-server-empty guard (`getTableNames(true).size() == 0`) right, given a partially-populated server would be skipped? | **Left as-is**, matching `ServoyModel` exactly. A per-table check would be a behavior change beyond this fix; noted as a possible follow-up if a partially-provisioned in-memory server is ever a real scenario. |
