package fr.zorg.bungeesk.bukkit;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import fr.zorg.bungeesk.bukkit.api.BukkitAPI;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.bukkit.utils.BungeeSKConfig;
import fr.zorg.bungeesk.bukkit.utils.ClientBuilder;
import fr.zorg.bungeesk.bukkit.utils.Metrics;
import fr.zorg.bungeesk.common.AutoUpdater;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BungeeSK extends JavaPlugin implements Listener {

    private static BukkitAPI api;
    private static ExecutorService executor;
    private Metrics metrics;

    @Override
    public void onEnable() {
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
        try {
            addon.loadClasses("fr.zorg.bungeesk.bukkit.skript");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.metrics.addCustomChart(new Metrics.SimplePie("skript_version", () -> Skript.getVersion().toString()));

        this.setupConnection();
    }

    @Override
    public void onDisable() {
        PacketClient.stop();
        if (executor != null)
            executor.shutdownNow();
    }

    private void setupConnection() {
        BungeeSKConfig.init();
        PacketClient.configureReconnect(
                BungeeSKConfig.RECONNECT$ENABLED.getBoolean(),
                BungeeSKConfig.RECONNECT$INITIAL_DELAY_SECONDS.getInt(),
                BungeeSKConfig.RECONNECT$MAX_DELAY_SECONDS.getInt());

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

    public static void callEvent(Event event) {
        getInstance().getServer().getScheduler().runTask(getInstance(), () -> {
            getInstance().getServer().getPluginManager().callEvent(event);
        });
    }

    public static int getMinecraftPort() {
        return getInstance().getServer().getPort();
    }

}