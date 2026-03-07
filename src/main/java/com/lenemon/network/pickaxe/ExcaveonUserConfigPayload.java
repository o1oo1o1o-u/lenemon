package com.lenemon.network.pickaxe;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S payload — sent by the client when the player confirms their Excaveon config.
 * Fields: autoSell, autoSmelt, miningMode (one of "1x1", "3x3x1", "3x3x2", "3x3x3").
 */
public record ExcaveonUserConfigPayload(
        boolean autoSell,
        boolean autoSmelt,
        String  miningMode
) implements CustomPayload {

    public static final Id<ExcaveonUserConfigPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "excaveon_user_cfg"));

    public static final PacketCodec<RegistryByteBuf, ExcaveonUserConfigPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL,   ExcaveonUserConfigPayload::autoSell,
                    PacketCodecs.BOOL,   ExcaveonUserConfigPayload::autoSmelt,
                    PacketCodecs.STRING, ExcaveonUserConfigPayload::miningMode,
                    ExcaveonUserConfigPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
