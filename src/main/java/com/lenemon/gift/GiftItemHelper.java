package com.lenemon.gift;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Gift item helper.
 */
public class GiftItemHelper {

    /**
     * Create gift ticket item stack.
     *
     * @param chestUUID the chest uuid
     * @param chestName the chest name
     * @return the item stack
     */
    public static ItemStack createGiftTicket(UUID chestUUID, String chestName) {
        ItemStack item = new ItemStack(Items.PAPER);
        item.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§6§l🎁 Bon Cadeau — §f" + chestName));

        List<Text> lore = List.of(
                Text.literal("§7Coffre : §f" + chestName),
                Text.literal("§7Utilisez ce bon sur le coffre §f" + chestName)
//                Text.literal("§8ID: " + chestUUID)
        );
        item.set(DataComponentTypes.LORE, new LoreComponent(lore));

        NbtCompound nbt = new NbtCompound();
        nbt.putString("giftChestId", chestUUID.toString());
        nbt.putString("giftChestName", chestName);
        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        item.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return item;
    }

    /**
     * Is gift ticket boolean.
     *
     * @param stack the stack
     * @return the boolean
     */
    public static boolean isGiftTicket(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return false;
        return data.copyNbt().contains("giftChestId");
    }

    /**
     * Gets chest uuid.
     *
     * @param stack the stack
     * @return the chest uuid
     */
    public static UUID getChestUUID(ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        String id = data.copyNbt().getString("giftChestId");
        try { return UUID.fromString(id); } catch (Exception e) { return null; }
    }

    /**
     * The constant COLORS.
     */
    public static final Map<String, String> COLORS = java.util.LinkedHashMap.newLinkedHashMap(7);
    static {
        COLORS.put("rouge", "cobblemon:gilded_chest");
        COLORS.put("jaune", "cobblemon:yellow_gilded_chest");
        COLORS.put("vert", "cobblemon:green_gilded_chest");
        COLORS.put("bleu", "cobblemon:blue_gilded_chest");
        COLORS.put("rose", "cobblemon:pink_gilded_chest");
        COLORS.put("noir", "cobblemon:black_gilded_chest");
        COLORS.put("blanc", "cobblemon:white_gilded_chest");
    }

    /**
     * Create gift chest item item stack.
     *
     * @param chestName the chest name
     * @param color     the color
     * @return the item stack
     */
    public static ItemStack createGiftChestItem(String chestName, String color) {
        String blockId = COLORS.getOrDefault(color.toLowerCase(), "cobblemon:yellow_gilded_chest");
        ItemStack item = new ItemStack(
                net.minecraft.registry.Registries.ITEM.get(
                        net.minecraft.util.Identifier.of(blockId)
                )
        );
        item.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§6§l🎁 " + chestName));
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("isGiftChest", true);
        nbt.putString("giftChestName", chestName);
        nbt.putString("giftChestBlock", blockId);
        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        item.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return item;
    }
}