package com.lenemon.ah;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lenemon.casino.CasinoPokemonStorage;
import com.lenemon.discord.AhDiscordNotifier;
import com.lenemon.network.ah.AhActionPayload;
import com.lenemon.network.ah.AhBrowsePayload;
import com.lenemon.network.ah.AhItemSlotDto;
import com.lenemon.network.ah.AhListingDto;
import com.lenemon.network.ah.AhMyListingsPayload;
import com.lenemon.network.ah.AhPartyPokemonDto;
import com.lenemon.network.ah.AhSellItemPayload;
import com.lenemon.network.ah.AhSellPokemonPayload;
import com.lenemon.util.EconomyHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestionnaire côté serveur de toutes les actions C2S de l'Hôtel des Ventes.
 */
public class AhActionHandler {

    private static final int PAGE_SIZE = 6;

    /** Nombre max de ventes actives selon les permissions LuckPerms. */
    private static int getMaxListings(ServerPlayerEntity player) {
        if (Permissions.check(player, "lenemon.ah.sell.20", 2)) return 20;
        if (Permissions.check(player, "lenemon.ah.sell.15", 2)) return 15;
        if (Permissions.check(player, "lenemon.ah.sell.10", 2)) return 10;
        if (Permissions.check(player, "lenemon.ah.sell.5",  2)) return 5;
        return 5; // valeur par défaut
    }

