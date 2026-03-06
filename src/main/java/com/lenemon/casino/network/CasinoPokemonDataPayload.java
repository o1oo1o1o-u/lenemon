package com.lenemon.casino.network;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The type Casino pokemon data payload.
 */
public record CasinoPokemonDataPayload(String species, Set<String> aspects) implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<CasinoPokemonDataPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "casino_pokemon_data"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<RegistryByteBuf, CasinoPokemonDataPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.species());
                        buf.writeVarInt(value.aspects().size());
                        for (String aspect : value.aspects()) {
                            buf.writeString(aspect);
                        }
                    },
                    buf -> {
                        String species = buf.readString();
                        int size = buf.readVarInt();
                        Set<String> aspects = new HashSet<>();
                        for (int i = 0; i < size; i++) {
                            aspects.add(buf.readString());
                        }
                        return new CasinoPokemonDataPayload(species, aspects);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}