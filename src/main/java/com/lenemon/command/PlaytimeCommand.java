package com.lenemon.command;

import com.lenemon.playtime.PlaytimeService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class PlaytimeCommand {

    private PlaytimeCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("playtime")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                ctx.getSource().sendError(Text.literal("[LeNeMon] Cette commande est réservée aux joueurs."));
                                return 0;
                            }
                            PlaytimeService.sendOpen(player);
                            return 1;
                        })
                        .then(CommandManager.literal("reset")
                                .requires(source -> Permissions.check(source, "lenemon.playtime.admin", 2))
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> executeReset(
                                                ctx.getSource(),
                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                "all"
                                        ))
                                        .then(CommandManager.argument("tier", StringArgumentType.word())
                                                .suggests((ctx, builder) -> CommandSource.suggestMatching(buildTierSuggestions(), builder))
                                                .executes(ctx -> executeReset(
                                                        ctx.getSource(),
                                                        EntityArgumentType.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "tier")
                                                ))
                                        )
                                )
                        )
        );
    }

    private static int executeReset(ServerCommandSource source, ServerPlayerEntity target, String tierRaw) {
        String tier = normalizeTier(tierRaw);
        int reset = PlaytimeService.resetClaims(source.getServer(), target.getUuid(), tier);
        if (reset <= 0) {
            source.sendError(Text.literal("[LeNeMon] Aucun claim à reset pour " + target.getName().getString() + "."));
            return 0;
        }

        if ("all".equals(tier)) {
            source.sendMessage(Text.literal("§a[Playtime] §f" + reset + " récompense(s) reset pour §e" + target.getName().getString()));
        } else {
            source.sendMessage(Text.literal("§a[Playtime] §fPalier §e" + tier + " §freset pour §e" + target.getName().getString()));
        }
        return 1;
    }

    private static String normalizeTier(String raw) {
        String value = raw == null ? "all" : raw.trim().toLowerCase();
        return switch (value) {
            case "1h" -> "tier_1";
            case "5h" -> "tier_2";
            case "10h" -> "tier_3";
            case "25h" -> "tier_4";
            case "50h" -> "tier_5";
            case "100h" -> "tier_6";
            case "150h" -> "tier_7";
            case "250h" -> "tier_8";
            case "350h" -> "tier_9";
            case "500h" -> "tier_10";
            default -> value;
        };
    }

    private static List<String> buildTierSuggestions() {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("all");
        suggestions.addAll(List.of("1h", "5h", "10h", "25h", "50h", "100h", "150h", "250h", "350h", "500h"));
        suggestions.addAll(List.of("tier_1", "tier_2", "tier_3", "tier_4", "tier_5", "tier_6", "tier_7", "tier_8", "tier_9", "tier_10"));
        return suggestions;
    }
}
