# Triage Report — SVY-21581

**Verdict:** PROCEED

## Reported problem
In the Servoy form editor (RFB / Angular designer), the **first** time the user
right-clicks near the **bottom** of the form, the context menu opens *downward* from the
cursor and its lower items are clipped by the console/status panel — the menu is not
completely visible. The attached `screenshot.jpg` shows the menu extending below the
canvas, its bottom entries hidden behind the "Command Console" bar. Subsequent
right-clicks behave correctly (the menu flips up and is fully visible).

The ticket proposes **no** solution — it only describes the symptom, including the key
detail that it happens *the first time*.

## Root-cause assessment
The context menu is `ContextMenuComponent`
(`com.servoy.eclipse.designer.rfb/node/src/designer/contextmenu/contextmenu.component.ts`).
Its positioning works in three steps inside the `contextmenu` DOM handler registered in
`setup()`:

1. The `menuItems` array is (re)built and mutated — `push`/`splice`/`filter` — to reflect
   the current selection, Add submenu, developer menus, Cypress entry, etc.
   (lines ~98–226). These items are rendered by the template's `@for (item of menuItems)`
   block (`contextmenu.component.html:3`).
2. `show(event)` (lines 249–265) sets `display:block` and positions the menu at the raw
   cursor (`event.pageX/pageY`). It calls `this.cdr.markForCheck()` (line 250).
3. `adjustMenuPosition(this.element().nativeElement)` (lines 228/232, impl 271–337)
   immediately reads `nativeElement.offsetHeight` and `offsetWidth` to decide whether to
   flip the menu **up** (`if (bounds.bottom > viewport.bottom) top -= offsetHeight - 10`,
   lines 295–298) and to clamp it inside the viewport (`maxTop = viewport.bottom -
   offsetHeight - statusBarHeight`, lines 292, 299–304).

The flip-up and clamp logic is entirely correct **when the menu's rendered height is known
at measurement time**. The bug is that on the first open it is not.

`show()` only calls `markForCheck()`, which under `ChangeDetectionStrategy.OnPush` **and**
the project's `provideZonelessChangeDetection()` merely *schedules* a change-detection
pass — it does not render synchronously. So when `adjustMenuPosition` runs on the same
tick and reads `offsetHeight`, the freshly-rebuilt `menuItems` have **not yet been
materialised** into `<li>` rows for a first-time-populated menu. `offsetHeight` is
therefore ~0 / far smaller than the real menu, so:

- `bounds.bottom > viewport.bottom` is false → the menu is **not** flipped up;
- `maxTop` is computed with a near-zero height, so the clamp does not pull it up either.

The menu is left hanging downward from the cursor and gets clipped. On the **second**
right-click the `<li>` rows already exist in the DOM from the first pass, so `offsetHeight`
is now correct and the flip-up/clamp work — exactly matching the "first time" symptom in
the ticket.

This is a regression from the **zoneless Angular migration**. Under the previous zone.js
model, `markForCheck()` effectively led to a synchronous render within the event turn, so
the subsequent `offsetHeight` read was valid. The `adjustMenuPosition` logic
(authored by lvostinar, commit `ac55cc7031d`, 2025-06-25) was written against that
assumption and is unchanged. The assumption was broken by:

- `856267353e` "migrate RFB to zoneless Angular: signals, remove zone.js" (2026-08-06)
- `a65221abed` "fix RFB zoneless change detection: convert template state to
  signals/markForCheck" (follow-up)

`menuItems` is still a plain mutated class field (not a signal) read by the template —
`node/AGENTS.md` explicitly lists `contextmenu.component.ts` under "Remaining: Component
State → Signals", confirming its template state was not fully converted during the
migration. The reported Servoy 2026.09 is the release line that shipped the zoneless
migration, consistent with a regression introduced there.

