package com.lenemon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.lenemon.config.NightVisionConfig;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The type Night vision command.
 */
public class NightVisionCommand {

    /**
     * Register.
     *
     * @param dispatcher the dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /nv et /nightvision sont des alias → même logique
        for (String alias : new String[]{"nv", "nightvision"}) {
            dispatcher.register(
                    CommandManager.literal(alias)
                            // /nv → applique la NV au joueur
                            .executes(ctx -> applyNV(ctx.getSource()))

                            // /nv config
                            .then(CommandManager.literal("config")
                                    .requires(src -> Permissions.check(src, "custommenu.nv.admin", 2))

                                    // /nv config list
                                    .then(CommandManager.literal("list")
                                            .executes(ctx -> listConfig(ctx.getSource()))
                                    )

                                    // /nv config set <grade> <secondes>
                                    .then(CommandManager.literal("set")
                                            .then(CommandManager.argument("grade", StringArgumentType.word())
                                                    .then(CommandManager.argument("secondes", IntegerArgumentType.integer(-1))
                                                            .executes(ctx -> {
                                                                String grade = StringArgumentType.getString(ctx, "grade");
                                                                int seconds = IntegerArgumentType.getInteger(ctx, "secondes");
                                                                NightVisionConfig.set(grade, seconds);
                                                                ctx.getSource().sendMessage(Text.literal(
                                                                        "§a[NV] Grade §f" + grade + "§a configuré : §f"
                                                                                + (seconds == -1 ? "Permanent" : seconds + "s")));
                                                                return 1;
                                                            })
                                                    )
                                            )
                                    )

                                    // /nv config remove <grade>
                                    .then(CommandManager.literal("remove")
                                            .then(CommandManager.argument("grade", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        String grade = StringArgumentType.getString(ctx, "grade");
                                                        NightVisionConfig.remove(grade);
                                                        ctx.getSource().sendMessage(Text.literal(
                                                                "§c[NV] Grade §f" + grade + "§c supprimé."));
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
            );
        }
    }

    private static int applyNV(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Joueurs uniquement."));
            return 0;
        }

        // Récupère les grades LuckPerms du joueur
        Set<String> grades = getPlayerGrades(player);

        int duration = NightVisionConfig.getBestDuration(grades);

        if (duration == -2) {
            player.sendMessage(Text.literal("§c[NV] Vous n'avez pas accès à la vision nocturne."), false);
            return 0;
        }

        // Vérifie si le joueur a déjà la NV → toggle off
        if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            player.sendMessage(Text.literal("§7[NV] Vision nocturne désactivée."), false);
            return 1;
        }

        // Applique la NV
        int ticks = duration == -1 ? Integer.MAX_VALUE : duration * 20;
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION, ticks, 0, false, false, true));

        String msg = duration == -1
                ? "§a[NV] Vision nocturne activée §7(permanent)§a."
                : "§a[NV] Vision nocturne activée §7(" + duration + "s)§a.";
        player.sendMessage(Text.literal(msg), false);
        return 1;
    }

    private static int listConfig(ServerCommandSource source) {
        Map<String, Integer> all = NightVisionConfig.getAll();
        source.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        source.sendMessage(Text.literal("§e§lConfiguration Night Vision"));
        source.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        if (all.isEmpty()) {
            source.sendMessage(Text.literal("§7Aucun grade configuré."));
        } else {
            all.forEach((grade, seconds) ->
                    source.sendMessage(Text.literal("§7- §f" + grade + " §7: §e"
                            + (seconds == -1 ? "Permanent" : seconds + "s"))));
        }
        source.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return 1;
    }

    private static java.util.Set<String> getPlayerGrades(net.minecraft.server.network.ServerPlayerEntity player) {
        java.util.Set<String> grades = new java.util.HashSet<>();

        var api = com.lenemon.compat.LuckPermsCompat.getApiOrNull();
        if (api == null) return grades;

        User user = api.getUserManager().getUser(player.getUuid());
        if (user == null) return grades;

        for (Node node : user.getNodes()) {
            String key = node.getKey();
            if (key != null && key.startsWith("group.")) {
                grades.add(key.substring("group.".length()));
            }
        }

        return grades;
    }
}