package com.lenemon.client.shop.screen;

import com.lenemon.network.shop.ShopActionPayload;
import com.lenemon.network.shop.ShopCategoryDto;
import com.lenemon.network.shop.ShopOpenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Ecran client-side du shop principal — affiche la grille des catégories.
 *
 * Reçoit un {@link ShopOpenPayload} depuis le serveur.
 * Clic sur une carte → envoie {@link ShopActionPayload}("open_category:N") au serveur.
 */
public class ShopScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_WIDTH   = 300;
    private static final int COLS        = 3;
    private static final int CARD_W      = 84;
    private static final int CARD_H      = 70;
    private static final int GAP         = 8;
    private static final int MARGIN      = 12;
    private static final int HEADER_H    = 26; // titre + séparateur
    private static final int FOOTER_H    = 34; // bouton fermer + marge bas

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xCC0A0A1A;
    private static final int COL_BORDER     = 0xFF2255AA;
    private static final int COL_SEPARATOR  = 0x55FFFFFF;
    private static final int COL_CARD_BG    = 0x55113355;
    private static final int COL_CARD_BDR   = 0xFF334466;
    private static final int COL_CARD_HOVER = 0xFF4477CC;
    private static final int COL_LABEL      = 0xAAAAAA;
    private static final int COL_CLOSE_BG   = 0x55330000;
    private static final int COL_CLOSE_BDR  = 0xFF443333;
    private static final int COL_CLOSE_HOV  = 0xFFAA2222;

    // ── Bouton Fermer ─────────────────────────────────────────────────────────
    private static final int CLOSE_W = 80;
    private static final int CLOSE_H = 18;

    // ── Données ───────────────────────────────────────────────────────────────
    private final ShopOpenPayload data;

    // ── Layout calculé dans init() ────────────────────────────────────────────
    private int guiX, guiY, guiH;
    private int closeBtnX, closeBtnY;
    private final List<CategoryCardLayout> cards = new ArrayList<>();

    public ShopScreen(ShopOpenPayload data) {
        super(Text.literal("Boutique du Serveur"));
        this.data = data;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Pas de fond sombre vanilla
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        cards.clear();

        List<ShopCategoryDto> categories = data.categories();
        int count = categories.size();

        // Nombre de rangées
        int rows = count == 0 ? 1 : (count + COLS - 1) / COLS;

        // Hauteur totale du GUI
        guiH = HEADER_H + MARGIN + rows * CARD_H + (rows - 1) * GAP + MARGIN + FOOTER_H;

        guiX = (this.width  - GUI_WIDTH) / 2;
        guiY = (this.height - guiH)      / 2;

        // Pré-calculer les coordonnées de chaque carte
        int contentStartY = guiY + HEADER_H + MARGIN;
        for (int i = 0; i < count; i++) {
            int col = i % COLS;
            int row = i / COLS;

            // Centrage horizontal de la rangée
            int rowCount = Math.min(COLS, count - row * COLS);
            int rowW = rowCount * CARD_W + (rowCount - 1) * GAP;
            int rowStartX = guiX + (GUI_WIDTH - rowW) / 2;

            int cx = rowStartX + col * (CARD_W + GAP);
            int cy = contentStartY + row * (CARD_H + GAP);

            cards.add(new CategoryCardLayout(cx, cy, CARD_W, CARD_H, i, categories.get(i)));
        }

        // Bouton fermer
        closeBtnX = guiX + (GUI_WIDTH - CLOSE_W) / 2;
        closeBtnY = guiY + guiH - FOOTER_H + (FOOTER_H - CLOSE_H) / 2;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // ── Fond + bordure ────────────────────────────────────────────────────
        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + guiH, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, guiH, COL_BORDER);

        // ── Titre ─────────────────────────────────────────────────────────────
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§l>> §fBoutique du Serveur §6§l<<"),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);

        // ── Séparateur ────────────────────────────────────────────────────────
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_WIDTH - 10, guiY + 21, COL_SEPARATOR);

        // ── Cartes catégories ─────────────────────────────────────────────────
        for (CategoryCardLayout card : cards) {
            boolean hover = card.isHovered(mouseX, mouseY);
            renderCategoryCard(ctx, card, hover);
        }

        // ── Bouton Fermer ─────────────────────────────────────────────────────
        renderCloseButton(ctx, mouseX, mouseY);

        // Widgets vanilla en dernier
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderCategoryCard(DrawContext ctx, CategoryCardLayout card, boolean hover) {
        int x = card.x();
        int y = card.y();
        int w = card.w();
        int h = card.h();
        ShopCategoryDto cat = card.data();

        // Fond + bordure
        ctx.fill(x, y, x + w, y + h, COL_CARD_BG);
        ctx.drawBorder(x, y, w, h, hover ? COL_CARD_HOVER : COL_CARD_BDR);

        // Icone de l'item (centree, 4px de marge en haut)
        net.minecraft.item.Item iconItem = Registries.ITEM.get(Identifier.of(cat.iconItemId()));
        if (iconItem == null || iconItem == Items.AIR) iconItem = Items.CHEST;
        int iconX = x + (w - 16) / 2;
        int iconY = y + 6;
        ctx.drawItem(new ItemStack(iconItem), iconX, iconY);

        // Nom de la catégorie (word-wrap, max 2 lignes)
        int textMaxW = w - 8;
        int textCentX = x + w / 2;
        var nameLines = textRenderer.wrapLines(Text.literal("§e§l" + cat.name()), textMaxW);
        int nameStartY = iconY + 18;
        for (int l = 0; l < Math.min(nameLines.size(), 2); l++) {
            ctx.drawCenteredTextWithShadow(textRenderer, nameLines.get(l),
                    textCentX, nameStartY + l * 10, 0xFFFFFF);
        }

        // Sous-texte : nombre d'items
        int subY = nameStartY + Math.min(nameLines.size(), 2) * 10 + 2;
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7" + cat.itemCount() + " item(s)"),
                textCentX, subY, COL_LABEL);
    }

    private void renderCloseButton(DrawContext ctx, int mouseX, int mouseY) {
        boolean hover = mouseX >= closeBtnX && mouseX <= closeBtnX + CLOSE_W
                && mouseY >= closeBtnY && mouseY <= closeBtnY + CLOSE_H;

        ctx.fill(closeBtnX, closeBtnY, closeBtnX + CLOSE_W, closeBtnY + CLOSE_H, COL_CLOSE_BG);
        ctx.drawBorder(closeBtnX, closeBtnY, CLOSE_W, CLOSE_H, hover ? COL_CLOSE_HOV : COL_CLOSE_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§c§lFermer"),
                closeBtnX + CLOSE_W / 2, closeBtnY + (CLOSE_H - 8) / 2, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Clic sur une carte catégorie
            for (CategoryCardLayout card : cards) {
                if (card.isHovered(mouseX, mouseY)) {
                    ClientPlayNetworking.send(new ShopActionPayload("open_category:" + card.index()));
                    return true;
                }
            }
            // Clic Fermer
            if (mouseX >= closeBtnX && mouseX <= closeBtnX + CLOSE_W
                    && mouseY >= closeBtnY && mouseY <= closeBtnY + CLOSE_H) {
                this.client.setScreen(null);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Layout interne ────────────────────────────────────────────────────────

    private record CategoryCardLayout(int x, int y, int w, int h, int index, ShopCategoryDto data) {
        boolean isHovered(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}
