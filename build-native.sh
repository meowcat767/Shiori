#!/bin/bash
# Shiori GraalVM Native Image Build Script
# Builds a native executable from the Java application
#
# Prerequisites:
# 1. GraalVM JDK 21+ installed
# 2. native-image component installed: gu install native-image
#
# Usage:
#   ./build-native.sh

set -e

echo "=========================================="
echo "Shiori Native Image Build Script"
echo "=========================================="

# Check if GraalVM is being used
if java -version 2>&1 | grep -q "GraalVM"; then
    echo "✓ GraalVM detected"
else
    echo "⚠ Warning: GraalVM not detected. Please ensure you're using GraalVM JDK."
    echo "  Download from: https://www.graalvm.org/downloads/"
fi

# Check if native-image is installed
if command -v native-image &> /dev/null; then
    echo "✓ native-image command found"
else
    echo "✗ native-image command not found."
    echo "  Install it with: gu install native-image"
    exit 1
fi

# Build the JAR first
echo ""
echo "Step 1: Building JAR with Maven..."
mvn clean package -DskipTests

# Find the JAR file
JAR_FILE=$(find target -name "Shiori-*.jar" -type f | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "✗ No JAR file found in target directory!"
    echo "Available files in target:"
    ls -la target/*.jar 2>/dev/null || echo "No JAR files found"
    exit 1
fi
echo "✓ Found JAR: $JAR_FILE"

# Build native image with --add-exports flags to fix module resolution error
echo ""
echo "Step 2: Building native image..."
echo "      (This may take several minutes...)"

# The --add-exports flags must be passed to the JVM running native-image
# using --vm. prefix to pass options directly to the JVM
native-image \
    --vm.--add-exports=jdk.graal.compiler/jdk.graal.compiler.nodes.graphbuilderconf=ALL-UNNAMED \
    --vm.--add-exports=jdk.graal.compiler/jdk.graal.compiler.nodes=ALL-UNNAMED \
    --vm.--add-exports=jdk.graal.compiler/jdk.graal.compiler.api=ALL-UNNAMED \
    --vm.--add-exports=jdk.graal.compiler/jdk.graal.compiler.util=ALL-UNNAMED \
    --vm.--add-exports=jdk.graal.compiler/jdk.graal.compiler.word=ALL-UNNAMED \
    --no-fallback \
    --enable-all-security-services \
    --report-unsupported-elements-at-runtime \
    -cp "$JAR_FILE" \
    Main

if [ -f "./shiori" ]; then
    echo ""
    echo "=========================================="
    echo "✓ Native image build successful!"
    echo "=========================================="
    echo "Executable: ./shiori"
    echo ""
    echo "To run:"
    echo "  ./shiori"
    ls -lh shiori
else
    echo ""
    echo "✗ Native image build failed!"
    exit 1
fi

