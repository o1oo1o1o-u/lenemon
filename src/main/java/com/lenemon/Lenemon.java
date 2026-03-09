package com.lenemon;

import com.lenemon.armor.ArmorEffectHandler;
import com.lenemon.armor.bonus.PokemonXpBonus;
import com.lenemon.armor.bonus.SetBonusManager;
import com.lenemon.armor.bonus.ShinyBonus;
import com.lenemon.casino.*;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import com.lenemon.discord.DiscordWebhookConfig;
import com.lenemon.discord.DiscordCommand;
import com.lenemon.discord.DiscordWebhookSender;
import com.lenemon.casino.screen.CasinoScreenRegistry;
import com.lenemon.command.*;
import com.lenemon.discord.DiscordCommand;
import com.lenemon.hunter.HunterManager;
import com.lenemon.hunter.HunterUtils;
import com.lenemon.hunter.quest.QuestConfigLoader;
import com.lenemon.hunter.reward.LevelRewardConfig;
import com.lenemon.item.ItemDespawnManager;
import com.lenemon.item.ModItems;
import com.lenemon.network.LenemonNetwork;
import com.lenemon.network.PacketHudBalance;
import com.lenemon.network.PacketHudHunter;
import com.lenemon.item.pickaxe.ExcaveonLevel;
import com.lenemon.item.pickaxe.ExcaveonPickaxe;
import com.lenemon.item.pickaxe.ExcaveonUserConfig;
import com.lenemon.network.pickaxe.ExcaveonOpenGuiPayload;
import com.lenemon.network.pickaxe.ExcaveonUserConfigPayload;
import com.lenemon.pickaxe.ExcaveonConfigLoader;
import com.lenemon.pickaxe.ExcaveonManager;
import com.lenemon.pokedex.PokedexClaimedStorage;
import com.lenemon.pokedex.PokedexRewardConfig;
import com.lenemon.pokedex.PokedexService;
import com.lenemon.playtime.PlaytimeConfig;
import com.lenemon.playtime.PlaytimeTracker;
import com.lenemon.network.menu.MenuActionHandler;
import com.lenemon.enchantment.AutoSmeltEnchantment;
import com.lenemon.pokemon.ShinyAnnouncer;

// Ces imports seront resolus par IntelliJ apres Optimize Imports
import com.lenemon.casino.holo.CasinoHolograms;
import com.lenemon.config.VoteConfig;
import com.lenemon.config.NightVisionConfig;
import com.lenemon.fly.FlyTimerManager;
import com.lenemon.gift.GiftChestData;
import com.lenemon.gift.GiftDeleteSession;
import com.lenemon.gift.GiftItemHelper;
import com.lenemon.gift.GiftPreviewScreen;
import com.lenemon.heal.HealPaperHelper;
import com.lenemon.hud.HudFlightTracker;
import com.lenemon.player.PlayerHudTicker;
import com.lenemon.player.PlayerItemEvents;
import com.lenemon.network.shop.ShopActionHandler;
import com.lenemon.shop.ShopConfig;
import com.lenemon.shop.ShopSellPendingService;
import com.lenemon.shop.ShopSellService;
import com.lenemon.util.EconomyHelper;
import com.lenemon.vote.VoteRewardManager;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import com.lenemon.block.ElevatorEventHandler;
import com.lenemon.registry.ModBlocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * The type Lenemon.
 */
public class Lenemon implements ModInitializer {

