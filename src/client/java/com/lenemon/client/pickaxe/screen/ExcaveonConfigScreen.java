package com.lenemon.client.pickaxe.screen;

import com.lenemon.item.pickaxe.ExcaveonLevel;
import com.lenemon.item.pickaxe.ExcaveonPickaxe;
import com.lenemon.item.pickaxe.ExcaveonUserConfig;
import com.lenemon.network.pickaxe.ExcaveonOpenGuiPayload;
import com.lenemon.network.pickaxe.ExcaveonUserConfigPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/**
 * GUI de configuration de la pioche Excaveon.
 * Ouverture : shift+clic droit en l'air.
 * 6 modes de minage : 1x1, 3x3x1, 3x3x2, 3x3x3, 4x4x3, 5x5x3
 */
public class ExcaveonConfigScreen extends Screen {

    // ── Dimensions ──────────────────────────────────────────────────────────
    private static final int GUI_W = 340;
    private static final int GUI_H = 240;

    // ── Palette ─────────────────────────────────────────────────────────────
    private static final int COL_BG          = 0xCC0A0A1A;
    private static final int COL_BORDER      = 0xFF2255AA;
    private static final int COL_SEPARATOR   = 0x55FFFFFF;
    private static final int COL_CARD_BG     = 0x55113355;
    private static final int COL_CARD_BDR    = 0xFF334466;
    private static final int COL_SELECTED    = 0xFF44FF44;
    private static final int COL_HOVER       = 0xFF4477CC;
    private static final int COL_LOCKED_BG   = 0x55330000;
    private static final int COL_LOCKED_BDR  = 0xFF443333;
    private static final int COL_LABEL       = 0xAAAAAA;
    private static final int COL_CONFIRM_BG  = 0x55003300;
    private static final int COL_CONFIRM_BDR = 0xFF116611;
    private static final int COL_CONFIRM_HOV = 0xFF22AA22;
    private static final int COL_CLOSE_BG    = 0x55330000;
    private static final int COL_CLOSE_BDR   = 0xFF443333;
    private static final int COL_CLOSE_HOV   = 0xFFAA2222;

    // ── Modes (6 zones, une par niveau + 3x3x1 intermédiaire) ──────────────
    private static final String[] MODES = {
            ExcaveonPickaxe.MODE_1X1,
            ExcaveonPickaxe.MODE_3X3X1,
            ExcaveonPickaxe.MODE_3X3X2,
            ExcaveonPickaxe.MODE_3X3X3,
            ExcaveonPickaxe.MODE_5X5X2,
            ExcaveonPickaxe.MODE_5X5X3
    };
    private static final String[] MODE_LABELS = { "1x1", "3x3x1", "3x3x2", "3x3x3", "5x5x2", "5x5x3" };
    private static final int MODE_COUNT = MODES.length;

    private static final int MODE_BTN_W = 48;
    private static final int MODE_BTN_H = 20;
    private static final int MODE_BTN_GAP = 4;

    private final int[] modeBtnX = new int[MODE_COUNT];
    private final int[] modeBtnY = new int[MODE_COUNT];

    // ── Toggles ─────────────────────────────────────────────────────────────
    private int autoSellX, autoSellY, autoSmeltX, autoSmeltY;
    private static final int TOGGLE_W = 140;
    private static final int TOGGLE_H = 18;

    // ── Boutons action ───────────────────────────────────────────────────────
    private int confirmX, confirmY;
    private static final int CONFIRM_W = 110;
    private static final int CONFIRM_H = 20;
    private int closeX, closeY;
    private static final int CLOSE_W = 70;
    private static final int CLOSE_H = 20;

    // ── Layout ───────────────────────────────────────────────────────────────
    private int guiX, guiY;

    // ── Données ──────────────────────────────────────────────────────────────
    private final int level;
    private final int blocks;
    private String  selectedMode;
    private boolean autoSellOn;
    private boolean autoSmeltOn;

