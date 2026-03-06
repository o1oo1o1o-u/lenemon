package com.lenemon.client.renderer;

import com.lenemon.item.pickaxe.ExcaveonPickaxe;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * The type Excaveon renderer.
 */
public class ExcaveonRenderer extends GeoItemRenderer<ExcaveonPickaxe> {

    /**
     * Instantiates a new Excaveon renderer.
     */
    public ExcaveonRenderer() {
        super(new GeoModel<ExcaveonPickaxe>() {
            @Override
            public Identifier getModelResource(ExcaveonPickaxe animatable) {
                return Identifier.of("lenemon", "geo/item/excaveon.geo.json");
            }

            @Override
            public Identifier getTextureResource(ExcaveonPickaxe animatable) {
                return Identifier.of("lenemon", "textures/item/nymphalie_pickaxe.png");
            }

            @Override
            public Identifier getAnimationResource(ExcaveonPickaxe animatable) {
                return Identifier.of("lenemon", "animations/item/nymphalie_pickaxe.animation.json");
            }
        });
    }
}