package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeeCommandEvent;
import fr.zorg.bungeesk.common.entities.BungeePlayer;

public class EvtBungeeCommand {

    static {
        Skript.registerEvent("bungee command", SimpleEvent.class, BungeeCommandEvent.class,
                        "bungee command")
                .description("When a command is executed on the network, this could be a Spigot command or a Bungee command")
                .examples("on bungee command:", "\tset {_command} to event-string", "\tset {_player} to event-bungeeplayer")
                .since("2.0.0");

        EventValues.registerEventValue(BungeeCommandEvent.class, BungeePlayer.class, BungeeCommandEvent::getPlayer);

        EventValues.registerEventValue(BungeeCommandEvent.class, String.class, BungeeCommandEvent::getCommand);
    }

}