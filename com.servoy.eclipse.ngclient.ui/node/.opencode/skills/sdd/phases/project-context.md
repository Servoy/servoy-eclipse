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

## Sablo WebSocket architecture (server ↔ client)

The Servoy runtime uses **Sablo** as its communication layer between Java (server)
and Angular (client). Understanding this flow is essential for debugging service
API issues.

### How server-side Java calls a client-side service API

```
Java (server)                          Angular (client)
─────────────────────────────────────────────────────────────
1. BaseWindow.sendSyncMessage()
   → sends JSON via WebSocket
     with `smsgid` (expects response)
                                       2. WebsocketService receives message
                                          → sees `smsgid` → knows server is waiting
                                       3. ServicesService.callServiceApi()
                                          → dispatches to the plugin service
                                          → e.g. DialogsService.showQuestionDialog()
                                       4. Return value (or Promise) is captured
                                          → Promise.resolve(returnValue).then(...)
                                          → sends response back with same `smsgid`
5. BaseWindow.waitResponse() unblocks
   → server code continues with result
```

### Key files

| File | Role |
|------|------|
| `src/sablo/websocket.service.ts` | WebSocket message handling, dispatches incoming calls |
| `src/sablo/services.service.ts` | `callServiceApi()` — dispatches to service instances, handles return values |
| Server: `BaseWindow.java` | `sendSyncMessage()` / `waitResponse()` — blocking call from Java |
| Server: `ClientSideTypeCache.java` | Sends client-side spec info (type converters, `shouldReturnValue` flag) |

### Important concepts

- **`smsgid`** — Server Message ID. When present in a message, the client MUST send
  a response. The server thread blocks until the response arrives.
- **`shouldReturnValue`** — Flag in the client-side spec that tells
  `callServiceApi()` to await the service function's return value (including Promises)
  before responding to the server. Without this flag, fire-and-forget calls return
  `undefined` immediately.
- **`waitForLoading()`** — Deferred pattern in `handleNormalServiceApis` that queues
  incoming API calls until the client-side service is fully loaded (introduced SVY-19700).
- **Service specs** — `.spec` files on the server define API functions, their parameters,
  return types, and whether they need type conversion. `ClientSideTypeCache` serializes
  relevant parts to the client.

### Common pitfalls

- If `serviceCallSpec` is `undefined` (spec not yet loaded or no type info needed),
  the client must still propagate the return value — otherwise the server unblocks
  immediately with `undefined`.
- Async service functions (like dialogs) return a **Promise**. The websocket layer
  uses `Promise.resolve(returnValue).then(...)` to handle both sync and async returns.
- Pre-login calls can hit timing issues where specs haven't loaded yet — the
  `|| serviceCallSpec === undefined` fallback ensures return values are still propagated.

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
