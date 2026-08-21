---
name: servoy-component-migration
description: "Use when migrating a Servoy component package to the latest standards: Angular 22, Vitest, ESLint flat config, standalone components, signals, OnPush, zoneless readiness. Triggered by 'migrate', 'migration', 'upgrade angular', 'standalone migration', or '/migrate'."
---

# Servoy Component Migration Pipeline

You are the **orchestrator** for migrating a Servoy component package to the latest platform standards. This skill handles the full lifecycle from Angular version upgrades through to zoneless-ready signal-based components.

## Design Principles

- **Incremental**: Each phase is independent. Skip phases that are already complete.
- **State-aware**: Always detect current status before acting.
- **Non-destructive**: Commit after each phase so rollback is easy.
- **Updatable**: When Angular 23 arrives or Servoy adds new requirements, update the relevant phase section.

---

## Phase 0 — Status Detection

Before doing anything, detect the current state of the project. Run these checks and present a summary table to the user:

```
| Phase | Status | Details |
|-------|--------|---------|
| Angular version | ? | Check package.json @angular/core version |
| TypeScript version | ? | Check package.json typescript version |
| ESLint config | ? | flat config (eslint.config.js) vs legacy (.eslintrc) |
| Test framework | ? | Vitest vs Karma/Jasmine |
| Deprecated packages | ? | List any deprecated @angular/* or other packages |
| Standalone components | ? | standalone: true on all components? |
| NG2-Components manifest | ? | NG2-Components attribute in MANIFEST |
| Servoy spec alignment | ? | All spec properties have @Input or serveronly tag |
| Signal inputs | ? | input() vs @Input() |
| Template-bound signals | ? | All mutable template-bound properties use signal() |
| OnPush | ? | ChangeDetectionStrategy.OnPush on all components |
| Zoneless readiness | ? | No ChangeDetectorRef usage, no zone.js dependency |
```

### How to detect:

1. **Angular version**: `jq .dependencies[\"@angular/core\"] package.json`
2. **TypeScript**: `jq .devDependencies.typescript package.json`
3. **ESLint**: check if `eslint.config.js` exists (flat) or `.eslintrc*` (legacy)
4. **Test framework**: check if `vitest` is in devDependencies, or `karma`/`jasmine`
5. **Deprecated packages**: check for `@angular/platform-browser-dynamic`, `@angular/compiler`, zone.js (if not needed)
6. **Standalone**: grep for `standalone: false` or absence of `standalone: true` in component .ts files
7. **NG2-Components**: check MANIFEST.MF for `NG2-Components:` attribute
8. **Spec alignment**: compare .spec model properties vs @Input declarations (include `model()` and `input()`)
9. **Signal inputs**: count `@Input()` vs `input()` in component files
10. **Template-bound signals**: scan for plain class properties (non-signal) used in template bindings that are mutated programmatically
11. **OnPush**: check for `ChangeDetectionStrategy.OnPush` in @Component decorators
12. **Zoneless**: grep for `ChangeDetectorRef`, `NgZone`, zone.js imports

Present the table, then ask the user which phase to start with (or suggest the logical next phase).

---

## Phase 1 — Angular Upgrade

Angular must be upgraded **one major version at a time**. You cannot skip versions.

### Determine upgrade path

Check current Angular version. If < 22, the upgrade path is:
- 19 → 20 → 21 → 22
- 20 → 21 → 22
- 21 → 22

### For each version step:

Tell the user to run:

```bash
npx @angular/cli@<target> update @angular/core@<target> @angular/cli@<target> --force
```

For example, from 20 to 21:
```bash
npx @angular/cli@21 update @angular/core@21 @angular/cli@21 --force
```

The Angular CLI will:
1. Update package.json dependencies
2. Run schematics that auto-migrate code
3. May prompt for confirmations — **always accept**

