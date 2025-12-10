=== SERVOY STYLE MANAGEMENT ===

**Goal**: Manage CSS/LESS styles using the available MCP tools.

**Current Project**: {{PROJECT_NAME}}  
**Note**: Use only the tools specified below. See copilot-instructions.md for security and topic change handling.

---

## [CRITICAL] TOOL USAGE RESTRICTIONS

**For style operations, use ONLY the tools specified below and NO other tools (like file_search, grep_search, workspace search, etc.).**

**See copilot-instructions.md RULE 6 for complete tool restrictions.**

**Key points:**
- [YES] ONLY use the 4 style tools listed in "AVAILABLE TOOLS" section below
- [YES] Stay within {{PROJECT_NAME}} project
- [YES] If tool returns "not found", ACCEPT that result and STOP immediately
- [NO] Do NOT use file system or search tools
- [NO] Do NOT search in other projects
- [NO] Do NOT try "alternative methods"

## AVAILABLE TOOLS

### Style Management Tools

#### addStyle
Adds or updates a CSS/LESS class in ai-generated.less file.

**Parameters:**
- className (string, REQUIRED) - CSS class name WITHOUT dot (e.g., "btn-custom-blue")
- cssContent (string, REQUIRED) - CSS/LESS rules - CONTENT ONLY, NOT the wrapper. Supports:
  - Simple CSS: "background-color: blue; color: white"
  - LESS with nesting: Multi-line with &:hover, &:active, etc.

**[CRITICAL] Send ONLY the CSS content:**
- [WRONG] cssContent=".btn-glow { color: red; }"  ← includes wrapper
- [CORRECT] cssContent="color: red;"  ← content only
- The tool automatically adds: .className { your-content }

**Behavior:**
- If className exists: Replaces existing content
- If className does not exist: Appends new class
- Auto-imports ai-generated.less into main solution .less file (first time only)
- Creates backup before modifications (.less.backup)
- VALIDATES syntax before writing (prevents malformed CSS)

**File Location:** medias/ai-generated.less

**IMPORTANT - LESS Syntax Support:**
- [YES] Nested selectors are supported: &:hover { }, &:active { }, &:focus { }
- [YES] LESS variables and features work: @color, mixins, etc.
- [NO] Do NOT use duplicate braces: "{ {" or "}; }"
- [REQUIRED] Braces must be balanced (same number of opening and closing)

**Examples:**

Simple flat CSS:
```
addStyle(
  className="btn-simple",
  cssContent="background-color: #007bff; color: white; padding: 10px 20px; border-radius: 8px"
)
```

LESS with nested selectors:
```
addStyle(
  className="btn-glow-green",
  cssContent="background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border: none;
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.5);
  
  &:hover {
    background: linear-gradient(135deg, #059669 0%, #047857 100%);
    box-shadow: 0 0 30px rgba(16, 185, 129, 0.8);
    transform: translateY(-2px);
  }
  
  &:active {
    transform: translateY(0);
  }"
)
```

