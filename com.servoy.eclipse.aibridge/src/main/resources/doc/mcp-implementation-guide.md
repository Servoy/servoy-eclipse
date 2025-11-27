# MCP Implementation Guide for Servoy Developer Copilot Plugin

**Last Updated:** November 26, 2025 - Bootstrap Components Implementation Complete

---

## CURRENT STATUS - Bootstrap UI Components Ready

**Phase 1 - Architecture Refactoring: COMPLETED (Nov 26, 2025 AM)**
- ✅ **Service Layer Created**: DatabaseSchemaService for shared database metadata access
- ✅ **Architecture Cleanup**: Removed CommonToolHandler, created DatabaseToolHandler
- ✅ **Tool Reorganization**: Moved FK discovery from common to RelationToolHandler
- ✅ **Rules Simplified**: All category rules files streamlined to ~130-160 lines each
- ✅ **New Database Tools**: Added `listTables` and `getTableInfo` (replaced getTableColumns)

**Phase 2 - Testing & Rule Enforcement: COMPLETED (Nov 26, 2025 PM)**
- ✅ **Critical Rules Added**: 4 major rules in copilot-instructions.md (~115 lines)
- ✅ **Text-Based Formatting**: Replaced all emoji/symbols with `[FORBIDDEN]` and `[REQUIRED]` markers
- ✅ **Tool Call Transparency**: Models must announce all parameter values before calling tools
- ✅ **User Cancellation Respect**: Explicit rules against retrying after user cancels
- ✅ **Parameter Guessing Prevention**: Multiple layers preventing parameter guessing

**Phase 3 - Bootstrap UI Components: COMPLETED (Nov 26, 2025 PM)**
- ✅ **FormFileService Created**: Safe .frm file JSON manipulation service
- ✅ **ComponentToolHandler Created**: Handler for bootstrap component operations
- ✅ **addButton Tool**: Adds bootstrap buttons to forms with validation
- ✅ **Bootstrap Rules Structure**: `/rules/bootstrap/buttons.md` (340 lines)
- ✅ **Bootstrap Embeddings**: `/embeddings/bootstrap/buttons.txt` (20 prompts)
- ✅ **UUID Generation**: Using `com.servoy.j2db.util.UUID` for component IDs
- ✅ **Backup Strategy**: Creates .frm.backup before any file modifications

**What's Working:**
- ✅ getContextFor tool implemented and working
- ✅ Action list generation works perfectly
- ✅ Similarity search fixed (20 prompts per category)
- ✅ Service layer enables code reuse across handlers
- ✅ Clear separation of concerns (database queries, relation ops, valuelist ops, form ops)

**Testing Phase Issues Discovered & Addressed:**
- 🔴 **Issue**: Model was calling tools multiple times with guessed serverName values
- ✅ **Fix**: Added prominent warnings in copilot-instructions.md and rule files
- 🔴 **Issue**: Model didn't announce parameter values before calling tools
- ✅ **Fix**: Added RULE 4: TOOL CALL TRANSPARENCY
- 🔴 **Issue**: Emoji symbols might not be reliably interpreted by all models
- ✅ **Fix**: Replaced all graphical symbols with text-based `[FORBIDDEN]`/`[REQUIRED]` markers

**Current Testing Focus:**
- Verifying models respect CRITICAL RULES
- Ensuring no parameter guessing occurs
- Testing user cancellation handling
- Validating tool call transparency

---

## Project Overview

We are building an MCP (Model Context Protocol) server for Servoy Developer that provides AI-powered context retrieval and tool execution for Servoy-specific development tasks.

**Main Location:** `/Users/marianvid/Servoy/git/master/servoy-eclipse/com.servoy.eclipse.aibridge/src/com/servoy/eclipse/mcp/`

---

## ⚠️ ARCHITECTURE EVOLUTION - NOVEMBER 25, 2025

### Critical Issue Discovered

**Problem:** Similarity-based intent detection with 100+ training examples per intent gets confused:
- "create a form" matched "create a relation" at 86% similarity
- As we add more intents (100+ Servoy categories), confusion increases
- Unpredictable user phrasing makes training data approach unscalable

**Root Cause:** Fighting against LLM capabilities instead of leveraging them.

### NEW APPROACH: Action List + Focused Context Retrieval

**Principle:** Model uses general AI knowledge to generate action list → Similarity search on focused queries → Returns only relevant context

#### Workflow:
```
1. User: "Create form with 2 buttons and customer dropdown"
   ↓
2. Model generates ACTION LIST:
   - create form
   - add buttons
   - create value list
   - add combobox
   ↓
3. Model calls: getContextFor({ queries: ["create form", "add buttons", ...] })
   ↓
4. MCP does similarity search PER LINE in knowledge base
   ↓
5. Returns aggregated context for matched categories
   ↓
6. Model creates execution plan with hierarchy
   ↓
7. Model executes tools in correct order
```

#### Key Changes:
- **Model-driven analysis** using general programming knowledge
- **Action list** = simple 2-4 word phrases (not single words, not full sentences)
- **One line per action type** (not per instance): "add buttons" even if 10 buttons
- **Focused similarity search** on specific actions, not entire user prompt
- **Auto-discovered categories** - no hardcoded list needed
- **Scalable** to 1000+ categories without instruction changes

#### Benefits:
✅ Leverages LLM's natural language understanding  
✅ Avoids similarity search confusion (focused queries vs mixed prompts)  
✅ Scalable indefinitely  
✅ Works with any LLM (Claude, GPT, etc.)  
✅ Categories auto-discovered through similarity matching  

---

## Current Architecture

### Core Components

1. **McpServletProvider** (`McpServletProvider.java`)
   - Entry point - creates and configures the MCP server
   - Provides `getContextFor` tool for action-based context retrieval
   - Auto-registers all handlers from ToolHandlerRegistry

2. **ToolHandlerRegistry** (`ToolHandlerRegistry.java`)
   - Central registry of all tool handlers
   - Provides helper method `registerTool()` to eliminate boilerplate
   - **To add new handler:** Just add one line to the `getHandlers()` array

3. **IToolHandler** (`IToolHandler.java`)
   - Interface all handlers must implement
   - Two methods: `registerTools(McpSyncServer)` and `getHandlerName()`

4. **ServoyEmbeddingService** (`ai/ServoyEmbeddingService.java`)
   - ONNX-based embedding service (BGE-small-en-v1.5)
   - Knowledge base with ~20 prompts per category
   - Provides similarity search for action matching

5. **DatabaseSchemaService** (`services/DatabaseSchemaService.java`)
   - Shared service layer for database metadata access
   - Used by all tool handlers that need database schema info
   - Methods: `getServer()`, `getTable()`, `getColumns()`, `getPrimaryKeyColumns()`, etc.
   - Analyzes explicit FKs and potential relations

