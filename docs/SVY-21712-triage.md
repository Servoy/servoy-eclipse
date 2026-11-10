# Triage Report — SVY-21712

**Verdict:** PROCEED — recommended approach: normalize native-UUID columns to the canonical logical type (MEDIA, length 16, UUID+NATIVE flags) on the serialization side, so the `.dbi` is dialect-independent and both UUID columns serialize identically. This is the representation SVY-21701's fix and Johan Compagner's root-cause comment treat as correct.

## Reported problem
When creating a new table in the IDE with the "Column Type" table-creation setting set to
**UUID (DB native)**, the UUID columns end up stored inconsistently in the `.dbi` file, and
the editor shows confusing values:

1. Create table `test`. The auto-created PK column shows: `test_uuid  UUID (DB Native)  length 16`.
2. Add a second UUID column via the type editor; it shows: `some_uuid  UUID (DB Native)  length 16`.
3. **Save.** The PK column now shows `length 36` (not 16), while the second column stays 16.
4. In the `.dbi`, the two columns are serialized very differently:
   - `test_uuid` (the PK): `dataType 1` (CHAR), `length 36`, `flags 37`, **no** `compatibleColumnTypes`,
     carries `autoEnterType 2 / autoEnterSubType 3` (UUID generator).
   - `some_uuid`: `dataType -4` (LONGVARBINARY/MEDIA), `length 16`, `flags 36`,
     `compatibleColumnTypes "[[1,36,0]]"`, no auto-enter.

The reporter also notes a dialect difference: a native-UUID PK serializes as `dataType 1`
(SQL Server `uniqueidentifier` → CHAR) but as `dataType 2000` (JAVA_OBJECT) on PostgreSQL —
and that this inconsistency can cause the DBI-only WAR build mismatch described in the linked
SVY-21701.

The ticket proposes **no specific solution** — it reports the inconsistency and asks for it to
be made consistent/sane.

## Root-cause assessment

Both UUID columns start life logically identical — `MEDIA, length 16`, with the
`UUID_COLUMN`+`NATIVE_COLUMN` flags — but the PK and the second column take two different code
paths into `ColumnInfo.configuredColumnType`, and only the second path ever writes a *configured*
type. The divergence is then frozen in by a DB roundtrip on save. Traced step by step:

### 1. NewTableAction → empty table, no columns
`NewTableAction.run()`
(`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/views/solutionexplorer/actions/NewTableAction.java:139-140`)
only calls `s.createNewTable(validator, name)` and opens the editor. It creates **no** columns;
the PK is created later by the editor. So the PK is born inside the table editor, not here.

### 2. TableEditor auto-creates the PK column (`test_uuid`)
`ColumnComposite.initDataBindings(ITable)`
(`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/editors/table/ColumnComposite.java:674-711`):
when the table has zero columns it reads `new DesignerPreferences().getPrimaryKeyUuidType()`
(default `PrimaryKeyType.UUD_NATIVE`, `DesignerPreferences.java:155`), which is
`UUD_NATIVE(16, IColumnTypes.MEDIA, true, true)` (`PrimaryKeyType.java:33`). It then does
(`ColumnComposite.java:700-704`):

```java
Column id = t.createNewColumn(nameValidator, colname, ColumnType.getInstance(keyType.getColumnType(), keyType.getLength(), 0)); // MEDIA, 16
id.setDatabasePK(true);
id.setSequenceType(defaultFirstColumnSequenceType); // UUID_GENERATOR
id.setFlag(IBaseColumn.UUID_COLUMN, keyType.isUUID());   // true
id.setFlag(IBaseColumn.NATIVE_COLUMN, keyType.isNative()); // true
```

`AbstractTable.createNewColumn` (`servoy_client/servoy_shared/.../persistence/AbstractTable.java:236-243`)
builds `new Column(this, colname, columnType, false)`; the `Column` constructor
(`Column.java:96-102`) stores the passed type into `this.columnType` (the "db type" field) via
`updateColumnType`. **No `ColumnInfo` is attached yet**, so `configuredColumnType` is unset.
`Column.getConfiguredColumnType()` (`Column.java:1166-1175`) falls back to `columnType`, i.e.
`MEDIA(16)` — which is why the PK initially *displays* `UUID (DB Native) length 16` via
`ColumnLabelProvider.getColumnText`/`getColumnTypeText` (`ColumnLabelProvider.java:97,117,122,149-156`).
At this moment the PK has the type **only in `column.columnType`, never in a configured type**.

