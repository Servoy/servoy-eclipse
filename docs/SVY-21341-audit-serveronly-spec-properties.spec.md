# Spec: SVY-21341 — Audit serveronly spec properties (verify usage or remove)

## 1. Goal

As part of the standalone-component migration (SVY-19023) a `"serveronly": true`
tag was added to component spec properties that had no corresponding `@Input` in the
migrated Angular component; those properties are excluded from template generation and
websocket communication. This effort audits every property listed in SVY-21341 across
three independent component repositories, categorizes each with evidence (keep / restore
binding / make two-way / remove), and applies the small set of concrete code changes that
the audit justifies. The bulk of the deliverable is the per-property categorization; the
code work is four targeted changes: remove one genuinely-dead spec entry
(`hashedColumns`), restore one lost migration binding (`containerStyleClass`), and convert
five `errorShow` properties to proper two-way bound model properties.

## 2. Background

### 2.1 What `serveronly` means

`"serveronly": true` marks a spec property that exists on the server-side model but is not
sent to the browser as a bound attribute and is skipped in generated Angular templates.
It is correct for:

- properties consumed only by platform server logic or the component `_server.js`;
- properties that are *configuration targets* referenced by another property's `config`,
  `displayTagsPropertyName`, `for`, etc. (the platform reads them, not the template);
- legacy AngularJS components that read `$scope.model.<prop>` directly rather than through
  an `@Input`.

It is **incorrect** (a bug) when a property was genuinely client-facing and its binding
was dropped during migration, or when the property is truly unused dead code.

### 2.2 Three independent repositories

The audited properties live in three separate git repositories, each with its own release
cycle:

- `bootstrapcomponents` (`C:\Users\vosti\git_master\bootstrapcomponents`)
- `aggridcomponents` (`C:\Users\vosti\git_master\aggridcomponents`)
- `servoy-extra-components` (`C:\Users\vosti\git_master\servoy-extra-components`)

Work is tracked under the single case SVY-21341 but delivered as **separate commits/PRs
per repository**.

### 2.3 `servoy-extra-components` are still legacy AngularJS

Every listed `servoy-extra` component still points its spec `definition` at a plain `.js`
file (e.g. `servoyextra/slider/slider.js`, `servoyextra/table/table.js`,
`servoyextra/dbtreeview/dbtreeview.js`). They read properties from `$scope.model`, so the
"no `@Input`" heuristic does not apply. The `serveronly` tag here just means "not pushed
to the browser as a bound attribute," which is correct.

### 2.4 `errorShow` current implementation (5 floatlabel components)

In the migrated Angular floatlabel components `errorShow` is declared as an internal
`signal` plus an `output` (`errorShowChange`), with the spec property tagged
`serveronly` + `pushToServer: "allow"`
(`projects/bootstrapcomponents/src/floatlabelcalendar/floatlabelcalendar.ts:18-19`,
`floatlabelcalendar.spec:32`). The value flows client → server (via the output) but the
server value does not flow back to the client. SVY-21341 asks these to be made
two-way bindable so a server-set `errorShow` reaches the client as well.

## 3. Design

### 3.1 servoy-extra-components — deep re-verification (audit only, no removals)

Each listed property was re-verified individually against the component's `.spec`, legacy
`.js`, `_doc.js`, and `.html`. Reference counts below exclude the spec declaration itself.

| Component | Property | Evidence (file:line / mechanism) | Category |
|-----------|----------|----------------------------------|----------|
| dbtreeview | `roots` | 38 non-spec refs in `dbtreeview.js`/html | keep (server-only, live) |
| htmlarea | `displaysTags` | config target of `displayTagsPropertyName` on `dataProviderID`/`text` (`htmlarea.spec:55,64`); platform tag-display mechanism reads it | keep (framework config target) |
| htmlarea | `findmode` | `type:"findmode"`, platform find-mode property (`htmlarea.spec:59`) | keep (platform find-mode) |
| spinner | `displaysTags` | config target of `displayTagsPropertyName` on 4 props (`spinner.spec:13,19,23,24`) | keep (framework config target) |
| spinner | `findmode` | `type:"findmode"` (`spinner.spec:17`) | keep (platform find-mode) |
| spinner | `readOnly` | 2 non-spec refs in `spinner.js` | keep (server-only, live) |
| spinner | `text` | 8 non-spec refs; also a `displayTagsPropertyName` target | keep (server-only, live) |
| select2tokenizer | `valuelistConfig` | `config` target of `valuelistID` (`select2tokenizer.spec:41`), `type:"valuelistConfig"` | keep (valuelist config target) |
| select2tokenizer | `searchingText` | 2 non-spec refs in `select2tokenizer.js` | keep (server-only, live) |
| select2tokenizer | `valueSeparator` | 9 non-spec refs in `select2tokenizer.js` | keep (server-only, live) |
| slider | `readOnly` | 4 non-spec refs in `slider.js` | keep (server-only, live) |
| slider | `ticksArray` | 2 non-spec refs in `slider.js` | keep (server-only, live) |
| slider | `stepsArray` | 7 non-spec refs in `slider.js` | keep (server-only, live) |
| splitpane | `panes` | 23 non-spec refs in `splitpane.js` | keep (server-only, live) |
| table | `sortColumnIndex` | 12 non-spec refs in `table.js` | keep (server-only, live) |

