#!/bin/bash

# Create JAR containing ONNX models for Maven deployment

set -e

echo "=========================================="
echo "Creating ONNX Models JAR"
echo "=========================================="
echo ""

# Configuration
JAR_NAME="onnx-models-bge-small-en-v1.5-1.0.0.jar"
MODELS_DIR="src/main/resources/models/bge-small-en-v1.5"

# Create temporary directory structure
TEMP_DIR=$(mktemp -d)
TARGET_DIR="$TEMP_DIR/models/bge-small-en-v1.5"

echo "Creating directory structure..."
mkdir -p "$TARGET_DIR"

# Copy only the 2 ONNX files
echo "Copying model.onnx (133 MB)..."
cp "$MODELS_DIR/model.onnx" "$TARGET_DIR/"

echo "Copying tokenizer.onnx (232 KB)..."
cp "$MODELS_DIR/tokenizer.onnx" "$TARGET_DIR/"

# Create JAR
echo ""
echo "Creating JAR: $JAR_NAME"
cd "$TEMP_DIR"
jar cf "$JAR_NAME" models/

# Move JAR to project root
mv "$JAR_NAME" "$OLDPWD/"

# Cleanup
cd "$OLDPWD"
rm -rf "$TEMP_DIR"

echo ""
echo "=========================================="
echo "JAR created successfully!"
echo "=========================================="
echo ""
echo "File: $JAR_NAME"
ls -lh "$JAR_NAME"
echo ""
echo "Contents:"
jar tf "$JAR_NAME"
echo ""
