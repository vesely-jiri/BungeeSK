package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeeServerStopEvent;
import fr.zorg.bungeesk.common.entities.BungeeServer;

@SuppressWarnings("removal") // deprecated EventValues.registerEventValue is the only modifiable event-value API Skript 2.16 exposes to addons
public class EvtBungeeServerStop {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "bungee server stop", BungeeServerStopEvent.class,
                                "bungee server (stop|disconnect)")
                        .addDescription("When a bungee server is stop / disconnected from BungeeSK")
                        .addExamples("on bungee server stop:", "\tset {_server} to event-bungeeserver")
                        .addSince("2.0.0"));

        EventValues.registerEventValue(BungeeServerStopEvent.class, BungeeServer.class, BungeeServerStopEvent::getBungeeServer);
    }

}