package com.lenemon.player;

import com.lenemon.hunter.HunterUtils;
import com.lenemon.network.PacketHudBalance;
import com.lenemon.network.PacketHudHunter;
import com.lenemon.util.EconomyHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class PlayerHudTicker {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 60 != 0) return;
            for (var player : server.getPlayerManager().getPlayerList()) {
                long balance   = EconomyHelper.getBalance(player);
                int level      = HunterUtils.getLevel(player.getUuid());
                float progress = HunterUtils.getLevelProgress(player.getUuid());
                ServerPlayNetworking.send(player, new PacketHudBalance(balance));
                ServerPlayNetworking.send(player, new PacketHudHunter(level, progress));
            }
        });
    }
}