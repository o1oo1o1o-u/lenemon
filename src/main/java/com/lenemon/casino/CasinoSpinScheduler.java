package com.lenemon.casino;

import com.lenemon.block.CasinoState;
import com.lenemon.util.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The type Casino spin scheduler.
 */
public final class CasinoSpinScheduler {

    private CasinoSpinScheduler() {}

    /**
     * The constant DEFAULT_DELAY_TICKS.
     */
    public static final int DEFAULT_DELAY_TICKS = 100; // doit matcher ton STOP_DELAY_TICKS client

    private static final class PendingSpin {
        /**
         * The Casino uuid.
         */
        final UUID casinoUuid;
        /**
         * The Player uuid.
         */
        final UUID playerUuid;
        /**
         * The Price.
         */
        final long price;
        /**
         * The Win.
         */
        final boolean win;
        /**
         * The Left.
         */
        final int left;
        /**
         * The Right.
         */
        final int right;
        /**
         * The Execute at tick.
         */
        final int executeAtTick;

        /**
         * Instantiates a new Pending spin.
         *
         * @param casinoUuid    the casino uuid
         * @param playerUuid    the player uuid
         * @param price         the price
         * @param win           the win
         * @param left          the left
         * @param right         the right
         * @param executeAtTick the execute at tick
         */
        PendingSpin(UUID casinoUuid, UUID playerUuid, long price, boolean win, int left, int right, int executeAtTick) {
            this.casinoUuid = casinoUuid;
            this.playerUuid = playerUuid;
            this.price = price;
            this.win = win;
            this.left = left;
            this.right = right;
            this.executeAtTick = executeAtTick;
        }
    }

    // 1 pending par casino (anti double spin par casino)
    private static final Map<UUID, PendingSpin> pendingByCasino = new HashMap<>();

    /**
     * Has pending boolean.
     *
     * @param casinoUuid the casino uuid
     * @return the boolean
     */
    public static boolean hasPending(UUID casinoUuid) {
        return pendingByCasino.containsKey(casinoUuid);
    }

    /**
     * Schedule.
     *
     * @param casinoUuid    the casino uuid
     * @param playerUuid    the player uuid
     * @param price         the price
     * @param win           the win
     * @param left          the left
     * @param right         the right
     * @param executeAtTick the execute at tick
     */
    public static void schedule(UUID casinoUuid, UUID playerUuid, long price, boolean win, int left, int right, int executeAtTick) {
        pendingByCasino.put(casinoUuid, new PendingSpin(casinoUuid, playerUuid, price, win, left, right, executeAtTick));
    }

    /**
     * Tick.
     *
     * @param server the server
     */
    public static void tick(MinecraftServer server) {
        int now = server.getTicks();

        Iterator<Map.Entry<UUID, PendingSpin>> it = pendingByCasino.entrySet().iterator();
        while (it.hasNext()) {
            PendingSpin p = it.next().getValue();
            if (now < p.executeAtTick) continue;
            it.remove();

            // Joueur offline : refund + unlock
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(p.playerUuid);
            if (player == null) {
                unlockCasinoAndMarkDirty(server, p.casinoUuid);
                EconomyHelper.credit(p.playerUuid, p.price);
                continue;
            }

            // Le casino reste locked jusqu'à ce que le client envoie CasinoAnimDonePayload
            // On ne fait rien ici — la résolution se passe dans le receiver CasinoAnimDonePayload
        }
    }

    // Si tu as déjà data.getCasinoByUUID, remplace ce helper par ton appel direct
    private static CasinoWorldData.CasinoData getCasinoByUUID(CasinoWorldData data, UUID casinoUuid) {
        return data.getCasinoByUUID(casinoUuid);
    }

    private static void unlockCasinoAndMarkDirty(MinecraftServer server, UUID casinoUuid) {
        for (var world : server.getWorlds()) {
            var data = CasinoWorldData.get(world);
            CasinoWorldData.CasinoData casino = null;

            try {
                casino = data.getCasinoByUUID(casinoUuid);
            } catch (Throwable ignored) {}

            if (casino != null) {
                casino.locked = false;
                data.markDirty();
                return;
            }
        }
    }

    /**
     * Remove pending.
     *
     * @param casinoUuid the casino uuid
     */
    public static void removePending(UUID casinoUuid) {
        pendingByCasino.remove(casinoUuid);
    }
}