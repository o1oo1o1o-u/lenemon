package com.lenemon.armor.effects;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

/**
 * The type Glowing effect.
 */
public class GlowingEffect implements ArmorEffect {

    private final Formatting color;

    /**
     * Instantiates a new Glowing effect.
     *
     * @param colorName the color name
     */
    public GlowingEffect(String colorName) {
        this.color = parseColor(colorName);
    }

    @Override
    public void onTick(ServerPlayerEntity player) {
        // Renouveler l'effet glowing toutes les 40 ticks (2s) pour qu'il ne s'arrête pas
        if (player.age % 40 == 0) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.GLOWING,
                    60,    // durée 3s
                    0,     // amplifier
                    false, // ambient
                    false, // showParticles
                    false  // showIcon
            ));

            // Appliquer la couleur d'équipe via scoreboard
            applyTeamColor(player);
        }
    }

    @Override
    public void onRemove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.GLOWING);
    }

    private void applyTeamColor(ServerPlayerEntity player) {
        var scoreboard = player.getServer().getScoreboard();
        String teamName = "lenemon_glow_" + color.getName();

        var team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
            team.setColor(color);
        }

        if (!team.getPlayerList().contains(player.getNameForScoreboard())) {
            scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), team);
        }
    }

    private static Formatting parseColor(String name) {
        return switch (name.toUpperCase()) {
            case "RED" -> Formatting.RED;
            case "BLUE" -> Formatting.BLUE;
            case "GREEN" -> Formatting.GREEN;
            case "YELLOW" -> Formatting.YELLOW;
            case "GOLD" -> Formatting.GOLD;
            case "PURPLE" -> Formatting.DARK_PURPLE;
            case "AQUA" -> Formatting.AQUA;
            case "WHITE" -> Formatting.WHITE;
            default -> Formatting.WHITE;
        };
    }
}