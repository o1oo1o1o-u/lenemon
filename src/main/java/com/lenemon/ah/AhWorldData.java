package com.lenemon.ah;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Données persistantes de l'Hôtel des Ventes, stockées via PersistentState.
 */
public class AhWorldData extends PersistentState {

    private static final String KEY = "lenemon_ah_data";
    private static final int MAX_HISTORY = 500;

    public List<AhListing> activeListings    = new ArrayList<>();
    public List<AhListing> pendingRecovery   = new ArrayList<>();
    public List<AhListing> salesHistory      = new ArrayList<>();

    // ── Écriture NBT ──────────────────────────────────────────────────────────

    @Override
    public NbtCompound writeNbt(NbtCompound nbt,
                                net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        nbt.put("active",   serializeList(activeListings));
        nbt.put("recovery", serializeList(pendingRecovery));
        nbt.put("history",  serializeList(salesHistory));
        return nbt;
    }

    private static NbtList serializeList(List<AhListing> list) {
        NbtList nbtList = new NbtList();
        for (AhListing l : list) {
            nbtList.add(serializeListing(l));
        }
        return nbtList;
    }

    private static NbtCompound serializeListing(AhListing l) {
        NbtCompound c = new NbtCompound();
        c.putUuid("listingId",    l.listingId);
        c.putUuid("sellerUuid",   l.sellerUuid);
        c.putString("sellerName", l.sellerName != null ? l.sellerName : "");
        c.putString("type",       l.type != null ? l.type : "item");

        // Item
        if (l.itemId != null)          c.putString("itemId",          l.itemId);
        if (l.itemDisplayName != null) c.putString("itemDisplayName", l.itemDisplayName);
        c.putInt("itemCount", l.itemCount);
        if (l.itemNbt != null)         c.putString("itemNbt",         l.itemNbt);

        // Pokémon
        if (l.pokemonNbt != null)         c.putString("pokemonNbt",         l.pokemonNbt);
        if (l.pokemonSpecies != null)     c.putString("pokemonSpecies",     l.pokemonSpecies);
        if (l.pokemonDisplayName != null) c.putString("pokemonDisplayName", l.pokemonDisplayName);
        c.putBoolean("pokemonShiny", l.pokemonShiny);
        c.putInt("pokemonLevel", l.pokemonLevel);

        // pokemonAspects → NbtList de NbtString
        NbtList aspects = new NbtList();
        if (l.pokemonAspects != null) {
            for (String a : l.pokemonAspects) {
                aspects.add(NbtString.of(a));
            }
        }
        c.put("pokemonAspects", aspects);

        // Pokémon stats supplémentaires
        if (l.pokemonNature != null)   c.putString("pokemonNature",   l.pokemonNature);
        if (l.pokemonAbility != null)  c.putString("pokemonAbility",  l.pokemonAbility);
        NbtList types = new NbtList();
        if (l.pokemonTypes != null) for (String t : l.pokemonTypes) types.add(NbtString.of(t));
        c.put("pokemonTypes", types);
        if (l.pokemonEvs != null)      c.putString("pokemonEvs",      l.pokemonEvs);
        NbtList moves = new NbtList();
        if (l.pokemonMoves != null) for (String m : l.pokemonMoves) moves.add(NbtString.of(m));
        c.put("pokemonMoves", moves);
        if (l.pokemonBall != null)     c.putString("pokemonBall",     l.pokemonBall);
        c.putBoolean("pokemonBreedable",  l.pokemonBreedable);
        c.putInt("pokemonFriendship",     l.pokemonFriendship);

        // Commun
        c.putLong("price",     l.price);
        c.putLong("listedAt",  l.listedAt);
        c.putLong("expiresAt", l.expiresAt);
        c.putBoolean("sold",    l.sold);
        c.putBoolean("expired", l.expired);

        if (l.buyerUuid != null)   c.putUuid("buyerUuid", l.buyerUuid);
        if (l.buyerName != null)   c.putString("buyerName", l.buyerName);

        return c;
    }

    // ── Lecture NBT ───────────────────────────────────────────────────────────

    public static AhWorldData readNbt(NbtCompound nbt,
                                      net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        AhWorldData data = new AhWorldData();
        data.activeListings  = deserializeList(nbt.getList("active",   10));
        data.pendingRecovery = deserializeList(nbt.getList("recovery", 10));
        data.salesHistory    = deserializeList(nbt.getList("history",  10));
        return data;
    }

