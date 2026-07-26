package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeeMessageReceiveEvent;
import fr.zorg.bungeesk.common.entities.BungeeServer;

@SuppressWarnings("removal") // deprecated EventValues.registerEventValue is the only modifiable event-value API Skript 2.16 exposes to addons
public class EvtBungeeMessageReceive {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "bungee message receive", BungeeMessageReceiveEvent.class,
                                "bungee [custom] message receive")
                        .addDescription("When a bungee message is received")
                        .addExamples("on bungee message receive:", "\tset {_server} to event-bungeeserver", "\tset {_message} to event-string")
                        .addSince("1.1.0"));

        EventValues.registerEventValue(BungeeMessageReceiveEvent.class, BungeeServer.class, BungeeMessageReceiveEvent::getFrom);

        EventValues.registerEventValue(BungeeMessageReceiveEvent.class, String.class, BungeeMessageReceiveEvent::getMessage);
    }

}