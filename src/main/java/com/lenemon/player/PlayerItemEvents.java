package com.lenemon.player;

import com.lenemon.fly.FlyFeatherHelper;
import com.lenemon.fly.FlyTimerManager;
import com.lenemon.heal.HealPaperHelper;
import com.lenemon.muffin.MagicMuffinHelper;
import com.lenemon.muffin.MuffinService;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

public class PlayerItemEvents {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient) {
                if (MagicMuffinHelper.isMagicMuffin(stack)) {
                    playMagicMuffinClientFeedback(player);
                    return TypedActionResult.success(stack);
                }
                return TypedActionResult.pass(stack);
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }

            if (FlyFeatherHelper.isFlyFeather(stack)) return handleFlyFeather(serverPlayer, stack);
            if (HealPaperHelper.isHealPaper(stack))   return handleHealPaper(serverPlayer, stack);
            if (MagicMuffinHelper.isMagicMuffin(stack)) return MuffinService.useMagicMuffin(serverPlayer, stack);

            return TypedActionResult.pass(stack);
        });
    }

    private static TypedActionResult<ItemStack> handleFlyFeather(ServerPlayerEntity player, ItemStack stack) {
        if (FlyTimerManager.hasSession(player.getUuid())) {
            player.sendMessage(Text.literal("§c[Fly] Vous avez déjà une plume active !"), false);
            return TypedActionResult.success(stack);
        }
        int seconds = FlyFeatherHelper.getFlySeconds(stack);
        FlyTimerManager.grantPermission(player);
        FlyTimerManager.register();
        FlyTimerManager.addSession(player, seconds);
        stack.decrement(1);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§b§l  🪶 Plume de Fly activée !"), false);
        player.sendMessage(Text.literal(seconds == -1
                ? "§7Durée : §aPermanente"
                : "§7Durée : §e" + (seconds / 60) + " minute(s)"), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        return TypedActionResult.success(stack);
    }

    private static TypedActionResult<ItemStack> handleHealPaper(ServerPlayerEntity player, ItemStack stack) {
        try {
            var srv = player.getServer();
            if (srv == null) {
                player.sendMessage(Text.literal("§c[Heal] Serveur indisponible."), false);
                return TypedActionResult.success(stack);
            }
            if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("luckperms")) {
                player.sendMessage(Text.literal("§c[Heal] LuckPerms non disponible."), false);
                return TypedActionResult.success(stack);
            }
            srv.getCommandManager().executeWithPrefix(srv.getCommandSource(),
                    "lp user " + player.getName().getString() + " permission set cobblemon.command.healpokemon.self true");
        } catch (Throwable e) {
            player.sendMessage(Text.literal("§c[Heal] Erreur : " + e.getMessage()), false);
            return TypedActionResult.success(stack);
        }
        stack.decrement(1);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§a§l  💊 Parchemin de Soin activé !"), false);
        player.sendMessage(Text.literal("§7Vous pouvez utiliser §f/pokeheal§7."), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        return TypedActionResult.success(stack);
    }

    private static void playMagicMuffinClientFeedback(net.minecraft.entity.player.PlayerEntity player) {
        player.swingHand(player.getActiveHand() != null ? player.getActiveHand() : net.minecraft.util.Hand.MAIN_HAND, true);
        player.getItemCooldownManager().set(player.getMainHandStack().getItem(), 8);
        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.65f, 1.55f);

        for (int i = 0; i < 12; i++) {
            double offsetX = (player.getRandom().nextDouble() - 0.5D) * 0.8D;
            double offsetY = 0.9D + player.getRandom().nextDouble() * 0.5D;
            double offsetZ = (player.getRandom().nextDouble() - 0.5D) * 0.8D;
            player.getWorld().addParticle(
                    ParticleTypes.ENCHANT,
                    player.getX() + offsetX,
                    player.getY() + offsetY,
                    player.getZ() + offsetZ,
                    0.0D,
                    0.02D,
                    0.0D
            );
        }
    }
}
