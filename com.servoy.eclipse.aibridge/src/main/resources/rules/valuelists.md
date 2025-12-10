=== SERVOY VALUE LIST OPERATIONS ===

**Goal**: Manage Servoy value lists using the available MCP tools.

**Current Project**: {{PROJECT_NAME}}  
**Note**: Use only the tools specified below. See copilot-instructions.md for security and topic change handling.

**Database queries**: Use `listTables` and `getTableInfo` from DatabaseToolHandler to discover available tables and columns.

---

## [CRITICAL] TOOL USAGE RESTRICTIONS

**For value list operations, use ONLY the tools specified below and NO other tools (like file_search, grep_search, workspace search, etc.).**

**See copilot-instructions.md RULE 6 for complete tool restrictions.**

**Key points:**
- [YES] ONLY use the 3 value list tools + 2 database tools listed below
- [YES] Stay within {{PROJECT_NAME}} project
- [NO] Do NOT use file system or search tools
- [NO] Do NOT search in other projects

---

## CRITICAL: DATABASE SERVER NAME

**When ANY database tool requires serverName parameter:**

[FORBIDDEN] NEVER guess server names  
[FORBIDDEN] NEVER try multiple server names hoping one works  
[FORBIDDEN] DO NOT proceed without the parameter

[REQUIRED] If user doesn't provide serverName: STOP and ASK THE USER explicitly  
[REQUIRED] Example: "I need the database server name. What server should I query?"

**This applies to:** listTables, getTableInfo

---

## AVAILABLE TOOLS

1. **openValueList** - Create or open valuelist (all 4 types: CUSTOM, DATABASE table, DATABASE related, GLOBAL_METHOD) with full property support
2. **getValueLists** - List all valuelists (renamed from listValueLists)
3. **deleteValueLists** - Delete multiple valuelists (array support, renamed from deleteValueList)
4. **listTables** - List all tables in a database server (from DatabaseToolHandler)
5. **getTableInfo** - Get comprehensive table information including columns (from DatabaseToolHandler)

---

### openValueList
**Create new or open existing valuelist - Supports all 4 types with properties map**

**Behavior:**
- If valuelist exists: Opens it (and updates properties if provided)
- If valuelist doesn't exist: Creates it with provided parameters
**Create new or open existing value list**

**Required:**
- `name` (string): ValueList name

**Type-Specific Parameters (provide ONE of these):**

**Type 1: CUSTOM_VALUES** (static list):
- `customValues` (array): Array of strings, e.g. `["Active", "Inactive", "Pending"]`

**Type 2: DATABASE_VALUES (table)** (from database table):
- `dataSource` (string): Table datasource (format: `server_name/table_name`)
- `displayColumn` (string): Column to display to user
- `returnColumn` (string, optional): Column value to store in database field

**Type 3: DATABASE_VALUES (related)** (from related table via relation):
- `relationName` (string): Relation name to follow (e.g., "orders_to_customers")
- `displayColumn` (string): Column to display to user
- `returnColumn` (string, optional): Column value to store in database field

**Type 4: GLOBAL_METHOD_VALUES** (dynamic from method):
- `globalMethod` (string): Global method name (e.g., "scopes.globals.getCountries")
  - Method must return JSDataSet with 1 or 2 columns (display, optional: real)

**Display vs Return Column Behavior**:
- **Only `displayColumn`**: Same column for BOTH display and return
- **Only `returnColumn`**: Same column for BOTH display and return
- **Both specified AND different**: Display one, return the other (common for ID/Name pairs)

**Optional Properties Map (11+ valuelist properties):**
```
properties: {
  "lazyLoading": boolean,  // Load on demand (for large lists)
  "displayValueType": int,  // Data type of display values (IColumnTypes)
  "realValueType": int,  // Data type of stored values (IColumnTypes)
  "separator": ", ",  // Multi-column separator
  "sortOptions": "column asc",  // Sort order
  "useTableFilter": boolean,  // Filter by valuelist_name column
  "addEmptyValue": true | false | "always" | "never",  // Allow null option
  "fallbackValueListID": "other_valuelist_name",  // Fallback valuelist
  "deprecated": "Use new_list instead",  // Deprecation message
  "encapsulation": "public" | "hide" | "module",  // Visibility
  "comment": "Documentation for this valuelist"  // Comment
}
```

