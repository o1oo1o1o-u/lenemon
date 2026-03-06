package com.lenemon.casino.network;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Casino anim done payload.
 */
public record CasinoAnimDonePayload() implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<CasinoAnimDonePayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "casino_anim_done"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<RegistryByteBuf, CasinoAnimDonePayload> CODEC =
            PacketCodec.unit(new CasinoAnimDonePayload());

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}