package com.lenemon.command;

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class CraftCommand {

    private CraftCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("craft")
                        .requires(source -> source.hasPermissionLevel(2)
                                || Permissions.check(source, "lenemon.craft", 2))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                ctx.getSource().sendError(Text.literal("Cette commande est réservée aux joueurs."));
                                return 0;
                            }
                            openCraftingTable(player);
                            return 1;
                        })
        );
    }

    private static void openCraftingTable(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, ignored) -> new CraftingScreenHandler(syncId, inv, ScreenHandlerContext.EMPTY) {
                    @Override
                    public boolean canUse(PlayerEntity player) {
                        return true;
                    }
                },
                Text.literal("Table de craft")
        ));
    }
}
