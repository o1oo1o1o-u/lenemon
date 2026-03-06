package com.lenemon.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The type Elevator block.
 */
public class ElevatorBlock extends Block {

    // Distance max de recherche d'un autre bloc ascenseur
    private static final int MAX_SEARCH_DISTANCE = 256;

    /**
     * Instantiates a new Elevator block.
     *
     * @param settings the settings
     */
    public ElevatorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, net.minecraft.entity.Entity entity) {
        // La logique est gérée dans ElevatorEventHandler via ServerTickEvents
    }

    /**
     * Cherche le prochain bloc ascenseur EN DESSOUS de `from`.
     * Retourne null si non trouvé.
     *
     * @param world the world
     * @param from  the from
     * @return the block pos
     */
    public static BlockPos findElevatorBelow(World world, BlockPos from) {
        for (int y = from.getY() - 1; y >= Math.max(world.getBottomY(), from.getY() - MAX_SEARCH_DISTANCE); y--) {
            BlockPos check = new BlockPos(from.getX(), y, from.getZ());
            if (world.getBlockState(check).getBlock() instanceof ElevatorBlock) {
                return check;
            }
        }
        return null;
    }

    /**
     * Cherche le prochain bloc ascenseur AU-DESSUS de `from`.
     * Retourne null si non trouvé.
     *
     * @param world the world
     * @param from  the from
     * @return the block pos
     */
    public static BlockPos findElevatorAbove(World world, BlockPos from) {
        for (int y = from.getY() + 1; y <= Math.min(world.getTopY(), from.getY() + MAX_SEARCH_DISTANCE); y++) {
            BlockPos check = new BlockPos(from.getX(), y, from.getZ());
            if (world.getBlockState(check).getBlock() instanceof ElevatorBlock) {
                return check;
            }
        }
        return null;
    }

    /**
     * Vérifie que les 2 blocs AU-DESSUS de `elevatorPos` sont libres (air ou traversables).
     *
     * @param world       the world
     * @param elevatorPos the elevator pos
     * @return the boolean
     */
    public static boolean hasSpaceAbove(World world, BlockPos elevatorPos) {
        BlockPos above1 = elevatorPos.up(1);
        BlockPos above2 = elevatorPos.up(2);
        return isPassable(world, above1) && isPassable(world, above2);
    }

    private static boolean isPassable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || !state.isSolidBlock(world, pos);
    }

    /**
     * Téléporte le joueur au-dessus du bloc ascenseur cible.
     * Le joueur se retrouve au Y+1 du bloc elevator.
     *
     * @param player         the player
     * @param targetElevator the target elevator
     */
    public static void teleportPlayer(ServerPlayerEntity player, BlockPos targetElevator) {
        double destX = targetElevator.getX() + 0.5;
        double destY = targetElevator.getY() + 1.0;
        double destZ = targetElevator.getZ() + 0.5;

        player.setVelocity(0, 0, 0);
        player.velocityModified = true;

        // Utilise requestTeleport qui reset le check anti-cheat côté serveur
        player.networkHandler.requestTeleport(destX, destY, destZ, player.getYaw(), player.getPitch());

        // Force la position serveur immédiatement
        player.updatePosition(destX, destY, destZ);

        player.setVelocity(0, 0, 0);
        player.velocityModified = true;

        player.getWorld().playSound(
                null,
                targetElevator,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.5f, 1.2f
        );
    }
}