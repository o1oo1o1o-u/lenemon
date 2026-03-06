package com.lenemon.hunter.quest;

import com.lenemon.hunter.data.HunterPlayerData;
import com.lenemon.hunter.data.HunterWorldData;
import com.lenemon.hunter.reward.LevelRewardConfig;
import com.lenemon.util.EconomyHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.LocalDateTime;
import java.util.*;

/**
 * The type Quest manager.
 */
public class QuestManager {

    private static int lastHour = -1;
    private static boolean notified30 = false;
    private static boolean notified1  = false;

    /**
     * Register.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(QuestManager::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 200 != 0) return;

        int currentHour = LocalDateTime.now().getHour();
        int currentMinute = LocalDateTime.now().getMinute();

        if (currentHour != lastHour) {
            lastHour = currentHour;
            refreshAllQuests(server);
            return;
        }

        // Notification 30 minutes
        if (currentMinute == 30 && !notified30) {
            notified30 = true;
            notified1  = false;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(Text.literal("§6[Chasseur] §eVos quêtes changent dans §f30 minutes§e !"), false);
            }
        }

        // Notification 1 minute — seulement si le joueur a des quêtes en cours
        if (currentMinute == 59 && !notified1) {
            notified1 = true;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                HunterPlayerData data = HunterWorldData.get(player.getUuid());
                boolean hasProgress = data.activeQuestProgress.values().stream().anyMatch(v -> v > 0);
                if (hasProgress) {
                    player.sendMessage(Text.literal("§c[Chasseur] ⚠ Vos quêtes changent dans §f1 minute §c! Dépêchez-vous !"), false);
                }
            }
        }

        // Reset des flags à chaque nouvelle heure
        if (currentMinute < 30) {
            notified30 = false;
            notified1  = false;
        }
    }

    private static void refreshAllQuests(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            assignQuests(player);
        }
        HunterWorldData.save();
    }

    /**
     * Assign quests.
     *
     * @param player the player
     */
    public static void assignQuests(ServerPlayerEntity player) {
        HunterPlayerData data = HunterWorldData.get(player.getUuid());
        data.activeQuests.clear();
        data.activeQuestProgress.clear();

        List<Quest> easy   = pickRandom(QuestConfigLoader.getByDifficulty(QuestDifficulty.EASY), 3);
        List<Quest> medium = pickRandom(QuestConfigLoader.getByDifficulty(QuestDifficulty.MEDIUM), 2);
        List<Quest> hard   = pickRandom(QuestConfigLoader.getByDifficulty(QuestDifficulty.HARD), 1);

        List<Quest> all = new ArrayList<>();
        all.addAll(easy);
        all.addAll(medium);
        all.addAll(hard);

        for (Quest q : all) {
            data.activeQuests.put(q.id, q);
            data.activeQuestProgress.put(q.id, 0);
        }

        player.sendMessage(Text.literal("§6[Chasseur] §eNouvelles quêtes disponibles !"), false);
    }

