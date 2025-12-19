## Building ONNX Runtime Extensions for Java

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

