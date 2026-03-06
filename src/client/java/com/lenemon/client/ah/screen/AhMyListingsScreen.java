package com.lenemon.client.ah.screen;

import com.lenemon.network.ah.AhActionPayload;
import com.lenemon.network.ah.AhListingDto;
import com.lenemon.network.ah.AhMyListingsPayload;
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
 * AhMyListingsScreen — écran "Mes ventes" de l'Hôtel des Ventes.
 *
 * Deux sections paginées indépendamment :
 *  - Ventes actives   (5 par page, bouton Annuler)
 *  - Invendus à récupérer (5 par page, bouton Récupérer)
 */
public class AhMyListingsScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_WIDTH  = 340;
    private static final int GUI_HEIGHT = 320;

    private static final int ENTRY_H     = 26;
    private static final int ENTRY_W_BTN = 64;
    private static final int SECTION_H   = 14;
    private static final int MARGIN      = 12;
    private static final int ENTRY_GAP   = 3;
    private static final int PER_PAGE    = 4;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xCC0A0A1A;
    private static final int COL_BORDER     = 0xFF2255AA;
    private static final int COL_SEPARATOR  = 0x55FFFFFF;
    private static final int COL_CARD_BG    = 0x55113355;
    private static final int COL_CARD_BDR   = 0xFF334466;
    private static final int COL_CARD_HOVER = 0xFF4477CC;
    private static final int COL_LABEL      = 0xAAAAAA;

    private static final int BTN_H = 13;
    private static final int BTN_W = 72;

    // ── Données ───────────────────────────────────────────────────────────────
    private final AhMyListingsPayload data;

    // ── Pagination ────────────────────────────────────────────────────────────
    private int activePage   = 0;
    private int recoveryPage = 0;

    // ── Layout ────────────────────────────────────────────────────────────────
    private int guiX, guiY;

    // Positions calculées dans init()
    private int activeLabelY;
    private int activeSepY;
    private int recoveryLabelY;

    // Boutons de pagination actives
    private int actPrevX, actPrevY, actNextX, actNextY;
    // Boutons de pagination recovery
    private int recPrevX, recPrevY, recNextX, recNextY;

    // Boutons bas
    private int btnBackX, btnBackY, btnSellX, btnSellY, btnPkmX, btnPkmY;

    // Entrées visibles
    private final List<EntryLayout> activeEntries   = new ArrayList<>();
    private final List<EntryLayout> recoveryEntries = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public AhMyListingsScreen(AhMyListingsPayload data) {
        super(Text.literal("Mes Ventes"));
        this.data = data;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean shouldPause() { return false; }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        guiX = (this.width  - GUI_WIDTH)  / 2;
        guiY = (this.height - GUI_HEIGHT) / 2;

        activeEntries.clear();
        recoveryEntries.clear();

        int entryW = GUI_WIDTH - MARGIN * 2;
        int cursorY = guiY + 26;

        // ── Section : Ventes actives ──────────────────────────────────────────
        activeLabelY = cursorY;
        cursorY += SECTION_H + 2;

        List<AhListingDto> active = data.myListings();
        int activeTotalPages = Math.max(1, (active.size() + PER_PAGE - 1) / PER_PAGE);
        activePage = Math.max(0, Math.min(activePage, activeTotalPages - 1));
        int activeFrom = activePage * PER_PAGE;
        int activeTo   = Math.min(activeFrom + PER_PAGE, active.size());

        for (int i = activeFrom; i < activeTo; i++) {
            activeEntries.add(new EntryLayout(guiX + MARGIN, cursorY, entryW, ENTRY_H, i, false));
            cursorY += ENTRY_H + ENTRY_GAP;
        }
        if (active.isEmpty()) cursorY += 14;

        // Boutons pagination section active
        actPrevX = guiX + MARGIN;
        actNextX = guiX + GUI_WIDTH - MARGIN - 36;
        actPrevY = actNextY = cursorY + 2;
        cursorY  += (activeTotalPages > 1) ? BTN_H + 6 : 6;

        // Séparateur milieu
        activeSepY = cursorY + 2;
        cursorY += 8;

        // ── Section : Invendus à récupérer ───────────────────────────────────
        recoveryLabelY = cursorY;
        cursorY += SECTION_H + 2;

        List<AhListingDto> recovery = data.pendingRecovery();
        int recoveryTotalPages = Math.max(1, (recovery.size() + PER_PAGE - 1) / PER_PAGE);
        recoveryPage = Math.max(0, Math.min(recoveryPage, recoveryTotalPages - 1));
        int recoveryFrom = recoveryPage * PER_PAGE;
        int recoveryTo   = Math.min(recoveryFrom + PER_PAGE, recovery.size());

        for (int i = recoveryFrom; i < recoveryTo; i++) {
            recoveryEntries.add(new EntryLayout(guiX + MARGIN, cursorY, entryW, ENTRY_H, i, true));
            cursorY += ENTRY_H + ENTRY_GAP;
        }
        if (recovery.isEmpty()) cursorY += 14;

        // Boutons pagination section recovery
        recPrevX = guiX + MARGIN;
        recNextX = guiX + GUI_WIDTH - MARGIN - 36;
        recPrevY = recNextY = cursorY + 2;

        // ── Boutons bas ───────────────────────────────────────────────────────
        int actY = guiY + GUI_HEIGHT - BTN_H - 8;
        btnBackX = guiX + MARGIN;
        btnBackY = actY;
        btnSellX = guiX + MARGIN + BTN_W + 4;
        btnSellY = actY;
        btnPkmX  = guiX + MARGIN + (BTN_W + 4) * 2;
        btnPkmY  = actY;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, COL_BORDER);

        // Titre
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a77\u00a7lMes ventes"),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);

        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_WIDTH - 10, guiY + 21, COL_SEPARATOR);

        // Balance
        String balTxt = "\u00a76\u00a7f" + data.balance() + "\u00a76\u20b1";
        ctx.drawTextWithShadow(textRenderer, Text.literal(balTxt),
                guiX + GUI_WIDTH - 8 - textRenderer.getWidth(balTxt), guiY + 8, 0xFFFFFF);

        // ── Section active ──────────────────────────────────────────────────
        List<AhListingDto> active = data.myListings();
        int activeTotalPages = Math.max(1, (active.size() + PER_PAGE - 1) / PER_PAGE);

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a77Ventes en cours (" + active.size() + ")"),
                guiX + MARGIN, activeLabelY, COL_LABEL);

        for (EntryLayout entry : activeEntries) {
            renderEntry(ctx, entry, active.get(entry.index()), false, mouseX, mouseY);
        }
        if (active.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("\u00a78Aucune vente active"),
                    guiX + MARGIN + 10, activeLabelY + SECTION_H + 4, 0xFFFFFF);
        }

        // Pagination active
        if (activeTotalPages > 1) {
            renderPaginationRow(ctx, mouseX, mouseY, actPrevX, actPrevY, actNextX, actNextY,
                    activePage, activeTotalPages, true);
        }

        // Séparateur milieu
        ctx.fill(guiX + 10, activeSepY, guiX + GUI_WIDTH - 10, activeSepY + 1, COL_SEPARATOR);

        // ── Section recovery ────────────────────────────────────────────────
        List<AhListingDto> recovery = data.pendingRecovery();
        int recoveryTotalPages = Math.max(1, (recovery.size() + PER_PAGE - 1) / PER_PAGE);

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a7eInvendus à récupérer (" + recovery.size() + ")"),
                guiX + MARGIN, recoveryLabelY, COL_LABEL);

        for (EntryLayout entry : recoveryEntries) {
            renderEntry(ctx, entry, recovery.get(entry.index()), true, mouseX, mouseY);
        }
        if (recovery.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("\u00a78Rien à récupérer"),
                    guiX + MARGIN + 10, recoveryLabelY + SECTION_H + 4, 0xFFFFFF);
        }

        // Pagination recovery
        if (recoveryTotalPages > 1) {
            renderPaginationRow(ctx, mouseX, mouseY, recPrevX, recPrevY, recNextX, recNextY,
                    recoveryPage, recoveryTotalPages, false);
        }

        // Séparateur bas
        ctx.fill(guiX + 10, guiY + GUI_HEIGHT - BTN_H - 12,
                guiX + GUI_WIDTH - 10, guiY + GUI_HEIGHT - BTN_H - 11, COL_SEPARATOR);

        renderBottomButtons(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderPaginationRow(DrawContext ctx, int mouseX, int mouseY,
                                      int prevX, int prevY, int nextX, int nextY,
                                      int page, int totalPages, boolean isActive) {
        String pageStr = "\u00a77" + (page + 1) + "/" + totalPages;
        int midX = guiX + GUI_WIDTH / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(pageStr), midX, prevY + (BTN_H - 8) / 2, 0xFFFFFF);

        if (page > 0) {
            boolean h = isHover(mouseX, mouseY, prevX, prevY, 36, BTN_H);
            ctx.fill(prevX, prevY, prevX + 36, prevY + BTN_H, COL_CARD_BG);
            ctx.drawBorder(prevX, prevY, 36, BTN_H, h ? COL_CARD_HOVER : COL_CARD_BDR);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77<"),
                    prevX + 18, prevY + (BTN_H - 8) / 2, 0xFFFFFF);
        }
        if (page < totalPages - 1) {
            boolean h = isHover(mouseX, mouseY, nextX, nextY, 36, BTN_H);
            ctx.fill(nextX, nextY, nextX + 36, nextY + BTN_H, COL_CARD_BG);
            ctx.drawBorder(nextX, nextY, 36, BTN_H, h ? COL_CARD_HOVER : COL_CARD_BDR);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77>"),
                    nextX + 18, nextY + (BTN_H - 8) / 2, 0xFFFFFF);
        }
    }

    private void renderEntry(DrawContext ctx, EntryLayout entry, AhListingDto dto,
                              boolean isRecovery, int mouseX, int mouseY) {
        int x = entry.x(), y = entry.y(), w = entry.w(), h = entry.h();

        ctx.fill(x, y, x + w, y + h, COL_CARD_BG);
        ctx.drawBorder(x, y, w, h, COL_CARD_BDR);

        // Icone
        int iconX = x + 4, iconY = y + (h - 16) / 2;
        if ("pokemon".equals(dto.type())) {
            ctx.drawItem(new ItemStack(Items.PAPER), iconX, iconY);
        } else {
            net.minecraft.item.Item mcItem = Registries.ITEM.get(Identifier.of(dto.itemId()));
            if (mcItem == null || mcItem == Items.AIR) mcItem = Items.BARRIER;
            ctx.drawItem(new ItemStack(mcItem), iconX, iconY);
        }

        // Nom (tronqué si nécessaire pour laisser de la place au bouton)
        int tx   = x + 24;
        int maxW = w - 24 - ENTRY_W_BTN - 6;
        String rawName = "pokemon".equals(dto.type())
                ? (dto.pokemonShiny() ? "\u00a7e\u2746 " : "") + "\u00a7b" + dto.pokemonDisplayName()
                : "\u00a7f" + dto.itemDisplayName();
        var nameLines = textRenderer.wrapLines(Text.literal(rawName), maxW);
        if (!nameLines.isEmpty()) ctx.drawTextWithShadow(textRenderer, nameLines.get(0), tx, y + 4, 0xFFFFFF);

        // Prix
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a7a" + dto.price() + "\u00a76\u20b1"), tx, y + 14, 0xFFFFFF);

        // Bouton d'action
        int btnX = x + w - ENTRY_W_BTN - 4;
        int btnY = y + (h - 12) / 2;
        boolean hoverBtn = isHover(mouseX, mouseY, btnX, btnY, ENTRY_W_BTN, 12);
        if (isRecovery) {
            ctx.fill(btnX, btnY, btnX + ENTRY_W_BTN, btnY + 12, 0x55003300);
            ctx.drawBorder(btnX, btnY, ENTRY_W_BTN, 12, hoverBtn ? 0xFF22AA22 : 0xFF226622);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a7aRécupérer"),
                    btnX + ENTRY_W_BTN / 2, btnY + 2, 0xFFFFFF);
        } else {
            ctx.fill(btnX, btnY, btnX + ENTRY_W_BTN, btnY + 12, 0x55330000);
            ctx.drawBorder(btnX, btnY, ENTRY_W_BTN, 12, hoverBtn ? 0xFFAA2222 : 0xFF662222);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a7cAnnuler"),
                    btnX + ENTRY_W_BTN / 2, btnY + 2, 0xFFFFFF);
        }
    }

    private void renderBottomButtons(DrawContext ctx, int mouseX, int mouseY) {
        boolean h1 = isHover(mouseX, mouseY, btnBackX, btnBackY, BTN_W, BTN_H);
        ctx.fill(btnBackX, btnBackY, btnBackX + BTN_W, btnBackY + BTN_H, COL_CARD_BG);
        ctx.drawBorder(btnBackX, btnBackY, BTN_W, BTN_H, h1 ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77\u25c4 Retour"),
                btnBackX + BTN_W / 2, btnBackY + (BTN_H - 8) / 2, 0xFFFFFF);

        boolean h2 = isHover(mouseX, mouseY, btnSellX, btnSellY, BTN_W, BTN_H);
        ctx.fill(btnSellX, btnSellY, btnSellX + BTN_W, btnSellY + BTN_H, COL_CARD_BG);
        ctx.drawBorder(btnSellX, btnSellY, BTN_W, BTN_H, h2 ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a7a+ Vendre item"),
                btnSellX + BTN_W / 2, btnSellY + (BTN_H - 8) / 2, 0xFFFFFF);

        boolean h3 = isHover(mouseX, mouseY, btnPkmX, btnPkmY, BTN_W, BTN_H);
        ctx.fill(btnPkmX, btnPkmY, btnPkmX + BTN_W, btnPkmY + BTN_H, COL_CARD_BG);
        ctx.drawBorder(btnPkmX, btnPkmY, BTN_W, BTN_H, h3 ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a7d+ Vendre Poke"),
                btnPkmX + BTN_W / 2, btnPkmY + (BTN_H - 8) / 2, 0xFFFFFF);
    }

    // ── Mouse events ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        List<AhListingDto> active   = data.myListings();
        List<AhListingDto> recovery = data.pendingRecovery();

        // Boutons entrées actives
        for (EntryLayout entry : activeEntries) {
            AhListingDto dto = active.get(entry.index());
            int btnX = entry.x() + entry.w() - ENTRY_W_BTN - 4;
            int btnY = entry.y() + (entry.h() - 12) / 2;
            if (isHover(mouseX, mouseY, btnX, btnY, ENTRY_W_BTN, 12)) {
                ClientPlayNetworking.send(new AhActionPayload("cancel:" + dto.listingId()));
                return true;
            }
        }

        // Boutons entrées recovery
        for (EntryLayout entry : recoveryEntries) {
            AhListingDto dto = recovery.get(entry.index());
            int btnX = entry.x() + entry.w() - ENTRY_W_BTN - 4;
            int btnY = entry.y() + (entry.h() - 12) / 2;
            if (isHover(mouseX, mouseY, btnX, btnY, ENTRY_W_BTN, 12)) {
                ClientPlayNetworking.send(new AhActionPayload("claim:" + dto.listingId()));
                return true;
            }
        }

        // Pagination active
        int activeTotalPages = Math.max(1, (active.size() + PER_PAGE - 1) / PER_PAGE);
        if (activeTotalPages > 1) {
            if (activePage > 0 && isHover(mouseX, mouseY, actPrevX, actPrevY, 36, BTN_H)) {
                activePage--;
                init();
                return true;
            }
            if (activePage < activeTotalPages - 1 && isHover(mouseX, mouseY, actNextX, actNextY, 36, BTN_H)) {
                activePage++;
                init();
                return true;
            }
        }

        // Pagination recovery
        int recoveryTotalPages = Math.max(1, (recovery.size() + PER_PAGE - 1) / PER_PAGE);
        if (recoveryTotalPages > 1) {
            if (recoveryPage > 0 && isHover(mouseX, mouseY, recPrevX, recPrevY, 36, BTN_H)) {
                recoveryPage--;
                init();
                return true;
            }
            if (recoveryPage < recoveryTotalPages - 1 && isHover(mouseX, mouseY, recNextX, recNextY, 36, BTN_H)) {
                recoveryPage++;
                init();
                return true;
            }
        }

        // Boutons bas
        if (isHover(mouseX, mouseY, btnBackX, btnBackY, BTN_W, BTN_H)) {
            ClientPlayNetworking.send(new AhActionPayload("open_ah")); return true;
        }
        if (isHover(mouseX, mouseY, btnSellX, btnSellY, BTN_W, BTN_H)) {
            ClientPlayNetworking.send(new AhActionPayload("open_sell_item")); return true;
        }
        if (isHover(mouseX, mouseY, btnPkmX, btnPkmY, BTN_W, BTN_H)) {
            ClientPlayNetworking.send(new AhActionPayload("open_sell_pokemon")); return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isHover(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ── Types internes ────────────────────────────────────────────────────────

    private record EntryLayout(int x, int y, int w, int h, int index, boolean recovery) {}
}
