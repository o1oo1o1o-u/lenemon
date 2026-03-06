package com.lenemon.armor.effects;

import com.lenemon.armor.config.PieceEffectConfig;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * The type Potion armor effect.
 */
public class PotionArmorEffect implements ArmorEffect {

    private final List<PieceEffectConfig> effectConfigs;

    /**
     * Instantiates a new Potion armor effect.
     *
     * @param effectConfigs the effect configs
     */
    public PotionArmorEffect(List<PieceEffectConfig> effectConfigs) {
        this.effectConfigs = effectConfigs;
    }

    @Override
    public void onTick(ServerPlayerEntity player) {
        if (player.age % 40 != 0) return;
        for (PieceEffectConfig config : effectConfigs) {
            if (config == null || config.effect == null || config.effect.isEmpty()) continue;
            RegistryEntry<StatusEffect> effect = parseEffect(config.effect);
            if (effect == null) continue;
            StatusEffectInstance existing = player.getStatusEffect(effect);

            boolean isNightVision = config.effect.equalsIgnoreCase("NIGHT_VISION");
            int duration = isNightVision ? 999999 : 100; // 100 ticks = 5 secondes
            int renewThreshold = isNightVision ? 800000 : 60; // 60 ticks = 3 secondes

            if (existing == null || existing.getDuration() < renewThreshold) {
                player.addStatusEffect(new StatusEffectInstance(
                        effect, duration, config.level - 1, false, false, false
                ));
            }
        }
    }

    @Override
    public void onRemove(ServerPlayerEntity player) {
        for (PieceEffectConfig config : effectConfigs) {
            if (config == null || config.effect == null || config.effect.isEmpty()) continue; // GUARD
            RegistryEntry<StatusEffect> effect = parseEffect(config.effect);
            if (effect != null) player.removeStatusEffect(effect);
        }
    }

    /**
     * Parse effect registry entry.
     *
     * @param name the name
     * @return the registry entry
     */
    public static RegistryEntry<StatusEffect> parseEffect(String name) {
        return switch (name.toUpperCase()) {
            case "SPEED" -> StatusEffects.SPEED;
            case "HASTE" -> StatusEffects.HASTE;
            case "STRENGTH" -> StatusEffects.STRENGTH;
            case "JUMP_BOOST" -> StatusEffects.JUMP_BOOST;
            case "REGENERATION" -> StatusEffects.REGENERATION;
            case "RESISTANCE" -> StatusEffects.RESISTANCE;
            case "FIRE_RESISTANCE" -> StatusEffects.FIRE_RESISTANCE;
            case "WATER_BREATHING" -> StatusEffects.WATER_BREATHING;
            case "NIGHT_VISION" -> StatusEffects.NIGHT_VISION;
            case "INVISIBILITY" -> StatusEffects.INVISIBILITY;
            case "SLOW_FALLING" -> StatusEffects.SLOW_FALLING;
            case "DOLPHINS_GRACE" -> StatusEffects.DOLPHINS_GRACE;
            case "HERO_OF_THE_VILLAGE" -> StatusEffects.HERO_OF_THE_VILLAGE;
            case "LUCK" -> StatusEffects.LUCK;
            default -> {
                System.err.println("[LeNeMon] Effet inconnu : " + name);
                yield null;
            }
        };
    }
}