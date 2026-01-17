#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

if [ ! -f "launcher/build/libs/TestServerLauncher.jar" ]; then
    echo "Launcher not built yet. Building now..."
    cd launcher && ./build-launcher.sh && cd ..
fi

echo "Starting Rubidium Test Server Launcher..."
java -jar launcher/build/libs/TestServerLauncher.jar
