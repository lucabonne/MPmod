package net.minepiece.qol.essentials;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.item.ItemStack;

/** Adapted from Aure64's background GUI hooks, with validation and vanilla replay. Client thread only. */
public final class BackgroundMenus {
    private final BackgroundReadState state = new BackgroundReadState();
    private ClientboundOpenScreenPacket open;
    private ClientboundContainerSetContentPacket full;
    private final List<ClientboundContainerSetSlotPacket> slots = new ArrayList<>();
    private List<ItemStack> items = List.of();
    private Predicate<List<ItemStack>> recognises;
    private Consumer<List<ItemStack>> consumer;
    private long lastRead;
    private boolean issuingCommand;
    public boolean busy() { return state.active(); }
    public boolean request(String command, Predicate<List<ItemStack>> validator, Consumer<List<ItemStack>> reader) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        if (busy() || now - lastRead < 1500 || mc.player == null || mc.getConnection() == null || mc.gui.screen() != null) return false;
        reset(); lastRead = now; recognises = validator; consumer = reader; state.start(now);
        issuingCommand = true;
        try { mc.player.connection.sendCommand(command); } finally { issuingCommand = false; }
        return true;
    }
    public boolean onOpen(ClientboundOpenScreenPacket packet) {
        if (busy() && state.container() >= 0) { replay(); return false; }
        if (!state.claim(packet.getContainerId(), System.currentTimeMillis())) return false;
        open = packet;
        return true;
    }
    public boolean onContent(ClientboundContainerSetContentPacket packet) {
        if (!state.owns(packet.containerId())) return false;
        full = packet; slots.clear();
        items = packet.items().stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        boolean valid;
        // A malformed server tooltip must be handed to vanilla rather than crash a network hook.
        try { valid = recognises.test(containerItems()); } catch (RuntimeException ignored) { valid = false; }
        state.content(valid, System.currentTimeMillis());
        return true;
    }
    public boolean onSlot(ClientboundContainerSetSlotPacket packet) {
        if (!state.owns(packet.getContainerId())) return false;
        slots.add(packet);
        int slot = packet.getSlot();
        if (slot >= 0 && slot < items.size()) items.set(slot, packet.getItem().copy());
        state.slot(System.currentTimeMillis());
        return true;
    }
    public boolean onClose(int id) {
        if (!state.owns(id)) return false;
        reset(); return true;
    }
    private List<ItemStack> containerItems() { return items.subList(0, Math.max(0, items.size() - 36)); }
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        switch (state.tick(System.currentTimeMillis(), mc.gui.screen() != null)) {
            case REPLAY -> replay();
            case TIMEOUT -> reset();
            case DELIVER -> {
                int id = state.container();
                var reader = consumer;
                var result = List.copyOf(containerItems());
                reset();
                if (mc.getConnection() != null) mc.getConnection().send(new ServerboundContainerClosePacket(id));
                reader.accept(result);
            }
            default -> { }
        }
    }
    public boolean isIssuingCommand() { return issuingCommand; }
    public void cancel() { if (open != null) replay(); else reset(); }
    private void replay() {
        var packet = open; var content = full; var updates = List.copyOf(slots);
        reset();
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || packet == null) return;
        connection.handleOpenScreen(packet);
        if (content != null) connection.handleContainerContent(content);
        updates.forEach(connection::handleContainerSetSlot);
    }
    public void reset() { state.reset(); open = null; full = null; slots.clear(); items = List.of(); recognises = null; consumer = null; }
}
