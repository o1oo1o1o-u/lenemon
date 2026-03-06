package com.lenemon.client.hud;

import com.lenemon.mixin.client.InGameHudAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;

/**
 * Affiche la hotbar custom (9 slots + barre XP + niveau joueur)
 * centrée en bas de l'écran, en remplacement de la hotbar vanilla.
 *
 * <p>La hotbar vanilla doit être masquée séparément via un mixin
 * ou en désactivant le rendu HUD d'origine.
 */
public class HotbarRenderer {

    private static final int SLOT_SIZE    = 20;
    private static final int SLOT_GAP     = 2;
    private static final int SLOT_COUNT   = 9;
    /** Largeur totale des 9 slots + 8 gaps : 9*20 + 8*2 = 196 px */
    private static final int HOTBAR_WIDTH = SLOT_COUNT * SLOT_SIZE + (SLOT_COUNT - 1) * SLOT_GAP;
    private static final int HOTBAR_PADDING = 4;

    /**
     * Durée du fade-out vanilla pour le nom d'item tenu (en ticks).
     * InGameHud initialise heldItemTooltipFade à 40 lors d'un changement de slot.
     */
    private static final int HELD_ITEM_FADE_TICKS = 40;

    /**
     * Hauteur de l'élément flight bar + son gap, utilisée pour calculer
     * le décalage Y du tooltip quand la barre de vol est active.
     * Doit être cohérent avec FlightBarRenderer : BAR_HEIGHT(7) + FLIGHT_BAR_GAP(3) = 10.
     */
    private static final int FLIGHT_BAR_TOTAL_HEIGHT = 10;


