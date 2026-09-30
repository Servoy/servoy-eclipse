# Triage Report — SVY-21466

**Verdict:** PROCEED

## Reported problem

"H2 inmem DB issues" (Developer / Eclipse environment). Two observed symptoms:

1. Created tables are gone after a Developer restart.
2. When *Synchronize with DB server information* is called, many errors appear because
   the existing tables don't have primary keys.

The ticket proposes no concrete solution — it only describes the symptoms.

## Root-cause assessment

The case is about the **file-backed H2 in-memory server** (server name `inmem_h2`,
`isInMemDriver() == true`), which is distinct from the pure solution-based mem server
(`_sv_inmem`, handled by `AbstractMemServer`). At startup the developer is supposed to
recreate this server's tables from the persisted `.dbi` column-info files.

That recreation lives in `com.servoy.eclipse.core.ServoyModel.updateResources(...)`, in the
`IRunnableWithProgress` block the user's breakpoint targets
(`ServoyModel.java` around line 1360-1400 — the `ServoyModel$13.run` frame). The guard is:

```java
if (server.getConfig().isInMemDriver()
    && !IServer.INMEM_SERVER.equals(server.getConfig().getServerName())
    && server.getTableNames(true).size() == 0)   // <-- the failing precondition
{
    // ... walk the server-information folder, read each .dbi file, and
    // EclipseDatabaseUtils.createNewTableFromColumnInfo(server, tableName, dbiFileContent, NO_UPDATE);
}
```

**Live debugger evidence (current suspended `servoy.product` session, `Server` id=645):**

- `getConfig().getServerName()` = `inmem_h2`
- `getConfig().isInMemDriver()` = `true`  → first two guard clauses pass.
- `getConfig().getServerUrl()` = `jdbc:h2:mem:.;DB_CLOSE_DELAY=-1`
- H2 database product version = `2.4.240 (2025-09-22)`
- `getConfig().getSchema()` / `getCatalog()` = `null` (no schema/catalog filter)
- `config.getSkipSysTables()` = `false`
- `tables.size()` = **35**; `getTableNames(true).size()` = **15**
- The loaded table set is entirely H2 metadata:
  `collations, columns, roles, check_constraints, query_statistics, sequences, locks,
  index_columns, constraint_column_usage, tables, indexes, column_privileges, rights,
  in_doubt, element_types, constants, session_state, referential_constraints, views,
  settings, sessions, table_constraints, schemata, enum_values, synonyms, domains,
  triggers, users, table_privileges, information_schema_catalog_name, key_column_usage,
  domain_constraints, routines, fields, parameters`
- `tables.get("columns").getSchema()` = `INFORMATION_SCHEMA`, `getCatalog()` = `.`

So the chain is:

1. At startup the H2 mem DB is empty of user tables (nothing has been recreated yet), but
   `Server.loadTables(...)` runs `DatabaseMetaData.getTables(catalog=null, schema=null, "%",
   {TABLE, PARTITIONED TABLE, VIEW, SYNONYM})`. With `skipSysTables == false` and no schema
   filter, H2 2.x returns all of its **INFORMATION_SCHEMA** system tables/views. They are
   added to `tables` (they don't start with the Servoy prefix, so they aren't diverted to
   `servoy_tables`).
2. `tablesLoaded` becomes `true` with 35 entries; `getTableNames(true).size()` is 15 — **not 0**.
3. Back in `ServoyModel.updateResources`, the guard `server.getTableNames(true).size() == 0`
   is therefore **false**, so the `.dbi`-driven `createNewTableFromColumnInfo(...)` recreation
   is **skipped entirely**. The user's real tables are never recreated → **symptom 1**
   (tables gone after restart).
4. Because the H2 INFORMATION_SCHEMA "tables" are now sitting in the server's table map as if
   they were normal tables — and they have no Servoy primary keys — a later *Synchronize with
   DB server information* iterates them and floods the UI with "no pk" errors → **symptom 2**.

The regression trigger is the H2 upgrade. Older H2 exposed its metadata under a
schema that Servoy's `skipSysTables`/`SYS`-prefix filtering excluded; H2 2.x serves it from
`INFORMATION_SCHEMA`, which is only filtered when `skipSysTables == true`. For this
in-memory server config `skipSysTables` is `false`, so nothing filters it out. Two code
locations are implicated:

- `com.servoy.j2db.server.persistence.Server.loadTables(...)` (`j2db_server`) — lets
  `INFORMATION_SCHEMA` objects into the table map for this connection.
