# Triage Report — SVY-21315

**Verdict:** PROCEED

## Reported problem

When an absolute position form contains a form component element whose "containedForm" property references a responsive form, the Outline view crashes with a `ClassCastException` and becomes permanently unavailable:

```
java.lang.ClassCastException: class com.servoy.eclipse.model.util.WebFormComponentChildType
  cannot be cast to class com.servoy.j2db.persistence.ICommonWebComponent
    at com.servoy.j2db.persistence.WebCustomType.<init>(WebCustomType.java:99)
    at com.servoy.eclipse.model.util.WebFormComponentChildType$WebFormComponentCustomType.<init>(WebFormComponentChildType.java:633)
    at com.servoy.eclipse.model.util.WebFormComponentChildType.createWebCustomType(WebFormComponentChildType.java:235)
    ...
    at com.servoy.eclipse.designer.outline.FormOutlinePage.createControl(FormOutlinePage.java:354)
```

## Root-cause assessment

The crash originates at `WebCustomType.java:99`:

```java
if (PersistHelper.isArrayOfCustomJSONObject(((ICommonWebComponent)parentWebObject).getPropertyDescription().getProperty(jsonKey).getType()))
```

This line unconditionally casts `parentWebObject` to `ICommonWebComponent`. The assumption holds for the normal case (parent is a `WebComponent` or another `WebCustomType`), but fails when the parent is a `WebFormComponentChildType`.

**Call chain:**
1. `FormOutlinePage.createControl()` → `defaultExpand()` → tree viewer calls `hasChildren()`
2. `FormOutlineContentProvider.hasChildren()` → `hasPersistChild()` → `getPersistChildrenAsList()`
3. `WebFormComponentChildType.getAllObjectsAsList()` → `getProperty()` → `convertToJavaType()`
4. `convertToJavaType()` → `createWebCustomType(this, ...)` — passes `this` (a `WebFormComponentChildType`) as parent
5. `WebFormComponentCustomType.<init>()` → `super(parentWebObject, ...)` → `WebCustomType.<init>()`
6. Line 99 casts `parentWebObject` to `ICommonWebComponent` — **crash**

`WebFormComponentChildType` implements `IBasicWebObject` but NOT `ICommonWebComponent`, even though it already has the exact method required by that interface (`PropertyDescription getPropertyDescription()`).

## Ticket premise check

The ticket correctly identifies the problem (ClassCastException in the Outline view) and does not propose a specific code solution. The premise is accurate.

## Approaches considered

1. **Add `ICommonWebComponent` to `WebFormComponentChildType`'s implements clause** — The class already has the `getPropertyDescription()` method (line 117). Adding the interface is a one-line change that satisfies the `WebCustomType` constructor's contract. Pros: minimal, correct, aligns the type hierarchy. Cons: none — this is clearly what was intended.

2. **Add a defensive `instanceof` check in `WebCustomType.<init>()`** — Guard line 99 with `if (parentWebObject instanceof ICommonWebComponent)`. Pros: prevents crash in any future case. Cons: masks the real problem (the parent should provide this interface); the code path that needs `getPropertyDescription()` would silently get wrong behaviour (skip array detection).

3. **Catch the exception in `FormOutlineContentProvider.hasChildren()`** — Wrap the `getAllObjectsAsList()` call in a try-catch. Pros: prevents outline crash. Cons: hides the bug, children won't display correctly.

4. **No code change needed** — Not viable. This is a clear regression from the SVY-20784 refactor.

## Recommendation

**Approach 1: Add `ICommonWebComponent` to `WebFormComponentChildType`'s implements clause.**

This is a one-line fix to the class declaration in `/com.servoy.eclipse.model/src/com/servoy/eclipse/model/util/WebFormComponentChildType.java`:

```java
// Before:
public class WebFormComponentChildType extends BaseComponent implements IBasicWebObject, IParentOverridable

// After:
public class WebFormComponentChildType extends BaseComponent implements IBasicWebObject, IParentOverridable, ICommonWebComponent
```

The method `getPropertyDescription()` already exists at line 117-120 and returns the correct `PropertyDescription` for this type. No additional code is needed.

## Git history findings

- **Commit `0183d6c74f`** (SVY-20784, "refactor the flattened stuff in for the WebCustomType", 2026-05-08, lvostinar): This commit introduced the `ICommonWebComponent` interface and added the cast at `WebCustomType.java:99`. It modified `WebFormComponentChildType.java` but did not add `ICommonWebComponent` to its implements clause — an oversight, since `WebFormComponentChildType.createWebCustomType()` passes `this` as the parent to `WebCustomType`'s constructor.
