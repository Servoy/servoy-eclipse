=== SERVOY BOOTSTRAP LABEL COMPONENT - COMPLETE CRUD ===

**Goal**: Manage bootstrap label components in Servoy forms (Create, Read, Update, Delete).

**Current Project**: {{PROJECT_NAME}}  
**Note**: Use only the tools specified below. See copilot-instructions.md for security and topic change handling.

---

## [CRITICAL] TOOL USAGE RESTRICTIONS

**For label operations, use ONLY the 5 tools specified below and NO other tools.**

**See copilot-instructions.md RULE 6 for complete tool restrictions.**

**Key points:**
- [YES] ONLY use the 5 label tools listed below (addLabel, updateLabel, deleteLabel, listLabels, getLabelInfo)
- [YES] Call tools directly - they handle validation and return clear errors
- [YES] Stay within {{PROJECT_NAME}} project
- [NO] Do NOT use file_search, grep_search, read_file, or other workspace tools
- [NO] Do NOT manipulate .frm files directly
- [NO] Do NOT search in other projects

---

## AVAILABLE TOOLS (5)

1. **addLabel** - Add a new label to a form
2. **updateLabel** - Modify existing label properties
3. **deleteLabel** - Remove a label from a form
4. **listLabels** - List all labels in a form
5. **getLabelInfo** - Get detailed info about a specific label

---

## TOOL 1: addLabel

**Create a new bootstrap label component on a form**

**Parameters**:
- `formName` (REQUIRED): Name of the form (without .frm extension)
- `name` (REQUIRED): Unique component name (e.g., "lblCustomerName", "label_1")
- `cssPosition` (REQUIRED): Position string "top,right,bottom,left,width,height"
  - **Format**: "distance_from_top,distance_from_right,distance_from_bottom,distance_from_left,width,height"
  - **Values are DISTANCES from edges, not coordinates!**
  - Use `-1` for edges you don't want to constrain
  - Example: "20,-1,-1,25,80,30" = 20px from top, 25px from left, 80x30 size
- `text` (optional, default="Label"): Text to display in the label
- `styleClass` (optional): CSS classes to apply (space-separated, e.g., "label-primary label-bold")
- `labelFor` (optional): Name of input field this label is associated with
- `showAs` (optional): Display mode - "text" (default), "html", or "trusted_html"
- `enabled` (optional): Boolean - defaults to true if not specified
- `visible` (optional): Boolean - defaults to true if not specified
- `toolTipText` (optional): Tooltip text to display on hover

**Examples**:
```
# Add label at top-left (20px from top, 25px from left)
addLabel(
  formName="CustomerForm",
  name="lblCustomerName",
  text="Customer Name:",
  cssPosition="20,-1,-1,25,120,30"
)

# Add label below previous one (60px from top = 20 + 30 + 10 spacing)
addLabel(
  formName="CustomerForm",
  name="lblEmail",
  text="Email:",
  cssPosition="60,-1,-1,25,120,30"
)

# Add label with larger size for header
addLabel(
  formName="CustomerForm",
  name="lblTitle",
  text="Customer Information",
  cssPosition="10,-1,-1,25,250,35",
  styleClass="label-primary"
)

# Add three labels side by side (same top, different left positions)
addLabel(formName="OrderForm", name="lblStatus", text="Status:", cssPosition="20,-1,-1,20,80,25")
addLabel(formName="OrderForm", name="lblDate", text="Date:", cssPosition="20,-1,-1,110,80,25")
addLabel(formName="OrderForm", name="lblTotal", text="Total:", cssPosition="20,-1,-1,200,80,25")

# Add right-aligned label (25px from right edge)
addLabel(
  formName="HeaderForm",
  name="lblUser",
  text="User:",
  cssPosition="15,25,-1,-1,100,25"
)

# Add label with HTML content
addLabel(
  formName="InfoForm",
  name="lblNote",
  text="<strong>Note:</strong> Required field",
  cssPosition="50,-1,-1,25,200,25",
  showAs="html"
)
```

