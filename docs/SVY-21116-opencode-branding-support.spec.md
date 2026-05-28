# Spec: SVY-21116 — Opencode branding support

## 1. Goal

Apply lightweight Servoy branding to the opencode web UI displayed inside the
Servoy AI view — without forking or replacing the opencode npm package.
Two complementary mechanisms are used:

1. **opencode config `"theme"`** — select the closest built-in colour palette via
   `opencode.json`.
2. **CSS injection** — after each page load, execute a small JavaScript snippet via
   `IBrowser.execute()` to inject a `<style>` block that overrides opencode's CSS
   variables with Servoy brand colours.

Together these give a branded look-and-feel (brand colours, fonts, minor
cosmetic tweaks) while keeping the full opencode UI and all its functionality
intact.

---

## 2. Background

### 2.1 What opencode config exposes for branding

opencode has **two independent theming systems** that do not share variables:

- **TUI (terminal renderer)** — JSON theme files with keys `primary`,
  `background`, `text` etc. Selected via `"theme"` in `opencode.json` or
  `tui.json`. **We do not use the TUI.** This system has no effect on the
  browser view we embed.
- **Web UI (browser iframe)** — CSS custom properties on `:root` with names
  like `--surface-brand-base`, `--button-primary-base`, `--text-interactive-base`.
  This is what we target.

The full list of Web UI CSS variables and the rationale for which ones to
override is documented in **`docs/opencode-webui-theming.md`** (living reference,
update after each `opencode-ai` npm upgrade).

There is no `"css"`, `"logo"`, or `"favicon"` key in `opencode.json`. Logo and
favicon customisation would require replacing the opencode web frontend entirely,
which is out of scope.

### 2.2 CSS injection via `IBrowser.execute()`

`OpenCodeView` holds an `IBrowser` instance. `IBrowser` exposes `execute(String)`,
which evaluates arbitrary JavaScript in the current page. After the opencode page
finishes loading, we can run:

```js
(function(){
  var s = document.createElement('style');
  s.id = 'servoy-brand';
  if (document.getElementById('servoy-brand')) return;
  s.textContent = '/* Servoy brand overrides */ :root { --color-primary: #0073b7; ... }';
  document.head.appendChild(s);
})();
```

This is re-injected on every navigation (via a `LocationListener`) so the
branding survives page reloads and URL changes within the opencode SPA.

### 2.3 When to inject

`OpenCodeView` already registers a `LocationListener` pattern (the URL switcher
thread) but does not currently hook `CHANGED` events. We need to add a
`LocationListener` that fires `browser.execute(INJECT_CSS_JS)` on
`locationChanged` (page finished loading).

---

## 3. Design

### 3.1 CSS variable targets

Only Web UI CSS variables are targeted. The TUI `"theme"` config key has no
effect on our embedded browser and must **not** be set.

The Servoy brand overrides applied are documented in `docs/opencode-webui-theming.md`.
In summary: `--surface-brand-*`, `--surface-interactive-*`,
`--button-primary-base`, `--text-interactive-base`, `--border-interactive-*`,
`--icon-brand-base`, `--icon-interactive-base` are overridden with Servoy blue
(`#0073b7`). All backgrounds, text, and semantic colours are left at defaults.

### 3.2 CSS injection in `OpenCodeView`

**New constant** in `OpenCodeView`:

```java
/** CSS injected into the opencode web app on every page load to apply Servoy branding. */
private static final String BRAND_CSS = """
    :root {
      /* Servoy primary blue */
      --color-primary: #0073b7;
      --color-primary-hover: #005a8e;
      /* Background / surface colours */
      --color-background: #1e1e2e;
      --color-surface: #27273a;
      /* Accent */
      --color-accent: #0073b7;
    }
    """;

private static final String INJECT_CSS_JS =
    "(function(){" +
    "  if (document.getElementById('servoy-brand')) return;" +
    "  var s = document.createElement('style');" +
    "  s.id = 'servoy-brand';" +
    "  s.textContent = " + toJsString(BRAND_CSS) + ";" +
    "  document.head.appendChild(s);" +
    "})();";
```

**`toJsString(String)`** — private static helper that wraps the CSS in a JS
string literal (escaping backslashes, backticks, and `$`).

**LocationListener** — registered once in `createPartControl()`:

```java
browser.addLocationListener(new LocationAdapter() {
    @Override
    public void changed(LocationEvent event) {
        browser.execute(INJECT_CSS_JS);
    }
});
```

`LocationAdapter` is `org.eclipse.swt.browser.LocationAdapter`.
`changed()` fires after the page finishes loading (equivalent to
`document.readyState === "complete"`), so the DOM is available when we inject.

The `if (document.getElementById('servoy-brand')) return;` guard prevents
duplicate style tags on partial navigation events within the SPA.

### 3.3 CSS variable scope

opencode's web UI uses CSS custom properties on `:root`. The exact variable names
must be determined by inspecting the opencode web app DOM in a browser dev-tools
session. The spec records the Servoy brand values; the variable names are open
(see §7).

---

## 4. Implementation plan

1. **`ProviderConfigWriter.java`** (`com.servoy.eclipse.opencode`):
   Add `"theme": "opencode"` to `PROVIDER_CONFIG_JSON`. No structural code
   changes needed.

2. **`OpenCodeView.java`** (`com.servoy.eclipse.opencode`):
   - Add `BRAND_CSS` and `INJECT_CSS_JS` constants.
   - Add private static `toJsString(String)` helper.
   - In `createPartControl()`, after creating the browser, register a
     `LocationListener` whose `changed()` method calls
     `browser.execute(INJECT_CSS_JS)`.

3. **Compilation check**: `eclipse-ide_getCompilationErrors` on
   `com.servoy.eclipse.opencode` — zero errors required.

---

## 5. Acceptance criteria

- [ ] `opencode.json` written to `{user.home}/.servoy/opencode/` contains
      `"theme": "opencode"` (or the agreed palette name).
- [ ] After the opencode page loads in the Servoy AI view, a `<style
      id="servoy-brand">` element is present in the DOM.
- [ ] Reloading or navigating within the opencode SPA re-injects the style without
      creating duplicate `<style>` elements.
- [ ] No compilation errors in `com.servoy.eclipse.opencode`.

---

## 6. Out of scope

- Replacing or removing the opencode logo / favicon (requires forking the npm
  package).
- A full custom UI on top of opencode.
- Eclipse perspective / view icon changes (separate task).
- Changes to `opencode-loading.html` / `opencode-no-solution.html`.

---

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| The CSS variable names were obtained by DOM inspection of opencode v1.15.x. They may change on npm upgrade — re-run the inspection snippet in `docs/opencode-webui-theming.md` after each upgrade. | Developer | open |
| The mockup images attached to SVY-21116 (`image-20260528-134930.png`, `image-20260528-135119.png`) were not accessible during spec authoring. Design should confirm whether the current Servoy blue overrides match the intended look, or whether additional variables need to be adjusted. | Design | open |
