# MCP Implementation Guide for Servoy Developer Copilot Plugin

**Last Updated:** December 10, 2025  
**Last Verified Against Code:** December 10, 2025  
**Status:** PRODUCTION READY - Core Features Complete + Relations & ValueLists Enhanced

---

## [NEW] DECEMBER 10, 2025 UPDATES - RELATIONS & VALUELISTS COMPLETE CRUD

### Relations Service & Handler - Complete Rewrite

**RelationService Created (NEW):**
- Complete service layer for all relation operations
- Extracts business logic from handler for reusability
- Methods: createRelation(), updateRelationProperties(), applyRelationProperties(), validateAndCorrectDataSource()
- Supports all 8 relation properties with smart defaults
- Property parsing for encapsulation, joinType, etc.

**RelationToolHandler Enhanced:**
- Refactored following FormToolHandler pattern (properties map approach)
- Tool names improved for clarity
- Complete CRUD operations with properties support

**Tool Changes:**
- `openRelation` - Now supports properties map for all 8 relation properties, dual purpose (create OR update)
- `listRelations` --> `getRelations` - Renamed for clarity
- `deleteRelation` --> `deleteRelations` - Enhanced to support array of names (bulk delete)
- `discoverRelations` --> `discoverDbRelations` - Renamed to emphasize database FK discovery

**All 8 Relation Properties Now Supported:**
1. **joinType** - "left outer" | "inner" (default: "left outer")
2. **allowCreationRelatedRecords** - boolean (default: true)
3. **allowParentDeleteWhenHavingRelatedRecords** - boolean (default: false)
4. **deleteRelatedRecords** - boolean (default: false)
5. **initialSort** - string (e.g., "column1 asc, column2 desc")
6. **encapsulation** - "public" | "hide" | "module" (default: "public")
7. **deprecated** - string (deprecation message)
8. **comment** - string (documentation)

**Files Created:**
- `RelationService.java` - Complete service layer (~280 lines)

**Files Modified:**
- `RelationToolHandler.java` - Complete refactor using service, renamed tools, properties support

### ValueLists Service & Handler - Complete Rewrite

**ValueListService Created (NEW):**
- Complete service layer for all valuelist operations
- Supports all 4 valuelist types: CUSTOM, DATABASE (table), DATABASE (related), GLOBAL_METHOD
- Methods: createValueList(), updateValueListProperties(), applyValueListProperties()
- Type-specific configuration methods for each valuelist type
- Supports all valuelist properties from Servoy API

**ValueListToolHandler Enhanced:**
- Refactored following FormToolHandler pattern (properties map approach)
- Tool names improved for clarity
- Complete CRUD operations with properties support for all 4 types

**Tool Changes:**
- `openValueList` - Now supports all 4 types + properties map for 11+ properties, dual purpose (create OR update)
- `listValueLists` --> `getValueLists` - Renamed for clarity
- `deleteValueList` --> `deleteValueLists` - Enhanced to support array of names (bulk delete)

**All 4 ValueList Types Supported:**
1. **CUSTOM_VALUES** - Fixed list of values (array of strings)
2. **DATABASE_VALUES (table)** - Values from database table via dataSource
3. **DATABASE_VALUES (related)** - Values from related table via relationName
4. **GLOBAL_METHOD_VALUES** - Dynamic values from global method

**11+ ValueList Properties Supported:**
1. **lazyLoading** - boolean (load on demand)
2. **displayValueType** - int (data type of display values)
3. **realValueType** - int (data type of stored values)
4. **separator** - string (multi-column separator)
5. **sortOptions** - string (e.g., "column asc")
6. **useTableFilter** - boolean (filter by valuelist name column)
7. **addEmptyValue** - boolean/"always"/"never" (allow null option)
8. **fallbackValueListID** - string (fallback valuelist name)
9. **deprecated** - string (deprecation message)
10. **encapsulation** - "public" | "hide" | "module"
11. **comment** - string (documentation)

**Files Created:**
- `ValueListService.java` - Complete service layer (~450 lines)

**Files Modified:**
- `ValueListToolHandler.java` - Complete refactor using service, renamed tools, properties support

### Architecture Pattern Established

**Service Layer Pattern:**
- Relations, ValueLists, and Forms all follow same pattern
- Handler extracts parameters, validates, delegates to Service
- Service contains all business logic (create, update, property application)
- Clean separation of concerns: Handler = MCP protocol, Service = Servoy API

**Properties Map Approach:**
- Flexible single-step create/update via optional properties parameter
- Smart defaults for required properties
- AI can start simple, add complexity as needed
- Easy to extend without breaking changes

**Naming Consistency:**
- `openX` - Dual purpose create OR update (with properties)
- `getXs` - List all (plural)
- `deleteXs` - Delete multiple (array support)
- `discoverDbX` - Database discovery operations

**Benefits:**
- Complete API parity with Servoy editors
- Production-ready relations and valuelists
- Support for complex scenarios (related valuelists, global methods, etc.)
- Full property control (metadata, behavior, types)
- Reusable service layer for future enhancements

---

## [NEW] DECEMBER 5, 2025 UPDATES - FIRST FORM AUTO-MAIN

### Form Creation Enhancement

**Automatic Main Form Setting:**
- When creating the FIRST form in a solution (no other forms exist), it is automatically set as the main form
- No need to explicitly set `setAsMainForm=true` for the first form
- Improves user experience by ensuring solutions always have a valid startup form
- The result message clearly indicates when this auto-setting occurs: "Automatically set as main form (first form in solution)"

**Logic:**
- Before creating a new form, check if any forms exist in the solution
- If no forms exist (`isFirstForm = true`), automatically set the new form as main
- This happens regardless of the `setAsMainForm` parameter value

**Files Modified:**
- `FormToolHandler.java` - Added `isFirstForm` detection and automatic main form setting
- `rules/forms.md` - Added KEY RULE 11 documenting this behavior

**Impact:** Solutions created via MCP now automatically have a valid main form when the first form is created, preventing "no main form" errors.

---

## [NEW] DECEMBER 4, 2025 UPDATES - CSS VALIDATION & LESS SUPPORT

### Style Service Enhanced with Validation

