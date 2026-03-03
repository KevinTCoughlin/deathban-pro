#!/bin/bash
# Deploy plugin to local Minecraft server for testing
# Usage: ./scripts/deploy-local.sh [server-path]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Default server path (can be overridden by argument)
SERVER_PATH="${1:-$HOME/Development/minecraft/dev-server}"
PLUGINS_PATH="$SERVER_PATH/plugins"

# Check if server exists
if [ ! -d "$PLUGINS_PATH" ]; then
    echo "Error: Server plugins directory not found at $PLUGINS_PATH"
    echo "Usage: $0 [server-path]"
    exit 1
fi

# Build the plugin
echo "Building plugin..."
cd "$PROJECT_DIR"
./gradlew shadowJar --no-daemon -q

# Find the built JAR
JAR_FILE=$(ls -1 build/libs/*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "Error: No JAR file found in build/libs/"
    exit 1
fi

# Copy to server
echo "Deploying $JAR_FILE to $PLUGINS_PATH/DeathBanPro.jar"
cp "$JAR_FILE" "$PLUGINS_PATH/DeathBanPro.jar"

echo "Done! Restart the server or use /plugman reload DeathBanPro"
