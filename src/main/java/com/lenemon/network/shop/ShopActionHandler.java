package com.lenemon.network.shop;

import com.lenemon.shop.ShopCategory;
import com.lenemon.shop.ShopConfig;
import com.lenemon.shop.ShopItem;
import com.lenemon.util.EconomyHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler C2S pour toutes les actions du shop.
 * Remplace la logique de ShopScreen et ShopCategoryScreen côté serveur.
 */
public class ShopActionHandler {

    private static final int PAGE_SIZE = 12;

    public static void handle(ShopActionPayload payload, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            ServerPlayerEntity player = ctx.player();
            String action = payload.action();

            if (action.equals("open_shop") || action.equals("back_to_shop")) {
                sendShopOpen(player);
            } else if (action.startsWith("open_category:")) {
                // Format : "open_category:<index>" ou "open_category:<index>:page:<n>"
                String remainder = action.substring("open_category:".length());
                String[] parts = remainder.split(":page:");
                try {
                    int index = Integer.parseInt(parts[0]);
                    int page = parts.length >= 2 ? Integer.parseInt(parts[1]) : 0;
                    sendCategoryOpen(player, index, page);
                } catch (NumberFormatException e) {
                    player.sendMessage(Text.literal("§c[Shop] Action invalide : " + action), false);
                }
            } else if (action.startsWith("buy:")) {
                handleBuyAction(player, action);
            } else if (action.startsWith("sell_all:")) {
                handleSellAllAction(player, action);
            } else if (action.startsWith("sell:")) {
                handleSellAction(player, action);
            } else {
                player.sendMessage(Text.literal("§c[Shop] Action inconnue : " + action), false);
            }
        });
    }

    /**
     * Envoie le payload d'ouverture du shop principal (liste des catégories).
     * Appelable depuis Lenemon.java ou ShopScreen.open().
     */
    public static void sendShopOpen(ServerPlayerEntity player) {
        List<ShopCategory> categories = ShopConfig.getCategories();
        List<ShopCategoryDto> dtos = new ArrayList<>();
        for (ShopCategory cat : categories) {
            dtos.add(new ShopCategoryDto(cat.name, cat.icon, cat.items.size()));
        }
        ServerPlayNetworking.send(player, new ShopOpenPayload(dtos));
    }

    /**
     * Envoie le payload d'ouverture d'une catégorie avec ses items paginés.
     */
    public static void sendCategoryOpen(ServerPlayerEntity player, int categoryIndex, int page) {
        List<ShopCategory> categories = ShopConfig.getCategories();
        if (categoryIndex < 0 || categoryIndex >= categories.size()) {
            player.sendMessage(Text.literal("§c[Shop] Catégorie introuvable."), false);
            return;
        }
        ShopCategory cat = categories.get(categoryIndex);

        int totalPages = Math.max(1, (cat.items.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, cat.items.size());
        List<ShopItemDto> itemDtos = new ArrayList<>();
        for (int i = start; i < end; i++) {
            ShopItem item = cat.items.get(i);
            itemDtos.add(new ShopItemDto(item.id, item.displayName, item.buyPrice, item.sellPrice));
        }

        long balance = EconomyHelper.getBalance(player);
        ServerPlayNetworking.send(player, new ShopCategoryOpenPayload(
                cat.name, categoryIndex, itemDtos, balance, page, totalPages
        ));
    }

    // ── Achat ─────────────────────────────────────────────────────────────────

    /**
     * Parse et traite une action d'achat.
     * Format : "buy:<namespace>:<path>:<qty>:cat:<catIndex>:page:<page>"
     * Exemple : "buy:minecraft:diamond:1:cat:0:page:0"
     */
    private static void handleBuyAction(ServerPlayerEntity player, String action) {
        // Retirer le préfixe "buy:"
        String rest = action.substring("buy:".length());
        // Séparer la partie item+qty de la partie cat+page
        // Format de la fin : ":cat:<ci>:page:<p>"
        int catIdx = rest.lastIndexOf(":cat:");
        if (catIdx < 0) {
            player.sendMessage(Text.literal("§c[Shop] Format d'action invalide."), false);
            return;
        }
        String itemQtyPart = rest.substring(0, catIdx);    // "minecraft:diamond:1"
        String catPagePart = rest.substring(catIdx + 5);  // "<ci>:page:<p>"

        int pageIdx = catPagePart.indexOf(":page:");
        if (pageIdx < 0) {
            player.sendMessage(Text.literal("§c[Shop] Format d'action invalide."), false);
            return;
        }

        int categoryIndex;
        int page;
        try {
            categoryIndex = Integer.parseInt(catPagePart.substring(0, pageIdx));
            page = Integer.parseInt(catPagePart.substring(pageIdx + 6));
        } catch (NumberFormatException e) {
            player.sendMessage(Text.literal("§c[Shop] Format d'action invalide."), false);
            return;
        }

        // Extraire itemId et qty depuis "namespace:path:qty"
        // Le dernier segment est la quantité, le reste est l'ID
        int lastColon = itemQtyPart.lastIndexOf(':');
        if (lastColon < 0) {
            player.sendMessage(Text.literal("§c[Shop] Format d'item invalide."), false);
            return;
        }
        String itemId = itemQtyPart.substring(0, lastColon);
        int qty;
        try {
            qty = Integer.parseInt(itemQtyPart.substring(lastColon + 1));
        } catch (NumberFormatException e) {
            player.sendMessage(Text.literal("§c[Shop] Quantité invalide."), false);
            return;
        }

        ShopItem shopItem = findItem(categoryIndex, itemId);
        if (shopItem == null) {
            player.sendMessage(Text.literal("§c[Shop] Item introuvable dans la catégorie."), false);
            return;
        }

        handleBuy(player, shopItem, qty, categoryIndex, page);
    }

    private static void handleBuy(ServerPlayerEntity player, ShopItem shopItem, int qty, int categoryIndex, int page) {
        if (shopItem.buyPrice < 0) {
            player.sendMessage(Text.literal("§c[Shop] Cet item n'est pas en vente."), false);
            return;
        }

        long total = (long) Math.ceil(shopItem.buyPrice * qty);

        if (!EconomyHelper.debit(player, total, "Shop")) {
            player.sendMessage(Text.literal(
                    "§c[Shop] Solde insuffisant ! Il vous faut §f" + total + "₽§c."), false);
            sendCategoryOpen(player, categoryIndex, page);
            return;
        }

        ItemStack reward = new ItemStack(Registries.ITEM.get(Identifier.of(shopItem.id)), qty);
        boolean added = player.getInventory().insertStack(reward);
        if (!added) {
            EconomyHelper.credit(player, total);
            player.sendMessage(Text.literal("§c[Shop] Inventaire plein !"), false);
            sendCategoryOpen(player, categoryIndex, page);
            return;
        }

        player.sendMessage(Text.literal(
                "§a[Shop] Acheté §f" + qty + "x " + shopItem.displayName
                        + "§a pour §f" + total + "₽§a."), false);
        sendCategoryOpen(player, categoryIndex, page);
    }

    // ── Vente x1 ──────────────────────────────────────────────────────────────

    /**
     * Parse et traite une action de vente unitaire.
     * Format : "sell:<namespace>:<path>:1:cat:<catIndex>:page:<page>"
     */
    private static void handleSellAction(ServerPlayerEntity player, String action) {
        String rest = action.substring("sell:".length());
        int catIdx = rest.lastIndexOf(":cat:");
        if (catIdx < 0) {
            player.sendMessage(Text.literal("§c[Shop] Format d'action invalide."), false);
            return;
        }
        String itemQtyPart = rest.substring(0, catIdx);
        String catPagePart = rest.substring(catIdx + 5);

        int pageIdx = catPagePart.indexOf(":page:");
        if (pageIdx < 0) {
            player.sendMessage(Text.literal("§c[Shop] Format d'action invalide."), false);
            return;
        }

        int categoryIndex;
        int page;
        try {
            categoryIndex = Integer.parseInt(catPagePart.substring(0, pageIdx));
            page = Integer.parseInt(catPagePart.substring(pageIdx + 6));
        } catch (NumberFormatException e) {
            player.sendMessage(Text.literal("§c[Shop] Format d'action invalide."), false);
            return;
        }

        // Extraire itemId (ignorer le qty "1" à la fin)
        int lastColon = itemQtyPart.lastIndexOf(':');
        if (lastColon < 0) {
            player.sendMessage(Text.literal("§c[Shop] Format d'item invalide."), false);
            return;
        }
        String itemId = itemQtyPart.substring(0, lastColon);

        ShopItem shopItem = findItem(categoryIndex, itemId);
        if (shopItem == null) {
            player.sendMessage(Text.literal("§c[Shop] Item introuvable dans la catégorie."), false);
            return;
        }

        handleSell(player, shopItem, false, categoryIndex, page);
    }

    // ── Vente tout ────────────────────────────────────────────────────────────

    /**
     * Parse et traite une action de vente totale.
     * Format : "sell_all:<namespace>:<path>:cat:<catIndex>:page:<page>"
     */
    private static void handleSellAllAction(ServerPlayerEntity player, String action) {
        String rest = action.substring("sell_all:".length());
        int catIdx = rest.lastIndexOf(":cat:");
        if (catIdx < 0) {
            player.sendMessage(Text.literal("§c[Shop] Format d'action invalide."), false);
            return;
        }
        String itemId = rest.substring(0, catIdx);
        String catPagePart = rest.substring(catIdx + 5);

        int pageIdx = catPagePart.indexOf(":page:");
        if (pageIdx < 0) {
            player.sendMessage(Text.literal("§c[Shop] Format d'action invalide."), false);
            return;
        }

        int categoryIndex;
        int page;
        try {
            categoryIndex = Integer.parseInt(catPagePart.substring(0, pageIdx));
            page = Integer.parseInt(catPagePart.substring(pageIdx + 6));
        } catch (NumberFormatException e) {
            player.sendMessage(Text.literal("§c[Shop] Format d'action invalide."), false);
            return;
        }

        ShopItem shopItem = findItem(categoryIndex, itemId);
        if (shopItem == null) {
            player.sendMessage(Text.literal("§c[Shop] Item introuvable dans la catégorie."), false);
            return;
        }

        handleSell(player, shopItem, true, categoryIndex, page);
    }

    private static void handleSell(ServerPlayerEntity player, ShopItem shopItem, boolean sellAll, int categoryIndex, int page) {
        if (shopItem.sellPrice < 0) {
            player.sendMessage(Text.literal("§c[Shop] Le serveur ne rachète pas cet item."), false);
            return;
        }

        var targetItem = Registries.ITEM.get(Identifier.of(shopItem.id));
        int qty = 0;

        if (sellAll) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() == targetItem) {
                    qty += stack.getCount();
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                }
            }
        } else {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() == targetItem && !stack.isEmpty()) {
                    stack.decrement(1);
                    qty = 1;
                    break;
                }
            }
        }

        if (qty == 0) {
            player.sendMessage(Text.literal(
                    "§c[Shop] Vous n'avez pas de §f" + shopItem.displayName + "§c."), false);
            sendCategoryOpen(player, categoryIndex, page);
            return;
        }

        long total = (long) Math.ceil(shopItem.sellPrice * qty);
        EconomyHelper.credit(player, total);
        player.sendMessage(Text.literal(
                "§a[Shop] Vendu §f" + qty + "x " + shopItem.displayName
                        + "§a pour §f" + total + "₽§a."), false);
        sendCategoryOpen(player, categoryIndex, page);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Cherche un ShopItem par son id dans une catégorie donnée (index global).
     * Parcourt tous les items de la catégorie (pas seulement la page courante).
     */
    private static ShopItem findItem(int categoryIndex, String itemId) {
        List<ShopCategory> categories = ShopConfig.getCategories();
        if (categoryIndex < 0 || categoryIndex >= categories.size()) return null;
        for (ShopItem item : categories.get(categoryIndex).items) {
            if (item.id.equals(itemId)) return item;
        }
        return null;
    }

    private static String formatPrice(double price) {
        if (price == Math.floor(price)) return String.valueOf((long) price);
        return String.format("%.1f", price).replace(",", ".");
    }
}
