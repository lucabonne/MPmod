package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minepiece.qol.parse.TooltipParsers;

public final class ProfileStatsTracker {
    private Double health;
    private Double strength;
    private Double damage;
    private Double criticalChance;
    private Double criticalDamage;
    private Double power;
    private Double energy;
    private Double energyRegeneration;
    private Double speed;
    private Double dexterity;
    private Double defense;
    private Double regeneration;
    private long lastUpdateMs;
    private boolean awaitingProfileHoverSync;
    private int lastSelectedSlot = -1;
    private final Map<Integer, TooltipParsers.ProfileStatsData> learnedSlotStats = new LinkedHashMap<>();
    private PersistentState persistentState;
    private Consumer<PersistentState> stateSaver;

    public void attachPersistence(PersistentState persistentState, Consumer<PersistentState> stateSaver) {
        this.persistentState = persistentState;
        this.stateSaver = stateSaver;
        if (persistentState == null || persistentState.profileStats == null) {
            return;
        }
        this.health = persistentState.profileStats.health;
        this.strength = persistentState.profileStats.strength;
        this.damage = persistentState.profileStats.damage;
        this.criticalChance = persistentState.profileStats.criticalChance;
        this.criticalDamage = persistentState.profileStats.criticalDamage;
        this.power = persistentState.profileStats.power;
        this.energy = persistentState.profileStats.energy;
        this.energyRegeneration = persistentState.profileStats.energyRegeneration;
        this.speed = persistentState.profileStats.speed;
        this.dexterity = persistentState.profileStats.dexterity;
        this.defense = persistentState.profileStats.defense;
        this.regeneration = persistentState.profileStats.regeneration;
    }

    public Double getHealth() {
        return this.health;
    }

    public Double getPower() {
        return this.power;
    }

    public Double getStrength() {
        return this.strength;
    }

    public Double getDamage() {
        return this.damage;
    }

    public Double getCriticalChance() {
        return this.criticalChance;
    }

    public Double getCriticalDamage() {
        return this.criticalDamage;
    }

    public Double getEnergy() {
        return this.energy;
    }

    public Double getEnergyRegeneration() {
        return this.energyRegeneration;
    }

    public Double getSpeed() {
        return this.speed;
    }

    public Double getDexterity() {
        return this.dexterity;
    }

    public Double getDefense() {
        return this.defense;
    }

    public Double getRegeneration() {
        return this.regeneration;
    }

    public void setStats(
        double health,
        double strength,
        double damage,
        double criticalChance,
        double criticalDamage,
        double power,
        double energy,
        double energyRegeneration,
        double speed,
        double dexterity,
        double defense,
        double regeneration
    ) {
        this.health = health;
        this.strength = strength;
        this.damage = damage;
        this.criticalChance = criticalChance;
        this.criticalDamage = criticalDamage;
        this.power = power;
        this.energy = energy;
        this.energyRegeneration = energyRegeneration;
        this.speed = speed;
        this.dexterity = dexterity;
        this.defense = defense;
        this.regeneration = regeneration;
        this.lastUpdateMs = System.currentTimeMillis();
        persistIfPossible();
    }

    public void update(TooltipParsers.ProfileStatsData statsData, int selectedSlot, boolean rememberSlot) {
        if (statsData == null) {
            return;
        }
        applySnapshot(statsData);
        this.lastUpdateMs = System.currentTimeMillis();
        if (rememberSlot && selectedSlot >= 0) {
            this.learnedSlotStats.put(selectedSlot, statsData);
        }
        persistIfPossible();
    }

    public void armProfileHoverSync() {
        this.awaitingProfileHoverSync = true;
    }

