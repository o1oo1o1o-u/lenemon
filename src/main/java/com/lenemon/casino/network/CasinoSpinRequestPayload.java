package com.lenemon.casino.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Casino spin request payload.
 */
public record CasinoSpinRequestPayload() implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<CasinoSpinRequestPayload> ID =
            new Id<>(Identifier.of("lenemon", "casino_spin_request"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<PacketByteBuf, CasinoSpinRequestPayload> CODEC =
            PacketCodec.unit(new CasinoSpinRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}