# Spec: SVY-21514 — MCP: generateFormSpec/testForm doesn't work with multiple solutions

## 1. Goal

Make the Cypress MCP tools `generateFormSpec` and `testForm` resolve forms the same
module-aware way the rest of the Developer IDE does, so they work for any form in the
active solution **or any of its referenced/inherited modules** — not just forms whose
`.frm` file happens to live directly under the active project's own `forms/` folder.
`FormSpecGenerator.generateSpec(String formName)` must stop doing a raw `IFile`
existence check against the active project and instead resolve the form through
`FlattenedSolution.getForm(formName)`. As a direct consequence of no longer having a
`.frm` file to re-read, the metadata the generator needs (data source, element names,
types, data provider bindings, button/label/web-component classification) must be read
from the resolved `Form` persist object and its elements instead of regex-scraping the
raw `.frm` JSON text.

## 2. Background

### 2.1 Reported problem

The reporter's repro: a `tutorials` form exists and is resolvable through the Servoy
model, but lives in a referenced/inherited module rather than the active project's own
`forms/` directory. Both `generateFormSpec({formName:"tutorials"})` and
`testForm({formName:"tutorials"})` fail with:

```
Error: Form file not found: forms/tutorials.frm
```

even though the form is perfectly valid and reachable through the model.

### 2.2 Root cause (from triage, `docs/SVY-21514-triage.md`)

`com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecGenerator.java`,
method `generateSpec(String formName)` (lines 32-85):

1. Gets only the **active** `ServoyProject`
   (`ServoyModelFinder.getServoyModel().getActiveProject()`) — never consults the
   solution's modules.
2. Resolves the form purely as a workspace resource:
   `activeProject.getProject().getFile("forms/" + formName + ".frm")`. This is scoped
   to a single project's own `forms/` directory; module/import-hierarchy awareness is
   entirely absent.
3. If that file doesn't exist at that exact path, it immediately returns the
   `"Error: Form file not found..."` string and aborts. It never attempts a
   Servoy-model-level resolution.
