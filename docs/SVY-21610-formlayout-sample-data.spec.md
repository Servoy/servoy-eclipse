# Spec: SVY-21610 — sample data in the `getFormLayout` / form-template render

## 1. Goal

Make the stateless form-template render (`/formtemplate/<form>.html`, SVY-21460 — the render
behind the `getFormLayout` MCP tool) show **data**, not empty component chrome: a textbox shows
text, a data label shows a value instead of the literal string `"DataLabel"`, and a
foundset-based grid shows **3 rows** of cells instead of headers over an empty body.

The data is **synthetic and deterministic** — no database, no running client, no websocket. The
render stays stateless. "Deterministic" is a hard requirement, not a nicety: the agent loop is
*render → change one thing → render again → diff*, so two consecutive `getFormLayout` calls on an
unchanged form must produce byte-identical values. Random sample values would make the tool
useless for its own purpose.

Sample data is **on by default** for `getFormLayout`, with an opt-out for inspecting bare chrome.

## 2. Background

### 2.1 The two tickets are one problem

SVY-21610 was filed as "how do I show fixed text in a textbox without code?" — the Developer agent
had written four form variables plus an `onLoad` to fill specimen fields, which is code where the
prompt asked for none, and which *still* rendered empty at design time because `onLoad` does not
run there. Johan closed SVY-21590 (*getFormLayout renders no data*) as a duplicate of this one:
both are the same blind spot. The agreed direction in the comments (Johan, Sean) is to push data
into the generated JSON, and it explicitly does **not** have to be real data — "lorum ipsum type
data, or perhaps some dummy values per data type" (Sean), "we can push some stuff in the json we
generate for the getFormLayout()" (Johan).

There is also a smaller, separate answer to the ticket's literal question, which this spec
delivers for free (§3.3): a variable **initialiser** (`var vFilled = 'Value';`) instead of an
`onLoad` assignment. `ScriptVariable.getInitValue()` reads that default value, so a form variable
with an initialiser renders its value at design time. That is exactly what the reporter asked
for; it should also be stated in the agent skills, but that is outside this spec.

### 2.2 Where the data is dropped today (verified in code)

