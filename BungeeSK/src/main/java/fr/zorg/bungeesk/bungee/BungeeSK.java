package fr.zorg.bungeesk.bungee;

import fr.zorg.bungeesk.bungee.api.BungeeAPI;
import fr.zorg.bungeesk.bungee.commands.BungeeSKCommand;
import fr.zorg.bungeesk.bungee.packets.PacketServer;
import fr.zorg.bungeesk.bungee.storage.GlobalScripts;
import fr.zorg.bungeesk.bungee.storage.GlobalVariables;
import fr.zorg.bungeesk.bungee.utils.BungeeConfig;
import fr.zorg.bungeesk.bungee.utils.BungeeEventsListener;
import fr.zorg.bungeesk.bungee.utils.Metrics;
import fr.zorg.bungeesk.common.AutoUpdater;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class BungeeSK extends Plugin {

    private static BungeeSK instance;
    private static BungeeAPI api;
    private Metrics metrics;

    @Override
    public void onEnable() {
        final long startTime = System.currentTimeMillis();
        instance = this;
        api = new BungeeAPI();
        this.metrics = new Metrics(this, 11146);

        this.launchAutoUpdater();
        BungeeConfig.init();
        GlobalVariables.init();
        PacketServer.start();

        api.registerListeners("fr.zorg.bungeesk.bungee.packets.listeners");
        super.getProxy().getPluginManager().registerListener(this, new BungeeEventsListener());
        super.getProxy().getPluginManager().registerCommand(this, new BungeeSKCommand());
        GlobalScripts.listenFileChanging();

        this.logStartupBanner(System.currentTimeMillis() - startTime);
    }

    /**
     * Prints a small structured, coloured startup summary to the BungeeCord console.
     */
    private void logStartupBanner(long ms) {
        final boolean up = PacketServer.getServerSocket() != null && !PacketServer.getServerSocket().isClosed();
        final String line = "&8&m                                                        ";
        this.console(line);
        this.console(" &6&lBungeeSK &7v" + this.getDescription().getVersion() + " &8• &7BungeeCord proxy");
        this.console(" &8» &7Listening: &f" + BungeeConfig.PORT.get() + " " + (up ? "&8(&aonline&8)" : "&8(&coffline&8)"));
        this.console(" &8» &7Encryption: " + ((Boolean) BungeeConfig.ENCRYPT.get() ? "&aon" : "&coff")
                + " &7| IP whitelist: " + ((Boolean) BungeeConfig.WHITELIST_IP$ENABLE.get() ? "&aon" : "&7off"));
        this.console(" &8» &aEnabled in &f" + ms + "ms");
        this.console(line);
    }

    private void console(String s) {
        this.getProxy().getConsole().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', s)));
    }

    public static BungeeSK getInstance() {
        return instance;
    }

    private void launchAutoUpdater() {
        this.getProxy().getScheduler().schedule(this, () -> {
            if (!AutoUpdater.isUpToDate(this.getDescription().getVersion())) {
                this.getLogger().warning("BungeeSK is not up to date ! Please download the latest version here: https//github.com/ZorgBtw/BungeeSK/releases/latest");
            }
        }, 0, 1L, TimeUnit.DAYS); // Everyday
    }

    public static void runAsync(Runnable task) {
        instance.getProxy().getScheduler().runAsync(instance, task);
    }

    public static BungeeAPI getApi() {
        return api;
    }

}