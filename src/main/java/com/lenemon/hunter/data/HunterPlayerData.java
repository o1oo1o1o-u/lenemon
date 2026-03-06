package com.lenemon.hunter.data;

import com.lenemon.hunter.quest.Quest;
import com.lenemon.hunter.quest.QuestDifficulty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The type Hunter player data.
 */
public class HunterPlayerData {

    /**
     * The Player uuid.
     */
    public UUID playerUuid;
    /**
     * The Level.
     */
    public int level = 1;
    /**
     * The Xp.
     */
    public long xp = 0;

    /**
     * The Active quest progress.
     */
// quêtes actives : id quête → progression actuelle
    public Map<String, Integer> activeQuestProgress = new HashMap<>();
    /**
     * The Active quests.
     */
// quêtes actives : id quête → objet Quest
    public Map<String, Quest> activeQuests = new HashMap<>();

    /**
     * Instantiates a new Hunter player data.
     *
     * @param uuid the uuid
     */
    public HunterPlayerData(UUID uuid) {
        this.playerUuid = uuid;
    }

    /**
     * Xp for next level long.
     *
     * @param level the level
     * @return the long
     */
// XP nécessaire pour passer au niveau suivant
    public static long xpForNextLevel(int level) {
        return 100L * level * level;
    }

    /**
     * Add xp boolean.
     *
     * @param amount the amount
     * @return the boolean
     */
// Ajoute de l'XP et gère les montées de niveau
    // Retourne true si level up
    public boolean addXp(long amount) {
        this.xp += amount;
        boolean leveledUp = false;
        while (this.xp >= xpForNextLevel(this.level) && this.level < 200) {
            this.xp -= xpForNextLevel(this.level);
            this.level++;
            leveledUp = true;
        }
        return leveledUp;
    }

    /**
     * Gets progress.
     *
     * @param questId the quest id
     * @return the progress
     */
    public int getProgress(String questId) {
        return activeQuestProgress.getOrDefault(questId, 0);
    }

    /**
     * Increment progress.
     *
     * @param questId the quest id
     */
    public void incrementProgress(String questId) {
        activeQuestProgress.merge(questId, 1, Integer::sum);
    }

    /**
     * Is quest complete boolean.
     *
     * @param questId the quest id
     * @return the boolean
     */
    public boolean isQuestComplete(String questId) {
        Quest q = activeQuests.get(questId);
        if (q == null) return false;
        return getProgress(questId) >= q.amount;
    }
}