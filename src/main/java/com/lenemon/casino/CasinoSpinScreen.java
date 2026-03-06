package com.lenemon.casino;

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
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * The type Casino spin screen.
 */
public class CasinoSpinScreen extends GenericContainerScreenHandler {

    private static final int ROWS = 3;

    /**
     * Instantiates a new Casino spin screen.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     * @param inventory       the inventory
     */
    public CasinoSpinScreen(int syncId, PlayerInventory playerInventory, SimpleInventory inventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, ROWS);
        drawIdle(inventory);
    }

    private void drawIdle(SimpleInventory inv) {
        ItemStack filler = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        filler.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        filler.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP,
                net.minecraft.util.Unit.INSTANCE);
        for (int i = 0; i < ROWS * 9; i++) inv.setStack(i, filler.copy());

        // 3 laines grises initiales
        ItemStack gray = new ItemStack(Items.GRAY_WOOL);
        gray.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§7?"));
        gray.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP,
                net.minecraft.util.Unit.INSTANCE);
        inv.setStack(11, gray.copy());
        inv.setStack(13, gray.copy());
        inv.setStack(15, gray.copy());
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
        if (!(player instanceof ServerPlayerEntity sp)) return;

        ItemStack clicked = this.getInventory().getStack(slotIndex);
        if (clicked.isEmpty()) return;

        if (slotIndex == 22 || slotIndex == 20) {
            sp.closeHandledScreen();
            return;
        }

        if (slotIndex == 24) {
            // Rejouer — récupère le casino depuis le SpinTicker
            sp.closeHandledScreen();
            CasinoSpinTicker.requestReplay(sp);
        }
    }

    /**
     * Open.
     *
     * @param player the player
     * @param casino the casino
     * @param data   the data
     * @param pos    the pos
     */
    public static void open(ServerPlayerEntity player, CasinoWorldData.CasinoData casino,
                            CasinoWorldData data, BlockPos pos) {
        SimpleInventory inventory = new SimpleInventory(ROWS * 9);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new CasinoSpinScreen(syncId, playerInv, inventory),
                Text.literal("§8§l» §f🎰 Spin en cours...")
        ));
        CasinoSpinTicker.start(player, casino, data, pos, inventory);
    }
}