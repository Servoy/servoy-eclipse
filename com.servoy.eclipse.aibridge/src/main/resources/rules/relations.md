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

**This applies to:** discoverDbRelations, listTables, getTableInfo

---

## AVAILABLE TOOLS

1. **openRelation** - Create or open relation with full property support (all 8 properties)
2. **getRelations** - List all relations (renamed from listRelations)
3. **deleteRelations** - Delete multiple relations (array support, renamed from deleteRelation)
4. **discoverDbRelations** - Discover potential relations by analyzing foreign keys (renamed from discoverRelations)

---

### openRelation
**Create new or open existing relation - Dual purpose tool with properties map**

**Behavior:**
- If relation exists: Opens it (and updates properties if provided)
- If relation doesn't exist: Creates it with provided parameters

**Required for creation:**
- `name` (string): Relation name
- `primaryDataSource` (string): Primary table (format: `server_name/table_name`)
- `foreignDataSource` (string): Foreign table (format: `server_name/table_name`)

**Optional for creation:**
- `primaryColumn` (string): Column in primary table
- `foreignColumn` (string): Column in foreign table
- `properties` (object): Map of relation properties (see below)

**Properties Map - All 8 Relation Properties:**
```
properties: {
  "joinType": "left outer" | "inner",  // Default: "left outer"
  "allowCreationRelatedRecords": boolean,  // Default: true
  "allowParentDeleteWhenHavingRelatedRecords": boolean,  // Default: false
  "deleteRelatedRecords": boolean,  // Default: false (cascade delete)
  "initialSort": "column1 asc, column2 desc",  // Optional sorting
  "encapsulation": "public" | "hide" | "module",  // Default: "public"
  "deprecated": "Use new_relation instead",  // Optional deprecation message
  "comment": "Documentation for this relation"  // Optional comment
}
```

**Examples**:

Simple - open existing:
```
openRelation(name="orders_to_customers")
```

Create with defaults:
```
openRelation(
  name="orders_to_customers",
  primaryDataSource="example_data/orders",
  foreignDataSource="example_data/customers",
  primaryColumn="customer_id",
  foreignColumn="customer_id"
)
```

Create with properties:
```
openRelation(
  name="orders_to_customers",
  primaryDataSource="example_data/orders",
  foreignDataSource="example_data/customers",
  primaryColumn="customer_id",
  foreignColumn="customer_id",
  properties={
    "joinType": "inner",
    "deleteRelatedRecords": true,
    "initialSort": "order_date desc",
    "comment": "Links orders to their customers"
  }
)
```

Update existing relation properties:
```
openRelation(
  name="orders_to_customers",
  properties={
    "encapsulation": "module",
    "deprecated": "Use orders_to_customers_v2 instead"
  }
)
```

### getRelations
**List all relations in current project**

**Parameters**: None

**Example**: `getRelations()`

**Returns**: List of all relations with their primary/foreign datasources

### deleteRelations
**Delete one or more relations**

**Parameters**: 
- `names` (required array): Array of relation names to delete

**Examples**: 
```
deleteRelations(names=["old_relation"])
deleteRelations(names=["temp_rel1", "temp_rel2", "deprecated_relation"])
```

**Returns**: Success/not found details for each relation

### discoverDbRelations
**Discover potential relations by analyzing foreign keys**

**Parameters**:
- `serverName` (required): Database server name - [FORBIDDEN] NEVER guess this, [REQUIRED] always ASK THE USER if not provided

**Example**: `discoverDbRelations(serverName="example_data")`

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
4. **Distinguish FKs**: Always show EXPLICIT FKs separately from POTENTIAL relations when using discoverDbRelations
5. **Missing info**: Use `discoverDbRelations` to discover available relationships, or ASK THE USER
6. **DataSource format**: `server_name/table_name` (tool adds `db:/` prefix automatically)
7. **Properties are optional**: Start simple (just create), add properties when needed for specific behavior
8. **Update via properties**: To modify existing relation, call openRelation with just name + properties map
9. **Bulk delete**: Use deleteRelations with array for multiple relations at once

---

## WORKFLOW

**Creating a Relation**:
1. Ask for database server name if not provided
2. If user doesn't know what relations to create: `discoverDbRelations(serverName="...")`
3. Show EXPLICIT FKs first, then POTENTIAL relations
4. When user mentions two tables: first = primary, second = foreign
5. Collect all parameters, then call `openRelation`
6. Add properties map if user specifies behavior (join type, cascade delete, etc.)

**Discovering Relations**:
- Call `discoverDbRelations(serverName="...")` to analyze FK relationships
- Present both EXPLICIT and POTENTIAL relations to user
- Help user choose which relation to create

**Updating Relations**:
- To update properties: `openRelation(name="...", properties={...})`
- Can update any of the 8 properties independently

**Other Operations**:
- Open existing: `openRelation(name="relation_name")`
- Delete one: `deleteRelations(names=["relation_name"])`
- Delete multiple: `deleteRelations(names=["rel1", "rel2"])`
- List all: `getRelations()`

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
--> discoverDbRelations(serverName="example_data")
--> Show: EXPLICIT FKs + POTENTIAL relations
--> User chooses tables
--> openRelation(...with parameters...)
```

**Example 3: List existing**
```
User: "Show me all relations"
--> getRelations()
```