---

## TOOL 2: updateLabel

**Update properties of an existing label component**

**Parameters**:
- `formName` (REQUIRED): Name of the form
- `name` (REQUIRED): Name of the label to update
- Any property to update (optional): text, cssPosition, styleClass, labelFor, showAs, enabled, visible, toolTipText

**Only specified properties will be updated - others remain unchanged.**

**Examples**:
```
# Update label text
updateLabel(
  formName="CustomerForm",
  name="lblName",
  text="Full Name:"
)

# Move label to new position
updateLabel(
  formName="CustomerForm",
  name="lblName",
  cssPosition="30,-1,-1,20,120,25"
)

# Update multiple properties
updateLabel(
  formName="CustomerForm",
  name="lblName",
  text="Customer Name:",
  styleClass="label-primary",
  visible=true
)

# Change to HTML display
updateLabel(
  formName="CustomerForm",
  name="lblInfo",
  text="<i>Important</i> Note",
  showAs="html"
)
```

---

## TOOL 3: deleteLabel

**Remove a label component from a form**

**Parameters**:
- `formName` (REQUIRED): Name of the form
- `name` (REQUIRED): Name of the label to delete

**Examples**:
```
# Delete a label
deleteLabel(
  formName="CustomerForm",
  name="lblOldField"
)

# Clean up multiple labels
deleteLabel(formName="OrderForm", name="lblTemp1")
deleteLabel(formName="OrderForm", name="lblTemp2")
```

---

## TOOL 4: listLabels

**List all label components in a form**

**Parameters**:
- `formName` (REQUIRED): Name of the form

**Returns**: JSON array with label details (name, cssPosition, text, styleClass)

**Examples**:
```
# List all labels in a form
listLabels(formName="CustomerForm")

# Returns:
[
  {
    "name": "lblName",
    "typeName": "bootstrapcomponents-label",
    "cssPosition": "20,-1,-1,20,120,25",
    "text": "Name:",
    "styleClass": "label-default"
  },
  {
    "name": "lblEmail",
    "typeName": "bootstrapcomponents-label",
    "cssPosition": "50,-1,-1,20,120,25",
    "text": "Email:"
  }
]
```

**Use Case**: Before adding new label, list existing labels to:
- See current layout and positions
- Avoid duplicate names
- Calculate position for new label to avoid overlaps

---

## TOOL 5: getLabelInfo

**Get detailed information about a specific label**

**Parameters**:
- `formName` (REQUIRED): Name of the form
- `name` (REQUIRED): Name of the label

**Returns**: Full JSON object with all label properties

**Examples**:
```
# Get specific label details
getLabelInfo(
  formName="CustomerForm",
  name="lblName"
)

# Returns complete component JSON:
{
  "cssPosition": "20,-1,-1,20,120,25",
  "json": {
    "cssPosition": {
      "top": "20",
      "right": "-1",
      "bottom": "-1",
      "left": "20",
      "width": "120",
      "height": "25"
    },
    "text": "Name:",
    "styleClass": "label-default",
    "enabled": true,
    "visible": true
  },
  "name": "lblName",
  "typeName": "bootstrapcomponents-label",
  "typeid": 47,
  "uuid": "..."
}
```

**Use Case**: Before updating, get current label state to:
- See all current properties
- Determine what needs to change
- Preserve properties you don't want to modify

---

## CSS POSITIONING - CRITICAL UNDERSTANDING

**Format**: "top,right,bottom,left,width,height"

**[CRITICAL] First 4 values are DISTANCES from edges, NOT coordinates!**
- `top` = distance from TOP edge (in pixels)
- `right` = distance from RIGHT edge (in pixels) 
- `bottom` = distance from BOTTOM edge (in pixels)
- `left` = distance from LEFT edge (in pixels)
- `width` = component width (in pixels)
- `height` = component height (in pixels)
- `-1` = not set/unconstrained for that edge

