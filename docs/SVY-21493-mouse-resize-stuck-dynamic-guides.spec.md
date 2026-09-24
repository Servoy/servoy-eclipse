# Spec: SVY-21493 — Mouse resize gets stuck when a dynamic guide equal-size match appears

## 1. Goal
With dynamic guides enabled, resizing a component with the mouse stops working the moment
an *equal-size* (same width/height) guide matches another component, and never recovers for
the rest of the gesture. This fix makes the `ResizeKnobDirective` recover once the guide
stops matching, by processing the `snapData` null emission (currently dropped) and clearing
its internal `this.snapData` — mirroring the already-correct `DragselectionComponent`. Mouse
resize resumes as soon as the equal-size match goes away, while the snapped size is still
applied on mouseup when a guide is active.

## 2. Background

### 2.1 Where the bug lives
The defect is entirely in the RFB Angular frontend, in
`com.servoy.eclipse.designer.rfb/node/src/designer/directives/resizeknob.directive.ts`, in
how it consumes the `DynamicGuidesService.snapData` signal.

Flow during a mouse resize:

1. `DynamicGuidesService.onMouseMove` → `computeGuides` runs on every mouse move.
2. When an equal-size guide matches, the service sets `snapData` to a `SnapData` carrying
   `width`/`height` (`this.snapData.set(this.properties)`). When the guide no longer matches,
   the service calls `this.snapData.set(null)`.
3. The directive's `effect` reacts and calls `snap(value)`
   (`resizeknob.directive.ts:31-34`), which stores it in the directive field
   `this.snapData = data` (`:84`) and applies the snapped width/height.
4. Mouse-driven resizing in `resizeSelection` is gated by
   `!this.snapData?.width && !this.snapData?.height` (`resizeknob.directive.ts:192`). With a
   populated `snapData`, mouse resize is intentionally suppressed so the snapped size wins.

### 2.2 Root cause
The directive's effect drops the null emission:

```ts
const value = this.guidesService.snapData();
if (value) untracked(() => this.snap(value));   // resizeknob.directive.ts:32-33
```

The `if (value)` guard short-circuits when the service clears `snapData` to `null`, so
`snap(null)` is never called and the directive's `this.snapData` stays stale with
`width`/`height` set. From that point the `resizeSelection` guard at `:192` is permanently
false and mouse resize is dead for the rest of the gesture — exactly the reported symptom.

SHIFT+ARROW resizing is a different code path (keyboard handling, not `resizeSelection` / the
`snapData` signal), which is why keyboard resize keeps working. That corroborates the root
cause.

### 2.3 Why the sibling drag path does NOT have the bug
`DragselectionComponent` consumes the same signal unconditionally and its `snap()` accepts
`SnapData | null`, resetting `this.snapData = data` (to `null`) when passed a falsy value
(`dragselection.component.ts:51-54`, `:343-345`). Drag therefore recovers correctly from a
cleared guide; only the resize directive filters out the null and never recovers.

