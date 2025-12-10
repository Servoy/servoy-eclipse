=== SERVOY BOOTSTRAP BUTTON COMPONENT - COMPLETE CRUD ===

**Goal**: Manage bootstrap button components in Servoy forms (Create, Read, Update, Delete).

**Current Project**: {{PROJECT_NAME}}  
**Note**: Use only the tools specified below. See copilot-instructions.md for security and topic change handling.

---

## [CRITICAL] TOOL USAGE RESTRICTIONS

**For button operations, use ONLY the 5 tools specified below and NO other tools.**

**See copilot-instructions.md RULE 6 for complete tool restrictions.**

**Key points:**
- [YES] ONLY use the 5 button tools listed below (addButton, updateButton, deleteButton, listButtons, getButtonInfo)
- [YES] Call tools directly - they handle validation and return clear errors
- [YES] Stay within {{PROJECT_NAME}} project
- [NO] Do NOT use file_search, grep_search, read_file, or other workspace tools
- [NO] Do NOT manipulate .frm files directly
- [NO] Do NOT search in other projects

---

## AVAILABLE TOOLS (5)

1. **addButton** - Add a new button to a form
2. **updateButton** - Modify existing button properties
3. **deleteButton** - Remove a button from a form
4. **listButtons** - List all buttons in a form
5. **getButtonInfo** - Get detailed info about a specific button

---

## TOOL 1: addButton

**Create a new bootstrap button component on a form**

**Parameters**:
- `formName` (REQUIRED): Name of the form (without .frm extension)
- `name` (REQUIRED): Unique component name (e.g., "btnSave", "button_1")
- `cssPosition` (REQUIRED): Position string "top,right,bottom,left,width,height"
  - **Format**: "distance_from_top,distance_from_right,distance_from_bottom,distance_from_left,width,height"
  - **Values are DISTANCES from edges, not coordinates!**
  - Use `-1` for edges you don't want to constrain
  - Example: "20,-1,-1,25,80,30" = 20px from top, 25px from left, 80x30 size
- `text` (REQUIRED, default="Button"): Text to display on the button
- `styleClass` (optional): CSS classes to apply (space-separated, e.g., "btn btn-primary align-center")
- `imageStyleClass` (optional): Icon to display to the left (e.g., "fas fa-save")
- `trailingImageStyleClass` (optional): Icon to display to the right (e.g., "fas fa-chevron-right")
- `showAs` (optional): Display mode - "text" (default), "html", or "trusted_html"
- `tabSeq` (optional): Tab sequence order number
- `enabled` (optional): Boolean - defaults to true if not specified
- `visible` (optional): Boolean - defaults to true if not specified
- `toolTipText` (optional): Tooltip text to display on hover

**Examples**:
```
# Add button at top-left (20px from top, 25px from left)
addButton(
  formName="CustomerForm",
  name="btnSave",
  text="Save",
  cssPosition="20,-1,-1,25,80,30"
)

# Add button below previous one (60px from top = 20 + 30 + 10 spacing)
addButton(
  formName="CustomerForm",
  name="btnCancel",
  text="Cancel",
  cssPosition="60,-1,-1,25,80,30"
)

# Add button with icon and styling
addButton(
  formName="CustomerForm",
  name="btnDelete",
  text="Delete",
  cssPosition="20,-1,-1,115,80,30",
  imageStyleClass="fas fa-trash",
  styleClass="btn btn-danger"
)

# Add three buttons side by side (same top, different left positions)
addButton(formName="OrderForm", name="btnNew", text="New", cssPosition="20,-1,-1,20,80,30")
addButton(formName="OrderForm", name="btnSave", text="Save", cssPosition="20,-1,-1,110,80,30")
addButton(formName="OrderForm", name="btnDelete", text="Delete", cssPosition="20,-1,-1,200,80,30")

# Add right-aligned button (25px from right edge)
addButton(
  formName="HeaderForm",
  name="btnLogout",
  text="Logout",
  cssPosition="15,25,-1,-1,100,30"
)

# Add button with HTML content
addButton(
  formName="InfoForm",
  name="btnSubmit",
  text="<strong>Submit</strong> Order",
  cssPosition="50,-1,-1,25,120,35",
  showAs="html"
)

# Add button with tooltip
addButton(
  formName="CustomerForm",
  name="btnHelp",
  text="Help",
  cssPosition="20,-1,-1,300,60,30",
  imageStyleClass="fas fa-question-circle",
  toolTipText="Click for help documentation"
)
```

