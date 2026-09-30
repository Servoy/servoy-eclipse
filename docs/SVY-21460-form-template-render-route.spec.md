# Spec: SVY-21460 — Stateless form-template render route (real DOM of any form, no data, no client, no websocket)

## 1. Goal

Add a **fully stateless** way to render the **real runtime HTML DOM** of any Servoy form in the browser, purely as a developer/AI read aid:

- **URL:** `/formtemplate/<formname>.html` — no `clientnr`, no `solutionname`. The active (editing) flattened solution is always used, and a form name is unique across it.
- **No session, no websocket, no server round-trips after the initial page load.** The form-state JSON **and the component client-side specs it needs** are pushed **into the served HTML at load time** (inline `<script type="application/json">`) so the route can register the specs itself for that form. The **solution CSS is served by its own stateless endpoint** `/formtemplate/stylesheet.css` (a normal `<link>` in the page), produced the same design-time way the solution stylesheet is normally produced — it is not inlined. If the form changes, the browser is simply refreshed.
- **No data.** Component models carry only their design-time property values; no foundsets, no data providers, no i18n resolution required.
- **No handlers / no communication layer.** Button clicks and other events do nothing (for now). Nothing is sent back to the server.
- **Production-like DOM.** Render with a **dedicated, much simpler copy of the form component** (`FormTemplateComponent`, selector `svy-formtemplate`) that produces the same runtime `.svy-form` DOM as `svy-form` — NOT the designer's `svy-designform` chrome (no `svy-id`, no `designclass`, no ghosts/decorators/wireframe/variants).

The approach: rather than reuse the runtime `FormComponent` (which pulls in `FormService`, `SabloService`, `ServoyService`, `ConverterService`, `WindowRefService`, etc.), create a **stripped copy** of `form_component.component.ts` — exactly as the designer already keeps a copy in `designer/designform_component.component.ts` — that injects **only the minimal services needed to render components without data** (real or mocked), and has `WebPackagesListener` generate the per-component templates into it too (the same marker-replacement it already does for the runtime and designer files). The copy carries the identical `.svy-form` template markup so the DOM matches production, but omits everything tied to a live client (data push, handlers, resize/communication, form lifecycle notifications).

## 2. Background

### 2.1 The ticket vs. the approved scope

