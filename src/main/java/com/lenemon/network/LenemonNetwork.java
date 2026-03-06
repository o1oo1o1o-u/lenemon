package com.lenemon.network;

import com.lenemon.armor.ArmorEffectHandler;
import com.lenemon.armor.ArmorSet;
import com.lenemon.armor.config.EffectConfig;
import com.lenemon.casino.network.CasinoPokemonDataPayload;
import com.lenemon.casino.network.CasinoSpinOutcomePayload;
import com.lenemon.casino.network.CasinoSpinRequestPayload;
import com.lenemon.network.menu.HunterMenuOpenPayload;
import com.lenemon.network.menu.MenuActionHandler;
import com.lenemon.network.menu.MenuActionPayload;
import com.lenemon.network.menu.MenuOpenPayload;
import com.lenemon.network.menu.TpMenuOpenPayload;
import com.lenemon.network.shop.ShopActionHandler;
import com.lenemon.network.shop.ShopActionPayload;
import com.lenemon.network.shop.ShopBalanceUpdatePayload;
import com.lenemon.network.shop.ShopCategoryOpenPayload;
import com.lenemon.network.shop.ShopOpenPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import com.lenemon.pickaxe.ExcaveonConfigLoader;
import com.lenemon.network.PacketExcaveonConfig;

import java.util.List;

/**
 * The type Lenemon network.
 */
public class LenemonNetwork {

