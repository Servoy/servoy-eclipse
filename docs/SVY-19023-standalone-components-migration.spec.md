# Spec: SVY-19023 — Standalone Components Migration

## 1. Goal

Migrate the TiNG Angular runtime from NgModule-based component registration to standalone components, enabling consumption of modern Angular libraries that no longer ship NgModules (e.g. ng2-charts v6). The migration is phased: internal components go standalone first (keeping external NgModule imports working), then external packages migrate, and finally the code generator is updated to support direct standalone component imports.

## 2. Background

### 2.1 Current Architecture

The TiNG runtime uses a dynamically generated module hierarchy to make components available to form templates:

```
AppModule
  └─ AppRoutingModule → ServoyModule (lazy-loaded)
       └─ LFCModule
            ├─ AllComponentsModule  (generated — imports all external package NgModules)
            ├─ AllServicesModules   (generated — imports all service modules)
            ├─ ServoyPublicModule
            ├─ ServoyCoreComponentsModule
            └─ [ListFormComponent, RowRenderer]
```

`FormComponent` is declared in `ServoyModule` with `standalone: false`. It gets access to all component selectors through the module hierarchy above.

### 2.2 Code Generation Pipeline

1. **MANIFEST.MF** in each external package declares:
   - `NG2-Module: <ModuleClassName>` (e.g. `ServoyBootstrapComponentsModule`)
   - `NPM-PackageName: <pkg>` (e.g. `@servoy/bootstrapcomponents`)
   - `Entry-Point: <path>` (e.g. `dist/servoy/bootstrapcomponents`)

2. **`PackageSpecification.java`** (`sablo` project, line 38-48) reads these MANIFEST attributes and exposes them via `getNg2Module()`, `getNpmPackageName()`, `getEntryPoint()`.

3. **`WebPackagesListener.java:490-511`** iterates all `componentPackageSpecToReader` entries and generates `allcomponents.module.ts`:
   ```typescript
   import { ServoyBootstrapComponentsModule } from '@servoy/bootstrapcomponents';
   @NgModule({ imports: [...], exports: [...] })
   export class AllComponentsModule { }
   ```

4. **`ComponentTemplateGenerator.java:113-228`** generates per-component `<ng-template>` entries and `viewChild` signal queries into `form_component.component.ts`. It iterates all properties from each component's `.spec` file and emits `[propertyName]="state.model.propertyName"` bindings.

### 2.3 The Problem

When a library ships **only** standalone components (no NgModule), the pipeline has no way to import them — there's no `NG2-Module` to reference. Additionally, Angular's strict template type checking for standalone components rejects bindings for properties that exist in `.spec` files but don't have corresponding `@Input()` decorators on the Angular component class.

### 2.4 Prior Work (standalone branch)

The `standalone` branch (commits `0f5a3c1f9d`, `f0ecb1968a`, `ee5a4fe1df`) demonstrated:
- `FormComponent` can be made `standalone: true` with `imports: [ServoyModule, CommonModule, FormsModule, ServoyCoreComponentsModule, LFCModule]`
- Template bindings for non-`@Input()` properties must be skipped — solved by using `pd.isInternal()` check in `ComponentTemplateGenerator`
- Some servoycore components (`servoycore-formcomponent`, `servoycore-navigator`, `servoycore-portal`) must not be generated as templates (they are used structurally, not via `<ng-template>`)

### 2.5 Key Insight

Angular standalone components **can import NgModules**. This means Phase A requires zero changes to external packages — they keep exporting their NgModule, and it gets imported into a standalone `FormComponent` instead of into another NgModule.

## 3. Design

### 3.1 Phase A — Internal Standalone + NgModule Bridge

**Goal:** Make all 26 internal components/directives/pipes and `FormComponent` standalone while keeping `AllComponentsModule` as the bridge to external packages.

#### 3.1.1 Convert Internal Declarations to Standalone

All components currently in `ServoyCoreComponentsModule`, `LFCModule`, `ServoyModule`, and the library modules (`ServoyPublicModule`, `ServoyDefaultComponentsModule`, `DialogModule`, `WindowServiceModule`) become `standalone: true` with their own `imports` array.

