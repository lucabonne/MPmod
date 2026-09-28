package net.minepiece.qol.state;

import com.google.gson.Gson;
import java.util.List;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.parse.ProfileXpParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgressHudControllerTest {
    private static final List<String> PROFILE = List.of(
        "Information", "Progression", "Ascension: 0", "Level: 63 (15.54%)", "47784 ▰▱ 307450",
        "Statistics", "Health +2665", "Strength +343", "Defense +266"
    );

    @Test
    void visibleServerXpReconcilesBoostersAndHiddenBarKeepsUpdating() {
        ProgressHudController controller = new ProgressHudController();
        controller.profile().sync(new ProfileXpParser.Snapshot(0, 63, 1000, 10000));
        var initial = new ProfileXpParser.BarProgress(63, 10, true);
        controller.observeXpBar(initial, 1000);
        controller.observeXpBar(initial, 1500);
        controller.onActionbar("+1 军 +100 实", 2000);
        controller.onItemXp(100, true, 2100);
        var boosted = new ProfileXpParser.BarProgress(63, 12, true);
        controller.observeXpBar(boosted, 2200);
        controller.observeXpBar(boosted, 2700);
        assertEquals(1200, controller.profile().getCurrentXp(), 0.001);
        assertEquals(200, controller.grinding().getXp(), 0.001);
        controller.onActionbar("+2 军 +200 实", 3000);
        controller.onItemXp(100, true, 3100);
        // The old visible text must not undo an immediate item update.
        controller.observeXpBar(boosted, 3200);
        assertEquals(1300, controller.profile().getCurrentXp(), 0.001);
        // No bar observations while it is hidden; catch up when it returns, even after danger expires.
        var returned = new ProfileXpParser.BarProgress(63, 14, true);
        controller.observeXpBar(returned, 7000);
        controller.observeXpBar(returned, 7500);
        controller.observeXpBar(returned, 8000);
        assertEquals(1400, controller.profile().getCurrentXp(), 0.001);
        assertEquals(400, controller.grinding().getXp(), 0.001);
        assertFalse(controller.grinding().isActive(63100)); // Corrections do not restart the idle timer.
    }

    @Test
    void serverPacketBeforeItemPacketDoesNotDoubleCountReward() {
        ProgressHudController controller = new ProgressHudController();
        controller.profile().sync(new ProfileXpParser.Snapshot(0, 63, 1000, 10000));
        var boosted = new ProfileXpParser.BarProgress(63, 12, true);
        controller.onActionbar("+1 军", 1000);
        controller.observeXpBar(boosted, 1000);
        controller.onItemXp(100, true, 1200);
        controller.observeXpBar(boosted, 1500);
        assertEquals(1200, controller.profile().getCurrentXp(), 0.001);
        assertEquals(200, controller.grinding().getXp(), 0.001);
    }

    @Test
    void cachedCorrectionDoesNotCountOfflineXpAndLowerSameLevelReadingIsValid() {
        ProgressHudController controller = new ProgressHudController();
        controller.profile().restore(new ProfileXpTracker.SavedProfile(
            new ProfileXpParser.Snapshot(0, 63, 1500, 10000), false, 63));
        var live = new ProfileXpParser.BarProgress(63, 12, true);
        controller.observeXpBar(live, 1000);
        controller.observeXpBar(live, 1500);
        assertEquals(1200, controller.profile().getCurrentXp(), 0.001);
        assertEquals(0, controller.grinding().getXp());
        assertFalse(controller.profile().needsSync());
        controller.onActionbar("+1 军", 2000);
        controller.onItemXp(200, true, 2100);
        live = new ProfileXpParser.BarProgress(63, 13, true);
        controller.observeXpBar(live, 2200);
        controller.observeXpBar(live, 2700);
        assertEquals(1300, controller.profile().getCurrentXp(), 0.001);
        assertEquals(100, controller.grinding().getXp(), 0.001);
        assertFalse(controller.profile().needsSync());
        live = new ProfileXpParser.BarProgress(64, 1, true);
        controller.observeXpBar(live, 3000);
        controller.observeXpBar(live, 3500);
        assertTrue(controller.profile().needsSync());
    }

    @Test
    void persistsPerAccountAndServerAndCountsSharedXpWithoutTopBar() {
        long now = System.currentTimeMillis();
        PersistentState state = new PersistentState();
        ProgressHudController controller = new ProgressHudController();
        controller.attachPersistence(state, () -> { });
        controller.connect("server|player1", now);
        controller.profile().sync(new ProfileXpParser.Snapshot(0, 63, 47784, 307450));
        controller.onActionbar("+1 军 +100 实", now + 100);
        controller.onItemXp(100, true, now + 200);
        assertEquals(47884, controller.profile().getCurrentXp());
        assertEquals(100, controller.grinding().getXp());
        controller.disconnect(now + 300);
        PersistentState decoded = new Gson().fromJson(new Gson().toJson(state), PersistentState.class);
        controller.attachPersistence(decoded, () -> { });
        controller.connect("server|player1", now + 400);
        assertEquals(47884, controller.profile().getCurrentXp());
        assertEquals(100, controller.grinding().getMoney());
        controller.connect("server|player2", now + 500);
        assertNull(controller.profile().getAnchor());
        assertEquals(0, controller.grinding().getMoney());
    }

    @Test
    void itemPacketCanArriveBeforeDangerButNoncombatUpgradesAreNotPlayerXp() {
        long now = System.currentTimeMillis();
        ProgressHudController controller = new ProgressHudController();
        controller.profile().sync(new ProfileXpParser.Snapshot(0, 63, 47784, 307450));
        controller.onItemXp(100, true, now);
        assertEquals(47784, controller.profile().getCurrentXp());
        controller.onActionbar("+1 军 +100 实", now + 100);
        assertEquals(47884, controller.profile().getCurrentXp());
        controller.onItemXp(500, true, now + 60000);
        controller.onActionbar("+1 军 +100 实", now + 65000);
        assertEquals(47884, controller.profile().getCurrentXp());
    }

    @Test
    void readsOnlyRequestedOwnProfileAndResetClearsSession() {
        ProgressHudController controller = new ProgressHudController();
        controller.captureTooltip(PROFILE);
        assertNull(controller.profile().getAnchor());
        controller.onCommandSent("profile SomeoneElse");
        controller.captureTooltip(PROFILE);
        assertNull(controller.profile().getAnchor());
        controller.onCommandSent("/profile");
        controller.captureTooltip(PROFILE);
        assertEquals(47784, controller.profile().getCurrentXp());
        controller.profile().updatePercent(new ProfileXpParser.BarProgress(64, 5, true));
        assertTrue(controller.profile().needsSync());
        controller.captureTooltip(PROFILE);
        assertFalse(controller.profile().needsSync());
        controller.grinding().onActionbar("+1 军 +100 实", 1000);
        controller.reset();
        assertNull(controller.profile().getAnchor());
        assertEquals(0, controller.grinding().getMoney());
        controller.captureTooltip(PROFILE);
        assertNull(controller.profile().getAnchor());
    }

    @Test
    void existingConfigGetsNewPanelDefaultsWithoutChangingOldSettings() {
        ConfigManager.ModConfig config = new Gson().fromJson(
            "{\"inventoryXpHudEnabled\":false,\"moneyHudX\":42}", ConfigManager.ModConfig.class);
        assertFalse(config.inventoryXpHudEnabled);
        assertEquals(42, config.moneyHudX);
        assertTrue(config.profileXpHudEnabled);
        assertTrue(config.grindingHudEnabled);
        assertEquals(1.0F, config.inventoryXpHudScale);
        assertEquals(1.0F, config.grindingHudScale);
    }
}
