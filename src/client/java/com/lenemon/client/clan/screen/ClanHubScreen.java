package com.lenemon.client.clan.screen;

import com.lenemon.network.clan.ClanActionPayload;
import com.lenemon.network.clan.ClanGuiPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Ecran principal de gestion du clan.
 * Layout : en-tete XP | membres (gauche) | infos + actions (droite)
 */
public class ClanHubScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_W         = 360;
    private static final int GUI_H         = 280;
    private static final int MEMBER_PANEL_W = 160;
    private static final int MEMBER_ROW_H  = 18;

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
    private static final int COL_XP_BAR_BG  = 0x55334455;
    private static final int COL_XP_BAR_FG  = 0xFF44AAFF;
    private static final int COL_OWNER      = 0xFFFF5555;
    private static final int COL_OFFICER    = 0xFFFFAA00;
    private static final int COL_MEMBER_C   = 0xFFAAAAAA;
    private static final int COL_ACTION_BG  = 0x55003300;
    private static final int COL_ACTION_BDR = 0xFF225522;
    private static final int COL_ACTION_HOV = 0xFF44AA44;
    private static final int COL_CFG_BG     = 0x55003344;
    private static final int COL_CFG_BDR    = 0xFF004466;
    private static final int COL_CFG_HOV    = 0xFF0077BB;

    // ── Donnees ───────────────────────────────────────────────────────────────
    private final ClanGuiPayload data;
    private final boolean isOwner;
    private final boolean isOfficer;

    // ── Layout ────────────────────────────────────────────────────────────────
    private int gx, gy;
    private int memberPanelX, memberPanelY;
    private final List<MemberRow> memberRows = new ArrayList<>();

    // Boutons
    private Btn btnConfig, btnLeave, btnDisband, btnClose;
    private Btn btnConfirmYes, btnConfirmNo;
    private Btn btnBuyLevel;

    // Confirmation
    private String pendingAction = null;
    private String pendingLabel  = null;

    // Membre survole
    private MemberRow hoveredMember = null;

    // ── Constructeur ─────────────────────────────────────────────────────────

    public ClanHubScreen(ClanGuiPayload data) {
        super(Text.literal("Clan : " + data.clanName()));
        this.data      = data;
        this.isOwner   = "OWNER".equals(data.viewerRole());
        this.isOfficer = "OFFICER".equals(data.viewerRole()) || isOwner;
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        gx = (width  - GUI_W) / 2;
        gy = (height - GUI_H) / 2;

        memberPanelX = gx + 10;
        memberPanelY = gy + 64;

        memberRows.clear();
        int ry = memberPanelY + 4;
        for (ClanGuiPayload.MemberDto m : data.members()) {
            memberRows.add(new MemberRow(m, memberPanelX + 4, ry, MEMBER_PANEL_W - 8, MEMBER_ROW_H - 2));
            ry += MEMBER_ROW_H;
        }

        // Bouton achat level (owner uniquement, panel droit)
        int rpX0 = gx + MEMBER_PANEL_W + 20;
        int rpW0 = GUI_W - MEMBER_PANEL_W - 30;
        // Libelle dynamique selon disponibilite
        String buyLevelLabel;
        if (data.nextLevelPrice() <= 0) {
            buyLevelLabel = "§7Level max atteint";
        } else {
            buyLevelLabel = "§a▲ Acheter Level §7(§e" + data.nextLevelPrice() + " coins§7)";
        }
        btnBuyLevel = new Btn(rpX0, gy + 48 + 96, rpW0, 16, buyLevelLabel);

        // Boutons droit
        int bx = gx + MEMBER_PANEL_W + 20;
        int bw = GUI_W - MEMBER_PANEL_W - 30;
        int bh = 16;
        int by = gy + GUI_H - 80;

        btnConfig  = new Btn(bx, by,           bw, bh, "§b Configuration");
        btnLeave   = new Btn(bx, by + bh + 4,  bw, bh, "Quitter le clan");
        btnDisband = new Btn(bx, by + (bh+4)*2, bw, bh, "Dissoudre le clan");
        btnClose   = new Btn(gx + GUI_W - 70, gy + GUI_H - 22, 60, 14, "Fermer");

        // Confirmation modal (centree)
        int cfmW = 180, cfmH = 60;
        int cfmX = gx + (GUI_W - cfmW) / 2;
        int cfmY = gy + (GUI_H - cfmH) / 2;
        btnConfirmYes = new Btn(cfmX + 8,          cfmY + cfmH - 22, 76, 14, "§aOui");
        btnConfirmNo  = new Btn(cfmX + cfmW - 84,  cfmY + cfmH - 22, 76, 14, "§cAnnuler");
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Fond + bordure
        ctx.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);
        drawBorder(ctx, gx, gy, GUI_W, GUI_H, COL_BORDER);

        // En-tete
        ctx.drawText(textRenderer,
                Text.literal("§e" + data.clanName() + " §8[§b" + data.clanTag() + "§8]"),
                gx + 10, gy + 8, 0xFFFFFF, true);
        ctx.drawText(textRenderer,
                Text.literal("Prestige §e" + data.level() + " §7| XP §e" + data.xp() + "§7/§e" + data.xpNextLevel()),
                gx + 10, gy + 20, COL_LABEL, false);

        // Barre XP
        int xbX = gx + 10, xbY = gy + 32, xbW = GUI_W - 20, xbH = 6;
        ctx.fill(xbX, xbY, xbX + xbW, xbY + xbH, COL_XP_BAR_BG);
        float xpR = data.xpNextLevel() > 0 ? Math.min(1f, (float) data.xp() / data.xpNextLevel()) : 0f;
        ctx.fill(xbX, xbY, xbX + (int)(xbW * xpR), xbY + xbH, COL_XP_BAR_FG);
        drawBorder(ctx, xbX, xbY, xbW, xbH, COL_CARD_BDR);
        ctx.fill(gx + 8, gy + 42, gx + GUI_W - 8, gy + 43, COL_SEPARATOR);

        // Panel membres
        ctx.drawText(textRenderer,
                Text.literal("§7Membres (" + data.members().size() + ")"),
                memberPanelX, memberPanelY - 10, COL_LABEL, false);

        hoveredMember = null;
        for (MemberRow row : memberRows) {
            boolean hov = mx >= row.x && mx <= row.x + row.w && my >= row.y && my <= row.y + row.h;
            if (hov) hoveredMember = row;
            ctx.fill(row.x, row.y, row.x + row.w, row.y + row.h, hov ? COL_CARD_HOVER : COL_CARD_BG);
            drawBorder(ctx, row.x, row.y, row.w, row.h, hov ? COL_CARD_HOVER : COL_CARD_BDR);

            String rankColor = getRankColor(row.dto.rankId());
            String rolePrefix = switch (row.dto.role()) {
                case "OWNER"   -> rankColor + "[O] ";
                case "OFFICER" -> rankColor + "[+] ";
                default        -> rankColor + "[-] ";
            };
            ctx.drawText(textRenderer,
                    Text.literal(rolePrefix + "§f" + row.dto.name()),
                    row.x + 3, row.y + 4, 0xFFFFFF, false);
        }

        // Separateur vertical
        ctx.fill(gx + MEMBER_PANEL_W + 12, gy + 48, gx + MEMBER_PANEL_W + 13, gy + GUI_H - 26, COL_SEPARATOR);

        // Panel droit
        int rpX = gx + MEMBER_PANEL_W + 20;
        int rpY = gy + 48;

        ctx.drawText(textRenderer, Text.literal("§7Banque du clan"), rpX, rpY, COL_LABEL, false);
        ctx.drawText(textRenderer, Text.literal("§e" + data.bankBalance() + " §7coins"), rpX, rpY + 12, 0xFFFFFF, false);
        ctx.drawText(textRenderer,
                Text.literal("§7Membres : §e" + data.members().size() + " / " + maxMembers()),
                rpX, rpY + 28, COL_LABEL, false);
        ctx.drawText(textRenderer,
                Text.literal("§7Cree : §f" + formatDate(data.createdAt())),
                rpX, rpY + 40, COL_LABEL, false);

        // Rangs systeme visibles (info limite du viewer)
        ClanGuiPayload.RankDto viewerRank = getViewerRank();
        if (viewerRank != null) {
            String limStr = viewerRank.withdrawLimit() == -1 ? "§aIllimite" : (viewerRank.withdrawLimit() == 0 ? "§cAucun retrait" : "§e" + viewerRank.withdrawLimit() + " §7coins/j");
            ctx.drawText(textRenderer,
                    Text.literal("§7Ton retrait max : " + limStr),
                    rpX, rpY + 54, COL_LABEL, false);
        }

        // Level economique + Chunks
        ctx.drawText(textRenderer,
                Text.literal("§7Level : §e" + data.clanLevel()),
                rpX, rpY + 68, COL_LABEL, false);
        ctx.drawText(textRenderer,
                Text.literal("§7Chunks : §e" + data.usedClaims() + "§7/§e" + data.maxClaims()),
                rpX, rpY + 80, COL_LABEL, false);

        // Bouton achat level (owner uniquement)
        if (isOwner) {
            boolean canBuy = data.nextLevelPrice() > 0;
            int buyBg  = canBuy ? 0x55002200 : 0x55222222;
            int buyBdr = canBuy ? 0xFF224422 : 0xFF444444;
            int buyHov = canBuy ? 0xFF44AA44 : 0xFF555555;
            renderBtn(ctx, btnBuyLevel, canBuy ? mx : -1, canBuy ? my : -1, buyBg, buyBdr, buyHov);
        }

        // Boutons
        if (isOwner) renderBtn(ctx, btnConfig,  mx, my, COL_CFG_BG,    COL_CFG_BDR,    COL_CFG_HOV);
        renderBtn(ctx, btnLeave,   mx, my, COL_ACTION_BG, COL_ACTION_BDR, COL_ACTION_HOV);
        if (isOwner) renderBtn(ctx, btnDisband, mx, my, COL_CLOSE_BG,  COL_CLOSE_BDR,  COL_CLOSE_HOV);
        renderBtn(ctx, btnClose,   mx, my, COL_CLOSE_BG,  COL_CLOSE_BDR,  COL_CLOSE_HOV);

        // Tooltip membre
        if (hoveredMember != null && isOfficer && pendingAction == null) {
            renderMemberTooltip(ctx, mx, my, hoveredMember);
        }

        // Modal confirmation (par-dessus tout)
        if (pendingAction != null) {
            renderConfirmModal(ctx, mx, my);
        }

        super.render(ctx, mx, my, delta);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int imx = (int) mx, imy = (int) my;

        // Modal confirmation active — absorber tous les clics
        if (pendingAction != null) {
            if (isOver(btnConfirmYes, imx, imy)) {
                String act = pendingAction;
                pendingAction = null;
                pendingLabel  = null;
                sendAction(act);
            } else if (isOver(btnConfirmNo, imx, imy)) {
                pendingAction = null;
                pendingLabel  = null;
            }
            return true;
        }

        if (isOver(btnClose, imx, imy)) { this.close(); return true; }

        if (isOwner && isOver(btnConfig, imx, imy)) {
            client.setScreen(new ClanConfigScreen(data));
            return true;
        }
        if (isOwner && data.nextLevelPrice() > 0 && isOver(btnBuyLevel, imx, imy)) {
            ClientPlayNetworking.send(new ClanActionPayload("buy_level"));
            return true;
        }
        if (isOver(btnLeave, imx, imy)) {
            pendingAction = "leave";
            pendingLabel  = "Quitter le clan ?";
            return true;
        }
        if (isOwner && isOver(btnDisband, imx, imy)) {
            pendingAction = "disband";
            pendingLabel  = "Dissoudre le clan ?";
            return true;
        }

        // Clic sur un membre : ouvrir l'ecran de profil
        if (hoveredMember != null && isOfficer) {
            client.setScreen(new ClanMemberScreen(data, hoveredMember.dto));
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    // ── Helpers render ────────────────────────────────────────────────────────

    private void renderConfirmModal(DrawContext ctx, int mx, int my) {
        int cfmW = 180, cfmH = 60;
        int cfmX = gx + (GUI_W - cfmW) / 2;
        int cfmY = gy + (GUI_H - cfmH) / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 200);

        ctx.fill(cfmX, cfmY, cfmX + cfmW, cfmY + cfmH, 0xEE0A0A1A);
        drawBorder(ctx, cfmX, cfmY, cfmW, cfmH, COL_CLOSE_HOV);

        String lbl = pendingLabel != null ? pendingLabel : "Confirmer ?";
        int tw = textRenderer.getWidth(lbl);
        ctx.drawText(textRenderer, Text.literal("§e" + lbl),
                cfmX + (cfmW - tw) / 2, cfmY + 12, 0xFFFFFF, true);

        // Boutons INSIDE push/pop (Z=200) pour etre au-dessus du GUI
        renderBtn(ctx, btnConfirmYes, mx, my, 0x55003300, 0xFF225522, 0xFF44AA44);
        renderBtn(ctx, btnConfirmNo,  mx, my, COL_CLOSE_BG, COL_CLOSE_BDR, COL_CLOSE_HOV);

        ctx.getMatrices().pop();
    }

    private void renderMemberTooltip(DrawContext ctx, int mx, int my, MemberRow row) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("§f" + row.dto.name()));
        String rankDisplay = getRankDisplay(row.dto.rankId());
        lines.add(Text.literal("§7Rang : " + rankDisplay));
        lines.add(Text.literal("§eClic §7: Voir le profil"));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400);
        ctx.drawTooltip(textRenderer, lines, mx, my);
        ctx.getMatrices().pop();
    }

    /** Retourne le nom+couleur du rang a partir de son ID via data.ranks(). */
    private String getRankDisplay(String rankId) {
        for (ClanGuiPayload.RankDto r : data.ranks()) {
            if (r.id().equals(rankId)) {
                return colorCodeToFormatting(r.colorCode()) + r.name();
            }
        }
        // Fallback systeme
        return switch (rankId) {
            case "owner"   -> "§cProprietaire";
            case "officer" -> "§6Officer";
            default        -> "§7Membre";
        };
    }

    /** Retourne uniquement le code couleur Minecraft du rang (pour prefixe liste). */
    private String getRankColor(String rankId) {
        for (ClanGuiPayload.RankDto r : data.ranks()) {
            if (r.id().equals(rankId)) {
                return colorCodeToFormatting(r.colorCode());
            }
        }
        return switch (rankId) {
            case "owner"   -> "§c";
            case "officer" -> "§6";
            default        -> "§7";
        };
    }

    /**
     * Convertit un colorCode hex (#RRGGBB) ou code §X en code de formatage Minecraft.
     * Si le colorCode est deja un code §X on le retourne directement.
     */
    private static String colorCodeToFormatting(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) return "§f";
        if (colorCode.startsWith("§")) return colorCode;
        // Les codes couleur custom sont stockes comme "§c", "§6", etc.
        // Si c'est un char seul c'est un code legacy
        if (colorCode.length() == 1) return "§" + colorCode;
        return colorCode; // retourne tel quel (peut etre §X deja)
    }

    private void renderBtn(DrawContext ctx, Btn btn, int mx, int my,
                           int bgColor, int bdrColor, int hoverColor) {
        boolean hov = isOver(btn, mx, my);
        ctx.fill(btn.x, btn.y, btn.x + btn.w, btn.y + btn.h, bgColor);
        drawBorder(ctx, btn.x, btn.y, btn.w, btn.h, hov ? hoverColor : bdrColor);
        int tw = textRenderer.getWidth(btn.label);
        ctx.drawText(textRenderer, Text.literal(btn.label),
                btn.x + (btn.w - tw) / 2, btn.y + (btn.h - 8) / 2,
                hov ? 0xFFFFFF : COL_LABEL, false);
    }

    static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w, y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w, y + h,     color);
        ctx.fill(x,         y,         x + 1, y + h,     color);
        ctx.fill(x + w - 1, y,         x + w, y + h,     color);
    }

    static boolean isOver(Btn btn, int mx, int my) {
        return mx >= btn.x && mx <= btn.x + btn.w && my >= btn.y && my <= btn.y + btn.h;
    }

    private void sendAction(String action) {
        ClientPlayNetworking.send(new ClanActionPayload(action));
        if (action.equals("leave") || action.equals("disband")) this.close();
    }

    private int maxMembers() {
        return 10 + 5 * (data.level() - 1);
    }

    private ClanGuiPayload.RankDto getViewerRank() {
        String sysId = switch (data.viewerRole()) {
            case "OWNER"   -> "owner";
            case "OFFICER" -> "officer";
            default        -> "member";
        };
        for (ClanGuiPayload.RankDto r : data.ranks()) {
            if (r.id().equals(sysId)) return r;
        }
        return null;
    }

    private static String formatDate(long ts) {
        return new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(ts));
    }

    // ── DTOs internes ─────────────────────────────────────────────────────────

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    private static class MemberRow {
        final ClanGuiPayload.MemberDto dto;
        final int x, y, w, h;
        MemberRow(ClanGuiPayload.MemberDto dto, int x, int y, int w, int h) {
            this.dto = dto; this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    static class Btn {
        final int x, y, w, h;
        final String label;
        Btn(int x, int y, int w, int h, String label) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.label = label;
        }
    }
}
