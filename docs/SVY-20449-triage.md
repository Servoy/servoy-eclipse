# Triage Report — SVY-20449

**Verdict:** PROCEED

> **Status (Sep 2026): RESOLVED.** Three rounds of fixes landed (servoycore formcontainer,
> servoydefault tabpanel/tablesspanel, bootstrap tabpanel/accordion). A third reopening
> surfaced two remaining scroll containers — the servoydefault tabpanel's `getForm()`
> resolution and the bootstrap accordion's **outer** `.svy-accordion-scrollable` div — both
> now fixed and verified against the reporter's sample. See "Third reopening" below and the
> spec §3.7–§3.8.

## Reported problem
A form is larger than its container and has **both scrollbars set to "never"**. In NG1
(the legacy web client) the form correctly shows no scrollbars; in NG2 (the Titanium /
ng-modern client) scrollbars still appear.

The issue was fixed and closed once, then reopened. It has now been reopened a **second
time** (Laurian Vostinar, 2026-09-09: *"This is still not completely fixed, see sample"*,
attaching `testscrollbars.servoy`). Per the reporter's screenshot the remaining failure is
on a **default bootstrap tabpanel** (`bootstrapcomponents` tabpanel, tab labelled
`TAB_2`) which shows both a vertical and a horizontal scrollbar on the outer tabpanel even
though the inner form is set to scrollbars = never.

## Root-cause assessment
"Scrollbars never" on a form is stored as the form body part's layout overflow
(`overflow-x: hidden` / `overflow-y: hidden`). A container that hosts the form must copy
that overflow onto its own scroll container, otherwise the container's default
`overflow: auto` produces scrollbars regardless of the form setting.

The prior fixes patched two of the three hosting components, but **not** the bootstrap
tabpanel that the current sample uses:

1. **Servoycore form container** — `ServoyCoreFormContainer.getContainerStyle()` reads
   `formCache.parts[0].layout` overflow-x/-y and applies `overflowX`/`overflowY`.
   `com.servoy.eclipse.ngclient.ui/node/src/servoycore/formcontainer/formcontainer.ts`
   (commits `7e37e4b799`, `60cc8fd890`, Aug 2025).
2. **Servoydefault tabpanel / tablesspanel** — `BaseTabpanel.applyOverflowFromForm()`
   reads the form's body-part layout via the new `IFormCache.getBodyPartLayout()` and
   applies it; wired into `tabpanel.ts` and `tablesspanel.ts`.
   `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/tabpanel/*`
   and `.../projects/servoy-public/src/lib/services/servoy_public.service.ts`,
   `.../node/src/ngclient/types.ts` (commit `e086211dd4` / `14e6698c71`, Aug 2026;
   partial tablesspanel revert `cc36006c51`).
3. **Bootstrap tabpanel** — **NOT touched.** The NG2 component
   `ServoyBootstrapTabpanel` lives in the separate `bootstrapcomponents` repo
   (`components/projects/bootstrapcomponents/src/tabpanel/tabpanel.ts`). Its scroll
   container hardcodes overflow and never consults the contained form:

   ```ts
   // tabpanel.ts:37
   containerStyle = { position: 'relative', minHeight: '0px', overflow: 'auto' };
   ```

   `getContainerStyle()` (tabpanel.ts:95-142) mutates height / margin but never reads the
   form's body-part layout, so `overflow: auto` always stays on the
   `[ngbNavOutlet]` div (tabpanel.html:36). Result: scrollbars appear even when the inner
   form is scrollbars = never. This matches the reopened screenshot exactly.

For contrast, the **NG1** bootstrap tabpanel does *not* set overflow in
`getContainerStyle` (`components/tabpanel/tabpanel.js:322` returns only
`position`/`minHeight`); overflow is handled by CSS on `.tab-content` while the inner
form wrapper still honors the form's own scrollbar setting — which is why NG1 works and
NG2 does not.

## Ticket premise check
The ticket's premise (NG2 does not honor scrollbars = never) is **correct and confirmed**.
The reopening comments correctly narrowed it to the default tab panel component. The only
gap is that the Aug-2026 fix addressed the **servoydefault** tabpanel but the customer's
sample uses the **bootstrap** tabpanel, in a different repository, so the fix never reached
the failing component. No API redesign is needed — the required public API
(`ServoyPublicService.getFormCacheByName()` + `IFormCache.getBodyPartLayout()`) already
exists and is exported from `@servoy/public`, so the bootstrap tabpanel can consume it the
same way `servoydefault`'s `BaseTabpanel.applyOverflowFromForm()` does.

