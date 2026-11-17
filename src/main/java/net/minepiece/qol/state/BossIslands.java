package net.minepiece.qol.state;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Island aliases adapted from Essentials; detection only accepts location-shaped bars. */
public final class BossIslands {
    public static final String UNKNOWN = "unknown";
    private static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();
    static {
        add("fuchsia", "Fuchsia", "Fucsia");
        add("orange_town", "Orange Town", "Ville Orange");
        add("baratie", "Baratie");
        add("arlong_park", "Arlong Park");
        add("logue_town", "Logue Town", "Loguetown");
        add("drum", "Drum", "Royaume de Drum", "Drum Kingdom");
        add("alabasta", "Alabasta", "Alabasta Kingdom");
        add("jaya", "Jaya");
        add("skypiea", "Skypiea");
        add("water_seven", "Water Seven", "Water 7");
        add("enies_lobby", "Enies Lobby");
        add("thriller_bark", "Thriller Bark");
        add("sabaody", "Sabaody", "Archipel des Sabaody", "Sabaody Archipelago");
        add("amazon_lily", "Amazon Lily");
        add("fishman_island", "Fishman Island", "Fishman", "Ile des Hommes-Poissons", "Ile des Hommes Poissons", "Ile Hommes Poissons");
        add("punk_hazard", "Punk Hazard");
        add("dressrosa", "Dressrosa");
        add("zou", "Zou", "Ile de Zou", "Zou Island");
        add("whole_cake", "Whole Cake Island", "Whole Cake", "W. Island");
        add("komugi", "Komugi Island", "Komugi");
    }
    private BossIslands() { }
    private static void add(String id, String... names) {
        ALIASES.put(id, java.util.Arrays.stream(names).map(BossIslands::normalize).toList());
    }
    private static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text.replaceAll("§.", ""), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace('-', ' ').replaceAll("\\s+", " ").strip();
    }
    public static String fromMenuTitle(String title) {
        String name = normalize(title);
        for (var entry : ALIASES.entrySet()) if (entry.getValue().contains(name)) return entry.getKey();
        return UNKNOWN;
    }
    public static String fromLocationBars(List<String> bars) {
        String found = UNKNOWN;
        for (String raw : bars) {
            String cleaned = normalize(raw).replaceFirst("^[^\\p{L}\\p{N}]+", "");
            String text = cleaned.replaceFirst("^(?:island|location|zone|ile|isola|isla|insel|ilha|wyspa|pulau|ada)\\s*[:：]\\s*", "");
            text = text.replaceFirst("^(?:vous arrivez a|vous etes (?:a|sur)|you arrive (?:at|in)|you are (?:at|on|in))\\s+", "");
            final String location = text;
            for (var entry : ALIASES.entrySet()) {
                boolean matches = entry.getValue().stream().anyMatch(alias -> {
                    var matcher = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(alias) + "(?![\\p{L}\\p{N}])").matcher(location);
                    if (!matcher.find() || matcher.start() != 0) return false;
                    String remainder = location.substring(matcher.end()).strip();
                    return remainder.isEmpty() || remainder.matches("[|•:() \u00b7\\-]*?(?:(?:lvl\\.?|level|niveau|livello|nivel)\\s*[:.]?\\s*\\d+[() ]*)?");
                });
                if (matches) {
                    if (!UNKNOWN.equals(found) && !found.equals(entry.getKey())) return UNKNOWN;
                    found = entry.getKey();
                }
            }
        }
        return found;
    }
}