6. **FormFileService** (`services/FormFileService.java`) - **NEW**
   - Safe manipulation of Servoy form (.frm) files
   - Handles JSON parsing, component addition, validation, file writing
   - Methods: `addButtonToForm()`, `validateFormFile()`, `getFormInfo()`
   - Creates backups before any file modifications
   - Generates UUIDs using `com.servoy.j2db.util.UUID`

### Tool Handlers (in `handlers/` directory)

- **DatabaseToolHandler** - Database schema queries
  - `listTables(serverName)` - List all tables in a database server
  - `getTableInfo(serverName, tableName)` - Get table structure and columns
  
- **RelationToolHandler** - Relation operations and FK discovery
  - `openRelation(...)` - Create/open relation
  - `deleteRelation(name)` - Delete relation
  - `listRelations()` - List all relations
  - `discoverRelations(serverName)` - Discover potential relations by analyzing FKs
  
- **ValueListToolHandler** - ValueList operations
  - `openValueList(...)` - Create/open valuelist (custom or database-based)
  - `deleteValueList(name)` - Delete valuelist
  - `listValueLists()` - List all valuelists
  
- **FormToolHandler** - Form creation and management
  - `createForm(...)` - Create new form

- **ComponentToolHandler** - Bootstrap UI component operations
  - `addButton(formName, buttonName, text, cssPosition)` - Add button to form

### Service Layer Architecture

**Key Innovation**: Separation of concerns through service layer

```
┌─────────────────────────────────────────┐
│         Tool Handlers (MCP Interface)    │
│  - DatabaseToolHandler                   │
│  - RelationToolHandler                   │
│  - ValueListToolHandler                  │
│  - FormToolHandler                       │
│  - ComponentToolHandler                  │
└─────────────────────────────────────────┘
                  ↓ (uses)
┌─────────────────────────────────────────┐
│         Service Layer                    │
│  - DatabaseSchemaService                 │
│    (shared database metadata access)     │
│  - FormFileService                       │
│    (safe .frm file manipulation)         │
└─────────────────────────────────────────┘
                  ↓ (accesses)
┌─────────────────────────────────────────┐
│      Servoy Persistence & Files         │
│  - IServerInternal, ITable, Column       │
│  - Form .frm files (JSON)                │
│  - Form .js files (JavaScript)           │
└─────────────────────────────────────────┘
```

**Benefits**:
- No code duplication across handlers
- Consistent database access patterns
- Easy to test and maintain
- Scalable to many handlers

---

## Implementation Status

### ✅ Phase 1: Core Architecture - COMPLETED (Nov 26, 2025)

**Service Layer:**
- ✅ DatabaseSchemaService created with all shared database operations
- ✅ Methods for tables, columns, PKs, explicit FKs, potential relations
- ✅ Used by all handlers - no code duplication

**Tool Handlers:**
- ✅ **DatabaseToolHandler** - Database queries (listTables, getTableInfo)
- ✅ **RelationToolHandler** - Relation operations + FK discovery (4 tools)
- ✅ **ValueListToolHandler** - ValueList operations (3 tools)
- ✅ **FormToolHandler** - Form creation (1 tool)

**Rules Documentation:**
- ✅ relations.md - Simplified to 131 lines, uses discoverRelations
- ✅ valuelists.md - Simplified to 155 lines, uses listTables + getTableInfo
- ✅ forms.md - Enhanced to 135 lines with comprehensive examples
- ✅ All rules follow consistent format

**Architecture Cleanup:**
- ✅ Removed CommonToolHandler (replaced with DatabaseToolHandler)
- ✅ Moved FK discovery from generic tool to RelationToolHandler (domain-specific)
- ✅ Clear separation: database queries vs relation operations vs valuelist operations

**Embedding Service:**
- ✅ ONNX BGE-small-en-v1.5 model loaded and working
- ✅ Similarity search operational
- ✅ Knowledge base restructured: ~20 prompts per category (forms, relations, valuelists)

**copilot-instructions.md:**
- ✅ Updated with action list approach
- ✅ Removed hardcoded category list
- ✅ Added Servoy basics primer
- ✅ Clear workflow: Analyze → Action List → getContextFor → Plan → Execute

### ⏳ Phase 2: Testing & Integration - IN PROGRESS

**Next Steps:**
- 🔴 Test all tools end-to-end in Servoy Developer
- 🔴 Verify getContextFor returns proper tool documentation
- 🔴 Test multi-step workflows (e.g., create form → create valuelist → bind valuelist)
- 🔴 Add error handling enhancements based on testing feedback

### 🔨 To Be Implemented

**1. ✅ Knowledge Base Restructuring - COMPLETED Nov 25, 2025**

**Old structure (caused confusion):**
```
/embeddings/
  forms.txt         (81+ full prompts - overlap with relations)
  relations.txt     (277+ full prompts - confusing similarity)
  valuelists.txt    (249+ prompts - too many)
```

**✅ New structure (simplified, ~20 prompts each):**
```
/embeddings/
  forms.txt         (21 prompts - DISTINCT: "create form", "add screen", "create page")
  relations.txt     (20 prompts - DISTINCT: "create relation", "link tables", "foreign key")
  valuelists.txt    (21 prompts - DISTINCT: "create value list", "create dropdown list")
```

**🔑 CRITICAL REQUIREMENT: Creating New Embedding Categories**

When adding new category files, **ALWAYS follow these rules:**

1. **Maximum ~20 prompts per category** (NOT 100+, NOT 200+)
2. **DISTINCT from other categories** - zero overlap with existing category prompts
3. **AI perspective** - write as AI model would phrase them (2-4 word actions)
4. **Simple action phrases** - "create button", "add label", "style component"
5. **Verb-first** - start with: create, add, make, build, design
6. **Category-specific vocabulary:**
   - FORMS: form, screen, page, interface, UI
   - RELATIONS: relation, tables, foreign key, link, database
   - VALUELISTS: value list, dropdown, lookup, selection list
   - BUTTONS: button, click element, action button
   - STYLING: style, css, appearance, color, design

**Example: BUTTONS.txt (new category)**
```
# BUTTONS category - AI perspective queries
create button
add button
make button
build button
place button
insert button
add action button
create UI button
button creation
button component
button element
clickable button
action button
command button
submit button
form button
click button
interactive button
add click element
create button element
```

**Why exactly ~20 prompts?**
- ✅ Enough variety for accurate matching
- ✅ Not overwhelming for similarity search (keeps scores meaningful)
- ✅ Forces clear, distinct vocabulary per category
- ✅ Prevents confusion between similar concepts (form vs relation)
- ✅ Easy to maintain and update

**Future categories to add:**
```
/embeddings/
  buttons.txt       (~20 prompts)
  labels.txt        (~20 prompts)
  fields.txt        (~20 prompts)
  styling.txt       (~20 prompts - applies to all components)
  calculations.txt  (~20 prompts)
  methods.txt       (~20 prompts)
  [... more as needed]
```

---

## 🔧 MAJOR REFACTORING (Nov 26, 2025)

