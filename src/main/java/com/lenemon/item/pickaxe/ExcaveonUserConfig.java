package com.lenemon.item.pickaxe;

/**
 * Immutable DTO representing the player-chosen configuration of an Excaveon pickaxe.
 * Stored as an NBT sub-compound on the item stack via ExcaveonPickaxe.setUserConfig().
 */
public final class ExcaveonUserConfig {

    public final boolean autoSell;
    public final boolean autoSmelt;
    /** One of ExcaveonPickaxe.MODE_* constants. */
    public final String miningMode;

    public ExcaveonUserConfig(boolean autoSell, boolean autoSmelt, String miningMode) {
        this.autoSell   = autoSell;
        this.autoSmelt  = autoSmelt;
        this.miningMode = miningMode;
    }

    // ── Zone de minage ────────────────────────────────────────────────────────

    /**
     * Dimensions complètes d'une zone de minage.
     * xFrom/xTo : bornes du décalage X (perpendiculaire au regard)
     * yFrom/yTo : bornes du décalage Y (vertical)
     * depth     : nombre de couches en profondeur (dz de 0 à depth-1)
     *             0 = pas de minage étendu (mode 1x1)
     */
    public record MiningZone(int xFrom, int xTo, int yFrom, int yTo, int depth) {
        public boolean isArea() { return depth > 0; }
    }

    /** Retourne les dimensions de la zone pour le mode choisi. */
    public MiningZone zone() {
        return switch (miningMode) {
            case ExcaveonPickaxe.MODE_1X1   -> new MiningZone( 0,  0,  0,  0, 0);
            case ExcaveonPickaxe.MODE_3X3X1 -> new MiningZone(-1,  1, -1,  1, 1);
            case ExcaveonPickaxe.MODE_3X3X2 -> new MiningZone(-1,  1, -1,  1, 2);
            case ExcaveonPickaxe.MODE_3X3X3 -> new MiningZone(-1,  1, -1,  1, 3);
            case ExcaveonPickaxe.MODE_5X5X2 -> new MiningZone(-2,  2, -1,  3, 2); // -2..+2 = 5 wide, -1..+3 = 5 tall, prof. 2
            case ExcaveonPickaxe.MODE_5X5X3 -> new MiningZone(-2,  2, -1,  3, 3); // -2..+2 = 5 wide, -1..+3 = 5 tall
            default                          -> new MiningZone( 0,  0,  0,  0, 0);
        };
    }

    // ── Niveau minimum requis par mode ────────────────────────────────────────

    /** Vérifie si le mode est débloqué au niveau donné. */
    public static boolean isModeUnlocked(String mode, int level) {
        int minLevel = switch (mode) {
            case ExcaveonPickaxe.MODE_1X1   -> 1;
            case ExcaveonPickaxe.MODE_3X3X1 -> 2;
            case ExcaveonPickaxe.MODE_3X3X2 -> 2;
            case ExcaveonPickaxe.MODE_3X3X3 -> 3;
            case ExcaveonPickaxe.MODE_5X5X2 -> 4;
            case ExcaveonPickaxe.MODE_5X5X3 -> 5;
            default -> Integer.MAX_VALUE;
        };
        return level >= minLevel;
    }

    /** Niveau minimum auquel un mode est débloqué (pour les tooltips GUI). */
    public static int modeUnlockLevel(String mode) {
        return switch (mode) {
            case ExcaveonPickaxe.MODE_1X1   -> 1;
            case ExcaveonPickaxe.MODE_3X3X1 -> 2;
            case ExcaveonPickaxe.MODE_3X3X2 -> 2;
            case ExcaveonPickaxe.MODE_3X3X3 -> 3;
            case ExcaveonPickaxe.MODE_5X5X2 -> 4;
            case ExcaveonPickaxe.MODE_5X5X3 -> 5;
            default -> 5;
        };
    }

    // ── Valeurs par défaut ────────────────────────────────────────────────────

    /**
     * Valeurs par défaut pour un niveau donné.
     * autoSmelt = true (comportement identique à l'ancien système).
     * miningMode = meilleur mode débloqué au niveau actuel.
     */
    public static ExcaveonUserConfig defaults(int level) {
        ExcaveonLevel lvl = ExcaveonLevel.fromLevel(level);
        return new ExcaveonUserConfig(lvl.autoSell, true, bestModeForLevel(level));
    }

    /** Meilleur mode (zone maximale) disponible au niveau donné. */
    public static String bestModeForLevel(int level) {
        return switch (level) {
            case 1  -> ExcaveonPickaxe.MODE_1X1;
            case 2  -> ExcaveonPickaxe.MODE_3X3X2;
            case 3  -> ExcaveonPickaxe.MODE_3X3X3;
            case 4  -> ExcaveonPickaxe.MODE_5X5X2;
            default -> ExcaveonPickaxe.MODE_5X5X3;
        };
    }
}
