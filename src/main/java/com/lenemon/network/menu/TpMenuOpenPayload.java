package com.lenemon.network.menu;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TpMenuOpenPayload(
        boolean hasNether,
        boolean hasEnd
) implements CustomPayload {

    public static final Id<TpMenuOpenPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "tp_menu_open"));

    public static final PacketCodec<RegistryByteBuf, TpMenuOpenPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL, TpMenuOpenPayload::hasNether,
                    PacketCodecs.BOOL, TpMenuOpenPayload::hasEnd,
                    TpMenuOpenPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