---

## TOOL 2: updateButton

**Update properties of an existing button component**

**Parameters**:
- `formName` (REQUIRED): Name of the form
- `name` (REQUIRED): Name of the button to update
- Any property to update (optional): text, cssPosition, styleClass, imageStyleClass, trailingImageStyleClass, showAs, tabSeq, enabled, visible, toolTipText

**Only specified properties will be updated - others remain unchanged.**

**Examples**:
```
# Update button text
updateButton(
  formName="CustomerForm",
  name="btnSave",
  text="Save Changes"
)

# Move button to new position
updateButton(
  formName="CustomerForm",
  name="btnSave",
  cssPosition="30,-1,-1,20,100,30"
)

# Update multiple properties
updateButton(
  formName="CustomerForm",
  name="btnSave",
  text="Save Customer",
  styleClass="btn btn-primary",
  enabled=true
)

# Add icon to existing button
updateButton(
  formName="CustomerForm",
  name="btnSave",
  imageStyleClass="fas fa-save"
)

# Change to HTML display
updateButton(
  formName="CustomerForm",
  name="btnInfo",
  text="<i>Click</i> Here",
  showAs="html"
)
```

---

## TOOL 3: deleteButton

**Remove a button component from a form**

**Parameters**:
- `formName` (REQUIRED): Name of the form
- `name` (REQUIRED): Name of the button to delete

**Examples**:
```
# Delete a button
deleteButton(
  formName="CustomerForm",
  name="btnOldButton"
)

# Delete multiple buttons (call multiple times)
deleteButton(formName="OrderForm", name="btnTemp1")
deleteButton(formName="OrderForm", name="btnTemp2")
```

---

## TOOL 4: listButtons

**List all button components in a form**

**Parameters**:
- `formName` (REQUIRED): Name of the form

**Returns**: JSON array with button details (name, typeName, cssPosition, text, styleClass)

**Examples**:
```
# List all buttons in a form
listButtons(formName="CustomerForm")

# Use before adding to see existing buttons
listButtons(formName="OrderForm")
```

---

## TOOL 5: getButtonInfo

**Get detailed information about a specific button**

**Parameters**:
- `formName` (REQUIRED): Name of the form
- `name` (REQUIRED): Name of the button

**Returns**: Full JSON object with all button properties

**Examples**:
```
# Get details of a specific button
getButtonInfo(
  formName="CustomerForm",
  name="btnSave"
)

# Check button properties before updating
getButtonInfo(formName="OrderForm", name="btnSubmit")
```

---

## CSS POSITIONING - CRITICAL UNDERSTANDING

**Format**: "top,right,bottom,left,width,height"

**First 4 values are DISTANCES from edges, NOT coordinates:**
- `top`: Distance from top edge (or -1 if unconstrained)
- `right`: Distance from right edge (or -1 if unconstrained)
- `bottom`: Distance from bottom edge (or -1 if unconstrained)
- `left`: Distance from left edge (or -1 if unconstrained)
- `width`: Component width in pixels
- `height`: Component height in pixels

**Examples:**
```
"20,-1,-1,25,80,30"
→ 20px from top, 25px from left, 80px wide, 30px tall

"20,25,-1,-1,80,30"
→ 20px from top, 25px from right, 80px wide, 30px tall

"-1,-1,10,25,80,30"
→ 10px from bottom, 25px from left, 80px wide, 30px tall
```

**Calculating Positions:**

**Vertical stack:**
```
First button:  top=20
Second button: top = 20 + 30 (height) + 10 (spacing) = 60
Third button:  top = 60 + 30 + 10 = 100
```

