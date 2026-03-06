package com.lenemon.armor.effects;

import com.lenemon.armor.config.EffectConfig;
import com.lenemon.armor.config.PieceEffectConfig;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The type Armor effect registry.
 */
public class ArmorEffectRegistry {

    /**
     * Build effects list.
     *
     * @param configs the configs
     * @return the list
     */
    public static List<ArmorEffect> buildEffects(List<EffectConfig> configs) {
        return configs.stream()
                .map(ArmorEffectRegistry::buildEffect)
                .filter(e -> e != null)
                .collect(Collectors.toList());
    }

    /**
     * Build potion effect armor effect.
     *
     * @param configs the configs
     * @return the armor effect
     */
    public static ArmorEffect buildPotionEffect(List<PieceEffectConfig> configs) {
        if (configs == null || configs.isEmpty()) return null;
        List<PieceEffectConfig> valid = configs.stream()
                .filter(c -> c != null && c.effect != null && !c.effect.isEmpty())
                .toList();
        if (valid.isEmpty()) return null;
        return new PotionArmorEffect(valid);
    }

    private static ArmorEffect buildEffect(EffectConfig config) {
        return switch (config.type.toLowerCase()) {
            case "glowing" -> new GlowingEffect(config.color);
            default -> null;
        };
    }
}