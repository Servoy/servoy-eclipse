# Maven Migration Summary

## Completed Steps

### 1. ✅ Added Maven Dependencies to Target Definition

Added to `/Volumes/Servoy/git/master/servoy-eclipse/launch_targets/com.servoy.eclipse.target.target`:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>1.7.1</version>
    <type>jar</type>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-core</artifactId>
    <version>1.7.1</version>
    <type>jar</type>
</dependency>
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.19.2</version>
    <type>jar</type>
</dependency>
```

These will be resolved from Maven Central.

### 2. ✅ Updated MANIFEST.MF

**Removed from Bundle-ClassPath:**
- `lib/langchain4j-1.7.1.jar`
- `lib/langchain4j-core-1.7.1.jar`
- `lib/onnxruntime-1.19.2.jar`

**Added to Import-Package:**
- `ai.onnxruntime`
- `ai.onnxruntime.extensions`
- `dev.langchain4j.data.document`
- `dev.langchain4j.data.embedding`
- `dev.langchain4j.data.segment`
- `dev.langchain4j.store.embedding`
- `dev.langchain4j.store.embedding.inmemory`

### 3. ✅ Updated .classpath

**Removed:**
- `<classpathentry kind="lib" path="lib/onnxruntime-1.19.2.jar"/>`
- `<classpathentry kind="lib" path="lib/langchain4j-1.7.1.jar"/>`
- `<classpathentry kind="lib" path="lib/langchain4j-core-1.7.1.jar"/>`

**Kept:**
- `<classpathentry kind="lib" path="lib/onnxruntime-extensions-darwin-arm64-0.15.0.jar"/>`
  (This stays because it's custom-built and not in Maven Central)

---

## Next Steps: ONNX Runtime Extensions

The custom ONNX Runtime Extensions JARs still need to be handled. You have 6 platform-specific JARs:

### Current JARs in lib/
- `onnxruntime-extensions-darwin-arm64-0.15.0.jar` (1.0 MB)
- `onnxruntime-extensions-darwin-x64-0.15.0.jar` (1.1 MB)
- `onnxruntime-extensions-linux-arm64-0.15.0.jar` (1.2 MB)
- `onnxruntime-extensions-linux-x64-0.15.0.jar` (1.7 MB)
- `onnxruntime-extensions-win-arm64-0.15.0.jar` (944 KB)
- `onnxruntime-extensions-win-x64-0.15.0.jar` (1.0 MB)

### Option A: Deploy to Servoy Maven Repository (Recommended)

**Advantages:**
- Clean integration with target platform
- Automatic platform-specific resolution
- Version management
- Shared across projects

**Steps:**
1. Create Maven POM for the artifacts
2. Your boss deploys to Servoy Maven repository
3. Add to target definition with classifiers
4. Update MANIFEST.MF to import from target platform

### Option B: Keep in lib/ Directory (Current Approach)

**Advantages:**
- No deployment needed
- Works immediately

**Disadvantages:**
- Need to manually select platform-specific JAR in MANIFEST.MF
- Harder to maintain across platforms
- Not shareable across projects

---

## Testing After Migration

1. **Reload Target Platform** in Eclipse:
   - Open target definition
   - Click "Reload Target Platform"
   - Wait for dependencies to download

2. **Clean and Build** your plugin:
   - Project → Clean
   - Rebuild

3. **Verify Dependencies**:
   - Check that langchain4j and onnxruntime are resolved from target platform
   - No compilation errors

4. **Test Runtime**:
   - Launch Eclipse with your plugin
   - Test MCP functionality
   - Verify ONNX embeddings work

---

## Current Status

✅ **Migrated to Maven:**
- LangChain4j 1.7.1
- ONNX Runtime 1.19.2

⏳ **Still Local:**
- ONNX Runtime Extensions (6 platform-specific JARs)

---

## Questions?

Let me know if you want to:
1. Deploy ONNX Runtime Extensions to Servoy Maven repository
2. Keep current approach with local JARs
3. Something else
