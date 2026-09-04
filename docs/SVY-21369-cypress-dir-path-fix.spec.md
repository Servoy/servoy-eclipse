# Spec: SVY-21369 — MCP `screenshotform` creates `.metadata` outside the workspace

## 1. Goal

Fix `FormSpecRunner.getCypressDir()` so that the Cypress working directory is created inside the workspace's own `.metadata/.plugins/` folder rather than one directory level above it. This eliminates filesystem pollution and avoids unexpected permission prompts for paths outside the workspace root.

## 2. Background

### 2.1 Eclipse workspace `.metadata` convention

Eclipse stores plugin state under `<workspace-root>/.metadata/.plugins/<bundle-id>/`. The API `ResourcesPlugin.getWorkspace().getRoot().getLocation()` returns the workspace root itself (e.g. `/Users/rene/servoy-workspace`). The `.metadata` directory is a direct child of this path.

### 2.2 Current bug

`FormSpecRunner.getCypressDir()` (line 413 of `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecRunner.java`) calls `.getParent()` on the workspace root before resolving `.metadata`:

```java
Path metadataPlugins = workspaceRoot.getParent().resolve(".metadata").resolve(".plugins");
```

This navigates UP from the workspace root (e.g. to `/Users/rene/`) and creates `.metadata` there — exactly as reported.

### 2.3 Callers affected

`getCypressDir()` is used by:
- `screenshotForm` (via `FormPreviewService`) — the MCP tool that triggered the report
- `runFormCypressTests` — form-level Cypress test execution
- `runE2ECypressTests` — E2E test execution
- `HeadlessFormTestExecutor.execute` — headless test runner

All callers will automatically benefit from the fix.

### 2.4 Git history

Introduced in commit `4b85c04c` (SVY-21025). Carried over unchanged in refactor `37a932a3` (SVY-21296) that moved `FormSpecRunner` into `com.servoy.eclipse.cypress`. The `.getParent()` was never intentional — it was a misunderstanding of what `getRoot().getLocation()` returns.

## 3. Design

### 3.1 Remove `.getParent()` from path resolution

Change line 413 from:

```java
Path metadataPlugins = workspaceRoot.getParent().resolve(".metadata").resolve(".plugins");
```

to:

```java
Path metadataPlugins = workspaceRoot.resolve(".metadata").resolve(".plugins");
```

This single-token removal restores the correct Eclipse convention path: `<workspace-root>/.metadata/.plugins/<MCP_PLUGIN_DIR>/cypress/`.

### 3.2 Add a unit test for correct path resolution

The existing `FormSpecRunnerTest` only checks that the `getCypressDir` method exists via reflection. A new test should verify the returned path contains `.metadata` as a direct child of the workspace root (i.e. does NOT contain a parent traversal). Since `getCypressDir()` calls `ResourcesPlugin` (unavailable in plain JUnit), the test should use reflection or a mock to validate the path structure logic.

## 4. Implementation plan

1. Edit `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecRunner.java` line 413: remove `.getParent()`.
2. Add a unit test in `com.servoy.eclipse.cypress.tests` that validates `getCypressDir()` returns a path whose parent chain includes `<workspace-root>/.metadata/.plugins/` (not `<workspace-root>/../.metadata/.plugins/`).
3. Verify zero compilation errors.
4. Run `FormSpecRunnerTest` to confirm all tests pass.

## 5. Acceptance criteria

- [ ] `getCypressDir()` returns a path under `<workspace-root>/.metadata/.plugins/com.servoy.eclipse.developer.mcp/cypress/`
- [ ] No `.metadata` directory is created outside the workspace root when `screenshotform`, `runFormCypressTests`, or `runE2ECypressTests` is invoked
- [ ] Existing unit tests in `com.servoy.eclipse.cypress.tests` continue to pass
- [ ] New unit test validates correct path resolution

## 6. Out of scope

- Migrating any data previously written to the incorrect location outside the workspace
- Switching to `Platform.getStateLocation()` API (considered and rejected in triage — more invasive, crosses bundle boundaries)
- Changes to callers of `getCypressDir()`

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should existing stray `.metadata` directories outside workspace be cleaned up on startup? | Product | open |
