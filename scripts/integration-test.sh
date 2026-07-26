#!/usr/bin/env bash
#
# Two-server integration test: a Velocity proxy + a Paper game server, both running the built BungeeSK
# jar, actually connected over BungeeSK's socket. This exercises the connection-dependent behaviour a
# single-server boot (scripts/run-syntax-tests.sh) can't reach:
#
#   1. the game server auto-connects to the proxy               ("Connected to the proxy")
#   2. a real request/response round-trip works over the link   (network player count resolves)
#   3. the link stays up under the keep-alive                   (no "Lost the connection" while up)
#   4. a dropped proxy is detected and auto-reconnected          ("Lost the connection" -> "Reconnected")
#   5. shutting the game server down WHILE CONNECTED is clean    (no IllegalPluginAccessException)
#
# Usage: scripts/integration-test.sh [path-to-built-universal-BungeeSK-jar]
#   Defaults to build/libs/BungeeSK.jar (the :universalJar output).
#
# Env overrides: MC_VERSION, VELOCITY_VERSION, SKRIPT_VERSION, CONNECTED_SECONDS.

set -uo pipefail
# A write to a server console whose process has already gone (broken pipe) must not kill the harness.
trap '' PIPE

MC_VERSION="${MC_VERSION:-1.21.11}"
VELOCITY_VERSION="${VELOCITY_VERSION:-3.5.1}"
SKRIPT_VERSION="${SKRIPT_VERSION:-2.16.0}"
CONNECTED_SECONDS="${CONNECTED_SECONDS:-15}"   # how long to hold the link during the stability check
PASSWORD="bungeesk-integration-test"
UA="BungeeSK-integration-test"
JAVA="${JAVA:-java}"

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:-$REPO_ROOT/build/libs/BungeeSK.jar}"
[ -f "$JAR" ] || { echo "Built universal jar not found: $JAR (run :universalJar first)"; exit 2; }

WORK="$(mktemp -d)"
PXY="$WORK/proxy"; SRV="$WORK/server"
PROXY_LOG="$PXY/proxy.log"; GLOG="$SRV/game.log"
PROXY_FIFO="$PXY/console";  GAME_FIFO="$SRV/console"
mkdir -p "$PXY/plugins/bungeesk" "$SRV/plugins/BungeeSK" "$SRV/plugins/Skript/scripts"
cleanup() {
  exec 7>&- 2>/dev/null; exec 8>&- 2>/dev/null
  pkill -f "$PXY/velocity.jar" 2>/dev/null; pkill -f "$SRV/paper.jar" 2>/dev/null
  rm -rf "$WORK" 2>/dev/null
}
trap cleanup EXIT

fetch_papermc() { # project version outfile
  # Newest build's download URL from the v3 "fill" API without jq: builds are newest-first and each
  # server jar URL embeds "<project>-<version>", so the first match wins.
  local url
  url="$(curl -fsSL -H "User-Agent: $UA" "https://fill.papermc.io/v3/projects/$1/versions/$2/builds" \
        | grep -oE "https://[^\"]*$1-$2[^\"]*\.jar" | head -1)"
  [ -n "$url" ] || { echo "no $1 build for $2"; exit 2; }
  curl -fsSL -H "User-Agent: $UA" -o "$3" "$url"
}

# Wait up to $3 seconds for pattern $1 to appear in file $2.
wait_for() { local i; for ((i=0; i<$3; i++)); do grep -q "$1" "$2" 2>/dev/null && return 0; sleep 1; done; return 1; }

# Boot (or re-boot) the proxy with its console on fd 7. Truncates the log so "wait_for listening" only
# sees the current boot.
boot_proxy() {
  rm -f "$PROXY_FIFO"; mkfifo "$PROXY_FIFO"; : > "$PROXY_LOG"
  ( cd "$PXY" && timeout 300 "$JAVA" -Xmx1024M -jar velocity.jar < "$PROXY_FIFO" >> "$PROXY_LOG" 2>&1 ) &
  PROXY_JOB=$!
  exec 7> "$PROXY_FIFO"
}
stop_proxy() {
  echo shutdown >&7 2>/dev/null || true    # Velocity's stop command; EOF below also stops it
  exec 7>&- 2>/dev/null || true
  local i; for ((i=0; i<12; i++)); do kill -0 "$PROXY_JOB" 2>/dev/null || break; sleep 1; done
  pkill -f "$PXY/velocity.jar" 2>/dev/null || true
  wait "$PROXY_JOB" 2>/dev/null || true
}

