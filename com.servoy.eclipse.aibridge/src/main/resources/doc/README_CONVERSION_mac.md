# Tokenizer to ONNX Conversion Guide

## Overview

This directory contains the conversion script to convert the BGE-small-en-v1.5 tokenizer to ONNX format using Microsoft's ONNX Runtime Extensions.

## Prerequisites

- Conda installed on your system
- Internet connection (for downloading packages)

## Step-by-Step Instructions

### 1. Create Conda Environment

```bash
conda create -n onnx python=3.12
```

### 2. Activate Environment

```bash
conda activate onnx
```

### 3. Install Required Packages

```bash
pip install onnxruntime onnxruntime-extensions transformers onnx
```

### 4. Navigate to This Directory

```bash
cd /Volumes/Servoy/git/master/servoy-eclipse/com.servoy.eclipse.aibridge/src/main/resources/models/bge-small-en-v1.5
```

### 5. Run Conversion Script

```bash
python convert_tokenizer.py
```

### 6. Verify Output

After successful conversion, you should see:
- ✅ `tokenizer.onnx` file created in this directory
- File size should be a few MB

### 7. Deactivate Environment (Optional)

```bash
conda deactivate
```

## Expected Output

```
============================================================
BGE-small-en-v1.5 Tokenizer to ONNX Converter
============================================================

Step 1: Loading BGE-small-en-v1.5 tokenizer from HuggingFace...
✅ Tokenizer loaded successfully

Step 2: Converting tokenizer to ONNX format...
(This uses Microsoft ONNX Runtime Extensions)
✅ Conversion successful

Step 3: Saving tokenizer to tokenizer.onnx...
✅ Tokenizer saved successfully (XXX,XXX bytes)

============================================================
✅ CONVERSION COMPLETE!
============================================================
```

## Files After Conversion

```
bge-small-en-v1.5/
├── model.onnx              (133 MB - embedding model)
├── tokenizer.onnx          (NEW - converted tokenizer)
├── vocab.txt               (231 KB - vocabulary)
├── config.json             (743 B - model config)
├── tokenizer.json          (711 KB - original tokenizer)
├── tokenizer_config.json   (366 B - tokenizer config)
├── special_tokens_map.json (125 B - special tokens)
├── convert_tokenizer.py    (this conversion script)
└── README_CONVERSION.md    (this file)
```

## Troubleshooting

### Issue: "ModuleNotFoundError: No module named 'onnxruntime_extensions'"

**Solution:** Make sure you activated the conda environment and installed packages:
```bash
conda activate onnx
pip install onnxruntime-extensions transformers onnx
```

### Issue: "Failed to load tokenizer"

**Solution:** Check internet connection. The script downloads the tokenizer from HuggingFace on first run.

### Issue: Permission denied

**Solution:** Make sure you have write permissions in this directory.

## Official Documentation

- **Microsoft ONNX Runtime Extensions:** https://github.com/microsoft/onnxruntime-extensions
- **Official Docs:** https://onnxruntime.ai/docs/extensions/
- **BGE Model:** https://huggingface.co/BAAI/bge-small-en-v1.5

## Next Steps: Building ONNX Runtime Extensions for Java

After successful tokenizer conversion, you need to build the ONNX Runtime Extensions Java library to use the tokenizer in your Java application.

### Prerequisites for Building

- **CMake** 3.18+ (`brew install cmake` on macOS)
- **Java JDK** 21+
- **Maven** (for Java bindings)
- **Git**

### Step 1: Clone ONNX Runtime Extensions Repository

```bash
cd /Volumes/Servoy/git/master
git clone https://github.com/microsoft/onnxruntime-extensions.git
cd onnxruntime-extensions
```

### Step 2: Fix Gradle Version Compatibility

The repository uses Gradle 8.0.1 which doesn't support Java 21. Update it:

```bash
# Edit the Gradle wrapper properties
nano java/gradle/wrapper/gradle-wrapper.properties
```

Change the `distributionUrl` line to:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

Or remove the `distributionSha256Sum` line if present.

### Step 3: Build Native Library and Java Bindings

```bash
# Build for macOS ARM64 (or your platform)
./build.sh -DOCOS_BUILD_JAVA=ON
```

This will:
- Build the native C++ library (`libortextensions.dylib` on macOS)
- Build the JNI wrapper (`libonnxruntime_extensions4j_jni.dylib`)
- Create the Java JAR with native libraries bundled inside

**Build time:** ~2-3 minutes on first build

