# Spec: SVY-20449 — form "scrollbars = never" ignored in NG2 (all host containers)

> **Single source of truth.** This spec (with `SVY-20449-triage.md`) is the canonical
> record for SVY-20449 across **both** repositories — `servoy-eclipse` (servoycore
> formcontainer, servoydefault tabpanel/tablesspanel, `@servoy/public` API) and
> `bootstrapcomponents` (bootstrap tabpanel + accordion). The `bootstrapcomponents` repo no
> longer carries its own copy; see §8 for the bootstrap-repo details, cross-branch port, and
> commit table.

## 1. Goal
Make every host container that shows a contained form honor the form's "scrollbars = never"
setting in NG2/TiNG, so no scrollbars appear on the host when the inner form's body part is
set to `overflow: hidden`. The final gaps closed here are the **bootstrap** tabpanel
(`ServoyBootstrapTabpanel`) and accordion, plus the **servoydefault** default tabpanel when
`tabOrientation` is set. The reporter's sample (`testscrollbars.servoy`, tab `TAB_2`) uses a
bootstrap tabpanel; the accordion and the servoydefault tabpanel are the third-reopening
cases.

## 2. Background

### 2.1 What "scrollbars = never" means
A Servoy form with scrollbars set to "never" stores this as the body part's layout
overflow (`overflow-x: hidden` / `overflow-y: hidden`). Any container that hosts the form
must copy that overflow onto its own scroll container. Otherwise the container's default
`overflow: auto` produces scrollbars regardless of the form setting.

### 2.2 NG1 vs NG2 divergence
- **NG1** bootstrap tabpanel does *not* set overflow in `getContainerStyle`
  (`components/tabpanel/tabpanel.js` returns only `position`/`minHeight`); overflow is
  handled by CSS on `.tab-content` while the inner form wrapper still honors the form's own
  scrollbar setting. NG1 therefore works.
- **NG2** bootstrap tabpanel hardcodes `overflow: 'auto'` on `containerStyle` and never
  consults the contained form, so scrollbars always appear. This is the regression.

### 2.3 The three hosting components and what was already fixed
1. **Servoycore form container** — `ServoyCoreFormContainer.getContainerStyle()` reads
   `formCache.parts[0].layout` overflow and applies `overflowX`/`overflowY`
   (`com.servoy.eclipse.ngclient.ui/node/src/servoycore/formcontainer/formcontainer.ts`,
   commits `7e37e4b799`, `60cc8fd890`, Aug 2025). **Fixed.**
2. **Servoydefault tabpanel / tablesspanel** — `BaseTabpanel.applyOverflowFromForm()`
   reads the form's body-part layout via `IFormCache.getBodyPartLayout()` and applies it;
   wired into `tabpanel.ts` / `tablesspanel.ts`
   (`.../projects/servoydefault/src/lib/tabpanel/*`, commit `e086211dd4` / `14e6698c71`,
   Aug 2026; partial tablesspanel revert `cc36006c51` restored inline `overflow:auto`).
   **Fixed.**
3. **Bootstrap tabpanel** — `ServoyBootstrapTabpanel`, in the separate
   `bootstrapcomponents` repo. **NOT fixed** — this is the remaining defect.

### 2.4 The bootstrap tabpanel defect
`components/projects/bootstrapcomponents/src/tabpanel/tabpanel.ts`:

```ts
// line 37
containerStyle = { position: 'relative', minHeight: '0px', overflow: 'auto' };
```

`getContainerStyle(element)` (tabpanel.ts:95-142) mutates height / margin but never reads
the form's body-part layout, so `overflow: auto` always stays on the `[ngbNavOutlet]` div
(`tabpanel.html:36`). Result: scrollbars appear even when the inner form is
scrollbars = never. This matches the reopened screenshot exactly.

### 2.5 The public API to reuse (already available)
The servoydefault fix introduced the API this fix needs, and it is already exported from
`@servoy/public`:
- `ServoyPublicService.getFormCacheByName(containedForm): IFormCache`
  (`.../projects/servoy-public/src/lib/services/servoy_public.service.ts:85`)
- `IFormCache.getBodyPartLayout?(): { [property: string]: string }` (same file, line 130),
  implemented in `.../node/src/ngclient/types.ts:91` as `return this._parts[0]?.layout;`