# Cache the (large) server jars between runs; a fresh CI run just downloads once.
CACHE="${INTEGRATION_CACHE:-$HOME/.cache/bungeesk-integration}"
mkdir -p "$CACHE"
VEL="$CACHE/velocity-$VELOCITY_VERSION.jar"; PAP="$CACHE/paper-$MC_VERSION.jar"; SKR="$CACHE/skript-$SKRIPT_VERSION.jar"
[ -f "$VEL" ] || { echo "==> Downloading Velocity $VELOCITY_VERSION"; fetch_papermc velocity "$VELOCITY_VERSION" "$VEL"; }
[ -f "$PAP" ] || { echo "==> Downloading Paper $MC_VERSION"; fetch_papermc paper "$MC_VERSION" "$PAP"; }
[ -f "$SKR" ] || { echo "==> Downloading Skript $SKRIPT_VERSION"; curl -fsSL -o "$SKR" \
  "https://repo.skriptlang.org/releases/com/github/SkriptLang/Skript/${SKRIPT_VERSION}/Skript-${SKRIPT_VERSION}.jar"; }
cp "$VEL" "$PXY/velocity.jar"; cp "$PAP" "$SRV/paper.jar"; cp "$SKR" "$SRV/plugins/Skript.jar"

# --- proxy: BungeeSK listens on the socket port with a known password ---
cp "$JAR" "$PXY/plugins/BungeeSK.jar"
cat > "$PXY/plugins/bungeesk/config.yml" <<YML
port: 20000
password: "$PASSWORD"
encrypt: true
YML

# --- game server: auto-connect to the proxy + a script that proves a live round-trip ---
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
cat > "$SRV/plugins/Skript/scripts/roundtrip.sk" <<'SK'
on script load:
	wait 12 seconds
	set {_c} to network player count
	send "IT-ROUNDTRIP network-player-count=%{_c}%" to console
SK

fail=0
note() { echo "$1"; }

echo "==> Booting proxy"
boot_proxy
wait_for "Listening: 20000" "$PROXY_LOG" 60 || { echo "FAIL: proxy never started listening"; tail -n 20 "$PROXY_LOG"; exit 1; }

echo "==> Booting game server"
rm -f "$GAME_FIFO"; mkfifo "$GAME_FIFO"
( cd "$SRV" && timeout 300 "$JAVA" -Xmx1500M -jar paper.jar nogui < "$GAME_FIFO" > "$GLOG" 2>&1 ) &
GAME_JOB=$!
exec 8> "$GAME_FIFO"

# 1. connect
if wait_for "Connected to the proxy" "$GLOG" 120; then note "OK: connected to the proxy"; else note "FAIL: never connected to the proxy"; fail=1; fi

# 2. request/response round-trip over the live link (network player count resolves to a number)
if wait_for "IT-ROUNDTRIP network-player-count=" "$GLOG" 40; then
  if grep -qE "IT-ROUNDTRIP network-player-count=[0-9]" "$GLOG"; then note "OK: round-trip resolved (proxy answered)"
  else note "FAIL: round-trip returned no value (the proxy did not answer)"; fail=1; fi
else note "FAIL: round-trip line never logged"; fail=1; fi

# 3. stability: hold the link with the proxy up; no spurious keep-alive drop
sleep "$CONNECTED_SECONDS"
if grep -q "Lost the connection" "$GLOG"; then note "FAIL: link dropped while the proxy was up (keep-alive regression)"; fail=1
else note "OK: link stable for ${CONNECTED_SECONDS}s with the proxy up"; fi

# 4. reconnect: drop the proxy -> expect detection -> restart it -> expect a reconnect
note "==> Reconnect test: stopping the proxy"
stop_proxy
if wait_for "Lost the connection" "$GLOG" 30; then note "OK: game detected the dropped proxy"; else note "FAIL: game never noticed the proxy going away"; fail=1; fi
note "==> Restarting the proxy"
sleep 3   # let the OS release the socket port before the new proxy binds it
boot_proxy
wait_for "Listening: 20000" "$PROXY_LOG" 60 || note "WARN: proxy restart was slow"
if wait_for "Reconnected to the proxy" "$GLOG" 90; then note "OK: game auto-reconnected"; else note "FAIL: game did not reconnect"; fail=1; fi

# 5. clean shutdown WHILE connected (the callEvent-during-onDisable fix)
note "==> Graceful game-server shutdown while connected"
echo stop >&8 2>/dev/null || true
exec 8>&- 2>/dev/null || true
wait "$GAME_JOB" 2>/dev/null || true
if grep -qE "IllegalPluginAccessException|register task while disabled" "$GLOG"; then note "FAIL: scheduling during onDisable threw"; fail=1; fi
grep -q "Disabling BungeeSK" "$GLOG" || { note "FAIL: game server did not shut down cleanly"; fail=1; }
stop_proxy

echo "----- game server log (BungeeSK lines) -----"
grep -iE "Auto-connecting|Connected to the proxy|IT-ROUNDTRIP|Lost the connection|Reconnected|Disabling BungeeSK|IllegalPluginAccess" "$GLOG" | sed -E 's/\x1b\[[0-9;]*m//g' || true
echo "--------------------------------------------"

if [ "$fail" -eq 0 ]; then
  echo "INTEGRATION TEST PASSED: connect + round-trip + stable + reconnect + clean shutdown."
else
  echo "INTEGRATION TEST FAILED (see above)."
fi
exit "$fail"
