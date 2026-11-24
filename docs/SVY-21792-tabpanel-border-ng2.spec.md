# Spec: SVY-21792 — Legacy TabPanel Matte border shows in NG1 but not in NG2 (TiNG)

## 1. Goal
Make the `borderType` property of the legacy `servoydefault` tabpanel render in TiNG the way it does in NG1. The border must enclose the whole component (tab strip and content) for both the tabbed (`ServoyDefaultTabpanel`) and the tabless (`ServoyDefaultTablesspanel`) panel. It must work for multi-value borders such as Matte, not only for single-width Line borders. The fix uses the same mechanism as the other servoydefault components (`PropertyUtils.setBorder`).

## 2. Background

### 2.1 Current TiNG behaviour (branch `lts_2024`)
Folder: `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/tabpanel/`.

- `BaseTabpanel` (`basetabpanel.ts`) extends `ServoyBaseComponent<HTMLDivElement>`, not `ServoyDefaultBaseComponent`. It declares `@Input() borderType` (line 13) but never applies it. `ServoyDefaultBaseComponent.svyOnChanges` (`basecomponent.ts` lines 72-74) is the only place that calls `PropertyUtils.setBorder(this.getNativeChild(), this.renderer, change.currentValue)`.
- `ServoyDefaultTabpanel`: `tabpanel.html` has the outer `<div class="svy-tabpanel" #element style="height:100%;width:100%;position:absolute;">`. It contains the `<ul ngbNav>` tab strip and `<div [ngbNavOutlet] [ngStyle]="getContainerStyle(element)">`. `getContainerStyle` sets `height: calc(100% - <ul.clientHeight>px)` and the overflow. `borderType` is never read, so no border is drawn for any border type.
- `ServoyDefaultTablesspanel`: `tablesspanel.html` has a single `<div #element style="position:absolute;width:100%;height:100%;" [ngStyle]="getContainerStyle()">`. `getContainerStyle()` (`tablesspanel.ts` line 23) sets `containerStyle['border'] = this.borderStyle`.
- `borderStyle` getter (`basetabpanel.ts` lines 72-78) returns `"${borderWidth} ${borderStyle} ${borderColor}"`. This only works for single-value Line borders. The server sends Matte as `borderWidth: "T Rpx Bpx Lpx "` (multi-value, trailing space), `borderStyle: "solid"`, `borderColor: <color>`. The shorthand `border: 1px 2px 1px 2px  solid red` is invalid CSS and is dropped. Bevel, Etched and SpecialMatte are affected the same way.

### 2.2 NG1 reference
`servoy-client/servoy_ngclient/war/servoydefault/tabpanel/tabpanel.js` line 690 applies `svy-border='model.borderType'` on the outer wrapper `<div style='height:100%;width:100%;position:absolute;'>`. The border therefore encloses the tab strip and the content for every template (tabbed, tabless, accordion). The `svy-border` directive applies each key of `borderStyle` as its own CSS property.

### 2.3 `PropertyUtils.setBorder`
`servoy-public/src/lib/utils/property_utils.ts` lines 42-87:
- `null` or non-object value: removes the `border` style and returns.
- `TitledBorder`: creates a `fieldset` with a `legend` and moves all child element nodes of the target into it. This is DOM surgery.
- Otherwise, if `borderStyle` is set: removes `border`, then sets every key as its own CSS property (`borderWidth` becomes `border-width`, and so on). This handles multi-value widths and colors.

### 2.4 Git history
- `0cbe7bcd10` (SVYX-1071, Aug 2025) added the `borderStyle` getter and the tabless-only `border` shorthand, based on single-width line borders.
- The tabbed panel has no border handling in this folder's history.
- `4e0e34245e` (SVY-20512), `e086211dd4`, `2e6e77446e`, `375c21ee34` (SVY-20449) and `34ffabf11f` (SVY-21648) are unrelated to borders. The SVY-21648 specs in `tabpanel.spec.ts` must keep passing.
- Not verified: the Jira attachments (`NG1.png`, `NG2.png`, `tab_border_ting.rar`) were not downloaded. SVY-21793 is a closed duplicate.

## 3. Design

### 3.1 Constraints (approved, authoritative)
- The `borderStyle` getter in `basetabpanel.ts` stays. It is a legacy property.
- The servoydefault `tabpanel.spec` in servoy-client is not changed.
- Follow NG1: apply the border once, on the outer container that fills the component.

