package net.minepiece.qol.ui;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.UiSettings;
import net.minepiece.qol.state.RarityDetector.Rarity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class AppearanceScreen extends Screen {
    private final MinepieceQolClient mod;
    private final Screen parent;
    private int page;
    private int selected;
    private int selectedPicture;
    private int x;
    private int y;
    private String status = "";
    private TextFieldWidget name;
    private TextFieldWidget path;

    public AppearanceScreen(MinepieceQolClient mod, Screen parent, int page) {
        super(Text.literal(mod.tr("ui.appearance")));
        this.mod = mod; this.parent = parent; this.page = page;
    }

    private String tr(String key) { return this.mod.tr("ui." + key); }
    private void save() { this.mod.saveConfig(); }
    private void refresh() { clearAndInit(); }
    private void button(String label, int dx, int dy, int width, Runnable action) {
        addDrawableChild(new ThemedButton(this.mod, ButtonWidget.builder(Text.literal(label), button -> action.run()).dimensions(x + dx, y + dy, width, 18).build()));
    }
    private void toggle(String label, int dx, int dy, boolean checked, Consumer<Boolean> action) {
        addDrawableChild(CheckboxWidget.builder(Text.literal(label), this.textRenderer).pos(x + dx, y + dy).checked(checked)
            .callback((box, value) -> { action.accept(value); save(); }).build());
    }

    @Override protected void init() {
        this.x = (this.width - 440) / 2;
        this.y = (this.height - 300) / 2;
        String[] titles = {tr("appearance"), tr("loadouts"), tr("pictures")};
        for (int i = 0; i < titles.length; i++) {
            final int target = i;
            button(titles[i], 10 + i * 138, 10, 132, () -> { this.page = target; this.status = ""; refresh(); });
        }
        button(this.mod.tr("button.edit_hud_layout"), 10, 273, 205, () -> this.mod.openHudLayoutEditor());
        button(tr("done"), 225, 273, 205, this::close);
        if (page == 0) buildStyle();
        if (page == 1) buildLoadouts();
        if (page == 2) buildPictures();
    }

    private void colorField(String value, int dy, Consumer<String> setter) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x + 130, y + dy, 74, 18, Text.literal(tr("hex")));
        field.setMaxLength(6);
        field.setText(value);
        field.setTextPredicate(text -> text.matches("[0-9a-fA-F]{0,6}"));
        field.setChangedListener(text -> { if (UiSettings.validHex(text)) { setter.accept(text); save(); } });
        addDrawableChild(field);
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
            this.mod.getConfig().appearance = defaults;
            save(); refresh();
        });
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
        this.name = new TextFieldWidget(this.textRenderer, x + 10, y + 91, 175, 18, Text.literal(tr("name")));
        name.setMaxLength(32);
        name.setText(current == null ? tr("my_setup") : current.name);
        addDrawableChild(name);
        button(tr("save_new"), 191, 91, 80, () -> saveLayout(false));
        button(tr("update"), 277, 91, 74, () -> saveLayout(true));
        button(tr("delete"), 357, 91, 73, () -> {
            if (current != null) { this.mod.getConfig().uiLoadouts.remove(current); save(); refresh(); }
        });
        for (int id = 1; id <= 11; id++) {
            int panel = id;
            toggle(this.mod.getHudPanelName(id), id <= 6 ? 10 : 230, 132 + ((id - 1) % 6) * 19,
                this.mod.isHudPanelVisible(id), value -> {
                    var hidden = this.mod.getConfig().appearance.hiddenPanels;
                    hidden.remove(Integer.valueOf(panel));
                    if (!value) hidden.add(panel);
                });
        }
    }

    private void saveLayout(boolean update) {
        String title = this.name.getText().trim();
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
        this.path = new TextFieldWidget(this.textRenderer, x + 10, y + 52, 328, 18, Text.literal(tr("path")));
        path.setMaxLength(2048);
        addDrawableChild(path);
        button(tr("import"), 344, 52, 86, () -> {
            try { importFile(Path.of(path.getText().trim().replaceAll("^\"|\"$", ""))); }
            catch (RuntimeException error) { status = tr("bad_path"); }
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
        button(tr("delete"), 10, 174, 194, () -> {
            pictures.remove(picture); this.mod.getCustomPictures().clear(); save(); refresh();
        });
    }

    private void importFile(Path source) {
        try {
            var pictures = this.mod.getConfig().appearance.pictures;
            if (pictures.size() >= 8) { status = tr("image_limit"); return; }
            var picture = CustomPictures.importPicture(source, CustomPictures.directory());
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

    @Override public void onFilesDropped(List<Path> files) {
        if (this.page == 2) for (Path file : files) importFile(file);
    }

    private void label(DrawContext context, String value, int dx, int dy, int color) {
        context.drawText(this.textRenderer, this.textRenderer.trimToWidth(value, 416), x + dx, y + dy, color, this.mod.getConfig().appearance.shadows);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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
        if (page == 0) {
            String[] labels = {tr("accent"), tr("background"), tr("borders"), tr("text")};
            String[] colors = {a.accent, a.background, a.border, a.text};
            for (int i = 0; i < labels.length; i++) {
                label(context, labels[i], 10, 51 + 24 * i, text);
                context.fill(x + 210, y + 47 + i * 24, x + 220, y + 63 + i * 24, UiSettings.color(colors[i], 100));
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
                picture.width = source.width; picture.height = source.height; picture.opacity = source.opacity;
                picture.x = x + 270; picture.y = y + 114;
                picture.scale = Math.min(1, 94F / Math.max(picture.width, picture.height));
                this.mod.getCustomPictures().draw(context, picture, true);
            }
        }
        super.render(context, mouseX, mouseY, delta);
        label(context, this.status, 10, 254, accent);
    }

    @Override public void close() { save(); this.client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
