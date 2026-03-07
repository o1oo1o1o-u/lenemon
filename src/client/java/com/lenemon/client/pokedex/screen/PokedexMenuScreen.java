package com.lenemon.client.pokedex.screen;

import com.lenemon.network.menu.MenuActionPayload;
import com.lenemon.network.pokedex.PokedexClaimPayload;
import com.lenemon.network.pokedex.PokedexOpenPayload;
import com.lenemon.network.pokedex.PokedexRegionDto;
import com.lenemon.network.pokedex.PokedexRewardTierDto;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu Pokédex : affiche une grille de cartes par région avec les % vus/capturés
 * et les paliers de récompenses. Cliquer une carte réclame les récompenses disponibles.
 */
public class PokedexMenuScreen extends Screen {

    // ── Dimensions GUI ──────────────────────────────────────────────────────
    private static final int GUI_W = 430;
    private static final int GUI_H = 290;

    // ── Grille ──────────────────────────────────────────────────────────────
    private static final int COLS        = 4;
    private static final int CARD_W      = 96;
    private static final int CARD_H      = 66;
    private static final int CARD_GAP_X  = 8;
    private static final int CARD_GAP_Y  = 7;
    private static final int GRID_TOP    = 32; // offset Y depuis guiY

    // ── Pagination ──────────────────────────────────────────────────────────
    private static final int CARDS_PER_PAGE = COLS * 3; // 12
    private int page = 0;

    // ── Palette ─────────────────────────────────────────────────────────────
    private static final int COL_BG          = 0xCC0A0A1A;
    private static final int COL_BORDER      = 0xFF2255AA;
    private static final int COL_SEPARATOR   = 0x55FFFFFF;
    private static final int COL_CARD_BG     = 0x55113355;
    private static final int COL_CARD_BDR    = 0xFF334466;
    private static final int COL_CARD_HOVER  = 0xFF4477CC;
    private static final int COL_CLAIMABLE   = 0xFF44FF44;  // bordure verte si récompense dispo
    private static final int COL_BAR_BG      = 0xFF1A1A2E;
    private static final int COL_BAR_SEEN    = 0xFF4488FF;
    private static final int COL_BAR_CAUGHT  = 0xFF44CC44;
    private static final int COL_LABEL       = 0xAAAAAA;
    private static final int COL_BTN_BG      = 0x55003355;
    private static final int COL_BTN_BDR     = 0xFF334466;
    private static final int COL_BTN_HOV     = 0xFF4477CC;

    // ── Données ─────────────────────────────────────────────────────────────
    private List<PokedexRegionDto> regions;

    // ── Layout calculé dans init() ───────────────────────────────────────────
    private int guiX, guiY;
    private int gridStartX;
    // Boutons de navigation
    private int prevBtnX, prevBtnY, nextBtnX, nextBtnY;
    private int backBtnX, backBtnY;
    private static final int NAV_BTN_W = 70, NAV_BTN_H = 18;

    public PokedexMenuScreen(PokedexOpenPayload payload) {
        super(Text.literal("Pokédex Rewards"));
        this.regions = new ArrayList<>(payload.regions());
    }

    /** Appelé par le client network pour rafraîchir l'affichage après un claim. */
    public void refresh(PokedexOpenPayload payload) {
        this.regions = new ArrayList<>(payload.regions());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) { }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        guiX = (this.width  - GUI_W) / 2;
        guiY = (this.height - GUI_H) / 2;

        // Largeur totale grille : COLS * CARD_W + (COLS-1) * GAP
        int gridW = COLS * CARD_W + (COLS - 1) * CARD_GAP_X;
        gridStartX = guiX + (GUI_W - gridW) / 2;

        // Boutons navigation bas
        int btnY = guiY + GUI_H - 24;
        prevBtnX = guiX + 8;   prevBtnY = btnY;
        nextBtnX = guiX + GUI_W - NAV_BTN_W - 8; nextBtnY = btnY;
        backBtnX = guiX + (GUI_W - NAV_BTN_W) / 2; backBtnY = btnY;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Fond + bordure
        ctx.fill(guiX, guiY, guiX + GUI_W, guiY + GUI_H, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_W, GUI_H, COL_BORDER);

