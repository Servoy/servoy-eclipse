# Triage Report — SVY-21388

**Verdict:** PROCEED (but with a fundamentally different approach than proposed)

## Reported problem

When component packages migrated from `NG2-Module` to `NG2-Components` (standalone), the `SpecTypesService.registerType()` calls in NgModule constructors stopped executing. These calls register custom object type constructors so that the JSON object converter (`CustomObjectType.initCustomObjectValue()`) can set the correct class prototype when deserializing server data. Without the registration, custom objects arrive as plain objects lacking any class-defined methods.

The ticket's concrete symptom example: `dataproviderToLowerCase is not a function` on PowerGridColumn in the aggrid powergrid component.

## Root-cause assessment

The problem is real but **dramatically narrower** than the ticket describes. After auditing every custom type class across all six affected packages, the evidence shows:

### Only ONE class has a functional method

| Package | Class | Has methods? | registerType needed? |
|---------|-------|-------------|---------------------|
| aggrid | `PowerGridColumn` | **Yes**: `dataproviderToLowerCase()` (12 call sites) | **Yes** — only this one |
| aggrid | `DataGridColumn`, `GroupedColumn`, `HashedFoundset`, `IconConfig`, `ToolPanelConfig`, `MainMenuItemsConfig`, `AggFuncInfo` | No — pure data holders | No |
| bootstrap | `Tab` | No — pure data holder | No |
| bootstrapextra | `MenuItem` (×2), `Slide`, `AddOn`, `AddOnButton` | No — pure data holders | No |
| servoy-extra | `Column`, `KeycodeSettings`, `Binding`, `Callback`, `RelationInfo`, `LevelVisibilityType` | No — pure data holders | No |
| custom-rendered | `SortableOptions` | Only `getWatchedProperties()` → returns `[]` (deprecated no-op) | No |
| window | `Popup`, `MenuItem`, `Shortcut`, `PopupMenuShowCommand` | No — pure data holders | No |

**Evidence sources:**
- `aggridcomponents/aggrid/projects/nggrids/src/powergrid/powergrid.ts:2659-2661` — the single method
- `com.servoy.eclipse.ngclient.ui/node/projects/servoy-public/src/lib/spectypes.service.ts:35-45` — `registerType()` deprecation: "Calling this IS NOT MANDATORY"
- `com.servoy.eclipse.ngclient.ui/node/src/ngclient/converters/json_object_converter.ts:523-539` — converter fallback: when no type is registered, `BaseCustomObject.prototype` is used (identical to what these data-holder classes provide)

### Why pure data holders don't need registerType

The converter (`json_object_converter.ts:523-539`) does this when deserializing:
1. **If registered** → clones `CustomObjectValue.prototype` and chains `registeredCustomObjectTypeConstructor.prototype`
2. **If NOT registered** → uses `customObjectValuePrototypeWithDeprecated` which chains `BaseCustomObject.prototype`

For pure data holder classes that only declare fields (e.g. `headerText!: string;`), the TypeScript field declarations with `!` are type annotations only — they produce no runtime prototype methods. The prototype chain is functionally identical whether the class is registered or not, because the actual data comes from the server JSON and is set directly on the object instance.

### Current MANIFEST.MF status confirms dead code

| Package | MANIFEST.MF | registerType runs? |
|---------|------------|-------------------|
| aggrid | `NG2-Components` only | **No** — dead code |
| bootstrapcomponents | `NG2-Components` only | **No** — dead code |
| bootstrapextracomponents | `NG2-Components` only | **No** — dead code |
| servoy-extra-components | Both `NG2-Module` + `NG2-Components` | Yes (interim workaround) |
| custom-rendered-components | Both `NG2-Module` + `NG2-Components` | Yes (interim workaround) |

Three packages are already running with standalone-only MANIFEST.MF and non-executing registerType calls. The sky hasn't fallen — except for `PowerGridColumn.dataproviderToLowerCase()`.

## Ticket premise check

The ticket proposes a **new `customTypeRegistration` spec-level property** + template generator changes + registration service pattern + migration skill updates. This is a multi-layer infrastructure change spanning Java (sablo spec parsing, ComponentTemplateGenerator), Angular (allservices.service.ts, new service pattern), component .spec files, and documentation.

**This is over-engineered for the actual problem.** The ticket assumes all ~25 registerType calls are functionally necessary. The code proves only 1 is. Building generic infrastructure to preserve a deprecated pattern (that the codebase itself documents as "NOT MANDATORY") is the wrong direction.

The `SpecTypesService.registerType()` was deprecated precisely because the system no longer needs it for most cases. The correct migration path is to complete the deprecation, not build new mechanisms to keep it alive.

## Approaches considered

