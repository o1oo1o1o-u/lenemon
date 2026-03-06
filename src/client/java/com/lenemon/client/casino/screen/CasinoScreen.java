package com.lenemon.client.casino.screen;

import com.lenemon.casino.network.CasinoAnimDonePayload;
import com.lenemon.casino.network.CasinoSpinRequestPayload;
import com.lenemon.casino.screen.CasinoScreenHandler;
import com.lenemon.casino.util.CasinoTextures;
import com.lenemon.client.casino.render.CasinoRenderer;
import com.lenemon.client.casino.render.SlotReel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;

import java.util.Set;

/**
 * The type Casino screen.
 */
public class CasinoScreen extends HandledScreen<CasinoScreenHandler> {

    // ── Réels ─────────────────────────────────────────────────────────────
    private final SlotReel reelLeft  = new SlotReel(CasinoTextures.ICON_COUNT);
    private final SlotReel reelRight = new SlotReel(CasinoTextures.ICON_COUNT);

    // ── État visuel ───────────────────────────────────────────────────────
    private boolean btnPressed    = false;
    private boolean spinLocked    = false; // true pendant l'animation et après un win
    private boolean animDoneSent  = false; // ping serveur envoyé une seule fois
    private boolean hasWon        = false; // true si le dernier spin était un win

    // Délai avant stopOn
    private static final int STOP_DELAY_TICKS = 80;
    private int  stopDelayCounter = 0;
    private boolean stopScheduled = false;

    // Win overlay
    private float   winAlpha   = 0f;
    private boolean winPulsing = false;
    private int     winTick    = 0;

    // Résultat affiché (debug + message)
    private String resultMessage = "";

    private ModelWidget modelWidget = null;

    /**
     * Instantiates a new Casino screen.
     *
     * @param handler   the handler
     * @param inventory the inventory
     * @param title     the title
     */
    public CasinoScreen(CasinoScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth  = CasinoTextures.GUI_WIDTH;
        this.backgroundHeight = CasinoTextures.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        spinLocked    = false;
        animDoneSent  = false;
        hasWon        = false;
        resultMessage = "";

        // Remplit pendingSpecies depuis le handler AVANT de rebuild
        String species = handler.getPokemonSpecies();
        if (!species.isEmpty()) {
            this.pendingSpecies  = species;
            this.pendingAspects  = handler.getPokemonAspects();
        }

        //rebuildModelWidget();

        ClientPlayNetworking.send(new com.lenemon.casino.network.CasinoCanSpinRequestPayload());
    }

    // ── Rendu fond ────────────────────────────────────────────────────────
    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        CasinoRenderer.drawBackground(ctx, x, y);
        CasinoRenderer.drawSlotBackgrounds(ctx, x, y);

        boolean glow = winPulsing;

        CasinoRenderer.drawSlotIcons(ctx, x, y,
                CasinoTextures.SLOT_L_TX, CasinoTextures.SLOT_L_TY,
                CasinoTextures.SLOT_L_TW, CasinoTextures.SLOT_L_TH,
                reelLeft, glow, this.client.getWindow().getScaleFactor());

        CasinoRenderer.drawSlotIcons(ctx, x, y,
                CasinoTextures.SLOT_R_TX, CasinoTextures.SLOT_R_TY,
                CasinoTextures.SLOT_R_TW, CasinoTextures.SLOT_R_TH,
                reelRight, glow, this.client.getWindow().getScaleFactor());

        CasinoRenderer.drawSpinButton(ctx, x, y, btnPressed && canStartSpin());
        CasinoRenderer.drawBetBox(ctx, x, y, (int) handler.getEntryPrice());

        if (winPulsing) {
            CasinoRenderer.drawWinOverlay(ctx, x, y, winAlpha);
        }

