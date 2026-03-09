package com.lenemon.hud;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.lenemon.network.PacketHudBattle;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HudBattleTracker {

    private static MinecraftServer server;

    private HudBattleTracker() {}

    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(srv -> server = srv);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(srv -> server = null);

        CobblemonEvents.BATTLE_STARTED_POST.subscribe(event -> {
            if (server == null) return;
            setBattleHudHidden(event.getBattle(), true);
        });

        CobblemonEvents.BATTLE_VICTORY.subscribe(event -> {
            if (server == null) return;
            setBattleHudHidden(event.getBattle(), false);
        });

        CobblemonEvents.BATTLE_FLED.subscribe(event -> {
            if (server == null) return;
            setBattleHudHidden(event.getBattle(), false);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, srv) ->
                ServerPlayNetworking.send(handler.player, new PacketHudBattle(false)));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, srv) -> {
            if (server != null) {
                ServerPlayNetworking.send(handler.player, new PacketHudBattle(false));
            }
        });
    }

    private static void setBattleHudHidden(PokemonBattle battle, boolean hidden) {
        for (BattleActor actor : battle.getActors()) {
            actor.getPlayerUUIDs().forEach(uuid -> {
                if (server == null) return;
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player != null) {
                    ServerPlayNetworking.send(player, new PacketHudBattle(hidden));
                }
            });
        }
    }
}
