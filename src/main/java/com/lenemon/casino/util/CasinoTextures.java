package com.lenemon.casino.util;

import net.minecraft.util.Identifier;

/**
 * The type Casino textures.
 */
public final class CasinoTextures {

    private CasinoTextures() {}

    // ── Textures ─────────────────────────────────────────────────────────
    /**
     * The constant ICONS.
     */
    public static final Identifier ICONS = Identifier.of("lenemon", "textures/gui/casino/casino_icons.png");

    // ── Spritesheet icônes : 6 colonnes x 2 lignes, chaque icône 32x32 ──
    /**
     * The constant ICON_SIZE.
     */
    public static final int ICON_SIZE  = 32;
    /**
     * The constant ICON_COUNT.
     */
    public static final int ICON_COUNT = 6;  // nombre d'icônes différentes

    // ligne 0 = normal, ligne 1 = glow victoire
    /**
     * Icon u int.
     *
     * @param index the index
     * @return the int
     */
    public static int iconU(int index)         { return index * ICON_SIZE; }

    /**
     * Icon v int.
     *
     * @param glowState the glow state
     * @return the int
     */
    public static int iconV(boolean glowState) { return glowState ? ICON_SIZE : 0; }

    // ── Taille d'affichage du GUI ─────────────────────────────────────────
    /**
     * The constant GUI_WIDTH.
     */
    public static final int GUI_WIDTH  = 193;
    /**
     * The constant GUI_HEIGHT.
     */
    public static final int GUI_HEIGHT = 256;

    // ── Slot gauche ───────────────────────────────────────────────────────
    /**
     * The constant SLOT_L_TX.
     */
    public static final int SLOT_L_TX = 8;
    /**
     * The constant SLOT_L_TY.
     */
    public static final int SLOT_L_TY = 28;
    /**
     * The constant SLOT_L_TW.
     */
    public static final int SLOT_L_TW = 44;
    /**
     * The constant SLOT_L_TH.
     */
    public static final int SLOT_L_TH = 120;

    // ── Slot droit ────────────────────────────────────────────────────────
    /**
     * The constant SLOT_R_TX.
     */
    public static final int SLOT_R_TX = 141;
    /**
     * The constant SLOT_R_TY.
     */
    public static final int SLOT_R_TY = 28;
    /**
     * The constant SLOT_R_TW.
     */
    public static final int SLOT_R_TW = 44;
    /**
     * The constant SLOT_R_TH.
     */
    public static final int SLOT_R_TH = 120;

    // ── Zone centrale Pokémon ─────────────────────────────────────────────
    /**
     * The constant CENTER_TX.
     */
    public static final int CENTER_TX = 58;
    /**
     * The constant CENTER_TY.
     */
    public static final int CENTER_TY = 28;
    /**
     * The constant CENTER_TW.
     */
    public static final int CENTER_TW = 77;
    /**
     * The constant CENTER_TH.
     */
    public static final int CENTER_TH = 120;

    // ── Bouton SPIN (rectangle) ───────────────────────────────────────────
    /**
     * The constant BTN_X.
     */
    public static final int BTN_X  = 51;
    /**
     * The constant BTN_Y.
     */
    public static final int BTN_Y  = 168;
    /**
     * The constant BTN_W.
     */
    public static final int BTN_W  = 90;
    /**
     * The constant BTN_H.
     */
    public static final int BTN_H  = 26;
    /** Centre X — pour compatibilité éventuelle. */
    public static final int BTN_CX = BTN_X + BTN_W / 2;  // = 96
    /** Centre Y — pour compatibilité éventuelle. */
    public static final int BTN_CY = BTN_Y + BTN_H / 2;  // = 181

    // ── Palette programmatique (thème casino sombre) ──────────────────────
    public static final int COLOR_BG_OUTER         = 0xEE070D1A;
    public static final int COLOR_BG_BORDER        = 0xFF1A2A4A;
    public static final int COLOR_BG_ACCENT        = 0xFF4A90E2;
    public static final int COLOR_REEL_BG          = 0xFF080E1C;
    public static final int COLOR_REEL_BORDER      = 0xFF1E3A6E;
    public static final int COLOR_REEL_LINE        = 0x77FFD700;  // ligne cible dorée
    public static final int COLOR_CENTER_BG        = 0xFF050A14;
    public static final int COLOR_CENTER_BORDER    = 0xFF2A4A7A;
    public static final int COLOR_BTN_NORMAL       = 0xFF1A4A1A;
    public static final int COLOR_BTN_HOVER        = 0xFF2A6A2A;
    public static final int COLOR_BTN_PRESSED      = 0xFF0A1A0A;
    public static final int COLOR_BTN_LOCKED       = 0xFF1A1A2A;
    public static final int COLOR_BTN_BORDER       = 0xFF44CC44;
    public static final int COLOR_BTN_BORDER_LOCKED = 0xFF444455;
    public static final int COLOR_BTN_TEXT         = 0xFFAAFFAA;
    public static final int COLOR_BTN_TEXT_LOCKED  = 0xFF666677;
    public static final int COLOR_BET_LABEL        = 0xFF00E5FF;
    public static final int COLOR_BET_VALUE        = 0xFFFFD700;
    public static final int COLOR_WIN              = 0xFF44FF88;
    public static final int COLOR_TITLE            = 0xFFFFD700;
}
