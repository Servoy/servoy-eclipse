# Spec: SVY-21680 — JSUnit JSON coverage attributes coverage per declaring solution via a `solution` field

| Field        | Value                                                        |
|--------------|--------------------------------------------------------------|
| Jira         | [SVY-21680](https://servoy-cloud.atlassian.net/browse/SVY-21680) |
| Type         | Task (follow-up of a Bug)                                    |
| Priority     | Minor                                                        |
| Status       | In Progress                                                  |
| Fix Version  | 2026.12                                                      |
| Related      | [SVY-21607](https://servoy-cloud.atlassian.net/browse/SVY-21607) (LCOV per-solution global attribution — already encodes `SF:<solution>/<scope>.js`), [SVY-21131](https://servoy-cloud.atlassian.net/browse/SVY-21131) (introduced the JSON writer) |
| Triage       | `docs/SVY-21680-triage.md`                                  |

## 1. Goal

When Servoy Developer JSUnit tests run with coverage enabled, the generated JSON
coverage report (`jsunit-coverage.json`, written by `JSUnitCoverageWriter`) must
attribute every scope and form to the **solution that declares it** — the active/main
solution **or** one of its modules — by adding a single new `solution` field to each
`scopes[]` and `forms[]` entry, and by splitting the merged `globals` bucket into one
entry per declaring solution (each keeping `name: "globals"`). This brings the JSON
report to parity with the LCOV report, which already encodes solution + scope as
`SF:<solution>/<scope>.js` since SVY-21607.

The JSON stays **flat** — top-level `scopes[]` and `forms[]` arrays are preserved (no
nesting by solution). The schema change is **purely additive**: one new `solution`
field on every entry, no existing field removed or renamed. Summary totals are
unchanged (functions are only regrouped/annotated, never duplicated or dropped).

> **A note on terminology.** "Solution" here means the declaring root object of a scope
> or form. That can be the **active/main solution** (e.g. `A`), which is **not** a module,
> or any **module** (`B`, `C`, `D`) of that main solution. The new field is named
> `solution` precisely because the declaring root object is always a `Solution` persist,
> whether it is the main solution or a module.

## 2. Background

### 2.1 Where the JSON writer lives

The JSON coverage report is produced in **this** repo (`servoy-eclipse`) by
`com.servoy.eclipse.jsunit.runner.JSUnitCoverageWriter`
(`com.servoy.eclipse.jsunit/src/com/servoy/eclipse/jsunit/runner/JSUnitCoverageWriter.java`),
added by SVY-21131. Its public entry point is `write(...)`, which delegates all
grouping/serialisation to `buildJson(lineNumbers, reachableLines, solutionName, flattenedSolution)`.
A `FlattenedSolution` is already plumbed into `write(...)`/`buildJson(...)` (the caller
`com.servoy.eclipse.jsunit.smart.RunClientTests.writeCoverageIfAvailable()` passes
`testApp.getFlattenedSolution()`), so no further signature change is needed for this fix —
only the *use* of that `FlattenedSolution` inside `buildJson(...)` is corrected.

This is a **different output** from the SVY-21607 LCOV writer (separate `test` repo). The
two share no code path; SVY-21607 explicitly deferred the JSON side to this ticket.

### 2.2 The current output is WRONG (defect to fix)

The committed first attempt special-cased the merged `globals` bucket and emitted one
`scopes[]` entry per declaring solution, but **named the entry after the solution** instead
of the scope. For the user's real sample — main solution `A` with a `test_globals` scope,
and module `B` whose `globals.js` declares `foo()` — the current writer produces:

```json
"scopes": [
  { "name": "test_globals", "functions": [ { "name": "test_fooReturnsStringFoo", "coveredLines": [9,10,11], "uncoveredLines": [] } ] },
  { "name": "B",            "functions": [ { "name": "foo", "coveredLines": [6,7], "uncoveredLines": [] } ] }
]
```

That `{ "name": "B", ... }` entry is wrong on several counts:

1. **It replaced the scope name `globals` with a solution/module name.** The entry's `name`
   is now a solution, not a scope — the `globals` scope identity is lost, and `B.foo` is not
   a real, addressable Servoy reference (the real reference is `globals.foo` declared in `B`).
2. **It mixes two kinds of key in one flat array.** `{ "name": "B" }` (a solution) sits next
   to `{ "name": "test_globals" }` (a real named scope). A consumer reading `name` can no
   longer tell whether it holds a scope or a solution.
3. **Same-named globals across modules become indistinguishable from each other and from the
   scope they belong to.** Two modules each declaring a `globals.foo()` would collapse into two
   entries keyed by module name, with no indication they are both the `globals` scope.

### 2.3 Why globals are module-less in the raw hit map

The runtime hit map is keyed `type → scopeName → funcName → line → hits`. On the collection
side, `JSUnitDebugFrame.onLineChange(...)` records every global hit under
`globalScope.getScopeName()`, which returns the literal `"globals"`
(`ScriptVariable.GLOBAL_SCOPE`) for **all** solutions. So the `lineNumbers` map's `"scopes"`
bucket holds exactly one `"globals"` entry containing the global functions of every solution —
there is no per-solution key at collection time. This is the same collection-side cause
SVY-21607 identified for LCOV; the collection side is **not** changed — the declaring solution
is recovered at write time from the persist model. Named scopes and forms are already 1:1 with
a single declaring solution in the raw map.

### 2.4 The declaring solution is recoverable at write time

Each scope/form function's declaring solution can be resolved from the persist model via the
`FlattenedSolution` the writer already holds (see §3.3 for the exact APIs). The fix annotates
every entry with its declaring `solution` and splits `globals` per declaring solution — while
keeping each entry's `name` as the true scope/form name.

### 2.5 The flat schema and the single added field

The current JSON shape (per SVY-21131) is:

```json
{
  "solution": "A",
  "timestamp": "2026-06-05T10:00:00Z",
  "summary": { "coveredLines": 142, "uncoveredLines": 38 },
  "scopes": [ { "name": "myScope", "functions": [ { "name": "fn", "coveredLines": [1,2], "uncoveredLines": [3] } ] } ],
  "forms":  [ { "name": "myForm",  "functions": [ ... ] } ]
}
```

The fix keeps this flat shape and adds exactly **one** field — `solution` — to every
`scopes[]` and `forms[]` entry. The top-level `solution` (main/active solution name),
`timestamp`, and `summary` are unchanged. No field is removed or renamed.

### 2.6 Consumer and LCOV

The AI/MCP consumer of this JSON is `JSUnitCoverageService` in the **Servoy-Copilot** repo
(see §3.6). Because the arrays stay flat and top-level `scopes`/`forms` keep their shape, the
consumer keeps working against the old schema; a minimal update makes it *use* the new
`solution` field so its report and test suggestions are unambiguous.

LCOV already encodes solution + scope as `SF:<solution>/<scope>.js` (SVY-21607), so **no LCOV
change** is needed — this ticket only brings the JSON to the same per-solution parity.

## 3. Design

Inside `buildJson(...)`, keep the flat `scopes[]`/`forms[]` arrays. For every entry resolve its
declaring solution and emit a new `solution` field. Split only the merged `globals` bucket into
one entry per declaring solution, each keeping `name: "globals"`. Everything else about the
serialisation (function sort, summary, number formatting, `EXCLUDED_SOURCES` filtering) is
unchanged.

### 3.1 Add a `solution` field to every scope and form entry

Every `scopes[]` and `forms[]` entry gains a `solution` string = the name of the declaring
`Solution` (main solution or a module). The entry's `name` always stays the real scope/form
name (`globals`, `utils`, `test_globals`, a form name) — **never** a solution name. The field
is present and consistent on **all** entries, not only globals.

Corrected sample output for the §2.2 example:

```json
"scopes": [
  { "name": "test_globals", "solution": "A", "functions": [ { "name": "test_fooReturnsStringFoo", "coveredLines": [9,10,11], "uncoveredLines": [] } ] },
  { "name": "globals",      "solution": "B", "functions": [ { "name": "foo", "coveredLines": [6,7], "uncoveredLines": [] } ] }
]
```

Here `{ "name": "globals", "solution": "B" }` corresponds exactly to LCOV's `SF:B/globals.js`,
and `test_globals` is correctly annotated with its declaring main solution `A`.

### 3.2 Split the merged `globals` bucket into one entry per declaring solution

The runtime merges every solution's globals into one `globals` scope. For the
`type == "scopes"`, `scopeName.equals(ScriptVariable.GLOBAL_SCOPE)` bucket, group its functions
by declaring solution and emit one `ScopeEntry` per solution, **each with `name = "globals"`**
and its own resolved `solution`. This yields LCOV parity — e.g.
`{ "name": "globals", "solution": "B" }` ↔ `SF:B/globals.js` — and makes same-named globals
across modules distinguishable by their `solution`.

This is the correction of the current behaviour: the per-solution split stays, but the entry is
`{ name: "globals", solution: <declaringSolution> }`, **not** `{ name: <declaringSolution> }`.

### 3.3 Resolve the declaring solution for globals, named scopes and forms

All resolution goes through the `FlattenedSolution fs` already passed into `buildJson(...)`.
Confirmed APIs (read from `com.servoy.j2db.FlattenedSolution` and the persist types):

- **globals function → solution:**
  `ScriptMethod sm = fs.getScriptMethod(ScriptVariable.GLOBAL_SCOPE, funcName)`. If `sm != null`
  and `sm.getRootObject() instanceof Solution declaring`, then `declaring.getName()` is the
  declaring solution. (`getRootObject()` is inherited via `AbstractBase`/`AbstractRootObject`;
  for a global method it returns the owning `Solution`. `ScriptVariable.GLOBAL_SCOPE` is the
  public constant `"globals"`.) This is exactly what the current `splitGlobalsByModule`
  already does to compute the group key — but the resolved solution must go into the new
  `solution` field while the entry keeps `name: "globals"`, instead of renaming the entry to
  the solution.
- **named scope → solution:** `fs.getScopes()` returns
  `Collection<Pair<String, IRootObject>>` (scope name → declaring root object), built from
  `getScopesPerSolution()`, which maps each scope name to the single `Solution` that declares
  it. Resolve the scope's declaring solution from that map (the `Pair`'s right-hand
  `IRootObject` is the declaring `Solution`; use its `getName()`). A parent solution and a
  module **cannot** declare a scope with the same name (`getScopesPerSolution()` keeps one
  entry per scope name), so there is no collision — each named scope is 1:1 with one solution.
- **form → solution:** `Form form = fs.getForm(formName)`; `form.getRootObject()` is the
  declaring `Solution`; use its `getName()`.

### 3.4 Fallback and logging

If a scope/form's declaring solution cannot be resolved (`null` script method/form, a root
object that is not a `Solution`, or an absent scope mapping), attribute the entry to the
**active/main solution name** (`solutionName`) and `ServoyLog.logWarning(...)` the unresolved
name — never drop coverage. If `flattenedSolution` is `null` altogether, every entry is
attributed to the active solution name (single `globals` entry, with a logged warning), so the
writer always produces a valid, additive-schema report.

