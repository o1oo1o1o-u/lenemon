package com.lenemon.armor.sets;

import com.lenemon.armor.ArmorSet;
import com.lenemon.armor.config.*;
import com.lenemon.armor.effects.ArmorEffect;
import com.lenemon.armor.effects.ArmorEffectRegistry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Abstract armor set.
 */
public abstract class AbstractArmorSet implements ArmorSet {

    /**
     * The Config.
     */
    protected ArmorSetConfig config;
    /**
     * The Effects.
     */
    protected List<ArmorEffect> effects;
    /**
     * The Helmet effect.
     */
    protected ArmorEffect helmetEffect;
    /**
     * The Chestplate effect.
     */
    protected ArmorEffect chestplateEffect;
    /**
     * The Leggings effect.
     */
    protected ArmorEffect leggingsEffect;
    /**
     * The Boots effect.
     */
    protected ArmorEffect bootsEffect;
    /**
     * The Set bonus effect.
     */
    protected ArmorEffect setBonusEffect;

    /**
     * Instantiates a new Abstract armor set.
     */
    public AbstractArmorSet() {
        reload(); // Ne pas assigner, reload() fait tout en interne
    }


    /**
     * Nom du fichier JSON sans extension  @return  the config name
     *
     * @return the config name
     */
    protected abstract String getConfigName();

    /**
     * Valeurs par défaut du set  @return  the armor set config
     *
     * @return the armor set config
     */
    protected abstract ArmorSetConfig buildDefaults();

    @Override
    public void reload() {
        this.config = ArmorConfigLoader.load(getConfigName(), buildDefaults());
        this.effects          = ArmorEffectRegistry.buildEffects(config.effects);
        this.helmetEffect     = ArmorEffectRegistry.buildPotionEffect(config.pieceEffects.getOrDefault("helmet", List.of()));
        this.chestplateEffect = ArmorEffectRegistry.buildPotionEffect(config.pieceEffects.getOrDefault("chestplate", List.of()));
        this.leggingsEffect   = ArmorEffectRegistry.buildPotionEffect(config.pieceEffects.getOrDefault("leggings", List.of()));
        this.bootsEffect      = ArmorEffectRegistry.buildPotionEffect(config.pieceEffects.getOrDefault("boots", List.of()));
        this.setBonusEffect   = ArmorEffectRegistry.buildPotionEffect(config.setBonusEffects);
    }

    @Override
    public List<ArmorEffect> getPieceEffects(ServerPlayerEntity player) {
        List<ArmorEffect> active = new ArrayList<>();
        if (player.getEquippedStack(EquipmentSlot.HEAD).getItem()  == getHelmetItem()     && helmetEffect     != null) active.add(helmetEffect);
        if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() == getChestplateItem() && chestplateEffect != null) active.add(chestplateEffect);
        if (player.getEquippedStack(EquipmentSlot.LEGS).getItem()  == getLeggingsItem()   && leggingsEffect   != null) active.add(leggingsEffect);
        if (player.getEquippedStack(EquipmentSlot.FEET).getItem()  == getBootsItem()      && bootsEffect      != null) active.add(bootsEffect);
        return active;
    }

    @Override
    public ArmorEffect getSetBonusEffect() { return setBonusEffect; }

    @Override
    public List<ArmorEffect> getEffects() { return effects; }

    @Override
    public boolean isEnabled() { return config.enabled; }

    @Override
    public boolean isWearing(ServerPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).getItem()  == getHelmetItem()
                && player.getEquippedStack(EquipmentSlot.CHEST).getItem() == getChestplateItem()
                && player.getEquippedStack(EquipmentSlot.LEGS).getItem()  == getLeggingsItem()
                && player.getEquippedStack(EquipmentSlot.FEET).getItem()  == getBootsItem();
    }

    @Override
    public Text getActivationMessage() { return ColorParser.parse(config.activationMessage); }

    @Override
    public Text getDeactivationMessage() { return ColorParser.parse(config.deactivationMessage); }

    @Override
    public List<String> getBoostedPokemon() { return config.boostedPokemon; }

    @Override
    public float getWeightMultiplier() { return config.weightMultiplier; }

    @Override
    public boolean ignoresBiome() { return config.ignoresBiome; }

    @Override
    public List<EffectConfig> getEffectConfigs() { return config.effects; }

    @Override
    public ArmorSetConfig getConfig() { return config; }

    public List<ArmorEffect> getAllPieceEffects() {
        List<ArmorEffect> all = new ArrayList<>();
        if (helmetEffect     != null) all.add(helmetEffect);
        if (chestplateEffect != null) all.add(chestplateEffect);
        if (leggingsEffect   != null) all.add(leggingsEffect);
        if (bootsEffect      != null) all.add(bootsEffect);
        return all;
    }


}