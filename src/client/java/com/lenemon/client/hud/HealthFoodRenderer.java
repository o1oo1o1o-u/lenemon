package com.lenemon.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.HungerManager;

/**
 * Affiche les barres de vie et de nourriture au-dessus de la hotbar custom,
 * symétriques par rapport au centre de l'écran.
 *
 * <p>Disposition :
 * <pre>
 *   [coeur] [===vie===]   [===nourriture===] [•]
 * </pre>
 * La barre de vie est à gauche du centre, la nourriture à droite.
 */
public class HealthFoodRenderer {

    private static final int BAR_WIDTH  = 80;
    private static final int BAR_HEIGHT = 6;
    /** Espacement horizontal entre le centre et chaque barre. */
    private static final int CENTER_GAP = 4;
    /** Décalage Y depuis le bord supérieur de la hotbar. */
    private static final int BAR_Y_OFFSET_FROM_HOTBAR = 14;


    public static void register() {
        HudRenderCallback.EVENT.register(HealthFoodRenderer::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.currentScreen != null) return;

        int screenWidth  = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int hotbarY = screenHeight - HudConfig.hotbarBottomOffset;
        int barY    = hotbarY - BAR_Y_OFFSET_FROM_HOTBAR;
        int centerX = screenWidth / 2;

        // ── VIE (côté gauche) ─────────────────────────────────────────────────
        float maxHealth = (float) client.player.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        float health    = client.player.getHealth();
        float absorption = client.player.getAbsorptionAmount();
        // Éviter la division par zéro si l'attribut n'est pas encore initialisé
        if (maxHealth <= 0f) maxHealth = 20f;

        int heartBarX = centerX - CENTER_GAP - BAR_WIDTH;

        // Icône cœur à gauche de la barre
        context.drawText(client.textRenderer, "\u2665",
                heartBarX - 10, barY, HudConfig.colorHeartFill, true);

        // Fond + bordure
        context.fill(heartBarX, barY, heartBarX + BAR_WIDTH, barY + BAR_HEIGHT,
                HudConfig.colorHeartBg);
        context.fill(heartBarX, barY,
                heartBarX + BAR_WIDTH, barY + 1, HudConfig.hotbarBorder);          // top
        context.fill(heartBarX, barY + BAR_HEIGHT - 1,
                heartBarX + BAR_WIDTH, barY + BAR_HEIGHT, HudConfig.hotbarBorder); // bottom

        // Remplissage vie (clampé à 100 %)
        int healthFillW = (int) ((BAR_WIDTH - 2) * Math.min(health / maxHealth, 1f));
        if (healthFillW > 0) {
            context.fill(heartBarX + 1, barY + 1,
                    heartBarX + 1 + healthFillW, barY + BAR_HEIGHT - 1,
                    HudConfig.colorHeartFill);
        }

        // Absorption par-dessus la vie, en or
        if (absorption > 0f) {
            int absFillW = (int) ((BAR_WIDTH - 2) * Math.min(absorption / maxHealth, 1f));
            // L'absorption commence là où la vie s'arrête (peut déborder de la barre)
            int absStart = heartBarX + 1 + healthFillW;
            int absEnd   = Math.min(absStart + absFillW, heartBarX + BAR_WIDTH - 1);
            if (absEnd > absStart) {
                context.fill(absStart, barY + 1, absEnd, barY + BAR_HEIGHT - 1, 0xFFFFD700);
            }
        }

        // Texte HP au-dessus de la barre
        String hpText = (int) health + "/" + (int) maxHealth;
        context.drawText(client.textRenderer, hpText,
                heartBarX, barY - 9, 0xFFEAEAEA, true);

        // ── NOURRITURE (côté droit) ───────────────────────────────────────────
        HungerManager hunger   = client.player.getHungerManager();
        int foodLevel          = hunger.getFoodLevel(); // 0–20

        int foodBarX = centerX + CENTER_GAP;

        // Icône nourriture à droite de la barre
        context.drawText(client.textRenderer, "\u25CF",
                foodBarX + BAR_WIDTH + 2, barY, HudConfig.colorFoodFill, true);

        // Fond + bordure
        context.fill(foodBarX, barY, foodBarX + BAR_WIDTH, barY + BAR_HEIGHT,
                HudConfig.colorFoodBg);
        context.fill(foodBarX, barY,
                foodBarX + BAR_WIDTH, barY + 1, HudConfig.hotbarBorder);           // top
        context.fill(foodBarX, barY + BAR_HEIGHT - 1,
                foodBarX + BAR_WIDTH, barY + BAR_HEIGHT, HudConfig.hotbarBorder);  // bottom

        // Remplissage nourriture
        int foodFillW = (int) ((BAR_WIDTH - 2) * (foodLevel / 20f));
        if (foodFillW > 0) {
            context.fill(foodBarX + 1, barY + 1,
                    foodBarX + 1 + foodFillW, barY + BAR_HEIGHT - 1,
                    HudConfig.colorFoodFill);
        }

        // Texte nourriture au-dessus de la barre
        String foodText = foodLevel + "/20";
        context.drawText(client.textRenderer, foodText,
                foodBarX, barY - 9, 0xFFEAEAEA, true);
    }
}