### 3.5 Flat shape, summary, schema-additive

- **Flat arrays preserved:** top-level `scopes[]` and `forms[]` stay — there is **no** nesting
  by solution. Only the number of `scopes[]` entries changes (globals splits), and each entry
  gains a `solution` field.
- **Summary unchanged:** `summary.coveredLines/uncoveredLines` are computed by summing every
  function's covered/uncovered line counts across `scopes` and `forms`. Functions are only
  regrouped/annotated (never duplicated, added, or dropped), so the totals are identical.
- **Schema additive:** the only shape change is the new `solution` string on each entry; top-level
  `solution`, `timestamp`, `summary`, and the array names are unchanged. `name`,
  `functions[]`, `coveredLines`, `uncoveredLines` are all preserved.
- **Collection side / `EXCLUDED_SOURCES`:** `JSUnitDebugFrame`/`JSUnitDebugger` and the excluded
  JsUnit infrastructure scripts are unchanged.

### 3.6 Consumer update (Servoy-Copilot — separate commit)

`JSUnitCoverageService`
(`.../com.servoy.eclipse.developer.mcp/src/com/servoy/eclipse/developer/mcp/services/JSUnitCoverageService.java`)
reads top-level `scopes`/`forms` (the flat shape keeps it working unchanged), renders a per-scope
table in `getCoverageReport` → `appendSectionSummary`, and flattens functions for `suggestTests`
(building `scopeName.functionName`). A **minimal** update makes it use the new `solution` field:

- `collectFunctions` / `FunctionCoverage` read the new `solution` field alongside `name`.
- `getCoverageReport`'s `appendSectionSummary` table gains a **`Solution`** column (or qualifies
  the scope with its solution), so two `globals` rows for different solutions are distinguishable.
- `suggestTests` prefixes each suggestion with its solution — e.g. ``B / globals.foo`` — so
  suggestions are unambiguous across same-named globals in different solutions.

This is a **separate commit in the Servoy-Copilot repo**; the writer change lives in
`servoy-eclipse`. The consumer still tolerates an absent `solution` field (older reports) by
falling back to the active solution / omitting the column.

## 4. Implementation plan

Writer changes are in **this** repo (`servoy-eclipse`), one file; the consumer change is a
distinct commit in the **Servoy-Copilot** repo. Edit via `eclipse-coder`; after each edit run
`eclipse-ide.getCompilationErrors`, `eclipse-coder.organizeImports`, and `eclipse-coder.formatFile`.

1. **`com.servoy.eclipse.jsunit/src/com/servoy/eclipse/jsunit/runner/JSUnitCoverageWriter.java`**
   (writer)
   1. **Add the `solution` field to the serialiser.** Extend the `ScopeEntry` holder with a
      `solution` field and emit `"solution": <jsonString(solution)>` in `appendScopeArray(...)`
      for every entry (right after `name`). This is the additive schema change.
   2. **Revert the `name = solution` behaviour.** In `splitGlobalsByModule(...)`, keep the
      grouping-by-declaring-solution, but construct each entry as
      `new ScopeEntry("globals", <resolvedSolution>, functions)` — i.e. `name` stays
      `ScriptVariable.GLOBAL_SCOPE` and the resolved solution goes into the new `solution` field.
      (Consider renaming the helper to e.g. `splitGlobalsBySolution` for clarity.)
   3. **Resolve `solution` for named scopes and forms too.** For the non-globals branch, resolve
      the declaring solution (§3.3: `fs.getScopes()`/`getScopesPerSolution()` for scopes,
      `fs.getForm(name).getRootObject()` for forms) and pass it into the `ScopeEntry`. Build the
      scope→solution map once per `buildJson` call to avoid repeated lookups.
   4. **Fallback + log** (§3.4): unresolved → active solution name + `ServoyLog.logWarning`;
      `flattenedSolution == null` → active solution name for all entries, single `globals` entry,
      one warning.
   5. Update the class Javadoc sample JSON to show the `solution` field and the two `globals`
      entries; keep imports (`FlattenedSolution`, `ScriptMethod`, `ScriptVariable`, `Solution`,
      plus `Pair`/`IRootObject` as needed for scope resolution).
   6. Compile clean (no new SpotBugs high/medium findings), organize imports, format.
