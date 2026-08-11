# AGENTS.md - Servoy Designer RFB (Angular Frontend)

## Project Overview

This is the **Servoy Form Designer** frontend — an Angular SPA embedded in the Eclipse-based Servoy Developer IDE. It provides the visual drag-and-drop form editor.

- **Name:** `servoy-designer`
- **Version:** 2026.9.0
- **Framework:** Angular 22.1 (NgModule-based)
- **Language:** TypeScript 6.0
- **Build:** Angular CLI with `@angular/build:application` (esbuild)

## Commands

| Command | Purpose |
|---------|---------|
| `npm run lint` | Run ESLint — must pass with zero errors |
| `npm run build_debug_nowatch` | Verify build compiles |
| `npm test` | Run Vitest unit tests (jsdom, no watch) |
| `npm run test:watch` | Run Vitest in watch mode |
| `npm run test:browser` | Run browser integration tests (Chromium via Playwright) |
| `npm start` | Dev server on localhost:4200 |

## Prerequisites (first time / after lib changes)

Before `npm install` works, the shared libraries must be built:

```bash
cd ../../com.servoy.eclipse.ngclient.ui/node && npm run build_libs
```

This builds `@servoy/public`, `@servoy/sablo`, `@servoy/designer` as tgz packages. Only needed when their source changes. On Jenkins/Maven this is handled automatically.

## After Every Code Change

1. Run `npm run lint` — fix all errors
2. Run `npm run build_debug_nowatch` — verify compilation
3. If tests exist for the changed code, run `npm test`

## Project Structure

All source lives under `src/designer/`:

| Directory | Purpose |
|-----------|---------|
| `services/` | Core services (EditorSession, EditorContent, URLParser, DesignSize, DesignerUtils, DynamicGuides) |
| `dragselection/` | Drag-and-drop for absolute layout |
| `dragselection-responsive/` | Drag-and-drop for responsive layout |
| `dynamicguides/` | Alignment snap guides |
| `mouseselection/` | Selection logic (lasso, click, resize) |
| `palette/` | Component palette panel |
| `toolbar/` | Top toolbar |
| `editorcontent/` | Iframe content area |
| `ghostscontainer/` | Ghost elements |
| `highlight/` | Hover highlight |
| `contextmenu/` | Right-click menu |
| `inlinedit/` | Inline property editing |
| `statusbar/` | Status bar |

## Code Conventions

- Component selector prefix: `designer-` or `app-` (kebab-case)
- Directive selector prefix: `designer` or `app` (camelCase)
- All components: standalone with own `imports` array, bootstrapped via `bootstrapApplication()`
- **Dependency injection:** Use `inject()` function at field level (not constructor injection)
- **Change detection:** All components use `ChangeDetectionStrategy.OnPush`
- Single quotes enforced
- Arrow functions preferred
- 2-space indentation
- **No `any` types without justification** — use proper types, generics, or `unknown` with type narrowing
- **No `$any()` casts in templates** — fix the method signature instead (e.g. `MouseEvent` → `Event` when handling both click and keydown)
- **No `as any` casts unless unavoidable** — prefer type guards, generics, or widening the parameter type

## Linting

- **Config:** `eslint.config.js` (ESLint 10 flat config)
- **Plugins:** `angular-eslint`, `typescript-eslint`, `@stylistic/eslint-plugin`, `eslint-plugin-only-warn`, `eslint-plugin-prefer-arrow`
- All lint errors are downgraded to warnings via `eslint-plugin-only-warn`

## Cross-project Dependencies

| Import | Source |
|--------|--------|
| `@servoy/sablo` | Pre-built tgz from `ngclient.ui/node/dist-sablo/` |
| `@servoy/public` | Pre-built tgz from `ngclient.ui/node/dist-public/` |
| `@servoy/designer` | Pre-built tgz from `ngclient.ui/node/dist-designer/` |

**Important:** These are installed as tgz packages (not source paths) to avoid duplicate `@angular/core` module instances. Never add tsconfig `paths` pointing to source in another Angular project.

## Testing

- **Framework:** Vitest 4.x
- **Builder:** `@angular/build:unit-test`
- **Unit tests (jsdom):** `npm test` — fast, 299 tests, `*.spec.ts` files
- **Browser tests (Chromium):** `npm run test:browser` — TestBed + real DOM, `*.browser.spec.ts` files
- **Config:** `vitest-base.config.ts` (unit), `vitest-browser.config.ts` (browser)
- **Test files:** Co-located as `*.spec.ts` (unit) or `*.browser.spec.ts` (integration)
- **Unit test pattern:** `Object.create(Class.prototype)` with manual mock injection (bypasses `inject()`)
- **Browser test pattern:** `TestBed.configureTestingModule` + `createComponent` with mocked providers

### Zoneless Change Detection Testing

This project uses `provideZonelessChangeDetection()`. When testing components:
- Template-bound state **must** be a signal for Angular to detect changes from non-Angular contexts
- `addEventListener` callbacks and service callbacks do NOT trigger change detection
- Browser tests (`*.browser.spec.ts`) verify that DOM actually updates after signal changes
- Use `fixture.detectChanges()` + `await fixture.whenStable()` after triggering state changes

### Existing tests:
  - `src/designer/services/dynamicguides.service.spec.ts`
  - `src/designer/services/urlparser.service.spec.ts`
  - `src/designer/services/designerutils.service.spec.ts`
  - `src/designer/services/editorcontent.service.spec.ts`
  - `src/designer/services/editorsession.service.spec.ts`
  - `src/designer/services/designsize.service.spec.ts`
  - `src/designer/autoscroll/autoscroll.component.spec.ts`
  - `src/designer/statusbar/statusbar.component.spec.ts`
  - `src/designer/highlight/highlight.component.spec.ts`
  - `src/designer/directives/keyboardlayout.directive.spec.ts`
  - `src/designer/samesizeindicator/samesizeindicator.component.spec.ts`
  - `src/designer/designer.component.spec.ts`
  - `src/designer/resizer/resizer.component.spec.ts`
  - `src/designer/anchoringindicator/anchoringindicator.component.spec.ts`
  - `src/designer/palette/palette.component.spec.ts`
  - `src/designer/contextmenu/contextmenu.component.spec.ts`
  - `src/designer/mouseselection/mouseselection.component.spec.ts`
  - `src/designer/mouseselection/mouseselection.browser.spec.ts`
  - `src/designer/dragselection/dragselection.component.spec.ts`
  - `src/designer/dragselection-responsive/dragselection-responsive.component.spec.ts`
  - `src/designer/editorcontent/editorcontent.component.spec.ts`
  - `src/designer/ghostscontainer/ghostscontainer.component.spec.ts`
  - `src/designer/inlinedit/inlineedit.component.spec.ts`
  - `src/designer/toolbar/toolbar.component.spec.ts`
  - `src/designer/variantscontent/variantscontent.component.spec.ts`
  - `src/designer/variantspreview/variantspreview.component.spec.ts`
  - `src/designer/resizeeditorheight/resizeeditorheight.component.spec.ts`
  - `src/designer/resizeeditorwidth/resizeeditorwidth.component.spec.ts`
  - `src/designer/directives/resizeknob.directive.spec.ts`

## Spec / Design Documents

Feature specs live in `docs/` at the repository root:
- Name: `docs/<JIRA-KEY>-<slug>.spec.md`
- Example: `docs/SVY-21129-dynamic-guides-resize.spec.md`

## Commit Messages

- When AI-generated: end subject with `[ai]`
- When related to Jira: include case number in subject
- Example: `SVY-21129 implement dynamic guides resize snapping [ai]`
