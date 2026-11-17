package net.minepiece.qol.essentials;

import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minepiece.qol.MinepieceQolClient;

/** Optional read of public GitHub releases. No gameplay data is included in the request. */
public final class UpdateNotice {
    private static final String RELEASES = "https://github.com/lucabonne/MPmod/releases";
    private static final Pattern VERSION = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:\\+.*)?$", Pattern.CASE_INSENSITIVE);
    private boolean checked, notified;
    private volatile String available;
    public void tick(MinepieceQolClient mod) {
        if (!mod.getConfig().updateCheckEnabled) return;
        if (!checked) {
            checked = true;
            Thread.ofVirtual().name("minepiece-qol-update-check").start(this::fetch);
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (notified || available == null || mc.player == null) return;
        notified = true;
        String current = FabricLoader.getInstance().getModContainer("minepiece-qol").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("0.0.0");
        if (!newer(available, current)) return;
        mc.player.sendSystemMessage(Component.literal("[Minepiece QoL] " + mod.tr("extras.update_available") + ": " + available)
            .withStyle(s -> s.withColor(0x55AAFF).withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create(RELEASES)))));
    }
    private void fetch() {
        try (var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()) {
            var request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/lucabonne/MPmod/releases?per_page=20"))
                .header("User-Agent", "minepiece-qol").timeout(Duration.ofSeconds(8)).GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;
            for (var element : JsonParser.parseString(response.body()).getAsJsonArray()) {
                var release = element.getAsJsonObject();
                if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) continue;
                boolean compatible = false;
                for (var asset : release.getAsJsonArray("assets")) {
                    String name = asset.getAsJsonObject().get("name").getAsString();
                    if (name.endsWith("+mc26.2.jar")) compatible = true;
                }
                String version = release.get("tag_name").getAsString();
                if (compatible && (available == null || newer(version, available))) available = version;
            }
        } catch (Exception ignored) { }
    }
    static boolean newer(String latest, String current) {
        var a = VERSION.matcher(latest); var b = VERSION.matcher(current);
        if (!a.matches() || !b.matches()) return false;
        try {
            for (int i = 1; i <= 3; i++) {
                int compare = Long.compare(Long.parseLong(a.group(i)), Long.parseLong(b.group(i)));
                if (compare != 0) return compare > 0;
            }
        } catch (NumberFormatException ignored) { }
        return false;
    }
}
