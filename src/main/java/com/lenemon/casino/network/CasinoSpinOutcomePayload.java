package com.lenemon.casino.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Casino spin outcome payload.
 */
public record CasinoSpinOutcomePayload(boolean win, int left, int right) implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<CasinoSpinOutcomePayload> ID =
            new Id<>(Identifier.of("lenemon", "casino_spin_outcome"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<PacketByteBuf, CasinoSpinOutcomePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeBoolean(value.win());
                        buf.writeVarInt(value.left());
                        buf.writeVarInt(value.right());
                    },
                    buf -> new CasinoSpinOutcomePayload(
                            buf.readBoolean(),
                            buf.readVarInt(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}