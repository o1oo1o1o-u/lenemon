package com.lenemon.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

public final class DiscordLinkConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("lenemon_discord_link.json");

    private static DiscordLinkConfig instance;

    private String inviteUrl = "https://discord.gg/MHYt54GBp2";
    private String promptText = "§bPour nous rejoindre sur Discord ";
    private String clickText = "§f§n[clique ici]";
    private String logoGlyph = "\uE000";
    private int blankLinesAboveLogo = 2;

    private DiscordLinkConfig() {}

    public static DiscordLinkConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            instance = new DiscordLinkConfig();
            instance.save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            instance = GSON.fromJson(reader, DiscordLinkConfig.class);
            if (instance == null) instance = new DiscordLinkConfig();
            instance.sanitize();
            instance.save();
        } catch (Exception e) {
            System.err.println("[Lenemon] Erreur lecture config Discord link : " + e.getMessage());
            instance = new DiscordLinkConfig();
        }
    }

    public String getInviteUrl() {
        return inviteUrl;
    }

    public String getPromptText() {
        return promptText;
    }

    public String getClickText() {
        return clickText;
    }

    public String getLogoGlyph() {
        return logoGlyph;
    }

    public int getBlankLinesAboveLogo() {
        return blankLinesAboveLogo;
    }

    private void sanitize() {
        if (inviteUrl == null || inviteUrl.isBlank()) inviteUrl = "https://discord.gg/MHYt54GBp2";
        if (promptText == null || promptText.isBlank()) promptText = "§bPour nous rejoindre sur Discord ";
        if (clickText == null || clickText.isBlank()) clickText = "§f§n[clique ici]";
        if (logoGlyph == null || logoGlyph.isBlank()) logoGlyph = "\uE000";
        if (blankLinesAboveLogo < 0) blankLinesAboveLogo = 0;
        if (blankLinesAboveLogo > 6) blankLinesAboveLogo = 6;
    }

    private void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            System.err.println("[Lenemon] Erreur sauvegarde config Discord link : " + e.getMessage());
        }
    }
}
