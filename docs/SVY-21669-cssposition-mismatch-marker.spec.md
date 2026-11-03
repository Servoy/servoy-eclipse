# Spec: SVY-21669 — Warn when an element's compact `cssPosition` string disagrees with its `json.cssPosition`

## 1. Goal

When an AI agent (or any tool) authors a `.frm` file, it can write a web component whose two
position representations disagree: the item-level compact `cssPosition` string (e.g.
`"8,-1,12,-1,100,20"`) and the component's `json.cssPosition` object (e.g.
`{top:8, right:12, bottom:-1, left:-1, width:100, height:20}`). Servoy renders from the compact
string, so when the agent swaps `right`/`bottom` (or `width`/`height`), right-anchored elements
land at the wrong edge and overlap — with no warning anywhere. This spec adds a **ServoyBuilder
warning marker** that fires when a web component's stored compact `cssPosition` decodes to values
that disagree with the `cssPosition` object inside its `json` property, plus an Eclipse
**quickfix** that reconciles the two (rewrites the `json.cssPosition` object to match the
authoritative compact string). This gives the developer an immediate, visible signal in the
Problems view instead of a silently broken layout.

This spec implements **only ticket option 3** (the platform-side mismatch detection), scoped to
the `servoy-eclipse` repository. Ticket options 1 and 2 (skill wording + reviewer/generator
check) live in the separate `skill4servoy` repository and are explicitly **out of scope** here.

## 2. Background

### 2.1 The two representations and which one wins

- The **authoritative stored property** is the item-level compact `cssPosition` string. It is the
  content-spec property `cssPosition` (`StaticContentSpecLoader.PROPERTY_CSS_POSITION`), read back
  as a `CSSPosition` object via `BaseComponent.getCssPosition()`
  (`servoy_shared/.../persistence/BaseComponent.java:341`).
- `PersistHelper.createCSSPosition(String)`
  (`servoy_shared/.../util/PersistHelper.java:456-481`) tokenizes the compact string in the fixed
  order **top, right, bottom, left, width, height**, and `createCSSPositionString(CSSPosition)`
  (lines 438-453) writes it back in that same order. Missing tokens default to `"-1"`.
- `CSSPosition`'s constructor
  (`servoy_shared/.../persistence/CSSPosition.java:39`) is
  `CSSPosition(top, right, bottom, left, width, height)`, and its fields are public `String`s.
  `CSSPosition.equals(Object)` (lines 49-59) already compares all six fields with
  `Utils.equalObjects`.
- The **`json` block** is the web-component model echo: an own-property `JSONObject` stored under
  `StaticContentSpecLoader.PROPERTY_JSON` and read via
  `WebComponent.getOwnProperty(PROPERTY_JSON.getPropertyName())`. It may contain a nested
  `cssPosition` **object** (`{top, right, bottom, left, width, height}`) whose field names are
  explicit, so its field order does not matter — only the values per named field.

The two are **not** contractually required to be byte-identical (the json block can legitimately
be partial or absent), which is the main false-positive risk this spec must avoid (see §3.4).

### 2.2 The `top,right,bottom,left,width,height` order is intentional and must not change

Git history (confirmed): commit **`ae4f6aeb2`** in servoy-client
(*"SVY-12927 change css position order to follow real css order"*, lvostinar, 2018) deliberately
set the compact string order to mirror real CSS (`top right bottom left`). This has been stable
since 2018; it is **not** a regression. This spec must **not** change the platform parse/serialize
order — it only adds detection of authoring-time disagreement between the two representations.

### 2.3 Existing ServoyBuilder marker + quickfix infrastructure (the framework to extend)

All of the following already exist and are the pattern to follow:

**Raising markers (model plugin — `com.servoy.eclipse.model`):**

- `ServoyBuilder.checkSpecs(IPersist o, IProject project, SpecProviderState state)`
  (`com.servoy.eclipse.model/.../builder/ServoyBuilder.java:926`) already runs per persist, and for
  a `WebComponent` with a resolved spec it already reads **both** the item-level persist and the
  `json` own-property — it calls
  `checkMissingProperties((JSONObject)((WebComponent)o).getOwnProperty(PROPERTY_JSON...), spec, o, project, "")`
  at line 982. This is the natural insertion point for the new check: the code already has `o`
  (the WebComponent), the resolved `spec`, and the `json` JSONObject in hand.
