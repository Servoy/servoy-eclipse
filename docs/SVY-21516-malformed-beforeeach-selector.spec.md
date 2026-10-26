# Spec: SVY-21516 — MCP: generateFormSpec emits a malformed beforeEach selector (missing `]` in `[data-cy^=...]` wait)

## 1. Goal

Fix the malformed CSS attribute selector in `FormSpecGenerator.generateCypressSpecContent()` that causes every generated Cypress form spec with named elements to crash at setup. The `beforeEach` wait gate emits `cy.get('[data-cy^="<form>."', ...)` — missing the closing `]` — so `querySelectorAll` throws a `SyntaxError` before any assertion runs. Add a regression test that exercises the generator's real `beforeEach` output to prevent this from recurring.

## 2. Background

### 2.1 FormSpecGenerator and the Cypress plugin

The `com.servoy.eclipse.cypress` plugin (extracted in SVY-21296) provides MCP tooling that generates Cypress test specs for Servoy forms. `FormSpecGenerator.generateCypressSpecContent(FormMetadata)` is the private method that builds the `.spec.cy.js` file content, including a `beforeEach` block that waits for the form to render before assertions run.

### 2.2 The two beforeEach branches

The `beforeEach` block has two branches depending on whether the form has named elements:

- **No elements:** uses `.svy-form` class selector (line 250) — this works correctly.
- **Has elements:** uses a `[data-cy^="<formName>."]` prefix attribute selector (lines 252-253) — this is broken.

### 2.3 The bug

On line 253 the template fragment `.append(".\"', { timeout:")` closes the attribute value quote (`"`) and the JS string quote (`'`) but omits the `]` that closes the CSS attribute selector bracket opened by `[data-cy^=`. The emitted JavaScript is:

```javascript
cy.get('[data-cy^="myForm."', { timeout: 30000 }).should('exist');
//      ^--- opens [         ^--- no closing ]
```

Cypress passes this to `querySelectorAll`, which throws a `SyntaxError`, aborting the entire spec before any `it()` block runs.

The correct per-element selectors on lines 264-265 use `\"]'` (with the `]`), confirming this is a typo rather than an intentional pattern.

### 2.4 Test gap

The existing test `testFormSpecGenerator_generateCypressSpec_usesCyGet` (line 231 of `FormSpecGeneratorTest.java`) only asserts the per-element `cy.get('[data-cy="myForm.btn1"]')` selector from the `it()` block (line 264 output). No test exercises the `beforeEach` wait selector (line 252 output), so the malformed bracket was never caught.

### 2.5 Git history

- **Introducing commit:** `f51f46bf4e` (lvostinar, 2026-08-14) — "SVY-21296 extract Cypress testing into standalone plugin [ai]". The `generateCypressSpecContent` method was written fresh in this commit. The typo has been present since the method's creation — there is no earlier correct version.
- The correct `\"]` pattern is used consistently in every other `cy.get('[data-cy=...')` call in the same method (lines 264, 275), confirming this is an oversight.

## 3. Design

### 3.1 Fix the typo (one character)

In `FormSpecGenerator.java` line 253, change the template fragment from:

```java
.append(".\"', { timeout: 30000 }).should('exist');\n");
```

to:

```java
.append(".\"]', { timeout: 30000 }).should('exist');\n");
```

This inserts the missing `]` before the closing `'`, producing the correct output:

```javascript
cy.get('[data-cy^="myForm."]', { timeout: 30000 }).should('exist');
```

### 3.2 Regression test

Add a new test method in `FormSpecGeneratorTest.java` that:

1. Builds a `FormMetadata` with at least one named element (using the existing `buildMetadata` reflection helper).
2. Invokes `generateCypressSpecContent` via reflection (using the existing `getGenerateCypressSpecContentMethod` helper).
3. Asserts the output contains the correctly-closed `beforeEach` wait selector: `[data-cy^="<formName>."]` — specifically the `"]'` closing sequence.

This directly closes the test gap identified in the ticket: the `beforeEach` selector is now exercised against the generator's real output, not a hand-constructed expected string.

## 4. Implementation plan

1. **`com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecGenerator.java`** — line 253: change `.append(".\"', {` to `.append(".\"]', {` (insert `]`).

2. **`com.servoy.eclipse.cypress.tests/src/test/java/com/servoy/eclipse/cypress/services/FormSpecGeneratorTest.java`** — add a new test method `testFormSpecGenerator_generateCypressSpec_beforeEachWaitSelectorIsClosed`:
   - Build metadata via `buildMetadata("myForm", null, new Object[][] { { "elem1", null, null, false, false, true } })`.
   - Invoke `generateCypressSpecContent` via `getGenerateCypressSpecContentMethod()`.
   - Assert the output contains `cy.get('[data-cy^="myForm."]'` — the correctly-closed attribute selector in the `beforeEach` block.
   - Optionally also assert `!spec.contains("[data-cy^=\"myForm.\"'")` (the malformed pattern without `]`) as a negative guard.

3. **Verify:** check compilation errors and run the test class.

## 5. Acceptance criteria

- [ ] `FormSpecGenerator.generateCypressSpecContent()` emits `[data-cy^="<formName>."]` (with closing `]`) in the `beforeEach` wait selector when the form has named elements.
- [ ] The generated `beforeEach` selector is a valid CSS attribute selector (no `SyntaxError` from `querySelectorAll`).
- [ ] A new test in `FormSpecGeneratorTest` invokes `generateCypressSpecContent` with a metadata object containing named elements and asserts the `beforeEach` output contains the correctly-closed `[data-cy^="<formName>."]` pattern.
- [ ] The new test fails on the pre-fix code (missing `]`) and passes on the fixed code.
- [ ] All existing `FormSpecGeneratorTest` tests continue to pass.
- [ ] No compilation errors in `com.servoy.eclipse.cypress` or `com.servoy.eclipse.cypress.tests`.

## 6. Out of scope

- The `.svy-form` fallback branch (no named elements) — already correct.
- The per-element `cy.get('[data-cy="..."]')` selectors — already correct and tested.
- The Cypress runner incorrectly reporting crashed specs as "passing" (mentioned in the ticket as a separate issue).
- Any other `FormSpecGenerator` template improvements beyond this specific selector fix.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| None — the fix and test are fully specified. | — | — |
