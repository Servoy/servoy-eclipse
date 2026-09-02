# Triage Report — SVY-21335

**Verdict:** PROCEED

## Reported problem

After a version upgrade, AG Grid logs three console warnings (grouped as "AG Grid
found 3 warnings" in the validation overlay) for the List Form Component:

```
No value for `--ag-row-border`. This usually means that the grid has been
initialised before styles have been loaded. The default value of `1` will be used
and updated when styles load.
No value for `--ag-pinned-row-border`. ...
No value for `--ag-header-row-border`. ...
```

The ticket description says only "Upgrade triggered these warnings." No fix is
proposed. User context: *"I think these warnings are on the LFC because there is no
theme set?"* — this is essentially correct and matches the root cause below.

## Root-cause assessment

The warnings originate from AG Grid's CSS-variable measurement logic, not from a
Servoy runtime defect. On grid creation AG Grid appends a hidden
`.ag-measurement-container` to the grid root (`eRootDiv.appendChild(container)` —
`node_modules/ag-grid-community/dist/ag-grid-community.noStyle.js:4173`) and measures
three "border" CSS custom properties by reading `offsetWidth`:

- `ROW_BORDER_WIDTH` → `--ag-row-border` (`...noStyle.js:31322`)
- `PINNED_BORDER_WIDTH` → `--ag-pinned-row-border` (`:31323`)
- `HEADER_ROW_BORDER_WIDTH` → `--ag-header-row-border` (`:31324`)

If the composite variable cannot be resolved, the sentinel `NO_VALUE_SENTINEL = 15538`
survives (`:4287`), the measurement returns `"no-styles"` (`:4164`, `:4193`), and
`varError` → `log.warn(9, …)` fires the exact message in the ticket (`:31419`,
`:65250`).

Why the variables do not resolve for the LFC grid:

1. The app runs AG Grid in **legacy theme mode**:
   `provideGlobalGridOptions({ theme: 'legacy' })`
   (`src/servoycore/ag-grid-initializer.ts:15`). Legacy mode means AG Grid injects no
   Theming-API CSS and relies entirely on the imported legacy stylesheets.
2. Those stylesheets are imported globally in `src/styles.css:3-5`
   (`ag-grid.css`, `agGridClassicFont.css`, `ag-theme-alpine.css`).
3. In the legacy `ag-grid.css`, the border variables are scoped under theme classes,
   **not** on `:root`:
   - `--ag-row-border-style/-color/-width` live under `[class*=ag-theme-]`
     (selector opens at `node_modules/ag-grid-community/styles/ag-grid.css:1338`;
     `--ag-row-border-width: 1px` at `:1409`).
   - The composite `--ag-row-border` / `--ag-pinned-row-border` /
     `--ag-header-row-border` are only declared inside the `.ag-measurement-container`
     block (`ag-grid.css:3707-3714`), and they reference the theme-scoped sub-vars
     which only exist when an ancestor carries an `ag-theme-*` class.
4. The **LFC grid element has no theme class**. The only AG Grid usage in TiNG is
   `<ag-grid-angular #aggrid …>` at
   `src/servoycore/listformcomponent/listformcomponent.ts:51`. Its style binding
   `getAGGridStyle()` (`:1073-1085`) sets `--ag-row-height`, `--ag-header-height`,
   `--ag-list-item-height` and `height`, but **never adds `class="ag-theme-alpine"`**
   and never sets the border variables. So the measurement container's
   `var(--ag-row-border, …)` has no value → sentinel → warning.

The warning is **benign / cosmetic**: AG Grid explicitly states it falls back to the
default width of `1` and updates via a ResizeObserver once styles resolve
(`...noStyle.js:4196-4205`). It is console noise, plus (post-upgrade) an on-grid
"Warnings (N)" overlay badge as seen in the attached screenshot — which is the real
user-visible annoyance.

## Ticket premise check

The ticket proposes no solution, so there is no premise to overturn — but the user's
hypothesis ("no theme set") is confirmed correct. The grid runs in `theme: 'legacy'`
and the LFC grid element carries no `ag-theme-*` class, so the theme-scoped border
variables never resolve on the measurement element. This is a real, self-inflicted
gap exposed by the AG Grid 35→36 upgrade, not expected behaviour to be dismissed.

## Approaches considered

1. **Add the missing theme class to the LFC grid element** (e.g.
   `class="ag-theme-alpine"` on `<ag-grid-angular>`, or set it via `getAGGridStyle`/a
   host class). — *Pros:* addresses the root cause (variables resolve), the grid picks
   up the intended alpine legacy theme it was clearly meant to use (alpine CSS is
   already imported but nothing applies its class), warnings disappear naturally.
   *Cons:* alpine styling may visually change the LFC borders/spacing vs. the current
   themeless rendering; needs a visual check so existing solutions don't shift.

2. **Provide the three border variables inline on the grid element** (extend
   `getAGGridStyle()` to also emit `--ag-row-border`, `--ag-pinned-row-border`,
   `--ag-header-row-border`, matching current values). — *Pros:* silences the warnings
   at the exact measured element with zero broad visual impact; minimal, targeted.
   *Cons:* hardcodes border values that legacy theme CSS would otherwise own;
   duplicates theme responsibility.

3. **Suppress the specific validation warning** via AG Grid options (the initializer
   already uses `ValidationModule.with({ showOverlayOn: [] })` at
   `ag-grid-initializer.ts:23`; extend to silence variable-resolution warnings, or drop
   the ValidationModule in production). — *Pros:* one central change, removes both the
   console noise and the overlay badge for all grids. *Cons:* hides a class of
   diagnostics that could mask genuine future style-loading problems; treats the
   symptom, not the cause.

4. **No code change** (accept as benign console noise). — *Pros:* zero risk; the
   warning is genuinely harmless and self-healing. *Cons:* the post-upgrade on-grid
   "Warnings (N)" overlay badge is visible to end users in the screenshot, which is not
   acceptable for a shipped product; the ticket is explicitly filed to remove it. Not
   appropriate.

## Recommendation

**PROCEED.** Recommended approach: **#1 (apply the intended `ag-theme-alpine` class to
the LFC grid element)**, because the alpine legacy theme CSS is already imported and
was clearly meant to style the grid — the class is simply never applied, which is the
true defect. This resolves the border variables at the source and removes the warnings
without hardcoding values.

