package com.lenemon.network.ah;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload C2S : action générique envoyée par le client vers le serveur pour l'AH.
 */
public record AhActionPayload(String action) implements CustomPayload {

    public static final Id<AhActionPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "ah_action"));

    public static final PacketCodec<RegistryByteBuf, AhActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, AhActionPayload::action,
                    AhActionPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