        // Nom du Pokémon
//        if (!handler.getPokemonName().isEmpty()) {
//            ctx.drawCenteredTextWithShadow(
//                    this.client.textRenderer,
//                    Text.literal("§b" + handler.getPokemonName()),
//                    x + CasinoTextures.CENTER_TX + CasinoTextures.CENTER_TW / 2,
//                    y + CasinoTextures.CENTER_TY - 10,
//                    0xFFFFFF
//            );
//        }
        if(!hasWon) {
            if (modelWidget != null) {
                modelWidget.render(ctx, mouseX, mouseY, delta);

            }
        }

        // DEBUG : affiche "GAGNÉ" si win, ou message résultat
        if (hasWon) {
            ctx.drawCenteredTextWithShadow(
                    this.client.textRenderer,
                    Text.literal("§a§lGAGNÉ !"),
                    x + backgroundWidth / 2,
                    y + backgroundHeight - 20,
                    0xFFFFFF
            );
        } else if (!resultMessage.isEmpty()) {
            ctx.drawCenteredTextWithShadow(
                    this.client.textRenderer,
                    Text.literal("§7" + resultMessage),
                    x + backgroundWidth / 2,
                    y + backgroundHeight - 20,
                    0xFFFFFF
            );
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {

        // ── Tick réels ───────────────────────────────────────────────────
        reelLeft.tick();
        reelRight.tick();

        // ── Délai avant stopOn ───────────────────────────────────────────
        if (stopScheduled) {
            stopDelayCounter++;
            if (stopDelayCounter >= STOP_DELAY_TICKS) {
                stopScheduled = false;
                reelLeft.stopOn(handler.getResultLeft());
                reelRight.stopOn(handler.getResultRight());
            }
        }

        // ── Détection fin d'animation → ping serveur ─────────────────────
        if (!animDoneSent
                && !stopScheduled
                && reelLeft.isIdle()
                && reelRight.isIdle()
                && handler.getSpinState() != CasinoScreenHandler.SpinState.IDLE) {

            animDoneSent = true;
            handler.resetState();

            // Ping serveur : l'anim est finie, résous et envoie le résultat
            ClientPlayNetworking.send(new CasinoAnimDonePayload());
        }

        // ── Win overlay pulse ────────────────────────────────────────────
        if (winPulsing) {
            winTick++;
            winAlpha = (float)(Math.sin(winTick * 0.15f) * 0.5f + 0.5f);
            if (winTick > 80) {
                winPulsing = false;
                winAlpha   = 0f;
            }
        }

        //this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Inputs ────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverSpinButton(mouseX, mouseY) && canStartSpin()) {
            btnPressed = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean wasPressed = btnPressed;
            btnPressed = false;
            if (wasPressed && isOverSpinButton(mouseX, mouseY) && canStartSpin()) {
                startSpin();
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean canStartSpin() {
        if (spinLocked) return false;
        if (stopScheduled) return false;
        if (!reelLeft.isIdle() || !reelRight.isIdle()) return false;
        if (handler.getSpinState() != CasinoScreenHandler.SpinState.IDLE) return false;
        return handler.canSpin(); // source de vérité : mis à jour par setCanSpin
    }

    private boolean isOverSpinButton(double mouseX, double mouseY) {
        int cx = this.x + CasinoTextures.BTN_CX;
        int cy = this.y + CasinoTextures.BTN_CY;
        int r  = CasinoTextures.BTN_W / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        return dx * dx + dy * dy <= r * r;
    }

    // ── Démarrage du spin ─────────────────────────────────────────────────
    private void startSpin() {
        spinLocked   = true;
        animDoneSent = false;
        hasWon       = false;
        resultMessage = "";
        winPulsing   = false;
        winAlpha     = 0f;
        winTick      = 0;

        handler.requestSpin();
        ClientPlayNetworking.send(new CasinoSpinRequestPayload());

        reelLeft.spin();
        reelRight.spin();
        stopScheduled    = true;
        stopDelayCounter = 0;
    }

    // ── Appelé par le network client quand le serveur répond ─────────────

    /**
     * Réponse can_spin : met à jour le handler + déverrouille si autorisé  @param allowed the allowed
     *
     * @param allowed the allowed
     * @param balance the balance
     * @param price   the price
     */
    public void setSpinEnabled(boolean allowed, long balance, long price) {
        handler.setCanSpin(allowed, price, balance, false);
        // Ne déverrouille PAS spinLocked ici : seul onSpinResult le fait après un lose
    }

    /**
     * Réponse du serveur après CasinoAnimDonePayload  @param win the win
     *
     * @param win     the win
     * @param message the message
     */
    public void onSpinResult(boolean win, String message) {
        if (win) {
            hasWon     = true;
            winPulsing = true;
            winTick    = 0;
            spinLocked = true; // reste bloqué : le pokemon est parti
            handler.setCanSpin(false, handler.getEntryPrice(), 0, false);
        } else {
            hasWon        = false;
            resultMessage = message;
            spinLocked    = false; // déverrouille pour pouvoir rejouer
            // Re-demande au serveur si on a encore les sous
            ClientPlayNetworking.send(new com.lenemon.casino.network.CasinoCanSpinRequestPayload());
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // vide intentionnel
    }

    // Méthode appelée par le network :
//    public void updatePokemonModel(String speciesName, Set<String> aspects) {
//        var species = com.cobblemon.mod.common.api.pokemon.PokemonSpecies.INSTANCE.getByName(speciesName);
//        if (species == null) return;
//
//        RenderablePokemon renderable = new RenderablePokemon(species, aspects, net.minecraft.item.ItemStack.EMPTY);
//
//        // Position dans le GUI — ajuste selon ta zone centrale
//        int px = this.x + CasinoTextures.CENTER_TX;
//        int py = this.y + CasinoTextures.CENTER_TY;
//        int pw = CasinoTextures.CENTER_TW;
//        int ph = CasinoTextures.CENTER_TH;
//
//        modelWidget = new ModelWidget(
//                px, py, pw, ph,
//                renderable,
//                2.0f,   // baseScale — ajuste
//                0f,     // rotationY
//                0.0,    // offsetY
//                true,   // playCryOnClick
//                true    // shouldFollowCursor
//        );
//    }
    // Stocke juste les données, ne crée pas le widget ici
    private String pendingSpecies = "";
    private Set<String> pendingAspects = new java.util.HashSet<>();

    /**
     * Update pokemon model.
     *
     * @param speciesName the species name
     * @param aspects     the aspects
     */
    public void updatePokemonModel(String speciesName, Set<String> aspects) {
        this.pendingSpecies = speciesName;
        this.pendingAspects = aspects;
        rebuildModelWidget();
    }

    private void rebuildModelWidget() {
        var species = com.cobblemon.mod.common.api.pokemon.PokemonSpecies.INSTANCE
                .getByName(pendingSpecies.toLowerCase());
//        net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
//                net.minecraft.text.Text.literal("§a[DEBUG] Species: " + (species != null ? species.getName() : "NULL")
//                        + " | pending: " + pendingSpecies
//                        + " | x=" + this.x + " y=" + this.y), false);
        if (pendingSpecies.isEmpty()) return;
        if (this.x == 0 && this.y == 0) return; // pas encore initialisé

        //var species = com.cobblemon.mod.common.api.pokemon.PokemonSpecies.INSTANCE.getByName(pendingSpecies);

        if (species == null) {
            net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
                    net.minecraft.text.Text.literal("§c[DEBUG] Species introuvable : " + pendingSpecies), false);
            return;
        }

        RenderablePokemon renderable = new RenderablePokemon(
                species, pendingAspects, net.minecraft.item.ItemStack.EMPTY);

        int px = this.x + CasinoTextures.CENTER_TX;
        int py = this.y + CasinoTextures.CENTER_TY;
        int pw = CasinoTextures.CENTER_TW;
        int ph = CasinoTextures.CENTER_TH;

        modelWidget = new ModelWidget(px, py, pw, ph, renderable,
                2.0f, 0f, 0.0, true, true);
    }
}