package com.lenemon.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Affiche la barre de vol Pokémon sous la barre XP de la hotbar custom.
 *
 * <p>La barre n'est rendue que lorsque {@link HudFlightCache#isActive()} retourne true.
 * Elle clignote quand la progression descend sous 20 % pour alerter le joueur.
 */
public class FlightBarRenderer {

    /** Largeur identique à la hotbar (196 px). */
    private static final int BAR_WIDTH  = 196;
    private static final int BAR_HEIGHT = 7;

    /** Gap entre l'encadré niveau et la barre de vol (px). */
    private static final int FLIGHT_BAR_GAP       = 3;

    /** Seuil de progression en dessous duquel la barre clignote. */
    private static final float BLINK_THRESHOLD    = 0.2f;
    /** Période de clignotement en millisecondes (300 ms allumé / 300 ms éteint). */
    private static final long  BLINK_PERIOD_MS    = 600L;

    public static void register() {
        HudRenderCallback.EVENT.register(FlightBarRenderer::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!HudFlightCache.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.currentScreen != null) return;

        int screenWidth  = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Position Y : on remonte depuis les slots vers le haut
        // slots  →  XP bar (–8)  →  level box (–14)  →  flight bar (–10)
        int hotbarY  = screenHeight - HudConfig.hotbarBottomOffset;
        int xpBarY   = hotbarY - 8;                    // barre XP juste au-dessus des slots
        int levelBoxY = xpBarY - 14;                   // encadré niveau au-dessus de la barre XP
        int barY     = levelBoxY - FLIGHT_BAR_GAP - BAR_HEIGHT; // barre vol au-dessus de l'encadré
        int barX     = screenWidth / 2 - BAR_WIDTH / 2;

        // Lire la vraie stamina via ClientPlayerEntity.getMountJumpStrength()
        // (valeur 0-1 mise à jour automatiquement par le client depuis le TrackedData Cobblemon)
        float progress = Math.clamp(client.player.getMountJumpStrength(), 0f, 1f);
        boolean blink    = progress < BLINK_THRESHOLD
                && (System.currentTimeMillis() % BLINK_PERIOD_MS < BLINK_PERIOD_MS / 2);

        // ── Fond ─────────────────────────────────────────────────────────────
        context.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT,
                HudConfig.colorFlightBg);

        // ── Bordure ───────────────────────────────────────────────────────────
        context.fill(barX,              barY,              barX + BAR_WIDTH, barY + 1,              HudConfig.colorFlightBorder); // top
        context.fill(barX,              barY + BAR_HEIGHT - 1, barX + BAR_WIDTH, barY + BAR_HEIGHT, HudConfig.colorFlightBorder); // bottom
        context.fill(barX,              barY,              barX + 1,              barY + BAR_HEIGHT, HudConfig.colorFlightBorder); // left
        context.fill(barX + BAR_WIDTH - 1, barY,          barX + BAR_WIDTH,      barY + BAR_HEIGHT, HudConfig.colorFlightBorder); // right

        // ── Remplissage (masqué pendant le clignotement) ─────────────────────
        if (!blink) {
            int fillW = (int) ((BAR_WIDTH - 2) * progress);
            if (fillW > 0) {
                context.fill(barX + 1, barY + 1,
                        barX + 1 + fillW, barY + BAR_HEIGHT - 1,
                        HudConfig.colorFlightFill);
                // Accent lumineux sur le bord droit du fill
                if (fillW > 2) {
                    context.fill(barX + fillW - 1, barY + 1,
                            barX + fillW,    barY + BAR_HEIGHT - 1,
                            HudConfig.colorFlightGlow);
                }
            }
        }

        // ── Icône Pokéball (12×12 pixels, à gauche de la barre) ──────────────
        drawPokeballIcon(context, barX - 14, barY - 1);
    }

    /**
     * Dessine une Pokéball simplifiée en pixels (12×12).
     * Palette : rouge en haut, blanc en bas, bande noire centrale, cercle central.
     */
    private static void drawPokeballIcon(DrawContext context, int x, int y) {
        final int RED   = 0xFFE53935;
        final int WHITE = 0xFFEEEEEE;
        final int BLACK = 0xFF111111;

        // Moitié supérieure rouge
        context.fill(x + 2, y,     x + 10, y + 1,  BLACK); // arc supérieur
        context.fill(x + 1, y + 1, x + 11, y + 2,  RED);
        context.fill(x,     y + 2, x + 12, y + 5,  RED);

        // Bande centrale noire
        context.fill(x,     y + 5, x + 12, y + 7,  BLACK);

        // Moitié inférieure blanche
        context.fill(x,     y + 7, x + 12, y + 10, WHITE);
        context.fill(x + 1, y + 10, x + 11, y + 11, WHITE);
        context.fill(x + 2, y + 11, x + 10, y + 12, BLACK); // arc inférieur

        // Petit cercle central (blanc + point noir)
        context.fill(x + 4, y + 5, x + 8,  y + 7,  WHITE);
        context.fill(x + 5, y + 4, x + 7,  y + 8,  WHITE);
        context.fill(x + 5, y + 5, x + 7,  y + 7,  BLACK);
    }
}
