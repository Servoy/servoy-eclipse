# Triage Report — SVY-21706

**Verdict:** PROCEED

## Reported problem

On a new, blank CSS-position form in the NG2 form designer, resizing the form by
dragging its edge is awkward. Three distinct symptoms (the ticket contains **no**
proposed solution — only the open question "can we increase that area a bit? or is it
like this on purpose?"):

1. **Hard to hit the drag handle (bottom / right).** Resizing from the bottom edge or the
   right edge works, but it is hard to aim the mouse so the resize cursor appears; the
   grab area is very thin.
2. **Corner resize does not work at all.** Dragging from the bottom-right corner (to
   resize width and height together) does nothing.
3. **Feels sluggish / slow-motion.** Dragging feels damped rather than free; the user
   expects the edge to follow the mouse 1:1 and fast.

## Root-cause assessment

All three symptoms are in **this project's code** (the RFB designer Angular frontend,
`com.servoy.eclipse.designer.rfb/node/src/designer/`), and each maps to a single concrete
mechanism — none is a misconfiguration or misunderstanding.

### 1. Thin grab area — it is *not* "on purpose", just a 2px element

The form's edge resize handles are two separate sibling elements rendered inside each
ghost container (`ghostscontainer/ghostscontainer.component.html` lines 8 and 22):

- **Right edge:** `designer-resizeeditorwidth` → `.resize-editor-width` is
  **`width: 2px; height: 100%; left: 100%`** (`resizeeditorwidth/resizeeditorwidth.component.css`).
- **Bottom edge:** `designer-resizeeditorheight` → `.resize-editor-height` is
  **`width: 100%; height: 2px`** (`resizeeditorheight/resizeeditorheight.component.css`).

Each handle is a hard 2px strip carrying the `ew-resize` / `ns-resize` cursor and the
`mousedown` listener (`resizeeditorwidth.component.ts:31`, `resizeeditorheight.component.ts:34`).
A 2px-wide target is genuinely hard to aim at — this is the direct cause of symptom 1, and
the answer to the reporter's question is "not on purpose, it is simply a 2px element; the
hit area can be widened."

### 2. No corner handle exists

There is no combined bottom-right ("se") resize element anywhere. Width and height are two
**independent** 2px strips: the width strip spans the right edge (`left:100%`, full
height) and the height strip spans the bottom edge (full width, `height:2px`), rendered as
separate siblings. They do not overlap in the corner, and no element listens for a
diagonal drag. So a corner drag hits neither handle and nothing resizes — this fully
explains symptom 2. This is a missing feature, not a regression.

### 3. "Sluggish" is the autoscroll coupling, not damped math

The move handlers themselves are **1:1**, not slowed:
`onMouseMove` computes `step = event.pageX - mousePoint.x` (width) /
`event.pageY - mousePoint.y` (height) and adds the full delta to `currentPosition`
(`resizeeditorwidth.component.ts:64-82`, `resizeeditorheight.component.ts:77-98`). So the
"slow-motion" feel is not a multiplier.

