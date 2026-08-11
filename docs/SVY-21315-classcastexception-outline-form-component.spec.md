# Spec: SVY-21315 — ClassCastException in Outline view when form component references a responsive form

## 1. Goal

Fix a `ClassCastException` that crashes the Outline view when an absolute-position form contains a form component whose `containedForm` property references a responsive form. The crash makes the Outline view permanently unavailable for the affected form.

## 2. Background

### 2.1 The ICommonWebComponent interface

`ICommonWebComponent` (in `servoy_shared`) is a single-method interface introduced in the SVY-20784 refactor:

```java
public interface ICommonWebComponent {
    PropertyDescription getPropertyDescription();
}
```

The `WebCustomType` constructor casts its `parentWebObject` parameter to `ICommonWebComponent` at line 99 to access `getPropertyDescription()`.

### 2.2 WebFormComponentChildType

`WebFormComponentChildType` is a virtual persist that represents a child element within a form component. Its inner class `WebFormComponentCustomType` extends `WebCustomType`, and the `createWebCustomType()` method passes `this` (the `WebFormComponentChildType` instance) as the parent.

The class already implements `getPropertyDescription()` (line 117–120) and returns the correct `PropertyDescription`. However, it does not declare `ICommonWebComponent` in its `implements` clause — an oversight from the SVY-20784 refactor.

### 2.3 Git history

Commit `0183d6c74f` (SVY-20784, 2026-05-08, lvostinar) introduced `ICommonWebComponent` and added the cast in `WebCustomType`. It modified `WebFormComponentChildType` but did not add `ICommonWebComponent` to its implements list, even though `createWebCustomType()` passes `this` as the parent to the `WebCustomType` constructor.

## 3. Design

### 3.1 Add ICommonWebComponent to the implements clause

The fix is to add `ICommonWebComponent` to `WebFormComponentChildType`'s class declaration. The method `getPropertyDescription()` already satisfies the interface contract — no additional code is needed.

**Before:**
```java
public class WebFormComponentChildType extends BaseComponent implements IBasicWebObject, IParentOverridable
```

**After:**
```java
public class WebFormComponentChildType extends BaseComponent implements IBasicWebObject, IParentOverridable, ICommonWebComponent
```

An import for `com.servoy.j2db.persistence.ICommonWebComponent` must be added (though it is likely already imported transitively or available in the same dependency graph).

## 4. Implementation plan

1. Open `com.servoy.eclipse.model/src/com/servoy/eclipse/model/util/WebFormComponentChildType.java`
2. Add `ICommonWebComponent` to the `implements` clause on line 58
3. Add the import for `com.servoy.j2db.persistence.ICommonWebComponent` if not already present
4. Verify no compilation errors

## 5. Acceptance criteria

- [ ] `WebFormComponentChildType` implements `ICommonWebComponent`
- [ ] Opening the Outline view for an absolute-position form containing a form component that references a responsive form no longer throws `ClassCastException`
- [ ] The Outline view correctly displays the children of such form components
- [ ] No compilation errors in the `com.servoy.eclipse.model` plugin
- [ ] Existing form component behaviour (non-responsive contained forms) is unaffected

## 6. Out of scope

- Defensive `instanceof` checks in `WebCustomType.<init>()` — the type hierarchy fix is the correct solution
- Changes to `FormOutlineContentProvider` error handling

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| None | — | — |
