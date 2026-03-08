package com.lenemon.client.clan.screen;

import com.lenemon.clan.ClanRank;
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
 * Ecran de gestion des rangs du clan.
 * Owner peut : renommer, changer la couleur, reordonner (▲/▼), ajouter (max 20), supprimer.
 */
public class ClanRankConfigScreen extends Screen {

    private static final int GUI_W = 360;
    private static final int GUI_H = 250;
    private static final int ROW_H = 24;

    // Tailles des boutons d'action
    private static final int ORD_W = 12; // ▲ / ▼
    private static final int ORD_H = 12;
    private static final int DEL_W = 14;
    private static final int DEL_H = 14;

    private static final int COL_BG        = 0xCC0A0A1A;
    private static final int COL_BORDER    = 0xFF2255AA;
    private static final int COL_SEPARATOR = 0x55FFFFFF;
    private static final int COL_LABEL     = 0xAAAAAA;
    private static final int COL_ROW_BG    = 0x55113355;
    private static final int COL_ROW_SEL   = 0x55224466;
    private static final int COL_BTN_BG    = 0x55222244;
    private static final int COL_BTN_BDR   = 0xFF334466;
    private static final int COL_BTN_HOV   = 0xFF4477CC;
    private static final int COL_DEL_BG    = 0x55330000;
    private static final int COL_DEL_BDR   = 0xFF443333;
    private static final int COL_DEL_HOV   = 0xFFAA2222;
    private static final int COL_ADD_BG    = 0x55003300;
    private static final int COL_ADD_BDR   = 0xFF225522;
    private static final int COL_ADD_HOV   = 0xFF44AA44;
    private static final int COL_CLOSE_BG  = 0x55330000;
    private static final int COL_CLOSE_BDR = 0xFF443333;
    private static final int COL_CLOSE_HOV = 0xFFAA2222;

    private final Screen parent;
    private int gx, gy;
    private int scrollOffset = 0;

    // Etat local mutable
    private final List<RankState> ranks = new ArrayList<>();
    private String editingRankId = null;
    private String editBuffer    = "";

    // Confirmation suppression
    private String pendingDeleteId = null;

    private ClanConfigScreen.BtnLayout btnAdd;
    private ClanConfigScreen.BtnLayout btnBack;
    private ClanConfigScreen.BtnLayout btnConfirmYes;
    private ClanConfigScreen.BtnLayout btnConfirmNo;

    private static class RankState {
        String id;
        String name;
        String colorCode;
        boolean isSystem;
        int colorIdx;
        int sortOrder;

        RankState(ClanGuiPayload.RankDto dto) {
            this.id        = dto.id();
            this.name      = dto.name();
            this.colorCode = dto.colorCode();
            this.isSystem  = dto.id().equals("owner") || dto.id().equals("officer") || dto.id().equals("member");
            this.colorIdx  = ClanRank.colorIndex(dto.colorCode());
            this.sortOrder = dto.sortOrder();
        }
    }

    public ClanRankConfigScreen(ClanGuiPayload data, Screen parent) {
        super(Text.literal("Config Rangs - " + data.clanName()));
        this.parent = parent;
        List<ClanGuiPayload.RankDto> sorted = new ArrayList<>(data.ranks());
        sorted.sort(Comparator.comparingInt(ClanGuiPayload.RankDto::sortOrder));
        for (ClanGuiPayload.RankDto dto : sorted) {
            ranks.add(new RankState(dto));
        }
    }

    @Override
    protected void init() {
        gx = (width  - GUI_W) / 2;
        gy = (height - GUI_H) / 2;

        int listBottom = gy + GUI_H - 32;
        btnAdd  = new ClanConfigScreen.BtnLayout(gx + 10, listBottom, 100, 16, "§a+ Ajouter rang");
        btnBack = new ClanConfigScreen.BtnLayout(gx + GUI_W - 80, gy + GUI_H - 22, 70, 14, "§7◄ Retour");

        int cfmX = gx + (GUI_W - 180) / 2;
        int cfmY = gy + (GUI_H - 56) / 2;
        btnConfirmYes = new ClanConfigScreen.BtnLayout(cfmX + 8,  cfmY + 38, 76, 14, "§aOui, supprimer");
        btnConfirmNo  = new ClanConfigScreen.BtnLayout(cfmX + 96, cfmY + 38, 76, 14, "§cAnnuler");
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);
        ClanConfigScreen.drawBorder(ctx, gx, gy, GUI_W, GUI_H, COL_BORDER);

