package com.lenemon.ah;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Vérifie toutes les 60 secondes (1200 ticks) si des ventes ont expiré.
 */
public class AhExpiryTicker {

    private static int tickCount = 0;

    public static void tick(MinecraftServer server) {
        if (++tickCount < 1200) return;
        tickCount = 0;

        AhWorldData data = AhWorldData.get(server.getOverworld());
        long now = System.currentTimeMillis();

        List<AhListing> toExpire = new ArrayList<>(data.activeListings).stream()
                .filter(l -> !l.sold && !l.expired && now > l.expiresAt)
                .collect(Collectors.toList());

        for (AhListing listing : toExpire) {
            data.expireListing(listing.listingId);

            ServerPlayerEntity seller = server.getPlayerManager().getPlayer(listing.sellerUuid);
            if (seller != null) {
                String itemName = "pokemon".equals(listing.type)
                        ? listing.pokemonDisplayName
                        : listing.itemDisplayName;
                seller.sendMessage(Text.literal(
                        "§e[AH] Votre vente de §f" + itemName
                                + " §ea expiré. Récupérez-la via /ah mystuff."), false);
            }
        }
    }
}
