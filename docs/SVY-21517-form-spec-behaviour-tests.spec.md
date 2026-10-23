# Spec: SVY-21517 — MCP: generateFormSpec should generate real behaviour tests, not just visibility checks

Companion documents: `docs/SVY-21517-triage.md` (authoritative for scope and approach), and Servoy-Copilot `docs/SVY-21573-bug-triage-form-lookup.spec.md` (constraints on the element service).

> **Status (after implementation and a first run).** All three steps are implemented. This spec was updated to match what was built: the work lands on `lts_2026` (3.2); two defects found by running the generated specs are fixed and described in 2.7 (the `.frm` parser could not read Servoy's unquoted keys; hidden elements broke the generated spec); the model has a `visible` flag (3.3, 3.4, 3.7); the tests of section 5 exist and pass; section 6 shows which acceptance criteria are verified; open questions are updated in section 8. Not verified: the recognised on-form assertions in a real browser, the Tycho reactor build and Copilot's CI.

## 1. Goal

`generateFormSpec` (and the auto-generate paths of `showFormInBrowser`, `testForm`, `showAndTest`) today emits a Cypress spec that only proves the form renders. It never clicks anything. This spec makes the generator emit one deterministic case per bound handler: perform the handler's trigger on the element's `data-cy` selector, then assert the effects the handler has **on the form** (visibility, enabled state, style class, text), then assert that no error overlay appeared. Effects that cannot be asserted in phase 1 (off-form calls, anything unrecognised, branches, events without a known gesture) are emitted as labelled comments or skipped cases, never as guessed assertions. The same form must always generate byte-identical output.

The work is delivered in three independently buildable and testable steps across three repos: a neutral model plus emitter in servoy-eclipse (Step 1), a builder and wiring in Servoy-Copilot (Step 2), and a documentation update in skill4servoy (Step 3).

## 2. Background

### 2.1 Current behaviour (verified in source)

`com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecGenerator.java`. Line numbers differ per branch, both are given.

| Item | `lts_2026` (current working tree) | `master` |
|------|-----------------------------------|----------|
| `generateSpec(String formName)` | 32 | 32 |
| early return "Spec files already exist" | 58-62 | about 52-58 |
| `parseFrmFile(String, String)` (regex splitter, private) | 264 | 201 |
| `generateCypressSpecContent(FormMetadata)` (private) | 303 | 236 |
| `.limit(8)` (visible elements) / `.limit(3)` (buttons) | 328 / 338 | 257 / 266 |
| `getFormUrl` | 356 | 282 |
| `generateSetupContent(FormMetadata)` | 361 | 286 |
| `FormMetadata` / `ElementInfo` | 407 / 415 | 327 / 334 |

- `generateCypressSpecContent` emits only `cy.visit`, a `[data-cy^="<form>."]` existence wait, an error-overlay check, `be.visible` for at most 8 elements and `be.visible`/`be.enabled` for at most 3 buttons.
- `parseFrmFile` keeps `name`, `typeName`, `dataSource` and a few `typeid` booleans. It never reads a handler binding. [UPDATED] It matched only quoted keys (`"dataSource":"..."`), while Servoy writes unquoted keys (`dataSource:"..."`); fixed, see 2.7.
- `generateSpec` never overwrites: when both the `.spec.cy.js` and the `.spec.js` exist it returns `Spec files already exist: ...`. An improved generator therefore only affects forms that have no spec yet.
- `generateSetupContent` writes `UUID.randomUUID()` into the `.spec.js` (not into the `.spec.cy.js`). That file is out of scope here.
- `FormSpecGenerator` is `@Creatable` and instantiated directly with `new` by `ServoyTestingServer.java:52`, `PersistRenameService.java:54` (Copilot) and `HeadlessFormTestExecutor.java:93` (this repo, calls `generateSpec(formName)`).

### 2.2 Dependency direction (why a neutral model)

- Servoy-Copilot `com.servoy.eclipse.developer.mcp` has `com.servoy.eclipse.cypress` in `Require-Bundle` (MANIFEST line 16). Cypress does not require the Copilot bundle and exports `actions`, `headless` and `services`.
- `FormInteractiveElementsService` (Copilot) therefore cannot be called from the generator. Information flows Copilot -> cypress through a plain data type that lives in an exported cypress package: `FormSpecModel`.
- servoy-eclipse callers (headless executor, context-menu actions, cypress tests) have no Copilot at runtime, so the existing regex path stays as the fallback and `generateSpec(String)` keeps its exact behaviour.

### 2.3 What SVY-21573 already delivers (Servoy-Copilot `770bbc7`, ancestor of Copilot `master` HEAD `93d0085`)

- `FormInteractiveElementsService.collect(Form, FlattenedSolution, boolean includeStatic)` walks the flattened form (inherited elements, layout containers, portals) and returns `List<InteractiveElement>`; each has `name`, `type`, `kind` (`control|input|container|static`), `dataProvider`, `container`, `inherited`, `specMissing` and `List<HandlerBinding>` (`FormInteractiveElementsService.java:122-143`).
- Handlers are sorted by key (web components via `new TreeSet<>(handlerNames)` at line 282; legacy via `new TreeMap<>(properties)` at line 323) and resolved to `ScriptMethod` in `Collector.resolveHandler` (lines 352-368). The resolved `ScriptMethod` is in hand there, but only its name and owner are kept.
- `HandlerBinding` is a record `(handler, method, owner, unresolved, methodId)` with factories `resolved(handler, method, owner)` and `unresolved(handler, methodId)`; production code and tests only use the factories (grep: no `new HandlerBinding(` outside `HandlerBinding.java`).
- `FormInteractiveElementsFormatter` writes the JSON field by field (lines 140-152), so adding a component to `HandlerBinding` does not change the `getFormInteractiveElements` payload.
- `FormInteractiveElementsFormatter.selector(form, element)` produces `[data-cy="<form>.<element>"]` and escapes backslash and double quote.
- `ScriptMethod.getDeclaration()` (`servoy-client/servoy_shared/.../AbstractScriptProvider.java:186`) returns the method's full source including doc comment and `function` header.
- No effect extraction exists anywhere in the Copilot bundle. `FormNavigationGraphService.analyzeScriptFile` (lines 440-534) is the in-repo precedent for regex scanning of `.js` text.
- SVY-21573's own spec lists changing `generateFormSpec`/`FormSpecGenerator` as out of scope and defers "let `generateFormSpec` consume the element service" to a follow-up (Q7). That follow-up is this case.

### 2.4 What skill4servoy `41d866e` (HEAD of skill4servoy `master`) says

`.opencode-shippable/skills/servoy-e2etester/context/tooling.md` lines 57-95 tell the agent to scaffold with `generateFormSpec` and then author click cases and per-effect assertions by hand, and call centralising this in the generator "the open enhancement". Line 77 says "Stub/`cy.intercept` at the form's edge", which contradicts `SKILL.md:105` ("No `cy.intercept()`") and `references/author/author-rules.md:82` and `:85` (Servoy uses websockets, there is no HTTP request to stub).

### 2.5 Ticket premise check (from triage)

- On-form effects can be asserted directly. Off-form stubbing as written in the ticket (`cy.get('@loadStub')`) has no known mechanism: handlers run server-side behind a websocket, and it is unverified that the generated `.spec.js` setUp/tearDown is executed anywhere at runtime. Phase 1 therefore labels off-form effects instead of stubbing them.
- NG-only: a closed allowlist of effects satisfies "never scaffold `bgcolor`/`fgcolor`/`font`/`border`".

### 2.6 Git history

- servoy-eclipse `FormSpecGenerator.java`: `f51f46bf4e` SVY-21296 (extracted into the standalone plugin), `9149bbb685` SVY-21323 (solution-prefixed paths, formpreview shutdown), `2995c88fee` SVY-21284 (tests split, `lts_2026` only). No commit ever added click or behaviour assertions; the visibility-only shape is original. This is a feature gap, not a regression.
- `master` and `lts_2026` differ on this file by 172 insertions / 90 deletions. On `master` the bundle version is `2026.12.0.qualifier`, on `lts_2026` `2026.3.2.qualifier`.
- Servoy-Copilot: `FormInteractiveElementsService`, `HandlerBinding`, `InteractiveElement`, `FormLocatorService` come solely from `770bbc7`; `ServoyTestingServer` is the only caller of `generateFormSpec`.
- Not a dependency or target-platform change.

### 2.7 Defects found by running the generated specs

Both were invisible when reading the source and showed up when the specs of the six forms of `test_webcomponents2` were generated and run in Cypress.

1. **`parseFrmFile` could not read unquoted `.frm` keys.** The patterns required `"dataSource"`, `"name"`, `"typeName"` and `"typeid":47` with quotes. Servoy writes `dataSource:"db:/servoy_test/orders"`, `name:"btn_next"`, `typeid:47`; 5 of the 6 forms are unquoted. Effects: the setup file said "No dataSource on this form", and the legacy path found no elements and no web-component, button or label flags (so it generated a spec that checks nothing). The defect is older than this case; the model path was not affected for elements because it does not use this parser for them. Fixed in `FormSpecGenerator` (3.5) and covered by `FormSpecGeneratorFrmFormatTest` (5.1).
2. **A hidden element broke the generated spec.** f3_multifile_upload has `button_2` with `visible:false`. NG does not render such an element, so `be.visible` in the baseline and the `.click()` of its handler failed with "Expected to find element ... but never found it". Fixed with a `visible` flag in the model (3.3, 3.4, 3.7), tests in 5.1 and 5.2, and Q6.

## 3. Design

### 3.1 Landing order and independence

| Step | Repo | Location on disk | Landing | Build/test on its own |
|------|------|------------------|---------|-----------------------|
| 1 | servoy-eclipse (THIS repo, implementable now) | `D:\Eclipse\202603Workspace\servoy-eclipse` | first | Yes. No caller passes a model yet, so behaviour is unchanged. Plain tests only. |
| 2 | Servoy-Copilot (sibling, reachable on disk, branch `master`) | `D:\Eclipse\202603Workspace\Servoy-Copilot` | after Step 1 is available to Copilot's build | Yes once Step 1 is resolvable. Falls back to the legacy path on any failure. |
| 3 | skill4servoy (sibling, reachable on disk, branch `master`) | `D:\Eclipse\202603Workspace\skill4servoy` | after Steps 1 and 2 | Docs only. |

Step 2 cannot compile until the Copilot build resolves a `com.servoy.eclipse.cypress` that contains `FormSpecModel`. How Copilot's Tycho build consumes the servoy-eclipse bundle (p2 site vs. workspace) was not verified. If Copilot's CI cannot see Step 1 yet, Step 2 is a hand-off until Step 1 is published.

Each step is a separate commit series per repo. Per AGENTS.md, ask before every commit; subjects contain `SVY-21517` and end with ` [ai]`.

### 3.2 Branch precondition and decision

**Decision (user): the work lands on `lts_2026` in servoy-eclipse.** The triage had suggested `master`, but Servoy-Copilot is built against servoy-eclipse `lts_2026`: Copilot's `RenamePersistIntegrationTest.java:371` calls `FormSpecGenerator.getOldSpecFilePath`, which exists only on `lts_2026`. Step 1 must therefore be on `lts_2026` for Step 2 to compile. The change then travels upward by the normal merge chain `lts_2026` -> `release` -> `master`; no separate backport or forward-port is done by this case.

- The servoy-eclipse working tree is on `lts_2026` and is dirty: modified `com.servoy.eclipse.core.tests/testresources/servoy.properties` and `com.servoy.eclipse.core/launch_files/Servoy Launch Windows.launch`, plus the untracked `docs/SVY-21517-*.md` files. The two modified files are unrelated to this case and must not be staged.
- Line numbers in 2.1 for `lts_2026` were re-checked against the branch and match.
- Upward-merge risk (not verified by an actual merge): `master`'s `FormSpecGenerator.java` differs from `lts_2026` by 172 insertions / 90 deletions, including a different brace style and different deprecated-method set (`getOldSpecFilePath` and the `old*` methods are absent on `master`). Keep the Step 1 edits to `FormSpecGenerator.java` small and additive (new overload, delegation, one switch line) and put the bulk in the new files `FormSpecModel.java` and `FormSpecModelEmitter.java`, so the merge into `master` conflicts as little as possible. Use the formatter settings of the branch you are on.
- Servoy-Copilot has only `master` (and one old remote branch) and skill4servoy is on `master`; both are used as they are.

### 3.3 Neutral model (`FormSpecModel`, servoy-eclipse)

New file `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecModel.java` in the already exported `com.servoy.eclipse.cypress.services` package. No MANIFEST change. Plain Java 21 records and enums, no Servoy/Eclipse types, immutable (lists copied with `List.copyOf`, null lists become empty).

```java
public record FormSpecModel(String formName, List<Element> elements)
{
	public enum Kind { CONTROL, INPUT, CONTAINER, STATIC }

	public enum Gesture { CLICK, RIGHT_CLICK, CHANGE, NONE }

	public enum EffectKind { VISIBLE, ENABLED, TEXT, ADD_STYLE_CLASS, REMOVE_STYLE_CLASS, OFF_FORM }

	/** target = element name (null for OFF_FORM); value = "true"/"false", the text literal, the class name,
	    or for OFF_FORM the single-line source statement. */
	public record Effect(EffectKind kind, String target, String value) {}

	public record Handler(String key, String method, String owner, Gesture gesture,
		boolean unresolved, boolean branching, List<Effect> effects) {}

	public record Element(String name, String type, Kind kind, boolean hasDataProvider, boolean visible, List<Handler> handlers) {
		public Element(String name, String type, Kind kind, boolean hasDataProvider, List<Handler> handlers) { this(name, type, kind, hasDataProvider, true, handlers); }
	}
}
```

Names are free; the shape is the contract. `visible` is false when the element, or any container around it, has `visible=false` in the form; the five-argument constructor keeps callers that do not know about it working (visible). The model deliberately has no way to express `bgcolor`/`fgcolor`/`font`/`border`: the allowed effects are the `EffectKind` enum, so the NG-only rule is enforced by the type.

`Gesture.forHandler(String handlerKey, String elementType, boolean hasDataProvider)` is the single, shared gesture table (so both repos use one definition and Step 1 can test it alone):

| Handler key (exact match) | Element condition | Gesture |
|---------------------------|-------------------|---------|
| `onActionMethodID`, `onAction` | type neither in the text-entry set nor the legacy `Field` | `CLICK` |
| `onActionMethodID`, `onAction` | type in the text-entry set, or the legacy persist type `Field` (a text field fires on Enter, not on click) | `NONE` (user decision Q7) |
| `onRightClickMethodID`, `onRightClick` | any | `RIGHT_CLICK` |
| `onDataChangeMethodID`, `onDataChange` | `hasDataProvider` and type in the text-entry set | `CHANGE` |
| anything else (including `onDataChange*` on other types and on the legacy `Field`, `onCellClick`, `onColumnDataChange`, `onFocus*`, `onRender`, null) | any | `NONE` |

Text-entry set (closed, additive later): `bootstrapcomponents-textbox`, `bootstrapcomponents-textarea`, `servoydefault-textfield`, `servoydefault-textarea`. Reason: `clear().type().blur()` on a checkbox, combobox or calendar fails, and a generated case that fails for a wrong gesture is worse than a skipped one. All four names were read from the `name` field of their `.spec` files (`bootstrapcomponents/components/textbox|textarea`, `servoy_ngclient/war/servoydefault/textfield|textarea`); see Q5.

Legacy persists report their Java simple name as `type` (`Field`, `GraphicalComponent`, ...). A legacy `Field` can be a text field, combobox, calendar, checkbox, ... depending on its display type, and the model carries no display type. So `Field` is added to the `onAction*` => `NONE` rule only (the text-field case fires on Enter, and a click is not known to be right for the other display types). It is deliberately not in the set for `onDataChange*` => `CHANGE`: `clear().type().blur()` would fail on a combobox, calendar or checkbox, and a wrongly gestured case is worse than a skipped one.

Exact-match keys are used instead of prefix matching so that `onActionMethodID` cannot be confused with other keys. The bootstrap and servoydefault specs all name their handlers `onActionMethodID`, `onDataChangeMethodID` and `onRightClickMethodID` (verified in the `.spec` files); the bare names stay in the table as harmless extras.

### 3.4 Emitter (`FormSpecModelEmitter`, servoy-eclipse)

New package-private, stateless class `FormSpecModelEmitter` in `com.servoy.eclipse.cypress.services` with `static String emit(String formUrl, FormSpecModel model)`. It is pure (no workspace, no files), so it is tested by plain JUnit without reflection. `com.servoy.eclipse.cypress.tests` is a fragment of the cypress bundle (`Fragment-Host: com.servoy.eclipse.cypress`), so package-private access works from the tests in the same package.

Determinism rules:
- Elements sorted by `name` with `String.compareTo`; handlers of an element sorted by `key` with `String.compareTo`; effects keep the order given in the model (source order). The emitter sorts regardless of input order.
- No caps (`.limit(8)` and `.limit(3)` are not used in the model path), no timestamps, no random ids.
- All dynamic text placed in a JS single-quoted string escapes `\`, `'`, `\r`, `\n`. Selectors follow `FormInteractiveElementsFormatter.selector` (escape `\` and `"`) and then the JS escaping. Comment text is collapsed to one line (`\s+` -> single space, trimmed, trailing `;` removed, truncated to 160 chars with `...`), so no comment can break onto a second line.

Output structure for a model with at least one element (2-space indent as in the legacy emitter):

```js
describe('<form>', () => {

  beforeEach(() => {
    cy.visit('<formUrl>');
    cy.get('[data-cy^="<form>."]', { timeout: 30000 }).should('exist');
  });

  it('loads without errors and all elements are visible', () => {
    cy.get('.svy-error, .error-overlay').should('not.exist');
    cy.get('[data-cy="<form>.<el>"]').should('be.visible');      // one per CONTROL/INPUT element
  });

  // <el>.<key> -> <owner>.<method>
  it('<el>: <key> (<method>)', () => {
    cy.get('[data-cy="<form>.<el>"]').click();                    // gesture line, see below
    cy.get('[data-cy="<form>.<target>"]').should('not.be.visible'); // one per recognised effect
    // OFF-FORM (not asserted): scopes.orders.load(id)            // one per unrecognised/off-form effect
    cy.get('.svy-error, .error-overlay').should('not.exist');
  });

});
```

With zero elements the `beforeEach` waits on `.svy-form` exactly like the legacy emitter, and the baseline `it` contains only the error-overlay check.

Baseline visibility: asserts `be.visible` for elements of kind `CONTROL` and `INPUT` only. `STATIC` and `CONTAINER` elements are present in the model as possible effect targets but are not asserted in the baseline. The legacy `it('buttons are clickable')` is not emitted in the model path: a click case on a disabled or hidden button fails Cypress actionability, which covers the same ground. An element with `visible == false` is left out of the baseline as well (and out of the visibility count), because NG does not render it at all (Q6). It stays in the model so that a visible button can still assert `be.visible` on it after the handler showed it.

Gesture lines (all on `cy.get('[data-cy="<form>.<el>"]')`):

| Gesture | Emitted |
|---------|---------|
| `CLICK` | `.click();` |
| `RIGHT_CLICK` | `.rightclick();` |
| `CHANGE` | `.clear().type('1').blur();` (a type-aware value is deferred, Q4) |
| `NONE` | no case body, a skipped case: `it.skip('<el>: <key> (no gesture defined)', () => {});` |
| `unresolved == true` (any gesture) | `it.skip('<el>: <key> (handler method could not be resolved)', () => {});` |
| element `visible == false` (any gesture, resolved or not) | `it.skip('<el>: <key> (element is hidden by design)', () => {});`. Checked first, so it wins over "no gesture" and "unresolved". |

Effect lines, each on `cy.get('[data-cy="<form>.<target>"]')`:

| `EffectKind` | value | Emitted |
|--------------|-------|---------|
| `VISIBLE` | `true` | `.should('be.visible');` |
| `VISIBLE` | `false` | `.should('not.be.visible');` |
| `ENABLED` | `true` | `.should('be.enabled');` |
| `ENABLED` | `false` | `.should('be.disabled');` |
| `ADD_STYLE_CLASS` | class | `.should('have.class', '<class>');` |
| `REMOVE_STYLE_CLASS` | class | `.should('not.have.class', '<class>');` |
| `TEXT` | literal | `.should('contain.text', '<literal>');` |
| `TEXT` | null, empty or blank | line comment `// OFF-FORM (not asserted): elements.<target>.text = <empty>` (no `cy.get`; `contain.text` with an empty string can never fail) |
| `OFF_FORM` | source | line comment `// OFF-FORM (not asserted): <source>` (no `cy.get`) |

A handler with `branching == true` gets the gesture line, then the single comment `// BRANCHING: one case per branch needed (<owner>.<method>)`, then the final error-overlay assertion. Its `effects` are ignored by the emitter (no per-branch assertions, and no unconditional-looking assertions for code that may be conditional).

The final no-error-overlay assertion is always the last line of every non-skipped handler case.

### 3.5 `FormSpecGenerator` changes (servoy-eclipse)

- Add `public String generateSpec(String formName, FormSpecModel model)`. `generateSpec(String formName)` stays public and becomes `return generateSpec(formName, null);`.
- First statement of the new method (before any workspace access, so it is plain-testable): if `model != null` and `!formName.equals(model.formName())`, return `"Error: Model is for form '<model>' but spec was requested for form '<formName>'"`. The `Error` prefix matches the callers' `startsWith("Error")` handling.
- Everything else is unchanged: active project lookup, `forms/<formName>.frm` existence check, directory creation, the "Spec files already exist" early return, `parseFrmFile` (still used for `dataSource` and for `generateSetupContent`), `.spec.js` generation.
- Where the `.spec.cy.js` content is produced: `model == null ? generateCypressSpecContent(metadata) : FormSpecModelEmitter.emit(getFormUrl(metadata.solutionName, metadata.formName), model)`.
- Result message in the model path keeps the prefix `Created: <relative path>` (the existing integration test asserts `Created`) and reports `(<E> element visibility assertions, <H> handler cases)`; the legacy path message is unchanged.
- [UPDATED] Fix in `parseFrmFile`: the four key patterns (`dataSource`, `name`, `typeName`, `typeid`) accept the key with or without quotes, with a lookbehind `(?<![\w$])` so `subname` or `nodataSource` do not match, and the `typeid` flags are derived from the number read by `TYPE_ID_PATTERN` instead of `item.contains("\"typeid\":47")`. This is the only change to the legacy code path (2.7).
- Do not change the signatures or names of `parseFrmFile(String, String)`, `generateCypressSpecContent(FormMetadata)` or `generateSetupContent(FormMetadata)`, and do not add overloads with those names. `FormSpecGeneratorTest` finds them by name through `getDeclaredMethods()` and calls `getDeclaredMethod("parseFrmFile", String.class, String.class)` (lines 78-125, 127-245). The new emitter has its own name for that reason.
- Existing limitation unchanged: the form must have a `forms/<name>.frm` in the active project; forms that live only in a module still return `Error: Form file not found`.

### 3.6 Effect recogniser (`HandlerEffectRecognizer`, Servoy-Copilot)

Pure class (string in, `FormSpecModel` pieces out; no Servoy types) in `com.servoy.eclipse.developer.mcp.services`. Input: the method declaration text (from `ScriptMethod.getDeclaration()`). Output: `analyzable`, `branching`, ordered `List<FormSpecModel.Effect>`.

Algorithm (closed patterns, regex grade, roughly 150-250 LOC):
1. Strip `/* ... */` and `// ...` comments with a small scanner that respects `'`, `"` and backtick strings. If a string or comment is unterminated, or the function body braces do not balance, the result is not analysable.
2. Find the first `function` header and take the text between its matching braces as the body. No match -> not analysable.
3. Branching: true if the stripped body contains any of the tokens `if`, `else`, `switch`, `for`, `while`, `do`, `try`, `catch`, a ternary `?` outside strings, or a `return` that is not the last statement. Tokens are matched on word boundaries outside strings.
4. Split the body into top-level statements on `;` at paren/bracket/brace depth 0 and outside strings. A statement without a trailing `;` merges with the next line and falls into the unrecognised bucket (documented limitation).
5. Classify each statement (whole-statement match, whitespace tolerant):

| Statement | Result |
|-----------|--------|
| `elements.<n>.visible = true\|false` | `VISIBLE` target `<n>` |
| `elements.<n>.enabled = true\|false` | `ENABLED` |
| `elements.<n>.text = '<lit>'` or `"<lit>"`, literal without backslash or quote, non-blank (not empty, not whitespace only) and containing none of `<`, `>`, `&` (markup and entities render differently from the source literal) | `TEXT` |
| `elements.<n>.text = '<lit>'` with an empty, whitespace-only or `<`/`>`/`&` containing literal | `OFF_FORM` with the collapsed source text |
| `elements.<n>.addStyleClass('<c>')` | `ADD_STYLE_CLASS`, `<c>` matches `[A-Za-z0-9_-]+` |
| `elements.<n>.removeStyleClass('<c>')` | `REMOVE_STYLE_CLASS`, same class rule |
| assignment (`=` or compound `+=`) to `elements.<n>.bgcolor\|fgcolor\|font\|border`, also through bracket access `elements['<n>']...` and the qualified form `forms.<f>.elements.<n>...` | dropped entirely (not a comment, not an assertion). The rule is a closed regex that stops at the end of the statement (`;`) or line, so a following statement without a separating `;` is never swallowed |
| a final top-level `return;` or `return <literal>;` (`true`, `false`, `null`, `undefined`, a number or a string literal) | dropped (no effect on the form; `onDataChange` handlers conventionally end with `return true;`). It does not count as branching. A final `return <expression>` (for example `return scopes.x.check(v);`) is still an `OFF_FORM` |
| any other non-empty statement (scope or form function call, DB, HTTP, navigation, expression, variable declaration, anything not matching above) | `OFF_FORM` with the collapsed source text |

6. Target validation is done by the builder, not here: an `elements.<n>` effect whose `<n>` is not a named element of the form is converted to `OFF_FORM` there.
7. Collapse: for the same `(kind-family, target)` only the last statement is kept, where the families are visible, enabled, text, and (add or remove) per class name. Only the final state is observable, so earlier superseded statements are dropped without a comment.
8. `analyzable == false` -> a single `OFF_FORM` effect with value `method body could not be analyzed`.
9. `declaration == null` (should not happen for a resolved method) behaves as not analysable.

### 3.7 Builder (`FormSpecModelBuilder`, Servoy-Copilot)

- `FormSpecModel build(String formName)`: resolves the form like `FormInteractiveElementsService.getElements` does (`ServoyModelManager...getActiveProject()`, `getEditingFlattenedSolution()`, `fs.getForm(name)`), calls `new FormInteractiveElementsService().collect(form, fs, true)` (static included, so labels and named containers can be effect targets) and delegates to the pure `fromElements`. Returns `null` when there is no active solution or the form is not found, so callers fall back to the legacy path.
- `static FormSpecModel fromElements(String formName, List<InteractiveElement> elements)` (pure, plain-testable):
  - drops unnamed elements (cannot be targeted by `data-cy`), drops `specMissing` elements, keeps the first occurrence of a duplicate name;
  - maps `kind` to `FormSpecModel.Kind`, `hasDataProvider = dataProvider() != null`, and passes `visible()` on;
  - per `HandlerBinding`: `gesture = Gesture.forHandler(handler(), type(), hasDataProvider)`, `unresolved` from the binding, `method`/`owner` from the binding; for resolved bindings runs the recogniser over `declaration()` and maps `elements.<n>` effects whose target is not a named element in the form to `OFF_FORM` (value = original statement text);
  - for `NONE` gesture or `unresolved` bindings the recogniser is not run.
- [UPDATED] Visibility: `InteractiveElement` gets a new component `visible` (before `handlers`), with its previous constructor kept (visible). `FormInteractiveElementsService.Collector` sets it to `!isHidden(persist)`: a persist is hidden when it, or any parent up to the form, has `visible=false` (`WebComponent.getProperty("visible")` or its flattened JSON; `AbstractBase.getProperty("visible")` for legacy persists and layout containers; a missing property means visible). `FormInteractiveElementsFormatter` does not write it, so the JSON payload is unchanged.
- Carry the method source: add a sixth record component `declaration` to `HandlerBinding`, keep `resolved(handler, method, owner)` and `unresolved(handler, methodId)` (the latter two set `declaration = null`) and add `resolved(handler, method, owner, declaration)`. `Collector.resolveHandler` (line 367) passes `method.getDeclaration()`. `FormInteractiveElementsFormatter` is not touched, so the `getFormInteractiveElements` JSON stays identical (verify with the existing `FormInteractiveElementsFormatterTest`).

### 3.8 `ServoyTestingServer` wiring (Servoy-Copilot)

File `bundles/com.servoy.eclipse.developer.mcp/src/com/servoy/eclipse/developer/mcp/servers/ServoyTestingServer.java`.
- Add a field `private final FormSpecModelBuilder specModelBuilder = new FormSpecModelBuilder();` next to `specGenerator` (line 52).
- Add `private String generateSpecWithModel(String formName)`: build the model inside `try`, catching `RuntimeException`, logging with `ServoyLog.logError` and using `null`; return `specGenerator.generateSpec(formName, model)`.
- Replace `specGenerator.generateSpec(formName)` at lines 205 (`showFormInBrowser`), 272 (`testForm`), 298 (`showAndTest`) and 420 (`generateFormSpec`).
- Update the `generateFormSpec` tool description (lines 408-414). It currently says "Reads the form's .frm file to extract element names, dataSource, and structure." Replace with a description of the new behaviour (per handler: trigger, on-form assertions, labelled OFF-FORM/BRANCHING markers). It must still contain `.spec.cy.js` and `jenkins-custom/e2e-test-scripts/cypress/cy-form` and must not contain `medias/tests` (asserted by `ServoyTestingServerTest.testGenerateFormSpec_descriptionMentionsSpecCy`, lines 312-323). Keep the `servoy.ngclient.testingMode=true` sentence.
- Fallback rule: if `build` returns `null` or throws, the legacy `.frm` path runs unchanged.

### 3.9 Skill documentation (skill4servoy)

`.opencode-shippable/skills/servoy-e2etester/context/tooling.md`, section "Single-form test (`#formtest`)" (lines 57-95):
- State that the generator now emits, per bound handler, the trigger plus recognised on-form assertions plus a no-error-overlay check, and that the agent completes only the labelled `// OFF-FORM`, `// BRANCHING` and `it.skip` markers, and extends rather than overwrites.
- Resolve the contradiction: line 77 ("Stub/`cy.intercept` at the form's edge") must follow `SKILL.md:105` and `references/author/author-rules.md:82,85`. Off-form effects are not stubbed with `cy.intercept` (websockets). Until a stub mechanism exists they stay as labelled comments; the logic behind them belongs to JSUnit, the full flow to `#e2e`. Also fix the example at lines 80-81 ("assert the load stub was called") and the "open enhancement (below)" wording at lines 82-87 (check what "below" refers to).
- Lines 59-61 describe the generated file as "render + element-visible baseline"; update.
- The file contains non-ASCII characters (dashes, arrows); edit it UTF-8 safe and do not rewrite untouched lines.

## 4. Implementation plan

Legend: **[THIS REPO]** = servoy-eclipse, implementable now. **[SIBLING: Copilot]** = `D:\Eclipse\202603Workspace\Servoy-Copilot`, reachable on disk, implement if the Step 1 bundle is resolvable there, otherwise hand-off. **[SIBLING: skill4servoy]** = `D:\Eclipse\202603Workspace\skill4servoy`.

Tooling reminders: servoy-eclipse and Copilot files in Eclipse projects are edited through the `eclipse-coder` tools via `execute` (AGENTS.md); after each change run `organizeImports`, `formatFile`, `getCompilationErrors`; `.tests` are plain JUnit 5/6 Jupiter; no `Assume.*`; one specific expected outcome per assertion.

### Step 1 — servoy-eclipse [THIS REPO]

1. Precondition: work on `lts_2026` (current branch; confirm with `git branch --show-current`). Do not stage the two unrelated modified files. Re-read `FormSpecGenerator.java` before editing (line numbers in 2.1).
2. Characterisation test first, on the unchanged code: in `com.servoy.eclipse.cypress.tests/src/test/java/com/servoy/eclipse/cypress/plainjunit/services/` add a test (reflection on `parseFrmFile` and `generateCypressSpecContent`, same technique as `FormSpecGeneratorTest`) that records the exact legacy output for a fixed `.frm` string. It must stay green through the whole change and proves the `null`-model path is untouched.
3. Create `FormSpecModel.java` (3.3) including `Gesture.forHandler`.
4. Create `FormSpecModelEmitter.java` (3.4).
5. Modify `FormSpecGenerator.java` (3.5): new overload, delegation, mismatch error, switch of the cy content producer, model-path result message. No signature change on existing methods.
6. No MANIFEST or `build.properties` change (new classes in an existing exported package and source folder). Verify that `com.servoy.eclipse.cypress.tests` still resolves.
7. Add tests (section 5.1).
8. Run `getCompilationErrors`, plain tests via `eclipse-ide` `runJUnitTests` (project `com.servoy.eclipse.cypress.tests`, package `com.servoy.eclipse.cypress.plainjunit`). Ask before committing; subject e.g. `SVY-21517 FormSpecModel and behaviour-case emitter for generateSpec [ai]`.

### Step 2 — Servoy-Copilot [SIBLING: Copilot]

Precondition: Step 1 is resolvable by the Copilot build.
1. `HandlerBinding.java`: add `declaration` component and the new `resolved(..., declaration)` overload (3.7).
2. `FormInteractiveElementsService.java` line 367: pass `method.getDeclaration()`.
3. Create `HandlerEffectRecognizer.java` (3.6) in `bundles/com.servoy.eclipse.developer.mcp/src/com/servoy/eclipse/developer/mcp/services/`. The package is already exported; no MANIFEST change.
4. Create `FormSpecModelBuilder.java` (3.7).
5. `ServoyTestingServer.java`: field, helper, four call-site swaps, tool description (3.8).
6. Before merging: run the spike in 6 (Cypress behaviour checks), because it decides whether `not.be.visible` and the `data-cy` placement assumptions hold.
7. Tests (section 5.2). Run plain tests with `runJUnitTests` for `com.servoy.eclipse.developer.mcp.tests`, package `com.servoy.eclipse.developer.mcp.plainjunit`; integration with `runJUnitPluginTests` and the `PDEIntegrationTestsBaseCopy.launch` of that project.
8. Ask before committing; subject starts with `SVY-21517` and ends with `[ai]`.

### Step 3 — skill4servoy [SIBLING: skill4servoy]

1. Update `tooling.md` as described in 3.9.
2. No code. Ask before committing.

### 6. Verification spike (before Step 2 merges)

On a real form in a running Servoy Developer with NG client: generate a spec with a button whose handler does `elements.lbl.visible = false;`, run `testForm`, and confirm (a) `should('not.be.visible')` passes when NG keeps the element in the DOM and what happens when NG removes it, (b) `data-cy` sits on the element on which `.click()`, `.type()`, `be.enabled` and `have.class` act. Record the result in this spec's Q2/Q3 and adjust the assertion table in 3.4 if needed (for example `not.exist` alternative).

[UPDATED] Result of the first real run (six forms of `test_webcomponents2`, IDE Cypress runner): `data-cy` selectors worked for `bootstrapcomponents-button`, `-textbox`, `-imagemedia`, `servoyextra-fileupload`/`multifileupload` and `aggrid-groupingtable` (baseline `be.visible` and `.click()` on buttons passed). An element with `visible:false` is not in the page at all ("never found"), which led to the hidden-element rule. NOT covered by the run: none of the six forms has a handler with a recognised on-form effect, so `not.be.visible` after a click, `have.class` and `contain.text`, `be.enabled`, and typing into an input are still unproven in a browser. The spike therefore stays open for those.

## 5. Tests

### 5.1 servoy-eclipse, `com.servoy.eclipse.cypress.tests`, plain JUnit (package `com.servoy.eclipse.cypress.plainjunit.services`)

Existing `FormSpecGeneratorTest` (reflection on `parseFrmFile`, `generateCypressSpecContent`, `generateSetupContent`) and `FormSpecGeneratorSolutionPrefixTest` must pass unchanged.

`FormSpecModelTest` (gesture table), each an `assertEquals` on one value:
- `forHandler("onActionMethodID", "servoydefault-button", true) == CLICK`
- `forHandler("onAction", "bootstrapcomponents-button", false) == CLICK`
- `forHandler("onActionMethodID", "bootstrapcomponents-textbox", true) == NONE`
- `forHandler("onActionMethodID", "servoydefault-textfield", true) == NONE`
- `forHandler("onActionMethodID", "Field", true) == NONE` (legacy text field fires on Enter)
- `forHandler("onActionMethodID", "GraphicalComponent", false) == CLICK`
- `forHandler("onDataChangeMethodID", "Field", true) == NONE` (legacy `Field` display type is unknown, so no `CHANGE`)
- `forHandler("onRightClickMethodID", "servoydefault-label", false) == RIGHT_CLICK`
- `forHandler("onDataChangeMethodID", "bootstrapcomponents-textbox", true) == CHANGE`
- `forHandler("onDataChangeMethodID", "bootstrapcomponents-textbox", false) == NONE`
- `forHandler("onDataChange", "servoydefault-checkbox", true) == NONE`
- `forHandler("onCellClick", "aggrid-groupingtable", false) == NONE`
- `forHandler("onColumnDataChange", "bootstrapcomponents-textbox", true) == NONE`
- `forHandler(null, "x", false) == NONE`
- list inputs are copied (mutating the source list does not change the model).

`FormSpecModelEmitterTest` (`assertEquals` on full strings or exact lines):
- golden: a model with two elements and no handlers equals the exact expected full text (baseline only, includes `cy.visit('<url>')`, the `[data-cy^="<form>."]` wait and `be.visible` lines).
- golden: empty element list uses the `.svy-form` wait and emits only the error-overlay check in the baseline `it`.
- `CLICK` handler with `VISIBLE false` on another element: the case body equals exactly the click line, the `not.be.visible` line and the error-overlay line, in that order.
- `RIGHT_CLICK` emits `.rightclick();`.
- `CHANGE` emits `.clear().type('1').blur();`.
- one test per effect line in the 3.4 table (`be.visible`, `not.be.visible`, `be.enabled`, `be.disabled`, `have.class`, `not.have.class`, `contain.text`).
- `OFF_FORM` emits exactly `    // OFF-FORM (not asserted): scopes.orders.load(id)` and no `cy.get` for it.
- a `TEXT` effect with an empty value (and one with a blank value) emits exactly `    // OFF-FORM (not asserted): elements.x.text = <empty>` and no `contain.text` line.
- branching handler: body equals click line, `    // BRANCHING: one case per branch needed (forms.orders_list.newOrder)`, error-overlay line; the effects of that handler do not appear.
- `NONE` gesture emits exactly `it.skip('<el>: <key> (no gesture defined)', () => {});` and no click.
- unresolved handler emits exactly the `(handler method could not be resolved)` skipped case.
- determinism: the same model with elements and handlers supplied in two different orders yields equal strings; emitting twice yields equal strings.
- ordering: elements `b`, `a` produce `a` cases before `b` cases; handlers `onRightClickMethodID`, `onActionMethodID` are emitted in key order.
- escaping: element name `o'k` is emitted as `o\'k` in JS strings; an `OFF_FORM` value containing a newline and 300 characters is emitted as one comment line of at most `// OFF-FORM (not asserted): ` + 160 characters (ending with `...`).
- no model path leakage: the output never contains `bgcolor`, `fgcolor`, `font` or `border` for any model.

`FormSpecGeneratorFrmFormatTest` (parser, 9 tests, built from a trimmed copy of the real f1 `.frm`):
- unquoted keys: data source, element names in source order without the form name, type names, `isWebComponent` (typeid 47), `isButton`/`isLabel` (typeid 7), the setup file names the data source (no "No dataSource on this form"), and the legacy spec asserts the real elements and waits on `data-cy` instead of `.svy-form`
- quoted keys still work (`mem:test_grid`, `groupingtable`)
- `subname` and `nodataSource` are not taken for a key

`FormSpecModelHiddenElementTest` (7 tests):
- a hidden element is left out of the baseline and its handler is exactly `it.skip('<el>: <key> (element is hidden by design)', () => {});` (golden full text)
- it is never selected or clicked; hidden wins over "no gesture" and "unresolved"; an effect on a hidden element from a visible button is still asserted; the visibility count excludes it while the handler count includes it; a form with only hidden elements still waits on `data-cy`; an `Element` built without the flag is visible

`FormSpecGeneratorModelTest`:
- `new FormSpecGenerator().generateSpec("a", modelFor("b"))` returns exactly `Error: Model is for form 'b' but spec was requested for form 'a'` (the check runs before workspace access).
- the legacy characterisation test from step 2 stays green.

Integration (PDE, `integrationpde`, launcher `/com.servoy.eclipse.core.tests/PDEIntegrationTestsBase.launch`, following the pattern of `FormSpecGeneratorSolutionPrefixIntegrationTest`): `generateSpec(form, model)` writes the `.spec.cy.js` in the solution-prefixed location and the file equals the emitter output; the second call returns `Spec files already exist: ...` (never overwrites).

### 5.2 Servoy-Copilot, `com.servoy.eclipse.developer.mcp.tests`

Plain JUnit (`.../plainjunit/services/`):

`HandlerEffectRecognizerTest`, one outcome per pattern:
- `elements.detail.visible = false;` -> `[VISIBLE detail false]`; `= true` -> `[VISIBLE detail true]`
- `elements.x.enabled = false;` -> `[ENABLED x false]`
- `elements.x.text = 'Hi';` and with double quotes -> `[TEXT x Hi]`; `elements.x.text = 'a' + b;` -> single `OFF_FORM` with that source
- `elements.x.addStyleClass('active');` -> `[ADD_STYLE_CLASS x active]`; `removeStyleClass` -> `[REMOVE_STYLE_CLASS x active]`; class `'a b'` -> `OFF_FORM`
- `elements.x.text = '';` and `elements.x.text = '   ';` -> single `OFF_FORM` with that source (no `TEXT`)
- `elements.x.text = '<b>Hi</b>';`, `'a & b'` and `'1 > 0'` -> single `OFF_FORM` with that source (no `TEXT`)
- `elements.x.bgcolor = '#fff';` (also `fgcolor`, `font`, `border`) -> empty effect list
- `elements.x.bgcolor += 'a';`, `elements['x'].border = 'b';` and `forms.f.elements.x.font = 'c';` -> empty effect list
- `elements.x.bgcolor = '#fff'` (no `;`) followed on the next line by `scopes.a.b();` -> `scopes.a.b()` is not swallowed by the dropped line, it is still reported as `OFF_FORM`
- a final `return;`, `return true;`, `return false;`, `return null;`, `return undefined;`, `return 1;`, `return 'x';` -> no effect for it and `branching == false`
- a final `return scopes.x.check(v);` -> `[OFF_FORM "return scopes.x.check(v)"]`; a non-final `return true;` still yields `branching == true`
- `scopes.orders.load(id);` -> `[OFF_FORM "scopes.orders.load(id)"]`
- mixed body `elements.detail.visible = false; scopes.orders.load(id);` -> exactly two effects in that order
- same target set twice (`visible = false; ... visible = true;`) -> only the last remains
- comments and JSDoc around the function and inside the body are ignored; a `;` inside a string literal does not split a statement
- `if`, `switch`, ternary, `for`, `while`, `try`, a non-final `return` each yield `branching == true`; a single final `return x;` does not
- unbalanced braces and a missing `function` header yield `analyzable == false` and the single `OFF_FORM` note
- a statement without a trailing `;` followed by another statement is reported as one `OFF_FORM` (documented limitation)
- the declaration `null` yields not analysable.

`FormSpecModelBuilderHiddenElementTest` (3 tests): an `InteractiveElement` built without the flag is visible; hidden and shown elements keep their flag in the model; a hidden element stays in the model so it can be an effect target (a visible button that sets `elements.secret.visible = true` yields that `VISIBLE` effect).

`FormSpecModelBuilderTest` (uses `FormSpecModelBuilder.fromElements`, no workspace):
- unnamed and blank-named elements are dropped
- `specMissing` elements are dropped
- duplicate names keep the first
- `onActionMethodID` -> `CLICK`; `onCellClick` -> `NONE` and the recogniser is not run for it
- unresolved binding -> `unresolved == true`
- effect targeting a name that is not in the element list becomes `OFF_FORM` with the original statement
- `STATIC` and `CONTAINER` elements are present in the model with their kind
- `HandlerBinding` equality and the factories still behave (`resolved` without declaration has `declaration == null`).

Existing tests that must stay green: `FormInteractiveElementsFormatterTest` (JSON unchanged), `FormInteractiveElementsServiceGuardTest`, `ServoyTestingServerTest` (tool description rules in 3.8).

Integration (PDE), extend `CypressFormTestingIntegrationTest` (pattern: `ensureForm`, `specGenerator`, `testingServer`): create a form with a button and a label wired to a form method `function hide() { elements.lbl.visible = false; scopes.x.y(); }` (write the `.frm`/`.js` into the test solution project, as `JSUnitRunnerGroupedTest` does for its minimal `.frm`), call `testingServer.generateFormSpec(form)` and assert that the generated `.spec.cy.js` contains the exact lines `cy.get('[data-cy="<form>.btn"]').click();`, `cy.get('[data-cy="<form>.lbl"]').should('not.be.visible');` and `// OFF-FORM (not asserted): scopes.x.y()`. A second test asserts that with no active solution or an unknown form the legacy output is produced (`Created` result and the baseline `it`). The existing assertions at lines 300-345 (`data-cy` or `.svy-form`, `beforeEach(`, `cy.visit(`, `svy_testmode=true`, `function spec_setUp()`) must keep passing for the model path.

## 6. Acceptance criteria

Legend: [x] verified by the test named at the end; [~] implemented and covered by tests, but not proven in a real browser or CI; [ ] not verified.

- [x] `generateSpec(String)` behaves exactly as before; the legacy characterisation tests and all existing `FormSpecGeneratorTest` tests pass without modification (`FormSpecGeneratorModelTest`, `FormSpecGeneratorTest`).
- [x] `generateSpec(String, FormSpecModel)` exists and, for a model whose form name differs from the requested one, returns the exact error string from 3.5 without touching the workspace (`FormSpecGeneratorModelTest`).
- [x] For a model, each bound handler produces one case, sorted by element name then handler key, with the gesture from the table in 3.3 (`FormSpecModelTest`, `FormSpecModelEmitterTest`).
- [~] Recognised on-form effects are asserted exactly as in the 3.4 table, and the allowlist is `visible`, `enabled`, `text`, `addStyleClass`, `removeStyleClass` only; `bgcolor`, `fgcolor`, `font` and `border` never appear in the output. Exact lines unit-tested and covered by the PDE test; not run in a browser because none of the six forms has such a handler.
- [x] Unrecognised and off-form statements appear as `// OFF-FORM (not asserted): <statement>` comments, branching handlers carry the `// BRANCHING: ...` marker with no per-branch assertions, events without a gesture and unresolved handlers become skipped cases that name the event. Also seen on the six real forms (pending tests in Cypress).
- [x] Every non-skipped handler case ends with the no-error-overlay assertion.
- [x] The same model, or the same form, always generates a byte-identical `.spec.cy.js` (no caps, no random values, stable ordering) (`FormSpecModelEmitterTest` determinism tests).
- [x] An existing spec is never overwritten or merged (PDE `FormSpecGeneratorModelIntegrationTest`, `FormSpecBehaviourIntegrationTest`).
- [~] In Copilot, `generateFormSpec`, `showFormInBrowser`, `testForm` and `showAndTest` use the model path and fall back to the legacy path when there is no active solution, the form is not found, or building the model throws. The four call sites use the helper and `generateFormSpec` was seen taking the model path on real forms; the other three tools and the "no active solution" fallback have no test.
- [x] The `getFormInteractiveElements` JSON payload is unchanged by the `HandlerBinding` and `InteractiveElement` changes (`FormInteractiveElementsFormatterTest`).
- [x] `ServoyTestingServerTest` description assertions still pass and the description no longer claims the generator reads the `.frm` file for element names (126 of 126).
- [x] skill4servoy `tooling.md` describes the new generator output and no longer contradicts `SKILL.md:105` and `author-rules.md:82,85` on `cy.intercept` (skill4servoy commit `8f3d1e7`; code-reviewed).
- [~] Each step builds and its tests pass on its own; no cross-repo test-project dependency is introduced. Eclipse compile has 0 errors in all four projects and the plain and PDE tests pass; the Tycho reactor build and Copilot CI resolving the new cypress bundle were not run.
- [x] A `.frm` in Servoy's unquoted key format gives the same metadata as the quoted format, and the setup file names the real data source (`FormSpecGeneratorFrmFormatTest`; seen in the generated setup files of all six forms).
- [x] An element hidden in the form is left out of the baseline and its handlers are skipped cases that are never selected or clicked (`FormSpecModelHiddenElementTest`, `FormSpecModelBuilderHiddenElementTest`, PDE `FormSpecBehaviourIntegrationTest`; f3_multifile_upload ran green in Cypress with `button_2` pending).
## 7. Out of scope

- Off-form stubbing and call assertions (no known mechanism, `spec_setUp` runtime wiring unverified); needs its own case.
- Per-branch cases and a real AST effect analyzer (DLTK/Rhino); follow-up.
- Regeneration or merging into existing specs (`generateSpec` still never overwrites).
- Gestures for component-specific spec events beyond the three-row table (cell click, cell edit, per-column handlers), and type-aware `CHANGE` values.
- Any manual port to `release`/`master`: the change reaches them through the normal upward merge `lts_2026` -> `release` -> `master`.
- An `ENTER` gesture for `onAction` on text-entry inputs (they are skipped cases, Q7).
- Any change to `getFormInteractiveElements` output, to `generateSetupContent`/the `.spec.js` content, to forms that exist only in modules, or to the E2E generators.
- Creating the Jira link between SVY-21517 and SVY-21573 (see Q10).
- Form-level events (`onShow`, `onLoad`, `onRecordSelection`, ...) and effects through form variables bound with `%%var%%`; gestures for grid and component spec events (`onDoubleClick`, `onTabClicked`, cell and header events); a guard for destructive handlers. The real run showed these as the main gaps (see the triage report, *Limits seen on the real solution*).
- Moving the E2E-Tester's hand-written `.cy.ts` coverage into `.cy.js` files of the cy-form folder.
- Finding and running existing form tests from an agent (`listFormTests` / `runAllFormTests`): SVY-21663. Form-test guidance in skill4servoy (`.cy.ts` vs `.spec.cy.js`, the "mock the side effect" wording, routing, a separate Form-Tester agent): SVY-21665.

## 8. Open questions

Status legend: **resolved** = answered from source; **decision** = needs the user, a recommended default is given and used if nobody objects; **spike** = can only be answered by running a real form (section 6 spike), it gates the merge of Step 2, not the start of Step 1; **follow-up** = deliberately out of scope.

| # | Question | Owner | Status |
|---|----------|-------|--------|
| Q1 | Branch. Triage suggested `master`; user decided `lts_2026` because Servoy-Copilot is built against servoy-eclipse `lts_2026`. Changes reach `release` and `master` by the normal upward merge. See 3.2. | user | **resolved** |
| Q2 | Does Cypress `should('not.be.visible')` pass for an NG element removed from the DOM? [UPDATED] Partly answered by the run: NG does not render an element with `visible=false` at all (f3 `button_2`: "Expected to find element ... but never found it"), so that case is now handled by the hidden-element rule. Still open: an element that is visible on load and hidden by a handler (not present in the test solution); the fallback `should('not.exist')` is accepted either way. | implementer | **spike** (partly answered)
| Q3 | Is `data-cy` on the element that receives `.click()`, `.type()`, `be.enabled`, `have.class`, `contain.text`, or on a wrapper? [UPDATED] Partly answered: `be.visible` and `.click()` on `bootstrapcomponents-button` worked through `data-cy` in all six forms, and `be.visible` worked for textbox, image, file upload and grid. `.type()`, `be.enabled`, `have.class` and `contain.text` were not exercised. | implementer | **spike** (partly answered)
| Q4 | `CHANGE` types `'1'` (the text-entry set only holds text inputs, so it is valid input). Type-aware values are out of scope (section 7). | user | **resolved** (user: keep) |
| Q5 | Handler key names and component names of the text-entry set. Read from the `.spec` files: `bootstrapcomponents-textbox`, `bootstrapcomponents-textarea`, `servoydefault-textfield`, `servoydefault-textarea` all exist with exactly these `name` values, and every one of them (and the button specs) names its handlers `onActionMethodID`, `onDataChangeMethodID`, `onRightClickMethodID`. The bare `onAction`/`onDataChange`/`onRightClick` entries stay in the table as harmless extras. | implementer | **resolved** |
| Q6 | Elements hidden in the form (visible=false). Found by running the generated f3_multifile_upload spec: `button_2` has `visible:false`, NG does not render it, so the baseline `be.visible` and the click case failed with "never found". Decision (user): keep the real state of each element in the model. The service reads `visible` from the element and from every container around it; `FormSpecModel.Element` has a `visible` flag; the emitter leaves a hidden element out of the `be.visible` baseline and emits each of its handlers as `it.skip('<el>: <key> (element is hidden by design)')`. A hidden element stays in the model, so a visible button that shows it can still assert `be.visible` on it. | user | **resolved** |
| Q7 | `onAction` on text-entry inputs fires on Enter, not on click. Text-entry types map to `NONE` and so get the standard skipped case `it.skip('<el>: <key> (no gesture defined)', ...)` from 3.4; buttons and other types keep `CLICK` (see 3.3). | user | **resolved** (user: skipped case) |
| Q8 | Keep the `TEXT` effect (`contain.text`)? No form of the test solution sets `text`, so the run could not show whether it is flaky. Keep it; drop it from the allowlist if a real run shows it is. | implementer | **spike**
| Q9 | Which servoy-eclipse branch does Servoy-Copilot build against? `lts_2026`: Copilot's `RenamePersistIntegrationTest.java:371` calls `getOldSpecFilePath`, which exists only on `lts_2026`. (`findExistingSpecFile`, used by `PersistRenameService.java:68`, exists on both branches.) How Copilot's Tycho/CI resolves the servoy-eclipse bundle (p2 site vs workspace) is still not verified; it decides when Step 2 can compile in CI, not what it contains. | user | **resolved** (branch); CI resolution stays a Step 2 precondition |
| Q10 | Jira has no link between SVY-21517 and SVY-21573 (checked: SVY-21517 links only to SVY-21663 and SVY-21665). The relation is documented in code and docs only (SVY-21573 spec Q7, skill4servoy `tooling.md` "open enhancement"). Add a `Relates` link? Does not block anything. | user | **decision** |
| Q11 | Whether `spec_setUp`/`spec_tearDown` in the generated `.spec.js` runs at runtime. Only relevant to deferred off-form stubbing; this spec does not touch that file. | follow-up case | **follow-up** |
| Q12 | Handlers nested in array properties of spec components (for example per-column data grid handlers) are not read by the top-level `json.opt(handlerName)` at `FormInteractiveElementsService.java:284`. If missing they simply produce no case; check against the data grid spec during Step 2. | implementer | **follow-up** (checked in Step 2, no spec change) |
| Q13 | Can `org.mozilla.javascript.ast` be imported in the Copilot bundle? Its MANIFEST imports only `org.mozilla.javascript` (line 108); no `.ast` package is imported, so it would need an `Import-Package` addition. Only matters for the deferred AST analyzer. | follow-up case | **follow-up** |
| Q14 | The agent's own `testForm` runs failed with `[data-cy^="<form>."]` not found (30 s timeout) and the set of failing specs changed from run to run (first f3 and f6_header_checkbox_click, in the re-run the other four), while the same specs were green every time from the IDE runner. Testing mode is NOT the cause: every generated spec visits `...?formpreview=<form>&svy_testmode=true`, `NGClient.handleArguments` turns that argument into a client property, and `Utils.isInTestingMode` lets the client property win over the global `servoy.ngclient.testingMode`; the global setting is also true in `application_server\servoy.properties`. The IDE handler and the agent call the same `FormSpecRunner.runFormCypressTests`. The difference seen on disk: the agent ran the six specs at the same time (all six videos of one run written within 2 s; failure screenshots of three specs in the same second), while the IDE handler (`RunAllCypressFormTestsHandler`, loop at lines 219-239) runs one form after another (21-29 s each) and `runFormCypressTests` has no lock. Screenshots of the failed runs show a blank page. Parallel load as the cause is inferred from the timestamps, not measured; the agent's own calls were not seen. Mitigation: run forms one at a time (SVY-21663 `runAllFormTests` must be sequential; a lock in `FormSpecRunner` would make parallel calls queue). | user | **open** (likely parallel runs)
| Q15 | Visibility inherited from a container (`visible=false` on a layout container or tab around the element) is implemented by walking the parents but only a hidden web component and a hidden legacy persist were tested. | implementer | **open** |

Note: this spec was written from source reading and then implemented. Verified by running: compile, the plain and PDE tests, and the generated specs of six real forms in the IDE Cypress runner (green after the two fixes of 2.7). Not verified: the recognised on-form assertions in a browser, the Tycho reactor build, Copilot CI. Re-check line numbers on the branch actually used.
