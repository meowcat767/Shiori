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
else
    echo "Build failed!"
    exit 1
fi

