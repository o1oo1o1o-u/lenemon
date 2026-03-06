package com.lenemon.armor.bonus;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.lenemon.armor.ArmorEffectHandler;
import com.lenemon.armor.ArmorSet;
import com.lenemon.armor.config.ArmorSetConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Random;

/**
 * The type Shiny bonus.
 */
public class ShinyBonus {

    private static final Random RANDOM = new Random();

    /**
     * Register.
     */
    public static void register() {
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(event -> {
            // Trouver le joueur le plus proche
            ServerWorld world = (ServerWorld) event.getEntity().getWorld();
            ServerPlayerEntity nearestPlayer = world.getClosestPlayer(
                    event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                    64, false
            ) instanceof ServerPlayerEntity sp ? sp : null;

            if (nearestPlayer == null) return;

            for (ArmorSet set : ArmorEffectHandler.ARMOR_SETS) {
                if (!set.isEnabled() || !set.isWearing(nearestPlayer)) continue;
                ArmorSetConfig config = set.getConfig();
                if (config == null || !config.isShinyEnabled()) continue;

                // Si pas déjà shiny, appliquer la chance multipliée
                if (!event.getEntity().getPokemon().getShiny()) {
                    float baseShinyRate = 1f / 4096f; // taux vanilla Cobblemon
                    float boostedRate = baseShinyRate * config.shinyMultiplier;
                    if (RANDOM.nextFloat() < boostedRate) {
                        event.getEntity().getPokemon().setShiny(true);
                    }
                }
                break;
            }
        });
    }
}