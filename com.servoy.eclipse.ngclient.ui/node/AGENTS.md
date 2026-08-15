# Agent Guidelines for Servoy TiNG (NG Client UI)

This is the **Servoy TiNG** runtime — the Angular-based NG Client UI that powers the Servoy application runtime in the browser. It is a large Angular workspace with one application and multiple library sub-projects.

---

## 1. Project Overview

| Aspect | Value |
|--------|-------|
| Name | TiNG |
| Version | 2026.9.0 |
| Framework | Angular 22 |
| Language | TypeScript 6.0 |
| Build tool | Angular CLI (`@angular/build:application`, esbuild-based) |
| Test framework | Vitest 4.x (with jsdom environment) |
| Linter | ESLint 10 (`@angular-eslint`) |
| Package manager | npm (with `legacy-peer-deps=true`) |
| License | GNU Affero General Public License |

## 2. Workspace Structure

### Application

| Project | Type | Source |
|---------|------|--------|
| `ngclient2` | application | `src/` |

### Libraries

| Project | Package | Source |
|---------|---------|--------|
| `@servoy/public` | Core APIs, types, services | `projects/servoy-public/` |
| `@servoy/servoydefault` | Default UI components | `projects/servoydefault/` |
| `@servoy/dialogs` | Dialog components | `projects/dialogs/` |
| `@servoy/window` | Window service | `projects/window/` |
| `@servoy/ngclientutils` | Client utility services | `projects/ngclientutils/` |

### Architecture Layers (in `src/`)

| Layer | Path | Purpose |
|-------|------|---------|
| Sablo | `src/sablo/` | WebSocket communication, type converters, service registry |
| NG Client | `src/ngclient/` | Runtime services, form management, data converters |
| Servoy Core | `src/servoycore/` | Core Servoy components (formcontainer, navigator, etc.) |
| Designer | `src/designer/` | Form designer integration (embedded in Eclipse) |
| App | `src/app/` | Bootstrap, routing, root module |

### Key Files

| File | Purpose |
|------|---------|
| `package.json` | Dependencies and scripts |
| `angular.json` | Workspace and build configuration |
| `tsconfig.json` | Root TypeScript config |
| `eslint.config.js` | Linting rules (flat config) |
| `karma.conf.js` | Base test runner config |
| `karma.dev.conf.js` | Dev test config (all browsers) |
| `karma.dev.once.conf.js` | Single-run test config (CI) |

---

## 3. Build Commands

| Task | Command |
|------|---------|
| Quick typecheck (no output) | `npx tsc --noEmit -p src/tsconfig.app.json` |
| Typecheck library | `npx tsc --noEmit -p projects/servoy-public/tsconfig.lib.json` |
| Lint | `npm run lint` |
| Build library (required first) | `npm run build_lib` |
| Build application (production) | `npm run build` |
| Build application (debug/watch) | `npm run build_debug` |
| Build library (debug/watch) | `npm run build_lib_debug` |
| Serve locally | `npm start` |

The `@servoy/public` library must be built before the application because the app depends on it via `"@servoy/public": "file:dist-public"`.

**Validation workflow after code changes:**
1. `npx tsc --noEmit -p src/tsconfig.app.json` — fast TypeScript validation
2. `npx ng lint` — ESLint check
3. `npx ng build ngclient2 --configuration development` — full build (only at end)

---

## 4. Testing

### Framework

- **Vitest** 4.x as test runner (with jsdom environment)
- **Angular TestBed** for component/service testing
- **`@angular/build:unit-test`** builder (AOT-compiled tests)

### Running Tests

| Task | Command |
|------|---------|
| All tests (CI, single-run) | `run_tests.bat` or `npm run test` |
| `ngclient2` app (watch) | `npm run test:watch` |
| `ngclient2` app (Vitest UI) | `npm run test:ui` |
| `@servoy/public` library | `npm run test_public` |
| `@servoy/servoydefault` library | `npm run test_default` |
| `@servoy/dialogs` library | `npm run test_dialogs` |
| `@servoy/window` library | `npm run test_window` |
| `@servoy/ngclientutils` library | `npm run test_ngclientutils` |
| Specific spec file | `npx ng test --include="**/my-component.spec.ts" --no-watch` |

### Test File Location

Tests live next to the source file they test:
- `my-component.component.ts` → `my-component.component.spec.ts`
- `my.service.ts` → `my.service.spec.ts`

### Test Conventions