After each step:
1. Run `npm install --legacy-peer-deps`
2. Fix any TypeScript compilation errors (`npx tsc --noEmit`)
3. Fix any build errors (`npm run build`)
4. Commit: `upgrade to Angular <version> [ai]`

### TypeScript alignment

Angular 22 requires TypeScript 6.0+. After Angular upgrade, verify:
```bash
npx tsc --version
```

If TypeScript was also upgraded, check for breaking changes:
- `any` type inference changes
- Stricter null checks
- New ES target requirements

### Remove deprecated packages

After reaching Angular 22, remove:
- `@angular/platform-browser-dynamic` (use `@angular/platform-browser`)
- `@angular/compiler` (if no JIT usage — AOT is default)
- Any other deprecated packages flagged by `ng update`

### esbuild: Node.js globals polyfill

Angular 22 uses the `@angular/build:application` builder (esbuild-based) instead of the old webpack-based builder. **esbuild does NOT polyfill Node.js globals** (`global`, `process`, `Buffer`) that webpack provided automatically.

**Detection:** After a successful build, check if any dependency uses CommonJS libraries that reference `global`:
```bash
grep -r "global\b" node_modules/<suspect-package>/ --include="*.js" -l | head -5
```

Known problematic libraries: `dragula`, `custom-event`, `crossvent`, any package using `global` as a fallback for `window`.

**Fix:** Create a side-effect polyfill file in the component that imports the problematic library:
```typescript
// global-polyfill.ts
(window as any).global = window;
```

Import it BEFORE the problematic library (ESM evaluates side-effect imports in order):
```typescript
import './global-polyfill';
import jKanban from "@servoy/jkanban"; // ← this pulls in dragula → global
```

**Important:** Do NOT add this to the core TiNG polyfills.ts — the component that uses the library is responsible for its own polyfills.

---

## Phase 2 — ESLint Migration

### From legacy (.eslintrc) to flat config (eslint.config.js)

1. Install ESLint 10+: `npm install eslint@latest --save-dev --legacy-peer-deps`
2. Install Angular ESLint: `npm install @angular-eslint/builder @angular-eslint/eslint-plugin @angular-eslint/eslint-plugin-template @angular-eslint/template-parser --save-dev --legacy-peer-deps`
3. Create `eslint.config.js` using the flat config format:

```javascript
const eslint = require('@eslint/js');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = tseslint.config(
  { files: ['**/*.ts'], extends: [...tseslint.configs.recommended, ...angular.configs.tsRecommended], rules: { /* project rules */ } },
  { files: ['**/*.html'], extends: [...angular.configs.templateRecommended, ...angular.configs.templateAccessibility] }
);
```

4. Remove legacy files: `.eslintrc.json`, `.eslintrc.js`, `.eslintrc`
5. Update `angular.json` lint target to use the new builder
6. Run `npx ng lint` and fix issues
7. Commit: `migrate to ESLint flat config [ai]`

### Standard rules for Servoy packages:

- `@stylistic/ts/quotes`: single quotes
- `max-len`: 200 characters
- Component selector prefix matches package (e.g. `bootstrapcomponents-`)
- `@angular-eslint/component-class-suffix`: off
- Use `eslint-plugin-only-warn` to downgrade errors to warnings

---

## Phase 3 — Test Migration (Karma → Vitest)

### Setup Vitest

1. Install: `npm install vitest @angular/build --save-dev --legacy-peer-deps`
2. Create `vitest-base.config.ts`:
```typescript
import { defineConfig } from 'vitest/config';
export default defineConfig({ test: { globals: true, environment: 'jsdom' } });
```
3. Update `angular.json` test target:
```json
{ "builder": "@angular/build:unit-test", "options": { "tsConfig": "tsconfig.spec.json" } }
```
4. Add scripts to `package.json`:
```json
"test": "ng test --no-watch",
"test:watch": "ng test",
"test:ui": "ng test --ui"
```

### Migrate test files