**Visual Example:**
```
Form boundaries:
+----------------------------------+
|  top=20                          |
|  left=25 --> [Label 80x30]       |
|                                  |
+----------------------------------+

cssPosition = "20,-1,-1,25,80,30"
→ 20px from top edge
→ -1 for right (not set)
→ -1 for bottom (not set)  
→ 25px from left edge
→ 80px wide
→ 30px tall
```

**Common Patterns:**

### Pattern 1: Top-Left Positioned Label
```
cssPosition="20,-1,-1,25,80,30"
→ 20px from top, 25px from left, 80x30 size
```

### Pattern 2: Label Below Another (Vertical Stack)
```
First label:  "20,-1,-1,25,80,30"  (top=20, height=30)
Second label: "60,-1,-1,25,80,30"  (top=60, gives 10px spacing)
Third label:  "100,-1,-1,25,80,30" (top=100, gives 10px spacing)

Calculation: previous_top + previous_height + spacing = new_top
Example: 20 + 30 + 10 = 60
```

### Pattern 3: Labels Side by Side (Horizontal)
```
First label:  "20,-1,-1,25,80,30"   (left=25, width=80)
Second label: "20,-1,-1,115,80,30"  (left=115, gives 10px spacing)
Third label:  "20,-1,-1,205,80,30"  (left=205, gives 10px spacing)

Calculation: previous_left + previous_width + spacing = new_left
Example: 25 + 80 + 10 = 115
```

### Pattern 4: Right-Aligned Label
```
cssPosition="20,25,-1,-1,80,30"
→ 20px from top
→ 25px from RIGHT edge (note: right is set, left is -1)
→ 80x30 size
```

### Pattern 5: Bottom-Aligned Label
```
cssPosition="-1,-1,20,25,80,30"
→ top is -1 (not set)
→ 20px from BOTTOM edge
→ 25px from left edge
→ 80x30 size
```

### Pattern 6: Centered Label (using both left and right)
```
cssPosition="20,50,-1,50,200,30"
→ 20px from top
→ 50px from right edge
→ 50px from left edge
→ Centered horizontally (left+right spacing balances)
→ 200px wide, 30px tall
```

**Common Label Sizes:**
- Small: 60-80px wide, 20-25px tall
- Medium: 100-120px wide, 25-30px tall  
- Large: 150-200px wide, 30-35px tall
- Headers: 200-300px wide, 35-40px tall

**Typical Spacing:**
- Between labels vertically: 5-10px
- Between labels horizontally: 10-15px
- From form edges: 10-25px
- Between label and input field: 5px

**WRONG Examples:**
```
[WRONG] cssPosition="20,20,-1,-1,80,30"
→ This means 20px from top AND 20px from RIGHT edge (unusual)

[WRONG] cssPosition="20,-1,30,25,80,30"  
→ This means 20px from top AND 30px from bottom (conflicts!)

[WRONG] Using coordinates instead of distances:
cssPosition="100,200,-1,-1,80,30"
→ This is NOT at position x=100,y=200
→ This is 100px from top and 200px from RIGHT edge!
```

**CORRECT Examples:**
```
[CORRECT] Top-left corner with margins:
cssPosition="10,-1,-1,10,100,25"
→ 10px from top, 10px from left

[CORRECT] Below another component:
Previous: "10,-1,-1,10,100,25" (ends at top+height = 35px)
Current:  "45,-1,-1,10,100,25" (starts at 45px, 10px spacing)

[CORRECT] Next to another component:
Previous: "10,-1,-1,10,100,25" (ends at left+width = 110px)  
Current:  "10,-1,-1,120,100,25" (starts at 120px, 10px spacing)
```

---

## KEY RULES

1. **Required Parameters**:
   - addLabel: formName, name, cssPosition (text defaults to "Label")
   - updateLabel: formName, name, + at least one property to update
   - deleteLabel: formName, name
   - listLabels: formName
   - getLabelInfo: formName, name

