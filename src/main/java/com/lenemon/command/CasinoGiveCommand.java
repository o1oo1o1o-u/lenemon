package com.lenemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.lenemon.casino.CasinoItemHelper;
import com.lenemon.casino.holo.CasinoHolograms;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * The type Casino give command.
 */
public class CasinoGiveCommand {

    /**
     * Register.
     *
     * @param dispatcher the dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("casino")
                        .then(CommandManager.literal("give")
                                .requires(src -> Permissions.check(src, "custommenu.casino.give", 2))
                                .executes(ctx -> {
                                    ServerCommandSource source = ctx.getSource();
                                    if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                        source.sendError(Text.literal("Joueurs uniquement. Utilisez /casino give <joueur>"));
                                        return 0;
                                    }
                                    giveCasino(player);
                                    return 1;
                                })
                                .then(CommandManager.argument("target", net.minecraft.command.argument.EntityArgumentType.player())
                                        .requires(src -> Permissions.check(src, "custommenu.casino.give", 2))
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "target");
                                            giveCasino(target);
                                            ctx.getSource().sendMessage(Text.literal("§a[Casino] Item casino donné à §f" + target.getName().getString()));
                                            return 1;
                                        })
                                )
                        )
                        // /casino purgeholos
                        .then(CommandManager.literal("purgeholos")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> {
                                    CasinoHolograms.purgeAll();
                                    ctx.getSource().sendFeedback(
                                            () -> Text.literal("§a[Casino] Tous les holograms ont été purgés."),
                                            false
                                    );
                                    return 1;
                                })
                        )
        );
    }

    private static void giveCasino(ServerPlayerEntity player) {
        ItemStack pcItem = CasinoItemHelper.createCasinoItem();
        player.giveItemStack(pcItem);
        player.sendMessage(Text.literal("§a[Casino] Item casino obtenu ! Posez-le pour créer un casino."), false);
    }
}