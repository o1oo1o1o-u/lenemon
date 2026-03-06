package com.lenemon.item.armor;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;


import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The type Lenemon armor.
 */
public class LenemonArmor extends ArmorItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Object renderProvider = GeoRenderProvider.of(this);
    private final Formatting[] nameFormats;
    private final Supplier<List<Text>> loreSupplier;

    /**
     * Instantiates a new Lenemon armor.
     *
     * @param material     the material
     * @param type         the type
     * @param settings     the settings
     * @param nameFormats  the name formats
     * @param loreSupplier the lore supplier
     */
    public LenemonArmor(RegistryEntry<ArmorMaterial> material, Type type,
                        Item.Settings settings, Formatting[] nameFormats,
                        Supplier<List<Text>> loreSupplier) {
        super(material, type, settings);
        this.nameFormats = nameFormats;
        this.loreSupplier = loreSupplier;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }



    @Override
    public Text getName(ItemStack stack) {
        MutableText name = Text.translatable(getTranslationKey(stack));
        for (Formatting f : nameFormats) {
            name = name.formatted(f);
        }
        return name;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {}

    @Override
    public Object getRenderProvider() { return this.renderProvider; }

    /**
     * Sets render provider.
     *
     * @param provider the provider
     */
    public void setRenderProvider(Object provider) { this.renderProvider = provider; }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();

        // Applique le lore sur le stack
        if (this.loreSupplier != null) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(this.loreSupplier.get()));
        }

        return stack;
    }

    /**
     * Gets lore lines.
     *
     * @return the lore lines
     */
    public List<Text> getLoreLines() {
        return loreSupplier == null ? List.of() : loreSupplier.get();
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        // Enlève "Sur le torse:", "Aux pieds:", etc
        tooltip.removeIf(line -> {
            TextContent c = line.getContent();
            if (c instanceof TranslatableTextContent t) {
                return t.getKey().startsWith("item.modifiers.");
            }
            return false;
        });
    }
}