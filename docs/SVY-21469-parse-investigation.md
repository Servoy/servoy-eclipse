# SVY-21469 — customProperties string→JSON parse investigation

Investigation of why a form-component child element's `customProperties` (holding
`attributes: { data-Target: ... }`) resolves at runtime in 2026.6 but comes back empty
(so the handler's `dataTarget` arg is `null`) in 2026.9, and where a migration should
convert the legacy loose-string form.

Every claim is labelled **[CONFIRMED]** (with file:line / commit hash) or **[SUSPECTED]**.

---

## 0. TL;DR

- The two `.frm` shapes both describe the same value; the difference is whether
  `customProperties` is stored as a **JSON string** (legacy, loose) or a nested **JSON
  object** (current). **[CONFIRMED]**
- The format switch happened in **SVY-18589** (commit `9d673c4f8` in `servoy-client`,
  `832d7ea6da` in `servoy-eclipse`, both 2025-06-12), which flipped `ServoyJSONObject`'s
  default quoting and made `SolutionSerializer` write `customProperties` as a pure JSON
  object. This is the "JSON migration between 2026.6 and 2026.9". **[CONFIRMED]**
- For **form-component child** elements (which is exactly what `env_health` /
  `status_icon` are — they live inside the parent's `json` blob, not as top-level
  persists), the legacy string was **not** parsed back into an object on read, so
  `getAttributes()` saw a `String` instead of a `Map` and returned an empty map →
  `data-Target` missing. **[CONFIRMED for the read path; SUSPECTED as the exact
  end-user symptom]**
- That read-side gap was closed by **SVY-20667** (commit `6e85bdecd8`, 2025-11-14) in
  `WebFormComponentChildType.getProperty`. **[CONFIRMED]**
- There is **no general migration** that rewrites the loose string in the `.frm` to the
  object form. The only write-side conversion is opportunistic, in
  `WebFormComponentChildType.getJson(forMutation=true)` (also SVY-20667), i.e. it only
  rewrites when that specific child is mutated. **[CONFIRMED]**

---

## 1. The exact parse path

### 1a. Where a JSON-typed `.frm` property string becomes a JSONObject

`customProperties` is declared as an `IRepository.JSON`-typed content-spec element for
every persist type. **[CONFIRMED]**
`servoy_shared/src/com/servoy/j2db/persistence/StaticContentSpecLoader.java:749-901`
(e.g. line 749 FORMS, 754 GRAPHICALCOMPONENTS, 901 WEBCOMPONENTS).

**IDE / workspace load path** (top-level persists):
- `SolutionDeserializer.getPropertyValuesForJsonObject(...)` reads each property; for a
  present property it takes `obj.get(propertyName).toString()` then converts it.
  `com.servoy.eclipse.model/.../repository/SolutionDeserializer.java:2216-2229` **[CONFIRMED]**
- Conversion of a `JSON`-typed value string → object:
  `AbstractPersistFactory.convertArgumentStringToObject(...)`,
  `servoy_shared/.../persistence/AbstractPersistFactory.java:241-250`:
  ```java
  case IRepository.JSON :
      retval = new ServoyJSONObject(s, false);   // noBrackets=false
  ```
  **[CONFIRMED]**

**Runtime consumption for `attributes`** (both IDE and client):
- `AbstractBase.getCustomPropertyNonFlattenedInternal(String[] path)` wraps the stored
  typed property into a map:
  `servoy_shared/.../persistence/AbstractBase.java:1012-1046` — specifically line 1014
  `getTypedProperty(PROPERTY_CUSTOMPROPERTIES)` and line 1020
  `new JSONWrapperMap(new ServoyJSONObject(customProperties, ServoyJSONObject.getNames(customProperties), false, true))`.
  **[CONFIRMED]**
- `getAttributes()` reads path `{ "attributes" }` and only returns a real map when the
  value is a `Map`:
  `servoy_shared/.../persistence/BaseComponent.java:411-420`:
  ```java
  Object customProperty = getCustomProperty(new String[] { PROPERTY_ATTRIBUTES });
  if (customProperty instanceof Map) return Collections.unmodifiableMap(...);
  return Collections.emptyMap();
  ```
  **[CONFIRMED]** — this `instanceof Map` gate is the empty-result branch when the value
  is left as a `String`.
- `PROPERTY_ATTRIBUTES = "attributes"`:
  `servoy_shared/.../persistence/IContentSpecConstants.java:223` **[CONFIRMED]**

### 1b. Form-component-child path (the relevant one for env_health / status_icon)

`env_health` and `status_icon` in the example are **child components of a form-component
property**, stored inside the parent web component's `json` blob (keyed by the child
name), not as standalone `.frm` persists. Such children are represented by
`WebFormComponentChildType`, whose `getProperty` returns values straight out of the
parent's flattened `json`:
`com.servoy.eclipse.model/.../util/WebFormComponentChildType.java:252-275` and
`getJson(forMutation, flattened)` at `:468-535`. **[CONFIRMED]**

