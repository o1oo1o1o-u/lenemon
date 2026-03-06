package com.lenemon.gift;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The type Gift delete session.
 */
public class GiftDeleteSession {

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
         * Instantiates a new Session.
         *
         * @param chest     the chest
         * @param worldData the world data
         * @param pos       the pos
         * @param world     the world
         */
        Session(GiftChestData.ChestEntry chest, GiftChestData worldData,
                BlockPos pos, ServerWorld world) {
            this.chest = chest;
            this.worldData = worldData;
            this.pos = pos;
            this.world = world;
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
                sender.sendMessage(Text.literal("§e[Cadeau] Suppression annulée."), false);
                GiftChestConfigScreen.open(sender, session.chest, session.worldData,
                        session.pos, session.world);
                return false;
            }

            if (content.equalsIgnoreCase("confirmer")) {
                // Supprime les données
                session.worldData.removeChest(session.pos);
                GiftChestConfig.delete(sender.getServer(), session.chest.chestUUID);

                // Supprime les blocs
                session.world.getChunk(session.pos)
                        .setBlockState(session.pos, Blocks.AIR.getDefaultState(), false);
                session.world.getChunk(session.pos.up())
                        .setBlockState(session.pos.up(), Blocks.AIR.getDefaultState(), false);
                session.world.updateListeners(session.pos,
                        session.world.getBlockState(session.pos), Blocks.AIR.getDefaultState(), 3);
                session.world.updateListeners(session.pos.up(),
                        session.world.getBlockState(session.pos.up()), Blocks.AIR.getDefaultState(), 3);

                SESSIONS.remove(uuid);
                sender.sendMessage(Text.literal(
                        "§a[Cadeau] Coffre §f" + session.chest.chestName + "§a supprimé."), false);
                return false;
            }

            sender.sendMessage(Text.literal("§c[Cadeau] Tapez §fconfirmer §cou §fannuler§c."), false);
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
     */
    public static void start(ServerPlayerEntity player, GiftChestData.ChestEntry chest,
                             GiftChestData worldData, BlockPos pos, ServerWorld world) {
        register();
        SESSIONS.put(player.getUuid(), new Session(chest, worldData, pos, world));
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§c§l  ⚠ Supprimer le coffre " + chest.chestName), false);
        player.sendMessage(Text.literal("§7Cette action est §cirrréversible§7."), false);
        player.sendMessage(Text.literal("§7Tapez §cconfirmer §7pour supprimer."), false);
        player.sendMessage(Text.literal("§7Tapez §cannuler §7pour revenir."), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
    }
}