    public static void handle(AhActionPayload payload, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = ctx.server();
            String action = payload.action();

            try {
                if (action.equals("open_ah")) {
                    sendBrowse(player, server, 0, "all", "date_listed");
                } else if (action.startsWith("browse:page:")) {
                    // Format: browse:page:<n>  ou  browse:page:<n>:filter:<f>:sort:<s>
                    // Exemple: browse:page:0:filter:all:sort:date_listed
                    String[] parts = action.split(":");
                    int page      = Integer.parseInt(parts[2]);
                    String filter = "all";
                    String sort   = "date_listed";
                    if (parts.length >= 7) {
                        // parts[3]="filter", parts[4]=<f>, parts[5]="sort", parts[6]=<s>
                        filter = parts[4];
                        sort   = parts[6];
                    }
                    sendBrowse(player, server, page, filter, sort);
                } else if (action.equals("open_my_listings")) {
                    sendMyListings(player, server);
                } else if (action.equals("open_sell_item")) {
                    handleOpenSellItem(player);
                } else if (action.equals("open_sell_pokemon")) {
                    handleOpenSellPokemon(player);
                } else if (action.startsWith("sell_item:")) {
                    handleSellItem(player, server, action);
                } else if (action.startsWith("sell_pokemon:")) {
                    handleSellPokemon(player, server, action);
                } else if (action.startsWith("buy:")) {
                    handleBuy(player, server, action.substring("buy:".length()));
                } else if (action.startsWith("cancel:")) {
                    handleCancel(player, server, action.substring("cancel:".length()));
                } else if (action.startsWith("claim:")) {
                    handleClaim(player, server, action.substring("claim:".length()));
                } else if (action.startsWith("ah_admin_remove:")) {
                    handleAdminRemove(player, server, action.substring("ah_admin_remove:".length()));
                } else {
                    player.sendMessage(Text.literal("§c[AH] Action inconnue : " + action), false);
                }
            } catch (Exception e) {
                player.sendMessage(Text.literal("§c[AH] Erreur interne : " + e.getMessage()), false);
                System.err.println("[Lenemon][AH] Erreur action '" + action + "' : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ── Browse ────────────────────────────────────────────────────────────────

    public static void sendBrowse(ServerPlayerEntity player, MinecraftServer server,
                                  int page, String filter, String sort) {
        AhWorldData data = AhWorldData.get(server.getOverworld());
        List<AhListing> active = data.getActiveListings();

        // Filtrage
        if ("item".equals(filter)) {
            active = active.stream().filter(l -> "item".equals(l.type)).collect(Collectors.toList());
        } else if ("pokemon".equals(filter)) {
            active = active.stream().filter(l -> "pokemon".equals(l.type)).collect(Collectors.toList());
        }

        // Tri
        if ("date_expires".equals(sort)) {
            active = active.stream()
                    .sorted(Comparator.comparingLong(l -> l.expiresAt))
                    .collect(Collectors.toList());
        } else if ("alpha".equals(sort)) {
            active = active.stream()
                    .sorted(Comparator.comparing(l -> {
                        if ("pokemon".equals(l.type)) return l.pokemonDisplayName != null ? l.pokemonDisplayName : "";
                        return l.itemDisplayName != null ? l.itemDisplayName : "";
                    }))
                    .collect(Collectors.toList());
        } else {
            // date_listed : du plus récent au plus ancien
            active = active.stream()
                    .sorted(Comparator.comparingLong((AhListing l) -> l.listedAt).reversed())
                    .collect(Collectors.toList());
        }

        int totalPages = Math.max(1, (active.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int from = page * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, active.size());
        List<AhListingDto> dtos = active.subList(from, to).stream()
                .map(l -> AhListingDto.from(l, player.getUuid()))
                .collect(Collectors.toList());

        long balance = EconomyHelper.getBalance(player);
        String safeFilter = filter != null ? filter : "all";
        String safeSort   = sort   != null ? sort   : "date_listed";
        ServerPlayNetworking.send(player,
                new AhBrowsePayload(dtos, page, totalPages, balance, safeFilter, safeSort));
    }

    // ── My Listings ───────────────────────────────────────────────────────────

    public static void sendMyListings(ServerPlayerEntity player, MinecraftServer server) {
        AhWorldData data = AhWorldData.get(server.getOverworld());
        List<AhListingDto> mine = data.getListingsBySeller(player.getUuid()).stream()
                .map(l -> AhListingDto.from(l, player.getUuid()))
                .collect(Collectors.toList());
        List<AhListingDto> recovery = data.getPendingRecovery(player.getUuid()).stream()
                .map(l -> AhListingDto.from(l, player.getUuid()))
                .collect(Collectors.toList());
        long balance = EconomyHelper.getBalance(player);
        ServerPlayNetworking.send(player, new AhMyListingsPayload(mine, recovery, balance, mine.size()));
    }

    // ── Open Sell Item ────────────────────────────────────────────────────────

    private static void handleOpenSellItem(ServerPlayerEntity player) {
        List<AhItemSlotDto> items = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                String id   = Registries.ITEM.getId(stack.getItem()).toString();
                String name = stack.getName().getString();
                items.add(new AhItemSlotDto(slot, id, name, stack.getCount(), true));
            }
        }
        ServerPlayNetworking.send(player, new AhSellItemPayload(items, 0));
    }

    // ── Open Sell Pokemon ─────────────────────────────────────────────────────

    private static void handleOpenSellPokemon(ServerPlayerEntity player) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        List<AhPartyPokemonDto> dtos = new ArrayList<>();
        int idx = 0;
        for (Pokemon pokemon : party) {
            if (pokemon == null) { idx++; continue; }

            String ivs = pokemon.getIvs().get(Stats.HP) + "/" +
                    pokemon.getIvs().get(Stats.ATTACK) + "/" +
                    pokemon.getIvs().get(Stats.DEFENCE) + "/" +
                    pokemon.getIvs().get(Stats.SPECIAL_ATTACK) + "/" +
                    pokemon.getIvs().get(Stats.SPECIAL_DEFENCE) + "/" +
                    pokemon.getIvs().get(Stats.SPEED);

            String evs = pokemon.getEvs().get(Stats.HP) + "/" +
                    pokemon.getEvs().get(Stats.ATTACK) + "/" +
                    pokemon.getEvs().get(Stats.DEFENCE) + "/" +
                    pokemon.getEvs().get(Stats.SPECIAL_ATTACK) + "/" +
                    pokemon.getEvs().get(Stats.SPECIAL_DEFENCE) + "/" +
                    pokemon.getEvs().get(Stats.SPEED);

            // Ability
            String ability = "";
            try { ability = pokemon.getAbility().getName().toString(); } catch (Exception ignored) {}

            // Types
            List<String> types = new ArrayList<>();
            try {
                types.add(pokemon.getSpecies().getPrimaryType().getName().toLowerCase());
                var secondary = pokemon.getSpecies().getSecondaryType();
                if (secondary != null) types.add(secondary.getName().toLowerCase());
            } catch (Exception ignored) {}

            // Moves
            List<String> moves = new ArrayList<>();
            try {
                for (var move : pokemon.getMoveSet()) {
                    if (move != null) moves.add(move.getTemplate().getName());
                }
            } catch (Exception ignored) {}

            // Ball
            String ball = "";
            try {
                ball = pokemon.getCaughtBall().getName().getPath();
            } catch (Exception ignored) {}

            // Breedable (via egg groups — UNDISCOVERED = non reproductible)
            boolean breedable = false;
            try {
                var eggGroups = pokemon.getSpecies().getEggGroups();
                breedable = eggGroups != null && !eggGroups.isEmpty() &&
                        eggGroups.stream().anyMatch(eg -> !eg.name().equalsIgnoreCase("undiscovered"));
            } catch (Exception ignored) {}

            // Friendship
            int friendship = 0;
            try { friendship = pokemon.getFriendship(); } catch (Exception ignored) {}

            dtos.add(new AhPartyPokemonDto(
                    idx,
                    pokemon.getSpecies().getName().toLowerCase(),
                    new ArrayList<>(pokemon.getAspects()),
                    pokemon.getDisplayName(false).getString(),
                    pokemon.getShiny(),
                    pokemon.getLevel(),
                    pokemon.getNature().getName().toString(),
                    ivs,
                    ability,
                    types,
                    evs,
                    moves,
                    ball,
                    breedable,
                    friendship
            ));
            idx++;
        }
        ServerPlayNetworking.send(player, new AhSellPokemonPayload(dtos));
    }

    // ── Sell Item ─────────────────────────────────────────────────────────────

    private static void handleSellItem(ServerPlayerEntity player, MinecraftServer server, String action) {
        // Format : "sell_item:<slot>:<price>:<durationHours>"
        String[] parts = action.split(":");
        if (parts.length < 4) {
            player.sendMessage(Text.literal("§c[AH] Format invalide."), false);
            return;
        }
        int slot;
        long price;
        int durationHours;
        try {
            slot          = Integer.parseInt(parts[1]);
            price         = Long.parseLong(parts[2]);
            durationHours = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(Text.literal("§c[AH] Paramètres invalides."), false);
            return;
        }

        if (durationHours > 72 || durationHours <= 0 || price <= 0) {
            player.sendMessage(Text.literal("§c[AH] Prix ou durée invalide."), false);
            return;
        }
        if (slot < 0 || slot >= 36) {
            player.sendMessage(Text.literal("§c[AH] Slot invalide."), false);
            return;
        }

        AhWorldData data = AhWorldData.get(server.getOverworld());
        int maxListings = getMaxListings(player);
        if (data.getListingsBySeller(player.getUuid()).size() >= maxListings) {
            player.sendMessage(Text.literal("§c[AH] Vous avez atteint la limite de " + maxListings + " ventes actives."), false);
            return;
        }

        ItemStack stack = player.getInventory().getStack(slot);
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("§c[AH] Ce slot est vide."), false);
            return;
        }

