package com.lenemon.casino;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lenemon.block.CasinoBlockEntity;
import com.lenemon.block.CasinoState;
import com.lenemon.casino.holo.CasinoHolograms;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * The type Casino spin handler.
 */
public class CasinoSpinHandler {

    private static final Random RANDOM = new Random();

    /**
     * Spin.
     *
     * @param player the player
     * @param entity the entity
     * @param pos    the pos
     */
    public static void spin(ServerPlayerEntity player, CasinoBlockEntity entity, BlockPos pos) {

        // ── 1. Verrou anti-spam ──────────────────────────
        if (entity.isLocked()) {
            player.sendMessage(Text.literal("§c[Casino] Déjà en cours, attendez !"), false);
            return;
        }
        entity.setLocked(true);

        // ── 2. Vérification solde via Impactor ───────────
        long price = entity.getEntryPrice();
        boolean hasFunds = checkAndDebit(player, price);

        if (!hasFunds) {
            entity.setLocked(false);
            player.sendMessage(Text.literal("§c[Casino] Fonds insuffisants ! Il vous faut §e"
                    + price + " PokéCoins§c."), false);
            return;
        }

        // ── 3. RNG serveur ───────────────────────────────
        int roll = RANDOM.nextInt(10000) + 1; // 1 à 10000
        boolean win = roll <= entity.getWinChance();

        // ── 4. Suspense (délai 2s via thread serveur) ────
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§6§l  🎰 Le casino tourne..."), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);

        player.getServer().execute(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}

            player.getServer().execute(() -> resolveResult(player, entity, win, price));
        });
    }

    private static void resolveResult(ServerPlayerEntity player, CasinoBlockEntity entity,
                                      boolean win, long price) {
        entity.setLocked(false);

        if (win) {
            // ── Victoire ─────────────────────────────────
            NbtCompound pokemonNbt = entity.getPokemonData();
            if (pokemonNbt == null) {
                player.sendMessage(Text.literal("§c[Casino] Erreur : le Pokémon a disparu !"), false);
                return;
            }

            Pokemon pokemon = new Pokemon();
            pokemon.loadFromNBT(player.getServerWorld().getRegistryManager(), pokemonNbt);

            // Donne le Pokémon au joueur
            PlayerPartyStore party = com.cobblemon.mod.common.Cobblemon.INSTANCE
                    .getStorage().getParty(player);

            boolean addedToParty = false;
            if (!CasinoPokemonStorage.isPartyFull(party)) {
                party.add(pokemon);
                addedToParty = true;
            }

            if (!addedToParty) {
                PCStore pc = com.cobblemon.mod.common.Cobblemon.INSTANCE
                        .getStorage().getPC(player.getUuid(), player.getServerWorld().getRegistryManager());
                if (pc != null) {
                    pc.add(pokemon);
                }
            }

            // Vide le casino
            entity.clearPokemon();
            entity.setState(CasinoState.EMPTY);

            // Messages victoire
            player.sendMessage(Text.literal("§a§l  🎉 VOUS AVEZ GAGNÉ !"), false);
            player.sendMessage(Text.literal("§7Pokémon obtenu : §b" + entity.getPokemonDisplayName()), false);
            if (!addedToParty) {
                player.sendMessage(Text.literal("§7(Party pleine → envoyé au PC)"), false);
            }

            // Notif au proprio
            notifyOwner(player, entity, true, price);

        } else {
            // ── Défaite ───────────────────────────────────
            player.sendMessage(Text.literal("§c§l  💔 Pas de chance cette fois..."), false);
            player.sendMessage(Text.literal("§7Vous avez perdu §e" + price + " PokéCoins§7."), false);

            notifyOwner(player, entity, false, price);
        }


        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);



    }

    private static void notifyOwner(ServerPlayerEntity player, CasinoBlockEntity entity,
                                    boolean win, long price) {
        if (entity.getOwnerUUID() == null) return;
        ServerPlayerEntity owner = player.getServer()
                .getPlayerManager().getPlayer(entity.getOwnerUUID());
        if (owner == null) return; // owner hors ligne

        if (win) {
            owner.sendMessage(Text.literal("§e[Casino] §b" + player.getName().getString()
                    + "§e a gagné votre Pokémon §b" + entity.getPokemonDisplayName() + "§e !"), false);
        } else {
//            owner.sendMessage(Text.literal("§e[Casino] §b" + player.getName().getString()
//                    + "§e a joué et perdu §e" + price + " PokéCoins§e."), false);
        }
    }

    // ── Impactor Economy ─────────────────────────────────

    private static boolean checkAndDebit(ServerPlayerEntity player, long amount) {
        try {
            var service = net.impactdev.impactor.api.economy.EconomyService.instance();
            var currency = service.currencies().primary();

            var account = service.account(currency, player.getUuid()).join();

            var balance = account.balance();
            java.math.BigDecimal cost = java.math.BigDecimal.valueOf(amount);

            if (balance.compareTo(cost) < 0) {
                return false;
            }

            account.withdraw(cost);
            return true;

        } catch (Exception e) {
            player.sendMessage(Text.literal("§c[Casino] Erreur economy : " + e.getMessage()), false);
            return false;
        }
    }
    private static void creditOwner(ServerPlayerEntity player, CasinoWorldData.CasinoData casino, long amount) {
        try {
            var service = net.impactdev.impactor.api.economy.EconomyService.instance();
            var currency = service.currencies().primary();
            var account = service.account(currency, casino.ownerUUID).join();
            account.deposit(java.math.BigDecimal.valueOf(amount));
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c[Casino] Erreur versement owner : " + e.getMessage()), false);
        }
    }

    /**
     * Spin from world data.
     *
     * @param player the player
     * @param casino the casino
     * @param data   the data
     * @param pos    the pos
     */
    public static void spinFromWorldData(ServerPlayerEntity player,
                                         CasinoWorldData.CasinoData casino,
                                         CasinoWorldData data, BlockPos pos) {
        if (casino.locked) {
            player.sendMessage(Text.literal("§c[Casino] Déjà en cours, attendez !"), false);
            return;
        }
        casino.locked = true;
        data.markDirty();

        long price = casino.entryPrice;
        boolean hasFunds = checkAndDebit(player, price);

        if (!hasFunds) {
            casino.locked = false;
            data.markDirty();
            player.sendMessage(Text.literal("§c[Casino] Fonds insuffisants ! Il vous faut §e"
                    + price + " PokéCoins§c."), false);
            return;
        }

        // RNG serveur → résultat connu AVANT l'ouverture du GUI
        int roll = RANDOM.nextInt(10000) + 1;
        boolean win = roll <= casino.winChance;

        // Ouvre le nouveau GUI avec le résultat pré-calculé
        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new com.lenemon.casino.screen.CasinoScreenHandler(
                        syncId, inv, casino, data, pos
                ),
                net.minecraft.text.Text.literal("Casino")
        ));
    }

    /**
     * Resolve result from world data.
     *
     * @param player the player
     * @param casino the casino
     * @param data   the data
     * @param win    the win
     * @param price  the price
     */
    public static void resolveResultFromWorldData(ServerPlayerEntity player,
                                                   CasinoWorldData.CasinoData casino,
                                                   CasinoWorldData data, boolean win, long price) {
        casino.locked = false;
        creditOwner(player, casino, price);
        if (win) {
            Pokemon pokemon = CasinoPokemonStorage.loadPokemon(
                    player.getServer(),
                    casino.casinoUUID,
                    player.getServerWorld().getRegistryManager()
            );

            if (pokemon == null) {
                player.sendMessage(Text.literal("§c[Casino] Erreur : Pokémon introuvable !"), false);
                data.markDirty();
                return;
            }

            PlayerPartyStore party = com.cobblemon.mod.common.Cobblemon.INSTANCE
                    .getStorage().getParty(player);

            boolean addedToParty = false;
            if (!CasinoPokemonStorage.isPartyFull(party)) {
                party.add(pokemon);
                addedToParty = true;
            }

            if (!addedToParty) {
                PCStore pc = com.cobblemon.mod.common.Cobblemon.INSTANCE
                        .getStorage().getPC(player.getUuid(), player.getServerWorld().getRegistryManager());
                if (pc != null) pc.add(pokemon);
            }



            CasinoPokemonStorage.deletePokemon(player.getServer(), casino.casinoUUID);
            String pokemonName = casino.pokemonDisplayName;
            casino.pokemonDisplayName = "";
            casino.state = CasinoState.EMPTY;
            data.markDirty();

            String ownerName = casino.ownerName != null && !casino.ownerName.isBlank()
                    ? casino.ownerName
                    : "Inconnu";


            BlockPos casinoPos = data.getPosByCasinoUUID(casino.casinoUUID);
            if (casinoPos == null) {
                player.sendMessage(Text.literal("§c[Casino] Position du casino introuvable !"), false);
                return;
            }
            CasinoHolograms.removeCasinoHologramAround(casinoPos);
            CasinoHolograms.recreateClosedCasinoHologram(
                    player.getServerWorld(),
                    casinoPos,
                    ownerName,
                    casino.entryPrice,
                    casino.winChance / 100.0
            );

            player.sendMessage(Text.literal("§a§l  🎉 VOUS AVEZ GAGNÉ !"), false);
            player.sendMessage(Text.literal("§7Pokémon obtenu : §b" + pokemonName), false);
            if (!addedToParty)
                player.sendMessage(Text.literal("§7(Party pleine → envoyé au PC)"), false);

            notifyOwnerFromWorldData(player, casino);
        } else {
            data.markDirty();
            player.sendMessage(Text.literal("§c§l  💔 Pas de chance cette fois..."), false);
            player.sendMessage(Text.literal("§7Vous avez perdu §e" + price + " PokéCoins§7."), false);
            notifyOwnerFromWorldData(player, casino);
        }

        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);


    }

    private static void notifyOwnerFromWorldData(ServerPlayerEntity player,
                                                 CasinoWorldData.CasinoData casino) {
        if (casino.ownerUUID == null) return;
        ServerPlayerEntity owner = player.getServer()
                .getPlayerManager().getPlayer(casino.ownerUUID);
        if (owner == null) return;
        if (casino.state == CasinoState.EMPTY) {
            owner.sendMessage(
                    Text.literal("§e[Casino] §b" + player.getName().getString()
                            + "§e a gagné le Pokémon de votre casino !"),
                    false
            );
        }
    }
}