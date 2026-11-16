# Spec: SVY-21706 — Form designer: resize form by dragging is hard

## 1. Goal

Make resizing a CSS-position form by dragging its edges feel easy and fast in the NG2
(RFB) form designer. Three concrete UX defects reported on a blank form must be fixed:
the edge drag handles are too thin to aim at, there is no bottom-right corner handle to
resize width and height together, and the drag feels sluggish/slow-motion instead of
following the mouse 1:1. All changes are in the RFB Angular frontend
(`com.servoy.eclipse.designer.rfb/node/`) and must preserve the existing SVY-17588
"drag past the viewport edge to grow the form via autoscroll" behaviour.

## 2. Background

### 2.1 Where the edge handles live

Each ghost container renders two sibling resize strips
(`ghostscontainer/ghostscontainer.component.html`):

- `<designer-resizeeditorwidth>` (line 8) — the **right** edge handle.
- `<designer-resizeeditorheight>` (line 22) — the **bottom** edge handle.

Both are only rendered when `!container.propertyName` (i.e. the real form container, not
a property sub-container).

The right strip (`resizeeditorwidth.component.css`) is `width: 2px; height: 100%;
left: 100%; cursor: ew-resize`. The bottom strip (`resizeeditorheight.component.css`) is
`width: 100%; height: 2px; cursor: ns-resize`. Each is a flat 2px element carrying the
resize cursor and a `mousedown` listener. A 2px target is genuinely hard to aim at — this
is the direct cause of symptom 1. There is **no** element at the bottom-right corner where
the two strips meet, so a diagonal corner drag hits nothing — symptom 2.

### 2.2 How a drag works today

On `mousedown` each resizer:
- stores the start `mousePoint`, grabs the ghost containers / content / glasspane,
  computes `currentPosition` from the current ghost container offset size,
- sets `editorSession.dragging.set(true)` and calls `editorSession.registerAutoscroll(this)`,
- attaches `mousemove`/`mouseup` on `editorContentService.getDocument()`.

`onMouseMove` is **1:1**: width uses `step = event.pageX - mousePoint.x` and
height uses `step = event.pageY - mousePoint.y`, adding the full `step` to
`currentPosition` and applying it to the ghost containers, `.content` and the glasspane
(`resizeeditorwidth.component.ts:64-83`, `resizeeditorheight.component.ts:77-98`). So the
move math itself is not damped.

