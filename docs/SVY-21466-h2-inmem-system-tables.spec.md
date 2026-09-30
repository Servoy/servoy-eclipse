# Spec: SVY-21466 — H2 inmem DB: system tables leak into the loaded table set

## 1. Goal

Fix the file-backed H2 in-memory server (`inmem_h2`) in Servoy Developer so that (1) tables
created by the user survive a Developer restart, and (2) *Synchronize with DB server
information* no longer reports the H2 system tables as tables without a primary key. Both
symptoms share one root cause: H2 2.x `INFORMATION_SCHEMA` system tables are loaded into the
server's table map and treated as if they were user tables.

The fix has three parts, each addressing the root cause at a different layer:

- A `skipSysTables` overload on the server's table-name lookup, used by the developer startup
  guard so it counts only real (non-system) user tables.
- Making newly created H2 in-mem servers skip system tables **by default** (via the server
  config template), which addresses symptom 2 for new servers at the load layer.
- Hardening the startup recreation loop so it never tries to recreate a `.dbi` table whose
  name is already loaded on the server (e.g. a stray system table on a pre-existing server).

## 2. Background

### 2.1 The two in-memory server kinds

- **Solution mem server** (`_sv_inmem`, `IServer.INMEM_SERVER`): pure in-memory, backed by
  solution `TableNode`s (`com.servoy.eclipse.model.inmemory.AbstractMemServer`); its config
  already sets `skipSysTables = true`. Not affected.
- **File-backed H2 in-memory server** (`inmem_h2`): a real JDBC `Server`
  (`com.servoy.j2db.server.persistence.Server`) over `jdbc:h2:mem:.;DB_CLOSE_DELAY=-1`, whose
  tables are persisted as `.dbi` column-info files and recreated at startup. This is the
  server in the ticket.

### 2.2 Startup recreation from `.dbi`

`com.servoy.eclipse.core.ServoyModel.updateResources(int, IProgressMonitor)` contains an
`IRunnableWithProgress` (`ServoyModel$13.run`) that, per server, does:

```java
if (server.getConfig().isInMemDriver()
    && !IServer.INMEM_SERVER.equals(server.getConfig().getServerName())
    && server.getTableNames(true).size() == 0)          // guard
{
    // walk dataModelManager.getServerInformationFolder(server.getName())
    // for each *.dbi: EclipseDatabaseUtils.createNewTableFromColumnInfo(server, tableName, dbiFileContent, NO_UPDATE)
}
```

`createNewTableFromColumnInfo` is called from exactly two places — this startup block and
`SynchronizeDBIWithDBWizard` — matching the two ticket symptoms.

### 2.3 Root cause (live debugger evidence)

Suspended `servoy.product` session, `Server` id=645, stopped in `loadTablesIfNecessary`:

- `getServerName()` = `inmem_h2`; `isInMemDriver()` = `true`; H2 product `2.4.240`.
- `getSchema()` = `null`, `getCatalog()` = `null`, `getSkipSysTables()` = `false`.
- `tables.size()` = 35, `getTableNames(true).size()` = **15** — all H2 metadata
  (`columns`, `tables`, `indexes`, `views`, `sequences`, `users`, `routines`, …).
- `tables.get("columns").getSchema()` = `INFORMATION_SCHEMA`.

`Server.loadTables` runs `DatabaseMetaData.getTables(null, null, "%", …)`. It filters system
tables only when `skipSysTables == true` (schema starts with `SYS` or equals
`INFORMATION_SCHEMA`) or the JDBC type name contains `SYS`. For this server config
`skipSysTables == false`, so H2 2.x's `INFORMATION_SCHEMA` objects enter the normal `tables`
map. Consequences:

1. At startup a fresh H2 mem DB has no user tables, but the loaded set is 15
   `INFORMATION_SCHEMA` tables → `getTableNames(true).size()` is 15, not 0 → the guard is
   false → the `.dbi` recreation is skipped → **user tables gone after restart (symptom 1)**.
2. *Synchronize with DB server information* iterates the server's tables and reports the
   pk-less `INFORMATION_SCHEMA` tables as errors → **symptom 2**.

The `.dbi`-backed user tables are, by construction, **normal tables in a user schema** — never
H2 `INFORMATION_SCHEMA` system tables.

### 2.4 Regression trigger

