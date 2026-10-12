# Spec: SVY-21581 — Context menu is not completely visible in form editor

## 1. Goal
Fix the form-editor (RFB / Angular designer) context menu so that the **first** right-click
near the bottom of the form shows a fully-visible menu that flips up and stays inside the
canvas, exactly as subsequent right-clicks already do. Today the first open measures the
menu before Angular has rendered its items (a zoneless change-detection timing bug), reads a
near-zero height, and therefore neither flips the menu up nor clamps it — leaving its lower
entries clipped behind the console/status bar. The fix forces the menu DOM to render before
`adjustMenuPosition` measures it, so the existing (correct) geometry logic has a real height
to work with on the very first open.

## 2. Background

### 2.1 Where the menu lives
The context menu is `ContextMenuComponent` at
`com.servoy.eclipse.designer.rfb/node/src/designer/contextmenu/contextmenu.component.ts`
(standalone, `ChangeDetectionStrategy.OnPush`). Its items are rendered by the template's
`@for (item of menuItems)` block in `contextmenu.component.html`. The component is 989 lines;
`menuItems` is a plain, mutated class field (`menuItems!: ContextmenuItem[]`, line 36) — it is
**not** a signal.

### 2.2 How a right-click positions the menu
A `contextmenu` DOM listener is registered on the content area inside `setup()`
(`addEventListener('contextmenu', …)`, line 61). On each right-click it:

1. Rebuilds/mutates `menuItems` (`push`/`splice`/`filter`) to reflect the current selection,
   the Add submenu, developer menus and the optional Cypress entry (lines ~98–226).
2. Calls `this.show(event)` (lines 249–265): sets `display:block`, positions the menu at the
   raw cursor (`event.pageX/pageY`), and calls `this.cdr.markForCheck()` (line 250).
3. Calls `adjustMenuPosition(this.element().nativeElement)` (lines 228 and 232; impl 271–337),
   which reads `nativeElement.offsetHeight` / `offsetWidth` to decide whether to flip the menu
   **up** (`if (bounds.bottom > viewport.bottom) top -= offsetHeight - 10`, lines 295–298) and
   to clamp it inside the viewport (`maxTop = viewport.bottom - offsetHeight - statusBarHeight`,
   lines 292, 299–304).

Both `show()` and `adjustMenuPosition()` run **twice** — once in the success branch and once
in the `.catch` branch of the `hasCypressFormTest()` promise (lines 227–233).

### 2.3 Root cause (from triage, confirmed)
`show()` only calls `markForCheck()`. Under `ChangeDetectionStrategy.OnPush` **and** the
project's `provideZonelessChangeDetection()`, `markForCheck()` merely *schedules* a change
detection pass — it does not render synchronously. So when `adjustMenuPosition` runs on the
same tick and reads `offsetHeight`, the freshly-rebuilt `menuItems` have **not yet** been
materialised into `<li>` rows on a first-time-populated menu. `offsetHeight` is therefore
~0 / far smaller than the real menu, so:

- `bounds.bottom > viewport.bottom` is false → the menu is **not** flipped up;
- `maxTop` is computed with a near-zero height → the clamp does not pull it up either.

The menu hangs downward from the cursor and is clipped by the console/status panel. On the
**second** right-click the `<li>` rows already exist in the DOM from the first pass, so
`offsetHeight` is correct and the flip-up/clamp work — exactly matching the "first time only"
symptom in the ticket.