- Markers are raised with the overload
  `ServoyBuilder.addMarker(IResource resource, String type, String message, int lineNumber, Pair<String,ProblemSeverity> problemSeverity, int priority, String location, IPersist persist)`
  (line 3290), which resolves the user-configurable severity via
  `getSeverity(...)` / `getTranslatedSeverity(...)`. Marker attributes (`Uuid`, `SolutionName`,
  `PropertyName`, …) are set on the returned `IMarker`, exactly as
  `addMissingPropertyFromSpecMarker` (line 1713) and `addDeprecatedPropertyUsageMarker`
  (line 1641) do.
- A **marker type id** is declared as a `public static final String … = _PREFIX + ".xxx"` constant
  on `ServoyBuilder` (e.g. `MISSING_PROPERTY_FROM_SPEC = _PREFIX + ".missingPropetyFromSpec"`,
  line 295; `DEPRECATED_PROPERTY_USAGE`, line 271) **and** as an `org.eclipse.core.resources.markers`
  extension in `com.servoy.eclipse.model/plugin.xml` that `<super type="com.servoy.eclipse.core.builderProblem">`
  (see the `missingPropetyFromSpec` extension at plugin.xml:891-901).
- A **user-configurable severity** is a `Pair<String, ProblemSeverity>` constant on `ServoyBuilder`
  (e.g. `DEPRECATED_SPECIFICATION = new Pair<>("deprecatedSpec", ProblemSeverity.WARNING)`,
  line 512), surfaced in `ServoyErrorWarningPreferencePage` (an `ErrorWarningPreferenceItem`, e.g.
  line 404) so the developer can raise/lower/ignore it.
- Human-readable message templates are `ServoyMarker` constants in
  `com.servoy.eclipse.model/.../builder/MarkerMessages.java` (`new ServoyMarker("… {0} …", ServoyBuilder.<TYPE>)`,
  filled via `.fill(...)`), optionally with a `fix` hint string.

**Quickfixes (UI plugin — `com.servoy.eclipse.ui`):**

- `ServoyQuickFixGenerator implements IMarkerResolutionGenerator`
  (`com.servoy.eclipse.ui/.../quickfix/ServoyQuickFixGenerator.java`) maps a marker type to one or
  more `IMarkerResolution`s inside `getResolutions(IMarker)`. It switches on `marker.getType()` and
  reads marker attributes (e.g. `Uuid`, `SolutionName`, `PropertyName`).
- Each marker type that offers a quickfix is wired with a `<markerResolutionGenerator
  class="com.servoy.eclipse.ui.quickfix.ServoyQuickFixGenerator" markerType="…">` entry under the
  `org.eclipse.ui.ide.markerResolution` extension in `com.servoy.eclipse.ui/plugin.xml`
  (lines 675-760; e.g. the `missingPropetyFromSpec` entry at 689-692).
- An individual resolution resolves the persist via
  `ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName).getEditingPersist(UUID.fromString(uuid))`
  and edits it (see `RemoveJSONPropertyQuickFix.run` / `.removeProperty`, which edits the
  WebComponent's `json` own-property and saves; and `BaseSetPropertyQuickFix`, which uses the
  standard `IPropertySource` adapter mechanism). Editing the persist re-triggers the build, which
  clears the marker when the mismatch is gone.

### 2.4 Evidence from the attached file

In the attached `run-011_fc_customer_row.frm`, element `lbl_customerid` has item-level
`cssPosition: "8,-1,12,-1,100,20"` (decodes to top=8, right=-1, bottom=12, left=-1, width=100,
height=20) but a `json.cssPosition` of `{top:8, right:12, bottom:-1, left:-1, width:100,
height:20}`. `right` and `bottom` disagree (`-1`/`12` vs `12`/`-1`) — exactly the mismatch this
marker must catch. The reporter counted 17 such elements across six forms (every right-anchored
element, plus one list container with width/height swapped).

