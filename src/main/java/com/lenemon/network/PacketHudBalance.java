package com.lenemon.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Packet hud balance.
 */
public record PacketHudBalance(long balance) implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<PacketHudBalance> ID = new Id<>(Identifier.of("lenemon", "hud_balance"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<RegistryByteBuf, PacketHudBalance> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_LONG, PacketHudBalance::balance, PacketHudBalance::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}