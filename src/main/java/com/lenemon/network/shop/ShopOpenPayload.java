package com.lenemon.network.shop;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload S2C : ouvre l'écran principal du shop avec la liste des catégories.
 */
public record ShopOpenPayload(List<ShopCategoryDto> categories) implements CustomPayload {

    public static final Id<ShopOpenPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "shop_open"));

    public static final PacketCodec<RegistryByteBuf, ShopOpenPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.categories().size());
                        for (ShopCategoryDto dto : value.categories()) {
                            ShopCategoryDto.PACKET_CODEC.encode(buf, dto);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<ShopCategoryDto> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            list.add(ShopCategoryDto.PACKET_CODEC.decode(buf));
                        }
                        return new ShopOpenPayload(list);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
