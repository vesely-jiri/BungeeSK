package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.ClientConnectEvent;

public class EvtClientConnect {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "client connect", ClientConnectEvent.class,
                                "[bungee] client connect")
                        .addDescription("When the client connects to a server")
                        .addExamples("on bungee client connect:", "\tbroadcast \"&aClient connected !\"")
                        .addSince("1.0.0"));
    }

}