package com.lenemon.casino;

import com.lenemon.block.CasinoState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.*;

/**
 * The type Casino world data.
 */
public class CasinoWorldData extends PersistentState {

    private static final String KEY = "custommenu_casinos";
    private final Map<BlockPos, CasinoData> casinos = new HashMap<>();

    /**
     * Gets casinos view.
     *
     * @return the casinos view
     */
    public Map<BlockPos, CasinoData> getCasinosView() {
        return Collections.unmodifiableMap(casinos);
    }

    /**
     * The type Casino data.
     */
    public static class CasinoData {
        /**
         * The Owner uuid.
         */
        public UUID ownerUUID;
        /**
         * The Owner name.
         */
        public String ownerName;
        /**
         * The Entry price.
         */
        public long entryPrice;
        /**
         * The Win chance.
         */
        public int winChance;
        /**
         * The Casino uuid.
         */
        public UUID casinoUUID;
        /**
         * The Pokemon display name.
         */
        public String pokemonDisplayName = "";
        /**
         * The State.
         */
        public CasinoState state = CasinoState.UNCONFIGURED;
        /**
         * The Locked.
         */
        public boolean locked = false;
        /**
         * The Pokemon nature.
         */
        public String pokemonNature = "";
        /**
         * The Pokemon i vs.
         */
        public String pokemonIVs = "";
        /**
         * The Pokemon species.
         */
        public String pokemonSpecies = "";
        /**
         * The Pokemon aspects.
         */
        public Set<String> pokemonAspects = new java.util.HashSet<>();
    }


    /**
     * Is casino boolean.
     *
     * @param pos the pos
     * @return the boolean
     */
    public boolean isCasino(BlockPos pos) {
        return casinos.containsKey(pos) || casinos.containsKey(pos.down());
    }

    /**
     * Gets casino.
     *
     * @param pos the pos
     * @return the casino
     */
    public CasinoData getCasino(BlockPos pos) {
        if (casinos.containsKey(pos)) return casinos.get(pos);
        return casinos.get(pos.down());
    }

    /**
     * Register casino.
     *
     * @param bottomPos the bottom pos
     * @param ownerUUID the owner uuid
     * @param ownerName the owner name
     */
    public void registerCasino(BlockPos bottomPos, UUID ownerUUID, String ownerName) {
        CasinoData data = new CasinoData();
        data.ownerUUID = ownerUUID;
        data.ownerName = ownerName;
        data.casinoUUID = UUID.randomUUID();
        casinos.put(bottomPos, data);
        markDirty();
    }

    /**
     * Remove casino.
     *
     * @param pos the pos
     */
    public void removeCasino(BlockPos pos) {
        casinos.remove(pos);
        casinos.remove(pos.down());
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt,
                                net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        casinos.forEach((pos, data) -> {
            NbtCompound entry = new NbtCompound();
            entry.putInt("x", pos.getX());
            entry.putInt("y", pos.getY());
            entry.putInt("z", pos.getZ());
            entry.putUuid("ownerUUID", data.ownerUUID);
            entry.putString("ownerName", data.ownerName);
            entry.putLong("entryPrice", data.entryPrice);
            entry.putInt("winChance", data.winChance);
            entry.putUuid("casinoUUID", data.casinoUUID);
            entry.putString("state", data.state.name());
            entry.putBoolean("locked", false);
            entry.putString("pokemonDisplayName", data.pokemonDisplayName);
            entry.putString("pokemonNature", data.pokemonNature);
            entry.putString("pokemonIVs", data.pokemonIVs);
            entry.putString("pokemonSpecies", data.pokemonSpecies);
            NbtList aspectsList = new NbtList();
            for (String aspect : data.pokemonAspects) {
                aspectsList.add(net.minecraft.nbt.NbtString.of(aspect));
            }
            entry.put("pokemonAspects", aspectsList);
            list.add(entry);
        });
        nbt.put("casinos", list);
        return nbt;
    }

    /**
     * Read nbt casino world data.
     *
     * @param nbt        the nbt
     * @param registries the registries
     * @return the casino world data
     */
    public static CasinoWorldData readNbt(NbtCompound nbt,
                                          net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        CasinoWorldData data = new CasinoWorldData();
        NbtList list = nbt.getList("casinos", 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            BlockPos pos = new BlockPos(
                    entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            CasinoData casino = new CasinoData();
            casino.ownerUUID = entry.getUuid("ownerUUID");
            casino.ownerName = entry.getString("ownerName");
            casino.entryPrice = entry.getLong("entryPrice");
            casino.winChance = entry.getInt("winChance");
            casino.casinoUUID = entry.getUuid("casinoUUID");
            casino.locked = false;
            casino.pokemonDisplayName = entry.getString("pokemonDisplayName");
            casino.pokemonNature = entry.getString("pokemonNature");
            casino.pokemonIVs = entry.getString("pokemonIVs");
            casino.pokemonSpecies = entry.getString("pokemonSpecies");
            NbtList aspectsList = entry.getList("pokemonAspects", 8); // 8 = NbtString
            for (int j = 0; j < aspectsList.size(); j++) {
                casino.pokemonAspects.add(aspectsList.getString(j));
            }
            try {
                casino.state = CasinoState.valueOf(entry.getString("state"));
                if (casino.state == CasinoState.LOCKED) {
                    casino.state = CasinoState.ACTIVE;
                }
            } catch (IllegalArgumentException e) {
                casino.state = CasinoState.UNCONFIGURED;
            }
            data.casinos.put(pos, casino);
        }
        return data;
    }

    /**
     * Get casino world data.
     *
     * @param world the world
     * @return the casino world data
     */
    public static CasinoWorldData get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(
                new Type<>(CasinoWorldData::new, CasinoWorldData::readNbt, null),
                KEY
        );
    }

    /**
     * Is registered at boolean.
     *
     * @param pos the pos
     * @return the boolean
     */
    public boolean isRegisteredAt(BlockPos pos) {
        return casinos.containsKey(pos);
    }

    /**
     * Gets pos by casino uuid.
     *
     * @param uuid the uuid
     * @return the pos by casino uuid
     */
    public BlockPos getPosByCasinoUUID(UUID uuid) {
        for (Map.Entry<BlockPos, CasinoData> entry : casinos.entrySet()) {
            if (entry.getValue().casinoUUID.equals(uuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gets casino by uuid.
     *
     * @param casinoUuid the casino uuid
     * @return the casino by uuid
     */
    public CasinoData getCasinoByUUID(UUID casinoUuid) {
        // si tu as une map pos -> casinoData:
        for (CasinoData c : this.casinos.values()) {
            if (c != null && casinoUuid.equals(c.casinoUUID)) return c;
        }
        return null;
    }
}