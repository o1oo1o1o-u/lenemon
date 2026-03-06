package com.lenemon.casino.network;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The type Casino owner data payload.
 */
// Données d'un pokemon de party pour l'affichage
public record CasinoOwnerDataPayload(
        long currentPrice,
        double currentChance,
        String pokemonSpecies,       // species du pokemon EN JEU (vide si aucun)
        Set<String> pokemonAspects,  // aspects du pokemon EN JEU
        String pokemonDisplayName,   // nom affiché du pokemon EN JEU
        List<PartyPokemonData> party // party du proprio
) implements CustomPayload {

    /**
     * The type Party pokemon data.
     */
    public record PartyPokemonData(
            String species,
            Set<String> aspects,
            String displayName,
            String nature,
            boolean shiny,
            String ivs
    ) {}

    /**
     * The constant ID.
     */
    public static final Id<CasinoOwnerDataPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "casino_owner_data"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<RegistryByteBuf, CasinoOwnerDataPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeLong(value.currentPrice());
                        buf.writeDouble(value.currentChance());
                        buf.writeString(value.pokemonSpecies());
                        buf.writeVarInt(value.pokemonAspects().size());
                        for (String a : value.pokemonAspects()) buf.writeString(a);
                        buf.writeString(value.pokemonDisplayName());
                        buf.writeVarInt(value.party().size());
                        for (PartyPokemonData p : value.party()) {
                            buf.writeString(p.species());
                            buf.writeVarInt(p.aspects().size());
                            for (String a : p.aspects()) buf.writeString(a);
                            buf.writeString(p.displayName());
                            buf.writeString(p.nature());
                            buf.writeBoolean(p.shiny());
                            buf.writeString(p.ivs());
                        }
                    },
                    buf -> {
                        long price = buf.readLong();
                        double chance = buf.readDouble();
                        String species = buf.readString();
                        int aspectCount = buf.readVarInt();
                        Set<String> aspects = new HashSet<>();
                        for (int i = 0; i < aspectCount; i++) aspects.add(buf.readString());
                        String displayName = buf.readString();
                        int partySize = buf.readVarInt();
                        List<PartyPokemonData> party = new ArrayList<>();
                        for (int i = 0; i < partySize; i++) {
                            String ps  = buf.readString();
                            int ac     = buf.readVarInt();
                            Set<String> pa = new HashSet<>();
                            for (int j = 0; j < ac; j++) pa.add(buf.readString());
                            String pn      = buf.readString();
                            String nature  = buf.readString();
                            boolean shiny  = buf.readBoolean();
                            String ivs     = buf.readString();
                            party.add(new PartyPokemonData(ps, pa, pn, nature, shiny, ivs));
                        }
                        return new CasinoOwnerDataPayload(price, chance, species, aspects, displayName, party);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}