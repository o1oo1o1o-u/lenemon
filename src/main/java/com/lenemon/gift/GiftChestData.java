package com.lenemon.gift;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The type Gift chest data.
 */
public class GiftChestData extends PersistentState {

    private static final String KEY = "custommenu_giftchests";
    private final Map<BlockPos, ChestEntry> chests = new HashMap<>();

    /**
     * The type Chest entry.
     */
    public static class ChestEntry {
        /**
         * The Chest uuid.
         */
        public UUID chestUUID;
        /**
         * The Chest name.
         */
        public String chestName;
        /**
         * The Owner name.
         */
        public String ownerName;
        /**
         * The Owner uuid.
         */
        public UUID ownerUUID;
        /**
         * The Block id.
         */
        public String blockId = "cobblemon:yellow_gilded_chest";
    }

    /**
     * Is gift chest boolean.
     *
     * @param pos the pos
     * @return the boolean
     */
    public boolean isGiftChest(BlockPos pos) {
        return chests.containsKey(pos) || chests.containsKey(pos.down());
    }

    /**
     * Is registered at boolean.
     *
     * @param pos the pos
     * @return the boolean
     */
    public boolean isRegisteredAt(BlockPos pos) {
        return chests.containsKey(pos);
    }

    /**
     * Gets chest.
     *
     * @param pos the pos
     * @return the chest
     */
    public ChestEntry getChest(BlockPos pos) {
        if (chests.containsKey(pos)) return chests.get(pos);
        return chests.get(pos.down());
    }

    /**
     * Gets all.
     *
     * @return the all
     */
    public Map<BlockPos, ChestEntry> getAll() {
        return chests;
    }

    /**
     * Register chest.
     *
     * @param bottomPos the bottom pos
     * @param ownerUUID the owner uuid
     * @param ownerName the owner name
     * @param chestName the chest name
     * @param blockId   the block id
     */
    public void registerChest(BlockPos bottomPos, UUID ownerUUID, String ownerName,
                              String chestName, String blockId) {
        ChestEntry entry = new ChestEntry();
        entry.chestUUID = UUID.randomUUID();
        entry.chestName = chestName;
        entry.ownerUUID = ownerUUID;
        entry.ownerName = ownerName;
        entry.blockId = blockId;
        chests.put(bottomPos, entry);
        markDirty();
    }

    /**
     * Remove chest.
     *
     * @param pos the pos
     */
    public void removeChest(BlockPos pos) {
        chests.remove(pos);
        chests.remove(pos.down());
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt,
                                net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        chests.forEach((pos, entry) -> {
            NbtCompound e = new NbtCompound();
            e.putInt("x", pos.getX());
            e.putInt("y", pos.getY());
            e.putInt("z", pos.getZ());
            e.putUuid("chestUUID", entry.chestUUID);
            e.putString("chestName", entry.chestName);
            e.putUuid("ownerUUID", entry.ownerUUID);
            e.putString("ownerName", entry.ownerName);
            e.putString("blockId", entry.blockId);
            list.add(e);
        });
        nbt.put("chests", list);
        return nbt;
    }

    /**
     * Read nbt gift chest data.
     *
     * @param nbt        the nbt
     * @param registries the registries
     * @return the gift chest data
     */
    public static GiftChestData readNbt(NbtCompound nbt,
                                        net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        GiftChestData data = new GiftChestData();
        NbtList list = nbt.getList("chests", 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);
            BlockPos pos = new BlockPos(e.getInt("x"), e.getInt("y"), e.getInt("z"));
            ChestEntry entry = new ChestEntry();
            entry.chestUUID = e.getUuid("chestUUID");
            entry.chestName = e.getString("chestName");
            entry.ownerUUID = e.getUuid("ownerUUID");
            entry.ownerName = e.getString("ownerName");
            entry.blockId = e.contains("blockId") ? e.getString("blockId") : "cobblemon:yellow_gilded_chest";
            data.chests.put(pos, entry);
        }
        return data;
    }

    /**
     * Get gift chest data.
     *
     * @param world the world
     * @return the gift chest data
     */
    public static GiftChestData get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(
                new Type<>(GiftChestData::new, GiftChestData::readNbt, null),
                KEY
        );
    }
}