=== SERVOY VALUE LIST OPERATIONS ===

**Goal**: Manage Servoy value lists using the available MCP tools.

**Current Project**: {{PROJECT_NAME}}  
**Note**: Use only the tools specified below. See copilot-instructions.md for security and topic change handling.

**Database queries**: Use `listTables` and `getTableInfo` from DatabaseToolHandler to discover available tables and columns.

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

1. **openValueList** - Create or open value list (custom or database-based)
2. **deleteValueList** - Delete value list
3. **listValueLists** - List all value lists
4. **listTables** - List all tables in a database server (from DatabaseToolHandler)
5. **getTableInfo** - Get comprehensive table information including columns (from DatabaseToolHandler)

---

### openValueList
**Create new or open existing value list**

**Parameters**:
- `name` (required): Value list name

**For CUSTOM value lists** (static):
- `customValues`: Array of strings, e.g. `["Active", "Inactive", "Pending"]`

**For DATABASE value lists** (from table):
- `dataSource`: Table datasource (format: `server_name/table_name`)
- `displayColumn`: Column to display
- `returnColumn`: Column to return (defaults to displayColumn)
- `sortOptions`: "NONE", "ASC", or "DESC" (default: "NONE")

**Examples**:
```
openValueList(name="status_list")

openValueList(
  name="status_list",
  customValues=["Active", "Inactive", "Pending"]
)

openValueList(
  name="countries_list",
  dataSource="example_data/countries",
  displayColumn="country_name",
  returnColumn="country_code",
  sortOptions="ASC"
)
```

### deleteValueList
**Delete a value list**

**Parameters**: `name` (required)

**Example**: `deleteValueList(name="old_list")`

### listValueLists
**List all value lists in current project**

**Parameters**: None

**Example**: `listValueLists()`

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

1. **Custom vs Database**: If user provides values → custom list. If user mentions table → database list.
2. **Database server name is REQUIRED** for database value lists - always ask first if not provided
3. **Project ({{PROJECT_NAME}}) vs Database server**: If user mentions a name that's NOT "{{PROJECT_NAME}}", it's likely a database server
4. **Missing info**: Use `listTables` to discover tables, then `getTableInfo` to discover columns, or ASK THE USER
5. **Extract values carefully**: Preserve casing and order from user input
6. **DataSource format**: `server_name/table_name` (tool adds `db:/` prefix automatically)

---

## WORKFLOW

**Creating Custom Value List**:
1. Extract values from user input (comma-separated, list format, etc.)
2. Call `openValueList` with `customValues` parameter

**Creating Database Value List**:
1. Ask for database server name if not provided
2. If user doesn't know tables: `listTables(serverName="...")`
3. Ask for table name if not provided
4. If user doesn't know columns: `getTableInfo(serverName="...", tableName="...")`
5. Collect parameters (dataSource, displayColumn, optional returnColumn)
6. Call `openValueList` with database parameters

**Other Operations**:
- Open existing: `openValueList(name="valuelist_name")`
- Delete: `deleteValueList(name="valuelist_name")`
- List all: `listValueLists()`

---

## EXAMPLES

**Example 1: Custom value list**
```
User: "Create value list called status with Active, Inactive, Pending"
→ openValueList(name="status", customValues=["Active", "Inactive", "Pending"])
```

**Example 2: Database value list**
```
User: "Create value list from customers table showing company names"
→ Ask: "What's your database server name?"
User: "example_data"
→ openValueList(name="customers_list",
                dataSource="example_data/customers",
                displayColumn="company_name")
```

**Example 3: List existing**
```
User: "Show me all value lists"
→ listValueLists()
```

**Example 4: Database list with display and return columns**
```
User: "Value list from countries, show name but return code, sorted"
→ Ask: "What's your database server name?"
User: "example_data"
→ openValueList(name="countries_list",
                dataSource="example_data/countries",
                displayColumn="country_name",
                returnColumn="country_code",
                sortOptions="ASC")
```

---

## LIMITATIONS

Not yet supported (inform user gracefully):
- Multi-column display
- Global method value lists
- Related value lists
- Fallback values
