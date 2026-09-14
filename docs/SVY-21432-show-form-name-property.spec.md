# Spec: SVY-21432 — Form `name` property is missing from Properties view

## 1. Goal

Restore the Form `name` property to the Eclipse Properties view (RFB designer and elsewhere)
and make editing it there safe. After this change, renaming a form from the Properties view
must be functionally equivalent to, and as safe as, the Solution Explorer "Rename form" action:

- The `name` row is visible again for a selected Form.
- Typing an invalid value (null, empty, or a non-Java-identifier) is rejected and leaves the
  current name unchanged.
- The "Restore Default" context-menu action cannot clear the name to `null`.

The end result closes the SVY-20310 corruption path (`null.js`) at its source — missing
validation — instead of hiding the property.

## 2. Background

The Form `name` was intentionally removed from the Properties view in 2025.9.0 by SVY-20310
("Form name can be restored to default value - null"), commit `b238fa153e8` in the
**servoy-client** repo. SVY-21432 reports the resulting absence as a defect (screenshot shows a
Form selected in the RFB designer whose Properties view lists `comment`, `dataSource`,
`deprecated`, `designTimeProperties`, `encapsulation`, `extendsForm`, `styleClass`, `titleText`,
etc., but no `name` row). SVY-21432 has fixVersion **2026.3.2 LTS** and links to SVY-20310 as
"is triggered by".

Root cause of the original SVY-20310 bug, confirmed during triage:

- Both the Solution Explorer "Rename form" action and the old Properties-view edit call the
  **same** `ISupportUpdateableName.updateName(nameValidator, name)` mechanism. Neither is a
  special JDT refactor; the `.js` file rename and reference updates happen downstream in the
  model layer for both.
- The only difference is input validation. Solution Explorer wraps the rename in an
  `InputDialog` whose `IInputValidator` rejects empty/invalid names via
  `IdentDocumentValidator.isJavaIdentifier`
  (`RenamePersistAction.java:81-85`). The Properties-view branch accepted `null`/empty and passed
  them straight to `updateName`, producing the `null.js` corruption.

SVY-20310 removed the property (suppressed it in `RepositoryHelper.shouldShow`) rather than add
the missing validation. This spec adds the validation and restores the property.

## 3. Design

Three coordinated changes across two repositories.

### 3.1 Show `name` again — revert the suppression (servoy-client repo)

File: `servoy-client/servoy_shared/src/com/servoy/j2db/persistence/RepositoryHelper.java`
(SEPARATE git repo at `/home/gabi/github_2026.3/servoy-client`, not the eclipse repo).

Remove the Form-name suppression added by SVY-20310 at line 733:

```java
if (Form.class.isAssignableFrom(persistClass) && "name".equals(name))
{
    return false;
}
```

`shouldShow` is consulted by `PersistPropertySource.shouldShow(...)` which gates
`registerProperty(...)`; deleting this block causes the `name` descriptor to be registered for a
Form again, so the row reappears in the Properties view.

### 3.2 Validate typed input (eclipse repo)

File: `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/property/PersistPropertySource.java`,
the `name` branch of `setPersistPropertyValue` (lines 2715-2726).

Current code passes any `String` or `null` straight to `updateName`:

```java
if ("name".equals(id) && beanPropertyPersist instanceof ISupportUpdateableName)
{
    if (value instanceof String || value == null)
    {
        changed |= !Utils.equalObjects(value, ((ISupportUpdateableName)beanPropertyPersist).getName());
        ((ISupportUpdateableName)beanPropertyPersist).updateName(..., (String)value);
        ...
    }
    ...
}
```

Change: before calling `updateName`, reject values that are `null`, empty, or not a Java
identifier, using the same check the Solution Explorer dialog uses
(`IdentDocumentValidator.isJavaIdentifier`, from `com.servoy.j2db.util.docvalidator`). When the
value is invalid, keep the current name unchanged (do not call `updateName`) and refresh the
properties view so the cell reverts to the old value. Do not treat an unchanged name as an error.

This is a **silent safety net** — the cell-editor validator (§3.2.1) is the real gate and already
blocks invalid input inline before the setter runs, so no dialog is shown here. Verified against a
live developer: for a Form, `setPersistPropertyValue` never receives an invalid/empty name because
the cell editor rejects it first.

#### 3.2.1 Cell-editor validator is the real entry point (fixes empty-string → null.js)

The Form `name` cell editor (created in the `id.equals("name")` branch, ~line 3787) uses
`NULL_STRING_CONVERTER`, whose `convertValue("")` returns `null`. Its existing
`ICellEditorValidator.isValid(...)` only ran `checkName` when `value.length() > 0`, so an empty
string passed as valid and was committed as `null` → `null.js` (SVY-20310).

Fix: in that `isValid(...)`, for a `Form` persist reject any value that is not a valid Java
identifier (covers `null`, empty and non-identifier) by returning `"Invalid form name"` *before* the
`length() > 0` check. Committing is driven by `ModifiedPropertySheetEntry.applyEditorValue()`, which
calls `editor.isValueValid()`; when the validator returns an error the value is **not committed**,
the previous name is kept, and the property sheet shows the inline error text. This is the single
user-facing gate and is consistent with Solution Explorer's inline validation. Logging on a live
developer confirmed the validator fires for every commit (`persist=Form value=[]` for empty) and the
setter branch is never reached with an invalid value.

