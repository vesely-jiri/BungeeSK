package fr.zorg.bungeesk.common.packets;

import fr.zorg.bungeesk.common.entities.BungeePlayer;

/**
 * Plays a sound to a networked player. Sent server → proxy, which forwards it to the game server the
 * player is on; that server plays the sound on the local Bukkit player (proxies can't play sounds
 * uniformly, so the game server does it).
 */
public class PlaySoundPacket implements BungeeSKPacket {

    private final BungeePlayer bungeePlayer;
    private final String sound;
    private final float volume;
    private final float pitch;

    public PlaySoundPacket(BungeePlayer bungeePlayer, String sound, float volume, float pitch) {
        this.bungeePlayer = bungeePlayer;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public BungeePlayer getBungeePlayer() {
        return this.bungeePlayer;
    }

    public String getSound() {
        return this.sound;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

}
