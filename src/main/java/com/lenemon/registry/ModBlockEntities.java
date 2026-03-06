package com.lenemon.registry;

import com.lenemon.Lenemon;
import com.lenemon.block.CasinoBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * The type Mod block entities.
 */
public class ModBlockEntities {

    /**
     * The Casino block entity.
     */
    public static BlockEntityType<CasinoBlockEntity> CASINO_BLOCK_ENTITY;

    /**
     * Register.
     */
    public static void register() {
        CASINO_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(Lenemon.MOD_ID, "casino_block_entity"),
                BlockEntityType.Builder.create(CasinoBlockEntity::new, ModBlocks.CASINO_BLOCK).build(null)
        );
        Lenemon.LOGGER.info("BlockEntities enregistrées");
    }
}