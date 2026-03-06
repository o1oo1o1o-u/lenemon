package com.lenemon.hunter;

import com.lenemon.hunter.data.HunterPlayerData;
import com.lenemon.hunter.data.HunterWorldData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * The type Hunter utils.
 */
public class HunterUtils {

    /**
     * Gets level.
     *
     * @param uuid the uuid
     * @return the level
     */
    public static int getLevel(UUID uuid) {
        return HunterWorldData.get(uuid).level;
    }

    /**
     * Gets xp.
     *
     * @param uuid the uuid
     * @return the xp
     */
    public static long getXp(UUID uuid) {
        return HunterWorldData.get(uuid).xp;
    }

    /**
     * Gets xp for next level.
     *
     * @param uuid the uuid
     * @return the xp for next level
     */
    public static long getXpForNextLevel(UUID uuid) {
        return HunterPlayerData.xpForNextLevel(HunterWorldData.get(uuid).level);
    }

    /**
     * Gets level progress.
     *
     * @param uuid the uuid
     * @return the level progress
     */
// Retourne le % d'avancement dans le niveau actuel (0.0 à 1.0)
    public static float getLevelProgress(UUID uuid) {
        HunterPlayerData data = HunterWorldData.get(uuid);
        long xpNeeded = HunterPlayerData.xpForNextLevel(data.level);
        if (xpNeeded <= 0) return 1.0f;
        return Math.min(1.0f, (float) data.xp / xpNeeded);
    }

    /**
     * Gets level progress percent.
     *
     * @param uuid the uuid
     * @return the level progress percent
     */
// Retourne le % sous forme lisible ex: "67%"
    public static String getLevelProgressPercent(UUID uuid) {
        return (int)(getLevelProgress(uuid) * 100) + "%";
    }

    /**
     * Gets progress bar.
     *
     * @param uuid the uuid
     * @return the progress bar
     */
// Retourne une barre de progression visuelle ex: "§a█████§7░░░░░"
    public static String getProgressBar(UUID uuid) {
        float progress = getLevelProgress(uuid);
        int filled = (int)(progress * 20);
        int empty  = 20 - filled;
        return "§a" + "█".repeat(filled) + "§7" + "░".repeat(empty);
    }
}