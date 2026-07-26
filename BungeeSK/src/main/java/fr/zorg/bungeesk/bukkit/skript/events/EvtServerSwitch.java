package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.ServerSwitchEvent;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.entities.BungeeServer;

public class EvtServerSwitch {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "server switch", ServerSwitchEvent.class,
                                "[bungee] server switch")
                        .addDescription("When the player switches between 2 servers")
                        .addExamples("on server switch:", "\tbroadcast \"&6%event-bungeeplayer% &7switched from server &c%past-server% &7to &a%event-bungeeplayer's server% &7!\"")
                        .addSince("1.0.0"));


        EventValues.registerEventValue(ServerSwitchEvent.class, BungeePlayer.class, ServerSwitchEvent::getPlayer);

        EventValues.registerEventValue(ServerSwitchEvent.class, BungeeServer.class, ServerSwitchEvent::getServer);
    }

}