Example transformation:
```typescript
// Before
@Component({ standalone: false, ... })
export class DefaultNavigator { ... }

// After
@Component({ standalone: true, imports: [CommonModule, ServoyPublicModule], ... })
export class DefaultNavigator { ... }
```

#### 3.1.2 Make FormComponent Standalone

`FormComponent` becomes `standalone: true` with:
```typescript
@Component({
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        AllComponentsModule,       // generated NgModule — bridge to external packages
        AllServicesModules,        // generated services module
        ServoyCoreComponentsModule, // can remain as NgModule re-exporting standalone components
        ServoyPublicModule,
        // all servoycore standalone components needed in its template
        ListFormComponent,
        ...
    ],
    ...
})
export class FormComponent { ... }
```

#### 3.1.3 Fix ComponentTemplateGenerator

Extend the `isInternal()` fix from commit `ee5a4fe1df`:
- Only emit `[propertyName]` bindings for properties that are NOT marked as `internal` (already done in standalone branch)
- Skip components that should not be generated: `servoycore-formcomponent`, `servoycore-navigator`, `servoycore-portal` (already prototyped)
- Generalize the skip list using a property on `WebObjectSpecification` rather than hardcoding names

#### 3.1.4 Remove Intermediate NgModules

After all declarations are standalone:
- `ServoyCoreComponentsModule` → either keep as a convenience re-export NgModule or remove and import components directly
- `LFCModule` → dissolve; its declarations (`ListFormComponent`, `RowRenderer`) become standalone
- `ServoyModule` → dissolve; `FormComponent` is standalone, services move to `provideRouter()` providers
- `MainRoutingModule` → convert to `provideRouter(routes)` with standalone route config
- `AppRoutingModule` → convert to `provideRouter(routes)` with lazy loading
- `AppModule` → replace with `bootstrapApplication()` in `main.ts`

#### 3.1.5 AllComponentsModule Stays (Temporarily)

`AllComponentsModule` continues to be generated by `WebPackagesListener.java` and imported by `FormComponent`. External packages are unaffected. The generated file now only needs `imports` (not `exports`) since `FormComponent` imports it directly.

### 3.2 Phase B — External Packages Migrate to Standalone

**Goal:** External component packages (bootstrapcomponents, servoy-extra-components, aggridcomponents, etc.) make their components standalone while maintaining backwards compatibility.

#### 3.2.1 Package Migration Pattern

Each package:
1. Makes all its components `standalone: true` with their own `imports`
2. Keeps the NgModule as a backwards-compatible re-export wrapper:
   ```typescript
   @NgModule({ imports: [...allStandaloneComponents], exports: [...allStandaloneComponents] })
   export class ServoyBootstrapComponentsModule { ... }
   ```
3. Additionally exports a flat array constant:
   ```typescript
   export const BOOTSTRAP_COMPONENTS = [
       ServoyBootstrapButton,
       ServoyBootstrapLabel,
       ...
   ] as const;
   ```

#### 3.2.2 New MANIFEST Attribute

Add a new optional attribute to MANIFEST.MF:
```
NG2-Components: ServoyBootstrapButton,ServoyBootstrapLabel,...
```

This is a comma-separated list of standalone component/directive/pipe class names exported from the package's entry point. When present, the code generator can import them directly instead of via the NgModule.

The existing `NG2-Module` attribute is kept for backwards compatibility — packages that haven't migrated continue to work unchanged.

#### 3.2.3 Constructor Side Effects

Some NgModule constructors perform side effects (e.g. `ServoyBootstrapComponentsModule` registers custom types via `SpecTypesService`, `ServoyCoreComponentsModule` sets up AG Grid license). These must be moved to:
- An `APP_INITIALIZER` provider, or
- An `inject()` call in the component that needs it, or
- A standalone `provideXxx()` function exported by the package

The code generator must detect when a package has side effects and generate the appropriate provider registration.

### 3.3 Phase C — Code Generator Update

**Goal:** `WebPackagesListener.java` generates direct standalone component imports when the package supports it.

#### 3.3.1 PackageSpecification Extension

