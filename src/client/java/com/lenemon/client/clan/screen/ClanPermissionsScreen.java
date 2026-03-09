package com.lenemon.client.clan.screen;

import com.lenemon.network.clan.ClanActionPayload;
import com.lenemon.network.clan.ClanGuiPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ecran de configuration des permissions par rang minimum.
 * Chaque permission (kick, promote, demote) a un rang minimum requis.
 * Clic sur le rang affiché le fait cycler : owner → officer → member → owner...
 *
 * Accessible depuis ClanConfigScreen (owner uniquement).
 */
public class ClanPermissionsScreen extends Screen {

    // ── Dimensions ──────────────────────────────────────────────────────────
    private static final int GUI_W = 360;
    private static final int GUI_H = 260;
    private static final int ROW_H = 28;
    private static final int LIST_TOP = 44;
    private static final int LIST_BOTTOM = 208;

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
    private static final int COL_CLOSE_BG  = 0x55330000;
    private static final int COL_CLOSE_BDR = 0xFF443333;
    private static final int COL_CLOSE_HOV = 0xFFAA2222;
    private static final int COL_SCROLL_BG = 0x55224455;
    private static final int COL_SCROLL_BDR = 0xFF446688;
    private static final int COL_SCROLL_HOV = 0xFF66AAEE;

    // ── Etat ────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final ClanGuiPayload data;

    /** Cycle des rangs construit depuis le payload (triés par sortOrder). */
    private final java.util.List<ClanGuiPayload.RankDto> rankCycle = new ArrayList<>();

    private String nextRole(String current) {
        for (int i = 0; i < rankCycle.size(); i++) {
            if (rankCycle.get(i).id().equals(current)) {
                return rankCycle.get((i + 1) % rankCycle.size()).id();
            }
        }
        return rankCycle.isEmpty() ? "officer" : rankCycle.get(0).id();
    }

    private String formatRoleFromCycle(String rankId) {
        for (ClanGuiPayload.RankDto r : rankCycle) {
            if (r.id().equals(rankId)) return r.colorCode() + r.name();
        }
        return "§7" + rankId;
    }
    private int gx, gy;

    /**
     * Permissions locales : action -> roleId minimum requis.
     * Initialisees depuis le payload ou defaults.
     */
    private final Map<String, String> permissions = new LinkedHashMap<>();

    /** Descriptions des actions pour l'affichage. */
    private static final Map<String, String> ACTION_LABELS = new LinkedHashMap<>();
    static {
        ACTION_LABELS.put("kick",      "Kicker un membre");
        ACTION_LABELS.put("promote",   "Promouvoir un membre");
        ACTION_LABELS.put("demote",    "Retrograder un membre");
        ACTION_LABELS.put("claim",     "Claimer des chunks");
        ACTION_LABELS.put("buy_level", "Acheter un level");
        ACTION_LABELS.put("edit_enter_message", "Modifier msg entree");
        ACTION_LABELS.put("edit_leave_message", "Modifier msg sortie");
    }

    /** Layout des lignes pre-calcule. */
    private static class PermRow {
        String action;
        String label;
        int x, y, w, h;         // coordonnees de la carte
        int btnX, btnY, btnW, btnH; // coordonnees du bouton de role
    }
    private final List<PermRow> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private int visibleRowCount = 0;
    private int maxScrollOffset = 0;

    private ClanConfigScreen.BtnLayout btnBack;
    private ClanConfigScreen.BtnLayout btnScrollUp;
    private ClanConfigScreen.BtnLayout btnScrollDown;

    // ── Constructeur ─────────────────────────────────────────────────────────

    public ClanPermissionsScreen(ClanGuiPayload data, Screen parent) {
        this(data, parent, 0);
    }

