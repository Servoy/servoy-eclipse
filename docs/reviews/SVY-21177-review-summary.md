# SVY-21177 — Peer-review summary

**Issue:** Inline styling on valuelist items blocked by Angular security
**Risk verdict: LOW.** A contained, strictly opt-in change — a developer-gated `trusted_html`
display mode that bypasses Angular's HTML sanitiser only when the developer explicitly selects
it, mirroring the pattern already shipped for combobox/label/datalabel/checkbox/button. The
default stays `html` (fully sanitised), so no existing solution changes behaviour on upgrade,
and the trust trade-off is the developer's informed per-field choice.

**Scope reviewed:** bootstrapcomponents @ `0208b60` (code + specs); servoy-eclipse @ `c9bd2b93d4`
(SDD spec document only, no runtime coupling).

## Manual test plan

**Verifying the fix**
1. Create a valuelist whose display value is HTML with an inline style, e.g.
   `'<span style="height:16px;width:16px;display:inline-block;background-color:#FF0000"></span> Red'`.
2. Bind it to a Bootstrap typeahead with `showAs = "html"` (default); open the dropdown — the
   swatch should be **missing** (styles stripped), reproducing the bug.
3. Set `showAs = "trusted_html"` (or enable application-level `servoyApi.trustAsHtml()`); reopen —
   the swatch should now **render**.
4. Repeat on the **floatlabel typeahead** variant to confirm the inherited gate works there.

**Regression checks**
- `showAs = "html"` still sanitises (inline style stripped) — unchanged.
- `showAs = "text"` still renders plain text — unchanged.
- Type a term that matches part of a styled item: highlighting still applies, and in
  `trusted_html` mode the styling survives alongside the highlight.
- Empty result / empty term: no error, no `[object Object]`, no `SafeValue must use [property]=...`.

**Automated checks**
- `cd components && npm run test` targeting `highlight.spec.ts` — the Vitest coverage of
  trusted/untrusted behaviour.
- `npx ng lint` (zero warnings expected) and `npm run build`.

**Surfaces to cover:** Bootstrap typeahead and floatlabel typeahead HTML-mode dropdowns, and the
design-time `showAs` dropdown in the form designer (now offers a third value). No other component
is affected — the shared `SvyNgbHighlight` defaults to `trusted=false`.

## Possible improvements / follow-ups

- **Documentation (non-blocking):** consider spelling out in the component docs that
  `trusted_html` on a database/foundset-backed valuelist renders stored data unsanitised, so it
  is reserved for display values the developer controls. The spec `doc` tag already says
  "trusted html (unsanitized, allowing inline styles)" but stops short of an explicit stored-XSS
  note. Responsibility sits with the developer who opts in, so this is awareness only.
- **Default value:** the SDD spec left "keep `html` vs switch to `trusted_html` as default" as an
  open Product question. The code keeps `html` (the safe choice); confirm that is the agreed outcome.
- **Term-split edge case (low likelihood):** the highlighter splits already-HTML content on the
  user term and `bypassSecurityTrustHtml`'s each fragment into its own `<span [innerHTML]>`.
  Reasoned safe (fragments parse independently, term is regex-escaped) but not runtime-proven; a
  quick runtime check with an unbalanced tag + a term that splits mid-tag would settle it.

## Resolved during review

- The Cypress `highlight.cy.ts` added in `0208b60` was later removed by `e6c9b8f` (migrate
  component tests from Cypress to Vitest). Current coverage is the Vitest `highlight.spec.ts`,
  which exercises `trusted=true`/`trusted=false`. No coverage gap, no cleanup needed.
- The floatlabel template binding `[trusted]="isTrustedHTML()"` resolves — `ServoyFloatLabelBootstrapTypeahead extends ServoyBootstrapTypeahead`, so the method is inherited.