        // Titre
        ctx.drawText(textRenderer, Text.literal("§eGestion des rangs §8(" + ranks.size() + "/" + ClanRank.MAX_RANKS + ")"),
                gx + 10, gy + 8, 0xFFFFFF, true);
        ctx.drawText(textRenderer, Text.literal("§7Clic droit couleur | Clic renommer | ▲▼ reordonner | §c✗ §7supprimer"),
                gx + 10, gy + 20, COL_LABEL, false);
        ctx.fill(gx + 8, gy + 30, gx + GUI_W - 8, gy + 31, COL_SEPARATOR);

        // Liste des rangs
        int listX    = gx + 10;
        int listY    = gy + 36;
        int listH    = GUI_H - 36 - 36;
        int visRows  = listH / ROW_H;
        int max      = Math.min(ranks.size(), scrollOffset + visRows);

        for (int i = scrollOffset; i < max; i++) {
            RankState rs  = ranks.get(i);
            int rowY      = listY + (i - scrollOffset) * ROW_H;
            boolean editing = rs.id.equals(editingRankId);
            boolean rowHov  = !editing && mx >= listX && mx <= gx + GUI_W - 12
                    && my >= rowY && my <= rowY + ROW_H - 2;

            ctx.fill(listX, rowY, gx + GUI_W - 12, rowY + ROW_H - 2,
                    editing ? COL_ROW_SEL : (rowHov ? 0x55223344 : COL_ROW_BG));

            // Swatch couleur
            int swatchX = listX + 4;
            ctx.fill(swatchX, rowY + 5, swatchX + 12, rowY + ROW_H - 7, colorHex(rs.colorCode));
            ClanConfigScreen.drawBorder(ctx, swatchX, rowY + 5, 12, ROW_H - 12, 0xFF666666);

            // Nom (ou buffer d'edition)
            int nameX = swatchX + 18;
            if (editing) {
                ctx.fill(nameX, rowY + 4, nameX + 120, rowY + ROW_H - 4, 0xFF000022);
                ClanConfigScreen.drawBorder(ctx, nameX, rowY + 4, 120, ROW_H - 8, 0xFF4477FF);
                String displayed = editBuffer + (System.currentTimeMillis() % 1000 < 500 ? "§7|" : " ");
                ctx.drawText(textRenderer, Text.literal("§f" + displayed), nameX + 3, rowY + 8, 0xFFFFFF, false);
            } else {
                ctx.drawText(textRenderer, Text.literal(rs.colorCode + rs.name), nameX, rowY + 8, 0xFFFFFF, false);
                if (rs.isSystem) {
                    ctx.drawText(textRenderer, Text.literal("§8[sys]"), nameX + 100, rowY + 8, COL_LABEL, false);
                }
            }

            // Boutons droite (del, ▼, ▲)
            // rightEdge de la ligne
            int rightEdge = gx + GUI_W - 14;

            if (!rs.isSystem) {
                // Bouton supprimer (le plus a droite)
                int delX = rightEdge - DEL_W;
                int delY = rowY + 5;
                boolean delHov = isOverXY(mx, my, delX, delY, DEL_W, DEL_H);
                ctx.fill(delX, delY, delX + DEL_W, delY + DEL_H, COL_DEL_BG);
                ClanConfigScreen.drawBorder(ctx, delX, delY, DEL_W, DEL_H, delHov ? COL_DEL_HOV : COL_DEL_BDR);
                ctx.drawText(textRenderer, Text.literal("§cx"), delX + 4, delY + 3, 0xFFFFFF, false);

                // Bouton ▼ (juste avant suppr)
                int dnX = delX - ORD_W - 3;
                int dnY = rowY + 6;
                boolean dnHov = isOverXY(mx, my, dnX, dnY, ORD_W, ORD_H);
                renderOrdBtn(ctx, dnX, dnY, ORD_W, ORD_H, "▼", dnHov, i < ranks.size() - 1);

                // Bouton ▲ (juste avant ▼)
                int upX = dnX - ORD_W - 2;
                int upY = rowY + 6;
                boolean upHov = isOverXY(mx, my, upX, upY, ORD_W, ORD_H);
                renderOrdBtn(ctx, upX, upY, ORD_W, ORD_H, "▲", upHov, i > 0);
            }
        }

