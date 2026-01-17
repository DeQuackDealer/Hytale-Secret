#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "Building Rubidium Test Server Launcher..."

./gradlew fatJar --quiet

if [ -f "build/libs/TestServerLauncher.jar" ]; then
    echo "Build successful!"
    echo "JAR location: launcher/build/libs/TestServerLauncher.jar"
    echo ""
    echo "To run: java -jar launcher/build/libs/TestServerLauncher.jar"
else
    echo "Build failed. Check for errors above."
    exit 1
fi
