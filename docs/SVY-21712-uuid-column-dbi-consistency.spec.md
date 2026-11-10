# Spec: SVY-21712 — Inconsistent behavior with UUID columns in DBI files

## 1. Goal
When a new table is created in the Developer with the "Column Type" table-creation
setting on **UUID (DB native)**, the auto-created native-UUID primary-key column and any
additional native-UUID columns must be stored **identically and dialect-independently** in
the `.dbi` file. Today the auto-created PK decays, on save, to the realized DB type
(`dataType 1` / `CHAR(36)` on SQL Server, `dataType 2000` / `JAVA_OBJECT` on PostgreSQL),
while a dropdown-created UUID column keeps the canonical logical form (`dataType -4` /
`MEDIA`, `length 16`). This spec makes the native-UUID PK carry the same canonical
`configuredColumnType = MEDIA(16)` as a dropdown-created column — the single root-cause fix,
at the PK creation point in `ColumnComposite`. The result removes the confusing
"length 36 vs 16" display, removes the cross-dialect `.dbi` divergence, and fixes the
DBI-only WAR build mismatch described in the linked SVY-21701.

## 2. Background

### 2.1 Reported behavior
Creating table `test` with the native-UUID default:
1. The auto-created PK shows `test_uuid  UUID (DB Native)  length 16`.
2. A second UUID column added via the type dropdown shows `some_uuid  UUID (DB Native)  length 16`.
3. **After Save**, the PK flips to `length 36`; the second column stays `16`.
4. The two columns serialize very differently in the `.dbi`:
   - `test_uuid` (PK): `dataType 1` (CHAR), `length 36`, `flags 37`, **no** `compatibleColumnTypes`, `autoEnterType 2 / autoEnterSubType 3`.
   - `some_uuid`: `dataType -4` (MEDIA/LONGVARBINARY), `length 16`, `flags 36`, `compatibleColumnTypes "[[1,36,0]]"`, no auto-enter.

### 2.2 Root cause (from the triage report)
Both columns start logically identical — `MEDIA, length 16`, with `UUID_COLUMN | NATIVE_COLUMN`
flags — but take two different paths into `ColumnInfo.configuredColumnType`, and only one of
them ever writes a *configured* type:

- **PK path** (`ColumnComposite.initDataBindings`,
  `com.servoy.eclipse.ui/.../editors/table/ColumnComposite.java:700-704`): calls
  `t.createNewColumn(..., ColumnType.getInstance(MEDIA, 16, 0))` and sets the PK / UUID /
  NATIVE flags. `AbstractTable.createNewColumn` (`servoy_shared/.../persistence/AbstractTable.java:239`)
  builds `new Column(...)` and stores the type only in the column's `columnType` ("db type")
  field. **No `ColumnInfo` is attached here** — it is created lazily later, when the row is
  first selected, via `ColumnComposite.java:161 → DataModelManager.createNewColumnInfo →
  DatabaseUtils.createNewColumnInfo` (`servoy_shared/.../util/DatabaseUtils.java:255-269`),
  which sets flags but **never sets a `configuredColumnType`**. So the PK's configured type
  stays null and `Column.getConfiguredColumnType()`
  (`servoy_shared/.../persistence/Column.java:1166-1175`) falls back to `columnType`.

- **Dropdown path** (`ColumnTypeEditingSupport.setValue`,
  `com.servoy.eclipse.ui/.../editors/table/ColumnTypeEditingSupport.java:110-159`): the
  `UUID_NATIVE` branch sets `MEDIA, 16` plus the flags **and explicitly calls**
  `pi.getColumnInfo().setConfiguredColumnType(MEDIA(16))` (line 158). This is what keeps a
  dropdown-created column canonical.

On **Save**, `Server.syncTableObjWithDB` realizes the table in the DB (native type chosen per
dialect in `DBDependentHandler.createHibernateColumn:732-736`, keyed off the `NATIVE_COLUMN`
flag) and reads the columns back via JDBC, overwriting each column's `columnType` with the
DB-reported type (`Server.askTableForColumns`, `Server.java:2290` → `CHAR(36)` on SQL Server).
It then records the realized type as a *compatible* type **only when `configuredColumnType !=
columnType`** (`Server.java:3145-3152`):
- `some_uuid`: configured `MEDIA(16)` ≠ realized `CHAR(36)` → `CHAR,36` added to
  `compatibleColumnTypes`; configured type stays `MEDIA(16)`.
- PK: configured type is null → falls back to the just-overwritten `CHAR(36)` → configured ==
  db type → **no** compatible type recorded, and the PK's serialized type is now `CHAR(36)`.

