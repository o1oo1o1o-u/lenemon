package com.lenemon.client.menu.screen;

import com.lenemon.network.menu.HunterMenuOpenPayload;
import com.lenemon.network.menu.MenuActionPayload;
import com.lenemon.network.menu.QuestDto;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * HunterMenuScreen — écran de consultation des quêtes du chasseur de Pokémon.
 *
 * <p>Reçoit un {@link HunterMenuOpenPayload} depuis le serveur et affiche :
 * <ul>
 *   <li>La bande de niveau / XP avec barre de progression et timer de reset</li>
 *   <li>Jusqu'à trois sections de quêtes (Facile / Moyen / Difficile), masquées si vides</li>
 *   <li>Un bouton retour qui renvoie un {@link MenuActionPayload} au serveur</li>
 * </ul>
 *
 * <p>Toutes les coordonnées de cartes sont pré-calculées dans {@link #init()} pour
 * éviter tout calcul superflu dans {@link #render}.
 */
public class HunterMenuScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_WIDTH  = 340;
    private static final int GUI_HEIGHT = 370;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xCC0A0A1A;
    private static final int COL_BORDER     = 0xFF2255AA;
    private static final int COL_SEPARATOR  = 0x55FFFFFF;
    private static final int COL_CARD_BG    = 0x55113355;
    private static final int COL_CARD_BDR   = 0xFF334466;
    private static final int COL_CARD_HOVER = 0xFF4477CC;
    private static final int COL_LABEL      = 0xAAAAAA;
    private static final int COL_EASY_BG    = 0x22004400;
    private static final int COL_EASY_BDR   = 0xFF226622;
    private static final int COL_MED_BG     = 0x22442200;
    private static final int COL_MED_BDR    = 0xFF664422;
    private static final int COL_HARD_BG    = 0x22220044;
    private static final int COL_HARD_BDR   = 0xFF442266;

    // ── Constantes de mise en page ────────────────────────────────────────────
    /** Hauteur de la bande niveau (px). */
    private static final int LEVEL_BAND_H  = 30;
    /** Hauteur des cartes easy et medium (px). */
    private static final int CARD_H_NORMAL = 68;
    /** Hauteur de la carte hard (px). */
    private static final int CARD_H_HARD   = 60;
    /** Largeur des cartes easy (px). */
    private static final int CARD_W_EASY   = 96;
    /** Largeur des cartes medium (px). */
    private static final int CARD_W_MED    = 140;
    /** Largeur de la carte hard (px). */
    private static final int CARD_W_HARD   = 260;
    /** Gap horizontal entre cartes easy. */
    private static final int GAP_EASY      = 6;
    /** Gap horizontal entre cartes medium. */
    private static final int GAP_MED       = 12;
    /** Hauteur d'un label de section (px). */
    private static final int SECTION_LABEL_H = 14;
    /** Espacement vertical entre éléments de layout. */
    private static final int SECTION_GAP    = 6;
    /** Marge horizontale interne du GUI. */
    private static final int MARGIN         = 8;

    // ── Données reçues du serveur ─────────────────────────────────────────────
    private final HunterMenuOpenPayload data;

    // ── Coordonnées calculées dans init() ────────────────────────────────────
    private int guiX, guiY;

    /** Bande niveau : coin supérieur gauche. */
    private int levelBandX, levelBandY;
    /** Largeur effective de la bande. */
    private int levelBandW;

    /** Préfixe niveau formaté, calculé une fois. */
    private String levelPrefix;
    /** Partie droite de la ligne XP, calculée une fois. */
    private String xpSuffix;
    /** Ligne d'information secondaire (palier + reset), calculée une fois. */
    private String levelInfoLine;

    /** Cartes de quêtes pré-calculées pour éviter les calculs dans render(). */
    private final List<CardLayout> easyCards   = new ArrayList<>();
    private final List<CardLayout> medCards    = new ArrayList<>();
    private final List<CardLayout> hardCards   = new ArrayList<>();

    /** Labels de sections, calculés dans init(). */
    private String easyLabel, medLabel, hardLabel;
    /** Position Y des labels de section. */
    private int easyLabelY, medLabelY, hardLabelY;

    /** Bouton retour : bounds pré-calculés. */
    private int backBtnX, backBtnY;
    private static final int BACK_BTN_W = 80;
    private static final int BACK_BTN_H = 18;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crée l'écran à partir du payload reçu du serveur.
     *
     * @param data données de l'état chasseur (niveau, XP, quêtes, timer)
     */
    public HunterMenuScreen(HunterMenuOpenPayload data) {
        super(Text.literal("Chasseur de Pokémon"));
        this.data = data;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        guiX = (this.width  - GUI_WIDTH)  / 2;
        guiY = (this.height - GUI_HEIGHT) / 2;

        // Textes pré-calculés pour la bande niveau
        levelPrefix  = "§b§l✦ Niveau " + data.level();
        xpSuffix     = "§e" + data.xp() + "§7/§e" + data.xpForNext() + " §7XP";
        levelInfoLine = "§7Prochain palier : §6Level " + data.nextRewardLevel()
                + "  §8•  §7Reset dans : §e" + data.resetTimer();

        // Bande niveau
        levelBandX = guiX + MARGIN;
        levelBandY = guiY + 26;
        levelBandW = GUI_WIDTH - MARGIN * 2;

        // Point de départ vertical pour les sections de quêtes
        int cursorY = levelBandY + LEVEL_BAND_H + SECTION_GAP;

        // ── Section Easy ──────────────────────────────────────────────────────
        easyCards.clear();
        if (!data.easyQuests().isEmpty()) {
            easyLabel  = "§a§l— Facile —";
            easyLabelY = cursorY;
            cursorY   += SECTION_LABEL_H + SECTION_GAP;

            List<QuestDto> quests = data.easyQuests();
            int count    = Math.min(quests.size(), 3);
            int totalW   = count * CARD_W_EASY + (count - 1) * GAP_EASY;
            int startX   = guiX + (GUI_WIDTH - totalW) / 2;

            for (int i = 0; i < count; i++) {
                int cx = startX + i * (CARD_W_EASY + GAP_EASY);
                easyCards.add(new CardLayout(cx, cursorY, CARD_W_EASY, CARD_H_NORMAL,
                        COL_EASY_BG, COL_EASY_BDR, quests.get(i)));
            }
            cursorY += CARD_H_NORMAL + SECTION_GAP;
        }

        // ── Section Medium ────────────────────────────────────────────────────
        medCards.clear();
        if (!data.mediumQuests().isEmpty()) {
            medLabel  = "§e§l— Moyen —";
            medLabelY = cursorY;
            cursorY  += SECTION_LABEL_H + SECTION_GAP;

            List<QuestDto> quests = data.mediumQuests();
            int count    = Math.min(quests.size(), 2);
            int totalW   = count * CARD_W_MED + (count - 1) * GAP_MED;
            int startX   = guiX + (GUI_WIDTH - totalW) / 2;

            for (int i = 0; i < count; i++) {
                int cx = startX + i * (CARD_W_MED + GAP_MED);
                medCards.add(new CardLayout(cx, cursorY, CARD_W_MED, CARD_H_NORMAL,
                        COL_MED_BG, COL_MED_BDR, quests.get(i)));
            }
            cursorY += CARD_H_NORMAL + SECTION_GAP;
        }

        // ── Section Hard ──────────────────────────────────────────────────────
        hardCards.clear();
        if (!data.hardQuests().isEmpty()) {
            hardLabel  = "§5§l— Difficile —";
            hardLabelY = cursorY;
            cursorY   += SECTION_LABEL_H + SECTION_GAP;

            QuestDto quest = data.hardQuests().get(0);
            int cx = guiX + (GUI_WIDTH - CARD_W_HARD) / 2;
            hardCards.add(new CardLayout(cx, cursorY, CARD_W_HARD, CARD_H_HARD,
                    COL_HARD_BG, COL_HARD_BDR, quest));
            cursorY += CARD_H_HARD + SECTION_GAP;
        }

        // ── Bouton retour ─────────────────────────────────────────────────────
        backBtnX = guiX + (GUI_WIDTH - BACK_BTN_W) / 2;
        backBtnY = cursorY;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Surcharge vide : supprime le fond sombre et le blur vanilla.
     */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Intentionnellement vide — pas de fond sombre vanilla
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // ── Fond + bordure principale ─────────────────────────────────────────
        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, COL_BORDER);

        // ── Titre ─────────────────────────────────────────────────────────────
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b§l⚔ §fChasseur de Pokémon"),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);

        // ── Séparateur titre ──────────────────────────────────────────────────
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_WIDTH - 10, guiY + 21, COL_SEPARATOR);

        // ── Bande niveau ──────────────────────────────────────────────────────
        renderLevelBand(ctx);

        // ── Sections de quêtes ────────────────────────────────────────────────
        // Variable pour stocker la carte survolée et ses données tooltip (affiché en dernier)
        CardLayout hoveredCard = null;

        if (!easyCards.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(easyLabel),
                    guiX + GUI_WIDTH / 2, easyLabelY, 0xFFFFFF);
            for (CardLayout card : easyCards) {
                boolean hover = card.isHovered(mouseX, mouseY);
                renderCard(ctx, card, hover);
                if (hover) hoveredCard = card;
            }
        }

        if (!medCards.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(medLabel),
                    guiX + GUI_WIDTH / 2, medLabelY, 0xFFFFFF);
            for (CardLayout card : medCards) {
                boolean hover = card.isHovered(mouseX, mouseY);
                renderCard(ctx, card, hover);
                if (hover) hoveredCard = card;
            }
        }

        if (!hardCards.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(hardLabel),
                    guiX + GUI_WIDTH / 2, hardLabelY, 0xFFFFFF);
            for (CardLayout card : hardCards) {
                boolean hover = card.isHovered(mouseX, mouseY);
                renderCard(ctx, card, hover);
                if (hover) hoveredCard = card;
            }
        }

        // ── Bouton retour ─────────────────────────────────────────────────────
        renderBackButton(ctx, mouseX, mouseY);

        // ── Tooltip (affiché en dernier pour passer par-dessus tout) ─────────
        if (hoveredCard != null) {
            ctx.drawTooltip(textRenderer, buildTooltip(hoveredCard.quest()), mouseX, mouseY);
        }

        // ── Widgets vanilla (focus, etc.) ─────────────────────────────────────
        super.render(ctx, mouseX, mouseY, delta);
    }

    /**
     * Rend la bande de niveau : fond, bordure, ligne XP et ligne info.
     */
    private void renderLevelBand(DrawContext ctx) {
        // Fond + bordure
        ctx.fill(levelBandX, levelBandY,
                levelBandX + levelBandW, levelBandY + LEVEL_BAND_H, COL_CARD_BG);
        ctx.drawBorder(levelBandX, levelBandY, levelBandW, LEVEL_BAND_H, COL_CARD_BDR);

        // Ligne 1 : niveau (gauche) | XP (droite) | barre de progression (centre)
        int line1Y = levelBandY + 5;
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(levelPrefix),
                levelBandX + 4, line1Y, 0xFFFFFF);

        int xpSuffixWidth = textRenderer.getWidth(xpSuffix);
        int xpSuffixX = levelBandX + levelBandW - 4 - xpSuffixWidth;
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(xpSuffix),
                xpSuffixX, line1Y, 0xFFFFFF);

        // Barre de progression custom entre le texte niveau et le texte XP
        int levelPrefixWidth = textRenderer.getWidth(levelPrefix);
        int barX1 = levelBandX + 4 + levelPrefixWidth + 4;
        int barX2 = xpSuffixX - 4;
        int barW  = barX2 - barX1;
        if (barW > 4) {
            float progress = (data.xpForNext() > 0)
                    ? Math.min(1.0f, (float) data.xp() / data.xpForNext())
                    : 0.0f;
            int barY    = line1Y + 1;
            int barH    = 6;
            int fillW   = Math.round(barW * progress);
            // Fond de la barre
            ctx.fill(barX1, barY, barX1 + barW, barY + barH, 0xFF1A1A2E);
            // Remplissage
            if (fillW > 0) {
                ctx.fill(barX1, barY, barX1 + fillW, barY + barH, 0xFF44AAFF);
            }
        }

        // Ligne 2 : prochain palier + reset (centré)
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(levelInfoLine),
                levelBandX + levelBandW / 2, levelBandY + 17, 0xFFFFFF);
    }

    /**
     * Rend une carte de quête (fond, bordure colorée selon hover, icône, titre, statut).
     *
     * @param ctx   contexte de rendu
     * @param card  données de mise en page de la carte
     * @param hover true si la souris survole la carte
     */
    private void renderCard(DrawContext ctx, CardLayout card, boolean hover) {
        int x = card.x();
        int y = card.y();
        int w = card.w();
        int h = card.h();
        QuestDto quest = card.quest();

        // Fond
        ctx.fill(x, y, x + w, y + h, card.bgColor());
        // Bordure (couleur de la difficulté ou hover)
        ctx.drawBorder(x, y, w, h, hover ? COL_CARD_HOVER : card.bdrColor());

        // ── Icône (item de ball) ──────────────────────────────────────────────
        if (!quest.ballItemId().isEmpty()) {
            net.minecraft.item.Item ballItem = Registries.ITEM.get(Identifier.of(quest.ballItemId()));
            ctx.drawItem(new ItemStack(ballItem), x + 4, y + 4);
        }

        // ── Titre (word-wrap, centré horizontalement) ─────────────────────────
        // Réserve 24px à gauche pour l'icône + marge droite symétrique
        int titleMaxW  = w - 28;
        int titleCentX = x + w / 2;
        String rawTitle = quest.colorCode() + "§l" + quest.description();
        var titleLines = textRenderer.wrapLines(Text.literal(rawTitle), titleMaxW);
        for (int l = 0; l < titleLines.size(); l++) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    titleLines.get(l),
                    titleCentX, y + 6 + l * 10, 0xFFFFFF);
        }

        // ── Statut ────────────────────────────────────────────────────────────
        int statusY = y + 6 + titleLines.size() * 10 + 2;
        String statusLine;
        if (quest.complete()) {
            statusLine = "§a✔ Complétée";
        } else {
            statusLine = "§7" + quest.progress() + "§f/§7" + quest.amount();
        }
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(statusLine),
                titleCentX, statusY, 0xFFFFFF);
    }

    /**
     * Rend le bouton retour avec son état hover.
     */
    private void renderBackButton(DrawContext ctx, int mouseX, int mouseY) {
        boolean hover = mouseX >= backBtnX && mouseX <= backBtnX + BACK_BTN_W
                && mouseY >= backBtnY && mouseY <= backBtnY + BACK_BTN_H;

        ctx.fill(backBtnX, backBtnY,
                backBtnX + BACK_BTN_W, backBtnY + BACK_BTN_H, COL_CARD_BG);
        ctx.drawBorder(backBtnX, backBtnY, BACK_BTN_W, BACK_BTN_H,
                hover ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7§l« Retour"),
                backBtnX + BACK_BTN_W / 2, backBtnY + (BACK_BTN_H - 8) / 2, 0xFFFFFF);
    }

    /**
     * Construit les lignes du tooltip pour une quête donnée.
     *
     * @param quest quête survolée
     * @return liste de {@link Text} à passer à {@code ctx.drawTooltip}
     */
    private List<Text> buildTooltip(QuestDto quest) {
        List<Text> lines = new ArrayList<>();

        lines.add(Text.literal(quest.difficultyLabel()));
        lines.add(Text.literal(""));
        lines.add(Text.literal("§f" + quest.description()));
        lines.add(Text.literal(""));

        String progressText = quest.complete()
                ? "§a§l✔ COMPLÉTÉE"
                : "§7" + quest.progress() + " §f/ §7" + quest.amount();
        lines.add(Text.literal("§7Progression : " + progressText));
        lines.add(Text.literal(""));

        lines.add(Text.literal("§6Récompenses :"));
        lines.add(Text.literal("§7  • XP : §b+" + quest.xpReward()));
        lines.add(Text.literal("§7  • Money : §a+" + quest.moneyReward() + " $"));

        if (!quest.itemsLabel().isEmpty()) {
            lines.add(Text.literal("§7  • Items : §f" + quest.itemsLabel()));
        }

        return lines;
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    /**
     * Gère le clic sur le bouton retour.
     * Les cartes de quêtes sont en consultation uniquement (aucun clic traité).
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && mouseX >= backBtnX && mouseX <= backBtnX + BACK_BTN_W
                && mouseY >= backBtnY && mouseY <= backBtnY + BACK_BTN_H) {
            ClientPlayNetworking.send(new MenuActionPayload("open_menu"));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ── Types internes ────────────────────────────────────────────────────────

    /**
     * Enregistrement immuable contenant toutes les coordonnées pré-calculées d'une carte.
     * Evite tout calcul géométrique dans la méthode render().
     *
     * @param x        coordonnée X absolue (coin supérieur gauche)
     * @param y        coordonnée Y absolue (coin supérieur gauche)
     * @param w        largeur en pixels
     * @param h        hauteur en pixels
     * @param bgColor  couleur de fond ARGB
     * @param bdrColor couleur de bordure ARGB (sans hover)
     * @param quest    données de la quête associée
     */
    private record CardLayout(int x, int y, int w, int h,
                               int bgColor, int bdrColor, QuestDto quest) {

        /** @return true si le point (mx, my) est dans les bounds de la carte. */
        boolean isHovered(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }
}
