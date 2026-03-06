package com.lenemon.armor.config;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Lore builder.
 */
public class LoreBuilder {

    private static final Map<String, String> EFFECT_NAMES = Map.ofEntries(
            Map.entry("SPEED",               "Vitesse"),
            Map.entry("HASTE",               "Célérité"),
            Map.entry("STRENGTH",            "Force"),
            Map.entry("JUMP_BOOST",          "Saut amélioré"),
            Map.entry("REGENERATION",        "Régénération"),
            Map.entry("RESISTANCE",          "Résistance"),
            Map.entry("FIRE_RESISTANCE",     "Résistance au feu"),
            Map.entry("WATER_BREATHING",     "Respiration aquatique"),
            Map.entry("NIGHT_VISION",        "Vision nocturne"),
            Map.entry("INVISIBILITY",        "Invisibilité"),
            Map.entry("SLOW_FALLING",        "Chute lente"),
            Map.entry("DOLPHINS_GRACE",      "Grâce des dauphins"),
            Map.entry("LUCK",                "Chance"),
            Map.entry("HERO_OF_THE_VILLAGE", "Héros du village")
    );

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};

    /**
     * Build list.
     *
     * @param config the config
     * @param slot   the slot
     * @return the list
     */
    public static List<Text> build(ArmorSetConfig config, String slot) {
        List<Text> lore = new ArrayList<>();

        // Effets de la pièce
        List<PieceEffectConfig> pieceEffects = config.pieceEffects.getOrDefault(slot, List.of());
        if (!pieceEffects.isEmpty()) {
            lore.add(Text.literal("Effets :").formatted(Formatting.GRAY));
            for (PieceEffectConfig effect : pieceEffects) {
                String name = EFFECT_NAMES.getOrDefault(effect.effect, effect.effect);
                String level = effect.level <= ROMAN.length - 1 ? ROMAN[effect.level] : String.valueOf(effect.level);
                lore.add(Text.literal("  ● " + name + " " + level).formatted(Formatting.YELLOW));
            }
        }

        // Bonus de set
        if (!config.setBonusEffects.isEmpty()) {
            lore.add(Text.literal(""));
            lore.add(Text.literal("Bonus set complet :").formatted(Formatting.GOLD));
            for (PieceEffectConfig effect : config.setBonusEffects) {
                String name = EFFECT_NAMES.getOrDefault(effect.effect, effect.effect);
                String level = effect.level <= ROMAN.length - 1 ? ROMAN[effect.level] : String.valueOf(effect.level);
                lore.add(Text.literal("  ● " + name + " " + level).formatted(Formatting.GREEN));
            }
        }



        return lore;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}