package com.lenemon.gift;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Gift rate session.
 */
public class GiftRateSession {

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static boolean registered = false;

    private static class Session {
        /**
         * The Chest.
         */
        final GiftChestData.ChestEntry chest;
        /**
         * The World data.
         */
        final GiftChestData worldData;
        /**
         * The Pos.
         */
        final BlockPos pos;
        /**
         * The World.
         */
        final ServerWorld world;
        /**
         * The Item.
         */
        final ItemStack item;

        /**
         * Instantiates a new Session.
         *
         * @param chest     the chest
         * @param worldData the world data
         * @param pos       the pos
         * @param world     the world
         * @param item      the item
         */
        Session(GiftChestData.ChestEntry chest, GiftChestData worldData,
                BlockPos pos, ServerWorld world, ItemStack item) {
            this.chest = chest;
            this.worldData = worldData;
            this.pos = pos;
            this.world = world;
            this.item = item;
        }
    }

    /**
     * Register.
     */
    public static void register() {
        if (registered) return;
        registered = true;

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            UUID uuid = sender.getUuid();
            if (!SESSIONS.containsKey(uuid)) return true;

            Session session = SESSIONS.get(uuid);
            String content = message.getContent().getString().trim();

            if (content.equalsIgnoreCase("annuler")) {
                SESSIONS.remove(uuid);
                sender.sendMessage(Text.literal("§c[Cadeau] Annulé."), false);
                GiftChestConfigScreen.open(sender, session.chest, session.worldData,
                        session.pos, session.world);
                return false;
            }

            try {
                double rate = Double.parseDouble(content.replace(",", "."));
                if (rate <= 0 || rate > 100) {
                    sender.sendMessage(Text.literal("§c[Cadeau] Valeur entre 0.01 et 100."), false);
                    return false;
                }

                String displayName = session.item.getName().getString();
                NbtCompound itemNbt = new NbtCompound();
// Sérialise l'item complet avec tous ses composants
                ItemStack.CODEC.encodeStart(
                        net.minecraft.registry.RegistryOps.of(
                                net.minecraft.nbt.NbtOps.INSTANCE,
                                sender.getServerWorld().getRegistryManager()
                        ), session.item
                ).result().ifPresent(nbt -> itemNbt.copyFrom((NbtCompound) nbt));

                GiftReward reward = new GiftReward("item", itemNbt.toString(),
                        session.item.getCount(), rate, displayName);

                List<GiftReward> rewards = GiftChestConfig.load(
                        sender.getServer(), session.chest.chestUUID);
                rewards.add(reward);
                GiftChestConfig.save(sender.getServer(), session.chest.chestUUID, rewards);

                SESSIONS.remove(uuid);
                sender.sendMessage(Text.literal("§a[Cadeau] Récompense ajoutée : §f"
                        + displayName + " §a(§e" + rate + "%§a)"), false);

                GiftChestConfigScreen.open(sender, session.chest, session.worldData,
                        session.pos, session.world);

            } catch (NumberFormatException e) {
                sender.sendMessage(Text.literal("§c[Cadeau] Valeur invalide. Ex: 15.5"), false);
            }

            return false;
        });
    }

    /**
     * Start.
     *
     * @param player    the player
     * @param chest     the chest
     * @param worldData the world data
     * @param pos       the pos
     * @param world     the world
     * @param item      the item
     */
    public static void start(ServerPlayerEntity player, GiftChestData.ChestEntry chest,
                             GiftChestData worldData, BlockPos pos,
                             ServerWorld world, ItemStack item) {
        register();
        SESSIONS.put(player.getUuid(), new Session(chest, worldData, pos, world, item));
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§6§l  🎁 Ajout d'une récompense item"), false);
        player.sendMessage(Text.literal("§7Item : §f" + item.getName().getString()), false);
        player.sendMessage(Text.literal("§7Entrez le §f% de chance §7(0.01 à 100) :"), false);
        player.sendMessage(Text.literal("§7Tapez §cannuler §7pour revenir."), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
    }
}