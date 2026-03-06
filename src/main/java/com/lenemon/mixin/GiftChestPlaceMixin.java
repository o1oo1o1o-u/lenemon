package com.lenemon.mixin;

import com.lenemon.gift.GiftChestData;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The type Gift chest place mixin.
 */
@Mixin(targets = "com.cobblemon.mod.common.block.chest.GildedChestBlock")
public class GiftChestPlaceMixin {

    @Inject(method = "onPlaced", at = @At("TAIL"))
    private void onGiftChestPlaced(World world, BlockPos pos, BlockState state,
                                   LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (world.isClient) return;
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(placer instanceof ServerPlayerEntity player)) return;

        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return;
        var nbt = customData.copyNbt();
        if (!nbt.getBoolean("isGiftChest")) return;

        String chestName = nbt.getString("giftChestName");
        String blockId = nbt.getString("giftChestBlock");
        GiftChestData data = GiftChestData.get(serverWorld);
        data.registerChest(pos, player.getUuid(), player.getName().getString(), chestName, blockId);
        player.sendMessage(Text.literal(
                "§a[Cadeau] Coffre §f" + chestName + "§a enregistré ! Shift+clic pour configurer."), false);
    }
}