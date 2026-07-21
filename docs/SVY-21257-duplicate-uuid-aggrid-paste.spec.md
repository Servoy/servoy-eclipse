# Spec: SVY-21257 — Duplicating AG Grid tables generates duplicate UUID errors

## 1. Goal

Fix the copy/paste of AG Grid tables (and any WebComponent with custom type children) between forms so that columns receive new UUIDs instead of retaining the original UUIDs, which causes duplicate UUID validation errors.

## 2. Background

### 2.1 How WebComponent columns are stored

AG Grid columns are stored as `WebCustomType` children of the `WebComponent`. They are persisted inside the component's JSON property as a JSON array, each entry having a `uuid` field. At runtime, `WebComponent.initCustomTypes()` reads this JSON and creates `WebCustomType` child objects, registering them in `allobjects`. The flag `customTypesInitialized` (transient, defaults to `false`) guards against re-initialization.

### 2.2 How cloneObj works for WebComponent

`WebComponent.cloneObj()` overrides the base `AbstractBase.cloneObj()` to handle UUID regeneration for custom type children:

1. Calls `super.cloneObj()` — which internally:
   a. Creates a new persist via `ChangeHandler.cloneObj()` (new UUID)
   b. Calls `initClone()` → `copyPropertiesMap()` which copies all properties (including JSON with old UUIDs) by invoking setters via introspection
   c. If `deep && this.allobjects != null`: resets `clone.allobjects` to an empty list, then loops children — but `WebCustomType` does not implement `IPersistCloneable`, so nothing is deep-cloned
2. Then visits the clone's children with `acceptVisitor()` and calls `resetUUID()` on each `WebCustomType`

### 2.3 The bug — `customTypesInitialized` stale flag

During step 1b, `copyPropertiesMap()` invokes `setTypeName()` and `setJson()` on the clone via introspection. Both methods trigger `initCustomTypes()` when `customTypesInitialized == false`. Once both typeName and JSON are set, `initCustomTypes()` succeeds:
- Creates `WebCustomType` children from JSON (with **old** UUIDs)
- Sets `customTypesInitialized = true`
- Populates `clone.allobjects`

Then in step 1c, `AbstractBase.cloneObj` unconditionally replaces `clone.allobjects` with a new empty list (line 853). This leaves the clone in an inconsistent state:
- `customTypesInitialized = true`
- `allobjects` is empty

In step 2, `clone.acceptVisitor()` calls `getAllObjectsAsList()`. The WebComponent override checks `customTypesInitialized` — since it's `true`, it does NOT call `initCustomTypes()` and returns the empty list. The visitor finds no children, `resetUUID()` is never invoked, and the JSON retains the original UUIDs.

When the form is saved, the .frm file contains duplicate UUIDs (same as the source form), triggering the builder's duplicate UUID markers.

### 2.4 Git history

The `WebComponent.cloneObj()` UUID-reset logic was introduced in 2016 (commit `4d0a323892d`). The `extendsID` / flattenedJson handling was added in commit `cc6158dea5b` (Dec 2025) for SVY-20728 ("Aggrid copy doesn't copy also the columns"). The underlying timing issue with `customTypesInitialized` likely existed before but may have been masked or less common. The recent changes made copying of override components more common, exposing the bug.

### 2.5 The `name_copy` side issue

The ticket also mentions unwanted `name_copy` suffixes. `AbstractBase.cloneObj` always appends `_copy<random>` when `changeName=true`. `ElementFactory.copyComponent` then calls `updateName()` which resets the name to the original if it's available on the target form. This part works correctly for cross-form paste when the name is free. The `name_copy` issue may appear when pasting to extended forms where `changeNames=false` but `changeName=true` is still passed to `cloneObj`.

## 3. Design

### 3.1 Reset `customTypesInitialized` before visitor traversal

In `WebComponent.cloneObj()`, after `super.cloneObj()` returns and before calling `clone.acceptVisitor()`, reset the clone's `customTypesInitialized` flag to `false`. This ensures that `getAllObjectsAsList()` inside the visitor will trigger `initCustomTypes()`, which re-creates `WebCustomType` children from the JSON (with old UUIDs). The visitor then calls `resetUUID()` on each, generating new UUIDs and updating the JSON entries.

