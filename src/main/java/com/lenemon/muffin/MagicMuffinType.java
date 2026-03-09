package com.lenemon.muffin;

import java.util.Arrays;

public enum MagicMuffinType {
    NORMAL("normal", "§6§lMuffin Magique"),
    SHINY("shiny", "§b§lMuffin Magique Shiny"),
    LEGENDARY("legendary", "§c§lMuffin Magique Legendaire");

    private final String id;
    private final String displayName;

    MagicMuffinType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static MagicMuffinType fromId(String raw) {
        if (raw == null) return null;
        return Arrays.stream(values())
                .filter(type -> type.id.equalsIgnoreCase(raw))
                .findFirst()
                .orElse(null);
    }
}
