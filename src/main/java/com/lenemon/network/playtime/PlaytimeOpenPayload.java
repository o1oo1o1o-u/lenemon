package com.lenemon.network.playtime;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record PlaytimeOpenPayload(
        long playtimeTicks,
        int claimedCount,
        List<PlaytimeTierDto> tiers
) implements CustomPayload {

    public static final Id<PlaytimeOpenPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "playtime_open"));

    public static final PacketCodec<RegistryByteBuf, PlaytimeOpenPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeLong(value.playtimeTicks());
                        buf.writeVarInt(value.claimedCount());
                        buf.writeVarInt(value.tiers().size());
                        for (PlaytimeTierDto tier : value.tiers()) {
                            PlaytimeTierDto.CODEC.encode(buf, tier);
                        }
                    },
                    buf -> {
                        long playtimeTicks = buf.readLong();
                        int claimedCount = buf.readVarInt();
                        int size = buf.readVarInt();
                        List<PlaytimeTierDto> tiers = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) tiers.add(PlaytimeTierDto.CODEC.decode(buf));
                        return new PlaytimeOpenPayload(playtimeTicks, claimedCount, tiers);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
