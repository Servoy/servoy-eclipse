=== SERVOY VALUE LIST CREATION RULES ===

**Goal**: Create a Servoy value list JSON file in the RelationTest project.

**CRITICAL - READ FIRST**:
- You are working in the RelationTest project
- Create the file at: RelationTest/valueLists/<name>.val
- Do NOT search in other locations or workspace root
- Do NOT ask for location - use RelationTest/valueLists/

**IMPORTANT**:
- Check if valuelists directory exists first
- Create the directory if it doesn't exist
- Generate a valid UUID for the value list
- Never generate XML, only JSON

**File Location & Naming**:
- CRITICAL: Create file in valuelists directory
- Full path: RelationTest/valueLists/<valuelist_name>.val
- Example: RelationTest/valueLists/priority_list.val
- Create the valueLists directory if it doesn't exist
- Encoding: UTF-8

**UUID Policy**:
- Generate one UUID for the value list
- Format: lowercase hex with hyphens (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
- Example: ef1a4d6d-df0b-4504-976b-ad7498c6d445

**JSON Schema** (exact structure required):
```json
{
  "customValues": "<value1>\n<value2>\n<value3>",
  "name": "<valuelist_name>",
  "typeid": 21,
  "uuid": "<UUID>",
  "valueListType": 1
}
```

**Field Descriptions**:
- customValues: Newline-separated list of values (use \n for line breaks)
- name: Value list name (snake_case preferred)
- typeid: Always 21 for value lists
- uuid: Generated UUID
- valueListType: 1 for custom values, 2 for database values

**Example 1** - Simple status list:
```json
{
  "customValues": "Active\nInactive\nPending",
  "name": "status_list",
  "typeid": 21,
  "uuid": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
  "valueListType": 1
}
```

**Example 2** - Priority list:
```json
{
  "customValues": "Low\nMedium\nHigh\nCritical",
  "name": "priority_list",
  "typeid": 21,
  "uuid": "f1e2d3c4-b5a6-4978-8c9d-0e1f2a3b4c5d",
  "valueListType": 1
}
```

**Steps to Execute**:
1. Extract value list name from user prompt
2. Extract values from user prompt (if provided)
3. Check if RelationTest/valueLists/ directory exists, create if needed
4. Generate a valid UUID (lowercase hex with hyphens)
5. Create JSON content following the schema above
6. Save to RelationTest/valueLists/<name>.val
7. Confirm creation with full path to user

Now process the original request above using these rules.
