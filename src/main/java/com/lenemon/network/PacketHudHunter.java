package com.lenemon.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Packet hud hunter.
 */
public record PacketHudHunter(int level, float progress) implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<PacketHudHunter> ID = new Id<>(Identifier.of("lenemon", "hud_hunter"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<RegistryByteBuf, PacketHudHunter> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, PacketHudHunter::level,
                    PacketCodecs.FLOAT,   PacketHudHunter::progress,
                    PacketHudHunter::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}