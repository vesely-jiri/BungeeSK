package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.BungeeCommandEvent;
import fr.zorg.bungeesk.common.entities.BungeePlayer;

// EventValues.registerEventValue is deprecated for removal, but Skript 2.16 exposes only a read-only
// EventValueRegistry to addons (registering throws UnsupportedOperationException) — the deprecated
// call is the sole working way to register event-values, so the warning is suppressed here.
@SuppressWarnings("removal")
public class EvtBungeeCommand {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "bungee command", BungeeCommandEvent.class,
                                "bungee command")
                        .addDescription("When a command is executed on the network, this could be a Spigot command or a Bungee command")
                        .addExamples("on bungee command:", "\tset {_command} to event-string", "\tset {_player} to event-bungeeplayer")
                        .addSince("2.0.0"));

        EventValues.registerEventValue(BungeeCommandEvent.class, BungeePlayer.class, BungeeCommandEvent::getPlayer);

        EventValues.registerEventValue(BungeeCommandEvent.class, String.class, BungeeCommandEvent::getCommand);
    }

}