package com.lenemon.clan;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

/**
 * Protection du territoire des clans.
 * Empeche les non-membres de casser des blocs, de placer des blocs
 * et d'interagir avec des conteneurs dans les chunks claims.
 *
 * Bypass : permission lenemon.clan.bypass (niveau 2 = op).
 */
public class ClanTerritoryProtection {

    private ClanTerritoryProtection() {}

    /**
     * Enregistre les events Fabric de protection.
     * A appeler dans Lenemon.onInitialize().
     */
    public static void register() {

        // Block break
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (world.isClient()) return true;
            return canInteract(player, pos, world);
        });

        // Block place / Container interaction (UseBlockCallback gere les deux cas)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (Permissions.check(sp, "lenemon.clan.bypass", 2)) return ActionResult.PASS;

            RegistryKey<World> dim = world.getRegistryKey();
            BlockPos targetPos = hitResult.getBlockPos();
            BlockPos placePos  = targetPos.offset(hitResult.getSide());

            // Verifier le bloc cible (conteneur / interaction directe)
            Clan targetOwner = ClanWorldData.getChunkOwner(dim, new ChunkPos(targetPos));
            if (targetOwner != null && !targetOwner.isMember(player.getUuid())) {
                sp.sendMessage(Text.literal("§c[Clan] §rCe territoire appartient au clan §e"
                        + targetOwner.name + "§r."), true);
                return ActionResult.FAIL;
            }

            // Verifier la position de placement
            Clan placeOwner = ClanWorldData.getChunkOwner(dim, new ChunkPos(placePos));
            if (placeOwner != null && !placeOwner.isMember(player.getUuid())) {
                sp.sendMessage(Text.literal("§c[Clan] §rCe territoire appartient au clan §e"
                        + placeOwner.name + "§r."), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        // Fluid buckets (lava, water, powder snow) — UseBlockCallback ne les capte pas
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!(player instanceof ServerPlayerEntity sp)) return TypedActionResult.pass(player.getStackInHand(hand));

            var stack = sp.getStackInHand(hand);
            boolean isFluidBucket = stack.getItem() == Items.LAVA_BUCKET
                    || stack.getItem() == Items.WATER_BUCKET
                    || stack.getItem() == Items.POWDER_SNOW_BUCKET
                    || (stack.getItem() instanceof BucketItem bi
                        && bi != Items.BUCKET && bi != Items.MILK_BUCKET);

            if (!isFluidBucket) return TypedActionResult.pass(stack);
            if (Permissions.check(sp, "lenemon.clan.bypass", 2)) return TypedActionResult.pass(stack);

            HitResult hit = sp.raycast(sp.getBlockInteractionRange(), 0f, false);
            if (hit instanceof BlockHitResult blockHit) {
                BlockPos placePos = blockHit.getBlockPos().offset(blockHit.getSide());
                RegistryKey<World> dim = world.getRegistryKey();
                Clan placeOwner = ClanWorldData.getChunkOwner(dim, new ChunkPos(placePos));
                if (placeOwner != null && !placeOwner.isMember(player.getUuid())) {
                    sp.sendMessage(Text.literal("§c[Clan] §rCe territoire appartient au clan §e"
                            + placeOwner.name + "§r."), true);
                    return TypedActionResult.fail(stack);
                }
            }
            return TypedActionResult.pass(stack);
        });
    }

    // -------------------------------------------------------------------------
    // Helper prive
    // -------------------------------------------------------------------------

    private static boolean canInteract(PlayerEntity player, BlockPos pos, World world) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
        if (Permissions.check(serverPlayer, "lenemon.clan.bypass", 2)) return true;

        RegistryKey<World> dim = world.getRegistryKey();
        ChunkPos chunkPos = new ChunkPos(pos);
        Clan owner = ClanWorldData.getChunkOwner(dim, chunkPos);

        if (owner == null) return true; // chunk non claim -> libre

        if (!owner.isMember(player.getUuid())) {
            serverPlayer.sendMessage(Text.literal("§c[Clan] §rCe territoire appartient au clan §e"
                    + owner.name + "§r."), true);
            return false;
        }

        return true;
    }
}
