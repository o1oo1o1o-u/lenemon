package com.lenemon.client.network;

import com.lenemon.armor.config.EffectConfig;
import com.lenemon.casino.network.CasinoSpinOutcomePayload;
import com.lenemon.casino.screen.CasinoScreenHandler;
import com.lenemon.client.hud.HudBalanceCache;
import com.lenemon.client.hud.HudFlightCache;
import com.lenemon.client.hud.HudHunterCache;
import com.lenemon.client.menu.screen.HunterMenuScreen;
import com.lenemon.client.menu.screen.MenuScreen;
import com.lenemon.client.menu.screen.TpMenuScreen;
import com.lenemon.network.PacketArmorEffects;
import com.lenemon.network.PacketHudBalance;
import com.lenemon.network.PacketHudFlight;
import com.lenemon.network.PacketHudHunter;
import com.lenemon.network.menu.HunterMenuOpenPayload;
import com.lenemon.network.menu.MenuOpenPayload;
import com.lenemon.network.menu.TpMenuOpenPayload;
import com.lenemon.network.shop.ShopBalanceUpdatePayload;
import com.lenemon.network.shop.ShopCategoryOpenPayload;
import com.lenemon.network.shop.ShopOpenPayload;
import com.lenemon.client.shop.screen.ShopCategoryScreen;
import com.lenemon.client.shop.screen.ShopScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.lenemon.network.PacketExcaveonConfig;
import com.lenemon.pickaxe.ExcaveonConfigLoader;

import java.util.*;

/**
 * The type Lenemon network client.
 */
public class LenemonNetworkClient {

    // UUID joueur → liste d'effets actifs
    private static final Map<UUID, List<EffectConfig>> playerEffects = new HashMap<>();

    /**
     * Register.
     */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PacketArmorEffects.ID, (payload, context) -> {
            playerEffects.put(payload.playerUuid(), payload.effects());
        });

        ClientPlayNetworking.registerGlobalReceiver(PacketExcaveonConfig.ID, (payload, context) -> {
            ExcaveonConfigLoader.setServerConfig(payload.toConfig());
        });

        ClientPlayNetworking.registerGlobalReceiver(PacketHudBalance.ID, (payload, context) -> {
            HudBalanceCache.setBalance(payload.balance());
        });

        ClientPlayNetworking.registerGlobalReceiver(PacketHudHunter.ID, (payload, context) -> {
            HudHunterCache.set(payload.level(), payload.progress());
        });

        ClientPlayNetworking.registerGlobalReceiver(PacketHudFlight.ID, (payload, context) -> {
            HudFlightCache.set(payload.active(), payload.staminaRatio());
        });

        // Dans register(), remplace TOUS les receivers casino par ceci :

// Outcome : le serveur envoie win/left/right pour lancer l'anim
        ClientPlayNetworking.registerGlobalReceiver(CasinoSpinOutcomePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player == null) return;
                if (!(context.client().player.currentScreenHandler instanceof CasinoScreenHandler handler)) return;
                handler.applySpinResult(payload.win(), payload.left(), payload.right());
            });
        });

// Can spin response : met à jour le bouton
        ClientPlayNetworking.registerGlobalReceiver(
                com.lenemon.casino.network.CasinoCanSpinResponsePayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (context.client().player == null) return;
                        if (!(context.client().player.currentScreenHandler instanceof CasinoScreenHandler handler)) return;
                        handler.setCanSpin(payload.allowed(), payload.price(), payload.balance(), payload.locked());
                        if (context.client().currentScreen instanceof com.lenemon.client.casino.screen.CasinoScreen screen) {
                            screen.setSpinEnabled(payload.allowed(), payload.balance(), payload.price());
                        }
                    });
                }
        );

// Spin result : le serveur résout après l'anim
        ClientPlayNetworking.registerGlobalReceiver(
                com.lenemon.casino.network.CasinoSpinResultPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (!(context.client().currentScreen instanceof com.lenemon.client.casino.screen.CasinoScreen screen)) return;
                        screen.onSpinResult(payload.win(), payload.message());
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                com.lenemon.casino.network.CasinoPokemonDataPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (!(context.client().player.currentScreenHandler instanceof CasinoScreenHandler handler)) return;
                        handler.setPokemonRenderData(payload.species(), payload.aspects(),
                                payload.winChance(), payload.displayName(),
                                payload.nature(), payload.ivs());

                        if (context.client().currentScreen instanceof com.lenemon.client.casino.screen.CasinoScreen screen) {
                            screen.updatePokemonModel(payload.species(), payload.aspects());
                            screen.updatePokemonInfo(payload.winChance(), payload.displayName(),
                                    payload.nature(), payload.ivs(), payload.aspects());
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                com.lenemon.casino.network.CasinoOwnerDataPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        context.client().setScreen(
                                new com.lenemon.client.casino.screen.CasinoOwnerScreen(payload)
                        );
                    });
                }
        );

        // Receivers S2C menu
        ClientPlayNetworking.registerGlobalReceiver(MenuOpenPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> ctx.client().setScreen(new MenuScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(TpMenuOpenPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> ctx.client().setScreen(new TpMenuScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(HunterMenuOpenPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> ctx.client().setScreen(new HunterMenuScreen(payload))));

        // Receivers S2C shop
        ClientPlayNetworking.registerGlobalReceiver(ShopOpenPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> ctx.client().setScreen(new ShopScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(ShopCategoryOpenPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> ctx.client().setScreen(new ShopCategoryScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(ShopBalanceUpdatePayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> {
                    if (ctx.client().currentScreen instanceof ShopCategoryScreen screen) {
                        screen.updateBalance(payload.balance());
                    }
                }));

        // AH receivers S2C
        ClientPlayNetworking.registerGlobalReceiver(com.lenemon.network.ah.AhBrowsePayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> ctx.client().setScreen(new com.lenemon.client.ah.screen.AhBrowseScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(com.lenemon.network.ah.AhMyListingsPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> ctx.client().setScreen(new com.lenemon.client.ah.screen.AhMyListingsScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(com.lenemon.network.ah.AhSellItemPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> ctx.client().setScreen(new com.lenemon.client.ah.screen.AhSellItemScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(com.lenemon.network.ah.AhSellPokemonPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> ctx.client().setScreen(new com.lenemon.client.ah.screen.AhSellPokemonScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(com.lenemon.network.ah.AhPriceInfoPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> {
                    net.minecraft.client.gui.screen.Screen screen = ctx.client().currentScreen;
                    if (screen instanceof com.lenemon.client.ah.screen.AhSellItemScreen s) {
                        s.updatePriceInfo(payload.avgListedPrice(), payload.avgSoldPrice());
                    } else if (screen instanceof com.lenemon.client.ah.screen.AhSellPokemonScreen s) {
                        s.updatePriceInfo(payload.avgListedPrice(), payload.avgSoldPrice());
                    }
                }));

    }

    /**
     * Gets effects.
     *
     * @param playerUuid the player uuid
     * @return the effects
     */
    public static List<EffectConfig> getEffects(UUID playerUuid) {
        return playerEffects.getOrDefault(playerUuid, List.of());
    }

    /**
     * Nettoyer quand un joueur se déconnecte  @param playerUuid the player uuid
     *
     * @param playerUuid the player uuid
     */
    public static void clearPlayer(UUID playerUuid) {
        playerEffects.remove(playerUuid);
    }
}