The bootstrap repo depends on `@servoy/public` `^2026.9.3` (see
`bootstrapcomponents/components/package.json`), so the published `@servoy/public` version it
consumes must contain `getBodyPartLayout`. This is an open question to confirm (§7).

### 2.6 Reference implementation to mirror
`BaseTabpanel.applyOverflowFromForm()` in
`.../projects/servoydefault/src/lib/tabpanel/basetabpanel.ts:80-101`:

```ts
applyOverflowFromForm(containerStyle: { [property: string]: any }) {
    const formName = this.getForm();
    if (formName && this.servoyPublicService) {
        const formCache = this.servoyPublicService.getFormCacheByName(formName);
        if (formCache) {
            const layout = formCache.getBodyPartLayout ? formCache.getBodyPartLayout() : null;
            if (layout?.['overflow-x']) containerStyle['overflowX'] = layout['overflow-x'];
            else delete containerStyle['overflowX'];
            if (layout?.['overflow-y']) containerStyle['overflowY'] = layout['overflow-y'];
            else delete containerStyle['overflowY'];
            if (layout?.['overflow-x'] || layout?.['overflow-y']) delete containerStyle['overflow'];
        }
    }
}
```

Wired via `getContainerStyle()` in servoydefault `tabpanel.ts:54`
(`this.applyOverflowFromForm(this.containerStyle);`).

### 2.7 DI difference in the bootstrap tabpanel
Unlike servoydefault (constructor injection), `ServoyBootstrapTabpanel` is a
signal/standalone component that uses `inject()` and `input()` (see tabpanel.ts and its
base `bts_basetabpanel.ts`). It does **not** currently inject `ServoyPublicService`. The
accordion sibling shows the established pattern in this repo:
`protected readonly servoyPublic = inject(ServoyPublicService);`
(`accordion.ts:23`, used at `accordion.ts:108`). The fix must inject `ServoyPublicService`
the same way rather than adding a constructor.

### 2.8 The `getForm` semantics in the bootstrap tabpanel
`ServoyBootstrapTabpanel.getForm(tab)` returns the form only for the currently visible tab
(tabpanel.ts:232). `ServoyBootstrapBaseTabPanel.getForm(tab)` (bts_basetabpanel.ts:43)
returns `tab.containedForm` for the selected tab. The overflow logic must resolve the
**currently selected/visible** contained form name, i.e. the form actually rendered in the
`[ngbNavOutlet]`. Use the selected tab via `this.tabs()?.[this.getRealTabIndex()]?.containedForm`.

## 3. Design

### 3.1 Inject ServoyPublicService
Add `protected readonly servoyPublic = inject(ServoyPublicService);` to
`ServoyBootstrapTabpanel` (mirroring `accordion.ts:23`) and import `ServoyPublicService`
from `@servoy/public`.

### 3.2 Add applyOverflowFromForm to the bootstrap tabpanel
Add a private method that reads the selected contained form's body-part layout and mutates
`containerStyle`, mirroring `BaseTabpanel.applyOverflowFromForm()`:

```ts
private applyOverflowFromForm() {
    const cs = this.containerStyle as { [property: string]: any };
    const formName = this.tabs()?.[this.getRealTabIndex()]?.containedForm;
    if (formName) {
        const formCache = this.servoyPublic.getFormCacheByName(formName);
        const layout = formCache?.getBodyPartLayout ? formCache.getBodyPartLayout() : null;
        if (layout?.['overflow-x']) cs['overflowX'] = layout['overflow-x'];
        else delete cs['overflowX'];
        if (layout?.['overflow-y']) cs['overflowY'] = layout['overflow-y'];
        else delete cs['overflowY'];
        if (layout?.['overflow-x'] || layout?.['overflow-y']) delete cs['overflow'];
        else cs['overflow'] = 'auto';
    }
}
```

Notes:
- The initializer `containerStyle = { position, minHeight, overflow: 'auto' }` stays; the
  method deletes `overflow` and sets `overflowX`/`overflowY` only when the form specifies
  hidden, and restores `overflow: 'auto'` otherwise. This keeps default (scrollbars = auto)
  behavior unchanged and only removes scrollbars when the form asks for it.
- Because `containerStyle` is a mutated object reused across renders, resetting
  `overflow`/`overflowX`/`overflowY` on every call prevents a stale hidden value from
  leaking when the user switches to a tab whose form wants scrollbars.