    public ClanPermissionsScreen(ClanGuiPayload data, Screen parent, int initialScrollOffset) {
        super(Text.literal("Permissions - " + data.clanName()));
        this.data   = data;
        this.parent = parent;
        this.scrollOffset = Math.max(0, initialScrollOffset);

        // Construire le cycle depuis les rangs du payload (triés par sortOrder)
        rankCycle.clear();
        data.ranks().stream()
                .sorted(java.util.Comparator.comparingInt(ClanGuiPayload.RankDto::sortOrder))
                .forEach(rankCycle::add);

        // Lire depuis le payload, fallback sur defaults
        java.util.Map<String, String> payloadPerms = data.permissions();
        permissions.put("kick",      payloadPerms.getOrDefault("kick",      "officer"));
        permissions.put("promote",   payloadPerms.getOrDefault("promote",   "owner"));
        permissions.put("demote",    payloadPerms.getOrDefault("demote",    "owner"));
        permissions.put("claim",     payloadPerms.getOrDefault("claim",     "member"));
        permissions.put("buy_level", payloadPerms.getOrDefault("buy_level", "owner"));
        permissions.put("edit_enter_message", payloadPerms.getOrDefault("edit_enter_message", "owner"));
        permissions.put("edit_leave_message", payloadPerms.getOrDefault("edit_leave_message", "owner"));
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        gx = (width  - GUI_W) / 2;
        gy = (height - GUI_H) / 2;

        btnBack = new ClanConfigScreen.BtnLayout(gx + GUI_W - 80, gy + GUI_H - 22, 70, 14, "§7◄ Retour");
        btnScrollUp = new ClanConfigScreen.BtnLayout(gx + GUI_W - 18, gy + LIST_TOP, 14, 18, "§f▲");
        btnScrollDown = new ClanConfigScreen.BtnLayout(gx + GUI_W - 18, gy + LIST_BOTTOM - 18, 14, 18, "§f▼");

        rows.clear();
        int listX = gx + 10;
        int listW = GUI_W - 20;
        visibleRowCount = Math.max(1, (LIST_BOTTOM - LIST_TOP) / (ROW_H + 4));
        maxScrollOffset = Math.max(0, ACTION_LABELS.size() - visibleRowCount);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));

        int i = 0;
        for (Map.Entry<String, String> entry : ACTION_LABELS.entrySet()) {
            PermRow row = new PermRow();
            row.action = entry.getKey();
            row.label  = entry.getValue();
            row.x = listX;
            row.y = 0;
            row.w = listW - (maxScrollOffset > 0 ? 18 : 0);
            row.h = ROW_H;

            // Bouton role : a droite de la ligne
            row.btnW = 80;
            row.btnH = ROW_H - 8;
            row.btnX = row.x + row.w - row.btnW - 4;
            row.btnY = row.y + 4;

            rows.add(row);
            i++;
        }
    }

    // ── Render ───────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);
        ClanConfigScreen.drawBorder(ctx, gx, gy, GUI_W, GUI_H, COL_BORDER);

        // Titre
        ctx.drawText(textRenderer, Text.literal("§dPermissions du clan"),
                gx + 10, gy + 8, 0xFFFFFF, true);
        ctx.drawText(textRenderer, Text.literal("§7Clic sur le rang pour le faire cycler"),
                gx + 10, gy + 20, COL_LABEL, false);
        ctx.fill(gx + 8, gy + 32, gx + GUI_W - 8, gy + 33, COL_SEPARATOR);

        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(rows.size(), visibleStart + visibleRowCount);
        int baseY = gy + LIST_TOP;
        for (int i = visibleStart; i < visibleEnd; i++) {
            PermRow row = rows.get(i);
            row.y = baseY + (i - visibleStart) * (ROW_H + 4);
            row.btnX = row.x + row.w - row.btnW - 4;
            row.btnY = row.y + 4;
            String currentRole = permissions.getOrDefault(row.action, "officer");

            boolean rowHov = mx >= row.x && mx <= row.x + row.w && my >= row.y && my <= row.y + row.h;
            ctx.fill(row.x, row.y, row.x + row.w, row.y + row.h,
                    rowHov ? 0x55223344 : COL_ROW_BG);
            ClanConfigScreen.drawBorder(ctx, row.x, row.y, row.w, row.h, COL_ROW_BDR);

            // Nom de la permission
            ctx.drawText(textRenderer, Text.literal("§f" + row.label),
                    row.x + 6, row.y + (ROW_H - 8) / 2, 0xFFFFFF, false);

            // Bouton rang (cliquable)
            boolean btnHov = mx >= row.btnX && mx <= row.btnX + row.btnW
                    && my >= row.btnY && my <= row.btnY + row.btnH;
            ctx.fill(row.btnX, row.btnY, row.btnX + row.btnW, row.btnY + row.btnH,
                    btnHov ? 0x55334455 : COL_BTN_BG);
            ClanConfigScreen.drawBorder(ctx, row.btnX, row.btnY, row.btnW, row.btnH,
                    btnHov ? COL_BTN_HOV : COL_BTN_BDR);

            String roleDisplay = formatRoleFromCycle(currentRole) + " §8▼";
            int tw = textRenderer.getWidth(roleDisplay);
            ctx.drawText(textRenderer, Text.literal(roleDisplay),
                    row.btnX + (row.btnW - tw) / 2, row.btnY + (row.btnH - 8) / 2,
                    btnHov ? 0xFFFFFF : COL_LABEL, false);
        }

        if (maxScrollOffset > 0) {
            renderBtn(ctx, btnScrollUp, mx, my, COL_SCROLL_BG, COL_SCROLL_BDR, COL_SCROLL_HOV);
            renderBtn(ctx, btnScrollDown, mx, my, COL_SCROLL_BG, COL_SCROLL_BDR, COL_SCROLL_HOV);
            String page = "§7" + (visibleStart + 1) + "-" + visibleEnd + " / " + rows.size();
            int tw = textRenderer.getWidth(page);
            ctx.drawText(textRenderer, Text.literal(page),
                    gx + GUI_W - tw - 22, gy + 20, COL_LABEL, false);
        }

        // Bouton retour
        renderBtn(ctx, btnBack, mx, my, COL_CLOSE_BG, COL_CLOSE_BDR, COL_CLOSE_HOV);

        super.render(ctx, mx, my, delta);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int imx = (int) mx, imy = (int) my;

        if (ClanConfigScreen.isOver(btnBack, imx, imy)) {
            client.setScreen(parent);
            return true;
        }

        if (maxScrollOffset > 0 && ClanConfigScreen.isOver(btnScrollUp, imx, imy)) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }
        if (maxScrollOffset > 0 && ClanConfigScreen.isOver(btnScrollDown, imx, imy)) {
            scrollOffset = Math.min(maxScrollOffset, scrollOffset + 1);
            return true;
        }

        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(rows.size(), visibleStart + visibleRowCount);
        for (int i = visibleStart; i < visibleEnd; i++) {
            PermRow row = rows.get(i);
            if (imx >= row.btnX && imx <= row.btnX + row.btnW
                    && imy >= row.btnY && imy <= row.btnY + row.btnH) {
                // Cycler le role
                String current = permissions.getOrDefault(row.action, "officer");
                String next    = nextRole(current);
                permissions.put(row.action, next);
                ClientPlayNetworking.send(new ClanActionPayload("perm_set:" + row.action + ":" + next));
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
        if (maxScrollOffset <= 0) return super.mouseScrolled(mx, my, horizontalAmount, verticalAmount);
        if (verticalAmount < 0) {
            scrollOffset = Math.min(maxScrollOffset, scrollOffset + 1);
            return true;
        }
        if (verticalAmount > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }
        return super.mouseScrolled(mx, my, horizontalAmount, verticalAmount);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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

    public int getScrollOffset() {
        return scrollOffset;
    }

    // ── Screen ───────────────────────────────────────────────────────────────

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}
}
