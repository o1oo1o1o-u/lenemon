package com.lenemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.lenemon.config.VoteConfig;
import com.lenemon.vote.VoteRewardManager;
import com.lenemon.vote.VoteRewardStorage;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/**
 * The type Vote command.
 */
public class VoteCommand {

    /**
     * Register.
     *
     * @param dispatcher the dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("vote")

                        // /vote sans argument → lien pour joueur
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                source.sendError(Text.literal("Usage console : /vote <joueur>"));
                                return 0;
                            }
                            String url = VoteConfig.getVoteUrl();
                            player.sendMessage(Text.literal(""), false);
                            player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
                            player.sendMessage(Text.literal("§6§l  🗳️ Voter pour le serveur !"), false);
                            player.sendMessage(Text.literal("§7Soutenez le serveur en votant :"), false);
                            player.sendMessage(
                                    Text.literal("§b§l➤ " + url)
                                            .setStyle(Style.EMPTY
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                                    .withColor(net.minecraft.util.Formatting.AQUA)
                                                    .withBold(true)
                                                    .withUnderline(true)),
                                    false
                            );
                            player.sendMessage(Text.literal("§7Vous recevrez §f§lx2 Bon Cadeau Basic§7 !"), false);
                            player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
                            return 1;
                        })

                        // /vote <joueur> → RCON uniquement (niveau 4)
                        .then(CommandManager.argument("joueur", StringArgumentType.word())
                                .requires(src -> src.hasPermissionLevel(4))
                                .executes(ctx -> {
                                    String playerName = StringArgumentType.getString(ctx, "joueur");
                                    ServerCommandSource source = ctx.getSource();
                                    ServerPlayerEntity player = source.getServer()
                                            .getPlayerManager().getPlayer(playerName);

                                    if (player != null) {
                                        VoteRewardManager.giveReward(source.getServer(), player);
                                    } else {
                                        VoteRewardStorage.addPendingVote(source.getServer(), playerName);
                                        source.sendMessage(Text.literal(
                                                "[Vote] " + playerName + " hors ligne — récompense en attente."));
                                    }
                                    return 1;
                                })
                        )
        );
    }
}