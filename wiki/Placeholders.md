# PlaceholderAPI

If [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) is installed on a game server, BungeeSK registers an expansion (identifier `bungeesk`) exposing:

| Placeholder | Value |
|-------------|-------|
| `%bungeesk_connected%` | `true` / `false` — whether this server is linked to the proxy |
| `%bungeesk_state%` | `connected` / `connecting` / `reconnecting` / `disconnected` |
| `%bungeesk_network_players%` | total players online across the whole network |

The network player count is served from a small cache that BungeeSK refreshes asynchronously, so resolving the placeholder never does a blocking proxy round-trip.

PlaceholderAPI is an **optional** soft-dependency — nothing changes if it isn't installed, and BungeeSK doesn't bundle it.

> For a specific server's player count, use the `player count of %bungeeserver%` Skript expression (see **[Skript Syntax](Skript-Syntax)**) rather than a placeholder.
