# Spec: SVY-21405 — Zoom in/out toolbar buttons not enabling on selection

## 1. Goal

In the responsive form editor, the toolbar's *Zoom in* and *Zoom out* buttons must
enable/disable in response to selection and container-navigation changes. On master they
stay visually disabled even after a row/column/container is selected. This spec makes the
`ToolbarItem.enabled` field reactive (a signal) so that programmatic mutations of a
button's enabled state are picked up by the OnPush toolbar-button components and reflected
in the DOM.

## 2. Background

### 2.1 The regression

The RFB designer frontend was migrated to zoneless Angular with `OnPush` change detection
(`856267353e` — signals/remove zone.js; `83e74f6da7` — OnPush on all components). Under the
old zone.js setup, zone patching triggered change detection on every async callback, so a
plain field mutation such as `this.btnZoomIn.enabled = true` was picked up incidentally.
After the zoneless/OnPush move this no longer happens: mutating a plain field on an object
does not change the object's identity, so an OnPush child whose `item` input points at that
object is never marked dirty and its bindings are never re-evaluated.

### 2.2 The sibling fix (SVY-21360) as the pattern to mirror

Commit `26e5fdfd50` ("SVY-21360 Toggle exploded view button works in reverse [ai]") fixed
exactly this class of bug for the sibling `state` field by converting it from a plain
boolean to a signal:

- `toolbar.component.ts:1213` — `readonly state = signal<boolean | undefined>(undefined);`
- Read sites use `item()!.state()` (template) and `this.btnX.state()` (component).
- Write sites use `this.btnX.state.set(value)`.

The `enabled` field was left as a plain mutable field, which is the direct cause of
SVY-21405.

### 2.3 Current shape of the `enabled` field

`ToolbarItem` (`toolbar.component.ts:1197`) declares `enabled` as a constructor parameter:

```ts
constructor(
    public text: string,
    public icon: string | null,
    public enabled: (() => boolean) | boolean,
    public onclick: ((text?: string) => void) | null) {
}
```

Unlike `state` (boolean-only), `enabled` can be **either** a boolean **or** a
`() => boolean`. Most buttons pass a boolean (e.g. `btnZoomIn` / `btnZoomOut` at
`:432` / `:442` pass `false`); at least one passes a function (`btnSetMaxLevelContainer`
at `:451` passes `() => this.editorSession.showWireframe()`). The signal migration must
preserve support for both forms.

### 2.4 Read and write sites

**Read sites** (evaluate `enabled`):
- `toolbaritem.component.ts:14-17` — `isDisabled()` reads `this.item()!.enabled` and handles
  the function-or-boolean union: `typeof(enabled) == 'function' ? !enabled() : !enabled`.
- `toolbarbutton.component.html:3` — `[disabled]="isDisabled()"` (non-list button).
- `toolbarbutton.component.html:11` — `[disabled]="!item()!.enabled"` (list button, no icon-style).
- `toolbarbutton.component.html:14` — `[disabled]="!item()!.enabled"` (list dropdown toggle).
- `toolbarspinner.component.html:2,5,6` — spinner buttons/input read `isDisabled()` (inherited
  from `ToolbarItemComponent`), so they are covered by the `isDisabled()` change alone.

**Write sites** (mutate `enabled`):
- `toolbar.component.ts:145` — `this.btnToggleDesignMode.enabled = false;`
- `toolbar.component.ts:198` — `this.btnZoomOut.enabled = this.urlParser.isShowingContainer() != null;`
- `toolbar.component.ts:268` — `this.btnShowErrors.enabled = true;`
- `toolbar.component.ts:1088` — `this.btnZoomOut.enabled = this.urlParser.isShowingContainer() != null;`
- `toolbar.component.ts:1108` — `this.btnMoveUp.enabled = selection.length == 1;`
- `toolbar.component.ts:1109` — `this.btnMoveDown.enabled = selection.length == 1;`
- `toolbar.component.ts:1110` — `this.btnZoomIn.enabled = selection.length == 1;`
- (The many commented-out `//this.btnXxx.enabled = …` lines around `:1085`–`:1106` are dead
  code and are left as-is.)

`selectionChanged()` — which contains the `:1088`/`:1108`–`:1110` writes — is invoked from
`EditorSessionService.updateSelection()` as a plain listener callback, i.e. outside any
Angular DOM event, which is exactly why the mutation is not otherwise detected.

**Constructor call sites** passing the `enabled` argument (all `new ToolbarItem(...)` calls
in `setupItems()`, roughly `:279`–`:1018`): each still passes a boolean or a `() => boolean`
as today; the constructor wraps that value into the signal, so the call sites do not change.

## 3. Design

### 3.1 Convert `enabled` to a signal

Change `ToolbarItem` so `enabled` is a `signal` holding the existing union type. Because
`enabled` is a constructor parameter, remove it from the constructor's `public` parameter
list and instead initialise a field signal from the passed value, mirroring how `state`
is a `readonly` signal field.

```ts
export class ToolbarItem {
    // …existing fields…
    readonly state = signal<boolean | undefined>(undefined);
    readonly enabled = signal<(() => boolean) | boolean>(false);
    tooltip!: string;
    onSet!: (value: unknown) => void;

    constructor(
        public text: string,
        public icon: string | null,
        enabled: (() => boolean) | boolean,
        public onclick: ((text?: string) => void) | null) {
        this.enabled.set(enabled);
    }
}
```