4. Having found the file, it re-reads and **regex-parses the raw `.frm` JSON text**
   (`parseFrmFile`, lines 201-234) using ad hoc `Pattern`s for `dataSource`, element
   `name`, `typeName`, `dataProviderID`, and substring checks like
   `item.contains("\"typeid\":47")` to classify web components, and
   `item.contains("\"typeid\":7")` + presence/absence of `onActionMethodID` to tell
   buttons from labels. This is fragile (breaks on nested JSON, escaped quotes,
   components whose `"name"` key doesn't immediately follow in text order) and
   duplicates information the real persist model already exposes in typed form.

Not a regression — git history on the file shows only its introduction
(`f51f46bf4e`, SVY-21296, extracting Cypress tooling into its own plugin) and one
unrelated follow-up (`9149bbb685`, SVY-21323, client shutdown + name-collision
handling). Neither touched the lookup/parsing logic. This is a pre-existing design gap
that only manifests with a real multi-module solution.

### 2.3 Established pattern elsewhere in the codebase

Every other module-aware form lookup in the IDE goes through
`FlattenedSolution.getForm(String)`, obtained from a `ServoyProject`'s
**editing** flattened solution (so in-editor unsaved state is visible), not a raw
`IFile`/`IProject` resource lookup:

- `com.servoy.eclipse.designer.rfb/src/.../endpoint/HeadlessFormTemplateContent.java`
  — `getEditingFlattenedSolution()` → `activeProject.getEditingFlattenedSolution()`,
  then `fs.getForm(formName)` (lines 68-84). This is the closest sibling to the Cypress
  MCP use case: headless, no open editor/client, resolves a form by name from the
  active project for an HTTP/MCP-style endpoint.
- `com.servoy.eclipse.designer.rfb/src/.../startup/DesignerFilter.java` — same
  `activeProject.getEditingFlattenedSolution()` → `fl.getForm(formName)` pattern, with
  a fallback to a "developer projects list" flattened solution when not found in the
  active one (lines 134-143, 575-579).
- Also used by `TestTarget`, `JSUnitTestRunnerUI`, `DeveloperBridge`, and
  `ServoyBuilderUtils` (per triage).

`FlattenedSolution.getForm(String)` flattens across the active solution **and its
modules**, which is exactly the capability missing from `FormSpecGenerator`.

### 2.4 Persist API available once the `Form` is resolved

Once a `Form` is resolved, its elements are available directly as typed persists,
replacing the need to re-read/parse the `.frm` file at all:

- `Form.getDataSource()` — replaces the `DATA_SOURCE_PATTERN` regex.
- `Form.getFormElementsSortedByFormIndex()` → `Iterator<ISupportFormElement>` — the
  form's named elements, replacing the `content.split("\\{")` + per-item regex scan.
  (`ServoyBuilder.java:1415` is the one other caller of this method in the codebase,
  confirming it's safe/idiomatic for iterating a form's own elements.)
- Each element can be narrowed:
  - `com.servoy.j2db.persistence.WebComponent extends BaseComponent` — has
    `getTypeName()` directly (replaces `TYPE_NAME_PATTERN`); `instanceof WebComponent`
    replaces the `item.contains("\"typeid\":47")` check (web components are persisted
    with `IRepository.WEBCOMPONENTS` type id, which is the typeid 47 the regex was
    sniffing for in the raw JSON).
  - `com.servoy.j2db.persistence.GraphicalComponent extends BaseComponent implements
    ISupportDataProviderID` — has `getDataProviderID()` directly (replaces
    `DATA_PROVIDER_PATTERN`) and `getOnActionMethodID()` (non-null/non-empty UUID
    string distinguishes a button from a label — replaces the
    `item.contains("\"typeid\":7") [+ onActionMethodID presence]` checks; `typeid:7`
    is `IRepository.GRAPHICALCOMPONENTS`).
  - All form elements implement `IFormElement` (and thus `ISupportFormElement`),
    exposing `getName()` uniformly.

This lets `parseFrmFile`'s regex extraction be replaced by a straightforward iteration
over `form.getFormElementsSortedByFormIndex()`, narrowing each `IFormElement` by
`instanceof` to `WebComponent` or `GraphicalComponent` to pull `typeName` /
`dataProviderID` / button-vs-label classification, with no file I/O and no regex at
all.

## 3. Design

### 3.1 Form resolution

Replace the `IProject`/`IFile` lookup in `generateSpec(String formName)` with:

```java
ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
if (activeProject == null) {
    return "Error: No active Servoy project.";
}

FlattenedSolution flattenedSolution = activeProject.getEditingFlattenedSolution();
if (flattenedSolution == null) {
    return "Error: Could not resolve the active solution.";
}

Form form = flattenedSolution.getForm(formName);
if (form == null) {
    return "Error: Form not found: " + formName;
}
```

- Use `getEditingFlattenedSolution()` (not `getFlattenedSolution()`), matching
  `HeadlessFormTemplateContent`/`DesignerFilter` — this is the convention sibling
  services in this area already use for active-project form lookups, and it reflects
  any unsaved editor state, which matters for an interactive MCP/dev-tooling use case.
- `FlattenedSolution.getForm(String)` already searches the active solution and all its
  modules, so no extra module-iteration logic is needed in `FormSpecGenerator` itself —
  this one call is the entire fix for the reported bug.
- No fallback to a "developer projects list" solution (as `DesignerFilter` has) is
  needed here: that fallback exists for a different scenario (editor open on a project
  that isn't the active one) that doesn't apply to this MCP tool, which always operates
  against the active project's solution scope. Out of scope for this ticket.
- The error message changes from `"Error: Form file not found: forms/" + formName +
  ".frm"` to `"Error: Form not found: " + formName` since there is no longer a specific
  file path to report — the form simply isn't resolvable anywhere in the active
  solution or its modules.

### 3.2 Metadata extraction without `.frm` file I/O

Remove the `Files.readAllBytes(frmFile...)` read and the `parseFrmFile(String content,
String formName)` method entirely. Replace with a new
`FormMetadata buildMetadata(Form form)` (or similarly named) method that reads directly
from the persist model:

```java
private FormMetadata buildMetadata(Form form) {
    FormMetadata metadata = new FormMetadata();
    metadata.formName = form.getName();
    metadata.dataSource = form.getDataSource();

    Iterator<ISupportFormElement> elements = form.getFormElementsSortedByFormIndex();
    while (elements.hasNext()) {
        ISupportFormElement element = elements.next();
        if (!(element instanceof IFormElement formElement)) {
            continue;
        }
        String name = formElement.getName();
        if (name == null || name.equals(form.getName())) {
            continue;
        }

        ElementInfo elem = new ElementInfo();
        elem.name = name;
        elem.isWebComponent = element instanceof WebComponent;
        if (elem.isWebComponent) {
            elem.typeName = ((WebComponent)element).getTypeName();
        }
        if (element instanceof GraphicalComponent gc) {
            elem.dataProviderID = gc.getDataProviderID();
            boolean hasAction = gc.getOnActionMethodID() != null && !gc.getOnActionMethodID().isEmpty();
            elem.isButton = hasAction;
            elem.isLabel = !hasAction;
        }

        metadata.namedElements.add(elem);
    }

    return metadata;
}
```

(Exact null-handling / UUID-string checks to be adapted to match
`GraphicalComponent.getOnActionMethodID()`'s real return contract — confirm during
implementation whether it returns `null`, an empty string, or a UUID string for "no
method set", and compare against how other callers in the codebase check this method,
to keep button/label classification behaviourally identical to the regex it replaces.)

`generateSpec` then calls `metadata = buildMetadata(form); metadata.solutionName =
activeProject.getSolution().getName();` in place of the old
read-file-then-`parseFrmFile` sequence. `generateCypressSpecContent` and
`generateSetupContent` (the two consumers of `FormMetadata`) are unchanged — they only
read fields off `FormMetadata`/`ElementInfo`, which keep the same shape.

### 3.3 Imports / dependencies

- Remove: `java.nio.file.Files` read-of-`.frm` usage stays for the *output* spec files
  (still written via `Files.writeString`/`Files.exists` elsewhere in the class) — only
  the `.frm`-file-specific read goes away. `java.util.regex.Matcher`/`Pattern` and the
  four `*_PATTERN` constants are removed entirely, as is `IFile`/`IProject` (no longer
  needed once the lookup goes through `FlattenedSolution`).
- Add: `com.servoy.j2db.FlattenedSolution`, `com.servoy.j2db.persistence.Form`,
  `com.servoy.j2db.persistence.IFormElement`, `com.servoy.j2db.persistence.ISupportFormElement`,
  `com.servoy.j2db.persistence.WebComponent`, `com.servoy.j2db.persistence.GraphicalComponent`,
  `java.util.Iterator`.
- `com.servoy.eclipse.model.nature.ServoyProject` and
  `com.servoy.eclipse.model.ServoyModelFinder` imports are kept (still used to get the
  active project). All of these types already live in `servoy_shared`/`com.servoy.eclipse.model`,
  both of which `com.servoy.eclipse.cypress` already depends on (per the existing
  `ServoyProject`/`ServoyModelFinder` usage) — no new `MANIFEST.MF` dependency should be
  required, but confirm `Require-Bundle`/`Import-Package` coverage for `servoy_shared`
  during implementation and add it if a compile error surfaces.

### 3.4 `testForm` / `FormSpecRunner`

`FormSpecRunner.runFormCypressTests` does not itself do form-file resolution — it only
looks up the **generated spec file** via `specGenerator.findExistingSpecFile(formName,
solutionName)` (disk-based, under `jenkins-custom/e2e-test-scripts/...`, unrelated to
the `.frm` lookup bug). The reported `testForm` failure is a consequence of
`showFormInBrowser`/`generateFormSpec` failing first (per the ticket's own repro, both
calls hit the same `.frm`-not-found error) — fixing `FormSpecGenerator.generateSpec` is
therefore sufficient to fix both reported symptoms. No change is needed in
`FormSpecRunner` itself.

### 3.5 Git history

Confirmed via triage (see `docs/SVY-21514-triage.md`): only two commits exist on
`FormSpecGenerator.java` — `f51f46bf4e` (SVY-21296, file's introduction, carried the
`IFile`-based lookup over from wherever the Cypress tooling was extracted from) and
`9149bbb685` (SVY-21323, unrelated client-shutdown/name-collision follow-up). The
module-resolution logic has never been touched; this is a day-one design gap, not a
regression.

## 4. Implementation plan

1. In `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecGenerator.java`:
   - Remove the four `*_PATTERN` `Pattern` constants and the `java.util.regex.Matcher`/`Pattern` imports.
   - Remove the `org.eclipse.core.resources.IFile`/`IProject` imports (no longer used).
   - Add imports: `com.servoy.j2db.FlattenedSolution`, `com.servoy.j2db.persistence.Form`,
     `com.servoy.j2db.persistence.IFormElement`, `com.servoy.j2db.persistence.ISupportFormElement`,
     `com.servoy.j2db.persistence.WebComponent`, `com.servoy.j2db.persistence.GraphicalComponent`,
     `java.util.Iterator`.
   - In `generateSpec(String formName)`: replace the `IProject project = ...` /
     `IFile frmFile = ...` / existence-check block with
     `activeProject.getEditingFlattenedSolution()` → `flattenedSolution.getForm(formName)`,
     returning `"Error: Form not found: " + formName` when `null`.
   - Replace the `Files.readAllBytes(frmFile...)` + `parseFrmFile(frmContent, formName)`
     call with a call to the new `buildMetadata(form)` method (see §3.2).
   - Remove the `parseFrmFile(String content, String formName)` method.
   - Add the new `private FormMetadata buildMetadata(Form form)` method per §3.2.
   - Leave `generateCypressSpecContent`, `generateSetupContent`, `getFormUrl`,
     `FormMetadata`, `ElementInfo`, and all the disk-path helper methods
     (`resolveFormSpecDir`, `specExists`, `getSpecFilePath`, `findExistingSpecFile`,
     etc.) unchanged — they are unaffected by this fix.
2. Check `com.servoy.eclipse.cypress/META-INF/MANIFEST.MF`: confirm `servoy_shared` is
   reachable (via existing `Require-Bundle`/`Import-Package` or transitively through an
   already-required bundle). Add an explicit dependency only if a compile error shows
   it's missing — use `eclipse-coder` MANIFEST.MF-safe editing per the repo's gotchas
   (strict 72-byte line length).
3. Run `eclipse-ide_getCompilationErrors` on the file after edits; apply quick fixes or
   fix manually; `eclipse-coder_organizeImports`; `eclipse-coder_formatFile`.
4. Manually verify behaviourally (no existing automated test project covers
   `com.servoy.eclipse.cypress`): with a multi-module test solution open in Developer,
   call `generateFormSpec`/`testForm` for a form that lives only in a referenced module
   (not the active project's own `forms/` folder) and confirm it now succeeds and
   produces the same shape of `.spec.cy.js`/`.spec.js` output as before for a
   same-project form (regression check: run it once for a form in the active project's
   own `forms/` folder too, to confirm the new persist-based `buildMetadata` produces
   equivalent `ElementInfo` data — in particular button vs. label classification —
   to the old regex-based `parseFrmFile` for at least one form with buttons and one
   with plain labels/web components).

## 5. Acceptance criteria

- [ ] `generateFormSpec({formName: "<form in a referenced module>"})` succeeds (no
      longer returns `"Error: Form file not found: forms/<name>.frm"`) for a form that
      exists only in a module of the active solution, not the active project's own
      `forms/` directory.
- [ ] `testForm({formName: "<form in a referenced module>"})` likewise succeeds (via
      the fixed `generateFormSpec`/`showFormInBrowser` path it depends on).
- [ ] `generateFormSpec` still succeeds, unchanged in output shape, for a form that
      lives directly in the active project's own `forms/` folder (no regression for
      the previously-working case).
- [ ] Generated `.spec.cy.js` content for at least one form with buttons and one with
      plain labels/web-components matches the previous regex-based output's element
      classification (button vs. label vs. web-component, `dataProviderID`,
      `typeName`), confirming the persist-API rework is behaviourally equivalent.
- [ ] No `.frm` file is read or regex-parsed anywhere in `FormSpecGenerator` after the
      change (`parseFrmFile` and the `*_PATTERN` constants are removed).
- [ ] `eclipse-ide_getCompilationErrors` reports no errors on
      `FormSpecGenerator.java` after the change.
- [ ] Calling with a form name that doesn't exist anywhere in the active solution or
      its modules still returns a clear `"Error: Form not found: <name>"` message
      rather than throwing.

## 6. Out of scope

- `FormSpecRunner` (test execution) is not changed — its spec-file lookup is
  disk-based and unrelated to the `.frm`/model resolution bug.
- No fallback to a "developer projects list" flattened solution (as seen in
  `DesignerFilter`) is added — not needed for this MCP tool's use case.
- No change to the generated `.spec.cy.js`/`.spec.js` file naming, directory layout, or
  content *format* — only how the input `FormMetadata` is derived.
- No automated JUnit test project is created for `com.servoy.eclipse.cypress` in this
  ticket (none currently exists); verification is manual per §4 step 4.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Exact null/empty contract of `GraphicalComponent.getOnActionMethodID()` for "no method set" (null vs. empty string) — needs confirming during implementation to keep button/label classification identical to the old regex. | Implementer | open |
| Whether `servoy_shared` needs an explicit new `MANIFEST.MF` dependency in `com.servoy.eclipse.cypress`, or is already reachable transitively. | Implementer | open |
