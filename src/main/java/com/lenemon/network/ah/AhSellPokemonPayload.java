package com.lenemon.network.ah;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload S2C : liste des Pokémon de la party proposés à la vente.
 */
public record AhSellPokemonPayload(
        List<AhPartyPokemonDto> party
) implements CustomPayload {

    public static final Id<AhSellPokemonPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "ah_sell_pokemon"));

    public static final PacketCodec<RegistryByteBuf, AhSellPokemonPayload> CODEC =
            PacketCodec.of(AhSellPokemonPayload::encode, AhSellPokemonPayload::decode);

    private static void encode(AhSellPokemonPayload value, RegistryByteBuf buf) {
        buf.writeVarInt(value.party().size());
        for (AhPartyPokemonDto dto : value.party()) {
            buf.writeVarInt(dto.partyIndex());
            buf.writeString(dto.species());
            List<String> aspects = dto.aspects();
            buf.writeVarInt(aspects.size());
            for (String a : aspects) buf.writeString(a);
            buf.writeString(dto.displayName());
            buf.writeBoolean(dto.shiny());
            buf.writeVarInt(dto.level());
            buf.writeString(dto.nature());
            buf.writeString(dto.ivs());
            // Stats supplémentaires
            buf.writeString(dto.ability());
            List<String> types = dto.types();
            buf.writeVarInt(types.size());
            for (String t : types) buf.writeString(t);
            buf.writeString(dto.evs());
            List<String> moves = dto.moves();
            buf.writeVarInt(moves.size());
            for (String m : moves) buf.writeString(m);
            buf.writeString(dto.ball());
            buf.writeBoolean(dto.breedable());
            buf.writeVarInt(dto.friendship());
        }
    }

    private static AhSellPokemonPayload decode(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<AhPartyPokemonDto> party = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int partyIndex       = buf.readVarInt();
            String species       = buf.readString();
            int aspectCount      = buf.readVarInt();
            List<String> aspects = new ArrayList<>(aspectCount);
            for (int j = 0; j < aspectCount; j++) aspects.add(buf.readString());
            String displayName   = buf.readString();
            boolean shiny        = buf.readBoolean();
            int level            = buf.readVarInt();
            String nature        = buf.readString();
            String ivs           = buf.readString();
            // Stats supplémentaires
            String ability       = buf.readString();
            int typeCount        = buf.readVarInt();
            List<String> types   = new ArrayList<>(typeCount);
            for (int j = 0; j < typeCount; j++) types.add(buf.readString());
            String evs           = buf.readString();
            int moveCount        = buf.readVarInt();
            List<String> moves   = new ArrayList<>(moveCount);
            for (int j = 0; j < moveCount; j++) moves.add(buf.readString());
            String ball          = buf.readString();
            boolean breedable    = buf.readBoolean();
            int friendship       = buf.readVarInt();
            party.add(new AhPartyPokemonDto(partyIndex, species, aspects, displayName, shiny, level,
                    nature, ivs, ability, types, evs, moves, ball, breedable, friendship));
        }
        return new AhSellPokemonPayload(party);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
