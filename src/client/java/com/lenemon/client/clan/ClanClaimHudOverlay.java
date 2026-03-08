package com.lenemon.client.clan;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Overlay HUD affiche en bas a gauche quand le mode claim est actif.
 * Informe le joueur de son compteur de claims et des commandes disponibles.
 */
public class ClanClaimHudOverlay {

    /** Fond semi-transparent du panneau (ARGB). */
    private static final int COLOR_BG          = 0xBB000000;
    /** Marge interieure du panneau. */
    private static final int PADDING           = 6;
    /** Hauteur d'une ligne de texte (textRenderer.fontHeight = 9). */
    private static final int LINE_HEIGHT       = 11;
    /** Nombre de lignes affichees. */
    private static final int LINE_COUNT        = 5;
    /** Decalage depuis le bas de l'ecran. */
    private static final int BOTTOM_MARGIN     = 80;

    private ClanClaimHudOverlay() {}

    /**
     * Enregistre l'overlay sur HudRenderCallback.EVENT.
     */
    public static void register() {
        HudRenderCallback.EVENT.register(ClanClaimHudOverlay::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!ClanClaimSession.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        int screenWidth  = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int panelWidth  = 160;
        int panelHeight = PADDING * 2 + LINE_COUNT * LINE_HEIGHT;
        int panelX      = 8;
        int panelY      = screenHeight - BOTTOM_MARGIN - panelHeight;

        // Fond semi-transparent
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BG);

        int textX = panelX + PADDING;
        int textY = panelY + PADDING;

        // Ligne 1 : titre
        context.drawText(
                client.textRenderer,
                "\u00a7b\u00a7lMode Claim Actif",
                textX, textY,
                0xFFFFFFFF,
                true
        );

        // Ligne 2 : compteur
        context.drawText(
                client.textRenderer,
                "\u00a77Chunks : \u00a7e" + ClanClaimSession.getUsedClaims()
                        + "\u00a77/\u00a7e" + ClanClaimSession.getMaxClaims(),
                textX, textY + LINE_HEIGHT,
                0xFFFFFFFF,
                true
        );

        // Ligne 3 : commande claim
        context.drawText(
                client.textRenderer,
                "\u00a7a/clan claim \u00a77- Claimer ce chunk",
                textX, textY + LINE_HEIGHT * 2,
                0xFFFFFFFF,
                true
        );

        // Ligne 4 : commande unclaim
        context.drawText(
                client.textRenderer,
                "\u00a7c/clan unclaim \u00a77- Retirer le claim",
                textX, textY + LINE_HEIGHT * 3,
                0xFFFFFFFF,
                true
        );

        // Ligne 5 : commande exit
        context.drawText(
                client.textRenderer,
                "\u00a77/clan claim exit \u00a77- Terminer",
                textX, textY + LINE_HEIGHT * 4,
                0xFFFFFFFF,
                true
        );
    }
}
