package com.lenemon.client.hud;

/**
 * The type Hud config.
 */
public class HudConfig {

    /**
     * The constant x.
     */
// Panel tête
    public static int x = 8;
    /**
     * The constant y.
     */
    public static int y = 8;
    /**
     * The constant panelWidth.
     */
    public static int panelWidth = 62;
    /**
     * The constant panelHeight.
     */
    public static int panelHeight = 62;

    /**
     * The constant colorBg.
     */
// Couleurs (ARGB)
    public static int colorBg = 0xCC353854;
    /**
     * The constant colorBorder.
     */
    public static int colorBorder = 0xFF5E6496;
    /**
     * The constant colorBalance.
     */
    public static int colorBalance = 0xFFEAEAEA;

    /**
     * The constant panelTexX.
     */
// Panel texture
    public static int panelTexX = 59;
    /**
     * The constant panelTexY.
     */
    public static int panelTexY = 9;
    /**
     * The constant panelTexW.
     */
    public static int panelTexW = 170;
    /**
     * The constant panelTexH.
     */
    public static int panelTexH = 60;

    /**
     * The constant coinOffsetX.
     */
// Icône pièce
    public static int coinOffsetX = 75;
    /**
     * The constant coinOffsetY.
     */
    public static int coinOffsetY = 5;
    /**
     * The constant coinSize.
     */
    public static int coinSize = 24;

    /**
     * The constant textScale.
     */
// Texte balance
    public static float textScale = 1.5f;
    /**
     * The constant textOffsetX.
     */
    public static int textOffsetX = 105;
    /**
     * The constant textOffsetY.
     */
    public static int textOffsetY = 13;

    /**
     * The constant balancePanelW.
     */
// Panel balance
    public static int balancePanelW = 137;

    /**
     * The constant hunterPanelOffsetY.
     */
// Panel hunter
    public static int hunterPanelOffsetY = 5;  // décalage Y par rapport au panel balance
    /**
     * The constant hunterPanelW.
     */
    public static int hunterPanelW       = 137; // même largeur que balance
    /**
     * The constant hunterPanelH.
     */
    public static int hunterPanelH       = 20;
    /**
     * The constant barOffsetX.
     */
    public static int barOffsetX         = 4;
    /**
     * The constant barOffsetY.
     */
    public static int barOffsetY         = 10;
    /**
     * The constant barWidth.
     */
    public static int barWidth           = 129;
    /**
     * The constant barHeight.
     */
    public static int barHeight          = 6;
    /**
     * The constant colorBarBg.
     */
    public static int colorBarBg         = 0xAA333333;
    /**
     * The constant colorBarFill.
     */
    public static int colorBarFill       = 0xFF4FC3F7; // bleu clair
    /**
     * The constant colorLevelText.
     */
    public static int colorLevelText     = 0xFFEAEAEA;
    /**
     * The constant levelTextScale.
     */
    public static float levelTextScale   = 1.2f;
    /**
     * The constant levelTextOffsetX.
     */
    public static int levelTextOffsetX   = 4;
    /**
     * The constant levelTextOffsetY.
     */
    public static int levelTextOffsetY   = 2;

    // ── Hotbar custom ────────────────────────────────────────────────────────

    /** Décalage depuis le bas de l'écran jusqu'au bord supérieur des slots. */
    public static int hotbarBottomOffset = 36;

    /** Fond semi-transparent de la hotbar. */
    public static int hotbarBg           = 0xBB0D0D1A;
    /** Bordure de la hotbar et des slots non-sélectionnés. */
    public static int hotbarBorder       = 0xFF1A2A4A;
    /** Fond d'un slot normal. */
    public static int slotBg             = 0x881A1A2E;
    /** Fond du slot actuellement sélectionné. */
    public static int slotSelectedBg     = 0xCC1E3A6E;
    /** Bordure du slot actuellement sélectionné (bleu vif). */
    public static int slotSelectedBorder = 0xFF4A90E2;

    // ── Barre XP joueur (hotbar) ─────────────────────────────────────────────
    // colorBarBg et colorBarFill déjà définis ci-dessus — réutilisés dans HotbarRenderer.

    /** Couleur du texte de niveau XP (or). */
    public static int colorXpText        = 0xFFFFD700;

    // ── Barres de vie / nourriture ────────────────────────────────────────────

    /** Couleur de remplissage de la barre de vie (rouge). */
    public static int colorHeartFill     = 0xFFE53935;
    /** Couleur de fond de la barre de vie. */
    public static int colorHeartBg       = 0x88660000;
    /** Couleur de remplissage de la barre de nourriture (orange). */
    public static int colorFoodFill      = 0xFFFF8C00;
    /** Couleur de fond de la barre de nourriture. */
    public static int colorFoodBg        = 0x88442200;

    // ── Barre de vol Pokémon ──────────────────────────────────────────────────

    /** Couleur de remplissage de la barre de vol (violet). */
    public static int colorFlightFill    = 0xFF7C4DFF;
    /** Couleur de fond de la barre de vol. */
    public static int colorFlightBg      = 0xAA1A0033;
    /** Couleur de bordure de la barre de vol. */
    public static int colorFlightBorder  = 0xFF7C4DFF;
    /** Couleur de l'accent lumineux sur le bord droit du fill et du texte label. */
    public static int colorFlightGlow    = 0xFFAB82FF;
}