Add to `PackageSpecification.java`:
```java
private static final String NG2_COMPONENTS = "NG2-Components";
private final String ng2Components; // comma-separated component class names

public String getNg2Components() { return ng2Components; }
public boolean hasStandaloneComponents() { return !Utils.stringIsEmpty(ng2Components); }
```

#### 3.3.2 WebPackagesListener Changes

The `allcomponents.module.ts` generation logic (lines 490-511) is updated:

```java
// For each package:
if (spec.hasStandaloneComponents()) {
    // Import individual components
    String[] components = spec.getNg2Components().split(",");
    for (String component : components) {
        allComponentsImports.append(component.trim());
        allComponentsImports.append(",\n");
    }
} else {
    // Fallback: import NgModule (backwards compat)
    allComponentsImports.append(spec.getNg2Module());
    allComponentsImports.append(",\n");
}
```

#### 3.3.3 Generated File Evolution

The generated file transitions from:
```typescript
// Phase A: still a module
@NgModule({ imports: [...modules], exports: [...modules] })
export class AllComponentsModule { }
```

To (Phase C):
```typescript
// allcomponents.ts (no longer a module)
import { ServoyBootstrapButton, ... } from '@servoy/bootstrapcomponents';
import { SomeExtraComponent, ... } from '@servoy/extra';
import { LegacyModule } from '@servoy/legacy-pkg'; // fallback for non-migrated

export const ALL_COMPONENTS = [
    ServoyBootstrapButton,
    ...,
    SomeExtraComponent,
    ...,
    LegacyModule, // NgModules can be in the array — Angular handles both
] as const;
```

And `FormComponent` imports the array:
```typescript
@Component({
    standalone: true,
    imports: [...ALL_COMPONENTS, ...CORE_COMPONENTS],
    ...
})
```

### 3.4 Template Binding Safety

Angular's strict template type checking for standalone components is more aggressive. The `ComponentTemplateGenerator` must be hardened:

1. **Skip non-input properties**: Use `pd.isInternal()` to filter properties that don't correspond to `@Input()` (size, location, formIndex, anchors, visible/enabled dataproviders)
2. **Skip structural components**: Components like `servoycore-formcomponent`, `servoycore-navigator`, `servoycore-portal` that are referenced structurally in the template (not via generated `<ng-template>`) must not get generated template entries
3. **CUSTOM_ELEMENTS_SCHEMA removal**: Currently `ServoyModule` uses `CUSTOM_ELEMENTS_SCHEMA` which suppresses binding errors. Once `FormComponent` is standalone and all legitimate components are properly imported, this schema should be removed to catch real errors early.

## 4. Implementation Plan

### Phase A (Internal Standalone)

1. **Fix `ComponentTemplateGenerator.java`** — extend `isInternal()` filtering and component skip list from standalone branch (commit `ee5a4fe1df`)
2. **Convert servoycore components to standalone** — `DefaultNavigator`, `ErrorBean`, `ServoyCoreSlider`, `ServoyCoreFormContainer`, `AddAttributeDirective`, `ServoyCoreFormcomponentResponsiveCotainer`, `SessionView`
3. **Convert servoycore LFC components** — `ListFormComponent`, `RowRenderer`
4. **Convert servoy-public library declarations** to standalone
5. **Convert servoydefault library declarations** to standalone
6. **Convert dialogs/window/ngclientutils library declarations** to standalone
7. **Make `FormComponent` standalone** — with `imports: [CommonModule, FormsModule, AllComponentsModule, AllServicesModules, ServoyPublicModule, ...standaloneServoyCoreComponents]`
8. **Make `DesignFormComponent` standalone** — mirror of FormComponent for designer
9. **Dissolve `ServoyCoreComponentsModule`** — move AG Grid setup to `APP_INITIALIZER` or provider function
10. **Dissolve `LFCModule`** — no longer needed
11. **Dissolve `ServoyModule`** — FormComponent is standalone, move providers to route config
12. **Convert routing** — `MainRoutingModule` → standalone route config with `provideRouter()`
13. **Convert `AppModule`** → `bootstrapApplication()` in `main.ts`
14. **Remove `CUSTOM_ELEMENTS_SCHEMA`** — all component selectors should be properly resolved
15. **Update `WebPackagesListener.java`** — adjust generation of `allcomponents.module.ts` if needed (may need to remove `exports` since FormComponent imports it directly)
16. **Run full test suite and manual testing**

