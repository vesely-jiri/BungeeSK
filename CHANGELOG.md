# Changelog

## Unreleased

### Fixes
- **Auto-reconnect no longer stalls.** A connection reset during the socket handshake used to dump a raw stack trace and then stop retrying (the failing `SocketClient` notified a not-yet-assigned client, so no further attempt was scheduled). The constructor now throws instead of swallowing the error, the read loop starts only after the client is published, and a handshake failure is logged cleanly and reschedules the next attempt.
- **`/bungeesk` no longer crashes on Velocity.** Help text such as `<IP:PORT / ALL>` was mis-detected as MiniMessage and its `§` codes made the strict parser throw. Tag detection now requires a real tag (no whitespace) and never treats `§`-coded strings as MiniMessage; Velocity's formatter also falls back to legacy on any parse error.

### Features
- **Configurable reconnect schedule.** `reconnect.delays-seconds` is a per-attempt list whose last value repeats (`[30]` = a fixed 30s, `[5, 10, 20, 30]` = 5s→10s→20s→30s…); leave it empty to keep the previous exponential backoff between `initial-delay-seconds` and `max-delay-seconds`. New `reconnect.log-attempts` toggles per-attempt logging — off by default, so you get one "connection lost" and one "reconnected" message instead of a repeating warning.
- **Admin commands.** New game-server `/bungeesk` command (`status`, `reconnect`, `disconnect`, `reload`, `version`) with tab-completion and the `bungeesk.command` permission. The proxy command is renamed to `/bungeeskproxy` (alias `/bsproxy`) so it no longer shadows the game-server `/bungeesk` on a proxied network. Both proxies also gained a `reload` subcommand (re-reads `config.yml` and restarts the listener).
- **Structured startup banner** on all three platforms, including how long enabling took.
- **Universal jar.** `gradlew buildAll` now also produces `BungeeSK-Universal.jar` — a single jar that runs on Paper/Spigot, BungeeCord and Velocity (it fuses the two per-platform jars; each platform loads only its own descriptor and main class).

### Internal
- Global-variable request/response hardened: a `ConcurrentHashMap` for pending requests, a 5s (was 1s) round-trip timeout so a remote proxy is not cut off, and `PreparedStatement`s instead of string-concatenated SQL.

## 2.1.0

### Platform / build
- **Minecraft 1.21.11 (Paper) support.** Game-server module now compiles against `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT` (was `spigot-api:1.20`).
- **Java 21** toolchain (was Java 11) — required by Minecraft 1.21+.
- **Skript 2.16.0** (was 2.9.5). Migrated the removed/changed APIs:
  - `EventValues.registerEventValue(..., Getter, int)` → `registerEventValue(..., Converter)` (method references) across all events.
  - `ScriptLoader.isCurrentEvent(...)` → `ParserInstance.get().isCurrentEvent(...)`.
- **BungeeCord** `net.md-5:bungeecord-api:1.21-R0.4`; **Velocity** `velocity-api:3.4.0-SNAPSHOT`.
- Build tooling: **Gradle 9.6.1**, shadow plugin migrated `com.github.johnrengelman.shadow` (dead) → **`com.gradleup.shadow:9.6.1`**. Dependency bumps (sqlite-jdbc, annotations, bStats).
- Removed a phantom `BungeeSK-Universal` Gradle module reference and clarified jar outputs: `BungeeSK.jar` (game servers + BungeeCord) and `BungeeSK-Velocity.jar` (Velocity).
- Deleted dead code (`bukkit/utils/EffectSection`, `bukkit/utils/ReflectionUtils`) — the latter crashed on modern "unversioned CraftBukkit" Paper via `getPackage().getName().split(".v")[1]`. Removing it is what makes 1.21.11 / 26.x work.

### Security
- **Java deserialization hardening.** All socket reads (bukkit `SocketClient`, bungee & velocity `SocketServer`, and `PacketUtils`) now use an `ObjectInputFilter` whitelist (`fr.zorg.bungeesk.*` + JDK value types only, with depth/ref/byte caps). The receiving read happens **before** the password check, so this closes a pre-authentication deserialization RCE vector.

### Features
- **Auto-reconnect** with exponential backoff (`initial-delay-seconds` → `max-delay-seconds`). Wrong password disables the loop and logs a clear message.
- **Game-server `config.yml`** with **auto-connect** (address/port/password) — no script required.
- **Connection status & control** for Skript:
  - `bungee connection state` expression (`connected`/`connecting`/`reconnecting`/`disconnected`).
  - `reconnect to the proxy` effect.
  - `disconnect the client` now also disables auto-reconnect.
- **Hex + MiniMessage colors** in messages/titles/action bars/broadcasts, on **both** proxies. Hex (`&#rrggbb`) everywhere; MiniMessage via native Adventure on Velocity and a shaded+relocated Adventure on BungeeCord (`net.kyori` → `fr.zorg.shaded.kyori`, so it can't clash with Paper's own Adventure).

### Fixes / performance
- `AutoUpdater` now sends a `User-Agent` (GitHub returned 403 without one, so the check silently always failed) and is fully fail-safe (never blocks or spams enable).
- Replaced per-packet `new Thread(...)` with a shared cached thread pool; async work is cleaned up on disable.
- Serialized `ObjectOutputStream` writes (fixes a latent stream-corruption race when multiple packets were sent concurrently); `disconnect()` is now idempotent (no duplicate disconnect events).
- Replaced deprecated `new URL(String)` with `URI.create(...).toURL()` in `AutoUpdater` and both `PingUtils`.
