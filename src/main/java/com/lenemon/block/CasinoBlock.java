package com.lenemon.block;


import com.lenemon.casino.CasinoCancelHandler;
import com.lenemon.casino.CasinoConfigSession;
import com.lenemon.screen.CasinoScreenHandler;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;


/**
 * The type Casino block.
 */
public class CasinoBlock extends BlockWithEntity {

    @Override
    public MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(CasinoBlock::new);
    }

    /**
     * Instantiates a new Casino block.
     *
     * @param settings the settings
     */
    public CasinoBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CasinoBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        CasinoBlockEntity entity = (CasinoBlockEntity) world.getBlockEntity(pos);
        if (entity == null) return ActionResult.PASS;

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        boolean isOwner = entity.isOwner(serverPlayer.getUuid());

        if (player.isSneaking() && isOwner) {
            CasinoState cstate = entity.getCasinoState();

            if (cstate == CasinoState.ACTIVE) {
                // Annulation → rend le Pokémon
                CasinoCancelHandler.cancel(serverPlayer, entity, pos);
            } else if (cstate == CasinoState.CONFIGURED) {
                // Prix + % déjà configurés → sélection Pokémon
                // Plus utilisé, le mixin gère tout via PCBlock
            } else {
                // UNCONFIGURED ou autre → config prix + %
                CasinoConfigSession.startConfig(serverPlayer, pos, entity);
            }
        } else {
            CasinoScreenHandler.open(serverPlayer, entity, pos);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.isClient) return super.onBreak(world, pos, state, player);

        CasinoBlockEntity entity = (CasinoBlockEntity) world.getBlockEntity(pos);
        if (entity == null) return super.onBreak(world, pos, state, player);

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        if (!entity.isOwner(serverPlayer.getUuid())) {
            serverPlayer.sendMessage(
                    Text.literal("§cVous n'êtes pas le propriétaire de ce casino !"), false);
            return state;
        }

        if (entity.isLocked()) {
            serverPlayer.sendMessage(
                    Text.literal("§cImpossible — un joueur est en train de jouer !"), false);
            return state;
        }

        if (entity.getCasinoState() == CasinoState.ACTIVE) {
            serverPlayer.sendMessage(
                    Text.literal("§cAnnulez d'abord le casino (shift+clic) pour récupérer votre Pokémon."), false);
            return state;
        }

        return super.onBreak(world, pos, state, player);
    }
}