# Triage Report — SVY-21516

**Verdict:** PROCEED

## Reported problem

`FormSpecGenerator.generateCypressSpecContent()` emits a malformed CSS attribute selector in the `beforeEach` wait gate. The generated `cy.get('[data-cy^="<form>."', ...)` is missing the closing `]`, so `querySelectorAll` throws a `SyntaxError` and the entire Cypress spec crashes before any assertion runs. This affects every form that has named elements (the `data-cy^=` branch on line 252).

## Root-cause assessment

The bug is a **single-character typo** on line 253 of `FormSpecGenerator.java`:

```java
// Line 252-253 — CURRENT (broken)
sb.append("    cy.get('[data-cy^=\"").append(metadata.formName)
        .append(".\"', { timeout: 30000 }).should('exist');\n");
```

The template fragment `."'` closes the attribute value quote and the JS string, but never appends the `]` to close the CSS attribute selector. The emitted output is:

```javascript
cy.get('[data-cy^="myForm."', { timeout: 30000 }).should('exist');
//      ^--- opens [         ^--- no closing ]
```

Compare with the correct pattern used on lines 264-265 (per-element selectors):

```java
sb.append("    cy.get('[data-cy=\"").append(metadata.formName).append(".").append(elem.name)
        .append("\"]').should('be.visible');\n");
//              ^^--- correctly has ]
```

The fix is to change `.append(".\"', ...)` to `.append(".\"]', ...)`.

**Introduced in:** commit `f51f46bf4e` by lvostinar on 2026-08-14 ("SVY-21296 extract Cypress testing into standalone plugin [ai]"). The typo was present from the initial extraction — this code was written fresh for the new `com.servoy.eclipse.cypress` plugin.

## Ticket premise check

The ticket's analysis is **entirely correct**:
1. It correctly identifies the missing `]` as the root cause.
2. It correctly identifies line 252-253 as the location.
3. It correctly identifies the fix: change `."'` to `."]'`.
4. It correctly notes that existing tests don't exercise the `beforeEach` selector — `FormSpecGeneratorTest.testFormSpecGenerator_generateCypressSpec_usesCyGet` only checks the per-element selector (line 264), not the `beforeEach` wait selector (line 252).

## Approaches considered

1. **Fix the typo + add a test** — Add the missing `]` on line 253. Add a test in `FormSpecGeneratorTest` that invokes `generateCypressSpecContent` with a metadata object that has named elements and asserts the `beforeEach` wait selector contains the correctly-formed `[data-cy^="<form>."]` pattern.
   - *Pros:* Fixes the bug, prevents regression, minimal change.
   - *Cons:* None.

2. **Fix the typo only (no test)** — Just add the `]`.
   - *Pros:* One-character fix.
   - *Cons:* The test gap that allowed this to ship remains open. Another template edit could re-break it.

3. **No code change** — Leave as-is.
   - *Pros:* None.
   - *Cons:* Every generated form spec with named elements crashes at setup. High impact.

## Recommendation

**Approach 1: Fix the typo and add a test.** The fix is a single character (`]`) on line 253. The test should call `generateCypressSpecContent` via reflection (matching the existing test pattern in `FormSpecGeneratorTest`) with a metadata object that has at least one named element, and assert the output contains `[data-cy^="<formName>."]` — the correctly-closed attribute selector. This is straightforward and directly addresses both the bug and the test gap the ticket identifies.

## Git history findings

- **Introducing commit:** `f51f46bf4e` (lvostinar, 2026-08-14) — "SVY-21296 extract Cypress testing into standalone plugin [ai]". The `generateCypressSpecContent` method was written fresh in this commit as part of the new `com.servoy.eclipse.cypress` plugin. The typo has been present since the method's creation.
- **No prior version of this code exists** — SVY-21296 was a greenfield extraction. There is no earlier correct version that was regressed; the template was simply written with a missing `]` from the start.
- The correct `\"]` pattern is used consistently in every other `cy.get('[data-cy=...')` call in the same method (lines 264, 275), confirming this is an oversight rather than an intentional pattern.