    public ExcaveonConfigScreen(ExcaveonOpenGuiPayload payload) {
        super(Text.literal("Config Pioche"));
        this.level        = payload.level();
        this.blocks       = payload.blocks();
        this.selectedMode = payload.cfgMiningMode();
        this.autoSellOn   = payload.cfgAutoSell();
        this.autoSmeltOn  = payload.cfgAutoSmelt();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Pas de flou vanilla
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Init ────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        guiX = (this.width  - GUI_W) / 2;
        guiY = (this.height - GUI_H) / 2;

        // Ligne de boutons de mode (6 boutons centrés)
        int totalModesW = MODE_COUNT * MODE_BTN_W + (MODE_COUNT - 1) * MODE_BTN_GAP;
        int modeRowX = guiX + (GUI_W - totalModesW) / 2;
        int modeRowY = guiY + 68;
        for (int i = 0; i < MODE_COUNT; i++) {
            modeBtnX[i] = modeRowX + i * (MODE_BTN_W + MODE_BTN_GAP);
            modeBtnY[i] = modeRowY;
        }

        // Toggles (centrés, sous les modes)
        int optCenterX = guiX + GUI_W / 2;
        int toggleY = modeRowY + MODE_BTN_H + 18;
        autoSellX  = optCenterX - TOGGLE_W - 5;
        autoSellY  = toggleY;
        autoSmeltX = optCenterX + 5;
        autoSmeltY = toggleY;

        // Boutons Confirmer / Fermer
        int btnRowY = guiY + GUI_H - 28;
        confirmX = guiX + (GUI_W / 2) - CONFIRM_W - 5;
        confirmY = btnRowY;
        closeX   = guiX + (GUI_W / 2) + 5;
        closeY   = btnRowY;
    }

    // ── Render ──────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Fond + bordure
        ctx.fill(guiX, guiY, guiX + GUI_W, guiY + GUI_H, COL_BG);
        ctx.drawBorder(guiX, guiY, GUI_W, GUI_H, COL_BORDER);

