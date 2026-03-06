package com.lenemon.casino;

import com.lenemon.block.CasinoState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Unit;

import java.util.*;

/**
 * The type Casino spin ticker.
 */
public class CasinoSpinTicker {

    private static final int[] REEL_SLOTS = {11, 13, 15};
    private static final int SPIN_TICKS = 60; // 3 secondes
    private static final Map<UUID, SpinTask> TASKS = new HashMap<>();
    private static boolean registered = false;
    private static final Map<UUID, FinishedTask> FINISHED = new HashMap<>();

    private static class FinishedTask {
        /**
         * The Casino.
         */
        final CasinoWorldData.CasinoData casino;
        /**
         * The Data.
         */
        final CasinoWorldData data;
        /**
         * The Pos.
         */
        final BlockPos pos;

        /**
         * Instantiates a new Finished task.
         *
         * @param casino the casino
         * @param data   the data
         * @param pos    the pos
         */
        FinishedTask(CasinoWorldData.CasinoData casino, CasinoWorldData data, BlockPos pos) {
            this.casino = casino;
            this.data = data;
            this.pos = pos;
        }
    }

    private static class SpinTask {
        /**
         * The Player id.
         */
        final UUID playerId;
        /**
         * The Casino.
         */
        final CasinoWorldData.CasinoData casino;
        /**
         * The Data.
         */
        final CasinoWorldData data;
        /**
         * The Pos.
         */
        final BlockPos pos;
        /**
         * The Inventory.
         */
        final SimpleInventory inventory;
        /**
         * The Win.
         */
        final boolean win;
        /**
         * The Ticks left.
         */
        int ticksLeft = SPIN_TICKS;
        /**
         * The Rng.
         */
        final Random rng = new Random();
        /**
         * The Player ref.
         */
        final ServerPlayerEntity playerRef;


        /**
         * Instantiates a new Spin task.
         *
         * @param player    the player
         * @param casino    the casino
         * @param data      the data
         * @param pos       the pos
         * @param inventory the inventory
         * @param win       the win
         */
        SpinTask(ServerPlayerEntity player, CasinoWorldData.CasinoData casino,
                 CasinoWorldData data, BlockPos pos, SimpleInventory inventory, boolean win) {
            this.playerId = player.getUuid();
            this.casino = casino;
            this.data = data;
            this.pos = pos;
            this.inventory = inventory;
            this.win = win;
            this.playerRef = player;
        }
    }

    /**
     * Register.
     */
    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(CasinoSpinTicker::onTick);
    }

    /**
     * Request replay.
     *
     * @param player the player
     */
    public static void requestReplay(ServerPlayerEntity player) {
        // Récupère la dernière tâche terminée pour ce joueur
        // On stocke les infos dans une map séparée
        FinishedTask finished = FINISHED.get(player.getUuid());
        if (finished == null) return;

        // Vérifie que le casino est encore jouable
        if (finished.casino.state != com.lenemon.block.CasinoState.ACTIVE) {
            player.sendMessage(Text.literal("§c[Casino] Ce casino n'est plus disponible."), false);
            return;
        }

        CasinoSpinHandler.spinFromWorldData(player, finished.casino, finished.data, finished.pos);
        FINISHED.remove(player.getUuid());
    }

    /**
     * Start.
     *
     * @param player    the player
     * @param casino    the casino
     * @param data      the data
     * @param pos       the pos
     * @param inventory the inventory
     */
    public static void start(ServerPlayerEntity player, CasinoWorldData.CasinoData casino,
                             CasinoWorldData data, BlockPos pos, SimpleInventory inventory) {
        register();
        int roll = new Random().nextInt(10000) + 1;
        boolean win = roll <= casino.winChance;
        TASKS.put(player.getUuid(), new SpinTask(player, casino, data, pos, inventory, win));
    }

    private static void onTick(MinecraftServer server) {
        if (TASKS.isEmpty()) return;

        Iterator<Map.Entry<UUID, SpinTask>> it = TASKS.entrySet().iterator();
        while (it.hasNext()) {
            SpinTask task = it.next().getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(task.playerId);

            if (player == null) {
                // Joueur déconnecté → unlock
                task.casino.locked = false;
                task.data.markDirty();
                it.remove();
                continue;
            }

            // Animation toutes les 4 ticks
            if (task.ticksLeft % 4 == 0) {
                if (task.ticksLeft > 8) {
                    // Animation aléatoire
                    for (int slot : REEL_SLOTS) {
                        task.inventory.setStack(slot, task.rng.nextBoolean() ? greenWool() : redWool());
                    }
                } else {
                    // Dernières frames → fige sur le résultat
                    if (task.win) {
                        for (int slot : REEL_SLOTS) {
                            task.inventory.setStack(slot, greenWool());
                        }
                    } else {
                        // Aléatoire mais jamais 3 vertes
                        ItemStack[] result = generateLoseResult(task.rng);
                        for (int i = 0; i < REEL_SLOTS.length; i++) {
                            task.inventory.setStack(REEL_SLOTS[i], result[i]);
                        }
                    }
                }
            }

            task.ticksLeft--;
            if (task.ticksLeft > 0) continue;

            // Fin du spin → résout le résultat mais garde la fenêtre ouverte
            CasinoSpinHandler.resolveResultFromWorldData(player, task.casino, task.data,
                    task.win, task.casino.entryPrice);

            if (task.win) {
                // Bouton fermer uniquement
                ItemStack closeBtn = new ItemStack(Items.EMERALD);
                closeBtn.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal("§a§l🎉 Vous avez gagné ! Cliquez pour fermer"));
                closeBtn.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                task.inventory.setStack(22, closeBtn);
            } else {
                // Bouton fermer + bouton rejouer
                ItemStack closeBtn = new ItemStack(Items.BARRIER);
                closeBtn.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§c§lFermer"));
                closeBtn.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                task.inventory.setStack(20, closeBtn);

                ItemStack replayBtn = new ItemStack(Items.EMERALD);
                replayBtn.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§a§lRejouer !"));
                replayBtn.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                        List.of(Text.literal("§7Coût : §e" + task.casino.entryPrice + " PokéCoins"))));
                replayBtn.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                task.inventory.setStack(24, replayBtn);
            }
            if (!task.win) {
                FINISHED.put(task.playerId, new FinishedTask(task.casino, task.data, task.pos));
            }
            it.remove();
        }
    }

    private static ItemStack[] generateLoseResult(Random rng) {
        ItemStack[] result = new ItemStack[3];
        // Génère jusqu'à avoir au moins un rouge
        do {
            for (int i = 0; i < 3; i++) {
                result[i] = rng.nextBoolean() ? greenWool() : redWool();
            }
        } while (result[0].getItem() == Items.LIME_WOOL
                && result[1].getItem() == Items.LIME_WOOL
                && result[2].getItem() == Items.LIME_WOOL);
        return result;
    }

    private static ItemStack greenWool() {
        ItemStack item = new ItemStack(Items.LIME_WOOL);
        item.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§a§l✔"));
        item.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return item;
    }

    private static ItemStack redWool() {
        ItemStack item = new ItemStack(Items.RED_WOOL);
        item.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§c§l✘"));
        item.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return item;
    }
}