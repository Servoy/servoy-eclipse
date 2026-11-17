# Spec: SVY-21686 — Errors in the console when having sidenav with Servoy menu

## 1. Goal

A form containing a `servoyextra-sidenav` fed by a `servoyMenu` (a `JSMenu`) must launch
**without logging the single first-show NG0100** that the attached `tabpanelSize.servoy`
reproduces:

```
ERROR org.sablo.BrowserConsole - : NG0100: ExpressionChangedAfterItHasBeenCheckedError:
Expression has changed after it was checked.
Previous value: 'undefined'. Current value: '{}'.
Expression location: _FormComponent component.
```

The error is a zoneless dev-mode change-detection gap: a property the host `FormComponent`
binds onto the sidenav element is `undefined` when the host first checks the sidenav, and
has become an empty object `{}` by the time the exhaustive re-check runs — because the
sidenav's own `servoyMenu` seed path mutates that property in a microtask that runs **after**
the host's first check. The fix makes that seed hold a single, stable value before the host's
first check completes.

The fix lives entirely in the **sibling `servoy-extra-components` repo** (`sidenav.ts` +
`sidenav.spec.ts`). It is **not** a change to this `servoy-eclipse` checkout: the generic
host `FormComponent` and the `JSMenu` converter only reflect what the sidenav sets, and
hardening them generically was considered and rejected (§3.2, §6). This repo is involved only
as the host that surfaces the error and as the home of this one spec file.

This is a **residual first-show** case of the same mechanism family as SVY-21654 — not a
duplicate of it. SVY-21654 fixed the repeating ~1 Hz `{"1":"customers"} ↔ {}` loop; this
ticket is the single `undefined → {}` flip at launch that the SVY-21654 fix did not close.

## 2. Background

### 2.1 The symptom and the exact NG0100 stack

The ticket (SVY-21686, `Minor`, component `NGClient`, fixVersion `2026.12`, In Progress, no
comments) reports exactly two facts: *"In the attached solution, the sidenav has Servoy
menu"* and *"after client launch i get this error in the Dev console"*, followed by the stack
above (attachment: `tabpanelSize.servoy`). Reading the stack:

- It is an **NG0100**, surfaced by the zoneless dev-mode exhaustive no-changes check
  (`provideCheckNoChangesConfig({ exhaustive: true, interval: 500 })` configured in this repo
  at `com.servoy.eclipse.ngclient.ui/node/src/main.ts`), the same class of failure as
  SVY-21654 / SVY-21678.
- The failing binding is in the **generated `FormComponent` host template**
  (`??property` → `bindingUpdated` → `FormComponent_ng_template_148_servoyextra_sidenav_0_Template`).
  So it is a property **bound onto the sidenav element from its host form**, not an internal
  sidenav template binding.
- The value transitions `undefined → {}`: the property is `undefined` when the host first
  checks the sidenav, and has become an empty object `{}` when the exhaustive re-check runs.
  It fires **once at launch** (the reporter shows a single occurrence), not the recurring
  flood SVY-21654 fixed.

**Important — the exact property is not yet pinned down.** `??property` is minified in the
stack, so the implementation must *identify* which bound sidenav-element property flips (see
§3.1 / §7), rather than assume it. The strong candidates, from the code below, are the two-way
`selectedIndex` / `expandedIndex` model properties (normalized to `{}` via
`parseIndex(undefined)`) or the `servoyMenu` / derived `menu` object.

### 2.2 The servoyMenu seed path in `sidenav.ts` and why the microtask opens the NG0100 window

The sidenav never binds the raw server inputs in its template; it copies them into private
signals and the template reads those. The relevant declarations
(`components/projects/servoyextracomponents/src/sidenav/sidenav.ts:59-62`):

```ts
_selectedIndex = signal<any>({});
_expandedIndex = signal<any>({});
_open = signal<boolean | undefined>(undefined);
_menu = linkedSignal<MenuItem[] | undefined>(() => this.menu());
```

`svyOnInit` seeds them and — when a `servoyMenu` is present — schedules `copyServoyMenu()` in
a **microtask** (`sidenav.ts:90-104`):

