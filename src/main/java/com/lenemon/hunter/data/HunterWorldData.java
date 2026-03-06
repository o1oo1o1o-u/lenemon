package com.lenemon.hunter.data;

import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * The type Hunter world data.
 */
public class HunterWorldData {

    private static final Map<UUID, HunterPlayerData> playerData = new HashMap<>();
    private static MinecraftServer server;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Register.
     *
     * @param srv the srv
     */
    public static void register(MinecraftServer srv) {
        server = srv;
        load();
    }

    /**
     * Get hunter player data.
     *
     * @param uuid the uuid
     * @return the hunter player data
     */
    public static HunterPlayerData get(UUID uuid) {
        return playerData.computeIfAbsent(uuid, HunterPlayerData::new);
    }

    /**
     * Save.
     */
    public static void save() {
        try {
            File file = getSaveFile();
            file.getParentFile().mkdirs();
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();

            for (Map.Entry<UUID, HunterPlayerData> entry : playerData.entrySet()) {
                HunterPlayerData data = entry.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("uuid", data.playerUuid.toString());
                obj.addProperty("level", data.level);
                obj.addProperty("xp", data.xp);

                // quêtes actives
                JsonObject quests = new JsonObject();
                for (Map.Entry<String, Integer> q : data.activeQuestProgress.entrySet()) {
                    quests.addProperty(q.getKey(), q.getValue());
                }
                obj.add("questProgress", quests);

                array.add(obj);
            }

            root.add("players", array);
            try (FileWriter w = new FileWriter(file)) { w.write(GSON.toJson(root)); }

        } catch (Exception e) {
            System.err.println("[Hunter] Erreur sauvegarde : " + e.getMessage());
        }
    }

    /**
     * Load.
     */
    public static void load() {
        try {
            File file = getSaveFile();
            if (!file.exists()) return;

            JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            playerData.clear();

            for (JsonElement el : root.getAsJsonArray("players")) {
                JsonObject obj = el.getAsJsonObject();
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                HunterPlayerData data = new HunterPlayerData(uuid);
                data.level = obj.get("level").getAsInt();
                data.xp    = obj.get("xp").getAsLong();

                if (obj.has("questProgress")) {
                    for (Map.Entry<String, JsonElement> q : obj.getAsJsonObject("questProgress").entrySet()) {
                        data.activeQuestProgress.put(q.getKey(), q.getValue().getAsInt());
                    }
                }
                playerData.put(uuid, data);
            }

        } catch (Exception e) {
            System.err.println("[Hunter] Erreur chargement : " + e.getMessage());
        }
    }

    private static File getSaveFile() {
        return new File(server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile(), "lenemon_hunter.json");
    }
}