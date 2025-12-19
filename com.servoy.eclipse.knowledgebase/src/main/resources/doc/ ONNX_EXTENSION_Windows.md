# Windows Build Guide (from Windows 11 ARM host)

This guide documents every step needed to build the ONNX Runtime Extensions JAR and native binaries for both **Windows x64** and **Windows ARM64** when working on a **Windows 11 ARM** VM (e.g., Parallels on Apple Silicon).

**Architecture Coverage:**
- **Windows x64 (Section A)**: Cross-compilation for Intel/AMD processors - required for compatibility with most existing Windows systems
- **Windows ARM64 (Section B)**: Native build for ARM processors - optimal performance on Windows 11 ARM devices

Both builds can be performed from the same Windows 11 ARM host using Visual Studio's toolchain.

---

## 1. Prerequisites

All commands were executed in **PowerShell** unless noted.

### 1.1 Install Git
```powershell
winget install --id Git.Git -e --source winget
```
Open a new PowerShell session afterwards and confirm:
```powershell
git --version
```

### 1.2 Install Visual Studio 2022 Build Tools

**Important for ARM64 builds:** If you previously installed Build Tools without ARM64 support, you must uninstall and reinstall to avoid toolchain conflicts. Mixed x64/ARM64 toolchains can cause build failures.

**Uninstall existing Build Tools (if applicable):**
```powershell
winget uninstall --id Microsoft.VisualStudio.2022.BuildTools
```

**Install with both x64 and ARM64 support:**
```powershell
winget install --id Microsoft.VisualStudio.2022.BuildTools -e `
  --override '--quiet --wait --norestart `
    --add Microsoft.VisualStudio.Workload.VCTools `
    --add Microsoft.VisualStudio.Component.Windows11SDK.22621 `
    --add Microsoft.VisualStudio.Component.VC.CMake.Project `
    --add Microsoft.VisualStudio.Component.VC.Tools.ARM64'
```

**Why ARM64 tools are needed:** The `Microsoft.VisualStudio.Component.VC.Tools.ARM64` component provides native ARM64 compilers and build tools. Without this, you can only cross-compile to x64, and native ARM64 builds will fail.

### 1.3 Install JDK 21

**For x64 builds only:**
```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e --architecture x64
```

**For ARM64 builds, uninstall x64 JDK and install ARM64 version:**

**Why this is necessary:** Having both x64 and ARM64 JDKs installed simultaneously can cause conflicts during the build process. The Gradle build system may pick up the wrong architecture's JDK, leading to build failures or incorrect binaries.

```powershell
# Uninstall x64 JDK
winget uninstall --id EclipseAdoptium.Temurin.21.JDK

# Install ARM64 JDK
winget install --id EclipseAdoptium.Temurin.21.JDK -e --architecture arm64
```

```powershell
java -version
```

**Note:** After switching between x64 and ARM64 JDKs, always verify the architecture matches your build target.

---

# SECTION A: Building for Windows x64 (Cross-compilation)

---

## A1. Prepare the x64 build environment

1. **Start a plain Command Prompt** (`cmd.exe`).
2. Load the Visual Studio toolchain with x64 host & target:
   ```cmd
   call "%ProgramFiles(x86)%\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=x64 -host_arch=x64
   ```
3. Set JDK variables:
   ```cmd
   set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot
   set PATH=%JAVA_HOME%\bin;%PATH%
   java -version
   ```
4. Ensure MSBuild resolves to the x64 binary (the cross-tools environment still exposes the ARM build):
   ```cmd
   set PATH=C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\MSBuild\Current\Bin\amd64;%PATH%
   where msbuild
   dumpbin /headers "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\MSBuild\Current\Bin\amd64\MSBuild.exe" | find "machine"
   ```
   Output should include `8664 machine (x64)`.

5. Set CMake path (required for build.bat to find CMake):
   ```cmd
   set cmake_exe="C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe"
   ```

---

## A2. Clone the repository
```cmd
cd %USERPROFILE%
git clone https://github.com/microsoft/onnxruntime-extensions.git
cd onnxruntime-extensions
```

---

## A3. Update Gradle wrapper
The original wrapper targets Gradle 8.0.1 (not compatible with JDK 21). Update `java\gradle\wrapper\gradle-wrapper.properties`:
```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```
Remove `distributionSha256Sum` if present.

---

## A4. Clean any previous build artifacts
If you attempted other builds, wipe the output directory:
```cmd
rmdir /S /Q out\Windows
```

---

## A5. Run the Windows x64 build
Use the explicit architecture flags (required on ARM hosts):
```cmd
build.bat -A x64 -T host=x64 -DOCOS_BUILD_JAVA=ON
```
Key log lines to confirm:
- Compiler paths point to `...\bin\Hostx64\x64\cl.exe`
- `CMAKE_GENERATOR_PLATFORM=x64`

The build downloads dependencies, compiles native code, and runs the Gradle task with Gradle 8.5.

---

## A6. Verify x64 outputs

### A6.1 JAR contents
```cmd
jar tf out\Windows\java\build\libs\onnxruntime-extensions-0.15.0.jar
```
Ensure it contains:
```
ai/onnxruntime/extensions/native/win-x64/onnxruntime_extensions4j_jni.dll
```

### A6.2 Native binary architecture
```cmd
dumpbin /headers out\Windows\bin\RelWithDebInfo\onnxruntime_extensions4j_jni.dll | find "machine"
```
Expect:
```
8664 machine (x64)
```

The combined jar + DLL confirms an x64 package built from the ARM host.

---

