package com.lenemon.gift;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Gift preview screen.
 */
public class GiftPreviewScreen extends GenericContainerScreenHandler {

    private static final int ROWS = 6;

    /**
     * Instantiates a new Gift preview screen.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     * @param inventory       the inventory
     * @param player          the player
     * @param chest           the chest
     */
    public GiftPreviewScreen(int syncId, PlayerInventory playerInventory,
                             SimpleInventory inventory, ServerPlayerEntity player,
                             GiftChestData.ChestEntry chest) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, ROWS);
        populate(inventory, player, chest);
    }

    private void populate(SimpleInventory inv, ServerPlayerEntity player,
                          GiftChestData.ChestEntry chest) {
        // Fond
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        filler.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        for (int i = 0; i < ROWS * 9; i++) inv.setStack(i, filler.copy());

        List<GiftReward> rewards = GiftChestConfig.load(player.getServer(), chest.chestUUID);

        // Titre info slot 4
        ItemStack title = new ItemStack(Items.CHEST);
        title.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§6§l🎁 " + chest.chestName));
        List<Text> titleLore = new ArrayList<>();
        titleLore.add(Text.literal("§7" + rewards.size() + " récompense(s) possible(s)"));
        //titleLore.add(Text.literal("§7Propriétaire : §f" + chest.ownerName));
        title.set(DataComponentTypes.LORE, new LoreComponent(titleLore));
        title.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(4, title);
        double total = rewards.stream().mapToDouble(r -> r.chance).sum();
        // Récompenses slots 9 à 44
        int slot = 9;
        for (GiftReward r : rewards) {
            if (slot > 44) break;

            ItemStack display;
            if (r.type.equals("item")) {
                try {
                    net.minecraft.nbt.NbtCompound nbt = net.minecraft.nbt.StringNbtReader.parse(r.data);
                    display = ItemStack.CODEC.parse(
                            net.minecraft.registry.RegistryOps.of(
                                    net.minecraft.nbt.NbtOps.INSTANCE,
                                    player.getServerWorld().getRegistryManager()
                            ), nbt
                    ).result().orElse(new ItemStack(Items.BARRIER));
                    if (display.isEmpty()) display = new ItemStack(Items.BARRIER);
                } catch (Exception e) {
                    // Fallback ancien format ID simple
                    try {
                        display = new ItemStack(Registries.ITEM.get(Identifier.of(r.data)));
                        if (display.isEmpty()) display = new ItemStack(Items.BARRIER);
                    } catch (Exception e2) {
                        display = new ItemStack(Items.BARRIER);
                    }
                }
                display.setCount(r.count);
            } else {
                display = new ItemStack(Items.COMMAND_BLOCK);
            }

            display.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("§f" + r.displayName));
            List<Text> lore = new ArrayList<>();
            //lore.add(Text.literal("§7Chance : §e" + r.chance + "%"));
            lore.add(Text.literal("§7Chance : §e" + String.format("%.2f", (r.chance / total * 100)) + "%"));
            if (r.type.equals("item"))
                lore.add(Text.literal("§7Quantité : §f" + r.count));
            //else
             //   lore.add(Text.literal("§7Commande : §f/" + r.data));
            display.set(DataComponentTypes.LORE, new LoreComponent(lore));
            display.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
            inv.setStack(slot, display);
            slot++;
        }

        if (rewards.isEmpty()) {
            ItemStack empty = new ItemStack(Items.BARRIER);
            empty.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("§c§lAucune récompense configurée"));
            empty.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
            inv.setStack(22, empty);
        }

        // Bouton fermer slot 49
        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§cFermer"));
        close.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(49, close);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex == 49 && player instanceof ServerPlayerEntity sp) {
            sp.closeHandledScreen();
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
                (syncId, playerInv, p) -> new GiftPreviewScreen(syncId, playerInv, inventory, player, chest),
                Text.literal("§8§l» §f🎁 Contenu — §6" + chest.chestName)
        ));
    }
}