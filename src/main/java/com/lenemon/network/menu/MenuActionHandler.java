package com.lenemon.network.menu;

import com.lenemon.hunter.HunterUtils;
import com.lenemon.hunter.data.HunterPlayerData;
import com.lenemon.hunter.data.HunterWorldData;
import com.lenemon.hunter.quest.Quest;
import com.lenemon.hunter.quest.QuestDifficulty;
import com.lenemon.hunter.quest.QuestManager;
import com.lenemon.pokedex.PokedexService;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuActionHandler {

    public static void handle(MenuActionPayload payload, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            ServerPlayerEntity player = ctx.player();
            switch (payload.action()) {
                case "open_menu"     -> sendMenuOpen(player);
                case "open_tp"       -> sendTpMenuOpen(player);
                case "open_hunter"   -> sendHunterMenuOpen(player);
                case "open_pokedex"  -> PokedexService.sendPokedexOpen(player);
                case "tp_spawn"      -> executeTP(player, "Spawn", "spawn");
                case "tp_overworld"  -> executeTP(player, "Monde Principal", "execute in minecraft:overworld run rtp");
                case "tp_resource"   -> executeTP(player, "Monde Ressource", "execute in multiworld:ressource1 run rtp");
                case "tp_nether"     -> {
                    if (Permissions.check(player, "custommenu.tp.nether", 2)) {
                        executeTP(player, "Nether", "execute in minecraft:the_nether run rtp");
                    } else {
                        player.sendMessage(Text.literal("§cVous n'avez pas la permission d'aller dans le Nether."), false);
                    }
                }
                case "tp_end"        -> {
                    if (Permissions.check(player, "custommenu.tp.end", 2)) {
                        executeTP(player, "The End", "execute in minecraft:the_end run rtp");
                    } else {
                        player.sendMessage(Text.literal("§cVous n'avez pas la permission d'aller dans l'End."), false);
                    }
                }
                default -> player.sendMessage(Text.literal("§cAction inconnue : " + payload.action()), false);
            }
        });
    }

    // Appelable depuis MenuCommand
    public static void sendMenuOpen(ServerPlayerEntity player) {
        int level          = HunterUtils.getLevel(player.getUuid());
        float progress     = HunterUtils.getLevelProgress(player.getUuid());
        String progressBar = HunterUtils.getProgressBar(player.getUuid());
        String progressPct = HunterUtils.getLevelProgressPercent(player.getUuid());

        MenuOpenPayload payload = new MenuOpenPayload(level, progress, progressBar, progressPct);
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendTpMenuOpen(ServerPlayerEntity player) {
        boolean hasNether = Permissions.check(player, "custommenu.tp.nether", 2);
        boolean hasEnd    = Permissions.check(player, "custommenu.tp.end", 2);

        TpMenuOpenPayload payload = new TpMenuOpenPayload(hasNether, hasEnd);
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendHunterMenuOpen(ServerPlayerEntity player) {
        HunterPlayerData data = HunterWorldData.get(player.getUuid());

        int level          = data.level;
        long xp            = data.xp;
        long xpForNext     = HunterPlayerData.xpForNextLevel(data.level);
        String progressBar = HunterUtils.getProgressBar(player.getUuid());
        String progressPct = HunterUtils.getLevelProgressPercent(player.getUuid());
        int nextReward     = getNextRewardLevel(data.level);
        String resetTimer  = QuestManager.timeUntilResetFormatted();

        Map<String, Quest> quests = data.activeQuests;
        List<QuestDto> easyQuests   = new ArrayList<>();
        List<QuestDto> mediumQuests = new ArrayList<>();
        List<QuestDto> hardQuests   = new ArrayList<>();

        for (Quest quest : quests.values()) {
            QuestDto dto = buildQuestDto(quest, data);
            switch (quest.difficulty) {
                case EASY   -> easyQuests.add(dto);
                case MEDIUM -> mediumQuests.add(dto);
                case HARD   -> hardQuests.add(dto);
            }
        }

        HunterMenuOpenPayload payload = new HunterMenuOpenPayload(
                level, xp, xpForNext, progressBar, progressPct,
                nextReward, resetTimer,
                easyQuests, mediumQuests, hardQuests
        );
        ServerPlayNetworking.send(player, payload);
    }

    private static QuestDto buildQuestDto(Quest quest, HunterPlayerData data) {
        int progress  = data.getProgress(quest.id);
        boolean done  = data.isQuestComplete(quest.id);

        String difficultyLabel = switch (quest.difficulty) {
            case EASY   -> "§a[Facile]";
            case MEDIUM -> "§e[Moyen]";
            case HARD   -> "§5[Difficile]";
        };

        String colorCode = switch (quest.difficulty) {
            case EASY   -> "§a";
            case MEDIUM -> "§e";
            case HARD   -> "§5";
        };

        String ballItemId = switch (quest.difficulty) {
            case EASY   -> "cobblemon:poke_ball";
            case MEDIUM -> "cobblemon:ultra_ball";
            case HARD   -> "cobblemon:master_ball";
        };

        // Reproduit exactement la logique de HunterMenuHandler#buildQuestItem pour itemsLabel
        String itemsLabel = "";
        if (!quest.itemRewards.isEmpty() || !quest.commandsLabel.isEmpty()) {
            List<String> rewardNames = new ArrayList<>();
            for (String rewardItemId : quest.itemRewards) {
                var item = Registries.ITEM.get(Identifier.of(rewardItemId));
                rewardNames.add(item.getName().getString());
            }
            if (!quest.commandsLabel.isEmpty()) {
                rewardNames.add(quest.commandsLabel);
            }
            itemsLabel = String.join(", ", rewardNames);
        }

        return new QuestDto(
                quest.id,
                quest.getDescription(),
                difficultyLabel,
                colorCode,
                progress,
                quest.amount,
                done,
                quest.xpReward,
                quest.moneyReward,
                itemsLabel,
                ballItemId
        );
    }

    private static void executeTP(ServerPlayerEntity player, String worldName, String command) {
        player.closeHandledScreen();
        player.sendMessage(
                Text.literal("✦ ")
                        .formatted(Formatting.GOLD)
                        .append(Text.literal("Téléportation vers ")
                                .formatted(Formatting.GRAY))
                        .append(Text.literal(worldName)
                                .formatted(Formatting.AQUA, Formatting.BOLD)),
                false
        );
        player.getServer().getCommandManager().executeWithPrefix(
                player.getCommandSource().withMaxLevel(4),
                command
        );
    }

    private static int getNextRewardLevel(int currentLevel) {
        int[] rewardLevels = {10, 25, 50, 100, 150, 200};
        for (int l : rewardLevels) {
            if (l > currentLevel) return l;
        }
        return 200;
    }
}
