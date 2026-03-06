package com.lenemon.economy;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The type Impactor economy service.
 */
public class ImpactorEconomyService implements EconomyService {

    @Override
    public long getBalance(ServerPlayerEntity player) {
        // TODO: brancher Impactor ici
        // exemple (fictif): return EconomyApi.getBalance(player.getUuid());
        throw new UnsupportedOperationException("Impactor API not wired");
    }

    @Override
    public boolean withdraw(ServerPlayerEntity player, long amount) {
        // TODO: brancher Impactor ici
        // exemple (fictif): return EconomyApi.withdraw(player.getUuid(), amount);
        throw new UnsupportedOperationException("Impactor API not wired");
    }

    @Override
    public void deposit(ServerPlayerEntity player, long amount) {
        // TODO: brancher Impactor ici
        // exemple (fictif): EconomyApi.deposit(player.getUuid(), amount);
        throw new UnsupportedOperationException("Impactor API not wired");
    }
}