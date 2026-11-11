# Spec: SVY-21700 — Quick-fix for the "CSS position form without body part" marker

## 1. Goal

Add a Servoy marker quick-fix for the `FormCssPositionNoBodyPart` build error
("Form … uses CSS position layout but has no body part; it will be shown in record view at
runtime."). The quick-fix clears the form's CSS-position layout flag
(`form.setUseCssPosition(null)`) — the correct action for an abstract base form, which
should carry no layout information (matching how the New Form wizard creates forms). This
productizes the exact manual workaround the reporter uses today (hand-editing each `.frm` to
set `useCssPosition:false`) as a one-click resolution. The builder check keeps firing as
before; the quick-fix is the resolution the user applies.

## 2. Background

### 2.1 The reported symptom

Since 2026.03.2, the four abstract base forms of the `svyImportDialog` module each show a
`FormCssPositionNoBodyPart` ERROR marker. They are abstract bases (extended by concrete
subforms that supply the body) but persist `useCssPosition:true`, so the builder flags them.
The only current workaround is to hand-edit each `.frm` to `useCssPosition:false`.

### 2.2 Where the marker comes from

`ServoyFormBuilder.addFormMarkers`
(`com.servoy.eclipse.model/src/com/servoy/eclipse/model/builder/ServoyFormBuilder.java`,
~lines 734-742):

```java
// check for css position form without a body part (will fall back to record view at runtime)
if (!form.isResponsiveLayout() && Boolean.TRUE.equals(form.getUseCssPosition()))
{
    Form flattenedForm = ServoyBuilder.getPersistFlattenedSolution(form, fs).getFlattenedForm(form);
    if (flattenedForm != null && !flattenedForm.hasPart(Part.BODY))
    {
        ServoyMarker mk = MarkerMessages.FormCssPositionNoBodyPart.fill(form.getName());
        ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART,
            IMarker.PRIORITY_NORMAL, null, form);
    }
}
```

- `MarkerMessages.FormCssPositionNoBodyPart` (`MarkerMessages.java:314-316`) uses marker
  **type** `ServoyBuilder.PROJECT_FORM_MARKER_TYPE` (`com.servoy.eclipse.core.formProblem`)
  — a type shared by many other form checks.
- Severity pair `ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART` (`ServoyBuilder.java:442`) is
  `ProblemSeverity.ERROR`.

### 2.3 The quick-fix infrastructure

- `com.servoy.eclipse.core.quickfix.ServoyQuickFixGenerator` implements
  `org.eclipse.ui.IMarkerResolutionGenerator`. Its `getResolutions(IMarker)` switches on
  `marker.getType()` and returns an `IMarkerResolution[]` per type.
- `com.servoy.eclipse.core/plugin.xml` registers `ServoyQuickFixGenerator` against many
  marker types via `org.eclipse.ui.ide.markerResolution` extensions, **including
  `com.servoy.eclipse.core.formProblem`** (already present). So no new plugin.xml entry is
  required for the `PROJECT_FORM_MARKER_TYPE` — but there is currently **no code branch** for
  that type in `getResolutions`, so it offers no fixes today.
- Existing single-form quick-fixes (e.g. `ClearLabelForElementProperty`) follow this shape:
  resolve the persist from marker attributes (`Uuid` + `SolutionName`), mutate it, then
  `servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true)`.

### 2.4 The problem for a quick-fix: no Uuid on this marker

`ServoyBuilder.addMarker(...)` (`ServoyBuilder.java:~3419-3426`) only sets marker attributes
per type. For `PROJECT_FORM_MARKER_TYPE` it sets **`SolutionName` only** — it does **not**
set a `Uuid`. A quick-fix therefore has no direct handle to the specific form. Since
`formProblem` is a shared type used by other checks too, the quick-fix must also distinguish
*this* message from other `formProblem` markers.

### 2.5 Git history

- The check/marker and its ERROR severity are present on the current `lts_2026` branch
  (checkout `C:\Users\vosti\git_2026.3\servoy-eclipse`), matching the reporter's "2026.03.2
  only" observation.
- `ServoyQuickFixGenerator` + `plugin.xml` already wire `formProblem` to the generator; the
  only missing piece is the code branch + the marker's `Uuid`.

## 3. Design

Chosen approach (user decision): **A — add a `Uuid` attribute to this marker**, and have the
quick-fix resolve the form by `Uuid` + `SolutionName`, matching the css-no-body case by
message text so it is not offered on unrelated `formProblem` markers. Keep the marker firing
as-is (no severity change, no predicate narrowing).

### 3.1 Builder: carry the form UUID on this marker

The `FormCssPositionNoBodyPart` marker needs to identify its form. Rather than widen the
generic `PROJECT_FORM_MARKER_TYPE` attribute handling (which would add a `Uuid` to every
`formProblem` marker and risk affecting other code that reads these markers), set the `Uuid`
**narrowly, only for this marker**, right where it is raised.

In `ServoyFormBuilder.addFormMarkers`, change the css-no-body block to capture the created
marker, set the form UUID on it, and set a dedicated boolean flag attribute that identifies
this marker (so the quick-fix can discriminate it from other `formProblem` markers without
relying on the message text):

```java
ServoyMarker mk = MarkerMessages.FormCssPositionNoBodyPart.fill(form.getName());
IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
    ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART, IMarker.PRIORITY_NORMAL, null, form);
if (marker != null)
{
    marker.setAttribute("Uuid", form.getUUID().toString());
    marker.setAttribute(ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART_ATTRIBUTE, Boolean.TRUE);
}
```

`ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART_ATTRIBUTE` is a new `public static final String`
constant (`"cssPositionNoBodyPart"`) added next to the severity pair.

`ServoyBuilder.addMarker(...)` returns the created `IMarker` (it already sets `SolutionName`
for `PROJECT_FORM_MARKER_TYPE`), so after the call the marker carries both `SolutionName` and
`Uuid`. Wrap the `setAttribute` in the existing checked-exception handling style of
`addFormMarkers` (the method already declares/handles `CoreException` around marker work — do
it consistently with the surrounding code; if the enclosing scope does not already catch it,
use a small try/catch that logs via `ServoyLog`/`ModelUtils` as neighbouring code does).

Rationale for the message-based discrimination in the quick-fix (below): because
`PROJECT_FORM_MARKER_TYPE` is shared, other `formProblem` markers may now also be inspected by
the new quick-fix branch; the branch must only offer the fix when the marker is actually the
css-no-body one.

### 3.2 Quick-fix class: clear the layout flag

Add `com.servoy.eclipse.core.quickfix.ClearFormCssPositionQuickFix implements
IMarkerResolution` (new file in the same package as the other quick-fixes), modelled on
`ClearLabelForElementProperty`:

- Constructor takes the form `Uuid` (String) and `SolutionName` (String).
- `getLabel()`: e.g. `"Clear CSS position layout flag of form '<name>'"` (resolve the form to
  show its name; fall back to the UUID if unavailable).
- `run(IMarker)`:
  1. Resolve the `ServoyProject` via
     `ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName)`.
  2. Resolve the form persist via
     `AbstractRepository.searchPersist(servoyProject.getEditingSolution(), UUID.fromString(uuid))`
     (use the **editing** solution so the change is saved, as other quick-fixes do).
  3. If it is a `Form`, call `form.setUseCssPosition(null)` (clear the flag; `null` is the
     "unset" state a plain abstract form has — verify against `Form.getUseCssPosition()` /
     the setter signature and the persisted default; if the API requires a Boolean, use the
     value that yields the same persisted state as a freshly-created abstract form, i.e. the
     property absent / default). Do **not** touch `responsiveLayout` (out of scope — user
     chose "clear useCssPosition only").
  4. Save: `servoyProject.saveEditingSolutionNodes(new IPersist[] { form }, true);`
  5. Log failures via `ServoyLog.logError`.

### 3.3 Generator: add the branch for PROJECT_FORM_MARKER_TYPE

In `ServoyQuickFixGenerator.getResolutions(IMarker)`, add an
`else if (type.equals(ServoyBuilder.PROJECT_FORM_MARKER_TYPE))` branch that:

- reads `Uuid` and `SolutionName` attributes;
- **only** offers the fix when the marker is the css-no-body one. Discriminate by a dedicated
  boolean **marker attribute flag** set by the builder (§3.1):
  `marker.getAttribute(ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART_ATTRIBUTE, false)`. This is
  robust against i18n and any change to the message wording (message-substring matching was
  rejected for exactly that fragility).
- when it matches and `Uuid`/`SolutionName` are present, return
  `new IMarkerResolution[] { new ClearFormCssPositionQuickFix(uuid, solName) }`; otherwise
  return the existing empty array (so other `formProblem` markers are unaffected).

**Discrimination is by a marker attribute flag, not the message text.** The builder sets a
boolean marker attribute `ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART_ATTRIBUTE`
(`"cssPositionNoBodyPart"` = `true`) on this specific marker, and the generator branch tests
`marker.getAttribute(FORM_CSS_POSITION_NO_BODY_PART_ATTRIBUTE, false)`. This is robust against
i18n / message-wording changes, unlike matching a substring of the message.

### 3.4 What stays the same

- The builder predicate and the marker firing: unchanged (marker still appears for these
  forms until the user applies the fix).
- Default severity (ERROR): unchanged (separate release-owner decision — Open questions).
- Marker message text and marker type: unchanged.
- No change to `Form.isAbstractForm()` or to runtime `WebFormController` behaviour.

## 4. Implementation plan

1. **Builder — carry the UUID.** In
   `com.servoy.eclipse.model/.../builder/ServoyFormBuilder.java` `addFormMarkers`, capture the
   `IMarker` returned by `ServoyBuilder.addMarker(...)` for the css-no-body case and
   `marker.setAttribute("Uuid", form.getUUID().toString())` (guard for `null` marker; handle
   `CoreException` consistently with the surrounding code).
2. **New quick-fix class.** Create
   `com.servoy.eclipse.core/.../quickfix/ClearFormCssPositionQuickFix.java` implementing
   `IMarkerResolution`, per §3.2 (resolve form by Uuid+SolutionName on the editing solution,
   `setUseCssPosition(null)`, save; label shows the form name).
3. **Generator branch.** In
   `com.servoy.eclipse.core/.../quickfix/ServoyQuickFixGenerator.java` `getResolutions`, add
   the `PROJECT_FORM_MARKER_TYPE` branch per §3.3 (message-substring discrimination, Uuid +
   SolutionName, return the new quick-fix).
4. **plugin.xml.** Confirm `com.servoy.eclipse.core/plugin.xml` already registers
   `ServoyQuickFixGenerator` for `com.servoy.eclipse.core.formProblem` (it does). No new entry
   needed; add one only if verification shows it missing.
5. Post-edit on each Java file: `organizeImports` → `formatFile` →
   `getCompilationErrors` (fix all) → address top-two-severity SpotBugs.
6. Add the test(s) in §4a and run them.

### 4a. Test changes

Add the test to an **existing** test project that already has a working `integrationpde`
harness, to avoid adding new pom/MANIFEST wiring. `com.servoy.eclipse.ui.tests` already has
the `integration` Maven profile, the `PDEIntegrationTestsBase` launcher target, and a
`Require-Bundle: com.servoy.eclipse.core.tests` for the shared infra — and it is the natural
home since the quick-fix lives in `com.servoy.eclipse.core`/`com.servoy.eclipse.ui`'s area.
Put the class in `com.servoy.eclipse.ui.tests.integrationpde.quickfix` and run it with
`eclipse-pde_runJUnitPluginTests` + launcher
`/com.servoy.eclipse.core.tests/PDEIntegrationTestsBase.launch`. The only wiring needed is a
few extra `Import-Package` entries in that project's MANIFEST (the packages the test imports);
no pom change. The test engineer should:

- Pick the correct project/package by what the code under test needs (resolving a form from a
  `ServoyProject` editing solution and saving requires OSGi/ServoyModel → `integrationpde`).
- **Builder-carries-Uuid test:** create a non-responsive, `useCssPosition==true`, body-less
  form in a test solution; build; find the `FormCssPositionNoBodyPart` marker; assert it has a
  non-null `Uuid` attribute equal to the form's UUID and a `SolutionName`. (Guards the §3.1
  change.)
