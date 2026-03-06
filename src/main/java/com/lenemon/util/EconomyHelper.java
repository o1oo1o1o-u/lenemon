package com.lenemon.util;

import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account ;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

/**
 * The type Economy helper.
 */
public class EconomyHelper {

    // Cache de l'objet account — même instance réutilisée
    private static final Map<UUID, Account > accountCache = new ConcurrentHashMap<>();


    // Verrou anti double transaction
    private static final Set<UUID> locks = Collections.synchronizedSet(ConcurrentHashMap.newKeySet());

    /**
     * Try lock boolean.
     *
     * @param uuid the uuid
     * @return the boolean
     */
    public static boolean tryLock(UUID uuid) {

        return locks.add(uuid);
    }

    /**
     * Unlock.
     *
     * @param uuid the uuid
     */
    public static void unlock(UUID uuid) {
        locks.remove(uuid);
    }

    /**
     * Précharge le compte en cache — appelle ça au login du joueur  @param player the player
     *
     * @param player the player
     */
    public static void preloadAccount(ServerPlayerEntity player) {
        try {
            var service = EconomyService.instance();
            var currency = service.currencies().primary();
            var account = service.account(currency, player.getUuid()).join();

            accountCache.put(player.getUuid(), account);
        } catch (Exception ignored) {}
    }

    /**
     * Vide le cache au logout  @param uuid the uuid
     *
     * @param uuid the uuid
     */
    public static void evictAccount(UUID uuid) {
        accountCache.remove(uuid);
        locks.remove(uuid);
    }

    private static Account getAccount(ServerPlayerEntity player) {
        return accountCache.computeIfAbsent(player.getUuid(), uuid -> {
            try {
                var service = EconomyService.instance();
                var currency = service.currencies().primary();
                return service.account(currency, uuid).join();
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Gets balance.
     *
     * @param player the player
     * @return the balance
     */
    public static long getBalance(ServerPlayerEntity player) {
        try {
            Account  account = getAccount(player);
            if (account == null) return 0;
            return account.balance().longValue();
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c[Economy] Erreur lecture balance : " + e.getMessage()), false);
            return 0;
        }
    }

    /**
     * Debit boolean.
     *
     * @param player     the player
     * @param amount     the amount
     * @param pluginName the plugin name
     * @return the boolean
     */
    public static boolean debit(ServerPlayerEntity player, long amount, String pluginName) {
        try {
            Account  account = getAccount(player);
            if (account == null) return false;

            BigDecimal cost = BigDecimal.valueOf(amount);

            if (account.balance().compareTo(cost) < 0) return false;

            var result = account.withdraw(cost);
            return result.successful();

        } catch (Exception e) {
            player.sendMessage(Text.literal("§c[" + pluginName + "] Erreur economy : " + e.getMessage()), false);
            return false;
        }
    }

    /**
     * Credit.
     *
     * @param targetUUID the target uuid
     * @param amount     the amount
     */
    public static void credit(UUID targetUUID, long amount) {
        try {
            var service = EconomyService.instance();
            var currency = service.currencies().primary();
            // Pour le credit on peut appeler directement, pas de race condition
            var account = service.account(currency, targetUUID).join();
            account.deposit(BigDecimal.valueOf(amount));
        } catch (Exception ignored) {}
    }

    /**
     * Credit.
     *
     * @param player the player
     * @param amount the amount
     */
    public static void credit(ServerPlayerEntity player, long amount) {
        credit(player.getUuid(), amount);
    }

    /**
     * Debit async.
     *
     * @param player the player
     * @param amount the amount
     */
    public static void debitAsync(ServerPlayerEntity player, long amount) {
        var service = EconomyService.instance();
        var currency = service.currencies().primary();
        service.account(currency, player.getUuid())
                .thenAccept(account -> account.withdraw(BigDecimal.valueOf(amount)));
    }
}