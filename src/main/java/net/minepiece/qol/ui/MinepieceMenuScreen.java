package net.minepiece.qol.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.ConfigManager;
import net.minepiece.qol.i18n.UiLocalization;
import net.minepiece.qol.state.BossTracker;
import net.minepiece.qol.state.ChatTranslationManager;
import net.minepiece.qol.state.JobsTracker;
import net.minepiece.qol.state.PersistentState;

public final class MinepieceMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 468;
    private static final int PANEL_HEIGHT = 320;
    private static final int SIDEBAR_WIDTH = 100;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_SPACING = 4;
    private static final int CONTENT_PADDING = 10;
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
    private int COLOR_BG_OVERLAY = 0xC0050818;
    private int COLOR_PANEL_BG = 0xEE0B1838;
    private int COLOR_PANEL_BORDER = 0xFF3D78E0;
    private int COLOR_SIDEBAR_BG = 0xEE091434;
    private int COLOR_TAB_NORMAL = 0xCC0E2150;
    private int COLOR_TAB_HOVER = 0xDD13347A;
    private int COLOR_TAB_ACTIVE = 0xFF1F4FB8;
    private int COLOR_TAB_ACCENT = 0xFF6FA8FF;
    private int COLOR_TEXT = 0xFFEAF1FF;
    private int COLOR_TEXT_DIM = 0xFF89A8DA;
    private int COLOR_HEADER = 0xFFAFD0FF;
    private int COLOR_DIVIDER = 0x553D78E0;
    private int COLOR_ACTION_BUTTON = 0xFF16396E;
    private int COLOR_ACTION_BUTTON_HOVER = 0xFF1F4A8C;
    private int COLOR_ACTION_BUTTON_DISABLED = 0xFF1A2B49;
    private int COLOR_ACTION_BORDER = 0xFFDDBA6A;
    private int COLOR_ROW_BOX = 0x66112A52;
    private int COLOR_ROW_BOX_BORDER = 0x88649CE4;
    private static final int CLOSE_BUTTON_SIZE = 16;
    private static final int CLOSE_BUTTON_INSET = 4;
    private static final int RESET_CONFIRM_WIDTH = 260;
    private static final int RESET_CONFIRM_HEIGHT = 88;
    private static final int RESET_CONFIRM_BUTTON_SIZE = 18;
    private static final int RESET_CONFIRM_BUTTON_GAP = 14;

    /** Minepiece gold, used for the title line, the active page and the version badge. */
    private static final int GOLD = 0xFFDDBA6A;
    private static final String VERSION = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("minepiece-qol")
        .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
    private static final int SIDEBAR_TABS_Y = 52;

    private final MinepieceQolClient mod;
    private Tab currentTab = Tab.MAIN;
    private EditBox searchBox;
    // Section layout for the simple settings pages: a running y offset and the boxes to draw behind the rows.
    private static final int SECTION_HEADER = 14, SECTION_GAP = 6, SECTION_INSET = 4;
    private record Section(String title, int top, int bottom) { }
    private record SectionText(String text, int y) { }
    private final List<Section> sections = new ArrayList<>();
    private final List<SectionText> sectionTexts = new ArrayList<>();
    private final Map<Checkbox, String> switchDescriptions = new java.util.IdentityHashMap<>();
    private int layoutY;
    private String openSection;
    private int openSectionTop;
    private String searchQuery = "";
    private List<SearchEntry> searchIndex;
    private final Map<Group, Tab> selectedPages = new java.util.EnumMap<>(Group.class);
    private final Map<Tab, Integer> pageScroll = new java.util.EnumMap<>(Tab.class);
    private float menuScale = 1;
    private int panelHeight = PANEL_HEIGHT;
    private int contentTop;
    private int contentBottom;
    private int maxPageScroll;
    private int panelX;
    private int panelY;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private final List<TabButton> tabButtons = new ArrayList<>();

    // Money widgets
    private EditBox moneyTotalField;
    private EditBox moneyMadeTodayField;
    private EditBox moneyAhSoldField;
    private EditBox moneyAhBoughtField;
    private int moneyHistoryOffset;

    // Jobs widgets
    private final EditBox[] jobLevelFields = new EditBox[4];
    private final EditBox[] jobCurrentXpFields = new EditBox[4];
    private final EditBox[] jobNeededXpFields = new EditBox[4];

    // Profile widgets
    private final EditBox[] profileStatFields = new EditBox[PROFILE_FIELD_KEYS.length];

    // Events widgets
    private EditBox eventNameField;
    private EditBox eventTimeField;
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
        super(Component.literal(mod == null ? "Minepiece QoL" : mod.tr("menu.title")));
        this.mod = mod;
    }

    /** Opens on a named page, e.g. "EXTRAS" (used by the client smoke test). */
    public MinepieceMenuScreen(MinepieceQolClient mod, String page) {
        this(mod);
        this.currentTab = Tab.valueOf(page);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        refreshPalette();
        this.menuScale = Math.min(1F, Math.min(this.width / 484F, this.height / 220F));
        int logicalWidth = Math.round(this.width / menuScale);
        int logicalHeight = Math.round(this.height / menuScale);
        this.panelHeight = Math.min(PANEL_HEIGHT, logicalHeight - 16);
        this.panelX = (logicalWidth - PANEL_WIDTH) / 2;
        this.panelY = (logicalHeight - panelHeight) / 2;
        this.contentX = this.panelX + SIDEBAR_WIDTH + CONTENT_PADDING;
        this.contentTop = this.panelY + 58;
        this.contentBottom = this.panelY + this.panelHeight - 20;
        this.contentY = contentTop - pageScroll.getOrDefault(currentTab, 0);
        this.contentWidth = PANEL_WIDTH - SIDEBAR_WIDTH - CONTENT_PADDING * 2;
        this.contentHeight = Math.max(260, contentBottom - contentTop);

        buildSidebar();
        this.searchBox = new EditBox(this.font, this.panelX + 6, this.panelY + 30, SIDEBAR_WIDTH - 12, 16, Component.literal(tr("menu.search")));
        this.searchBox.setHint(Component.literal(tr("menu.search")));
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(text -> {
            if (text.equals(this.searchQuery)) return;
            this.searchQuery = text;
            rebuild();
            setFocused(this.searchBox);
        });
        buildTabContent();
    }

    private void buildSidebar() {
        this.tabButtons.clear();
        int sidebarX = this.panelX + 6;
        int sidebarY = this.panelY + SIDEBAR_TABS_Y;
        Group[] groups = Group.values();
        for (int i = 0; i < groups.length; i++) {
            Tab tab = groups[i].pages()[0];
            int tabStep = Math.min(TAB_HEIGHT + TAB_SPACING, (panelHeight - SIDEBAR_TABS_Y - 20) / groups.length);
            int by = sidebarY + i * tabStep;
            TabButton button = new TabButton(sidebarX, by, SIDEBAR_WIDTH - 12, Math.min(TAB_HEIGHT, tabStep - 2), tab);
            this.tabButtons.add(button);
        }
    }

    private void buildTabContent() {
        // Reset content widgets but preserve sidebar interaction (which we hand-draw, not as widgets)
        clearWidgets();
        if (this.searchBox != null) addWidget(this.searchBox);
        this.contentY = contentTop - pageScroll.getOrDefault(currentTab, 0);
        this.removeActions.clear();
        resetSections();
        if (isSearching()) buildSearchResults();
        else buildPage(this.currentTab);
        endSection();
        List<net.minecraft.client.gui.components.AbstractWidget> built = new ArrayList<>();
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && child != this.searchBox) built.add(widget);
        for (var child : List.copyOf(children())) {
            if (child instanceof Button button) {
                removeWidget(button);
                addRenderableWidget(new ThemedButton(this.mod, button));
            } else if (child instanceof Checkbox box) {
                removeWidget(box);
                boolean inSection = box.getX() == this.contentX + SECTION_INSET;
                int width = box.getMessage().getString().isEmpty() ? box.getWidth()
                    : inSection ? this.contentWidth - 6 - SECTION_INSET * 2 : switchWidth(box, built);
                addRenderableWidget(new ThemedSwitch(this.mod, box, width, this.switchDescriptions.get(box)));
            }
        }
        if (!isSearching()) selectedPages.put(groupFor(currentTab), currentTab);
        finishTabContent();
    }

    /** Setting rows stretch to the next widget on the same row, or to the content edge. */
    private int switchWidth(Checkbox box, List<net.minecraft.client.gui.components.AbstractWidget> built) {
        int limit = this.contentX + this.contentWidth - 6;
        for (var other : built) {
            if (other == box || other.getX() <= box.getX()) continue;
            boolean sameRow = other.getY() < box.getY() + box.getHeight() && other.getY() + other.getHeight() > box.getY();
            if (sameRow) limit = Math.min(limit, other.getX() - 4);
        }
        return Math.max(40, limit - box.getX());
    }

    private boolean isSearching() {
        return this.searchQuery != null && !this.searchQuery.isBlank();
    }

    private record SearchEntry(Tab tab, String label) { }

    /** Every page's own widget labels, read by building each page once off-screen. */
    private List<SearchEntry> searchIndex() {
        if (this.searchIndex != null) return this.searchIndex;
        Tab saved = this.currentTab;
        List<SearchEntry> entries = new ArrayList<>();
        for (Tab tab : Tab.values()) {
            entries.add(new SearchEntry(tab, tab.label(this)));
            this.currentTab = tab;
            clearWidgets();
            this.removeActions.clear();
            resetSections();
            try {
                buildPage(tab);
                endSection();
            } catch (RuntimeException ignored) {
                // a page that cannot build off-screen is still found by its name
            }
            for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && !(widget instanceof EditBox)) {
                String label = widget.getMessage().getString().trim();
                if (!label.isEmpty()) entries.add(new SearchEntry(tab, label));
            }
        }
        this.currentTab = saved;
        clearWidgets();
        this.removeActions.clear();
        resetSections();
        if (this.searchBox != null) addWidget(this.searchBox);
        this.searchIndex = entries;
        return entries;
    }

    private List<SearchEntry> searchMatches() {
        String query = this.searchQuery.trim().toLowerCase(Locale.ROOT);
        java.util.LinkedHashSet<SearchEntry> found = new java.util.LinkedHashSet<>();
        for (SearchEntry entry : searchIndex()) {
            String group = tr(groupFor(entry.tab()).key);
            if ((entry.label() + " " + entry.tab().label(this) + " " + group).toLowerCase(Locale.ROOT).contains(query)) found.add(entry);
        }
        return List.copyOf(found);
    }

    private void buildSearchResults() {
        var matches = searchMatches();
        for (int i = 0; i < matches.size(); i++) {
            SearchEntry entry = matches.get(i);
            String text = tr(groupFor(entry.tab()).key) + " › " + entry.tab().label(this)
                + (entry.label().equals(entry.tab().label(this)) ? "" : ": " + entry.label());
            addRenderableWidget(Button.builder(Component.literal(text), b -> {
                this.currentTab = entry.tab();
                this.searchQuery = "";
                this.searchBox.setValue("");
                rebuild();
            }).bounds(this.contentX, this.contentY + i * 22, this.contentWidth - 6, 18).build());
        }
    }

    private void buildPage(Tab tab) {
        switch (tab) {
            case MAIN -> buildMainTab();
            case BOSSES -> buildBossesTab();
            case MONEY -> buildMoneyTab();
            case JOBS -> buildJobsTab();
            case EVENTS -> buildEventsTab();
            case PROFILE -> buildProfileTab();
            case LANGUAGE -> buildLanguageTab();
            case AUCTION_HOUSE -> {
                section("menu.section.colors");
                describedSwitch("setting.auction_highlight", this.mod.getConfig().auctionHighlightEnabled, v -> this.mod.setAuctionHighlightEnabled(v));
                describedSwitch("setting.auction_price_key", this.mod.getConfig().auctionPriceKeyEnabled, v -> this.mod.getConfig().auctionPriceKeyEnabled = v);
                section("menu.section.how_it_works");
                sectionText(tr("auction.how1"));
                sectionText(tr("auction.how2"));
                sectionText(tr("auction.how3"));
            }
            case PROGRESS -> buildProgressTab();
            case COOKING -> buildCookingTab();
            case EXTRAS -> buildExtrasTab();
            case COMBAT_SETTINGS -> buildCombatSettings();
            case STAT_DISPLAY -> buildStatDisplay();
            case PASS_PETS -> buildPassPets();
            case SCROLLS -> {
                section("menu.section.panels");
                describedSwitch("setting.scrolls_hud", this.mod.getConfig().scrollsEnabled, v -> this.mod.setScrollsEnabled(v));
            }
            case HELP -> addRenderableWidget(Button.builder(trText("extras.help"), b -> this.minecraft.gui.setScreen(new HelpScreen(this.mod, this)))
                .bounds(this.contentX, this.contentY, 220, 18).build());
            case APPEARANCE -> {
                section("menu.section.customize");
                String[] labels = {"ui.panels", "ui.appearance", "ui.loadouts", "ui.pictures"};
                int[] pages = {3, 0, 1, 2};
                for (int i = 0; i < pages.length; i++) {
                    final int page = pages[i];
                    sectionButton(trText(labels[i]), button -> this.minecraft.gui.setScreen(new AppearanceScreen(this.mod, this, page)));
                }
                section("menu.section.layout");
                sectionButton(trText("button.edit_hud_layout"), b -> this.mod.openHudLayoutEditor());
                sectionButton(trText("button.reset_hud_layout"), b -> this.resetHudLayoutConfirmOpen = true);
            }
        }
    }

    private void finishTabContent() {
        int extent = isSearching() ? 0 : switch (currentTab) {
            case COOKING -> 255;
            case BOSSES -> BOSSES_ACTIVE_LIST_Y_OFFSET + BOSSES_ACTIVE_LIST_HEIGHT + BOSSES_REGISTRY_GAP + BOSSES_REGISTRY_LIST_HEIGHT;
            case MONEY, EVENTS, LANGUAGE -> contentHeight - 4;
            default -> 0;
        };
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && child != this.searchBox)
            extent = Math.max(extent, widget.getY() + widget.getHeight() - contentY);
        for (var remove : removeActions) extent = Math.max(extent, remove.y() + REMOVE_BUTTON_SIZE - contentY);
        for (var section : sections) extent = Math.max(extent, section.bottom());
        maxPageScroll = Math.max(0, extent - (contentBottom - contentTop));
        int offset = pageScroll.getOrDefault(currentTab, 0);
        if (offset > maxPageScroll) { pageScroll.put(currentTab, maxPageScroll); buildTabContent(); }
        updateContentVisibility();
    }

    private void buildMainTab() {
        var cfg = this.mod.getConfig();
        section("menu.section.core");
        describedSwitch("setting.mod_enabled", cfg.modEnabled, this.mod::setModEnabled);
        describedSwitch("setting.show_all_features", cfg.allFeaturesVisible, this.mod::setAllFeaturesVisible);
        section("menu.section.language_updates");
        var languages = this.mod.getSupportedUiLanguages();
        uiLanguageIndex = findUiLanguageIndex(languages, this.mod.getUiLanguage());
        sectionButton(Component.literal(tr("main.mod_language") + ": " + languages.get(uiLanguageIndex).label()), b -> {
            uiLanguageIndex = (uiLanguageIndex + 1) % languages.size();
            this.mod.setUiLanguage(languages.get(uiLanguageIndex).code()); rebuild();
        });
        describedSwitch("extras.updates", cfg.updateCheckEnabled, v -> cfg.updateCheckEnabled = v);
        section("menu.section.layout");
        sectionButton(trText("button.edit_hud_layout"), b -> this.mod.openHudLayoutEditor());
        if (this.mod.isDebugToolsAvailable()) {
            section("menu.section.developer");
            describedSwitch("setting.debug_logging", cfg.debugEnabled, this.mod::setDebugEnabled);
        }
    }

    private void featureToggle(String label, boolean value, int dy, java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(Checkbox.builder(trText(label), this.font).pos(contentX, contentY + dy).selected(value)
            .onValueChange((box, checked) -> { setter.accept(checked); this.mod.saveConfig(); }).build());
    }

    private void buildCombatSettings() {
        var cfg = this.mod.getConfig();
        section("menu.section.boss_timers");
        describedSwitch("bosses.automatic_reads", cfg.automaticBossReads, v -> cfg.automaticBossReads = v);
        describedSwitch("extras.boss_alerts", cfg.bossAlertsEnabled, v -> cfg.bossAlertsEnabled = v);
        sectionButton(Component.literal(tr("bosses.order") + ": " + tr(cfg.appearance.bossesByTime ? "bosses.by_time" : "bosses.by_island")), b -> {
            cfg.appearance.bossesByTime = !cfg.appearance.bossesByTime;
            this.mod.saveConfig(); rebuild();
        });
        sectionButton(trText("extras.refresh"), b -> this.mod.getEssentials().refresh());
        section("menu.section.cooldowns");
        describedSwitch("setting.haki_cooldown", cfg.hakiEnabled, this.mod::setHakiEnabled);
    }

    private void buildPassPets() {
        var cfg = this.mod.getConfig();
        section("menu.section.panels");
        describedSwitch("extras.daily_quests", cfg.dailyQuestsEnabled, v -> cfg.dailyQuestsEnabled = v);
        describedSwitch("extras.active_pets", cfg.activePetsEnabled, v -> cfg.activePetsEnabled = v);
        describedSwitch("extras.safes", cfg.safeChestsEnabled, v -> cfg.safeChestsEnabled = v);
        section("menu.section.background");
        describedSwitch("extras.background", cfg.backgroundRefreshEnabled, v -> cfg.backgroundRefreshEnabled = v);
        sectionButton(trText("extras.refresh"), b -> this.mod.getEssentials().refresh());
        section("menu.section.safes");
        sectionButton(trText("extras.safes_reset"), b -> this.mod.getSafeChests().clear());
    }

    private void buildStatDisplay() {
        var selected = this.mod.getConfig().appearance.profileStats;
        var keys = net.minepiece.qol.state.ProfileStatsTracker.STAT_KEYS;
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            int index = selected.indexOf(key);
            String label = (index < 0 ? "" : (index + 1) + ". ") + tr("profile." + key);
            addRenderableWidget(Checkbox.builder(Component.literal(label), this.font).pos(contentX, contentY + i * 22)
                .selected(index >= 0).onValueChange((box, checked) -> {
                    selected.remove(key); if (checked) selected.add(key);
                    this.mod.saveConfig(); rebuild();
                }).build());
            if (index >= 0) for (int direction : new int[]{-1, 1}) {
                var button = Button.builder(Component.literal(direction < 0 ? "↑" : "↓"), b -> {
                    java.util.Collections.swap(selected, index, index + direction);
                    this.mod.saveConfig(); rebuild();
                }).bounds(contentX + 270 + (direction > 0 ? 28 : 0), contentY + i * 22, 24, 18).build();
                button.active = index + direction >= 0 && index + direction < selected.size();
                addRenderableWidget(button);
            }
        }
        addRenderableWidget(Button.builder(trText("ui.reset_stats"), b -> {
            selected.clear(); selected.addAll(List.of("power", "strength", "speed"));
            this.mod.saveConfig(); rebuild();
        }).bounds(contentX, contentY + 272, 220, 18).build());
    }

    private void buildBossesTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY;

        addRenderableWidget(Checkbox.builder(trText("setting.boss_tracking"), this.font)
            .pos(x, y).selected(this.mod.isBossTrackingEnabled())
            .onValueChange((box, checked) -> this.mod.setBossTrackingEnabled(checked))
            .build());

        addRenderableWidget(Checkbox.builder(trText("setting.miniboss_waypoints"), this.font)
            .pos(x + 130, y).selected(cfg.minibossTrackingEnabled)
            .onValueChange((box, checked) -> this.mod.setMinibossTrackingEnabled(checked))
            .build());

        addRenderableWidget(Checkbox.builder(trText("setting.miniboss_panel"), this.font)
            .pos(x, y + BOSSES_MINIBOSS_PANEL_Y_OFFSET).selected(this.mod.isMinibossHudEnabled())
            .onValueChange((box, checked) -> this.mod.setMinibossHudEnabled(checked))
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
            addRenderableWidget(Button.builder(Component.literal("+15m"), b -> {
                    Minecraft client = Minecraft.getInstance();
                    int px = 0, py = 0, pz = 0;
                    if (client != null && client.player != null) {
                        px = (int) Math.floor(client.player.getX());
                        py = (int) Math.floor(client.player.getY());
                        pz = (int) Math.floor(client.player.getZ());
                    }
                    this.mod.getBossTracker().addBossFromRegistry(name, px, py, pz);
                    rebuild();
                })
                .bounds(plusX, rowY, plusWidth, rowHeight)
                .build());
            addRenderableWidget(Button.builder(Component.literal("Never"), b -> {
                    this.mod.getBossTracker().ignoreBossForever(name);
                    rebuild();
                })
                .bounds(neverX, rowY, neverWidth, rowHeight)
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

        addRenderableWidget(Checkbox.builder(trText("setting.money_tracking"), this.font)
            .pos(x, y).selected(cfg.moneyTrackingEnabled)
            .onValueChange((box, checked) -> this.mod.setMoneyTrackingEnabled(checked))
            .build());

        int fy = y + 26;
        this.moneyTotalField = new EditBox(this.font, x + MONEY_VALUE_FIELD_X_OFFSET, fy, MONEY_VALUE_FIELD_WIDTH, 16, Component.literal(""));
        this.moneyTotalField.setValue(Long.toString(this.mod.getMoneyTracker().getCurrentBalance()));
        addRenderableWidget(this.moneyTotalField);

        this.moneyMadeTodayField = new EditBox(this.font, x + MONEY_VALUE_FIELD_X_OFFSET, fy + 22, MONEY_VALUE_FIELD_WIDTH, 16, Component.literal(""));
        this.moneyMadeTodayField.setValue(Long.toString(this.mod.getMoneyTracker().getMadeToday()));
        addRenderableWidget(this.moneyMadeTodayField);

        this.moneyAhSoldField = new EditBox(this.font, x + MONEY_VALUE_FIELD_X_OFFSET, fy + 44, MONEY_VALUE_FIELD_WIDTH, 16, Component.literal(""));
        this.moneyAhSoldField.setValue(Long.toString(this.mod.getMoneyTracker().getAhMadeToday()));
        addRenderableWidget(this.moneyAhSoldField);

        this.moneyAhBoughtField = new EditBox(this.font, x + MONEY_VALUE_FIELD_X_OFFSET, fy + 66, MONEY_VALUE_FIELD_WIDTH, 16, Component.literal(""));
        this.moneyAhBoughtField.setValue(Long.toString(this.mod.getMoneyTracker().getAhSpentToday()));
        addRenderableWidget(this.moneyAhBoughtField);

        addRenderableWidget(Button.builder(trText("button.save"), b -> {
                long total = parseLongSafe(this.moneyTotalField.getValue());
                long madeToday = parseLongSafe(this.moneyMadeTodayField.getValue());
                long ahSold = parseLongSafe(this.moneyAhSoldField.getValue());
                long ahBought = parseLongSafe(this.moneyAhBoughtField.getValue());
                this.mod.getMoneyTracker().setAhMadeToday(ahSold);
                this.mod.getMoneyTracker().setAhSpentToday(ahBought);
                this.mod.getMoneyTracker().setMadeToday(madeToday);
                this.mod.getMoneyTracker().setCurrentBalance(total);
            })
            .bounds(x + 178, y + 1, 46, 16)
            .build());

        addRenderableWidget(Button.builder(trText("button.run_balance"), b -> this.mod.runBalanceCommand())
            .bounds(x + 228, y + 1, 90, 16)
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

        addRenderableWidget(Checkbox.builder(trText("setting.jobs_stats"), this.font)
            .pos(x, y).selected(cfg.jobsTrackingEnabled)
            .onValueChange((box, checked) -> this.mod.setJobsTrackingEnabled(checked))
            .build());

        int rowY = y + JOBS_TABLE_Y_OFFSET;
        for (int i = 0; i < JobsTracker.HUD_JOB_ORDER.size(); i++) {
            String jobName = JobsTracker.HUD_JOB_ORDER.get(i);
            PersistentState.JobInfo info = this.mod.getJobsTracker().getJob(jobName);

            int by = rowY + i * JOBS_ROW_STEP;
            int fieldY = by + (JOBS_ROW_HEIGHT - JOBS_FIELD_HEIGHT) / 2;

            this.jobLevelFields[i] = new EditBox(this.font, x + JOBS_LEVEL_X_OFFSET, fieldY, JOBS_LEVEL_WIDTH, JOBS_FIELD_HEIGHT, Component.literal(""));
            this.jobLevelFields[i].setCentered(true);
            if (info != null && info.levelEstimated) this.jobLevelFields[i].setTooltip(
                net.minecraft.client.gui.components.Tooltip.create(trText("jobs.estimated_hint")));
            this.jobLevelFields[i].setValue(info == null ? "0" : Integer.toString(info.level));
            addRenderableWidget(this.jobLevelFields[i]);

            this.jobCurrentXpFields[i] = new EditBox(this.font, x + JOBS_CUR_X_OFFSET, fieldY, JOBS_CUR_WIDTH, JOBS_FIELD_HEIGHT, Component.literal(""));
            this.jobCurrentXpFields[i].setCentered(true);
            this.jobCurrentXpFields[i].setValue(info == null || info.currentXp < 0 ? "0" : formatDouble(info.currentXp));
            addRenderableWidget(this.jobCurrentXpFields[i]);

            this.jobNeededXpFields[i] = new EditBox(this.font, x + JOBS_NEED_X_OFFSET, fieldY, JOBS_NEED_WIDTH, JOBS_FIELD_HEIGHT, Component.literal(""));
            this.jobNeededXpFields[i].setCentered(true);
            this.jobNeededXpFields[i].setValue(info == null ? "0" : formatDouble(info.neededXp));
            addRenderableWidget(this.jobNeededXpFields[i]);
        }

        addRenderableWidget(Button.builder(trText("button.save_jobs"), b -> {
                for (int i = 0; i < JobsTracker.HUD_JOB_ORDER.size(); i++) {
                    String jobName = JobsTracker.HUD_JOB_ORDER.get(i);
                    int level = (int) parseLongSafe(this.jobLevelFields[i].getValue());
                    double cur = parseDoubleSafe(this.jobCurrentXpFields[i].getValue());
                    double need = parseDoubleSafe(this.jobNeededXpFields[i].getValue());
                    this.mod.getJobsTracker().setJobValues(jobName, level, cur, need);
                }
            })
            .bounds(x, rowY + 4 * JOBS_ROW_STEP + 6, 90, 18)
            .build());
    }

    private void buildEventsTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY;

        addRenderableWidget(Checkbox.builder(trText("setting.events_enabled"), this.font)
            .pos(x, y).selected(cfg.eventsEnabled)
            .onValueChange((box, checked) -> this.mod.setEventsEnabled(checked))
            .build());

        int formY = y + 26;
        this.eventNameField = new EditBox(this.font, x, formY, 130, 16, Component.literal(""));
        this.eventNameField.setMaxLength(64);
        this.eventNameField.setHint(trText("events.name_placeholder"));
        addRenderableWidget(this.eventNameField);

        this.eventTimeField = new EditBox(this.font, x + 138, formY, 60, 16, Component.literal(""));
        this.eventTimeField.setMaxLength(5);
        this.eventTimeField.setHint(Component.literal("HH:mm"));
        addRenderableWidget(this.eventTimeField);

        if (isEditingEvent()) {
            this.eventNameField.setValue(this.editingEventOriginalName);
            this.eventTimeField.setValue(this.editingEventOriginalTime);
        }

        addRenderableWidget(Button.builder(Component.literal(isEditingEvent() ? tr("button.save") : tr("button.add")), b -> {
                if (isEditingEvent()) {
                    this.mod.getEventCountdownTracker().editEvent(
                        this.editingEventOriginalName,
                        this.editingEventOriginalTime,
                        this.eventNameField.getValue(),
                        this.eventTimeField.getValue()
                    );
                } else {
                    this.mod.getEventCountdownTracker().addEvent(this.eventNameField.getValue(), this.eventTimeField.getValue());
                }
                clearEventEditState();
                this.eventNameField.setValue("");
                this.eventTimeField.setValue("");
                rebuild();
            })
            .bounds(x + 206, formY, 58, 18)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
                clearEventEditState();
                this.eventNameField.setValue("");
                this.eventTimeField.setValue("");
            })
            .bounds(x + 268, formY, 46, 18)
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
            addRenderableWidget(Button.builder(Component.literal("Edit"), b -> {
                    beginEventEdit(name, time);
                })
                .bounds(editX, editY, EVENTS_EDIT_BUTTON_WIDTH, EVENTS_EDIT_BUTTON_HEIGHT)
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

        addRenderableWidget(Checkbox.builder(trText("setting.profile_stats"), this.font)
            .pos(x, y).selected(cfg.profileStatsEnabled)
            .onValueChange((box, checked) -> this.mod.setProfileStatsEnabled(checked))
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
            this.profileStatFields[i] = new EditBox(this.font, x + PROFILE_FIELD_X_OFFSET, fieldY, PROFILE_FIELD_WIDTH, PROFILE_FIELD_HEIGHT, Component.literal(""));
            this.profileStatFields[i].setCentered(true);
            this.profileStatFields[i].setValue(values[i] == null ? "0" : formatDouble(values[i]));
            addRenderableWidget(this.profileStatFields[i]);
        }

        addRenderableWidget(Button.builder(trText("button.save"), b -> {
                double health = parseDoubleSafe(this.profileStatFields[0].getValue());
                double strength = parseDoubleSafe(this.profileStatFields[1].getValue());
                double damage = parseDoubleSafe(this.profileStatFields[2].getValue());
                double criticalChance = parseDoubleSafe(this.profileStatFields[3].getValue());
                double criticalDamage = parseDoubleSafe(this.profileStatFields[4].getValue());
                double power = parseDoubleSafe(this.profileStatFields[5].getValue());
                double energy = parseDoubleSafe(this.profileStatFields[6].getValue());
                double energyRegeneration = parseDoubleSafe(this.profileStatFields[7].getValue());
                double speed = parseDoubleSafe(this.profileStatFields[8].getValue());
                double dexterity = parseDoubleSafe(this.profileStatFields[9].getValue());
                double defense = parseDoubleSafe(this.profileStatFields[10].getValue());
                double regeneration = parseDoubleSafe(this.profileStatFields[11].getValue());
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
            .bounds(x + PROFILE_SAVE_BUTTON_X_OFFSET, y + 1, PROFILE_SAVE_BUTTON_WIDTH, 18)
            .build());

        addRenderableWidget(Button.builder(trText("button.sync_profile"), b -> {
                this.mod.runProfileCommand();
            })
            .bounds(x + PROFILE_SYNC_BUTTON_X_OFFSET, y + 1, PROFILE_SYNC_BUTTON_WIDTH, 18)
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

        addRenderableWidget(Checkbox.builder(Component.empty(), this.font)
            .pos(x + LANGUAGE_MASTER_CHECKBOX_X_OFFSET, y + LANGUAGE_MASTER_CHECKBOX_Y_OFFSET).selected(cfg.chatTranslationEnabled)
            .onValueChange((box, checked) -> {
                this.mod.setChatTranslationEnabled(checked);
                enforceTranslationRouteGuard();
                rebuild();
            })
            .build());

        addRenderableWidget(Checkbox.builder(Component.empty(), this.font)
            .pos(x + LANGUAGE_AGGRESSIVE_CHECKBOX_X_OFFSET, y + LANGUAGE_MASTER_CHECKBOX_Y_OFFSET).selected(cfg.chatTranslationAggressiveEnabled)
            .onValueChange((box, checked) -> {
                this.mod.setChatTranslationAggressiveEnabled(checked);
                rebuild();
            })
            .build());

        addRenderableWidget(Checkbox.builder(Component.empty(), this.font)
            .pos(x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET, y + LANGUAGE_PUBLIC_CHECKBOX_Y_OFFSET).selected(cfg.chatTranslationPublicEnabled)
            .onValueChange((box, checked) -> {
                this.mod.setChatTranslationPublicEnabled(checked);
                enforceTranslationRouteGuard();
                rebuild();
            })
            .build());
        addRenderableWidget(Checkbox.builder(Component.empty(), this.font)
            .pos(x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET, y + LANGUAGE_PRIVATE_CHECKBOX_Y_OFFSET).selected(cfg.chatTranslationPrivateEnabled)
            .onValueChange((box, checked) -> {
                this.mod.setChatTranslationPrivateEnabled(checked);
                enforceTranslationRouteGuard();
                rebuild();
            })
            .build());
        addRenderableWidget(Checkbox.builder(Component.empty(), this.font)
            .pos(x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET, y + LANGUAGE_SYSTEM_CHECKBOX_Y_OFFSET).selected(cfg.chatTranslationSystemEnabled)
            .onValueChange((box, checked) -> {
                this.mod.setChatTranslationSystemEnabled(checked);
                enforceTranslationRouteGuard();
                rebuild();
            })
            .build());

        int fromRowY = y + LANGUAGE_FROM_ROW_Y_OFFSET;
        int toRowY = y + LANGUAGE_TO_ROW_Y_OFFSET;
        int fromArrowY = fromRowY + (LANGUAGE_SELECTOR_ROW_HEIGHT - LANGUAGE_SELECTOR_BUTTON_HEIGHT) / 2;
        int toArrowY = toRowY + (LANGUAGE_SELECTOR_ROW_HEIGHT - LANGUAGE_SELECTOR_BUTTON_HEIGHT) / 2;

        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                this.translationSourceIndex = Math.floorMod(this.translationSourceIndex - 1, languages.size());
                rebuild();
            })
            .bounds(x + LANGUAGE_SELECTOR_LEFT_BUTTON_X_OFFSET, fromArrowY, LANGUAGE_SELECTOR_BUTTON_WIDTH, LANGUAGE_SELECTOR_BUTTON_HEIGHT)
            .build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                this.translationSourceIndex = Math.floorMod(this.translationSourceIndex + 1, languages.size());
                rebuild();
            })
            .bounds(x + LANGUAGE_SELECTOR_RIGHT_BUTTON_X_OFFSET, fromArrowY, LANGUAGE_SELECTOR_BUTTON_WIDTH, LANGUAGE_SELECTOR_BUTTON_HEIGHT)
            .build());

        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                this.translationTargetIndex = Math.floorMod(this.translationTargetIndex - 1, languages.size());
                rebuild();
            })
            .bounds(x + LANGUAGE_SELECTOR_LEFT_BUTTON_X_OFFSET, toArrowY, LANGUAGE_SELECTOR_BUTTON_WIDTH, LANGUAGE_SELECTOR_BUTTON_HEIGHT)
            .build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                this.translationTargetIndex = Math.floorMod(this.translationTargetIndex + 1, languages.size());
                rebuild();
            })
            .bounds(x + LANGUAGE_SELECTOR_RIGHT_BUTTON_X_OFFSET, toArrowY, LANGUAGE_SELECTOR_BUTTON_WIDTH, LANGUAGE_SELECTOR_BUTTON_HEIGHT)
            .build());

        addRenderableWidget(Button.builder(trText("button.add_rule"), b -> {
                ChatTranslationManager.LanguageOption source = languages.get(this.translationSourceIndex);
                ChatTranslationManager.LanguageOption target = languages.get(this.translationTargetIndex);
                this.mod.addChatTranslationRule(source.code(), target.code());
                rebuild();
            })
            .bounds(x + this.contentWidth - LANGUAGE_ADD_RULE_WIDTH, y + LANGUAGE_ADD_RULE_Y_OFFSET, LANGUAGE_ADD_RULE_WIDTH, LANGUAGE_ADD_RULE_HEIGHT)
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
            addRenderableWidget(Button.builder(Component.literal(rule.enabled ? tr("button.on") : tr("button.off")), b -> {
                    this.mod.setChatTranslationRuleEnabled(index, !rule.enabled);
                    rebuild();
                })
                .bounds(x + this.contentWidth - 64, toggleY, LANGUAGE_RULE_TOGGLE_WIDTH, LANGUAGE_RULE_TOGGLE_HEIGHT)
                .build());
            addRemoveButton(x + this.contentWidth - REMOVE_BUTTON_SIZE - REMOVE_BUTTON_GAP_RIGHT, rowY, LANGUAGE_RULE_ROW_HEIGHT, () -> {
                    this.mod.removeChatTranslationRule(index);
                    rebuild();
                });
        }

        if (totalSize > maxRows) {
            addRenderableWidget(Button.builder(Component.literal("^"), b -> {
                    this.translationRulesOffset = Math.max(0, this.translationRulesOffset - maxRows);
                    rebuild();
                })
                .bounds(x + this.contentWidth - 96, listY, 20, 12)
                .build());
            addRenderableWidget(Button.builder(Component.literal("v"), b -> {
                    if (this.translationRulesOffset + maxRows < totalSize) {
                        this.translationRulesOffset += maxRows;
                    }
                    rebuild();
                })
                .bounds(x + this.contentWidth - 96, listY + 14, 20, 12)
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

    private void buildExtrasTab() {
        var cfg = this.mod.getConfig();
        section("menu.section.item_icons");
        describedSwitch("setting.rarity_icons", cfg.rarityIconsEnabled, this.mod::setRarityIconsEnabled);
        describedSwitch("setting.pet_icons", cfg.petStatIconsEnabled, this.mod::setPetStatIconsEnabled);
        describedSwitch("extras.hotbar", cfg.hotbarRarityEnabled, v -> cfg.hotbarRarityEnabled = v);
        section("menu.section.tooltips");
        describedSwitch("extras.minion", cfg.minionCalculatorEnabled, v -> cfg.minionCalculatorEnabled = v);
        describedSwitch("extras.pet_rolls", cfg.petRollsEnabled, v -> cfg.petRollsEnabled = v);
        section("menu.section.storage");
        describedSwitch("extras.rarity_filter", cfg.rarityFilterEnabled, v -> cfg.rarityFilterEnabled = v);
        describedSwitch("extras.sorting", cfg.inventorySortingEnabled, v -> cfg.inventorySortingEnabled = v);
        section("menu.section.help");
        sectionButton(trText("extras.help"), b -> this.minecraft.gui.setScreen(new HelpScreen(this.mod, this)));
    }

    private void buildCookingTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        int x = this.contentX;
        int y = this.contentY + 8;
        addRenderableWidget(Checkbox.builder(trText("hud.panel.cooking"), this.font)
            .pos(x, y).selected(cfg.cookingHudEnabled)
            .onValueChange((box, checked) -> { cfg.cookingHudEnabled = checked; this.mod.saveConfig(); }).build());
        addRenderableWidget(Button.builder(Component.literal("−"), button -> {
            cfg.cookingQuantity = Math.max(1, cfg.cookingQuantity - 1);
            this.mod.saveConfig();
            rebuild();
        }).bounds(x, y + 30, 24, 20).build());
        EditBox quantity = new EditBox(this.font, x + 32, y + 30, 48, 20, trText("cooking.quantity"));
        quantity.setMaxLength(2);
        quantity.setValue(Integer.toString(cfg.cookingQuantity));
        quantity.setResponder(value -> {
            if (value.matches("[0-9]{1,2}") && Integer.parseInt(value) >= 1 && Integer.parseInt(value) <= 64) {
                cfg.cookingQuantity = Integer.parseInt(value);
                this.mod.saveConfig();
            }
        });
        addRenderableWidget(quantity);
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            cfg.cookingQuantity = Math.min(64, cfg.cookingQuantity + 1);
            this.mod.saveConfig();
            rebuild();
        }).bounds(x + 88, y + 30, 24, 20).build());
        addRenderableWidget(Button.builder(trText("cooking.clear"), button -> this.mod.getCookingTracker().clear())
            .bounds(x, y + 60, 158, 20).build());
        addRenderableWidget(Button.builder(trText("button.edit_hud_layout"), button -> this.mod.openHudLayoutEditor())
            .bounds(x + 166, y + 60, 162, 20).build());
    }

    private void buildProgressTab() {
        ConfigManager.ModConfig cfg = this.mod.getConfig();
        section("menu.section.panels");
        describedSwitch("hud.panel.profile_xp", cfg.profileXpHudEnabled, v -> cfg.profileXpHudEnabled = v);
        describedSwitch("setting.xp_hud", cfg.inventoryXpHudEnabled, this.mod::setInventoryXpHudEnabled);
        describedSwitch("hud.panel.grinding", cfg.grindingHudEnabled, v -> cfg.grindingHudEnabled = v);
        section("menu.section.actions");
        sectionButtons(trText("button.sync_profile"), button -> this.mod.runProfileCommand(),
            trText("grinding.reset"), button -> {
                this.mod.getProgressHudController().grinding().reset();
                this.mod.getProgressHudController().grinding().observeProfileXp(
                    this.mod.getProgressHudController().profile().getGainedXp(), System.currentTimeMillis());
            });
        sectionButton(trText("button.edit_hud_layout"), button -> this.mod.openHudLayoutEditor());
        section("menu.section.tips");
        sectionText(tr("progress.sync_hint"));
        sectionText(tr("grinding.pause_hint"));
        sectionText(tr("progress.estimate_hint"));
    }

    private void resetSections() {
        this.sections.clear();
        this.sectionTexts.clear();
        this.switchDescriptions.clear();
        this.layoutY = 0;
        this.openSection = null;
    }

    /** Starts a titled box; rows added after it stack inside until the next section. */
    private void section(String titleKey) {
        endSection();
        this.openSection = tr(titleKey);
        this.openSectionTop = this.layoutY;
        this.layoutY += SECTION_HEADER + 2;
    }

    private void endSection() {
        if (this.openSection == null) return;
        this.layoutY += 2;
        this.sections.add(new Section(this.openSection, this.openSectionTop, this.layoutY));
        this.layoutY += SECTION_GAP;
        this.openSection = null;
    }

    /** A switch row; its description is the label key + ".desc" when that text exists. */
    private void describedSwitch(String labelKey, boolean value, java.util.function.Consumer<Boolean> setter) {
        Checkbox box = Checkbox.builder(trText(labelKey), this.font).pos(this.contentX + SECTION_INSET, this.contentY + this.layoutY)
            .selected(value).onValueChange((b, checked) -> { setter.accept(checked); this.mod.saveConfig(); }).build();
        addRenderableWidget(box);
        boolean described = UiLocalization.has(labelKey + ".desc");
        if (described) this.switchDescriptions.put(box, tr(labelKey + ".desc"));
        this.layoutY += described ? 23 : 16;
    }

    private void sectionButton(Component label, Button.OnPress onPress) {
        addRenderableWidget(Button.builder(label, onPress).bounds(this.contentX + SECTION_INSET, this.contentY + this.layoutY + 2,
            this.contentWidth - 6 - SECTION_INSET * 2, 16).build());
        this.layoutY += 20;
    }

    private void sectionButtons(Component left, Button.OnPress onLeft, Component right, Button.OnPress onRight) {
        int width = (this.contentWidth - 6 - SECTION_INSET * 2 - 4) / 2;
        addRenderableWidget(Button.builder(left, onLeft).bounds(this.contentX + SECTION_INSET, this.contentY + this.layoutY + 2, width, 16).build());
        addRenderableWidget(Button.builder(right, onRight).bounds(this.contentX + SECTION_INSET + width + 4, this.contentY + this.layoutY + 2, width, 16).build());
        this.layoutY += 20;
    }

    private void sectionText(String text) {
        this.sectionTexts.add(new SectionText(text, this.layoutY + 2));
        this.layoutY += 11;
    }

    private void drawSections(GuiGraphicsExtractor drawContext) {
        int x = this.contentX, width = this.contentWidth - 6;
        for (Section section : this.sections) {
            int top = this.contentY + section.top(), bottom = this.contentY + section.bottom();
            drawContext.fill(x, top, x + width, bottom, COLOR_ROW_BOX);
            drawOutline(drawContext, x, top, width, bottom - top, COLOR_DIVIDER);
            drawFittedText(drawContext, section.title().toUpperCase(Locale.ROOT), x + 5, top + 4, width - 10, GOLD);
            drawContext.fill(x + 1, top + SECTION_HEADER, x + width - 1, top + SECTION_HEADER + 1, COLOR_DIVIDER);
        }
        for (SectionText text : this.sectionTexts)
            drawFittedText(drawContext, text.text(), x + SECTION_INSET + 2, this.contentY + text.y(), width - SECTION_INSET * 2 - 4, COLOR_TEXT_DIM);
    }

    private void rebuild() {
        buildTabContent();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float deltaTicks) {
        drawContext.pose().pushMatrix();
        drawContext.pose().scale(menuScale, menuScale);
        extractMenuState(drawContext, (int) (mouseX / menuScale), (int) (mouseY / menuScale), deltaTicks);
        drawContext.pose().popMatrix();
    }

    private void extractMenuState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float deltaTicks) {
        refreshPalette();
        // Dark transparent overlay
        drawContext.fill(0, 0, Math.round(this.width / menuScale), Math.round(this.height / menuScale), COLOR_BG_OVERLAY);

        // Sapphire panel
        drawSapphirePanel(drawContext);

        // Sidebar background
        drawContext.fill(this.panelX + 4, this.panelY + 28, this.panelX + SIDEBAR_WIDTH, this.panelY + this.panelHeight - 4, COLOR_SIDEBAR_BG);
        drawDivider(drawContext, this.panelX + SIDEBAR_WIDTH + 1, this.panelY + 28, this.panelX + SIDEBAR_WIDTH + 2, this.panelY + this.panelHeight - 4);

        drawTitleBar(drawContext);

        // Sidebar search and tabs
        this.searchBox.extractRenderState(drawContext, mouseX, mouseY, deltaTicks);
        for (TabButton button : this.tabButtons) {
            button.extractRenderState(drawContext, mouseX, mouseY);
        }

        // Close (X) button top-right
        drawCloseButton(drawContext, mouseX, mouseY);


        drawSubpages(drawContext, mouseX, mouseY);
        drawContext.enableScissor(contentX, contentTop, contentX + contentWidth, contentBottom);
        updateContentVisibility();
        // Render tab-specific decorations (labels for fields, etc.) before widgets
        renderTabDecorations(drawContext, mouseX, mouseY);

        // Widgets
        super.extractRenderState(drawContext, mouseX, mouseY, deltaTicks);

        // Restyle default buttons and then custom remove buttons
        drawRemoveButtons(drawContext, mouseX, mouseY);
        drawContext.disableScissor();
        if (maxPageScroll > 0) {
            int viewport = contentBottom - contentTop;
            int thumb = Math.max(12, viewport * viewport / (viewport + maxPageScroll));
            int top = contentTop + (viewport - thumb) * pageScroll.getOrDefault(currentTab, 0) / maxPageScroll;
            drawContext.fill(contentX + contentWidth - 2, top, contentX + contentWidth, top + thumb, COLOR_HEADER);
        }

        drawFooter(drawContext);

        if (this.resetHudLayoutConfirmOpen) {
            drawResetHudConfirmPopup(drawContext, mouseX, mouseY);
        }
    }

    private void renderTabDecorations(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        Font renderer = this.font;
        int x = this.contentX;
        if (isSearching()) {
            if (children().stream().noneMatch(child -> child instanceof ThemedButton))
                drawFittedText(drawContext, tr("menu.search_none"), x, contentY + 4, contentWidth - 4, COLOR_TEXT_DIM);
            return;
        }
        drawSections(drawContext);
        switch (this.currentTab) {
            case MAIN -> { } // the save shortcut is in the footer now
            case BOSSES -> {
                drawFittedCenteredText(drawContext, this.tr("bosses.collapse_hint"), x, this.contentY + 35, this.contentWidth - 4, 10, COLOR_TEXT);
                int activeListY = this.contentY + BOSSES_ACTIVE_LIST_Y_OFFSET;
                int activeListBottom = activeListY + BOSSES_ACTIVE_LIST_HEIGHT;
                int registryListY = activeListBottom + BOSSES_REGISTRY_GAP;
                drawContext.text(renderer, this.tr("bosses.tracked_by_spawn"), x, this.contentY + BOSSES_ACTIVE_LABEL_Y_OFFSET, COLOR_TEXT);
                drawBossActiveLabels(drawContext, x, activeListY);
                drawContext.text(renderer, this.tr("bosses.registry"), x, registryListY - BOSSES_REGISTRY_LABEL_OFFSET, COLOR_TEXT);
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
                drawContext.text(renderer, this.tr("money.receipts_latest"), x, fy + MONEY_RECEIPTS_TITLE_Y_OFFSET, COLOR_HEADER);
                drawMoneyHistoryLabels(drawContext, x, fy + MONEY_RECEIPTS_LIST_Y_OFFSET);
            }
            case JOBS -> {
                drawFittedCenteredText(drawContext, this.tr("jobs.estimated_hint"), x,
                    this.contentY + JOBS_TABLE_Y_OFFSET + 4 * JOBS_ROW_STEP + 30, this.contentWidth - 4, 10, COLOR_TEXT);
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
                drawContext.text(renderer, this.tr("events.existing"), x, formY + EVENTS_LIST_TITLE_Y_OFFSET, COLOR_HEADER);
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
                    drawContext.text(renderer, this.tr("language.no_languages"), x, this.contentY + 24, COLOR_TEXT_DIM);
                } else {
                    drawContext.text(renderer, this.tr("setting.chat_translate"), x + LANGUAGE_MASTER_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_MASTER_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
                    drawContext.text(renderer, this.tr("setting.translate_aggressive"), x + LANGUAGE_AGGRESSIVE_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_MASTER_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
                    drawContext.text(renderer, this.tr("setting.translate_public"), x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_PUBLIC_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
                    drawContext.text(renderer, this.tr("setting.translate_private"), x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_PRIVATE_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
                    drawContext.text(renderer, this.tr("setting.translate_system"), x + LANGUAGE_CHILD_CHECKBOX_X_OFFSET + LANGUAGE_CHECKBOX_LABEL_GAP, this.contentY + LANGUAGE_SYSTEM_CHECKBOX_Y_OFFSET + 4, COLOR_TEXT);
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
                    drawContext.text(renderer, this.tr("language.active_rules"), x, this.contentY + LANGUAGE_RULES_TITLE_Y_OFFSET, COLOR_HEADER);
                    drawTranslationRuleLabels(drawContext, x, this.contentY + LANGUAGE_RULES_LIST_Y_OFFSET);
                }
            }
            case COOKING -> {
                drawFittedText(drawContext, tr("cooking.quantity"), x + 124, this.contentY + 44, 204, COLOR_TEXT);
                List<String> lines = this.mod.getCookingTracker().getHudLines(this.mod::tr, this.mod.getConfig().cookingQuantity);
                for (int i = 0; i < Math.min(10, lines.size()); i++) {
                    drawFittedText(drawContext, lines.get(i), x, this.contentY + 108 + i * 14, this.contentWidth - 4, COLOR_TEXT);
                }
            }
        }
    }

    private void drawBossActiveLabels(GuiGraphicsExtractor drawContext, int x, int y) {
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

    private void drawBossRegistryLabels(GuiGraphicsExtractor drawContext, int x, int y) {
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

    private void drawMoneyHistoryLabels(GuiGraphicsExtractor drawContext, int x, int y) {
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


    private void drawEventsLabels(GuiGraphicsExtractor drawContext, int x, int y) {
        List<PersistentState.ScheduledEvent> events = this.mod.getEventCountdownTracker().getRawEvents();
        int listHeight = this.contentHeight - (y - this.contentY) - 4;
        int maxRows = Math.max(1, listHeight / EVENTS_ROW_STEP);
        int totalSize = events.size();
        if (totalSize == 0) {
            drawRowBox(drawContext, x, y, this.contentWidth - 4, EVENTS_ROW_HEIGHT);
            drawContext.text(this.font, tr("bosses.none"), x, y + 2, COLOR_TEXT_DIM);
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

    private void drawTranslationRuleLabels(GuiGraphicsExtractor drawContext, int x, int y) {
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

    private void drawTitleBar(GuiGraphicsExtractor drawContext) {
        drawContext.fill(this.panelX + 1, this.panelY + 24, this.panelX + PANEL_WIDTH - 1, this.panelY + 25, GOLD);
        String brand = "Minepiece QoL";
        drawContext.text(this.font, brand, this.panelX + 8, this.panelY + 8, 0xFFFFFFFF);
        int crumbX = this.panelX + 16 + this.font.width(brand);
        String version = VERSION.isEmpty() ? "" : "v" + VERSION;
        int versionWidth = this.font.width(version) + 8;
        int versionX = closeButtonX() - versionWidth - 6;
        String crumb = isSearching() ? tr("menu.search")
            : tr(groupFor(currentTab).key) + "  ›  " + currentTab.label(this);
        drawFittedText(drawContext, crumb, crumbX, this.panelY + 8, versionX - crumbX - 6, COLOR_TEXT_DIM);
        if (!version.isEmpty()) {
            drawOutline(drawContext, versionX, this.panelY + 5, versionWidth, 13, GOLD);
            drawContext.text(this.font, version, versionX + 4, this.panelY + 8, GOLD);
        }
    }

    private void drawFooter(GuiGraphicsExtractor drawContext) {
        int y = this.panelY + this.panelHeight - 14;
        int x = this.panelX + SIDEBAR_WIDTH + CONTENT_PADDING;
        x = drawKeycap(drawContext, this.mod.getHudEditorShortcutName(), x, y);
        drawContext.text(this.font, tr("menu.footer_save"), x + 4, y, COLOR_TEXT_DIM);
        x += 4 + this.font.width(tr("menu.footer_save")) + 10;
        x = drawKeycap(drawContext, "Esc", x, y);
        drawContext.text(this.font, tr("menu.footer_back"), x + 4, y, COLOR_TEXT_DIM);
        String credits = tr("menu.credits");
        drawContext.text(this.font, credits, this.panelX + PANEL_WIDTH - this.font.width(credits) - 8, y, COLOR_TEXT_DIM);
    }

    /** Draws a small key label and returns the x just after it. */
    private int drawKeycap(GuiGraphicsExtractor drawContext, String key, int x, int y) {
        int width = this.font.width(key) + 6;
        drawContext.fill(x, y - 2, x + width, y + 9, COLOR_ACTION_BUTTON);
        drawOutline(drawContext, x, y - 2, width, 11, COLOR_PANEL_BORDER);
        drawContext.text(this.font, key, x + 3, y, 0xFFFFFFFF);
        return x + width;
    }

    private void drawFittedText(GuiGraphicsExtractor drawContext, String raw, int x, int y, int maxWidth, int color) {
        String text = raw == null ? "" : raw;
        if (maxWidth <= 6) {
            return;
        }
        if (this.font.width(text) <= maxWidth) {
            drawContext.text(this.font, text, x, y, color);
            return;
        }
        String ellipsis = "...";
        int trimmedWidth = maxWidth - this.font.width(ellipsis);
        if (trimmedWidth <= 0) {
            return;
        }
        String clipped = this.font.plainSubstrByWidth(text, trimmedWidth) + ellipsis;
        drawContext.text(this.font, clipped, x, y, color);
    }

    private void drawRowBox(GuiGraphicsExtractor drawContext, int x, int y, int width, int height) {
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

    private void drawCloseButton(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        int bx = closeButtonX();
        int by = closeButtonY();
        boolean hover = closeButtonContains(mouseX, mouseY);
        int bg = hover ? COLOR_ACTION_BUTTON_HOVER : COLOR_ACTION_BUTTON;
        drawContext.fill(bx, by, bx + CLOSE_BUTTON_SIZE, by + CLOSE_BUTTON_SIZE, bg);
        drawOutline(drawContext, bx, by, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, COLOR_ACTION_BORDER);
        int textX = bx + (CLOSE_BUTTON_SIZE - this.font.width("X")) / 2;
        int textY = by + (CLOSE_BUTTON_SIZE - 8) / 2;
        drawContext.text(this.font, "X", textX, textY, 0xFFFFFFFF);
    }

    private void refreshPalette() {
        var a = this.mod.getConfig().appearance;
        COLOR_BG_OVERLAY = net.minepiece.qol.config.UiSettings.color(a.background, a.menuOpacity / 2);
        COLOR_PANEL_BG = net.minepiece.qol.config.UiSettings.color(a.background, a.menuOpacity);
        COLOR_PANEL_BORDER = net.minepiece.qol.config.UiSettings.color(a.border, a.menuOpacity);
        COLOR_SIDEBAR_BG = COLOR_PANEL_BG;
        COLOR_TAB_NORMAL = net.minepiece.qol.config.UiSettings.color(a.accent, a.menuOpacity / 5);
        COLOR_TAB_HOVER = net.minepiece.qol.config.UiSettings.color(a.accent, a.menuOpacity / 3);
        COLOR_TAB_ACTIVE = net.minepiece.qol.config.UiSettings.color(a.accent, a.menuOpacity / 2);
        COLOR_TAB_ACCENT = net.minepiece.qol.config.UiSettings.color(a.accent, 100);
        COLOR_TEXT = net.minepiece.qol.config.UiSettings.color(a.text, 100);
        COLOR_TEXT_DIM = net.minepiece.qol.config.UiSettings.color(a.text, 75);
        COLOR_HEADER = COLOR_TAB_ACCENT;
        COLOR_DIVIDER = net.minepiece.qol.config.UiSettings.color(a.border, a.menuOpacity / 2);
        COLOR_ACTION_BUTTON = COLOR_TAB_NORMAL;
        COLOR_ACTION_BUTTON_HOVER = COLOR_TAB_HOVER;
        COLOR_ACTION_BUTTON_DISABLED = COLOR_PANEL_BG;
        COLOR_ACTION_BORDER = COLOR_PANEL_BORDER;
        COLOR_ROW_BOX = COLOR_TAB_NORMAL;
        COLOR_ROW_BOX_BORDER = COLOR_DIVIDER;
    }

    private void drawSapphirePanel(GuiGraphicsExtractor drawContext) {
        drawContext.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + this.panelHeight, COLOR_PANEL_BG);
        drawContext.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + 24, COLOR_TAB_NORMAL);
        drawOutline(drawContext, this.panelX, this.panelY, PANEL_WIDTH, this.panelHeight, COLOR_PANEL_BORDER);
    }

    private void drawOutline(GuiGraphicsExtractor drawContext, int x, int y, int width, int height, int color) {
        for (int i = 0; i < this.mod.getConfig().appearance.borderWidth; i++) {
            drawContext.fill(x + i, y + i, x + width - i, y + i + 1, color);
            drawContext.fill(x + i, y + height - i - 1, x + width - i, y + height - i, color);
            drawContext.fill(x + i, y + i, x + i + 1, y + height - i, color);
            drawContext.fill(x + width - i - 1, y + i, x + width - i, y + height - i, color);
        }
    }

    private void drawDivider(GuiGraphicsExtractor drawContext, int x1, int y1, int x2, int y2) {
        drawContext.fill(x1, y1, x2, y2, COLOR_DIVIDER);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        return clickMenu(scaledClick(click), doubled);
    }

    private boolean clickMenu(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        if (!this.resetHudLayoutConfirmOpen && this.searchBox.isMouseOver(click.x(), click.y())) {
            setFocused(this.searchBox);
            this.searchBox.mouseClicked(click, doubled);
            return true;
        }
        if (getFocused() == this.searchBox) setFocused(null);
        if (!this.resetHudLayoutConfirmOpen && click.button() == 0 && !isSearching()) {
            Tab[] pages = groupFor(currentTab).pages();
            int[][] chips = subpageChips(pages);
            for (int i = 0; i < pages.length; i++) {
                int[] r = chips[i];
                if (click.x() >= r[0] && click.x() < r[0] + r[2] && click.y() >= r[1] && click.y() < r[1] + r[3]) {
                    if (currentTab != pages[i]) { currentTab = pages[i]; rebuild(); }
                    return true;
                }
            }
        }
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
            if (inContent(click.x(), click.y()) && action.contains((int) click.x(), (int) click.y())) {
                action.action().run();
                return true;
            }
        }

        // Close (X) button
        if (closeButtonContains(click.x(), click.y())) {
            onClose();
            return true;
        }
        // Sidebar tab clicks
        for (TabButton button : this.tabButtons) {
            if (button.contains((int) click.x(), (int) click.y())) {
                if (this.currentTab != button.tab || isSearching()) {
                    Group group = groupFor(button.tab);
                    this.currentTab = selectedPages.getOrDefault(group, button.tab);
                    this.searchQuery = "";
                    this.searchBox.setValue("");
                    rebuild();
                }
                return true;
            }
        }
        return inContent(click.x(), click.y()) && super.mouseClicked(click, doubled);
    }

    private net.minecraft.client.input.MouseButtonEvent scaledClick(net.minecraft.client.input.MouseButtonEvent click) {
        return new net.minecraft.client.input.MouseButtonEvent(click.x() / menuScale, click.y() / menuScale, click.buttonInfo());
    }

    @Override public void mouseMoved(double x, double y) { super.mouseMoved(x / menuScale, y / menuScale); }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent click) { return super.mouseReleased(scaledClick(click)); }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double dx, double dy) {
        return super.mouseDragged(scaledClick(click), dx / menuScale, dy / menuScale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        mouseX /= menuScale; mouseY /= menuScale;
        if (!inContent(mouseX, mouseY)) return false;
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
                    int nextOffset = clampOffset(this.bossActiveOffset + step, maxOffset);
                    handled = nextOffset != this.bossActiveOffset;
                    this.bossActiveOffset = nextOffset;
                }

                int registryY = activeY + activeHeight + BOSSES_REGISTRY_GAP;
                int registryHeight = BOSSES_REGISTRY_LIST_HEIGHT;
                int registryMaxRows = Math.max(1, registryHeight / BOSSES_REGISTRY_ROW_STEP);
                if (containsRect(mx, my, this.contentX, registryY, this.contentWidth, registryHeight)) {
                    int totalRows = this.mod.getBossTracker().getBossRegistry().size();
                    int maxOffset = Math.max(0, totalRows - registryMaxRows);
                    int nextOffset = clampOffset(this.bossRegistryOffset + step, maxOffset);
                    handled = nextOffset != this.bossRegistryOffset;
                    this.bossRegistryOffset = nextOffset;
                }
            }
            case MONEY -> {
                int listY = this.contentY + 26 + MONEY_RECEIPTS_LIST_Y_OFFSET;
                int listHeight = this.contentHeight - (listY - this.contentY) - 4;
                int maxRows = Math.max(1, listHeight / MONEY_RECEIPT_ROW_STEP);
                if (containsRect(mx, my, this.contentX, listY, this.contentWidth, listHeight)) {
                    int totalRows = this.mod.getMoneyTracker().getHistory().size();
                    int maxOffset = Math.max(0, totalRows - maxRows);
                    int nextOffset = clampOffset(this.moneyHistoryOffset + step, maxOffset);
                    handled = nextOffset != this.moneyHistoryOffset;
                    this.moneyHistoryOffset = nextOffset;
                }
            }
            case EVENTS -> {
                int listY = this.contentY + 26 + EVENTS_LIST_START_Y_OFFSET;
                int listHeight = this.contentHeight - (listY - this.contentY) - 4;
                int maxRows = Math.max(1, listHeight / EVENTS_ROW_STEP);
                if (containsRect(mx, my, this.contentX, listY, this.contentWidth, listHeight)) {
                    int totalRows = this.mod.getEventCountdownTracker().getRawEvents().size();
                    int maxOffset = Math.max(0, totalRows - maxRows);
                    int nextOffset = clampOffset(this.eventsOffset + step, maxOffset);
                    handled = nextOffset != this.eventsOffset;
                    this.eventsOffset = nextOffset;
                }
            }
            case LANGUAGE -> {
                int listY = this.contentY + LANGUAGE_RULES_LIST_Y_OFFSET;
                int listHeight = this.contentHeight - (listY - this.contentY) - 4;
                int maxRows = Math.max(1, listHeight / LANGUAGE_RULE_ROW_STEP);
                if (containsRect(mx, my, this.contentX, listY, this.contentWidth, listHeight)) {
                    int totalRows = this.mod.getConfig().chatTranslationRules.size();
                    int maxOffset = Math.max(0, totalRows - maxRows);
                    int nextOffset = clampOffset(this.translationRulesOffset + step, maxOffset);
                    handled = nextOffset != this.translationRulesOffset;
                    this.translationRulesOffset = nextOffset;
                }
            }
            default -> {
            }
        }

        if (handled) {
            rebuild();
            return true;
        }
        if (inContent(mouseX, mouseY) && maxPageScroll > 0 && Math.abs(verticalAmount) > 0.001) {
            int before = pageScroll.getOrDefault(currentTab, 0);
            int after = Math.clamp(before - (int) Math.round(verticalAmount * 22), 0, maxPageScroll);
            int change = after - before;
            pageScroll.put(currentTab, after);
            contentY -= change;
            for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) widget.setY(widget.getY() - change);
            for (int i = 0; i < removeActions.size(); i++) {
                var old = removeActions.get(i); removeActions.set(i, new RemoveAction(old.x(), old.y() - change, old.action()));
            }
            updateContentVisibility();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.gui.setScreen(null);
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
        return this.panelY + (this.panelHeight - RESET_CONFIRM_HEIGHT) / 2;
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

    private void drawResetHudConfirmPopup(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        int x = resetConfirmX();
        int y = resetConfirmY();
        int acceptX = x + RESET_CONFIRM_WIDTH / 2 - RESET_CONFIRM_BUTTON_GAP - RESET_CONFIRM_BUTTON_SIZE;
        int cancelX = x + RESET_CONFIRM_WIDTH / 2 + RESET_CONFIRM_BUTTON_GAP;
        int buttonsY = y + RESET_CONFIRM_HEIGHT - RESET_CONFIRM_BUTTON_SIZE - 10;

        drawContext.fill(this.panelX + 3, this.panelY + 25, this.panelX + PANEL_WIDTH - 3, this.panelY + this.panelHeight - 3, 0x8801030B);
        drawContext.fill(x, y, x + RESET_CONFIRM_WIDTH, y + RESET_CONFIRM_HEIGHT, 0xEE0E2650);
        drawOutline(drawContext, x, y, RESET_CONFIRM_WIDTH, RESET_CONFIRM_HEIGHT, COLOR_ACTION_BORDER);

        drawContext.centeredText(
            this.font,
            this.tr("main.reset_hud_confirm_title"),
            x + RESET_CONFIRM_WIDTH / 2,
            y + 16,
            COLOR_HEADER
        );
        drawContext.centeredText(
            this.font,
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

        int checkX = acceptX + (RESET_CONFIRM_BUTTON_SIZE - this.font.width("✓")) / 2;
        int xX = cancelX + (RESET_CONFIRM_BUTTON_SIZE - this.font.width("X")) / 2;
        int textY = buttonsY + (RESET_CONFIRM_BUTTON_SIZE - 8) / 2;
        drawContext.text(this.font, "✓", checkX, textY, 0xFFFFFFFF);
        drawContext.text(this.font, "X", xX, textY, 0xFFFFFFFF);
    }

    private boolean isEditingEvent() {
        return !this.editingEventOriginalName.isBlank() && !this.editingEventOriginalTime.isBlank();
    }

    private void beginEventEdit(String name, String time) {
        this.editingEventOriginalName = name == null ? "" : name;
        this.editingEventOriginalTime = time == null ? "" : time;
        if (this.eventNameField != null) {
            this.eventNameField.setValue(this.editingEventOriginalName);
        }
        if (this.eventTimeField != null) {
            this.eventTimeField.setValue(this.editingEventOriginalTime);
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

    private Component trText(String key) {
        return Component.literal(tr(key));
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

        void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
            boolean active = groupFor(MinepieceMenuScreen.this.currentTab) == groupFor(this.tab);
            boolean hover = contains(mouseX, mouseY);
            int bg = active ? COLOR_TAB_ACTIVE : (hover ? COLOR_TAB_HOVER : COLOR_TAB_NORMAL);
            drawContext.fill(this.x, this.y, this.x + this.width, this.y + this.height, bg);
            drawOutline(drawContext, this.x, this.y, this.width, this.height, active ? COLOR_TAB_ACCENT : COLOR_DIVIDER);
            Group group = groupFor(this.tab);
            boolean icon = this.height >= 18;
            if (icon) drawContext.item(new net.minecraft.world.item.ItemStack(group.icon()), this.x + 4, this.y + (this.height - 16) / 2);
            if (active) drawContext.fill(this.x + this.width - 7, this.y + this.height / 2 - 2, this.x + this.width - 3, this.y + this.height / 2 + 2, GOLD);
            int textX = this.x + (icon ? 24 : 8);
            drawFittedText(drawContext, tr(group.key), textX, this.y + (this.height - 8) / 2, this.x + this.width - 10 - textX, active ? 0xFFFFFFFF : COLOR_TEXT);
        }
    }

    private void drawRemoveButtons(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        for (RemoveAction action : this.removeActions) {
            boolean hover = action.contains(mouseX, mouseY);
            int bg = hover ? COLOR_ACTION_BUTTON_HOVER : COLOR_ACTION_BUTTON;
            int border = COLOR_ACTION_BORDER;
            int x = action.x();
            int y = action.y();
            drawContext.fill(x, y, x + REMOVE_BUTTON_SIZE, y + REMOVE_BUTTON_SIZE, bg);
            drawOutline(drawContext, x, y, REMOVE_BUTTON_SIZE, REMOVE_BUTTON_SIZE, border);
            int textX = x + (REMOVE_BUTTON_SIZE - this.font.width("X")) / 2;
            int textY = y + (REMOVE_BUTTON_SIZE - 8) / 2;
            drawContext.text(this.font, "X", textX, textY, 0xFFFFFFFF);
        }
    }

    private void drawFittedCenteredText(GuiGraphicsExtractor drawContext, String text, int x, int y, int width, int height, int color) {
        String value = text == null ? "" : text;
        int maxWidth = Math.max(2, width - 8);
        if (this.font.width(value) > maxWidth) {
            String ellipsis = "...";
            int trimWidth = Math.max(2, maxWidth - this.font.width(ellipsis));
            value = this.font.plainSubstrByWidth(value, trimWidth) + ellipsis;
        }
        int tx = x + (width - this.font.width(value)) / 2;
        int ty = y + (height - 8) / 2;
        drawContext.text(this.font, value, tx, ty, color);
    }

    private record RemoveAction(int x, int y, Runnable action) {
        boolean contains(int mx, int my) {
            return mx >= this.x && mx < this.x + REMOVE_BUTTON_SIZE && my >= this.y && my < this.y + REMOVE_BUTTON_SIZE;
        }
    }

    private boolean inContent(double x, double y) {
        return x >= contentX && x < contentX + contentWidth && y >= contentTop && y < contentBottom;
    }

    private void updateContentVisibility() {
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && child != this.searchBox)
            widget.visible = widget.getY() >= contentTop && widget.getY() + widget.getHeight() <= contentBottom;
    }

    /** One {x, y, width, height} per page tab; they share the row and shrink evenly when the labels are long. */
    private int[][] subpageChips(Tab[] pages) {
        int gap = 4, y = panelY + 32, height = 16, available = contentWidth - 4;
        int[] widths = new int[pages.length];
        int total = gap * (pages.length - 1);
        for (int i = 0; i < pages.length; i++) total += widths[i] = this.font.width(pages[i].label(this)) + 14;
        if (total > available) java.util.Arrays.fill(widths, (available - gap * (pages.length - 1)) / pages.length);
        int[][] chips = new int[pages.length][];
        int x = contentX;
        for (int i = 0; i < pages.length; i++) {
            chips[i] = new int[]{x, y, widths[i], height};
            x += widths[i] + gap;
        }
        return chips;
    }

    private void drawSubpages(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (isSearching()) return;
        Tab[] pages = groupFor(currentTab).pages();
        int[][] chips = subpageChips(pages);
        for (int i = 0; i < pages.length; i++) {
            int[] r = chips[i];
            boolean active = pages[i] == currentTab;
            boolean hover = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
            context.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], active ? COLOR_TAB_ACTIVE : hover ? COLOR_TAB_HOVER : COLOR_TAB_NORMAL);
            drawOutline(context, r[0], r[1], r[2], r[3], active ? GOLD : COLOR_DIVIDER);
            drawFittedCenteredText(context, pages[i].label(this), r[0] + 3, r[1], r[2] - 6, r[3], active ? 0xFFFFFFFF : COLOR_TEXT_DIM);
        }
    }

    private enum Group {
        GENERAL("menu.group.general"), HUD("menu.group.hud"), COMBAT("menu.group.combat"),
        PROGRESS("menu.group.progress"), ECONOMY("menu.group.economy"), TOOLS("menu.group.tools");
        final String key;
        Group(String key) { this.key = key; }
        net.minecraft.world.item.Item icon() {
            return switch (this) {
                case GENERAL -> net.minecraft.world.item.Items.COMPASS;
                case HUD -> net.minecraft.world.item.Items.PAINTING;
                case COMBAT -> net.minecraft.world.item.Items.IRON_SWORD;
                case PROGRESS -> net.minecraft.world.item.Items.EXPERIENCE_BOTTLE;
                case ECONOMY -> net.minecraft.world.item.Items.GOLD_INGOT;
                case TOOLS -> net.minecraft.world.item.Items.IRON_PICKAXE;
            };
        }
        Tab[] pages() {
            return switch (this) {
                case GENERAL -> new Tab[]{Tab.MAIN, Tab.LANGUAGE};
                case HUD -> new Tab[]{Tab.APPEARANCE};
                case COMBAT -> new Tab[]{Tab.BOSSES, Tab.COMBAT_SETTINGS};
                case PROGRESS -> new Tab[]{Tab.PROGRESS, Tab.STAT_DISPLAY, Tab.PROFILE, Tab.PASS_PETS};
                case ECONOMY -> new Tab[]{Tab.MONEY, Tab.JOBS, Tab.AUCTION_HOUSE, Tab.COOKING};
                case TOOLS -> new Tab[]{Tab.EVENTS, Tab.SCROLLS, Tab.EXTRAS, Tab.HELP};
            };
        }
    }

    private static Group groupFor(Tab tab) {
        for (var group : Group.values()) for (var page : group.pages()) if (page == tab) return group;
        throw new IllegalArgumentException("Unknown page " + tab);
    }

    private enum Tab {
        MAIN("tab.main"),
        BOSSES("tab.bosses"),
        MONEY("tab.money"),
        JOBS("tab.jobs"),
        EVENTS("tab.events"),
        PROFILE("tab.profile"),
        LANGUAGE("tab.language"),
        AUCTION_HOUSE("ui.auction_house"),
        PROGRESS("tab.progress"),
        COOKING("hud.panel.cooking"),
        EXTRAS("ui.inventory_helpers"),
        APPEARANCE("ui.hud_customization"),
        COMBAT_SETTINGS("bosses.settings"),
        STAT_DISPLAY("ui.selected_stats"),
        PASS_PETS("ui.pass_pets"),
        SCROLLS("hud.panel.scrolls"),
        HELP("extras.help");

        final String key;

        Tab(String key) {
            this.key = key;
        }

        String label(MinepieceMenuScreen screen) {
            return screen.tr(this.key);
        }
    }
}
