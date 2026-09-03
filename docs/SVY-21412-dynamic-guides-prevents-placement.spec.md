# Spec: SVY-21412 — Dynamic guides prevents component placement

## 1. Goal

When dropping a new component (e.g. a textbox or combobox) from the palette onto an
absolute-layout form with dynamic guides enabled, a horizontal or vertical alignment
guide that appears once must not permanently "lock" the drop position. Today, as soon
as a guide appears the dragged component becomes stuck at the snapped position and can
no longer be placed anywhere else on the form (for example, in the middle of the form).
This spec fixes that so the preview keeps following the mouse and only snaps while the
mouse is actually within a guide's snap threshold.

## 2. Background

### 2.1 Dynamic guides architecture (RFB designer)

Dynamic guides live in the Angular form designer frontend
(`com.servoy.eclipse.designer.rfb/node/`). The relevant pieces:

- `services/dynamicguides.service.ts` — `DynamicGuidesService` listens to `mousemove`
  on the content area. On each move it computes alignment/distance/size guides in
  `computeGuides()` and publishes the result through a signal:
  `public readonly snapData = signal<SnapData | null>(null);`
  When the mouse is within a guide's snap threshold it sets a `SnapData` with the
  snapped `left`/`top` (and possibly `width`/`height`); when nothing matches it sets
  the signal back to `null` (see `computeGuides()` calling `this.snapData.set(null)`
  and `this.snapData.set(properties.guides!.length == 0 ? null : properties)`).

- Consumers subscribe to `snapData()` via an Angular `effect()` and mirror it into a
  local `snapData` field which their own `mousemove`/`mouseup` handlers read.

### 2.2 Two consumers, two behaviours

There are two consumers of `guidesService.snapData()`, and they handle the signal
differently:

**`dragselection/dragselection.component.ts`** (dragging existing components) — the
effect forwards every value, including `null`:

```ts
const value = this.guidesService.snapData();
untracked(() => this.snap(value));           // snap(data: SnapData | null)
```

Its `snap(null)` resets the local `this.snapData = null`, so when the mouse leaves the
guide threshold the component resumes free movement.

**`palette/palette.component.ts`** (dropping a *new* component from the palette) — the
effect only forwards *truthy* values:

```ts
effect(() => {
    const value = this.guidesService.snapData();
    if (value) untracked(() => this.snap(value));   // snap(data: SnapData)
});
```

Because of the `if (value)` guard, when the guide clears (`snapData()` becomes `null`)
the palette's `snap()` is never called, so the palette's local `this.snapData` keeps
its last (non-null) value for the rest of the drag.

### 2.3 Why placement gets stuck

`palette.component.ts` `onMouseMove` bails out early once a snap is active:

```ts
onMouseMove = (event: MouseEvent) => {
    ...
    if (this.snapData) return;   // <-- freezes preview + canDrop once snapped
    ...
}
```

And `onMouseUp` uses `this.snapData.left/top` for the final drop position when set:

```ts
if (this.snapData) {
    component.x = Math.round(this.snapData.left);
    component.y = Math.round(this.snapData.top);
    ...
}
```

So the failure sequence is:

1. User drags a new textbox/combobox from the palette; a guide appears →
   `guidesService.snapData()` emits a `SnapData`; palette effect calls `snap(value)`;
   `this.snapData` is set.
2. User moves the mouse away from the guide → `guidesService.snapData()` emits `null`,
   but the palette effect's `if (value)` guard skips `snap(null)`, so
   `this.snapData` stays non-null.
3. Every subsequent `onMouseMove` hits `if (this.snapData) return;` and never updates
   the preview position or `canDrop`.
4. On drop, `onMouseUp` uses the stale `snapData.left/top`, so the component lands at
   the old snapped location instead of where the user released the mouse.

This is a regression introduced during the zoneless/signals migration (commit
`93b3da3ff6`), where the palette effect was given the `if (value)` guard and
`snap()`'s parameter is typed `SnapData` (non-nullable) rather than `SnapData | null`.

### 2.4 Scope note

The bug is specific to the **absolute layout** palette drop path — the effect is only
registered `if (this.urlParser.isAbsoluteFormLayout())`, and the ticket reproduces by
dropping a textbox/combobox on an absolute-layout form (attachment
`tst_DynamicGuides.servoy`).

## 3. Design

### 3.1 Forward `null` snap values to the palette

Make the palette consume `null` the same way `dragselection` does, so a cleared guide
resets the palette's local snap state and the preview resumes following the mouse.

