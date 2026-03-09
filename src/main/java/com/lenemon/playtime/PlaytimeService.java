package com.lenemon.playtime;

import com.lenemon.network.playtime.PlaytimeOpenPayload;
import com.lenemon.network.playtime.PlaytimeTierDto;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PlaytimeService {

    private static final String PREFIX = "§6[Playtime] §r";

    private PlaytimeService() {}

    public static void sendOpen(ServerPlayerEntity player) {
        syncPlayer(player);
        ServerPlayNetworking.send(player, buildPayload(player));
    }

    public static void claim(ServerPlayerEntity player, String tierId) {
        PlaytimeConfig.RootConfig config = PlaytimeConfig.get();
        PlaytimeConfig.TierConfig tier = config.tiers.stream()
                .filter(entry -> entry.id.equals(tierId))
                .findFirst()
                .orElse(null);

        if (tier == null) {
            player.sendMessage(Text.literal(PREFIX + "§cPalier introuvable."), false);
            sendOpen(player);
            return;
        }

        PlaytimeWorldData data = PlaytimeWorldData.get(player.getServer().getOverworld());
        long playtimeTicks = syncPlayer(player);
        long requiredTicks = hoursToTicks(tier.hoursRequired);

        if (data.isClaimed(player.getUuid(), tier.id)) {
            player.sendMessage(Text.literal(PREFIX + "§7Cette récompense a déjà été récupérée."), false);
            sendOpen(player);
            return;
        }
        if (playtimeTicks < requiredTicks) {
            long missingSeconds = Math.max(0L, (requiredTicks - playtimeTicks) / 20L);
            player.sendMessage(Text.literal(PREFIX + "§cPalier non débloqué. Il manque §e" + formatDuration(missingSeconds) + "§c."), false);
            sendOpen(player);
            return;
        }

        giveRewards(player, tier);
        data.markClaimed(player.getUuid(), tier.id);

        player.sendMessage(Text.literal(PREFIX + "§aRécompense du palier §e" + tier.label + " §arécupérée !"), false);
        sendOpen(player);
    }

    public static long syncPlayer(ServerPlayerEntity player) {
        PlaytimeWorldData data = PlaytimeWorldData.get(player.getServer().getOverworld());
        long statTicks = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
        return data.syncPlaytime(player.getUuid(), statTicks);
    }

    public static int resetClaims(net.minecraft.server.MinecraftServer server, UUID uuid, String tierId) {
        PlaytimeWorldData data = PlaytimeWorldData.get(server.getOverworld());
        if ("all".equalsIgnoreCase(tierId)) {
            return data.clearClaims(uuid);
        }
        return data.unclaim(uuid, tierId) ? 1 : 0;
    }

    public static String formatDuration(long totalSeconds) {
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        if (days > 0) return days + "j " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return Math.max(0L, minutes) + "m";
    }

    public static long hoursToTicks(int hours) {
        return hours * 60L * 60L * 20L;
    }

    public static String resolveDisplayName(PlaytimeConfig.ItemRewardConfig reward) {
        if (reward.displayName != null && !reward.displayName.isBlank()) return reward.displayName;
        if (reward.customName != null && !reward.customName.isBlank()) return reward.customName;
        String path = reward.itemId;
        int colon = path.indexOf(':');
        if (colon >= 0) path = path.substring(colon + 1);
        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return builder.isEmpty() ? reward.itemId : builder.toString();
    }

    private static PlaytimeOpenPayload buildPayload(ServerPlayerEntity player) {
        PlaytimeConfig.RootConfig config = PlaytimeConfig.get();
        PlaytimeWorldData data = PlaytimeWorldData.get(player.getServer().getOverworld());
        long playtimeTicks = data.getPlaytimeTicks(player.getUuid());

        List<PlaytimeTierDto> tiers = new ArrayList<>();
        for (PlaytimeConfig.TierConfig tier : config.tiers) {
            tiers.add(new PlaytimeTierDto(
                    tier.id,
                    tier.label,
                    tier.hoursRequired,
                    data.isClaimed(player.getUuid(), tier.id),
                    buildRewardLines(tier)
            ));
        }

        return new PlaytimeOpenPayload(
                playtimeTicks,
                data.getClaimedCount(player.getUuid(), config.tiers.stream().map(entry -> entry.id).toList()),
                tiers
        );
    }

    private static List<String> buildRewardLines(PlaytimeConfig.TierConfig tier) {
        List<String> lines = new ArrayList<>();
        for (PlaytimeConfig.ItemRewardConfig item : tier.rewards.items) {
            lines.add("§f" + item.count + "x §b" + resolveDisplayName(item));
        }
        for (PlaytimeConfig.CommandRewardConfig command : tier.rewards.commands) {
            lines.add("§d" + resolveDisplayName(command));
        }
        return lines;
    }

    private static void giveRewards(ServerPlayerEntity player, PlaytimeConfig.TierConfig tier) {
        for (PlaytimeConfig.ItemRewardConfig itemReward : tier.rewards.items) {
            giveItemReward(player, itemReward);
        }
        for (PlaytimeConfig.CommandRewardConfig command : tier.rewards.commands) {
            String resolved = command.command
                    .replace("%player%", player.getName().getString())
                    .replace("{player}", player.getName().getString());
            if (resolved.startsWith("/")) resolved = resolved.substring(1);
            player.getServer().getCommandManager().executeWithPrefix(
                    player.getServer().getCommandSource().withMaxLevel(4),
                    resolved
            );
        }
    }

    public static String resolveDisplayName(PlaytimeConfig.CommandRewardConfig reward) {
        if (reward.displayName != null && !reward.displayName.isBlank()) return reward.displayName;
        return "Récompense spéciale";
    }

    private static void giveItemReward(ServerPlayerEntity player, PlaytimeConfig.ItemRewardConfig reward) {
        Item item;
        try {
            item = Registries.ITEM.get(Identifier.of(reward.itemId));
        } catch (Exception e) {
            System.err.println("[Playtime] Item invalide ignoré : " + reward.itemId);
            return;
        }

        if (item == null || item == net.minecraft.item.Items.AIR) {
            System.err.println("[Playtime] Item invalide ignoré : " + reward.itemId);
            return;
        }

        int remaining = reward.count;
        int maxStack = Math.max(1, item.getMaxCount());
        while (remaining > 0) {
            int give = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(item, give);
            if (reward.customName != null && !reward.customName.isBlank()) {
                stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(reward.customName));
            }
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
            remaining -= give;
        }
    }
}
