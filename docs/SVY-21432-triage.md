# Triage Report — SVY-21432

**Verdict:** NEEDS_INPUT

## Reported problem
The `name` property of a Form is missing from the Properties view. The attached
screenshot shows a Form selected in the RFB designer; the Properties view lists
`comment`, `dataSource`, `deprecated`, `designTimeProperties`, `encapsulation`,
`extendsForm`, `styleClass`, `titleText`, etc., but there is no `name` row. The
ticket frames this as a defect ("property is missing").

## Root-cause assessment
The omission is **deliberate and recent**, not a regression or accidental loss.

The Form `name` is explicitly suppressed in the properties view by
`RepositoryHelper.shouldShow(...)` in
`servoy-client/servoy_shared/src/com/servoy/j2db/persistence/RepositoryHelper.java:733`:

```java
if (Form.class.isAssignableFrom(persistClass) && "name".equals(name))
{
    return false;
}
```

`shouldShow` is consulted by `PersistPropertySource.shouldShow(...)`
(`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/property/PersistPropertySource.java:2181`),
which gates `registerProperty(...)` (same file, line 727). So returning `false`
means the `name` descriptor is never registered for a Form — it does not even land
in the hidden-descriptors map; it is simply not built.

There is corroborating handling elsewhere that treats the Form `name` as a special,
non-inheritable identity property rather than an ordinary editable property — e.g.
`PersistPropertySource.createPropertyDescriptor(...)` (line 899) explicitly skips the
inheritance-override wrapper for `Form` + `PROPERTY_NAME`.

### Correction (post-review): both paths use the same rename mechanism
An initial reading suggested the Solution Explorer "Rename form" action performed a
true JDT-style refactor while the Properties-view edit did not. That is **incorrect**.

- `RenamePersistAction.run()`
  (`com.servoy.eclipse.ui/.../solutionexplorer/actions/RenamePersistAction.java:97`)
  and the old Properties-view path
  (`PersistPropertySource.java:2720`) both call the **same**
  `ISupportUpdateableName.updateName(nameValidator, name)`. The `.js` file rename and
  reference updates happen downstream from that persist-name change in the model layer,
  in both cases. Neither path invokes a dedicated refactoring participant.

The **actual** difference is input validation:
- Solution Explorer wraps the rename in an `InputDialog` with an `IInputValidator`
  that rejects empty and non-identifier names via
  `IdentDocumentValidator.isJavaIdentifier(newText)`
  (`RenamePersistAction.java:81-85`). Null/empty can never be submitted.
- The Properties-view branch (`PersistPropertySource.java:2715-2726`) accepts
  `value == null` (and empty strings) and passes them straight to `updateName`,
  producing the `null.js` corruption reported in SVY-20310.

So SVY-20310 removed the property rather than add the missing name validation to the
Properties-view path.

## Ticket premise check
The ticket's premise — that the missing `name` is a bug — is partially valid. The
removal was intentional and made to fix a different bug, but the root cause of that
bug was a missing name validator, not the property's presence. Git history:

- Introducing commit: `b238fa153e8` (2025-06-17), on `servoy_shared`.
- Commit message:
  > `SVY-20310 Form name can be restored to default value - null`
  > `property "name" is not shown in the form property view`
- SVY-20310 ("Form name can be restored to default value - null", Closed/Fixed,
  fixVersion 2025.9.0) reported that editing the Form `name` in the Properties view
  allowed it to be **restored to its default (null)** and saved, producing a
  `null.js` file and cascading errors that persisted even after renaming. The fix was
  to stop showing `name` in the Form properties view altogether.

So the current behaviour is exactly what the user's context suspects: the Form
`name` was intentionally removed from the Properties view so a form can only be
renamed via Solution Explorer (which triggers the proper rename/refactor path),
avoiding the null-restore corruption. The property **was** in the Properties view
previously and was removed in 2025.9.0.

## Approaches considered
1. **No code change (behaviour is by design).** — Pros: preserves the SVY-20310 fix;
   renaming stays on the Solution Explorer refactor path that keeps the `.js` file and
   references in sync; avoids re-opening the null-restore corruption. Cons: users who
   expect to see (even read-only) the form name in Properties get no feedback; the
   ticket reporter perceives it as missing.
2. **Show `name` as read-only in the Properties view.** — Pros: restores visibility /
   discoverability without allowing the dangerous edit or the "restore default → null"
   path that caused SVY-20310. Cons: small amount of work; must ensure the read-only
   descriptor cannot be reset to default; slight inconsistency with other identity
   properties.
