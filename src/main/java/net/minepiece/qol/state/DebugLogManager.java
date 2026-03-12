package net.minepiece.qol.state;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import net.minecraft.client.MinecraftClient;

public final class DebugLogManager {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logPath;
    private final Queue<String> recentLines = new ArrayDeque<>();
    private boolean enabled;
    private String lastLine = "";

    public DebugLogManager(Path baseDir, boolean enabled) {
        this.logPath = baseDir.resolve("debug.log");
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void logChat(String message) {
        log("CHAT", message);
    }

    public void logActionbar(String message) {
        log("ACTIONBAR", message);
    }

    public void logTitle(String message) {
        log("TITLE", message);
    }

    public void logTabFooter(String message) {
        log("TAB", message);
    }

    public void logTooltip(List<String> lines) {
        if (!this.enabled || lines.isEmpty()) {
            return;
        }
        log("TOOLTIP", String.join(" | ", lines));
    }

    public void logInternal(String message) {
        log("INTERNAL", message);
    }

    public String getLastLine() {
        return this.lastLine;
    }

    public void copyLastToClipboard(MinecraftClient client) {
        if (client != null && !this.lastLine.isBlank()) {
            client.keyboard.setClipboard(this.lastLine);
        }
    }

    private void log(String category, String message) {
        if (!this.enabled) {
            return;
        }

        String line = "[" + FORMATTER.format(LocalDateTime.now()) + "] [" + category + "] " + message;
        this.lastLine = line;
        this.recentLines.add(line);
        while (this.recentLines.size() > 200) {
            this.recentLines.poll();
        }

        try {
            Files.createDirectories(this.logPath.getParent());
            Files.writeString(this.logPath, line + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
