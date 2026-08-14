# Zoneless Migration Specification — Servoy TiNG Runtime

## Summary

This document describes the migration path to remove Zone.js from the Servoy TiNG Angular application and switch to Angular's zoneless change detection (`provideZonelessChangeDetection()`). It covers the current state, blocking issues, required changes across the core project and the 4 external component packages, and a phased execution plan.

## Current State (Angular 22)

### Core Project (ngclient2)

| Aspect | Status |
|--------|--------|
| Angular version | 22 |
| Standalone components | 100% complete |
| `bootstrapApplication()` | Done |
| Template control flow (`@if`/`@for`) | 100% complete |
| `inject()` migration | Done (servoydefault + src/) |
| Signal queries (local) | Partial (basechoice, baselabel, check, radio, spinner, calendar, combobox, typeahead, bg_splitter) |
| `@Input()` → `input()` signal migration | Not started in core (architecturally complex) |
| OnPush change detection | Used on most components via `ServoyBaseComponent` pattern |
| Zone.js | Still loaded as polyfill |
| `ChangeDetectorRef` usage | Active in basecomponent.ts, form_component, listformcomponent, session-view, designer |
| `NgZone` usage | Active in websocket.service.ts (5 call sites) |

### Component Packages

| Package | Angular | OnPush | Signal Inputs | Standalone | Test Framework | Zoneless-Ready |
|---------|---------|--------|---------------|-----------|----------------|----------------|
| `@servoy/bootstrapextracomponents` | 22.1 | ✅ All | ✅ All | ✅ All | Vitest | ✅ Yes |
| `@servoy/bootstrapcomponents` | 22.1 | ✅ All | ✅ All | ❌ NgModule | Vitest | ✅ Yes |
| `@servoy/servoyextracomponents` | 22.0.8 | ✅ All | ✅ All | ❌ NgModule | Vitest | ✅ Yes |
| `@servoy/nggrids` | 22.0.8 | ✅ All | ✅ All | ❌ NgModule | Vitest | ✅ Yes |

All 4 packages use signal inputs + OnPush + explicit change notification, making them zoneless-compatible.

---

## Angular 22 Zoneless Requirements

From the official Angular documentation:

### What Triggers Change Detection Without Zone.js

1. `ChangeDetectorRef.markForCheck()` (also called by `AsyncPipe`)
2. `ComponentRef.setInput()`
3. Updating a signal that's read in a template
4. Bound host or template listener callbacks (DOM events handled by Angular)
5. Attaching a view marked dirty by one of the above

### What's Compatible (No Changes Needed)

- `NgZone.run()` and `NgZone.runOutsideAngular()` — **explicitly compatible** with zoneless
- `ChangeDetectorRef.detectChanges()` — still works, is an explicit CD trigger
- `ChangeDetectorRef.markForCheck()` — the preferred way to notify Angular

### What Must Be Removed

- `NgZone.onMicrotaskEmpty` / `NgZone.onUnstable` / `NgZone.isStable` / `NgZone.onStable` — never emit in zoneless

### What Needs Attention

- `setTimeout`/`setInterval` callbacks that mutate UI state without explicit notification
- Reactive forms `setValue`/`patchValue` — need `markForCheck()` or signal binding
- Any async operation that modifies template-bound state without signals or `markForCheck()`

---

## Blocking Issues Analysis

### Issue 1: `setTimeout`/`setInterval` Without CD Notification (HIGH)

**Impact:** ~34 call sites in `src/` that use `setTimeout`/`setInterval`  
**Problem:** Without zone.js, these callbacks no longer trigger change detection automatically.  
**Risk:** UI won't update after timeout-based state changes.

| File | Count | Purpose |
|------|-------|---------|
| `src/ngclient/services/application.service.ts` | 2 | Info panel hide, UI updates |
| `src/ngclient/services/bootstrap-window/bswindow_manager.service.ts` | 2 | Modal backdrop, window operations |
| `src/ngclient/services/bootstrap-window/bswindow.ts` | 2 | Title blink (setInterval + setTimeout) |
| `src/ngclient/services/popupform.service.ts` | 2 | Popup positioning |
| `src/ngclient/services/file-upload-window/file-upload-window.component.ts` | 1 | Upload progress |
| `src/ngclient/services/clientdesign.service.ts` | 1 | Design mode |
| `src/ngclient/services/window.service.ts` | 1 | setInterval polling |
| `src/ngclient/form.service.ts` | 1 | Form loading timeout |
| `src/servoycore/listformcomponent/listformcomponent.ts` | 3 | Resize + scroll handling |
| `src/servoycore/session-view/session-view.ts` | 1 | Session redirect |
| `src/sablo/sablo.service.ts` | 2 | Service call timeouts |
| `src/sablo/defer.service.ts` | 1 | Deferred execution |
| `src/sablo/io/mobilebridge.ts` | 1 | Mobile init |
| `src/sablo/io/reconnecting.websocket.ts` | 2 | Reconnection delays |
| `src/sablo/util/loading-indicator/loading-indicator.service.ts` | 2 | Show/hide loading |
| `src/designer/servoydesigner.component.ts` | 1 | Designer init |

