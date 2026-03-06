package com.lenemon.fly;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

import java.util.List;

/**
 * The type Fly feather helper.
 */
public class FlyFeatherHelper {

    /**
     * Create feather item stack.
     *
     * @param seconds the seconds
     * @return the item stack
     */
    public static ItemStack createFeather(int seconds) {
        ItemStack item = new ItemStack(Items.FEATHER);

        String name;
        String loreTime;
        if (seconds == -1) {
            name = "§b§l🪶 Plume de Fly §7— §fPermanente";
            loreTime = "§7Durée : §aPermanente";
        } else if (seconds >= 3600) {
            int hours = seconds / 3600;
            name = "§b§l🪶 Plume de Fly §7— §f" + hours + "h";
            loreTime = "§7Durée : §e" + hours + " heure(s)";
        } else {
            int minutes = seconds / 60;
            name = "§b§l🪶 Plume de Fly §7— §f" + minutes + " min";
            loreTime = "§7Durée : §e" + minutes + " minute(s)";
        }

        item.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        item.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal(loreTime),
                Text.literal("§7Le timer démarre quand vous volez."),
                Text.literal(""),
                Text.literal("§eClic droit pour activer le fly !")
        )));

        // Enchantement cosmétique
        NbtCompound enchNbt = new NbtCompound();
        enchNbt.putInt("lvl", 1);
        // On utilise CUSTOM_DATA pour stocker les infos
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("isFlyFeather", true);
        nbt.putInt("flySeconds", seconds);
        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        // Enchantement visuel via StoredEnchantments trick
        item.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        item.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);

        return item;
    }

    /**
     * Is fly feather boolean.
     *
     * @param stack the stack
     * @return the boolean
     */
    public static boolean isFlyFeather(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return false;
        return data.copyNbt().getBoolean("isFlyFeather");
    }

    /**
     * Gets fly seconds.
     *
     * @param stack the stack
     * @return the fly seconds
     */
    public static int getFlySeconds(ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return 0;
        return data.copyNbt().getInt("flySeconds");
    }
}