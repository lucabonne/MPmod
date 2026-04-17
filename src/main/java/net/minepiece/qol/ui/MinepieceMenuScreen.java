package net.minepiece.qol.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.i18n.UiLocalization;
import net.minepiece.qol.state.BossTracker;
import net.minepiece.qol.state.ChatTranslationManager;
import net.minepiece.qol.state.JobsTracker;
import net.minepiece.qol.state.PersistentState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class MinepieceMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 320;
    private static final int SIDEBAR_WIDTH = 110;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_SPACING = 4;
    private static final int CONTENT_PADDING = 12;
    private static final int LIST_ROW_HEIGHT = 18;
    private static final int LIST_ROW_GAP = 4;
    private static final int LIST_ROW_STEP = LIST_ROW_HEIGHT + LIST_ROW_GAP;
    private static final int REMOVE_BUTTON_SIZE = 14;
    private static final int REMOVE_BUTTON_GAP_RIGHT = 2;
    private static final int LIST_TEXT_INSET = 2;
    private static final int LIST_TEXT_BASELINE_OFFSET = 5;
    private static final int PROFILE_FIELD_X_OFFSET = 130;
    private static final int PROFILE_FIELD_WIDTH = 80;
    private static final int PROFILE_ROW_HEIGHT = 16;
    private static final int PROFILE_ROW_STEP = 18;
    private static final int PROFILE_FIELD_HEIGHT = 14;
    private static final int PROFILE_ROWS_Y_OFFSET = 24;
    private static final int PROFILE_SAVE_BUTTON_X_OFFSET = 206;
    private static final int PROFILE_SAVE_BUTTON_WIDTH = 48;
    private static final int PROFILE_SYNC_BUTTON_X_OFFSET = 258;
    private static final int PROFILE_SYNC_BUTTON_WIDTH = 84;
    private static final int MONEY_VALUE_FIELD_X_OFFSET = 80;
    private static final int MONEY_VALUE_FIELD_WIDTH = 80;
    private static final int MONEY_RECEIPTS_TITLE_Y_OFFSET = 92;
    private static final int MONEY_RECEIPTS_LIST_Y_OFFSET = 104;
    private static final int MONEY_RECEIPT_ROW_HEIGHT = 14;
    private static final int MONEY_RECEIPT_ROW_STEP = 16;
    private static final int JOBS_TABLE_Y_OFFSET = 44;
    private static final int JOBS_ROW_HEIGHT = 16;
    private static final int JOBS_ROW_STEP = 18;
    private static final int JOBS_FIELD_HEIGHT = 14;
    private static final int JOBS_LEVEL_X_OFFSET = 70;
    private static final int JOBS_LEVEL_WIDTH = 36;
    private static final int JOBS_CUR_X_OFFSET = 130;
    private static final int JOBS_CUR_WIDTH = 60;
    private static final int JOBS_NEED_X_OFFSET = 210;
    private static final int JOBS_NEED_WIDTH = 60;
    private static final int EVENTS_LIST_TITLE_Y_OFFSET = 24;
    private static final int EVENTS_LIST_START_Y_OFFSET = 34;
    private static final int EVENTS_ROW_HEIGHT = 16;
    private static final int EVENTS_ROW_STEP = 18;
    private static final int EVENTS_EDIT_BUTTON_WIDTH = 34;
    private static final int EVENTS_EDIT_BUTTON_HEIGHT = 14;
    private static final int LANGUAGE_MASTER_CHECKBOX_X_OFFSET = 0;
    private static final int LANGUAGE_AGGRESSIVE_CHECKBOX_X_OFFSET = 204;
    private static final int LANGUAGE_CHILD_CHECKBOX_X_OFFSET = 12;
    private static final int LANGUAGE_CHECKBOX_LABEL_GAP = 18;
    private static final int LANGUAGE_MASTER_CHECKBOX_Y_OFFSET = 0;
    private static final int LANGUAGE_PUBLIC_CHECKBOX_Y_OFFSET = 20;
    private static final int LANGUAGE_PRIVATE_CHECKBOX_Y_OFFSET = 40;
    private static final int LANGUAGE_SYSTEM_CHECKBOX_Y_OFFSET = 60;
    private static final int LANGUAGE_SELECTOR_ROW_HEIGHT = 16;
    private static final int LANGUAGE_SELECTOR_BUTTON_WIDTH = 16;
    private static final int LANGUAGE_SELECTOR_BUTTON_HEIGHT = 14;
    private static final int LANGUAGE_SELECTOR_LEFT_BUTTON_X_OFFSET = 116;
    private static final int LANGUAGE_SELECTOR_VALUE_X_OFFSET = 136;
    private static final int LANGUAGE_SELECTOR_RIGHT_BUTTON_X_OFFSET = 250;
    private static final int LANGUAGE_FROM_ROW_Y_OFFSET = 92;
    private static final int LANGUAGE_TO_ROW_Y_OFFSET = 110;
    private static final int LANGUAGE_ADD_RULE_Y_OFFSET = 128;
    private static final int LANGUAGE_ADD_RULE_WIDTH = 84;
    private static final int LANGUAGE_ADD_RULE_HEIGHT = 16;
    private static final int LANGUAGE_RULES_TITLE_Y_OFFSET = 148;
    private static final int LANGUAGE_RULES_LIST_Y_OFFSET = 158;
    private static final int LANGUAGE_RULE_ROW_HEIGHT = 16;
    private static final int LANGUAGE_RULE_ROW_STEP = 18;
    private static final int LANGUAGE_RULE_TOGGLE_WIDTH = 32;
    private static final int LANGUAGE_RULE_TOGGLE_HEIGHT = 14;
    private static final int[] HUD_COLOR_PANEL_IDS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int MAIN_COLOR_ROW_HEIGHT = 14;
    private static final int MAIN_COLOR_ROW_STEP = 14;
    private static final int MAIN_COLOR_LEFT_BUTTON_X_OFFSET = 132;
    private static final int MAIN_COLOR_RIGHT_BUTTON_X_OFFSET = 220;
    private static final int MAIN_COLOR_BUTTON_WIDTH = 16;
    private static final int MAIN_COLOR_BUTTON_HEIGHT = 14;
    private static final int MAIN_CHECKBOX_ROW_STEP = 22;
    private static final int MAIN_COMPACT_SHIFT_NO_DEBUG = -22;
    private static final int MAIN_LANGUAGE_ROW_Y_OFFSET = 72;
    private static final int MAIN_EDIT_HUD_BUTTON_Y_OFFSET = 96;
    private static final int MAIN_HUD_BUTTON_GAP = 8;
    private static final int MAIN_PANEL_COLORS_Y_OFFSET = 118;
    private static final int BOSSES_MINIBOSS_PANEL_Y_OFFSET = 20;
    private static final int BOSSES_ACTIVE_LABEL_Y_OFFSET = 48;
    private static final int BOSSES_ACTIVE_LIST_Y_OFFSET = 58;
    private static final int BOSSES_ACTIVE_LIST_HEIGHT = 98;
    private static final int BOSSES_ACTIVE_ROW_HEIGHT = 14;
    private static final int BOSSES_ACTIVE_ROW_STEP = 16;
    private static final int BOSSES_REGISTRY_GAP = 18;
    private static final int BOSSES_REGISTRY_LABEL_OFFSET = 14;
    private static final int BOSSES_REGISTRY_ROW_HEIGHT = 16;
    private static final int BOSSES_REGISTRY_ROW_STEP = 18;
    private static final int BOSSES_REGISTRY_LIST_HEIGHT = 72;
    private static final String[] PROFILE_FIELD_KEYS = {
        "profile.health",
        "profile.strength",
        "profile.damage",
        "profile.critical_chance",
        "profile.critical_damage",
        "profile.power",
        "profile.energy",
        "profile.energy_regeneration",
        "profile.speed",
        "profile.dexterity",
        "profile.defense",
        "profile.regeneration"
    };

    // Sapphire palette
    private static final int COLOR_BG_OVERLAY = 0xC0050818;
    private static final int COLOR_PANEL_BG = 0xEE0B1838;
    private static final int COLOR_PANEL_BORDER = 0xFF3D78E0;
    private static final int COLOR_PANEL_BORDER_DARK = 0xFF0E2A6E;
    private static final int COLOR_SIDEBAR_BG = 0xEE091434;
    private static final int COLOR_TAB_NORMAL = 0xCC0E2150;
    private static final int COLOR_TAB_HOVER = 0xDD13347A;
    private static final int COLOR_TAB_ACTIVE = 0xFF1F4FB8;
    private static final int COLOR_TAB_ACCENT = 0xFF6FA8FF;
    private static final int COLOR_TEXT = 0xFFEAF1FF;
    private static final int COLOR_TEXT_DIM = 0xFF89A8DA;
    private static final int COLOR_HEADER = 0xFFAFD0FF;
    private static final int COLOR_DIVIDER = 0x553D78E0;
    private static final int COLOR_ACTION_BUTTON = 0xFF16396E;
    private static final int COLOR_ACTION_BUTTON_HOVER = 0xFF1F4A8C;
    private static final int COLOR_ACTION_BUTTON_DISABLED = 0xFF1A2B49;
    private static final int COLOR_ACTION_BORDER = 0xFFDDBA6A;
    private static final int COLOR_ROW_BOX = 0x66112A52;
    private static final int COLOR_ROW_BOX_BORDER = 0x88649CE4;
    private static final int CLOSE_BUTTON_SIZE = 16;
    private static final int CLOSE_BUTTON_INSET = 4;
    private static final int RESET_CONFIRM_WIDTH = 260;
    private static final int RESET_CONFIRM_HEIGHT = 88;
    private static final int RESET_CONFIRM_BUTTON_SIZE = 18;
    private static final int RESET_CONFIRM_BUTTON_GAP = 14;

    private final MinepieceQolClient mod;
    private Tab currentTab = Tab.MAIN;
    private int panelX;
    private int panelY;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private final List<TabButton> tabButtons = new ArrayList<>();

    // Money widgets
    private TextFieldWidget moneyTotalField;
    private TextFieldWidget moneyMadeTodayField;
    private TextFieldWidget moneyAhSoldField;
    private TextFieldWidget moneyAhBoughtField;
    private int moneyHistoryOffset;

    // Jobs widgets
    private final TextFieldWidget[] jobLevelFields = new TextFieldWidget[4];
    private final TextFieldWidget[] jobCurrentXpFields = new TextFieldWidget[4];
    private final TextFieldWidget[] jobNeededXpFields = new TextFieldWidget[4];

    // Profile widgets
    private final TextFieldWidget[] profileStatFields = new TextFieldWidget[PROFILE_FIELD_KEYS.length];

    // Events widgets
    private TextFieldWidget eventNameField;
    private TextFieldWidget eventTimeField;
    private int eventsOffset;
    private int translationRulesOffset;
    private int translationSourceIndex = 1;
    private int translationTargetIndex = 0;
    private boolean translationSelectionInitialized;
    private int uiLanguageIndex;
    private String editingEventOriginalName = "";
    private String editingEventOriginalTime = "";

    // Bosses page
    private int bossActiveOffset;
    private int bossRegistryOffset;
    private final List<RemoveAction> removeActions = new ArrayList<>();
    private boolean resetHudLayoutConfirmOpen;

    public MinepieceMenuScreen(MinepieceQolClient mod) {
        super(Text.literal(mod == null ? "Minepiece QoL" : mod.tr("menu.title")));
        this.mod = mod;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;
        this.contentX = this.panelX + SIDEBAR_WIDTH + CONTENT_PADDING;
        this.contentY = this.panelY + 40;
        this.contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - CONTENT_PADDING * 2;
        this.contentHeight = this.panelY + PANEL_HEIGHT - CONTENT_PADDING - this.contentY;

        buildSidebar();
        buildTabContent();
    }

    private void buildSidebar() {
        this.tabButtons.clear();
        int sidebarX = this.panelX + 6;
        int sidebarY = this.panelY + 30;
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int by = sidebarY + i * (TAB_HEIGHT + TAB_SPACING);
            TabButton button = new TabButton(sidebarX, by, SIDEBAR_WIDTH - 12, TAB_HEIGHT, tab);
            this.tabButtons.add(button);
        }
    }

    private void buildTabContent() {
        // Reset content widgets but preserve sidebar interaction (which we hand-draw, not as widgets)
        clearChildren();
        this.removeActions.clear();
        switch (this.currentTab) {
            case MAIN -> buildMainTab();
            case BOSSES -> buildBossesTab();
            case MONEY -> buildMoneyTab();
            case JOBS -> buildJobsTab();
            case EVENTS -> buildEventsTab();
            case PROFILE -> buildProfileTab();
            case LANGUAGE -> buildLanguageTab();
            case AUCTION_HOUSE -> buildOtherTab();
        }
    }

    private void buildMainTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY + 8;
        boolean showDebugControls = this.mod.isDebugToolsAvailable();
        int compactShift = showDebugControls ? 0 : MAIN_COMPACT_SHIFT_NO_DEBUG;
        List<UiLocalization.LanguageOption> uiLanguages = this.mod.getSupportedUiLanguages();
        this.uiLanguageIndex = findUiLanguageIndex(uiLanguages, this.mod.getUiLanguage());
        int languageRowY = y + MAIN_LANGUAGE_ROW_Y_OFFSET + compactShift;
        int languageButtonY = languageRowY + (LIST_ROW_HEIGHT - MAIN_COLOR_BUTTON_HEIGHT) / 2;
        int editHudButtonY = y + MAIN_EDIT_HUD_BUTTON_Y_OFFSET + compactShift;
        int panelSectionY = y + MAIN_PANEL_COLORS_Y_OFFSET + compactShift;

        addDrawableChild(CheckboxWidget.builder(trText("setting.mod_enabled"), this.textRenderer)
            .pos(x, y).checked(cfg.modEnabled)
            .callback((box, checked) -> this.mod.setModEnabled(checked))
            .build());

        addDrawableChild(CheckboxWidget.builder(trText("setting.show_all_features"), this.textRenderer)
            .pos(x, y + MAIN_CHECKBOX_ROW_STEP).checked(cfg.allFeaturesVisible)
            .callback((box, checked) -> this.mod.setAllFeaturesVisible(checked))
            .build());

        if (showDebugControls) {
            addDrawableChild(CheckboxWidget.builder(trText("setting.debug_logging"), this.textRenderer)
                .pos(x, y + MAIN_CHECKBOX_ROW_STEP * 2).checked(cfg.debugEnabled)
                .callback((box, checked) -> this.mod.setDebugEnabled(checked))
                .build());
        }

        int hudButtonsTotalWidth = this.contentWidth - 4;
        int hudButtonWidth = (hudButtonsTotalWidth - MAIN_HUD_BUTTON_GAP) / 2;
        addDrawableChild(ButtonWidget.builder(trText("button.edit_hud_layout"), button -> {
                this.mod.openHudLayoutEditor();
            })
            .dimensions(x, editHudButtonY, hudButtonWidth, 20)
            .build());
        addDrawableChild(ButtonWidget.builder(trText("button.reset_hud_layout"), button -> {
                this.resetHudLayoutConfirmOpen = true;
            })
            .dimensions(x + hudButtonWidth + MAIN_HUD_BUTTON_GAP, editHudButtonY, hudButtonWidth, 20)
            .build());

        if (!uiLanguages.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                    this.uiLanguageIndex = Math.floorMod(this.uiLanguageIndex - 1, uiLanguages.size());
                    this.mod.setUiLanguage(uiLanguages.get(this.uiLanguageIndex).code());
                    rebuild();
                })
                .dimensions(x + MAIN_COLOR_LEFT_BUTTON_X_OFFSET, languageButtonY, MAIN_COLOR_BUTTON_WIDTH, MAIN_COLOR_BUTTON_HEIGHT)
                .build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                    this.uiLanguageIndex = Math.floorMod(this.uiLanguageIndex + 1, uiLanguages.size());
                    this.mod.setUiLanguage(uiLanguages.get(this.uiLanguageIndex).code());
                    rebuild();
                })
                .dimensions(x + MAIN_COLOR_RIGHT_BUTTON_X_OFFSET, languageButtonY, MAIN_COLOR_BUTTON_WIDTH, MAIN_COLOR_BUTTON_HEIGHT)
                .build());
        }

        for (int i = 0; i < HUD_COLOR_PANEL_IDS.length; i++) {
            int panelId = HUD_COLOR_PANEL_IDS[i];
            int rowY = panelSectionY + 16 + i * MAIN_COLOR_ROW_STEP;
            int buttonY = rowY + (MAIN_COLOR_ROW_HEIGHT - MAIN_COLOR_BUTTON_HEIGHT) / 2;
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                    this.mod.cycleHudPanelColor(panelId, -1);
                    this.mod.saveConfig();
                })
                .dimensions(x + MAIN_COLOR_LEFT_BUTTON_X_OFFSET, buttonY, MAIN_COLOR_BUTTON_WIDTH, MAIN_COLOR_BUTTON_HEIGHT)
                .build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                    this.mod.cycleHudPanelColor(panelId, 1);
                    this.mod.saveConfig();
                })
                .dimensions(x + MAIN_COLOR_RIGHT_BUTTON_X_OFFSET, buttonY, MAIN_COLOR_BUTTON_WIDTH, MAIN_COLOR_BUTTON_HEIGHT)
                .build());
        }
    }

    private void buildBossesTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY;

        addDrawableChild(CheckboxWidget.builder(trText("setting.boss_tracking"), this.textRenderer)
            .pos(x, y).checked(this.mod.isBossTrackingEnabled())
            .callback((box, checked) -> this.mod.setBossTrackingEnabled(checked))
            .build());

        addDrawableChild(CheckboxWidget.builder(trText("setting.miniboss_waypoints"), this.textRenderer)
            .pos(x + 130, y).checked(cfg.minibossTrackingEnabled)
            .callback((box, checked) -> this.mod.setMinibossTrackingEnabled(checked))
            .build());

        addDrawableChild(CheckboxWidget.builder(trText("setting.miniboss_panel"), this.textRenderer)
            .pos(x, y + BOSSES_MINIBOSS_PANEL_Y_OFFSET).checked(this.mod.isMinibossHudEnabled())
            .callback((box, checked) -> this.mod.setMinibossHudEnabled(checked))
            .build());

        // Active bosses list
        int listY = y + BOSSES_ACTIVE_LIST_Y_OFFSET;
        int listHeight = BOSSES_ACTIVE_LIST_HEIGHT;
        addBossActiveButtons(x, listY, listHeight);

        // Registry list
        int regY = listY + listHeight + BOSSES_REGISTRY_GAP;
        int regHeight = BOSSES_REGISTRY_LIST_HEIGHT;
        addBossRegistryButtons(x, regY, regHeight);
    }

    private void addBossActiveButtons(int x, int y, int height) {
        List<TrackedBossRow> rows = buildTrackedBossRows();
        int rowHeight = BOSSES_ACTIVE_ROW_HEIGHT;
        int maxRows = Math.max(1, height / BOSSES_ACTIVE_ROW_STEP);
        if (this.bossActiveOffset >= rows.size()) {
            this.bossActiveOffset = Math.max(0, rows.size() - maxRows);
        }
        int end = Math.min(rows.size(), this.bossActiveOffset + maxRows);
        for (int i = this.bossActiveOffset; i < end; i++) {
            TrackedBossRow row = rows.get(i);
            if (!row.removable()) {
                continue;
            }
            int rowY = y + (i - this.bossActiveOffset) * BOSSES_ACTIVE_ROW_STEP;
            addRemoveButton(x + this.contentWidth - REMOVE_BUTTON_SIZE - REMOVE_BUTTON_GAP_RIGHT, rowY, rowHeight, () -> {
                    if (row.type() == BossRowType.SPAWN_HEADER) {
                        this.mod.getBossTracker().removeSpawn(row.spawnId());
                    } else {
                        this.mod.getBossTracker().removeActiveBossByKey(row.entryKey());
                    }
                    rebuild();
                });
        }
    }

    private void addBossRegistryButtons(int x, int y, int height) {
        List<String> registry = new ArrayList<>(this.mod.getBossTracker().getBossRegistry());
        int rowHeight = BOSSES_REGISTRY_ROW_HEIGHT;
        int maxRows = Math.max(1, height / BOSSES_REGISTRY_ROW_STEP);
        if (this.bossRegistryOffset >= registry.size()) {
            this.bossRegistryOffset = Math.max(0, registry.size() - maxRows);
        }
        int end = Math.min(registry.size(), this.bossRegistryOffset + maxRows);
        for (int i = this.bossRegistryOffset; i < end; i++) {
            String name = registry.get(i);
            int rowY = y + (i - this.bossRegistryOffset) * BOSSES_REGISTRY_ROW_STEP;
            int removeX = x + this.contentWidth - REMOVE_BUTTON_SIZE - REMOVE_BUTTON_GAP_RIGHT;
            int neverWidth = 44;
            int neverX = removeX - 4 - neverWidth;
            int plusWidth = 40;
            int plusX = neverX - 4 - plusWidth;
            addDrawableChild(ButtonWidget.builder(Text.literal("+15m"), b -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    int px = 0, py = 0, pz = 0;
                    if (client != null && client.player != null) {
                        px = (int) Math.floor(client.player.getX());
                        py = (int) Math.floor(client.player.getY());
                        pz = (int) Math.floor(client.player.getZ());
                    }
                    this.mod.getBossTracker().addBossFromRegistry(name, px, py, pz);
                    rebuild();
                })
                .dimensions(plusX, rowY, plusWidth, rowHeight)
                .build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Never"), b -> {
                    this.mod.getBossTracker().ignoreBossForever(name);
                    rebuild();
                })
                .dimensions(neverX, rowY, neverWidth, rowHeight)
                .build());
            addRemoveButton(removeX, rowY, rowHeight, () -> {
                    this.mod.getBossTracker().removeFromRegistry(name);
                    rebuild();
                });
        }

    }

    private void buildMoneyTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY;

        addDrawableChild(CheckboxWidget.builder(trText("setting.money_tracking"), this.textRenderer)
            .pos(x, y).checked(cfg.moneyTrackingEnabled)
            .callback((box, checked) -> this.mod.setMoneyTrackingEnabled(checked))
            .build());

        int fy = y + 26;
        this.moneyTotalField = new TextFieldWidget(this.textRenderer, x + MONEY_VALUE_FIELD_X_OFFSET, fy, MONEY_VALUE_FIELD_WIDTH, 16, Text.literal(""));
        this.moneyTotalField.setText(Long.toString(this.mod.getMoneyTracker().getCurrentBalance()));
        addDrawableChild(this.moneyTotalField);

        this.moneyMadeTodayField = new TextFieldWidget(this.textRenderer, x + MONEY_VALUE_FIELD_X_OFFSET, fy + 22, MONEY_VALUE_FIELD_WIDTH, 16, Text.literal(""));
        this.moneyMadeTodayField.setText(Long.toString(this.mod.getMoneyTracker().getMadeToday()));
        addDrawableChild(this.moneyMadeTodayField);

        this.moneyAhSoldField = new TextFieldWidget(this.textRenderer, x + MONEY_VALUE_FIELD_X_OFFSET, fy + 44, MONEY_VALUE_FIELD_WIDTH, 16, Text.literal(""));
        this.moneyAhSoldField.setText(Long.toString(this.mod.getMoneyTracker().getAhMadeToday()));
        addDrawableChild(this.moneyAhSoldField);

        this.moneyAhBoughtField = new TextFieldWidget(this.textRenderer, x + MONEY_VALUE_FIELD_X_OFFSET, fy + 66, MONEY_VALUE_FIELD_WIDTH, 16, Text.literal(""));
        this.moneyAhBoughtField.setText(Long.toString(this.mod.getMoneyTracker().getAhSpentToday()));
        addDrawableChild(this.moneyAhBoughtField);

        addDrawableChild(ButtonWidget.builder(trText("button.save"), b -> {
                long total = parseLongSafe(this.moneyTotalField.getText());
                long madeToday = parseLongSafe(this.moneyMadeTodayField.getText());
                long ahSold = parseLongSafe(this.moneyAhSoldField.getText());
                long ahBought = parseLongSafe(this.moneyAhBoughtField.getText());
                this.mod.getMoneyTracker().setAhMadeToday(ahSold);
                this.mod.getMoneyTracker().setAhSpentToday(ahBought);
                this.mod.getMoneyTracker().setMadeToday(madeToday);
                this.mod.getMoneyTracker().setCurrentBalance(total);
            })
            .dimensions(x + 178, y + 1, 46, 16)
            .build());

        addDrawableChild(ButtonWidget.builder(trText("button.run_balance"), b -> this.mod.runBalanceCommand())
            .dimensions(x + 228, y + 1, 90, 16)
            .build());

        // Receipts list
        int receiptsY = fy + MONEY_RECEIPTS_LIST_Y_OFFSET;
        int receiptsHeight = this.contentHeight - (receiptsY - this.contentY) - 4;
        int rowHeight = MONEY_RECEIPT_ROW_HEIGHT;
        int maxRows = Math.max(1, receiptsHeight / MONEY_RECEIPT_ROW_STEP);
        List<PersistentState.Transaction> history = this.mod.getMoneyTracker().getHistory();
        int totalSize = history.size();
        if (this.moneyHistoryOffset >= totalSize) {
            this.moneyHistoryOffset = Math.max(0, totalSize - maxRows);
        }
        int displayed = 0;
        for (int i = totalSize - 1 - this.moneyHistoryOffset; i >= 0 && displayed < maxRows; i--, displayed++) {
            PersistentState.Transaction tx = history.get(i);
            String txId = tx.id;
            int rowY = receiptsY + displayed * MONEY_RECEIPT_ROW_STEP;
            addRemoveButton(x + this.contentWidth - REMOVE_BUTTON_SIZE - REMOVE_BUTTON_GAP_RIGHT, rowY, rowHeight, () -> {
                    this.mod.getMoneyTracker().removeTransaction(txId);
                    rebuild();
                });
        }
    }

    private void buildJobsTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY;

        addDrawableChild(CheckboxWidget.builder(trText("setting.jobs_stats"), this.textRenderer)
            .pos(x, y).checked(cfg.jobsTrackingEnabled)
            .callback((box, checked) -> this.mod.setJobsTrackingEnabled(checked))
            .build());

        int rowY = y + JOBS_TABLE_Y_OFFSET;
        for (int i = 0; i < JobsTracker.HUD_JOB_ORDER.size(); i++) {
            String jobName = JobsTracker.HUD_JOB_ORDER.get(i);
            PersistentState.JobInfo info = this.mod.getJobsTracker().getJob(jobName);

            int by = rowY + i * JOBS_ROW_STEP;
            int fieldY = by + (JOBS_ROW_HEIGHT - JOBS_FIELD_HEIGHT) / 2;

            this.jobLevelFields[i] = new TextFieldWidget(this.textRenderer, x + JOBS_LEVEL_X_OFFSET, fieldY, JOBS_LEVEL_WIDTH, JOBS_FIELD_HEIGHT, Text.literal(""));
            this.jobLevelFields[i].setCentered(true);
            this.jobLevelFields[i].setText(info == null ? "0" : Integer.toString(info.level));
            addDrawableChild(this.jobLevelFields[i]);

            this.jobCurrentXpFields[i] = new TextFieldWidget(this.textRenderer, x + JOBS_CUR_X_OFFSET, fieldY, JOBS_CUR_WIDTH, JOBS_FIELD_HEIGHT, Text.literal(""));
            this.jobCurrentXpFields[i].setCentered(true);
            this.jobCurrentXpFields[i].setText(info == null || info.currentXp < 0 ? "0" : formatDouble(info.currentXp));
            addDrawableChild(this.jobCurrentXpFields[i]);

            this.jobNeededXpFields[i] = new TextFieldWidget(this.textRenderer, x + JOBS_NEED_X_OFFSET, fieldY, JOBS_NEED_WIDTH, JOBS_FIELD_HEIGHT, Text.literal(""));
            this.jobNeededXpFields[i].setCentered(true);
            this.jobNeededXpFields[i].setText(info == null ? "0" : formatDouble(info.neededXp));
            addDrawableChild(this.jobNeededXpFields[i]);
        }

        addDrawableChild(ButtonWidget.builder(trText("button.save_jobs"), b -> {
                for (int i = 0; i < JobsTracker.HUD_JOB_ORDER.size(); i++) {
                    String jobName = JobsTracker.HUD_JOB_ORDER.get(i);
                    int level = (int) parseLongSafe(this.jobLevelFields[i].getText());
                    double cur = parseDoubleSafe(this.jobCurrentXpFields[i].getText());
                    double need = parseDoubleSafe(this.jobNeededXpFields[i].getText());
                    this.mod.getJobsTracker().setJobValues(jobName, level, cur, need);
                }
            })
            .dimensions(x, rowY + 4 * JOBS_ROW_STEP + 6, 90, 18)
            .build());
    }

    private void buildEventsTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY;

        addDrawableChild(CheckboxWidget.builder(trText("setting.events_enabled"), this.textRenderer)
            .pos(x, y).checked(cfg.eventsEnabled)
            .callback((box, checked) -> this.mod.setEventsEnabled(checked))
            .build());

        int formY = y + 26;
        this.eventNameField = new TextFieldWidget(this.textRenderer, x, formY, 130, 16, Text.literal(""));
        this.eventNameField.setMaxLength(64);
        this.eventNameField.setPlaceholder(trText("events.name_placeholder"));
        addDrawableChild(this.eventNameField);

        this.eventTimeField = new TextFieldWidget(this.textRenderer, x + 138, formY, 60, 16, Text.literal(""));
        this.eventTimeField.setMaxLength(5);
        this.eventTimeField.setPlaceholder(Text.literal("HH:mm"));
        addDrawableChild(this.eventTimeField);

        if (isEditingEvent()) {
            this.eventNameField.setText(this.editingEventOriginalName);
            this.eventTimeField.setText(this.editingEventOriginalTime);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal(isEditingEvent() ? tr("button.save") : tr("button.add")), b -> {
                if (isEditingEvent()) {
                    this.mod.getEventCountdownTracker().editEvent(
                        this.editingEventOriginalName,
                        this.editingEventOriginalTime,
                        this.eventNameField.getText(),
                        this.eventTimeField.getText()
                    );
                } else {
                    this.mod.getEventCountdownTracker().addEvent(this.eventNameField.getText(), this.eventTimeField.getText());
                }
                clearEventEditState();
                this.eventNameField.setText("");
                this.eventTimeField.setText("");
                rebuild();
            })
            .dimensions(x + 206, formY, 58, 18)
            .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> {
                clearEventEditState();
                this.eventNameField.setText("");
                this.eventTimeField.setText("");
            })
            .dimensions(x + 268, formY, 46, 18)
            .build());

        // Existing events list
        int listY = formY + EVENTS_LIST_START_Y_OFFSET;
        int listHeight = this.contentHeight - (listY - this.contentY) - 4;
        int rowHeight = EVENTS_ROW_HEIGHT;
        int maxRows = Math.max(1, listHeight / EVENTS_ROW_STEP);
        List<PersistentState.ScheduledEvent> events = this.mod.getEventCountdownTracker().getRawEvents();
        int totalSize = events.size();
        if (this.eventsOffset >= totalSize) {
            this.eventsOffset = Math.max(0, totalSize - maxRows);
        }
        int end = Math.min(totalSize, this.eventsOffset + maxRows);
        for (int i = this.eventsOffset; i < end; i++) {
            PersistentState.ScheduledEvent ev = events.get(i);
            String name = ev.name;
            String time = ev.time;
            int rowY = listY + (i - this.eventsOffset) * EVENTS_ROW_STEP;
            int removeX = x + this.contentWidth - REMOVE_BUTTON_SIZE - REMOVE_BUTTON_GAP_RIGHT;
            int editX = removeX - 4 - EVENTS_EDIT_BUTTON_WIDTH;
            int editY = rowY + (rowHeight - EVENTS_EDIT_BUTTON_HEIGHT) / 2;
            addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> {
                    beginEventEdit(name, time);
                })
                .dimensions(editX, editY, EVENTS_EDIT_BUTTON_WIDTH, EVENTS_EDIT_BUTTON_HEIGHT)
                .build());
            addRemoveButton(removeX, rowY, rowHeight, () -> {
                    this.mod.getEventCountdownTracker().removeEvent(name, time);
                    if (isEditingEvent()
                        && this.editingEventOriginalName.equals(name)
                        && this.editingEventOriginalTime.equals(time)) {
                        clearEventEditState();
                    }
                    rebuild();
                });
        }
    }

    private void buildProfileTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY;

        addDrawableChild(CheckboxWidget.builder(trText("setting.profile_stats"), this.textRenderer)
            .pos(x, y).checked(cfg.profileStatsEnabled)
            .callback((box, checked) -> this.mod.setProfileStatsEnabled(checked))
            .build());

        int rowsY = y + PROFILE_ROWS_Y_OFFSET;
        Double[] values = {
            this.mod.getProfileStatsTracker().getHealth(),
            this.mod.getProfileStatsTracker().getStrength(),
            this.mod.getProfileStatsTracker().getDamage(),
            this.mod.getProfileStatsTracker().getCriticalChance(),
            this.mod.getProfileStatsTracker().getCriticalDamage(),
            this.mod.getProfileStatsTracker().getPower(),
            this.mod.getProfileStatsTracker().getEnergy(),
            this.mod.getProfileStatsTracker().getEnergyRegeneration(),
            this.mod.getProfileStatsTracker().getSpeed(),
            this.mod.getProfileStatsTracker().getDexterity(),
            this.mod.getProfileStatsTracker().getDefense(),
            this.mod.getProfileStatsTracker().getRegeneration()
        };

        for (int i = 0; i < PROFILE_FIELD_KEYS.length; i++) {
            int rowY = rowsY + i * PROFILE_ROW_STEP;
            int fieldY = rowY + (PROFILE_ROW_HEIGHT - PROFILE_FIELD_HEIGHT) / 2;
            this.profileStatFields[i] = new TextFieldWidget(this.textRenderer, x + PROFILE_FIELD_X_OFFSET, fieldY, PROFILE_FIELD_WIDTH, PROFILE_FIELD_HEIGHT, Text.literal(""));
            this.profileStatFields[i].setCentered(true);
            this.profileStatFields[i].setText(values[i] == null ? "0" : formatDouble(values[i]));
            addDrawableChild(this.profileStatFields[i]);
        }

        addDrawableChild(ButtonWidget.builder(trText("button.save"), b -> {
                double health = parseDoubleSafe(this.profileStatFields[0].getText());
                double strength = parseDoubleSafe(this.profileStatFields[1].getText());
                double damage = parseDoubleSafe(this.profileStatFields[2].getText());
                double criticalChance = parseDoubleSafe(this.profileStatFields[3].getText());
                double criticalDamage = parseDoubleSafe(this.profileStatFields[4].getText());
                double power = parseDoubleSafe(this.profileStatFields[5].getText());
                double energy = parseDoubleSafe(this.profileStatFields[6].getText());
                double energyRegeneration = parseDoubleSafe(this.profileStatFields[7].getText());
                double speed = parseDoubleSafe(this.profileStatFields[8].getText());
                double dexterity = parseDoubleSafe(this.profileStatFields[9].getText());
                double defense = parseDoubleSafe(this.profileStatFields[10].getText());
                double regeneration = parseDoubleSafe(this.profileStatFields[11].getText());
                this.mod.getProfileStatsTracker().setStats(
                    health,
                    strength,
                    damage,
                    criticalChance,
                    criticalDamage,
                    power,
                    energy,
                    energyRegeneration,
                    speed,
                    dexterity,
                    defense,
                    regeneration
                );
            })
            .dimensions(x + PROFILE_SAVE_BUTTON_X_OFFSET, y + 1, PROFILE_SAVE_BUTTON_WIDTH, 18)
            .build());

        addDrawableChild(ButtonWidget.builder(trText("button.sync_profile"), b -> {
                this.mod.runProfileCommand();
            })
            .dimensions(x + PROFILE_SYNC_BUTTON_X_OFFSET, y + 1, PROFILE_SYNC_BUTTON_WIDTH, 18)
            .build());
    }

    private void buildLanguageTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        List<ChatTranslationManager.LanguageOption> languages = this.mod.getSupportedChatLanguages();
        if (languages.isEmpty()) {
            return;
        }
        syncLanguageSelection(cfg, languages);

        int x = this.contentX;
        int y = this.contentY;

        addDrawableChild(CheckboxWidget.builder(Text.empty(), this.textRenderer)
            .pos(x + LANGUAGE_MASTER_CHECKBOX_X_OFFSET, y + LANGUAGE_MASTER_CHECKBOX_Y_OFFSET).checked(cfg.chatTranslationEnabled)
            .callback((box, checked) -> {
                this.mod.setChatTranslationEnabled(checked);
                enforceTranslationRouteGuard();
                rebuild();
            })
            .build());

        addDrawableChild(CheckboxWidget.builder(Text.empty(), this.textRenderer)
            .pos(x + LANGUAGE_AGGRESSIVE_CHECKBOX_X_OFFSET, y + LANGUAGE_MASTER_CHECKBOX_Y_OFFSET).checked(cfg.chatTranslationAggressiveEnabled)
            .callback((box, checked) -> {
                this.mod.setChatTranslationAggressiveEnabled(checked);
                rebuild();
            })
            .build());

        addDrawableChild(CheckboxWidget.builder(Text.empty(), this.textRenderer)
            .pos(x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET, y + LANGUAGE_PUBLIC_CHECKBOX_Y_OFFSET).checked(cfg.chatTranslationPublicEnabled)
            .callback((box, checked) -> {
                this.mod.setChatTranslationPublicEnabled(checked);
                enforceTranslationRouteGuard();
                rebuild();
            })
            .build());
        addDrawableChild(CheckboxWidget.builder(Text.empty(), this.textRenderer)
            .pos(x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET, y + LANGUAGE_PRIVATE_CHECKBOX_Y_OFFSET).checked(cfg.chatTranslationPrivateEnabled)
            .callback((box, checked) -> {
                this.mod.setChatTranslationPrivateEnabled(checked);
                enforceTranslationRouteGuard();
                rebuild();
            })
            .build());
        addDrawableChild(CheckboxWidget.builder(Text.empty(), this.textRenderer)
            .pos(x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET, y + LANGUAGE_SYSTEM_CHECKBOX_Y_OFFSET).checked(cfg.chatTranslationSystemEnabled)
            .callback((box, checked) -> {
                this.mod.setChatTranslationSystemEnabled(checked);
                enforceTranslationRouteGuard();
                rebuild();
            })
            .build());

        int fromRowY = y + LANGUAGE_FROM_ROW_Y_OFFSET;
        int toRowY = y + LANGUAGE_TO_ROW_Y_OFFSET;
        int fromArrowY = fromRowY + (LANGUAGE_SELECTOR_ROW_HEIGHT - LANGUAGE_SELECTOR_BUTTON_HEIGHT) / 2;
        int toArrowY = toRowY + (LANGUAGE_SELECTOR_ROW_HEIGHT - LANGUAGE_SELECTOR_BUTTON_HEIGHT) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                this.translationSourceIndex = Math.floorMod(this.translationSourceIndex - 1, languages.size());
                rebuild();
            })
            .dimensions(x + LANGUAGE_SELECTOR_LEFT_BUTTON_X_OFFSET, fromArrowY, LANGUAGE_SELECTOR_BUTTON_WIDTH, LANGUAGE_SELECTOR_BUTTON_HEIGHT)
            .build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                this.translationSourceIndex = Math.floorMod(this.translationSourceIndex + 1, languages.size());
                rebuild();
            })
            .dimensions(x + LANGUAGE_SELECTOR_RIGHT_BUTTON_X_OFFSET, fromArrowY, LANGUAGE_SELECTOR_BUTTON_WIDTH, LANGUAGE_SELECTOR_BUTTON_HEIGHT)
            .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                this.translationTargetIndex = Math.floorMod(this.translationTargetIndex - 1, languages.size());
                rebuild();
            })
            .dimensions(x + LANGUAGE_SELECTOR_LEFT_BUTTON_X_OFFSET, toArrowY, LANGUAGE_SELECTOR_BUTTON_WIDTH, LANGUAGE_SELECTOR_BUTTON_HEIGHT)
            .build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                this.translationTargetIndex = Math.floorMod(this.translationTargetIndex + 1, languages.size());
                rebuild();
            })
            .dimensions(x + LANGUAGE_SELECTOR_RIGHT_BUTTON_X_OFFSET, toArrowY, LANGUAGE_SELECTOR_BUTTON_WIDTH, LANGUAGE_SELECTOR_BUTTON_HEIGHT)
            .build());

        addDrawableChild(ButtonWidget.builder(trText("button.add_rule"), b -> {
                ChatTranslationManager.LanguageOption source = languages.get(this.translationSourceIndex);
                ChatTranslationManager.LanguageOption target = languages.get(this.translationTargetIndex);
                this.mod.addChatTranslationRule(source.code(), target.code());
                rebuild();
            })
            .dimensions(x + this.contentWidth - LANGUAGE_ADD_RULE_WIDTH, y + LANGUAGE_ADD_RULE_Y_OFFSET, LANGUAGE_ADD_RULE_WIDTH, LANGUAGE_ADD_RULE_HEIGHT)
            .build());

        int listY = y + LANGUAGE_RULES_LIST_Y_OFFSET;
        int listHeight = this.contentHeight - (listY - this.contentY) - 4;
        int maxRows = Math.max(1, listHeight / LANGUAGE_RULE_ROW_STEP);
        int totalSize = cfg.chatTranslationRules.size();
        if (this.translationRulesOffset >= totalSize) {
            this.translationRulesOffset = Math.max(0, totalSize - maxRows);
        }
        int end = Math.min(totalSize, this.translationRulesOffset + maxRows);
        for (int i = this.translationRulesOffset; i < end; i++) {
            ConfigManager.ModConfig.ChatTranslationRule rule = cfg.chatTranslationRules.get(i);
            int rowY = listY + (i - this.translationRulesOffset) * LANGUAGE_RULE_ROW_STEP;
            int index = i;
            int toggleY = rowY + (LANGUAGE_RULE_ROW_HEIGHT - LANGUAGE_RULE_TOGGLE_HEIGHT) / 2;
            addDrawableChild(ButtonWidget.builder(Text.literal(rule.enabled ? tr("button.on") : tr("button.off")), b -> {
                    this.mod.setChatTranslationRuleEnabled(index, !rule.enabled);
                    rebuild();
                })
                .dimensions(x + this.contentWidth - 64, toggleY, LANGUAGE_RULE_TOGGLE_WIDTH, LANGUAGE_RULE_TOGGLE_HEIGHT)
                .build());
            addRemoveButton(x + this.contentWidth - REMOVE_BUTTON_SIZE - REMOVE_BUTTON_GAP_RIGHT, rowY, LANGUAGE_RULE_ROW_HEIGHT, () -> {
                    this.mod.removeChatTranslationRule(index);
                    rebuild();
                });
        }

        if (totalSize > maxRows) {
            addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> {
                    this.translationRulesOffset = Math.max(0, this.translationRulesOffset - maxRows);
                    rebuild();
                })
                .dimensions(x + this.contentWidth - 96, listY, 20, 12)
                .build());
            addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> {
                    if (this.translationRulesOffset + maxRows < totalSize) {
                        this.translationRulesOffset += maxRows;
                    }
                    rebuild();
                })
                .dimensions(x + this.contentWidth - 96, listY + 14, 20, 12)
                .build());
        }
    }

    private void syncLanguageSelection(ConfigManager.ModConfig cfg, List<ChatTranslationManager.LanguageOption> languages) {
        this.translationSourceIndex = clampLanguageIndex(this.translationSourceIndex, languages.size());
        this.translationTargetIndex = clampLanguageIndex(this.translationTargetIndex, languages.size());
        if (this.translationSelectionInitialized || cfg.chatTranslationRules.isEmpty()) {
            return;
        }
        ConfigManager.ModConfig.ChatTranslationRule fallbackRule = cfg.chatTranslationRules.get(0);
        if (fallbackRule == null) {
            return;
        }
        int source = findLanguageIndex(languages, fallbackRule.sourceLanguage);
        int target = findLanguageIndex(languages, fallbackRule.targetLanguage);
        if (source >= 0) {
            this.translationSourceIndex = source;
        }
        if (target >= 0) {
            this.translationTargetIndex = target;
        }
        this.translationSelectionInitialized = true;
    }

    private static int clampLanguageIndex(int index, int size) {
        if (size <= 0) {
            return 0;
        }
        if (index < 0 || index >= size) {
            return 0;
        }
        return index;
    }

    private static int findLanguageIndex(List<ChatTranslationManager.LanguageOption> languages, String code) {
        String normalized = ChatTranslationManager.normalizeLanguageCode(code);
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).code().equals(normalized)) {
                return i;
            }
        }
        return -1;
    }

    private void buildOtherTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY;

        addDrawableChild(CheckboxWidget.builder(trText("setting.auction_highlight"), this.textRenderer)
            .pos(x, y).checked(cfg.auctionHighlightEnabled)
            .callback((box, checked) -> this.mod.setAuctionHighlightEnabled(checked))
            .build());

        addDrawableChild(CheckboxWidget.builder(trText("setting.haki_cooldown"), this.textRenderer)
            .pos(x, y + 24).checked(cfg.hakiEnabled)
            .callback((box, checked) -> this.mod.setHakiEnabled(checked))
            .build());

        addDrawableChild(CheckboxWidget.builder(trText("setting.rarity_icons"), this.textRenderer)
            .pos(x, y + 48).checked(cfg.rarityIconsEnabled)
            .callback((box, checked) -> this.mod.setRarityIconsEnabled(checked))
            .build());

        addDrawableChild(CheckboxWidget.builder(trText("setting.pet_icons"), this.textRenderer)
            .pos(x, y + 72).checked(cfg.petStatIconsEnabled)
            .callback((box, checked) -> this.mod.setPetStatIconsEnabled(checked))
            .build());

        addDrawableChild(CheckboxWidget.builder(trText("setting.scrolls_hud"), this.textRenderer)
            .pos(x, y + 96).checked(cfg.scrollsEnabled)
            .callback((box, checked) -> this.mod.setScrollsEnabled(checked))
            .build());

        addDrawableChild(CheckboxWidget.builder(trText("setting.xp_hud"), this.textRenderer)
            .pos(x, y + 120).checked(cfg.inventoryXpHudEnabled)
            .callback((box, checked) -> this.mod.setInventoryXpHudEnabled(checked))
            .build());
    }

    private void rebuild() {
        buildTabContent();
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float deltaTicks) {
        // Dark transparent overlay
        drawContext.fill(0, 0, this.width, this.height, COLOR_BG_OVERLAY);

        // Sapphire panel
        drawSapphirePanel(drawContext);

        // Sidebar background
        drawContext.fill(this.panelX + 4, this.panelY + 28, this.panelX + SIDEBAR_WIDTH, this.panelY + PANEL_HEIGHT - 4, COLOR_SIDEBAR_BG);
        drawDivider(drawContext, this.panelX + SIDEBAR_WIDTH + 1, this.panelY + 28, this.panelX + SIDEBAR_WIDTH + 2, this.panelY + PANEL_HEIGHT - 4);

        // Section title
        drawContext.drawCenteredTextWithShadow(this.textRenderer, this.currentTab.label(this), this.panelX + PANEL_WIDTH / 2, this.panelY + 10, COLOR_HEADER);

        // Sidebar tabs
        for (TabButton button : this.tabButtons) {
            button.render(drawContext, mouseX, mouseY);
        }

        // Close (X) button top-right
        drawCloseButton(drawContext, mouseX, mouseY);

        drawContext.fill(this.contentX, this.panelY + 30, this.contentX + this.contentWidth, this.panelY + 31, COLOR_DIVIDER);

        // Render tab-specific decorations (labels for fields, etc.) before widgets
        renderTabDecorations(drawContext, mouseX, mouseY);

        // Widgets
        super.render(drawContext, mouseX, mouseY, deltaTicks);

        // Restyle default buttons and then custom remove buttons
        drawStyledVanillaButtons(drawContext, mouseX, mouseY);
        drawRemoveButtons(drawContext, mouseX, mouseY);

        // Credits bottom right
        String credits = tr("menu.credits");
        int cw = this.textRenderer.getWidth(credits);
        drawContext.drawTextWithShadow(this.textRenderer, credits, this.panelX + PANEL_WIDTH - cw - 8, this.panelY + PANEL_HEIGHT - 12, COLOR_TEXT_DIM);

        if (this.resetHudLayoutConfirmOpen) {
            drawResetHudConfirmPopup(drawContext, mouseX, mouseY);
        }
    }

    private void renderTabDecorations(DrawContext drawContext, int mouseX, int mouseY) {
        TextRenderer renderer = this.textRenderer;
        int x = this.contentX;
        switch (this.currentTab) {
            case MAIN -> {
                int compactShift = this.mod.isDebugToolsAvailable() ? 0 : MAIN_COMPACT_SHIFT_NO_DEBUG;
                int languageRowY = this.contentY + 8 + MAIN_LANGUAGE_ROW_Y_OFFSET + compactShift;
                List<UiLocalization.LanguageOption> uiLanguages = this.mod.getSupportedUiLanguages();
                if (!uiLanguages.isEmpty()) {
                    int idx = clampLanguageIndex(this.uiLanguageIndex, uiLanguages.size());
                    int labelBoxWidth = MAIN_COLOR_LEFT_BUTTON_X_OFFSET - 8;
                    int valueBoxX = x + MAIN_COLOR_LEFT_BUTTON_X_OFFSET + MAIN_COLOR_BUTTON_WIDTH + 4;
                    int valueBoxWidth = MAIN_COLOR_RIGHT_BUTTON_X_OFFSET - (MAIN_COLOR_LEFT_BUTTON_X_OFFSET + MAIN_COLOR_BUTTON_WIDTH) - 8;
                    drawRowBox(drawContext, x, languageRowY, this.contentWidth - 4, LIST_ROW_HEIGHT);
                    drawFittedCenteredText(drawContext, this.tr("main.mod_language"), x + 2, languageRowY, labelBoxWidth, LIST_ROW_HEIGHT, COLOR_TEXT);
                    drawFittedCenteredText(drawContext, uiLanguages.get(idx).label(), valueBoxX, languageRowY, valueBoxWidth, LIST_ROW_HEIGHT, COLOR_HEADER);
                }
                drawHudColorSection(drawContext, x, this.contentY + 8 + MAIN_PANEL_COLORS_Y_OFFSET + compactShift);
            }
            case BOSSES -> {
                int activeListY = this.contentY + BOSSES_ACTIVE_LIST_Y_OFFSET;
                int activeListBottom = activeListY + BOSSES_ACTIVE_LIST_HEIGHT;
                int registryListY = activeListBottom + BOSSES_REGISTRY_GAP;
                drawContext.drawTextWithShadow(renderer, this.tr("bosses.tracked_by_spawn"), x, this.contentY + BOSSES_ACTIVE_LABEL_Y_OFFSET, COLOR_TEXT);
                drawBossActiveLabels(drawContext, x, activeListY);
                drawContext.drawTextWithShadow(renderer, this.tr("bosses.registry"), x, registryListY - BOSSES_REGISTRY_LABEL_OFFSET, COLOR_TEXT);
                drawBossRegistryLabels(drawContext, x, registryListY);
            }
            case MONEY -> {
                int fy = this.contentY + 26;
                int labelBoxWidth = MONEY_VALUE_FIELD_X_OFFSET - 4;
                drawRowBox(drawContext, x, fy, this.contentWidth - 4, LIST_ROW_HEIGHT);
                drawRowBox(drawContext, x, fy + LIST_ROW_STEP, this.contentWidth - 4, LIST_ROW_HEIGHT);
                drawRowBox(drawContext, x, fy + LIST_ROW_STEP * 2, this.contentWidth - 4, LIST_ROW_HEIGHT);
                drawRowBox(drawContext, x, fy + LIST_ROW_STEP * 3, this.contentWidth - 4, LIST_ROW_HEIGHT);
                drawFittedCenteredText(drawContext, this.tr("money.total"), x + 2, fy, labelBoxWidth, LIST_ROW_HEIGHT, COLOR_TEXT);
                drawFittedCenteredText(drawContext, this.tr("money.made_today"), x + 2, fy + LIST_ROW_STEP, labelBoxWidth, LIST_ROW_HEIGHT, COLOR_TEXT);
                drawFittedCenteredText(drawContext, this.tr("money.ah_sold"), x + 2, fy + LIST_ROW_STEP * 2, labelBoxWidth, LIST_ROW_HEIGHT, COLOR_TEXT);
                drawFittedCenteredText(drawContext, this.tr("money.ah_bought"), x + 2, fy + LIST_ROW_STEP * 3, labelBoxWidth, LIST_ROW_HEIGHT, COLOR_TEXT);
                drawContext.drawTextWithShadow(renderer, this.tr("money.receipts_latest"), x, fy + MONEY_RECEIPTS_TITLE_Y_OFFSET, COLOR_HEADER);
                drawMoneyHistoryLabels(drawContext, x, fy + MONEY_RECEIPTS_LIST_Y_OFFSET);
            }
            case JOBS -> {
                int rowY = this.contentY + JOBS_TABLE_Y_OFFSET;
                int headerY = rowY - JOBS_ROW_STEP;
                int jobColWidth = JOBS_LEVEL_X_OFFSET - 6;
                drawRowBox(drawContext, x, headerY, this.contentWidth - 4, JOBS_ROW_HEIGHT);
                drawFittedCenteredText(drawContext, this.tr("jobs.job"), x + 2, headerY, jobColWidth, JOBS_ROW_HEIGHT, COLOR_HEADER);
                drawFittedCenteredText(drawContext, this.tr("jobs.level"), x + JOBS_LEVEL_X_OFFSET, headerY, JOBS_LEVEL_WIDTH, JOBS_ROW_HEIGHT, COLOR_HEADER);
                drawFittedCenteredText(drawContext, this.tr("jobs.cur_xp"), x + JOBS_CUR_X_OFFSET, headerY, JOBS_CUR_WIDTH, JOBS_ROW_HEIGHT, COLOR_HEADER);
                drawFittedCenteredText(drawContext, this.tr("jobs.need_xp"), x + JOBS_NEED_X_OFFSET, headerY, JOBS_NEED_WIDTH, JOBS_ROW_HEIGHT, COLOR_HEADER);
                for (int i = 0; i < JobsTracker.HUD_JOB_ORDER.size(); i++) {
                    String jobName = JobsTracker.HUD_JOB_ORDER.get(i);
                    int by = rowY + i * JOBS_ROW_STEP;
                    drawRowBox(drawContext, x, by, this.contentWidth - 4, JOBS_ROW_HEIGHT);
                    drawFittedCenteredText(drawContext, capitalize(jobName), x + 2, by, jobColWidth, JOBS_ROW_HEIGHT, COLOR_TEXT);
                }
            }
            case EVENTS -> {
                int formY = this.contentY + 26;
                drawRowBox(drawContext, x, formY, this.contentWidth - 4, LIST_ROW_HEIGHT);
                drawContext.drawTextWithShadow(renderer, this.tr("events.existing"), x, formY + EVENTS_LIST_TITLE_Y_OFFSET, COLOR_HEADER);
                drawEventsLabels(drawContext, x, formY + EVENTS_LIST_START_Y_OFFSET);
            }
            case PROFILE -> {
                int rowsY = this.contentY + PROFILE_ROWS_Y_OFFSET;
                int labelBoxWidth = PROFILE_FIELD_X_OFFSET - 4;
                for (int i = 0; i < PROFILE_FIELD_KEYS.length; i++) {
                    int rowY = rowsY + i * PROFILE_ROW_STEP;
                    drawRowBox(drawContext, x, rowY, this.contentWidth - 4, PROFILE_ROW_HEIGHT);
                    drawFittedCenteredText(drawContext, profileFieldLabel(i) + ":", x + 2, rowY, labelBoxWidth, PROFILE_ROW_HEIGHT, COLOR_TEXT);
                }
            }
            case LANGUAGE -> {
                List<ChatTranslationManager.LanguageOption> languages = this.mod.getSupportedChatLanguages();
                if (languages.isEmpty()) {
                    drawContext.drawTextWithShadow(renderer, this.tr("language.no_languages"), x, this.contentY + 24, COLOR_TEXT_DIM);
                } else {
                    drawContext.drawTextWithShadow(renderer, this.tr("setting.chat_translate"), x + LANGUAGE_MASTER_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_MASTER_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
                    drawContext.drawTextWithShadow(renderer, this.tr("setting.translate_aggressive"), x + LANGUAGE_AGGRESSIVE_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_MASTER_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
                    drawContext.drawTextWithShadow(renderer, this.tr("setting.translate_public"), x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_PUBLIC_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
                    drawContext.drawTextWithShadow(renderer, this.tr("setting.translate_private"), x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_PRIVATE_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
                    drawContext.drawTextWithShadow(renderer, this.tr("setting.translate_system"), x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_SYSTEM_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
                    ConfigManager.ModConfig cfg = this.mod.getConfig();
                    if (!hasAnyTranslationRoutesEnabled(cfg)) {
                        drawFittedText(
                            drawContext,
                            this.tr("language.routes_disabled_warning"),
                            x,
                            this.contentY + 84,
                            this.contentWidth - 8,
                            0xFFFFC78A
                        );
                    }
                    int source = clampLanguageIndex(this.translationSourceIndex, languages.size());
                    int target = clampLanguageIndex(this.translationTargetIndex, languages.size());
                    int fromRowY = this.contentY + LANGUAGE_FROM_ROW_Y_OFFSET;
                    int toRowY = this.contentY + LANGUAGE_TO_ROW_Y_OFFSET;
                    int selectorLabelBoxWidth = LANGUAGE_SELECTOR_LEFT_BUTTON_X_OFFSET - 4;
                    int selectorValueBoxWidth = LANGUAGE_SELECTOR_RIGHT_BUTTON_X_OFFSET - LANGUAGE_SELECTOR_VALUE_X_OFFSET;
                    drawRowBox(drawContext, x, fromRowY, this.contentWidth - 4, LANGUAGE_SELECTOR_ROW_HEIGHT);
                    drawRowBox(drawContext, x, toRowY, this.contentWidth - 4, LANGUAGE_SELECTOR_ROW_HEIGHT);
                    drawFittedCenteredText(drawContext, this.tr("language.from"), x + 2, fromRowY, selectorLabelBoxWidth, LANGUAGE_SELECTOR_ROW_HEIGHT, COLOR_TEXT);
                    drawFittedCenteredText(drawContext, languages.get(source).label(), x + LANGUAGE_SELECTOR_VALUE_X_OFFSET, fromRowY, selectorValueBoxWidth, LANGUAGE_SELECTOR_ROW_HEIGHT, COLOR_HEADER);
                    drawFittedCenteredText(drawContext, this.tr("language.to"), x + 2, toRowY, selectorLabelBoxWidth, LANGUAGE_SELECTOR_ROW_HEIGHT, COLOR_TEXT);
                    drawFittedCenteredText(drawContext, languages.get(target).label(), x + LANGUAGE_SELECTOR_VALUE_X_OFFSET, toRowY, selectorValueBoxWidth, LANGUAGE_SELECTOR_ROW_HEIGHT, COLOR_HEADER);
                    drawContext.drawTextWithShadow(renderer, this.tr("language.active_rules"), x, this.contentY + LANGUAGE_RULES_TITLE_Y_OFFSET, COLOR_HEADER);
                    drawTranslationRuleLabels(drawContext, x, this.contentY + LANGUAGE_RULES_LIST_Y_OFFSET);
                }
            }
            case AUCTION_HOUSE -> {
                int boxY = this.contentY + 154;
                drawRowBox(drawContext, x, boxY, this.contentWidth - 4, LIST_ROW_HEIGHT);
                drawRowBox(drawContext, x, boxY + LIST_ROW_STEP, this.contentWidth - 4, LIST_ROW_HEIGHT);
                drawRowBox(drawContext, x, boxY + LIST_ROW_STEP * 2, this.contentWidth - 4, LIST_ROW_HEIGHT);
                drawFittedText(drawContext, this.tr("other.desc1"), x + LIST_TEXT_INSET, boxY + LIST_TEXT_BASELINE_OFFSET, this.contentWidth - 10, COLOR_TEXT_DIM);
                drawFittedText(drawContext, this.tr("other.desc2"), x + LIST_TEXT_INSET, boxY + LIST_ROW_STEP + LIST_TEXT_BASELINE_OFFSET, this.contentWidth - 10, COLOR_TEXT_DIM);
                drawFittedText(drawContext, this.tr("other.desc3"), x + LIST_TEXT_INSET, boxY + LIST_ROW_STEP * 2 + LIST_TEXT_BASELINE_OFFSET, this.contentWidth - 10, COLOR_TEXT_DIM);
            }
        }
    }

    private void drawBossActiveLabels(DrawContext drawContext, int x, int y) {
        List<TrackedBossRow> rows = buildTrackedBossRows();
        int maxRows = Math.max(1, BOSSES_ACTIVE_LIST_HEIGHT / BOSSES_ACTIVE_ROW_STEP);
        if (rows.isEmpty()) {
            drawRowBox(drawContext, x, y, this.contentWidth - 4, BOSSES_ACTIVE_ROW_HEIGHT);
            drawFittedCenteredText(drawContext, tr("bosses.none"), x + 2, y, this.contentWidth - 4, BOSSES_ACTIVE_ROW_HEIGHT, COLOR_TEXT_DIM);
            return;
        }
        int textBoxX = x + 2;
        int textBoxWidth = this.contentWidth - REMOVE_BUTTON_SIZE - 10;
        int end = Math.min(rows.size(), this.bossActiveOffset + maxRows);
        for (int i = this.bossActiveOffset; i < end; i++) {
            TrackedBossRow row = rows.get(i);
            int rowY = y + (i - this.bossActiveOffset) * BOSSES_ACTIVE_ROW_STEP;
            drawRowBox(drawContext, x, rowY, this.contentWidth - 4, BOSSES_ACTIVE_ROW_HEIGHT);
            int color = switch (row.type()) {
                case SPAWN_HEADER -> COLOR_HEADER;
                case MINIBOSS_HEADER -> 0xFFB7E8C2;
                case MINIBOSS_ENTRY -> 0xFFD0F2D8;
                case BOSS_ENTRY -> COLOR_TEXT;
            };
            drawFittedCenteredText(drawContext, row.label(), textBoxX, rowY, textBoxWidth, BOSSES_ACTIVE_ROW_HEIGHT, color);
        }
    }

    private List<TrackedBossRow> buildTrackedBossRows() {
        List<Map.Entry<String, PersistentState.BossSpawnState>> entries = this.mod.getBossTracker().getActiveEntries();
        if (entries.isEmpty()) {
            return List.of();
        }

        String currentSpawnRaw = normalizedSpawnKey(this.mod.getBossTracker().getCurrentSpawnId());
        Map<String, List<Map.Entry<String, PersistentState.BossSpawnState>>> bossesBySpawn = new LinkedHashMap<>();
        List<Map.Entry<String, PersistentState.BossSpawnState>> minibosses = new ArrayList<>();
        for (Map.Entry<String, PersistentState.BossSpawnState> entry : entries) {
            if (entry.getValue().miniboss) {
                minibosses.add(entry);
                continue;
            }
            bossesBySpawn.computeIfAbsent(normalizedSpawnKey(entry.getValue().spawnId), key -> new ArrayList<>()).add(entry);
        }

        List<String> orderedSpawns = new ArrayList<>(bossesBySpawn.keySet());
        if (!currentSpawnRaw.isBlank() && !bossesBySpawn.containsKey(currentSpawnRaw)) {
            orderedSpawns.add(currentSpawnRaw);
            bossesBySpawn.put(currentSpawnRaw, new ArrayList<>());
        }
        orderedSpawns.sort((a, b) -> this.compareSpawnIds(a, b, currentSpawnRaw));

        long now = System.currentTimeMillis();
        List<TrackedBossRow> rows = new ArrayList<>();
        for (String spawnKey : orderedSpawns) {
            String displaySpawn = displaySpawnId(spawnKey);
            boolean isCurrent = !currentSpawnRaw.isBlank() && spawnKey.equals(currentSpawnRaw);
            rows.add(TrackedBossRow.spawnHeader(
                tr("bosses.spawn") + " " + displaySpawn + (isCurrent ? " (" + tr("bosses.current") + ")" : ""),
                spawnKey
            ));
            List<Map.Entry<String, PersistentState.BossSpawnState>> spawnEntries = bossesBySpawn.get(spawnKey);
            if (spawnEntries.isEmpty()) {
                rows.add(TrackedBossRow.bossInfo("  - " + tr("bosses.none_tracked")));
            }
            for (Map.Entry<String, PersistentState.BossSpawnState> entry : spawnEntries) {
                PersistentState.BossSpawnState state = entry.getValue();
                String timer = formatTrackedTimer(state.nextSpawnEpochMs, now);
                String name = state.bossName == null || state.bossName.isBlank() ? "<unnamed>" : state.bossName;
                rows.add(TrackedBossRow.bossEntry("  - " + name + " - " + timer, entry.getKey()));
            }
        }

        if (!minibosses.isEmpty()) {
            minibosses.sort(Comparator
                .comparing((Map.Entry<String, PersistentState.BossSpawnState> entry) -> displaySpawnId(entry.getValue().spawnId).toLowerCase(Locale.ROOT))
                .thenComparingLong(entry -> entry.getValue().nextSpawnEpochMs)
                .thenComparing(entry -> normalizeName(entry.getValue().bossName)));
            rows.add(TrackedBossRow.minibossHeader(tr("bosses.minibosses")));
            for (Map.Entry<String, PersistentState.BossSpawnState> entry : minibosses) {
                PersistentState.BossSpawnState state = entry.getValue();
                String spawn = displaySpawnId(state.spawnId);
                String timer = formatTrackedTimer(state.nextSpawnEpochMs, now);
                String name = state.bossName == null || state.bossName.isBlank() ? tr("bosses.minibosses") : state.bossName;
                rows.add(TrackedBossRow.minibossEntry("  - [" + spawn + "] " + name + " - " + timer, entry.getKey()));
            }
        }
        return rows;
    }

    private int compareSpawnIds(String a, String b, String currentSpawnRaw) {
        boolean aCurrent = !currentSpawnRaw.isBlank() && a.equals(currentSpawnRaw);
        boolean bCurrent = !currentSpawnRaw.isBlank() && b.equals(currentSpawnRaw);
        if (aCurrent && !bCurrent) {
            return -1;
        }
        if (!aCurrent && bCurrent) {
            return 1;
        }
        boolean aUnknown = "__global__".equals(a);
        boolean bUnknown = "__global__".equals(b);
        if (aUnknown && !bUnknown) {
            return 1;
        }
        if (!aUnknown && bUnknown) {
            return -1;
        }
        return displaySpawnId(a).compareToIgnoreCase(displaySpawnId(b));
    }

    private static String normalizedSpawnKey(String spawnId) {
        if (spawnId == null || spawnId.isBlank() || "__global__".equals(spawnId)) {
            return "__global__";
        }
        return spawnId.trim();
    }

    private String displaySpawnId(String spawnId) {
        if (spawnId == null || spawnId.isBlank() || "__global__".equals(spawnId)) {
            return tr("common.unknown").toLowerCase(Locale.ROOT);
        }
        return spawnId.trim();
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String formatTrackedTimer(long nextSpawnEpochMs, long now) {
        if (nextSpawnEpochMs <= 0L) {
            return tr("common.unknown");
        }
        long remain = nextSpawnEpochMs - now;
        return remain <= 0L ? tr("common.ready") : formatTimer(remain);
    }

    private void drawBossRegistryLabels(DrawContext drawContext, int x, int y) {
        List<String> registry = new ArrayList<>(this.mod.getBossTracker().getBossRegistry());
        int maxRows = Math.max(1, BOSSES_REGISTRY_LIST_HEIGHT / BOSSES_REGISTRY_ROW_STEP);
        if (registry.isEmpty()) {
            drawRowBox(drawContext, x, y, this.contentWidth - 4, BOSSES_REGISTRY_ROW_HEIGHT);
            drawFittedCenteredText(drawContext, tr("bosses.empty_registry"), x + 2, y, this.contentWidth - 4, BOSSES_REGISTRY_ROW_HEIGHT, COLOR_TEXT_DIM);
            return;
        }
        int end = Math.min(registry.size(), this.bossRegistryOffset + maxRows);
        for (int i = this.bossRegistryOffset; i < end; i++) {
            int rowY = y + (i - this.bossRegistryOffset) * BOSSES_REGISTRY_ROW_STEP;
            drawRowBox(drawContext, x, rowY, this.contentWidth - 4, BOSSES_REGISTRY_ROW_HEIGHT);
            int textBoxWidth = this.contentWidth - 122;
            drawFittedCenteredText(drawContext, registry.get(i), x + 2, rowY, textBoxWidth, BOSSES_REGISTRY_ROW_HEIGHT, COLOR_TEXT);
        }
    }

    private void drawMoneyHistoryLabels(DrawContext drawContext, int x, int y) {
        List<PersistentState.Transaction> history = this.mod.getMoneyTracker().getHistory();
        int receiptsHeight = this.contentHeight - (y - this.contentY) - 4;
        int maxRows = Math.max(1, receiptsHeight / MONEY_RECEIPT_ROW_STEP);
        int totalSize = history.size();
        if (totalSize == 0) {
            drawRowBox(drawContext, x, y, this.contentWidth - 4, MONEY_RECEIPT_ROW_HEIGHT);
            drawFittedCenteredText(drawContext, tr("money.no_receipts"), x + 2, y, this.contentWidth - 4, MONEY_RECEIPT_ROW_HEIGHT, COLOR_TEXT_DIM);
            return;
        }
        int displayed = 0;
        for (int i = totalSize - 1 - this.moneyHistoryOffset; i >= 0 && displayed < maxRows; i--, displayed++) {
            PersistentState.Transaction tx = history.get(i);
            int rowY = y + displayed * MONEY_RECEIPT_ROW_STEP;
            drawRowBox(drawContext, x, rowY, this.contentWidth - 4, MONEY_RECEIPT_ROW_HEIGHT);
            String sign = "SELL".equalsIgnoreCase(tx.type) ? "+" : "-";
            String amt = String.format(Locale.ROOT, "%,d", Math.round(tx.amount));
            String label = sign + amt + " " + (tx.itemName == null ? "" : tx.itemName);
            int color = "SELL".equalsIgnoreCase(tx.type) ? 0xFF80E082 : 0xFFE08080;
            int textBoxWidth = this.contentWidth - REMOVE_BUTTON_SIZE - 10;
            drawFittedCenteredText(drawContext, label, x + 2, rowY, textBoxWidth, MONEY_RECEIPT_ROW_HEIGHT, color);
        }
    }

    private void drawHudColorSection(DrawContext drawContext, int x, int y) {
        drawContext.drawTextWithShadow(this.textRenderer, tr("main.panel_colors"), x, y, COLOR_HEADER);
        int labelBoxWidth = MAIN_COLOR_LEFT_BUTTON_X_OFFSET - 8;
        int valueBoxX = x + MAIN_COLOR_LEFT_BUTTON_X_OFFSET + MAIN_COLOR_BUTTON_WIDTH + 4;
        int valueBoxWidth = MAIN_COLOR_RIGHT_BUTTON_X_OFFSET - (MAIN_COLOR_LEFT_BUTTON_X_OFFSET + MAIN_COLOR_BUTTON_WIDTH) - 8;
        for (int i = 0; i < HUD_COLOR_PANEL_IDS.length; i++) {
            int panelId = HUD_COLOR_PANEL_IDS[i];
            int rowY = y + 16 + i * MAIN_COLOR_ROW_STEP;
            drawRowBox(drawContext, x, rowY, this.contentWidth - 4, MAIN_COLOR_ROW_HEIGHT);
            String panelName = this.mod.getHudPanelName(panelId);
            String color = prettyColorName(this.mod.getHudPanelColor(panelId));
            drawFittedCenteredText(drawContext, panelName, x + 2, rowY, labelBoxWidth, MAIN_COLOR_ROW_HEIGHT, COLOR_TEXT);
            drawFittedCenteredText(drawContext, color, valueBoxX, rowY, valueBoxWidth, MAIN_COLOR_ROW_HEIGHT, COLOR_HEADER);
        }
    }

    private void drawEventsLabels(DrawContext drawContext, int x, int y) {
        List<PersistentState.ScheduledEvent> events = this.mod.getEventCountdownTracker().getRawEvents();
        int listHeight = this.contentHeight - (y - this.contentY) - 4;
        int maxRows = Math.max(1, listHeight / EVENTS_ROW_STEP);
        int totalSize = events.size();
        if (totalSize == 0) {
            drawRowBox(drawContext, x, y, this.contentWidth - 4, EVENTS_ROW_HEIGHT);
            drawContext.drawTextWithShadow(this.textRenderer, tr("bosses.none"), x, y + 2, COLOR_TEXT_DIM);
            return;
        }
        int end = Math.min(totalSize, this.eventsOffset + maxRows);
        for (int i = this.eventsOffset; i < end; i++) {
            PersistentState.ScheduledEvent ev = events.get(i);
            int rowY = y + (i - this.eventsOffset) * EVENTS_ROW_STEP;
            drawRowBox(drawContext, x, rowY, this.contentWidth - 4, EVENTS_ROW_HEIGHT);
            int maxWidth = this.contentWidth - (EVENTS_EDIT_BUTTON_WIDTH + REMOVE_BUTTON_SIZE + 14);
            drawFittedText(drawContext, ev.time + " - " + ev.name, x + LIST_TEXT_INSET, rowY + LIST_TEXT_BASELINE_OFFSET, maxWidth, COLOR_TEXT);
        }
    }

    private void drawTranslationRuleLabels(DrawContext drawContext, int x, int y) {
        List<ConfigManager.ModConfig.ChatTranslationRule> rules = this.mod.getConfig().chatTranslationRules;
        int listHeight = this.contentHeight - (y - this.contentY) - 4;
        int maxRows = Math.max(1, listHeight / LANGUAGE_RULE_ROW_STEP);
        int totalSize = rules.size();
        if (totalSize == 0) {
            drawRowBox(drawContext, x, y, this.contentWidth - 4, LANGUAGE_RULE_ROW_HEIGHT);
            drawFittedCenteredText(drawContext, tr("language.none_yet"), x + 2, y, this.contentWidth - 4, LANGUAGE_RULE_ROW_HEIGHT, COLOR_TEXT_DIM);
            return;
        }
        int end = Math.min(totalSize, this.translationRulesOffset + maxRows);
        for (int i = this.translationRulesOffset; i < end; i++) {
            ConfigManager.ModConfig.ChatTranslationRule rule = rules.get(i);
            int rowY = y + (i - this.translationRulesOffset) * LANGUAGE_RULE_ROW_STEP;
            drawRowBox(drawContext, x, rowY, this.contentWidth - 4, LANGUAGE_RULE_ROW_HEIGHT);
            String source = ChatTranslationManager.languageName(rule.sourceLanguage);
            String target = ChatTranslationManager.languageName(rule.targetLanguage);
            String label = source + " -> " + target;
            int color = rule.enabled ? COLOR_TEXT : COLOR_TEXT_DIM;
            int textBoxWidth = this.contentWidth - 72;
            drawFittedCenteredText(drawContext, label, x + 2, rowY, textBoxWidth, LANGUAGE_RULE_ROW_HEIGHT, color);
        }
    }

    private void addRemoveButton(int x, int rowY, Runnable onPress) {
        addRemoveButton(x, rowY, LIST_ROW_HEIGHT, onPress);
    }

    private void addRemoveButton(int x, int rowY, int rowHeight, Runnable onPress) {
        int buttonY = rowY + (rowHeight - REMOVE_BUTTON_SIZE) / 2;
        this.removeActions.add(new RemoveAction(x, buttonY, onPress));
    }

    private void drawFittedText(DrawContext drawContext, String raw, int x, int y, int maxWidth, int color) {
        String text = raw == null ? "" : raw;
        if (maxWidth <= 6) {
            return;
        }
        if (this.textRenderer.getWidth(text) <= maxWidth) {
            drawContext.drawTextWithShadow(this.textRenderer, text, x, y, color);
            return;
        }
        String ellipsis = "...";
        int trimmedWidth = maxWidth - this.textRenderer.getWidth(ellipsis);
        if (trimmedWidth <= 0) {
            return;
        }
        String clipped = this.textRenderer.trimToWidth(text, trimmedWidth) + ellipsis;
        drawContext.drawTextWithShadow(this.textRenderer, clipped, x, y, color);
    }

    private void drawRowBox(DrawContext drawContext, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int effectiveWidth = width + 4;
        drawContext.fill(x, y, x + effectiveWidth, y + height, COLOR_ROW_BOX);
        drawOutline(drawContext, x, y, effectiveWidth, height, COLOR_ROW_BOX_BORDER);
    }

    private int closeButtonX() {
        return this.panelX + PANEL_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_INSET;
    }

    private int closeButtonY() {
        return this.panelY + 3;
    }

    private boolean closeButtonContains(double mx, double my) {
        int bx = closeButtonX();
        int by = closeButtonY();
        return mx >= bx && mx < bx + CLOSE_BUTTON_SIZE && my >= by && my < by + CLOSE_BUTTON_SIZE;
    }

    private void drawCloseButton(DrawContext drawContext, int mouseX, int mouseY) {
        int bx = closeButtonX();
        int by = closeButtonY();
        boolean hover = closeButtonContains(mouseX, mouseY);
        int bg = hover ? COLOR_ACTION_BUTTON_HOVER : COLOR_ACTION_BUTTON;
        drawContext.fill(bx, by, bx + CLOSE_BUTTON_SIZE, by + CLOSE_BUTTON_SIZE, bg);
        drawOutline(drawContext, bx, by, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, COLOR_ACTION_BORDER);
        int textX = bx + (CLOSE_BUTTON_SIZE - this.textRenderer.getWidth("X")) / 2;
        int textY = by + (CLOSE_BUTTON_SIZE - 8) / 2;
        drawContext.drawTextWithShadow(this.textRenderer, "X", textX, textY, 0xFFFFFFFF);
    }

    private void drawSapphirePanel(DrawContext drawContext) {
        // Outer border
        drawContext.fill(this.panelX - 1, this.panelY - 1, this.panelX + PANEL_WIDTH + 1, this.panelY + PANEL_HEIGHT + 1, COLOR_PANEL_BORDER);
        drawContext.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + PANEL_HEIGHT, COLOR_PANEL_BG);
        // Header strip
        drawContext.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + 24, 0xFF132E66);
        drawContext.fill(this.panelX, this.panelY + 24, this.panelX + PANEL_WIDTH, this.panelY + 25, COLOR_PANEL_BORDER_DARK);
        // Inner border
        drawOutline(drawContext, this.panelX + 2, this.panelY + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, 0x6638629E);
    }

    private static void drawOutline(DrawContext drawContext, int x, int y, int width, int height, int color) {
        drawContext.fill(x, y, x + width, y + 1, color);
        drawContext.fill(x, y + height - 1, x + width, y + height, color);
        drawContext.fill(x, y, x + 1, y + height, color);
        drawContext.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static void drawDivider(DrawContext drawContext, int x1, int y1, int x2, int y2) {
        drawContext.fill(x1, y1, x2, y2, COLOR_DIVIDER);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (this.resetHudLayoutConfirmOpen) {
            if (resetConfirmAcceptContains(click.x(), click.y())) {
                this.mod.resetHudLayoutToDefaults();
                this.resetHudLayoutConfirmOpen = false;
                rebuild();
                return true;
            }
            if (resetConfirmCancelContains(click.x(), click.y())) {
                this.resetHudLayoutConfirmOpen = false;
                return true;
            }
            return true;
        }

        for (RemoveAction action : this.removeActions) {
            if (action.contains((int) click.x(), (int) click.y())) {
                action.action().run();
                return true;
            }
        }

        // Close (X) button
        if (closeButtonContains(click.x(), click.y())) {
            close();
            return true;
        }
        // Sidebar tab clicks
        for (TabButton button : this.tabButtons) {
            if (button.contains((int) click.x(), (int) click.y())) {
                if (this.currentTab != button.tab) {
                    this.currentTab = button.tab;
                    this.bossActiveOffset = 0;
                    this.bossRegistryOffset = 0;
                    this.moneyHistoryOffset = 0;
                    this.eventsOffset = 0;
                    this.translationRulesOffset = 0;
                    clearEventEditState();
                    rebuild();
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.resetHudLayoutConfirmOpen) {
            return true;
        }
        if (Math.abs(verticalAmount) < 0.001D) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int step = verticalAmount > 0.0D ? -1 : 1;
        int mx = (int) mouseX;
        int my = (int) mouseY;
        boolean handled = false;
        switch (this.currentTab) {
            case BOSSES -> {
                int activeY = this.contentY + BOSSES_ACTIVE_LIST_Y_OFFSET;
                int activeHeight = BOSSES_ACTIVE_LIST_HEIGHT;
                int activeMaxRows = Math.max(1, activeHeight / BOSSES_ACTIVE_ROW_STEP);
                if (containsRect(mx, my, this.contentX, activeY, this.contentWidth, activeHeight)) {
                    int totalRows = buildTrackedBossRows().size();
                    int maxOffset = Math.max(0, totalRows - activeMaxRows);
                    this.bossActiveOffset = clampOffset(this.bossActiveOffset + step, maxOffset);
                    handled = true;
                }

                int registryY = activeY + activeHeight + BOSSES_REGISTRY_GAP;
                int registryHeight = BOSSES_REGISTRY_LIST_HEIGHT;
                int registryMaxRows = Math.max(1, registryHeight / BOSSES_REGISTRY_ROW_STEP);
                if (containsRect(mx, my, this.contentX, registryY, this.contentWidth, registryHeight)) {
                    int totalRows = this.mod.getBossTracker().getBossRegistry().size();
                    int maxOffset = Math.max(0, totalRows - registryMaxRows);
                    this.bossRegistryOffset = clampOffset(this.bossRegistryOffset + step, maxOffset);
                    handled = true;
                }
            }
            case MONEY -> {
                int listY = this.contentY + 26 + MONEY_RECEIPTS_LIST_Y_OFFSET;
                int listHeight = this.contentHeight - (listY - this.contentY) - 4;
                int maxRows = Math.max(1, listHeight / MONEY_RECEIPT_ROW_STEP);
                if (containsRect(mx, my, this.contentX, listY, this.contentWidth, listHeight)) {
                    int totalRows = this.mod.getMoneyTracker().getHistory().size();
                    int maxOffset = Math.max(0, totalRows - maxRows);
                    this.moneyHistoryOffset = clampOffset(this.moneyHistoryOffset + step, maxOffset);
                    handled = true;
                }
            }
            case EVENTS -> {
                int listY = this.contentY + 26 + EVENTS_LIST_START_Y_OFFSET;
                int listHeight = this.contentHeight - (listY - this.contentY) - 4;
                int maxRows = Math.max(1, listHeight / EVENTS_ROW_STEP);
                if (containsRect(mx, my, this.contentX, listY, this.contentWidth, listHeight)) {
                    int totalRows = this.mod.getEventCountdownTracker().getRawEvents().size();
                    int maxOffset = Math.max(0, totalRows - maxRows);
                    this.eventsOffset = clampOffset(this.eventsOffset + step, maxOffset);
                    handled = true;
                }
            }
            case LANGUAGE -> {
                int listY = this.contentY + LANGUAGE_RULES_LIST_Y_OFFSET;
                int listHeight = this.contentHeight - (listY - this.contentY) - 4;
                int maxRows = Math.max(1, listHeight / LANGUAGE_RULE_ROW_STEP);
                if (containsRect(mx, my, this.contentX, listY, this.contentWidth, listHeight)) {
                    int totalRows = this.mod.getConfig().chatTranslationRules.size();
                    int maxOffset = Math.max(0, totalRows - maxRows);
                    this.translationRulesOffset = clampOffset(this.translationRulesOffset + step, maxOffset);
                    handled = true;
                }
            }
            default -> {
            }
        }

        if (handled) {
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }

    private static int clampOffset(int candidate, int maxOffset) {
        if (maxOffset <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(maxOffset, candidate));
    }

    private static boolean containsRect(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private int resetConfirmX() {
        return this.panelX + (PANEL_WIDTH - RESET_CONFIRM_WIDTH) / 2;
    }

    private int resetConfirmY() {
        return this.panelY + (PANEL_HEIGHT - RESET_CONFIRM_HEIGHT) / 2;
    }

    private boolean resetConfirmAcceptContains(double mx, double my) {
        int x = resetConfirmX() + RESET_CONFIRM_WIDTH / 2 - RESET_CONFIRM_BUTTON_GAP - RESET_CONFIRM_BUTTON_SIZE;
        int y = resetConfirmY() + RESET_CONFIRM_HEIGHT - RESET_CONFIRM_BUTTON_SIZE - 10;
        return mx >= x && mx < x + RESET_CONFIRM_BUTTON_SIZE && my >= y && my < y + RESET_CONFIRM_BUTTON_SIZE;
    }

    private boolean resetConfirmCancelContains(double mx, double my) {
        int x = resetConfirmX() + RESET_CONFIRM_WIDTH / 2 + RESET_CONFIRM_BUTTON_GAP;
        int y = resetConfirmY() + RESET_CONFIRM_HEIGHT - RESET_CONFIRM_BUTTON_SIZE - 10;
        return mx >= x && mx < x + RESET_CONFIRM_BUTTON_SIZE && my >= y && my < y + RESET_CONFIRM_BUTTON_SIZE;
    }

    private void drawResetHudConfirmPopup(DrawContext drawContext, int mouseX, int mouseY) {
        int x = resetConfirmX();
        int y = resetConfirmY();
        int acceptX = x + RESET_CONFIRM_WIDTH / 2 - RESET_CONFIRM_BUTTON_GAP - RESET_CONFIRM_BUTTON_SIZE;
        int cancelX = x + RESET_CONFIRM_WIDTH / 2 + RESET_CONFIRM_BUTTON_GAP;
        int buttonsY = y + RESET_CONFIRM_HEIGHT - RESET_CONFIRM_BUTTON_SIZE - 10;

        drawContext.fill(this.panelX + 3, this.panelY + 25, this.panelX + PANEL_WIDTH - 3, this.panelY + PANEL_HEIGHT - 3, 0x8801030B);
        drawContext.fill(x, y, x + RESET_CONFIRM_WIDTH, y + RESET_CONFIRM_HEIGHT, 0xEE0E2650);
        drawOutline(drawContext, x, y, RESET_CONFIRM_WIDTH, RESET_CONFIRM_HEIGHT, COLOR_ACTION_BORDER);

        drawContext.drawCenteredTextWithShadow(
            this.textRenderer,
            this.tr("main.reset_hud_confirm_title"),
            x + RESET_CONFIRM_WIDTH / 2,
            y + 16,
            COLOR_HEADER
        );
        drawContext.drawCenteredTextWithShadow(
            this.textRenderer,
            this.tr("main.reset_hud_confirm_body"),
            x + RESET_CONFIRM_WIDTH / 2,
            y + 34,
            COLOR_TEXT
        );

        boolean acceptHover = resetConfirmAcceptContains(mouseX, mouseY);
        boolean cancelHover = resetConfirmCancelContains(mouseX, mouseY);
        int acceptBg = acceptHover ? COLOR_ACTION_BUTTON_HOVER : COLOR_ACTION_BUTTON;
        int cancelBg = cancelHover ? 0xFF7A2430 : 0xFF5E1A25;
        drawContext.fill(acceptX, buttonsY, acceptX + RESET_CONFIRM_BUTTON_SIZE, buttonsY + RESET_CONFIRM_BUTTON_SIZE, acceptBg);
        drawContext.fill(cancelX, buttonsY, cancelX + RESET_CONFIRM_BUTTON_SIZE, buttonsY + RESET_CONFIRM_BUTTON_SIZE, cancelBg);
        drawOutline(drawContext, acceptX, buttonsY, RESET_CONFIRM_BUTTON_SIZE, RESET_CONFIRM_BUTTON_SIZE, COLOR_ACTION_BORDER);
        drawOutline(drawContext, cancelX, buttonsY, RESET_CONFIRM_BUTTON_SIZE, RESET_CONFIRM_BUTTON_SIZE, COLOR_ACTION_BORDER);

        int checkX = acceptX + (RESET_CONFIRM_BUTTON_SIZE - this.textRenderer.getWidth("✓")) / 2;
        int xX = cancelX + (RESET_CONFIRM_BUTTON_SIZE - this.textRenderer.getWidth("X")) / 2;
        int textY = buttonsY + (RESET_CONFIRM_BUTTON_SIZE - 8) / 2;
        drawContext.drawTextWithShadow(this.textRenderer, "✓", checkX, textY, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(this.textRenderer, "X", xX, textY, 0xFFFFFFFF);
    }

    private boolean isEditingEvent() {
        return !this.editingEventOriginalName.isBlank() && !this.editingEventOriginalTime.isBlank();
    }

    private void beginEventEdit(String name, String time) {
        this.editingEventOriginalName = name == null ? "" : name;
        this.editingEventOriginalTime = time == null ? "" : time;
        if (this.eventNameField != null) {
            this.eventNameField.setText(this.editingEventOriginalName);
        }
        if (this.eventTimeField != null) {
            this.eventTimeField.setText(this.editingEventOriginalTime);
        }
    }

    private void clearEventEditState() {
        this.editingEventOriginalName = "";
        this.editingEventOriginalTime = "";
    }

    private static long parseLongSafe(String text) {
        if (text == null) return 0L;
        try {
            return Long.parseLong(text.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static double parseDoubleSafe(String text) {
        if (text == null) return 0.0D;
        try {
            return Double.parseDouble(text.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return 0.0D;
        }
    }

    private String tr(String key) {
        return this.mod.tr(key);
    }

    private Text trText(String key) {
        return Text.literal(tr(key));
    }

    private boolean hasAnyTranslationRoutesEnabled(ConfigManager.ModConfig cfg) {
        if (cfg == null) {
            return true;
        }
        return cfg.chatTranslationPublicEnabled
            || cfg.chatTranslationPrivateEnabled
            || cfg.chatTranslationSystemEnabled;
    }

    private void enforceTranslationRouteGuard() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        if (cfg == null) {
            return;
        }
        if (hasAnyTranslationRoutesEnabled(cfg)) {
            return;
        }
        if (cfg.chatTranslationEnabled) {
            this.mod.setChatTranslationEnabled(false);
        }
    }

    private static int findUiLanguageIndex(List<UiLocalization.LanguageOption> languages, String code) {
        String normalized = UiLocalization.normalizeLanguageCode(code);
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).code().equals(normalized)) {
                return i;
            }
        }
        return 0;
    }

    private String profileFieldLabel(int index) {
        if (index < 0 || index >= PROFILE_FIELD_KEYS.length) {
            return "";
        }
        return tr(PROFILE_FIELD_KEYS[index]);
    }

    private static String formatDouble(double value) {
        if (Math.floor(value) == value && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String prettyColorName(String raw) {
        String normalized = MinepieceQolClient.normalizeHudColorName(raw);
        if ("default".equals(normalized)) {
            return "Default";
        }
        String[] words = normalized.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.toString();
    }

    private static String formatTimer(long ms) {
        long seconds = ms / 1000L;
        long h = seconds / 3600L;
        long m = (seconds % 3600L) / 60L;
        long s = seconds % 60L;
        if (h > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    private enum BossRowType {
        SPAWN_HEADER,
        BOSS_ENTRY,
        MINIBOSS_HEADER,
        MINIBOSS_ENTRY
    }

    private record TrackedBossRow(BossRowType type, String label, String spawnId, String entryKey, boolean removable) {
        static TrackedBossRow spawnHeader(String label, String spawnId) {
            return new TrackedBossRow(BossRowType.SPAWN_HEADER, label, spawnId, null, true);
        }

        static TrackedBossRow bossEntry(String label, String entryKey) {
            return new TrackedBossRow(BossRowType.BOSS_ENTRY, label, null, entryKey, true);
        }

        static TrackedBossRow bossInfo(String label) {
            return new TrackedBossRow(BossRowType.BOSS_ENTRY, label, null, null, false);
        }

        static TrackedBossRow minibossHeader(String label) {
            return new TrackedBossRow(BossRowType.MINIBOSS_HEADER, label, null, null, false);
        }

        static TrackedBossRow minibossEntry(String label, String entryKey) {
            return new TrackedBossRow(BossRowType.MINIBOSS_ENTRY, label, null, entryKey, true);
        }
    }

    private final class TabButton {
        final int x;
        final int y;
        final int width;
        final int height;
        final Tab tab;

        TabButton(int x, int y, int width, int height, Tab tab) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.tab = tab;
        }

        boolean contains(int mx, int my) {
            return mx >= this.x && mx < this.x + this.width && my >= this.y && my < this.y + this.height;
        }

        void render(DrawContext drawContext, int mouseX, int mouseY) {
            boolean active = MinepieceMenuScreen.this.currentTab == this.tab;
            boolean hover = contains(mouseX, mouseY);
            int bg = active ? COLOR_TAB_ACTIVE : (hover ? COLOR_TAB_HOVER : COLOR_TAB_NORMAL);
            drawContext.fill(this.x, this.y, this.x + this.width, this.y + this.height, bg);
            if (active) {
                drawContext.fill(this.x, this.y, this.x + 2, this.y + this.height, COLOR_TAB_ACCENT);
            }
            drawOutline(drawContext, this.x, this.y, this.width, this.height, 0x553D78E0);
            int textY = this.y + (this.height - 8) / 2;
            drawContext.drawTextWithShadow(MinepieceMenuScreen.this.textRenderer, this.tab.label(MinepieceMenuScreen.this), this.x + 8, textY, COLOR_TEXT);
        }
    }

    private void drawRemoveButtons(DrawContext drawContext, int mouseX, int mouseY) {
        for (RemoveAction action : this.removeActions) {
            boolean hover = action.contains(mouseX, mouseY);
            int bg = hover ? COLOR_ACTION_BUTTON_HOVER : COLOR_ACTION_BUTTON;
            int border = COLOR_ACTION_BORDER;
            int x = action.x();
            int y = action.y();
            drawContext.fill(x, y, x + REMOVE_BUTTON_SIZE, y + REMOVE_BUTTON_SIZE, bg);
            drawOutline(drawContext, x, y, REMOVE_BUTTON_SIZE, REMOVE_BUTTON_SIZE, border);
            int textX = x + (REMOVE_BUTTON_SIZE - this.textRenderer.getWidth("X")) / 2;
            int textY = y + (REMOVE_BUTTON_SIZE - 8) / 2;
            drawContext.drawTextWithShadow(this.textRenderer, "X", textX, textY, 0xFFFFFFFF);
        }
    }

    private void drawStyledVanillaButtons(DrawContext drawContext, int mouseX, int mouseY) {
        for (var child : this.children()) {
            if (!(child instanceof ButtonWidget button)) {
                continue;
            }
            int x = button.getX();
            int y = button.getY();
            int width = button.getWidth();
            int height = button.getHeight();
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
            int bg = button.active
                ? (hovered ? COLOR_ACTION_BUTTON_HOVER : COLOR_ACTION_BUTTON)
                : COLOR_ACTION_BUTTON_DISABLED;
            drawContext.fill(x, y, x + width, y + height, bg);
            drawOutline(drawContext, x, y, width, height, COLOR_ACTION_BORDER);
            drawFittedCenteredText(drawContext, button.getMessage().getString(), x, y, width, height, 0xFFFFFFFF);
        }
    }

    private void drawFittedCenteredText(DrawContext drawContext, String text, int x, int y, int width, int height, int color) {
        String value = text == null ? "" : text;
        int maxWidth = Math.max(2, width - 8);
        if (this.textRenderer.getWidth(value) > maxWidth) {
            String ellipsis = "...";
            int trimWidth = Math.max(2, maxWidth - this.textRenderer.getWidth(ellipsis));
            value = this.textRenderer.trimToWidth(value, trimWidth) + ellipsis;
        }
        int tx = x + (width - this.textRenderer.getWidth(value)) / 2;
        int ty = y + (height - 8) / 2;
        drawContext.drawTextWithShadow(this.textRenderer, value, tx, ty, color);
    }

    private record RemoveAction(int x, int y, Runnable action) {
        boolean contains(int mx, int my) {
            return mx >= this.x && mx < this.x + REMOVE_BUTTON_SIZE && my >= this.y && my < this.y + REMOVE_BUTTON_SIZE;
        }
    }

    private enum Tab {
        MAIN("tab.main"),
        BOSSES("tab.bosses"),
        MONEY("tab.money"),
        JOBS("tab.jobs"),
        EVENTS("tab.events"),
        PROFILE("tab.profile"),
        LANGUAGE("tab.language"),
        AUCTION_HOUSE("tab.other");

        final String key;

        Tab(String key) {
            this.key = key;
        }

        String label(MinepieceMenuScreen screen) {
            return screen.tr(this.key);
        }
    }
}
