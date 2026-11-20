# Spec: SVY-21727 — AssertionError in TypeInferencerVisitor.initializeVariables for `let` declarations

## 1. Goal
With assertions enabled (`-ea`, as in the copilot_skilltest tycho-surefire run), building a solution whose scope file uses `let` in a for-loop head crashes the type inferencer with `java.lang.AssertionError` in `TypeInferencerVisitor.initializeVariables`. The solution then fails to load and JSUnit runs 0 tests. Without `-ea` nothing crashes, but loop `let` variables silently lose their variable metadata (no `JSVariable`, no `ReferenceKind`, no declaration location, no unused-variable tracking). The fix registers for-loop-head `let` bindings on the loop's scope in the parser and has the inferencer create them when it enters the loop scope. The `assert result.exists()` invariant stays.

## 2. Background

### 2.1 Where the code lives
All changes are in the sibling repo **`C:\Users\vosti\git_master\org.eclipse.dltk.javascript`** (Servoy's DLTK JavaScript fork). Current branch: **`servoy`** (HEAD `ecdb51ea`, clean working tree). `origin/lts_2026` exists too, so a backport/cherry-pick there is expected. Nothing changes in servoy-eclipse except this spec.

### 2.2 How a declaration reaches the inferencer
1. The ES6 parser `org.eclipse.dltk.javascript.parser.rhino.Parser` registers declarations via `SymbolTable.add(name, kind, declaration)`, which calls `JSScope.addDeclaration(...)` on the owning scope node (`Script`, `FunctionStatement`, `StatementBlock`, or `AbstractForStatement`, which implements `JSScope`). This parser is used whenever `JavascriptParserPreferences.useES6Parser()` is true (the default) via `JavaScriptParserUtil`.
2. `TypeInferencerVisitor.handleDeclarations(JSScope)` (line ~2477) runs when a scope is entered and calls `createVariable(...)` for every `VariableBinding` declared on that scope. This sets `IReferenceAttributes.VARIABLE`, kind and location, and triggers model-builder callbacks.
3. When the statement is visited, `initializeVariables(IVariableStatement)` (line ~2929) does `collection.getChild(varName)` and `assert result.exists()`.

Plain `let`/`const` at script, function and block level work: `Parser.defineSymbol` (line ~2851, `Token.LET` branch) adds them to `blockScopes.peek()`, and `visitScript`, `visitFunctionBody` and `visitStatementBlock` call `handleDeclarations`.

### 2.3 The gaps (for-loop heads only)
- **Classic `for (let i = 0; ...; ...)` (parser bug).** `forLoopInit` → `variables(tt, ..., false)` with `inForInit = true` → `defineSymbol(..., ignoreNotInBlock = true, ...)` returns early for `Token.LET` (`if (ignoreNotInBlock) return; // is for loop init`). `Parser.forStatement` (lines ~1982–2002) only re-adds `LetStatement` bindings to the loop scope inside `if (isForIn || isForOf)`. So `ForStatement.getDeclarations()` stays empty. `TypeInferencerVisitor.visitForStatement` (line ~1309) then visits `getInitial()` → `visitLetStatement` → assert fails.
- **`for (let k in obj)` (inferencer bug).** The parser declares `k` on the `ForInStatement` (covered by `TestRhinoParser.testForInLet`). `visitForInStatement` (line ~1294) enters a nested collection but never calls `handleDeclarations(node)` → assert fails.
- **`for each (let x in arr)` (inferencer bug).** `ForEachInStatement extends ForInStatement`. The parser handles it in the `isForIn` branch, so the binding is declared on the node. `visitForEachInStatement` (line ~1227) creates no nested scope and does not handle declarations → assert fails.
- **`for (let x of arr)` already works.** `visitForOfStatement` (line ~3271) does `createNestedScopeValueCollection` + `enterContext` + `handleDeclarations(node)`. It is the template for the fix.

### 2.4 Git history
From the triage report (`docs/SVY-21727-triage.md`), not re-dug:
- `initializeVariables` with the shared `assert result.exists()`: **9b3ff8f2** (SVY-18611, destructuring, 2025-06-06). It merged per-kind bodies that each already had the assert. The let assert itself dates from **a3ce7d78** (SVY-18097, let support, 2023-07-13). The assert is not the regression.
- `if (ignoreNotInBlock) return; // is for loop init` and the for-in/of-only re-registration: **d811f1b7** (SVY-19677, 2024-10-14). Before that, the for-init bindings were added to the loop scope for all loop kinds. This commit restricted it to `isForIn || isForOf`, which dropped ordinary for-loops. Adjusted for destructuring in **e4c66739** / **8f99c888** (SVY-20811, 2026-02-19).
- `handleDeclarations(node)` in `visitForOfStatement`: **2a2d218b** (SVY-19260, 2024-05-28). `visitForStatement`/`visitForInStatement` never got the same treatment.

## 3. Design

### 3.1 Parser: register `let` bindings for every loop kind
In `Parser.forStatement`, move the `LetStatement` registration out of the `isForIn || isForOf` guard so it also runs for ordinary for-loops. Keep the "only one variable" check (`msg.mult.index`) for for-in/of only. Ordinary for-loops legally allow `for (let i = 0, j = 10; ...)`, so all bindings and all their variable names (destructuring included, via `binding.getVariableNames()`) must be added to `blockScopes.peek()`, which at that point is the `SymbolTable` of the `ForStatement`. Target shape:

```java
if (init instanceof IVariableStatement vs) {
    boolean multipleNotAllowed = (isForIn || isForOf) && vs.getVariables().size() > 1;
    if (multipleNotAllowed) {
        reportError("msg.mult.index");
    } else if (init instanceof LetStatement) {
        vs.getBindings().forEach(binding ->
            binding.getVariableNames().forEach(name ->
                // duplicates already checked in variables(..)
                blockScopes.peek().add(name, SymbolKind.LET, binding)));
    }
}
if (isForOf && isForEach) {
    reportError("msg.invalid.for.each");
}
```

The registration still happens before the body is parsed (before `enterLoop(pn)`), so body-level redeclaration checks see the loop binding exactly as they already do for for-in/of. `var` in a for-head is unchanged. It is still declared on the function/script scope via `defineSymbol`'s `Token.VAR` branch. `defineSymbol` itself is not changed.

### 3.2 Inferencer: create loop-scope declarations on entry
In `TypeInferencerVisitor` (`plugins/org.eclipse.dltk.javascript.core/src/org/eclipse/dltk/internal/javascript/ti/TypeInferencerVisitor.java`):
- `visitForStatement`: call `handleDeclarations(node)` right after `enterContext(collection)`.
- `visitForInStatement`: same.
- `visitForEachInStatement`: wrap the body in a nested scope like `visitForInStatement` (`createNestedScopeValueCollection(peekContext())`, `enterContext`, `handleDeclarations(node)`, and `leaveContext()` at the end). The existing item/iterator/`inferIteratorType`/body logic stays as it is.

`handleDeclarations` on a loop node only sees that node's own declarations, which are `let` bindings from the head. `var` bindings live on the enclosing function/script scope and are already created there, so nothing gets created twice. `CompletionVisitor` overrides `handleDeclarations` and the for visitors and delegates to `super`, so it picks this up automatically. `TypeInfoValidator.ValidationVisitor.visitForStatement` calls `super` first, so the const-step check is unaffected.

### 3.3 Keep the assert
`assert result.exists()` in `initializeVariables` stays unchanged. It is the invariant that caught this bug. No lazy-create fallback is added (triage approach 2 was rejected as masking).

### 3.4 Behaviour change to expect
Loop-head `let` variables now get full metadata. That means the validator's unused-variable tracking now covers them. An unused `for (let i ...)` may produce an `UNUSED_VARIABLE` warning where none appeared before. That warning is correct. "Hides" checks (`validateHidesByVariable`) already skip `LetStatement`. Hover, navigation and JSDoc type handling for these variables improve.

## 4. Implementation plan
All paths are relative to `C:\Users\vosti\git_master\org.eclipse.dltk.javascript` (branch `servoy`).

1. **`plugins/org.eclipse.dltk.javascript.parser/src/org/eclipse/dltk/javascript/parser/rhino/Parser.java`**, method `forStatement` (~lines 1982–2002): restructure the post-header block as in §3.1 so `LetStatement` bindings are added to `blockScopes.peek()` for ordinary for-loops too. Keep `msg.mult.index` limited to for-in/of and `msg.invalid.for.each` unchanged.
2. **`plugins/org.eclipse.dltk.javascript.core/src/org/eclipse/dltk/internal/javascript/ti/TypeInferencerVisitor.java`**:
   - `visitForStatement` (~1309): add `handleDeclarations(node);` after `enterContext(collection);`.
   - `visitForInStatement` (~1294): add `handleDeclarations(node);` after `enterContext(collection);`.
   - `visitForEachInStatement` (~1227): add the nested collection, `enterContext`, `handleDeclarations(node)`, and `leaveContext()`.
3. **Parser tests**: `tests/org.eclipse.dltk.javascript.parser.tests/src/org/eclipse/dltk/javascript/parser/tests/TestRhinoParser.java` (JUnit 4 `@Test`, uses `getScriptv4(...)` = rhino parser):
   - new `testForLet_declaredOnLoopScope`: `for (let i = 1; i < 3; i++) {}` → the `ForStatement` has exactly 1 declaration (`i`), and the script has 0.
   - new `testForLetMultiple_declaredOnLoopScope`: `for (let i = 0, j = 10; i < j; i++) {}` → 2 declarations on the `ForStatement`, and no `msg.mult.index` problem reported.
   - new `testForVar_notOnLoopScope`: `for (var i = 0; i < 3; i++) {}` → 0 declarations on the `ForStatement`, 1 on the script.
   - update `testConst_scope`: additionally assert `for_.getDeclarations().size() == 1` (the `y`). The existing function and for-body counts stay 1.
   - keep `testForInLet` and `testForOfLet` green, unchanged.
4. **Inferencer/validator tests**: new class `tests/org.eclipse.dltk.javascript.core.tests/src/org/eclipse/dltk/javascript/core/tests/typeinference/LetLoopTypeInferenceTests.java`. The existing `TypeInferenceTests` uses the old ANTLR `org.eclipse.dltk.javascript.parser.JavaScriptParser`, which never produces the rhino `LetStatement` scope shape, so a separate class is needed. It parses with `new org.eclipse.dltk.javascript.parser.rhino.JavaScriptParser().parse(new ModuleSource(code), reporter)` (fail on parse errors) and runs `new TestTypeInferencer2().doInferencing(script)`. Use JUnit 3 `TestCase`, like the neighbours, or JUnit 4 with a `JUnit4TestAdapter` registration. Cases:
   - script-level `for (let i = 0; i < 3; i++) { var s = i; }`: no `AssertionError`, and `s` resolves to a Number type. Also capture the loop variable's reference (via a small `TypeInferencerVisitor` subclass or a `@type`-free body assignment) and assert it has `IReferenceAttributes.VARIABLE` set, plus a location matching the `i` identifier offset.
   - function-level `function f(){ for (let i = 0; i < 3; i++) {} }`: no error.
   - `for (let k in obj) { }`: no error, and `k` is typed String inside the loop.
   - `for each (let x in [1,2]) { }`: no error.
   - `for (let x of [1,2]) { }`, plain `let a = 1;`, `const c = 2;`, and block-level `{ let b = 1; }`: no error (regression guards).

   The `AssertionError` checks only mean something with `-ea`. Both repo launch configs (`JS-Core-Tests.launch`, `JS-Parser-Tests.launch`) and tycho-surefire (assertions on by default) provide it. As a guard, add a test that asserts `TypeInferencerVisitor.class.desiredAssertionStatus()` is true, so the suite fails loudly if `-ea` is missing.
5. **Validator regression**: in `tests/org.eclipse.dltk.javascript.core.tests/src/org/eclipse/dltk/javascript/core/tests/validation/TypeInfoValidationTests.java`, add a test that calls `validate("function f(){ for (let i = 0; i < 3; i++) { application.output(i); } for (let k in {}) { } }")` (or similar, without Servoy-specific globals) and asserts an empty problem list. This exercises the exact stack from the ticket (`TypeInfoValidator$ValidationVisitor` → `visitLetStatement`). `TestBuildContext` → `JavaScriptValidations.parse` → `JavaScriptParserUtil.parse`, which uses the rhino parser when the `ES6_Parser` preference is true (the default). In the test, first verify, or explicitly set `new JavascriptParserPreferences().useES6Parser(true)`, so it doesn't silently fall back to the old parser.
6. Register the new class in `tests/org.eclipse.dltk.javascript.core.tests/src/org/eclipse/dltk/javascript/core/tests/AllTests.java` (`suite.addTestSuite(...)`, or `JUnit4TestAdapter` for JUnit 4). Maven/Tycho only runs `**/AllTests.java`. Note that `TestRhinoParser` is not currently registered in the parser project's `AllTests` (only `DeclarationScopeTest`, `ObjectInitializerTests` and `SimpleJSDocParserTests` are adapted). Either register it with `new JUnit4TestAdapter(TestRhinoParser.class)` or put the new parser tests in an already-registered class. Registering it is preferred, but first check that the whole class is green.
7. Check compile errors (`tools["eclipse-ide"].getCompilationErrors`) and run the tests (see §4.1).

### 4.1 Test projects and how to run them
| Project | Path | Framework | Runner |
|---|---|---|---|
| `org.eclipse.dltk.javascript.parser.tests` | `tests/org.eclipse.dltk.javascript.parser.tests` | `Require-Bundle: org.junit` (JUnit 4; `TestRhinoParser` uses `org.junit.Test`) | PDE JUnit plug-in test: `JS-Parser-Tests.launch` (`AllTests`, `-ea`). `TestRhinoParser` itself has no OSGi need and can also run as plain JUnit 4 (`junitVersion: "4"`) |
| `org.eclipse.dltk.javascript.core.tests` | `tests/org.eclipse.dltk.javascript.core.tests` | JUnit 3/4 (`org.junit` 4.10+, `TestCase` style) | PDE JUnit plug-in test: `JS-Core-Tests.launch` (`AllTests`, JUnit 4 kind, `-ea`). Needs OSGi: `JavaScriptParserPlugin` preferences, `TypeInfoModelLoader` |

Maven: `mvn verify` in the repo's `tests/` reactor (tycho-surefire, `**/AllTests.java`). The servoy-eclipse `plainjunit`/`integrationpde` split does **not** apply in this repo, so follow its existing layout.

**Workspace status:** the production projects `org.eclipse.dltk.javascript.parser` and `org.eclipse.dltk.javascript.core` are open in the Eclipse workspace, so they can be edited via `eclipse-coder`. The test projects `org.eclipse.dltk.javascript.parser.tests` and `org.eclipse.dltk.javascript.core.tests` are **not** imported, only some of their `workspace/` fixture folders (`search`, `Joose`, `rename`, `objectLiteralCompletion`). Import them (`tools["eclipse-ide"].openProject`/import) before creating or running tests through the Eclipse MCP tools, or run them via Maven.

## 5. Acceptance criteria
- [ ] With `-ea`, inferring/validating a script with `for (let i = 1; i < 3; i++) {}` at script level and inside a function throws no `AssertionError`.
- [ ] Same for `for (let k in obj) {}` and `for each (let x in arr) {}`.
- [ ] The rhino parser declares for-head `let` bindings on the `ForStatement` scope, including multiple bindings (`let i = 0, j = 1`) and destructuring names. They are not declared on the script/function scope.
- [ ] `for (var i = 0; ...)` still declares `i` on the enclosing function/script scope, and the `ForStatement` has no declarations.
- [ ] For-head `let` variables get `IReferenceAttributes.VARIABLE` and a declaration location after inference. The for-in `let` item is typed String.
- [ ] `assert result.exists()` in `initializeVariables` is unchanged.
- [ ] Existing `TestRhinoParser` tests (`testForInLet`, `testForOfLet`, `testConst_scope` with the added assert) and the existing `org.eclipse.dltk.javascript.core.tests` `AllTests` suite stay green.
- [ ] New tests fail when either the parser change or the inferencer change is reverted.

## 6. Out of scope
- Removing, weakening, or guarding the assert (the ticket's original proposal).
- A lazy-create fallback in `initializeVariables` (triage approach 2).
- `const` in for-heads. `forLoopInit` only handles `VAR`/`LET`, so it is not affected.
- The legacy ANTLR parser (`org.eclipse.dltk.javascript.parser.JavaScriptParser`).
- Changes in servoy-eclipse or the copilot_skilltest setup.
- The backport to `lts_2026` (a follow-up cherry-pick after merge on `servoy`).

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| The original `orders.js` isn't attached. Assumed the trigger was `for (let i = 1; ...)`. Confirm with the reporter, or by rerunning the skilltest after the fix. | Reporter / implementer | open |
| New `UNUSED_VARIABLE` warnings for unused loop-head `let`s: acceptable, or should they be suppressed? (They are correct JS-wise.) | Product / DLTK maintainer | open (assumed acceptable) |
| Register `TestRhinoParser` in the parser project's `AllTests` so CI runs it? It isn't run by Maven today. | Implementer | open (recommended if the class is green) |
| Backport to `lts_2026`: same commit cherry-picked? | Release owner | open |
