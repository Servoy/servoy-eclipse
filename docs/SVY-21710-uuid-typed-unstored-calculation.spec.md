# Spec: SVY-21710 — Allow an unstored calculation to be a real UUID type in Servoy

## 1. Goal
Let a developer mark an unstored calculation as a UUID from the calculations editor ("Returned type" combo), persist that marker in the existing calc format, and have `Relation.checkKeyTypes` treat the calc as a UUID. A UUID calc then matches a native-UUID (MEDIA + `UUID_COLUMN`) foreign column through the normal `isUUID(primary) && isUUID(foreign)` rule. The SVY-21701 allowance (a TEXT calc is accepted against a native-UUID column) stays, for backward compatibility.

The work spans two repositories:
- **servoy-client** (`/home/gabi/github_master/servoy-client`): `Relation`, `ScriptCalculation`, tests.
- **servoy-eclipse** (this repo): the calculations editor UI.

## 2. Background

Based on `docs/SVY-21710-triage.md` and a re-read of the code.

### 2.1 The persistent marker already exists
- `ScriptCalculation.getFlags()` returns `ScriptVariable.getFlags(getDeclaration())`.
- `ScriptVariable.getFlags(String)` returns `NORMAL_COLUMN | UUID_COLUMN` when the first JSDoc block (up to the first `*/`) has a `@type` line containing `{UUID}`.
- A UUID calc is stored as `type:-4` (MEDIA) in `@properties={...}`, the same shape as a native UUID column.
- Consumers that already honour the flag: `ServoyFormBuilder.checkValueListRealValueToDataProviderTypeMatch`, `SQLGenerator` (via `getFlags()` into `Column.getAsRightType`), and `JSCalculation.hasFlag`.

### 2.2 The gap in `Relation.isUUID`
`Relation.isUUID(IDataProvider)` (servoy_shared `Relation.java`, ~l.910) works like this:
- For any `AbstractBase` it checks `getSerializableRuntimeProperty(IScriptProvider.TYPE) == "UUID"`.
- For a `Column` it checks `hasFlag(UUID_COLUMN)`.

`ScriptCalculation` extends `AbstractScriptProvider`, which extends `AbstractBase`, so it takes the first branch. `IScriptProvider.TYPE` comes from the `jsType` attribute, which `SolutionDeserializer` sets only for variables, never for functions or calcs. So `isUUID(calc)` is always false, even with a correct `@type {UUID}`. This is the real reason SVY-21701 needed the TEXT-calc workaround.

### 2.3 The UI gap
`CalculationTypeEditingSupport` (`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/editors/table/`):
- Builds its combo from `Column.allDefinedTypes` (TEXT, INTEGER, NUMBER, DATETIME, MEDIA).
- `setValue` only calls `setTypeAndCheck` and rewrites the default `return ...;` line when the user hasn't customised it.
- It never touches the JSDoc, and `getValue` only looks at `getType()`.

`CalculationLabelProvider` (`CI_TYPE` column) shows `Column.getDisplayTypeString(mapToDefaultType(getType()))`, so a UUID calc would show as "MEDIA".

### 2.4 The sample solution uses `@return {UUID}`
`ws_demo/uuidCalc/.../uc_parent_calculations.js` declares the calc with `@return {UUID}` and `type:-4`, not `@type {UUID}`. `getFlags()` ignores `@return`.

### 2.5 Git history
- servoy-client `b3d5956546` (SVY-21701): added `primaryType == TEXT && primary[i] instanceof ScriptCalculation` in `Relation.checkKeyTypes`. Keep it.
- servoy-client `6c85fc0a58` (SVY-18643): put the calc `@type {UUID}` parsing in `ScriptVariable.getFlags(String)`. This confirms `@type {UUID}` is the intended marker.
- servoy-client `0ac67da7059` (2020): introduced `Relation.isUUID` on the `TYPE` property.
- servoy-client `ca1d6ca61d` (SVY-21511): UUID array key handling. It uses `isUUID(foreign[i])` and the `TYPE` property on the primary. Do not change that.
- No prior spec for SVY-21701 or SVY-21710 exists in `docs/`.

## 3. Design

