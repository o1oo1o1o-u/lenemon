package com.lenemon.mixin;

import com.lenemon.block.CasinoState;
import com.lenemon.casino.CasinoCancelHandler;
import com.lenemon.casino.CasinoConfigSession;
import com.lenemon.casino.CasinoSpinHandler;
import com.lenemon.casino.CasinoWorldData;
import com.lenemon.casino.PokemonSelectSession;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The type Player interact block mixin.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class PlayerInteractBlockMixin {

    /**
     * The Player.
     */
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    private void onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        BlockPos clickedPos = packet.getBlockHitResult().getBlockPos();

        // Vérifie que le bloc cliqué est bien un PC Cobblemon
        var blockState = serverWorld.getBlockState(clickedPos);
        if (!(blockState.getBlock() instanceof com.cobblemon.mod.common.block.PCBlock)) return;

        CasinoWorldData data = CasinoWorldData.get(serverWorld);

        // Résout la position "casino" (PC est sur 2 blocs: bas/haut)
        BlockPos casinoPos = null;
        if (data.isRegisteredAt(clickedPos)) casinoPos = clickedPos;
        else if (data.isRegisteredAt(clickedPos.down())) casinoPos = clickedPos.down();
        else if (data.isRegisteredAt(clickedPos.up())) casinoPos = clickedPos.up();

        // Si ce n'est pas un casino enregistré, laisse le PC normal faire sa vie
        if (casinoPos == null) return;

        final BlockPos finalCasinoPos = casinoPos;
        final CasinoWorldData.CasinoData casino = data.getCasino(finalCasinoPos);

        player.getServer().execute(() -> {

            // Owner → ouvre toujours la config
            if (casino.ownerUUID != null && casino.ownerUUID.equals(player.getUuid())) {

                // Ferme les joueurs sur ce casino + annule spin en cours
                for (ServerPlayerEntity onlinePlayer : player.getServer().getPlayerManager().getPlayerList()) {
                    if (onlinePlayer == player) continue;
                    if (onlinePlayer.currentScreenHandler instanceof com.lenemon.casino.screen.CasinoScreenHandler h
                            && h.casinoData == casino) {
                        onlinePlayer.closeHandledScreen();
                        onlinePlayer.sendMessage(Text.literal("§e[Casino] Le propriétaire a modifié le casino."), false);
                    }
                }

                if (casino.locked) {
                    casino.locked = false;
                    com.lenemon.casino.CasinoSpinScheduler.removePending(casino.casinoUUID);
                    data.markDirty();
                }

                var partyStore = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
                java.util.List<com.lenemon.casino.network.CasinoOwnerDataPayload.PartyPokemonData> partyList = new java.util.ArrayList<>();
                for (var p : partyStore) {
                    if (p == null) continue;
                    partyList.add(new com.lenemon.casino.network.CasinoOwnerDataPayload.PartyPokemonData(
                            p.getSpecies().getName().toLowerCase(),
                            new java.util.HashSet<>(p.getAspects()),
                            p.getDisplayName(false).getString() + " Niv." + p.getLevel(),
                            p.getNature().getName().toString(),
                            p.getShiny(),
                            p.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP) + "/" +
                                    p.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK) + "/" +
                                    p.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE) + "/" +
                                    p.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK) + "/" +
                                    p.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE) + "/" +
                                    p.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED)
                    ));
                }

                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                        new com.lenemon.casino.network.CasinoOwnerDataPayload(
                                casino.entryPrice,
                                casino.winChance / 100.0,
                                casino.pokemonSpecies != null ? casino.pokemonSpecies : "",
                                casino.pokemonAspects != null ? casino.pokemonAspects : new java.util.HashSet<>(),
                                casino.pokemonDisplayName != null ? casino.pokemonDisplayName : "",
                                partyList
                        )
                );
                return;
            }

            // Joueur non-owner → ouvre le GUI casino
            boolean hasPokemon = casino.pokemonDisplayName != null && !casino.pokemonDisplayName.isBlank();
            if (casino.state != CasinoState.ACTIVE || !hasPokemon) {
                player.sendMessage(Text.literal("§c[Casino] Ce casino n'est pas disponible."), false);
                return;
            }

            if (casino.locked) {
                player.sendMessage(Text.literal("§c[Casino] Déjà en cours, attendez !"), false);
                return;
            }

            com.lenemon.casino.screen.CasinoScreenOpener.open(player, casino, data, finalCasinoPos);
        });

        // On annule l'interaction vanilla du PC, car c'est un casino
        ci.cancel();
    }
}