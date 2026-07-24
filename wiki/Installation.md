# Installation

## Requirements

- **Game servers:** Paper/Spigot **1.21.11** on **Java 21**, with [Skript](https://github.com/SkriptLang/Skript/releases/latest) **2.16+** installed.
- **Proxy:** **BungeeCord** (1.21) **or** **Velocity** (3.4+). The proxy does **not** need Skript.

## Option A — the universal jar (simplest)

`BungeeSK-Universal.jar` runs on **all three** platforms. Drop the **same file** into `plugins/` everywhere:

| Server | Put it in |
|--------|-----------|
| Each Paper/Spigot game server | `plugins/` |
| Your BungeeCord **or** Velocity proxy | `plugins/` |

That's it — one jar to keep in sync.

## Option B — per-platform jars

If you prefer separate jars:

| Jar | Goes on |
|-----|---------|
| `BungeeSK.jar` | Every game server **and** a BungeeCord proxy |
| `BungeeSK-Velocity.jar` | A Velocity proxy |

So a Velocity network uses `BungeeSK.jar` on the game servers and `BungeeSK-Velocity.jar` on the proxy; a BungeeCord network uses `BungeeSK.jar` on both.

## After installing

1. Start each server once so BungeeSK generates its `plugins/BungeeSK/config.yml`.
2. Set the **same port and password** in the proxy's config and every game server's config — see **[Configuration](Configuration)**.
3. Enable `connection.auto-connect` on the game servers (or connect from a script).

## Where to download

Grab the jars from the repository's **Releases** page (raw `.jar` files), or **[build from source](Building-from-Source)**.

> **Updating on Paper:** Paper caches remapped plugins in `plugins/.paper-remapped/`. After replacing the jar, delete that folder (or the matching entry) so the new jar is used, not a stale remapped copy.