2. **Tests (servoy-eclipse).** Extend the existing plainjunit test of
   `buildJson`/`splitGlobals...` (`com.servoy.eclipse.jsunit.tests`,
   `JSUnitCoverageWriterGlobalModuleTest`) to assert the corrected shape: mock
   `FlattenedSolution.getScriptMethod`/`getScopes`/`getForm` to return `ScriptMethod`/`Pair`/`Form`
   whose `getRootObject()` yields distinct `Solution`s, then parse the JSON and assert `name` is
   the real scope/form name, `solution` is correct on every entry, `globals` appears once per
   declaring solution, and summary totals are unchanged. Mock the persist types with
   `doReturn(...).when(mock).call()` (their `AbstractBase`/`AbstractRootObject` supertypes have
   heavy static init; the `when(mock.call())` form can invoke the real method and fail stubbing).
3. **`JSUnitCoverageService.java`** (Servoy-Copilot — **separate commit**): the minimal consumer
   update in §3.6 (`collectFunctions`/`FunctionCoverage` read `solution`; `appendSectionSummary`
   gets a `Solution` column; `suggestTests` prefixes suggestions with the solution). Tolerate an
   absent `solution` field for older reports.

## 5. Acceptance criteria

- [ ] The merged `globals` bucket is split into **one entry per declaring solution**; e.g. for
      main solution `A` (with `test_globals`) and module `B` (whose `globals.js` declares `foo`),
      `scopes[]` contains `{ "name": "globals", "solution": "B", functions:[foo…] }` and
      `{ "name": "test_globals", "solution": "A", functions:[…] }`.
