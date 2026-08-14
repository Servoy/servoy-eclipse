# Triage Report — SVY-19023

**Verdict:** PROCEED

## Reported problem

Third-party Angular libraries (e.g. ng2-charts v6.0) have dropped their NgModule and now ship only standalone components. The Servoy TiNG runtime cannot consume them because its entire dynamic code-generation pipeline (`WebPackagesListener.java` → `allcomponents.module.ts`) is built around importing NgModules. More broadly, the 15 internal NgModules and 26 `standalone: false` declarations in the TiNG Angular workspace need to be migrated to standalone to stay current with Angular 22+ best practices and to unblock the eventual zoneless migration.

## Root-cause assessment

The problem is structural, not a bug. The dynamic code generation architecture assumes every external component package exposes a single NgModule:

1. **MANIFEST.MF** in each package declares `NG2-Module: <ModuleClassName>` and `NPM-PackageName: <pkg>`.
2. **`WebPackagesListener.java:490-511`** reads these and generates `allcomponents.module.ts` — a single NgModule that imports/exports all package modules.
3. **`LFCModule`** → imports `AllComponentsModule` → makes everything available to the template hierarchy.
4. **`ComponentTemplateGenerator.java`** generates per-component `<ng-template>` entries and `viewChild` references directly into `form_component.component.ts`.

When a library ships *only* standalone components (no NgModule), the current pipeline has no way to import them — there's no `NG2-Module` to reference.

Additionally, the `standalone` branch (commits `0f5a3c1f9d`, `f0ecb1968a`, `ee5a4fe1df`) reveals a secondary issue: when `FormComponent` is made `standalone: true`, Angular's strict template type checking (which is more aggressive for standalone components than for NgModule-declared components with `CUSTOM_ELEMENTS_SCHEMA`) rejects bindings for properties defined in `.spec` files that don't have corresponding `@Input()` decorators on the Angular component class.

## Ticket premise check

The ticket's premise is correct: standalone components need a different integration path. Johan's comment ("the modules are gone, so who is going to include all the components — I guess the form_component should do this") is precisely the right insight. The `standalone` branch already demonstrates the viable approach (FormComponent imports modules/components directly).

The secondary issue noted in the branch ("a lot of problems in properties that are not implemented in TiNG") was partially solved in `ee5a4fe1df` by using `PropertyDescription.isInternal()` to skip properties without `@Input()`. This needs to be extended to fully cover all property/binding mismatches.

## Approaches considered

### 1. Phased standalone migration with backwards-compatible NgModule bridge (RECOMMENDED)

**Phase A — Internal standalone + keep NgModule imports:**
- Make all 26 internal components/directives/pipes `standalone: true`
- `FormComponent` becomes `standalone: true` with `imports: [AllComponentsModule, ServoyCoreComponentsModule, ...]`
- External packages keep their NgModules unchanged — Angular allows standalone components to import NgModules
- `WebPackagesListener` still generates `allcomponents.module.ts` (now consumed by FormComponent's imports instead of via module hierarchy)
- Fix `ComponentTemplateGenerator` to only emit bindings for properties that have `@Input()` (extend the `isInternal()` fix)

**Phase B — External packages migrate to standalone:**
- Each package makes its components `standalone: true`
- Packages export both: a backwards-compatible NgModule (re-exporting standalone components) AND a component array constant (e.g. `BOOTSTRAP_COMPONENTS`)
- Add new MANIFEST attribute: `NG2-Components: ComponentA,ComponentB,...` (comma-separated list of exported standalone component class names)

**Phase C — Code generator update:**
- `WebPackagesListener` detects the new `NG2-Components` attribute
- When present, generates direct component imports instead of module import
- When absent (backwards compat), falls back to `NG2-Module` import
- `allcomponents.module.ts` is eventually replaced by a generated `allcomponents.ts` exporting a flat array

**Pros:** Incremental, backwards-compatible at each step, no big-bang migration needed. External packages can migrate at their own pace.
**Cons:** Longer timeline, temporary code supporting both patterns.

### 2. Big-bang migration — all packages go standalone simultaneously

Convert everything at once: internal TiNG modules, all external packages, and the code generator in one coordinated release.

**Pros:** Clean cut, no transitional code.
**Cons:** Extremely high risk, requires coordinating all external package repos (bootstrapcomponents, servoy-extra-components, aggridcomponents, etc.) simultaneously. All-or-nothing delivery.

### 3. Keep NgModules, only add standalone consumption support

Keep all Servoy packages as NgModules but add a mechanism to consume third-party standalone components. Add a new MANIFEST attribute (e.g. `NG2-StandaloneComponents`) that lists individual component class names to import directly.

**Pros:** Minimal disruption to existing packages. Unblocks the ng2-charts use case immediately.
**Cons:** Doesn't address the long-term technical debt of 15 NgModules. Doesn't prepare for zoneless (which requires standalone). Kicks the can down the road.

### 4. No code change

**Pros:** Zero effort.
**Cons:** Cannot consume any modern library that has dropped its NgModule. Blocks future Angular modernization (zoneless requires standalone). The Angular team has signaled that NgModules are in maintenance mode. Accumulating technical debt.

## Recommendation

**Approach 1 (phased migration)** is recommended. It aligns with the existing modernization roadmap (Phase 6 in `angular22-modernization-remaining.spec.md`), builds on the existing `standalone` branch experiment, and allows incremental delivery without breaking external packages.

The key technical insight that makes this tractable: **standalone components can import NgModules**. This means Phase A requires zero changes to external packages — they keep exporting their NgModule, and it just gets imported into a standalone `FormComponent` instead of into another NgModule. The migration of external packages (Phase B) can happen gradually and independently.

The critical path is:
1. Fix `ComponentTemplateGenerator` to not emit bindings for properties without `@Input()` (extends `ee5a4fe1df`)
2. Make `FormComponent` standalone, importing `AllComponentsModule` (the generated NgModule that aggregates external packages)
3. Remove internal NgModules one by one (leaf → container → routing → bootstrap)
4. Later: update MANIFEST/code-gen to support direct standalone component imports from packages

## Git history findings

- **Branch `standalone`** (3 commits by Johan, 2025-05-23): Experimental work making `FormComponent` `standalone: true`. Identified two issues: (a) template bindings for non-`@Input()` properties, (b) some servoycore components that shouldn't be generated.
- **Commit `ee5a4fe1df`**: Uses `pd.isInternal()` to skip properties without `@Input()` in the template generator. Also hardcodes skipping `servoycore-formcomponent`, `servoycore-navigator`, `servoycore-portal`.
- **Commit `0f5a3c1f9d`**: Made FormComponent `standalone: true` with `imports: [CommonModule, FormsModule, ServoyCoreComponentsModule, LFCModule]`. This is the proof-of-concept showing the approach works structurally.
- **`WebPackagesListener.java` last changed**: `2cc9c97628` (allservices inject() pattern), unrelated to standalone.
- **MANIFEST.MF pattern** (bootstrapcomponents): `NG2-Module: ServoyBootstrapComponentsModule`, `NPM-PackageName: @servoy/bootstrapcomponents`, `Entry-Point: dist/servoy/bootstrapcomponents` — this is the contract that needs extending (not replacing) in Phase B.
