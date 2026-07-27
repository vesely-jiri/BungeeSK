#!/usr/bin/env bash
#
# Two-server integration test: a Velocity proxy + TWO Paper game servers, all running the built BungeeSK
# jar, actually connected over BungeeSK's socket. This exercises the connection-dependent behaviour a
# single-server boot (scripts/run-syntax-tests.sh) can't reach:
#
#   1. both game servers auto-connect to the proxy             ("Connected to the proxy")
#   2. a real request/response round-trip works over the link  (network player count resolves)
#   3. the link stays up under the keep-alive                  (no "Lost the connection" while up)
#   4. a dropped proxy is detected and BOTH auto-reconnect      ("Lost the connection" -> "Reconnected"),
#      and doing so together does NOT trigger an encryption desync — the bug where the proxy broadcast a
#      plaintext BungeeServerStartPacket into a peer's auth window and the peer, already encrypting, read
#      it as a byte[] and threw ("... cannot be cast to [B" / ClassCastException).
#   5. shutting the game servers down WHILE CONNECTED is clean  (no IllegalPluginAccessException)
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
PXY="$WORK/proxy"
PROXY_LOG="$PXY/proxy.log"; PROXY_FIFO="$PXY/console"
SRV1="$WORK/server1"; SRV2="$WORK/server2"
GLOG1="$SRV1/game.log"; GLOG2="$SRV2/game.log"
GAME_FIFO1="$SRV1/console"; GAME_FIFO2="$SRV2/console"
mkdir -p "$PXY/plugins/bungeesk"
mkdir -p "$SRV1/plugins/BungeeSK" "$SRV1/plugins/Skript/scripts"
mkdir -p "$SRV2/plugins/BungeeSK" "$SRV2/plugins/Skript/scripts"
cleanup() {
  exec 7>&- 2>/dev/null; exec 8>&- 2>/dev/null; exec 9>&- 2>/dev/null
  pkill -f "$PXY/velocity.jar" 2>/dev/null
  pkill -f "$SRV1/paper.jar" 2>/dev/null; pkill -f "$SRV2/paper.jar" 2>/dev/null
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
cp "$VEL" "$PXY/velocity.jar"
cp "$PAP" "$SRV1/paper.jar"; cp "$SKR" "$SRV1/plugins/Skript.jar"
cp "$PAP" "$SRV2/paper.jar"; cp "$SKR" "$SRV2/plugins/Skript.jar"

# --- proxy: BungeeSK listens on the socket port with a known password. encrypt: true is REQUIRED to
#     exercise the reconnect encryption-desync path. ---
cp "$JAR" "$PXY/plugins/BungeeSK.jar"
cat > "$PXY/plugins/bungeesk/config.yml" <<YML
port: 20000
password: "$PASSWORD"
encrypt: true
YML

# --- game server: auto-connect to the proxy ---
setup_game() { # dir port
  cp "$JAR" "$1/plugins/BungeeSK.jar"
  echo "eula=true" > "$1/eula.txt"
  printf 'online-mode=false\nlevel-type=flat\nmax-players=1\nserver-port=%s\nspawn-protection=0\n' "$2" > "$1/server.properties"
  cat > "$1/plugins/BungeeSK/config.yml" <<YML
connection:
  auto-connect: true
  address: "127.0.0.1"
  port: 20000
  password: "$PASSWORD"
reconnect:
  enabled: true
  log-attempts: true
YML
}
setup_game "$SRV1" 25566
setup_game "$SRV2" 25567
# a script that proves a live round-trip (server1 only)
cat > "$SRV1/plugins/Skript/scripts/roundtrip.sk" <<'SK'
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

echo "==> Booting game server 1"
rm -f "$GAME_FIFO1"; mkfifo "$GAME_FIFO1"
( cd "$SRV1" && timeout 300 "$JAVA" -Xmx1024M -jar paper.jar nogui < "$GAME_FIFO1" > "$GLOG1" 2>&1 ) &
GAME_JOB1=$!
exec 8> "$GAME_FIFO1"

echo "==> Booting game server 2"
rm -f "$GAME_FIFO2"; mkfifo "$GAME_FIFO2"
( cd "$SRV2" && timeout 300 "$JAVA" -Xmx1024M -jar paper.jar nogui < "$GAME_FIFO2" > "$GLOG2" 2>&1 ) &
GAME_JOB2=$!
exec 9> "$GAME_FIFO2"

# 1. connect (both)
if wait_for "Connected to the proxy" "$GLOG1" 120; then note "OK: server1 connected to the proxy"; else note "FAIL: server1 never connected"; fail=1; fi
if wait_for "Connected to the proxy" "$GLOG2" 120; then note "OK: server2 connected to the proxy"; else note "FAIL: server2 never connected"; fail=1; fi

# 2. request/response round-trip over the live link (network player count resolves to a number)
if wait_for "IT-ROUNDTRIP network-player-count=" "$GLOG1" 40; then
  if grep -qE "IT-ROUNDTRIP network-player-count=[0-9]" "$GLOG1"; then note "OK: round-trip resolved (proxy answered)"
  else note "FAIL: round-trip returned no value (the proxy did not answer)"; fail=1; fi
else note "FAIL: round-trip line never logged"; fail=1; fi

# 3. stability: hold the link with the proxy up; no spurious keep-alive drop
sleep "$CONNECTED_SECONDS"
if grep -q "Lost the connection" "$GLOG1" || grep -q "Lost the connection" "$GLOG2"; then note "FAIL: a link dropped while the proxy was up (keep-alive regression)"; fail=1
else note "OK: both links stable for ${CONNECTED_SECONDS}s with the proxy up"; fi

# 4. reconnect storm: drop the proxy -> both detect -> restart it -> both reconnect TOGETHER
note "==> Reconnect test: stopping the proxy (both servers should drop)"
stop_proxy
if wait_for "Lost the connection" "$GLOG1" 30; then note "OK: server1 detected the dropped proxy"; else note "FAIL: server1 never noticed the proxy going away"; fail=1; fi
if wait_for "Lost the connection" "$GLOG2" 30; then note "OK: server2 detected the dropped proxy"; else note "FAIL: server2 never noticed the proxy going away"; fail=1; fi
note "==> Restarting the proxy (both reconnect together)"
sleep 3   # let the OS release the socket port before the new proxy binds it
boot_proxy
wait_for "Listening: 20000" "$PROXY_LOG" 60 || note "WARN: proxy restart was slow"
if wait_for "Reconnected to the proxy" "$GLOG1" 90; then note "OK: server1 auto-reconnected"; else note "FAIL: server1 did not reconnect"; fail=1; fi
if wait_for "Reconnected to the proxy" "$GLOG2" 90; then note "OK: server2 auto-reconnected"; else note "FAIL: server2 did not reconnect"; fail=1; fi
# The two servers' start-broadcasts cross while each is finishing auth — give them a moment, then assert
# neither game server hit the plaintext-broadcast-in-encrypting-mode ClassCastException.
sleep 5
if grep -qE "cannot be cast|ClassCastException" "$GLOG1" "$GLOG2"; then
  note "FAIL: encryption desync on simultaneous reconnect (plaintext broadcast read as encrypted)"
  grep -nE "cannot be cast|ClassCastException" "$GLOG1" "$GLOG2" | sed -E 's/\x1b\[[0-9;]*m//g' | head -5
  fail=1
else note "OK: no encryption desync across the simultaneous reconnect"; fi

# 5. clean shutdown WHILE connected (the callEvent-during-onDisable fix), both servers
note "==> Graceful game-server shutdown while connected"
echo stop >&8 2>/dev/null || true; exec 8>&- 2>/dev/null || true
echo stop >&9 2>/dev/null || true; exec 9>&- 2>/dev/null || true
wait "$GAME_JOB1" 2>/dev/null || true
wait "$GAME_JOB2" 2>/dev/null || true
if grep -qE "IllegalPluginAccessException|register task while disabled" "$GLOG1" "$GLOG2"; then note "FAIL: scheduling during onDisable threw"; fail=1; fi
grep -q "Disabling BungeeSK" "$GLOG1" || { note "FAIL: server1 did not shut down cleanly"; fail=1; }
grep -q "Disabling BungeeSK" "$GLOG2" || { note "FAIL: server2 did not shut down cleanly"; fail=1; }
stop_proxy

for tag in "server1:$GLOG1" "server2:$GLOG2"; do
  echo "----- ${tag%%:*} log (BungeeSK lines) -----"
  grep -iE "Auto-connecting|Connected to the proxy|IT-ROUNDTRIP|Lost the connection|Reconnected|Disabling BungeeSK|IllegalPluginAccess|cannot be cast|ClassCastException" "${tag#*:}" | sed -E 's/\x1b\[[0-9;]*m//g' || true
done
echo "--------------------------------------------"

if [ "$fail" -eq 0 ]; then
  echo "INTEGRATION TEST PASSED: connect + round-trip + stable + simultaneous reconnect (no desync) + clean shutdown."
else
  echo "INTEGRATION TEST FAILED (see above)."
fi
exit "$fail"
