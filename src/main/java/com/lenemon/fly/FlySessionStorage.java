package com.lenemon.fly;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The type Fly session storage.
 */
public class FlySessionStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getFile(MinecraftServer server) {
        File dir = new File(
                server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile(),
                "custommenu"
        );
        dir.mkdirs();
        return new File(dir, "fly_sessions.json");
    }

    /**
     * Save.
     *
     * @param server   the server
     * @param sessions the sessions
     */
    public static void save(MinecraftServer server, Map<UUID, Integer> sessions) {
        try {
            JsonObject json = new JsonObject();
            sessions.forEach((uuid, seconds) ->
                    json.addProperty(uuid.toString(), seconds));
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(getFile(server)), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            System.err.println("[Fly] Erreur sauvegarde sessions : " + e.getMessage());
        }
    }

    /**
     * Load map.
     *
     * @param server the server
     * @return the map
     */
    public static Map<UUID, Integer> load(MinecraftServer server) {
        Map<UUID, Integer> sessions = new HashMap<>();
        File file = getFile(server);
        if (!file.exists()) return sessions;
        try (Reader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            json.entrySet().forEach(entry -> {
                try {
                    sessions.put(UUID.fromString(entry.getKey()),
                            entry.getValue().getAsInt());
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            System.err.println("[Fly] Erreur chargement sessions : " + e.getMessage());
        }
        return sessions;
    }

    /**
     * Delete.
     *
     * @param server the server
     */
    public static void delete(MinecraftServer server) {
        getFile(server).delete();
    }
}