package com.lenemon.client.renderer.armor;

import com.lenemon.item.armor.DevArmor;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * The type Ray armor renderer.
 */
public class RayArmorRenderer extends GeoArmorRenderer<DevArmor> {

    /**
     * Instantiates a new Ray armor renderer.
     */
    public RayArmorRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.of("lenemon", "armor/ray_armor")));
    }
}