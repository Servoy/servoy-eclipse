# Triage Report — SVY-21369

**Verdict:** PROCEED

## Reported problem

The `screenshotForm` MCP tool creates a `.metadata` folder (and its children `.plugins/com.servoy.eclipse.developer.mcp/cypress/`) one directory level above the Eclipse workspace root, instead of inside the workspace's own `.metadata` directory. This pollutes the user's filesystem with unexpected directories and can trigger extra permission checks since the path is outside the normal working root.

## Root-cause assessment

The bug is in `FormSpecRunner.getCypressDir()` at line 413 of `com.servoy.eclipse.cypress/src/com/servoy/eclipse/cypress/services/FormSpecRunner.java`:

```java
public Path getCypressDir()
{
    Path workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
    Path metadataPlugins = workspaceRoot.getParent().resolve(".metadata").resolve(".plugins");
    return metadataPlugins.resolve(MCP_PLUGIN_DIR).resolve(CYPRESS_DIR);
}
```

`ResourcesPlugin.getWorkspace().getRoot().getLocation()` returns the workspace root itself (e.g. `/Users/rene/servoy-workspace`). The `.metadata` directory is a direct child of this root (`/Users/rene/servoy-workspace/.metadata/`).

The erroneous `.getParent()` call navigates UP to `/Users/rene/`, and then `.resolve(".metadata")` creates the `.metadata` directory there — exactly as reported.

This bug was introduced in the very first commit of `FormSpecRunner.java` (commit `4b85c04c`, SVY-21025) and has persisted since.

## Ticket premise check

The ticket correctly identifies the symptom: `.metadata` created outside the workspace. It correctly states the fix should place it inside the workspace `.metadata`. The premise is fully valid.

## Approaches considered

1. **Remove `.getParent()` from line 413** — Change `workspaceRoot.getParent().resolve(".metadata")` to `workspaceRoot.resolve(".metadata")`. Single character-level fix, directly addresses the root cause.
   - Pros: Minimal change, obvious correctness, matches standard Eclipse `.metadata/.plugins/<id>/` convention.
   - Cons: None.

2. **Use Eclipse `getStateLocation()` API** — Replace the manual path construction with `Platform.getStateLocation(bundle)` or plugin activator's `getStateLocation()`.
   - Pros: More idiomatic Eclipse code, framework handles path resolution.
   - Cons: `FormSpecRunner` uses `MCP_PLUGIN_DIR` (a different bundle ID than its own), so using `getStateLocation()` would change the target directory or require getting another plugin's activator. More invasive than needed.

3. **No code change** — The tool still works (it just writes to the wrong place).
   - Pros: None.
   - Cons: Pollutes filesystem, triggers permission issues, creates confusion for users.

## Recommendation

Approach 1: Remove `.getParent()` on line 413 of `FormSpecRunner.getCypressDir()`. The fix is:

```java
Path metadataPlugins = workspaceRoot.resolve(".metadata").resolve(".plugins");
```

This is a one-line fix with zero risk of side effects. All callers of `getCypressDir()` (screenshot, Cypress spec running, `ensureCypressInstalled`) will automatically resolve to the correct workspace-internal path.

## Git history findings

- Introduced in commit `4b85c04c` (SVY-21025, "add spec.js generator where the playwright tests will be") — the `.getParent()` pattern was present from the very first version.
- Later refactored in `37a932a3` (SVY-21296, "extract Cypress testing into standalone plugin") which moved `FormSpecRunner` from the MCP bundle to `com.servoy.eclipse.cypress` — the bug was carried over unchanged.
- No evidence this was intentional; it was a misunderstanding of what `getRoot().getLocation()` returns.
