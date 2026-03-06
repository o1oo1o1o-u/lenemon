package com.lenemon.gift;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Gift command session.
 */
public class GiftCommandSession {

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static boolean registered = false;

    private enum Step {
        /**
         * Waiting command step.
         */
        WAITING_COMMAND,
        /**
         * Waiting display name step.
         */
        WAITING_DISPLAY_NAME,
        /**
         * Waiting rate step.
         */
        WAITING_RATE }

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
         * The Step.
         */
        Step step = Step.WAITING_COMMAND;
        /**
         * The Command.
         */
        String command = "";
        /**
         * The Display name.
         */
        String displayName = "";

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
                sender.sendMessage(Text.literal("§c[Cadeau] Annulé."), false);
                GiftChestConfigScreen.open(sender, session.chest, session.worldData,
                        session.pos, session.world);
                return false;
            }

            if (session.step == Step.WAITING_COMMAND) {
                session.command = content.startsWith("/") ? content.substring(1) : content;
                session.step = Step.WAITING_DISPLAY_NAME;
                sender.sendMessage(Text.literal("§7Commande enregistrée : §f/" + session.command), false);
                sender.sendMessage(Text.literal("§7Entrez le §fnom d'affichage §7de la récompense :"), false);
                sender.sendMessage(Text.literal("§7Ex : §f1500 PokéCoins"), false);
                return false;
            }

            if (session.step == Step.WAITING_DISPLAY_NAME) {
                session.displayName = content;
                session.step = Step.WAITING_RATE;
                sender.sendMessage(Text.literal("§7Nom : §f" + session.displayName), false);
                sender.sendMessage(Text.literal("§7Entrez le §f% de chance §7(0.01 à 100) :"), false);
                return false;
            }

            if (session.step == Step.WAITING_RATE) {
                try {
                    double rate = Double.parseDouble(content.replace(",", "."));
                    if (rate <= 0 || rate > 100) {
                        sender.sendMessage(Text.literal("§c[Cadeau] Valeur entre 0.01 et 100."), false);
                        return false;
                    }

                    GiftReward reward = new GiftReward("command", session.command,
                            1, rate, session.displayName);

                    List<GiftReward> rewards = GiftChestConfig.load(
                            sender.getServer(), session.chest.chestUUID);
                    rewards.add(reward);
                    GiftChestConfig.save(sender.getServer(), session.chest.chestUUID, rewards);

                    SESSIONS.remove(uuid);
                    sender.sendMessage(Text.literal("§a[Cadeau] Commande ajoutée : §f"
                            + session.displayName + " §a(§e" + rate + "%§a)"), false);

                    GiftChestConfigScreen.open(sender, session.chest, session.worldData,
                            session.pos, session.world);

                } catch (NumberFormatException e) {
                    sender.sendMessage(Text.literal("§c[Cadeau] Valeur invalide."), false);
                }
                return false;
            }

            return true;
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
        player.sendMessage(Text.literal("§6§l  🎁 Ajout d'une récompense commande"), false);
        player.sendMessage(Text.literal("§7Entrez la commande §7(sans §f/§7) :"), false);
        player.sendMessage(Text.literal("§7Ex : §fbalance %player% add 1500"), false);
        player.sendMessage(Text.literal("§7Tapez §cannuler §7pour revenir."), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
    }
}