Because approach #1 carries a visual-regression risk (alpine borders/spacing on the
LFC), the implementer must verify the LFC appearance before/after. If the visual
change is unacceptable, fall back to **#2 (emit the three border variables inline in
`getAGGridStyle()`)** as a minimal, appearance-preserving alternative. Approach #3
(broad warning suppression) is a last resort only if neither #1 nor #2 is workable, as
it hides diagnostics rather than fixing the missing theme.

## Git history findings

- The warnings were introduced by the AG Grid **35.3.1 → 36.0.2** bump in commit
  `2cebe283f0` "SVY-21274 upgrade core to angular 22" (current workspace is on
  `36.1.0`). AG Grid 36's stricter theming/validation surfaced the previously silent
  missing-theme condition. This matches the ticket's "Upgrade triggered these
  warnings."
- `provideGlobalGridOptions({ theme: 'legacy' })` was set in
  `ag-grid-initializer.ts:15` by commit `d1794cf2fd` (part of the SVY-19023 standalone
  migration line) — this is the intentional decision to stay on legacy CSS themes
  rather than the AG Grid Theming API.
- The legacy stylesheet imports in `src/styles.css:3-5` (including `ag-theme-alpine.css`)
  date to `1ed2fc09f0` "SVY-19919 upgrade to Aggrid 33" (Gabi Boros, 2025-02-07). The
  alpine theme has been imported since then, but no LFC element applies its class.
- No prior spec exists for the AG Grid 36 upgrade or for LFC theming.