```java
// In WebComponent.cloneObj(), before the acceptVisitor call:
wcClone.customTypesInitialized = false;
```

### 3.2 Why this is safe

- After `super.cloneObj()`, the clone's JSON property is a deep copy (ServoyJSONObject.clone() serializes/deserializes). Modifying it does not affect the original.
- `initCustomTypes()` calls `internalClearAllObjects()` first, so any stale children are removed.
- The `resetUUID()` call updates both the in-memory UUID and the JSON entry (`fullJSONInFrmFile.put(UUID_KEY, ...)`), keeping them in sync.
- The existing `fireIPersistChanged` calls after the visitor ensure the persist index is updated.

### 3.3 Extended form case (extendsID != null)

When the source component is an override, `wcClone.setProperty(PROPERTY_JSON, flattenedJson)` is called before the visitor. Since `customTypesInitialized` will now be `false`, `setJson()` will call `initCustomTypes()` (populating children from the flattened JSON with old UUIDs and setting `customTypesInitialized = true`). Then we reset it to `false` again before the visitor, OR we can place the reset AFTER the flattenedJson block. Either way, the visitor's `getAllObjectsAsList()` will re-init and the UUIDs will be reset.

The cleanest approach: place the reset immediately before `clone.acceptVisitor()`, after any JSON manipulation.

### 3.4 Duplicate columns / `name_copy` (secondary)

The duplicate column entries and `name_copy` suffixes are a side effect of the duplicate UUIDs: the builder flags the persist as problematic, and the stale `customTypesInitialized = true` state may cause double-initialization in some editor flows. Fixing the UUID issue should resolve or significantly reduce these symptoms. If `name_copy` persists after the UUID fix, it can be addressed separately by adjusting the `changeName` parameter passed to `cloneObj` in `ElementFactory.copyComponent` when pasting across forms.

## 4. Implementation plan

1. **File:** `servoy_shared/src/com/servoy/j2db/persistence/WebComponent.java` (in `servoy-client` repo)
   - In `cloneObj()`, add `wcClone.customTypesInitialized = false;` immediately before the `clone.acceptVisitor(...)` call (after the `extendsID`/flattenedJson block, around line 404).

2. **Verify** that the fix works for:
   - Copying AG Grid from form A to form B (non-extended)
   - Copying AG Grid from an extended form to a non-extended form
   - Copying AG Grid within the same form (duplicate)
   - Copying AG Grid to an extended form

3. **Test:** No existing automated test covers this scenario. A manual test reproducing the steps from the ticket should be performed:
   - Copy AG Grid table from one form, paste to another
   - Verify no duplicate UUID errors appear
   - Project clean → verify no errors reappear
   - Verify columns are intact and functional

## 5. Acceptance criteria

- [ ] Copying an AG Grid table from one form to another does not produce duplicate UUID errors
- [ ] After a project clean, no duplicate UUID errors appear for the pasted component
- [ ] Columns of the pasted AG Grid retain their properties (dataproviders, headers, widths, etc.)
- [ ] The pasted component's columns have new, unique UUIDs (different from the source)
- [ ] Copying from an extended form works correctly (columns from parent are included)
- [ ] No regression in existing copy/paste behavior for other WebComponents (tab panels, custom components)
- [ ] The component name does not get `_copy` suffix when the name is free on the target form

## 6. Out of scope

- Refactoring the `customTypesInitialized` mechanism to prevent similar state inconsistencies in other code paths
- Automated integration test (would require plugin test infrastructure with designer simulation)
- The deeper architectural question of why `AbstractBase.cloneObj` deep-clone logic clears `allobjects` for types that aren't `IPersistCloneable`

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `AbstractBase.cloneObj` skip the deep-clone block entirely when the original's children don't implement `IPersistCloneable`? | Architecture | open |
| Is the `name_copy` issue fully resolved by fixing UUIDs, or does it need a separate fix for extended form paste? | QA | open |
| Should `WebCustomType` implement `IPersistCloneable` so that `AbstractBase.cloneObj` can properly deep-clone columns? | Architecture | open |
