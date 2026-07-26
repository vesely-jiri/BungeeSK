package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeeServerStartEvent;
import fr.zorg.bungeesk.common.entities.BungeeServer;

@SuppressWarnings("removal") // deprecated EventValues.registerEventValue is the only modifiable event-value API Skript 2.16 exposes to addons
public class EvtBungeeServerStart {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "bungee server start", BungeeServerStartEvent.class,
                                "bungee server (start|connect)")
                        .addDescription("When a bungee server is started / connected to BungeeSK")
                        .addExamples("on bungee server start:", "\tset {_server} to event-bungeeserver")
                        .addSince("2.0.0"));

        EventValues.registerEventValue(BungeeServerStartEvent.class, BungeeServer.class, BungeeServerStartEvent::getBungeeServer);
    }

}