### 3.3 Call site
Invoke `this.applyOverflowFromForm()` inside `getContainerStyle(element)` (tabpanel.ts:95),
before `return this.containerStyle;` (line 141). `getContainerStyle` is already called from
the template's `[ngStyle]="getContainerStyle(element)"` binding (tabpanel.html:36) on every
change-detection pass, so per-tab overflow stays correct as tabs change.

### 3.4 Sibling components — do they share the defect?
- **`ServoyBootstrapAccordion`** — **shares the defect.** `accordion.ts` does not use a
  `containerStyle` object; instead `accordion.html:10` hardcodes `overflow: auto` as an
  inline `style` string on the `ngbAccordionBody` div. Because it is a template inline
  style (not a mutated `containerStyle` object), it cannot be fixed through the same
  `getContainerStyle()` path. Instead it is fixed with a `computed` signal
  (`bodyOverflow`) that reads the selected form's body-part layout and is bound to the
  div via `[style.overflow]` / `[style.overflow-x]` / `[style.overflow-y]`. See §3.6.
- **`ServoyBootstrapTablessPanel`** — `getContainerStyle()` (tablesspanel.ts:89) returns
  `{ position: 'relative', minHeight }` and does **not** set `overflow`, so it does not
  produce the extra scrollbar and does not need the fix. (Consistent with the servoydefault
  tablesspanel revert `cc36006c51`.)
- **Split pane** — there is **no** split-pane component in the `bootstrapcomponents` repo
  (component list: accordion, tablesspanel, tabpanel + field/form components). Split pane
  lives in servoydefault/servoycore, out of scope here.

Both the **tabpanel** and the **accordion** require the change; tablesspanel does not.

### 3.6 Accordion overflow (computed signal + template binding)
The accordion is a signal/OnPush standalone component that already injects
`ServoyPublicService` (`accordion.ts:23`). Add a `computed` signal that resolves the
selected contained form's body-part layout and yields the overflow values, mirroring the
tabpanel logic but shaped for template style bindings:

```ts
readonly bodyOverflow = computed(() => {
    const formName = this.tabs()?.[this.getRealTabIndex()]?.containedForm;
    if (formName) {
        const formCache = this.servoyPublic.getFormCacheByName(formName);
        const layout = formCache?.getBodyPartLayout ? formCache.getBodyPartLayout() : null;
        if (layout?.['overflow-x'] || layout?.['overflow-y']) {
            return {
                overflow: null,
                overflowX: layout['overflow-x'] ?? null,
                overflowY: layout['overflow-y'] ?? null
            };
        }
    }
    return { overflow: 'auto', overflowX: null, overflowY: null };
});
```

`accordion.html:10` drops the inline `overflow: auto` and binds instead:
```html
[style.overflow]="bodyOverflow().overflow"
[style.overflow-x]="bodyOverflow().overflowX"
[style.overflow-y]="bodyOverflow().overflowY"
```
A `null` style binding removes the property, so when the form is scrollbars = never the
`overflow` shorthand is not emitted and `overflow-x`/`overflow-y` become `hidden`; for a
default form `overflow: auto` is restored and the per-axis properties are cleared. Using
`computed` (not a method mutating shared state) avoids NG0600 signal-write-during-render
issues and re-evaluates automatically when `tabs`/`tabIndex` change.

### 3.5 Git history
See §2.3. No prior commit under SVY-20449 touched the `bootstrapcomponents` repo, confirming
the bootstrap tabpanel was never fixed. The required public API already exists in
`@servoy/public` from the Aug-2026 servoydefault fix. The bootstrap tabpanel + accordion
inner-body fix landed as `c907b53` / `481d0f0` (Sep 2026).

### 3.7 Third reopening — servoydefault default tabpanel (servoy-eclipse)
After the bootstrap fix the reporter's sample still showed scrollbars on the **default
tabpanel when `tabOrientation` is set** (screenshot: `TAB_2`). Root cause:
`BaseTabpanel.applyOverflowFromForm()` resolved the contained form via `this.getForm()`
(no argument), but `ServoyDefaultTabpanel` overrides `getForm(tab)` to gate on
`visibleTabIndex` (`tabpanel.ts:110`). Called with no argument, the override returns `null`,
so the form's `overflow: hidden` was never copied onto the `[ngbNavOutlet]` container. The
`servoydefault-tablesspanel` variant does not override `getForm`, so it already worked —
which is exactly why the defect only appeared once `tabOrientation` produced the real
tabpanel (tabs visible) instead of the tablesspanel fallback (`tabOrientation` HIDE / a
single tab).

