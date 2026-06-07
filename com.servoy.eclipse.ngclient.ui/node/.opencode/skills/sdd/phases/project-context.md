# Project Context — Servoy TiNG (NG Client UI)

This project is the **Servoy TiNG** runtime — the Angular-based NG Client UI
that powers the Servoy application runtime in the browser. It is a large Angular
workspace with one application and multiple library sub-projects.

## Technology stack

| Aspect | Value |
|--------|-------|
| Framework | Angular 21 |
| Language | TypeScript 5.9 |
| Build tool | Angular CLI (`@angular/build:application`, esbuild-based) |
| Test framework | Jasmine + Karma |
| Linter | ESLint 9 (`@angular-eslint`) |
| Package manager | npm (with `legacy-peer-deps=true`) |
| Version | 2026.9.0 |

## Architecture

The application has a layered architecture:

```
src/sablo/         → WebSocket communication, type converters, service registry
src/ngclient/      → Runtime services, form management, data converters
src/servoycore/    → Core Servoy components (formcontainer, navigator, etc.)
src/designer/      → Form designer integration (embedded in Eclipse)
src/app/           → Bootstrap, routing, root module
```

### Library sub-projects

```
@servoy/public (core APIs, types, base classes)
    ↓
@servoy/servoydefault, @servoy/dialogs, @servoy/window, @servoy/ngclientutils
    ↓
ngclient2 (main application)
```

| Library | Source | Purpose |
|---------|--------|---------|
| `@servoy/public` | `projects/servoy-public/` | Core APIs, types, services, base classes |
| `@servoy/servoydefault` | `projects/servoydefault/` | Default UI components |
| `@servoy/dialogs` | `projects/dialogs/` | Dialog components |
| `@servoy/window` | `projects/window/` | Window service |
| `@servoy/ngclientutils` | `projects/ngclientutils/` | Client utility services |

## Angular development essentials

When writing code for this project, you are writing an **Angular application**:

### Signals & reactivity
- Use Angular signals (`signal`, `computed`, `effect`) for reactive state in new code
- **NEVER** write to signals during template rendering (causes NG0600)
- Use `computed()` for derived values that are used in templates
- Use `readonly` for signal properties

### Components
- Use `standalone: false` for components in existing NgModules
- Prefer `inject()` function over constructor injection in new code (check what the file uses)
- Follow the existing import style (barrel imports from `@servoy/public`, relative imports within same module)
- Component selectors: `servoydefault-`, `servoycore-`, `svy-`, `testcomponents-` (kebab-case)

### Change detection
- Components use default strategy; avoid manual `ChangeDetectorRef` unless necessary
- The app uses Zone.js — don't introduce zoneless patterns

### RxJS
- Unsubscribe in `ngOnDestroy` or use `takeUntilDestroyed()` / `async` pipe
- Never leave dangling subscriptions

### Template syntax
- Use `@if`, `@for`, `@switch` (new control flow) in files that already use it
- Use `*ngIf`/`*ngFor` in files that use the old syntax

## Dependencies

- Check `package.json` before assuming a library is available
- Use `npm install --legacy-peer-deps` (`.npmrc` enforces this)
- Prefer existing libraries over introducing new ones
- Key libraries: `lodash-es`, `luxon`, `numbro`, `bignumber.js`, `ag-grid-angular`

## Build

| Task | Command |
|------|---------|
| Build library (required first) | `npm run build_lib` |
| Build application (production) | `npm run build` |
| Build application (debug/watch) | `npm run build_debug` |
| Serve locally | `npm start` |

**Important:** `@servoy/public` must be built before the app (`npm run build_lib`).

## Testing

- **Jasmine** for test authoring (`describe`/`it`/`expect`)
- **Karma** as test runner
- **Angular TestBed** for component/service testing
- Tests live next to their source: `my.component.ts` → `my.component.spec.ts`
- Run specific test: `npx ng test --include="**/file.spec.ts" --watch=false --browsers=ChromeHeadless`
- If Chrome is not installed, use `npm run test_edge` / `test_edge_nowatch` or set `CHROME_BIN`:
  ```powershell
  # Windows (Edge)
  $env:CHROME_BIN = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
  # macOS (Edge)
  export CHROME_BIN="/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"
  # Linux (Chromium)
  export CHROME_BIN=$(which chromium-browser)
  ```

## AGENTS.md

Always read `AGENTS.md` at the start of your work — it contains the full tool usage
policy, workflow requirements, testing commands, and code conventions.

## Gotchas

Things that will trip you up if you don't know them:

- **NG0600 — Writing to signals during rendering:** Never call `signal.set()` or
  `signal.update()` from a method/getter called in a template interpolation.
  Use `computed()` instead.

- **`@servoy/public` is a local file dependency:** It must be built (`npm run build_lib`)
  before the app can compile. Changes to `projects/servoy-public/` require a library rebuild.

- **esbuild platform mismatch:** If `node_modules` was copied from another architecture,
  run `npm ci` to reinstall native binaries.

- **Karma browser:** If Chrome is not available, use `npm run test_edge` / `test_edge_nowatch`
  or set `CHROME_BIN` to an alternative Chromium-based browser (Edge, Chromium).

- **`legacy-peer-deps=true`:** Required due to Angular 21 peer dependency conflicts.
  Always use this flag when installing.

- **Zone.js:** The app still uses Zone.js for change detection. Don't introduce
  zoneless patterns unless the project migrates.

- **SVG as text:** SVG files are loaded as text strings (configured in `angular.json`
  loader section). Import them as strings, not as image URLs.

- **No comments in code:** Do not add code comments unless explicitly asked.

- **Template rendering safety:** Never call methods that have side effects from
  Angular templates. Template expressions must be pure.
