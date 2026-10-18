#!/usr/bin/env bash
# Generates the server plugin API Javadoc (Permissions) into dist/javadoc.
set -euo pipefail
cd "$(dirname "$0")"
SERVER="${OPENVOXEL_SERVER_JAR:-}"
for c in server/openvoxel-server.jar ../../../ServerTest/openvoxel-server-0.0.8-SNAPSHOT.jar; do [ -z "$SERVER" ] && [ -f "$c" ] && SERVER="$c"; done
[ -n "$SERVER" ] || { echo "Server JAR not found; set OPENVOXEL_SERVER_JAR." >&2; exit 1; }
rm -rf dist/javadoc
javadoc --release 11 -quiet -Xdoclint:none -encoding UTF-8 -charset UTF-8 -windowtitle "OpenVoxel ModLoader server API" \
  -doctitle "OpenVoxel ModLoader server API" -sourcepath src/main/java -classpath "$SERVER" \
  -link https://docs.oracle.com/en/java/javase/11/docs/api/ -d dist/javadoc \
  src/main/java/com/happyuky7/openvoxelmoding/server/core/Permissions.java
echo "Javadoc in dist/javadoc"
