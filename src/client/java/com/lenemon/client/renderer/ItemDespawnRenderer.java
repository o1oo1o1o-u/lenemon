package com.lenemon.client.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/**
 * The type Item despawn renderer.
 */
public class ItemDespawnRenderer {

    /**
     * Register.
     */
    public static void register() {
        HudRenderCallback.EVENT.register(ItemDespawnRenderer::renderHud);
    }

    private static void renderHud(net.minecraft.client.gui.DrawContext ctx,
                                  net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (client.gameRenderer == null) return;

        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) return;

        Vec3d camPos = camera.getPos();
        int screenW  = client.getWindow().getScaledWidth();
        int screenH  = client.getWindow().getScaledHeight();

        // Matrice de projection
        Matrix4f proj = new Matrix4f(client.gameRenderer.getBasicProjectionMatrix(
                client.options.getFov().getValue()));

        // Matrice de vue : rotation inverse de la caméra + translation
        Quaternionf camRot = new Quaternionf(camera.getRotation());
        Quaternionf invRot = camRot.conjugate(new Quaternionf());

        Matrix4f view = new Matrix4f()
                .rotate(invRot)
                .translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);

        Matrix4f projView = new Matrix4f(proj).mul(view);

        TextRenderer textRenderer = client.textRenderer;

        for (ItemEntity item : client.world.getEntitiesByClass(
                ItemEntity.class,
                client.player.getBoundingBox().expand(24),
                e -> true)) {

            int age            = item.getItemAge();
            int ticksRemaining = 1200 - age;
            if (ticksRemaining <= 0) continue;

            int color;
            if (ticksRemaining <= 200) {
                color = 0xFFFF4444;
            } else if (ticksRemaining <= 600) {
                color = 0xFFFF8800;
            } else {
                color = 0xFF44FF44;
            }

            float wx = (float) item.getX();
            float wy = (float) (item.getY() + item.getHeight() + 0.5f);
            float wz = (float) item.getZ();

            Vector4f pos = new Vector4f(wx, wy, wz, 1.0f);
            projView.transform(pos);

            if (pos.w <= 0) continue; // derrière la caméra

            float ndcX = pos.x / pos.w;
            float ndcY = pos.y / pos.w;

            if (ndcX < -1.1f || ndcX > 1.1f || ndcY < -1.1f || ndcY > 1.1f) continue;

            int sx = (int) ((ndcX + 1f) / 2f * screenW);
            int sy = (int) ((1f - ndcY) / 2f * screenH);

            String label = "§l!";
            int tw = textRenderer.getWidth(label);
            ctx.drawText(textRenderer, label, sx - tw / 2, sy, color, true);
        }
    }
}