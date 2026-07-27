package fr.zorg.bungeesk.bukkit.skript.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.packets.MakeBungeePlayerCommandPacket;
import org.bukkit.event.Event;

@Name("Make bungee player execute command")
@Description({"Make a player on the bungeecord execute a specific command",
        "Note: reaches players even on servers without BungeeSK, unless 'affect_all_servers' is disabled in the proxy config."})
@Examples("make bungee player named \"Notch\" execute command \"say What's up people !\"")
@Since("1.1.0")
public class EffMakeBungeePlayerExecuteCommand extends Effect {

    static {
        Syntax.effect(EffMakeBungeePlayerExecuteCommand.class, EffMakeBungeePlayerExecuteCommand::new,
                "make %bungeeplayers% execute command %string%");
    }

    private Expression<BungeePlayer> players;
    private Expression<String> command;

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.players = (Expression<BungeePlayer>) exprs[0];
        this.command = (Expression<String>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        final BungeePlayer[] players = this.players.getArray(e);
        final String command = this.command.getSingle(e);
        if (players.length == 0 || command == null)
            return;

        for (final BungeePlayer player : players)
            PacketClient.sendPacket(new MakeBungeePlayerCommandPacket(player, command));
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "make " + this.players.toString(e, debug) + " execute command " + this.command.toString(e, debug);
    }

}