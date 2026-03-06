package com.lenemon.casino;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lenemon.block.CasinoState;
import com.lenemon.casino.holo.CasinoHolograms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Pokemon select session.
 */
public class PokemonSelectSession extends GenericContainerScreenHandler {

    private static final int ROWS = 3;
    private final ServerPlayerEntity owner;
    private final CasinoWorldData.CasinoData casinoData;
    private final CasinoWorldData worldData;
    private final BlockPos casinoPos;
    private final ServerWorld world;
    private final List<Pokemon> partyPokemons = new ArrayList<>();

    /**
     * Instantiates a new Pokemon select session.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     * @param inventory       the inventory
     * @param owner           the owner
     * @param casinoData      the casino data
     * @param worldData       the world data
     * @param casinoPos       the casino pos
     * @param world           the world
     */
    public PokemonSelectSession(int syncId, PlayerInventory playerInventory,
                                Inventory inventory, ServerPlayerEntity owner,
                                CasinoWorldData.CasinoData casinoData,
                                CasinoWorldData worldData,
                                BlockPos casinoPos, ServerWorld world) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, ROWS);
        this.owner = owner;
        this.casinoData = casinoData;
        this.worldData = worldData;
        this.casinoPos = casinoPos;
        this.world = world;
        populateParty();
    }

    private void populateParty() {
        Inventory inv = this.getInventory();

        // Fond gris
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        filler.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP,
                net.minecraft.util.Unit.INSTANCE);
        for (int i = 0; i < ROWS * 9; i++) {
            inv.setStack(i, filler.copy());
        }

        // Récupère la party Cobblemon
        PlayerPartyStore party = com.cobblemon.mod.common.Cobblemon.INSTANCE
                .getStorage().getParty(owner);

        if (party == null) {
            owner.sendMessage(Text.literal("§c[Casino] Impossible de lire votre party."), false);
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15};
        int index = 0;

        for (Pokemon pokemon : party) {
            if (pokemon == null || index >= 6) continue;

            partyPokemons.add(pokemon);

            ItemStack pokemonItem = new ItemStack(Items.PAPER);
            String shinyPrefix = pokemon.getShiny() ? "§e✦ §r" : "";
            pokemonItem.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                    Text.literal(shinyPrefix + "§b" + pokemon.getDisplayName(false).getString()
                            + " §7Niv." + pokemon.getLevel()));

            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal("§7Nature : §f" + pokemon.getNature().getName().toString()));
            lore.add(Text.literal("§7Shiny : " + (pokemon.getShiny() ? "§aOui" : "§cNon")));
            lore.add(Text.literal("§7IVs : §f" +
                    pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP) + "/" +
                    pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK) + "/" +
                    pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE) + "/" +
                    pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK) + "/" +
                    pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE) + "/" +
                    pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED)));
            lore.add(Text.literal(""));
            lore.add(Text.literal("§e► Cliquez pour mettre en jeu"));

            pokemonItem.set(net.minecraft.component.DataComponentTypes.LORE,
                    new net.minecraft.component.type.LoreComponent(lore));
            pokemonItem.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP,
                    net.minecraft.util.Unit.INSTANCE);

            inv.setStack(slots[index], pokemonItem);
            index++;
        }

        if (partyPokemons.isEmpty()) {
            owner.sendMessage(Text.literal("§c[Casino] Votre party est vide !"), false);
        }

        ItemStack cancel = new ItemStack(Items.BARRIER);
        cancel.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                Text.literal("§c§lAnnuler"));
        cancel.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP,
                net.minecraft.util.Unit.INSTANCE);
        inv.setStack(22, cancel);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Bloque shift+clic et tout autre action de déplacement
        if (actionType == SlotActionType.QUICK_MOVE
                || actionType == SlotActionType.THROW
                || actionType == SlotActionType.SWAP
                || actionType == SlotActionType.CLONE) {
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15};
        for (int i = 0; i < slots.length; i++) {
            if (slotIndex == slots[i] && i < partyPokemons.size()) {
                selectPokemon(serverPlayer, partyPokemons.get(i));
                return;
            }
        }

        if (slotIndex == 22) {
            serverPlayer.closeHandledScreen();
        }
    }



    private void selectPokemon(ServerPlayerEntity player, Pokemon pokemon) {
        player.closeHandledScreen();

        PlayerPartyStore party = com.cobblemon.mod.common.Cobblemon.INSTANCE
                .getStorage().getParty(player);
        if (party == null) {
            player.sendMessage(Text.literal("§c[Casino] Erreur party."), false);
            return;
        }
        party.remove(pokemon);

        // Sauvegarde dans le fichier externe
        CasinoPokemonStorage.savePokemon(
                player.getServer(),
                casinoData.casinoUUID,
                pokemon,
                player.getServerWorld().getRegistryManager()
        );

        String displayName = pokemon.getDisplayName(false).getString()
                + " Niv." + pokemon.getLevel()
                + (pokemon.getShiny() ? " ✦" : "");
        String nature = pokemon.getNature().getName().toString();
        String ivs = pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP) + "/" +
                pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK) + "/" +
                pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE) + "/" +
                pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK) + "/" +
                pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE) + "/" +
                pokemon.getIvs().get(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED);

        casinoData.pokemonNature = nature;
        casinoData.pokemonIVs = ivs;
        casinoData.pokemonSpecies = pokemon.getSpecies().getName().toLowerCase();
        casinoData.pokemonAspects = new java.util.HashSet<>(pokemon.getAspects());

        casinoData.pokemonDisplayName = displayName;
        casinoData.state = CasinoState.ACTIVE;
        worldData.markDirty();

        // Recréation du holo "casino ouvert"
        String ownerName;
        if (casinoData.ownerName != null && !casinoData.ownerName.isBlank()) {
            ownerName = casinoData.ownerName;
        } else {
            ownerName = owner.getName().getString(); // fallback
        }

        CasinoHolograms.recreateConfiguredCasinoHologram(
                world,                 // champ de la session
                casinoPos,             // champ de la session
                ownerName,
                displayName,
                casinoData.entryPrice,
                casinoData.winChance / 100.0
        );

        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        player.sendMessage(Text.literal("§a§l  ✔ Casino activé !"), false);
        player.sendMessage(Text.literal("§7Pokémon en jeu : §b" + displayName), false);
        player.sendMessage(Text.literal("§7Prix d'entrée : §e" + casinoData.entryPrice + " PokéCoins"), false);
        player.sendMessage(Text.literal("§7Chance de gain : §e" + (casinoData.winChance / 100.0) + "%"), false);
        player.sendMessage(Text.literal("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);

    }

    /**
     * Open.
     *
     * @param player     the player
     * @param casino     the casino
     * @param data       the data
     * @param clickedPos the clicked pos
     * @param world      the world
     */
    public static void open(ServerPlayerEntity player, CasinoWorldData.CasinoData casino,
                            CasinoWorldData data, BlockPos clickedPos, ServerWorld world) {

        final BlockPos bottomPos =
                data.isRegisteredAt(clickedPos) ? clickedPos :
                        data.isRegisteredAt(clickedPos.down()) ? clickedPos.down() :
                                data.isRegisteredAt(clickedPos.up()) ? clickedPos.up() :
                                        null;

        if (bottomPos == null) {
            player.sendMessage(Text.literal("§c[Casino] Casino introuvable (position invalide)."), false);
            return;
        }

        SimpleInventory inventory = new SimpleInventory(ROWS * 9);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new PokemonSelectSession(
                        syncId, playerInv, inventory, player, casino, data, bottomPos, world),
                Text.literal("§8§l» §fChoisir un Pokémon")
        ));
    }
}