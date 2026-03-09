package com.lenemon.muffin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MuffinConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File configFile;
    private static RootConfig config = RootConfig.createDefault();

    private MuffinConfig() {}

    public static void load(MinecraftServer server) {
        configFile = server.getRunDirectory().resolve("config/lenemon/muffin_pools.json").toFile();
        File parent = configFile.getParentFile();
        if (parent != null) parent.mkdirs();

        if (!configFile.exists()) {
            config = RootConfig.createDefault();
            save();
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            RootConfig loaded = GSON.fromJson(reader, RootConfig.class);
            config = loaded != null ? loaded.sanitize() : RootConfig.createDefault();
        } catch (Exception e) {
            System.err.println("[Muffin] Erreur chargement config : " + e.getMessage());
            config = RootConfig.createDefault();
        }
    }

    public static RootConfig get() {
        return config;
    }

    public static MuffinTypeConfig get(MagicMuffinType type) {
        return switch (type) {
            case NORMAL -> config.normal;
            case SHINY -> config.shiny;
            case LEGENDARY -> config.legendary;
        };
    }

    public static void save() {
        if (configFile == null) return;
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(config.sanitize(), writer);
        } catch (Exception e) {
            System.err.println("[Muffin] Erreur sauvegarde config : " + e.getMessage());
        }
    }

    public static final class RootConfig {
        public MuffinTypeConfig normal = MuffinTypeConfig.normalDefault();
        public MuffinTypeConfig shiny = MuffinTypeConfig.shinyDefault();
        public MuffinTypeConfig legendary = MuffinTypeConfig.legendaryDefault();

        public static RootConfig createDefault() {
            return new RootConfig();
        }

        public RootConfig sanitize() {
            if (normal == null) normal = MuffinTypeConfig.normalDefault();
            if (shiny == null) shiny = MuffinTypeConfig.shinyDefault();
            if (legendary == null) legendary = MuffinTypeConfig.legendaryDefault();
            normal = normal.sanitize(MuffinTypeConfig.normalDefault());
            shiny = shiny.sanitize(MuffinTypeConfig.shinyDefault());
            legendary = legendary.sanitize(MuffinTypeConfig.legendaryDefault());
            return this;
        }
    }

    public static final class MuffinTypeConfig {
        public int level = 5;
        public double legendaryChance = 0.0D;
        public double shinyChance = 0.0D;
        public PoolSet pools = PoolSet.defaultPools();

        public MuffinTypeConfig sanitize(MuffinTypeConfig fallback) {
            if (level < 1) level = fallback.level;
            legendaryChance = clampChance(legendaryChance, fallback.legendaryChance);
            shinyChance = clampChance(shinyChance, fallback.shinyChance);
            if (pools == null) pools = fallback.pools.copy();
            pools = pools.sanitize();
            return this;
        }

        public static MuffinTypeConfig normalDefault() {
            MuffinTypeConfig cfg = new MuffinTypeConfig();
            cfg.level = 5;
            cfg.legendaryChance = 2.0D;
            cfg.shinyChance = 0.0D;
            cfg.pools = PoolSet.defaultPools();
            return cfg;
        }

        public static MuffinTypeConfig shinyDefault() {
            MuffinTypeConfig cfg = new MuffinTypeConfig();
            cfg.level = 5;
            cfg.legendaryChance = 1.0D;
            cfg.shinyChance = 100.0D;
            cfg.pools = PoolSet.defaultPools();
            return cfg;
        }

        public static MuffinTypeConfig legendaryDefault() {
            MuffinTypeConfig cfg = new MuffinTypeConfig();
            cfg.level = 5;
            cfg.legendaryChance = 100.0D;
            cfg.shinyChance = 1.0D;
            cfg.pools = PoolSet.defaultPools();
            return cfg;
        }

        private static double clampChance(double value, double fallback) {
            if (Double.isNaN(value) || Double.isInfinite(value)) return fallback;
            return Math.max(0.0D, Math.min(100.0D, value));
        }
    }

    public static final class PoolSet {
        public PoolConfig standard = PoolConfig.defaultPool();
        public PoolConfig legendary = PoolConfig.defaultPool();

        public static PoolSet defaultPools() {
            return new PoolSet();
        }

        public PoolSet sanitize() {
            if (standard == null) standard = PoolConfig.defaultPool();
            if (legendary == null) legendary = PoolConfig.defaultPool();
            standard = standard.sanitize();
            legendary = legendary.sanitize();
            return this;
        }

        public PoolSet copy() {
            PoolSet set = new PoolSet();
            set.standard = standard.copy();
            set.legendary = legendary.copy();
            return set;
        }
    }

    public static final class PoolConfig {
        public List<String> regions = new ArrayList<>(List.of("national"));
        public List<String> species = new ArrayList<>();
        public List<String> excludeSpecies = new ArrayList<>();

        public static PoolConfig defaultPool() {
            return new PoolConfig();
        }

        public PoolConfig sanitize() {
            if (regions == null || regions.isEmpty()) regions = new ArrayList<>(List.of("national"));
            if (species == null) species = new ArrayList<>();
            if (excludeSpecies == null) excludeSpecies = new ArrayList<>();
            regions = normalizeList(regions);
            species = normalizeList(species);
            excludeSpecies = normalizeList(excludeSpecies);
            return this;
        }

        public PoolConfig copy() {
            PoolConfig copy = new PoolConfig();
            copy.regions = new ArrayList<>(regions);
            copy.species = new ArrayList<>(species);
            copy.excludeSpecies = new ArrayList<>(excludeSpecies);
            return copy;
        }

        private static List<String> normalizeList(List<String> input) {
            List<String> out = new ArrayList<>();
            for (String value : input) {
                if (value == null) continue;
                String normalized = value.trim().toLowerCase();
                if (!normalized.isEmpty() && !out.contains(normalized)) {
                    out.add(normalized);
                }
            }
            return out;
        }
    }
}
