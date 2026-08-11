# Angular 22 Modernization — Remaining Work

**Project:** TiNG (com.servoy.eclipse.ngclient.ui/node)  
**Date:** 2026-08-11  
**Status:** In Progress

---

## Summary

The Angular 22 upgrade is substantially done. Karma→Vitest migration is complete, ESLint flat config is in place, and signal inputs/outputs have been partially migrated. However, several modernization tracks remain incomplete.

---

## 1. Karma/Jasmine Leftovers

| Item | Status | Action |
|------|--------|--------|
| `karma.conf.js`, `karma.dev.conf.js`, `karma.dev.once.conf.js` | **GONE** ✅ | Fully removed |
| `@types/jasmine`, `karma-*` packages | **GONE** ✅ | Not in package.json |
| jasmine in source code | 1 comment remains | `formatting.service.spec.ts:51` — trivial TODO comment mentioning old Jasmine no-op |
| `.vscode/launch.json` | **Stale** ❌ | Still references "Debug karma tests" on port 9876; should be updated for Vitest |
| `dist/test-out/` folder | **Stale** ❌ | Contains old Karma/Jasmine build artifacts. In .gitignore (`/dist`) so not tracked, but should be deleted locally |

---

## 2. Dependencies — Cleanup Candidates

| Package | Status | Action |
|---------|--------|--------|
| `ts-node` (10.9.2) | **Unused** ❌ | Was needed for Karma TS config; Vitest doesn't need it. Remove. |
| `zone.js` (0.16.2) | **Still required** ⚠️ | App still uses Zone.js change detection. Keep for now; remove only when migrating to zoneless. |
| `@angular/platform-browser-dynamic` | **Review** ⚠️ | May not be needed with esbuild builder; check if bootstrapping still uses it. |
| `core-js` in `allowScripts` | **Stale** ❌ | core-js is not a direct dependency anymore; remove the `allowScripts` entry. |
| `ng2-dragula` reference in `polyfills.ts` | **Review** ⚠️ | `(window as any).global = window` workaround — check if ng2-dragula is still used (found 1 reference). |
| `(window as any).ICAL = {}` in `polyfills.ts` | **Review** ⚠️ | Unclear if still needed; investigate. |

---

## 3. NgModules → Standalone Migration

| Metric | Count |
|--------|-------|
| Total `.component.ts` files | 14 |
| Components with `standalone: false` | 26 (includes directives/pipes) |
| Components with `standalone: true` | 1 |
| `.module.ts` files | **15** |

### Module Files Still Present

**Libraries:**
- `projects/dialogs/src/lib/dialog.module.ts`
- `projects/servoy-public/src/lib/servoy_public.module.ts`
- `projects/servoy-public/src/lib/testing/publictesting.module.ts`
- `projects/servoydefault/src/lib/servoydefault.module.ts`
- `projects/window/src/lib/windowservice.module.ts`

**Application:**
- `src/app/app.module.ts`
- `src/app/app-routing.module.ts`
- `src/designer/servoydesigner.module.ts`
- `src/designer/servoydesigner-routing.module.ts`
- `src/ngclient/allcomponents.module.ts`
- `src/ngclient/lfc.module.ts`
- `src/ngclient/main-routing.module.ts`
- `src/ngclient/servoy.module.ts`
- `src/servoycore/servoycore.module.ts`
- `src/testing/servoytesting.module.ts`

**Action:** All 26 declarations still use `standalone: false`. Full standalone migration has NOT started. This is the biggest remaining modernization effort.

---

## 4. Signal Inputs/Outputs Migration

| API Style | Files using it |
|-----------|---------------|
| `input()` (signal) | 32 files ✅ |
| `@Input()` (decorator) | 5 files ⚠️ (architecturally blocked) |
| `output()` (signal) | 1 file |
| `model()` (signal) | 1 file (splitpane.divLocation) |
| `@Output()` (decorator) | 1 file ⚠️ (architecturally blocked) |

### Remaining `@Input()` files — ALL architecturally blocked:
- `projects/servoy-public/src/lib/basecomponent.ts` — base class for ALL components; `name`/`servoyApi` accessed as `this.name`/`this.servoyApi` in 100+ subclasses; tied to `ngOnChanges`
- `projects/servoydefault/src/lib/splitpane/splitpane.ts` — all props consumed via `svyOnChanges(changes: SimpleChanges)`, signal inputs don't trigger `ngOnChanges`
- `projects/servoydefault/src/lib/tabpanel/basetabpanel.ts` — same as splitpane + `tabIndex` mutated internally with two-way binding
- `src/designer/designform_component.component.ts` — setter-based `@Input() set containers(...)`, complex logic
- `src/ngclient/form/form_component.component.ts` — same pattern as designform

