package com.lenemon.client.playtime.screen;

import com.lenemon.network.playtime.PlaytimeClaimPayload;
import com.lenemon.network.playtime.PlaytimeOpenPayload;
import com.lenemon.network.playtime.PlaytimeTierDto;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PlaytimeScreen extends Screen {

    private static final int GUI_W = 432;
    private static final int GUI_H = 278;
    private static final int COLS = 5;
    private static final int CARD_W = 76;
    private static final int CARD_H = 84;
    private static final int GAP_X = 8;
    private static final int GAP_Y = 10;

    private static final int COL_BG = 0xCC0A0A1A;
    private static final int COL_BORDER = 0xFF2255AA;
    private static final int COL_SEPARATOR = 0x55FFFFFF;
    private static final int COL_LABEL = 0xAAAAAA;
    private static final int COL_CARD_BG = 0x55113355;
    private static final int COL_CARD_BDR = 0xFF334466;
    private static final int COL_CARD_HOVER = 0xFF4477CC;
    private static final int COL_LOCKED = 0x66444444;
    private static final int COL_LOCKED_BDR = 0xFF555555;
    private static final int COL_CLAIMABLE = 0x55335511;
    private static final int COL_CLAIMABLE_BDR = 0xFF66CC55;
    private static final int COL_CLAIMED = 0x55225544;
    private static final int COL_CLAIMED_BDR = 0xFF44CCAA;
    private static final int COL_BAR_BG = 0xFF1A1A2E;
    private static final int COL_BAR_FILL = 0xFF44AAFF;
    private static final int COL_BAR_DONE = 0xFF44CC66;
    private static final int COL_CLOSE_BG = 0x55330000;
    private static final int COL_CLOSE_BDR = 0xFF443333;
    private static final int COL_CLOSE_HOV = 0xFFAA2222;

    private final PlaytimeOpenPayload data;
    private int guiX;
    private int guiY;
    private final List<Card> cards = new ArrayList<>();
    private Btn closeBtn;

    public PlaytimeScreen(PlaytimeOpenPayload data) {
        super(Text.literal("Playtime"));
        this.data = data;
    }

    @Override
    protected void init() {
        guiX = (width - GUI_W) / 2;
        guiY = (height - GUI_H) / 2;
        closeBtn = new Btn(guiX + GUI_W - 78, guiY + GUI_H - 22, 68, 14, "Fermer");

        cards.clear();
        int gridW = COLS * CARD_W + (COLS - 1) * GAP_X;
        int startX = guiX + (GUI_W - gridW) / 2;
        int startY = guiY + 54;
        for (int i = 0; i < data.tiers().size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            PlaytimeTierDto tier = data.tiers().get(i);
            cards.add(new Card(tier, startX + col * (CARD_W + GAP_X), startY + row * (CARD_H + GAP_Y), CARD_W, CARD_H));
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(guiX, guiY, guiX + GUI_W, guiY + GUI_H, COL_BG);
        drawBorder(ctx, guiX, guiY, GUI_W, GUI_H, COL_BORDER);

        ctx.drawText(textRenderer, Text.literal("§ePlaytime Rewards"), guiX + 10, guiY + 8, 0xFFFFFF, true);
        ctx.drawText(textRenderer, Text.literal("§7Temps de jeu total : §f" + formatDuration(data.playtimeTicks() / 20L)),
                guiX + 10, guiY + 20, COL_LABEL, false);
        ctx.drawText(textRenderer, Text.literal("§7Récompenses récupérées : §f" + data.claimedCount() + "§7/§f" + data.tiers().size()),
                guiX + GUI_W - 180, guiY + 20, COL_LABEL, false);
        ctx.fill(guiX + 8, guiY + 34, guiX + GUI_W - 8, guiY + 35, COL_SEPARATOR);

        for (Card card : cards) renderCard(ctx, card, mouseX, mouseY);
        renderButton(ctx, closeBtn, mouseX, mouseY, COL_CLOSE_BG, COL_CLOSE_BDR, COL_CLOSE_HOV);
        renderTooltip(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderCard(DrawContext ctx, Card card, int mouseX, int mouseY) {
        long totalSeconds = data.playtimeTicks() / 20L;
        long requiredSeconds = card.tier.hoursRequired() * 3600L;
        boolean claimable = !card.tier.claimed() && totalSeconds >= requiredSeconds;
        boolean hovered = isOver(mouseX, mouseY, card.x, card.y, card.w, card.h);
        float progress = requiredSeconds <= 0L ? 1.0F : Math.min(1.0F, (float) totalSeconds / requiredSeconds);

        int bg = COL_CARD_BG;
        int border = hovered ? COL_CARD_HOVER : COL_CARD_BDR;
        if (card.tier.claimed()) {
            bg = COL_CLAIMED;
            border = COL_CLAIMED_BDR;
        } else if (claimable) {
            bg = COL_CLAIMABLE;
            border = COL_CLAIMABLE_BDR;
        } else {
            bg = COL_LOCKED;
            border = hovered ? COL_CARD_HOVER : COL_LOCKED_BDR;
        }

        ctx.fill(card.x, card.y, card.x + card.w, card.y + card.h, bg);
        drawBorder(ctx, card.x, card.y, card.w, card.h, border);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§l" + card.tier.label()), card.x + card.w / 2, card.y + 6, 0xFFFFFF);

        String status = card.tier.claimed() ? "§aRéclamé" : (claimable ? "§eDisponible" : "§8Verrouillé");
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), card.x + card.w / 2, card.y + 18, 0xFFFFFF);

        int barX = card.x + 6;
        int barY = card.y + 32;
        int barW = card.w - 12;
        ctx.fill(barX, barY, barX + barW, barY + 6, COL_BAR_BG);
        ctx.fill(barX, barY, barX + Math.max(0, Math.min(barW, Math.round(barW * progress))), barY + 6,
                card.tier.claimed() || claimable ? COL_BAR_DONE : COL_BAR_FILL);
        drawBorder(ctx, barX, barY, barW, 6, 0xFF223344);

        int pct = Math.min(100, Math.round(progress * 100.0F));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b" + pct + "%"), card.x + card.w / 2, card.y + 42, 0xFFFFFF);

        List<String> lines = card.tier.rewardLines();
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            String fitted = trimToWidth(lines.get(i), card.w - 10);
            ctx.drawText(textRenderer, Text.literal(fitted), card.x + 5, card.y + 55 + i * 9, COL_LABEL, false);
        }
        if (lines.size() > 2) {
            String more = "§7+" + (lines.size() - 2) + " autres";
            ctx.drawText(textRenderer, Text.literal(trimToWidth(more, card.w - 10)), card.x + 5, card.y + 73, COL_LABEL, false);
        }
    }

    private void renderTooltip(DrawContext ctx, int mouseX, int mouseY) {
        for (Card card : cards) {
            if (!isOver(mouseX, mouseY, card.x, card.y, card.w, card.h)) continue;

            long totalSeconds = data.playtimeTicks() / 20L;
            long requiredSeconds = card.tier.hoursRequired() * 3600L;
            long missingSeconds = Math.max(0L, requiredSeconds - totalSeconds);
            float progress = requiredSeconds <= 0L ? 1.0F : Math.min(1.0F, (float) totalSeconds / requiredSeconds);

            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§e§lPalier " + card.tier.label()));
            lines.add(Text.literal("§7Progression : §f" + formatDuration(totalSeconds) + " §7/ §f" + formatDuration(requiredSeconds)));
            lines.add(Text.literal("§7Pourcentage : §b" + Math.min(100, Math.round(progress * 100.0F)) + "%"));
            lines.add(Text.literal(""));
            lines.add(Text.literal("§6Récompenses"));
            for (String rewardLine : card.tier.rewardLines()) {
                lines.add(Text.literal(rewardLine));
            }
            lines.add(Text.literal(""));
            if (card.tier.claimed()) {
                lines.add(Text.literal("§aCette récompense a déjà été récupérée."));
            } else if (totalSeconds >= requiredSeconds) {
                lines.add(Text.literal("§eClique pour réclamer cette récompense."));
            } else {
                lines.add(Text.literal("§cTemps restant : §f" + formatDuration(missingSeconds)));
            }
            ctx.drawTooltip(textRenderer, lines, mouseX, mouseY);
            break;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (isOver(closeBtn, mx, my)) {
            close();
            return true;
        }

        long totalSeconds = data.playtimeTicks() / 20L;
        for (Card card : cards) {
            if (!isOver(mx, my, card.x, card.y, card.w, card.h)) continue;
            long requiredSeconds = card.tier.hoursRequired() * 3600L;
            boolean claimable = !card.tier.claimed() && totalSeconds >= requiredSeconds;
            if (claimable) {
                ClientPlayNetworking.send(new PlaytimeClaimPayload(card.tier.id()));
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    private static String formatDuration(long totalSeconds) {
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        if (days > 0) return days + "j " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return Math.max(0L, minutes) + "m";
    }

    private String trimToWidth(String line, int maxWidth) {
        if (textRenderer.getWidth(line) <= maxWidth) return line;
        String ellipsis = "...";
        int end = line.length();
        while (end > 0) {
            String candidate = line.substring(0, end) + ellipsis;
            if (textRenderer.getWidth(candidate) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return ellipsis;
    }

    private static boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean isOver(Btn btn, int mx, int my) {
        return isOver(mx, my, btn.x, btn.y, btn.w, btn.h);
    }

    private void renderButton(DrawContext ctx, Btn btn, int mouseX, int mouseY, int bg, int border, int hoverBorder) {
        boolean hovered = isOver(btn, mouseX, mouseY);
        ctx.fill(btn.x, btn.y, btn.x + btn.w, btn.y + btn.h, bg);
        drawBorder(ctx, btn.x, btn.y, btn.w, btn.h, hovered ? hoverBorder : border);
        int tw = textRenderer.getWidth(btn.label);
        ctx.drawText(textRenderer, Text.literal(btn.label), btn.x + (btn.w - tw) / 2, btn.y + (btn.h - 8) / 2, hovered ? 0xFFFFFF : COL_LABEL, false);
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private record Card(PlaytimeTierDto tier, int x, int y, int w, int h) {}
    private record Btn(int x, int y, int w, int h, String label) {}
}
