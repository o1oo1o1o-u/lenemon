package com.lenemon.client.menu.screen;

import com.lenemon.network.menu.MenuActionPayload;
import com.lenemon.network.menu.TpMenuOpenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Sous-menu de teleportation du mod LeNeMon.
 *
 * Rangee 1 : Overworld, Spawn, Ressource (toujours accessibles).
 * Rangee 2 : Nether et End (verrouillee si permission absente).
 * Bouton retour en bas.
 */
public class TpMenuScreen extends Screen {

    // ── Dimensions globales ───────────────────────────────────────────────────
    private static final int GUI_WIDTH  = 300;
    private static final int GUI_HEIGHT = 220;

    // ── Dimensions des cartes rangee 1 ────────────────────────────────────────
    private static final int ROW1_CARD_W  = 76;
    private static final int ROW1_CARD_H  = 80;
    private static final int ROW1_CARD_GAP = 8;
    private static final int ROW1_COUNT   = 3;
    private static final int ROW1_Y_OFFSET = 34; // relatif a guiY

    // ── Dimensions des cartes rangee 2 ────────────────────────────────────────
    private static final int ROW2_CARD_W  = 100;
    private static final int ROW2_CARD_H  = 50;
    private static final int ROW2_CARD_GAP = 16;
    private static final int ROW2_COUNT   = 2;
    private static final int ROW2_Y_OFFSET = 130; // relatif a guiY

    // ── Bouton retour ─────────────────────────────────────────────────────────
    private static final int BTN_W        = 80;
    private static final int BTN_H        = 20;
    private static final int BTN_Y_OFFSET = 190; // relatif a guiY

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xCC0A0A1A;
    private static final int COL_BORDER     = 0xFF2255AA;
    private static final int COL_SEPARATOR  = 0x55FFFFFF;
    private static final int COL_CARD_BG    = 0x55113355;
    private static final int COL_CARD_BDR   = 0xFF334466;
    private static final int COL_CARD_HOVER = 0xFF4477CC;
    private static final int COL_LABEL      = 0xAAAAAA;
    private static final int COL_LOCKED_BG  = 0x55330000;
    private static final int COL_LOCKED_BDR = 0xFF443333;

    // ── Donnees ───────────────────────────────────────────────────────────────
    private final boolean hasNether;
    private final boolean hasEnd;

    // ── Position calculee dans init() ─────────────────────────────────────────
    private int guiX, guiY;

    // ── Coordonnees des zones cliquables (calculees dans init()) ─────────────
    // Rangee 1 : [0]=overworld, [1]=spawn, [2]=resource
    private final int[] row1X = new int[ROW1_COUNT];
    private int row1Y;

    // Rangee 2 : [0]=nether, [1]=end
    private final int[] row2X = new int[ROW2_COUNT];
    private int row2Y;

    // Bouton retour
    private int btnX, btnY;

    /**
     * Cree l'ecran a partir du payload recu du serveur.
     *
     * @param data payload indiquant les permissions Nether/End
     */
    public TpMenuScreen(TpMenuOpenPayload data) {
        super(Text.literal("Teleportation"));
        this.hasNether = data.hasNether();
        this.hasEnd    = data.hasEnd();
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

        // ── Rangee 1 : 3 cartes centrees ──────────────────────────────────────
        // Largeur totale occupee : 3*76 + 2*8 = 244
        int row1TotalW = ROW1_COUNT * ROW1_CARD_W + (ROW1_COUNT - 1) * ROW1_CARD_GAP;
        int row1StartX = guiX + (GUI_WIDTH - row1TotalW) / 2;
        row1Y = guiY + ROW1_Y_OFFSET;
        for (int i = 0; i < ROW1_COUNT; i++) {
            row1X[i] = row1StartX + i * (ROW1_CARD_W + ROW1_CARD_GAP);
        }

        // ── Rangee 2 : 2 cartes centrees ──────────────────────────────────────
        // Largeur totale occupee : 2*100 + 1*16 = 216
        int row2TotalW = ROW2_COUNT * ROW2_CARD_W + (ROW2_COUNT - 1) * ROW2_CARD_GAP;
        int row2StartX = guiX + (GUI_WIDTH - row2TotalW) / 2;
        row2Y = guiY + ROW2_Y_OFFSET;
        for (int i = 0; i < ROW2_COUNT; i++) {
            row2X[i] = row2StartX + i * (ROW2_CARD_W + ROW2_CARD_GAP);
        }

        // ── Bouton retour ─────────────────────────────────────────────────────
        btnX = guiX + (GUI_WIDTH - BTN_W) / 2;
        btnY = guiY + BTN_Y_OFFSET;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // ── Fond global + bordure ─────────────────────────────────────────────
        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, COL_BORDER);

