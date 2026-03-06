package com.lenemon.network.ah;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload C2S : le client demande les prix moyens pour un itemId donné.
 */
public record AhRequestPricePayload(String itemId) implements CustomPayload {

    public static final Id<AhRequestPricePayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "ah_request_price"));

    public static final PacketCodec<RegistryByteBuf, AhRequestPricePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeString(value.itemId()),
                    buf -> new AhRequestPricePayload(buf.readString())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