3. **Show `name` as editable again but block reset-to-default / null and route the
   edit through the rename refactor.** — Pros: full editability from Properties.
   Cons: this is essentially reverting SVY-20310 and re-implementing rename-with-
   refactor inside the properties path; high risk of reintroducing the original bug;
   larger effort.

## Recommendation
This needs a product decision before a spec can be written, because the "fix"
requested by SVY-21432 directly conflicts with the intentional fix delivered in
SVY-20310. The technical facts are unambiguous (the removal is by design, per commit
`b238fa153e8`), but *what the desired end state should be* is a judgement call:

- If the intent is "the name should not be editable from Properties, that's correct" →
  **NO_ACTION** (optionally close SVY-21432 as working-as-intended, referencing
  SVY-20310).
- If the intent is "users should at least see the form name" → a small change to show
  it **read-only** (approach 2) is the safe path.
- Making it editable again (approach 3) should be avoided unless there is a strong
  product reason, as it risks the SVY-20310 corruption.

I recommend **not** simply reverting the suppression. My suggested path, pending
confirmation, is approach 2 (read-only display) or NO_ACTION.

### Post-review addendum: the validation gap can be closed directly
Since both rename paths share `ISupportUpdateableName.updateName(...)` and the only
difference is validation, the SVY-20310 corruption can be prevented in the
Properties-view path itself rather than by hiding the property. Options:

- **Guard in `PersistPropertySource.setPersistPropertyValue` (name branch,
  lines 2715-2726):** reject null / empty / non-identifier names (using
  `IdentDocumentValidator.isJavaIdentifier`, the same check the Solution Explorer
  dialog uses) before calling `updateName`, leaving the existing name unchanged.
  Simplest, but rejects silently — no inline feedback in the cell.
- **Cell-editor validator:** attach an `ICellEditorValidator` to the `name` property
  descriptor so the property cell rejects invalid input inline, matching the dialog.

Either option also requires reverting the `RepositoryHelper.java:733` suppression so
the `name` property is shown again. This would make the Properties-view rename
functionally equivalent to (and as safe as) the Solution Explorer rename.

### Restore Default is a second, distinct corruption path
Typed input is not the only way to null the name. The Properties view context menu
"Restore Default" routes through
`resetPropertyValue` → `defaultResetProperty` (`PersistPropertySource.java:2542`).
Because the Form `name` lives on the persist itself
(`beanPropertyDescriptor.valueObject == persistContext.getPersist()`), it takes the
`clearAbstractBaseProperty` branch (line 2563), which calls `clearProperty("name")` —
removing the name entirely. This reproduces the SVY-20310 corruption independently of
the cell editor, so a cell-editor validator alone is **insufficient**.

The full fix therefore requires three coordinated changes:
1. **Show `name` again** — revert the `RepositoryHelper.java:733` Form-name suppression.
2. **Validate typed input** — in the name branch of `setPersistPropertyValue`
   (lines 2715-2726), reject null / empty / non-identifier values (using
   `IdentDocumentValidator.isJavaIdentifier`, matching Solution Explorer) and keep the
   current name.
3. **Prevent Restore Default from clearing the name** — make the Form `name` property
   non-resettable: have `isPropertySet` return `false` for Form + `name` (which
   disables the "Restore Default" menu item) and/or guard `defaultResetProperty` /
   `clearAbstractBaseProperty` so the name cannot be cleared to null.

## Git history findings
- `RepositoryHelper.java:733` suppression added in commit `b238fa153e8`
  ("SVY-20310 Form name can be restored to default value - null / property \"name\" is
  not shown in the form property view"), 2025-06-17, shipped in 2025.9.0.
- SVY-20310 is Closed/Fixed and documents the concrete data-corruption motivation
  (`null.js`) for removing the property from the Form properties view.
- No prior `docs/` spec exists for SVY-20310 or this area.

## Questions for the reporter
1. The Form `name` was intentionally removed from the Properties view in 2025.9.0 by
   SVY-20310, because editing it there allowed the name to be reset to its default
   (null), which saved the form as `null.js` and caused persistent errors. Given that,
   what is the desired outcome here?
   - (a) Leave it as-is — renaming a form should only be done from Solution Explorer
     (which runs the rename refactor). This ticket would be closed as working-as-
     intended.
   - (b) Show the form `name` again, but **read-only**, so it is visible for reference
     without allowing the dangerous edit / reset-to-default.
   - (c) Make `name` fully editable in the Properties view again (this would require
     re-introducing rename-with-refactor there and guarding against the null/reset
     case that SVY-20310 fixed).
2. If (b) or (c): should editing/renaming still be blocked while the form is open in a
   read-only or inherited context, and should it behave identically to the Solution
   Explorer "Rename" action (updating the `.js` file and all references)?