    private static List<AhListing> deserializeList(NbtList nbtList) {
        List<AhListing> list = new ArrayList<>();
        for (int i = 0; i < nbtList.size(); i++) {
            NbtCompound c = nbtList.getCompound(i);
            list.add(deserializeListing(c));
        }
        return list;
    }

    private static AhListing deserializeListing(NbtCompound c) {
        AhListing l = new AhListing();
        l.listingId  = c.getUuid("listingId");
        l.sellerUuid = c.getUuid("sellerUuid");
        l.sellerName = c.getString("sellerName");
        l.type       = c.getString("type");

        if (c.contains("itemId"))          l.itemId          = c.getString("itemId");
        if (c.contains("itemDisplayName")) l.itemDisplayName = c.getString("itemDisplayName");
        l.itemCount = c.getInt("itemCount");
        if (c.contains("itemNbt"))         l.itemNbt         = c.getString("itemNbt");

        if (c.contains("pokemonNbt"))         l.pokemonNbt         = c.getString("pokemonNbt");
        if (c.contains("pokemonSpecies"))     l.pokemonSpecies     = c.getString("pokemonSpecies");
        if (c.contains("pokemonDisplayName")) l.pokemonDisplayName = c.getString("pokemonDisplayName");
        l.pokemonShiny = c.getBoolean("pokemonShiny");
        l.pokemonLevel = c.getInt("pokemonLevel");

        Set<String> aspects = new HashSet<>();
        NbtList nbtAspects = c.getList("pokemonAspects", 8); // 8 = NbtString type id
        for (int i = 0; i < nbtAspects.size(); i++) {
            aspects.add(nbtAspects.getString(i));
        }
        l.pokemonAspects = aspects;

        // Pokémon stats supplémentaires (avec guards pour compatibilité anciens listings)
        if (c.contains("pokemonNature"))  l.pokemonNature  = c.getString("pokemonNature");
        if (c.contains("pokemonAbility")) l.pokemonAbility = c.getString("pokemonAbility");
        NbtList typesNbt = c.getList("pokemonTypes", 8);
        l.pokemonTypes = new java.util.ArrayList<>();
        for (int i = 0; i < typesNbt.size(); i++) l.pokemonTypes.add(typesNbt.getString(i));
        if (c.contains("pokemonEvs"))     l.pokemonEvs     = c.getString("pokemonEvs");
        NbtList movesNbt = c.getList("pokemonMoves", 8);
        l.pokemonMoves = new java.util.ArrayList<>();
        for (int i = 0; i < movesNbt.size(); i++) l.pokemonMoves.add(movesNbt.getString(i));
        if (c.contains("pokemonBall"))       l.pokemonBall       = c.getString("pokemonBall");
        if (c.contains("pokemonBreedable"))  l.pokemonBreedable  = c.getBoolean("pokemonBreedable");
        if (c.contains("pokemonFriendship")) l.pokemonFriendship = c.getInt("pokemonFriendship");

        l.price     = c.getLong("price");
        l.listedAt  = c.getLong("listedAt");
        l.expiresAt = c.getLong("expiresAt");
        l.sold      = c.getBoolean("sold");
        l.expired   = c.getBoolean("expired");

        if (c.contains("buyerUuid")) l.buyerUuid = c.getUuid("buyerUuid");
        if (c.contains("buyerName")) l.buyerName = c.getString("buyerName");

        return l;
    }

    // ── Méthodes métier ───────────────────────────────────────────────────────

    /** Ajoute une vente active. */
    public void addListing(AhListing listing) {
        activeListings.add(listing);
        markDirty();
    }

    /** Retire une vente de la liste active par son ID. */
    public void removeListing(UUID listingId) {
        activeListings.removeIf(l -> listingId.equals(l.listingId));
        markDirty();
    }

    /** Cherche dans activeListings + pendingRecovery. */
    public AhListing findById(UUID listingId) {
        for (AhListing l : activeListings) {
            if (listingId.equals(l.listingId)) return l;
        }
        for (AhListing l : pendingRecovery) {
            if (listingId.equals(l.listingId)) return l;
        }
        return null;
    }

    /** Ventes actives non expirées, non vendues. */
    public List<AhListing> getActiveListings() {
        return activeListings.stream()
                .filter(l -> !l.sold && !l.expired)
                .collect(Collectors.toList());
    }

