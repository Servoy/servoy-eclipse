# Agent Guidelines for Servoy TiNG (NG Client UI)

This is the **Servoy TiNG** runtime — the Angular-based NG Client UI that powers the Servoy application runtime in the browser. It is a large Angular workspace with one application and multiple library sub-projects.

---

## 1. Project Overview

| Aspect | Value |
|--------|-------|
| Name | TiNG |
| Version | 2026.9.0 |
| Framework | Angular 21 |
| Language | TypeScript 5.9 |
| Build tool | Angular CLI (`@angular/build:application`, esbuild-based) |
| Test framework | Jasmine + Karma |
| Linter | ESLint 9 (`@angular-eslint`) |
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
| `.eslintrc.json` | Linting rules |
| `karma.conf.js` | Base test runner config |
| `karma.dev.conf.js` | Dev test config (all browsers) |
| `karma.dev.once.conf.js` | Single-run test config (CI) |

---

## 3. Build Commands

| Task | Command |
|------|---------|
| Build library (required first) | `npm run build_lib` |
| Build application (production) | `npm run build` |
| Build application (debug/watch) | `npm run build_debug` |
| Build library (debug/watch) | `npm run build_lib_debug` |
| Serve locally | `npm start` |

The `@servoy/public` library must be built before the application because the app depends on it via `"@servoy/public": "file:dist-public"`.

---

## 4. Testing

### Framework

- **Jasmine** for test authoring (describe/it/expect)
- **Karma** as test runner
- **Angular TestBed** for component/service testing

### Running Tests

| Task | Command |
|------|---------|
| All tests (CI, headless, single-run) | `run_tests.bat` or `npm run test_dev_all_nowatch` |
| All tests (headless Chrome) | `npm run test_headless` |
| `@servoy/public` library | `npm run test_public` |
| `@servoy/servoydefault` library | `npm run test_default` |
| `@servoy/dialogs` library | `npm run test_dialogs` |
| `@servoy/window` library | `npm run test_dev_window` |
| `@servoy/ngclientutils` library | `npm run test_dev_ngclientutils` |
| Specific spec file | `npx ng test --include="**/my-component.spec.ts" --watch=false --browsers=ChromeHeadless` |

### Browser Configuration

Tests default to **Chrome**. If Chrome is installed, everything works out of the box.

If Chrome is **not** available (common on some Windows machines), you have two options:

**Option A — Use the Edge test scripts:**
| Task | Command |
|------|---------|
| All tests (Edge, watch) | `npm run test_edge` |
| All tests (Edge, single-run) | `npm run test_edge_nowatch` |
| `@servoy/public` (Edge) | `npm run test_public_edge` |
| `@servoy/servoydefault` (Edge) | `npm run test_default_edge` |

**Option B — Set `CHROME_BIN` to an alternative Chromium-based browser:**
```powershell
# Windows (Edge)
$env:CHROME_BIN = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

# macOS (Edge)
export CHROME_BIN="/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"

# Linux (Chromium)
export CHROME_BIN=$(which chromium-browser)
```
After setting `CHROME_BIN`, the regular `test_headless` / `test_dev_all_nowatch` commands will use that browser.

### Test File Location

Tests live next to the source file they test:
- `my-component.component.ts` → `my-component.component.spec.ts`
- `my.service.ts` → `my.service.spec.ts`

### Test Conventions

- Use `describe` blocks to group tests by component/service
- Use nested `describe` for specific scenarios
- Use `beforeEach(waitForAsync(...))` for TestBed configuration
- Mock dependencies with `jasmine.createSpyObj`
- Use `fixture.detectChanges()` to trigger Angular change detection
- Verify no runtime errors (like NG0600) by asserting `detectChanges()` doesn't throw

---

## 5. Linting

```bash
npx ng lint
```

ESLint configuration (`.eslintrc.json`):
- Single quotes, no trailing semicolons optional (check existing code)
- Prefer arrow functions
- Component selectors: `servoydefault-`, `servoycore-`, `svy-`, `testcomponents-` (kebab-case)
- Directive selectors: same prefixes (camelCase)
- Max line length: 200

---

## 6. Code Conventions

- Follow existing patterns in neighboring files — consistency over personal preference
- Use Angular signals (`signal`, `computed`, `effect`) for reactive state in new code
- Never write to signals during template rendering (causes NG0600)
- Use `readonly` for signal properties
- No comments unless explicitly asked
- Prefer `inject()` function over constructor injection in new code (check what the file uses)
- Use `standalone: false` for components in existing NgModules (check the module)
- Follow the existing import style (barrel imports from `@servoy/public`, relative imports within same module)

### Angular-specific rules

- **No signal writes in getters/methods called from templates** — use `computed` signals instead
- **Change detection:** components use default strategy; avoid manual `ChangeDetectorRef` unless necessary
- **RxJS:** unsubscribe in `ngOnDestroy` or use `takeUntilDestroyed()` / `async` pipe
- **Template syntax:** use `@if`, `@for`, `@switch` (new control flow) in existing files that already use it; use `*ngIf`/`*ngFor` in files that use the old syntax

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
- **`legacy-peer-deps=true`:** Required due to Angular 21 peer dependency conflicts. Always use this flag when installing.
- **Zone.js:** The app still uses Zone.js for change detection. Don't introduce zoneless patterns unless the project migrates.
- **SVG as text:** SVG files are loaded as text strings (configured in `angular.json` loader section). Import them as strings, not as image URLs.
