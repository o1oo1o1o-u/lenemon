package com.lenemon.network.shop;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload C2S envoyé par le client pour déclencher une action du shop.
 *
 * Actions valides :
 *   "open_shop"                                        — ouvre le menu catégories
 *   "back_to_shop"                                     — retour au menu catégories
 *   "open_category:<index>"                            — ouvre une catégorie page 0
 *   "open_category:<index>:page:<n>"                   — ouvre une catégorie à la page n
 *   "buy:<namespace>:<path>:<qty>:cat:<ci>:page:<p>"   — achète qty items
 *   "sell:<namespace>:<path>:1:cat:<ci>:page:<p>"      — vend 1 item
 *   "sell_all:<namespace>:<path>:cat:<ci>:page:<p>"    — vend tout
 */
public record ShopActionPayload(String action) implements CustomPayload {

    public static final Id<ShopActionPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "shop_action"));

    public static final PacketCodec<RegistryByteBuf, ShopActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, ShopActionPayload::action,
                    ShopActionPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
