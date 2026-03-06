package com.lenemon.client.hud;

/**
 * The type Hud balance cache.
 */
public class HudBalanceCache {

    private static long balance = 0;

    /**
     * Sets balance.
     *
     * @param balance the balance
     */
    public static void setBalance(long balance) {
        HudBalanceCache.balance = balance;
    }

    /**
     * Gets balance.
     *
     * @return the balance
     */
    public static long getBalance() {
        return balance;
    }
}