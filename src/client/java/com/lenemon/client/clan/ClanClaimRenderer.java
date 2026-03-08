package com.lenemon.client.clan;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import org.joml.Matrix4f;

import java.util.Set;

/**
 * Rend les bordures des chunks claims sous forme de lignes colorees 3D,
 * avec des murs translucides sur les 4 faces verticales de chaque chunk.
 * Enregistre sur WorldRenderEvents.AFTER_TRANSLUCENT dans LenemonClient.
 *
 * Vert (#00FF88) pour les chunks du clan du joueur.
 * Rouge (#FF4444) pour les chunks d'autres clans a proximite.
 *
 * Les lignes utilisent RenderLayer.getLines() via VertexConsumerProvider.
 * Les murs translucides utilisent Tessellator + RenderSystem directement
 * (l'approche VertexConsumerProvider + getDebugQuads causait des triangles parasites).
 */
public class ClanClaimRenderer {

    /** Rayon de rendu en chunks (ne rendre que les chunks proches). */
    private static final int RENDER_RADIUS = 8;

    private static final float Y_OFFSET_MIN = -32f;
    private static final float Y_OFFSET_MAX =  32f;

    // Couleurs des lignes (aretes)
    private static final int COLOR_OWN_R   = 0x00;
    private static final int COLOR_OWN_G   = 0xFF;
    private static final int COLOR_OWN_B   = 0x88;
    private static final int COLOR_OTHER_R = 0xFF;
    private static final int COLOR_OTHER_G = 0x44;
    private static final int COLOR_OTHER_B = 0x44;
    private static final int ALPHA_LINES   = 0xAA;

    // Alpha des murs translucides
    private static final int ALPHA_WALL_OWN   = 0x33; // ~20% opacity
    private static final int ALPHA_WALL_OTHER  = 0x44; // ~27% opacity

    private ClanClaimRenderer() {}

