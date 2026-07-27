package fr.zorg.velocitysk.packets.listeners;

import com.velocitypowered.api.proxy.Player;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.SendTitlePacket;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.VelocityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;

public class SendTitleListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof SendTitlePacket) {
            final SendTitlePacket sendTitlePacket = (SendTitlePacket) packet;

            final BungeePlayer bungeePlayer = sendTitlePacket.getBungeePlayer();
            final Player player = VelocityUtils.getManipulablePlayer(bungeePlayer);
            if (player == null)
                return;

            final Component titleComponent = VelocityUtils.getTextComponent(sendTitlePacket.getTitle());
            final Component subTitle = sendTitlePacket.getSubTitle() == null ? Component.empty() : VelocityUtils.getTextComponent(sendTitlePacket.getSubTitle());

            final Title title;
            if (sendTitlePacket.getTime() != null) {
                // BungeeSK sends the timings in ticks (1 tick = 50 ms); Adventure wants Durations. The old
                // code fed the tick count straight into Duration.of(..., SECONDS), so "for 2 seconds" (40
                // ticks) actually displayed for 40 seconds.
                final Title.Times times = Title.Times.times(
                        ticksToDuration(sendTitlePacket.getFadeIn(), 10L),
                        ticksToDuration(sendTitlePacket.getTime(), 70L),
                        ticksToDuration(sendTitlePacket.getFadeOut(), 20L));
                title = Title.title(titleComponent, subTitle, times);
            } else {
                title = Title.title(titleComponent, subTitle);
            }

            player.showTitle(title);
        }
    }

    private static Duration ticksToDuration(Long ticks, long defaultTicks) {
        // BungeeSK sends title timings in ticks; Adventure wants a Duration (1 tick = 50 ms). Fall back
        // to the vanilla defaults (fade-in 10, stay 70, fade-out 20 ticks) when a value is unset.
        return Duration.ofMillis((ticks == null ? defaultTicks : ticks) * 50L);
    }

}