SVY-21460 originally scoped a non-rendering Java-only projection (tag tree + JSON model via an MCP endpoint). Triage (`docs/SVY-21460-triage.md`) flagged a scope divergence and a human chose the larger, rendered-DOM direction, then refined it to a **stateless, websocket-free** design (this spec) rendered by a **simplified copy** of the form component (mirroring the designer's `designform_component.component.ts` copy). The human's decision overrides the ticket's "NOT meant to render" framing.

### 2.2 What the runtime FormComponent needs (verified in code)

`FormComponent` (`node/src/ngclient/form/form_component.component.ts`) extends `AbstractFormComponent` and injects `FormService`, `SabloService`, `ServoyService`, `LoggerFactory`, `ChangeDetectorRef`, `ElementRef`, `Renderer2`, `ConverterService`, `DOCUMENT`, `WindowRefService` (constructor lines 255–268). Its template (lines 136–213) hard-codes the `.svy-form` structure, `.svy-wrapper` `position:absolute` wrappers, `.svy-layoutcontainer` responsive divs, and one `<ng-template>` per `servoycore-*` component; `<!-- component template generate ... -->` markers are filled at build time by Java (`WebPackagesListener` rewrites this file and generates `allcomponents.module.ts`).

Client-coupled behaviour and how it degrades statelessly:

- `ngAfterViewInit`/`ngAfterViewChecked` → `formservice.resolveComponentCache(this)` → `sabloService.callService('formService','formLoaded',…)` (`form.service.ts` line 364). **`SabloService.callService` returns immediately with no-op when there is no websocket session** (`sablo.service.ts` line 250: `if (!this.wsSession) return;`). So with no `connect()`, this is inert.
- `onResize`/`datachange` → `formservice.sendChanges(...)` → early-return when `isInDesigner` is true (`form.service.ts` lines 694, 736), and also no-op without a wsSession.
- `getHandler(...)` → `formservice.executeEvent(...)` → `callService('formService','executeEvent',…)` — inert without a wsSession, but we additionally want handlers to be `null` so no event functions are wired at all.
- `ngOnDestroy` → `formservice.destroy(name,true)` → `formUnloaded` callService — inert without a wsSession.

Net: **without a websocket session, every server call from `FormComponent`/`FormService` is already a silent no-op.** The stateless design leans on this rather than forking the component.

### 2.3 The critical gap: TypesRegistry is empty without the server handshake

`FormService.createFormCache(formName, jsonData, url)` (`form.service.ts` lines 483–518) is **pure-local**: it builds a `FormCache` and, in `walkOverChildren` (lines 802–885), for each component looks up `this.typesRegistry.getComponentSpecification(elem.specName)` (line 827) and converts every model property with `converterService.convertFromServerToClient(value, componentSpec?.getPropertyType(propName), …)` (lines 894–896).

`TypesRegistry.getComponentSpecification(name)` returns `undefined` unless specs were registered via `addComponentClientSideSpecs(...)` (`types_registry.ts` lines 88–95). A grep shows **the running app only ever calls `addComponentClientSideSpecs` from the Java side** over the connection — the designer does it in `DesignerWebsocketSession.java` (lines 218, 664); the runtime client via the server index-page/handshake. There is **no client TS runtime path** that fills the registry. Consequence: a no-websocket load has an **empty registry**, so typed properties (dates, valuelists, custom objects/arrays, etc.) would not convert. **Therefore the served HTML must also carry the component client-side specs**, and the stateless bootstrap must feed them into `TypesRegistry.addComponentClientSideSpecs` before `createFormCache`.

Note: `ServoyService`'s constructor already registers the global property/converter *types* (Date, CustomArray/Object, Valuelist, Foundset, Component, etc.) into the `TypesRegistry` locally with no server call (`servoy.service.ts` lines 104–123). So the global type factories are available as soon as `ServoyService` is constructed; only the **per-component specs** are missing and must be injected.

### 2.4 The form-component copy precedent (WebPackagesListener)

The runtime `form_component.component.ts` is **not** hand-maintained end-to-end: `WebPackagesListener.java` rewrites it at build time, replacing marker regions with generated per-component `<ng-template>`s and `viewChild` refs. Crucially it already does the **same generation into a second, copied file** — the designer's `designer/designform_component.component.ts`:

- `content = FileUtils.readFileToString(new File(projectFolder, "src/ngclient/form/form_component.component.ts"), …)` (line 426)
- `editorContent = FileUtils.readFileToString(new File(projectFolder, "src/designer/designform_component.component.ts"), …)` (line 427)
- both get the `<!-- component template generate start/end -->` and `// component viewchild template generate start/end` (and structure) regions filled (lines 430–456), and are written back if changed (lines 470, 477).

So there is an established pattern: **keep a copy of the form component with its own services/behaviour, and register it in `WebPackagesListener` so the generated component templates are injected into it too.** This spec follows exactly that pattern for a third, *simpler* copy dedicated to stateless rendering. The template markup is copy/paste-maintained alongside the other two (accepted cost, same as the designer copy today), but the surrounding code is far smaller.

### 2.5 Other existing precedents to reuse

- **Stateless form JSON already works:** the designer's "VariantsForm" path calls `createFormCache` from an **inlined JSON template** with no `getData` round-trip (`servoydesigner.component.ts` line 34, 54–57).
- **Headless form-state generation already works:** `MobileExporter` loops all forms and runs `new AngularFormGenerator(flattenedSolution, form, name, false, null).generateJS()` with no editor and no client (`MobileExporter.java` ~743–755). The designer's editor-bound equivalent is `DesignerWebsocketSession.executeMethod("getData",…)` (lines 202–253), which also assembles the per-component specs (`compSpecsToSend`, lines 208–219) — this is exactly the specs blob we need to emit into the HTML alongside the form-state JSON.
- **HTML injection already works:** served HTML is enhanced by `org.sablo.IndexPageEnhancer.enhance(…)` with a `variableSubstitution` map and extra scripts/css (`EditorContentFilter.java` lines 106–144). This is the mechanism to inject our form-state + specs JSON blobs into the page.
- **Solution stylesheet is produced design-time already:** the designer/solution CSS is assembled without a running client (`DesignerWebsocketSession.getSolutionStyleSheets`/design-css, lines 130–151, 344–372). We expose it via a dedicated stateless endpoint `/formtemplate/stylesheet.css` rather than inlining it.
- **Components are compiled into the bundle** (per-solution generated `allcomponents.module.ts` imported via `LFCModule`; `CUSTOM_ELEMENTS_SCHEMA` set). So rendering the tags needs no server code — only the specs (for conversion) and the form-state JSON.

## 3. Design

### 3.1 High-level shape

Two stateless HTTP endpoints plus a compiled-in simplified form component:

```
GET /formtemplate/<formname>.html
   → server: run AngularFormGenerator for <formname> in the active editing flattened solution (headless),
     collect the per-component client-side specs, inject BOTH into the HTML as inline
     <script type="application/json"> blobs, add a <link rel="stylesheet" href="stylesheet.css">
     and a form-name marker (via IndexPageEnhancer).
GET /formtemplate/stylesheet.css
   → server: emit the active solution's stylesheet (produced the same design-time way the
     solution CSS is normally produced), as plain CSS. No runtime/client involved.
   → browser: FormTemplate route reads the embedded form-state + specs, registers the specs
     into TypesRegistry ITSELF, createFormCache(formName, formState, null), and renders
     <svy-formtemplate> — a simplified copy of the form component wired with only the minimal
     services needed to render components without data.
```

No `clientnr`, no `solution`, no websocket, no session. Refresh = re-fetch.

### 3.2 Server side (Java) — two stateless endpoints

1. **`HeadlessFormTemplateContent`** (new; `com.servoy.eclipse.designer`, package `com.servoy.eclipse.designer.editor.rfb`). Given a form name, using the active project's editing flattened solution (`ServoyModelFinder.getServoyModel().getActiveProject()` → `getEditingFlattenedSolution()`), no open editor:
   - `String getFormDataJS(String formName)` — `fs.getForm(formName)` → `fs.getFlattenedForm(form)` → `new AngularFormGenerator(fs, flattenedForm, formName, true, null).generateJS(...)` (mirror `MobileExporter`; verify whether `Settings.TESTING_MODE` is needed).
   - `String getComponentSpecsJSON(String formName)` — the per-component client-side specs for the components the form uses, in the same shape `DesignerWebsocketSession` sends via `getTypesRegistryService().addComponentClientSideSpecs(...)` (extract that assembly, lines 208–219). Emitted **together with the form-state JSON** so the client can register exactly the specs that form needs. This is what fixes the empty-registry gap (2.3).
   - `String getSolutionStyleSheet()` — the active solution's CSS produced the design-time way the solution stylesheet is normally produced (factored from `DesignerWebsocketSession.getSolutionStyleSheets`/design-css, lines 130–151, 344–372, minus designer-only sheets). Served as its own resource, not inlined.
2. **`FormTemplateHttpEndpoint`** (new; `com.servoy.eclipse.designer.rfb`) — a servlet/filter mapped to `/formtemplate/*`:
   - **`/formtemplate/<formname>.html`** — load the compiled Angular `index.html` and enhance it via `org.sablo.IndexPageEnhancer.enhance(...)` (as `EditorContentFilter` does) to inject:
     - `<script id="svy-formtemplate-formstate" type="application/json">…getFormDataJS…</script>`
     - `<script id="svy-formtemplate-specs" type="application/json">…getComponentSpecsJSON…</script>`
     - `<link rel="stylesheet" href="stylesheet.css">` (relative → `/formtemplate/stylesheet.css`).
     - a marker (`variableSubstitution` entry `formtemplateName=<formname>`) so the route knows which form to render.
   - **`/formtemplate/stylesheet.css`** — return `getSolutionStyleSheet()` as `text/css`. Stateless; independent of any open runtime/client.
   Reuse the existing enhancer/filter pattern (`EditorContentFilter` / `ServicesProvider`). **Do not modify the designer path's behaviour** when extracting shared helpers.
3. **No websocket endpoint, no `WebsocketSession`, no `clientnr` handling** for this route.

### 3.3 Client side (Angular) — a simplified copy of the form component

Rather than reuse the full runtime `FormComponent` and swap out its many services, create a **simplified copy** at `node/src/formtemplate/`, mirroring how the designer keeps `designer/designform_component.component.ts`. The copy renders the same `.svy-form` DOM but injects only what is needed to render components without data.

`node/src/formtemplate/`:

1. **Route** `formtemplate/:formname` registered in `node/src/app/app-routing.module.ts` **before** the `**` catch-all, lazy-loading `FormTemplateModule`. (The `.html` is served by the Java endpoint; the route reads the form name from the injected `formtemplateName` marker, falling back to the path.)
2. **`formtemplate_component.component.ts`** (new; selector `svy-formtemplate`) — a **stripped copy of `form_component.component.ts`** carrying the identical template markup (same `.svy-form` / `.svy-wrapper` / `.svy-layoutcontainer` structure and the same `<!-- component template generate start/end -->` + `// component viewchild template generate start/end` marker regions so `WebPackagesListener` can fill them). It extends `AbstractFormComponent` and:
   - injects **only** the minimal services required to render (see below) — NOT `SabloService`, NOT `ServoyService`, NOT the resize/window plumbing;
   - `getHandler()` → `return null` (no event functions wired — clicks do nothing);
   - `callApi()` → `return null`;
   - no `resolveComponentCache`, no `onResize`/resize subscription, no `sendChanges`, no `formLoaded`/`formUnloaded`, no `datachange` push;
   - `getServoyApi(item)` → returns a minimal no-op `ServoyApi` (design-time only; e.g. `trustAsHtml` returns the value as-is) sufficient for components to render.
3. **Minimal services** (new, tiny, in `node/src/formtemplate/`, provided at `FormTemplateModule` scope):
   - A **minimal form-state provider** that exposes `getFormCache()` from a `FormCache` built once from the injected form-state JSON. The copy component reads its cache from here instead of the full `FormService`. Building the `FormCache` reuses the SAME conversion path the real `FormService.createFormCache`/`walkOverChildren` uses (it needs `ConverterService` + `TypesRegistry`), so either (a) reuse `FormService` solely for `createFormCache`/`getFormCache` (it is local-only for those), or (b) extract the `createFormCache`/`walkOverChildren` builder into a small shared helper the provider calls. Decide in implementation; reuse of the builder is required (no re-implementing conversion).
   - A **minimal ServoyApi/public stub** for whatever the rendered components pull from `ServoyApi`/`ServoyPublicService` (reuse the designer's `ServoyPublicServiceDesignerImpl` no-op stub if it fits).
   - Reuse the real `ConverterService`, `TypesRegistry`, `LoggerFactory`, `WindowRefService` (local-only), and `LFCModule`'s compiled component modules.
4. **Spec self-registration:** the route root reads `#svy-formtemplate-specs` and calls `typesRegistry.addComponentClientSideSpecs(<parsed specs>)` **before** building the form cache, so typed-property conversion works for exactly this form (fixes 2.3). Then it builds the cache from `#svy-formtemplate-formstate` and renders `<svy-formtemplate [name]="formName">`.
5. **`WebPackagesListener` change:** register the new copy alongside the runtime and designer files — read `src/formtemplate/formtemplate_component.component.ts`, run the same `replace(...)` marker substitutions (component + structure template + viewchild regions) as done for `content` and `editorContent` (lines 426–477), and write it back if changed. Keep the three files' marker regions identical so one generation pass fills all three.
6. **`FormTemplateModule`** — imports `LFCModule` + routing; declares the route root + `svy-formtemplate` copy; `providers` for the minimal services above; `CUSTOM_ELEMENTS_SCHEMA`.

### 3.4 Why this is safe / has no client dependency

- The copy component **never calls the server** — it has no `SabloService`/`WebsocketService` and no `FormService` methods that would (no `resolveComponentCache`, `sendChanges`, `executeEvent`, `formLoaded`/`formUnloaded`).
- The only genuine data dependency for building the cache is the `TypesRegistry` specs, which the route registers itself from the injected specs blob (2.3, 3.3.4).
- Component Angular classes are compiled into the bundle (2.5); `WebPackagesListener` fills the copy's template so all component tags resolve.
- Solution CSS comes from the dedicated `/formtemplate/stylesheet.css` endpoint (design-time produced), so styling matches without a runtime.

### 3.5 DOM contract

`.svy-form` root; CSS-position forms → `.svy-wrapper` `position:absolute` divs wrapping runtime component tags (e.g. `<servoydefault-textfield>`); responsive forms → nested `.svy-layoutcontainer` divs. No designer artifacts (`svy-id`, `designclass`, ghosts, wireframe, `svy-designform`). Models carry design-time values only. The copy's markup is kept identical to `svy-form` so the DOM matches production.

## 4. Implementation plan (ordered)

Server (Java):
1. Create `com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/HeadlessFormTemplateContent.java` (`getFormDataJS`, `getComponentSpecsJSON`, `getSolutionStyleSheet`) using the active editing flattened solution, no editor. Extract the spec-assembly and stylesheet logic from `DesignerWebsocketSession` into shared helpers; leave the designer path behaviourally unchanged.
2. Create `com.servoy.eclipse.designer.rfb/.../FormTemplateHttpEndpoint.java` (servlet/filter for `/formtemplate/*`): `/(formname).html` → enhanced Angular `index.html` (form-state + specs inline blobs, `<link>` to stylesheet.css, form-name marker via `IndexPageEnhancer`); `/stylesheet.css` → `getSolutionStyleSheet()` as `text/css`. Register alongside the existing designer content serving (`ServicesProvider` / `EditorContentFilter` pattern).

Angular templates generation:
3. Modify `com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/WebPackagesListener.java` — read `src/formtemplate/formtemplate_component.component.ts`, run the same component/structure template + viewchild marker `replace(...)` substitutions applied to `content`/`editorContent` (around lines 426–477), and write it back when changed.

Client (Angular, `com.servoy.eclipse.ngclient.ui/node`):
4. `node/src/formtemplate/formtemplate-routing.module.ts` — `RouterModule.forChild([{ path: '', component: <route root> }])`.
5. `node/src/formtemplate/formtemplate_component.component.ts` (`svy-formtemplate`) — stripped copy of `form_component.component.ts` with identical template + generation markers; minimal injected services; `getHandler`→null, `callApi`→null, no resolve/resize/sendChanges/formLoaded/formUnloaded; minimal `getServoyApi`.
6. `node/src/formtemplate/*` minimal services — a minimal form-state provider (builds `FormCache` from the injected JSON, reusing the existing `createFormCache`/`walkOverChildren` builder) and a minimal ServoyApi/public stub (reuse `ServoyPublicServiceDesignerImpl` if it fits).
7. Route root component — reads `#svy-formtemplate-specs` → `typesRegistry.addComponentClientSideSpecs(...)`, then `#svy-formtemplate-formstate` → build cache → render `<svy-formtemplate [name]="formName">`.
8. `node/src/formtemplate/formtemplate.module.ts` — imports `LFCModule` + routing; declares route root + `svy-formtemplate`; `providers` for the minimal services; `CUSTOM_ELEMENTS_SCHEMA`.
9. Modify `node/src/app/app-routing.module.ts` — add the `formtemplate` route before `**`.

Tests:
10. Angular specs (Section 5).
11. Java test in `com.servoy.eclipse.tests` for `HeadlessFormTemplateContent`.

## 5. Acceptance criteria

- [ ] `GET /formtemplate/<formname>.html` returns an HTML page with the form-state JSON and the component specs JSON embedded inline plus a `<link>` to `stylesheet.css` — no `clientnr`/`solution` in the URL, and no websocket is opened by the page.
- [ ] `GET /formtemplate/stylesheet.css` returns the active solution's CSS as `text/css`, produced design-time (no running client), independent of the `.html` request.
- [ ] The page renders a `.svy-form` root with the form's runtime component tags: CSS-position → `.svy-wrapper` `position:absolute` wrappers; responsive → nested `.svy-layoutcontainer`.
- [ ] Rendered DOM has NO designer-only artifacts (`svy-id`, `designclass`, wireframe/ghost/drag markup, `svy-designform`) and matches the runtime `svy-form` markup.
- [ ] Typed component properties convert correctly (the route registers the injected specs into `TypesRegistry` before building the cache) — assert a form with a typed property (e.g. a date/valuelist) renders without conversion errors.
- [ ] No server communication occurs after load: no websocket connection; the copy component injects no `SabloService`/`WebsocketService`; `getHandler` returns `null`; `callApi` returns `null`; no `resolveComponentCache`/`sendChanges`/`formLoaded`/`formUnloaded`.
- [ ] Button/event handlers do nothing (no event functions wired).
- [ ] `HeadlessFormTemplateContent.getFormDataJS(form)` returns valid `AngularFormGenerator.generateJS()`-shaped JSON for a form in the active flattened solution with NO open editor (Java test, headless like `MobileExporter`); `getComponentSpecsJSON`/`getSolutionStyleSheet` return the expected specs/CSS with no editor.
- [ ] Existing designer route unchanged after the server-side helper extraction (designer still renders; ghosts/decorators work).
- [ ] `WebPackagesListener` fills the copy's component/structure template + viewchild marker regions (like it does for the runtime and designer files) so all component tags resolve; the copy's markup stays in sync with `svy-form`.
- [ ] `npx tsc --noEmit -p src/tsconfig.app.json`, `npx ng lint`, and Karma via `com.servoy.eclipse.ngclient.ui/node/run_tests.bat` pass, incl. new specs.
- [ ] Java compiles clean; no new blocking spotbugs (two highest severities) in new/modified code.

## 6. Out of scope

- Embedding a browser in the Servoy AI view + AI DOM access/click/screenshot (explicit follow-up case). This case ends when `/formtemplate/<form>.html` renders the real DOM of any form (no data/no client/no websocket).
- Any MCP endpoint wiring on top of the route (the ticket's original Java-only projection endpoint).
- Live data, foundsets, running server-side scripting, working event handlers.
- Multi-solution / cross-solution disambiguation by URL (always the active editing flattened solution; form names are unique within it).
- Live update of a rendered form (change the form → refresh the browser).
- Persist/client-runtime behaviour changes; the runtime `FormComponent` is left untouched — the render path is a separate simplified copy (like the designer's), not a modification of `form_component.component.ts`.

## 7. Open questions

| # | Question | Suggested default |
|---|---|---|
| 1 | Reuse `FormService.createFormCache`/`walkOverChildren` builder from the minimal provider, or extract it into a shared helper? | Reuse the existing builder (it is local-only for cache building); extract a helper only if injecting `FormService` drags in unwanted deps. Do not re-implement conversion. |
| 2 | How much of `form_component.component.ts` can be stripped in the copy while keeping identical DOM? (getServoyApi/servoyApiCache, handlerCache, containers/cssstyles setters, etc.) | Keep everything that shapes DOM/models; drop only client-communication (`sendChanges`, `executeEvent`, `resolveComponentCache`, resize, `formLoaded`/`formUnloaded`). Verify components still render. |
| 3 | Exact shape/source of the component client-side specs blob to inject (must match what `TypesRegistry.addComponentClientSideSpecs` expects, i.e. what `DesignerWebsocketSession` sends). | Extract the same assembly `DesignerWebsocketSession` uses (lines 208–219) into a shared helper and emit it as JSON alongside the form-state. |
| 4 | Is `Settings.TESTING_MODE=true` (as `MobileExporter` sets) required for headless `generateJS`? | Match `MobileExporter` unless the designer path proves it unnecessary; confirm during implementation. |
| 5 | `getSolutionStyleSheet()` — reuse the exact design-time solution-CSS assembly the designer/solution uses (from LESS), minus designer-only sheets. | Factor the solution-CSS part out of `DesignerWebsocketSession.getSolutionStyleSheets`; exclude designer-specific sheets; serve as `text/css`. |
| 6 | i18n values in the model (designer uses `SHOW_I18N_VALUES_IN_ANGULAR_DESIGNER`). What should the stateless generator pass? | Pass `null` messages manager (raw keys) for a stable, data-free projection unless resolved i18n is explicitly wanted. |
| 7 | How does the Angular route learn the form name — from the injected `formtemplateName` marker or by parsing the `.html` path? | Inject a marker (`formtemplateName`) via the enhancer; fall back to parsing the path. |
| 8 | A minimal no-op `ServoyApi` may be enough, or do some components need real `ServoyApi` behaviour to render (e.g. `trustAsHtml`, formcomponent APIs)? | Start with a no-op/design-time `ServoyApi` (reuse designer's `FormComponentDesignServoyApi` pattern); expand only if a component fails to render. |
