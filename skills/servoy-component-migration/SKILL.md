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
10. **OnPush**: check for `ChangeDetectionStrategy.OnPush` in @Component decorators
11. **Zoneless**: grep for `ChangeDetectorRef`, `NgZone`, zone.js imports

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
1. Add `NG2-Components: Component1,Component2,...` listing all Angular component class names
2. Add `Entry-Point: projects/<package-name>` pointing to the Angular library
3. Ensure `NPM-PackageName: @scope/package` is set

The ComponentTemplateGenerator uses these to determine which packages to include in the generated template.

Commit: `convert to standalone components [ai]`

---

## Phase 5 — Signal Inputs & OnPush

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

### Spec property alignment check

After signal migration, verify:
- Every spec model property has a matching `input()` or `model()` signal, OR is tagged `"serveronly": true`
- No `size` or `location` in specs (handled by framework)
- Deprecated specs don't need Angular components

Commit: `migrate to signal inputs and OnPush [ai]`

---

## Phase 6 — Zoneless Readiness Check

This is a verification phase, not necessarily an implementation phase (zoneless requires the whole app to be ready, not just one package).

Check for:
1. No direct `NgZone` usage
2. No `ChangeDetectorRef.detectChanges()` calls (use signals instead)
3. No `setTimeout`/`setInterval` that expects zone.js to trigger change detection
4. All state changes go through signals
5. All components are OnPush

Report findings — don't force zoneless if the runtime (Servoy TiNG) isn't ready yet.

---

## Phase 7 — Final Verification

1. `npm run build` — clean production build
2. `npx ng lint` — zero warnings (or only accepted ones)
3. `npm run test` — all tests pass
4. Spec alignment scan — no properties without @Input/serveronly
5. MANIFEST.MF has correct NG2-Components and Entry-Point

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
