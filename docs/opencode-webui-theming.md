# opencode Web UI — Theming Reference

This document captures what we know about the opencode web UI's theming system
so future branding work has a clear starting point.

---

## Architecture: two separate systems

opencode has **two independent theming systems** — they do not share variables:

| Layer | Used by | Mechanism | Where to configure |
|-------|---------|-----------|-------------------|
| **TUI** (terminal renderer) | `opencode serve` CLI | JSON theme file with keys `primary`, `background`, `text`, etc. | `~/.config/opencode/themes/<name>.json` |
| **Web UI** (browser iframe) | The browser view we embed in Eclipse | CSS custom properties on `:root` | CSS injection via `IBrowser.execute()` |

**We only use the Web UI.** The TUI theming system (`"theme"` in `opencode.json`,
theme JSON files) has no effect on what we display in the Servoy AI view.

---

## How we inject branding

`OpenCodeView` registers a `LocationAdapter` on the embedded browser. After every
page load (`changed()` event) it runs a JavaScript IIFE that appends a
`<style id="servoy-brand">` element to `document.head`. A guard prevents duplicate
injection on SPA navigation.

See: `com.servoy.eclipse.opencode/src/com/servoy/eclipse/opencode/OpenCodeView.java`
— constants `BRAND_CSS`, `INJECT_CSS_JS`, and helper `toJsString()`.

---

## Current Servoy brand overrides

Inspected from a running opencode instance via:

```js
[...document.styleSheets]
  .flatMap(s => { try { return [...s.cssRules] } catch { return [] } })
  .filter(r => r.selectorText === ':root')
  .flatMap(r => [...r.style])
  .filter(p => p.startsWith('--'))
```

We override only the **brand / interactive / button** colour family.
Backgrounds, text, and semantic (success/warning/critical/diff) colours are
left at the opencode defaults.

```css
:root {
  /* Servoy primary blue applied to brand + interactive surfaces */
  --surface-brand-base: #0073b7;
  --surface-brand-hover: #005a8e;
  --surface-interactive-base: rgba(0, 115, 183, 0.15);
  --surface-interactive-hover: rgba(0, 115, 183, 0.25);
  --surface-interactive-weak: rgba(0, 115, 183, 0.08);
  --surface-interactive-weak-hover: rgba(0, 115, 183, 0.15);
  /* Primary button */
  --button-primary-base: #0073b7;
  /* Interactive text (links, active items) — lightened for dark bg readability */
  --text-interactive-base: #3fa8e0;
  /* Interactive borders */
  --border-interactive-base: #0073b7;
  --border-interactive-hover: #005a8e;
  --border-interactive-active: #004970;
  /* Brand + interactive icons */
  --icon-brand-base: #0073b7;
  --icon-interactive-base: #3fa8e0;
}
```

**Servoy brand palette used:**

| Token | Hex | Purpose |
|-------|-----|---------|
| Primary blue | `#0073b7` | Main brand colour — surfaces, buttons, borders |
| Hover blue | `#005a8e` | Hover state — slightly darker |
| Active blue | `#004970` | Active/pressed state |
| Light blue | `#3fa8e0` | Text and icon tints on dark background |

---

## Complete list of CSS custom properties (opencode v1.15.x)

Obtained by running the inspection snippet above on a live instance.
Re-run after upgrading the `opencode-ai` npm package to check for changes.

### Background & surface

```
--background-base          --background-weak           --background-strong
--background-stronger      --surface-base              --base
--surface-base-hover       --surface-base-active       --surface-base-interactive-active
--base2                    --base3
--surface-inset-base       --surface-inset-base-hover  --surface-inset-strong
--surface-inset-strong-hover
--surface-raised-base      --surface-raised-base-hover --surface-raised-base-active
--surface-raised-strong    --surface-raised-strong-hover --surface-raised-stronger
--surface-raised-stronger-hover                         --surface-raised-stronger-non-alpha
--surface-float-base       --surface-float-base-hover
--surface-weak             --surface-weaker            --surface-strong
```

### Brand & interactive surfaces ← Servoy overrides here

```
--surface-brand-base       --surface-brand-hover
--surface-interactive-base --surface-interactive-hover
--surface-interactive-weak --surface-interactive-weak-hover
```

### Semantic surfaces

```
--surface-success-base  --surface-success-weak  --surface-success-strong
--surface-warning-base  --surface-warning-weak  --surface-warning-strong
--surface-critical-base --surface-critical-weak --surface-critical-strong
--surface-info-base     --surface-info-weak     --surface-info-strong
```

### Diff surfaces

```
--surface-diff-unchanged-base
--surface-diff-skip-base
--surface-diff-hidden-base  --surface-diff-hidden-weak  --surface-diff-hidden-weaker
--surface-diff-hidden-strong  --surface-diff-hidden-stronger
--surface-diff-add-base  --surface-diff-add-weak  --surface-diff-add-weaker
--surface-diff-add-strong  --surface-diff-add-stronger
--surface-diff-delete-base  --surface-diff-delete-weak  --surface-diff-delete-weaker
--surface-diff-delete-strong  --surface-diff-delete-stronger
```

### Input

```
--input-base  --input-hover  --input-active  --input-selected
--input-focus  --input-disabled
```

