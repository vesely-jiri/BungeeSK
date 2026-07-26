package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.CustomRequestEvent;
import fr.zorg.bungeesk.common.entities.BungeeServer;

public class EvtCustomRequest {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "bungee custom request", CustomRequestEvent.class,
                                "[bungee] custom request")
                        .addDescription("When a custom request is asked from another BungeeServer")
                        .addExamples("broadcast custom request \"hello\" from bungee server named \"lobby\"",
                                "",
                                "#On the other server",
                                "on custom request:",
                                "\tif event-string is \"hello\":",
                                "\t\tset custom request response to \"hi !\"")
                        .addSince("2.0.0"));


        EventValues.registerEventValue(CustomRequestEvent.class, String.class, CustomRequestEvent::getName);

        EventValues.registerEventValue(CustomRequestEvent.class, BungeeServer.class, CustomRequestEvent::getFrom);
    }

}