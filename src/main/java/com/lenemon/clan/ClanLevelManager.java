package com.lenemon.clan;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Gere la progression XP et les montees de niveau des clans.
 *
 * Les points d'entree sont :
 *   - addXp(playerUUID, source, server) : appele par les systemes du jeu
 *
 * Toutes les methodes doivent etre appelees depuis le thread serveur.
 */
public class ClanLevelManager {

    /** Sources d'XP configurables. Lire les valeurs depuis ClanConfig. */
    public enum Source {
        HUNTER_QUEST_COMPLETE,
        POKEMON_CATCH,
        SHINY_CATCH
    }

    private ClanLevelManager() {}

    /**
     * Ajoute de l'XP au clan d'un joueur en fonction de la source.
     * Si le joueur n'est dans aucun clan, ne fait rien.
     * Gere automatiquement la montee de niveau et le broadcast.
     *
     * @param playerUUID UUID du joueur qui a declenche l'action
     * @param source     Source de l'XP
     * @param server     Instance du serveur (pour broadcast)
     */
    public static void addXp(UUID playerUUID, Source source, MinecraftServer server) {
        Clan clan = ClanWorldData.getClanOf(playerUUID);
        if (clan == null) return;

        ClanConfig cfg = ClanConfig.get();
        long xpGained = switch (source) {
            case HUNTER_QUEST_COMPLETE -> cfg.xpPerHunterQuest;
            case POKEMON_CATCH         -> cfg.xpPerPokemonCatch;
            case SHINY_CATCH           -> cfg.xpPerShinyCatch;
        };

        if (xpGained <= 0) return;

        long newXp    = clan.xp + xpGained;
        int  newLevel = clan.level;

        // Calcul des montees de niveau possibles
        while (newLevel < cfg.maxLevel) {
            long xpRequired = cfg.xpRequiredForLevel(newLevel + 1);
            if (newXp >= xpRequired) {
                newLevel++;
                broadcastLevelUp(clan, newLevel, server);
            } else {
                break;
            }
        }

        // Si niveau max atteint, capper l'XP
        if (newLevel >= cfg.maxLevel) {
            newXp = cfg.xpRequiredForLevel(cfg.maxLevel);
        }

        ClanWorldData.setLevelAndXp(clan.id, newLevel, newXp);
    }

    /**
     * Broadcast la montee de niveau a tous les membres du clan en ligne.
     */
    private static void broadcastLevelUp(Clan clan, int newLevel, MinecraftServer server) {
        String msg = "§6§l[Clan] §eLe clan §f" + clan.name + " §eest passe au §6niveau " + newLevel + " §e!";
        for (UUID memberUUID : clan.members.keySet()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(memberUUID);
            if (p != null) {
                p.sendMessage(Text.literal(msg), false);
            }
        }
    }
}
