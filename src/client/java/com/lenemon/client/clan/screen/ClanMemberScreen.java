package com.lenemon.client.clan.screen;

import com.lenemon.network.clan.ClanActionPayload;
import com.lenemon.network.clan.ClanGuiPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Ecran de profil d'un membre du clan.
 * Affiche les infos du joueur (rang, contributions, statut) et les boutons d'action.
 * Acces depuis ClanHubScreen en cliquant sur un membre.
 */
public class ClanMemberScreen extends Screen {

    // ── Dimensions ──────────────────────────────────────────────────────────
    private static final int GUI_W = 280;
    private static final int GUI_H = 220;

    // ── Palette ─────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xCC0A0A1A;
    private static final int COL_BORDER     = 0xFF2255AA;
    private static final int COL_SEPARATOR  = 0x55FFFFFF;
    private static final int COL_LABEL      = 0xAAAAAA;
    private static final int COL_CARD_BG    = 0x55113355;
    private static final int COL_CARD_BDR   = 0xFF334466;
    private static final int COL_ACTION_BG  = 0x55003300;
    private static final int COL_ACTION_BDR = 0xFF225522;
    private static final int COL_ACTION_HOV = 0xFF44AA44;
    private static final int COL_DEMOTE_BG  = 0x55332200;
    private static final int COL_DEMOTE_BDR = 0xFF554422;
    private static final int COL_DEMOTE_HOV = 0xFFAA6600;
    private static final int COL_KICK_BG    = 0x55330000;
    private static final int COL_KICK_BDR   = 0xFF443333;
    private static final int COL_KICK_HOV   = 0xFFAA2222;
    private static final int COL_BACK_BG    = 0x55222233;
    private static final int COL_BACK_BDR   = 0xFF334466;
    private static final int COL_BACK_HOV   = 0xFF4477CC;
    private static final int COL_DISABLED   = 0x55333333;
    private static final int COL_DISABLED_T = 0xFF555566;

    // ── Etat ────────────────────────────────────────────────────────────────
    private final ClanGuiPayload data;
    private final ClanGuiPayload.MemberDto member;
    private final boolean viewerIsOwner;
    private final boolean viewerIsOfficer;
    private final boolean viewerHasOwnerPrivileges;

    private int gx, gy;

    // Boutons d'action
    private Btn btnPromote;
    private Btn btnDemote;
    private Btn btnKick;
    private Btn btnBack;

    // Confirmation en attente
    private String  pendingAction          = null;
    private String  pendingLabel           = null;
    private boolean pendingIsOwnerTransfer = false;
    private Btn btnConfirmYes;
    private Btn btnConfirmNo;

    // ── Constructeur ─────────────────────────────────────────────────────────

    public ClanMemberScreen(ClanGuiPayload data, ClanGuiPayload.MemberDto member) {
        super(Text.literal("Profil - " + member.name()));
        this.data           = data;
        this.member         = member;
        this.viewerIsOwner  = "OWNER".equals(data.viewerRole());
        this.viewerIsOfficer = "OFFICER".equals(data.viewerRole()) || viewerIsOwner;
        this.viewerHasOwnerPrivileges = viewerIsOwner || data.ranks().stream()
                .anyMatch(r -> r.id().equals(data.viewerRankId()) && r.ownerPrivileges());
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        gx = (width  - GUI_W) / 2;
        gy = (height - GUI_H) / 2;

        int bx = gx + 10;
        int bw = GUI_W - 20;
        int bh = 18;

        // Boutons d'action empiles en bas
        int by = gy + GUI_H - 28;
        btnBack = new Btn(bx, by, bw, bh, "§7◄ Retour");

        // Disposition des boutons d'action (3 en ligne au-dessus du bouton retour)
        int abh  = 16;
        int abw  = (bw - 8) / 3;
        int aby  = by - abh - 6;
        btnPromote = new Btn(bx,              aby, abw, abh, "▲ Promouvoir");
        btnDemote  = new Btn(bx + abw + 4,    aby, abw, abh, "▼ Retrograder");
        btnKick    = new Btn(bx + (abw + 4)*2, aby, abw, abh, "§c✗ Kicker");

        // Modal confirmation (taille adaptee selon le type d'action)
        int cfmW = 220, cfmH = 80;
        int cfmX = gx + (GUI_W - cfmW) / 2;
        int cfmY = gy + (GUI_H - cfmH) / 2;
        btnConfirmYes = new Btn(cfmX + 8,         cfmY + cfmH - 22, 90, 14, "§aOui, confirmer");
        btnConfirmNo  = new Btn(cfmX + cfmW - 98, cfmY + cfmH - 22, 90, 14, "§cAnnuler");
    }