2. **AI Controls Layout**: AI model calculates cssPosition based on user requirements
   - [REQUIRED] Use listLabels to see existing components before adding/updating
   - [REQUIRED] Calculate positions to avoid overlaps
   - [FORBIDDEN] Do NOT auto-generate positions - AI decides based on user intent

3. **Direct Tool Calls**: All tools validate internally and return clear errors
   - [YES] Call tools directly
   - [NO] Do NOT use file_search, grep_search, or read_file for validation

4. **Unique Names**: Each component name must be unique on the form

5. **No Manual Editing**: [FORBIDDEN] NEVER edit .frm files directly - use tools only

6. **Error Handling**: If tool returns error, report to user - do not retry with different approach

7. **Workflow for Complex Layouts**:
   - User: "Add 3 labels at top, divide form into sections"
   - AI: listLabels(form) → sees existing layout
   - AI: Calculates 3 positions to divide form
   - AI: addLabel() 3 times with calculated positions

---

## WORKFLOWS

### Workflow 1: Add New Label
1. [OPTIONAL] Call listLabels(formName) to see existing components and calculate positions
2. Calculate cssPosition based on layout requirements (avoid overlaps)
3. Call addLabel with formName, name, cssPosition, and other properties
4. Tool validates, creates component, saves form

### Workflow 2: Update Existing Label
1. [OPTIONAL] Call getLabelInfo(formName, name) to see current properties
2. Call updateLabel with formName, name, and properties to change
3. Tool updates specified properties, saves form

### Workflow 3: Delete Label
1. Call deleteLabel(formName, name)
2. Tool removes component, saves form

### Workflow 4: Complex Layout (Multiple Labels)
```
User: "Add Name, Email, Phone labels in a column at left side"

Step 1: Check existing layout
listLabels(formName="ContactForm")
→ See existing components and positions

Step 2: Calculate positions for new labels (column layout, 10px spacing)
Understanding: Format is "top,right,bottom,left,width,height"
- First 4 values = distances from edges
- All labels: 25px from left edge, 100px wide, 30px tall

Position calculations:
- lblName:  top=20  (first label, 20px from top)
- lblEmail: top=60  (previous: 20 top + 30 height + 10 spacing = 60)
- lblPhone: top=100 (previous: 60 top + 30 height + 10 spacing = 100)

Step 3: Add labels with calculated positions
addLabel(formName="ContactForm", name="lblName", cssPosition="20,-1,-1,25,100,30", text="Name:")
addLabel(formName="ContactForm", name="lblEmail", cssPosition="60,-1,-1,25,100,30", text="Email:")
addLabel(formName="ContactForm", name="lblPhone", cssPosition="100,-1,-1,25,100,30", text="Phone:")

Result: Three labels stacked vertically, all aligned 25px from left edge, 10px spacing between
```

### Integration with Styles
```
# Create custom style
addStyle(className="label-header", cssContent="font-size: 18px; font-weight: bold; color: #333")

# Add label with custom style
addLabel(formName="MyForm", name="lblHeader", cssPosition="20,-1,-1,20,200,30", text="Section Header", styleClass="label-header")

# Add label with multiple styles (space-separated)
addLabel(formName="MyForm", name="lblHeader2", cssPosition="60,-1,-1,20,200,30", text="Another Header", styleClass="label-header label-primary")
```

---

## COMPLETE EXAMPLES

### Example 1: CREATE - Add multiple labels in column layout
```
User: "Add Name, Email and Phone labels to CustomerForm in a column"

# AI calculates positions for vertical layout with 10px spacing
# Each label: 100px wide, 30px tall, 25px from left edge
# Position calculation: top = previous_top + previous_height + spacing

addLabel(formName="CustomerForm", name="lblName", text="Name:", cssPosition="20,-1,-1,25,100,30")
# First label: 20px from top, 25px from left

addLabel(formName="CustomerForm", name="lblEmail", text="Email:", cssPosition="60,-1,-1,25,100,30")
# Second label: 60px from top (20 + 30 + 10 spacing)

addLabel(formName="CustomerForm", name="lblPhone", text="Phone:", cssPosition="100,-1,-1,25,100,30")
# Third label: 100px from top (60 + 30 + 10 spacing)
```

