package com.lenemon.enchantment;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The type Auto smelt enchantment.
 */
public class AutoSmeltEnchantment {

    /**
     * The constant ID.
     */
    public static final Identifier ID = Identifier.of("lenemon", "auto_smelt");

    /**
     * Register.
     */
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(AutoSmeltEnchantment::onBlockBreak);
    }

    private static boolean onBlockBreak(World world, PlayerEntity player,
                                        BlockPos pos, BlockState state, BlockEntity be) {
        if (!(world instanceof ServerWorld serverWorld)) return true;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;

        ItemStack tool = serverPlayer.getMainHandStack();
        if (tool.getItem() instanceof com.lenemon.item.pickaxe.ExcaveonPickaxe) return true;
        if (!hasAutoSmelt(serverWorld, tool)) return true;

        // Récupérer les drops
        List<ItemStack> drops = Block.getDroppedStacks(state, serverWorld, pos, be, player, tool);

        // Fondre les drops
        List<ItemStack> smelted = drops.stream()
                .map(drop -> smelt(serverWorld, drop))
                .collect(java.util.stream.Collectors.toList());


        // Supprimer le bloc sans drop vanilla
        serverWorld.breakBlock(pos, false, player);

// Dropper les items fondus au sol (comportement vanilla)
        for (ItemStack drop : smelted) {
            if (drop.isEmpty()) continue;
            Block.dropStack(serverWorld, pos, drop);
        }

        return false; // annule le break vanilla + ses drops
    }

    /**
     * Has auto smelt boolean.
     *
     * @param world the world
     * @param tool  the tool
     * @return the boolean
     */
    public static boolean hasAutoSmelt(ServerWorld world, ItemStack tool) {
        if (tool.isEmpty()) return false;
        ItemEnchantmentsComponent enchantments = tool.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return false;

        var registryOpt = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if (registryOpt.isEmpty()) return false;

        var entry = registryOpt.get().getEntry(ID);
        if (entry.isEmpty()) return false;

        return enchantments.getLevel(entry.get()) > 0;
    }

    /**
     * Smelt item stack.
     *
     * @param world the world
     * @param input the input
     * @return the item stack
     */
    public static ItemStack smelt(ServerWorld world, ItemStack input) {
        if (input.isEmpty()) return input;

        SingleStackRecipeInput recipeInput = new SingleStackRecipeInput(input);
        Optional<net.minecraft.recipe.RecipeEntry<SmeltingRecipe>> recipe =
                world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, recipeInput, world);

        if (recipe.isEmpty()) return input;

        ItemStack result = recipe.get().value().craft(recipeInput, world.getRegistryManager());
        result.setCount(input.getCount());
        return result;
    }
}