package net.minepiece.qol.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minepiece.qol.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class ChatTranslationManager {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(4L);
    private static final Duration AGGRESSIVE_REQUEST_TIMEOUT = Duration.ofMillis(2_500L);
    private static final long CACHE_TTL_MS = 10L * 60L * 1000L;
    private static final long ERROR_LOG_THROTTLE_MS = 30_000L;
    private static final int MAX_TRANSLATION_CHARS = 220;
    private static final int MAX_CACHE_ENTRIES = 600;
    private static final int MIN_TRANSLATION_WORKERS = 2;
    private static final int MAX_TRANSLATION_WORKERS = 4;
    private static final String TRANSLATION_PREFIX = "[TR ";
    private static final char[] MESSAGE_DIVIDERS = {'›', '»', '>'};

    private static final List<LanguageOption> SUPPORTED_LANGUAGES = List.of(
        new LanguageOption("en", "English"),
        new LanguageOption("fr", "French"),
        new LanguageOption("es", "Spanish"),
        new LanguageOption("de", "German"),
        new LanguageOption("it", "Italian"),
        new LanguageOption("pt", "Portuguese"),
        new LanguageOption("pl", "Polish"),
        new LanguageOption("id", "Indonesian"),
        new LanguageOption("tr", "Turkish")
    );

    private final DebugLogManager debugLogManager;
    private final HttpClient httpClient;
    private final ExecutorService requestExecutor;
    private final Map<String, CachedTranslation> cache = Collections.synchronizedMap(
        new LinkedHashMap<>(256, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedTranslation> eldest) {
                return size() > MAX_CACHE_ENTRIES;
            }
        }
    );
    private final Set<String> pendingRequests = ConcurrentHashMap.newKeySet();

    private volatile long lastErrorLogEpochMs;

    public ChatTranslationManager(DebugLogManager debugLogManager) {
        this.debugLogManager = debugLogManager;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
        this.requestExecutor = Executors.newFixedThreadPool(computeWorkerCount(), runnable -> {
            Thread thread = new Thread(runnable, "minepiece-chat-translate");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static List<LanguageOption> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    public static String normalizeLanguageCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        int dash = normalized.indexOf('-');
        if (dash > 0) {
            normalized = normalized.substring(0, dash);
        }
        int underscore = normalized.indexOf('_');
        if (underscore > 0) {
            normalized = normalized.substring(0, underscore);
        }
        return normalized;
    }

    public static boolean isSupportedLanguageCode(String code) {
        String normalized = normalizeLanguageCode(code);
        for (LanguageOption option : SUPPORTED_LANGUAGES) {
            if (option.code().equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static String languageName(String code) {
        String normalized = normalizeLanguageCode(code);
        for (LanguageOption option : SUPPORTED_LANGUAGES) {
            if (option.code().equals(normalized)) {
                return option.label();
            }
        }
        return normalized.isBlank() ? "Unknown" : normalized.toUpperCase(Locale.ROOT);
    }

    public boolean isSyntheticTranslationLine(String normalizedLine) {
        return normalizedLine != null && normalizedLine.startsWith(TRANSLATION_PREFIX);
    }

    public static ChatLineClassification classifyLine(String normalizedLine) {
        if (normalizedLine == null || normalizedLine.isBlank()) {
            return new ChatLineClassification(ChatLineType.INVALID, "", "", "", false, "blank");
        }

        DividerMatch divider = findMessageDivider(normalizedLine);
        String authorPrefix = "";
        String messageBody = normalizedLine.trim();
        ChatLineType lineType = ChatLineType.SYSTEM;
        if (divider.index() > 0 && divider.index() < normalizedLine.length() - 1) {
            authorPrefix = normalizedLine.substring(0, divider.index()).trim();
            messageBody = normalizedLine.substring(divider.index() + 1).trim();
            if (authorPrefix.isBlank() || messageBody.isBlank()) {
                return new ChatLineClassification(ChatLineType.INVALID, authorPrefix, messageBody, divider.symbol(), false, "empty_segment");
            }
            lineType = looksLikePrivatePrefix(authorPrefix) ? ChatLineType.PRIVATE : ChatLineType.PUBLIC;
        } else if (messageBody.isBlank()) {
            return new ChatLineClassification(ChatLineType.INVALID, "", "", divider.symbol(), false, "blank_body");
        }
        return new ChatLineClassification(lineType, authorPrefix, messageBody, divider.symbol(), true, "");
    }

    public static boolean isLineTypeEnabled(
        ChatLineType lineType,
        boolean translatePublic,
        boolean translatePrivate,
        boolean translateSystem
    ) {
        return switch (lineType) {
            case PUBLIC -> translatePublic;
            case PRIVATE -> translatePrivate;
            case SYSTEM -> translateSystem;
            case INVALID -> false;
        };
    }

    public void shutdown() {
        this.requestExecutor.shutdownNow();
    }

    public void onChatMessage(
        String normalizedLine,
        List<ConfigManager.ModConfig.ChatTranslationRule> rawRules,
        boolean translatePublic,
        boolean translatePrivate,
        boolean translateSystem,
        boolean aggressiveMode
    ) {
        if (normalizedLine == null || normalizedLine.isBlank()) {
            return;
        }
        if (isSyntheticTranslationLine(normalizedLine)) {
            return;
        }

        ChatLineClassification classification = classifyLine(normalizedLine);
        if (!classification.valid()) {
            return;
        }
        ChatLineType lineType = classification.lineType();
        String authorPrefix = classification.authorPrefix();
        String messageBody = classification.messageBody();
        boolean allowed = isLineTypeEnabled(lineType, translatePublic, translatePrivate, translateSystem);
        if (!allowed) {
            return;
        }

        final String finalAuthorPrefix = authorPrefix;
        final String finalMessageBody = messageBody;

        List<Rule> activeRules = sanitizeRules(rawRules);
        if (activeRules.isEmpty()) {
            return;
        }
        Map<String, List<Rule>> rulesByTarget = groupRulesByTarget(activeRules);
        long now = System.currentTimeMillis();
        for (Map.Entry<String, List<Rule>> targetEntry : rulesByTarget.entrySet()) {
            String target = targetEntry.getKey();
            List<Rule> targetRules = targetEntry.getValue();
            TranslationResult cached = getCachedTranslation(finalMessageBody, target, now);
            if (cached != null) {
                postMatchingRules(finalAuthorPrefix, finalMessageBody, targetRules, cached, aggressiveMode);
                continue;
            }

            String pendingKey = lineType + "|" + finalAuthorPrefix + "|" + finalMessageBody + "|" + target;
            if (!this.pendingRequests.add(pendingKey)) {
                continue;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    TranslationResult translated = translateAuto(finalMessageBody, target, aggressiveMode);
                    if (translated == null || translated.text().isBlank()) {
                        return;
                    }
                    postMatchingRules(finalAuthorPrefix, finalMessageBody, targetRules, translated, aggressiveMode);
                } catch (Throwable throwable) {
                    logTranslationFailure("translate", throwable);
                } finally {
                    this.pendingRequests.remove(pendingKey);
                }
            }, this.requestExecutor);
        }
    }

    private List<Rule> sanitizeRules(List<ConfigManager.ModConfig.ChatTranslationRule> rawRules) {
        if (rawRules == null || rawRules.isEmpty()) {
            return List.of();
        }
        List<Rule> output = new ArrayList<>(rawRules.size());
        Set<String> seen = new LinkedHashSet<>();
        for (ConfigManager.ModConfig.ChatTranslationRule rawRule : rawRules) {
            if (rawRule == null || !rawRule.enabled) {
                continue;
            }
            String source = normalizeLanguageCode(rawRule.sourceLanguage);
            String target = normalizeLanguageCode(rawRule.targetLanguage);
            if (!isSupportedLanguageCode(source) || !isSupportedLanguageCode(target) || source.equals(target)) {
                continue;
            }
            String key = source + "->" + target;
            if (!seen.add(key)) {
                continue;
            }
            output.add(new Rule(source, target));
        }
        return output;
    }

    private TranslationResult translateAuto(String text, String targetLanguage, boolean aggressiveMode) {
        String normalizedTarget = normalizeLanguageCode(targetLanguage);
        if (text == null || text.isBlank() || !isSupportedLanguageCode(normalizedTarget)) {
            return null;
        }

        long now = System.currentTimeMillis();
        TranslationResult cached = getCachedTranslation(text, normalizedTarget, now);
        if (cached != null) {
            return cached;
        }

        TranslationResult fetched = fetchGoogleFreeTranslation(text, normalizedTarget, aggressiveMode);
        if (fetched != null) {
            synchronized (this.cache) {
                this.cache.put(cacheKey(text, normalizedTarget), new CachedTranslation(fetched, now));
                pruneExpiredCacheEntriesLocked(now);
            }
        }
        return fetched;
    }

    private Map<String, List<Rule>> groupRulesByTarget(List<Rule> activeRules) {
        Map<String, List<Rule>> grouped = new LinkedHashMap<>();
        for (Rule rule : activeRules) {
            grouped.computeIfAbsent(rule.targetLanguage(), key -> new ArrayList<>()).add(rule);
        }
        return grouped;
    }

    private TranslationResult getCachedTranslation(String text, String targetLanguage, long now) {
        String normalizedTarget = normalizeLanguageCode(targetLanguage);
        if (text == null || text.isBlank() || !isSupportedLanguageCode(normalizedTarget)) {
            return null;
        }
        String cacheKey = cacheKey(text, normalizedTarget);
        synchronized (this.cache) {
            CachedTranslation cached = this.cache.get(cacheKey);
            if (cached != null && now - cached.timestamp() > CACHE_TTL_MS) {
                this.cache.remove(cacheKey);
                return null;
            }
            return cached == null ? null : cached.result();
        }
    }

    private static String cacheKey(String text, String normalizedTargetLanguage) {
        return text + "|" + normalizedTargetLanguage;
    }

    private void postMatchingRules(
        String authorPrefix,
        String original,
        List<Rule> targetRules,
        TranslationResult translated,
        boolean aggressiveMode
    ) {
        if (targetRules == null || targetRules.isEmpty() || translated == null || translated.text().isBlank()) {
            return;
        }
        String detectedLanguage = normalizeLanguageCode(translated.detectedLanguage());
        boolean posted = false;
        for (Rule rule : targetRules) {
            if (!rule.sourceLanguage().equals(detectedLanguage)) {
                continue;
            }
            postTranslatedLine(authorPrefix, original, rule, translated.text(), aggressiveMode);
            posted = true;
        }
        if (!posted && aggressiveMode) {
            postTranslatedLine(authorPrefix, original, targetRules.getFirst(), translated.text(), true);
        }
    }

    private void pruneExpiredCacheEntriesLocked(long now) {
        this.cache.entrySet().removeIf(entry -> now - entry.getValue().timestamp() > CACHE_TTL_MS);
    }

    private TranslationResult fetchGoogleFreeTranslation(String text, String targetLanguage, boolean aggressiveMode) {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
            + targetLanguage + "&dt=t&q=" + encodedText;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(aggressiveMode ? AGGRESSIVE_REQUEST_TIMEOUT : REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .header("User-Agent", "MinepieceQoL/1.0")
            .GET()
            .build();

        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logTranslationFailure("request", exception);
            return null;
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logTranslationFailure("status", new IllegalStateException("status=" + response.statusCode()));
            return null;
        }

        return parseGoogleResponse(response.body());
    }

    private TranslationResult parseGoogleResponse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (RuntimeException ex) {
            logTranslationFailure("json", ex);
            return null;
        }
        if (!root.isJsonArray()) {
            return null;
        }

        JsonArray payload = root.getAsJsonArray();
        if (payload.isEmpty()) {
            return null;
        }

        StringBuilder translated = new StringBuilder();
        JsonElement chunksElement = payload.get(0);
        if (chunksElement.isJsonArray()) {
            JsonArray chunks = chunksElement.getAsJsonArray();
            for (JsonElement chunk : chunks) {
                if (!chunk.isJsonArray()) {
                    continue;
                }
                JsonArray chunkArray = chunk.getAsJsonArray();
                if (chunkArray.isEmpty()) {
                    continue;
                }
                JsonElement token = chunkArray.get(0);
                if (token != null && token.isJsonPrimitive()) {
                    translated.append(token.getAsString());
                }
            }
        }

        String translatedText = translated.toString().trim();
        if (translatedText.isBlank()) {
            return null;
        }

        String detectedLanguage = "";
        if (payload.size() > 2 && payload.get(2).isJsonPrimitive()) {
            detectedLanguage = normalizeLanguageCode(payload.get(2).getAsString());
        }
        return new TranslationResult(translatedText, detectedLanguage);
    }

    private void postTranslatedLine(String authorPrefix, String original, Rule rule, String translatedText, boolean aggressiveMode) {
        String normalizedTranslated = normalizeWhitespace(translatedText);
        if (normalizedTranslated.isBlank()) {
            return;
        }
        if (!aggressiveMode
            && Objects.equals(normalizeWhitespace(original).toLowerCase(Locale.ROOT), normalizedTranslated.toLowerCase(Locale.ROOT))) {
            return;
        }

        String sourceCode = rule.sourceLanguage().toUpperCase(Locale.ROOT);
        String targetCode = rule.targetLanguage().toUpperCase(Locale.ROOT);
        String clipped = clip(normalizedTranslated, MAX_TRANSLATION_CHARS);
        String outgoing = authorPrefix == null || authorPrefix.isBlank()
            ? String.format(Locale.ROOT, "[TR %s->%s] %s", sourceCode, targetCode, clipped)
            : String.format(Locale.ROOT, "[TR %s->%s] %s › %s", sourceCode, targetCode, authorPrefix, clipped);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.inGameHud != null && client.inGameHud.getChatHud() != null) {
                client.inGameHud.getChatHud().addMessage(Text.literal(outgoing));
            }
        });
    }

    private static String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }

    private static String clip(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        if (maxLength <= 3) {
            return text.substring(0, Math.max(0, maxLength));
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private void logTranslationFailure(String stage, Throwable throwable) {
        long now = System.currentTimeMillis();
        if (now - this.lastErrorLogEpochMs < ERROR_LOG_THROTTLE_MS) {
            return;
        }
        this.lastErrorLogEpochMs = now;
        this.debugLogManager.logInternal("[TRANSLATE] " + stage + " failed: "
            + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
    }

    public record LanguageOption(String code, String label) {
    }

    private record TranslationResult(String text, String detectedLanguage) {
    }

    private record CachedTranslation(TranslationResult result, long timestamp) {
    }

    private record Rule(String sourceLanguage, String targetLanguage) {
    }

    public enum ChatLineType {
        PUBLIC,
        PRIVATE,
        SYSTEM,
        INVALID
    }

    public record ChatLineClassification(
        ChatLineType lineType,
        String authorPrefix,
        String messageBody,
        String divider,
        boolean valid,
        String reason
    ) {
    }

    private record DividerMatch(int index, String symbol) {
    }

    private static int computeWorkerCount() {
        int available = Runtime.getRuntime().availableProcessors();
        return Math.max(MIN_TRANSLATION_WORKERS, Math.min(MAX_TRANSLATION_WORKERS, available));
    }

    private static DividerMatch findMessageDivider(String line) {
        if (line == null || line.isBlank()) {
            return new DividerMatch(-1, "");
        }
        for (char divider : MESSAGE_DIVIDERS) {
            int index = line.indexOf(divider);
            if (index >= 0) {
                return new DividerMatch(index, Character.toString(divider));
            }
        }
        return new DividerMatch(-1, "");
    }

    private static boolean looksLikePrivatePrefix(String authorPrefix) {
        if (authorPrefix == null || authorPrefix.isBlank()) {
            return false;
        }
        String lower = authorPrefix.toLowerCase(Locale.ROOT);
        return lower.contains("->")
            || lower.contains("<-")
            || lower.contains(" pm ")
            || lower.contains(" msg ")
            || lower.contains(" whisper ")
            || lower.contains(" tell ")
            || lower.startsWith("to ")
            || lower.startsWith("from ")
            || lower.startsWith("[pm")
            || lower.startsWith("[msg");
    }
}
