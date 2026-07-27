# Skript Syntax

Every syntax BungeeSK registers, grouped by kind. These run in **Skript scripts on the game servers** (`plugins/Skript/scripts/`). Patterns use Skript notation: `[optional]`, `(choice|choice)`, `%type%`.

Jump to: [Events](#events) · [Expressions](#expressions) · [Effects](#effects) · [Conditions](#conditions) · [Sections](#sections) · [Types](#types)

---

## Events

### Proxy ping — `on (proxy|bungee) ping:`
Fired when a player pings the proxy. First announce that this server wants to handle it with the `listen to bungee proxy ping` effect (e.g. `on load:`). Inside, read/set the ping expressions (motd, max/connected players, hover list, favicon, protocol version/message, pinger's IP).

### Client connect — `on [bungee] client connect:`
This server's client linked to the proxy.

### Client disconnect — `on [bungee] client disconnect:`
This server's client disconnected from the proxy.

### Bungee command — `on bungee command:`
A command was run anywhere on the network.
Event values: `event-bungeeplayer` (executor), `event-string` (the command).

### Message receive — `on bungee [custom] message receive:`
A custom message arrived from another server.
Event values: `event-bungeeserver` (sender), `event-string` (the message).

### Player join — `on bungee [player] join:`
A player joined the network.
Event values: `event-bungeeplayer`.

### Player leave — `on bungee [player] (leave|quit):`
A player left the network.
Event values: `event-bungeeplayer`, `past-server` (the server they were on).

### Server start — `on bungee server (start|connect):`
A server connected to BungeeSK.
Event values: `event-bungeeserver`.

### Server stop — `on bungee server (stop|disconnect):`
A server disconnected from BungeeSK.
Event values: `event-bungeeserver`.

### Custom request — `on [bungee] custom request:`
Another server sent a custom request; reply with `set custom request response to ...`.
Event values: `event-string` (request name), `event-bungeeserver` (requesting server).

### Global script receive — `on (bungee|global) script receive:`
A global script was received from the proxy.
Event values: `event-string` (script name).

### Server switch — `on [bungee] server switch:`
A player moved from one server to another.
Event values: `event-bungeeplayer`, `event-bungeeserver` (destination), `past-server` (origin). **No `event-string`.**

---

## Expressions

### Connection state — `[the] [bungee[cord]|proxy] connection (state|status)`
This server's link state: `"connected"`, `"connecting"`, `"reconnecting"`, `"disconnected"`.

### All bungee players — `[(all [[of] the]|the)] bungee players`
Every player on the network.

### Players on a server — `[(all [[of] the]|the)] bungee players on %bungeeserver%`
Every player on the given server.

### Player by name — `bungee[ ]player (with name|named) %string%`
### Player by UUID — `bungee[ ]player with uuid %string%`

### Player's name — `[the] [user]name of %bungeeplayer%` · `%bungeeplayer%'s [user]name`
### Player's UUID — `[the] bungee uuid of %bungeeplayer%` · `%bungeeplayer%'s bungee uuid`
### Player's IP — `[the] ip [address] of %bungeeplayer%` · `%bungeeplayer%'s ip [address]`
### Player's server — `[the] server of %bungeeplayer%` · `%bungeeplayer%'s server`
Available ~2 ticks after join.
### Player's ping — `[the] bungee ping of %bungeeplayer%` · `%bungeeplayer%'s bungee ping`
Latency to the proxy, in milliseconds. *(2.2.0)*
### Player's protocol version — `[the] protocol [version] of %bungeeplayer%` · `%bungeeplayer%'s protocol [version]`
Client protocol number (e.g. 767 for 1.21). *(2.2.0)*

### All bungee servers — `[(all [[of] the]|the)] [bungee] servers`
### Server by name — `bungee[ ]server (with name|named) %string%`
### Server by address & port — `bungee[ ]server with address %string% and port %integer%`
### This server — `this bungee[ ]server`

### Server's name — `[the] bungee name of %bungeeserver%` · `%bungeeserver%'s bungee name`
### Server's address — `[the] bungee address of %bungeeserver%` · `%bungeeserver%'s bungee address`
### Server's port — `[the] bungee port of %bungeeserver%` · `%bungeeserver%'s bungee port`
### Server's MOTD — `[the] bungee motd of %bungeeserver%` · `%bungeeserver%'s bungee motd`

### Network player count — `[the] (network|total) [online] [bungee] player[s] count`
Total players online across the network. *(2.2.0)*
### Server player count — `[the] [bungee] player[s] count (of|on) %bungeeserver%` · `%bungeeserver%'s [bungee] player[s] count`
Players connected to a specific server. *(2.2.0)*

### Global variable — `global var[iable] [named] %string%` · *(settable)*
Get/set/delete a variable stored globally on the proxy; value may be any serializable type. See **[Global Variables & Scripts](Global-Variables-and-Scripts)**.

### Custom request — `custom request [named] %string% from [server] %bungeeserver%`
Sends a request to another server and returns its response.

### Custom request response — `custom request response` · *(settable, custom request event only)*

### Past server — `past-server`
The server a player came from; usable in the **server switch** and **player leave** events.

**Proxy-ping expressions** (only inside `on proxy ping:`, all settable except the last):
- MOTD — `ping motd`
- Connected players — `connected players [size]`
- Max players — `max players [size]`
- Hover list — `hover list`
- Favicon URL — `favicon [url]`
- Protocol version — `protocol version [number]`
- Protocol message — `protocol message`
- Pinger's IP — `bungee ip of pinger` · `pinger's bungee ip [address]`

**Connection builder** (inside `create new bungee connection`, all settable):
- Last built — `[the] [last] [(generated|created)] (connection|server)`
- Address — `[the] (ip|address) of %bungeeconn%` · `%bungeeconn%'s (ip|address)`
- Port — `[the] port of %bungeeconn%` · `%bungeeconn%'s port`
- Password — `[the] password of %bungeeconn%` · `%bungeeconn%'s password`

**Server builder** (inside `create new bungee server`, all settable):
- Last built — `[the] [last] [(generated|created)] [bungee] server [builder]`
- Name — `[the] name of %serverbuilder%` · `%serverbuilder%'s name`
- Address — `[the] (ip|address) of %serverbuilder%` · `%serverbuilder%'s (ip|address)`
- Port — `[the] port of %serverbuilder%` · `%serverbuilder%'s port`
- MOTD — `[the] motd of %serverbuilder%` · `%serverbuilder%'s motd`

---

## Effects

### Start connection — `start new connection with %bungeeconn%`
### Reconnect — `reconnect [to] [the] [bungee[cord]|proxy]`
Immediate reconnect; also re-enables auto-reconnect if a previous disconnect turned it off.
### Disconnect — `disconnect (the|this) client`
Disconnects and disables auto-reconnect.

> Player-targeted effects take **one player, a list, or a variable** (`%bungeeplayers%`). The ones that would otherwise be shadowed by a built-in Skript effect **require** a `bungee`/`proxy` keyword (interchangeable) — that keyword is what lets a *variable* recipient resolve to BungeeSK. *(2.3.1)*

### Send message — `send (bungee|proxy) message %string% to %bungeeplayers%`
### Send action bar — `send (bungee|proxy) action bar %string% to %bungeeplayers%`
### Send title — `send (bungee|proxy) title %string% [with subtitle %-string%] to %bungeeplayers% [for %-timespan%] [with fade[(-| )]in %-timespan%] [(and|with) fade[(-| )]out %-timespan%]`
Argument order matches Skript's own `send title`.
### Play sound *(2.2.0)* — `(play|send) (bungee|proxy) sound %string% [(at|with) volume %-number%] [(and|with) pitch %-number%] to %bungeeplayers%`
Plays a sound (namespaced key, e.g. `entity.experience_orb.pickup`) to a player on any server.
### Show boss bar *(2.2.0)* — `show [(bungee|proxy)] boss[ ]bar [title] %string% [with colo[u]r %-string%] [with style %-string%] [with progress %-number%] [for %-timespan%] to %bungeeplayers%`
Shows a timed boss bar (auto-removes) to a player on any server. Colour: pink/blue/red/green/yellow/purple/white; style: solid/segmented_6/…

### Send player to server — `(send|connect) %bungeeplayers% to %bungeeserver%`
### Kick player — `kick %bungeeplayers% from (bungee[cord]|proxy|[the] network) [(due to|because of) %-string%]`
### Player runs command — `make %bungeeplayers% execute command %string%` (runs on their current server)
### Player runs proxy command — `make %bungeeplayers% execute (bungee|proxy) command %string%`

### Console command — one of:
- `make bungee[cord] [server] execute console command %string%` (the proxy)
- `make %bungeeserver% execute console command %string%` (one server)
- `make all servers execute console command %string%` (every server)

### Broadcast to network — `broadcast %string% to [the] network`
### Broadcast to server — `broadcast bungee message %string% to %bungeeserver%`
### Broadcast title *(2.2.0)* — `broadcast [bungee[cord]] title %string% [with subtitle %-string%] [for %-timespan%] [with fade-in %-timespan%] [(and|with) fade-out %-timespan%] to [the] network` (or `… to %bungeeserver%`)
### Broadcast action bar *(2.2.0)* — `broadcast [bungee[cord]] action bar [message] %string% to [the] network` (or `… to %bungeeserver%`)
### Custom message — `send custom message %string% to %bungeeservers%`
Received via the message-receive event.
### To proxy console — `send %string% to bungee console`

### Retrieve scripts — `retrieve all (scripts|skripts) from bungee`
### Listen to ping — `listen to bungee proxy ping`

### Add dynamic server — `put [dynamic server] %serverbuilder% into bungeecord`
### Delete dynamic server — `delete server named %string% from [the] bungeecord`
### Stop a server — `stop %bungeeserver%`

---

## Conditions

### Client connected — `client is [(n't| not)] connected`
### Player connected — `%bungeeplayer% is [(n't| not)] connected`
### Player has permission — `%bungeeplayer% (has|(doesn't|does not) have) permission %string%`
### Server started — `%bungeeserver% is [(n't| not)] (started|online)`

---

## Sections

### New connection — `(create|init) new bungee connection`
Configure a connection, then start it:
```applescript
on load:
    create new bungee connection:
        set address of connection to "127.0.0.1"
        set port of connection to 20000
        set password of connection to "YourPassword"
    start new connection with connection
```

### New dynamic server — `create new bungee server`
Configure a dynamic server, then register it:
```applescript
create new bungee server:
    set name of server builder to "minigame-1"
    set address of server builder to "127.0.0.1"
    set port of server builder to 25570
    set motd of server builder to "Minigame lobby"
put server builder into bungeecord
```

---

## Types

| Type | Meaning |
|------|---------|
| `bungeeconn` | A connection builder (address, port, password) to the proxy |
| `bungeeplayer` | A player anywhere on the network |
| `bungeeserver` | A server on the network / defined in the proxy config |
| `serverbuilder` | A builder for a dynamic server (name, address, port, motd) to add to the proxy |
