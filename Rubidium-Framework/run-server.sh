#!/bin/bash

# Rubidium Test Server Runner
# This script builds and runs a local Rubidium server for testing

echo "==================================="
echo "   Rubidium Framework Test Server  "
echo "==================================="
echo ""

# Check Java version
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed. Please install Java 25 or later."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "[INFO] Java version: $JAVA_VERSION"

# Create server directory structure
SERVER_DIR="./test-server"
mkdir -p "$SERVER_DIR"
mkdir -p "$SERVER_DIR/plugins"
mkdir -p "$SERVER_DIR/worlds"
mkdir -p "$SERVER_DIR/logs"
mkdir -p "$SERVER_DIR/config"

# Build the framework if needed
if [ ! -f "build/libs/Rubidium-1.0.0.jar" ] || [ "$1" == "--rebuild" ]; then
    echo "[INFO] Building Rubidium Framework..."
    ./gradlew shadowJar --quiet
    if [ $? -ne 0 ]; then
        echo "[ERROR] Build failed!"
        exit 1
    fi
    echo "[INFO] Build complete!"
fi

# Copy JAR to server directory
cp build/libs/Rubidium-1.0.0.jar "$SERVER_DIR/rubidium.jar"

# Create default server config if not exists
if [ ! -f "$SERVER_DIR/config/server.properties" ]; then
    cat > "$SERVER_DIR/config/server.properties" << 'EOF'
# Rubidium Server Configuration
server-name=Rubidium Test Server
server-port=25565
max-players=20
view-distance=10
simulation-distance=8
online-mode=false
enable-command-block=true
spawn-protection=0
debug-mode=true
EOF
    echo "[INFO] Created default server.properties"
fi

# Create EULA file
echo "eula=true" > "$SERVER_DIR/eula.txt"

echo ""
echo "[INFO] Starting Rubidium server..."
echo "[INFO] Server directory: $SERVER_DIR"
echo "[INFO] Press Ctrl+C to stop the server"
echo ""
echo "-----------------------------------"

cd "$SERVER_DIR"
java -Xms512M -Xmx2G \
    -XX:+UseG1GC \
    -XX:+ParallelRefProcEnabled \
    -XX:MaxGCPauseMillis=200 \
    -jar rubidium.jar \
    --nogui \
    2>&1 | tee logs/latest.log

echo ""
echo "[INFO] Server stopped."