### Phase B (External Package Migration)

17. **Define migration guide** for external package maintainers
18. **Migrate `bootstrapcomponents`** — make all components standalone, keep NgModule as re-export, add `BOOTSTRAP_COMPONENTS` constant
19. **Migrate `servoy-extra-components`** — same pattern
20. **Migrate `aggridcomponents`** — same pattern
21. **Migrate `bootstrapextracomponents`** — same pattern
22. **Add `NG2-Components` attribute** to each package's MANIFEST.MF
23. **Move constructor side effects** to `provideXxx()` functions or `APP_INITIALIZER`

### Phase C (Code Generator Update)

24. **Extend `PackageSpecification.java`** — parse new `NG2-Components` MANIFEST attribute
25. **Update `WebPackagesListener.java`** — detect `NG2-Components` and generate direct imports; fall back to `NG2-Module` when absent
26. **Replace `allcomponents.module.ts`** with `allcomponents.ts` exporting a flat array
27. **Update `FormComponent`** imports to use the generated array
28. **Test with mixed packages** — some migrated (standalone), some not (NgModule fallback)
29. **Update WAR exporter** — ensure `exportNG2ToWar` generates correct allcomponents for the export target

## 5. Acceptance Criteria

- [ ] `FormComponent` is `standalone: true` and renders all component types correctly
- [ ] All 15 internal `.module.ts` files are removed
- [ ] External packages with `NG2-Module` (not yet migrated) continue to work unchanged
- [ ] External packages with `NG2-Components` are imported as individual standalone components
- [ ] Libraries that ship only standalone components (e.g. ng2-charts v6) can be consumed
- [ ] `ComponentTemplateGenerator` does not emit bindings for non-`@Input()` properties
- [ ] No `CUSTOM_ELEMENTS_SCHEMA` is needed — all selectors resolve via imports
- [ ] WAR export generates correct `allcomponents` file for both module and standalone packages
- [ ] `bootstrapApplication()` replaces `platformBrowserDynamic().bootstrapModule()`
- [ ] All existing tests pass
- [ ] Designer (DesignFormComponent) works correctly with standalone architecture

## 6. Out of Scope

- Zoneless migration (Phase 7 in modernization roadmap — requires standalone to be complete first)
- `svyOnChanges` → `effect()`/`computed()` redesign (deferred, architecturally blocked)
- Signal inputs migration for `basecomponent.ts` (blocked by svyOnChanges pattern)
- NG1 compatibility layer removal
- Performance optimization of the generated template (e.g. deferred loading)

## 7. Open Questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `NG2-Components` list be auto-detected from the package's public_api.ts exports, or must it be explicitly declared in MANIFEST.MF? | Johan | resolved — MANIFEST should declare individual component names (e.g. `NG2-Components: ServoyBootstrapButton,ServoyBootstrapLabel,...`) since the code generator needs the exact class names + the NPM package path to produce the import statement. The dist structure (`dist/servoy/bootstrapcomponents/`) gives the package path, MANIFEST gives the class names. |
| How should package constructor side effects (SpecTypesService registration, AG Grid license) be migrated — `APP_INITIALIZER`, environment injector, or `provideXxx()` pattern? | Johan | resolved — use `APP_INITIALIZER`. Needs to run once per package at app startup. Built into the framework, not environment-specific. |
| Should the `allcomponents.module.ts` → `allcomponents.ts` rename happen in Phase A or only in Phase C? | Team | resolved — Phase C (keep module in Phase A for minimal disruption) |
| What is the minimum Angular version external package consumers must support? | Team | resolved — Angular 22 is the target for all packages |
| Should `ListFormComponent` import `AllComponentsModule` directly (same as FormComponent)? | Johan | resolved — yes, LFC is treated identically to FormComponent for imports |
| Should `ListFormComponent` import `AllComponentsModule` directly (to render arbitrary components inside list rows) or should the LFC template also be generated? | Johan | open |
