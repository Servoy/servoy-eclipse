# AGENTS.md - Servoy Web Package Manager (Angular Frontend)

## Project Overview

This is the **Servoy Web Package Manager** (WPM) frontend — an Angular SPA embedded in the Eclipse-based Servoy Developer IDE. It provides the UI for managing web packages, modules, and solutions.

- **Name:** `wpm2`
- **Version:** 2026.9.0
- **Framework:** Angular 22.1 (NgModule-based)
- **Language:** TypeScript 6.0
- **Build:** Angular CLI with `@angular/build:application` (esbuild)

## Commands

| Command | Purpose |
|---------|---------|
| `npm run lint` | Run ESLint — must pass with zero warnings |
| `npm run build` | Production build |
| `npm test` | Run Vitest tests (no watch) |
| `npm run test:watch` | Run Vitest in watch mode |
| `npm run test:ui` | Run Vitest with UI |
| `npm start` | Dev server on localhost:4200 |

## After Every Code Change

1. Run `npm run lint` — fix all warnings
2. Run `npm run build` — verify production build compiles
3. Run `npm test` — verify all tests pass

## Project Structure

All source lives under `src/wpm/`:

| File/Directory | Purpose |
|----------------|---------|
| `main.component.ts` | Root component (app-wpm) |
| `wpm.service.ts` | Core service: WebSocket messaging, package management |
| `websocket.service.ts` | WebSocket connection management |
| `header/` | Header component (repository selector, update-all button) |
| `content/` | Content component (tab groups by package type) |
| `packages/` | Packages component (package list with install/uninstall) |
| `update-dialog/` | Update packages dialog |

## Code Conventions

- Component selector prefix: `app-` or `wpm-` (kebab-case)
- Directive selector prefix: `app` or `wpm` (camelCase)
- All components: `standalone: true` with own `imports` array
- All components use `ChangeDetectionStrategy.OnPush`
- Bootstrap via `bootstrapApplication()` in `main.ts` (no NgModule)
- Single quotes enforced
- Arrow functions preferred
- **No `any` types without justification** — use proper types, generics, or `unknown` with type narrowing
- **No `$any()` casts in templates** — fix the method signature instead (e.g. `MouseEvent` → `Event` when handling both click and keydown)
- **No `as any` casts unless unavoidable** — prefer type guards, generics, or widening the parameter type

## Linting

- **Config:** `eslint.config.js` (ESLint 10 flat config)
- **Plugins:** `angular-eslint`, `typescript-eslint`, `@stylistic/eslint-plugin`, `eslint-plugin-only-warn`, `eslint-plugin-prefer-arrow`
- All lint errors are downgraded to warnings via `eslint-plugin-only-warn`

## Testing

- **Framework:** Vitest 4.x + jsdom
- **Builder:** `@angular/build:unit-test`
- **Config:** `vitest-base.config.ts` (JUnit XML output to `../target/vitest-results.xml`)
- **Test files:** Co-located as `*.spec.ts`
- **Run:** `npm test` (no watch) or `npm run test:watch`
- **Pattern:** `Object.create(Class.prototype)` with manual mock injection (matching RFB pattern)
- Import test functions explicitly: `import { describe, it, expect, beforeEach, vi } from 'vitest';`

## Commit Messages

- When AI-generated: end subject with `[ai]`
- When related to Jira: include case number in subject
- Example: `SVY-21300 add package search to WPM [ai]`
