package com.lenemon.mixin.client;

import com.lenemon.screen.CustomTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The type Title screen mixin.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void replaceWithCustomScreen(CallbackInfo ci) {
        MinecraftClient.getInstance().setScreen(new CustomTitleScreen());
        ci.cancel();
    }

}