- **Quick-fix resolves + clears test:** drive `ServoyQuickFixGenerator.getResolutions(marker)`
  for that marker; assert exactly one resolution of type `ClearFormCssPositionQuickFix` is
  returned; run it; assert the form's `getUseCssPosition()` is now cleared (null/false to the
  abstract-form default) and the form was saved. (Guards §3.2–§3.3.)
- **Discrimination test:** for a *different* `formProblem` marker (another form check that
  uses `PROJECT_FORM_MARKER_TYPE`), assert the new branch returns **no**
  `ClearFormCssPositionQuickFix` (so the fix is not offered on unrelated form markers). If no
  convenient second `formProblem` marker exists in a test, assert that a `formProblem` marker
  whose message does **not** contain the css-no-body text yields no such resolution.
- Follow AGENTS.md test rules: no `Assume.*`, no green-for-green assertions, assert the one
  specific expected outcome; integration test deletes its workspace projects in
  `@BeforeAll`/`@BeforeClass`; JUnit 6 Jupiter.

## 5. Acceptance criteria

- [ ] The `FormCssPositionNoBodyPart` marker now carries a `Uuid` attribute equal to the
      form's UUID (plus the existing `SolutionName`).
- [ ] Right-clicking the marker offers a quick-fix labelled to clear the form's CSS position
      layout flag.
