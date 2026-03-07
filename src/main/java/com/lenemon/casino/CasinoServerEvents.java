package com.lenemon.casino;

import com.lenemon.block.CasinoState;
import com.lenemon.casino.network.*;
import com.lenemon.casino.screen.CasinoScreenHandler;
import com.lenemon.util.EconomyHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CasinoServerEvents {

    public static void register() {
        registerNetworkReceivers();
        registerBlockBreak();
    }

    private static void registerNetworkReceivers() {

        ServerPlayNetworking.registerGlobalReceiver(
                CasinoSpinRequestPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (!(player.currentScreenHandler instanceof CasinoScreenHandler handler)) return;
                    if (handler.casinoData == null) return;

                    var casino = handler.casinoData;
                    var data   = handler.worldData;

                    if (casino.locked) return;

                    long price = casino.entryPrice;
                    if (handler.playerBalance < price) {
                        player.sendMessage(Text.literal("§c[Casino] Fonds insuffisants."), false);
                        ServerPlayNetworking.send(player, new CasinoCanSpinResponsePayload(
                                false, price, handler.playerBalance, false));
                        return;
                    }

                    handler.playerBalance -= price;
                    casino.locked = true;
                    data.markDirty();

                    EconomyHelper.debitAsync(player, price);

                    boolean win = player.getRandom().nextInt(10000) + 1 <= casino.winChance;
                    int left = player.getRandom().nextInt(4);
                    int right;
                    if (win) { right = left; }
                    else { do { right = player.getRandom().nextInt(4); } while (right == left); }

                    handler.applySpinResult(win, left, right);
                    ServerPlayNetworking.send(player, new CasinoSpinOutcomePayload(win, left, right));

                    int executeAt = player.getServer().getTicks() + CasinoSpinScheduler.DEFAULT_DELAY_TICKS;
                    CasinoSpinScheduler.schedule(casino.casinoUUID, player.getUuid(), price, win, left, right, executeAt);
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                CasinoCanSpinRequestPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    var player = context.player();
                    if (!(player.currentScreenHandler instanceof CasinoScreenHandler handler)) return;
                    if (handler.casinoData == null) return;

                    long price   = handler.casinoData.entryPrice;
                    long balance = EconomyHelper.getBalance(player);
                    handler.playerBalance = balance;

                    boolean locked     = handler.casinoData.locked;
                    boolean hasPokemon = handler.casinoData.state != CasinoState.EMPTY;
                    boolean allowed    = balance >= price && !locked && hasPokemon;

                    ServerPlayNetworking.send(player, new CasinoCanSpinResponsePayload(allowed, price, balance, locked));

                    if (!handler.casinoData.pokemonSpecies.isEmpty()) {
                        ServerPlayNetworking.send(player, new CasinoPokemonDataPayload(
                                handler.casinoData.pokemonSpecies,
                                handler.casinoData.pokemonAspects,
                                handler.casinoData.winChance,
                                handler.casinoData.pokemonDisplayName,
                                handler.casinoData.pokemonNature,
                                handler.casinoData.pokemonIVs
                        ));
                    }
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                CasinoAnimDonePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (!(player.currentScreenHandler instanceof CasinoScreenHandler handler)) return;
                    if (handler.casinoData == null) return;

                    var casino = handler.casinoData;
                    var data   = handler.worldData;

                    // Ne résoudre que si encore locked (évite double résolution avec le fallback scheduler)
                    if (!casino.locked) return;

                    CasinoSpinHandler.resolveResultFromWorldData(
                            player, casino, data, handler.getPendingWin(), casino.entryPrice);

                    String msg = handler.getPendingWin() ? "Vous avez gagné !" : "Pas de chance...";
                    ServerPlayNetworking.send(player, new CasinoSpinResultPayload(handler.getPendingWin(), msg));
                    CasinoSpinScheduler.removePending(casino.casinoUUID);

                    long balance   = EconomyHelper.getBalance(player);
                    long price     = casino.entryPrice;
                    boolean hasPokemon = casino.state != CasinoState.EMPTY;
                    boolean allowed    = balance >= price && !casino.locked && hasPokemon;

                    ServerPlayNetworking.send(player, new CasinoCanSpinResponsePayload(allowed, price, balance, casino.locked));
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                CasinoOwnerSavePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    var world = player.getServerWorld();
                    var data  = CasinoWorldData.get(world);

                    CasinoWorldData.CasinoData casino = null;
                    BlockPos casinoPos = null;

                    for (var entry : data.getCasinosView().entrySet()) {
                        if (player.getUuid().equals(entry.getValue().ownerUUID)) {
                            casino    = entry.getValue();
                            casinoPos = entry.getKey();
                            break;
                        }
                    }

                    if (casino == null) {
                        player.sendMessage(Text.literal("§c[Casino] Casino introuvable."), false);
                        return;
                    }

                    casino.entryPrice = payload.price();
                    casino.winChance  = (int) Math.round(payload.chance() * 100);

                    if (payload.removePokemon() && payload.selectedPartyIndex() < 0) {
                        CasinoCancelHandler.cancelFromWorldData(player, casino, data, casinoPos);
                    }

                    if (payload.selectedPartyIndex() >= 0) {
                        if (CasinoPokemonStorage.hasPokemon(player.getServer(), casino.casinoUUID)) {
                            CasinoCancelHandler.cancelFromWorldData(player, casino, data, casinoPos);
                        }

                        var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
                        List<com.cobblemon.mod.common.pokemon.Pokemon> partyList = new ArrayList<>();
                        for (var p : party) { if (p != null) partyList.add(p); }

                        if (payload.selectedPartyIndex() < partyList.size()) {
                            var pokemon = partyList.get(payload.selectedPartyIndex());
                            party.remove(pokemon);

                            CasinoPokemonStorage.savePokemon(
                                    player.getServer(), casino.casinoUUID, pokemon,
                                    player.getServerWorld().getRegistryManager());

                            casino.pokemonDisplayName = pokemon.getDisplayName(false).getString()
                                    + " Niv." + pokemon.getLevel() + (pokemon.getShiny() ? " ✦" : "");
                            casino.pokemonSpecies  = pokemon.getSpecies().getName().toLowerCase();
                            casino.pokemonAspects  = new HashSet<>(pokemon.getAspects());
                            casino.pokemonNature   = pokemon.getNature().getName().toString();
                            casino.pokemonIVs      = buildIvs(pokemon);
                            casino.state           = CasinoState.ACTIVE;
                        }
                    }

                    data.markDirty();
                    sendOwnerDataRefresh(player, casino, data);
                    rebuildHologram(world, casino, casinoPos, player);
                    player.sendMessage(Text.literal("§a[Casino] Configuration sauvegardée !"), false);
                })
        );
    }

    private static void registerBlockBreak() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient || !(world instanceof ServerWorld serverWorld)) return true;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;

            var data = CasinoWorldData.get(serverWorld);

            BlockPos bottomPos = null;
            if (data.isRegisteredAt(pos)) bottomPos = pos;
            else if (data.isRegisteredAt(pos.down())) bottomPos = pos.down();
            if (bottomPos == null) return true;

            var casino    = data.getCasino(bottomPos);
            boolean isAdmin = Permissions.check(serverPlayer, "lenemon.casino.admin", 2);
            boolean isOwner = casino.ownerUUID.equals(serverPlayer.getUuid());

            if (!isOwner && !isAdmin) {
                serverPlayer.sendMessage(Text.literal("§c[Casino] Vous n'êtes pas le propriétaire !"), false);
                return false;
            }
            if (casino.state == CasinoState.ACTIVE) {
                serverPlayer.sendMessage(Text.literal("§c[Casino] Un Pokémon est en jeu !"), false);
                return false;
            }
            if (casino.state == CasinoState.LOCKED) {
                serverPlayer.sendMessage(Text.literal("§c[Casino] Un spin est en cours !"), false);
                return false;
            }

            data.removeCasino(bottomPos);
            serverWorld.getChunk(bottomPos).setBlockState(bottomPos,
                    net.minecraft.block.Blocks.AIR.getDefaultState(), false);
            serverWorld.getChunk(bottomPos.up()).setBlockState(bottomPos.up(),
                    net.minecraft.block.Blocks.AIR.getDefaultState(), false);
            serverWorld.updateListeners(bottomPos,
                    serverWorld.getBlockState(bottomPos), net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
            serverWorld.updateListeners(bottomPos.up(),
                    serverWorld.getBlockState(bottomPos.up()), net.minecraft.block.Blocks.AIR.getDefaultState(), 3);

            CasinoItemHelper.dropCasinoItem(serverWorld, bottomPos);
            com.lenemon.casino.holo.CasinoHolograms.removeCasinoHologramAround(bottomPos);
            return false;
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String buildIvs(com.cobblemon.mod.common.pokemon.Pokemon p) {
        var ivs = p.getIvs();
        return ivs.get(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP) + "/" +
                ivs.get(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK) + "/" +
                ivs.get(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE) + "/" +
                ivs.get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK) + "/" +
                ivs.get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE) + "/" +
                ivs.get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED);
    }

    private static void sendOwnerDataRefresh(ServerPlayerEntity player,
                                             CasinoWorldData.CasinoData casino,
                                             CasinoWorldData data) {
        var partyStore = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
        List<CasinoOwnerDataPayload.PartyPokemonData> partyList = new ArrayList<>();
        for (var p : partyStore) {
            if (p == null) continue;
            partyList.add(new CasinoOwnerDataPayload.PartyPokemonData(
                    p.getSpecies().getName().toLowerCase(),
                    new HashSet<>(p.getAspects()),
                    p.getDisplayName(false).getString() + " Niv." + p.getLevel(),
                    p.getNature().getName().toString(),
                    p.getShiny(),
                    buildIvs(p)
            ));
        }
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                new CasinoOwnerDataPayload(
                        casino.entryPrice,
                        casino.winChance / 100.0,
                        casino.pokemonSpecies != null ? casino.pokemonSpecies : "",
                        casino.pokemonAspects != null ? casino.pokemonAspects : new HashSet<>(),
                        casino.pokemonDisplayName != null ? casino.pokemonDisplayName : "",
                        partyList
                ));
    }

    private static void rebuildHologram(ServerWorld world,
                                        CasinoWorldData.CasinoData casino,
                                        BlockPos casinoPos,
                                        ServerPlayerEntity player) {
        String ownerName = casino.ownerName != null ? casino.ownerName : player.getName().getString();
        com.lenemon.casino.holo.CasinoHolograms.removeCasinoHologramAround(casinoPos);
        if (casino.state == CasinoState.ACTIVE) {
            com.lenemon.casino.holo.CasinoHolograms.recreateConfiguredCasinoHologram(
                    world, casinoPos, ownerName, casino.pokemonDisplayName,
                    casino.entryPrice, casino.winChance / 100.0);
        } else {
            com.lenemon.casino.holo.CasinoHolograms.recreateClosedCasinoHologram(
                    world, casinoPos, ownerName, casino.entryPrice, casino.winChance / 100.0);
        }
    }
}