package com.lenemon.shop;

import com.lenemon.util.EconomyHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The type Shop sell pending service.
 */
public class ShopSellPendingService {

    private static final String NBT_PENDING_SELL = "lenemon_pending_sell";

    /**
     * Sell pending int.
     *
     * @param player  the player
     * @param percent the percent
     * @return the int
     */
    public static int sellPending(ServerPlayerEntity player, float percent) {

        ItemStack tool = player.getMainHandStack();
        if (tool.isEmpty()) return 0;

        NbtComponent comp = tool.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return 0;

        NbtCompound root = comp.copyNbt();
        NbtList list = root.getList(NBT_PENDING_SELL, 10);
        if (list.isEmpty()) return 0;

        var registries = player.getServer().getRegistryManager();
        Map<Item, Double> prices = buildPriceMap();

        double total = 0;
        NbtList remaining = new NbtList();

        for (int i = 0; i < list.size(); i++) {
            var el = list.get(i);
            var opt = ItemStack.fromNbt(registries, el);
            if (opt.isEmpty()) continue;

            ItemStack st = opt.get();
            if (st.isEmpty()) continue;

            Double unit = prices.get(st.getItem());
            if (unit == null) {
                remaining.add(el);
                continue;
            }

            total += unit * st.getCount();
        }

        if (remaining.isEmpty()) root.remove(NBT_PENDING_SELL);
        else root.put(NBT_PENDING_SELL, remaining);

        tool.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));

        if (total <= 0) return 1;

        double bonus = total * percent;
        long finalAmount = (long) Math.ceil(total + bonus);

        EconomyHelper.credit(player, finalAmount);
        return 1;
    }

    private static Map<Item, Double> buildPriceMap() {
        Map<Item, Double> sellable = new HashMap<>();

        for (ShopCategory category : ShopConfig.getCategories()) {
            for (ShopItem shopItem : category.items) {
                if (shopItem.sellPrice < 0) continue;
                Item item = Registries.ITEM.get(Identifier.of(shopItem.id));
                sellable.put(item, shopItem.sellPrice);
            }
        }

        return sellable;
    }
}