### Problem: Poor Separation of Concerns

**Before refactoring:**
```
CommonToolHandler
  └─ queryDatabaseSchema (generic name, but relation-specific FK analysis)
  └─ getTableColumns (truly generic)

RelationToolHandler
  └─ openRelation, deleteRelation, listRelations
  └─ No FK discovery capability
```

**Issues:**
1. `queryDatabaseSchema` was doing relation-specific work (FK analysis) but living in "common" handler
2. RelationToolHandler couldn't discover relations - had to rely on user calling queryDatabaseSchema
3. Code duplication for database metadata access
4. No service layer - logic embedded in tool handlers

### Solution: Service Layer + Domain-Specific Tools

**Step 1: Created DatabaseSchemaService**
- Extracted all database metadata access into reusable service
- Methods: `getServer()`, `getTable()`, `getColumns()`, `getPrimaryKeyColumns()`, etc.
- Methods: `getExplicitForeignKeys()`, `getPotentialRelationships()`, `getIncomingForeignKeys()`
- Used by all handlers - zero duplication

**Step 2: Reorganized Tools by Domain**

**DatabaseToolHandler** (database queries only):
```java
listTables(serverName)           // List all tables
getTableInfo(serverName, table)  // Get columns + metadata
```

**RelationToolHandler** (relation-specific):
```java
openRelation(...)                // Create/open relation
deleteRelation(name)             // Delete relation
listRelations()                  // List relations
discoverRelations(serverName)    // NEW: Discover FKs for relation creation
```

**Step 3: Removed CommonToolHandler**
- Deleted entire file
- Moved `getTableColumns` → became `getTableInfo` in DatabaseToolHandler
- Moved FK discovery → became `discoverRelations` in RelationToolHandler

**Step 4: Updated All Rules Files**
- relations.md: Now uses `discoverRelations` instead of `queryDatabaseSchema`
- valuelists.md: Now uses `listTables` + `getTableInfo` from DatabaseToolHandler
- All rules simplified to ~130-160 lines

### Architecture After Refactoring

```
Service Layer:
  DatabaseSchemaService
    ├─ getServer(), getTable(), getColumns()
    ├─ getPrimaryKeyColumns(), getPrimaryKeyNames()
    └─ getExplicitForeignKeys(), getPotentialRelationships()

Tool Handlers (MCP Interface):
  DatabaseToolHandler → listTables, getTableInfo
  RelationToolHandler → openRelation, deleteRelation, listRelations, discoverRelations
  ValueListToolHandler → openValueList, deleteValueList, listValueLists
  FormToolHandler → createForm

All handlers use DatabaseSchemaService internally
```

### Benefits Achieved

✅ **Clear separation of concerns**: Database queries vs domain operations  
✅ **No code duplication**: FK analysis logic in ONE place (service layer)  
✅ **Better tool semantics**: `discoverRelations` clearly relation-specific  
✅ **Reusable service**: All handlers can access database metadata  
✅ **Scalable**: Easy to add new handlers that need database info  
✅ **Testable**: Service layer can be unit tested independently  

---

## 🎨 BOOTSTRAP COMPONENTS IMPLEMENTATION (Nov 26, 2025 PM)

### The Challenge: Safe UI Component Manipulation

**Problem:** UI components (buttons, labels, textfields, etc.) require direct manipulation of form .frm files:
- .frm files are JSON format with strict structure
- Direct file editing by AI models risks corruption
- Components need unique UUIDs, proper positioning, correct typeid/typeName
- Must maintain form hierarchy (forms exist before components)

### Solution: FormFileService + Component-Specific Rules

**Architecture:**

```
User: "Add Save button to CustomerForm"
     ↓
AI Model → addButton tool
     ↓
ComponentToolHandler
     ↓
FormFileService.addButtonToForm()
     ↓
1. Read CustomerForm.frm
2. Parse JSON safely
3. Validate structure
4. Generate UUID (com.servoy.j2db.util.UUID)
5. Create button component JSON
6. Add to items array
7. Create .frm.backup
8. Write updated JSON
     ↓
Success: Button added
```

### FormFileService - Safe File Manipulation

**Key Features:**
- JSON parsing with validation
- UUID generation using Servoy's UUID class
- Backup creation before any modification
- Structure validation (required fields, items array)
- Pretty-printing for maintainability
- Error handling and logging

**Methods:**
```java
addButtonToForm(projectPath, formName, buttonName, text, cssPosition)
validateFormFile(projectPath, formName)
getFormInfo(projectPath, formName)
```

### Component Rules Structure

**Decision: Separate file per component type**
- Each component has unique properties, methods, events
- Bootstrap package alone has 100+ component types
- Separate files prevent overwhelming rule documents

**Structure:**
```
/rules/bootstrap/
  ├─ buttons.md       ✅ (340 lines)
  ├─ labels.md        (future)
  ├─ textfields.md    (future)
  └─ [100+ more]      (future)

/embeddings/bootstrap/
  ├─ buttons.txt      ✅ (20 prompts)
  ├─ labels.txt       (future)
  └─ [100+ more]      (future)
```

### Bootstrap Button Implementation

**Component JSON Structure:**
```json
{
    "cssPosition": "39,-1,-1,60,80,30",
    "json": {
        "cssPosition": {
            "top": "39", "right": "-1", "bottom": "-1",
            "left": "60", "width": "80", "height": "30"
        },
        "text": "Button Text"
    },
    "name": "button_name",
    "typeName": "bootstrapcomponents-button",
    "typeid": 47,
    "uuid": "[AUTO-GENERATED]"
}
```

**Key Rules in buttons.md:**
```
[FORBIDDEN] NEVER manipulate .frm files directly
[FORBIDDEN] NEVER use Copilot's file editing tools on .frm files
[REQUIRED] Form must exist first
[REQUIRED] Form must use CSS positioning
[REQUIRED] Use addButton tool ONLY
```

### CSS Positioning System

**Format:** "top,right,bottom,left,width,height"
**Values:** -1 = not set, positive numbers = pixels

**Examples:**
```
"39,-1,-1,60,80,30"   → Top-left, 39px from top, 60px from left
"39,60,-1,-1,80,30"   → Top-right, 39px from top, 60px from right
"39,10,-1,10,-1,30"   → Full-width with 10px margins
```

### Expansion Strategy

**Adding new bootstrap components:**
1. Add method to FormFileService (e.g., `addLabelToForm()`)
2. Create `/rules/bootstrap/[component].md` with component specifics
3. Create `/embeddings/bootstrap/[component].txt` (20 prompts)
4. Add tool to ComponentToolHandler
5. Document typeName, typeid, required properties

**Benefits:**
- ✅ Safe file manipulation (no corruption risk)
- ✅ Component-specific documentation
- ✅ Scalable to 100+ component types
- ✅ Consistent validation and backup strategy
- ✅ Real UUID generation
- ✅ Clear separation: one file per component type

