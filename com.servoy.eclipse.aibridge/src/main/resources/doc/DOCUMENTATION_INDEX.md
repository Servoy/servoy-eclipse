# Documentation Index

Complete guide to all documentation files in the Servoy AI Bridge project.

## Getting Started

Start here if you're new to the project:

1. **[README.md](README.md)** - Project overview and features
2. **[QUICK_START.md](QUICK_START.md)** - Get up and running in 5 minutes
3. **[CHANGELOG.md](CHANGELOG.md)** - What's new and recent changes

## Architecture & Implementation

Deep dive into how the system works:

- **[AI_INTEGRATION.md](AI_INTEGRATION.md)** - Complete architecture guide
  - Components overview
  - Setup instructions
  - Usage examples
  - Performance metrics
  - Troubleshooting

## ONNX Tokenizer

Everything about the ONNX tokenizer integration:

- **[ONNX_TOKENIZER_INTEGRATION.md](ONNX_TOKENIZER_INTEGRATION.md)** - Integration guide
  - Building ONNX Runtime Extensions
  - Java code integration
  - Troubleshooting

- **[src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md](src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md)** - Conversion guide
  - Tokenizer conversion steps
  - ONNX Extensions build process
  - Multi-platform compilation
  - Complete reference

## Integration Guides

- **[INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)** - LangChain4j integration
  - MCP tool setup
  - Agent implementation
  - Testing

## Documentation by Task

### I want to...

#### Set up the project
→ [QUICK_START.md](QUICK_START.md)

#### Understand the architecture
→ [AI_INTEGRATION.md](AI_INTEGRATION.md)

#### Build ONNX Runtime Extensions
→ [src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md](src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md)

#### Integrate the ONNX tokenizer
→ [ONNX_TOKENIZER_INTEGRATION.md](ONNX_TOKENIZER_INTEGRATION.md)

#### Add a new intent
→ [QUICK_START.md#adding-new-intents](QUICK_START.md#adding-new-intents)  
→ [AI_INTEGRATION.md#extending-the-knowledge-base](AI_INTEGRATION.md#extending-the-knowledge-base)

#### Troubleshoot issues
→ [AI_INTEGRATION.md#troubleshooting](AI_INTEGRATION.md#troubleshooting)  
→ [ONNX_TOKENIZER_INTEGRATION.md#troubleshooting](ONNX_TOKENIZER_INTEGRATION.md#troubleshooting)

#### See what changed recently
→ [CHANGELOG.md](CHANGELOG.md)

#### Build for multiple platforms
→ [src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md#building-for-multiple-platforms](src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md#building-for-multiple-platforms)

## File Organization

```
Documentation Files:
├── README.md                     # Project overview
├── QUICK_START.md               # Quick setup guide
├── CHANGELOG.md                 # Version history
├── DOCUMENTATION_INDEX.md       # This file
├── AI_INTEGRATION.md            # Architecture guide
├── ONNX_TOKENIZER_INTEGRATION.md # Tokenizer integration
├── INTEGRATION_GUIDE.md         # LangChain4j guide
└── src/main/resources/models/bge-small-en-v1.5/
    └── README_CONVERSION.md     # Tokenizer conversion & build

Source Code:
├── src/com/servoy/eclipse/
│   ├── aibridge/
│   │   └── Activator.java       # Startup initialization
│   └── mcp/
│       ├── ai/
│       │   ├── ServoyEmbeddingService.java  # Core AI service
│       │   ├── IntentDetector.java          # Intent detection
│       │   └── PromptEnricher.java          # Prompt enrichment
│       └── McpServletProvider.java          # MCP endpoint

Resources:
├── src/main/resources/
│   ├── models/bge-small-en-v1.5/
│   │   ├── model.onnx           # Embedding model
│   │   ├── tokenizer.onnx       # ONNX tokenizer
│   │   └── README_CONVERSION.md # Conversion guide
│   └── embeddings/
│       ├── embeddings.list      # Intent file list
│       ├── relation_create.txt  # Relation examples
│       └── valuelist_create.txt # Value list examples

Dependencies:
└── lib/
    ├── langchain4j-1.7.1.jar
    ├── langchain4j-core-1.7.1.jar
    ├── onnxruntime-1.19.2.jar
    └── onnxruntime-extensions-0.15.0.jar
```

## Documentation Status

| Document | Status | Last Updated |
|----------|--------|--------------|
| README.md | ✅ Current | 2025-11-03 |
| QUICK_START.md | ✅ Current | 2025-11-03 |
| CHANGELOG.md | ✅ Current | 2025-11-03 |
| AI_INTEGRATION.md | ✅ Current | 2025-11-03 |
| ONNX_TOKENIZER_INTEGRATION.md | ✅ Current | 2025-11-03 |
| INTEGRATION_GUIDE.md | ✅ Current | 2025-11-03 |
| README_CONVERSION.md | ✅ Current | 2025-11-03 |

## Contributing to Documentation

When updating code, please also update:
1. Relevant documentation files
2. CHANGELOG.md with your changes
3. Code comments and JavaDoc
4. This index if adding new docs

## Questions?

If you can't find what you're looking for:
1. Check the troubleshooting sections
2. Review console output for errors
3. Verify all dependencies are present
4. Check the GitHub issues (if applicable)
