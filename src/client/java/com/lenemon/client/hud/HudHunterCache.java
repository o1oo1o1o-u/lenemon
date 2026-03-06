package com.lenemon.client.hud;

/**
 * The type Hud hunter cache.
 */
public class HudHunterCache {
    private static int level = 1;
    private static float progress = 0f;

    /**
     * Set.
     *
     * @param level    the level
     * @param progress the progress
     */
    public static void set(int level, float progress) {
        HudHunterCache.level    = level;
        HudHunterCache.progress = progress;
    }

    /**
     * Gets level.
     *
     * @return the level
     */
    public static int getLevel()      { return level; }

    /**
     * Gets progress.
     *
     * @return the progress
     */
    public static float getProgress() { return progress; }
}