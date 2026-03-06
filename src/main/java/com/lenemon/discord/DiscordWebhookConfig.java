package com.lenemon.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

/**
 * The type Discord webhook config.
 */
public class DiscordWebhookConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("lenemon_discord.json");

    private String webhookUrl = "";
    private boolean enabled = true;
    private String ahWebhookUrl = "";

    private static DiscordWebhookConfig instance;

    /**
     * Get discord webhook config.
     *
     * @return the discord webhook config
     */
    public static DiscordWebhookConfig get() {
        if (instance == null) load();
        return instance;
    }

    /**
     * Gets webhook url.
     *
     * @return the webhook url
     */
    public String getWebhookUrl() { return webhookUrl; }

    /**
     * Is enabled boolean.
     *
     * @return the boolean
     */
    public boolean isEnabled()    { return enabled; }

    /**
     * Gets ah webhook url.
     *
     * @return the ah webhook url
     */
    public String getAhWebhookUrl() { return ahWebhookUrl; }

    /**
     * Sets enabled.
     *
     * @param enabled the enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    /**
     * Load.
     */
    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            instance = new DiscordWebhookConfig();
            instance.save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            instance = GSON.fromJson(reader, DiscordWebhookConfig.class);
            if (instance == null) instance = new DiscordWebhookConfig();
        } catch (Exception e) {
            System.err.println("[Lenemon] Erreur lecture config Discord : " + e.getMessage());
            instance = new DiscordWebhookConfig();
        }
    }

    private void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            System.err.println("[Lenemon] Erreur sauvegarde config Discord : " + e.getMessage());
        }
    }
}