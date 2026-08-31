# Triage Report — SVY-21338

**Verdict:** PROCEED

## Reported problem

When a form's `dataSource` property is reset via right-click → "Restore Default Value" in the Properties view (with the form selected in Solution Explorer), the UI does not update immediately. The property still shows the old datasource value. The user must click away to another node and back to see the change reflected. The underlying model *is* updated correctly — only the visual refresh is broken.

## Root-cause assessment

The bug is in `RetargetToEditorPersistProperties.resetPropertyValue()` (`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/property/RetargetToEditorPersistProperties.java:73-82`).

This method uses `Display.getCurrent().asyncExec()` to schedule the actual property reset:

```java
public void resetPropertyValue(final Object id) {
    Display.getCurrent().asyncExec(new Runnable() {
        public void run() {
            updateProperty(false, id, null);
        }
    });
}
```

The `asyncExec` defers execution to *after* the current event processing completes. But the caller — `PropertySheetEntry.resetPropertyValue()` (`PropertySheetEntry.java:724-761`) — calls `refreshFromRoot()` *immediately* after `resetPropertyValue()` returns:

```java
source.resetPropertyValue(descriptor.getId()); // schedules async — does NOT execute yet
change = true;
...
if (change) {
    refreshFromRoot(); // reads the OLD value because the reset hasn't run yet
}
```

So the refresh reads the still-unchanged model value, and the UI appears stuck.

Notably, `setPropertyValue()` in the same class was changed from `asyncExec` to `syncExec` in commit `36a1864c9bc` (Rob Gansevles, 2024-03-15, SVY-18860) to fix exactly this kind of ordering/refresh problem. But `resetPropertyValue()` was missed in that refactoring — it still has the original `asyncExec` from the 2010 initial commit.

## Ticket premise check

The ticket describes the symptom accurately and proposes no specific solution. The premise is correct: this is a real Servoy IDE bug in the properties view refresh after "Restore Default Value".

## Approaches considered

1. **Change `asyncExec` to `syncExec` in `RetargetToEditorPersistProperties.resetPropertyValue()`** — This aligns `resetPropertyValue()` with the pattern already established for `setPropertyValue()` since SVY-18860. When called from the UI thread, `syncExec` executes the Runnable inline (synchronously), so the model is updated *before* the caller proceeds to refresh. Minimal, surgical change.
   - Pros: One-line fix, mirrors the existing `setPropertyValue()` pattern, directly addresses root cause, very low risk.
   - Cons: None identified.

2. **Add a post-reset refresh via `asyncExec` in the caller** — Instead of fixing the timing, schedule an additional `refreshFromRoot()` after the async reset completes.
   - Pros: Does not change the `asyncExec` behavior.
   - Cons: Adds complexity, band-aid fix, doesn't address the underlying timing inconsistency with `setPropertyValue()`.

3. **No code change** — The value does update on re-selection, so it's cosmetic.
   - Pros: Zero risk.
   - Cons: Clearly a usability bug; "Restore Default Value" should give immediate visual feedback.

## Recommendation

**Approach 1: Change `asyncExec` to `syncExec`** in `RetargetToEditorPersistProperties.resetPropertyValue()`. This is a one-line change that brings `resetPropertyValue()` in line with `setPropertyValue()`, which was already fixed for the same class of problem in SVY-18860. The fix is low-risk and directly addresses the root cause.

## Git history findings

- **Commit `0d078765309`** (Jan Blok, 2010-06-05, "first commit"): Both `resetPropertyValue()` and `setPropertyValue()` originally used `asyncExec`.
- **Commit `36a1864c9bc`** (Rob Gansevles, 2024-03-15, "SVY-18860"): `setPropertyValue()` was refactored from `asyncExec` to `syncExec` as part of a major Solution Explorer property handling fix. `resetPropertyValue()` was **not** updated — this is the oversight that causes SVY-21338.
