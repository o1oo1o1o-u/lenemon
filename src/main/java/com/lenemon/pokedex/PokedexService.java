package com.lenemon.pokedex;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.CaughtPercent;
import com.cobblemon.mod.common.api.pokedex.Dexes;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokedex.SeenPercent;
import com.cobblemon.mod.common.api.pokedex.def.PokedexDef;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.lenemon.network.pokedex.PokedexOpenPayload;
import com.lenemon.network.pokedex.PokedexRegionDto;
import com.lenemon.network.pokedex.PokedexRewardTierDto;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Logique serveur du système Pokédex Reward.
 * Calcule les % vus/capturés par région via l'API Cobblemon,
 * détermine les paliers atteignables, et distribue les récompenses.
 */
public class PokedexService {

    /** Régions dans l'ordre d'affichage. "national" = toutes régions. */
    private static final String[] REGION_IDS = {
            "national", "kanto", "johto", "hoenn", "sinnoh",
            "unova", "kalos", "alola", "galar", "hisui", "paldea", "unknown"
    };

    private static final Map<String, String> REGION_LABELS = new LinkedHashMap<>();
    static {
        REGION_LABELS.put("national", "National");
        REGION_LABELS.put("kanto",    "Kanto");
        REGION_LABELS.put("johto",    "Johto");
        REGION_LABELS.put("hoenn",    "Hoenn");
        REGION_LABELS.put("sinnoh",   "Sinnoh");
        REGION_LABELS.put("unova",    "Unova");
        REGION_LABELS.put("kalos",    "Kalos");
        REGION_LABELS.put("alola",    "Alola");
        REGION_LABELS.put("galar",    "Galar");
        REGION_LABELS.put("hisui",    "Hisui");
        REGION_LABELS.put("paldea",   "Paldea");
        REGION_LABELS.put("unknown",  "Inconnue");
    }

    // ── Envoi du payload ────────────────────────────────────────────────────

    public static void sendPokedexOpen(ServerPlayerEntity player) {
        PokedexOpenPayload payload = buildPayload(player);
        ServerPlayNetworking.send(player, payload);
    }

    // ── Construction du payload ─────────────────────────────────────────────

    private static PokedexOpenPayload buildPayload(ServerPlayerEntity player) {
        PokedexManager dexManager = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
        UUID uuid = player.getUuid();

        List<PokedexRegionDto> regionDtos = new ArrayList<>();
        for (String regionId : REGION_IDS) {
            Identifier dexId = Identifier.of("cobblemon", regionId);
            PokedexDef def = Dexes.INSTANCE.getDexEntryMap().get(dexId);
            if (def == null) continue;

            // Build entries map for the region
            Map<Identifier, PokedexEntry> entryMap = new LinkedHashMap<>();
            for (PokedexEntry entry : def.getEntries()) {
                entryMap.put(entry.getId(), entry);
            }

            float seenPct   = toPercent(SeenPercent.INSTANCE.calculate(dexManager, entryMap));
            float caughtPct = toPercent(CaughtPercent.INSTANCE.calculate(dexManager, entryMap));

            List<PokedexRewardTierDto> tiers = buildTiers(uuid, regionId, seenPct, caughtPct);
            String label = REGION_LABELS.getOrDefault(regionId, regionId);
            regionDtos.add(new PokedexRegionDto(regionId, label, seenPct, caughtPct, tiers));
        }

        return new PokedexOpenPayload(regionDtos);
    }

    private static List<PokedexRewardTierDto> buildTiers(UUID uuid, String regionId, float seenPct, float caughtPct) {
        List<PokedexRewardTierDto> list = new ArrayList<>();
        for (String type : List.of("caught", "seen")) {
            Map<Integer, PokedexRewardConfig.RewardEntry> tiers = PokedexRewardConfig.getTiers(regionId, type);
            for (Map.Entry<Integer, PokedexRewardConfig.RewardEntry> e : tiers.entrySet()) {
                int threshold = e.getKey();
                boolean claimed = PokedexClaimedStorage.isClaimed(uuid, regionId, type, threshold);
                list.add(new PokedexRewardTierDto(type, threshold, claimed, e.getValue().description()));
            }
        }
        // Sort: caught first, then seen; within each type by threshold ascending
        list.sort(Comparator.comparing(PokedexRewardTierDto::type).reversed()
                .thenComparingInt(PokedexRewardTierDto::threshold));
        return list;
    }

