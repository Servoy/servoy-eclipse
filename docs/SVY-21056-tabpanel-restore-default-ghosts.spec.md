# Spec: SVY-21056 — Restore default value on tabpanel tab element causes NPE

## 1. Goal

Fix the "Restore Default Value" action on an individual tab element within a tabpanel's `tabs` array property. Currently it produces a `NullPointerException` because the reset logic returns `null` as the default value for custom object array elements. The fix should create a proper default `WebCustomType` instance (like `toggleValue` does) instead of inserting null into the array.

## 2. Background

### 2.1 How tabs are stored

A tabpanel WebComponent (e.g., `bootstrapcomponents-tabpanel`) stores its `tabs` as an array of `WebCustomType` child objects maintained in both the persist tree (`allobjects`) and the JSON model.

### 2.2 Restore Default flow for an individual tab element

When the user selects a specific tab (e.g., `tabs[1]`) and clicks "Restore Default Value":

1. `ResetValueCommand.redo()` calls `target.resetPropertyValue(propertyName)` (the `else` branch, since `CustomArrayPropertySource` is not a `ComplexPropertySourceWithStandardReset`)
2. `CustomArrayPropertySource.resetPropertyValue(id)` → `PersistPropertySource.adjustPropertyValueAndReset()`
3. Delegation chain: `ArrayItemPropertyDescriptorWrapper.resetPropertyValue()` → `CustomObjectTypePropertyController.resetPropertyValue()` → `CustomArrayPropertySource.defaultResetProperty(id)`
4. `defaultResetProperty(id)` calls `defaultSetProperty(id, getDefaultElementProperty(id))`
5. **`getDefaultElementProperty(id)`** returns `getArrayElementPD().getDefaultValue()` which is **null** for custom object types (tabs have no declared default in the spec file)
6. `defaultSetElement(null, idx)` sets `array[idx] = null`
7. The null propagates to `PersistHelper.setWebComponentProperty` which calls `webComponent.addChild(null)` → NPE

### 2.3 How `toggleValue` correctly creates a default object

`CustomObjectTypePropertyController.toggleValue()` (lines 190-230) already knows how to create a proper default `WebCustomType` for array elements:

```java
if (id instanceof ArrayPropertyChildId arrayPropertyChildId)
{
    parentKey = String.valueOf(arrayPropertyChildId.arrayPropId);
    indexInArray = arrayPropertyChildId.idx;
}
// ...
newPropertyValue = WebCustomType.createNewInstance(parent, propertyDescription, parentKey, indexInArray);
typeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(propertyDescription.getType());
newPropertyValue.setTypeName(typeName);
```

This creates a fresh, empty `WebCustomType` instance with the correct parent, property description, and array index. This is exactly what "Restore Default" should produce for a custom object array element.

### 2.4 Ghost refresh (already fixed)

The ghost refresh after restoring default on the entire `tabs` node is already working correctly. This spec only addresses the individual tab element reset.

## 3. Design

### 3.1 Root cause

`CustomArrayPropertySource.getDefaultElementProperty(Object id)` at `CustomArrayTypePropertyController.java:346` returns null for custom object array elements:

```java
protected Object getDefaultElementProperty(Object id)
{
    return getArrayElementPD().getDefaultValue();  // null for custom types
}
```

### 3.2 Proposed fix: Create a default WebCustomType instance on reset

Override `getDefaultElementProperty(Object id)` in `CustomArrayPropertySource` (inside `CustomArrayTypePropertyController`) so that when the element property descriptor's default is null AND the element is a custom object type, it creates a new `WebCustomType` instance using the same logic as `CustomObjectTypePropertyController.toggleValue()`.

**Approach:** In `CustomArrayPropertySource.getDefaultElementProperty(id)`:
1. Call `getArrayElementPD().getDefaultValue()` as before
2. If the result is null and the element type is a custom object (check via `propertyDescription`), create a fresh `WebCustomType` using `WebCustomType.createNewInstance(parent, propertyDescription, parentKey, indexInArray)`
3. Set the type name on the new instance
4. Return the new instance as the default value

This mirrors the `toggleValue` logic: when the id is an `ArrayPropertyChildId`, always create a fresh instance rather than returning null.

### 3.3 The reset then works normally

With a proper `WebCustomType` default value:
1. `defaultResetProperty(id)` → `defaultSetProperty(id, newDefaultInstance)`
2. `defaultSetElement(newDefaultInstance, idx)` sets the array slot to the fresh instance
3. The array (with a valid object at that index) propagates normally through `PersistHelper.setWebComponentProperty`
4. `webComponent.addChild(newDefaultInstance)` works correctly — no NPE

## 4. Implementation plan

1. **`com.servoy.eclipse.ui/src/.../CustomArrayTypePropertyController.java`** — In the inner class `CustomArrayPropertySource`, override `getDefaultElementProperty(Object id)`: when `getArrayElementPD().getDefaultValue()` returns null and the element is a custom object type, create a new `WebCustomType` instance using `WebCustomType.createNewInstance(parent, propertyDescription, parentKey, indexInArray)` and set its type name, following the same pattern as `CustomObjectTypePropertyController.toggleValue()`.

2. **Test scenarios:**
   - Restore default on an individual tab: tab is reset to empty default state, no NPE, designer refreshes
   - Restore default on entire `tabs` array: all tabs removed (existing behavior, already working)
   - Undo after restore default on a tab: previous tab state is restored correctly

## 5. Acceptance criteria

- [ ] Restoring default on an individual tab element replaces it with a fresh default `WebCustomType` (no null insertion)
- [ ] No `NullPointerException` during or after the restore default operation
- [ ] The form designer refreshes correctly after the tab is reset (ghosts update)
- [ ] The property sheet shows the reset tab with default/empty properties
- [ ] Undo correctly restores the previous tab state
- [ ] Restoring default on the entire `tabs` property still works (deletes all tabs)

## 6. Out of scope

- Adding null guards in `PersistHelper.setWebComponentProperty` or `AbstractBase.internalAddChild` (defensive, but not the root fix)
- Changes to the ghost refresh mechanism (already working)
- Removing elements from the array (the correct behavior is to reset to a default object, not remove)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the default instance have any properties pre-populated from the spec's property defaults, or just be an empty shell? | Developer | open |
