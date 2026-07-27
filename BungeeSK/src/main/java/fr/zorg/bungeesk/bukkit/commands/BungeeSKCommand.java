package fr.zorg.bungeesk.bukkit.commands;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.bukkit.utils.BungeeSKConfig;
import fr.zorg.bungeesk.common.BuildInfo;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The game-server (Bukkit) side {@code /bungeesk} admin command. Unlike the proxy command it runs
 * locally on the backend server, so it can report and control THIS server's link to the proxy
 * (state, reconnect, disconnect) and reload {@code config.yml} without a full server restart.
 */
public class BungeeSKCommand implements CommandExecutor, TabCompleter {

    public static final String PREFIX = color("&6BungeeSK &7» ");
    private static final List<String> SUBCOMMANDS = Arrays.asList("help", "status", "reconnect", "disconnect", "reload", "version");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bungeesk.command")) {
            sender.sendMessage(PREFIX + color("&cYou don't have permission to use this command."));
            return true;
        }

        final String sub = args.length == 0 ? "help" : args[0].toLowerCase();
        switch (sub) {
            case "status":
            case "state": {
                final boolean connected = PacketClient.isConnected();
                sender.sendMessage(PREFIX + color("&3Status"));
                sender.sendMessage(color("  &8» &7State: " + (connected ? "&a" : "&c") + PacketClient.getState().name().toLowerCase()));
                sender.sendMessage(color("  &8» &7Proxy: &f" + BungeeSKConfig.CONNECTION$ADDRESS.getString()
                        + ":" + BungeeSKConfig.CONNECTION$PORT.getInt()));
                sender.sendMessage(color("  &8» &7Auto-connect: " + onOff(BungeeSKConfig.CONNECTION$AUTO_CONNECT.getBoolean())
                        + " &7| Reconnect: " + onOff(BungeeSKConfig.RECONNECT$ENABLED.getBoolean())));
                break;
            }
            case "reconnect": {
                PacketClient.reconnectNow();
                sender.sendMessage(PREFIX + color("&eReconnecting to the proxy..."));
                break;
            }
            case "disconnect": {
                PacketClient.stop();
                sender.sendMessage(PREFIX + color("&7Disconnected from the proxy (auto-reconnect disabled until you reconnect)."));
                break;
            }
            case "reload": {
                BungeeSK.getInstance().reload();
                sender.sendMessage(PREFIX + color("&aReloaded config.yml. State: &b" + PacketClient.getState().name().toLowerCase()));
                break;
            }
            case "version": {
                sender.sendMessage(PREFIX + color("&7Running version &f" + BuildInfo.describe()));
                break;
            }
            case "help":
            default: {
                sender.sendMessage(PREFIX + color("&bHelp"));
                sender.sendMessage(color("  &8» &6/&fbungeesk &3status&e: &7Show this server's connection to the proxy"));
                sender.sendMessage(color("  &8» &6/&fbungeesk &areconnect&e: &7Reconnect to the proxy now"));
                sender.sendMessage(color("  &8» &6/&fbungeesk &cdisconnect&e: &7Disconnect from the proxy"));
                sender.sendMessage(color("  &8» &6/&fbungeesk &ereload&e: &7Reload config.yml and re-apply the connection"));
                sender.sendMessage(color("  &8» &6/&fbungeesk &3version&e: &7Show the plugin version"));
                break;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("bungeesk.command") || args.length != 1)
            return new ArrayList<>();
        final String start = args[0].toLowerCase();
        return SUBCOMMANDS.stream().filter(s -> s.startsWith(start)).collect(Collectors.toList());
    }

    private static String onOff(boolean on) {
        return on ? "&aon" : "&coff";
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
