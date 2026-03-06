package com.lenemon.mixin;

import com.lenemon.gift.*;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * The type Gift chest block mixin.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class GiftChestBlockMixin {

    /**
     * The Player.
     */
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    private void onGiftChestInteract(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        BlockPos pos = packet.getBlockHitResult().getBlockPos();
        var blockState = serverWorld.getBlockState(pos);

        String blockId = net.minecraft.registry.Registries.BLOCK
                .getId(blockState.getBlock()).toString();
        if (!GiftItemHelper.COLORS.containsValue(blockId)) return;

        GiftChestData data = null;
        GiftChestData.ChestEntry chest = null;
        BlockPos chestPos = null;
        ServerWorld foundWorld = null;

        for (ServerWorld w : player.getServer().getWorlds()) {
            GiftChestData d = GiftChestData.get(w);
            if (d.isRegisteredAt(pos)) {
                data = d; chestPos = pos; chest = d.getChest(pos); foundWorld = w; break;
            }
            if (d.isRegisteredAt(pos.down())) {
                data = d; chestPos = pos.down(); chest = d.getChest(pos.down()); foundWorld = w; break;
            }
        }
        if (chestPos == null) return;

        final BlockPos finalChestPos = chestPos;
        final GiftChestData finalData = data;
        final GiftChestData.ChestEntry finalChest = chest;
        final ServerWorld finalFoundWorld = foundWorld;

        player.getServer().execute(() -> {
            boolean isAdmin = Permissions.check(player, "custommenu.gift.admin", 2);

            // Shift+clic admin → GUI config
            if (player.isSneaking() && isAdmin) {
                GiftChestConfigScreen.open(player, finalChest, finalData, finalChestPos, finalFoundWorld);
                return;
            }

            // Clic normal → vérifie le bon cadeau dans la main
            var mainHand = player.getMainHandStack();
            if (!GiftItemHelper.isGiftTicket(mainHand)) {
                player.sendMessage(Text.literal(
                        "§c[Cadeau] Il vous faut un §f🎁 Bon Cadeau — " + finalChest.chestName
                                + "§c pour ouvrir ce coffre !"), false);
                // Knockback
                Vec3d dir = player.getPos().subtract(
                        finalChestPos.getX() + 0.5,
                        finalChestPos.getY() + 0.5,
                        finalChestPos.getZ() + 0.5
                ).normalize().multiply(0.8);
                player.setVelocity(dir.x, 0.3, dir.z);
                player.velocityModified = true;
                return;
            }

            // Vérifie que le bon correspond à ce coffre
            UUID ticketId = GiftItemHelper.getChestUUID(mainHand);

            //debug
           // player.sendMessage(Text.literal("§7[Debug] ticketId=" + ticketId), false);
            //player.sendMessage(Text.literal("§7[Debug] chest.chestUUID=" + finalChest.chestUUID), false);
            //player.sendMessage(Text.literal("§7[Debug] chest.chestName=" + finalChest.chestName), false);
            //debug

            if (ticketId == null || !ticketId.equals(finalChest.chestUUID)) {
                player.sendMessage(Text.literal(
                        "§c[Cadeau] Ce bon n'est pas valide pour ce coffre !"), false);
                return;
            }

            // Ouvre le GUI d'animation
            GiftOpenScreen.open(player, finalChest, finalFoundWorld);
        });

        ci.cancel();
    }
}