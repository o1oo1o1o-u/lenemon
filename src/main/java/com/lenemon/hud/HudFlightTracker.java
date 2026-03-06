package com.lenemon.hud;

import com.cobblemon.mod.common.api.riding.RidingStyle;
import com.cobblemon.mod.common.api.riding.stats.RidingStat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lenemon.network.PacketHudFlight;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HudFlightTracker {

    /**
     * Intervalle d'envoi des mises à jour de stamina (en ticks).
     * 4 ticks = 0.2 s, suffisant pour une barre fluide sans spam réseau.
     */
    private static final int STAMINA_SYNC_INTERVAL = 4;

    private static final Map<UUID, Boolean> flightState = new HashMap<>();
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HudFlightTracker::tick);
    }

    private static void tick(MinecraftServer server) {
        tickCounter++;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PokemonEntity mount = getMountedFlyingPokemon(player);
            boolean isRidingFlying = mount != null;
            boolean wasFlying = flightState.getOrDefault(player.getUuid(), false);

            if (isRidingFlying && !wasFlying) {
                // Montée : on active la barre avec le ratio initial
                flightState.put(player.getUuid(), true);
                ServerPlayNetworking.send(player, buildPacket(true, mount));

            } else if (!isRidingFlying && wasFlying) {
                // Descente : on désactive
                flightState.put(player.getUuid(), false);
                ServerPlayNetworking.send(player, new PacketHudFlight(false, 0f));

            } else if (isRidingFlying && tickCounter % STAMINA_SYNC_INTERVAL == 0) {
                // Mise à jour périodique de la stamina réelle
                ServerPlayNetworking.send(player, buildPacket(true, mount));
            }
        }
    }

    /**
     * Retourne le PokemonEntity monté si le joueur est actuellement sur un Pokémon
     * capable de voler, ou null sinon.
     */
    private static PokemonEntity getMountedFlyingPokemon(ServerPlayerEntity player) {
        if (!(player.getVehicle() instanceof PokemonEntity pokemonEntity)) return null;
        if (pokemonEntity.isRemoved()) return null;
        try {
            boolean canFly = pokemonEntity.getPokemon()
                    .getSpecies()
                    .getBehaviour()
                    .getMoving()
                    .getFly()
                    .getCanFly();
            return canFly ? pokemonEntity : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Construit le packet avec le ratio stamina actuel/max depuis les données Cobblemon.
     *
     * <p>{@code Pokemon.getRideStamina()} retourne la stamina courante.
     * <p>{@code Pokemon.getRideStat(RidingStyle.AIR, RidingStat.STAMINA)} retourne le max calculé
     * d'après les stats du Pokémon (expression MoLang {@code staminaExpr} dans les ride_settings).
     */
    private static PacketHudFlight buildPacket(boolean active, PokemonEntity pokemonEntity) {
        if (!active) return new PacketHudFlight(false, 0f);

        try {
            Pokemon pokemon = pokemonEntity.getPokemon();
            float current = pokemon.getRideStamina();
            float max = pokemon.getRideStat(RidingStyle.AIR, RidingStat.STAMINA);

            float ratio = (max > 0f) ? Math.clamp(current / max, 0f, 1f) : 1f;
            return new PacketHudFlight(true, ratio);
        } catch (Exception ignored) {
            // En cas d'erreur API, on affiche plein (pas de fausse alerte)
            return new PacketHudFlight(true, 1f);
        }
    }
}
