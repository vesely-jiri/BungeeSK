# Colors & Formatting

BungeeSK renders both **legacy/hex** colours and **MiniMessage** in the messages, titles, action bars and broadcasts it sends across the network.

## Legacy & hex — works on both proxies

Standard `&` colour codes plus hex, in both the short and long forms:

```applescript
send bungee message "&aGreen &and &lbold" to {_bungeeplayer}
send bungee message "&#ff5555Hello in a custom red!" to {_bungeeplayer}
broadcast "&#55ff55Welcome to the network" to the network
```

- `&#rrggbb` — short hex (e.g. `&#ff5555`)
- `&x&r&r&g&g&b&b` — the long/“unusual” hex form is also accepted

## MiniMessage — works on both proxies

[MiniMessage](https://docs.papermc.io/adventure/minimessage/format) tags (gradients, hex, styles, …) render on **both** Velocity and BungeeCord:

```applescript
broadcast "<gradient:#00ffcc:#0066ff>Welcome!</gradient>" to the network
send bungee message "<#ff0000><bold>Alert</bold></#ff0000> something happened" to {_bungeeplayer}
broadcast "<rainbow>Party time</rainbow>" to the network
```

On **Velocity**, MiniMessage is provided natively by the platform. On **BungeeCord**, BungeeSK bundles a shaded, relocated copy of Adventure (`net.kyori` → `fr.zorg.shaded.kyori`) so it can't clash with the game server's own Adventure.

## How a string is interpreted

For each message BungeeSK decides between MiniMessage and legacy:

1. If the string contains a **real MiniMessage tag** — a `<…>` whose name starts with a letter or `#` and has **no spaces** (e.g. `<red>`, `<#ff0000>`, `<gradient:#a:#b>`) — it is parsed as MiniMessage.
2. Otherwise (including any string that already contains legacy `§`/`&` codes) it is parsed as **legacy** with hex support.

This is why plain text like `<IP:PORT / ALL>` is **not** mistaken for MiniMessage — it contains a space, so it is treated as literal text.

> Mixing a leftover `§` code into a MiniMessage string forces the whole string down the legacy path (MiniMessage's strict parser rejects `§`). Stick to one style per message.
