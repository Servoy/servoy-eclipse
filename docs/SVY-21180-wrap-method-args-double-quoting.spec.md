# Spec: SVY-21180 — Weird string concatenation issue in 2026.06

## 1. Goal

Fix a regression where `solutionModel.wrapMethodWithArguments()` double-quotes string arguments that were constructed via variable concatenation. When a handler argument is built as `'"' + someVariable + '"'`, the handler receives the raw quoted string (e.g. `"testWrapped"`) instead of the expected unquoted value (`testWrapped`). This worked correctly in 2026.03.

## 2. Background

### How `wrapMethodWithArguments` works

1. User calls `solutionModel.wrapMethodWithArguments(method, [null, handlerArgs])` where `handlerArgs = '"testWrapped"'`
2. This creates a `JSMethodWithArguments` holding the raw argument array
3. When assigned to an event via `setHandler()`, arguments are stored via `AbstractBase.putMethodParameters()` into the persist's JSON custom properties

### The regression — Root Cause

The bug is in **Rhino's `NativeArray.unwrap()`** method. In the Servoy fork, `NativeArray` implements `Wrapper`, and its `unwrap()` method returns a raw `Object[]` without flattening `ConsString` elements.

The coercion path when JS calls `wrapMethodWithArguments(method, [null, handlerArgs])`:

1. Rhino's `MemberBox.wrapArgsInternal()` (varargs path) calls `Context.jsToJava(NativeArray, Object[])` 
2. `coerceTypeImpl(Object[], NativeArray)` is invoked
3. `getJSTypeCode(NativeArray)` returns `JSTYPE_JAVA_OBJECT` (because NativeArray is a `Wrapper`)
4. The JSTYPE_JAVA_OBJECT case calls `NativeArray.unwrap()` → returns raw `Object[]` with ConsString elements
5. `type.isInstance(Object[])` → `true` → returns the array **as-is** without per-element coercion
6. ConsString leaks into `JSMethodWithArguments.arguments`

The alternative path (JSTYPE_OBJECT case at line 687) would have done per-element coercion via `coerceTypeImpl(Object_component, element)` which properly flattens ConsString via the JSTYPE_STRING case. But that path is never reached because NativeArray is classified as JSTYPE_JAVA_OBJECT.

### Why this is a regression from 2026.03

The SVY-20904 update (Rhino 1.9.0, April 2026) changed how/when `ConsString` is produced during string concatenation. In the old Rhino, variable concatenation (`'"' + variable + '"'`) may have produced a plain `String` (possibly due to compiler optimizations or different `ScriptRuntime.add()` behavior). In Rhino 1.9.0, it consistently produces `ConsString` for any runtime concatenation involving non-literals.

Since `NativeArray.unwrap()` never flattened ConsString (it was never needed before because ConsString didn't appear in this path), the new behavior exposes the gap.

### Key difference: literal vs variable concatenation

- `'"' + 'testWrapped' + '"'` → plain String (compile-time optimization) → works
- `'"' + variable + '"'` → ConsString (runtime) → broken

## 3. Design

### 3.1 Fix in `NativeArray.unwrap()`

Flatten `ConsString` elements to `String` before returning the array from `unwrap()`:

```java
// In NativeArray.unwrap(), when reading elements:
Object o = get(index, this);
if (o instanceof ConsString) o = o.toString();
```

This ensures that regardless of how the coercion path processes the unwrapped array, ConsString elements are always proper Strings.

## 4. Implementation plan

1. **`rhino/src/main/java/org/mozilla/javascript/NativeArray.java`** — In `unwrap()` method (line 257): add `if (o instanceof ConsString) o = o.toString();` after `Object o = get(index, this);`
2. **`j2db_test/src/com/servoy/j2db/util/UtilsTest.java`** — Add `testParseJSExpressionWithCharSequence()` test verifying that `parseJSExpression` handles CharSequence inputs (defensive test for downstream code)

## 5. Acceptance criteria

- [ ] `solutionModel.wrapMethodWithArguments()` with a string argument built via variable concatenation delivers the correct unquoted value to the handler
- [ ] `solutionModel.wrapMethodWithArguments()` with a string argument built via literal concatenation continues to work as before
- [ ] `NativeArray.unwrap()` returns flattened Strings, not ConsStrings
- [ ] No regression in event handler argument passing for NG Client

## 6. Out of scope

- Changes to `coerceTypeImpl` ordering (risky, could have side effects)
- Changes to `Utils.parseJSExpression` (not in the actual code path for this sample)
- The secondary issue about `cssPosition` in the sample solution
- Broader audit of ConsString leaks in other paths

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Exact change in Rhino 1.9.0 that causes ConsString to appear where String was produced before (cannot verify without old code) | Dev | won't fix — root cause understood, fix applied |
| Should `NativeArray.unwrap()` also unwrap other Wrapper types inside the array? | Dev | open |