    /**
     * On progress.
     *
     * @param player      the player
     * @param type        the type
     * @param species     the species
     * @param pokemonType the pokemon type
     * @param shiny       the shiny
     * @param legendary   the legendary
     */
    public static void onProgress(ServerPlayerEntity player, QuestType type, String species, String pokemonType, boolean shiny, boolean legendary) {
        HunterPlayerData data = HunterWorldData.get(player.getUuid());

        // XP de base hors quête — lu depuis le JSON
        long baseXp = switch (type) {
            case KILL    -> HunterSettings.xpPerKill;
            case CAPTURE -> HunterSettings.xpPerCapture;
            default      -> 0;
        };

        if (shiny)     baseXp = (long)(baseXp * HunterSettings.shinyMultiplier);
        if (legendary) baseXp = (long)(baseXp * HunterSettings.legendaryMultiplier);

        if (baseXp > 0) {
            boolean leveledUp = data.addXp(baseXp);
            if (leveledUp) onLevelUp(player, data);
        }

        // Progression des quêtes actives
        for (Map.Entry<String, Quest> entry : new HashMap<>(data.activeQuests).entrySet()) {
            String questId = entry.getKey();
            Quest quest    = entry.getValue();

            if (data.isQuestComplete(questId)) continue;

            boolean matches = switch (quest.type) {
                case KILL              -> type == QuestType.KILL;
                case CAPTURE           -> type == QuestType.CAPTURE;
                case CAPTURE_TYPE      -> type == QuestType.CAPTURE && quest.target.equalsIgnoreCase(pokemonType);
                case KILL_TYPE         -> type == QuestType.KILL && quest.target.equalsIgnoreCase(pokemonType);
                case KILL_SPECIES      -> type == QuestType.KILL && quest.target.equalsIgnoreCase(species);
                case CAPTURE_SHINY     -> type == QuestType.CAPTURE && shiny;
                case CAPTURE_LEGENDARY -> type == QuestType.CAPTURE && legendary;
            };

            if (matches) {
                data.incrementProgress(questId);
                if (data.isQuestComplete(questId)) {
                    completeQuest(player, data, quest);
                }
            }
        }

        HunterWorldData.save();
    }

    private static void completeQuest(ServerPlayerEntity player, HunterPlayerData data, Quest quest) {
        player.sendMessage(Text.literal("§a[Chasseur] Quête complétée : §f" + quest.getDescription()), false);

        if (quest.moneyReward > 0) {
            EconomyHelper.credit(player, quest.moneyReward);
            player.sendMessage(Text.literal("§6+" + quest.moneyReward + " $"), true);
        }

        for (String itemId : quest.itemRewards) {
            var item = Registries.ITEM.get(Identifier.of(itemId));
            player.getInventory().insertStack(new ItemStack(item));
        }
        for (String cmd : quest.commands) {
            String finalCmd = cmd
                    .replace("{player}", player.getName().getString())
                    .replaceFirst("^/", "");
            player.getServer().getCommandManager().executeWithPrefix(
                    player.getServer().getCommandSource(),
                    finalCmd
            );
        }

        boolean leveledUp = data.addXp(quest.xpReward);
        if (leveledUp) onLevelUp(player, data);
    }

    /**
     * On level up.
     *
     * @param player the player
     * @param data   the data
     */
    public static void onLevelUp(ServerPlayerEntity player, HunterPlayerData data) {
        player.sendMessage(Text.literal("§b§l[Chasseur] ★ Niveau " + data.level + " atteint !"), false);

        LevelRewardConfig.LevelReward reward = LevelRewardConfig.getReward(data.level);
        if (reward != null) {
            player.sendMessage(Text.literal(reward.message()), false);

            if (reward.money() > 0) EconomyHelper.credit(player, reward.money());

            for (String itemId : reward.items()) {
                var item = Registries.ITEM.get(Identifier.of(itemId));
                player.getInventory().insertStack(new ItemStack(item));
            }

            // Exécution des commandes serveur
            for (String cmd : reward.commands()) {
                String finalCmd = cmd
                        .replace("{player}", player.getName().getString())
                        .replaceFirst("^/", ""); // retire le / si présent
                player.getServer().getCommandManager().executeWithPrefix(
                        player.getServer().getCommandSource(),
                        finalCmd
                );
            }
        }
    }

    private static <T> List<T> pickRandom(List<T> list, int count) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.stream().limit(count).toList();
    }

    /**
     * Minutes until reset int.
     *
     * @return the int
     */
// Retourne les minutes restantes avant le prochain reset
    public static int minutesUntilReset() {
        LocalDateTime now = LocalDateTime.now();
        int minutesLeft = 60 - now.getMinute();
        return minutesLeft;
    }

    /**
     * Time until reset formatted string.
     *
     * @return the string
     */
// Retourne un String formaté ex: "42 minutes" ou "1 minute"
    public static String timeUntilResetFormatted() {
        int minutes = minutesUntilReset();
        return minutes + " minute" + (minutes > 1 ? "s" : "");
    }
}