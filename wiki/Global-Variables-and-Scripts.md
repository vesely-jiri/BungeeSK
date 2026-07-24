# Global Variables & Scripts

## Global variables

Global variables live **on the proxy** (in a small SQLite database) and are shared by every connected game server. They are a separate system from Skript's own `{variables}` — you read and write them through a dedicated expression, using a **string name**:

```applescript
# Set on server A:
set global variable "rank.%player%" to "Admin"
set global variable "money.%uuid of player%" to 1000
set global variable "event.active" to true

# Read on server B:
set {_rank} to global variable "rank.%player%"
send "Your rank is %global variable ""rank.%player%""%"

# Delete:
delete global variable "event.active"
```

- The name is a **quoted string**, not a `{braces}` variable.
- Values may be any serializable Skript type (text, number, boolean, …).
- Reads are a live request to the proxy, so the game server must be **connected**; if the round-trip times out the value is empty. The timeout is generous (5s) to allow for a remote proxy.

> These are stored on the proxy — if a game server is offline, its `set` simply doesn't happen until it reconnects; reads always reflect the proxy's current value.

## Global scripts

Global scripts live in `plugins/BungeeSK/scripts/` **on the proxy** and are synchronised out to game servers, so you can manage shared logic in one place.

Behaviour is controlled by the proxy `config.yml` (`files:` section — see **[Configuration](Configuration)**):

- `sync-at-connect: true` — a game server receives the global scripts when it connects.
- `auto-update: true` — editing a global script re-pushes it to every server.
- `auto-delete: true` — deleting a global script removes it network-wide.

You can also fetch them from Skript on demand — see the “retrieve scripts” effect in the **[Skript Syntax](Skript-Syntax)** reference.