> **Apple Silicon + Intel (x86_64) build**
>
> When running on Apple Silicon but targeting macOS x86_64, run these additional steps before invoking the build script:
>
> 1. **Install Rosetta (once):**
>    ```bash
>    softwareupdate --install-rosetta --agree-to-license
>    ```
> 2. **Install Intel Homebrew toolchain:**
>    ```bash
>    arch -x86_64 /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
>    arch -x86_64 /usr/local/bin/brew install cmake
>    arch -x86_64 /usr/local/bin/brew install --cask temurin@21
>    ```
> 3. **Launch an x86_64 shell and point to the Intel JDK:**
>    ```bash
>    arch -x86_64 /usr/bin/env bash
>    export PATH="/usr/local/bin:/opt/homebrew/bin:$PATH"
>    export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
>    ```
>    Verify the toolchain:
>    ```bash
>    file /usr/local/bin/cmake   # -> ... executable x86_64
>    file "$JAVA_HOME/bin/java"  # -> ... executable x86_64
>    ```
> 4. **Force the build to emit x86_64 binaries:**
>    ```bash
>    OCOS_CMAKE_ARGS="-DCMAKE_OSX_ARCHITECTURES=x86_64" \
>      ./build.sh -DOCOS_BUILD_JAVA=ON
>    ```
> 5. **Verify the results:**
>    ```bash
>    file out/Darwin/RelWithDebInfo/lib/libonnxruntime_extensions4j_jni.dylib
>    jar tf out/Darwin/RelWithDebInfo/java/build/libs/onnxruntime-extensions-0.15.0.jar | grep osx
>    ```
>    Expect the native binaries under `ai/onnxruntime/extensions/native/osx-x64/` inside the JAR.
> 6. **(Optional) Restore your default ARM environment** once finished:
>    ```bash
>    export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
>    export PATH="/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"
>    ```
>    (Adjust the paths to match your ARM JDK/homebrew locations.)

### Step 4: Locate the Built JAR

After successful build, find the JAR at:
```
out/Darwin/RelWithDebInfo/java/build/libs/onnxruntime-extensions-0.15.0.jar
```

### Step 5: Copy JAR to Your Project

```bash
cp out/Darwin/RelWithDebInfo/java/build/libs/onnxruntime-extensions-0.15.0.jar \
   /Volumes/Servoy/git/master/servoy-eclipse/com.servoy.eclipse.aibridge/lib/
```

### Step 6: Update Project Configuration

#### Update `META-INF/MANIFEST.MF`:
```manifest
Bundle-ClassPath: .,
 lib/langchain4j-1.7.1.jar,
 lib/langchain4j-core-1.7.1.jar,
 lib/onnxruntime-1.19.2.jar,
 lib/onnxruntime-extensions-0.15.0.jar
```

#### Update `.classpath` (for Eclipse IDE):
```xml
<classpathentry kind="lib" path="lib/onnxruntime-extensions-0.15.0.jar"/>
```

### Step 7: Use in Java Code

```java
import ai.onnxruntime.extensions.OrtxPackage;

// Register ONNX Runtime Extensions before creating tokenizer session
OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath());

// Create tokenizer session with extensions enabled
OrtSession tokenizerSession = env.createSession(tokenizerBytes, sessionOptions);
```

### Building for Multiple Platforms

To support Windows and Linux, you need to build on those platforms:

#### **Windows (x86_64):**
```cmd
build.bat -DOCOS_BUILD_JAVA=ON
```

#### **Linux (x86_64):**
```bash
./build.sh -DOCOS_BUILD_JAVA=ON
```

Then collect all platform-specific JARs and merge the native libraries into one universal JAR.

### Troubleshooting Build Issues

#### Issue: "cmake: command not found"
```bash
brew install cmake  # macOS
```

#### Issue: Gradle version incompatibility
- Update `gradle-wrapper.properties` to Gradle 8.5 or higher
- Clear Gradle cache: `rm -rf ~/.gradle/caches/8.0.1`

#### Issue: Java version mismatch
- Ensure Java 21+ is installed: `java --version`
- Gradle 8.5+ supports Java 21

### Verification

After integration, verify the setup:
1. Build your Eclipse plugin
2. Check console output for: `[ServoyEmbeddings] ONNX Runtime Extensions registered`
3. Tokenizer should process text without errors

## Complete Integration

Your final setup should have:
1. ✅ `tokenizer.onnx` - ONNX tokenizer model
2. ✅ `model.onnx` - BGE embedding model  
3. ✅ `onnxruntime-extensions-0.15.0.jar` - Java library with native bindings
4. ✅ `ServoyEmbeddingService.java` - Using ONNX tokenizer with extensions