### Example 2: READ - List and inspect labels
```
User: "Show me all labels in CustomerForm"

# List all labels
listLabels(formName="CustomerForm")

# Get specific label details
getLabelInfo(formName="CustomerForm", name="lblName")
```

### Example 3: UPDATE - Modify existing label
```
User: "Change the Name label text to 'Full Name' and make it bold"

# Option A: Update text and add style class
updateLabel(
  formName="CustomerForm",
  name="lblName",
  text="Full Name:",
  styleClass="font-weight-bold"
)

# Option B: First check current state, then update
getLabelInfo(formName="CustomerForm", name="lblName")
→ See current properties
updateLabel(formName="CustomerForm", name="lblName", text="Full Name:")
```

### Example 4: UPDATE - Move label to new position
```
User: "Move the Email label down by 50 pixels"

# Get current position
getLabelInfo(formName="CustomerForm", name="lblEmail")
→ Current: "60,-1,-1,25,100,30" (top=60, left=25)

# Update with new position (add 50 to top: 60 + 50 = 110)
updateLabel(
  formName="CustomerForm",
  name="lblEmail",
  cssPosition="110,-1,-1,25,100,30"
)
```

### Example 5: DELETE - Remove label
```
User: "Remove the old status label from OrderForm"

deleteLabel(formName="OrderForm", name="lblOldStatus")
```

### Example 6: COMPLEX - Layout with 3 section headers
```
User: "Add 3 labels at top dividing form into Personal, Address, and Notes sections"

# AI divides form width (assume 640px) into 3 equal sections (~213px each)
# Each label: same top position (10px), different left positions
# Calculation: section_width = 640 / 3 ≈ 213px, label_width = 200px

addLabel(formName="ContactForm", name="lblPersonal", text="Personal Info", cssPosition="10,-1,-1,10,200,30")
# First section: 10px from left

addLabel(formName="ContactForm", name="lblAddress", text="Address", cssPosition="10,-1,-1,220,200,30")  
# Second section: 220px from left (10 + 200 + 10 spacing)

addLabel(formName="ContactForm", name="lblNotes", text="Notes", cssPosition="10,-1,-1,430,200,30")
# Third section: 430px from left (220 + 200 + 10 spacing)
```

### Example 7: STYLED - Create and use custom styles
```
# Create header style
addStyle(
  className="section-header",
  cssContent="font-size: 16px; font-weight: bold; color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 5px"
)

# Add label with custom style
addLabel(
  formName="DashboardForm",
  name="lblDashboard",
  text="Dashboard Overview",
  cssPosition="20,-1,-1,20,250,35",
  styleClass="section-header"
)
```

### Example 8: UPDATE - Change label styling
```
User: "Make the title label use the warning style"

# Update to use warning style class
updateLabel(
  formName="AlertForm",
  name="lblTitle",
  styleClass="label-warning"
)

# Or add multiple style classes
updateLabel(
  formName="AlertForm",
  name="lblTitle",
  styleClass="label-warning label-bold"
)
```

---

## COMMON USE CASES

**Form Field Labels (sequential vertical placement):**
```
addLabel(formName="RegistrationForm", name="lblFirstName", text="First Name:", cssPosition="20,-1,-1,20,100,25")
addLabel(formName="RegistrationForm", name="lblLastName", text="Last Name:", cssPosition="75,-1,-1,20,100,25")
addLabel(formName="RegistrationForm", name="lblEmail", text="Email:", cssPosition="130,-1,-1,20,100,25")
addLabel(formName="RegistrationForm", name="lblPassword", text="Password:", cssPosition="185,-1,-1,20,100,25")
```

