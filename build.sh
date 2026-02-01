#!/bin/bash
# Shiori Build Script - Sets up correct Java environment

# Unset JAVA_HOME to use the default Java 21 in PATH
# (The project's pom.xml is configured for Java 21)
unset JAVA_HOME

echo "Building Shiori with Java 21..."

# Run Maven build
mvn clean compile

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo ""
    echo "To run the application:"
    echo "  mvn exec:java"
    echo ""
    echo "To build native image with GraalVM:"
    echo "  mvn package -DskipTests"
    echo "  native-image \\"
    echo "    --no-fallback \\"
    echo "    --enable-all-security-services \\"
    echo "    --report-unsupported-elements-at-runtime \\"
    echo "    --add-exports=jdk.graal.compiler/jdk.graal.compiler.nodes.graphbuilderconf=ALL-UNNAMED \\"
    echo "    --add-exports=jdk.graal.compiler/jdk.graal.compiler.nodes=ALL-UNNAMED \\"
    echo "    --add-exports=jdk.graal.compiler/jdk.graal.compiler.api=ALL-UNNAMED \\"
    echo "    --add-exports=jdk.graal.compiler/jdk.graal.compiler.util=ALL-UNNAMED \\"
    echo "    --add-exports=jdk.graal.compiler/jdk.graal.compiler.word=ALL-UNNAMED \\"
    echo "    -cp target/Shiori-1.0.jar Main"
else
    echo "Build failed!"
    exit 1
fi

