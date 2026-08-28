# Spec: SVY-21388 — Remove deprecated registerType calls and complete SpecTypesService deprecation

## 1. Goal

Remove all deprecated `SpecTypesService.registerType()` calls across component packages and inline the single functional method (`PowerGridColumn.dataproviderToLowerCase()`). This completes the deprecation already documented in `SpecTypesService`, eliminates dead NgModule code from standalone-migrated packages, and unblocks full NgModule removal (Angular modernization Phase 6b). No new infrastructure is needed — the converter's built-in fallback (`BaseCustomObject.prototype`) already handles all pure data holder types correctly.

## 2. Background

### 2.1 How custom type registration worked (NG2-Module era)

When component packages used `NG2-Module`, the NgModule constructor ran at app startup, calling `SpecTypesService.registerType()` for each custom object type. This registered a class constructor so that the JSON object converter (`CustomObjectType.initCustomObjectValue()`) could set the correct prototype when deserializing server data.

### 2.2 The standalone migration broke timing

With `NG2-Components` (standalone), the NgModule is never instantiated — so constructor-based `registerType()` calls become dead code. Three packages (aggrid, bootstrapcomponents, bootstrapextracomponents) already have `NG2-Components` only in their `MANIFEST.MF`, meaning their registerType calls have been dead for some time. Two packages (servoy-extra-components, customrenderedcomponents) still declare both `NG2-Module` and `NG2-Components`.

### 2.3 The converter fallback makes registration unnecessary for data holders

The converter in `json_object_converter.ts:523-539` has two paths:
1. **Registered type** → clones `CustomObjectValue.prototype` and chains the registered constructor's prototype
2. **Unregistered type** → uses `customObjectValuePrototypeWithDeprecated` which chains `BaseCustomObject.prototype`

For classes that only declare fields with `!` (type annotations only — no runtime prototype methods), the prototype chain is functionally identical whether registered or not. The actual data comes from server JSON and is set directly on the object instance.

### 2.4 Only one class has a functional method

Audit of all ~27 registerType calls across 7 packages found exactly ONE class with a real method:

| Package | Class | Method | Needed? |
|---------|-------|--------|---------|
| aggrid | `PowerGridColumn` | `dataproviderToLowerCase()` — 12 call sites | **Yes** (inline it) |
| aggrid | `DataGridColumn`, `GroupedColumn`, `HashedFoundset`, `IconConfig`, `ToolPanelConfig`, `MainMenuItemsConfig`, `AggFuncInfo` | None | No |
| bootstrapcomponents | `Tab` | None | No |
| bootstrapextracomponents | `MenuItem`, `dropdown_MenuItem`, `Slide`, `AddOn`, `AddOnButton` | None | No |
| servoy-extra-components | `Column`, `KeycodeSettings`, `Binding`, `Callback`, `RelationInfo`, `LevelVisibilityType` | None | No |
| customrenderedcomponents | `SortableOptions` | `getWatchedProperties()` → returns `[]` (deprecated no-op) | No |
| window | `PopupMenuShowCommand`, `Popup`, `Shortcut`, `MenuItem`, `PopupForm` | None | No |

### 2.5 Current MANIFEST.MF status

| Package | NG2-Module | NG2-Components | registerType runs? |
|---------|-----------|---------------|-------------------|
| aggrid | **No** | Yes | No — dead code |
| bootstrapcomponents | **No** | Yes | No — dead code |
| bootstrapextracomponents | **No** | Yes | No — dead code |
| servoy-extra-components | Yes | Yes | Yes (via module) |
| customrenderedcomponents | Yes | Yes | Yes (via module) |
| window (internal) | Yes | N/A (service) | Yes (via module) |

### 2.6 SpecTypesService deprecation documentation

`SpecTypesService.registerType()` itself is marked `@deprecated` with the message:
> "SpecTypesService.registerType updates and extending BaseCustomObject is no longer necessary. Code that registers it [...] should be safe to remove/clean up."

The companion `registerCustomObjectType()` docs state:
> "Calling this IS NOT MANDATORY. You can just use simple interfaces (or interfaces that extend as well ICustomObjectValue) for your custom objects now."

## 3. Design

### 3.1 Inline `dataproviderToLowerCase()` in powergrid.ts

The method body is:
```typescript
dataproviderToLowerCase(): string {
    return this.dataprovider?.toLowerCase?.() || '';
}
```

Each of the 12 call sites (`column.dataproviderToLowerCase()`) is replaced with the inline expression `(column.dataprovider?.toLowerCase?.() || '')`. This is a trivial one-liner that does not warrant a standalone utility function.

