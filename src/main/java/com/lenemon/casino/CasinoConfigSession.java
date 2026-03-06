package com.lenemon.casino;

import com.lenemon.block.CasinoBlockEntity;
import com.lenemon.block.CasinoState;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The type Casino config session.
 */
public class CasinoConfigSession {

    /**
     * The Casino data.
     */
    public CasinoWorldData.CasinoData casinoData;
    /**
     * The World data.
     */
    public CasinoWorldData worldData;

    /**
     * The enum Config step.
     */
    public enum ConfigStep {
        /**
         * Waiting price config step.
         */
        WAITING_PRICE,
        /**
         * Waiting chance config step.
         */
        WAITING_CHANCE
    }

    private static class Session {
        /**
         * The Pos.
         */
        final BlockPos pos;
        /**
         * The Entity.
         */
        final CasinoBlockEntity entity;
        /**
         * The Step.
         */
        ConfigStep step;
        /**
         * The Price.
         */
        long price;
        /**
         * The Casino data.
         */
        CasinoWorldData.CasinoData casinoData;  // AJOUTE
        /**
         * The World data.
         */
        CasinoWorldData worldData;              // AJOUTE

        /**
         * Instantiates a new Session.
         *
         * @param pos    the pos
         * @param entity the entity
         */
        Session(BlockPos pos, CasinoBlockEntity entity) {
            this.pos = pos;
            this.entity = entity;
            this.step = ConfigStep.WAITING_PRICE;
        }
    }

    // Map des sessions actives par joueur
    private static final Map<UUID, Session> activeSessions = new HashMap<>();

    // ── Démarrage de la config ────────────────────────────

    /**
     * Start config.
     *
     * @param player the player
     * @param pos    the pos
     * @param entity the entity
     */
    public static void startConfig(ServerPlayerEntity player, BlockPos pos, CasinoBlockEntity entity) {
        // Si déjà une session active, on la remplace
        activeSessions.put(player.getUuid(), new Session(pos, entity));

        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§6§l  🎰 Configuration du Casino"), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§7Entrez le §fprix d'entrée §7(en PokéCoins) :"), false);
        player.sendMessage(Text.literal("§7Exemple : §e500"), false);
        player.sendMessage(Text.literal("§7Tapez §cannuler §7pour arrêter."), false);
    }

    // ── Enregistrement de l'intercepteur de messages ──────

    /**
     * Register.
     */
    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            UUID uuid = sender.getUuid();
            if (!activeSessions.containsKey(uuid)) return true; // message normal

            Session session = activeSessions.get(uuid);
            String content = message.getContent().getString().trim();

            // Annulation
            if (content.equalsIgnoreCase("annuler")) {
                activeSessions.remove(uuid);
                sender.sendMessage(Text.literal("§c[Casino] Configuration annulée."), false);
                return false; // bloque le message dans le chat public
            }

            switch (session.step) {
                case WAITING_PRICE -> handlePrice(sender, session, content);
                case WAITING_CHANCE -> handleChance(sender, session, content);
            }

            return false; // bloque TOUS les messages de config du chat public
        });
    }

    // ── Étape 1 : Prix ────────────────────────────────────

    private static void handlePrice(ServerPlayerEntity player, Session session, String input) {
        try {
            long price = Long.parseLong(input);
            if (price <= 0) {
                player.sendMessage(Text.literal("§c[Casino] Le prix doit être supérieur à 0."), false);
                return;
            }
            session.price = price;
            session.step = ConfigStep.WAITING_CHANCE;

            player.sendMessage(Text.literal("§a[Casino] Prix défini : §f" + price + " PokéCoins"), false);
            player.sendMessage(Text.literal("§7Entrez maintenant le §f% de chance de gain §7(0.01 à 100) :"), false);
            player.sendMessage(Text.literal("§7Exemple : §e15.5 §7= 15.5% de chance"), false);

        } catch (NumberFormatException e) {
            player.sendMessage(Text.literal("§c[Casino] Valeur invalide. Entrez un nombre entier."), false);
        }
    }

    // ── Étape 2 : % de chance ─────────────────────────────

    private static void handleChance(ServerPlayerEntity player, Session session, String input) {
        try {
            double percent = Double.parseDouble(input.replace(",", "."));
            if (percent <= 0 || percent > 100) {
                player.sendMessage(Text.literal("§c[Casino] Valeur entre 0.01 et 100 uniquement."), false);
                return;
            }

            int winChance = (int) Math.round(percent * 100); // ex: 15.5% → 1550 sur 10000

            if (session.entity != null) {
                // Ancien système CasinoBlockEntity
                session.entity.setEntryPrice(session.price);
                session.entity.setWinChance(winChance);
                if (session.entity.getCasinoState() == CasinoState.UNCONFIGURED) {
                    session.entity.setState(CasinoState.CONFIGURED);
                }
            } else if (session.casinoData != null && session.worldData != null) {
                // Nouveau système WorldData
                session.casinoData.entryPrice = session.price;
                session.casinoData.winChance = winChance;
                if (session.casinoData.state == CasinoState.UNCONFIGURED) {
                    session.casinoData.state = CasinoState.CONFIGURED;
                }
                session.worldData.markDirty();
            }

            activeSessions.remove(player.getUuid());

            player.sendMessage(Text.literal("§a[Casino] Chance de gain : §f" + percent + "%"), false);
            player.sendMessage(Text.literal(""), false);
            player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            player.sendMessage(Text.literal("§a§l  ✔ Casino configuré avec succès !"), false);
            player.sendMessage(Text.literal("§7Prix    : §e" + session.price + " PokéCoins"), false);
            player.sendMessage(Text.literal("§7Chance  : §e" + percent + "%"), false);
            player.sendMessage(Text.literal("§7État    : §6En attente d'un Pokémon"), false);
            player.sendMessage(Text.literal("§7→ Shift+clic pour ajouter votre Pokémon"), false);
            player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);

        } catch (NumberFormatException e) {
            player.sendMessage(Text.literal("§c[Casino] Valeur invalide. Exemple : 15.5"), false);
        }
    }

    // ── Utilitaire ────────────────────────────────────────

    /**
     * Has active session boolean.
     *
     * @param uuid the uuid
     * @return the boolean
     */
    public static boolean hasActiveSession(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    /**
     * Remove session.
     *
     * @param uuid the uuid
     */
    public static void removeSession(UUID uuid) {
        activeSessions.remove(uuid);
    }

    /**
     * Start config from world data.
     *
     * @param player the player
     * @param pos    the pos
     * @param casino the casino
     * @param data   the data
     */
    public static void startConfigFromWorldData(ServerPlayerEntity player, BlockPos pos,
                                                CasinoWorldData.CasinoData casino,
                                                CasinoWorldData data) {
        Session session = new Session(pos, null);
        session.casinoData = casino;
        session.worldData = data;
        activeSessions.put(player.getUuid(), session);

        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§6§l  🎰 Configuration du Casino"), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§7Entrez le §fprix d'entrée §7(en PokéCoins) :"), false);
        player.sendMessage(Text.literal("§7Exemple : §e500"), false);
        player.sendMessage(Text.literal("§7Tapez §cannuler §7pour arrêter."), false);
    }

}