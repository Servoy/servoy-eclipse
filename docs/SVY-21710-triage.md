# Triage Report — SVY-21710

**Verdict:** PROCEED

## Reported problem
An unstored calculation cannot be marked as UUID in Servoy. The "Returned type" dropdown in the calculations editor only offers TEXT, INTEGER, NUMBER, DATETIME and MEDIA. The ticket says adding `@type {UUID}` in the calc script has no effect. As a result `Relation.checkKeyTypes` cannot treat such a calc as UUID-compatible with a native-UUID (MEDIA + UUID_COLUMN) foreign column. SVY-21701 worked around this with a "TEXT ScriptCalculation is accepted against a native-UUID foreign column" special case.

Proposed solution in the ticket: make an unstored calc genuinely UUID-typed and persisted, so the normal `isUUID(primary) && isUUID(foreign)` rule applies. Then possibly narrow or remove the SVY-21701 special case.

## Root-cause assessment
The ticket's premise ("no persistent marker exists, `@type {UUID}` has no effect") is **only partly correct**. A persistent UUID marker for calcs already exists, but it is half-wired and read inconsistently.

1. **Persistence format already exists: the JSDoc of the calc function.**
   - `ScriptCalculation.getFlags()` (servoy-client `servoy_shared/.../persistence/ScriptCalculation.java`) returns `ScriptVariable.getFlags(getDeclaration())`.
   - That returns `NORMAL_COLUMN | UUID_COLUMN` when the doc comment contains `@type {UUID}`.
   - The calc is stored as MEDIA (`@properties={type:-4,...}`), the same shape as a native UUID column (MEDIA + UUID_COLUMN).
   - Consumers that already honour the flag:
     - `ServoyFormBuilder.checkValueListRealValueToDataProviderTypeMatch` (`dataProvider.hasFlag(UUID_COLUMN)`).
     - `SQLGenerator` (lines ~557/562): `primary[x].getFlags()` is passed to `Column.getAsRightType`.
     - `JSCalculation.hasFlag` (solution model).

2. **`Relation.isUUID` reads a different source than the rest of the code.**
   - For any `AbstractBase` dataprovider (which includes `ScriptCalculation`) it checks `getSerializableRuntimeProperty(IScriptProvider.TYPE) == "UUID"`, not `getFlags()`.
   - `IScriptProvider.TYPE` is populated from the `jsType` JSON attribute (`SolutionDeserializer` ~l.2048 for variables, ~l.2159 for script providers).
   - `jsType` is only produced by `getScriptVariableJSON` (global/form variables, parsed from `@type`). The function/calculation path (`SolutionDeserializer` ~l.1256-1375) never sets `jsType`.
   - So for a calc `TYPE` is always null and `isUUID(calc)` is always false, even with a correct `@type {UUID}`. This is the actual gap behind SVY-21701.
   - `isUUID` has been there since 0ac67da7059 (2020), and the calc `getFlags` logic since before SVY-18643 (6c85fc0a58, 2023).

3. **The sample solution does not use the marker `getFlags` understands.**
   - `/home/gabi/programs/servoy_linux_2026.9/ws_demo/uuidCalc/datasources/example_data/uc_parent_calculations.js` uses `@return {UUID}`, not `@type {UUID}`. `getFlags()` only looks for `@type`.
   - `@return {UUID}` only feeds `METHOD_RETURN_TYPE` (code completion), not the flags.
   - The calc is typed MEDIA (`type:-4`).
   - I did not run the sample in a live IDE. From code reading, MEDIA vs MEDIA passes the final `primaryType != foreignType` check, and TEXT is what fails without the SVY-21701 allowance.

4. **UI gap is real.**
   - `CalculationTypeEditingSupport` builds its combo from `Column.allDefinedTypes` = `{TEXT, INTEGER, NUMBER, DATETIME, MEDIA}`.
   - `setValue` only calls `setTypeAndCheck`. It never writes the `@type {UUID}` tag.
   - Unlike a column editor, there is no way to say "UUID" for a calc.

