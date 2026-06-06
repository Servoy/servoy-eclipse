# Spec: SVY-21157 — ClassCastException when trying to open a calculation

## 1. Goal

Fix a `ClassCastException` that occurs when double-clicking a newly created calculation in the IDE. The error prevents the calculation from being opened/saved and crashes the "Save solution data" job.

## 2. Background

In Servoy Developer, calculations are persisted as `ScriptCalculation` objects. When a persist is serialized (e.g. on save after creation), `SolutionSerializer` generates JSDoc comments including `@param` tags. The `generateParams` method was written assuming it would only receive `ScriptMethod` instances, but it is also called for `ScriptCalculation` instances via `replacePropertiesTag` and `generateDefaultJSDoc`.

Class hierarchy:
- `AbstractScriptProvider` ← `ScriptCalculation` (NOT a `ScriptMethod`)
- `AbstractScriptProvider` ← `ScriptMethod`

Both share `AbstractScriptProvider` as a parent, but `ScriptCalculation` does not extend `ScriptMethod`.

## 3. Design

### 3.1 Root cause

In `SolutionSerializer.generateParams()` at line 1100:

```java
ScriptMethod parentMethod = PersistHelper.getOverridenMethod((ScriptMethod)abstractScriptProvider);
```

This unconditionally casts the `AbstractScriptProvider` parameter to `ScriptMethod`. When a `ScriptCalculation` is passed in (which happens during serialization of a newly created calculation), a `ClassCastException` is thrown.

The call path is:
1. `EclipseRepository.updateNodes()` → `SolutionSerializer.writePersist()`
2. → `serializePersist()` → `replacePropertiesTag()`
3. → `generateParams()` (line 1033) or `generateDefaultJSDoc()` → `generateParams()` (line 1071)

### 3.2 Git history / how this was introduced

**Commit:** `1339de257ce` (Diana Bunaciu, 2026-03-05)
**Jira:** SVY-20909 — "Overriding methods from base form does not copy JSDOC"

The original `generateParams` code (from 2015, lvostinar) simply iterated over the
provider's arguments and wrote `@param` tags — no casting involved. The SVY-20909 change
added logic to inherit parameter types from a parent method (for form-level overrides).
This required calling `PersistHelper.getOverridenMethod((ScriptMethod)...)` to find the
parent. However, the method signature still accepts `AbstractScriptProvider`, and the
author overlooked that `ScriptCalculation` is also an `AbstractScriptProvider` but does
NOT extend `ScriptMethod`.

**Conclusion:** This was an unintentional oversight. The parent-type-inheritance feature
is only meaningful for `ScriptMethod` overrides — calculations cannot override methods.
The `instanceof` guard is safe and does not break SVY-20909 behavior.

### 3.3 Fix

Guard the cast with an `instanceof` check. Calculations cannot override methods, so the parent-method lookup should be skipped entirely when the provider is not a `ScriptMethod`.

Replace line 1100:
```java
ScriptMethod parentMethod = PersistHelper.getOverridenMethod((ScriptMethod)abstractScriptProvider);
```

With:
```java
ScriptMethod parentMethod = abstractScriptProvider instanceof ScriptMethod
    ? PersistHelper.getOverridenMethod((ScriptMethod)abstractScriptProvider)
    : null;
```

This is consistent with how `generateDefaultJSDoc` already handles this at line 1085:
```java
if (abstractScriptProvider instanceof ScriptMethod && PersistHelper.getOverridenMethod((ScriptMethod)abstractScriptProvider) != null)
```

## 4. Implementation plan

1. Edit `SolutionSerializer.java` line 1100 in `com.servoy.eclipse.model` to add the `instanceof` guard.
2. Verify no compilation errors.
3. Test by creating a new calculation and double-clicking it — no exception should occur.

## 5. Acceptance criteria

- [ ] Double-clicking a newly created calculation no longer throws a `ClassCastException`
- [ ] The "Save solution data" job completes without error for solutions containing calculations
- [ ] Existing behavior for `ScriptMethod` serialization (including override/param inheritance) is unchanged

## 6. Out of scope

- Refactoring `generateParams` to use a common interface instead of casting
- Changes to `ScriptCalculation` class hierarchy

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should calculations ever support parameter JSDoc generation? | Developer team | open |
