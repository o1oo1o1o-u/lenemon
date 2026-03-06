package com.lenemon.fly;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

/**
 * The type Fly timer manager.
 */
public class FlyTimerManager {

    private static final Map<UUID, FlySession> SESSIONS = new HashMap<>();
    private static boolean registered = false;
    private static MinecraftServer server;

    private static class FlySession {
        /**
         * The Seconds left.
         */
        int secondsLeft; // -1 = permanent
        /**
         * The Permanent.
         */
        final boolean permanent;

        /**
         * Instantiates a new Fly session.
         *
         * @param seconds the seconds
         */
        FlySession(int seconds) {
            this.secondsLeft = seconds;
            this.permanent = (seconds == -1);
        }
    }

    /**
     * Register.
     */
    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            server = s;
            onTick(s);
        });
    }

    /**
     * Load from disk.
     *
     * @param server the server
     */
// Nouvelle méthode pour charger au démarrage
    public static void loadFromDisk(MinecraftServer server) {
        FlyTimerManager.server = server;
        Map<UUID, Integer> saved = FlySessionStorage.load(server);
        saved.forEach((uuid, seconds) -> {
            SESSIONS.put(uuid, new FlySession(seconds));
        });
        if (!saved.isEmpty()) {
            System.out.println("[Fly] " + saved.size() + " session(s) restaurée(s).");
        }
    }

    // Nouvelle méthode pour sauvegarder
    private static void saveToDisk() {
        if (server == null) return;
        Map<UUID, Integer> toSave = new HashMap<>();
        SESSIONS.forEach((uuid, session) -> {
            if (!session.permanent) {
                toSave.put(uuid, session.secondsLeft);
            }
        });
        FlySessionStorage.save(server, toSave);
    }

    /**
     * Add session.
     *
     * @param player  the player
     * @param seconds the seconds
     */
    public static void addSession(ServerPlayerEntity player, int seconds) {
        SESSIONS.put(player.getUuid(), new FlySession(seconds));
        saveToDisk();
    }

    /**
     * Has session boolean.
     *
     * @param uuid the uuid
     * @return the boolean
     */
    public static boolean hasSession(UUID uuid) {
        return SESSIONS.containsKey(uuid);
    }

    /**
     * Remove session.
     *
     * @param uuid the uuid
     */
    public static void removeSession(UUID uuid) {
        SESSIONS.remove(uuid);
    }

    private static int tickCounter = 0;
    private static int saveCounter = 0;

    private static void onTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        // Sauvegarde toutes les 60 secondes
        saveCounter++;
        if (saveCounter >= 60) {
            saveCounter = 0;
            saveToDisk();
        }

        if (SESSIONS.isEmpty()) return;

        Iterator<Map.Entry<UUID, FlySession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, FlySession> entry = it.next();
            FlySession session = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null) continue; // garde la session si déconnecté

            // Permanent → juste vérifier que la permission est toujours là
            if (session.permanent) continue;

            // Décompte uniquement si le joueur est en train de voler
            if (!player.getAbilities().flying) continue;

            session.secondsLeft--;

            // Avertissements
            if (session.secondsLeft == 60) {
                player.sendMessage(Text.literal("§e[Fly] ⚠ Il vous reste §f1 minute §ede vol !"), false);
            } else if (session.secondsLeft == 30) {
                player.sendMessage(Text.literal("§e[Fly] ⚠ Il vous reste §f30 secondes §ede vol !"), false);
            } else if (session.secondsLeft == 10) {
                player.sendMessage(Text.literal("§c[Fly] ⚠ Il vous reste §f10 secondes §cde vol !"), false);
            }

            if (session.secondsLeft <= 0) {
                // Temps écoulé
                expireFly(player, server);
                it.remove();
                saveToDisk();
            }
        }
    }

    private static void expireFly(ServerPlayerEntity player, MinecraftServer server) {
        // Désactive le fly
        server.getCommandManager().executeWithPrefix(
                server.getCommandSource(),
                "fly " + player.getName().getString() + " false"
        );

        // Retire la permission LuckPerms
        revokePermission(player, server);

        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§c§l  🪶 Votre plume de fly est épuisée !"), false);
        player.sendMessage(Text.literal("§7Le fly a été désactivé automatiquement."), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
    }

    private static void revokePermission(ServerPlayerEntity player, MinecraftServer server) {
        try {
            if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("luckperms")) return;

            String name = player.getName().getString();
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "lp user " + name + " permission unset essentialcommands.fly.self"
            );
        } catch (Throwable e) {
            System.err.println("[Fly] Erreur retrait permission : " + e.getMessage());
        }
    }

    /**
     * Grant permission.
     *
     * @param player the player
     */
    public static void grantPermission(ServerPlayerEntity player) {
        try {
            MinecraftServer srv = player.getServer();
            if (srv == null) return;

            // si LuckPerms n'est pas installé, ne rien faire
            if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("luckperms")) return;

            String name = player.getName().getString();
            srv.getCommandManager().executeWithPrefix(
                    srv.getCommandSource(),
                    "lp user " + name + " permission set essentialcommands.fly.self true"
            );
        } catch (Throwable e) {
            System.err.println("[Fly] Erreur ajout permission : " + e.getMessage());
        }
    }
}