### 2.3 Which representation is canonical
SVY-21701 settles this. Its root-cause analysis and shipped fix (servoy-client commit
`b3d595654`, `servoy_shared/.../persistence/Relation.java:832-847`) treat the native-UUID
column's **MEDIA (`dataType -4`), length 16, with `UUID_COLUMN | NATIVE_COLUMN`** as the
form the `.dbi` is "stored with", and the `CHAR` / `JAVA_OBJECT` forms as the volatile,
dialect-dependent live-DB reading. The canonical `.dbi` form is therefore exactly what
`some_uuid` already produces. The PK is the one that is wrong.

### 2.4 Serializer
The `.dbi` is produced by `DataModelManager.getColumnInfoDef` +
`serializeTableInfo`
(`com.servoy.eclipse.model/src/com/servoy/eclipse/model/repository/DataModelManager.java:883-968`).
`getColumnInfoDef` copies `cid.columnType = column.getConfiguredColumnType()` (line 891) and
`cid.compatibleColumnTypes = ci.getCompatibleColumnTypes()` (line 892) verbatim;
`serializeTableInfo` writes `dataType = cid.columnType.getSqlType()` (line 950) and `length`
(line 951). Normalizing `cid.columnType` here for UUID+NATIVE columns is the single choke
point that repairs every serialization path.

## 3. Design

### 3.1 Set `configuredColumnType = MEDIA(16)` on the native-UUID PK at creation (root-cause fix, UI side)
In `ColumnComposite.initDataBindings`
(`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/editors/table/ColumnComposite.java:700-704`),
after the PK column is created and flagged, ensure the column carries an explicit
`configuredColumnType` equal to the `PrimaryKeyType`'s logical type (the same
`MEDIA(16)` already passed to `createNewColumn`) — but **only for UUID key types**, so the
INTEGER PK path is unaffected.

The subtlety is timing: `createNewColumn` does **not** attach a `ColumnInfo`
(`getColumnInfo()` returns null immediately after). `ColumnTypeEditingSupport` can simply
call `pi.getColumnInfo().setConfiguredColumnType(...)` because by the time the dropdown
fires the row has been selected and the lazy `createNewColumnInfo` has run. At the
`initDataBindings` creation point there is no column info yet. The fix therefore:

- eagerly creates the column info for the new PK (calls the same
  `DataModelManager.createNewColumnInfo(id, false)` path already used on selection at
  `ColumnComposite.java:161`), then, for a UUID key type, sets
  `id.getColumnInfo().setConfiguredColumnType(ColumnType.getInstance(keyType.getColumnType(),
  keyType.getLength(), 0))` — mirroring `ColumnTypeEditingSupport.setValue:158`. Guard the
  call on `id.getColumnInfo() != null`.

Keep this change in the **eclipse repo** (`ColumnComposite`) only. Do **not** change the
shared `DatabaseUtils.createNewColumnInfo` in servoy-client: it is a lower-level, widely
reused creation path and a change there carries a much larger blast radius for a problem
that is fully fixable at the single PK-creation point here.

The goal is: immediately after creation the native-UUID PK's
`getConfiguredColumnType()` returns `MEDIA(16)` from a real column info, not from the
`columnType` fallback. Then the save/read-back cycle leaves the configured type at
`MEDIA(16)`, and `Server.java:3145-3152` records the realized `CHAR(36)` as a compatible
type — making the PK's `.dbi` entry structurally identical to `some_uuid` except for the PK
flag and the UUID-generator auto-enter (both correct).

### 3.2 No serializer-side normalization — why Fix 2 was rejected
An earlier draft proposed a guard in `DataModelManager.getColumnInfoDef` that would force
**any** `UUID_COLUMN | NATIVE_COLUMN` column to `MEDIA(16)` at serialization time, as a
self-healing measure for existing divergent `.dbi` files. **This is rejected as unsound.**
Not every native-UUID column is logically `MEDIA(16)`: the configured/realized type of a
UUID+NATIVE column can legitimately differ (it is not an invariant that the canonical form
is always `MEDIA` with length 16), so a blanket serializer override would **corrupt columns
that are correctly stored in another form** — it changes bytes it does not understand. The
serializer must continue to write whatever the column info actually holds. Fixing the one
creation path that fails to set a configured type (§3.1) is the correct, bounded change;
pre-existing divergent files are out of scope (see §6).

### 3.3 Why the runtime still realizes the correct native DB type
Storing `MEDIA(16)` in the `.dbi` does not change what the live DB column becomes:
`DBDependentHandler.createHibernateColumn` (`DBDependentHandler.java:732-736`) chooses the
native DB type from the `NATIVE_COLUMN` flag, not from the stored `dataType`. So both
SQL Server (`uniqueidentifier`) and PostgreSQL (`uuid`) are still produced correctly.

## 4. Implementation plan