- `com.servoy.eclipse.core.ServoyModel.updateResources(...)` (`com.servoy.eclipse.core`,
  the breakpoint block) — uses `getTableNames(true).size() == 0` as the "nothing loaded yet,
  safe to recreate from dbi" precondition, which the stray system tables defeat.

## Ticket premise check

The ticket proposes no fix, so there is no premise to overturn. The two symptoms are **one
root cause with two visible effects**, not two independent bugs: the H2 INFORMATION_SCHEMA
tables leaking into the loaded table set both (a) block the startup dbi-recreation guard and
(b) are the pk-less tables that *Synchronize with DB server information* complains about.
Any fix must address the leak/guard, not the two symptoms separately.

## Approaches considered

1. **Exclude H2 INFORMATION_SCHEMA objects at the JDBC-metadata load in `Server.loadTables`**
   (skip `TABLE_SCHEM == "INFORMATION_SCHEMA"` — and/or the H2 system schemas — regardless of
   the `skipSysTables` flag, or force `skipSysTables`/a schema filter for the inmem H2 driver).
   *Pros:* fixes the true root cause; the stray tables never enter the map, so both symptoms
   disappear and the startup guard works again; least surprising for downstream code that
   iterates `tables`. *Cons:* touches shared server-side loading code (`j2db_server`), so it
   must be scoped carefully to the H2/in-mem case to avoid changing behavior for real DB
   servers that legitimately expose views.

2. **Fix only the recreation guard in `ServoyModel.updateResources`** — instead of
   `getTableNames(true).size() == 0`, always walk the server-information folder and call
   `createNewTableFromColumnInfo` for each `.dbi` that has no corresponding *user* table.
   *Pros:* localized to developer code; restores the tables. *Cons:* leaves the
   INFORMATION_SCHEMA tables in the map, so *Synchronize with DB server information* would
   still list pk-less system tables (symptom 2 only partly addressed); treats a symptom.

3. **Filter the stray system tables in the Synchronize-with-DB path only.** *Pros:* silences
   symptom 2's errors. *Cons:* pure symptom-masking; symptom 1 (tables gone) is untouched; the
   polluted table map persists everywhere else.

4. **No code change.** *Cons:* not viable — this is a real, reproducible developer-side
   regression (confirmed live in the debugger) with data-loss-like UX (user tables vanish on
   restart) and an error flood on sync.

## Recommendation

**Proceed with Approach 1 as the primary fix**, verified against the live debugger: prevent
H2's `INFORMATION_SCHEMA` (and equivalent system) objects from being loaded into the in-memory
H2 server's table map in `Server.loadTables(...)`, scoped to the in-mem/H2 case so real database
servers are unaffected. With the stray tables gone, `getTableNames(true).size()` is 0 again at
startup, so the existing `.dbi`-based recreation in `ServoyModel.updateResources` runs (symptom
1 fixed) and *Synchronize with DB server information* no longer sees pk-less system tables
(symptom 2 fixed).

Consider pairing it with a small hardening of the `updateResources` guard (Approach 2) so the
recreation is keyed off "are the expected user tables present?" rather than "is the table list
empty?", making startup recreation robust even if some unrelated object ever slips into the map
again. The PM/spec phase should decide whether to include that belt-and-braces change or keep
the fix minimal.

The exact filtering predicate (schema-name match vs. forcing `skipSysTables` for the in-mem H2
driver vs. a schema filter on the H2 connection) is an implementation detail for the spec — all
three converge on the same root cause.

## Git history findings

- The auto-create-on-startup block and its `getTableNames(true).size() == 0` guard live in
  `com.servoy.eclipse.core/src/com/servoy/eclipse/core/ServoyModel.java` (the `ServoyModel$13`
  runnable inside `updateResources`, ~line 1360-1400). Comment there: *"auto create in mem
  server's tables if dbis are available (very useful for working with test in mem DBs)"*.
- `createNewTableFromColumnInfo` is defined in
  `com.servoy.eclipse.core/src/com/servoy/eclipse/core/util/EclipseDatabaseUtils.java` and is
  called from exactly two places: this startup block and `SynchronizeDBIWithDBWizard` — i.e.
  the two flows named in the two ticket symptoms.
- `Server.loadTables(...)` (`j2db_server/src/com/servoy/j2db/server/persistence/Server.java`)
  filters system tables only when `skipSysTables == true` or the type name contains `SYS`;
  H2 2.x serves metadata from `INFORMATION_SCHEMA`, which passes both filters for this config
  (`skipSysTables == false`). This is consistent with an H2 major-version upgrade being the
  regression trigger; the spec phase should confirm the H2 version bump in the target platform
  git history as the introducing change.
