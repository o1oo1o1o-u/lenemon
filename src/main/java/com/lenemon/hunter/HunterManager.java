package com.lenemon.hunter;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lenemon.hunter.data.HunterPlayerData;
import com.lenemon.hunter.data.HunterWorldData;
import com.lenemon.hunter.quest.QuestManager;
import com.lenemon.hunter.quest.QuestType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The type Hunter manager.
 */
public class HunterManager {

    private static MinecraftServer server;

    /**
     * Register.
     */
    public static void register() {

        ServerLifecycleEvents.SERVER_STARTED.register(srv -> {
            server = srv;
            HunterWorldData.register(srv);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            HunterWorldData.save();
        });

        // Event capture
        CobblemonEvents.POKEMON_CAPTURED.subscribe(event -> {
            if (!(event.getPlayer() instanceof ServerPlayerEntity player)) return;

            Pokemon pokemon   = event.getPokemon();
            String species    = pokemon.getSpecies().getName();
            String type       = pokemon.getSpecies().getPrimaryType().getName().toLowerCase();
            boolean shiny     = pokemon.getShiny();
            boolean legendary = pokemon.getSpecies().getLabels().contains("legendary");

            QuestManager.onProgress(player, QuestType.CAPTURE, species, type, shiny, legendary);

            // XP de clan : capture normale
            com.lenemon.clan.ClanLevelManager.addXp(
                    player.getUuid(),
                    shiny
                            ? com.lenemon.clan.ClanLevelManager.Source.SHINY_CATCH
                            : com.lenemon.clan.ClanLevelManager.Source.POKEMON_CATCH,
                    player.getServer()
            );
        });

        // Event faint
        CobblemonEvents.BATTLE_FAINTED.subscribe(event -> {
            BattlePokemon killed = event.getKilled();
            Pokemon pokemon = killed.getOriginalPokemon();

            if (pokemon.getOwnerUUID() != null) return;

            String species = pokemon.getSpecies().getName();
            String type    = pokemon.getSpecies().getPrimaryType().getName().toLowerCase();

            event.getBattle().getActors().forEach(actor -> {
                actor.getPlayerUUIDs().forEach(uuid -> {
                    if (server == null) return;
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                    if (player != null) {
                        QuestManager.onProgress(player, QuestType.KILL, species, type, false, false);
                    }
                });
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            HunterPlayerData data = HunterWorldData.get(player.getUuid());

            // Si le joueur n'a pas de quêtes actives, on lui en assigne
            if (data.activeQuests.isEmpty()) {
                QuestManager.assignQuests(player);
                HunterWorldData.save();
            }
        });

        QuestManager.register();
    }
}