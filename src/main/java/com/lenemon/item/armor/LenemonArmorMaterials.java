package com.lenemon.item.armor;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The type Lenemon armor materials.
 */
public class LenemonArmorMaterials {

    /**
     * The constant EMPTY.
     */
    public static final RegistryEntry<ArmorMaterial> EMPTY = register("empty", createEmpty());

    private static ArmorMaterial createEmpty() {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 0);
        defense.put(ArmorItem.Type.CHESTPLATE, 0);
        defense.put(ArmorItem.Type.LEGGINGS, 0);
        defense.put(ArmorItem.Type.BOOTS, 0);
        defense.put(ArmorItem.Type.BODY, 0); // présent en 1.21.x

        int enchantability = 0;
        RegistryEntry<SoundEvent> equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
        Supplier<Ingredient> repairIngredient = () -> Ingredient.EMPTY;

        // Important: le nom de layer sert a la texture d’armure.
        // Il doit correspondre a un chemin du style:
        // assets/lenemon/textures/models/armor/empty_layer_1.png (et layer_2 si leggings)
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(Identifier.of("lenemon", "empty")));

        float toughness = 0.0f;
        float knockbackResistance = 0.0f;

        // Signature exacte du record ArmorMaterial:
        // ton IDE va te proposer les params dans l’ordre, accepte l’autocomplete.
        // Selon mapping, il peut y avoir un param "durabilityMultiplier" (souvent avant defense).
        return new ArmorMaterial(defense, enchantability, equipSound, repairIngredient, layers, toughness, knockbackResistance);
    }

    private static RegistryEntry<ArmorMaterial> register(String name, ArmorMaterial material) {
        Identifier id = Identifier.of("lenemon", name);
        Registry.register(Registries.ARMOR_MATERIAL, id, material);

        RegistryKey<ArmorMaterial> key = RegistryKey.of(RegistryKeys.ARMOR_MATERIAL, id);
        return Registries.ARMOR_MATERIAL.getEntry(key).orElseThrow();
    }
}