        // Titre
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b§lConfiguration Nymphalie"),
                guiX + GUI_W / 2, guiY + 8, 0xFFFFFF);

        // Séparateur haut
        ctx.fill(guiX + 10, guiY + 20, guiX + GUI_W - 10, guiY + 21, COL_SEPARATOR);

        // Niveau / blocs
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§aNiveau §f" + level + "  §8|  §7Blocs cassés : §f" + blocks),
                guiX + GUI_W / 2, guiY + 26, 0xFFFFFF);

        // Label zone
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Zone de minage :"),
                guiX + GUI_W / 2, guiY + 52, COL_LABEL);

        // Boutons de mode
        for (int i = 0; i < MODE_COUNT; i++) {
            renderModeButton(ctx, i, mouseX, mouseY);
        }

        // Séparateur milieu
        int sepY = modeBtnY[0] + MODE_BTN_H + 8;
        ctx.fill(guiX + 10, sepY, guiX + GUI_W - 10, sepY + 1, COL_SEPARATOR);

        // Label options
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Options :"),
                guiX + GUI_W / 2, autoSellY - 12, COL_LABEL);

        // Toggles
        ExcaveonLevel lvl = ExcaveonLevel.fromLevel(level);
        renderToggle(ctx, autoSellX,  autoSellY,  TOGGLE_W, TOGGLE_H, "Auto-vente",   autoSellOn,  lvl.autoSell, mouseX, mouseY);
        renderToggle(ctx, autoSmeltX, autoSmeltY, TOGGLE_W, TOGGLE_H, "Auto-cuisson", autoSmeltOn, true,         mouseX, mouseY);

        // Séparateur bas
        int statsY = autoSellY + TOGGLE_H + 10;
        ctx.fill(guiX + 10, statsY, guiX + GUI_W - 10, statsY + 1, COL_SEPARATOR);

        // Info prochain niveau
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(level >= 5 ? "§6✦ Niveau maximum atteint !" : "§7Continuez à miner pour progresser..."),
                guiX + GUI_W / 2, statsY + 4, COL_LABEL);

        // Bouton Confirmer
        boolean confirmHov = isOver(mouseX, mouseY, confirmX, confirmY, CONFIRM_W, CONFIRM_H);
        ctx.fill(confirmX, confirmY, confirmX + CONFIRM_W, confirmY + CONFIRM_H, confirmHov ? COL_CONFIRM_HOV : COL_CONFIRM_BG);
        ctx.drawBorder(confirmX, confirmY, CONFIRM_W, CONFIRM_H, COL_CONFIRM_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§aConfirmer"),
                confirmX + CONFIRM_W / 2, confirmY + (CONFIRM_H - 8) / 2, 0xFFFFFF);

        // Bouton Fermer
        boolean closeHov = isOver(mouseX, mouseY, closeX, closeY, CLOSE_W, CLOSE_H);
        ctx.fill(closeX, closeY, closeX + CLOSE_W, closeY + CLOSE_H, closeHov ? COL_CLOSE_HOV : COL_CLOSE_BG);
        ctx.drawBorder(closeX, closeY, CLOSE_W, CLOSE_H, COL_CLOSE_BDR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§cFermer"),
                closeX + CLOSE_W / 2, closeY + (CLOSE_H - 8) / 2, 0xFFFFFF);

        // Tooltips modes verrouillés
        for (int i = 0; i < MODE_COUNT; i++) {
            if (!ExcaveonUserConfig.isModeUnlocked(MODES[i], level)
                    && isOver(mouseX, mouseY, modeBtnX[i], modeBtnY[i], MODE_BTN_W, MODE_BTN_H)) {
                ctx.drawTooltip(textRenderer,
                        List.of(Text.literal("§cMode verrouillé"),
                                Text.literal("§7Débloqué au niveau §e" + ExcaveonUserConfig.modeUnlockLevel(MODES[i]))),
                        mouseX, mouseY);
            }
        }
        // Tooltip auto-vente verrouillée
        if (!lvl.autoSell && isOver(mouseX, mouseY, autoSellX, autoSellY, TOGGLE_W, TOGGLE_H)) {
            ctx.drawTooltip(textRenderer,
                    List.of(Text.literal("§cAuto-vente verrouillée"),
                            Text.literal("§7Débloquée au niveau §e2")),
                    mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderModeButton(DrawContext ctx, int i, int mouseX, int mouseY) {
        int bx = modeBtnX[i], by = modeBtnY[i];
        String mode  = MODES[i];
        String label = MODE_LABELS[i];
        boolean unlocked = ExcaveonUserConfig.isModeUnlocked(mode, level);
        boolean selected = mode.equals(selectedMode);
        boolean hover    = unlocked && isOver(mouseX, mouseY, bx, by, MODE_BTN_W, MODE_BTN_H);

        int bg  = unlocked ? (selected ? 0x55003300 : COL_CARD_BG) : COL_LOCKED_BG;
        int bdr = unlocked ? (selected ? COL_SELECTED : (hover ? COL_HOVER : COL_CARD_BDR)) : COL_LOCKED_BDR;

        ctx.fill(bx, by, bx + MODE_BTN_W, by + MODE_BTN_H, bg);
        ctx.drawBorder(bx, by, MODE_BTN_W, MODE_BTN_H, bdr);

        String txt = unlocked ? (selected ? "§a§l" + label : "§f" + label) : "§8" + label;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(txt),
                bx + MODE_BTN_W / 2, by + (MODE_BTN_H - 8) / 2, 0xFFFFFF);
    }

    private void renderToggle(DrawContext ctx, int x, int y, int w, int h,
                               String label, boolean enabled, boolean unlocked,
                               int mouseX, int mouseY) {
        boolean hover = unlocked && isOver(mouseX, mouseY, x, y, w, h);
        int bg  = unlocked ? COL_CARD_BG : COL_LOCKED_BG;
        int bdr = unlocked ? (hover ? COL_HOVER : COL_CARD_BDR) : COL_LOCKED_BDR;

        ctx.fill(x, y, x + w, y + h, bg);
        ctx.drawBorder(x, y, w, h, bdr);

        String checkmark = enabled && unlocked ? "§a[✓]" : (unlocked ? "§7[ ]" : "§c[✗]");
        String labelTxt  = unlocked ? (enabled ? "§f" + label : "§7" + label) : "§8" + label + " §c(verrouillé)";
        ctx.drawTextWithShadow(textRenderer, Text.literal(checkmark + " " + labelTxt),
                x + 5, y + (h - 8) / 2, 0xFFFFFF);
    }

    private static boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ── Input ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int imx = (int) mx, imy = (int) my;

        // Boutons de mode
        for (int i = 0; i < MODE_COUNT; i++) {
            if (isOver(imx, imy, modeBtnX[i], modeBtnY[i], MODE_BTN_W, MODE_BTN_H)) {
                if (ExcaveonUserConfig.isModeUnlocked(MODES[i], level)) selectedMode = MODES[i];
                return true;
            }
        }

        // Toggle auto-vente
        if (isOver(imx, imy, autoSellX, autoSellY, TOGGLE_W, TOGGLE_H)) {
            if (ExcaveonLevel.fromLevel(level).autoSell) autoSellOn = !autoSellOn;
            return true;
        }

        // Toggle auto-cuisson
        if (isOver(imx, imy, autoSmeltX, autoSmeltY, TOGGLE_W, TOGGLE_H)) {
            autoSmeltOn = !autoSmeltOn;
            return true;
        }

        // Confirmer
        if (isOver(imx, imy, confirmX, confirmY, CONFIRM_W, CONFIRM_H)) {
            sendConfig();
            this.close();
            return true;
        }

        // Fermer
        if (isOver(imx, imy, closeX, closeY, CLOSE_W, CLOSE_H)) {
            this.close();
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 69) { this.close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void sendConfig() {
        ClientPlayNetworking.send(new ExcaveonUserConfigPayload(autoSellOn, autoSmeltOn, selectedMode));
    }
}
