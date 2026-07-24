package fr.zorg.velocitysk;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.zorg.bungeesk.common.AutoUpdater;
import fr.zorg.velocitysk.api.BungeeAPI;
import fr.zorg.velocitysk.commands.BungeeSKCommand;
import fr.zorg.velocitysk.packets.PacketServer;
import fr.zorg.velocitysk.storage.GlobalScripts;
import fr.zorg.velocitysk.storage.GlobalVariables;
import fr.zorg.velocitysk.utils.BungeeConfig;
import fr.zorg.velocitysk.utils.BungeeEventsListener;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;

@Plugin(
        id = "bungeesk",
        name = "BungeeSK",
        version = "2.1.0",
        authors = {"Zorg"},
        url = "https://github.com/ZorgBtw/BungeeSK"
)
public class BungeeSK {

    private static ProxyServer server;
    private static Logger logger;
    private static Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private static BungeeAPI api;
    private static BungeeSK instance;

    @Inject
    public BungeeSK(ProxyServer server, Logger logger, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
        BungeeSK.server = server;
        BungeeSK.logger = logger;
        BungeeSK.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        final long startTime = System.currentTimeMillis();
        metricsFactory.make(this, 20201);

        BungeeSK.api = new BungeeAPI();
        BungeeSK.api.registerListeners("fr.zorg.velocitysk.packets.listeners");
        BungeeSK.instance = this;
        this.launchAutoUpdater();
        BungeeConfig.init();
        GlobalVariables.init();
        GlobalScripts.listenFileChanging();
        PacketServer.start();

        BungeeSK.getServer().getEventManager().register(this, new BungeeEventsListener());

        // Registered as "bungeeskproxy" (alias "bsproxy"), NOT "bungeesk": on a proxy network the
        // proxy intercepts a "/bungeesk" it owns and would shadow the game-server-side "/bungeesk"
        // command (leaving it reachable only as "/bungeesk:bungeesk").
        final CommandManager commandManager = BungeeSK.getServer().getCommandManager();
        BungeeSK.getServer().getCommandManager().register(commandManager.metaBuilder("bungeeskproxy").aliases("bsproxy").plugin(this).build(), new BungeeSKCommand());

        this.logStartupBanner(System.currentTimeMillis() - startTime);
    }

    /**
     * Logs a small structured startup summary via SLF4J (no colour codes — Velocity's logger does not
     * render legacy §/& codes).
     */
    private void logStartupBanner(long ms) {
        final boolean up = PacketServer.getServerSocket() != null && !PacketServer.getServerSocket().isClosed();
        logger.info("BungeeSK v{} (Velocity proxy) enabled in {}ms", this.getVersion(), ms);
        logger.info("  - Listening: {} ({})", BungeeConfig.PORT.get(), up ? "online" : "offline");
        logger.info("  - Encryption: {} | IP whitelist: {}",
                (Boolean) BungeeConfig.ENCRYPT.get() ? "on" : "off",
                (Boolean) BungeeConfig.WHITELIST_IP$ENABLE.get() ? "on" : "off");
    }

    private String getVersion() {
        return BungeeSK.getServer().getPluginManager().getPlugin("bungeesk")
                .flatMap(c -> c.getDescription().getVersion()).orElse("2.1.0");
    }

    public static ProxyServer getServer() {
        return server;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static Path getDataDirectory() {
        return dataDirectory;
    }

    public static BungeeAPI getApi() {
        return api;
    }

    private void launchAutoUpdater() {
        BungeeSK.getServer().getScheduler().buildTask(this, () -> {
                    if (!AutoUpdater.isUpToDate(BungeeSK.getServer().getPluginManager().getPlugin("bungeesk").get().getDescription().getVersion().get())) {
                        BungeeSK.getLogger().warn("BungeeSK is not up to date ! Please download the latest version here: https//github.com/ZorgBtw/BungeeSK/releases/latest");
                    }
                })
                .repeat(Duration.ofDays(1))
                .schedule(); // Everyday
    }

    public static void runAsync(Runnable task) {
        BungeeSK.getServer().getScheduler().buildTask(BungeeSK.getInstance(), task).schedule();
    }

    public static BungeeSK getInstance() {
        return instance;
    }

}