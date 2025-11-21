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
- Training file: `relations.txt` (~280 sample prompts)
- Rules file: `relations.md` (comprehensive workflow guide)
- Tools: `openRelation`, `deleteRelation`, `listRelations`, `queryDatabaseSchema`

**Example - VALUELISTS Intent:**
- Training file: `valuelists.txt` (~270 sample prompts)
- Rules file: `valuelists.md` (comprehensive workflow guide)
- Tools: `openValueList`, `deleteValueList`, `listValueLists`, `queryDatabaseSchema`, `getTableColumns` (shared)

---

## Component Details

### 1. MCP Server (`McpServletProvider.java`)

**Location:** `src/com/servoy/eclipse/mcp/McpServletProvider.java`

**Purpose:** Main orchestrator that hosts the MCP server and delegates tool registration to handler classes.

**Key Responsibilities:**
- Initializes MCP server with capabilities
- Registers `processPrompt` tool for AI-powered intent detection
- Delegates domain-specific tool registration to handler classes
- Manages prompt enrichment through PromptEnricher

**Important Implementation Details:**

```java
// Singleton pattern for PromptEnricher to avoid reloading models
final PromptEnricher enricher = new PromptEnricher();

@Override
public Set<ServletInstance> getServletInstances(String context)
{
    // Initialize MCP server
    McpSyncServer server = McpServer.sync(transportProvider)...build();

    // Register processPrompt tool (AI-powered intent detection)
    server.addTool(processPromptSpec);

    // AUTO-REGISTRATION: Automatically discover and register all tool handlers
    registerHandlers(server);

    return Set.of(new ServletInstance(transportProvider, "/mcp"));
}

private void registerHandlers(McpSyncServer server)
{
    // Get all registered handlers from ToolHandlerRegistry
    IToolHandler[] handlers = ToolHandlerRegistry.getHandlers();
    
    // Register each handler
    for (IToolHandler handler : handlers)
    {
        try
        {
            handler.registerTools(server);
            ServoyLog.logInfo("[MCP] Registered handler: " + handler.getHandlerName());
        }
        catch (Exception e)
        {
            ServoyLog.logError("[MCP] Failed to register handler: " + handler.getHandlerName(), e);
        }
    }
}
```

**Key Methods:**
- `handleProcessPrompt()` - Routes to PromptEnricher for intent detection and prompt enrichment
- `registerHandlers()` - Auto-discovers and registers all tool handlers from `ToolHandlerRegistry`

---

### 1a. Tool Handlers (Handler Classes)

**Location:** `src/com/servoy/eclipse/mcp/handlers/`

**Purpose:** Domain-specific tool registration and handling, organized by intent area.

**Architecture Pattern:**
- Each handler class implements `IToolHandler` interface
- Each handler manages tools for a specific domain (Relations, ValueLists, Common)
- **Map-based tool registration** eliminates boilerplate
- Instance methods (not static) for clean object-oriented design
- Clean separation of concerns

**Handler Classes:**

#### RelationToolHandler.java
- `openRelation` - Creates/opens relations
- `deleteRelation` - Deletes relations
- `listRelations` - Lists all relations in solution

#### ValueListToolHandler.java
- `openValueList` - Creates/opens value lists (custom or database-based)
- `deleteValueList` - Deletes value lists
- `listValueLists` - Lists all value lists in solution

#### CommonToolHandler.java
- `queryDatabaseSchema` - Queries database servers for tables and FKs
- `getTableColumns` - Retrieves detailed column information for specific table

**New Handler Structure (Map-Based Registration):**

