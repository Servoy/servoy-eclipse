# Maven Integration Resume - Where We Left Off

## Current State (Nov 6, 2025 - 8:02 PM)

### Working Configuration
- **Status**: Fully functional with LOCAL setup
- **Models**: In `src/main/resources/models/bge-small-en-v1.5/` (model.onnx + tokenizer.onnx)
- **Extensions**: 6 JARs in `lib/` directory
- **Code**: `ServoyEmbeddingService.java` ALREADY UPDATED to use OSGi Platform API with `Platform.getBundle()` and `URL`

### What Boss Has
- **Location**: `/Users/marianvid/Servoy/Work/tmp/mcp.zip`
- **Contents**: 7 JARs + deployment script
- **Artifacts to deploy**:
  1. `com.servoy.eclipse:onnx-models-bge-small:1.0.0`
  2. `com.servoy.eclipse:onnxruntime-extensions-darwin-arm64:0.15.0`
  3. `com.servoy.eclipse:onnxruntime-extensions-darwin-x64:0.15.0`
  4. `com.servoy.eclipse:onnxruntime-extensions-linux-arm64:0.15.0`
  5. `com.servoy.eclipse:onnxruntime-extensions-linux-x64:0.15.0`
  6. `com.servoy.eclipse:onnxruntime-extensions-win-arm64:0.15.0`
  7. `com.servoy.eclipse:onnxruntime-extensions-win-x64:0.15.0`

### Code Already Modified (DO NOT REDO)
File: `ServoyEmbeddingService.java`
- Lines 58-71: Loads model from bundle using `Platform.getBundle("wrapped.com.servoy.eclipse.onnx-models-bge-small")` and `modelsBundle.getEntry("models/bge-small-en-v1.5/model.onnx")`
- Lines 77-84: Loads tokenizer from same bundle
- Imports added: `java.net.URL`, `org.eclipse.core.runtime.Platform`, `org.osgi.framework.Bundle`

File: `MANIFEST.MF`
- Line 87: Added `wrapped.com.servoy.eclipse.onnx-models-bge-small;bundle-version="1.0.0"` to Require-Bundle

## NEXT STEPS (After Boss Deploys to Maven)

### 1. Update Target Definition
File: `/Volumes/Servoy/git/master/servoy-eclipse/launch_targets/com.servoy.eclipse.target.target`

Add after line 524 (after onnxruntime dependency):
```xml
<dependency>
    <groupId>com.servoy.eclipse</groupId>
    <artifactId>onnx-models-bge-small</artifactId>
    <version>1.0.0</version>
    <type>jar</type>
</dependency>
```

**IMPORTANT**: Check if we still have the 6 extension dependencies with BND instructions from our previous attempt (lines 520-593 in the backup). If NOT, we need to add them back with separate artifactIds:
- Each extension needs its own `<dependency>` with unique `<artifactId>onnxruntime-extensions-darwin-arm64</artifactId>` etc.
- Each needs BND instructions with `<instructions>` tag for wrapping

### 2. Clean Up Local Files (AFTER Maven works)
- Delete `lib/onnxruntime-extensions-*.jar` (all 6)
- Delete entire `src/main/resources/models/` directory
- Update `.classpath`: Remove 6 lib entries (lines 6-11)
- Update `build.properties`: Remove `src/main/resources/` from source.. line

### 3. Test Procedure
1. In Eclipse: Window → Preferences → Plug-in Development → Target Platform
2. Click "Reload" on active target
3. Project → Clean → Clean all projects
4. Restart Eclipse
5. Check console for: `[ServoyEmbeddings] Loading ONNX embedding model from bundle...`
6. Should see: `Models bundle not found` OR successful load

### 4. If Bundle Not Found
- Check bundle name in error message
- Verify in Eclipse: Run Configurations → Plugins tab → search "onnx"
- Bundle should be: `wrapped.com.servoy.eclipse.onnx-models-bge-small`
- If different prefix, update MANIFEST.MF line 87 and ServoyEmbeddingService.java line 60

## Backup Info
- Target definition backup: User created manually (ask where it is)
- Working local state: Current commit in git

## Key Insight
- Using `Platform.getBundle()` requires bundle to be in OSGi runtime
- Eclipse PDE wraps Maven JARs with `wrapped.` prefix
- Models JAR structure: `models/bge-small-en-v1.5/model.onnx` (no leading slash in bundle.getEntry())
- Extensions need platform detection logic (already in code, lines 92-114)

## If Something Breaks
1. Revert to local setup: restore lib/ JARs and resources/models/
2. Change ServoyEmbeddingService.java back to `getClass().getResourceAsStream("/main/resources/models/...")`
3. Remove Platform.getBundle() code
4. Remove bundle dependency from MANIFEST.MF
