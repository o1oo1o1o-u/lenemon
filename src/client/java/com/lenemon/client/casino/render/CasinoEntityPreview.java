package com.lenemon.client.casino.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;


/**
 * The type Casino entity preview.
 */
public final class CasinoEntityPreview {

    private CasinoEntityPreview() {}

    /**
     * The constant BOX_X1.
     */
// Ta zone "milieu" relative au guiX guiY
    public static final int BOX_X1 = 96;
    /**
     * The constant BOX_Y1.
     */
    public static final int BOX_Y1 = 46;
    /**
     * The constant BOX_X2.
     */
    public static final int BOX_X2 = 160;
    /**
     * The constant BOX_Y2.
     */
    public static final int BOX_Y2 = 145;

    /**
     * Affiche une LivingEntity dans la zone centrale du GUI.
     * guiX guiY = top-left de ta GUI (comme d'habitude dans Screen.drawBackground)
     *
     * @param ctx    the ctx
     * @param guiX   the gui x
     * @param guiY   the gui y
     * @param entity the entity
     * @param mouseX the mouse x
     * @param mouseY the mouse y
     */
    public static void drawLivingInCenterBox(DrawContext ctx, int guiX, int guiY, LivingEntity entity, float mouseX, float mouseY) {
        if (entity == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        int x1 = guiX + BOX_X1;
        int y1 = guiY + BOX_Y1;
        int x2 = guiX + BOX_X2;
        int y2 = guiY + BOX_Y2;

        int boxW = x2 - x1;
        int boxH = y2 - y1;

        // Centre de la box (x), bas de la box (y)
        int centerX = x1 + boxW / 2;
        int bottomY = y2;

        // Tu ajusteras ensuite
        int scale = Math.min(boxW, boxH); // point de départ
        scale = Math.max(20, scale);

        // Clip dans la zone
        ctx.enableScissor(x1, y1, x2, y2);

        drawEntity(ctx, centerX, bottomY, scale, mouseX, mouseY, entity);

        ctx.disableScissor();
    }

    /**
     * Rendu "inventaire" d'une LivingEntity.
     *
     * @param ctx    the ctx
     * @param x      the x
     * @param y      the y
     * @param scale  the scale
     * @param mouseX the mouse x
     * @param mouseY the mouse y
     * @param entity the entity
     */
    public static void drawEntity(DrawContext ctx, int x, int y, int scale, float mouseX, float mouseY, LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        // Rotation légère avec la souris (optionnelle)
        float yaw = (float) Math.atan((x - mouseX) / 40.0f);
        float pitch = (float) Math.atan((y - mouseY) / 40.0f);

        var matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(x, y, 50.0);
        matrices.scale(scale, scale, -scale);

        Quaternionf rotZ = RotationAxis.POSITIVE_Z.rotationDegrees(180.0f);
        Quaternionf rotX = RotationAxis.POSITIVE_X.rotationDegrees(pitch * 20.0f);
        rotZ.mul(rotX);
        matrices.multiply(rotZ);

        // Sauvegarde orientation entity
        float prevBodyYaw = entity.bodyYaw;
        float prevYaw = entity.getYaw();
        float prevPitch = entity.getPitch();
        float prevHeadYaw = entity.headYaw;
        float prevPrevHeadYaw = entity.prevHeadYaw;

        entity.bodyYaw = 180.0f + yaw * 40.0f;
        entity.setYaw(180.0f + yaw * 40.0f);
        entity.setPitch(-pitch * 20.0f);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();

        // Lumière GUI (1.21.1: Vector3f)
        DiffuseLighting.enableGuiDepthLighting();
        Vector3f light0 = new Vector3f(0.2f, 1.0f, -0.7f).normalize();
        Vector3f light1 = new Vector3f(-0.2f, 1.0f, 0.7f).normalize();
        RenderSystem.setShaderLights(light0, light1);

        var consumers = client.getBufferBuilders().getEntityVertexConsumers();
        dispatcher.setRenderShadows(false);

        dispatcher.render(
                entity,
                0.0, 0.0, 0.0,
                0.0f,
                1.0f,
                matrices,
                consumers,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
        );

        consumers.draw();
        dispatcher.setRenderShadows(true);

        // Restore entity
        entity.bodyYaw = prevBodyYaw;
        entity.setYaw(prevYaw);
        entity.setPitch(prevPitch);
        entity.headYaw = prevHeadYaw;
        entity.prevHeadYaw = prevPrevHeadYaw;

        matrices.pop();
    }
}