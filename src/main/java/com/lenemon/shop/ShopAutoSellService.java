package com.lenemon.shop;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * The type Shop auto sell service.
 */
public final class ShopAutoSellService {

    private ShopAutoSellService() {}

    private static volatile Map<String, Double> SELL_PRICE_BY_ID = new HashMap<>();

    /**
     * The type Sell result.
     */
    public record SellResult(long earned, List<ItemStack> remaining) {}

    /**
     * Invalidate cache.
     */
    public static void invalidateCache() {
        SELL_PRICE_BY_ID = new HashMap<>();
    }

    private static Map<String, Double> getSellMap() {
        Map<String, Double> existing = SELL_PRICE_BY_ID;
        if (existing != null && !existing.isEmpty()) return existing;

        Map<String, Double> map = new HashMap<>();
        for (ShopCategory cat : ShopConfig.getCategories()) {
            if (cat == null || cat.items == null) continue;
            for (ShopItem it : cat.items) {
                if (it == null || it.id == null) continue;
                if (it.sellPrice <= 0) continue;
                map.put(it.id, it.sellPrice);
            }
        }

        SELL_PRICE_BY_ID = map;
        return map;
    }

    /**
     * Sell sell result.
     *
     * @param player       the player
     * @param items        the items
     * @param percentBonus the percent bonus
     * @return the sell result
     */
    public static SellResult sell(ServerPlayerEntity player, List<ItemStack> items, float percentBonus) {
        if (items == null || items.isEmpty()) return new SellResult(0L, List.of());

        Map<String, Double> sellMap = getSellMap();

        HashMap<String, Integer> countsById = new HashMap<>();
        ArrayList<ItemStack> remaining = new ArrayList<>();

        for (ItemStack st : items) {
            if (st == null || st.isEmpty()) continue;

            String key = net.minecraft.registry.Registries.ITEM.getId(st.getItem()).toString();

            Double unitPrice = sellMap.get(key);
            if (unitPrice == null || unitPrice <= 0) {
                remaining.add(st);
                continue;
            }

            countsById.merge(key, st.getCount(), Integer::sum);
        }

        long earned = 0L;

        for (var entry : countsById.entrySet()) {
            String id = entry.getKey();
            int count = entry.getValue();

            double unit = sellMap.get(id);
            double base = unit * count;
            double bonus = percentBonus > 0f ? base * percentBonus : 0.0;

            earned += (long) Math.ceil(base + bonus);
        }

        if (earned > 0) {
            com.lenemon.util.EconomyHelper.credit(player, earned);
        }

        return new SellResult(earned, remaining);
    }
}