**Examples**:

Open existing:
```
openValueList(name="status_list")
```

Custom valuelist:
```
openValueList(
  name="status_list",
  customValues=["Active", "Inactive", "Pending"]
)
```

Database (table) valuelist:
```
openValueList(
  name="countries_list",
  dataSource="example_data/countries",
  displayColumn="country_name",
  returnColumn="country_code",
  properties={"sortOptions": "country_name asc", "lazyLoading": true}
)
```

Database (related) valuelist:
```
openValueList(
  name="customer_orders",
  relationName="customers_to_orders",
  displayColumn="order_number",
  returnColumn="order_id"
)
```

Global method valuelist:
```
openValueList(
  name="dynamic_countries",
  globalMethod="scopes.globals.getCountries"
)
```

Update existing valuelist properties:
```
openValueList(
  name="status_list",
  properties={
    "addEmptyValue": true,
    "deprecated": "Use status_list_v2 instead"
  }
)
```

### getValueLists
**List all valuelists in current project**

**Parameters**: None

**Example**: `getValueLists()`

**Returns**: List of all valuelists with type indicators (CUSTOM, DATABASE, GLOBAL_METHOD)

### deleteValueLists
**Delete one or more valuelists**

**Parameters**: 
- `names` (required array): Array of valuelist names to delete

**Examples**: 
```
deleteValueLists(names=["old_list"])
deleteValueLists(names=["temp_list1", "temp_list2", "deprecated_list"])
```

**Returns**: Success/not found details for each valuelist

### listTables
**List all tables in a database server**

**Parameters**:
- `serverName` (required): Database server name - [FORBIDDEN] NEVER guess this, [REQUIRED] always ASK THE USER if not provided

**Example**: `listTables(serverName="example_data")`

**Returns**: List of all table names in the database server

### getTableInfo
**Get comprehensive information about a database table**

**Parameters**:
- `serverName` (required): Database server name - [FORBIDDEN] NEVER guess this, [REQUIRED] always ASK THE USER if not provided
- `tableName` (required): Table name

**Example**: `getTableInfo(serverName="example_data", tableName="customers")`

**Returns**: Table name, datasource, and list of columns with their names, types, and primary key status

---

## KEY RULES

1. **4 ValueList Types**: CUSTOM (array), DATABASE table (dataSource), DATABASE related (relationName), GLOBAL_METHOD (globalMethod)
2. **Choose type based on context**: 
   - User provides values → CUSTOM
   - User mentions table → DATABASE (table)
   - User mentions relation → DATABASE (related)
   - User wants dynamic/code-generated → GLOBAL_METHOD
3. **Database server name is REQUIRED** for database valuelists - always ask first if not provided
4. **Project ({{PROJECT_NAME}}) vs Database server**: If user mentions a name that's NOT "{{PROJECT_NAME}}", it's likely a database server
5. **Missing info**: Use `listTables` to discover tables, then `getTableInfo` to discover columns, or ASK THE USER
6. **Extract values carefully**: Preserve casing and order from user input
7. **DataSource format**: `server_name/table_name` (tool adds `db:/` prefix automatically)
8. **Display vs Return columns**:
   - **Display column**: What the user SEES in the dropdown/combobox
   - **Return column**: What gets STORED in the database field
   - **Common use case**: Display human-readable names (e.g., "John Smith"), return IDs (e.g., 12345)
   - **When to use both**: When table has separate ID and name columns (customer_id vs customer_name)
   - **When to use one**: When same value should be displayed and stored (e.g., country codes, status values)
9. **Properties are optional**: Start simple (just create), add properties when needed for specific behavior
10. **Update via properties**: To modify existing valuelist, call openValueList with just name + properties map
11. **Bulk delete**: Use deleteValueLists with array for multiple valuelists at once
12. **Global methods**: Method must return JSDataSet, use format "scopes.globals.methodName"

---

## WORKFLOW

**Creating Custom ValueList**:
1. Extract values from user input (comma-separated, list format, etc.)
2. Call `openValueList` with `customValues` parameter

