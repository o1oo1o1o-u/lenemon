package com.lenemon.player;

import com.lenemon.fly.FlyFeatherHelper;
import com.lenemon.fly.FlyTimerManager;
import com.lenemon.heal.HealPaperHelper;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

public class PlayerItemEvents {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer))
                return TypedActionResult.pass(stack);

            if (FlyFeatherHelper.isFlyFeather(stack)) return handleFlyFeather(serverPlayer, stack);
            if (HealPaperHelper.isHealPaper(stack))   return handleHealPaper(serverPlayer, stack);

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
}