package com.lenemon.network.ah;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload S2C : mes ventes + objets à récupérer.
 */
public record AhMyListingsPayload(
        List<AhListingDto> myListings,
        List<AhListingDto> pendingRecovery,
        long balance,
        int activeCount
) implements CustomPayload {

    public static final Id<AhMyListingsPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "ah_my_listings"));

    public static final PacketCodec<RegistryByteBuf, AhMyListingsPayload> CODEC =
            PacketCodec.of(AhMyListingsPayload::encode, AhMyListingsPayload::decode);

    private static void encode(AhMyListingsPayload value, RegistryByteBuf buf) {
        writeList(value.myListings(), buf);
        writeList(value.pendingRecovery(), buf);
        buf.writeLong(value.balance());
        buf.writeVarInt(value.activeCount());
    }

    private static AhMyListingsPayload decode(RegistryByteBuf buf) {
        List<AhListingDto> mine     = readList(buf);
        List<AhListingDto> recovery = readList(buf);
        long balance                = buf.readLong();
        int  activeCount            = buf.readVarInt();
        return new AhMyListingsPayload(mine, recovery, balance, activeCount);
    }

    private static void writeList(List<AhListingDto> list, RegistryByteBuf buf) {
        buf.writeVarInt(list.size());
        for (AhListingDto dto : list) AhBrowsePayload.encodeDto(dto, buf);
    }

    private static List<AhListingDto> readList(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<AhListingDto> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(AhBrowsePayload.decodeDto(buf));
        return list;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