- Import test functions explicitly: `import { describe, it, expect, beforeEach, vi } from 'vitest';`
- Use `describe` blocks to group tests by component/service
- Use nested `describe` for specific scenarios
- Mock dependencies with `vi.fn()` and manual mock objects (NOT `jasmine.createSpyObj`)
- Use `vi.spyOn(obj, 'method')` to spy on methods
- Use `vi.useFakeTimers()` / `vi.advanceTimersByTime(ms)` / `vi.useRealTimers()` for timer tests (NOT `fakeAsync`/`tick`)
- Use `async`/`await` for async tests (NOT `waitForAsync`)
- Each spec builds its own TestBed module with explicit `declarations` of all directives/pipes needed
- Do NOT use `NO_ERRORS_SCHEMA` — always declare what the template needs
- Do NOT import `ServoyPublicTestingModule` or `ServoyDefaultComponentsModule` — declare directives individually
- Use `fixture.detectChanges()` to trigger Angular change detection
- Verify no runtime errors (like NG0600) by asserting `detectChanges()` doesn't throw

### Critical: Global Mocking Rules

- **NEVER** use `vi.stubGlobal('document', ...)` or `vi.stubGlobal('window', ...)` — this replaces the entire jsdom DOM and breaks ALL subsequent tests in the same fork/thread. The error manifests as `this.doc.querySelector is not a function` in Angular's renderer.
- Instead, mock individual methods and restore them:
  ```typescript
  let originalMethod: typeof document.elementFromPoint;
  beforeEach(() => {
    originalMethod = document.elementFromPoint;
    document.elementFromPoint = vi.fn() as any;
  });
  afterEach(() => {
    document.elementFromPoint = originalMethod;
  });
  ```
- Similarly, never replace `window.location`, `window.navigator` etc. via `stubGlobal` — use `vi.spyOn` or direct property assignment with restore.
- The `vitest-setup.ts` contains a diagnostic that detects a corrupted document and logs details. If you see `[vitest-setup] FATAL: document.querySelector is not a function` in CI, look for `vi.stubGlobal('document', ...)` in recently added/modified spec files.

### Debugging: Log First, Fix Later

When facing unclear test failures (locally or on CI), **do NOT spend multiple rounds guessing root causes**. Instead:

1. **Add diagnostic logging immediately** — log the state of the failing object (e.g. `typeof`, `constructor.name`, `Object.keys()`, `JSON.stringify`) at the point of failure
2. **Run (or push and let CI run)** — get real data from the actual environment
3. **Fix based on evidence** — one log statement that shows actual state is worth more than three speculative fixes

---

## 5. Linting

```bash
npm run lint
```

ESLint configuration (`eslint.config.js` — flat config, ESLint 10):
- All errors are downgraded to warnings via `eslint-plugin-only-warn`
- Single quotes, prefer arrow functions
- Component selectors: `servoydefault-`, `servoycore-`, `svy-`, `testcomponents-` (kebab-case)
- Directive selectors: same prefixes (camelCase)
- Max line length: 200
- Type-aware linting enabled (uses `project: './tsconfig.json'`)
- Template accessibility rules enabled (`angular.configs.templateAccessibility`)

---

## 6. Code Conventions

### Angular CLI MCP Tools

Use these tools for Angular-specific guidance:
- **`angular-cli_get_best_practices`** — official Angular coding standards for our version (Angular 22)
- **`angular-cli_find_examples`** — find modern code examples (signals, control flow, deferrable views, etc.)
- **`angular-cli_search_documentation`** — search angular.dev docs for API details
- **`angular-cli_list_projects`** — get workspace project info (names, types, test framework)

Consult `get_best_practices` before writing new components or refactoring existing ones.
Use `find_examples` when working with newer Angular features (signal inputs, model inputs, etc.).

### General Rules

- Follow existing patterns in neighboring files — consistency over personal preference
- Use Angular signals (`signal`, `computed`, `effect`) for reactive state in new code
- Never write to signals during template rendering (causes NG0600)
- Use `readonly` for signal properties
- No comments unless explicitly asked
- Prefer `inject()` function over constructor injection in new code (check what the file uses)
- All components are `standalone: true` with their own `imports` array
- Follow the existing import style (barrel imports from `@servoy/public`, relative imports within same module)

### Angular-specific rules

- **No signal writes in getters/methods called from templates** — use `computed` signals instead
- **Change detection:** App uses `provideZonelessChangeDetection()`. Components use `OnPush` (default in Angular 22). Use signals or `markForCheck()` to notify Angular of state changes in async callbacks (setTimeout, Promises, WebSocket messages).
- **RxJS:** unsubscribe in `ngOnDestroy` or use `takeUntilDestroyed()` / `async` pipe
- **Template syntax:** use `@if`, `@for`, `@switch` (new control flow) — all templates have been migrated

---

## 7. Commit Message Convention

Any Git commit with AI-generated changes must follow:
- Subject line must end with ` [ai]`
- If related to a Jira case, include the case number (e.g. `SVY-123`, `SVYX-456`)
- Subject under 100 characters

Example: `SVY-21234 fix NG0600 signal write in file-upload-window [ai]`

---

