package com.lenemon.network.pokedex;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** S2C : ouvre le menu Pokédex avec les stats et paliers de récompenses. */
public record PokedexOpenPayload(List<PokedexRegionDto> regions) implements CustomPayload {

    public static final Id<PokedexOpenPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "pokedex_open"));

    public static final PacketCodec<RegistryByteBuf, PokedexOpenPayload> CODEC =
            PacketCodec.of(
                    (v, buf) -> {
                        buf.writeVarInt(v.regions().size());
                        for (PokedexRegionDto r : v.regions()) PokedexRegionDto.CODEC.encode(buf, r);
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        List<PokedexRegionDto> regions = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) regions.add(PokedexRegionDto.CODEC.decode(buf));
                        return new PokedexOpenPayload(regions);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