```ts
svyOnInit() {
    super.svyOnInit();
    this._selectedIndex.set(this.parseIndex(this.selectedIndex()));
    this._expandedIndex.set(this.parseIndex(this.expandedIndex()));
    this._open.set(this.open());
    if (this.servoyMenu()) {
        Promise.resolve().then(() => this.copyServoyMenu());   // <- after-checked microtask
    }
    ...
}
```

`parseIndex(undefined)` returns `{}` (`sidenav.ts:247-251`):

```ts
private parseIndex(value: any): any {
    if (!value) return {};                          // undefined -> {}
    if (typeof value === 'string') return JSON.parse(value);
    return value;
}
```

So with no `onLoad`-set selection, `_selectedIndex` / `_expandedIndex` are seeded to a **fresh
`{}`** in `svyOnInit`. The base class (`ServoyBaseComponent.ngAfterViewInit` → first
`svyOnChanges` → `cdRef.detectChanges()`, per
`components/node_modules/@servoy/public/fesm2022/servoy-public.mjs`) runs the first change
handling **after** the host view has already been created and checked once — the classic
NG0100 window. The `Promise.resolve().then(() => this.copyServoyMenu())` pushes the servoyMenu
re-derivation even later, into a microtask that runs after that first check.

`copyServoyMenu()` (`sidenav.ts:1146-1187`) rebuilds `_menu`, and when the `JSMenu` has **no
`isSelected` item** it deliberately keeps the already-seeded selection (the SVY-21654/-21634
no-isSelected branch) and `markForCheck()`s:

```ts
private copyServoyMenu() {
    const servoyMenu = this.servoyMenu();
    if (servoyMenu) {
        const expandedIndex = this._expandedIndex();
        if (expandedIndex && typeof expandedIndex == 'string') {
            this._expandedIndex.set(JSON.parse(expandedIndex));
        }
        const selectedNode: any = {};
        const oldMenu: any[] = [];
        if (servoyMenu.items) {
            for (const item of servoyMenu.items) {
                this.copyServoyMenuItem(oldMenu, item, 1, selectedNode);
            }
        }
        this._menu.set(oldMenu);
        const previousSelectedIndex = this._selectedIndex();
        const selection = Object.keys(selectedNode);
        if (selection && selection.length == 1) {
            this.applyingServerSelection = true;
            try {
                this.updateSelectedNode(selectedNode[selection[0]], this._menu()!, parseInt(selection[0]));
            } finally {
                this.applyingServerSelection = false;
            }
        }
        // else: no isSelected item — keep the seeded selection; do NOT clear to {}.
        if (isEqual(previousSelectedIndex, this._selectedIndex())) {
            this._selectedIndex.set(previousSelectedIndex);
        }
        this.cdRef.markForCheck();
    }
}
```

The combination — a two-way model property (`selectedIndex` / `expandedIndex` /
`servoyMenu`) that starts `undefined` server-side but is normalized/derived to an empty-object
value inside this after-checked microtask, and reflected back onto the element the host bound
— is what makes the host `FormComponent`'s exhaustive re-check see the one bound property flip
`undefined → {}`, producing the single launch-time NG0100.

Note the two-way model shapes the fix must respect (`components/sidenav/sidenav.spec:17-19`):

```json
"servoyMenu"    : {"type":"JSMenu", "pushToServer":"allow", ...},
"selectedIndex" : {"type":"string", "tags":{"scope":"private"}, "pushToServer":"deep"},
"expandedIndex" : {"type":"string", "tags":{"scope":"private"}, "pushToServer":"deep"}
```

`selectedIndex` / `expandedIndex` are deep-pushed **string** properties (JSON strings on the
wire); `servoyMenu` is a `JSMenu` object property. The empty-object `{}` the stack shows is on
the client side, after the sidenav normalizes/derives one of these.

### 2.3 How this differs from the SVY-21654 repeating-loop fix — residual first-show

SVY-21654 (`82aeb4e`, 2026-10-27, lvostinar) targeted the **repeating** ~1 Hz
`{"1":"customers"} ↔ {}` loop: it removed the transient `this._selectedIndex.set({})` and the
per-sync `selectedIndexChange` echo, added `applyingServerSelection` to suppress the two-way
echo for server-derived selections, and switched `copyServoyMenu` from `detectChanges()` to
`markForCheck()`. That stopped the **loop**.