Because these values come out of the parent JSON blob directly (not through
`AbstractPersistFactory.convertArgumentStringToObject`), a legacy loose **string**
`customProperties` inside that blob was returned as a raw `String` unless explicitly
converted. **[CONFIRMED]**

---

## 2. What changed 2026.6 → 2026.9

### 2a. SVY-18589 — the JSON migration (the smoking gun for the *write* format switch)

`servoy-client` commit **`9d673c4f8`** "SVY-18589 Migrate to valid JSON content all our
solution JSON files" (lvostinar, **2025-06-12**). **[CONFIRMED]**
`servoy-eclipse` companion commit **`832d7ea6da`** (same subject, same day). **[CONFIRMED]**

Key diffs:

`servoy_shared/.../util/ServoyJSONObject.java` — flipped the default quoting mode and the
convenience constructor, and made the parser tolerant of already-bracketed data:
```diff
- protected boolean noQuotes = true;
+ protected boolean noQuotes = false;
  ...
  public ServoyJSONObject(String data, boolean noBrackets) throws JSONException
- { this(data, noBrackets, true, true); }
+ { this(data, noBrackets, false, true); }
  ...
- super((noBrackets ? "{" : "") + (newLines ? replaceEmbeddedStringNewlines(data) : data) + (noBrackets ? "}" : ""));
+ // noBrackets false is just a hint now, for backwards compatibility we have to check the actual data if it has brackets or not
+ super((noBrackets || (data != null && !data.startsWith("{") && !data.startsWith("[")) ? "{" : "") +
+     (newLines ? replaceEmbeddedStringNewlines(data) : data) +
+     (noBrackets || (data != null && !data.startsWith("}") && !data.startsWith("[")) ? "}" : ""));
```
**[CONFIRMED]**

`servoy-eclipse` `SolutionSerializer.java` — now treats `customProperties` as JSON and
writes it as a pure JSON object (and switched serialization to quoted keys):
```diff
- boolean isJSON = propertyObjectValue instanceof JSONObject;
+ boolean isJSON = propertyObjectValue instanceof JSONObject || element.getTypeID() == IRepository.JSON ||
+     StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName().equals(propertyName);
  ...
+ if (isJSON) {
+     property_values.put(propertyName, new ServoyJSONObject(propertyValue, false, false, true)); // always store as pure json
+ }
```
(current form of this logic: `SolutionSerializer.java:1210-1221`). **[CONFIRMED]**

Net effect: **after SVY-18589, any save writes `customProperties` in the nested-object
form** ("correct form"). Solutions authored/saved by an older Servoy still carry the
legacy loose-string form ("worked in 2026.6"). **[CONFIRMED]**

Timeline confirms the version window: SVY-18589 = 2025-06-12; the read-side compat fix
below = 2025-11-14 — consistent with a 2026.6 → 2026.9 regression. **[CONFIRMED]**

### 2b. SVY-20667 — the read/write compat for form-component children

