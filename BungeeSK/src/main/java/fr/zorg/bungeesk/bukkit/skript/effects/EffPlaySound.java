package fr.zorg.bungeesk.bukkit.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.packets.PlaySoundPacket;
import org.bukkit.event.Event;

@Name("Play sound to a bungee player")
@Description({"Plays a sound to a player anywhere on the network. The sound is a namespaced key, e.g.",
        "\"entity.experience_orb.pickup\" or \"minecraft:block.note_block.pling\". Volume and pitch default to 1."})
@Examples({"play bungee sound \"entity.experience_orb.pickup\" to bungee player named \"Notch\"",
        "play bungee sound \"block.note_block.pling\" with volume 1 and pitch 2 to {_bungeeplayer}"})
@Since("2.2.0")
public class EffPlaySound extends Effect {

    static {
        Skript.registerEffect(EffPlaySound.class,
                "(play|send) [bungee] sound %string% [(at|with) volume %-number%] [(and|with) pitch %-number%] to %bungeeplayer%");
    }

    private Expression<String> sound;
    private Expression<Number> volume;
    private Expression<Number> pitch;
    private Expression<BungeePlayer> player;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.sound = (Expression<String>) exprs[0];
        this.volume = (Expression<Number>) exprs[1];
        this.pitch = (Expression<Number>) exprs[2];
        this.player = (Expression<BungeePlayer>) exprs[3];
        return true;
    }

    @Override
    protected void execute(Event e) {
        final BungeePlayer bungeePlayer = this.player.getSingle(e);
        final String sound = this.sound.getSingle(e);
        if (bungeePlayer == null || sound == null)
            return;

        final float volume = number(this.volume, e, 1f);
        final float pitch = number(this.pitch, e, 1f);
        PacketClient.sendPacket(new PlaySoundPacket(bungeePlayer, sound, volume, pitch));
    }

    private static float number(Expression<Number> expr, Event e, float fallback) {
        if (expr == null)
            return fallback;
        final Number value = expr.getSingle(e);
        return value == null ? fallback : value.floatValue();
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "play sound " + this.sound.toString(e, debug) + " to " + this.player.toString(e, debug);
    }

}
