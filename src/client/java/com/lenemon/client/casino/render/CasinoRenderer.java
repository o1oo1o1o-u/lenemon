package com.lenemon.client.casino.render;

import com.lenemon.casino.util.CasinoTextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer programmatique pur pour le GUI casino.
 * Aucune texture PNG — tout est rendu via ctx.fill(), ctx.drawText() et ctx.drawItem().
 */
public final class CasinoRenderer {

    private CasinoRenderer() {}

    // ── Items des rouleaux (Pokéballs Cobblemon + items thématiques) ──────
    // Initialisés comme champs static final : lookup Registries une seule fois au chargement.
    private static final List<ItemStack> REEL_ITEMS = List.of(
            new ItemStack(Registries.ITEM.get(Identifier.of("cobblemon", "poke_ball"))),
            new ItemStack(Registries.ITEM.get(Identifier.of("cobblemon", "great_ball"))),
            new ItemStack(Registries.ITEM.get(Identifier.of("cobblemon", "ultra_ball"))),
            new ItemStack(Registries.ITEM.get(Identifier.of("cobblemon", "master_ball"))),
            new ItemStack(Items.DIAMOND),
            new ItemStack(Items.NETHER_STAR)
    );

    // ── Fond principal du GUI ─────────────────────────────────────────────
    /**
     * Dessine le fond général du GUI (bordures, titre, séparateur).
     *
     * @param ctx DrawContext courant
     * @param x   coin haut-gauche du GUI en pixels écran
     * @param y   coin haut-gauche du GUI en pixels écran
     */
    public static void drawBackground(DrawContext ctx, int x, int y) {
        int w = CasinoTextures.GUI_WIDTH;
        int h = CasinoTextures.GUI_HEIGHT;

        // Fond sombre principal
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xEE070D1A);

