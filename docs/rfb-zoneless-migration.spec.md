# RFB Designer — Zoneless Migration Spec

## Overview

Migrate the RFB designer Angular project (`com.servoy.eclipse.designer.rfb/node`) from zone.js-based change detection to Angular's zoneless mode (`provideZonelessChangeDetection()`).

**Prerequisites (already done):**
- All components use `ChangeDetectionStrategy.OnPush`
- All components are standalone (no NgModule)
- All ViewChild/ViewChildren migrated to signal queries
- inject() used everywhere (no constructor injection)

---

## Current Architecture

### State Management

`EditorSessionService` holds a `State` class instance with 13 properties, accessed via `getState()`:

```typescript
class State {
  showWireframe: boolean;
  showSolutionSpecificLayoutContainerClasses: boolean;
  showSolutionCss: boolean;
  sameSizeIndicator: boolean;
  anchoringIndicator: boolean;
  statusText: string;
  maxLevel: number;
  dragging: boolean;
  resizing: boolean;
  ghosthandle: boolean;
  pointerEvents: string;
  packages: Package[];
  drop_highlight: string;
}
```

### Notification Mechanism

Only 4 properties emit on a `stateListener` BehaviorSubject (with `markForCheck()` in subscribers):
- `statusText`, `sameSizeIndicator`, `anchoringIndicator`, `dragging`

The other 9 properties are mutated directly and rely on zone.js to trigger change detection.

---

## Target Architecture

Replace the `State` class with 13 individual `WritableSignal` fields on `EditorSessionService`. Remove `stateListener` and `markForCheck()` — signals auto-schedule change detection for reading components.

---

## Migration Plan

### Step 1: Convert State to Signals on EditorSessionService

Replace:
```typescript
private state = new State();
getState(): State { return this.state; }
```

With:
```typescript
readonly dragging = signal(false);
readonly resizing = signal(false);
readonly ghosthandle = signal(false);
readonly pointerEvents = signal('none');
readonly showWireframe = signal(false);
readonly showSolutionSpecificLayoutContainerClasses = signal(false);
readonly showSolutionCss = signal(false);
readonly maxLevel = signal(0);
readonly packages = signal<Package[]>([]);
readonly drop_highlight = signal('');
readonly statusText = signal('');
readonly sameSizeIndicator = signal(false);
readonly anchoringIndicator = signal(false);
```

### Step 2: Remove stateListener BehaviorSubject

Delete the `stateListener` property and all `stateListener.next()` calls from the service. Components will react to signal reads instead.

### Step 3: Update Component/Directive State Writes

Replace all direct mutations:
```typescript
// Before
this.editorSession.getState().dragging = true;

// After
this.editorSession.dragging.set(true);
```

**Components with writes:**

| Component | Properties written |
|-----------|-------------------|
| ghostscontainer | `ghosthandle`, `dragging` |
| palette | `packages`, `dragging` |
| resizeeditorwidth | `dragging` |
| resizeeditorheight | `dragging` |
| dragselection-responsive | `dragging`, `drop_highlight` |
| autoscroll | `pointerEvents` |
| toolbar | `showWireframe`, `showSolutionCss`, `showSolutionSpecificLayoutContainerClasses`, `maxLevel`, `sameSizeIndicator`, `anchoringIndicator` |
| resizeknob directive | `resizing` |

### Step 4: Update Component/Service State Reads

Replace all `getState().x` reads:
```typescript
// Before
if (this.editorSession.getState().dragging) { ... }

// After
if (this.editorSession.dragging()) { ... }
```

**Components/services with reads:**

| Component/Service | Properties read |
|-------------------|-----------------|
| mouseselection | `dragging`, `showWireframe`, `ghosthandle`, `resizing` |
| dragselection | `dragging` |
| dragselection-responsive | `dragging` |
| dynamicguides | `dragging`, `resizing` |
| anchoringindicator | `anchoringIndicator`, `dragging` |
| samesizeindicator | `sameSizeIndicator`, `dragging` |
| statusbar | `statusText` |
| autoscroll (template) | `pointerEvents` |
| toolbar | `showWireframe`, `showSolutionCss`, `showSolutionSpecificLayoutContainerClasses`, `maxLevel`, `sameSizeIndicator`, `anchoringIndicator` |
| contextmenu | `packages` |
| palette | `packages` |
| dynamicguides.service | `dragging`, `resizing`, `packages` |
| designerutils.service | `packages` |

