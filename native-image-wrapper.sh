#!/bin/bash
# Shiori Native Image Build Wrapper
# This script sets up the proper environment for GraalVM native-image
# to fix the module resolution error with Java 21+
#
# Error: "Module org.graalvm.nativeimage.foreign does not read a module that exports jdk.graal.compiler.nodes.graphbuilderconf"
#
# Usage: ./native-image-wrapper.sh [native-image args...]

# Set JAVA_HOME to GraalVM
export JAVA_HOME="${JAVA_HOME:-/home/deck/graalvm-jdk-25.0.2+10.1}"

# Set PATH to include GraalVM binaries
export PATH="$JAVA_HOME/bin:$PATH"

# JAVA_TOOL_OPTIONS is read by the JVM that native-image spawns internally
# These --add-exports flags allow the GraalVM compiler to access internal modules
export JAVA_TOOL_OPTIONS="--add-exports=jdk.graal.compiler/jdk.graal.compiler.nodes.graphbuilderconf=ALL-UNNAMED --add-exports=jdk.graal.compiler/jdk.graal.compiler.nodes=ALL-UNNAMED --add-exports=jdk.graal.compiler/jdk.graal.compiler.api=ALL-UNNAMED --add-exports=jdk.graal.compiler/jdk.graal.compiler.util=ALL-UNNAMED --add-exports=jdk.graal.compiler/jdk.graal.compiler.word=ALL-UNNAMED --add-reads=jdk.graal.compiler=ALL-UNNAMED"

echo "Building Shiori native image..."
echo "JAVA_HOME: $JAVA_HOME"
echo "Using native-image from: $(which native-image)"
echo ""

# Check if native-image exists
if ! command -v native-image &> /dev/null; then
    echo "Error: native-image command not found in PATH"
    echo "Make sure GraalVM is installed and in PATH"
    exit 1
fi

# Run native-image with all arguments passed to this script
native-image "$@"

exit $?

