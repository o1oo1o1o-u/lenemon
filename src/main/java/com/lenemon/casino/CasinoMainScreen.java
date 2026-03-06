package com.lenemon.casino;

import com.lenemon.block.CasinoBlockEntity;
import com.lenemon.block.CasinoState;
import com.lenemon.casino.CasinoCancelHandler;
import com.lenemon.casino.CasinoConfigSession;
import com.lenemon.casino.PokemonSelectSession;
import com.lenemon.casino.CasinoSpinScreen;

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
 * The type Casino main screen.
 */
public class CasinoMainScreen extends GenericContainerScreenHandler {

    private static final int ROWS = 3;
    private final ServerPlayerEntity viewer;
    private final CasinoBlockEntity entity;
    private final BlockPos pos;

    /**
     * Instantiates a new Casino main screen.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     * @param viewer          the viewer
     * @param entity          the entity
     * @param pos             the pos
     */
    public CasinoMainScreen(int syncId, PlayerInventory playerInventory,
                            ServerPlayerEntity viewer, CasinoBlockEntity entity, BlockPos pos) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, new SimpleInventory(ROWS * 9), ROWS);
        this.viewer = viewer;
        this.entity = entity;
        this.pos = pos;
        redraw();
    }

    private void redraw() {
        var inv = this.getInventory();

        ItemStack filler = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        filler.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        filler.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        for (int i = 0; i < ROWS * 9; i++) inv.setStack(i, filler.copy());

        // Infos
        ItemStack info = new ItemStack(Items.PAPER);
        info.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§f§lInfos"));
        info.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
                Text.literal("§7Etat : §f" + entity.getCasinoState().name()),
                Text.literal("§7Pokemon : §b" + (entity.getPokemonDisplayName().isEmpty() ? "Aucun" : entity.getPokemonDisplayName())),
                Text.literal("§7Prix : §e" + entity.getEntryPrice() + " PokéCoins"),
                Text.literal("§7Chance : §e" + (entity.getWinChance() / 100.0) + "%")
        )));
        info.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        inv.setStack(4, info);

        // Jouer
        ItemStack play = new ItemStack(Items.EMERALD);
        play.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§a§lJouer"));
        play.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
                Text.literal("§7Lance un spin"),
                Text.literal("§7Prix : §e" + entity.getEntryPrice() + " PokéCoins")
        )));
        play.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        inv.setStack(11, play);

        // Mettre un Pokémon (owner)
        ItemStack setPoke = new ItemStack(Items.NAME_TAG);
        setPoke.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§b§lMettre un Pokemon"));
        setPoke.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
                Text.literal("§7Owner only"),
                Text.literal("§7Choisir dans la party")
        )));
        setPoke.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        inv.setStack(13, setPoke);

        // Config (owner)
        ItemStack config = new ItemStack(Items.COMPARATOR);
        config.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§6§lConfigurer"));
        config.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
                Text.literal("§7Owner only"),
                Text.literal("§7Prix + chance via chat")
        )));
        config.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        inv.setStack(15, config);

        // Annuler / rendre Pokémon (owner)
        ItemStack cancel = new ItemStack(Items.BARRIER);
        cancel.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§c§lAnnuler"));
        cancel.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
                Text.literal("§7Owner only"),
                Text.literal("§7Rendre le Pokemon")
        )));
        cancel.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        inv.setStack(22, cancel);

        // Fermer
        ItemStack close = new ItemStack(Items.IRON_DOOR);
        close.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§7Fermer"));
        close.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        inv.setStack(26, close);

        // Si casino pas jouable, on peut rendre le bouton “jouer” visuellement rouge
        if (entity.getCasinoState() != CasinoState.ACTIVE || entity.isLocked()) {
            ItemStack disabled = new ItemStack(Items.REDSTONE);
            disabled.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§c§lIndisponible"));
            disabled.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
                    Text.literal("§7Etat : §f" + entity.getCasinoState().name()),
                    Text.literal(entity.isLocked() ? "§7Un spin est en cours" : "§7Aucun Pokemon en jeu")
            )));
            disabled.set(net.minecraft.component.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
            inv.setStack(11, disabled);
        }
    }

    private boolean isOwner(ServerPlayerEntity p) {
        return entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(p.getUuid());
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

        if (slotIndex == 26) {
            sp.closeHandledScreen();
            return;
        }

        // Jouer
        if (slotIndex == 11) {
            if (entity.isLocked() || entity.getCasinoState() != CasinoState.ACTIVE) {
                sp.sendMessage(Text.literal("§c[Casino] Indisponible pour le moment."), false);
                return;
            }
            sp.closeHandledScreen();
            return;
        }

        // Owner actions
        if (slotIndex == 13) {
            if (!isOwner(sp)) {
                sp.sendMessage(Text.literal("§c[Casino] Reserve au proprietaire."), false);
                return;
            }
            if (CasinoConfigSession.hasActiveSession(sp.getUuid())) {
                sp.sendMessage(Text.literal("§c[Casino] Une config est deja en cours (chat)."), false);
                return;
            }
            // Mettre un Pokemon: on ouvre le select
            sp.closeHandledScreen();
            // handled by PCBlockMixin
            return;
        }

        if (slotIndex == 15) {
            if (!isOwner(sp)) {
                sp.sendMessage(Text.literal("§c[Casino] Reserve au proprietaire."), false);
                return;
            }
            sp.closeHandledScreen();
            CasinoConfigSession.startConfig(sp, pos, entity);
            return;
        }

        if (slotIndex == 22) {
            if (!isOwner(sp)) {
                sp.sendMessage(Text.literal("§c[Casino] Reserve au proprietaire."), false);
                return;
            }
            sp.closeHandledScreen();
            CasinoCancelHandler.cancel(sp, entity, pos);
            return;
        }
    }

    /**
     * Open.
     *
     * @param player the player
     */
    public static void open(ServerPlayerEntity player) {
        // Variante: si tu veux un menu casino global sans bloc, a toi de choisir
        player.sendMessage(Text.literal("§c[Casino] Ouvre via le bloc casino."), false);
    }

    /**
     * Open.
     *
     * @param player the player
     * @param entity the entity
     * @param pos    the pos
     */
    public static void open(ServerPlayerEntity player, CasinoBlockEntity entity, BlockPos pos) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new CasinoMainScreen(syncId, inv, player, entity, pos),
                Text.literal("§8§l» §fCasino")
        ));
    }
}