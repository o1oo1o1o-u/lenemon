package com.lenemon.network.clan;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payload S2C : ouvre l'ecran principal de gestion du clan.
 * Transporte toutes les donnees necessaires a l'affichage du GUI.
 */
public record ClanGuiPayload(
        String clanId,
        String clanName,
        String clanTag,
        int level,
        long xp,
        long xpNextLevel,
        long bankBalance,
        long createdAt,
        String viewerRole,
        List<MemberDto> members,
        List<RankDto> ranks,
        Map<String, String> permissions,
        int clanLevel,
        int maxClaims,
        int usedClaims,
        long nextLevelPrice
) implements CustomPayload {

    public static final Id<ClanGuiPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "clan_gui_open"));

    public static final PacketCodec<RegistryByteBuf, ClanGuiPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.clanId());
                        buf.writeString(value.clanName());
                        buf.writeString(value.clanTag());
                        buf.writeVarInt(value.level());
                        buf.writeLong(value.xp());
                        buf.writeLong(value.xpNextLevel());
                        buf.writeLong(value.bankBalance());
                        buf.writeLong(value.createdAt());
                        buf.writeString(value.viewerRole());
                        // Members
                        buf.writeVarInt(value.members().size());
                        for (MemberDto dto : value.members()) {
                            buf.writeString(dto.uuid());
                            buf.writeString(dto.name());
                            buf.writeString(dto.role());
                            buf.writeLong(dto.totalContributed());
                            buf.writeLong(dto.lastSeen());
                            buf.writeBoolean(dto.isOnline());
                            buf.writeString(dto.rankId());
                        }
                        // Ranks
                        buf.writeVarInt(value.ranks().size());
                        for (RankDto r : value.ranks()) {
                            buf.writeString(r.id());
                            buf.writeString(r.name());
                            buf.writeString(r.colorCode());
                            buf.writeLong(r.withdrawLimit());
                            buf.writeVarInt(r.sortOrder());
                        }
                        // Permissions
                        Map<String, String> perms = value.permissions() != null ? value.permissions() : Map.of();
                        buf.writeVarInt(perms.size());
                        for (Map.Entry<String, String> e : perms.entrySet()) {
                            buf.writeString(e.getKey());
                            buf.writeString(e.getValue());
                        }
                        // Champs level economique et claims
                        buf.writeVarInt(value.clanLevel());
                        buf.writeVarInt(value.maxClaims());
                        buf.writeVarInt(value.usedClaims());
                        buf.writeLong(value.nextLevelPrice());
                    },
                    buf -> {
                        String clanId      = buf.readString();
                        String clanName    = buf.readString();
                        String clanTag     = buf.readString();
                        int    level       = buf.readVarInt();
                        long   xp          = buf.readLong();
                        long   xpNextLevel = buf.readLong();
                        long   bankBalance = buf.readLong();
                        long   createdAt   = buf.readLong();
                        String viewerRole  = buf.readString();

                        int memberCount = buf.readVarInt();
                        List<MemberDto> members = new ArrayList<>(memberCount);
                        for (int i = 0; i < memberCount; i++) {
                            members.add(new MemberDto(
                                    buf.readString(), buf.readString(), buf.readString(),
                                    buf.readLong(), buf.readLong(), buf.readBoolean(), buf.readString()
                            ));
                        }

                        int rankCount = buf.readVarInt();
                        List<RankDto> ranks = new ArrayList<>(rankCount);
                        for (int i = 0; i < rankCount; i++) {
                            ranks.add(new RankDto(
                                    buf.readString(), buf.readString(),
                                    buf.readString(), buf.readLong(), buf.readVarInt()
                            ));
                        }

                        int permCount = buf.readVarInt();
                        Map<String, String> permissions = new HashMap<>(permCount);
                        for (int i = 0; i < permCount; i++) {
                            permissions.put(buf.readString(), buf.readString());
                        }

                        int clanLevel      = buf.readVarInt();
                        int maxClaims      = buf.readVarInt();
                        int usedClaims     = buf.readVarInt();
                        long nextLevelPrice = buf.readLong();

                        return new ClanGuiPayload(clanId, clanName, clanTag, level, xp,
                                xpNextLevel, bankBalance, createdAt, viewerRole, members, ranks, permissions,
                                clanLevel, maxClaims, usedClaims, nextLevelPrice);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * DTO d'un membre pour le transport reseau.
     * totalContributed : coins deposes au total dans la banque du clan.
     * lastSeen : timestamp Unix (ms), 0 si le joueur est en ligne.
     * isOnline : true si le joueur est connecte.
     * rankId : ID du rang systeme ou custom du joueur ("owner"/"officer"/"member"/UUID-court).
     */
    public record MemberDto(
            String uuid,
            String name,
            String role,
            long totalContributed,
            long lastSeen,
            boolean isOnline,
            String rankId
    ) {}

    /** DTO d'un rang pour le transport reseau. */
    public record RankDto(String id, String name, String colorCode, long withdrawLimit, int sortOrder) {}
}
