<h2 align="center">BungeeSK</h2>
<p align="center">A Skript addon to talk to your BungeeCord <em>or</em> Velocity proxy from your game servers.</p>

<p align="center">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.21.11%20(Paper)-brightgreen">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange">
  <img alt="Skript" src="https://img.shields.io/badge/Skript-2.16%2B-blue">
  <img alt="Version" src="https://img.shields.io/badge/BungeeSK-2.2.0-lightgrey">
</p>

---

## ✅ Compatibility

| Component        | Supported                                                                 |
|------------------|---------------------------------------------------------------------------|
| Game servers     | **Paper / Spigot 1.21.11** (built against `paper-api`). Forward-compatible with the 26.x line as long as Skript itself supports it. |
| Skript           | **2.16.0+** (required for 1.21.x)                                          |
| Java             | **21** (required by Minecraft 1.21+)                                       |
| Proxies          | **BungeeCord / Waterfall** and **Velocity 3.4+**                          |

> **26.1+ note:** BungeeSK uses only stable Bukkit + Skript API (no version-specific NMS), so the same `BungeeSK.jar` is expected to keep working on newer game versions once your Paper build and Skript support them. Run the newest Skript for the newest Minecraft.

## 📥 Downloads

One jar does it all:

| Jar            | Where it goes                                                                 |
|----------------|-------------------------------------------------------------------------------|
| `BungeeSK.jar` | Every **game server** and your **proxy** (BungeeCord **or** Velocity) — drop the same file into `plugins/` everywhere. |

Grab it from the [latest release](https://github.com/ZorgBtw/BungeeSK/releases/latest), or [build from source](#-building-from-source). Make sure [Skript](https://github.com/SkriptLang/Skript/releases/latest) is installed on every game server.

## 🚀 Getting connected

Pick **one** of the two ways to link a game server to the proxy.

### Option A — config.yml (recommended, new in 2.1.0)

On each game server, edit `plugins/BungeeSK/config.yml`:

```yaml
connection:
  auto-connect: true          # connect automatically on server start
  address: "127.0.0.1"        # proxy address (127.0.0.1 if same machine)
  port: 20000                 # must match the proxy's config.yml
  password: "YourStrongPassword"   # must match the proxy's config.yml
reconnect:
  enabled: true                    # auto-reconnect if the link drops
  delays-seconds: [5, 10, 20, 30]  # wait before each attempt; last value repeats. [30] = always 30s. [] = exponential
  initial-delay-seconds: 5         # exponential-backoff fallback (only used when delays-seconds is empty)
  max-delay-seconds: 60            # exponential-backoff fallback cap
  log-attempts: false              # true = log every retry; false = only "connection lost" / "reconnected"
```

No script needed — the server connects on start and **keeps itself reconnected**. The retry schedule is configurable: a per-attempt list (last value repeats, so `[30]` is a fixed 30s), or leave it empty for exponential backoff between `initial-delay-seconds` and `max-delay-seconds`.

### Option B — from a script

```applescript
on load:
    create new bungee connection:
        set address of connection to "127.0.0.1"
        set port of connection to 20000
        set password of connection to "YourStrongPassword"
    start new connection with connection
```

Unlike older versions, you **no longer need a manual `while` retry loop** — auto-reconnect is built in. If the proxy is offline when you connect, BungeeSK keeps retrying on its own.

## 🎛️ Commands

| Where | Command | Subcommands |
|-------|---------|-------------|
| Game server (Paper/Spigot) | `/bungeesk` | `status`, `reconnect`, `disconnect`, `reload`, `version` — controls **this server's** link to the proxy |
| Proxy (BungeeCord / Velocity) | `/bungeeskproxy` (alias `/bsproxy`) | `servers`, `disconnect <ip:port\|all>`, `start`, `stop`, `restart`, `reload` — manages the **proxy listener** |

Both require the `bungeesk.command` permission. The proxy command is `/bungeeskproxy` (not `/bungeesk`) on purpose: a proxy intercepts a `/bungeesk` it owns and would shadow the game-server `/bungeesk`, leaving that one reachable only as the ugly `/bungeesk:bungeesk`.

## 🆕 Changelog

Per-release changes are tracked in **[CHANGELOG.md](CHANGELOG.md)** — kept current there instead of duplicated here.

## 🎨 Colors & formatting

```applescript
# Hex — works on BungeeCord and Velocity:
send bungee message "&#ff5555Hello in red-ish!" to {_bungeeplayer}

# MiniMessage (hex, gradients, …) — works on both Velocity and BungeeCord:
send bungee message "<gradient:#ff0000:#0000ff>Gradient text</gradient>" to {_bungeeplayer}
broadcast "<rainbow>Welcome to the network!</rainbow>" to the network
```

## 🧩 Handy syntaxes (already available)

```applescript
broadcast "&aHello network!" to the network
broadcast bungee message "&eServer message" to bungee server named "lobby"
send %bungeeplayer% action bar message "&bWelcome!"
send bungeecord title "&6Title" with subtitle "&7Subtitle" to {_bungeeplayer}

set {_players::*} to all bungee players on bungee server named "lobby"
send {_bungeeplayer} to bungee server named "hub"
make all servers execute console command "say hi"
make %bungeeplayer% execute command "spawn"
send custom message "hello" to {_bungeeservers::*}
```

See the full documentation on [SkriptHub](https://skripthub.net/docs/?addon=BungeeSK) and [skUnity](https://docs.skunity.com/syntax/search/addon:bungeesk).

## 🔧 Building from source

Requires **JDK 21**. From the repo root:

```bash
./gradlew buildAll
```

Output — the single distributable jar:
- `build/libs/BungeeSK.jar` (runs on Paper/Spigot, BungeeCord **and** Velocity)

It is fused from two internal, per-platform jars (`BungeeSK/build/libs/BungeeSK-Paper-Bungee.jar` and `VelocitySK/build/libs/BungeeSK-Velocity.jar`) that are build intermediates, not something you install.

> **Windows + OneDrive:** if the project lives inside a OneDrive-synced folder, the sync client can lock `build/` and break Gradle. Redirect build output out of the synced folder:
> ```bash
> ./gradlew buildAll -PbuildDirBase=C:/Users/you/AppData/Local/bungeesk-build
> ```

## 📚 Support

- [**Discord server**](https://discord.gg/PCnyMDsTRA)
- [**Wiki**](https://bungeesk.zorgdev.fr)
- Docs: [SkriptHub](https://skripthub.net/docs/?addon=BungeeSK) · [skUnity](https://docs.skunity.com/syntax/search/addon:bungeesk)
