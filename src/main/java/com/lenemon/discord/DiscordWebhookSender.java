package com.lenemon.discord;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The type Discord webhook sender.
 */
public class DiscordWebhookSender {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Lenemon-Discord-Webhook");
        t.setDaemon(true);
        return t;
    });

    /**
     * Send.
     *
     * @param playerName the player name
     * @param message    the message
     */
    public static void send(String playerName, String message) {
        DiscordWebhookConfig config = DiscordWebhookConfig.get();

        if (!config.isEnabled()) return;
        String url = config.getWebhookUrl();
        if (url == null || url.isBlank()) return;

        // Escape caractères spéciaux JSON
        String safePlayer  = escape(playerName);
        String safeMessage = escape(message);

        String json = String.format(
                "{\"username\":\"Chat Serveur\",\"content\":\"**%s** : %s\"}",
                safePlayer, safeMessage
        );

        EXECUTOR.submit(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "Lenemon-Mod");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code != 204 && code != 200) {
                    System.err.println("[Lenemon] Webhook Discord erreur HTTP " + code);
                }
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("[Lenemon] Webhook Discord erreur : " + e.getMessage());
            }
        });
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}