        // Titre
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b§l✦ Pokédex Rewards"),
                guiX + GUI_W / 2, guiY + 8, 0xFFFFFF);

        // Séparateur titre
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_W - 10, guiY + 21, COL_SEPARATOR);

        // Grille de cartes
        int startIdx = page * CARDS_PER_PAGE;
        int endIdx   = Math.min(startIdx + CARDS_PER_PAGE, regions.size());

        for (int i = startIdx; i < endIdx; i++) {
            int slot = i - startIdx;
            int col  = slot % COLS;
            int row  = slot / COLS;
            int cx   = gridStartX + col * (CARD_W + CARD_GAP_X);
            int cy   = guiY + GRID_TOP + row * (CARD_H + CARD_GAP_Y);
            renderCard(ctx, regions.get(i), cx, cy, mouseX, mouseY);
        }

        // Bouton Retour (centre)
        boolean backHov = isOver(mouseX, mouseY, backBtnX, backBtnY, NAV_BTN_W, NAV_BTN_H);
        ctx.fill(backBtnX, backBtnY, backBtnX + NAV_BTN_W, backBtnY + NAV_BTN_H, backHov ? COL_BTN_HOV : COL_BTN_BG);
        ctx.drawBorder(backBtnX, backBtnY, NAV_BTN_W, NAV_BTN_H, COL_BTN_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7◄ Menu"),
                backBtnX + NAV_BTN_W / 2, backBtnY + (NAV_BTN_H - 8) / 2, 0xFFFFFF);

        int totalPages = Math.max(1, (int) Math.ceil((double) regions.size() / CARDS_PER_PAGE));
        if (totalPages > 1) {
            // Bouton Précédent
            if (page > 0) {
                boolean prevHov = isOver(mouseX, mouseY, prevBtnX, prevBtnY, NAV_BTN_W, NAV_BTN_H);
                ctx.fill(prevBtnX, prevBtnY, prevBtnX + NAV_BTN_W, prevBtnY + NAV_BTN_H, prevHov ? COL_BTN_HOV : COL_BTN_BG);
                ctx.drawBorder(prevBtnX, prevBtnY, NAV_BTN_W, NAV_BTN_H, COL_BTN_BDR);
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7◄ Préc."),
                        prevBtnX + NAV_BTN_W / 2, prevBtnY + (NAV_BTN_H - 8) / 2, 0xFFFFFF);
            }
            // Bouton Suivant
            if (page < totalPages - 1) {
                boolean nextHov = isOver(mouseX, mouseY, nextBtnX, nextBtnY, NAV_BTN_W, NAV_BTN_H);
                ctx.fill(nextBtnX, nextBtnY, nextBtnX + NAV_BTN_W, nextBtnY + NAV_BTN_H, nextHov ? COL_BTN_HOV : COL_BTN_BG);
                ctx.drawBorder(nextBtnX, nextBtnY, NAV_BTN_W, NAV_BTN_H, COL_BTN_BDR);
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Suiv. ►"),
                        nextBtnX + NAV_BTN_W / 2, nextBtnY + (NAV_BTN_H - 8) / 2, 0xFFFFFF);
            }
            // Page indicator
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§8" + (page + 1) + "/" + totalPages),
                    guiX + GUI_W / 2, backBtnY + (NAV_BTN_H - 8) / 2 + 10, COL_LABEL);
        }

        // Tooltips (paliers de récompense)
        renderTooltips(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Rendu d'une carte région ─────────────────────────────────────────────

    private void renderCard(DrawContext ctx, PokedexRegionDto region, int cx, int cy, int mouseX, int mouseY) {
        boolean hover     = isOver(mouseX, mouseY, cx, cy, CARD_W, CARD_H);
        boolean claimable = region.hasClaimable();

        int bdr = claimable ? COL_CLAIMABLE : (hover ? COL_CARD_HOVER : COL_CARD_BDR);

        ctx.fill(cx, cy, cx + CARD_W, cy + CARD_H, COL_CARD_BG);
        ctx.drawBorder(cx, cy, CARD_W, CARD_H, bdr);

        // Badge "!" si récompense dispo
        if (claimable) {
            ctx.fill(cx + CARD_W - 10, cy, cx + CARD_W, cy + 10, COL_CLAIMABLE);
            ctx.drawTextWithShadow(textRenderer, Text.literal("§l!"), cx + CARD_W - 8, cy + 1, 0x003300);
        }

        // Nom région (tronqué si nécessaire mais CARD_W-8 px est suffisant pour les noms choisis)
        String titleColor = claimable ? "§a§l" : (hover ? "§b§l" : "§f§l");
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(titleColor + region.label()),
                cx + CARD_W / 2, cy + 5, 0xFFFFFF);

        // Barres Vu / Capturé
        int barX1 = cx + 5;
        int barW  = CARD_W - 10;
        int barH  = 5;

        // Barre vu
        int seenBarY = cy + 16;
        ctx.fill(barX1, seenBarY, barX1 + barW, seenBarY + barH, COL_BAR_BG);
        int seenFill = Math.min(barW, Math.round(barW * region.seenPct() / 100f));
        if (seenFill > 0) ctx.fill(barX1, seenBarY, barX1 + seenFill, seenBarY + barH, COL_BAR_SEEN);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§9Vu: §f" + String.format("%.2f", region.seenPct()) + "%"),
                barX1, seenBarY + barH + 2, 0xFFFFFF);

        // Barre capturé
        int caughtBarY = cy + 34;
        ctx.fill(barX1, caughtBarY, barX1 + barW, caughtBarY + barH, COL_BAR_BG);
        int caughtFill = Math.min(barW, Math.round(barW * region.caughtPct() / 100f));
        if (caughtFill > 0) ctx.fill(barX1, caughtBarY, barX1 + caughtFill, caughtBarY + barH, COL_BAR_CAUGHT);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§aCapturé: §f" + String.format("%.2f", region.caughtPct()) + "%"),
                barX1, caughtBarY + barH + 2, 0xFFFFFF);

        // Résumé paliers : "X/Y récompenses"
        long total    = region.tiers().size();
        long obtained = region.tiers().stream().filter(PokedexRewardTierDto::claimed).count();
        String rewardSummary = "§7" + obtained + "/" + total + " récomp.";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(rewardSummary),
                cx + CARD_W / 2, cy + CARD_H - 10, COL_LABEL);
    }

    // ── Tooltips ────────────────────────────────────────────────────────────

    private void renderTooltips(DrawContext ctx, int mouseX, int mouseY) {
        int startIdx = page * CARDS_PER_PAGE;
        int endIdx   = Math.min(startIdx + CARDS_PER_PAGE, regions.size());

        for (int i = startIdx; i < endIdx; i++) {
            int slot = i - startIdx;
            int col  = slot % COLS;
            int row  = slot / COLS;
            int cx   = gridStartX + col * (CARD_W + CARD_GAP_X);
            int cy   = guiY + GRID_TOP + row * (CARD_H + CARD_GAP_Y);

            if (!isOver(mouseX, mouseY, cx, cy, CARD_W, CARD_H)) continue;

            PokedexRegionDto region = regions.get(i);
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§b§l" + region.label()));
            lines.add(Text.literal("§9Vu : §f" + String.format("%.2f", region.seenPct()) + "%  §aCapturé : §f" + String.format("%.2f", region.caughtPct()) + "%"));

            if (region.tiers().isEmpty()) {
                lines.add(Text.literal("§8Aucune récompense configurée"));
            } else {
                lines.add(Text.literal(""));
                for (PokedexRewardTierDto tier : region.tiers()) {
                    float pct = tier.type().equals("caught") ? region.caughtPct() : region.seenPct();
                    String typeLabel = tier.type().equals("caught") ? "§aCapturé" : "§9Vu";
                    boolean available = tier.isAvailable(pct);
                    String status;
                    if (tier.claimed())   status = "§7[✓ Récupéré]";
                    else if (available)   status = "§a[✦ Disponible !]";
                    else                  status = "§8[" + String.format("%.2f", pct) + "/" + tier.threshold() + "%]";
                    lines.add(Text.literal(typeLabel + " §f" + tier.threshold() + "% " + status + " §7" + tier.rewardDesc()));
                }
                if (region.hasClaimable()) {
                    lines.add(Text.literal(""));
                    lines.add(Text.literal("§e► Cliquez pour récupérer les récompenses !"));
                }
            }

            ctx.drawTooltip(textRenderer, lines, mouseX, mouseY);
            break; // un seul tooltip à la fois
        }
    }

    // ── Input ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int imx = (int) mx, imy = (int) my;

        // Bouton retour menu
        if (isOver(imx, imy, backBtnX, backBtnY, NAV_BTN_W, NAV_BTN_H)) {
            ClientPlayNetworking.send(new MenuActionPayload("open_menu"));
            return true;
        }

        // Pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) regions.size() / CARDS_PER_PAGE));
        if (page > 0 && isOver(imx, imy, prevBtnX, prevBtnY, NAV_BTN_W, NAV_BTN_H)) {
            page--;
            return true;
        }
        if (page < totalPages - 1 && isOver(imx, imy, nextBtnX, nextBtnY, NAV_BTN_W, NAV_BTN_H)) {
            page++;
            return true;
        }

        // Clic carte région
        int startIdx = page * CARDS_PER_PAGE;
        int endIdx   = Math.min(startIdx + CARDS_PER_PAGE, regions.size());
        for (int i = startIdx; i < endIdx; i++) {
            int slot = i - startIdx;
            int col  = slot % COLS;
            int row  = slot / COLS;
            int cx   = gridStartX + col * (CARD_W + CARD_GAP_X);
            int cy   = guiY + GRID_TOP + row * (CARD_H + CARD_GAP_Y);
            if (isOver(imx, imy, cx, cy, CARD_W, CARD_H)) {
                PokedexRegionDto region = regions.get(i);
                if (region.hasClaimable()) {
                    ClientPlayNetworking.send(new PokedexClaimPayload(region.id()));
                }
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { this.close(); return true; } // ESC
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private static boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
