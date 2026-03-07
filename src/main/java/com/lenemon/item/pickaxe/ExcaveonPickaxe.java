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
    public static final String NBT_BLOCKS       = "excaveon_blocks";
    /**
     * The constant NBT_LEVEL.
     */
    public static final String NBT_LEVEL        = "excaveon_level";
    /**
     * NBT sub-compound that stores the player's chosen configuration.
     */
    public static final String NBT_USER_CFG     = "excaveon_user_cfg";
    public static final String NBT_CFG_AUTO_SELL   = "autoSell";
    public static final String NBT_CFG_AUTO_SMELT  = "autoSmelt";
    public static final String NBT_CFG_MINING_MODE = "miningMode";

    /** Mining mode constants. Each mode encodes both width and depth. */
    public static final String MODE_1X1   = "1x1";
    public static final String MODE_3X3X1 = "3x3x1";
    public static final String MODE_3X3X2 = "3x3x2";
    public static final String MODE_3X3X3 = "3x3x3";
    public static final String MODE_5X5X2 = "5x5x2";  // débloqué niveau 4
    public static final String MODE_5X5X3 = "5x5x3";  // débloqué niveau 5

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

    // === Config joueur (mining mode, auto-sell, auto-smelt) ===

    /**
     * Returns the user config stored on this stack.
     * If absent, returns defaults appropriate for the current level.
     */
    public static ExcaveonUserConfig getUserConfig(ItemStack stack) {
        NbtCompound root = getNbt(stack);
        if (root == null || !root.contains(NBT_USER_CFG)) {
            return ExcaveonUserConfig.defaults(getLevel(stack));
        }
        NbtCompound cfg = root.getCompound(NBT_USER_CFG);
        boolean autoSell  = cfg.getBoolean(NBT_CFG_AUTO_SELL);
        boolean autoSmelt = cfg.getBoolean(NBT_CFG_AUTO_SMELT);
        String  mode      = cfg.getString(NBT_CFG_MINING_MODE);
        if (mode == null || mode.isEmpty()) mode = ExcaveonUserConfig.defaults(getLevel(stack)).miningMode;
        return new ExcaveonUserConfig(autoSell, autoSmelt, mode);
    }

    /**
     * Writes the user config into the stack's NBT.
     */
    public static void setUserConfig(ItemStack stack, ExcaveonUserConfig config) {
        NbtCompound root = getOrCreateNbt(stack);
        NbtCompound cfg  = new NbtCompound();
        cfg.putBoolean(NBT_CFG_AUTO_SELL,   config.autoSell);
        cfg.putBoolean(NBT_CFG_AUTO_SMELT,  config.autoSmelt);
        cfg.putString(NBT_CFG_MINING_MODE,  config.miningMode);
        root.put(NBT_USER_CFG, cfg);
        saveNbt(stack, root);
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