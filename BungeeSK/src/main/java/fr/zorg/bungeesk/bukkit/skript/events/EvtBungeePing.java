package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeePingEvent;

public class EvtBungeePing {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "proxy ping", BungeePingEvent.class,
                                "(proxy|bungee) ping")
                        .addDescription("When the proxy is being ping by a player. Need to inform the proxy " +
                                "that this server is listening to this event with the 'Listen to proxy ping' effect")
                        .addExamples("on proxy ping:", "\tset max players to 10", "\tset connected players to 5", "\t#And so on...")
                        .addSince("2.0.0"));
    }

}