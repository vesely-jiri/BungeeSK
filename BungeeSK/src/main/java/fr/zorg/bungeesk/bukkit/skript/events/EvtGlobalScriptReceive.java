package fr.zorg.bungeesk.bukkit.skript.events;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.GlobalScriptReceiveEvent;

public class EvtGlobalScriptReceive {

    static {
        Syntax.registerEvent(
                Syntax.event(SimpleEvent.class, SimpleEvent::new, "global script receive", GlobalScriptReceiveEvent.class,
                                "(bungee|global) script receive")
                        .addDescription("When a global script is received")
                        .addExamples("on global script receive:", "\tset {_name} to event-string")
                        .addSince("2.0.0"));

        EventValues.registerEventValue(GlobalScriptReceiveEvent.class, String.class, GlobalScriptReceiveEvent::getScriptName);
    }

}