**UX note (decided with reporter):** invalid input is silently reverted to the previous name (no
popup). This is acceptable and matches standard Eclipse cell-editor behaviour.

### 3.3 Prevent "Restore Default" from clearing the name (eclipse repo)

File: `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/property/PersistPropertySource.java`.

`PersistPropertySource` implements `IPropertySource2` and overrides `isPropertyResettable(id)` to
return `false` for a `Form` + `PROPERTY_NAME`. Servoy's property-sheet entry
(`PropertySheetEntry.resetPropertyValue()`) explicitly checks `isPropertyResettable` and skips the
reset, so "Restore Default" becomes a guaranteed no-op for the form name.

Additional guards, as defense in depth:
- `isPropertySet` / `isPersistPropertySet` return `false` for a `Form` + `PROPERTY_NAME`.
- `clearAbstractBaseProperty` short-circuits for a `Form` + `PROPERTY_NAME`, so even if a reset path
  is somehow invoked, `clearProperty("name")` is never called and the name cannot be nulled.

**Known limitation (accepted with reporter):** the "Restore Default" context-menu item still appears
(the platform property-sheet action does not grey out per-entry based on `isPropertyResettable`), but
invoking it does nothing. Making the item disappear would require overriding the platform action in
`ModifiedPropertySheetPage`, which was judged out of scope.

Use the existing `Form` type check and the `PROPERTY_NAME` constant already used elsewhere
in this class (e.g. the `createPropertyDescriptor` special-case at line 900).

### 3.4 Consistency notes

- The Properties-view edit and Solution Explorer rename must remain on the same
  `ISupportUpdateableName.updateName(...)` path so `.js` file rename and reference updates stay in
  sync in both.
- Read-only / inherited contexts already short-circuit (`isPropertySet` returns `false` when
  `readOnly`; `isPersistPropertySet` returns `false` for non-owning form contexts at line 2468),
  so the visible name behaves like other identity properties in inherited forms.

## 4. Implementation plan

1. **servoy-client** repo: delete the `Form` + `name` suppression block in
   `RepositoryHelper.shouldShow` (`RepositoryHelper.java:733-736`). Rebuild so the eclipse repo
   picks up the change.
2. **eclipse** repo, Form `name` cell-editor validator (the `id.equals("name")` branch, ~line
   3777): for a `Form` persist reject any value that is not a valid Java identifier (covers
   null/empty/non-identifier) with the message `"Invalid form name"`, before the `length() > 0`
   check. This is the real entry point that stops the empty-string → `null.js` case.
3. **eclipse** repo, `PersistPropertySource.setPersistPropertyValue` name branch: keep the
   `IdentDocumentValidator.isJavaIdentifier` guard as defense in depth (reject null/empty/invalid,
   keep the current name); ensure the import for `IdentDocumentValidator` is present.
4. **eclipse** repo, `PersistPropertySource` implements `IPropertySource2` and overrides
   `isPropertyResettable(id)` to return `false` for a Form + `name`; `PropertySheetEntry`
   consults this and skips the reset, making "Restore Default" a no-op.
5. **eclipse** repo, `isPropertySet` / `isPersistPropertySet` / `clearAbstractBaseProperty`: guard so
   a Form `name` reports not-set and is never cleared to null (defense in depth).
6. Organize imports and format the modified Java files.
7. Verify: `eclipse-ide_getCompilationErrors` clean; run relevant tests in
   `com.servoy.eclipse.tests` if any cover PersistPropertySource / rename.
8. Manual check in the RFB designer (verified on a live developer): `name` row visible; typing a
   valid name renames the form and the `.js` file and references; typing empty/invalid is rejected
   inline and the name stays (no `null.js`); "Restore Default" does nothing for `name`.

## 5. Acceptance criteria

- Selecting a Form in the RFB designer shows a `name` row in the Properties view.
- Editing `name` to a valid Java identifier renames the form, its `.js` file, and updates
  references — identical to the Solution Explorer "Rename form" action.
- Editing `name` to null, empty, or a non-identifier value is rejected; the form keeps its
  previous name; no `null.js` is produced.
- "Restore Default" on the Form `name` property does not change or clear the name (guaranteed no-op
  via `IPropertySource2.isPropertyResettable`); the menu item may still be visible.
- No `null.js` file or cascading errors can be produced from the Properties view (the SVY-20310
  regression does not return).
- Behaviour in read-only / inherited form contexts is unchanged (name not editable there, as
  before).

## 6. Out of scope

- Any redesign of the rename/refactor mechanism itself (`ISupportUpdateableName.updateName`).
- Changes to Solution Explorer "Rename form" behaviour.
- Renaming other identity properties (e.g. component `name`) — only Form `name` is in scope.
- Overriding the platform property-sheet action to hide/grey the "Restore Default" menu item for the
  form name (the action is a documented no-op instead).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Invalid-input UX | reporter | resolved — invalid/empty input is silently reverted inline by the cell-editor validator (no popup); verified on a live developer |
| Restore Default menu | reporter | resolved — made a guaranteed no-op via `IPropertySource2.isPropertyResettable`; menu item stays visible-but-inert (accepted) |
| Branch scope | reporter | resolved — implement on `lts_2026`; will be merged to `release` and `master` afterwards (to be confirmed by reporter) |
