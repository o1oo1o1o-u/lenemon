package com.lenemon.client.clan.screen;

import com.lenemon.network.clan.ClanActionPayload;
import com.lenemon.network.clan.ClanGuiPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Ecran principal de configuration du clan (accessible owner only).
 * Hub avec des boutons vers les sous-ecrans de config.
 */
public class ClanConfigScreen extends Screen {

    private static final int GUI_W = 240;
    private static final int GUI_H = 220; // agrandi pour accueillir 3 boutons

    private static final int COL_BG        = 0xCC0A0A1A;
    private static final int COL_BORDER    = 0xFF2255AA;
    private static final int COL_SEPARATOR = 0x55FFFFFF;
    private static final int COL_LABEL     = 0xAAAAAA;
    private static final int COL_BTN_BG    = 0x55113355;
    private static final int COL_BTN_BDR   = 0xFF334466;
    private static final int COL_BTN_HOV   = 0xFF4477CC;
    private static final int COL_PERM_BG   = 0x55110033;
    private static final int COL_PERM_BDR  = 0xFF440066;
    private static final int COL_PERM_HOV  = 0xFF9933CC;
    private static final int COL_CLOSE_BG  = 0x55330000;
    private static final int COL_CLOSE_BDR = 0xFF443333;
    private static final int COL_CLOSE_HOV = 0xFFAA2222;

    private final ClanGuiPayload data;
    private int gx, gy;

    private BtnLayout btnBank;
    private BtnLayout btnRanks;
    private BtnLayout btnPerms;
    private BtnLayout btnBack;

    public ClanConfigScreen(ClanGuiPayload data) {
        super(Text.literal("Configuration - " + data.clanName()));
        this.data = data;
    }

    @Override
    protected void init() {
        gx = (width  - GUI_W) / 2;
        gy = (height - GUI_H) / 2;

        int bx = gx + (GUI_W - 160) / 2;
        int bw = 160;
        int bh = 24;

        btnBank  = new BtnLayout(bx, gy + 60,         bw, bh, "§e Banque du clan");
        btnRanks = new BtnLayout(bx, gy + 60 + 32,    bw, bh, "§b Gestion des rangs");
        btnPerms = new BtnLayout(bx, gy + 60 + 32 * 2, bw, bh, "§d Permissions");
        btnBack  = new BtnLayout(gx + GUI_W - 80, gy + GUI_H - 22, 70, 14, "§7◄ Retour");
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);
        drawBorder(ctx, gx, gy, GUI_W, GUI_H, COL_BORDER);

        // Titre
        String title = "§eConfiguration §8- §f" + data.clanName() + " §8[§b" + data.clanTag() + "§8]";
        ctx.drawText(textRenderer, Text.literal(title), gx + 10, gy + 10, 0xFFFFFF, true);
        ctx.fill(gx + 8, gy + 22, gx + GUI_W - 8, gy + 23, COL_SEPARATOR);

        ctx.drawText(textRenderer, Text.literal("§7Choisir une categorie :"), gx + 10, gy + 32, COL_LABEL, false);

        renderBtn(ctx, btnBank,  mx, my, COL_BTN_BG,  COL_BTN_BDR,  COL_BTN_HOV);
        renderBtn(ctx, btnRanks, mx, my, COL_BTN_BG,  COL_BTN_BDR,  COL_BTN_HOV);
        renderBtn(ctx, btnPerms, mx, my, COL_PERM_BG, COL_PERM_BDR, COL_PERM_HOV);
        renderBtn(ctx, btnBack,  mx, my, COL_CLOSE_BG, COL_CLOSE_BDR, COL_CLOSE_HOV);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int imx = (int) mx;
        int imy = (int) my;

        if (isOver(btnBank, imx, imy)) {
            client.setScreen(new ClanBankConfigScreen(data, this));
            return true;
        }
        if (isOver(btnRanks, imx, imy)) {
            client.setScreen(new ClanRankConfigScreen(data, this));
            return true;
        }
        if (isOver(btnPerms, imx, imy)) {
            client.setScreen(new ClanPermissionsScreen(data, this));
            return true;
        }
        if (isOver(btnBack, imx, imy)) {
            client.setScreen(new ClanHubScreen(data));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    // ── Helpers (statiques, reutilises par les sous-ecrans) ──────────────────

    private void renderBtn(DrawContext ctx, BtnLayout btn, int mx, int my,
                           int bgColor, int bdrColor, int hoverColor) {
        boolean hovered = isOver(btn, mx, my);
        ctx.fill(btn.x, btn.y, btn.x + btn.w, btn.y + btn.h, bgColor);
        drawBorder(ctx, btn.x, btn.y, btn.w, btn.h, hovered ? hoverColor : bdrColor);
        int tw = textRenderer.getWidth(btn.label);
        ctx.drawText(textRenderer, Text.literal(btn.label),
                btn.x + (btn.w - tw) / 2, btn.y + (btn.h - 8) / 2,
                hovered ? 0xFFFFFF : COL_LABEL, false);
    }

    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    public static boolean isOver(BtnLayout btn, int mx, int my) {
        return mx >= btn.x && mx <= btn.x + btn.w && my >= btn.y && my <= btn.y + btn.h;
    }

    public static class BtnLayout {
        final int x, y, w, h;
        final String label;
        BtnLayout(int x, int y, int w, int h, String label) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.label = label;
        }
    }
}
