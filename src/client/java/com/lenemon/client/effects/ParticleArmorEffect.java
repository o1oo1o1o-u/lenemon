package com.lenemon.client.effects;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;

/**
 * The type Particle armor effect.
 */
public class ParticleArmorEffect {

    private final net.minecraft.particle.ParticleEffect particleType;
    private final int density;
    private final double radius;

    /**
     * Instantiates a new Particle armor effect.
     *
     * @param particleName the particle name
     * @param density      the density
     * @param radius       the radius
     */
    public ParticleArmorEffect(String particleName, int density, double radius) {
        this.particleType = parseParticle(particleName);
        this.density = density;
        this.radius = radius;
    }

    /**
     * On client tick.
     *
     * @param world the world
     * @param pos   the pos
     */
    public void onClientTick(ClientWorld world, Vec3d pos) {
        for (int i = 0; i < density; i++) {
            double angle = Math.random() * Math.PI * 2;
            double height = (Math.random() - 0.5) * 2;
            double x = pos.x + Math.cos(angle) * radius;
            double y = pos.y + 1.0 + height;
            double z = pos.z + Math.sin(angle) * radius;

            world.addParticle(particleType, x, y, z, 0, 0, 0);
        }
    }

    private static net.minecraft.particle.ParticleEffect parseParticle(String name) {
        return switch (name.toUpperCase()) {
            case "ELECTRIC_SPARK" -> ParticleTypes.ELECTRIC_SPARK;
            case "FLAME" -> ParticleTypes.FLAME;
            case "DRAGON_BREATH" -> ParticleTypes.DRAGON_BREATH;
            case "PORTAL" -> ParticleTypes.PORTAL;
            case "ENCHANT" -> ParticleTypes.ENCHANT;
            case "END_ROD" -> ParticleTypes.END_ROD;
            case "SOUL_FIRE_FLAME" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "SNOWFLAKE" -> ParticleTypes.SNOWFLAKE;
            case "CLOUD" -> ParticleTypes.CLOUD;
            case "HEART" -> ParticleTypes.HEART;
            case "WITCH" -> ParticleTypes.WITCH;
            default -> ParticleTypes.ELECTRIC_SPARK;
        };
    }
}