```java
public class RelationToolHandler implements IToolHandler
{
    @Override
    public String getHandlerName()
    {
        return "RelationToolHandler";
    }

    // All tools defined in one place
    private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
    {
        Map<String, ToolHandlerRegistry.ToolDefinition> tools = new LinkedHashMap<>();
        
        tools.put("openRelation", new ToolHandlerRegistry.ToolDefinition(
            "Opens an existing database relation or creates a new relation...",
            this::handleOpenRelation));
        
        tools.put("deleteRelation", new ToolHandlerRegistry.ToolDefinition(
            "Deletes an existing database relation...",
            this::handleDeleteRelation));
        
        tools.put("listRelations", new ToolHandlerRegistry.ToolDefinition(
            "Retrieves a list of all existing database relations...",
            this::handleListRelations));
        
        return tools;
    }

    @Override
    public void registerTools(McpSyncServer server)
    {
        // Generic iteration - same for all handlers
        for (Map.Entry<String, ToolHandlerRegistry.ToolDefinition> entry : getToolDefinitions().entrySet())
        {
            ToolHandlerRegistry.registerTool(server, entry.getKey(), 
                entry.getValue().description, entry.getValue().handler);
        }
    }

    // Handler methods (implementation)
    private McpSchema.CallToolResult handleOpenRelation(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
    {
        // Implementation
    }
}
```

**Key Benefits:**
- ✅ **No boilerplate** - `registerXxx()` methods eliminated
- ✅ **All tool metadata in one place** - `getToolDefinitions()` method
- ✅ **Uniform pattern** - All handlers use same registration logic
- ✅ **Easy to add tools** - Just add one entry to the map
- ✅ **Type-safe** - No reflection, compiler catches errors

**Logging Standards:**
- Use `ServoyLog.logInfo()` for essential operations (create, delete)
- Use `ServoyLog.logError()` for exceptions with stack traces
- Prefix: `[HandlerName]` (e.g., `[RelationToolHandler]`, `[ValueListToolHandler]`)
- No verbose System.out/System.err output

---

### 1b. ToolHandlerRegistry (Central Registry & Utilities)

**Location:** `src/com/servoy/eclipse/mcp/ToolHandlerRegistry.java`

**Purpose:** Central registry for all tool handlers and shared utilities for tool registration.

**Key Components:**

#### Handler Registry

```java
public static IToolHandler[] getHandlers()
{
    return new IToolHandler[] {
        new RelationToolHandler(),
        new ValueListToolHandler(),
        new CommonToolHandler()
        // ADD NEW HANDLERS HERE - just add one line
    };
}
```

**To add a new handler:**
1. Create handler class implementing `IToolHandler`
2. Add one line to the array above
3. Done! Zero changes to `McpServletProvider`

#### ToolDefinition Helper Class

```java
public static class ToolDefinition
{
    public final String description;
    public final BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> handler;
    
    public ToolDefinition(String description, BiFunction<...> handler)
    {
        this.description = description;
        this.handler = handler;
    }
}
```

**Purpose:** Pairs a tool description with its handler method reference for map-based registration.

#### registerTool() Utility Method

```java
public static void registerTool(
    McpSyncServer server,
    String toolName,
    String description,
    BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> handler)
{
    Tool tool = McpSchema.Tool.builder()
        .inputSchema(new JsonSchema("object", null, null, null, null, null))
        .name(toolName)
        .description(description)
        .build();

    SyncToolSpecification spec = SyncToolSpecification.builder()
        .tool(tool)
        .callHandler(handler)
        .build();

    server.addTool(spec);
}
```

**Purpose:** Eliminates boilerplate code in handler classes. All handlers use this utility to register tools.

**Benefits:**
- ✅ Single source of truth for all handlers
- ✅ Shared utilities reduce code duplication
- ✅ Easy to extend - add handler in one place

---

### 2. AI Components (Intent Detection & Prompt Enrichment)

**Location:** `src/com/servoy/eclipse/mcp/ai/`

The AI pipeline consists of three main components working together:

---

#### 2a. PromptEnricher.java

**Purpose:** Orchestrator for intent detection and prompt enrichment.

**Key Responsibilities:**
- Coordinates between IntentDetector and RulesCache
- Enriches prompts with context-specific rules
- Performs template variable substitution
- Gathers Servoy context (project name, active solution)

**Implementation:**

```java
public class PromptEnricher
{
    private final IntentDetector intentDetector;

    public String processPrompt(String prompt)
    {
        // 1. Detect intent using embedding similarity
        String intent = intentDetector.detectIntent(prompt);

        // 2. Check if Servoy-related
        if (!intentDetector.isServoyIntent(intent)) {
            return "PASS_THROUGH";
        }

        // 3. Enrich with rules
        return enrichPrompt(intent, prompt);
    }

    private String enrichPrompt(String intent, String prompt)
    {
        // Load rules for detected intent
        String rules = RulesCache.getRules(intent);

        // Gather context
        String context = gatherServoyContext();
        String projectName = getProjectName();

        // Substitute template variables
        String processedRules = rules.replace("{{PROJECT_NAME}}", projectName);

        // Return enriched prompt
        return context + "\n\nUSER REQUEST:\n" + prompt + "\n\n" + processedRules;
    }
}
```

