package com.lenemon.casino.network;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Casino can spin request payload.
 */
public record CasinoCanSpinRequestPayload() implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<CasinoCanSpinRequestPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "casino_can_spin_request"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<RegistryByteBuf, CasinoCanSpinRequestPayload> CODEC =
            PacketCodec.unit(new CasinoCanSpinRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}