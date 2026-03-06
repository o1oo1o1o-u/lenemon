package com.lenemon.screen;

import com.lenemon.block.CasinoBlockEntity;
import com.lenemon.block.CasinoState;
import com.lenemon.casino.CasinoSpinHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import com.lenemon.casino.CasinoWorldData;
import net.minecraft.server.world.ServerWorld;


import java.util.ArrayList;
import java.util.List;

/**
 * The type Casino screen handler.
 */
public class CasinoScreenHandler extends GenericContainerScreenHandler {

    private static final int ROWS = 4;
    private final CasinoBlockEntity casinoEntity;
    private final BlockPos casinoPos;
    private final ServerPlayerEntity viewer;
    private CasinoWorldData.CasinoData casinoData;
    private CasinoWorldData worldData;

    /**
     * Instantiates a new Casino screen handler.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     * @param inventory       the inventory
     * @param casinoEntity    the casino entity
     * @param casinoPos       the casino pos
     * @param viewer          the viewer
     */
    public CasinoScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
                               CasinoBlockEntity casinoEntity, BlockPos casinoPos, ServerPlayerEntity viewer) {
        super(ScreenHandlerType.GENERIC_9X4, syncId, playerInventory, inventory, ROWS);
        this.casinoEntity = casinoEntity;
        this.casinoPos = casinoPos;
        this.viewer = viewer;
        populateGui();
    }

    /**
     * Instantiates a new Casino screen handler.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     * @param inventory       the inventory
     * @param casinoPos       the casino pos
     * @param viewer          the viewer
     * @param casinoData      the casino data
     * @param worldData       the world data
     */
    public CasinoScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
                               BlockPos casinoPos, ServerPlayerEntity viewer,
                               CasinoWorldData.CasinoData casinoData, CasinoWorldData worldData) {
        super(ScreenHandlerType.GENERIC_9X4, syncId, playerInventory, inventory, ROWS);
        this.casinoEntity = null;
        this.casinoPos = casinoPos;
        this.viewer = viewer;
        this.casinoData = casinoData;
        this.worldData = worldData;
        populateGui();
    }

    private void populateGui() {
        Inventory inv = this.getInventory();

        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        filler.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        for (int i = 0; i < ROWS * 9; i++) inv.setStack(i, filler.copy());

        // Source unifiée
        CasinoState state = (casinoData != null) ? casinoData.state :
                (casinoEntity != null) ? casinoEntity.getCasinoState() : CasinoState.UNCONFIGURED;
        String pokemonName = (casinoData != null) ? casinoData.pokemonDisplayName :
                (casinoEntity != null) ? casinoEntity.getPokemonDisplayName() : "";
        long price = (casinoData != null) ? casinoData.entryPrice :
                (casinoEntity != null) ? casinoEntity.getEntryPrice() : 0;
        int chance = (casinoData != null) ? casinoData.winChance :
                (casinoEntity != null) ? casinoEntity.getWinChance() : 0;
        String ownerName = (casinoData != null) ? casinoData.ownerName :
                (casinoEntity != null) ? casinoEntity.getOwnerName() : "?";

        if (state == CasinoState.ACTIVE) {
            populateActive(inv, pokemonName, price, chance, ownerName);
        } else {
            populateUnavailable(inv, state);
        }
    }

    private void populateActive(Inventory inv, String pokemonName, long price, int chance, String ownerName) {
        ItemStack pokemonInfo = new ItemStack(Items.FILLED_MAP);
        pokemonInfo.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                Text.literal("§b§l🏆 Pokémon en jeu"));
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("§f" + pokemonName));
        String nature = (casinoData != null) ? casinoData.pokemonNature : "";
        String ivs = (casinoData != null) ? casinoData.pokemonIVs : "";
        lore.add(Text.literal("§7Nature : §f" + nature));
        lore.add(Text.literal("§7IVs : §f" + ivs));
        lore.add(Text.literal(""));
        lore.add(Text.literal("§7Propriétaire : §e" + ownerName));
        pokemonInfo.set(net.minecraft.component.DataComponentTypes.LORE,
                new net.minecraft.component.type.LoreComponent(lore));
        pokemonInfo.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(4, pokemonInfo);

        ItemStack priceInfo = new ItemStack(Items.GOLD_NUGGET);
        priceInfo.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                Text.literal("§e§lPrix d'entrée"));
        priceInfo.set(net.minecraft.component.DataComponentTypes.LORE,
                new net.minecraft.component.type.LoreComponent(
                        List.of(Text.literal("§f" + price + " PokéCoins"))));
        priceInfo.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(18, priceInfo);

        ItemStack chanceInfo = new ItemStack(Items.COMPARATOR);
        chanceInfo.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                Text.literal("§a§lChance de gain"));
        chanceInfo.set(net.minecraft.component.DataComponentTypes.LORE,
                new net.minecraft.component.type.LoreComponent(
                        List.of(Text.literal("§f" + (chance / 100.0) + "%"))));
        chanceInfo.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(26, chanceInfo);

        ItemStack spinButton = new ItemStack(Items.EMERALD);
        spinButton.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                Text.literal("§a§l▶ JOUER !"));
        List<Text> spinLore = new ArrayList<>();
        spinLore.add(Text.literal("§7Coût : §e" + price + " PokéCoins"));
        spinLore.add(Text.literal("§7Chance : §a" + (chance / 100.0) + "%"));
        spinLore.add(Text.literal(""));
        spinLore.add(Text.literal("§eCliquez pour tenter votre chance !"));
        spinButton.set(net.minecraft.component.DataComponentTypes.LORE,
                new net.minecraft.component.type.LoreComponent(spinLore));
        spinButton.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(22, spinButton);
    }

    private void populateUnavailable(Inventory inv, CasinoState state) {
        ItemStack unavailable = new ItemStack(Items.BARRIER);
        String message = switch (state) {
            case UNCONFIGURED -> "§c§l⚠ Machine non configurée";
            case CONFIGURED   -> "§e§l⚠ En attente d'un Pokémon";
            case LOCKED       -> "§6§l⏳ Spin en cours...";
            case EMPTY        -> "§c§l⚠ Machine vide";
            default           -> "§c§l⚠ Non disponible";
        };
        unavailable.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                Text.literal(message));
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("§7Cette machine n'est pas disponible."));
        unavailable.set(net.minecraft.component.DataComponentTypes.LORE,
                new net.minecraft.component.type.LoreComponent(lore));
        unavailable.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(22, unavailable);
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
        if (casinoData == null || casinoData.state != CasinoState.ACTIVE) return;

        if (slotIndex == 22) {
            if (casinoData.locked) {
                serverPlayer.sendMessage(Text.literal("§c[Casino] Un spin est déjà en cours !"), false);
                return;
            }
            serverPlayer.closeHandledScreen();
            CasinoSpinHandler.spinFromWorldData(serverPlayer, casinoData, worldData, casinoPos);
        }
    }

    /**
     * Open.
     *
     * @param player the player
     * @param entity the entity
     * @param pos    the pos
     */
    public static void open(ServerPlayerEntity player, CasinoBlockEntity entity, BlockPos pos) {
        // Le proprio ne peut pas jouer sur son propre casino
        if (entity.isOwner(player.getUuid())) {
            player.sendMessage(Text.literal("§c[Casino] Vous ne pouvez pas jouer sur votre propre casino !"), false);
            return;
        }

        SimpleInventory inventory = new SimpleInventory(ROWS * 9);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new CasinoScreenHandler(syncId, playerInv, inventory, entity, pos, player),
                Text.literal("§8§l» §f🎰 Casino de §6" + entity.getOwnerName())
        ));
    }

    /**
     * Open from world data.
     *
     * @param player the player
     * @param casino the casino
     * @param pos    the pos
     * @param world  the world
     */
    public static void openFromWorldData(ServerPlayerEntity player,
                                         CasinoWorldData.CasinoData casino,
                                         BlockPos pos, ServerWorld world) {
        if (casino.ownerUUID.equals(player.getUuid())) {
            player.sendMessage(Text.literal("§c[Casino] Vous ne pouvez pas jouer sur votre propre casino !!"), false);
            return;
        }

        SimpleInventory inventory = new SimpleInventory(ROWS * 9);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new CasinoScreenHandler(
                        syncId, playerInv, inventory, pos, player, casino, CasinoWorldData.get(world)),
                Text.literal("§8§l» §f🎰 Casino de §6" + casino.ownerName)
        ));
    }
}