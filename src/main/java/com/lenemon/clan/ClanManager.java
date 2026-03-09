package com.lenemon.clan;

import com.lenemon.compat.LuckPermsCompat;
import com.lenemon.util.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Logique metier du systeme de clan.
 * Toutes les methodes retournent un boolean (succes) et envoient
 * des messages de feedback au(x) joueur(s) concerne(s).
 *
 * Doit etre appele depuis le thread serveur uniquement.
 */
public class ClanManager {

    private static final String PREFIX = "§6[Clan] §r";
    private static final String ERROR  = "§c[Clan] §r";

    private ClanManager() {}

    // -------------------------------------------------------------------------
    // Creation / Dissolution
    // -------------------------------------------------------------------------

    /**
     * Cree un nouveau clan.
     * Conditions : joueur pas deja dans un clan, nom et tag disponibles,
     * longueurs respectees.
     */
    public static boolean createClan(ServerPlayerEntity player, String name, String tag) {
        UUID uuid = player.getUuid();
        ClanConfig cfg = ClanConfig.get();

        if (ClanWorldData.isInClan(uuid)) {
            player.sendMessage(Text.literal(ERROR + "Tu es deja dans un clan. Quitte-le d'abord."), false);
            return false;
        }
        if (name.length() > cfg.maxNameLength) {
            player.sendMessage(Text.literal(ERROR + "Nom trop long (max " + cfg.maxNameLength + " caracteres)."), false);
            return false;
        }
        if (tag.length() > cfg.maxTagLength || tag.isEmpty()) {
            player.sendMessage(Text.literal(ERROR + "Tag invalide (1-" + cfg.maxTagLength + " caracteres)."), false);
            return false;
        }
        if (!tag.matches("[A-Za-z0-9]+")) {
            player.sendMessage(Text.literal(ERROR + "Le tag ne peut contenir que des lettres et chiffres."), false);
            return false;
        }
        if (ClanWorldData.isNameTaken(name)) {
            player.sendMessage(Text.literal(ERROR + "Ce nom de clan est deja utilise."), false);
            return false;
        }
        if (ClanWorldData.isTagTaken(tag)) {
            player.sendMessage(Text.literal(ERROR + "Ce tag est deja utilise."), false);
            return false;
        }

        Clan clan = new Clan(UUID.randomUUID(), name, tag, uuid);
        ClanWorldData.addClan(clan);

        LuckPermsCompat.setClanSuffix(uuid, clan.tag);
        player.sendMessage(Text.literal(PREFIX + "Clan §e" + name + " §r[§b" + tag.toUpperCase() + "§r] cree avec succes !"), false);
        return true;
    }

    /**
     * Dissout un clan.
     * Seul l'owner peut dissoudre le clan.
     */
    public static boolean disbandClan(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        Clan clan = ClanWorldData.getClanOf(uuid);

        if (clan == null) {
            player.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return false;
        }
        if (clan.getRole(uuid) != ClanRole.OWNER) {
            player.sendMessage(Text.literal(ERROR + "Seul le proprietaire peut dissoudre le clan."), false);
            return false;
        }

        String clanName = clan.name;

        // Rembourser la banque au proprietaire si elle n'est pas vide
        if (clan.bankBalance > 0) {
            EconomyHelper.credit(uuid, clan.bankBalance);
            player.sendMessage(Text.literal(PREFIX + "§e" + clan.bankBalance + " §rcoins de la banque du clan te sont restitues."), false);
        }

        // Notifier tous les membres en ligne + effacer leur prefix LP
        for (UUID memberUUID : clan.members.keySet()) {
            LuckPermsCompat.clearClanSuffix(memberUUID);
            if (memberUUID.equals(uuid)) continue;
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberUUID);
            if (member != null) {
                member.sendMessage(Text.literal(ERROR + "Le clan §e" + clanName + " §ra ete dissout par son proprietaire."), false);
            }
        }

