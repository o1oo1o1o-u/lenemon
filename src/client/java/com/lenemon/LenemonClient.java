package com.lenemon;

import com.lenemon.armor.config.EffectConfig;
import com.lenemon.client.casino.screen.CasinoScreen;
import com.lenemon.casino.screen.CasinoScreenRegistry;
import com.lenemon.client.effects.ParticleArmorEffect;
import com.lenemon.client.hud.HudEditCommand;
import com.lenemon.client.hud.HudRenderer;
import com.lenemon.client.network.LenemonNetworkClient;
import com.lenemon.client.renderer.ExcaveonRenderer;
import com.lenemon.client.renderer.ItemDespawnRenderer;
import com.lenemon.client.renderer.armor.DevArmorRenderer;
import com.lenemon.client.renderer.armor.RayArmorRenderer;
import com.lenemon.guieditor.GuiEditorClient;
import com.lenemon.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import java.util.List;

/**
 * The type Lenemon client.
 */
public class LenemonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HudRenderer.register();
        // A décommenter pour activer les commandes de config /lenemonhud
        HudEditCommand.register();
        ItemDespawnRenderer.register();

        //GuiEditorClient.init();
        // Provider DEV
        GeoRenderProvider devProvider = new GeoRenderProvider() {
            private DevArmorRenderer renderer;

            @Override
            public <T extends LivingEntity> GeoArmorRenderer<?> getGeoArmorRenderer(
                    T livingEntity, ItemStack itemStack,
                    EquipmentSlot equipmentSlot, BipedEntityModel<T> original) {
                if (this.renderer == null) this.renderer = new DevArmorRenderer();
                return this.renderer;
            }
        };

        // Provider RAY
        GeoRenderProvider rayProvider = new GeoRenderProvider() {
            private RayArmorRenderer renderer;

            @Override
            public <T extends LivingEntity> GeoArmorRenderer<?> getGeoArmorRenderer(
                    T livingEntity, ItemStack itemStack,
                    EquipmentSlot equipmentSlot, BipedEntityModel<T> original) {
                if (this.renderer == null) this.renderer = new RayArmorRenderer();
                return this.renderer;
            }
        };

        ModItems.DEV_HELMET.setRenderProvider(devProvider);
        ModItems.DEV_CHESTPLATE.setRenderProvider(devProvider);
        ModItems.DEV_LEGGINGS.setRenderProvider(devProvider);
        ModItems.DEV_BOOTS.setRenderProvider(devProvider);

        ModItems.RAY_HELMET.setRenderProvider(rayProvider);
        ModItems.RAY_CHESTPLATE.setRenderProvider(rayProvider);
        ModItems.RAY_LEGGINGS.setRenderProvider(rayProvider);
        ModItems.RAY_BOOTS.setRenderProvider(rayProvider);

        LenemonNetworkClient.register();




        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;

            // Appliquer les effets pour tous les joueurs visibles
            client.world.getPlayers().forEach(player -> {
                List<EffectConfig> effects = LenemonNetworkClient.getEffects(player.getUuid());
                for (EffectConfig effectConfig : effects) {
                    if (!effectConfig.type.equalsIgnoreCase("particles")) continue;
                    new ParticleArmorEffect(effectConfig.particle, effectConfig.density, effectConfig.radius)
                            .onClientTick(client.world, player.getPos());
                }
            });
        });

        GeoRenderProvider excaveonProvider = new GeoRenderProvider() {
            private ExcaveonRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getGeoItemRenderer() {
                if (this.renderer == null) this.renderer = new ExcaveonRenderer();
                return this.renderer;
            }
        };
        ModItems.EXCAVEON.setRenderProvider(excaveonProvider);

        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.EXCAVEON, (stack, mode, matrices, vertexConsumers, light, overlay) -> {
            BuiltinModelItemRenderer renderer = GeoRenderProvider.of(ModItems.EXCAVEON).getGeoItemRenderer();
            if (renderer != null) {
                renderer.render(stack, mode, matrices, vertexConsumers, light, overlay);
            }
        });


        HandledScreens.register(CasinoScreenRegistry.CASINO_SCREEN_HANDLER, CasinoScreen::new);



    }
}