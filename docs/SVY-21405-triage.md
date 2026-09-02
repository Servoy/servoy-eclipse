# Triage Report — SVY-21405

**Verdict:** PROCEED

## Reported problem

In the responsive form editor, when a row / column / container is selected, the
Zoom in (and Zoom out) buttons in the toolbar should become enabled. On master
this is not happening — the Zoom in button stays visually disabled even after a
container is selected.

The ticket describes the symptom only; it does not propose a specific fix.

## Root-cause assessment

This is a real regression caused by an incomplete signal migration during the
RFB zoneless / OnPush conversion.

The zoom buttons are `ToolbarItem` objects rendered by the OnPush
`ToolbarButtonComponent`, which receives the item through an `item` input signal
and reads its disabled state via `isDisabled()`:

- `com.servoy.eclipse.designer.rfb/node/src/designer/toolbar/item/toolbaritem.component.ts:14`
  — `isDisabled()` reads `this.item()!.enabled`.
- `com.servoy.eclipse.designer.rfb/node/src/designer/toolbar/item/toolbarbutton.component.html:3`
  — `[disabled]="isDisabled()"`.

The enable logic runs in `ToolbarComponent.selectionChanged()`:

- `.../toolbar/toolbar.component.ts:1110` — `this.btnZoomIn.enabled = selection.length == 1;`
- `.../toolbar/toolbar.component.ts:1088` and `:198` — `this.btnZoomOut.enabled = this.urlParser.isShowingContainer() != null;`

`selectionChanged()` is invoked from `EditorSessionService.updateSelection()`
(`.../services/editorsession.service.ts:270`) as a plain listener callback — not
an Angular DOM event originating inside the button component.

The `enabled` field on `ToolbarItem` is a **plain mutable boolean/function
field**, not a signal (`.../toolbar/toolbar.component.ts:1220` —
`public enabled: (() => boolean) | boolean`). Mutating `btnZoomIn.enabled = true`
does not change the `ToolbarItem` object's identity, so the OnPush
`ToolbarButtonComponent` whose `item` input points at that object is never marked
dirty, and `isDisabled()` / `[disabled]` is never re-evaluated. The button stays
visually disabled.

Under the previous zone.js setup, zone patching triggered change detection on
every async callback, so the mutation was picked up incidentally. After the move
to zoneless + OnPush this no longer happens.

## Ticket premise check

The ticket premise holds — this is a genuine bug, not expected behaviour or a
user-side issue. It is entirely inside Servoy RFB designer code. The ticket
proposes no particular implementation, so there is no proposed approach to
challenge; only the symptom, which is accurate.

## Approaches considered

1. **Convert `ToolbarItem.enabled` to a signal** (mirroring the SVY-21360 fix for
   `.state`), update the read sites (`isDisabled()`, the two template `[disabled]`
   bindings, the spinner disabled expressions) and the write sites
   (`btnZoomIn.enabled = …`, `btnZoomOut.enabled = …`, `btnToggleDesignMode`,
   `btnShowErrors`).
   - Pros: robust across *every* programmatic mutation path; consistent with the
     already-established `state` signal fix; removes the underlying OnPush
     fragility for good.
   - Cons: `enabled` can be either a boolean or a `() => boolean`, so slightly more
     involved than `state` (which was boolean-only); touches ~6–8 sites.

2. **Call `cdr.markForCheck()` at the end of `selectionChanged()`** (the
   `ChangeDetectorRef` is already injected at `toolbar.component.ts:123`).
   - Pros: minimal, one line; matches the existing pattern used after
     `setupItems()` (`toolbar.component.ts:135`).
   - Cons: patches this one call path only; any other code that mutates
     `.enabled` outside an Angular event keeps the same latent bug.

3. **No code change.**
   - Pros: none.
   - Cons: leaves a real, reproducible regression in a shipping feature. Not valid.

## Recommendation

**PROCEED.** Recommended approach: **option 1 — convert `ToolbarItem.enabled` to a
signal.** It resolves SVY-21405 and eliminates the same class of OnPush staleness
for all `.enabled` mutation paths, staying consistent with the SVY-21360 `.state`
signal fix that was made for exactly this reason. If a minimal, low-risk patch is
preferred for this release, option 2 (`markForCheck()` in `selectionChanged()`)
fixes the reported symptom with one line, at the cost of leaving the field's
underlying fragility in place.

## Git history findings

- `26e5fdfd50` — "SVY-21360 Toggle exploded view button works in reverse [ai]"
  (Diana Bunaciu, 2026-08-26). Migrated `ToolbarItem.state` from a plain boolean
  to `signal<boolean | undefined>` precisely because OnPush children reading it
  through the `item` input were not re-rendered after programmatic mutation. The
  commit message notes that direct clicks masked the bug (the child is marked
  dirty for its own event) while programmatic/init updates stayed stuck. The
  sibling `enabled` field was not migrated — that omission is the direct cause of
  SVY-21405.
- `856267353e` — "migrate RFB to zoneless Angular: signals, remove zone.js [ai]"
  and `83e74f6da7` — "switch all RFB components to ChangeDetectionStrategy.OnPush"
  are the commits that exposed this latent problem (zone.js previously masked it).
- `d62d0d7079d` (2021) introduced the `btnZoomIn.enabled = selection.length == 1`
  logic; it was correct under zone.js and untouched by the migration.
