# Triage Report — SVY-21680

**Verdict:** PROCEED

Apply per-module global-function attribution to the IDE-side JSON coverage writer
`JSUnitCoverageWriter` (`com.servoy.eclipse.jsunit`), mirroring the per-module LCOV
fix landed in SVY-21607, so the JSON and LCOV coverage reports agree. The change is
confirmed to belong in **this** repo and the required persist resolution API is
reachable from where the writer runs.

## Reported problem

- **Context (verified).** SVY-21680 is a genuine follow-up of SVY-21607, linked
  "relates to" both SVY-21607 and SVY-21131. SVY-21607 ("Code coverage books all
  global functions under the active solution's globals.js instead of the module that
  defines them", In Progress, Bug) fixed the **LCOV** report so that each module's
  global functions are attributed to `SF:<module>/globals.js` — one record per module
  — instead of being collapsed into a single `SF:<activeSolution>/globals.js` record.
  That fix lives in the **`test`** repo (`j2db_test/.../ServoyJSUnitTestRunner.java`);
  see `docs/SVY-21607-globals-coverage-per-module.spec.md` and
  `docs/SVY-21607-triage.md` (both committed here by `737b262`, and §3.6/§6 of that
  spec explicitly flag the JSON writer as a deferred follow-up — which is this ticket).
  SVY-21131 ("servoy developer jsunit test coverage report", Closed) added the
  JSON writer `JSUnitCoverageWriter` used by the AI/MCP integration.

- **The defect.** `JSUnitCoverageWriter` emits coverage grouped by raw *scope name*.
  The global scope is recorded under the single literal name `"globals"`
  (`ScriptVariable.GLOBAL_SCOPE = "globals"`), so every module's global functions are
  written into **one** `scopes` entry named `"globals"`, with no per-module
  attribution. The JSON therefore disagrees with the (now per-module) LCOV report for
  global-function coverage.

- **Requested fix.** Resolve each global function's owning module via
  `FlattenedSolution.getScriptMethod("globals", funcName).getRootObject()` and emit one
  `scopes` entry per module (keyed/named after the module that declares each function),
  leaving named-scope and form coverage unchanged.

## Root-cause assessment

Confirmed by reading the code (not assumed):

- **Where globals are keyed module-less.** `JSUnitDebugFrame.onLineChange(...)`
  (lines 65–73) records every global hit as
  `debugger.addLineNumberHit("scopes", globalScope.getScopeName(), name, lineNumber)`.
  For the shared global scope, `GlobalScope.getScopeName()` returns the literal
  `"globals"` for **all** modules (the runtime has a single merged global scope). So
  the `lineNumbers` map's `"scopes"` bucket contains exactly one `"globals"` entry
  holding the global functions of every module — there is no per-module key at
  collection time. This is the *same* collection-side cause SVY-21607 identified for
  LCOV; the module information is simply not present in the runtime hit map.

- **Where it surfaces in the JSON writer.** `JSUnitCoverageWriter.buildJson(...)`
  (lines 92–188) iterates `lineNumbers` as `type → scopeName → funcName → line → hits`
  and builds one `ScopeEntry` per `scopeName`. For `type == "scopes"` and
  `scopeName == "globals"` that yields a single module-less `"globals"` scope entry —
  exactly the reported behaviour. Named scopes (`type == "scopes"`, a real scope name)
  and forms (`type == "forms"`) already map 1:1 to a source and are correct; they must
  stay unchanged.

- **The per-function module IS recoverable at write time** (same mechanism the
  SVY-21607 LCOV fix uses). `FlattenedSolution.getScriptMethod("globals", funcName)`
  returns the `ScriptMethod`; its inherited `getRootObject()` (`IPersist.getRootObject()`
  → for a global method the owning `Solution`/module) gives the declaring module, and
  `getRootObject().getName()` is the module name. `GLOBAL_SCOPE` is a public constant on
  `com.servoy.j2db.persistence.ScriptVariable` (`"globals"`). So the writer can group
  the single `"globals"` bucket's functions by owning module and emit one entry per
  module.

- **Reachability of the resolution API from the writer — verified, and this is the key
  divergence from SVY-21607.** Unlike the LCOV writer (which already had the
  `FlattenedSolution` in hand), `JSUnitCoverageWriter.write(...)` today only receives
  the raw maps plus a `String solutionName` and an output path — it has **no**
  `FlattenedSolution`. The resolution must therefore be plumbed in. It is available at
  the call site: `RunClientTests.writeCoverageIfAvailable()` (the sole caller,
  lines 179–202) holds `DebugTestClient testApp` (a `ClientState`/`IServiceProvider`,
  so `testApp.getFlattenedSolution()` is available) and `testTarget.getActiveSolution()`.
  `com.servoy.eclipse.jsunit` already depends on `servoy_shared`/`servoy_debug` and
  already imports these persist types, so passing a `FlattenedSolution` (or the pieces
  the writer needs) into `write(...)` is a small, local signature change with no new
  bundle dependency.

## Ticket premise check

- **Is it really this project's code?** Yes. `JSUnitCoverageWriter` and its only caller
  `RunClientTests` are both in `com.servoy.eclipse.jsunit` in **this** repo
  (`servoy-eclipse`). The LCOV fix was in the `test` repo; the two are genuinely
  separate outputs. The premise that this is the right repo is correct.

- **Is "mirror the LCOV fix" the right approach?** Largely yes, but one premise needs
  qualifying. The LCOV spec's phrasing "resolve via `FlattenedSolution.getScriptMethod(...)`"
  is sound, but the ticket implies the writer can just do it — it cannot today, because
  the writer has no `FlattenedSolution`. The real (small) work is **plumbing the
  `FlattenedSolution` into `write(...)`**, then grouping by module. Call this out so it
  isn't underestimated as a pure "writer-internal" change.

- **Would changing the grouping break the JSON schema / AI consumer?** Low risk, worth
  stating. The JSON `scopes[]` entries are keyed by a free-form `"name"` string
  (per `docs/SVY-21131-*.spec.md` the example even uses `"myGlobalScope"`), and the
  consumer (`JSUnitCoverageService` / the MCP `getJSUnitCoverageReport` /
  `suggestTestsFromCoverage` tools) reads a *list* of scope entries and summarises per
  scope/function. Splitting one `"globals"` entry into several per-module entries stays
  within the same shape (more entries, still `{name, functions[]}`); it does not add or
  remove fields. The `summary.coveredLines/uncoveredLines` totals are unaffected because
  the same functions/lines are merely regrouped. The only semantic change a consumer
  sees is that `"globals"` becomes N module-named entries — which is exactly the point
  (parity with LCOV). No schema field change is required.

- **Is there a cleaner shared abstraction than mirroring?** In principle both writers
  could consume a common "per-module-resolved coverage" structure, but the LCOV writer
  is in a **different repo** (`test`) with no shared code path to `com.servoy.eclipse.jsunit`.
  A cross-repo shared helper is impractical here (same conclusion the repo's AGENTS/test
  conventions reach about cross-repo sharing). So "mirror the logic" is the pragmatic
  correct choice; factor a small private helper *within* `JSUnitCoverageWriter` for the
  per-module grouping rather than attempting cross-repo reuse.

## Approaches considered

### A. Mirror the LCOV per-module resolution directly in `JSUnitCoverageWriter` (recommended)

Plumb a `FlattenedSolution` into `JSUnitCoverageWriter.write(...)` (obtained in
`RunClientTests.writeCoverageIfAvailable()` from `testApp.getFlattenedSolution()`),
and in `buildJson(...)` special-case the `type == "scopes"`, `scopeName == "globals"`
bucket: for each global `funcName`, resolve `fs.getScriptMethod(ScriptVariable.GLOBAL_SCOPE,
funcName)` → `getRootObject()` (cast to `Solution`) → module name, group functions by
module, and emit one `ScopeEntry` per module named after the module. Fall back to the
active solution name + a logged warning for any function that cannot be resolved (so no
coverage is silently dropped). Leave named scopes and forms untouched.

- **Pros:** Directly satisfies the ticket; produces JSON/LCOV parity; small, localized
  change in one class plus a one-line caller change; uses the exact API the LCOV fix
  uses; schema-compatible. Mirrors an already-reviewed, just-landed pattern.
- **Cons:** Requires a signature change to `write(...)` (and its single caller). A small
  amount of logic duplicated across repos (acceptable — cross-repo sharing is
  impractical). `getRootObject()` returns `IRootObject`; needs an `instanceof Solution`
  guard.

### B. Extract a shared helper both the LCOV writer and the JSON writer use

Factor the "group global functions by owning module" logic into one reusable utility.

- **Pros:** Single source of truth; future per-module changes touch one place.
- **Cons:** The LCOV writer is in the **`test`** repo with no build/classpath path to
  `com.servoy.eclipse.jsunit`; a shared bundle for this one helper is disproportionate
  (same reasoning the project applies elsewhere to reject cross-repo test/infra sharing).
  Rejected as impractical for now; a *within-class* private helper in
  `JSUnitCoverageWriter` captures most of the benefit without the cross-repo cost.

### C. No code change needed

- **Pros:** Zero risk.
- **Cons:** Leaves the reported defect: JSON keeps collapsing all modules' globals under
  one module-less `"globals"` entry, disagreeing with the per-module LCOV report and
  misleading the AI coverage tools about which module's globals are covered. The defect
  is confirmed in code. Rejected.

## Recommendation

**PROCEED with Approach A.** The defect is real and confirmed in this repo's
`JSUnitCoverageWriter` + `JSUnitDebugFrame`. The fix is to plumb a `FlattenedSolution`
into `write(...)` from `RunClientTests.writeCoverageIfAvailable()` and, inside
`buildJson(...)`, split the single `"globals"` scope bucket into one `ScopeEntry` per
owning module using `FlattenedSolution.getScriptMethod(ScriptVariable.GLOBAL_SCOPE,
funcName).getRootObject()` (with an `instanceof Solution` guard), keeping named-scope
and form output byte-for-byte unchanged and preserving the `summary` totals. Include the
active-solution fallback with a logged warning for unresolved functions. The JSON
schema/shape (`scopes: [{name, functions:[{name, coveredLines, uncoveredLines}]}]`) is
preserved, so the SVY-21131 AI/MCP consumers keep working — they simply see per-module
`globals` entries instead of one merged entry, which is the intended parity with LCOV.
Flag to the reviewer that the only non-trivial part is the `write(...)` signature change
(the writer has no `FlattenedSolution` today), not the grouping itself.

## Git history findings

- `JSUnitCoverageWriter` was introduced by SVY-21131 — commits `6b7d0573be`
  ("SVY-21131 JSUnit test coverage report with AI integration [ai]") and `bddaa8846f`
  ("SVY-21131 wire up EXCLUDED_SOURCES filter, remove dead code ... [ai]"). Its
  scope-name-only keying was not a deliberate per-module decision; it simply serialises
  whatever `JSUnitDebugger.getLineNumbers()` produces, and that map keys the global
  scope as the single runtime name `"globals"`. Changing the writer to regroup by module
  therefore reverts no intentional design — it is the JSON-side counterpart of the same
  gap SVY-21607 fixed for LCOV.
- The reference pattern just landed for the LCOV side: this repo's commit `737b262`
  added `docs/SVY-21607-*` (the implementation itself is in the `test` repo). That spec
  (§3.1–3.3) is the behavioural blueprint to mirror: special-case `GLOBAL_SCOPE`, group
  by `getScriptMethod(GLOBAL_SCOPE, funcName).getRootObject()`, emit per-module, fall
  back to the active solution with a warning on unresolved functions.
- `JSUnitDebugFrame.onLineChange` (SVY-21131 vintage) keying globals under
  `globalScope.getScopeName()` → `"globals"` is the shared collection-side cause; it is
  intentionally **not** changed (the module is recovered at write time from the persist
  model), consistent with SVY-21607's out-of-scope note for the collector.
