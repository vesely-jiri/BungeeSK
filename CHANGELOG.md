# Changelog

## Unreleased

> **Requires Skript 2.16.0 or newer.** The syntax-registration migration below is built against the Skript 2.16 API (`SyntaxInfo.simple` / `SyntaxRegistry`); on older Skript (2.15.x and earlier) BungeeSK will **not** enable — it throws `NoSuchMethodError: SyntaxInfo.simple` on start and registers no syntax. Update Skript before updating BungeeSK.

### Breaking changes
- **Player-targeted effect syntax redesigned — old forms removed, update your scripts.** These effects now take **one player, a list, or a variable**. The ones that would otherwise be shadowed by a built-in Skript effect **require** a `bungee` **or** `proxy` keyword (interchangeable — the addon drives Velocity too, not only BungeeCord):
  - `send (bungee|proxy) message "…" to <players>`
  - `send (bungee|proxy) action bar "…" to <players>`
  - `send (bungee|proxy) title "…" [with subtitle …] [for …] to <players> [with fade-in …] [and fade-out …]`
  - `(play|send) (bungee|proxy) sound "…" [with volume …] [and pitch …] to <players>`
  - `show [(bungee|proxy)] boss bar "…" [with colour …] [with style …] [with progress …] [for …] to <players>`
  - `kick <players> from (bungee|proxy|the network) [due to "…"]`
  - `connect <players> to <server>` (also `send <players> to <server>`)
  - `make <players> execute command "…"` (runs on their server) and `make <players> execute (bungee|proxy) command "…"` (a proxy command)

  The `bungee`/`proxy` keyword is required precisely so a **variable** recipient — `send bungee action bar "…" to {_p}` — resolves to BungeeSK; without it Skript's own `send action bar` / `title` / `sound` effect matches the variable first and silently wins.
- **Proxy command alias renamed** `/bsproxy` → `/bskproxy` (the main command `/bungeeskproxy` is unchanged).

### Fixes
- **Fixed a `ClassCastException` (`BungeeServerStartPacket cannot be cast to [B`) when several servers reconnect at once.** During a reconnecting server's brief auth window, the proxy could broadcast a *plaintext* packet to a client that had already switched its side of the link to encrypted, so the client read the plaintext object as an encrypted `byte[]` and threw on its async thread (the packet was also lost). The proxy now broadcasts only to fully-authenticated sockets and does not run the keep-alive on a socket before it authenticates; the game server additionally drops — rather than crashes on — a stray plaintext packet while encrypting.
- **Clean disable on an unsupported Skript.** BungeeSK 2.3+ needs Skript 2.16's API; on an older Skript it now logs `BungeeSK requires Skript 2.16.0 or newer, but found <x>. Disabling BungeeSK — update Skript.` and disables cleanly, instead of throwing a raw `NoSuchMethodError` mid-enable and leaving every syntax unregistered.
- **No more spurious "Lost the connection to the proxy" reconnect churn.** The proxy disconnected a game server after a *single* missed keep-alive (a 1-second round-trip every 5s), so one GC pause or a momentary network/TPS hiccup — on either end — dropped the link and forced a reconnect, often on several servers at once. The proxy now only disconnects after several consecutive missed keep-alives.
- **Effects fired during a brief outage are no longer silently lost.** While the link is down, fire-and-forget effects from a game server (messages, titles, action bars, broadcasts, kicks, cross-server commands, sounds, boss bars, global-variable writes) are buffered and replayed once it reconnects. The buffer is bounded and has a short time-to-live, so a command that has been waiting too long is dropped instead of firing long after it made sense. Requests (which already fail fast when offline) and connection-control packets are never buffered.
- **Fixed a stacktrace when a connected game server shuts down.** On disable, the client-disconnect event was dispatched through the Bukkit scheduler, which rejects new tasks once the plugin is disabling (`IllegalPluginAccessException: Plugin attempted to register task while disabled`). The event is now fired directly on the main thread when already on it (the shutdown path), and only scheduled from the async reader while still enabled.
- **Fixed a proxy-side keep-alive task leak.** When a game server disconnected, the proxy's per-connection keep-alive task kept running for the life of the proxy (blocking a thread every few seconds and pinning the dead connection object), so leaks accumulated across server restarts. The task is now cancelled on disconnect. Disconnect is also idempotent now, so a link that drops while the keep-alive is mid-flight no longer fires the server-stop event twice.