For each `*.spec.ts`:
1. Replace `import { ... } from 'jasmine'` patterns
2. Add `import { describe, it, expect, beforeEach, vi } from 'vitest'`
3. Replace `jasmine.createSpyObj` → manual mock objects with `vi.fn()`
4. Replace `spyOn(obj, 'method')` → `vi.spyOn(obj, 'method')`
5. Replace `fakeAsync/tick` → `vi.useFakeTimers()/vi.advanceTimersByTime()`
6. Replace `waitForAsync` → `async/await`
7. Use `fixture.componentRef.setInput('name', value)` for signal inputs

### Remove Karma

Remove from devDependencies: `karma`, `karma-*`, `jasmine-core`, `@types/jasmine`
Remove files: `karma.conf.js`, `karma.dev.conf.js`

### Critical: Global Mocking Rules

- **NEVER** use `vi.stubGlobal('document', ...)` or `vi.stubGlobal('window', ...)` — this breaks jsdom
- Mock individual methods with `vi.spyOn` and restore in `afterEach`

### Zoneless Testing

Component library tests should run **without Zone.js** for speed and accuracy:

1. **Remove `zone.js` from polyfills** in the dummy project's `angular.json`:
   ```json
   "polyfills": [],
   ```

2. **For components with heavy third-party libs** (e.g., Uppy/Tus with internal timers), add `provideZonelessChangeDetection()` to the TestBed config to prevent `fixture.whenStable()` from waiting on unrelated macrotasks:
   ```typescript
   import { provideZonelessChangeDetection } from '@angular/core';

   await TestBed.configureTestingModule({
       imports: [MyComponent, ServoyPublicTestingModule],
       providers: [provideZonelessChangeDetection()],
   }).compileComponents();
   ```

3. **`setTimeout` callbacks are invisible to `fixture.whenStable()`** without Zone.js. If component code uses `setTimeout(() => callback())`, the test must flush it explicitly:
   ```typescript
   input.click();
   fixture.detectChanges();
   await new Promise(resolve => setTimeout(resolve, 0));
   expect(callback).toHaveBeenCalled();
   ```

4. **Suppress expected console warnings** in tests that intentionally trigger them:
   ```typescript
   const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
   // ... trigger the warning ...
   expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('expected message'));
   consoleSpy.mockRestore();
   ```

Commit: `migrate tests to Vitest [ai]`

---

## Phase 4 — Standalone Components

### Convert components to standalone

For each component:
1. Add `standalone: true` to `@Component` decorator
2. Add `imports: [...]` with all directives/pipes used in the template
3. Remove component from NgModule `declarations`

### Update NgModule

The NgModule becomes a re-export barrel only (or can be removed entirely):
```typescript
@NgModule({ imports: [AllComponents...], exports: [AllComponents...] })
export class MyComponentsModule {}
```

### Servoy integration: NG2-Components

Update the package MANIFEST.MF:
1. **Replace** `NG2-Module: <ModuleName>` with `NG2-Components: Component1,Component2,...` listing all Angular component class names
2. Add `Entry-Point: projects/<package-name>` pointing to the Angular library
3. Ensure `NPM-PackageName: @scope/package` is set

**Important:** After converting to standalone, the old `NG2-Module` attribute is no longer valid — standalone components are registered individually, not via a module. If `NG2-Module` is still present, **remove it** and add `NG2-Components` instead. The `ComponentTemplateGenerator` uses `NG2-Components` to determine which packages to include in the generated template.

### Update test files

After converting to standalone, all test files that use `TestBed.configureTestingModule` must be updated:
- Move the component under test from `declarations: [...]` to `imports: [...]`
- If `declarations` becomes empty, remove it entirely
- Keep `schemas: [NO_ERRORS_SCHEMA]` if present
- Keep other items in `imports` (like `ServoyPublicTestingModule`, `FormsModule`)

