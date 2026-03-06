package com.lenemon.armor.sets;

import com.lenemon.armor.config.ArmorSetConfig;
import com.lenemon.armor.config.PieceEffectConfig;
import com.lenemon.item.ModItems;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The type Ray armor set.
 */
public class RayArmorSet extends AbstractArmorSet {

    @Override
    protected String getConfigName() { return "ray_armor"; }

    @Override
    protected ArmorSetConfig buildDefaults() {
        ArmorSetConfig d = new ArmorSetConfig();
        d.enabled = true;
        d.activationMessage = "&e&lSet Rayquaza activé !";
        d.deactivationMessage = "&7Set Rayquaza désactivé.";
        d.ignoresBiome = false;
        d.weightMultiplier = 1f;
        d.boostedPokemon = List.of("rayquaza");
        d.effects = new ArrayList<>();
        d.pieceEffects = new HashMap<>();
        d.pieceEffects.put("helmet",     List.of(new PieceEffectConfig("NIGHT_VISION", 1)));
        d.pieceEffects.put("chestplate", List.of(new PieceEffectConfig("RESISTANCE", 1)));
        d.pieceEffects.put("leggings",   List.of(new PieceEffectConfig("SPEED", 1)));
        d.pieceEffects.put("boots",      List.of(new PieceEffectConfig("SLOW_FALLING", 1)));
        d.setBonusEffects = List.of(new PieceEffectConfig("STRENGTH", 1), new PieceEffectConfig("SPEED", 2));
        d.pokemonXpMultiplierEnabled = false;
        d.pokemonXpMultiplier = 1.0f;
        d.shinyMultiplierEnabled = false;
        d.shinyMultiplier = 1.0f;
        d.miningGiftEnabled = false;
        d.miningGiftChance = 0.00005f;
        d.miningGiftCommands = new ArrayList<>(List.of("gift give Basic {player} 1", "gift give Rare {player} 1"));
        return d;
    }

    @Override public Item getHelmetItem()     { return ModItems.RAY_HELMET; }
    @Override public Item getChestplateItem() { return ModItems.RAY_CHESTPLATE; }
    @Override public Item getLeggingsItem()   { return ModItems.RAY_LEGGINGS; }
    @Override public Item getBootsItem()      { return ModItems.RAY_BOOTS; }
}