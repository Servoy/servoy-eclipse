# Spec: SVY-21131 — Servoy Developer JSUnit Test Coverage Report

## 1. Goal

Generate a code-coverage report when running Servoy Developer JSUnit tests and
expose it to an AI assistant (ServoyCopilot) that produces a human-readable
coverage summary and suggests additional test cases to improve coverage.

## 2. Background

### 2.1 Existing JSUnit infrastructure

Servoy Developer ships a `com.servoy.eclipse.jsunit` plugin that runs
solution-level JavaScript unit tests directly inside the IDE using the Rhino
JavaScript engine. The execution chain is:

```
JSUnitLaunchConfigurationDelegate
  → RunClientTests (extends RunJSUnitTests)
      → TestClientTestSuite (JUnit3 suite)
          → ApplicationJSTestSuite
              → JSUnitSuite
                  → JSUnitToJavaRunner
                      → Rhino with JSUnitDebugger installed
```

`JSUnitDebugger` already implements `org.mozilla.javascript.debug.Debugger` and
during debug-mode runs records per-line hit counts in the structure:

```
type ("scopes"|"forms") → scopeName → functionName → lineNumber → hitCount
```

This data is exposed via `JSUnitSuite.getLineNumbers()` after each run. It is
currently never serialised to any output format.

### 2.2 Reachable-lines problem

