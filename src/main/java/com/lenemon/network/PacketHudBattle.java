package com.lenemon.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PacketHudBattle(boolean inBattle) implements CustomPayload {

    public static final Id<PacketHudBattle> ID =
            new Id<>(Identifier.of("lenemon", "hud_battle"));

    public static final PacketCodec<PacketByteBuf, PacketHudBattle> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeBoolean(value.inBattle()),
                    buf -> new PacketHudBattle(buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