    // ── Render ───────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);
        ClanConfigScreen.drawBorder(ctx, gx, gy, GUI_W, GUI_H, COL_BORDER);

        // Titre : nom du membre colore par la couleur de son rang custom
        String rankColor = getRankColor();
        ctx.drawText(textRenderer, Text.literal(rankColor + member.name()),
                gx + 10, gy + 8, 0xFFFFFF, true);
        ctx.fill(gx + 8, gy + 20, gx + GUI_W - 8, gy + 21, COL_SEPARATOR);

        // Infos
        int iy = gy + 28;
        int lx = gx + 10;
        int gap = 16;

        // Rang actuel (nom du rang custom)
        ctx.drawText(textRenderer, Text.literal("§7Rang : " + getRankDisplay()), lx, iy, COL_LABEL, false);
        iy += gap;

        // Statut en ligne / derniere connexion
        String statusStr;
        if (member.isOnline()) {
            statusStr = "§aEn ligne";
        } else if (member.lastSeen() > 0) {
            statusStr = "§7Vu le : §f" + formatDate(member.lastSeen());
        } else {
            statusStr = "§7Statut inconnu";
        }
        ctx.drawText(textRenderer, Text.literal("§7Statut : " + statusStr), lx, iy, COL_LABEL, false);
        iy += gap;

        // Contributions
        ctx.drawText(textRenderer,
                Text.literal("§7Contributions : §e" + member.totalContributed() + " §7coins"),
                lx, iy, COL_LABEL, false);
        iy += gap;

        // UUID (info debug, discret)
        ctx.drawText(textRenderer,
                Text.literal("§8" + member.uuid().substring(0, 16) + "..."),
                lx, iy, 0xFF444466, false);

        // Separateur avant les boutons
        ctx.fill(gx + 8, gy + GUI_H - 56, gx + GUI_W - 8, gy + GUI_H - 55, COL_SEPARATOR);

        // Boutons d'action : grisés si le viewer n'a pas les permissions
        boolean canPromote = viewerHasOwnerPrivileges && !member.rankId().equals("owner");
        boolean canDemote  = viewerHasOwnerPrivileges && !member.rankId().equals("owner") && !isAtLowestRank();
        boolean canKick    = viewerIsOfficer && !member.role().equals("OWNER");

        renderActionBtn(ctx, btnPromote, mx, my, canPromote,
                COL_ACTION_BG, COL_ACTION_BDR, COL_ACTION_HOV);
        renderActionBtn(ctx, btnDemote, mx, my, canDemote,
                COL_DEMOTE_BG, COL_DEMOTE_BDR, COL_DEMOTE_HOV);
        renderActionBtn(ctx, btnKick, mx, my, canKick,
                COL_KICK_BG, COL_KICK_BDR, COL_KICK_HOV);

        renderBtn(ctx, btnBack, mx, my, COL_BACK_BG, COL_BACK_BDR, COL_BACK_HOV);

        // Modal confirmation
        if (pendingAction != null) {
            renderConfirmModal(ctx, mx, my);
        }

        super.render(ctx, mx, my, delta);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int imx = (int) mx, imy = (int) my;

        // Modal confirmation active
        if (pendingAction != null) {
            if (isOver(btnConfirmYes, imx, imy)) {
                String act = pendingAction;
                pendingAction = null;
                pendingLabel  = null;
                ClientPlayNetworking.send(new ClanActionPayload(act));
                // Retourner au hub (le serveur enverra un nouveau payload)
                client.setScreen(new ClanHubScreen(data));
            } else if (isOver(btnConfirmNo, imx, imy)) {
                pendingAction = null;
                pendingLabel  = null;
            }
            return true;
        }

