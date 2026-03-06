package com.lenemon.armor.bonus;

import com.lenemon.armor.ArmorEffectHandler;
import com.lenemon.armor.ArmorSet;
import com.lenemon.armor.config.ArmorSetConfig;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * The type Set bonus manager.
 */
public class SetBonusManager {

    private static final Random RANDOM = new Random();

    /**
     * Register.
     */
    public static void register() {
        // Event minage
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

            for (ArmorSet set : ArmorEffectHandler.ARMOR_SETS) {
                if (!set.isEnabled() || !set.isWearing(serverPlayer)) continue;
                ArmorSetConfig config = set.getConfig();
                if (config == null || !config.isMiningGiftEnabled()) continue;

                if (RANDOM.nextFloat() < config.miningGiftChance) {
                    if (config.miningGiftCommands.isEmpty()) continue;
                    String cmd = config.miningGiftCommands.get(RANDOM.nextInt(config.miningGiftCommands.size()));
                    String finalCmd = cmd.replace("{player}", serverPlayer.getName().getString());

                    serverWorld.getServer().getCommandManager().executeWithPrefix(
                            serverWorld.getServer().getCommandSource().withSilent(),
                            finalCmd
                    );

                    serverPlayer.sendMessage(
                            net.minecraft.text.Text.literal("✦ Vous avez trouvé un cadeau en minant !")
                                    .formatted(net.minecraft.util.Formatting.GOLD),
                            false
                    );
                }
            }
        });
    }

    private static void onBlockBreak(ServerWorld world, net.minecraft.entity.player.PlayerEntity player,
                                     BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        for (ArmorSet set : ArmorEffectHandler.ARMOR_SETS) {
            if (!set.isEnabled() || !set.isWearing(serverPlayer)) continue;
            ArmorSetConfig config = set.getConfig();
            if (config == null || !config.isMiningGiftEnabled()) continue;

            if (RANDOM.nextFloat() < config.miningGiftChance) {
                // Choisir une commande au hasard dans la liste
                if (config.miningGiftCommands.isEmpty()) continue;
                String cmd = config.miningGiftCommands.get(RANDOM.nextInt(config.miningGiftCommands.size()));
                String finalCmd = cmd.replace("{player}", serverPlayer.getName().getString());

                world.getServer().getCommandManager().executeWithPrefix(
                        world.getServer().getCommandSource().withSilent(),
                        finalCmd
                );

                serverPlayer.sendMessage(
                        net.minecraft.text.Text.literal("✦ Vous avez trouvé un cadeau en minant !")
                                .formatted(net.minecraft.util.Formatting.GOLD),
                        false
                );
            }
        }
    }
}