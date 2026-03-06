package com.lenemon.gift;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

import java.util.List;

/**
 * The type Gift open screen.
 */
public class GiftOpenScreen extends GenericContainerScreenHandler {

    private static final int ROWS = 3;
    // Ligne du milieu slots 9-17, on utilise 9 à 17
    private static final int[] REEL = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int CENTER = 13; // slot central

    private final SimpleInventory inv;

    /**
     * Instantiates a new Gift open screen.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     * @param inventory       the inventory
     */
    public GiftOpenScreen(int syncId, PlayerInventory playerInventory,
                          SimpleInventory inventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, ROWS);
        this.inv = inventory;
        initGui();
    }

    private void initGui() {
        ItemStack filler = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        filler.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        filler.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        for (int i = 0; i < ROWS * 9; i++) inv.setStack(i, filler.copy());

        // Indicateurs haut/bas pour montrer le centre
        ItemStack arrow = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
        arrow.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§a▼"));
        arrow.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(4, arrow.copy()); // haut centre
        ItemStack arrowB = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
        arrowB.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§a▲"));
        arrowB.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(22, arrowB.copy()); // bas centre

        // Items gris initiaux sur la ligne du milieu
        ItemStack gray = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        gray.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§7..."));
        gray.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        for (int slot : REEL) inv.setStack(slot, gray.copy());
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Bouton fermer slot 26 uniquement quand animation terminée
        if (slotIndex == 26 && !inv.getStack(26).isEmpty()
                && inv.getStack(26).getItem() != Items.BLACK_STAINED_GLASS_PANE) {
            if (player instanceof ServerPlayerEntity sp) sp.closeHandledScreen();
        }
        // Tout le reste bloqué
    }

    /**
     * Open.
     *
     * @param player the player
     * @param chest  the chest
     * @param world  the world
     */
    public static void open(ServerPlayerEntity player, GiftChestData.ChestEntry chest,
                            ServerWorld world) {
        SimpleInventory inventory = new SimpleInventory(ROWS * 9);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GiftOpenScreen(syncId, playerInv, inventory),
                Text.literal("§8§l» §f🎁 " + chest.chestName + " — Bonne chance !")
        ));
        GiftLotteryTicker.start(player, chest, world, inventory);
    }
}