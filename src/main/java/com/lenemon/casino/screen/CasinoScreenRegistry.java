package com.lenemon.casino.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

/**
 * The type Casino screen registry.
 */
public final class CasinoScreenRegistry {

    private CasinoScreenRegistry() {}

    /**
     * The Casino screen handler.
     */
    public static ScreenHandlerType<CasinoScreenHandler> CASINO_SCREEN_HANDLER;

    /**
     * Register.
     */
    public static void register() {
        CASINO_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of("lenemon", "casino"),
                new ScreenHandlerType<>(CasinoScreenHandler::new, FeatureSet.empty())
        );
    }
}