package fr.zorg.bungeesk.bukkit;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxRegistry;
import fr.zorg.bungeesk.bukkit.api.BukkitAPI;
import fr.zorg.bungeesk.bukkit.commands.BungeeSKCommand;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.bukkit.utils.BungeeSKConfig;
import fr.zorg.bungeesk.bukkit.utils.ClientBuilder;
import fr.zorg.bungeesk.bukkit.utils.Metrics;
import fr.zorg.bungeesk.common.AutoUpdater;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BungeeSK extends JavaPlugin implements Listener {

    private static BukkitAPI api;
    private static ExecutorService executor;
    private static SyntaxRegistry syntaxRegistry;
    private Metrics metrics;

    // Skript.registerAddon(JavaPlugin) + ch.njol.skript.SkriptAddon are deprecated for removal, but the
    // modern org.skriptlang.skript.addon.SkriptAddon has no loadClasses(...) — which is how every syntax
    // class here gets loaded and self-registered — so the classic addon is the only workable path.
    @SuppressWarnings("removal")
    @Override
    public void onEnable() {
        final long startTime = System.currentTimeMillis();
        executor = Executors.newCachedThreadPool(runnable -> {
            final Thread thread = new Thread(runnable, "BungeeSK-Async");
            thread.setDaemon(true);
            return thread;
        });

        this.launchAutoUpdater();
        api = new BukkitAPI();
        this.metrics = new Metrics(this, 10655);

        api.registerListeners("fr.zorg.bungeesk.bukkit.packets.listeners", this);

        final SkriptAddon addon = Skript.registerAddon(this);
        // Published before loadClasses(): loading the skript package runs each syntax class's static
        // initializer, which registers itself through Syntax -> getSyntaxRegistry().
        syntaxRegistry = addon.syntaxRegistry();
        try {
            addon.loadClasses("fr.zorg.bungeesk.bukkit.skript");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.metrics.addCustomChart(new Metrics.SimplePie("skript_version", () -> Skript.getVersion().toString()));

        final PluginCommand command = this.getCommand("bungeesk");
        if (command != null) {
            final BungeeSKCommand executor = new BungeeSKCommand();
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        this.setupConnection();
        this.setupPlaceholders();

        this.logStartupBanner(System.currentTimeMillis() - startTime);
    }

    /**
     * Registers the PlaceholderAPI expansion and starts the async network-stats cache — but only when
     * PlaceholderAPI is installed. Reflection-free: the expansion class is loaded lazily here, so a
     * server without PlaceholderAPI never touches its (absent) classes.
     */
    private void setupPlaceholders() {
        if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null)
            return;
        fr.zorg.bungeesk.bukkit.utils.NetworkStats.start();
        new fr.zorg.bungeesk.bukkit.placeholders.BungeeSKExpansion().register();
        this.getLogger().info("Hooked into PlaceholderAPI (%bungeesk_...%).");
    }

    /**
     * Re-reads {@code config.yml}, re-applies reconnect settings and (if auto-connect is on)
     * re-establishes the connection. Backs the {@code /bungeesk reload} command.
     */
    public void reload() {
        this.setupConnection();
    }

    /**
     * Prints a small structured, coloured startup summary to the console (rendered by Paper's
     * console). Shows platform, Skript version, the configured proxy target, reconnect state and how
     * long enabling took.
     */
    private void logStartupBanner(long ms) {
        final boolean autoConnect = BungeeSKConfig.CONNECTION$AUTO_CONNECT.getBoolean();
        final String target = BungeeSKConfig.CONNECTION$ADDRESS.getString() + ":" + BungeeSKConfig.CONNECTION$PORT.getInt();
        final ConsoleCommandSender console = Bukkit.getConsoleSender();
        final String line = "&8&m                                                        ";
        console.sendMessage(color(line));
        console.sendMessage(color(" &6&lBungeeSK &7v" + this.getDescription().getVersion() + " &8• &7game-server side"));
        console.sendMessage(color(" &8» &7Server:    &f" + Bukkit.getName() + " " + Bukkit.getBukkitVersion()));
        console.sendMessage(color(" &8» &7Skript:    &f" + Skript.getVersion()));
        console.sendMessage(color(" &8» &7Proxy:     &f" + target + (autoConnect ? " &8(&aauto-connect&8)" : " &8(&7manual&8)")));
        console.sendMessage(color(" &8» &7Reconnect: " + (BungeeSKConfig.RECONNECT$ENABLED.getBoolean() ? "&aenabled" : "&cdisabled")));
        console.sendMessage(color(" &8» &aEnabled in &f" + ms + "ms"));
        console.sendMessage(color(line));
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public void onDisable() {
        fr.zorg.bungeesk.bukkit.utils.NetworkStats.stop();
        PacketClient.stop();
        if (executor != null)
            executor.shutdownNow();
    }

    private void setupConnection() {
        BungeeSKConfig.init();
        PacketClient.configureReconnect(
                BungeeSKConfig.RECONNECT$ENABLED.getBoolean(),
                BungeeSKConfig.RECONNECT$DELAYS_SECONDS.getIntList(),
                BungeeSKConfig.RECONNECT$INITIAL_DELAY_SECONDS.getInt(),
                BungeeSKConfig.RECONNECT$MAX_DELAY_SECONDS.getInt(),
                BungeeSKConfig.RECONNECT$LOG_ATTEMPTS.getBoolean());

        if (!BungeeSKConfig.CONNECTION$AUTO_CONNECT.getBoolean())
            return;

        final String password = BungeeSKConfig.CONNECTION$PASSWORD.getString();
        if (password.isEmpty()) {
            this.getLogger().warning("Auto-connect is enabled but no password is set in config.yml — skipping.");
            return;
        }

        final String address = BungeeSKConfig.CONNECTION$ADDRESS.getString();
        final int port = BungeeSKConfig.CONNECTION$PORT.getInt();
        final ClientBuilder builder = new ClientBuilder()
                .setAddress(address)
                .setPort(port)
                .setPassword(password.toCharArray());
        this.getLogger().info("Auto-connecting to the proxy at " + address + ":" + port + " ...");
        PacketClient.start(builder);
    }

    public static BungeeSK getInstance() {
        return JavaPlugin.getPlugin(BungeeSK.class);
    }

    public static void runAsync(Runnable task) {
        final ExecutorService pool = executor;
        if (pool != null && !pool.isShutdown()) {
            pool.execute(task);
        } else {
            final Thread thread = new Thread(task, "BungeeSK-Async");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void launchAutoUpdater() {
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (!AutoUpdater.isUpToDate(this.getDescription().getVersion())) {
                this.getLogger().warning("BungeeSK is not up to date ! Please download the latest version here: https//github.com/ZorgBtw/BungeeSK/releases/latest");
            }
        }, 10L, 1728000L); // Everyday
    }

    public static BukkitAPI getApi() {
        return api;
    }

    /** Skript's syntax registry for this addon; set during {@link #onEnable()} before syntaxes load. */
    public static SyntaxRegistry getSyntaxRegistry() {
        return syntaxRegistry;
    }

    public static void callEvent(Event event) {
        getInstance().getServer().getScheduler().runTask(getInstance(), () -> {
            getInstance().getServer().getPluginManager().callEvent(event);
        });
    }

    public static int getMinecraftPort() {
        return getInstance().getServer().getPort();
    }

}