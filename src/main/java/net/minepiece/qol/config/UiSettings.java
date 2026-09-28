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
    public List<Integer> hiddenPanels = new ArrayList<>();
    public List<Picture> pictures = new ArrayList<>();

    public static final class Picture {
        public String file = "";
        public String name = "Picture";
        public int x = 12;
        public int y = 12;
        public int width = 80;
        public int height = 80;
        public float scale = 1;
        public int opacity = 100;
        public boolean visible = true;
    }

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
        if (hiddenPanels == null) hiddenPanels = new ArrayList<>();
        if (pictures == null) pictures = new ArrayList<>();
        pictures.removeIf(p -> p == null || p.file == null || !p.file.matches("[a-zA-Z0-9-]+\\.png"));
        for (Picture p : pictures) {
            p.scale = Float.isFinite(p.scale) ? Math.clamp(p.scale, 0.6F, 1.8F) : 1;
            p.opacity = Math.clamp(p.opacity, 0, 100);
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
