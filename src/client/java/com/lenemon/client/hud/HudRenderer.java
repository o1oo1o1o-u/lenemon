package com.lenemon.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * The type Hud renderer.
 */
public class HudRenderer {

    private static final Identifier COIN_TEXTURE  = Identifier.of("lenemon", "textures/hud/hud_coin.png");
    private static final Identifier PANEL_TEXTURE = Identifier.of("lenemon", "textures/hud/hud_panel.png");

    /**
     * Register.
     */
    public static void register() {
        HudRenderCallback.EVENT.register(HudRenderer::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        int x           = HudConfig.x;
        int y           = HudConfig.y;
        int panelWidth  = HudConfig.panelWidth;
        int panelHeight = HudConfig.panelHeight;

        // ── Panel texture (arrondi) ──────────────────────────
        RenderSystem.enableBlend();
        context.drawTexture(
                PANEL_TEXTURE,
                HudConfig.panelTexX, HudConfig.panelTexY,
                HudConfig.panelTexW, HudConfig.panelTexH,
                0f, 0f, 256, 64, 256, 64
        );
        RenderSystem.disableBlend();

        // ── Bordure + tête ───────────────────────────────────
        context.fill(x - 1, y - 1, x + panelWidth + 1, y + panelHeight + 1, HudConfig.colorBorder);

        RenderSystem.enableBlend();
        context.drawTexture(
                client.player.getSkinTextures().texture(),
                x + 1, y + 1, 60, 60, 8f, 8f, 8, 8, 64, 64
        );

        // ── Icône pièce ──────────────────────────────────────
        context.drawTexture(
                COIN_TEXTURE,
                x + HudConfig.coinOffsetX, y + HudConfig.coinOffsetY,
                HudConfig.coinSize, HudConfig.coinSize,
                0f, 0f, 64, 64, 64, 64
        );
        RenderSystem.disableBlend();

        // ── Panel balance ────────────────────────────────────
        int balancePanelX = x + HudConfig.coinOffsetX;
        int balancePanelY = y + HudConfig.coinOffsetY + 1;
        context.fill(
                balancePanelX, balancePanelY,
                balancePanelX + HudConfig.balancePanelW,
                y + HudConfig.coinOffsetY + HudConfig.coinSize,
                HudConfig.colorBg
        );

        // ── Texte balance ────────────────────────────────────
        context.getMatrices().push();
        context.getMatrices().scale(HudConfig.textScale, HudConfig.textScale, 1.0f);
        context.drawText(
                client.textRenderer,
                formatBalance(HudBalanceCache.getBalance()),
                (int)((x + HudConfig.textOffsetX) / HudConfig.textScale),
                (int)((y + HudConfig.textOffsetY) / HudConfig.textScale),
                HudConfig.colorBalance,
                true
        );
        context.getMatrices().pop();

        // ── Panel Hunter (en dessous du panel balance) ───────
        int hunterPanelX = balancePanelX;
        int hunterPanelY = balancePanelY + HudConfig.coinSize + HudConfig.hunterPanelOffsetY;

        context.fill(
                hunterPanelX, hunterPanelY,
                hunterPanelX + HudConfig.hunterPanelW,
                hunterPanelY + HudConfig.hunterPanelH,
                HudConfig.colorBg
        );

        // ── Texte niveau ─────────────────────────────────────
        context.getMatrices().push();
        context.getMatrices().scale(HudConfig.levelTextScale, HudConfig.levelTextScale, 1.0f);
        context.drawText(
                client.textRenderer,
                "Niv. " + HudHunterCache.getLevel(),
                (int)((hunterPanelX + HudConfig.levelTextOffsetX) / HudConfig.levelTextScale),
                (int)((hunterPanelY + HudConfig.levelTextOffsetY) / HudConfig.levelTextScale),
                HudConfig.colorLevelText,
                true
        );
        context.getMatrices().pop();

        // ── Barre de progression XP ──────────────────────────
        int barX = hunterPanelX + HudConfig.barOffsetX;
        int barY = hunterPanelY + HudConfig.barOffsetY;
        int barW = HudConfig.barWidth;
        int barH = HudConfig.barHeight;

        // Fond barre
        context.fill(barX, barY, barX + barW, barY + barH, HudConfig.colorBarBg);

        // Remplissage barre selon progression
        int fillW = (int)(barW * HudHunterCache.getProgress());
        if (fillW > 0) {
            context.fill(barX, barY, barX + fillW, barY + barH, HudConfig.colorBarFill);
        }
    }

    private static String formatBalance(long balance) {
        return String.format("%,d", balance).replace(",", " ");
    }
}