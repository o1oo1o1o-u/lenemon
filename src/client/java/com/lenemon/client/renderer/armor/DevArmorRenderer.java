package com.lenemon.client.renderer.armor;

import com.lenemon.item.armor.DevArmor;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * The type Dev armor renderer.
 */
public class DevArmorRenderer extends GeoArmorRenderer<DevArmor> {

    /**
     * Instantiates a new Dev armor renderer.
     */
    public DevArmorRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.of("lenemon", "armor/dev_armor")));
    }
}