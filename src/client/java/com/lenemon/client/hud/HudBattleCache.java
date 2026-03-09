package com.lenemon.client.hud;

public final class HudBattleCache {

    private static boolean inBattle = false;

    private HudBattleCache() {}

    public static boolean isInBattle() {
        return inBattle;
    }

    public static void setInBattle(boolean value) {
        inBattle = value;
    }
}