## A7. Copy x64 artifacts to the plugin
After verification, copy the jar into the plugin (adjust the target path as needed):
```cmd
copy out\Windows\java\build\libs\onnxruntime-extensions-0.15.0.jar \path\to\servoy-eclipse\com.servoy.eclipse.aibridge\lib\onnxruntime-extensions-windows-x64-0.15.0.jar
```
Rename the jar to make the target architecture explicit. Update the plugin manifest/classpath when integrating.

---

# SECTION B: Building for Windows ARM64 (Native)

---

## B1. Switch to ARM64 JDK

**Important:** Before building ARM64, you must switch from x64 JDK to ARM64 JDK to avoid conflicts.

```powershell
# Uninstall x64 JDK
winget uninstall --id EclipseAdoptium.Temurin.21.JDK

# Install ARM64 JDK
winget install --id EclipseAdoptium.Temurin.21.JDK -e --architecture arm64
```

Verify:
```powershell
java -version
```

---

## B2. Prepare the ARM64 build environment

1. **Start a new Command Prompt** (`cmd.exe`).
2. Load the Visual Studio toolchain with ARM64 host & target:
   ```cmd
   call "%ProgramFiles(x86)%\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64
   ```
3. Verify the environment:
   ```cmd
   echo %VSCMD_ARG_TGT_ARCH%
   echo %VSCMD_ARG_HOST_ARCH%
   where cl
   ```
   Expected output:
   - `VSCMD_ARG_TGT_ARCH=arm64`
   - `VSCMD_ARG_HOST_ARCH=arm64`
   - `cl` path should contain `HostARM64\arm64\cl.exe`

4. Set JDK and CMake variables:
   ```cmd
   set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot
   set PATH=%JAVA_HOME%\bin;%PATH%
   set cmake_exe="C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe"
   java -version
   ```

---

## B3. Navigate to repository
```cmd
cd %USERPROFILE%\git\onnxruntime-extensions
```

(Gradle wrapper should already be updated from Section A. If not, update it as shown in A3.)

---

## B4. Clean previous build artifacts
```cmd
rmdir /S /Q out\Windows
```

---

## B5. Run the Windows ARM64 build
```cmd
build.bat -A ARM64 -T host=ARM64 -DOCOS_BUILD_JAVA=ON
```
Key log lines to confirm:
- Compiler paths point to `...\bin\HostARM64\arm64\cl.exe`
- `CMAKE_GENERATOR_PLATFORM=ARM64`

The build runs natively on ARM64 (no emulation), making it faster than x64 cross-compilation.

---

## B6. Verify ARM64 outputs

### B6.1 JAR contents
```cmd
jar tf java\build\libs\onnxruntime-extensions-0.15.0.jar | findstr win-aarch64
```
Ensure it contains:
```
ai/onnxruntime/extensions/native/win-aarch64/onnxruntime_extensions4j_jni.dll
```

**Note:** The path uses `win-aarch64` (not `win-arm64`). This is the standard naming convention - `aarch64` is the official architecture identifier for 64-bit ARM.

### B6.2 Native binary architecture
```cmd
"C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC\14.44.35207\bin\HostARM64\arm64\dumpbin.exe" /headers out\Windows\bin\RelWithDebInfo\onnxruntime_extensions4j_jni.dll | findstr /i "machine"
```
Expect:
```
AA64 machine (ARM64)
```

The combined JAR + DLL confirms a native ARM64 package.

---

## B7. Copy ARM64 artifacts to the plugin
After verification, copy the jar into the plugin (adjust the target path as needed):
```cmd
copy java\build\libs\onnxruntime-extensions-0.15.0.jar \path\to\servoy-eclipse\com.servoy.eclipse.aibridge\lib\onnxruntime-extensions-windows-aarch64-0.15.0.jar
```
Rename the jar to make the target architecture explicit. Update the plugin manifest/classpath when integrating.

---

# Final Notes

## Optional sanity checks (for any build):
- `dir out\Windows\bin\RelWithDebInfo` – inspect all generated binaries
- `dumpbin /headers out\Windows\lib\RelWithDebInfo\onnxruntime_extensions.dll | findstr /i "machine"` – confirm support library architecture

## Next steps:
- Write a small standalone Java test to load `OrtxPackage.getLibraryPath()` and run the tokenizer to validate runtime loading on Windows (test both architectures if possible)
- Merge the Windows x64, Windows ARM64, macOS ARM64, and macOS x86_64 JARs into your deployment strategy
- Update plugin manifest to include both Windows architecture variants

---

## Summary

This guide enables building ONNX Runtime Extensions for both Windows architectures from a single Windows 11 ARM VM:

**Windows x64 (Section A):**
- Cross-compiled using `build.bat -A x64 -T host=x64 -DOCOS_BUILD_JAVA=ON`
- Produces: `ai/onnxruntime/extensions/native/win-x64/onnxruntime_extensions4j_jni.dll`
- Requires: x64 JDK, x64 toolchain via VsDevCmd.bat
- Compatible with Intel/AMD Windows systems

**Windows ARM64 (Section B):**
- Native build using `build.bat -A ARM64 -T host=ARM64 -DOCOS_BUILD_JAVA=ON`
- Produces: `ai/onnxruntime/extensions/native/win-aarch64/onnxruntime_extensions4j_jni.dll`
- Requires: ARM64 JDK, ARM64 toolchain via VsDevCmd.bat
- Native performance on Windows 11 ARM devices

**Key Requirements:**
1. Visual Studio 2022 Build Tools with ARM64 components (`Microsoft.VisualStudio.Component.VC.Tools.ARM64`)
2. Gradle 8.5 wrapper update for JDK 21 compatibility
3. Switch JDK architecture between builds to avoid conflicts
4. Proper environment setup via `VsDevCmd.bat` with correct architecture flags
5. Set `cmake_exe` variable before running build.bat
