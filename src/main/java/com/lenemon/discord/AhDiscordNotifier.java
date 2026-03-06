package com.lenemon.discord;

import com.lenemon.ah.AhListing;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Envoie une notification Discord lors d'une nouvelle mise en vente sur l'AH.
 */
public class AhDiscordNotifier {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Lenemon-AH-Discord");
        t.setDaemon(true);
        return t;
    });

    public static void notifyNewListing(AhListing listing) {
        DiscordWebhookConfig config = DiscordWebhookConfig.get();
        String url = config.getAhWebhookUrl();
        if (url == null || url.isBlank()) return;

        String itemName = "pokemon".equals(listing.type)
                ? listing.pokemonDisplayName
                : listing.itemDisplayName;
        long durationHours = (listing.expiresAt - listing.listedAt) / 3_600_000L;

        String safeVendeur = escape(listing.sellerName != null ? listing.sellerName : "");
        String safeItem    = escape(itemName != null ? itemName : "");

        // \uD83C\uDFEA = emoji maison de vente, \u20bd = rouble
        String json = String.format(
                "{\"embeds\":[{\"title\":\"\\uD83C\\uDFEA Nouvelle vente\",\"color\":16776960,"
                + "\"fields\":["
                + "{\"name\":\"Vendeur\",\"value\":\"%s\",\"inline\":true},"
                + "{\"name\":\"Article\",\"value\":\"%s\",\"inline\":true},"
                + "{\"name\":\"Prix\",\"value\":\"%d \\u20bd\",\"inline\":true},"
                + "{\"name\":\"Dur\\u00e9e\",\"value\":\"%dh\",\"inline\":true}"
                + "]}]}",
                safeVendeur, safeItem, listing.price, durationHours
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
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("[Lenemon] AH Discord erreur : " + e.getMessage());
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
