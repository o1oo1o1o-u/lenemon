package com.lenemon.network.clan;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload S2C : envoie la liste des chunks claims au client en mode claim.
 * Envoye a l'entree en mode claim et apres chaque claim/unclaim.
 */
public record ClanClaimMapPayload(
        List<ChunkEntry> ownChunks,
        List<ChunkEntry> nearbyOther,
        int maxClaims,
        int usedClaims
) implements CustomPayload {

    public static final Id<ClanClaimMapPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "clan_claim_map"));

    public static final PacketCodec<RegistryByteBuf, ClanClaimMapPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.ownChunks().size());
                        for (ChunkEntry e : value.ownChunks()) {
                            buf.writeVarInt(e.chunkX());
                            buf.writeVarInt(e.chunkZ());
                        }
                        buf.writeVarInt(value.nearbyOther().size());
                        for (ChunkEntry e : value.nearbyOther()) {
                            buf.writeVarInt(e.chunkX());
                            buf.writeVarInt(e.chunkZ());
                        }
                        buf.writeVarInt(value.maxClaims());
                        buf.writeVarInt(value.usedClaims());
                    },
                    buf -> {
                        int ownSize = buf.readVarInt();
                        List<ChunkEntry> own = new ArrayList<>(ownSize);
                        for (int i = 0; i < ownSize; i++) {
                            own.add(new ChunkEntry(buf.readVarInt(), buf.readVarInt()));
                        }
                        int otherSize = buf.readVarInt();
                        List<ChunkEntry> other = new ArrayList<>(otherSize);
                        for (int i = 0; i < otherSize; i++) {
                            other.add(new ChunkEntry(buf.readVarInt(), buf.readVarInt()));
                        }
                        int maxClaims  = buf.readVarInt();
                        int usedClaims = buf.readVarInt();
                        return new ClanClaimMapPayload(own, other, maxClaims, usedClaims);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    /** Coordonnees d'un chunk (en coordonnees chunk, pas en blocs). */
    public record ChunkEntry(int chunkX, int chunkZ) {}
}