`HeadlessFormTemplateContent.getFormDataJS` →
`new AngularFormGenerator(fs, flattenedForm, name, /* isDesigner */ false, /* formTemplate */ true, null)`
produces correct *structure* (SVY-21460 added `formTemplate` mode precisely so custom-array /
custom-object typed properties such as a grid's `columns` are written). Values are dropped at four
distinct places:

| # | Where | What happens |
|---|-------|--------------|
| 1 | `DataproviderPropertyType.valueInTemplate()` (`DataproviderPropertyType.java:141`) | returns `false` unconditionally — *"we have no value until the client is created for dataproviders"*. `FormElementToJSON.toJSONValue` therefore writes **nothing at all**, so the component model has no `dataProviderID` key → empty textbox/label. This is the SVY-21610 symptom. |
| 2 | `FoundsetPropertyType.toTemplateJSONValue()` (`FoundsetPropertyType.java:94`) | writes a deliberate empty dummy: `serverSize 0`, `selectedRowIndexes []`, `viewPort {startIndex 0, size 0, rows []}` → grid renders headers, zero rows. |
| 3 | `FoundsetLinkedPropertyType.toTemplateJSONValue()` (`FoundsetLinkedPropertyType.java:181`) | writes only `{forFoundset: ...}`; the wrapped value is emitted only when the wrapped type says `valueInTemplate` — and the wrapped type is a dataprovider, which per #1 says `false`. So `column.dataprovider[i]` is `undefined` even if rows existed. |
| 4 | `ChildrenJSONGenerator.writeFormElement` (`ChildrenJSONGenerator.java:356-362`) | the `IDesignerDefaultWriter` hook — the one existing fake-data mechanism — is invoked only `if (designer)`. In the form-template render `designer == false`, so even `ValueListPropertyType.toDesignerDefaultJSONValue` (which already fabricates `Item1`/`Item2`/`Item3`) never runs. |

### 2.3 The client-side blocker

`FormTemplateServoyApi` (`node/src/formtemplate/formtemplate_component.component.ts:299`) calls
`super(item, formname, absolute, formservice, servoyService, /* isDesigner */ true)`. So
`servoyApi().isInDesigner()` is `true` for the whole form-template render. Server-side sample data
alone is therefore **not sufficient** — several components take a design branch that discards it:

- `DataGrid.svyOnInit()` (`datagrid.ts:1176-1193`) returns early when `isInDesigner()`, so
  `isRendered` is never set and `initRootFoundset()` → `setGridOption('serverSideDatasource', …)`
  never runs. The grid shows **zero rows no matter what the server sends**.
- `NGGridDirective.svyOnInit` / `onGridReadyBase` (`nggrid.ts:97,109`) skip init entirely.
- `bootstrapcomponents-datalabel` pipes its value through `DesignTextPipe`
  (`datalabel.ts:47`), which returns the literal string `"DataLabel"` — SVY-21590's headline symptom.
- `servoyextra-table` (`table.ts:301,337`) returns out of `svyOnChanges` for the `foundset` case,
  never registering its change listener.
- `servoycore-listformcomponent` forces `numberOfCells = 1` and returns a single empty
  `designerViewportRows` entry (`listformcomponent.ts:731,1085`).

### 2.4 Why `isInDesigner()` must become `false` (the cheap-vs-expensive fork)

Across `bootstrapcomponents`, `aggridcomponents`, `servoy-extra-components` and TiNG core there are
~32 `isInDesigner()` sites. They fall into three groups:

- **A — chrome only (17 sites).** Hide a download icon, force a min-height, suppress a floating
  filter. Harmless either way.
- **B — data blockers (9 sites).** §2.3. These must *stop* running.
- **C — fake-data fabricators (6 sites).** They synthesise their own placeholder data.

If the form-template render keeps `isInDesigner() === true` and we add an extra flag that each
design branch must additionally consult, then **all ~32 sites** become sites that need updating,
and any component package not updated stays broken. If instead the render reports
`isInDesigner() === false` and we add a *separate* `hasSampleData()` that only group C consults,
group B is unblocked for free, group A simply stops running, and only **group C (6 sites)** needs
code changes. We take the second option.

The flip is a real behaviour change, not a no-op: today `isDesigner = true` is also what makes
`FormService.dataPush`/`applyDataprovider` early-return (`form.service.ts:861,903`, set via
`setDesignerMode()`). That protection must be re-established another way — and it already is,
twice over: `FormTemplateComponent.datachange()` is inert by construction, and
`SabloService.callService` returns immediately with no `wsSession`
(`sablo.service.ts:259`). This spec adds explicit test coverage for that so a later change cannot
silently re-introduce a push path.

### 2.5 Two property types have no design-time projection at all

`DatasetPropertyType` (behind `servoyextra-treeview`'s `jsDataSet`) implements neither
`IFormElementToTemplateJSON` nor `ISupportTemplateValue`, and there is no
`FoundsetTreePropertyType` template path either (behind `servoyextra-dbtreeview`). Producing
sample data for trees is a separate, larger job. **Out of scope here** (§6): those two components
keep their existing hardcoded fake data, with their condition widened so it still fires.

## 3. Design

### 3.1 Threading the flag (Java)

`formTemplate` is already a dedicated boolean for this one render path, threaded through
`FormElement`, `FormElementHelper.getFormElement`, `IFormElementCache.getFormElement`,
`ChildrenJSONGenerator`, `AngularFormGenerator` and `INGFormElement`. Add a second boolean
`sampleData` alongside it, in the same positions, following the existing
`boolean designer, boolean formTemplate` precedent:

- `FormElement` — new field + accessor `hasSampleData()`; new constructor arity; keep every
  existing overload delegating with `sampleData = false`.
- `INGFormElement.hasSampleData()` — `default false`, mirroring `isFormTemplate()`.
- `FormElementExtension.hasSampleData()` — delegate to the parent form-element context.
- `FormElementHelper.getFormElement(…, designer, formTemplate, sampleData)` — new arity; the
  existing 4- and 5-arg overloads delegate with `false`. Sample-data form elements must **not** be
  cached (same reasoning as `formTemplate` today: a distinct render projection).
- `ChildrenJSONGenerator` — new constructor arity; pass `sampleData` down every recursive
  `new ChildrenJSONGenerator(...)` call (form-component children, layout containers, LFC).
- `AngularFormGenerator` — new constructor arity used by `HeadlessFormTemplateContent`.

**Invariant:** `sampleData == true` implies `formTemplate == true`. Sample data is only ever
produced by the form-template render. Assert this in the `AngularFormGenerator` constructor rather
than leaving it to convention.

### 3.2 `SampleDataProvider` (new, `servoy_ngclient`)

One new class, `com.servoy.j2db.server.ngclient.property.types.SampleDataProvider`, holding all
value fabrication so the property types stay thin and the rules are testable in one place.

```java
public final class SampleDataProvider
{
    public static final int SAMPLE_ROW_COUNT = 3;

    /** Sample value for a dataprovider, or null when none should be written. */
    public static Object getSampleValue(String dataProviderID, int rowIndex,
        FormElementContext context);
}
```

Resolution order for `dataProviderID`:

| Kind | Source of the value | Fabricated? |
|------|--------------------|-------------|
| form variable / global-scope variable | `ScriptVariable.getInitValue()` — the **real** initial value | no |
| dataprovider with a valuelist attached | a display value from the valuelist (real values when resolvable, else the existing `Item1..3`) | partly |
| column, `TEXT` | deterministic word, seeded by the dataprovider name | yes |
| column, `INTEGER` / `NUMBER` | deterministic number (the component's own `format` still applies client-side) | yes |
| column, `DATETIME` | fixed reference date | yes |
| column, `MEDIA` | `null` — nothing written | — |
| unresolvable dataprovider | `null` — nothing written | — |

Variable lookup uses `ScopesUtils.isVariableScope(id)` + `Form.getScriptVariable(name)` /
the flattened solution's global scope, mirroring
`DataproviderPropertyType.getDataLinks` (`DataproviderPropertyType.java:117-137`). Column type
resolution reuses `FlattenedSolution.getDataProviderForTable` + the existing
`NGUtils.getDataProviderPropertyDescription` type mapping rather than re-deriving types.

Determinism: seed from a stable hash of the dataprovider name (and `rowIndex` for grid rows).
Never `Math.random()`, never `new Date()` for the DATETIME case.

### 3.3 `DataproviderPropertyType` — write a sample value

Make `DataproviderPropertyType` implement `IFormElementToTemplateJSON<String>`. In
`FormElementToJSON.toJSONValue` that branch is checked **before** the `ISupportTemplateValue` /
`valueInTemplate` branch (`NGConversions.java:339-347`), so this takes over cleanly:

```java
public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue,
    PropertyDescription pd, FormElementContext formElementContext)
{
    if (!hasSampleData(formElementContext)) return writer;   // today's behaviour exactly: write nothing
    Object sample = SampleDataProvider.getSampleValue(formElementValue, 0, formElementContext);
    if (sample == null) return writer;
    // write with its client-side type when one is needed (e.g. a Date needs _T:"date")
    ...
}
```

`valueInTemplate()` keeps returning `false` and is left untouched — it is no longer consulted for
this type, and leaving it alone keeps the non-sample path provably identical.

A `DATETIME` sample must be written through
`JSONUtils.writeConvertedValueWithClientType(writer, key, <type>, …)` so the client's
`ConverterService` sees `_T` and converts it to a real `Date`; otherwise the component receives a
raw number and formatting/alignment checks would be measured against the wrong thing.

### 3.4 `FoundsetPropertyType` — 3 synthetic rows

In sample mode replace the empty dummy with:

```
serverSize        : 3
selectedRowIndexes: [0]
multiSelect       : false
findMode          : false
hasMoreRows       : false
viewPort          : { startIndex: 0, size: 3, rows: [ {_svyRowId:"1"}, {_svyRowId:"2"}, {_svyRowId:"3"} ] }
```

`_svyRowId` is required: `FoundsetManager.getViewPortRow` reads `viewPortRow._svyRowId`
(`datagrid.ts:5211`) and `aggrid`'s `getRowId` is built from the manager's own UUID plus the
viewport index, so no other row key is needed.

`selectedRowIndexes: [0]` is deliberate — selected-row styling was one of the defects SVY-21590
could not see.

**Critical invariant: `serverSize === viewPort.rows.length` and `hasMoreRows: false`.** If the grid
believes more rows exist, `FoundsetManager.getLastRowIndex()` returns `-1`
(`datagrid.ts:5243-5252`) and `FoundsetDatasource.getRows` calls
`foundset.loadExtraRecordsAsync()`, which pushes a request and returns a deferred promise
(`foundset_converter.ts:382`). With no websocket session **that promise never resolves** and the
grid sits in a permanent loading state. Hold the invariant and aggrid takes its synchronous
`callback(getViewPortData(...), getLastRowIndex())` path instead.

### 3.5 `FoundsetLinkedPropertyType` — emit the single value

In sample mode, emit the wrapped value under the `SINGLE_VALUE` (`"sv"`) key regardless of the
wrapped type's `valueInTemplate`. The client's `SingleValueState.generateWholeViewportFromOneValue`
then fans that one value across the foundset's viewport size
(`foundsetLinked_converter.ts:175-181`), which is 3 per §3.4 — so every row of every column gets a
cell value, and `column.dataprovider[rowIndex]` resolves in
`FoundsetManager.getViewPortRow`.

Because the wrapped type is a dataprovider, the value itself comes from §3.3 — one code path
serves both the plain-field and the grid-column case. (A per-row-distinct variant is possible later
by emitting a full `vp` viewport instead; `sv` is chosen for v1 because it is strictly simpler and
3 identical rows are enough to verify layout, alignment and formatting.)

### 3.6 `ChildrenJSONGenerator` — reuse the existing `IDesignerDefaultWriter` hook

Widen the gate at `ChildrenJSONGenerator.java:356` from `if (designer)` to
`if (designer || sampleData)` for the `IDesignerDefaultWriter` loop only. The surrounding
designer-only writes (`svyVisible`, `svyInheritedElement`, design ids) stay gated on `designer`
alone.

This is deliberately reuse rather than a new mechanism: `ValueListPropertyType` already implements
exactly this interface to fabricate `Item1`/`Item2`/`Item3`, and it means a third-party component
package can supply its own sample value for its own property type without a core change.

### 3.7 Server → page → client plumbing (`servoy-eclipse`)

- `HeadlessFormTemplateContent.getFormDataJS(String formName, boolean sampleData)` — pass
  `sampleData` into `AngularFormGenerator`. Keep the 1-arg overload delegating with `false`.
- `FormTemplateHttpEndpoint` — accept `?sampleData=false` on
  `/formtemplate/<form>.html` (**default true**), and emit a
  `window.formtemplateSampleData` marker next to the existing `window.formtemplateName`.
- `ServoyFormTemplateComponent` — read the marker, pass it into `<svy-formtemplate>`.
- `FormTemplateComponent` / `FormTemplateServoyApi` — pass `isDesigner = false` to `ServoyApi`
  (§2.4) and carry the sample-data flag.

### 3.8 `ServoyApi.hasSampleData()` (`@servoy/public`)

```typescript
/**
 * Returns true when this component is rendered in the stateless form-template render with
 * synthetic sample data supplied by the server (SVY-21610). Components that fabricate their
 * own placeholder data in the designer should not do so when this is true.
 */
public hasSampleData(): boolean { return false; }
```

Added as a **concrete method with a default**, not an abstract one, so `ServoyApiTesting` and any
external implementor keep compiling. `FormTemplateServoyApi` overrides it.

### 3.9 Component changes (group C)

| Component | Repo | Change |
|-----------|------|--------|
| `aggrid-*` editor hint (`datagrid.ts:3807`, `powergrid.ts`) | aggridcomponents | `isInDesigner()` → `isInDesigner() \|\| hasSampleData()` — the "drop columns here" hint is still wanted for a column-less grid |
| `bootstrapcomponents-imagemedia` (`imagemedia.ts:79`) | bootstrapcomponents | same widening — sample media is out of scope, keep the placeholder image |
| `bootstrapcomponents-choicegroup` (`choicegroup.ts:41`) | bootstrapcomponents | **remove** the hardcoded `Item1..3`; §3.6 now supplies it server-side |
| `servoyextra-treeview` (`treeview.ts:114`) | servoy-extra-components | widening — `dataset` sample data out of scope (§2.5) |
| `servoyextra-dbtreeview` (`dbtreeview.ts:554`) | servoy-extra-components | widening — same |
| `servoycore-listformcomponent` (`listformcomponent.ts:731,1085`) | servoy-eclipse | no change needed; drops out once the foundset carries rows. Verify. |

### 3.10 MCP tool (`Servoy-Copilot`)

`getFormLayout` gains a `sampleData` boolean param, **default true**, forwarded as the query
parameter from §3.7. The tool description must change too: it currently states *"this is a
DESIGN-TIME render … with NO DATA … Do NOT expect any data"*, which becomes wrong and would
actively mislead the agent.

## 4. Implementation order

1. `servoy-client` — flag threading (§3.1), `SampleDataProvider` (§3.2), the three property types
   (§3.3–3.5), the `ChildrenJSONGenerator` gate (§3.6).
2. `servoy-eclipse` — endpoint + page marker (§3.7), `hasSampleData()` on `ServoyApi` (§3.8), the
   `isInDesigner` flip (§2.4).
3. Tests (§5).
4. Component repos (§3.9) — separate repos, separate commits.
5. `Servoy-Copilot` — tool param + corrected description (§3.10).

## 5. Acceptance criteria

- [ ] With `sampleData` off, the generated form-state JSON is **byte-identical** to today's for the
      same form — the whole feature is inert when the flag is off.
- [ ] Two consecutive renders of an unchanged form produce **byte-identical** JSON (determinism).
- [ ] A textbox/textarea bound to a **form variable with an initialiser** shows that initial value.
- [ ] A textbox bound to a **text column** shows a non-empty deterministic value.
- [ ] A `bootstrapcomponents-datalabel` shows a value, **not** the literal string `"DataLabel"`.
- [ ] An `aggrid-groupingtable` / `aggrid-datasettable` renders **3 rows** with populated cells, and
      does **not** hang in a loading state (`serverSize === rows.length`, `hasMoreRows: false`).
- [ ] A date-typed dataprovider arrives client-side as a real `Date` (`_T` written), so format and
      alignment can be judged.
- [ ] A valuelist-backed component shows valuelist display values via the
      `IDesignerDefaultWriter` hook — and `choicegroup` no longer fabricates its own.
- [ ] `servoyApi().isInDesigner()` is `false` in the form-template render, and **no** data is pushed
      to the server: no websocket opened, `datachange`/`apply` inert, `SabloService.callService`
      never reached. Asserted in a spec, not assumed.
- [ ] The designer render and the real NG client are **behaviourally unchanged** (`sampleData`
      defaults to `false` everywhere they go through).
- [ ] Java compiles clean; no new blocking spotbugs (two highest severities) in new/changed code.
- [ ] `npx tsc --noEmit -p src/tsconfig.app.json`, `npx ng lint` and the Vitest suites pass.

## 6. Out of scope

- Sample data for `dataset` (`servoyextra-treeview`) and `foundsettree`
  (`servoyextra-dbtreeview`) — neither type has any design-time projection today (§2.5). Those
  components keep their existing hardcoded fake data.
- Sample **media** / images.
- Per-row-distinct grid values (v1 fans one value across the 3 rows, §3.5).
- Real data from the database — that is the separate `FormPreviewNGClient` / Cypress path, which
  should stay a distinct tool rather than changing `getFormLayout`'s stateless contract.
- Working handlers, running server-side scripting, live updates.
- Skills guidance telling the agent to prefer variable initialisers over `onLoad` (§2.1) — a
  content change, tracked separately.

## 7. Open questions

| # | Question | Default taken |
|---|----------|---------------|
| 1 | Row count for sample foundsets | **3** (agreed with the user) — enough for striping, selection and alignment without bloating the JSON |
| 2 | `sampleData` default for `getFormLayout` | **on** (agreed with the user); `sampleData=false` still available for bare chrome |
| 3 | Two booleans (`formTemplate`, `sampleData`) or one enum? | Two booleans, matching the existing `designer, formTemplate` precedent; the implied invariant is asserted (§3.1). Revisit if a fourth mode appears |
| 4 | Sample text style — lorem ipsum, or names/words seeded by column name? | Seeded words. Sean explicitly allowed lorem ipsum; seeded words read better in a specimen sheet and cost the same |
| 5 | Should `selectedRowIndexes` be `[0]` or `[]`? | `[0]` — selected-row styling was an unverifiable defect class in SVY-21590 |

---

## Addendum: nested forms in the stateless render (second commit)

### A.1 The gap

A form shown *inside* another form through a `form`-typed property rendered as an empty body: a
`bootstrapcomponents-tabpanel` tab, `tablesspanel`, `accordion`, `splitpane` and
`servoycore-formcontainer`. Not caused by the sample-data work — a pre-existing SVY-21460 gap — but
sample data made it conspicuous, since the outer form suddenly showed values while the tab stayed
blank.

The chain: `FormPropertyType.toTemplateJSONValue` *does* write the referenced form (as a name or a
UUID) into the model, and `ComponentTemplateGenerator` emits
`<svy-formtemplate *ngIf="isFormAvailable(name)" [name]="name">` for it. But
`isFormAvailable()` → `FormService.hasFormCacheEntry(name)`, and the endpoint generated exactly
**one** form, so the nested form had no cache entry and the guard was false. At runtime this works
because `formWillShow` round-trips to the server; statelessly there is no round-trip.

Form components and list-form-components were never affected — `ChildrenJSONGenerator` writes those
inline as `children_<property>`.

### A.2 Why the transport needed no change

`AngularFormGenerator.generateJS()` already emits `{"<formName>": {...}}` keyed by form name, and
the route already read `formStateContainer[this.formName]`. One blob can therefore carry several
forms with no format change — only the producer and that one consumer line needed touching.

`FormService.createFormCache` already handles the nested case too: when the newly cached form is not
the one with a live Angular component it calls `detectChanges()` on every cached form component,
with the comment *"1 of those could show the new form in it (as tab for example)"*.

### A.3 Design

**Server** (`HeadlessFormTemplateContent`):

- `collectFormsToGenerate(form, fs)` — breadth-first from the requested form, de-duplicated by form
  name, capped at `MAX_NESTED_FORM_DEPTH = 5`. The requested form is always first.
  - A **visited set is required, not defensive**: a tabpanel whose tab shows a form that itself
    contains a tabpanel pointing back is legal Servoy and would otherwise recurse forever.
  - **All tabs, not just the active one.** Which tab is visible is decided client-side from the
    model (`tabIndex`/`activeTabIndex`), so generating only tab 0 would render a form wrong whenever
    another tab is selected by default. The cost is page size, not server round-trips.
- `getNestedForms` / `collectFormsFromWebObject` — visits the form's persists, reads
  `spec.getProperties(FormPropertyType.INSTANCE)` off each web object, then recurses into
  `WebCustomType` children so a form-typed property declared *inside a custom type* is found (a
  tabpanel's `tabs` is a custom-type array whose `containedForm` is the form-typed property). Same
  shape as `ServoyFormBuilder.addWebComponentMissingReferences`.
- `resolveForm` — the stored value may be a form **name or UUID** (a tabpanel tab stores the UUID),
  so it does `fs.getForm` then `fs.searchPersist`, mirroring `FormPropertyType.getForm`.
- The **requested** form is generated without a try/catch so its failure still surfaces as 404/500;
  each **nested** form is best-effort, so one broken nested form renders one empty tab instead of
  taking the page down.
- `getComponentSpecsJSON` unions the specs across the same collected set — otherwise a component
  appearing only inside a tab has no client-side spec and its typed properties silently fail to
  convert.

**Client**:

- `ServoyFormTemplateComponent.ngOnInit` builds a cache for **every** key in the blob, requested
  form first; only the requested form is rendered at the top level.
- `FormTemplateComponent.ngOnInit` reads sample-data mode from the `window.formtemplateSampleData`
  marker when the input was not set. A nested `<svy-formtemplate>` is created by the *generated*
  template, which binds only `[name]`, so it cannot be handed the input — without this a tab's
  fields would render empty while the outer form's showed values.

### A.4 Acceptance criteria

- [ ] A form with a tabpanel renders the contained form's components inside the tab body.
- [ ] All tabs are generated, not only the active one.
- [ ] A cyclic nesting (A contains B contains A) terminates and renders.
- [ ] Nesting deeper than `MAX_NESTED_FORM_DEPTH` is truncated, not failed.
- [ ] A nested form's components convert their typed properties (specs unioned).
- [ ] Sample data reaches nested forms, not just the top-level form.
- [ ] A form with no nested forms produces a blob with exactly one key (no behaviour change).
- [ ] A broken nested form leaves the rest of the page rendering.
