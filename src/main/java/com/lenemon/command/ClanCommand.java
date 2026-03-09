package com.lenemon.command;

import com.lenemon.clan.ClanManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Toutes les commandes /clan.
 * Enregistrees via CommandRegistrationCallback dans Lenemon.java.
 */
public class ClanCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(CommandManager.literal("clan")

                // /clan create <nom> <tag>
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("nom", StringArgumentType.word())
                                .then(CommandManager.argument("tag", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                            String nom = StringArgumentType.getString(ctx, "nom");
                                            String tag = StringArgumentType.getString(ctx, "tag");
                                            ctx.getSource().getServer().execute(() ->
                                                    ClanManager.createClan(player, nom, tag));
                                            return 1;
                                        })
                                )
                        )
                )

                // /clan disband
                .then(CommandManager.literal("disband")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    ClanManager.disbandClan(player, ctx.getSource().getServer()));
                            return 1;
                        })
                )

                // /clan invite <joueur>
                .then(CommandManager.literal("invite")
                        .then(CommandManager.argument("joueur", EntityArgumentType.player())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "joueur");
                                    ctx.getSource().getServer().execute(() ->
                                            ClanManager.invitePlayer(player, target, ctx.getSource().getServer()));
                                    return 1;
                                })
                        )
                )

                // /clan accept
                .then(CommandManager.literal("accept")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    ClanManager.acceptInvite(player, ctx.getSource().getServer()));
                            return 1;
                        })
                )

                // /clan decline
                .then(CommandManager.literal("decline")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    ClanManager.declineInvite(player));
                            return 1;
                        })
                )

                // /clan kick <joueur>
                .then(CommandManager.literal("kick")
                        .then(CommandManager.argument("joueur", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    String targetName = StringArgumentType.getString(ctx, "joueur");
                                    ctx.getSource().getServer().execute(() ->
                                            ClanManager.kickMember(player, targetName, ctx.getSource().getServer()));
                                    return 1;
                                })
                        )
                )

                // /clan leave
                .then(CommandManager.literal("leave")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    ClanManager.leaveClan(player, ctx.getSource().getServer()));
                            return 1;
                        })
                )

                // /clan promote <joueur>
                .then(CommandManager.literal("promote")
                        .then(CommandManager.argument("joueur", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    String targetName = StringArgumentType.getString(ctx, "joueur");
                                    ctx.getSource().getServer().execute(() ->
                                            ClanManager.promote(player, targetName, ctx.getSource().getServer()));
                                    return 1;
                                })
                        )
                )

                // /clan demote <joueur>
                .then(CommandManager.literal("demote")
                        .then(CommandManager.argument("joueur", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    String targetName = StringArgumentType.getString(ctx, "joueur");
                                    ctx.getSource().getServer().execute(() ->
                                            ClanManager.demote(player, targetName, ctx.getSource().getServer()));
                                    return 1;
                                })
                        )
                )

                // /clan info [nom|tag]
                .then(CommandManager.literal("info")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    ClanManager.showInfo(player, null, ctx.getSource().getServer()));
                            return 1;
                        })
                        .then(CommandManager.argument("nom", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    String input = builder.getRemaining().toLowerCase();
                                    for (com.lenemon.clan.Clan c : com.lenemon.clan.ClanWorldData.getAll()) {
                                        if (c.name.toLowerCase().startsWith(input)) builder.suggest(c.name);
                                        if (c.tag.toLowerCase().startsWith(input))  builder.suggest(c.tag);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    String nom = StringArgumentType.getString(ctx, "nom");
                                    ctx.getSource().getServer().execute(() ->
                                            ClanManager.showInfo(player, nom, ctx.getSource().getServer()));
                                    return 1;
                                })
                        )
                )

                // /clan top
                .then(CommandManager.literal("top")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    ClanManager.showTop(player, ctx.getSource().getServer()));
                            return 1;
                        })
                )

                // /clan gui
                .then(CommandManager.literal("gui")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    com.lenemon.clan.ClanGuiHandler.sendGuiOpen(player));
                            return 1;
                        })
                )

                // /clan claim [exit] -- entre en mode claim, ou claim le chunk si deja actif
                .then(CommandManager.literal("claim")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() -> {
                                if (com.lenemon.clan.ClanClaimHandler.isInClaimMode(player.getUuid())) {
                                    com.lenemon.clan.ClanClaimHandler.claimCurrentChunk(player, ctx.getSource().getServer());
                                } else {
                                    com.lenemon.clan.ClanClaimHandler.enterClaimMode(player, ctx.getSource().getServer());
                                }
                            });
                            return 1;
                        })
                        .then(CommandManager.literal("exit")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    ctx.getSource().getServer().execute(() ->
                                            com.lenemon.clan.ClanClaimHandler.exitClaimMode(player));
                                    return 1;
                                })
                        )
                )

                // /clan unclaim -- unclaim le chunk actuel
                .then(CommandManager.literal("unclaim")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    com.lenemon.clan.ClanClaimHandler.unclaimCurrentChunk(player, ctx.getSource().getServer()));
                            return 1;
                        })
                )

                // /clan level -- acheter le prochain level economique
                .then(CommandManager.literal("level")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    com.lenemon.clan.ClanClaimHandler.handleBuyLevel(player, ctx.getSource().getServer()));
                            return 1;
                        })
                )

                // /clan bank deposit <montant>
                // /clan bank withdraw <montant>
                .then(CommandManager.literal("bank")
                        .then(CommandManager.literal("deposit")
                                .then(CommandManager.argument("montant", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                            long amount = LongArgumentType.getLong(ctx, "montant");
                                            ctx.getSource().getServer().execute(() ->
                                                    ClanManager.bankDeposit(player, amount));
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("withdraw")
                                .then(CommandManager.argument("montant", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                            long amount = LongArgumentType.getLong(ctx, "montant");
                                            ctx.getSource().getServer().execute(() ->
                                                    ClanManager.bankWithdraw(player, amount));
                                            return 1;
                                        })
                                )
                        )
                )

                // /clan help → affiche l'aide
                .then(CommandManager.literal("message")
                        .then(CommandManager.literal("enter")
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                            String message = StringArgumentType.getString(ctx, "message");
                                            ctx.getSource().getServer().execute(() ->
                                                    ClanManager.setTerritoryMessage(player, "enter", message));
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("leave")
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                            String message = StringArgumentType.getString(ctx, "message");
                                            ctx.getSource().getServer().execute(() ->
                                                    ClanManager.setTerritoryMessage(player, "leave", message));
                                            return 1;
                                        })
                                )
                        )
                )

                // /clan help → affiche l'aide
                .then(CommandManager.literal("help")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            player.sendMessage(Text.literal("§6[Clan] §rCommandes disponibles :"), false);
                            player.sendMessage(Text.literal("§e/clan create <nom> <tag>§7 - Creer un clan"), false);
                            player.sendMessage(Text.literal("§e/clan invite <joueur>§7 - Inviter un joueur"), false);
                            player.sendMessage(Text.literal("§e/clan accept §7/ §e/clan decline§7 - Repondre a une invitation"), false);
                            player.sendMessage(Text.literal("§e/clan kick <joueur>§7 - Exclure un membre"), false);
                            player.sendMessage(Text.literal("§e/clan leave§7 - Quitter le clan"), false);
                            player.sendMessage(Text.literal("§e/clan disband§7 - Dissoudre le clan"), false);
                            player.sendMessage(Text.literal("§e/clan promote §7/ §e/clan demote <joueur>§7 - Changer le rang"), false);
                            player.sendMessage(Text.literal("§e/clan info [nom]§7 - Informations sur un clan"), false);
                            player.sendMessage(Text.literal("§e/clan top§7 - Classement des clans"), false);
                            player.sendMessage(Text.literal("§e/clan bank deposit/withdraw <montant>§7 - Banque du clan"), false);
                            player.sendMessage(Text.literal("§e/clan claim§7 - Toggle mode claim de territoire"), false);
                            player.sendMessage(Text.literal("§e/clan claim exit§7 - Quitter le mode claim"), false);
                            player.sendMessage(Text.literal("§e/clan unclaim§7 - Retirer le claim du chunk actuel"), false);
                            player.sendMessage(Text.literal("§e/clan level§7 - Acheter le prochain level du clan"), false);
                            player.sendMessage(Text.literal("§e/clan message enter <message>§7 - Definir le message d'entree (§7codes & autorises§7)"), false);
                            player.sendMessage(Text.literal("§e/clan message leave <message>§7 - Definir le message de sortie (§7codes & autorises§7)"), false);
                            return 1;
                        })
                )

                // /clan (sans sous-commande) → ouvre le GUI
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                    ctx.getSource().getServer().execute(() ->
                            com.lenemon.clan.ClanGuiHandler.sendGuiOpen(player));
                    return 1;
                })
        );
    }
}
