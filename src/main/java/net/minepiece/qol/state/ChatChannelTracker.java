package net.minepiece.qol.state;

import java.util.Locale;
import java.util.regex.Pattern;
import net.minepiece.qol.util.TextUtil;

public final class ChatChannelTracker {
    public enum Channel { UNKNOWN, PUBLIC, PARTY, ISLAND }
    private static final Pattern LANGUAGE = Pattern.compile("(?:You are now following the channel|Ahora estás siguiendo el canal) ([\\p{L}_-]+)\\.", Pattern.CASE_INSENSITIVE);
    private Channel channel = Channel.UNKNOWN;
    private String language = "";

    public void reset() {
        this.channel = Channel.UNKNOWN;
        this.language = "";
    }

    public void onServerMessage(String message) {
        String text = TextUtil.normalize(message);
        if (text.startsWith("瓦 ") || text.startsWith("莱 ")) text = text.substring(2).trim();
        switch (text) {
            case "Island chat enabled", "Chat de isla activado" -> this.channel = Channel.ISLAND;
            case "Island chat disabled", "Chat de isla desactivado" -> {
                if (this.channel == Channel.ISLAND || this.channel == Channel.UNKNOWN) this.channel = Channel.PUBLIC;
            }
            case "The game chat is now disabled. Your messages will be sent publicly.",
                "El chat de la partida ahora está desactivado. Tus mensajes se enviarán públicamente." -> this.channel = Channel.PUBLIC;
            case "Party chat is now enabled. Your messages will only be sent to members of your party.",
                "El chat del grupo ahora está activado. Tus mensajes solo se enviarán a los miembros de tu grupo." -> this.channel = Channel.PARTY;
            default -> {
                var match = LANGUAGE.matcher(text);
                if (match.matches()) this.language = match.group(1).toLowerCase(Locale.ROOT);
            }
        }
    }

    public Channel channel() { return this.channel; }
    public String language() { return this.language; }

    public String languageCode() {
        return switch (this.language) {
            case "english", "en" -> "en";
            case "italian", "italiano", "it" -> "it";
            case "french", "français", "francais", "fr" -> "fr";
            case "german", "deutsch", "de" -> "de";
            case "spanish", "español", "espanol", "es" -> "es";
            case "portuguese", "português", "portugues", "pt" -> "pt";
            case "brazilian", "br" -> "br";
            case "polish", "polski", "pl" -> "pl";
            case "indonesian", "indonesia", "id" -> "id";
            case "turkish", "türkçe", "turkce", "tr" -> "tr";
            case "russian", "ru" -> "ru";
            case "dutch", "nederlands", "nl" -> "nl";
            default -> "";
        };
    }
}
