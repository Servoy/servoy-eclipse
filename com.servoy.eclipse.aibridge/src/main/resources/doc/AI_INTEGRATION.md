# AI Integration with LangChain4J and ONNX

## Overview

The MCP provider uses **LangChain4J with ONNX Runtime** for local AI-powered intent detection and prompt enrichment. The system uses **BGE-small-en-v1.5** embedding model with a custom ONNX tokenizer for high-quality semantic search.

## Architecture

```
Eclipse Startup
    ↓
Activator.start() → Pre-load embedding service (background)
    ↓
[Knowledge base ready]
    ↓
User Prompt
    ↓
McpServletProvider.processPromptWithAgent()
    ↓
PromptEnricher.processPrompt()
    ↓
IntentDetector.detectIntent() ← Uses ONNX embeddings
    ↓
ServoyEmbeddingService.search() ← In-memory vector store
    ↓
Return: Enriched Prompt or "PASS_THROUGH"
```

## Components

### 1. **ServoyEmbeddingService** (`com.servoy.eclipse.mcp.ai.ServoyEmbeddingService`)
- **Purpose**: Manages embeddings and semantic search
- **Model**: BGE-small-en-v1.5 (ONNX, ~133MB, 384 dimensions)
- **Tokenizer**: ONNX tokenizer with ONNX Runtime Extensions
- **Storage**: In-memory vector store (LangChain4j)
- **Pre-loaded Knowledge**: Intent examples loaded at Eclipse startup
- **Initialization**: Background job in Activator (non-blocking)

### 2. **IntentDetector** (`com.servoy.eclipse.mcp.ai.IntentDetector`)
- **Purpose**: Detects user intent using semantic similarity
- **Method**: Embedding-based search (finds most similar intent example)
- **Intents**: 
  - `RELATION_CREATE`, `RELATION_OPEN`, `RELATION_EDIT`
  - `VALUELIST_CREATE`, `VALUELIST_OPEN`
  - `PASS_THROUGH` (non-Servoy tasks)

### 3. **PromptEnricher** (`com.servoy.eclipse.mcp.ai.PromptEnricher`)
- **Purpose**: Enriches prompts with Servoy-specific rules and examples
- **Process**:
  1. Detect intent
  2. Route to appropriate enrichment method
  3. Return enriched prompt with JSON schemas, examples, and instructions

## Setup

### 1. Required Dependencies

The following JARs must be in the `lib/` directory:

```
lib/
├── langchain4j-1.7.1.jar
├── langchain4j-core-1.7.1.jar
├── onnxruntime-1.19.2.jar
└── onnxruntime-extensions-0.15.0.jar
```

### 2. ONNX Runtime Extensions

Build the ONNX Runtime Extensions library (required for ONNX tokenizer):

See `src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md` for complete build instructions.

**Quick summary:**
```bash
cd /Volumes/Servoy/git/master
git clone https://github.com/microsoft/onnxruntime-extensions.git
cd onnxruntime-extensions
# Fix Gradle version for Java 21
./build.sh -DOCOS_BUILD_JAVA=ON
cp out/Darwin/RelWithDebInfo/java/build/libs/onnxruntime-extensions-0.15.0.jar \
   ../servoy-eclipse/com.servoy.eclipse.aibridge/lib/
```

### 3. Update MANIFEST.MF

```manifest
Bundle-ClassPath: .,
 lib/langchain4j-1.7.1.jar,
 lib/langchain4j-core-1.7.1.jar,
 lib/onnxruntime-1.19.2.jar,
 lib/onnxruntime-extensions-0.15.0.jar
```

### 4. Update .classpath (for Eclipse IDE)

```xml
<classpathentry kind="lib" path="lib/onnxruntime-1.19.2.jar"/>
<classpathentry kind="lib" path="lib/onnxruntime-extensions-0.15.0.jar"/>
<classpathentry kind="lib" path="lib/langchain4j-1.7.1.jar"/>
<classpathentry kind="lib" path="lib/langchain4j-core-1.7.1.jar"/>
```

### 5. Restart Eclipse

The AI service initializes automatically at startup via `Activator.start()` in a background job.

## Usage

### From McpServletProvider

```java
private String processPromptWithAgent(String prompt) {
    PromptEnricher enricher = new PromptEnricher();
    return enricher.processPrompt(prompt);
}
```

### Testing

**Test prompts:**
- "Create a relation between orders and customers" → Detects `RELATION_CREATE`
- "Make a value list with Active, Inactive, Pending" → Detects `VALUELIST_CREATE`
- "Explain this code" → Detects `PASS_THROUGH`

