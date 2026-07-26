package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeePlayerJoinEvent;
import fr.zorg.bungeesk.common.entities.BungeePlayer;

public class EvtBungeePlayerJoin {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "bungee player join", BungeePlayerJoinEvent.class,
                                "bungee [player] join")
                        .addDescription("When a bungee player joins the network")
                        .addExamples("on bungee player join:", "\tset {_player} to event-bungeeplayer")
                        .addSince("1.0.0"));

        EventValues.registerEventValue(BungeePlayerJoinEvent.class, BungeePlayer.class, BungeePlayerJoinEvent::getPlayer);
    }

}