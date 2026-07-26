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

# Skript — the same artifact the build compiles against (Maven layout on repo.skriptlang.org).
curl -fsSL -o "$WORKDIR/plugins/Skript.jar" \
  "https://repo.skriptlang.org/releases/com/github/SkriptLang/Skript/${SKRIPT_VERSION}/Skript-${SKRIPT_VERSION}.jar"

# Paper — latest build for the target Minecraft version, via the v3 "fill" API (v2 is gone / HTTP 410).
# The endpoint returns builds newest-first; [0] is the latest and carries a direct download URL.
PAPER_UA="BungeeSK-syntax-tests (+https://github.com/vesely-jiri/BungeeSK)"
PAPER_URL="$(curl -fsSL -H "User-Agent: $PAPER_UA" \
  "https://fill.papermc.io/v3/projects/paper/versions/${MC_VERSION}/builds" \
  | jq -r '.[0].downloads."server:default".url')"
[ -n "$PAPER_URL" ] && [ "$PAPER_URL" != "null" ] || { echo "Could not resolve a Paper build for $MC_VERSION"; exit 2; }
curl -fsSL -H "User-Agent: $PAPER_UA" -o "$WORKDIR/paper.jar" "$PAPER_URL"

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