The sluggishness comes from how resize is wired through the **autoscroll** machinery
(introduced together with form resizing in SVY-17588, commit `c9bc4a9f65`, "increase /
decrease form size using autoscroll areas"):

- On `mousedown` both resizers call `editorSession.registerAutoscroll(this)` and set
  `editorSession.dragging`.
- `AutoscrollComponent` (`autoscroll/autoscroll.component.ts`) reacts via an `effect()` to
  `editorSession.autoscrollTarget()` and flips the autoscroll overlays'
  `pointerEvents` to `'all'` (`:37-40`). Those overlays sit over the editor edges
  (`autoscroll.component.css` has `pointer-events:none` by default, overridden while a
  target is registered).
- Near the edges the autoscroll overlay drives movement through
  `updateLocationCallback(...)` on a `setInterval(..., 50)` timer with a ramped `speed`
  (2 → 15, `:148-170`). That timed, speed-limited cadence — plus the extra pointer-events
  overlay intercepting the mouse — is what feels like slow-motion compared with a free 1:1
  drag, especially on a small blank form where the mouse is close to the autoscroll band.

So symptom 3 is a real behavioural issue in the resize↔autoscroll integration, convergent
on the `AutoscrollComponent` timer/pointer-events path, not a mystery.

## Ticket premise check

The ticket proposes **no fix** — it reports symptoms and asks whether the thin area is
intentional. The premise to validate is only "is this a real, in-code problem worth
addressing?" and the answer is yes for all three parts:

- The thin area is incidental (a 2px element), not a deliberate design constraint, so
  widening the hit area is safe and directly addresses the complaint.
- The corner never worked because no corner handle was ever implemented — adding one is new
  behaviour, not a revert of any intentional decision.
- The sluggishness is a genuine side-effect of routing resize through the autoscroll
  overlay/timer; it can be decoupled without touching the 1:1 move math.

No part of the ticket's framing needs to be overturned; it is a valid, correctly-reported
set of UX defects.

## Approaches considered

1. **Widen the hit area only (symptom 1), minimal.**
   Increase the clickable thickness of `.resize-editor-width` / `.resize-editor-height`
   (e.g. a wider transparent hit zone while keeping the 2px visible line) so the resize
   cursor is easy to land on.
   - *Pros:* smallest change, directly answers the reporter's main question, low risk.
   - *Cons:* leaves symptoms 2 (corner) and 3 (sluggish) unaddressed.

2. **Address all three: widen hit area + add a bottom-right corner handle + decouple
   resize from autoscroll damping (recommended).**
   - Widen both edge handles' grab zones (symptom 1).
   - Add a combined "se" corner handle (a small element at the bottom-right where the two
     strips meet) that drives both the width and height resize paths together (symptom 2).
   - Make edge/corner dragging follow the mouse freely — keep the autoscroll **edge
     auto-scroll** behaviour but stop it from governing the normal in-view drag cadence
     (e.g. only engage the autoscroll timer when genuinely at the viewport edge, and ensure
     the resize overlay doesn't throttle the direct 1:1 move) (symptom 3).
   - *Pros:* fixes every reported symptom; matches user expectation of a free, fast resize
     with a working corner.
   - *Cons:* largest surface; touches `resizeeditorwidth`, `resizeeditorheight`,
     `ghostscontainer` template, and the resize↔`AutoscrollComponent` interaction — needs
     care so the existing "drag past the edge to grow the form via autoscroll" (SVY-17588)
     still works, and new Vitest coverage for the corner handle.

3. **No code change.**
   - *Pros:* zero risk.
   - *Cons:* the complaints are legitimate and reproducible in code (2px targets, missing
     corner, autoscroll-throttled feel). Doing nothing leaves a known-awkward core
     interaction (resizing a brand-new form) in place. Not justified.

## Recommendation

**PROCEED with Approach 2.** All three symptoms have a single, well-understood cause each
and are fixable in the RFB frontend:

- Symptom 1 → widen the grab zones of `.resize-editor-width` / `.resize-editor-height`.
- Symptom 2 → add a bottom-right corner handle that invokes both resize paths.
- Symptom 3 → decouple the in-view 1:1 drag from the autoscroll `setInterval`/`speed`
  cadence and the `pointerEvents:'all'` overlay, while preserving the SVY-17588 edge
  auto-grow behaviour.

If scope must be limited for this Minor-priority ticket, **Approach 1** (widen the hit
area) is the single highest-value slice and the most direct answer to the reporter — but
Approach 2 is what actually resolves the whole ticket. The corner handle (symptom 2) and
the autoscroll decoupling (symptom 3) should be verified with Vitest specs in
`resizeeditorwidth.component.spec.ts` / `resizeeditorheight.component.spec.ts` and the
ghostscontainer template, and manually in a blank CSS-position form.

## Git history findings

- Form-size resizing and the autoscroll coupling were introduced **together** in
  `c9bc4a9f65` — *"SVY-17588 NG2 designer: increase / decrease form size using autoscroll
  areas"* (vidmarian, 2022-11-17), which added `autoscroll.component.*` and reworked both
  `resizeeditorwidth.component.ts` and `resizeeditorheight.component.ts` plus the
  `ghostscontainer` template. This confirms the "sluggish" feel (symptom 3) is an
  intentional *integration with* autoscroll, not an accidental multiplier — so decoupling
  must keep the edge auto-grow use case working.
- The 2px handle thickness and the two-separate-strips layout (symptoms 1 and 2) predate
  the recent Angular-22/zoneless/standalone/OnPush migrations (`856267353e`,
  `7e854931ac`, `82b7d03fb9`, `83e74f6da7`, `72f081d27e`), which only changed the
  component mechanics — none of them touched the handle sizing or added a corner, so no
  intentional "keep it thin / no corner" decision exists to revert.
- No prior `docs/` spec exists for SVY-21706.
