# Triage Report — SVY-21686

**Verdict:** PROCEED

## Reported problem

**Symptom (not a proposed solution — the ticket proposes none):** With the attached
solution (`tabpanelSize.servoy`), a form has a **sidenav** fed by a **Servoy menu**
(`servoyMenu` property). Right after the client launches, a single NG0100 is logged to the
dev console:

```
ERROR org.sablo.BrowserConsole - : NG0100: ExpressionChangedAfterItHasBeenCheckedError:
Expression has changed after it was checked.
Previous value: 'undefined'. Current value: '{}'.
Expression location: _FormComponent component.
    at throwErrorIfNoChangesMode (...chunk-XWVVTJAK.js:9402:9)
    at bindingUpdated (...chunk-XWVVTJAK.js:13153:9)
    at ??property (...chunk-XWVVTJAK.js:19461:7)
    at FormComponent_ng_template_148_servoyextra_sidenav_0_Template (...chunk-AYHSUK5D.js:2863:876)
    at executeTemplate (...chunk-XWVVTJAK.js:11095:5)
```

Key facts extracted from the stack:
- It is an NG0100 (zoneless dev-mode exhaustive no-changes check — the same class of
  failure as SVY-21654/SVY-21678), surfaced by `provideCheckNoChangesConfig({ exhaustive:
  true })` configured in this repo at
  `com.servoy.eclipse.ngclient.ui/node/src/main.ts:23`.
- The failing binding is in the **generated `FormComponent` host template** (`??property`
  → `bindingUpdated`), on the `servoyextra-sidenav` element instance
  (`FormComponent_ng_template_148_servoyextra_sidenav_0_Template`). So it is a **property
  bound onto the sidenav component from its host form**, not an internal template binding.
- The value transitions `undefined → {}`: a property that is `undefined` when the parent
  form view is first checked, and has become an **empty object `{}`** when the exhaustive
  re-check runs — i.e. something sets that property to `{}` during the sidenav's own
  initialization microtask, *after* the host `FormComponent` already checked it. It fires
  **once at launch** (the reporter shows a single occurrence), not the ~1 Hz flood that
  SVY-21654 addressed.

## Root-cause assessment

**The fault is in the `servoy-extra-components` sidenav component, not in this
`servoy-eclipse` repo.** The relevant source was found on disk in the sibling checkout:
`C:\Users\vosti\git_master\servoy-extra-components\components\projects\servoyextracomponents\src\sidenav\sidenav.ts`
(and `sidenav.html`). The `servoy-eclipse` side involved here is only the generic host
(`com.servoy.eclipse.ngclient.ui/node/src/ngclient/form/form_component.component.ts`, the
`FormComponent` that owns the `servoyextra_sidenav_0` template) and the `JSMenu` converter
(`.../src/ngclient/converters/jsmenu_converter.ts`) — both generic and not the source of
the bug.

This is the **servoyMenu-seeded selection/expansion path**, the same mechanism family as
SVY-21654, but a residual **first-show** case the SVY-21654 fix did not fully close:

1. The sidenav binds a private `signal` to the template (`_selectedIndex`,
   `_expandedIndex`, `_open`, `_menu`), each defaulting to `{}` / `undefined`
   (`sidenav.ts:59-62`). The server two-way model properties on the element
   (`selectedIndex`, `expandedIndex` — both `pushToServer: "deep"` **string** props;
   `servoyMenu` — a `JSMenu` object prop; `sidenav.spec:17-19`) start `undefined`
   server-side when nothing is set in `onLoad`.
2. When a `servoyMenu` is present, `svyOnInit` schedules `copyServoyMenu()` in a microtask
   (`Promise.resolve().then(...)`, `sidenav.ts:97-99`). The base class
   (`ServoyBaseComponent.ngAfterViewInit` → `svyOnChanges` → `cdRef.detectChanges()`, seen
   in `@servoy/public` fesm2022) runs the first change handling **after** the host view has
   been created/checked once — the classic NG0100 window.
3. In that microtask `copyServoyMenu` rebuilds `_menu`, and when the `JSMenu` has **no
   `isSelected` item** it leaves the selection empty, then writes/normalizes the selection
   signal and `markForCheck()`s (`sidenav.ts:1160-1185`). Combined with the two-way
   `selectedIndex`/`expandedIndex`/`servoyMenu` model properties being mutated from
   `undefined` to an empty-object value and pushed/reflected onto the element, the host
   `FormComponent`'s exhaustive re-check sees the bound property flip `undefined → {}` for
   that one sidenav element → the single launch-time NG0100.

The SVY-21654 commit (`82aeb4e`, 2026-10-27) specifically removed the transient
`this._selectedIndex.set({})` and the per-sync `selectedIndexChange` echo to stop the
**repeating** loop, and added `applyingServerSelection`. It did **not** eliminate the
**one-time `undefined → {}`** that occurs on the very first servoyMenu seed before the
component has a selection — which is exactly the single-shot signature SVY-21686 reports.
The reset-to-`{}` / fresh-object-each-pass risk sites are enumerated in the component repo's
own `docs/SVY-21654-ng0100-investigation.md` (e.g. `svyOnChanges` selectedIndex/expandedIndex
branches, and the referentially-unstable `getSidenavClass()`/`get*FormStyle()`/
`getContainerStyle()` + inline `{container:true, reservedGap:50}` / `{name:...}` literals in
`sidenav.html`), several of which remain unaddressed and are plausible contributors to a
host-level `undefined → {}` flip.

## Ticket premise check

