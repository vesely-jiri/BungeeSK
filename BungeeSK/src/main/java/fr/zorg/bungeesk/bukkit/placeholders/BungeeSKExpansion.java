package fr.zorg.bungeesk.bukkit.placeholders;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.bukkit.utils.NetworkStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion exposing BungeeSK's connection state and cached network stats. Registered
 * only when PlaceholderAPI is installed (see {@code BungeeSK#setupPlaceholders}).
 *
 * <p>Placeholders:
 * <ul>
 *     <li>{@code %bungeesk_connected%} — {@code true}/{@code false} (local, instant)</li>
 *     <li>{@code %bungeesk_state%} — connected/connecting/reconnecting/disconnected (local, instant)</li>
 *     <li>{@code %bungeesk_network_players%} — total players online network-wide (cached, refreshed async)</li>
 * </ul>
 * For a specific server's player count, use the {@code player count of %bungeeserver%} Skript expression.
 */
public class BungeeSKExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "bungeesk";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Zorg";
    }

    @Override
    public @NotNull String getVersion() {
        return BungeeSK.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase()) {
            case "connected":
                return String.valueOf(PacketClient.isConnected());
            case "state":
                return PacketClient.getState().name().toLowerCase();
            case "network_players":
                return String.valueOf(NetworkStats.getNetworkPlayerCount());
            default:
                return null;
        }
    }

}