SVY-21686 is the **one-time `undefined → {}`** that happens on the very first servoyMenu seed,
*before* the component has any selection, when the `JSMenu` carries **no pre-selected item**.
The SVY-21654 fix leaves the first-show seed (`svyOnInit` → `parseIndex(undefined)` → `{}` →
microtask `copyServoyMenu`) intact; that seed is exactly what still flips the host-bound
property from `undefined` to `{}` after the host's first check. So this is a **refinement of
the same code path**, not a revert of SVY-21654 and not a second occurrence of the loop.

### 2.4 Git history

Component repo (`servoy-extra-components`, branch `master`, HEAD `45ef6dc` 2026-11-16
"SVY-21678 fix slider console errors"). Sidenav history, newest first:

- `aff2812` (2026-10-30) **SVY-21635** `allowMultipleExpanded` — touches expand/collapse
  multi-open logic, not the servoyMenu seed echo; not the introducer.
- `b734d62` (2026-10-27) **SVY-21634** "reflect onLoad-set sidenav selection/expansion on
  first show" — introduced the `svyOnInit` seed via `parseIndex(...)`, where
  `parseIndex(undefined)` returns `{}` (`sidenav.ts:247-251`). **Directly relevant** to the
  `undefined → {}` transition this ticket is about.
- `82aeb4e` (2026-10-27) **SVY-21654** "fix repeating NG0100 in sidenav on server-set
  selection" — removed the transient `_selectedIndex.set({})` + per-sync echo, added
  `applyingServerSelection`, switched `copyServoyMenu` to `markForCheck()`. Fixed the
  **repeating** loop; the **first-show single `undefined → {}`** is the residual case this
  ticket now reports. The fix here refines this same code, it does not revert it.
- Earlier: the reset-to-`{}` + fresh-object template-method pattern predates the signals
  migration (`0d16291` / `475de68`, cPecican 2026-02-04, per
  `docs/SVY-21654-ng0100-investigation.md`), so fixing it refines the signals migration
  rather than reverting a recent deliberate decision.

This repo (`servoy-eclipse`): the exhaustive dev-mode check that surfaces the error was
enabled with zoneless change detection in `src/main.ts`. The generic `FormComponent` host and
`JSMenu` converter are not recent regressions. No prior `docs/SVY-21686-*` spec exists in
either repo.

### 2.5 The two-repo layout — the host is not the fault

| What | Repo | File(s) |
|------|------|---------|
| Sidenav fix (seed stabilization + regression test) | **`servoy-extra-components`** (sibling checkout, own git + Angular 22 / Vitest node tooling) | `components/projects/servoyextracomponents/src/sidenav/sidenav.ts`, `.html`, `.spec.ts`; `components/sidenav/sidenav.spec` |
| Generic host that surfaces the NG0100 — **NOT changed** | **this `servoy-eclipse` repo** | `com.servoy.eclipse.ngclient.ui/node/src/ngclient/form/form_component.component.ts` |
| `JSMenu` converter — **NOT changed** | **this `servoy-eclipse` repo** | `com.servoy.eclipse.ngclient.ui/node/src/ngclient/converters/jsmenu_converter.ts` |
| Dev-mode exhaustive no-changes check that reports it | **this `servoy-eclipse` repo** | `com.servoy.eclipse.ngclient.ui/node/src/main.ts` |

The host `FormComponent` binding only reflects whatever value the sidenav writes into its
two-way model property. The generic host and the `JSMenu` converter are correct; the defect is
the sidenav's own first-show seed. **Crucially, this `servoy-eclipse` repo cannot build or run
the sidenav's Vitest suite** — that is the `servoy-extra-components` node project with its own
tooling (Angular 22, Vitest; use `npm.cmd` / `npx.cmd` on Windows). The sidenav fix is built
and tested there; only this spec file lives here.

## 3. Design

### 3.1 Approach — identify the flipping property, then seed it stably

The design is deliberately staged, because the exact property is not yet pinned (§2.1). The
repo's own "log first, then fix" guidance (`servoy-extra-components` AGENTS.md §"Debugging:
Log First, Fix Later") is authoritative here.

