package net.minepiece.qol.config;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Visual-only settings: loadouts never change gameplay tracking state. */
public final class UiSettings {
    public String accent = "6FA8FF";
    public String background = "101723";
    public String border = "59677C";
    public String text = "EAF1FF";
    public String chatPanelColor = "default";
    public int menuOpacity = 94;
    public int hudOpacity = 72;
    public int borderWidth = 1;
    public boolean compact = true;
    public boolean iconRows = true;
    public boolean decorations = false;
    public boolean shadows = true;
    public boolean headers = true;
    public Map<Integer, PanelStyle> panelStyles = new LinkedHashMap<>();
    public List<String> profileStats = new ArrayList<>(List.of("power", "strength", "speed"));
    public boolean bossesByTime;

    public static final class PanelStyle {
        public float textScale = 1;
        public int minWidth;
        public int minHeight;
        public Boolean title;
        public boolean titleVisible(boolean defaultValue) { return title == null ? defaultValue : title; }
        public void normalize() {
            textScale = Float.isFinite(textScale) ? Math.clamp(textScale, 0.6F, 1.8F) : 1;
            minWidth = Math.clamp(minWidth, 0, 2048);
            minHeight = Math.clamp(minHeight, 0, 2048);
        }
    }

    public PanelStyle panelStyle(int id) {
        return panelStyles.getOrDefault(id, new PanelStyle());
    }

    public PanelStyle editPanelStyle(int id) {
        return panelStyles.computeIfAbsent(id, ignored -> new PanelStyle());
    }
    public List<Integer> hiddenPanels = new ArrayList<>();
    public List<String> collapsedBossIslands = new ArrayList<>();
    public List<Picture> pictures = new ArrayList<>();

    public static final class Picture {
        public String file = "";
        public String name = "Picture";
        public List<String> rotationFiles = new ArrayList<>();
        public int intervalSeconds = 10;
        public int x = 12;
        public int y = 12;
        public int width = 80;
        public int height = 80;
        public float scale = 1;
        public int opacity = 100;
        public boolean visible = true;
        /** Cover the whole screen (cropped, never stretched) instead of using x/y/size. */
        public boolean fullscreen = false;
        /** Edge the image fades out toward: none, top, bottom, left or right. */
        public String fade = "none";
        /** How far the fade reaches across the image, in percent. */
        public int fadePercent = 50;
    }

    public static final List<String> FADES = List.of("none", "top", "bottom", "left", "right");

    public static final class Placement {
        public int x;
        public int y;
        public float scale = 1;
        public String color = "default";
        public Placement() { }
        public Placement(int x, int y, float scale, String color) {
            this.x = x; this.y = y; this.scale = scale; this.color = color;
        }
    }

    public static final class Loadout {
        public String name = "";
        public int screenWidth = 1;
        public int screenHeight = 1;
        public Map<Integer, Placement> panels = new LinkedHashMap<>();
        public UiSettings appearance = new UiSettings();
    }

    public UiSettings copy() {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(this), UiSettings.class);
    }

    public void normalize() {
        accent = validHex(accent) ? accent : "6FA8FF";
        background = validHex(background) ? background : "101723";
        border = validHex(border) ? border : "59677C";
        text = validHex(text) ? text : "EAF1FF";
        menuOpacity = Math.clamp(menuOpacity, 0, 100);
        hudOpacity = Math.clamp(hudOpacity, 0, 100);
        borderWidth = Math.clamp(borderWidth, 0, 3);
        if (panelStyles == null) panelStyles = new LinkedHashMap<>();
        panelStyles.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getKey() < 0 || e.getKey() > 15);
        panelStyles.values().forEach(PanelStyle::normalize);
        if (profileStats == null) profileStats = new ArrayList<>(List.of("power", "strength", "speed"));
        profileStats = new ArrayList<>(profileStats.stream().filter(key -> key != null && net.minepiece.qol.state.ProfileStatsTracker.STAT_KEYS.contains(key)).distinct().toList());
        if (collapsedBossIslands == null) collapsedBossIslands = new ArrayList<>();
        collapsedBossIslands.removeIf(id -> id == null || id.isBlank());
        if (hiddenPanels == null) hiddenPanels = new ArrayList<>();
        if (pictures == null) pictures = new ArrayList<>();
        pictures.removeIf(p -> p == null || p.file == null || !p.file.matches("[a-zA-Z0-9-]+\\.png"));
        for (Picture p : pictures) {
            if (p.rotationFiles == null) p.rotationFiles = new ArrayList<>();
            p.rotationFiles = new ArrayList<>(p.rotationFiles.stream()
                .filter(file -> file != null && file.matches("[a-zA-Z0-9-]+\\.png")).distinct().toList());
            p.intervalSeconds = Math.clamp(p.intervalSeconds, 1, 3600);
            p.scale = Float.isFinite(p.scale) ? Math.clamp(p.scale, 0.6F, 1.8F) : 1;
            p.opacity = Math.clamp(p.opacity, 0, 100);
            if (!FADES.contains(p.fade)) p.fade = "none";
            p.fadePercent = Math.clamp(p.fadePercent, 5, 100);
            p.width = Math.clamp(p.width, 8, 512);
            p.height = Math.clamp(p.height, 8, 512);
        }
    }

    public int hudTextColor(int originalColor) {
        return "EAF1FF".equalsIgnoreCase(this.text) ? originalColor : color(this.text, 100);
    }

    public static boolean validHex(String value) {
        return value != null && value.matches("[0-9a-fA-F]{6}");
    }

    public static int color(String hex, int opacity) {
        return (Math.round(Math.clamp(opacity, 0, 100) * 2.55F) << 24) | Integer.parseInt(hex, 16);
    }

    public static int resizePosition(int position, int from, int to) {
        return position < 0 ? position : Math.clamp((int) Math.round(position * (double) to / Math.max(1, from)), 0, Math.max(0, to - 16));
    }
}
