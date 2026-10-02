# Triage Report — SVY-21514

**Verdict:** PROCEED

## Reported problem

The MCP tools `generateFormSpec` (backed by `FormSpecGenerator.generateSpec(String formName)`)
and `testForm` fail with `Error: Form file not found: forms/<formName>.frm` whenever the
named form does not physically live as a `.frm` file directly under the **active project's**
`forms/` folder — e.g. when the form lives in a referenced/inherited module of a multi-module
solution. The reporter's repro (`tutorials` form) hit this directly: both `generateFormSpec`
and `testForm` returned the same "Form file not found" error even though `tutorials` exists
and is resolvable through the Servoy model (just not in the active project's own `forms/`
directory).

The attached screenshot of `FormSpecGenerator.java` circles exactly this block:

```java
IProject project = activeProject.getProject();
IFile frmFile = project.getFile("forms/" + formName + ".frm");
if (!frmFile.exists()) {
    return "Error: Form file not found: forms/" + formName + ".frm";
}
```

## Root-cause assessment

Confirmed by reading `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecGenerator.java`
(lines 32-85, `generateSpec`):

1. It gets the **active** `ServoyProject` only (`ServoyModelFinder.getServoyModel().getActiveProject()`), never consults the solution's modules.
2. It resolves the form purely as a workspace resource: `activeProject.getProject().getFile("forms/" + formName + ".frm")`. This is a raw `IProject`/`IFile` lookup scoped to a single project's own `forms/` directory — module/import-hierarchy awareness is entirely absent.
3. If that file literally doesn't exist at that path, it immediately returns the `"Error: Form file not found..."` string and aborts — it never attempts a Servoy-model-level resolution (`FlattenedSolution.getForm(formName)`), which is the mechanism used everywhere else in the codebase for name-based form lookup across modules.
4. Having found the file, it re-reads and **regex-parses the raw `.frm` JSON text** (`parseFrmFile`, lines 201-234, using ad hoc `Pattern`s for `dataSource`, `name`, `typeName`, `dataProviderID`, and substring checks like `item.contains("\"typeid\":47")`) instead of using the real `Form`/`IFormElement` persist model. This is a second, related code-smell: Servoy already has typed persist APIs for this (`Form`, `IFormElement`, `BaseComponent`, `WebComponent`, etc. — see e.g. `HeadlessFormTemplateContent.java` iterating `wrapper.getBaseComponents()` as `IFormElement`), so hand-rolled regex scraping of serialized JSON is fragile and unrelated to the reported bug but sits right next to it in the same method.

Elsewhere in the codebase, the correct, module-aware pattern for resolving a form by name is
consistently: get a `FlattenedSolution` (via `ServoyProject.getFlattenedSolution()` /
`getEditingFlattenedSolution()`, or `ServoyModelFinder.getServoyModel().getFlattenedSolution()`),
then call `.getForm(formName)` — see `TestTarget.java:202/218`, `JSUnitTestRunnerUI.java:420/435`,
`DesignerFilter.java:135/143/579`, `DeveloperBridge.java:71`, `HeadlessFormTemplateContent.java:84`,
`ServoyBuilderUtils.java:136`. `FlattenedSolution.getForm(String)` flattens across the active
solution **and its modules**, which is exactly the capability `FormSpecGenerator` is missing.

### Not a regression

