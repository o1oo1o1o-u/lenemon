package com.lenemon.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * The type Styled button.
 */
public class StyledButton extends ButtonWidget {

    private final int colorNormal;
    private final int colorHover;

    /**
     * Instantiates a new Styled button.
     *
     * @param x           the x
     * @param y           the y
     * @param width       the width
     * @param height      the height
     * @param message     the message
     * @param colorNormal the color normal
     * @param colorHover  the color hover
     * @param onPress     the on press
     */
    public StyledButton(int x, int y, int width, int height,
                        Text message,
                        int colorNormal, int colorHover,
                        PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.colorNormal = colorNormal;
        this.colorHover  = colorHover;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int bg     = this.isHovered() ? colorHover : colorNormal;
        int border = brighten(bg, 40);

        // Fond
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), withAlpha(bg, 220));

        // Bordures
        context.fill(getX(), getY(),                          getX() + getWidth(), getY() + 1,             border); // haut
        context.fill(getX(), getY() + getHeight() - 1,        getX() + getWidth(), getY() + getHeight(),   border); // bas
        context.fill(getX(), getY(),                          getX() + 1,          getY() + getHeight(),   border); // gauche
        context.fill(getX() + getWidth() - 1, getY(),         getX() + getWidth(), getY() + getHeight(),   border); // droite

        // Highlight hover
        if (this.isHovered()) {
            context.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + 3, 0x33FFFFFF);
        }

        // Texte
        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                this.active ? 0xFFFFFFFF : 0xFF888888
        );
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8)  & 0xFF) + amount);
        int b = Math.min(255, ((color)       & 0xFF) + amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}