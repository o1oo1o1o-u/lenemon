package com.lenemon.armor.sets;

import com.lenemon.armor.config.ArmorSetConfig;
import com.lenemon.armor.config.PieceEffectConfig;
import com.lenemon.item.ModItems;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The type Dev armor set.
 */
public class DevArmorSet extends AbstractArmorSet {

    @Override
    protected String getConfigName() { return "dev_armor"; }

    @Override
    protected ArmorSetConfig buildDefaults() {
        ArmorSetConfig d = new ArmorSetConfig();
        d.enabled = true;
        d.activationMessage = "&e&lSet Développeur activé ! &6Les Pikachu affluent vers toi...";
        d.deactivationMessage = "&7Set Développeur désactivé.";
        d.ignoresBiome = true;
        d.weightMultiplier = 50f;
        d.boostedPokemon = List.of("pikachu");
        d.effects = new ArrayList<>();
        d.pieceEffects = new HashMap<>();
        d.pieceEffects.put("helmet",     List.of(new PieceEffectConfig("NIGHT_VISION", 1)));
        d.pieceEffects.put("chestplate", List.of(new PieceEffectConfig("JUMP_BOOST", 1)));
        d.pieceEffects.put("leggings",   List.of(new PieceEffectConfig("HASTE", 2)));
        d.pieceEffects.put("boots",      List.of(new PieceEffectConfig("SPEED", 2)));
        d.setBonusEffects = List.of(new PieceEffectConfig("SPEED", 3), new PieceEffectConfig("HASTE", 2));
        d.pokemonXpMultiplierEnabled = false;
        d.pokemonXpMultiplier = 1.0f;
        d.shinyMultiplierEnabled = false;
        d.shinyMultiplier = 1.0f;
        d.miningGiftEnabled = false;
        d.miningGiftChance = 0.00005f;
        d.miningGiftCommands = new ArrayList<>(List.of("gift give Basic {player} 1", "gift give Rare {player} 1"));
        return d;
    }

    @Override public Item getHelmetItem()     { return ModItems.DEV_HELMET; }
    @Override public Item getChestplateItem() { return ModItems.DEV_CHESTPLATE; }
    @Override public Item getLeggingsItem()   { return ModItems.DEV_LEGGINGS; }
    @Override public Item getBootsItem()      { return ModItems.DEV_BOOTS; }
}