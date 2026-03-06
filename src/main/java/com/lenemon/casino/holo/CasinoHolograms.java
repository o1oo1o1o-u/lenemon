package com.lenemon.casino.holo;

import com.lenemon.casino.CasinoWorldData;
import dev.furq.holodisplays.api.HoloDisplaysAPI;
import dev.furq.holodisplays.data.HologramData;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The type Casino holograms.
 */
public final class CasinoHolograms {
    private static final HoloDisplaysAPI api = HoloDisplaysAPI.get("custom-menu-o1");
    private CasinoHolograms() {}

    /**
     * Hologram id string.
     *
     * @param pos the pos
     * @return the string
     */
    public static String hologramId(BlockPos pos) {
        return "casino_holo_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }

    /**
     * Display id string.
     *
     * @param pos the pos
     * @return the string
     */
    public static String displayId(BlockPos pos) {
        return "casino_disp_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }

    private static void wipe(BlockPos pos) {
        String hId = hologramId(pos);
        String dId = displayId(pos);

        // Pas besoin des isRegistered, on tente et c'est tout
        api.unregisterHologram(hId);
        api.unregisterDisplay(dId);
    }

    /**
     * Recreate initial closed.
     *
     * @param world     the world
     * @param pos       the pos
     * @param ownerName the owner name
     */
    public static void recreateInitialClosed(ServerWorld world, BlockPos pos, String ownerName) {
        wipe(pos);

        String dId = displayId(pos);
        api.createTextDisplay(dId, b -> {
            b.text("§e" + ownerName, "§cCasino fermé");
            b.billboardMode("center");
            b.shadow(true);
            b.seeThrough(true);
            b.opacity(0.95f);
            b.scale(1.0f, 1.0f, 1.0f);
        });

        register(world, pos, dId, 1.8f);
    }

    /**
     * Recreate configured casino hologram.
     *
     * @param world         the world
     * @param pos           the pos
     * @param ownerName     the owner name
     * @param pokemonName   the pokemon name
     * @param price         the price
     * @param percentChance the percent chance
     */
    public static void recreateConfiguredCasinoHologram(
            ServerWorld world,
            BlockPos pos,
            String ownerName,
            String pokemonName,
            long price,
            double percentChance
    ) {
        wipe(pos);

        String dId = displayId(pos);
        api.createTextDisplay(dId, b -> {
            b.text(
                    "§e" + ownerName,
                    "§aCasino ouvert",
                    "§fPokémon : §6" + pokemonName,
                    "§fPrix : §e" + price + " PokéCoins",
                    "§fChance : §d" + percentChance + "%"
            );
            b.billboardMode("center");
            b.shadow(true);
            b.seeThrough(false);
            b.opacity(0.95f);
            b.scale(1.0f, 1.0f, 1.0f);
        });

        register(world, pos, dId, 2.5f);
    }

    /**
     * Recreate closed casino hologram.
     *
     * @param world         the world
     * @param pos           the pos
     * @param ownerName     the owner name
     * @param price         the price
     * @param percentChance the percent chance
     */
    public static void recreateClosedCasinoHologram(
            ServerWorld world,
            BlockPos pos,
            String ownerName,
            long price,
            double percentChance
    ) {
        wipe(pos);

        String dId = displayId(pos);
        api.createTextDisplay(dId, b -> {
            b.text(
                    "§e" + ownerName,
                    "§cCasino fermé",
                    "§fPrix : §e" + price + " PokéCoins",
                    "§fChance : §d" + percentChance + "%"
            );
            b.billboardMode("center");
            b.shadow(true);
            b.seeThrough(true);
            b.opacity(0.95f);
            b.scale(1.0f, 1.0f, 1.0f);
        });

        register(world, pos, dId, 1.8f);
    }

    private static void register(ServerWorld world, BlockPos pos, String dId, float yOffset) {
        String hId = hologramId(pos);
        String dimensionId = world.getRegistryKey().getValue().toString();

        HologramData hologram = api.createHologramBuilder()
                .world(dimensionId)
                .position(pos.getX() + 0.5f, pos.getY() + yOffset, pos.getZ() + 0.5f)
                .billboardMode("center")
                .updateRate(20)
                .viewRange(48.0)
                .addDisplay(dId, 0f, 0f, 0f)
                .build();

        api.registerHologram(hId, hologram);
    }

    /**
     * Remove casino hologram.
     *
     * @param pos the pos
     */
    public static void removeCasinoHologram(BlockPos pos) {
        removeCasinoHologramAround(pos);
    }

    /**
     * Purge all.
     */
    public static void purgeAll() {
        int holos = api.unregisterAllHolograms();
        int displays = api.unregisterAllDisplays();
//        Lenemon.LOGGER.info("[CasinoHolo] purgeAll holos={} displays={}", holos, displays);
    }

    /**
     * Resolve casino pos block pos.
     *
     * @param data       the data
     * @param clickedPos the clicked pos
     * @return the block pos
     */
    public static BlockPos resolveCasinoPos(CasinoWorldData data, BlockPos clickedPos) {
        // priorité: pos exacte enregistrée
        if (data.isRegisteredAt(clickedPos)) return clickedPos;

        // cas clique sur le haut
        BlockPos down = clickedPos.down();
        if (data.isRegisteredAt(down)) return down;

        // sécurité: si un jour tu reçois déjà le bas mais data est sur pos.up (rare)
        BlockPos up = clickedPos.up();
        if (data.isRegisteredAt(up)) return up;

        // fallback: on renvoie la pos cliquée
        return clickedPos;
    }

    /**
     * Wipe both heights.
     *
     * @param clickedPos the clicked pos
     */
    public static void wipeBothHeights(BlockPos clickedPos) {
        // supprime holo/display à la pos cliquée et à y-1 (cas PC 2 blocs)
        removeCasinoHologram(clickedPos);
        removeCasinoHologram(clickedPos.down());
    }

    /**
     * Remove casino hologram around.
     *
     * @param pos the pos
     */
    public static void removeCasinoHologramAround(BlockPos pos) {
        wipe(pos);
        wipe(pos.down());
        wipe(pos.up());
    }
}