package com.lenemon.shop;

import com.lenemon.util.EconomyHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Shop sell service.
 */
public class ShopSellService {

    private static Map<Item, Double> SELL_PRICES = null;

    private static Map<Item, Double> getSellPrices() {
        if (SELL_PRICES != null) return SELL_PRICES;

        Map<Item, Double> map = new HashMap<>();
        for (ShopCategory category : ShopConfig.getCategories()) {
            for (ShopItem si : category.items) {
                if (si.sellPrice >= 0) {
                    map.put(Registries.ITEM.get(Identifier.of(si.id)), si.sellPrice);
                }
            }
        }
        SELL_PRICES = map;
        return SELL_PRICES;
    }

    /**
     * Invalidate cache.
     */
    public static void invalidateCache() {
        SELL_PRICES = null;
    }

    /**
     * Sell stacks with percent long.
     *
     * @param player  the player
     * @param stacks  the stacks
     * @param percent the percent
     * @return the long
     */
    public static long sellStacksWithPercent(ServerPlayerEntity player, List<ItemStack> stacks, float percent) {
        Map<Item, Double> prices = getSellPrices();

        double total = 0;
        for (ItemStack st : stacks) {
            if (st == null || st.isEmpty()) continue;

            Double unit = prices.get(st.getItem());
            if (unit == null) continue;

            total += unit * st.getCount();
        }

        if (total <= 0) return 0;

        double bonus = total * percent;
        long finalAmount = (long) Math.ceil(total + bonus);

        EconomyHelper.credit(player, finalAmount);
        return finalAmount;
    }
}