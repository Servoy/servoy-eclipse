# ONNX Tokenizer Conversion Guide

This document explains how the BGE-small-en-v1.5 tokenizer was converted from HuggingFace format to ONNX format for use with ONNX Runtime Extensions.

## Overview

The tokenizer conversion process transforms a HuggingFace tokenizer into an ONNX model that can be loaded and executed by ONNX Runtime Extensions. This enables native tokenization without requiring Python dependencies at runtime.

## Prerequisites

### 1. Install Conda

Download and install Miniconda or Anaconda:
- **macOS/Linux**: https://docs.conda.io/en/latest/miniconda.html
- **Windows**: https://docs.conda.io/en/latest/miniconda.html

### 2. Create Conda Environment

```bash
conda create -n onnx-tokenizer python=3.10
conda activate onnx-tokenizer
```

### 3. Install Required Packages

```bash
pip install torch
pip install transformers
pip install onnx
pip install onnxruntime
pip install onnxruntime-extensions
```

## Conversion Process

### Step 1: Download the Model

The BGE-small-en-v1.5 model files are downloaded from HuggingFace:

```python
from transformers import AutoTokenizer

model_name = "BAAI/bge-small-en-v1.5"
tokenizer = AutoTokenizer.from_pretrained(model_name)
tokenizer.save_pretrained("./bge-small-en-v1.5")
```

This creates a directory with:
- `tokenizer.json` - Fast tokenizer configuration
- `tokenizer_config.json` - Tokenizer settings
- `vocab.txt` - Vocabulary file
- `special_tokens_map.json` - Special tokens

### Step 2: Convert to ONNX Format

Use the conversion script located at `src/main/resources/models/bge-small-en-v1.5/convert_tokenizer.py`:

```bash
cd src/main/resources/models/bge-small-en-v1.5
python convert_tokenizer.py
```

The script performs these operations:

1. **Load the tokenizer** from the saved files
2. **Export to ONNX** using ONNX Runtime Extensions
3. **Generate tokenizer.onnx** - The ONNX model file

### Step 3: Test the Conversion

Verify the converted tokenizer works correctly:

```bash
python test_tokenizer.py
```

This script:
- Loads the ONNX tokenizer
- Tokenizes sample text
- Compares output with the original HuggingFace tokenizer
- Validates token IDs match

## Output Files

After conversion, you'll have:

- **`tokenizer.onnx`** (232 KB) - The ONNX tokenizer model
  - Used by Java code via ONNX Runtime Extensions
  - Contains the BertTokenizer operator
  - Platform-independent

- **`model.onnx`** (133 MB) - The embedding model
  - Separate from tokenizer
  - Used for generating embeddings

## Integration with Java

The converted tokenizer is loaded in `ServoyEmbeddingService.java`:

```java
// Load ONNX tokenizer
InputStream tokenizerStream = getClass()
    .getResourceAsStream("/models/bge-small-en-v1.5/tokenizer.onnx");

// Register ONNX Runtime Extensions (required for BertTokenizer)
OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath());

// Create tokenizer session
tokenizerSession = env.createSession(tokenizerBytes, sessionOptions);
```

## Platform-Specific Extensions

The ONNX Runtime Extensions library is required for the BertTokenizer operator. Platform-specific builds are provided:

- `onnxruntime-extensions-darwin-arm64-0.15.0.jar` (macOS Apple Silicon)
- `onnxruntime-extensions-darwin-x64-0.15.0.jar` (macOS Intel)
- `onnxruntime-extensions-linux-arm64-0.15.0.jar` (Linux ARM64)
- `onnxruntime-extensions-linux-x64-0.15.0.jar` (Linux x86_64)
- `onnxruntime-extensions-win-arm64-0.15.0.jar` (Windows ARM64)
- `onnxruntime-extensions-win-x64-0.15.0.jar` (Windows x86_64)

See platform-specific build documentation in `src/main/resources/models/bge-small-en-v1.5/README_CONVERSION_*.md`

## Troubleshooting

### Issue: Import Error for onnxruntime_extensions

**Solution**: Ensure you're in the conda environment:
```bash
conda activate onnx-tokenizer
pip install onnxruntime-extensions
```

### Issue: Tokenizer output doesn't match

**Solution**: Verify you're using the same model version:
- Check `tokenizer_config.json` for model name
- Ensure all tokenizer files are from the same download

### Issue: ONNX Runtime Extensions not found at runtime

**Solution**: Verify the platform-specific JAR is in the classpath:
- Check `MANIFEST.MF` Bundle-ClassPath
- Ensure correct platform JAR is loaded
- Verify `OrtxPackage.getLibraryPath()` returns valid path

## References

- **ONNX Runtime Extensions**: https://github.com/microsoft/onnxruntime-extensions
- **BGE Model**: https://huggingface.co/BAAI/bge-small-en-v1.5
- **HuggingFace Transformers**: https://huggingface.co/docs/transformers
- **ONNX Format**: https://onnx.ai/

## Version History

- **v1.0.0** (2024-11-06): Initial tokenizer conversion using ONNX Runtime Extensions 0.15.0
- Model: BGE-small-en-v1.5
- ONNX Runtime: 1.19.2
- Python: 3.10
