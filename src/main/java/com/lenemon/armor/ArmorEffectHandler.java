package com.lenemon.armor;

import com.lenemon.armor.effects.ArmorEffect;
import com.lenemon.armor.sets.AbstractArmorSet;
import com.lenemon.armor.sets.DevArmorSet;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory;
import com.lenemon.armor.sets.RayArmorSet;
import com.lenemon.network.LenemonNetwork;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Armor effect handler.
 */
public class ArmorEffectHandler {

    /**
     * The constant ARMOR_SETS.
     */
// Rendre ARMOR_SETS accessible
    public static final List<ArmorSet> ARMOR_SETS = List.of(
            new DevArmorSet(),
            new RayArmorSet()
    );

    private static final Map<UUID, Map<ArmorSet, Boolean>> playerSetStates = new HashMap<>();
    private static final Map<UUID, Map<ArmorSet, Boolean[]>> playerPieceStates = new HashMap<>();
// Boolean[4] = [helmet, chestplate, leggings, boots]

    /**
     * Gets set.
     *
     * @param <T>   the type parameter
     * @param clazz the clazz
     * @return the set
     */
    @SuppressWarnings("unchecked")
    public static <T extends ArmorSet> T getSet(Class<T> clazz) {
        return (T) ARMOR_SETS.stream()
                .filter(s -> s.getClass() == clazz)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Set non trouvé : " + clazz.getSimpleName()));
    }

    /**
     * Register.
     */
    public static void register() {
        // Enregistre une influence par set pour chaque joueur
        for (ArmorSet set : ARMOR_SETS) {
            PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(player -> {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    return new BaseSpawnInfluence(serverPlayer, set);
                }
                return null;
            });
        }

        // Messages d'activation/désactivation
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Map<ArmorSet, Boolean> states = playerSetStates.computeIfAbsent(
                        player.getUuid(), k -> new HashMap<>()
                );

                for (ArmorSet set : ARMOR_SETS) {
                    boolean isWearing = set.isWearing(player);
                    boolean wasWearing = states.getOrDefault(set, false);

                    if (!wasWearing && isWearing) {
                        states.put(set, true);
                        player.sendMessage(set.getActivationMessage(), false);
                        LenemonNetwork.sendArmorEffects(player);
                    } else if (wasWearing && !isWearing) {
                        states.put(set, false);
                        player.sendMessage(set.getDeactivationMessage(), false);
                        LenemonNetwork.sendArmorEffects(player);
                        // Retirer TOUS les effets de potion
                        set.getAllPieceEffects().forEach(effect -> effect.onRemove(player));
                        ArmorEffect bonus = set.getSetBonusEffect();
                        if (bonus != null) bonus.onRemove(player);
                    }
                }





                for (ArmorSet set : ARMOR_SETS) {
                    if (!set.isEnabled()) continue;

                    // Effets cosmétiques (glow etc.)
                    if (set.isWearing(player)) {
                        set.getEffects().forEach(effect -> effect.onTick(player));
                    }

                    // Effets par pièce (chaque pièce individuellement)
                    set.getPieceEffects(player).forEach(effect -> effect.onTick(player));

                    // Bonus de set complet
                    if (set.isWearing(player)) {
                        ArmorEffect bonus = set.getSetBonusEffect();
                        if (bonus != null) bonus.onTick(player);
                    }
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            server.execute(() -> {
                LenemonNetwork.sendArmorEffects(player);
                LenemonNetwork.sendExcaveonConfig(player);
            });
        });
    }
}