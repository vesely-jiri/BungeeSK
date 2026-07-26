package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeePlayerLeaveEvent;
import fr.zorg.bungeesk.common.entities.BungeePlayer;

@SuppressWarnings("removal") // deprecated EventValues.registerEventValue is the only modifiable event-value API Skript 2.16 exposes to addons
public class EvtBungeePlayerLeave {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "bungee player leave", BungeePlayerLeaveEvent.class,
                                "bungee [player] (leave|quit)")
                        .addDescription("When a bungee player leaves the network")
                        .addExamples("on bungee player leave:", "\tset {_player} to event-bungeeplayer",
                                "on bungee player quit:" +
                                        "\tbroadcast \"The player was in the %past-server% server !\"")
                        .addSince("1.0.0"));

        EventValues.registerEventValue(BungeePlayerLeaveEvent.class, BungeePlayer.class, BungeePlayerLeaveEvent::getPlayer);
    }

}