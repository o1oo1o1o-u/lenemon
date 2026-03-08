package com.lenemon.network.clan;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload S2C : resultat d'un claim ou unclaim.
 * Envoye apres chaque tentative de claim/unclaim, avec feedback et compteurs mis a jour.
 */
public record ClanClaimResultPayload(
        boolean success,
        String message,
        int maxClaims,
        int usedClaims,
        int chunkX,
        int chunkZ,
        boolean claimed
) implements CustomPayload {

    public static final Id<ClanClaimResultPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "clan_claim_result"));

    public static final PacketCodec<RegistryByteBuf, ClanClaimResultPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeBoolean(value.success());
                        buf.writeString(value.message());
                        buf.writeVarInt(value.maxClaims());
                        buf.writeVarInt(value.usedClaims());
                        buf.writeVarInt(value.chunkX());
                        buf.writeVarInt(value.chunkZ());
                        buf.writeBoolean(value.claimed());
                    },
                    buf -> new ClanClaimResultPayload(
                            buf.readBoolean(),
                            buf.readString(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