### 3.2 Convert custom type classes to interfaces

Classes that only declare fields (with `!` non-null assertions) produce no runtime prototype methods — the `!` is a TypeScript type annotation only. These classes can be safely converted to interfaces (removing `extends BaseCustomObject`) without any runtime behavior change. The interfaces can optionally extend `ICustomObjectValue` if the component needs `markSubPropertyAsHavingDeepChanges()`.

Classes that remain as interfaces:
- `PowerGridColumn` → interface (after inlining the method)
- `AggFuncInfo` → keep as class (has `aggFunc` callback field — but no registerType needed)
- All other custom type classes across packages → can be converted to interfaces at the package maintainer's discretion; this is **optional** and not required for correctness

The minimum change is to remove the `registerType()` calls. Class-to-interface conversion is a follow-up improvement.

### 3.3 Remove NgModule files from standalone-only packages

Packages that have `NG2-Components` only (no `NG2-Module` in MANIFEST.MF) can have their `*.module.ts` files deleted entirely:
- `aggrid/projects/nggrids/src/nggrids.module.ts`
- `bootstrapcomponents/projects/bootstrapcomponents/src/servoybootstrap.module.ts`
- `bootstrapextracomponents/projects/bootstrapextracomponents/src/servoybootstrapextra.module.ts`

These modules are never instantiated and serve no purpose.

### 3.4 Clean up dual-mode packages (servoy-extra, custom-rendered)

For packages that still declare both `NG2-Module` and `NG2-Components`:
1. Remove `registerType()` calls from the NgModule constructor
2. Remove `NG2-Module` line from `META-INF/MANIFEST.MF`
3. If the module constructor is now empty and the module has no providers, delete the module file
4. Remove `SpecTypesService` import if no longer used

**Special case — customrenderedcomponents:** The module file also contains `Sortable.mount()` side effects (lines 9-10 of `customrenderedcomponents.module.ts`). These are unrelated to registerType and must be moved to the component files that use them (`customlist.component.ts` and `foundsetlist.component.ts`) or to a top-level side-effect import before the module file can be deleted.

### 3.5 Clean up window service module

The `windowservice.module.ts` also provides `WindowPluginService`, `ShortcutService`, and `PopupMenuService` as module providers. The module cannot be deleted — only the `registerType()` calls and `SpecTypesService` import should be removed.

### 3.6 No Java-side changes needed

The `SpecTypesService` deprecation is entirely client-side (Angular/TypeScript). No changes are needed in:
- Sablo spec parsing
- `ComponentTemplateGenerator`
- `WebPackagesListener`
- Any Java plugin code

## 4. Implementation plan

### Phase 1: aggrid (the only package with a real method)