**Fix:** Each site needs one of:
- `markForCheck()` after mutating state
- Signal update (`someSignal.set(...)`)
- No fix needed if the timeout doesn't affect template bindings

### Issue 2: FormService Imperative `detectChanges()` Push Model (MEDIUM)

**Impact:** ~5 call sites in `form.service.ts`  
**Problem:** The FormService explicitly calls `formComponent.detectChanges()` after receiving WebSocket property changes. This works in zoneless (it's explicit), but is fragile — if any path misses a call, components won't update.

**Key locations:**
- `form.service.ts:130` — after form state update
- `form.service.ts:638-644` — broadcast detectChanges to all forms when new form loaded
- `form.service.ts:980` — after root property changes

**Assessment:** NOT a blocker — the explicit calls are zoneless-compatible. But the pattern should eventually migrate to signal-based state management for robustness.

### Issue 3: `basecomponent.ts` — `detectChanges()` in Lifecycle (LOW-MEDIUM)

**Impact:** ALL components (100+ subclasses in core + 4 external packages)  
**Problem:** `ngAfterViewInit()` calls `this.cdRef.detectChanges()` (line 54). In zoneless, this is still valid but can surface `ExpressionChangedAfterItHasBeenChecked` errors if the detect triggers writes.

**Public API:**
```typescript
// basecomponent.ts:127
public detectChanges() {
    this.cdRef.detectChanges();
}
```

This method is called by the form layer and by components themselves after timeouts/promises. It's compatible with zoneless.

**Assessment:** No immediate change needed. Monitor for `ExpressionChangedAfterItHasBeenChecked` errors.

### Issue 4: `component_converter.ts` — Same-Ref Property Change Triggering (MEDIUM)

**Impact:** Core property push mechanism  
**Problem:** Line 571 comment: "trigger ngOnChanges for properties that had updates but the ref remained the same (as those will not automatically be triggered by root detectChanges())". This relies on the parent form calling `detectChanges()`, which still works in zoneless.

**Assessment:** Compatible, but tightly coupled to the imperative model. Long-term, converting to signals would make this obsolete.

### Issue 5: Third-Party Library Compatibility (UNKNOWN)

| Library | Used By | Zoneless Risk |
|---------|---------|---------------|
| AG Grid 36 | nggrids | **Low** — has own change detection, OnPush compatible |
| @ng-bootstrap 21 | bootstrapcomponents, bootstrapextra | **Low** — widely used, actively maintained |
| TinyMCE | servoy-extra (htmlarea) | **Medium** — has own event loop, may need wrapper with `markForCheck()` |
| @eonasdan/tempus-dominus | bootstrapcomponents, nggrids | **Medium** — DOM-event based, may not trigger Angular CD |
| @angular-slider/ngx-slider | servoy-extra | **Unknown** |
| ng-select2-component | servoy-extra | **Unknown** |
| @ali-hm/angular-tree-component | servoy-extra | **Unknown** |

**Fix:** Test each library in zoneless mode. Libraries using DOM events that are bound via Angular template bindings (`(click)="..."`) will work. Libraries that modify state via internal `setTimeout`/`requestAnimationFrame` without Angular awareness will need wrappers.

### Issue 6: WebSocket Layer `NgZone.run()` (NOT A BLOCKER)

**Location:** `src/sablo/websocket.service.ts`  
**Pattern:**
```typescript
this.websocket.onmessage = (message) => 
    this.handleHeartbeat(message) || this.ngZone.run(() => this.handleMessage(message));
```

**Assessment:** Angular docs explicitly state `NgZone.run()` is **compatible** with zoneless and should NOT be removed (it helps apps that still use Zone.js). No change needed.

---

## Migration Plan

### Phase 1: Hybrid Mode (Non-Breaking)

**Goal:** Enable zoneless alongside zone.js to detect issues without breaking anything.

1. Add `provideZonelessChangeDetection()` to bootstrap (replaces zone-based CD)
2. **Keep `zone.js` in polyfills** temporarily (for safety)
3. Add debug check in development mode:
   ```typescript
   provideCheckNoChangesConfig({ exhaustive: true, interval: 500 })
   ```
4. Run the application and observe `ExpressionChangedAfterItHasBeenChecked` errors
5. Document all failing paths

### Phase 2: Fix setTimeout/setInterval Patterns (~34 sites)

**Goal:** Ensure all async state mutations notify Angular.

For each `setTimeout`/`setInterval` site:
1. Determine if the callback modifies template-bound state
2. If yes, add `markForCheck()` or convert the state to a signal
3. If it only modifies internal service state not bound to templates, no change needed

**Priority order:**
1. `loading-indicator.service.ts` — visible UI impact
2. `application.service.ts` — user-facing
3. `bswindow*.ts` — modal/window operations
4. `popupform.service.ts` — popup positioning
5. `listformcomponent.ts` — list scrolling
6. Remaining services

### Phase 3: Verify Third-Party Libraries

**Goal:** Confirm all external libraries work without zone.js.

1. Create a test form with each library component:
   - AG Grid (power grid + data grid)
   - Calendar (tempus-dominus)
   - HTML Area (TinyMCE)
   - Slider, Tree, Select2
2. Test user interactions (click, type, select, scroll)
3. For any that fail, create wrapper directives that call `markForCheck()` on library events

### Phase 4: Remove Zone.js

**Goal:** Fully remove zone.js from the build.

1. Remove `zone.js` from `polyfills` in `angular.json` (both `build` and `test` targets)
2. Remove `zone.js/testing` from test configuration
3. Run `npm uninstall zone.js`
4. Verify all tests pass with `provideZonelessChangeDetection()` in TestBed
5. Verify full application works end-to-end

### Phase 5: Cleanup (Optional, Long-Term)

**Goal:** Modernize the change detection model.

1. Replace `detectChanges()` calls with signal-based reactivity where feasible
2. Migrate the FormService property-push model to use signals
3. Remove `ChangeDetectorRef` injection from `basecomponent.ts` (requires all subclasses to use signals)
4. Remove `NgZone` imports (optional — they're compatible but unnecessary)

---

## Available Tooling

### Angular CLI Schematics (Prerequisites — Already Done)

| Schematic | Status |
|-----------|--------|
| `@angular/core:signal-input-migration` | ✅ Done in all 4 packages |
| `@angular/core:signal-queries-migration` | ✅ Done in all 4 packages |
| `@angular/core:output-migration` | ✅ Done in all 4 packages |
| `@angular/core:standalone-migration` | ✅ Done in core |
| `@angular/core:control-flow-migration` | ✅ Done in core |
| `@angular/core:inject-migration` | ✅ Done in core (servoydefault + src/) |

### No Zoneless-Specific Schematic Exists

Angular does **not** provide a schematic for zoneless migration. The migration is manual and guided by:
- `provideCheckNoChangesConfig()` — runtime detection of missing notifications
- `onpush_zoneless_migration` MCP tool — per-file analysis and step-by-step guidance
- Manual code review of async patterns

### Debugging Tools

| Tool | Purpose |
|------|---------|
| `provideCheckNoChangesConfig({exhaustive: true, interval: 500})` | Runtime detection of bindings that update without notification |
| `ExpressionChangedAfterItHasBeenChecked` errors | Surfaces state written during rendering |
| Angular DevTools (Chrome extension) | Visualize change detection cycles |
| `onpush_zoneless_migration` MCP tool | Per-file migration guidance |

---

## Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Third-party libraries break silently | Components render stale data | Phase 3 dedicated testing with all libraries |
| Missed `setTimeout` sites | Sporadic UI update failures | `provideCheckNoChangesConfig` catches them at runtime |
| Performance regression from too many `markForCheck()` | Excessive CD runs | Profile with Angular DevTools, batch notifications |
| External packages (customer packages) break | Customer solutions fail | Document zoneless requirements, provide migration guide for package developers |
| `basecomponent.ts` change affects all packages | Breaking change across ecosystem | Keep `detectChanges()` public API, add `markForCheck()` as alternative |

---

## Success Criteria

- [ ] Application boots without `zone.js` in polyfills
- [ ] No `ExpressionChangedAfterItHasBeenChecked` errors in dev mode with exhaustive checks enabled
- [ ] All 4 component packages' tests pass with `provideZonelessChangeDetection()` in TestBed
- [ ] Core project Vitest suite passes without zone.js
- [ ] WebSocket property push → component update works without zone.js
- [ ] All third-party library components (AG Grid, calendar, TinyMCE, tree, slider) respond to user interaction
- [ ] Bundle size reduced (zone.js is ~45KB gzipped)
- [ ] No performance regression on form load and component rendering

---

## Timeline Estimate

| Phase | Effort | Dependency |
|-------|--------|-----------|
| Phase 1: Hybrid mode | 1 day | None |
| Phase 2: Fix setTimeout patterns | 3-5 days | Phase 1 |
| Phase 3: Third-party library verification | 2-3 days | Phase 1 |
| Phase 4: Remove zone.js | 1 day | Phase 2 + 3 |
| Phase 5: Cleanup (optional) | Ongoing | Phase 4 |

**Total estimated effort: 1-2 weeks**
