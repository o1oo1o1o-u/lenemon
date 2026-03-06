package com.lenemon.network.ah;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload S2C : liste paginée des ventes actives de l'AH.
 */
public record AhBrowsePayload(
        List<AhListingDto> listings,
        int page,
        int totalPages,
        long playerBalance,
        String filter,
        String sort
) implements CustomPayload {

    public static final Id<AhBrowsePayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "ah_browse"));

    public static final PacketCodec<RegistryByteBuf, AhBrowsePayload> CODEC =
            PacketCodec.of(AhBrowsePayload::encode, AhBrowsePayload::decode);

    private static void encode(AhBrowsePayload value, RegistryByteBuf buf) {
        buf.writeVarInt(value.listings().size());
        for (AhListingDto dto : value.listings()) {
            encodeDto(dto, buf);
        }
        buf.writeVarInt(value.page());
        buf.writeVarInt(value.totalPages());
        buf.writeLong(value.playerBalance());
        buf.writeString(value.filter());
        buf.writeString(value.sort());
    }

    private static AhBrowsePayload decode(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<AhListingDto> listings = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            listings.add(decodeDto(buf));
        }
        int page = buf.readVarInt();
        int totalPages = buf.readVarInt();
        long balance = buf.readLong();
        String filter = buf.readString();
        String sort   = buf.readString();
        return new AhBrowsePayload(listings, page, totalPages, balance, filter, sort);
    }

    static void encodeDto(AhListingDto dto, RegistryByteBuf buf) {
        buf.writeString(dto.listingId());
        buf.writeString(dto.sellerName());
        buf.writeString(dto.type());
        buf.writeString(dto.itemId());
        buf.writeString(dto.itemDisplayName());
        buf.writeVarInt(dto.itemCount());
        buf.writeString(dto.pokemonSpecies());
        List<String> aspects = dto.pokemonAspects();
        buf.writeVarInt(aspects.size());
        for (String a : aspects) buf.writeString(a);
        buf.writeString(dto.pokemonDisplayName());
        buf.writeBoolean(dto.pokemonShiny());
        buf.writeVarInt(dto.pokemonLevel());
        // Stats supplémentaires
        buf.writeString(dto.pokemonNature());
        buf.writeString(dto.pokemonAbility());
        List<String> types = dto.pokemonTypes();
        buf.writeVarInt(types.size());
        for (String t : types) buf.writeString(t);
        buf.writeString(dto.pokemonEvs());
        List<String> moves = dto.pokemonMoves();
        buf.writeVarInt(moves.size());
        for (String m : moves) buf.writeString(m);
        buf.writeString(dto.pokemonBall());
        buf.writeBoolean(dto.pokemonBreedable());
        buf.writeVarInt(dto.pokemonFriendship());
        // Commun
        buf.writeLong(dto.price());
        buf.writeLong(dto.expiresAt());
        buf.writeBoolean(dto.isMine());
    }

    static AhListingDto decodeDto(RegistryByteBuf buf) {
        String listingId         = buf.readString();
        String sellerName        = buf.readString();
        String type              = buf.readString();
        String itemId            = buf.readString();
        String itemDisplayName   = buf.readString();
        int    itemCount         = buf.readVarInt();
        String pokemonSpecies    = buf.readString();
        int    aspectCount       = buf.readVarInt();
        List<String> aspects     = new ArrayList<>(aspectCount);
        for (int i = 0; i < aspectCount; i++) aspects.add(buf.readString());
        String pokemonDisplayName = buf.readString();
        boolean pokemonShiny      = buf.readBoolean();
        int pokemonLevel          = buf.readVarInt();
        // Stats supplémentaires
        String pokemonNature    = buf.readString();
        String pokemonAbility   = buf.readString();
        int typeCount           = buf.readVarInt();
        List<String> pokemonTypes = new ArrayList<>(typeCount);
        for (int i = 0; i < typeCount; i++) pokemonTypes.add(buf.readString());
        String pokemonEvs       = buf.readString();
        int moveCount           = buf.readVarInt();
        List<String> pokemonMoves = new ArrayList<>(moveCount);
        for (int i = 0; i < moveCount; i++) pokemonMoves.add(buf.readString());
        String pokemonBall      = buf.readString();
        boolean pokemonBreedable = buf.readBoolean();
        int pokemonFriendship   = buf.readVarInt();
        // Commun
        long price    = buf.readLong();
        long expiresAt = buf.readLong();
        boolean isMine = buf.readBoolean();
        return new AhListingDto(listingId, sellerName, type, itemId, itemDisplayName, itemCount,
                pokemonSpecies, aspects, pokemonDisplayName, pokemonShiny, pokemonLevel,
                pokemonNature, pokemonAbility, pokemonTypes, pokemonEvs, pokemonMoves,
                pokemonBall, pokemonBreedable, pokemonFriendship,
                price, expiresAt, isMine);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
