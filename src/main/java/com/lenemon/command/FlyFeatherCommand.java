package com.lenemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.lenemon.fly.FlyFeatherHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * The type Fly feather command.
 */
public class FlyFeatherCommand {

    /**
     * Register.
     *
     * @param dispatcher the dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("flyfeather")
                        .requires(src -> Permissions.check(src, "custommenu.fly.admin", 2))

                        // /flyfeather give <joueur> <secondes> (-1 = permanent)
                        .then(CommandManager.literal("give")
                                .then(CommandManager.argument("target",
                                                net.minecraft.command.argument.EntityArgumentType.player())
                                        .then(CommandManager.argument("secondes",
                                                        IntegerArgumentType.integer(-1))
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest(-1, Text.literal("Permanent"));
                                                    builder.suggest(600, Text.literal("10 minutes"));
                                                    builder.suggest(1800, Text.literal("30 minutes"));
                                                    builder.suggest(3600, Text.literal("1 heure"));
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target =
                                                            net.minecraft.command.argument.EntityArgumentType
                                                                    .getPlayer(ctx, "target");
                                                    int seconds = IntegerArgumentType.getInteger(ctx, "secondes");

                                                    target.giveItemStack(FlyFeatherHelper.createFeather(seconds));
                                                    ctx.getSource().sendMessage(Text.literal(
                                                            "§a[Fly] Plume §f("
                                                                    + (seconds == -1 ? "Permanente" : seconds + "s")
                                                                    + "§a) donnée à §f" + target.getName().getString()));
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }
}