#!/usr/bin/env bash
#
# Two-server integration test: a Velocity proxy + a Paper game server, both running the built BungeeSK
# jar, actually connected over BungeeSK's socket. This exercises the connection-dependent behaviour a
# single-server boot (scripts/run-syntax-tests.sh) can't reach:
#
#   1. the game server auto-connects to the proxy               ("Connected to the proxy")
#   2. the link stays up under the keep-alive                   (no "Lost the connection")
#   3. shutting the game server down WHILE CONNECTED is clean   (no IllegalPluginAccessException from
#      scheduling the disconnect event during onDisable)
#
# Usage: scripts/integration-test.sh [path-to-built-universal-BungeeSK-jar]
#   Defaults to build/libs/BungeeSK.jar (the :universalJar output).
#
# Env overrides: MC_VERSION, VELOCITY_VERSION, SKRIPT_VERSION, CONNECTED_SECONDS.

set -uo pipefail

MC_VERSION="${MC_VERSION:-1.21.11}"
VELOCITY_VERSION="${VELOCITY_VERSION:-3.5.1}"
SKRIPT_VERSION="${SKRIPT_VERSION:-2.16.0}"
CONNECTED_SECONDS="${CONNECTED_SECONDS:-40}"   # how long to hold the link before the graceful stop
PASSWORD="bungeesk-integration-test"
UA="BungeeSK-integration-test"
JAVA="${JAVA:-java}"

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:-$REPO_ROOT/build/libs/BungeeSK.jar}"
[ -f "$JAR" ] || { echo "Built universal jar not found: $JAR (run :universalJar first)"; exit 2; }

WORK="$(mktemp -d)"
PXY="$WORK/proxy"; SRV="$WORK/server"
mkdir -p "$PXY/plugins/bungeesk" "$SRV/plugins/BungeeSK"
cleanup() { pkill -f "$PXY/velocity.jar" 2>/dev/null; pkill -f "$SRV/paper.jar" 2>/dev/null; rm -rf "$WORK" 2>/dev/null; }
trap cleanup EXIT

fetch_papermc() { # project version outfile
  # Pull the newest build's download URL from the v3 "fill" API without needing jq: the builds array
  # is newest-first and each server jar URL embeds "<project>-<version>", so the first match wins.
  local url
  url="$(curl -fsSL -H "User-Agent: $UA" "https://fill.papermc.io/v3/projects/$1/versions/$2/builds" \
        | grep -oE "https://[^\"]*$1-$2[^\"]*\.jar" | head -1)"
  [ -n "$url" ] || { echo "no $1 build for $2"; exit 2; }
  curl -fsSL -H "User-Agent: $UA" -o "$3" "$url"
}

# Cache the (large) server jars between runs; a fresh CI run just downloads once.
CACHE="${INTEGRATION_CACHE:-$HOME/.cache/bungeesk-integration}"
mkdir -p "$CACHE"
VEL="$CACHE/velocity-$VELOCITY_VERSION.jar"
PAP="$CACHE/paper-$MC_VERSION.jar"
SKR="$CACHE/skript-$SKRIPT_VERSION.jar"
[ -f "$VEL" ] || { echo "==> Downloading Velocity $VELOCITY_VERSION"; fetch_papermc velocity "$VELOCITY_VERSION" "$VEL"; }
[ -f "$PAP" ] || { echo "==> Downloading Paper $MC_VERSION"; fetch_papermc paper "$MC_VERSION" "$PAP"; }
[ -f "$SKR" ] || { echo "==> Downloading Skript $SKRIPT_VERSION"; curl -fsSL -o "$SKR" \
  "https://repo.skriptlang.org/releases/com/github/SkriptLang/Skript/${SKRIPT_VERSION}/Skript-${SKRIPT_VERSION}.jar"; }
cp "$VEL" "$PXY/velocity.jar"
cp "$PAP" "$SRV/paper.jar"
cp "$SKR" "$SRV/plugins/Skript.jar"

# --- proxy: BungeeSK listens on the socket port with a known password ---
cp "$JAR" "$PXY/plugins/BungeeSK.jar"
cat > "$PXY/plugins/bungeesk/config.yml" <<YML
port: 20000
password: "$PASSWORD"
encrypt: true
YML

# --- game server: auto-connect to the proxy with the matching password ---
cp "$JAR" "$SRV/plugins/BungeeSK.jar"
echo "eula=true" > "$SRV/eula.txt"
printf 'online-mode=false\nlevel-type=flat\nmax-players=1\nserver-port=25566\nspawn-protection=0\n' > "$SRV/server.properties"
cat > "$SRV/plugins/BungeeSK/config.yml" <<YML
connection:
  auto-connect: true
  address: "127.0.0.1"
  port: 20000
  password: "$PASSWORD"
reconnect:
  enabled: true
  log-attempts: true
YML

echo "==> Booting proxy"
# timeout is a backstop so the proxy can't outlive the test if the EXIT cleanup can't signal it.
( cd "$PXY" && timeout $((CONNECTED_SECONDS + 90)) "$JAVA" -Xmx1024M -jar velocity.jar > "$PXY/proxy.log" 2>&1 ) &
PROXY_PID=$!
sleep 24

echo "==> Booting game server; waiting for the link, holding it ${CONNECTED_SECONDS}s, then a graceful stop"
# Feed the game server's console: wait until it is actually connected (don't race the boot — a "stop"
# sent before Paper's console is ready is lost), hold the link, then send "stop" and keep the pipe open
# while it shuts down. The feeder writes ONLY "stop" to stdout, so nothing else reaches the console.
{
  for _ in $(seq 1 90); do grep -q "Connected to the proxy" "$SRV/game.log" 2>/dev/null && break; sleep 1; done
  sleep "$CONNECTED_SECONDS"
  echo stop
  sleep 15
} | ( cd "$SRV" && timeout $((CONNECTED_SECONDS + 150)) "$JAVA" -Xmx1500M -jar paper.jar nogui > "$SRV/game.log" 2>&1 )

# The game server is down; the proxy has served its purpose — stop it so it can't hold the run open.
kill "$PROXY_PID" 2>/dev/null || true
pkill -f "$PXY/velocity.jar" 2>/dev/null || true

GLOG="$SRV/game.log"
echo "----- game server log (BungeeSK lines) -----"
grep -iE "Auto-connecting|Connected to the proxy|Lost the connection|Reconnected|Disabling BungeeSK|IllegalPluginAccess|register task while disabled" "$GLOG" | sed -E 's/\x1b\[[0-9;]*m//g' || true
echo "--------------------------------------------"

fail=0
grep -q "Connected to the proxy" "$GLOG" || { echo "FAIL: game server never connected to the proxy"; fail=1; }
if grep -q "Lost the connection" "$GLOG"; then echo "FAIL: the link dropped while connected (keep-alive regression)"; fail=1; fi
if grep -q "IllegalPluginAccessException\|register task while disabled" "$GLOG"; then
  echo "FAIL: scheduling during onDisable threw (shutdown-while-connected regression)"; fail=1; fi
grep -q "Disabling BungeeSK" "$GLOG" || { echo "FAIL: game server did not shut down cleanly"; fail=1; }

if [ "$fail" -eq 0 ]; then
  echo "INTEGRATION TEST PASSED: connected, stable, and clean shutdown while connected."
else
  echo "INTEGRATION TEST FAILED (see above)."; echo "--- proxy log tail ---"; tail -n 30 "$PXY/proxy.log" | sed -E 's/\x1b\[[0-9;]*m//g'
fi
exit "$fail"
