package com.lenemon.network;

import com.lenemon.armor.config.EffectConfig;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The type Packet armor effects.
 */
public record PacketArmorEffects(UUID playerUuid, List<EffectConfig> effects) implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final CustomPayload.Id<PacketArmorEffects> ID =
            new CustomPayload.Id<>(Identifier.of("lenemon", "armor_effects"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<PacketByteBuf, PacketArmorEffects> CODEC = PacketCodec.of(
            PacketArmorEffects::write,
            PacketArmorEffects::read
    );

    private static void write(PacketArmorEffects packet, PacketByteBuf buf) {
        buf.writeUuid(packet.playerUuid());
        buf.writeInt(packet.effects().size());
        for (EffectConfig effect : packet.effects()) {
            buf.writeString(effect.type);
            buf.writeString(effect.particle);
            buf.writeInt(effect.density);
            buf.writeDouble(effect.radius);
            buf.writeString(effect.color);
            buf.writeFloat(effect.volume);
            buf.writeFloat(effect.pitch);
        }
    }

    private static PacketArmorEffects read(PacketByteBuf buf) {
        UUID uuid = buf.readUuid();
        int size = buf.readInt();
        List<EffectConfig> effects = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            EffectConfig e = new EffectConfig();
            e.type = buf.readString();
            e.particle = buf.readString();
            e.density = buf.readInt();
            e.radius = buf.readDouble();
            e.color = buf.readString();
            e.volume = buf.readFloat();
            e.pitch = buf.readFloat();
            effects.add(e);
        }
        return new PacketArmorEffects(uuid, effects);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}