# Spec: SVY-21129 — Dynamic guides problems on resize

## 1. Goal

Stop the form designer from "jumping" / mirror-moving a component when the user
drags a single-direction resize knob (e.g. dragging the east handle should only
change `width`, never `left`). The current behaviour is a regression introduced
by SVY-20571 ("Resize with dynamic guides enabled — improve resize behaviour
and add support for west and north resize directions"): when the dynamic-guides
service decides which side of the component is being resized, it asks
`pointCloserToTopOrLeftSide` instead of looking at which knob was actually
grabbed. Once the cursor crosses the centre of the component during a drag,
the answer flips and the opposite edge starts moving — making the component
visibly jump and (as in the user's repro) overlap a neighbouring container.

## 2. Background

### 2.1 Resize knobs and the cursor channel

The selection overlay renders eight resize knobs in
`com.servoy.eclipse.designer.rfb/node/src/designer/mouseselection/mouseselection.component.html`.
Each knob is bound to the `ResizeKnobDirective` with a hard-coded `direction`
string from the set `{n, s, e, w, nw, ne, sw, se}`. On `mousedown`, the
directive sets the glasspane cursor to `<direction>-resize`.

The CSS for the knobs themselves
(`com.servoy.eclipse.designer.rfb/node/src/designer/mouseselection/mouseselection.component.css`)
already maps each knob to the matching cursor (`w-resize`, `e-resize`,
`n-resize`, `s-resize`, plus the four corner variants). No CSS changes are
required for this fix.

### 2.2 How the resize direction reaches the dynamic-guides service

`DynamicGuidesService` reads the direction back from the glasspane cursor:

```ts
const resizing = this.editorSession.getState().resizing
    ? this.editorContentService.getGlassPane().style.cursor.split('-')[0]
    : null;
```

So `resizing` is one of `n | s | e | w | nw | ne | sw | se` — the exact knob
is already known.

### 2.3 The faulty direction guess

`pointCloserToTopOrLeftSide` returns whether the cursor is closer to the top
(or left) edge of the component. The pre-fix code used it to decide which
edge of the component was being resized. That works at the start of the drag
but flips as soon as the cursor crosses the centre of the (still resizing)
rectangle.

Reproduction (matches the ticket screenshot): grab the **top-left** knob and
drag toward the bottom-right. As soon as the cursor crosses the component's
midpoint, `pointCloserToTopOrLeftSide` flips, the code switches from "left/top
edges moving" to "right/bottom edges moving", and the component visibly jumps
— `left` snaps back to its original value while `width` grows past the right
edge, overlapping the gray container next to it.

### 2.4 What the resize should actually do per direction

Given a knob direction and a cursor delta `(dx, dy)` from the resize-start
point, the geometry should be:

| direction | `left`    | `top`     | `width` | `height` |
|-----------|-----------|-----------|---------|----------|
| `e`       | unchanged | unchanged | `+dx`   | unchanged |
| `w`       | `+dx`     | unchanged | `-dx`   | unchanged |
| `n`       | unchanged | `+dy`     | unchanged | `-dy`  |
| `s`       | unchanged | unchanged | unchanged | `+dy`  |
| `ne`      | unchanged | `+dy`     | `+dx`   | `-dy`    |
| `nw`      | `+dx`     | `+dy`     | `-dx`   | `-dy`    |
| `se`      | unchanged | unchanged | `+dx`   | `+dy`    |
| `sw`      | `+dx`     | unchanged | `-dx`   | `+dy`    |

### 2.5 Snap target on the opposite edge

When a snap fires during a resize, the guide and the css-anchor must point to
the **opposite** edge from the one being dragged:

| direction(s) | snap source coord | css anchor |
|--------------|-------------------|------------|
| `w`, `nw`, `sw` | cursor `x` ≈ component left | `cssPosition.left` |
| `e`, `ne`, `se` | cursor `x` ≈ component right | `cssPosition.right` |
| `n`, `nw`, `ne` | cursor `y` ≈ component top | `cssPosition.top` |
| `s`, `sw`, `se` | cursor `y` ≈ component bottom | `cssPosition.bottom` |

Per the ticket comment: "we should not set `properties.left` if the cursor is
e-resize. We should just set the width and `guideX` should be set to
`this.rightPos.get(snapX.uuid)`." The mirror holds for `s`.

## 3. Design

### 3.1 Use the resize direction as the single source of truth

Add direction flags inside the resize-relevant methods:

```ts
const resizesLeft   = !!resizing && resizing.indexOf('w') >= 0;
const resizesRight  = !!resizing && resizing.indexOf('e') >= 0;
const resizesTop    = !!resizing && resizing.indexOf('n') >= 0;
const resizesBottom = !!resizing && resizing.indexOf('s') >= 0;
```

Drop the `pointCloserToTopOrLeftSide` calls in `getDraggedElementRect` and
gate the snap-handler branches by direction during resize. Leave
`pointCloserToTopOrLeftSide` itself in place — it is still used by
`addEqualWidthGuides` and `addEqualHeightGuides`, where the equal-size guide
genuinely can fire on either side of the component.

### 3.2 Fix `getDraggedElementRect`

Replace the two `pointCloserToTopOrLeftSide` calls with the direction-driven
version. Dragging the `e` or `s` handle never modifies `left`/`top`; the `w`
and `n` mirrors keep the right/bottom edge stable.

### 3.3 Fix `handleHorizontalSnap`

Two-branch structure today: a "left edge" branch (writes `properties.left`,
sets `cssPosition.left`) and a "right edge" branch (sets `cssPosition.right`).
Gate each branch by `resizesLeft` / `resizesRight` during resize, falling back
to the existing cursor-position behaviour for non-resize.

For `e`-resize:
- do not write `properties.left` — leave it at the rect's unchanged left edge
- compute `width` as `guideX - rect.left` (right snap target minus unchanged left)
- set `cssPosition.right`

For `w`-resize:
- write `properties.left` to the snapped left coordinate
- compute `width` as `rect.right - properties.left` so the right edge stays put
- set `cssPosition.left`

### 3.4 Fix `handleVerticalSnap`

Mirror of 3.3 against `n`/`s`.

### 3.5 Cursor CSS

No changes. The knob CSS and the glasspane cursor are already correct.

## 4. Implementation plan

1. `com.servoy.eclipse.designer.rfb/node/src/designer/services/dynamicguides.service.ts`:
   * `getDraggedElementRect`: replace the two `pointCloserToTopOrLeftSide`
     checks with the direction flags from §3.2.
   * `handleHorizontalSnap`: replace `closerToTheLeft = pointCloserToTopOrLeftSide(...)`
     with the direction-aware version from §3.3. For `e`-resize, drop the
     `properties.left = rect.left` write and recompute width from `rect.left`
     instead of the (now untouched) `properties.left`. For `w`-resize,
     compute width from `rect.right - properties.left`.
   * `handleVerticalSnap`: mirror change for `n`/`s` per §3.4.
   * Leave `pointCloserToTopOrLeftSide` itself in place.
2. Manual smoke test in the form designer following the ticket's repro
   (component with neighbour container to its right; drag from each of the 8
   knobs).
3. Add Karma/Jasmine unit tests under
   `com.servoy.eclipse.designer.rfb/node/src/designer/services/` covering the
   matrix in §2.4 and the snap behaviour in §2.5.
4. Run the RFB designer test suite via the Angular workspace under `node/`.

## 5. Acceptance criteria

- [ ] Resizing a component from the **east** handle changes only `width`; the
  component's `left` and `top` stay equal to their start-of-drag values for
  the entire drag, regardless of cursor position relative to the component
  centre.
- [ ] Resizing from the **south** handle changes only `height`; `left` and
  `top` stay constant.
- [ ] Resizing from the **west** handle moves `left` and changes `width`
  inversely (right edge stays put). Cursor position past the centre does not
  flip the behaviour.
- [ ] Resizing from the **north** handle moves `top` and changes `height`
  inversely (bottom edge stays put).
- [ ] The four corner handles (`nw`, `ne`, `sw`, `se`) follow the matrix in
  §2.4 — each one moves only its two adjacent edges, never the opposite two.
- [ ] When a snap fires during an `e`/`ne`/`se` resize, the rendered guide
  aligns to the snap target's right or left edge using `rightPos`, the
  computed width matches the cursor's snapped position relative to the
  unchanged `left`, and `cssPosition.right` is populated (not
  `cssPosition.left`).
- [ ] Mirror condition holds for `w`/`s`/`n` and the corresponding corners.
- [ ] No "jumping" / opposite-corner movement is observed in the ticket's
  reproduction scenario (selected component with a neighbour container to
  the right; drag the top-left knob toward the bottom-right). The component
  resizes monotonically toward the cursor and does not overlap the
  neighbour container as a side effect of the bug.
- [ ] Knob cursors during drag remain `w-resize`, `e-resize`, `n-resize`,
  `s-resize`, `nw-resize`, `ne-resize`, `sw-resize`, `se-resize`
  respectively — no regression in cursor display.
- [ ] Existing equal-size, equal-distance and middle-axis snapping
  (non-resize drag and resize-with-equal-size paths) continue to work; the
  remaining `pointCloserToTopOrLeftSide` callers in `addEqualWidthGuides` /
  `addEqualHeightGuides` are unchanged.
- [ ] Palette drag (drop a new component onto the form) continues to work as
  before this fix — the cursor-position heuristic still drives that path,
  since the gating only flips during resize.

## 6. Out of scope

- Re-architecting the cursor-as-channel design (reading `glassPane.style.cursor`
  to recover the resize direction). A cleaner approach would expose the
  direction via `EditorSessionService.getState()`, but that is a follow-up.
- Changing the snap-target priority order (currently same-size > edge-snap >
  equal-distance > middle-axis).
- Multi-selection resize behaviour. The ticket and the affected code paths
  already short-circuit when more than one item is selected.
- Touch / non-mouse pointer events.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the resize direction be stored on `EditorSessionService.getState()` instead of recovered from `glassPane.style.cursor.split('-')[0]`? Cleaner, but a larger change. | dev | open |
| Should `addEqualWidthGuides`/`addEqualHeightGuides` also switch from `pointCloserToTopOrLeftSide` to a direction-driven choice during resize? Today they are reached only when `resizing` contains the matching axis letter, so the cursor heuristic isn't necessarily wrong, but it can still flip mid-drag. | dev | open |
