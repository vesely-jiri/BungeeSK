# Troubleshooting

## BungeeSK won't enable / every BungeeSK syntax says "Can't understand"

Symptom — on startup the server logs:

```
Error occurred while enabling BungeeSK vX (Is it up to date?)
java.lang.NoSuchMethodError: '… SyntaxInfo.simple(…)'
```

and afterwards **every** BungeeSK line in your scripts reports *Can't understand this structure* / *Can't understand this expression*.

**Cause:** your **Skript is older than 2.16.0**. BungeeSK 2.3.x is built against the Skript 2.16 API (`SyntaxInfo.simple`, the modern `SyntaxRegistry`), which does not exist in 2.15.x and earlier, so BungeeSK crashes during enable and registers no syntax at all — which is why *all* of its syntaxes then fail to parse, not just some.

**Fix:** update [Skript](https://github.com/SkriptLang/Skript/releases/latest) to **2.16.0 or newer** and restart. Check the running version with `/skript info` (or `/sk info`).

## A player effect (action bar / title / message / sound) stopped working after 2.3.1

2.3.1 redesigned the player-targeted effects (old forms removed). If one no longer parses (*Can't understand*), or `send action bar "…" to {_player}` silently does nothing, add the `bungee` (or `proxy`) keyword:

```applescript
send bungee action bar "&aHi" to {_player}      # not: send action bar "&aHi" to {_player}
send proxy title "&6Hi" to all bungee players
```

The keyword is **required** on the effects that share a name with a built-in Skript effect (`send message` / `send action bar` / `send title` / `play sound`). Without it, a **variable** recipient is grabbed by Skript's own effect instead of BungeeSK's — which is why the plain form can parse yet do nothing. `bungee` and `proxy` are interchangeable. Also: `connect <players> to <server>`, `kick <players> from the network`, `make <players> execute proxy command "…"`. See **[Skript Syntax](Skript-Syntax)** for the full list.

## The game server never connects

- **Port / password mismatch.** The `port` and `password` must be **identical** on the proxy and every game server. A wrong password fails the handshake.
- **Proxy not listening.** On the proxy, run `/bungeeskproxy servers` — if it says *“currently stopped”*, run `/bungeeskproxy start`.
- **Firewall / networking.** The proxy's BungeeSK `port` (default `20000`) must be reachable from the game servers. This is a **separate** port from the Minecraft/proxy port. On Pterodactyl it also needs to be an **allocation** on the proxy, and game servers connect via the proxy's internal IP (not `127.0.0.1`) — see **[Networking & Firewall](Networking-and-Firewall)**.
- **Auto-connect off.** On the game server, check `connection.auto-connect: true`, or connect from a script.

You can watch the state on the game server with `/bungeesk status`.

## It keeps retrying / “Could not reach the proxy”

That's auto-reconnect doing its job — the proxy is unreachable. With `reconnect.log-attempts: false` (default) you'll see a single *“Could not reach the proxy … will keep retrying”* message; set it to `true` to log every attempt. Once the proxy is back you'll get *“Reconnected to the proxy.”* Tune the cadence with `reconnect.delays-seconds` — see **[Configuration](Configuration)**.

## `/bungeesk` shows red / only `/bungeesk:bungeesk` works

You're on an **older build**. The proxy command is now `/bungeeskproxy` so it no longer shadows the game-server `/bungeesk`. Update the jar everywhere. See **[Commands](Commands)**.

## A colour/tag isn't rendering

- MiniMessage tags must have **no spaces** in the tag itself (`<gradient:#a:#b>`, not `<grad ient>`).
- Don't mix `§`/`&` legacy codes into a MiniMessage string — the whole string then falls back to legacy. See **[Colors & Formatting](Colors-and-Formatting)**.

## Paper runs an old version after I replaced the jar

Paper caches remapped plugins in `plugins/.paper-remapped/`. Delete that folder after swapping the jar so Paper re-remaps the new one instead of running a stale copy.

## A global variable reads empty

- Use the **string-named** syntax: `global variable "name"`, not a `{braces}` variable (which is a normal, per-server Skript variable).
- The game server must be **connected** to the proxy when you read (reads are a live round-trip). Check `/bungeesk status`.

See **[Global Variables & Scripts](Global-Variables-and-Scripts)**.

## SQLite “unsupported platform” on an exotic OS/arch

The jars ship native SQLite libraries only for common server platforms (Linux glibc + Alpine/musl, Windows, Apple-Silicon Mac — all x86_64/aarch64). If you run something else, [build from source](Building-from-Source) after adding your platform back in `ext.prunedJarPaths` in the root `build.gradle`.
