package com.lenemon.muffin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.lenemon.casino.CasinoPokemonStorage;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuffinService {

    private static final long SWING_SUPPRESSION_MS = 1200L;
    private static final Map<UUID, Long> SWING_SUPPRESS_UNTIL = new ConcurrentHashMap<>();

    private MuffinService() {}

    public static TypedActionResult<ItemStack> useMagicMuffin(ServerPlayerEntity player, ItemStack stack) {
        MagicMuffinType type = MagicMuffinHelper.getType(stack);
        if (type == null) return TypedActionResult.pass(stack);

        markSwingSuppression(player);

        Pokemon pokemon = rollPokemon(player, type);
        if (pokemon == null) {
            player.sendMessage(Text.literal("§c[Muffin] Aucun Pokemon valide dans la configuration."), false);
            return TypedActionResult.success(stack);
        }

        DeliveryResult result = deliverPokemon(player, pokemon);
        if (!result.success) {
            player.sendMessage(Text.literal("§c[Muffin] Impossible de donner le Pokemon."), false);
            return TypedActionResult.success(stack);
        }

        stack.decrement(1);
        sendPlayerRewardMessage(player, pokemon, result);
        maybeBroadcastRareReward(player, type, pokemon);
        return TypedActionResult.success(stack);
    }

    private static Pokemon rollPokemon(ServerPlayerEntity player, MagicMuffinType type) {
        MuffinConfig.MuffinTypeConfig cfg = MuffinConfig.get(type);
        boolean useLegendaryPool = rollPercent(player, cfg.legendaryChance);

        Species species = chooseSpecies(player, type, useLegendaryPool);
        if (species == null) {
            species = chooseSpecies(player, type, !useLegendaryPool);
        }
        if (species == null) return null;

        Pokemon pokemon = species.create(cfg.level);
        pokemon.setShiny(rollPercent(player, cfg.shinyChance));
        pokemon.updateAspects();
        pokemon.updateForm();
        pokemon.setOriginalTrainer(player.getUuid());
        return pokemon;
    }

    private static Species chooseSpecies(ServerPlayerEntity player, MagicMuffinType type, boolean legendaryPool) {
        List<Species> candidates = MuffinPoolCache.getCandidates(type, legendaryPool);
        if (candidates.isEmpty()) return null;
        int index = player.getRandom().nextInt(candidates.size());
        return candidates.get(index);
    }

    private static boolean rollPercent(ServerPlayerEntity player, double chance) {
        if (chance <= 0.0D) return false;
        if (chance >= 100.0D) return true;
        return player.getRandom().nextDouble() * 100.0D < chance;
    }

    public static boolean shouldSuppressServerSwing(ServerPlayerEntity player) {
        Long until = SWING_SUPPRESS_UNTIL.get(player.getUuid());
        if (until == null) return false;
        long now = System.currentTimeMillis();
        if (now > until) {
            SWING_SUPPRESS_UNTIL.remove(player.getUuid());
            return false;
        }
        return true;
    }

    private static void markSwingSuppression(ServerPlayerEntity player) {
        SWING_SUPPRESS_UNTIL.put(player.getUuid(), System.currentTimeMillis() + SWING_SUPPRESSION_MS);
    }

    private static DeliveryResult deliverPokemon(ServerPlayerEntity player, Pokemon pokemon) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        if (!CasinoPokemonStorage.isPartyFull(party)) {
            party.add(pokemon);
            return new DeliveryResult(true, false);
        }

        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player.getUuid(), player.getServerWorld().getRegistryManager());
        if (pc == null) {
            return new DeliveryResult(false, false);
        }

        pc.add(pokemon);
        return new DeliveryResult(true, true);
    }

    private static void sendPlayerRewardMessage(ServerPlayerEntity player, Pokemon pokemon, DeliveryResult result) {
        MutableText pokemonName = getLocalizedPokemonName(pokemon)
                .formatted(pokemon.getShiny() ? Formatting.AQUA : Formatting.YELLOW, Formatting.BOLD);
        MutableText rarity = getRarityLabel(pokemon);
        Text destination = Text.literal(result.sentToPc ? "PC" : "party")
                .formatted(result.sentToPc ? Formatting.GOLD : Formatting.GREEN, Formatting.BOLD);

        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§6§l  Muffin Magique ouvert !"), false);
        player.sendMessage(Text.literal("§7Vous avez obtenu : ")
                .append(rarity)
                .append(Text.literal(" "))
                .append(pokemonName), false);
        player.sendMessage(Text.literal("§7Destination : ").append(destination), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
    }

    private static void maybeBroadcastRareReward(ServerPlayerEntity player, MagicMuffinType type, Pokemon pokemon) {
        boolean legendary = pokemon.isLegendary() || pokemon.isMythical() || pokemon.isUltraBeast();
        boolean shouldBroadcast = switch (type) {
            case SHINY, LEGENDARY -> true;
            case NORMAL -> pokemon.getShiny() || legendary;
        };
        if (!shouldBroadcast) return;
        if (player.getServer() == null) return;

        MutableText pokemonName = getLocalizedPokemonName(pokemon)
                .formatted(pokemon.getShiny() ? Formatting.AQUA : Formatting.YELLOW, Formatting.BOLD);
        MutableText rewardKind = switch (type) {
            case SHINY -> Text.literal("Pokémon shiny").formatted(Formatting.AQUA, Formatting.BOLD);
            case LEGENDARY -> legendary && pokemon.getShiny()
                    ? Text.literal("Pokémon légendaire shiny").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                    : Text.literal("Pokémon légendaire").formatted(Formatting.GOLD, Formatting.BOLD);
            case NORMAL -> {
                if (legendary && pokemon.getShiny()) {
                    yield Text.literal("Pokémon légendaire shiny").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
                }
                if (legendary) {
                    yield Text.literal("Pokémon légendaire").formatted(Formatting.GOLD, Formatting.BOLD);
                }
                yield Text.literal("Pokémon shiny").formatted(Formatting.AQUA, Formatting.BOLD);
            }
        };

        MutableText message = Text.literal("§6§l[Muffin Magique] ")
                .append(Text.literal("§eUn joueur a remporté le "))
                .append(rewardKind)
                .append(Text.literal(" §7: "))
                .append(pokemonName);

        if (pokemon.getShiny()) {
            message.append(Text.literal(" §b✦"));
        }

        player.getServer().getPlayerManager().getPlayerList()
                .forEach(target -> target.sendMessage(message, false));
    }

    private static MutableText getLocalizedPokemonName(Pokemon pokemon) {
        return pokemon.getSpecies().getTranslatedName().copy();
    }

    private static MutableText getRarityLabel(Pokemon pokemon) {
        boolean legendary = pokemon.isLegendary() || pokemon.isMythical() || pokemon.isUltraBeast();
        if (legendary && pokemon.getShiny()) {
            return Text.literal("Légendaire Shiny").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
        }
        if (legendary) {
            return Text.literal("Légendaire").formatted(Formatting.GOLD, Formatting.BOLD);
        }
        if (pokemon.getShiny()) {
            return Text.literal("Shiny").formatted(Formatting.AQUA, Formatting.BOLD);
        }
        return Text.literal("Pokémon").formatted(Formatting.GREEN, Formatting.BOLD);
    }

    private record DeliveryResult(boolean success, boolean sentToPc) {}
}
