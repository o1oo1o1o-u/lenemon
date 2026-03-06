package com.lenemon.network.shop;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload S2C : ouvre l'écran d'une catégorie du shop avec ses items paginés.
 */
public record ShopCategoryOpenPayload(
        String categoryName,
        int categoryIndex,
        List<ShopItemDto> items,
        long balance,
        int page,
        int totalPages
) implements CustomPayload {

    public static final Id<ShopCategoryOpenPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "shop_category_open"));

    public static final PacketCodec<RegistryByteBuf, ShopCategoryOpenPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.categoryName());
                        buf.writeVarInt(value.categoryIndex());
                        buf.writeVarInt(value.items().size());
                        for (ShopItemDto dto : value.items()) {
                            ShopItemDto.PACKET_CODEC.encode(buf, dto);
                        }
                        buf.writeLong(value.balance());
                        buf.writeVarInt(value.page());
                        buf.writeVarInt(value.totalPages());
                    },
                    buf -> {
                        String categoryName = buf.readString();
                        int categoryIndex = buf.readVarInt();
                        int size = buf.readVarInt();
                        List<ShopItemDto> items = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            items.add(ShopItemDto.PACKET_CODEC.decode(buf));
                        }
                        long balance = buf.readLong();
                        int page = buf.readVarInt();
                        int totalPages = buf.readVarInt();
                        return new ShopCategoryOpenPayload(categoryName, categoryIndex, items, balance, page, totalPages);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