If a standalone component imports a third-party component that fails in jsdom (e.g., canvas-gauges, Uppy Dashboard), use `TestBed.overrideComponent()` to swap it with a mock:
```typescript
@Component({ selector: 'the-selector', template: '', standalone: true })
class MockComponent {}

TestBed.configureTestingModule({
    imports: [TheComponentUnderTest],
}).overrideComponent(TheComponentUnderTest, {
    remove: { imports: [RealThirdParty] },
    add: { imports: [MockComponent] }
}).compileComponents();
```

Alternatively, if the third-party just needs a browser API (like `canvas.getContext`), mock the API directly:
```typescript
let originalGetContext: typeof HTMLCanvasElement.prototype.getContext;
beforeEach(() => {
    originalGetContext = HTMLCanvasElement.prototype.getContext;
    HTMLCanvasElement.prototype.getContext = vi.fn().mockReturnValue({ /* mock 2d ctx */ }) as any;
});
afterEach(() => {
    HTMLCanvasElement.prototype.getContext = originalGetContext;
});
```

Commit: `convert to standalone components [ai]`

---

## Phase 5 — Signal Inputs & OnPush

### Update @servoy/public to latest

Before converting component signals, update `@servoy/public` to the latest version that has signal-based base class properties (`name`, `servoyApi`, `elementRef`).

```bash
npm install @servoy/public@latest --legacy-peer-deps
```

Verify the installed version has signal-based base class:
```bash
grep "InputSignal" node_modules/@servoy/public/types/servoy-public.d.ts | head -3
```

If the latest npm version doesn't have the signal changes yet (ask the user), install from a local tgz build:
```bash
npm pack <path-to-ngclient.ui/node/dist-public> --pack-destination .
npm install ./servoy-public-<version>.tgz --legacy-peer-deps --force
```

The `--force` is required because npm caches `file:` references by version string, not content.

### Fix base class signal changes (compiler-driven approach)

After installing the new `@servoy/public`, run `npm run build`. The compiler will report all errors. Fix them using these patterns:

**TypeScript files (.ts):**
| Error | Fix |
|-------|-----|
| `Property 'x' does not exist on type 'InputSignal<ServoyApi>'` | `this.servoyApi.x()` → `this.servoyApi().x()` |
| `Property 'nativeElement' does not exist on type 'Signal<ElementRef\|undefined>'` | `this.elementRef.nativeElement` → `this.elementRef()!.nativeElement` |
| `This condition will always return true since 'Signal<...>' is always defined` | `if (this.elementRef)` → `if (this.elementRef())` |

**Template files (.html):**
| Error | Fix |
|-------|-----|
| `Property 'getMarkupId' does not exist on type 'InputSignal<ServoyApi>'` | `servoyApi.getMarkupId()` → `servoyApi().getMarkupId()` |
| `Type 'InputSignal<string>' is not assignable to type 'string'` | `[prop]='name'` → `[prop]='name()'` |

**Test files (.spec.ts):**
| Error | Fix |
|-------|-----|
| `Cannot assign to 'servoyApi' because it is a read-only property` | `component.servoyApi = x` → `fixture.componentRef.setInput('servoyApi', x)` |
| `Cannot assign to 'name' because it is a read-only property` | `component.name = x` → `fixture.componentRef.setInput('name', x)` |
| Mock object passed to method expecting ServoyBaseComponent | `{ name: 'x' }` → `{ name: () => 'x' }` |

