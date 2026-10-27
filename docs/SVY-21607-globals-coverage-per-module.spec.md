# Spec: SVY-21607 — LCOV coverage of global functions attributed per module

| Field        | Value                                                        |
|--------------|--------------------------------------------------------------|
| Jira         | [SVY-21607](https://servoy-cloud.atlassian.net/browse/SVY-21607) |
| Type         | Bug                                                          |
| Priority     | Major                                                        |
| Fix Version  | 2026.12                                                     |
| Related      | [SVY-20918](https://servoy-cloud.atlassian.net/browse/SVY-20918) (Code coverage reporting in unit test), [SVY-21278](https://servoy-cloud.atlassian.net/browse/SVY-21278) (uncovered-lines), [SVY-21131](https://servoy-cloud.atlassian.net/browse/SVY-21131) (JSON coverage writer) |
| Triage       | `docs/SVY-21607-triage.md` (verdict: PROCEED, approach 1)   |

## 1. Goal

When unit tests of a solution with modules run with coverage enabled, the generated LCOV report must attribute each global function's coverage to the `globals.js` of the **module that defines it** — emitting one `SF:<module>/globals.js` record per module — exactly as named scopes are already attributed to their owning module. Today every global function from every module is collapsed into a single `SF:<activeSolution>/globals.js` record keyed by the active solution, which has no `globals.js` of its own. This makes coverage tools (SonarQube) treat every real module `globals.js` as an unresolved path and report 0% coverage, forcing users to exclude all `globals.js` files from coverage.

## 2. Background

### 2.1 Where the LCOV report is written

The LCOV report is produced by `ServoyJSUnitTestRunner.suite()`'s `tearDown()` in the **`test` repo** — `C:\Users\merae\git\test\j2db_test\src\com\servoy\automation\jsunit\smartclient\ServoyJSUnitTestRunner.java`, the `"scopes"` branch (lines ~226–270). This `servoy-eclipse` repo only hosts the hit/reachable-line *collection* (`com.servoy.eclipse.jsunit`), which does **not** change for this fix. This matches the prior SVY-21278 spec, which already located the LCOV writer in `j2db_test`.

### 2.2 Why global coverage is mis-attributed

The hit data from `SolutionJSTestSuite.getLineNumbers()` is keyed as `type → scopeName → funcName → line → hits`. The collection side records every global hit under the single literal scope name `"globals"` (`ScriptVariable.GLOBAL_SCOPE = "globals"`), because at runtime all modules contribute to one shared global scope. So the `"scopes"` bucket contains a single `"globals"` entry holding the global functions of **all** modules, with no per-module key.

When writing LCOV, the current `"scopes"` branch resolves the file path for each scope name via:

```java
Pair<String, IRootObject> solutionPair = importRunner.getApplication().getFlattenedSolution()
    .getScopes().stream().filter(s -> s.getLeft().equals(scopeName)).findFirst().get();
Solution solution = (Solution)solutionPair.getRight();
builder.append("SF:").append(solution.getName()).append("/").append(scopeName).append(".js");
```

For a *named* scope `getScopes()` maps 1:1 to the module that declares it, so named scopes land in the right file. For the global scope, `FlattenedSolution.getScopes()` → `addGlobalsScope()` unconditionally maps `"globals"` to the active (main) solution, so all global coverage is attributed to `SF:<activeSolution>/globals.js`.

### 2.3 The per-function module information exists

Each global function's owning module is recoverable at write time from the persist model: `FlattenedSolution.getScriptMethod("globals", funcName)` returns the `ScriptMethod`, and `ScriptMethod.getRootObject()` returns the owning `Solution` (the module). `getRootObject().getName()` is the module name. This is the same persist API named scopes implicitly rely on, so the writer can group the `"globals"` bucket's functions by module and emit one `SF:<module>/globals.js` block per module.

Note: `FlattenedSolution.getScriptMethods(scopeName, false)` and `Solution.getScriptMethods(scopeName, false)` both filter only by scope name, not by module — so the existing `FNF:` computation (`Utils.asList(solution.getScriptMethods(scopeName, false)).size()`) would count global functions across all modules and must be replaced by a per-module count for the global scope.

### 2.4 Git history (from triage)

- The LCOV writer and the `getScopes().stream().filter(...)` scope-to-solution resolution were introduced by SVY-20918 (`test` commit `bac749e2`, 2026-06-06). The matching hit-collection changes landed in this repo in `e66b16285b` (SVY-20918, 2026-04-03).
- SVY-21278 (`test` commits `a730ee6`, `f590990`, 2026-07-27) added uncovered-line (`DA:line,0`) and `LF`/`LH` support to the same method.
- `addGlobalsScope()` mapping `"globals"` → `mainSolution` predates and is unrelated to coverage; the coverage writer's reliance on it for file-path resolution is the incidental cause here, not a deliberate coverage design. Fixing the writer reverts no intentional decision.

## 3. Design

### 3.1 Approach (approved: triage approach 1)

Inside the `"scopes"` branch of `tearDown()`, special-case the global scope (`scopeName.equals(ScriptVariable.GLOBAL_SCOPE)` / `"globals"`):

- For a **named** scope: keep the existing behaviour unchanged — resolve the single owning solution via `getScopes()`, emit one `SF:<module>/<scope>.js` block.
- For the **global** scope: do not emit a single block. Instead, group the functions in the `"globals"` bucket by their owning module, then emit one `SF:<module>/globals.js` block per module, each with its own `FNF`/`FNH`/`FN`/`FNDA`/`DA`/`LF`/`LH`.

### 3.2 Grouping global functions by module

For the `funcMap` (`funcName → line → hits`) of the `"globals"` bucket, build `Map<String, List<String>> moduleToFuncs` (module name → function names), or equivalently `Map<Solution, List<String>>`:

For each `funcName` in `funcMap.keySet()`:
1. Resolve `ScriptMethod sm = flattenedSolution.getScriptMethod(ScriptVariable.GLOBAL_SCOPE, funcName)`.
2. If `sm != null` and `sm.getRootObject() instanceof Solution`, the module is `sm.getRootObject().getName()`.
3. **Fallback:** if `sm` is `null` or its root object cannot be resolved to a `Solution`, attribute the function to the **active solution** (as today) and `log.warn(...)` the function name so no coverage is silently lost. Use a stable sentinel (the active solution's name) as the group key for these.

### 3.3 Emitting one block per module

Iterate the module groups in a deterministic order (sort module names so output is stable). For each module group, reproduce the existing per-scope emission logic, scoped to just that module's functions:

- `SF:<module>/globals.js` — `module` is the group's module name; file suffix `SolutionSerializer.JS_FILE_EXTENSION`. (`globals` = `ScriptVariable.GLOBAL_SCOPE`.)
- `FNF:` — the number of global functions **declared in that module**. Compute per module, not for the whole scope. Options, in order of preference:
  - Resolve the module `Solution` persist and count `Utils.asList(moduleSolution.getScriptMethods(ScriptVariable.GLOBAL_SCOPE, false)).size()` — `Solution.getScriptMethods(scopeName,...)` returns only that one solution's methods (it iterates `getAllObjectsAsList()` of that `Solution`), so this is a true per-module count.
  - To get the module `Solution`: take it from a resolved `ScriptMethod.getRootObject()` in the group (cast to `Solution`), or look it up by name among the flattened solution's modules.
  - For the fallback group (unresolved functions attributed to the active solution), `FNF` is less meaningful; use the count of global methods declared in the active solution, consistent with the pre-fix single-block behaviour.
- `FNH:` — the number of **hit** functions in this module's group (the size of that group's function sub-map), not `funcMap.size()`.
- `FN`/`FNDA`/`DA`/`LF`/`LH` — exactly as the existing loop, but iterating only the functions of this module's group. Keep the SVY-21278 union-of-hit-and-reachable-lines logic and the `findReachableForScope`/`findReachableLinesForFunction` calls unchanged; they key off `scopeName = "globals"` which is the same for all modules (reachable-line resolution is scope-name based, not module based — see 3.4).
- `end_of_record` per module block.

### 3.4 Reachable-line resolution is unaffected

`findReachableForScope(reachable, "scopes", "globals")` and `findReachableLinesForFunction(...)` resolve reachable lines by scope name / function name, not by module. The global scope name is `"globals"` for every module, and function names are unique within the shared global scope, so the same reachable-line lookups continue to work when the DA records are split across module blocks. Do **not** change these helpers.

### 3.5 Forms branch unchanged

The `"forms"` branch already resolves each form to its owning module via `form.getRootObject().getName()` and is correct. Leave it untouched.

### 3.6 Out of scope: JSON writer

`com.servoy.eclipse.jsunit`'s `JSUnitCoverageWriter` (this repo, SVY-21131) writes a JSON coverage report with the same scope-name-only keying and would show the same module-less grouping. It is a different output format with different consumers and is **out of scope** for this ticket. Flag it to the team as a candidate follow-up.

## 4. Implementation plan

**All code changes are in the `test` repo, in a single file:**
`C:\Users\merae\git\test\j2db_test\src\com\servoy\automation\jsunit\smartclient\ServoyJSUnitTestRunner.java` — the `"scopes"` branch of the anonymous `TestSetup.tearDown()` (lines ~226–270). This is the same file SVY-21278 and SVY-20918 edited. **No change is needed in `servoy-eclipse`.**

1. In `tearDown()`, inside the `if (lineHits != null)` block for the `"scopes"` bucket, split the per-scope loop into two paths based on `scopeName`:
   - If `scopeName` is not `ScriptVariable.GLOBAL_SCOPE` → keep the existing code verbatim (named scope, single `SF:<module>/<scope>.js` block).
   - If `scopeName` equals `ScriptVariable.GLOBAL_SCOPE` → run the new per-module emission (steps 2–5).

2. Build a module grouping for the `"globals"` `funcMap`:
   - `Map<String, List<String>> moduleToFuncs` keyed by module name, preserving a reference to each module's `Solution` (e.g. a parallel `Map<String, Solution>`), by resolving each `funcName` through `importRunner.getApplication().getFlattenedSolution().getScriptMethod(ScriptVariable.GLOBAL_SCOPE, funcName)` and `getRootObject()`.
   - Add the active-solution fallback (with a `log.warn`) for any `funcName` whose `ScriptMethod` or `Solution` root cannot be resolved.

3. Iterate the module groups in sorted module-name order, and for each group emit a complete LCOV record:
   - `SF:<module>/globals.js`
   - `FNF:` = per-module declared global-method count (`moduleSolution.getScriptMethods(GLOBAL_SCOPE, false)` size).
   - `FNH:` = size of this group's hit-function set.
   - For each function in the group (sorted by first line, as today): `FN`, `FNDA`, and the union-of-lines `DA` records (reuse `findReachableForScope`/`findReachableLinesForFunction` with `scopeName = "globals"`), accumulating per-module `LF`/`LH`.
   - `LF:` / `LH:` / `end_of_record` per module.

4. Factor the shared per-function DA/FN/FNDA/LF-LH emission so the named-scope path and each global module block use the same code (optional but recommended to avoid duplication — e.g. a private helper `appendFunctionCoverage(builder, funcMap, subsetFuncNames, reachable, reachableForScope, type, scopeName, lfLh)`).

5. Add any required imports (`java.util.List`, `java.util.Map`, `java.util.TreeMap`, etc. — most are already present; `Solution`, `ScriptVariable` from `com.servoy.j2db.persistence`).

6. Build the `test` repo / `j2db_test` and confirm it compiles.

## 5. Acceptance criteria

- [ ] For a solution A with module B, where B's `globals.js` defines `foo()` and A's unit test calls `globals.foo()`, the LCOV report contains an `SF:B/globals.js` record whose DA lines cover `foo()` (covered lines with their counts; uncovered reachable lines as `,0` per SVY-21278). There is **no** `SF:A/globals.js` record for B's functions.
- [ ] Global functions from different modules land in **separate** `SF:<module>/globals.js` records, each attributed to the module that declares the function.
- [ ] `FNF`, `FNH`, `LF`, `LH` are computed **per module**, not for the whole global scope. `FNF` for a module equals the count of global methods declared in that module.
- [ ] Any global function whose `ScriptMethod` persist cannot be resolved is attributed to the active solution (previous behaviour) and a warning is logged — no global coverage is silently dropped.
- [ ] Named-scope (`SF:<module>/<scope>.js`) output is unchanged (same SF/FNF/FNH/FN/FNDA/DA/LF/LH/end_of_record).
- [ ] Form (`SF:<module>/forms/<form>.js`) output is unchanged.
- [ ] SVY-21278 uncovered-line behaviour (`DA:line,0`, `LF`/`LH`) still holds within each per-module global block.
- [ ] SonarQube resolves each module's real `globals.js` against the report (no unresolved `<activeSolution>/globals.js` path).

## 6. Out of scope

- The IDE-side JSON coverage writer `com.servoy.eclipse.jsunit/.../JSUnitCoverageWriter.java` (SVY-21131) — same module-less grouping, different format/consumers. Flag as a possible follow-up; do not change here.
- Any change to the runtime hit/reachable-line collection in `com.servoy.eclipse.jsunit` (`JSUnitDebugFrame`, `JSUnitDebugger`). The module is recovered at write time from the persist model instead.
- Any change to `FlattenedSolution.addGlobalsScope()` / `getScopes()` — rejected in triage (wrong model, wide blast radius).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| For the unresolved-function fallback group, is attributing to the active solution's `globals.js` acceptable, or should it use a distinct sentinel file name so such cases are visible in the report? | reporter / team | open |
| Should the same per-module fix be applied to the JSON writer (SVY-21131) in a follow-up, so IDE and LCOV coverage agree? | team | open |
