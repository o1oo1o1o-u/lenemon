package com.lenemon.network.pokedex;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Données d'une région Pokédex envoyées au client.
 *
 * @param id         identifiant cobblemon (ex: "national", "kanto" ...)
 * @param label      nom affiché (ex: "Toutes les régions", "Kanto" ...)
 * @param seenPct    pourcentage de Pokémon vus (0.0–100.0)
 * @param caughtPct  pourcentage de Pokémon capturés (0.0–100.0)
 * @param tiers      paliers de récompenses (sorted by threshold)
 */
public record PokedexRegionDto(
        String id,
        String label,
        float seenPct,
        float caughtPct,
        List<PokedexRewardTierDto> tiers
) {
    public static final PacketCodec<RegistryByteBuf, PokedexRegionDto> CODEC =
            PacketCodec.of(
                    (v, buf) -> {
                        buf.writeString(v.id());
                        buf.writeString(v.label());
                        buf.writeFloat(v.seenPct());
                        buf.writeFloat(v.caughtPct());
                        buf.writeVarInt(v.tiers().size());
                        for (PokedexRewardTierDto t : v.tiers()) PokedexRewardTierDto.CODEC.encode(buf, t);
                    },
                    buf -> {
                        String id    = buf.readString();
                        String label = buf.readString();
                        float seen   = buf.readFloat();
                        float caught = buf.readFloat();
                        int n        = buf.readVarInt();
                        List<PokedexRewardTierDto> tiers = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) tiers.add(PokedexRewardTierDto.CODEC.decode(buf));
                        return new PokedexRegionDto(id, label, seen, caught, tiers);
                    }
            );

    /** true si au moins un palier est disponible et non récupéré */
    public boolean hasClaimable() {
        for (PokedexRewardTierDto t : tiers) {
            float pct = t.type().equals("caught") ? caughtPct : seenPct;
            if (t.isAvailable(pct)) return true;
        }
        return false;
    }
}
