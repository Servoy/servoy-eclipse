# Building ONNX Runtime Extensions for x86_64 on an ARM64 Linux Machine

This guide provides detailed instructions on how to build the `onnxruntime-extensions.jar` for the `x86_64` (amd64) architecture on an ARM64 Linux machine.

## Context

The primary challenge is to compile for an `x86_64` architecture while on an `arm64` machine. This process was developed on a Parallels VM running ARM64 Ubuntu with Apple's Rosetta 2 support, which enables the execution of `x86_64` binaries.

The solution involves using Docker to create an isolated `x86_64` build environment where we can install the necessary dependencies and run the build process.

## Prerequisites

1.  An `arm64` Linux machine (e.g., a Parallels VM on an Apple Silicon Mac).
2.  Docker installed and configured. You can verify your installation by running `docker --version`.

## Step 1: Configure Docker for Cross-Platform Builds

To run `x86_64` containers on an `arm64` host, Docker needs to be configured for cross-platform emulation. Without this, you will encounter an `exec format error` when trying to run commands in the `x86_64` container.

Run the following command once to register the necessary handlers with `binfmt_misc`:

```bash
sudo docker run --privileged --rm tonistiigi/binfmt --install all
```

This command uses the `tonistiigi/binfmt` image to install the QEMU handlers that allow your system to emulate various architectures, including `x86_64`.

## Step 2: Create the Dockerfile

Create a file named `Dockerfile` with the following content. This file defines the `x86_64` build environment.

```dockerfile
FROM --platform=linux/amd64 ubuntu:24.04

# Set DEBIAN_FRONTEND to noninteractive
ARG DEBIAN_FRONTEND=noninteractive

# Install QEMU and dependencies
RUN apt-get update && apt-get install -y qemu-user-static git cmake maven openjdk-11-jdk-headless build-essential

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

**Key points about this Dockerfile:**

*   `FROM --platform=linux/amd64 ubuntu:24.04`: This specifies that we want to use the `x86_64` version of the Ubuntu 24.04 image. We use 24.04 to get a recent version of CMake (3.28+) which is required by the build.
*   `qemu-user-static`: Provides the emulation layer inside the container.
*   `build-essential`: Installs the C/C++ compilers and other essential build tools.

## Step 3: Clone the ONNX Runtime Extensions Repository

Clone the repository from GitHub into a local directory:

```bash
git clone https://github.com/microsoft/onnxruntime-extensions.git onnxruntime-extensions-x64
```

## Step 4: Create the Build Script

To simplify the build process and avoid command-line errors, create a shell script named `build_x64.sh` with the following content:

```bash
#!/bin/bash

# Exit on error
set -e

# Build the Docker image
echo "Building the x86_64 Docker image..."
sudo docker build -t onnx-x64-build .

# Run the build inside the Docker container
echo "Running the build inside the Docker container..."
sudo docker run -it --rm \
  -v "$(pwd)/onnxruntime-extensions-x64:/workspace/onnxruntime-extensions" \
  --workdir /workspace/onnxruntime-extensions \
  onnx-x64-build \
  ./build.sh -DOCOS_BUILD_JAVA=ON

echo "Build complete!"
```

This script automates the entire process: it builds the Docker image and then runs the `onnxruntime-extensions` build inside a container created from that image.

## Step 5: Run the Build

1.  **Make the script executable:**
    ```bash
    chmod +x build_x64.sh
    ```

2.  **Run the script:**
    ```bash
    ./build_x64.sh
    ```

The script will now execute, and the build process will start. This may take a significant amount of time.

## Step 6: Verify the Build

Once the build is complete, the generated JAR file will be located in the `onnxruntime-extensions-x64/java/build/libs/` directory.

You can verify that the JAR contains the `x86_64` native library by listing its contents:

```bash
unzip -l onnxruntime-extensions-x64/java/build/libs/onnxruntime-extensions-*.jar | grep libonnxruntime_extensions4j_jni.so
```

The output should contain a path that includes `linux-x64`, confirming that the native library is for the `x86_64` architecture:

```
  5877128  2025-11-05 14:10   ai/onnxruntime/extensions/native/linux-x64/libonnxruntime_extensions4j_jni.so
```
