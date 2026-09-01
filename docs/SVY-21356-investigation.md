# SVY-21356 — Second-opinion investigation

This is an independent re-investigation of the four "potential gaps" that a prior
triage pass (`docs/SVY-21356-triage.md`) marked NO_ACTION. Every claim below was
verified against the current source in the open `servoy-eclipse` workspace
projects (`com.servoy.eclipse.ui`, `com.servoy.eclipse.model`, `servoy_shared`,
`servoy_ngclient`) and the MCP artifact-creation path in
`com.servoy.eclipse.developer.mcp` (Servoy-Copilot repo), not taken from the
triage doc's line numbers.

---

## 1. Form with no Body part

**Wizard behavior** (`com.servoy.eclipse.ui.wizards.NewFormWizard.performFinish()`,
`src/com/servoy/eclipse/ui/wizards/NewFormWizard.java:382-386`):

```java
if (!newFormWizardPage.isResponsiveLayout())
{
    // create default form, most is already set in createNewForm
    if ((superForm == null || !superForm.getParts().hasNext()) && !newFormWizardPage.isAbstractForm())
        form.createNewPart(Part.BODY, 480/* height */); // else the form just inherits parts from super; no need to add body
    ...
```

So through the wizard, a non-responsive form **always** gets a `Part.BODY`
unless either (a) it inherits parts from a super form that already has some, or
(b) the user explicitly picked the "Abstract (no UI)" radio button
(`bTypeAbstract`, lines 959-961). `isAbstractForm()` on the wizard page
(line 727) just returns whether that radio button is selected.

Crucially, on the persisted `Form` object, "abstract" is **not a stored flag** —
`Form.isAbstractForm()` (`servoy_shared/src/com/servoy/j2db/persistence/Form.java:2634-2641`)
is purely structural:

```java
public boolean isAbstractForm()
{
    if (isResponsiveLayout()) return false;
    if (getUseCssPosition().booleanValue() == true) return false;
    Iterator<Part> it = getParts();
    if (it.hasNext()) return false; //abstract form has no parts
    return true;
}
```

This means: for a plain anchored form (`useCssPosition == false`,
non-responsive) with zero parts, "abstract" and "accidentally missing body" are
**indistinguishable persisted states** — the wizard legitimately produces this
exact state when the user picks "Abstract", so a builder marker that fires on
"anchored form, no parts" would false-positive on every intentionally-abstract
form. That part of the triage's conclusion is correct.

