package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeeMessageReceiveEvent;
import fr.zorg.bungeesk.common.entities.BungeeServer;

public class EvtBungeeMessageReceive {

    static {
        Skript.registerEvent("bungee message receive", SimpleEvent.class, BungeeMessageReceiveEvent.class,
                        "bungee [custom] message receive")
                .description("When a bungee message is received")
                .examples("on bungee message receive:", "\tset {_server} to event-bungeeserver", "\tset {_message} to event-string")
                .since("1.1.0");

        EventValues.registerEventValue(BungeeMessageReceiveEvent.class, BungeeServer.class, BungeeMessageReceiveEvent::getFrom);

        EventValues.registerEventValue(BungeeMessageReceiveEvent.class, String.class, BungeeMessageReceiveEvent::getMessage);
    }

}