1. **`ColumnComposite.initDataBindings`** (`com.servoy.eclipse.ui`,
   `src/com/servoy/eclipse/ui/editors/table/ColumnComposite.java:700-704`): for a UUID
   `keyType`, after creating and flagging the PK column, ensure it has a `ColumnInfo` with
   `configuredColumnType = ColumnType.getInstance(keyType.getColumnType(),
   keyType.getLength(), 0)` (MEDIA, 16), mirroring `ColumnTypeEditingSupport.setValue:158`.
   Prefer eagerly creating the column info via the existing
   `DataModelManager.createNewColumnInfo` path (as used at line 161); guard on non-null
   column info. Leave the INTEGER PK path unchanged. Add imports for `ColumnType` /
   `ColumnInfo` / `DataModelManager` only if not already present.
   Keep the change in the eclipse repo only — do **not** touch the shared
   `DatabaseUtils.createNewColumnInfo` in servoy-client (see §3.1).
2. **Verify the full round-trip:** create a native-UUID table, save, and confirm the PK's
   `.dbi` entry matches the spec in §5 (`dataType -4`, `length 16`, realized type in
   `compatibleColumnTypes`) and is structurally identical to a dropdown-created UUID column.
3. **Tests** (see acceptance criteria): add a test in `com.servoy.eclipse.model.tests`
   (or `com.servoy.eclipse.ui.tests` if the natural seam is the editor creation path) that
   drives the native-UUID PK creation + serialization and asserts the PK serializes as
   `dataType -4, length 16`, matching the dropdown-created column. Use
   `doReturn(...).when(mock).call()` for the persist mocks (per project conventions). Place
   it in `plainjunit` if it can run with mocked persists, or `integrationpde` if it needs
   workspace/OSGi state — justify the placement.
4. **After each code change:** run `tools["eclipse-ide"].getCompilationErrors`, organize
   imports, and run the relevant tests.

## 5. Acceptance criteria
- [ ] After creating a native-UUID table and saving, the auto-created PK column serializes
      in the `.dbi` as `dataType -4` (MEDIA), `length 16`, with the realized DB type recorded
      in `compatibleColumnTypes` (e.g. `"[[1,36,0]]"` on SQL Server).
- [ ] The PK's `.dbi` entry is structurally identical to a dropdown-created native-UUID
      column, differing only by the PK flag (`flags` includes `PK_COLUMN`) and the
      UUID-generator auto-enter (`autoEnterType 2 / autoEnterSubType 3`).
- [ ] The editor shows the native-UUID PK as `length 16` both before and after Save (no
      flip to 36).
- [ ] The `.dbi` for a newly-created native-UUID column is identical across SQL Server and
      PostgreSQL (no `dataType 1` vs `2000` divergence).
- [ ] The live DB column is still realized as the correct native type (`uniqueidentifier` on
      SQL Server, `uuid` on PostgreSQL) — no runtime regression.
- [ ] The INTEGER PK creation path is unchanged.
- [ ] A `com.servoy.eclipse.model.tests` test asserts the canonical serialization for a
      native-UUID column whose realized DB type is `CHAR(36)`.
- [ ] No new compilation errors; imports organized; the two highest Spotbugs severities clean
      in changed code.

## 6. Out of scope
- The SVY-21701 relation/calculation fix itself (already shipped in servoy-client
  `b3d595654`); this spec only removes the `.dbi` divergence that triggered it.
- Changing how the live DB realizes native-UUID columns per dialect
  (`DBDependentHandler` / dialect extensions) — those stay as-is.
- UUID columns that are **not** native (`UUID (MEDIA 16)` / `UUID (TEXT 36)` without the
  `NATIVE_COLUMN` flag) — their serialization is already consistent and is not changed.
- **Repairing pre-existing divergent `.dbi` files.** The fix corrects the creation path so
  newly-created native-UUID PKs are canonical. It does **not** rewrite tables already saved
  in the divergent form — a serializer-side normalization was considered and rejected as
  unsound (§3.2), because not every native-UUID column is logically `MEDIA(16)` and a blanket
  override would corrupt correctly-stored columns. No migration/rewrite tool is in scope.
- Any change to the shared `DatabaseUtils.createNewColumnInfo` in servoy-client.
- The VECTOR column path in `ColumnTypeEditingSupport`.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Scope of the fix: keep it in `ColumnComposite` (eclipse repo) only, not the shared `DatabaseUtils.createNewColumnInfo`. | — | resolved (user) |
| Serializer-side normalization guard (former "Fix 2"): rejected as unsound — not all native-UUID columns are logically MEDIA(16), so a blanket override would corrupt correctly-stored columns. | — | resolved (user) |
| Does driving the creation+serialization in a unit test need OSGi/workspace state (→ `integrationpde`), or can it run with mocked persists (→ `plainjunit`)? To be confirmed during test authoring. | dev | open |