        if (isOver(btnBack, imx, imy)) {
            client.setScreen(new ClanHubScreen(data));
            return true;
        }

        boolean canPromote = viewerHasOwnerPrivileges && !member.rankId().equals("owner");
        boolean canDemote  = viewerHasOwnerPrivileges && !member.rankId().equals("owner") && !isAtLowestRank();
        boolean canKick    = viewerIsOfficer && !member.role().equals("OWNER");

        if (canPromote && isOver(btnPromote, imx, imy)) {
            pendingAction          = "promote:" + member.uuid();
            pendingIsOwnerTransfer = wouldBeOwner();
            pendingLabel           = pendingIsOwnerTransfer
                    ? "§c⚠ Transfert de propriete !"
                    : "Promouvoir §e" + member.name() + "§r ?";
            return true;
        }
        if (canDemote && isOver(btnDemote, imx, imy)) {
            pendingAction          = "demote:" + member.uuid();
            pendingIsOwnerTransfer = false;
            pendingLabel           = "Retrograder §e" + member.name() + "§r ?";
            return true;
        }
        if (canKick && isOver(btnKick, imx, imy)) {
            pendingAction          = "kick:" + member.uuid();
            pendingIsOwnerTransfer = false;
            pendingLabel           = "Exclure §e" + member.name() + "§r ?";
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    // ── Helpers render ────────────────────────────────────────────────────────

    private void renderConfirmModal(DrawContext ctx, int mx, int my) {
        int cfmW = 220, cfmH = 80;
        int cfmX = gx + (GUI_W - cfmW) / 2;
        int cfmY = gy + (GUI_H - cfmH) / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 200);

        int borderColor = pendingIsOwnerTransfer ? 0xFFAA2222 : COL_KICK_HOV;
        ctx.fill(cfmX, cfmY, cfmX + cfmW, cfmY + cfmH, 0xEE0A0A1A);
        ClanConfigScreen.drawBorder(ctx, cfmX, cfmY, cfmW, cfmH, borderColor);

        String lbl = pendingLabel != null ? pendingLabel : "Confirmer ?";
        int tw = textRenderer.getWidth(lbl);
        ctx.drawText(textRenderer, Text.literal(lbl),
                cfmX + (cfmW - tw) / 2, cfmY + 10, 0xFFFFFF, true);

        if (pendingIsOwnerTransfer) {
            // Ligne 2 : qui devient quoi
            String newOwnerLine = "§e" + member.name() + " §7deviendra §cProprietaire";
            int tw2 = textRenderer.getWidth(newOwnerLine);
            ctx.drawText(textRenderer, Text.literal(newOwnerLine),
                    cfmX + (cfmW - tw2) / 2, cfmY + 26, 0xFFFFFF, false);
            // Ligne 3 : rang de l'actuel owner après transfert
            String actorNewRank = getNextRankBelowOwner();
            String actorLine = "§7Tu deviendras §6" + actorNewRank;
            int tw3 = textRenderer.getWidth(actorLine);
            ctx.drawText(textRenderer, Text.literal(actorLine),
                    cfmX + (cfmW - tw3) / 2, cfmY + 38, 0xFFFFFF, false);
        }

        renderBtn(ctx, btnConfirmYes, mx, my, COL_ACTION_BG, COL_ACTION_BDR, COL_ACTION_HOV);
        renderBtn(ctx, btnConfirmNo,  mx, my, COL_KICK_BG, COL_KICK_BDR, COL_KICK_HOV);

        ctx.getMatrices().pop();
    }

    private void renderActionBtn(DrawContext ctx, Btn btn, int mx, int my, boolean enabled,
                                 int bgColor, int bdrColor, int hoverColor) {
        if (!enabled) {
            ctx.fill(btn.x, btn.y, btn.x + btn.w, btn.y + btn.h, COL_DISABLED);
            ClanConfigScreen.drawBorder(ctx, btn.x, btn.y, btn.w, btn.h, COL_DISABLED_T);
            int tw = textRenderer.getWidth(btn.label);
            ctx.drawText(textRenderer, Text.literal("§8" + stripCodes(btn.label)),
                    btn.x + (btn.w - tw) / 2, btn.y + (btn.h - 8) / 2, COL_DISABLED_T, false);
        } else {
            renderBtn(ctx, btn, mx, my, bgColor, bdrColor, hoverColor);
        }
    }

