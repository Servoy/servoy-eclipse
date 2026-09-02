# Spec: SVY-21335 — Eliminate AG Grid border-variable console warnings on the List Form Component

## 1. Goal

After the AG Grid 35 → 36 upgrade, the List Form Component (LFC) grid logs three
console warnings on creation and shows an on-grid "Warnings (N)" overlay badge:

```
No value for `--ag-row-border`. This usually means that the grid has been
initialised before styles have been loaded. The default value of `1` will be used
and updated when styles load.
No value for `--ag-pinned-row-border`. ...
No value for `--ag-header-row-border`. ...
```

The **sole goal** is to eliminate these three warnings (and the associated on-grid
"Warnings (N)" overlay badge) with **ZERO visual or behavioral change** to the LFC.
The LFC intentionally has **no theme class applied** precisely so that borders are
**not** rendered; that borderless appearance and all current behavior must remain
**exactly** the same. This is a warnings-only fix — not a theming or styling change.

## 2. Background

### 2.1 Where the warnings come from

The warnings originate from AG Grid's CSS-variable measurement logic, not from a
Servoy runtime defect. On grid creation AG Grid appends a hidden
`.ag-measurement-container` to the grid root and measures three composite "border"
CSS custom properties by reading `offsetWidth`:

- `ROW_BORDER_WIDTH` → `--ag-row-border`
- `PINNED_BORDER_WIDTH` → `--ag-pinned-row-border`
- `HEADER_ROW_BORDER_WIDTH` → `--ag-header-row-border`

If a composite variable cannot be resolved, AG Grid's `NO_VALUE_SENTINEL` survives the
measurement, the result becomes `"no-styles"`, and `log.warn` fires the exact message
above. AG Grid 36's stricter theming/validation surfaced this previously-silent
condition (see Git history).

### 2.2 Why the variables do not resolve for the LFC grid

1. The app runs AG Grid in **legacy theme mode**:
   `provideGlobalGridOptions({ theme: 'legacy' })`
   (`src/servoycore/ag-grid-initializer.ts:15`). Legacy mode injects no Theming-API
   CSS and relies entirely on the imported legacy stylesheets.
2. Those stylesheets are imported globally in `src/styles.css:3-5`
   (`ag-grid.css`, `agGridClassicFont.css`, `ag-theme-alpine.css`).
3. In the legacy `ag-grid.css`, the border sub-variables are scoped under theme
   classes, **not** on `:root`:
   - `--ag-row-border-style/-color/-width` live under `[class*=ag-theme-]`
     (`node_modules/ag-grid-community/styles/ag-grid.css:1407-1409`).
   - The composite variables are declared inside the `.ag-measurement-container`
     block (`ag-grid.css:3711-3713`) and reference the theme-scoped sub-vars:
     ```css
     --ag-header-row-border: var(--ag-borders-critical) var(--ag-border-color);
     --ag-pinned-row-border: var(--ag-borders-critical) var(--ag-border-color);
     --ag-row-border: var(--ag-row-border-style) var(--ag-row-border-color) var(--ag-row-border-width);
     ```
     These only produce a value when an ancestor carries an `ag-theme-*` class.
4. The **LFC grid element has no theme class**. The only AG Grid usage in TiNG is
   `<ag-grid-angular #aggrid …>` at
   `src/servoycore/listformcomponent/listformcomponent.ts:51`. Its style binding
   `getAGGridStyle()` (`:1073-1085`) sets `--ag-row-height`, `--ag-header-height`,
   `--ag-list-item-height` and `height`, but **never** applies an `ag-theme-*` class
   and **never** sets the border variables. So the measurement container's
   `var(--ag-row-border, …)` has no value → sentinel → warning.

The warnings are **benign / cosmetic** — AG Grid falls back to width `1` and updates
via a ResizeObserver once styles resolve — but the on-grid "Warnings (N)" overlay badge
is visible to end users, which is not acceptable for a shipped product.

### 2.3 Hard constraint (authoritative)