**Template Variable Substitution:**
- `{{PROJECT_NAME}}` → Active Servoy project name

---

#### 2b. IntentDetector.java

**Purpose:** Semantic intent detection using ONNX embeddings.

**Key Responsibilities:**
- Uses ServoyEmbeddingService for embedding generation
- Performs semantic similarity search
- Returns best matching intent or PASS_THROUGH

**Implementation:**

```java
public class IntentDetector
{
    private final ServoyEmbeddingService embeddingService;

    public String detectIntent(String prompt)
    {
        // Search for best matching intent using embeddings
        List<SearchResult> results = embeddingService.search(prompt, 1);

        if (!results.isEmpty()) {
            SearchResult bestMatch = results.get(0);
            String intent = bestMatch.metadata.get("intent");
            ServoyLog.logInfo("[IntentDetector] Detected intent: " + intent +
                            " (score: " + bestMatch.score + ")");
            return intent;
        }

        return "PASS_THROUGH";
    }

    public boolean isServoyIntent(String intent)
    {
        return !intent.equals("PASS_THROUGH");
    }
}
```

---

#### 2c. ServoyEmbeddingService.java

**Purpose:** ONNX-based embedding generation and semantic search engine.

**Key Responsibilities:**
- Loads ONNX model (bge-small-en-v1.5) and tokenizer from OSGi bundle
- Generates embeddings for user prompts
- Maintains in-memory knowledge base of training samples
- Performs cosine similarity search
- Singleton pattern for efficient resource usage

**Implementation:**

```java
public class ServoyEmbeddingService
{
    private static ServoyEmbeddingService instance;
    private OrtEnvironment env;
    private OrtSession modelSession;
    private OrtSession tokenizerSession;
    private List<EmbeddingEntry> knowledgeBase;

    private void initializeKnowledgeBase()
    {
        // Load training samples from embeddings.list
        // For each .txt file (e.g., relations.txt):
        String intentKey = filename.substring(0, filename.lastIndexOf('.')).toUpperCase();
        // e.g., relations.txt -> RELATIONS

        // Load examples and generate embeddings
        loadEmbeddingsFromFile("/main/resources/embeddings/" + filename, intentKey);
    }

    public List<SearchResult> search(String query, int topK)
    {
        // 1. Generate embedding for query
        float[] queryEmbedding = generateEmbedding(query);

        // 2. Calculate cosine similarity with all training samples
        List<SearchResult> results = new ArrayList<>();
        for (EmbeddingEntry entry : knowledgeBase) {
            double similarity = cosineSimilarity(queryEmbedding, entry.embedding);
            results.add(new SearchResult(entry.text, similarity, entry.metadata));
        }

        // 3. Sort by similarity (descending) and return top K
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.subList(0, Math.min(topK, results.size()));
    }
}
```

**Key Features:**
- Uses ONNX Runtime for cross-platform inference
- Loads model from `onnx-models-bge-small-en` OSGi bundle
- Generates 384-dimensional embeddings
- Singleton pattern ensures model loaded only once
- In-memory knowledge base for fast search

**Cosine Similarity Formula:**

```
similarity = (A · B) / (||A|| × ||B||)

Where:
- A = user prompt embedding
- B = training sample embedding
- Result ranges from -1 (opposite) to 1 (identical)
- Higher score = better match
```

---

#### 2d. RulesCache.java

**Purpose:** Rules file loader and caching system.

**Key Responsibilities:**
- Auto-discovers `.md` files from `rules.list`
- Loads rule content at startup
- Caches rules in memory for fast access
- Provides exact-match rule lookup by intent

**Implementation:**

