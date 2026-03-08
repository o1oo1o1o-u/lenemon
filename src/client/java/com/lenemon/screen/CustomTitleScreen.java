package com.lenemon.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import com.lenemon.widget.StyledButton;

import java.net.URI;

/**
 * The type Custom title screen.
 */
public class CustomTitleScreen extends Screen {

    // ══ CONFIG ══════════════════════════════════════════
    private static final String SERVER_IP   = "pkm.boxtoplay.com";
    private static final int    SERVER_PORT = 25565;
    private static final String DISCORD_URL = "https://discord.gg/vbTjDrnd";
    private static final Identifier BACKGROUND = Identifier.of("lenemon", "textures/gui/background.png");
    // ════════════════════════════════════════════════════

    private static final int BTN_W = 240;
    private static final int BTN_H = 24;
    private static final int GAP   = 6;

    /**
     * Instantiates a new Custom title screen.
     */
    public CustomTitleScreen() {
        super(Text.literal("LeNeMon"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y  = this.height / 2 - 20;


        // ① Rejoindre le serveur
        this.addDrawableChild(new StyledButton(
                cx - BTN_W / 2, y, BTN_W, BTN_H,
                Text.literal("▶  Rejoindre le serveur"),
                0xFF2D7D46, 0xFF4DC96A,
                btn -> connectToServer()
        ));

        // ② Mods | Options
        y += BTN_H + GAP;
        int half = (BTN_W - GAP) / 2;
        this.addDrawableChild(new StyledButton(
                cx - BTN_W / 2, y, half, BTN_H,
                Text.literal("⚙  Mods"),
                0xFF3A3A3A, 0xFF4E4E4E,
                btn -> openMods()
        ));
        this.addDrawableChild(new StyledButton(
                cx - BTN_W / 2 + half + GAP, y, half, BTN_H,
                Text.literal("⚙  Options"),
                0xFF3A3A3A, 0xFF4E4E4E,
                btn -> this.client.setScreen(new OptionsScreen(this, this.client.options))
        ));

        // ③ Discord
        y += BTN_H + GAP;
        this.addDrawableChild(new StyledButton(
                cx - BTN_W / 2, y, BTN_W, BTN_H,
                Text.literal("💬  Rejoindre notre Discord"),
                0xFF5865F2, 0xFF6D78F5,
                btn -> Util.getOperatingSystem().open(URI.create(DISCORD_URL))
        ));

        // ④ Quitter
        y += BTN_H + GAP + 4;
        this.addDrawableChild(new StyledButton(
                cx - 60, y, 120, 20,
                Text.literal("Quitter le jeu"),
                0xFF5C1A1A, 0xFF8B2020,
                btn -> this.client.scheduleStop()
        ));
        int sSize = 20; // Taille du bouton (carré)
        int padding = 10; // Espacement avec le bord de l'écran

        this.addDrawableChild(new StyledButton(
                this.width - sSize - padding,
                this.height - sSize - padding,
                sSize, sSize,
                Text.literal("🌐"), // Un simple globe ou laisse vide ""
                0xFF3A3A3A, 0xFF5E5E5E,
                btn -> this.client.setScreen(new MultiplayerScreen(this))
        ));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Vide — bloque le blur ET le panorama vanilla
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Image de fond plein écran
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        context.drawTexture(
                BACKGROUND,
                0, 0,           // x, y destination
                0, 0,           // u, v source
                this.width, this.height,  // width, height destination
                this.width, this.height   // textureWidth, textureHeight
        );

        // Dégradé pour lisibilité des boutons
        context.fillGradient(0, 0, this.width, this.height / 3, 0xBB000000, 0x00000000);
        context.fillGradient(0, this.height * 2 / 3, this.width, this.height, 0x00000000, 0xBB000000);

        // Logo
        int cx = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§2§lLeNeMon"), cx + 2, this.height / 2 - 90 + 2, 0xFF000000);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§a§lLeNeMon"), cx,     this.height / 2 - 90,     0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§8Minecraft Server"),  cx, this.height / 2 - 74, 0xFFAAAAAA);

        // Tagline
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Bienvenue sur le serveur LeNeMon !"),
                cx, this.height / 2 - 50, 0xFFCCCCCC);

        // Version bas gauche
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("§8LeNeMon | Fabric 1.21.1"),
                4, this.height - 12, 0x888888);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    private void connectToServer() {
        ServerAddress address = ServerAddress.parse(SERVER_IP + ":" + SERVER_PORT);
        ServerInfo info = new ServerInfo("LeNeMon", SERVER_IP + ":" + SERVER_PORT, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, address, info, false, null);
    }

    private void openMods() {
        try {
            Class<?> modMenu = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            this.client.setScreen((Screen) modMenu.getConstructor(Screen.class).newInstance(this));
        } catch (Exception e) {
            // ModMenu pas installé, on ignore
        }
    }
}