### Internal
- Both proxies' request/response map is now a `ConcurrentHashMap` (it was a plain `HashMap` accessed from two threads) and no longer leaks a pending entry when a round-trip times out.
- `SO_KEEPALIVE` is enabled on the proxy and game-server sockets so an idle link isn't silently reaped by a NAT/conntrack table.
- Added unit tests for the offline packet buffer (FIFO drain, TTL expiry, overflow eviction).
- Migrated all Skript syntax registration off the deprecated (marked-for-removal) `Skript.registerEffect/registerExpression/registerCondition/registerSection/registerEvent`, `ExpressionType` and `PropertyExpression.register` APIs to the modern `SyntaxRegistry`, via a single `Syntax` helper. Also replaced the deprecated `Timespan.getTicks_i()`/`getTicks()` with `getAs(TimePeriod.TICK)`. The build is now warning-free. A few Skript 2.16 APIs have no addon-usable modern replacement yet — event-value registration (`EventValues.registerEventValue`; the modern `EventValueRegistry` is exposed read-only), addon bootstrap (`Skript.registerAddon` + `loadClasses`), and `PropertyExpression`'s pattern generator — so those keep the working deprecated call with a localized `@SuppressWarnings("removal")` and a note. No behaviour change.
- Added a Skript syntax test (`BungeeSK/src/test/skript/tests/syntaxes.sk` + `scripts/run-syntax-tests.sh` + a CI job): boots a throwaway Paper server with Skript and the built jar and loads a script exercising every effect/expression/condition/section/event, failing if any no longer registers or parses. Verified locally on Paper 1.21.11 + Skript 2.16.0 ("all scripts loaded without errors").
- Added an integration test (`scripts/integration-test.sh` + a CI job): boots a real Velocity proxy and **two** Paper game servers, all on the built jar and connected over BungeeSK's socket, and checks the connection-dependent behaviour a single-server boot can't reach — auto-connect, a live request/response round-trip (`network player count` resolving over the socket), keep-alive stability, **both** servers auto-reconnecting together after the proxy is dropped and restarted (asserting no encryption desync / `ClassCastException` when their start-broadcasts cross mid-auth), and a clean shutdown-while-connected. Verified on CI (Velocity 3.5.1 + Paper 1.21.11).
- CI test jobs now reuse the jar the build job produces (the actual shipped universal jar) instead of each rebuilding it, and cache the downloaded server jars.
- The game-server startup banner is tidied: the auto-connect and PlaceholderAPI lines are folded into the coloured enable summary instead of being printed as separate uncoloured lines around it, and the banner is printed before the connection opens so the async connection result lands just below it.
- The jar artifact is uploaded with `compression-level: 0` — it is already a compressed zip, so storing it skips a pointless re-deflate (the download is a zip container either way).

## 2.3.0

### Security
- **Generated connection password now uses `SecureRandom`** instead of `java.util.Random` / `Math.random()`, so a freshly generated password is cryptographically strong. Existing passwords in `config.yml` are unchanged (delete the `password` line and restart to regenerate).

### Fixes
- **`/bungeeskproxy servers` no longer lists port-scanner connections.** Internet scanners that reached an exposed socket showed up as bogus `IP:0` entries; the list now shows only authenticated, registered servers. The IP whitelist also correctly rejects a non-whitelisted connection instead of still registering it.

### Features
- **`affect_all_servers` proxy config** (default `true`). Set to `false` to make broadcasts and player effects (send, title, action bar, kick, send to server, make run command) only reach players whose current server is connected through BungeeSK.
- **`/bsk` alias** for the game-server `/bungeesk` command.
- **Tab-completion** for the proxy `/bungeeskproxy` command — subcommands, plus connected `IP:port`s for `disconnect`.

