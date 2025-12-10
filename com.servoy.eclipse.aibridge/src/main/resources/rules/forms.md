=== SERVOY FORM OPERATIONS ===

**Goal**: Manage Servoy forms using the available MCP tools.

**Current Project**: {{PROJECT_NAME}}  
**Note**: Use only the tools specified below. See copilot-instructions.md for security and topic change handling.

---

## [CRITICAL] TOOL USAGE RESTRICTIONS

**For form operations, use ONLY the tools specified below and NO other tools (like file_search, grep_search, workspace search, semantic search, etc.).**

**See copilot-instructions.md RULE 6 for complete tool restrictions.**

**Key points:**
- [YES] ONLY use the 3 form tools listed in "AVAILABLE TOOLS" section below
- [YES] Stay within {{PROJECT_NAME}} project
- [YES] If listForms() doesn't show a form, it does NOT exist - STOP immediately
- [NO] Do NOT use file system or search tools to "find" forms
- [NO] Do NOT search in other projects

---

## AVAILABLE TOOLS

1. **openForm** - Open existing form or create new form
2. **setMainForm** - Set solution's main/first form
3. **listForms** - List all forms in solution
4. **getFormProperties** - Get properties of a form

---

### openForm
**Open an existing form or create a new form in the active solution**

**Parameters**:
- `name` (required): Form name
- `create` (optional): Boolean, default false - if true, creates form if it doesn't exist
- `width` (optional): Width in pixels (default: 640)
- `height` (optional): Height in pixels (default: 480)
- `style` (optional): "css" or "responsive" (default: "css")
- `dataSource` (optional): Database table (format: `db:/server_name/table_name`)
- `extendsForm` (optional): Parent form name for inheritance
- `setAsMainForm` (optional): Boolean, default false - sets as solution's first form
- `properties` (optional): Object/map of form properties to set (see Properties section below)

**Examples**:
```
# Open existing form
openForm(name="CustomerForm")

# Create new form if doesn't exist
openForm(name="OrderEntry", create=true, width=1024, height=768)

# Create responsive form
openForm(name="Dashboard", create=true, style="responsive")

# Create form with datasource and set as main
openForm(
  name="ProductList",
  create=true,
  width=800,
  height=600,
  style="css",
  dataSource="db:/example_data/products",
  setAsMainForm=true
)

# Create form with inheritance
openForm(
  name="CustomerDetail",
  create=true,
  extendsForm="BaseForm"
)

# Update existing form size (simple syntax)
openForm(name="ExistingForm", width=1024, height=768)

# Update existing form properties
openForm(
  name="ExistingForm",
  properties={
    "showInMenu": true,
    "useMinWidth": true,
    "initialSort": "customer_name asc"
  }
)

# Update size via properties (alternative syntax)
openForm(
  name="ExistingForm",
  properties={
    "width": 1024,
    "height": 768
  }
)
```

---

### setMainForm
**Set the solution's main/first form (the form that loads when the solution starts)**

**Parameters**:
- `name` (required): Form name to set as main form

**Examples**:
```
setMainForm(name="Dashboard")
```

---

### listForms
**List all forms in the active solution**

**Parameters**: None

**Examples**:
```
listForms()
```

Returns list of all forms with indicator for main form

---

### getFormProperties
**Get the properties of a form**

**Parameters**:
- `name` (required): Form name

**Examples**:
```
getFormProperties(name="CustomerForm")
```

**Returns**: Detailed form properties including:
- Dimensions (width, height, useMinWidth, useMinHeight)
- Form type (css-positioned, responsive, or absolute-positioned)
- Data source
- Settings (showInMenu, styleName, navigatorID, initialSort)
- Inheritance (parent form if applicable)
- Main form status

---

## [REMINDER] THESE ARE THE ONLY VALID TOOLS

**For ALL form-related operations, you may ONLY use these 4 tools:**
1. `openForm` - Open/create/edit forms
2. `setMainForm` - Set main form
3. `listForms` - List all forms
4. `getFormProperties` - Get form properties

**[CRITICAL] Use only these specified tools for form operations and no other tools (like file_search, grep_search, workspace search, semantic search, etc.). See copilot-instructions.md RULE 6.**

**To check if a form exists:** Use `listForms()` - this is the ONLY valid method.

**If form not in listForms() output:** It does NOT exist in {{PROJECT_NAME}} - STOP and inform user.

---

## FORM PROPERTIES

The `properties` parameter accepts an object/map with these keys:

**Dimension Properties**:
- `width` (int): Form width in pixels
- `height` (int): Form height in pixels
- `useMinWidth` (boolean): Enable minimum width constraint
- `useMinHeight` (boolean): Enable minimum height constraint

