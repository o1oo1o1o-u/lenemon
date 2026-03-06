package com.lenemon.network.ah;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload S2C : le serveur répond avec les deux prix moyens pour un itemId.
 */
public record AhPriceInfoPayload(long avgListedPrice, long avgSoldPrice) implements CustomPayload {

    public static final Id<AhPriceInfoPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "ah_price_info"));

    public static final PacketCodec<RegistryByteBuf, AhPriceInfoPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeLong(value.avgListedPrice());
                        buf.writeLong(value.avgSoldPrice());
                    },
                    buf -> new AhPriceInfoPayload(buf.readLong(), buf.readLong())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
