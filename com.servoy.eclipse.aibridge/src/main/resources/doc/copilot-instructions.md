# GitHub Copilot Instructions - Servoy MCP Multi-Tool Workflow

## Overview

This document defines how to interact with Servoy MCP tools for intelligent, context-aware development assistance.

---

## CRITICAL: Tool Calling Policy

### For EVERY NEW user request, follow this sequence:

1. **Call `processPrompt` tool FIRST**
   - Pass the complete user message as the `prompt` parameter
   - Do NOT analyze, interpret, or pre-process the message yourself
   - Do NOT decide if it's Servoy-related or not
   - Just call the tool immediately

2. **Handle the `processPrompt` response:**

   **If returns `"PASS_THROUGH"`:**
   - The request is not Servoy-related
   - Handle it yourself using your normal capabilities
   - Do NOT call any other Servoy tools
   - Do NOT call `processPrompt` again for this request

   **If returns enriched prompt (contains rules, context, tool instructions):**
   - You now have:
     - Servoy development context (project, datasources, tables)
     - User's original request
     - Detailed rules for handling this type of request
     - Instructions on which tool to call and how
   - Analyze the enriched prompt carefully
   - Extract required parameters from the user's request
   - Follow the rules to determine next action (see below)

---

## Working with Enriched Prompts

When you receive an enriched prompt from `processPrompt`:

### Step 1: Analyze Parameters

Check if you have all REQUIRED parameters for the suggested tool:
- Read the tool requirements from the enriched prompt
- Extract available parameters from the user's request
- Use the context (project name, datasources, tables) provided in the enriched prompt

### Step 2: Decide Action

**IF you have ALL required parameters:**
- Call the appropriate Servoy tool immediately
- Use the exact tool name and parameters specified in the rules
- The tool will handle creation/opening and return a result

**IF you are MISSING required parameters:**
- ASK THE USER for the missing information
- Be specific about what you need
- Reference the available datasources/tables from the context
- **DO NOT call `processPrompt` again** - the user's response goes directly to you
- Once you have all parameters, call the Servoy tool

### Step 3: Handle Tool Response

After calling a Servoy tool (e.g., `openRelation`, `openValueList`):
- Display the tool's response to the user
- The tool may return success, error, or request more info
- If error asks for more parameters, ask user and retry the tool call
- **This ends the current sequence** - next user message starts fresh with `processPrompt`

---

## Multi-Turn Conversations

**IMPORTANT**: You may have multiple back-and-forth messages with the user before calling a Servoy tool:

```
User: "Create a relation called my_relation"
  ↓
You call: processPrompt(prompt="Create a relation called my_relation")
  ↓
Receives: Enriched prompt with relation rules + context
  ↓
You analyze: Missing primaryDataSource, foreignDataSource, columns
  ↓
You respond: "To create 'my_relation', I need:
              - Primary table
              - Foreign table
              - Join columns
              Available tables: orders, customers, products..."
  ↓
User: "Primary is orders, foreign is customers, use customer_id"
  ↓
**DO NOT call processPrompt again** - user is answering your question
  ↓
You now have all parameters
  ↓
You call: openRelation(name="my_relation", primaryDataSource="example_data/orders", ...)
  ↓
Tool responds: "Relation 'my_relation' created successfully"
  ↓
You display: Success message to user
  ↓
**SEQUENCE ENDS**
```

**Next user message = NEW SEQUENCE** → Call `processPrompt` again

---

## Available Servoy Tools

### 1. `processPrompt` (Intent Detection & Enrichment)

**When to call**: ALWAYS call this FIRST for every new user request

**Parameters**:
- `prompt` (string, required): The complete user message

**Returns**:
- `"PASS_THROUGH"` = Not Servoy-related, handle yourself
- Enriched prompt = Servoy task with context, rules, and tool instructions

**DO NOT call this for**:
- User's follow-up answers to your questions
- Clarifications in an ongoing conversation
- After you've already called it once in the current sequence