`servoy-eclipse` commit **`6e85bdecd8`** "SVY-20667 Form with JSON format throws parsing
error due to formcomponent attribute" (lvostinar, **2025-11-14**), only touching
`WebFormComponentChildType.java`. **[CONFIRMED]**
```diff
+ if (propVal instanceof String && propertyName.equals(PROPERTY_CUSTOMPROPERTIES.getPropertyName())) {
+     // legacy string value, convert to json object
+     propVal = new ServoyJSONObject((String)propVal, false, false, true);
+ }
  return convertToJavaType(propertyName, propVal);
  ...
+ if (propertyValue != null && forMutation &&
+     propertyValue.opt(PROPERTY_CUSTOMPROPERTIES.getPropertyName()) instanceof String legacyCustomPropertiesValue) {
+     // save it in pure json format
+     propertyValue.put(PROPERTY_CUSTOMPROPERTIES.getPropertyName(),
+         new ServoyJSONObject(legacyCustomPropertiesValue, false, false, true));
+ }
```
Current form: read compat at `WebFormComponentChildType.java:269-273`; write compat at
`:527-533`. **[CONFIRMED]**

---

## 3. Why the legacy string form breaks now but worked before — concrete mechanism

The legacy `.frm` stores the value as a JSON **string** whose content is a loose
Servoy-JSON fragment with unquoted keys:
```
"env_health": { "customProperties": "attributes:{\<nl>data-Target:\"dashboard-health\"\<nl>}" }
```
The current form stores a nested **object**:
```
"status_icon": { "customProperties": { "attributes": { "data-Target": "dashboard-status" } } }
```
**[CONFIRMED — matches the two shapes in the ticket]**

For a **form-component child** (env_health/status_icon), the value is read via
`WebFormComponentChildType.getProperty` straight from the parent's flattened `json`
(section 1b). Before SVY-20667 there was no `String → ServoyJSONObject` conversion there,
so:
- `getProperty("customProperties")` returned a `String`. **[CONFIRMED — the exact line
  added by SVY-20667 was absent before]**
- `getAttributes()` then hit the `customProperty instanceof Map` gate
  (`BaseComponent.java:414-419`), which is `false` for a `String`, so it returned
  `Collections.emptyMap()`. **[CONFIRMED]**
- Therefore `attributes` (and `data-Target`) were empty → the handler's `dataTarget`
  arg was `null`. **[SUSPECTED — this is the plausible end-user symptom; not reproduced
  here]**

Why it "worked in 2026.6": before SVY-18589 the loose string *was* the canonical stored
format and the codebase's default `noQuotes=true` parsing/round-tripping treated it
consistently; the writer produced it and the readers understood it. After SVY-18589 the
canonical format became the nested object, and the loose-string branch for
form-component children was simply not handled on read until SVY-20667.
**[SUSPECTED — strong, based on the diffs; the precise 2026.6 read path for FC children
was not exhaustively traced]**

Note on top-level (non-FC) persists: these do **not** break, because
`AbstractPersistFactory.convertArgumentStringToObject` (1a) always parses the JSON-typed
string into a `ServoyJSONObject` at load, and `new ServoyJSONObject(s, false)` wraps a
brace-less unquoted fragment in `{ }` and org.json tolerates unquoted keys. **[CONFIRMED
from code; not runtime-verified]**

---

## 4. Migration tool findings

- There is **no dedicated migration pass** that walks solutions and rewrites legacy
  loose-string `customProperties` into the object form. Searches for `customProperties`
  across both repos surface only the read/consume code, the serializer, and the
  SVY-20667 FC-child compat — no version-gated migration step. **[CONFIRMED — grep
  across servoy-client persistence/util and servoy-eclipse.model]**
- The closest thing to a migration is **implicit and lazy**:
  1. **Write on save** — `SolutionSerializer.generateJSONObject` always emits
     `customProperties` as a `ServoyJSONObject` now, so any resave of a persist rewrites
     it to the object form. `SolutionSerializer.java:1210-1221`. **[CONFIRMED]**
  2. **FC-child write on mutation only** —
     `WebFormComponentChildType.getJson(forMutation=true)` converts a legacy string to an
     object, but *only when that child is being mutated*.
     `WebFormComponentChildType.java:527-533`. **[CONFIRMED]**