    /** Ventes actives d'un vendeur donné. */
    public List<AhListing> getListingsBySeller(UUID sellerUuid) {
        return activeListings.stream()
                .filter(l -> sellerUuid.equals(l.sellerUuid))
                .collect(Collectors.toList());
    }

    /** Objets en attente de récupération pour un joueur. */
    public List<AhListing> getPendingRecovery(UUID sellerUuid) {
        return pendingRecovery.stream()
                .filter(l -> sellerUuid.equals(l.sellerUuid))
                .collect(Collectors.toList());
    }

    /**
     * Expire une vente : retire de activeListings, ajoute à pendingRecovery.
     */
    public void expireListing(UUID listingId) {
        AhListing found = null;
        for (AhListing l : activeListings) {
            if (listingId.equals(l.listingId)) { found = l; break; }
        }
        if (found == null) return;
        found.expired = true;
        activeListings.remove(found);
        pendingRecovery.add(found);
        markDirty();
    }

    /**
     * Marque une vente comme vendue et la retire de activeListings.
     */
    public void markSold(UUID listingId, UUID buyerUuid, String buyerName) {
        for (AhListing l : activeListings) {
            if (listingId.equals(l.listingId)) {
                l.sold      = true;
                l.buyerUuid = buyerUuid;
                l.buyerName = buyerName;
                activeListings.remove(l);
                markDirty();
                return;
            }
        }
    }

    /**
     * Ajoute une entrée à l'historique (max 500 entrées).
     */
    public void addToHistory(AhListing listing) {
        if (salesHistory.size() >= MAX_HISTORY) {
            salesHistory.remove(0);
        }
        salesHistory.add(listing);
        markDirty();
    }

    /** Retire un listing de pendingRecovery après réclamation. */
    public void claimRecovery(UUID listingId) {
        pendingRecovery.removeIf(l -> listingId.equals(l.listingId));
        markDirty();
    }

    /** Prix moyen listé par unité pour un itemId donné (0 si aucune vente active). */
    public long getAverageListedPrice(String itemId) {
        List<Long> prices = activeListings.stream()
                .filter(l -> "item".equals(l.type) && itemId.equals(l.itemId) && !l.sold && !l.expired)
                .map(l -> l.price / Math.max(1, l.itemCount))
                .collect(Collectors.toList());
        if (prices.isEmpty()) return 0L;
        return prices.stream().mapToLong(Long::longValue).sum() / prices.size();
    }

    /** Prix moyen vendu par unité pour un itemId donné (0 si aucun historique). */
    public long getAverageSoldPrice(String itemId) {
        List<Long> prices = salesHistory.stream()
                .filter(l -> l.sold && "item".equals(l.type) && itemId.equals(l.itemId))
                .map(l -> l.price / Math.max(1, l.itemCount))
                .collect(Collectors.toList());
        if (prices.isEmpty()) return 0L;
        return prices.stream().mapToLong(Long::longValue).sum() / prices.size();
    }

    /** Prix moyen listé pour un Pokémon donné (0 si aucune vente active). */
    public long getAveragePokemonListedPrice(String species, boolean shiny) {
        List<Long> prices = activeListings.stream()
                .filter(l -> "pokemon".equals(l.type) && species.equalsIgnoreCase(l.pokemonSpecies)
                        && l.pokemonShiny == shiny && !l.sold && !l.expired)
                .map(l -> l.price)
                .collect(Collectors.toList());
        if (prices.isEmpty()) return 0L;
        return prices.stream().mapToLong(Long::longValue).sum() / prices.size();
    }

    /** Prix moyen vendu pour un Pokémon donné (0 si aucun historique). */
    public long getAveragePokemonSoldPrice(String species, boolean shiny) {
        List<Long> prices = salesHistory.stream()
                .filter(l -> "pokemon".equals(l.type) && species.equalsIgnoreCase(l.pokemonSpecies)
                        && l.pokemonShiny == shiny && l.sold)
                .map(l -> l.price)
                .collect(Collectors.toList());
        if (prices.isEmpty()) return 0L;
        return prices.stream().mapToLong(Long::longValue).sum() / prices.size();
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static AhWorldData get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(
                new Type<>(AhWorldData::new, AhWorldData::readNbt, null),
                KEY
        );
    }
}