**Result:** no removals and no binding changes in `servoy-extra-components`. Every listed
property is either live in the legacy `.js` or a platform/framework config target. The
outcome for this repo is documentation only (this table). No code change / no PR needed
for servoy-extra unless the audit table is captured somewhere the team wants it (e.g. a
ticket comment).

> Note: `displaysTags` / `findmode` matches in the legacy `.js` were `$scope.findMode`
> (different casing) — a false positive; the real consumers are the platform mechanisms
> referenced above, which is why these stay `serveronly`.

### 3.2 bootstrapcomponents — `errorShow` → two-way bindable (5 floatlabel components)

Affected components (spec + ts + html):

- `floatlabelcalendar`
- `floatlabelcombobox`
- `floatlabeltextarea`
- `floatlabeltextbox`
- `floatlabeltypeahead`

Current: `errorShow` is `serveronly` + `pushToServer:"allow"` in the spec, and in the `.ts`
it is an internal `signal<boolean|undefined>` plus an `output<boolean>` `errorShowChange`.

Target: make `errorShow` a proper two-way (model) bindable property so the server value
flows to the client and client changes flow back.

Per component:

1. **Spec** — remove the `"serveronly": true` tag from the `errorShow` property; keep
   `pushToServer:"allow"` (the mechanism that allows client→server writes on a two-way
   bound property). Example, `floatlabelcalendar.spec:32`:
   - from `"errorShow" : {"type":"boolean","pushToServer":"allow","tags":{"serveronly":true,"scope":"private"}}`
   - to `"errorShow" : {"type":"boolean","pushToServer":"allow","tags":{"scope":"private"}}`
2. **Component `.ts`** — replace the internal `signal` + `output` pair with a two-way
   bound property. Use Angular `model<boolean>()` for `errorShow` (which pairs an
   `input` with an auto-generated `errorShowChange` output), and update the internal
   writes:
   - remove `readonly errorShow = signal<boolean|undefined>(undefined)` and
     `readonly errorShowChange = output<boolean>()`;
   - add `readonly errorShow = model<boolean>()` (import `model` from `@angular/core`,
     drop the now-unused `signal`/`output` imports if no longer referenced);
   - `this.errorShow.set(true)` still works (model exposes `.set`); the explicit
     `this.errorShowChange.emit(...)` calls become `this.errorShow.set(true/false)` so
     the two-way binding propagates to the server automatically. The designer branch that
     currently does `this.errorShow.set(true)` is unchanged.
3. **Template `.html`** — `@if (errorShow())` reads stay identical (`model` is callable
   like a signal), so no template change is required beyond confirming the getter call
   still compiles.

The Servoy two-way binding convention (`<prop>Change` output name derived from the input)
is satisfied by `model()`, which is why converting to `model` is the minimal correct
change rather than hand-wiring `input()` + a separate output.

### 3.3 aggridcomponents — remove `hashedColumns` from `groupingtable`

`hashedColumns` (`groupingtable.spec:45`,
`"hashedColumns": {"type":"string[]","default":[],"tags":{"serveronly":true,"scope":"private"}}`)
has **zero references** anywhere in the repo (spec-only) and was introduced by commit
`7ec67ff5` ("add designsize property to grid component specs [ai]", 2026-08-18) without a
consumer. It is genuinely dead. Remove the entire line from the spec.

`_internalVisible` (3 refs in `groupingtable_server.js`) and `keepAppliedFilterOnHide`
(used in `groupingtable_server.js`) are **kept** — they are live server-side logic. Only
`hashedColumns` is removed.

### 3.4 bootstrapcomponents — restore `containerStyleClass` binding on accordion

`containerStyleClass` (`accordion.spec:14`, `type:"styleclass"`, tagged `serveronly`) is a
**migration regression**, not dead code. The legacy AngularJS template bound it
(`accordion/accordionpanel.html:8` `ng-class='model.containerStyleClass'`), but the
migrated Angular template
(`projects/bootstrapcomponents/src/accordion/accordion.html`) never binds it. Sample
solutions still set it. Restore the client binding:

1. **Spec** — remove `"serveronly": true` from the `containerStyleClass` tags at
   `accordion.spec:14` so the property is sent to the client.
2. **Component `.ts`** (`accordion.ts`) — the base `ServoyBootstrapBaseTabPanel` does not
   currently declare `containerStyleClass`. Add an input for it:
   `readonly containerStyleClass = input<string>();`
   (import `input` from `@angular/core`).
3. **Template `.html`** (`accordion.html`) — apply the class to the body wrapper that
   holds the contained form (the `ngbAccordionBody` div at
   `accordion.html:10`), e.g. add `[ngClass]="containerStyleClass()"` (import/enable
   `NgClass`) or bind via `[class]`. This mirrors the legacy `ng-class='model.containerStyleClass'`
   which was applied to the form-include wrapper.

Do **not** remove the property.

### 3.5 valuelistConfig (typeahead / floatlabeltypeahead, bootstrap) — keep

`valuelistConfig` is the `config` object referenced by the `valuelistID` property
(`typeahead.spec` `"config": "valuelistConfig"`) and consumed by the platform valuelist
mechanism, not a plain `@Input`. Keep as-is. No change.

## 4. Implementation plan

### bootstrapcomponents (commit/PR #1)

1. `components/accordion/accordion.spec` — remove `"serveronly": true` from
   `containerStyleClass` (line 14).
2. `components/projects/bootstrapcomponents/src/accordion/accordion.ts` — add
   `readonly containerStyleClass = input<string>();` (import `input`).
3. `components/projects/bootstrapcomponents/src/accordion/accordion.html` — bind the
   contained-form wrapper with `[ngClass]="containerStyleClass()"` (enable `NgClass`),
   restoring the legacy behaviour.
4. For each of the 5 floatlabel components
   (`floatlabelcalendar`, `floatlabelcombobox`, `floatlabeltextarea`,
   `floatlabeltextbox`, `floatlabeltypeahead`):
   - `.spec` — remove `"serveronly": true` from `errorShow` (keep `pushToServer:"allow"`).
   - `.ts` — replace `errorShow` `signal` + `errorShowChange` `output` with
     `readonly errorShow = model<boolean>()`; update `.emit(...)` calls to `.set(...)`;
     fix imports.
   - `.html` — confirm `@if (errorShow())` still compiles (no change expected).
5. Run lint + build; run component tests. Zero lint warnings before commit.

### aggridcomponents (commit/PR #2)

6. `aggrid/groupingtable/groupingtable.spec` — delete the `hashedColumns` line (45).
   Verify no other file references it (confirmed zero). Keep `_internalVisible` and
   `keepAppliedFilterOnHide`.
7. Run lint + build; run tests.

### servoy-extra-components (no code change)

8. No spec/binding changes. Capture the §3.1 audit table as the deliverable (e.g. a
   SVY-21341 comment) confirming every listed property is keep-as-is.

## 5. Acceptance criteria

- [ ] Every property in the ticket is categorized (keep / restore binding / make two-way /
      remove) with evidence — see §3.1–§3.5.
- [ ] `hashedColumns` is removed from `groupingtable.spec`; `_internalVisible` and
      `keepAppliedFilterOnHide` remain.
- [ ] `containerStyleClass` binding is restored in the migrated accordion (spec no longer
      `serveronly`; `.ts` input added; `.html` binds the contained-form wrapper) and a
      form set with `containerStyleClass` renders the class in NG client.
- [ ] `errorShow` on all 5 floatlabel components is a two-way bound model property: a
      server-set value reaches the client, and a client change is pushed back; the
      `serveronly` tag is removed and `pushToServer:"allow"` retained.
- [ ] No `servoy-extra-components` property is removed or rebound; audit table documents
      keep-as-is.
- [ ] All three repos build and pass lint with zero warnings; existing component tests
      pass.

## 6. Out of scope

- Any change to `servoy-extra-components` spec files or bindings (all keep-as-is).
- Removing or changing `valuelistConfig` (typeahead / floatlabeltypeahead / select2tokenizer).
- Removing `_internalVisible` or `keepAppliedFilterOnHide` from groupingtable.
- Migrating any legacy AngularJS `servoy-extra` component to Angular.
- Any broader `serveronly` audit beyond the properties explicitly listed in SVY-21341.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| For accordion, should `containerStyleClass` be applied to the `ngbAccordionBody` wrapper (matching legacy) or the outer item — confirm the exact target element parity with the old `ng-include` wrapper. | reporter/UI | open |
| Where should the servoy-extra keep-as-is audit table be recorded (ticket comment vs this spec only)? | reporter | open |
| Confirm `pushToServer:"allow"` is the correct level for the now two-way `errorShow` (vs `shallow`) given it is a simple boolean. | reviewer | open |
