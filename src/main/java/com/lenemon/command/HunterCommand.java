package com.lenemon.command;

import com.lenemon.hunter.HunterUtils;
import com.lenemon.hunter.data.HunterPlayerData;
import com.lenemon.hunter.data.HunterWorldData;
import com.lenemon.hunter.quest.QuestConfigLoader;
import com.lenemon.hunter.quest.QuestManager;
import com.lenemon.hunter.reward.LevelRewardConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * The type Hunter command.
 */
public class HunterCommand {

    /**
     * Register.
     *
     * @param dispatcher the dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("hunter")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload")
                                .executes(ctx -> {
                                    QuestConfigLoader.load();
                                    LevelRewardConfig.load();
                                    ctx.getSource().sendMessage(Text.literal("§a[Hunter] Config rechargée (quêtes + récompenses de niveau)."));
                                    return 1;
                                }))
                        .then(CommandManager.literal("xp")
                                .then(CommandManager.literal("add")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                                            HunterPlayerData data = HunterWorldData.get(target.getUuid());
                                                            boolean leveledUp = data.addXp(amount);
                                                            if (leveledUp) QuestManager.onLevelUp(target, data);
                                                            HunterWorldData.save();
                                                            ctx.getSource().sendMessage(Text.literal("§a[Hunter] +" + amount + " XP à "
                                                                    + target.getName().getString()
                                                                    + " (Niveau " + data.level + ")"));
                                                            return 1;
                                                        })))))

                        .then(CommandManager.literal("level")
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 200))
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                            int level = IntegerArgumentType.getInteger(ctx, "level");
                                                            HunterPlayerData data = HunterWorldData.get(target.getUuid());
                                                            data.level = level;
                                                            data.xp = 0;
                                                            HunterWorldData.save();
                                                            ctx.getSource().sendMessage(Text.literal("§a[Hunter] Niveau de "
                                                                    + target.getName().getString() + " défini à " + level));
                                                            target.sendMessage(Text.literal(
                                                                    "§b[Chasseur] Votre niveau a été défini à §l" + level), false);
                                                            return 1;
                                                        })))))

                        .then(CommandManager.literal("info")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                            HunterPlayerData data = HunterWorldData.get(target.getUuid());
                                            ctx.getSource().sendMessage(Text.literal(
                                                    "§e[Hunter] " + target.getName().getString()
                                                            + " — Niveau §b" + data.level
                                                            + " §7| XP : §b" + data.xp
                                                            + "§7/§b" + HunterPlayerData.xpForNextLevel(data.level)
                                                            + " §7| Progression : §b" + HunterUtils.getLevelProgressPercent(target.getUuid())
                                            ));
                                            return 1;
                                        })))

                        .then(CommandManager.literal("quests")
                                .then(CommandManager.literal("refresh")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                    QuestManager.assignQuests(target);
                                                    HunterWorldData.save();
                                                    ctx.getSource().sendMessage(Text.literal("§a[Hunter] Quêtes de "
                                                            + target.getName().getString() + " régénérées."));
                                                    return 1;
                                                }))))
        );
    }
}