**Section Headers:**
```
addLabel(formName="ContactForm", name="lblPersonalInfo", text="Personal Information", cssPosition="20,-1,-1,20,200,30")
addLabel(formName="ContactForm", name="lblAddressInfo", text="Address Information", cssPosition="200,-1,-1,20,200,30")
```

**Status Labels:**
```
addLabel(formName="OrderForm", name="lblStatus", text="Status: Pending", cssPosition="20,-1,-1,20,150,25")
addLabel(formName="OrderForm", name="lblTotal", text="Total: $0.00", cssPosition="50,-1,-1,20,150,25")
```

**Instruction Labels:**
```
addLabel(
  formName="LoginForm",
  name="lblInstruction",
  text="Please enter your credentials to continue",
  cssPosition="20,-1,-1,20,350,25",
  showAs="text"
)
```

---

## ERROR HANDLING

**All tools validate internally and return clear error messages**

**If error occurs:**
- Report the error message to user
- Do NOT retry with file_search, grep_search, or other workarounds
- User can fix the issue and retry the tool

**Common errors:**

**addLabel errors:**
- "Error: 'formName' parameter is required"
- "Error: 'name' parameter is required"
- "Error: 'cssPosition' parameter is required"
- "Form 'xyz' not found. Available forms: CustomerForm, OrderForm, ..."
- "Invalid CSS position format. Expected 'top,right,bottom,left,width,height'"
- "Component with name 'lblX' already exists"

**updateLabel errors:**
- "Error: 'formName' parameter is required"
- "Error: 'name' parameter is required"
- "Error: No properties specified to update"
- "Form 'xyz' not found. Available forms: ..."
- "Component 'lblX' not found in form 'FormY'"
- "Invalid CSS position format. Expected 'top,right,bottom,left,width,height'"

**deleteLabel errors:**
- "Error: 'formName' parameter is required"
- "Error: 'name' parameter is required"
- "Form 'xyz' not found. Available forms: ..."
- "Component 'lblX' not found in form 'FormY'"

**listLabels errors:**
- "Error: 'formName' parameter is required"
- "Form 'xyz' not found. Available forms: ..."

**getLabelInfo errors:**
- "Error: 'formName' parameter is required"
- "Error: 'name' parameter is required"
- "Form 'xyz' not found. Available forms: ..."
- "Component 'lblX' not found in form 'FormY'"

---

## PARAMETER HANDLING

[FORBIDDEN] Never guess required parameter values  
[REQUIRED] If required parameter missing: ASK user  

**Required parameters by tool:**

**addLabel:**
- formName - ASK user if not provided
- name - ASK user OR generate unique name (lblFieldName, label_1, etc.)
- cssPosition - AI MUST calculate based on layout (use listLabels first to see existing components)
- text - Defaults to "Label" if not provided

**updateLabel:**
- formName - ASK user if not provided
- name - ASK user if not provided
- At least one property to update - ASK user what to change

**deleteLabel:**
- formName - ASK user if not provided
- name - ASK user if not provided

**listLabels:**
- formName - ASK user if not provided

**getLabelInfo:**
- formName - ASK user if not provided
- name - ASK user if not provided

**AI Responsibilities:**
- Calculate cssPosition based on user's layout intent
- Use listLabels to discover existing components
- Generate unique names when user doesn't specify
- Determine appropriate spacing and positioning

---

## COMPREHENSIVE POSITIONING EXAMPLES

### Scenario 1: Form Header and Content Labels
```
User: "Add a header label 'Customer Details' at top, then Name and Email labels below it"

# Header label - larger, bold
addLabel(formName="CustomerForm", name="lblHeader", text="Customer Details", 
  cssPosition="15,-1,-1,20,250,35", styleClass="font-weight-bold")
# 15px from top, 20px from left, 250x35 size

# Name label - below header with extra spacing
addLabel(formName="CustomerForm", name="lblName", text="Name:", 
  cssPosition="65,-1,-1,20,100,30")
# 65px from top (15 + 35 + 15 spacing)

# Email label - below name
addLabel(formName="CustomerForm", name="lblEmail", text="Email:", 
  cssPosition="105,-1,-1,20,100,30")
# 105px from top (65 + 30 + 10 spacing)
```

