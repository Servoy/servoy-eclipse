# Triage Report — SVY-21493

**Verdict:** PROCEED

## Reported problem
With dynamic guides enabled, resizing a component (e.g. a button) with the **mouse**
stops working as soon as a dynamic guide shows an *equal-size* (same width/height) match
against another component. The component gets stuck and can no longer be resized with the
mouse. Resizing the same component with **SHIFT+ARROW** keys still works. Reported as a
regression from 2026.06.

## Root-cause assessment
The bug is entirely in the RFB Angular frontend, in
`com.servoy.eclipse.designer.rfb/node/src/designer/directives/resizeknob.directive.ts`,
in how it consumes the `DynamicGuidesService.snapData` signal.

Flow during a mouse resize:

1. `DynamicGuidesService.onMouseMove` → `computeGuides` runs on every mouse move
   (`dynamicguides.service.ts:134`, `:294`).
2. When an equal-size guide matches, it sets `snapData` to a `SnapData` carrying
   `width`/`height` (`dynamicguides.service.ts:336-345`,
   `this.snapData.set(this.properties)`).
3. The directive's `effect` reacts and calls `snap(value)`
   (`resizeknob.directive.ts:31-34`), which stores it in the directive field
   `this.snapData = data` (`:84`) with `width`/`height` populated.
4. Mouse-driven resizing in `resizeSelection` is gated by
   `!this.snapData?.width && !this.snapData?.height` (`resizeknob.directive.ts:192`).
   With a populated `snapData`, mouse resize is intentionally suppressed (so the snapped
   size wins).

The defect: **the directive never clears `this.snapData` back to null.** When the mouse
moves such that the guide no longer matches, `computeGuides` calls
`this.snapData.set(null)` at the service level (`dynamicguides.service.ts:381`,
`:297`, `:316`), but the directive's effect is written as:

```ts
const value = this.guidesService.snapData();
if (value) untracked(() => this.snap(value));   // resizeknob.directive.ts:32-33
```

The `if (value)` guard **drops the null emission**, so `snap(null)` is never called and
the directive's `this.snapData` remains stale with `width`/`height` set. From that point
on, the `resizeSelection` guard at `:192` is permanently false and mouse resize is dead
for the rest of the gesture — matching the symptom exactly.

SHIFT+ARROW resizing is unaffected because it is a **different code path** (keyboard
events handled server-side / via the layout key handling, not through
`resizeSelection` / the `snapData` signal), which is why the ticket notes keyboard resize
still works. This is a strong corroboration of the root cause.

### Why the sibling drag path does NOT have the bug
`DragSelectionComponent` consumes the same signal but **unconditionally**:

```ts
const value = this.guidesService.snapData();
untracked(() => this.snap(value));              // dragselection.component.ts:52-53
```

Its `snap()` resets `this.snapData = null` when passed a falsy value
(`dragselection.component.ts:343-345`), so drag correctly recovers from a cleared guide.
Only the resize directive filters out the null and never recovers.

## Ticket premise check
The ticket describes only the symptom and offers no proposed solution, so there is no
premise to overturn. The user context correctly points at the mouse-resize path vs the
keyboard path and at `resizeknob.directive.ts` / the dynamic-guides snapping service. That
direction is accurate.

## Approaches considered
1. **Make the directive process the null emission (recommended).** Change
   `if (value) untracked(() => this.snap(value))` to
   `untracked(() => this.snap(value))` and have `snap()` clear `this.snapData` when the
   value is falsy (mirroring `DragSelectionComponent.snap`). This restores the
   drag/resize symmetry and lets `resizeSelection`'s guard reopen once the guide stops
   matching.
   - Pros: minimal, targeted, matches the already-correct drag implementation; fixes the
     exact regression; low blast radius; easy to unit-test.
   - Cons: must confirm `snap(null)` does not leave the element at the snapped size when
     the guide clears mid-gesture (need to allow `resizeSelection` to take over again —
     which is precisely the intended behaviour).

2. **Reset the directive's `this.snapData` on `snapData()===null` via a small extra effect
   / explicit clear on mouse move.** Keep the `if (value)` guard but add a separate
   branch that nulls `this.snapData` when the signal is null.
   - Pros: leaves the existing guarded call untouched.
   - Cons: two code paths for one concern; more code than approach 1; diverges from the
     drag pattern.

3. **No code change.** — Rejected. This is a real, reproducible regression in Servoy code
   (a mouse-only resize dead-lock), traceable to a specific commit; keyboard resize
   working proves it is not expected behaviour.
   - Pros: none.
   - Cons: leaves a shipped regression that blocks a core designer interaction.

## Recommendation
**PROCEED** with **approach 1**: drop the `if (value)` guard in the
`ResizeKnobDirective` effect and make `snap()` reset `this.snapData` (to `null`/undefined)
when it receives a falsy/null value, aligning it with `DragSelectionComponent.snap`. Add a
Vitest unit test asserting that after a `snap(<data with width/height>)` a subsequent
`snap(null)` clears `this.snapData` so `resizeSelection` is no longer blocked. Verify the
gesture end-to-end in the attached `mySmp.servoy` sample (resize a button until an
equal-size guide appears, move away from the match, confirm mouse resize resumes).

The equal-size guide value itself is still applied on `mouseup` (`sendChanges`,
`resizeknob.directive.ts:242-249`), so snapping semantics are preserved — the fix only
lets the directive recover when the guide is no longer active.

## Git history findings
- The stale-null filtering was introduced in commit **`93b3da3ff6`** ("fix unintended
  signal tracking in effects with untracked(), reset snapData in clear() [ai]", Aug 11
  2026). That commit changed the effect body from `if (value) this.snap(value);` to
  `if (value) untracked(() => this.snap(value));`. The `untracked()` wrapping was correct
  and intended; retaining/introducing the effect's `if (value)` short-circuit is what
  drops the null and causes this regression. This is consistent with the ticket's
  "regression from 2026.06" note.
- The mouse-resize suppression guard `!this.snapData?.width && !this.snapData?.height`
  and the same-size resize snapping originate from the original dynamic-guides work
  (`SVY-18345`, and same-size-on-resize `SVY-19106`), so the guard is by design — the fix
  must not remove it, only ensure `this.snapData` is cleared when the guide clears.