**Validation Errors:**
If syntax is invalid, you will receive a detailed error message explaining the issue. Common errors:
- "Duplicate opening brace '{ {' detected" - Remove extra brace
- "Unbalanced braces" - Check that each { has a matching }
- "Invalid semicolon after final closing brace" - Remove "; " after last }
```

#### getStyle
Gets the CSS content of a class from ai-generated.less file.

**Parameters:**
- className (string, REQUIRED) - CSS class name WITHOUT dot

**Returns:** CSS rules if found, or message if not found

**Example:**
```
getStyle(className="btn-custom-blue")
```

#### listStyles
Lists all CSS class names defined in ai-generated.less file.

**Parameters:** None

**Returns:** Comma-separated list of class names (without dots)

**Example:**
```
listStyles()
```

#### deleteStyle
Deletes a CSS class from ai-generated.less file.

**Parameters:**
- className (string, REQUIRED) - CSS class name WITHOUT dot

**Behavior:** Creates backup before deletion

**Example:**
```
deleteStyle(className="btn-custom-blue")
```

## KEY RULES

### 1. Style Naming Convention
- Class names should be descriptive and kebab-case
- No dots in className parameter (e.g., "btn-custom-blue", not ".btn-custom-blue")
- Avoid generic names that might conflict with existing styles
- Consider prefixing: "btn-", "label-", "form-", etc.

### 3. CSS/LESS Content Format
Two formats supported:

**Simple CSS (semicolon-separated):**
- Use semicolon-separated property-value pairs
- No curly braces needed (added automatically at class level)
- Example: "background: blue; color: white; padding: 10px"
- Properties are formatted with 2-space indent in file

**LESS with nested selectors:**
- Multi-line format with nested blocks
- Use &:hover, &:active, &:focus for pseudo-classes
- Each nested block needs its own braces
- Example:
  ```
  background: blue;
  color: white;
  
  &:hover {
    background: darkblue;
  }
  ```

**[CRITICAL] Syntax Rules:**
- [YES] One opening brace per selector: .btn { NOT .btn { {
- [YES] One closing brace per block: } NOT }; } or } }
- [YES] Balanced braces: same count of { and }
- [YES] Semicolons after properties: color: red;
- [NO] No semicolon after class closing brace: } NOT };
- [NO] No duplicate braces: { { or } }

**[WRONG] Common Syntax Errors:**
```
.btn-glow {
{                           <-- WRONG: Duplicate opening brace
  color: red;
}
};                          <-- WRONG: Semicolon after closing brace
}                           <-- WRONG: Extra closing brace
```

**[CORRECT] Proper Syntax:**
```
.btn-glow {
  color: red;
  &:hover {
    color: darkred;
  }
}
```

### 4. Variant Categories
Common categories:
- button
- label
- checkbox
- textbox
- textarea
- combobox
- calendar
- (any component type)

### 5. Applying Styles to Components
Create styles with addStyle, then apply using styleClass parameter in component tools:
- Example: addButton(formName="myForm", name="btnSubmit", text="Submit", styleClass="btn btn-custom-blue")
- Multiple classes supported: styleClass="btn btn-primary btn-large"

### 6. Check Before Create
Always check if style exists before creating:

**For Styles:**
```
getStyle(className="btn-custom-blue")
If not found --> addStyle(...)
If found and needs update --> addStyle(...) [replaces content]
```

### 7. File Structure
Style files are in medias/ folder:
- medias/ai-generated.less - AI-generated CSS classes
- medias/{solutionName}.less - Main solution styles (auto-import added)

### 8. Backup Safety
All modifications create backup files:
- .less.backup for style changes
- Backups allow recovery if needed

### 9. LESS Import
First time ai-generated.less is created, import is added to main solution .less file:
- Import statement: @import "ai-generated.less";
- Added at top of file (after initial comments)
- Only imported once (duplicate check)

### 10. Parameter Guessing Prevention
[FORBIDDEN] Never guess parameter values
[REQUIRED] If className or cssContent not provided: ASK user

## WORKFLOW

### Creating Styled Components

**Scenario 1: User wants custom styled button**

1. Clarify requirements:
   - What style properties? (colors, padding, borders, etc.)
   - What's a good class name? (suggest based on appearance)

2. Create the CSS style:
   ```
   addStyle(
     className="btn-demo-primary",
     cssContent="background: #007bff; color: white; padding: 12px 24px; border-radius: 6px; border: none"
   )
   ```

3. Add component with style:
   ```
   addButton(
     formName="myForm",
     name="btnSubmit",
     text="Submit",
     cssPosition="20,-1,-1,25,100,40",
     styleClass="btn btn-demo-primary"
   )
   ```

**Scenario 2: User wants to update existing style**

1. Check current style:
   ```
   getStyle(className="btn-demo-primary")
   ```

2. Update with new content:
   ```
   addStyle(
     className="btn-demo-primary",
     cssContent="NEW CSS RULES HERE"
   )
   ```
   [Note: This REPLACES existing content]

**Scenario 3: User wants to list all styles/variants**

1. List styles:
   ```
   listStyles()
   ```

2. List all variants:
   ```
   listVariants()
   ```

3. List variants by category:
   ```
   listVariants(category="button")
   ```

**Scenario 4: User wants to delete style**

Delete style:
```
deleteStyle(className="btn-demo-primary")
```

## EXAMPLES

### Example 1: Blue Gradient Button Style

**User Request:** "Create a blue gradient button style"

**AI Response:**
```
I'll create a blue gradient button style for you.

Creating CSS class 'btn-blue-gradient':
addStyle(
  className="btn-blue-gradient",
  cssContent="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 24px; border-radius: 8px; border: none; font-weight: 600"
)

Style created successfully. You can now use it with:
styleClass="btn btn-blue-gradient"
```

### Example 2: Highlight Label Style

**User Request:** "Create a yellow highlight label style"

**AI Response:**
```
I'll create a yellow highlight label style.

Creating CSS class 'label-highlight':
addStyle(
  className="label-highlight",
  cssContent="background-color: #fff3cd; color: #856404; padding: 5px 10px; border: 1px solid #ffc107; border-radius: 4px; font-weight: bold"
)

Style created successfully. You can use it with:
styleClass="label-highlight"
```

### Example 3: Update Existing Style

**User Request:** "Change the button gradient to be more purple"

**AI Response:**
```
I'll update the blue gradient to be more purple.

First, checking current style:
getStyle(className="btn-blue-gradient")

Updating with new purple gradient:
addStyle(
  className="btn-blue-gradient",
  cssContent="background: linear-gradient(135deg, #a855f7 0%, #6b21a8 100%); color: white; padding: 12px 24px; border-radius: 8px; border: none; font-weight: 600"
)

