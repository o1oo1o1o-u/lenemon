package com.lenemon.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.math.Box;

public class ItemDespawnManager {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;
            for (var world : server.getWorlds()) {
                for (var entity : world.getEntitiesByClass(
                        net.minecraft.entity.ItemEntity.class,
                        new Box(-30000000, -2048, -30000000, 30000000, 2048, 30000000),
                        e -> true)) {
                    if (entity.getItemAge() >= 1200) entity.discard();
                }
            }
        });
    }
}