---

## 🔧 TESTING PHASE ENHANCEMENTS (Nov 26, 2025 PM)

### Issues Discovered During Testing

#### Issue 1: Parameter Guessing Behavior
**Problem:** Model was calling `listTables` multiple times with guessed serverName values instead of asking user.

**Example of Wrong Behavior:**
```
Model: [silently calls listTables(serverName="example_data")] - Failed
Model: [silently calls listTables(serverName="db")] - Failed  
Model: [silently calls listTables(serverName="servoy")] - Failed
Model: [silently calls listTables(serverName="main")] - Failed
User: "Why did it fail?"
```

**Solution Implemented:**
1. Added **RULE 2: WHEN TOOL REQUIRES PARAMETER** to copilot-instructions.md
2. Added prominent **CRITICAL: DATABASE SERVER NAME** warnings to relations.md and valuelists.md
3. Added tool-level reminders in `discoverRelations`, `listTables`, `getTableInfo` documentation
4. Multiple examples showing WRONG vs CORRECT behavior

#### Issue 2: Lack of Tool Call Transparency
**Problem:** Model was calling tools without announcing parameter values to user, leading to confusion about what was being attempted.

**Solution Implemented:**
1. Added **RULE 4: TOOL CALL TRANSPARENCY** to copilot-instructions.md
2. Requires models to announce tool name and all parameter values BEFORE calling
3. Requires reporting success/failure clearly AFTER tool returns
4. Provides concrete examples for all tool types

#### Issue 3: Not Respecting User Cancellations
**Problem:** When user said "I don't know" or "Cancel", model tried alternative approaches instead of stopping.

**Solution Implemented:**
1. Added **RULE 1: WHEN USER CANCELS OR DECLINES** to copilot-instructions.md
2. Explicit list of FORBIDDEN behaviors (5 items)
3. Explicit list of REQUIRED behaviors (4 items)
4. Clear examples showing STOP requirement

#### Issue 4: Graphical Symbols Unreliable
**Problem:** Emoji symbols (🚨, ⛔, ❌, ✅) might not be reliably interpreted by all AI models.

**Solution Implemented:**
1. Replaced ALL emoji and graphical symbols with text-based markers
2. New markers: `[FORBIDDEN]`, `[REQUIRED]`, `[WRONG]`, `[CORRECT]`
3. Applied consistently across all files:
   - copilot-instructions.md
   - relations.md
   - valuelists.md
4. Text-based separators: `=============================================================================`

### Current Rule Enforcement Structure

**copilot-instructions.md** (~115 lines of CRITICAL RULES at top):
```
=============================================================================
=== CRITICAL RULES - READ THIS FIRST - THESE ARE ABSOLUTE REQUIREMENTS ===
=============================================================================

## RULE 1: WHEN USER CANCELS OR DECLINES
  [FORBIDDEN] × 5 explicit prohibitions
  [REQUIRED] × 4 explicit requirements

## RULE 2: WHEN TOOL REQUIRES PARAMETER
  [FORBIDDEN] × 4 explicit prohibitions
  [REQUIRED] × 4 explicit requirements

## RULE 3: RESPECT USER INTENT
  Clear STOP requirements

## RULE 4: TOOL CALL TRANSPARENCY
  [REQUIRED] × 6 explicit requirements
  Examples for all tool types

=============================================================================
=== END OF CRITICAL RULES ===
=============================================================================
```

**relations.md & valuelists.md** (domain-specific warnings):
```
## CRITICAL: DATABASE SERVER NAME
  [FORBIDDEN] NEVER guess server names
  [FORBIDDEN] NEVER try multiple server names
  [FORBIDDEN] DO NOT proceed without parameter
  [REQUIRED] STOP and ASK THE USER explicitly
```

**Tool-level reminders** in each affected tool:
```
- serverName (required): ... - [FORBIDDEN] NEVER guess this, [REQUIRED] always ASK...
```

### Text-Based Formatting System

All instruction files now use consistent text-based markers:

| Marker | Meaning | Usage |
|--------|---------|-------|
| `[FORBIDDEN]` | Absolutely prohibited | Rule violations |
| `[REQUIRED]` | Must do this | Required behaviors |
| `[WRONG]` | Incorrect example | Teaching examples |
| `[CORRECT]` | Correct example | Teaching examples |
| `>>` | Direction indicator | "If X >> STOP" |
| `>>>` | Strong emphasis | ">>> STOP HERE" |
| `<<` | Failed result | "Failed <<" |
| `=====` | Section separator | Headers |

### Benefits Achieved

✅ **Universal Understanding**: Text markers work with all AI models  
✅ **Clear Semantics**: No ambiguity about forbidden vs required  
✅ **Four-Layer Protection**: Global rules + domain rules + tool rules + examples  
✅ **Explicit Examples**: Shows WRONG vs CORRECT behavior for every scenario  
✅ **Future-Proof**: No dependency on emoji rendering  

---

## 🔴 TODO - Next Testing Phase

**Problem:** AI calls `queryDatabaseSchema` instead of using Servoy tools after `getContextFor` succeeds.

**Root Cause:** `getContextFor` returns placeholder message:
```
"(Note: Full tool definitions, rules, and examples will be added in Phase 3)
For now, showing matched categories to verify action list approach works."
```

AI sees this, thinks "not enough info", falls back to other tools.

**Evidence from Test:**
- ✅ Action list generation works: `["create form", "add buttons", "style buttons"]`
- ✅ `getContextFor` called successfully
- ✅ Similarity search works perfectly: "create form" → FORMS (100% match!)
- ✅ No confusion with RELATIONS (old problem solved!)
- ❌ But AI still calls `queryDatabaseSchema` because response lacks real tool definitions

**Console Output:**
```
[McpServletProvider] Matched: FORMS (score: 1.0)
[McpServletProvider] Matched: FORMS (score: 0.9731291504365036)
[McpServletProvider] Matched: FORMS (score: 0.9639178795981753)
[McpServletProvider] Returning context for 2 categories
→ AI receives placeholder message
→ AI calls queryDatabaseSchema (wrong!)
```

### TODO #1: Add Real Tool Context to getContextFor Response

**Location:** `McpServletProvider.java::handleGetContextFor()` around line 270

**Current (Placeholder):**
```java
response.append("(Note: Full tool definitions, rules, and examples will be added in Phase 3)\n");
response.append("For now, showing matched categories to verify action list approach works.\n\n");

for (CategoryMatch match : categoryMatches.values()) {
    response.append("Category: ").append(match.category).append("\n");
    response.append("  - Tools will be defined here\n");
    response.append("  - Rules and requirements\n");
    response.append("  - Parameter specifications\n");
    response.append("  - Usage examples\n\n");
}
```

**Need to Replace With:**

For each matched category, return actual tool information:

**FORMS Category:**
```
=== FORMS Category ===

Available Tools:
1. createForm
   - Required: name (string)
   - Optional: width (int, default 640), height (int, default 480)
   - Optional: style (string: 'css' or 'responsive', default 'css')
   - Optional: dataSource (string, format: 'db:/server_name/table_name')
   
   Usage: createForm(name="OrderForm", width=800, height=600)

Rules:
- Forms are UI screens/pages in Servoy
- Default size: 640x480 pixels
- CSS forms: Use CSS positioning
- Responsive forms: Use responsive layout
- Forms must be created BEFORE adding UI components (buttons, labels) to them

Examples:
- createForm(name="CustomerList")
- createForm(name="OrderEntry", width=1024, height=768, style="responsive")
- createForm(name="Products", dataSource="db:/example_data/products")
```

**VALUELISTS Category:**
```
=== VALUELISTS Category ===

Available Tools:
1. openValueList
   - Required: name (string)
   - Optional: values (array of strings)
   
   Usage: openValueList(name="StatusList", values=["Active", "Inactive", "Pending"])

Rules:
- ValueLists provide data for dropdown/combobox components
- Can be database-driven or static values
- If valuelist exists: Opens it for editing
- If doesn't exist: Creates new valuelist

Examples:
- openValueList(name="Priorities", values=["High", "Medium", "Low"])
- openValueList(name="CountriesList")
```

**BUTTONS Category (when implemented):**
```
=== BUTTONS Category ===

Available Tools:
1. addButton (NOT YET IMPLEMENTED)
   - Required: formName (string), buttonName (string)
   - Optional: x (int), y (int), width (int), height (int)
   - Optional: action (string)
   
   Usage: addButton(formName="OrderForm", buttonName="Save", x=10, y=10)

Rules:
- Buttons must be added to existing forms
- Form must exist first (create form before adding buttons)
- Positioning: x,y coordinates or CSS positioning
- Actions: onAction event handler

Note: This tool is not yet implemented. For now, you can create the form and inform the user 
that button functionality is coming soon.
```

**Implementation Strategy:**
1. Create method `getCategoryContext(String category)` that returns formatted string
2. For each known category (FORMS, RELATIONS, VALUELISTS), return real tool info
3. For unknown/unimplemented categories, return "NOT YET IMPLEMENTED" message
4. Call this method in the loop instead of placeholder

### TODO #2: Handle "Category Not Yet Implemented" Scenario

When similarity search matches a category that doesn't have tools yet (e.g., BUTTONS, STYLING):

**Scenario 1: Partial Match (some categories implemented, some not)**
```
User: "Create form with 3 buttons styled blue"
Matches: FORMS (implemented), BUTTONS (not yet), STYLING (not yet)

Response should include:
✅ FORMS - Full tool context (createForm with all parameters)
⚠️ BUTTONS - NOT YET IMPLEMENTED: "Button functionality is planned. For now, create the form 
   and inform user that button creation will be available in a future update."
⚠️ STYLING - NOT YET IMPLEMENTED: "Styling/CSS functionality is planned. For now, create 
   basic elements and inform user that styling customization will be available in a future update."

AI should:
1. Create the form using createForm tool
2. Tell user: "I've created the form 'X'. Button creation and styling are not yet implemented 
   but are planned for future updates."
```

**Scenario 2: No Matches Implemented**
```
User: "Add 3 buttons with blue style"
Matches: BUTTONS (not yet), STYLING (not yet)

Response:
⚠️ NO IMPLEMENTED TOOLS FOR YOUR REQUEST

Your queries matched these categories:
- BUTTONS (not yet implemented)
- STYLING (not yet implemented)

These features are planned but not available yet. Please try requests related to:
✅ Forms (create, edit forms)
✅ Relations (database table relationships)
✅ ValueLists (dropdown data sources)
```

**Implementation:**
```java
private String getCategoryContext(String category) {
    switch (category) {
        case "FORMS":
            return getFormsContext();
        case "RELATIONS":
            return getRelationsContext();
        case "VALUELISTS":
            return getValueListsContext();
        case "BUTTONS":
        case "STYLING":
        case "CALCULATIONS":
        case "METHODS":
            return getNotYetImplementedContext(category);
        default:
            return "⚠️ Unknown category: " + category;
    }
}

private String getNotYetImplementedContext(String category) {
    return "=== " + category + " Category ===\n\n" +
           "⚠️ NOT YET IMPLEMENTED\n\n" +
           "This category was matched by similarity search, but tools for " + category + 
           " are not yet available.\n" +
           "This feature is planned for future implementation.\n\n" +
           "For now, inform the user that this functionality is coming soon.\n";
}
```

### Testing Checklist for Tomorrow:

- [ ] Add `getCategoryContext()` method with real tool definitions
- [ ] Test: "Create form Orders" → Should return FORMS context with createForm tool
- [ ] Test: "Create form with buttons" → Should return FORMS + "BUTTONS not yet implemented"
- [ ] Test: AI uses createForm tool (not queryDatabaseSchema!)
- [ ] Test: AI tells user about unimplemented features gracefully
- [ ] Update this guide with results

---

**2. getContextFor Tool - IMPLEMENTED Nov 25, 2025**

```java
/**
 * Receives action list from model, does similarity search per action,
 * returns aggregated context.
 * 
 * Input: { "queries": ["create form", "add buttons", "create relation"] }
 * Output: Aggregated context with tools, rules, examples for matched categories
 */
private McpSchema.CallToolResult handleGetContextFor(
    McpSyncServerExchange exchange, 
    McpSchema.CallToolRequest request) {
    
    // Extract queries array
    List<String> queries = extractQueries(request);
    
    // For each query, do similarity search in knowledge base
    Map<String, CategoryContext> contexts = new HashMap<>();
    for (String query : queries) {
        List<SearchResult> matches = embeddingService.search(query, 3);
        for (SearchResult match : matches) {
            String category = match.metadata.get("category");
            if (!contexts.containsKey(category)) {
                contexts.put(category, loadCategoryContext(category));
            }
        }
    }
    
    // Aggregate and return context
    String enrichedContext = aggregateContexts(contexts.values());
    return McpSchema.CallToolResult.builder()
        .content(List.of(new TextContent(enrichedContext)))
        .build();
}
```

**3. Category Context Files**

For each category, create detailed context file:
```
/rules/
  FORMS/
    tools.md          (createForm tool definition)
    rules.md          (form creation rules, hierarchy)
    examples.md       (5-10 examples)
  BUTTONS/
    tools.md          (addButton tool definition)
    rules.md          (button placement rules)
    examples.md
  [... etc]
```

**4. Update PromptEnricher**

Refactor to handle action list queries instead of full prompt intent detection.

---

## Critical Concerns & Solutions

### Concern 1: Similarity Search Confusion with Scale

**Problem:** As categories grow to 100+, similarity search on full prompts gets confused.