## Ticket premise check
- The goal (a calc that is truly a UUID, persisted, and used consistently) is valid.
- "Needs a new place to persist the marker" does not hold. The marker is `@type {UUID}` in the calc's JSDoc, already parsed into `getFlags()`. What is missing:
  - a UI way to set it,
  - consistent reading in `Relation`,
  - possibly acceptance of `@return {UUID}`.
- "Adding `@type {UUID}` has no effect" is inaccurate: it has an effect for valuelists, SQL conversion and the solution model. It has none in `Relation.checkKeyTypes`, because of the `isUUID` inconsistency in point 2.
- The suggestion to narrow or remove the SVY-21701 TEXT-calc allowance is risky. Existing solutions already have TEXT calcs returning UUID strings. Removing the allowance would reintroduce the errors SVY-21701 fixed, so it should stay for backward compatibility.

## Approaches considered
1. **Unify on `getFlags()` plus add UI option (recommended).**
   - `Relation.isUUID`: for `ScriptCalculation` (and any `IDataProvider`) use `hasFlag(UUID_COLUMN)`, keeping the `TYPE == "UUID"` check for variables.
   - Optionally make `ScriptVariable.getFlags(String)` also accept `@return {UUID}` for calcs, so the sample solution works.
   - Add a "UUID" entry to the calc "Returned type" combo in `CalculationTypeEditingSupport`. It stores type MEDIA and writes or removes `@type {UUID}` in the declaration, and the combo `getValue` reflects the flag.
   - Keep the TEXT-calc allowance. Optionally add a builder warning suggesting `@type {UUID}`.
   - Pros: small, uses the existing persistence format, no file-format change, consistent with native UUID columns.
   - Cons: touches servoy-client (`Relation`, maybe `ScriptVariable`) and servoy-eclipse (UI), so it needs coordinated commits.
2. **Real new persisted UUID type value (e.g. a new `IColumnTypes` value or new calc property).**
   - Pros: explicit.
   - Cons: file-format change, wide blast radius (`Column.mapToDefaultType`, SQL, serialization). No benefit over the flag approach. Not recommended.
3. **UI-only change (combo writes `@type {UUID}`) with no `Relation` change.**
   - Cons: `Relation.isUUID` would still be false for calcs, so the UUID calc would only work because of the type MEDIA coincidence. Does not meet "used as a UUID throughout".
4. **No code change.**
   - Pros: SVY-21701 already unblocks the reported case.
   - Cons: the user still cannot mark a calc as UUID from the UI, and the `getFlags`/`isUUID` inconsistency remains.

## Recommendation
PROCEED with approach 1.
- Fix `Relation.isUUID` to use `getFlags()` for `ScriptCalculation`.
- Add a UUID option to the calc type combo that persists `@type {UUID}` and MEDIA.
- Decide whether `@return {UUID}` should also count (the sample solution uses it).
- Keep the SVY-21701 TEXT-calc allowance for backward compatibility.
- Add tests: extend `RelationUuidArrayKeyTypeTest` in servoy-client with a MEDIA calc flagged UUID against a native-UUID column. Add a plain-JUnit test for the combo/declaration rewrite if feasible.

Not verified: runtime behaviour of the sample solution in the IDE, and whether other consumers read `IScriptProvider.TYPE` on calcs.

## Git history findings
- servoy-client `b3d5956546` (SVY-21701, 2026-11-03): added `primaryType == TEXT && primary[i] instanceof ScriptCalculation` allowance in `Relation.checkKeyTypes`.
- servoy-client `6c85fc0a58` (SVY-18643, 2023): moved calc `@type {UUID}` flag parsing into `ScriptVariable.getFlags(String)` and added it for variables too. This confirms the intended marker is `@type {UUID}` in the JSDoc.
- servoy-client `0ac67da7059` (2020): introduced `Relation.isUUID` using the `TYPE` runtime property.
- servoy-client `ca1d6ca61d` (SVY-21511): UUID array key type handling in `checkKeyTypes`.
- servoy-eclipse: `CalculationTypeEditingSupport` unchanged in this area (last touched by SVY-17209). `SolutionDeserializer` function path never sets `jsType`.
- No prior spec in `docs/` for SVY-21710 or SVY-21701.
