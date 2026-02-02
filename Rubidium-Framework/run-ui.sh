#!/bin/bash
echo "========================================"
echo "  Rubidium Framework UI Launcher"
echo "  Version 1.0"
echo "========================================"
echo

JAR_PATH="build/libs/rubidium-dev-1.0.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: JAR not found at $JAR_PATH"
    echo "Please run: ./gradlew rubidiumDevJar"
    exit 1
fi

echo "Starting Rubidium UI..."
java -cp "$JAR_PATH:build/libs/*" rubidium.RubidiumLauncher
