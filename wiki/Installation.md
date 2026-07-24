# Installation

## Requirements

- **Game servers:** Paper/Spigot **1.21.11** on **Java 21**, with [Skript](https://github.com/SkriptLang/Skript/releases/latest) **2.16+** installed.
- **Proxy:** **BungeeCord** (1.21) **or** **Velocity** (3.4+). The proxy does **not** need Skript.

## One jar, everywhere

There is a single download — **`BungeeSK.jar`** — and it runs on **all three** platforms. Drop the **same file** into `plugins/` on every server:

| Server | Put it in |
|--------|-----------|
| Each Paper/Spigot game server | `plugins/` |
| Your BungeeCord **or** Velocity proxy | `plugins/` |

That's it — nothing platform-specific to pick, one jar to keep in sync.

> Building from source also produces two internal per-platform jars (`BungeeSK-Paper-Bungee.jar`, `BungeeSK-Velocity.jar`); those are build intermediates that get fused into `BungeeSK.jar` and are not something you install.

## After installing

1. Start each server once so BungeeSK generates its `plugins/BungeeSK/config.yml`.
2. Set the **same port and password** in the proxy's config and every game server's config — see **[Configuration](Configuration)**.
3. Enable `connection.auto-connect` on the game servers (or connect from a script).

## Where to download

Grab the jars from the repository's **Releases** page (raw `.jar` files), or **[build from source](Building-from-Source)**.

> **Updating on Paper:** Paper caches remapped plugins in `plugins/.paper-remapped/`. After replacing the jar, delete that folder (or the matching entry) so the new jar is used, not a stale remapped copy.
