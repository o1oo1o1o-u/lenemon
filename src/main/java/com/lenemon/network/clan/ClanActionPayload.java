package com.lenemon.network.clan;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload C2S : action effectuee depuis le GUI de clan.
 *
 * Convention action :
 *   "disband"              → dissoudre le clan
 *   "leave"                → quitter le clan
 *   "kick:<uuid>"          → kicker un membre
 *   "promote:<uuid>"       → promouvoir un membre
 *   "demote:<uuid>"        → retrograder un officier
 *   "deposit:<montant>"    → depot banque
 *   "withdraw:<montant>"   → retrait banque
 *   "open_gui"             → demande d'ouverture du GUI (rafraichissement)
 */
public record ClanActionPayload(String action) implements CustomPayload {

    public static final Id<ClanActionPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "clan_action"));

    public static final PacketCodec<RegistryByteBuf, ClanActionPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeString(value.action()),
                    buf -> new ClanActionPayload(buf.readString())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
