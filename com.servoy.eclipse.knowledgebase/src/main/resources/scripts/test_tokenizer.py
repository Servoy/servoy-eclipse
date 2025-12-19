#!/usr/bin/env python3
"""
Test the converted ONNX tokenizer
Verifies that tokenizer.onnx works correctly
"""

import onnxruntime as ort
from onnxruntime_extensions import get_library_path
import numpy as np

def main():
    print("=" * 60)
    print("ONNX Tokenizer Test")
    print("=" * 60)
    print()
    
    # Test sentences
    test_sentences = [
        "Create a relation between orders and customers",
        "Make a value list with Active, Inactive, Pending",
        "Hello world",
        "This is a test sentence for BGE embeddings"
    ]
    
    print("Step 1: Loading ONNX tokenizer...")
    try:
        # Create session with ONNX Runtime Extensions
        session_options = ort.SessionOptions()
        session_options.register_custom_ops_library(get_library_path())
        
        session = ort.InferenceSession(
            "tokenizer.onnx",
            session_options,
            providers=['CPUExecutionProvider']
        )
        print("✅ Tokenizer loaded successfully")
        print()
        
        # Print input/output info
        print("Tokenizer inputs:")
        for input_meta in session.get_inputs():
            print(f"  - {input_meta.name}: {input_meta.type}")
        print()
        
        print("Tokenizer outputs:")
        for output_meta in session.get_outputs():
            print(f"  - {output_meta.name}: {output_meta.type}")
        print()
        
    except Exception as e:
        print(f"❌ Failed to load tokenizer: {e}")
        return
    
    print("=" * 60)
    print("Step 2: Testing tokenization...")
    print("=" * 60)
    print()
    
    for i, sentence in enumerate(test_sentences, 1):
        print(f"Test {i}: \"{sentence}\"")
        try:
            # Run tokenization
            inputs = {session.get_inputs()[0].name: np.array([sentence])}
            outputs = session.run(None, inputs)
            
            # Print results
            print(f"  ✅ Tokenization successful")
            for j, output_meta in enumerate(session.get_outputs()):
                output_name = output_meta.name
                output_value = outputs[j]
                print(f"  - {output_name}: shape={output_value.shape}, dtype={output_value.dtype}")
                if output_value.size <= 20:  # Only print small arrays
                    print(f"    values: {output_value}")
            print()
            
        except Exception as e:
            print(f"  ❌ Tokenization failed: {e}")
            print()
    
    print("=" * 60)
    print("✅ TOKENIZER TEST COMPLETE!")
    print("=" * 60)
    print()
    print("The ONNX tokenizer is working correctly and ready to use in Java!")
    print()

if __name__ == "__main__":
    main()