Fix (in `basetabpanel.ts`): decouple form-name resolution from the display-gating override.
- Add `protected getSelectedFormName(): string` holding the base selection logic (select the
  tab at `getRealTabIndex()`, else the first tab, then return `selectedTab.containsFormId`).
- `applyOverflowFromForm()` calls `getSelectedFormName()` instead of `getForm()`, so the
  overridden `getForm(tab)` no longer suppresses it.
- `getForm()` is refactored to delegate to `getSelectedFormName()` for its no-arg branch,
  preserving existing behavior.

Files: `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/tabpanel/basetabpanel.ts`.
Tests: `tabpanel.spec.ts` — asserts the `[ngbNavOutlet]` container gets `overflowX/Y: hidden`
(no `overflow: auto`) for a `scrollbars=never` form, and keeps `overflow: auto` otherwise.

### 3.8 Third reopening — bootstrap accordion outer container (bootstrapcomponents)
The accordion still showed a vertical scrollbar (screenshots: circled scrollbar to the right
of the body). The `getBodyStyle()` fix (§3.6 / `c907b53`) corrected the inner
`ngbAccordionBody`, but the **outer** container
`<div class="bts-accordion svy-accordion-scrollable">` (`accordion.html:1`) carries
`overflow-y: auto` from the `.svy-accordion-scrollable` rule in
`svy_bootstrapcomponents.css`. The overflow-from-form logic was never applied to that outer
div, so it kept producing the scrollbar.

Fix (mirrors `getBodyStyle`): add `getContainerStyle()` returning `{ overflowY: 'auto' }` and
running `applyOverflowFromForm()` over it, and bind it on the outer div via
`[ngStyle]="getContainerStyle()"`. When the selected form is `scrollbars=never`,
`applyOverflowFromForm` replaces `overflowY: 'auto'` with the form's `overflow-y: hidden`;
otherwise the outer div keeps `overflow-y: auto`.

Files: `components/projects/bootstrapcomponents/src/accordion/accordion.ts` +
`accordion.html`. Tests: `accordion.cy.ts` — asserts both the outer `getContainerStyle()`
and inner `getBodyStyle()` honor `scrollbars=never` and fall back to `auto` for a default
form.

Note: the shared `applyOverflowFromForm()` on `bts_basetabpanel` already resolves the
selected tab via `tabs()[getRealTabIndex()]` (not a display-gating override), so the bootstrap
side needed no equivalent to the §3.7 `getSelectedFormName()` change.

## 4. Implementation plan
All changes are in the **bootstrapcomponents** repo
(`D:\Eclipse\202603Workspace\bootstrapcomponents`), not `servoy-eclipse`.

1. `components/projects/bootstrapcomponents/src/tabpanel/tabpanel.ts`
   - Import `ServoyPublicService` from `@servoy/public` (add to existing import on line 2).
   - Add `protected readonly servoyPublic = inject(ServoyPublicService);` to the component.
   - Add the private `applyOverflowFromForm()` method (§3.2).
   - Call `this.applyOverflowFromForm();` in `getContainerStyle(element)` just before the
     `return this.containerStyle;` at line 141.
2. `components/projects/bootstrapcomponents/src/accordion/accordion.ts` + `accordion.html`
   (§3.6)
   - Import `computed` from `@angular/core`.
   - Add the `bodyOverflow` computed signal (accordion already injects `ServoyPublicService`).
   - In `accordion.html:10`, remove the inline `overflow: auto` from the `ngbAccordionBody`
     div's `style` and add `[style.overflow]` / `[style.overflow-x]` / `[style.overflow-y]`
     bindings to `bodyOverflow()`.
   - Confirm `tablesspanel` needs no change; confirm no split-pane component exists.
