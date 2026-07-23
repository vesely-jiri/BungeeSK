package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.GlobalScriptReceiveEvent;

public class EvtGlobalScriptReceive {

    static {
        Skript.registerEvent("global script receive", SimpleEvent.class, GlobalScriptReceiveEvent.class,
                        "(bungee|global) script receive")
                .description("When a global script is received")
                .examples("on global script receive:", "\tset {_name} to event-string")
                .since("2.0.0");

        EventValues.registerEventValue(GlobalScriptReceiveEvent.class, String.class, GlobalScriptReceiveEvent::getScriptName);
    }

}