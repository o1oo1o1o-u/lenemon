package com.lenemon.casino;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * The type Casino item helper.
 */
public class CasinoItemHelper {

    /**
     * Create casino item item stack.
     *
     * @return the item stack
     */
    public static ItemStack createCasinoItem() {
        ItemStack casinoItem = new ItemStack(
                net.minecraft.registry.Registries.ITEM.get(
                        net.minecraft.util.Identifier.of("cobblemon", "pc")
                )
        );
        casinoItem.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§6§l🎰 Casino"));
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("isCasino", true);
        casinoItem.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        return casinoItem;
    }

    /**
     * Drop casino item.
     *
     * @param world the world
     * @param pos   the pos
     */
    public static void dropCasinoItem(ServerWorld world, BlockPos pos) {
        ItemEntity itemEntity = new ItemEntity(
                world,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                createCasinoItem()
        );
        itemEntity.setPickupDelay(10);
        world.spawnEntity(itemEntity);
    }
}