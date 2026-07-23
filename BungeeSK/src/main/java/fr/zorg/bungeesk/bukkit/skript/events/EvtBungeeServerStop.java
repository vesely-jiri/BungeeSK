package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeeServerStopEvent;
import fr.zorg.bungeesk.common.entities.BungeeServer;

public class EvtBungeeServerStop {

    static {
        Skript.registerEvent("bungee server stop", SimpleEvent.class, BungeeServerStopEvent.class,
                        "bungee server (stop|disconnect)")
                .description("When a bungee server is stop / disconnected from BungeeSK")
                .examples("on bungee server stop:", "\tset {_server} to event-bungeeserver")
                .since("2.0.0");

        EventValues.registerEventValue(BungeeServerStopEvent.class, BungeeServer.class, BungeeServerStopEvent::getBungeeServer);
    }

}