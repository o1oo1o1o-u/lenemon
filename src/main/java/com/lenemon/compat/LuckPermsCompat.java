package com.lenemon.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.SuffixNode;

import java.util.UUID;

/**
 * Pont vers l'API LuckPerms.
 * LP est en compileOnly — les imports directs sont valides a la compilation.
 * A l'execution, LP doit etre charge sur le serveur (guard isModLoaded).
 */
public final class LuckPermsCompat {
    private LuckPermsCompat() {}

    /**
     * Retourne true si LuckPerms est charge en runtime.
     */
    public static boolean isLuckPermsLoaded() {
        return FabricLoader.getInstance().isModLoaded("luckperms");
    }

    /**
     * Retourne l'instance LP ou null si non disponible.
     */
    public static LuckPerms getApiOrNull() {
        if (!isLuckPermsLoaded()) return null;
        try {
            return LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Retourne le User LP pour un UUID donne, ou null si non trouve.
     * Necessite que le joueur soit en ligne (getUserManager().getUser() ne charge pas depuis la DB).
     */
    public static User getUserOrNull(LuckPerms api, UUID uuid) {
        if (api == null) return null;
        return api.getUserManager().getUser(uuid);
    }

    /**
     * Definit un suffix de clan comme node persistant LuckPerms (priority 900).
     * Remplace tout suffix de priorite 900 existant pour ce joueur.
     * Utilise modifyUser() qui gere le chargement et la sauvegarde automatiquement,
     * y compris si le joueur est hors ligne.
     *
     * @param uuid le UUID du joueur
     * @param tag  le tag du clan (sans formatage), ou null pour seulement effacer
     */
    public static void setClanSuffix(UUID uuid, String tag) {
        LuckPerms api = getApiOrNull();
        if (api == null) return;

        api.getUserManager().modifyUser(uuid, user -> {
            // Supprimer tous les SuffixNode existants de priorite 900
            user.data().clear(NodeType.SUFFIX.predicate(
                    sn -> sn.getPriority() == 900
            ));

            // Ajouter le nouveau suffix si fourni
            if (tag != null) {
                SuffixNode suffix = SuffixNode.builder(
                        " \u00a78[\u00a7b" + tag.toUpperCase() + "\u00a78]",
                        900
                ).build();
                user.data().add(suffix);
            }
        });
    }

    /**
     * Efface le suffix de clan persistant LuckPerms pour ce joueur.
     *
     * @param uuid le UUID du joueur
     */
    public static void clearClanSuffix(UUID uuid) {
        setClanSuffix(uuid, null);
    }
}
