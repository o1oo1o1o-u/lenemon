package com.lenemon.network.menu;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record QuestDto(
        String id,
        String description,
        String difficultyLabel,
        String colorCode,
        int progress,
        int amount,
        boolean complete,
        long xpReward,
        long moneyReward,
        String itemsLabel,
        String ballItemId
) {

    public static final PacketCodec<RegistryByteBuf, QuestDto> PACKET_CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.id());
                        buf.writeString(value.description());
                        buf.writeString(value.difficultyLabel());
                        buf.writeString(value.colorCode());
                        buf.writeVarInt(value.progress());
                        buf.writeVarInt(value.amount());
                        buf.writeBoolean(value.complete());
                        buf.writeLong(value.xpReward());
                        buf.writeLong(value.moneyReward());
                        buf.writeString(value.itemsLabel());
                        buf.writeString(value.ballItemId());
                    },
                    buf -> new QuestDto(
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readBoolean(),
                            buf.readLong(),
                            buf.readLong(),
                            buf.readString(),
                            buf.readString()
                    )
            );
}