**Core Properties**:
- `dataSource` (string): Database table (format: `db:/server_name/table_name`)
- `showInMenu` (boolean): Show form in Window menu (Servoy Client only)
- `styleName` (string): Servoy style name
- `navigatorID` (string): Navigator form ID or special values (DEFAULT, NONE, IGNORE)
- `initialSort` (string): Default sort order (e.g., "customer_name asc, id desc")

**Example**:
```
properties={
  "width": 1024,
  "useMinWidth": true,
  "showInMenu": false,
  "initialSort": "name asc"
}
```

---

## KEY RULES

1. **Form name is REQUIRED** - Must be a valid Servoy identifier
2. **Opening vs Creating**: Use `create=true` to create form if it doesn't exist
3. **Default size**: 640x480 pixels if not specified
4. **Default style**: CSS positioning ("css") if not specified
5. **CSS forms**: Use absolute positioning, suitable for desktop layouts
6. **Responsive forms**: Use Bootstrap 12-column grid, suitable for web/mobile
7. **DataSource format**: `db:/server_name/table_name` (prefix `db:/` is required for forms)
8. **Form inheritance**: Use `extendsForm` to specify parent form for inheritance
9. **Main form**: Solution's first form loads automatically on startup
10. **Property updates**: Can update properties on existing forms without `create=true`
11. **First form auto-main**: When creating the FIRST form in a solution (no other forms exist), it is AUTOMATICALLY set as the main form - no need to specify `setAsMainForm=true`
12. **Width/Height for updates**: The `width` and `height` parameters work for BOTH creating AND updating forms - use them directly: `openForm(name="MyForm", width=800, height=600)` to resize an existing form

---

## CRITICAL: FORM VALIDATION RULES

**[CRITICAL] See copilot-instructions.md RULE 6 for tool restrictions. Use ONLY the 3 form tools - no file system or search tools.**

**[REQUIRED] When validating if a form exists:**

1. **ALWAYS use listForms tool** - This is the ONLY way to check form existence in {{PROJECT_NAME}}
2. **NEVER guess** - If form not in listForms output, it does NOT exist
3. **listForms is the ONLY source of truth** - Accept its results

**[REQUIRED] When user specifies extendsForm (parent form):**

1. **FIRST call listForms()** - Get list of all available forms in {{PROJECT_NAME}}
2. **CHECK if parent form exists** - Look in the listForms output ONLY
3. **If parent form NOT found** - Display error to user immediately:
   ```
   Error: Parent form 'FormName' does not exist in project {{PROJECT_NAME}}.
   Available forms: [list from listForms output]
   ```
4. **STOP immediately** - Do NOT call openForm if parent doesn't exist
5. **STOP immediately** - Accept that the form doesn't exist and inform user

**[FORBIDDEN] After listForms returns no match:**

If you call `listForms()` and the form is NOT in the output:
- [YES] ACCEPT the result: Form does NOT exist in {{PROJECT_NAME}}
- [YES] Display error to user with available forms
- [YES] STOP - Do NOT proceed with operation
- [NO] Do NOT use any other tools to search for the form (See RULE 6)

**[WRONG] Incorrect approach:**
```
User: "Create form that extends BaseForm"

[NO] WRONG - After listForms fails, AI searches file system
[NO] WRONG - After listForms fails, AI searches other projects  
[NO] WRONG - After listForms fails, AI uses any other tools

See copilot-instructions.md RULE 6 - ALL non-specified tools are FORBIDDEN!
```

**[CORRECT] Correct approach:**
```
User: "Create form that extends BaseForm"

Step 1: Call listForms() - The ONLY tool for checking forms
Step 2: Check if "BaseForm" is in the output
Step 3a: If YES --> Proceed with openForm(extendsForm="BaseForm")
Step 3b: If NO --> Display error and STOP

Example when BaseForm NOT found:
--> listForms() returns: Dashboard, OrderList, ProductView
--> "BaseForm" is NOT in the list
--> Display: "Error: Parent form 'BaseForm' does not exist in {{PROJECT_NAME}}.
   Available forms: Dashboard, OrderList, ProductView"
--> STOP - Do NOT use any other tools (RULE 6)
```

**Example of correct validation:**
```
User: "Create CustomerDetail form that extends BaseForm"

Step 1: Call listForms()
Output: "Forms in solution (3 total):
- Dashboard [MAIN FORM]
- OrderList
- ProductView"

Step 2: Check if "BaseForm" exists
Result: NO (not in list)

Step 3: Display error
"Error: Parent form 'BaseForm' does not exist.
Available forms:
- Dashboard
- OrderList  
- ProductView

Please create BaseForm first, or choose an existing form as parent."
```