Style updated successfully. Existing components using this class will automatically reflect the change.
```

### Example 4: Check Before Create

**User Request:** "Create a primary button style"

**AI Response:**
```
Let me check if a primary button style already exists.

Checking existing styles:
listStyles()

[If found in list]
I see there's already a 'btn-primary' style. Would you like me to:
1. Update the existing 'btn-primary' style?
2. Create a new style with a different name (e.g., 'btn-my-primary')?

[If not found]
No existing primary style found. Creating new style:
addStyle(
  className="btn-primary",
  cssContent="background: #007bff; color: white; padding: 10px 20px; border-radius: 5px; border: none"
)
```

### Example 5: Multiple Styles for Form

**User Request:** "Create styles for a contact form: submit button (green), cancel button (red), and form container"

**AI Response:**
```
I'll create three styles for your contact form.

1. Green Submit Button:
addStyle(
  className="btn-submit-green",
  cssContent="background: #28a745; color: white; padding: 12px 30px; border-radius: 6px; border: none; font-weight: 600"
)

2. Red Cancel Button:
addStyle(
  className="btn-cancel-red",
  cssContent="background: #dc3545; color: white; padding: 12px 30px; border-radius: 6px; border: none; font-weight: 600"
)

3. Form Container:
addStyle(
  className="form-container",
  cssContent="padding: 20px; background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1)"
)

All styles created. You can now apply them to your form components.
```

## ERROR HANDLING

### Common Errors

**Error: 'className' parameter is required**
- Cause: Missing className parameter
- Solution: Provide className when calling addStyle, getStyle, or deleteStyle

**Error: 'cssContent' parameter is required**
- Cause: Missing cssContent parameter in addStyle
- Solution: Provide CSS rules as cssContent parameter

### CSS Syntax Validation Errors

The addStyle tool validates CSS/LESS syntax before writing to prevent file corruption. If validation fails, you will receive a detailed error message explaining the issue and how to fix it.

**Error: "CSS syntax error: Duplicate opening brace '{ {' detected"**
- Cause: Extra opening brace after selector
- [WRONG]:
  ```
  .btn-glow-green {
  {
    color: red;
  }
  ```
- [CORRECT]:
  ```
  .btn-glow-green {
    color: red;
  }
  ```
- Solution: Remove the extra opening brace

**Error: "CSS syntax error: Duplicate closing brace '} }' detected"**
- Cause: Extra closing brace in the middle of the style
- [WRONG]:
  ```
  .btn-glow {
    color: red;
  }
  }
  ```
- [CORRECT]:
  ```
  .btn-glow {
    color: red;
  }
  ```
- Solution: Remove the extra closing brace

**Error: "CSS syntax error: Invalid closing sequence '};' detected"**
- Cause: Semicolon after closing brace at class level
- [WRONG]:
  ```
  .btn-glow {
    color: red;
  };
  ```
- [CORRECT]:
  ```
  .btn-glow {
    color: red;
  }
  ```
- Solution: Remove semicolon after final closing brace (CSS classes end with } not };)

**Error: "CSS syntax error: Unbalanced braces"**
- Cause: Different number of opening and closing braces
- [WRONG]:
  ```
  .btn-glow {
    color: red;
    &:hover {
      color: blue;
  }
  ```
  (Missing closing brace for &:hover)
- [CORRECT]:
  ```
  .btn-glow {
    color: red;
    &:hover {
      color: blue;
    }
  }
  ```
- Solution: Add missing braces or remove extra braces to balance

**Real-World Example: The Glow Button Error**

This was the problematic CSS that triggered validation:
```
.btn-glow-green {
{                               <-- ERROR 1: Duplicate opening brace
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  &:hover {
    background: linear-gradient(135deg, #059669 0%, #047857 100%);
  }
};                              <-- ERROR 2: Invalid semicolon
}                               <-- ERROR 3: Extra closing brace
```

Validation would return:
```
"CSS syntax error: Duplicate opening brace '{ {' detected. 
Each selector should have only one opening brace. 
Example: '.btn { color: red; }' not '.btn { { color: red; }'"
```

Corrected version:
```
.btn-glow-green {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border: none;
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.5);
  transition: all 0.3s ease;
  
  &:hover {
    background: linear-gradient(135deg, #059669 0%, #047857 100%);
    box-shadow: 0 0 30px rgba(16, 185, 129, 0.8);
    transform: translateY(-2px);
  }
  
  &:active {
    background: linear-gradient(135deg, #047857 0%, #065f46 100%);
    transform: translateY(0);
  }
}
```

**Key Takeaway:** When you receive a validation error, READ THE ERROR MESSAGE carefully. It will tell you:
1. What the problem is (duplicate brace, unbalanced braces, etc.)
2. Where the problem is (line number if available)
3. How to fix it (example of correct syntax)

Then retry with corrected CSS.

---

**END OF STYLE MANAGEMENT RULES**