```java
public class RulesCache
{
    private static final Map<String, String> rulesCache = new HashMap<>();

    static {
        autoDiscoverRules();
    }

    private static void autoDiscoverRules()
    {
        // Read rules.list manifest
        // For each .md file (e.g., relations.md):
        String baseName = filename.substring(0, filename.lastIndexOf('.'));
        String intentKey = baseName.toUpperCase();
        // e.g., relations.md -> RELATIONS

        loadRule(intentKey, "/main/resources/rules/" + filename);
    }

    public static String getRules(String intent)
    {
        // Exact match lookup
        String rules = rulesCache.get(intent);
        return rules != null ? rules : "";
    }
}
```

**Important:** Intent names must match exactly:
- `relations.txt` → intent `RELATIONS` → rule key `RELATIONS` → from `relations.md`
- `valuelists.txt` → intent `VALUELISTS` → rule key `VALUELISTS` → from `valuelists.md`

---

### 3. Intent Detection Files

**Location:** `src/main/resources/embeddings/`

#### Training Files (`.txt`)

**Format:** One prompt per line

**Purpose:**
- Training samples for intent classification
- Loaded at startup by ServoyEmbeddingService
- Embeddings generated on-the-fly using ONNX model

**Example - `relations.txt`:**
```
Create a relation between orders and customers
Make a relation from products to categories
Add a relation linking invoices to customers
I need to create a relation from employees to departments
Open the orders_to_customers relation
Delete old_relation
Show me all relations
...
```

**Example - `valuelists.txt`:**
```
create a value list with Active, Inactive, Pending
create value list called status with Active and Inactive
I need a value list with Low, Medium, High
make a value list from customers table
value list from database table countries
...
```

**Naming Convention:**
- Intent name in lowercase
- Plural form for domain intents (relations, valuelists)
- Must match exactly with rule file name (without extension)
- Example: `relations.txt` → intent `RELATIONS` → matches `relations.md`
- Example: `valuelists.txt` → intent `VALUELISTS` → matches `valuelists.md`

**Registry File:** `embeddings.list`
- Contains list of all `.txt` files to load
- Read by ServoyEmbeddingService at startup
- Format: one filename per line

**Current Intent Files:**
- `relations.txt` (~280 training samples)
- `valuelists.txt` (~270 training samples)
- `run_test_embeddings.txt` (test/development samples)

---

### 4. Rules Files (`.md`)

**Location:** `src/main/resources/rules/`

**Purpose:** Comprehensive workflow guide for the LLM after intent is detected

**Registry File:** `rules.list`
- Contains list of all `.md` files to load
- Read by RulesCache at startup
- Format: one filename per line

**Current Rule Files:**
- `relations.md` - Comprehensive guide for relation operations
- `valuelists.md` - Comprehensive guide for value list operations
- `run_test_embeddings.md` (test/development rules)

**Intent Matching:**
- Rule file basename (without `.md`) must match intent name from embeddings
- Example: `RELATIONS` intent → loads `RELATIONS` rule key → from `relations.md`
- Example: `VALUELISTS` intent → loads `VALUELISTS` rule key → from `valuelists.md`
- Matching is case-insensitive and exact

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
// IntentDetector uses ServoyEmbeddingService
List<SearchResult> results = embeddingService.search("I need a relation between orders and customers", 1);

// Compare against all training samples in knowledge base
// relations.txt samples: best match similarity = 0.87 ✓
// valuelists.txt samples: best match similarity = 0.32 ✗

SearchResult bestMatch = results.get(0);
String detectedIntent = bestMatch.metadata.get("intent"); // "RELATIONS"
```

#### 5. Load and Enrich Rules

```java
// Load rules from RulesCache (cached at startup)
String rules = RulesCache.getRules("RELATIONS"); // Loads relations.md content

// Gather Servoy context
String context = gatherServoyContext();
String projectName = getProjectName(); // e.g., "my_servoy_project"

// Substitute template variables
String processedRules = rules.replace("{{PROJECT_NAME}}", projectName);

// Return enriched prompt with context
return context + "\n\nUSER REQUEST:\n" + prompt + "\n\n" + processedRules;
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

**RelationToolHandler.handleOpenRelation()** is called with the arguments:

```java
// Extract parameters from request
String name = "orders_to_customers";
String primaryDS = "example_data/orders";
String foreignDS = "example_data/customers";
String primaryColumn = "customer_id";
String foreignColumn = "customer_id";

// Get Servoy model
IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

// Check if relation exists
Relation relation = servoyModel.getActiveProject().getEditingSolution().getRelation(name);

if (relation == null) {
    // Create new relation
    ServoyLog.logInfo("[RelationToolHandler] Creating relation: " + name);
    relation = servoyModel.getActiveProject().getEditingSolution().createNewRelation(
        servoyModel.getNameValidator(),
        name,
        primaryDS,
        foreignDS,
        IQueryConstants.LEFT_OUTER_JOIN
    );
    
    // Add column mapping if provided
    relation.createNewRelationItems(...);
}

// Open editor on UI thread
Display.getDefault().asyncExec(() -> {
    EditorUtil.openRelationEditor(relation, true);
});

return "Relation 'orders_to_customers' created successfully (from example_data/orders to example_data/customers)";
```

#### 9. User Sees Result

- Relation editor opens in Eclipse
- Copilot shows success message
- User can continue conversation

---

## Intent Detection System

### ONNX Embedding Model

**Model:** `bge-small-en-v1.5` (BAAI General Embedding)
**Location:** OSGi bundle `onnx-models-bge-small-en`
**Files:**
- `models/bge-small-en-v1.5/model.onnx` - Embedding model
- `models/bge-small-en-v1.5/tokenizer.onnx` - Tokenizer (uses ONNX Runtime Extensions)

**Architecture:**
- Sentence transformer model optimized for semantic search
- Converts text → 384-dimensional float vector
- Trained to create semantically similar embeddings for similar text

**Model Loading (from OSGi bundle):**

```java
// Get bundle
Bundle modelsBundle = Platform.getBundle("onnx-models-bge-small-en");

// Load embedding model
URL modelURL = modelsBundle.getEntry("models/bge-small-en-v1.5/model.onnx");
InputStream modelStream = modelURL.openStream();
byte[] modelBytes = modelStream.readAllBytes();
modelSession = env.createSession(modelBytes);

// Load tokenizer with ONNX Runtime Extensions
URL tokenizerURL = modelsBundle.getEntry("models/bge-small-en-v1.5/tokenizer.onnx");
InputStream tokenizerStream = tokenizerURL.openStream();
byte[] tokenizerBytes = tokenizerStream.readAllBytes();

OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath());
tokenizerSession = env.createSession(tokenizerBytes, sessionOptions);
```

**Embedding Generation:**

```java
public float[] generateEmbedding(String text)
{
    // 1. Tokenize using ONNX tokenizer
    OnnxTensor inputTensor = OnnxTensor.createTensor(env, new String[][] {{text}});
    OrtSession.Result tokenizerResult = tokenizerSession.run(
        Collections.singletonMap("inputs", inputTensor)
    );
    
    long[][] inputIds = (long[][])tokenizerResult.get("input_ids").getValue();
    long[][] attentionMask = (long[][])tokenizerResult.get("attention_mask").getValue();
    
    // 2. Generate embedding using model
    Map<String, OnnxTensor> inputs = new HashMap<>();
    inputs.put("input_ids", OnnxTensor.createTensor(env, inputIds));
    inputs.put("attention_mask", OnnxTensor.createTensor(env, attentionMask));
    
    OrtSession.Result modelResult = modelSession.run(inputs);
    
    // 3. Extract embedding vector (384 dimensions)
    float[][][] embeddings = (float[][][])modelResult.get(0).getValue();
    return embeddings[0][0]; // [batch_size=1][sequence_pos=0][embedding_dim=384]
}
```

### Intent Matching Algorithm

**Process:**

1. **Initialize Knowledge Base (at startup):**
   ```java
   List<EmbeddingEntry> knowledgeBase = new ArrayList<>();
   
   // Load and embed all training samples from embeddings.list
   for (String filename : embeddingFiles) {
       String intentKey = filename.substring(0, filename.lastIndexOf('.')).toUpperCase();
       // e.g., relations.txt -> RELATIONS
       
       List<String> samples = readLines(filename);
       for (String sample : samples) {
           float[] embedding = generateEmbedding(sample);
           knowledgeBase.add(new EmbeddingEntry(
               sample,
               embedding,
               Map.of("intent", intentKey)
           ));
       }
   }
   ```

