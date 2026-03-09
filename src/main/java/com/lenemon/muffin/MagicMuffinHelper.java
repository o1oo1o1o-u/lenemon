package com.lenemon.muffin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

import java.util.List;

public final class MagicMuffinHelper {

    private static final String DATA_KEY = "magicMuffinType";
    private static final Identifier MUFFIN_ID = Identifier.of("cobblemon", "jubilife_muffin");

    private MagicMuffinHelper() {}

    public static ItemStack create(MagicMuffinType type) {
        Item item = Registries.ITEM.get(MUFFIN_ID);
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(type.displayName()));
        stack.set(DataComponentTypes.LORE, new LoreComponent(buildLore(type)));

        net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
        nbt.putString(DATA_KEY, type.id());
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return stack;
    }

    public static boolean isMagicMuffin(ItemStack stack) {
        return getType(stack) != null;
    }

    public static MagicMuffinType getType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        return MagicMuffinType.fromId(data.copyNbt().getString(DATA_KEY));
    }

    private static List<Text> buildLore(MagicMuffinType type) {
        return switch (type) {
            case NORMAL -> List.of(
                    Text.literal("§7Donne un Pokemon aleatoire."),
                    Text.literal(""),
                    Text.literal("§eClic droit pour l'ouvrir")
            );
            case SHINY -> List.of(
                    Text.literal("§7Donne un Pokemon shiny garanti."),
                    Text.literal(""),
                    Text.literal("§eClic droit pour l'ouvrir")
            );
            case LEGENDARY -> List.of(
                    Text.literal("§7Donne un Pokemon legendaire."),
                    Text.literal(""),
                    Text.literal("§eClic droit pour l'ouvrir")
            );
        };
    }
}
