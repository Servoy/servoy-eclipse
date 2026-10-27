# Triage Report — SVY-21607

**Verdict:** PROCEED

## Reported problem

When the unit tests of a solution with modules are run with coverage enabled, the generated
LCOV report lists each *named* scope under its own file path (`SF:<module>/<scope>.js`), but
the **global scope is handled differently**: every global function, from every module, is
collapsed into a single record keyed by the *active solution's* name:

```
SF:globis_unit_test/globals.js
FNH:489
LF:13893
LH:7625
```

`globis_unit_test` is the solution the tests run in; it has no `globals.js` of its own. The
489 functions / ~13,893 lines actually come from the `globals.js` files of its modules
(`database_globis_framework/globals.js`, `globis_basis_methods/globals.js`, …). None of those
real module `globals.js` files appear in the report.

**Expected:** an entry `SF:<module>/globals.js` per module that defines global functions, with
those functions' covered lines (and, since SVY-21278, uncovered reachable lines as `,0`).
**Actual:** all global coverage lands under one `SF:<activeSolution>/globals.js` and no
per-module `globals.js` entries exist.

**Impact (reporter):** SonarQube (and similar tools) cannot resolve `<activeSolution>/globals.js`
to a real source file, so every module's `globals.js` is reported as 0% covered even though the
tests execute it. In the reporter's project that is 51 `globals.js` files (~140,000 lines); they
currently exclude all `globals.js` from coverage as a workaround. The merged entry's line numbers
cannot be traced back to their files, so this cannot be fixed in post-processing.

**Proposed solution (ticket):** write the coverage of global functions under the `globals.js` of
the module that defines them, as is already done for named scopes.

## Root-cause assessment

The LCOV report is produced by `ServoyJSUnitTestRunner.suite()`'s `tearDown()` in the **`test`
repo** (not this `servoy-eclipse` repo):

- `C:\Users\merae\git\test\j2db_test\src\com\servoy\automation\jsunit\smartclient\ServoyJSUnitTestRunner.java`,
  lines ~226–270 (the `"scopes"` branch).

The coverage hit data is a map keyed only by **scope name**:

- `JSUnitDebugFrame.onLineChange()` (this repo,
  `com.servoy.eclipse.jsunit/src/com/servoy/eclipse/jsunit/runner/JSUnitDebugFrame.java:69`)
  records a hit as `debugger.addLineNumberHit("scopes", globalScope.getScopeName(), name, line)`.
  For every module's `globals.js` the scope name is the **same literal `"globals"`**
  (`ScriptVariable.GLOBAL_SCOPE = "globals"`, `servoy_shared/.../ScriptVariable.java:66`), because
  all modules contribute to one shared global scope at runtime.
- So in `JSUnitDebugger.lineNumbers` (`type → scopeName → funcName → line → hits`) there is a
  single `"globals"` bucket holding the functions of **all** modules, with no per-module key.

When writing LCOV, `tearDown()` resolves the file path for a scope like this
(`ServoyJSUnitTestRunner.java:231-237`):

```java
Pair<String, IRootObject> solutionPair = importRunner.getApplication().getFlattenedSolution()
    .getScopes().stream().filter(s -> s.getLeft().equals(scopeName)).findFirst().get();
Solution solution = (Solution)solutionPair.getRight();
builder.append("SF:").append(solution.getName()).append("/").append(scopeName).append(".js");
```

`FlattenedSolution.getScopes()` → `getScopesPerSolution()` (servoy_shared/.../FlattenedSolution.java)
maps each **named** scope to the module that declares it, but for the global scope it calls
`addGlobalsScope()` (line 1197), which unconditionally maps `"globals"` to **`mainSolution`**
(the active solution). That is why named scopes are attributed to the right module and the
global scope is always attributed to the active solution.

The mismatch is therefore twofold and both parts point at the same cause — *the global scope is
module-agnostic by design at the scope level*:

1. **Collection side:** all modules' global functions share one `"globals"` bucket (no module in
   the key).
2. **Resolution side:** `getScopes()`/`addGlobalsScope()` hard-maps `"globals"` to the active
   solution.

The information needed to fix it **does exist per function**: a global function's `ScriptMethod`
persist knows its owning module via `getRootObject()`. `FlattenedSolution.getScriptMethod(String
scopeName, String name)` (servoy_shared) returns that `ScriptMethod`. So the writer can, for each
function name in the `"globals"` bucket, resolve the owning module and emit one
`SF:<module>/globals.js` record per module, grouping that module's functions under it — exactly
the way named scopes already work.

The reporter's observation "named scopes are fine, only globals is wrong" is fully consistent
with this: named scopes are 1:1 with a module in `getScopesPerSolution()`, the global scope is
not.

## Ticket premise check

The premise holds. The ticket reports a genuine product bug (the LCOV output is objectively wrong
— it attributes module source to a solution that doesn't contain it, and omits the real files),
and its proposed fix ("report global functions under the `globals.js` of the module that defines
them, like named scopes") is the correct direction and is supported by existing API
(`ScriptMethod.getRootObject()` / `FlattenedSolution.getScriptMethod(scope, name)`). It is not a
request that fights an intentional design: `addGlobalsScope()`'s mapping of `"globals"` to the
active solution is incidental to this report, not a deliberate coverage decision.

Two clarifications worth recording:

- **The file to change is in the `test` repo, not this one.** The LCOV writer is
  `j2db_test/.../smartclient/ServoyJSUnitTestRunner.java`. This matches the prior SVY-21278 spec
  in `docs/`, which already located LCOV generation there. This `servoy-eclipse` repo only hosts
  the hit/reachable-line *collection* (`com.servoy.eclipse.jsunit`), which does not need to change
  for the LCOV fix — the module can be recovered at write time from the persist model.
- **A parallel, separate code path exists but is out of scope.** `com.servoy.eclipse.jsunit`'s
  `JSUnitCoverageWriter` (this repo) writes a **JSON** coverage report (SVY-21131, for the IDE AI
  integration) and has the same scope-name-only keying — it would show the same module-less
  grouping. It is a different output format with different consumers; this ticket is specifically
  about the LCOV report. Worth noting to the implementer, but not part of this fix unless the team
  wants to extend scope.

## Approaches considered

1. **Resolve the owning module per global function at LCOV-write time and emit one
   `SF:<module>/globals.js` per module (ticket's approach).**
   In the `"globals"` branch of `ServoyJSUnitTestRunner.tearDown()`, group the functions of the
   `"globals"` bucket by their owning module — for each `funcName`, look up
   `flattenedSolution.getScriptMethod("globals", funcName)` and take `.getRootObject().getName()`
   as the module — then write a separate `SF:<module>/globals.js` block (with its own FN/FNDA/DA/
   LF/LH) for each module. Keep named-scope handling unchanged.
   - *Pros:* fixes exactly the reported defect; uses existing API; mirrors named-scope output so
     SonarQube resolves the files; no change to the runtime hit-collection; contained to one file.
   - *Cons:* functions whose persist can't be resolved (edge cases) need a defined fallback (e.g.
     keep them under the active solution, or a dedicated bucket) so none are silently dropped;
     requires care that `FNF` (functions found) is computed per module, not for the whole scope.

2. **Make the hit collection module-aware (change the key from `"globals"` to per-module).**
   Change `JSUnitDebugFrame`/`JSUnitDebugger` (this repo) to split the global scope per module at
   collection time.
   - *Pros:* module info present in the data structure itself.
   - *Cons:* the runtime global scope is genuinely a single shared `"globals"` scope across
     modules; the frame only has the runtime `GlobalScope` (scope name `"globals"`), not the
     defining module, so it would need persist lookups at runtime anyway. More invasive, touches a
     shared runtime path used beyond coverage, higher regression risk. The module is more cheaply
     and safely recovered at write time (approach 1).

3. **Fix `addGlobalsScope()` to map `"globals"` to the owning module.**
   - *Pros:* small.
   - *Cons:* wrong model — there is exactly one `"globals"` entry but many modules define global
     functions, so a single mapping cannot express the 1-scope-to-many-modules relationship. Would
     also change behavior for every other caller of `getScopes()`/`getScopesPerSolution()`, a wide
     blast radius for a coverage-reporting concern. Rejected.

4. **No code change** — tell the reporter to keep excluding `globals.js`, or post-process.
   - *Pros:* none beyond zero effort.
   - *Cons:* the report is objectively incorrect (real module files missing, lines attributed to a
     solution that doesn't contain them); the reporter already states post-processing is impossible
     because merged line numbers can't be traced back to files. Does not address the defect.

## Recommendation

**PROCEED with approach 1.** In `ServoyJSUnitTestRunner.tearDown()`'s `"scopes"` branch, when the
scope name is the global scope (`ScriptVariable.GLOBAL_SCOPE` / `"globals"`), group that bucket's
functions by their owning module via `FlattenedSolution.getScriptMethod("globals", funcName)
.getRootObject()` and emit one `SF:<module>/globals.js` LCOV block per module (named scopes stay
as they are). This is the ticket's intent, is supported by existing persist API, is localized to a
single file in the `test` repo, and makes the global-scope output consistent with named scopes so
coverage tools resolve each module's real `globals.js`.

Implementation notes for the spec/implementer:
- The change is in the **`test`** repo:
  `j2db_test/src/com/servoy/automation/jsunit/smartclient/ServoyJSUnitTestRunner.java` (the same
  file SVY-21278 edited), *not* in `servoy-eclipse`.
- Compute `FNF`/`FNH`/`LF`/`LH` **per module**, not for the whole global bucket.
- Define a fallback (e.g. attribute to the active solution, as today) for any global function whose
  `ScriptMethod` persist cannot be resolved, so no coverage is silently lost; log it.
- Consider flagging to the team that the IDE-side JSON writer
  (`com.servoy.eclipse.jsunit/.../JSUnitCoverageWriter.java`, SVY-21131) has the same module-less
  grouping, in case they want a matching fix there in a follow-up.

Alternatives 2–4 are documented above; 3 and 4 are rejected, 2 is a more invasive fallback only if
write-time persist resolution proves insufficient.

## Git history findings

- The LCOV writer was introduced by SVY-20918 ("Code coverage reporting in unit test",
  `test` repo commit `bac749e2`, Johan Compagner, 2026-06-06) — this is the commit that added the
  `SF:`/`FNF:`/`FNH:` writing and the `getScopes().stream().filter(...)` scope-to-solution
  resolution that hard-maps the global scope to the active solution. In this repo the matching
  hit-collection changes landed in `e66b16285b` (SVY-20918, lvostinar, 2026-04-03) across
  `JSUnitDebugFrame`, `JSUnitDebugger`, `JSUnitSuite`, `JSUnitToJavaRunner`.
- SVY-21278 (`test` commits `a730ee6`, `f590990`, emera, 2026-07-27) later added uncovered-line
  (`DA:line,0`) and `LF`/`LH` support to the same method; `docs/SVY-21278-lcov-uncovered-lines.spec.md`
  documents it and confirms the LCOV writer lives in `j2db_test`.
- `addGlobalsScope()` mapping `"globals"` → `mainSolution`
  (`servoy_shared/.../FlattenedSolution.java:1197`) predates and is unrelated to coverage; it exists
  to guarantee a global scope always exists. The coverage code's reliance on it for file-path
  resolution is the incidental cause here, not a deliberate coverage design — so fixing the
  coverage writer does not revert any intentional decision.
- No prior spec for SVY-21607 exists in `docs/`.
