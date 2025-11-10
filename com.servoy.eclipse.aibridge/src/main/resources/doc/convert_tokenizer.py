#!/usr/bin/env python3
"""
Convert BGE-small-en-v1.5 tokenizer to ONNX format
Based on Microsoft ONNX Runtime Extensions

Usage:
    1. Create conda environment:
       conda create -n onnx python=3.10
       conda activate onnx
    
    2. Install dependencies:
       pip install onnxruntime-extensions transformers onnx
    
    3. Run conversion:
       python convert_tokenizer.py
    
    4. Output: tokenizer.onnx will be created in this directory
"""

import onnx
from transformers import AutoTokenizer
from onnxruntime_extensions import gen_processing_models
import os

def main():
    print("=" * 60)
    print("BGE-small-en-v1.5 Tokenizer to ONNX Converter")
    print("=" * 60)
    print()
    
    # 1. Load BGE-small-en-v1.5 tokenizer from HuggingFace
    print("Step 1: Loading BGE-small-en-v1.5 tokenizer from HuggingFace...")
    try:
        tokenizer = AutoTokenizer.from_pretrained("BAAI/bge-small-en-v1.5")
        print("✅ Tokenizer loaded successfully")
    except Exception as e:
        print(f"❌ Failed to load tokenizer: {e}")
        return
    
    print()
    
    # 2. Convert tokenizer to ONNX format using Microsoft's gen_processing_models
    print("Step 2: Converting tokenizer to ONNX format...")
    print("(This uses Microsoft ONNX Runtime Extensions)")
    try:
        onnx_models = gen_processing_models(tokenizer, pre_kwargs={})
        tokenizer_onnx = onnx_models[0]
        print("✅ Conversion successful")
    except Exception as e:
        print(f"❌ Failed to convert: {e}")
        return
    
    print()
    
    # 3. Save the tokenizer as ONNX file in current directory
    output_path = os.path.join(os.path.dirname(__file__), "tokenizer.onnx")
    print(f"Step 3: Saving tokenizer to {output_path}...")
    try:
        onnx.save(tokenizer_onnx, output_path)
        file_size = os.path.getsize(output_path)
        print(f"✅ Tokenizer saved successfully ({file_size:,} bytes)")
    except Exception as e:
        print(f"❌ Failed to save: {e}")
        return
    
    print()
    print("=" * 60)
    print("✅ CONVERSION COMPLETE!")
    print("=" * 60)
    print()
    print("Output file: tokenizer.onnx")
    print()
    print("Next steps:")
    print("1. The tokenizer.onnx file is now in this directory")
    print("2. Update ServoyEmbeddingService.java to use ONNX tokenizer")
    print("3. Add ONNX Runtime Extensions Java library to your project")
    print()

if __name__ == "__main__":
    main()