**Step A — reproduce and identify (diagnostic first, not a guess).** Reproduce the single
launch NG0100 with the attached `tabpanelSize.servoy` (sidenav fed by a `servoyMenu`, no
pre-selected item), then add **one** diagnostic that reveals which sidenav-element property the
host `FormComponent` binds that flips `undefined → {}` across the host's first check. The most
likely culprits, in order:

1. `selectedIndex` / `expandedIndex` — seeded in `svyOnInit` to a fresh `{}` via
   `parseIndex(undefined)` (`sidenav.ts:94-95`, `247-251`) and reflected through the two-way
   `deep` string channel.
2. the `servoyMenu` / derived `_menu` object re-set inside the after-checked microtask
   (`copyServoyMenu`, `sidenav.ts:1160`).

Confirm the one that matches the stack (`FormComponent_ng_template_148_servoyextra_sidenav_0_Template`,
`undefined → {}`) before changing code. This confirmation is Open Question §7.

**Step B — seed that property stably, once, before the host's first check.** Once identified,
make the seed hold a single stable value so the host sees one value, not `undefined` then `{}`.
Frame the candidate techniques — the implementer picks and justifies one against the sample,
this spec does **not** prescribe a single settled line:

- **Seed synchronously in `svyOnInit` where feasible**, rather than deferring the
  selection/menu derivation to the `Promise.resolve().then(() => copyServoyMenu())` microtask,
  so the bound value is already its final value when the host first checks. (If the microtask
  is genuinely required for menu resolution timing — see §7 — keep it but ensure the
  *host-bound* property is already stable before the first check.)
- **Do not write an empty object into a two-way property that was `undefined`.** For the
  no-isSelected / no-onLoad case, avoid flipping a previously-`undefined` bound property to a
  fresh `{}` at all; leave it `undefined`, or seed it to its final value directly. The
  `parseIndex(undefined) → {}` normalization (`sidenav.ts:248`) is the prime suspect for
  manufacturing the `{}`.
- **Keep references stable.** Where the component re-derives the same empty/seed value, reuse
  the existing reference (the `isEqual(...)` restore at `sidenav.ts:1180-1182` already does
  this for `_selectedIndex`; the fix may need the analogous guarantee for whichever property
  the diagnostic implicates, and/or to avoid producing the empty object in the first place).

The exact one-line change is intentionally left to Step B, decided from the Step-A evidence.

### 3.2 What is explicitly NOT changed

- **The generic host `FormComponent`** (`com.servoy.eclipse.ngclient.ui/node/src/ngclient/form/form_component.component.ts`)
  — it correctly reflects the sidenav's two-way property; masking the flip there would paper
  over the component bug and risk every other component (triage approach 2, rejected).
- **The `JSMenu` converter** (`.../src/ngclient/converters/jsmenu_converter.ts`) — generic,
  not the source of the `{}`.
- **The already-fixed SVY-21654 repeating-loop path** — the `applyingServerSelection` echo
  suppression, the removed transient `_selectedIndex.set({})`, and the `markForCheck()` switch
  stay as they are. This fix must not regress them.
- **The `pushToServer: deep` string contract** of `selectedIndex` / `expandedIndex` and the
  `JSMenu` shape of `servoyMenu` (`sidenav.spec:17-19`) — the fix converges the client seed, it
  does not change the push mode or the wire shape.
- **The dev-mode exhaustive no-changes check** in this repo's `src/main.ts` — it is the correct
  detector; it is not relaxed to hide the error.

### 3.3 NO_ACTION fallback

If Step A cannot isolate the flipping property to a change that is demonstrably safe (no
regression to selection/expansion behaviour, to the SVY-21654 loop fix, or to SVY-21634
first-show rendering), the honest fallback is **NO_ACTION**: accept the single dev-mode-only
launch message (NG0100 is dev-mode only; production skips the exhaustive check) and record the
finding on the ticket. **Do NOT** fall back to hardening the generic host / `JSMenu` converter
(§3.2) — that is explicitly rejected. NO_ACTION is preferred over a speculative change that
risks the already-shipped SVY-21654/-21634 behaviour.

## 4. Implementation plan

