package com.lenemon.pickaxe;

import com.lenemon.enchantment.AutoSmeltEnchantment;
import com.lenemon.item.ModItems;
import com.lenemon.item.pickaxe.ExcaveonLevel;
import com.lenemon.item.pickaxe.ExcaveonPickaxe;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.HashSet;
import java.util.Set;

/**
 * The type Excaveon manager.
 */
public class ExcaveonManager {

    // Évite les boucles infinies quand on casse les blocs adjacents
    private static final Set<BlockPos> breaking = new HashSet<>();

    /**
     * Register.
     */
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(ExcaveonManager::onBlockBreakBefore);
    }



    private static boolean onBlockBreakBefore(World world, PlayerEntity player,
                                              BlockPos pos, BlockState state, BlockEntity be) {
        if (!(world instanceof ServerWorld serverWorld)) return true;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
        if (breaking.contains(pos)) return true;

        ItemStack held = serverPlayer.getMainHandStack();
        if (!(held.getItem() instanceof ExcaveonPickaxe)) return true;

        ExcaveonLevel lvl = ExcaveonLevel.fromLevel(ExcaveonPickaxe.getLevel(held));

        // compteur blocs (inchangé)
        int blocksToAdd = countBreakableBlocks(serverWorld, serverPlayer, pos, lvl);
        int blocks = ExcaveonPickaxe.getBlocks(held) + blocksToAdd;
        ExcaveonPickaxe.setBlocks(held, blocks);

        int currentLevel = ExcaveonPickaxe.getLevel(held);
        int newLevel = computeLevel(blocks);
        if (newLevel > currentLevel) {
            ExcaveonPickaxe.setLevel(held, newLevel);
            notifyLevelUp(serverPlayer, newLevel);
            lvl = ExcaveonLevel.fromLevel(newLevel);
        }

        // bloc du milieu -> drops -> inventaire
        breaking.add(pos);

        List<ItemStack> drops = Block.getDroppedStacks(state, serverWorld, pos, be, serverPlayer, held);
        drops = drops.stream()
                .map(drop -> AutoSmeltEnchantment.smelt(serverWorld, drop))
                .collect(java.util.stream.Collectors.toList());
        serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

//        for (ItemStack drop : drops) {
//            if (!serverPlayer.getInventory().insertStack(drop)) {
//                serverPlayer.dropItem(drop, false);
//            }
//        }

        if (!lvl.autoSell) {

            for (ItemStack drop : drops) {
                if (!serverPlayer.getInventory().insertStack(drop)) {
                    serverPlayer.dropItem(drop, false);
                }
            }

        } else {

            appendPendingDropsToTool(serverWorld.getRegistryManager(), held, drops);

            java.util.List<ItemStack> pending = decodePending(serverWorld, held);

            var result = com.lenemon.shop.ShopAutoSellService.sell(serverPlayer, pending, (float) lvl.sellBonus);

// rendre invendus
            for (ItemStack rest : result.remaining()) {
                if (rest == null || rest.isEmpty()) continue;
                if (!serverPlayer.getInventory().insertStack(rest)) {
                    serverPlayer.dropItem(rest, false);
                }
            }

// clear pending
            setPendingListOnTool(held, new NbtList());
        }

        breaking.remove(pos);

        // casser zone étendue (ta version modifiée)
        breakExtended(serverWorld, serverPlayer, pos, state, lvl);

        return false; // annule le break vanilla
    }

    private static int countBreakableBlocks(ServerWorld world, ServerPlayerEntity player,
                                            BlockPos center, ExcaveonLevel lvl) {

        int halfW = lvl.width / 2;
        int halfH = lvl.height / 2;
        int count = 1; // bloc principal déjà cassé

        Direction facing = Direction.fromRotation(player.getYaw());

        for (int dx = -halfW; dx <= halfW; dx++) {
            for (int dy = -lvl.bottomOffset; dy < lvl.height - lvl.bottomOffset; dy++) {
                for (int dz = 0; dz < lvl.depth; dz++) {

                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos target = switch (facing) {
                        case NORTH, SOUTH -> center.add(dx, dy,
                                facing == Direction.NORTH ? -dz : dz);
                        case WEST, EAST -> center.add(
                                facing == Direction.WEST ? -dz : dz,
                                dy,
                                dx);
                        default -> center.add(dx, dz, dy);
                    };

                    BlockState targetState = world.getBlockState(target);

                    if (!targetState.isAir() &&
                            targetState.getHardness(world, target) >= 0) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private static void breakExtended(ServerWorld world, ServerPlayerEntity player,
                                      BlockPos center, BlockState originalState,
                                      ExcaveonLevel lvl) {

        int halfW = lvl.width / 2;
        int halfH = lvl.height / 2;
        Direction facing = Direction.fromRotation(player.getYaw());

        // On accumule tout
        java.util.ArrayList<ItemStack> allDrops = new java.util.ArrayList<>();

        for (int dx = -halfW; dx <= halfW; dx++) {
            for (int dy = -lvl.bottomOffset; dy < lvl.height - lvl.bottomOffset; dy++) {
                for (int dz = 0; dz < lvl.depth; dz++) {

                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos target = switch (facing) {
                        case NORTH, SOUTH -> center.add(dx, dy, facing == Direction.NORTH ? -dz : dz);
                        case WEST, EAST -> center.add(facing == Direction.WEST ? -dz : dz, dy, dx);
                        default -> center.add(dx, dz, dy);
                    };

                    BlockState targetState = world.getBlockState(target);
                    if (targetState.isAir()) continue;
                    if (targetState.getHardness(world, target) < 0) continue;

                    if (breaking.contains(target)) continue;
                    breaking.add(target);

                    BlockEntity targetBe = world.getBlockEntity(target);

                    List<ItemStack> drops = Block.getDroppedStacks(
                            targetState, world, target, targetBe, player, player.getMainHandStack()
                    );
                    ItemStack heldTool = player.getMainHandStack();
                    drops = drops.stream()
                            .map(drop -> AutoSmeltEnchantment.smelt((ServerWorld) world, drop))
                            .collect(java.util.stream.Collectors.toList());

                    // Casse sans drop vanilla
                    world.setBlockState(target, Blocks.AIR.getDefaultState(), 3);

                    // Accumule
                    if (drops != null && !drops.isEmpty()) {
                        allDrops.addAll(drops);
                    }

                    breaking.remove(target);
                }
            }
        }

        if (allDrops.isEmpty()) return;

        if (!lvl.autoSell) {
            // Mode normal: inventaire direct
            for (ItemStack drop : allDrops) {
                if (drop == null || drop.isEmpty()) continue;
                if (!player.getInventory().insertStack(drop)) {
                    player.dropItem(drop, false);
                }
            }
            return;
        }

        ItemStack held = player.getMainHandStack();

        appendPendingDropsToTool(world.getRegistryManager(), held, allDrops);

        java.util.List<ItemStack> pending = decodePending(world, held);

        var result = com.lenemon.shop.ShopAutoSellService.sell(player, pending, (float) lvl.sellBonus);

        for (ItemStack rest : result.remaining()) {
            if (rest == null || rest.isEmpty()) continue;
            if (!player.getInventory().insertStack(rest)) {
                player.dropItem(rest, false);
            }
        }

        setPendingListOnTool(held, new NbtList());
    }

    private static int computeLevel(int blocks) {
        var config = ExcaveonConfigLoader.getEffective();

        int lvl2 = config.blocksToLevel2;
        int lvl3 = lvl2 + config.blocksToLevel3;
        int lvl4 = lvl3 + config.blocksToLevel4;
        int lvl5 = lvl4 + config.blocksToLevel5;

        if (blocks >= lvl5) return 5;
        if (blocks >= lvl4) return 4;
        if (blocks >= lvl3) return 3;
        if (blocks >= lvl2) return 2;
        return 1;
    }

    private static void notifyLevelUp(ServerPlayerEntity player, int newLevel) {
        ExcaveonConfig config = ExcaveonConfigLoader.get();
        ExcaveonLevel lvl = ExcaveonLevel.fromLevel(newLevel);
        String msg = config.levelUpMessage
                .replace("{level}", lvl.displayName);
        player.sendMessage(
                com.lenemon.armor.config.ColorParser.parse(msg),
                false
        );
    }

    private static final String NBT_PENDING_SELL = "lenemon_pending_sell";

    private static void appendPendingDropsToTool(
            net.minecraft.registry.RegistryWrapper.WrapperLookup registries,
            ItemStack tool,
            List<ItemStack> drops){
        NbtComponent comp = tool.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound root = comp != null ? comp.copyNbt() : new NbtCompound();

        NbtList list = root.getList(NBT_PENDING_SELL, 10); // 10 = compound
        for (ItemStack st : drops) {
            if (st == null || st.isEmpty()) continue;
            list.add(st.encode(registries));
        }

        root.put(NBT_PENDING_SELL, list);
        tool.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
    }

    private static NbtList getPendingListFromTool(ItemStack tool) {
        NbtComponent comp = tool.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound root = comp != null ? comp.copyNbt() : new NbtCompound();
        return root.getList(NBT_PENDING_SELL, 10);
    }

    private static void setPendingListOnTool(ItemStack tool, NbtList list) {
        NbtComponent comp = tool.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound root = comp != null ? comp.copyNbt() : new NbtCompound();

        if (list == null || list.isEmpty()) root.remove(NBT_PENDING_SELL);
        else root.put(NBT_PENDING_SELL, list);

        tool.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
    }



    private static java.util.List<ItemStack> decodePending(ServerWorld world, ItemStack tool) {
        java.util.ArrayList<ItemStack> out = new java.util.ArrayList<>();
        NbtList list = getPendingListFromTool(tool);

        for (int i = 0; i < list.size(); i++) {
            var el = list.get(i);
            var opt = ItemStack.fromNbt(world.getRegistryManager(), el);
            if (opt.isEmpty()) continue;
            ItemStack st = opt.get();
            if (st.isEmpty()) continue;
            out.add(st);
        }
        return out;
    }

}