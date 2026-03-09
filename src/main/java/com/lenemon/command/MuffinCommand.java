package com.lenemon.command;

import com.lenemon.muffin.MagicMuffinHelper;
import com.lenemon.muffin.MagicMuffinType;
import com.lenemon.muffin.MuffinConfig;
import com.lenemon.muffin.MuffinPoolCache;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class MuffinCommand {

    private MuffinCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("lenemon")
                        .then(CommandManager.literal("muffin")
                                .then(CommandManager.literal("reload")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> executeReload(ctx.getSource()))
                                )
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .requires(source -> Permissions.check(source, "lenemon.muffin", 2))
                                        .then(CommandManager.argument("type", StringArgumentType.word())
                                                .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                                        List.of("normal", "shiny", "legendary"),
                                                        builder
                                                ))
                                                .executes(ctx -> executeGive(
                                                        ctx.getSource(),
                                                        EntityArgumentType.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "type")
                                                ))
                                        )
                                )
                        )
        );
    }

    private static int executeGive(ServerCommandSource source, ServerPlayerEntity target, String rawType) {
        MagicMuffinType type = MagicMuffinType.fromId(rawType);
        if (type == null) {
            source.sendError(Text.literal("§c[Muffin] Type invalide. Utilisez normal, shiny ou legendary."));
            return 0;
        }

        target.giveItemStack(MagicMuffinHelper.create(type));
        source.sendFeedback(() -> Text.literal("§a[Muffin] " + type.id() + " donne a §f" + target.getName().getString()), false);
        if (!source.getName().equalsIgnoreCase(target.getName().getString())) {
            target.sendMessage(Text.literal("§6[Muffin] Vous avez recu un " + type.displayName() + "§6."), false);
        }
        return 1;
    }

    private static int executeReload(ServerCommandSource source) {
        MuffinConfig.load(source.getServer());
        MuffinPoolCache.rebuild(source.getServer());
        source.sendFeedback(() -> Text.literal("§a[Muffin] Configuration et cache recharges."), false);
        return 1;
    }
}
