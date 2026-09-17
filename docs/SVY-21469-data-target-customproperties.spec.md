# SVY-21469 — `data-target` no longer pushed to `onAction` for form-component children with legacy `customProperties`

> Status: developer-ready spec. Do NOT start implementing before reading the two authoritative
> investigation docs this spec builds on:
> - `docs/SVY-21469-triage.md`
> - `docs/SVY-21469-parse-investigation.md`

---

## 1. Goal

Make the `data-target` value that a user sets via a component's `attributes` property reach the
`onAction` handler again for **form-component child** components whose `customProperties` was written
by an older Servoy as a **legacy loose JSON string** (rather than the current nested JSON object).

Concretely:

1. **Fix the Blocker at runtime in the IDE** — the developer-side flattened, non-mutation read path
   must normalize a nested child's legacy `customProperties` string into a `ServoyJSONObject`, so
   `BaseComponent.getAttributes()` returns a non-empty map and `data-Target` is delivered.
2. **Fix the migration tool** — "Upgrade solution files to new json format"
   (`ConvertToNewFormatAction`) must permanently rewrite a form-component child's legacy string
   `customProperties` to the object form in the `.frm`, so old solutions stop being fragile.
3. **Verify the deployed / server runtime path** and add a fix there only if the same gap exists.
4. **Add Java regression tests** for the read normalization and the migration write.

Idempotency is required: a value already stored as a JSON object must be left unchanged by every
change below.

---

## 2. Background

### 2.1 The two `.frm` shapes

`customProperties` is an `IRepository.JSON`-typed content-spec element for every persist type
(`StaticContentSpecLoader.java:749-901`). It carries the `attributes` map that produces the DOM
attributes (`data-target`) delivered to `onAction`.

A form-component child (e.g. a label used as a clickable *tile* inside a `containedForm`) can be
stored in the parent web component's `json` blob in two shapes:

- **Legacy loose string** (older Servoy):
  `"env_health": { "customProperties": "attributes:{ data-Target:\"dashboard-health\" }" }`
- **Current nested object** (post-migration):
  `"status_icon": { "customProperties": { "attributes": { "data-Target": "dashboard-status" } } }`

Both describe the same value; only the encoding differs. Confirmed in
`docs/SVY-21469-parse-investigation.md` §3.

### 2.2 SVY-18589 — the JSON format switch (2026.6 → 2026.9)

- servoy-client commit **`9d673c4f8`**, servoy-eclipse commit **`832d7ea6da`** — "SVY-18589 Migrate
  to valid JSON content all our solution JSON files" (both **2025-06-12**).
- It flipped `ServoyJSONObject`'s default quoting (`noQuotes` `true` → `false`) and made
  `SolutionSerializer` always write `customProperties` as a pure JSON object
  (`SolutionSerializer.java:1210-1221`).
- Net effect: after SVY-18589, any save writes the nested-object form. Solutions authored/saved by
  an older Servoy still carry the legacy loose-string form. This is the "JSON migration between
  2026.6 and 2026.9".

Top-level persists do **not** break, because on load
`AbstractPersistFactory.convertArgumentStringToObject` always parses the JSON-typed string into a
`ServoyJSONObject` (`AbstractPersistFactory.java:241-250`, `case IRepository.JSON`). Form-component
children do **not** go through that path — their values come straight out of the parent's `json`
blob.

### 2.3 SVY-20667 — the partial read/write compat for form-component children

- servoy-eclipse commit **`6e85bdecd8`** — "SVY-20667 Form with JSON format throws parsing error due
  to formcomponent attribute" (**2025-11-14**), touching only `WebFormComponentChildType.java`.
- It added two normalizations to `WebFormComponentChildType`:
  - **read compat** in `getProperty(...)`: if the resolved value is a `String` and the property is
    `customProperties`, convert it via
    `new ServoyJSONObject((String)propVal, false, false, true)` — `WebFormComponentChildType.java:269-273`.
  - **write compat** in `getJson(forMutation=true, ...)`: if `propertyValue.opt(customProperties)`
    is a `String`, replace it with a `ServoyJSONObject` — `WebFormComponentChildType.java:527-533`.
- (Companion servoy-client commit `19741fbf0` / `7e004ce14` hardened the analogous merge inside
  `FormElementHelper.generateFormComponentPersists` — see §3.3.)

