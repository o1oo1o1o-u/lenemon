package com.lenemon.clan;

/**
 * Integration du tag de clan dans le chat.
 * Le tag est desormais injecte comme prefix LuckPerms transient (priority 900)
 * via LuckPermsCompat.setClanSuffix(), ce qui le place AVANT le prefix LP existant.
 *
 * Cette classe est conservee pour compatibilite mais register() est un no-op.
 */
public class ClanChatFormatter {

    private ClanChatFormatter() {}

    /**
     * Anciennement enregistrait un ServerMessageDecoratorEvent.
     * Remplace par l'integration LuckPerms transient prefix.
     */
    public static void register() {
        // No-op : le tag est gere via LuckPermsCompat.setClanSuffix()
    }
}
