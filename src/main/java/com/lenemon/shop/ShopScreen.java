package com.lenemon.shop;

import com.lenemon.network.shop.ShopActionHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Point d'entrée serveur pour ouvrir le shop.
 * Délègue désormais à ShopActionHandler (custom Screen client-side).
 */
public class ShopScreen {
    public static void open(ServerPlayerEntity player) {
        ShopActionHandler.sendShopOpen(player);
    }
}