### 2.4 The two remaining gaps (this ticket)

1. **Runtime read gap (the Blocker).** `WebFormComponentChildType.getProperty("customProperties")`
   normalizes (`:269-273`) and `getJson(forMutation=true, ...)` normalizes (`:527-533`). But the
   **flattened, non-mutation** read path — `getJson(false, true)` — does **not**. This is the path
   used to build the `json` blob returned for the child (`getProperty` for `PROPERTY_JSON` calls
   `getJson(false, true)` — `WebFormComponentChildType.java:255-258`), and the flattened branch
   (`getJson`, `:508-525`) copies `element.getFlattenedPropertiesMap()` values via
   `ServoyJSONObject.mergeAndDeepCloneJSON` (`:524`) which can carry a legacy `String`
   `customProperties` straight through. The `forMutation`-only normalization block sits at `:527-533`
   and does not fire on this path (`forMutation` is `false`). So the child's `customProperties`
   can stay a `String`; `BaseComponent.getAttributes()` (`BaseComponent.java:411-420`) hits its
   `instanceof Map` gate and returns `Collections.emptyMap()`, and `data-Target` is dropped →
   `onAction`'s `dataTarget` arg is `null`.

2. **Migration-tool gap.** The Solution Explorer action "Upgrade solution files to new json format"
   = `ConvertToNewFormatAction` (`ConvertToNewFormatAction.java`, `convertSolutionToNewFormat` at
   `:222-254`) rewrites each Form via `SolutionSerializer.writePersist(...)`.
   `SolutionSerializer.generateJSONObject` normalizes **top-level** persist `customProperties` to the
   object form (`SolutionSerializer.java:1210-1221`), but a form-component child's
   `customProperties` lives **inside** the parent web component's `json` blob and reaches the
   serializer as part of that blob's value (via `getPersistAsValueMap` → `repository.getPersistAsValueMap`,
   `SolutionSerializer.java:1165-1179` / `:1190`). It is only normalized when `getJson` is called with
   `forMutation=true` — which the migration write path does not trigger. So even after running the
   migration, legacy strings remain on FC children in the `.frm`.

### 2.5 What is proven correct and not touched

- The DOM write and the bootstrap-component read are fine. For a plain label, the user's browser
  screenshot shows `data-target="pec"` on the `<span class="bts-label">` host, and `getDataTarget`
  (`bts_baselabel.ts:62-68`) resolves it. The bootstrap `getDataTarget`/`closest('[data-target]')`
  logic is unchanged (`docs/SVY-21469-triage.md`). The prior "attribute never reaches the DOM" /
  `getDataTarget` DOM-traversal theory is **disproven** and is explicitly out of scope (see §6).
- The value is null because `getAttributes()` returned an empty map on the server/IDE, not because
  the client failed to read the attribute — this is a **Java persistence** bug.

---

## 3. Design

### 3.1 Read-side normalization (fixes the Blocker for existing solutions at runtime — IDE)

**File:** `com.servoy.eclipse.model/src/com/servoy/eclipse/model/util/WebFormComponentChildType.java`
**Method:** `private JSONObject getJson(boolean forMutation, boolean flattened)` (`:468-535`)

Add a normalization that also fires for the **flattened, non-mutation** read, mirroring the existing
`forMutation=true` branch at `:527-533` and the `getProperty` branch at `:269-273`.

Design intent:

- The existing `forMutation`-guarded block at `:527-533` converts a legacy `String`
  `customProperties` on `propertyValue` before returning it.