### Docs
- Each effect's description now notes whether it can reach players on servers without BungeeSK installed.
- New wiki page **Networking & Firewall** (Pterodactyl allocations, keeping the socket port private with a firewall / IP whitelist). Fixed the proxy `config.yml` example: proxy keys keep underscores (`whitelist_ip`, `files.sync_at_connect`), game-server keys use hyphens.

### Internal
- Added unit tests for the packet codec, UUID encoding and `Pair`.
- Enabled the Gradle build cache, parallel builds and the configuration cache.
- The release workflow now publishes the matching CHANGELOG section as the GitHub Release notes (instead of an auto-generated commit list).

## 2.2.0

### New Skript syntaxes
- **Player info:** `%bungeeplayer%'s bungee ping` (latency in ms) and `%bungeeplayer%'s protocol version` (client protocol number).
- **Player counts:** `network player count` (total online across the network) and `player count of %bungeeserver%` (one server).
- **Broadcast title / action bar:** `broadcast title "…" [with subtitle …] [for …] [with fade-in …] [and fade-out …] to the network | to %bungeeserver%` and `broadcast action bar "…" to the network | to %bungeeserver%`.
- **Cross-server sound & boss bar:** `play sound "<key>" [with volume N] [and pitch N] to %bungeeplayer%` and `show boss bar "<title>" [with colour …] [with style …] [with progress N] [for <timespan>] to %bungeeplayer%` (timed, auto-removes). These reach a player on any server — the proxy forwards them to the player's game server, which plays them locally.

### Integrations
- **PlaceholderAPI** (soft-depend): `%bungeesk_connected%`, `%bungeesk_state%`, `%bungeesk_network_players%` (the count comes from a small async cache so resolving a placeholder never blocks).
- **Redis backend** for global variables (opt-in via `redis.enabled` in the proxy config) — lets several proxies share global variables. SQLite remains the default; the Redis path is fail-safe.

### Fixes
- **Auto-reconnect no longer stalls.** A connection reset during the socket handshake used to dump a raw stack trace and then stop retrying (the failing `SocketClient` notified a not-yet-assigned client, so no further attempt was scheduled). The constructor now throws instead of swallowing the error, the read loop starts only after the client is published, and a handshake failure is logged cleanly and reschedules the next attempt.
- **`/bungeesk` no longer crashes on Velocity.** Help text such as `<IP:PORT / ALL>` was mis-detected as MiniMessage and its `§` codes made the strict parser throw. Tag detection now requires a real tag (no whitespace) and never treats `§`-coded strings as MiniMessage; Velocity's formatter also falls back to legacy on any parse error.

### Features
- **Configurable reconnect schedule.** `reconnect.delays-seconds` is a per-attempt list whose last value repeats (`[30]` = a fixed 30s, `[5, 10, 20, 30]` = 5s→10s→20s→30s…); leave it empty to keep the previous exponential backoff between `initial-delay-seconds` and `max-delay-seconds`. New `reconnect.log-attempts` toggles per-attempt logging — off by default, so you get one "connection lost" and one "reconnected" message instead of a repeating warning.
- **Admin commands.** New game-server `/bungeesk` command (`status`, `reconnect`, `disconnect`, `reload`, `version`) with tab-completion and the `bungeesk.command` permission. The proxy command is renamed to `/bungeeskproxy` (alias `/bsproxy`) so it no longer shadows the game-server `/bungeesk` on a proxied network. Both proxies also gained a `reload` subcommand (re-reads `config.yml` and restarts the listener).
- **Structured startup banner** on all three platforms, including how long enabling took.
- **One universal jar.** `BungeeSK.jar` now runs on Paper/Spigot, BungeeCord **and** Velocity — the same file goes into `plugins/` everywhere. It is fused from two internal per-platform jars; each platform loads only its own descriptor and main class. It is also the only published artifact, and it is ~60% smaller after trimming unused SQLite native libraries.

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