    /**
     * Register.
     */
    public static void register() {
        // Enregistrer le type de packet
        PayloadTypeRegistry.playS2C().register(PacketArmorEffects.ID, PacketArmorEffects.CODEC);
        PayloadTypeRegistry.playS2C().register(PacketExcaveonConfig.ID, PacketExcaveonConfig.CODEC);
        PayloadTypeRegistry.playS2C().register(PacketHudBalance.ID, PacketHudBalance.CODEC);
        PayloadTypeRegistry.playS2C().register(PacketHudHunter.ID, PacketHudHunter.CODEC);
        PayloadTypeRegistry.playS2C().register(PacketHudFlight.ID, PacketHudFlight.CODEC);
        PayloadTypeRegistry.playC2S().register(
                CasinoSpinRequestPayload.ID,
                CasinoSpinRequestPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                CasinoSpinOutcomePayload.ID,
                CasinoSpinOutcomePayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                com.lenemon.casino.network.CasinoCanSpinRequestPayload.ID,
                com.lenemon.casino.network.CasinoCanSpinRequestPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                com.lenemon.casino.network.CasinoCanSpinResponsePayload.ID,
                com.lenemon.casino.network.CasinoCanSpinResponsePayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                com.lenemon.casino.network.CasinoAnimDonePayload.ID,
                com.lenemon.casino.network.CasinoAnimDonePayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                com.lenemon.casino.network.CasinoSpinResultPayload.ID,
                com.lenemon.casino.network.CasinoSpinResultPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                CasinoPokemonDataPayload.ID,
                CasinoPokemonDataPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                com.lenemon.casino.network.CasinoOwnerDataPayload.ID,
                com.lenemon.casino.network.CasinoOwnerDataPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                com.lenemon.casino.network.CasinoOwnerSavePayload.ID,
                com.lenemon.casino.network.CasinoOwnerSavePayload.CODEC
        );

        // Menu payloads S2C
        PayloadTypeRegistry.playS2C().register(MenuOpenPayload.ID, MenuOpenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TpMenuOpenPayload.ID, TpMenuOpenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HunterMenuOpenPayload.ID, HunterMenuOpenPayload.CODEC);

        // Menu payloads C2S
        PayloadTypeRegistry.playC2S().register(MenuActionPayload.ID, MenuActionPayload.CODEC);

        // Handler C2S menu
        ServerPlayNetworking.registerGlobalReceiver(MenuActionPayload.ID, MenuActionHandler::handle);

        // Shop payloads S2C
        PayloadTypeRegistry.playS2C().register(ShopOpenPayload.ID, ShopOpenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShopCategoryOpenPayload.ID, ShopCategoryOpenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShopBalanceUpdatePayload.ID, ShopBalanceUpdatePayload.CODEC);

        // Shop payload C2S
        PayloadTypeRegistry.playC2S().register(ShopActionPayload.ID, ShopActionPayload.CODEC);

        // Handler C2S shop
        ServerPlayNetworking.registerGlobalReceiver(ShopActionPayload.ID, ShopActionHandler::handle);

        // AH payloads S2C
        PayloadTypeRegistry.playS2C().register(com.lenemon.network.ah.AhBrowsePayload.ID,      com.lenemon.network.ah.AhBrowsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.lenemon.network.ah.AhMyListingsPayload.ID,  com.lenemon.network.ah.AhMyListingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.lenemon.network.ah.AhSellItemPayload.ID,    com.lenemon.network.ah.AhSellItemPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.lenemon.network.ah.AhSellPokemonPayload.ID, com.lenemon.network.ah.AhSellPokemonPayload.CODEC);

        // AH payloads C2S
        PayloadTypeRegistry.playC2S().register(com.lenemon.network.ah.AhActionPayload.ID, com.lenemon.network.ah.AhActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.lenemon.network.ah.AhRequestPricePayload.ID, com.lenemon.network.ah.AhRequestPricePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.lenemon.network.ah.AhRequestPokemonPricePayload.ID, com.lenemon.network.ah.AhRequestPokemonPricePayload.CODEC);

        // AH payload S2C prix
        PayloadTypeRegistry.playS2C().register(com.lenemon.network.ah.AhPriceInfoPayload.ID, com.lenemon.network.ah.AhPriceInfoPayload.CODEC);

        // AH handlers C2S
        ServerPlayNetworking.registerGlobalReceiver(com.lenemon.network.ah.AhActionPayload.ID, com.lenemon.ah.AhActionHandler::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.lenemon.network.ah.AhRequestPricePayload.ID, (payload, ctx) -> {
            ctx.server().execute(() -> {
                net.minecraft.server.network.ServerPlayerEntity player = ctx.player();
                com.lenemon.ah.AhWorldData data = com.lenemon.ah.AhWorldData.get(ctx.server().getOverworld());
                long avgListed = data.getAverageListedPrice(payload.itemId());
                long avgSold   = data.getAverageSoldPrice(payload.itemId());
                ServerPlayNetworking.send(player, new com.lenemon.network.ah.AhPriceInfoPayload(avgListed, avgSold));
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(com.lenemon.network.ah.AhRequestPokemonPricePayload.ID, (payload, ctx) -> {
            ctx.server().execute(() -> {
                net.minecraft.server.network.ServerPlayerEntity player = ctx.player();
                com.lenemon.ah.AhWorldData data = com.lenemon.ah.AhWorldData.get(ctx.server().getOverworld());
                long avgListed = data.getAveragePokemonListedPrice(payload.species(), payload.shiny());
                long avgSold   = data.getAveragePokemonSoldPrice(payload.species(), payload.shiny());
                ServerPlayNetworking.send(player, new com.lenemon.network.ah.AhPriceInfoPayload(avgListed, avgSold));
            });
        });
    }

    /**
     * Send excaveon config.
     *
     * @param player the player
     */
    public static void sendExcaveonConfig(ServerPlayerEntity player) {
        var cfg = ExcaveonConfigLoader.get();
        ServerPlayNetworking.send(player, PacketExcaveonConfig.from(cfg));
    }

    /**
     * Send all excaveon config.
     *
     * @param server the server
     */
// AJOUT
    public static void sendAllExcaveonConfig(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            sendExcaveonConfig(p);
        }
    }


    /**
     * Envoie les effets actifs d'un joueur à tous les joueurs connectés  @param player the player
     *
     * @param player the player
     */
    public static void sendArmorEffects(ServerPlayerEntity player) {
        List<EffectConfig> activeEffects = List.of();
        for (ArmorSet set : ArmorEffectHandler.ARMOR_SETS) {
            if (set.isEnabled() && set.isWearing(player)) {
                activeEffects = set.getEffectConfigs();
                break;
            }
        }

        PacketArmorEffects packet = new PacketArmorEffects(player.getUuid(), activeEffects);

        // Envoyer à tous les joueurs connectés (incluant le joueur lui-même)
        for (ServerPlayerEntity target : PlayerLookup.all(player.getServer())) {
            ServerPlayNetworking.send(target, packet);
        }
    }

    /**
     * Reload : renvoie les effets de tous les joueurs à tous  @param server the server
     *
     * @param server the server
     */
    public static void sendAllArmorEffects(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendArmorEffects(player);
        }
    }
}