package com.lenemon.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Rendu d'une LivingEntity dans un GUI, style "preview inventaire".
 * Utilisable pour PokemonEntity si c'est une LivingEntity côté Cobblemon.
 */
public final class GuiEntityRenderer {

    private GuiEntityRenderer() {}

    /**
     * Draw entity.
     *
     * @param drawContext contexte de rendu du GUI
     * @param x           centre X (pixels écran)
     * @param y           centre Y (pixels écran)
     * @param scale       taille (ex: 35-60 selon ton cadre)
     * @param mouseX      pour faire tourner avec la souris (optionnel)
     * @param mouseY      pour faire tourner avec la souris (optionnel)
     * @param entity      entité à rendre (doit exister dans un world client, même temporaire)
     */
    public static void drawEntity(DrawContext drawContext, int x, int y, int scale, float mouseX, float mouseY, LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        float yaw = (float) Math.atan(mouseX / 40.0f);
        float pitch = (float) Math.atan(mouseY / 40.0f);

        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(x, y, 50.0);
        matrices.scale(scale, scale, -scale);

        Quaternionf rotationZ = RotationAxis.POSITIVE_Z.rotationDegrees(180.0f);
        Quaternionf rotationX = RotationAxis.POSITIVE_X.rotationDegrees(pitch * 20.0f);
        rotationZ.mul(rotationX);
        matrices.multiply(rotationZ);

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

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        Quaternionf prevDispatcherRotation = dispatcher.getRotation();
        dispatcher.setRotation(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        // lumière "GUI"
        DiffuseLighting.enableGuiDepthLighting();
        // lumière GUI simple
        Vector3f light0 = new Vector3f(0.2f, 1.0f, -0.7f).normalize();
        Vector3f light1 = new Vector3f(-0.2f, 1.0f, 0.7f).normalize();

        RenderSystem.setShaderLights(light0, light1);

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
        dispatcher.setRotation(prevDispatcherRotation);

        // restore
        entity.bodyYaw = prevBodyYaw;
        entity.setYaw(prevYaw);
        entity.setPitch(prevPitch);
        entity.headYaw = prevHeadYaw;
        entity.prevHeadYaw = prevPrevHeadYaw;

        matrices.pop();
    }
}