**Horizontal row:**
```
First button:  left=20
Second button: left = 20 + 80 (width) + 10 (spacing) = 110
Third button:  left = 110 + 80 + 10 = 200
```

---

## KEY RULES

1. **Form Must Exist**: Form must exist before adding buttons
2. **Unique Names**: Each button must have a unique name within the form
3. **Position Required**: You must calculate and provide cssPosition (use listButtons to see existing components)
4. **No Manual Editing**: [FORBIDDEN] NEVER edit .frm files directly - use button tools only
5. **Styling Options**: Can use styleClass OR variant (not both)

---

## WORKFLOW

**Adding Buttons to a Form**:
1. Check current form (use getCurrentForm) or specify formName
2. List existing buttons to see what's already there: listButtons(formName="MyForm")
3. Calculate position based on existing components
4. Call addButton with all REQUIRED parameters (formName, name, cssPosition)
5. System will validate and add the button

**Updating Buttons**:
1. List buttons to find the one to update: listButtons(formName="MyForm")
2. Call updateButton with the properties to change
3. Only specified properties are updated

**Deleting Buttons**:
1. List buttons to confirm name: listButtons(formName="MyForm")
2. Call deleteButton with formName and name

---

## EXAMPLES

**Example 1: CRUD Form Buttons**
```
# First, list existing buttons to see positions
listButtons(formName="CustomerForm")

# Add New button at top-left
addButton(
  formName="CustomerForm",
  name="btnNew",
  text="New",
  cssPosition="20,-1,-1,20,80,30",
  styleClass="btn btn-primary"
)

# Add Save button next to it (same top, different left)
addButton(
  formName="CustomerForm",
  name="btnSave",
  text="Save",
  cssPosition="20,-1,-1,110,80,30",
  styleClass="btn btn-success",
  imageStyleClass="fas fa-save"
)

# Add Delete button
addButton(
  formName="CustomerForm",
  name="btnDelete",
  text="Delete",
  cssPosition="20,-1,-1,200,80,30",
  styleClass="btn btn-danger",
  imageStyleClass="fas fa-trash"
)

# Add Cancel button
addButton(
  formName="CustomerForm",
  name="btnCancel",
  text="Cancel",
  cssPosition="20,-1,-1,290,80,30"
)
```

**Example 2: Styled Buttons with Custom CSS**
```
# First create custom style (if using addStyle tool)
addStyle(className="btn-custom", cssContent="background: #007bff; color: white; padding: 10px 20px;")

# Add button with custom style
addButton(
  formName="OrderForm",
  name="btnSubmit",
  text="Submit Order",
  cssPosition="60,-1,-1,20,120,35",
  styleClass="btn btn-custom"
)
```

**Example 3: Update Button Properties**
```
# Change button text
updateButton(
  formName="CustomerForm",
  name="btnSave",
  text="Save Changes"
)

# Add icon to existing button
updateButton(
  formName="CustomerForm",
  name="btnSave",
  imageStyleClass="fas fa-save"
)

# Change button style
updateButton(
  formName="CustomerForm",
  name="btnSave",
  styleClass="btn btn-primary btn-lg"
)

# Move button to new position
updateButton(
  formName="CustomerForm",
  name="btnSave",
  cssPosition="30,-1,-1,25,100,35"
)
```

**Example 4: Grid Layout (3x2 buttons)**
```
# Row 1
addButton(formName="MenuForm", name="btn1", text="Option 1", cssPosition="20,-1,-1,20,100,40")
addButton(formName="MenuForm", name="btn2", text="Option 2", cssPosition="20,-1,-1,130,100,40")
addButton(formName="MenuForm", name="btn3", text="Option 3", cssPosition="20,-1,-1,240,100,40")

# Row 2 (70 = 20 top + 40 height + 10 spacing)
addButton(formName="MenuForm", name="btn4", text="Option 4", cssPosition="70,-1,-1,20,100,40")
addButton(formName="MenuForm", name="btn5", text="Option 5", cssPosition="70,-1,-1,130,100,40")
addButton(formName="MenuForm", name="btn6", text="Option 6", cssPosition="70,-1,-1,240,100,40")
```