        // ── Titre ─────────────────────────────────────────────────────────────
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a7b\u00a7l\u00bb \u00a7fTeleportation"),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);

        // ── Separateur titre ──────────────────────────────────────────────────
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_WIDTH - 10, guiY + 21, COL_SEPARATOR);

        // ── Rangee 1 ──────────────────────────────────────────────────────────
        renderRow1Card(ctx, mouseX, mouseY, 0, Items.GRASS_BLOCK,
                "\u00a7a\u00a7lOverworld", "\u00a77Monde principal");

        renderRow1Card(ctx, mouseX, mouseY, 1, Items.BEACON,
                "\u00a7e\u00a7lSpawn", "\u00a77Point central");

        renderRow1Card(ctx, mouseX, mouseY, 2, Items.IRON_PICKAXE,
                "\u00a76\u00a7lRessource", "\u00a77Monde ressources");

        // ── Rangee 2 ──────────────────────────────────────────────────────────
        renderNetherCard(ctx, mouseX, mouseY);
        renderEndCard(ctx, mouseX, mouseY);

        // ── Bouton retour ─────────────────────────────────────────────────────
        renderBackButton(ctx, mouseX, mouseY);

        // Toujours en dernier
        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Renderers de cartes ───────────────────────────────────────────────────

    /**
     * Dessine une carte de la rangee 1 (toujours accessible).
     *
     * @param index 0=Overworld, 1=Spawn, 2=Ressource
     */
    private void renderRow1Card(DrawContext ctx, int mouseX, int mouseY,
                                int index, net.minecraft.item.Item icon,
                                String title, String desc) {
        int x = row1X[index];
        int y = row1Y;
        boolean hovered = isHover(mouseX, mouseY, x, y, x + ROW1_CARD_W, y + ROW1_CARD_H);

        ctx.fill(x, y, x + ROW1_CARD_W, y + ROW1_CARD_H, COL_CARD_BG);
        ctx.drawBorder(x, y, ROW1_CARD_W, ROW1_CARD_H, hovered ? COL_CARD_HOVER : COL_CARD_BDR);

        // Icone centree en haut
        int iconX = x + (ROW1_CARD_W - 16) / 2;
        ctx.drawItem(new ItemStack(icon), iconX, y + 6);

        // Titre — word-wrap, centré
        int centerX = x + ROW1_CARD_W / 2;
        int maxW = ROW1_CARD_W - 8;
        var titleLines = textRenderer.wrapLines(Text.literal(title), maxW);
        for (int l = 0; l < titleLines.size(); l++) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    titleLines.get(l), centerX, y + 28 + l * 10, 0xFFFFFF);
        }

        // Description — word-wrap, centré
        int descStartY = y + 28 + titleLines.size() * 10 + 2;
        var descLines = textRenderer.wrapLines(Text.literal(desc), maxW);
        for (int l = 0; l < descLines.size(); l++) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    descLines.get(l), centerX, descStartY + l * 10, COL_LABEL);
        }
    }

    /**
     * Dessine la carte Nether (verrouillee si !hasNether).
     */
    private void renderNetherCard(DrawContext ctx, int mouseX, int mouseY) {
        int x = row2X[0];
        int y = row2Y;

        if (hasNether) {
            boolean hovered = isHover(mouseX, mouseY, x, y, x + ROW2_CARD_W, y + ROW2_CARD_H);
            ctx.fill(x, y, x + ROW2_CARD_W, y + ROW2_CARD_H, COL_CARD_BG);
            ctx.drawBorder(x, y, ROW2_CARD_W, ROW2_CARD_H, hovered ? COL_CARD_HOVER : COL_CARD_BDR);
            ctx.drawItem(new ItemStack(Items.NETHERRACK), x + (ROW2_CARD_W - 16) / 2, y + 4);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a7c\u00a7lNether"),
                    x + ROW2_CARD_W / 2, y + 26, 0xFFFFFF);
        } else {
            ctx.fill(x, y, x + ROW2_CARD_W, y + ROW2_CARD_H, COL_LOCKED_BG);
            ctx.drawBorder(x, y, ROW2_CARD_W, ROW2_CARD_H, COL_LOCKED_BDR);
            ctx.drawItem(new ItemStack(Items.BARRIER), x + (ROW2_CARD_W - 16) / 2, y + 4);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a78Nether"),
                    x + ROW2_CARD_W / 2, y + 26, 0x666666);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a78Permission requise"),
                    x + ROW2_CARD_W / 2, y + 38, 0x666666);
        }
    }

    /**
     * Dessine la carte The End (verrouillee si !hasEnd).
     */
    private void renderEndCard(DrawContext ctx, int mouseX, int mouseY) {
        int x = row2X[1];
        int y = row2Y;

        if (hasEnd) {
            boolean hovered = isHover(mouseX, mouseY, x, y, x + ROW2_CARD_W, y + ROW2_CARD_H);
            ctx.fill(x, y, x + ROW2_CARD_W, y + ROW2_CARD_H, COL_CARD_BG);
            ctx.drawBorder(x, y, ROW2_CARD_W, ROW2_CARD_H, hovered ? COL_CARD_HOVER : COL_CARD_BDR);
            ctx.drawItem(new ItemStack(Items.END_STONE), x + (ROW2_CARD_W - 16) / 2, y + 4);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a75\u00a7lThe End"),
                    x + ROW2_CARD_W / 2, y + 26, 0xFFFFFF);
        } else {
            ctx.fill(x, y, x + ROW2_CARD_W, y + ROW2_CARD_H, COL_LOCKED_BG);
            ctx.drawBorder(x, y, ROW2_CARD_W, ROW2_CARD_H, COL_LOCKED_BDR);
            ctx.drawItem(new ItemStack(Items.BARRIER), x + (ROW2_CARD_W - 16) / 2, y + 4);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a78The End"),
                    x + ROW2_CARD_W / 2, y + 26, 0x666666);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a78Permission requise"),
                    x + ROW2_CARD_W / 2, y + 38, 0x666666);
        }
    }

    /**
     * Dessine le bouton retour stylise.
     */
    private void renderBackButton(DrawContext ctx, int mouseX, int mouseY) {
        boolean hovered = isHover(mouseX, mouseY, btnX, btnY, btnX + BTN_W, btnY + BTN_H);
        ctx.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, COL_CARD_BG);
        ctx.drawBorder(btnX, btnY, BTN_W, BTN_H, hovered ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a77\u00a7l\u00ab Retour"),
                btnX + BTN_W / 2, btnY + (BTN_H - 8) / 2, 0xFFFFFF);
    }

    // ── Gestion des clics ─────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // ── Rangee 1 : toujours cliquables ────────────────────────────────
            if (isHover(mouseX, mouseY, row1X[0], row1Y, row1X[0] + ROW1_CARD_W, row1Y + ROW1_CARD_H)) {
                ClientPlayNetworking.send(new MenuActionPayload("tp_overworld"));
                return true;
            }
            if (isHover(mouseX, mouseY, row1X[1], row1Y, row1X[1] + ROW1_CARD_W, row1Y + ROW1_CARD_H)) {
                ClientPlayNetworking.send(new MenuActionPayload("tp_spawn"));
                return true;
            }
            if (isHover(mouseX, mouseY, row1X[2], row1Y, row1X[2] + ROW1_CARD_W, row1Y + ROW1_CARD_H)) {
                ClientPlayNetworking.send(new MenuActionPayload("tp_resource"));
                return true;
            }

            // ── Rangee 2 : conditionnelles ────────────────────────────────────
            if (hasNether && isHover(mouseX, mouseY, row2X[0], row2Y, row2X[0] + ROW2_CARD_W, row2Y + ROW2_CARD_H)) {
                ClientPlayNetworking.send(new MenuActionPayload("tp_nether"));
                return true;
            }
            if (hasEnd && isHover(mouseX, mouseY, row2X[1], row2Y, row2X[1] + ROW2_CARD_W, row2Y + ROW2_CARD_H)) {
                ClientPlayNetworking.send(new MenuActionPayload("tp_end"));
                return true;
            }

            // ── Bouton retour ─────────────────────────────────────────────────
            if (isHover(mouseX, mouseY, btnX, btnY, btnX + BTN_W, btnY + BTN_H)) {
                ClientPlayNetworking.send(new MenuActionPayload("open_menu"));
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