---

### 2. `openRelation` (Create/Open Database Relations)

**When to call**: After `processPrompt` returns relation_create enrichment

**Parameters**:
- `name` (string, required): Relation name
- `primaryDataSource` (string, optional): Format `server_name/table_name` or `db:/server_name/table_name`
- `foreignDataSource` (string, optional): Format `server_name/table_name` or `db:/server_name/table_name`
- `primaryColumn` (string, optional): Primary table column name
- `foreignColumn` (string, optional): Foreign table column name

**Returns**:
- Success: "Relation 'name' created successfully" or "Relation 'name' opened in editor"
- Error: Message describing missing parameters or validation errors

**Notes**:
- If relation exists: Opens it (only `name` needed)
- If relation doesn't exist: Creates it (needs datasources and columns)
- Tool handles UUID generation, validation, and editor opening

---

### 3. `openValueList` (Create/Open Value Lists)

**When to call**: After `processPrompt` returns valuelist_create enrichment

**Parameters**:
- `name` (string, required): Value list name
- `values` (array of strings, optional): Custom values for the list

**Returns**:
- Success: "Value list 'name' created successfully" or "Value list 'name' opened in editor"
- Error: Message describing issues

**Notes**:
- If value list exists: Opens it (only `name` needed)
- If doesn't exist: Creates it with optional custom values
- Tool handles creation and editor opening

---

## Core Rules

### 1. Stateless Operation

- **DO NOT use memory from previous sequences**
- **DO NOT remember what tools returned earlier**
- **DO NOT assume entities exist because they were created earlier**
- Treat each NEW user request as the first message
- **ALWAYS call `processPrompt` fresh** for new requests

### 2. Never Skip `processPrompt`

- Even if the request seems non-Servoy related
- Even if you "know" the answer
- Even if you just called it for a similar request
- **Exception**: User is answering your question in an ongoing sequence

### 3. Ask Before Acting

- If required parameters are missing, ASK THE USER
- Do NOT guess datasources, table names, or column names
- Reference the available options from the context
- Multiple back-and-forth exchanges are OK and expected

### 4. Respect Tool Boundaries

- Do NOT create files manually (JSON, XML, etc.)
- Do NOT generate UUIDs yourself
- Do NOT manipulate Servoy project files directly
- Always use the appropriate MCP tool

### 5. Sequence Boundaries

- Tool call (openRelation, openValueList, etc.) = END of sequence
- Next user message = START of new sequence → Call `processPrompt` first
- User answering your question = SAME sequence → Do NOT call `processPrompt`

---

## Example Workflows

### Example 1: Complete Information Provided

```
User: "Create a relation from orders to customers using customer_id"
  ↓
Call: processPrompt(prompt="Create a relation from orders to customers using customer_id")
  ↓
Receives: Enriched prompt with:
  - Context: Project=MyProject, Datasources: example_data/orders, example_data/customers
  - Rules: Use openRelation tool with parameters...
  ↓
Analyze: Have all parameters
  - name: orders_to_customers (derived)
  - primaryDataSource: example_data/orders (from context)
  - foreignDataSource: example_data/customers (from context)
  - primaryColumn: customer_id (from user)
  - foreignColumn: customer_id (from user)
  ↓
Call: openRelation(name="orders_to_customers", primaryDataSource="example_data/orders",
                   foreignDataSource="example_data/customers", primaryColumn="customer_id",
                   foreignColumn="customer_id")
  ↓
Tool: "Relation 'orders_to_customers' created successfully"
  ↓
Display: Success message
  ↓
✅ SEQUENCE ENDS
```

---

### Example 2: Missing Parameters - Multi-Turn