`git log` on the file shows only two commits:
- `f51f46bf4e` — "SVY-21296 extract Cypress testing into standalone plugin [ai]" (the file's introduction, carrying the IFile-based lookup over from wherever it was extracted from)
- `9149bbb685` — "SVY-21323 shutdown formpreview clients after tests and fix form name collisions [ai]"

Neither commit touched the module-resolution logic; the raw-IFile lookup has been there since
the plugin was created. This is not a regression — it is a pre-existing design limitation that
only manifests once a real multi-module solution is tested (the reporter's scenario), which
likely wasn't part of the original single-project test scope.

## Ticket premise check

The reporter's diagnosis and proposed fix direction are both correct and align with the
codebase's established pattern:

- **Problem diagnosis is accurate**: the code is scoped to the active project's own `forms/`
  folder via raw `IFile`/`IProject` APIs, so forms in other modules are invisible to it.
- **Proposed fix is the right one and matches existing conventions**: resolve forms via
  `FlattenedSolution.getForm(formName)` (reachable from `ServoyModelFinder.getServoyModel().getFlattenedSolution()`,
  or more precisely `activeProject.getFlattenedSolution()`/`getEditingFlattenedSolution()` to
  pick up unsaved editor state) instead of raw `IFile` existence checks. This is exactly the
  pattern used by `TestTarget`, `JSUnitTestRunnerUI`, `DesignerFilter`, `DeveloperBridge`, and
  others.
- One extra consideration beyond the ticket's literal ask: once you have a `Form` object from
  `getForm()`, you also get access to the typed persist model (`Form.getFormElements()` /
  iterating `IFormElement`s) instead of needing the file's raw text at all. The current
  `parseFrmFile` regex-based parsing of the `.frm` JSON (for `dataSource`, element names,
  `typeName`, `typeid`, etc.) becomes unnecessary once you resolve a real `Form` — you can read
  `form.getDataSource()` and iterate its elements directly via the persist API, which is more
  robust than string/regex scraping of serialized JSON. This is a natural extension of the fix,
  not strictly required to solve the reported bug, but strongly recommended since the ticket's
  own proposed direction (go through the model, not the raw file) applies with equal force to
  the parsing step that immediately follows the lookup.

## Approaches considered

1. **Resolve the form via `FlattenedSolution.getForm(formName)` and read persist data directly from the `Form` object** (no `.frm` file I/O at all) — Pros: fully module-aware (any form in the active solution or any of its modules resolves correctly), eliminates the fragile regex `.frm` parsing in `parseFrmFile`, matches the established codebase pattern, more robust against future `.frm` serialization changes. Cons: requires reworking `parseFrmFile`'s regex-based `FormMetadata` extraction into persist-API-based extraction (moderate-size change, but self-contained to one class); need to decide which `FlattenedSolution` to use (`getFlattenedSolution()` for saved state vs `getEditingFlattenedSolution()` for in-editor unsaved state — likely the latter, to match other MCP/test tooling that should see current editor state).
2. **Minimal fix: resolve the form via `FlattenedSolution.getForm(formName)` just to locate which project/module the form lives in, then build the `IFile` path into that module's project and keep the existing regex-based `parseFrmFile` as-is** — Pros: smaller, more surgical change; keeps blast radius limited to the lookup. Cons: leaves the fragile regex-based `.frm` parsing in place, which is already a latent correctness risk (e.g. nested JSON objects, strings containing escaped quotes, components whose `"name"` appears before `"typeid"` in a different element) and misses the chance to clean it up while touching this exact method.
3. **No code change** — Pros: zero risk, zero effort. Cons: does not fix the reported bug at all; `generateFormSpec`/`testForm` remain unusable for any form that isn't in the active project's own `forms/` folder, which is a Major-priority, actively-targeted (2026.9.0) defect with a clear, confirmed root cause and an established fix pattern elsewhere in the codebase. Not viable.

## Recommendation

**PROCEED** with Approach 1: resolve the form through `FlattenedSolution.getForm(formName)`
(using `activeProject.getEditingFlattenedSolution()` for parity with other MCP/dev-tooling that
should reflect current unsaved editor state, falling back to `getFlattenedSolution()` if that
convention doesn't hold here — confirm during implementation by checking what sibling MCP
services in `com.servoy.eclipse.cypress` already use for other active-project lookups), then
read `dataSource` and element metadata directly from the returned `Form`/`IFormElement`
persist objects instead of re-reading and regex-parsing the `.frm` file.

Approach 2 (minimal `IFile`-path-only fix, keep regex parsing) is a reasonable fallback if the
persist-API rework is judged too large for this ticket's scope, but it leaves a known-fragile
parsing mechanism in place right after fixing the lookup that feeds it, which is an odd
half-measure. Recommend Approach 1 unless time pressure dictates otherwise.

## Git history findings

- `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecGenerator.java` has two commits total:
  - `f51f46bf4e` SVY-21296 "extract Cypress testing into standalone plugin [ai]" — introduced the file (and the raw IFile-based form lookup) when the Cypress tooling was pulled into its own plugin.
  - `9149bbb685` SVY-21323 "shutdown formpreview clients after tests and fix form name collisions [ai]" — unrelated follow-up (client shutdown + name-collision handling), did not touch the lookup/parsing logic.
- No dependency/target-platform changes are implicated; this is a design gap present since the method was first written, not a regression from an upgrade.
- Established, repeatedly-used fix pattern across the codebase (`TestTarget`, `JSUnitTestRunnerUI`, `DesignerFilter`, `DeveloperBridge`, `HeadlessFormTemplateContent`, `ServoyBuilderUtils`) is `FlattenedSolution.getForm(String formName)`, confirming the reporter's suggested direction is consistent with how the rest of the IDE resolves forms by name across modules.