    public static void register() {
        HudRenderCallback.EVENT.register(HotbarRenderer::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.currentScreen != null) return;

        // Spectateur : rien du tout
        if (client.player.isSpectator()) return;

        // Créatif : pas de XP ni niveau
        boolean showXp = !client.player.isCreative();

        int screenWidth  = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int hotbarX = screenWidth  / 2 - HOTBAR_WIDTH / 2;
        int hotbarY = screenHeight - HudConfig.hotbarBottomOffset;

        // ── Fond de la hotbar ────────────────────────────────────────────────
        int bgX = hotbarX - HOTBAR_PADDING;
        int bgY = hotbarY - HOTBAR_PADDING;
        int bgW = HOTBAR_WIDTH  + HOTBAR_PADDING * 2;
        int bgH = SLOT_SIZE     + HOTBAR_PADDING * 2;
        drawBorderedRect(context, bgX, bgY, bgW, bgH,
                HudConfig.hotbarBg, HudConfig.hotbarBorder);

        // ── Slots ────────────────────────────────────────────────────────────
        int selectedSlot = client.player.getInventory().selectedSlot;
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX    = hotbarX + i * (SLOT_SIZE + SLOT_GAP);
            int slotY    = hotbarY;
            boolean selected = (i == selectedSlot);

            if (selected) {
                // Slot actif : encadré légèrement plus grand + couleur bleue
                drawBorderedRect(context,
                        slotX - 1, slotY - 1, SLOT_SIZE + 2, SLOT_SIZE + 2,
                        HudConfig.slotSelectedBg, HudConfig.slotSelectedBorder);
            } else {
                drawBorderedRect(context, slotX, slotY, SLOT_SIZE, SLOT_SIZE,
                        HudConfig.slotBg, HudConfig.hotbarBorder);
            }

            // Item + count overlay (drawItemInSlot gère le count string en blanc)
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                // Décalage de 2 px pour centrer l'item (16px) dans le slot (20px)
                context.drawItem(stack, slotX + 2, slotY + 2);
                context.drawItemInSlot(client.textRenderer, stack, slotX + 2, slotY + 2);
            }
        }

        // ── Barre XP + encadré niveau (masqués en créatif) ───────────────────
        int xpBarY = hotbarY - 8;
        if (showXp) {
            int xpBarW  = HOTBAR_WIDTH;
            float xpPct = client.player.experienceProgress;

            drawBorderedRect(context, hotbarX, xpBarY, xpBarW, 5,
                    HudConfig.colorBarBg, HudConfig.hotbarBorder);
            int xpFillW = (int) ((xpBarW - 2) * xpPct);
            if (xpFillW > 0) {
                context.fill(hotbarX + 1, xpBarY + 1,
                        hotbarX + 1 + xpFillW, xpBarY + 4,
                        HudConfig.colorBarFill);
            }

            int level = client.player.experienceLevel;
            String lvlText = "Niv. " + level;
            int boxW = client.textRenderer.getWidth(lvlText) + 10;
            int boxH = 12;
            int boxX = screenWidth / 2 - boxW / 2;
            int boxY = xpBarY - boxH - 2;
            context.fill(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + boxH - 1, HudConfig.hotbarBg);
            context.fill(boxX,           boxY,           boxX + boxW, boxY + 1,         HudConfig.slotSelectedBorder);
            context.fill(boxX,           boxY + boxH - 1, boxX + boxW, boxY + boxH,     HudConfig.slotSelectedBorder);
            context.fill(boxX,           boxY,           boxX + 1,    boxY + boxH,      HudConfig.slotSelectedBorder);
            context.fill(boxX + boxW - 1, boxY,          boxX + boxW, boxY + boxH,      HudConfig.slotSelectedBorder);
            context.drawText(client.textRenderer, lvlText, boxX + 5, boxY + 2, HudConfig.colorXpText, true);
        }

        // ── Nom de l'item tenu (remplace le tooltip vanilla, avec fade-out) ──
        int fade = ((InGameHudAccessor) client.inGameHud).getHeldItemTooltipFade();
        if (fade > 0) {
            ItemStack heldStack = client.player.getMainHandStack();
            if (!heldStack.isEmpty()) {
                // Opacité : 0..1 sur 40 ticks, avec un palier plein sur les 10 premiers
                float alpha = Math.min(1f, fade / (float) HELD_ITEM_FADE_TICKS * 2f);

                // Position Y : au-dessus de toute la stack UI.
                // On monte encore d'une ligne au-dessus de la barre de vol si elle est active.
                int slotsY    = screenHeight - HudConfig.hotbarBottomOffset; // Y des slots
                int xpBarY2   = slotsY - 8;                                 // barre XP
                int levelBoxY2 = xpBarY2 - 14;                              // encadré niveau (h=12 + gap 2)
                int topOfStack = levelBoxY2;                                 // sommet de la hotbar sans vol
                if (HudFlightCache.isActive()) {
                    // flight bar : BAR_HEIGHT(7) + FLIGHT_BAR_GAP(3) = 10 px
                    topOfStack = levelBoxY2 - FLIGHT_BAR_TOTAL_HEIGHT;
                }
                int tooltipY = topOfStack - 12; // 12 px au-dessus du sommet (hauteur d'une ligne de texte + marge)

                String itemName = heldStack.getName().getString();
                int nameW = client.textRenderer.getWidth(itemName);
                int nameX = screenWidth / 2 - nameW / 2;

                // Couleurs avec alpha appliqué
                int bgAlpha   = (int) (0x88 * alpha) << 24;
                int textAlpha = (int) (0xFF * alpha) << 24;

                // Fond semi-transparent
                context.fill(nameX - 4, tooltipY - 2, nameX + nameW + 4, tooltipY + 10, bgAlpha);
                // Texte (ombre portée incluse via le flag shadow=true)
                context.drawText(client.textRenderer, itemName, nameX, tooltipY, textAlpha | 0xFFFFFF, true);
            }
        }
    }

    /**
     * Dessine un rectangle avec une bordure d'1 pixel et un fond intérieur.
     *
     * @param context     contexte de rendu courant
     * @param x           coin supérieur gauche
     * @param y           coin supérieur gauche
     * @param w           largeur totale (bords inclus)
     * @param h           hauteur totale (bords inclus)
     * @param fillColor   couleur ARGB du fond intérieur
     * @param borderColor couleur ARGB de la bordure
     */
    static void drawBorderedRect(DrawContext context,
                                 int x, int y, int w, int h,
                                 int fillColor, int borderColor) {
        // Bordure 1 px sur les 4 côtés
        context.fill(x,         y,         x + w,     y + 1,     borderColor); // top
        context.fill(x,         y + h - 1, x + w,     y + h,     borderColor); // bottom
        context.fill(x,         y,         x + 1,     y + h,     borderColor); // left
        context.fill(x + w - 1, y,         x + w,     y + h,     borderColor); // right
        // Fond intérieur
        context.fill(x + 1,     y + 1,     x + w - 1, y + h - 1, fillColor);
    }
}