### Remaining `@Output()` files:
- `projects/servoydefault/src/lib/tabpanel/basetabpanel.ts` — `tabIndexChange` used for two-way binding; needs `model()` but `tabIndex` is also consumed in `svyOnChanges`

### Why these cannot be migrated now:
The entire `ServoyBaseComponent` architecture is built on `ngOnChanges(changes: SimpleChanges)` → `svyOnChanges()`. Signal inputs do NOT trigger `ngOnChanges`. Migrating requires:
1. Replacing the `svyOnChanges` pattern with `effect()` or `computed()`
2. Updating ALL subclasses that override `svyOnChanges`
3. Changing `this.name` → `this.name()` in 100+ files (for basecomponent)

**Decision:** Defer to Phase 6 (standalone migration) where the full component architecture will be redesigned together. False positives removed: `spectypes.service.ts` only has @Input/@Output in doc comments, `decimalkeyconverter.directive.spec.ts` is a test mock.

---

## 5. Dependency Injection — `inject()` vs Constructor

| Pattern | File count (non-spec, non-module) |
|---------|----------------------------------|
| Uses `inject()` | 43 |
| Uses constructor (no inject) | 79 |

**Action:** 79 files still use pure constructor injection. The `inject()` migration (commit `74eff86`) only covered a partial set. Remaining files should be migrated incrementally.

---

## 6. Template Control Flow

| Syntax | Template files using it |
|--------|------------------------|
| `@if` / `@for` / `@switch` (new) | 17 files ✅ |
| `*ngIf` / `*ngFor` (old) | **0 files** ✅ |

**Status: COMPLETE** ✅ — All templates use modern control flow.

---

## 7. ViewChild/ContentChild — Signal Queries

| Pattern | Count |
|---------|-------|
| `@ViewChild` / `@ContentChild` (decorator) | 24 files |
| `viewChild()` / `contentChild()` (signal) | 6 files |

**Action:** Migrate 24 files from decorator-based queries to signal queries.

---

## 8. Change Detection & Signals

| Metric | Count |
|--------|-------|
| Files with `ChangeDetectorRef` or `ChangeDetectionStrategy` | 56 |
| Files using `OnPush` | 41 |
| Files using `signal()` / `computed()` / `effect()` | 4 |
| Files with `ngOnDestroy` (manual cleanup) | 24 |
| Files with `takeUntilDestroyed()` / `DestroyRef` | 0 |
| Files with `.subscribe()` | 13 |

**Action:**
- 13 files use `.subscribe()` without `takeUntilDestroyed()` — potential memory leaks. Migrate to `takeUntilDestroyed()` or `toSignal()`.
- Only 4 files use reactive signals (`signal()`/`computed()`/`effect()`) — this is very low for Angular 22. Incremental adoption recommended.
- Zone.js is still in use; zoneless migration is a future milestone.

---

## 9. Polyfills File

`src/polyfills.ts` still exists with:
```typescript
import 'zone.js';                      // Required (Zone.js still active)
(window as any).ICAL = {};             // Unclear if needed
(window as any).global = window;       // ng2-dragula workaround
```

**Action:** Investigate if `ICAL` and `global` workarounds are still needed. If ng2-dragula is removed, the `global` hack can go.

---

## 10. Prioritized Roadmap (Optimale Volgorde)

> **Principe:** "van binnen naar buiten" — eerst alle per-component wijzigingen afronden,
> daarna pas de structurele module-laag weghalen. Zo raak je elke file maar één keer aan
> voor de standalone conversie, in plaats van bij elke stap opnieuw.

### Phase 1 — Cleanup (geen dependencies, low risk)
1. Update `.vscode/launch.json` — replace Karma debug config with Vitest debug config
2. Remove `ts-node` devDependency (was only needed for Karma TS config)
3. Remove `core-js` from `allowScripts` (not a direct dependency)
4. Delete `dist/test-out/` locally (old Karma build artifacts)
5. Remove jasmine TODO comment from `formatting.service.spec.ts`
6. Investigate and clean polyfills (`ICAL`, `global` workarounds)

