package com.lenemon.client.casino.render;

import com.lenemon.casino.screen.CasinoScreenHandler;
import com.lenemon.casino.util.CasinoTextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * The type Casino renderer.
 */
public final class CasinoRenderer {

    private CasinoRenderer() {}

    /**
     * Draw background.
     *
     * @param ctx the ctx
     * @param x   the x
     * @param y   the y
     */
    public static void drawBackground(DrawContext ctx, int x, int y) {
        ctx.drawTexture(
                CasinoTextures.BACKGROUND,
                x, y,
                31, 0,
                CasinoTextures.GUI_WIDTH, CasinoTextures.GUI_HEIGHT,
                CasinoTextures.TEX_SIZE,  CasinoTextures.TEX_SIZE
        );
    }

    /**
     * Draw slot backgrounds.
     *
     * @param ctx the ctx
     * @param x   the x
     * @param y   the y
     */
    public static void drawSlotBackgrounds(DrawContext ctx, int x, int y) {
        ctx.drawTexture(
                CasinoTextures.SLOT_LEFT,
                x, y,
                CasinoTextures.GUI_U_OFFSET, 0,              // <- IMPORTANT
                CasinoTextures.GUI_WIDTH, CasinoTextures.GUI_HEIGHT,
                CasinoTextures.TEX_SIZE, CasinoTextures.TEX_SIZE
        );

        ctx.drawTexture(
                CasinoTextures.SLOT_RIGHT,
                x, y,
                CasinoTextures.GUI_U_OFFSET, 0,              // <- IMPORTANT
                CasinoTextures.GUI_WIDTH, CasinoTextures.GUI_HEIGHT,
                CasinoTextures.TEX_SIZE, CasinoTextures.TEX_SIZE
        );
    }

    /**
     * Draw slot icons.
     *
     * @param ctx       the ctx
     * @param guiX      the gui x
     * @param guiY      the gui y
     * @param slotTx    the slot tx
     * @param slotTy    the slot ty
     * @param slotTw    the slot tw
     * @param slotTh    the slot th
     * @param reel      the reel
     * @param glowState the glow state
     * @param scale     the scale
     */
    public static void drawSlotIcons(
            DrawContext ctx,
            int guiX, int guiY,
            int slotTx, int slotTy, int slotTw, int slotTh,
            SlotReel reel,
            boolean glowState,
            double scale
    ) {
        int screenX = guiX + slotTx;
        int screenY = guiY + slotTy;

        ctx.enableScissor(screenX, screenY, screenX + slotTw, screenY + slotTh);

        int iconDrawSize = SlotReel.getIconSize();
        int iconX = screenX + (slotTw - iconDrawSize) / 2;

        for (SlotReel.DrawEntry entry : reel.getDrawEntries(slotTh)) {
            int iconY = screenY + (int) entry.yOffset();
            int u = CasinoTextures.iconU(entry.iconIndex());
            int v = CasinoTextures.iconV(glowState);

            ctx.drawTexture(
                    CasinoTextures.ICONS,
                    iconX, iconY,
                    u, v,
                    iconDrawSize, iconDrawSize,
                    128, 64
            );
        }

        ctx.disableScissor();
    }

    /**
     * Draw spin button.
     *
     * @param ctx     the ctx
     * @param guiX    the gui x
     * @param guiY    the gui y
     * @param pressed the pressed
     */
    public static void drawSpinButton(DrawContext ctx, int guiX, int guiY, boolean pressed) {

        int btnX = guiX + CasinoTextures.BTN_CX - CasinoTextures.BTN_W / 2;
        int btnY = guiY + CasinoTextures.BTN_CY - CasinoTextures.BTN_H / 2;

        ctx.drawTexture(
                pressed ? CasinoTextures.BTN_CLICKED : CasinoTextures.BTN_NORMAL,
                btnX, btnY,
                CasinoTextures.BTN_U, CasinoTextures.BTN_V,     // <- UV texture
                CasinoTextures.BTN_W, CasinoTextures.BTN_H,
                CasinoTextures.TEX_SIZE, CasinoTextures.TEX_SIZE
        );
    }

    /**
     * Draw bet box.
     *
     * @param ctx       the ctx
     * @param guiX      the gui x
     * @param guiY      the gui y
     * @param betAmount the bet amount
     */
    public static void drawBetBox(DrawContext ctx, int guiX, int guiY, int betAmount) {

        int boxW = 80;
        int boxH = 20;

        int boxX = guiX + CasinoTextures.GUI_WIDTH / 2 - boxW / 2;
        int boxY = guiY + CasinoTextures.BTN_CY + CasinoTextures.BTN_H / 2 + 6; // 🔥 sous le bouton

        ctx.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xCC0a1a2e);
        ctx.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xFF00e5ff);
        ctx.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0xFF00e5ff);
        ctx.fill(boxX, boxY, boxX + 1, boxY + boxH, 0xFF00e5ff);
        ctx.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, 0xFF00e5ff);

        var tr = MinecraftClient.getInstance().textRenderer;

        ctx.drawText(tr, Text.literal("BET"), boxX + 6, boxY + 6, 0xFF00e5ff, false);

        String val = String.valueOf(betAmount);
        int valX = boxX + boxW - 6 - tr.getWidth(val);
        ctx.drawText(tr, Text.literal(val), valX, boxY + 6, 0xFFFFD700, false);
    }

    /**
     * Draw win overlay.
     *
     * @param ctx   the ctx
     * @param guiX  the gui x
     * @param guiY  the gui y
     * @param alpha the alpha
     */
    public static void drawWinOverlay(DrawContext ctx, int guiX, int guiY, float alpha) {
        int a = (int)(alpha * 255) & 0xFF;
        int color = (a << 24) | 0x0080FF;

        ctx.fill(
                guiX + CasinoTextures.SLOT_L_TX, guiY + CasinoTextures.SLOT_L_TY,
                guiX + CasinoTextures.SLOT_L_TX + CasinoTextures.SLOT_L_TW,
                guiY + CasinoTextures.SLOT_L_TY + CasinoTextures.SLOT_L_TH,
                color
        );
        ctx.fill(
                guiX + CasinoTextures.SLOT_R_TX, guiY + CasinoTextures.SLOT_R_TY,
                guiX + CasinoTextures.SLOT_R_TX + CasinoTextures.SLOT_R_TW,
                guiY + CasinoTextures.SLOT_R_TY + CasinoTextures.SLOT_R_TH,
                color
        );
    }

}