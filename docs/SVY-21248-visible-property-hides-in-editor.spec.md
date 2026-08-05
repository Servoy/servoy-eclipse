# Spec: SVY-21248 — Visible property hides the component in form editor

## 1. Goal

When a developer sets the `visible` property to `false` on a component in the responsive form editor (or CSS-position layout), the component must remain visible at design time — shown with reduced opacity and a dashed border — so the developer can still select and edit it. Currently, certain components (slider, DBTreeView, LightBox gallery) disappear entirely from the form editor, making them unselectable.

## 2. Background

### 2.1 How absolute forms handle visibility (correct behavior)

In absolute layout forms, the designer wraps each component in a div that uses the `svyVisible` model property (a design-time flag) to apply the CSS class `invisible_element`:

```html
<!-- designform_component.component.ts line 35 -->
<div class="svy-wrapper" [ngClass]="{'invisible_element' : item.model.svyVisible === false}" ...>
```

The `invisible_element` CSS class (defined in `designform.css`) renders the component semi-transparently:

```css
.invisible_element {
    opacity: 0.3;
    border: 2px dashed;
}
```

This keeps the component visible and selectable in the designer while indicating it will be hidden at runtime.

### 2.2 How CSS-position containers handle visibility (bug)

In CSS-position containers within the designer template, the wrapper div uses the runtime pattern:

```html
<!-- designform_component.component.ts line 77 -->
<div class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" ...>
```

This completely hides the component with `display: none`, making it unselectable.

### 2.3 How responsive forms handle visibility (bug)

For responsive layout forms, items are rendered via `getTemplate(item)` without a visibility-handling wrapper. The server sends the `visible` property value to the designer's component model. In `editorcontent.service.ts`:

- Line 202: The model is initialized with `{visible:true}`
- Lines 208–213: Server properties are converted and written into the model, overwriting the initial `visible:true` with the actual persist value (e.g., `false`)

When `visible` ends up as `false` in the model, some components that honor the `visible` property in their own templates (e.g., via `ngIf` or host element styling) disappear entirely from the editor.

### 2.4 Why some components are affected and others are not

The root cause is the `visible` property type in the component spec files. Components that declared `"visible": "boolean"` (plain boolean) instead of `"visible": "visible"` (the framework's visibility type) were affected:

- When a component uses the proper `"visible"` type, the framework handles visibility via the generated `*ngIf` / `@if` wrapper in the runtime template. In the designer, this wrapper is stripped so the component always renders regardless of the visible value.
- When a component used `"visible": "boolean"`, the property was passed as a plain `[visible]="state.model.visible"` input binding to the component. The component itself would then honor this value — typically hiding itself with `display:none` or `*ngIf` in its own template. This caused the component to disappear in the designer.

**Fix:** The three affected components (slider, DBTreeView, LightBox gallery) had their spec files updated to use the `"visible"` type instead of `"boolean"`. All other components across all packages already used the correct type.

## 3. Design

### 3.1 Add visibility indicator for responsive layout components

For responsive forms, there is no wrapper div that can carry the `invisible_element` class without breaking flex/grid layout. A plain wrapper `<div>` disrupts Bootstrap grid classes (`col-*`) because it becomes the direct child of the flex container instead of the component.

**Solution:** Wrap responsive items in a `<div class="svy-responsive-wrapper">` that uses `display: contents`. This CSS property makes the wrapper produce no layout box — its child (the component) behaves as a direct child of the responsive container. The `invisible_element` class is applied to this wrapper when `svyVisible === false`, and a CSS descendant selector targets the rendered component element to apply the visual indicator.

```html
<div class="svy-responsive-wrapper" [ngClass]="{'invisible_element': item.model?.svyVisible === false}">
  <ng-template [ngTemplateOutlet]="getTemplate(item)" ...></ng-template>
</div>
```

```css
.svy-responsive-wrapper {
    display: contents;
}
.svy-responsive-wrapper.invisible_element > * {
    opacity: 0.3;
    outline: 2px dotted;
    border: 2px dashed;
}
```

## 4. Implementation plan

1. **`designform_component.component.ts`** — responsive div template (`#svyResponsiveDiv`, line 67–73):
   - Wrap each item in a `<div class="svy-responsive-wrapper">` with `[ngClass]="{'invisible_element': item.model?.svyVisible === false}"`.
   - The wrapper uses `display: contents` so it doesn't affect flex/grid layout.

2. **`designform.css`** — add CSS rules:
   ```css
   .svy-responsive-wrapper {
       display: contents;
   }
   .svy-responsive-wrapper.invisible_element > * {
       opacity: 0.3;
       outline: 2px dotted;
       border: 2px dashed;
   }
   ```

3. **`editorcontent.service.ts`** — no changes needed. The `svyVisible` value is already correctly set in both the component creation path and `updateComponentProperties`.

4. **Verify** that the `formComponentAbsoluteDiv` and `formComponentResponsiveDiv` templates (lines 85–98) already use `svyVisible` correctly — they do.

5. **Run lint and build:** `npm run lint` and `npm run build_debug_nowatch` in `com.servoy.eclipse.ngclient.ui/node`.

6. **Test manually** with slider, DBTreeView, LightBox gallery, label, and button components on responsive forms:
   - Set `visible=false` in properties view → component remains visible with 30% opacity, dotted outline, and dashed border
   - Component is still selectable and editable
   - Button layout is not broken (no overlapping)
   - At runtime, component is correctly hidden

## 5. Acceptance criteria

- [ ] Setting `visible=false` on any component in the responsive form editor does NOT hide the component from the designer canvas.
- [ ] Setting `visible=false` on any component in a CSS-position container does NOT hide the component from the designer canvas.
- [ ] Components with `visible=false` are rendered at 30% opacity with a dashed border (matching the existing `invisible_element` style used by absolute forms).
- [ ] Components with `visible=false` remain selectable and editable in the form editor.
- [ ] At runtime, components with `visible=false` are correctly hidden (no regression).
- [ ] The fix works for all component types, including third-party components (DBTreeView, LightBox gallery, slider).
- [ ] No regression for absolute layout forms (which already work correctly with `svyVisible`).

## 6. Out of scope

- Changing the visual indicator style (opacity, border) — current `invisible_element` CSS is reused as-is.
- Ghost container handling for invisible components — already works for non-renderable components.
- Legacy SWT/GEF form designer (only applies to the RFB/Angular designer).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the `svyVisible` flag also affect the properties view (e.g., show an icon indicator)? | UX/Product | open |
| Are there other properties (e.g., `enabled`) that should also be overridden at design time? | Architect | open |
| Do third-party component packages (DBTreeView, LightBox) rely on `visible` in their spec in ways beyond simple display toggling? | Component devs | open |
