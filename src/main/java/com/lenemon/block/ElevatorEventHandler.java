package com.lenemon.block;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The type Elevator event handler.
 */
public class ElevatorEventHandler {

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final int COOLDOWN_TICKS = 5; // juste anti double-TP

    private static final Set<UUID> wasOnGround = new HashSet<>();
    private static final Set<UUID> wasSneaking = new HashSet<>();

    // Track joueurs venant de se TP pour supprimer le warning
    private static final Map<UUID, Long> justTeleported = new HashMap<>();

    /**
     * Register.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    handlePlayer(player, world);
                }
            }
        });
    }

    /**
     * Is just teleported boolean.
     *
     * @param uuid the uuid
     * @return the boolean
     */
    public static boolean isJustTeleported(UUID uuid) {
        Long time = justTeleported.get(uuid);
        if (time == null) return false;
        boolean still = System.currentTimeMillis() - time < 1000L;
        if (!still) justTeleported.remove(uuid);
        return still;
    }

    private static void handlePlayer(ServerPlayerEntity player, ServerWorld world) {
        UUID uuid = player.getUuid();

        BlockPos standingOn = player.getBlockPos().down();
        boolean onElevator = world.getBlockState(standingOn).getBlock() instanceof ElevatorBlock;

        long now = world.getTime();
        boolean onCooldown = cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_TICKS;

        if (onElevator && !onCooldown) {
            boolean currentlySneaking = player.isSneaking();
            boolean currentlyOnGround = player.isOnGround();
            boolean prevOnGround = wasOnGround.contains(uuid);
            boolean prevSneaking = wasSneaking.contains(uuid);

            // Sneak : front montant uniquement
            if (currentlySneaking && !prevSneaking) {
                tryGoDown(player, world, standingOn, now, uuid);
            }
            // Saut : décollage détecté
            else if (!currentlySneaking && prevOnGround && !currentlyOnGround) {
                tryGoUp(player, world, standingOn, now, uuid);
            }
        }

        if (player.isOnGround()) wasOnGround.add(uuid);
        else wasOnGround.remove(uuid);

        if (player.isSneaking()) wasSneaking.add(uuid);
        else wasSneaking.remove(uuid);
    }

    private static void tryGoDown(ServerPlayerEntity player, ServerWorld world,
                                  BlockPos currentElevator, long now, UUID uuid) {
        BlockPos target = ElevatorBlock.findElevatorBelow(world, currentElevator);
        if (target == null) return;

        if (!ElevatorBlock.hasSpaceAbove(world, target)) {
            player.sendMessage(Text.literal("§c[Ascenseur] Passage bloqué en dessous !"), true);
            return;
        }

        cooldowns.put(uuid, now);
        justTeleported.put(uuid, System.currentTimeMillis());
        ElevatorBlock.teleportPlayer(player, target);
    }

    private static void tryGoUp(ServerPlayerEntity player, ServerWorld world,
                                BlockPos currentElevator, long now, UUID uuid) {
        BlockPos target = ElevatorBlock.findElevatorAbove(world, currentElevator);
        if (target == null) return;

        if (!ElevatorBlock.hasSpaceAbove(world, target)) {
            player.sendMessage(Text.literal("§c[Ascenseur] Passage bloqué au dessus !"), true);
            return;
        }

        cooldowns.put(uuid, now);
        justTeleported.put(uuid, System.currentTimeMillis());
        ElevatorBlock.teleportPlayer(player, target);
    }
}