package com.lenemon.clan;

import com.lenemon.compat.LuckPermsCompat;
import com.lenemon.network.clan.ClanActionPayload;
import com.lenemon.network.clan.ClanGuiPayload;
import com.lenemon.util.EconomyHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler serveur pour les actions C2S du GUI de clan.
 * Dispatch les actions recues depuis ClanActionPayload.
 */
public class ClanGuiHandler {

    private static final String ERROR = "§c[Clan] §r";

    private ClanGuiHandler() {}

    /**
     * Envoie les donnees du clan au joueur pour ouvrir le GUI.
     * A appeler depuis le thread serveur.
     */
    public static void sendGuiOpen(ServerPlayerEntity player) {
        Clan clan = ClanWorldData.getClanOf(player.getUuid());
        if (clan == null) {
            player.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return;
        }

        ClanGuiPayload payload = buildPayload(clan, player);
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * Handle le payload C2S d'action depuis le GUI.
     * Doit etre appele dans server().execute() (thread safety).
     */
    public static void handle(ClanActionPayload payload, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = ctx.server();
            String action = payload.action();

            if (action.equals("open_gui")) {
                sendGuiOpen(player);
                return;
            }
            if (action.equals("buy_level")) {
                ClanClaimHandler.handleBuyLevel(player, server);
                sendGuiOpen(player);
                return;
            }
            if (action.equals("disband")) {
                ClanManager.disbandClan(player, server);
                return;
            }
            if (action.equals("leave")) {
                ClanManager.leaveClan(player, server);
                return;
            }
            if (action.startsWith("kick:")) {
                String targetUuidStr = action.substring(5);
                handleKickByUuid(player, targetUuidStr, server);
                sendGuiOpen(player); // refresh GUI
                return;
            }
            if (action.startsWith("promote:")) {
                String targetUuidStr = action.substring(8);
                handlePromoteByUuid(player, targetUuidStr, server);
                sendGuiOpen(player); // refresh GUI
                return;
            }
            if (action.startsWith("demote:")) {
                String targetUuidStr = action.substring(7);
                handleDemoteByUuid(player, targetUuidStr, server);
                sendGuiOpen(player); // refresh GUI
                return;
            }
            // ── Gestion des rangs / configuration avancee ────────────────────────
            if (action.startsWith("rank_set_limit:")) {
                // rank_set_limit:<rankId>:<amount>   (-1 = illimite)
                String[] parts = action.substring(15).split(":", 2);
                if (parts.length == 2) {
                    try {
                        long limit = Long.parseLong(parts[1]);
                        Clan clan = ClanWorldData.getClanOf(player.getUuid());
                        if (clan != null && ClanManager.hasOwnerPrivileges(clan, player.getUuid())) {
                            // -1 = illimite, sinon clamp a >= 0
                            long sanitized = (limit == -1L) ? -1L : Math.max(0L, limit);
                            ClanWorldData.setRankWithdrawLimit(clan.id, parts[0], sanitized);
                        }
                        // Pas de sendGuiOpen : le client gere localement
                    } catch (NumberFormatException e) {
                        player.sendMessage(Text.literal(ERROR + "Montant invalide."), false);
                    }
                }
                sendGuiOpen(player); // refresh pour syncer les limites dans ClanBankConfigScreen
                return;
            }
            if (action.startsWith("rank_add:")) {
                // rank_add:<name>:<colorCode>
                String rest = action.substring(9);
                int sep = rest.lastIndexOf(":");
                if (sep > 0) {
                    String name      = rest.substring(0, sep);
                    String colorCode = rest.substring(sep + 1);
                    Clan clan = ClanWorldData.getClanOf(player.getUuid());
                    if (clan != null && ClanManager.hasOwnerPrivileges(clan, player.getUuid())) {
                        if (!ClanWorldData.addRank(clan.id, name, colorCode)) {
                            player.sendMessage(Text.literal(ERROR + "Limite de " + ClanRank.MAX_RANKS + " rangs atteinte."), false);
                        }
                    }
                    sendGuiOpen(player);
                }
                return;
            }
            if (action.startsWith("rank_remove:")) {
                String rankId = action.substring(12);
                Clan clan = ClanWorldData.getClanOf(player.getUuid());
                if (clan != null && ClanManager.hasOwnerPrivileges(clan, player.getUuid())) {
                    if (!ClanWorldData.removeRank(clan.id, rankId)) {
                        player.sendMessage(Text.literal(ERROR + "Ce rang ne peut pas etre supprime."), false);
                    }
                }
                sendGuiOpen(player);
                return;
            }
            if (action.startsWith("rank_rename:")) {
                // rank_rename:<rankId>:<newName>
                String rest = action.substring(12);
                int sep = rest.indexOf(":");
                if (sep > 0) {
                    String rankId  = rest.substring(0, sep);
                    String newName = rest.substring(sep + 1).trim();
                    if (!newName.isEmpty() && newName.length() <= 24) {
                        Clan clan = ClanWorldData.getClanOf(player.getUuid());
                        if (clan != null && ClanManager.hasOwnerPrivileges(clan, player.getUuid())) {
                            ClanWorldData.renameRank(clan.id, rankId, newName);
                        }
                    }
                }
                sendGuiOpen(player); // refresh pour syncer le nom dans ClanBankConfigScreen
                return;
            }
            if (action.startsWith("rank_set_color:")) {
                // rank_set_color:<rankId>:<colorCode>
                String rest = action.substring(15);
                int sep = rest.indexOf(":");
                if (sep > 0) {
                    String rankId    = rest.substring(0, sep);
                    String colorCode = rest.substring(sep + 1);
                    Clan clan = ClanWorldData.getClanOf(player.getUuid());
                    if (clan != null && ClanManager.hasOwnerPrivileges(clan, player.getUuid())) {
                        ClanWorldData.setRankColor(clan.id, rankId, colorCode);
                    }
                }
                sendGuiOpen(player); // refresh pour syncer la couleur
                return;
            }
            if (action.startsWith("rank_toggle_owner_privileges:")) {
                String rest = action.substring("rank_toggle_owner_privileges:".length());
                int sep = rest.indexOf(":");
                if (sep > 0) {
                    String rankId = rest.substring(0, sep);
                    boolean enabled = Boolean.parseBoolean(rest.substring(sep + 1));
                    Clan clan = ClanWorldData.getClanOf(player.getUuid());
                    if (clan != null && ClanManager.hasOwnerPrivileges(clan, player.getUuid())) {
                        ClanWorldData.setRankOwnerPrivileges(clan.id, rankId, enabled);
                    }
                }
                sendGuiOpen(player);
                return;
            }
            if (action.startsWith("rank_reorder:")) {
                // rank_reorder:<rankId>:<up|down>
                String rest = action.substring(13);
                int sep = rest.lastIndexOf(":");
                if (sep > 0) {
                    String rankId   = rest.substring(0, sep);
                    String direction = rest.substring(sep + 1);
                    boolean moveUp  = "up".equals(direction);
                    Clan clan = ClanWorldData.getClanOf(player.getUuid());
                    if (clan != null && ClanManager.hasOwnerPrivileges(clan, player.getUuid())) {
                        ClanWorldData.reorderRank(clan.id, rankId, moveUp);
                    }
                    sendGuiOpen(player);
                }
                return;
            }
            if (action.startsWith("perm_set:")) {
                // perm_set:<action>:<roleId>
                String rest = action.substring(9);
                int sep = rest.indexOf(":");
                if (sep > 0) {
                    String permAction  = rest.substring(0, sep);
                    String requiredRole = rest.substring(sep + 1);
                    Clan clan = ClanWorldData.getClanOf(player.getUuid());
                    if (clan != null && ClanManager.hasOwnerPrivileges(clan, player.getUuid())) {
                        // Valider que le rankId existe dans le clan
                        boolean validRankId = clan.ranks.stream().anyMatch(r -> r.id.equals(requiredRole));
                        if (validRankId) {
                            ClanWorldData.setPermission(clan.id, permAction, requiredRole);
                        }
                    }
                    sendGuiOpen(player);
                }
                return;
            }
            if (action.startsWith("set_enter_message:")) {
                ClanManager.setTerritoryMessage(player, "enter", action.substring("set_enter_message:".length()));
                sendGuiOpen(player);
                return;
            }
            if (action.startsWith("set_leave_message:")) {
                ClanManager.setTerritoryMessage(player, "leave", action.substring("set_leave_message:".length()));
                sendGuiOpen(player);
                return;
            }
            if (action.startsWith("deposit:")) {
                try {
                    long amount = Long.parseLong(action.substring(8));
                    ClanManager.bankDeposit(player, amount);
                    sendGuiOpen(player); // refresh GUI
                } catch (NumberFormatException e) {
                    player.sendMessage(Text.literal(ERROR + "Montant invalide."), false);
                }
                return;
            }
            if (action.startsWith("withdraw:")) {
                try {
                    long amount = Long.parseLong(action.substring(9));
                    ClanManager.bankWithdraw(player, amount);
                    sendGuiOpen(player); // refresh GUI
                } catch (NumberFormatException e) {
                    player.sendMessage(Text.literal(ERROR + "Montant invalide."), false);
                }
                return;
            }

            player.sendMessage(Text.literal(ERROR + "Action inconnue : " + action), false);
        });
    }

    // -------------------------------------------------------------------------
    // Helpers prives
    // -------------------------------------------------------------------------

    private static ClanGuiPayload buildPayload(Clan clan, ServerPlayerEntity viewer) {
        MinecraftServer server = viewer.getServer();
        ClanRole viewerRole = clan.getRole(viewer.getUuid());
        String viewerRankId = ClanManager.getMemberRankId(clan, viewer.getUuid());
        long xpNext = ClanConfig.get().xpRequiredForLevel(clan.level + 1);

        List<ClanGuiPayload.MemberDto> memberDtos = new ArrayList<>();
        for (Map.Entry<UUID, ClanRole> entry : clan.members.entrySet()) {
            UUID memberUuid = entry.getKey();
            String name = ClanManager.resolvePlayerName(memberUuid, server);
            long totalContributed = clan.memberContributions.getOrDefault(memberUuid, 0L);

            // Determiner si le joueur est en ligne
            boolean isOnline = server.getPlayerManager().getPlayer(memberUuid) != null;
            long lastSeen = isOnline ? 0L : ClanWorldData.getLastSeen(memberUuid);

            // ID du rang affiche (custom ou systeme)
            String rankId = clan.memberRanks.getOrDefault(memberUuid, switch (entry.getValue()) {
                case OWNER -> "owner"; case OFFICER -> "officer"; default -> "member";
            });

            memberDtos.add(new ClanGuiPayload.MemberDto(
                    memberUuid.toString(),
                    name,
                    entry.getValue().name(),
                    totalContributed,
                    lastSeen,
                    isOnline,
                    rankId
            ));
        }

        List<ClanGuiPayload.RankDto> rankDtos = clan.ranks.stream()
                .sorted(java.util.Comparator.comparingInt(r -> r.sortOrder))
                .map(r -> new ClanGuiPayload.RankDto(r.id, r.name, r.colorCode, r.withdrawLimit, r.sortOrder, r.ownerPrivileges))
                .collect(Collectors.toList());

        return new ClanGuiPayload(
                clan.id.toString(),
                clan.name,
                clan.tag,
                clan.level,
                clan.xp,
                xpNext,
                clan.bankBalance,
                clan.createdAt,
                viewerRole != null ? viewerRole.name() : "MEMBER",
                viewerRankId,
                memberDtos,
                rankDtos,
                clan.permissions != null ? new java.util.HashMap<>(clan.permissions) : java.util.Map.of(),
                clan.enterMessage != null ? clan.enterMessage : "",
                clan.leaveMessage != null ? clan.leaveMessage : "",
                clan.clanLevel,
                clan.maxClaims(),
                clan.claimedChunks.size(),
                ClanConfig.get().levelUpPrice(clan.clanLevel)
        );
    }

    private static void handleKickByUuid(ServerPlayerEntity actor, String targetUuidStr, MinecraftServer server) {
        try {
            UUID targetUuid = UUID.fromString(targetUuidStr);
            Clan clan = ClanWorldData.getClanOf(actor.getUuid());
            if (clan == null) return;

            ClanRole actorRole = clan.getRole(actor.getUuid());
            boolean actorHasOwnerPrivileges = ClanManager.hasOwnerPrivileges(clan, actor.getUuid());
            if (actorRole == null || actorRole == ClanRole.MEMBER) {
                actor.sendMessage(Text.literal(ERROR + "Permission insuffisante."), false);
                return;
            }
            ClanRole targetRole = clan.getRole(targetUuid);
            if (targetRole == null) {
                actor.sendMessage(Text.literal(ERROR + "Ce joueur n'est pas dans le clan."), false);
                return;
            }
            if (targetUuid.equals(clan.ownerUUID)) {
                actor.sendMessage(Text.literal(ERROR + "Tu ne peux pas kicker le proprietaire."), false);
                return;
            }
            if (!actorHasOwnerPrivileges && !actorRole.canModerate(targetRole)) {
                actor.sendMessage(Text.literal(ERROR + "Tu ne peux pas kicker ce joueur."), false);
                return;
            }

            String targetName = ClanManager.resolvePlayerName(targetUuid, server);
            ClanWorldData.removeMember(clan.id, targetUuid);
            LuckPermsCompat.clearClanSuffix(targetUuid);

            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
            if (target != null) {
                target.sendMessage(Text.literal(ERROR + "Tu as ete exclu du clan §e" + clan.name + "§r."), false);
            }
            actor.sendMessage(Text.literal("§6[Clan] §r§e" + targetName + " §ra ete exclu du clan."), false);

        } catch (IllegalArgumentException e) {
            actor.sendMessage(Text.literal(ERROR + "UUID invalide."), false);
        }
    }

    private static void handlePromoteByUuid(ServerPlayerEntity actor, String targetUuidStr, MinecraftServer server) {
        try {
            UUID targetUuid = UUID.fromString(targetUuidStr);
            ClanManager.promoteByUuid(actor, targetUuid, server);
            sendGuiOpen(actor);
        } catch (IllegalArgumentException e) {
            actor.sendMessage(Text.literal(ERROR + "UUID invalide."), false);
        }
    }

    private static void handleDemoteByUuid(ServerPlayerEntity actor, String targetUuidStr, MinecraftServer server) {
        try {
            UUID targetUuid = UUID.fromString(targetUuidStr);
            ClanManager.demoteByUuid(actor, targetUuid, server);
            sendGuiOpen(actor);
        } catch (IllegalArgumentException e) {
            actor.sendMessage(Text.literal(ERROR + "UUID invalide."), false);
        }
    }
}
