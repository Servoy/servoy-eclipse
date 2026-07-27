# Spec: SVY-21118 — {Object<String>} param is seen as Object

## 1. Goal

When a `Map<String, String>` parameter is annotated with `{Object<String>}` in the JSDoc `@param` tag, the Servoy Developer scripting environment should display the type as `Object<String>` (not just `Object`) in method signatures shown in the Solution Explorer list view and tooltips.

## 2. Background

### 2.1 Current behaviour

In the AI plugin's `MCPClientBuilder` class, several methods accept `Map<String, String>` parameters (e.g. `headers`, `environment`). These are annotated in the Java source with:

```java
@param headers {Object<String>} any request headers...
```

However, in Servoy Developer's Solution Explorer (list content provider), the parameter type is displayed as plain `Object` — the `<String>` generic qualifier is stripped.

### 2.2 The signature generation stack

The signature is generated via this call chain (from the Solution Explorer):

```
SolutionExplorerListContentProvider.getJSMethodsViaJavaMembers()
  → XMLScriptObjectAdapter.getJSTranslatedSignature(methodName, argTypes, returnType)
    → FunctionDocumentation.getFullJSTranslatedSignature(withNames=false, withTypes=true, returnType)
      → FunctionDocumentation.getJSTranslatedSignature(prefix=null, withNames=false, withTypes=true)
        → parDoc.getType()  // returns java.util.Map.class
        → getClassStringType(parDocType)
          → DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(Map.class)
            → returns "Object"
```

Key code in `FunctionDocumentation.getJSTranslatedSignature()` (line 276-290 in `j2db_documentation`):
```java
if (withTypes)
{
    Class<?> parDocType = parDoc.getType();
    if (parDocType != null)
    {
        // ... varargs handling ...
        sb.append(getClassStringType(parDocType));  // ← always uses Class, ignores jsType
    }
}
```

The method **never** checks `parDoc.getJSType()`, even though the `IParameterDocumentation` interface already has a `getJSType()` method and the XML parsing already reads the `jstype` attribute.

### 2.3 How types flow from Java to the scripting environment

1. The Java source uses `@ServoyDocumented` and JSDoc-style `@param` annotations.
2. The **doc generator** (`com.servoy.eclipse.docgenerator`) processes `@param` tags:
   - `DocumentedParameterData.checkIfHasType(holder, typeMapper)` tokenizes the description, takes the first token (e.g. `{Object<String>}`), creates a `TypeName` from it, and calls `typeMapper.mapType()`.
   - `TypeMapper.mapType()` **fails** to resolve `{Object<String>}` (due to braces and angle brackets), so `wasFound[0] = false` and neither `type` nor `jsType` gets set on the parameter.
   - The `setJSType()` method exists on `DocumentedParameterData` but is only called from `DesigntimeMethodStoragePlace` (for `@templateparam` tags), never for regular `@param` tags.
3. The generated `servoy-extension.xml` ends up with:
   ```xml
   <parameter name="headers"
              type="com.servoy.j2db.documentation.scripting.docs.Object"
              typecode="java.util.Map">
     <description><![CDATA[{Object<String>} any request headers...]]></description>
   </parameter>
   ```
   Note: no `jstype` attribute is emitted.