### 3. Add the second UUID column (`some_uuid`) via the type dropdown
`ColumnTypeEditingSupport.setValue`
(`com.servoy.eclipse.ui/.../editors/table/ColumnTypeEditingSupport.java:110-159`), UUID_NATIVE branch:

```java
type = IColumnTypes.MEDIA; length = 16;
column.setFlag(IBaseColumn.UUID_COLUMN, true);
column.setFlag(IBaseColumn.NATIVE_COLUMN, true);
...
ColumnType newColumnType = ColumnType.getInstance(type, length, 0); // MEDIA, 16
if (pi.getColumnInfo() != null) pi.getColumnInfo().setConfiguredColumnType(newColumnType); // ← writes configured MEDIA(16)
if (!pi.getExistInDB()) pi.updateColumnType(newColumnType);
```

Crucially this path **sets `configuredColumnType = MEDIA(16)` explicitly** on the column info (line
158). The PK never goes through here, so the PK's column info — once created — has a **null**
`configuredColumnType`.

### 4. Save → table realized in the DB, then columns read back
Saving calls `Server.syncTableObjWithDB` (`server/j2db_server/.../persistence/Server.java:2972`).
It creates the table/columns. The *native* DB type is chosen per dialect in
`DBDependentHandler.createHibernateColumn`
(`server/j2db_server/src/com/servoy/j2db/dblayer/DBDependentHandler.java:732-736`):

```java
if (col.hasFlag(IBaseColumn.NATIVE_COLUMN) && dialect instanceof DialectExtensionNativeColumntype ext)
    c.setSqlType(ext.getNativeColumntype(col));
```

→ `uniqueidentifier` on SQL Server (`SQLServerDialectExtension.java:206-212`), `uuid` on
PostgreSQL (`PostgreSQLDialectExtension.java:433-438`).

After creating, `Server.askTableForColumns` (`Server.java:2212`, called from line 3138) re-reads
the live column via JDBC `DatabaseMetaData`. For each DB column it does
`c.updateColumnType(columnType)` (`Server.java:2290`) with the DB-reported type, and if the dialect
flags it, re-sets `isUUID`/`isNativetype` (`Server.java:2297-2306`). SQL Server reports
`uniqueidentifier` back as `Types.CHAR` and `convertColumnMetaInfo` maps it to `isUUID/isNativetype`
(`SQLServerDialectExtension.java:185-191`) with `COLUMN_SIZE = 36`. So after read-back the PK's
`column.columnType` becomes **CHAR(36)**.

Then `Server.syncTableObjWithDB` records the realized type as a *compatible* type
(`Server.java:3145-3152`):

```java
for (Column c : createdColumns)
    if (c.getColumnInfo() != null && !c.getColumnType().equals(c.getConfiguredColumnType()))
        c.getColumnInfo().addCompatibleColumnType(c.getColumnType());
```

This fires **only when `configuredColumnType != columnType`**. For `some_uuid` the configured type
was explicitly set to MEDIA(16) in step 3, the DB realized it as CHAR(36), so `MEDIA(16) != CHAR(36)`
→ `compatibleColumnTypes` gets `CHAR,36,0` added → serialized as `"[[1,36,0]]"`. For the PK, its
column info's `configuredColumnType` is **null**, so `getConfiguredColumnType()` falls back to
`columnType`, which was just overwritten to CHAR(36) — i.e. configured == db type → the `!=` test is
false → **no compatible type is added** for the PK.

### 5. Why each `.dbi` field differs — exact serializer lines
The serializer is `DataModelManager.getColumnInfoDef` + `serializeTableInfo`
(`com.servoy.eclipse.model/src/com/servoy/eclipse/model/repository/DataModelManager.java:883-962`).
It copies the live column state verbatim:

- `cid.columnType = column.getConfiguredColumnType()` (`DataModelManager.java:891`) →
  written as `dataType` at `serializeTableInfo` line **950** (`cid.columnType.getSqlType()`) and
  `length` at line **951**.
  - PK: configured type is null → falls back to `columnType` = **CHAR(36)** → `dataType 1, length 36`.
  - `some_uuid`: configured type was set to MEDIA(16) in step 3 → `dataType -4, length 16`.
- `cid.compatibleColumnTypes = ci.getCompatibleColumnTypes()` (`DataModelManager.java:892`) →
  written at line **954-958**.
  - PK: none added (step 4) → field omitted.
  - `some_uuid`: `[CHAR,36,0]` added → `"[[1,36,0]]"`.
