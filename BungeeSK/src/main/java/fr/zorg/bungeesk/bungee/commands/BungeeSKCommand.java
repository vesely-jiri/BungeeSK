package fr.zorg.bungeesk.bungee.commands;

import fr.zorg.bungeesk.bungee.packets.PacketServer;
import fr.zorg.bungeesk.bungee.packets.SocketServer;
import fr.zorg.bungeesk.bungee.utils.BungeeConfig;
import fr.zorg.bungeesk.bungee.utils.BungeeUtils;
import fr.zorg.bungeesk.common.BuildInfo;
import fr.zorg.bungeesk.common.entities.BungeeServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BungeeSKCommand extends Command implements TabExecutor, Listener {

    public static final String PREFIX = "§6BungeeSK §7» ";

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "help", "servers", "disconnect", "start", "stop", "restart", "reload", "version");

    public BungeeSKCommand() {
        // Named "bungeeskproxy" (alias "bskproxy") so it does not collide with the game-server-side
        // "/bungeesk" command: on a proxy network the proxy would otherwise intercept "/bungeesk"
        // and shadow the backend command (which would then only be reachable as "/bungeesk:bungeesk").
        super("bungeeskproxy", null, "bskproxy");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bungeesk.command")) {
            sender.sendMessage(BungeeUtils.getTextComponent("§cYou don't have permission to use this command"));
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§bHelp"));
            sender.sendMessage(BungeeUtils.getTextComponent("  §8» §6/§fbungeeskproxy §3servers§e: §7Displays all servers connected to BungeeSK"));
            sender.sendMessage(BungeeUtils.getTextComponent("  §8» §6/§fbungeeskproxy §cdisconnect <IP:PORT / ALL>§e: §7Disconnect a specific server under BungeeSK"));
            sender.sendMessage(BungeeUtils.getTextComponent("  §8» §6/§fbungeeskproxy §a<start|stop|restart>§e: §7Start, stop or restart BungeeSK"));
            sender.sendMessage(BungeeUtils.getTextComponent("  §8» §6/§fbungeeskproxy §dreload§e: §7Reload config.yml and restart the connection listener"));
            sender.sendMessage(BungeeUtils.getTextComponent("  §8» §6/§fbungeeskproxy §3version§e: §7Show the plugin version and build"));
        } else if (args[0].equalsIgnoreCase("version")) {
            sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§7Running version §f" + BuildInfo.describe()));
        } else if (args[0].equalsIgnoreCase("servers")) {
            if (PacketServer.getServerSocket() == null) {
                sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§cBungeeSK is currently stopped"));
                return;
            }

            // Only list authenticated, fully-registered backends. Internet port-scanners hitting the
            // exposed socket get accepted (then dropped after 5s by the watchdog) and would otherwise
            // show here as bogus "IP:0" entries with no server name.
            final List<SocketServer> connected = PacketServer.getClientSockets().stream()
                    .filter(socket -> socket.isAuthenticated() && socket.getMinecraftPort() != 0)
                    .collect(Collectors.toList());
            if (connected.isEmpty()) {
                sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§fNo servers are connected to BungeeSK"));
                return;
            }
            sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§3Servers"));
            connected.forEach(socket -> {
                String message = "  §8» §6" + socket.getSocket().getInetAddress().getHostAddress() + ":" + socket.getMinecraftPort();
                final BungeeServer server = BungeeUtils.getServerFromSocket(socket);
                if (server != null)
                    message += " §f(§e" + server.getName() + " §7-> §b" + socket.getPing() + "ms§f)";
                if (sender instanceof ProxiedPlayer) {
                    final TextComponent component = new TextComponent(message);
                    final TextComponent disconnectServerComponent = new TextComponent(" §c[✖]");
                    disconnectServerComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, BungeeUtils.getTextComponent("§cDisconnect this server")));
                    disconnectServerComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bungeeskproxy disconnect " + socket.getSocket().getInetAddress().getHostAddress() + ":" + socket.getMinecraftPort()));
                    component.addExtra(disconnectServerComponent);
                    sender.sendMessage(component);
                } else
                    sender.sendMessage(BungeeUtils.getTextComponent(message));
            });
        } else if (args[0].equalsIgnoreCase("disconnect")) {
            if (args.length < 2) {
                sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§cYou must specify a server to disconnect !"));
                return;
            }

            if (args[1].equalsIgnoreCase("all")) {
                new ArrayList<>(PacketServer.getClientSockets()).forEach(SocketServer::disconnect);
                sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§aAll BungeeSK clients are now disconnected !"));
                return;
            }

            if (!args[1].contains(":")) {
                sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§cThe server IP:PORT is invalid !"));
                return;
            }

            final SocketServer server = PacketServer.getClientSockets().stream().filter(socket -> socket.getSocket().getInetAddress().getHostAddress().equals(args[1].split(":")[0]) && socket.getMinecraftPort() == Integer.parseInt(args[1].split(":")[1])).findFirst().orElse(null);

            if (server == null) {
                sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§cNo server exists under this IP:PORT"));
                return;
            }

            server.disconnect();
            sender.sendMessage(BungeeUtils.getTextComponent(PREFIX +
                    String.format("§7Disconnected server under address %s and port %s", server.getSocket().getInetAddress().getHostAddress(), server.getMinecraftPort())));
        } else if (args[0].equalsIgnoreCase("stop")) {
            if (PacketServer.getServerSocket() == null || PacketServer.getServerSocket().isClosed()) {
                sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§cBungeeSK is already stopped !"));
                return;
            }
            PacketServer.stop();
            sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§cBungeeSK has been stopped successfully !"));
        } else if (args[0].equalsIgnoreCase("start")) {
            if (PacketServer.getServerSocket() != null && !PacketServer.getServerSocket().isClosed()) {
                sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§cBungeeSK is already started !"));
                return;
            }
            PacketServer.start();
            sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§aBungeeSK has been started successfully !"));
        } else if (args[0].equalsIgnoreCase("restart")) {
            if (PacketServer.getServerSocket() == null || PacketServer.getServerSocket().isClosed()) {
                sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§cBungeeSK is already started !"));
                return;
            }
            PacketServer.stop();
            PacketServer.start();
            sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§aBungeeSK has been restarted successfully !"));
        } else if (args[0].equalsIgnoreCase("reload")) {
            BungeeConfig.init();
            if (PacketServer.getServerSocket() != null && !PacketServer.getServerSocket().isClosed())
                PacketServer.stop();
            PacketServer.start();
            sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§aReloaded config.yml and restarted BungeeSK on port §f" + BungeeConfig.PORT.get() + "§a."));
        } else {
            sender.sendMessage(BungeeUtils.getTextComponent(PREFIX + "§cUnknown command !"));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bungeesk.command"))
            return Collections.emptyList();

        if (args.length <= 1) {
            final String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("disconnect")) {
            final String prefix = args[1].toLowerCase();
            final List<String> options = new ArrayList<>();
            options.add("all");
            PacketServer.getClientSockets().stream()
                    .filter(socket -> socket.isAuthenticated() && socket.getMinecraftPort() != 0)
                    .forEach(socket -> options.add(socket.getSocket().getInetAddress().getHostAddress() + ":" + socket.getMinecraftPort()));
            return options.stream()
                    .filter(option -> option.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent e) {
        if (((CommandSender) e.getSender()).hasPermission("bungeesk.command"))
            return;
        e.getSuggestions().removeIf(s -> s.equalsIgnoreCase("bungeeskproxy") || s.equalsIgnoreCase("bskproxy"));
    }

}