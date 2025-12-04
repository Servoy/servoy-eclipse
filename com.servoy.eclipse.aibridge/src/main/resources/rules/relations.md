=== SERVOY RELATION OPERATIONS ===

**Goal**: Manage Servoy database relations using the available MCP tools.

**Current Project**: {{PROJECT_NAME}}  
**Note**: Use only the tools specified below. See copilot-instructions.md for security and topic change handling.

---

## [CRITICAL] TOOL USAGE RESTRICTIONS

**For relation operations, use ONLY the tools specified below and NO other tools (like file_search, grep_search, workspace search, etc.).**

**See copilot-instructions.md RULE 6 for complete tool restrictions.**

**Key points:**
- [YES] ONLY use the 4 relation tools + 2 database tools listed below
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

**This applies to:** discoverRelations, listTables, getTableInfo

---

## AVAILABLE TOOLS

1. **openRelation** - Create or open relation
2. **deleteRelation** - Delete relation  
3. **listRelations** - List all relations
4. **discoverRelations** - Discover potential relations by analyzing foreign keys

---

### openRelation
**Create new or open existing relation**

**Parameters**:
- `name` (required): Relation name
- `primaryDataSource`: Primary table (format: `server_name/table_name`)
- `foreignDataSource`: Foreign table (format: `server_name/table_name`)
- `primaryColumn`: Column in primary table
- `foreignColumn`: Column in foreign table

**Examples**:
```
openRelation(name="orders_to_customers")

openRelation(
  name="orders_to_customers",
  primaryDataSource="example_data/orders",
  foreignDataSource="example_data/customers",
  primaryColumn="customer_id",
  foreignColumn="customer_id"
)
```

### deleteRelation
**Delete a relation**

**Parameters**: `name` (required)

**Example**: `deleteRelation(name="old_relation")`

### listRelations
**List all relations in current project**

**Parameters**: None

**Example**: `listRelations()`

### discoverRelations
**Discover potential relations by analyzing foreign keys**

**Parameters**:
- `serverName` (required): Database server name - [FORBIDDEN] NEVER guess this, [REQUIRED] always ASK THE USER if not provided

**Example**: `discoverRelations(serverName="example_data")`

**Returns**:
- List of tables in the database
- **EXPLICIT Foreign Keys**: Actual database FK constraints (most reliable)
- **POTENTIAL Relations**: Inferred from PK name matching

**Always present both types separately to the user.**

---

## KEY RULES

1. **Database server name is REQUIRED** - Always ask first if not provided
2. **Project ({{PROJECT_NAME}}) vs Database server**: If user mentions a name that's NOT "{{PROJECT_NAME}}", it's likely a database server  
3. **Two tables mentioned**: First = primary, second = foreign (don't ask unless ambiguous)
4. **Distinguish FKs**: Always show EXPLICIT FKs separately from POTENTIAL relations when using discoverRelations
5. **Missing info**: Use `discoverRelations` to discover available relationships, or ASK THE USER
6. **DataSource format**: `server_name/table_name` (tool adds `db:/` prefix automatically)

---

## WORKFLOW

**Creating a Relation**:
1. Ask for database server name if not provided
2. If user doesn't know what relations to create: `discoverRelations(serverName="...")`
3. Show EXPLICIT FKs first, then POTENTIAL relations
4. When user mentions two tables: first = primary, second = foreign
5. Collect all parameters, then call `openRelation`

**Discovering Relations**:
- Call `discoverRelations(serverName="...")` to analyze FK relationships
- Present both EXPLICIT and POTENTIAL relations to user
- Help user choose which relation to create

**Other Operations**:
- Open existing: `openRelation(name="relation_name")`
- Delete: `deleteRelation(name="relation_name")`
- List all: `listRelations()`

---

## EXAMPLES

**Example 1: User knows everything**
```
User: "Create relation from orders to customers using customer_id"
--> openRelation(name="orders_to_customers",
               primaryDataSource="example_data/orders",
               foreignDataSource="example_data/customers",
               primaryColumn="customer_id",
               foreignColumn="customer_id")
```

**Example 2: User doesn't know what's available**
```
User: "I need a relation"
--> Ask: "What's your database server name?"
User: "example_data"
--> discoverRelations(serverName="example_data")
--> Show: EXPLICIT FKs + POTENTIAL relations
--> User chooses tables
--> openRelation(...with parameters...)
```

**Example 3: List existing**
```
User: "Show me all relations"
--> listRelations()
```