### 3.1 Persistence format (no file-format change)
A UUID calc is a calc with:
- `type` = MEDIA (`-4`), and
- a `@type {UUID}` line in the first JSDoc block of its declaration.

Example:
```js
/**
 * @type {UUID}
 *
 * @properties={type:-4,typeid:36,uuid:"..."}
 */
function calc_uuid() {
	return parent_uuid;
}
```
No new `IColumnTypes` value and no new persisted property. This is approach 1 of the triage.

### 3.2 `@return {UUID}` as a marker (recommended; see open question 1)
- `ScriptCalculation.getFlags()` also returns `NORMAL_COLUMN | UUID_COLUMN` when the first JSDoc block has an `@return` line containing `{UUID}`.
- This is done in `ScriptCalculation` only, through a small private or static helper. `ScriptVariable.getFlags(String)` keeps its current `@type`-only behaviour, because variables carry their type via `jsType`.
- Reason: the sample solution and code completion already use `@return {UUID}`, and it is the natural JSDoc for a function.
- The UI helper (3.4) must understand both tags, otherwise the combo cannot switch a calc back from UUID.

Also harden the existing parsing while touching it. `ScriptVariable.getFlags(String)` calls `comment.indexOf('\n', typeIndex)` and uses it as a `substring` end. It throws `StringIndexOutOfBoundsException` if there is no newline after `@type`. Fall back to the `*/` index in that case.

### 3.3 `Relation.isUUID` (servoy-client)
Check `ScriptCalculation` before the generic `AbstractBase` branch:

```java
IDataProvider real = ...; // unchanged ColumnWrapper unwrapping
if (real instanceof ScriptCalculation)
{
    return mapToDefaultType(real.getDataProviderType()) == IColumnTypes.MEDIA
        && real.hasFlag(IBaseColumn.UUID_COLUMN);
}
if (real instanceof AbstractBase) { ...unchanged TYPE == "UUID"... }
if (real instanceof Column) { ...unchanged... }
```

- The MEDIA guard stops a TEXT calc that carries a stray `@type {UUID}` from being treated as a native UUID. That case stays covered by the SVY-21701 allowance.
- Variables keep the `TYPE == "UUID"` check.
- In `checkKeyTypes`, the branch `if (isUUID(foreign[i])) { if (isUUID(primary[i])) continue; if (TEXT && ScriptCalculation) continue; }` stays as is.
- The final `primaryType != foreignType` check already passes for a MEDIA calc against a MEDIA foreign column. The change matters for the explicit UUID-to-UUID branch and for any `isUUID(primary)` use.
- Only `isUUID` changes. The `typeProperty` logic for arrays is untouched, since calcs have no `TYPE` runtime property.

### 3.4 Calculations editor UI (servoy-eclipse)

**New helper `CalculationUuidMarker`** in `com.servoy.eclipse.ui.editors.table`, a plain Java class with no SWT or OSGi dependency so it is unit-testable:
- `static boolean hasUuidMarker(String declaration)`: true if the first JSDoc block has a `@type` or `@return` line containing `{UUID}`.
- `static String setUuidMarker(String declaration, boolean uuid)`:
  - `uuid == true`:
    - If a marker already exists, return the declaration unchanged.
    - Otherwise insert ` * @type {UUID}` into the first JSDoc block, before the `@properties` line, or before the closing `*/` if there is none.
    - If the declaration has no leading JSDoc block, create a `/** ... */` block. Use the file's line delimiter and the ` * ` prefix style.
  - `uuid == false`:
    - Remove any ` * @type {UUID}` line.
    - Remove any `@return {UUID}` line (the `{UUID}` return tag, whole line) from the first block.
    - Leave other tags and text untouched. Do not leave an empty ` *` line behind if the block would otherwise gain a double blank line.

**`CalculationTypeEditingSupport` changes:**
- Combo items: the existing five entries in `Column.allDefinedTypes` order, plus a final **"UUID"** entry (index `Column.allDefinedTypes.length`). The label is a plain string, with no new message constant unless the other labels use one.
- `setValue(element, value)`:
  - Index < `allDefinedTypes.length`: set the type as today and call `setUuidMarker(declaration, false)`, so choosing a normal type clears the UUID marker.
  - Index == UUID: `setTypeAndCheck(IColumnTypes.MEDIA, ...)` and `setUuidMarker(declaration, true)`.
  - Keep the existing "replace default `return ...;`" behaviour, using the old type to detect an untouched default body. Apply it after the type change so it works for UUID too (default `return null;`).
  - Declaration edits go through `calculation.setDeclaration(...)`, as the existing code does.
