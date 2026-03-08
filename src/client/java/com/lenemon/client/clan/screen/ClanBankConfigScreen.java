package com.lenemon.client.clan.screen;

import com.lenemon.network.clan.ClanActionPayload;
import com.lenemon.network.clan.ClanGuiPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ecran de configuration de la banque du clan.
 * Permet a l'owner de definir la limite de retrait journalier par rang.
 * -1 = illimite. 0 = bloque. >= 0 = limite concrete en coins.
 *
 * Layout : Rang (120px) | Limite (centree, 90px) | Boutons (-5000 -500 +500 +5000 ∞)
 */
public class ClanBankConfigScreen extends Screen {

    // ── Dimensions ──────────────────────────────────────────────────────────
    private static final int GUI_W  = 420;
    private static final int GUI_H  = 240;
    private static final int ROW_H  = 22;

    // Colonnes (positions relatives au debut de la liste, pas au gx)
    private static final int COL_RANK_X    = 4;   // x relatif dans la ligne
    private static final int COL_RANK_W    = 120;
    private static final int COL_LIMIT_X   = 128; // debut de la colonne limite
    private static final int COL_LIMIT_W   = 90;  // largeur de la zone limite (texte centre)
    private static final int COL_BTNS_X    = 222; // debut des boutons

    // Tailles des boutons
    private static final int BTN_W  = 36;
    private static final int BTN_H  = 14;
    private static final int INF_W  = 16;
    private static final int BTN_GAP = 3;

    // ── Palette ─────────────────────────────────────────────────────────────
    private static final int COL_BG        = 0xCC0A0A1A;
    private static final int COL_BORDER    = 0xFF2255AA;
    private static final int COL_SEPARATOR = 0x55FFFFFF;
    private static final int COL_LABEL     = 0xAAAAAA;
    private static final int COL_ROW_BG    = 0x55113355;
    private static final int COL_ROW_BDR   = 0xFF334466;
    private static final int COL_BTN_BG    = 0x55222244;
    private static final int COL_BTN_BDR   = 0xFF334466;
    private static final int COL_BTN_HOV   = 0xFF4477CC;
    private static final int COL_INF_BG    = 0x55002244;
    private static final int COL_INF_BDR   = 0xFF004488;
    private static final int COL_INF_HOV   = 0xFF0088CC;
    private static final int COL_CLOSE_BG  = 0x55330000;
    private static final int COL_CLOSE_BDR = 0xFF443333;
    private static final int COL_CLOSE_HOV = 0xFFAA2222;

    // ── Etat ────────────────────────────────────────────────────────────────
    private final Screen parent;
    private int gx, gy;
    private int scrollOffset = 0;

    /** Copie locale mutable des rangs avec leur limite. */
    private final List<RankLimit> limits = new ArrayList<>();

    private static class RankLimit {
        final String rankId;
        String name;
        String colorCode;
        long limit; // -1=illimite, 0=bloque, >0=limite

        RankLimit(ClanGuiPayload.RankDto dto) {
            this.rankId    = dto.id();
            this.name      = dto.name();
            this.colorCode = dto.colorCode();
            this.limit     = dto.withdrawLimit();
        }
    }

    /** Position calculee de chaque ligne visible. */
    private static class RowLayout {
        int y;
        int listX; // x absolu de debut de la ligne
    }
    private final List<RowLayout> rowLayouts = new ArrayList<>();
    private ClanConfigScreen.BtnLayout btnBack;

    // ── Constructeur ────────────────────────────────────────────────────────

    public ClanBankConfigScreen(ClanGuiPayload data, Screen parent) {
        super(Text.literal("Config Banque - " + data.clanName()));
        this.parent = parent;
        List<ClanGuiPayload.RankDto> sorted = new ArrayList<>(data.ranks());
        sorted.sort(Comparator.comparingInt(ClanGuiPayload.RankDto::sortOrder));
        for (ClanGuiPayload.RankDto dto : sorted) {
            limits.add(new RankLimit(dto));
        }
    }

