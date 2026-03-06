package com.lenemon.gift;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * The type Gift lottery ticker.
 */
public class GiftLotteryTicker {

    private static final int[] REEL = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int CENTER = 13;
    private static final int SPIN_TICKS = 80; // 4 secondes
    private static final Map<UUID, LotteryTask> TASKS = new HashMap<>();
    private static boolean registered = false;


    private static class LotteryTask {
        /**
         * The Player id.
         */
        final UUID playerId;
        /**
         * The Chest.
         */
        final GiftChestData.ChestEntry chest;
        /**
         * The World.
         */
        final ServerWorld world;
        /**
         * The Inventory.
         */
        final SimpleInventory inventory;
        /**
         * The Won reward.
         */
        final GiftReward wonReward;
        /**
         * The All rewards.
         */
        final List<GiftReward> allRewards;
        /**
         * The Ticks left.
         */
        int ticksLeft = SPIN_TICKS;
        /**
         * The Rng.
         */
        final Random rng = new Random();
        /**
         * The Total chance.
         */
        final double totalChance;


        /**
         * Instantiates a new Lottery task.
         *
         * @param player     the player
         * @param chest      the chest
         * @param world      the world
         * @param inventory  the inventory
         * @param wonReward  the won reward
         * @param allRewards the all rewards
         */
        LotteryTask(ServerPlayerEntity player, GiftChestData.ChestEntry chest,
                    ServerWorld world, SimpleInventory inventory,
                    GiftReward wonReward, List<GiftReward> allRewards) {
            this.playerId = player.getUuid();
            this.chest = chest;
            this.world = world;
            this.inventory = inventory;
            this.wonReward = wonReward;
            this.allRewards = allRewards;
            this.totalChance = allRewards.stream().mapToDouble(r -> r.chance).sum();
        }
    }