**Watch out for:**
- Inner classes with their own `name: string` or `servoyApi` fields — do NOT change those
- Components that shadow base class with their own `@Input() name` — leave those as-is
- Callbacks where `this` refers to a different object
- **Shadowed fields**: Some components defined their own `elementRef!: ElementRef` or `name!: string` to override the base class (often as a workaround for ViewChild timing). These must be REMOVED after migration — the base class now provides these as readonly signals. The compiler reports `TS2416` (incompatible override) or `TS2540` (cannot assign to read-only). Remove the shadow field and replace any custom `viewChild` + effect pattern with the base class `elementRef()` directly.
- **Initialization timing**: With `viewChild()`, `elementRef()` returns undefined until after view init. If `svyOnInit()` calls methods that access `elementRef()` or other not-yet-initialized objects (like canvas/chart instances), add null guards: `if (this.elementRef() && this.myObject) { ... }`
- **Optional chaining on elementRef**: Code using `this.elementRef?.nativeElement` must become `this.elementRef()?.nativeElement` — don't forget the `()` before `?.`
- **getNativeElement() overrides**: If a component overrides `getNativeElement()` with `this.elementRef()!.nativeElement.firstChild`, it can crash during `ngOnDestroy` when elementRef is already undefined. Use optional chaining: `this.elementRef()?.nativeElement?.firstChild`
- **viewChild `{ read: ElementRef }` is critical**: The base class `ServoyBaseComponent` uses `viewChild('element', { read: ElementRef })`. Without `{ read: ElementRef }`, when `#element` is placed on an Angular component tag (like `<ngb-progressbar #element>`), `viewChild` returns the **component instance** instead of the ElementRef — causing `nativeElement` to be `undefined`. Note: do NOT use an explicit generic like `viewChild<ElementRef<T>>(...)` — TypeScript picks the wrong overload and reports `read` as unknown property. The correct form is `viewChild('element', { read: ElementRef })` without explicit generic.
- **Test for getNativeElement()**: Every component spec MUST include a regression test that verifies `getNativeElement()` returns a valid element. This catches elementRef resolution bugs immediately:
  ```typescript
  it('should return a valid native element from getNativeElement()', () => {
      expect(component.getNativeElement()).not.toBeNull();
      expect(component.getNativeElement()).toBeInstanceOf(HTMLElement);
  });
  ```
- **Test for servoyAttributes**: Components where `#element` is on a child component (not a plain div) should also verify that `addAttributes()` works:
  ```typescript
  it('should apply servoyAttributes on the native element', async () => {
      fixture.componentRef.setInput('servoyAttributes', { 'data-testid': 'my-comp', 'aria-label': 'label' });
      fixture.detectChanges();
      await fixture.whenStable();
      const nativeEl = component.getNativeElement();
      expect(nativeEl.getAttribute('data-testid')).toBe('my-comp');
  });
  ```
- **Indirect signal access on other instances**: Not just `this.servoyApi` but also references to signals on OTHER component instances need `()`. E.g. `this.ngGrid.servoyApi.` → `this.ngGrid.servoyApi().` and `this.dataGrid.servoyApi.` → `this.dataGrid.servoyApi().`
- **`this.name` as function argument**: When `this.name` is passed to a function expecting `string`, the compiler reports `InputSignal<string> is not assignable to string`. Fix: `this.name()`

**Approach:** Always use the compiler. Do NOT do blind regex replacements across all files. The compiler knows exactly which `this.name` is the signal and which is a local property.

**Important: signal inputs and ngOnChanges compatibility**

Angular 22 signal inputs DO still trigger `ngOnChanges` and appear in `SimpleChanges`. This is officially documented at angular.dev:
> "While you should prefer computed and effect when working with signal-based inputs, the ngOnChanges method does include value changes for signal-based inputs."

This means:
- The `svyOnChanges(SimpleChanges)` pattern continues to work for signal inputs
- You do NOT need to replace svyOnChanges with effect() just because inputs became signals
- Components can gradually migrate to effect() for cleaner code, but it's not required for correctness

### Convert @Input/@Output to signals

For each component:
1. `@Input() myProp: string` → `readonly myProp = input<string>(undefined as any)`
2. `@Input() myProp: string; @Output() myPropChange = new EventEmitter<string>()` → `readonly myProp = model<string>(undefined as any)`
3. `@Output() onAction = new EventEmitter()` → `readonly onAction = output<Event>()`

