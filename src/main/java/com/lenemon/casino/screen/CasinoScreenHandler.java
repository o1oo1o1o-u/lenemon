package com.lenemon.casino.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import com.lenemon.casino.CasinoWorldData;
import net.minecraft.screen.PropertyDelegate;

import java.util.Random;
import java.util.Set;

/**
 * The type Casino screen handler.
 */
public class CasinoScreenHandler extends ScreenHandler {

    /**
     * The Casino data.
     */
// ── Données casino réelles ────────────────────────────────────────────
    public final CasinoWorldData.CasinoData casinoData;
    /**
     * The World data.
     */
    public final CasinoWorldData worldData;
    /**
     * The Casino pos.
     */
    public final BlockPos casinoPos;
    private final PropertyDelegate properties;
    private boolean pendingWin = false;


    // ── Résultat RNG (calculé au spin) ─────────────────────────────
    private boolean win;
    private int resultLeft;
    private int resultRight;

    private boolean canSpin = false;
    private long lastPrice = 0;
    private long lastBalance = 0;
    private boolean casinoLocked = false;
    /**
     * The Spin was debited.
     */
    public boolean spinWasDebited = false;

    /**
     * The Player balance.
     */
    public long playerBalance = 0;

    /**
     * The enum Spin state.
     */
    public enum SpinState {
        /**
         * Idle spin state.
         */
        IDLE,
        /**
         * Spinning spin state.
         */
        SPINNING,
        /**
         * Win spin state.
         */
        WIN,
        /**
         * Lose spin state.
         */
        LOSE }
    private SpinState spinState = SpinState.IDLE;

    private final Random random = new Random();

    /**
     * Instantiates a new Casino screen handler.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     */
// ── Constructeur minimal (pour l'enregistrement Fabric) ───────────────
    public CasinoScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(CasinoScreenRegistry.CASINO_SCREEN_HANDLER, syncId);

        this.casinoData = null;
        this.worldData  = null;
        this.casinoPos  = null;

        this.properties = new PropertyDelegate() {
            private long entry = 0;

            @Override public int get(int index) {
                return index == 0 ? (int) entry : 0;
            }

            @Override public void set(int index, int value) {
                if (index == 0) entry = value;
            }

            @Override public int size() { return 1; }
        };

        this.addProperties(this.properties);
    }

    /**
     * Instantiates a new Casino screen handler.
     *
     * @param syncId          the sync id
     * @param playerInventory the player inventory
     * @param casinoData      the casino data
     * @param worldData       the world data
     * @param casinoPos       the casino pos
     */
// ── Constructeur réel avec données casino ─────────────────────────────
    public CasinoScreenHandler(int syncId, PlayerInventory playerInventory,
                               CasinoWorldData.CasinoData casinoData,
                               CasinoWorldData worldData,
                               BlockPos casinoPos) {

        super(CasinoScreenRegistry.CASINO_SCREEN_HANDLER, syncId);

        this.casinoData = casinoData;
        this.worldData  = worldData;
        this.casinoPos  = casinoPos;

        this.properties = new PropertyDelegate() {

            @Override public int get(int index) {
                if (index == 0) return (int) casinoData.entryPrice;
                return 0;
            }

            @Override public void set(int index, int value) {}

            @Override public int size() { return 1; }
        };

        this.addProperties(this.properties);
    }

//    public void requestSpin() {
//        if (spinState != SpinState.IDLE) return;
//        spinState = SpinState.SPINNING;
//
//        if (win) {
//            // Victoire → les deux slots montrent la même icône
//            resultLeft  = random.nextInt(4);
//            resultRight = resultLeft;
//            spinState   = SpinState.WIN;
//        } else {
//            // Défaite → on s'assure que les deux sont différents
//            resultLeft  = random.nextInt(4);
//            do { resultRight = random.nextInt(4); }
//            while (resultRight == resultLeft);
//            spinState = SpinState.LOSE;
//        }
//    }

    /**
     * Request spin.
     */
    public void requestSpin() {
        if (spinState != SpinState.IDLE) return;
        spinState = SpinState.SPINNING;
    }

    /**
     * Apply spin result.
     *
     * @param win   the win
     * @param left  the left
     * @param right the right
     */
