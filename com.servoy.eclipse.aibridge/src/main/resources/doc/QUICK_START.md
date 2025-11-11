# Quick Start Guide - Servoy AI Bridge

## What is This?

The Servoy AI Bridge adds intelligent intent detection to Eclipse using local ONNX models. It detects what you want to do (create relation, value list, etc.) and enriches prompts with Servoy-specific context.

## How It Works

```
1. You type a prompt in Copilot
2. AI detects your intent (e.g., "RELATION_CREATE")
3. Prompt is enriched with Servoy schemas and examples
4. Copilot gets better context → better code generation
```

## Quick Setup

### 1. Check Dependencies

Ensure these JARs are in `lib/`:
```
lib/langchain4j-1.7.1.jar
lib/langchain4j-core-1.7.1.jar
lib/onnxruntime-1.19.2.jar
lib/onnxruntime-extensions-0.15.0.jar
```

### 2. Verify Resources

Check these files exist:
```
src/main/resources/models/bge-small-en-v1.5/model.onnx
src/main/resources/models/bge-small-en-v1.5/tokenizer.onnx
src/main/resources/embeddings/embeddings.list
```

### 3. Start Eclipse

The AI system loads automatically in the background (~3-5 seconds).

### 4. Test It

Try these prompts in Copilot:
- "Create a relation between orders and customers"
- "Make a value list with Active, Inactive, Pending"

Check console for:
```
[Activator] Pre-loading Servoy AI knowledge base...
[ServoyEmbeddings] Embedding service ready!
```

## Adding New Intents

### 1. Create Example File

Create `src/main/resources/embeddings/form_create.txt`:
```
Create a new form
Make a form with fields
Add a form to the solution
```

### 2. Add to List

Edit `embeddings.list`:
```
relation_create.txt
valuelist_create.txt
form_create.txt
```

### 3. Add Handler

In `PromptEnricher.java`:
```java
case "FORM_CREATE" -> enrichFormCreate(prompt);
```

### 4. Restart Eclipse

The new intent is automatically loaded!

## Troubleshooting

### No console output at startup?
- Check `Activator.java` has `initializeEmbeddingService()` call
- Verify background job is scheduled

### "BertTokenizer not registered" error?
- Missing `onnxruntime-extensions-0.15.0.jar`
- See `ONNX_TOKENIZER_INTEGRATION.md` for build instructions

### Slow first prompt?
- Should be fast if loaded at startup
- Check console for initialization errors

### Wrong intent detected?
- Add more examples to `/embeddings/*.txt` files
- Check similarity scores in console output
- Adjust `minScore` threshold (default: 0.7)

## File Structure

```
com.servoy.eclipse.aibridge/
├── lib/                          # JAR dependencies
├── src/
│   ├── com/servoy/eclipse/
│   │   ├── aibridge/
│   │   │   └── Activator.java   # Startup initialization
│   │   └── mcp/
│   │       ├── ai/
│   │       │   ├── ServoyEmbeddingService.java  # Core AI service
│   │       │   ├── IntentDetector.java          # Intent detection
│   │       │   └── PromptEnricher.java          # Prompt enrichment
│   │       └── McpServletProvider.java          # MCP endpoint
│   └── main/resources/
│       ├── models/bge-small-en-v1.5/
│       │   ├── model.onnx       # Embedding model (133 MB)
│       │   ├── tokenizer.onnx   # ONNX tokenizer (226 KB)
│       │   └── README_CONVERSION.md
│       └── embeddings/
│           ├── embeddings.list  # List of intent files
│           ├── relation_create.txt
│           └── valuelist_create.txt
├── AI_INTEGRATION.md            # Full architecture guide
├── ONNX_TOKENIZER_INTEGRATION.md # Tokenizer setup
├── CHANGELOG.md                 # Recent changes
└── QUICK_START.md              # This file
```

## Performance

- **Startup**: 3-5 seconds (background)
- **Per prompt**: 100-200ms
- **Memory**: ~300MB
- **Network**: None (fully local)

## Learn More

- **Architecture**: `AI_INTEGRATION.md`
- **Tokenizer Setup**: `ONNX_TOKENIZER_INTEGRATION.md`
- **Conversion Guide**: `src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md`
- **Recent Changes**: `CHANGELOG.md`

## Support

For issues or questions:
1. Check console output for errors
2. Review troubleshooting sections in docs
3. Verify all dependencies are present
4. Check ONNX Runtime Extensions build for your platform
