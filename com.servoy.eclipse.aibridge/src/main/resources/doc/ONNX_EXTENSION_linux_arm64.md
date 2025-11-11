# Building ONNX Runtime Extensions for ARM64 on Linux

This guide details the process of building the `onnxruntime-extensions.jar` for the ARM64 architecture on a native ARM64 Linux machine.

## 1. Initial Machine Setup

Start with a fresh ARM64 Linux VM (e.g., Ubuntu).

### 1.1. Update Package Lists

```bash
sudo apt-get update
```

### 1.2. Install Essential Tools

Install `git`, `cmake`, `maven`, and the ARM64 `openjdk-21-jdk`.

```bash
sudo apt-get install -y git cmake maven openjdk-21-jdk-headless
```

## 2. Clone the ONNX Runtime Extensions Repository

Clone the repository from GitHub.

```bash
git clone https://github.com/microsoft/onnxruntime-extensions.git
cd onnxruntime-extensions
```

## 3. Build for Linux ARM64 (aarch64)

### 3.1. Set JAVA_HOME for ARM64

Point `JAVA_HOME` to the native ARM64 JDK.

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
```

### 3.2. Run the ARM64 Build

Execute the build script to generate the ARM64 JAR.

```bash
./build.sh -DOCOS_BUILD_JAVA=ON
```

### 3.3. Verify the ARM64 JAR

After the build completes, a JAR file will be created in `onnxruntime-extensions/java/build/libs/`. You can verify its contents to ensure it contains the ARM64 native library (`libortx_jni.so`).

```bash
# Navigate to the directory containing the JAR
cd java/build/libs/

# List the contents of the JAR and filter for the native library
unzip -l onnxruntime-extensions-*.jar | grep libortx_jni.so
```

The output should show the path to the native library, for example:
`ai/onnxruntime/extensions/native/linux/aarch64/libortx_jni.so`
