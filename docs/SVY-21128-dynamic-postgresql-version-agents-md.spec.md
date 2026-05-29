# Spec: SVY-21128 — Dynamic PostgreSQL Version in AGENTS.MD

## 1. Goal

Replace the hard-coded PostgreSQL version string in `SkillsZipExtractor` with the
actual version of the PostgreSQL server that Servoy is currently connected to in
the developer environment. This makes the `postgres_version` field written to the
project's `AGENTS.MD` file accurate for every developer's setup rather than fixed
at the compile-time constant `"17.6"`.

## 2. Background

### 2.1 Existing AGENTS.MD write path

`SkillsZipExtractor.writeOrUpdateAgentsMd()` stamps three dynamic values into the
YAML block at the top of `AGENTS.MD`:

| Field | Source |
|---|---|
| `servoy_version` | `ClientVersion.getMajorVersion()` / `getMiddleVersion()` |
| `postgres_version` | **hardcoded** `POSTGRES_VERSION = "17.6"` |
| `databases.active` | `getDatabaseNames()` via `ApplicationServerRegistry` |

The `databases.active` field already queries the live server registry; the
PostgreSQL version should do the same.

### 2.2 Servoy server registry

In the Eclipse developer context, `ApplicationServerRegistry.get().getServerManager()`
returns an `IServerManagerInternal`. Its `getServer(name, mustBeEnabled, mustBeValid)`
method returns an `IServer` reference that is, locally, always an instance of
`IServerInternal` (the full server implementation). Casting is safe and is already
done elsewhere in the codebase (e.g. `WorkspaceExporter`, `TypeCreator`).

`IServerInternal.getConnection()` returns an `ITransactionConnection`, which
extends `java.sql.Connection`. From a `Connection` the JDBC `DatabaseMetaData`
provides `getDatabaseMajorVersion()` and `getDatabaseMinorVersion()` — the exact
fields needed to construct a `"major.minor"` version string without executing any
SQL statement.

`IServer.getDatabaseProductName()` returns the JDBC product name (e.g.
`"PostgreSQL"`) and is used to detect which servers are PostgreSQL.

### 2.3 Fallback

When no PostgreSQL server is reachable (developer not running Servoy, no enabled
PostgreSQL server configured, or any exception) the existing constant
`POSTGRES_VERSION = "17.6"` is used as a safe fallback so that `AGENTS.MD`
always contains a reasonable value.

## 3. Design

### 3.1 New `getPostgresVersion()` helper

Add a package-private static method `getPostgresVersion()` to
`SkillsZipExtractor`:

```
1. If ApplicationServerRegistry does not exist → return POSTGRES_VERSION
2. Get enabled server names via getServerManager().getServerNames(true, false, false, false)
3. For each server name:
   a. Get IServer via getServer(name, true, false)   (enabled only)
   b. If null → skip
   c. Call server.getDatabaseProductName()
   d. If the product name contains "postgresql" (case-insensitive):
      - Cast server to IServerInternal
      - Call si.getConnection()
      - Read conn.getMetaData().getDatabaseMajorVersion() + getDatabaseMinorVersion()
      - Close the connection (try-with-resources)
      - Return "major.minor"
4. If no PostgreSQL server found → return POSTGRES_VERSION
```

Exceptions at any step are caught and cause a skip to the next server (or the
fallback). The method must not throw.

### 3.2 Wire `getPostgresVersion()` into `writeOrUpdateAgentsMd()`

In `writeOrUpdateAgentsMd()`, replace the line:

```java
String postgresVersion = POSTGRES_VERSION;
```

with:

```java
String postgresVersion = getPostgresVersion();
```

No other change to the method is needed.

### 3.3 Keep `POSTGRES_VERSION` constant

The constant `POSTGRES_VERSION = "17.6"` must remain — it is used by the fallback
path and is referenced in existing tests
(`SkillsZipExtractorTest.testUpdateAgentsYamlUpdatesPostgresVersion`).

## 4. Implementation plan

1. **Add `getPostgresVersion()` to `SkillsZipExtractor.java`** (package-private static):
   - Follows the algorithm in §3.1.
   - Uses `IServerInternal` (already in `servoy_shared`) and standard JDBC
     `DatabaseMetaData`.
   - Catches all exceptions per server; falls back to `POSTGRES_VERSION`.
   - Connection opened in try-with-resources for resource safety.

2. **Edit `writeOrUpdateAgentsMd()`** in `SkillsZipExtractor.java`:
   - Change `String postgresVersion = POSTGRES_VERSION;`
     to `String postgresVersion = getPostgresVersion();`.

3. **Post-edit workflow**:
   - `eclipse-coder_organizeImports`
   - `eclipse-coder_formatFile`
   - `eclipse-ide_getCompilationErrors` — fix all errors
   - `eclipse-ide_executeQuickFix` if quick fixes are available

## 5. Acceptance criteria

- [ ] When a PostgreSQL server is enabled and reachable in Servoy developer,
      `AGENTS.MD` is written with the live `major.minor` PostgreSQL version
      (e.g. `"17.6"`, `"16.3"`) rather than the constant `"17.6"`.
- [ ] When no PostgreSQL server is configured or the registry is unavailable,
      `AGENTS.MD` is written with the fallback `POSTGRES_VERSION` constant.
- [ ] A connection exception on one server does not prevent checking further
      servers in the list.
- [ ] The `POSTGRES_VERSION` constant remains in the class (not removed).
- [ ] No compilation errors remain in `com.servoy.eclipse.opencode`.
- [ ] Existing tests in `com.servoy.eclipse.opencode.tests` continue to pass.

## 6. Out of scope

- Supporting non-PostgreSQL databases (MySQL, Oracle, etc.) — those are not
  written into `AGENTS.MD`.
- Caching the detected version between sessions.
- Exposing the detected version outside `SkillsZipExtractor`.
- Any UI for the PostgreSQL version.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the patch version also be included (e.g. `"17.6.1"` vs `"17.6"`)? | Johan | resolved — major.minor only |
| If multiple PostgreSQL servers are configured, which one to use? | Johan | resolved — first enabled one found |
