# AGENTS.md - Servoy Designer RFB (Angular Frontend)

## Project Overview

This is the **Servoy Form Designer** frontend — an Angular SPA embedded in the Eclipse-based Servoy Developer IDE. It provides the visual drag-and-drop form editor.

- **Name:** `servoy-designer`
- **Version:** 2026.3.0
- **Framework:** Angular 21 (NgModule-based)
- **Language:** TypeScript 5.9
- **Build:** Angular CLI with `@angular/build:application` (esbuild)

## Commands

| Command | Purpose |
|---------|---------|
| `npm run lint` | Run ESLint — must pass with zero errors |
| `npm run build_debug_nowatch` | Verify build compiles |
| `npm test` | Run Jasmine/Karma tests |
| `npm start` | Dev server on localhost:4200 |

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
- All components: `standalone: false`, declared in `DesignerModule`
- Single quotes enforced
- Arrow functions preferred
- 2-space indentation
- No `any` types without justification

## Cross-project Dependencies

| Import | Source |
|--------|--------|
| `@servoy/sablo` | `../../com.servoy.eclipse.ngclient.ui/node/src/sablo/public-api` |
| `@servoy/public` | `../../com.servoy.eclipse.ngclient.ui/node/projects/servoy-public/src/public-api` |
| `@servoy/designer` | `../../com.servoy.eclipse.ngclient.ui/node/src/designer/public-api` |

## Testing

- **Framework:** Jasmine 6 + Karma
- **Test files:** Co-located as `*.spec.ts`
- **Run:** `npm test` or `npx ng test --watch=false --browsers=ChromeHeadless`
- **Existing tests:** `src/designer/dynamicguides/dynamicguides.service.spec.ts`

## Spec / Design Documents

Feature specs live in `docs/` at the repository root:
- Name: `docs/<JIRA-KEY>-<slug>.spec.md`
- Example: `docs/SVY-21129-dynamic-guides-resize.spec.md`

## Commit Messages

- When AI-generated: end subject with `[ai]`
- When related to Jira: include case number in subject
- Example: `SVY-21129 implement dynamic guides resize snapping [ai]`