---

## WORKFLOW

**Opening/Creating a Form**:
1. Extract form name from user request
2. Determine if form should be created (analyze user intent)
3. **[CRITICAL] If extendsForm specified**:
   - **FIRST call listForms()** to get all available forms
   - **CHECK if parent form exists** in the output
   - **If parent NOT found**: Display error with available forms, STOP
   - **If parent found**: Continue to next step
4. Check if width/height/style specified (use defaults if not)
5. Check if dataSource specified (optional)
6. Check if should be set as main form
7. Call `openForm` with parameters
8. Form opens automatically in designer after operation

**Setting Main Form**:
1. Extract form name from user request
2. **[RECOMMENDED] Call listForms()** to verify form exists
3. Call `setMainForm` with form name
4. Confirm to user

**Listing Forms**:
1. Call `listForms()`
2. Present list to user with main form indicator

**Validating Form Existence (General)**:
1. **ALWAYS use listForms()** - This is the canonical source
2. **NEVER search file system** - Forms only exist if in listForms output
3. Parse listForms output to check for form name
4. If not found, display error with available options

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

**Example 1: Open existing form**
```
User: "Open CustomerList form"
--> openForm(name="CustomerList")
```

**Example 2: Create simple form**
```
User: "Create a form called CustomerList"
--> openForm(name="CustomerList", create=true)
```

**Example 3: Create form with custom size**
```
User: "Create a form Orders with size 1024x768"
--> openForm(name="Orders", create=true, width=1024, height=768)
```

**Example 4: Create responsive form**
```
User: "Create responsive form Dashboard"
--> openForm(name="Dashboard", create=true, style="responsive")
```

**Example 5: Create form with datasource**
```
User: "Create form ProductList bound to products table"
--> Ask: "What's your database server name?"
User: "example_data"
--> openForm(name="ProductList", create=true,
           dataSource="db:/example_data/products")
```

**Example 6: Complete specification with main form**
```
User: "Create CSS form Invoice, 800x600, bound to invoices table in example_data, and set it as main form"
--> openForm(name="Invoice",
           create=true,
           width=800,
           height=600,
           style="css",
           dataSource="db:/example_data/invoices",
           setAsMainForm=true)
```

**Example 7: Create form with inheritance**
```
User: "Create CustomerDetail form that extends BaseForm"
--> openForm(name="CustomerDetail",
           create=true,
           extendsForm="BaseForm")
```

**Example 8: Update existing form properties**
```
User: "Set ExistingForm to show in menu and enable minimum width"
--> openForm(name="ExistingForm",
           properties={
             "showInMenu": true,
             "useMinWidth": true
           })
```

**Example 9: List all forms**
```
User: "Show me all forms in this solution"
--> listForms()
```

**Example 10: Set main form**
```
User: "Make Dashboard the main form"
--> setMainForm(name="Dashboard")
```

**Example 11: Form validation - Parent form exists**
```
User: "Create CustomerDetail form that extends BaseForm"

Step 1: Validate parent form exists
--> listForms()
Output: "Forms in solution (4 total):
- BaseForm
- Dashboard [MAIN FORM]
- OrderList
- ProductView"

Step 2: BaseForm found in list, proceed
--> openForm(name="CustomerDetail", create=true, extendsForm="BaseForm")

Result: SUCCESS
```

**Example 12: Form validation - Parent form does NOT exist**
```
User: "Create CustomerDetail form that extends NonExistentForm"

Step 1: Validate parent form exists
--> listForms()
Output: "Forms in solution (3 total):
- Dashboard [MAIN FORM]
- OrderList
- ProductView"

Step 2: NonExistentForm NOT found in list
--> ACCEPT this result - the form does NOT exist in {{PROJECT_NAME}}
--> Display error to user:

"Error: Parent form 'NonExistentForm' does not exist in project {{PROJECT_NAME}}.

Available forms in this solution:
- Dashboard
- OrderList
- ProductView

Please either:
1. Create 'NonExistentForm' first
2. Choose an existing form as parent
3. Create CustomerDetail without a parent (remove extends)"

Step 3: STOP IMMEDIATELY
--> Do NOT use any other tools to search for the form
--> See copilot-instructions.md RULE 6 for tool restrictions

Result: STOPPED, user informed, NO openForm call made, NO other tool calls made
```

---

## LIMITATIONS

Not yet fully supported (inform user gracefully):
- Deleting forms
- Adding UI components (buttons, labels, fields) to forms (use separate component tools)
- Form variables, methods, or calculations (coming soon)
- Complex form layout modifications
- Cross-project form operations

**Note**: Forms must exist before UI components can be added. Creating/opening forms is the first step in UI development.
