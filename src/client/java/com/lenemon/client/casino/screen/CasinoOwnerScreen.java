package com.lenemon.client.casino.screen;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.lenemon.casino.network.CasinoOwnerDataPayload;
import com.lenemon.casino.network.CasinoOwnerSavePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Set;

/**
 * The type Casino owner screen.
 */
public class CasinoOwnerScreen extends Screen {

    private static final int GUI_WIDTH  = 256;
    private static final int GUI_HEIGHT = 256;

    private final CasinoOwnerDataPayload data;

    // Widgets
    private TextFieldWidget priceField;
    private TextFieldWidget chanceField;

    // Sélection pokemon party
    private int selectedPartyIndex = -1; // -1 = aucun sélectionné (garde l'actuel)
    private boolean removePokemon = false;

    // ModelWidgets
    private ModelWidget currentPokemonWidget = null;
    private final ModelWidget[] partyWidgets;

    private int guiX, guiY;

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Ne pas appeler super → pas de blur, pas de fond sombre vanilla
    }


    /**
     * Instantiates a new Casino owner screen.
     *
     * @param data the data
     */
    public CasinoOwnerScreen(CasinoOwnerDataPayload data) {
        super(Text.literal("Configuration Casino"));
        this.data = data;
        this.partyWidgets = new ModelWidget[data.party().size()];
    }

    @Override
    protected void init() {
        guiX = (this.width  - GUI_WIDTH)  / 2;
        guiY = (this.height - GUI_HEIGHT) / 2;

        // ── Champs texte ─────────────────────────────────────────
        priceField = new TextFieldWidget(
                this.textRenderer,
                guiX + 14, guiY + 186, 106, 16,
                Text.literal("Prix")
        );
        priceField.setMaxLength(10);
        priceField.setText(data.currentPrice() > 0 ? String.valueOf(data.currentPrice()) : "");
        priceField.setPlaceholder(Text.literal("§7Prix (PokéCoins)"));
        this.addSelectableChild(priceField);

        chanceField = new TextFieldWidget(
                this.textRenderer,
                guiX + 136, guiY + 186, 106, 16,
                Text.literal("Chance")
        );
        chanceField.setMaxLength(6);
        chanceField.setText(data.currentChance() > 0 ? String.valueOf(data.currentChance()) : "");
        chanceField.setPlaceholder(Text.literal("§7% gain"));
        this.addSelectableChild(chanceField);

        // ── Bouton Valider ────────────────────────────────────────
        this.addDrawableChild(new net.minecraft.client.gui.widget.ButtonWidget.Builder(
                Text.literal("§a§lValider"),
                btn -> onValidate()
        ).dimensions(guiX + 88, guiY + 230, 80, 18).build());

        // ── Bouton Retirer pokemon ────────────────────────────────
        if (!data.pokemonSpecies().isEmpty() && !data.pokemonDisplayName().isEmpty()) {
            this.addDrawableChild(new net.minecraft.client.gui.widget.ButtonWidget.Builder(
                    Text.literal("§c⊗ Retirer"),
                    btn -> {
                        removePokemon = true;
                        selectedPartyIndex = -2;
                        currentPokemonWidget = null;
                        btn.active = false;
                    }
            ).dimensions(guiX + 176, guiY + 34, 66, 14).build());
        }

        // ── ModelWidget pokemon EN JEU ────────────────────────────
        if (!data.pokemonSpecies().isEmpty() && !removePokemon) {
            buildCurrentPokemonWidget();
        }

        // ── ModelWidgets party ────────────────────────────────────
        buildPartyWidgets();
    }

    private void buildCurrentPokemonWidget() {
        if (data.pokemonSpecies().isEmpty() || data.pokemonDisplayName().isEmpty()) return;
        var species = PokemonSpecies.INSTANCE.getByName(data.pokemonSpecies().toLowerCase());
        if (species == null) return;
        RenderablePokemon rp = new RenderablePokemon(species, data.pokemonAspects(), net.minecraft.item.ItemStack.EMPTY);
        currentPokemonWidget = new ModelWidget(
                guiX + 88, guiY + 28, 80, 80,
                rp, 1.5f, 0f, 0.0, false, true
        );
    }

    private void buildPartyWidgets() {
        List<CasinoOwnerDataPayload.PartyPokemonData> party = data.party();
        int slotW   = 36;
        int totalW  = party.size() * slotW;
        int offsetX = (GUI_WIDTH - totalW) / 2;

        for (int i = 0; i < party.size(); i++) {
            var p = party.get(i);
            var species = PokemonSpecies.INSTANCE.getByName(p.species().toLowerCase());
            if (species == null) continue;
            RenderablePokemon rp = new RenderablePokemon(species, p.aspects(), net.minecraft.item.ItemStack.EMPTY);
            partyWidgets[i] = new ModelWidget(
                    guiX + offsetX + i * slotW, guiY + 118,
                    slotW, 48,
                    rp, 0.8f, 0f, 0.0, false, false
            );
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Fond + bordure
        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xCC0A0A1A);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 0xFF2255AA);

        // ── Titre ─────────────────────────────────────────────────
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b⚙ Configuration Casino"),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);

        // ── Séparateur titre ──────────────────────────────────────
        ctx.fill(guiX + 10, guiY + 18, guiX + GUI_WIDTH - 10, guiY + 19, 0x55FFFFFF);

        // ── Section pokemon en jeu ────────────────────────────────
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7Pokémon en jeu :"),
                guiX + 10, guiY + 24, 0xAAAAAA);

        if (removePokemon || data.pokemonSpecies().isEmpty() || data.pokemonDisplayName().isEmpty()) {
            ctx.fill(guiX + 88, guiY + 28, guiX + 168, guiY + 108, 0x33113355);
            ctx.drawBorder(guiX + 88, guiY + 28, 80, 80, 0xFF334466);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§8Aucun"),
                    guiX + 128, guiY + 64, 0x666666);
        } else {
            if (currentPokemonWidget != null) {
                currentPokemonWidget.render(ctx, mouseX, mouseY, delta);
            }
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§b" + data.pokemonDisplayName()),
                    guiX + 128, guiY + 112, 0xFFFFFF);
        }

        // ── Séparateur ────────────────────────────────────────────
        ctx.fill(guiX + 10, guiY + 114, guiX + GUI_WIDTH - 10, guiY + 115, 0x55FFFFFF);

        // ── Section party ─────────────────────────────────────────
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7Votre party :"),
                guiX + 10, guiY + 120, 0xAAAAAA);

        List<CasinoOwnerDataPayload.PartyPokemonData> party = data.party();
        int totalW  = party.size() * 36;
        int offsetX = (GUI_WIDTH - totalW) / 2;

        for (int i = 0; i < partyWidgets.length; i++) {
            int px = guiX + offsetX + i * 36;
            int py = guiY + 118;

            ctx.fill(px, py, px + 34, py + 48, 0x55113355);
            if (selectedPartyIndex == i) {
                ctx.drawBorder(px, py, 34, 48, 0xFF44FF44);
            } else {
                ctx.drawBorder(px, py, 34, 48, 0xFF334466);
            }

            if (partyWidgets[i] != null) {
                partyWidgets[i].render(ctx, mouseX, mouseY, delta);
            }
        }

        // party nom tooltip
        for (int i = 0; i < party.size(); i++) {
            int px = guiX + offsetX + i * 36;
            int py = guiY + 118;

            if (mouseX >= px && mouseX <= px + 34 && mouseY >= py && mouseY <= py + 48) {
                var p = party.get(i);
                java.util.List<Text> tooltip = new java.util.ArrayList<>();
                tooltip.add(Text.literal((p.shiny() ? "§e✦ " : "") + "§b" + p.displayName()));
                tooltip.add(Text.literal("§7Nature : §f" + p.nature()));
                tooltip.add(Text.literal("§7Shiny : " + (p.shiny() ? "§aOui" : "§cNon")));
                tooltip.add(Text.literal("§7IVs : §f" + p.ivs()));
                ctx.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
            }
        }

        // ── Séparateur ────────────────────────────────────────────
        ctx.fill(guiX + 10, guiY + 178, guiX + GUI_WIDTH - 10, guiY + 179, 0x55FFFFFF);

        // ── Labels champs ─────────────────────────────────────────
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7Prix (PokéCoins) :"),
                guiX + 14, guiY + 176, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7% de gain :"),
                guiX + 136, guiY + 176, 0xAAAAAA);

        priceField.render(ctx, mouseX, mouseY, delta);
        chanceField.render(ctx, mouseX, mouseY, delta);

        // ── Erreur validation ─────────────────────────────────────
        String error = getValidationError();
        if (!error.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§c" + error),
                    guiX + GUI_WIDTH / 2, guiY + 216, 0xFF4444);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Click sur un slot party
        List<CasinoOwnerDataPayload.PartyPokemonData> party = data.party();
        int totalW = party.size() * 36;
        int offsetX = (GUI_WIDTH - totalW) / 2;

        for (int i = 0; i < party.size(); i++) {
            int px = guiX + offsetX + i * 36;
            int py = guiY + 90;
            if (mouseX >= px && mouseX <= px + 34 && mouseY >= py && mouseY <= py + 50) {
                selectedPartyIndex = (selectedPartyIndex == i) ? -1 : i; // toggle
                removePokemon = false;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String getValidationError() {
        String priceStr  = priceField.getText().trim();
        String chanceStr = chanceField.getText().trim();

        if (priceStr.isEmpty()) return "Prix requis";
        try { long p = Long.parseLong(priceStr); if (p <= 0) return "Prix > 0"; }
        catch (NumberFormatException e) { return "Prix invalide"; }

        if (chanceStr.isEmpty()) return "% de gain requis";
        try {
            double c = Double.parseDouble(chanceStr.replace(",", "."));
            if (c <= 0 || c > 100) return "% entre 0.01 et 100";
        } catch (NumberFormatException e) { return "% invalide"; }

        // Plus besoin de pokemon obligatoire — casino peut être en mode "configuré sans pokemon"
        return "";
    }

    private void onValidate() {
        if (!getValidationError().isEmpty()) return;

        long price = Long.parseLong(priceField.getText().trim());
        double chance = Double.parseDouble(chanceField.getText().trim().replace(",", "."));

        ClientPlayNetworking.send(new CasinoOwnerSavePayload(
                price, chance, selectedPartyIndex, removePokemon
        ));

        this.close();
    }

    @Override
    public boolean shouldPause() { return false; }
}