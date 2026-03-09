package com.lenemon.playtime;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlaytimeConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File configFile;
    private static RootConfig config = RootConfig.createDefault();

    private PlaytimeConfig() {}

    public static void load(MinecraftServer server) {
        configFile = server.getRunDirectory().resolve("config/lenemon/playtime_rewards.json").toFile();
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
            save();
        } catch (Exception e) {
            System.err.println("[Playtime] Erreur chargement config : " + e.getMessage());
            config = RootConfig.createDefault();
        }
    }

    public static void save() {
        if (configFile == null) return;
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(config.sanitize(), writer);
        } catch (Exception e) {
            System.err.println("[Playtime] Erreur sauvegarde config : " + e.getMessage());
        }
    }

    public static RootConfig get() {
        return config;
    }

    public static final class RootConfig {
        public List<TierConfig> tiers = createDefaultTiers();

        public static RootConfig createDefault() {
            return new RootConfig();
        }

        public RootConfig sanitize() {
            List<TierConfig> defaults = createDefaultTiers();
            if (tiers == null) tiers = new ArrayList<>();

            List<TierConfig> sanitized = new ArrayList<>();
            Set<String> seenIds = new HashSet<>();
            for (int i = 0; i < defaults.size(); i++) {
                TierConfig fallback = defaults.get(i);
                TierConfig tier = i < tiers.size() && tiers.get(i) != null ? tiers.get(i) : fallback.copy();
                tier = tier.sanitize(fallback);
                if (!seenIds.add(tier.id)) {
                    tier.id = fallback.id;
                    tier.label = fallback.label;
                }
                sanitized.add(tier);
            }
            tiers = sanitized;
            return this;
        }

        private static List<TierConfig> createDefaultTiers() {
            List<TierConfig> defaults = new ArrayList<>();
            defaults.add(tier("tier_1", "1h", 1,
                    List.of(
                            item("cobblemon:great_ball", 64, "Super Ball"),
                            item("cobblemon:ultra_ball", 32, "Hyper Ball"),
                            item("cobblemon:rare_candy", 16, "Super Bonbon")
                    ),
                    List.of(command("/lenemon muffin %player% normal 1", "1 Muffin Magique"))));
            defaults.add(tier("tier_2", "5h", 5,
                    List.of(
                            item("cobblemon:ultra_ball", 128, "Hyper Ball"),
                            item("cobblemon:rare_candy", 32, "Super Bonbon")
                    ),
                    List.of(
                            command("/lenemon muffin %player% normal 2", "2 Muffins Magiques"),
                            command("/gift give Rare %player% 1", "1 Gift Rare")
                    )));
            defaults.add(tier("tier_3", "10h", 10,
                    List.of(
                            item("cobblemon:ultra_ball", 128, "Hyper Ball"),
                            item("cobblemon:rare_candy", 48, "Super Bonbon")
                    ),
                    List.of(
                            command("/lenemon muffin %player% normal 2", "2 Muffins Magiques"),
                            command("/casino give %player%", "1 Ticket Casino"),
                            command("/gift give Rare %player% 2", "2 Gifts Rares")
                    )));
            defaults.add(tier("tier_4", "25h", 25,
                    List.of(
                            item("cobblemon:ultra_ball", 128, "Hyper Ball"),
                            item("cobblemon:rare_candy", 64, "Super Bonbon")
                    ),
                    List.of(
                            command("/lenemon muffin %player% normal 2", "2 Muffins Magiques"),
                            command("/gift give Rare %player% 2", "2 Gifts Rares"),
                            command("/gift give Epique %player% 1", "1 Gift Epique")
                    )));
            defaults.add(tier("tier_5", "50h", 50,
                    List.of(
                            item("cobblemon:ultra_ball", 256, "Hyper Ball"),
                            item("cobblemon:rare_candy", 64, "Super Bonbon")
                    ),
                    List.of(
                            command("/lenemon muffin %player% normal 2", "2 Muffins Magiques"),
                            command("/lenemon muffin %player% shiny 1", "1 Muffin Magique Shiny"),
                            command("/gift give Rare %player% 2", "2 Gifts Rares"),
                            command("/gift give Epique %player% 1", "1 Gift Epique")
                    )));
            defaults.add(tier("tier_6", "100h", 100,
                    List.of(
                            item("cobblemon:ultra_ball", 256, "Hyper Ball"),
                            item("cobblemon:rare_candy", 64, "Super Bonbon")
                    ),
                    List.of(
                            command("/lenemon muffin %player% normal 2", "2 Muffins Magiques"),
                            command("/lenemon muffin %player% shiny 1", "1 Muffin Magique Shiny"),
                            command("/lenemon muffin %player% legendary 1", "1 Muffin Magique Legendaire"),
                            command("/gift give Rare %player% 2", "2 Gifts Rares"),
                            command("/gift give Epique %player% 1", "1 Gift Epique")
                    )));
            defaults.add(tier("tier_7", "150h", 150,
                    List.of(
                            item("cobblemon:master_ball", 1, "Master Ball"),
                            item("cobblemon:rare_candy", 128, "Super Bonbon")
                    ),
                    List.of(
                            command("/lenemon muffin %player% normal 2", "2 Muffins Magiques"),
                            command("/lenemon muffin %player% shiny 1", "1 Muffin Magique Shiny"),
                            command("/lenemon muffin %player% legendary 1", "1 Muffin Magique Legendaire"),
                            command("/gift give Legendaire %player% 1", "1 Gift Legendaire"),
                            command("/gift give Epique %player% 1", "1 Gift Epique")
                    )));
            defaults.add(tier("tier_8", "250h", 250,
                    List.of(
                            item("cobblemon:master_ball", 1, "Master Ball"),
                            item("cobblemon:rare_candy", 128, "Super Bonbon")
                    ),
                    List.of(
                            command("/lenemon muffin %player% shiny 2", "2 Muffins Magiques Shiny"),
                            command("/lenemon muffin %player% legendary 1", "1 Muffin Magique Legendaire"),
                            command("/gift give Legendaire %player% 2", "2 Gifts Legendaires"),
                            command("/gift give Epique %player% 1", "1 Gift Epique")
                    )));
            defaults.add(tier("tier_9", "350h", 350,
                    List.of(
                            item("cobblemon:master_ball", 1, "Master Ball"),
                            item("cobblemon:rare_candy", 128, "Super Bonbon")
                    ),
                    List.of(
                            command("/lenemon muffin %player% shiny 2", "2 Muffins Magiques Shiny"),
                            command("/lenemon muffin %player% legendary 2", "2 Muffins Magiques Legendaires"),
                            command("/gift give Legendaire %player% 2", "2 Gifts Legendaires"),
                            command("/gift give Epique %player% 2", "2 Gifts Epiques")
                    )));
            defaults.add(tier("tier_10", "500h", 500,
                    List.of(
                            item("cobblemon:ancient_ball", 1, "Ancient Ball"),
                            item("cobblemon:rare_candy", 128, "Super Bonbon")
                    ),
                    List.of(
                            command("/lenemon muffin %player% shiny 1", "1 Muffin Magique Shiny"),
                            command("/lenemon muffin %player% legendary 2", "2 Muffins Magiques Legendaires"),
                            command("/gift give Legendaire %player% 2", "2 Gifts Legendaires"),
                            command("/gift give Epique %player% 4", "4 Gifts Epiques")
                    )));
            return defaults;
        }
    }

    public static final class TierConfig {
        public String id;
        public String label;
        public int hoursRequired;
        public RewardConfig rewards = new RewardConfig();

        public TierConfig sanitize(TierConfig fallback) {
            if (id == null || id.isBlank()) id = fallback.id;
            if (label == null || label.isBlank()) label = fallback.label;
            if (hoursRequired < 1) hoursRequired = fallback.hoursRequired;
            if (rewards == null) rewards = fallback.rewards.copy();
            rewards = rewards.sanitize();
            return this;
        }

        public TierConfig copy() {
            TierConfig copy = new TierConfig();
            copy.id = id;
            copy.label = label;
            copy.hoursRequired = hoursRequired;
            copy.rewards = rewards.copy();
            return copy;
        }
    }

    public static final class RewardConfig {
        public List<ItemRewardConfig> items = new ArrayList<>();
        public List<CommandRewardConfig> commands = new ArrayList<>();

        public RewardConfig sanitize() {
            if (items == null) items = new ArrayList<>();
            if (commands == null) commands = new ArrayList<>();
            items.removeIf(item -> item == null || item.itemId == null || item.itemId.isBlank() || item.count < 1);
            for (ItemRewardConfig item : items) item.sanitize();
            commands.removeIf(cmd -> cmd == null || cmd.command == null || cmd.command.isBlank());
            for (CommandRewardConfig command : commands) command.sanitize();
            return this;
        }

        public RewardConfig copy() {
            RewardConfig copy = new RewardConfig();
            for (ItemRewardConfig item : items) copy.items.add(item.copy());
            for (CommandRewardConfig command : commands) copy.commands.add(command.copy());
            return copy;
        }
    }

    public static final class ItemRewardConfig {
        public String itemId;
        public int count;
        public String displayName;
        public String customName;

        public void sanitize() {
            if (count < 1) count = 1;
            if (displayName != null && displayName.isBlank()) displayName = null;
            if (customName != null && customName.isBlank()) customName = null;
        }

        public ItemRewardConfig copy() {
            ItemRewardConfig copy = new ItemRewardConfig();
            copy.itemId = itemId;
            copy.count = count;
            copy.displayName = displayName;
            copy.customName = customName;
            return copy;
        }
    }

    public static final class CommandRewardConfig {
        public String command;
        public String displayName;

        public void sanitize() {
            if (command != null && command.isBlank()) command = null;
            if (displayName != null && displayName.isBlank()) displayName = null;
        }

        public CommandRewardConfig copy() {
            CommandRewardConfig copy = new CommandRewardConfig();
            copy.command = command;
            copy.displayName = displayName;
            return copy;
        }
    }

    private static TierConfig tier(String id, String label, int hoursRequired,
                                   List<ItemRewardConfig> items, List<CommandRewardConfig> commands) {
        TierConfig tier = new TierConfig();
        tier.id = id;
        tier.label = label;
        tier.hoursRequired = hoursRequired;
        tier.rewards = new RewardConfig();
        tier.rewards.items = new ArrayList<>(items);
        tier.rewards.commands = new ArrayList<>(commands);
        return tier;
    }

    private static ItemRewardConfig item(String itemId, int count, String displayName) {
        ItemRewardConfig item = new ItemRewardConfig();
        item.itemId = itemId;
        item.count = count;
        item.displayName = displayName;
        return item;
    }

    private static CommandRewardConfig command(String command, String displayName) {
        CommandRewardConfig reward = new CommandRewardConfig();
        reward.command = command;
        reward.displayName = displayName;
        return reward;
    }
}
