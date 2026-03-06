package com.lenemon.heal;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

import java.util.List;

/**
 * The type Heal paper helper.
 */
public class HealPaperHelper {

    /**
     * Create heal paper item stack.
     *
     * @return the item stack
     */
    public static ItemStack createHealPaper() {
        ItemStack item = new ItemStack(Items.PAPER);
        item.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§a§l💊 Parchemin de Soin"));
        item.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7Soigne tous vos Pokémon instantanément."),
                Text.literal("§7Permission permanente accordée."),
                Text.literal(""),
                Text.literal("§eClic droit pour activer !")
        )));

        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("isHealPaper", true);
        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        item.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        item.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return item;
    }

    /**
     * Is heal paper boolean.
     *
     * @param stack the stack
     * @return the boolean
     */
    public static boolean isHealPaper(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return false;
        return data.copyNbt().getBoolean("isHealPaper");
    }
}