4. At load time, `ParameterDocumentation.fromXML()` reads the `jstype` attribute (returns null since it's absent) and loads the `typecode` as the `Class<?>`.
5. The signature generator uses only the `Class<?>` type, which maps `Map` → `Object`.

### 2.4 Existing infrastructure for `jstype`

The plumbing for `jstype` already exists end-to-end:

| Layer | Support |
|-------|---------|
| `IParameterDocumentation.getJSType()` | Interface method exists |
| `ParameterDocumentation` (j2db_documentation) | Stores `jsType` field, reads `ATTR_JSTYPE` from XML |
| `DocumentedParameterData` (docgenerator) | Has `setJSType()`/`getJSType()` |
| `MethodStoragePlace.toXML()` (docgenerator) | Emits `jstype` attribute if `par.getJSType() != null` |
| `FunctionDocumentation.getJSTranslatedSignature()` | **Does NOT check `getJSType()`** ← gap #1 |
| `DefaultDocumentationGenerator.doTypeMappingForAll()` | Calls `par.checkIfHasType()` but **never calls `setJSType()` for `{Type<Generic>}` patterns** ← gap #2 |

### 2.5 Affected parameters in the AI plugin

| Method | Parameter | Java type | JSDoc type |
|--------|-----------|-----------|------------|
| `connectViaSTDIO` | `environment` | `Map<String, String>` | `{Object<String>}` |
| `connectViaStreamableHTTP` (5 overloads) | `headers` | `Map<String, String>` | `{Object<String>}` |

## 3. Design

### 3.1 Four-part fix using existing `jstype` infrastructure

The fix bridges four gaps in the existing `jstype` infrastructure:

**Part A — Doc generator: extract `jstype` from `{Type<Generic>}` in `@param` descriptions**

In `DocumentedParameterData.checkIfHasType(MetaModelHolder, TypeMapper)`:
- When the first token of the description matches the pattern `{Type<...>}` (braces wrapping a type with angle-bracket generics):
  1. Strip the braces to get `Type<Generic>` (e.g. `Object<String>`)
  2. Call `setJSType("Object<String>")` so it gets emitted as the `jstype` XML attribute
  3. Extract the base type (e.g. `Object`) and still try to resolve it via `typeMapper.mapType()` for the `type`/`typecode` attributes
  4. Strip the `{Object<String>}` prefix from the description text (so it doesn't appear as duplicate in tooltips)

This ensures the generated XML will contain: `jstype="Object&lt;String&gt;"` (angle brackets are XML-escaped automatically by DOM)

**Part B — Signature generator: prefer `jsType` over class-based type translation**

In `FunctionDocumentation.getJSTranslatedSignature()` at the point where parameter types are appended to the signature (line ~287-290):
- Check `parDoc.getJSType()` first
- If non-null and non-empty, use it directly as the display type string
- Otherwise fall back to the existing `getClassStringType(parDocType)` logic

**Part C — Type system: propagate `jsType` through `ScriptParameter` to `TypeCreator`**

The DLTK type system (used for code completion and type checking) also needs to see the generic type:

1. `ScriptParameter` — accept a `jsType` parameter; when set, `getType()` returns it directly instead of translating from the Java class.
2. `XMLScriptObjectAdapter.getParameters()` — pass `argDoc.getJSType()` to `ScriptParameter` so the jsType flows from documentation to the type system.
3. `TypeCreator` — when building JSType for a parameter, check if `param.getType()` contains generic markers (`<`). If so, parse it with `JSDocTypeParser` (same parser used for return types) instead of translating the raw Java class.

**Part D — XSD: declare `jstype` attribute on `parameter` element**

In `servoydoc.xsd`, add `jstype` as an optional string attribute on the `parameter` complex type so the XML is schema-valid.

### 3.2 Why this approach

- Uses existing infrastructure (`jstype` attribute, `getJSType()` method) that was designed for this purpose but never connected for regular `@param` tags
- XSD updated to formally recognize the `jstype` attribute on `<parameter>`
- Works retroactively for all plugins that use `{Type<Generic>}` JSDoc notation in `@param` descriptions
- The type system (code completion) also benefits — generic types appear correctly
- The AI plugin does NOT need Java source changes — re-running the doc generator will produce correct XML

## 4. Implementation plan

### 4.1 Servoy Developer changes (this repository)

1. **`DocumentedParameterData.checkIfHasType(MetaModelHolder, TypeMapper)`** in `com.servoy.eclipse.docgenerator`:
   - Add detection for `{Type<...>}` pattern at the start of description
   - Extract generic type string, call `setJSType()`
   - Strip from description, resolve base type for `type`/`typecode`

2. **`FunctionDocumentation.getJSTranslatedSignature()`** in `j2db_documentation`:
   - Before `sb.append(getClassStringType(parDocType))`, check `parDoc.getJSType()`
   - If available, append it instead of the class-translated type

3. **`ScriptParameter`** in `servoy_shared`:
   - Add new constructor overload accepting `jsType` string
   - When `jsType` is non-null and non-empty, use it directly as `getType()` return value instead of translating from the Java class

4. **`XMLScriptObjectAdapter.getParameters()`** in `servoy_shared`:
   - Pass `argDoc.getJSType()` to `ScriptParameter` constructor so jsType propagates from documentation to the type system

5. **`TypeCreator`** in `com.servoy.eclipse.debug`:
   - When building JSType for a parameter, check if `param.getType()` contains `<`
   - If so, parse it with `JSDocTypeParser` to produce a proper generic JSType
   - Fall back to `getMemberTypeName()` if parsing fails

6. **`servoydoc.xsd`** in `com.servoy.eclipse.core`:
   - Add `jstype` as optional string attribute on the `parameter` complex type

### 4.2 AI Plugin changes (separate repository)

7. **Re-generate `servoy-extension.xml`** — after the doc generator fix, re-running it on the AI plugin source will produce the correct `jstype="Object&lt;String&gt;"` attributes automatically.

   Alternatively (immediate workaround): manually add `jstype="Object&lt;String&gt;"` to the relevant `<parameter>` elements in the existing XML so the fixes work immediately without waiting for a doc regeneration.

## 5. Acceptance criteria

- [ ] In Servoy Developer's Solution Explorer, `MCPClientBuilder.connectViaStreamableHTTP`'s `headers` parameter displays as `Object<String>` (not `Object`) in the method signature.
- [ ] `connectViaSTDIO`'s `environment` parameter also displays as `Object<String>`.
- [ ] Other parameter types (String, Number, Boolean, Array<String>, custom documented types) are not affected.
- [ ] The doc generator emits `jstype="Object<String>"` for parameters with `{Object<String>}` in their `@param` description.
- [ ] Parameters without `{...}` type prefixes in their description continue to work as before.
- [ ] The type system (code completion) shows `Object<String>` for parameters with jsType set.

## 6. Out of scope

- Full Java generics resolution in DLTK (covered by SVY-21117).
- Code completion on the values/keys of `Object<String>` parameters.
- Supporting complex nested generics like `Object<Array<String>>`.
- Changes to the AI plugin Java source code.

## 7. Files to modify

| File | Project | Change |
|------|---------|--------|
| `src/com/servoy/build/documentation/FunctionDocumentation.java` | `j2db_documentation` | Use `getJSType()` in signature generation |
| `src/com/servoy/eclipse/docgenerator/generators/DocumentedParameterData.java` | `com.servoy.eclipse.docgenerator` | Extract `{Type<Generic>}` → `setJSType()` |
| `src/com/servoy/j2db/documentation/ScriptParameter.java` | `servoy_shared` | Accept jsType, prefer it in `getType()` |
| `src/com/servoy/j2db/documentation/XMLScriptObjectAdapter.java` | `servoy_shared` | Pass `argDoc.getJSType()` to ScriptParameter |
| `src/com/servoy/eclipse/debug/script/TypeCreator.java` | `com.servoy.eclipse.debug` | Parse generic type strings via `JSDocTypeParser` |
| `src/com/servoy/eclipse/core/doc/servoydoc.xsd` | `com.servoy.eclipse.core` | Add `jstype` attribute to `parameter` type |
