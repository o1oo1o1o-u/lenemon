package com.lenemon.pickaxe;

import com.lenemon.clan.Clan;
import com.lenemon.clan.ClanWorldData;
import com.lenemon.enchantment.AutoSmeltEnchantment;
import com.lenemon.item.ModItems;
import com.lenemon.item.pickaxe.ExcaveonLevel;
import com.lenemon.item.pickaxe.ExcaveonPickaxe;
import com.lenemon.item.pickaxe.ExcaveonUserConfig;
import me.lucko.fabric.api.permissions.v0.Permissions;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExcaveonManager {

    // Évite les boucles infinies quand on casse les blocs adjacents
    private static final Set<BlockPos> breaking = new HashSet<>();

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

        // Lire la config une seule fois
        ExcaveonUserConfig userCfg = ExcaveonPickaxe.getUserConfig(held);
        ExcaveonLevel lvl = ExcaveonLevel.fromLevel(ExcaveonPickaxe.getLevel(held));

        // ── Protection territoire clan ─────────────────────────────────────────
        if (!canBreakInClan(serverPlayer, serverWorld, pos)) return true; // laisse vanilla gérer (bloqué par son event)

        // ── Casser le bloc central ────────────────────────────────────────────
        breaking.add(pos);
        List<ItemStack> centerDrops = Block.getDroppedStacks(state, serverWorld, pos, be, serverPlayer, held);
        if (userCfg.autoSmelt) {
            centerDrops = centerDrops.stream()
                    .map(drop -> AutoSmeltEnchantment.smelt(serverWorld, drop))
                    .collect(java.util.stream.Collectors.toList());
        }
        serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        breaking.remove(pos);

        // ── Casser la zone étendue (passage unique) ───────────────────────────
        BreakResult extended = breakExtended(serverWorld, serverPlayer, pos, lvl, userCfg);

        // ── Mettre à jour le compteur de blocs (1 centre + zone) ─────────────
        int blocks = ExcaveonPickaxe.getBlocks(held) + 1 + extended.count;
        ExcaveonPickaxe.setBlocks(held, blocks);

        // ── Level up ─────────────────────────────────────────────────────────
        int currentLevel = ExcaveonPickaxe.getLevel(held);
        int newLevel = computeLevel(blocks);
        if (newLevel > currentLevel) {
            ExcaveonPickaxe.setLevel(held, newLevel);
            notifyLevelUp(serverPlayer, newLevel);
            lvl = ExcaveonLevel.fromLevel(newLevel);
            userCfg = ExcaveonPickaxe.getUserConfig(held);
        }

        // ── Distribuer tous les drops en une seule opération ──────────────────
        List<ItemStack> allDrops = new ArrayList<>(centerDrops.size() + extended.drops.size());
        allDrops.addAll(centerDrops);
        allDrops.addAll(extended.drops);

        if (!userCfg.autoSell || !lvl.autoSell) {
            for (ItemStack drop : allDrops) {
                if (drop == null || drop.isEmpty()) continue;
                if (!serverPlayer.getInventory().insertStack(drop)) {
                    serverPlayer.dropItem(drop, false);
                }
            }
        } else {
            var result = com.lenemon.shop.ShopAutoSellService.sell(serverPlayer, allDrops, (float) lvl.sellBonus);
            for (ItemStack rest : result.remaining()) {
                if (rest == null || rest.isEmpty()) continue;
                if (!serverPlayer.getInventory().insertStack(rest)) {
                    serverPlayer.dropItem(rest, false);
                }
            }
        }

        return false; // annule le break vanilla
    }

    /** Résultat du minage étendu : drops collectés + nombre de blocs cassés. */
    private record BreakResult(List<ItemStack> drops, int count) {}

    /**
     * Casse la zone étendue autour du bloc central.
     * Utilise les dimensions définies par le mode choisi (userCfg.zone()),
     * indépendamment des dimensions du niveau.
     * Retourne les drops collectés et le nombre de blocs cassés.
     */
    private static BreakResult breakExtended(ServerWorld world, ServerPlayerEntity player,
                                              BlockPos center, ExcaveonLevel lvl,
                                              ExcaveonUserConfig userCfg) {

        ExcaveonUserConfig.MiningZone zone = userCfg.zone();
        if (!zone.isArea()) return new BreakResult(List.of(), 0);

        Direction facing = Direction.fromRotation(player.getYaw());
        List<ItemStack> allDrops = new ArrayList<>();
        int count = 0;

        for (int dx = zone.xFrom(); dx <= zone.xTo(); dx++) {
            for (int dy = zone.yFrom(); dy <= zone.yTo(); dy++) {
                for (int dz = 0; dz < zone.depth(); dz++) {

                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos target = switch (facing) {
                        case NORTH -> center.add( dx, dy, -dz);
                        case SOUTH -> center.add( dx, dy,  dz);
                        case WEST  -> center.add(-dz, dy,  dx);
                        case EAST  -> center.add( dz, dy,  dx);
                        default    -> center.add( dx, dz,  dy); // UP/DOWN : minage horizontal
                    };

                    BlockState targetState = world.getBlockState(target);
                    if (targetState.isAir()) continue;
                    if (targetState.getHardness(world, target) < 0) continue;
                    if (breaking.contains(target)) continue;
                    if (!canBreakInClan(player, world, target)) continue;

                    breaking.add(target);
                    BlockEntity targetBe = world.getBlockEntity(target);

                    List<ItemStack> drops = Block.getDroppedStacks(
                            targetState, world, target, targetBe, player, player.getMainHandStack());

                    if (userCfg.autoSmelt) {
                        drops = drops.stream()
                                .map(drop -> AutoSmeltEnchantment.smelt(world, drop))
                                .collect(java.util.stream.Collectors.toList());
                    }

                    world.setBlockState(target, Blocks.AIR.getDefaultState(), 3);
                    breaking.remove(target);

                    allDrops.addAll(drops);
                    count++;
                }
            }
        }

        return new BreakResult(allDrops, count);
    }

    public static int computeLevel(int blocks) {
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

    /**
     * Retourne true si le joueur peut casser un bloc a cette position.
     * False si le chunk est claim par un clan dont le joueur n'est pas membre.
     */
    private static boolean canBreakInClan(PlayerEntity player, World world, BlockPos pos) {
        if (!(player instanceof ServerPlayerEntity sp)) return true;
        if (Permissions.check(sp, "lenemon.clan.bypass", 2)) return true;
        Clan owner = ClanWorldData.getChunkOwner(world.getRegistryKey(), new net.minecraft.util.math.ChunkPos(pos));
        if (owner == null) return true;
        return owner.isMember(player.getUuid());
    }

    public static void notifyLevelUp(ServerPlayerEntity player, int newLevel) {
        ExcaveonConfig config = ExcaveonConfigLoader.get();
        ExcaveonLevel lvl = ExcaveonLevel.fromLevel(newLevel);
        String msg = config.levelUpMessage
                .replace("{level}", lvl.displayName);
        player.sendMessage(
                com.lenemon.armor.config.ColorParser.parse(msg),
                false
        );
    }
}