- [ ] Every entry's `name` is the **real scope/form name** (`globals`, a named scope, a form
      name) — **never** a solution name. There is no `{ "name": "B" }`-style entry.
- [ ] A new `solution` field is present and correct on **every** `scopes[]` and `forms[]` entry
      (named scopes and forms included, not only globals).
- [ ] Same-named globals declared in different solutions are **distinguishable**: two
      `{ "name": "globals" }` entries with different `solution` values, functions not merged.
- [ ] Named scopes and forms carry the **correct** declaring `solution` (resolved via
      `getScopes()`/`getScopesPerSolution()` and `getForm(...).getRootObject()`).
- [ ] Any entry whose declaring solution cannot be resolved is attributed to the active/main
      solution name and a warning is logged; if `flattenedSolution` is `null`, all entries use the
      active solution name (single `globals` entry) with a logged warning — no coverage dropped.
- [ ] `summary.coveredLines` / `summary.uncoveredLines` totals are **identical** to before the
      change for the same run.
- [ ] The flat top-level `scopes[]` and `forms[]` arrays are preserved (no nesting by solution);
      the only schema change is the added `solution` field — no field removed or renamed.
- [ ] The Servoy-Copilot consumer (`JSUnitCoverageService`) shows the solution (a `Solution`
      column / qualified scope) in its report, and its test suggestions are prefixed with the
      solution (e.g. ``B / globals.foo``) so they are unambiguous.
- [ ] The JSON per-solution attribution **agrees with the SVY-21607 LCOV** output for the same
      run: `{ "name": "globals", "solution": "B" }` ↔ `SF:B/globals.js`, named scope/form ↔
      `SF:<solution>/<scope>.js`.

## 6. Out of scope

- **No top-level nesting by solution.** The JSON stays flat (`scopes[]`/`forms[]` at the top);
  per-solution attribution is carried by the added `solution` field, not by a nested structure.
- **The runtime hit/reachable-line collection** in `com.servoy.eclipse.jsunit`
  (`JSUnitDebugFrame.onLineChange`, `JSUnitDebugger`) — unchanged; the declaring solution is
  recovered at write time from the persist model (same decision as SVY-21607).
- **The LCOV writer** — already encodes `SF:<solution>/<scope>.js` per SVY-21607 in the `test`
  repo; not touched here.
- **Removal or renaming of any existing JSON field** — the change is purely additive (one new
  `solution` field).
- **A cross-repo shared helper** between the LCOV writer (`test` repo) and the JSON writer (this
  repo) — impractical (no shared classpath); the resolution/grouping is a small private helper
  within `JSUnitCoverageWriter`.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| For the unresolved-entry fallback, is attributing to the active/main solution name (and logging a warning) acceptable, or should such entries carry a visibly distinct sentinel `solution` value so they stand out in the JSON? | reporter / team | open |
| Should the consumer render the `solution` as a separate table column, or inline-qualify the scope name (e.g. `B/globals`)? Both satisfy the AC; pick the clearer one for the MCP report. | team | open |
| Do any other current JSON consumers (beyond `JSUnitCoverageService`) read these reports and need the same `solution`-aware update? Confirm none rely on the old `{ "name": "<solution>" }` behaviour from the first attempt. | team | open |