### Update template reads

- `this.myProp` → `this.myProp()` (signal read)
- `this.myProp = x` → `this.myProp.set(x)` (for model signals)

### Add OnPush

Add to every `@Component`:
```typescript
changeDetection: ChangeDetectionStrategy.OnPush
```

### Remove ChangeDetectorRef usage

Replace `this.cdRef.detectChanges()` / `this.cdRef.markForCheck()` with proper signal reactivity. If a value changes that the template observes, use a `signal()` or `computed()`.

### Convert template-bound class properties to signals

**This step is critical for OnPush + standalone components.** After converting inputs/outputs to signals, there are often plain class properties (declared as `propertyName: type = value` or `propertyName!: type`) that are bound in the template via `[prop]="propertyName"`, `{{propertyName}}`, `@if (propertyName)`, or `@for (item of propertyName)`.

These properties are mutated programmatically (e.g., in lifecycle hooks, event handlers, or async callbacks). With `OnPush` change detection, Angular may not detect these mutations, causing either:
- **NG0100 ExpressionChangedAfterItHasBeenCheckedError** — when the value changes during a check cycle
- **Stale UI** — when the value changes outside a check cycle and the template never updates

**Detection:** For each component, compare template bindings against class property declarations. Any property that is:
1. Used in a template binding (`[x]="prop"`, `{{prop}}`, `@if (prop)`, `@for (item of prop)`, `[(ngModel)]="prop"`)
2. NOT declared as `input()`, `output()`, `signal()`, `computed()`, `linkedSignal()`, `model()`, `viewChild()`, `contentChild()`
3. Mutated anywhere in the class (via `this.prop = ...` or `this.prop.push(...)`)

...must be converted to a `signal()`.

**Conversion pattern:**
```typescript
// Before
tabIndex!: number;
data: MyType[] = [];
showDashboard = false;

// After
readonly tabIndex = signal<number>(undefined as any);
readonly data = signal<MyType[]>([]);
readonly showDashboard = signal(false);
```

**Update mutation sites:**
```typescript
// Before
this.tabIndex = -1;
this.data = newData;

// After
this.tabIndex.set(-1);
this.data.set(newData);
```

**Update template reads:**
```html
<!-- Before -->
[tabIndex]="tabIndex"
@if (showDashboard) { ... }
[data]="data"

<!-- After -->
[tabIndex]="tabIndex()"
@if (showDashboard()) { ... }
[data]="data()"
```

**Exceptions — do NOT convert:**
- Properties bound via `[(ngModel)]="prop"` where the component implements two-way binding with the template — use `linkedSignal()` or keep as a plain property with `FormsModule` (ngModel manages its own change tracking)
- Properties that are `EventEmitter` instances used for `[manualRefresh]` patterns — these are observables, not state
- Properties only read in the template but never mutated after initialization (truly constant after constructor/init) — these are safe as plain properties

**Common candidates per Servoy component pattern:**
- Internal state like `isReady`, `showX`, `isCollapsed`
- Computed display data like `data`, `displayNodes`, `images`, `options`
- UI control state like `tabIndex`, `selection`, `containerStyle`

### Remove constructor parameters to ServoyBaseComponent

`ServoyBaseComponent` no longer requires `Renderer2` or `ChangeDetectorRef` in its constructor (parameters are optional in the current release and will be removed entirely in the next). Remove them from subclass constructors:

**Before:**
```typescript
constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
    super(renderer, cdRef);
}
```

**After:** Remove the constructor entirely (if it only calls `super`). If the component has no other constructor logic, Angular will use the default inherited constructor.

If the component still needs `Renderer2` for its own DOM operations, use `inject()`:
```typescript
private readonly renderer = inject(Renderer2);
```

This is required for zoneless readiness — injecting `ChangeDetectorRef` prevents zoneless operation.

### Spec property alignment check

