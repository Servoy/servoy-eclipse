# Spec: SVY-21176 — QBSelect case condition broken in 2026.06

## 1. Goal

Fix the `QBSelect.case` property so it is correctly exposed as a read-only property in the IDE's code completion and validation, rather than being hidden and replaced by the raw `js_case()` method.

## 2. Background

### 2.1 How properties are exposed from Java to JavaScript

The Servoy scripting layer uses `InstanceJavaMembers` (in `servoy_shared`) to map Java methods to JavaScript-visible names. Methods prefixed with `js_` are renamed by stripping the prefix (e.g. `js_case` → `case`). Methods annotated with `@JSReadonlyProperty` are additionally converted into `BeanProperty` objects (getter-only properties).

The `QBSelect.js_case()` method is unique in the codebase: it has **both** a `js_` prefix AND a `@JSReadonlyProperty(property = "case")` annotation. This dual qualification creates a problematic interaction in `InstanceJavaMembers.extractBeaning()`.

### 2.2 How the IDE resolves types

The `TypeCreator.fill()` method in `com.servoy.eclipse.debug` calls `ScriptObjectRegistry.getJavaMembers(QBSelect.class, null)` to get the `InstanceJavaMembers`, then iterates `getIds(false)` to discover all visible members. It removes entries listed in `getGettersAndSettersToHide()` before presenting them to the user.

### 2.3 Root cause

In `InstanceJavaMembers.extractBeaning()`:

1. **First loop** (line 118-191): `"js_case"` is renamed to `"case"` via `putNewValueMergeForDuplicates` because `Ident.checkIfJavascriptKeyword("case")` returns `false` ("case" is not in the `js_keywords` set).

2. **Second loop** (line 213-261): The entry at key `"case"` (now a `NativeJavaMethod`) is found to have `@JSReadonlyProperty`. A `BeanProperty("case", njm)` is created and placed under key `"case"` in the map. The displaced `NativeJavaMethod` is put back at key `"js_case"` (its original `getFunctionName()`). Then `addMethodToHide("js_case")` is called.

3. **The bug** — `addMethodToHide` (line 317-328) strips the `"js_"` prefix:
   ```java
   if (newName.startsWith("js_"))
       newName = newName.substring(3);
   gettersAndSettersToHide.add(newName); // adds "case" !
   ```

4. **Result**: `getGettersAndSettersToHide()` returns `["case"]`. The `TypeCreator` removes `"case"` from the visible members list, hiding the property. Only `"js_case"` (the NativeJavaMethod) remains visible and is shown as a method.

### 2.4 Why `addMethodToHide` strips `js_`

The `js_` stripping exists for standard bean properties (lines 206-207). When a `js_getXyz`/`js_setXyz` pair is detected as a bean, the getter/setter methods should be hidden. After the first loop renames `"js_getXyz"` to `"getXyz"` in the members map, the Method's `getName()` still returns `"js_getXyz"`, so stripping is necessary to match the map key.

However, at line 255 the `functionName` is `"js_case"` which is the **actual key** in the members map (it was put back at line 253 under that key). Stripping it produces `"case"` which is the key of the BeanProperty — incorrectly hiding the property.

## 3. Design

### 3.1 Add `stripJsPrefix` parameter to `addMethodToHide`

Change the signature of `addMethodToHide` from:

```java
protected void addMethodToHide(String name)
```

to:

```java
protected void addMethodToHide(String name, boolean stripJsPrefix)
```

When `stripJsPrefix` is `true`, the method strips the `"js_"` prefix before adding to the hide list (existing behaviour). When `false`, the name is added as-is.

### 3.2 Update call sites

1. **Lines 206-207** (bean property getter/setter hiding): pass `true` — these methods have names like `js_getXyz` but the map key is `getXyz`, so stripping is needed.

   ```java
   addMethodToHide(beanProperty.getGetter().getName(), true);
   addMethodToHide(beanProperty.getSetter().getName(), true);
   ```

2. **Line 255** (`@JSReadonlyProperty` displaced method hiding): pass `false` — `functionName` is already the actual key in the members map (e.g. `"js_case"`). Stripping would produce `"case"` which incorrectly hides the BeanProperty.

   ```java
   addMethodToHide(functionName, false);
   ```

### 3.3 Why a parameter rather than inlining

A parameter keeps the logic centralised in one method and makes the intent explicit at each call site. It avoids duplicating the null-check and list creation logic.

## 4. Implementation plan

1. **Modify** `servoy_shared/src/com/servoy/j2db/scripting/InstanceJavaMembers.java`: add a `boolean stripJsPrefix` parameter to `addMethodToHide`.

2. **Update call sites**: lines 206-207 pass `true`, line 255 passes `false`.

3. **Verify** in the IDE that `query.case` is visible as a property in code completion and does not produce a warning.

4. **Verify** that `query.js_case()` does NOT appear in code completion (is hidden).

5. **Verify** that standard bean properties with `js_` prefixed getter/setter methods still work correctly.

4. **Verify** that standard bean properties with `js_` prefixed getters/setters (if any exist) still work correctly — their getter/setter methods remain hidden.

## 5. Acceptance criteria

- [ ] `query.case` appears as a read-only property in code completion for QBSelect
- [ ] No validation warning when using `query.case` in a script
- [ ] `js_case()` does NOT appear in code completion
- [ ] At runtime, `query.case` returns a QBCase instance
- [ ] No regression in other `@JSReadonlyProperty` properties (e.g. `query.columns.x.not`, `query.columns.x.isNull`)
- [ ] No regression in standard bean properties with `js_` prefixed getter/setter methods

## 6. Out of scope

- Adding "case" to `js_keywords` (it is intentionally excluded since it works as a property access in Servoy's scripting context)
- Refactoring the entire `extractBeaning()` method or splitting it into smaller methods

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Was this working in 2026.03 via a different code path, or has the stripping logic always been present? If always present, was there a masking mechanism that was removed? | Developer | open |
| Are there any other `js_` prefixed methods with `@JSReadonlyProperty` besides `js_case()`? | Developer | resolved — only `js_case()` has both |
