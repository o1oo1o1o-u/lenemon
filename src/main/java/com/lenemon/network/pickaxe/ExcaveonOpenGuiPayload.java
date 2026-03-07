package com.lenemon.network.pickaxe;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C payload — tells the client to open the Excaveon config GUI.
 * Carries the item's current state so the screen can display it correctly.
 */
public record ExcaveonOpenGuiPayload(
        int     level,
        int     blocks,
        boolean cfgAutoSell,
        boolean cfgAutoSmelt,
        String  cfgMiningMode
) implements CustomPayload {

    public static final Id<ExcaveonOpenGuiPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "excaveon_open_gui"));

    public static final PacketCodec<RegistryByteBuf, ExcaveonOpenGuiPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, ExcaveonOpenGuiPayload::level,
                    PacketCodecs.VAR_INT, ExcaveonOpenGuiPayload::blocks,
                    PacketCodecs.BOOL,    ExcaveonOpenGuiPayload::cfgAutoSell,
                    PacketCodecs.BOOL,    ExcaveonOpenGuiPayload::cfgAutoSmelt,
                    PacketCodecs.STRING,  ExcaveonOpenGuiPayload::cfgMiningMode,
                    ExcaveonOpenGuiPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
