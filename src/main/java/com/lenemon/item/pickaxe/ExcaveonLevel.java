package com.lenemon.item.pickaxe;

/**
 * The enum Excaveon level.
 */
public enum ExcaveonLevel {
    /**
     * The Level 1.
     */
    LEVEL_1("🔹 Sylvaxe lv.1",          1, 3, 3, 1, false, 0f, 1),  // centré
    /**
     * The Level 2.
     */
    LEVEL_2("🔹 Sylvaxe lv.2",         2, 3, 3, 2, true,  0f, 1),  // centré
    /**
     * The Level 3.
     */
    LEVEL_3("🔹 Sylvaxe lv.3",     3, 3, 3, 3, true,  0.10f, 1), // 1 bas, 2 haut
    /**
     * The Level 4.
     */
    LEVEL_4("🔹 Sylvaxe lv.4",     4, 4, 4, 3, true,  0.15f, 1),
    /**
     * The Level 5.
     */
    LEVEL_5("🔹 Sylvaxe lv.5", 5, 5, 5, 3, true,  0.20f, 1);

    /**
     * The Display name.
     */
    public final String displayName;
    /**
     * The Level.
     */
    public final int level;
    /**
     * The Width.
     */
    public final int width;   // X
    /**
     * The Height.
     */
    public final int height;  // Z
    /**
     * The Depth.
     */
    public final int depth;   // Y profondeur
    /**
     * The Auto sell.
     */
    public final boolean autoSell;
    /**
     * The Sell bonus.
     */
    public final float sellBonus; // 0.10 = +10%
    /**
     * The Bottom offset.
     */
    public final int bottomOffset;

    ExcaveonLevel(String displayName, int level, int width, int height, int depth, boolean autoSell, float sellBonus, int bottomOffset) {
        this.displayName = displayName;
        this.level = level;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.autoSell = autoSell;
        this.sellBonus = sellBonus;
        this.bottomOffset = bottomOffset;
    }

    /**
     * From level excaveon level.
     *
     * @param level the level
     * @return the excaveon level
     */
    public static ExcaveonLevel fromLevel(int level) {
        for (ExcaveonLevel l : values()) {
            if (l.level == level) return l;
        }
        return LEVEL_1;
    }
}