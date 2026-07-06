# Spec: SVY-21154 â Fix warning thrown by aggrid and viewFoundset

## 1. Goal

Eliminate the spurious type-mismatch warning that fires when assigning a `ViewFoundSet` to a component's foundset property (e.g. `elements.table.myFoundset.foundset = viewFs;`). The IDE should recognize `ViewFoundSet` as assignment-compatible with `JSFoundSet` since `ViewFoundSet` extends `FoundSet` at the Java class level.

## 2. Background

### 2.1 Runtime class hierarchy

In the Servoy runtime (`servoy_shared`):

```
FoundSet  (scriptingName = "JSFoundSet")
  âââ ViewFoundSet  (VIEW_FOUNDSET = "ViewFoundSet")
```

`ViewFoundSet extends FoundSet`, so at runtime a view foundset IS-A foundset.

### 2.2 IDE type system (DLTK)

The IDE uses a custom DLTK-based type system where types are registered via `IScopeTypeCreator` implementations in `TypeCreator.java`:

- `FoundSetCreator` registers the `JSFoundSet` type (line 524)
- `ViewFoundSetCreator` registers the `ViewFoundSet` type (line 526)

These are registered as **separate top-level types** without a super-type relationship between them. There is no `type.setSuperType(getType(context, FoundSet.JS_FOUNDSET))` call when the base `ViewFoundSet` type is created.

### 2.3 Type compatibility validation

`TypeInfoValidator.java` (in `org.eclipse.dltk.javascript.core`) performs assignment validation at lines 2186â2208. It calls `declaredType.isAssignableFrom(assignedType)` and `assignedType.isAssignableFrom(declaredType)`. If both return non-TRUE, the warning `AssignmentNotFollowingDeclaredType` is reported.

### 2.4 The problem

When a component (e.g. aggrid) declares its foundset sub-property as type `JSFoundSet`, and the user assigns a `ViewFoundSet<view:xxx>` to it, the type checker sees two unrelated types and reports:

```
The type ViewFoundSet<view:vw_patterns> that is being assigned does not follow the declared type JSFoundSet of foundset
```

### 2.5 How component foundset types are resolved

In `ElementResolver.FoundsetTypeNameCreator` (line 837), the form's foundset type is correctly resolved to `ViewFoundSet<datasource>` for view datasources. The issue is that the component's `.foundset` property has a declared type of `JSFoundSet` and the DLTK type system doesn't know `ViewFoundSet` is a subtype of `JSFoundSet`.

## 3. Design

### 3.1 Establish ViewFoundSet â JSFoundSet super-type relationship

The root cause is that `ViewFoundSetCreator.createType()` (line 3366) creates the base `ViewFoundSet` type without setting its super type to `JSFoundSet`. The fix is to set the super type of the `ViewFoundSet` base type to `JSFoundSet`, mirroring the Java class hierarchy.

In `TypeCreator.ViewFoundSetCreator.createType()` at line 3371, after creating the base type via `createBaseType(context, fullTypeName, ViewFoundSet.class)`, set its super type:

```java
Type type = createBaseType(context, fullTypeName, ViewFoundSet.class);
type.setSuperType(getType(context, FoundSet.JS_FOUNDSET));
return addType(null, type);
```

This ensures that `JSFoundSet.isAssignableFrom(ViewFoundSet)` returns `TypeCompatibility.TRUE` (since the DLTK type system walks the super-type chain during `isAssignableFrom` checks).

### 3.2 Handle parameterized ViewFoundSet types

For parameterized types like `ViewFoundSet<view:xxx>`, the type returned by `getCombinedTypeWithRelationsAndDataproviders()` at line 3435 already has `getType(context, ViewFoundSet.VIEW_FOUNDSET)` as its super type. Since that base `ViewFoundSet` type will now itself have `JSFoundSet` as super type, the transitivity of `isAssignableFrom` should make `ViewFoundSet<view:xxx>` assignable to `JSFoundSet<datasource>` and to the plain `JSFoundSet` type.

### 3.3 MenuFoundSet consideration

`MenuFoundSet` (which also extends `FoundSet`) might need similar treatment. However, it's currently commented out (`//addScopeType(MenuFoundSet.MENU_FOUNDSET, new MenuFoundSetCreator())`), so it's out of scope for now.

## 4. Implementation plan

1. **Modify `TypeCreator.ViewFoundSetCreator.createType()`** in `com.servoy.eclipse.debug/src/com/servoy/eclipse/debug/script/TypeCreator.java` (line 3371):
   - After creating the base type, call `type.setSuperType(getType(context, FoundSet.JS_FOUNDSET))` before `addType()`.

2. **Verify parameterized type compatibility**: Confirm that the combined type returned at line 3435 (for `ViewFoundSet<datasource>`) inherits the super-type chain through the base `ViewFoundSet` type, making it assignable to both `JSFoundSet` and `JSFoundSet<datasource>`.

3. **Run existing tests** in `com.servoy.eclipse.tests` to ensure no regressions in type resolution, code completion, or validation.

4. **Manual testing**: In the IDE, create a form with a view datasource, place an aggrid with a foundset property, and verify:
   - `elements.table.myFoundset.foundset = viewFs;` produces no warning
   - Code completion on the assigned viewFoundset still works correctly
   - Assigning an incompatible type (e.g. a String) still produces the warning

## 5. Acceptance criteria

- [ ] `elements.table.myFoundset.foundset = viewFs;` (where `viewFs` is a `ViewFoundSet`) does not produce a type-mismatch warning
- [ ] Assigning a regular `JSFoundSet` to the same property still works without warnings
- [ ] Assigning an incompatible type (e.g. `String`, `Number`) still produces the warning
- [ ] Code completion on ViewFoundSet variables still shows ViewFoundSet-specific members
- [ ] No regressions in existing type validation tests

## 6. Out of scope

- `MenuFoundSet` type compatibility (currently disabled/commented out)
- Adding a `.spec` file tag to explicitly mark which components support view foundsets (future enhancement per Johan's comment)
- Changing the runtime behaviour of the foundset property type in `servoy_ngclient`

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should parameterized `ViewFoundSet<view:xxx>` also be assignable to parameterized `JSFoundSet<datasource>` (same datasource)? | dev | open |
| Does setting super type affect code completion negatively (showing JSFoundSet members that ViewFoundSet overrides differently)? | dev | open |
| Should `MenuFoundSet` get the same treatment when it's re-enabled? | Johan | open |
