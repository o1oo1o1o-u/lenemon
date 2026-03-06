package com.lenemon.pokemon;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ShinyAnnouncer {

    public static void register() {
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(event -> {
            if (!event.getEntity().getPokemon().getShiny()) return;

            ServerWorld world = (ServerWorld) event.getEntity().getWorld();

            ServerPlayerEntity nearestPlayer = world.getClosestPlayer(
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    128, false
            ) instanceof ServerPlayerEntity sp ? sp : null;

            if (nearestPlayer == null) return;

            MutableText pokemonName = event.getEntity().getPokemon().getDisplayName(false).copy();
            String playerName  = nearestPlayer.getName().getString();
            int x = event.getEntity().getBlockX();
            int y = event.getEntity().getBlockY();
            int z = event.getEntity().getBlockZ();

            MutableText message = Text.literal("[Shiny] ").formatted(Formatting.GOLD)
                    .append(pokemonName.formatted(Formatting.YELLOW, Formatting.BOLD))
                    .append(Text.literal(" shiny est apparu prêt de ").formatted(Formatting.WHITE))
                    .append(Text.literal(playerName).formatted(Formatting.AQUA))
                    .append(Text.literal(" - ").formatted(Formatting.WHITE))
                    .append(Text.literal("[" + x + ", " + y + ", " + z + "]")
                            .formatted(Formatting.WHITE, Formatting.BOLD));

            world.getServer().getPlayerManager().getPlayerList()
                    .forEach(p -> p.sendMessage(message, false));
        });
    }
}