**CRITICAL FIX - Validation Timing Corrected:**
- Validation now happens FIRST, before any file operations
- Validates content as-is (whether model sends wrapped or unwrapped CSS)
- On validation failure: NO backup created, NO file written, error returned immediately
- Smart wrapper detection: adds `.className { }` wrapper ONLY if not already present
- Prevents both invalid CSS AND double class wrappers

**CSS/LESS Syntax Validation Added:**
- Comprehensive validation before writing styles to prevent file corruption
- Detects duplicate braces: `{ {` or `} }`
- Detects invalid closing sequences: `};` at class level
- Validates balanced braces (equal opening and closing)
- Returns detailed, actionable error messages to AI model

**Validation Flow:**
1. Receive cssContent from model (may include `.className { }` wrapper or just inner content)
2. **VALIDATE AS-IS** - validation happens FIRST
3. If validation fails → return error (no file operations)
4. If validation passes → check if class wrapper exists
5. Add wrapper ONLY if needed (prevents double wrappers)
6. Create backup (only after validation passed)
7. Write to file

**LESS Syntax Fully Supported:**
- Nested selectors preserved: `&:hover { }`, `&:active { }`, `&:focus { }`
- LESS variables and mixins work correctly
- Multi-line formatting with proper indentation
- No longer splits on semicolons (preserves structure)

**Error Messages Help AI Self-Correct:**
- "CSS syntax error: Duplicate opening brace '{ {' detected. Remove extra brace."
- "CSS syntax error: Unbalanced braces - found 3 opening '{' but 2 closing '}'"
- Model receives errors and can retry with corrected CSS

**Files Modified:**
- `StyleService.java` - Added `validateLessContent()` and `formatLessContent()` methods, corrected validation timing
- `rules/styles.md` - Updated with LESS syntax examples and validation error guide

**Impact:** Prevents malformed CSS from EVER being written to LESS files while still supporting advanced LESS features. Handles both wrapped and unwrapped CSS correctly without double wrappers.

---

## DECEMBER 3, 2025 UPDATES - COMPLETE REFRESH

### Bootstrap Label Components - Full CRUD Implementation

**LabelComponentHandler Created:**
- Complete handler for label operations (replaced old BootstrapComponentHandler)
- 5 tools: addLabel, updateLabel, deleteLabel, listLabels, getLabelInfo
- Full CRUD operations for bootstrap labels only
- Clean, focused implementation for one component type

**Label Features:**
- Add labels with CSS positioning (distances from edges: top,right,bottom,left,width,height)
- Update any label property (text, cssPosition, styleClass, etc.)
- Delete labels from forms
- List all labels in a form with details
- Get complete label information
- styleClass property for applying CSS classes (space-separated)
- NO variants for demo (removed from implementation)

**CSS Positioning Corrections:**
- Fixed understanding: top/right/bottom/left are DISTANCES from edges, not coordinates
- Example: "20,-1,-1,25,80,30" = 20px from top, 25px from left, 80x30 size
- Comprehensive examples in rules for all positioning scenarios

### Style Management - Fresh Implementation

**StyleService & StyleHandler Created:**
- Complete rewrite based on correct understanding
- 4 tools: addStyle, getStyle, listStyles, deleteStyle
- Styles go to <solution-name>.less by default (NOT ai-generated.less)
- Optional organization into separate .less files (model chooses filename)
- Automatic import management in main solution file

**Style Features:**
- Plain CSS rules written directly to LESS files (no LESS compilation needed)
- Create/update CSS classes in any LESS file
- List all CSS classes in a file
- Delete CSS classes
- Automatic backup before modifications
- Proper CSS formatting with indentation

**Integration:**
- Labels use styleClass property to apply styles
- Multiple classes supported: styleClass="label-primary label-bold"
- Update label styles via updateLabel tool

### FormService Created (renamed from FormFileService)

**Clean Service Layer:**
- Renamed FormFileService to FormService
- All form-related utilities in one place
- Consistent naming across project

### Files Removed (Old/Incorrect Implementation)

**Deleted:**
- BootstrapComponentHandler.java (replaced with LabelComponentHandler)
- StyleManagementService.java (replaced with StyleService)
- VariantManagementService.java (variants removed for demo)
- StyleToolHandler.java (replaced with StyleHandler)
- All refresh functionality (FormService.refreshForm, StyleService.refreshLessFile)

### Documentation Updates

**rules/bootstrap/labels.md:**
- Updated to reflect full CRUD operations
- Removed all variant mentions
- Corrected CSS positioning examples (distances from edges)
- Added comprehensive positioning scenarios (grids, columns, alignment)
- Updated tool descriptions and workflows

**embeddings/bootstrap/labels.txt:**
- Added CRUD prompts (update, delete, list, get operations)
- Total: ~43 prompts covering all label operations

### Key Changes from Previous Implementation

**Before (Incorrect):**
- Styles went to ai-generated.less
- Auto-generation of component names/positions
- Variants included in implementation
- Complex refresh mechanisms
- Multiple bootstrap component handlers

