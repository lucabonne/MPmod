package net.minepiece.qol.essentials;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.state.SafeChestTracker;

/** A chat line and a ping when a Marineford safe's lock timer runs out. */
public final class SafeReadyAlerts {
    private final Map<String, SafeChestTracker.Status> previous = new HashMap<>();

    public record Alert(String text, int color) { }

    public void tick(MinepieceQolClient mod) {
        List<Alert> alerts = check(mod.getSafeChests(), mod::tr, System.currentTimeMillis());
        var mc = Minecraft.getInstance();
        if (alerts.isEmpty() || mc.player == null) return;
        for (Alert alert : alerts) mc.player.sendSystemMessage(Component.literal(alert.text()).withColor(alert.color() & 0xFFFFFF));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.5F));
    }

    /** Safes that went from a running timer to Ready since the last check. The first check after a reset only remembers. */
    List<Alert> check(SafeChestTracker safes, Function<String, String> tr, long now) {
        List<Alert> alerts = new ArrayList<>();
        Map<String, SafeChestTracker.Status> seen = new HashMap<>();
        for (String spawn : safes.spawnIds(now)) {
            for (int i = 0; i < SafeChestTracker.SAFES.size(); i++) {
                String key = spawn + "|" + i;
                SafeChestTracker.Status status = safes.status(spawn, i, now);
                seen.put(key, status);
                if (status == SafeChestTracker.Status.READY && previous.get(key) == SafeChestTracker.Status.LOCKED) {
                    var safe = SafeChestTracker.SAFES.get(i);
                    String where = spawn.isEmpty() ? tr.apply("common.unknown") : spawn;
                    alerts.add(new Alert(String.format(java.util.Locale.ROOT, tr.apply("safes.alert"),
                        tr.apply("safes." + safe.key()), where), safe.color()));
                }
            }
        }
        previous.clear();
        previous.putAll(seen);
        return alerts;
    }

    public void reset() { previous.clear(); }
}