`JSUnitDebugger` only records lines that are *executed*. To report *uncovered*
lines we must also know which lines are *reachable* (i.e. exist in the compiled
source). Rhino's `Debugger.handleCompilationDone(Context, DebuggableScript,
String)` callback fires once per compiled script and provides
`DebuggableScript.getLineNumbers()` — an `int[]` of every executable line — and
`DebuggableScript.getSourceName()` — the source file name (e.g. `"myScope.js"`,
`"forms/myForm.js"`). A recursive walk of the `DebuggableScript` tree in
`handleCompilationDone` captures all reachable lines before any test executes.

### 2.3 AI integration — ServoyCopilot

`com.servoy.eclipse.servoypilot` provides an LLM-backed MCP server. Tools are
Java interfaces with `@Tool`/`@P` annotations (LangChain4j) registered in
`AllToolsForMCP`. The servoypilot bundle already depends on
`com.servoy.eclipse.jsunit` via `Require-Bundle` and has Jackson available for
JSON handling.

### 2.4 CLI runner relationship

The separate (non-IDE) `ServoyJSUnitTestRunner` in the `j2db_test` repo already
writes LCOV coverage files from the same `getLineNumbers()` data after CI runs.
This feature is the IDE counterpart: it produces JSON (not LCOV) and feeds it
to the AI chat.

## 3. Design

### 3.1 Reachable-line collection

Extend `JSUnitDebugger.handleCompilationDone()` to recursively walk the
`DebuggableScript` tree and record all executable lines:

```
reachableLines: Map<sourceName, Map<functionName, Set<Integer>>>
```

`sourceName` matches what Rhino was given when the script was compiled (e.g.
`"globals.js"`, `"myScope.js"`, `"forms/myForm.js"`). Functions with a null or
empty name are stored under the key `"<top-level>"`.

Expose via `JSUnitToJavaRunner.getReachableLines()` and
`JSUnitSuite.getReachableLines()`.

### 3.2 Coverage data collection hook

`JSUnitSuite.run()` already calls `releaseScopes()` in its `finally` block,
which saves `runner.getLineNumbers()`. The same pattern is extended to also save
`runner.getReachableLines()`.

`TestClientTestSuite` stores the last-run suite instance in a static field so
that `RunClientTests` can read coverage data after `runJUnitClass()` returns.

### 3.3 JSON output format

After each JSUnit debug-mode run, `JSUnitCoverageWriter` serialises the
collected data to JSON at `${workspace_loc}/jsunit-coverage.json` (default).
The file is written using simple `StringBuilder`-based JSON — no external
library is needed in the jsunit bundle.

```json
{
  "solution": "mySolution",
  "timestamp": "2026-06-05T10:00:00Z",
  "summary": {
    "coveredLines": 142,
    "uncoveredLines": 38
  },
  "scopes": [
    {
      "name": "myGlobalScope",
      "functions": [
        {
          "name": "myFunction",
          "coveredLines": [10, 11, 12],
          "uncoveredLines": [15, 16]
        }
      ]
    }
  ],
  "forms": [
    {
      "name": "myForm",
      "functions": [
        {
          "name": "onLoad",
          "coveredLines": [5, 6, 7],
          "uncoveredLines": []
        }
      ]
    }
  ]
}
```

`uncoveredLines` per function = `reachableLines[sourceName][funcName]` minus
the keys of `lineNumbers[type][scopeName][funcName]`. Source name to scope name
resolution: strip the `.js` suffix and any `forms/` prefix from `sourceName`
to obtain the scope/form name used as the key in `lineNumbers`.

JsUnit library scripts (`JsUtil.js`, `JsUnit.js`, `JsUnitToJava.js`,
`solutionTestSuite.js`) are excluded from the output.

### 3.4 MCP tool — `IJSUnitCoverageTool`

New tool interface in `com.servoy.eclipse.servoypilot`, package
`tools/testgeneration`. Two tool methods:

| Method | Description |
|--------|-------------|
| `getJSUnitCoverageReport(coveragePath?)` | Reads the JSON file and returns a compact markdown summary of covered/uncovered lines per scope and function |
| `suggestTestsFromCoverage(coveragePath?, maxFunctions?)` | Reads the JSON and returns concrete suggestions for additional test cases targeting uncovered lines (no source code embedded) |

`coveragePath` defaults to `${workspace_loc}/jsunit-coverage.json` when
omitted. Registered in `AllToolsForMCP`.

### 3.5 Output path

The JSON file is written to the Eclipse workspace root as
`jsunit-coverage.json` by default. The path is resolved via
`ResourcesPlugin.getWorkspace().getRoot().getLocation()`. No preference UI is
needed for the initial release — the MCP tool accepts an optional path override.

## 4. Implementation plan

1. **`JSUnitDebugger`** — add `reachableLines` map; populate recursively in
   `handleCompilationDone` via `DebuggableScript.getLineNumbers()` and
   `getSourceName()`.

2. **`JSUnitToJavaRunner`** — add `getReachableLines()` getter; propagate from
   the `JSUnitDebugger` instance in `runInRhino()` (same pattern as
   `lineNumbers`).

3. **`JSUnitSuite`** — add `reachableLines` field; save in `releaseScopes()`;
   add `getReachableLines()` getter (mirroring `getLineNumbers()`).

4. **`TestClientTestSuite`** — add `static JSUnitSuite lastRunSuite` field; set
   it in `suite()` after creating the instance; add `getLastRunSuite()` static
   accessor.

5. **`JSUnitCoverageWriter`** (new class in `runner/`) — static
   `write(lineNumbers, reachableLines, solutionName, outputPath)` method;
   serialises to JSON using `StringBuilder`; excludes internal JS library
   source names.

6. **`RunClientTests`** — after `runJUnitClass(port, TestClientTestSuite.class)`
   returns, read `TestClientTestSuite.getLastRunSuite()` and call
   `JSUnitCoverageWriter.write(...)` with the workspace-root path.

7. **`IJSUnitCoverageTool`** (new interface in servoypilot
   `tools/testgeneration/`) — `getJSUnitCoverageReport` and
   `suggestTestsFromCoverage` tool methods; reads JSON with Jackson;
   formats response as markdown.

8. **`AllToolsForMCP`** — add `IJSUnitCoverageTool.class` to the tool registry.

## 5. Acceptance criteria

- [ ] Running JSUnit tests in debug mode produces a valid JSON file at
      `${workspace_loc}/jsunit-coverage.json`.
- [ ] The JSON lists every solution scope and form that was loaded during the
      run with correct `coveredLines` and `uncoveredLines` arrays per function.
- [ ] `uncoveredLines` contains lines present in the compiled source but never
      executed during the test run.
- [ ] JsUnit internal library scripts are excluded from the JSON output.
- [ ] The `getJSUnitCoverageReport` MCP tool returns a readable markdown summary
      when the JSON file exists.
- [ ] The `suggestTestsFromCoverage` MCP tool returns concrete test suggestions
      for functions with uncovered lines; it does not embed raw source code.
- [ ] Both MCP tools accept an optional `coveragePath` parameter to read from a
      non-default location.
- [ ] Coverage collection does not break existing JSUnit test results or the
      JUnit view in Eclipse.

## 6. Out of scope

- Branch/function-level coverage (line-level only).
- Preference UI for the output path (deferred).
- Auto-trigger of AI analysis after each run (on-demand via MCP tool only).
- AI-generated test code automatically inserted into the solution.
- Coverage for server-side Java code executed during JSUnit runs.
- SonarQube XML output format.

## 7. Decisions

| Question | Decision |
|----------|----------|
| Where does AI response appear? | ServoyCopilot MCP tool returns text directly to the AI chat |
| Cap uncovered lines sent to AI? | `suggestTestsFromCoverage` accepts `maxFunctions` param (default 20) |
| Shared output path with standard runner? | Independent path: `${workspace_loc}/jsunit-coverage.json` |
| Which AI model? | Same as configured in ServoyCopilot (no separate config) |
| `SonarQubeCoverageFormatter` location? | Not used — JSON format written directly by `JSUnitCoverageWriter` |
| Enable/disable flag? | Not needed for MVP — written automatically on every debug-mode run |