The absence of a theme on the LFC is **intentional**: it is the reason borders are not
shown. The LFC's visual appearance and behavior must remain **exactly** the same.
Applying a theme class (e.g. `ag-theme-alpine`) would reintroduce borders/spacing and
change the visual — that is **explicitly rejected**. The only acceptable outcome is
that the three warnings and the overlay badge disappear while the rendered LFC is
pixel-for-pixel identical to today.

## 3. Design

### 3.1 Primary approach — define the border *sub-variables* inline (appearance-preserving)

**Key correction (learned during implementation):** setting the three *composite*
variables (`--ag-row-border`, `--ag-pinned-row-border`, `--ag-header-row-border`) on
the grid element does **not** work. AG Grid's `.ag-measurement-container` rule
(`ag-grid.css:3707-3714`) **reassigns** those composites *inside* the container from the
theme sub-variables:

```css
.ag-measurement-container {
  --ag-header-row-border: var(--ag-borders-critical) var(--ag-border-color);
  --ag-pinned-row-border: var(--ag-borders-critical) var(--ag-border-color);
  --ag-row-border: var(--ag-row-border-style) var(--ag-row-border-color) var(--ag-row-border-width);
}
```

Because this rule wins on the measurement element, our inline composites are overwritten
by the theme-scoped sub-vars — which are unresolved without an `ag-theme-*` ancestor →
sentinel survives → warning still fires.

The correct fix is to define the **sub-variables** the container's rule references, so
the composites it builds resolve to a zero-width, transparent (invisible) border:

```typescript
getAGGridStyle(): any {
  const aggridStyle: Record<string, any> = {
    '--ag-row-height': 42,
    '--ag-header-height': 48,
    '--ag-list-item-height': 24,
    '--ag-row-border-style': 'solid',
    '--ag-row-border-color': 'transparent',
    '--ag-row-border-width': '0',
    '--ag-borders-critical': '0 solid',
    '--ag-border-color': 'transparent',
  };
  ...
}
```

- `--ag-row-border` resolves to `solid transparent 0` (via style/color/width sub-vars).
- `--ag-pinned-row-border` / `--ag-header-row-border` resolve to `0 solid transparent`
  (via `--ag-borders-critical` + `--ag-border-color`).

All three composites are now resolvable (no sentinel → no warning) and measure to zero
visible width, preserving the exact borderless LFC rendering. The sub-vars are inherited
by the measurement container from the grid element, so no theme class is needed. This is
minimal and has zero broad visual impact.

### 3.2 Fallback approach — centrally suppress the specific validation warning

**Use only if 3.1 cannot fully silence all three warnings.** The AG Grid initializer
already registers `ValidationModule.with({ showOverlayOn: [] })`
(`src/servoycore/ag-grid-initializer.ts:23`). If inline variables do not fully
suppress the warnings, extend the initializer to silence the specific
variable-resolution warning (or otherwise configure validation) so both the console
noise and the overlay badge are removed.

This approach is **less preferred**: it hides a class of diagnostics that could mask
genuine future style-loading problems, and it treats the symptom rather than the
missing value. It must not be used pre-emptively — only if 3.1 is proven insufficient.

### 3.3 Rejected approach — applying a theme class

Adding `ag-theme-alpine` (or any `ag-theme-*`) class to the LFC grid element is
**explicitly rejected**. It would resolve the variables via the theme, but it would
also reintroduce alpine borders/spacing and change the LFC visual, violating the hard
constraint. See Out of scope.

### 3.4 Git history

- The warnings were introduced by the AG Grid **35.3.1 → 36.0.2** bump in commit
  `2cebe283f0` "SVY-21274 upgrade core to angular 22" (workspace is on `36.1.0`).
  AG Grid 36's stricter theming/validation surfaced the previously-silent
  missing-theme condition — matching the ticket's "Upgrade triggered these warnings."
- `provideGlobalGridOptions({ theme: 'legacy' })` was set in
  `ag-grid-initializer.ts:15` by commit `d1794cf2fd` (SVY-19023 standalone migration) —
  the intentional decision to stay on legacy CSS themes.
