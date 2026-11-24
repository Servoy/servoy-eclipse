# Triage Report — SVY-21792

**Verdict:** PROCEED

## Reported problem
Legacy `servoydefault` TabPanel with `borderType` = Matte: the border shows in NG1 but not in NG2 (TiNG). Reported on 2024.3.13 LTS. The ticket proposes no solution. Attachments: `NG1.png`, `NG2.png`, `border_property.png`, `tab_border_ting.rar` (sample solution). I did not download them. SVY-21793 is a closed duplicate of this ticket.

## Root-cause assessment
The component lives in this repo, in `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/tabpanel/`. The server-side border serialization is in servoy-client (`BorderPropertyType.java`) and looks fine.

1. **`ServoyDefaultTabpanel` (the tabbed variant) never applies `borderType`.**
   - `BaseTabpanel` extends `ServoyBaseComponent`, not `ServoyDefaultBaseComponent`. The latter is where `PropertyUtils.setBorder(...)` is called for `borderType` (`basecomponent.ts` line 72).
   - `tabpanel.html` and `tabpanel.ts` never read `borderType`. `getContainerStyle()` only sets height and overflow.
   - So no border is rendered for any border type on the tabbed panel. This is the most likely scenario for the ticket.

2. **Even where it is applied (`ServoyDefaultTablesspanel`), Matte would be wrong.**
   - Commit `0cbe7bcd10` (SVYX-1071) added the `borderStyle` getter in `basetabpanel.ts`. It builds `"${borderWidth} ${borderStyle} ${borderColor}"` and is used in `tablesspanel.ts` (`containerStyle['border']`).
   - The server sends Matte as `borderWidth: "T Rpx Bpx Lpx "` (multi-value, trailing space), `borderStyle: "solid"`, `borderColor: <color>` (`BorderPropertyType.java` lines 343-353).
   - The CSS `border` shorthand accepts only one width, so `border: 1px 2px 1px 2px  solid red` is invalid and is dropped.
   - The same applies to Bevel and Etched, which send multi-value colors, and to SpecialMatte (per-side widths and colors, no `borderWidth`/`borderColor` keys).
   - Only Line borders work, because they have a single width and color.
   - It also ignores `TitledBorder`.

3. **The working path for other components is `PropertyUtils.setBorder`** (`servoy-public/.../utils/property_utils.ts`). It sets each `borderStyle` key as an individual CSS property (`border-width`, `border-style`, `border-color`, ...), which handles multi-value Matte. It also handles `TitledBorder` through a fieldset. The tabpanels do not use it.

## Ticket premise check
There is no proposed solution. The symptom is valid and is in this repo's TiNG code. It is not a server-side or user-configuration issue. The tabless panel was only partially fixed for Line borders in SVYX-1071.

## Approaches considered
1. **Apply `PropertyUtils.setBorder` to the container element for `borderType` changes in `BaseTabpanel`/`ServoyDefaultTabpanel`/`ServoyDefaultTablesspanel`, and remove the hand-built `borderStyle` shorthand.**
   - Pros: consistent with every other servoydefault component; fixes Matte, Bevel, Etched, SpecialMatte and Titled in both panels.
   - Cons: where to apply it needs care. NG1 draws the border around the whole panel, including the tab strip. Also `setBorder` with `TitledBorder` moves child nodes into a fieldset, which can clash with ngbNav and OnPush. It may be better to skip Titled or handle it separately.
2. **Apply only the border CSS properties on the container via `[ngStyle]` using `borderStyle` keys directly (no fieldset).**
   - Pros: simplest, OnPush friendly, no DOM moves.
   - Cons: no TitledBorder support (which is also missing today).
3. **Patch only the Matte case in the existing `borderStyle` getter.**
   - Pros: minimal.
   - Cons: still leaves the tabbed panel without a border, and Bevel/Etched/SpecialMatte stay broken.
4. **No code change.**
   - Pros: none.
   - Cons: a reproducible NG1 vs NG2 regression remains. Not acceptable.

## Recommendation (revised after reviewer input)
Reviewer constraints: `borderStyle` is a legacy property, so the existing `borderStyle` getter in
`basetabpanel.ts` must NOT be removed, and the servoydefault tabpanel spec must NOT change.

NG1 reference (`servoy-client/servoy_ngclient/war/servoydefault/tabpanel/tabpanel.js`, line 690):
the border is applied once, on the outer wrapper div that fills the whole component
(`<div style='height:100%;width:100%;position:absolute;' svy-border='model.borderType' ...>`).
So the border encloses the tab strip and the content, for every template (tabbed, tabless, accordion).
The generic `svy-border` directive applies each border key as its own CSS property.

Recommended: mirror this in TiNG. Apply `borderType` to the outer container of the tab panel
(the equivalent of the NG1 wrapper) through `PropertyUtils.setBorder`, like the other servoydefault
components, for both the tabbed and the tabless panel. Keep the `borderStyle` getter and the spec
untouched. For the tabless panel, make sure the existing `containerStyle['border']` shorthand does
not conflict with (double-draw) the new border. Fix it without removing the getter.

The previous recommendation below is superseded where it says to remove the getter.

(Original) Approach 2, with the option to move to 1 if TitledBorder is wanted.
- Add a `borderType` handler to `BaseTabpanel`/the two panels that applies each `borderType.borderStyle` key as a CSS property on the right element.
  - Tabbed panel: the outer `.svy-tabpanel` div.
  - Tabless panel: replace the shorthand in `tablesspanel.ts`.
- Remove the now-wrong `borderStyle` getter.
- Add specs in `tabpanel.spec.ts` and `tablesspanel.spec.ts` for Matte (per-side widths), Line, and null `borderType`.
- This is `lts_2024` code. Check whether master has the same code and needs a forward port. Fix versions on the ticket are 2026.12, 2024.3.13 LTS, 2025.3.8 LTS and 2026.3.3 LTS.
- Open points to confirm during spec:
  - Whether the border should enclose the tab strip (NG1 behavior).
  - Whether a border on the panel affects the `calc(100% - tabs height)` container height.

## Git history findings
- `0cbe7bcd10` (SVYX-1071, Aug 2025): added the `borderStyle` getter and `[ngStyle]="{ border: borderStyle }"` for the tabless panel only, based on single-width line borders.
- `4e0e34245e` (SVY-20512), `e086211dd4`/`2e6e77446e`/`375c21ee34` (SVY-20449), `34ffabf11f` (SVY-21648) are later changes in the same folder and are unrelated to borders.
- The tabbed panel has had no border handling in this folder's history.
- Not verified: the attachments and the master branch state. I did not run a reproduction.
