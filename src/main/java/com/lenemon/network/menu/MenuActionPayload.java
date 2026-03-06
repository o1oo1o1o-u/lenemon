package com.lenemon.network.menu;

import com.lenemon.Lenemon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload C2S envoyé par le client pour déclencher une action de menu.
 *
 * Actions valides :
 *   "open_menu"      — ouvre le menu principal
 *   "open_tp"        — ouvre le sous-menu de téléportation
 *   "open_hunter"    — ouvre le menu chasseur
 *   "tp_spawn"       — téléporte au spawn
 *   "tp_overworld"   — téléporte dans le monde principal
 *   "tp_resource"    — téléporte dans le monde ressource
 *   "tp_nether"      — téléporte dans le Nether (permission requise)
 *   "tp_end"         — téléporte dans l'End (permission requise)
 */
public record MenuActionPayload(String action) implements CustomPayload {

    public static final Id<MenuActionPayload> ID =
            new Id<>(Identifier.of(Lenemon.MOD_ID, "menu_action"));

    public static final PacketCodec<RegistryByteBuf, MenuActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, MenuActionPayload::action,
                    MenuActionPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
