package com.lenemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.lenemon.network.menu.MenuActionHandler;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * The type Menu command.
 */
public class MenuCommand {

    /**
     * Register.
     *
     * @param dispatcher the dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("menu")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (source.getEntity() instanceof ServerPlayerEntity player) {
                                MenuActionHandler.sendMenuOpen(player);
                            } else {
                                source.sendError(Text.literal("Cette commande est réservée aux joueurs."));
                            }
                            return 1;
                        })
        );

        dispatcher.register(
                CommandManager.literal("ec")
                        .requires(src -> Permissions.check(src, "custommenu.ec", 2))
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                source.sendError(Text.literal("Joueurs uniquement."));
                                return 0;
                            }
                            player.getServer().getCommandManager().executeWithPrefix(
                                    player.getCommandSource().withMaxLevel(4),
                                    "enderchest"
                            );
                            return 1;
                        })
        );

        dispatcher.register(
                CommandManager.literal("custommenu")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("vote")
                                .then(CommandManager.literal("reload")
                                        .executes(ctx -> {
                                            com.lenemon.config.VoteConfig.load(ctx.getSource().getServer());
                                            ctx.getSource().sendMessage(Text.literal(
                                                    "§a[Vote] Configuration rechargée ! URL : §f"
                                                            + com.lenemon.config.VoteConfig.getVoteUrl()));
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("nv")
                                .then(CommandManager.literal("reload")
                                        .executes(ctx -> {
                                            com.lenemon.config.NightVisionConfig.load(ctx.getSource().getServer());
                                            ctx.getSource().sendMessage(Text.literal(
                                                    "§a[NV] Configuration rechargée !"));
                                            return 1;
                                        })
                                )
                        )
        );
    }
}