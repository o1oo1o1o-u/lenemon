package com.lenemon.hunter.quest;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Quest.
 */
public class Quest {
    /**
     * The Id.
     */
    public String id;
    /**
     * The Type.
     */
    public QuestType type;
    /**
     * The Difficulty.
     */
    public QuestDifficulty difficulty;
    /**
     * The Target.
     */
    public String target;        // "*" = tous, "fire" = type, "Pikachu" = espèce
    /**
     * The Amount.
     */
    public int amount;
    /**
     * The Xp reward.
     */
    public long xpReward;
    /**
     * The Money reward.
     */
    public long moneyReward;
    /**
     * The Item rewards.
     */
    public List<String> itemRewards;
    /**
     * The Commands.
     */
    public List<String> commands = new ArrayList<>();
    /**
     * The Commands label.
     */
    public String commandsLabel = "";

    /**
     * Gets description.
     *
     * @return the description
     */
// Généré à l'affichage
    public String getDescription() {
        return switch (type) {
            case KILL           -> "Tuer " + amount + (target.equals("*") ? " Pokémon sauvages" : " Pokémon de type " + target);
            case CAPTURE        -> "Capturer " + amount + (target.equals("*") ? " Pokémon sauvages" : " Pokémon de type " + target);
            case CAPTURE_TYPE   -> "Capturer " + amount + " Pokémon de type " + target;
            case CAPTURE_SHINY  -> "Capturer " + amount + " Pokémon shiny";
            case CAPTURE_LEGENDARY -> "Capturer " + amount + " Pokémon légendaire(s)";
            case KILL_TYPE      -> "Tuer " + amount + " Pokémon de type " + target;
            case KILL_SPECIES   -> "Tuer " + amount + " " + target;
        };
    }
}