#!/usr/bin/env bash
# Plain-javac build of the server core plugin. Equivalent to: ./gradlew jar
set -euo pipefail
cd "$(dirname "$0")"
SERVER="${OPENVOXEL_SERVER_JAR:-}"
for c in server/openvoxel-server.jar ../../../ServerTest/openvoxel-server-0.0.8-SNAPSHOT.jar; do [ -z "$SERVER" ] && [ -f "$c" ] && SERVER="$c"; done
[ -n "$SERVER" ] || { echo "Server JAR not found. Set OPENVOXEL_SERVER_JAR or copy it to server/openvoxel-server.jar" >&2; exit 1; }
OUT="dist/ModLoader-v1.0.0-vgame-0.0.8-server.jar"
rm -rf build/classes && mkdir -p build/classes dist
find src/main/java -name '*.java' > build/sources.txt
javac --release 11 -encoding UTF-8 -Xlint:-options -classpath "$SERVER" -d build/classes @build/sources.txt
cp -r src/main/resources/. build/classes/
jar --create --file "$OUT" -C build/classes .
echo "Built $OUT"