```
User: "Create a relation called order_rel"
  ↓
Call: processPrompt(prompt="Create a relation called order_rel")
  ↓
Receives: Enriched prompt with context and rules
  ↓
Analyze: Missing primaryDataSource, foreignDataSource, columns
  ↓
Respond: "To create 'order_rel', I need:
          - Primary table
          - Foreign table
          - Join columns

          Available datasources:
            - Server: example_data
              Tables: orders, customers, products

          Please specify the tables and columns."
  ↓
User: "Primary is orders, foreign is customers, use customer_id"
  ↓
**DO NOT call processPrompt** - user is answering
  ↓
Analyze: Now have all parameters
  ↓
Call: openRelation(name="order_rel", primaryDataSource="example_data/orders",
                   foreignDataSource="example_data/customers", primaryColumn="customer_id",
                   foreignColumn="customer_id")
  ↓
Tool: "Relation 'order_rel' created successfully"
  ↓
Display: Success message
  ↓
✅ SEQUENCE ENDS
```

---

### Example 3: Tool Returns Error

```
User: "Open the sales_relation"
  ↓
Call: processPrompt(prompt="Open the sales_relation")
  ↓
Receives: Enriched prompt
  ↓
Analyze: Have name, no creation params needed
  ↓
Call: openRelation(name="sales_relation")
  ↓
Tool: "Relation 'sales_relation' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'."
  ↓
Display error and ask: "The relation 'sales_relation' doesn't exist yet. To create it, I need:
                       - Primary table
                       - Foreign table
                       - Join columns

                       Available datasources: ..."
  ↓
User: "Primary is sales, foreign is products, use product_id"
  ↓
**DO NOT call processPrompt** - still in same sequence
  ↓
Call: openRelation(name="sales_relation", primaryDataSource="example_data/sales",
                   foreignDataSource="example_data/products", primaryColumn="product_id",
                   foreignColumn="product_id")
  ↓
Tool: "Relation 'sales_relation' created successfully"
  ↓
✅ SEQUENCE ENDS
```

---

### Example 4: Non-Servoy Request

```
User: "Explain how async/await works in JavaScript"
  ↓
Call: processPrompt(prompt="Explain how async/await works in JavaScript")
  ↓
Receives: "PASS_THROUGH"
  ↓
Handle yourself: [Provide explanation using normal capabilities]
  ↓
✅ SEQUENCE ENDS
```

---

## Decision Tree

```
New user message received
  ↓
Is user answering my question in ongoing sequence?
├─ YES → Don't call processPrompt
│        → Analyze if you now have all parameters
│        → Call appropriate Servoy tool
│        → SEQUENCE ENDS
│
└─ NO → Call processPrompt(prompt=user_message)
        ↓
        Response = "PASS_THROUGH"?
        ├─ YES → Handle yourself with normal capabilities
        │        → SEQUENCE ENDS
        │
        └─ NO → Enriched prompt received
                ↓
                Analyze: Have all required parameters?
                ├─ YES → Call appropriate Servoy tool
                │        → SEQUENCE ENDS
                │
                └─ NO → ASK USER for missing params
                        → Wait for user response
                        → Continue in this sequence
```

---

## Common Mistakes to Avoid

❌ **Calling `processPrompt` when user is answering your question**
✅ Only call `processPrompt` for NEW user requests

❌ **Guessing datasource or column names**
✅ Ask user if parameters are unclear

❌ **Creating files or UUIDs manually**
✅ Use the MCP tools

❌ **Skipping `processPrompt` because request seems simple**
✅ Always call `processPrompt` first for new requests

❌ **Calling tool without checking for required parameters**
✅ Ask user first if parameters are missing

---

## Summary

1. **NEW user request** → Call `processPrompt` first
2. **Enriched response** → Analyze parameters, ask if needed, call Servoy tool
3. **User answering** → Don't call `processPrompt`, continue sequence
4. **Tool called** → Sequence ends
5. **PASS_THROUGH** → Handle yourself, sequence ends

**Every tool call ends the sequence. Next user message starts fresh.**