- `getValue`: if `type == MEDIA` and the calc has the UUID marker, return the UUID index. Otherwise return the existing index.
- Stored calcs: if a table column with the calc's name exists, the type follows the column (this is what `setTypeAndCheck` already does). The UUID entry must not write the tag in that case. Skip the tag edit, and `getValue` shows the normal mapped type. The ticket scope is unstored calcs only.
- Fire the change event and update the viewer as today.

**`CalculationLabelProvider`:** in `CI_TYPE`, return `"UUID"` when `calculation.getFlags() & IBaseColumn.UUID_COLUMN != 0` and the mapped type is MEDIA. Otherwise return the existing text.

No MANIFEST change is expected. `com.servoy.eclipse.ui` already exports `com.servoy.eclipse.ui.editors.table`, and `IBaseColumn` is reachable through the existing j2db dependency.

### 3.5 Backward compatibility
- The SVY-21701 TEXT-calc allowance remains.
- Existing MEDIA calcs without a marker behave as before. Nothing is auto-migrated.
- Solutions that already have `@type {UUID}` on a calc start being accepted by `Relation` (previously silently ignored there). This is intended.
- Optional follow-up (not in this case): a builder warning for a TEXT calc used against a UUID column, suggesting `@type {UUID}`.

## 4. Implementation plan

**servoy-client (`servoy_shared`)**
1. `ScriptCalculation.getFlags()`: keep `ScriptVariable.getFlags(getDeclaration())` and add the `@return {UUID}` detection (3.2), if the open question is confirmed.
2. `ScriptVariable.getFlags(String)`: guard against `indexOf('\n') == -1` (3.2).
3. `Relation.isUUID`: add the `ScriptCalculation` branch ahead of the `AbstractBase` branch (3.3).

**servoy-client tests (`servoy_ngclient.tests`)**

4. Extend `RelationUuidArrayKeyTypeTest`. Add a new nested class and a `runCalc(...)` helper that:
   - builds a primary `ScriptCalculation` (via `createNewObject` under a `TableNode` or the dummy parent; `getTable()` failures are caught in `getDataProviderType`),
   - sets `setType(MEDIA)` and a declaration with the requested JSDoc,
   - extends `StubDataProviderHandler` so `getDataProviderForTable` returns the calc for the primary table (the current stub returns null there).

   Cases:
   - MEDIA calc with `@type {UUID}` vs native-UUID column: accepted (`assertNull`).
   - MEDIA calc with `@return {UUID}` vs native-UUID column: accepted.
   - MEDIA calc without a marker vs native-UUID column: the outcome is whatever today's final type check gives. Assert the real behaviour (MEDIA vs MEDIA passes). If the implementer finds it differs, record it.
   - TEXT calc, no marker vs native-UUID column: still accepted (SVY-21701 regression guard).
   - TEXT calc with `@type {UUID}` vs native-UUID column: accepted via the allowance, and `isUUID` is false because of the MEDIA guard.
   - MEDIA calc with `@type {UUID}` vs non-UUID MEDIA column: accepted by type equality (MEDIA == MEDIA), same as a non-UUID MEDIA variable.
   - MEDIA calc with `@type {UUID}` vs TEXT or INTEGER column: rejected (`assertNotNull`).
   - INTEGER calc vs native-UUID column: rejected.
5. Add a `ScriptVariable.getFlags(String)` test for `@type {UUID}` as the last line with no trailing newline. Put it where the project keeps plain persistence tests.

**servoy-eclipse (`com.servoy.eclipse.ui`)**

6. Create `CalculationUuidMarker` (3.4).
7. Update `CalculationTypeEditingSupport` (combo entry, `setValue`, `getValue`, stored-calc guard).
8. Update `CalculationLabelProvider` so `CI_TYPE` shows "UUID".

**servoy-eclipse tests**

