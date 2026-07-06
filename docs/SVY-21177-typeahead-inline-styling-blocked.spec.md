# Spec: SVY-21177 — Inline styling on valuelist items blocked by Angular security

## 1. Goal

Allow developers to use inline styles (e.g. `background-color`, `color`) in valuelist display values rendered in the Bootstrap typeahead component. Currently, Angular's built-in XSS sanitization strips all inline `style` attributes from HTML content rendered via `[innerHTML]`, making it impossible to apply per-item styling in typeahead dropdown options.

## 2. Background

### 2.1 How the typeahead renders valuelist items

The typeahead component (`bootstrapcomponents-typeahead`) has a `showAs` property with values `"html"` (default) and `"text"`:

- **text mode:** Uses ng-bootstrap's standard `ngb-highlight` component — plain text with search term highlighting.
- **html mode:** Uses a custom `svy-ngb-highlight` component that splits the formatted result by the search term and renders each part via `[innerHTML]="part"`.

The `svy-ngb-highlight` component (`highlight.ts`) is an internal fork of ng-bootstrap's highlight component, modified to use `[innerHTML]` instead of `[textContent]` so that HTML markup in valuelist display values is rendered.

### 2.2 Angular innerHTML sanitization

Angular's security model sanitizes all values bound to `[innerHTML]`. Specifically, it strips:
- Inline `style` attributes
- `javascript:` URLs
- Event handlers (`onclick`, etc.)

This is intentional XSS protection. To bypass it for trusted content, Angular provides `DomSanitizer.bypassSecurityTrustHtml()`.

### 2.3 Existing pattern in other components

Other Bootstrap components (combobox, checkbox, choicegroup, datalabel, button, label) already support a `"trusted_html"` value for their `showAs` property. They use the `trustAsHtml` pipe from `@servoy/public` which calls `DomSanitizer.bypassSecurityTrustHtml()` when the `trusted` flag is true. The flag is set when either:
- `showAs === 'trusted_html'`
- `servoyApi.trustAsHtml()` returns true (global application-level setting)

The typeahead and its float-label variant do not yet support `"trusted_html"`.

### 2.4 Use case

A developer creates a valuelist with a calculation that returns HTML like:
```html
<span style="height: 16px; width: 16px; display: inline-block; background-color: #FF0000"></span> Red
```

This is used to show a colored indicator next to each valuelist item in the typeahead dropdown. With the current implementation the `style` attribute is stripped and the colored span is invisible.

## 3. Design

### 3.1 Add `trusted_html` to the typeahead `showAs` property

Update the component spec (`typeahead.spec`) to include `"trusted_html"` in the `showAs` values array, consistent with other Bootstrap components. Update the documentation tag accordingly.

When `showAs` is `"trusted_html"`, the typeahead uses the HTML result template (`rtHTML`) — same as `"html"` — but passes a trust flag to the highlight component.

### 3.2 Modify `SvyNgbHighlight` to support trusted HTML

Add a boolean `trusted` input to the `SvyNgbHighlight` component. When `trusted` is `true`, the component uses `DomSanitizer.bypassSecurityTrustHtml()` to convert each part string into a `SafeHtml` object before binding to `[innerHTML]`.

Implementation approach:
- Inject `DomSanitizer` into `SvyNgbHighlight`
- Add `readonly trusted = input(false)` signal input
- Store parts as `(string | SafeHtml)[]`
- In `ngOnChanges`, after computing the string parts, map them through `bypassSecurityTrustHtml()` if `trusted` is true

### 3.3 Update typeahead templates to pass the trust flag

Both `typeahead.html` and `floatlabeltypeahead.html` have an `rtHTML` template:
```html
<ng-template #rtHTML let-r="result" let-t="term" let-formatter="formatter">
  <svy-ngb-highlight [result]="formatter(r)" [term]="t"></svy-ngb-highlight>
</ng-template>
```

Update to pass `[trusted]="isTrustedHTML()"`.

### 3.4 Add `isTrustedHTML()` to the typeahead component

Add a method to `ServoyBootstrapTypeahead` following the same pattern used in `bts_baselabel.ts` and `combobox.ts`:

```typescript
isTrustedHTML(): boolean {
    return this.servoyApi.trustAsHtml() || this.showAs() === 'trusted_html';
}
```

Since `ServoyFloatLabelBootstrapTypeahead` extends `ServoyBootstrapTypeahead`, it inherits this method automatically.

### 3.5 Security considerations

- The `"html"` mode continues to sanitize content (no behavior change for existing users).
- The `"trusted_html"` mode is opt-in and requires the developer to explicitly set it in the designer or the application-level `trustAsHtml` property to be enabled.
- This matches the security model of all other Bootstrap components in the package.

## 4. Implementation plan

1. **`components/typeahead/typeahead.spec`** — Add `"trusted_html"` to the `showAs.values` array. Update the `doc` tag to mention trusted HTML.

2. **`components/floatlabeltypeahead/floatlabeltypeahead.spec`** — Same change (if it has its own `showAs` definition; otherwise verify it inherits from typeahead).

3. **`components/projects/bootstrapcomponents/src/typeahead/highlight.ts`** — Inject `DomSanitizer`, add `trusted` input, convert parts to `SafeHtml` when trusted.

4. **`components/projects/bootstrapcomponents/src/typeahead/typeahead.ts`** — Add `isTrustedHTML()` method.

5. **`components/projects/bootstrapcomponents/src/typeahead/typeahead.html`** — Pass `[trusted]="isTrustedHTML()"` to `svy-ngb-highlight` in the `rtHTML` template.

6. **`components/projects/bootstrapcomponents/src/floatlabeltypeahead/floatlabeltypeahead.html`** — Same template change.

## 5. Acceptance criteria

- [ ] Setting `showAs` to `"trusted_html"` on a typeahead allows inline styles in valuelist HTML display values to render without being stripped.
- [ ] Setting `showAs` to `"html"` (default) continues to sanitize inline styles (existing behavior preserved).
- [ ] Setting `showAs` to `"text"` continues to render plain text (existing behavior preserved).
- [ ] The global `servoyApi.trustAsHtml()` setting also enables trusted HTML rendering in the typeahead when `showAs` is `"html"` or `"trusted_html"`.
- [ ] Search term highlighting still works correctly in trusted HTML mode.
- [ ] The float-label typeahead variant has the same behavior.
- [ ] The fix works with HTML containing inline styles like `background-color`, `color`, `width`, `height`, `display`.

## 6. Out of scope

- Supporting `<script>` tags or event handlers in valuelist HTML (these remain stripped even with `bypassSecurityTrustHtml` at the Angular level since innerHTML never executes scripts).
- Adding a separate sanitization allowlist (e.g. allowing only `style` but blocking other attributes) — Angular does not provide granular innerHTML sanitization without a custom sanitizer.
- Changes to non-typeahead components (combobox, select, etc. already support `trusted_html`).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `showAs` default be changed from `"html"` to `"trusted_html"` for backward compatibility with users who relied on styles working in older (pre-Angular) versions? | Product | open |
| Should the component spec `doc` tag warn about XSS risks when using `trusted_html` with user-supplied data? | Product | open |
