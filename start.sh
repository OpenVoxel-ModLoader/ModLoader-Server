#!/usr/bin/env bash
# OpenVoxel ModLoader - dedicated server launcher (Linux/macOS). Console input stays attached.
set -uo pipefail
cd "$(dirname "$0")"
cfg() { awk -v sec="$1" -v key="$2" '
  /^[^ #][^:]*:[ ]*$/ { s=$1; sub(":","",s); next }
  /^  [a-z-]+:/ { k=$1; sub(":","",k); if (s==sec && k==key) { $1=""; sub(/^ +/,""); gsub(/^["'"'"']|["'"'"']$/,""); print; exit } }' server.properties.yml; }
mkdir -p mods plugins logs crash-reports dist
JAR="$(cfg server jar)"; [ -n "$JAR" ] && JAR="$PWD/$JAR"
[ -z "$JAR" ] && JAR="${OPENVOXEL_SERVER_JAR:-}"
for c in server/openvoxel-server.jar ../../../ServerTest/openvoxel-server-0.0.8-SNAPSHOT.jar; do [ -z "$JAR" ] && [ -f "$c" ] && JAR="$PWD/$c"; done
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "EN: Server JAR not found. Download openvoxel-server-*.jar and set server.jar in server.properties.yml (or copy it to server/openvoxel-server.jar)." >&2
  echo "ES: No se encontró el JAR del servidor. Descarga openvoxel-server-*.jar y configura server.jar en server.properties.yml (o cópialo a server/openvoxel-server.jar)." >&2
  exit 1
fi
CORE="dist/ModLoader-v1.1.0-pre2-vgame-0.0.8-server.jar"
[ -f "$CORE" ] || OPENVOXEL_SERVER_JAR="$JAR" ./build.sh
for old in plugins/ModLoader-v*-server.jar; do [ -e "$old" ] && [ "$(basename "$old")" != "$(basename "$CORE")" ] && rm -f "$old"; done
cp -f "$CORE" plugins/
[ "$(cfg runtime auto-deploy-mods)" = "true" ] && cp -f mods/*.jar plugins/ 2>/dev/null
JAVA="$(cfg jvm java)"; JAVA="${JAVA:-java}"
ARGS=(-Xms"$(cfg jvm min-memory)" -Xmx"$(cfg jvm max-memory)")
[ "$(cfg jvm use-g1gc)" = "true" ] && ARGS+=(-XX:+UseG1GC)
ARGS+=(-jar "$JAR" --port "$(cfg server port)" --seed "$(cfg server seed)" --online-mode "$(cfg server online-mode)" --auth-server "$(cfg server auth-server)")
SID="$(cfg server server-id)"
case "$SID" in ""|auto|auto-from-client) ;; *) ARGS+=(--server-id "$SID");; esac
echo "OpenVoxel ModLoader server - output is shown here and saved to logs/latest.log"
echo "Console: help, say <msg>, players, kick <name>, stop   |   Consola: help, say <msg>, players, kick <nombre>, stop"
"$JAVA" "${ARGS[@]}" 2>&1 | tee logs/latest.log
CODE=${PIPESTATUS[0]}
[ "$CODE" != "0" ] && [ "$(cfg runtime crash-reports)" = "true" ] && printf 'Exit code: %s\nSee: logs/latest.log\n' "$CODE" > crash-reports/latest-crash.txt
exit "$CODE"