Ordered. Each step is tagged with the repo it lives in. The reproduce-and-log step is first.

1. **[servoy-extra-components] Reproduce and add a diagnostic (Step A).** Launch the attached
   `tabpanelSize.servoy` (sidenav fed by a `servoyMenu`, no pre-selected item) against a dev
   build and confirm the single launch NG0100 with the §2.1 stack. Add **one** diagnostic
   (e.g. log `typeof`, reference identity, and value of `selectedIndex` / `expandedIndex` /
   `servoyMenu` / `_menu` at the `svyOnInit` seed and at the end of the `copyServoyMenu`
   microtask) to identify which host-bound property flips `undefined → {}`. Do not theorize —
   read the actual value. Remove the diagnostic before committing.

2. **[servoy-extra-components] Stabilize the seed (Step B).** In
   `components/projects/servoyextracomponents/src/sidenav/sidenav.ts`, make the identified
   property hold a single stable value before the host's first check — using one of the §3.1
   techniques (synchronous `svyOnInit` seed where feasible; do not flip a previously-`undefined`
   two-way property to a fresh `{}`; keep references stable). Touch only what the evidence
   requires; leave the SVY-21654 `applyingServerSelection` / `markForCheck` logic and the
   SVY-21634 seed semantics intact.

3. **[servoy-extra-components] Add a Vitest regression.** In
   `components/projects/servoyextracomponents/src/sidenav/sidenav.spec.ts` (extend the existing
   `servoyMenu` / selection suites — e.g. the `servoyMenu once-per-second NG0100 loop
   (SVY-21654)` describe with its `makeUnselectedServoyMenu` helper, and the `onLoad
   selection/expansion on first show (SVY-21634)` describe), add a **first-show** test:
   - Build a `servoyMenu` with **no `isSelected` item** and **no** `onLoad` `selectedIndex` /
     `expandedIndex` (the ticket's case). Drive the real first-show path: `svyOnInit`, let the
     `Promise.resolve().then(copyServoyMenu)` microtask run (`await Promise.resolve()` +
     `fixture.whenStable()`), then `fixture.detectChanges()`.
   - Assert the identified host-bound property **does not flip `undefined → {}`** across the
     initial check — i.e. it holds one stable value (either stays `undefined`, or is its final
     value) through `svyOnInit` → microtask → first `detectChanges()` — and that
     `fixture.detectChanges()` **does not throw** `ExpressionChangedAfterItHasBeenChecked`.
     Assert the one specific outcome, not "contains passed or failed".
   - Keep the existing SVY-21654 (`should not throw NG0100 across repeated identical servoyMenu
     syncs`, `should NOT emit selectedIndexChange for the server-originated selection`, `should
     still emit selectedIndexChange for a genuine user selection`) and SVY-21634 (`should keep
     the onLoad-seeded selection when the servoyMenu has no isSelected item`) tests green —
     proving no regression to the loop fix or to first-show seeding.
   - Follow the repo's test conventions (`TestBed.createComponent`,
     `fixture.componentRef.setInput(...)`, `NO_ERRORS_SCHEMA`, `ServoyPublicTestingModule`,
     `ServoyApiTesting`; the `createJSEvent` jsdom override already used in the SVY-21654
     suite). **Never** `vi.stubGlobal('document'/'window', ...)`.

4. **[servoy-extra-components] Build, lint, test.** From `components/`:
   - `npm run build` (ng-packagr production build — full TypeScript typecheck).
   - `npm run lint` (ESLint 10; warnings surface via `eslint-plugin-only-warn` but must be
     addressed — zero warnings before committing).
   - `npx ng test @servoy/servoyextracomponents --no-watch --include "projects/servoyextracomponents/src/sidenav/sidenav.spec.ts"`
     (or `npm run test` for the whole suite). On Windows use `npm.cmd` / `npx.cmd`.

5. **[servoy-eclipse docs] Keep this spec as the only artifact in this repo.** No source change
   in `servoy-eclipse`. This spec file (`docs/SVY-21686-sidenav-ng0100-firstshow.spec.md`) is
   the sole deliverable here; the host `FormComponent` and `JSMenu` converter are untouched.

