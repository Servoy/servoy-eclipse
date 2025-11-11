# ONNX Tokenizer Integration Guide

## Overview

This guide explains how to integrate the ONNX tokenizer (`tokenizer.onnx`) with ONNX Runtime Extensions into the Java project. The tokenizer replaces `SimpleBertTokenizer` with a native ONNX implementation.

## Prerequisites

- **Java 21+**
- **CMake 3.18+** (for building ONNX Runtime Extensions)
- **Gradle 8.5+** (included in ONNX Runtime Extensions repo)
- **Git**

---

## Step 1: Build ONNX Runtime Extensions JAR

**Note:** Maven Central version (0.14.0) does NOT include the Java bindings. You must build from source.

### Clone and Build

```bash
cd /Volumes/Servoy/git/master
git clone https://github.com/microsoft/onnxruntime-extensions.git
cd onnxruntime-extensions
```

### Fix Gradle Compatibility (Java 21)

Edit `java/gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

### Build with Java Bindings

```bash
./build.sh -DOCOS_BUILD_JAVA=ON
```

This builds:
- Native C++ library (`libortextensions.dylib` on macOS)
- JNI wrapper (`libonnxruntime_extensions4j_jni.dylib`)
- Java JAR with bundled native libraries

### Copy JAR to Project

```bash
cp out/Darwin/RelWithDebInfo/java/build/libs/onnxruntime-extensions-0.15.0.jar \
   /Volumes/Servoy/git/master/servoy-eclipse/com.servoy.eclipse.aibridge/lib/
```

### Expected Files

After build, you should have:
```
lib/
├── langchain4j-1.7.1.jar
├── langchain4j-core-1.7.1.jar
├── onnxruntime-1.19.2.jar
└── onnxruntime-extensions-0.15.0.jar  (BUILT FROM SOURCE)
```

---

## Step 2: Update MANIFEST.MF

Add the JAR to your bundle classpath in `META-INF/MANIFEST.MF`:

```manifest
Bundle-ClassPath: .,
 lib/langchain4j-1.7.1.jar,
 lib/langchain4j-core-1.7.1.jar,
 lib/onnxruntime-1.19.2.jar,
 lib/onnxruntime-extensions-0.15.0.jar
```

---

## Step 3: Update .classpath (for Eclipse IDE)

Add to `.classpath`:

```xml
<classpathentry kind="lib" path="lib/onnxruntime-extensions-0.15.0.jar"/>
```

---

## Step 4: Update ServoyEmbeddingService.java

### Add Import

```java
import ai.onnxruntime.extensions.OrtxPackage;
```

### Add Tokenizer Session Field

```java
private OrtSession tokenizerSession;
```

### Load and Register ONNX Tokenizer in initialize()

```java
// Load ONNX tokenizer from resources
System.out.println("[ServoyEmbeddings] Loading ONNX tokenizer...");
InputStream tokenizerStream = getClass().getResourceAsStream("/models/bge-small-en-v1.5/tokenizer.onnx");
if (tokenizerStream == null) {
    throw new RuntimeException("Tokenizer file not found in resources");
}
byte[] tokenizerBytes = tokenizerStream.readAllBytes();
System.out.println("[ServoyEmbeddings] Tokenizer loaded (" + (tokenizerBytes.length / 1024) + " KB), creating session...");

// Register ONNX Runtime Extensions for custom operators (BertTokenizer)
OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath());
System.out.println("[ServoyEmbeddings] ONNX Runtime Extensions registered");

// Create tokenizer session with ONNX Runtime Extensions
tokenizerSession = env.createSession(tokenizerBytes, sessionOptions);
System.out.println("[ServoyEmbeddings] ONNX tokenizer session created");
```

### Update generateEmbedding() to Use ONNX Tokenizer

```java
// Step 1: Tokenize using ONNX tokenizer
Map<String, OnnxTensor> tokenizerInputs = new HashMap<>();
String[] textArray = new String[] { text };
OnnxTensor textTensor = OnnxTensor.createTensor(env, textArray);
tokenizerInputs.put("text", textTensor);

