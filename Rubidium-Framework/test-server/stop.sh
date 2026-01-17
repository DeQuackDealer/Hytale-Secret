#!/bin/bash
echo "Stopping Hytale server..."
pkill -f "HytaleServer.jar" || echo "Server not running"
