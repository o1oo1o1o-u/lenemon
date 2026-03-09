package com.lenemon.playtime;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PlaytimeTracker {

    private static int tickCounter = 0;

    private PlaytimeTracker() {}

    public static void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlaytimeService.syncPlayer(player);
        }
    }

    public static void onDisconnect(ServerPlayerEntity player) {
        PlaytimeService.syncPlayer(player);
    }
}
