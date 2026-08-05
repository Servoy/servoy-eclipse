# Spec: SVY-21149 — Error when searching in the workspace

## 1. Goal

Eliminate the error dialog that appears when using the Ctrl+H text search in Servoy Developer with the dark theme enabled. The error is caused by missing icon files referenced by the `ImageReplacementMapper` and by unhandled SVG icons from third-party plugins (`org.eclipse.lsp4e`, `org.eclipse.m2e.core.ui`) that SWT cannot render.

## 2. Background

### 2.1 Image replacement system

The `com.servoy.eclipse.ui.tweaks` plugin contains an `ImageReplacementMapper` that intercepts icon loading from Eclipse platform plugins and substitutes Servoy-branded icons. It uses a `{0}` placeholder in URLs that resolves to either `icons` (light theme) or `darkicons` (dark theme) via `getIconsPath()`.

### 2.2 The mapping that triggers the error

In `ImageReplacementMapper.java` (lines 486–487):

```java
urlReplacements.put(URI.create("platform:/plugin/org.eclipse.search/icons/full/dlcl16/expandall.png"),
    formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/expandall-disabled.png"));
```

This maps the disabled "Expand All" toolbar button in the Search view to `com.servoy.eclipse.ui.tweaks/darkicons/expandall-disabled.png` when dark theme is active. However, that file does not exist — only `icons/expandall-disabled.png` (light theme variant) is present.

### 2.3 SVG rendering failures from third-party plugins

The log also shows `SWTException: Unsupported or unrecognized format` errors from SVG icons in:
- `org.eclipse.lsp4e/icons/full/elcl16/link_to_editor.svg`
- `org.eclipse.lsp4e/icons/full/elcl16/alphab_sort_co.svg`
- `org.eclipse.lsp4e/icons/full/elcl16/fields_co.svg`
- `org.eclipse.m2e.core.ui/icons/update_dependencies.svg`

These occur when the Content Outline view toolbar renders its buttons during the search flow. No image replacement mappings exist for these SVG resources, and SWT's `FileFormat.load()` cannot handle them.

### 2.4 Cascading DecorationOverlayIcon failure

After the missing icon causes the initial error, a secondary `DeviceResourceException: Unable to create resource org.eclipse.jface.viewers.DecorationOverlayIcon` failure occurs when the Search view tries to decorate search result items. This is a direct consequence of the first failure.

### 2.5 Existing assets

| File | `icons/` (light) | `darkicons/` (dark) |
|------|:-:|:-:|
| `expandall.png` | ✓ | ✓ |
| `expandall@2x.png` | ✓ | ✓ |
| `expandall-disabled.png` | ✓ | **✗ MISSING** |
| `expandall-disabled@2x.png` | ✓ | **✗ MISSING** |
| `collapseall-disabled.png` | ✓ | ✓ |
| `collapseall-disabled@2x.png` | ✓ | ✓ |

## 3. Design

### 3.1 Add missing dark theme icon files

Create `expandall-disabled.png` and `expandall-disabled@2x.png` in the `darkicons/` directory of `com.servoy.eclipse.ui.tweaks`. These should follow the same visual style as the existing `darkicons/collapseall-disabled.png` — a desaturated/dimmed version of the `darkicons/expandall.png` icon appropriate for the disabled state.

### 3.2 Add SVG replacement mappings for lsp4e icons (optional hardening)

Add entries to `ImageReplacementMapper` to intercept the SVG icons from `org.eclipse.lsp4e` that cause `SWTException` during search. The `ImageReplacementMapper` already has a fallback mechanism (`getReplacementFromFile` at line 803) that checks for `.svg` → `.png` replacements, but this only applies to class/filename-based lookups, not URL-based ones. Adding explicit URL mappings for these lsp4e SVGs would prevent the errors.

Alternatively, if appropriate PNG replacements are not available, these errors could be left as-is since they originate from third-party plugins and do not block the search functionality itself (they produce non-fatal warning dialogs).

## 4. Implementation plan

1. Create `com.servoy.eclipse.ui.tweaks/darkicons/expandall-disabled.png` — generate a dimmed/disabled variant of `darkicons/expandall.png` matching the style of `darkicons/collapseall-disabled.png`.
2. Create `com.servoy.eclipse.ui.tweaks/darkicons/expandall-disabled@2x.png` — HiDPI variant.
3. Verify the `build.properties` already includes `darkicons/` in `bin.includes` (confirmed — it does).
4. (Optional) Add image replacement mappings in `ImageReplacementMapper.java` for `org.eclipse.lsp4e` SVG icons that fail to render, providing PNG fallbacks or suppress the error.
5. Test by opening Ctrl+H search dialog and performing a text search with dark theme active — verify no error dialog appears.

## 5. Acceptance criteria

- [ ] `darkicons/expandall-disabled.png` exists and is a valid 16×16 PNG
- [ ] `darkicons/expandall-disabled@2x.png` exists and is a valid 32×32 PNG
- [ ] Performing a Ctrl+H workspace text search with dark theme active does not produce an error dialog
- [ ] The "Expand All" button in the Search results view renders correctly in its disabled state
- [ ] No `FileNotFoundException` for `expandall-disabled.png` in the workspace log
- [ ] The `DecorationOverlayIcon` error no longer occurs during search result display

## 6. Out of scope

- Fixing SVG rendering in SWT itself (upstream Eclipse issue)
- Replacing all SVG icons from third-party plugins with PNG equivalents
- Changes to light-theme icons (those are already present and working)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should we add explicit PNG replacements for the lsp4e/m2e SVG icons to prevent the secondary SWT errors, or leave them as non-blocking third-party issues? | Dev team | open |
| Are the lsp4e SVG errors a separate issue that should be tracked independently? | PM | open |
