# Triage Report — SVY-21517

**Verdict:** PROCEED

> **Status update (implementation phase).** The approved approach B' was implemented in all three repos. Three things changed from what this report first said; they are marked `[UPDATED]` below and explained in the new section *Updates after implementation*. (1) The work lands on `lts_2026`, not `master`. (2) Running the first generated specs exposed two defects that source reading had not shown: `parseFrmFile` could not read the unquoted `.frm` format Servoy writes, and a form element with `visible=false` made the generated spec fail. Both are fixed. (3) Text inputs and the legacy `Field` type get a skipped `onAction` case, because a text field fires `onAction` on Enter, not on click.

Redo of the earlier NEEDS_INPUT pass. The four open questions are answered from code below. Recommended approach: **B' — a neutral spec model in `com.servoy.eclipse.cypress` (servoy-eclipse), filled by a new builder in `com.servoy.eclipse.developer.mcp` (Servoy-Copilot) from `FormInteractiveElementsService`**. Phase 1 emits click + on-form assertions from a closed pattern set. Stubbing off-form effects, per-branch cases and a real AST effect analyzer are deferred.

## Reported problem

`generateFormSpec` produces a render/visibility-only Cypress spec. It never clicks, never asserts an effect, never stubs. The ticket wants one scaffolded case per bound handler (element `onAction`/`onRightClick`/`onDataChange` plus component-spec events such as a data grid's `onColumnDataChange`/`onCellClick`), with these rules:

- On-form NG effects (`visible`, `enabled`, style classes, `text`, `setSize`, ...) are asserted directly.
- Off-form effects (scope function, DB, HTTP, navigation) are stubbed at the edge and the call is asserted.
- A branching handler gets one case per branch.
- Legacy Smart Client properties (`bgcolor`, `fgcolor`, `font`, `border`) are never scaffolded.
- The same form must always generate the same test.

Jira (read in full): SVY-21517 is a New Feature, In Progress, created 2026-09-29. It has **no comments, no links and no attachments**, so nothing in Jira answers the open questions. SVY-21573 is a Task, In Review, with no comments. Its only link is to SVY-21169. There is no Jira link between SVY-21517 and SVY-21573, and one should be added.

## Root-cause assessment

This is a feature gap, not a defect. The symptom is confirmed in `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecGenerator.java`:

- `generateCypressSpecContent` (lines 303-354) emits only `cy.visit`, an error-overlay check, `should('be.visible')` for at most 8 elements (`.limit(8)`, line 328) and `be.visible`/`be.enabled` for at most 3 buttons (`.limit(3)`, line 338).
- `parseFrmFile` (lines 264-301) is a regex splitter over `.frm` text. It keeps `name`, `typeName`, `dataSource` and a few `typeid` booleans. It never reads a handler binding.
- `generateSpec` returns early with "Spec files already exist" when both files exist (lines 58-62). Any improvement only affects forms that have no spec yet. Existing visibility-only specs are never upgraded or merged.

### Dependency direction (answers "which repo")

- `com.servoy.eclipse.developer.mcp` (Servoy-Copilot) `MANIFEST.MF` has `com.servoy.eclipse.cypress` in `Require-Bundle`.
- `com.servoy.eclipse.cypress` (servoy-eclipse) `MANIFEST.MF` does not require `developer.mcp`. It exports `com.servoy.eclipse.cypress.services`.
- The `generateFormSpec` MCP tool is registered in Copilot, `ServoyTestingServer.java:408-427`, and calls `specGenerator.generateSpec(formName)` (line 420). `showFormInBrowser` (205) and the test tools (272, 298) call it too. `PersistRenameService.java:54` also instantiates `FormSpecGenerator`.
- Result: cypress cannot see `FormInteractiveElementsService`, and the reverse dependency would be a cycle. The element service cannot be called from inside `FormSpecGenerator`. Information has to flow Copilot -> cypress through a neutral data type that lives in cypress.
- servoy-eclipse callers have no Copilot at runtime: `HeadlessFormTestExecutor.java:93`, the context-menu actions and the cypress tests. The existing regex path must therefore stay as the fallback.

### What SVY-21573 / skill4servoy 41d866e / Servoy-Copilot 770bbc7 already deliver

Servoy-Copilot `770bbc7` (SVY-21573, diffs read):

- Adds `FormInteractiveElementsService` (413 lines), `HandlerBinding`, `InteractiveElement`, the formatter and `FormLocatorService`. It also adds the MCP tools `getFormInteractiveElements` and `locateForm`.
- The service walks the flattened form model, including inherited elements, layout containers and portals.
- It resolves every handler id to a `ScriptMethod`: `resolveHandler`, lines 352-368, owner `forms.X` or `scopes.Y`. It also produces the `data-cy` selector `<form>.<element>`.
- Web-component handlers come from `ComponentSpecService.getHandlerNames` (`ComponentSpecService.java:303-311`, `spec.getHandlers().keySet()`). Legacy handlers come from `BaseComponent.isEventOrCommandProperty`.
- Nothing effect-related. `HandlerBinding` (`HandlerBinding.java:20`) carries only `handler`, `method`, `owner`, `unresolved` and `methodId`. It has no method body.
- Its spec lists "changing `generateFormSpec`/`FormSpecGenerator` (servoy-eclipse stays untouched)" as out of scope (`docs/SVY-21573-bug-triage-form-lookup.spec.md:450`). Q7 (lines 469, 503) defers "let `generateFormSpec` consume the element service" to a follow-up case. That follow-up is SVY-21517.

skill4servoy `41d866e` (SVY-21573, diff read):

- Agent permissions and guidance only. E2E-Tester and Researcher are allowed `getFormInteractiveElements`/`locateForm`, and `svyCreateFormTest.md` now takes its element list from the tool.
- The per-effect rules already existed in `.opencode-shippable/skills/servoy-e2etester/context/tooling.md` as unchanged context lines of this diff. They say to assert per effect, directly on-form and stubbed off-form. They say a branching handler gets one case per branch. They also say "centralising it in the generator is the open enhancement" (line 86). That is this ticket, stated by the skill author. The skill already tells the agent to author the behaviour by hand on top of the generator's baseline, so the agent flow is a stop-gap, not the goal.
- No generator, template or effect-analysis code.

What remains for SVY-21517:

1. Consume the element service in the generator path.
2. Click each handler.
3. Recognise effects in the bound method body.
4. Emit assertions deterministically.

### Handler-body analysis: honest sizing (answers Q2)

- No effect-extraction machinery exists anywhere. In the Copilot bundle, grep for `getDeclaration`/`getMethodCode`/`getSourceCode` returns no matches, so no Copilot code reads a method body from the model.
- `CodeAnalysisService` (570 lines) only does reference search and call hierarchy via `ScriptMethodSearch`/`ScriptVariableSearch`. It answers "who calls whom", not "what does this do".
- The only body-text scanning is `FormNavigationGraphService.analyzeScriptFile` (lines 440-534). It uses regexes plus function-offset slicing on `.js` text to find `showForm` and navigation, and tags the edges `confidence("dynamic")`. That is the in-repo precedent for a regex scan.
- The body text is directly available: `ScriptMethod.getDeclaration()` (`servoy-client/.../AbstractScriptProvider.java:186`) returns the full source of the method. `resolveHandler` already holds the `ScriptMethod`, so this needs no DLTK.
- A real AST analyzer would be greenfield. No `org.mozilla.javascript.Parser`/`ast` usage exists in the Copilot bundle. Whether `org.mozilla.javascript.ast` can be imported there is not verified.
- Conclusion: a closed-pattern recogniser is small (roughly 150-250 LOC plus tests). A branch-aware effect analyzer is a separate project and is deferred.

### Deterministic generator vs agent-driven (answers Q3)

- Who consumes the output today: the agent (`E2E-Tester` has `generateFormSpec: allow`), `HeadlessFormTestExecutor.generateMissingSpecs` (CI) and the auto-generate in `showFormInBrowser`/`testForm`. Headless and CI have no agent, so only a deterministic generator can improve them.
- What metadata alone gives you: the click/right-click/change trigger, selector and bound method per handler are fully deterministic from `InteractiveElement`/`HandlerBinding`. Effect assertions are not derivable from metadata. A click with no assertion is exactly the "a click that asserts nothing is not a test" case the skill forbids. So body inspection cannot be skipped entirely, but a literal-pattern scan is enough for phase 1.
- Determinism is straightforward: sort elements by name and handlers by key, drop the arbitrary `.limit(8)`/`.limit(3)` caps (or make them explicit), and never use random ids in the spec. The setup file's `UUID.randomUUID()` (line 378) is in `.spec.js`, not the `.spec.cy.js`.

### Spec events (answers Q4)

- Enumeration is free. `addWebComponent` iterates every handler name the spec declares and reads `json.opt(handlerName)` (`FormInteractiveElementsService.java:282-287`), so `onColumnDataChange`/`onCellClick` appear whenever they are bound at the component level.
- Triggering is not free. A cell click or a cell edit needs component-specific gestures. A fixed gesture table covers `onAction*`/click, `onRightClick`/`rightclick` and `onDataChange` on dataprovider inputs. Other spec events should be emitted as a labelled, skipped case that names the event, not as a guessed gesture.
- Handlers nested inside array properties (e.g. per-column handlers of a grid) are not read by the top-level `json.opt`. This is not verified against the data-grid spec and should be checked in the implementation.

## Ticket premise check

1. **Problem: real and verified in code.**
2. **"The generator should look at what the bound method does": right goal, wrong source.**
   - The current regex `.frm` parser cannot resolve handlers. Extending it would duplicate what `FormInteractiveElementsService` does properly, which SVY-21573 Q7 already flagged.
   - The generator cannot call the service (dependency direction above). The fix is a neutral model plus a Copilot-side builder.
3. **The ticket's off-form stub example does not hold as written.**
   - It uses `cy.get('@loadStub')`, i.e. a browser-side `cy.stub`. Servoy handlers run server-side behind a websocket.
   - skill4servoy says as much: "Servoy is websockets, so there is no HTTP request to stub or wait on" (`author-rules.md:85`) and `SKILL.md:105` ("No `cy.intercept()`").
   - The skill contradicts itself: `tooling.md:77` still says "Stub/`cy.intercept` at the form's edge".
   - A stub that records scope-function calls would have to live in the Servoy runtime, i.e. the generated `.spec.js` setUp, and Cypress would then need a way to read the recorded call back. I found no consumer of `spec_setUp`/`spec_tearDown` in servoy-eclipse, Servoy-Copilot `bundles`, `server` or `servoy-client` (Java sources) or the ngclient.ui node sources. It is not verified that the setUp file is wired to anything at runtime, so there is no known stub mechanism today.
   - This is why off-form stubbing is explicitly deferred, not silently dropped: phase 1 labels those effects instead of stubbing them.
4. **On-form assertions are feasible**, e.g. `elements.detail.visible = false` -> `should('not.be.visible')`. One risk to confirm in a PDE integration run: Cypress treats `not.be.visible` on an element that is not in the DOM at all as a failure, and NG may remove invisible elements. Not verified.
5. **NG-only rule:** trivially satisfied by a closed allowlist. Anything not on the list, including `bgcolor`/`fgcolor`/`font`/`border`, is never scaffolded.

## Approaches considered

1. **No code change; rely on the skill/agent flow.**
   - Pros: nothing to build.
   - Cons: headless/CI and the auto-generate paths stay visibility-only. The skill itself calls centralising this in the generator "the open enhancement" (`tooling.md:86`). Output is not deterministic.
2. **Enrich `FormSpecGenerator` in place (extend the regex parser).**
   - Pros: single repo.
   - Cons: a worse re-implementation of handler resolution, no method owner or spec handlers, and it entrenches the duplication Q7 wanted removed. Still needs body access, which the `.frm` text only offers in a fragile way.
3. **Move `FormInteractiveElementsService` into servoy-eclipse so cypress can use it.**
   - Pros: no model type in the middle.
   - Cons: the service depends on Copilot's `ComponentSpecService` and `ServoySolutionService.getFormLayoutType`. This is a big move that breaks SVY-21573's tests and layout. Not worth it.
4. **(Recommended) B': neutral model in cypress + builder in Copilot.**
   - Pros: dependency direction is respected, a single source of truth for handler resolution, a legacy fallback for headless callers, and a small, reviewable first slice.
   - Cons: a two-repo change that must land in order; the effect recogniser is regex-grade.
5. **Full AST effect analyzer first.**
   - Pros: the only way to get branches right.
   - Cons: greenfield, large, and blocked on the unknown stub mechanism. Right as a follow-up, wrong as the first step.

## Recommendation

**PROCEED with B', phased. [UPDATED] Targets `lts_2026` in servoy-eclipse; the change reaches `release` and `master` through the normal upward merge.** This report first recommended `master` with a worktree. The user decided otherwise because Servoy-Copilot is built against servoy-eclipse `lts_2026`: Copilot's `RenamePersistIntegrationTest.java:371` calls `FormSpecGenerator.getOldSpecFilePath`, which exists only on `lts_2026`, so Step 1 must be on `lts_2026` for Step 2 to compile. To keep the later merge small, the edits to `FormSpecGenerator.java` are additive and the bulk sits in the new files `FormSpecModel.java` and `FormSpecModelEmitter.java`.

### Step 1 — servoy-eclipse (lands first)

Files:

- `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecGenerator.java`
- a new model file in the same exported package, e.g. `FormSpecModel.java` with plain records for element, handler, effect and a trigger gesture

Changes:

- Add `generateSpec(String formName, FormSpecModel model)`. `generateSpec(String)` stays and delegates with `null`, which keeps the current regex path. `HeadlessFormTestExecutor.java:93`, `PersistRenameService` and the cypress unit tests continue to work.
- Keep the private `parseFrmFile`/`generateCypressSpecContent(FormMetadata)` signatures. `FormSpecGeneratorTest` calls them by reflection (lines 79-125 and 253+). Add a new private emitter for the model path instead of changing those.
- Without MANIFEST changes: the model sits in an already-exported package, and Copilot already requires `com.servoy.eclipse.cypress`.

New output (model path), per bound handler, in sorted order:

- A case that performs the handler's trigger on `[data-cy="<form>.<element>"]`:
  - `click()` for `onAction*`
  - `rightclick()` for `onRightClick*`
  - clear/type/blur for `onDataChange` on dataprovider inputs
- Then the recognised on-form assertions, from a closed set:
  - `elements.<x>.visible|enabled = <boolean literal>`
  - `elements.<x>.addStyleClass('c')`/`removeStyleClass('c')`
  - possibly `elements.<x>.text = '<literal>'`
  - never the legacy colour/font/border properties
- Always a final no-error-overlay assertion after the action.
- Each effect that is not recognised, or that leaves the form (scope function, DB, HTTP, navigation), becomes a labelled comment line carrying the source statement, e.g. `// OFF-FORM (not asserted): scopes.orders.load(id)`.
- A handler with `if`/`switch`/`?:` gets the click case plus a marker comment `// BRANCHING: one case per branch needed`, with no per-branch assertions.
- For spec events with no gesture-table entry: a skipped case naming the event.

### Step 2 — Servoy-Copilot

Files:

- New `FormSpecModelBuilder` (name free) in `com.servoy.eclipse.developer.mcp/.../services`. It maps `FormInteractiveElementsService.collect(...)` output plus the handler's `ScriptMethod.getDeclaration()` into `FormSpecModel` and runs the pattern recogniser.
- `HandlerBinding.java`/`FormInteractiveElementsService.resolveHandler` (lines 352-368): carry the declaration text, ignored by `FormInteractiveElementsFormatter` so the `getFormInteractiveElements` JSON is unchanged.
- `ServoyTestingServer.java`: switch `generateFormSpec` (420), `showFormInBrowser` (205) and the two test-tool calls (272, 298) to the enriched entry point. Update the tool description at lines 408-414: it currently says it "Reads the form's .frm file to extract element names".
- If `getFormInteractiveElements` would return an error (no active solution, form not found), fall back to the legacy call so behaviour never regresses.
- Tests: plainjunit for the recogniser (pure string input, the one-outcome-per-pattern rule from AGENTS.md); integrationpde via the existing `CypressFormTestingIntegrationTest` pattern for a real form.

### Step 3 — skill4servoy (after both)

- `.opencode-shippable/skills/servoy-e2etester/context/tooling.md` (about lines 59-90): state that the generator now emits click + recognised on-form assertions, and that the agent only completes the labelled OFF-FORM/BRANCHING markers.
- Resolve the `cy.intercept` contradiction: `tooling.md:77` against `SKILL.md:105` and `author-rules.md:82`.

### Explicitly deferred

- Off-form stubbing and call assertions: no known mechanism and `spec_setUp` wiring unverified. Needs its own investigation (follow-up case).
- Per-branch cases and any real AST effect analyzer (DLTK/Rhino): follow-up.
- Regeneration or merge of existing specs: `generateSpec` still never overwrites (line 58-62), and the skill's "extend, never overwrite" is the agent's job.
- Gestures for component-specific spec events beyond the three-row table.
- A manual port to `release`/`master` (the change travels by the normal upward merge).

### Defaults assumed (override if wrong)

- [UPDATED] `lts_2026` in servoy-eclipse, `master` in Servoy-Copilot and skill4servoy.
- The effect set is the closed allowlist above; adding more later is additive.
- Unrecognised effects become comments, never guessed assertions.

### Not verified

- Whether `spec_setUp` is executed anywhere at runtime.
- `should('not.be.visible')` behaviour for NG elements removed from the DOM. [UPDATED] Partly answered by the run: NG does not render an element with `visible=false` at all (f3 `button_2`: "Expected to find element ... but never found it"). Still not seen: an element that is visible on load and hidden by a handler.
- Handlers nested in array properties of spec components.
- Whether `org.mozilla.javascript.ast` is importable in the Copilot bundle (only matters for the deferred AST analyzer).
- [UPDATED] Nothing was built or run when this triage was written. The implementation was built and run afterwards; see *Updates after implementation* for what is now verified and what is still open.

## Git history findings

- servoy-eclipse `FormSpecGenerator.java` (log on the `lts_2026` working tree): `2995c88fee` SVY-21284 (tests split), `9149bbb685` SVY-21323 (solution-prefixed paths, formpreview shutdown), `f51f46bf4e` SVY-21296 (extracted into the standalone plugin). On `master` only the latter two appear. No commit ever added click or behaviour assertions: the visibility-only shape is original.
- `master` and `lts_2026` differ on this file by roughly 260 lines, and the cypress bundle version differs (`2026.12.0.qualifier` on `master` vs `2026.3.2.qualifier`). Any change needs a deliberate branch choice.
- Servoy-Copilot: `FormInteractiveElementsService`, `HandlerBinding`, `InteractiveElement` and `FormLocatorService` come solely from `770bbc7`. `CodeAnalysisService` was last touched by `addc313` SVY-21023 and has no body-analysis code. `ServoyTestingServer` is the only caller of `generateFormSpec`.
- skill4servoy `41d866e`: permissions and guidance only. The per-effect rules in `tooling.md` predate it.
- No dependency or target-platform change is involved, and this is not a regression. Both repos have unrelated uncommitted changes and untracked files in the working tree, so check before committing.

## Updates after implementation

The approved approach B' was implemented in all three repos and run in a Developer. The sections above are the triage as it was approved; where the run superseded something it is marked `[UPDATED]`.

### What was built (as approved)

- servoy-eclipse (`lts_2026`): `FormSpecModel` (neutral records and the shared `Gesture.forHandler` table), `FormSpecModelEmitter`, and a small additive change to `FormSpecGenerator` (`generateSpec(String, FormSpecModel)`; `generateSpec(String)` delegates with `null`).
- Servoy-Copilot (`master`): `HandlerBinding.declaration`, the pass-through in `FormInteractiveElementsService`, `HandlerEffectRecognizer`, `FormSpecModelBuilder`, and the wiring in `ServoyTestingServer` (`generateFormSpec`, `showFormInBrowser`, `testForm`, `showAndTest`, with fallback to the legacy path).
- skill4servoy (`master`, commit `8f3d1e7`): `tooling.md` and `agents/Orchestrator.md`.

### Defects the run exposed (not visible from reading the source)

1. **`parseFrmFile` could not read the format Servoy writes.** It matched only quoted keys (`"dataSource":"..."`). Servoy writes `.frm` files with unquoted keys (`dataSource:"..."`, `name:"..."`, `typeid:47`); 5 of the 6 forms of `test_webcomponents2` are unquoted. Effect: the setup file said "No dataSource on this form", and the legacy path would have generated a spec that checks no elements. It is older than this case and not caused by it. Fixed in `FormSpecGenerator` (optional quotes, a lookbehind so `subname` or `nodataSource` do not match, and `typeid` read as a number). New test class `FormSpecGeneratorFrmFormatTest`: 9 tests, 7 failed before the fix, all pass after.
2. **A hidden element made the generated spec fail.** f3 `button_2` has `visible:false`. NG does not render it, so the baseline `be.visible` and the click case failed with "never found". This is the open question Q6 of the spec, which the user had first answered "keep the baseline"; the run changed the answer. Fixed: `InteractiveElement` and `FormSpecModel.Element` carry a `visible` flag (read from the element and from every container around it); the emitter leaves a hidden element out of the baseline and emits each of its handlers as `it.skip('<el>: <key> (element is hidden by design)')`. The element stays in the model, so a visible button that shows it can still assert `be.visible` on it.

### Decisions taken during implementation

- Branch `lts_2026` instead of `master` (see the recommendation above).
- `onAction*` on a text input (the four web text inputs) and on the legacy `Field` type is `Gesture.NONE`, a skipped case: a text field fires `onAction` on Enter, not on click. `onDataChange*` stays `CHANGE` only for the four web text inputs, because the model has no display type for a legacy `Field`.
- `CHANGE` types the literal `'1'`; type-aware values are out of scope.
- Code-review follow-ups: a final `return;` or `return <literal>;` is not reported as an off-form effect; a `TEXT` effect needs a non-blank literal without `<`, `>`, `&`; the "dropped" rule for `bgcolor`/`fgcolor`/`font`/`border` also covers `+=`, bracket access and `forms.<f>.elements...`, and cannot swallow the next statement.

### Verified and not verified

| Verified | How |
|----------|-----|
| Compile, 0 errors | `com.servoy.eclipse.cypress`, `.cypress.tests`, `com.servoy.eclipse.developer.mcp`, `.developer.mcp.tests` |
| servoy-eclipse plain tests | 568 of 568 pass |
| Servoy-Copilot plain tests | 785 of 787 pass; the 2 failures are `JSUnitCoverageServiceTest` and come from the machine locale (`66,7` instead of `66.7`), unrelated |
| `ServoyTestingServerTest` (tool description rules) | 126 of 126 pass |
| PDE `FormSpecBehaviourIntegrationTest` | 9 of 9 pass, including a hidden button |
| Generated specs run in Cypress, 6 forms of `test_webcomponents2` | first run 5 of 6 (f3: 2 failing tests on `button_2`); after the hidden-element fix and a Developer restart 6 of 6 green, `button_2` shown as pending "(element is hidden by design)" |
| Setup files name the real data source | `db:/servoy_test/orders`, `mem:sample_data`, `mem:test_grid` |

| Not verified | Why |
|--------------|-----|
| Recognised on-form assertions in a real browser (`not.be.visible` after a click, `have.class`, `contain.text`, `be.enabled`) | none of the 6 forms has a handler with a recognised on-form effect, so these were only unit-tested |
| An element that is visible on load and hidden by a handler | not present in the test solution |
| Hidden through a container (`visible=false` on a layout container or tab around the element) | the code walks the parents, but only a hidden web component and a hidden legacy persist were tested |
| Tycho reactor build, Copilot CI resolving the new cypress bundle | not run |
| The agent's own `testForm` runs | failed with `[data-cy^="..."]` not found and the failing set changed between runs (first f3 and f6, in the re-run the other four); the IDE runner was green every time. Not testing mode: the spec URL carries `svy_testmode=true` and the client property overrides the global setting. The agent ran the six specs at the same time (all six videos of one run written within 2 s; the IDE loop runs one form after another and `FormSpecRunner.runFormCypressTests` has no lock). Failed runs show a blank page. Cause under parallel load inferred, not measured. See Q14 of the spec; mitigation in SVY-21663 (sequential run-all). |

### Limits seen on the real solution

- Of 39 handler cases on the 6 forms, 10 are emitted and 29 are skipped: 26 grid events with no gesture (`onCellClick`, `onSort`, `onSelectedRowsChanged`, ...), 2 file-upload events, 1 hidden button.
- None of the 10 emitted cases asserts anything on the form beyond "no error overlay": `btn_clear` sets a form variable (`fvar = ""`) that a label shows through `%%fvar%%`, which the recogniser cannot follow; the rest navigate or change data.
- Handlers really run: f3 `btn_delete` calls `foundset.deleteAllRecords()` on `db:/servoy_test/orders`, and the `onShow` of f1, f2 and f4 deletes or reseeds data. f6_header_checkbox_click2 `button_31` shows a form (`grid_header_checkbox_not_working`) that does not exist in the solution, and the case still passes.
- The generator never reads form-level events (`onShow`, `onLoad`, `onRecordSelection`, ...).

### Follow-ups (outside this case)

- Found while testing, now tracked: **SVY-21663** (MCP: `listFormTests` / `runAllFormTests`; `listE2ETests` reads only `cypress/e2e`, so agents report "no form tests" while the six specs in `cy-form` pass in the IDE) and **SVY-21665** (skill4servoy form-test guidance: file type, mocking wording, routing, and whether form tests get their own agent). Both relate to this case; SVY-21665 depends on SVY-21663.
- Gestures for grid and component spec events (`onDoubleClick`, `onTabClicked`, cell and header events).
- Form-level events, and effects through form variables bound with `%%var%%`.
- A stub mechanism for off-form effects, and a guard for destructive handlers.
- Jira link between SVY-21517 and SVY-21573 (checked in Jira: SVY-21517 links only to SVY-21663 and SVY-21665).
- The skill's E2E-Tester asks the agent to write richer tests as `.cy.ts` in `servoy_test\jenkins-custom`, a folder the IDE runner does not read. Moving that coverage to `.cy.js` in `workspace\jenkins-custom\...\cy-form` was proposed and set aside.

### Before committing

- Remove every line marked `// SVY-21517-TRACE` in servoy-eclipse and Servoy-Copilot (temporary `System.out` tracing added for the test run), then re-run compile and the plain tests.
- Stage only the files of this case in each repo; each repo has unrelated modified files (`servoy.properties`, launch files, `angular.json`, ...).
- Commit order: servoy-eclipse first (Servoy-Copilot needs the new `FormSpecModel`), then Servoy-Copilot, then skill4servoy (already committed). Ask before every commit.