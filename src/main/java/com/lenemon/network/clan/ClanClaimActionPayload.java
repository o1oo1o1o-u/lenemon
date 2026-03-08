package com.lenemon.network.clan;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload C2S : action du joueur en mode claim.
 * Valeurs : "claim" | "unclaim" | "exit"
 *
 * Le serveur utilise player.getChunkPos() pour determiner le chunk cible.
 * Aucune coordonnee n'est envoyee par le client (anti-triche).
 */
public record ClanClaimActionPayload(
        String action
) implements CustomPayload {

    public static final Id<ClanClaimActionPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "clan_claim_action"));

    public static final PacketCodec<RegistryByteBuf, ClanClaimActionPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeString(value.action()),
                    buf -> new ClanClaimActionPayload(buf.readString())
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
