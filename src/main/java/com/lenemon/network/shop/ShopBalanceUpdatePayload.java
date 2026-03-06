package com.lenemon.network.shop;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload S2C : met à jour la balance affichée dans le ShopCategoryScreen sans rouvrir l'écran.
 */
public record ShopBalanceUpdatePayload(long balance) implements CustomPayload {

    public static final Id<ShopBalanceUpdatePayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "shop_balance_update"));

    public static final PacketCodec<RegistryByteBuf, ShopBalanceUpdatePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeLong(value.balance()),
                    buf -> new ShopBalanceUpdatePayload(buf.readLong())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
