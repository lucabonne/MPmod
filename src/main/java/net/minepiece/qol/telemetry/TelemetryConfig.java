package net.minepiece.qol.telemetry;

public final class TelemetryConfig {
    public boolean enabled;
    public boolean discordEnabled;
    public String webhookUrl = "";

    public boolean inventoryEnabled = true;
    public boolean economyEnabled = true;
    public boolean combatEnabled = true;
    public boolean chatCommandsEnabled = true;
    public boolean interactionsEnabled = true;

    public boolean sessionAlerts = true;
    public boolean deathAlerts = true;
    public boolean failureAlerts = true;
    public boolean overflowAlerts = true;
}