- `cid.flags = column.getFlags()` (`DataModelManager.java:894`) → written at line **960**.
  Flag constants (`servoy_base/.../IBaseColumn.java:30-35`): `PK_COLUMN=1`, `UUID_COLUMN=4`,
  `NATIVE_COLUMN=32`. PK = 1+4+32 = **37**; `some_uuid` = 4+32 = **36**.
- `cid.autoEnterType`/`autoEnterSubType = ci.getAutoEnterType()/getAutoEnterSubType()`
  (`DataModelManager.java:896-897`) → written at lines **964-967**. Only the PK has
  `sequenceType = UUID_GENERATOR` (`ColumnInfo.UUID_GENERATOR = 3`, set in step 2 at
  `ColumnComposite.java:702`), which maps to `autoEnterType 2 (SEQUENCE_AUTO_ENTER) / subType 3` →
  PK gets `autoEnterType 2, autoEnterSubType 3`; `some_uuid` has none.

The editor's post-save "length 36" flip (step 3 of the report) is the same effect surfacing through
`ColumnLabelProvider` line 122, which reads `getConfiguredColumnType().getLength()` — now CHAR(36)
for the PK because of the read-back at `Server.java:2290`.

**Net:** the divergence is not two UI paths disagreeing cosmetically; it is that the PK's configured
type is **never written** (so it decays to the dialect-realized DB type on read-back), while a
dropdown-created column **keeps** its configured MEDIA(16) and records the DB type only as
`compatibleColumnTypes`. Both are "the same column" conceptually; one stores the logical form, the
other stores the realized form.

## Ticket premise check / Recommendation — which representation is correct

SVY-21701 settles it. Johan Compagner's root-cause comment on SVY-21701 (3 Nov 2026) states, verbatim:

- "The Cloud/WAR build is a DBI_ONLY_EXPORT: it follows the `.dbi` exactly. The column is stored
  there with `dataType -4` (SQL LONGVARBINARY), which Servoy maps to **MEDIA**."
- "In Developer the live database is consulted instead of the `.dbi`, and the column there reports
  type 2000 (JAVA_OBJECT) … maps to TEXT."

That is, SVY-21701 treats **MEDIA (`dataType -4`) as the value that a native-UUID column is "stored
with" in the `.dbi`**, and treats the CHAR/JAVA_OBJECT forms as the volatile, dialect-dependent live-DB
reading. SVY-21701's shipped fix (`servoy-client` commit `b3d595654`,
`servoy_shared/src/com/servoy/j2db/persistence/Relation.java:832-847`) special-cases exactly this —
`if (isUUID(foreign[i])) { ... if (primaryType == TEXT && primary instanceof ScriptCalculation) continue; }` —
i.e. it standardizes on the native-UUID column being recognized by its **MEDIA + UUID_COLUMN** logical
identity, independent of what the live DB reports. The whole point of that fix is that the `.dbi`'s
MEDIA form is the canonical one and the builder must reason about it as a UUID column.

Therefore the **correct `.dbi` representation for a "UUID (DB native)" column is the dialect-independent
logical form: `dataType -4` (MEDIA), `length 16`, flags carrying `UUID_COLUMN|NATIVE_COLUMN`** — exactly
what `some_uuid` already produces, and what `ColumnTypeEditingSupport.setValue` writes. The PK column is
the one that is wrong: it loses its configured MEDIA(16) because no code ever sets a configured type on
the auto-created PK's column info, so it decays to the realized CHAR(36)/`uuid` type on read-back.

This makes the fix direction concrete and avoids the earlier NEEDS_INPUT questions:
- **Canonical form = MEDIA(16) + UUID + NATIVE** — decided by SVY-21701, not guessed.
- The second column (`some_uuid`) is already canonical; the work is to make the PK match it.
- Making both canonical also removes the `dataType 1`-vs-`2000` dialect dependency from the `.dbi`,
  which is the precise portability problem that triggered SVY-21701's DBI-only WAR mismatch.

### Recommended approach
Ensure the native-UUID PK carries an explicit `configuredColumnType = MEDIA(16)` on its column info so
it never decays to the realized DB type — mirroring what `ColumnTypeEditingSupport.setValue` already
does for dropdown-created UUID columns. Two viable insertion points:

1. **At creation (preferred, UI side):** in `ColumnComposite.initDataBindings`
   (`ColumnComposite.java:700-704`), after the PK column gets its column info, set
   `configuredColumnType` to the `PrimaryKeyType`'s logical `MEDIA(16)` (the same value already passed
   to `createNewColumn`). This keeps the PK logical form stable across the save/read-back cycle, so on
   serialize `getConfiguredColumnType()` returns MEDIA(16) and `addCompatibleColumnType` records
   CHAR(36) as a compatible type — making the PK's `.dbi` entry structurally identical to `some_uuid`
   (differing only by the PK flag and the UUID-generator auto-enter, which are correct).

2. **At serialization (model side, belt-and-braces / fixes existing `.dbi` files):** in
   `DataModelManager.getColumnInfoDef` (`DataModelManager.java:883-917`), when a column has
   `UUID_COLUMN|NATIVE_COLUMN`, normalize the emitted `columnType` to the canonical MEDIA(16) form
   regardless of the realized DB type. This also repairs already-divergent tables on next save.

Option 1 is the root-cause fix; option 2 guarantees consistency even for columns created before the
fix and for any other creation path that forgets to set the configured type. Implementing option 1,
with option 2 as a serialization-time guard, gives both a correct source of truth and a self-healing
`.dbi`.

## Approaches considered
1. **Set the native-UUID PK's configured type to MEDIA(16) at creation (ColumnComposite) and/or
   normalize in the serializer (DataModelManager).** — *Recommended.*
   - Pros: dialect-independent, portable `.dbi`; both UUID columns serialize identically (bar the PK
     flag + UUID-generator auto-enter); removes the "length 36 vs 16" confusion; removes the
     `dataType 1` vs `2000` divergence that SVY-21701 had to work around; matches the representation
     SVY-21701 already treats as canonical.
   - Cons: changes `.dbi` bytes for existing native-UUID PK tables (a diff/re-sync on next save); must
     confirm the runtime still realizes the correct native DB type from the canonical MEDIA(16) form —
     it does, because `DBDependentHandler.createHibernateColumn` keys off the `NATIVE_COLUMN` flag
     (`DBDependentHandler.java:732-736`), not off the stored `dataType`.
2. **Make the two editor paths agree pre-save only.** Rejected: the real decay happens on the DB
   read-back (`Server.java:2290` + the `addCompatibleColumnType` guard at `Server.java:3148`), not in
   the editor; a UI-only change does not stop the PK's null configured type from falling back to CHAR(36).
3. **Accept the realized DB type for both columns (force `some_uuid` through the same decay as the PK).**
   Rejected: still dialect-dependent (`dataType 1` on SQL Server vs `2000` on PostgreSQL), so the `.dbi`
   stays non-portable — the exact opposite of what SVY-21701 needs, and contradicts the comment that
   treats MEDIA as the stored form.
4. **No code change / document as expected.** Rejected: SVY-21701 proves the inconsistency causes a real
   DBI-only WAR build failure, so "working as designed" is not defensible.

## Git history findings
- `50f2b267a8` / `4ad8993c99` (SVY-13580) introduced UUID / native-UUID column support — the origin of
  the MEDIA(16)+UUID+NATIVE encoding and the two separate creation paths.
- `ff15e932c7` (SVY-20445) fixed an NPE when changing the type of a native-UUID PK on a new table —
  confirms the native-UUID PK path has repeatedly needed patching.
- `6a95cafbc1` (2026-06, Johan Compagner) authored the current `ColumnTypeEditingSupport` native-UUID
  branch (lines 110-130) — the path that *does* set the configured type.
- `434e212c67` (SVY-20207, 2025-04) last touched `ColumnComposite.initDataBindings` — the PK-creation
  path that does *not* set a configured type.
- **SVY-21701 fix: `servoy-client` commit `b3d595654` "SVY-21701 accept a TEXT calc against a native-UUID
  relation column [ai]"** (Johan Compagner, 3 Nov 2026), touching
  `servoy_shared/src/com/servoy/j2db/persistence/Relation.java` — the special-case that recognizes
  native-UUID columns by their MEDIA+UUID_COLUMN logical identity and is the authority for which `.dbi`
  form is canonical. Fixed on `lts_2026`; SVY-21701 fix versions `2026.12` and `2026.3.3 LTS`.
- No dependency/target-platform version change is involved; this is an unreconciled design seam between
  the two column-creation paths, not a version regression.
- No prior `docs/SVY-21712*.spec.md` exists.