- Legacy stylesheet imports in `src/styles.css:3-5` (including `ag-theme-alpine.css`)
  date to `1ed2fc09f0` "SVY-19919 upgrade to Aggrid 33". The alpine theme has been
  imported since then, but no LFC element applies its class (by design).
- No prior spec exists for the AG Grid 36 upgrade or for LFC theming.

## 4. Implementation plan

1. **Modify `getAGGridStyle()`** in
   `src/servoycore/listformcomponent/listformcomponent.ts` (lines ~1073-1085) to add
   the three composite border CSS variables (`--ag-row-border`,
   `--ag-pinned-row-border`, `--ag-header-row-border`) with a resolvable value that
   measures to zero visible border (e.g. `0 solid transparent` / `none`), preserving
   the current borderless look.
2. **Verify appearance is unchanged:** run the LFC (both absolute-layout and
   responsive-height paths, with `useScrolling` enabled so the AG Grid branch renders),
   compare before/after — no borders, spacing, row/header heights, or behavior may
   change.
3. **Verify warnings are gone:** confirm the three `--ag-*-border` warnings no longer
   appear in the browser console and the on-grid "Warnings (N)" overlay badge is gone.
4. **If (and only if) any of the three warnings remain**, apply the fallback (§3.2):
   extend `ValidationModule` configuration / validation options in
   `src/servoycore/ag-grid-initializer.ts` to suppress the specific
   variable-resolution warning, then re-verify §3 items.
5. **Validate the build:**
   - `npx tsc --noEmit -p src/tsconfig.app.json`
   - `npm run lint` (zero warnings)
   - `npx ng build ngclient2 --configuration development`
6. **Run relevant tests:** existing LFC / core specs
   (`npx ng test --include="**/listformcomponent*.spec.ts" --no-watch` if present),
   ensure no regressions.

## 5. Acceptance criteria

- [ ] The three AG Grid console warnings (`No value for --ag-row-border`,
      `--ag-pinned-row-border`, `--ag-header-row-border`) no longer appear when an LFC
      grid is created.
- [ ] The on-grid "Warnings (N)" overlay badge no longer appears on the LFC.
- [ ] The LFC visual appearance is **identical** before and after the change — no
      borders are introduced, spacing is unchanged, row/header/list-item heights are
      unchanged (borderless rendering preserved).
- [ ] The LFC behavior is unchanged (selection, scrolling, paging, focus, keyboard
      handling all behave as before).
- [ ] No theme class (`ag-theme-*`) is applied to the LFC grid element.
- [ ] `npx tsc --noEmit -p src/tsconfig.app.json` passes.
- [ ] `npm run lint` passes with zero warnings.
- [ ] `npx ng build ngclient2 --configuration development` succeeds.

## 6. Out of scope

- Applying `ag-theme-alpine` (or any `ag-theme-*`) class to the LFC grid element.
- Any change to the LFC visual appearance (borders, spacing, colors, heights).
- Migrating AG Grid from legacy theme mode to the AG Grid Theming API.
- Changing global AG Grid stylesheet imports in `src/styles.css`.
- Fixing or restyling any other AG Grid usage (there is currently only the LFC grid in
  TiNG); no other grids should be affected.
- Broadly disabling AG Grid validation beyond the single variable-resolution warning
  (fallback §3.2 must be as narrow as possible, and only if §3.1 is insufficient).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Confirm the exact zero-width border token (`0 solid transparent` vs `none` vs `0`) that makes AG Grid's `offsetWidth` measurement resolve to 0 while remaining a valid, resolvable `var()` value. To be verified empirically during implementation (§4 step 2-3). | Implementer | open |
| Confirm inline emission on `<ag-grid-angular>` reaches the `.ag-measurement-container` node (it is appended to the grid root, which is the same element `getAGGridStyle()` styles). If not, the fallback §3.2 applies. | Implementer | open |
