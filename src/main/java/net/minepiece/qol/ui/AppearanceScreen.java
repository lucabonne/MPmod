package net.minepiece.qol.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.UiSettings;
import net.minepiece.qol.state.RarityDetector.Rarity;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class AppearanceScreen extends Screen {
    private final MinepieceQolClient mod;
    private final Screen parent;
    private int page;
    private int selected;
    private int selectedPicture;
    private int selectedPanel;
    private float menuScale = 1;
    private int x;
    private int y;
    private String status = "";
    private EditBox name;
    private EditBox path;

    public AppearanceScreen(MinepieceQolClient mod, Screen parent, int page) {
        super(Component.literal(mod.tr("ui.appearance")));
        this.mod = mod; this.parent = parent; this.page = page;
    }

    private String tr(String key) { return this.mod.tr("ui." + key); }
    private void save() { this.mod.saveConfig(); }
    private void refresh() { rebuildWidgets(); }
    private void button(String label, int dx, int dy, int width, Runnable action) {
        addRenderableWidget(new ThemedButton(this.mod, Button.builder(Component.literal(label), button -> action.run()).bounds(x + dx, y + dy, width, 18).build()));
    }
    private void toggle(String label, int dx, int dy, boolean checked, Consumer<Boolean> action) {
        addRenderableWidget(Checkbox.builder(Component.literal(label), this.font).pos(x + dx, y + dy).selected(checked)
            .onValueChange((box, value) -> { action.accept(value); save(); }).build());
    }

    @Override protected void init() {
        menuScale = Math.min(1F, Math.min(width / 456F, height / 316F));
        this.x = (Math.round(this.width / menuScale) - 440) / 2;
        this.y = (Math.round(this.height / menuScale) - 300) / 2;
        String[] titles = {tr("appearance"), tr("loadouts"), tr("pictures"), tr("panels")};
        for (int i = 0; i < titles.length; i++) {
            final int target = i;
            button(titles[i], 10 + i * 105, 10, 99, () -> { this.page = target; this.status = ""; refresh(); });
        }
        button(this.mod.tr("button.edit_hud_layout"), 10, 273, 205, () -> this.mod.openHudLayoutEditor());
        button(tr("done"), 225, 273, 205, this::onClose);
        if (page == 0) buildStyle();
        if (page == 1) buildLoadouts();
        if (page == 2) buildPictures();
        if (page == 3) buildPanels();
    }

    private void colorField(String value, int dy, Consumer<String> setter) {
        EditBox field = new EditBox(this.font, x + 130, y + dy, 74, 18, Component.literal(tr("hex")));
        field.setMaxLength(7);
        field.setValue(value);
        field.setResponder(text -> {
            String hex = text.startsWith("#") ? text.substring(1) : text;
            if (UiSettings.validHex(hex)) { setter.accept(hex); save(); }
        });
        addRenderableWidget(field);
    }

    private void number(String label, int value, int dy, int min, int max, int step, IntConsumer setter) {
        button("−", 130, dy, 22, () -> { setter.accept(Math.max(min, value - step)); save(); refresh(); });
        button(label + ": " + value, 10, dy, 114, () -> { });
        button("+", 182, dy, 22, () -> { setter.accept(Math.min(max, value + step)); save(); refresh(); });
    }

    private void buildStyle() {
        var a = this.mod.getConfig().appearance;
        colorField(a.accent, 46, value -> a.accent = value);
        colorField(a.background, 70, value -> a.background = value);
        colorField(a.border, 94, value -> a.border = value);
        colorField(a.text, 118, value -> a.text = value);
        number(tr("menu_opacity"), a.menuOpacity, 146, 0, 100, 5, value -> a.menuOpacity = value);
        number(tr("hud_opacity"), a.hudOpacity, 170, 0, 100, 5, value -> a.hudOpacity = value);
        number(tr("borders"), a.borderWidth, 194, 0, 3, 1, value -> a.borderWidth = value);
        toggle(tr("compact"), 230, 46, a.compact, value -> a.compact = value);
        toggle(tr("headers"), 230, 70, a.headers, value -> a.headers = value);
        toggle(tr("decorations"), 230, 94, a.decorations, value -> a.decorations = value);
        toggle(tr("shadows"), 230, 118, a.shadows, value -> a.shadows = value);
        toggle(tr("rarities"), 230, 146, this.mod.getConfig().rarityIconsEnabled, value -> this.mod.setRarityIconsEnabled(value));
        toggle(tr("icon_rows"), 230, 170, a.iconRows, value -> a.iconRows = value);
        button(tr("reset_style"), 230, 194, 195, () -> {
            var defaults = new UiSettings();
            defaults.pictures = a.pictures;
            defaults.hiddenPanels = a.hiddenPanels;
            defaults.panelStyles = a.panelStyles;
            defaults.profileStats = a.profileStats;
            defaults.bossesByTime = a.bossesByTime;
            defaults.collapsedBossIslands = a.collapsedBossIslands;
            this.mod.getConfig().appearance = defaults;
            save(); refresh();
        });
    }

    private void buildPanels() {
        selectedPanel = Math.floorMod(selectedPanel, this.mod.getHudPanelCount() + 1);
        int id = selectedPanel;
        var a = this.mod.getConfig().appearance;
        var style = a.panelStyle(id);
        var picture = this.mod.getHudPicture(id);
        button("<", 10, 40, 24, () -> { selectedPanel--; refresh(); });
        button(this.mod.getHudPanelName(id), 40, 40, 360, () -> { });
        button(">", 406, 40, 24, () -> { selectedPanel++; refresh(); });
        toggle(tr("visible"), 10, 68, picture != null ? picture.visible : this.mod.isHudPanelVisible(id), value -> {
            if (picture != null) picture.visible = value;
            else {
                a.hiddenPanels.remove(Integer.valueOf(id));
                if (!value) a.hiddenPanels.add(id);
            }
        });
        if (id > 0) number(tr("overall_scale") + " %", Math.round(this.mod.getHudPanelScale(id) * 100), 96, 60, 180, 5,
            value -> this.mod.setHudPanelLayout(id, this.mod.getHudPanelX(id), this.mod.getHudPanelY(id), value / 100F));
        if (picture != null) {
            button(tr("pictures"), 230, 96, 195, () -> { page = 2; selectedPicture = id - 15; refresh(); });
            return;
        }
        number(tr("text_size") + " %", Math.round(style.textScale * 100), 122, 60, 180, 5,
            value -> a.editPanelStyle(id).textScale = value / 100F);
        if (id != 6 && id != 7) {
            dimension(tr("box_width"), style.minWidth, 148, value -> a.editPanelStyle(id).minWidth = value);
            dimension(tr("box_height"), style.minHeight, 174, value -> a.editPanelStyle(id).minHeight = value);
            if (id != 0) button(tr("title") + ": " + tr(style.title == null ? "default" : style.title ? "on" : "off"), 230, 68, 195, () -> {
                a.editPanelStyle(id).title = style.title == null ? true : style.title ? false : null;
                save(); refresh();
            });
        }
        int dy = 202;
        button(tr("panel_color"), 10, dy, 114, () -> { });
        String hex = String.format(java.util.Locale.ROOT, "%06X", HudOverlay.panelBackground(this.mod.getHudPanelColor(id), a) & 0xFFFFFF);
        colorField(hex, dy, value -> this.mod.setHudPanelColor(id, value));
        button(tr("random"), 230, dy, 195, () -> {
            this.mod.setHudPanelColor(id, String.format(java.util.Locale.ROOT, "%06X", java.util.concurrent.ThreadLocalRandom.current().nextInt(0x1000000)));
            save(); refresh();
        });
    }

    private void dimension(String label, int value, int dy, IntConsumer setter) {
        button(label, 10, dy, 114, () -> { });
        EditBox field = new EditBox(this.font, x + 130, y + dy, 74, 18, Component.literal(label));
        field.setMaxLength(4);
        field.setHint(Component.literal(tr("auto")));
        field.setValue(value == 0 ? "" : Integer.toString(value));
        field.setResponder(text -> {
            if (text.isEmpty()) { setter.accept(0); save(); }
            else if (text.matches("[0-9]{1,4}")) {
                int parsed = Integer.parseInt(text);
                if (parsed <= 2048) { setter.accept(parsed); save(); }
            }
        });
        addRenderableWidget(field);
        button(tr("auto"), 230, dy, 195, () -> { setter.accept(0); save(); refresh(); });
    }

    private UiSettings.Loadout selectedLoadout() {
        var layouts = this.mod.getConfig().uiLoadouts;
        return layouts.isEmpty() ? null : layouts.get(Math.floorMod(selected, layouts.size()));
    }

    private void buildLoadouts() {
        String[] presets = {"Farming", "Fighting", "Grinding"};
        for (int i = 0; i < presets.length; i++) {
            String preset = presets[i];
            button(tr(preset.toLowerCase(java.util.Locale.ROOT)), 10 + i * 140, 40, 134, () -> {
                // Preserve the current arrangement before the first preset is used.
                if (this.mod.getConfig().uiLoadouts.isEmpty()) {
                    this.mod.getConfig().uiLoadouts.add(UiLoadouts.capture(this.mod, tr("original"), this.width, this.height));
                }
                UiLoadouts.preset(this.mod, preset, this.width, this.height);
                save();
                status = tr("applied") + ": " + tr(preset.toLowerCase(java.util.Locale.ROOT)); refresh();
            });
        }
        var current = selectedLoadout();
        button("<", 10, 65, 22, () -> { selected--; refresh(); });
        button(current == null ? tr("no_loadouts") : current.name, 38, 65, 290, () -> {
            if (current != null) {
                this.mod.getCustomPictures().clear();
                UiLoadouts.apply(this.mod, current, this.width, this.height);
                save();
                status = tr("applied") + ": " + current.name;
                refresh();
            }
        });
        button(">", 334, 65, 22, () -> { selected++; refresh(); });
        button(tr("apply"), 362, 65, 68, () -> {
            if (current != null) {
                this.mod.getCustomPictures().clear();
                UiLoadouts.apply(this.mod, current, this.width, this.height);
                save();
                status = tr("applied") + ": " + current.name;
                refresh();
            }
        });
        this.name = new EditBox(this.font, x + 10, y + 91, 175, 18, Component.literal(tr("name")));
        name.setMaxLength(32);
        name.setValue(current == null ? tr("my_setup") : current.name);
        addRenderableWidget(name);
        button(tr("save_new"), 191, 91, 80, () -> saveLayout(false));
        button(tr("update"), 277, 91, 74, () -> saveLayout(true));
        button(tr("delete"), 357, 91, 73, () -> {
            if (current != null) { this.mod.getConfig().uiLoadouts.remove(current); save(); refresh(); }
        });
        for (int id = 1; id <= 14; id++) {
            int panel = id;
            toggle(this.mod.getHudPanelName(id), id <= 7 ? 10 : 230, 132 + ((id - 1) % 7) * 18,
                this.mod.isHudPanelVisible(id), value -> {
                    var hidden = this.mod.getConfig().appearance.hiddenPanels;
                    hidden.remove(Integer.valueOf(panel));
                    if (!value) hidden.add(panel);
                });
        }
    }

    private void saveLayout(boolean update) {
        String title = this.name.getValue().trim();
        if (title.isBlank()) { status = tr("name_required"); return; }
        var current = selectedLoadout();
        if (update && current == null) return;
        var layouts = this.mod.getConfig().uiLoadouts;
        if (layouts.stream().anyMatch(layout -> layout.name.equalsIgnoreCase(title) && (!update || layout != current))) {
            status = tr("name_exists"); return;
        }
        var saved = UiLoadouts.capture(this.mod, title, this.width, this.height);
        if (update) layouts.set(layouts.indexOf(current), saved);
        else { layouts.add(saved); selected = layouts.size() - 1; }
        this.mod.getConfig().activeUiLoadout = title;
        status = tr("saved") + ": " + title;
        save(); refresh();
    }

    private void buildPictures() {
        this.path = new EditBox(this.font, x + 10, y + 52, 230, 18, Component.literal(tr("path")));
        path.setMaxLength(2048);
        addRenderableWidget(path);
        button(tr("import"), 246, 52, 64, () -> {
            try { importFile(Path.of(path.getValue().trim().replaceAll("^\"|\"$", ""))); }
            catch (RuntimeException error) { status = tr("bad_path"); }
        });
        button(tr("choose_folder"), 316, 52, 114, () -> {
            String folder = TinyFileDialogs.tinyfd_selectFolderDialog(tr("choose_folder"), (String) null);
            if (folder != null) importFile(Path.of(folder));
        });
        var pictures = this.mod.getConfig().appearance.pictures;
        if (pictures.isEmpty()) return;
        selectedPicture = Math.floorMod(selectedPicture, pictures.size());
        var picture = pictures.get(selectedPicture);
        button("<", 10, 84, 22, () -> { selectedPicture--; refresh(); });
        button(picture.name, 38, 84, 364, () -> { });
        button(">", 408, 84, 22, () -> { selectedPicture++; refresh(); });
        toggle(tr("visible"), 10, 115, picture.visible, value -> picture.visible = value);
        number(tr("opacity"), picture.opacity, 145, 0, 100, 5, value -> picture.opacity = value);
        if (!picture.rotationFiles.isEmpty()) {
            button(tr("rotation_interval"), 10, 174, 114, () -> { });
            EditBox interval = new EditBox(this.font, x + 130, y + 174, 74, 18,
                Component.literal(tr("rotation_interval")));
            interval.setMaxLength(4);
            interval.setValue(Integer.toString(picture.intervalSeconds));
            interval.setResponder(value -> {
                if (!value.matches("[0-9]{1,4}") || Integer.parseInt(value) < 1 || Integer.parseInt(value) > 3600) {
                    status = tr("interval_range");
                    return;
                }
                picture.intervalSeconds = Integer.parseInt(value);
                status = "";
                save();
            });
            addRenderableWidget(interval);
            button("−", 210, 174, 22, () -> interval.setValue(Integer.toString(Math.max(1, picture.intervalSeconds - 1))));
            button("+", 236, 174, 22, () -> interval.setValue(Integer.toString(Math.min(3600, picture.intervalSeconds + 1))));
        }
        button(tr("delete"), 10, picture.rotationFiles.isEmpty() ? 174 : 202, 194, () -> {
            pictures.remove(picture); this.mod.getCustomPictures().clear(); save(); refresh();
        });
    }

    private void importFile(Path source) {
        try {
            var pictures = this.mod.getConfig().appearance.pictures;
            if (pictures.size() >= 8) { status = tr("image_limit"); return; }
            var picture = Files.isDirectory(source)
                ? CustomPictures.importFolder(source, CustomPictures.directory())
                : CustomPictures.importPicture(source, CustomPictures.directory());
            picture.x = 12 + pictures.size() * 20;
            picture.y = Math.max(12, this.height - picture.height - 28);
            pictures.add(picture);
            selectedPicture = pictures.size() - 1;
            status = tr("imported");
            save(); refresh();
        } catch (Exception error) {
            status = error.getMessage() == null ? tr("bad_path") : error.getMessage();
        }
    }

    @Override public void onFilesDrop(List<Path> files) {
        if (this.page == 2) for (Path file : files) importFile(file);
    }

    private void label(GuiGraphicsExtractor context, String value, int dx, int dy, int color) {
        context.text(this.font, this.font.plainSubstrByWidth(value, 416), x + dx, y + dy, color, this.mod.getConfig().appearance.shadows);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.pose().pushMatrix();
        context.pose().scale(menuScale, menuScale);
        mouseX = (int) (mouseX / menuScale); mouseY = (int) (mouseY / menuScale);
        var a = this.mod.getConfig().appearance;
        int text = UiSettings.color(a.text, 100);
        int accent = UiSettings.color(a.accent, 100);
        context.fill(x, y, x + 440, y + 300, UiSettings.color(a.background, a.menuOpacity));
        for (int i = 0; i < a.borderWidth; i++) {
            int border = UiSettings.color(a.border, a.menuOpacity);
            context.fill(x + i, y + i, x + 440 - i, y + i + 1, border);
            context.fill(x + i, y + 299 - i, x + 440 - i, y + 300 - i, border);
            context.fill(x + i, y + i, x + i + 1, y + 300 - i, border);
            context.fill(x + 439 - i, y + i, x + 440 - i, y + 300 - i, border);
        }
        if (page == 3 && this.mod.getHudPicture(selectedPanel) == null) {
            int color = HudOverlay.panelBackground(this.mod.getHudPanelColor(selectedPanel), a) | 0xFF000000;
            context.fill(x + 208, y + 203, x + 225, y + 219, text);
            context.fill(x + 209, y + 204, x + 224, y + 218, color);
        }
        if (page == 0) {
            String[] labels = {tr("accent"), tr("background"), tr("borders"), tr("text")};
            String[] colors = {a.accent, a.background, a.border, a.text};
            for (int i = 0; i < labels.length; i++) {
                label(context, labels[i], 10, 51 + 24 * i, text);
                context.fill(x + 210, y + 47 + i * 24, x + 220, y + 63 + i * 24, text);
                context.fill(x + 211, y + 48 + i * 24, x + 219, y + 62 + i * 24, UiSettings.color(colors[i], 100));
            }
            int index = 0;
            for (Rarity rarity : List.of(Rarity.EPIC, Rarity.MYTHIC, Rarity.PRIMORDIAL)) {
                RarityBadges.draw(context, rarity, x + 10 + index * 138, y + 226, 14);
                label(context, rarity.name(), 30 + index * 138, 230, accent);
                index++;
            }
        }
        if (page == 1) label(context, tr("visible_panels"), 10, 117, accent);
        if (page == 2) {
            label(context, tr("image_hint"), 10, 36, text);
            label(context, tr("image_edit_hint"), 10, 224, text);
            if (!a.pictures.isEmpty()) {
                var source = a.pictures.get(Math.floorMod(selectedPicture, a.pictures.size()));
                var picture = new UiSettings.Picture();
                picture.file = source.file; picture.name = source.name;
                picture.rotationFiles = source.rotationFiles; picture.intervalSeconds = source.intervalSeconds;
                picture.width = source.width; picture.height = source.height; picture.opacity = source.opacity;
                picture.x = x + 270; picture.y = y + 114;
                picture.scale = Math.min(1, 94F / Math.max(picture.width, picture.height));
                this.mod.getCustomPictures().draw(context, picture, true);
            }
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        label(context, this.status, 10, 254, accent);
        context.pose().popMatrix();
    }

    private net.minecraft.client.input.MouseButtonEvent scaledClick(net.minecraft.client.input.MouseButtonEvent click) {
        return new net.minecraft.client.input.MouseButtonEvent(click.x() / menuScale, click.y() / menuScale, click.buttonInfo());
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) { return super.mouseClicked(scaledClick(click), doubled); }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent click) { return super.mouseReleased(scaledClick(click)); }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double dx, double dy) { return super.mouseDragged(scaledClick(click), dx / menuScale, dy / menuScale); }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(x / menuScale, y / menuScale); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) { return super.mouseScrolled(x / menuScale, y / menuScale, horizontal, vertical); }

    @Override public void onClose() { save(); this.minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
