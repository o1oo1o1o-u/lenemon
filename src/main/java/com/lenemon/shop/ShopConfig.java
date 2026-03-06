package com.lenemon.shop;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Shop config.
 */
public class ShopConfig {

    private static List<ShopCategory> categories = new ArrayList<>();

    /**
     * Gets categories.
     *
     * @return the categories
     */
    public static List<ShopCategory> getCategories() { return categories; }

    /**
     * Reload.
     *
     * @param server the server
     */
    public static void reload(MinecraftServer server) {
        File file = getFile(server);
        if (!file.exists()) {
            createDefault(file);
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            categories = new ArrayList<>();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                ShopCategory cat = new ShopCategory();
                cat.name = obj.get("name").getAsString();
                cat.icon = obj.get("icon").getAsString();
                cat.items = new ArrayList<>();
                for (JsonElement itemEl : obj.getAsJsonArray("items")) {
                    JsonObject itemObj = itemEl.getAsJsonObject();
                    ShopItem item = new ShopItem();
                    item.id = itemObj.get("id").getAsString();
                    item.displayName = itemObj.get("displayName").getAsString();
                    item.buyPrice = itemObj.has("buyPrice") ? itemObj.get("buyPrice").getAsDouble() : -1;
                    item.sellPrice = itemObj.has("sellPrice") ? itemObj.get("sellPrice").getAsDouble() : -1;
                    cat.items.add(item);
                }
                categories.add(cat);
            }
            ShopAutoSellService.invalidateCache();
        } catch (Exception e) {
            System.err.println("[Shop] Erreur chargement : " + e.getMessage());
        }
    }

    private static File getFile(MinecraftServer server) {
        File dir = new File(
                server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile(),
                "shop"
        );
        dir.mkdirs();
        return new File(dir, "shop.json");
    }

    private static void createDefault(File file) {
        String json = """
        [
          {
            "name": "Pokéballs",
            "icon": "cobblemon:poke_ball",
            "items": [
              { "id": "cobblemon:poke_ball", "displayName": "Poké Ball", "buyPrice": 100, "sellPrice": 50 },
              { "id": "cobblemon:great_ball", "displayName": "Super Ball", "buyPrice": 300, "sellPrice": 150 },
              { "id": "cobblemon:ultra_ball", "displayName": "Hyper Ball", "buyPrice": 600, "sellPrice": 300 },
              { "id": "cobblemon:master_ball", "displayName": "Master Ball", "buyPrice": 100000 }
            ]
          },
          {
            "name": "Minerais",
            "icon": "minecraft:diamond",
            "items": [
              { "id": "minecraft:diamond", "displayName": "Diamant", "buyPrice": 1000, "sellPrice": 800 },
              { "id": "minecraft:emerald", "displayName": "Émeraude", "sellPrice": 500 },
              { "id": "minecraft:iron_ingot", "displayName": "Lingot de Fer", "buyPrice": 50, "sellPrice": 30 }
            ]
          }
        ]
        """;
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write(json);
        } catch (Exception e) {
            System.err.println("[Shop] Erreur création fichier par défaut : " + e.getMessage());
        }
    }
}