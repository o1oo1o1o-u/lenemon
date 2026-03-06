package com.lenemon.client.ah.screen;

import com.lenemon.network.ah.AhActionPayload;
import com.lenemon.network.ah.AhBrowsePayload;
import com.lenemon.network.ah.AhListingDto;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AhBrowseScreen — écran principal de l'Hôtel des Ventes.
 * Layout (de haut en bas) :
 *   - Titre + balance                        y = guiY + 8
 *   - Séparateur                             y = guiY + 20
 *   - Boutons filtre (3) + tri (3)           y = guiY + 24
 *   - Séparateur                             y = guiY + 40
 *   - Grille 2×3 de cards                   y = guiY + 44 .. guiY + 44 + 3*(CARD_H+ROW_GAP)
 *   - Séparateur                             y = guiY + 44 + 3*(CARD_H+ROW_GAP) + 4
 *   - Pagination (Préc / Page X/Y / Suiv)   y = navY
 *   - Séparateur                             y = navY + BTN_H + 4
 *   - Boutons action (Mes ventes / Vendre item / Vendre Pokémon)
 */
public class AhBrowseScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_WIDTH  = 360;
    private static final int GUI_HEIGHT = 320;

    private static final int CARD_W  = 160;
    private static final int CARD_H  = 70;
    private static final int COLS    = 2;
    private static final int ROWS    = 3;
    private static final int COL_GAP = 8;
    private static final int ROW_GAP = 4;
    private static final int MARGIN_L = 12;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xCC0A0A1A;
    private static final int COL_BORDER     = 0xFF2255AA;
    private static final int COL_SEPARATOR  = 0x55FFFFFF;
    private static final int COL_CARD_BG    = 0x55113355;
    private static final int COL_CARD_BDR   = 0xFF334466;
    private static final int COL_CARD_HOVER = 0xFF4477CC;
    private static final int COL_CARD_MINE    = 0xFF225522;
    private static final int COL_CARD_CONFIRM = 0xFFFF8800;
    private static final int COL_BTN_SEL    = 0xFF1144AA;
    private static final int COL_BTN_SEL_BG = 0x55001166;

    // ── Taille des boutons ────────────────────────────────────────────────────
    private static final int BTN_H    = 13;
    private static final int BTN_W_SM = 50;  // filtre / tri / pagination
    private static final int BTN_W_MD = 72;  // boutons action bas

    // ── Données reçues ────────────────────────────────────────────────────────
    private AhBrowsePayload data;

    // État filtre/tri local (peut différer du payload si l'utilisateur vient de cliquer)
    private String currentFilter;
    private String currentSort;

    // ── Layout calculé dans init() ────────────────────────────────────────────
    private int guiX, guiY;
    private final List<CardRect> cards = new ArrayList<>();

    // Boutons filtre : Tous / Items / Pokémon
    private int filterAllX, filterItemX, filterPkmX, filterBtnY;
    // Boutons tri : Date mise / Date fin / Alpha
    private int sortDateListedX, sortDateExpiresX, sortAlphaX, sortBtnY;

    // Pagination
    private int btnPrevX, btnPrevY, btnNextX, btnNextY;
    private int pageTextX, pageTextY;

    // Boutons action bas
    private int btnMyX, btnMyY, btnSellX, btnSellY, btnPkmX, btnPkmY;

    // ── État interaction ──────────────────────────────────────────────────────
    private String confirmingId = null;

    // ── Tooltip ───────────────────────────────────────────────────────────────
    private List<Text> pendingTooltip = null;

    // ── ModelWidgets pokemon ──────────────────────────────────────────────────
    private final List<ModelWidget> cardWidgets = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public AhBrowseScreen(AhBrowsePayload data) {
        super(Text.literal("Hotel des Ventes"));
        this.data = data;
        this.currentFilter = data.filter();
        this.currentSort   = data.sort();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean shouldPause() { return false; }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        guiX = (this.width  - GUI_WIDTH)  / 2;
        guiY = (this.height - GUI_HEIGHT) / 2;

        cards.clear();
        cardWidgets.clear();

        // Boutons filtre — ligne y = guiY + 24
        filterBtnY   = guiY + 24;
        filterAllX   = guiX + MARGIN_L;
        filterItemX  = filterAllX + BTN_W_SM + 4;
        filterPkmX   = filterItemX + BTN_W_SM + 4;

        // Boutons tri — même ligne mais côté droit
        sortBtnY         = guiY + 24;
        sortAlphaX       = guiX + GUI_WIDTH - MARGIN_L - BTN_W_SM;
        sortDateExpiresX = sortAlphaX - BTN_W_SM - 4;
        sortDateListedX  = sortDateExpiresX - BTN_W_SM - 4;

        // Grille de cards — y = guiY + 44
        int startY = guiY + 44;

        List<AhListingDto> listings = data.listings();
        for (int i = 0; i < Math.min(listings.size(), COLS * ROWS); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx  = guiX + MARGIN_L + col * (CARD_W + COL_GAP);
            int cy  = startY + row * (CARD_H + ROW_GAP);
            cards.add(new CardRect(cx, cy, CARD_W, CARD_H, i));

            AhListingDto dto = listings.get(i);
            if ("pokemon".equals(dto.type()) && !dto.pokemonSpecies().isEmpty()) {
                var species = PokemonSpecies.INSTANCE.getByName(dto.pokemonSpecies().toLowerCase());
                if (species != null) {
                    Set<String> aspects = new HashSet<>(dto.pokemonAspects());
                    RenderablePokemon rp = new RenderablePokemon(species, aspects, ItemStack.EMPTY);
                    cardWidgets.add(new ModelWidget(cx + 2, cy + 2, 20, 20, rp, 0.5f, 0f, 0.0, false, false));
                } else {
                    cardWidgets.add(null);
                }
            } else {
                cardWidgets.add(null);
            }
        }

        // Zone après la grille
        int gridBottom = startY + ROWS * (CARD_H + ROW_GAP);

        // Pagination — y = gridBottom + 8
        int navY  = gridBottom + 8;
        btnPrevY  = navY;
        btnNextY  = navY;
        btnPrevX  = guiX + 10;
        btnNextX  = guiX + GUI_WIDTH - 10 - BTN_W_SM;
        pageTextX = guiX + GUI_WIDTH / 2;
        pageTextY = navY + (BTN_H - 8) / 2;

        // Boutons action — y = navY + BTN_H + 8
        int actY = navY + BTN_H + 8;
        btnMyX   = guiX + 8;
        btnMyY   = actY;
        btnSellX = guiX + 8 + BTN_W_MD + 4;
        btnSellY = actY;
        btnPkmX  = guiX + 8 + (BTN_W_MD + 4) * 2;
        btnPkmY  = actY;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        pendingTooltip = null;

        // Fond + bordure
        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, COL_BORDER);

        // Titre
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a76\u00a7l\uD83C\uDFAA Hotel des Ventes"),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);

        // Balance haut droite
        String balTxt = "\u00a76\u00a7f" + data.playerBalance() + "\u00a76\u20b1";
        ctx.drawTextWithShadow(textRenderer, Text.literal(balTxt),
                guiX + GUI_WIDTH - 8 - textRenderer.getWidth(balTxt), guiY + 8, 0xFFFFFF);

        // Séparateur titre
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_WIDTH - 10, guiY + 21, COL_SEPARATOR);

        // Boutons filtre + tri
        renderFilterSortButtons(ctx, mouseX, mouseY);

        // Séparateur sous les boutons
        ctx.fill(guiX + 10, guiY + 40, guiX + GUI_WIDTH - 10, guiY + 41, COL_SEPARATOR);

        // Grille de cards
        List<AhListingDto> listings = data.listings();
        for (int i = 0; i < cards.size(); i++) {
            CardRect rect = cards.get(i);
            AhListingDto dto = listings.get(rect.dtoIndex());
            boolean hover = isHover(mouseX, mouseY, rect.x(), rect.y(), rect.w(), rect.h());
            renderCard(ctx, rect, dto, hover, mouseX, mouseY, delta, i);
            if (hover) pendingTooltip = buildTooltip(dto);
        }

        if (listings.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a77Aucune vente disponible"),
                    guiX + GUI_WIDTH / 2, guiY + 110, 0xFFFFFF);
        }

        // Séparateur avant pagination
        int startY = guiY + 44;
        int gridBottom = startY + ROWS * (CARD_H + ROW_GAP);
        ctx.fill(guiX + 10, gridBottom + 2, guiX + GUI_WIDTH - 10, gridBottom + 3, COL_SEPARATOR);

        // Pagination
        renderPagination(ctx, mouseX, mouseY);

        // Séparateur avant boutons action
        int actSepY = btnMyY - 6;
        ctx.fill(guiX + 10, actSepY, guiX + GUI_WIDTH - 10, actSepY + 1, COL_SEPARATOR);

        // Boutons action bas
        renderActionButtons(ctx, mouseX, mouseY);

        // Tooltip
        if (pendingTooltip != null) {
            ctx.drawTooltip(textRenderer, pendingTooltip, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderFilterSortButtons(DrawContext ctx, int mouseX, int mouseY) {
        // ── Filtre ──
        drawSmallBtn(ctx, mouseX, mouseY, filterAllX,  filterBtnY, BTN_W_SM, "\u00a77Tous",
                "all".equals(currentFilter));
        drawSmallBtn(ctx, mouseX, mouseY, filterItemX, filterBtnY, BTN_W_SM, "\u00a7fItems",
                "item".equals(currentFilter));
        drawSmallBtn(ctx, mouseX, mouseY, filterPkmX,  filterBtnY, BTN_W_SM, "\u00a7dPokemon",
                "pokemon".equals(currentFilter));

        // ── Tri ──
        drawSmallBtn(ctx, mouseX, mouseY, sortDateListedX,  sortBtnY, BTN_W_SM, "\u00a77\u23F3 Recents",
                "date_listed".equals(currentSort));
        drawSmallBtn(ctx, mouseX, mouseY, sortDateExpiresX, sortBtnY, BTN_W_SM, "\u00a77\u23F1 Expire",
                "date_expires".equals(currentSort));
        drawSmallBtn(ctx, mouseX, mouseY, sortAlphaX,       sortBtnY, BTN_W_SM, "\u00a77A-Z",
                "alpha".equals(currentSort));
    }

    private void drawSmallBtn(DrawContext ctx, int mouseX, int mouseY,
                               int x, int y, int w, String label, boolean selected) {
        boolean hover = isHover(mouseX, mouseY, x, y, w, BTN_H);
        ctx.fill(x, y, x + w, y + BTN_H, selected ? COL_BTN_SEL_BG : COL_CARD_BG);
        ctx.drawBorder(x, y, w, BTN_H, selected ? COL_BTN_SEL : (hover ? COL_CARD_HOVER : COL_CARD_BDR));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(label),
                x + w / 2, y + (BTN_H - 8) / 2, 0xFFFFFF);
    }

    private void renderCard(DrawContext ctx, CardRect rect, AhListingDto dto,
                             boolean hover, int mouseX, int mouseY, float delta, int widgetIndex) {
        int x = rect.x(), y = rect.y(), w = rect.w(), h = rect.h();

        ctx.fill(x, y, x + w, y + h, COL_CARD_BG);

        int bdr = dto.listingId().equals(confirmingId) ? COL_CARD_CONFIRM
                : dto.isMine() ? COL_CARD_MINE
                : hover ? COL_CARD_HOVER
                : COL_CARD_BDR;
        ctx.drawBorder(x, y, w, h, bdr);

        // Icone
        int iconX = x + 4, iconY = y + 4;
        if ("pokemon".equals(dto.type())) {
            ModelWidget widget = (widgetIndex < cardWidgets.size()) ? cardWidgets.get(widgetIndex) : null;
            if (widget != null) {
                widget.render(ctx, mouseX, mouseY, delta);
            } else {
                ctx.drawItem(new ItemStack(Items.PAPER), iconX, iconY);
            }
        } else {
            net.minecraft.item.Item mcItem = Registries.ITEM.get(Identifier.of(dto.itemId()));
            if (mcItem == null || mcItem == Items.AIR) mcItem = Items.BARRIER;
            ctx.drawItem(new ItemStack(mcItem), iconX, iconY);
        }

        // Texte
        int tx = x + 26;
        int maxTx = w - 28;

        String rawName;
        if ("pokemon".equals(dto.type())) {
            rawName = (dto.pokemonShiny() ? "\u00a7e\u2746 \u00a7r" : "") +
                    "\u00a7b" + localizedPokemonName(dto) + " \u00a78Lv." + dto.pokemonLevel();
        } else {
            rawName = "\u00a7f" + localizedItemName(dto) +
                    (dto.itemCount() > 1 ? " \u00a78x" + dto.itemCount() : "");
        }
        var nameLines = textRenderer.wrapLines(Text.literal(rawName), maxTx);
        if (!nameLines.isEmpty()) ctx.drawTextWithShadow(textRenderer, nameLines.get(0), tx, y + 5, 0xFFFFFF);

        // Vendeur
        ctx.drawTextWithShadow(textRenderer, Text.literal("\u00a78" + dto.sellerName()), tx, y + 15, 0xFFFFFF);

        // Prix
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a7a" + dto.price() + "\u00a76\u20b1"), tx, y + 25, 0xFFFFFF);

        // Temps restant
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a77" + formatRemaining(dto.expiresAt())), tx, y + 35, 0xFFFFFF);

        // Nature pour pokemon
        if ("pokemon".equals(dto.type()) && !dto.pokemonNature().isEmpty()) {
            String natStr = "\u00a78" + AhTranslations.nature(dto.pokemonNature());
            ctx.drawTextWithShadow(textRenderer, Text.literal(natStr), tx, y + 45, 0xFFFFFF);
        }

        // Badge
        if (dto.isMine()) {
            String badge = "\u00a7e[MA VENTE]";
            ctx.drawTextWithShadow(textRenderer, Text.literal(badge),
                    x + w - textRenderer.getWidth(badge) - 3, y + h - 10, 0xFFFFFF);
        }
        if (dto.listingId().equals(confirmingId)) {
            String badge = "\u00a7e[Confirmer ?]";
            ctx.drawTextWithShadow(textRenderer, Text.literal(badge),
                    x + w - textRenderer.getWidth(badge) - 3, y + h - 10, 0xFFFFFF);
        }
    }

    private void renderPagination(DrawContext ctx, int mouseX, int mouseY) {
        if (data.page() > 0) {
            boolean hover = isHover(mouseX, mouseY, btnPrevX, btnPrevY, BTN_W_SM, BTN_H);
            ctx.fill(btnPrevX, btnPrevY, btnPrevX + BTN_W_SM, btnPrevY + BTN_H, COL_CARD_BG);
            ctx.drawBorder(btnPrevX, btnPrevY, BTN_W_SM, BTN_H, hover ? COL_CARD_HOVER : COL_CARD_BDR);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77< Préc"),
                    btnPrevX + BTN_W_SM / 2, btnPrevY + (BTN_H - 8) / 2, 0xFFFFFF);
        }
        if (data.totalPages() > 0) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a77" + (data.page() + 1) + "/" + data.totalPages()),
                    pageTextX, pageTextY, 0xFFFFFF);
        }
        if (data.page() < data.totalPages() - 1) {
            boolean hover = isHover(mouseX, mouseY, btnNextX, btnNextY, BTN_W_SM, BTN_H);
            ctx.fill(btnNextX, btnNextY, btnNextX + BTN_W_SM, btnNextY + BTN_H, COL_CARD_BG);
            ctx.drawBorder(btnNextX, btnNextY, BTN_W_SM, BTN_H, hover ? COL_CARD_HOVER : COL_CARD_BDR);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77Suiv >"),
                    btnNextX + BTN_W_SM / 2, btnNextY + (BTN_H - 8) / 2, 0xFFFFFF);
        }
    }

    private void renderActionButtons(DrawContext ctx, int mouseX, int mouseY) {
        boolean h1 = isHover(mouseX, mouseY, btnMyX, btnMyY, BTN_W_MD, BTN_H);
        ctx.fill(btnMyX, btnMyY, btnMyX + BTN_W_MD, btnMyY + BTN_H, COL_CARD_BG);
        ctx.drawBorder(btnMyX, btnMyY, BTN_W_MD, BTN_H, h1 ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77Mes ventes"),
                btnMyX + BTN_W_MD / 2, btnMyY + (BTN_H - 8) / 2, 0xFFFFFF);

        boolean h2 = isHover(mouseX, mouseY, btnSellX, btnSellY, BTN_W_MD, BTN_H);
        ctx.fill(btnSellX, btnSellY, btnSellX + BTN_W_MD, btnSellY + BTN_H, COL_CARD_BG);
        ctx.drawBorder(btnSellX, btnSellY, BTN_W_MD, BTN_H, h2 ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a7aVendre item"),
                btnSellX + BTN_W_MD / 2, btnSellY + (BTN_H - 8) / 2, 0xFFFFFF);

        boolean h3 = isHover(mouseX, mouseY, btnPkmX, btnPkmY, BTN_W_MD, BTN_H);
        ctx.fill(btnPkmX, btnPkmY, btnPkmX + BTN_W_MD, btnPkmY + BTN_H, COL_CARD_BG);
        ctx.drawBorder(btnPkmX, btnPkmY, BTN_W_MD, BTN_H, h3 ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a7dVendre Poke"),
                btnPkmX + BTN_W_MD / 2, btnPkmY + (BTN_H - 8) / 2, 0xFFFFFF);
    }

    // ── Mouse events ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        List<AhListingDto> listings = data.listings();

        // Clic sur une card
        for (CardRect rect : cards) {
            if (!isHover(mouseX, mouseY, rect.x(), rect.y(), rect.w(), rect.h())) continue;
            AhListingDto dto = listings.get(rect.dtoIndex());
            if ("pokemon".equals(dto.type())) {
                if (dto.listingId().equals(confirmingId)) {
                    ClientPlayNetworking.send(new AhActionPayload("buy:" + dto.listingId()));
                    confirmingId = null;
                } else {
                    confirmingId = dto.listingId();
                }
            } else {
                if (dto.listingId().equals(confirmingId)) {
                    ClientPlayNetworking.send(new AhActionPayload("buy:" + dto.listingId()));
                    confirmingId = null;
                } else {
                    confirmingId = dto.listingId();
                }
            }
            return true;
        }

        confirmingId = null;

        // Boutons filtre
        if (isHover(mouseX, mouseY, filterAllX, filterBtnY, BTN_W_SM, BTN_H)) {
            sendBrowse(0, "all", currentSort); return true;
        }
        if (isHover(mouseX, mouseY, filterItemX, filterBtnY, BTN_W_SM, BTN_H)) {
            sendBrowse(0, "item", currentSort); return true;
        }
        if (isHover(mouseX, mouseY, filterPkmX, filterBtnY, BTN_W_SM, BTN_H)) {
            sendBrowse(0, "pokemon", currentSort); return true;
        }

        // Boutons tri
        if (isHover(mouseX, mouseY, sortDateListedX, sortBtnY, BTN_W_SM, BTN_H)) {
            sendBrowse(0, currentFilter, "date_listed"); return true;
        }
        if (isHover(mouseX, mouseY, sortDateExpiresX, sortBtnY, BTN_W_SM, BTN_H)) {
            sendBrowse(0, currentFilter, "date_expires"); return true;
        }
        if (isHover(mouseX, mouseY, sortAlphaX, sortBtnY, BTN_W_SM, BTN_H)) {
            sendBrowse(0, currentFilter, "alpha"); return true;
        }

        // Pagination
        if (data.page() > 0 && isHover(mouseX, mouseY, btnPrevX, btnPrevY, BTN_W_SM, BTN_H)) {
            sendBrowse(data.page() - 1, currentFilter, currentSort); return true;
        }
        if (data.page() < data.totalPages() - 1
                && isHover(mouseX, mouseY, btnNextX, btnNextY, BTN_W_SM, BTN_H)) {
            sendBrowse(data.page() + 1, currentFilter, currentSort); return true;
        }

        // Boutons action
        if (isHover(mouseX, mouseY, btnMyX, btnMyY, BTN_W_MD, BTN_H)) {
            ClientPlayNetworking.send(new AhActionPayload("open_my_listings")); return true;
        }
        if (isHover(mouseX, mouseY, btnSellX, btnSellY, BTN_W_MD, BTN_H)) {
            ClientPlayNetworking.send(new AhActionPayload("open_sell_item")); return true;
        }
        if (isHover(mouseX, mouseY, btnPkmX, btnPkmY, BTN_W_MD, BTN_H)) {
            ClientPlayNetworking.send(new AhActionPayload("open_sell_pokemon")); return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sendBrowse(int page, String filter, String sort) {
        currentFilter = filter;
        currentSort   = sort;
        ClientPlayNetworking.send(new AhActionPayload(
                "browse:page:" + page + ":filter:" + filter + ":sort:" + sort));
    }

    private boolean isHover(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String formatRemaining(long expiresAt) {
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) return "\u00a7cExpiré";
        long hours   = remaining / 3_600_000L;
        long minutes = (remaining % 3_600_000L) / 60_000L;
        return hours + "h " + minutes + "m";
    }

    private List<Text> buildTooltip(AhListingDto dto) {
        List<Text> lines = new ArrayList<>();
        if ("pokemon".equals(dto.type())) {
            String shinyPfx = dto.pokemonShiny() ? "\u00a7e\u2746 \u00a7r" : "";
            lines.add(Text.literal(shinyPfx + "\u00a7b" + localizedPokemonName(dto)));
            lines.add(Text.literal("\u00a77Niveau : \u00a7f" + dto.pokemonLevel()));
            if (!dto.pokemonNature().isEmpty())
                lines.add(Text.literal("\u00a77Nature : \u00a7f" + AhTranslations.nature(dto.pokemonNature())));
            if (!dto.pokemonAbility().isEmpty())
                lines.add(Text.literal("\u00a77Talent : \u00a7f" + AhTranslations.ability(dto.pokemonAbility())));
            if (!dto.pokemonTypes().isEmpty())
                lines.add(Text.literal("\u00a77Type : \u00a7f" + AhTranslations.types(dto.pokemonTypes())));
            if (!dto.pokemonBall().isEmpty())
                lines.add(Text.literal("\u00a77Ball : \u00a7f" + AhTranslations.ball(dto.pokemonBall())));
            if (!dto.pokemonEvs().isEmpty() && !dto.pokemonEvs().equals("0/0/0/0/0/0"))
                lines.add(Text.literal("\u00a77EVs : \u00a7f" + dto.pokemonEvs()));
            if (!dto.pokemonMoves().isEmpty()) {
                lines.add(Text.literal("\u00a77Attaques :"));
                for (String m : dto.pokemonMoves())
                    lines.add(Text.literal("  \u00a7f" + AhTranslations.ability(m)));
            }
            lines.add(Text.literal("\u00a77Reproductible : " +
                    (dto.pokemonBreedable() ? "\u00a7aOui" : "\u00a7cNon")));
            if (dto.pokemonFriendship() > 0)
                lines.add(Text.literal("\u00a77Amitié : \u00a7f" + dto.pokemonFriendship()));
            lines.add(Text.literal("\u00a77Shiny : " + (dto.pokemonShiny() ? "\u00a7aOui" : "\u00a7cNon")));
            lines.add(Text.literal(""));
            if (!dto.listingId().equals(confirmingId))
                lines.add(Text.literal("\u00a7eCliquer pour confirmer l'achat"));
            else
                lines.add(Text.literal("\u00a7eCliquer à nouveau pour acheter !"));
        } else {
            lines.add(Text.literal("\u00a7f" + localizedItemName(dto)));
            if (dto.itemCount() > 1) lines.add(Text.literal("\u00a77Quantité : \u00a7f" + dto.itemCount()));
            lines.add(Text.literal(""));
            if (!dto.listingId().equals(confirmingId))
                lines.add(Text.literal("\u00a7eCliquer pour confirmer l'achat"));
            else
                lines.add(Text.literal("\u00a7eCliquer \u00e0 nouveau pour acheter !"));
        }
        lines.add(Text.literal("\u00a77Vendeur : \u00a7f" + dto.sellerName()));
        lines.add(Text.literal("\u00a7aPrix : " + dto.price() + " \u00a76\u20b1"));
        lines.add(Text.literal("\u00a77Expire : " + formatRemaining(dto.expiresAt())));
        return lines;
    }

    private String localizedItemName(AhListingDto dto) {
        String id = dto.itemId();
        if (id != null && !id.isEmpty()) {
            try {
                net.minecraft.item.Item item = Registries.ITEM.get(Identifier.of(id));
                if (item != null && item != Items.AIR) {
                    return item.getName().getString();
                }
            } catch (Exception ignored) {}
        }
        return dto.itemDisplayName();
    }

    private String localizedPokemonName(AhListingDto dto) {
        String species = dto.pokemonSpecies();
        if (species != null && !species.isEmpty()) {
            try {
                var sp = PokemonSpecies.INSTANCE.getByName(species.toLowerCase());
                if (sp != null) {
                    return sp.getTranslatedName().getString();
                }
            } catch (Exception ignored) {}
        }
        return dto.pokemonDisplayName();
    }

    // ── Types internes ────────────────────────────────────────────────────────

    private record CardRect(int x, int y, int w, int h, int dtoIndex) {}
}
