package com.lenemon.network.menu;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record HunterMenuOpenPayload(
        int level,
        long xp,
        long xpForNext,
        String progressBar,
        String progressPercent,
        int nextRewardLevel,
        String resetTimer,
        List<QuestDto> easyQuests,
        List<QuestDto> mediumQuests,
        List<QuestDto> hardQuests
) implements CustomPayload {

    public static final Id<HunterMenuOpenPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "hunter_menu_open"));

    public static final PacketCodec<RegistryByteBuf, HunterMenuOpenPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.level());
                        buf.writeLong(value.xp());
                        buf.writeLong(value.xpForNext());
                        buf.writeString(value.progressBar());
                        buf.writeString(value.progressPercent());
                        buf.writeVarInt(value.nextRewardLevel());
                        buf.writeString(value.resetTimer());
                        writeQuestList(buf, value.easyQuests());
                        writeQuestList(buf, value.mediumQuests());
                        writeQuestList(buf, value.hardQuests());
                    },
                    buf -> new HunterMenuOpenPayload(
                            buf.readVarInt(),
                            buf.readLong(),
                            buf.readLong(),
                            buf.readString(),
                            buf.readString(),
                            buf.readVarInt(),
                            buf.readString(),
                            readQuestList(buf),
                            readQuestList(buf),
                            readQuestList(buf)
                    )
            );

    private static void writeQuestList(RegistryByteBuf buf, List<QuestDto> quests) {
        buf.writeVarInt(quests.size());
        for (QuestDto q : quests) {
            QuestDto.PACKET_CODEC.encode(buf, q);
        }
    }

    private static List<QuestDto> readQuestList(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<QuestDto> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(QuestDto.PACKET_CODEC.decode(buf));
        }
        return list;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