### 3.2 Which element gets the border
The `#element` div in each template, which is also what `getNativeElement()` returns:
- Tabbed: the outer `div.svy-tabpanel` (`height:100%;width:100%;position:absolute`). It is the NG1 wrapper equivalent and contains both the `ul` and the content outlet.
- Tabless: the single `#element` div. It is the outer container, and there is no tab strip.

`getNativeChild()` defaults to `elementRef.nativeElement`, so `getNativeElement()` and `getNativeChild()` are the same here. Use `getNativeElement()`.

### 3.3 Where and when to apply it
Add a `borderType` case in `BaseTabpanel.svyOnChanges`, before `super.svyOnChanges(changes)`:

```ts
if (changes['borderType']) {
    this.applyBorder(changes['borderType'].currentValue);
}
```

- `svyOnChanges` is the hook that `ServoyDefaultBaseComponent` uses. The base class only calls it once `#element` exists. Changes that arrive before the view is ready are buffered and replayed from `ngAfterViewInit`. So the first value is applied at initial render, and later server updates are applied through the same path.
- One implementation in `BaseTabpanel` covers both panels. Neither subclass overrides `svyOnChanges`, so no subclass change is needed for the hook.
- Add a protected `applyBorder(borderType)` in `BaseTabpanel`:
  - If `borderType?.type === 'TitledBorder'`, call `PropertyUtils.setBorder(el, this.renderer, null)`. This clears any previous border and moves no DOM (see 3.6).
  - Otherwise call `PropertyUtils.setBorder(this.getNativeElement(), this.renderer, borderType)`.
- Import `PropertyUtils` from `@servoy/public` (the file already imports from it).

### 3.4 OnPush
Both panels are `ChangeDetectionStrategy.OnPush`. `setBorder` mutates the DOM directly through `Renderer2`, so no change detection or `markForCheck` is needed. The base class `ngAfterViewInit` already calls `detectChanges()`.

### 3.5 Tabless panel: the `border` shorthand conflict
In the tabless panel the `[ngStyle]="getContainerStyle()"` binding and `setBorder` target the same element.

- `ngStyle` writes `border` on every change, and the value is `''` when there is no border. An empty value removes the property. That can wipe or fight the longhand properties (`border-width` and others) written by `setBorder`.
- For Line borders it would draw the same border twice, written two ways.

Decision: remove `containerStyle['border'] = this.borderStyle;` from `ServoyDefaultTablesspanel.getContainerStyle()` so the shorthand is no longer used for rendering. Keep the getter in `BaseTabpanel`, unchanged and with its existing tests. `setBorder` is then the only writer of border CSS on that element.

Reasons:
- It is the only way to avoid two writers on one element.
- `setBorder` is a superset of what the shorthand did for Line borders.
- It matches NG1, where only `svy-border` writes the border.

Do not remove `containerStyle['border']` initial values elsewhere. `containerStyle` only contains `overflow` (tabless) and `position`, `overflow` and `height` (tabbed).

### 3.6 TitledBorder: out of scope
Decision: out of scope, and it must not change the DOM.
- It is not rendered today, and the ticket is about Matte.
- `setBorder` for `TitledBorder` moves child element nodes into a new `fieldset`. In the tabbed panel that would move Angular-managed nodes (`ul`, the `ngbNavOutlet` container). In the tabless panel it would move the `ngTemplateOutlet` content. This is a high risk for ngbNav, `*ngFor` and OnPush bindings, and for the `sabloTabseq` handling.
- So `TitledBorder` is mapped to "no border" (3.3). Treat it as a follow-up if requested.

### 3.7 Interaction with `calc(100% - tabs height)`
- The content container's `height: calc(100% - <ul.clientHeight>px)` is resolved against the parent's content box. With `box-sizing: border-box` on the outer div, the border sits inside the 100% size and the inner content shrinks accordingly, so nothing overflows. The Servoy/bootstrap CSS normally applies `box-sizing: border-box` globally. The implementer must verify this in the running client. If it is not the case for the panel, set `box-sizing: border-box` on the outer element.
- `tabs.clientHeight` is measured on the `ul` and is not affected by the outer border.
- Matte widths are applied per side by the longhand properties, so the horizontal and vertical space taken by the border is handled by the browser.

### 3.8 Known limitation (not fixed)
`setBorder` removes only the `border` shorthand when the type changes. Switching from SpecialMatte (per-side keys) to Line could leave stale per-side longhands. This is existing behaviour shared with every servoydefault component. Out of scope.

## 4. Implementation plan
All paths are under `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/tabpanel/`.