- [ ] Applying the quick-fix clears `useCssPosition` on the exact flagged form, saves it, and
      (on the next build) the marker for that form is gone — with no manual `.frm` editing.
- [ ] The quick-fix is offered **only** for the css-no-body marker, not for other
      `com.servoy.eclipse.core.formProblem` markers.
- [ ] The builder check still fires as before (no predicate or severity change).
- [ ] `getUseCssPosition()` after the fix matches the persisted state of a freshly-created
      abstract form (the flag is unset), and `responsiveLayout` is untouched.
- [ ] Tests in §4a pass under the correct runner; no new compilation errors or top-two
      SpotBugs issues in changed files.

## 6. Out of scope

- Narrowing the builder predicate to auto-suppress the marker for extended base forms
  (user chose the quick-fix route; marker keeps firing).
- Lowering the default severity ERROR → WARNING (separate release-owner call — Open
  questions).
- Clearing `responsiveLayout` or any other layout property — only `useCssPosition` is cleared.
- Changing `Form.isAbstractForm()` semantics or runtime `WebFormController` behaviour.
- Changing the marker message text or introducing a brand-new marker type (message-substring
  discrimination on the existing shared type is used instead).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the default severity of `FORM_CSS_POSITION_NO_BODY_PART` also be relaxed from ERROR to WARNING? Kept out of scope unless the release owner asks. | Release owner | Open |
| Is message-substring matching acceptable to discriminate the css-no-body case on the shared `PROJECT_FORM_MARKER_TYPE`, or should a dedicated marker type be introduced (larger change)? | Reviewer | Open |
| Exact "cleared" value for `setUseCssPosition` — confirm `null` yields the same persisted state as a freshly-created abstract form (vs `Boolean.FALSE`). | Implementer | Open |