**Creating Database ValueList (table)**:
1. Ask for database server name if not provided
2. If user doesn't know tables: `listTables(serverName="...")`
3. Ask for table name if not provided
4. If user doesn't know columns: `getTableInfo(serverName="...", tableName="...")`
5. Determine display and return columns:
   - If user wants to show names but store IDs: Use BOTH displayColumn and returnColumn
   - If same value for display and storage: Use only displayColumn (or only returnColumn)
   - Ask user if unclear: "Should I display and store the same column, or display one column but store another?"
6. Collect parameters (dataSource, displayColumn, optional returnColumn)
7. Call `openValueList` with database parameters
8. Add properties map if user specifies behavior (lazy loading, sorting, etc.)

**Creating Database ValueList (related)**:
1. User mentions relation name or "values from related table"
2. Verify relation exists (can use `getRelations` to list)
3. Ask for displayColumn/returnColumn for the related table
4. Call `openValueList` with relationName parameter

**Creating Global Method ValueList**:
1. User mentions "dynamic values" or "from code/method"
2. Ask for global method name if not provided (format: "scopes.globals.methodName")
3. Remind user: Method must return JSDataSet with 1-2 columns
4. Call `openValueList` with globalMethod parameter

**Updating ValueLists**:
- To update properties: `openValueList(name="...", properties={...})`
- Can update any of the 11+ properties independently

**Other Operations**:
- Open existing: `openValueList(name="valuelist_name")`
- Delete one: `deleteValueLists(names=["valuelist_name"])`
- Delete multiple: `deleteValueLists(names=["list1", "list2"])`
- List all: `getValueLists()`

---

## EXAMPLES

**Example 1: Custom value list**
```
User: "Create value list called status with Active, Inactive, Pending"
--> openValueList(name="status", customValues=["Active", "Inactive", "Pending"])
```

**Example 2: Database value list (same column for display and return)**
```
User: "Create value list from customers table showing company names"
--> Ask: "What's your database server name?"
User: "example_data"
--> openValueList(name="customers_list",
                dataSource="example_data/customers",
                displayColumn="company_name")
Note: company_name will be both displayed AND stored
```

**Example 3: Database value list (different display and return)**
```
User: "Create value list showing customer names but storing customer IDs"
--> Ask: "What's your database server name?"
User: "example_data"
--> Ask: "What table contains the customers?"
User: "customers"
--> openValueList(name="customers_list",
                dataSource="example_data/customers",
                displayColumn="customer_name",
                returnColumn="customer_id")
Note: User sees "John Smith", database stores 12345
```

**Example 4: Related value list**
```
User: "Create value list showing orders for the current customer"
--> openValueList(name="customer_orders",
                relationName="customers_to_orders",
                displayColumn="order_number",
                returnColumn="order_id")
Note: Values are dynamically filtered based on current customer record via relation
```

**Example 5: Global method value list**
```
User: "Create value list that gets countries from our custom API"
--> Ask: "What global method returns the countries?"
User: "getCountriesFromAPI in globals scope"
--> openValueList(name="api_countries",
                globalMethod="scopes.globals.getCountriesFromAPI")
Note: Method must return JSDataSet with 1 or 2 columns (display, optional: real value)
```

**Example 6: Value list with properties**
```
User: "Create countries valuelist with lazy loading and sorted alphabetically"
--> openValueList(name="countries",
                dataSource="example_data/countries",
                displayColumn="country_name",
                returnColumn="country_code",
                properties={
                  "lazyLoading": true,
                  "sortOptions": "country_name asc",
                  "addEmptyValue": true
                })
                returnColumn="customer_id")
Note: User sees customer_name, but customer_id is stored in the field
```

**Example 4: Database value list with country codes**
```
User: "Create value list from countries table with country codes"
--> Ask: "What's your database server name?"
User: "example_data"
--> openValueList(name="countries_list",
                dataSource="example_data/countries",
                displayColumn="country_code")
Note: Same value (country_code) for display and storage
```

**Example 5: List existing**
```
User: "Show me all value lists"
--> getValueLists()
```

**Example 6: Database list with display and return columns**
```
User: "Value list from countries, show name but return code, sorted"
--> Ask: "What's your database server name?"
User: "example_data"
--> openValueList(name="countries_list",
                dataSource="example_data/countries",
                displayColumn="country_name",
                returnColumn="country_code",
                properties={"sortOptions": "country_name asc"})
```

---