## 8. Spec / Design Documents

Feature specs live in `docs/` at the repository root (one level above this project):
- `../../docs/<KEY>-<slug>.spec.md`
- Name files after the Jira case with a `.spec.md` extension

---

## 9. Dependencies

### Adding Dependencies

- Check `package.json` before assuming a library is available
- Use `npm install --legacy-peer-deps` (`.npmrc` enforces this)
- Prefer existing libraries already in the project over introducing new ones
- Key libraries already available: `lodash-es`, `luxon`, `numbro`, `bignumber.js`, `ag-grid-angular`

### Library Dependency Chain

```
@servoy/public (core APIs, types, base classes)
    ↓
@servoy/servoydefault, @servoy/dialogs, @servoy/window, @servoy/ngclientutils
    ↓
ngclient2 (main application)
```

---

## 10. Gotchas

- **NG0600 — Writing to signals during rendering:** Never call `signal.set()` or `signal.update()` from a method/getter that is called in a template interpolation. Use `computed()` instead.
- **`@servoy/public` is a local file dependency:** It must be built (`npm run build_lib`) before the app can compile. Changes to `projects/servoy-public/` require a library rebuild.
- **esbuild platform mismatch:** If `node_modules` was copied from another architecture, run `npm ci` to reinstall native binaries.
- **Karma browser:** If Chrome is not available, use `npm run test_edge` / `test_edge_nowatch` or set `CHROME_BIN` to an alternative Chromium-based browser (Edge, Chromium).
- **`legacy-peer-deps=true`:** Required due to Angular 22 peer dependency conflicts. Always use this flag when installing.
- **Zone.js:** The app uses `provideZonelessChangeDetection()`. Zone.js is still in polyfills as a safety net during migration but does NOT trigger change detection. Use signals or `markForCheck()` to notify Angular of async state changes (setTimeout, Promises). See `docs/zoneless-migration.spec.md`.
- **SVG as text:** SVG files are loaded as text strings (configured in `angular.json` loader section). Import them as strings, not as image URLs.

---

## 11. Angular 22 Modernization Status

Full spec: `../../docs/angular22-modernization-remaining.spec.md`

### Completed
- **Template control flow:** 100% — all templates use `@if`/`@for`/`@switch`
- **Karma removal:** 100% — Vitest 4 with jsdom, forks pool
- **Polyfills cleanup:** 100% — only zone.js remains
- **inject() migration:** Done for servoydefault (29 files) and src/ (8 files). Remaining are plain classes (not Angular DI).
- **takeUntilDestroyed():** Done — tooltip-html.directive, form_component.component
- **Signal queries (local):** Done — basechoice, baselabel, check, radio, spinner, calendar, combobox, typeahead, bg_splitter
- **Signal queries (basecomponent.ts):** Done — `elementRef` is now `viewChild<ElementRef<T>>('element')` signal query
- **Standalone components:** 100% — all directives, pipes, and components converted to `standalone: true`
- **bootstrapApplication():** Done — `AppModule` removed, app bootstraps with `bootstrapApplication()` + `provideRouter()`
- **Routing modules removed:** `AppRoutingModule`, `MainRoutingModule`, `ServoyDesignerRoutingModule` replaced with plain `Routes` arrays
- **AbstractFormComponent extraction:** Extracted to own file (`abstract_form_component.component.ts`), injected via `forwardRef` providers pattern

### Architecturally Blocked (requires full redesign)
- **@Input()/@Output() → signals:** Tied to `ngOnChanges` → `svyOnChanges(SimpleChanges)` pattern. Requires replacing entire change detection model with `effect()`/`computed()`.

**Note:** Angular 22 signal inputs DO still trigger `ngOnChanges` (backward compatible). So migrating `@Input()` to `input()` does NOT break the `svyOnChanges` pattern. The migration to `effect()` is optional for cleaner code but not required for correctness.
- **basetabpanel.ts `templateRef`:** Used in templates of subclasses (accordion, tabpanel, tablesspanel, splitpane).
- **servoydesigner.component.ts:** Setter-based ViewChild for `#element` (ResizeObserver). `mainForm` converted to signal.

### Remaining Phases
- **Phase 6b — NgModule removal:** Remove remaining barrel NgModules (servoycore.module, servoydefault.module, servoy_public.module, servoy.module, servoydesigner.module, lfc.module, allcomponents.module, dialog.module, windowservice.module). These currently serve only as re-export groupings; all components already have their own imports.
- **Phase 7 — Zoneless (IN PROGRESS):** `provideZonelessChangeDetection()` is active. Zone.js still loaded as safety net. Remaining work: fix ~34 setTimeout/setInterval patterns that mutate state without `markForCheck()`, verify third-party library compatibility, then remove zone.js from polyfills. See `docs/zoneless-migration.spec.md`.