**Solution:** 
- Model generates focused action list (2-4 word phrases)
- Similarity search on EACH action separately
- Each knowledge file has 20-30 phrase variations (manageable, distinct)
- Multiple matches per action = OK (return multiple contexts)

### Concern 2: Unpredictable User Phrasing

**Problem:** Can't anticipate every way users will phrase requests.

**Solution:**
- Don't try! Let the model normalize user language
- Model uses general AI knowledge to map user intent → standard actions
- Example: "I need a screen" → model generates "create form"
- Similarity search matches "create form" in FORMS.txt knowledge base

### Concern 3: Category Ambiguity

**Problem:** "picker" could mean valuelist picker or color picker.

**Solution:**
- Use 2-4 word phrases, not single words
- "create value list picker" vs "add color picker component"
- More context in query = better matching
- Multiple matches = OK, return multiple contexts, model decides

### Concern 4: Context Window Size

**Problem:** With 100+ categories, can't send all documentation every time.

**Solution:**
- **Action list filters context retrieval**
- Only matched categories return documentation
- Typical request: 3-6 actions → 3-6 category docs (not 100+)
- Each category doc: 1-2 pages (tools + rules + examples)
- Total context: 5-15 pages vs 200+ pages if sending everything

---

## Migration Path

### Phase 1: Knowledge Base Restructuring ⏳
1. Create `/knowledge/` directory structure
2. For each major category, create knowledge file with 20-30 action phrases
3. Start with: FORMS, BUTTONS, RELATIONS, VALUELISTS, COMBOBOX
4. Test similarity search with new structure

### Phase 2: Implement getContextFor Tool ⏳
1. Create `getContextFor` handler in `McpServletProvider`
2. Input: Array of action queries
3. Output: Aggregated context from matched categories
4. Keep `processPrompt` as fallback during transition

### Phase 3: Create Category Context Files ⏳
1. For each category, create `/rules/CATEGORY/` folder
2. Add: tools.md, rules.md, examples.md
3. Define tools with clear parameter specifications
4. Document hierarchy and dependencies

### Phase 4: Update Existing Handlers ⏳
1. Ensure all handlers work with new context format
2. Test multi-step operations with hierarchy
3. Validate parameter extraction and error handling

### Phase 5: Testing & Refinement ⏳
1. Test with diverse user prompts
2. Monitor similarity search accuracy
3. Adjust knowledge base phrases as needed
4. Add more categories incrementally

### Phase 6: Deprecate Old System ⏳
1. Remove `processPrompt` tool
2. Remove old `/embeddings/` training files
3. Clean up intent detection code
4. Update all documentation

---

## Next Steps (Immediate Actions)

### 1. Create Knowledge Base Files
**Priority: HIGH**

Start with top 5 categories:

**FORMS.txt:**
```
# Category: FORMS
create form
make form
add form
new form
build form
form creation
create screen
make screen
add page
build page
design form
form design
create UI screen
make UI page
...
```

**BUTTONS.txt:**
```
# Category: BUTTONS
add button
add buttons
create button
place button
button on form
insert button
make button
build button
add UI button
create UI button
button component
...
```

Similar files for: RELATIONS, VALUELISTS, COMBOBOX

### 2. Implement getContextFor Tool
**Priority: HIGH**

Location: `McpServletProvider.java`

```java
// Register getContextFor tool
Tool getContextForTool = McpSchema.Tool.builder()
    .inputSchema(new JsonSchema("object", null, null, null, null, null))
    .name("getContextFor")
    .description("Retrieves Servoy documentation and tools for specified action queries. " +
                 "Input: queries (array of strings) - action phrases like 'create form', 'add buttons'")
    .build();

SyncToolSpecification getContextForSpec = SyncToolSpecification.builder()
    .tool(getContextForTool)
    .callHandler(this::handleGetContextFor)
    .build();
    
server.addTool(getContextForSpec);
```

### 3. Test Action List Approach
**Priority: MEDIUM**

Test prompts:
- "Create a form called Orders"
- "Form with 2 buttons and dropdown"
- "Create relation between orders and customers"
- Complex: "Form with Save/Cancel buttons, customer dropdown from database, and totals calculation"

Verify:
- Model generates correct action list
- Similarity search finds right categories
- Context aggregation works
- Model creates proper execution plan

### 4. Create Category Context Structure
**Priority: MEDIUM**

For each category:
```
/rules/
  FORMS/
    tools.md          # createForm, editForm definitions
    rules.md          # CSS vs responsive, hierarchy, defaults
    examples.md       # 5-10 creation examples
  BUTTONS/
    tools.md          # addButton, removeButton definitions
    rules.md          # Placement on forms, event handling
    examples.md
```

### 5. Monitor and Iterate
**Priority: ONGOING**

- Track similarity search accuracy
- Log which actions match which categories
- Identify missing categories
- Refine knowledge base phrases
- Update copilot-instructions.md as needed

---

## Code Patterns (Existing System)

### Adding a New Tool Handler

1. Create handler class implementing `IToolHandler` in `handlers/` package
2. Implement `registerTools()` method to register tools:
```java
@Override
public void registerTools(McpSyncServer server) {
    ToolHandlerRegistry.registerTool(
        server,
        "toolName",
        "Tool description with parameters",
        this::handleToolName);
}
```

3. Implement handler method:
```java
private McpSchema.CallToolResult handleToolName(
    McpSyncServerExchange exchange, 
    McpSchema.CallToolRequest request) {
    // Extract parameters from request.arguments()
    // Execute logic (use Display.getDefault().syncExec() for UI operations)
    // Return CallToolResult with TextContent
}
```

4. Add handler to `ToolHandlerRegistry.getHandlers()` array

### Working with Servoy APIs

**Form Creation:**
- Get active project: `ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject()`
- Create form: `solution.createNewForm(validator, style, name, dataSource, showInMenu, size)`
- Add parts: `form.createNewPart(Part.BODY, height)`
- Enable CSS: `form.setUseCssPosition(Boolean.TRUE)`
- Save: `servoyProject.saveEditingSolutionNodes(new IPersist[] { form }, true)`
- Open editor: `EditorUtil.openFormDesignEditor(form, true, true)`

**Validation:**
- Use `ScriptNameValidator` for form/variable names
- Check active project exists before operations

**UI Thread:**
- All Eclipse UI operations must run on Display thread
- Use `Display.getDefault().syncExec(() -> { ... })` for synchronous
- Use `Display.getDefault().asyncExec(() -> { ... })` for asynchronous

## Key Classes & Locations

### Servoy Eclipse (aibridge project)
- `com.servoy.eclipse.core.ServoyModelManager` - Access to Servoy model
- `com.servoy.eclipse.model.nature.ServoyProject` - Project wrapper
- `com.servoy.eclipse.ui.util.EditorUtil` - Editor utilities