1. **Inline `dataproviderToLowerCase()`** — In `aggrid/projects/nggrids/src/powergrid/powergrid.ts`, replace all 12 occurrences of `column.dataproviderToLowerCase()` with `(column.dataprovider?.toLowerCase?.() || '')` at lines: 840, 944, 1790, 1803, 1909, 1952, 1954, 1957, 1974, 1976, 1979, 2259.
2. **Convert `PowerGridColumn` to an interface** — Remove `extends BaseCustomObject`, change `class` to `interface`, remove the `dataproviderToLowerCase()` method body, remove `!` assertions (interface fields don't need them). Keep the field declarations for TypeScript typing. Also convert or keep `AggFuncInfo` as-is (it has no registerType dependency but has a callback field).
3. **Delete `nggrids.module.ts`** — The file only contains registerType calls and re-exports standalone components. It is not referenced by any MANIFEST.MF.
4. **Remove `BaseCustomObject` import** from `powergrid.ts` if no longer used.
5. **Lint and build** the aggrid package.

### Phase 2: bootstrapcomponents

6. **Delete `servoybootstrap.module.ts`** — Only contains registerType calls for `Tab`. Not referenced from MANIFEST.MF.
7. **Verify `Tab` class** in `bts_basetabpanel.ts` — If it only declares fields, optionally convert to interface. Not required for correctness.
8. **Lint and build** the bootstrapcomponents package.

### Phase 3: bootstrapextracomponents

9. **Delete `servoybootstrapextra.module.ts`** — Only contains registerType calls. Not referenced from MANIFEST.MF.
10. **Lint and build** the bootstrapextracomponents package.

### Phase 4: servoy-extra-components

11. **Remove registerType calls** from constructor in `servoyextra.module.ts`.
12. **Remove `NG2-Module: ServoyExtraComponentsModule`** line from `META-INF/MANIFEST.MF`.
13. **Delete `servoyextra.module.ts`** if the module constructor is now empty and has no non-registerType providers. (Note: it has `FileTypesUtilsService` as a provider and `CUSTOM_ELEMENTS_SCHEMA` — the module may need to stay if those are needed. If so, just remove the registerType lines and SpecTypesService constructor parameter.)
14. **Lint and build** the servoy-extra-components package.

### Phase 5: customrenderedcomponents

15. **Move `Sortable.mount()` calls** from `customrenderedcomponents.module.ts` lines 9-10 to the component files that use Sortable (`customlist.component.ts` and/or `foundsetlist.component.ts`), or to a shared side-effect file imported by both components.
16. **Remove registerType calls** from the module constructor.
17. **Convert `SortableOptions` to an interface** in `sortableoptions.ts` — remove `extends BaseCustomObject` and the `getWatchedProperties()` method (deprecated no-op returning `[]`).
18. **Remove `NG2-Module: CustomRenderedComponentsModule`** from `META-INF/MANIFEST.MF`.
19. **Delete `customrenderedcomponents.module.ts`** if now empty.
20. **Lint and build** the customrenderedcomponents package.

### Phase 6: window (internal service)

21. **Remove registerType calls** (5 calls) from `windowservice.module.ts` constructor. Keep the module — it provides `WindowPluginService`, `ShortcutService`, `PopupMenuService`.
22. **Remove `SpecTypesService` import** and constructor parameter injection.
23. **Lint and build** the ngclient.ui project.

### Phase 7: Verify public API (d.ts) and optional cleanup

24. **Rebuild `@servoy/public` library** (`npm run build_libs` in `com.servoy.eclipse.ngclient.ui/node/`) to update the `.d.ts` files. The `registerType()` and `registerCustomObjectType()` methods remain available (for backwards compatibility of third-party packages) but are documented as deprecated.
25. **Optionally remove `BaseCustomObject` extends** from any remaining custom type classes across packages. This is cosmetic and can be done incrementally.

## 5. Acceptance criteria

- [ ] All `registerType()` calls are removed from: aggrid, bootstrapcomponents, bootstrapextracomponents, servoy-extra-components, customrenderedcomponents, window
- [ ] `PowerGridColumn.dataproviderToLowerCase()` is inlined at all 12 call sites in `powergrid.ts`
- [ ] `PowerGridColumn` is converted from a class to an interface (no runtime behavior change)
- [ ] NgModule files are deleted from standalone-only packages (aggrid, bootstrapcomponents, bootstrapextracomponents)
- [ ] `NG2-Module` is removed from MANIFEST.MF in servoy-extra-components and customrenderedcomponents
- [ ] `Sortable.mount()` side effects in customrenderedcomponents are preserved (moved to component files)
- [ ] `windowservice.module.ts` retains its service providers but has registerType calls removed
- [ ] All affected packages build and lint successfully
- [ ] No Java-side changes are required or introduced
- [ ] The NG client runs correctly with forms using: AG Grid (powergrid + datagrid), Bootstrap tabpanel/accordion, Bootstrap Extra navbar/carousel/dropdown/inputgroup, Servoy Extra table/treeview/splitpane, Custom Rendered lists
- [ ] Column IDs in PowerGrid correctly resolve to lowercase dataprovider names (regression test for SVY-20337)

## 6. Out of scope

- Building new spec-level `customTypeRegistration` infrastructure (the ticket's original proposal) — not needed since only 1 of ~27 registrations had a functional method
- Converting all custom type classes to interfaces — optional follow-up; only `PowerGridColumn` and `SortableOptions` are converted as part of this spec
- Deprecating or removing `SpecTypesService.registerCustomObjectType()` API — it stays available for third-party packages that genuinely need custom classes
- Removing `BaseCustomObject` from `@servoy/public` exports — backwards compatibility
- Migrating `globiscomponents` registerType calls (third-party, separate repo/schedule)
- Full NgModule removal from window service module (it still provides services)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Can `servoyextra.module.ts` be deleted entirely, or do its providers (`FileTypesUtilsService`) and `CUSTOM_ELEMENTS_SCHEMA` need to remain in a module? If providers are needed, they can be moved to `providedIn: 'root'` on the service itself. | dev | open |
| Should `AggFuncInfo` (which has an `aggFunc` callback field) be converted to an interface or kept as a class? It doesn't use registerType but extends `BaseCustomObject`. | dev | open |
| Do the `Sortable.mount()` calls need to run once globally, or can they safely run in each component constructor? If global, a shared `sortable-init.ts` side-effect file is cleaner. | dev | open |
