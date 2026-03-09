package com.lenemon.mixin;

import com.lenemon.muffin.MuffinService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("HEAD"), cancellable = true)
    private void lenemon$cancelMagicMuffinSwing(Hand hand, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && MuffinService.shouldSuppressServerSwing(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true)
    private void lenemon$cancelMagicMuffinSwingForced(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && MuffinService.shouldSuppressServerSwing(player)) {
            ci.cancel();
        }
    }
}