- **Gap:** a form-component child that is never mutated keeps its legacy loose-string
  `customProperties` in the parent `.frm` blob. On master this is masked at read time by
  the SVY-20667 read compat (`WebFormComponentChildType.java:269-273`), so functionally
  it resolves; but the `.frm` is never normalized until a mutation/resave happens.
  **[CONFIRMED for the write gap; the runtime masking is CONFIRMED in code]**

---

## 5. Recommended fix location(s) — locate & recommend only (not implemented)

Depending on what SVY-21469 actually reports (runtime-empty attributes vs. an
un-migrated `.frm`), the candidates are:

1. **Read-side lenient parse (primary safety net).**
   Ensure every read path that can surface a legacy string `customProperties` converts
   it to an object, mirroring SVY-20667:
   - `WebFormComponentChildType.getProperty` — already handled
     (`WebFormComponentChildType.java:269-273`). Verify the *flattened* / merged path
     (`getJson(false, true)` at `:508-525`) also normalizes nested child
     `customProperties`, since merging copies raw values via
     `ServoyJSONObject.mergeAndDeepCloneJSON` (`:524`) and could re-introduce a string.
     **[SUSPECTED — worth a targeted check]**
   - `AbstractBase.getCustomPropertyNonFlattenedInternal`
     (`AbstractBase.java:1014-1020`) assumes `getTypedProperty(CUSTOMPROPERTIES)` is
     already a `JSONObject`. For top-level persists that holds (parsed in 1a). If any
     runtime path can hand it a `String`, this is the single choke point to harden.
     **[SUSPECTED]**

2. **A real migration/normalization step (root cause for un-migrated `.frm`).**
   If the goal is to actually rewrite legacy loose-string `customProperties` to objects
   on import/upgrade rather than only lazily, the natural home is the deserialize path
   that already normalizes JSON-typed values:
   - `SolutionDeserializer.getPropertyValuesForJsonObject`
     (`SolutionDeserializer.java:2198-2233`) / `setPersistValues` (`:2192-2196`) — where
     JSON-typed values are converted; a normalization here would apply to top-level
     persists on load.
   - For FC children specifically, extend the SVY-20667 write compat so it also fires on
     load/normalize, not only `forMutation`
     (`WebFormComponentChildType.java:527-533`).
   **[SUSPECTED — these are the correct seams; exact choice depends on desired scope]**

3. **Write-side (already correct).**
   `SolutionSerializer.generateJSONObject` (`SolutionSerializer.java:1210-1221`) already
   writes the object form; no change needed unless a forced re-normalization of all
   persists is desired. **[CONFIRMED]**

**Recommendation:** confirm from the actual failing solution's `.frm` whether the broken
element is a form-component child (matches env_health/status_icon) or a top-level
persist. If FC child on a 2026.9 build that already contains SVY-20667, the runtime read
should resolve — so re-verify the installed build actually includes `6e85bdecd8`, and
check the flattened/merge path in (5.1). If the requirement is to stop shipping loose
strings, add the migration in (5.2).

---

## Confirmed anchors (file:line / commit)

- `StaticContentSpecLoader.java:749-901` — customProperties is `IRepository.JSON`. **[CONFIRMED]**
- `AbstractPersistFactory.java:241-250` — `IRepository.JSON` → `new ServoyJSONObject(s, false)`. **[CONFIRMED]**
- `SolutionDeserializer.java:2216-2229` — per-property string→object conversion on load. **[CONFIRMED]**
- `AbstractBase.java:1014-1020` — wraps stored customProperties into `JSONWrapperMap`. **[CONFIRMED]**
- `BaseComponent.java:411-420` — `getAttributes()` `instanceof Map` gate → empty map. **[CONFIRMED]**
- `WebFormComponentChildType.java:269-273` / `:527-533` — SVY-20667 read/write compat. **[CONFIRMED]**
- servoy-client `9d673c4f8`, servoy-eclipse `832d7ea6da` — SVY-18589, 2025-06-12. **[CONFIRMED]**
- servoy-eclipse `6e85bdecd8` — SVY-20667, 2025-11-14. **[CONFIRMED]**
