package com.lenemon.network.clan;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload S2C : active ou desactive le mode claim cote client.
 */
public record ClanClaimModePayload(
        boolean active,
        int maxClaims,
        int usedClaims
) implements CustomPayload {

    public static final Id<ClanClaimModePayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "clan_claim_mode"));

    public static final PacketCodec<RegistryByteBuf, ClanClaimModePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeBoolean(value.active());
                        buf.writeVarInt(value.maxClaims());
                        buf.writeVarInt(value.usedClaims());
                    },
                    buf -> new ClanClaimModePayload(
                            buf.readBoolean(),
                            buf.readVarInt(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