## Ticket premise check
The ticket proposes no fix, so there is no premise to overturn — but the *framing* ("shown
downwards, not completely visible") could mislead toward tweaking the CSS or the pixel
offsets in `adjustMenuPosition`. That would be wrong: the geometry math is correct. The
real defect is a **timing** problem — the menu is measured before Angular has rendered its
items under zoneless change detection. The fix must ensure measurement happens *after* the
menu DOM is laid out, not adjust the offsets.

## Approaches considered
1. **Measure after render — force synchronous CD, then position** (recommended).
   In the `contextmenu` handler, after rebuilding `menuItems` and calling `show(event)`,
   render the menu DOM before `adjustMenuPosition` reads `offsetHeight`. Options, in order
   of preference:
   - Inject `ApplicationRef`/`ChangeDetectorRef` and call `detectChanges()` on the
     component (or `appRef.tick()`) immediately after `show()` so the `@for` items are in
     the DOM, then call `adjustMenuPosition`. Deterministic, no visible flicker.
   - Or defer the `adjustMenuPosition` call to after the next render using
     `afterNextRender`/`afterRender` (Angular's zoneless-friendly render hooks), or a
     `requestAnimationFrame`. Simple, but introduces a one-frame reposition the user may
     see as a flicker at the bottom of the form.
   - Pros: targets the actual root cause; small, localized change; aligns the code with
     the zoneless model. Cons: must pick the render-timing mechanism carefully to avoid a
     visible jump; needs a regression test that asserts measurement happens post-render.
2. **Convert `menuItems` (and the per-item class/style) to signals.**
   Finish the migration flagged in `node/AGENTS.md` so template state updates drive CD
   predictably. Pros: removes the whole class of "mutated field read before render" bugs
   in this component; the right long-term direction. Cons: large refactor of a 989-line
   component whose items are built by heavy array mutation in the event handler — high
   risk/effort for a Minor bug; still would need an explicit "measure after render" step,
   so it does not by itself guarantee a synchronous DOM before `offsetHeight` is read.
3. **No code change.**
   Pros: none. Cons: a real, reproducible, user-visible regression against the shipped
   2026.09/2026.12 line; the menu is genuinely unusable (clipped actions) on the first
   bottom-of-form right-click. Not acceptable.

## Recommendation
**PROCEED** with Approach 1: make the menu's DOM render before `adjustMenuPosition`
measures it. Concretely, after `show(event)` populates/`markForCheck`es the menu, force a
synchronous change-detection pass (e.g. a `ChangeDetectorRef.detectChanges()` /
`ApplicationRef.tick()`) so the `@for` menu items exist in the DOM, then call
`adjustMenuPosition(this.element().nativeElement)` — in **both** the success and the
`.catch` branches of the `hasCypressFormTest()` promise (lines 227–233). Prefer the
synchronous-CD variant over `requestAnimationFrame`/`afterNextRender` to avoid a visible
one-frame downward flash before the flip-up. Keep the existing geometry logic unchanged.

Alternatives if the team prefers: Approach 2 (signal migration of `menuItems`) as a
larger, separate cleanup — but it should not block this fix, and it still needs the
post-render measurement step.

A regression test should be added at
`com.servoy.eclipse.designer.rfb/node/src/designer/contextmenu/contextmenu.component.spec.ts`
asserting that, with a populated/clipped menu, `adjustMenuPosition` reads a non-zero
height and flips the menu up when `bounds.bottom > viewport.bottom` (i.e. the top is
reduced), reproducing the first-open scenario.

## Git history findings
- `adjustMenuPosition` positioning logic: `ac55cc7031d` (lvostinar, 2025-06-25) — the
  flip-up/clamp math; unchanged since, and correct given a valid `offsetHeight`.
- `3ccc827e8d` SVY-20703 — prior context-menu positioning fix (tabpanel/splitpane),
  predates the regression.
- Zoneless migration that broke the synchronous-render assumption:
  `856267353e` (2026-08-06, "migrate RFB to zoneless Angular") and follow-up
  `a65221abed` ("fix RFB zoneless change detection"). `show()` was reduced to a
  `markForCheck()` call (schedules, does not render).
- `node/AGENTS.md` records `contextmenu.component.ts` as **not yet** migrated to signals
  ("`menuItems` array mutated via push/splice/filter in contextmenu handler"), which is
  the direct enabler of the measure-before-render defect.
- No existing `docs/SVY-21581-*.spec.md` was found.
