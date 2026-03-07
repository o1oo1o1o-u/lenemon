package com.lenemon.network.pokedex;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** C2S : le joueur demande à récupérer toutes les récompenses disponibles pour une région. */
public record PokedexClaimPayload(String regionId) implements CustomPayload {

    public static final Id<PokedexClaimPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "pokedex_claim"));

    public static final PacketCodec<RegistryByteBuf, PokedexClaimPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, PokedexClaimPayload::regionId,
                    PokedexClaimPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