    // ── Claim ───────────────────────────────────────────────────────────────

    /**
     * Donne toutes les récompenses disponibles et non récupérées pour la région donnée.
     * Renvoie ensuite un payload mis à jour.
     */
    public static void claimRegion(ServerPlayerEntity player, String regionId) {
        PokedexManager dexManager = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
        UUID uuid = player.getUuid();

        Identifier dexId = Identifier.of("cobblemon", regionId);
        PokedexDef def = Dexes.INSTANCE.getDexEntryMap().get(dexId);
        if (def == null) {
            sendPokedexOpen(player);
            return;
        }

        Map<Identifier, PokedexEntry> entryMap = new LinkedHashMap<>();
        for (PokedexEntry entry : def.getEntries()) entryMap.put(entry.getId(), entry);

        float seenPct   = toPercent(SeenPercent.INSTANCE.calculate(dexManager, entryMap));
        float caughtPct = toPercent(CaughtPercent.INSTANCE.calculate(dexManager, entryMap));

        int claimedCount = 0;
        for (String type : List.of("caught", "seen")) {
            float pct = type.equals("caught") ? caughtPct : seenPct;
            Map<Integer, PokedexRewardConfig.RewardEntry> tiers = PokedexRewardConfig.getTiers(regionId, type);
            for (Map.Entry<Integer, PokedexRewardConfig.RewardEntry> e : tiers.entrySet()) {
                int threshold = e.getKey();
                if (pct >= threshold && !PokedexClaimedStorage.isClaimed(uuid, regionId, type, threshold)) {
                    giveReward(player, e.getValue(), regionId, type, threshold);
                    PokedexClaimedStorage.markClaimed(uuid, regionId, type, threshold);
                    claimedCount++;
                }
            }
        }

        if (claimedCount == 0) {
            player.sendMessage(Text.literal("§7Aucune récompense disponible pour cette région."), true);
        }

        // Refresh the GUI
        sendPokedexOpen(player);
    }

    // ── Récompenses ─────────────────────────────────────────────────────────

    private static void giveReward(ServerPlayerEntity player, PokedexRewardConfig.RewardEntry reward,
                                   String regionId, String type, int threshold) {
        // Money
        if (reward.money() > 0) {
            try {
                com.lenemon.util.EconomyHelper.credit(player, reward.money());
            } catch (Exception e) {
                player.sendMessage(Text.literal("§c[Pokédex] Erreur dépôt argent : " + e.getMessage()), false);
            }
        }

        // Items
        for (String itemId : reward.items()) {
            var item = Registries.ITEM.get(Identifier.of(itemId));
            if (item != null) {
                net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);
                if (!player.getInventory().insertStack(stack)) {
                    player.dropItem(stack, false);
                }
            }
        }

        // Commands
        String playerName = player.getName().getString();
        for (String cmd : reward.commands()) {
            String resolved = cmd.replace("{player}", playerName);
            player.getServer().getCommandManager().executeWithPrefix(
                    player.getServer().getCommandSource().withMaxLevel(4),
                    resolved
            );
        }

        String label = REGION_LABELS.getOrDefault(regionId, regionId);
        String typeLabel = type.equals("caught") ? "capturé" : "vu";
        player.sendMessage(Text.literal(
                "§a✦ Récompense Pokédex §f" + label + " §a(" + threshold + "% " + typeLabel + ") §areçue !"
        ), false);
    }

    // ── Util ────────────────────────────────────────────────────────────────

    private static float toPercent(Float f) {
        return f != null ? f : 0f;
    }
}
