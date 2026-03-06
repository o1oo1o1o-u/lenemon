package com.lenemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.lenemon.heal.HealPaperHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * The type Heal paper command.
 */
public class HealPaperCommand {

    /**
     * Register.
     *
     * @param dispatcher the dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("healpaper")
                        .requires(src -> Permissions.check(src, "custommenu.heal.admin", 2))
                        .then(CommandManager.literal("give")
                                .then(CommandManager.argument("target",
                                                net.minecraft.command.argument.EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target =
                                                    net.minecraft.command.argument.EntityArgumentType
                                                            .getPlayer(ctx, "target");
                                            target.giveItemStack(HealPaperHelper.createHealPaper());
                                            ctx.getSource().sendMessage(Text.literal(
                                                    "§a[Heal] Parchemin de soin donné à §f"
                                                            + target.getName().getString()));
                                            return 1;
                                        })
                                )
                        )
        );
    }
}