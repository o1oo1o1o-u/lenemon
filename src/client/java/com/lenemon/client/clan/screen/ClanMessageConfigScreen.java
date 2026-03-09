package com.lenemon.client.clan.screen;

import com.lenemon.network.clan.ClanActionPayload;
import com.lenemon.network.clan.ClanGuiPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ClanMessageConfigScreen extends Screen {

    private static final int GUI_W = 360;
    private static final int GUI_H = 220;

    private static final int COL_BG        = 0xCC0A0A1A;
    private static final int COL_BORDER    = 0xFF2255AA;
    private static final int COL_SEPARATOR = 0x55FFFFFF;
    private static final int COL_LABEL     = 0xAAAAAA;
    private static final int COL_INPUT_BG  = 0xCC101826;
    private static final int COL_BTN_BG    = 0x55113355;
    private static final int COL_BTN_BDR   = 0xFF334466;
    private static final int COL_BTN_HOV   = 0xFF4477CC;
    private static final int COL_SAVE_BG   = 0x55002200;
    private static final int COL_SAVE_BDR  = 0xFF225522;
    private static final int COL_SAVE_HOV  = 0xFF44AA44;
    private static final int COL_CLOSE_BG  = 0x55330000;
    private static final int COL_CLOSE_BDR = 0xFF443333;
    private static final int COL_CLOSE_HOV = 0xFFAA2222;

    private final ClanGuiPayload data;
    private final Screen parent;
    private final boolean canEditEnter;
    private final boolean canEditLeave;

    private int gx, gy;
    private TextFieldWidget enterField;
    private TextFieldWidget leaveField;
    private ClanConfigScreen.BtnLayout btnSaveEnter;
    private ClanConfigScreen.BtnLayout btnSaveLeave;
    private ClanConfigScreen.BtnLayout btnBack;

    public ClanMessageConfigScreen(ClanGuiPayload data, Screen parent) {
        super(Text.literal("Messages - " + data.clanName()));
        this.data = data;
        this.parent = parent;
        this.canEditEnter = canManage(data, "edit_enter_message");
        this.canEditLeave = canManage(data, "edit_leave_message");
    }

    @Override
    protected void init() {
        gx = (width - GUI_W) / 2;
        gy = (height - GUI_H) / 2;

        enterField = new TextFieldWidget(textRenderer, gx + 16, gy + 62, 250, 20, Text.literal("Message d'entree"));
        enterField.setMaxLength(120);
        enterField.setText(data.enterMessage());
        enterField.setEditable(canEditEnter);
        addDrawableChild(enterField);

        leaveField = new TextFieldWidget(textRenderer, gx + 16, gy + 126, 250, 20, Text.literal("Message de sortie"));
        leaveField.setMaxLength(120);
        leaveField.setText(data.leaveMessage());
        leaveField.setEditable(canEditLeave);
        addDrawableChild(leaveField);

        btnSaveEnter = new ClanConfigScreen.BtnLayout(gx + 276, gy + 62, 68, 20, "§aSauver");
        btnSaveLeave = new ClanConfigScreen.BtnLayout(gx + 276, gy + 126, 68, 20, "§aSauver");
        btnBack = new ClanConfigScreen.BtnLayout(gx + GUI_W - 80, gy + GUI_H - 22, 70, 14, "§7◄ Retour");
        setInitialFocus(canEditEnter ? enterField : (canEditLeave ? leaveField : null));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);
        ClanConfigScreen.drawBorder(ctx, gx, gy, GUI_W, GUI_H, COL_BORDER);

        ctx.drawText(textRenderer, Text.literal("§dMessages de territoire"),
                gx + 10, gy + 8, 0xFFFFFF, true);
        ctx.drawText(textRenderer, Text.literal("§7Placeholders : {clan}, {tag} | Couleurs : &a &6 &l ..."),
                gx + 10, gy + 20, COL_LABEL, false);
        ctx.fill(gx + 8, gy + 32, gx + GUI_W - 8, gy + 33, COL_SEPARATOR);

        ctx.drawText(textRenderer, Text.literal("§fMessage d'entree"),
                gx + 16, gy + 48, 0xFFFFFF, false);
        ctx.fill(gx + 14, gy + 60, gx + 268, gy + 84, COL_INPUT_BG);
        enterField.render(ctx, mx, my, delta);
        renderBtn(ctx, btnSaveEnter, mx, my,
                canEditEnter ? COL_SAVE_BG : COL_BTN_BG,
                canEditEnter ? COL_SAVE_BDR : COL_BTN_BDR,
                canEditEnter ? COL_SAVE_HOV : COL_BTN_BDR);
        if (!canEditEnter) {
            ctx.drawText(textRenderer, Text.literal("§8Permission insuffisante"),
                    gx + 16, gy + 88, COL_LABEL, false);
        }

        ctx.drawText(textRenderer, Text.literal("§fMessage de sortie"),
                gx + 16, gy + 112, 0xFFFFFF, false);
        ctx.fill(gx + 14, gy + 124, gx + 268, gy + 148, COL_INPUT_BG);
        leaveField.render(ctx, mx, my, delta);
        renderBtn(ctx, btnSaveLeave, mx, my,
                canEditLeave ? COL_SAVE_BG : COL_BTN_BG,
                canEditLeave ? COL_SAVE_BDR : COL_BTN_BDR,
                canEditLeave ? COL_SAVE_HOV : COL_BTN_BDR);
        if (!canEditLeave) {
            ctx.drawText(textRenderer, Text.literal("§8Permission insuffisante"),
                    gx + 16, gy + 152, COL_LABEL, false);
        }

        ctx.drawText(textRenderer, Text.literal("§7Apercu entree :"), gx + 16, gy + 170, COL_LABEL, false);
        ctx.drawText(textRenderer, Text.literal(preview(enterField.getText())), gx + 104, gy + 170, 0xFFFFFF, false);
        ctx.drawText(textRenderer, Text.literal("§7Apercu sortie :"), gx + 16, gy + 184, COL_LABEL, false);
        ctx.drawText(textRenderer, Text.literal(preview(leaveField.getText())), gx + 104, gy + 184, 0xFFFFFF, false);

        renderBtn(ctx, btnBack, mx, my, COL_CLOSE_BG, COL_CLOSE_BDR, COL_CLOSE_HOV);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int imx = (int) mx;
        int imy = (int) my;
        if (ClanConfigScreen.isOver(btnBack, imx, imy)) {
            client.setScreen(parent);
            return true;
        }
        if (canEditEnter && ClanConfigScreen.isOver(btnSaveEnter, imx, imy)) {
            ClientPlayNetworking.send(new ClanActionPayload("set_enter_message:" + enterField.getText()));
            return true;
        }
        if (canEditLeave && ClanConfigScreen.isOver(btnSaveLeave, imx, imy)) {
            ClientPlayNetworking.send(new ClanActionPayload("set_leave_message:" + leaveField.getText()));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

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

    static boolean canManage(ClanGuiPayload data, String action) {
        String requiredRankId = data.permissions().getOrDefault(action, "owner");
        int viewerOrder = rankOrder(data, data.viewerRankId());
        int requiredOrder = rankOrder(data, requiredRankId);
        return viewerOrder <= requiredOrder;
    }

    private static int rankOrder(ClanGuiPayload data, String rankId) {
        for (ClanGuiPayload.RankDto rank : data.ranks()) {
            if (rank.id().equals(rankId)) return rank.sortOrder();
        }
        return Integer.MAX_VALUE;
    }

    private String preview(String text) {
        return translateColorCodes(
                text.replace("{clan}", data.clanName()).replace("{tag}", data.clanTag())
        );
    }

    private static String translateColorCodes(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '&' && i + 1 < value.length()) {
                char next = Character.toLowerCase(value.charAt(i + 1));
                if ((next >= '0' && next <= '9')
                        || (next >= 'a' && next <= 'f')
                        || (next >= 'k' && next <= 'o')
                        || next == 'r') {
                    out.append('§').append(next);
                    i++;
                    continue;
                }
            }
            out.append(current);
        }
        return out.toString();
    }
}