**Console output (at Eclipse startup):**
```
[Activator] Pre-loading Servoy AI knowledge base...
[ServoyEmbeddings] Initializing ONNX embedding service with ONNX tokenizer...
[ServoyEmbeddings] ONNX Runtime initialized
[ServoyEmbeddings] Loading ONNX embedding model...
[ServoyEmbeddings] Model loaded (126 MB), creating session...
[ServoyEmbeddings] ONNX model session created
[ServoyEmbeddings] Loading ONNX tokenizer...
[ServoyEmbeddings] Tokenizer loaded (226 KB), creating session...
[ServoyEmbeddings] ONNX Runtime Extensions registered
[ServoyEmbeddings] ONNX tokenizer session created
[ServoyEmbeddings] Loading knowledge base from /embeddings/ directory...
[ServoyEmbeddings] Loaded 3 examples for intent: RELATION_CREATE
[ServoyEmbeddings] Loaded 2 examples for intent: RELATION_OPEN
[ServoyEmbeddings] Knowledge base loaded with 9 examples
[ServoyEmbeddings] Embedding service ready!
[Activator] Servoy AI knowledge base loaded successfully
```

**Console output (on prompt):**
```
[IntentDetector] Prompt: "Create a relation between orders and customers"
[ServoyEmbeddings] === SEMANTIC SEARCH ===
[ServoyEmbeddings] Query: "Create a relation between orders and customers"
[ServoyEmbeddings] Tokenizing with ONNX tokenizer: "Create a relation..."
[ServoyEmbeddings] Token count: 12
[ServoyEmbeddings]   ✓ Match: "Create a relation between orders and customers" (similarity: 0.982)
[IntentDetector] Best match: "Create a relation between orders and customers" (score: 0.982)
[IntentDetector] Detected intent: RELATION_CREATE
[MCP] Result: ENRICHED
```

## Extending the Knowledge Base

### Add New Intent Examples

Create a new file in `src/main/resources/embeddings/`:

**Example: `form_create.txt`**
```
Create a new form
Make a form with fields
Add a form to the solution
```

Add to `embeddings.list`:
```
relation_create.txt
valuelist_create.txt
form_create.txt
```

The system will automatically load it at startup.

### Add New Intent Handler

```java
// In PromptEnricher.processPrompt()
case "FORM_CREATE" -> enrichFormCreate(prompt);
```

## Performance

- **Startup time**: ~3-5 seconds (background, non-blocking)
- **Model loading**: ~2 seconds (ONNX model + tokenizer)
- **Knowledge base loading**: ~1 second (9 examples)
- **Intent detection**: ~100-200ms per prompt (includes tokenization)
- **Memory usage**: ~300MB (model + tokenizer + embeddings)
- **Fully local**: No API calls, no network required

## Dependencies

- `langchain4j-core`: 1.7.1
- `langchain4j`: 1.7.1
- `onnxruntime`: 1.19.2
- `onnxruntime-extensions`: 0.15.0 (custom build)
- In-memory vector store (LangChain4j built-in)

## Future Enhancements

1. **Multi-platform support**: Build ONNX Runtime Extensions for Windows/Linux
2. **Multiple intent detection**: Detect multiple intents in a single prompt
3. **Larger knowledge base**: Add more Servoy examples (forms, scopes, methods, etc.)
4. **Context retrieval**: Embed Servoy docs, schemas, code examples
5. **Persistent storage**: Save embeddings to disk for faster startup
6. **Dynamic learning**: Add user interactions to knowledge base

## Troubleshooting

### "ClassNotFoundException: ai.onnxruntime.extensions.OrtxPackage"
- Build and add `onnxruntime-extensions-0.15.0.jar` to `lib/`
- See `src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md`
- Update MANIFEST.MF and .classpath

### "Error: BertTokenizer is not a registered function/op"
- ONNX Runtime Extensions not registered
- Check that `sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())` is called
- Verify JAR contains native libraries for your platform

### "Model file not found in resources"
- Ensure `model.onnx` and `tokenizer.onnx` are in `src/main/resources/models/bge-small-en-v1.5/`
- Check build includes resources folder

### "Low intent detection accuracy"
- Add more training examples in `/embeddings/*.txt` files
- Adjust `minScore` threshold in `ServoyEmbeddingService.search()` (default: 0.7)
- Check similarity scores in console output

### Slow startup
- Normal: ~3-5 seconds for model loading (background job)
- Check console for errors during initialization
- Verify ONNX Runtime Extensions native library loads correctly

## Notes

- Embedding service is singleton (initialized once at startup)
- Thread-safe for concurrent requests
- All AI logic is in `com.servoy.eclipse.mcp.ai` package
- Knowledge base files in `src/main/resources/embeddings/`
- ONNX models in `src/main/resources/models/bge-small-en-v1.5/`