    public boolean consumeProfileHoverSync(TooltipParsers.ProfileStatsData statsData) {
        if (!this.awaitingProfileHoverSync || statsData == null) {
            return false;
        }
        if (statsData.health() == null
            || statsData.strength() == null
            || statsData.damage() == null
            || statsData.criticalChance() == null
            || statsData.criticalDamage() == null
            || statsData.power() == null
            || statsData.energy() == null
            || statsData.energyRegeneration() == null
            || statsData.speed() == null
            || statsData.dexterity() == null
            || statsData.defense() == null
            || statsData.regeneration() == null) {
            return false;
        }
        this.awaitingProfileHoverSync = false;
        update(statsData, this.lastSelectedSlot, false);
        return true;
    }

    public boolean isAwaitingProfileHoverSync() {
        return this.awaitingProfileHoverSync;
    }

    public void onSelectedSlotChanged(int selectedSlot) {
        this.lastSelectedSlot = selectedSlot;
        TooltipParsers.ProfileStatsData remembered = this.learnedSlotStats.get(selectedSlot);
        if (remembered != null) {
            applySnapshot(remembered);
            this.lastUpdateMs = System.currentTimeMillis();
            persistIfPossible();
        }
    }

    public List<String> getHudLines() {
        return getHudLines(null);
    }

    public List<String> getHudLines(Function<String, String> localizer) {
        List<String> lines = new ArrayList<>(3);
        lines.add(localized(localizer, "profile.power", "Power") + ": " + formatValue(this.power));
        lines.add(localized(localizer, "profile.strength", "Strength") + ": " + formatValue(this.strength));
        lines.add(localized(localizer, "profile.speed", "Speed") + ": " + formatValue(this.speed));
        return lines;
    }

    public long getLastUpdateMs() {
        return this.lastUpdateMs;
    }

    private void applySnapshot(TooltipParsers.ProfileStatsData statsData) {
        this.health = statsData.health();
        this.strength = statsData.strength();
        this.damage = statsData.damage();
        this.criticalChance = statsData.criticalChance();
        this.criticalDamage = statsData.criticalDamage();
        this.power = statsData.power();
        this.energy = statsData.energy();
        this.energyRegeneration = statsData.energyRegeneration();
        this.speed = statsData.speed();
        this.dexterity = statsData.dexterity();
        this.defense = statsData.defense();
        this.regeneration = statsData.regeneration();
    }

    private void persistIfPossible() {
        if (this.persistentState == null) {
            return;
        }
        if (this.persistentState.profileStats == null) {
            this.persistentState.profileStats = new PersistentState.ProfileStats();
        }
        this.persistentState.profileStats.health = this.health == null ? 0.0D : this.health;
        this.persistentState.profileStats.strength = this.strength == null ? 0.0D : this.strength;
        this.persistentState.profileStats.damage = this.damage == null ? 0.0D : this.damage;
        this.persistentState.profileStats.criticalChance = this.criticalChance == null ? 0.0D : this.criticalChance;
        this.persistentState.profileStats.criticalDamage = this.criticalDamage == null ? 0.0D : this.criticalDamage;
        this.persistentState.profileStats.power = this.power == null ? 0.0D : this.power;
        this.persistentState.profileStats.energy = this.energy == null ? 0.0D : this.energy;
        this.persistentState.profileStats.energyRegeneration = this.energyRegeneration == null ? 0.0D : this.energyRegeneration;
        this.persistentState.profileStats.speed = this.speed == null ? 0.0D : this.speed;
        this.persistentState.profileStats.dexterity = this.dexterity == null ? 0.0D : this.dexterity;
        this.persistentState.profileStats.defense = this.defense == null ? 0.0D : this.defense;
        this.persistentState.profileStats.regeneration = this.regeneration == null ? 0.0D : this.regeneration;
        if (this.stateSaver != null) {
            this.stateSaver.accept(this.persistentState);
        }
    }

    private static String formatValue(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String localized(Function<String, String> localizer, String key, String fallback) {
        if (localizer == null) {
            return fallback;
        }
        String value = localizer.apply(key);
        return value == null || value.isBlank() || value.equals(key) ? fallback : value;
    }
}
