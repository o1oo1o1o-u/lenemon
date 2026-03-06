package com.lenemon.item.pickaxe;

import com.lenemon.pickaxe.ExcaveonConfigLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The type Excaveon pickaxe.
 */
public class ExcaveonPickaxe extends PickaxeItem implements GeoItem {

    /**
     * The constant NBT_BLOCKS.
     */
    public static final String NBT_BLOCKS = "excaveon_blocks";
    /**
     * The constant NBT_LEVEL.
     */
    public static final String NBT_LEVEL  = "excaveon_level";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    /**
     * Instantiates a new Excaveon pickaxe.
     *
     * @param material the material
     * @param settings the settings
     */
    public ExcaveonPickaxe(ToolMaterial material, Item.Settings settings) {
        super(material, settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // === Données NBT ===

    /**
     * Gets blocks.
     *
     * @param stack the stack
     * @return the blocks
     */
    public static int getBlocks(ItemStack stack) {
        NbtCompound nbt = getNbt(stack);
        return nbt != null ? nbt.getInt(NBT_BLOCKS) : 0;
    }

    /**
     * Gets level.
     *
     * @param stack the stack
     * @return the level
     */
    public static int getLevel(ItemStack stack) {
        NbtCompound nbt = getNbt(stack);
        return nbt != null ? Math.max(1, nbt.getInt(NBT_LEVEL)) : 1;
    }

    /**
     * Sets blocks.
     *
     * @param stack  the stack
     * @param blocks the blocks
     */
    public static void setBlocks(ItemStack stack, int blocks) {
        NbtCompound nbt = getOrCreateNbt(stack);
        nbt.putInt(NBT_BLOCKS, blocks);
        saveNbt(stack, nbt);
    }

    /**
     * Sets level.
     *
     * @param stack the stack
     * @param level the level
     */
    public static void setLevel(ItemStack stack, int level) {
        NbtCompound nbt = getOrCreateNbt(stack);
        nbt.putInt(NBT_LEVEL, level);
        saveNbt(stack, nbt);
    }

    private static NbtCompound getNbt(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        return comp != null ? comp.copyNbt() : null;
    }

    private static NbtCompound getOrCreateNbt(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        return comp != null ? comp.copyNbt() : new NbtCompound();
    }

    private static void saveNbt(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    // === Nom dynamique ===

    @Override
    public Text getName(ItemStack stack) {
        ExcaveonLevel level = ExcaveonLevel.fromLevel(getLevel(stack));
        return Text.literal(level.displayName).formatted(Formatting.AQUA, Formatting.BOLD);
    }

    // === Lore dynamique ===

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        int blocks = getBlocks(stack);
        int level = getLevel(stack);
        ExcaveonLevel lvl = ExcaveonLevel.fromLevel(level);

        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("Niveau : " + level).formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Blocs cassés : " + blocks).formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Zone : " + lvl.width + "x" + lvl.height + "x" + lvl.depth).formatted(Formatting.YELLOW));

        if (lvl.autoSell) {
            tooltip.add(Text.literal("✦ Vente auto activée").formatted(Formatting.GREEN));
        }
        if (lvl.sellBonus > 0) {
            tooltip.add(Text.literal("✦ Bonus vente : +" + (int)(lvl.sellBonus * 100) + "%").formatted(Formatting.GOLD));
        }

        // Prochain niveau
        if (level < 5) {
            int nextThreshold = getNextThreshold(level);
            int remaining = nextThreshold - blocks;
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("Prochain niveau dans : " + remaining + " blocs").formatted(Formatting.DARK_GRAY));
        } else {
            tooltip.add(Text.literal("✦ Niveau maximum atteint !").formatted(Formatting.GOLD, Formatting.BOLD));
        }
    }

    private int getNextThreshold(int currentLevel) {
        var config = ExcaveonConfigLoader.getEffective();

        int lvl2 = config.blocksToLevel2;
        int lvl3 = lvl2 + config.blocksToLevel3;
        int lvl4 = lvl3 + config.blocksToLevel4;
        int lvl5 = lvl4 + config.blocksToLevel5;

        return switch (currentLevel) {
            case 1 -> lvl2;
            case 2 -> lvl3;
            case 3 -> lvl4;
            case 4 -> lvl5;
            default -> Integer.MAX_VALUE;
        };
    }

    // === GeoLib ===

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }





    private Object renderProvider = null; // null au lieu de GeoRenderProvider.of(this)

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {}

    @Override
    public Object getRenderProvider() {
        if (renderProvider == null) renderProvider = GeoRenderProvider.of(this);
        return renderProvider;
    }

    /**
     * Sets render provider.
     *
     * @param provider the provider
     */
    public void setRenderProvider(Object provider) { this.renderProvider = provider; }


}