6. **Verify end-to-end.** Re-launch `tabpanelSize.servoy`: confirm the single launch-time
   NG0100 is gone and the sidenav still renders, selects, and expands normally. Confirm
   lint/build/tests are green in the **sibling** repo.

## 5. Acceptance criteria

- [ ] **No launch-time NG0100 with `tabpanelSize.servoy`** (sidenav fed by a `servoyMenu` with
      no pre-selected item): launching the sample no longer logs the
      `NG0100 ExpressionChangedAfterItHasBeenCheckedError … Previous value: 'undefined'.
      Current value: '{}'. Expression location: _FormComponent component` from the
      `servoyextra_sidenav` host template.
- [ ] **The identified property holds a single stable value** through the host's first check —
      it does not transition `undefined → {}` across `svyOnInit` → the `copyServoyMenu`
      microtask → the first `detectChanges()`. Which property it is, is confirmed by the Step-A
      diagnostic (§7).
- [ ] **No regression to sidenav selection/expansion behaviour** — selecting a node, expanding
      and collapsing groups (single-open and `allowMultipleExpanded`), and `onLoad`-set
      selection/expansion rendering on first show (SVY-21634) all behave exactly as before.
- [ ] **No regression to the SVY-21654 repeating-loop fix** — repeated identical `servoyMenu`
      syncs still do not throw NG0100; `selectedIndexChange` is still suppressed for a
      server-originated selection and still emitted for a genuine user click.
- [ ] **A Vitest regression is added** in `sidenav.spec.ts`: a first-show `servoyMenu`-with-no-
      `isSelected` (and no `onLoad` index) case asserting the host-bound property does not flip
      `undefined → {}` and `fixture.detectChanges()` does not throw. It asserts the one specific
      outcome and fails against the pre-fix component.
- [ ] **lint/build/tests green in the sibling repo** — from `servoy-extra-components/components`,
      `npm run build`, `npm run lint` (zero warnings), and the sidenav Vitest run all pass.
- [ ] **No change to this repo's host/converter** — `form_component.component.ts`,
      `jsmenu_converter.ts`, and `src/main.ts` are untouched in `servoy-eclipse`.

## 6. Out of scope

- **Hardening the generic host `FormComponent` or the `JSMenu` converter** in `servoy-eclipse`
  (triage approach 2) — the host only reflects the sidenav's value; fixing it generically would
  mask the component bug and risk every other component. Explicitly rejected.
- **The SVY-21654 repeating ~1 Hz `{"1":"customers"} ↔ {}` loop** — already fixed; this ticket
  is the distinct residual first-show case.
- **Unrelated console noise** in the same session (e.g. any Developer AI/OpenAI startup
  messages) — not part of this NG0100.
- **Relaxing or removing the dev-mode exhaustive no-changes check** (`src/main.ts`) — it is the
  correct detector and stays on.
- **Broader refactors of the sidenav's style/class template methods** (`getSidenavClass`,
  `get*FormStyle`, `getContainerStyle`) or the inline-object-literal template bindings flagged
  in `docs/SVY-21654-ng0100-investigation.md` — forward hardening unrelated to this specific
  first-show flip; only touch them if the Step-A diagnostic directly implicates one.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| **Which exact property flips `undefined → {}`?** The stack minified it to `??property`. Must be confirmed by the Step-A diagnostic before Step B — most likely `selectedIndex`/`expandedIndex` (via `parseIndex(undefined) → {}`), or the `servoyMenu`/`_menu` object. | Implementer | open — resolved by reproduce-and-log |
| **Is synchronous seeding in `svyOnInit` safe, or is the `Promise.resolve().then(copyServoyMenu)` microtask genuinely required** for the servoyMenu to be resolved before deriving `_menu`/selection? Decide against the sample: if the microtask is required for menu resolution, ensure the *host-bound* property is already stable before the first check rather than removing the microtask. | Implementer | open — measured against `tabpanelSize.servoy` |
| **Does the no-isSelected first-show case need `undefined` left as-is, or a stable final seed?** i.e. should the fix avoid producing `{}` at all (leave the two-way property `undefined` until a real selection), or seed a stable canonical value — whichever keeps SVY-21634 first-show rendering and the SVY-21654 loop fix intact. | Implementer | open |
