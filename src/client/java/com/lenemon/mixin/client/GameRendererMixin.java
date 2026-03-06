package com.lenemon.mixin.client;

import com.lenemon.screen.CustomTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The type Game renderer mixin.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "renderBlur", at = @At("HEAD"), cancellable = true)
    private void cancelBlur(CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof CustomTitleScreen) {
            ci.cancel();
        }
    }
}