OrtSession.Result tokenizerResults = tokenizerSession.run(tokenizerInputs);

// Extract tokenizer outputs (1D arrays)
long[] inputIds = (long[])tokenizerResults.get(0).getValue();
long[] attentionMask = (long[])tokenizerResults.get(2).getValue();
long[] tokenTypeIds = (long[])tokenizerResults.get(1).getValue();

System.out.println("[ServoyEmbeddings] Token count: " + inputIds.length);

// Cleanup tokenizer tensors
textTensor.close();
tokenizerResults.close();

// Step 2: Wrap in 2D arrays for embedding model (add batch dimension)
long[][] inputIdsArray = new long[][] { inputIds };
long[][] attentionMaskArray = new long[][] { attentionMask };
long[][] tokenTypeIdsArray = new long[][] { tokenTypeIds };

// Continue with embedding model inference...
```

---

## Step 5: Pre-load at Startup (Optional but Recommended)

Add initialization to `Activator.start()` to pre-load the knowledge base:

```java
// In Activator.start()
Job job = new Job("Loading Servoy AI Knowledge Base") {
    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            monitor.beginTask("Initializing embedding service...", IProgressMonitor.UNKNOWN);
            ServoyEmbeddingService.getInstance();
            return Status.OK_STATUS;
        } finally {
            monitor.done();
        }
    }
};
job.setSystem(true);
job.schedule();
```

---

## Step 6: Test

After making changes:

1. **Rebuild the project**
2. **Restart Eclipse**
3. **Check console for initialization logs:**
   ```
   [Activator] Pre-loading Servoy AI knowledge base...
   [ServoyEmbeddings] ONNX Runtime Extensions registered
   [ServoyEmbeddings] ONNX tokenizer session created
   ```
4. **Test with a prompt:**
   - "Create a relation between orders and customers"
   - Check console for tokenization logs

---

## Benefits

✅ **Native ONNX tokenization** - No SimpleBertTokenizer needed  
✅ **Official Microsoft solution** - Well-supported and maintained  
✅ **OSGi compatible** - Works in Eclipse plugins  
✅ **Consistent tokenization** - Identical to Python HuggingFace tokenizer  
✅ **High quality** - BGE-small-en-v1.5 model with proper tokenization  

---

## Troubleshooting

### Issue: "Tokenizer file not found"
**Solution:** Ensure `tokenizer.onnx` is in `src/main/resources/models/bge-small-en-v1.5/`

### Issue: "ClassNotFoundException: ai.onnxruntime.extensions.OrtxPackage"
**Solution:** 
- Build `onnxruntime-extensions-0.15.0.jar` from source (Maven Central version doesn't have Java bindings)
- Add to `lib/` folder
- Update MANIFEST.MF and .classpath

### Issue: "BertTokenizer is not a registered function/op"
**Solution:** 
- Ensure `sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())` is called
- Verify JAR contains native libraries for your platform

### Issue: "UnsatisfiedLinkError"
**Solution:** 
- ONNX Runtime Extensions requires native libraries
- Rebuild for your platform (macOS ARM64, x86_64, Windows, Linux)
- Check JAR contains `.dylib` (macOS), `.so` (Linux), or `.dll` (Windows)

### Issue: "class [J cannot be cast to class [[J"
**Solution:** 
- Tokenizer returns 1D arrays (`long[]`), not 2D (`long[][]`)
- Use: `long[] inputIds = (long[])tokenizerResults.get(0).getValue();`
- Then wrap: `long[][] inputIdsArray = new long[][] { inputIds };`

---

## References

- **ONNX Runtime Extensions:** https://github.com/microsoft/onnxruntime-extensions
- **Official Docs:** https://onnxruntime.ai/docs/extensions/
- **Build Instructions:** `src/main/resources/models/bge-small-en-v1.5/README_CONVERSION.md`
- **BGE Model:** https://huggingface.co/BAAI/bge-small-en-v1.5
