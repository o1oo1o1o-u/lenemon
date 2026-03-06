package com.lenemon.vote;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The type Vote reward storage.
 */
public class VoteRewardStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getFile(MinecraftServer server) {
        File dir = new File(
                server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile(),
                "custommenu"
        );
        dir.mkdirs();
        return new File(dir, "pending_votes.json");
    }

    /**
     * Add pending vote.
     *
     * @param server     the server
     * @param playerName the player name
     */
    public static void addPendingVote(MinecraftServer server, String playerName) {
        File file = getFile(server);

        List<String> pending = load(server);
        pending.add(playerName);
        save(server, pending);
    }

    /**
     * Load list.
     *
     * @param server the server
     * @return the list
     */
    public static List<String> load(MinecraftServer server) {
        File file = getFile(server);

        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            List<String> list = new ArrayList<>();
            array.forEach(el -> list.add(el.getAsString()));
            return list;
        } catch (Exception e) {
            System.err.println("[Vote] Erreur chargement : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Save.
     *
     * @param server  the server
     * @param pending the pending
     */
    public static void save(MinecraftServer server, List<String> pending) {
        try {
            JsonArray array = new JsonArray();
            pending.forEach(array::add);
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(getFile(server)), StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception e) {
            System.err.println("[Vote] Erreur sauvegarde : " + e.getMessage());
        }
    }
}