### Step 5: Update Template Bindings

```html
<!-- Before -->
[ngStyle]="{'pointer-events': editorSession.getState().pointerEvents}"
[hidden]="editorSession.getState().dragging"

<!-- After -->
[ngStyle]="{'pointer-events': editorSession.pointerEvents()}"
[hidden]="editorSession.dragging()"
```

### Step 6: Remove ChangeDetectorRef + markForCheck

Remove `ChangeDetectorRef` injection and `markForCheck()` calls from components that only reacted to state changes via `stateListener`:
- `StatusBarComponent`
- `AnchoringIndicatorComponent`
- `SameSizeIndicatorComponent`
- `MouseSelectionComponent`

These components now read signals directly in their templates or effects, so Angular's scheduler handles them.

**Note:** Keep `markForCheck()` in `PaletteComponent` (HTTP callback) and `VariantsContentComponent` (non-state subscription) — those aren't covered by state signals.

### Step 7: Update main.ts

```typescript
// Add
provideZonelessChangeDetection()

// Remove zone.js from polyfills in angular.json
```

### Step 8: Remove zone.js

- Remove `zone.js` from `package.json` dependencies
- Remove from `angular.json` polyfills array

### Step 9: Update Tests

- Remove `stateListener` BehaviorSubject mocks
- Replace `editorSession.getState()` mocks with signal mocks:
  ```typescript
  editorSession = {
    dragging: signal(false),
    resizing: signal(false),
    // ...
  };
  ```
- Remove `cdr` mocks from components where `markForCheck()` was removed

---

## Risk Areas

### 1. `packages` Signal (Array Immutability)

The `packages` array is currently mutated in-place in some components (e.g., palette adds properties to component objects). Signals require reference changes to trigger reactivity:
```typescript
// Won't trigger reactivity:
this.editorSession.packages()[0].name = 'new';

// Must do:
this.editorSession.packages.set([...newPackages]);
```

**Mitigation:** Audit all `packages` mutations. Most writes already do full replacement (`state.packages = packages`), but verify palette's internal mutations.

### 2. `dragging` Signal — Multiple Writers

Written by 6+ components. Currently a simple boolean assignment. With signals, all become `.set()` calls — functionally identical but verify no race conditions.

### 3. WebSocket Layer (Sablo)

The sablo WebSocket layer delivers messages that update EditorSessionService state. Verify that:
- Signal writes from WebSocket callbacks properly schedule change detection
- No wrapper like `NgZone.run()` is needed (signal writes are zone-independent)

### 4. Toolbar Template Complexity

The toolbar has complex template expressions reading multiple state properties. Verify all conditional bindings work correctly when signals are read (they should — signal reads in templates auto-register as dependencies).

---

## Acceptance Criteria

1. `npm run build` passes with no errors or warnings
2. `npm run lint` passes with 0 warnings
3. `npm test` — all 310 tests pass
4. `zone.js` is not in `package.json` or `angular.json`
5. `provideZonelessChangeDetection()` is in `main.ts`
6. No `ChangeDetectorRef` or `markForCheck()` for state-signal-driven components
7. All state properties are `WritableSignal` on EditorSessionService
8. Manual testing: toolbar buttons enable/disable, status bar updates, drag & drop works, palette loads

---

## Effort Estimate

| Task | Files | Complexity |
|------|-------|-----------|
| Convert State → 13 signals on service | 1 | Medium |
| Remove stateListener + subscriptions | 5 | Low |
| Update mutations (`.getState().x = y` → `.x.set(y)`) | 14 components + 1 directive | Medium (mechanical) |
| Update template bindings | 2 templates | Low |
| Update reads (`.getState().x` → `.x()`) | 14 components + 2 services + 1 directive | Medium (mechanical) |
| Remove CDR/markForCheck | 4 components | Low |
| Update main.ts + remove zone.js | 2 | Low |
| Update spec files (mock changes) | ~28 | Medium |
| **Total** | **~35 files** | **2-3 hours** |
