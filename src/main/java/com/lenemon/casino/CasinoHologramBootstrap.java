package com.lenemon.casino;

import com.lenemon.block.CasinoState;
import com.lenemon.casino.holo.CasinoHolograms;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The type Casino hologram bootstrap.
 */
public final class CasinoHologramBootstrap {

    private CasinoHologramBootstrap() {}

    /**
     * Register.
     */
    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> rebuildForWorld(world));
    }

    private static void rebuildForWorld(ServerWorld world) {
        CasinoWorldData data = CasinoWorldData.get(world);

        for (var entry : data.getCasinosView().entrySet()) {
            BlockPos pos = entry.getKey();
            CasinoWorldData.CasinoData casino = entry.getValue();

            double percent = casino.winChance / 100.0;

            // Si ACTIVE et pokemonDisplayName present, on affiche "ouvert"
            if (casino.state == CasinoState.ACTIVE && casino.pokemonDisplayName != null && !casino.pokemonDisplayName.isBlank()) {
                CasinoHolograms.recreateConfiguredCasinoHologram(
                        world,
                        pos,
                        casino.ownerName,
                        casino.pokemonDisplayName,
                        casino.entryPrice,
                        percent
                );
            } else {
                // UNCONFIGURED ou CONFIGURED (sans pokemon) -> "fermé"
                CasinoHolograms.recreateClosedCasinoHologram(
                        world,
                        pos,
                        casino.ownerName,
                        casino.entryPrice,
                        percent
                );
            }
        }
    }
}