### 1. Remove deprecated registerType calls, inline the one method (Recommended)

**Changes needed:**
- **aggrid**: Replace 12 occurrences of `column.dataproviderToLowerCase()` with `(column.dataprovider?.toLowerCase?.() || '')` in `powergrid.ts`. Convert `PowerGridColumn` to an interface. Remove the registerType call.
- **All other packages**: Convert custom type classes to interfaces (or just remove the registerType calls — the classes can remain for TypeScript typing but the registration is unnecessary).
- **Remove dead NgModule code** from packages that only have `NG2-Components` in MANIFEST.MF.
- **servoy-extra, custom-rendered**: Remove `NG2-Module` from MANIFEST.MF once registerType calls are removed.

**Pros:**
- Completes the migration that SpecTypesService deprecation started
- Zero Java-side changes needed
- Removes dead code across 6 packages
- No new infrastructure to maintain
- Aligns with documented best practice ("Calling this IS NOT MANDATORY")

**Cons:**
- Touches multiple external repos (aggrid, bootstrap, bootstrapextra, servoy-extra, custom-rendered)
- `dataproviderToLowerCase` was intentionally added for SVY-20337; inlining it is slightly less DRY (but it's a one-liner)

### 2. New spec-level `customTypeRegistration` property (ticket's proposal)

**Changes needed:**
- Sablo: extend spec parsing to read `customTypeRegistration` property
- Java: modify `ComponentTemplateGenerator` / `WebPackagesListener` to generate imports + inject for registration services
- Angular: modify `allservices.service.ts` to accept generated registration service injections
- Each package: create registration services, add spec entries
- Migration skill: document new pattern

**Pros:**
- Generic mechanism for future third-party packages that genuinely need custom object classes
- Tree-shakeable

**Cons:**
- Preserves and incentivizes a deprecated pattern
- ~25 of 26 affected registrations don't need this mechanism at all
- Multi-layer change (Java spec parsing + template generator + Angular services + 6 component packages)
- Adds ongoing maintenance burden for infrastructure that solves a problem that barely exists

### 3. Angular `ENVIRONMENT_INITIALIZER` for the rare case

**Changes needed:**
- Packages that genuinely need `registerCustomObjectType()` export an `ENVIRONMENT_INITIALIZER` provider
- `WebPackagesListener` adds a new MANIFEST.MF attribute (e.g. `NG2-Initializers`) and generates the provider imports

**Pros:**
- Uses standard Angular DI mechanism (no custom spec property needed)
- Only packages that need it opt in
- Smaller Java-side change than approach 2

**Cons:**
- Still builds infrastructure for a near-nonexistent problem
- Requires generator changes

### 4. No code change — keep NG2-Module alongside NG2-Components

**Pros:**
- Zero effort
- Already working for servoy-extra and custom-rendered

**Cons:**
- Packages carry dead NgModule code indefinitely
- Doesn't fix aggrid/bootstrap/bootstrapextra (already standalone-only)
- Blocks full NgModule removal (Angular modernization Phase 6b)

## Recommendation

**Approach 1: Remove deprecated registerType calls, inline the one method.**

The problem is real but surgical. Out of ~25 custom type registrations across 6 packages, exactly ONE has a functional method (`PowerGridColumn.dataproviderToLowerCase()`). The rest are pure data holders where `registerType()` has zero runtime effect.

The fix is to complete the deprecation already documented in `SpecTypesService`, not to build new infrastructure to preserve the deprecated pattern. This aligns with the Angular modernization roadmap (Phase 6b NgModule removal) and removes dead code.

If in the future a third-party package genuinely needs `registerCustomObjectType()` with a class that has real methods, approach 3 (ENVIRONMENT_INITIALIZER) can be added at that point — YAGNI until then.

**Note on `custom-rendered-components`:** The `Sortable.mount()` side effects in `customrenderedcomponents.module.ts` (lines 9-10) are a separate concern from registerType. These plugin registrations should be moved to component constructors or an initializer independently.

## Git history findings

- **`PowerGridColumn.dataproviderToLowerCase()`** added in commit `7c3fac01` (2025-07-15) by dtimut for SVY-20337: "datasets created by querying in-memory datasets are created with uppercase column names — use lowercase inside powergrid". This is an intentional convenience method, but trivially inlineable.
- **All registerType calls** in aggrid/nggrids.module.ts originate from commit `2a27c713` (2021-10-29) by Gabi Boros — the original NG2 migration. These were written before the SpecTypesService deprecation made them unnecessary.
- **Standalone migration** (SVY-19023 Phase C) in commit `877f83cbe5` added `NG2-Components` support to `WebPackagesListener` but did not address the orphaned registerType calls.
