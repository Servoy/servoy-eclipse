# Spec: SVY-21356 — Add builder error markers for invalid artifacts that bypass UI wizard validation

## 1. Goal

Close two narrow gaps between the Servoy Developer wizard-time validation and the
`ServoyBuilder` (build-time) validation, so that forms/relations created or edited
outside the wizard (via AI MCP tools or direct `.frm`/`.rel` file editing) get the
same Problems-view feedback a user going through the UI wizards would have been
protected from missing:

1. A CSS-position form with no `Part.BODY` gets a new **error** marker in the
   Problems view (the wizard structurally cannot produce this state; only direct
   file edits or a regressed/future MCP tool can).
2. A relation item with a key-type mismatch (`Relation.checkKeyTypes()` failure)
   gets **error** severity instead of the current default **warning**, matching
   the interactive `RelationEditor`, which already treats this as save-blocking.

Both items were confirmed by direct code investigation
(`docs/SVY-21356-investigation.md`); the other three candidate gaps from the raw
ticket were investigated and found to be NO GAP — see section 6.

## 2. Background

The ticket's premise is that AI MCP tools and direct file edits bypass the
UI wizards (`NewFormWizard`, `NewRelationWizard`, `NewValueListWizard`), which
normally prevent invalid configurations interactively. One such gap was already
fixed for DB valuelists with no dataproviders set (SVY-21281,
`ServoyBuilder.VALUELIST_DB_NO_DATAPROVIDERS` /
`MarkerMessages.ValuelistDBNoDataproviders`, in
`ServoyValuelistBuilder.java:315-328`). This ticket audits the same
wizard-vs-builder pattern for `NewFormWizard` and `NewRelationWizard`.

An independent investigation (`docs/SVY-21356-investigation.md`) examined five
candidate gaps in depth, verifying each claim against the current source rather
than trusting an earlier triage pass. Two were confirmed as real, actionable
items (below); three were confirmed as **NO GAP** and are explicitly out of
scope for this spec (section 6).

All target files live in the `servoy-eclipse` repository (already open in this
workspace), **not** in Servoy-Copilot:
- `com.servoy.eclipse.model/src/com/servoy/eclipse/model/builder/MarkerMessages.java`
- `com.servoy.eclipse.model/src/com/servoy/eclipse/model/builder/ServoyBuilder.java`
- `com.servoy.eclipse.model/src/com/servoy/eclipse/model/builder/ServoyFormBuilder.java`
- `com.servoy.eclipse.model/src/com/servoy/eclipse/model/builder/ServoyRelationBuilder.java`
- `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/preferences/ServoyErrorWarningPreferencePage.java`
- `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/Messages.java`
- `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/messages.properties`

**Compatibility note (Item B):** raising `RELATION_ITEM_TYPE_PROBLEM` from
`WARNING` to `ERROR` is a behavior change for existing projects. Any relation
that already has a key-type mismatch and previously showed only as a build
*warning* will now show as a build **error** after this change ships. This is
called out explicitly as an open question in section 7 rather than silently
absorbed into the implementation — the team should confirm this is acceptable
before release, since it could surface new build errors in existing customer
projects on upgrade (the relation itself is not newly broken; only its build
severity changes).

## 3. Design

### 3.1 CSS-position form with no Body part marker

**Trigger condition** — a form is flagged when both of these hold:
- `!form.isResponsiveLayout()`
- `Boolean.TRUE.equals(form.getUseCssPosition())`

and its flattened form has no `Part.BODY`:
- `Form flattenedForm = ServoyBuilder.getPersistFlattenedSolution(form, fs).getFlattenedForm(form);`
- `!flattenedForm.hasPart(Part.BODY)`

This mirrors the exact runtime predicate in
`servoy_ngclient/.../WebFormController.java:110-118`
(`!application.getFlattenedSolution().getFlattenedForm(form).hasPart(IPartConstants.BODY)`),
which falls back to record view and logs a JS console warning — this marker
surfaces that same condition in the IDE Problems view at build/edit time instead
of only at runtime.

Why this condition and not "any anchored form with zero parts": for a plain
anchored (non-CSS-position) form, zero parts is the exact same persisted state
as a legitimate wizard-created "Abstract (no UI)" form — `Form.isAbstractForm()`
treats that as intentional. A CSS-position form with zero parts, however, is a
state `NewFormWizard.performFinish()` can never produce (it unconditionally
calls `form.createNewPart(Part.BODY, ...)` for any non-abstract, non-responsive
form, and `isAbstractForm()` treats `useCssPosition == true` as automatically
"not abstract" regardless of parts) — so this narrower condition has no false
positives on legitimate wizard output.

**New `MarkerMessages.java` entry** (add near `FormDuplicatePart`):
```java
public static ServoyMarker FormCssPositionNoBodyPart = new ServoyMarker(
    "Form \"{0}\" uses CSS position layout but has no body part; it will be shown in record view at runtime.",
    ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
```