        // Sérialiser l'ItemStack via CODEC pour préserver les enchantements, DataComponents etc.
        NbtCompound itemCompound = new NbtCompound();
        ItemStack.CODEC.encodeStart(
                RegistryOps.of(NbtOps.INSTANCE, player.getRegistryManager()),
                stack
        ).result().ifPresent(nbt -> itemCompound.copyFrom((NbtCompound) nbt));

        AhListing listing = new AhListing();
        listing.listingId       = UUID.randomUUID();
        listing.sellerUuid      = player.getUuid();
        listing.sellerName      = player.getName().getString();
        listing.type            = "item";
        listing.itemId          = Registries.ITEM.getId(stack.getItem()).toString();
        listing.itemDisplayName = stack.getName().getString();
        listing.itemCount       = stack.getCount();
        listing.itemNbt         = itemCompound.isEmpty() ? null : itemCompound.asString();
        listing.price           = price;
        listing.listedAt        = System.currentTimeMillis();
        listing.expiresAt       = listing.listedAt + (long) durationHours * 3_600_000L;

        player.getInventory().setStack(slot, ItemStack.EMPTY);
        data.addListing(listing);
        AhDiscordNotifier.notifyNewListing(listing);
        sendMyListings(player, server);
    }

    // ── Sell Pokemon ──────────────────────────────────────────────────────────

    private static void handleSellPokemon(ServerPlayerEntity player, MinecraftServer server, String action) {
        // Format : "sell_pokemon:<partyIndex>:<price>:<durationHours>"
        String[] parts = action.split(":");
        if (parts.length < 4) {
            player.sendMessage(Text.literal("§c[AH] Format invalide."), false);
            return;
        }
        int partyIndex;
        long price;
        int durationHours;
        try {
            partyIndex    = Integer.parseInt(parts[1]);
            price         = Long.parseLong(parts[2]);
            durationHours = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(Text.literal("§c[AH] Paramètres invalides."), false);
            return;
        }

        if (durationHours > 72 || durationHours <= 0 || price <= 0) {
            player.sendMessage(Text.literal("§c[AH] Prix ou durée invalide."), false);
            return;
        }

        AhWorldData data = AhWorldData.get(server.getOverworld());
        int maxListings = getMaxListings(player);
        if (data.getListingsBySeller(player.getUuid()).size() >= maxListings) {
            player.sendMessage(Text.literal("§c[AH] Limite de " + maxListings + " ventes atteinte."), false);
            return;
        }

        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        List<Pokemon> partyList = new ArrayList<>();
        for (Pokemon p : party) partyList.add(p);

        if (partyIndex < 0 || partyIndex >= partyList.size() || partyList.get(partyIndex) == null) {
            player.sendMessage(Text.literal("§c[AH] Pokémon introuvable."), false);
            return;
        }

        Pokemon pokemon = partyList.get(partyIndex);
        party.remove(pokemon);

        NbtCompound nbt = pokemon.saveToNBT(player.getServerWorld().getRegistryManager(), new NbtCompound());

        // Construire les stats supplémentaires
        String ability = "";
        try { ability = pokemon.getAbility().getName().toString(); } catch (Exception ignored) {}

        List<String> types = new ArrayList<>();
        try {
            types.add(pokemon.getSpecies().getPrimaryType().getName().toLowerCase());
            var secondary = pokemon.getSpecies().getSecondaryType();
            if (secondary != null) types.add(secondary.getName().toLowerCase());
        } catch (Exception ignored) {}

        String evs = "";
        try {
            evs = pokemon.getEvs().get(Stats.HP) + "/" +
                    pokemon.getEvs().get(Stats.ATTACK) + "/" +
                    pokemon.getEvs().get(Stats.DEFENCE) + "/" +
                    pokemon.getEvs().get(Stats.SPECIAL_ATTACK) + "/" +
                    pokemon.getEvs().get(Stats.SPECIAL_DEFENCE) + "/" +
                    pokemon.getEvs().get(Stats.SPEED);
        } catch (Exception ignored) {}

        List<String> moves = new ArrayList<>();
        try {
            for (var move : pokemon.getMoveSet()) {
                if (move != null) moves.add(move.getTemplate().getName());
            }
        } catch (Exception ignored) {}

        String ball = "";
        try { ball = pokemon.getCaughtBall().getName().getPath(); } catch (Exception ignored) {}

        boolean breedable = false;
        try {
            var eggGroups = pokemon.getSpecies().getEggGroups();
            breedable = eggGroups != null && !eggGroups.isEmpty() &&
                    eggGroups.stream().anyMatch(eg -> !eg.name().equalsIgnoreCase("undiscovered"));
        } catch (Exception ignored) {}

        int friendship = 0;
        try { friendship = pokemon.getFriendship(); } catch (Exception ignored) {}

        AhListing listing = new AhListing();
        listing.listingId          = UUID.randomUUID();
        listing.sellerUuid         = player.getUuid();
        listing.sellerName         = player.getName().getString();
        listing.type               = "pokemon";
        listing.pokemonNbt         = nbt.asString();
        listing.pokemonSpecies     = pokemon.getSpecies().getName().toLowerCase();
        listing.pokemonAspects     = new HashSet<>(pokemon.getAspects());
        listing.pokemonDisplayName = pokemon.getDisplayName(false).getString()
                + " Niv." + pokemon.getLevel()
                + (pokemon.getShiny() ? " \u2726" : "");
        listing.pokemonShiny       = pokemon.getShiny();
        listing.pokemonLevel       = pokemon.getLevel();
        listing.pokemonNature      = pokemon.getNature().getName().toString();
        listing.pokemonAbility     = ability;
        listing.pokemonTypes       = types;
        listing.pokemonEvs         = evs;
        listing.pokemonMoves       = moves;
        listing.pokemonBall        = ball;
        listing.pokemonBreedable   = breedable;
        listing.pokemonFriendship  = friendship;
        listing.price              = price;
        listing.listedAt           = System.currentTimeMillis();
        listing.expiresAt          = listing.listedAt + (long) durationHours * 3_600_000L;

        data.addListing(listing);
        AhDiscordNotifier.notifyNewListing(listing);
        sendMyListings(player, server);
    }

    // ── Buy ───────────────────────────────────────────────────────────────────

    private static void handleBuy(ServerPlayerEntity player, MinecraftServer server, String listingIdStr) {
        UUID buyerUuid = player.getUuid();
        if (!EconomyHelper.tryLock(buyerUuid)) {
            player.sendMessage(Text.literal("§c[AH] Une transaction est déjà en cours."), false);
            return;
        }
        try {
            UUID listingId;
            try {
                listingId = UUID.fromString(listingIdStr);
            } catch (IllegalArgumentException e) {
                player.sendMessage(Text.literal("§c[AH] ID de vente invalide."), false);
                return;
            }

            AhWorldData data = AhWorldData.get(server.getOverworld());
            AhListing listing = data.findById(listingId);

            if (listing == null || listing.sold || listing.expired) {
                player.sendMessage(Text.literal("§c[AH] Cette vente n'est plus disponible."), false);
                sendBrowse(player, server, 0, "all", "date_listed");
                return;
            }
            if (listing.sellerUuid.equals(buyerUuid)) {
                player.sendMessage(Text.literal("§c[AH] Vous ne pouvez pas acheter votre propre vente."), false);
                return;
            }
            if (!EconomyHelper.debit(player, listing.price, "AH")) {
                player.sendMessage(Text.literal("§c[AH] Solde insuffisant (il vous faut §f" + listing.price + "₽§c)."), false);
                sendBrowse(player, server, 0, "all", "date_listed");
                return;
            }

            boolean giveOk = giveListingToPlayer(player, listing);
            if (!giveOk) {
                EconomyHelper.credit(buyerUuid, listing.price);
                player.sendMessage(Text.literal("§c[AH] Inventaire / PC plein, achat annulé."), false);
                return;
            }

            EconomyHelper.credit(listing.sellerUuid, listing.price);
            data.markSold(listing.listingId, buyerUuid, player.getName().getString());
            data.addToHistory(listing);

            ServerPlayerEntity seller = server.getPlayerManager().getPlayer(listing.sellerUuid);
            if (seller != null) {
                String itemName = "pokemon".equals(listing.type) ? listing.pokemonDisplayName : listing.itemDisplayName;
                seller.sendMessage(Text.literal(
                        "§a[AH] §f" + player.getName().getString()
                                + " §a a acheté votre §f" + itemName
                                + " §a pour §6" + listing.price + "₽§a."), false);
            }

            player.sendMessage(Text.literal("§a[AH] Achat réussi !"), false);
            sendBrowse(player, server, 0, "all", "date_listed");

        } finally {
            EconomyHelper.unlock(buyerUuid);
        }
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    private static void handleCancel(ServerPlayerEntity player, MinecraftServer server, String listingIdStr) {
        UUID listingId;
        try {
            listingId = UUID.fromString(listingIdStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("§c[AH] ID invalide."), false);
            return;
        }

        AhWorldData data = AhWorldData.get(server.getOverworld());
        AhListing listing = data.findById(listingId);

        if (listing == null) {
            player.sendMessage(Text.literal("§c[AH] Vente introuvable."), false);
            return;
        }
        if (!listing.sellerUuid.equals(player.getUuid())) {
            player.sendMessage(Text.literal("§c[AH] Ce n'est pas votre vente."), false);
            return;
        }
        if (listing.sold || listing.expired) {
            player.sendMessage(Text.literal("§c[AH] Cette vente est déjà terminée."), false);
            return;
        }

        data.expireListing(listing.listingId);
        player.sendMessage(Text.literal("§a[AH] Vente annulée. Récupérez votre objet via /ah mystuff."), false);
        sendMyListings(player, server);
    }

    // ── Claim ─────────────────────────────────────────────────────────────────

    private static void handleClaim(ServerPlayerEntity player, MinecraftServer server, String listingIdStr) {
        UUID listingId;
        try {
            listingId = UUID.fromString(listingIdStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("§c[AH] ID invalide."), false);
            return;
        }

        AhWorldData data = AhWorldData.get(server.getOverworld());
        List<AhListing> recovery = data.getPendingRecovery(player.getUuid());
        AhListing listing = null;
        for (AhListing l : recovery) {
            if (listingId.equals(l.listingId)) { listing = l; break; }
        }

        if (listing == null) {
            player.sendMessage(Text.literal("§c[AH] Objet introuvable dans vos récupérations."), false);
            return;
        }

        boolean ok = giveListingToPlayer(player, listing);
        if (!ok) {
            player.sendMessage(Text.literal("§c[AH] Inventaire / PC plein, impossible de récupérer."), false);
            return;
        }

        data.claimRecovery(listing.listingId);
        player.sendMessage(Text.literal("§a[AH] Objet récupéré !"), false);
        sendMyListings(player, server);
    }

    // ── Admin Remove ──────────────────────────────────────────────────────────

    private static void handleAdminRemove(ServerPlayerEntity player, MinecraftServer server, String listingIdStr) {
        if (!Permissions.check(player, "lenemon.ah.admin", 2)) {
            player.sendMessage(Text.literal("§c[AH] Permission refusée."), false);
            return;
        }

        UUID listingId;
        try {
            listingId = UUID.fromString(listingIdStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("§c[AH] ID invalide."), false);
            return;
        }

        AhWorldData data = AhWorldData.get(server.getOverworld());
        data.expireListing(listingId);
        player.sendMessage(Text.literal("§a[AH] Vente retirée par l'admin."), false);
        sendBrowse(player, server, 0, "all", "date_listed");
    }

    // ── Helper : donner l'objet / Pokémon au joueur ───────────────────────────

    static boolean giveListingToPlayer(ServerPlayerEntity player, AhListing listing) {
        if ("pokemon".equals(listing.type)) {
            return givePokemonToPlayer(player, listing);
        } else {
            return giveItemToPlayer(player, listing);
        }
    }

    private static boolean givePokemonToPlayer(ServerPlayerEntity player, AhListing listing) {
        try {
            NbtCompound nbt = StringNbtReader.parse(listing.pokemonNbt);
            Pokemon pokemon = Pokemon.Companion.loadFromNBT(
                    player.getServerWorld().getRegistryManager(), nbt);

            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (!CasinoPokemonStorage.isPartyFull(party)) {
                party.add(pokemon);
            } else {
                PCStore pc = Cobblemon.INSTANCE.getStorage()
                        .getPC(player.getUuid(), player.getServerWorld().getRegistryManager());
                if (pc == null) return false;
                pc.add(pokemon);
            }
            return true;
        } catch (Exception e) {
            System.err.println("[Lenemon][AH] Erreur chargement Pokémon : " + e.getMessage());
            return false;
        }
    }

    private static boolean giveItemToPlayer(ServerPlayerEntity player, AhListing listing) {
        try {
            ItemStack stack;
            if (listing.itemNbt != null && !listing.itemNbt.isEmpty()) {
                NbtCompound nbt = StringNbtReader.parse(listing.itemNbt);
                var result = ItemStack.fromNbt(player.getRegistryManager(), nbt);
                stack = result.orElseGet(() ->
                        new ItemStack(Registries.ITEM.get(Identifier.of(listing.itemId)), listing.itemCount));
            } else {
                stack = new ItemStack(
                        Registries.ITEM.get(Identifier.of(listing.itemId)), listing.itemCount);
            }

            if (player.getInventory().insertStack(stack)) return true;
            return stack.isEmpty();
        } catch (Exception e) {
            System.err.println("[Lenemon][AH] Erreur restauration item : " + e.getMessage());
            return false;
        }
    }
}