        ClanInviteSession.removeAllForClan(clan.id);
        ClanWorldData.removeClan(clan.id);
        player.sendMessage(Text.literal(PREFIX + "Clan §e" + clanName + " §rdissout."), false);
        return true;
    }

    static boolean hasOwnerPrivileges(Clan clan, UUID uuid) {
        if (clan == null) return false;
        if (uuid.equals(clan.ownerUUID)) return true;
        ClanRank rank = clan.getRankById(getMemberRankId(clan, uuid));
        return rank != null && rank.ownerPrivileges;
    }

    static boolean isActualOwner(Clan clan, UUID uuid) {
        return clan != null && uuid.equals(clan.ownerUUID);
    }

    // -------------------------------------------------------------------------
    // Invitation / Acceptation / Refus
    // -------------------------------------------------------------------------

    /**
     * Envoie une invitation a un joueur.
     * Seul owner ou officer peut inviter.
     */
    public static boolean invitePlayer(ServerPlayerEntity inviter, ServerPlayerEntity target, MinecraftServer server) {
        UUID inviterUUID = inviter.getUuid();
        UUID targetUUID = target.getUuid();
        Clan clan = ClanWorldData.getClanOf(inviterUUID);

        if (clan == null) {
            inviter.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return false;
        }
        ClanRole inviterRole = clan.getRole(inviterUUID);
        if (inviterRole == ClanRole.MEMBER) {
            inviter.sendMessage(Text.literal(ERROR + "Seuls les officers et le proprietaire peuvent inviter."), false);
            return false;
        }
        if (ClanWorldData.isInClan(targetUUID)) {
            inviter.sendMessage(Text.literal(ERROR + target.getName().getString() + " est deja dans un clan."), false);
            return false;
        }

        ClanConfig cfg = ClanConfig.get();
        if (clan.size() >= cfg.maxMembersForLevel(clan.level)) {
            inviter.sendMessage(Text.literal(ERROR + "Le clan est plein (max " + cfg.maxMembersForLevel(clan.level) + " membres pour le niveau " + clan.level + ")."), false);
            return false;
        }

        ClanInviteSession.add(targetUUID, clan.id);

        // Message a l'invite avec boutons cliquables
        MutableText acceptBtn = Text.literal("§a[Accepter]")
                .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan accept")));
        MutableText declineBtn = Text.literal("§c[Refuser]")
                .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan decline")));
        MutableText inviteMsg = Text.literal(PREFIX + "§e" + inviter.getName().getString() +
                " §rt'invite a rejoindre le clan §e" + clan.name +
                " §r[§b" + clan.tag + "§r]. ")
                .append(acceptBtn).append(Text.literal(" ")).append(declineBtn);
        target.sendMessage(inviteMsg, false);

        inviter.sendMessage(Text.literal(PREFIX + "Invitation envoyee a §e" + target.getName().getString() + "§r."), false);
        return true;
    }

    /**
     * Le joueur accepte son invitation en attente.
     */
    public static boolean acceptInvite(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();

        if (ClanWorldData.isInClan(uuid)) {
            player.sendMessage(Text.literal(ERROR + "Tu es deja dans un clan."), false);
            return false;
        }
        if (!ClanInviteSession.hasPending(uuid)) {
            player.sendMessage(Text.literal(ERROR + "Tu n'as aucune invitation en attente."), false);
            return false;
        }

        UUID clanId = ClanInviteSession.getPendingClanId(uuid);
        Clan clan = ClanWorldData.getById(clanId);
        if (clan == null) {
            ClanInviteSession.remove(uuid);
            player.sendMessage(Text.literal(ERROR + "Ce clan n'existe plus."), false);
            return false;
        }

        ClanConfig cfg = ClanConfig.get();
        if (clan.size() >= cfg.maxMembersForLevel(clan.level)) {
            ClanInviteSession.remove(uuid);
            player.sendMessage(Text.literal(ERROR + "Le clan est maintenant plein."), false);
            return false;
        }

        ClanInviteSession.remove(uuid);
        ClanWorldData.addMember(clanId, uuid, ClanRole.MEMBER);

        LuckPermsCompat.setClanSuffix(uuid, clan.tag);
        player.sendMessage(Text.literal(PREFIX + "Tu as rejoint le clan §e" + clan.name + " §r[§b" + clan.tag + "§r] !"), false);
        broadcastToClan(clan, PREFIX + "§e" + player.getName().getString() + " §ra rejoint le clan !", server, uuid);
        return true;
    }

    /**
     * Le joueur decline son invitation en attente.
     */
    public static boolean declineInvite(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        if (!ClanInviteSession.hasPending(uuid)) {
            player.sendMessage(Text.literal(ERROR + "Tu n'as aucune invitation en attente."), false);
            return false;
        }

        UUID clanId = ClanInviteSession.getPendingClanId(uuid);
        Clan clan = ClanWorldData.getById(clanId);
        ClanInviteSession.remove(uuid);

        player.sendMessage(Text.literal(PREFIX + "Invitation refusee."), false);
        if (clan != null) {
            broadcastToClan(clan, ERROR + "§e" + player.getName().getString() + " §ra refuse l'invitation.", player.getServer(), new UUID[0]);
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Kick / Leave
    // -------------------------------------------------------------------------

    /**
     * Kick un membre du clan.
     * Owner peut kicker officers et membres. Officers peuvent kicker membres.
     */
    public static boolean kickMember(ServerPlayerEntity actor, String targetName, MinecraftServer server) {
        UUID actorUUID = actor.getUuid();
        Clan clan = ClanWorldData.getClanOf(actorUUID);

        if (clan == null) {
            actor.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return false;
        }

        ClanRole actorRole = clan.getRole(actorUUID);
        boolean actorHasOwnerPrivileges = hasOwnerPrivileges(clan, actorUUID);
        if (actorRole == ClanRole.MEMBER) {
            actor.sendMessage(Text.literal(ERROR + "Tu n'as pas la permission de kicker des membres."), false);
            return false;
        }

        // Chercher la cible parmi les membres du clan
        UUID targetUUID = findMemberByName(clan, targetName, server);
        if (targetUUID == null) {
            actor.sendMessage(Text.literal(ERROR + "Joueur §e" + targetName + " §rintrouvable dans le clan."), false);
            return false;
        }
        if (targetUUID.equals(actorUUID)) {
            actor.sendMessage(Text.literal(ERROR + "Tu ne peux pas te kicker toi-meme."), false);
            return false;
        }

        ClanRole targetRole = clan.getRole(targetUUID);
        if (targetUUID.equals(clan.ownerUUID)) {
            actor.sendMessage(Text.literal(ERROR + "Tu ne peux pas exclure le proprietaire du clan."), false);
            return false;
        }
        if (!actorHasOwnerPrivileges && !actorRole.canModerate(targetRole)) {
            actor.sendMessage(Text.literal(ERROR + "Tu ne peux pas kicker un membre de rang superieur ou egal au tien."), false);
            return false;
        }

        ClanWorldData.removeMember(clan.id, targetUUID);
        LuckPermsCompat.clearClanSuffix(targetUUID);

        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUUID);
        if (target != null) {
            target.sendMessage(Text.literal(ERROR + "Tu as ete exclu du clan §e" + clan.name + "§r."), false);
        }
        actor.sendMessage(Text.literal(PREFIX + "§e" + targetName + " §ra ete exclu du clan."), false);
        broadcastToClan(clan, PREFIX + "§e" + targetName + " §ra ete exclu du clan.", server, actorUUID, targetUUID);
        return true;
    }

    /**
     * Le joueur quitte son clan.
     * L'owner ne peut pas quitter s'il y a d'autres membres (doit dissoudre ou transferer).
     */
    public static boolean leaveClan(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        Clan clan = ClanWorldData.getClanOf(uuid);

        if (clan == null) {
            player.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return false;
        }

        if (clan.getRole(uuid) == ClanRole.OWNER) {
            if (clan.size() > 1) {
                player.sendMessage(Text.literal(ERROR + "Tu es le proprietaire. Utilise §e/clan disband §rpour dissoudre, ou §e/clan promote §rpour transferer la propriete."), false);
                return false;
            }
            // Seul membre restant : dissoudre automatiquement
            return disbandClan(player, server);
        }

        String clanName = clan.name;
        ClanWorldData.removeMember(clan.id, uuid);
        LuckPermsCompat.clearClanSuffix(uuid);
        player.sendMessage(Text.literal(PREFIX + "Tu as quitte le clan §e" + clanName + "§r."), false);
        broadcastToClan(clan, PREFIX + "§e" + player.getName().getString() + " §ra quitte le clan.", server, uuid);
        return true;
    }

    // -------------------------------------------------------------------------
    // Promotion / Demote
    // -------------------------------------------------------------------------

    /**
     * Promeut un MEMBER en OFFICER, ou transfere la propriete (OFFICER -> OWNER).
     * Seul l'OWNER peut promouvoir.
     */
    public static boolean promote(ServerPlayerEntity actor, String targetName, MinecraftServer server) {
        UUID actorUUID = actor.getUuid();
        Clan clan = ClanWorldData.getClanOf(actorUUID);

        if (clan == null) {
            actor.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return false;
        }
        if (!hasOwnerPrivileges(clan, actorUUID)) {
            actor.sendMessage(Text.literal(ERROR + "Tu n'as pas la permission de promouvoir des membres."), false);
            return false;
        }

        UUID targetUUID = findMemberByName(clan, targetName, server);
        if (targetUUID == null) {
            actor.sendMessage(Text.literal(ERROR + "Joueur §e" + targetName + " §rintrouvable dans le clan."), false);
            return false;
        }
        if (targetUUID.equals(actorUUID)) {
            actor.sendMessage(Text.literal(ERROR + "Tu es deja proprietaire."), false);
            return false;
        }

        ClanRole currentRole = clan.getRole(targetUUID);
        ClanRole newRole;
        String actionMsg;

        if (currentRole == ClanRole.MEMBER) {
            newRole = ClanRole.OFFICER;
            actionMsg = "§e" + targetName + " §rest maintenant §6Officer§r.";
        } else if (currentRole == ClanRole.OFFICER) {
            if (!isActualOwner(clan, actorUUID)) {
                actor.sendMessage(Text.literal(ERROR + "Seul le proprietaire peut transferer la propriete du clan."), false);
                return false;
            }
            // Transfert de propriete : l'ancien owner devient officer
            ClanWorldData.setRole(clan.id, actorUUID, ClanRole.OFFICER);
            newRole = ClanRole.OWNER;
            actionMsg = "§e" + targetName + " §rest maintenant §cProprietaire §rdu clan. Tu es desormais §6Officer§r.";
        } else {
            actor.sendMessage(Text.literal(ERROR + targetName + " est deja au rang maximum."), false);
            return false;
        }

        ClanWorldData.setRole(clan.id, targetUUID, newRole);

        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUUID);
        if (target != null) {
            target.sendMessage(Text.literal(PREFIX + actionMsg), false);
        }
        broadcastToClan(clan, PREFIX + actionMsg, server, actorUUID, targetUUID);
        actor.sendMessage(Text.literal(PREFIX + actionMsg), false);
        return true;
    }

    /**
     * Retrograde un OFFICER en MEMBER.
     * Seul l'OWNER peut retrograder.
     */
    public static boolean demote(ServerPlayerEntity actor, String targetName, MinecraftServer server) {
        UUID actorUUID = actor.getUuid();
        Clan clan = ClanWorldData.getClanOf(actorUUID);

        if (clan == null) {
            actor.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return false;
        }
        if (!hasOwnerPrivileges(clan, actorUUID)) {
            actor.sendMessage(Text.literal(ERROR + "Tu n'as pas la permission de retrograder des membres."), false);
            return false;
        }

        UUID targetUUID = findMemberByName(clan, targetName, server);
        if (targetUUID == null) {
            actor.sendMessage(Text.literal(ERROR + "Joueur §e" + targetName + " §rintrouvable dans le clan."), false);
            return false;
        }

        ClanRole currentRole = clan.getRole(targetUUID);
        if (currentRole != ClanRole.OFFICER) {
            actor.sendMessage(Text.literal(ERROR + targetName + " n'est pas Officer."), false);
            return false;
        }

        ClanWorldData.setRole(clan.id, targetUUID, ClanRole.MEMBER);
        String msg = "§e" + targetName + " §rest maintenant §7Membre§r.";

        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUUID);
        if (target != null) {
            target.sendMessage(Text.literal(PREFIX + msg), false);
        }
        broadcastToClan(clan, PREFIX + msg, server, actorUUID, targetUUID);
        actor.sendMessage(Text.literal(PREFIX + msg), false);
        return true;
    }

    // -------------------------------------------------------------------------
    // Banque
    // -------------------------------------------------------------------------

    /**
     * Depot dans la banque du clan.
     */
    public static boolean bankDeposit(ServerPlayerEntity player, long amount) {
        UUID uuid = player.getUuid();
        Clan clan = ClanWorldData.getClanOf(uuid);

        if (clan == null) {
            player.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return false;
        }
        if (amount <= 0) {
            player.sendMessage(Text.literal(ERROR + "Le montant doit etre positif."), false);
            return false;
        }

        // Verifier et debiter le joueur
        if (!EconomyHelper.tryLock(uuid)) {
            player.sendMessage(Text.literal(ERROR + "Transaction en cours, reessaie dans un instant."), false);
            return false;
        }
        try {
            long balance = EconomyHelper.getBalance(player);
            if (balance < amount) {
                player.sendMessage(Text.literal(ERROR + "Solde insuffisant (§e" + balance + " §rcoins disponibles)."), false);
                return false;
            }
            boolean debited = EconomyHelper.debit(player, amount, "clan_bank_deposit");
            if (!debited) {
                player.sendMessage(Text.literal(ERROR + "Echec du debit. Reessaie."), false);
                return false;
            }
        } finally {
            EconomyHelper.unlock(uuid);
        }

        ClanWorldData.setBank(clan.id, clan.bankBalance + amount);
        // Tracker la contribution totale du joueur
        ClanWorldData.addContribution(clan.id, uuid, amount);
        player.sendMessage(Text.literal(PREFIX + "§e+" + amount + " §rcoins deposes dans la banque du clan. Solde : §e" + clan.bankBalance + " §rcoins."), false);
        return true;
    }

    /**
     * Retrait depuis la banque du clan.
     * Members : limite journaliere. Officers et Owner : limite configurable.
     */
    public static boolean bankWithdraw(ServerPlayerEntity player, long amount) {
        UUID uuid = player.getUuid();
        Clan clan = ClanWorldData.getClanOf(uuid);

        if (clan == null) {
            player.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return false;
        }
        if (amount <= 0) {
            player.sendMessage(Text.literal(ERROR + "Le montant doit etre positif."), false);
            return false;
        }
        if (clan.bankBalance < amount) {
            player.sendMessage(Text.literal(ERROR + "La banque du clan ne contient que §e" + clan.bankBalance + " §rcoins."), false);
            return false;
        }

        ClanRole role = clan.getRole(uuid);

        // Limite definie par le rang affiche du joueur (-1 = illimite, 0 = bloque, >0 = limite)
        String rankId = getMemberRankId(clan, uuid);
        ClanRank rank = clan.getRankById(rankId);
        long limit = rank != null ? rank.withdrawLimit : -1L;
        if (limit == 0L) {
            player.sendMessage(Text.literal(ERROR + "Ton rang ne t'autorise aucun retrait."), false);
            return false;
        }
        if (limit > 0 && amount > limit) {
            String rankName = rank.name;
            player.sendMessage(Text.literal(ERROR + "Ta limite de retrait (rang " + rankName + ") est de §e" + limit + " §rcoins."), false);
            return false;
        }

        ClanWorldData.setBank(clan.id, clan.bankBalance - amount);
        EconomyHelper.credit(uuid, amount);
        player.sendMessage(Text.literal(PREFIX + "§e" + amount + " §rcoins retires de la banque du clan. Solde : §e" + clan.bankBalance + " §rcoins."), false);
        return true;
    }

    // -------------------------------------------------------------------------
    // Promote / Demote par UUID (hiérarchie custom)
    // -------------------------------------------------------------------------

    /**
     * Promeut un joueur d'un cran dans la hierarchie des rangs du clan.
     * Si le rang cible est "owner", effectue un transfert de propriete :
     * l'ancien owner prend le rang juste en dessous de "owner".
     */
    public static boolean promoteByUuid(ServerPlayerEntity actor, UUID targetUUID, MinecraftServer server) {
        UUID actorUUID = actor.getUuid();
        Clan clan = ClanWorldData.getClanOf(actorUUID);
        if (clan == null) { actor.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false); return false; }
        if (!hasOwnerPrivileges(clan, actorUUID)) { actor.sendMessage(Text.literal(ERROR + "Tu n'as pas la permission de promouvoir."), false); return false; }
        if (!clan.isMember(targetUUID)) { actor.sendMessage(Text.literal(ERROR + "Ce joueur n'est pas dans le clan."), false); return false; }
        if (targetUUID.equals(actorUUID)) { actor.sendMessage(Text.literal(ERROR + "Tu es deja au rang maximum."), false); return false; }

        List<ClanRank> sorted = clan.ranks.stream()
                .sorted(Comparator.comparingInt(r -> r.sortOrder))
                .collect(Collectors.toList());

        String currentRankId = getMemberRankId(clan, targetUUID);
        int currentIdx = indexOfRank(sorted, currentRankId);
        if (currentIdx == -1) currentIdx = sorted.size() - 1;
        if (currentIdx == 0) { actor.sendMessage(Text.literal(ERROR + "Ce joueur est deja au rang maximum."), false); return false; }

        ClanRank newRank = sorted.get(currentIdx - 1);
        String targetName = resolvePlayerName(targetUUID, server);

        if (newRank.id.equals("owner")) {
            if (!isActualOwner(clan, actorUUID)) {
                actor.sendMessage(Text.literal(ERROR + "Seul le proprietaire peut transferer la propriete du clan."), false);
                return false;
            }
            // Transfert de propriete : l'owner actuel prend le rang juste sous owner
            ClanRank actorNewRank = sorted.size() > 1 ? sorted.get(1) : sorted.get(sorted.size() - 1);
            ClanWorldData.setMemberRankAndRole(clan.id, actorUUID, actorNewRank.id, roleForRank(clan, actorNewRank));
            actor.sendMessage(Text.literal(PREFIX + "Tu as transfère la propriete du clan a §e" + targetName +
                    "§r. Tu es desormais " + actorNewRank.colorCode + actorNewRank.name + "§r."), false);
        }

        ClanWorldData.setMemberRankAndRole(clan.id, targetUUID, newRank.id, roleForRank(clan, newRank));
        String msg = PREFIX + "§e" + targetName + " §rest maintenant " + newRank.colorCode + newRank.name + "§r.";
        broadcastToClan(clan, msg, server, actorUUID, targetUUID);
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUUID);
        if (target != null) target.sendMessage(Text.literal(msg), false);
        return true;
    }

    /**
     * Retrograde un joueur d'un cran dans la hierarchie des rangs du clan.
     * Ne peut pas retrograder le proprietaire.
     */
    public static boolean demoteByUuid(ServerPlayerEntity actor, UUID targetUUID, MinecraftServer server) {
        UUID actorUUID = actor.getUuid();
        Clan clan = ClanWorldData.getClanOf(actorUUID);
        if (clan == null) { actor.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false); return false; }
        if (!hasOwnerPrivileges(clan, actorUUID)) { actor.sendMessage(Text.literal(ERROR + "Tu n'as pas la permission de retrograder."), false); return false; }
        if (targetUUID.equals(actorUUID)) { actor.sendMessage(Text.literal(ERROR + "Tu ne peux pas te retrograder toi-meme."), false); return false; }
        if (!clan.isMember(targetUUID)) { actor.sendMessage(Text.literal(ERROR + "Ce joueur n'est pas dans le clan."), false); return false; }

        List<ClanRank> sorted = clan.ranks.stream()
                .sorted(Comparator.comparingInt(r -> r.sortOrder))
                .collect(Collectors.toList());

        String currentRankId = getMemberRankId(clan, targetUUID);
        if (currentRankId.equals("owner")) { actor.sendMessage(Text.literal(ERROR + "Tu ne peux pas retrograder le proprietaire."), false); return false; }

        int currentIdx = indexOfRank(sorted, currentRankId);
        if (currentIdx == -1) currentIdx = 0;
        if (currentIdx >= sorted.size() - 1) { actor.sendMessage(Text.literal(ERROR + "Ce joueur est deja au rang minimum."), false); return false; }

        ClanRank newRank = sorted.get(currentIdx + 1);
        String targetName = resolvePlayerName(targetUUID, server);

        ClanWorldData.setMemberRankAndRole(clan.id, targetUUID, newRank.id, roleForRank(clan, newRank));
        String msg = PREFIX + "§e" + targetName + " §rest maintenant " + newRank.colorCode + newRank.name + "§r.";
        broadcastToClan(clan, msg, server, actorUUID, targetUUID);
        actor.sendMessage(Text.literal(msg), false);
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUUID);
        if (target != null) target.sendMessage(Text.literal(msg), false);
        return true;
    }

    // -------------------------------------------------------------------------
    // Info / Top
    // -------------------------------------------------------------------------

    /**
     * Affiche les informations d'un clan dans le chat du joueur.
     * Si clanName est null, affiche les infos du clan du joueur.
     */
    public static boolean showInfo(ServerPlayerEntity player, String clanName, MinecraftServer server) {
        Clan clan;
        if (clanName == null) {
            clan = ClanWorldData.getClanOf(player.getUuid());
            if (clan == null) {
                player.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan. Specifie un nom : §e/clan info <nom>"), false);
                return false;
            }
        } else {
            clan = ClanWorldData.getAll().stream()
                    .filter(c -> c.name.equalsIgnoreCase(clanName) || c.tag.equalsIgnoreCase(clanName))
                    .findFirst().orElse(null);
            if (clan == null) {
                player.sendMessage(Text.literal(ERROR + "Clan §e" + clanName + " §rintrouvable."), false);
                return false;
            }
        }

        long xpNext = ClanConfig.get().xpRequiredForLevel(clan.level + 1);
        String ownerName = resolvePlayerName(clan.ownerUUID, server);

        player.sendMessage(Text.literal("§6§l=== " + clan.name + " [" + clan.tag + "] ==="), false);
        player.sendMessage(Text.literal("§7Proprietaire : §f" + ownerName), false);
        player.sendMessage(Text.literal("§7Niveau : §e" + clan.level + "§7/§e" + ClanConfig.get().maxLevel +
                " §7| XP : §e" + clan.xp + "§7/§e" + xpNext), false);
        player.sendMessage(Text.literal("§7Membres : §e" + clan.size() + "§7/§e" +
                ClanConfig.get().maxMembersForLevel(clan.level)), false);
        player.sendMessage(Text.literal("§7Banque : §e" + clan.bankBalance + " §7coins"), false);
        player.sendMessage(Text.literal("§7Cree le : §f" + formatDate(clan.createdAt)), false);

        // Liste des membres
        StringBuilder sb = new StringBuilder("§7Membres : ");
        for (Map.Entry<UUID, ClanRole> entry : clan.members.entrySet()) {
            String mName = resolvePlayerName(entry.getKey(), server);
            String rankId = getMemberRankId(clan, entry.getKey());
            ClanRank rank = clan.getRankById(rankId);
            String roleColor = rank != null ? rank.colorCode : switch (entry.getValue()) {
                case OWNER   -> "§c";
                case OFFICER -> "§6";
                case MEMBER  -> "§7";
            };
            sb.append(roleColor).append(mName).append("§8, ");
        }
        String memberList = sb.toString();
        if (memberList.endsWith("§8, ")) {
            memberList = memberList.substring(0, memberList.length() - 4);
        }
        player.sendMessage(Text.literal(memberList), false);

        return true;
    }

    /**
     * Affiche le classement des 10 meilleurs clans.
     */
    public static void showTop(ServerPlayerEntity player, MinecraftServer server) {
        List<Clan> sorted = new ArrayList<>(ClanWorldData.getAll());
        sorted.sort((a, b) -> {
            if (b.level != a.level) return Integer.compare(b.level, a.level);
            return Long.compare(b.xp, a.xp);
        });

        player.sendMessage(Text.literal("§6§l=== Top Clans ==="), false);
        int limit = Math.min(10, sorted.size());
        for (int i = 0; i < limit; i++) {
            Clan c = sorted.get(i);
            player.sendMessage(Text.literal(
                    "§e#" + (i + 1) + " §f" + c.name + " §7[§b" + c.tag + "§7] " +
                    "§7Niv.§e" + c.level + " §7| §e" + c.size() + " §7membres"
            ), false);
        }
        if (sorted.isEmpty()) {
            player.sendMessage(Text.literal("§7Aucun clan pour l'instant."), false);
        }
    }

    public static boolean setTerritoryMessage(ServerPlayerEntity player, String type, String message) {
        UUID uuid = player.getUuid();
        Clan clan = ClanWorldData.getClanOf(uuid);
        if (clan == null) {
            player.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return false;
        }

        String action = switch (type) {
            case "enter" -> "edit_enter_message";
            case "leave" -> "edit_leave_message";
            default -> null;
        };
        if (action == null) {
            player.sendMessage(Text.literal(ERROR + "Type de message inconnu."), false);
            return false;
        }

        if (!ClanClaimHandler.hasPermission(clan, uuid, action)) {
            player.sendMessage(Text.literal(ERROR + "Tu n'as pas la permission de modifier ce message."), false);
            return false;
        }

        String sanitized = message == null ? "" : message.trim();
        if (sanitized.isEmpty()) {
            player.sendMessage(Text.literal(ERROR + "Le message ne peut pas etre vide."), false);
            return false;
        }

        if ("enter".equals(type)) {
            ClanWorldData.setEnterMessage(clan.id, sanitized);
            player.sendMessage(Text.literal(PREFIX + "Message d'entree mis a jour :"), false);
            player.sendMessage(Text.literal(formatTerritoryMessagePreview(clan.enterMessage, clan)), false);
        } else {
            ClanWorldData.setLeaveMessage(clan.id, sanitized);
            player.sendMessage(Text.literal(PREFIX + "Message de sortie mis a jour :"), false);
            player.sendMessage(Text.literal(formatTerritoryMessagePreview(clan.leaveMessage, clan)), false);
        }
        return true;
    }

    private static String formatTerritoryMessagePreview(String template, Clan clan) {
        String resolved = template
                .replace("{clan}", clan.name)
                .replace("{tag}", clan.tag);
        StringBuilder out = new StringBuilder(resolved.length());
        for (int i = 0; i < resolved.length(); i++) {
            char current = resolved.charAt(i);
            if (current == '&' && i + 1 < resolved.length()) {
                char next = Character.toLowerCase(resolved.charAt(i + 1));
                if ((next >= '0' && next <= '9')
                        || (next >= 'a' && next <= 'f')
                        || (next >= 'k' && next <= 'o')
                        || next == 'r') {
                    out.append('§').append(next);
                    i++;
                    continue;
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    // -------------------------------------------------------------------------
    // Utilitaires prives
    // -------------------------------------------------------------------------

    /**
     * Recherche un membre du clan par son nom (en ligne ou dans le cache des noms).
     * Retourne son UUID, ou null si non trouve.
     */
    private static UUID findMemberByName(Clan clan, String name, MinecraftServer server) {
        // D'abord parmi les joueurs en ligne
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
        if (online != null && clan.isMember(online.getUuid())) {
            return online.getUuid();
        }
        // Sinon parcourir les membres (UUID uniquement, pas de lookup hors-ligne dans cette version)
        for (UUID memberUUID : clan.members.keySet()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(memberUUID);
            if (p != null && p.getName().getString().equalsIgnoreCase(name)) {
                return memberUUID;
            }
        }
        return null;
    }

    /** Retourne le rankId affiche d'un membre (depuis memberRanks ou derive du ClanRole). */
    static String getMemberRankId(Clan clan, UUID uuid) {
        String rankId = clan.memberRanks.get(uuid);
        if (rankId != null) return rankId;
        ClanRole role = clan.getRole(uuid);
        if (role == null) return "member";
        return switch (role) { case OWNER -> "owner"; case OFFICER -> "officer"; default -> "member"; };
    }

    /**
     * Determine la ClanRole (permission) d'un rang :
     * owner → OWNER, officer → OFFICER, member → MEMBER.
     * Rang custom : OFFICER si sortOrder < celui d'officer, sinon MEMBER.
     */
    static ClanRole roleForRank(Clan clan, ClanRank rank) {
        if (rank.id.equals("owner"))   return ClanRole.OWNER;
        if (rank.id.equals("officer")) return ClanRole.OFFICER;
        if (rank.id.equals("member"))  return ClanRole.MEMBER;
        int officerOrder = clan.ranks.stream()
                .filter(r -> r.id.equals("officer"))
                .mapToInt(r -> r.sortOrder).findFirst().orElse(Integer.MAX_VALUE);
        return rank.sortOrder < officerOrder ? ClanRole.OFFICER : ClanRole.MEMBER;
    }

    private static int indexOfRank(List<ClanRank> sorted, String rankId) {
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).id.equals(rankId)) return i;
        }
        return -1;
    }

    /** Resout un UUID en nom de joueur (en ligne ou via le cache Minecraft). */
    static String resolvePlayerName(UUID uuid, MinecraftServer server) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
        if (p != null) return p.getName().getString();
        return server.getUserCache()
                .getByUuid(uuid)
                .map(com.mojang.authlib.GameProfile::getName)
                .orElse(uuid.toString().substring(0, 8) + "...");
    }

    /** Broadcast un message a tous les membres en ligne du clan, sauf les exclus. */
    private static void broadcastToClan(Clan clan, String message, MinecraftServer server, UUID... excludes) {
        Set<UUID> excluded = new HashSet<>(Arrays.asList(excludes));
        for (UUID memberUUID : clan.members.keySet()) {
            if (excluded.contains(memberUUID)) continue;
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(memberUUID);
            if (p != null) {
                p.sendMessage(Text.literal(message), false);
            }
        }
    }

    private static String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(new java.util.Date(timestamp));
    }
}
