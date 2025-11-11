=== SERVOY RELATION CREATION RULES ===

**Goal**: Create a Servoy relation JSON file in the RelationTest project.

**CRITICAL - READ FIRST**:
- You are working in the RelationTest project
- Create the file at: RelationTest/relations/<relation_name>.rel
- Do NOT search in other locations or workspace root
- Do NOT ask for location - use RelationTest/relations/
- Always scan the relations/ directory before creating a new relation
- Do not rely on cached or remembered state

**Inputs (strict format)**:
- Primary data source: db:/<server>/<primary_table>
- Foreign data source: db:/<server>/<foreign_table>
- Only accept db:/.../... format. If prompt provides anything else, normalize it
- Join columns: If not provided, ask for primary_column and foreign_column. Do not guess.
- Relation name: If not provided, derive snake_case as <primary_table>_to_<foreign_table>

**File Location & Naming**:
- Directory: RelationTest/relations/ (create if missing)
- Filename: <relation_name>.rel (e.g., customers_to_orders.rel)
- Encoding: UTF-8

**UUID Policy**:
- Generate TWO UUIDs locally: one for relation, one for relation item
- Format: lowercase hex with hyphens (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
- Regex: ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$
- Example: ef1a4d6d-df0b-4504-976b-ad7498c6d445

**JSON Schema** (exact structure required - no extra fields):
```json
{
  "allowCreationRelatedRecords": true,
  "foreignDataSource": "db:/<server>/<foreign_table>",
  "items": [
    {
      "foreignColumnName": "<foreign_column>",
      "primaryDataProviderID": "<primary_column>",
      "typeid": 23,
      "uuid": "<UUID-ITEM>"
    }
  ],
  "joinType": 1,
  "name": "<relation_name>",
  "primaryDataSource": "db:/<server>/<primary_table>",
  "typeid": 22,
  "uuid": "<UUID-RELATION>"
}
```

**Field Descriptions**:
- allowCreationRelatedRecords: Always true
- foreignDataSource: Foreign table in db:/<server>/<table> format
- items: Array with one item containing column mapping
- foreignColumnName: Column name in foreign table
- primaryDataProviderID: Column name in primary table
- typeid: 23 for relation items, 22 for relation
- joinType: 1 for LEFT OUTER JOIN
- uuid: Generated UUIDs (different for relation and item)

**Example** - Orders to Customers relation:
```json
{
  "allowCreationRelatedRecords": true,
  "foreignDataSource": "db:/example_data/customers",
  "items": [
    {
      "foreignColumnName": "customer_id",
      "primaryDataProviderID": "customer_id",
      "typeid": 23,
      "uuid": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d"
    }
  ],
  "joinType": 1,
  "name": "orders_to_customers",
  "primaryDataSource": "db:/example_data/orders",
  "typeid": 22,
  "uuid": "f1e2d3c4-b5a6-4978-8c9d-0e1f2a3b4c5d"
}
```

**Steps to Execute**:
1. Extract primary and foreign data sources from prompt
2. Normalize to db:/<server>/<table> format if needed
3. Extract or ask for primary_column and foreign_column
4. Derive relation name if not provided
5. Check if RelationTest/relations/ directory exists, create if needed
6. Generate TWO valid UUIDs (one for relation, one for item)
7. Create JSON content following the schema above
8. Save to RelationTest/relations/<relation_name>.rel
9. Confirm creation with full path to user

**IMPORTANT**: Never generate XML, only JSON. Follow the schema exactly.
