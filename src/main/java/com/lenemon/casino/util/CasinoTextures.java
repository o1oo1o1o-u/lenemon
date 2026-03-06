package com.lenemon.casino.util;

import net.minecraft.util.Identifier;

/**
 * The type Casino textures.
 */
public final class CasinoTextures {

    private CasinoTextures() {}

    /**
     * The constant BACKGROUND.
     */
// ── Textures ────────────────────────────────────────────────────────
    public static final Identifier BACKGROUND   = Identifier.of("lenemon", "textures/gui/casino/casino_background.png");
    /**
     * The constant SLOT_LEFT.
     */
    public static final Identifier SLOT_LEFT    = Identifier.of("lenemon", "textures/gui/casino/casino_slot_left.png");
    /**
     * The constant SLOT_RIGHT.
     */
    public static final Identifier SLOT_RIGHT   = Identifier.of("lenemon", "textures/gui/casino/casino_slot_right.png");
    /**
     * The constant BTN_NORMAL.
     */
    public static final Identifier BTN_NORMAL   = Identifier.of("lenemon", "textures/gui/casino/casino_btn_normal.png");
    /**
     * The constant BTN_CLICKED.
     */
    public static final Identifier BTN_CLICKED  = Identifier.of("lenemon", "textures/gui/casino/casino_btn_clicked.png");
    /**
     * The constant ICONS.
     */
    public static final Identifier ICONS        = Identifier.of("lenemon", "textures/gui/casino/casino_icons.png");

    /**
     * The constant TEX_SIZE.
     */
// ── Taille du canvas texture (toujours 256x256) ─────────────────────
    public static final int TEX_SIZE = 256;
    /**
     * The constant GUI_U_OFFSET.
     */
    public static final int GUI_U_OFFSET = 31;

    /**
     * The constant SLOT_L_TX.
     */
// ── Coordonnées issues de Photoshop (espace texture 256x256) ────────
    // Slot gauche
    public static final int SLOT_L_TX = 43 - GUI_U_OFFSET, /**
     * The Slot l ty.
     */
    SLOT_L_TY = 36;
    /**
     * The constant SLOT_L_TW.
     */
    public static final int SLOT_L_TW = 38, /**
     * The Slot l th.
     */
    SLOT_L_TH = 109;

    /**
     * The constant SLOT_R_TX.
     */
// Slot droit
    public static final int SLOT_R_TX = 175 - GUI_U_OFFSET, /**
     * The Slot r ty.
     */
    SLOT_R_TY = 37;
    /**
     * The constant SLOT_R_TW.
     */
    public static final int SLOT_R_TW = 40, /**
     * The Slot r th.
     */
    SLOT_R_TH = 107;

    /**
     * The constant CENTER_TX.
     */
// Zone centrale
    public static final int CENTER_TX = 95 - GUI_U_OFFSET, /**
     * The Center ty.
     */
    CENTER_TY = 45;
    /**
     * The constant CENTER_TW.
     */
    public static final int CENTER_TW = 66, /**
     * The Center th.
     */
    CENTER_TH = 101;

    /**
     * The constant BTN_CX.
     */
// Bouton SPIN (centre)
    public static final int BTN_CX = 128 - GUI_U_OFFSET, /**
     * The Btn cy.
     */
    BTN_CY = 183;
    /**
     * The constant BTN_W.
     */
    public static final int BTN_W  = 52, /**
     * The Btn h.
     */
    BTN_H  = 52; // rayon ~26px autour du centre

    /**
     * The constant ICON_SIZE.
     */
// ── Spritesheet icônes : 4 colonnes x 2 lignes, chaque icône 32x32 ──
    public static final int ICON_SIZE   = 32;
    /**
     * The constant ICON_COUNT.
     */
    public static final int ICON_COUNT  = 4;  // nombre d'icônes différentes

    /**
     * Icon u int.
     *
     * @param index the index
     * @return the int
     */
// ligne 0 = normal, ligne 1 = glow victoire
    public static int iconU(int index)          { return index * ICON_SIZE; }

    /**
     * Icon v int.
     *
     * @param glowState the glow state
     * @return the int
     */
    public static int iconV(boolean glowState)  { return glowState ? ICON_SIZE : 0; }

    /**
     * The constant GUI_WIDTH.
     */
// ── Taille d'affichage du GUI (échelle 1:1 avec la texture) ─────────
    public static final int GUI_WIDTH  = 193;
    /**
     * The constant GUI_HEIGHT.
     */
    public static final int GUI_HEIGHT = 256;

    /**
     * The constant BTN_U.
     */
    public static final int BTN_U = 128 - (BTN_W / 2);
    /**
     * The constant BTN_V.
     */
    public static final int BTN_V = 183 - (BTN_H / 2);

}