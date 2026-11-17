package net.minepiece.qol.essentials;

import java.util.HashSet;
import java.util.Set;
import net.minepiece.qol.MinepieceQolClient;

/** Visual boss notifications, using QoL timers. */
public final class BossAlerts {
    private final Set<String> alerted = new HashSet<>();
    private long lastAlert;
    public void tick(MinepieceQolClient mod) {
        long now = System.currentTimeMillis();
        Set<String> present = new HashSet<>();
        for (var boss : mod.getBossTracker().getActiveBossWaypoints()) {
            String key = boss.name() + "|" + boss.x() + "|" + boss.y() + "|" + boss.z();
            present.add(key);
            if (boss.remainingMs() > 30000) { alerted.remove(key); continue; }
            if (boss.remainingMs() <= 0 || alerted.contains(key) || now - lastAlert < 5000) continue;
            net.minecraft.client.Minecraft.getInstance().player.sendSystemMessage(net.minecraft.network.chat.Component.literal(boss.name() + " • " + mod.tr("extras.boss_soon")).withColor(0xFFCC55));
            alerted.add(key); lastAlert = now;
        }
        alerted.retainAll(present);
    }
    public void reset() { alerted.clear(); lastAlert = 0; }
}