### Phase 2 — Signal Inputs/Outputs — ⚠️ DEFERRED

> **Bevinding na analyse:** Alle resterende `@Input()` / `@Output()` usages zitten in de
> kern-architectuur (`ServoyBaseComponent`, `BaseTabpanel`, `AbstractFormComponent`). Ze zijn
> onlosmakelijk verbonden met het `ngOnChanges` → `svyOnChanges(changes: SimpleChanges)` patroon.
> Signal inputs triggeren GEEN `ngOnChanges`, dus migratie vereist een volledige herstructurering
> van het change detection model (naar `effect()` / `computed()`).
>
> **Besluit:** Samengevoegd met Phase 6 (standalone migration) waar de hele component-architectuur
> wordt herontworpen. Geen losse actie meer.

### Phase 3 — `inject()` Migration (medium risk, 79 files)
9. Migrate remaining 79 files from constructor injection → `inject()`

> **Waarom vóór standalone:** Bij standalone conversie reorganiseer je providers.
> Als constructors al leeg zijn (alle DI via `inject()` fields), is de standalone
> wijziging puur een decorator + imports change.
>
> **Waarom vóór takeUntilDestroyed:** `takeUntilDestroyed()` vereist `inject(DestroyRef)`.
> Als inject() nog niet gemigreerd is, zou je `DestroyRef` via constructor injecteren
> en dat later weer moeten omschrijven.

### Phase 4 — `takeUntilDestroyed()` + RxJS cleanup (low-medium risk, 13 files)
10. Replace manual `ngOnDestroy` + `unsubscribe()` with `takeUntilDestroyed()`
11. Where applicable, replace `.subscribe()` chains with `toSignal()` / `toObservable()`

> **Waarom nu:** `inject()` is klaar, dus `inject(DestroyRef)` werkt overal.
> Elimineert memory leak risico's en verwijdert boilerplate.

### Phase 5 — Signal Queries (medium risk, 24 files)
12. Migrate 24 `@ViewChild`/`@ContentChild` → `viewChild()` / `contentChild()` signal queries

> **Waarom hier:** Na input/output signals en inject() ben je klaar met alle
> per-component signal-migraties. Elke component is nu "signal-ready".

### Phase 6 — Standalone Migration (high risk, breaking, 15 modules → 0)
13. Convert leaf components/directives/pipes to `standalone: true` (remove from module declarations)
14. Convert container components and shared modules
15. Convert routing modules → standalone `provideRouter()` with route configs
16. Convert `app.module.ts` → `bootstrapApplication()` with providers
17. Remove all 15 `.module.ts` files

> **Waarom als laatste per-component stap:** Alle components hebben nu al signal inputs,
> inject(), signal queries. De standalone conversie is nu een schone one-pass operatie:
> alleen `standalone: true` + `imports: [...]` toevoegen per component.
>
> **Interne volgorde:** leaf → containers → routing → bootstrap. Zo breekt niets tussentijds.

### Phase 7 — Zoneless (high risk, apart project, toekomst)
18. Remove `zone.js` dependency
19. Switch to `provideZonelessChangeDetection()`
20. Remove/replace all `ChangeDetectorRef.detectChanges()` / `markForCheck()` calls
21. Ensure all components use signal-based reactivity (computed, effect)
22. Performance testing en validatie

> **Waarom apart:** Zoneless vereist dat standalone klaar is, signals overal zitten,
> en alle ChangeDetectorRef-gebruik geëlimineerd is. Dit is een eigen epic.

---

## 11. Metrics Dashboard

| Category | Done | Remaining | % Complete | Notes |
|----------|------|-----------|------------|-------|
| Karma removal | ✅ | 0 | **100%** | Phase 1 complete |
| Template control flow | ✅ | 0 | **100%** | |
| Polyfills cleanup | ✅ | 0 | **100%** | Phase 1 complete |
| Signal inputs | 32 files | 5 files (blocked) | 86% | Requires svyOnChanges redesign |
| Signal outputs | 1 file | 1 file (blocked) | 50% | Requires svyOnChanges redesign |
| inject() migration | 43 files | 79 files | 35% | Next actionable phase |
| Signal queries | 6 files | 24 files | 20% | |
| Standalone components | 1 | 26 | 4% | |
| NgModule removal | 0 | 15 | 0% | |
| Reactive signals usage | 4 files | — | Low | |
| takeUntilDestroyed | 0 | 13 | 0% | |
| Zoneless | — | — | Not started | |
