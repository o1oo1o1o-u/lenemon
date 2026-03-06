package com.lenemon.network.ah;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload S2C : liste des items de l'inventaire proposés à la mise en vente.
 */
public record AhSellItemPayload(
        List<AhItemSlotDto> items,
        int page
) implements CustomPayload {

    public static final Id<AhSellItemPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "ah_sell_item"));

    public static final PacketCodec<RegistryByteBuf, AhSellItemPayload> CODEC =
            PacketCodec.of(AhSellItemPayload::encode, AhSellItemPayload::decode);

    private static void encode(AhSellItemPayload value, RegistryByteBuf buf) {
        buf.writeVarInt(value.items().size());
        for (AhItemSlotDto dto : value.items()) {
            buf.writeVarInt(dto.slot());
            buf.writeString(dto.itemId());
            buf.writeString(dto.displayName());
            buf.writeVarInt(dto.count());
            buf.writeBoolean(dto.canSell());
        }
        buf.writeVarInt(value.page());
    }

    private static AhSellItemPayload decode(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<AhItemSlotDto> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int slot        = buf.readVarInt();
            String itemId   = buf.readString();
            String display  = buf.readString();
            int count       = buf.readVarInt();
            boolean canSell = buf.readBoolean();
            items.add(new AhItemSlotDto(slot, itemId, display, count, canSell));
        }
        int page = buf.readVarInt();
        return new AhSellItemPayload(items, page);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
