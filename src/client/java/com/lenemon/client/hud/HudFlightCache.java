package com.lenemon.client.hud;

/**
 * Cache client-side de l'état de la barre de stamina de ride Pokémon.
 *
 * <p>La valeur de progression est désormais fournie directement par le serveur via
 * {@link com.lenemon.network.PacketHudFlight} (ratio stamina réelle / stamina max Cobblemon).
 * Ce cache ne fait plus de décompte local — il reflète fidèlement l'état serveur.
 */
public class HudFlightCache {
    private static boolean active = false;
    private static float staminaRatio = 1f;

    /**
     * Appelé à la réception d'un {@link com.lenemon.network.PacketHudFlight}.
     *
     * @param active       true si le joueur est monté sur un Pokémon volant
     * @param staminaRatio ratio stamina actuelle/max fourni par Cobblemon (0.0–1.0)
     */
    public static void set(boolean active, float staminaRatio) {
        HudFlightCache.active = active;
        HudFlightCache.staminaRatio = active ? Math.clamp(staminaRatio, 0f, 1f) : 0f;
    }

    /** Aucun décompte local — la valeur est mise à jour uniquement par le serveur. */
    public static void tick() {
        // intentionnellement vide : la stamina est synchronisée depuis le serveur
    }

    public static boolean isActive() {
        return active;
    }

    /**
     * Retourne le ratio de progression de la barre (0.0 = vide, 1.0 = plein).
     * Valeur directement issue de Cobblemon via le packet serveur.
     */
    public static float getProgress() {
        return staminaRatio;
    }
}