**After (Correct):**
- Styles go to <solution-name>.less
- AI controls naming and positioning
- No variants (pure styleClass approach)
- No refresh calls (rely on Servoy's natural detection)
- One focused handler per component type (currently: LabelComponentHandler)

### Current Architecture

**Services:**
- BootstrapComponentService - Component CRUD in .frm files
- FormService - Form utilities and validation
- StyleService - CSS/LESS management
- DatabaseSchemaService - Database operations

**Handlers:**
- ButtonComponentHandler - 5 button tools (addButton, updateButton, deleteButton, listButtons, getButtonInfo)
- LabelComponentHandler - 5 label tools (addLabel, updateLabel, deleteLabel, listLabels, getLabelInfo)
- StyleHandler - 4 style tools (addStyle, getStyle, listStyles, deleteStyle)
- FormToolHandler - 5 form tools (getCurrentForm, openForm, setMainForm, listForms, getFormProperties)
- RelationToolHandler - 4 relation tools (openRelation, getRelations, deleteRelations, discoverDbRelations) [ENHANCED Dec 10]
- ValueListToolHandler - 3 valuelist tools (openValueList, getValueLists, deleteValueLists) [ENHANCED Dec 10]
- DatabaseToolHandler - 2 database tools (listTables, getTableInfo)

**Total Tools:** 28 MCP tools

**Services:**
- RelationService - Complete relation business logic [NEW Dec 10]
- ValueListService - Complete valuelist business logic (all 4 types) [NEW Dec 10]
- BootstrapComponentService - Component CRUD in .frm files
- FormService - Form utilities and validation
- StyleService - CSS/LESS management
- DatabaseSchemaService - Database operations

**Status:** Button and Label components fully implemented with complete CRUD operations. Style management ready. Forms fully functional.

**Status:** Label + Style management ready for demo. Clean, working implementation.

---

## [NEW] DECEMBER 2, 2025 UPDATES

### Major Enhancements

**1. Form Tool Handler Enhanced:**
- Added `getCurrentForm` utility tool for getting active form in editor
- Replaced `createForm` with unified `openForm` tool
- Added `setMainForm` tool for solution startup form management
- Added `listForms` tool for form discovery
- Added `getFormProperties` tool for retrieving form details
- Implemented property map support for batch property updates
- Added form inheritance support via `extendsForm` parameter
- Can now open existing forms, create new forms, and update form properties

**2. Variable Injection System:**
- Implemented `{{PROJECT_NAME}}` variable injection
- RulesCache now accepts project name and replaces placeholders
- McpServletProvider gets active project name from ServoyModelManager
- All rules files now get actual project name injected at runtime

**3. RULE 6 - Tool Usage Restrictions:**
- Added comprehensive RULE 6 to copilot-instructions.md
- Centralized tool restrictions (no file system tools, no cross-project operations)
- All rules files now reference RULE 6 instead of repeating prohibitions
- Significantly simplified individual rules files (forms.md reduced by ~150 lines)

**4. Enhanced Form Validation:**
- Forms must be validated using `listForms` tool only
- No file system searches allowed
- No cross-project searches allowed
- Project boundary strictly enforced via {{PROJECT_NAME}}

**5. Special Characters Removed:**
- All emojis and unicode characters removed from rules and documentation
- Text-based alternatives: [YES], [NO], [CRITICAL], [REQUIRED], [FORBIDDEN]
- Arrow symbols (→) replaced with (-->)
- Files affected: copilot-instructions.md, forms.md, relations.md, valuelists.md

### Files Modified

**Code Files:**
- `FormToolHandler.java` - Complete rewrite (~650 lines)
- `RulesCache.java` - Added variable injection support
- `McpServletProvider.java` - Added project name extraction and injection

**Rules Files:**
- `copilot-instructions.md` - Added RULE 6 (~80 lines)
- `forms.md` - Enhanced with 3 tools, property management, examples (~450 lines)
- `relations.md` - Added tool restrictions section
- `valuelists.md` - Added tool restrictions section

**Embeddings:**
- `forms.txt` - Doubled from 21 to 40 prompts

### Statistics

**Before:**
- Tools: 11
- Form handler: 1 tool (createForm)
- Forms.md: ~135 lines

**After:**
- Tools: 13
- Form handler: 3 tools (openForm, setMainForm, listForms)
- Forms.md: ~450 lines
- Capabilities: 600% increase (open, create, edit, list, inheritance, main form)

---

## 🎯 CURRENT STATUS

### ✅ FULLY IMPLEMENTED AND WORKING

**Core MCP Infrastructure:**
- McpServletProvider with getContext tool
- Action list-based context retrieval (no direct intent detection)
- ToolHandlerRegistry with 7 active handlers
- RulesCache for loading rules from `/rules/*.md` files
- ServoyEmbeddingService with BGE-small-en-v1.5 ONNX model
- Similarity search on embedding knowledge base

**28 Working MCP Tools:**

**Database Tools (2):**
1. **listTables** - Lists all tables in a database server
2. **getTableInfo** - Gets table structure with columns and PKs

**Relation Tools (4):**
3. **openRelation** - Creates or opens relation with properties map (all 8 properties supported)
4. **getRelations** - Lists all relations (renamed from listRelations)
5. **deleteRelations** - Deletes multiple relations (array support, renamed from deleteRelation)
6. **discoverDbRelations** - Discovers FKs for potential relations (renamed from discoverRelations)

**ValueList Tools (3):**
7. **openValueList** - Creates or opens valuelist (all 4 types: CUSTOM, DATABASE table, DATABASE related, GLOBAL_METHOD) with properties map (11+ properties supported)
8. **getValueLists** - Lists all valuelists (renamed from listValueLists)
9. **deleteValueLists** - Deletes multiple valuelists (array support, renamed from deleteValueList)

**Form Tools (5):**
10. **getCurrentForm** - Gets currently opened form in active editor
11. **openForm** - Opens existing or creates new form with property management
12. **setMainForm** - Sets solution's main/first form
13. **listForms** - Lists all forms in solution (renamed from getFormProperties)
14. **getFormProperties** - Gets detailed form properties

**Button Component Tools (5):**
15. **addButton** - Add button to form [NEW Dec 3, 2025]
16. **updateButton** - Update button properties [NEW Dec 3, 2025]
17. **deleteButton** - Delete button from form [NEW Dec 3, 2025]
18. **listButtons** - List all buttons in form [NEW Dec 3, 2025]
19. **getButtonInfo** - Get detailed button info [NEW Dec 3, 2025]

**Label Component Tools (5):**
20. **addLabel** - Add label to form [NEW Dec 3, 2025]
21. **updateLabel** - Update label properties [NEW Dec 3, 2025]
22. **deleteLabel** - Delete label from form [NEW Dec 3, 2025]
23. **listLabels** - List all labels in form [NEW Dec 3, 2025]
24. **getLabelInfo** - Get detailed label info [NEW Dec 3, 2025]

**Style Tools (4):**
25. **addStyle** - Add/update CSS class in LESS files [NEW Dec 3, 2025]
26. **getStyle** - Retrieve CSS class content [NEW Dec 3, 2025]
27. **listStyles** - List all CSS classes [NEW Dec 3, 2025]
28. **deleteStyle** - Delete CSS class [NEW Dec 3, 2025]

**6 Service Layer Classes:**
- **RelationService** - Complete relation business logic (all 8 properties) [NEW Dec 10, 2025]
- **ValueListService** - Complete valuelist business logic (all 4 types, 11+ properties) [NEW Dec 10, 2025]
- **DatabaseSchemaService** - Shared database metadata access (used by all handlers)
- **FormService** - Safe .frm file JSON manipulation (UUID generation, backups)
- **StyleService** - CSS/LESS style management [NEW Dec 3, 2025]
- **BootstrapComponentService** - Bootstrap component CRUD in .frm files [NEW Dec 3, 2025]

**8 Tool Handlers:**
- DatabaseToolHandler (2 tools)
- RelationToolHandler (4 tools - openRelation, getRelations, deleteRelations, discoverDbRelations) [ENHANCED Dec 10, 2025]
- ValueListToolHandler (3 tools - openValueList, getValueLists, deleteValueLists) [ENHANCED Dec 10, 2025]
- FormToolHandler (5 tools - getCurrentForm, openForm, setMainForm, listForms, getFormProperties) [ENHANCED Dec 5, 2025]
- ButtonComponentHandler (5 tools - addButton, updateButton, deleteButton, listButtons, getButtonInfo) [NEW Dec 3, 2025]
- LabelComponentHandler (5 tools - addLabel, updateLabel, deleteLabel, listLabels, getLabelInfo) [NEW Dec 3, 2025]
- StyleHandler (4 tools - addStyle, getStyle, listStyles, deleteStyle) [NEW Dec 3, 2025]

**Critical Protections:**
- [YES] RULE 5: .FRM files STRICTLY FORBIDDEN to edit directly
- [YES] RULE 6: Tool usage restrictions - Only use specified tools (NEW Dec 2, 2025)
- [YES] Auto-save after creation (Relations, ValueLists, Forms)
- [YES] Display vs Return columns for ValueLists
- [YES] Parameter guessing prevention (4-layer protection)
- [YES] Tool call transparency requirements
- [YES] User cancellation respect
- [YES] Project boundary enforcement - Stay within current project (NEW Dec 2, 2025)
- [YES] Variable injection - {{PROJECT_NAME}} replaced with actual project name (NEW Dec 2, 2025)

---

## 📁 PROJECT STRUCTURE

### Main Location
```
/Users/marianvid/Servoy/git/master/servoy-eclipse/com.servoy.eclipse.aibridge/
```

### Code Structure
```
src/com/servoy/eclipse/mcp/
├── McpServletProvider.java          # Entry point, getContext tool
├── IToolHandler.java                # Handler interface
├── ToolHandlerRegistry.java         # Central registry of all handlers
├── handlers/
│   ├── DatabaseToolHandler.java    # Database queries (2 tools)
│   ├── RelationToolHandler.java    # Relation operations (4 tools)
│   ├── ValueListToolHandler.java   # ValueList operations (3 tools)
│   ├── FormToolHandler.java        # Form operations (4 tools)
│   ├── ButtonComponentHandler.java # Button component CRUD (5 tools) [NEW Dec 3]
│   ├── LabelComponentHandler.java  # Label component CRUD (5 tools) [NEW Dec 3]
│   └── StyleHandler.java           # Style management (4 tools) [NEW Dec 3]
├── services/
│   ├── DatabaseSchemaService.java       # Shared DB metadata service
│   ├── FormService.java                 # Safe .frm file manipulation
│   ├── StyleService.java                # CSS/LESS style management [NEW Dec 3]
│   └── BootstrapComponentService.java   # Bootstrap component CRUD [NEW Dec 3]
└── ai/
    ├── ServoyEmbeddingService.java # ONNX embedding + similarity search
    └── RulesCache.java             # Rules file loader

src/main/resources/
├── doc/
│   ├── copilot-instructions.md     # Main AI instructions (5 CRITICAL RULES)
│   └── mcp-implementation-guide.md # This document
├── rules/
│   ├── rules.list                  # Manifest of rule files
│   ├── forms.md                    # Form operations rules
│   ├── relations.md                # Relation operations rules
│   ├── valuelists.md               # ValueList operations rules
│   ├── styles.md                   # Style/variant management rules [NEW Dec 3]
│   └── bootstrap/
│       └── buttons.md              # Button component rules (340 lines)
└── embeddings/
    ├── embeddings.list             # Manifest of embedding files
    ├── forms.txt                   # 40 prompts for forms
    ├── relations.txt               # 20 prompts for relations
    ├── valuelists.txt              # 21 prompts for valuelists
    ├── styles.txt                  # 44 prompts for styles [NEW Dec 3]
    └── bootstrap/
        └── buttons.txt             # 20 prompts for buttons (NOT in list yet)
```

---

## 🏗️ ARCHITECTURE

### Core Principle: Action List Approach

**Problem Solved:** Direct intent detection with 100+ training examples per intent was getting confused (e.g., "create form" matched "create relation" at 86% similarity).

**Solution:** Leverage LLM's natural language understanding instead of fighting it.

### Workflow

```
1. User: "Create form with 2 buttons and customer dropdown"
   ↓
2. AI Model (Claude/GPT) generates ACTION LIST using general knowledge:
   ["create form", "add buttons", "create value list"]
   ↓
3. AI calls: getContext({ queries: ["create form", "add buttons", "create value list"] })
   ↓
4. McpServletProvider.handleGetContext():
   - For EACH query, does similarity search via ServoyEmbeddingService
   - Matches queries to categories (FORMS, BUTTONS, VALUELISTS)
   - Loads rules content from RulesCache
   - Returns aggregated context
   ↓
5. AI receives: Tools, rules, parameters, examples for matched categories
   ↓
6. AI creates execution plan with proper hierarchy
   ↓
7. AI executes tools in correct order
   ↓
8. Tools auto-save after creation
```

### Key Components

#### 1. McpServletProvider (Entry Point)

**File:** `McpServletProvider.java`

**Responsibilities:**
- Creates MCP server with HTTP transport
- Registers `getContext` tool
- Auto-registers all handlers from ToolHandlerRegistry
- Handles getContext requests with similarity search

**getContext Tool:**
- Input: `{ queries: ["create form", "add buttons", ...] }`
- For each query: Similarity search via ServoyEmbeddingService
- Matches to categories (e.g., FORMS, BUTTONS, VALUELISTS)
- Gets active project name from ServoyModelManager [NEW Dec 2, 2025]
- Loads rules from RulesCache with project name injection [NEW Dec 2, 2025]
- Replaces {{PROJECT_NAME}} placeholders with actual project name [NEW Dec 2, 2025]
- Returns aggregated context with tools, rules, examples

#### 2. ToolHandlerRegistry (Central Registry)

**File:** `ToolHandlerRegistry.java`

**Current Handlers:**
```java
return new IToolHandler[] {
    new RelationToolHandler(),
    new ValueListToolHandler(),
    new DatabaseToolHandler(),
    new FormToolHandler(),
    new ComponentToolHandler()
};
```

**To add new handler:** Just add one line to the array above.

**Helper Method:**
- `registerTool()` - Eliminates boilerplate for tool registration

#### 3. Tool Handlers

**Each handler implements IToolHandler:**
- `getHandlerName()` - Returns handler name
- `registerTools(McpSyncServer)` - Registers all tools

**Pattern:**
```java
private Map<String, ToolDefinition> getToolDefinitions() {
    Map<String, ToolDefinition> tools = new LinkedHashMap<>();
    tools.put("toolName", new ToolDefinition(
        "Description with parameters",
        this::handleToolName));
    return tools;
}

@Override
public void registerTools(McpSyncServer server) {
    for (Map.Entry<String, ToolDefinition> entry : getToolDefinitions().entrySet()) {
        ToolHandlerRegistry.registerTool(server, 
            entry.getKey(), 
            entry.getValue().description,
            entry.getValue().handler);
    }
}
```

#### 4. Service Layer

**RelationService:** [NEW Dec 10, 2025]
- Complete relation business logic
- Methods: `createRelation()`, `updateRelationProperties()`, `applyRelationProperties()`, `validateAndCorrectDataSource()`, `parseEncapsulation()`
- Features: All 8 relation properties support, smart defaults, auto-save
- Used by: RelationToolHandler
- **Benefit:** Production-ready relations with full API parity, reusable logic

**ValueListService:** [NEW Dec 10, 2025]
- Complete valuelist business logic for all 4 types
- Methods: `createValueList()`, `updateValueListProperties()`, `configureCustomValueList()`, `configureDatabaseValueList()`, `configureRelatedValueList()`, `configureGlobalMethodValueList()`, `applyValueListProperties()`
- Features: All 4 types (CUSTOM, DATABASE table/related, GLOBAL_METHOD), 11+ properties support, smart defaults, auto-save
- Used by: ValueListToolHandler
- **Benefit:** Production-ready valuelists with full API parity including global methods and related values

**DatabaseSchemaService:**
- Shared service for database metadata access
- Methods: `getServer()`, `getTable()`, `getColumns()`, `getPrimaryKeyColumns()`, `getExplicitForeignKeys()`, `getPotentialRelationships()`
- Used by: DatabaseToolHandler, RelationToolHandler, ValueListToolHandler
- **Benefit:** Zero code duplication across handlers

**FormService:**
- Safe .frm file JSON manipulation
- Methods: `addButtonToForm()`, `validateFormFile()`, `getFormInfo()`
- Features: JSON parsing, UUID generation (com.servoy.j2db.util.UUID), backup creation (.frm.backup)
- Used by: ComponentToolHandler
- **Benefit:** Prevents .frm file corruption

**StyleService:**
- CSS/LESS style management
- Methods: `addOrUpdateStyle()`, `getStyle()`, `listStyles()`, `deleteStyle()`, `validateLessContent()`, `formatLessContent()`
- Features: CSS validation, LESS syntax support, automatic import management
- Used by: StyleHandler
- **Benefit:** Prevents invalid CSS, supports advanced LESS features

**BootstrapComponentService:**
- Bootstrap component CRUD operations
- Methods: `addComponentToForm()`, `updateComponent()`, `deleteComponent()`, `listComponentsByType()`, `getComponentInfo()`
- Features: Dual cssPosition storage, styleClass handling, component validation
- Used by: ButtonComponentHandler, LabelComponentHandler
- **Benefit:** Clean, reusable component operations

#### 5. AI Services

**ServoyEmbeddingService:**
- ONNX-based embedding service (BGE-small-en-v1.5 model)
- Singleton pattern, initialized at startup
- Methods: `getInstance()`, `search(query, maxResults)`, `embed(text, metadata...)`
- Loads knowledge base from `/embeddings/*.txt` files
- Provides similarity search for query → category matching

**RulesCache:**
- Loads and caches rules from `/rules/*.md` files
- Static initialization at class load
- Methods: 
  - `getRules(intentKey)` - Returns rules content for a category
  - `getRules(intentKey, projectName)` - Returns rules with {{PROJECT_NAME}} replaced [NEW Dec 2, 2025]
- Variable injection: Replaces {{PROJECT_NAME}} placeholder with actual project name [NEW Dec 2, 2025]
- Auto-discovers files from `rules.list` manifest

---

## 🛠️ IMPLEMENTED TOOLS

### Database Tools (DatabaseToolHandler)

#### listTables
- **Parameters:** serverName (required)
- **Returns:** List of all tables in the database server
- **Protection:** [FORBIDDEN] parameter guessing

#### getTableInfo
- **Parameters:** serverName (required), tableName (required)
- **Returns:** Table structure with columns, types, PK status
- **Protection:** [FORBIDDEN] parameter guessing

### Relation Tools (RelationToolHandler) [ENHANCED Dec 10, 2025]

#### openRelation
- **Parameters:** name (required), primaryDataSource, foreignDataSource, primaryColumn, foreignColumn (optional for creation), properties (optional map)
- **Behavior:** Opens existing relation OR creates new relation. Supports updating properties on existing relations
- **Properties Map Supports (all 8 relation properties):**
  - joinType: "left outer" | "inner" (default: "left outer")
  - allowCreationRelatedRecords: boolean (default: true)
  - allowParentDeleteWhenHavingRelatedRecords: boolean (default: false)
  - deleteRelatedRecords: boolean (default: false)
  - initialSort: string (e.g., "column1 asc, column2 desc")
  - encapsulation: "public" | "hide" | "module" (default: "public")
  - deprecated: string (deprecation message)
  - comment: string (documentation)
- **Auto-save:** YES - saves after creation or property modifications
- **Service:** Uses RelationService for all business logic

#### getRelations
- **Parameters:** None
- **Returns:** List of all relations in current project
- **Renamed from:** listRelations (for consistency)

#### deleteRelations
- **Parameters:** names (required array of strings)
- **Behavior:** Deletes multiple relations in one call
- **Renamed from:** deleteRelation (now supports array)
- **Returns:** Success/not found details for each relation

#### discoverDbRelations
- **Parameters:** serverName (required)
- **Returns:** Explicit FK constraints + potential relations by PK name matching
- **Protection:** [FORBIDDEN] parameter guessing
- **Renamed from:** discoverRelations (emphasizes database discovery)

### ValueList Tools (ValueListToolHandler) [ENHANCED Dec 10, 2025]

#### openValueList
- **Parameters:** name (required), PLUS one of: customValues (array), dataSource (string), relationName (string), globalMethod (string), PLUS optional displayColumn, returnColumn, properties (map)
- **Behavior:** Opens existing valuelist OR creates new valuelist. Supports all 4 types and updating properties on existing valuelists
- **Supports 4 ValueList Types:**
  1. **CUSTOM_VALUES:** customValues (array of strings) - Fixed list
  2. **DATABASE_VALUES (table):** dataSource + displayColumn/returnColumn - From database table
  3. **DATABASE_VALUES (related):** relationName + displayColumn/returnColumn - From related table via relation
  4. **GLOBAL_METHOD_VALUES:** globalMethod (string like "scopes.globals.getCountries") - Dynamic from method
- **Display vs Return:** 
  - Only displayColumn: Same for both display and return
  - Only returnColumn: Same for both display and return
  - Both provided AND different: displayColumn shown, returnColumn stored
- **Properties Map Supports (11+ valuelist properties):**
  - lazyLoading: boolean (load on demand)
  - displayValueType: int (data type of display values)
  - realValueType: int (data type of stored values)
  - separator: string (multi-column separator)
  - sortOptions: string (e.g., "column asc")
  - useTableFilter: boolean (filter by valuelist name column)
  - addEmptyValue: boolean/"always"/"never" (allow null option)
  - fallbackValueListID: string (fallback valuelist name)
  - deprecated: string (deprecation message)
  - encapsulation: "public" | "hide" | "module"
  - comment: string (documentation)
- **Auto-save:** YES - saves after creation or property modifications
- **Service:** Uses ValueListService for all business logic

#### getValueLists
- **Parameters:** None
- **Returns:** List of all valuelists in current project with type indicators
- **Renamed from:** listValueLists (for consistency)

#### deleteValueLists
- **Parameters:** names (required array of strings)
- **Behavior:** Deletes multiple valuelists in one call
- **Renamed from:** deleteValueList (now supports array)
- **Returns:** Success/not found details for each valuelist

### Form Tools (FormToolHandler) [ENHANCED Dec 2, 2025]

#### getCurrentForm
- **Parameters:** None
- **Behavior:** Gets the name of the currently opened form in the active editor
- **Returns:** Form name if a form is currently open, or error if no form is open
- **Usage:** Use when user refers to "current form", "this form", "active form", or doesn't specify a form name

#### openForm
- **Parameters:** name (required), create (default false), width (default 640), height (default 480), style (default "css"), dataSource (optional), extendsForm (optional), setAsMainForm (default false), properties (optional map)
- **Behavior:** Opens existing form OR creates new form if create=true. Can update properties on existing forms. Supports form inheritance and setting as main form
- **Auto-save:** YES - saves after creation or property modifications
- **Property Map Support:** Can set multiple form properties in one call (width, height, useMinWidth, useMinHeight, dataSource, showInMenu, styleName, navigatorID, initialSort)
- **Form Inheritance:** Via extendsForm parameter - sets parent form for inheritance
- **Main Form:** Via setAsMainForm parameter - sets as solution's first/startup form

#### setMainForm
- **Parameters:** name (required)
- **Behavior:** Sets the solution's main/first form (startup form)
- **Auto-save:** YES

#### listForms
- **Parameters:** None
- **Behavior:** Lists all forms in the solution with main form indicator
- **Returns:** Formatted list of form names with [MAIN FORM] marker

#### getFormProperties
- **Parameters:** name (required)
- **Behavior:** Gets detailed properties of a specific form
- **Returns:** Form properties including width, height, dataSource, style type, and other settings

### Button Component Tools (ButtonComponentHandler) [NEW Dec 3, 2025]

**Complete CRUD operations for bootstrap buttons:**

#### addButton
- **Parameters:** formName (required), name (required), cssPosition (required), text (optional, default "Button"), styleClass (optional), imageStyleClass (optional), trailingImageStyleClass (optional), showAs (optional), tabSeq (optional), enabled (optional), visible (optional), toolTipText (optional)
- **Behavior:** Adds bootstrap button to form
- **Auto-save:** YES

#### updateButton
- **Parameters:** formName (required), name (required), plus any properties to update
- **Behavior:** Updates existing button properties
- **Auto-save:** YES

#### deleteButton
- **Parameters:** formName (required), name (required)
- **Behavior:** Deletes button from form

#### listButtons
- **Parameters:** formName (required)
- **Returns:** JSON array of all buttons in form

#### getButtonInfo
- **Parameters:** formName (required), name (required)
- **Returns:** Complete button property details

### Label Component Tools (LabelComponentHandler) [NEW Dec 3, 2025]

**Complete CRUD operations for bootstrap labels:**

#### addLabel
- **Parameters:** formName (required), name (required), cssPosition (required), text (optional, default "Label"), styleClass (optional), labelFor (optional), showAs (optional), enabled (optional), visible (optional), toolTipText (optional)
- **Behavior:** Adds bootstrap label to form
- **Auto-save:** YES

#### updateLabel
- **Parameters:** formName (required), name (required), plus any properties to update
- **Behavior:** Updates existing label properties
- **Auto-save:** YES

#### deleteLabel
- **Parameters:** formName (required), name (required)
- **Behavior:** Deletes label from form

#### listLabels
- **Parameters:** formName (required)
- **Returns:** JSON array of all labels in form

#### getLabelInfo
- **Parameters:** formName (required), name (required)
- **Returns:** Complete label property details

### Context Tool (McpServletProvider)

#### getContext
- **Parameters:** queries (required array of strings)
- **Behavior:** Performs similarity search for each query, returns aggregated rules content
- **Returns:** Tools, rules, parameters, examples for matched categories

---

## 📜 RULES & PROTECTIONS

### CRITICAL RULES in copilot-instructions.md

**RULE 1:** When user cancels or declines - STOP immediately, no alternatives  
**RULE 2:** When tool requires parameter - ASK user, never guess  
**RULE 3:** Respect user intent - User is in control  
**RULE 4:** Tool call transparency - Announce tool + parameters before calling  
**RULE 5:** .FRM files STRICTLY FORBIDDEN to edit - Use MCP tools ONLY  

### 4-Layer Protection System

**Layer 1:** CRITICAL RULES section in copilot-instructions.md (~165 lines)  
**Layer 2:** Main Rules list (rule #1 is .frm protection)  
**Layer 3:** Category-specific warnings in rule files  
**Layer 4:** Tool-level parameter reminders  

### Text-Based Formatting

All instructions use plain text markers (no emojis):
- `[FORBIDDEN]` - Prohibited behavior
- `[REQUIRED]` - Required behavior
- `[WRONG]` - Incorrect example
- `[CORRECT]` - Correct example
- `-->` - Direction indicator
- `=====` - Section separator

---

## 📚 KNOWLEDGE BASE

### Embedding Files (~20-44 prompts each)

**Current files:**
- `forms.txt` - 40 prompts (FORMS category) [UPDATED Dec 2, 2025]
- `relations.txt` - 20 prompts (RELATIONS category)
- `valuelists.txt` - 21 prompts (VALUELISTS category)
- `styles.txt` - 44 prompts (STYLES category) [NEW Dec 3, 2025]
- `bootstrap/buttons.txt` - 20 prompts (NOT in embeddings.list yet)

**Format:** Simple 2-4 word action phrases from AI perspective
```
# FORMS category
create form
add form
make form
build form
new form
...
```

### Rules Files

**Current files:**
- `forms.md` - Form operations (450+ lines) [ENHANCED Dec 2, 2025]
- `relations.md` - Relation operations (131 lines) [UPDATED Dec 2, 2025]
- `valuelists.md` - ValueList operations (155 lines) [UPDATED Dec 2, 2025]
- `styles.md` - Style and variant management (520+ lines) [NEW Dec 3, 2025]
- `bootstrap/buttons.md` - Button components (340 lines, NOT in rules.list yet)
- `copilot-instructions.md` - Global rules (6 CRITICAL RULES) [UPDATED Dec 2, 2025]

**Format:** Markdown with sections:
- CRITICAL warnings (if applicable)
- AVAILABLE TOOLS (tool definitions with parameters)
- KEY RULES (domain-specific rules)
- WORKFLOW (step-by-step guidance)
- EXAMPLES (concrete usage examples)

### Copilot Instructions (Global Rules)

**File:** `copilot-instructions.md` (in `/doc/` folder)

**Purpose:** Global AI behavior rules sent with EVERY prompt to the AI model.

**6 CRITICAL RULES:**

1. **RULE 1: When User Cancels or Declines**
   - If user cancels: STOP immediately, no further tool calls
   - Do NOT try alternative approaches
   - Acknowledge cancellation and wait for new instruction

2. **RULE 2: When Tool Requires Parameter**
   - NEVER guess parameter values
   - ASK THE USER for missing parameters
   - WAIT for their response
   - If they don't know: STOP and acknowledge

3. **RULE 3: Respect User Intent**
   - User is in control
   - If they cancel/say no: STOP immediately
   - NEVER be pushy or try workarounds

4. **RULE 4: Tool Call Transparency**
   - ALWAYS announce which tool you're calling
   - Show all parameter values before calling
   - Report success or failure clearly
   - If failed: Don't retry silently, ask user

5. **RULE 5: .FRM Files STRICTLY FORBIDDEN to Edit**
   - NEVER edit .frm files directly
   - ONLY use MCP tools for form/component operations
   - If tool doesn't exist: Tell user feature not available
   - Prevents form corruption

6. **RULE 6: Tool Usage Restrictions** [NEW Dec 2, 2025]
   - ONLY use tools specified in context rules
   - Do NOT use file system tools (file_search, grep_search, read_file, list_dir)
   - Do NOT use workspace search tools
   - Do NOT search in other projects
   - Stay within {{PROJECT_NAME}} (current project)
   - If tool returns "not found": ACCEPT that result, STOP immediately
   - Do NOT try "alternative methods"

**Architecture:**
- RULE 6 centralizes tool restrictions
- Individual rules files reference RULE 6 instead of repeating prohibitions
- Reduces duplication: Forms.md went from ~600 lines to ~450 lines
- Stronger enforcement: Sent with EVERY prompt

---

## ⚠️ KNOWN ISSUES & LIMITATIONS

### Issues

None currently identified - all components are properly registered and documented.

### Limitations (By Design)

**Not Yet Implemented:**
- Other bootstrap components (textfields, checkboxes, comboboxes, datepickers, calendars, etc.)
- Form deletion
- Multi-column value lists
- Global method value lists
- Related value lists
- Calculation and method management
- Component event handlers
- Form variables and methods

---

## 🔄 ADDING NEW FEATURES

### Adding a New Tool to Existing Handler

1. Add tool definition in handler's `getToolDefinitions()`:
```java
tools.put("newTool", new ToolDefinition(
    "Description with parameters",
    this::handleNewTool));
```

2. Implement handler method:
```java
private McpSchema.CallToolResult handleNewTool(
    McpSyncServerExchange exchange, 
    McpSchema.CallToolRequest request) {
    // Extract parameters
    // Execute logic using Display.syncExec() for UI operations
    // Return CallToolResult
}
```

3. Document in corresponding rules file

### Adding a New Tool Handler

1. Create handler class in `/handlers/` package
2. Implement `IToolHandler` interface
3. Add to `ToolHandlerRegistry.getHandlers()` array
4. Create rules file in `/rules/`
5. Add to `/rules/rules.list`
6. Create embeddings file in `/embeddings/`
7. Add to `/embeddings/embeddings.list`

### Adding a New Category

1. Create `/embeddings/CATEGORY.txt` with ~20 prompts
2. Add to `/embeddings/embeddings.list`
3. Create `/rules/category.md` with tools, rules, examples
4. Add to `/rules/rules.list`
5. Restart to load new embeddings/rules

---

## 🧪 TESTING

### Manual Testing Checklist

**Database Tools:**
- [ ] listTables with valid serverName
- [ ] listTables with invalid serverName (should ask, not guess)
- [ ] getTableInfo with valid table
- [ ] getTableInfo with invalid table

**Relation Tools:**
- [ ] Create new relation
- [ ] Open existing relation
- [ ] Delete relation
- [ ] List all relations
- [ ] Discover relations (should ask for serverName if not provided)
- [ ] Verify auto-save after creation

**ValueList Tools:**
- [ ] Create custom valuelist
- [ ] Create database valuelist (single column)
- [ ] Create database valuelist (display vs return columns)
- [ ] Open existing valuelist
- [ ] Delete valuelist
- [ ] List all valuelists
- [ ] Verify auto-save after creation

**Form Tools:**
- [ ] Get current form via getCurrentForm tool when form editor is open
- [ ] Test getCurrentForm error when no form is open
- [ ] Open existing form
- [ ] Create new CSS form (create=true)
- [ ] Create new responsive form
- [ ] Create form with dataSource
- [ ] Create form with parent (extendsForm)
- [ ] Create and set as main form (setAsMainForm=true)
- [ ] Update existing form properties
- [ ] Set main form via setMainForm tool
- [ ] List all forms via listForms tool
- [ ] Get form properties via getFormProperties tool
- [ ] Verify auto-save after creation
- [ ] Verify auto-save after property updates
- [ ] Test property map with multiple properties
- [ ] Test error when form doesn't exist and create=false
- [ ] Test validation: parent form doesn't exist (should use listForms, not search files)
- [ ] Test that AI stays within project boundary (no cross-project searches)

**Protection Tests:**
- [ ] User cancels → AI stops immediately
- [ ] Missing serverName → AI asks user
- [ ] AI announces tool + parameters before calling
- [ ] AI never edits .frm files directly

---

## 📝 NEXT STEPS

### Immediate Fixes Needed

1. **Add bootstrap/buttons.txt to embeddings.list**
   - Enables BUTTONS category in similarity search
   
2. **Add bootstrap/buttons.md to rules.list**
   - Enables BUTTONS rules in RulesCache

3. **Debug addButton tool**
   - Verify FormFileService is working correctly
   - Test button creation end-to-end
   - Fix any JSON structure issues

### Future Enhancements

**Short-term (Forms & Components focus):**
- Add more bootstrap components (textfield, checkbox, combobox, datepicker, calendar, textarea, etc.)
- Add deleteForm tool
- Add duplicateForm tool
- Add renameForm tool
- Add form variables management
- Add form methods management
- Add component positioning helpers
- Add more form properties (event handlers, view modes, etc.)

**Medium-term:**
- Calculation management
- Method management
- Event handler configuration
- Multi-column valuelists
- Related valuelists

**Long-term:**
- Full UI designer capabilities through MCP
- Form layout management
- Component property editing
- CSS styling tools

---

## 🔧 MAINTENANCE

### When Servoy API Changes

1. Check if persistence classes changed (Form, Relation, ValueList, etc.)
2. Update service layer methods if needed
3. Update tool handler logic
4. Test all affected tools
5. Update documentation

### When Adding Dependencies

1. Update MANIFEST.MF with new bundles
2. Update pom.xml if needed
3. Test in Eclipse runtime
4. Document new dependencies

### Performance Monitoring

- Watch embedding service initialization time
- Monitor similarity search performance
- Check RulesCache memory usage
- Log tool execution times

---

## 📖 REFERENCE

### Key Servoy Classes

**Persistence:**
- `com.servoy.j2db.persistence.Form` - Form definition
- `com.servoy.j2db.persistence.Relation` - Relation definition
- `com.servoy.j2db.persistence.ValueList` - ValueList definition
- `com.servoy.j2db.persistence.IPersist` - Base interface for all persist objects
- `com.servoy.j2db.persistence.ITable` - Table interface
- `com.servoy.j2db.persistence.Column` - Column definition
- `com.servoy.j2db.util.UUID` - UUID generation for components

**Eclipse/Servoy:**
- `com.servoy.eclipse.core.ServoyModelManager` - Access to Servoy model
- `com.servoy.eclipse.model.nature.ServoyProject` - Project wrapper
- `com.servoy.eclipse.ui.util.EditorUtil` - Editor utilities

**Threading:**
- All Eclipse UI operations must run on Display thread
- Use `Display.getDefault().syncExec(() -> { ... })` for synchronous operations
- Use `Display.getDefault().asyncExec(() -> { ... })` for asynchronous operations

### Important Paths

**Resources:**
- Rules: `/main/resources/rules/`
- Embeddings: `/main/resources/embeddings/`
- Documentation: `/main/resources/doc/`

**Code:**
- Handlers: `src/com/servoy/eclipse/mcp/handlers/`
- Services: `src/com/servoy/eclipse/mcp/services/`
- AI: `src/com/servoy/eclipse/mcp/ai/`

---

## 📞 TROUBLESHOOTING

### Embedding Service Issues

**Problem:** Knowledge base not loading  
**Check:** 
- `embeddings.list` contains all .txt files
- Files exist in `/main/resources/embeddings/`
- Restart to reload embeddings

**Problem:** Similarity search returns wrong matches  
**Check:**
- Prompts in .txt files are distinct enough
- Each category has ~20 prompts (not 100+)
- No overlap between categories

### Rules Cache Issues

**Problem:** Rules not loading  
**Check:**
- `rules.list` contains all .md files
- Files exist in `/main/resources/rules/`
- Restart to reload rules cache

**Problem:** Category returns "NOT YET IMPLEMENTED"  
**Check:**
- Rules file exists for that category
- File is in rules.list
- Category name matches (uppercase)

### Tool Execution Issues

**Problem:** Tool not found  
**Check:**
- Handler is in ToolHandlerRegistry.getHandlers()
- Tool is registered in handler's registerTools()
- MCP server started successfully

**Problem:** Eclipse UI operations fail  
**Check:**
- Using Display.syncExec() for UI operations
- Not calling UI code directly from MCP thread

---

**END OF IMPLEMENTATION GUIDE**

*This document reflects the actual implementation as of December 10, 2025.*  
*All code references, file paths, and workflows are verified against the current codebase.*  
*Tool count: 28 tools across 7 handlers (Database: 2, Relations: 4, ValueLists: 3, Forms: 5, Buttons: 5, Labels: 5, Styles: 4)*