9. Add `CalculationUuidMarkerTest` in `com.servoy.eclipse.ui.tests/src/test/java/com/servoy/eclipse/ui/tests/plainjunit/editors/table/` (JUnit 6, plain, no OSGi). Cases:
   - add the marker to a declaration with `@properties`;
   - add to a declaration with no JSDoc;
   - add is idempotent;
   - detect `@type {UUID}`;
   - detect `@return {UUID}`;
   - remove `@type {UUID}`;
   - remove `@return {UUID}`;
   - a non-UUID `@type {String}` or `@return {String}` is neither detected nor removed;
   - unrelated tags (`@properties`, `@param`) are preserved byte-for-byte;
   - CRLF declarations keep CRLF.
10. Run `eclipse-ide_runJUnitTests` with `projectName=com.servoy.eclipse.ui.tests`, `packageName=com.servoy.eclipse.ui.tests.plainjunit.editors.table`, `junitVersion=6`. Run the servoy-client test via its own build.

**Build and verification**

11. After each edit: `getCompilationErrors`, `organizeImports`, `formatFile`. Both repos must compile. servoy-eclipse consumes servoy-client, so the servoy-client change must be available (workspace project or built jar) before the UI code is checked.
12. Commit order: servoy-client first, then servoy-eclipse. Each commit subject has `SVY-21710` and, if mostly AI-generated, `[ai]`. Ask for permission before every commit.

## 5. Acceptance criteria
- [ ] The calculations editor "Returned type" combo has a **UUID** entry.
- [ ] Selecting UUID on an unstored calc stores type MEDIA and adds `@type {UUID}` to the first JSDoc block of the declaration.
- [ ] Selecting any other type on a UUID calc sets that type and removes the UUID marker (`@type {UUID}` and `@return {UUID}`).
- [ ] The combo shows UUID for an unstored MEDIA calc that has a UUID marker, and the type column shows "UUID".
- [ ] `ScriptCalculation.getFlags()` includes `UUID_COLUMN` for `@type {UUID}` and, if confirmed, `@return {UUID}`.
- [ ] `Relation.checkKeyTypes` accepts a MEDIA calc with a UUID marker against a native-UUID foreign column through the `isUUID` rule.
- [ ] A TEXT calc is still accepted against a native-UUID column (SVY-21701 unchanged).
- [ ] A UUID-marked calc is still rejected against a non-MEDIA, non-UUID column (for example TEXT or INTEGER). Against a plain MEDIA column behaviour is unchanged (accepted by type equality).
- [ ] `Relation.isUUID` for variables, columns and array key checks (SVY-21511) is unchanged. `RelationUuidArrayKeyTypeTest` passes unchanged.
- [ ] A stored calc is not given a `@type {UUID}` tag by the combo.
- [ ] No file-format change. Solutions saved before this change load and behave as before.
- [ ] New and extended tests pass. There are no compilation errors and no new high-severity Spotbugs findings.

## 6. Out of scope
- Removing or narrowing the SVY-21701 TEXT-calc allowance.
- A new persisted type value or property for calcs.
- Stored calcs (they follow the backing column type).
- Using the UUID marker for stored calcs in `Relation.isUUID`.
- Builder warnings or markers suggesting `@type {UUID}`.
- Setting `jsType` or `IScriptProvider.TYPE` for calcs in `SolutionDeserializer`.
- Auto-migrating existing calcs.
- Running the `uuidCalc` sample solution in a live IDE. The triage did not verify it either.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Should `@return {UUID}` count as a UUID marker for calcs (recommended, since the sample solution and code completion use it)? If not, the sample solution needs `@type {UUID}` instead, and step 1 plus the `@return` handling in `CalculationUuidMarker` and its tests are dropped. | Product/dev lead | open (spec assumes yes) |
| Is the MEDIA guard in `Relation.isUUID` for calcs correct, so that a TEXT calc with a stray `@type {UUID}` is not a native UUID and is accepted only through the SVY-21701 allowance? | Dev lead | open (spec assumes yes) |
| Should the combo write the tag for stored calcs (type follows the column) or skip it, as currently specified? | Dev lead | open (spec assumes skip) |
| Should a builder warning for TEXT calcs against UUID columns be added in a follow-up case? | Product | open |
| Does any other consumer read `IScriptProvider.TYPE` on calcs (not verified in triage)? | Implementer | open |
