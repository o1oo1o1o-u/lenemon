package com.lenemon.item;

import com.lenemon.armor.ArmorEffectHandler;
import com.lenemon.armor.config.LoreBuilder;
import com.lenemon.armor.sets.DevArmorSet;
import com.lenemon.armor.sets.RayArmorSet;
import com.lenemon.item.armor.DevArmor;
import com.lenemon.item.armor.LenemonArmorMaterials;
import com.lenemon.item.armor.RayArmor;

import com.lenemon.item.pickaxe.ExcaveonPickaxe;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.item.ToolMaterials;

/**
 * The type Mod items.
 */
public class ModItems {

    private static final String MODID = "lenemon";

    // Matériau commun (tu peux séparer si un jour ça change)
    private static final RegistryEntry<ArmorMaterial> DEV_MATERIAL = LenemonArmorMaterials.EMPTY;
    private static final RegistryEntry<ArmorMaterial> RAY_MATERIAL = LenemonArmorMaterials.EMPTY;

    private static final Formatting[] DEV_FORMAT = new Formatting[]{Formatting.GOLD, Formatting.BOLD};
    private static final Formatting[] RAY_FORMAT = new Formatting[]{Formatting.GREEN, Formatting.BOLD};

    // Settings commun à toutes tes armures (identique à ce que tu avais)
    private static Item.Settings armorSettings() {
        return new Item.Settings()
                .maxDamage(0)
                ;
    }

    /**
     * The constant DEV_HELMET.
     */
// DEV ARMOR
    public static final DevArmor DEV_HELMET     = register("dev_helmet",     devPiece(ArmorItem.Type.HELMET,     "helmet"));
    /**
     * The constant DEV_CHESTPLATE.
     */
    public static final DevArmor DEV_CHESTPLATE = register("dev_chestplate", devPiece(ArmorItem.Type.CHESTPLATE, "chestplate"));
    /**
     * The constant DEV_LEGGINGS.
     */
    public static final DevArmor DEV_LEGGINGS   = register("dev_leggings",   devPiece(ArmorItem.Type.LEGGINGS,   "leggings"));
    /**
     * The constant DEV_BOOTS.
     */
    public static final DevArmor DEV_BOOTS      = register("dev_boots",      devPiece(ArmorItem.Type.BOOTS,      "boots"));

    private static DevArmor devPiece(ArmorItem.Type type, String loreKey) {
        return new DevArmor(
                DEV_MATERIAL,
                type,
                armorSettings(),
                DEV_FORMAT,
                () -> LoreBuilder.build(
                        ArmorEffectHandler.getSet(DevArmorSet.class).getConfig(),
                        loreKey
                )
        );
    }

    /**
     * The constant RAY_HELMET.
     */
// RAY ARMOR
    public static final RayArmor RAY_HELMET     = register("ray_helmet",     rayPiece(ArmorItem.Type.HELMET,     "helmet"));
    /**
     * The constant RAY_CHESTPLATE.
     */
    public static final RayArmor RAY_CHESTPLATE = register("ray_chestplate", rayPiece(ArmorItem.Type.CHESTPLATE, "chestplate"));
    /**
     * The constant RAY_LEGGINGS.
     */
    public static final RayArmor RAY_LEGGINGS   = register("ray_leggings",   rayPiece(ArmorItem.Type.LEGGINGS,   "leggings"));
    /**
     * The constant RAY_BOOTS.
     */
    public static final RayArmor RAY_BOOTS      = register("ray_boots",      rayPiece(ArmorItem.Type.BOOTS,      "boots"));

    private static RayArmor rayPiece(ArmorItem.Type type, String loreKey) {
        return new RayArmor(
                RAY_MATERIAL,
                type,
                armorSettings(),
                RAY_FORMAT,
                () -> LoreBuilder.build(
                        ArmorEffectHandler.getSet(RayArmorSet.class).getConfig(),
                        loreKey
                )
        );
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, Identifier.of(MODID, name), item);
    }

    /**
     * Register all.
     */
    public static void registerAll() {}

    /**
     * The constant EXCAVEON.
     */
    public static final ExcaveonPickaxe EXCAVEON = register("nymphalie_pickaxe",
            new ExcaveonPickaxe(
                    ToolMaterials.NETHERITE,
                    new Item.Settings().maxDamage(0)
                            .component(DataComponentTypes.UNBREAKABLE, new net.minecraft.component.type.UnbreakableComponent(false))
            ));
}