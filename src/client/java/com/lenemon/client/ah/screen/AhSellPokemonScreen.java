package com.lenemon.client.ah.screen;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.lenemon.network.ah.AhActionPayload;
import com.lenemon.network.ah.AhPartyPokemonDto;
import com.lenemon.network.ah.AhRequestPokemonPricePayload;
import com.lenemon.network.ah.AhSellPokemonPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AhSellPokemonScreen — écran de mise en vente d'un Pokémon de la party.
 *
 * Phase 0 : sélection du Pokémon dans la grille de party (6 emplacements max).
 * Phase 1 : saisie du prix (TextFieldWidget) et de la durée.
 */
public class AhSellPokemonScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_WIDTH  = 320;
    private static final int GUI_HEIGHT = 240;

    private static final int CARD_W   = 90;
    private static final int CARD_H   = 62;
    private static final int CARD_GAP = 6;

    private static final int[] DURATIONS = {6, 12, 24, 48, 72};
    private static final int DUR_BTN_W   = 38;
    private static final int DUR_BTN_H   = 14;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xCC0A0A1A;
    private static final int COL_BORDER     = 0xFF2255AA;
    private static final int COL_SEPARATOR  = 0x55FFFFFF;
    private static final int COL_CARD_BG    = 0x55113355;
    private static final int COL_CARD_BDR   = 0xFF334466;
    private static final int COL_CARD_HOVER = 0xFF4477CC;
    private static final int COL_LABEL      = 0xAAAAAA;

    private static final int COL_CARD_SEL    = 0x5500AA00;
    private static final int COL_CARD_SEL_BDR= 0xFF22AA22;
    private static final int COL_BTN_SEL_BG  = 0x55004400;
    private static final int COL_BTN_SEL_BDR = 0xFF22AA22;

    // ── Données ───────────────────────────────────────────────────────────────
    private final AhSellPokemonPayload data;

    // ── État ─────────────────────────────────────────────────────────────────
    private int  phase              = 0;
    private int  selectedPartyIndex = -1;
    private int  selectedDuration   = 0;
    private long avgListedPrice     = 0L;
    private long avgSoldPrice       = 0L;

    // ── Layout ────────────────────────────────────────────────────────────────
    private int guiX, guiY;

    private final List<CardLayout>  partyCards   = new ArrayList<>();
    private final List<ModelWidget> partyWidgets = new ArrayList<>();

    private TextFieldWidget priceField;
    private int[] durBtnX, durBtnY;
    private int confirmBtnX, confirmBtnY;
    private int backBtnX,    backBtnY;
    private static final int BTN_W = 72;
    private static final int BTN_H = 14;

    // Tooltip
    private List<Text> pendingTooltip = null;

    // ─────────────────────────────────────────────────────────────────────────

    public AhSellPokemonScreen(AhSellPokemonPayload data) {
        super(Text.literal("Vendre un Pokémon"));
        this.data = data;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean shouldPause() { return false; }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init(); // réinitialise la liste des children
        guiX = (this.width  - GUI_WIDTH)  / 2;
        guiY = (this.height - GUI_HEIGHT) / 2;

        partyCards.clear();
        partyWidgets.clear();

        List<AhPartyPokemonDto> party = data.party();
        int partyCount = Math.min(party.size(), 6);
        int cols = Math.min(Math.max(partyCount, 1), 3);
        int gridW  = cols * CARD_W + (cols - 1) * CARD_GAP;
        int startX = guiX + (GUI_WIDTH - gridW) / 2;
        int startY = guiY + 30;

        for (int i = 0; i < partyCount; i++) {
            int col = i % cols, row = i / cols;
            int cx  = startX + col * (CARD_W + CARD_GAP);
            int cy  = startY + row * (CARD_H + CARD_GAP);
            partyCards.add(new CardLayout(cx, cy, CARD_W, CARD_H, i));

            AhPartyPokemonDto dto = party.get(i);
            if (!dto.species().isEmpty()) {
                var species = PokemonSpecies.INSTANCE.getByName(dto.species().toLowerCase());
                if (species != null) {
                    Set<String> aspects = new HashSet<>(dto.aspects());
                    RenderablePokemon rp = new RenderablePokemon(species, aspects, ItemStack.EMPTY);
                    partyWidgets.add(new ModelWidget(cx + 2, cy + 2, 32, 32, rp, 0.6f, 0f, 0.0, false, false));
                } else {
                    partyWidgets.add(null);
                }
            } else {
                partyWidgets.add(null);
            }
        }

        // Boutons durée
        int durStartX = guiX + (GUI_WIDTH - (DURATIONS.length * (DUR_BTN_W + 4) - 4)) / 2;
        int durStartY = guiY + 148;
        durBtnX = new int[DURATIONS.length];
        durBtnY = new int[DURATIONS.length];
        for (int i = 0; i < DURATIONS.length; i++) {
            durBtnX[i] = durStartX + i * (DUR_BTN_W + 4);
            durBtnY[i] = durStartY;
        }

        // Boutons confirmer / retour
        confirmBtnX = guiX + GUI_WIDTH / 2 + 4;
        confirmBtnY = guiY + GUI_HEIGHT - BTN_H - 8;
        backBtnX    = guiX + 8;
        backBtnY    = guiY + GUI_HEIGHT - BTN_H - 8;

        // TextFieldWidget prix
        if (priceField == null) {
            priceField = new TextFieldWidget(textRenderer,
                    guiX + (GUI_WIDTH - 140) / 2, guiY + 90,
                    140, 16, Text.literal("Prix"));
            priceField.setMaxLength(12);
            priceField.setPlaceholder(Text.literal("\u00a78Ex: 5000 ou 5000.50"));
            priceField.setTextPredicate(s -> s.matches("[0-9.,]*"));
        }

        if (phase == 1) {
            addDrawableChild(priceField);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        pendingTooltip = null;

        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, COL_BORDER);

        String title = phase == 0 ? "\u00a7dSélectionner un Pokémon" : "\u00a7dPrix et durée";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(title),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_WIDTH - 10, guiY + 21, COL_SEPARATOR);

        if (phase == 0) {
            renderPhase0(ctx, mouseX, mouseY, delta);
        } else {
            renderPhase1(ctx, mouseX, mouseY);
        }

        if (pendingTooltip != null) {
            ctx.drawTooltip(textRenderer, pendingTooltip, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderPhase0(DrawContext ctx, int mouseX, int mouseY, float delta) {
        List<AhPartyPokemonDto> party = data.party();

        if (party.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a7cVotre party est vide"),
                    guiX + GUI_WIDTH / 2, guiY + 60, 0xFFFFFF);
        } else {
            for (int i = 0; i < partyCards.size(); i++) {
                CardLayout card = partyCards.get(i);
                AhPartyPokemonDto dto = party.get(card.partyIdx());
                boolean hover    = isHover(mouseX, mouseY, card.x(), card.y(), card.w(), card.h());
                boolean selected = dto.partyIndex() == selectedPartyIndex;

                ctx.fill(card.x(), card.y(), card.x() + card.w(), card.y() + card.h(),
                        selected ? COL_CARD_SEL : COL_CARD_BG);
                ctx.drawBorder(card.x(), card.y(), card.w(), card.h(),
                        selected ? COL_CARD_SEL_BDR : (hover ? COL_CARD_HOVER : COL_CARD_BDR));

                ModelWidget widget = (i < partyWidgets.size()) ? partyWidgets.get(i) : null;
                if (widget != null) widget.render(ctx, mouseX, mouseY, delta);
                else ctx.drawItem(new ItemStack(Items.PAPER), card.x() + 4, card.y() + 4);

                // Nom + niveau
                int tx = card.x() + 36;
                int maxW = card.w() - 38;
                String shinyPfx = dto.shiny() ? "\u00a7e\u2746 " : "";
                var nameLines = textRenderer.wrapLines(
                        Text.literal(shinyPfx + "\u00a7b" + dto.displayName()), maxW);
                if (!nameLines.isEmpty())
                    ctx.drawTextWithShadow(textRenderer, nameLines.get(0), tx, card.y() + 5, 0xFFFFFF);

                ctx.drawTextWithShadow(textRenderer,
                        Text.literal("\u00a77Lv." + dto.level()), tx, card.y() + 15, 0xFFFFFF);

                // Nature en français
                String natureFr = AhTranslations.nature(dto.nature());
                String natureStr = "\u00a78" + natureFr;
                var natureLines = textRenderer.wrapLines(Text.literal(natureStr), maxW);
                if (!natureLines.isEmpty())
                    ctx.drawTextWithShadow(textRenderer, natureLines.get(0), tx, card.y() + 25, 0xFFFFFF);

                // Types en français
                if (!dto.types().isEmpty()) {
                    String typeStr = "\u00a77" + AhTranslations.types(dto.types());
                    var typeLines = textRenderer.wrapLines(Text.literal(typeStr), maxW);
                    if (!typeLines.isEmpty())
                        ctx.drawTextWithShadow(textRenderer, typeLines.get(0), tx, card.y() + 35, 0xFFFFFF);
                }

                if (hover) pendingTooltip = buildPokemonTooltip(dto);
            }
        }

        // Bouton retour
        boolean hb = isHover(mouseX, mouseY, backBtnX, backBtnY, BTN_W, BTN_H);
        ctx.fill(backBtnX, backBtnY, backBtnX + BTN_W, backBtnY + BTN_H, COL_CARD_BG);
        ctx.drawBorder(backBtnX, backBtnY, BTN_W, BTN_H, hb ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77\u25c4 Retour"),
                backBtnX + BTN_W / 2, backBtnY + (BTN_H - 8) / 2, 0xFFFFFF);
    }

    private void renderPhase1(DrawContext ctx, int mouseX, int mouseY) {
        AhPartyPokemonDto selDto = findByPartyIndex(selectedPartyIndex);
        if (selDto != null) {
            String shinyPfx = selDto.shiny() ? "\u00a7e\u2746 \u00a7r" : "";
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(shinyPfx + "\u00a7b" + selDto.displayName() + " \u00a77Lv." + selDto.level()),
                    guiX + GUI_WIDTH / 2, guiY + 26, 0xFFFFFF);
        }

        // Label + champ prix
        ctx.drawTextWithShadow(textRenderer, Text.literal("\u00a77Prix (₽) :"),
                guiX + (GUI_WIDTH - 140) / 2, guiY + 78, COL_LABEL);

        // Validation prix
        String rawPrice = priceField.getText().replace(',', '.');
        long parsedPrice = 0;
        boolean priceValid = false;
        try {
            double d = Double.parseDouble(rawPrice);
            parsedPrice = (long) d;
            priceValid = parsedPrice > 0;
        } catch (NumberFormatException ignored) {}

        if (!rawPrice.isEmpty() && !priceValid) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("\u00a7cNombre invalide"),
                    guiX + (GUI_WIDTH - 140) / 2, guiY + 108, 0xFFFFFF);
        } else if (priceValid) {
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal("\u00a7a= " + parsedPrice + "\u00a76\u20b1"),
                    guiX + (GUI_WIDTH - 140) / 2, guiY + 108, 0xFFFFFF);
        }

        // Séparateur
        ctx.fill(guiX + 10, guiY + 140, guiX + GUI_WIDTH - 10, guiY + 141, COL_SEPARATOR);

        // Label durée
        ctx.drawTextWithShadow(textRenderer, Text.literal("\u00a77Durée :"),
                guiX + 8, guiY + 143, COL_LABEL);

        // Boutons durée
        for (int i = 0; i < DURATIONS.length; i++) {
            boolean sel   = selectedDuration == DURATIONS[i];
            boolean hover = isHover(mouseX, mouseY, durBtnX[i], durBtnY[i], DUR_BTN_W, DUR_BTN_H);
            ctx.fill(durBtnX[i], durBtnY[i], durBtnX[i] + DUR_BTN_W, durBtnY[i] + DUR_BTN_H,
                    sel ? COL_BTN_SEL_BG : COL_CARD_BG);
            ctx.drawBorder(durBtnX[i], durBtnY[i], DUR_BTN_W, DUR_BTN_H,
                    sel ? COL_BTN_SEL_BDR : (hover ? COL_CARD_HOVER : COL_CARD_BDR));
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a7f" + DURATIONS[i] + "h"),
                    durBtnX[i] + DUR_BTN_W / 2, durBtnY[i] + (DUR_BTN_H - 8) / 2, 0xFFFFFF);
        }

        // Prix de référence marché Pokémon
        AhPartyPokemonDto selDtoForPrice = findByPartyIndex(selectedPartyIndex);
        boolean isShiny = selDtoForPrice != null && selDtoForPrice.shiny();
        ctx.fill(guiX + 10, guiY + 163, guiX + GUI_WIDTH - 10, guiY + 164, COL_SEPARATOR);
        String priceLabel = isShiny ? "\u00a77March\u00e9 shiny :" : "\u00a77March\u00e9 normal :";
        ctx.drawTextWithShadow(textRenderer, Text.literal(priceLabel),
                guiX + 8, guiY + 166, COL_LABEL);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a77Actif : " +
                        (avgListedPrice > 0 ? "\u00a7e" + avgListedPrice + "\u00a76\u20b1" : "\u00a78N/A") +
                        "  \u00a77Vendu : " +
                        (avgSoldPrice > 0 ? "\u00a7a" + avgSoldPrice + "\u00a76\u20b1" : "\u00a78N/A")),
                guiX + 8, guiY + 176, COL_LABEL);

        // Résumé
        int resumeY = guiY + GUI_HEIGHT - 38;
        ctx.fill(guiX + 10, resumeY, guiX + GUI_WIDTH - 10, resumeY + 1, COL_SEPARATOR);
        String priceStr = priceValid ? "\u00a7a" + parsedPrice + "\u00a76\u20b1" : "\u00a7c---";
        String durStr   = selectedDuration > 0 ? "\u00a7e" + selectedDuration + "h" : "\u00a7c---";
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a77Prix : " + priceStr + "  \u00a77Durée : " + durStr),
                guiX + 10, resumeY + 4, 0xFFFFFF);

        // Bouton retour
        boolean hb = isHover(mouseX, mouseY, backBtnX, backBtnY, BTN_W, BTN_H);
        ctx.fill(backBtnX, backBtnY, backBtnX + BTN_W, backBtnY + BTN_H, COL_CARD_BG);
        ctx.drawBorder(backBtnX, backBtnY, BTN_W, BTN_H, hb ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77\u25c4 Retour"),
                backBtnX + BTN_W / 2, backBtnY + (BTN_H - 8) / 2, 0xFFFFFF);

        // Bouton confirmer
        boolean canConfirm = priceValid && selectedDuration > 0;
        boolean hc = canConfirm && isHover(mouseX, mouseY, confirmBtnX, confirmBtnY, BTN_W, BTN_H);
        ctx.fill(confirmBtnX, confirmBtnY, confirmBtnX + BTN_W, confirmBtnY + BTN_H,
                canConfirm ? 0x55220044 : 0x33333333);
        ctx.drawBorder(confirmBtnX, confirmBtnY, BTN_W, BTN_H,
                hc ? 0xFFAA22CC : (canConfirm ? 0xFF662288 : 0xFF444444));
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(canConfirm ? "\u00a7dConfirmer" : "\u00a78Confirmer"),
                confirmBtnX + BTN_W / 2, confirmBtnY + (BTN_H - 8) / 2, 0xFFFFFF);
    }

    // ── Mouse events ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (phase == 0) {
            List<AhPartyPokemonDto> party = data.party();
            for (CardLayout card : partyCards) {
                if (isHover(mouseX, mouseY, card.x(), card.y(), card.w(), card.h())) {
                    AhPartyPokemonDto selected = party.get(card.partyIdx());
                    selectedPartyIndex = selected.partyIndex();
                    avgListedPrice = 0L;
                    avgSoldPrice   = 0L;
                    phase = 1;
                    init(); // re-init pour ajouter le TextFieldWidget
                    priceField.setFocused(true);
                    // Demander les prix du marché pour ce Pokémon
                    if (!selected.species().isEmpty()) {
                        ClientPlayNetworking.send(new AhRequestPokemonPricePayload(selected.species(), selected.shiny()));
                    }
                    return true;
                }
            }
            if (isHover(mouseX, mouseY, backBtnX, backBtnY, BTN_W, BTN_H)) {
                ClientPlayNetworking.send(new AhActionPayload("open_ah"));
                return true;
            }

        } else {
            // Boutons durée
            for (int i = 0; i < DURATIONS.length; i++) {
                if (isHover(mouseX, mouseY, durBtnX[i], durBtnY[i], DUR_BTN_W, DUR_BTN_H)) {
                    selectedDuration = DURATIONS[i];
                    return true;
                }
            }

            // Bouton retour
            if (isHover(mouseX, mouseY, backBtnX, backBtnY, BTN_W, BTN_H)) {
                phase = 0;
                init(); // re-init pour retirer le TextFieldWidget
                return true;
            }

            // Bouton confirmer
            String rawPrice = priceField.getText().replace(',', '.');
            long parsedPrice = 0;
            boolean priceValid = false;
            try {
                double d = Double.parseDouble(rawPrice);
                parsedPrice = (long) d;
                priceValid = parsedPrice > 0;
            } catch (NumberFormatException ignored) {}

            if (priceValid && selectedDuration > 0
                    && isHover(mouseX, mouseY, confirmBtnX, confirmBtnY, BTN_W, BTN_H)) {
                ClientPlayNetworking.send(new AhActionPayload(
                        "sell_pokemon:" + selectedPartyIndex + ":" + parsedPrice + ":" + selectedDuration));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /** Appelé par le receiver S2C quand le serveur répond avec les prix du marché. */
    public void updatePriceInfo(long avgListed, long avgSold) {
        this.avgListedPrice = avgListed;
        this.avgSoldPrice   = avgSold;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isHover(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private AhPartyPokemonDto findByPartyIndex(int partyIndex) {
        for (AhPartyPokemonDto dto : data.party()) {
            if (dto.partyIndex() == partyIndex) return dto;
        }
        return null;
    }

    private List<Text> buildPokemonTooltip(AhPartyPokemonDto dto) {
        List<Text> lines = new ArrayList<>();
        String shinyPfx = dto.shiny() ? "\u00a7e\u2746 \u00a7r" : "";
        lines.add(Text.literal(shinyPfx + "\u00a7b" + dto.displayName()));
        lines.add(Text.literal("\u00a77Niveau : \u00a7f" + dto.level()));
        lines.add(Text.literal("\u00a77Nature : \u00a7f" + AhTranslations.nature(dto.nature())));
        lines.add(Text.literal("\u00a77Talent : \u00a7f" + AhTranslations.ability(dto.ability())));
        if (!dto.types().isEmpty())
            lines.add(Text.literal("\u00a77Type : \u00a7f" + AhTranslations.types(dto.types())));
        if (!dto.ball().isEmpty())
            lines.add(Text.literal("\u00a77Ball : \u00a7f" + AhTranslations.ball(dto.ball())));
        lines.add(Text.literal("\u00a77IVs : \u00a7f" + dto.ivs()));
        if (!dto.evs().isEmpty() && !dto.evs().equals("0/0/0/0/0/0"))
            lines.add(Text.literal("\u00a77EVs : \u00a7f" + dto.evs()));
        if (!dto.moves().isEmpty()) {
            lines.add(Text.literal("\u00a77Attaques :"));
            for (String m : dto.moves())
                lines.add(Text.literal("  \u00a7f" + AhTranslations.ability(m)));
        }
        lines.add(Text.literal("\u00a77Reproductible : " + (dto.breedable() ? "\u00a7aOui" : "\u00a7cNon")));
        if (dto.friendship() > 0)
            lines.add(Text.literal("\u00a77Amitié : \u00a7f" + dto.friendship()));
        lines.add(Text.literal("\u00a77Shiny : " + (dto.shiny() ? "\u00a7aOui" : "\u00a7cNon")));
        return lines;
    }

    // ── Types internes ────────────────────────────────────────────────────────

    private record CardLayout(int x, int y, int w, int h, int partyIdx) {}
}
