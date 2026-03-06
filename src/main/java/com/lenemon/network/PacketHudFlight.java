package com.lenemon.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Packet envoyé serveur → client pour synchroniser l'état de la barre de stamina de ride.
 *
 * <p>{@code active} : true si le joueur est monté sur un Pokémon volant.
 * <p>{@code staminaRatio} : ratio stamina actuelle / stamina max (0.0–1.0). Ignoré si active=false.
 */
public record PacketHudFlight(boolean active, float staminaRatio) implements CustomPayload {

    public static final Id<PacketHudFlight> ID = new Id<>(Identifier.of("lenemon", "hud_flight"));

    public static final PacketCodec<RegistryByteBuf, PacketHudFlight> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL,  PacketHudFlight::active,
                    PacketCodecs.FLOAT, PacketHudFlight::staminaRatio,
                    PacketHudFlight::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