        // Bordure extérieure bleu marine (1px)
        ctx.fill(x,         y,         x + w,     y + 1,     0xFF1A2A4A);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     0xFF1A2A4A);
        ctx.fill(x,         y,         x + 1,     y + h,     0xFF1A2A4A);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     0xFF1A2A4A);

        // Filet intérieur accent bleu (1px à +2 du bord)
        ctx.fill(x + 2,     y + 2,     x + w - 2, y + 3,     0xFF4A90E2);
        ctx.fill(x + 2,     y + h - 3, x + w - 2, y + h - 2, 0xFF4A90E2);
        ctx.fill(x + 2,     y + 2,     x + 3,     y + h - 2, 0xFF4A90E2);
        ctx.fill(x + w - 3, y + 2,     x + w - 2, y + h - 2, 0xFF4A90E2);

        // Titre "[ CASINO ]" centré horizontalement, en or
        var tr = MinecraftClient.getInstance().textRenderer;
        String title = "[ CASINO ]";
        int tw = tr.getWidth(title);
        ctx.drawText(tr, Text.literal(title), x + w / 2 - tw / 2, y + 9, 0xFFFFD700, true);

        // Séparateur double ligne sous le titre
        ctx.fill(x + 8, y + 20, x + w - 8, y + 21, 0xFF1A2A4A);
        ctx.fill(x + 8, y + 22, x + w - 8, y + 23, 0xFF4A90E2);
    }

    // ── Zone centrale (affichage Pokémon) ─────────────────────────────────
    /**
     * Dessine le cadre de la zone centrale.
     *
     * @param ctx DrawContext courant
     * @param x   coin haut-gauche du GUI
     * @param y   coin haut-gauche du GUI
     */
    public static void drawCenterZone(DrawContext ctx, int x, int y) {
        int cx = x + CasinoTextures.CENTER_TX;
        int cy = y + CasinoTextures.CENTER_TY;
        int cw = CasinoTextures.CENTER_TW;
        int ch = CasinoTextures.CENTER_TH;

        // Fond noir profond
        ctx.fill(cx + 1, cy + 1, cx + cw - 1, cy + ch - 1, 0xFF050A14);

        // Bordure bleu moyen (1px)
        ctx.fill(cx,          cy,          cx + cw, cy + 1,      0xFF2A4A7A);
        ctx.fill(cx,          cy + ch - 1, cx + cw, cy + ch,     0xFF2A4A7A);
        ctx.fill(cx,          cy,          cx + 1,  cy + ch,     0xFF2A4A7A);
        ctx.fill(cx + cw - 1, cy,          cx + cw, cy + ch,     0xFF2A4A7A);
    }

    // ── Fonds et cadres des rouleaux ──────────────────────────────────────
    /**
     * Dessine les fonds des deux rouleaux gauche/droit ainsi que la zone centrale.
     *
     * @param ctx DrawContext courant
     * @param x   coin haut-gauche du GUI
     * @param y   coin haut-gauche du GUI
     */
    public static void drawSlotBackgrounds(DrawContext ctx, int x, int y) {
        drawReelFrame(ctx,
                x + CasinoTextures.SLOT_L_TX, y + CasinoTextures.SLOT_L_TY,
                CasinoTextures.SLOT_L_TW,     CasinoTextures.SLOT_L_TH);
        drawReelFrame(ctx,
                x + CasinoTextures.SLOT_R_TX, y + CasinoTextures.SLOT_R_TY,
                CasinoTextures.SLOT_R_TW,     CasinoTextures.SLOT_R_TH);
        drawCenterZone(ctx, x, y);
    }

    /**
     * Dessine le cadre d'un rouleau individuel.
     * Inclut : fond noir, bordure marine, filets accent bleu haut/bas, ligne dorée centrale.
     */
    private static void drawReelFrame(DrawContext ctx, int rx, int ry, int rw, int rh) {
        // Fond noir du rouleau
        ctx.fill(rx + 1, ry + 1, rx + rw - 1, ry + rh - 1, 0xFF080E1C);

        // Bordure externe bleu marine (1px)
        ctx.fill(rx,          ry,          rx + rw, ry + 1,      0xFF1E3A6E);
        ctx.fill(rx,          ry + rh - 1, rx + rw, ry + rh,     0xFF1E3A6E);
        ctx.fill(rx,          ry,          rx + 1,  ry + rh,     0xFF1E3A6E);
        ctx.fill(rx + rw - 1, ry,          rx + rw, ry + rh,     0xFF1E3A6E);

        // Filets accent bleu haut et bas (2px chacun)
        ctx.fill(rx + 1, ry + 1,      rx + rw - 1, ry + 3,          0xFF4A90E2);
        ctx.fill(rx + 1, ry + rh - 3, rx + rw - 1, ry + rh - 1,     0xFF4A90E2);

        // Ligne dorée semi-transparente au centre (ligne gagnante)
        int midY = ry + rh / 2;
        ctx.fill(rx + 1, midY, rx + rw - 1, midY + 1, 0x77FFD700);
    }

    // ── Icônes des rouleaux (ItemStack rendus à ×2) ───────────────────────
    /**
     * Dessine les icônes d'un rouleau en utilisant de vrais ItemStack Minecraft.
     * Chaque item est rendu nativement 16×16 puis scalé ×2 (= 32px affiché).
     * Un scissor masque les icônes hors de la fenêtre du rouleau.
     * Des dégradés de fondu haut/bas dissimulent les icônes partielles.
     *
     * @param ctx       DrawContext courant
     * @param guiX      coin haut-gauche du GUI
     * @param guiY      coin haut-gauche du GUI
     * @param slotTx    offset X du rouleau par rapport au GUI
     * @param slotTy    offset Y du rouleau par rapport au GUI
     * @param slotTw    largeur du rouleau
     * @param slotTh    hauteur du rouleau
     * @param reel      état courant du rouleau (positions, animation)
     * @param glowState true si un halo doré de victoire doit être affiché
     * @param scale     facteur d'échelle de la fenêtre (non utilisé ici, conservé pour compatibilité)
     */
    public static void drawSlotIcons(
            DrawContext ctx,
            int guiX, int guiY,
            int slotTx, int slotTy, int slotTw, int slotTh,
            SlotReel reel,
            boolean glowState,
            double scale
    ) {
        int screenX = guiX + slotTx;
        int screenY = guiY + slotTy;

        // Scissor : on laisse 3px de marge haut/bas pour que les filets accent restent visibles
        ctx.enableScissor(screenX + 1, screenY + 3, screenX + slotTw - 1, screenY + slotTh - 3);

        // Taille native d'un item Minecraft = 16×16, affiché ×2 = 32px
        final int   ITEM_NATIVE = 16;
        final float ITEM_SCALE  = 2.0f;
        final int   SCALED_SIZE = (int)(ITEM_NATIVE * ITEM_SCALE); // 32

        // Centrage horizontal dans le rouleau
        int iconX = screenX + (slotTw - SCALED_SIZE) / 2;

        for (SlotReel.DrawEntry entry : reel.getDrawEntries(slotTh)) {
            int iconIndex = entry.iconIndex() % REEL_ITEMS.size();
            ItemStack stack = REEL_ITEMS.get(iconIndex);
            int iconY = screenY + (int) entry.yOffset();

            // Scale ×2 centré sur le point de dessin de l'item
            ctx.getMatrices().push();
            // Z=100 pour que l'item passe au bon layer par rapport aux fill() environnants
            ctx.getMatrices().translate(
                    iconX + SCALED_SIZE / 2f,
                    iconY + SCALED_SIZE / 2f,
                    100
            );
            ctx.getMatrices().scale(ITEM_SCALE, ITEM_SCALE, 1f);
            // drawItem dessine en (0,0) relatif au MatrixStack courant
            ctx.drawItem(stack, -ITEM_NATIVE / 2, -ITEM_NATIVE / 2);
            ctx.getMatrices().pop();

            // Halo doré autour de l'item sur la ligne gagnante (centre du rouleau)
            if (glowState) {
                int midY = screenY + slotTh / 2;
                int itemCenterY = iconY + SCALED_SIZE / 2;
                if (Math.abs(itemCenterY - midY) < SlotReel.getCellHeight() / 2) {
                    ctx.fill(
                            iconX - 2,             iconY - 2,
                            iconX + SCALED_SIZE + 2, iconY + SCALED_SIZE + 2,
                            0x44FFD700
                    );
                }
            }
        }

        // Dégradés de fondu haut (masquent les items partiellement visibles)
        ctx.fill(screenX + 1, screenY + 3,  screenX + slotTw - 1, screenY + 3  + 12, 0xCC080E1C);
        ctx.fill(screenX + 1, screenY + 3,  screenX + slotTw - 1, screenY + 3  + 6,  0xEE080E1C);
        // Dégradés de fondu bas
        ctx.fill(screenX + 1, screenY + slotTh - 15, screenX + slotTw - 1, screenY + slotTh - 3, 0xCC080E1C);
        ctx.fill(screenX + 1, screenY + slotTh - 9,  screenX + slotTw - 1, screenY + slotTh - 3, 0xEE080E1C);

        ctx.disableScissor();
    }

    // ── Bouton SPIN (3 états visuels + état verrouillé) ───────────────────
    /**
     * Dessine le bouton SPIN en rendu programmatique pur.
     *
     * @param ctx     DrawContext courant
     * @param guiX    coin haut-gauche du GUI
     * @param guiY    coin haut-gauche du GUI
     * @param pressed vrai si le bouton est actuellement enfoncé (mouseClicked en cours)
     * @param hovered vrai si la souris survole le bouton
     * @param locked  vrai si le spin est impossible (anime en cours, pas assez de fonds, etc.)
     */
    public static void drawSpinButton(DrawContext ctx, int guiX, int guiY,
                                      boolean pressed, boolean hovered, boolean locked) {
        int bx = guiX + CasinoTextures.BTN_X;
        int by = guiY + CasinoTextures.BTN_Y;
        int bw = CasinoTextures.BTN_W;
        int bh = CasinoTextures.BTN_H;

        // Couleurs selon l'état (locked > pressed > hovered > normal)
        int fillColor   = locked  ? 0xFF1A1A2A
                        : pressed ? 0xFF0A1A0A
                        : hovered ? 0xFF2A6A2A
                        :           0xFF1A4A1A;
        int borderColor = locked  ? 0xFF444455 : 0xFF44CC44;
        int textColor   = locked  ? 0xFF666677 : 0xFFAAFFAA;

        // Ombre portée (+2,+2)
        ctx.fill(bx + 2, by + 2, bx + bw + 2, by + bh + 2, 0x55000000);

        // Fond du bouton (sans les coins — la bordure les couvrira)
        ctx.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, fillColor);

        // Reflet clair sur la moitié haute (uniquement état actif non enfoncé)
        if (!locked && !pressed) {
            ctx.fill(bx + 1, by + 1, bx + bw - 1, by + bh / 2, 0x18FFFFFF);
        }

        // Bordure (4 côtés, 1px)
        ctx.fill(bx,          by,          bx + bw, by + 1,      borderColor);
        ctx.fill(bx,          by + bh - 1, bx + bw, by + bh,     borderColor);
        ctx.fill(bx,          by,          bx + 1,  by + bh,     borderColor);
        ctx.fill(bx + bw - 1, by,          bx + bw, by + bh,     borderColor);

        // Texte centré, décalé +1px en Y si enfoncé (effet press)
        var tr    = MinecraftClient.getInstance().textRenderer;
        String label = locked ? "SPIN" : "SPIN !";
        int tx = bx + bw / 2 - tr.getWidth(label) / 2;
        int ty = by + bh / 2 - 4 + (pressed ? 1 : 0);
        ctx.drawText(tr, Text.literal(label), tx, ty, textColor, true);
    }

    // ── BetBox ─────────────────────────────────────────────────────────────
    /**
     * Dessine la boîte d'affichage de la mise (BET / montant) et le pourcentage de victoire.
     */
    public static void drawBetBox(DrawContext ctx, int guiX, int guiY, int betAmount, int winChance) {
        final int BOX_W = 110;
        final int BOX_H = 36; // étendu pour accueillir la ligne WIN
        int boxX = guiX + CasinoTextures.GUI_WIDTH / 2 - BOX_W / 2;
        int boxY = guiY + CasinoTextures.BTN_Y + CasinoTextures.BTN_H + 8;

        // Fond très sombre
        ctx.fill(boxX + 1, boxY + 1, boxX + BOX_W - 1, boxY + BOX_H - 1, 0xCC040810);

        // Bordure bleu marine (1px)
        ctx.fill(boxX,             boxY,             boxX + BOX_W, boxY + 1,         0xFF1A2A4A);
        ctx.fill(boxX,             boxY + BOX_H - 1, boxX + BOX_W, boxY + BOX_H,     0xFF1A2A4A);
        ctx.fill(boxX,             boxY,             boxX + 1,     boxY + BOX_H,     0xFF1A2A4A);
        ctx.fill(boxX + BOX_W - 1, boxY,             boxX + BOX_W, boxY + BOX_H,     0xFF1A2A4A);

        // Filet accent haut/bas
        ctx.fill(boxX + 1, boxY + 1,          boxX + BOX_W - 1, boxY + 2,           0xFF4A90E2);
        ctx.fill(boxX + 1, boxY + BOX_H - 2,  boxX + BOX_W - 1, boxY + BOX_H - 1,   0xFF4A90E2);

        var tr = MinecraftClient.getInstance().textRenderer;

        // Séparateur vertical commun aux deux lignes
        int sepX = boxX + 30;
        ctx.fill(sepX, boxY + 3, sepX + 1, boxY + BOX_H - 3, 0x44FFFFFF);

        // Séparateur horizontal entre les deux lignes
        int midY = boxY + BOX_H / 2;
        ctx.fill(boxX + 1, midY, boxX + BOX_W - 1, midY + 1, 0x33FFFFFF);

        // Ligne 1 : BET
        ctx.drawText(tr, Text.literal("BET"), boxX + 8, boxY + 5, 0xFF00E5FF, false);
        String betVal = betAmount + " \u20BD";
        ctx.drawText(tr, Text.literal(betVal), boxX + BOX_W - 6 - tr.getWidth(betVal), boxY + 5, 0xFFFFD700, false);

        // Ligne 2 : WIN %
        ctx.drawText(tr, Text.literal("WIN"), boxX + 8, midY + 5, 0xFF00E5FF, false);
        // winChance est sur 10 000 (ex: 1500 = 15 %)
        String pct = String.format("%.2f%%", winChance / 100.0);
        int pctColor = winChance >= 5000 ? 0xFF44FF88 : winChance >= 2000 ? 0xFFFFD700 : 0xFFFF6644;
        ctx.drawText(tr, Text.literal(pct), boxX + BOX_W - 6 - tr.getWidth(pct), midY + 5, pctColor, false);
    }

    // ── Table de traduction des natures Cobblemon → français ─────────────
    private static final java.util.Map<String, String> NATURE_FR = java.util.Map.ofEntries(
            java.util.Map.entry("hardy",    "Hardi"),
            java.util.Map.entry("lonely",   "Solitaire"),
            java.util.Map.entry("brave",    "Brave"),
            java.util.Map.entry("adamant",  "Adamant"),
            java.util.Map.entry("naughty",  "Mauvais"),
            java.util.Map.entry("bold",     "Assuré"),
            java.util.Map.entry("docile",   "Docile"),
            java.util.Map.entry("relaxed",  "Relax"),
            java.util.Map.entry("impish",   "Malin"),
            java.util.Map.entry("lax",      "Lâche"),
            java.util.Map.entry("timid",    "Timide"),
            java.util.Map.entry("hasty",    "Pressé"),
            java.util.Map.entry("serious",  "Sérieux"),
            java.util.Map.entry("jolly",    "Jovial"),
            java.util.Map.entry("naive",    "Naïf"),
            java.util.Map.entry("modest",   "Modeste"),
            java.util.Map.entry("mild",     "Doux"),
            java.util.Map.entry("quiet",    "Calme"),
            java.util.Map.entry("bashful",  "Pudique"),
            java.util.Map.entry("rash",     "Foufou"),
            java.util.Map.entry("calm",     "Tranquille"),
            java.util.Map.entry("gentle",   "Gentil"),
            java.util.Map.entry("sassy",    "Fonceur"),
            java.util.Map.entry("careful",  "Prudent"),
            java.util.Map.entry("quirky",   "Bizarre")
    );

    /** Traduit une nature Cobblemon (ex: "cobblemon:adamant" ou "adamant") en français. */
    private static String translateNature(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // Retire le préfixe namespace si présent (ex: "cobblemon:adamant" → "adamant")
        String key = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        return NATURE_FR.getOrDefault(key.toLowerCase(), key);
    }

    // ── Tooltip info Pokémon ───────────────────────────────────────────────
    /**
     * Dessine un tooltip avec les informations du Pokémon à gagner.
     * À appeler quand la souris survole la zone centrale.
     */
    public static void drawPokemonInfoTooltip(DrawContext ctx, int mouseX, int mouseY,
                                               String displayName, boolean shiny,
                                               String nature, String ivs) {
        var tr = MinecraftClient.getInstance().textRenderer;

        // Construire les lignes du tooltip
        List<String> lines = new java.util.ArrayList<>();
        String nameLabel = (shiny ? "\u2605 " : "") + (displayName.isEmpty() ? "???" : displayName);
        lines.add(nameLabel);
        if (!nature.isEmpty()) lines.add("Nature : " + translateNature(nature));
        if (!ivs.isEmpty())    lines.add("IVs : " + ivs);

        int PAD = 5;
        int lineH = tr.fontHeight + 2;
        int tipW = 0;
        for (String line : lines) tipW = Math.max(tipW, tr.getWidth(line));
        tipW += PAD * 2;
        int tipH = lines.size() * lineH + PAD * 2 - 2;

        // Positionnement : au-dessus et à droite de la souris, recadré si sortie écran
        int tx = mouseX + 8;
        int ty = mouseY - tipH - 4;
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
        if (tx + tipW > screenW - 4) tx = mouseX - tipW - 8;
        if (ty < 4) ty = mouseY + 12;

        // Elève le Z au-dessus des items (Z=100+200=300 natif) et du ModelWidget Cobblemon
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400);

        // Fond semi-transparent foncé (opacité réduite)
        ctx.fill(tx - 1,       ty - 1,       tx + tipW + 1, ty + tipH + 1, 0x66000010);
        ctx.fill(tx,           ty,            tx + tipW,     ty + tipH,     0xAA050A18);

        // Bordure bleue
        ctx.fill(tx,           ty,            tx + tipW,     ty + 1,        0xFF4A90E2);
        ctx.fill(tx,           ty + tipH - 1, tx + tipW,     ty + tipH,     0xFF4A90E2);
        ctx.fill(tx,           ty,            tx + 1,        ty + tipH,     0xFF4A90E2);
        ctx.fill(tx + tipW - 1, ty,           tx + tipW,     ty + tipH,     0xFF4A90E2);

        // Texte
        for (int i = 0; i < lines.size(); i++) {
            int color;
            if (i == 0) {
                color = shiny ? 0xFFFFD700 : 0xFFEEEEEE;
            } else if (lines.get(i).startsWith("Nature")) {
                color = 0xFF7EC8E3;
            } else {
                color = 0xFF88FF88;
            }
            ctx.drawText(tr, Text.literal(lines.get(i)),
                    tx + PAD, ty + PAD + i * lineH, color, true);
        }

        ctx.getMatrices().pop();
    }

    // ── Panneau d'information Pokémon (face arrière du flip) ─────────────
    /**
     * Dessine le panneau d'informations textuelles du Pokémon dans la zone centrale.
     * Appelé pendant la phase 0.5→1 de l'animation de flip (après que la face avant
     * a pivoté hors champ). La mise à l'échelle du flip est gérée par l'appelant
     * via MatrixStack.
     *
     * @param ctx         DrawContext courant
     * @param guiX        coin haut-gauche du GUI
     * @param guiY        coin haut-gauche du GUI
     * @param displayName nom affiché du Pokémon
     * @param shiny       true si le Pokémon est shiny
     * @param nature      nature brute (ex: "cobblemon:adamant" ou "adamant")
     * @param ivs         IVs au format "X/X/X/X/X/X"
     */
    public static void drawCenterInfoPanel(DrawContext ctx, int guiX, int guiY,
                                           String displayName, boolean shiny,
                                           String nature, String ivs) {
        var tr = MinecraftClient.getInstance().textRenderer;

        // Zone interne (1px de marge sur la bordure existante de drawCenterZone)
        int cx = guiX + CasinoTextures.CENTER_TX + 1;
        int cy = guiY + CasinoTextures.CENTER_TY + 1;
        int cw = CasinoTextures.CENTER_TW - 2;
        int ch = CasinoTextures.CENTER_TH - 2;

        // Fond plein sombre recouvrant l'intérieur de la zone
        ctx.fill(cx, cy, cx + cw, cy + ch, 0xFF050A14);

        // Centre X absolu pour les textes centrés
        int centerX = guiX + CasinoTextures.CENTER_TX + CasinoTextures.CENTER_TW / 2;

        int lineY = cy + 8;
        final int LINE_H = tr.fontHeight + 3;

        // ── Nom du Pokémon ────────────────────────────────────────────────
        String nameLabel = (shiny ? "\u2605 " : "") + (displayName.isEmpty() ? "???" : displayName);
        int nameColor = shiny ? 0xFFFFD700 : 0xFFEEEEEE;
        ctx.drawCenteredTextWithShadow(tr, Text.literal(nameLabel), centerX, lineY, nameColor);
        lineY += LINE_H + 1;

        // ── Séparateur horizontal bleu ────────────────────────────────────
        ctx.fill(cx + 4, lineY, cx + cw - 4, lineY + 1, 0xFF4A90E2);
        lineY += 6;

        // ── Nature ────────────────────────────────────────────────────────
        if (!nature.isEmpty()) {
            ctx.drawCenteredTextWithShadow(tr, Text.literal("Nature"), centerX, lineY, 0xFF00E5FF);
            lineY += LINE_H;
            String natureFr = translateNature(nature);
            ctx.drawCenteredTextWithShadow(tr, Text.literal(natureFr), centerX, lineY, 0xFF7EC8E3);
            lineY += LINE_H + 3;
        }

        // ── Séparateur IVs ────────────────────────────────────────────────
        if (!ivs.isEmpty()) {
            ctx.fill(cx + 4, lineY, cx + cw - 4, lineY + 1, 0x44FFFFFF);
            lineY += 6;

            ctx.drawCenteredTextWithShadow(tr, Text.literal("IVs"), centerX, lineY, 0xFF00E5FF);
            lineY += LINE_H;

            // Découpe "X/X/X/X/X/X" en deux lignes de 3 stats
            String[] parts = ivs.split("/");
            if (parts.length == 6) {
                String line1 = parts[0] + " / " + parts[1] + " / " + parts[2];
                String line2 = parts[3] + " / " + parts[4] + " / " + parts[5];
                ctx.drawCenteredTextWithShadow(tr, Text.literal(line1), centerX, lineY, 0xFF88FF88);
                lineY += LINE_H;
                ctx.drawCenteredTextWithShadow(tr, Text.literal(line2), centerX, lineY, 0xFF88FF88);
            } else {
                // Format inattendu : affichage brut sur une ligne
                ctx.drawCenteredTextWithShadow(tr, Text.literal(ivs), centerX, lineY, 0xFF88FF88);
            }
        }
    }

    // ── Overlay de victoire (vert pulsant sur les 3 zones) ────────────────
    /**
     * Dessine un overlay coloré pulsant sur les trois zones (rouleaux + centre) lors d'un gain.
     * Affiche également le message "GAGNÉ !" centré sous la zone centrale.
     *
     * @param ctx   DrawContext courant
     * @param guiX  coin haut-gauche du GUI
     * @param guiY  coin haut-gauche du GUI
     * @param alpha intensité du pulse (0.0 – 1.0), interpolé par CasinoScreen
     */
    public static void drawWinOverlay(DrawContext ctx, int guiX, int guiY, float alpha) {
        // Clamp alpha, max opacité 200/255 pour ne pas masquer les icônes
        int a = Math.min(255, (int)(alpha * 200));
        int color = (a << 24) | 0x44FF88;

        // Rouleau gauche
        ctx.fill(
                guiX + CasinoTextures.SLOT_L_TX,
                guiY + CasinoTextures.SLOT_L_TY,
                guiX + CasinoTextures.SLOT_L_TX + CasinoTextures.SLOT_L_TW,
                guiY + CasinoTextures.SLOT_L_TY + CasinoTextures.SLOT_L_TH,
                color
        );
        // Zone centrale
        ctx.fill(
                guiX + CasinoTextures.CENTER_TX,
                guiY + CasinoTextures.CENTER_TY,
                guiX + CasinoTextures.CENTER_TX + CasinoTextures.CENTER_TW,
                guiY + CasinoTextures.CENTER_TY + CasinoTextures.CENTER_TH,
                color
        );
        // Rouleau droit
        ctx.fill(
                guiX + CasinoTextures.SLOT_R_TX,
                guiY + CasinoTextures.SLOT_R_TY,
                guiX + CasinoTextures.SLOT_R_TX + CasinoTextures.SLOT_R_TW,
                guiY + CasinoTextures.SLOT_R_TY + CasinoTextures.SLOT_R_TH,
                color
        );

        // Message "GAGNÉ !" centré sous la zone centrale, en or avec ombre
        var tr = MinecraftClient.getInstance().textRenderer;
        int cx = guiX + CasinoTextures.GUI_WIDTH / 2;
        int cy = guiY + CasinoTextures.CENTER_TY + CasinoTextures.CENTER_TH + 6;
        ctx.drawCenteredTextWithShadow(tr, Text.literal("\u2746 GAGN\u00C9 ! \u2746"), cx, cy, 0xFFFFD700);
    }
}
