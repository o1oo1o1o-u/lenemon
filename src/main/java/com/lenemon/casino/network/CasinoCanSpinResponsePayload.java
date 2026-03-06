package com.lenemon.casino.network;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Casino can spin response payload.
 */
public record CasinoCanSpinResponsePayload(
        boolean allowed,
        long price,
        long balance,
        boolean locked
) implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<CasinoCanSpinResponsePayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "casino_can_spin_response"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<RegistryByteBuf, CasinoCanSpinResponsePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL, CasinoCanSpinResponsePayload::allowed,
                    PacketCodecs.VAR_LONG, CasinoCanSpinResponsePayload::price,
                    PacketCodecs.VAR_LONG, CasinoCanSpinResponsePayload::balance,
                    PacketCodecs.BOOL, CasinoCanSpinResponsePayload::locked,
                    CasinoCanSpinResponsePayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}