H2 2.x serves its catalog from the `INFORMATION_SCHEMA` schema; older H2 exposed metadata in a
form Servoy's existing `SYS`-prefix filtering excluded. The H2 major-version upgrade is the
regression trigger. (H2 is a bundled jar / Maven dependency, not declared in
`launch_targets/*.target`.)

### 2.5 Scope caution — `isInMemDriver()` also matches HSQLDB

`ServerConfig.isInMemDriver()` returns true when the driver contains `org.h2` **or**
`hsqldb`. The `skipSysTables` name-count filter targets the H2 system schema
(`INFORMATION_SCHEMA`) specifically and is null-safe, so tables in a user schema are never
excluded.

## 3. Design

### 3.1 New `skipSysTables` overloads on the server (for the startup guard)

In `com.servoy.j2db.server.persistence.Server`:

- Add `public List<String> getTableNames(boolean hideTempTables, boolean skipSysTables)`.
- Refactor the private `getDBObjectNames(int type, boolean hideTempViews)` to
  `getDBObjectNames(int type, boolean hideTempViews, boolean skipSysTables)` that, when
  `skipSysTables` is true, additionally excludes any `Table` whose schema is a system schema.
- Keep the existing `getTableNames(boolean)` and `getViewNames(boolean)` delegating with
  `skipSysTables == false`, so all current callers are unchanged.
- Add a private `isSystemSchema(String schema)` helper: `INFORMATION_SCHEMA`
  case-insensitive, null-safe (a null schema is not a system table).

In `com.servoy.j2db.persistence.IServerInternal`: add a `default` method
`getTableNames(boolean hideTempTables, boolean skipSysTables)` that delegates to
`getTableNames(hideTempTables)` (ignores the flag). The real JDBC `Server` overrides it; the
other implementors (mem servers, view-foundset server, proxy) inherit the flag-ignoring
default, so they are unaffected. This is required because the startup guard calls
`getTableNames` through an `IServerInternal` reference.

### 3.2 Use the overload in the startup guard

In `ServoyModel.updateResources` (the `ServoyModel$13` runnable), change the guard's table
count from `server.getTableNames(true).size() == 0` to
`server.getTableNames(true, true).size() == 0`. A fresh H2 mem DB whose loaded tables are all
`INFORMATION_SCHEMA` now reports 0 real tables, so the `.dbi` recreation runs.

### 3.3 Skip already-loaded tables in the recreation loop

Inside the `serverInformationFolder.accept(...)` visitor, before recreating a table from its
`.dbi`, skip it when the server already has a table of that name:

```java
try {
    if (server.hasTable(tableName)) {
        return true;   // continue visiting other resources
    }
} catch (RepositoryException e) {
    ServoyLog.logError(e);
}
```

This ensures only tables that are not already present are (re)created. On a pre-existing
`inmem_h2` server whose config still has `skipSysTables == false`, a stray
`INFORMATION_SCHEMA` table with the same name as a `.dbi` will no longer trigger a recreation
attempt. `hasTable` normalizes the name and consults the loaded map; `return true` continues
the `IResourceVisitor` walk rather than aborting it.

### 3.4 Skip system tables by default for new H2 in-mem servers

In `com.servoy.j2db.serverconfigtemplates.InMemoryH2Template`, add `.setSkipSysTables(true)`
to the `ServerConfig.Builder`. Newly created H2 in-mem servers then skip system tables at the
`Server.loadTables` layer (its existing `skipSysTables` branch already excludes
`INFORMATION_SCHEMA`), so the stray system tables never enter the map. This is the root-cause
fix for symptom 2 for newly created servers, and it also means symptom 1 cannot arise for
them. It does **not** retroactively change the config of an already-created `inmem_h2` server
(that persisted config keeps `skipSysTables == false`), which is why §3.1–3.3 are still
needed for existing servers.

### 3.5 How the parts interact

- New servers: §3.4 keeps system tables out of the map entirely → both symptoms avoided.
- Existing servers (config `skipSysTables == false`): §3.2 makes the startup guard see 0 real
  tables so recreation runs; §3.3 stops the loop from colliding with a stray system table of
  the same name. Symptom 2's Synchronize path for existing servers is being investigated
  separately (see §6) — §3.1–3.3 do not change that wizard.

## 4. Implementation plan

1. `j2db_server/src/com/servoy/j2db/server/persistence/Server.java`: add
   `getTableNames(boolean, boolean)`; refactor `getDBObjectNames` to take `skipSysTables` and
   exclude system-schema tables; delegate the existing `getTableNames(boolean)` /
   `getViewNames(boolean)` with `false`; add private null-safe `isSystemSchema(String)`.
