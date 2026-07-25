# Networking & Firewall

BungeeSK opens its **own** TCP connection between the proxy and each game server — separate from the Minecraft/player traffic. This page explains how to wire that up across machines and containers (Pterodactyl in particular), and how to firewall it correctly.

## Two separate ports

| Port | Used by | Who listens |
|------|---------|-------------|
| Minecraft / proxy port (e.g. `25565`) | players joining | the proxy |
| **BungeeSK socket port** (default `20000`) | server ↔ proxy link | the **proxy** (BungeeSK) |

The **proxy is the server**: it listens on the BungeeSK port. Each **game server is a client**: it connects *out* to the proxy. So the BungeeSK port only needs to be open **on the proxy**, reachable from the game servers.

> The BungeeSK port and the Minecraft port are different things. Opening `25565` does nothing for BungeeSK.

## Same machine

Everything on one host? Use loopback:

```yaml
# game server config.yml
connection:
  address: "127.0.0.1"
  port: 20000          # == proxy port
```

## Separate machines / containers

`127.0.0.1` only works when the proxy is in the **same** machine/container. Otherwise set `address` to an IP of the proxy that the game server can actually reach — a **private/LAN IP**, not the public one (see [Firewall](#firewall--keep-the-socket-private)).

## Pterodactyl

Pterodactyl runs every server in its own Docker container, so two extra rules apply:

1. **The BungeeSK port must be an allocation on the proxy server.** Pterodactyl only exposes ports that are allocations. Add the BungeeSK port under the proxy server's **Network** tab (this is a *second* allocation, separate from the Minecraft port).
2. **Game servers connect via the proxy's internal IP, not `127.0.0.1`.** A container's loopback can't reach another container. Use the `pterodactyl0` bridge gateway **`172.18.0.1`** (or your node's private IP) — the same IP the allocation is bound to.

**Worked example** — allocation `172.18.0.1 : 5000` on the proxy:

```yaml
# proxy config.yml
port: 5000
```
```yaml
# every game server config.yml
connection:
  address: "172.18.0.1"
  port: 5000
```

> Whatever port you pick, it must be identical in three places: the proxy's `config.yml` `port`, every game server's `connection.port`, and the Pterodactyl **allocation** on the proxy.

## Firewall — keep the socket private

The BungeeSK socket is an **internal server-to-server channel**. Do **not** port-forward it to the public internet. It is protected by a shared password and AES encryption, but an internet-reachable port is still DoS/abuse surface — and internet port-scanners that connect show up as noise (harmless; filtered out of `/bungeeskproxy servers`). See the [security note](#security) below.

Allow the **internal** path, block the public one. With UFW (Docker subnet → BungeeSK port):

```bash
sudo ufw allow from 172.18.0.0/16 to any port 5000 proto tcp
sudo ufw reload
```

> **Docker often bypasses UFW** — published ports go through the iptables `DOCKER` chain, not UFW's `INPUT`. If a `ufw allow` doesn't take effect, add the rule to `DOCKER-USER` instead:
> ```bash
> sudo iptables -I DOCKER-USER -s 172.18.0.0/16 -p tcp --dport 5000 -j ACCEPT   # allow internal
> sudo iptables -I DOCKER-USER -i eth0 -p tcp --dport 5000 -j DROP              # block WAN (use your public NIC)
> ```

You can also gate connections in BungeeSK itself with the proxy's IP whitelist (note the **underscore** in the key):

```yaml
# proxy config.yml
whitelist_ip:
  enable: true
  whitelist:
    - "172.18.0.1"      # the address your game servers connect from
```

## Checklist

1. BungeeSK port is an **allocation** on the proxy server (Pterodactyl).
2. `port` matches in all three: proxy config, every game server config, the allocation.
3. Game server `address` = the proxy's **internal** IP (e.g. `172.18.0.1`), never `127.0.0.1` across containers.
4. Firewall: internal subnet allowed, public blocked.
5. Same `password` everywhere.
6. Verify: `/bungeeskproxy servers` on the proxy lists the game servers; `/bungeesk status` on a game server shows *connected*.

Still stuck? See **[Troubleshooting](Troubleshooting)**.

## Security

Never expose the BungeeSK port to the public internet. Firewall it to your backend hosts (and/or use `whitelist_ip`). The authentication and encryption protect the data, but a publicly open port is unnecessary attack surface.
