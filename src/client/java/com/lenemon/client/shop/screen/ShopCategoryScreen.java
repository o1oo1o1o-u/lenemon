package com.lenemon.client.shop.screen;

import com.lenemon.network.shop.ShopActionPayload;
import com.lenemon.network.shop.ShopCategoryOpenPayload;
import com.lenemon.network.shop.ShopItemDto;
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
 * Ecran client-side d'une catégorie du shop — grille d'items avec achat et vente.
 *
 * Reçoit un {@link ShopCategoryOpenPayload} depuis le serveur.
 * Clic gauche → achat x1 (shift → x64)
 * Clic droit  → vente x1 (shift → vendre tout)
 * Bouton Retour → retour à la liste des catégories.
 */
public class ShopCategoryScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_WIDTH    = 340;
    private static final int GUI_HEIGHT   = 340;
    private static final int COLS         = 4;
    private static final int ROWS_DISPLAY = 3;
    private static final int ITEM_CARD_W  = 72;
    private static final int ITEM_CARD_H  = 78;
    private static final int GAP          = 6;
    private static final int MARGIN       = 10;
    private static final int HEADER_H     = 32; // titre + séparateur + balance
    private static final int NAV_H        = 28; // barre de navigation bas

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
    private static final int COL_NAV_BG     = 0x55113355;
    private static final int COL_NAV_BDR    = 0xFF334466;
    private static final int COL_NAV_HOV    = 0xFF4477CC;

    // ── Données reçues ────────────────────────────────────────────────────────
    private final ShopCategoryOpenPayload data;
    private long balance;

    // ── Layout calculé dans init() ────────────────────────────────────────────
    private int guiX, guiY;
    private final List<ItemCardLayout> itemCards = new ArrayList<>();

    // Boutons de navigation
    private int backBtnX,  backBtnY,  backBtnW  = 70, backBtnH  = 18;
    private int closeBtnX, closeBtnY, closeBtnW = 70, closeBtnH = 18;
    private int prevBtnX,  prevBtnY,  prevBtnW  = 60, prevBtnH  = 18;
    private int nextBtnX,  nextBtnY,  nextBtnW  = 60, nextBtnH  = 18;

    // Carte en survol pour le tooltip
    private ItemCardLayout hoveredCard = null;

    public ShopCategoryScreen(ShopCategoryOpenPayload data) {
        super(Text.literal("Shop - " + data.categoryName()));
        this.data = data;
        this.balance = data.balance();
    }

    /**
     * Met à jour la balance affichée sans rouvrir l'écran.
     * Appelé par LenemonNetworkClient sur réception de ShopBalanceUpdatePayload.
     */
    public void updateBalance(long newBalance) {
        this.balance = newBalance;
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
        itemCards.clear();

        guiX = (this.width  - GUI_WIDTH)  / 2;
        guiY = (this.height - GUI_HEIGHT) / 2;

        // Zone de grille disponible
        int gridAreaY = guiY + HEADER_H + MARGIN;
        int gridW = COLS * ITEM_CARD_W + (COLS - 1) * GAP;
        int gridStartX = guiX + (GUI_WIDTH - gridW) / 2;

        List<ShopItemDto> items = data.items();
        for (int i = 0; i < items.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = gridStartX + col * (ITEM_CARD_W + GAP);
            int cy = gridAreaY  + row * (ITEM_CARD_H + GAP);
            itemCards.add(new ItemCardLayout(cx, cy, ITEM_CARD_W, ITEM_CARD_H, items.get(i)));
        }

        // Barre de navigation (bas du GUI)
        int navY = guiY + GUI_HEIGHT - NAV_H - 4;
        int navCenterX = guiX + GUI_WIDTH / 2;

        // Retour (gauche)
        backBtnX = guiX + MARGIN;
        backBtnY = navY;

        // Fermer (droite)
        closeBtnX = guiX + GUI_WIDTH - MARGIN - closeBtnW;
        closeBtnY = navY;

        // Prev / Next (centre)
        prevBtnX = navCenterX - prevBtnW - 4;
        prevBtnY = navY;
        nextBtnX = navCenterX + 4;
        nextBtnY = navY;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hoveredCard = null;

        // ── Fond + bordure ────────────────────────────────────────────────────
        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, COL_BORDER);

        // ── Titre ─────────────────────────────────────────────────────────────
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§l" + data.categoryName()),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);

        // ── Balance (haut droite) ──────────────────────────────────────────────
        String balanceTxt = "§6Balance : §f" + balance + "§6\u20bd";
        int balW = textRenderer.getWidth(balanceTxt);
        ctx.drawTextWithShadow(textRenderer, Text.literal(balanceTxt),
                guiX + GUI_WIDTH - MARGIN - balW, guiY + 8, 0xFFFFFF);

        // ── Séparateur ────────────────────────────────────────────────────────
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_WIDTH - 10, guiY + 21, COL_SEPARATOR);

        // ── Grille d'items ────────────────────────────────────────────────────
        for (ItemCardLayout card : itemCards) {
            boolean hover = card.isHovered(mouseX, mouseY);
            renderItemCard(ctx, card, hover);
            if (hover) hoveredCard = card;
        }

        // ── Navigation ────────────────────────────────────────────────────────
        renderNavBar(ctx, mouseX, mouseY);

        // ── Tooltip (affiché en dernier) ──────────────────────────────────────
        if (hoveredCard != null) {
            ctx.drawTooltip(textRenderer, buildTooltip(hoveredCard.item()), mouseX, mouseY);
        }

        // Widgets vanilla
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderItemCard(DrawContext ctx, ItemCardLayout card, boolean hover) {
        int x = card.x();
        int y = card.y();
        int w = card.w();
        int h = card.h();
        ShopItemDto item = card.item();

        // Fond + bordure
        ctx.fill(x, y, x + w, y + h, COL_CARD_BG);
        ctx.drawBorder(x, y, w, h, hover ? COL_CARD_HOVER : COL_CARD_BDR);

        // Icone (centree, 4px de marge en haut)
        net.minecraft.item.Item mcItem = Registries.ITEM.get(Identifier.of(item.id()));
        if (mcItem == null || mcItem == Items.AIR) mcItem = Items.BARRIER;
        int iconX = x + (w - 16) / 2;
        ctx.drawItem(new ItemStack(mcItem), iconX, y + 4);

        // Nom (word-wrap, max 2 lignes, centré)
        int textMaxW = w - 4;
        int textCentX = x + w / 2;
        var nameLines = textRenderer.wrapLines(Text.literal("§f" + item.displayName()), textMaxW);
        int nameY = y + 22;
        for (int l = 0; l < Math.min(nameLines.size(), 2); l++) {
            ctx.drawCenteredTextWithShadow(textRenderer, nameLines.get(l),
                    textCentX, nameY + l * 9, 0xFFFFFF);
        }

        // Prix achat
        int priceY = nameY + Math.min(nameLines.size(), 2) * 9 + 2;
        if (item.buyPrice() >= 0) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§aAchat: " + formatPrice(item.buyPrice()) + "\u20bd"),
                    textCentX, priceY, 0xFFFFFF);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§cNon dispo"),
                    textCentX, priceY, 0xFFFFFF);
        }

        // Prix vente
        int sellY = priceY + 9;
        if (item.sellPrice() >= 0) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§eVente: " + formatPrice(item.sellPrice()) + "\u20bd"),
                    textCentX, sellY, 0xFFFFFF);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§8Non repris"),
                    textCentX, sellY, 0xFFFFFF);
        }
    }

    private void renderNavBar(DrawContext ctx, int mouseX, int mouseY) {
        // Bouton Retour
        boolean hoverBack = isInBounds(mouseX, mouseY, backBtnX, backBtnY, backBtnW, backBtnH);
        ctx.fill(backBtnX, backBtnY, backBtnX + backBtnW, backBtnY + backBtnH, COL_NAV_BG);
        ctx.drawBorder(backBtnX, backBtnY, backBtnW, backBtnH, hoverBack ? COL_NAV_HOV : COL_NAV_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7§l< Retour"),
                backBtnX + backBtnW / 2, backBtnY + (backBtnH - 8) / 2, 0xFFFFFF);

        // Bouton Fermer
        boolean hoverClose = isInBounds(mouseX, mouseY, closeBtnX, closeBtnY, closeBtnW, closeBtnH);
        ctx.fill(closeBtnX, closeBtnY, closeBtnX + closeBtnW, closeBtnY + closeBtnH, COL_CLOSE_BG);
        ctx.drawBorder(closeBtnX, closeBtnY, closeBtnW, closeBtnH, hoverClose ? COL_CLOSE_HOV : COL_CLOSE_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§c§lFermer"),
                closeBtnX + closeBtnW / 2, closeBtnY + (closeBtnH - 8) / 2, 0xFFFFFF);

        // Bouton Page précédente
        if (data.page() > 0) {
            boolean hoverPrev = isInBounds(mouseX, mouseY, prevBtnX, prevBtnY, prevBtnW, prevBtnH);
            ctx.fill(prevBtnX, prevBtnY, prevBtnX + prevBtnW, prevBtnY + prevBtnH, COL_NAV_BG);
            ctx.drawBorder(prevBtnX, prevBtnY, prevBtnW, prevBtnH, hoverPrev ? COL_NAV_HOV : COL_NAV_BDR);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§e< Préc."),
                    prevBtnX + prevBtnW / 2, prevBtnY + (prevBtnH - 8) / 2, 0xFFFFFF);
        }

        // Bouton Page suivante
        if (data.page() < data.totalPages() - 1) {
            boolean hoverNext = isInBounds(mouseX, mouseY, nextBtnX, nextBtnY, nextBtnW, nextBtnH);
            ctx.fill(nextBtnX, nextBtnY, nextBtnX + nextBtnW, nextBtnY + nextBtnH, COL_NAV_BG);
            ctx.drawBorder(nextBtnX, nextBtnY, nextBtnW, nextBtnH, hoverNext ? COL_NAV_HOV : COL_NAV_BDR);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§eSuiv. >"),
                    nextBtnX + nextBtnW / 2, nextBtnY + (nextBtnH - 8) / 2, 0xFFFFFF);
        }

        // Indicateur de page (centré entre prev et next)
        if (data.totalPages() > 1) {
            String pageLabel = "§7" + (data.page() + 1) + "/" + data.totalPages();
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(pageLabel),
                    guiX + GUI_WIDTH / 2, prevBtnY + (prevBtnH - 8) / 2, 0xFFFFFF);
        }
    }

    private List<Text> buildTooltip(ShopItemDto item) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("§f§l" + item.displayName()));
        lines.add(Text.literal(""));

        if (item.buyPrice() >= 0) {
            String priceStr = formatPrice(item.buyPrice());
            String totalStr = formatPrice(item.buyPrice() * 64);
            lines.add(Text.literal("§aClic gauche §7: §fAcheter x1 §a(" + priceStr + "\u20bd)"));
            lines.add(Text.literal("§7Shift + Clic gauche §7: §fAcheter x64 §a(" + totalStr + "\u20bd)"));
        } else {
            lines.add(Text.literal("§cNon disponible a l'achat"));
        }

        lines.add(Text.literal(""));

        if (item.sellPrice() >= 0) {
            String priceStr = formatPrice(item.sellPrice());
            lines.add(Text.literal("§eClic droit §7: §fVendre x1 §e(" + priceStr + "\u20bd)"));
            lines.add(Text.literal("§7Shift + Clic droit §7: §fVendre tout"));
        } else {
            lines.add(Text.literal("§8Non repris par le serveur"));
        }

        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int catIndex = data.categoryIndex();
        int page     = data.page();
        boolean shift = hasShiftDown();

        // Clic gauche
        if (button == 0) {
            // Cartes items — achat
            for (ItemCardLayout card : itemCards) {
                if (card.isHovered(mouseX, mouseY)) {
                    ShopItemDto item = card.item();
                    if (item.buyPrice() >= 0) {
                        int qty = shift ? 64 : 1;
                        String action = "buy:" + item.id() + ":" + qty
                                + ":cat:" + catIndex + ":page:" + page;
                        ClientPlayNetworking.send(new ShopActionPayload(action));
                    }
                    return true;
                }
            }

            // Bouton Retour
            if (isInBounds(mouseX, mouseY, backBtnX, backBtnY, backBtnW, backBtnH)) {
                ClientPlayNetworking.send(new ShopActionPayload("back_to_shop"));
                return true;
            }

            // Bouton Fermer
            if (isInBounds(mouseX, mouseY, closeBtnX, closeBtnY, closeBtnW, closeBtnH)) {
                this.client.setScreen(null);
                return true;
            }

            // Page précédente
            if (data.page() > 0 && isInBounds(mouseX, mouseY, prevBtnX, prevBtnY, prevBtnW, prevBtnH)) {
                ClientPlayNetworking.send(new ShopActionPayload(
                        "open_category:" + catIndex + ":page:" + (page - 1)));
                return true;
            }

            // Page suivante
            if (data.page() < data.totalPages() - 1
                    && isInBounds(mouseX, mouseY, nextBtnX, nextBtnY, nextBtnW, nextBtnH)) {
                ClientPlayNetworking.send(new ShopActionPayload(
                        "open_category:" + catIndex + ":page:" + (page + 1)));
                return true;
            }
        }

        // Clic droit — vente
        if (button == 1) {
            for (ItemCardLayout card : itemCards) {
                if (card.isHovered(mouseX, mouseY)) {
                    ShopItemDto item = card.item();
                    if (item.sellPrice() >= 0) {
                        String action;
                        if (shift) {
                            action = "sell_all:" + item.id() + ":cat:" + catIndex + ":page:" + page;
                        } else {
                            action = "sell:" + item.id() + ":1:cat:" + catIndex + ":page:" + page;
                        }
                        ClientPlayNetworking.send(new ShopActionPayload(action));
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isInBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String formatPrice(double price) {
        if (price == Math.floor(price)) return String.valueOf((long) price);
        return String.format("%.1f", price).replace(",", ".");
    }

    // ── Layout interne ────────────────────────────────────────────────────────

    private record ItemCardLayout(int x, int y, int w, int h, ShopItemDto item) {
        boolean isHovered(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}