## Approaches considered
1. **Apply the same overflow-from-form logic in the bootstrap tabpanel** (recommended) —
   In `bootstrapcomponents`, have `ServoyBootstrapTabpanel.getContainerStyle()` read the
   contained form's body-part layout overflow (via `getFormCacheByName().getBodyPartLayout()`)
   and set `overflowX`/`overflowY` on `containerStyle`, deleting the blanket
   `overflow: 'auto'` when the form specifies hidden. Mirrors the proven servoydefault fix.
   - Pros: consistent with the existing, validated fix; reuses already-exported public API;
     small and localized; matches NG1 behavior.
   - Cons: change lives in the `bootstrapcomponents` repo (separate build/release); may also
     want the same treatment on the accordion/other bootstrap containers if they share the
     defect (needs a quick check during spec).
2. **Fix via CSS only in the bootstrap tabpanel** — rely on a class analogous to NG1's
   `.relativeMaxSize` and let the inner form wrapper own overflow.
   - Pros: no TS/service dependency.
   - Cons: CSS cannot read the per-form scrollbar setting (never/auto/scroll); it is a
     static rule and cannot reproduce the dynamic per-form behavior. Would not correctly
     handle forms that *do* want scrollbars. Rejected.
3. **No code change** — treat as user-side / expected.
   - Pros: none.
   - Cons: NG1 vs NG2 divergence is a real regression; the form's documented scrollbars =
     never is being ignored. This is a genuine Servoy bug. Rejected.

## Recommendation
**PROCEED with approach 1.** Implement the overflow-from-form behavior in the
`bootstrapcomponents` tabpanel (`ServoyBootstrapTabpanel.getContainerStyle()` in
`components/projects/bootstrapcomponents/src/tabpanel/tabpanel.ts`), reusing the existing
`ServoyPublicService.getFormCacheByName()` + `IFormCache.getBodyPartLayout()` API that the
prior servoydefault fix introduced. During the spec, verify whether the bootstrap
`tablesspanel`/accordion and any other bootstrap form-container components share the same
hardcoded `overflow: auto` and need the same treatment, and add a component test asserting
that a contained form with body-part `overflow-x/-y: hidden` produces no scrollbars on the
tabpanel container.

Note: the fix belongs in the `bootstrapcomponents` repository
(`D:\Eclipse\202603Workspace\bootstrapcomponents`), not in `servoy-eclipse`. Only the
public-API additions it depends on already live in `servoy-eclipse`.

## Git history findings
- `7e37e4b799`, `60cc8fd890` (Aug 18 2025, dtimut) — first fix, servoycore
  `formcontainer.ts` overflow-from-form. Validated 2025-09-02, then reopened.
- `e086211dd4` / `14e6698c71` (Aug 21 2026, Diana Bunaciu) — second fix: added
  `getBodyPartLayout()` to `IFormCache`/`FormCache`, `applyOverflowFromForm()` in
  servoydefault `BaseTabpanel`, wired into `tabpanel.ts`/`tablesspanel.ts`.
- `cc36006c51` (Aug 21 2026) — partial revert removing the dynamic overflow behavior from
  servoydefault `tablesspanel` (restored inline `overflow:auto`).
- No prior commit under SVY-20449 touched the `bootstrapcomponents` repo, confirming the
  bootstrap tabpanel was never fixed. No existing `docs/SVY-20449*.spec.md` was found.
- `c907b53` / `481d0f0` (Sep 14–15 2026, Diana Bunaciu, `bootstrapcomponents`) — third fix:
  `applyOverflowFromForm()` on `bts_basetabpanel`, wired into the bootstrap tabpanel
  `[ngbNavOutlet]` container and the accordion inner `ngbAccordionBody` (`getBodyStyle`).

## Third reopening — remaining findings (Sep 2026)

After `c907b53`/`481d0f0` the reporter's sample still showed scrollbars in two places
(screenshots: default tabpanel with `tabOrientation` set, and the accordion). Both were
caused by a scroll container the earlier fixes did **not** reach:

1. **servoydefault default tabpanel (`servoy-eclipse`).** `BaseTabpanel.applyOverflowFromForm()`
   resolved the contained form via `this.getForm()` (no-arg). `ServoyDefaultTabpanel`
   overrides `getForm(tab)` to gate on `visibleTabIndex`, so the no-arg call returned
   `null` and the form's `overflow: hidden` was never copied onto the `[ngbNavOutlet]`
   container. The `tablesspanel` variant does **not** override `getForm`, so it worked —
   which is why the defect only appeared when `tabOrientation` produced the real tabpanel
   (tabs shown) rather than the tablesspanel fallback (`tabOrientation` HIDE / single tab).
   Root cause: form-name resolution coupled to the display-gating override.

2. **bootstrap accordion (`bootstrapcomponents`).** `getBodyStyle()` corrected the inner
   `ngbAccordionBody`, but the **outer** `<div class="bts-accordion svy-accordion-scrollable">`
   carries `overflow-y: auto` from the `.svy-accordion-scrollable` CSS rule
   (`svy_bootstrapcomponents.css`). That outer container produced the remaining vertical
   scrollbar. The overflow-from-form logic was never applied to it.

Both were confirmed fixed against the sample (no scrollbars on the tabpanel, accordion, or
form-container variants). See the spec §3.7–§3.8 for the implemented changes.