    // ── Init ────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        gx = (width  - GUI_W) / 2;
        gy = (height - GUI_H) / 2;
        btnBack = new ClanConfigScreen.BtnLayout(gx + GUI_W - 80, gy + GUI_H - 22, 70, 14, "§7◄ Retour");
    }

    // ── Render ──────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);
        ClanConfigScreen.drawBorder(ctx, gx, gy, GUI_W, GUI_H, COL_BORDER);

        // Titre
        ctx.drawText(textRenderer, Text.literal("§eLimites de retrait par rang"),
                gx + 10, gy + 8, 0xFFFFFF, true);
        ctx.drawText(textRenderer, Text.literal("§7-1 / ∞ = Illimite  |  0 = Bloque  |  > 0 = Limite journaliere"),
                gx + 10, gy + 20, COL_LABEL, false);
        ctx.fill(gx + 8, gy + 32, gx + GUI_W - 8, gy + 33, COL_SEPARATOR);

        // En-tetes colonnes
        int listX = gx + 10;
        ctx.drawText(textRenderer, Text.literal("§7Rang"),   listX + COL_RANK_X,   gy + 36, COL_LABEL, false);
        ctx.drawText(textRenderer, Text.literal("§7Limite"), listX + COL_LIMIT_X,  gy + 36, COL_LABEL, false);
        ctx.drawText(textRenderer, Text.literal("§7Ajustements"), listX + COL_BTNS_X, gy + 36, COL_LABEL, false);

        // Liste
        int listY    = gy + 44;
        int listH    = GUI_H - 44 - 28;
        int visRows  = listH / ROW_H;
        int max      = Math.min(limits.size(), scrollOffset + visRows);

        rowLayouts.clear();
        for (int i = scrollOffset; i < max; i++) {
            RankLimit rl   = limits.get(i);
            int rowY       = listY + (i - scrollOffset) * ROW_H;

            RowLayout layout = new RowLayout();
            layout.y     = rowY;
            layout.listX = listX;
            rowLayouts.add(layout);

            boolean rowHov = mx >= listX && mx <= gx + GUI_W - 12
                    && my >= rowY && my <= rowY + ROW_H - 2;

            ctx.fill(listX, rowY, gx + GUI_W - 12, rowY + ROW_H - 2,
                    rowHov ? 0x55223344 : COL_ROW_BG);
            ClanConfigScreen.drawBorder(ctx, listX, rowY, GUI_W - 22, ROW_H - 2, COL_ROW_BDR);

            // Colonne rang : couleur + nom (tronque si necessaire)
            String rankLabel = rl.colorCode + rl.name;
            ctx.drawText(textRenderer, Text.literal(rankLabel),
                    listX + COL_RANK_X, rowY + 7, 0xFFFFFF, false);

            // Colonne limite : texte centre dans sa zone
            String limitStr = formatLimit(rl.limit);
            int limitTw = textRenderer.getWidth(Text.literal(limitStr));
            int limitCenterX = listX + COL_LIMIT_X + (COL_LIMIT_W - limitTw) / 2;
            ctx.drawText(textRenderer, Text.literal(limitStr),
                    limitCenterX, rowY + 7, 0xFFFFFF, false);

            // Boutons : -5000  -500  +500  +5000  ∞
            int bx = listX + COL_BTNS_X;
            int by = rowY + 4;
            renderSmallBtn(ctx, bx,                                          by, BTN_W, BTN_H, "-5000", mx, my, false);
            renderSmallBtn(ctx, bx + BTN_W + BTN_GAP,                        by, BTN_W, BTN_H, "-500",  mx, my, false);
            renderSmallBtn(ctx, bx + (BTN_W + BTN_GAP) * 2,                  by, BTN_W, BTN_H, "+500",  mx, my, false);
            renderSmallBtn(ctx, bx + (BTN_W + BTN_GAP) * 3,                  by, BTN_W, BTN_H, "+5000", mx, my, false);
            renderSmallBtn(ctx, bx + (BTN_W + BTN_GAP) * 4,                  by, INF_W, BTN_H, "∞",     mx, my, true);
        }

        // Scroll indicators
        if (scrollOffset > 0) {
            ctx.drawText(textRenderer, Text.literal("§7▲"), gx + GUI_W - 20, gy + 36, COL_LABEL, false);
        }
        if (scrollOffset + visRows < limits.size()) {
            ctx.drawText(textRenderer, Text.literal("§7▼"), gx + GUI_W - 20, gy + GUI_H - 36, COL_LABEL, false);
        }

        // Bouton retour
        renderBtn(ctx, btnBack, mx, my, COL_CLOSE_BG, COL_CLOSE_BDR, COL_CLOSE_HOV);

        super.render(ctx, mx, my, delta);
    }

    // ── Mouse ───────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int imx = (int) mx;
        int imy = (int) my;

        if (ClanConfigScreen.isOver(btnBack, imx, imy)) {
            client.setScreen(parent);
            return true;
        }

        int listH    = GUI_H - 44 - 28;
        int visRows  = listH / ROW_H;
        int max      = Math.min(limits.size(), scrollOffset + visRows);

        for (int i = scrollOffset; i < max; i++) {
            if (i - scrollOffset >= rowLayouts.size()) break;
            RankLimit rl   = limits.get(i);
            RowLayout layout = rowLayouts.get(i - scrollOffset);
            int rowY  = layout.y;
            int listX = layout.listX;
            int by    = rowY + 4;

            if (imy < by || imy > by + BTN_H) continue;

            int bx = listX + COL_BTNS_X;

            if (isOverBtn(imx, imy, bx,                             by, BTN_W, BTN_H)) { applyDelta(rl, -5000); return true; }
            if (isOverBtn(imx, imy, bx + BTN_W + BTN_GAP,           by, BTN_W, BTN_H)) { applyDelta(rl, -500);  return true; }
            if (isOverBtn(imx, imy, bx + (BTN_W + BTN_GAP) * 2,     by, BTN_W, BTN_H)) { applyDelta(rl, 500);   return true; }
            if (isOverBtn(imx, imy, bx + (BTN_W + BTN_GAP) * 3,     by, BTN_W, BTN_H)) { applyDelta(rl, 5000);  return true; }
            if (isOverBtn(imx, imy, bx + (BTN_W + BTN_GAP) * 4,     by, INF_W, BTN_H)) { applyInfinite(rl);     return true; }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        int visRows  = (GUI_H - 44 - 28) / ROW_H;
        int maxScroll = Math.max(0, limits.size() - visRows);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vy));
        return true;
    }

    // ── Logique limite ──────────────────────────────────────────────────────

    /**
     * Applique un delta a la limite d'un rang.
     * Si illimite (-1), passer a 0 d'abord avant d'ajouter un delta positif.
     * Ne descend jamais en dessous de 0 (sauf -1 via le bouton ∞).
     */
    private void applyDelta(RankLimit rl, long delta) {
        long current = rl.limit;
        long newLimit;
        if (current == -1L) {
            // Illimite : passer a 0 si delta negatif, 0 + delta si positif
            newLimit = delta > 0 ? delta : 0L;
        } else {
            newLimit = Math.max(0L, current + delta);
        }
        rl.limit = newLimit;
        ClientPlayNetworking.send(new ClanActionPayload("rank_set_limit:" + rl.rankId + ":" + newLimit));
    }

    /** Remet la limite a -1 (illimite). */
    private void applyInfinite(RankLimit rl) {
        rl.limit = -1L;
        ClientPlayNetworking.send(new ClanActionPayload("rank_set_limit:" + rl.rankId + ":-1"));
    }

    /** Formate la valeur de limite pour l'affichage. */
    private static String formatLimit(long limit) {
        if (limit == -1L) return "§aIllimite";
        if (limit == 0L)  return "§cBloque";
        return "§e" + limit + " §7coins";
    }

    // ── Helpers render ───────────────────────────────────────────────────────

    private void renderSmallBtn(DrawContext ctx, int x, int y, int w, int h,
                                String label, int mx, int my, boolean isInf) {
        boolean hov = isOverBtn(mx, my, x, y, w, h);
        int bg  = isInf ? (hov ? 0x55003355 : COL_INF_BG)  : (hov ? 0x55334455 : COL_BTN_BG);
        int bdr = isInf ? (hov ? COL_INF_HOV : COL_INF_BDR) : (hov ? COL_BTN_HOV : COL_BTN_BDR);
        ctx.fill(x, y, x + w, y + h, bg);
        ClanConfigScreen.drawBorder(ctx, x, y, w, h, bdr);
        int tw = textRenderer.getWidth(label);
        ctx.drawText(textRenderer, Text.literal(label),
                x + (w - tw) / 2, y + (h - 8) / 2,
                hov ? 0xFFFFFF : 0xAAAAFF, false);
    }

    private void renderBtn(DrawContext ctx, ClanConfigScreen.BtnLayout btn, int mx, int my,
                           int bgColor, int bdrColor, int hoverColor) {
        boolean hov = ClanConfigScreen.isOver(btn, mx, my);
        ctx.fill(btn.x, btn.y, btn.x + btn.w, btn.y + btn.h, bgColor);
        ClanConfigScreen.drawBorder(ctx, btn.x, btn.y, btn.w, btn.h, hov ? hoverColor : bdrColor);
        int tw = textRenderer.getWidth(btn.label);
        ctx.drawText(textRenderer, Text.literal(btn.label),
                btn.x + (btn.w - tw) / 2, btn.y + (btn.h - 8) / 2,
                hov ? 0xFFFFFF : COL_LABEL, false);
    }

    private static boolean isOverBtn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ── Screen ──────────────────────────────────────────────────────────────

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}
}
