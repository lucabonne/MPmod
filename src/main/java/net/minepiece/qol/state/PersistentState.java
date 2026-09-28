package net.minepiece.qol.state;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PersistentState {
    public Map<String, BossSpawnState> bosses = new LinkedHashMap<>();
    public Map<String, JobInfo> jobs = new LinkedHashMap<>();
    public String currentJob = "";
    public String jobsDayId = "";
    public double jobsDailyMoney;
    public List<ScheduledEvent> scheduledEvents = new ArrayList<>();
    public MoneyLedger money = new MoneyLedger();
    public Set<String> bossRegistry = new LinkedHashSet<>();
    public Set<String> ignoredBosses = new LinkedHashSet<>();
    public Map<String, ProgressRecord> playerProgress = new LinkedHashMap<>();

    public static final class ProgressRecord {
        public ProfileXpTracker.SavedProfile profile;
        public GrindingTracker.SavedDay grinding;
    }

    public ProfileStats profileStats = new ProfileStats();

    public static final class ProfileStats {
        public double level;
        public double health;
        public double power;
        public double strength;
        public double damage;
        public double criticalChance;
        public double criticalDamage;
        public double energy;
        public double energyRegeneration;
        public double speed;
        public double dexterity;
        public double defense;
        public double regeneration;
    }

    public static final class BossSpawnState {
        public String spawnId = "";
        public String bossName = "";
        public int x;
        public int y;
        public int z;
        public long nextSpawnEpochMs;
        public int cycleSeconds;
        public boolean miniboss;
        public String minibossSymbol = "";
        public long removeAfterEpochMs;
    }

    public static final class JobInfo {
        public int level;
        public double currentXp = -1.0D;
        public double neededXp;
        public double dailyXp;
        public long lastSeenMs;
    }

    public static final class MoneyLedger {
        public long currentBalance;
        public long lastLoggedBalance;
        public long dayStartBalance;
        public String dayId = "";
        public long nonAhToday;
        public long ahSpentToday;
        public long ahMadeToday;
        public int transactions;
        public List<Transaction> history = new ArrayList<>();
    }

    public static final class Transaction {
        public String id = "";
        public String type = "";
        public String itemName = "";
        public double amount;
        public long epochMs;
    }

    public static final class ScheduledEvent {
        public String name = "";
        public String time = "";
    }
}