## 3. Design

The detection and the quickfix both live in the form builder and share the exact same
rule: the json `cssPosition` echo must agree with the authoritative compact string on the
six position fields. The feature reuses the existing form-problem marker rather than a new
marker type.

### 3.1 Marker type, severity and message

- **Reuse the existing `ServoyBuilder.PROJECT_FORM_MARKER_TYPE`** (`…formProblem`) — no new
  marker type is declared. The specific problem is distinguished by a marker **attribute flag**
  `ServoyFormBuilder.CSS_POSITION_MISMATCH_PROBLEM` (`= "cssPositionMismatch"`, a
  `public static final String` at the top of the `ServoyFormBuilder` class body) set to
  `Boolean.TRUE` on the marker, which the quickfix generator keys on. No new
  `org.eclipse.core.resources.markers` extension is added to `com.servoy.eclipse.model/plugin.xml`.
- **User-configurable severity** on `ServoyBuilder`, default **WARNING** (per the ticket's "log a
  warning"): `public final static Pair<String, ProblemSeverity> CSS_POSITION_MISMATCH_SEVERITY =
  new Pair<>("cssPositionMismatch", ProblemSeverity.WARNING);`. Surfaced on
  `ServoyErrorWarningPreferencePage` as an `ErrorWarningPreferenceItem`
  (`Messages.ErrorWarningPreferencePage_cssPositionMismatch` + `messages.properties` entry) in the
  CSS-position/layout problem section.
- **Message** on `MarkerMessages`: `public static ServoyMarker CSSPositionMismatch = new
  ServoyMarker("Web component \"{0}\" in form \"{1}\" has a cssPosition string ({2}) that disagrees
  with its json cssPosition ({3}); Servoy uses the string, so the element may render in the wrong
  place.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "Reconcile the json cssPosition with the stored
  cssPosition string.");`

### 3.2 Detection (in `ServoyFormBuilder`)

Detection lives in `ServoyFormBuilder`, alongside the other form/web-component problem checks —
**not** in `ServoyBuilder.checkSpecs`. `addFormMarkers` calls a new private
`checkCssPositionMismatch(WebComponent, IProject)` for each web component (gated on
`o instanceof WebComponent`, right after `ServoyBuilder.checkSpecs`).

`checkCssPositionMismatch`:

1. Reads the authoritative compact position: `CSSPosition stringPos = wc.getCssPosition();`
   (`null` for the legacy size/location model → **do nothing**).
2. Reads the json echo:
   `JSONObject json = (JSONObject)wc.getOwnProperty(PROPERTY_JSON.getPropertyName());`
   then `Object jsonPos = json != null ? json.opt(PROPERTY_CSS_POSITION.getPropertyName()) : null;`
   If `jsonPos` is not a `JSONObject`, **do nothing** (no json echo / legacy shape).
3. If `cssPositionDisagrees(stringPos, (JSONObject)jsonPos)` is true, raise the marker on the form
   resource (`ServoyBuilderUtils.getPersistResource(form)`, `form = (Form)wc.getAncestor(FORMS)`)
   via `ServoyBuilder.addMarker(..., CSS_POSITION_MISMATCH_SEVERITY, IMarker.PRIORITY_NORMAL, null,
   wc)` and set attributes `Uuid`, `SolutionName` (= `project.getName()`) and
   `CSS_POSITION_MISMATCH_PROBLEM = Boolean.TRUE`. (No `PropertyName` attribute — the quickfix
   resolves the element from `SolutionName` + `Uuid` alone.)

`public static boolean cssPositionDisagrees(CSSPosition stringPos, JSONObject jsonPos)` (public and
static so it can be unit-tested directly, no live workspace) returns `false` when either argument is
null, otherwise returns true if any of the six fields disagree, via a small private helper:

```java
private static boolean fieldDisagrees(JSONObject jsonPos, String name, String stringValue)
{
    Object jsonValue = jsonPos.opt(name);
    if (JSONObject.NULL.equals(jsonValue)) jsonValue = null; // JSON null == no value
    return !Utils.equalObjects(jsonValue, stringValue);
}
```

Only the six fields (`top, right, bottom, left, width, height`) are compared; **extra json keys are
ignored**. `Utils.equalObjects` does the number/string normalization (`8`, `"8"`, `8.0` are equal)
and null handling, and `JSONObject.NULL` is normalized to `null` so a null authoritative value and a
JSON-null json value agree.

### 3.3 Marker lifecycle

Because the marker is a `PROJECT_FORM_MARKER_TYPE`, it is already deleted in
`ServoyFormBuilder.deleteMarkers(Form)` (which runs paired with `addFormMarkers` on every
incremental/affected-form build). So the marker is cleared and re-added each build with no extra
delete code, and applying the quickfix (which edits + saves the persist) re-triggers the build and
clears the marker once the json agrees. No dedicated project-level delete is needed.

### 3.4 Quickfix (reconcile json to the authoritative string)

`ReconcileCssPositionQuickFix` (a `WorkbenchMarkerResolution` in `com.servoy.eclipse.ui/.../quickfix/`,
modeled on `RemoveJSONPropertyQuickFix`):

- `getLabel()`: *"Fix json cssPosition to match the stored cssPosition string"*.
- `run(IMarker markerToFix)`: resolve the `WebComponent` from `SolutionName` + `Uuid`
  (`servoyProject.getEditingPersist(UUID.fromString(uuid))`); if `reconcile(webComponent)` made a
  change, `saveEditingSolutionNodes`. The override parameter is named `markerToFix` so it does not
  hide the instance `marker` field used by `findOtherMarkers`.
- `reconcile` sets each of the six fields to its authoritative value via a `setField` helper and
  returns whether anything changed; on change it calls `webComponent.flagChanged()` +
  `setJson(json)`. `setField` stores a null value as `JSONObject.NULL` and only writes (and reports
  a change) when `Utils.equalObjects` shows the current json value differs — so an already-correct
  json is never dirtied/re-serialized. The six `setField` calls are combined with the
  non-short-circuit `|` so every field is reconciled. Extra json keys are left untouched (mirroring
  detection, which ignores them).
- Rationale for the direction (json ← string): the compact string is the authoritative
  stored/rendered property, so reconciling means making the (cosmetic) json echo agree with what
  Servoy actually renders.

Wire it into `ServoyQuickFixGenerator.getResolutions`:
`if (type.equals(ServoyBuilder.PROJECT_FORM_MARKER_TYPE) && marker.getAttribute(ServoyFormBuilder.CSS_POSITION_MISMATCH_PROBLEM, false)) { return new IMarkerResolution[] { new ReconcileCssPositionQuickFix(marker) }; }`,
and register a `<markerResolutionGenerator class="…ServoyQuickFixGenerator"
markerType="com.servoy.eclipse.core.formProblem">` entry in `com.servoy.eclipse.ui/plugin.xml`.

## 4. Implementation plan

1. **`com.servoy.eclipse.model/.../builder/ServoyBuilder.java`** — add only the severity constant
   `CSS_POSITION_MISMATCH_SEVERITY` (`new Pair<>("cssPositionMismatch", ProblemSeverity.WARNING)`).
   No marker-type-id constant and no project-level delete.
2. **`com.servoy.eclipse.model/.../builder/ServoyFormBuilder.java`** — add the
   `CSS_POSITION_MISMATCH_PROBLEM` attribute-name constant (top of class), the
   `checkCssPositionMismatch` detection helper + its call from `addFormMarkers` (gated on
   `WebComponent`), and the `public static cssPositionDisagrees` + private `fieldDisagrees`
   comparison (§3.2).
3. **`com.servoy.eclipse.model/.../builder/MarkerMessages.java`** — add the `CSSPositionMismatch`
   `ServoyMarker` constant targeting `PROJECT_FORM_MARKER_TYPE` (§3.1).
4. **`com.servoy.eclipse.ui/.../preferences/ServoyErrorWarningPreferencePage.java`** (+
   `Messages.java` / `messages.properties`) — add an `ErrorWarningPreferenceItem` for
   `CSS_POSITION_MISMATCH_SEVERITY` in the CSS-position/layout section.
5. **`com.servoy.eclipse.ui/.../quickfix/ReconcileCssPositionQuickFix.java`** (new) — §3.4.
6. **`com.servoy.eclipse.ui/.../quickfix/ServoyQuickFixGenerator.java`** — add the
   `PROJECT_FORM_MARKER_TYPE` + `CSS_POSITION_MISMATCH_PROBLEM` attribute branch returning the new
   quickfix.
7. **`com.servoy.eclipse.ui/plugin.xml`** — register the `ServoyQuickFixGenerator`
   `markerResolutionGenerator` for `markerType="com.servoy.eclipse.core.formProblem"`.
8. **Tests** — a `plainjunit` test that calls `ServoyFormBuilder.cssPositionDisagrees` directly (no
   reflection): assert mismatch is detected for swapped right/bottom and width/height, a missing or
   JSON-null field (while the string has a value), and extra key is ignored; and **not** detected for
   matching values, numeric normalization (`8` vs `"8"` vs `8.0`), null vs JSON-null, and null
   string/json sides. Compact strings are decoded with `PersistHelper.createCSSPosition` so the
   intentional field order is encoded by the production tokenizer. Place it in
   `com.servoy.eclipse.model.tests` under the `plainjunit.builder` package (junitVersion 6).
9. **Post-edit checklist (per AGENTS.md):** `organizeImports` + `formatFile` on each edited Java
   file, then `getCompilationErrors`; resolve any blocking Spotbugs findings in new/changed code.

## 5. Acceptance criteria

- [x] A web component whose stored compact `cssPosition` string disagrees with its `json.cssPosition`
      object on any of the six fields produces a **warning** marker (default WARNING,
      user-configurable on the Error/Warning preference page), raised as a `PROJECT_FORM_MARKER_TYPE`
      marker carrying the `cssPositionMismatch` attribute.
- [x] The marker message names the component, the form, the compact string, and the json
      cssPosition.
- [x] The attached `run-011_fc_customer_row.frm` case (`lbl_customerid`, right/bottom swapped)
      triggers the marker; a width/height swap (the list-container case) also triggers it.
- [x] A quickfix is offered on the marker that reconciles the six fields of the `json.cssPosition`
      object to the authoritative compact string; after applying it and rebuilding, the marker is
      gone and the json agrees with the string.
- [x] Values are compared with `Utils.equalObjects`, so `8` vs `"8"` vs `8.0` are equal, and a null
      authoritative field and a JSON-null json field agree (no false positive). Extra json keys
      beyond the six are ignored. A legacy element (no `CSSPosition`) or a json with no
      `cssPosition` object produces **no** marker.
- [x] The platform compact-string parse/serialize order (`top,right,bottom,left,width,height`,
      SVY-12927 / `ae4f6aeb2`) is unchanged.
- [x] `getCompilationErrors` is clean on all edited model and ui files; the `plainjunit` comparison
      test passes.

## 6. Out of scope

- Ticket options 1 and 2 (skill wording in the Authoring Checklist, and the reviewer/generator
  decode-and-compare check) — these live in the **`skill4servoy`** repository and are handled in a
  separate session.
- Changing the platform's compact `cssPosition` parse/serialize order or the meaning of the `json`
  echo.
- Any NG-render-time warning or runtime log line — the detection is a build-time ServoyBuilder
  marker only, per the authoritative placement constraint.
- Auto-correcting the mismatch during the build without developer action — the fix is offered as a
  quickfix, not applied silently.
- Flagging elements that use the legacy size/location model (no `CSSPosition`), or treating extra
  json keys beyond the six cssPosition fields as a mismatch.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Default severity WARNING (user-configurable) — confirmed as the desired default. | reporter | resolved |
| Reconcile direction json ← string — confirmed (the compact string is authoritative). A string ← json resolution is not offered. | reporter | resolved |
