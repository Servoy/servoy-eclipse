# Servoy AI Bridge

Eclipse plugin that adds intelligent AI-powered intent detection and prompt enrichment for Servoy development using local ONNX models.

## Features

✨ **Local AI Intent Detection** - Detects what you want to do (create relation, value list, etc.)  
🚀 **Fast Startup** - Pre-loads at Eclipse startup in background (3-5 seconds)  
🎯 **Semantic Search** - Uses BGE-small-en-v1.5 embeddings for high-quality matching  
🔒 **Fully Local** - No API calls, no network required  
⚡ **ONNX Tokenizer** - Native tokenization with ONNX Runtime Extensions  
📚 **Extensible** - Easy to add new intents and examples  

## Quick Start

See **[QUICK_START.md](QUICK_START.md)** for setup and usage.

## Documentation

| Document | Description |
|----------|-------------|
| **[QUICK_START.md](QUICK_START.md)** | Get started in 5 minutes |
| **[AI_INTEGRATION.md](AI_INTEGRATION.md)** | Full architecture and implementation details |
| **[ONNX_TOKENIZER_INTEGRATION.md](ONNX_TOKENIZER_INTEGRATION.md)** | ONNX tokenizer setup and build instructions |
| **[CHANGELOG.md](CHANGELOG.md)** | Recent changes and version history |
| **[INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)** | LangChain4j integration guide |
| **[src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md](src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md)** | Tokenizer conversion and ONNX Extensions build |

## Architecture

```
Eclipse Startup
    ↓
Activator.start() → Pre-load AI service (background)
    ↓
[Knowledge base ready in 3-5 seconds]
    ↓
User types prompt in Copilot
    ↓
MCP processPrompt tool
    ↓
PromptEnricher → IntentDetector
    ↓
ServoyEmbeddingService (ONNX)
    ↓
Semantic search in knowledge base
    ↓
Intent detected → Prompt enriched
    ↓
Copilot gets better context
```

## Technology Stack

- **Embedding Model**: BGE-small-en-v1.5 (ONNX, 133 MB, 384 dimensions)
- **Tokenizer**: ONNX tokenizer with ONNX Runtime Extensions 0.15.0
- **Runtime**: ONNX Runtime 1.19.2
- **Framework**: LangChain4j 1.7.1
- **Storage**: In-memory vector store
- **Platform**: Eclipse OSGi plugin

## Supported Intents

Currently detects:
- `RELATION_CREATE` - Creating database relations
- `RELATION_OPEN` - Opening existing relations
- `VALUELIST_CREATE` - Creating value lists
- `VALUELIST_OPEN` - Opening value lists
- `PASS_THROUGH` - Non-Servoy tasks (handled by Copilot)

## Performance

- **Startup**: 3-5 seconds (background, non-blocking)
- **Intent detection**: 100-200ms per prompt
- **Memory usage**: ~300MB
- **Network**: None (fully local)

## Requirements

### Runtime
- Eclipse with OSGi
- Java 21+
- ONNX Runtime 1.19.2
- ONNX Runtime Extensions 0.15.0

### Build
- CMake 3.18+ (for ONNX Runtime Extensions)
- Gradle 8.5+ (for Java bindings)
- Git

## Platform Support

| Platform | Status |
|----------|--------|
| macOS ARM64 | ✅ Tested and working |
| macOS x86_64 | 🔄 TODO |
| Windows x64 | 🔄 TODO |
| Windows x86 | 🔄 TODO |
| Linux x64 | 🔄 TODO |
| Linux x86 | 🔄 TODO |

## Installation

1. **Build ONNX Runtime Extensions** (see [README_CONVERSION.md](src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md))
2. **Copy JAR** to `lib/onnxruntime-extensions-0.15.0.jar`
3. **Update MANIFEST.MF** and `.classpath`
4. **Rebuild** Eclipse plugin
5. **Restart** Eclipse

## Extending

### Add New Intent

1. Create `src/main/resources/embeddings/your_intent.txt`:
   ```
   Example prompt 1
   Example prompt 2
   Example prompt 3
   ```

2. Add to `embeddings.list`:
   ```
   relation_create.txt
   valuelist_create.txt
   your_intent.txt
   ```

3. Add handler in `PromptEnricher.java`:
   ```java
   case "YOUR_INTENT" -> enrichYourIntent(prompt);
   ```

4. Restart Eclipse - automatically loaded!

## Troubleshooting

See troubleshooting sections in:
- [AI_INTEGRATION.md](AI_INTEGRATION.md#troubleshooting)
- [ONNX_TOKENIZER_INTEGRATION.md](ONNX_TOKENIZER_INTEGRATION.md#troubleshooting)

## Future Enhancements

- [ ] Multi-platform ONNX Runtime Extensions builds
- [ ] Multiple intent detection in single prompt
- [ ] Expanded knowledge base (forms, scopes, methods)
- [ ] Context retrieval from Servoy docs
- [ ] Persistent embedding storage

## Contributing

When adding new features:
1. Update relevant documentation
2. Add examples to knowledge base
3. Test intent detection accuracy
4. Update CHANGELOG.md

## License

[Your License Here]

## Credits

- **BGE Model**: [BAAI/bge-small-en-v1.5](https://huggingface.co/BAAI/bge-small-en-v1.5)
- **ONNX Runtime**: [Microsoft ONNX Runtime](https://onnxruntime.ai/)
- **ONNX Runtime Extensions**: [microsoft/onnxruntime-extensions](https://github.com/microsoft/onnxruntime-extensions)
- **LangChain4j**: [langchain4j/langchain4j](https://github.com/langchain4j/langchain4j)