**New `ServoyBuilder.java` severity-pair constant** (add near
`FORM_DUPLICATE_PART`, `ServoyBuilder.java:441`):
```java
public final static Pair<String, ProblemSeverity> FORM_CSS_POSITION_NO_BODY_PART =
    new Pair<String, ProblemSeverity>("formCssPositionNoBodyPart", ProblemSeverity.ERROR);
```
**Severity revised to `ERROR` during implementation** (original spec draft
proposed `WARNING`; changed by the implementer before code review). Rationale:
although the runtime degrades gracefully (falls back to record view rather
than crashing), the form's intended CSS-position layout is silently lost for
the end user — this is a real functional defect for that form, not merely an
IDE-visibility nicety. `ERROR` also matches the severity given to structurally
invalid form state elsewhere in the same builder (e.g. `FORM_INVALID_TABLE`,
`FORM_DUPLICATE_PART`), and ensures the condition fails a headless/CI build
rather than being easy to overlook as a warning.

**Builder call site** — `ServoyFormBuilder.addFormMarkers(...)`, near the
existing duplicate-part check (`ServoyFormBuilder.java:702-721`), following the
exact pattern used for `ValuelistDBNoDataproviders`
(`ServoyValuelistBuilder.java:315-328`, from SVY-21281): look up the custom
severity via `ServoyBuilder.getSeverity(...)`, skip if `IGNORE`, else build the
marker via `MarkerMessages.FormCssPositionNoBodyPart.fill(form.getName())` and
add it with `ServoyBuilder.addMarker(...)`.

```java
if (!form.isResponsiveLayout() && Boolean.TRUE.equals(form.getUseCssPosition()))
{
    Form flattenedForm = ServoyBuilder.getPersistFlattenedSolution(form, fs).getFlattenedForm(form);
    if (flattenedForm != null && !flattenedForm.hasPart(Part.BODY))
    {
        String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART.getLeft(),
            ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART.getRight().name(), form);
        if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
        {
            ServoyMarker mk = MarkerMessages.FormCssPositionNoBodyPart.fill(form.getName());
            ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
                ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART, IMarker.PRIORITY_NORMAL, null, form);
        }
    }
}
```
Note: `ServoyBuilder.addMarker(resource, type, message, lineNumber, Pair<String,ProblemSeverity>, priority, location, persist)`
already performs the `getSeverity`/`IGNORE` check internally (see
`ServoyBuilder.java:3294-3306`), so the explicit `getSeverity`/`IGNORE` guard
above is redundant with that overload — mirror whichever pattern
`ServoyValuelistBuilder` actually uses (it builds a `Problem` object directly
rather than calling `addMarker`, since valuelist marker collection works
differently); the implementer should follow `ServoyFormBuilder`'s own existing
idiom (`addMarker` with the `Pair` overload, as used for `FormDuplicatePart`
at `ServoyFormBuilder.java:716-719`) rather than copy the valuelist builder's
`Problem`-list idiom verbatim, since they use different internal collection
mechanisms. `form` is used for persist-scoped severity lookup and as the marker
location.

**Import needed:** `com.servoy.j2db.persistence.Part` is already imported in
`ServoyFormBuilder.java:100`.

**Preference registration:** add a new `ErrorWarningPreferenceItem` in
`ServoyErrorWarningPreferencePage.getAssociatedProblemMarkers(...)`, in the
`ERROR_WARNING_FORM_PROBLEMS` branch (`ServoyErrorWarningPreferencePage.java:491-…`,
alongside `FORM_DUPLICATE_PART` at lines 503-504), following the exact same
three-argument pattern:
```java
associatedProblemMarkers.add(
    new ErrorWarningPreferenceItem(ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART,
        Messages.ErrorWarningPreferencePage_formCssPositionNoBodyPart, true));
```
Add the corresponding field to `Messages.java` (near
`ErrorWarningPreferencePage_formDuplicatePart` at line 255):
```java
public static String ErrorWarningPreferencePage_formCssPositionNoBodyPart;
```
And the resource string to `messages.properties` (near the
`formDuplicatePart`/`relationItemTypeProblem` entries):
```
ErrorWarningPreferencePage_formCssPositionNoBodyPart=Form with CSS position layout has no body part
```

### 3.2 Relation item type-mismatch severity fix

**Change 1** — `ServoyBuilder.java:535-536`, severity `WARNING` → `ERROR`:
```java
public final static Pair<String, ProblemSeverity> RELATION_ITEM_TYPE_PROBLEM = new Pair<String, ProblemSeverity>("relationItemTypeProblem",
    ProblemSeverity.ERROR);
```

**Change 2** — `ServoyRelationBuilder.java:406-412`, bump marker priority to
match the new severity:
```java
mk = MarkerMessages.RelationItemTypeProblem.fill(element.getName(), typeMismatchWarning);
ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
    ServoyBuilder.RELATION_ITEM_TYPE_PROBLEM, IMarker.PRIORITY_NORMAL,
    null, element);
```
(only the `IMarker.PRIORITY_LOW` → `IMarker.PRIORITY_NORMAL` argument changes).

Rationale: `Relation.checkKeyTypes(IDataProviderHandler)` is the identical
predicate called from both `RelationEditor.createAndcheck()` (which **blocks
save** with an error dialog when it returns non-null,
`RelationEditor.java:762-788, 822-908`) and
`ServoyRelationBuilder.checkRelation()` (`ServoyRelationBuilder.java:406-412`).
The detection is already correct and complete in both paths — this is a pure
severity-parity fix, not a new check.

