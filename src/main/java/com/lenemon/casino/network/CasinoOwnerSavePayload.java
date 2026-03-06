package com.lenemon.casino.network;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Casino owner save payload.
 */
public record CasinoOwnerSavePayload(
        long price,
        double chance,
        int selectedPartyIndex, // -1 = garde le pokemon actuel, -2 = retire le pokemon, sinon index party
        boolean removePokemon   // true = retire le pokemon en jeu
) implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final Id<CasinoOwnerSavePayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "casino_owner_save"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<RegistryByteBuf, CasinoOwnerSavePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeLong(value.price());
                        buf.writeDouble(value.chance());
                        buf.writeVarInt(value.selectedPartyIndex());
                        buf.writeBoolean(value.removePokemon());
                    },
                    buf -> new CasinoOwnerSavePayload(
                            buf.readLong(),
                            buf.readDouble(),
                            buf.readVarInt(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}