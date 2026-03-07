package com.lenemon.network.pokedex;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * Un palier de récompense Pokédex pour un type/région donné.
 *
 * @param type        "caught" ou "seen"
 * @param threshold   pourcentage requis (ex: 10, 25, 50 ...)
 * @param claimed     déjà récupéré par ce joueur
 * @param rewardDesc  description courte de la récompense (ex: "5000 $ + Diamant")
 */
public record PokedexRewardTierDto(
        String type,
        int threshold,
        boolean claimed,
        String rewardDesc
) {
    public static final PacketCodec<RegistryByteBuf, PokedexRewardTierDto> CODEC =
            PacketCodec.of(
                    (v, buf) -> {
                        buf.writeString(v.type());
                        buf.writeVarInt(v.threshold());
                        buf.writeBoolean(v.claimed());
                        buf.writeString(v.rewardDesc());
                    },
                    buf -> new PokedexRewardTierDto(
                            buf.readString(),
                            buf.readVarInt(),
                            buf.readBoolean(),
                            buf.readString()
                    )
            );

    /** true si la récompense est disponible mais pas encore récupérée (caughtPct/seenPct passé en paramètre) */
    public boolean isAvailable(float pct) {
        return !claimed && pct >= threshold;
    }
}