**No preference-page change needed** — `RELATION_ITEM_TYPE_PROBLEM` is already
registered as an `ErrorWarningPreferenceItem` at
`ServoyErrorWarningPreferencePage.java:452-453`; only its default severity
constant changes, the registration itself is untouched.

## 4. Implementation plan

1. Add `ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART` constant
   (`ServoyBuilder.java`).
2. Add `MarkerMessages.FormCssPositionNoBodyPart` (`MarkerMessages.java`).
3. Add the CSS-position/no-body-part check to
   `ServoyFormBuilder.addFormMarkers(...)`, near the existing duplicate-part
   check.
4. Register the new marker in `ServoyErrorWarningPreferencePage` (form
   problems section) + add `Messages.java` field +
   `messages.properties` entry.
5. Change `ServoyBuilder.RELATION_ITEM_TYPE_PROBLEM` severity from `WARNING`
   to `ERROR`.
6. Change the marker priority in `ServoyRelationBuilder.java:411` from
   `IMarker.PRIORITY_LOW` to `IMarker.PRIORITY_NORMAL`.
7. Compile-check the `com.servoy.eclipse.model` and `com.servoy.eclipse.ui`
   projects; run/adapt any existing builder unit/integration tests that cover
   `ServoyFormBuilder` or `ServoyRelationBuilder` marker output, and add new
   ones if none exist for these specific paths.

## 5. Acceptance criteria

- [ ] A CSS-position, non-responsive form saved with zero parts (e.g. via
      direct `.frm` edit or a hand-crafted MCP form-creation call that skips
      `createNewPart(Part.BODY, ...)`) produces a `formCssPositionNoBodyPart`
      **error** marker referencing the form by name.
- [ ] A CSS-position form that *does* have a `Part.BODY` produces no such
      marker.
- [ ] A responsive-layout form, or a non-CSS-position anchored form with zero
      parts (legitimate "Abstract" form), produces no such marker (no
      regression / no false positive).
- [ ] The new marker's severity is configurable via the Error/Warning
      preference page under "Form problems", following the same UI pattern as
      `formDuplicatePart`.
- [ ] A relation item with a `checkKeyTypes()` type mismatch now produces an
      **error**-severity marker (previously warning), with `PRIORITY_NORMAL`.
- [ ] `com.servoy.eclipse.model` and `com.servoy.eclipse.ui` compile with zero
      errors after the change.
- [ ] No existing test that asserts a `WARNING` severity for
      `relationItemTypeProblem` is left silently broken — any such test is
      updated to expect `ERROR`, or the discrepancy is flagged before merge.

## 6. Out of scope

The other three candidate gaps investigated in
`docs/SVY-21356-investigation.md` were found to be **NO GAP** and are
explicitly excluded from this spec:

- **Form elements with invalid/non-existent valuelist references** — already
  covered by the generic `ELEMENTS`-typed dangling-reference check in
  `ServoyFormBuilder.addFormMarkers` (`ServoyFormBuilder.java:157-320`) via
  `ServoyBuilderUtils.addNullReferenceMarker`; applies uniformly to
  `Field.valuelistID` with no special-casing needed.
- **Valuelist with invalid `databaseValuesType`** — `getDatabaseValuesType()`
  is a pure computed getter (`ValueList.java:128-131`) with no backing
  content-spec property; it cannot be persisted, set via the solution model,
  MCP tools, or direct file edits by any mechanism, so no marker is possible
  or needed.
- **Form with a disallowed `dataSource` format** — the generic
  `table == null` check in `ServoyFormBuilder.addFormMarkers`
  (`ServoyFormBuilder.java:553-587`) is format-agnostic and already raises
  `FORM_INVALID_TABLE` (`ERROR`) for every malformed-datasource shape (wrong
  scheme, missing table segment, unknown server/table, or garbage string).

Reference `docs/SVY-21356-investigation.md` for the full analysis and code
citations backing each verdict.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Is it acceptable that raising `RELATION_ITEM_TYPE_PROBLEM` to `ERROR` may surface new build errors in existing customer projects that already contain a relation with a key-type mismatch previously shown only as a warning? | Team / release owner | open |
| Should the new `formCssPositionNoBodyPart` preference key also get a dedicated "quick fix" (e.g. offer to add a Body part) the way some other form markers do, or is a Problems-view entry sufficient for this ticket? | Team | open |
| Confirm exact current line numbers in `ServoyFormBuilder.java`/`ServoyRelationBuilder.java`/`ServoyBuilder.java` at implementation time, since this spec's citations were captured at investigation/spec time and the file may have shifted slightly by the time implementation starts. | Implementer | resolved (verified via git diff at review time) |
| Is `ERROR` severity (rather than the originally-proposed `WARNING`) acceptable for `formCssPositionNoBodyPart`, given it can fail existing headless/CI builds for any project with a pre-existing CSS-position form lacking a body part? | Team / release owner | open |
</content>