//    public void applySpinResult(boolean win) {
        //        this.win = win;
        //
        //        if (win) {
        //            resultLeft  = random.nextInt(4);
        //            resultRight = resultLeft;
        //            spinState   = SpinState.WIN;
        //        } else {
        //            resultLeft  = random.nextInt(4);
        //            do { resultRight = random.nextInt(4); }
        //            while (resultRight == resultLeft);
        //            spinState = SpinState.LOSE;
        //        }
        //    }
    public void applySpinResult(boolean win, int left, int right) {
        this.win         = win;
        this.pendingWin  = win;  // ← ajoute
        this.resultLeft  = left;
        this.resultRight = right;
        this.spinState   = win ? SpinState.WIN : SpinState.LOSE;
    }

    /**
     * Gets pending win.
     *
     * @return the pending win
     */
// Ajoute le getter :
    public boolean getPendingWin() { return pendingWin; }

    /**
     * Reset state.
     */
    public void resetState()  { spinState = SpinState.IDLE; }

    /**
     * Gets result left.
     *
     * @return the result left
     */
    public int getResultLeft()      { return resultLeft; }

    /**
     * Gets result right.
     *
     * @return the result right
     */
    public int getResultRight()     { return resultRight; }

    /**
     * Gets spin state.
     *
     * @return the spin state
     */
    public SpinState getSpinState() { return spinState; }

    /**
     * Is win boolean.
     *
     * @return the boolean
     */
    public boolean isWin()          { return win; }

    /**
     * Gets entry price.
     *
     * @return the entry price
     */
    public long getEntryPrice() {
        return properties.get(0);
    }

    /**
     * Gets pokemon name.
     *
     * @return the pokemon name
     */
    public String getPokemonName()  { return casinoData != null ? casinoData.pokemonDisplayName : ""; }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

    /**
     * Sets can spin.
     *
     * @param allowed the allowed
     * @param price   the price
     * @param balance the balance
     * @param locked  the locked
     */
    public void setCanSpin(boolean allowed, long price, long balance, boolean locked) {
        this.canSpin = allowed;
        this.lastPrice = price;
        this.lastBalance = balance;
        this.casinoLocked = locked;
    }

    /**
     * Can spin now boolean.
     *
     * @return the boolean
     */
    public boolean canSpinNow() { return canSpin && spinState == SpinState.IDLE; }

    /**
     * Gets last price.
     *
     * @return the last price
     */
    public long getLastPrice() { return lastPrice; }

    /**
     * Gets last balance.
     *
     * @return the last balance
     */
    public long getLastBalance() { return lastBalance; }

    /**
     * Can spin boolean.
     *
     * @return the boolean
     */
    public boolean canSpin() { return canSpin; }

    /**
     * Is casino locked boolean.
     *
     * @return the boolean
     */
    public boolean isCasinoLocked() { return casinoLocked; }

    private String pokemonSpecies = "";
    private Set<String> pokemonAspects = new java.util.HashSet<>();

    /**
     * Sets pokemon render data.
     *
     * @param species the species
     * @param aspects the aspects
     */
    public void setPokemonRenderData(String species, Set<String> aspects) {
        this.pokemonSpecies = species;
        this.pokemonAspects = aspects;
    }

    /**
     * Gets pokemon species.
     *
     * @return the pokemon species
     */
    public String getPokemonSpecies() { return pokemonSpecies; }

    /**
     * Gets pokemon aspects.
     *
     * @return the pokemon aspects
     */
    public Set<String> getPokemonAspects() { return pokemonAspects; }
}