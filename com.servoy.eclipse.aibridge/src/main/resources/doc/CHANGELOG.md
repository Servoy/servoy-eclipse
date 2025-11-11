# Changelog - ONNX Tokenizer Integration

## [2025-11-03] - ONNX Tokenizer Integration Complete

### Added
- **ONNX Tokenizer Integration**: Replaced `SimpleBertTokenizer` with native ONNX tokenizer
  - Built ONNX Runtime Extensions 0.15.0 from source with Java bindings
  - Integrated `tokenizer.onnx` for BGE-small-en-v1.5 model
  - Added `OrtxPackage.getLibraryPath()` registration for custom operators
  
- **Startup Pre-loading**: Knowledge base now loads at Eclipse startup
  - Added background job in `Activator.start()`
  - Non-blocking initialization (3-5 seconds)
  - Ready for first prompt without delay

- **Documentation**:
  - `README_CONVERSION.md`: Complete guide for tokenizer conversion and ONNX Extensions build
  - Updated `AI_INTEGRATION.md` with current architecture
  - Updated `ONNX_TOKENIZER_INTEGRATION.md` with build instructions
  - Added status notes to `INTEGRATION_GUIDE.md`

### Changed
- **ServoyEmbeddingService**:
  - Renamed from `ServoyEmbeddingServiceONNX` to `ServoyEmbeddingService`
  - Fixed singleton pattern (removed duplicate `getInstance()` methods)
  - Updated tokenizer to use 1D array output (`long[]`) instead of 2D (`long[][]`)
  - Added proper initialization flow with `initialize()` method

- **Dependencies**:
  - Updated to `onnxruntime-extensions-0.15.0.jar` (custom build)
  - Fixed Gradle compatibility for Java 21 (Gradle 8.5)
  - Updated MANIFEST.MF and .classpath

### Removed
- **SimpleBertTokenizer.java**: Replaced by ONNX tokenizer
- **Old ServoyEmbeddingService.java**: Consolidated into single service

### Fixed
- **Tokenizer Output Handling**: Corrected array dimension casting
  - Tokenizer returns `long[]` (1D)
  - Wrapped to `long[][]` for embedding model batch dimension
  
- **Static Reference Errors**: Fixed singleton initialization issues
- **Compilation Errors**: Resolved all static/instance method conflicts

### Technical Details

**Model Stack:**
- **Embedding Model**: BGE-small-en-v1.5 (133 MB, 384 dimensions)
- **Tokenizer**: ONNX tokenizer (226 KB) with ONNX Runtime Extensions
- **Runtime**: ONNX Runtime 1.19.2
- **Framework**: LangChain4j 1.7.1

**Performance:**
- Startup time: ~3-5 seconds (background, non-blocking)
- Intent detection: ~100-200ms per prompt
- Memory usage: ~300MB (model + tokenizer + embeddings)

**Platform Support:**
- ✅ macOS ARM64 (tested)
- 🔄 macOS x86_64 (TODO)
- 🔄 Windows x64/x86 (TODO)
- 🔄 Linux x64/x86 (TODO)

### Next Steps

1. **Multi-platform builds**: Cross-compile ONNX Runtime Extensions for all platforms
2. **Multiple intent detection**: Support detecting multiple intents in one prompt
3. **Expanded knowledge base**: Add more Servoy examples (forms, scopes, methods)

---

## Previous Versions

### [2024-XX-XX] - Initial LangChain4J Integration
- Added LangChain4j with all-MiniLM-L6-v2 model
- Implemented SimpleBertTokenizer
- Created intent detection system
- Added in-memory vector store
