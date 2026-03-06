package com.lenemon.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * The type Luck perms compat.
 */
public final class LuckPermsCompat {
    private LuckPermsCompat() {}

    /**
     * Is luck perms loaded boolean.
     *
     * @return the boolean
     */
    public static boolean isLuckPermsLoaded() {
        return FabricLoader.getInstance().isModLoaded("luckperms");
    }

    /**
     * Gets api or null.
     *
     * @return the api or null
     */
    public static Object getApiOrNull() {
        if (!isLuckPermsLoaded()) return null;
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Method get = provider.getMethod("get");
            return get.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Gets user or null.
     *
     * @param api  the api
     * @param uuid the uuid
     * @return the user or null
     */
    public static Object getUserOrNull(Object api, UUID uuid) {
        if (api == null) return null;
        try {
            Method getUserManager = api.getClass().getMethod("getUserManager");
            Object um = getUserManager.invoke(api);

            Method getUser = um.getClass().getMethod("getUser", UUID.class);
            return getUser.invoke(um, uuid);
        } catch (Throwable ignored) {
            return null;
        }
    }
}