**Example 5: Button with Icons**
```
# Button with left icon
addButton(
  formName="ToolbarForm",
  name="btnSave",
  text="Save",
  cssPosition="10,-1,-1,10,80,30",
  imageStyleClass="fas fa-save"
)

# Button with right icon
addButton(
  formName="ToolbarForm",
  name="btnNext",
  text="Next",
  cssPosition="10,-1,-1,100,80,30",
  trailingImageStyleClass="fas fa-chevron-right"
)

# Icon-only button (empty text)
addButton(
  formName="ToolbarForm",
  name="btnSettings",
  text="",
  cssPosition="10,-1,-1,190,40,30",
  imageStyleClass="fas fa-cog",
  toolTipText="Settings"
)
```

---

## COMMON USE CASES

**Dialog Buttons (OK/Cancel):**
```
addButton(formName="ConfirmDialog", name="btnOk", text="OK", cssPosition="200,20,-1,-1,80,30", styleClass="btn btn-primary")
addButton(formName="ConfirmDialog", name="btnCancel", text="Cancel", cssPosition="200,110,-1,-1,80,30")
```

**Search Form:**
```
addButton(formName="SearchForm", name="btnSearch", text="Search", cssPosition="50,-1,-1,20,100,30", imageStyleClass="fas fa-search")
addButton(formName="SearchForm", name="btnClear", text="Clear", cssPosition="50,-1,-1,130,100,30")
```

**Navigation Buttons:**
```
addButton(formName="RecordNav", name="btnFirst", text="First", cssPosition="10,-1,-1,10,60,25")
addButton(formName="RecordNav", name="btnPrev", text="Prev", cssPosition="10,-1,-1,80,60,25")
addButton(formName="RecordNav", name="btnNext", text="Next", cssPosition="10,-1,-1,150,60,25")
addButton(formName="RecordNav", name="btnLast", text="Last", cssPosition="10,-1,-1,220,60,25")
```

---

## ERROR HANDLING

**Error: "Form file not found"**
- Check form exists: `listForms()`
- Create form if needed: `openForm(name="FormName", create=true)`

**Error: "Form does not use CSS positioning"**
- Form must be created with style="css" or style="responsive"

**Error: "Component with name X already exists"**
- Use listButtons to see existing names
- Choose a different unique name

**Error: "'name' parameter is required"**
- You must provide a unique component name

**Error: "'cssPosition' parameter is required"**
- You must calculate and provide the position
- Use listButtons to see existing component positions

---

## PARAMETER GUESSING PREVENTION

[FORBIDDEN] Never guess parameter values  
[REQUIRED] If formName not provided: ASK user or use getCurrentForm  
[REQUIRED] If name not provided: ASK user for button name  
[REQUIRED] If cssPosition not provided: ASK user or tell them to use listButtons to calculate position

**All three parameters (formName, name, cssPosition) are REQUIRED for addButton.**

---

## INTEGRATION WITH OTHER TOOLS

**With getCurrentForm:**
```
# Get current form first
getCurrentForm()
→ Returns: "CustomerForm"

# Then add button to that form
addButton(formName="CustomerForm", name="btnSave", text="Save", cssPosition="20,-1,-1,25,80,30")
```

**With listButtons:**
```
# List existing buttons to calculate next position
listButtons(formName="OrderForm")
→ Returns: [button at position "20,-1,-1,20,80,30"]

# Calculate next position (same top, next left = 20 + 80 + 10 = 110)
addButton(formName="OrderForm", name="btnNew", text="New", cssPosition="20,-1,-1,110,80,30")
```

**With Styles:**
```
# Create style first
addStyle(className="btn-primary-lg", cssContent="background: #007bff; color: white; font-size: 16px; padding: 12px 24px;")

# Use style on button
addButton(formName="MyForm", name="btnSubmit", text="Submit", cssPosition="50,-1,-1,20,120,40", styleClass="btn btn-primary-lg")
```

---

**END OF BUTTON RULES**
