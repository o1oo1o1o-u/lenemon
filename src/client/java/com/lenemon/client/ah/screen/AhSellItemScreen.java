package com.lenemon.client.ah.screen;

import com.lenemon.network.ah.AhActionPayload;
import com.lenemon.network.ah.AhItemSlotDto;
import com.lenemon.network.ah.AhSellItemPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * AhSellItemScreen — écran de mise en vente d'un item de l'inventaire.
 *
 * Phase 0 : sélection de l'item dans la grille d'inventaire.
 * Phase 1 : saisie du prix (TextFieldWidget) et de la durée.
 */
public class AhSellItemScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int GUI_WIDTH  = 300;
    private static final int GUI_HEIGHT = 240;

    private static final int SLOT_W    = 20;
    private static final int SLOT_H    = 20;
    private static final int SLOT_COLS = 9;
    private static final int SLOT_ROWS = 4;
    private static final int SLOT_GAP  = 2;

    private static final int[] DURATIONS  = {6, 12, 24, 48, 72};
    private static final int DUR_BTN_W    = 38;
    private static final int DUR_BTN_H    = 14;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final int COL_BG         = 0xCC0A0A1A;
    private static final int COL_BORDER     = 0xFF2255AA;
    private static final int COL_SEPARATOR  = 0x55FFFFFF;
    private static final int COL_CARD_BG    = 0x55113355;
    private static final int COL_CARD_BDR   = 0xFF334466;
    private static final int COL_CARD_HOVER = 0xFF4477CC;
    private static final int COL_LABEL      = 0xAAAAAA;

    private static final int COL_SLOT_CAN    = COL_CARD_BG;
    private static final int COL_SLOT_CANNOT = 0x22888888;
    private static final int COL_SLOT_SEL    = 0x5500AA00;
    private static final int COL_SLOT_SEL_BDR= 0xFF22AA22;
    private static final int COL_BTN_SEL_BG  = 0x55004400;
    private static final int COL_BTN_SEL_BDR = 0xFF22AA22;

    // ── Données ───────────────────────────────────────────────────────────────
    private final AhSellItemPayload data;

    // ── État ─────────────────────────────────────────────────────────────────
    private int    phase        = 0;
    private int    selectedSlot = -1;
    private int    selectedDuration = 0;
    private long   avgListedPrice = 0L;
    private long   avgSoldPrice   = 0L;

    // ── Layout ────────────────────────────────────────────────────────────────
    private int guiX, guiY;

    private int gridStartX, gridStartY;

    // Phase 1
    private TextFieldWidget priceField;
    private int[] durBtnX, durBtnY;
    private int confirmBtnX, confirmBtnY;
    private int backBtnX,    backBtnY;
    private static final int BTN_H_CTRL = 14;
    private static final int BTN_W_CTRL = 72;

    // ─────────────────────────────────────────────────────────────────────────

    public AhSellItemScreen(AhSellItemPayload data) {
        super(Text.literal("Vendre un Objet"));
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

        // Grille
        int gridW = SLOT_COLS * SLOT_W + (SLOT_COLS - 1) * SLOT_GAP;
        gridStartX = guiX + (GUI_WIDTH - gridW) / 2;
        gridStartY = guiY + 30;

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
        confirmBtnY = guiY + GUI_HEIGHT - BTN_H_CTRL - 8;
        backBtnX    = guiX + 8;
        backBtnY    = guiY + GUI_HEIGHT - BTN_H_CTRL - 8;

        // TextFieldWidget pour le prix (phase 1)
        if (priceField == null) {
            priceField = new TextFieldWidget(textRenderer,
                    guiX + (GUI_WIDTH - 140) / 2, guiY + 90,
                    140, 16, Text.literal("Prix"));
            priceField.setMaxLength(12);
            priceField.setPlaceholder(Text.literal("\u00a78Ex: 1500 ou 1500.50"));
            // N'accepter que chiffres, point et virgule
            priceField.setTextPredicate(s -> s.matches("[0-9.,]*"));
        }

        if (phase == 1) {
            addDrawableChild(priceField);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, COL_BORDER);

        String title = phase == 0 ? "\u00a7fSélectionner un item" : "\u00a7fPrix et durée";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(title),
                guiX + GUI_WIDTH / 2, guiY + 8, 0xFFFFFF);
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_WIDTH - 10, guiY + 21, COL_SEPARATOR);

        if (phase == 0) {
            renderPhase0(ctx, mouseX, mouseY);
        } else {
            renderPhase1(ctx, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderPhase0(DrawContext ctx, int mouseX, int mouseY) {
        List<AhItemSlotDto> items = data.items();

        for (int row = 0; row < SLOT_ROWS; row++) {
            for (int col = 0; col < SLOT_COLS; col++) {
                int slotIndex = row * SLOT_COLS + col;
                int sx = gridStartX + col * (SLOT_W + SLOT_GAP);
                int sy = gridStartY + row * (SLOT_H + SLOT_GAP);

                AhItemSlotDto dto = findSlot(items, slotIndex);
                if (dto == null) {
                    ctx.fill(sx, sy, sx + SLOT_W, sy + SLOT_H, 0x33113355);
                    ctx.drawBorder(sx, sy, SLOT_W, SLOT_H, 0xFF222233);
                } else {
                    boolean canSell  = dto.canSell();
                    boolean selected = dto.slot() == selectedSlot;
                    boolean hover    = canSell && isHover(mouseX, mouseY, sx, sy, SLOT_W, SLOT_H);
                    ctx.fill(sx, sy, sx + SLOT_W, sy + SLOT_H,
                            selected ? COL_SLOT_SEL : (canSell ? COL_SLOT_CAN : COL_SLOT_CANNOT));
                    ctx.drawBorder(sx, sy, SLOT_W, SLOT_H,
                            selected ? COL_SLOT_SEL_BDR : (hover ? COL_CARD_HOVER : COL_CARD_BDR));
                    net.minecraft.item.Item mcItem = Registries.ITEM.get(Identifier.of(dto.itemId()));
                    if (mcItem == null || mcItem == Items.AIR) mcItem = Items.BARRIER;
                    ItemStack stack = new ItemStack(mcItem, dto.count());
                    ctx.drawItem(stack, sx + 2, sy + 2);
                    ctx.drawItemInSlot(textRenderer, stack, sx + 2, sy + 2);
                }
            }
        }

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("\u00a77Cliquez sur un item à vendre"),
                guiX + GUI_WIDTH / 2, gridStartY + SLOT_ROWS * (SLOT_H + SLOT_GAP) + 6, 0xFFFFFF);

        boolean hb = isHover(mouseX, mouseY, backBtnX, backBtnY, BTN_W_CTRL, BTN_H_CTRL);
        ctx.fill(backBtnX, backBtnY, backBtnX + BTN_W_CTRL, backBtnY + BTN_H_CTRL, COL_CARD_BG);
        ctx.drawBorder(backBtnX, backBtnY, BTN_W_CTRL, BTN_H_CTRL, hb ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77\u25c4 Retour"),
                backBtnX + BTN_W_CTRL / 2, backBtnY + (BTN_H_CTRL - 8) / 2, 0xFFFFFF);
    }

    private void renderPhase1(DrawContext ctx, int mouseX, int mouseY) {
        AhItemSlotDto selDto = findSlotBySlot(data.items(), selectedSlot);

        // Icone + nom item sélectionné
        int iconX = guiX + GUI_WIDTH / 2 - 8;
        int iconY = guiY + 26;
        if (selDto != null) {
            net.minecraft.item.Item mcItem = Registries.ITEM.get(Identifier.of(selDto.itemId()));
            if (mcItem == null || mcItem == Items.AIR) mcItem = Items.BARRIER;
            ctx.drawItem(new ItemStack(mcItem), iconX, iconY);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("\u00a7f" + selDto.displayName() + " \u00a77x" + selDto.count()),
                    guiX + GUI_WIDTH / 2, iconY + 18, 0xFFFFFF);
        }

        // Label + champ prix
        ctx.drawTextWithShadow(textRenderer, Text.literal("\u00a77Prix (₽) :"),
                guiX + (GUI_WIDTH - 140) / 2, guiY + 78, COL_LABEL);
        // Le widget se dessine via super.render → already added in init

        // Validation visuelle du champ
        String rawPrice = priceField.getText().replace(',', '.');
        long parsedPrice = 0;
        boolean priceValid = false;
        try {
            double d = Double.parseDouble(rawPrice);
            parsedPrice = (long) d;
            priceValid = parsedPrice > 0;
        } catch (NumberFormatException ignored) {}

        if (!rawPrice.isEmpty() && !priceValid) {
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal("\u00a7cNombre invalide"),
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

        // Prix de référence marché
        ctx.fill(guiX + 10, guiY + 163, guiX + GUI_WIDTH - 10, guiY + 164, COL_SEPARATOR);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a77March\u00e9 actif (unit\u00e9) : " +
                        (avgListedPrice > 0 ? "\u00a7e" + avgListedPrice + "\u00a76\u20b1" : "\u00a78Aucune vente active")),
                guiX + 10, guiY + 166, COL_LABEL);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a77Vendu (moy./ item) : " +
                        (avgSoldPrice > 0 ? "\u00a7a" + avgSoldPrice + "\u00a76\u20b1" : "\u00a78Aucun historique")),
                guiX + 10, guiY + 176, COL_LABEL);

        // Résumé
        int resumeY = guiY + GUI_HEIGHT - 42;
        ctx.fill(guiX + 10, resumeY, guiX + GUI_WIDTH - 10, resumeY + 1, COL_SEPARATOR);
        String priceStr = priceValid ? "\u00a7a" + parsedPrice + "\u00a76\u20b1" : "\u00a7c---";
        String durStr   = selectedDuration > 0 ? "\u00a7e" + selectedDuration + "h" : "\u00a7c---";
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("\u00a77Prix : " + priceStr + "  \u00a77Durée : " + durStr),
                guiX + 10, resumeY + 4, 0xFFFFFF);

        // Bouton retour
        boolean hb = isHover(mouseX, mouseY, backBtnX, backBtnY, BTN_W_CTRL, BTN_H_CTRL);
        ctx.fill(backBtnX, backBtnY, backBtnX + BTN_W_CTRL, backBtnY + BTN_H_CTRL, COL_CARD_BG);
        ctx.drawBorder(backBtnX, backBtnY, BTN_W_CTRL, BTN_H_CTRL, hb ? COL_CARD_HOVER : COL_CARD_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a77\u25c4 Retour"),
                backBtnX + BTN_W_CTRL / 2, backBtnY + (BTN_H_CTRL - 8) / 2, 0xFFFFFF);

        // Bouton confirmer
        boolean canConfirm = priceValid && selectedDuration > 0;
        boolean hc = canConfirm && isHover(mouseX, mouseY, confirmBtnX, confirmBtnY, BTN_W_CTRL, BTN_H_CTRL);
        ctx.fill(confirmBtnX, confirmBtnY, confirmBtnX + BTN_W_CTRL, confirmBtnY + BTN_H_CTRL,
                canConfirm ? 0x55003300 : 0x33333333);
        ctx.drawBorder(confirmBtnX, confirmBtnY, BTN_W_CTRL, BTN_H_CTRL,
                hc ? 0xFF22AA22 : (canConfirm ? 0xFF226622 : 0xFF444444));
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(canConfirm ? "\u00a7aConfirmer" : "\u00a78Confirmer"),
                confirmBtnX + BTN_W_CTRL / 2, confirmBtnY + (BTN_H_CTRL - 8) / 2, 0xFFFFFF);
    }

    // ── Mouse events ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (phase == 0) {
            List<AhItemSlotDto> items = data.items();
            for (int row = 0; row < SLOT_ROWS; row++) {
                for (int col = 0; col < SLOT_COLS; col++) {
                    int slotIndex = row * SLOT_COLS + col;
                    int sx = gridStartX + col * (SLOT_W + SLOT_GAP);
                    int sy = gridStartY + row * (SLOT_H + SLOT_GAP);
                    if (isHover(mouseX, mouseY, sx, sy, SLOT_W, SLOT_H)) {
                        AhItemSlotDto dto = findSlot(items, slotIndex);
                        if (dto != null && dto.canSell()) {
                            selectedSlot = dto.slot();
                            phase = 1;
                            init(); // re-init pour ajouter le TextFieldWidget
                            priceField.setFocused(true);
                            // Demander les prix du marché pour cet item
                            ClientPlayNetworking.send(new com.lenemon.network.ah.AhRequestPricePayload(dto.itemId()));
                            return true;
                        }
                    }
                }
            }
            if (isHover(mouseX, mouseY, backBtnX, backBtnY, BTN_W_CTRL, BTN_H_CTRL)) {
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
            if (isHover(mouseX, mouseY, backBtnX, backBtnY, BTN_W_CTRL, BTN_H_CTRL)) {
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
                    && isHover(mouseX, mouseY, confirmBtnX, confirmBtnY, BTN_W_CTRL, BTN_H_CTRL)) {
                ClientPlayNetworking.send(new AhActionPayload(
                        "sell_item:" + selectedSlot + ":" + parsedPrice + ":" + selectedDuration));
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

    private AhItemSlotDto findSlot(List<AhItemSlotDto> items, int listIndex) {
        if (listIndex < 0 || listIndex >= items.size()) return null;
        return items.get(listIndex);
    }

    private AhItemSlotDto findSlotBySlot(List<AhItemSlotDto> items, int slot) {
        for (AhItemSlotDto dto : items) {
            if (dto.slot() == slot) return dto;
        }
        return null;
    }
}
