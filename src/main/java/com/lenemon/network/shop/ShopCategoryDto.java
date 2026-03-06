package com.lenemon.network.shop;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * DTO sérialisable représentant une catégorie du shop pour le client.
 */
public record ShopCategoryDto(String name, String iconItemId, int itemCount) {

    public static final PacketCodec<RegistryByteBuf, ShopCategoryDto> PACKET_CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.name());
                        buf.writeString(value.iconItemId());
                        buf.writeVarInt(value.itemCount());
                    },
                    buf -> new ShopCategoryDto(
                            buf.readString(),
                            buf.readString(),
                            buf.readVarInt()
                    )
            );
}