### Scenario 2: Two-Column Form Layout
```
User: "Create a two-column form with labels on left, space for inputs on right"

# Left column labels (25px from left)
addLabel(formName="ProfileForm", name="lblFirstName", text="First Name:", 
  cssPosition="20,-1,-1,25,100,30")
  
addLabel(formName="ProfileForm", name="lblLastName", text="Last Name:", 
  cssPosition="60,-1,-1,25,100,30")

addLabel(formName="ProfileForm", name="lblEmail", text="Email:", 
  cssPosition="100,-1,-1,25,100,30")

# Right column labels (300px from left - gives space for inputs in between)
addLabel(formName="ProfileForm", name="lblPhone", text="Phone:", 
  cssPosition="20,-1,-1,300,100,30")
  
addLabel(formName="ProfileForm", name="lblCity", text="City:", 
  cssPosition="60,-1,-1,300,100,30")

addLabel(formName="ProfileForm", name="lblCountry", text="Country:", 
  cssPosition="100,-1,-1,300,100,30")
```

### Scenario 3: Footer Labels (Bottom-Aligned)
```
User: "Add status labels at the bottom of the form"

# Bottom-aligned labels using bottom distance instead of top
addLabel(formName="DashboardForm", name="lblStatus", text="Status: Active", 
  cssPosition="-1,-1,20,20,150,25")
# 20px from bottom, 20px from left (top is -1 = not set)

addLabel(formName="DashboardForm", name="lblLastUpdate", text="Last Update: Today", 
  cssPosition="-1,-1,20,180,200,25")
# 20px from bottom, 180px from left (next to status)
```

### Scenario 4: Right-Aligned Labels
```
User: "Add user info label at top-right corner"

# Right-aligned using right distance
addLabel(formName="HeaderForm", name="lblUser", text="Welcome, User", 
  cssPosition="15,20,-1,-1,150,30")
# 15px from top, 20px from RIGHT edge (left is -1 = not set)

addLabel(formName="HeaderForm", name="lblLogout", text="Logout", 
  cssPosition="15,180,-1,-1,80,30")
# 15px from top, 180px from RIGHT edge (to left of user label)
# Calculation: 20 + 150 + 10 spacing = 180px from right
```

### Scenario 5: Grid Layout (3x3)
```
User: "Create a 3x3 grid of labels for a dashboard"

# Row 1 (top=20)
addLabel(formName="Dashboard", name="lblSales", text="Sales", cssPosition="20,-1,-1,20,150,40")
addLabel(formName="Dashboard", name="lblOrders", text="Orders", cssPosition="20,-1,-1,180,150,40")
addLabel(formName="Dashboard", name="lblCustomers", text="Customers", cssPosition="20,-1,-1,340,150,40")

# Row 2 (top=70, spacing of 10px from row 1)
addLabel(formName="Dashboard", name="lblRevenue", text="Revenue", cssPosition="70,-1,-1,20,150,40")
addLabel(formName="Dashboard", name="lblProfit", text="Profit", cssPosition="70,-1,-1,180,150,40")
addLabel(formName="Dashboard", name="lblGrowth", text="Growth", cssPosition="70,-1,-1,340,150,40")

# Row 3 (top=120)
addLabel(formName="Dashboard", name="lblTargets", text="Targets", cssPosition="120,-1,-1,20,150,40")
addLabel(formName="Dashboard", name="lblAchieved", text="Achieved", cssPosition="120,-1,-1,180,150,40")
addLabel(formName="Dashboard", name="lblPending", text="Pending", cssPosition="120,-1,-1,340,150,40")

# Grid calculation:
# Horizontal: left positions = 20, 180 (20+150+10), 340 (180+150+10)
# Vertical: top positions = 20, 70 (20+40+10), 120 (70+40+10)
```
