package com.lenemon.network.shop;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * DTO sérialisable représentant un item du shop pour le client.
 */
public record ShopItemDto(String id, String displayName, double buyPrice, double sellPrice) {

    public static final PacketCodec<RegistryByteBuf, ShopItemDto> PACKET_CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.id());
                        buf.writeString(value.displayName());
                        buf.writeDouble(value.buyPrice());
                        buf.writeDouble(value.sellPrice());
                    },
                    buf -> new ShopItemDto(
                            buf.readString(),
                            buf.readString(),
                            buf.readDouble(),
                            buf.readDouble()
                    )
            );
}