### 2.4 Git history
- The stale-null filtering was introduced in commit **`93b3da3ff6`** ("fix unintended signal
  tracking in effects with untracked(), reset snapData in clear() [ai]", Aug 11 2026), which
  changed the effect body to `if (value) untracked(() => this.snap(value));`. The
  `untracked()` wrapping was correct and intended; the retained `if (value)` short-circuit is
  what drops the null and causes this regression — consistent with the "regression from
  2026.06" note.
- The mouse-resize suppression guard `!this.snapData?.width && !this.snapData?.height` and the
  same-size resize snapping originate from the original dynamic-guides work (`SVY-18345`) and
  same-size-on-resize (`SVY-19106`), so the guard is by design. The fix must **not** remove
  the guard — only ensure `this.snapData` is cleared when the guide clears.

## 3. Design

### 3.1 Process the null emission in the effect
Drop the `if (value)` guard so the effect forwards every emission (including `null`) to
`snap()`:

```ts
constructor() {
    effect(() => {
        const value = this.guidesService.snapData();
        untracked(() => this.snap(value));
    });
}
```

### 3.2 Let `snap()` accept and clear a null value
Widen the `snap` parameter type to `SnapData | null` and let the existing assignment
`this.snapData = data` set it back to `null` when the guide clears. The current guard already
short-circuits the *application* branch when the value is falsy — the change is that
`this.snapData` is now reset instead of being left stale:

```ts
snap(data: SnapData | null): void {
    if (this.currentElementInfo && this.editorSession.resizing()) {
        this.snapData = data;
        if (this.initialElementInfo.size == 1 && (this.snapData?.width || this.snapData?.height)) {
            // ...unchanged: applies snapped left/top/width/height...
        }
    }
}
```

Behaviour after the change:
- During an active gesture `this.currentElementInfo` is set and `this.editorSession.resizing()`
  is `true`, so `snap(null)` enters the outer `if`, assigns `this.snapData = null`, and skips
  the apply branch (`this.snapData?.width || this.snapData?.height` is false). The
  `resizeSelection` guard at `:192` then reopens and mouse resize resumes.
- When a guide is active, `snap(<data>)` still stores the data and applies the snapped size,
  and `sendChanges` (`:242-249`) still emits the snapped width/height/left/top/cssPos on
  mouseup — snapping semantics are preserved.

### 3.3 Guard/consumer compatibility
- `resizeSelection` (`:192`) reads `this.snapData?.width` / `?.height` with optional chaining;
  `null` is already safe.
- `sendChanges` (`:242`) checks `if (this.snapData && ...)`; `null` correctly falls to the
  non-snap branch.
- No other consumer of `this.snapData` needs changes.

### 3.4 Type note
The `snap` signature currently is `snap(data: SnapData): void`. Widening it to
`SnapData | null` matches `DragselectionComponent.snap(data: SnapData | null)` and avoids a
type mismatch now that `null` is a valid argument. This is consistent with the RFB
"no `any`, proper types" convention.

## 4. Implementation plan

1. In `com.servoy.eclipse.designer.rfb/node/src/designer/directives/resizeknob.directive.ts`,
   change the constructor effect from
   `if (value) untracked(() => this.snap(value));` to
   `untracked(() => this.snap(value));` (line 33).
2. In the same file, widen the `snap` parameter type from `SnapData` to `SnapData | null`
   (line 82). The body is unchanged: the existing `this.snapData = data` assignment now clears
   the field when `data` is `null`, and the apply branch already self-guards on
   `this.snapData?.width || this.snapData?.height`.
3. Add a Vitest unit test in
   `com.servoy.eclipse.designer.rfb/node/src/designer/directives/resizeknob.directive.spec.ts`
   (in the existing `describe('snap', ...)` block) asserting that, while resizing a single
   selection, calling `snap(<data with width/height>)` followed by `snap(null)` leaves
   `this.snapData === null` so the `resizeSelection` guard is no longer blocked.
4. Run `npm run lint`, `npm run build_debug_nowatch`, and `npm test` in
   `com.servoy.eclipse.designer.rfb/node/`; all must pass with zero lint warnings.

## 5. Acceptance criteria
- [ ] The `ResizeKnobDirective` effect calls `snap(value)` for every `snapData` emission,
      including `null` (no `if (value)` short-circuit).
- [ ] After `snap(<data with width/height>)` then `snap(null)` during an active resize,
      `this.snapData` is `null`, so `resizeSelection`'s
      `!this.snapData?.width && !this.snapData?.height` guard is true again and mouse resize
      resumes.
- [ ] When an equal-size guide is active on mouseup, the snapped width/height/left/top (and
      `cssPos`) are still applied via `sendChanges` (snapping semantics preserved).
- [ ] Manual check in the attached `mySmp.servoy` sample: resize a button until an equal-size
      guide appears, move the mouse away from the match, and confirm mouse resize resumes;
      release on a match and confirm the snapped size is applied.
- [ ] SHIFT+ARROW resize behaviour is unchanged.
- [ ] A Vitest unit test covering the `snap(<data>)` → `snap(null)` clearing exists and
      passes; the existing `snap` tests still pass.
- [ ] `npm run lint` (zero warnings), `npm run build_debug_nowatch`, and `npm test` all pass.

## 6. Out of scope
- Any change to `DynamicGuidesService` / the guide computation — the service already emits
  `null` correctly.
- The keyboard (SHIFT+ARROW) resize path.
- Refactoring `resizeknob.directive.ts` state to signals (tracked separately in the RFB
  modernization backlog).
- The mouse-resize suppression guard itself (by design; must be preserved).

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Is a browser (`*.browser.spec.ts`) test needed in addition to the jsdom unit test, given the guard/recovery logic is pure state (no OnPush DOM binding)? Recommendation: unit test is sufficient. | dev | open |
