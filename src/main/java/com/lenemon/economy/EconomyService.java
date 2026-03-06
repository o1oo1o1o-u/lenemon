package com.lenemon.economy;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The interface Economy service.
 */
public interface EconomyService {
    /**
     * Gets balance.
     *
     * @param player the player
     * @return the balance
     */
    long getBalance(ServerPlayerEntity player);

    /**
     * Withdraw boolean.
     *
     * @param player the player
     * @param amount the amount
     * @return the boolean
     */
    boolean withdraw(ServerPlayerEntity player, long amount);

    /**
     * Deposit.
     *
     * @param player the player
     * @param amount the amount
     */
    void deposit(ServerPlayerEntity player, long amount);
}