    /**
     * Register.
     */
    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(GiftLotteryTicker::onTick);
    }

    /**
     * Start.
     *
     * @param player    the player
     * @param chest     the chest
     * @param world     the world
     * @param inventory the inventory
     */
    public static void start(ServerPlayerEntity player, GiftChestData.ChestEntry chest,
                             ServerWorld world, SimpleInventory inventory) {
        register();
        List<GiftReward> rewards = GiftChestConfig.load(player.getServer(), chest.chestUUID);
        if (rewards.isEmpty()) {
            player.sendMessage(Text.literal("§c[Cadeau] Ce coffre n'a pas encore de récompenses !"), false);
            player.closeHandledScreen();
            return;
        }
        GiftReward won = GiftChestConfig.roll(rewards);
        TASKS.put(player.getUuid(), new LotteryTask(player, chest, world, inventory, won, rewards));
    }

    private static void onTick(MinecraftServer server) {
        if (TASKS.isEmpty()) return;
        Iterator<Map.Entry<UUID, LotteryTask>> it = TASKS.entrySet().iterator();

        while (it.hasNext()) {
            LotteryTask task = it.next().getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(task.playerId);

            if (player == null) { it.remove(); return; }

            // Animation défilement de droite à gauche toutes les 2 ticks
            if (task.ticksLeft % 2 == 0) {
                if (task.ticksLeft > 20) {
                    shiftReel(task);
                } else {
                    shiftReelToWin(task);
                }
            }

            task.ticksLeft--;
            if (task.ticksLeft > 0) continue;

            // Fin → donne le cadeau
            giveReward(player, task, server);
            it.remove();
        }
    }

    private static void shiftReel(LotteryTask task) {
        // Décale à gauche
        for (int i = 0; i < REEL.length - 1; i++) {
            task.inventory.setStack(REEL[i], task.inventory.getStack(REEL[i + 1]));
        }
        // Nouvel item aléatoire à droite — jamais le cadeau gagné sauf dernier tick
        GiftReward random = task.allRewards.get(task.rng.nextInt(task.allRewards.size()));
        task.inventory.setStack(REEL[REEL.length - 1], rewardToDisplayItem(random, false, task.totalChance, task.world));
    }

    private static void shiftReelToWin(LotteryTask task) {
        // Décale à gauche
        for (int i = 0; i < REEL.length - 1; i++) {
            task.inventory.setStack(REEL[i], task.inventory.getStack(REEL[i + 1]));
        }
        // Remplit toute la ligne d'aléatoires sauf le centre
        for (int i = 0; i < REEL.length; i++) {
            if (REEL[i] != CENTER) {
                GiftReward r = task.allRewards.get(task.rng.nextInt(task.allRewards.size()));
                task.inventory.setStack(REEL[i], rewardToDisplayItem(r, false, task.totalChance, task.world));
            }
        }
        // Le cadeau gagné uniquement au centre, uniquement au dernier tick
        if (task.ticksLeft <= 2) {
            task.inventory.setStack(CENTER, rewardToDisplayItem(task.wonReward, true, task.totalChance, task.world));
        } else {
            GiftReward r = task.allRewards.get(task.rng.nextInt(task.allRewards.size()));
            task.inventory.setStack(CENTER, rewardToDisplayItem(r, false, task.totalChance, task.world));
        }
    }

    private static ItemStack rewardToDisplayItem(GiftReward r, boolean highlight,
                                                 double total, ServerWorld world) {
        ItemStack item;
        if (r.type.equals("item")) {
            try {
                NbtCompound nbt = net.minecraft.nbt.StringNbtReader.parse(r.data);
                item = ItemStack.CODEC.parse(
                        net.minecraft.registry.RegistryOps.of(
                                net.minecraft.nbt.NbtOps.INSTANCE,
                                world.getRegistryManager()
                        ), nbt
                ).result().orElse(ItemStack.EMPTY);
                if (item.isEmpty()) throw new Exception("empty");
            } catch (Exception e) {
                item = new ItemStack(Registries.ITEM.get(Identifier.of("minecraft:paper")));
            }
        } else {
            item = new ItemStack(Items.COMMAND_BLOCK);
        }

        item.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal((highlight ? "§6§l★ " : "§f") + r.displayName));
        List<Text> lore = new ArrayList<>();
        double realChance = total > 0 ? (r.chance / total * 100) : 0;
        lore.add(Text.literal("§7Chance : §e" + String.format("%.2f", realChance) + "%"));
        if (highlight) lore.add(Text.literal("§a§l▶ C'est votre récompense !"));
        item.set(DataComponentTypes.LORE, new LoreComponent(lore));
        item.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return item;
    }

    private static void giveReward(ServerPlayerEntity player, LotteryTask task,
                                   MinecraftServer server) {
        GiftReward reward = task.wonReward;

        // Retire le bon cadeau de la main
        player.getMainHandStack().decrement(1);

        if (reward.type.equals("item")) {
            ItemStack rewardItem;
            try {
                NbtCompound nbt = net.minecraft.nbt.StringNbtReader.parse(reward.data);
                rewardItem = ItemStack.CODEC.parse(
                        net.minecraft.registry.RegistryOps.of(
                                net.minecraft.nbt.NbtOps.INSTANCE,
                                player.getServerWorld().getRegistryManager()
                        ), nbt
                ).result().orElse(ItemStack.EMPTY);
            } catch (Exception e) {
                // Fallback sur l'ancien système si le data est un simple ID
                rewardItem = new ItemStack(Registries.ITEM.get(Identifier.of(reward.data)), reward.count);
            }
            boolean added = player.getInventory().insertStack(rewardItem);
            if (!added) {
                // Inventaire plein → drop par terre
                player.dropItem(rewardItem, false);
                player.sendMessage(Text.literal(
                        "§e[Cadeau] Inventaire plein ! §f" + reward.displayName
                                + " §ea été déposé par terre."), false);
            } else {
                player.sendMessage(Text.literal(
                        "§a[Cadeau] Vous avez reçu : §f" + reward.displayName + " §ax" + reward.count), false);
            }
        } else {
            // Commande : remplace %player% par le nom du joueur
            String cmd = reward.data.replace("%player%", player.getName().getString());
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(), cmd);
            player.sendMessage(Text.literal(
                    "§a[Cadeau] Récompense reçue : §f" + reward.displayName), false);
        }

        // Bouton fermer slot 26
        ItemStack closeBtn = new ItemStack(Items.EMERALD);
        closeBtn.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§a§l🎁 " + reward.displayName + " — Cliquez pour fermer"));
        closeBtn.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        task.inventory.setStack(26, closeBtn);
    }
}