package com.lenemon.registry;

import com.lenemon.Lenemon;
import com.lenemon.block.CasinoBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import com.lenemon.block.ElevatorBlock;

/**
 * The type Mod blocks.
 */
public class ModBlocks {

    /**
     * The constant CASINO_BLOCK.
     */
    public static final Block CASINO_BLOCK = Registry.register(
            Registries.BLOCK,
            Identifier.of(Lenemon.MOD_ID, "casino_block"),
            new CasinoBlock(AbstractBlock.Settings.create()
                    .strength(-1.0f, 3600000.0f) // incassable par défaut, on gère manuellement
                    .nonOpaque())
    );

    /**
     * The constant CASINO_BLOCK_ITEM.
     */
    public static final Item CASINO_BLOCK_ITEM = Registry.register(
            Registries.ITEM,
            Identifier.of(Lenemon.MOD_ID, "casino_block"),
            new BlockItem(CASINO_BLOCK, new Item.Settings())
    );

    /**
     * The constant ELEVATOR_BLOCK.
     */
    public static final Block ELEVATOR_BLOCK = Registry.register(
            Registries.BLOCK,
            Identifier.of(Lenemon.MOD_ID, "elevator_block"),
            new ElevatorBlock(AbstractBlock.Settings.create()
                    .strength(0.5f)      // cassage rapide, même vitesse à la main ou avec un outil
                    .sounds(BlockSoundGroup.WOOL))  // son de laine puisque c'est fait avec de la laine
    );

    /**
     * The constant ELEVATOR_BLOCK_ITEM.
     */
    public static final Item ELEVATOR_BLOCK_ITEM = Registry.register(
            Registries.ITEM,
            Identifier.of(Lenemon.MOD_ID, "elevator_block"),
            new BlockItem(ELEVATOR_BLOCK, new Item.Settings())
    );

    /**
     * Register.
     */
    public static void register() {
        Lenemon.LOGGER.info("Blocs enregistrés");
        // Ajout dans le groupe "Blocs de construction" pour le trouver en créatif
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS)
                .register(entries -> entries.add(CASINO_BLOCK_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS)
                .register(entries -> entries.add(ELEVATOR_BLOCK_ITEM));
    }
}