**However** — the wizard's code above shows a form with **`useCssPosition ==
true`** (CSS-position layout, the modern non-responsive default, see
`newFormWizardPage.isCSSPosition()` at `NewFormWizard.java:392-395`) is *never*
left without a `Part.BODY` by the wizard: the `createNewPart(Part.BODY, ...)`
call happens unconditionally for any non-abstract, non-responsive form,
*before* `isCSSPosition()` is checked, and `isAbstractForm()` on `Form` treats
`useCssPosition == true` as automatically "not abstract" regardless of parts.
So a CSS-position form with zero parts is a state the wizard **structurally
cannot produce** — it is exactly the kind of wizard-enforced invariant the
ticket is asking about, and it is silently possible via direct `.frm` editing
(e.g. an AI writes `"useCssPosition":true` with no `"parts"` array) or via
any future MCP tool that forgets the `form.createNewPart(Part.BODY, ...)` call
(the current `ServoyArtifactCreationService.createForm`/`createFormWithFields`
happen to always call it — `src/.../ServoyArtifactCreationService.java:83,
322` — but nothing in the builder would catch it if that ever regressed or if
the form is hand-edited).

**Runtime handling** confirmed in
`servoy_ngclient/src/com/servoy/j2db/server/ngclient/component/WebFormController.java:110-118`:

```java
if (form.getView() == IFormConstants.VIEW_TYPE_RECORD || form.getView() == IFormConstants.VIEW_TYPE_RECORD_LOCKED)
{
    formUI = new WebFormUI(this);
}
else if (!application.getFlattenedSolution().getFlattenedForm(form).hasPart(IPartConstants.BODY))
{
    formUI = new WebFormUI(this);
    getApplication().reportJSWarning("Form '" + form.getName() + "' is shown in record view because it does not have a body part.");
}
```

This is graceful (falls back to record view) but the warning only reaches the
JS console at runtime — nothing appears in the Problems view at build/edit
time. `ServoyFormBuilder` has no check anywhere referencing `hasPart`,
`Part.BODY`, or `isAbstractForm()` for this purpose (confirmed by grep across
`ServoyFormBuilder.java` — the only `getParts()`/`Part` usages are the
existing "duplicate part" check at lines 704-719 and unrelated location-bounds
checks).

**Verdict: GAP CONFIRMED** (narrow scope).

Recommended marker — fire only for the case the wizard actually prevents
(avoids false positives on legitimate abstract forms):

- **Condition**: in `ServoyFormBuilder.addFormMarkers(...)`, for a `Form` where
  `!form.isResponsiveLayout()` **and** `form.getUseCssPosition() ==
  Boolean.TRUE`, resolve the flattened form (`ServoyBuilder.getPersistFlattenedSolution(form, fs)`
  then `FlattenedSolution.getFlattenedForm(form)` or equivalent) and check
  `!flattenedForm.hasPart(IPartConstants.BODY)` (mirroring the exact runtime
  predicate in `WebFormController`). If true, add the marker.
- **MarkerMessages.java** entry:
  ```java
  public static ServoyMarker FormCssPositionNoBodyPart = new ServoyMarker(
      "Form \"{0}\" uses CSS position layout but has no body part; it will be shown in record view at runtime.",
      ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
  ```
- **ServoyBuilder.java** severity-pair constant:
  ```java
  public final static Pair<String, ProblemSeverity> FORM_CSS_POSITION_NO_BODY_PART =
      new Pair<String, ProblemSeverity>("formCssPositionNoBodyPart", ProblemSeverity.WARNING);
  ```
  (`WARNING`, not `ERROR`, because the runtime already degrades gracefully —
  this is an IDE-visibility improvement, not a broken-artifact error, matching
  the ticket's own framing of this specific example as "worth adding for IDE
  visibility even though runtime degrades gracefully".)
- **Builder call site**: inside `ServoyFormBuilder.addFormMarkers`, near the
  existing duplicate-part check (`ServoyFormBuilder.java:703-719`), guarded by
  `!form.isResponsiveLayout() && Boolean.TRUE.equals(form.getUseCssPosition())`.

---

## 2. Form elements with invalid/non-existent valuelist references

Confirmed the generic mechanism does cover this, by reading the real code:

- `PROPERTY_VALUELISTID` is registered as an `ELEMENTS`-typed property:
  `servoy_shared/src/com/servoy/j2db/persistence/StaticContentSpecLoader.java:377`:
  ```java
  cs.new Element(32, IRepository.FIELDS, PROPERTY_VALUELISTID.getPropertyName(), IRepository.ELEMENTS);
  ```
- `Field.getValuelistID()`/`setValuelistID()`
  (`servoy_shared/src/com/servoy/j2db/persistence/Field.java:311-319`) just
  store/return the raw UUID string via `setTypedProperty`/`getTypedProperty` —
  no valuelist-specific validation on the setter, consistent with the ticket's
  premise that direct edits bypass Java-level checks.
- `ServoyFormBuilder.addFormMarkers` (`src/.../ServoyFormBuilder.java:157-320`)
  iterates every `ELEMENTS`-typed content-spec property of every persist in the
  form via reflection (`typeId == IRepository.ELEMENTS`, line 252), resolves the
  UUID with `fs.searchPersist(element_uuid)` (line 261), and when the target
  can't be resolved calls
  `ServoyBuilderUtils.addNullReferenceMarker(markerResource, o, foundPersist, context, element)`
  (line 266). This is a generic reflection-driven loop — it makes no
  distinction between `valuelistID`, `extendsID`, or any other `ELEMENTS`
  property, so it applies uniformly to `Field.valuelistID`.
- `ServoyBuilderUtils.addNullReferenceMarker`
  (`src/.../ServoyBuilderUtils.java:358-447`) produces
  `PropertyOnElementInFormTargetNotFound` (or `PropertyOnElementTargetNotFound`
  outside a form) with severity `ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND`
  and text `"Property \"{0}\" from element \"{1}\" in form \"{2}\" is linked to
  an entity that does not exist."` (`MarkerMessages.java:283-288`).

This is a real, currently-firing check — a `Field` whose `valuelistID` points
at a deleted/non-existent `ValueList` UUID gets this marker on the next build,
regardless of how it was created (wizard, MCP tool, or hand-edited `.frm`).

**Verdict: NO GAP.** Confirmed by direct reading, not just trusting the
triage — the mechanism is real and does cover `valuelistID`.
(`ServoyFormBuilder.java:157-320` (elements loop), `ServoyBuilderUtils.java:358-447`
(`addNullReferenceMarker`), `StaticContentSpecLoader.java:377`
(`PROPERTY_VALUELISTID` registered as `ELEMENTS`), `Field.java:311-319`.)

---

## 3. Relation items with incompatible column types

**Wizard-equivalent path**: the simple `NewRelationWizard` /
`NewRelationAction.createRelation` (`com.servoy.eclipse.ui.wizards.NewRelationWizard.java:103-115`,
`.../actions/NewRelationAction.java:103-140`) creates an **empty** relation with
no items and just opens the `RelationEditor` — it performs **no** type
checking at all at that point (there's nothing to check yet). The actual
"wizard would reject this" enforcement point is `RelationEditor.doSave()`
(`src/.../RelationEditor.java:762-788`):

```java
String message = createAndcheck();
if (message != null)
{
    MessageDialog.openError(getSite().getShell(), "Error while saving", message);
    if (monitor != null) monitor.setCanceled(true);
    return;
}
```

`RelationEditor.createAndcheck()` (`RelationEditor.java:822-908`) calls
`r.checkKeyTypes(relationFlattenedSolution)` (line ~879) and, if it returns a
non-null message, **blocks the save** with an error dialog — the user cannot
persist an incompatible relation item through the editor.

**Builder path**: `ServoyRelationBuilder.checkRelation()`
(`src/.../ServoyRelationBuilder.java:129-425`) calls the exact same method at
the very end (line ~406):

```java
String typeMismatchWarning = element.checkKeyTypes(relationFlattenedSolution);
if (typeMismatchWarning != null)
{
    mk = MarkerMessages.RelationItemTypeProblem.fill(element.getName(), typeMismatchWarning);
    ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
        ServoyBuilder.RELATION_ITEM_TYPE_PROBLEM, IMarker.PRIORITY_LOW, null, element);
}
```

Both call sites invoke the identical `Relation.checkKeyTypes(IDataProviderHandler)`
(`servoy_shared/src/com/servoy/j2db/persistence/Relation.java:802-...`) —
**same predicate, same set of flagged cases** (literal/column type mismatch,
UUID-flag mismatch already has its own separate check earlier in
`checkRelation`, array/enum type mismatches, etc.). So the triage's claim that
"the same check, same code path" is used is correct on the *detection* side.

**Where it differs — severity, not detection.** `ServoyBuilder.java:535-536`:

```java
public final static Pair<String, ProblemSeverity> RELATION_ITEM_TYPE_PROBLEM =
    new Pair<String, ProblemSeverity>("relationItemTypeProblem", ProblemSeverity.WARNING);
```

The editor treats a `checkKeyTypes()` failure as **blocking** (cannot save —
equivalent to a hard error), while the builder's default severity for the same
condition is **WARNING**, not `ERROR`. This is a real strictness mismatch: an
AI-created or hand-edited relation with an incompatible key type gets a
build-time *warning* for something the interactive editor treats as fatal.

**Verdict: NO GAP in detection** (identical check fires in both paths, so no
case slips through undetected), **but a secondary severity inconsistency
exists** that is worth a one-line fix if the team wants exact parity with the
editor's strictness:

- Change `ServoyBuilder.RELATION_ITEM_TYPE_PROBLEM` from
  `ProblemSeverity.WARNING` to `ProblemSeverity.ERROR` in
  `ServoyBuilder.java:535-536`, and/or bump the marker priority from
  `IMarker.PRIORITY_LOW` to `IMarker.PRIORITY_NORMAL` at
  `ServoyRelationBuilder.java:406-412` to reflect that the equivalent editor
  action is a hard save-blocking error, not an informational note.

This is not a "no marker exists" gap (the ticket's core premise), so it is
reported separately from the GAP CONFIRMED items rather than as a new marker.

---

## 4. Valuelist with invalid `databaseValuesType`

Verified `getDatabaseValuesType()` is a pure computed getter with **no
backing property at all**, in `servoy_shared` (not `com.servoy.eclipse.model` —
the triage's location claim was right, `ValueList.java` lives in
`servoy_shared/src/com/servoy/j2db/persistence/ValueList.java`):

```java
public int getDatabaseValuesType()
{
    return (getRelationName() == null ? TABLE_VALUES : RELATED_VALUES);
}
```

(`ValueList.java:128-131`) — derived solely from `getRelationName()`. There is
no `setDatabaseValuesType` method anywhere in the class (confirmed via the
full outline — 52 methods listed, no such setter).

Checked the direct-file-edit escape hatch explicitly, since that's the part
the triage might have under-verified: a settable/persisted property in this
codebase is only possible if it is registered in the content spec
(`StaticContentSpecLoader`), because that's what both the generic
`setProperty(String, Object)` reflection path (`AbstractBase.java:184-202`,
which uses `RepositoryHelper.getSettersViaIntrospection` — i.e. it still needs
a real Java setter to exist) and the `.val`/JSON (de)serializer key off. Grepped
`StaticContentSpecLoader.java` for every `VALUELISTS`-typed `cs.new Element(...)`
registration (lines 503-963): `PROPERTY_VALUELISTTYPE`, `PROPERTY_RELATIONNAME`,
`PROPERTY_CUSTOMVALUES`, `PROPERTY_SERVERNAME`/`TABLENAME` (deprecated),
`PROPERTY_DATAPROVIDERID1/2/3`, `PROPERTY_SHOWDATAPROVIDERS`,
`PROPERTY_RETURNDATAPROVIDERS`, `PROPERTY_SEPARATOR`, `PROPERTY_SORTOPTIONS`,
`PROPERTY_ADDEMPTYVALUE`, `PROPERTY_USETABLEFILTER`, `PROPERTY_DATASOURCE`,
`PROPERTY_CUSTOMPROPERTIES`, `PROPERTY_FALLBACKVALUELISTID`,
`PROPERTY_ENCAPSULATION`, `PROPERTY_DEPRECATED`,
`PROPERTY_VALUELIST_REALVALUE_TYPE`, `PROPERTY_VALUELIST_DISPLAYVALUE_TYPE`,
`PROPERTY_COMMENT`. **`databaseValuesType` is not among them** — there is no
`IContentSpecConstants.PROPERTY_DATABASEVALUESTYPE` constant, no
`TypedProperty`, and no content-spec `Element` registration for it at all.

This means: (a) it can never be serialized into a `.val` file in the first
place (the JSON writer only emits registered/typed properties), and (b) even
if an AI hand-wrote a `"databaseValuesType": 1` key into a `.val` file directly,
the deserializer has no property slot to read it into — it would simply be
ignored as unrecognized JSON, not stored anywhere, and
`getDatabaseValuesType()` would still compute its value fresh from
`getRelationName()` every time it's called. There is no way — through the
editor, through solution-model script API (`JSValueList`), through the MCP
artifact tools, or through direct file editing — to make this getter return
something inconsistent with `getRelationName()`, because it never reads
stored state.

**Verdict: NO GAP — confirmed, including the direct-file-edit angle the task
asked to double-check.** The premise of this example is incorrect: there is no
persisted or settable `databaseValuesType`, by any mechanism, so no marker is
possible or needed.

---

## 5. Form with `dataSource` set to a datasource format the wizard wouldn't allow

Confirmed the resolution chain end-to-end:

- `ServoyFormBuilder.addFormMarkers` (`ServoyFormBuilder.java:553-587`):
  ```java
  table = formFlattenedSolution.getTable(form.getDataSource());
  if (table == null && form.getDataSource() != null)
  {
      ServoyMarker mk = MarkerMessages.FormTableNotAccessible.fill(form.getName(), form.getTableName());
      ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_TABLE, IMarker.PRIORITY_HIGH, null, form);
  }
  ...
  catch (Exception e) // also raises FORM_INVALID_TABLE
  ```
  `ServoyBuilder.FORM_INVALID_TABLE` is `ProblemSeverity.ERROR`
  (`ServoyBuilder.java:484`).
- `formFlattenedSolution` here is a `DeveloperFlattenedSolution`
  (`com.servoy.eclipse.model.DeveloperFlattenedSolution.java:178-182`):
  ```java
  public ITable getTable(String dataSource)
  {
      return ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(dataSource);
  }
  ```
- `getDataSource(String)` (`AbstractServoyModel.java:114-173`) tries, in order:
  `mem:` prefix → in-mem table, `view:` prefix → view table, `menu:` prefix →
  menu table, else `db:/server/table` via `DataSourceUtils.getDBServernameTablename`
  → real server/table lookup. **Every branch that fails to resolve — wrong
  prefix, missing separator, non-existent server, non-existent table, or a
  syntactically nonsensical string that matches none of the four prefixes —
  falls through to `return null`** (the final `else` block at lines 153-171
  itself returns null when `getDBServernameTablename` returns null, i.e. any
  string not starting with `db:/`). Every internal exception is caught and
  logged, never propagated (`catch (Exception e) { ServoyLog.logError(...); }`
  in each branch), so `getTable()` never throws — it uniformly returns `null`
  for any malformed shape.
- Back in `ServoyFormBuilder`, `table == null` unconditionally raises
  `FORM_INVALID_TABLE`, regardless of *which* way the datasource was
  malformed (wrong scheme, missing table segment, unknown server, unknown
  table, or garbage string). This is a single uniform null-check, not a
  scheme-specific one, so there is no sub-case (wrong prefix vs. missing
  separator vs. unknown server vs. unknown table) that produces a *different,
  uncovered* outcome — they all collapse to the same `table == null` branch
  and the same marker.

Cross-checked the MCP creation path too:
`ServoyArtifactCreationService.correctDataSource` (`.../ServoyArtifactCreationService.java:453-462`)
only prepends `db:/` if missing a leading `db:/` and the string contains `/`,
otherwise throws before creation — but even if a *worse*-formed string slipped
through some other path (e.g. a hand-written `.frm` with
`"dataSource":"totally:bogus"`), the builder's uniform `table == null` check
above would still catch it on the next build, per the mechanism just traced.

**Verdict: NO GAP.** The generic table-resolution null-check in
`ServoyFormBuilder.addFormMarkers` (lines 553-587) is provably
format-agnostic — it does not special-case any datasource shape, so there is
no malformed-datasource sub-case that bypasses it.

---

## Summary

| # | Item | Verdict | Marker (if applicable) | Target file(s) to modify |
|---|------|---------|-------------------------|---------------------------|
| 1 | Form with no Body part | **GAP CONFIRMED** (narrow: CSS-position forms only) | `FormCssPositionNoBodyPart` / `ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART` (`formCssPositionNoBodyPart`, `WARNING`) | `MarkerMessages.java`, `ServoyBuilder.java`, `ServoyFormBuilder.java` (`addFormMarkers`) |
| 2 | Form elements with invalid valuelist references | **NO GAP** — generic `ELEMENTS` dangling-reference check already covers `Field.valuelistID` | — | — (verified `ServoyFormBuilder.java:157-320`, `ServoyBuilderUtils.java:358-447`, `StaticContentSpecLoader.java:377`) |
| 3 | Relation items with incompatible column types | **NO GAP** in detection (identical `Relation.checkKeyTypes` call in both editor and builder); secondary finding: severity mismatch (editor blocks save = error-equivalent, builder default = `WARNING`) | Optional: bump `ServoyBuilder.RELATION_ITEM_TYPE_PROBLEM` to `ProblemSeverity.ERROR` | Optional: `ServoyBuilder.java:535-536`, `ServoyRelationBuilder.java:406-412` |
| 4 | Valuelist with invalid `databaseValuesType` | **NO GAP** — pure computed getter, not in content spec, cannot be persisted or set by any mechanism including direct file edits | — | — (verified `ValueList.java:128-131`, `StaticContentSpecLoader.java:503-963`) |
| 5 | Form with disallowed `dataSource` format | **NO GAP** — uniform `table == null` check in `ServoyFormBuilder.addFormMarkers` catches every malformed-shape sub-case | — | — (verified `ServoyFormBuilder.java:553-587`, `DeveloperFlattenedSolution.java:178-182`, `AbstractServoyModel.java:114-173`) |
