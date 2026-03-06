package com.lenemon.casino.screen;

import com.lenemon.casino.CasinoWorldData;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * The type Casino screen opener.
 */
public class CasinoScreenOpener {

    /**
     * Open.
     *
     * @param player the player
     * @param casino the casino
     * @param data   the data
     * @param pos    the pos
     */
    public static void open(ServerPlayerEntity player,
                            CasinoWorldData.CasinoData casino,
                            CasinoWorldData data,
                            BlockPos pos) {

        // Ici on n'enclenche rien: pas de lock, pas de debit, pas de RNG.
        // "win" placeholder: il faudra le definir au moment du spin
        boolean winPlaceholder = false;

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new CasinoScreenHandler(syncId, inv, casino, data, pos),
                Text.literal("Casino")
        ));
    }
}