`onMouseUp` clamps to the min limit, sends the new `width` / `height` (and the lowest
part's `y` for height) via `editorSession.sendChanges(...)`, resizes the glasspane, clears
`dragging`, and calls `unregisterAutoscroll(this)`.

Each resizer implements `ISupportAutoscroll` (`editorsession.service.ts:609-614`):
`updateLocationCallback(changeX, changeY)`, `onMouseUp`, `onMouseMove`,
`getAutoscrollLockId()` (`'resize-editor-width'` / `'resize-editor-height'`).

### 2.3 The autoscroll coupling (source of the sluggish feel)

`editorSession.registerAutoscroll(target)` sets `autoscrollTarget` and locks it to the
target's `getAutoscrollLockId()` so only one resizer drives autoscroll at a time
(`editorsession.service.ts:511-522`).

`AutoscrollComponent` (`autoscroll/autoscroll.component.ts`) has four instances, placed
`top`/`bottom`/`left`/`right` over the editor edges (`designer.component.html:24-27`). Its
constructor `effect()` reacts to `autoscrollTarget()`: when a target registers it sets
`editorSession.pointerEvents.set('all')` (otherwise `'none'`). The autoscroll overlay
binds `[ngStyle]="{'pointer-events': editorSession.pointerEvents()}"`
(`autoscroll.component.html`), so while resizing the four 30px edge overlays become
pointer-interactive and sit on top of the editor (`autoscroll.component.css`, `z-index:3`).

When the mouse enters an overlay and moves "forward" (toward the edge), the overlay starts
a `setInterval(..., 50)` timer (`startAutoScroll`, `:164-171`) that repeatedly calls
`scrollTarget.updateLocationCallback(...)` with a ramped `speed` (2 → 15, `autoscroll()`
`:148-162`). The overlay's own `onMouseMove` also forwards `scrollTarget.onMouseMove(event)`
(`:129`). Near the edges of a small blank form the mouse is often already over the band, so
the timer/`speed` cadence and the extra pointer-events overlay intercepting the mouse are
what feels like slow-motion — symptom 3. The 1:1 direct drag only runs cleanly while the
mouse is away from the autoscroll bands.

### 2.4 Git history (carried from triage)

Form-size resizing **and** the autoscroll coupling were introduced together in
`c9bc4a9f65` — *"SVY-17588 NG2 designer: increase / decrease form size using autoscroll
areas"*. The timer/pointer-events design is therefore an intentional *integration with*
autoscroll, not an accidental multiplier — decoupling must keep the edge auto-grow use
case working. The 2px thickness and two-separate-strips layout (symptoms 1 and 2) predate
the later Angular-22/zoneless/standalone/OnPush migrations, which only changed component
mechanics and never touched handle sizing or added a corner. No intentional "keep it
thin / no corner" decision exists to revert.

## 3. Design

All three symptoms are addressed. Components use Angular 22 conventions already in place in
the neighbouring resizer components: standalone, `inject()` at field level, `OnPush`,
`viewChild.required`, arrow-function handlers, 2-space indent, single quotes, no comments.

### 3.1 Symptom 1 — widen the edge hit area (CSS only)

Give each edge strip a wider **transparent** grab zone while keeping the thin visible line,
so the `ew-resize` / `ns-resize` cursor is easy to land on.

- `resizeeditorwidth.component.css` (`.resize-editor-width`): increase the interactive
  width from `2px` to a comfortable grab zone (target ~8px), keep the visible 2px line via
  a pseudo-element or a transparent border/background split, keep `cursor: ew-resize`.
  Because the element sits at `left: 100%` (to the right of the content), widening it must
  not visually push content; center/offset the visible 2px line within the wider hit zone.
- `resizeeditorheight.component.css` (`.resize-editor-height`): same treatment, increase
  interactive height from `2px` to ~8px, keep the visible 2px line, keep `cursor: ns-resize`.

Preferred technique: keep the element's visible background as the 2px line and add a
transparent `::before`/`::after` (or padding + `background-clip`) that extends the
mouse-target without changing the visible 2px appearance or layout. The exact visible
thickness stays 2px; only the clickable area grows.

### 3.2 Symptom 2 — bottom-right corner handle (new component)

Add a new corner resize component following the existing resizer structure:

- New folder `src/designer/resizeeditorcorner/` with
  `resizeeditorcorner.component.{ts,html,css}` and a `.spec.ts`.
- Selector `designer-resizeeditorcorner`, standalone, `OnPush`, `inject()` for
  `Renderer2`, `EditorSessionService`, `EditorContentService`, same imports pattern.
- Template: a single small square element (e.g. `~12px`) positioned at the bottom-right
  corner of the ghost container (`position: relative; left: 100%; top: 100%` with a
  negative translate, or absolute to the container bottom-right), `cursor: nwse-resize`,
  same `#element`/`viewChild.required` pattern.
- Rendered in `ghostscontainer.component.html` inside the same
  `@if (!container.propertyName)` block, as a third sibling alongside
  `designer-resizeeditorwidth` and `designer-resizeeditorheight`.

Behaviour: the corner drives **both** width and height resize simultaneously. Rather than
duplicate the (non-trivial) width/height math, the corner should **delegate to the two
existing resize paths** so the exact same form-size / glasspane / lowest-part / `sendChanges`
logic runs for each axis. Two viable implementations — pick the one that keeps the width
and height logic in a single place:

- **Option A (preferred): extract shared resize logic.** Pull the per-axis
  mousedown-setup, `onMouseMove` step application, and `onMouseUp` `sendChanges` from
  `ResizeEditorWidthComponent` / `ResizeEditorHeightComponent` into reusable units
  (e.g. small helper classes/functions or a shared base) that the corner composes for both
  axes. The width and height components keep their current public behaviour by using the
  same units.
- **Option B: corner holds references to the two axis resizers** (via `viewChild` on the
  ghost container or shared service) and forwards `mousedown`/`move`/`up` to both. Simpler
  to write but couples the corner to the sibling components' internals.

Whichever is chosen, on a corner drag:
- `step_x = event.pageX - startX` drives the width path, `step_y = event.pageY - startY`
  drives the height path, each 1:1 and independent, so a diagonal drag resizes width and
  height together.
- On `mouseup` it must emit the width change **and** the height change (and the lowest
  part's `y`) via `editorSession.sendChanges(...)` — matching what the two edge handles do
  individually — and resize the glasspane for both dimensions.
- It participates in autoscroll the same decoupled way as §3.3 (register/unregister), using
  its own `getAutoscrollLockId()` (e.g. `'resize-editor-corner'`); it may need
  `updateLocationCallback` to update both axes if the corner is dragged past an edge.

### 3.3 Symptom 3 — decouple the in-view 1:1 drag from autoscroll damping

Keep the SVY-17588 edge auto-grow behaviour (dragging past the viewport edge grows the
form via the autoscroll timer) but stop the autoscroll overlay/timer from governing the
normal in-view drag, which is what makes it feel sluggish. The 1:1 move math is already
correct and must not change.

Target the two mechanisms identified in triage:

1. **The `pointerEvents:'all'` overlay intercepting in-view moves.** While a resize target
   is registered, the four autoscroll overlays become pointer-interactive
   (`autoscroll.component.ts:37-40` + `autoscroll.component.html`). Near a small form the
   overlay catches moves that should be handled by the direct document `mousemove`
   listener, routing the drag through the overlay's `onMouseMove`/timer path. Only make the
   overlay pointer-interactive when it is actually needed to auto-scroll — i.e. do not blanket
   `pointerEvents.set('all')` for the whole drag; keep the band inert for a normal in-view
   drag and only engage it when the pointer genuinely reaches the edge band. The direct
   1:1 document `mousemove` handler on the resizer must remain the path that moves the edge
   while the mouse is inside the viewport.

2. **The `setInterval(...,50)` + ramped `speed` cadence governing movement.** The timed,
   speed-limited `autoscroll()` loop (`:148-171`) should only drive movement while the
   mouse is actually in the edge band and moving toward the edge (true auto-scroll). It must
   not be the mechanism that advances a normal in-view drag. Ensure that when the pointer is
   back inside the viewport, `stopAutoscroll()` is in effect and the direct 1:1 handler
   resumes, so the edge follows the mouse immediately with no timer/speed throttle.

Net effect: a normal drag (mouse inside the editor) follows the mouse 1:1 and fast; only
when the user deliberately drags into the 30px edge band does the autoscroll timer kick in
to grow the form past the viewport (unchanged SVY-17588 behaviour).

Implementation note: changes concentrate in `autoscroll/autoscroll.component.ts` (when to
flip `pointerEvents`, when to start/stop the interval), possibly
`editorsession.service.ts` (`pointerEvents` signal semantics / a flag distinguishing
"registered for autoscroll" from "actively auto-scrolling"), and must not alter the 1:1
`onMouseMove` math in the resizer components. Preserve `registerAutoscroll` /
`unregisterAutoscroll` and the lock-by-`getAutoscrollLockId()` behaviour so the corner and
edge handles don't fight over the autoscroll target.

### 3.4 Files touched

- `src/designer/resizeeditorwidth/resizeeditorwidth.component.css` — widen hit area (§3.1).
- `src/designer/resizeeditorheight/resizeeditorheight.component.css` — widen hit area (§3.1).
- `src/designer/resizeeditorwidth/resizeeditorwidth.component.ts` — only if §3.2 Option A
  extracts shared logic; otherwise unchanged.
- `src/designer/resizeeditorheight/resizeeditorheight.component.ts` — same as above.
- `src/designer/resizeeditorcorner/resizeeditorcorner.component.{ts,html,css}` — **new**
  corner handle (§3.2) + `resizeeditorcorner.component.spec.ts`.
- `src/designer/ghostscontainer/ghostscontainer.component.html` — render the corner handle
  (§3.2); `ghostscontainer.component.ts` only if it must expose the sibling resizers for
  §3.2 Option B.
- `src/designer/autoscroll/autoscroll.component.ts` (and `.css` if needed) — decouple
  pointer-events/timer from the in-view drag (§3.3).
- `src/designer/services/editorsession.service.ts` — only if §3.3 needs a flag to
  distinguish "registered" from "actively auto-scrolling".

## 4. Implementation plan

1. **Symptom 1 (CSS):** widen the grab zone of `.resize-editor-width` and
   `.resize-editor-height` while keeping the visible 2px line and correct cursors; verify
   no layout shift of the form content.
2. **Symptom 3 (decouple):** in `AutoscrollComponent`, stop blanket-enabling
   `pointerEvents:'all'` for the whole drag and stop the `setInterval`/`speed` cadence from
   driving in-view moves; only engage the band/timer when the pointer is actually in the
   edge band moving toward the edge. Keep edge auto-grow (SVY-17588) working. Do not touch
   the resizers' 1:1 `onMouseMove` math.
3. **Symptom 2 (corner handle):** create `resizeeditorcorner` component; choose §3.2
   Option A (extract shared per-axis resize logic) or Option B (forward to siblings);
   render it in `ghostscontainer.component.html` in the `!container.propertyName` block;
   wire its autoscroll participation with lock id `'resize-editor-corner'`.
4. **Tests (Vitest):** add `resizeeditorcorner.component.spec.ts`; extend
   `resizeeditorwidth.component.spec.ts` / `resizeeditorheight.component.spec.ts` and/or
   `autoscroll.component.spec.ts` to cover the decoupled in-view vs. edge behaviour; update
   `ghostscontainer.component.spec.ts` if the template assertion changes. Follow the
   existing `Object.create(Class.prototype)` + manual-mock unit pattern; add a
   `*.browser.spec.ts` only if real-DOM pointer-events behaviour must be verified.
5. **Verify:** `npm run lint` (zero warnings), `npm run build_debug_nowatch`, `npm test`
   (and `npm run test:browser` if a browser spec is added) from
   `com.servoy.eclipse.designer.rfb/node/`.
6. **Manual check:** on a new blank CSS-position form, confirm the right/bottom edges are
   easy to grab, the bottom-right corner resizes both dimensions, the drag follows the
   mouse 1:1 and fast, and dragging past the viewport edge still grows the form.

## 5. Acceptance criteria

- [ ] The right and bottom edge resize handles have a noticeably larger clickable grab zone
      (cursor appears without precise aiming) while the visible line stays ~2px and the form
      content does not shift.
- [ ] `ew-resize` is shown over the right edge, `ns-resize` over the bottom edge, and
      `nwse-resize` over the new bottom-right corner handle.
- [ ] Dragging the bottom-right corner resizes **both** width and height together, and on
      release sends both the `width` and `height` changes (plus the lowest part's `y`) via
      `sendChanges`, matching the two edge handles.
- [ ] A normal in-view drag (edge or corner) follows the mouse 1:1 with no slow-motion feel;
      the move math is unchanged.
- [ ] Dragging past the viewport edge still grows the form via autoscroll (SVY-17588
      behaviour preserved), and only then does the timed autoscroll engage.
- [ ] Only one resizer drives autoscroll at a time (lock-by-`getAutoscrollLockId()` still
      holds for width, height, and the new corner).
- [ ] `npm run lint` passes with zero warnings; `npm run build_debug_nowatch` compiles;
      `npm test` passes, including the new corner-handle and decoupling tests.

## 6. Out of scope

- Resizing from the top or left edge (only right/bottom/corner are reported and currently
  supported).
- Responsive-layout form resizing (this concerns CSS-position forms and the ghost
  container edge handles).
- Any change to the server-side Java designer, websocket protocol, or `sendChanges`
  payload shape (only existing `width`/`height`/`y` changes are reused).
- Snapping / dynamic guides behaviour during resize.
- Keyboard-based resizing.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| §3.2: prefer Option A (extract shared per-axis resize logic) or Option B (corner forwards to sibling resizers)? Option A is recommended for a single source of truth. | dev | open |
| Exact grab-zone width for the edges (~8px proposed) and corner size (~12px proposed) — tune for feel during manual testing. | dev / reporter | open |
| Should the new corner handle also support auto-grow past the corner (diagonal autoscroll), or only in-view diagonal resize for now? Assumed: in-view diagonal resize is the priority; edge auto-grow per-axis is a bonus. | dev | open |
