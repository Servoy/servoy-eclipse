# Triage Report — SVY-21727

**Verdict:** PROCEED

## Reported problem
With assertions enabled (`-ea`, as in the copilot_skilltest tycho-surefire argLine), building a solution whose scope file contains a `let` declaration crashes the type inferencer:

`java.lang.AssertionError at TypeInferencerVisitor.initializeVariables -> visitLetStatement -> ASTVisitor -> TypeInfoValidator$ValidationVisitor`

The solution then fails to load cleanly and JSUnit runs 0 tests. With a normal Developer launch (no `-ea`) the assert is skipped and nothing visibly crashes.

Proposed solution in the ticket: narrow or guard the `assert result.exists()` for let/const, and make sure let/const still get proper inference.

## Root-cause assessment
The fix is in the **`org.eclipse.dltk.javascript`** repo (branch `servoy`, also on `lts_2026`), not in servoy-eclipse.

How a declaration reaches the inferencer:
1. The parser (`org.eclipse.dltk.javascript.parser.rhino.Parser`, the ES6 parser used by `JavaScriptParserUtil` when `useES6Parser()` is on) calls `SymbolTable.add(name, kind, declaration)`, which calls `JSScope.addDeclaration(...)` on the owning scope node (Script, FunctionStatement, StatementBlock, or `AbstractForStatement`).
2. `TypeInferencerVisitor.handleDeclarations(JSScope)` runs when a scope is entered and calls `createVariable(...)` (with `context.createChild`) for each declared binding. This sets `VARIABLE`, kind and location.
3. When the statement itself is visited, `initializeVariables(IVariableStatement)` does `collection.getChild(varName)` and `assert result.exists()`.

The assert is a valid invariant: every declared binding must have been created by step 2. It fires only when a `let` binding was never declared on a scope that the inferencer handles. The ticket's explanation ("let is not hoisted the same way") is too broad. A plain `let x = 1;` at script, function or block level **is** registered:
- `Parser.defineSymbol` case `Token.LET` → `definingScope.add(...)`, which is `blockScopes.peek()` or the script scope. `getParent()` at that point is the `VariableDeclaration`, so the "not in block" error branch does not trigger.
- `visitScript`, `visitFunctionBody` and `visitStatementBlock` all call `handleDeclarations`.

The gaps are specific to **`let` in for-loop heads**:

1. **Classic `for (let i = 1; ...; ...)` — the binding is never declared (parser bug).**
   - `forLoopInit` calls `variables(tt, ..., false)` with `inForInit = true`, so `defineSymbol(..., ignoreNotInBlock = true, ...)` returns early for `Token.LET` (`if (ignoreNotInBlock) return; // is for loop init`, Parser.java ~2870).
   - In `Parser.forStatement` the bindings are only re-added to the loop's scope for `isForIn || isForOf` (Parser.java ~1982–1997). The ordinary for-loop branch never adds them, so `ForStatement.getDeclarations()` stays empty.
   - `TypeInferencerVisitor.visitForStatement` (~1309) creates a nested collection but doesn't call `handleDeclarations(node)`. It then visits `node.getInitial()` → `visitLetStatement` → `getChild("i").exists() == false` → **AssertionError**.
   - Existing test `TestRhinoParser.testConst_scope` (`for (let y = 1; y<10; y++)`) shows this indirectly: it asserts the function has 1 declaration (`const x`) and the for body has 1 (`const x`). `y` is declared nowhere.

2. **`for (let e in obj)` — declared by the parser, but the inferencer never handles it.**
   - The parser adds the binding to the `ForInStatement` scope (`testForInLet` asserts `statementv4.getDeclarations().size() == 1`).
   - `TypeInferencerVisitor.visitForInStatement` (~1294) enters a nested collection but **doesn't call `handleDeclarations(node)`**. It visits the item → `visitLetStatement` → assert fires.
   - `visitForOfStatement` already does this correctly (`handleDeclarations(node)` added in 2a2d218b, SVY-19260). `visitForEachInStatement` doesn't create a scope or handle declarations either; it only matters for the rare `for each (let x in ...)`.

The ticket's trigger, "`let i=1`" in an agent-generated `orders.js`, very likely was `for (let i = 1; ...)`. The file is not attached, so this is inferred, not confirmed. Both loop forms point to the same fix area, so the investigation converges.

**Why it "works" without `-ea`:** `ChildReference.createValue()` lazily creates the child in the current (loop) collection on the first `setAttribute`/`setValue`. So assignment inference still happens, but `createVariable` was skipped. The binding gets no `IReferenceAttributes.VARIABLE` (`JSVariable`), no `ReferenceKind.LOCAL`, no declaration `ReferenceLocation`, no model-builder/listener callbacks, and no unused-variable tracking. That means silently degraded hover, navigation, validation and JSDoc type handling for loop `let` variables even in normal Developer runs. It is a real (latent) bug, not just an assertion artifact.