Notes:
- Keep the union type `(() => boolean) | boolean` inside the signal — a caller may store a
  function that is later re-evaluated on each read (matching current `isDisabled()` behaviour).
- The `enabled` constructor parameter is no longer `public` (the signal field replaces it),
  so all `.enabled = …` mutations become `.enabled.set(…)` and all reads become `.enabled()`.

### 3.2 Update the read site in `toolbaritem.component.ts`

`isDisabled()` must call the signal and then apply the same function-or-boolean logic:

```ts
isDisabled(): boolean {
    const enabled = this.item()!.enabled();
    return typeof(enabled) == 'function' ? !enabled() : !enabled;
}
```

### 3.3 Update template read sites in `toolbarbutton.component.html`

The two list-button bindings read `enabled` directly. They must be routed through
`isDisabled()`, consistent with the non-list button on `:3` and safe against a
function-valued `enabled`:

- `:11` — change `[disabled]="!item()!.enabled"` to `[disabled]="isDisabled()"`.
- `:14` — change `[disabled]="!item()!.enabled"` to `[disabled]="isDisabled()"`.

### 3.4 Update write sites in `toolbar.component.ts`

Convert every `.enabled = X` assignment to `.enabled.set(X)`:

- `:145` — `this.btnToggleDesignMode.enabled.set(false);`
- `:198` — `this.btnZoomOut.enabled.set(this.urlParser.isShowingContainer() != null);`
- `:268` — `this.btnShowErrors.enabled.set(true);`
- `:1088` — `this.btnZoomOut.enabled.set(this.urlParser.isShowingContainer() != null);`
- `:1108` — `this.btnMoveUp.enabled.set(selection.length == 1);`
- `:1109` — `this.btnMoveDown.enabled.set(selection.length == 1);`
- `:1110` — `this.btnZoomIn.enabled.set(selection.length == 1);`

Leave the commented-out lines unchanged.

### 3.5 Update the unit test in `toolbar.component.spec.ts`

The existing spec (`:76`–`:96`) fakes the buttons as `{ enabled: false }` plain objects and
asserts `btnX.enabled` is a boolean. With the signal migration these must become signals:

- Construct fakes with `enabled: signal<(() => boolean) | boolean>(false)` (or real
  `ToolbarItem` instances).
- Assertions change from `expect(component.btnX.enabled).toBe(true)` to
  `expect(component.btnX.enabled()).toBe(true)`.

## 4. Implementation plan

1. In `com.servoy.eclipse.designer.rfb/node/src/designer/toolbar/toolbar.component.ts`:
   - In class `ToolbarItem`, add `readonly enabled = signal<(() => boolean) | boolean>(false);`
     as a field (next to `state`), remove `public enabled` from the constructor parameter list
     (keep a plain `enabled` parameter), and set `this.enabled.set(enabled);` in the body.
   - Convert the 7 active write sites (`:145`, `:198`, `:268`, `:1088`, `:1108`, `:1109`,
     `:1110`) from `.enabled = …` to `.enabled.set(…)`.
2. In `.../toolbar/item/toolbaritem.component.ts`:
   - Change `isDisabled()` to read `this.item()!.enabled()` (call the signal) before the
     function-or-boolean narrowing.
3. In `.../toolbar/item/toolbarbutton.component.html`:
   - Change the two `[disabled]="!item()!.enabled"` bindings (`:11`, `:14`) to
     `[disabled]="isDisabled()"`.
4. In `.../toolbar/toolbar.component.spec.ts`:
   - Update the button fakes to use `enabled` signals and update the corresponding
     assertions to call `.enabled()`.
5. Verify:
   - `npm run lint` (zero errors).
   - `npm run build_debug_nowatch` (compiles).
   - `npm test` (Vitest — toolbar spec passes).

## 5. Acceptance criteria

- [ ] Selecting a single row/column/container in the responsive editor enables the *Zoom in*
      button; deselecting (or multi-select) disables it.
- [ ] The *Zoom out* button enables when currently showing a container and disables at the
      top level, updating live as navigation changes.
- [ ] `btnMoveUp` / `btnMoveDown` continue to enable on single selection.
- [ ] `btnToggleDesignMode`, `btnShowErrors` and any other `.enabled`-driven buttons continue
      to reflect their enabled state correctly under OnPush.
- [ ] `ToolbarItem.enabled` is a signal; all reads use `enabled()` / `isDisabled()` and all
      writes use `enabled.set(...)`; no plain `.enabled = …` assignment or `!item()!.enabled`
      binding remains for the active code paths.
- [ ] Buttons that pass a function-valued `enabled` (e.g. `btnSetMaxLevelContainer`) still
      compute their disabled state correctly.
- [ ] `npm run lint`, `npm run build_debug_nowatch`, and `npm test` all pass.

## 6. Out of scope

- Migrating the remaining mutable/`markForCheck()`-driven state of `ToolbarComponent`
  (per the RFB AGENTS.md "Remaining: Component State → Signals" list) — only `enabled` is
  addressed here.
- Re-enabling the commented-out alignment/distribution `.enabled` logic around `:1085`–`:1106`.
- Any change to `EditorSessionService.updateSelection()` / `selectionChanged()` wiring beyond
  the write-site conversions.

## 7. Open questions

_None._
