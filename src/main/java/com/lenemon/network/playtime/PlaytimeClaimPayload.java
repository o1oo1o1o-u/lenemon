package com.lenemon.network.playtime;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlaytimeClaimPayload(String tierId) implements CustomPayload {

    public static final Id<PlaytimeClaimPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "playtime_claim"));

    public static final PacketCodec<RegistryByteBuf, PlaytimeClaimPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, PlaytimeClaimPayload::tierId, PlaytimeClaimPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
