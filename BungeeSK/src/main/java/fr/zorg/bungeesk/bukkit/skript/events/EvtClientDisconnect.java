package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.ClientDisconnectEvent;

public class EvtClientDisconnect {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "client disconnect", ClientDisconnectEvent.class,
                                "[bungee] client disconnect")
                        .addDescription("When the client disconnects from the server")
                        .addExamples("on bungee client disconnect:", "\tbroadcast \"&cClient disconnected !\"")
                        .addSince("1.0.0"));
    }

}