This is a regression from the zoneless Angular migration (`856267353e` "migrate RFB to
zoneless Angular", 2026-08-06, and follow-up `a65221abed`). Under the previous zone.js model,
`markForCheck()` effectively rendered synchronously within the event turn, so the subsequent
`offsetHeight` read was valid. The `adjustMenuPosition` logic (lvostinar, `ac55cc7031d`,
2025-06-25) is correct and unchanged; only the render-timing assumption underneath it broke.

### 2.4 Git history
- `adjustMenuPosition` flip-up/clamp math: `ac55cc7031d` (lvostinar, 2025-06-25) — unchanged
  and correct given a valid `offsetHeight`.
- Prior context-menu positioning fix (tabpanel/splitpane): `3ccc827e8d` (SVY-20703) — predates
  the regression.
- Regression introduced by the zoneless migration: `856267353e` (2026-08-06) and follow-up
  `a65221abed`, which reduced `show()` to a `markForCheck()` (schedules, does not render).
- `node/AGENTS.md` records `contextmenu.component.ts` as **not yet** migrated to signals
  (`menuItems` mutated via push/splice/filter), the direct enabler of the measure-before-render
  defect.

## 3. Design

### 3.1 Approach — force a synchronous render before measuring (approved Approach 1)
Keep the existing geometry logic in `adjustMenuPosition` **unchanged**. The only change is to
guarantee the menu's `@for` items are in the DOM before `adjustMenuPosition` reads
`offsetHeight`. Do this with a **synchronous** change-detection pass immediately after
`show(event)` populates/`markForCheck`es the menu, then call
`adjustMenuPosition(this.element().nativeElement)`.

Mechanism (preferred, deterministic, no visible flicker):

- Use the already-injected `ChangeDetectorRef` (`this.cdr`, line 45) and call
  `this.cdr.detectChanges()` right after `show(event)`, before `adjustMenuPosition(...)`.
  `detectChanges()` synchronously renders this OnPush component's view, materialising the
  rebuilt `menuItems` into `<li>` rows, so `offsetHeight` is correct on the first open.

The synchronous-CD variant is deliberately chosen over `requestAnimationFrame` /
`afterNextRender` so there is **no** visible one-frame downward flash before the menu flips up.

### 3.2 Where to apply it
The `hasCypressFormTest()` promise has two branches that each `show()` then
`adjustMenuPosition()` (lines 227–233). The render step must be inserted in **both**:

```ts
// success branch (after the optional Cypress entry is pushed)
this.show(event);
this.cdr.detectChanges();        // NEW: render @for items before measuring
this.adjustMenuPosition(this.element().nativeElement);
```

```ts
}).catch(() => {
    // backend call failed — still show the menu without the Cypress item
    this.show(event);
    this.cdr.detectChanges();    // NEW: render @for items before measuring
    this.adjustMenuPosition(this.element().nativeElement);
});
```

### 3.3 Why not just make `menuItems` a signal
Converting `menuItems` (and the per-item class/style) to signals is the right long-term
direction (and is flagged in `node/AGENTS.md`), but it is a large, high-risk refactor of a
989-line component whose items are built by heavy array mutation inside the event handler.
It is explicitly **out of scope** here; even after that refactor an explicit "measure after
render" step would still be needed, so it does not replace this fix. Track it separately.

### 3.4 Behavioural safety
- The change is additive and localized: one `detectChanges()` call in each of the two
  branches. No geometry, offsets, CSS, or item-building logic changes.
- A synchronous `detectChanges()` on an OnPush component with a small, static menu template is
  cheap and runs inside the existing right-click event turn.
- Subsequent right-clicks are unaffected (they already measured correctly); they simply render
  synchronously now too, which is harmless.

## 4. Implementation plan

All edits to files under the Angular RFB bundle go through the standard Angular/Vitest flow
(not the Java/Tycho flow). Follow `com.servoy.eclipse.designer.rfb/node/AGENTS.md`.

1. **Edit** `com.servoy.eclipse.designer.rfb/node/src/designer/contextmenu/contextmenu.component.ts`:
   - In the `hasCypressFormTest()` **success** branch (around lines 227–228), insert
     `this.cdr.detectChanges();` between `this.show(event);` and
     `this.adjustMenuPosition(this.element().nativeElement);`.
   - In the `.catch` branch (around lines 229–233), insert the same `this.cdr.detectChanges();`
     between `this.show(event);` and `this.adjustMenuPosition(this.element().nativeElement);`.
   - `ChangeDetectorRef`/`this.cdr` is already imported and injected (lines 1 and 45) — no new
     imports needed.
   - Leave `adjustMenuPosition`, `show`, `getElementOffset`, and all item-building logic
     unchanged.
2. **Add a regression test** at
   `com.servoy.eclipse.designer.rfb/node/src/designer/contextmenu/contextmenu.component.spec.ts`
   (the file already exists — add a new `describe` block). The test must reproduce the
   first-open scenario and assert the fix:
   - Build a `ContextMenuComponent` following the existing unit-test pattern in that file
     (`Object.create(ContextMenuComponent.prototype)` with manually injected mocks for
     `editorSession`, `editorContentService`, `urlParser`, `windowRef`, and `cdr`), consistent
     with the mocks already set up at the top of the spec.
   - Give the component an `element()` whose `nativeElement` is a real element whose
     `offsetHeight`/`offsetWidth` are controllable (e.g. define `offsetHeight`/`offsetWidth`
     getters, or use a stub element), modelling a tall, populated menu positioned near the
     bottom of the viewport so that `bounds.bottom > viewport.bottom`.
   - **Core assertion (flip-up on first open):** with a non-zero menu `offsetHeight` and the
     menu's top near the bottom of the viewport, call `adjustMenuPosition(nativeElement)` and
     assert the resulting `style.top` is **reduced** (flipped up) relative to the raw cursor
     `top` — i.e. the branch `top -= offsetHeight - 10` fired and/or the `maxTop` clamp pulled
     it inside the viewport. This is the behaviour that is broken when the height is read as 0.
   - **Regression guard for the root cause:** assert that `detectChanges()` is invoked
     **before** the height is measured. Concretely, spy on `cdr.detectChanges` and on
     `adjustMenuPosition`, drive the `contextmenu` handler path (or assert ordering via the
     mock call order), and verify `detectChanges` is called before `adjustMenuPosition` reads
     the element — in both the success and `.catch` branches of `hasCypressFormTest()`
     (one test with `hasCypressFormTest` resolving, one with it rejecting). A pre-fix build
     (no `detectChanges()` call) fails this ordering/flip-up assertion.
   - Follow the project's zoneless + jsdom testing rules in `node/AGENTS.md`: do **not** use
     `vi.stubGlobal('document'/'window', …)`; mock individual methods and restore them.
3. **Verify** per `node/AGENTS.md` "After Every Code Change", from
   `com.servoy.eclipse.designer.rfb/node`:
   - `npm run lint` — zero errors.
   - `npm run build_debug_nowatch` — compiles clean.
   - `npm test` — the full Vitest suite passes, including the new regression test.

## 5. Acceptance criteria
- [ ] The **first** right-click near the bottom of the form opens the context menu fully
      visible — it flips up and is clamped inside the canvas, matching the behaviour of
      subsequent right-clicks (addresses the ticket symptom).
- [ ] `adjustMenuPosition` measures a correct, non-zero menu height on the first open because
      the menu's `@for` items are rendered (via a synchronous `cdr.detectChanges()`) before the
      measurement.
- [ ] The synchronous-render step is applied in **both** the success and `.catch` branches of
      the `hasCypressFormTest()` promise.
- [ ] No change to the geometry logic in `adjustMenuPosition`, to `show()`'s positioning, or to
      the CSS/offsets — the fix is timing-only.
- [ ] There is **no** visible one-frame downward flash when the menu opens (synchronous CD, not
      `requestAnimationFrame`/`afterNextRender`).
- [ ] A Vitest regression test at
      `src/designer/contextmenu/contextmenu.component.spec.ts` reproduces the first-open
      bottom-of-form scenario, asserts the menu flips up with a non-zero height, and asserts
      `detectChanges()` runs before `adjustMenuPosition` measures — in both promise branches.
      The test fails on the pre-fix code and passes on the fix.
- [ ] `npm run lint`, `npm run build_debug_nowatch`, and `npm test` all pass with zero errors
      in `com.servoy.eclipse.designer.rfb/node`.

## 6. Out of scope
- Migrating `menuItems` (and per-item class/style) to signals / finishing the zoneless
  migration of `ContextMenuComponent` (track as a separate cleanup).
- Any change to the flip-up/clamp geometry, pixel offsets, or CSS of the context menu.
- Submenu positioning behaviour (the `else` branch of `adjustMenuPosition` for
  `.dropdown-submenu:hover`).
- Context-menu behaviour outside the form editor / other designers.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| None — triage root cause confirmed and approach approved; scope is fully bounded. | — | resolved |
