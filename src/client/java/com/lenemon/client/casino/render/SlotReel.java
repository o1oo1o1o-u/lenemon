package com.lenemon.client.casino.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The type Slot reel.
 */
public class SlotReel {

    /**
     * The enum State.
     */
    public enum State {
        /**
         * Idle state.
         */
        IDLE,
        /**
         * Spinning state.
         */
        SPINNING,
        /**
         * Stopping state.
         */
        STOPPING }

    // ── Config ───────────────────────────────────────────────────────────
    private static final float ACCEL         = 0.8f;   // px/tick d'accélération
    private static final float MAX_SPEED     = 12.0f;  // px/tick vitesse max
    private static final float DECEL         = 0.5f;   // px/tick de décélération
    private static final int   ICON_SIZE     = 32;  // taille apparente (scaled)
    private static final int   ICON_GAP      = 8;   // espace entre icônes, un peu plus aéré
    private static final int   CELL_HEIGHT   = ICON_SIZE + ICON_GAP; // 40px

    // ── État ─────────────────────────────────────────────────────────────
    private State state        = State.IDLE;
    private float speed        = 0f;
    private float offset       = 0f;   // 0 → CELL_HEIGHT, défilement vers le bas
    private int   topIndex     = 0;    // index de l'icône tout en haut de la bande
    private int   targetIndex  = -1;   // icône sur laquelle s'arrêter (au centre)
    private int   targetOffset = 0;    // offset pixel pour aligner la cible au centre

    private final List<Integer> band;  // séquence d'indices d'icônes sur la bande
    private final int iconCount;
    private final Random random = new Random();

    // résultat final (icône visible au centre quand IDLE)
    private int resultIndex = 0;

    /**
     * Instantiates a new Slot reel.
     *
     * @param iconCount the icon count
     */
    public SlotReel(int iconCount) {
        this.iconCount = iconCount;
        this.band = new ArrayList<>();
        buildBand();
    }

    // ── Construit une bande aléatoire de ~16 icônes ──────────────────────
    private void buildBand() {
        band.clear();
        for (int i = 0; i < 16; i++) {
            band.add(random.nextInt(iconCount));
        }
    }

    /**
     * Spin.
     */
// ── Démarre le spin ───────────────────────────────────────────────────
    public void spin() {
        if (state != State.IDLE) return;
        buildBand();
        topIndex = 0;
        offset   = 0f;
        speed    = 0f;
        state    = State.SPINNING;
    }

    /**
     * Stop on.
     *
     * @param iconIndex the icon index
     */
// ── Demande l'arrêt sur un index d'icône précis ───────────────────────
    // appelez ça depuis CasinoScreenHandler quand le résultat est connu
    public void stopOn(int iconIndex) {
        if (state != State.SPINNING) return;

        targetIndex = iconIndex;

        // On force la bande pour que l'icône cible soit exactement à topIndex + 1
        band.clear();

        // icône au dessus
        band.add(random.nextInt(iconCount));

        // icône centrale = target
        band.add(iconIndex);

        // icône en dessous
        band.add(random.nextInt(iconCount));

        topIndex = 0;
        offset = 0f;
        speed = 0f;

        state = State.IDLE;
        resultIndex = iconIndex;
    }

    /**
     * Tick.
     */
// ── Tick (appelé chaque frame depuis render via Screen.tick) ──────────
    public void tick() {
        switch (state) {
            case SPINNING -> {
                speed = Math.min(speed + ACCEL, MAX_SPEED);
                advance();
            }
            case STOPPING -> {
                speed = Math.max(speed - DECEL, 1.5f);
                advance();
                // vérifie si on est aligné sur la cible
                if (speed <= 2f && isAligned()) {
                    snapToTarget();
                    state = State.IDLE;
                    resultIndex = getCurrentCenterIcon();
                }
            }
            case IDLE -> {}
        }
    }

    private void advance() {
        offset += speed;
        while (offset >= CELL_HEIGHT) {
            offset -= CELL_HEIGHT;
            topIndex = (topIndex + 1) % band.size();
        }
    }

    // Vérifie que l'offset est proche de 0 (icône bien alignée)
    private boolean isAligned() {
        return offset < speed + 1f;
    }

    private void snapToTarget() {
        offset = 0f;
    }

    // ── Getters pour le renderer ──────────────────────────────────────────

    /**
     * Gets state.
     *
     * @return the state
     */
    public State getState() { return state; }

    /**
     * Gets result index.
     *
     * @return the result index
     */
    public int   getResultIndex() { return resultIndex; }

    /**
     * Is idle boolean.
     *
     * @return the boolean
     */
    public boolean isIdle() { return state == State.IDLE; }

    /**
     * Retourne la liste des icônes à dessiner avec leur offset Y relatif.
     * Le renderer itère dessus et applique le scissor.
     *
     * @param visibleHeight hauteur de la fenêtre visible en px
     * @return liste de DrawEntry (iconIndex, yOffset depuis le haut de la fenêtre)
     */
    public List<DrawEntry> getDrawEntries(int visibleHeight) {
        List<DrawEntry> entries = new ArrayList<>();
        int needed = (visibleHeight / CELL_HEIGHT) + 2; // icônes nécessaires + marge

        for (int i = 0; i < needed; i++) {
            int bandIdx  = (topIndex + i) % band.size();
            int iconIdx  = band.get(bandIdx);
            float yPos   = i * CELL_HEIGHT - offset;
            entries.add(new DrawEntry(iconIdx, yPos));
        }
        return entries;
    }

    private int getCurrentCenterIcon() {
        int centerY = 0; // centre de la fenêtre en relatif
        // l'icône dont le centre est le plus proche du milieu de la fenêtre
        // simplifié : c'est topIndex + 1 quand offset ~ 0
        return band.get((topIndex + 1) % band.size());
    }

    /**
     * The type Draw entry.
     */
    public record DrawEntry(int iconIndex, float yOffset) {}

    /**
     * Gets cell height.
     *
     * @return the cell height
     */
    public static int getCellHeight() { return CELL_HEIGHT; }

    /**
     * Gets icon size.
     *
     * @return the icon size
     */
    public static int getIconSize()   { return ICON_SIZE; }
}