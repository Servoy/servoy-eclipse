# LangChain4J Integration Guide

> **Status:** ✅ **IMPLEMENTED** - This integration is complete and working.  
> See `AI_INTEGRATION.md` for current architecture and `ONNX_TOKENIZER_INTEGRATION.md` for tokenizer setup.

## Overview

The MCP servlet provides a single `processPrompt` tool that routes all user prompts through the LangChain4J-based AI system for intelligent intent detection and prompt enrichment.

---

## Architecture

```
User → GitHub Copilot → MCP processPrompt Tool → Your LangChain4J Agent
                                                          ↓
                                                    Decision Logic
                                                          ↓
                                    ┌─────────────────────┴─────────────────────┐
                                    ↓                                           ↓
                            "I'll handle this"                          "Not for me"
                                    ↓                                           ↓
                    Execute Servoy operations                       Return "PASS_THROUGH"
                    Return result message                           Copilot handles it
```

---

## Implementation Steps

### 1. Current State

The `McpServletProvider.java` has:
- ✅ Single `processPrompt` MCP tool registered
- ✅ `handleProcessPrompt()` method that extracts the prompt
- ✅ Placeholder `processPromptWithAgent()` method

### 2. What You Need to Implement

Replace the `processPromptWithAgent()` method with your LangChain4J agent integration:

```java
private String processPromptWithAgent(String prompt)
{
    // 1. Call your LangChain4J agent
    String response = yourLangChain4JService.analyze(prompt);
    
    // 2. Your agent should return one of:
    //    - Success message: "Relation 'xyz' created successfully"
    //    - Error message: "Relation 'xyz' not found. Please provide..."
    //    - Pass-through: "PASS_THROUGH"
    
    return response;
}
```

### 3. Your LangChain4J Agent Should:

#### A. Analyze the Prompt
```java
// Determine intent
boolean isServoyRelated = detectServoyIntent(prompt);
if (!isServoyRelated) {
    return "PASS_THROUGH";
}
```

#### B. Extract Entities
```java
// Extract entity names and parameters
String entityType = extractEntityType(prompt); // "relation", "valuelist", "form"
String entityName = extractEntityName(prompt);
Map<String, String> parameters = extractParameters(prompt);
```

#### C. Execute Operations
```java
// Use existing Servoy APIs
IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

if (entityType.equals("relation")) {
    Relation relation = servoyModel.getActiveProject().getEditingSolution().getRelation(entityName);
    if (relation == null) {
        // Create new relation
        // Extract datasources from parameters
        // Return success message
    } else {
        // Open existing relation
        EditorUtil.openRelationEditor(relation, true);
        return "Relation '" + entityName + "' opened in editor.";
    }
}
```

---

## Response Format

Your agent must return one of these:

### 1. Success Message
```
"Relation 'orders_to_customers' created successfully"
"Value list 'status_list' opened in editor"
"Form 'customer_form' found in solution"
```

### 2. Error Message
```
"Relation 'xyz' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'."
"Value list 'abc' not found."
"Invalid datasource format: 'table_a'. Expected format: 'db:/server/table'"
```

### 3. Pass-Through Signal
```
"PASS_THROUGH"
```

---

## Example LangChain4J Agent Structure

```java
public class ServoyPromptAgent {
    
    private final ChatLanguageModel model;
    private final ServoyOperationsService servoyOps;
    
    public String processPrompt(String prompt) {
        // 1. Intent Detection
        PromptIntent intent = detectIntent(prompt);
        
        if (intent.isPassThrough()) {
            return "PASS_THROUGH";
        }
        
        // 2. Entity Extraction
        ServoyEntity entity = extractEntity(prompt, intent);
        
        // 3. Execute Operation
        try {
            String result = executeOperation(entity, intent);
            return result;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private PromptIntent detectIntent(String prompt) {
        // Use LLM to classify intent
        String classification = model.generate(
            "Classify this prompt as: RELATION_CREATE, RELATION_OPEN, " +
            "VALUELIST_CREATE, VALUELIST_OPEN, FORM_LIST, or PASS_THROUGH: " + prompt
        );
        return PromptIntent.from(classification);
    }
    
    private ServoyEntity extractEntity(String prompt, PromptIntent intent) {
        // Use LLM to extract entity details
        String extraction = model.generate(
            "Extract entity name and parameters from: " + prompt
        );
        return ServoyEntity.parse(extraction);
    }
    
    private String executeOperation(ServoyEntity entity, PromptIntent intent) {
        // Use servoyOps to execute the actual Servoy operations
        return servoyOps.execute(entity, intent);
    }
}
```

---

## Testing

### Test Case 1: Servoy Relation
```
User: "Create a relation between orders and customers"
Expected: Your agent handles it, returns success/error message
```

### Test Case 2: Non-Servoy Request
```
User: "Explain this code"
Expected: Your agent returns "PASS_THROUGH", Copilot handles it
```

### Test Case 3: Repeated Request
```
User: "I need value list my_list" (first time)
Expected: Agent creates it, returns "created successfully"

User: "I need value list my_list" (second time)
Expected: Agent opens it, returns "opened in editor"
```

---

## Benefits of This Approach

✅ **Full Control** - Your LLM decides what to handle  
✅ **Better Intent Understanding** - Use domain-specific prompts  
✅ **Flexible** - Easy to add new capabilities  
✅ **Fallback** - Copilot handles non-Servoy requests  
✅ **Single Entry Point** - No confusion about which tool to use  

---

## Next Steps

1. Implement your LangChain4J agent
2. Replace `processPromptWithAgent()` method
3. Test with various prompts
4. Refine intent detection and entity extraction
5. Add more Servoy operations as needed

---

## Questions?

The key integration point is the `processPromptWithAgent()` method in `McpServletProvider.java`.
Everything else is already set up and ready to go!