- Extend the guard so that the conversion also happens when the value is being read flattened (the
  path that produces the child's effective `json` blob). Prefer widening the existing condition to
  fire whenever `propertyValue.opt(customProperties) instanceof String`, regardless of `forMutation`
  — the conversion is idempotent (a `ServoyJSONObject` value is not a `String` so it is skipped) and
  harmless on the read path.
- Keep the `new ServoyJSONObject(legacyCustomPropertiesValue, false, false, true)` construction so it
  matches SVY-20667 exactly (`noBrackets=false, newLines=false, ordered=true`).

Caveat to respect during implementation: on the flattened branch (`:508-525`), `propertyValue` is
reassigned to the merged `full` object at `:525` *before* reaching the normalization block; verify
the normalization runs on the object that is actually returned (i.e. after the merge, on `full`),
and does not accidentally mutate a shared/cached `element` map. The merged `full` is a freshly built
`JSONObject`, so writing the normalized `customProperties` back onto it is safe. Do not normalize the
raw `element.getFlattenedPropertiesMap()` entries in place.

Result: `getProperty(PROPERTY_JSON)` → `getJson(false, true)` returns a blob whose child
`customProperties` is a `ServoyJSONObject`; downstream `getAttributes()` sees a `Map` and returns
`data-Target`.

### 3.2 Migration-tool normalization (permanently normalizes the `.frm`)

Goal: after "Upgrade solution files to new json format", a form-component child's `customProperties`
is stored as an object in the `.frm`, not a loose string.

**Primary seam — `SolutionSerializer.generateJSONObject`**
`com.servoy.eclipse.model/src/com/servoy/eclipse/model/repository/SolutionSerializer.java:1187-1253`.

The current code (`:1210-1221`) only normalizes a persist's *own* top-level `customProperties`
element. A `WebComponent` persist that hosts a form-component property carries the FC children
inside its `json`-typed property value (an `IRepository.JSON` element). When that `json` value is a
`JSONObject`, its nested `<childName>.customProperties` entries can still be `String`s.

Implementation options (pick one; name it precisely in the plan):

- **Option A (recommended): normalize nested FC-child `customProperties` inside the `json` value when
  serializing a `WebComponent`.** In `generateJSONObject`, when `element.getTypeID() == IRepository.JSON`
  and `propertyName` is the web component `json` property, after the value is turned into a
  `ServoyJSONObject` (`:1214-1221`), walk its immediate child objects and, for any child whose
  `customProperties` is a `String`, replace it with `new ServoyJSONObject(str, false, false, true)`.
  This is the same conversion used everywhere else and is idempotent. Keep the walk shallow-but-recursive
  enough to cover nested form components (a child can itself hold another FC property whose children
  have `customProperties`); reuse a small helper so the recursion is testable.
- **Option B: route the FC-child through the existing `WebFormComponentChildType` write compat.**
  In `ConvertToNewFormatAction.convertSolutionToNewFormat` (`:222-254`), before/at the
  `SolutionSerializer.writePersist(persist, ...)` call for each `Form`, ensure each of its
  form-component children is touched through a path that calls `getJson(forMutation=true, ...)`
  (`WebFormComponentChildType.java:527-533`), which already converts and writes back the object form.
  This requires enumerating the FC children of the form (the developer model already builds
  `WebFormComponentChildType` instances for the outline). Option B reuses existing, tested conversion
  but couples the migration to the developer FC-child model.

**Recommendation:** Option A, because the serializer is the single choke point for the `.frm` write
and does not depend on the FC-child outline model being materialized. It also fixes any resave path,
not only the explicit migration action.

**Files touched:**
- `com.servoy.eclipse.model/src/com/servoy/eclipse/model/repository/SolutionSerializer.java`
  (`generateJSONObject`, `:1187-1253`) — add the nested-`customProperties` normalization helper and
  call it for the web component `json` value.
- No change needed in `ConvertToNewFormatAction.java` if Option A is chosen (it already calls
  `writePersist` → `generateJSONObject`).

### 3.3 Server-side (deployed + developer runtime) path — FIX REQUIRED (the actual 26.9 regression)

**This supersedes the earlier "no change required" conclusion.** Git history pins the exact
regression between the 26.6 and 26.9 tags:

- servoy-client commit **`c446933c1`** "Skip incompatible legacy properties in form component
  persist generation" (2026-08-25, in `2026.9_RC1`, NOT in `2026.06.0`) added a skip guard in
  `FormElementHelper` at `FormElementHelper.java:378-383`:
  ```java
  if (!paramType.isAssignableFrom(val.getClass()) && !(paramType.isPrimitive() && val instanceof Number))
  {
      Debug.debug("Skipping incompatible legacy property '" + key + "' ...");
      continue;
  }
  ```

**Mechanism (why 26.6 worked, 26.9 doesn't):** a form-component child's legacy `customProperties`
is a `String`; its setter `setCustomProperties(JSONObject)` takes a `JSONObject`, so
`paramType.isAssignableFrom(String)` is false and control enters the type-mismatch block at
`:362`. In **26.6** that block attempted conversions and then fell through to the dedicated
`customProperties` branch at `:391` (`val instanceof String && key.equals(PROPERTY_CUSTOMPROPERTIES)`),
which parses the loose string and merges it via `setCustomProperties(...)` — so `data-Target` was
delivered. In **26.9**, commit `c446933c1`'s new guard at `:378-383` sees the still-`String` value
as "incompatible" and `continue`s, **skipping it before it ever reaches the `:391` branch**. The
attribute is dropped, `getAttributes()` returns empty, and `onAction`'s `dataTarget` is `null`.

That commit's intent was to skip genuinely stale legacy props (`location`/`size`/`anchors` removed
by the Angular 22 migration), but it over-reaches and also skips `customProperties`, which has a
legitimate `String`→object handler immediately below.

**Fix:** do not `continue` (skip) for keys that have a dedicated handling branch below — at minimum
`customProperties`. Options:
- **Option A (recommended, minimal):** in the skip guard at `:378-383`, exempt
  `StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName().equals(key)` so a legacy
  string `customProperties` falls through to the existing `:391` merge branch.
- **Option B:** move the skip guard to fire only after the dedicated `customProperties` / JSONObject
  / JSONArray merge branches have had a chance to run (reorder so the `continue` is a last resort).

Option A is the smallest, lowest-risk change and directly restores 26.6 behaviour for
`customProperties` while keeping the intended skip for genuinely incompatible legacy props.

**File touched:** `servoy_ngclient/src/com/servoy/j2db/server/ngclient/FormElementHelper.java`
(`generateFormComponentPersists`, the property loop around `:355-399`).

Note: `AbstractBase.getCustomPropertyNonFlattenedInternal` (`AbstractBase.java:1012-1046`) remains
safe once the value has reached `setCustomProperties` as a `JSONObject` via the restored `:391`
branch.

### 3.4 Server-side (deployed) runtime path — VERIFIED for the getCustomProperty wrapper

The IDE class `WebFormComponentChildType` is `com.servoy.eclipse.model` (developer-only) and does not
run in a deployed WAR. The deployed/server form-element build path for a form-component child is
`servoy_ngclient`'s `FormElementHelper.generateFormComponentPersists(...)`
(`servoy_ngclient/src/com/servoy/j2db/server/ngclient/FormElementHelper.java:246-418`).

For each child it clones the FC child persist and applies the parent blob's per-child JSON. The
`customProperties` case is handled at `FormElementHelper.java:391-398`:

```java
else if (val instanceof String && StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName().equals(key))
{
    // custom properties needs to be merged in..
    JSONObject json = ((AbstractBase)cloneOfChildOfFormComponent).getCustomProperties();
    JSONObject original = json != null ? new ServoyJSONObject(json, ServoyJSONObject.getNames(json), false, true)
        : new ServoyJSONObject(false, true);
    ServoyJSONObject.mergeAndDeepCloneJSON(new ServoyJSONObject((String)val, false), original);
    ((AbstractBase)cloneOfChildOfFormComponent).setCustomProperties(original);
}
```

A legacy loose `String` `customProperties` is parsed (`new ServoyJSONObject((String)val, false)`),
merged into an object, and stored back via `setCustomProperties(original)` as a `JSONObject`. So at
runtime `AbstractBase.getCustomPropertyNonFlattenedInternal` (`AbstractBase.java:1012-1046`) receives
a `JSONObject` for `getTypedProperty(PROPERTY_CUSTOMPROPERTIES)` (`:1014`), wraps it in a
`JSONWrapperMap` (`:1020`), and `BaseComponent.getAttributes()` (`BaseComponent.java:411-420`) sees a
`Map` and returns the attributes. This block was strengthened for exactly this case by SVY-20667's
servoy-client companion commits (`19741fbf0` / `7e004ce14`, confirmed via
`git log -L 391,399:FormElementHelper.java`).

**Conclusion:** the deployed server path already normalizes the legacy string before it reaches
`getCustomPropertyNonFlattenedInternal`. **Server-side change: not required, verified.** The
`AbstractBase.getCustomPropertyNonFlattenedInternal` `getTypedProperty` assumption
(`AbstractBase.java:1014`) is safe on the deployed path because of the conversion above. (If a
regression test in §5.3 ever shows a `String` reaching that method on the server, revisit
`FormElementHelper.java:391-398`, but the current code makes that unreachable.)

---

## 4. Implementation plan (ordered, exact files/methods)

1. **Read normalization (IDE, the Blocker).**
   `com.servoy.eclipse.model/.../util/WebFormComponentChildType.java` — in
   `getJson(boolean forMutation, boolean flattened)` (`:468-535`), widen the legacy-string
   `customProperties` conversion currently at `:527-533` so it also applies to the flattened,
   non-mutation read (on the returned/merged object), keeping the
   `new ServoyJSONObject(str, false, false, true)` construction and idempotency. Respect the merge
   ordering caveat in §3.1 (normalize the returned object, not shared caches).

2. **Migration normalization (IDE).**
   `com.servoy.eclipse.model/.../repository/SolutionSerializer.java` — in `generateJSONObject`
   (`:1187-1253`), after a web component's `json`-typed value is materialized as a `ServoyJSONObject`
   (`:1214-1221`), run a small recursive helper that replaces any nested `<child>.customProperties`
   that is a `String` with `new ServoyJSONObject(str, false, false, true)`. Helper must be
   package-visible/testable and idempotent. `ConvertToNewFormatAction.convertSolutionToNewFormat`
   (`:222-254`) needs no change (it already calls `writePersist` → `generateJSONObject`).

3. **Server-side fix (the actual 26.9 regression — §3.3).**
   `servoy-client` → `servoy_ngclient/src/com/servoy/j2db/server/ngclient/FormElementHelper.java`
   — in `generateFormComponentPersists` property loop (`:355-399`), the skip guard added by
   `c446933c1` at `:378-383` must NOT skip `customProperties`. Apply Option A: exempt
   `StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName().equals(key)` from the
   `continue`, so a legacy `String` `customProperties` falls through to the existing merge branch at
   `:391` (`setCustomProperties`). This is the change that restores 26.6 runtime behaviour for
   deployed AND developer NG clients.

4. **Tests** (see §5). Add to `com.servoy.eclipse.model.tests` (IDE) and a server-side test near
   `FormElementHelper` in `servoy-client` (see §5.3).

5. **Verification** (per repo AGENTS.md): after edits run
   `eclipse-ide_getCompilationErrors`, `eclipse-coder_organizeImports`, `eclipse-coder_formatFile`,
   fix any SpotBugs of the two highest severities, then run the new tests via
   `eclipse-ide_runClassTests` (or `eclipse-pde_runJUnitPluginTestClass` if the test needs the PDE
   runtime).

6. **Commit** with subject including the case number and `[ai]`, e.g.
   `SVY-21469 normalize legacy string customProperties for form-component children [ai]`.
   Do not push without explicit user approval.

---

## 5. Acceptance criteria (testable)

### 5.1 Read path (primary — the Blocker)

Given a form-component child whose stored `customProperties` is the legacy loose string
`"attributes:{ data-Target:\"dashboard-health\" }"`:

- `WebFormComponentChildType.getProperty(PROPERTY_JSON)` (→ `getJson(false, true)`) returns a
  `JSONObject` in which `customProperties` is a `ServoyJSONObject` (not a `String`).
- The effective `BaseComponent.getAttributes()` for that child returns a **non-empty** map containing
  key `data-Target` with value `dashboard-health`.
- A value already stored as a nested object is returned unchanged (idempotent).

### 5.2 Migration write

Given a Form persist containing a web component that hosts a form-component property whose child has
a legacy loose string `customProperties`:

- After `SolutionSerializer.writePersist(form, ...)` (the path `ConvertToNewFormatAction` uses), the
  serialized `.frm` content for that child contains `customProperties` as a **JSON object**
  (`"customProperties": { "attributes": { "data-Target": ... } }`), not a quoted string.
- Re-running the migration on an already-object `.frm` produces no diff (idempotent).

### 5.3 Server-side (regression test — the actual 26.9 fix)

- A test near the server form-element path (`servoy_ngclient`) that builds a form-component child
  from a parent blob whose child `customProperties` is a legacy loose string
  (`"attributes:{ data-Target:\"dashboard-health\" }"`), runs the
  `FormElementHelper.generateFormComponentPersists` property-merge logic, and asserts the resulting
  persist's `getCustomProperties()` / `getAttributes()` contains `data-Target == dashboard-health`.
- This test MUST FAIL against the current (regressed) `:378-383` skip guard and PASS after the
  Option A exemption — i.e. it pins the regression, not merely documents current behaviour.
- If a full `FormElementHelper` harness is impractical server-side, cover it with a focused unit
  test of the property-loop branch that exercises: legacy-string `customProperties` is NOT skipped
  and reaches `setCustomProperties`, while a genuinely incompatible legacy prop (e.g. `location`
  as a UUID string for a `Point` setter) IS still skipped (guard intent preserved).

### 5.4 Test locations & frameworks

- **Read + migration tests:** `com.servoy.eclipse.model.tests` (JUnit 5 / Jupiter + Mockito, matching
  `WebFormComponentChildTypeTest.java` and `SolutionSerializerTest.java`). Add:
  - a read-normalization test in
    `com.servoy.eclipse.model.util.WebFormComponentChildTypeTest` (new nested class), and
  - a migration-normalization test in
    `com.servoy.eclipse.model.repository.SolutionSerializerTest` (new nested class) exercising the new
    `generateJSONObject` helper directly (reflection is acceptable, as the existing test already uses
    it for `generateParams`).
- If the read test needs a materialized FC-child (parent web component + FC property + form), and that
  cannot be mocked cleanly, promote it to a plugin JUnit test (eclipse-test-plugin packaging, run via
  `eclipse-pde_runJUnitPluginTestClass`) and build a minimal synthetic solution, following the
  builder-marker harness pattern referenced in AGENTS.md.
- **Do not** add bootstrapcomponents Vitest tests for this fix — the DOM/handler read is correct; the
  fix is Java persistence.

---

## 6. Out of scope

- **The `getDataTarget` / DOM-traversal theory is disproven and not addressed.** The `attributes`
  value reaches the DOM correctly for a plain label (`data-target="pec"` on the host span), and the
  bootstrap `getDataTarget`/`event.target.closest('[data-target]')` reader
  (`bts_baselabel.ts:62-68`) is unchanged and correct. No bootstrapcomponents code is changed.
- **`getEventArgs` / server event-arg delivery** — confirmed non-truncating and unchanged
  (`converter.service.ts:173-219`, `BaseWebObject.executeEvent`); not touched.
- **Top-level (non-FC) persist `customProperties`** — already normalized on load via
  `AbstractPersistFactory.convertArgumentStringToObject` (`:241-250`); not changed.
- **The write-on-save serializer for top-level `customProperties`** — already correct
  (`SolutionSerializer.java:1210-1221`); only the *nested FC-child* case is added.
- **A generic version-gated migration for all persist types** — not needed; the two seams above cover
  the reported case.

---

## 7. Open questions

1. **Merge-ordering in `getJson` flattened branch (§3.1).** Confirm during implementation that
   normalizing on the merged `full` object (post-`mergeAndDeepCloneJSON`, `:524-525`) is correct and
   that no cached `element` map is mutated in place. If the returned object is shared/cached, clone
   before normalizing.
2. **Recursion depth in the migration helper (§3.2 Option A).** Nested form components mean a child's
   `json` can itself contain FC children with `customProperties`. Decide whether the helper recurses
   through all nested FC properties or relies on `generateJSONObject` already recursing per persist.
   The helper should at minimum handle one level of FC-child `customProperties` inside a web
   component `json` value; confirm nested FC coverage with a test.
3. **Which `PROPERTY_JSON`/json property name to match in the serializer (§3.2).** Confirm the exact
   content-spec element used for a web component's json blob so the helper targets only that element
   (avoid touching unrelated `IRepository.JSON` elements). Likely `StaticContentSpecLoader.PROPERTY_JSON`.
4. **Server-side test harness (§5.3).** Confirm whether `FormElementHelper.generateFormComponentPersists`
   can be exercised in a lightweight `servoy_ngclient` unit test or whether the verification is best
   expressed as a focused test of the `:391-398` conversion.