3. `components/projects/bootstrapcomponents/src/tabpanel/tabpanel.spec.ts` and
   `components/projects/bootstrapcomponents/src/accordion/accordion.spec.ts`
   - Add a test asserting that when the selected tab's contained form has a body-part layout
     of `overflow-x: hidden` / `overflow-y: hidden`, `getContainerStyle` produces
     `overflowX: 'hidden'` / `overflowY: 'hidden'` and no `overflow: 'auto'`.
   - Add a complementary test asserting that with no hidden overflow (defaulty form),
     `overflow: 'auto'` remains so normal scrolling still works.
   - Mock `ServoyPublicService.getFormCacheByName` to return an `IFormCache` whose
     `getBodyPartLayout()` yields the desired overflow map (align with the existing
     `ServoyPublicTestingModule` / `ServoyApiTesting` test setup already used in the spec).
4. Validate in the bootstrap repo: typecheck, `ng lint`, and run the tabpanel spec
   (the repo uses vitest per `tabpanel.spec.ts`).

## 5. Acceptance criteria
- [ ] A bootstrap tabpanel whose selected contained form has scrollbars = never
      (body-part `overflow-x: hidden` / `overflow-y: hidden`) shows **no** scrollbars on the
      outer tabpanel container in NG2, matching NG1 and the servoydefault tabpanel.
- [ ] A bootstrap tabpanel whose contained form uses default/auto scrollbars still shows
      scrollbars when content overflows (no regression).
- [ ] Switching between a scrollbars-never tab and a scrollbars-auto tab updates the
      container overflow correctly each time (no stale hidden/auto value).
- [ ] The reopened sample `testscrollbars.servoy` (tab `TAB_2`) renders without the extra
      vertical/horizontal scrollbars.
- [ ] A component test in `tabpanel.spec.ts` asserts the hidden-overflow and default-overflow
      cases.
- [ ] The bootstrap **accordion** also honors the contained form's scrollbars = never
      (no scrollbars on the accordion body), and default forms still scroll.
- [ ] `tablesspanel` is confirmed unaffected and no split-pane component exists; the
      accordion is fixed with the computed-signal/template-binding pattern; a note is
      recorded in the PR.
- [ ] Bootstrap repo typecheck + lint pass; the tabpanel spec passes.
- [x] **(Third reopening)** The servoydefault default tabpanel with `tabOrientation` set
      honors `scrollbars=never` — form-name resolution decoupled from the `getForm(tab)`
      display override via `getSelectedFormName()` (§3.7). Verified against the sample.
- [x] **(Third reopening)** The bootstrap accordion's **outer** `.svy-accordion-scrollable`
      container honors `scrollbars=never` via `getContainerStyle()` + `[ngStyle]` (§3.8),
      and still scrolls for default forms. Verified against the sample.

## 6. Out of scope
- Any change in `servoy-eclipse` (the public API it depends on already exists and is
  exported). No `servoy-eclipse` code changes are needed for this fix.
- Reworking the servoycore form container or servoydefault tabpanel/tablesspanel (already
  fixed).
- `tablesspanel` (does not set `overflow`) — no change expected.
- Split pane — no such component exists in the `bootstrapcomponents` repo.
- Changing the storage/representation of the form scrollbar setting or the
  `getBodyPartLayout()` API shape.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Does the published `@servoy/public` `^2026.9.3` consumed by the bootstrap repo include `getBodyPartLayout()`? | Dev | Resolved — `package-lock.json` pins `2026.9.3` (registry tarball); `getBodyPartLayout()` was introduced by commit `14e6698c71` (the earlier servoydefault SVY-20449 fix) before the 2026.9 train, so the published package contains it. No version bump needed. |
| Does `accordion.html` wrap its content in a scroll container with hardcoded `overflow`? Check accordion and split pane. | Dev | Resolved — yes, `accordion.html:10` hardcodes `overflow: auto` inline; fixed via the `bodyOverflow` computed signal + style bindings (§3.6). Split pane: no such component exists in this repo. |
| Confirm the bootstrap `[ngbNavOutlet]` div is the exact element carrying the scrollbars in the reopened sample (vs the inner form wrapper), so applying overflow to `containerStyle` fully removes them. | Dev/QA | Resolved — verified against the sample; no scrollbars on the tabpanel container. |
| Third reopening: which containers still scrolled after `c907b53`? | Dev | Resolved — (a) servoydefault tabpanel `[ngbNavOutlet]` (form name resolved to `null` via the `getForm(tab)` override); (b) bootstrap accordion **outer** `.svy-accordion-scrollable` div (`overflow-y: auto` from CSS). Both fixed (§3.7–§3.8) and verified. |