    /**
     * Enregistre le renderer sur l'evenement WorldRenderEvents.AFTER_TRANSLUCENT.
     */
    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ClanClaimRenderer::render);
    }

    /**
     * Point d'entree de rendu. Appele par WorldRenderEvents.AFTER_TRANSLUCENT.
     */
    public static void render(WorldRenderContext ctx) {
        if (!ClanClaimSession.isActive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;

        int playerCX = player.getChunkPos().x;
        int playerCZ = player.getChunkPos().z;
        float playerY = (float) player.getY();

        Set<Long> ownChunks   = ClanClaimSession.getOwnChunks();
        Set<Long> otherChunks = ClanClaimSession.getOtherChunks();

        MatrixStack matrices = ctx.matrixStack();
        if (matrices == null) return;

        // Offset camera
        double camX = ctx.camera().getPos().x;
        double camY = ctx.camera().getPos().y;
        double camZ = ctx.camera().getPos().z;

        // --- Lignes (RenderLayer.getLines() via VertexConsumerProvider) ---
        VertexConsumerProvider.Immediate vcProvider = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = vcProvider.getBuffer(RenderLayer.getLines());

        matrices.push();

        for (long key : ownChunks) {
            int cx = ChunkPos.getPackedX(key);
            int cz = ChunkPos.getPackedZ(key);
            if (Math.abs(cx - playerCX) > RENDER_RADIUS || Math.abs(cz - playerCZ) > RENDER_RADIUS) continue;
            drawChunkBorder(lines, matrices, cx, cz, playerY, camX, camY, camZ,
                    COLOR_OWN_R, COLOR_OWN_G, COLOR_OWN_B, ALPHA_LINES);
        }

        for (long key : otherChunks) {
            int cx = ChunkPos.getPackedX(key);
            int cz = ChunkPos.getPackedZ(key);
            if (Math.abs(cx - playerCX) > RENDER_RADIUS || Math.abs(cz - playerCZ) > RENDER_RADIUS) continue;
            drawChunkBorder(lines, matrices, cx, cz, playerY, camX, camY, camZ,
                    COLOR_OTHER_R, COLOR_OTHER_G, COLOR_OTHER_B, ALPHA_LINES);
        }

        matrices.pop();

        vcProvider.draw(RenderLayer.getLines());

        // --- Murs translucides (Tessellator + RenderSystem) ---
        // Cette approche contourne le VertexConsumerProvider qui causait des connexions
        // parasites entre quads de chunks differents (triangles s'etirant vers le ciel).
        renderTranslucentWalls(matrices, ownChunks, otherChunks,
                playerCX, playerCZ, playerY, camX, camY, camZ);
    }

    /**
     * Rend tous les murs translucides via Tessellator + RenderSystem.
     * Appele apres le flush des lignes.
     */
    private static void renderTranslucentWalls(MatrixStack matrices,
            Set<Long> ownChunks, Set<Long> otherChunks,
            int playerCX, int playerCZ, float playerY,
            double camX, double camY, double camZ) {

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest(); // garder l'occlusion par le terrain
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);  // ne pas ecrire dans le depth buffer (transparence correcte)
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int totalFaces = 0;

        // Chunks du clan (vert)
        for (long key : ownChunks) {
            int cx = ChunkPos.getPackedX(key);
            int cz = ChunkPos.getPackedZ(key);
            if (Math.abs(cx - playerCX) > RENDER_RADIUS || Math.abs(cz - playerCZ) > RENDER_RADIUS) continue;
            totalFaces += drawFaces(buf, matrix, cx, cz, playerY, camX, camY, camZ,
                    COLOR_OWN_R, COLOR_OWN_G, COLOR_OWN_B, ALPHA_WALL_OWN, ownChunks);
        }

        // Chunks autres clans (rouge)
        for (long key : otherChunks) {
            int cx = ChunkPos.getPackedX(key);
            int cz = ChunkPos.getPackedZ(key);
            if (Math.abs(cx - playerCX) > RENDER_RADIUS || Math.abs(cz - playerCZ) > RENDER_RADIUS) continue;
            totalFaces += drawFaces(buf, matrix, cx, cz, playerY, camX, camY, camZ,
                    COLOR_OTHER_R, COLOR_OTHER_G, COLOR_OTHER_B, ALPHA_WALL_OTHER, otherChunks);
        }

        // Ne pas appeler buf.end() si aucun vertex n'a ete emis — causerait IllegalStateException
        if (totalFaces > 0) {
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    /**
     * Emet les quads de bordure d'un chunk vers le BufferBuilder.
     *
     * Une face n'est rendue QUE si le chunk adjacent de ce cote n'appartient PAS au meme set.
     * Cela evite la superposition de quads sur les faces internes entre chunks adjacents du meme clan.
     *
     * Ordre des sommets par face : BL, BR, TR, TL (CCW vu de l'exterieur).
     * Avec DrawMode.QUADS chaque groupe de 4 vertices est independant — aucun triangle parasite
     * ne relie les faces entre elles (contrairement a TRIANGLE_STRIP).
     */
    /** Retourne le nombre de faces emises (0-4). */
    private static int drawFaces(BufferBuilder buf, Matrix4f matrix,
            int cx, int cz, float playerY,
            double camX, double camY, double camZ,
            int r, int g, int b, int a, Set<Long> sameSet) {

        float x0  = cx * 16       - (float) camX;
        float x1  = cx * 16 + 16  - (float) camX;
        float z0  = cz * 16       - (float) camZ;
        float z1  = cz * 16 + 16  - (float) camZ;
        float yMin = playerY + Y_OFFSET_MIN - (float) camY;
        float yMax = playerY + Y_OFFSET_MAX - (float) camY;

        int faces = 0;

        // Face Nord (z = z0) — ne rendre que si (cx, cz-1) absent du set
        if (!sameSet.contains(ChunkPos.toLong(cx, cz - 1))) {
            buf.vertex(matrix, x0, yMin, z0).color(r, g, b, a);
            buf.vertex(matrix, x1, yMin, z0).color(r, g, b, a);
            buf.vertex(matrix, x1, yMax, z0).color(r, g, b, a);
            buf.vertex(matrix, x0, yMax, z0).color(r, g, b, a);
            faces++;
        }

        // Face Sud (z = z1) — ne rendre que si (cx, cz+1) absent du set
        if (!sameSet.contains(ChunkPos.toLong(cx, cz + 1))) {
            buf.vertex(matrix, x1, yMin, z1).color(r, g, b, a);
            buf.vertex(matrix, x0, yMin, z1).color(r, g, b, a);
            buf.vertex(matrix, x0, yMax, z1).color(r, g, b, a);
            buf.vertex(matrix, x1, yMax, z1).color(r, g, b, a);
            faces++;
        }

        // Face Ouest (x = x0) — ne rendre que si (cx-1, cz) absent du set
        if (!sameSet.contains(ChunkPos.toLong(cx - 1, cz))) {
            buf.vertex(matrix, x0, yMin, z1).color(r, g, b, a);
            buf.vertex(matrix, x0, yMin, z0).color(r, g, b, a);
            buf.vertex(matrix, x0, yMax, z0).color(r, g, b, a);
            buf.vertex(matrix, x0, yMax, z1).color(r, g, b, a);
            faces++;
        }

        // Face Est (x = x1) — ne rendre que si (cx+1, cz) absent du set
        if (!sameSet.contains(ChunkPos.toLong(cx + 1, cz))) {
            buf.vertex(matrix, x1, yMin, z0).color(r, g, b, a);
            buf.vertex(matrix, x1, yMin, z1).color(r, g, b, a);
            buf.vertex(matrix, x1, yMax, z1).color(r, g, b, a);
            buf.vertex(matrix, x1, yMax, z0).color(r, g, b, a);
            faces++;
        }

        return faces;
    }

    /**
     * Dessine les 4 aretes verticales et les contours haut/bas d'un chunk.
     */
    private static void drawChunkBorder(VertexConsumer lines, MatrixStack matrices,
                                         int cx, int cz,
                                         float playerY,
                                         double camX, double camY, double camZ,
                                         int r, int g, int b, int a) {
        float worldX = cx * 16;
        float worldZ = cz * 16;
        float yMin = playerY + Y_OFFSET_MIN;
        float yMax = playerY + Y_OFFSET_MAX;

        // Les 4 coins du chunk
        float[][] corners = {
                {worldX,      worldZ},
                {worldX + 16, worldZ},
                {worldX + 16, worldZ + 16},
                {worldX,      worldZ + 16}
        };

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // 4 aretes verticales (coins)
        for (float[] corner : corners) {
            float vx = corner[0] - (float) camX;
            float vz = corner[1] - (float) camZ;
            float vy0 = yMin - (float) camY;
            float vy1 = yMax - (float) camY;

            line(lines, matrix, vx, vy0, vz, vx, vy1, vz, r, g, b, a);
        }

        // 4 aretes horizontales en bas
        for (int i = 0; i < 4; i++) {
            float[] c1 = corners[i];
            float[] c2 = corners[(i + 1) % 4];
            float vx1 = c1[0] - (float) camX;
            float vz1 = c1[1] - (float) camZ;
            float vx2 = c2[0] - (float) camX;
            float vz2 = c2[1] - (float) camZ;
            float vy = yMin - (float) camY;
            line(lines, matrix, vx1, vy, vz1, vx2, vy, vz2, r, g, b, a);
        }

        // 4 aretes horizontales en haut
        for (int i = 0; i < 4; i++) {
            float[] c1 = corners[i];
            float[] c2 = corners[(i + 1) % 4];
            float vx1 = c1[0] - (float) camX;
            float vz1 = c1[1] - (float) camZ;
            float vx2 = c2[0] - (float) camX;
            float vz2 = c2[1] - (float) camZ;
            float vy = yMax - (float) camY;
            line(lines, matrix, vx1, vy, vz1, vx2, vy, vz2, r, g, b, a);
        }
    }

    private static void line(VertexConsumer consumer, Matrix4f matrix,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              int r, int g, int b, int a) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0) return;
        nx /= len; ny /= len; nz /= len;

        consumer.vertex(matrix, x1, y1, z1)
                .color(r, g, b, a)
                .normal(nx, ny, nz);
        consumer.vertex(matrix, x2, y2, z2)
                .color(r, g, b, a)
                .normal(nx, ny, nz);
    }
}
