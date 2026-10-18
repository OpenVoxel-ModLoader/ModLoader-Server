#!/usr/bin/env bash
# Builds the ready-to-use server bundle: dist/ModLoader-Server-v1.1.0-pre2-vgame-0.0.8.zip
set -euo pipefail
cd "$(dirname "$0")"
JAR="dist/ModLoader-v1.1.0-pre2-vgame-0.0.8-server.jar"
[ -f "$JAR" ] || ./build.sh
STAGE="$(mktemp -d)/ModLoader-Server"
mkdir -p "$STAGE/dist" "$STAGE"/{mods,plugins,server,logs,agent}
cp agent/README.txt "$STAGE/agent/"
AGENT_JAR="$(ls -t ../ModLoader-Client/dist/ModLoader-v*-client.jar 2>/dev/null | head -n 1 || true)"
[ -n "$AGENT_JAR" ] && cp "$AGENT_JAR" "$STAGE/agent/"
cp "$JAR" "$STAGE/dist/"
cp start.bat start.ps1 start.sh server.properties.yml README.md LICENSE "$STAGE/"
chmod +x "$STAGE/start.sh"
echo "Download openvoxel-server-0.0.8-SNAPSHOT.jar from open-voxel.net and save it here as openvoxel-server.jar (or set server.jar in server.properties.yml)." > "$STAGE/server/PUT-openvoxel-server.jar-HERE.txt"
ZIP="$PWD/dist/ModLoader-Server-v1.1.0-pre2-vgame-0.0.8.zip"
rm -f "$ZIP"
(cd "$STAGE/.." && zip -qr "$ZIP" ModLoader-Server)
rm -rf "$(dirname "$STAGE")"
echo "Built $ZIP"
