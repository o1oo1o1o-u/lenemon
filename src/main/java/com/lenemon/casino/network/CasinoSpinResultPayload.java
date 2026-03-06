package com.lenemon.casino.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Casino spin result payload.
 */
public record CasinoSpinResultPayload(boolean win, String message) implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<CasinoSpinResultPayload> ID =
            new Id<>(Identifier.of("lenemon", "casino_spin_result"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<RegistryByteBuf, CasinoSpinResultPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL,   CasinoSpinResultPayload::win,
                    PacketCodecs.STRING, CasinoSpinResultPayload::message,
                    CasinoSpinResultPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}