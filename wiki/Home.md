# BungeeSK

**BungeeSK** is a [Skript](https://github.com/SkriptLang/Skript) addon that lets your game servers talk to your **BungeeCord** or **Velocity** proxy: move players between servers, run commands across the network, share variables and scripts, react to network events, and send richly-formatted messages — all from Skript.

This is the modernized **2.1.0+** line: Minecraft **1.21.11** / Paper, **Java 21**, Skript **2.16**, with auto-reconnect, a game-server config file, admin commands, hex + MiniMessage colours, and a single **universal jar** that runs on all three platforms.

## Quick start

1. **[Install](Installation)** the jar on your game servers and your proxy.
2. Put your connection details in **[config.yml](Configuration)** (or connect from a script).
3. Use the **[Skript syntaxes](Skript-Syntax)** in your scripts.

```applescript
on bungee player join:
    broadcast "&#55ff55%event-bungeeplayer% joined the network" to the network

command /hub:
    trigger:
        send bungee player named player's name to bungee server named "hub"
```

## Contents

- **[Installation](Installation)** — which jar goes where, dependencies, the universal jar
- **[Configuration](Configuration)** — game-server & proxy `config.yml`, auto-connect, reconnect schedule
- **[Commands](Commands)** — `/bungeesk` (game server) and `/bungeeskproxy` (proxy)
- **[Skript Syntax](Skript-Syntax)** — every event, expression, effect, condition and type
- **[Colors & Formatting](Colors-and-Formatting)** — hex and MiniMessage
- **[Global Variables & Scripts](Global-Variables-and-Scripts)** — share state and scripts across the network
- **[Building from Source](Building-from-Source)**
- **[Troubleshooting](Troubleshooting)**

## Supported platforms

| Component | Runs on |
|-----------|---------|
| Game servers | Paper / Spigot **1.21.11** (Java 21) with Skript **2.16+** |
| Proxy | **BungeeCord** (1.21) **or** Velocity (3.4+) |

> One download does it all: a single `BungeeSK.jar` runs on Paper/Spigot, BungeeCord **and** Velocity. See [Installation](Installation).