    /**
     * The constant MOD_ID.
     */
    public static final String MOD_ID = "lenemon";
    /**
     * The constant LOGGER.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        // ── Core ──────────────────────────────────────────────────────────────
        ModItems.registerAll();
        ModBlocks.register();
        LenemonNetwork.register();
        LenemonCommand.register();

        // ── Systèmes gameplay ─────────────────────────────────────────────────
        ElevatorEventHandler.register();
        ArmorEffectHandler.register();
        SetBonusManager.register();
        PokemonXpBonus.register();
        ShinyBonus.register();
        AutoSmeltEnchantment.register();
        ShinyAnnouncer.register();

        ExcaveonConfigLoader.load();
        ExcaveonManager.register();
        registerExcaveonGuiEvents();

        PokedexRewardConfig.load();
        PokedexClaimedStorage.load();

        QuestConfigLoader.load();
        LevelRewardConfig.load();
        HunterManager.register();

        // ── Casino ────────────────────────────────────────────────────────────
        CasinoScreenRegistry.register();
        CasinoConfigSession.register();
        CasinoSpinTicker.register();
        CasinoHologramBootstrap.register();
        CasinoServerEvents.register();   // ← nouveau

        // ── Joueur ────────────────────────────────────────────────────────────
        PlayerHudTicker.register();      // ← nouveau
        PlayerItemEvents.register();     // ← nouveau
        ItemDespawnManager.register();   // ← nouveau
        HudFlightTracker.register();

        // ── Gift ──────────────────────────────────────────────────────────────
        GiftDeleteSession.register();
        registerGiftBlockEvents();       // ← méthode privée en bas

        // ── Discord ───────────────────────────────────────────────────────────
        DiscordWebhookConfig.load();
        DiscordCommand.register();
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) ->
                DiscordWebhookSender.send(sender.getName().getString(),
                        message.getContent().getString()));

        // ── Clan chat tag ─────────────────────────────────────────────────────
        com.lenemon.clan.ClanChatFormatter.register();

        // ── Clan territoire protection ─────────────────────────────────────────
        com.lenemon.clan.ClanTerritoryProtection.register();

        // ── Lifecycle serveur ─────────────────────────────────────────────────
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            NightVisionConfig.load(server);
            FlyTimerManager.register();
            FlyTimerManager.loadFromDisk(server);
            VoteRewardManager.register();
            VoteRewardManager.processPending(server);
            VoteConfig.load(server);
            com.lenemon.muffin.MuffinConfig.load(server);
            com.lenemon.muffin.MuffinPoolCache.rebuild(server);
            PlaytimeConfig.load(server);
            ShopConfig.reload(server);
            ShopSellService.invalidateCache();
            // Clan system
            com.lenemon.clan.ClanConfig.load();
            com.lenemon.clan.ClanWorldData.register(server);
        });

        // ── Commandes ─────────────────────────────────────────────────────────
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerShortcutCommands(dispatcher);
            MenuCommand.register(dispatcher);
            CasinoGiveCommand.register(dispatcher);
            MuffinCommand.register(dispatcher);
            NightVisionCommand.register(dispatcher);
            FlyFeatherCommand.register(dispatcher);
            HealPaperCommand.register(dispatcher);
            VoteCommand.register(dispatcher);
            HunterCommand.register(dispatcher);
            PlaytimeCommand.register(dispatcher);
            registerShopCommands(dispatcher);
            registerGiftCommands(dispatcher);
            registerAhCommands(dispatcher);
            com.lenemon.command.ClanCommand.register(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(CasinoSpinScheduler::tick);
        ServerTickEvents.END_SERVER_TICK.register(com.lenemon.ah.AhExpiryTicker::tick);
        ServerTickEvents.END_SERVER_TICK.register(com.lenemon.clan.ClanTerritoryMessageTracker::tick);
        ServerTickEvents.END_SERVER_TICK.register(PlaytimeTracker::tick);

        // Nettoyage des invitations de clan a la deconnexion + lastSeen
        // Le suffix LP est persistant : pas besoin de le retirer a la deconnexion.
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID disconnectUuid = handler.player.getUuid();
            com.lenemon.clan.ClanInviteSession.remove(disconnectUuid);
            com.lenemon.clan.ClanClaimHandler.onPlayerDisconnect(disconnectUuid);
            com.lenemon.clan.ClanTerritoryMessageTracker.onPlayerDisconnect(disconnectUuid);
            PlaytimeTracker.onDisconnect(handler.player);
            if (com.lenemon.clan.ClanWorldData.isInClan(disconnectUuid)) {
                com.lenemon.clan.ClanWorldData.setLastSeen(disconnectUuid, System.currentTimeMillis());
            }
        });

        // Synchronisation du suffix LP a la connexion (filet de securite :
        // corrige un suffix stale si le joueur a ete kick/exclu hors-ligne)
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID joinUuid = handler.player.getUuid();
            com.lenemon.clan.Clan joinClan = com.lenemon.clan.ClanWorldData.getClanOf(joinUuid);
            if (joinClan != null) {
                com.lenemon.compat.LuckPermsCompat.setClanSuffix(joinUuid, joinClan.tag);
            } else {
                // Efface un eventuel suffix restant si le joueur n'est plus dans un clan
                com.lenemon.compat.LuckPermsCompat.clearClanSuffix(joinUuid);
            }
        });

        LOGGER.info("LeNeMon: init terminé.");
    }

    private static void registerShortcutCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
        // /world et /monde → GUI téléportation
        for (String alias : new String[]{"world", "monde"}) {
            dispatcher.register(
                    CommandManager.literal(alias)
                            .executes(ctx -> {
                                if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                ctx.getSource().getServer().execute(() -> MenuActionHandler.sendTpMenuOpen(player));
                                return 1;
                            })
            );
        }
        // /chasseur et /quetes → GUI Chasseur
        for (String alias : new String[]{"chasseur", "quetes"}) {
            dispatcher.register(
                    CommandManager.literal(alias)
                            .executes(ctx -> {
                                if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                ctx.getSource().getServer().execute(() -> MenuActionHandler.sendHunterMenuOpen(player));
                                return 1;
                            })
            );
        }
        // /dex → GUI Pokédex rewards
        dispatcher.register(
                CommandManager.literal("dex")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() -> PokedexService.sendPokedexOpen(player));
                            return 1;
                        })
        );
    }

    private static void registerShopCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("shop")

                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ShopActionHandler.sendShopOpen(player);
                            return 1;
                        })

                        .then(CommandManager.literal("reload")
                                // tu as change de namespace donc permissions aussi
                                .requires(src -> Permissions.check(src, "lenemon.shop.admin", 2))
                                .executes(ctx -> {
                                    ShopConfig.reload(ctx.getSource().getServer());
                                    ctx.getSource().sendMessage(Text.literal("§a[Shop] Config rechargée !"));
                                    return 1;
                                })
                        )

                        .then(CommandManager.literal("sellpending")
                                .requires(src -> src.hasPermissionLevel(4))
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("percent", FloatArgumentType.floatArg(0f))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                    float percent = FloatArgumentType.getFloat(ctx, "percent");
                                                    return ShopSellPendingService.sellPending(target, percent);
                                                })
                                        )
                                )
                        )
        );
    }

    private static void registerGiftCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("gift")
                        .requires(src -> Permissions.check(src, "lenemon.gift.admin", 2))

                        .then(CommandManager.literal("removepos")
                                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("y", IntegerArgumentType.integer())
                                                .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                                            BlockPos pos = new BlockPos(x, y, z);

                                                            for (ServerWorld w : ctx.getSource().getServer().getWorlds()) {
                                                                GiftChestData data = GiftChestData.get(w);
                                                                if (data.isRegisteredAt(pos)) {
                                                                    data.removeChest(pos);
                                                                    ctx.getSource().sendMessage(Text.literal("§a[Gift] Supprimé à " + pos));
                                                                    return 1;
                                                                }
                                                            }
                                                            ctx.getSource().sendError(Text.literal("§cAucun coffre à cette position."));
                                                            return 0;
                                                        })
                                                )
                                        )
                                )
                        )

                        .then(CommandManager.literal("list")
                                .executes(ctx -> {
                                    for (ServerWorld w : ctx.getSource().getServer().getWorlds()) {
                                        String worldName = w.getRegistryKey().getValue().toString();
                                        GiftChestData data = GiftChestData.get(w);
                                        if (data.getAll().isEmpty()) continue;

                                        ctx.getSource().sendMessage(Text.literal("§e=== " + worldName + " ==="));
                                        data.getAll().forEach((pos, entry) -> {
                                            ctx.getSource().sendMessage(Text.literal(
                                                    "§7[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "] " +
                                                            "§f" + entry.chestName + " §8" + entry.chestUUID
                                            ));
                                        });
                                    }
                                    return 1;
                                })
                        )

                        .then(CommandManager.literal("give")
                                .then(CommandManager.argument("chest", StringArgumentType.word())
                                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                                .executes(ctx -> {
                                                    String chestName = StringArgumentType.getString(ctx, "chest");
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");

                                                    UUID chestUUID = null;
                                                    for (ServerWorld w : ctx.getSource().getServer().getWorlds()) {
                                                        GiftChestData data = GiftChestData.get(w);
                                                        var entry = data.getAll().values().stream()
                                                                .filter(e -> e.chestName.equalsIgnoreCase(chestName))
                                                                .findFirst().orElse(null);
                                                        if (entry != null) {
                                                            chestUUID = entry.chestUUID;
                                                            break;
                                                        }
                                                    }

                                                    if (chestUUID == null) {
                                                        ctx.getSource().sendError(Text.literal("§c[Gift] Coffre '" + chestName + "' introuvable."));
                                                        return 0;
                                                    }

                                                    target.giveItemStack(GiftItemHelper.createGiftTicket(chestUUID, chestName));
                                                    ctx.getSource().sendMessage(Text.literal(
                                                            "§a[Gift] Bon §f" + chestName + "§a donné à §f" + target.getName().getString()
                                                    ));
                                                    return 1;
                                                })
                                                .then(CommandManager.argument("quantite", IntegerArgumentType.integer(1, 64))
                                                        .executes(ctx -> {
                                                            String chestName = StringArgumentType.getString(ctx, "chest");
                                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                                            int qty = IntegerArgumentType.getInteger(ctx, "quantite");

                                                            UUID chestUUID = null;
                                                            for (ServerWorld w : ctx.getSource().getServer().getWorlds()) {
                                                                GiftChestData data = GiftChestData.get(w);
                                                                var entry = data.getAll().values().stream()
                                                                        .filter(e -> e.chestName.equalsIgnoreCase(chestName))
                                                                        .findFirst().orElse(null);
                                                                if (entry != null) {
                                                                    chestUUID = entry.chestUUID;
                                                                    break;
                                                                }
                                                            }

                                                            if (chestUUID == null) {
                                                                ctx.getSource().sendError(Text.literal("§c[Gift] Coffre '" + chestName + "' introuvable."));
                                                                return 0;
                                                            }

                                                            for (int i = 0; i < qty; i++) {
                                                                target.giveItemStack(GiftItemHelper.createGiftTicket(chestUUID, chestName));
                                                            }

                                                            ctx.getSource().sendMessage(Text.literal(
                                                                    "§a[Gift] §f" + qty + "§a bon(s) §f" + chestName + "§a donné(s) à §f"
                                                                            + target.getName().getString()
                                                            ));
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )

                        .then(CommandManager.literal("create")
                                .then(CommandManager.argument("couleur", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            GiftItemHelper.COLORS.keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .then(CommandManager.argument("nom", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                                        ctx.getSource().sendError(Text.literal("Joueurs uniquement."));
                                                        return 0;
                                                    }
                                                    String color = StringArgumentType.getString(ctx, "couleur");
                                                    String nom = StringArgumentType.getString(ctx, "nom");

                                                    if (!GiftItemHelper.COLORS.containsKey(color.toLowerCase())) {
                                                        ctx.getSource().sendError(Text.literal(
                                                                "§c[Gift] Couleur invalide. Disponibles : " + String.join(", ", GiftItemHelper.COLORS.keySet())
                                                        ));
                                                        return 0;
                                                    }

                                                    for (ServerWorld w : ctx.getSource().getServer().getWorlds()) {
                                                        GiftChestData d = GiftChestData.get(w);
                                                        boolean exists = d.getAll().values().stream()
                                                                .anyMatch(e -> e.chestName.equalsIgnoreCase(nom));
                                                        if (exists) {
                                                            ctx.getSource().sendError(Text.literal(
                                                                    "§c[Gift] Un coffre nommé §f" + nom + "§c existe déjà !"
                                                            ));
                                                            return 0;
                                                        }
                                                    }

                                                    player.giveItemStack(GiftItemHelper.createGiftChestItem(nom, color));
                                                    ctx.getSource().sendMessage(Text.literal(
                                                            "§a[Gift] Coffre §f" + nom + "§a (§f" + color + "§a) créé ! Posez-le pour l'activer."
                                                    ));
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }

    private static void registerAhCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("ah")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            ctx.getSource().getServer().execute(() ->
                                    com.lenemon.ah.AhActionHandler.sendBrowse(player, ctx.getSource().getServer(), 0, "all", "date_listed"));
                            return 1;
                        })
                        .then(CommandManager.literal("mystuff")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    ctx.getSource().getServer().execute(() ->
                                            com.lenemon.ah.AhActionHandler.sendMyListings(player, ctx.getSource().getServer()));
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("admin")
                                .requires(src -> Permissions.check(src, "lenemon.ah.admin", 2))
                                .then(CommandManager.literal("remove")
                                        .then(CommandManager.argument("listingId", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                                    String id = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "listingId");
                                                    ctx.getSource().getServer().execute(() -> {
                                                        com.lenemon.ah.AhWorldData data = com.lenemon.ah.AhWorldData.get(ctx.getSource().getServer().getOverworld());
                                                        try {
                                                            data.expireListing(UUID.fromString(id));
                                                            player.sendMessage(Text.literal("§a[AH] Vente retirée."), false);
                                                        } catch (IllegalArgumentException e) {
                                                            player.sendMessage(Text.literal("§c[AH] ID invalide."), false);
                                                        }
                                                    });
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }

    /**
     * Registers the Excaveon GUI config system:
     *  - UseItemCallback (server-side): shift+right-click in air with the pickaxe opens the config GUI.
     *  - C2S receiver: validates and applies the config chosen by the player.
     */
    private static void registerExcaveonGuiEvents() {
        // Shift+use in air → send S2C payload to open the GUI on the client
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!player.isSneaking()) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof com.lenemon.item.pickaxe.ExcaveonPickaxe)) {
                return TypedActionResult.pass(stack);
            }

            int level  = ExcaveonPickaxe.getLevel(stack);
            int blocks = ExcaveonPickaxe.getBlocks(stack);
            ExcaveonUserConfig cfg = ExcaveonPickaxe.getUserConfig(stack);

            ServerPlayNetworking.send(serverPlayer, new ExcaveonOpenGuiPayload(
                    level, blocks, cfg.autoSell, cfg.autoSmelt, cfg.miningMode
            ));
            return TypedActionResult.success(stack);
        });

        // C2S receiver: player confirmed a new config
        ServerPlayNetworking.registerGlobalReceiver(ExcaveonUserConfigPayload.ID, (payload, ctx) -> {
            ctx.server().execute(() -> {
                ServerPlayerEntity player = ctx.player();
                ItemStack stack = player.getMainHandStack();
                if (!(stack.getItem() instanceof ExcaveonPickaxe)) {
                    // Try off-hand as fallback
                    stack = player.getOffHandStack();
                    if (!(stack.getItem() instanceof ExcaveonPickaxe)) return;
                }

                int level = ExcaveonPickaxe.getLevel(stack);
                ExcaveonLevel lvl = ExcaveonLevel.fromLevel(level);

                // Validate autoSell: only allowed if level unlocks it
                boolean autoSell = payload.autoSell() && lvl.autoSell;

                // Validate miningMode: requested depth must not exceed level's depth
                String mode = payload.miningMode();
                if (!ExcaveonUserConfig.isModeUnlocked(mode, level)) {
                    mode = ExcaveonUserConfig.bestModeForLevel(level);
                }

                ExcaveonUserConfig cfg = new ExcaveonUserConfig(autoSell, payload.autoSmelt(), mode);
                ExcaveonPickaxe.setUserConfig(stack, cfg);
            });
        });
    }

    private static void registerGiftBlockEvents() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            String blockId = net.minecraft.registry.Registries.BLOCK
                    .getId(serverWorld.getBlockState(pos).getBlock()).toString();

            if (!GiftItemHelper.COLORS.containsValue(blockId)) return ActionResult.PASS;

            GiftChestData data = GiftChestData.get(serverWorld);
            BlockPos chestPos = null;
            if (data.isRegisteredAt(pos)) chestPos = pos;
            else if (data.isRegisteredAt(pos.down())) chestPos = pos.down();
            if (chestPos == null) return ActionResult.PASS;

            final GiftChestData.ChestEntry chest = data.getChest(chestPos);
            player.getServer().execute(() -> GiftPreviewScreen.open(serverPlayer, chest, serverWorld));
            return ActionResult.SUCCESS;
        });
    }
}