After signal migration, verify:
- Every spec model property has a matching `input()` or `model()` signal, OR is tagged `"serveronly": true`
- No `size` or `location` as runtime properties in specs (these are NOT component inputs)
- **If a spec has a `size` property that is NOT used as an `@Input`/`input()` in the Angular component**, it must be converted to `designsize` — a server-only private property that provides the default dimensions in the form designer:
  ```json
  "designsize": {
    "type": "dimension",
    "tags": {"serveronly": true, "scope": "private"},
    "default": {"width": 80, "height": 30}
  }
  ```
  Keep the same default values that the old `size` property had.
  **Never simply remove `size` without adding `designsize`** — this breaks the designer's drag-and-drop sizing.
- Deprecated specs don't need Angular components

Commit: `migrate to signal inputs and OnPush [ai]`

---

## Phase 6 — Zoneless Readiness Check

This is a verification phase, not necessarily an implementation phase (zoneless requires the whole app to be ready, not just one package).

Check for:
1. No direct `NgZone` usage
2. No `ChangeDetectorRef.detectChanges()` calls (use signals instead)
3. No `setTimeout`/`setInterval` that expects zone.js to trigger change detection
4. All state changes go through signals (no plain class properties bound in templates that are mutated — see Phase 5 "Convert template-bound class properties to signals")
5. All components are OnPush

### Remove zone.js from component library test builds

Component libraries don't need Zone.js for testing. Remove it from the dummy application's polyfills in `angular.json`:
```json
"polyfills": [],
```

This eliminates:
- **NG0914 warnings** ("application is using zoneless change detection but still loading Zone.js")
- **Slow tests** caused by `fixture.whenStable()` waiting on third-party timers (e.g., Uppy/Tus retry delays, dynamic import() resolution)
- **False stability** where Zone.js hides timing issues that would surface in a real zoneless app

### `@for` track expressions (NG0956)

When using `@for` in templates, **never** track by object identity:
```html
<!-- BAD: track by identity → full DOM re-creation when array reference changes -->
@for (item of items(); track item; let idx = $index) {

<!-- GOOD: track by stable ID field -->
@for (item of items(); track item.id; let idx = $index) {

<!-- OK: track by index (when order is stable and items are never reordered) -->
@for (item of items(); track $index; let idx = $index) {
```

Angular emits **NG0956** at runtime when track-by-identity causes full collection re-creation. Use a stable identifier (ID field) from the object model. For Servoy custom objects, common ID fields are:
- `Collapsible.collapsibleId`
- `Card.cardId`
- `MenuItem.id`
- `Slide.imageUrl` (or an index if no unique field exists)

Report findings — don't force zoneless if the runtime (Servoy TiNG) isn't ready yet.

---

## Phase 7 — Final Verification

Before starting any phase, capture a **lint baseline** so you know which warnings are pre-existing vs introduced by the migration:
```bash
npx ng lint 2>&1 | tail -5   # note the warning count
```

After completing all phases, verify:

1. `npm run build` — clean production build, zero errors
2. `npx ng lint` — warning count must be equal to or lower than baseline (zero is the target)
3. `npm run test` — all tests pass
4. Spec alignment scan — no properties without @Input/serveronly
5. MANIFEST.MF has correct NG2-Components and Entry-Point

If lint warnings increased, fix them before committing. The goal is zero lint warnings at all times.

---

## Versioning & Future Updates

This skill is designed to be updated as the platform evolves:

- **Angular 23**: Update Phase 1 with new version step and breaking changes
- **New Servoy requirements**: Add to Phase 4 (integration) or create new phase
- **New deprecations**: Add to Phase 1 or Phase 2 removal lists

Current target versions (update these when platform moves forward):
- Angular: 22.1.x
- TypeScript: 6.0.x
- ESLint: 10.x with flat config
- Vitest: 4.x
- @servoy/public: 2026.9.x
