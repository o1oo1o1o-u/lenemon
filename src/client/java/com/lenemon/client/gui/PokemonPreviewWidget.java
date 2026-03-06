package com.lenemon.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Petit widget réutilisable: preview modèle + texte (nom, shiny, palette).
 * Tu lui donnes juste une entité rendable + les infos à afficher.
 */
public final class PokemonPreviewWidget {

    /**
     * The type Info.
     */
    public record Info(Text displayName, boolean shiny, String paletteKey) {}

    private PokemonPreviewWidget() {}

    /**
     * Render.
     *
     * @param ctx           the ctx
     * @param left          the left
     * @param top           the top
     * @param width         the width
     * @param height        the height
     * @param entityCenterX the entity center x
     * @param entityBottomY the entity bottom y
     * @param scale         the scale
     * @param mouseX        the mouse x
     * @param mouseY        the mouse y
     * @param entity        the entity
     * @param info          the info
     */
    public static void render(
            DrawContext ctx,
            int left,
            int top,
            int width,
            int height,
            int entityCenterX,
            int entityBottomY,
            int scale,
            float mouseX,
            float mouseY,
            LivingEntity entity,
            Info info
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // 1) rendu entité
        if (entity != null) {
            GuiEntityRenderer.drawEntity(ctx, left + entityCenterX, top + entityBottomY, scale, mouseX, mouseY, entity);
        }

        // 2) textes
        int textX = left + 8;
        int textY = top + height - 28;

        if (info != null) {
            Text name = info.displayName();
            ctx.drawText(tr, name, textX, textY, 0xFFFFFF, false);

            Text shinyText = info.shiny()
                    ? Text.literal("Shiny").formatted(Formatting.AQUA)
                    : Text.literal("Normal").formatted(Formatting.GRAY);

            String palette = (info.paletteKey() == null || info.paletteKey().isBlank())
                    ? ""
                    : " (" + info.paletteKey() + ")";

            ctx.drawText(tr, Text.literal("").append(shinyText).append(Text.literal(palette).formatted(Formatting.DARK_GRAY)),
                    textX, textY + 10, 0xFFFFFF, false);
        }
    }
}