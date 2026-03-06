package com.lenemon.mixin;

import com.lenemon.item.armor.LenemonArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * The type Item stack mixin.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void lenemon$removeArmorHeader(CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack)(Object)this;

        if (!(stack.getItem() instanceof LenemonArmor)) return;

        List<Text> tooltip = cir.getReturnValue();

        tooltip.removeIf(line -> {
            TextContent content = line.getContent();
            if (content instanceof TranslatableTextContent t) {
                String key = t.getKey();
                return key.startsWith("item.modifiers.");
            }
            return false;
        });
    }
}