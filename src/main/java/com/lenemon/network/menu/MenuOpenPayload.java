package com.lenemon.network.menu;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MenuOpenPayload(
        int hunterLevel,
        float hunterProgress,
        String progressBar,
        String progressPercent
) implements CustomPayload {

    public static final Id<MenuOpenPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "menu_open"));

    public static final PacketCodec<RegistryByteBuf, MenuOpenPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, MenuOpenPayload::hunterLevel,
                    PacketCodecs.FLOAT,   MenuOpenPayload::hunterProgress,
                    PacketCodecs.STRING,  MenuOpenPayload::progressBar,
                    PacketCodecs.STRING,  MenuOpenPayload::progressPercent,
                    MenuOpenPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
