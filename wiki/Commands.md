# Commands

BungeeSK has **two** admin commands: one on the game server, one on the proxy. They have **different names on purpose** — see [why](#why-two-names) below.

Both require the `bungeesk.command` permission (default: operators).

## Game server — `/bungeesk`

Controls **this game server's** link to the proxy.

| Subcommand | What it does |
|------------|--------------|
| `/bungeesk status` | Show the connection state, proxy target and reconnect settings |
| `/bungeesk reconnect` | Reconnect to the proxy now |
| `/bungeesk disconnect` | Disconnect (disables auto-reconnect until you reconnect) |
| `/bungeesk reload` | Reload `config.yml` and re-apply the connection |
| `/bungeesk version` | Show the plugin version |
| `/bungeesk help` | List the subcommands |

## Proxy — `/bungeeskproxy` (alias `/bskproxy`)

Manages the **proxy's connection listener** and the servers connected to it.

| Subcommand | What it does |
|------------|--------------|
| `/bungeeskproxy servers` | List the game servers connected to BungeeSK (with ping) |
| `/bungeeskproxy disconnect <ip:port \| all>` | Disconnect a specific game server, or all of them |
| `/bungeeskproxy start` | Start the listener |
| `/bungeeskproxy stop` | Stop the listener |
| `/bungeeskproxy restart` | Restart the listener |
| `/bungeeskproxy reload` | Reload `config.yml` and restart the listener |
| `/bungeeskproxy help` | List the subcommands |

## Why two names?

On a proxy network the proxy **intercepts** any command it owns before it reaches the backend server. If both the proxy and the game server registered `/bungeesk`, the proxy would win and the game-server command would only be reachable through the ugly `/bungeesk:bungeesk`.

So the proxy command is **`/bungeeskproxy`** and the game-server command keeps the clean **`/bungeesk`**. There is no `servers` subcommand on the game-server side — only the proxy knows the full list of connected servers.