2. **Search for Best Match (at runtime):**
   ```java
   public List<SearchResult> search(String query, int topK)
   {
       // Generate embedding for user query
       float[] queryEmbedding = generateEmbedding(query);
       
       // Calculate similarity with all training samples
       List<SearchResult> results = new ArrayList<>();
       for (EmbeddingEntry entry : knowledgeBase) {
           double similarity = cosineSimilarity(queryEmbedding, entry.embedding);
           results.add(new SearchResult(entry.text, similarity, entry.metadata));
       }
       
       // Sort by similarity (descending) and return top K
       results.sort((a, b) -> Double.compare(b.score, a.score));
       return results.subList(0, Math.min(topK, results.size()));
   }
   ```

3. **Extract Best Intent:**
   ```java
   // IntentDetector calls search with topK=1
   List<SearchResult> results = embeddingService.search(userPrompt, 1);
   
   if (!results.isEmpty()) {
       SearchResult bestMatch = results.get(0);
       String intent = bestMatch.metadata.get("intent");
       double score = bestMatch.score;
       
       ServoyLog.logInfo("[IntentDetector] Detected intent: " + intent + 
                       " (score: " + score + ")");
       return intent;
   }
   
   return "PASS_THROUGH"; // No match found
   ```

**Key Differences from Traditional Approach:**
- ✅ No pre-computed `.npy` files needed
- ✅ Embeddings generated on-the-fly at startup
- ✅ In-memory knowledge base for fast search
- ✅ Single model loading for all operations
- ✅ Cross-platform (no platform-specific `.npy` files)

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
│       ├── embeddings/
│       │   ├── relations.txt                # Training prompts for RELATIONS (~280)
│       │   ├── relations.npy                # Pre-computed embeddings
│       │   ├── valuelists.txt               # Training prompts for VALUE_LISTS (~270)
│       │   └── valuelists.npy               # Pre-computed embeddings
│       │
│       ├── rules/
│       │   ├── relations.md                 # Comprehensive RELATIONS rules
│       │   └── valuelist.md                 # Comprehensive VALUE_LISTS rules
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

### Adding a New Tool to Existing Handler

**Scenario:** You want to add a new tool to an existing handler (e.g., `renameRelation` to `RelationToolHandler`).

**Steps:**

**Step 1: Add tool to training file**

Add examples to `src/main/resources/embeddings/relations.txt`:
```
Rename the orders_to_customers relation
I need to rename a relation
Change the relation name from old_name to new_name
```

**Step 2: Add tool documentation to rules file**

Add tool section to `src/main/resources/rules/relations.md`:
```markdown
## TOOL: renameRelation

**Purpose**: Rename an existing relation

**Required Parameters**:
- oldName (string): Current relation name
- newName (string): New relation name

**Examples**: [add examples]
```

**Step 3: Add tool to handler's getToolDefinitions() map**

In `RelationToolHandler.java`:

```java
private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
{
    Map<String, ToolHandlerRegistry.ToolDefinition> tools = new LinkedHashMap<>();
    
    tools.put("openRelation", new ToolHandlerRegistry.ToolDefinition(...));
    tools.put("deleteRelation", new ToolHandlerRegistry.ToolDefinition(...));
    tools.put("listRelations", new ToolHandlerRegistry.ToolDefinition(...));
    
    // ADD NEW TOOL HERE - just add one entry!
    tools.put("renameRelation", new ToolHandlerRegistry.ToolDefinition(
        "Renames an existing relation. Required: oldName (string), newName (string).",
        this::handleRenameRelation));
    
    return tools;
}
```

**Step 4: Implement handler method**

```java
private McpSchema.CallToolResult handleRenameRelation(
    McpSyncServerExchange exchange, 
    McpSchema.CallToolRequest request)
{
    String oldName = extractParameter(request, "oldName");
    String newName = extractParameter(request, "newName");
    
    // Validation
    if (oldName == null || newName == null) {
        return buildErrorResult("Both oldName and newName are required");
    }
    
    try {
        // Implementation
        IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
        Relation relation = servoyModel.getActiveProject().getEditingSolution().getRelation(oldName);
        
        if (relation == null) {
            return buildErrorResult("Relation '" + oldName + "' not found");
        }
        
        relation.setName(newName);
        ServoyLog.logInfo("[RelationToolHandler] Renamed relation: " + oldName + " → " + newName);
        
        return McpSchema.CallToolResult.builder()
            .content(List.of(new TextContent("Relation renamed successfully")))
            .build();
    }
    catch (Exception e) {
        ServoyLog.logError("[RelationToolHandler] Error in handleRenameRelation: " + e.getMessage(), e);
        return buildErrorResult("Error: " + e.getMessage());
    }
}
```

