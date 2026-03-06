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
import com.lenemon.pickaxe.ExcaveonConfigLoader;
import com.lenemon.pickaxe.ExcaveonManager;
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

        // ── Gift ──────────────────────────────────────────────────────────────
        GiftDeleteSession.register();
        registerGiftBlockEvents();       // ← méthode privée en bas

        // ── Discord ───────────────────────────────────────────────────────────
        DiscordWebhookConfig.load();
        DiscordCommand.register();
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) ->
                DiscordWebhookSender.send(sender.getName().getString(),
                        message.getContent().getString()));

        // ── Lifecycle serveur ─────────────────────────────────────────────────
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            NightVisionConfig.load(server);
            FlyTimerManager.register();
            FlyTimerManager.loadFromDisk(server);
            VoteRewardManager.register();
            VoteRewardManager.processPending(server);
            VoteConfig.load(server);
            ShopConfig.reload(server);
            ShopSellService.invalidateCache();
        });

        // ── Commandes ─────────────────────────────────────────────────────────
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MenuCommand.register(dispatcher);
            CasinoGiveCommand.register(dispatcher);
            NightVisionCommand.register(dispatcher);
            FlyFeatherCommand.register(dispatcher);
            HealPaperCommand.register(dispatcher);
            VoteCommand.register(dispatcher);
            HunterCommand.register(dispatcher);
            registerShopCommands(dispatcher);
            registerGiftCommands(dispatcher);
            registerAhCommands(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(CasinoSpinScheduler::tick);
        ServerTickEvents.END_SERVER_TICK.register(com.lenemon.ah.AhExpiryTicker::tick);

        LOGGER.info("LeNeMon: init terminé.");
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