### Text

```
--text-base  --text-weak  --text-weaker  --text-strong  --text-stronger
--text-invert-base  --text-invert-weak  --text-invert-weaker  --text-invert-strong
--text-interactive-base          ← Servoy override
--text-on-brand-base  --text-on-brand-weak  --text-on-brand-weaker  --text-on-brand-strong
--text-on-interactive-base  --text-on-interactive-weak
--text-on-success-base  --text-on-success-weak  --text-on-success-strong
--text-on-critical-base  --text-on-critical-weak  --text-on-critical-strong
--text-on-warning-base  --text-on-warning-weak  --text-on-warning-strong
--text-on-info-base  --text-on-info-weak  --text-on-info-strong
--text-diff-add-base  --text-diff-add-strong
--text-diff-delete-base  --text-diff-delete-strong
--text-mix-blend-mode
```

### Buttons ← Servoy override

```
--button-primary-base
--button-secondary-base  --button-secondary-hover
--button-ghost-hover  --button-ghost-hover2
```

### Borders

```
--border-base  --border-hover  --border-active  --border-selected
--border-disabled  --border-focus  --border-color
--border-weak-base  --border-weak-hover  --border-weak-active
--border-weak-selected  --border-weak-disabled  --border-weak-focus
--border-weaker-base
--border-strong-base  --border-strong-hover  --border-strong-active
--border-strong-selected  --border-strong-disabled  --border-strong-focus
--border-interactive-base  --border-interactive-hover  --border-interactive-active  ← Servoy overrides
--border-interactive-selected  --border-interactive-disabled  --border-interactive-focus
--border-success-base  --border-success-hover  --border-success-selected
--border-warning-base  --border-warning-hover  --border-warning-selected
--border-critical-base  --border-critical-hover  --border-critical-selected
--border-info-base  --border-info-hover  --border-info-selected
```

### Icons ← Servoy override on brand/interactive

```
--icon-base  --icon-hover  --icon-active  --icon-selected
--icon-disabled  --icon-focus
--icon-invert-base
--icon-weak-base  --icon-weak-hover  --icon-weak-active
--icon-weak-selected  --icon-weak-disabled  --icon-weak-focus
--icon-strong-base  --icon-strong-hover  --icon-strong-active
--icon-strong-selected  --icon-strong-disabled  --icon-strong-focus
--icon-brand-base            ← Servoy override
--icon-interactive-base      ← Servoy override
--icon-success-base  --icon-success-hover  --icon-success-active
--icon-warning-base  --icon-warning-hover  --icon-warning-active
--icon-critical-base  --icon-critical-hover  --icon-critical-active
--icon-info-base  --icon-info-hover  --icon-info-active
--icon-on-brand-base  --icon-on-brand-hover  --icon-on-brand-selected
--icon-on-interactive-base
--icon-on-success-base  --icon-on-success-hover  --icon-on-success-selected
--icon-on-warning-base  --icon-on-warning-hover  --icon-on-warning-selected
--icon-on-critical-base  --icon-on-critical-hover  --icon-on-critical-selected
--icon-on-info-base  --icon-on-info-hover  --icon-on-info-selected
--icon-diff-add-base  --icon-diff-add-hover  --icon-diff-add-active
--icon-diff-delete-base  --icon-diff-delete-hover
--icon-diff-modified-base
--icon-agent-plan-base  --icon-agent-docs-base
--icon-agent-ask-base   --icon-agent-build-base
```

### Syntax highlighting

```
--syntax-comment  --syntax-regexp  --syntax-string  --syntax-keyword
--syntax-primitive  --syntax-operator  --syntax-variable  --syntax-property
--syntax-type  --syntax-constant  --syntax-punctuation  --syntax-object
--syntax-success  --syntax-warning  --syntax-critical  --syntax-info
--syntax-diff-add  --syntax-diff-delete  --syntax-diff-unknown
```

### Markdown

```
--markdown-heading  --markdown-text  --markdown-link  --markdown-link-text
--markdown-code  --markdown-block-quote  --markdown-emph  --markdown-strong
--markdown-horizontal-rule  --markdown-list-item  --markdown-list-enumeration
--markdown-image  --markdown-image-text  --markdown-code-block
```

### Avatar

```
--avatar-background-pink   --avatar-text-pink
--avatar-background-mint   --avatar-text-mint
--avatar-background-orange --avatar-text-orange
--avatar-background-purple --avatar-text-purple
--avatar-background-cyan   --avatar-text-cyan
--avatar-background-lime   --avatar-text-lime
```

---

## Notes for future tweaks

- Re-run the inspection snippet after each `opencode-ai` npm upgrade — new variables
  may appear or existing ones may be renamed.
- The `--surface-interactive-*` variables use `rgba()` so they layer transparently
  over different backgrounds. Use alpha values rather than opaque hex colours for
  these tokens.
- `--text-interactive-base` needs to be a **lighter blue** (`#3fa8e0`) rather than
  the full `#0073b7` because it renders on the dark default background
  (`--background-base`) and needs sufficient contrast.
- Syntax, diff, and semantic (success/warning/critical) colours should generally
  be left at their defaults to preserve readability of code and diffs.