2. `servoy_shared/src/com/servoy/j2db/persistence/IServerInternal.java`: add a `default`
   `getTableNames(boolean, boolean)` delegating to `getTableNames(boolean)`.
3. `com.servoy.eclipse.core/src/com/servoy/eclipse/core/ServoyModel.java`: guard uses
   `getTableNames(true, true)`; in the `.dbi` visitor, `return true` early when
   `server.hasTable(tableName)` (wrapped in try/catch for `RepositoryException`).
4. `servoy_shared/src/com/servoy/j2db/serverconfigtemplates/InMemoryH2Template.java`: add
   `.setSkipSysTables(true)` to the builder.
5. After edits: `getCompilationErrors` on all changed files; organize imports; fix any
   blocking SpotBugs.
6. Tests (see §5) and runtime verification via the debug launch where feasible.

## 5. Acceptance criteria

- [ ] `Server.getTableNames(hideTempTables, true)` excludes tables whose schema is
      `INFORMATION_SCHEMA` (case-insensitive); `getTableNames(hideTempTables, false)` and the
      existing `getTableNames(hideTempTables)` return the same result as before (system tables
      included).
- [ ] For a fresh H2 in-mem DB whose only loaded tables are `INFORMATION_SCHEMA` objects,
      `getTableNames(true, true).size()` is 0.
- [ ] The `skipSysTables` filter only affects the requested table type (a VIEW is not returned
      by `getTableNames`), and keeps user-schema and null-schema tables.
- [ ] After creating tables on the `inmem_h2` server and restarting Developer, the previously
      created tables are present again.
- [ ] The `updateResources` guard uses `getTableNames(true, true)`, and the `.dbi` recreation
      loop skips any table name already present on the server (`server.hasTable`).
- [ ] `InMemoryH2Template` builds its config with `skipSysTables == true`, so a newly created
      H2 in-mem server does not load `INFORMATION_SCHEMA` tables into its table map.
- [ ] No behavior change for non-H2 servers or for existing callers of `getTableNames(boolean)`.
- [ ] All changed files compile with no new errors and no new blocking SpotBugs.

## 6. Out of scope

- The *Synchronize with DB server information* wizard code path
  (`SynchronizeDBIWithDBWizard`, `getTableAndViewNames`/`getViewNames`) for **existing**
  servers — investigated separately. (§3.4 addresses symptom 2 for newly created servers at
  the load layer.)
- Changing `Server.loadTables` filtering logic itself.
- Adding `skipSysTables` to `getTableAndViewNames` or `getViewNames`.
- Retroactively migrating the persisted config of already-created in-mem H2 servers.
- The solution-based `_sv_inmem` / `AbstractMemServer` path.
- Upgrading/repackaging the H2 dependency.

## 7. Tests

- `j2db_test` → `com.servoy.j2db.server.persistence.ServerGetTableNamesSkipSysTablesTest`
  [JUnit 4] — builds a `Server` via its public constructor with a test `ITableLoader`
  (`FixedTableLoader implements ITableLoader`) that seeds the table map through the real
  `loadTablesIfNecessary()` path (no reflection, no JDBC). Covers: `getTableNames(true, true)`
  excludes `INFORMATION_SCHEMA` while keeping `PUBLIC`- and null-schema tables; a fresh H2
  DB with only system tables reports 0 real tables; case-insensitive schema match; the
  pre-existing `getTableNames(boolean)` still includes system tables; the filter only affects
  the requested table type. 5 tests, all passing.
- The §3.3 `hasTable` skip and §3.2 guard call site live in `ServoyModel.updateResources`,
  which requires the OSGi workbench + a resources project + `.dbi` files on disk. These are
  covered by code review rather than a heavyweight integration test, consistent with how
  other OSGi-only guard logic in this repo is verified.
- The §3.4 `InMemoryH2Template` default may optionally be covered by a tiny unit test asserting
  `new InMemoryH2Template().getServerConfig().getSkipSysTables()` (or the template's config
  accessor) is true — see open questions.

## 8. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Is matching `INFORMATION_SCHEMA` (case-insensitive) sufficient for all supported H2 versions, or should a broader H2 system-schema set be recognized? | dev | open |
| Add a unit test for `InMemoryH2Template` asserting `skipSysTables == true`, or is code review sufficient for a one-line template default? | reviewer | open |