### Servoy Client (persistence)
- `com.servoy.j2db.persistence.Solution` - Solution operations
- `com.servoy.j2db.persistence.Form` - Form definition
- `com.servoy.j2db.persistence.Part` - Form parts (BODY, HEADER, etc.)
- `com.servoy.j2db.persistence.Relation` - Relation definition
- `com.servoy.j2db.persistence.IValidateName` - Name validation interface
- `com.servoy.j2db.persistence.ScriptNameValidator` - Name validator implementation

### Additional Resources
- Servoy Eclipse: `/Users/marianvid/Servoy/git/master/servoy-eclipse/`
- Servoy Client: `/Users/marianvid/Servoy/git/master/servoy-client/`
- Reference wizards: `com.servoy.eclipse.ui.wizards.NewFormWizard`

## Implementation Notes

- **Form defaults based on NewFormWizard.java analysis:**
  - Default dimensions: 640x480 pixels
  - Default style: CSS-positioned (not responsive)
  - Body part height: 480 pixels
  - CSS positioning: Enabled by default
  
- **Error handling:**
  - Use `ServoyLog.logError()` for logging
  - Return `CallToolResult` with `isError(true)` for errors
  - Include descriptive error messages in TextContent

- **Threading:**
  - MCP handlers run on separate thread
  - Must use Display.syncExec/asyncExec for Servoy/Eclipse API calls
  - Store results in array for cross-thread communication

---

## Reference Documentation

### Key Files
- **copilot-instructions.md** - Instructions sent to AI model (action list approach)
- **action-list-approach-summary.md** - Detailed explanation of new architecture
- **intent-detection-improvements.md** - Analysis of old approach issues (Nov 25, 2025)

### Architecture Decisions

**Why Action List Approach?**
1. Leverages LLM's natural language understanding (don't fight it)
2. Scalable to 1000+ categories without confusion
3. Focused similarity search (per action, not mixed prompts)
4. Auto-discovered categories (no hardcoded lists)
5. Works with any LLM model

**Why 2-4 Word Phrases?**
1. More context than single words ("valuelist" vs "create value list")
2. Avoids ambiguity (color picker vs valuelist picker)
3. Natural for similarity matching
4. Easy for model to generate

**Why One Line Per Action Type?**
1. Avoids duplicate similarity searches
2. Model knows "add buttons" = potentially multiple buttons
3. Cleaner action list
4. More efficient

**Why Maximum ~20 Prompts Per Category?**
1. **Proven effective** - Nov 25, 2025: Reduced forms.txt from 81→21, relations.txt from 277→20, valuelists.txt from 249→21
2. **Prevents overlap** - Forces clear, distinct vocabulary per category
3. **Better similarity scores** - Fewer prompts = more meaningful match scores
4. **Easy to maintain** - Can review entire file quickly
5. **Faster similarity search** - Less data to process per category
6. **Avoids confusion** - Clear separation: "create form" vs "create relation" vs "create value list"

**What Happens with Too Many Prompts:**
- ❌ Similarity scores lose meaning (everything matches at 70-90%)
- ❌ Categories start overlapping (forms match relations at 86%)
- ❌ Search becomes slow
- ❌ Maintenance nightmare
- ❌ Hard to identify which prompts cause confusion

### Future Considerations

**When to Add New Category:**
- User requests not matching existing categories
- New Servoy features released
- Repeated similar requests need their own category

**How to Add New Category:**
1. Create `/embeddings/NEWCATEGORY.txt`
2. Write exactly ~20 DISTINCT prompts (AI perspective, 2-4 words)
3. Ensure NO overlap with existing categories
4. Add to `embeddings.list`
5. Restart embedding service to load
6. Test with sample queries

**Knowledge Base Maintenance:**
- Monitor similarity search logs
- Add phrases that users commonly use
- Remove redundant/confusing phrases
- Keep files under 50 lines

