package com.lenemon.armor.bonus;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.lenemon.armor.ArmorEffectHandler;
import com.lenemon.armor.ArmorSet;
import com.lenemon.armor.config.ArmorSetConfig;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The type Pokemon xp bonus.
 */
public class PokemonXpBonus {

    /**
     * Register.
     */
    public static void register() {
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(event -> {
            // En Cobblemon 1.7.x l'event s'appelle ExperienceGainedEvent
            // Le joueur se récupère via event.getPokemon().getOwnerPlayer()
            var owner = event.getPokemon().getOwnerPlayer();
            if (owner == null || !(owner instanceof ServerPlayerEntity serverPlayer)) return;

            for (ArmorSet set : ArmorEffectHandler.ARMOR_SETS) {
                if (!set.isEnabled() || !set.isWearing(serverPlayer)) continue;
                ArmorSetConfig config = set.getConfig();
                if (config == null || !config.isPokemonXpEnabled()) continue;

                event.setExperience((int)(event.getExperience() * config.pokemonXpMultiplier));
                break;
            }
        });
    }
}