**That's it!** No changes to:
- ❌ `McpServletProvider` (auto-registration handles it)
- ❌ `ToolHandlerRegistry` (handler already registered)
- ❌ `registerTools()` method (generic iteration handles it)

**Total effort:** Add 1 map entry + implement 1 handler method.

---

### Adding a New Handler

**Scenario:** You want to add a completely new handler (e.g., `FormToolHandler` for form operations).

**Steps:**

**Step 1: Create training file**

Create `src/main/resources/embeddings/forms.txt` with ~100+ examples

**Step 2: Add to embeddings.list**

Add to `src/main/resources/embeddings/embeddings.list`:
```
relations.txt
valuelists.txt
forms.txt
```

**Step 3: Create rules file**

Create `src/main/resources/rules/forms.md` with comprehensive rules

**Step 4: Add to rules.list**

Add to `src/main/resources/rules/rules.list`:
```
relations.md
valuelists.md
forms.md
```

**Step 5: Create handler class**

Create `src/com/servoy/eclipse/mcp/handlers/FormToolHandler.java`:

```java
public class FormToolHandler implements IToolHandler
{
    @Override
    public String getHandlerName()
    {
        return "FormToolHandler";
    }

    private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
    {
        Map<String, ToolHandlerRegistry.ToolDefinition> tools = new LinkedHashMap<>();
        
        tools.put("openForm", new ToolHandlerRegistry.ToolDefinition(
            "Opens a form in the editor...",
            this::handleOpenForm));
        
        return tools;
    }

    @Override
    public void registerTools(McpSyncServer server)
    {
        for (Map.Entry<String, ToolHandlerRegistry.ToolDefinition> entry : getToolDefinitions().entrySet())
        {
            ToolHandlerRegistry.registerTool(server, entry.getKey(), 
                entry.getValue().description, entry.getValue().handler);
        }
    }

    private McpSchema.CallToolResult handleOpenForm(
        McpSyncServerExchange exchange, 
        McpSchema.CallToolRequest request)
    {
        // Implementation
    }
}
```

**Step 6: Register in ToolHandlerRegistry**

In `ToolHandlerRegistry.getHandlers()`, add ONE line:

```java
public static IToolHandler[] getHandlers()
{
    return new IToolHandler[] {
        new RelationToolHandler(),
        new ValueListToolHandler(),
        new CommonToolHandler(),
        new FormToolHandler()  // <-- ADD THIS LINE
    };
}
```

**That's it!** The handler is now:
- ✅ Auto-discovered by `McpServletProvider`
- ✅ Auto-registered with MCP server
- ✅ Ready to use

**Total changes:**
- ✅ 4 new files (training, rules, handler, list entries)
- ✅ 1 line in `ToolHandlerRegistry.getHandlers()`
- ✅ Zero changes to `McpServletProvider`

---

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
valueList.setValueListType(IValueListConstants.CUSTOM_VALUES); // or DATABASE_VALUES
valueList.setCustomValues("Value1\nValue2\nValue3"); // for custom type
valueList.setDataSource("db:/server_name/table_name"); // for database type
valueList.setDataProviderID1("column_name"); // display column
valueList.setShowDataProviders(1); // bitmask for columns (1=col1, 2=col2, 4=col3)
valueList.setReturnDataProviders(1); // return columns bitmask
Iterator<ValueList> allValueLists = solution.getValueLists(true);

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
EditorUtil.openFormEditor(form, true);
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

**Document Version:** 1.1
**Last Updated:** 2025-11-19
**Author:** Generated from MCP workflow implementation analysis
**Recent Updates:**
- Added VALUE_LISTS intent implementation
- Updated training prompt counts (relations: ~280, valuelists: ~270)
- Added value list tool handlers and API examples
