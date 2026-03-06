package com.lenemon.network;

import com.lenemon.pickaxe.ExcaveonConfig;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * The type Packet excaveon config.
 */
public record PacketExcaveonConfig(
        int blocksToLevel2,
        int blocksToLevel3,
        int blocksToLevel4,
        int blocksToLevel5,
        String shopSellCommand,
        String levelUpMessage
) implements CustomPayload {

    /**
     * The constant ID.
     */
    public static final CustomPayload.Id<PacketExcaveonConfig> ID =
            new CustomPayload.Id<>(Identifier.of("lenemon", "excaveon_config"));

    /**
     * The constant CODEC.
     */
    public static final PacketCodec<PacketByteBuf, PacketExcaveonConfig> CODEC = PacketCodec.of(
            PacketExcaveonConfig::write,
            PacketExcaveonConfig::read
    );

    /**
     * From packet excaveon config.
     *
     * @param cfg the cfg
     * @return the packet excaveon config
     */
    public static PacketExcaveonConfig from(ExcaveonConfig cfg) {
        return new PacketExcaveonConfig(
                cfg.blocksToLevel2,
                cfg.blocksToLevel3,
                cfg.blocksToLevel4,
                cfg.blocksToLevel5,
                cfg.shopSellCommand,
                cfg.levelUpMessage
        );
    }

    /**
     * To config excaveon config.
     *
     * @return the excaveon config
     */
    public ExcaveonConfig toConfig() {
        ExcaveonConfig cfg = new ExcaveonConfig();
        cfg.blocksToLevel2 = blocksToLevel2;
        cfg.blocksToLevel3 = blocksToLevel3;
        cfg.blocksToLevel4 = blocksToLevel4;
        cfg.blocksToLevel5 = blocksToLevel5;
        cfg.shopSellCommand = shopSellCommand;
        cfg.levelUpMessage = levelUpMessage;
        return cfg;
    }

    private static void write(PacketExcaveonConfig p, PacketByteBuf buf) {
        buf.writeInt(p.blocksToLevel2());
        buf.writeInt(p.blocksToLevel3());
        buf.writeInt(p.blocksToLevel4());
        buf.writeInt(p.blocksToLevel5());
        buf.writeString(p.shopSellCommand());
        buf.writeString(p.levelUpMessage());
    }

    private static PacketExcaveonConfig read(PacketByteBuf buf) {
        int l2 = buf.readInt();
        int l3 = buf.readInt();
        int l4 = buf.readInt();
        int l5 = buf.readInt();
        String cmd = buf.readString();
        String msg = buf.readString();
        return new PacketExcaveonConfig(l2, l3, l4, l5, cmd, msg);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}