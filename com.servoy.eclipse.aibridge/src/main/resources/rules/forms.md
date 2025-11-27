=== SERVOY FORM OPERATIONS ===

**Goal**: Manage Servoy forms using the available MCP tools.

**Current Project**: {{PROJECT_NAME}}  
**Note**: Use only the tools specified below. See copilot-instructions.md for security and topic change handling.

---

## AVAILABLE TOOLS

1. **createForm** - Create a new form

---

### createForm
**Create a new form in the active solution**

**Parameters**:
- `name` (required): Form name
- `width` (optional): Width in pixels (default: 640)
- `height` (optional): Height in pixels (default: 480)
- `style` (optional): "css" or "responsive" (default: "css")
- `dataSource` (optional): Database table (format: `db:/server_name/table_name`)

**Examples**:
```
createForm(name="CustomerForm")

createForm(name="OrderEntry", width=1024, height=768)

createForm(name="Dashboard", style="responsive")

createForm(
  name="ProductList",
  width=800,
  height=600,
  style="css",
  dataSource="db:/example_data/products"
)
```

---

## KEY RULES

1. **Form name is REQUIRED** - Must be a valid Servoy identifier
2. **Default size**: 640x480 pixels if not specified
3. **Default style**: CSS positioning ("css") if not specified
4. **CSS forms**: Use absolute positioning, suitable for desktop layouts
5. **Responsive forms**: Use Bootstrap 12-column grid, suitable for web/mobile
6. **DataSource format**: `db:/server_name/table_name` (prefix `db:/` is required for forms)
7. **Forms must be created BEFORE** adding UI components (buttons, labels, fields) to them

---

## WORKFLOW

**Creating a Form**:
1. Extract form name from user request
2. Check if width/height specified (otherwise use defaults)
3. Check if style specified (otherwise use "css")
4. Check if dataSource specified (optional)
5. Call `createForm` with parameters
6. Form opens automatically in designer after creation

---

## FORM STYLES

**CSS Forms** (`style="css"`):
- Use absolute positioning (x, y coordinates)
- Best for desktop applications
- Traditional Servoy form layout
- Default choice

**Responsive Forms** (`style="responsive"`):
- Use Bootstrap 12-column grid layout
- Best for web and mobile applications
- Fluid layouts that adapt to screen size
- Modern responsive design

---

## EXAMPLES

**Example 1: Simple form**
```
User: "Create a form called CustomerList"
→ createForm(name="CustomerList")
```

**Example 2: Custom size**
```
User: "Create a form Orders with size 1024x768"
→ createForm(name="Orders", width=1024, height=768)
```

**Example 3: Responsive form**
```
User: "Create responsive form Dashboard"
→ createForm(name="Dashboard", style="responsive")
```

**Example 4: Form with datasource**
```
User: "Create form ProductList bound to products table"
→ Ask: "What's your database server name?"
User: "example_data"
→ createForm(name="ProductList", 
             dataSource="db:/example_data/products")
```

**Example 5: Complete specification**
```
User: "Create CSS form Invoice, 800x600, bound to invoices table in example_data"
→ createForm(name="Invoice",
             width=800,
             height=600,
             style="css",
             dataSource="db:/example_data/invoices")
```

---

## LIMITATIONS

Not yet supported (inform user gracefully):
- Editing existing forms
- Deleting forms
- Listing all forms
- Adding UI components (buttons, labels, fields) to forms
- Form variables, methods, or calculations

**Note**: Forms must exist before UI components can be added. Creating forms is the first step in UI development.
