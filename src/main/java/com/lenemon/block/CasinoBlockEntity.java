package com.lenemon.block;

import com.lenemon.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The type Casino block entity.
 */
public class CasinoBlockEntity extends BlockEntity {

    private UUID ownerUUID;
    private String ownerName = "";
    private long entryPrice = 0;
    private int winChance = 0;           // sur 10000
    private NbtCompound pokemonData = null;
    private String pokemonDisplayName = "";
    private CasinoState state = CasinoState.UNCONFIGURED;
    private boolean locked = false;
    private final Map<UUID, Integer> spinsPerPlayer = new HashMap<>();

    /**
     * Instantiates a new Casino block entity.
     *
     * @param pos   the pos
     * @param state the state
     */
    public CasinoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASINO_BLOCK_ENTITY, pos, state);
    }

    // ── Getters ──────────────────────────────────────────

    /**
     * Gets owner uuid.
     *
     * @return the owner uuid
     */
    public UUID getOwnerUUID()          { return ownerUUID; }

    /**
     * Gets owner name.
     *
     * @return the owner name
     */
    public String getOwnerName()        { return ownerName; }

    /**
     * Gets entry price.
     *
     * @return the entry price
     */
    public long getEntryPrice()         { return entryPrice; }

    /**
     * Gets win chance.
     *
     * @return the win chance
     */
    public int getWinChance()           { return winChance; }

    /**
     * Gets pokemon data.
     *
     * @return the pokemon data
     */
    public NbtCompound getPokemonData() { return pokemonData; }

    /**
     * Gets pokemon display name.
     *
     * @return the pokemon display name
     */
    public String getPokemonDisplayName() { return pokemonDisplayName; }

    /**
     * Gets casino state.
     *
     * @return the casino state
     */
    public CasinoState getCasinoState() { return state; }

    /**
     * Is locked boolean.
     *
     * @return the boolean
     */
    public boolean isLocked()           { return locked; }

    /**
     * Is owner boolean.
     *
     * @param uuid the uuid
     * @return the boolean
     */
    public boolean isOwner(UUID uuid)   { return ownerUUID != null && ownerUUID.equals(uuid); }

    // ── Setters ──────────────────────────────────────────

    /**
     * Sets owner.
     *
     * @param uuid the uuid
     * @param name the name
     */
    public void setOwner(UUID uuid, String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        markDirty();
    }

    /**
     * Sets entry price.
     *
     * @param price the price
     */
    public void setEntryPrice(long price) {
        this.entryPrice = price;
        markDirty();
    }

    /**
     * Sets win chance.
     *
     * @param chance the chance
     */
    public void setWinChance(int chance) {
        this.winChance = Math.min(10000, Math.max(0, chance));
        markDirty();
    }

    /**
     * Sets pokemon data.
     *
     * @param data        the data
     * @param displayName the display name
     */
    public void setPokemonData(NbtCompound data, String displayName) {
        this.pokemonData = data;
        this.pokemonDisplayName = displayName;
        markDirty();
    }

    /**
     * Sets state.
     *
     * @param state the state
     */
    public void setState(CasinoState state) {
        this.state = state;
        markDirty();
    }

    /**
     * Sets locked.
     *
     * @param locked the locked
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
        markDirty();
    }

    /**
     * Clear pokemon.
     */
    public void clearPokemon() {
        this.pokemonData = null;
        this.pokemonDisplayName = "";
        markDirty();
    }

    // ── NBT persistance ───────────────────────────────────

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);

        if (ownerUUID != null)
            nbt.putUuid("ownerUUID", ownerUUID);
        nbt.putString("ownerName", ownerName);
        nbt.putLong("entryPrice", entryPrice);
        nbt.putInt("winChance", winChance);
        nbt.putString("state", state.name());
        nbt.putBoolean("locked", locked);
        nbt.putString("pokemonDisplayName", pokemonDisplayName);

        if (pokemonData != null)
            nbt.put("pokemonData", pokemonData);

        // Sauvegarde spins par joueur
        NbtCompound spinsNbt = new NbtCompound();
        spinsPerPlayer.forEach((uuid, count) ->
                spinsNbt.putInt(uuid.toString(), count));
        nbt.put("spinsPerPlayer", spinsNbt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);

        if (nbt.containsUuid("ownerUUID"))
            ownerUUID = nbt.getUuid("ownerUUID");
        ownerName = nbt.getString("ownerName");
        entryPrice = nbt.getLong("entryPrice");
        winChance = nbt.getInt("winChance");
        locked = nbt.getBoolean("locked");
        pokemonDisplayName = nbt.getString("pokemonDisplayName");

        try {
            state = CasinoState.valueOf(nbt.getString("state"));
        } catch (IllegalArgumentException e) {
            state = CasinoState.UNCONFIGURED;
        }

        if (nbt.contains("pokemonData"))
            pokemonData = nbt.getCompound("pokemonData");

        NbtCompound spinsNbt = nbt.getCompound("spinsPerPlayer");
        spinsPerPlayer.clear();
        for (String key : spinsNbt.getKeys()) {
            try {
                spinsPerPlayer.put(UUID.fromString(key), spinsNbt.getInt(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }
}