        // Bouton ajouter
        if (ranks.size() < ClanRank.MAX_RANKS) {
            renderBtn(ctx, btnAdd, mx, my, COL_ADD_BG, COL_ADD_BDR, COL_ADD_HOV);
        }

        // Bouton retour
        renderBtn(ctx, btnBack, mx, my, COL_CLOSE_BG, COL_CLOSE_BDR, COL_CLOSE_HOV);

        // Modal de confirmation suppression
        if (pendingDeleteId != null) {
            renderDeleteConfirm(ctx, mx, my);
        }

        super.render(ctx, mx, my, delta);
    }

    private void renderOrdBtn(DrawContext ctx, int x, int y, int w, int h, String label, boolean hov, boolean enabled) {
        int bg  = enabled ? (hov ? 0x55334455 : COL_BTN_BG) : 0x22111122;
        int bdr = enabled ? (hov ? COL_BTN_HOV : COL_BTN_BDR) : 0xFF222233;
        int col = enabled ? (hov ? 0xFFFFFF : 0xAAAAFF) : 0xFF555566;
        ctx.fill(x, y, x + w, y + h, bg);
        ClanConfigScreen.drawBorder(ctx, x, y, w, h, bdr);
        int tw = textRenderer.getWidth(label);
        ctx.drawText(textRenderer, Text.literal(label), x + (w - tw) / 2, y + (h - 8) / 2, col, false);
    }

    private void renderDeleteConfirm(DrawContext ctx, int mx, int my) {
        int cfmW = 180, cfmH = 56;
        int cfmX = gx + (GUI_W - cfmW) / 2;
        int cfmY = gy + (GUI_H - cfmH) / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 200);
        ctx.fill(cfmX, cfmY, cfmX + cfmW, cfmY + cfmH, 0xEE0A0A1A);
        ClanConfigScreen.drawBorder(ctx, cfmX, cfmY, cfmW, cfmH, COL_DEL_HOV);
        String label = "Supprimer ce rang ?";
        int tw = textRenderer.getWidth(label);
        ctx.drawText(textRenderer, Text.literal("§e" + label),
                cfmX + (cfmW - tw) / 2, cfmY + 10, 0xFFFFFF, true);
        ctx.drawText(textRenderer, Text.literal("§cIrreversible"),
                cfmX + (cfmW - textRenderer.getWidth("§cIrreversible")) / 2, cfmY + 22, 0xFFFFFF, false);
        ctx.getMatrices().pop();

        renderBtn(ctx, btnConfirmYes, mx, my, COL_ADD_BG,   COL_ADD_BDR,   COL_ADD_HOV);
        renderBtn(ctx, btnConfirmNo,  mx, my, COL_CLOSE_BG, COL_CLOSE_BDR, COL_CLOSE_HOV);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int imx = (int) mx;
        int imy = (int) my;

        // Modal suppression active
        if (pendingDeleteId != null) {
            if (ClanConfigScreen.isOver(btnConfirmYes, imx, imy)) {
                ClientPlayNetworking.send(new ClanActionPayload("rank_remove:" + pendingDeleteId));
                pendingDeleteId = null;
            } else if (ClanConfigScreen.isOver(btnConfirmNo, imx, imy)) {
                pendingDeleteId = null;
            }
            return true;
        }

        // Si on etait en train d'editer, confirmer
        if (editingRankId != null) {
            commitEdit();
        }

        // Bouton ajouter
        if (ranks.size() < ClanRank.MAX_RANKS && ClanConfigScreen.isOver(btnAdd, imx, imy)) {
            ClientPlayNetworking.send(new ClanActionPayload("rank_add:Nouveau rang:§7"));
            return true;
        }

        // Bouton retour
        if (ClanConfigScreen.isOver(btnBack, imx, imy)) {
            client.setScreen(parent);
            return true;
        }

        // Clics sur les rangs
        int listX   = gx + 10;
        int listY   = gy + 36;
        int listH   = GUI_H - 36 - 36;
        int visRows = listH / ROW_H;
        int max     = Math.min(ranks.size(), scrollOffset + visRows);

        for (int i = scrollOffset; i < max; i++) {
            RankState rs = ranks.get(i);
            int rowY = listY + (i - scrollOffset) * ROW_H;
            if (imy < rowY || imy >= rowY + ROW_H - 2) continue;

            if (!rs.isSystem) {
                int rightEdge = gx + GUI_W - 14;

                // Bouton supprimer
                int delX = rightEdge - DEL_W;
                int delY = rowY + 5;
                if (isOverXY(imx, imy, delX, delY, DEL_W, DEL_H)) {
                    pendingDeleteId = rs.id;
                    return true;
                }

                // Bouton ▼
                int dnX = delX - ORD_W - 3;
                int dnY = rowY + 6;
                if (isOverXY(imx, imy, dnX, dnY, ORD_W, ORD_H) && i < ranks.size() - 1) {
                    ClientPlayNetworking.send(new ClanActionPayload("rank_reorder:" + rs.id + ":down"));
                    return true;
                }

                // Bouton ▲
                int upX = dnX - ORD_W - 2;
                int upY = rowY + 6;
                if (isOverXY(imx, imy, upX, upY, ORD_W, ORD_H) && i > 0) {
                    ClientPlayNetworking.send(new ClanActionPayload("rank_reorder:" + rs.id + ":up"));
                    return true;
                }
            }

            // Clic droit sur swatch couleur → cycle couleur
            if (button == 1 && imx >= listX + 4 && imx <= listX + 16) {
                cycleColor(rs);
                return true;
            }

            // Clic gauche sur la ligne → renommer
            if (button == 0) {
                editingRankId = rs.id;
                editBuffer    = rs.name;
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        int visRows  = (GUI_H - 36 - 36) / ROW_H;
        int maxScroll = Math.max(0, ranks.size() - visRows);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vy));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingRankId != null) {
            if (editBuffer.length() < 24) editBuffer += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingRankId != null) {
            if (keyCode == 256) { // Escape
                editingRankId = null;
                editBuffer    = "";
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                commitEdit();
                return true;
            }
            if (keyCode == 259 && !editBuffer.isEmpty()) { // Backspace
                editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void commitEdit() {
        if (editingRankId != null && !editBuffer.isBlank()) {
            String id = editingRankId;
            for (RankState rs : ranks) {
                if (rs.id.equals(id)) {
                    rs.name = editBuffer;
                    break;
                }
            }
            ClientPlayNetworking.send(new ClanActionPayload("rank_rename:" + id + ":" + editBuffer));
        }
        editingRankId = null;
        editBuffer    = "";
    }

    private void cycleColor(RankState rs) {
        rs.colorIdx  = ClanRank.nextColorIndex(rs.colorIdx);
        rs.colorCode = ClanRank.PRESET_COLORS[rs.colorIdx];
        ClientPlayNetworking.send(new ClanActionPayload("rank_set_color:" + rs.id + ":" + rs.colorCode));
    }

    private int colorHex(String code) {
        return switch (code) {
            case "§c" -> 0xFFFF5555;
            case "§6" -> 0xFFFFAA00;
            case "§e" -> 0xFFFFFF55;
            case "§a" -> 0xFF55FF55;
            case "§b" -> 0xFF55FFFF;
            case "§9" -> 0xFF5555FF;
            case "§d" -> 0xFFFF55FF;
            case "§f" -> 0xFFFFFFFF;
            case "§7" -> 0xFFAAAAAA;
            case "§8" -> 0xFF555555;
            default   -> 0xFFAAAAAA;
        };
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

    private static boolean isOverXY(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}
}
