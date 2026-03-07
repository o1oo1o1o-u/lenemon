package com.lenemon.client.menu.screen;

import com.lenemon.network.menu.MenuActionPayload;
import com.lenemon.network.menu.MenuOpenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Menu principal du mod LeNeMon.
 * Affiche trois cartes : acces au sous-menu TP, au sous-menu Chasseur et au Pokédex.
 */
public class MenuScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_WIDTH  = 295;
    private static final int GUI_HEIGHT = 180;

    // ── Dimensions des cartes ─────────────────────────────────────────────────
    private static final int CARD_WIDTH  = 82;
    private static final int CARD_HEIGHT = 100;
    private static final int CARD_Y_OFFSET = 34; // Y relatif au guiY
    private static final int CARD_MARGIN  = 11; // marge gauche/droite et entre les cartes

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xCC0A0A1A;
    private static final int COL_BORDER     = 0xFF2255AA;
    private static final int COL_SEPARATOR  = 0x55FFFFFF;
    private static final int COL_CARD_BG    = 0x55113355;
    private static final int COL_CARD_BDR   = 0xFF334466;
    private static final int COL_CARD_HOVER = 0xFF4477CC;
    private static final int COL_LABEL      = 0xAAAAAA;

    // ── Donnees ───────────────────────────────────────────────────────────────
    private final MenuOpenPayload data;

    // ── Position calculee dans init() ─────────────────────────────────────────
    private int guiX, guiY;

    // ── Coordonnees des cartes (calculees dans init() pour les hit-tests) ─────
    private int cardTpX1, cardTpX2, cardTpY1, cardTpY2;
    private int cardHunterX1, cardHunterX2, cardHunterY1, cardHunterY2;
    private int cardDexX1, cardDexX2, cardDexY1, cardDexY2;

    /**
     * Cree l'ecran a partir du payload recu du serveur.
     *
     * @param data payload contenant les donnees du chasseur
     */
    public MenuScreen(MenuOpenPayload data) {
        super(Text.literal("Menu Principal"));
        this.data = data;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Override vide : pas de blur ni de fond sombre vanilla
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        guiX = (this.width  - GUI_WIDTH)  / 2;
        guiY = (this.height - GUI_HEIGHT) / 2;

        // Carte TP
        cardTpX1 = guiX + CARD_MARGIN;
        cardTpX2 = cardTpX1 + CARD_WIDTH;
        cardTpY1 = guiY + CARD_Y_OFFSET;
        cardTpY2 = guiY + CARD_Y_OFFSET + CARD_HEIGHT;

        // Carte Chasseur
        cardHunterX1 = cardTpX1 + CARD_WIDTH + CARD_MARGIN;
        cardHunterX2 = cardHunterX1 + CARD_WIDTH;
        cardHunterY1 = guiY + CARD_Y_OFFSET;
        cardHunterY2 = guiY + CARD_Y_OFFSET + CARD_HEIGHT;

        // Carte Pokédex
        cardDexX1 = cardHunterX1 + CARD_WIDTH + CARD_MARGIN;
        cardDexX2 = cardDexX1 + CARD_WIDTH;
        cardDexY1 = guiY + CARD_Y_OFFSET;
        cardDexY2 = guiY + CARD_Y_OFFSET + CARD_HEIGHT;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // ── Fond global + bordure ─────────────────────────────────────────────
        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, COL_BORDER);

        // ── Titre ─────────────────────────────────────────────────────────────
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a7b\u00a7l\u00bb \u00a7fMenu Principal"),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);

        // ── Separateur titre ──────────────────────────────────────────────────
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_WIDTH - 10, guiY + 21, COL_SEPARATOR);

        // ── Carte TP ──────────────────────────────────────────────────────────
        boolean hoverTp = isHover(mouseX, mouseY, cardTpX1, cardTpY1, cardTpX2, cardTpY2);
        renderCard(ctx, cardTpX1, cardTpY1, CARD_WIDTH, CARD_HEIGHT, hoverTp);
        renderCardContentTp(ctx, cardTpX1, cardTpY1, CARD_WIDTH);

        // ── Carte Chasseur ────────────────────────────────────────────────────
        boolean hoverHunter = isHover(mouseX, mouseY, cardHunterX1, cardHunterY1, cardHunterX2, cardHunterY2);
        renderCard(ctx, cardHunterX1, cardHunterY1, CARD_WIDTH, CARD_HEIGHT, hoverHunter);
        renderCardContentHunter(ctx, cardHunterX1, cardHunterY1, CARD_WIDTH);

        // ── Carte Pokédex ─────────────────────────────────────────────────────
        boolean hoverDex = isHover(mouseX, mouseY, cardDexX1, cardDexY1, cardDexX2, cardDexY2);
        renderCard(ctx, cardDexX1, cardDexY1, CARD_WIDTH, CARD_HEIGHT, hoverDex);
        renderCardContentDex(ctx, cardDexX1, cardDexY1, CARD_WIDTH);

        // Toujours en dernier
        super.render(ctx, mouseX, mouseY, delta);
    }

    /**
     * Dessine le fond et la bordure d'une carte.
     * La bordure devient COL_CARD_HOVER quand la souris survole la carte.
     */
    private void renderCard(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        ctx.fill(x, y, x + w, y + h, COL_CARD_BG);
        ctx.drawBorder(x, y, w, h, hovered ? COL_CARD_HOVER : COL_CARD_BDR);
    }

    /**
     * Contenu de la carte Teleportation.
     * Icone : ender_pearl, centree en haut de la carte.
     */
    private void renderCardContentTp(DrawContext ctx, int cx, int cy, int cw) {
        // Icone centree horizontalement, 4px de marge en haut
        int iconX = cx + (cw - 16) / 2;
        int iconY = cy + 6;
        ctx.drawItem(new ItemStack(Items.ENDER_PEARL), iconX, iconY);

        // Titre gras sous l'icone
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a7b\u00a7lTeleportation"),
                cx + cw / 2, cy + 28, 0xFFFFFF);

        // Description — word-wrap, centré
        var descLines = textRenderer.wrapLines(Text.literal("\u00a77Accedez aux mondes"), cw - 8);
        for (int l = 0; l < descLines.size(); l++) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    descLines.get(l), cx + cw / 2, cy + 42 + l * 10, COL_LABEL);
        }
    }

    /**
     * Contenu de la carte Pokédex.
     * Icone : cobblemon:pokedex_red, fallback COMPASS.
     */
    private void renderCardContentDex(DrawContext ctx, int cx, int cy, int cw) {
        var dexItem = Registries.ITEM.get(Identifier.of("cobblemon", "pokedex_red"));
        var iconItem = (dexItem != null && dexItem != Items.AIR) ? dexItem : Items.BOOK;

        int iconX = cx + (cw - 16) / 2;
        int iconY = cy + 6;
        ctx.drawItem(new ItemStack(iconItem), iconX, iconY);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a7d\u00a7l\u2606 Pok\u00e9dex"),
                cx + cw / 2, cy + 28, 0xFFFFFF);

        var descLines = textRenderer.wrapLines(Text.literal("\u00a77R\u00e9compenses de capture"), cw - 4);
        for (int l = 0; l < descLines.size(); l++) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    descLines.get(l), cx + cw / 2, cy + 42 + l * 10, COL_LABEL);
        }
    }

    /**
     * Contenu de la carte Chasseur.
     * Icone : ancient_origin_ball (Cobblemon), ou ender_eye en fallback.
     * Affiche le niveau et la barre de progression depuis le payload.
     */
    private void renderCardContentHunter(DrawContext ctx, int cx, int cy, int cw) {
        // Icone : cobblemon:ancient_origin_ball, fallback sur ENDER_EYE si absent
        var ballItem = Registries.ITEM.get(Identifier.of("cobblemon", "ancient_origin_ball"));
        var iconItem = (ballItem != null && ballItem != Items.AIR) ? ballItem : Items.ENDER_EYE;

        int iconX = cx + (cw - 16) / 2;
        int iconY = cy + 6;
        ctx.drawItem(new ItemStack(iconItem), iconX, iconY);

        // Titre gras
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a76\u00a7l\u2694 Chasseur"),
                cx + cw / 2, cy + 28, 0xFFFFFF);

        // Niveau
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a77Niveau : \u00a7e" + data.hunterLevel()),
                cx + cw / 2, cy + 42, COL_LABEL);

        // Barre de progression custom
        int barMargin = 8;
        int barX1 = cx + barMargin;
        int barX2 = cx + cw - barMargin;
        int barW  = barX2 - barX1;
        int barY  = cy + 56;
        int barH  = 6;
        float progress = Math.max(0.0f, Math.min(1.0f, data.hunterProgress()));
        int fillW = Math.round(barW * progress);
        ctx.fill(barX1, barY, barX1 + barW, barY + barH, 0xFF1A1A2E);
        if (fillW > 0) {
            ctx.fill(barX1, barY, barX1 + fillW, barY + barH, 0xFF44AAFF);
        }

        // Pourcentage compact sous la barre
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a7e" + data.progressPercent()),
                cx + cw / 2, cy + 68, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Clic carte TP
            if (isHover(mouseX, mouseY, cardTpX1, cardTpY1, cardTpX2, cardTpY2)) {
                ClientPlayNetworking.send(new MenuActionPayload("open_tp"));
                return true;
            }
            // Clic carte Chasseur
            if (isHover(mouseX, mouseY, cardHunterX1, cardHunterY1, cardHunterX2, cardHunterY2)) {
                ClientPlayNetworking.send(new MenuActionPayload("open_hunter"));
                return true;
            }
            // Clic carte Pokédex
            if (isHover(mouseX, mouseY, cardDexX1, cardDexY1, cardDexX2, cardDexY2)) {
                ClientPlayNetworking.send(new MenuActionPayload("open_pokedex"));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Teste si (mx, my) est dans le rectangle [x1,y1]-[x2,y2]. */
    private static boolean isHover(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }
}
