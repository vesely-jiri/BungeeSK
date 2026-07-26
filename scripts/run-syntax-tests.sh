#!/usr/bin/env bash
#
# Skript syntax registration / parse test.
#
# Boots a throwaway headless Paper server with Skript + the built BungeeSK jar, loads
# BungeeSK/src/test/skript/tests/syntaxes.sk, and fails if Skript cannot parse any BungeeSK syntax.
# This is what guards the SyntaxRegistry migration end-to-end: it catches a syntax that no longer
# registers or whose pattern stopped parsing — things a compile check cannot see.
#
# Usage: scripts/run-syntax-tests.sh [path-to-built-BungeeSK-jar]
#   Defaults to BungeeSK/build/libs/BungeeSK-Paper-Bungee.jar (the :BungeeSK:shadowJar output).
#
# Env overrides: MC_VERSION, SKRIPT_VERSION, BOOT_TIMEOUT.

set -euo pipefail

MC_VERSION="${MC_VERSION:-1.21.11}"
SKRIPT_VERSION="${SKRIPT_VERSION:-2.16.0}"
BOOT_TIMEOUT="${BOOT_TIMEOUT:-150}"

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEST_SCRIPT="$REPO_ROOT/BungeeSK/src/test/skript/tests/syntaxes.sk"
BUNGEESK_JAR="${1:-$REPO_ROOT/BungeeSK/build/libs/BungeeSK-Paper-Bungee.jar}"

[ -f "$BUNGEESK_JAR" ] || { echo "Built BungeeSK jar not found: $BUNGEESK_JAR (run :BungeeSK:shadowJar first)"; exit 2; }
[ -f "$TEST_SCRIPT" ] || { echo "Test script not found: $TEST_SCRIPT"; exit 2; }

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT
mkdir -p "$WORKDIR/plugins/Skript/scripts"

echo "==> Staging server in $WORKDIR (MC $MC_VERSION, Skript $SKRIPT_VERSION)"
cp "$BUNGEESK_JAR" "$WORKDIR/plugins/BungeeSK.jar"
cp "$TEST_SCRIPT" "$WORKDIR/plugins/Skript/scripts/syntaxes.sk"

# Cache the (large) server jars between runs, sharing the same cache as the integration test (same
# Paper/Skript versions). Paper is resolved from the v3 "fill" API (v2 is gone / HTTP 410); the builds
# array is newest-first and each server jar URL embeds "paper-<version>", so no jq is needed.
CACHE="${INTEGRATION_CACHE:-$HOME/.cache/bungeesk-integration}"
mkdir -p "$CACHE"
UA="BungeeSK-syntax-tests"
PAP="$CACHE/paper-$MC_VERSION.jar"
SKR="$CACHE/skript-$SKRIPT_VERSION.jar"
[ -f "$SKR" ] || curl -fsSL -o "$SKR" \
  "https://repo.skriptlang.org/releases/com/github/SkriptLang/Skript/${SKRIPT_VERSION}/Skript-${SKRIPT_VERSION}.jar"
if [ ! -f "$PAP" ]; then
  purl="$(curl -fsSL -H "User-Agent: $UA" "https://fill.papermc.io/v3/projects/paper/versions/${MC_VERSION}/builds" \
        | grep -oE "https://[^\"]*paper-$MC_VERSION[^\"]*\.jar" | head -1)"
  [ -n "$purl" ] || { echo "Could not resolve a Paper build for $MC_VERSION"; exit 2; }
  curl -fsSL -H "User-Agent: $UA" -o "$PAP" "$purl"
fi
cp "$SKR" "$WORKDIR/plugins/Skript.jar"
cp "$PAP" "$WORKDIR/paper.jar"

echo "eula=true" > "$WORKDIR/eula.txt"
printf 'online-mode=false\nlevel-type=flat\nmax-players=1\nspawn-protection=0\n' > "$WORKDIR/server.properties"

echo "==> Booting server (timeout ${BOOT_TIMEOUT}s)"
LOG="$WORKDIR/server.log"
( cd "$WORKDIR" && timeout "$BOOT_TIMEOUT" java -Xmx1500M -jar paper.jar nogui > "$LOG" 2>&1 ) || true

echo "==> Skript load result:"
grep -E "Can't understand|can only be set|syntaxes\.sk|scripts loaded (with|without)" "$LOG" || true
echo "----------------------------------------"

if grep -q "Can't understand" "$LOG"; then
  echo "SYNTAX TEST FAILED: Skript could not parse a BungeeSK syntax (see above)."
  exit 1
fi
if ! grep -q "All scripts loaded without errors" "$LOG"; then
  echo "SYNTAX TEST INCONCLUSIVE: Skript's success line was not found. Server log tail:"
  tail -n 50 "$LOG"
  exit 1
fi
echo "SYNTAX TEST PASSED: every BungeeSK syntax registered and parsed."
