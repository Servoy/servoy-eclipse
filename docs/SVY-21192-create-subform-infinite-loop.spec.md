# Spec: SVY-21192 — Developer crashes on "Create new sub form" and "form - properties - extends"

## 1. Goal

Fix an infinite loop that freezes the Servoy Developer IDE when opening the "Create new sub form" wizard or changing a form's "extends" property, when the selected form belongs to a working set whose name matches a form name.

## 2. Background

### 2.1 Symptom

On SOME forms in customer solutions (reported by Deutsche Bahn / AMS solution), opening the "Create sub form" wizard or changing a form's "extends" property causes the IDE main thread to enter an infinite loop, consuming 100% CPU indefinitely. The user must kill `servoy.exe`. The issue occurs on both Windows and macOS and appears related to whether the form is in a working set.

### 2.2 Root cause analysis

The stack dump shows the main thread stuck (61+ seconds CPU, state RUNNABLE) in:

```
TreeSelectViewer.setSelection (line 727)
  → FormContentProvider.getParent (line 296)
    → ServoyResourcesProject.getContainingWorkingSet (line 267)
      → ServoyResourcesProject.getFormNames (line 244)
```

The infinite loop occurs in `TreeSelectViewer.setSelection()` lines 724-728:

```java
while (parent != null)
{
    path.add(parent);
    parent = contentProvider.getParent(parent);
}
```

**Why it loops forever:**

1. `FormContentProvider` uses both form UUIDs (Strings) and working set names (Strings) as tree elements. Forms are children of working sets.

2. `getParent(Object element)` treats ANY `String` as a form identifier:
   ```java
   if (element instanceof String elementUUID) {
       Form form = flattenedSolution.getForm(elementUUID);
       if (activeProject != null && form != null)
           return activeProject.getContainingWorkingSet(form.getName(), ...);
   }
   ```

3. `FlattenedSolution.getForm(String nameOrUUID)` resolves by UUID first, then **by name**:
   ```java
   Form frm = getIndex().getPersistByUUID(nameOrUUID, Form.class);
   if (frm != null) return frm;
   return getIndex().getPersistByName(nameOrUUID, Form.class);
   ```

4. When a working set is named the same as a form (e.g., working set "orders" contains a form also named "orders"), calling `getParent("orders")` resolves the form by name, then `getContainingWorkingSet("orders", ...)` returns `"orders"` again — creating an infinite cycle.

### 2.3 Affected code

| File | Class | Method |
|------|-------|--------|
| `com.servoy.eclipse.ui/src/.../dialogs/FormContentProvider.java` | `FormContentProvider` | `getParent()` |
| `com.servoy.eclipse.ui/src/.../views/TreeSelectViewer.java` | `TreeSelectViewer` | `setSelection()` |

## 3. Design

### 3.1 Primary fix: FormContentProvider.getParent()

Before treating a String element as a form UUID, check if it is a known working set name. Working sets are top-level elements with no parent — `getParent()` should return `null` for them.

```java
@Override
public Object getParent(Object element)
{
    if (element instanceof String elementUUID)
    {
        if (workingSetForms.containsKey(elementUUID))
        {
            return null;
        }
        ServoyResourcesProject activeProject = ServoyModelManager.getServoyModelManager()
            .getServoyModel().getActiveResourcesProject();
        Form form = flattenedSolution.getForm(elementUUID);
        if (activeProject != null && form != null)
        {
            return activeProject.getContainingWorkingSet(form.getName(),
                flattenedSolution.getSolutionNames());
        }
    }
    return null;
}
```

This is correct because:
- `workingSetForms` is a `Map<String, List<String>>` keyed by working set name
- A working set name appearing in this map is definitively a top-level group, not a form element
- This check is O(1) and adds negligible overhead

### 3.2 Defensive fix: TreeSelectViewer.setSelection() cycle detection

As a safety net against any `ITreeContentProvider.getParent()` implementation returning cycles, add a visited set to the parent-traversal loop:

```java
Object parent = contentProvider.getParent(value);
List<Object> path = new ArrayList<Object>();
path.add(value);
Set<Object> visited = new HashSet<>();
visited.add(value);
while (parent != null && visited.add(parent))
{
    path.add(parent);
    parent = contentProvider.getParent(parent);
}
```

`Set.add()` returns `false` if the element is already present, breaking the loop on any cycle.

## 4. Implementation plan

1. **Modify `FormContentProvider.getParent()`** (`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/dialogs/FormContentProvider.java`, line 286-300):
   - Add early return `null` when `workingSetForms.containsKey(elementUUID)` before attempting form resolution.

2. **Modify `TreeSelectViewer.setSelection()`** (`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/views/TreeSelectViewer.java`, lines 721-728):
   - Add a `Set<Object> visited` with cycle detection in the `while (parent != null)` loop.

3. **Organize imports** for `TreeSelectViewer.java` (add `java.util.Set`, `java.util.HashSet`).

4. **Verify** — run compilation error check and any relevant tests in `com.servoy.eclipse.tests`.

## 5. Acceptance criteria

- [ ] Opening "Create new sub form" wizard does not freeze the IDE regardless of working set configuration
- [ ] Changing a form's "extends" property does not freeze the IDE regardless of working set configuration
- [ ] Forms in working sets with matching names still display correctly in the tree selector
- [ ] Working sets still function as expected (grouping, expand/collapse)
- [ ] No regression in form selection dialogs when no working sets are defined
- [ ] The parent path traversal in `TreeSelectViewer` terminates even if a content provider has a cyclic `getParent()` implementation

## 6. Out of scope

- Preventing users from naming working sets the same as forms (this is a valid use case)
- Performance optimizations for `getContainingWorkingSet` / `getFormNames` (the loop itself iterates all working set paths for each call — could be cached but is a separate concern)
- Other potential callers of `FormContentProvider.getParent()` outside of `TreeSelectViewer`

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Can we confirm the Deutsche Bahn solution has a form with the same name as a working set? | QA / Joachim Hilgers | open |
| Should `TreeSelectViewer` log a warning when cycle detection triggers? | Developer | open |
