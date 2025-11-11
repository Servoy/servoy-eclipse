#!/bin/bash

# Deploy ONNX Models to Servoy Maven Repository

set -e

echo "=========================================="
echo "Deploying ONNX Models JAR"
echo "to Servoy Maven Repository"
echo "=========================================="
echo ""

# Configuration
REPO_URL="https://developer.servoy.com/mvn_repository/"
REPO_ID="servoy-releases"

echo "Repository: $REPO_URL"
echo "Repository ID: $REPO_ID"
echo ""

# Deploy ONNX Models JAR
echo "Deploying onnx-models-bge-small-en-v1.5..."
mvn deploy:deploy-file \
    -DgroupId=com.servoy.eclipse \
    -DartifactId=onnx-models-bge-small-en-v1.5 \
    -Dversion=1.0.0 \
    -Dpackaging=jar \
    -Dfile=onnx-models-bge-small-en-v1.5-1.0.0.jar \
    -DpomFile=pom.xml \
    -DrepositoryId=$REPO_ID \
    -Durl=$REPO_URL

echo "✓ onnx-models-bge-small-en-v1.5 deployed"
echo ""

echo "=========================================="
echo "Deployment successful!"
echo "=========================================="
echo ""
echo "Deployed to: $REPO_URL"
echo "Artifact: com.servoy.eclipse:onnx-models-bge-small-en-v1.5:1.0.0"
echo ""