    private void renderBtn(DrawContext ctx, Btn btn, int mx, int my,
                           int bgColor, int bdrColor, int hoverColor) {
        boolean hov = isOver(btn, mx, my);
        ctx.fill(btn.x, btn.y, btn.x + btn.w, btn.y + btn.h, bgColor);
        ClanConfigScreen.drawBorder(ctx, btn.x, btn.y, btn.w, btn.h, hov ? hoverColor : bdrColor);
        int tw = textRenderer.getWidth(btn.label);
        ctx.drawText(textRenderer, Text.literal(btn.label),
                btn.x + (btn.w - tw) / 2, btn.y + (btn.h - 8) / 2,
                hov ? 0xFFFFFF : COL_LABEL, false);
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    /** Retourne le rang custom affiché (colorCode + name) du membre. */
    private String getRankDisplay() {
        return data.ranks().stream()
                .filter(r -> r.id().equals(member.rankId()))
                .findFirst()
                .map(r -> r.colorCode() + r.name())
                .orElseGet(() -> switch (member.role()) {
                    case "OWNER" -> "§cProprietaire"; case "OFFICER" -> "§6Officer"; default -> "§7Membre";
                });
    }

    /** Retourne la couleur du rang custom du membre. */
    private String getRankColor() {
        return data.ranks().stream()
                .filter(r -> r.id().equals(member.rankId()))
                .findFirst()
                .map(ClanGuiPayload.RankDto::colorCode)
                .orElseGet(() -> switch (member.role()) {
                    case "OWNER" -> "§c"; case "OFFICER" -> "§6"; default -> "§7";
                });
    }

    /** Retourne true si le membre est deja au rang le plus bas. */
    private boolean isAtLowestRank() {
        List<ClanGuiPayload.RankDto> sorted = data.ranks().stream()
                .sorted(Comparator.comparingInt(ClanGuiPayload.RankDto::sortOrder))
                .toList();
        return !sorted.isEmpty() && sorted.get(sorted.size() - 1).id().equals(member.rankId());
    }

    /** Retourne true si promouvoir ce membre lui donnerait le rang owner. */
    private boolean wouldBeOwner() {
        List<ClanGuiPayload.RankDto> sorted = data.ranks().stream()
                .sorted(Comparator.comparingInt(ClanGuiPayload.RankDto::sortOrder))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).id().equals(member.rankId())) {
                return i > 0 && sorted.get(i - 1).id().equals("owner");
            }
        }
        return false;
    }

    /** Retourne le nom du rang qui sera donne a l'owner apres le transfert de propriete. */
    private String getNextRankBelowOwner() {
        List<ClanGuiPayload.RankDto> sorted = data.ranks().stream()
                .sorted(Comparator.comparingInt(ClanGuiPayload.RankDto::sortOrder))
                .toList();
        if (sorted.size() > 1) return sorted.get(1).colorCode() + sorted.get(1).name();
        return "§7Membre";
    }

    private static boolean isOver(Btn btn, int mx, int my) {
        return mx >= btn.x && mx <= btn.x + btn.w && my >= btn.y && my <= btn.y + btn.h;
    }

    private static String formatDate(long ts) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(ts));
    }

    /** Retire les codes Minecraft §x d'une chaine. */
    private static String stripCodes(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }

    // ── Accesseurs ───────────────────────────────────────────────────────────

    /** Retourne l'UUID du membre affiche (pour le refresh via LenemonNetworkClient). */
    public String getMemberUuid() {
        return member.uuid();
    }

    // ── Screen ───────────────────────────────────────────────────────────────

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    // ── Btn interne ──────────────────────────────────────────────────────────

    private static class Btn {
        final int x, y, w, h;
        final String label;
        Btn(int x, int y, int w, int h, String label) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.label = label;
        }
    }
}