## 8. bootstrapcomponents repo — details, cross-branch port, and commits

This section is the canonical record for the `bootstrapcomponents` side (previously a
separate `components/docs/SVY-20449-*` pair, now removed in favor of this file).

### 8.1 Components and root cause
The bootstrap host components hardcoded `overflow: auto` and never consulted the contained
form's body-part overflow:
- **tabpanel** — `containerStyle = { position, minHeight, overflow: 'auto' }` on the
  `[ngbNavOutlet]` div (`tabpanel.ts` / `tabpanel.html`).
- **accordion** — inner `ngbAccordionBody` inline `overflow: auto`, **and** the outer
  `<div class="bts-accordion svy-accordion-scrollable">` whose `.svy-accordion-scrollable`
  CSS rule (`svy_bootstrapcomponents.css`) forces `overflow-y: auto` on the root.
- **tablesspanel** — sets only `position`/`minHeight`; unaffected, out of scope.
- No split-pane component exists in this repo.

### 8.2 Shared helper
`applyOverflowFromForm(containerStyle)` lives on `bts_basetabpanel.ts`; it resolves the
selected tab via `tabs()[getRealTabIndex()]` (not a display-gating override, so it does not
have the servoydefault §3.7 problem), reads the form's body-part overflow, sets
`overflowX`/`overflowY`, and deletes the blanket `overflow` when an axis is constrained.
Tabpanel calls it in `getContainerStyle()`; accordion calls it in both `getBodyStyle()`
(inner body) and `getContainerStyle()` (outer root, `[ngStyle]` on `accordion.html:1`).

### 8.3 Published `@servoy/public` type gap (build break) — F1
The published `IFormCache` `.d.ts` does **not** declare `getBodyPartLayout?()` in any pinned
version (`2024.3.0`, `2025.3.0`, `2025.9.1`, `2026.9.3`), so a guarded call failed the CI
production build with `TS2339`. Fix: cast `getFormCacheByName()` to a local structural type
`{ getBodyPartLayout?(): { [property: string]: string } }`, keeping the runtime
optional-chaining guard (no-op on older runtimes). Applied in `bts_basetabpanel.ts`.

### 8.4 Accordion root scroll — F2 / F2b
The accordion **body** was correctly `overflow: hidden`, but the **root**
`.svy-accordion-scrollable` still scrolled. First observed and fixed on `2025.12`
(`getRootStyle()` + `[ngStyle]` on the root). Originally believed 2025.12-only, but
re-verified on `2026.6`: the root also carries `svy-accordion-scrollable`
(`overflow-y: auto`, `svy_bootstrapcomponents.css:82-85`), so it scrolled there too — this is
the third-reopening accordion case. Fixed on `2026.6` as `getContainerStyle()` (seeded
`{ overflowY: 'auto' }`, run through `applyOverflowFromForm`) bound via
`[ngStyle]="getContainerStyle()"` on `accordion.html:1`; functionally identical to the
2025.12 `getRootStyle()`.

### 8.5 Standalone accordion on master — F3
On `master` the accordion is `standalone: true`; adding the `[ngStyle]` binding surfaced
`NG8002` because `NgStyle` was not in `imports`. Fixed by adding `NgStyle` to the component
`imports`. On `2026.6` and earlier the accordion is `standalone: false` and gets `NgStyle`
from a shared module — no import change needed.

### 8.6 Cross-branch commit table
The bootstrap fix was ported across the maintained branches.

| Branch | Inner tabpanel/accordion fix | Cast fix (F1) | Root fix (F2/F2b) | NgStyle (F3) |
|--------|------------------------------|---------------|-------------------|--------------|
| 2024.3 | — | `640d554` | n/a | n/a |
| 2025.3 | — | `075cc1e` | n/a | n/a |
| 2025.06 | — | `ae9a742` | n/a | n/a |
| 2025.9 | — | `16c8fa8` | n/a | n/a |
| 2025.12 | — | `cf4c402` | `e50b3b1` | n/a |
| 2026.6 | `c907b53` | `481d0f0` | (this change) | n/a |
| master | — | `a9ab64e` | n/a | included in `a9ab64e` |

Note: on `2026.6` the tabpanel/accordion inner-body fix and the outer-root accordion fix are
the ones this spec's third-reopening work (§3.6, §3.8, §8.4) covers.