Change the palette effect to always call `snap(value)`:

```ts
effect(() => {
    const value = this.guidesService.snapData();
    untracked(() => this.snap(value));
});
```

### 3.2 Make `snap()` accept and handle `null`

Widen `PaletteComponent.snap`'s parameter to `SnapData | null` and ensure a `null`
value clears `this.snapData` and removes any snap-only styling from the dragged preview,
so `onMouseMove` stops bailing out early:

```ts
snap(data: SnapData | null) {
    if (!data) {
        this.snapData = null!;
        return;
    }
    if (this.dragItem?.paletteItemBeingDragged && !this.dragItem.ghost && !this.dragItem.contentItemBeingDragged) {
        this.dragItem.contentItemBeingDragged = this.editorContentService.getContentElementById('svy_draggedelement');
    }
    if (this.dragItem?.contentItemBeingDragged) {
        this.snapData = data;
        // ... existing snapped styling ...
    } else {
        this.snapData = null!;
    }
}
```

The exact reset of preview styling should mirror what the existing else-branch already
does (set `this.snapData = null`); the key behavioural requirement is that once `null`
arrives, `this.snapData` is cleared so `onMouseMove` resumes updating the preview and
`canDrop`, and `onMouseUp` uses the live mouse position.

### 3.3 Keep snapping behaviour intact

When the mouse is within a guide threshold, `guidesService.snapData()` still emits a
`SnapData`, `snap()` still sets `this.snapData`, and drop still uses the snapped
coordinates. The only change is that leaving the threshold now correctly releases the
snap.

## 4. Implementation plan

1. `com.servoy.eclipse.designer.rfb/node/src/designer/palette/palette.component.ts`:
   - In the constructor `effect()` (the `isAbsoluteFormLayout()` block), remove the
     `if (value)` guard so `snap(value)` is always invoked, matching
     `dragselection.component.ts`.
2. Same file — `snap()`:
   - Change the signature from `snap(data: SnapData)` to `snap(data: SnapData | null)`.
   - Add an early `null` branch that clears `this.snapData` (so `onMouseMove`'s
     `if (this.snapData) return;` no longer freezes the preview).
3. Verify `onMouseMove` (`if (this.snapData) return;`) and `onMouseUp` (snapped
   coordinates branch) now behave correctly once `this.snapData` is reset to `null`.
   No change expected in those methods, but confirm.
4. Add/extend unit tests in
   `com.servoy.eclipse.designer.rfb/node/src/designer/palette/palette.component.spec.ts`
   for the snap-then-clear sequence (see acceptance criteria).
5. Run `npm run lint`, `npm run build_debug_nowatch`, and `npm test` in
   `com.servoy.eclipse.designer.rfb/node/` — all must pass with zero warnings.

## 5. Acceptance criteria

- [ ] With dynamic guides enabled on an absolute-layout form (repro solution
      `tst_DynamicGuides.servoy`), dragging a textbox/combobox from the palette shows
      alignment guides when near other components.
- [ ] After a guide has appeared, moving the mouse away from the guide releases the
      snap: the dragged preview follows the mouse again and the component can be dropped
      anywhere on the form (including the middle), landing at the mouse position.
- [ ] Dropping while a guide is active still snaps the component to the guided
      position (existing snapping behaviour preserved).
- [ ] A unit test asserts that after `guidesService.snapData()` emits a `SnapData` and
      then `null`, `PaletteComponent`'s local `snapData` is reset to `null`.
- [ ] `PaletteComponent.snap()` accepts `SnapData | null` and handles `null` without
      throwing.
- [ ] Existing dynamic-guides behaviour for dragging/resizing *existing* components
      (dragselection) is unchanged.
- [ ] RFB `npm run lint`, `npm run build_debug_nowatch`, and `npm test` all pass with
      zero warnings.

## 6. Out of scope

- Distance/size guide logic and the guide-drawing algorithm in
  `DynamicGuidesService.computeGuides()` — no change to how guides are computed.
- Responsive-layout drops (the effect is only registered for absolute layout).
- The `dragselection` / `resizeknob` consumers, which already handle `null` correctly.
- Any change to the Java server side or `.spec` files.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should leaving the guide threshold also visually clear the snapped `width`/`height` styling applied to the dragged preview, or only reset the drop position? Current preview follows the mouse; snapped size resets on the next non-snap move. | Dev | open |
| Confirm the fix is sufficient for both single-drop textbox and combobox, or whether component-type-specific size hints (`shouldSnapToSize`) interact with the freeze. | Dev | open |
