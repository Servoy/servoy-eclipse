# MCP Workflow Architecture Documentation

## Overview

This document describes the complete Model Context Protocol (MCP) workflow implementation for Servoy Eclipse integration. The system uses AI-powered intent detection with ONNX embeddings to route user requests to appropriate tools and enrich prompts with context-specific rules.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Details](#component-details)
3. [Workflow Process](#workflow-process)
4. [Intent Detection System](#intent-detection-system)
5. [Rules and Prompt Enrichment](#rules-and-prompt-enrichment)
6. [MCP Tools](#mcp-tools)
7. [Out-of-Context Handling](#out-of-context-handling)
8. [File Structure](#file-structure)
9. [Development Guidelines](#development-guidelines)

---

## Architecture Overview

### High-Level Flow

```
User Input (via Copilot)
    ↓
processPrompt Tool (MCP Server)
    ↓
Intent Detection (ONNX Embeddings)
    ↓
Rule Matching & Prompt Enrichment
    ↓
Enriched Prompt → LLM (Copilot)
    ↓
LLM calls specific tools OR responds
    ↓
Tool executes → Returns result
    ↓
User sees result in Eclipse
```

### Key Principle: **Unified Intent Architecture**

Each **high-level intent** (e.g., RELATIONS, VALUE_LISTS) has:
- **One `.txt` file** with training prompts for embedding-based detection
- **One `.md` file** with comprehensive rules for ALL operations within that intent
- **Multiple MCP tools** that execute specific operations

**Example - RELATIONS Intent:**
- Training file: `relations.txt` (144 sample prompts)
- Rules file: `relations.md` (comprehensive workflow guide)
- Tools: `openRelation`, `deleteRelation`, `listRelations`, `queryDatabaseSchema`

---

## Component Details

### 1. MCP Server (`McpServletProvider.java`)

**Location:** `src/com/servoy/eclipse/mcp/McpServletProvider.java`

**Purpose:** Hosts the MCP server that exposes tools to the Copilot LLM.

**Key Responsibilities:**
- Registers MCP tools (`processPrompt`, `openRelation`, `deleteRelation`, etc.)
- Handles tool invocations from the LLM
- Executes Eclipse operations (opens editors, creates relations, etc.)
- Returns results to the LLM

**Important Implementation Details:**

```java
// Singleton pattern for PromptEnricher to avoid reloading models
final PromptEnricher enricher = new PromptEnricher();

// Tool registration
Tool processPromptTool = McpSchema.Tool.builder()
    .inputSchema(new JsonSchema("object", null, null, null, null, null))
    .name("processPrompt")
    .description("Process user prompts for Servoy operations...")
    .build();

server.addTool(SyncToolSpecification.builder()
    .tool(processPromptTool)
    .callHandler(this::handleProcessPrompt)
    .build());
```

**Key Methods:**
- `handleProcessPrompt()` - Routes to PromptEnricher for intent detection
- `handleOpenRelation()` - Creates/opens relations
- `handleDeleteRelation()` - Deletes relations
- `handleListRelations()` - Lists all relations in solution
- `handleQueryDatabaseSchema()` - Queries database servers for tables and FKs
- `handleOpenValueList()` - Creates/opens value lists

---

### 2. Prompt Enricher (`PromptEnricher.java`)

**Location:** `src/com/servoy/eclipse/mcp/ai/PromptEnricher.java`

**Purpose:** AI-powered intent detection and prompt enrichment using ONNX embeddings.

**Key Responsibilities:**
- Loads ONNX embedding model (`onnx_model_mac.onnx`)
- Generates embeddings for user prompts
- Compares against pre-computed training embeddings
- Finds best matching intent using cosine similarity
- Enriches prompt with intent-specific rules
- Performs template variable substitution

**Important Implementation Details:**

```java
public class PromptEnricher
{
    private OrtEnvironment env;
    private OrtSession session;
    private Map<String, List<float[]>> intentEmbeddings;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    public String processPrompt(String userPrompt)
    {
        // 1. Generate embedding for user prompt
        float[] promptEmbedding = generateEmbedding(userPrompt);

        // 2. Find best matching intent
        String detectedIntent = detectIntent(promptEmbedding);

        // 3. If no match, return PASS_THROUGH
        if (detectedIntent == null) {
            return "PASS_THROUGH";
        }

        // 4. Load rules for detected intent
        String rules = loadRulesForIntent(detectedIntent);

        // 5. Substitute template variables
        String enrichedRules = substituteTemplateVariables(rules);

        // 6. Return enriched prompt
        return "INTENT: " + detectedIntent + "\n\n" + enrichedRules;
    }
}
```

**Template Variable Substitution:**

Current template variables:
- `{{PROJECT_NAME}}` → Active Servoy project name

**Cosine Similarity Formula:**

```
similarity = (A · B) / (||A|| × ||B||)

Where:
- A = user prompt embedding
- B = training sample embedding
- Result ranges from -1 (opposite) to 1 (identical)
- Threshold = 0.5 for matching
```

---

### 3. Intent Detection Files

**Location:** `src/main/resources/intents/`

#### Training Files (`.txt`)

**Format:** One prompt per line

**Purpose:**
- Pre-computed embeddings stored in `.npy` files
- Used for cosine similarity matching at runtime

**Example - `relations.txt`:**
```
I need a relation in my database
Create a relation between orders and customers
Link the products table to categories
Show me all relations
Delete the old_relation
What foreign keys exist in the customers table?
...
```

**Naming Convention:**
- Intent name in lowercase
- Snake_case for multi-word intents
- Example: `relations.txt`, `value_lists.txt`

#### Embedding Files (`.npy`)

**Generated by:** External Python script (fine-tuning dataset generator)

**Format:** NumPy array of float32 embeddings

**Purpose:** Pre-computed embeddings for fast runtime lookup

**Example:** `relations.npy` contains embeddings for all 144 prompts in `relations.txt`

---

### 4. Rules Files (`.md`)

**Location:** `src/main/resources/rules/`

**Purpose:** Comprehensive workflow guide for the LLM after intent is detected

**Structure (Standard Format):**

```markdown
=== [INTENT NAME] OPERATIONS ===

**Goal**: [Brief description]

**CRITICAL: Current Project Name** = {{PROJECT_NAME}}

---

## AVAILABLE TOOLS

[List of tools with brief descriptions]

---

## TOOL 1: [toolName]

**Purpose**: [What it does]

**Required Parameters**:
- param1 (type): Description

**Optional Parameters**:
- param2 (type): Description

**When to use**:
- [Usage scenarios]

**Examples**:
```[code examples]```

---

## WORKFLOW DECISION TREE

[Step-by-step logic for routing user requests]

---

## CRITICAL DISAMBIGUATION RULES

[Rules for distinguishing similar concepts]

---

## EXAMPLES

### Example 1: [Scenario]
[Complete walkthrough with user input, analysis, and tool calls]

---

## IMPORTANT RULES

1. [Rule 1]
2. [Rule 2]
...

---

## ⚠️ CRITICAL: HANDLING OUT-OF-CONTEXT PROMPTS ⚠️

### ✅ CASE 1: Servoy-related but DIFFERENT topic
→ Call processPrompt again

### ❌ CASE 2: Completely unrelated to Servoy
→ Generic "I help with Servoy" response

---

## FINAL NOTES

[Additional guidance]
```

**Key Sections Explained:**

1. **AVAILABLE TOOLS** - Lists all tools for this intent
2. **TOOL DETAILS** - Detailed specs for each tool (parameters, examples)
3. **WORKFLOW DECISION TREE** - Logic for routing user requests to correct tools
4. **DISAMBIGUATION RULES** - Critical distinctions (e.g., database server vs project name)
5. **EXAMPLES** - Complete walkthroughs showing analysis → action
6. **OUT-OF-CONTEXT HANDLING** - Rules for topic switching or off-topic questions

---

## Workflow Process

### Step-by-Step Flow

#### 1. User Types Message in Copilot

```
User: "I need a relation between orders and customers"
```

#### 2. Copilot Calls `processPrompt` Tool

```json
{
  "tool": "processPrompt",
  "arguments": {
    "prompt": "I need a relation between orders and customers"
  }
}
```

#### 3. MCP Server Routes to PromptEnricher

```java
private McpSchema.CallToolResult handleProcessPrompt(Object exchange, McpSchema.CallToolRequest request)
{
    String prompt = extractPromptFromRequest(request);
    String result = enricher.processPrompt(prompt);
    return McpSchema.CallToolResult.builder()
        .content(List.of(new TextContent(result)))
        .build();
}
```

#### 4. PromptEnricher Detects Intent

```java
// Generate embedding for user prompt
float[] promptEmbedding = generateEmbedding("I need a relation between orders and customers");

// Compare against all intent embeddings
// relations.npy: similarity = 0.87 ✓ (above threshold)
// value_lists.npy: similarity = 0.32 ✗ (below threshold)

String detectedIntent = "RELATIONS"; // Best match
```

#### 5. Load and Enrich Rules

```java
// Load relations.md
String rules = loadRulesForIntent("RELATIONS");

// Substitute template variables
String projectName = getActiveProjectName(); // e.g., "my_servoy_project"
String enrichedRules = rules.replace("{{PROJECT_NAME}}", projectName);

// Return enriched prompt
return "INTENT: RELATIONS\n\n" + enrichedRules;
```

#### 6. Copilot Receives Enriched Context

The LLM now has:
- The original user message
- Full `relations.md` rules
- Project-specific context (project name substituted)

#### 7. LLM Analyzes and Calls Tool

Based on the rules in `relations.md`, the LLM:
- Identifies this as a CREATE RELATION request
- Extracts parameters: primary=orders, foreign=customers
- Calls the appropriate tool:

```json
{
  "tool": "openRelation",
  "arguments": {
    "name": "orders_to_customers",
    "primaryDataSource": "example_data/orders",
    "foreignDataSource": "example_data/customers",
    "primaryColumn": "customer_id",
    "foreignColumn": "customer_id"
  }
}
```

#### 8. Tool Executes in Eclipse

```java
private McpSchema.CallToolResult handleOpenRelation(Object exchange, McpSchema.CallToolRequest request)
{
    // Extract parameters
    String name = getArgument(request, "name");
    String primaryDS = getArgument(request, "primaryDataSource");
    String foreignDS = getArgument(request, "foreignDataSource");

    // Get Servoy model
    IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

    // Create or open relation
    Relation relation = servoyModel.getActiveProject().getEditingSolution().getRelation(name);
    if (relation == null) {
        relation = servoyModel.getActiveProject().getEditingSolution().createNewRelation(
            servoyModel.getNameValidator(),
            name,
            primaryDS,
            foreignDS,
            IQueryConstants.LEFT_OUTER_JOIN
        );
    }

    // Open editor on UI thread
    Display.getDefault().asyncExec(() -> {
        EditorUtil.openRelationEditor(relation, true);
    });

    return McpSchema.CallToolResult.builder()
        .content(List.of(new TextContent("Relation 'orders_to_customers' created successfully")))
        .build();
}
```

#### 9. User Sees Result

- Relation editor opens in Eclipse
- Copilot shows success message
- User can continue conversation

---

## Intent Detection System

### ONNX Embedding Model

**Model File:** `onnx_model_mac.onnx` (for macOS)

**Architecture:**
- Sentence transformer model (e.g., `all-MiniLM-L6-v2`)
- Converts text → 384-dimensional float vector
- Trained to create semantically similar embeddings for similar text

**Model Loading:**

```java
env = OrtEnvironment.getEnvironment();
session = env.createSession(
    PromptEnricher.class.getResourceAsStream("/models/onnx_model_mac.nnx"),
    new OrtSession.SessionOptions()
);
```

**Embedding Generation:**

```java
private float[] generateEmbedding(String text)
{
    // Tokenize text (simple whitespace split for demo)
    String[] tokens = text.toLowerCase().split("\\s+");

    // Create ONNX input tensor
    long[] inputIds = convertTokensToIds(tokens);
    OnnxTensor tensor = OnnxTensor.createTensor(env, inputIds);

    // Run inference
    OrtSession.Result result = session.run(Collections.singletonMap("input_ids", tensor));

    // Extract embedding vector
    float[][] output = (float[][])result.get(0).getValue();
    return output[0]; // Return first (and only) embedding
}
```

### Intent Matching Algorithm

**Process:**

1. **Load Pre-computed Embeddings:**
   ```java
   Map<String, List<float[]>> intentEmbeddings = loadAllIntentEmbeddings();
   // {
   //   "RELATIONS": [embedding1, embedding2, ..., embedding144],
   //   "VALUE_LISTS": [embedding1, embedding2, ..., embedding89],
   //   ...
   // }
   ```

2. **Generate User Prompt Embedding:**
   ```java
   float[] userEmbedding = generateEmbedding(userPrompt);
   ```

3. **Calculate Similarity with All Intents:**
   ```java
   Map<String, Double> intentScores = new HashMap<>();

   for (Map.Entry<String, List<float[]>> entry : intentEmbeddings.entrySet()) {
       String intent = entry.getKey();
       List<float[]> trainingEmbeddings = entry.getValue();

       // Find max similarity across all training samples for this intent
       double maxSimilarity = 0.0;
       for (float[] trainingEmbedding : trainingEmbeddings) {
           double similarity = cosineSimilarity(userEmbedding, trainingEmbedding);
           maxSimilarity = Math.max(maxSimilarity, similarity);
       }

       intentScores.put(intent, maxSimilarity);
   }
   ```

4. **Select Best Intent:**
   ```java
   String bestIntent = null;
   double bestScore = 0.0;

   for (Map.Entry<String, Double> entry : intentScores.entrySet()) {
       if (entry.getValue() > SIMILARITY_THRESHOLD && entry.getValue() > bestScore) {
           bestIntent = entry.getKey();
           bestScore = entry.getValue();
       }
   }

   return bestIntent; // null if no match above threshold
   ```

**Cosine Similarity Implementation:**

```java
private double cosineSimilarity(float[] a, float[] b)
{
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < a.length; i++) {
        dotProduct += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }

    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}
```

**Threshold Configuration:**

- **Current threshold:** 0.5
- **Meaning:** At least 50% similarity required for a match
- **Tuning:** Adjust based on false positive/negative rates
  - Higher threshold → More conservative (fewer matches)
  - Lower threshold → More liberal (more matches, potential false positives)

---

## Rules and Prompt Enrichment

### Template Variables

**Purpose:** Inject runtime context into static rule files

**Current Variables:**

| Variable | Description | Example Value |
|----------|-------------|---------------|
| `{{PROJECT_NAME}}` | Active Servoy project name | `my_servoy_project` |

**Usage in Rules:**

```markdown
**CRITICAL: Current Project Name** = {{PROJECT_NAME}}

## DISAMBIGUATION

- Project Name: {{PROJECT_NAME}} → Use listRelations()
- Database Server: example_data → Use queryDatabaseSchema(serverName="example_data")
```

**Substitution Process:**

```java
private String substituteTemplateVariables(String rules)
{
    String projectName = getProjectName();
    return rules.replace("{{PROJECT_NAME}}", projectName);
}

private String getProjectName()
{
    try {
        IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
        ServoyProject activeProject = servoyModel.getActiveProject();

        if (activeProject != null) {
            return activeProject.getProject().getName();
        }
    } catch (Exception e) {
        System.err.println("[PromptEnricher] Error getting project name: " + e.getMessage());
    }

    return "UNKNOWN";
}
```

**Future Variables (Examples):**

- `{{ACTIVE_SOLUTION}}` - Active solution name
- `{{DATABASE_SERVERS}}` - List of configured database servers
- `{{AVAILABLE_DATASOURCES}}` - List of available datasources
- `{{USER_NAME}}` - Current Eclipse user

---

## MCP Tools

### Tool Categories

#### 1. Intent Detection Tool

**Tool:** `processPrompt`

**Purpose:** Entry point for all user requests - detects intent and enriches context

**Parameters:**
- `prompt` (string, required): User's message

**Returns:**
- Enriched prompt with rules (if intent detected)
- `"PASS_THROUGH"` (if no Servoy intent detected)

**Usage Pattern:**
```
User message → processPrompt → Enriched context → LLM decides next action
```

#### 2. Execution Tools

**Tools:** `openRelation`, `deleteRelation`, `listRelations`, `queryDatabaseSchema`, `openValueList`, etc.

**Purpose:** Execute specific operations in Eclipse

**Characteristics:**
- Called by LLM after analyzing enriched context
- Perform actual Eclipse operations (create relations, open editors, etc.)
- Return success/error messages
- Run on Eclipse UI thread when needed (using `Display.getDefault().asyncExec()`)

### Tool Implementation Pattern

**Standard Pattern:**

```java
private McpSchema.CallToolResult handleToolName(Object exchange, McpSchema.CallToolRequest request)
{
    System.out.println("\n[MCP] ========================================");
    System.out.println("[MCP] toolName called");
    System.out.println("[MCP] ========================================");

    // 1. Extract parameters
    String param1 = extractParameter(request, "param1");
    String param2 = extractParameter(request, "param2");

    String errorMessage = null;

    try {
        // 2. Validate required parameters
        if (param1 == null || param1.trim().isEmpty()) {
            errorMessage = "The 'param1' argument is required.";
            return buildErrorResult(errorMessage);
        }

        // 3. Get Servoy model
        IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

        // 4. Perform operation
        // ... Eclipse/Servoy API calls ...

        // 5. Open editor on UI thread if needed
        Display.getDefault().asyncExec(() -> {
            EditorUtil.openSomeEditor(result, true);
        });

    } catch (Exception e) {
        errorMessage = e.getMessage();
        System.err.println("[MCP] Error: " + errorMessage);
        e.printStackTrace();
    }

    // 6. Return result
    String resultMessage = errorMessage != null ? errorMessage : "Operation successful";
    System.out.println("[MCP] Result: " + resultMessage);
    System.out.println("[MCP] ========================================\n");

    return McpSchema.CallToolResult.builder()
        .content(List.of(new TextContent(resultMessage)))
        .build();
}
```

### Relations Tools Deep Dive

#### Tool: `queryDatabaseSchema`

**Special Implementation Details:**

**Challenge:** Access database server metadata outside normal Servoy model flow

**Solution:** Use `ApplicationServerRegistry` to access server manager

```java
// Get server manager from ApplicationServerRegistry
IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();

// Get specific server
IServerInternal server = (IServerInternal)serverManager.getServer(serverName, false, false);

// Query tables
List<String> tables = server.getTableNames(false);

// Get table details
ITable table = server.getTable(tableName);
List<Column> pkColumns = table.getRowIdentColumns();
Collection<Column> allColumns = table.getColumns();
```

**Comprehensive Analysis (when no tableName provided):**

```java
// 1. List all tables
resultBuilder.append("Database Server: " + serverName + "\n");
resultBuilder.append("Tables (" + tables.size() + "):\n");

// 2. Find EXPLICIT foreign keys (from ColumnInfo metadata)
for (String tName : tables) {
    ITable t = server.getTable(tName);
    Collection<Column> columns = t.getColumns();

    for (Column col : columns) {
        ColumnInfo colInfo = col.getColumnInfo();
        if (colInfo != null) {
            String fkTarget = colInfo.getForeignType();
            if (fkTarget != null && !fkTarget.trim().isEmpty()) {
                // This column has explicit FK metadata
                resultBuilder.append(t.getSQLName() + "." + col.getName() + " → " + fkTarget + "\n");
            }
        }
    }
}

// 3. Find POTENTIAL relations (PK name + type matching)
for (String tName : tables) {
    ITable t = server.getTable(tName);
    List<Column> pkCols = t.getRowIdentColumns();

    for (Column pkCol : pkCols) {
        String pkName = pkCol.getName();
        ColumnType pkType = pkCol.getColumnType();

        // Look for matching columns in OTHER tables
        for (String otherTableName : tables) {
            if (otherTableName.equalsIgnoreCase(tName)) continue; // Skip same table

            ITable otherTable = server.getTable(otherTableName);
            Collection<Column> otherColumns = otherTable.getColumns();

            for (Column otherCol : otherColumns) {
                // Match on name (case-insensitive) AND type
                if (otherCol.getName().equalsIgnoreCase(pkName) &&
                    pkType.equals(otherCol.getColumnType())) {

                    // Check if already an explicit FK
                    boolean isExplicitFK = /* check colInfo.getForeignType() */;

                    if (!isExplicitFK) {
                        // This is a POTENTIAL relation
                        resultBuilder.append(otherTable.getSQLName() + "." + otherCol.getName() +
                                           " → " + t.getSQLName() + "." + pkName + " (PK match)\n");
                    }
                }
            }
        }
    }
}
```

**Key Innovation:** Single tool call returns:
- ✅ All tables in database
- ✅ All explicit FK relationships
- ✅ All potential relations (by PK matching)

**Before this enhancement:**
- User: "Show me all relations in example_data"
- LLM: Calls `queryDatabaseSchema` **13 times** (once per table)
- User experience: Slow, inefficient

**After this enhancement:**
- User: "Show me all relations in example_data"
- LLM: Calls `queryDatabaseSchema` **1 time**
- User experience: Fast, complete answer

---

## Out-of-Context Handling

### The Problem

**Scenario:**
```
User: "I need a relation between orders and customers"
[LLM receives relations.md context]

User: "Actually, can you create a value list with countries?"
[LLM still has relations.md context - WRONG CONTEXT!]
```

**Challenge:** How does the LLM know to switch contexts?

### The Solution

**Add to EVERY rule file:**

```markdown
## ⚠️ CRITICAL: HANDLING OUT-OF-CONTEXT PROMPTS ⚠️

### ✅ CASE 1: Servoy-related but DIFFERENT topic

If the user's message is about OTHER Servoy features but NOT about [CURRENT_INTENT]:

→ **IMMEDIATELY call the `processPrompt` tool again** with the user's exact message.

**Examples:**
- User: "I need a value list with countries" → Call processPrompt
- User: "Open the customers form" → Call processPrompt
- User: "Create a calculation" → Call processPrompt

### ❌ CASE 2: Completely unrelated to Servoy

If the user's message is NOT related to Servoy at all:

→ **DO NOT call any tools**. Respond directly:

"I'm here to help with Servoy development tasks only.

For general questions unrelated to Servoy, please use a general-purpose assistant."

**Examples:**
- User: "What is the capital of France?" → Generic response
- User: "Tell me a joke" → Generic response
```

### Workflow Flow with Context Switching

```
1. User: "I need a relation between orders and customers"
   → LLM calls processPrompt
   → Returns: INTENT: RELATIONS + relations.md rules
   → LLM analyzes and calls openRelation

2. User: "Now create a value list with countries"
   → LLM reads relations.md rules
   → Sees: "This is about value lists, NOT relations"
   → Sees rule: "Call processPrompt again for different topics"
   → LLM calls processPrompt("Now create a value list with countries")
   → Returns: INTENT: VALUE_LISTS + value_lists.md rules
   → LLM now has correct context
   → LLM calls openValueList

3. User: "What's the weather today?"
   → LLM reads value_lists.md rules
   → Sees: "This is NOT about Servoy at all"
   → Sees rule: "Respond with generic message, don't call tools"
   → LLM responds: "I'm here to help with Servoy development tasks only..."
```

### Key Benefits

✅ **Dynamic context switching** - Conversation can flow naturally between topics
✅ **No recursive tool calls** - LLM makes the decision, not Java code
✅ **Clean boundaries** - Clear distinction between Servoy and non-Servoy topics
✅ **User-friendly** - Handles off-topic gracefully without errors

---

## File Structure

```
com.servoy.eclipse.aibridge/
├── src/
│   ├── com/servoy/eclipse/mcp/
│   │   ├── McpServletProvider.java          # MCP server & tool handlers
│   │   └── ai/
│   │       └── PromptEnricher.java          # Intent detection & enrichment
│   │
│   └── main/resources/
│       ├── intents/
│       │   ├── relations.txt                # Training prompts for RELATIONS
│       │   ├── relations.npy                # Pre-computed embeddings
│       │   ├── value_lists.txt              # Training prompts for VALUE_LISTS
│       │   └── value_lists.npy              # Pre-computed embeddings
│       │
│       ├── rules/
│       │   ├── relations.md                 # Comprehensive RELATIONS rules
│       │   └── value_lists.md               # Comprehensive VALUE_LISTS rules
│       │
│       ├── models/
│       │   └── onnx_model_mac.onnx          # ONNX embedding model (macOS)
│       │
│       └── doc/
│           ├── copilot-instructions.md      # Instructions for Copilot integration
│           ├── onnx_extension_mac.md        # ONNX model documentation
│           └── mcp-workflow-architecture.md # This document
```

---

## Development Guidelines

### Adding a New Intent

**Step 1: Create Training File**

Create `src/main/resources/intents/[intent_name].txt`:

```
[100+ example prompts that represent this intent]
Create a new form
Open the customers form
Show me all forms
Delete the old_form
I need a form for invoices
...
```

**Guidelines:**
- Aim for 100-200 diverse prompts
- Include variations (formal, casual, technical, business language)
- Cover all operations within the intent (create, open, delete, list, query, etc.)
- Include edge cases and ambiguous phrasing

**Step 2: Generate Embeddings**

Run external Python script to generate `.npy` file:

```bash
python generate_embeddings.py --input intents/[intent_name].txt --output intents/[intent_name].npy
```

**Step 3: Create Rules File**

Create `src/main/resources/rules/[intent_name].md`:

Follow the standard structure:
1. Goal statement
2. Available tools
3. Tool details (parameters, examples)
4. Workflow decision tree
5. Disambiguation rules
6. Complete examples
7. Important rules checklist
8. **Out-of-context handling section** ⚠️
9. Final notes

**Step 4: Implement Tools**

Add tool handlers to `McpServletProvider.java`:

```java
// Register tool
Tool myNewTool = McpSchema.Tool.builder()
    .inputSchema(new JsonSchema("object", null, null, null, null, null))
    .name("myNewTool")
    .description("Description for the LLM")
    .build();

server.addTool(SyncToolSpecification.builder()
    .tool(myNewTool)
    .callHandler(this::handleMyNewTool)
    .build());

// Implement handler
private McpSchema.CallToolResult handleMyNewTool(Object exchange, McpSchema.CallToolRequest request)
{
    // Follow standard pattern (see Tool Implementation Pattern above)
}
```

**Step 5: Update PromptEnricher**

Ensure `PromptEnricher.java` loads your new intent:

```java
// Should automatically detect new .npy files in /intents/ directory
// Verify by checking logs during startup
```

**Step 6: Test**

```
1. Start Eclipse with MCP server
2. Open Copilot
3. Test various prompts from your training file
4. Verify correct intent detection
5. Verify correct tool calls
6. Test context switching (out-of-context prompts)
```

### Best Practices

#### Training File Quality

✅ **DO:**
- Include 100+ diverse prompts
- Use natural language variations
- Cover all operations
- Include domain-specific terminology
- Test with real user queries

❌ **DON'T:**
- Copy-paste similar prompts (reduces diversity)
- Use only technical language (include business terms too)
- Forget edge cases
- Ignore common misspellings or informal phrasing

#### Rules File Quality

✅ **DO:**
- Start with standard structure
- Include comprehensive examples
- Add decision trees for complex workflows
- Highlight critical rules with formatting
- Include out-of-context handling section
- Use template variables for dynamic content
- Test rules with actual LLM before finalizing

❌ **DON'T:**
- Assume LLM knows Servoy terminology (define everything)
- Skip disambiguation rules (these are critical!)
- Write ambiguous instructions
- Forget to include the out-of-context section

#### Tool Implementation

✅ **DO:**
- Follow standard pattern
- Add comprehensive logging
- Validate all required parameters
- Use try-catch for error handling
- Run UI operations on Display.getDefault().asyncExec()
- Return clear success/error messages
- Test edge cases (null values, missing resources, etc.)

❌ **DON'T:**
- Block the UI thread
- Assume parameters are valid
- Swallow exceptions silently
- Return cryptic error messages
- Skip logging

### Debugging Tips

**Intent Detection Issues:**

```java
// Add debug logging in PromptEnricher.java
System.out.println("[DEBUG] User prompt: " + userPrompt);
System.out.println("[DEBUG] Prompt embedding (first 5 dims): " + Arrays.toString(Arrays.copyOf(promptEmbedding, 5)));

for (Map.Entry<String, Double> entry : intentScores.entrySet()) {
    System.out.println("[DEBUG] " + entry.getKey() + " score: " + entry.getValue());
}

System.out.println("[DEBUG] Best intent: " + bestIntent + " (score: " + bestScore + ")");
```

**Tool Call Issues:**

```java
// Already included in standard pattern
System.out.println("[MCP] toolName called");
System.out.println("[MCP] Arguments: " + request.arguments());
System.out.println("[MCP] Extracted param1: " + param1);
```

**Rules Not Applied:**

```java
// Check if rules loaded correctly
String rules = loadRulesForIntent(detectedIntent);
System.out.println("[DEBUG] Rules loaded for " + detectedIntent + ": " + rules.length() + " chars");
System.out.println("[DEBUG] First 200 chars: " + rules.substring(0, Math.min(200, rules.length())));
```

### Performance Considerations

**ONNX Model Loading:**

- ⚠️ Loading ONNX model is expensive (~500ms)
- ✅ Use singleton pattern: `final PromptEnricher enricher = new PromptEnricher();`
- ✅ Load once at server startup, reuse for all requests

**Embedding Generation:**

- ⏱️ ~50-100ms per prompt
- ✅ Acceptable for interactive use
- ✅ Pre-compute training embeddings offline (stored in `.npy` files)

**Intent Matching:**

- ⏱️ ~1-5ms (pure cosine similarity calculations)
- ✅ Very fast - not a bottleneck
- ✅ Scales linearly with number of intents and training samples

**Total Latency:**

```
processPrompt call:
├─ Embedding generation: 50-100ms
├─ Intent matching: 1-5ms
├─ Rule loading: <1ms
├─ Template substitution: <1ms
└─ TOTAL: ~100-150ms (acceptable for interactive use)
```

---

## Future Enhancements

### Planned Improvements

1. **Multi-platform ONNX Models**
   - Currently: macOS only (`onnx_model_mac.onnx`)
   - Planned: Windows (`onnx_model_win.onnx`), Linux (`onnx_model_linux.onnx`)
   - Auto-detect platform and load appropriate model

2. **Intent Confidence Scoring**
   - Currently: Binary match/no-match (threshold = 0.5)
   - Planned: Return confidence score to LLM
   - LLM can ask clarifying questions for low-confidence matches

3. **Multi-intent Detection**
   - Currently: Single best intent
   - Planned: Detect multiple intents in complex requests
   - Example: "Create a relation and a value list" → RELATIONS + VALUE_LISTS

4. **Fine-tuned Models**
   - Currently: General-purpose sentence transformer
   - Planned: Fine-tune on Servoy-specific corpus
   - Expected: Better accuracy, lower threshold needed

5. **Dynamic Template Variables**
   - Currently: `{{PROJECT_NAME}}` only
   - Planned: Add more runtime context
   - Examples: `{{DATABASE_SERVERS}}`, `{{ACTIVE_SOLUTION}}`, `{{RECENT_FILES}}`

6. **Intent Analytics**
   - Track which intents are most common
   - Identify low-confidence matches
   - Optimize training data based on real usage

7. **Caching Layer**
   - Cache embeddings for repeated prompts
   - Reduce latency for common requests
   - Clear cache on training data updates

### Research Areas

1. **Hybrid Intent Detection**
   - Combine embedding similarity with keyword matching
   - Fallback to keyword-based detection if embedding fails
   - Example: "relation" keyword → strongly suggests RELATIONS intent

2. **Contextual History**
   - Track conversation history
   - Use previous intents to improve current detection
   - Example: "Now do the same for products" → infer intent from previous message

3. **Error Recovery**
   - Detect when LLM calls wrong tool
   - Automatically re-detect intent and retry
   - Learn from errors to improve training data

---

## Troubleshooting

### Common Issues

#### Issue: Intent not detected (returns PASS_THROUGH)

**Symptoms:**
- User prompt should match an intent but returns `PASS_THROUGH`
- Copilot doesn't get enriched context

**Diagnosis:**
```java
// Check similarity scores
System.out.println("[DEBUG] Intent scores:");
for (Map.Entry<String, Double> entry : intentScores.entrySet()) {
    System.out.println("  " + entry.getKey() + ": " + entry.getValue());
}
```

**Possible Causes:**
1. **Similarity below threshold (0.5)**
   - Solution: Add similar prompts to training file, regenerate embeddings
   - Or: Lower threshold (carefully - may increase false positives)

2. **Training embeddings not loaded**
   - Check: `intentEmbeddings.containsKey("RELATIONS")`
   - Solution: Verify `.npy` file exists and is readable

3. **ONNX model not loaded**
   - Check logs for model loading errors
   - Solution: Verify model file path and platform compatibility

#### Issue: Wrong intent detected

**Symptoms:**
- User asks about relations, gets value list context
- LLM calls wrong tools

**Diagnosis:**
- Check which intent scored highest
- Compare prompt to training samples

**Solutions:**
1. Add more diverse prompts to correct intent training file
2. Remove ambiguous prompts from incorrect intent training file
3. Add disambiguation rules to rules files

#### Issue: Tool fails with null pointer

**Symptoms:**
- Tool handler throws NullPointerException
- Error logged in console

**Diagnosis:**
```java
System.out.println("[MCP] Arguments: " + request.arguments());
System.out.println("[MCP] Parameter 'name': " + args.get("name"));
```

**Solutions:**
1. Always validate required parameters before use
2. Add null checks before accessing Servoy model objects
3. Use try-catch to handle unexpected null values gracefully

#### Issue: Editor doesn't open

**Symptoms:**
- Tool returns success but editor doesn't appear
- No UI update in Eclipse

**Diagnosis:**
- Check if operation runs on UI thread

**Solution:**
```java
// WRONG - runs on MCP server thread
EditorUtil.openRelationEditor(relation, true);

// CORRECT - runs on Eclipse UI thread
Display.getDefault().asyncExec(() -> {
    EditorUtil.openRelationEditor(relation, true);
});
```

#### Issue: Context switching doesn't work

**Symptoms:**
- User changes topic but LLM doesn't call processPrompt again
- LLM tries to use wrong tools

**Diagnosis:**
- Check if out-of-context section exists in rules file
- Verify section is properly formatted and highlighted

**Solution:**
- Add/update out-of-context handling section in rules file
- Use emoji warnings (⚠️ ✅ ❌) to make it stand out
- Test with various context-switching scenarios

---

## Appendix: Key APIs

### Servoy Model APIs

```java
// Get Servoy model
IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

// Get active project
ServoyProject activeProject = servoyModel.getActiveProject();
String projectName = activeProject.getProject().getName();

// Get active solution
Solution solution = activeProject.getEditingSolution();

// Relations
Relation relation = solution.getRelation("relation_name");
Relation newRelation = solution.createNewRelation(validator, name, primaryDS, foreignDS, joinType);
Iterator<Relation> allRelations = solution.getRelations(true);

// Value Lists
ValueList valueList = solution.getValueList("valuelist_name");
ValueList newValueList = solution.createNewValueList(validator, name);

// Database access via ApplicationServerRegistry
IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
IServerInternal server = (IServerInternal)serverManager.getServer(serverName, false, false);
List<String> tables = server.getTableNames(false);
ITable table = server.getTable(tableName);
List<Column> pkColumns = table.getRowIdentColumns();
Collection<Column> allColumns = table.getColumns();
```

### Eclipse UI APIs

```java
// Run on UI thread
Display.getDefault().asyncExec(() -> {
    // UI operations here
});

// Open editors
EditorUtil.openRelationEditor(relation, true);
EditorUtil.openValueListEditor(valueList, true);
```

### MCP APIs

```java
// Tool registration
Tool tool = McpSchema.Tool.builder()
    .inputSchema(new JsonSchema("object", null, null, null, null, null))
    .name("toolName")
    .description("Description")
    .build();

server.addTool(SyncToolSpecification.builder()
    .tool(tool)
    .callHandler(this::handleTool)
    .build());

// Extract parameters
Map<String, Object> args = request.arguments();
String param = args.get("paramName").toString();

// Return result
return McpSchema.CallToolResult.builder()
    .content(List.of(new TextContent(resultMessage)))
    .build();
```

### ONNX Runtime APIs

```java
// Load model
OrtEnvironment env = OrtEnvironment.getEnvironment();
OrtSession session = env.createSession(modelPath, new OrtSession.SessionOptions());

// Create tensor
OnnxTensor tensor = OnnxTensor.createTensor(env, inputData);

// Run inference
OrtSession.Result result = session.run(Collections.singletonMap("input_ids", tensor));

// Extract output
float[][] output = (float[][])result.get(0).getValue();
```

---

## Conclusion

This MCP workflow architecture provides a robust, extensible foundation for AI-powered Servoy development assistance. The key innovations are:

1. **Unified Intent Architecture** - One intent = one training file + one rules file + multiple tools
2. **ONNX-based Intent Detection** - Fast, accurate, offline embedding-based matching
3. **Comprehensive Rules** - Detailed workflow guides for the LLM
4. **Out-of-Context Handling** - Seamless context switching during conversations
5. **Template Variables** - Dynamic runtime context injection
6. **Single-call Comprehensive Analysis** - Efficient tools that return complete information

The system is designed to be:
- **Extensible** - Easy to add new intents and tools
- **Maintainable** - Clear separation of concerns, standard patterns
- **Performant** - Fast intent detection, efficient tool execution
- **User-friendly** - Natural conversations, graceful error handling

For questions or contributions, refer to the main project documentation.

---

**Document Version:** 1.0
**Last Updated:** 2025-01-19
**Author:** Generated from MCP workflow implementation analysis
