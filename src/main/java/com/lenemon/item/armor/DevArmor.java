package com.lenemon.item.armor;

import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import java.util.function.Supplier;
import java.util.List;
import net.minecraft.text.Text;


/**
 * The type Dev armor.
 */
public class DevArmor extends LenemonArmor {
    /**
     * Instantiates a new Dev armor.
     *
     * @param material     the material
     * @param type         the type
     * @param settings     the settings
     * @param nameFormats  the name formats
     * @param loreSupplier the lore supplier
     */
    public DevArmor(RegistryEntry<ArmorMaterial> material, Type type, Item.Settings settings, Formatting[] nameFormats, Supplier<List<Text>> loreSupplier) {
        super(material, type, settings, nameFormats, loreSupplier);
    }
}