package com.lenemon.mixin;

import com.cobblemon.mod.common.block.PCBlock;
import com.lenemon.block.CasinoState;
import com.lenemon.casino.*;
import com.lenemon.casino.holo.CasinoHolograms;
import com.lenemon.screen.CasinoScreenHandler;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * The type Pc block mixin.
 */
@Mixin(PCBlock.class)
public class PCBlockMixin {

    // ── Interaction (clic) ────────────────────────────────

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onUse(BlockState state, World world, BlockPos pos,
                       PlayerEntity player, BlockHitResult hit,
                       CallbackInfoReturnable<ActionResult> cir) {
        // Géré par UseBlockCallback dans Lenemon
    }

    private void handleOwnerInteraction(ServerPlayerEntity player,
                                        CasinoWorldData.CasinoData casino,
                                        CasinoWorldData data, BlockPos pos) {
        BlockPos bottomPos = CasinoHolograms.resolveCasinoPos(data, pos);
        switch (casino.state) {
            case CONFIGURED ->
                    PokemonSelectSession.open(player, casino, data, bottomPos,
                            (ServerWorld) player.getWorld());
            case UNCONFIGURED ->
                    CasinoConfigSession.startConfigFromWorldData(player, bottomPos, casino, data);
            case ACTIVE ->
                    CasinoCancelHandler.cancelFromWorldData(player, casino, data, pos);
            case EMPTY ->
                    player.sendMessage(Text.literal(
                            "§e[Casino] Le Pokémon a été gagné. Reconfigurez (shift+clic)."), false);
        }
    }

    // ── Placement ─────────────────────────────────────────

    @Inject(method = "onPlaced", at = @At("TAIL"))
    private void onPlaced(World world, BlockPos pos, BlockState state,
                          LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (world.isClient) return;
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(placer instanceof ServerPlayerEntity player)) return;

        // 🔒 Bloqué hors Overworld
        if (!serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
            player.sendMessage(Text.literal("§c[Casino] Les casinos ne peuvent être placés que dans l'Overworld."), false);

            // Optionnel: on casse le bloc posé automatiquement
            serverWorld.breakBlock(pos, true, player);

            return;
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return;
        if (!customData.copyNbt().getBoolean("isCasino")) return;

        CasinoWorldData data = CasinoWorldData.get(serverWorld);
        data.registerCasino(pos, player.getUuid(), player.getName().getString());

        CasinoHolograms.removeCasinoHologramAround(pos);
        CasinoHolograms.recreateInitialClosed(serverWorld, pos, player.getName().getString());

        player.sendMessage(Text.literal(
                "§a[Casino] Casino enregistré ! Shift+clic pour configurer."), false);

    }

    // ── Cassage ───────────────────────────────────────────

    @Inject(method = "onBreak", at = @At("HEAD"), cancellable = true)
    private void onBreak(World world, BlockPos pos, BlockState state,
                         PlayerEntity player, CallbackInfoReturnable<BlockState> cir) {
        if (world.isClient) return;
        if (!(world instanceof ServerWorld serverWorld)) return;
        CasinoWorldData data = CasinoWorldData.get(serverWorld);
        if (!data.isCasino(pos)) return;
        cir.cancel();
    }


}