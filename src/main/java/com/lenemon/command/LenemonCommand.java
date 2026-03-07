package com.lenemon.command;

import com.lenemon.armor.ArmorEffectHandler;
import com.lenemon.pokedex.PokedexClaimedStorage;
import com.lenemon.armor.BaseSpawnInfluence;
import com.lenemon.armor.config.LoreBuilder;
import com.lenemon.armor.sets.DevArmorSet;
import com.lenemon.armor.sets.RayArmorSet;
import com.lenemon.item.ModItems;
import com.lenemon.item.pickaxe.ExcaveonPickaxe;
import com.lenemon.network.LenemonNetwork;
import com.lenemon.pickaxe.ExcaveonManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


import net.minecraft.item.Item;
import java.util.List;

/**
 * The type Lenemon command.
 */
public class LenemonCommand {

    /**
     * Register.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("lenemon")
                            .requires(source -> source.hasPermissionLevel(2)) // OP niveau 2
                            .then(CommandManager.literal("armor")
                                    .then(CommandManager.literal("reload")
                                            .executes(ctx -> executeReload(ctx.getSource()))
                                    )
                            )
                            .then(CommandManager.literal("excaveon")
                                    .then(CommandManager.literal("reload")
                                            .executes(ctx -> executeExcaveonReload(ctx.getSource()))
                                    )
                            )
                            .then(CommandManager.literal("nymphalie")
                                    .requires(source -> Permissions.check(source, "lenemon.pickaxe.addblocks", 2))
                                    .then(CommandManager.literal("addblocks")
                                            .then(CommandManager.argument("quantite", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> executeNymphalieAddBlocks(
                                                            ctx.getSource(),
                                                            IntegerArgumentType.getInteger(ctx, "quantite")
                                                    ))
                                            )
                                    )
                            )
                            .then(CommandManager.literal("dex")
                                    .then(CommandManager.literal("rewardreset")
                                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                                    .then(CommandManager.argument("region", StringArgumentType.word())
                                                            .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                                                    List.of("all", "national", "kanto", "johto", "hoenn",
                                                                            "sinnoh", "unova", "kalos", "alola",
                                                                            "galar", "hisui", "paldea", "unknown"),
                                                                    builder
                                                            ))
                                                            .executes(ctx -> executeDexRewardReset(
                                                                    ctx.getSource(),
                                                                    EntityArgumentType.getPlayer(ctx, "player"),
                                                                    StringArgumentType.getString(ctx, "region")
                                                            ))
                                                    )
                                            )
                                    )
                            )
                            .then(CommandManager.literal("give")
                                    .then(CommandManager.argument("player", EntityArgumentType.player())
                                            .then(CommandManager.argument("set", StringArgumentType.word())
                                                    .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                                            List.of("dev", "ray"), builder
                                                    ))
                                                    .then(CommandManager.argument("piece", StringArgumentType.word())
                                                            .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                                                    List.of("helmet", "chestplate", "leggings", "boots"), builder
                                                            ))
                                                            .executes(ctx -> executeGive(
                                                                    ctx.getSource(),
                                                                    EntityArgumentType.getPlayer(ctx, "player"),
                                                                    StringArgumentType.getString(ctx, "set"),
                                                                    StringArgumentType.getString(ctx, "piece")
                                                            ))
                                                    )
                                            )
                                    )
                            )
            );
        });
    }

    private static int executeReload(ServerCommandSource source) {
        // Recharger toutes les configs
        ArmorEffectHandler.ARMOR_SETS.forEach(set -> set.reload());

        // Vider le cache des spawn details
        BaseSpawnInfluence.clearCache();

        LenemonNetwork.sendAllArmorEffects(source.getServer());

        source.sendMessage(
                Text.literal("[LeNeMon] ").formatted(Formatting.GOLD)
                        .append(Text.literal("Configs armures rechargées !").formatted(Formatting.GREEN))
        );

        System.out.println("[LeNeMon] Reload effectué par : " + source.getName());
        return 1;
    }

    private static int executeGive(ServerCommandSource source, ServerPlayerEntity target, String setRaw, String pieceRaw) {
        String set = setRaw.toLowerCase();
        String piece = pieceRaw.toLowerCase();

        Item item = resolveArmorItem(set, piece);
        if (item == null) {
            source.sendError(Text.literal("[LeNeMon] Set ou pièce invalide. Ex: dev helmet"));
            return 0;
        }

        ItemStack stack = new ItemStack(item);



        // Lore serveur basé sur JSON serveur
        List<Text> loreLines = resolveLore(set, piece);
        if (!loreLines.isEmpty()) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
        }


        boolean inserted = target.getInventory().insertStack(stack);
        if (!inserted) {
            target.dropItem(stack, false);
        }

        source.sendFeedback(() -> Text.literal("[LeNeMon] Donné: " + set + " " + piece + " à " + target.getName().getString()), false);
        return 1;
    }

    private static Item resolveArmorItem(String set, String piece) {
        return switch (set) {
            case "dev" -> switch (piece) {
                case "helmet" -> ModItems.DEV_HELMET;
                case "chestplate" -> ModItems.DEV_CHESTPLATE;
                case "leggings" -> ModItems.DEV_LEGGINGS;
                case "boots" -> ModItems.DEV_BOOTS;
                default -> null;
            };
            case "ray" -> switch (piece) {
                case "helmet" -> ModItems.RAY_HELMET;
                case "chestplate" -> ModItems.RAY_CHESTPLATE;
                case "leggings" -> ModItems.RAY_LEGGINGS;
                case "boots" -> ModItems.RAY_BOOTS;
                default -> null;
            };
            default -> null;
        };
    }

    private static List<Text> resolveLore(String set, String piece) {
        return switch (set) {
            case "dev" -> LoreBuilder.build(
                    ArmorEffectHandler.getSet(DevArmorSet.class).getConfig(),
                    piece
            );
            case "ray" -> LoreBuilder.build(
                    ArmorEffectHandler.getSet(RayArmorSet.class).getConfig(),
                    piece
            );
            default -> List.of();
        };
    }

    private static int executeExcaveonReload(ServerCommandSource source) {
        com.lenemon.pickaxe.ExcaveonConfigLoader.reload();

        // AJOUT
        com.lenemon.network.LenemonNetwork.sendAllExcaveonConfig(source.getServer());

        source.sendMessage(
                Text.literal("[LeNeMon] ").formatted(Formatting.GOLD)
                        .append(Text.literal("Config Excavéon rechargée !").formatted(Formatting.GREEN))
        );

        System.out.println("[LeNeMon] Excavéon reload effectué par : " + source.getName());
        return 1;
    }

    private static int executeDexRewardReset(ServerCommandSource source, ServerPlayerEntity target, String region) {
        String playerName = target.getName().getString();
        int removed;
        String label;

        if (region.equalsIgnoreCase("all")) {
            removed = PokedexClaimedStorage.resetAll(target.getUuid());
            label   = "national";
        } else {
            removed = PokedexClaimedStorage.resetRegion(target.getUuid(), region.toLowerCase());
            label   = region.toLowerCase();
        }

        if (removed == 0) {
            source.sendMessage(Text.literal("§e[Pokédex] Aucune récompense à réinitialiser pour §f"
                    + playerName + " §e(région: §f" + label + "§e)."));
        } else {
            source.sendFeedback(() -> Text.literal("[LeNeMon] ").formatted(Formatting.GOLD)
                    .append(Text.literal(removed + " récompense(s) Pokédex réinitialisée(s) pour §f"
                            + playerName + " §a(région: §f" + label + "§a).").formatted(Formatting.GREEN)),
                    false);
        }
        return 1;
    }

    private static int executeNymphalieAddBlocks(ServerCommandSource source, int quantite) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("[LeNeMon] Cette commande doit être exécutée par un joueur."));
            return 0;
        }

        ItemStack held = player.getMainHandStack();
        if (!(held.getItem() instanceof ExcaveonPickaxe)) {
            source.sendError(Text.literal("[LeNeMon] Vous devez tenir une pioche nymphalie en main."));
            return 0;
        }

        int currentBlocks = ExcaveonPickaxe.getBlocks(held);
        int newBlocks = currentBlocks + quantite;
        ExcaveonPickaxe.setBlocks(held, newBlocks);

        int currentLevel = ExcaveonPickaxe.getLevel(held);
        int newLevel = ExcaveonManager.computeLevel(newBlocks);
        if (newLevel > currentLevel) {
            ExcaveonPickaxe.setLevel(held, newLevel);
            ExcaveonManager.notifyLevelUp(player, newLevel);
        }

        source.sendFeedback(() -> Text.literal("[LeNeMon] ").formatted(Formatting.GOLD)
                .append(Text.literal("+" + quantite + " blocs ajoutés à la nymphalie de " + player.getName().getString()
                        + " (total : " + newBlocks + ", niveau : " + ExcaveonPickaxe.getLevel(held) + ")").formatted(Formatting.GREEN)),
                false);
        return 1;
    }
}