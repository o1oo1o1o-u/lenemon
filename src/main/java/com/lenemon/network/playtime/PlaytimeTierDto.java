package com.lenemon.network.playtime;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.ArrayList;
import java.util.List;

public record PlaytimeTierDto(
        String id,
        String label,
        int hoursRequired,
        boolean claimed,
        List<String> rewardLines
) {
    public static final PacketCodec<RegistryByteBuf, PlaytimeTierDto> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.id());
                        buf.writeString(value.label());
                        buf.writeVarInt(value.hoursRequired());
                        buf.writeBoolean(value.claimed());
                        buf.writeVarInt(value.rewardLines().size());
                        for (String line : value.rewardLines()) buf.writeString(line);
                    },
                    buf -> {
                        String id = buf.readString();
                        String label = buf.readString();
                        int hoursRequired = buf.readVarInt();
                        boolean claimed = buf.readBoolean();
                        int size = buf.readVarInt();
                        List<String> rewardLines = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) rewardLines.add(buf.readString());
                        return new PlaytimeTierDto(id, label, hoursRequired, claimed, rewardLines);
                    }
            );
}
