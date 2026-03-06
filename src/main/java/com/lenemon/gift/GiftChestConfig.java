package com.lenemon.gift;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The type Gift chest config.
 */
public class GiftChestConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getFile(MinecraftServer server, UUID chestUUID) {
        File dir = new File(
                server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile(),
                "gift_chests"
        );
        dir.mkdirs();
        return new File(dir, chestUUID + ".json");
    }

    /**
     * Load list.
     *
     * @param server    the server
     * @param chestUUID the chest uuid
     * @return the list
     */
    public static List<GiftReward> load(MinecraftServer server, UUID chestUUID) {
        File file = getFile(server, chestUUID);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            List<GiftReward> rewards = new ArrayList<>();
            JsonArray array = json.getAsJsonArray("rewards");
            for (JsonElement el : array) {
                rewards.add(GSON.fromJson(el, GiftReward.class));
            }
            return rewards;
        } catch (Exception e) {
            System.err.println("[Gift] Erreur chargement config : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Save.
     *
     * @param server    the server
     * @param chestUUID the chest uuid
     * @param rewards   the rewards
     */
    public static void save(MinecraftServer server, UUID chestUUID, List<GiftReward> rewards) {
        try {
            JsonObject json = new JsonObject();
            JsonArray array = new JsonArray();
            for (GiftReward r : rewards) {
                array.add(GSON.toJsonTree(r));
            }
            json.add("rewards", array);
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(getFile(server, chestUUID)), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            System.err.println("[Gift] Erreur sauvegarde config : " + e.getMessage());
        }
    }

    /**
     * Delete.
     *
     * @param server    the server
     * @param chestUUID the chest uuid
     */
    public static void delete(MinecraftServer server, UUID chestUUID) {
        getFile(server, chestUUID).delete();
    }

    /**
     * Roll gift reward.
     *
     * @param rewards the rewards
     * @return the gift reward
     */
// Tire une récompense aléatoire selon les taux
    public static GiftReward roll(List<GiftReward> rewards) {
        if (rewards.isEmpty()) return null;
        double total = rewards.stream().mapToDouble(r -> r.chance).sum();
        double roll = Math.random() * total;
        double cumul = 0;
        for (GiftReward r : rewards) {
            cumul += r.chance;
            if (roll <= cumul) return r;
        }
        return rewards.get(rewards.size() - 1);
    }
}