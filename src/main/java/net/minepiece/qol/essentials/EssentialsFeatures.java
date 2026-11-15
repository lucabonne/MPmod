package net.minepiece.qol.essentials;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.parse.TooltipParsers;
import net.minepiece.qol.state.PersistentState;
import net.minepiece.qol.state.RarityDetector;
import net.minepiece.qol.util.TextUtil;

/** Aure64's extra features adapted around QoL's existing trackers and HUD. */
public final class EssentialsFeatures {
    private static final Pattern LEVEL = Pattern.compile("(?:Level|Niveau|Nivel|Stufe|Livello|Nível|Poziom|Seviye)\\s*:\\s*(\\d+|Max)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROLL = Pattern.compile("\\(\\d+(?:[.,]\\d+)?%\\)\\s*$");
    private final MinepieceQolClient mod;
    private final PersistentState state;
    private final Runnable save;
    private final BackgroundMenus background = new BackgroundMenus();
    private final DailyQuests quests = new DailyQuests();
    private final PassRefreshSchedule passRefresh = new PassRefreshSchedule();
    private final BossAlerts alerts = new BossAlerts();
    private final UpdateNotice updateNotice = new UpdateNotice();
    private List<String> pets = List.of();
    private boolean confirmedServer;
    private boolean manualRefresh;
    private int pending = 7;
    private long joined, lastBossRead, nextScan;
    private int fingerprint;
    private Object lastScreen;

    public EssentialsFeatures(MinepieceQolClient mod, PersistentState state, Runnable save) {
        this.mod = mod; this.state = state; this.save = save;
        if (state.minionResourceXp == null) state.minionResourceXp = new java.util.LinkedHashMap<>();
    }
    public BackgroundMenus background() { return background; }
    public DailyQuests quests() { return quests; }
    public List<String> petLines() { return pets; }
    public void refresh() { pending = 7; manualRefresh = true; }
    public void reset() {
        background.reset(); quests.clear(); alerts.reset(); passRefresh.reset(); pets = List.of();
        confirmedServer = false; manualRefresh = false; pending = 7; joined = System.currentTimeMillis(); lastBossRead = nextScan = 0;
        lastScreen = null; fingerprint = 0;
    }
    public void observeServer(String text) {
        String caps = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡʏᴢ";
        String ascii = "abcdefghijklmnopqrstuvwyz";
        StringBuilder normalized = new StringBuilder();
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            int i = caps.indexOf(c); normalized.append(i < 0 ? c : ascii.charAt(i));
        }
        if (normalized.toString().contains("minepiece")) confirmedServer = true;
    }
    public boolean onServer() {
        var mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) observeServer(mc.getCurrentServer().ip);
        return confirmedServer;
    }
    public void onServerMessage(String text) {
        quests.onServerMessage(text);
        net.minepiece.qol.parse.ChatParsers.parseBossKill(text).ifPresent(ignored -> passRefresh.onBossKilled(System.currentTimeMillis()));
    }
    public void tick(Minecraft mc) {
        updateNotice.tick(mod);
        background.tick();
        var cfg = mod.getConfig();
        if (!cfg.modEnabled || !onServer()) { if (background.busy()) background.cancel(); return; }
        long now = System.currentTimeMillis();
        if (quests.resetDay(LocalDate.now())) pending |= 4;
        if (cfg.allFeaturesVisible && cfg.bossAlertsEnabled && mod.isBossTrackingEnabled()) alerts.tick(mod);
        if (now >= nextScan) {
            nextScan = now + 200;
            if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
                int hash = screen.getMenu().slots.stream().mapToInt(s -> 31 * s.getItem().getCount() + s.getItem().getComponents().hashCode()).reduce(1, (a,b) -> 31*a+b);
                if (lastScreen != screen || hash != fingerprint) {
                    lastScreen = screen; fingerprint = hash;
                    List<ItemStack> stacks = screen.getMenu().slots.stream().filter(s -> s.container != mc.player.getInventory()).map(s -> s.getItem()).toList();
                    if (cfg.dailyQuestsEnabled) scanQuests(stacks);
                    if (cfg.activePetsEnabled) scanPets(stacks);
                    if (cfg.minionCalculatorEnabled) learnResource(stacks);
                }
            } else lastScreen = null;
        }
        if ((!cfg.backgroundRefreshEnabled && !manualRefresh) || background.busy() || mc.gui.screen() != null
            || mod.getStatsRefreshController().isAwaitingProfileSync()
            || !manualRefresh && now - joined < 10000) return;
        boolean automaticBossAllowed = cfg.automaticBossReads && !mod.getBossTracker().getCurrentSpawnId().isBlank();
        pending &= (mod.isBossTrackingEnabled() && (manualRefresh || automaticBossAllowed) ? 1 : 0) | (cfg.activePetsEnabled ? 2 : 0) | (cfg.dailyQuestsEnabled ? 4 : 0);
        if (pending == 0) manualRefresh = false;
        if (cfg.backgroundRefreshEnabled && automaticBossAllowed && mod.isBossTrackingEnabled() && now - lastBossRead >= 300000) pending |= 1;
        if (cfg.backgroundRefreshEnabled && cfg.dailyQuestsEnabled && passRefresh.due(now, !quests.lines().isEmpty())) pending |= 4;
        // Mission snapshots take priority over periodic boss/pet reads after a kill.
        if ((pending & 4) != 0 && cfg.dailyQuestsEnabled) {
            if (background.request("pass quests", this::isQuestMenu, this::scanQuests)) {
                pending &= ~4;
                passRefresh.readStarted(now);
            }
            return;
        }
        if ((pending & 1) != 0 && mod.isBossTrackingEnabled()) {
            if (background.request("boss", this::isBossMenu, this::scanBosses)) { pending &= ~1; lastBossRead = now; }
        } else if ((pending & 2) != 0 && cfg.activePetsEnabled) {
            if (background.request("pets", this::isPetMenu, this::scanPets)) pending &= ~2;
        }
    }
    private boolean isBossMenu(List<ItemStack> stacks) {
        return stacks.stream().anyMatch(s -> !TooltipParsers.parseBossTooltips(normalized(s), s.getHoverName().getString()).isEmpty());
    }
    private void scanBosses(List<ItemStack> stacks) {
        for (var stack : stacks) mod.getBossTracker().captureParsedTooltips(stack.getHoverName().getString(),
            TooltipParsers.parseBossTooltips(normalized(stack), stack.getHoverName().getString()));
    }
    private boolean isQuestMenu(List<ItemStack> stacks) { return stacks.stream().anyMatch(s -> DailyQuests.parse(s.getHoverName().getString(), ItemText.lore(s)).isPresent()); }
    public boolean scanQuests(List<ItemStack> stacks) {
        boolean replaced = quests.replace(stacks.stream().map(s -> DailyQuests.parse(s.getHoverName().getString(), ItemText.lore(s))).flatMap(java.util.Optional::stream).toList());
        if (replaced) passRefresh.snapshotRead(System.currentTimeMillis());
        return replaced;
    }
    private boolean isPetMenu(List<ItemStack> stacks) { return stacks.stream().anyMatch(s -> s.is(Items.RABBIT_FOOT) && petAction(ItemText.lore(s)) != 0); }
    public boolean scanPets(List<ItemStack> stacks) {
        if (!isPetMenu(stacks)) return false;
        List<String> lines = new ArrayList<>();
        Map<PetStat, Double> totals = new EnumMap<>(PetStat.class);
        for (var stack : stacks) {
            var lore = ItemText.lore(stack);
            if (!stack.is(Items.RABBIT_FOOT) || petAction(lore) != 1) continue;
            int level = level(lore);
            lines.add(stack.getHoverName().getString() + (level > 0 ? " • " + mod.tr("jobs.level") + " " + level : ""));
            boolean inEffects = false;
            for (String line : lore) {
                String lower = line.toLowerCase(Locale.ROOT);
                if (TooltipParsers.isMinionEffectsHeader(TextUtil.normalize(line)) || lower.contains("effets du minion")) break;
                if (TooltipParsers.isPetEffectsHeader(TextUtil.normalize(line))) { inEffects = true; continue; }
                if (inEffects) PetEffectParser.parse(line).ifPresent(e -> totals.merge(e.stat(), e.value(), Double::sum));
            }
        }
        for (var entry : totals.entrySet()) {
            String key = switch (entry.getKey()) {
                case VITALITY -> "health"; case CRIT_CHANCE -> "critical_chance"; case CRIT_DAMAGE -> "critical_damage";
                case ENERGY_REGEN -> "energy_regeneration"; case REGEN -> "regeneration";
                default -> entry.getKey().name().toLowerCase(Locale.ROOT);
            };
            lines.add(mod.tr("profile." + key) + " +" + number(entry.getValue()));
        }
        pets = lines.isEmpty() ? List.of(mod.tr("extras.no_active_pets")) : List.copyOf(lines);
        return true;
    }
    private static int petAction(List<String> lore) {
        for (String line : lore) {
            String lower = TextUtil.normalize(Component.literal(line)).toLowerCase(Locale.ROOT);
            if (lower.matches(".*\\b(?:désactiver|disable|deactivate|desactivar|disattiva|deaktivieren|desativar|wyłącz|nonaktifkan|devre dışı bırak)\\b.*")) return 1;
            if (lower.matches(".*\\b(?:activer|activate|activar|attiva|aktivieren|ativar|włącz|aktifkan|etkinleştir)\\b.*")) return -1;
        }
        return 0;
    }
    private static int level(List<String> lines) {
        for (String line : lines) {
            var m = LEVEL.matcher(line);
            if (m.find()) try { return m.group(1).equalsIgnoreCase("Max") ? 20 : Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) { }
        }
        return 0;
    }
    private static List<String> normalized(ItemStack s) { return ItemText.nameAndLore(s).stream().map(v -> TextUtil.normalize(Component.literal(v))).toList(); }
    private String resourceKey(String id) {
        var mc = Minecraft.getInstance();
        String server = mc.getCurrentServer() == null ? "local" : mc.getCurrentServer().ip;
        return server + "|" + mc.getUser().getProfileId() + "|" + id;
    }
    private void learnResource(List<ItemStack> stacks) {
        for (var stack : stacks) {
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            if (!(name.contains("minion") && (name.contains("nourrir") || name.contains("feed") || name.contains("aliment") || name.contains("nutri")))) continue;
            for (String line : ItemText.lore(stack)) MinionFeedLine.parse(line).ifPresent(feed -> {
                for (var resource : stacks) {
                    if (!resource.isEmpty() && resource.getHoverName().getString().equals(feed.resourceName())) {
                        String key = resourceKey(BuiltInRegistries.ITEM.getKey(resource.getItem()).toString());
                        Double old = state.minionResourceXp.put(key, feed.xpPerItem());
                        if (old == null || Double.compare(old, feed.xpPerItem()) != 0) save.run();
                        break;
                    }
                }
            });
        }
    }
    public void annotate(ItemStack stack, List<Component> lines) {
        var cfg = mod.getConfig();
        if (!cfg.modEnabled || !cfg.allFeaturesVisible || !stack.is(Items.RABBIT_FOOT)) return;
        if (cfg.petRollsEnabled) {
            // QoL handles Mythic and Legendary. Fill only the rarities it did not previously cover.
            var rarity = RarityDetector.detect(stack, lines).map(r -> Rarity.fromTrack(r.name())).orElse(null);
            if (rarity == Rarity.COMMON || rarity == Rarity.RARE || rarity == Rarity.EPIC) {
                boolean effects = false;
                int level = level(ItemText.lore(stack));
                for (int i = 0; i < lines.size(); i++) {
                    String raw = lines.get(i).getString(), lower = raw.toLowerCase(Locale.ROOT);
                    if (TooltipParsers.isMinionEffectsHeader(TextUtil.normalize(raw)) || lower.contains("effets du minion")) break;
                    if (TooltipParsers.isPetEffectsHeader(TextUtil.normalize(raw))) { effects = true; continue; }
                    if (!effects || ROLL.matcher(raw).find()) continue;
                    var effect = PetEffectParser.parse(raw).orElse(null);
                    if (effect == null) continue;
                    int scale = Math.max(level, effect.tier());
                    var quality = PetStatEvaluator.quality(rarity, new PetEffect(scale, effect.stat(), effect.value(), effect.label()));
                    if (quality.isEmpty()) continue;
                    long percent = Math.round(quality.getAsDouble() * 100);
                    lines.set(i, lines.get(i).copy().append(Component.literal(" (" + percent + "%)").withColor(percent >= 90 ? 0x55FF55 : percent >= 75 ? 0xFFFF55 : percent >= 50 ? 0xFFAA00 : 0xFF5555)));
                }
            }
        }
        if (!cfg.minionCalculatorEnabled || lines.stream().anyMatch(l -> l.getStyle().getInsertion() != null && l.getStyle().getInsertion().equals("minepiece-qol:minion"))) return;
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        var minion = custom == null ? null : MinionNbt.parseTag(custom.copyTag()).orElse(null);
        if (minion == null) return;
        if (minion.isMaxed()) { addMinionLine(lines, mod.tr("extras.minion_maxed")); return; }
        Double per = state.minionResourceXp.get(resourceKey(MinionData.tokenToItemId(minion.foodToken())));
        boolean known = per != null && Double.isFinite(per) && per > 0;
        double next = MinionCost.remainingToNextPrestige(minion.currentXp(), minion.nextXp());
        double max = MinionCost.remainingToMax(minion.prestige(), minion.currentXp(), minion.nextXp());
        String suffix = known ? " " + mod.tr("extras.stacks") : " XP";
        addMinionLine(lines, mod.tr("extras.next_prestige") + ": " + number(Math.ceil(known ? next / (64 * per) : next)) + suffix);
        addMinionLine(lines, mod.tr("extras.max_prestige") + ": " + number(Math.ceil(known ? max / (64 * per) : max)) + suffix);
        if (!known) addMinionLine(lines, mod.tr("extras.feed_hint"));
    }
    private static void addMinionLine(List<Component> lines, String text) {
        lines.add(Component.literal(text).withStyle(s -> s.withColor(0xF0A857).withInsertion("minepiece-qol:minion")));
    }
    private static String number(double value) { return java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
}
