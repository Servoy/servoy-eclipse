# Project Context — Servoy Web Package Manager (WPM, Angular frontend)

This project is the **Servoy Web Package Manager** (WPM) frontend — an Angular SPA embedded
in the Eclipse-based Servoy Developer IDE. It provides the UI for managing web packages,
modules, and solutions. It communicates with the Java side over a WebSocket.

## SDD variant

This repo/sub-project uses the **sdd-angular** shared skill (Angular pipeline).

## Technology stack

| Aspect | Value |
|--------|-------|
| Name | `wpm2` |
| Framework | Angular 22.1 |
| Language | TypeScript 6.0 |
| Build tool | Angular CLI (`@angular/build:application`, esbuild) |
| Test runner | **Vitest 4** (`ng test` wired to Vitest, no watch) |
| Linter | ESLint 10 flat config (`angular-eslint`, `typescript-eslint`, `@stylistic`, `prefer-arrow`) |
| Version | 2026.9.0 |

## Commands

| Task | Command |
|------|---------|
| Lint (must be zero warnings) | `npm run lint` |
| Production build | `npm run build` |
| Tests (Vitest, no watch) | `npm test` |
| Tests (watch) | `npm run test:watch` |
| Dev server (localhost:4200) | `npm start` |

## After every code change
1. `npm run lint` — fix all warnings (zero-warning policy)
2. `npm run build` — verify production build compiles
3. `npm test` — verify all Vitest tests pass

## Project structure

All source lives under `src/wpm/`:

| File/Directory | Purpose |
|----------------|---------|
| `main.component.ts` | Root component (`app-wpm`) |
| `wpm.service.ts` | Core service: WebSocket messaging, package management |
| `websocket.service.ts` | WebSocket connection management |
| `header/` | Header component (repository selector, update-all button) |
| `content/` | Content component (tab groups by package type) |
| `packages/` | Packages component (package list with install/uninstall) |
| `update-dialog/` | Update packages dialog |

## Code conventions

- Component selector prefix: `app-` or `wpm-` (kebab-case); directive prefix `app`/`wpm` (camelCase)
- All components: `standalone: true` with their own `imports` array
- All components use `ChangeDetectionStrategy.OnPush`
- Bootstrap via `bootstrapApplication()` in `main.ts` (no NgModule)
- Single quotes; arrow functions preferred
- No `any` without justification; no `$any()` in templates; no `as any` unless unavoidable —
  prefer type guards / generics / widened parameter types
- WebSocket calls go through `WpmService` — follow the existing message pattern

## AGENTS.md

Always read the nearest `AGENTS.md` (in this `node/` folder) first — it has the full command
list, conventions, and linting rules that you must follow.

## Gotchas

- **Vitest, not Karma/Jasmine:** write tests with the Vitest API (`vi`, `describe`/`it`/`expect`)
  and Angular TestBed. Do not use `jasmine.createSpyObj`.
- **OnPush + standalone everywhere:** state that drives the view should be signal-based so
  OnPush change detection updates correctly.
- **Zero-warning lint gate:** the build is not done until `npm run lint` passes clean.
- **docs/ is at the git root:** this sub-project sits deep under
  `com.servoy.eclipse.designer.wpm/node`; specs and triage reports go in the repo-root `docs/`.
