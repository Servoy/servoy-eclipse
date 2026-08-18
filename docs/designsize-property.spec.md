# SVY-XXXXX: Add `designsize` property to component specs

## Problem

After removing the deprecated `size` property from component specs (which was never actually used as a component input), the form designer lost the ability to determine the correct initial size for components during:

1. **Drag preview** — the element shown while dragging from the palette uses a fallback of 200x100 instead of the appropriate component size (e.g., 80x20 for a button)
2. **Initial placement** — after dropping, the component is created with 200x100 dimensions instead of its natural default size

## Root Cause

The palette data is built in `DesignerFilter.doFilter()` which reads:

```java
PropertyDescription pd = spec.getProperty("size");
if (pd != null && pd.getDefaultValue() != null) {
    model.put("size", pd.getDefaultValue());
}
```

With `size` removed from specs, `spec.getProperty("size")` returns null → no `model.size` sent → client falls back to 200x100.

The client code in `designform_component.component.ts`:

```typescript
const elWidth = event.data.model.size ? event.data.model.size.width : 200;
const elHeight = event.data.model.size ? event.data.model.size.height : 100;
```

## Solution

Introduce a new `designsize` property in spec files — a server-only dimension property that specifies the component's default size in the form designer. This property is never sent to the runtime client, only used by the designer for palette drag/drop sizing.

### 1. Spec file change (all component specs)

Add to each component spec's `"properties"` section:

```json
"designsize": {
  "type": "dimension",
  "tags": {"serveronly": true},
  "default": {"width": 80, "height": 20}
}
```

Default values per component type (examples):
- Button: `{"width": 80, "height": 30}`
- Textbox/Field: `{"width": 140, "height": 30}`
- Label: `{"width": 80, "height": 20}`
- Textarea: `{"width": 140, "height": 80}`
- Checkbox: `{"width": 140, "height": 30}`
- Calendar: `{"width": 140, "height": 30}`
- DataGrid/Table: `{"width": 400, "height": 300}`
- Tabpanel: `{"width": 400, "height": 300}`
- Container/Layout: `{"width": 300, "height": 200}`

### 2. Server-side change (DesignerFilter.java)

```java
// Read designsize first, fall back to legacy size property
PropertyDescription pd = spec.getProperty("designsize");
if (pd == null) pd = spec.getProperty("size");
if (pd != null && pd.getDefaultValue() != null) {
    model.put("size", pd.getDefaultValue());
}
```

Location: `com.servoy.eclipse.designer.rfb/src/com/servoy/eclipse/designer/rfb/startup/DesignerFilter.java`

### 3. Client-side change

None required — the client already reads `model.size` and falls back to 200x100.

### 4. DefaultComponentPropertiesProvider

No change needed — `designsize` is declared per-spec, not as a universal default property.

## Affected Repositories

| Repository | Location | Components |
|-----------|----------|------------|
| `servoy-eclipse` | `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/` | Default components (button, textbox, label, etc.) |
| `bootstrapcomponents` | `components/*/` | Bootstrap UI components |
| `bootstrapextracomponents` | `components/*/` | Bootstrap extra components |
| `servoy-extra-components` | `components/*/` | Extra components (multi-file upload, etc.) |
| `aggridcomponents` | `aggrid/*/` | AG Grid components |
| `svykanban` | `*/` | Kanban board |
| `custom-rendered-components` | `*/` | Custom rendered components |

## Finding affected specs

Any `.spec` file that previously had a `size` property (now removed) needs `designsize` added. To find them:

```bash
# Find specs WITHOUT designsize (candidates for adding it)
find . -name "*.spec" -exec grep -L "designsize" {} \;

# Check git history for specs that had size removed
git log --all --oneline -- "*.spec" | grep -i "size\|deprecat\|migrat"
```

## Migration Skill Update

The `servoy-component-migration` skill must be updated:
- When migrating a component and removing the `size` property from a spec, automatically add `designsize` with the old default value
- Never remove `size` without converting to `designsize`

## Testing

1. Drag a button from palette → should show ~80x30 preview, not 200x100
2. Drop the button → should be created at ~80x30, not 200x100
3. Drag a datagrid → should show ~400x300 preview
4. Verify existing specs that still have `size` (legacy) continue to work via the fallback