**Performance Optimization:**
- Cache category contexts (don't reload every time)
- Pre-compute embeddings at startup
- Batch similarity searches if possible
- Monitor response times

---

## Troubleshooting

### Issue: Wrong Category Matched

**Symptom:** Action "create dropdown" matches BUTTONS instead of VALUELISTS

**Solution:**
1. Check knowledge base files - ensure distinct phrases
2. Add more specific phrases: "create value list", "dropdown data source"
3. Review similarity scores in debug logs
4. May need multiple matches (both VALUELISTS + UI_COMPONENTS)

### Issue: No Category Matched

**Symptom:** Action returns no matches or PASS_THROUGH

**Solution:**
1. Check if category exists in knowledge base
2. Add new knowledge file for missing category
3. Verify embedding service is working
4. Check similarity threshold (currently 0.7)

### Issue: Too Many Categories Returned

**Symptom:** Action matches 10+ categories, context too large

**Solution:**
1. Refine action phrases to be more specific
2. Adjust similarity threshold higher (0.75 or 0.8)
3. Limit results per query (currently maxResults=3)
4. Review knowledge base for overly generic phrases

### Issue: Model Not Generating Action List

**Symptom:** Model calls wrong tools or doesn't follow workflow

**Solution:**
1. Review copilot-instructions.md clarity
2. Check if model has access to instructions
3. Test with explicit prompt: "Generate action list for: [user request]"
4. May need stronger emphasis on workflow steps

---

## Testing Checklist

### Basic Functionality
- [ ] Single action: "Create form Orders"
- [ ] Multiple actions: "Form with buttons and dropdown"
- [ ] Complex request: Multi-step with hierarchy
- [ ] Non-Servoy request: Should reject gracefully

### Similarity Matching
- [ ] Forms actions match FORMS category
- [ ] Button actions match BUTTONS category
- [ ] Relation actions match RELATIONS category
- [ ] Valuelist actions match VALUELISTS category
- [ ] Ambiguous actions return multiple categories (OK)

### Context Retrieval
- [ ] getContextFor returns aggregated context
- [ ] Context includes tools, rules, examples
- [ ] Context size manageable (not 100+ pages)
- [ ] Multiple queries work (3-6 actions)

### Execution
- [ ] Model creates execution plan
- [ ] Model asks for missing parameters
- [ ] Model respects hierarchy (forms before buttons)
- [ ] Tools execute in correct order
- [ ] Error handling works

### Edge Cases
- [ ] Very long user prompt (10+ actions)
- [ ] Misspelled action phrases
- [ ] Completely unknown actions
- [ ] Multiple LLM models (Claude, GPT, etc.)

---

## COMPREHENSIVE SYSTEM SUMMARY

### All Implemented Tools (11 Total)

#### DatabaseToolHandler (2 tools)
1. **listTables(serverName)** - Lists all tables in a database server
   - Status: ✅ Implemented and tested
   - Rule protection: [FORBIDDEN] parameter guessing
   
2. **getTableInfo(serverName, tableName)** - Gets comprehensive table information
   - Status: ✅ Implemented and tested
   - Returns: Columns with names, types, PK status
   - Rule protection: [FORBIDDEN] parameter guessing

#### RelationToolHandler (4 tools)
3. **openRelation(name, primaryDataSource, foreignDataSource, primaryColumn, foreignColumn)** - Creates or opens relation
   - Status: ✅ Implemented and tested
   
4. **deleteRelation(name)** - Deletes a relation
   - Status: ✅ Implemented and tested
   
5. **listRelations()** - Lists all relations in current project
   - Status: ✅ Implemented and tested
   
6. **discoverRelations(serverName)** - Discovers potential relations by analyzing FKs
   - Status: ✅ Implemented and tested
   - Returns: EXPLICIT FKs + POTENTIAL relations
   - Rule protection: [FORBIDDEN] parameter guessing

#### ValueListToolHandler (3 tools)
7. **openValueList(name, customValues OR dataSource+displayColumn+returnColumn)** - Creates or opens valuelist
   - Status: ✅ Implemented and tested
   - Supports: Custom values (static) OR database-based
   
8. **deleteValueList(name)** - Deletes a valuelist
   - Status: ✅ Implemented and tested
   
9. **listValueLists()** - Lists all valuelists in current project
   - Status: ✅ Implemented and tested

#### FormToolHandler (1 tool)
10. **createForm(name, width, height, style, dataSource)** - Creates a new form
    - Status: ✅ Implemented and tested
    - Supports: CSS and responsive layouts

#### ComponentToolHandler (1 tool)
11. **addButton(formName, buttonName, text, cssPosition)** - Adds bootstrap button to form
    - Status: ✅ Implemented and tested
    - Uses: FormFileService for safe .frm file manipulation
    - Generates: Real UUIDs using com.servoy.j2db.util.UUID
    - Creates: Backup before modifications

### Service Layer

**DatabaseSchemaService** - Shared service used by all handlers
- Methods: getServer(), getTable(), getColumns(), getPrimaryKeyColumns(), getPrimaryKeyNames()
- Methods: getExplicitForeignKeys(), getPotentialRelationships(), getIncomingForeignKeys()
- Status: ✅ Fully implemented and tested
- Benefits: Zero code duplication, consistent database access

**FormFileService** - Safe form file manipulation
- Methods: addButtonToForm(), validateFormFile(), getFormInfo()
- Features: JSON parsing, UUID generation, backup creation, validation
- Status: ✅ Fully implemented and tested
- Benefits: Prevents .frm file corruption, safe component addition

### Documentation Files

#### copilot-instructions.md (~250 lines)
- Status: ✅ Complete with CRITICAL RULES section (~115 lines)
- Format: Text-based markers ([FORBIDDEN], [REQUIRED])
- Contains: 4 major rules with examples

#### Category Rules Files
1. **relations.md** (131 lines)
   - Status: ✅ Complete with critical warnings
   - Tools documented: 4 relation tools + database tools reference
   
2. **valuelists.md** (155 lines)
   - Status: ✅ Complete with critical warnings
   - Tools documented: 3 valuelist tools + database tools reference
   
3. **forms.md** (135 lines)
   - Status: ✅ Complete
   - Tools documented: 1 form tool

4. **bootstrap/buttons.md** (340 lines) - **NEW**
   - Status: ✅ Complete
   - Tools documented: 1 component tool (addButton)
   - Includes: CSS positioning guide, JSON structure, examples

#### Embedding Files
- forms.txt (21 prompts)
- relations.txt (20 prompts)
- valuelists.txt (21 prompts)
- bootstrap/buttons.txt (20 prompts) - **NEW**
- Status: ✅ Optimized for similarity matching

### Rule Enforcement System

**Four-Layer Protection:**

1. **Global Rules** (copilot-instructions.md):
   - RULE 1: User cancellation respect
   - RULE 2: Parameter requirement enforcement
   - RULE 3: User intent respect
   - RULE 4: Tool call transparency

2. **Domain Rules** (relations.md, valuelists.md):
   - CRITICAL: DATABASE SERVER NAME warnings
   - Prominent placement at top of each file

3. **Tool-Level Reminders**:
   - In each tool's parameter documentation
   - Format: `[FORBIDDEN] NEVER guess this, [REQUIRED] always ASK...`

4. **Examples**:
   - WRONG vs CORRECT behavior shown for every scenario
   - Concrete examples of what NOT to do

### Text-Based Formatting Standard

**Replaced all graphical symbols with text markers:**
- `[FORBIDDEN]` - Prohibited behavior
- `[REQUIRED]` - Required behavior
- `[WRONG]` - Incorrect example
- `[CORRECT]` - Correct example
- `>>` - Direction indicator
- `>>>` - Strong emphasis
- `=====` - Section separators

**Applied to:** All instruction files (copilot-instructions.md, relations.md, valuelists.md)

### Architecture Benefits

✅ **Clean separation**: Database queries vs domain operations  
✅ **No duplication**: FK analysis in one place (service layer)  
✅ **Scalable**: Easy to add new handlers  
✅ **Testable**: Service layer independently testable  
✅ **Clear semantics**: Tool names reflect their purpose  
✅ **Rule enforcement**: Multiple layers prevent common errors  
✅ **Model-agnostic**: Text-based formatting works with any AI model  

### Known Limitations

**Not Yet Implemented:**
- Adding UI components to forms (buttons, labels, fields)
- Form editing (only creation supported)
- Multi-column value lists
- Global method value lists
- Related value lists
- Calculation and method management

**Testing Notes:**
- Models may still attempt parameter guessing despite rules
- User cancellation handling requires explicit instruction following
- Tool call transparency depends on model cooperation

---

## Version History

**November 26, 2025 - Bootstrap UI Components Implementation**
- Created FormFileService for safe .frm file manipulation
- Created ComponentToolHandler for bootstrap component operations
- Implemented addButton tool with full validation
- Created /rules/bootstrap/buttons.md (340 lines)
- Created /embeddings/bootstrap/buttons.txt (20 prompts)
- UUID generation using com.servoy.j2db.util.UUID
- Backup strategy for all file modifications
- System now supports: forms + buttons workflow

**November 26, 2025 - Testing Phase & Rule Enforcement**
- Added 4 CRITICAL RULES to copilot-instructions.md
- Replaced all emoji/symbols with text-based markers
- Added tool call transparency requirements
- Enhanced parameter guessing prevention
- Added multiple testing phase fixes

**November 26, 2025 - Architecture Refactoring**
- Created DatabaseSchemaService
- Removed CommonToolHandler
- Created DatabaseToolHandler
- Added listTables and getTableInfo tools
- Moved FK discovery to RelationToolHandler
- Simplified all rules files

**November 25, 2025 - Major Architecture Change**
- Discovered intent detection confusion with scale
- Designed action list approach
- Updated copilot-instructions.md
- Documented migration path
- Added debug logging throughout

**Previous (Pre-Nov 25, 2025)**
- Initial MCP server implementation
- ONNX embedding service integration
- FormToolHandler, RelationToolHandler, ValueListToolHandler
- Intent detection with training examples (deprecated approach)
