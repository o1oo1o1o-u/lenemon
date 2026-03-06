package com.lenemon.network.ah;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload C2S : le client demande les prix moyens pour un Pokémon donné (espèce + shiny).
 */
public record AhRequestPokemonPricePayload(String species, boolean shiny) implements CustomPayload {

    public static final Id<AhRequestPokemonPricePayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "ah_request_pokemon_price"));

    public static final PacketCodec<RegistryByteBuf, AhRequestPokemonPricePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.species());
                        buf.writeBoolean(value.shiny());
                    },
                    buf -> new AhRequestPokemonPricePayload(buf.readString(), buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