Not runtime-verified: I did not run a reproduction. The conclusions come from reading the parser and inferencer code paths and the existing parser tests.

## Ticket premise check
- The problem is real and in our code (the DLTK fork), not user-side.
- **The proposed fix (guard or narrow the assert for let/const) is the wrong approach.** The assert correctly detects a missing declaration. Removing it would hide the symptom and keep the degraded inference described above. The ticket's second bullet ("check how var registers its child ... do the equivalent for let/const") is the right direction.
- `const` is not affected in these paths. `forLoopInit` only handles `VAR`/`LET`, and `const` at statement level is declared via the `Token.CONST` branch.

## Approaches considered
1. **Register for-head `let` bindings properly (recommended).**
   - Parser: in the ordinary for-loop branch of `Parser.forStatement`, add `LetStatement` bindings to `blockScopes.peek()` (the `ForStatement` scope), the same way the for-in/for-of branch already does. The simplest option is to apply the existing loop to all three branches.
   - Inferencer: call `handleDeclarations(node)` after `enterContext(collection)` in `visitForStatement` and `visitForInStatement`, matching `visitForOfStatement`. Consider the same for `visitForEachInStatement`, which also needs a nested scope.
   - Pros: fixes the crash and restores full variable metadata. Consistent with existing for-of handling. Small and local.
   - Cons: touches the parser and the inferencer. `TypeInfoValidator.createVariable` will now see these bindings, so new "unused variable" or "hides" warnings may appear for loop `let`s. Those warnings are correct, but they are a behaviour change. `validateHidesByVariable` already skips `LetStatement`. Existing parser tests that count declarations (e.g. `testConst_scope`) need checking.
2. **Lazily create the binding in `initializeVariables`:** if `!result.exists()`, call `createVariable(...)` there.
   - Pros: one-place defensive fix that also covers any other undeclared-let path.
   - Cons: masks parser/scope bugs. The binding is created at visit time instead of scope entry, so forward references inside the loop scope would differ. Duplicates the declaration logic.
3. **Remove or guard the assert (ticket's proposal).**
   - Pros: trivial, and stops the `-ea` crash.
   - Cons: leaves loop `let` variables without `JSVariable`, kind or location, and hides future declaration bugs.
4. **No code change.**
   - Pros: no risk. Production runs without `-ea`.
   - Cons: CI with `-ea` (skilltest) keeps failing on common modern JS (`for (let i...)`), and the latent degraded inference stays. Not acceptable.

## Recommendation
**PROCEED with approach 1** in `org.eclipse.dltk.javascript` (branch `servoy`, backport to `lts_2026`):
- `Parser.forStatement`: register `LetStatement` bindings on the loop's `SymbolTable` for ordinary for-loops too.
- `TypeInferencerVisitor.visitForStatement` / `visitForInStatement` (and optionally `visitForEachInStatement`): call `handleDeclarations(node)` after entering the nested collection.
- Keep `assert result.exists()`.
- Tests, run with `-ea` (the tycho-surefire default; the Eclipse JUnit launch also has `-ea` by default):
  - parser test: `ForStatement.getDeclarations()` contains the `let` binding for `for (let i = 1; i < 3; i++) {}`;
  - inferencer/validator test: script-level and function-level `for (let i = 0; ...)` and `for (let k in obj)` no longer throw, and `i` resolves with kind LOCAL/GLOBAL and the correct declaration location;
  - regression checks for plain `let`/`const` at script/function/block level and `for (let x of ...)`.

Alternatives: approach 2 as an extra safety net is acceptable but not a replacement. Approach 3 is not recommended.

## Git history findings
- `initializeVariables` with its shared `assert result.exists()` came from **9b3ff8f2** (emera, 2025-06-06, *SVY-18611 Support Array/Object destructuring*). It merged the separate var/let/const bodies, each of which already had the same assert. The let-version assert dates from **a3ce7d78** (emera, 2023-07-13, *SVY-18097 add support for the let statement*). The assert itself is not a regression. The for-loop gap predates it.
- `if (ignoreNotInBlock) return; // is for loop init` and the for-in/of-only re-registration came from **d811f1b7** (emera, 2024-10-14, *SVY-19677 ... fix declarations for forLoop*). The earlier code added the for-init binding to the loop scope for all loop kinds and all var kinds; this commit restricted it to `LetStatement` inside the `isForIn || isForOf` block, which drops ordinary for-loops. Updated for destructuring in **e4c66739** / **8f99c888** (SVY-20811, 2026-02-19).
- `handleDeclarations(node)` was added to `visitForOfStatement` only in **2a2d218b** (Johan Compagner, 2024-05-28, *SVY-19260 ES6 parser failing to infer type*). `visitForStatement` and `visitForInStatement` never got the equivalent.
- No dependency or target-platform change is involved.
