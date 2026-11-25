# Review Summary: SVY-21514 MCP generateFormSpec/testForm doesn't work with multiple solutions

**Risk (initial review): MODERATE.** The generator wrote module-form specs as `<ownerSolution>.<form>`, but `FormSpecRunner` only looked for `<activeSolution>.<form>`, so `testForm` on a module-only form most likely still failed.

**Scope reviewed:** servoy-eclipse, `ec45a72fbc` (fix, master/release) and `7021709544` (test fix). The same changes are on lts_2026 as `ab91199435` and `6aede848df`.

## Outcome of the review findings

All follow-up commits are on lts_2026 and merged up to release and master.

| # | Finding | Result | Commit |
|---|---|---|---|
| 1 | [High] `testForm` on a module-only form: the runner never found `<owner>.<form>` | Confirmed and fixed. New `FormSpecGenerator.resolveOwnerSolutionName(formName)`. The runner tries `<owner>.<form>` first, then the active solution name, then the bare name. | `cef6e39907` |
| 2 | [Medium] Hidden elements skipped in `buildMetadata` | Changed. Hidden elements are kept with a hidden flag and asserted `should('not.exist')`. They are no longer asserted visible or listed as clickable. An all-hidden form waits for `.svy-form`. Handlers on hidden elements stay `it.skip`. | `595079d3f0` |
| 3 | [Medium] `dataProviderID` commented out in `buildMetadata` | Dismissed. Nothing in the cypress plugin reads it. | none |
| 4 | [Low] Inherited elements (`extendsID`) missing | Confirmed and fixed. The metadata is built from `getFlattenedForm(form)`, with a fallback to the raw form. | `fc801bd8e9` |
| 4b | Duplicate form names across modules | Not a problem, Servoy forbids them. | none |
| 5 | Generate specs for ALL forms including sub-modules | Decided not to do it. The scope stays the form being worked on. Existing module-form specs run in the all-forms runner because their `<owner>.<form>` name matches the file name. | none |

## Verification status

- Plain JUnit (`com.servoy.eclipse.cypress.tests`, junitVersion 6): 665 of 665 pass on master.
- New PDE test `FormSpecGeneratorInheritanceIntegrationTest`: 2 of 2 pass with the fix. The child test fails without it.
- The runner path of fix 1 has no automated test. Only the new `resolveOwnerSolutionName` has a plain test.
- **Not verified at runtime:**
  - the `testForm` run on a real multi-module solution;
  - that hidden elements are absent from the DOM for every component type (containers, legacy components);
  - that the `data-cy` selectors of inherited elements match what the preview renders;
  - the preview URL with the owner solution name for module forms. Code reading suggests it is harmless: `IndexPageFilter` only uses the name from the URI for the index page, and `FormPreviewNGClient` resolves the form via the active flattened solution.

## Release target

Jira Fix Version 2026.9.0 (released). The original fix `ec45a72fbc` is in tag `2026.9.0`. The three follow-up commits above were made on 2026-11-25 and are in no release tag yet. They reach the next release from lts_2026, release and master. Confirm that the Fix Version, or a new case or version for the follow-ups, covers them.

## Manual test plan

Setup: solution `Main` with module `Mod`. Form `F` exists only in `Mod`. In `Mod`, form `A` has elements a1, a2, a3. Form `C` (in `Main` or `Mod`) extends `A`, has its own c1, and overrides a2. Form `H` has one visible element and one element with visible=false.

1. `generateFormSpec({formName:"F"})`: expect no "Form file not found" and a file `Mod.F.spec.cy.js` (plus `Mod.F.spec.js` setup) in `cypress/cy-form`.
2. `testForm({formName:"F"})` right after: expect the Cypress run to start and the form to load. "Spec file not found" means finding 1 is not fixed.
3. `generateFormSpec` on form `C`: the spec has `data-cy` assertions for c1, a1, a2 and a3, with a2 exactly once.
4. `generateFormSpec` on form `H`: the load test has `should('be.visible')` for the visible element and `should('not.exist')` for the hidden one. Run `testForm` and check it passes.
5. A form whose elements are all hidden: the spec waits for `.svy-form` and the run passes.
6. Regression: a form in the active solution gives the same spec name as before (`<Main>.<form>`). An existing spec gives the "already exist" message. An unknown form gives "Error: Form not found: X".
7. Run all form tests from the IDE: the module-form spec is found and run under its `<owner>.<form>` name.

## Follow-ups / open points

- Open point: hidden elements are asserted `should('not.exist')`. Confirm on component types such as containers and legacy components that hidden elements are really not rendered.
- Open point: add an automated test for the `FormSpecRunner` lookup order (owner, active solution, bare name).
- Optional: generating specs for all forms including sub-modules was deliberately left out.
