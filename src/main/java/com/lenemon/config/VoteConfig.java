package com.lenemon.config;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * The type Vote config.
 */
public class VoteConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String voteUrl = "https://lenemon.web.boxtoplay.com/vote";
    private static File configFile;

    /**
     * Load.
     *
     * @param server the server
     */
    public static void load(MinecraftServer server) {
        configFile = server.getRunDirectory().resolve("config/custommenu_vote.json").toFile();
        configFile.getParentFile().mkdirs();

        if (!configFile.exists()) {
            save();
            return;
        }

        try (Reader reader = new InputStreamReader(
                new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            voteUrl = json.get("voteUrl").getAsString();
        } catch (Exception e) {
            System.err.println("[Vote] Erreur chargement config : " + e.getMessage());
        }
    }

    /**
     * Save.
     */
    public static void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("voteUrl", voteUrl);
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            System.err.println("[Vote] Erreur sauvegarde config : " + e.getMessage());
        }
    }

    /**
     * Gets vote url.
     *
     * @return the vote url
     */
    public static String getVoteUrl() {
        return voteUrl;
    }
}