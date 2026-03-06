package com.lenemon.gift;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Gift chest config screen.
 */
public class GiftChestConfigScreen extends GenericContainerScreenHandler {

    private static final int ROWS = 6;
    private final ServerPlayerEntity admin;
    private final GiftChestData.ChestEntry chest;
    private final GiftChestData worldData;
    private final BlockPos chestPos;
    private final ServerWorld world;
    private final SimpleInventory inv;
    private List<GiftReward> rewards;

    /**
     * Instantiates a new Gift chest config screen.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     * @param inventory       the inventory
     * @param admin           the admin
     * @param chest           the chest
     * @param worldData       the world data
     * @param chestPos        the chest pos
     * @param world           the world
     */
    public GiftChestConfigScreen(int syncId, PlayerInventory playerInventory,
                                 SimpleInventory inventory, ServerPlayerEntity admin,
                                 GiftChestData.ChestEntry chest, GiftChestData worldData,
                                 BlockPos chestPos, ServerWorld world) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, ROWS);
        this.admin = admin;
        this.chest = chest;
        this.worldData = worldData;
        this.chestPos = chestPos;
        this.world = world;
        this.inv = inventory;
        this.rewards = GiftChestConfig.load(admin.getServer(), chest.chestUUID);
        populate();
    }

    private void populate() {
        // Fond
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        filler.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        for (int i = 0; i < ROWS * 9; i++) inv.setStack(i, filler.copy());

        // Ligne du haut : toolbar admin (slots 0-8) = items de la main pour ajouter
        // On affiche les items de l'inventaire de l'admin en slots 0-8
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarItem = admin.getInventory().getStack(i);
            if (!hotbarItem.isEmpty()) {
                inv.setStack(i, hotbarItem.copy());
            } else {
                ItemStack empty = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
                empty.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§7Slot vide"));
                empty.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                inv.setStack(i, empty);
            }
        }

        // Bouton ajouter commande slot 8 (override dernier slot toolbar)
        ItemStack addCmd = new ItemStack(Items.COMMAND_BLOCK);
        addCmd.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§e§l+ Ajouter une commande"));
        addCmd.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7Cliquez pour entrer une commande via le chat")
        )));
        addCmd.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(8, addCmd);

        // Récompenses existantes slots 9-44 (5 lignes)
        int slot = 9;
        for (int i = 0; i < rewards.size() && slot < 45; i++, slot++) {
            GiftReward r = rewards.get(i);
            ItemStack display = rewardToItem(r, i);
            inv.setStack(slot, display);
        }

        // Bouton récupérer bon cadeau slot 49
        ItemStack ticket = new ItemStack(Items.PAPER);
        ticket.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§6§l🎁 Récupérer un bon cadeau"));
        ticket.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7Cliquez pour obtenir un bon cadeau pour §f" + chest.chestName)
        )));
        ticket.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(49, ticket);

        // Bouton fermer slot 53
        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§cFermer"));
        close.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(53, close);

        // bouton detruire
        ItemStack deleteBtn = new ItemStack(Items.TNT);
        deleteBtn.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§c§l⚠ Supprimer ce coffre"));
        deleteBtn.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7Tapez §cconfirmer §7dans le chat pour supprimer.")
        )));
        deleteBtn.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        inv.setStack(45, deleteBtn);
    }

    private ItemStack rewardToItem(GiftReward r, int index) {
        ItemStack display;
        if (r.type.equals("item")) {
            try {
                // Parse le NBT complet pour récupérer l'item réel
                NbtCompound nbt = net.minecraft.nbt.StringNbtReader.parse(r.data);
                display = ItemStack.CODEC.parse(
                        net.minecraft.registry.RegistryOps.of(
                                net.minecraft.nbt.NbtOps.INSTANCE,
                                admin.getServerWorld().getRegistryManager()
                        ), nbt
                ).result().orElse(new ItemStack(Items.BARRIER));
                if (display.isEmpty()) display = new ItemStack(Items.BARRIER);
            } catch (Exception e) {
                // Fallback si c'est un ancien format ID simple
                try {
                    display = new ItemStack(
                            net.minecraft.registry.Registries.ITEM.get(
                                    net.minecraft.util.Identifier.of(r.data)));
                    if (display.isEmpty()) display = new ItemStack(Items.BARRIER);
                } catch (Exception e2) {
                    display = new ItemStack(Items.BARRIER);
                }
            }
        } else {
            display = new ItemStack(Items.COMMAND_BLOCK);
        }

        display.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§f" + r.displayName + " §7(" + r.chance + "%)"));
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("§7Type : §f" + r.type));
        if (r.type.equals("item")) lore.add(Text.literal("§7Quantité : §f" + r.count));
        else lore.add(Text.literal("§7Commande : §f/" + r.data));
        lore.add(Text.literal("§7Taux : §e" + r.chance + "%"));
        lore.add(Text.literal(""));
        lore.add(Text.literal("§cClic droit pour supprimer"));
        display.set(DataComponentTypes.LORE, new LoreComponent(lore));
        display.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return display;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        if (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.THROW) return;

        // Fermer
        if (slotIndex == 53) { sp.closeHandledScreen(); return; }

        if (slotIndex == 45) {
            sp.closeHandledScreen();
            GiftDeleteSession.start(sp, chest, worldData, chestPos, world);
        }

        // Récupérer bon cadeau
        if (slotIndex == 49) {
            sp.closeHandledScreen();
            sp.giveItemStack(GiftItemHelper.createGiftTicket(chest.chestUUID, chest.chestName));
            sp.sendMessage(Text.literal("§a[Cadeau] Bon cadeau §f" + chest.chestName + "§a obtenu !"), false);
            return;
        }

        // Ajouter commande
        if (slotIndex == 8) {
            sp.closeHandledScreen();
            GiftCommandSession.start(sp, chest, worldData, chestPos, world);
            return;
        }

        // Toolbar (slots 0-7) → ajouter item comme récompense
        if (slotIndex >= 0 && slotIndex <= 7) {
            ItemStack item = admin.getInventory().getStack(slotIndex);
            if (item.isEmpty()) return;

            sp.closeHandledScreen();
            GiftRateSession.start(sp, chest, worldData, chestPos, world, item.copy());
            return;
        }

        // Récompenses existantes (slots 9-44) → clic droit = supprimer
        if (slotIndex >= 9 && slotIndex <= 44 && button == 1) {
            int rewardIndex = slotIndex - 9;
            if (rewardIndex < rewards.size()) {
                rewards.remove(rewardIndex);
                GiftChestConfig.save(sp.getServer(), chest.chestUUID, rewards);
                sp.sendMessage(Text.literal("§c[Cadeau] Récompense supprimée."), false);
                populate();
            }
        }
    }

    /**
     * Open.
     *
     * @param player the player
     * @param chest  the chest
     * @param data   the data
     * @param pos    the pos
     * @param world  the world
     */
    public static void open(ServerPlayerEntity player, GiftChestData.ChestEntry chest,
                            GiftChestData data, BlockPos pos, ServerWorld world) {
        SimpleInventory inventory = new SimpleInventory(ROWS * 9);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GiftChestConfigScreen(
                        syncId, playerInv, inventory, player, chest, data, pos, world),
                Text.literal("§8§l» §f🎁 Config — §6" + chest.chestName)
        ));
    }
}