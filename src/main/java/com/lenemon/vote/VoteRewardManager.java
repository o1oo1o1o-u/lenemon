package com.lenemon.vote;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Vote reward manager.
 */
public class VoteRewardManager {

    private static final int CHECK_INTERVAL_SECONDS = 30;
    private static boolean registered = false;
    private static int tickCounter = 0;

    /**
     * Register.
     */
    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(VoteRewardManager::onTick);
    }

    private static void onTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_SECONDS * 20) return;
        tickCounter = 0;
        processPending(server);
    }

    /**
     * Give reward.
     *
     * @param server the server
     * @param player the player
     */
    public static void giveReward(MinecraftServer server, ServerPlayerEntity player) {
        server.getCommandManager().executeWithPrefix(
                server.getCommandSource(),
                "gift give Basic " + player.getName().getString() + " 2"
        );
        player.sendMessage(Text.literal(""), false);
        //player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        //player.sendMessage(Text.literal("§6§l  🗳️ Merci pour votre vote !"), false);
        player.sendMessage(Text.literal("§6§l  \uD83D\uDDF3\uFE0F Merci pour votre vote ! §aVous avez reçu §f§lx2 Bon Cadeau Basic§a ! §7Votez à nouveau dans §e1h30 §7pour plus de récompenses."), false);
        //player.sendMessage(Text.literal("§7Votez à nouveau dans §e24h §7pour plus de récompenses."), false);
        //player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
    }

    /**
     * Process pending.
     *
     * @param server the server
     */
    public static void processPending(MinecraftServer server) {
        List<String> pending = VoteRewardStorage.load(server);
        //System.out.println("[Vote] Pending chargé : " + pending);
        if (pending.isEmpty()) return;

        List<String> remaining = new ArrayList<>();

        for (String playerName : pending) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayerList()
                    .stream()
                    .filter(p -> p.getName().getString().equalsIgnoreCase(playerName))
                    .findFirst()
                    .orElse(null);

            if (player != null) {
                giveReward(server, player);
                System.out.println("[Vote] Récompense donnée à " + playerName);
            } else {
                remaining.add(playerName);
                System.out.println("[Vote] " + playerName + " hors ligne, gardé en attente.");
            }
        }

        System.out.println("[Vote] Remaining avant save : " + remaining);
        VoteRewardStorage.save(server, remaining);
        System.out.println("[Vote] Save effectué.");
    }
}