package com.lenemon.mixin.client;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Expose {@code InGameHud#heldItemTooltipFade} en lecture seule pour permettre
 * au {@code HotbarRenderer} de reproduire le fade-out vanilla du nom d'item tenu.
 *
 * <p>La valeur compte à rebours de 40 à 0 (40 ticks = 2 s) après un changement
 * de slot ou de stack. On la divise par 40f pour obtenir une opacité [0..1].
 */
@Mixin(InGameHud.class)
public interface InGameHudAccessor {

    @Accessor("heldItemTooltipFade")
    int getHeldItemTooltipFade();
}
