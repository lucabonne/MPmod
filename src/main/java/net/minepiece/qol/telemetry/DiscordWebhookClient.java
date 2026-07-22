package net.minepiece.qol.telemetry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

final class DiscordWebhookClient {
    static final long MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    static boolean isValidWebhook(String raw) {
        try {
            URI uri = URI.create(raw == null ? "" : raw.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean officialHost = host.equals("discord.com") || host.equals("www.discord.com")
                || host.equals("discordapp.com") || host.equals("www.discordapp.com");
            return "https".equalsIgnoreCase(uri.getScheme()) && officialHost
                && uri.getPath() != null && uri.getPath().startsWith("/api/webhooks/");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    DeliveryResult send(String webhook, String content, Path attachment) {
        return send(webhook, content, "", attachment);
    }

    DeliveryResult send(String webhook, String content, String payloadJson, Path attachment) {
        if (!isValidWebhook(webhook)) {
            return new DeliveryResult(false, 0L, "invalid webhook URL");
        }
        try {
            URI target = URI.create(webhook + (webhook.contains("?") ? "&wait=true" : "?wait=true"));
            HttpRequest.Builder request = HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(30));
            JsonObject payload = resolvePayload(content, payloadJson);
            if (attachment != null && Files.isRegularFile(attachment) && Files.size(attachment) <= MAX_ATTACHMENT_BYTES) {
                String boundary = "MinepieceQol-" + UUID.randomUUID();
                request.header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipart(boundary, payload, attachment)));
            } else {
                request.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8));
            }
            HttpResponse<String> response = this.client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new DeliveryResult(true, 0L, "");
            }
            return new DeliveryResult(false, retryAfterMs(response), "Discord HTTP " + response.statusCode());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new DeliveryResult(false, 0L, "delivery interrupted");
        } catch (IOException | RuntimeException exception) {
            return new DeliveryResult(false, 0L, exception.getClass().getSimpleName());
        }
    }

    private static byte[] multipart(String boundary, JsonObject payload, Path attachment) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"payload_json\"\r\nContent-Type: application/json\r\n\r\n");
        write(output, payload.toString());
        write(output, "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"files[0]\"; filename=\""
            + safeFilename(attachment.getFileName().toString()) + "\"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n");
        output.write(Files.readAllBytes(attachment));
        write(output, "\r\n--" + boundary + "--\r\n");
        return output.toByteArray();
    }

    private static JsonObject payload(String content) {
        JsonObject payload = new JsonObject();
        String safe = content == null ? "" : content;
        payload.addProperty("content", safe.substring(0, Math.min(2_000, safe.length())));
        addAllowedMentions(payload);
        return payload;
    }

    private static JsonObject resolvePayload(String content, String payloadJson) {
        if (payloadJson != null && !payloadJson.isBlank()) {
            try {
                JsonObject parsed = JsonParser.parseString(payloadJson).getAsJsonObject();
                addAllowedMentions(parsed);
                return parsed;
            } catch (RuntimeException ignored) {
                // Old or malformed queued payloads fall back to their plain-text content.
            }
        }
        return payload(content);
    }

    private static void addAllowedMentions(JsonObject payload) {
        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());
        payload.add("allowed_mentions", allowedMentions);
    }

    private static long retryAfterMs(HttpResponse<String> response) {
        String header = response.headers().firstValue("Retry-After").orElse("");
        try {
            if (!header.isBlank()) {
                return Math.max(0L, Math.round(Double.parseDouble(header) * 1_000.0D));
            }
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            return body.has("retry_after") ? Math.max(0L, Math.round(body.get("retry_after").getAsDouble() * 1_000.0D)) : 0L;
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    private static String safeFilename(String value) {
        return value.replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

    private static void write(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    record DeliveryResult(boolean success, long retryAfterMs, String error) {
    }
}