1. `basetabpanel.ts`
   - Import `PropertyUtils` from `@servoy/public`.
   - In `svyOnChanges`, add the `borderType` handling (3.3) before `super.svyOnChanges(changes)`.
   - Add `protected applyBorder(borderType: any)` with the TitledBorder guard.
   - Do NOT touch the `borderStyle` getter.
2. `tablesspanel.ts`: delete the line `this.containerStyle['border'] = this.borderStyle;` in `getContainerStyle()` (3.5).
3. `tabpanel.html` and `tablesspanel.html`: no change expected, because the border is applied imperatively to `#element`. Verify in the browser that the `box-sizing` assumption holds (3.7). Only add `box-sizing: border-box` to the outer div if it does not.
4. `tabpanel.spec.ts` and `tablesspanel.spec.ts`: add unit tests (section 5). The project uses Jasmine and Karma (`describe/it/spyOn`, `TestBed`, `ServoyPublicTestingModule`). Reuse the existing `createComponentWithTabs` helpers. Call `fixture.detectChanges()` first so `#element` exists, then `ngOnChanges` with a `borderType` `SimpleChange`, and assert on `element.style`.
5. Keep the existing `borderStyle getter` coverage as is. This is a legacy property that remains.
6. Not changed: the servoy-client tabpanel spec and `BorderPropertyType.java`.

## 5. Acceptance criteria
Unit tests, in both `tabpanel.spec.ts` (element `.svy-tabpanel`) and `tablesspanel.spec.ts` (element `.svy-tablesspanel`):
- [ ] Matte: `borderType = { type: 'MatteBorder', borderStyle: { borderWidth: '1px 2px 3px 4px ', borderStyle: 'solid', borderColor: 'red' } }`. Expect `border-top-width` 1px, `border-right-width` 2px, `border-bottom-width` 3px, `border-left-width` 4px, border style `solid` and color `red` on the element.
- [ ] Line: `borderStyle: { borderWidth: '2px', borderStyle: 'solid', borderColor: 'blue' }`. Expect 2px solid blue on all sides.
- [ ] `null` borderType (also `undefined`): no border on the element, and a previously applied border is removed.
- [ ] Changing from Line to `null` removes the border. Changing from Matte to Line updates the widths.
- [ ] TitledBorder: no `fieldset` is created and the child nodes of the element are unchanged.
- [ ] The border is set when `borderType` arrives in `ngOnChanges` before the view exists (applied from `ngAfterViewInit`), which is the initial-render case.
- [ ] Tabless: `getContainerStyle()` no longer contains a `border` key. Overflow handling is unchanged.
- [ ] Tabbed: the border is on the outer `.svy-tabpanel`, not on the `ul` or the content outlet. Existing SVY-21648, overflow and tab selection tests still pass.
- [ ] The existing `borderStyle getter` tests (if present on this branch) still pass unchanged.

Manual checks:
- [ ] A tabbed tabpanel with Matte border shows the border around the tab strip and the content, like NG1.
- [ ] A tabless panel with Matte and Line borders shows exactly one border, and the inner form is not clipped or scrolled because of it.
- [ ] No double border or missing border after toggling `borderType` at runtime from a script.

## 6. Out of scope
- `TitledBorder` rendering (3.6).
- Removing or changing the `borderStyle` getter, and any change to the servoy-client tabpanel spec or the server-side border serialization.
- Stale per-side longhands when switching border type (3.8).
- Accordion pane and other templates not present in this folder.
- Making `BaseTabpanel` extend `ServoyDefaultBaseComponent`.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Master (checked in `C:\Users\vosti\git_master\servoy-eclipse`): `basetabpanel.ts` uses signal inputs (`borderType = input<any>()`, `this.borderType()`), keeps the `borderStyle` getter, and has a `basetabpanel.spec.ts` that tests it. A grep of `tabpanel.ts`, `tablesspanel.ts` and both html files found no `borderStyle`, `borderType` or `setBorder` use. Whether master needs the same fix, adapted to signal inputs, and whether the ticket's other fix versions (2026.12, 2025.3.8 LTS, 2026.3.3 LTS) need forward ports. | PM / dev | open |
| Does the outer element really have `box-sizing: border-box` in TiNG, so that the border does not cause overflow? (3.7) | implementer | open |
| Is `TitledBorder` support wanted as a follow-up? | PM | open |
| The attachments on the ticket (`tab_border_ting.rar`) were not inspected. Does the sample use a tabbed or a tabless panel? | implementer | open |
