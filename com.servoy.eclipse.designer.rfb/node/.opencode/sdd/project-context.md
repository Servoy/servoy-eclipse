# Project Context â€” Servoy Designer RFB (Angular Frontend)

## SDD variant

This sub-project uses the **sdd-angular** shared skill (Angular pipeline).

This project is the **Servoy Form Designer** frontend â€” an Angular single-page application
embedded inside the Eclipse-based Servoy Developer IDE. It provides the visual drag-and-drop
form editor for designing Servoy forms.

## Technology stack

| Aspect | Value |
|--------|-------|
| Framework | Angular 22 (NgModule-based, NOT standalone components) |
| Language | TypeScript 5.9 |
| Build system | Angular CLI with `@angular/build:application` (esbuild) |
| CSS framework | Bootstrap 5.3 |
| Component library | ng-bootstrap 20 |
| Drag & drop | Angular CDK `DragDropModule` |
| Icons | Font Awesome 7 |
| Test framework | Vitest 4 (Angular TestBed) |
| Linting | ESLint 9 with `@angular-eslint` + `@typescript-eslint` |
| Package manager | npm (with `legacy-peer-deps=true`) |
| Node version | Bundled via `com.servoy.eclipse.nodejs.*` plugins |

## Architecture overview

The designer is embedded in Eclipse via the RFB (Remote Form Builder) mechanism:
- Built output goes to `../src/rfb/angular2` (Java plugin's classpath resources)
- Served by Eclipse's embedded Chromium browser
- Communicates with Eclipse Java backend via WebSocket (Sablo framework)
- The actual form being designed is loaded in an iframe

### Communication with Eclipse backend

The `EditorSessionService` manages a WebSocket connection to the Eclipse `formeditor`
service (via Sablo). Key operations:
- `createComponent` / `createComponents` â€” add components to form
- `sendChanges` â€” update component properties (position, size)
- `setSelection` â€” sync selection state with Eclipse
- `keyPressed` â€” forward keyboard events to Eclipse handlers
- `executeAction` â€” run named actions (z-order, alignment, reload)
- `openElementWizard` â€” open Eclipse dialogs

### Cross-project dependencies (tsconfig path aliases)

| Alias | Maps to | Purpose |
|-------|---------|---------|
| `@servoy/sablo` | `../../com.servoy.eclipse.ngclient.ui/node/src/sablo/public-api` | WebSocket/session framework |
| `@servoy/public` | `../../com.servoy.eclipse.ngclient.ui/node/projects/servoy-public/src/public-api` | Shared Servoy utilities |
| `@servoy/designer` | `../../com.servoy.eclipse.ngclient.ui/node/src/designer/public-api` | Designer-specific shared code |

## Source structure

All application code lives under `src/designer/`:

| Directory | Purpose |
|-----------|---------|
| `anchoringindicator/` | Visual indicator for CSS anchoring state |
| `autoscroll/` | Auto-scrolling when dragging near edges |
| `contextmenu/` | Right-click context menu for form elements |
| `directives/` | Shared directives (resize knobs, keyboard layout) |
| `dragselection/` | Drag-and-drop for absolute/CSS-position layouts |
| `dragselection-responsive/` | Drag-and-drop for responsive layouts |
| `dynamicguides/` | Alignment/snap guides during drag/resize |
| `editorcontent/` | Iframe-based content area rendering the form |
| `ghostscontainer/` | "Ghost" elements for hidden/non-visual parts |
| `highlight/` | Component highlight overlay on hover |
| `inlinedit/` | Inline text editing of component properties |
| `mouseselection/` | Selection logic (lasso, click, resize knobs) |
| `palette/` | Component palette panel (drag-to-drop) |
| `resizeeditorheight/` | Handle to resize editor canvas height |
| `resizeeditorwidth/` | Handle to resize editor canvas width |
| `resizer/` | Main resizer panel/wrapper |
| `samesizeindicator/` | Indicator for same-width/height components |
| `services/` | Core application services |
| `statusbar/` | Bottom status bar |
| `toolbar/` | Top toolbar with buttons, spinners, switches |
| `variantscontent/` | Style variant content area |
| `variantspreview/` | Style variant preview panel |

## Module structure

Single NgModule (`DesignerModule`) bootstrapping `DesignerComponent`:
- All components use `standalone: false`
- Root component selector: `<app-designer>`
- Imported modules: `BrowserModule`, `ServoyPublicModule`, `FormsModule`, `CommonModule`, `NgbModule`, `DragDropModule`

## Services

| Service | Role |
|---------|------|
| `EditorSessionService` | Core â€” WebSocket session, selection state, palette data, form actions |
| `EditorContentService` | DOM abstraction for iframe content, glasspane, cross-frame messaging |
| `URLParserService` | Parses designer URL query params (form name, solution, layout type) |
| `DesignSizeService` | Manages responsive design size presets (desktop/tablet/phone) |
| `DesignerUtilsService` | Utilities for drop targets, coordinate conversion, container detection |
| `DynamicGuidesService` | Dynamic snap-to alignment guides during drag/resize |

## Component hierarchy

```
app-designer
â”œâ”€â”€ designer-toolbar
â”œâ”€â”€ fill-area
â”‚   â”œâ”€â”€ designer-variantspreview
â”‚   â”œâ”€â”€ designer-palette
â”‚   â”œâ”€â”€ designer-resizer
â”‚   â”œâ”€â”€ content-area
â”‚   â”‚   â”œâ”€â”€ contentframe-overlay (glasspane)
â”‚   â”‚   â”‚   â”œâ”€â”€ selection-decorators (MouseSelectionComponent)
â”‚   â”‚   â”‚   â”œâ”€â”€ designer-highlight
â”‚   â”‚   â”‚   â”œâ”€â”€ designer-ghostscontainer
â”‚   â”‚   â”‚   â”œâ”€â”€ dragselection (absolute layout)
â”‚   â”‚   â”‚   â”œâ”€â”€ dragselection-responsive
â”‚   â”‚   â”‚   â”œâ”€â”€ designer-samesize-indicator
â”‚   â”‚   â”‚   â”œâ”€â”€ designer-anchoring-indicator
â”‚   â”‚   â”‚   â””â”€â”€ dynamic-guides (absolute layout)
â”‚   â”‚   â””â”€â”€ designer-editorcontent (iframe)
â”‚   â””â”€â”€ designer-autoscroll [top/bottom/left/right]
â”œâ”€â”€ designer-status-bar
â”œâ”€â”€ designer-contextmenu
â””â”€â”€ designer-inline-edit
```

## Build & scripts

| Script | Command | Purpose |
|--------|---------|---------|
| `npm start` | `ng serve` | Dev server (localhost:4200) |
| `npm run build` | `ng build --configuration production` | Production build |
| `npm run build_debug` | `ng build --watch` | Debug build with watch |
| `npm test` | `ng test` | Run unit tests (Vitest, no watch) |
| `npm run lint` | `ng lint` | Run ESLint |

## Code conventions

- Component selector prefix: `designer` or `app`, kebab-case
- Directive selector prefix: `designer` or `app`, camelCase
- Single quotes enforced (`@stylistic/ts/quotes`)
- Arrow functions preferred (`eslint-plugin-prefer-arrow`)
- 2-space indentation
- No unused imports
- Follow existing patterns in neighboring files â€” consistency over preference
- No `console.log` in production code â€” remove after debugging
- Use RxJS operators for async data flows
- Use Angular CDK for drag-and-drop, not custom implementations

## Gotchas

- **Cross-project imports:** The `@servoy/sablo`, `@servoy/public`, and `@servoy/designer`
  aliases resolve to `../../com.servoy.eclipse.ngclient.ui/node/`. If those files don't
  exist locally, the build will fail. Make sure that project is checked out.

- **Build output location:** Production build outputs to `../src/rfb/angular2` â€” this is
  the Java plugin's resource folder. Don't change the `outputPath` in angular.json.

- **Not standalone components:** This project uses NgModule-based architecture. All
  components must be declared in `DesignerModule` with `standalone: false`. Do NOT
  create standalone components.

- **iframe communication:** The designer content (the actual form) runs in an iframe.
  Cross-frame communication uses `EditorContentService`. Never directly access
  `document` for the iframe content â€” always go through the service.

- **WebSocket state:** The `EditorSessionService` manages the Sablo WebSocket session.
  All commands to the Eclipse backend go through this service. Never create direct
  HTTP connections to the backend.

- **Layout types:** The designer handles both absolute-position layouts and responsive
  layouts. Code that handles drag/resize often has separate paths for each layout type.
  Check `URLParserService.isAbsoluteFormLayout()` to determine the current mode.

- **Glasspane pattern:** The designer uses a transparent overlay (glasspane) on top of
  the iframe for mouse interactions. Selection, drag, resize all happen on this overlay,
  not on the actual form elements.

- **legacy-peer-deps:** The `.npmrc` has `legacy-peer-deps=true`. This is needed because
  some dependencies have peer dependency conflicts. Always use `npm install --legacy-peer-deps`.