The ticket proposes no solution — it only reports the console error with a reproducing
solution. The premise that this is a real defect holds: it is a genuine zoneless
change-detection gap in the sidenav + servoyMenu path, consistent with the component's own
prior NG0100 investigation. The only premise to correct is any assumption that SVY-21686 is
a duplicate of SVY-21654: it is a **distinct, residual first-show** NG0100 (single
`undefined → {}` at launch), not the ~1 Hz `{"1":"customers"} ↔ {}` loop SVY-21654 fixed.

The one caveat worth stating up front to the human: the stack trace does not name the exact
host property (`??property` is minified). The fix must begin by reproducing with the
attached `tabpanelSize.servoy` and identifying which bound property on the sidenav element
flips `undefined → {}` (most likely `selectedIndex`/`expandedIndex` normalized to `{}`, or
the `servoyMenu`/`menu` object), then stabilizing that seed so it holds a single, stable
value before the host's first check completes.

## Approaches considered

1. **Fix the first-show seed/echo in the `servoy-extra-components` sidenav (recommended).**
   In `copyServoyMenu` and the `svyOnInit`/`svyOnChanges` selectedIndex/expandedIndex paths,
   ensure the template/host-bound values are set to their final stable value **once**,
   before/without triggering an after-checked `undefined → {}` flip (e.g. seed synchronously
   in `svyOnInit` rather than in a later microtask where feasible; avoid writing an
   empty-object into a two-way property that was `undefined`; keep references stable).
   Extend `sidenav.spec.ts` with a servoyMenu-with-no-isSelected first-show case asserting no
   NG0100 across the initial `detectChanges()`.
   - Pros: fixes the actual defect at its source; matches the SVY-21654/SVY-21678 pattern and
     the component's own documented fix set; testable with Vitest in that repo.
   - Cons: **cannot be built/tested from this `servoy-eclipse` repo** — the sidenav lives in
     the sibling repo with its own git + Vitest; the work and its commit belong there.
2. **Harden the generic host `FormComponent` / `JSMenu` converter in this repo.**
   - Pros: stays in this repo.
   - Cons: wrong layer — the host binding only reflects what the sidenav sets; masking it
     generically would paper over the component bug and risk every other component. Reject.
3. **No code change (accept the single launch-time console error).**
   - Pros: zero risk; it is one dev-mode-only message (NG0100 is dev-mode; prod skips the
     exhaustive check), no functional breakage reported.
   - Cons: it is a real CD gap that pollutes the console the team is actively cleaning
     (SVY-21654/SVY-21678), can mask other NG0100s, and the reporter filed it as a defect.
     Not recommended, but it is the honest low-risk fallback if the exact flipping property
     cannot be pinned down and stabilized safely.

## Recommendation

**PROCEED**, with the fix implemented in the **`servoy-extra-components` sibling repo**
(approach 1), not in this `servoy-eclipse` checkout. Concretely:

1. Reproduce with the attached `tabpanelSize.servoy` (sidenav fed by a `servoyMenu`, no
   pre-selected item) and confirm the single launch NG0100.
2. Identify the exact sidenav-element property the generated `FormComponent` template binds
   that flips `undefined → {}` (add one diagnostic log at the failing binding rather than
   theorizing — per the repo's "log first" guidance).
3. Make that seed stable and single-valued before the host's first check (fix in
   `sidenav.ts`, following the directions already catalogued in that repo's
   `docs/SVY-21654-ng0100-investigation.md`), and add a Vitest regression (`sidenav.spec.ts`)
   for the first-show servoyMenu case.

Note for the human: this repo (`servoy-eclipse`) cannot build or run the sidenav's Vitest
suite — that is the `servoy-extra-components` node project. If the preference is to keep all
work in one place, the case should be worked from the sibling repo's own tooling.

Alternative if step 2 cannot isolate a safe fix: fall back to NO_ACTION (approach 3) rather
than hardening the generic host (approach 2).

## Git history findings

- Component repo (`servoy-extra-components`, branch `master`, HEAD `45ef6dc` 2026-11-16
  "SVY-21678 fix slider console errors"):
  - `82aeb4e` (2026-10-27, lvostinar) "SVY-21654 fix repeating NG0100 in sidenav on
    server-set selection" — removed the transient `_selectedIndex.set({})` and the
    per-sync `selectedIndexChange` echo, added `applyingServerSelection`, switched
    `copyServoyMenu` from `detectChanges()` to `markForCheck()`. This targeted the
    **repeating** loop; the **first-show single `undefined → {}`** is the residual case
    SVY-21686 now reports. The fix is a refinement of this same code, not a revert.
  - `b734d62` (2026-10-27) SVY-21634 seeds `_selectedIndex`/`_expandedIndex` in `svyOnInit`
    via `parseIndex(...)`; `parseIndex(undefined)` returns `{}` (`sidenav.ts:247-251`),
    which is directly relevant to the `undefined → {}` transition.
  - `aff2812` (2026-10-30) SVY-21635 added `allowMultipleExpanded`; touches
    expand/collapse, not the servoyMenu seed echo — not the introducer.
  - The reset-to-`{}` + fresh-object template-method pattern predates the signals migration
    (per that repo's `docs/SVY-21654-ng0100-investigation.md` git notes: `0d16291`/`475de68`
    cPecican 2026-02-04), so fixing it refines the signals migration rather than reverting a
    recent deliberate decision.
- This repo (`servoy-eclipse`): the exhaustive dev-mode check that surfaces the error was
  enabled by `c9e85ecef9` "enable zoneless change detection with dev-mode exhaustive
  checks" (`src/main.ts`). The generic `FormComponent` host and `JSMenu` converter are not
  recent regressions. A prior SVY-21686 `docs/` spec does not exist in either repo
  (`docs/` was checked in both).
